package com.arian.bot.music;

import dev.arbjerg.lavalink.client.player.Track;

/**
 * Una entrada en la cola de reproducción. {@code resolved} es el audio real ya listo para sonar;
 * puede ser null si viene de una playlist y todavía no le toca (se resuelve justo antes de reproducirla).
 */
public class QueuedTrack {
    public final String query;   // lo que hay que resolver con yt-dlp (link de YouTube)
    public final String title;
    public final String author;
    public final long durationMs; // 0 si se desconoce
    public final String requestedBy;
    public volatile Track resolved;

    public QueuedTrack(String query, String title, String author, long durationMs, String requestedBy) {
        this.query = query;
        this.title = title;
        this.author = author;
        this.durationMs = durationMs;
        this.requestedBy = requestedBy;
    }

    public String formattedDuration() {
        if (durationMs <= 0) return "?:??";
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : seconds);
    }
}
