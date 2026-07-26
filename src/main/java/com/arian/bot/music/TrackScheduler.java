package com.arian.bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Maneja la cola de reproducción de un servidor: agregar, prioridad, quitar, avanzar a la siguiente. */
public class TrackScheduler extends AudioEventAdapter {

    private static final ExecutorService resolverExecutor = Executors.newCachedThreadPool();

    private final AudioPlayer player;
    private final LinkedList<QueuedTrack> queue = new LinkedList<>();
    private volatile QueuedTrack nowPlaying;

    /** Se llama cuando el scheduler quiere avisar algo al canal (ej. "no pude reproducir X, la salto"). */
    public volatile Consumer<String> onNotify;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
    }

    /** Agrega al final de la cola; si no hay nada sonando, arranca de una. */
    public synchronized void queue(QueuedTrack track) {
        if (nowPlaying == null) {
            play(track);
        } else {
            queue.addLast(track);
        }
    }

    /** Agrega al frente de la cola (siguiente en sonar); si no hay nada sonando, arranca de una. */
    public synchronized void queuePriority(QueuedTrack track) {
        if (nowPlaying == null) {
            play(track);
        } else {
            queue.addFirst(track);
        }
    }

    /** Corta la canción actual y avanza a la siguiente ya mismo. */
    public synchronized void skip() {
        player.stopTrack();
        advance();
    }

    /** Vacía la cola y corta la reproducción. */
    public synchronized void stopAndClear() {
        queue.clear();
        nowPlaying = null;
        player.stopTrack();
    }

    public synchronized boolean togglePause() {
        boolean paused = !player.isPaused();
        player.setPaused(paused);
        return paused;
    }

    public synchronized QueuedTrack nowPlaying() {
        return nowPlaying;
    }

    public synchronized List<QueuedTrack> snapshot() {
        return new ArrayList<>(queue);
    }

    public synchronized QueuedTrack removeAt(int position) {
        if (position < 0 || position >= queue.size()) return null;
        return queue.remove(position);
    }

    public synchronized boolean moveToFront(int position) {
        QueuedTrack track = removeAt(position);
        if (track == null) return false;
        queue.addFirst(track);
        return true;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (!endReason.mayStartNext) return;
        synchronized (this) {
            advance();
        }
    }

    private void advance() {
        nowPlaying = null;
        QueuedTrack next = queue.pollFirst();
        if (next != null) play(next);
    }

    private void play(QueuedTrack track) {
        nowPlaying = track;
        if (track.resolved != null) {
            player.playTrack(track.resolved);
            return;
        }
        // Viene de una playlist y todavía no se resolvió: se hace justo ahora, sin bloquear el hilo de eventos.
        resolverExecutor.submit(() -> {
            boolean ok = YoutubeResolver.resolveAudio(track);
            synchronized (this) {
                if (nowPlaying != track) return; // se saltó mientras se resolvía
                if (ok) {
                    player.playTrack(track.resolved);
                } else {
                    notify("No pude reproducir **" + track.title + "**, la salto.");
                    advance();
                }
            }
        });
    }

    private void notify(String message) {
        Consumer<String> handler = onNotify;
        if (handler != null) handler.accept(message);
    }
}
