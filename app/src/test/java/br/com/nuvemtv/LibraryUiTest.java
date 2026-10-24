package br.com.nuvemtv;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import android.view.*;
import android.widget.*;
import org.json.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LibraryUiTest {
    ActivityController<MainActivity> controller;MainActivity activity;
    @Before public void open(){RuntimeEnvironment.getApplication().getSharedPreferences("vault",0).edit().clear().commit();RuntimeEnvironment.getApplication().getSharedPreferences("settings",0).edit().clear().commit();controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();}
    @After public void close(){controller.pause().stop().destroy();}
    void library(){activity.signedIn=true;activity.catalog=Json.arr(Json.obj("id","one","name","Uma aventura","cover","","publisher","Editora","genres",Json.arr("Ação")),Json.obj("id","two","name","Corrida","cover","","genres",Json.arr("Corrida")));activity.home();}
    @Test public void cardOpensDetailsAndBackPreservesLibrary(){library();activity.details(activity.catalog.optJSONObject(0));assertEquals("detail",activity.screen);assertNotNull(find(activity.getWindow().getDecorView(),"▶  Jogar"));assertNotNull(find(activity.getWindow().getDecorView(),"Voltar ao início"));activity.onBackPressed();assertEquals("home",activity.screen);assertEquals(2,activity.grid.getAdapter().getCount());}
    @Test public void favoritesAndFiltersAreAppliedToActualGrid(){library();activity.details(activity.catalog.optJSONObject(0));find(activity.getWindow().getDecorView(),"☆  Adicionar aos favoritos").performClick();activity.navigate("favorites");assertEquals(1,activity.grid.getAdapter().getCount());activity.navigate("home");activity.query="corrida";activity.renderGrid();assertEquals(1,activity.grid.getAdapter().getCount());}
    @Test public void connectedAccountDoesNotDisplayLoginOrOfficialSite(){library();assertEquals("●  Conta conectada",activity.accountButton.getText().toString());assertNull(find(activity.getWindow().getDecorView(),"Xbox.com/play"));assertNull(find(activity.getWindow().getDecorView(),"Entrar na conta"));}
    @Test public void coverRequestsCanBeSuspendedWithoutStartingStreaming(){assertFalse(activity.images.isPaused());activity.images.pause();assertTrue(activity.images.isPaused());assertEquals(0,activity.images.memoryBytes());activity.images.resume();assertFalse(activity.images.isPaused());}
    @Test public void laysOutCatalogAndDetailsAtTvResolution() throws Exception {
        library();
        String fixture=System.getProperty("bxbox.qa.fixture");
        if(fixture!=null){
            java.nio.file.Path directory=java.nio.file.Paths.get(fixture);
            activity.catalog=new JSONArray(new String(java.nio.file.Files.readAllBytes(directory.resolve("catalog.json")),java.nio.charset.StandardCharsets.UTF_8));
            java.io.File cache=new java.io.File(activity.getCacheDir(),"covers-v2");cache.mkdirs();
            try(java.util.stream.Stream<java.nio.file.Path> files=java.nio.file.Files.list(directory)){
                for(java.nio.file.Path file:files.filter(p->p.toString().endsWith(".jpg")).collect(java.util.stream.Collectors.toList()))java.nio.file.Files.copy(file,cache.toPath().resolve(file.getFileName()),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            activity.home();
        }
        snapshot("catalog.png");
        if(fixture!=null){Thread.sleep(600);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();snapshot("catalog.png");}
        activity.details(activity.catalog.optJSONObject(0));
        snapshot("details.png");
        if(fixture!=null){Thread.sleep(100);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();snapshot("details.png");}
    }
    void snapshot(String name) throws Exception {
        View root=activity.root;
        root.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));
        root.layout(0,0,1920,1080);
        assertTrue(root.getWidth()>0);
        String path=System.getProperty("bxbox.qa.dir");
        if(path==null)return;
        java.io.File dir=new java.io.File(path);dir.mkdirs();
        android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(1920,1080,android.graphics.Bitmap.Config.ARGB_8888);
        root.draw(new android.graphics.Canvas(image));
        try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(dir,name))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}
        image.recycle();
    }
    static TextView find(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return (TextView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView match=find(group.getChildAt(i),text);if(match!=null)return match;}}return null;}
}
