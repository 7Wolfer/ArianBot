package com.arian.bot.music;

import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.LavalinkLoadResult;
import dev.arbjerg.lavalink.client.player.LoadFailed;
import dev.arbjerg.lavalink.client.player.Track;
import dev.arbjerg.lavalink.client.player.TrackLoaded;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Resuelve canciones y playlists de YouTube con yt-dlp: se actualiza mucho más rápido que cualquier
 * librería Java ante los cambios de YouTube, así que se usa solo para conseguir la URL directa del
 * audio; Lavalink (el servidor de audio) se encarga de reproducirla de verdad.
 *
 * Si existe ~/yt-cookies.txt se usa para evitar el bloqueo de "confirma que no eres un robot" de
 * YouTube (mismo mecanismo que ya usa el comando de descarga).
 */
public class YoutubeResolver {

    private static final int TIMEOUT_SECONDS = 30;

    public static boolean isPlaylistUrl(String query) {
        return query.contains("list=") && !query.contains("watch?v=");
    }

    /** Resuelve una sola canción (nombre o link) con su audio ya listo para reproducir. Null si falla. */
    public static QueuedTrack resolve(Link link, String query, String requestedBy) {
        String ytdlpQuery = looksLikeUrl(query) ? query : "ytsearch1:" + query;
        JSONObject info = runYtDlpJson(ytdlpQuery);
        if (info == null) return null;

        String url = info.optString("url", "");
        if (url.isBlank()) return null;

        QueuedTrack queued = new QueuedTrack(
                info.optString("webpage_url", query),
                info.optString("title", "Desconocido"),
                info.optString("uploader", "Desconocido"),
                (long) (info.optDouble("duration", 0) * 1000),
                requestedBy
        );
        queued.resolved = loadAudioTrack(link, url, queued.title);
        return queued.resolved != null ? queued : null;
    }

    /** Listado rápido (sin resolver audio todavía) de los videos de una playlist. */
    public static List<QueuedTrack> listPlaylist(String url, String requestedBy, int limit) {
        List<QueuedTrack> tracks = new ArrayList<>();
        try {
            List<String> cmd = baseCommand();
            cmd.add("--flat-playlist");
            cmd.add("-J");
            cmd.add("--playlist-end");
            cmd.add(String.valueOf(limit));
            cmd.add(url);

            String output = runProcess(cmd);
            if (output == null || output.isBlank()) return tracks;

            JSONObject root = new JSONObject(output);
            JSONArray entries = root.optJSONArray("entries");
            if (entries == null) return tracks;

            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                String id = entry.optString("id", null);
                if (id == null) continue;
                tracks.add(new QueuedTrack(
                        "https://www.youtube.com/watch?v=" + id,
                        entry.optString("title", "Desconocido"),
                        entry.optString("uploader", "Desconocido"),
                        (long) (entry.optDouble("duration", 0) * 1000),
                        requestedBy
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Error al listar playlist: " + e.getMessage());
        }
        return tracks;
    }

    /** Resuelve el audio real de una entrada de cola pendiente (de una playlist), justo antes de sonar. */
    public static boolean resolveAudio(Link link, QueuedTrack queued) {
        JSONObject info = runYtDlpJson(queued.query);
        if (info == null) return false;
        String url = info.optString("url", "");
        if (url.isBlank()) return false;
        queued.resolved = loadAudioTrack(link, url, queued.title);
        return queued.resolved != null;
    }

    private static Track loadAudioTrack(Link link, String url, String title) {
        try {
            LavalinkLoadResult result = link.loadItem(url).block(java.time.Duration.ofSeconds(TIMEOUT_SECONDS));
            if (result instanceof TrackLoaded loaded) {
                return loaded.getTrack();
            }
            if (result instanceof LoadFailed failed) {
                System.err.println("❌ Error al cargar audio (" + title + "): " + failed.getException().getMessage());
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error al cargar audio (" + title + "): " + e.getMessage());
            return null;
        }
    }

    private static boolean looksLikeUrl(String s) {
        return s.startsWith("http://") || s.startsWith("https://");
    }

    private static JSONObject runYtDlpJson(String query) {
        try {
            List<String> cmd = baseCommand();
            cmd.add("-f");
            cmd.add("bestaudio");
            cmd.add("-j");
            cmd.add("--no-playlist");
            cmd.add(query);
            String output = runProcess(cmd);
            return output == null || output.isBlank() ? null : new JSONObject(firstLine(output));
        } catch (Exception e) {
            System.err.println("❌ Error al resolver con yt-dlp: " + e.getMessage());
            return null;
        }
    }

    private static List<String> baseCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(ytDlpPath());
        File cookies = new File(System.getProperty("user.home"), "yt-cookies.txt");
        if (cookies.isFile()) {
            cmd.add("--cookies");
            cmd.add(cookies.getAbsolutePath());
        }
        // yt-dlp necesita un runtime de JavaScript para resolver la firma de varios videos;
        // se pasa la ruta explícita porque ~/.deno/bin no está en el PATH del proceso del bot.
        File deno = new File(System.getProperty("user.home"), ".deno/bin/deno");
        if (deno.isFile()) {
            cmd.add("--js-runtimes");
            cmd.add("deno:" + deno.getAbsolutePath());
        }
        return cmd;
    }

    private static String ytDlpPath() {
        File local = new File(System.getProperty("user.home"), ".local/bin/yt-dlp");
        return local.isFile() ? local.getAbsolutePath() : "yt-dlp";
    }

    private static String firstLine(String s) {
        int nl = s.indexOf('\n');
        return nl == -1 ? s : s.substring(0, nl);
    }

    private static String runProcess(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
        }

        boolean finished = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            return null;
        }
        return p.exitValue() == 0 ? out.toString() : null;
    }
}
