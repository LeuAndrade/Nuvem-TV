package br.com.nuvemtv;
import org.junit.Test;import static org.junit.Assert.*;
public class NetTest {
 @Test public void acceptsXboxStreamingHost(){assertEquals("https://brazilsouth.gssv-play-prod.xboxlive.com",Net.cloudUrl("https://brazilsouth.gssv-play-prod.xboxlive.com/"));}
 @Test public void rejectsHostSpoofing(){for(String value:new String[]{"http://safe.xboxlive.com","https://safe.xboxlive.com.evil.test","https://evil.test/?safe.xboxlive.com","https://user@safe.xboxlive.com","https://safe.xboxlive.com:444"}){try{Net.cloudUrl(value);fail(value);}catch(IllegalArgumentException expected){}}}
}
