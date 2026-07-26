package com.arian.bot.music;

import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.NodeOptions;
import dev.arbjerg.lavalink.client.event.TrackEndEvent;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Un único LavalinkClient para todo el bot (conectado al servidor Lavalink local, que es quien de
 * verdad envía el audio a Discord), y un TrackScheduler por servidor.
 */
public class MusicManagers {

    private static LavalinkClient client;
    private static final Map<Long, TrackScheduler> schedulers = new ConcurrentHashMap<>();

    /** Se llama una sola vez al arrancar el bot, antes de construir JDA. */
    public static LavalinkClient init(long botUserId, String lavalinkPassword) {
        client = new LavalinkClient(botUserId);
        client.addNode(new NodeOptions.Builder()
                .setName("local")
                .setServerUri(URI.create("ws://127.0.0.1:2333"))
                .setPassword(lavalinkPassword)
                .build());

        client.on(TrackEndEvent.class).subscribe(event -> {
            TrackScheduler scheduler = schedulers.get(event.getGuildId());
            if (scheduler != null) scheduler.onTrackEnd(event.getEndReason());
        });

        return client;
    }

    public static TrackScheduler get(String guildId) {
        long id = Long.parseLong(guildId);
        return schedulers.computeIfAbsent(id, k -> new TrackScheduler(client.getOrCreateLink(id)));
    }

    public static TrackScheduler getIfExists(String guildId) {
        return schedulers.get(Long.parseLong(guildId));
    }

    public static void remove(String guildId) {
        TrackScheduler scheduler = schedulers.remove(Long.parseLong(guildId));
        if (scheduler != null) {
            scheduler.link().destroy().subscribe(v -> {}, err -> {});
        }
    }
}
