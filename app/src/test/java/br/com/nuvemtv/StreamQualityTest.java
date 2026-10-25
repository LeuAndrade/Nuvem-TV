package br.com.nuvemtv;
import org.json.*;import org.junit.*;import static org.junit.Assert.*;
public class StreamQualityTest {
 @Test public void selectedResolutionIsSentWithoutReduction()throws Exception{for(int height:new int[]{720,1080}){JSONObject device=Cloud.deviceInfo(height);JSONObject dev=device.getJSONObject("dev"),dimensions=dev.getJSONObject("displayInfo").getJSONObject("dimensions");assertEquals(height,dimensions.getInt("heightInPixels"));assertEquals(height*16/9,dimensions.getInt("widthInPixels"));assertEquals(height==1080?"windows":"android",dev.getJSONObject("os").getString("name"));}}
 @Test public void invalidResolutionIsNotSilentlyChanged(){try{Cloud.deviceInfo(480);fail();}catch(IllegalArgumentException expected){}}
}
