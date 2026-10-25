package br.com.nuvemtv;

import org.json.*;
import java.util.*;

/** Listas oficiais, mantendo a ordem publicada; nunca calcula popularidade local. */
final class HomeFeed {
    static final long REFRESH_MS=6*60*60*1000L;
    static final String[][] LISTS={
        {"gamepass","af206485-e87d-4624-9007-cb7f6d0cc42e"},
        {"own","e4c1d680-2c70-45e4-a38d-8a292c68c700"},
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
    static JSONArray promotions(String html)throws Exception{
        int key=html.indexOf("\"heroData\"");if(key<0)throw new Exception("Destaques oficiais indisponíveis");int start=html.indexOf('{',key);if(start<0)throw new Exception("Formato de destaques inválido");
        boolean quoted=false,escape=false;int depth=0,end=-1;for(int i=start;i<html.length();i++){char c=html.charAt(i);if(quoted){if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='\"')quoted=false;}else if(c=='\"')quoted=true;else if(c=='{')depth++;else if(c=='}'&&--depth==0){end=i+1;break;}}
        if(end<0)throw new Exception("Destaques incompletos");JSONArray source=new JSONObject(html.substring(start,end)).optJSONArray("data");if(source==null)throw new Exception("Formato de destaques inválido");
        JSONArray out=new JSONArray();Set<String> seen=new HashSet<>();for(int i=0;i<source.length();i++){JSONObject item=source.optJSONObject(i);if(item==null)continue;String id=item.optString("productID").toUpperCase(Locale.ROOT);if(!id.matches("[A-Z0-9]{12}")||!seen.add(id))continue;String image=item.optString("mobileFeaturedHeroImgUri",item.optString("mobileHeroImgUri"));out.put(Json.obj("productId",id,"name",item.optString("title"),"subtitle",item.optString("subtitle"),"cover",Catalog.coverUrl(image)));}return out;
    }
    static JSONArray promotedGames(JSONObject feed,JSONArray catalog){JSONArray out=new JSONArray(),promos=feed.optJSONArray("promotions");if(promos==null)return out;Map<String,JSONObject> byId=new HashMap<>();for(int i=0;i<catalog.length();i++){JSONObject g=catalog.optJSONObject(i);if(g!=null)byId.put(g.optString("productId"),g);}for(int i=0;i<promos.length();i++){JSONObject p=promos.optJSONObject(i);if(p==null)continue;JSONObject original=byId.get(p.optString("productId"));try{JSONObject g=original==null?Json.obj("id","store:"+p.optString("productId"),"productId",p.optString("productId"),"name",p.optString("name"),"cloudSupported",false):new JSONObject(original.toString());if(!p.optString("cover").isEmpty())g.put("cover",p.optString("cover"));g.put("promotionSubtitle",p.optString("subtitle"));out.put(g);}catch(Exception ignored){}}return out;}
    static boolean due(long last,long now){return last<=0||now<last||now-last>=REFRESH_MS;}
}
