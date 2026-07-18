package com.arian.bot.commands;

import com.arian.bot.Main;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Descarga videos (TikTok, Instagram, YouTube, etc.) con yt-dlp y los re-sube al canal.
 *   Slash:  /download url:&lt;link&gt;
 *   Prefix: a!download &lt;link&gt;   (alias: a!dl, a!descargar)
 *
 * Requiere que yt-dlp (y ffmpeg) estén en el PATH del proceso del bot.
 * Nota: algunos sitios (p. ej. YouTube desde una IP de VPS) pueden bloquear la descarga.
 */
public class DownloadCommand {

    // Margen para no pasarnos del límite real de subida de Discord (overhead del multipart, etc.)
    private static final long SIZE_MARGIN = 512 * 1024;          // 512 KB
    private static final long DEFAULT_LIMIT = 10L * 1024 * 1024; // 10 MB (fallback si no es un guild)
    private static final long TIMEOUT_SECONDS = 120;

    // Las descargas se hacen fuera del hilo de eventos de JDA
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void handleSlash(SlashCommandInteractionEvent event) {
        String url = event.getOption("url").getAsString().trim();
        long limit = event.getGuild() != null ? event.getGuild().getMaxFileSize() : DEFAULT_LIMIT;

        if (!esUrlValida(url)) {
            event.reply("eso no parece un link, pásame una url de tiktok/insta/youtube y esas cosas")
                    .setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();
        executor.submit(() -> {
            Resultado r = descargar(url, limit);
            if (r.file != null) {
                event.getHook().sendFiles(FileUpload.fromData(r.file, r.file.getName())).queue(
                        m -> limpiar(r.tempDir),
                        err -> { event.getHook().sendMessage("se subió mal, inténtalo otra vez").queue(); limpiar(r.tempDir); }
                );
            } else {
                event.getHook().sendMessage(r.error).queue();
            }
        });
    }

    public static void handlePrefix(MessageReceivedEvent event, String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            event.getChannel().sendMessage("úsalo así: `" + Main.PREFIX + "download <link>` (tiktok, insta, youtube...)").queue();
            return;
        }
        String url = args[1].trim();
        long limit = event.isFromGuild() ? event.getGuild().getMaxFileSize() : DEFAULT_LIMIT;

        if (!esUrlValida(url)) {
            event.getMessage().reply("eso no parece un link válido").mentionRepliedUser(false).queue();
            return;
        }

        event.getChannel().sendTyping().queue();
        executor.submit(() -> {
            Resultado r = descargar(url, limit);
            if (r.file != null) {
                event.getMessage().replyFiles(FileUpload.fromData(r.file, r.file.getName())).mentionRepliedUser(false).queue(
                        m -> limpiar(r.tempDir),
                        err -> { event.getChannel().sendMessage("se subió mal, inténtalo otra vez").queue(); limpiar(r.tempDir); }
                );
            } else {
                event.getMessage().reply(r.error).mentionRepliedUser(false).queue();
            }
        });
    }

    // ───────────────────────────── lógica ─────────────────────────────

    private static boolean esUrlValida(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private static Resultado descargar(String url, long discordLimit) {
        long maxBytes = Math.max(discordLimit - SIZE_MARGIN, 1);
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("arian-dl-");
            File log = tempDir.resolve("_ytdlp.log").toFile();

            List<String> cmd = new ArrayList<>(List.of(
                    "yt-dlp",
                    "--no-playlist",
                    "--no-warnings",
                    "--max-filesize", String.valueOf(maxBytes),
                    "--merge-output-format", "mp4",
                    "-f", "best[filesize<=" + maxBytes + "]/best[filesize_approx<=" + maxBytes + "]/best",
                    "-o", tempDir.resolve("%(id)s.%(ext)s").toString()
            ));
            // Si existe ~/yt-cookies.txt, se usa para sitios que requieren sesión
            // (p. ej. YouTube, que bloquea las IPs de VPS sin autenticación).
            File cookies = new File(System.getProperty("user.home"), "yt-cookies.txt");
            if (cookies.isFile()) {
                cmd.add("--cookies");
                cmd.add(cookies.getAbsolutePath());
            }
            cmd.add(url);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectOutput(log); // evita bloqueos por buffer de pipe lleno en descargas largas

            Process p = pb.start();
            boolean termino = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!termino) {
                p.destroyForcibly();
                return Resultado.error("se tardó demasiado (el video debe ser muy pesado)", tempDir);
            }

            if (p.exitValue() != 0) {
                String salida = leerLog(log).toLowerCase();
                if (salida.contains("larger than") || salida.contains("max-filesize")) {
                    return Resultado.error("ese video es muy pesado para Discord (límite " + mb(discordLimit) + " MB)", tempDir);
                }
                System.err.println("yt-dlp falló (" + url + "):\n" + leerLog(log));
                return Resultado.error("no pude descargar ese link (¿privado, borrado, o el sitio no deja?)", tempDir);
            }

            File video = archivoMasGrande(tempDir);
            if (video == null) {
                return Resultado.error("no encontré ningún video en ese link", tempDir);
            }
            if (video.length() > discordLimit) {
                return Resultado.error("el video pesa " + mb(video.length()) + " MB y aquí el límite de Discord es " + mb(discordLimit) + " MB", tempDir);
            }

            Resultado ok = new Resultado();
            ok.file = video;
            ok.tempDir = tempDir;
            return ok;

        } catch (Exception e) {
            System.err.println("Error descargando " + url + ": " + e.getMessage());
            return Resultado.error("algo salió mal al descargar, inténtalo de nuevo", tempDir);
        }
    }

    private static File archivoMasGrande(Path dir) throws Exception {
        try (var stream = Files.list(dir)) {
            return stream.map(Path::toFile)
                    .filter(File::isFile)
                    .filter(f -> !f.getName().equals("_ytdlp.log"))
                    .max(Comparator.comparingLong(File::length))
                    .orElse(null);
        }
    }

    private static String leerLog(File log) {
        try {
            return log.exists() ? Files.readString(log.toPath()) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static long mb(long bytes) {
        return bytes / (1024 * 1024);
    }

    /** Borra recursivamente el directorio temporal de la descarga. */
    private static void limpiar(Path tempDir) {
        if (tempDir == null) return;
        try (var stream = Files.walk(tempDir)) {
            stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (Exception ignored) {}
    }

    private static class Resultado {
        File file;
        Path tempDir;
        String error;

        /** Resultado de error: limpia el temporal de una vez y solo lleva el mensaje. */
        static Resultado error(String msg, Path tempDir) {
            limpiar(tempDir);
            Resultado r = new Resultado();
            r.error = msg;
            return r;
        }
    }
}
