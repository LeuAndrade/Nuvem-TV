package br.com.nuvemtv;
import android.annotation.SuppressLint;import android.media.*;
/** Teste local explícito: dois segundos, sem arquivo e sem transmissão. */
final class MicrophoneProbe {
 @SuppressLint("MissingPermission") static String run()throws Exception{
  AudioRecord record=null;int actualRate=0;
  outer:for(int source:new int[]{MediaRecorder.AudioSource.VOICE_COMMUNICATION,MediaRecorder.AudioSource.MIC})for(int rate:new int[]{48000,16000,24000}){
   int size=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(size<=0)continue;AudioRecord candidate=null;
   try{candidate=new AudioRecord.Builder().setAudioSource(source).setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(Math.max(size,rate/5)).build();if(candidate.getState()==AudioRecord.STATE_INITIALIZED){record=candidate;actualRate=rate;break outer;}}
   catch(SecurityException denied){throw denied;}catch(IllegalArgumentException|UnsupportedOperationException ignored){}
   if(candidate!=null)candidate.release();
  }
  if(record==null)throw new Exception("Não foi possível abrir a entrada de áudio em nenhuma taxa suportada");
  long samples=0;double sum=0;int peak=0;String route="";
  try{record.startRecording();short[] buffer=new short[1200];long end=android.os.SystemClock.elapsedRealtime()+2200;
   while(android.os.SystemClock.elapsedRealtime()<end&&!Thread.currentThread().isInterrupted()){int n=record.read(buffer,0,buffer.length,AudioRecord.READ_NON_BLOCKING);if(n<0)throw new Exception("Falha ao ler o microfone: "+n);if(n==0){Thread.sleep(15);continue;}for(int i=0;i<n;i++){int value=buffer[i];sum+=(double)value*value;peak=Math.max(peak,Math.abs(value));}samples+=n;}
   AudioDeviceInfo input=record.getRoutedDevice();if(input!=null)route=input.getProductName().toString();
  }finally{try{record.stop();}catch(Exception ignored){}record.release();}
  if(samples==0)throw new Exception("O Android não entregou amostras do microfone");return "Entrada: "+route+"\nTaxa capturada: "+actualRate+" Hz\nAmostras: "+samples+"\nNível médio: "+Math.round(Math.sqrt(sum/samples)*100/32768)+"%\nPico: "+Math.round(peak*100.0/32768)+"%\n"+(peak<80?"Sem voz detectável. Verifique a entrada, o mute do headset e a privacidade do Android.":"Captura local detectada. Isso não comprova que o jogo ou outra pessoa recebeu o áudio.");
 }
}
