package br.com.nuvemtv;
import java.nio.*;import java.util.*;
// Formato de pacotes adaptado de XStreaming src/webrtc/Packet (AGPL-3.0).
final class Wire {
 static ByteBuffer packet(int seq,double time,int buttons,float[] axes,boolean metadata){
  ByteBuffer b=ByteBuffer.allocateDirect(metadata?15:38).order(ByteOrder.LITTLE_ENDIAN);
  b.putShort((short)(metadata?8:2)).putInt(seq).putDouble(time);
  if(metadata)b.put((byte)0);else {b.put((byte)1).put((byte)0).putShort((short)buttons);for(int i=0;i<4;i++)b.putShort((short)Math.round(Math.max(-1,Math.min(1,axes[i]))*(axes[i]<0?32768:32767)));for(int i=4;i<6;i++)b.putShort((short)Math.round(Math.max(0,Math.min(1,axes[i]))*65535));b.putInt(0x3fffff).putInt(0);}
  b.flip();return b;
 }
 static String limitSdp(String sdp,int kbps){String[] sections=sdp.split("(?=m=)");StringBuilder out=new StringBuilder();for(String s:sections){if(s.startsWith("m=video ")){s=s.replaceAll("(?m)^b=(AS|TIAS):[^\r\n]*\r?\n","");int at=s.indexOf("\r\n");if(at>=0)s=s.substring(0,at+2)+"b=AS:"+kbps+"\r\nb=TIAS:"+(kbps*1000)+"\r\n"+s.substring(at+2);}out.append(s);}return out.toString();}
}
