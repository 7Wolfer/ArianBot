package com.arian.bot.music;

import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.LavalinkPlayer;
import dev.arbjerg.lavalink.protocol.v4.Message;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Maneja la cola de reproducción de un servidor: agregar, prioridad, quitar, avanzar a la siguiente. */
public class TrackScheduler {

    private static final ExecutorService resolverExecutor = Executors.newCachedThreadPool();

    private final Link link;
    private final LinkedList<QueuedTrack> queue = new LinkedList<>();
    private volatile QueuedTrack nowPlaying;

    /** Se llama cuando el scheduler quiere avisar algo al canal (ej. "no pude reproducir X, la salto"). */
    public volatile Consumer<String> onNotify;

    public TrackScheduler(Link link) {
        this.link = link;
    }

    public Link link() {
        return link;
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
        fireAndForget(link.createOrUpdatePlayer().stopTrack(), "saltar canción");
        advance();
    }

    /** Vacía la cola y corta la reproducción. */
    public synchronized void stopAndClear() {
        cleanupFile(nowPlaying);
        queue.clear();
        nowPlaying = null;
        fireAndForget(link.createOrUpdatePlayer().stopTrack(), "detener reproducción");
    }

    public synchronized boolean togglePause() {
        LavalinkPlayer cached = link.getCachedPlayer();
        boolean paused = cached == null || !cached.getPaused();
        fireAndForget(link.createOrUpdatePlayer().setPaused(paused), "pausar/reanudar");
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

    /** Llamado por el dispatcher central de MusicManagers cuando termina una canción en este servidor. */
    void onTrackEnd(Message.EmittedEvent.TrackEndEvent.AudioTrackEndReason reason) {
        if (!reason.getMayStartNext()) return;
        synchronized (this) {
            advance();
        }
    }

    private void advance() {
        cleanupFile(nowPlaying);
        nowPlaying = null;
        QueuedTrack next = queue.pollFirst();
        if (next != null) play(next);
    }

    private void play(QueuedTrack track) {
        nowPlaying = track;
        if (track.resolved != null) {
            fireAndForget(link.createOrUpdatePlayer().setTrack(track.resolved), "reproducir canción");
            return;
        }
        // Viene de una playlist y todavía no se resolvió: se hace justo ahora, sin bloquear el hilo de eventos.
        resolverExecutor.submit(() -> {
            boolean ok = YoutubeResolver.resolveAudio(link, track);
            synchronized (this) {
                if (nowPlaying != track) {
                    cleanupFile(track); // se saltó mientras se resolvía/descargaba: el archivo queda huérfano
                    return;
                }
                if (ok) {
                    fireAndForget(link.createOrUpdatePlayer().setTrack(track.resolved), "reproducir canción");
                } else {
                    notify("No pude reproducir **" + track.title + "**, la salto.");
                    advance();
                }
            }
        });
    }

    /** Borra el archivo de audio descargado para esta canción (y su carpeta temporal), si lo tiene. */
    private static void cleanupFile(QueuedTrack track) {
        if (track != null && track.localFile != null) {
            File parent = track.localFile.getParentFile();
            track.localFile.delete();
            track.localFile = null;
            if (parent != null) parent.delete();
        }
    }

    private static void fireAndForget(Mono<?> mono, String action) {
        mono.subscribe(v -> {}, err -> System.err.println("❌ Error al " + action + ": " + err.getMessage()));
    }

    private void notify(String message) {
        Consumer<String> handler = onNotify;
        if (handler != null) handler.accept(message);
    }
}
