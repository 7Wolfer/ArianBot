package com.arian.bot.commands.music;

import com.arian.bot.music.MusicManagers;
import com.arian.bot.music.TrackScheduler;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** Pausa o reanuda la reproducción. Slash: /pause · Prefix: a!pause */
public class PauseCommand {

    public static void handleSlash(SlashCommandInteractionEvent event) {
        event.reply(toggle(event.getGuild().getId())).queue();
    }

    public static void handlePrefix(MessageReceivedEvent event) {
        event.getChannel().sendMessage(toggle(event.getGuild().getId())).queue();
    }

    private static String toggle(String guildId) {
        TrackScheduler scheduler = MusicManagers.getIfExists(guildId);
        if (scheduler == null || scheduler.nowPlaying() == null) return "No hay nada sonando ahorita.";
        boolean paused = scheduler.togglePause();
        return paused ? "Pausado. ⏸️" : "Reanudado. ▶️";
    }
}
