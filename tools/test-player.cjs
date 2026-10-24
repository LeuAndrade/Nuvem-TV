'use strict';
const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const sent=[],calls=[],intervals=[],timeouts=[];
const video={srcObject:null,play:()=>Promise.resolve()};
class Peer {
 static instance;
 constructor(){Peer.instance=this;this.iceGatheringState='complete';this.connectionState='connected';this.channels={};}
 addTransceiver(kind){return {setCodecPreferences:codecs=>assert.equal(codecs[0].mimeType,'video/H264')};}
 createDataChannel(name,config){assert.ok(config.protocol);const d={readyState:'open',bufferedAmount:0,send:data=>sent.push({name,data}),close(){}};this.channels[name]=d;return d;}
 async createOffer(){return {type:'offer',sdp:'v=0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\na=recvonly\r\n'};}
 async setLocalDescription(offer){assert.match(offer.sdp,/b=AS:5000/);}
 async setRemoteDescription(answer){assert.equal(answer.type,'answer');}
 async addIceCandidate(c){assert.equal(typeof c.sdpMLineIndex,'number');}
 close(){this.closed=true;}
}
const sandbox={console,performance:{now:()=>123},ArrayBuffer,DataView,Math,Date,JSON,Number,String,Error,Promise,RTCPeerConnection:Peer,RTCRtpReceiver:{getCapabilities:()=>({codecs:[{mimeType:'video/H264'}]})},document:{getElementById:()=>video},MediaStream:class{addTrack(){}},setTimeout:f=>(timeouts.push(f),timeouts.length),setInterval:f=>(intervals.push(f),intervals.length),clearTimeout(){},clearInterval(){}};
sandbox.window=sandbox;
sandbox.CloudBridge={call(id,action,payload){const p=JSON.parse(payload);calls.push(action);let out={};if(action==='start')out={pulse:20000};if(action==='offer'){assert.match(p.sdp,/b=TIAS:5000000/);out={sdp:'answer'};}if(action==='ice')out=[{candidate:'candidate:1 1 UDP 123 192.0.2.1 12345 typ host',sdpMLineIndex:'0',sdpMid:'0'}];queueMicrotask(()=>sandbox.settle(id,out,null));},status(){},stats(){},connected(){},error(s){throw new Error(s);}};
vm.createContext(sandbox);vm.runInContext(fs.readFileSync('app/src/main/assets/player.js','utf8'),sandbox);
(async()=>{
 await sandbox.boot({title:'test',height:720,kbps:5000,codec:'H264',stats:false});
 assert.deepEqual(calls,['start','offer','ice']);
 Peer.instance.channels.message.onopen();
 Peer.instance.channels.message.onmessage({data:JSON.stringify({type:'HandshakeAck',version:'messageV1'})});
 assert.ok(sent.some(s=>s.name==='control'&&typeof s.data==='string'&&s.data.includes('authorizationRequest')));
 let packets=sent.filter(s=>s.data instanceof ArrayBuffer);assert.equal(packets[0].data.byteLength,15);
 sandbox.pad={buttons:16,axes:[1,-1,0,0,0,1]};intervals[intervals.length-1]();
 packets=sent.filter(s=>s.data instanceof ArrayBuffer);const b=new DataView(packets[packets.length-1].data);assert.equal(b.byteLength,38);assert.equal(b.getUint16(16,true),16);assert.equal(b.getInt16(18,true),32767);assert.equal(b.getInt16(20,true),-32768);assert.equal(b.getUint16(28,true),65535);
 sandbox.shutdown();assert.equal(Peer.instance.closed,true);
 console.log('PASS: boot, SDP, ICE, handshake, pacote e encerramento WebView');
})().catch(e=>{console.error(e);process.exitCode=1;});
