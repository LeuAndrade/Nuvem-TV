package br.com.nuvemtv;

import org.junit.Test;import org.json.*;import java.util.*;
import static org.junit.Assert.*;

public class CatalogTest {
    JSONObject game(String id,String name,String genre){return Json.obj("id",id,"name",name,"genres",Json.arr(genre));}
    @Test public void searchesIgnoringCaseAndAccents(){JSONArray games=Json.arr(game("1","Ação no Espaço","Ação"),game("2","Beta","Corrida"));assertEquals(1,Catalog.filter(games,"ACAO","","az",Collections.emptySet(),false).size());}
    @Test public void combinesGenreSearchFavoritesAndSort(){JSONArray games=Json.arr(game("1","Zebra","Ação"),game("2","Alfa","Ação"),game("3","Beta","Corrida"));Set<String> favorites=new HashSet<>(Arrays.asList("1","2"));List<JSONObject> result=Catalog.filter(games,"a","Ação","az",favorites,true);assertEquals(2,result.size());assertEquals("Alfa",result.get(0).optString("name"));assertEquals("Zebra",Catalog.filter(games,"","","za",favorites,true).get(0).optString("name"));}
    @Test public void preservesRecentOrderAndRemovesDuplicates(){JSONArray games=Json.arr(game("2","Zebra","Ação"),game("1","Alfa","Ação"),game("2","Zebra","Ação"));List<JSONObject> result=Catalog.filter(games,"","","recent",Collections.emptySet(),false);assertEquals(2,result.size());assertEquals("2",result.get(0).optString("id"));}
    @Test public void mapsMicrosoftArtworkPublisherAndGenres()throws Exception{JSONObject game=Catalog.product(Json.obj("ProductTitle","Jogo","PublisherName","Editora","Image_Tile",Json.obj("URL","//store-images.s-microsoft.com/image/test"),"LocalizedCategories",Json.arr("Corrida")),"product","title");assertEquals("Editora",game.getString("publisher"));assertTrue(game.getString("cover").startsWith("https://store-images.s-microsoft.com/"));assertTrue(game.getString("cover").contains("w=320"));assertEquals("Corrida",Catalog.genres(game));}
    @Test public void refusesArbitraryAndInsecureImageHosts(){for(String value:new String[]{"file:///etc/passwd","http://store-images.s-microsoft.com/test","https://evil.test/image","https://store-images.s-microsoft.com.evil.test/image","https://user@store-images.s-microsoft.com/image"})assertEquals("",Catalog.coverUrl(value));}
}
