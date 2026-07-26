package com.arian.bot.news;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Trae historias recientes y populares de Hacker News (vía la API pública de Algolia). */
public class HackerNewsSource {

    private static final String API_URL = "https://hn.algolia.com/api/v1/search_by_date";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static List<NewsItem> fetch(int limit) {
        List<NewsItem> items = new ArrayList<>();
        try {
            long since = Instant.now().minusSeconds(6L * 24 * 3600).getEpochSecond();
            String url = API_URL + "?tags=story&numericFilters=points%3E%3D60,created_at_i%3E" + since
                    + "&hitsPerPage=" + limit;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("❌ Error API Hacker News: " + response.statusCode());
                return items;
            }

            JSONArray hits = new JSONObject(response.body()).getJSONArray("hits");
            for (int i = 0; i < hits.length(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                String objectId = hit.getString("objectID");
                String title = hit.optString("title", "").trim();
                if (title.isBlank()) continue;

                String storyUrl = hit.optString("url", "");
                if (storyUrl.isBlank()) {
                    storyUrl = "https://news.ycombinator.com/item?id=" + objectId;
                }

                String author = hit.optString("author", "desconocido");
                String published = Instant.ofEpochSecond(hit.optLong("created_at_i", since))
                        .atZone(ZoneId.of("America/Mexico_City"))
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-MX")));

                // Solo los posts "Ask/Show HN" traen texto propio; el resto no tiene abstract disponible.
                String storyText = hit.optString("story_text", "");
                String sourceText = storyText.isBlank() ? null : storyText.replaceAll("<[^>]*>", "").trim();

                items.add(new NewsItem(
                        "hn:" + objectId,
                        title,
                        "Discusión iniciada por " + author,
                        "Hacker News",
                        storyUrl,
                        "Programación",
                        published,
                        sourceText
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Error al consultar Hacker News: " + e.getMessage());
        }
        return items;
    }
}
