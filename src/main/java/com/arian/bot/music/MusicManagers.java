package com.arian.bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Un único AudioPlayerManager compartido por todo el bot (solo necesita el source manager HTTP,
 * porque el audio de YouTube se resuelve aparte con yt-dlp), y un GuildMusicManager por servidor.
 */
public class MusicManagers {

    public static final AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();

    static {
        PLAYER_MANAGER.registerSourceManager(new HttpAudioSourceManager());
    }

    private static final Map<String, GuildMusicManager> managers = new ConcurrentHashMap<>();

    public static GuildMusicManager get(String guildId) {
        return managers.computeIfAbsent(guildId, id -> new GuildMusicManager(PLAYER_MANAGER));
    }

    public static GuildMusicManager getIfExists(String guildId) {
        return managers.get(guildId);
    }

    public static void remove(String guildId) {
        GuildMusicManager manager = managers.remove(guildId);
        if (manager != null) manager.player.destroy();
    }
}
