package br.com.nuvemtv;
import org.json.*;
final class Json {
 static JSONObject obj(Object... pairs) { JSONObject o=new JSONObject(); try {for(int i=0;i<pairs.length;i+=2)o.put((String)pairs[i],pairs[i+1]);}catch(JSONException e){throw new IllegalArgumentException(e);}return o; }
 static JSONArray arr(Object... items){JSONArray a=new JSONArray();for(Object v:items)a.put(v);return a;}
 static JSONObject parse(String s){try{return new JSONObject(s);}catch(JSONException e){throw new IllegalArgumentException("Resposta JSON inválida",e);}}
}
