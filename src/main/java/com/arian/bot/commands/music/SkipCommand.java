package com.arian.bot.commands.music;

import com.arian.bot.music.GuildMusicManager;
import com.arian.bot.music.MusicManagers;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** Salta la canción actual. Slash: /skip · Prefix: a!skip (alias: a!s, a!next) */
public class SkipCommand {

    public static void handleSlash(SlashCommandInteractionEvent event) {
        event.reply(skip(event.getGuild().getId())).queue();
    }

    public static void handlePrefix(MessageReceivedEvent event) {
        event.getChannel().sendMessage(skip(event.getGuild().getId())).queue();
    }

    private static String skip(String guildId) {
        GuildMusicManager music = MusicManagers.getIfExists(guildId);
        if (music == null || music.scheduler.nowPlaying() == null) return "No hay nada sonando ahorita.";
        music.scheduler.skip();
        return "Saltado. ⏭️";
    }
}
