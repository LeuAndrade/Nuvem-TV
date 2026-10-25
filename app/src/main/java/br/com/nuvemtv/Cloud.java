package br.com.nuvemtv;
import org.json.*;import java.util.*;
final class Cloud {
 final Auth auth; final String host;String locale="pt-BR",market="BR"; volatile String session="";volatile boolean stopped;int sdpRequest;String lastSdp="";long pulse=20000;
 Cloud(Auth a,String region)throws Exception{auth=a;host=a.host(region);}
 JSONObject get(String path)throws Exception{auth.ensure();return Net.json("GET",host+path,auth.gs,null);}
 JSONArray titles(boolean recent)throws Exception {auth.ensure();String raw=Net.request("GET",host+"/v2/titles"+(recent?"/mru?mr=25":""),auth.gs,null,"application/json",null);if(raw.trim().startsWith("["))return new JSONArray(raw);JSONObject j=Json.parse(raw);JSONArray a=j.optJSONArray("results");if(a==null)a=j.optJSONArray("titles");if(a==null)throw new Exception("Formato de catálogo não reconhecido");return a;}
 JSONArray allTitles()throws Exception {
  JSONArray out=titles(false);Set<String> seen=new HashSet<>();for(int i=0;i<out.length();i++){JSONObject d=out.getJSONObject(i).optJSONObject("details");if(d!=null)seen.add(d.optString("productId"));}
  // Listas públicas Microsoft: geral, Game Pass, compre e transmita, gratuitos.
  for(String sigl:new String[]{"29a81209-df6f-41fd-a528-2ae6b91f719c","af206485-e87d-4624-9007-cb7f6d0cc42e","e4c1d680-2c70-45e4-a38d-8a292c68c700","51f14e5d-bdcb-4e04-b9cb-76e5057702df"}){
   try{JSONArray ids=new JSONArray(Net.request("GET","https://catalog.gamepass.com/sigls/v2?id="+sigl+"&market=BR&language=pt-BR",null,null,"application/json",null));for(int i=0;i<ids.length();i++){String id=ids.getJSONObject(i).optString("id");if(!id.isEmpty()&&seen.add(id))out.put(Json.obj("details",Json.obj("productId",id)));}}catch(Net.HttpError ignored){/* Uma lista removida não apaga o catálogo autenticado. */}
  }return out;
 }
 JSONArray catalog(JSONArray titles)throws Exception{return catalog(titles,false);}
 JSONArray catalog(JSONArray titles,boolean includeNonCloud)throws Exception {
  JSONArray result=new JSONArray();for(int offset=0;offset<titles.length();offset+=40){JSONArray ids=new JSONArray();Map<String,JSONObject> map=new HashMap<>();
   for(int i=offset;i<Math.min(offset+40,titles.length());i++){JSONObject t=titles.getJSONObject(i);String id=t.optJSONObject("details")==null?t.optString("productId"):t.getJSONObject("details").optString("productId");if(!id.isEmpty()){ids.put(id);map.put(id,t);}}
   if(ids.length()==0)continue;
   JSONObject products=Net.json("POST","https://catalog.gamepass.com/v3/products?market="+market+"&language="+locale+"&hydration=RemoteLowJade0",null,Json.obj("Products",ids)).getJSONObject("Products");
   for(int productIndex=0;productIndex<ids.length();productIndex++){String id=ids.getString(productIndex);JSONObject p=products.optJSONObject(id);if(p==null)continue;JSONObject t=map.get(id);if(t==null)continue;String title=t.optString("titleId");if(title.isEmpty())title=p.isNull("XCloudTitleId")?"":p.optString("XCloudTitleId");if(!title.isEmpty()||includeNonCloud){JSONObject game=Catalog.product(p,id,title);JSONObject details=t.optJSONObject("details");if(details!=null&&details.has("hasEntitlement"))game.put("hasEntitlement",details.getBoolean("hasEntitlement"));result.put(game);}}
  }return result;
 }

 JSONObject feed(JSONObject previous)throws Exception{
  JSONObject result=HomeFeed.fetch(previous,id->new JSONArray(Net.request("GET","https://catalog.gamepass.com/sigls/v2?id="+id+"&market="+market+"&language="+locale,null,null,"application/json",null)),System.currentTimeMillis());
  try{String html=Net.request("GET","https://www.xbox.com/"+locale+"/play",null,null,"text/html",null);result.put("promotions",HomeFeed.promotions(html));}catch(Exception error){JSONArray old=previous.optJSONArray("promotions");if(old!=null)result.put("promotions",old);result.put("partial",true);result.put("updatedAt",previous.optLong("updatedAt"));}return result;
 }
 JSONArray withFeed(JSONArray all,JSONObject feed)throws Exception{Set<String> seen=new HashSet<>();for(int i=0;i<all.length();i++){JSONObject t=all.getJSONObject(i),d=t.optJSONObject("details");seen.add(d==null?t.optString("productId"):d.optString("productId"));}JSONObject limited=Json.obj("added",feed.optJSONArray("added"),"leaving",feed.optJSONArray("leaving"));JSONArray extra=HomeFeed.titles(limited);for(int i=0;i<extra.length();i++){JSONObject t=extra.getJSONObject(i);if(seen.add(t.getJSONObject("details").getString("productId")))all.put(t);}return all;}
 JSONObject chatOffer(String sdp)throws Exception{int request=++sdpRequest;call("POST","sdp",Json.obj("messageType","offer","requestId",request,"sdp",sdp,"configuration",Json.obj("isMediaStreamsChatRenegotiation",true)));return awaitSdp(request);}
 static boolean matchesSdp(JSONObject answer,int request,String previous){int responseId=answer.optInt("requestId",0);return responseId>0?responseId==request:!answer.optString("sdp").isEmpty()&&!answer.optString("sdp").equals(previous);}
 JSONObject awaitSdp(int request)throws Exception{long end=System.currentTimeMillis()+60000;while(!stopped&&System.currentTimeMillis()<end){JSONObject response=call("GET","sdp",null);String raw=response.optString("exchangeResponse");if(!raw.isEmpty()){JSONObject answer=Json.parse(raw);boolean matches=matchesSdp(answer,request,lastSdp);if(matches){if(answer.has("errorInfo"))throw new Exception("Servidor recusou SDP: "+answer.getJSONObject("errorInfo").optString("RtcErrorDetails"));lastSdp=answer.optString("sdp");return answer;}}Thread.sleep(300);}throw new Exception("Tempo esgotado na negociação de áudio/SDP");}
 JSONObject call(String method,String resource,JSONObject body)throws Exception{if(stopped)throw new Exception("Sessão encerrada");auth.ensure();String url=session+(resource.isEmpty()?"":"/"+resource);String raw=Net.request(method,url,auth.gs,body==null?null:body.toString(),"application/json",null);return raw.trim().isEmpty()?new JSONObject():Json.parse(raw);}
 JSONObject waitExchange(String path)throws Exception{long end=System.currentTimeMillis()+60000;while(!stopped&&System.currentTimeMillis()<end){JSONObject j=call("GET",path,null);if(!j.optString("exchangeResponse").isEmpty())return j;Thread.sleep(500);}throw new Exception("Tempo esgotado na negociação "+path);}
 void start(String title,int height,java.util.function.Consumer<String> status)throws Exception{
  String os=height<=720?"android":"windows";
  JSONObject device=Json.obj("appInfo",Json.obj("env",Json.obj("clientAppId","www.xbox.com","clientAppType","browser","clientAppVersion","29.9.35","clientSdkVersion","10.6.8","httpEnvironment","prod","sdkInstallId","")),"dev",Json.obj("hw",Json.obj("make","Microsoft","model","unknown","platformType","desktop","sdktype","web"),"os",Json.obj("name",os,"ver","22631.2715","platform","desktop"),"displayInfo",Json.obj("dimensions",Json.obj("widthInPixels",height*16/9,"heightInPixels",height),"pixelDensity",Json.obj("dpiX",1,"dpiY",1)),"browser",Json.obj("browserName","edge","browserVersion","140.0.3485.66")));
  JSONObject b=Json.obj("clientSessionId",UUID.randomUUID().toString(),"titleId",title,"systemUpdateGroup","","serverId","","fallbackRegionNames",new JSONArray(),"settings",Json.obj("nanoVersion","V3;WebrtcTransport.dll","enableTextToSpeech",false,"highContrast",0,"locale",locale,"useIceConnection",false,"timezoneOffsetMinutes",TimeZone.getDefault().getOffset(System.currentTimeMillis())/60000,"sdkType","web","osName",os));
  JSONObject r=Json.parse(Net.request("POST",host+"/v5/sessions/cloud/play",auth.gs,b.toString(),"application/json",device.toString()));
  String path=r.getString("sessionPath");String created=Net.cloudUrl(path.startsWith("https://")?path:host+"/"+path.replaceAll("^/+",""));session=created;if(stopped){try{Net.request("DELETE",created,auth.gs,null,"application/json",null);}finally{throw new Exception("Sessão cancelada");}}
  long end=System.currentTimeMillis()+15*60*1000;
  while(!stopped&&System.currentTimeMillis()<end){JSONObject state=call("GET","state",null);String s=state.optString("state");status.accept(s);
   if(!state.optString("transferUri").isEmpty()){String target=Net.cloudUrl(state.getString("transferUri"));session=target+java.net.URI.create(session).getPath();}
   if(s.equals("Provisioned")){JSONObject cfg=call("GET","configuration",null);pulse=Math.max(5000,cfg.optLong("keepAlivePulseInSeconds",20)*1000);return;}
   if(s.equals("ReadyToConnect"))call("POST","connect",Json.obj("userToken",auth.transfer()));
   else if(!s.equals("WaitingForResources")&&!s.equals("Provisioning"))throw new Exception("Streaming: "+s+" "+state.optJSONObject("errorDetails"));
   Thread.sleep(1000);
  }throw new Exception("Sessão cancelada ou fila expirou");
 }
 JSONObject offer(String sdp)throws Exception{int request=++sdpRequest;call("POST","sdp",Json.obj("messageType","offer","requestId",request,"sdp",sdp,"configuration",Json.obj("chatStream",Json.obj("minVersion",1,"maxVersion",1),"chatConfiguration",Json.obj("bytesPerSample",2,"expectedClipDurationMs",20,"format",Json.obj("codec","opus","container","webm"),"numChannels",1,"sampleFrequencyHz",24000),"chat",Json.obj("minVersion",1,"maxVersion",1),"control",Json.obj("minVersion",1,"maxVersion",3),"input",Json.obj("minVersion",1,"maxVersion",8),"message",Json.obj("minVersion",1,"maxVersion",1))));return awaitSdp(request);}
 JSONArray ice(JSONArray candidates)throws Exception{call("POST","ice",Json.obj("messageType","iceCandidate","candidate",candidates));return new JSONArray(waitExchange("ice").getString("exchangeResponse"));}
 void stop(){stopped=true;String old=session;session="";if(!old.isEmpty())try{Net.request("DELETE",old,auth.gs,null,"application/json",null);}catch(Exception ignored){}}
}
