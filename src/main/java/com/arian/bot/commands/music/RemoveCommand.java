package com.arian.bot.commands.music;

import com.arian.bot.music.GuildMusicManager;
import com.arian.bot.music.MusicManagers;
import com.arian.bot.music.QueuedTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

/** Quita una canción de la cola por su posición (según /queue). Slash: /remove posicion:N · Prefix: a!remove N (alias: a!rm) */
public class RemoveCommand {

    public static void handleSlash(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("posicion");
        event.reply(remove(event.getGuild().getId(), option != null ? (int) option.getAsLong() : -1)).queue();
    }

    public static void handlePrefix(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("úsalo así: `a!remove <posición>` (mira los números con `a!queue`)").queue();
            return;
        }
        int position = parse(args[1]);
        event.getChannel().sendMessage(remove(event.getGuild().getId(), position)).queue();
    }

    private static String remove(String guildId, int position) {
        if (position < 1) return "Dame un número de posición válido (mira `/queue` para los números).";
        GuildMusicManager music = MusicManagers.getIfExists(guildId);
        if (music == null) return "No hay ninguna cola activa.";
        QueuedTrack removed = music.scheduler.removeAt(position - 1);
        if (removed == null) return "No hay ninguna canción en esa posición.";
        return "Quité de la cola: **" + removed.title + "**";
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
