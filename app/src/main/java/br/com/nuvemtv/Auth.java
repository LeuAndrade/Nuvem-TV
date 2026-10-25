package br.com.nuvemtv;

import android.content.Context;
import org.json.*;

// Fluxo de interoperabilidade adaptado de Geocld/XStreaming, AGPL-3.0.
final class Auth {
    static final String DEFAULT_CLIENT = "1f907974-e22b-4810-a9de-d9647380c97e";
    static final String ROOT = "https://login.microsoftonline.com/consumers/oauth2/v2.0/";
    static final String SCOPE = "xboxlive.signin openid profile offline_access";

    interface CredentialStore {
        boolean exists();
        String load() throws Exception;
        void save(String value) throws Exception;
        void clear();
    }
    interface Http {
        JSONObject form(String url, Object... pairs) throws Exception;
        JSONObject json(String method, String url, String token, JSONObject body) throws Exception;
    }
    static final class SessionExpired extends Exception {
        SessionExpired() { super("A sessão foi desconectada pela Microsoft. Entre novamente."); }
    }
    private static final class NetworkHttp implements Http {
        public JSONObject form(String url, Object... pairs) throws Exception { return Net.postForm(url, pairs); }
        public JSONObject json(String method, String url, String token, JSONObject body) throws Exception {
            return Net.json(method, url, token, body);
        }
    }

    final CredentialStore vault;
    final Http http;
    final String client;
    volatile boolean cancelled;
    String refresh = "", access = "", gs = "";
    long expires;
    JSONObject login;

    Auth(Context context, String id) { this(new Vault(context), new NetworkHttp(), id); }
    Auth(CredentialStore vault, Http http, String id) {
        this.vault = vault;
        this.http = http;
        client = id.trim().isEmpty() ? DEFAULT_CLIENT : id.trim();
    }
    boolean hasSavedSession() { return vault.exists(); }

    JSONObject device() throws Exception {
        cancelled = false;
        return http.form(ROOT + "devicecode", "client_id", client, "scope", SCOPE);
    }
    void poll(JSONObject device) throws Exception {
        long end = System.currentTimeMillis() + device.getLong("expires_in") * 1000;
        int interval = device.optInt("interval", 5);
        while (!cancelled && System.currentTimeMillis() < end) {
            Thread.sleep(interval * 1000L);
            if (cancelled) break;
            try {
                tokens(http.form(ROOT + "token", "client_id", client, "grant_type",
                        "urn:ietf:params:oauth:grant-type:device_code", "device_code", device.getString("device_code")));
                return;
            } catch (Net.HttpError error) {
                if (error.code.equals("slow_down")) interval += 5;
                else if (!error.code.equals("authorization_pending")) throw error;
            }
        }
        throw new Exception(cancelled ? "Login cancelado" : "Código expirado");
    }
    synchronized void tokens(JSONObject token) throws Exception {
        access = token.getString("access_token");
        refresh = token.optString("refresh_token", refresh);
        expires = System.currentTimeMillis() + token.optInt("expires_in", 3600) * 1000L;
        if (!refresh.isEmpty()) vault.save(refresh);
    }
    synchronized void ensure() throws Exception {
        if (System.currentTimeMillis() + 120000 < expires && !gs.isEmpty()) return;
        if (access.isEmpty() || System.currentTimeMillis() + 120000 >= expires) {
            if (refresh.isEmpty()) refresh = vault.load();
            if (refresh.isEmpty()) throw new SessionExpired();
            try {
                tokens(http.form(ROOT + "token", "client_id", client, "grant_type", "refresh_token",
                        "refresh_token", refresh, "scope", SCOPE));
            } catch (Net.HttpError error) {
                // Uma falha de rede, GSSV ou região não apaga uma sessão salva.
                if (error.code.equals("invalid_grant")) {
                    logout();
                    throw new SessionExpired();
                }
                throw error;
            }
        }
        JSONObject user = http.json("POST", "https://user.auth.xboxlive.com/user/authenticate", null,
                Json.obj("Properties", Json.obj("AuthMethod", "RPS", "RpsTicket", "d=" + access,
                        "SiteName", "user.auth.xboxlive.com"), "RelyingParty", "http://auth.xboxlive.com", "TokenType", "JWT"));
        JSONObject xsts = http.json("POST", "https://xsts.auth.xboxlive.com/xsts/authorize", null,
                Json.obj("Properties", Json.obj("SandboxId", "RETAIL", "UserTokens", Json.arr(user.getString("Token"))),
                        "RelyingParty", "http://gssv.xboxlive.com/", "TokenType", "JWT"));
        try { login = streamLogin(xsts.getString("Token"), "xgpuweb"); }
        catch (Net.HttpError error) {
            if (error.status != 403 && error.status != 401) throw error;
            login = streamLogin(xsts.getString("Token"), "xgpuwebf2p");
        }
        gs = login.getString("gsToken");
    }
    JSONObject streamLogin(String token, String offering) throws Exception {
        return http.json("POST", "https://" + offering + ".gssv-play-prod.xboxlive.com/v2/login/user", null,
                Json.obj("token", token, "offeringId", offering));
    }
    JSONArray regions() throws Exception { ensure(); return login.getJSONObject("offeringSettings").getJSONArray("regions"); }
    String host(String name) throws Exception {
        JSONArray regions = regions();
        JSONObject chosen = null;
        for (int i = 0; i < regions.length(); i++) {
            JSONObject region = regions.getJSONObject(i);
            if (chosen == null || region.optBoolean("isDefault")) chosen = region;
            if (region.optString("name").equals(name)) return Net.cloudUrl(region.getString("baseUri"));
        }
        if (chosen == null) throw new Exception("Conta sem região disponível");
        return Net.cloudUrl(chosen.getString("baseUri"));
    }
    String transfer() throws Exception {
        JSONObject result = http.form("https://login.live.com/oauth20_token.srf", "client_id", client,
                "grant_type", "refresh_token", "refresh_token", refresh, "scope",
                "service::http://Passport.NET/purpose::PURPOSE_XBOX_CLOUD_CONSOLE_TRANSFER_TOKEN");
        return result.getString("access_token");
    }
    String collectionsAuthorization() throws Exception {return xboxAuthorization("http://licensing.xboxlive.com");}
    String xboxAuthorization(String relyingParty) throws Exception {
        ensure();
        JSONObject user=http.json("POST","https://user.auth.xboxlive.com/user/authenticate",null,
            Json.obj("Properties",Json.obj("AuthMethod","RPS","RpsTicket","d="+access,"SiteName","user.auth.xboxlive.com"),"RelyingParty","http://auth.xboxlive.com","TokenType","JWT"));
        JSONObject token=http.json("POST","https://xsts.auth.xboxlive.com/xsts/authorize",null,
            Json.obj("Properties",Json.obj("SandboxId","RETAIL","UserTokens",Json.arr(user.getString("Token"))),"RelyingParty",relyingParty,"TokenType","JWT"));
        String uhs=token.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0).getString("uhs");
        return "XBL3.0 x="+uhs+";"+token.getString("Token");
    }
    synchronized void logout() {
        cancelled = true;
        vault.clear();
        gs = ""; access = ""; refresh = ""; expires = 0; login = null;
    }
}
