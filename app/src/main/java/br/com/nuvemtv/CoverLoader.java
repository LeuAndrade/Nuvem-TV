package br.com.nuvemtv;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Miniaturas visíveis: duas requisições, fila limitada, 3 MiB RAM e 20 MiB disco. */
final class CoverLoader {
    static final int MEMORY_BYTES = 3 * 1024 * 1024;
    static final long DISK_BYTES = 20L * 1024 * 1024;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 15, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20), new ThreadPoolExecutor.AbortPolicy());
    private final LruCache<String, Bitmap> memory = new LruCache<String, Bitmap>(MEMORY_BYTES) {
        protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    private final Set<HttpURLConnection> connections = ConcurrentHashMap.newKeySet();
    private final AtomicInteger generation = new AtomicInteger();
    private final File directory;
    private volatile boolean paused, closed;

    CoverLoader(Context context) {
        directory = new File(context.getCacheDir(), "covers-v2");
        directory.mkdirs();
        workers.allowCoreThreadTimeOut(true);
    }
    void screen() { generation.incrementAndGet(); workers.getQueue().clear(); }
    void resume() { paused = false; screen(); }
    void pause() {
        paused = true;
        screen();
        for (HttpURLConnection connection : connections) connection.disconnect();
        memory.evictAll();
        main.removeCallbacksAndMessages(null);
    }
    void bind(ImageView view, String url) { bind(view,url,320,180); }
    void bindAvatar(ImageView view,String url){bind(view,AvatarUrl.resize(url),96,96);}
    void bindHero(ImageView view,String original) {
        String url=Catalog.coverUrl(original);
        if(!url.isEmpty())url=url.replace("w=320&h=180", "w=640&h=360");
        bind(view,url,640,360);
    }
    private void bind(ImageView view, String url,int width,int height) {
        view.setImageDrawable(width==height?new TvIcon("account",0xff9ab1bd,width):null);
        view.setTag(url);
        if (paused || closed || url.isEmpty() || Catalog.coverUrl(url).isEmpty()) return;
        Bitmap hit = memory.get(url);
        if (hit != null) { view.setImageBitmap(hit); return; }
        WeakReference<ImageView> reference = new WeakReference<>(view);
        int token = generation.get();
        try {
            workers.execute(() -> {
                if (paused || closed || token != generation.get()) return;
                try {
                    Bitmap bitmap = memory.get(url);
                    if (bitmap == null) {
                        File file = new File(directory, hash(url) + ".jpg");
                        if (!file.isFile()) download(url, file, token);
                        if (paused || closed || token != generation.get()) return;
                        bitmap = decode(file,width,height);
                        if (bitmap == null) { file.delete(); return; }
                        synchronized (memory) {
                            if (paused || closed || token != generation.get()) return;
                            memory.put(url, bitmap);
                        }
                        file.setLastModified(System.currentTimeMillis());
                    }
                    Bitmap result = bitmap;
                    main.post(() -> {
                        ImageView target = reference.get();
                        if (!paused && !closed && token == generation.get() && target != null && url.equals(target.getTag()))
                            target.setImageBitmap(result);
                    });
                } catch (Exception ignored) { /* O título segue acessível se a capa não carregar. */ }
            });
        } catch (RejectedExecutionException ignored) { /* Nunca cresce a fila durante navegação rápida. */ }
    }
    private void download(String url, File destination, int token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setInstanceFollowRedirects(false);
        connections.add(connection);
        File temporary = File.createTempFile("cover-", ".part", directory);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Capa indisponível");
            try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int total = 0, count;
                while ((count = input.read(buffer)) != -1) {
                    if (paused || closed || token != generation.get()) throw new InterruptedIOException();
                    total += count;
                    if (total > 3 * 1024 * 1024) throw new IOException("Capa grande demais");
                    output.write(buffer, 0, count);
                }
            }
            if (!temporary.renameTo(destination) && !destination.isFile()) throw new IOException("Cache indisponível");
            prune();
        } finally {
            temporary.delete();
            connections.remove(connection);
            connection.disconnect();
        }
    }
    private Bitmap decode(File file,int width,int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0 || options.outWidth > 8192 || options.outHeight > 8192) return null;
        options.inSampleSize = 1;
        while (options.outWidth / options.inSampleSize > width*2 || options.outHeight / options.inSampleSize > height*2)
            options.inSampleSize *= 2;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap != null && (bitmap.getWidth() > width || bitmap.getHeight() > height)) {
            float ratio = Math.min((float)width / bitmap.getWidth(), (float)height / bitmap.getHeight());
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(bitmap.getWidth() * ratio)),
                    Math.max(1, Math.round(bitmap.getHeight() * ratio)), true);
            if (bitmap != resized) bitmap.recycle();
            bitmap = resized;
        }
        return bitmap;
    }
    private synchronized void prune() {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".jpg"));
        if (files == null) return;
        long total = 0;
        for (File file : files) total += file.length();
        if (total <= DISK_BYTES) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (File file : files) { long size = file.length(); if (file.delete()) total -= size; if (total <= DISK_BYTES) break; }
    }
    private String hash(String url) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format(Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }
    int memoryBytes() { return memory.size(); }
    boolean isPaused() { return paused; }
    void close() { closed = true; pause(); workers.shutdownNow(); }
}
