package br.com.nuvemtv;

import org.json.*;
import java.util.*;

/** Listas oficiais, mantendo a ordem publicada; nunca calcula popularidade local. */
final class HomeFeed {
    static final long REFRESH_MS=6*60*60*1000L;
    static final String[][] LISTS={
        {"popular","6a589fa0-d493-472b-8e20-3813699d7056"},
        {"added","06323672-b8c8-43cc-b0de-32d5a9834749"},
        {"leaving","31ff2361-2772-4622-849b-f4f1abb4ad1b"}
    };
    interface Fetch { JSONArray get(String id)throws Exception; }
    static JSONObject fetch(JSONObject previous,Fetch fetch,long now)throws Exception{
        JSONObject result=new JSONObject();boolean complete=true;
        for(String[] list:LISTS){
            try{JSONArray raw=fetch.get(list[1]),ids=new JSONArray();Set<String> seen=new HashSet<>();
                for(int i=0;i<raw.length();i++){JSONObject item=raw.optJSONObject(i);String id=item==null?"":item.optString("id").toUpperCase(Locale.ROOT);if(id.matches("[A-Z0-9]{12}")&&seen.add(id))ids.put(id);}
                result.put(list[0],ids);
            }catch(Exception error){complete=false;JSONArray old=previous.optJSONArray(list[0]);if(old!=null)result.put(list[0],old);}
        }
        result.put("updatedAt",complete?now:previous.optLong("updatedAt",0));result.put("partial",!complete);return result;
    }
    static JSONArray select(JSONObject feed,String key,JSONArray catalog){
        Map<String,JSONObject> index=new HashMap<>();for(int i=0;i<catalog.length();i++){JSONObject g=catalog.optJSONObject(i);if(g!=null)index.put(g.optString("productId").toUpperCase(Locale.ROOT),g);}
        JSONArray result=new JSONArray(),ids=feed.optJSONArray(key);if(ids!=null)for(int i=0;i<ids.length();i++){JSONObject g=index.get(ids.optString(i).toUpperCase(Locale.ROOT));if(g!=null)result.put(g);}return result;
    }
    static JSONArray titles(JSONObject feed){JSONArray result=new JSONArray();Set<String> seen=new HashSet<>();for(String[] list:LISTS){JSONArray ids=feed.optJSONArray(list[0]);if(ids!=null)for(int i=0;i<ids.length();i++){String id=ids.optString(i);if(seen.add(id))result.put(Json.obj("details",Json.obj("productId",id)));}}return result;}
    static boolean due(long last,long now){return last<=0||now<last||now-last>=REFRESH_MS;}
}
