package br.com.nuvemtv;

import org.json.*;
import java.net.URLEncoder;
import java.util.*;

/** Requisições reais ao Xbox. Nenhum resultado de escrita é simulado localmente. */
final class SocialApi {
    interface Transport {JSONObject call(String method,String url,String contract,JSONObject body)throws Exception;}
    final Transport transport;
    SocialApi(Transport transport){this.transport=transport;}
    SocialApi(Auth auth,String locale){this(new Transport(){String token="";long at;
        public JSONObject call(String method,String url,String contract,JSONObject body)throws Exception{
            if(token.isEmpty()||System.currentTimeMillis()-at>10*60*1000L){token=auth.xboxAuthorization("http://xboxlive.com");at=System.currentTimeMillis();}
            Map<String,String> headers=new HashMap<>();headers.put("Authorization",token);headers.put("x-xbl-contract-version",contract);headers.put("Accept-Language",locale);
            String raw=Net.request(method,url,null,body==null?null:body.toString(),"application/json",null,headers);return raw.trim().isEmpty()?new JSONObject():Json.parse(raw);
        }
    });}
    static String xuid(String value){if(value==null||!value.matches("[0-9]{1,20}"))throw new IllegalArgumentException("XUID inválido");return value;}
    static JSONArray people(JSONObject response)throws Exception{JSONArray source=response.optJSONArray("people");if(source==null)throw new Exception("Formato de pessoas não reconhecido");JSONArray out=new JSONArray();Set<String> seen=new HashSet<>();for(int i=0;i<Math.min(100,source.length());i++){JSONObject person=source.optJSONObject(i);if(person==null)continue;String id=person.optString("xuid");if(id.matches("[0-9]{1,20}")&&seen.add(id))out.put(person);}return out;}
    static String query(String value)throws Exception{return URLEncoder.encode(value,"UTF-8").replace("+","%20");}
    JSONArray search(String value)throws Exception{
        String q=value.replace('\u00a0',' ').trim();if(q.isEmpty()||q.length()>64)throw new IllegalArgumentException("Digite um gamertag de até 64 caracteres");
        Net.HttpError last=null;
        // Search aceita detail/preferredColor; presenceDetail não faz parte desse contrato.
        for(String decoration:new String[]{"detail,preferredColor","detail"}){
            try{return people(transport.call("GET","https://peoplehub.xboxlive.com/users/me/people/search/decoration/"+decoration+"?q="+query(q)+"&maxItems=25","3",null));}
            catch(Net.HttpError error){if(error.status!=400&&error.status!=404)throw error;last=error;}
        }
        // Alternativa exata: Profile não depende da versão do decorador do PeopleHub.
        try{return profilePeople(transport.call("GET","https://profile.xboxlive.com/users/gt("+query(q)+")/profile/settings?settings=Gamertag,GameDisplayPicRaw","2",null));}
        catch(Net.HttpError error){if(error.status==404)return new JSONArray();if(last!=null&&error.status==400)throw new Exception("O Xbox recusou os formatos de pesquisa desta conta. Use a área social integrada.");throw error;}
    }
    static JSONArray profilePeople(JSONObject response)throws Exception{
        JSONArray source=response.optJSONArray("profileUsers");if(source==null)throw new Exception("Formato de perfil não reconhecido");JSONArray result=new JSONArray();
        for(int i=0;i<source.length();i++){JSONObject raw=source.optJSONObject(i);if(raw==null)continue;String id=raw.optString("id");if(!id.matches("[0-9]{1,20}"))continue;JSONObject user=Json.obj("xuid",id);JSONArray settings=raw.optJSONArray("settings");JSONObject detail=new JSONObject();if(settings!=null)for(int n=0;n<settings.length();n++){JSONObject item=settings.optJSONObject(n);if(item==null)continue;switch(item.optString("id")){case "Gamertag":user.put("gamertag",item.optString("value"));break;case "GameDisplayPicRaw":user.put("displayPicRaw",item.optString("value"));break;case "Gamerscore":user.put("gamerScore",item.optString("value"));break;case "Bio":detail.put("bio",item.optString("value"));break;default:break;}}user.put("detail",detail);result.put(user);}return result;
    }
    JSONArray friends()throws Exception{return people(transport.call("GET","https://peoplehub.xboxlive.com/users/me/people/social/decoration/detail,presenceDetail","7",null));}
    JSONArray suggestions()throws Exception{return people(transport.call("GET","https://peoplehub.xboxlive.com/users/me/people/recommendations/decoration/detail","7",null));}
    JSONObject profile(String id)throws Exception{JSONArray result=people(transport.call("GET","https://peoplehub.xboxlive.com/users/me/people/xuids("+xuid(id)+")/decoration/detail,presenceDetail","7",null));if(result.length()==0)throw new Exception("Perfil indisponível");return result.getJSONObject(0);}
    void follow(String id)throws Exception{transport.call("PUT","https://social.xboxlive.com/users/me/people/xuid("+xuid(id)+")","2",null);}
    void requestFriend(String id)throws Exception{JSONObject response=transport.call("POST","https://social.xboxlive.com/bulk/users/me/people/friends/v2?method=add","3",Json.obj("xuids",Json.arr(xuid(id))));JSONArray errors=response.optJSONArray("errors");if(response.has("error")||(errors!=null&&errors.length()>0))throw new Exception("O Xbox recusou a solicitação de amizade");}
    JSONObject conversation(String id)throws Exception{return transport.call("GET","https://xblmessaging.xboxlive.com/network/Xbox/users/me/conversations/users/xuid("+xuid(id)+")?maxItems=30","1",null);}
    JSONObject message(String id,String text)throws Exception{String content=text.trim();if(content.isEmpty()||content.length()>256)throw new IllegalArgumentException("A mensagem deve ter de 1 a 256 caracteres");JSONObject result=transport.call("POST","https://xblmessaging.xboxlive.com/network/Xbox/users/me/conversations/users/xuid("+xuid(id)+")","1",Json.obj("parts",Json.arr(Json.obj("contentType","text","version",0,"text",content))));if(result.has("error"))throw new Exception("O Xbox recusou a mensagem");return result;}
    static String gamertag(JSONObject person){String tag=person.optString("modernGamertag",person.optString("gamertag"));String suffix=person.optString("modernGamertagSuffix");return tag.isEmpty()?person.optString("xuid"):tag+(suffix.isEmpty()?"":"#"+suffix);}
}
