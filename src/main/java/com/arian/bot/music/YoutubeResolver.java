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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Resuelve canciones y playlists de YouTube con yt-dlp: se actualiza mucho más rápido que cualquier
 * librería Java ante los cambios de YouTube. yt-dlp descarga el audio a un archivo local y Lavalink
 * lo reproduce desde ahí (en vez de pedirle a Lavalink la URL directa de googlevideo, que YouTube
 * empezó a bloquear con 403 de forma intermitente cuando el que la pide no es el propio yt-dlp).
 *
 * Si existe ~/yt-cookies.txt se usa para evitar el bloqueo de "confirma que no eres un robot" de
 * YouTube (mismo mecanismo que ya usa el comando de descarga).
 */
public class YoutubeResolver {

    private static final int TIMEOUT_SECONDS = 45;

    public static boolean isPlaylistUrl(String query) {
        return query.contains("list=") && !query.contains("watch?v=");
    }

    /** Resuelve una sola canción (nombre o link) con su audio ya listo para reproducir. Null si falla. */
    public static QueuedTrack resolve(Link link, String query, String requestedBy) {
        String ytdlpQuery = looksLikeUrl(query) ? query : "ytsearch1:" + query;
        Downloaded dl = downloadAudio(ytdlpQuery);
        if (dl == null) return null;

        QueuedTrack queued = new QueuedTrack(query, dl.title, dl.author, dl.durationMs, requestedBy);
        queued.localFile = dl.file;
        queued.resolved = loadAudioTrack(link, dl.file, dl.title);
        if (queued.resolved == null) {
            dl.file.delete();
            return null;
        }
        return queued;
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
        Downloaded dl = downloadAudio(queued.query);
        if (dl == null) return false;
        queued.localFile = dl.file;
        queued.resolved = loadAudioTrack(link, dl.file, dl.title);
        if (queued.resolved == null) {
            dl.file.delete();
            queued.localFile = null;
            return false;
        }
        return true;
    }

    private static final class Downloaded {
        final File file;
        final String title;
        final String author;
        final long durationMs;

        Downloaded(File file, String title, String author, long durationMs) {
            this.file = file;
            this.title = title;
            this.author = author;
            this.durationMs = durationMs;
        }
    }

    /** Descarga el audio de una canción a un archivo temporal y devuelve su metadata. Null si falla. */
    private static Downloaded downloadAudio(String ytdlpQuery) {
        try {
            Path dir = Files.createTempDirectory("arian-music-");
            List<String> cmd = baseCommand();
            cmd.add("-f");
            cmd.add("bestaudio");
            cmd.add("--no-playlist");
            cmd.add("-o");
            cmd.add(dir.resolve("%(id)s.%(ext)s").toString());
            cmd.add("--print");
            cmd.add("%(title)s");
            cmd.add("--print");
            cmd.add("%(uploader)s");
            cmd.add("--print");
            cmd.add("%(duration)s");
            cmd.add("--print");
            cmd.add("after_move:filepath");
            cmd.add(ytdlpQuery);

            String output = runProcess(cmd);
            if (output == null) return null;
            String[] lines = output.split("\n", -1);
            if (lines.length < 4) return null;

            File file = new File(lines[3].trim());
            if (!file.isFile()) return null;

            double duration;
            try {
                duration = Double.parseDouble(lines[2].trim());
            } catch (NumberFormatException e) {
                duration = 0;
            }
            String title = lines[0].isBlank() ? "Desconocido" : lines[0];
            String author = lines[1].isBlank() ? "Desconocido" : lines[1];
            return new Downloaded(file, title, author, (long) (duration * 1000));
        } catch (Exception e) {
            System.err.println("❌ Error al descargar con yt-dlp: " + e.getMessage());
            return null;
        }
    }

    private static Track loadAudioTrack(Link link, File file, String title) {
        try {
            LavalinkLoadResult result = link.loadItem(file.getAbsolutePath()).block(java.time.Duration.ofSeconds(TIMEOUT_SECONDS));
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
        // Evita que yt-dlp baje el HTML completo del watch page y los configs extra;
        // no se necesitan para resolver el audio, y ahorra ~1s por canción.
        cmd.add("--extractor-args");
        cmd.add("youtube:skip=webpage,configs");
        return cmd;
    }

    private static String ytDlpPath() {
        File local = new File(System.getProperty("user.home"), ".local/bin/yt-dlp");
        return local.isFile() ? local.getAbsolutePath() : "yt-dlp";
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
