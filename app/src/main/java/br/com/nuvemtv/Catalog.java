package br.com.nuvemtv;

import org.json.*;
import java.net.URI;
import java.text.Normalizer;
import java.util.*;

final class Catalog {
    private static final java.util.regex.Pattern MARKS=java.util.regex.Pattern.compile("\\p{M}");
    private Catalog() { }
    static String normalized(String text) {
        return MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("").toLowerCase(Locale.ROOT);
    }
    static JSONObject product(JSONObject product, String id, String title) {
        JSONArray genres = product.optJSONArray("LocalizedCategories");
        if (genres == null) genres = product.optJSONArray("Categories");
        if (genres == null) genres = new JSONArray();
        JSONObject image = product.optJSONObject("Image_Tile");
        if (image == null) image = product.optJSONObject("Image_Poster");
        String cover = image == null ? "" : coverUrl(image.optString("URL"));
        return Json.obj("id", title, "productId", id, "name", product.optString("ProductTitle", id),
                "searchName", normalized(product.optString("ProductTitle", id)), "cover", cover, "publisher", product.optString("PublisherName"), "genres", genres);
    }
    static String coverUrl(String value) {
        try {
            if (value.startsWith("//")) value = "https:" + value;
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || uri.getUserInfo() != null || uri.getPort() != -1) return "";
            host = host.toLowerCase(Locale.ROOT);
            if (!(host.endsWith(".s-microsoft.com") || host.endsWith(".xboxlive.com") || host.endsWith(".xboxservices.com"))) return "";
            // Uma miniatura basta até para a tela de detalhes: sem baixar arte de vários megabytes.
            return new URI("https", host, uri.getPath(), "w=320&h=180&q=65&format=jpg", null).toASCIIString();
        } catch (Exception ignored) { return ""; }
    }
    static List<JSONObject> filter(JSONArray source, String query, String genre, String order,
                                   Set<String> favorites, boolean onlyFavorites) {
        String needle = normalized(query.trim());
        List<JSONObject> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < source.length(); i++) {
            JSONObject game = source.optJSONObject(i);
            if (game == null || !seen.add(game.optString("id"))) continue;
            if (onlyFavorites && !favorites.contains(game.optString("id"))) continue;
            if (!nameKey(game).contains(needle)) continue;
            JSONArray genres = game.optJSONArray("genres");
            boolean match = genre.isEmpty();
            if (genres != null) for (int n = 0; n < genres.length(); n++) if (genre.equals(genres.optString(n))) match = true;
            if (match) result.add(game);
        }
        if (!order.equals("recent")) {
            Comparator<JSONObject> byName = Comparator.comparing(Catalog::nameKey);
            result.sort(order.equals("za") ? byName.reversed() : byName);
        }
        return result;
    }
    private static String nameKey(JSONObject game) {
        String key=game.optString("searchName");
        return key.isEmpty()?normalized(game.optString("name")):key;
    }
    static String genres(JSONObject game) {
        JSONArray genres = game.optJSONArray("genres");
        if (genres == null) return "Cloud Gaming";
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < genres.length(); i++) labels.add(genres.optString(i));
        return labels.isEmpty() ? "Cloud Gaming" : String.join(" · ", labels);
    }
}
