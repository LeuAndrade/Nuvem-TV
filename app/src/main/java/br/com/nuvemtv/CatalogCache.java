package br.com.nuvemtv;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Apenas metadados; não contém tokens nem dados de autenticação. */
final class CatalogCache {
    private final AtomicFile file;
    CatalogCache(Context context) { file = new AtomicFile(new File(context.getFilesDir(), "catalog-v2.json")); }
    synchronized JSONObject read() {
        try (InputStream input = file.openRead(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) { output.write(buffer, 0, count); if (output.size() > 3 * 1024 * 1024) return new JSONObject(); }
            return Json.parse(output.toString("UTF-8"));
        } catch (Exception ignored) { return new JSONObject(); }
    }
    synchronized void write(JSONArray all, JSONArray recent) {
        FileOutputStream output = null;
        try {
            byte[] bytes = Json.obj("catalog", all, "recent", recent).toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 3 * 1024 * 1024) return;
            output = file.startWrite(); output.write(bytes); file.finishWrite(output);
        } catch (Exception ignored) { if (output != null) file.failWrite(output); }
    }
    synchronized void clear() { file.delete(); }
}
