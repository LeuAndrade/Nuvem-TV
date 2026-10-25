package br.com.nuvemtv;

import org.json.*;
import java.util.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.text.SimpleDateFormat;

/** Consulta pontual de licença. Erros, respostas incompletas e compartilhamento não são tratados como compra ausente. */
final class Entitlements {
    enum State { OWNED, ENTITLED, NOT_OWNED, UNKNOWN }
    static final String URL="https://collections.mp.microsoft.com/v8.0/collections/b2bLicensePreview";
    interface Query { JSONObject page(JSONObject body)throws Exception; }
    static State lookup(String productId,Query query)throws Exception{
        if(!productId.matches("[A-Za-z0-9]{12}"))return State.UNKNOWN;
        JSONObject body=Json.obj("maxPageSize",100,"excludeDuplicates",false,"expandSatisfyingItems",true,"market","neutral","validityType","Valid","entitlementFilters",Json.arr("*:Game"),"productSkuIds",Json.arr(Json.obj("productId",productId)));
        State result=State.NOT_OWNED;Set<String> seen=new HashSet<>();
        for(int pages=0;pages<20;pages++){
            JSONObject page=query.page(body);JSONArray items=page.optJSONArray("items");
            if(items==null)return State.UNKNOWN;
            for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null||!productId.equalsIgnoreCase(item.optString("productId")))continue;
                if(item.optString("status").isEmpty()||item.optString("startDate").isEmpty()||item.optString("endDate").isEmpty()){if(result!=State.ENTITLED)result=State.UNKNOWN;continue;}
                if(!"Active".equalsIgnoreCase(item.optString("status")))continue;
                if(!validDates(item,System.currentTimeMillis()))continue;
                String kind=item.optString("productType",item.optString("productKind"));if(!"Game".equalsIgnoreCase(kind))continue;
                JSONObject trial=item.optJSONObject("trialData");boolean isTrial=trial!=null&&trial.optBoolean("isTrial");JSONArray parents=item.optJSONArray("satisfiedByProductIds");
                if("Single".equalsIgnoreCase(item.optString("acquisitionType"))&&!isTrial&&(parents==null||parents.length()==0))return State.OWNED;
                result=State.ENTITLED;
            }
            String next=page.optString("continuationToken","");if(next.isEmpty())return result;
            if(!seen.add(next))return State.UNKNOWN;body.put("continuationToken",next);
        }return State.UNKNOWN;
    }
    static boolean validDates(JSONObject item,long now){try{
        for(String key:new String[]{"startDate","endDate"}){String value=item.optString(key);if(value.isEmpty())return false;
            value=value.replaceAll("\\.(\\d{3})\\d+",".$1");SimpleDateFormat format=new SimpleDateFormat(value.contains(".")?"yyyy-MM-dd'T'HH:mm:ss.SSSXXX":"yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US);format.setLenient(false);long date=format.parse(value).getTime();if(key.equals("startDate")?date>now:date<=now)return false;}
        return true;
    }catch(Exception ignored){return false;}}
    static Query client(Auth auth)throws Exception{
        String authorization=auth.collectionsAuthorization();KeyPairGenerator generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));PrivateKey key=generator.generateKeyPair().getPrivate();
        return body->{String raw=body.toString();Map<String,String> headers=new HashMap<>();headers.put("Authorization",authorization);headers.put("Signature",signature(key,authorization,raw,System.currentTimeMillis()));headers.put("User-Agent","BXboxCloudTV/0.5.0");return Json.parse(Net.request("POST",URL,null,raw,"application/json; charset=utf-8",null,headers));};
    }
    static State check(Auth auth,String productId)throws Exception{return lookup(productId,client(auth));}
    static JSONArray owned(Query query)throws Exception{
        JSONObject body=Json.obj("maxPageSize",100,"excludeDuplicates",false,"expandSatisfyingItems",true,"market","neutral","validityType","Valid","entitlementFilters",Json.arr("*:Game"));
        List<JSONObject> items=new ArrayList<>();Set<String> tokens=new HashSet<>();boolean complete=false;
        for(int page=0;page<50;page++){JSONObject response=query.page(body);JSONArray batch=response.optJSONArray("items");if(batch==null)throw new Exception("Resposta de licenças incompleta");for(int i=0;i<batch.length();i++){JSONObject item=batch.optJSONObject(i);if(item!=null)items.add(item);}String next=response.optString("continuationToken");if(next.isEmpty()){complete=true;break;}if(!tokens.add(next))throw new Exception("Paginação de licenças repetida");body.put("continuationToken",next);}
        if(!complete)throw new Exception("Biblioteca muito grande para esta consulta");return ownedProducts(items,System.currentTimeMillis());
    }
    static JSONArray ownedProducts(List<JSONObject> items,long now){Set<String> owned=new LinkedHashSet<>();List<JSONObject> candidates=new ArrayList<>();
        for(JSONObject item:items){String id=item.optString("productId").toUpperCase(Locale.ROOT);JSONObject trial=item.optJSONObject("trialData");if(!id.matches("[A-Z0-9]{12}")||!"Active".equalsIgnoreCase(item.optString("status"))||!"Single".equalsIgnoreCase(item.optString("acquisitionType"))||!"Game".equalsIgnoreCase(item.optString("productType",item.optString("productKind")))||(trial!=null&&trial.optBoolean("isTrial"))||!validDates(item,now))continue;JSONArray parents=item.optJSONArray("satisfiedByProductIds");if(parents==null||parents.length()==0)owned.add(id);else candidates.add(item);}
        boolean changed=true;while(changed){changed=false;for(JSONObject item:candidates){JSONArray parents=item.optJSONArray("satisfiedByProductIds");if(parents!=null)for(int i=0;i<parents.length();i++)if(owned.contains(parents.optString(i).toUpperCase(Locale.ROOT))){changed|=owned.add(item.optString("productId").toUpperCase(Locale.ROOT));break;}}}
        JSONArray result=new JSONArray();for(String id:owned)result.put(Json.obj("details",Json.obj("productId",id)));return result;
    }
    static String signature(PrivateKey key,String authorization,String body,long now)throws Exception{
        long ticks=(now/1000+11644473600L)*10000000L;byte[] time=ByteBuffer.allocate(8).putLong(ticks).array();ByteArrayOutputStream payload=new ByteArrayOutputStream();payload.write(new byte[]{0,0,0,1,0});payload.write(time);payload.write(0);
        for(String part:new String[]{"POST","/v8.0/collections/b2bLicensePreview",authorization,body}){payload.write(part.getBytes(StandardCharsets.UTF_8));payload.write(0);}
        Signature signer=Signature.getInstance("SHA256withECDSA");signer.initSign(key);signer.update(payload.toByteArray());byte[] der=signer.sign(),jose=new byte[64];int offset=2;
        if(der[0]!=0x30||der[offset++]!=2)throw new IOException("Assinatura inválida");int n=der[offset++]&255;copyInteger(der,offset,n,jose,0);offset+=n;if(der[offset++]!=2)throw new IOException("Assinatura inválida");n=der[offset++]&255;copyInteger(der,offset,n,jose,32);
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(new byte[]{0,0,0,1});out.write(time);out.write(jose);return android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);
    }
    static void copyInteger(byte[] source,int start,int length,byte[] target,int dest)throws IOException{while(length>32&&source[start]==0){start++;length--;}if(length>32||length<1)throw new IOException("Assinatura inválida");System.arraycopy(source,start,target,dest+32-length,length);}
}
