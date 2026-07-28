package com.arian.bot.listeners;

import com.arian.bot.music.MusicManagers;
import com.arian.bot.music.TrackScheduler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Si Arian se queda solo en un canal de voz (todos se salen), se desconecta pasados IDLE_MINUTES. */
public class VoiceIdleListener extends ListenerAdapter {

    private static final long IDLE_MINUTES = 7;

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Map<Long, ScheduledFuture<?>> pendingDisconnects = new ConcurrentHashMap<>();

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        var voiceState = guild.getSelfMember().getVoiceState();
        AudioChannel botChannel = voiceState != null ? voiceState.getChannel() : null;

        if (botChannel == null) {
            cancelPending(guild.getIdLong());
            return;
        }

        if (isAlone(botChannel)) {
            scheduleDisconnect(guild, botChannel.getIdLong());
        } else {
            cancelPending(guild.getIdLong());
        }
    }

    private static boolean isAlone(AudioChannel channel) {
        return channel.getMembers().stream().allMatch(m -> m.getUser().isBot());
    }

    private static void scheduleDisconnect(Guild guild, long channelId) {
        long guildId = guild.getIdLong();
        pendingDisconnects.computeIfAbsent(guildId, id -> scheduler.schedule(() -> {
            pendingDisconnects.remove(guildId);
            // Revisa de nuevo justo antes de desconectar, por si alguien volvió a entrar mientras esperaba.
            var voiceState = guild.getSelfMember().getVoiceState();
            AudioChannel current = voiceState != null ? voiceState.getChannel() : null;
            if (current == null || current.getIdLong() != channelId || !isAlone(current)) return;

            TrackScheduler trackScheduler = MusicManagers.getIfExists(guild.getId());
            if (trackScheduler != null) trackScheduler.stopAndClear();
            guild.getJDA().getDirectAudioController().disconnect(guild);
            MusicManagers.remove(guild.getId());
        }, IDLE_MINUTES, TimeUnit.MINUTES));
    }

    private static void cancelPending(long guildId) {
        ScheduledFuture<?> pending = pendingDisconnects.remove(guildId);
        if (pending != null) pending.cancel(false);
    }
}
