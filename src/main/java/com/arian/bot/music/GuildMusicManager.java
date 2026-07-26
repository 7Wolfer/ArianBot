package com.arian.bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

/** Agrupa el reproductor, la cola y el handler de envío de audio de un servidor. */
public class GuildMusicManager {
    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final AudioPlayerSendHandler sendHandler;

    public GuildMusicManager(AudioPlayerManager playerManager) {
        player = playerManager.createPlayer();
        scheduler = new TrackScheduler(player);
        player.addListener(scheduler);
        sendHandler = new AudioPlayerSendHandler(player);
    }
}
