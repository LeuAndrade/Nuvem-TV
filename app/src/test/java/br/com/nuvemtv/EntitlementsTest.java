package br.com.nuvemtv;
import org.junit.*;import org.json.*;import static org.junit.Assert.*;
public class EntitlementsTest {
 static final String ID="9AAAAAAAAAAA";
 JSONObject license(String acquisition)throws Exception{return Json.obj("productId",ID,"productKind","Game","status","Active","acquisitionType",acquisition,"startDate","2020-01-01T00:00:00.0000000+00:00","endDate","9999-12-31T23:59:59.9999999+00:00","satisfiedByProductIds",new JSONArray());}
 @Test public void distinguishesPurchaseFromSubscriptionAndTrial()throws Exception{
  assertEquals(Entitlements.State.OWNED,Entitlements.lookup(ID,b->Json.obj("items",Json.arr(license("Single")))));
  assertEquals(Entitlements.State.ENTITLED,Entitlements.lookup(ID,b->Json.obj("items",Json.arr(license("Recurring")))));
  JSONObject trial=license("Single").put("trialData",Json.obj("isTrial",true));assertEquals(Entitlements.State.ENTITLED,Entitlements.lookup(ID,b->Json.obj("items",Json.arr(trial))));
 }
 @Test public void excludesRevokedExpiredAndOtherProducts()throws Exception{
  JSONObject revoked=license("Single").put("status","Revoked"),expired=license("Single").put("endDate","2021-01-01T00:00:00Z"),other=license("Single").put("productId","9BBBBBBBBBBB");
  assertEquals(Entitlements.State.NOT_OWNED,Entitlements.lookup(ID,b->Json.obj("items",Json.arr(revoked,expired,other))));
 }
 @Test public void checksRemainingPagesAndDoesNotGuessFromIncompleteResponse()throws Exception{
  int[] pages={0};assertEquals(Entitlements.State.OWNED,Entitlements.lookup(ID,b->{pages[0]++;assertEquals("Valid",b.getString("validityType"));return pages[0]==1?Json.obj("items",new JSONArray(),"continuationToken","next"):Json.obj("items",Json.arr(license("Single")));}));assertEquals(2,pages[0]);
  assertEquals(Entitlements.State.UNKNOWN,Entitlements.lookup(ID,b->new JSONObject()));assertEquals(Entitlements.State.UNKNOWN,Entitlements.lookup(ID,b->Json.obj("items",Json.arr(Json.obj("productId",ID)))));assertEquals(Entitlements.State.UNKNOWN,Entitlements.lookup(ID,b->Json.obj("items",new JSONArray(),"continuationToken","loop")));
 }
 @Test public void queryFailureDoesNotBecomeNotOwned()throws Exception{try{Entitlements.lookup(ID,b->{throw new java.io.IOException("offline");});fail();}catch(java.io.IOException expected){}}
 @Test public void subscriptionBundleDoesNotBecomePurchase()throws Exception{JSONObject parent=license("Single").put("satisfiedByProductIds",Json.arr("9BBBBBBBBBBB"));assertEquals(Entitlements.State.ENTITLED,Entitlements.lookup(ID,b->Json.obj("items",Json.arr(parent))));}
}
