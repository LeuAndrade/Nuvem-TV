package br.com.nuvemtv;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.Looper;
import android.widget.ImageView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CoverLoaderTest {
    @Test public void decodesCachedArtworkAtBoundedSizeAndStopsWhenPaused()throws Exception{
        android.content.Context context=RuntimeEnvironment.getApplication();
        String url="https://store-images.s-microsoft.com/image/fixture?w=320&h=180&q=65&format=jpg";
        File dir=new File(context.getCacheDir(),"covers-v2");dir.mkdirs();StringBuilder hash=new StringBuilder();
        for(byte value:MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8)))hash.append(String.format("%02x",value&255));
        File file=new File(dir,hash+".jpg");Bitmap source=Bitmap.createBitmap(1280,720,Bitmap.Config.RGB_565);
        source.eraseColor(0xff72edab);try(FileOutputStream out=new FileOutputStream(file)){source.compress(Bitmap.CompressFormat.JPEG,80,out);}source.recycle();
        CoverLoader loader=new CoverLoader(context);
        try{
            ImageView image=new ImageView(context);loader.bind(image,url);long deadline=System.currentTimeMillis()+3000;
            while(image.getDrawable()==null&&System.currentTimeMillis()<deadline){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}
            assertTrue(image.getDrawable() instanceof BitmapDrawable);Bitmap bitmap=((BitmapDrawable)image.getDrawable()).getBitmap();assertTrue(bitmap.getWidth()<=320);assertTrue(bitmap.getHeight()<=180);assertTrue(loader.memoryBytes()<=CoverLoader.MEMORY_BYTES);
            loader.pause();assertEquals(0,loader.memoryBytes());loader.bind(image,url);assertNull(image.getDrawable());
        }finally{loader.close();}
    }
}
