package br.com.nuvemtv;
import org.junit.Test;import java.nio.*;import static org.junit.Assert.*;
public class WireTest {
 @Test public void metadataLayout(){ByteBuffer b=Wire.packet(123,4.5,0,new float[6],true);assertEquals(15,b.remaining());assertEquals(8,b.getShort());assertEquals(123,b.getInt());assertEquals(4.5,b.getDouble(),0);assertEquals(0,b.get());}
 @Test public void gamepadLayoutAndClamp(){ByteBuffer b=Wire.packet(7,10,16,new float[]{1,-1,2,-2,0,1},false);assertEquals(38,b.remaining());assertEquals(2,b.getShort());assertEquals(7,b.getInt());b.position(14);assertEquals(1,b.get());assertEquals(0,b.get());assertEquals(16,b.getShort());assertEquals(32767,b.getShort());assertEquals(-32768,b.getShort());assertEquals(32767,b.getShort());assertEquals(-32768,b.getShort());assertEquals(0,Short.toUnsignedInt(b.getShort()));assertEquals(65535,Short.toUnsignedInt(b.getShort()));assertEquals(0x3fffff,b.getInt());assertEquals(0,b.getInt());}
 @Test public void bitrateTouchesOnlyVideo(){String s="v=0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nb=AS:64\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\nb=AS:9000\r\na=recvonly\r\n";String out=Wire.limitSdp(s,3000);assertTrue(out.contains("b=AS:64"));assertTrue(out.contains("b=AS:3000"));assertTrue(out.contains("b=TIAS:3000000"));assertFalse(out.contains("9000"));assertEquals(out,Wire.limitSdp(out,3000));}
}
