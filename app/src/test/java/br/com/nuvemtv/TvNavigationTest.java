package br.com.nuvemtv;

import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.json.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TvNavigationTest {
    MainActivity activity;ActivityController<MainActivity> controller;
    @Before public void open(){
        RuntimeEnvironment.getApplication().getSharedPreferences("vault",0).edit().clear().commit();
        RuntimeEnvironment.getApplication().getSharedPreferences("settings",0).edit().clear().putString("language","pt-BR").commit();
        controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();activity.signedIn=true;
        JSONArray games=new JSONArray();for(int i=0;i<20;i++)games.put(Json.obj("id","game-"+i,"name",String.format("Jogo %02d",i),"cover","","genres",Json.arr(i%2==0?"Ação":"Corrida")));
        activity.catalog=games;activity.navigate("library");layout();
    }
    @After public void close(){controller.pause().stop().destroy();}
    void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
    void layout(){idle();View root=activity.root;root.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));root.layout(0,0,1920,1080);idle();}
    void downFrom(View view){view.requestFocus();assertTrue(view.hasFocus());view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_DOWN));layout();}
    void assertGreenFocus(){
        assertTrue(activity.grid.hasVisibleSelection());View child=activity.grid.getSelectedView();assertNotNull(child);
        Bitmap image=Bitmap.createBitmap(activity.grid.getWidth(),activity.grid.getHeight(),Bitmap.Config.ARGB_8888);activity.grid.draw(new Canvas(image));
        int x=child.getLeft()+child.getWidth()/2;int hits=0;for(int y=Math.max(0,child.getTop());y<Math.min(image.getHeight(),child.getTop()+20);y++){int color=image.getPixel(x,y);if(Color.green(color)>220&&Color.red(color)>100&&Color.blue(color)<100)hits++;}image.recycle();assertTrue("O cartão deve ter um contorno verde visível",hits>0);
    }
    @Test public void dpadDownFromBothFiltersShowsActualGreenBorder(){downFrom(activity.sortButton);assertGreenFocus();activity.genreButton.requestFocus();downFrom(activity.genreButton);assertGreenFocus();}
    @Test public void changingSortThenDownRetainsVisibleSelectionAndMovesAcrossCards(){
        activity.sortButton.requestFocus();activity.sortButton.performClick();idle();TextView reverse=LibraryUiTest.find(activity.choiceDialog.getWindow().getDecorView(),"Z–A");assertNotNull(reverse);reverse.performClick();layout();assertEquals("za",activity.order);downFrom(activity.sortButton);assertGreenFocus();
        int first=activity.grid.getSelectedItemPosition();activity.grid.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));activity.grid.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_RIGHT));layout();assertEquals(first+1,activity.grid.getSelectedItemPosition());assertGreenFocus();
    }
    @Test public void genreChoiceThenDownFocusesFilteredGame(){activity.genreButton.performClick();idle();TextView racing=LibraryUiTest.find(activity.choiceDialog.getWindow().getDecorView(),"Corrida");assertNotNull(racing);racing.performClick();layout();assertEquals(10,activity.grid.getCount());downFrom(activity.genreButton);assertGreenFocus();assertTrue(activity.counter.getText().toString().contains("Jogo"));}
    @Test public void firstRowReturnsToFilterAndFirstColumnReturnsToSidebar(){downFrom(activity.genreButton);activity.grid.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP));layout();assertTrue(activity.genreButton.hasFocus());downFrom(activity.sortButton);activity.grid.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT));layout();assertTrue(activity.navButtons.get("home").hasFocus());}
    @Test public void settingsKeepAllOptionsWithoutClientEditor(){
        activity.settingsPage="stream";activity.settings();layout();
        for(String label:new String[]{"Motor de streaming","Resolução solicitada","Limite de bitrate solicitado","Codec","Solicitar baixa latência no Native","Exibir estatísticas do streaming","Aplicar perfil leve"})assertNotNull(label,LibraryUiTest.find(activity.root,label));
        assertNull(LibraryUiTest.find(activity.root,"Client ID de autenticação (avançado)"));
        activity.settingsPage="network";activity.settings();assertNotNull(LibraryUiTest.find(activity.root,"Servidor: Automático   ▾"));assertNotNull(LibraryUiTest.find(activity.root,"Diagnóstico dos decodificadores"));
        activity.settingsPage="account";activity.settings();assertNotNull(LibraryUiTest.find(activity.root,"Sair da conta"));assertNotNull(LibraryUiTest.find(activity.root,"Créditos e licenças"));
    }
    @Test public void hidesClientEditorWithoutChangingPreviouslySavedIdentifier(){
        String existing="00000000-1111-2222-3333-444444444444";
        activity.prefs.edit().putString("client",existing).commit();controller.pause().stop().destroy();
        controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();
        assertEquals(existing,activity.auth.client);
        for(String section:new String[]{"stream","network","account"}){activity.settingsPage=section;activity.settings();assertNull(LibraryUiTest.find(activity.root,"Client ID de autenticação (avançado)"));}
        assertEquals(existing,activity.prefs.getString("client",""));
    }
    @Test public void streamingBoundaryReleasesArtworkViewsAndDialogAnchors(){
        activity.genreButton.performClick();assertNotNull(activity.choiceDialog);
        activity.playing=true;activity.releaseLibraryViews();
        assertTrue(activity.images.isPaused());assertEquals(0,activity.images.memoryBytes());
        assertNull(activity.root);assertNull(activity.grid);assertNull(activity.counter);assertNull(activity.heroImage);assertNull(activity.choiceDialog);assertTrue(activity.navButtons.isEmpty());
        activity.playing=false;activity.home();layout();assertEquals(20,activity.grid.getCount());
    }
    @Test public void settingChoiceAndToggleStillPersistTheirValues(){
        activity.settingsPage="stream";activity.settings();layout();View stats=activity.root.findViewWithTag("setting:stats");assertTrue(stats instanceof Switch);((Switch)stats).setChecked(true);assertTrue(activity.prefs.getBoolean("stats",false));
        Button quality=(Button)activity.root.findViewWithTag("setting:resolution");quality.performClick();idle();TextView full=LibraryUiTest.find(activity.choiceDialog.getWindow().getDecorView(),"1080p");assertNotNull(full);full.performClick();assertEquals("1080",activity.prefs.getString("resolution",""));assertEquals("1080p    ▾",quality.getText().toString());
    }
}
