package br.com.nuvemtv;

import org.junit.Test;
import org.json.*;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

public class AuthTest {
    static final class Store implements Auth.CredentialStore {
        String value="refresh-old";
        public boolean exists(){return !value.isEmpty();}
        public String load(){return value;}
        public void save(String text){value=text;}
        public void clear(){value="";}
    }
    static final class Http implements Auth.Http {
        List<String> urls=new ArrayList<>();Exception failure;
        public JSONObject form(String url,Object... fields)throws Exception{
            urls.add(url);if(failure!=null)throw failure;
            assertFalse(url.endsWith("devicecode"));
            Map<String,Object> pairs=new HashMap<>();for(int i=0;i<fields.length;i+=2)pairs.put(fields[i].toString(),fields[i+1]);
            assertEquals("refresh_token",pairs.get("grant_type"));
            return Json.obj("access_token","access","refresh_token","refresh-new","expires_in",3600);
        }
        public JSONObject json(String method,String url,String token,JSONObject body){
            urls.add(url);
            if(url.contains("user/authenticate"))return Json.obj("Token","user");
            if(url.contains("xsts/authorize"))return Json.obj("Token","xsts");
            return Json.obj("gsToken","stream");
        }
    }
    @Test public void restoresAcrossAppInstancesWithoutDeviceCode()throws Exception{
        Store store=new Store();Http first=new Http();Auth app=new Auth(store,first,Auth.DEFAULT_CLIENT);
        assertTrue(app.hasSavedSession());app.ensure();assertEquals("refresh-new",store.value);assertEquals("stream",app.gs);
        Http restarted=new Http();Auth next=new Auth(store,restarted,Auth.DEFAULT_CLIENT);next.ensure();
        assertTrue(next.hasSavedSession());assertFalse(restarted.urls.stream().anyMatch(url->url.endsWith("devicecode")));
    }
    @Test public void validSessionDoesNotRefreshOnEveryOperation()throws Exception{
        Http http=new Http();Auth auth=new Auth(new Store(),http,Auth.DEFAULT_CLIENT);auth.ensure();int calls=http.urls.size();auth.ensure();assertEquals(calls,http.urls.size());
    }
    @Test public void temporaryNetworkFailureKeepsSavedAccount()throws Exception{
        Store store=new Store();Http http=new Http();http.failure=new IOException("offline");Auth auth=new Auth(store,http,Auth.DEFAULT_CLIENT);
        try{auth.ensure();fail();}catch(IOException expected){}assertTrue(auth.hasSavedSession());assertEquals("refresh-old",store.value);
    }
    @Test public void revokedRefreshTokenDisconnectsAccount()throws Exception{
        Store store=new Store();Http http=new Http();http.failure=new Net.HttpError(400,"invalid_grant");Auth auth=new Auth(store,http,Auth.DEFAULT_CLIENT);
        try{auth.ensure();fail();}catch(Auth.SessionExpired expected){}assertFalse(auth.hasSavedSession());assertEquals("",auth.gs);
    }
    @Test public void explicitLogoutRemovesPersistedAndMemoryCredentials()throws Exception{
        Store store=new Store();Auth auth=new Auth(store,new Http(),Auth.DEFAULT_CLIENT);auth.ensure();auth.logout();
        assertFalse(auth.hasSavedSession());assertEquals("",auth.refresh);assertEquals("",auth.access);assertEquals("",auth.gs);
    }
}
