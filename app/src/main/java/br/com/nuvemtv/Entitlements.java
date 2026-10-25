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
    static State check(Auth auth,String productId)throws Exception{
        String authorization=auth.collectionsAuthorization();KeyPairGenerator generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));PrivateKey key=generator.generateKeyPair().getPrivate();
        return lookup(productId,body->{String raw=body.toString();Map<String,String> headers=new HashMap<>();headers.put("Authorization",authorization);headers.put("Signature",signature(key,authorization,raw,System.currentTimeMillis()));headers.put("User-Agent","BXboxCloudTV/0.4.0");return Json.parse(Net.request("POST",URL,null,raw,"application/json; charset=utf-8",null,headers));});
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
