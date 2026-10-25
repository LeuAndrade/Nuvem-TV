'use strict';
const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const source=fs.readFileSync('app/src/main/assets/player.js','utf8');
function fixture(mode='off'){
 const sent=[],calls=[],intervals=[],timeouts=[],statuses=[],stats=[];let statsCalls=0;let captures=0,stops=0,pendingCapture;
 const track={kind:'audio',stop(){stops++;}},stream={getTracks:()=>[track],getAudioTracks:()=>[track]};
 const media={};for(const id of ['video','audio'])media[id]={srcObject:null,muted:true,volume:0,plays:0,paused:true,addEventListener(){},play(){this.paused=false;this.plays++;return Promise.resolve();}};
 const video=media.video;
 class Peer {
  constructor(){Peer.instance=this;this.iceGatheringState='complete';this.connectionState='connected';this.signalingState='stable';this.channels={};this.transceivers=[];}
  addTransceiver(kind,config){const t={receiver:{track:{kind}},sender:{replaceTrack:async track=>{t.sender.track=track;}},direction:config.direction,setCodecPreferences:codecs=>assert.equal(codecs[0].mimeType,'video/H264')};this.transceivers.push(t);return t;}
  addTrack(track,stream){this.addedTrack=track;this.addedStream=stream;return {track};}
  async getStats(){statsCalls++;return new Map([["audio",{type:"outbound-rtp",kind:"audio",packetsSent:12,bytesSent:1200}]]);}
  getTransceivers(){return this.transceivers;}
  createDataChannel(name,config){assert.ok(config.protocol);const d={readyState:'open',bufferedAmount:0,send:data=>sent.push({name,data}),close(){}};this.channels[name]=d;return d;}
  async createOffer(){return {type:'offer',sdp:'v=0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\na=recvonly\r\n'};}
  async setLocalDescription(offer){if(offer.type==='rollback'){this.signalingState='stable';return;}assert.match(offer.sdp,/b=AS:5000/);this.signalingState='have-local-offer';}
  async setRemoteDescription(answer){assert.equal(answer.type,'answer');this.signalingState='stable';}
  async addIceCandidate(c){assert.equal(typeof c.sdpMLineIndex,'number');}
  close(){this.closed=true;}
 }
 const sandbox={console,performance:{now:()=>123},ArrayBuffer,DataView,Math,Date,JSON,Number,String,Error,Promise,RTCPeerConnection:Peer,RTCRtpReceiver:{getCapabilities:()=>({codecs:[{mimeType:'video/H264'}]})},document:{getElementById:id=>media[id],addEventListener(){}},MediaStream:class{constructor(){this.tracks=[];}addTrack(track){if(!this.tracks.includes(track))this.tracks.push(track);}getTracks(){return this.tracks;}getAudioTracks(){return this.tracks.filter(t=>t.kind==='audio');}removeTrack(track){this.tracks=this.tracks.filter(t=>t!==track);}},navigator:{mediaDevices:{getUserMedia:async options=>{captures++;assert.equal(options.video,false);if(mode==='denied')throw new Error('NotAllowedError');if(mode==='pending')return new Promise(resolve=>pendingCapture=()=>resolve(stream));return stream;}}},setTimeout:f=>(timeouts.push(f),timeouts.length),setInterval:f=>(intervals.push(f),intervals.length),clearTimeout(){},clearInterval(){}};
 sandbox.window=sandbox;
 sandbox.CloudBridge={call(id,action,payload){const p=JSON.parse(payload);calls.push(action);let out={};if(action==='start')out={pulse:20000};if(action==='offer'||action==='chatOffer'){assert.match(p.sdp,/b=TIAS:5000000/);out={sdp:'answer',chatStream:mode==='unsupported'?0:1};}if(action==='ice')out=[{candidate:'candidate:1 1 UDP 123 192.0.2.1 12345 typ host',sdpMLineIndex:'0',sdpMid:'0'}];queueMicrotask(()=>sandbox.settle(id,out,null));},voice(s){statuses.push(s);},status(s){statuses.push(s);},quality(w,h){stats.push("quality:"+w+"x"+h);},stats(s){stats.push(s);},connected(){},error(s){throw new Error(s);}};
 vm.createContext(sandbox);vm.runInContext(source,sandbox);
 return {sandbox,sent,calls,intervals,Peer,statuses,stats,statsCalls:()=>statsCalls,media,captures:()=>captures,stops:()=>stops,release:()=>pendingCapture()};
}
async function drain(){for(let i=0;i<40;i++)await Promise.resolve();}
async function start(f,microphone,voiceStats=false,height=720){await f.sandbox.boot({title:'test',height,kbps:5000,codec:'H264',stats:false,microphone,voiceStats});f.Peer.instance.channels.message.onopen();f.Peer.instance.channels.message.onmessage({data:JSON.stringify({type:'HandshakeAck',version:'messageV1'})});await drain();}
(async()=>{
 const f=fixture();await start(f,false);assert.deepEqual(f.calls,['start','offer','ice']);assert.equal(f.captures(),0);assert.equal(f.Peer.instance.transceivers[0].direction,'recvonly');assert.equal(f.intervals.length,2);
 assert.ok(f.sent.some(s=>s.name==='control'&&typeof s.data==='string'&&s.data.includes('authorizationRequest')));
 let packets=f.sent.filter(s=>s.data instanceof ArrayBuffer);assert.equal(packets[0].data.byteLength,15);
 f.sandbox.pad={buttons:16,axes:[1,-1,0,0,0,1]};f.intervals[f.intervals.length-1]();
 packets=f.sent.filter(s=>s.data instanceof ArrayBuffer);const b=new DataView(packets[packets.length-1].data);assert.equal(b.byteLength,38);assert.equal(b.getUint16(16,true),16);assert.equal(b.getInt16(18,true),32767);assert.equal(b.getInt16(20,true),-32768);assert.equal(b.getUint16(28,true),65535);
 let remoteStops=0;const gameAudio={kind:'audio',id:'remote-a',enabled:false,stop(){remoteStops++;}},gameVideo={kind:'video',id:'remote-v',stop(){remoteStops++;}};
 f.Peer.instance.ontrack({track:gameAudio});f.Peer.instance.ontrack({track:gameVideo});await drain();assert.equal(f.media.audio.muted,false);assert.equal(f.media.audio.volume,1);assert.equal(gameAudio.enabled,true);assert.equal(f.media.audio.srcObject.getTracks()[0],gameAudio);assert.equal(f.media.video.srcObject.getTracks()[0],gameVideo);assert.equal(f.media.video.muted,true);assert.ok(f.media.audio.plays>0);const extraAudio={kind:'audio',id:'remote-chat',stop(){remoteStops++;}};f.Peer.instance.ontrack({track:extraAudio});assert.ok(f.media.audio.srcObject.getTracks().includes(gameAudio));assert.ok(f.media.audio.srcObject.getTracks().includes(extraAudio));
 const plays=f.media.audio.plays;f.sandbox.resumeMedia();f.sandbox.resumeMedia();assert.equal(f.media.audio.plays,plays);
 const report=new Map([['transport',{type:'transport',selectedCandidatePairId:'live'}],['old',{type:'candidate-pair',nominated:true,state:'succeeded',currentRoundTripTime:9}],['live',{type:'candidate-pair',currentRoundTripTime:.037}]]);assert.equal(f.sandbox.ping(report),'37');assert.equal(f.sandbox.ping(new Map()),'—');report.get('live').currentRoundTripTime=NaN;assert.equal(f.sandbox.ping(report),'—');report.get('live').currentRoundTripTime=.037;report.set('video',{id:'v',type:'inbound-rtp',kind:'video',frameWidth:1280,frameHeight:720,timestamp:100,framesDecoded:20});f.sandbox.videoReport(report);assert.ok(f.stats.some(s=>s.includes('Ping 37 ms')));
 f.media.video.videoWidth=1280;f.media.video.videoHeight=720;f.sandbox.receivedSize();assert.ok(f.stats.includes('quality:1280x720'));
 f.sandbox.shutdown();assert.equal(f.Peer.instance.closed,true);assert.equal(remoteStops,3);assert.equal(f.media.audio.srcObject,null);assert.equal(f.media.video.srcObject,null);
 const fullhd=fixture();await start(fullhd,false,false,1080);const dimensionPacket=fullhd.sent.find(s=>s.name==='message'&&typeof s.data==='string'&&s.data.includes('dimensionschanged'));assert.ok(dimensionPacket);const dimensions=JSON.parse(JSON.parse(dimensionPacket.data).content);assert.equal(dimensions.preferredWidth,1920);assert.equal(dimensions.preferredHeight,1080);fullhd.sandbox.shutdown();
 const enabled=fixture('on');await start(enabled,true);assert.equal(enabled.captures(),1);assert.deepEqual(enabled.calls,['start','offer','ice','chatOffer']);assert.equal(enabled.Peer.instance.transceivers[0].direction,'sendrecv');assert.ok(enabled.Peer.instance.addedTrack);assert.equal(enabled.intervals.length,2);await enabled.sandbox.collectStats({microphone:true,voiceStats:true,stats:false});assert.ok(enabled.statuses.some(s=>s.includes('12 pacotes')));enabled.sandbox.shutdown();assert.equal(enabled.stops(),1);
 const info=fixture('on');await start(info,true,true);assert.equal(info.intervals.length,3);let before=info.statsCalls();await info.intervals[0]();await info.sandbox.collectStats({microphone:true,voiceStats:true,stats:true});assert.equal(info.statsCalls(),before+1);info.sandbox.shutdown();
 const denied=fixture('denied');await start(denied,true);assert.equal(denied.calls.includes('chatOffer'),false);assert.ok(denied.statuses.some(s=>s.includes('falha na negociação')));assert.equal(denied.Peer.instance.closed,undefined);denied.sandbox.shutdown();
 const unsupported=fixture('unsupported');await start(unsupported,true);assert.equal(unsupported.captures(),0);assert.equal(unsupported.calls.includes('chatOffer'),false);assert.ok(unsupported.statuses.some(s=>s.includes('chatStream')));unsupported.sandbox.shutdown();
 const pending=fixture('pending');await start(pending,true);assert.equal(pending.captures(),1);pending.sandbox.shutdown();pending.release();await drain();assert.equal(pending.stops(),1);assert.equal(pending.calls.includes('chatOffer'),false);
 console.log('PASS: SDP/ICE, áudio recebido separado do vídeo e não mudo, controle, voz opcional, RTT/ping, resolução 720p/1080p, coleta única e liberação');
})().catch(e=>{console.error(e);process.exitCode=1;});
