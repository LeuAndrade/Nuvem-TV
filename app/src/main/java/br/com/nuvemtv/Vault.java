package br.com.nuvemtv;
import android.content.*;import android.security.keystore.*;import android.util.Base64;import java.security.KeyStore;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;
final class Vault {
 private final Context context;Vault(Context c){context=c;}
 private javax.crypto.SecretKey key()throws Exception{KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);if(!k.containsAlias("nuvem.refresh")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("nuvem.refresh",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();}return (javax.crypto.SecretKey)k.getKey("nuvem.refresh",null);}
 synchronized void save(String s)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());context.getSharedPreferences("vault",0).edit().putString("iv",Base64.encodeToString(c.getIV(),2)).putString("data",Base64.encodeToString(c.doFinal(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)),2)).commit();}
 synchronized String load()throws Exception{android.content.SharedPreferences p=context.getSharedPreferences("vault",0);if(!p.contains("data"))return "";Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),2)));return new String(c.doFinal(Base64.decode(p.getString("data",""),2)),java.nio.charset.StandardCharsets.UTF_8);}
 synchronized void clear(){context.getSharedPreferences("vault",0).edit().clear().commit();}
}
