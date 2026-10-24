package br.com.nuvemtv;
import org.json.*;import android.content.Context;
// Fluxo de interoperabilidade adaptado de Geocld/XStreaming, AGPL-3.0.
final class Auth {
 static final String DEFAULT_CLIENT="1f907974-e22b-4810-a9de-d9647380c97e";
 static final String ROOT="https://login.microsoftonline.com/consumers/oauth2/v2.0/";
 static final String SCOPE="xboxlive.signin openid profile offline_access";
 final Vault vault; final String client; volatile boolean cancelled;String refresh="",access="",gs="";long expires;JSONObject login;
 Auth(Context c,String id){vault=new Vault(c);client=id.trim().isEmpty()?DEFAULT_CLIENT:id.trim();}
 JSONObject device()throws Exception{cancelled=false;return Net.postForm(ROOT+"devicecode","client_id",client,"scope",SCOPE);}
 void poll(JSONObject d)throws Exception {long end=System.currentTimeMillis()+d.getLong("expires_in")*1000;int interval=d.optInt("interval",5);
  while(!cancelled&&System.currentTimeMillis()<end){Thread.sleep(interval*1000L);if(cancelled)break;try{tokens(Net.postForm(ROOT+"token","client_id",client,"grant_type","urn:ietf:params:oauth:grant-type:device_code","device_code",d.getString("device_code")));return;}catch(Net.HttpError e){if(e.code.equals("slow_down"))interval+=5;else if(!e.code.equals("authorization_pending"))throw e;}}
  throw new Exception(cancelled?"Login cancelado":"Código expirado");
 }
 void tokens(JSONObject t)throws Exception{access=t.getString("access_token");refresh=t.optString("refresh_token",refresh);expires=System.currentTimeMillis()+t.optInt("expires_in",3600)*1000L;if(!refresh.isEmpty())vault.save(refresh);}
 void ensure()throws Exception{
  if(System.currentTimeMillis()+120000<expires&&!gs.isEmpty())return;
  if(access.isEmpty()||System.currentTimeMillis()+120000>=expires){if(refresh.isEmpty())refresh=vault.load();if(refresh.isEmpty())throw new Exception("Entre na conta primeiro");tokens(Net.postForm(ROOT+"token","client_id",client,"grant_type","refresh_token","refresh_token",refresh,"scope",SCOPE));}
  JSONObject user=Net.json("POST","https://user.auth.xboxlive.com/user/authenticate",null,Json.obj("Properties",Json.obj("AuthMethod","RPS","RpsTicket","d="+access,"SiteName","user.auth.xboxlive.com"),"RelyingParty","http://auth.xboxlive.com","TokenType","JWT"));
  JSONObject xsts=Net.json("POST","https://xsts.auth.xboxlive.com/xsts/authorize",null,Json.obj("Properties",Json.obj("SandboxId","RETAIL","UserTokens",Json.arr(user.getString("Token"))),"RelyingParty","http://gssv.xboxlive.com/","TokenType","JWT"));
  try{login=streamLogin(xsts.getString("Token"),"xgpuweb");}catch(Net.HttpError e){if(e.status!=403&&e.status!=401)throw e;login=streamLogin(xsts.getString("Token"),"xgpuwebf2p");}gs=login.getString("gsToken");
 }
 JSONObject streamLogin(String t,String offering)throws Exception{return Net.json("POST","https://"+offering+".gssv-play-prod.xboxlive.com/v2/login/user",null,Json.obj("token",t,"offeringId",offering));}
 JSONArray regions()throws Exception{ensure();return login.getJSONObject("offeringSettings").getJSONArray("regions");}
 String host(String name)throws Exception{JSONArray a=regions();JSONObject chosen=null;for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);if(chosen==null||r.optBoolean("isDefault"))chosen=r;if(r.optString("name").equals(name))return Net.cloudUrl(r.getString("baseUri"));}if(chosen==null)throw new Exception("Conta sem região disponível");return Net.cloudUrl(chosen.getString("baseUri"));}
 String transfer()throws Exception{JSONObject r=Net.postForm("https://login.live.com/oauth20_token.srf","client_id",client,"grant_type","refresh_token","refresh_token",refresh,"scope","service::http://Passport.NET/purpose::PURPOSE_XBOX_CLOUD_CONSOLE_TRANSFER_TOKEN");return r.getString("access_token");}
 void logout(){cancelled=true;vault.clear();gs="";access="";refresh="";expires=0;login=null;}
}
