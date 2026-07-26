package com.arian.bot.music;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resuelve links de Spotify (canción, álbum o playlist) leyendo su metadata real de la API de
 * Spotify (título + artista) y buscando esa canción en YouTube — Spotify no permite streaming
 * directo del audio, así que solo se usa para saber QUÉ buscar.
 */
public class SpotifyResolver {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "open\\.spotify\\.com/(?:intl-[a-zA-Z-]+/)?(track|playlist|album)/([a-zA-Z0-9]+)");

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static volatile String cachedToken;
    private static volatile Instant tokenExpiry = Instant.EPOCH;

    public static boolean isSpotifyUrl(String query) {
        return URL_PATTERN.matcher(query).find();
    }

    public static boolean isTrackUrl(String query) {
        Matcher m = URL_PATTERN.matcher(query);
        return m.find() && m.group(1).equals("track");
    }

    /** Resuelve una sola canción de Spotify a una entrada de cola (sin audio todavía, se busca en YouTube). */
    public static QueuedTrack resolveTrack(String url, String requestedBy) {
        Matcher m = URL_PATTERN.matcher(url);
        if (!m.find() || !m.group(1).equals("track")) return null;
        String token = getAccessToken();
        if (token == null) return null;

        JSONObject track = getJson("https://api.spotify.com/v1/tracks/" + m.group(2), token);
        if (track == null) return null;
        return trackToQueuedTrack(track, requestedBy);
    }

    /** Listado de una playlist o álbum de Spotify (sin resolver audio; se busca en YouTube justo antes de sonar). */
    public static List<QueuedTrack> listPlaylistOrAlbum(String url, String requestedBy, int limit) {
        List<QueuedTrack> tracks = new ArrayList<>();
        Matcher m = URL_PATTERN.matcher(url);
        if (!m.find()) return tracks;
        String type = m.group(1);
        String id = m.group(2);
        String token = getAccessToken();
        if (token == null) return tracks;

        String next = type.equals("album")
                ? "https://api.spotify.com/v1/albums/" + id + "/tracks?limit=" + Math.min(limit, 50)
                : "https://api.spotify.com/v1/playlists/" + id + "/tracks?limit=" + Math.min(limit, 50);

        while (next != null && tracks.size() < limit) {
            JSONObject page = getJson(next, token);
            if (page == null) break;
            JSONArray items = page.optJSONArray("items");
            if (items == null) break;
            for (int i = 0; i < items.length() && tracks.size() < limit; i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject track = type.equals("album") ? item : item.optJSONObject("track");
                QueuedTrack qt = trackToQueuedTrack(track, requestedBy);
                if (qt != null) tracks.add(qt);
            }
            next = page.isNull("next") ? null : page.optString("next", null);
        }
        return tracks;
    }

    private static QueuedTrack trackToQueuedTrack(JSONObject track, String requestedBy) {
        if (track == null) return null;
        String name = track.optString("name", null);
        if (name == null || name.isBlank()) return null;

        JSONArray artists = track.optJSONArray("artists");
        String artist = (artists != null && artists.length() > 0)
                ? artists.getJSONObject(0).optString("name", "Desconocido")
                : "Desconocido";

        long durationMs = track.optLong("duration_ms", 0);
        String searchQuery = "ytsearch1:" + name + " " + artist;
        return new QueuedTrack(searchQuery, name, artist, durationMs, requestedBy);
    }

    private static String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) return cachedToken;
        synchronized (SpotifyResolver.class) {
            if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) return cachedToken;

            String clientId = System.getenv("SPOTIFY_CLIENT_ID");
            String clientSecret = System.getenv("SPOTIFY_CLIENT_SECRET");
            if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
                System.err.println("⚠️ Faltan SPOTIFY_CLIENT_ID / SPOTIFY_CLIENT_SECRET, no se pueden resolver links de Spotify.");
                return null;
            }

            try {
                String creds = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://accounts.spotify.com/api/token"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Basic " + creds)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    System.err.println("❌ Error al autenticar con Spotify: " + response.statusCode() + " — " + response.body());
                    return null;
                }
                JSONObject json = new JSONObject(response.body());
                cachedToken = json.getString("access_token");
                tokenExpiry = Instant.now().plusSeconds(Math.max(json.optInt("expires_in", 3600) - 60, 60));
                return cachedToken;
            } catch (Exception e) {
                System.err.println("❌ Error al autenticar con Spotify: " + e.getMessage());
                return null;
            }
        }
    }

    private static JSONObject getJson(String url, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("❌ Error API Spotify (" + url + "): " + response.statusCode());
                return null;
            }
            return new JSONObject(response.body());
        } catch (Exception e) {
            System.err.println("❌ Error al consultar Spotify: " + e.getMessage());
            return null;
        }
    }
}
