package br.com.nuvemtv;
import org.junit.*;import static org.junit.Assert.*;
public class AvatarUrlTest {
 @Test public void preservesImageIdentityInMicrosoftQuery(){String original="https://images-eds-ssl.xboxlive.com/image?url=https%3A%2F%2Favatar-ssl.xboxlive.com%2Favatar%2F123%2Fpic.png&w=208&h=208";String sized=AvatarUrl.resize(original);assertTrue(sized.contains("url=https%3A%2F%2Favatar-ssl.xboxlive.com%2Favatar%2F123%2Fpic.png"));assertTrue(sized.endsWith("w=96&h=96"));assertFalse(sized.contains("w=208"));}
 @Test public void onlyUsesMicrosoftImageHosts(){for(String value:new String[]{"https://evil.example/p.png","https://avatar-ssl.xboxlive.com.evil.test/p.png","https://user@avatar-ssl.xboxlive.com/p.png","file:///etc/file","https://avatar-ssl.xboxlive.com:444/p.png"})assertEquals("",AvatarUrl.resize(value));assertTrue(AvatarUrl.resize("http://avatar.xboxlive.com/p.png").startsWith("https://"));}
 @Test public void resolvesProfileAndSearchPhotosConsistently(){String url="https://avatar-ssl.xboxlive.com/p.png";assertEquals(AvatarUrl.resize(url),AvatarUrl.from(Json.obj("displayPicRaw",url)));assertEquals(AvatarUrl.resize(url),AvatarUrl.from(Json.obj("GameDisplayPicRaw",url)));}
}
