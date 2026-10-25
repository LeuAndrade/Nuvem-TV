'use strict';
const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const source=fs.readFileSync('app/src/main/assets/player.js','utf8');
function fixture(mode='off'){
 const sent=[],calls=[],intervals=[],timeouts=[],statuses=[];let captures=0,stops=0,pendingCapture;
 const track={kind:'audio',stop(){stops++;}},stream={getTracks:()=>[track],getAudioTracks:()=>[track]};
 const video={srcObject:null,play:()=>Promise.resolve()};
 class Peer {
  constructor(){Peer.instance=this;this.iceGatheringState='complete';this.connectionState='connected';this.signalingState='stable';this.channels={};this.transceivers=[];}
  addTransceiver(kind,config){const t={receiver:{track:{kind}},sender:{replaceTrack:async track=>{t.sender.track=track;}},direction:config.direction,setCodecPreferences:codecs=>assert.equal(codecs[0].mimeType,'video/H264')};this.transceivers.push(t);return t;}
  getTransceivers(){return this.transceivers;}
  createDataChannel(name,config){assert.ok(config.protocol);const d={readyState:'open',bufferedAmount:0,send:data=>sent.push({name,data}),close(){}};this.channels[name]=d;return d;}
  async createOffer(){return {type:'offer',sdp:'v=0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\na=recvonly\r\n'};}
  async setLocalDescription(offer){if(offer.type==='rollback'){this.signalingState='stable';return;}assert.match(offer.sdp,/b=AS:5000/);this.signalingState='have-local-offer';}
  async setRemoteDescription(answer){assert.equal(answer.type,'answer');this.signalingState='stable';}
  async addIceCandidate(c){assert.equal(typeof c.sdpMLineIndex,'number');}
  close(){this.closed=true;}
 }
 const sandbox={console,performance:{now:()=>123},ArrayBuffer,DataView,Math,Date,JSON,Number,String,Error,Promise,RTCPeerConnection:Peer,RTCRtpReceiver:{getCapabilities:()=>({codecs:[{mimeType:'video/H264'}]})},document:{getElementById:()=>video},MediaStream:class{addTrack(){}},navigator:{mediaDevices:{getUserMedia:async options=>{captures++;assert.equal(options.video,false);if(mode==='denied')throw new Error('NotAllowedError');if(mode==='pending')return new Promise(resolve=>pendingCapture=()=>resolve(stream));return stream;}}},setTimeout:f=>(timeouts.push(f),timeouts.length),setInterval:f=>(intervals.push(f),intervals.length),clearTimeout(){},clearInterval(){}};
 sandbox.window=sandbox;
 sandbox.CloudBridge={call(id,action,payload){const p=JSON.parse(payload);calls.push(action);let out={};if(action==='start')out={pulse:20000};if(action==='offer'||action==='chatOffer'){assert.match(p.sdp,/b=TIAS:5000000/);out={sdp:'answer'};}if(action==='ice')out=[{candidate:'candidate:1 1 UDP 123 192.0.2.1 12345 typ host',sdpMLineIndex:'0',sdpMid:'0'}];queueMicrotask(()=>sandbox.settle(id,out,null));},status(s){statuses.push(s);},stats(){},connected(){},error(s){throw new Error(s);}};
 vm.createContext(sandbox);vm.runInContext(source,sandbox);
 return {sandbox,sent,calls,intervals,Peer,statuses,captures:()=>captures,stops:()=>stops,release:()=>pendingCapture()};
}
async function drain(){for(let i=0;i<40;i++)await Promise.resolve();}
async function start(f,microphone){await f.sandbox.boot({title:'test',height:720,kbps:5000,codec:'H264',stats:false,microphone});f.Peer.instance.channels.message.onopen();f.Peer.instance.channels.message.onmessage({data:JSON.stringify({type:'HandshakeAck',version:'messageV1'})});await drain();}
(async()=>{
 const f=fixture();await start(f,false);assert.deepEqual(f.calls,['start','offer','ice']);assert.equal(f.captures(),0);
 assert.ok(f.sent.some(s=>s.name==='control'&&typeof s.data==='string'&&s.data.includes('authorizationRequest')));
 let packets=f.sent.filter(s=>s.data instanceof ArrayBuffer);assert.equal(packets[0].data.byteLength,15);
 f.sandbox.pad={buttons:16,axes:[1,-1,0,0,0,1]};f.intervals[f.intervals.length-1]();
 packets=f.sent.filter(s=>s.data instanceof ArrayBuffer);const b=new DataView(packets[packets.length-1].data);assert.equal(b.byteLength,38);assert.equal(b.getUint16(16,true),16);assert.equal(b.getInt16(18,true),32767);assert.equal(b.getInt16(20,true),-32768);assert.equal(b.getUint16(28,true),65535);
 f.sandbox.shutdown();assert.equal(f.Peer.instance.closed,true);
 const enabled=fixture('on');await start(enabled,true);assert.equal(enabled.captures(),1);assert.deepEqual(enabled.calls,['start','offer','ice','chatOffer']);assert.equal(enabled.Peer.instance.transceivers[0].direction,'sendrecv');enabled.sandbox.shutdown();assert.equal(enabled.stops(),1);
 const denied=fixture('denied');await start(denied,true);assert.equal(denied.calls.includes('chatOffer'),false);assert.ok(denied.statuses.some(s=>s.includes('Microfone indisponível')));assert.equal(denied.Peer.instance.closed,undefined);denied.sandbox.shutdown();
 const pending=fixture('pending');await start(pending,true);assert.equal(pending.captures(),1);pending.sandbox.shutdown();pending.release();await drain();assert.equal(pending.stops(),1);assert.equal(pending.calls.includes('chatOffer'),false);
 console.log('PASS: SDP/ICE, controle, microfone desligado/ligado/negado, renegociação e liberação inclusive captura pendente');
})().catch(e=>{console.error(e);process.exitCode=1;});
