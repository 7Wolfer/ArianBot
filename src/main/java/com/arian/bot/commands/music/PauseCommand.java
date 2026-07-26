package com.arian.bot.commands.music;

import com.arian.bot.music.GuildMusicManager;
import com.arian.bot.music.MusicManagers;
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
        GuildMusicManager music = MusicManagers.getIfExists(guildId);
        if (music == null || music.scheduler.nowPlaying() == null) return "No hay nada sonando ahorita.";
        boolean paused = music.scheduler.togglePause();
        return paused ? "Pausado. ⏸️" : "Reanudado. ▶️";
    }
}
