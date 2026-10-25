package br.com.nuvemtv;
import android.content.SharedPreferences;import org.json.*;
/** Cache somente de operações aceitas, reconciliado com flags explícitas do perfil. */
final class SocialState {
 final SharedPreferences prefs;SocialState(SharedPreferences p){prefs=p;}
 static Boolean flag(JSONObject user,String key){JSONObject detail=user.optJSONObject("detail");if(detail!=null&&detail.has(key)&&!detail.isNull(key))return detail.optBoolean(key);return user.has(key)&&!user.isNull(key)?user.optBoolean(key):null;}
 void accepted(String id,boolean friend){prefs.edit().putLong((friend?"request:":"follow:")+id,System.currentTimeMillis()).apply();}
 void observe(JSONObject user){String id=user.optString("xuid");SharedPreferences.Editor edit=prefs.edit();Boolean followed=flag(user,"isFollowedByCaller"),sent=flag(user,"isFriendRequestSent"),friend=flag(user,"isFriend");long now=System.currentTimeMillis();if(followed!=null&&(followed||now-prefs.getLong("follow:"+id,0)>60000))edit.remove("follow:"+id);if(Boolean.TRUE.equals(friend)||(sent!=null&&(sent||now-prefs.getLong("request:"+id,0)>60000)))edit.remove("request:"+id);edit.apply();}
 boolean following(JSONObject user){return Boolean.TRUE.equals(flag(user,"isFollowedByCaller"))||prefs.contains("follow:"+user.optString("xuid"));}
 String friendLabel(JSONObject user){if(Boolean.TRUE.equals(flag(user,"isFriend")))return "Amigos";if(Boolean.TRUE.equals(flag(user,"isFriendRequestSent"))||prefs.contains("request:"+user.optString("xuid")))return "Amizade solicitada";return "Solicitar amizade";}
 boolean canRequest(JSONObject user){return friendLabel(user).equals("Solicitar amizade");}
 void clear(){prefs.edit().clear().apply();}
}
