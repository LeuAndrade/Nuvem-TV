package br.com.nuvemtv;
import org.json.JSONObject;import java.net.URI;import java.util.*;
final class AvatarUrl {
 static String from(JSONObject user){for(String key:new String[]{"displayPicRaw","gameDisplayPicRaw","GameDisplayPicRaw","gamerpic","picture"}){String url=resize(user.optString(key));if(!url.isEmpty())return url;}return "";}
 static String resize(String value){try{
  if(value.startsWith("//"))value="https:"+value;URI uri=URI.create(value);String host=uri.getHost();if(host==null||uri.getUserInfo()!=null||uri.getPort()!=-1)return "";host=host.toLowerCase(Locale.ROOT);
  if(!(host.endsWith(".xboxlive.com")||host.endsWith(".xboxservices.com")||host.endsWith(".s-microsoft.com")||host.equals("res.public.onecdn.static.microsoft")))return "";
  if(!uri.getScheme().equals("https")&&!uri.getScheme().equals("http"))return "";
  List<String> query=new ArrayList<>();String raw=uri.getRawQuery();if(raw!=null)for(String part:raw.split("&")){String key=part.split("=",2)[0].toLowerCase(Locale.ROOT);if(!Arrays.asList("w","h","width","height").contains(key))query.add(part);}
  query.add("w=96");query.add("h=96");return URI.create("https://"+host+(uri.getRawPath()==null?"":uri.getRawPath())+"?"+String.join("&",query)).toASCIIString();
 }catch(Exception ignored){return "";}}
}
