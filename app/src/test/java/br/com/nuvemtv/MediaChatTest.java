package br.com.nuvemtv;
import org.junit.*;import static org.junit.Assert.*;
public class MediaChatTest {
 @Test public void doesNotReuseStaleSdpForMicrophoneNegotiation(){assertFalse(Cloud.matchesSdp(Json.obj("requestId",1,"sdp","answer"),2,"old"));assertTrue(Cloud.matchesSdp(Json.obj("requestId",2,"sdp","answer"),2,"old"));assertFalse(Cloud.matchesSdp(Json.obj("sdp","old"),2,"old"));assertTrue(Cloud.matchesSdp(Json.obj("requestId",0,"sdp","new"),2,"old"));}
}
