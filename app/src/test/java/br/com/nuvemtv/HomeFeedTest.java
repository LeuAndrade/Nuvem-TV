package br.com.nuvemtv;
import org.junit.*;import org.json.*;import static org.junit.Assert.*;
public class HomeFeedTest {
 @Test public void preservesOfficialOrderDeduplicatesAndFiltersToCloud()throws Exception{
  JSONObject feed=HomeFeed.fetch(new JSONObject(),id->Json.arr(Json.obj("title","meta"),Json.obj("id","9AAAAAAAAAAA"),Json.obj("id","9BBBBBBBBBBB"),Json.obj("id","9AAAAAAAAAAA"),Json.obj("id","9CCCCCCCCCCC")),123);
  JSONArray games=Json.arr(Json.obj("productId","9BBBBBBBBBBB","id","b"),Json.obj("productId","9AAAAAAAAAAA","id","a"));JSONArray selected=HomeFeed.select(feed,"popular",games);
  assertEquals(2,selected.length());assertEquals("a",selected.getJSONObject(0).getString("id"));assertEquals("b",selected.getJSONObject(1).getString("id"));assertEquals(3,HomeFeed.titles(feed).length());assertEquals(123,feed.getLong("updatedAt"));
 }
 @Test public void keepsPreviousListOnFailureButReplacesWithOfficialEmptyList()throws Exception{
  JSONObject old=Json.obj("popular",Json.arr("9AAAAAAAAAAA"),"added",Json.arr("9BBBBBBBBBBB"),"updatedAt",12);
  JSONObject fresh=HomeFeed.fetch(old,id->{if(id.equals(HomeFeed.LISTS[0][1]))throw new Exception("offline");return new JSONArray();},999);
  assertEquals(1,fresh.getJSONArray("popular").length());assertEquals(0,fresh.getJSONArray("added").length());assertTrue(fresh.getBoolean("partial"));assertEquals(12,fresh.getLong("updatedAt"));
 }
 @Test public void refreshIntervalDoesNotPollContinuously(){assertTrue(HomeFeed.due(0,100));assertFalse(HomeFeed.due(100,100+HomeFeed.REFRESH_MS-1));assertTrue(HomeFeed.due(100,100+HomeFeed.REFRESH_MS));assertTrue(HomeFeed.due(200,100));}
}
