package br.com.nuvemtv;
import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import org.json.*;
final class Net {
 static final class HttpError extends IOException { final int status; final String code;
  HttpError(int s,String c){super("HTTP "+s+" ("+c+")");status=s;code=c;}}
 static String request(String method,String url,String token,String body,String content, String device) throws Exception {
  URI u=URI.create(url);if(!"https".equals(u.getScheme()))throw new IOException("Somente HTTPS permitido");
  HttpURLConnection c=(HttpURLConnection)u.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setRequestMethod(method);
  c.setRequestProperty("Accept","application/json");c.setRequestProperty("Origin","https://www.xbox.com");c.setRequestProperty("x-gssv-client","XboxComBrowser");
  if("catalog.gamepass.com".equals(u.getHost())){c.setRequestProperty("ms-cv","0");c.setRequestProperty("calling-app-name","Xbox Cloud Gaming Web");c.setRequestProperty("calling-app-version","24.17.63");}
  if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
  if(device!=null)c.setRequestProperty("X-MS-Device-Info",device);
  try {
   if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type",content);try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
   int status=c.getResponseCode();InputStream in=status>=200&&status<300?c.getInputStream():c.getErrorStream();String result="";
   if(in!=null)try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=stream.read(b))!=-1){out.write(b,0,n);if(out.size()>12*1024*1024)throw new IOException("Resposta grande demais");}result=out.toString("UTF-8");}
   if(status<200||status>=300){String code="resposta recusada";try{JSONObject j=new JSONObject(result);code=j.optString("error",j.optString("XErr",code));}catch(Exception ignored){}throw new HttpError(status,code);}
   return result;
  }finally{c.disconnect();}
 }
 static JSONObject json(String method,String url,String token,JSONObject body)throws Exception{return Json.parse(request(method,url,token,body==null?null:body.toString(),"application/json",null));}
 static String form(Object... p)throws Exception{StringBuilder s=new StringBuilder();for(int i=0;i<p.length;i+=2){if(i>0)s.append('&');s.append(URLEncoder.encode(p[i].toString(),"UTF-8")).append('=').append(URLEncoder.encode(p[i+1].toString(),"UTF-8"));}return s.toString();}
 static JSONObject postForm(String url,Object... p)throws Exception{return Json.parse(request("POST",url,null,form(p),"application/x-www-form-urlencoded",null));}
 static String cloudUrl(String value){URI u=URI.create(value);String h=u.getHost();if(!"https".equals(u.getScheme())||h==null||!h.endsWith(".xboxlive.com")||u.getUserInfo()!=null||u.getPort()!=-1)throw new IllegalArgumentException("Host de streaming inválido");return value.replaceAll("/+$","");}
}
