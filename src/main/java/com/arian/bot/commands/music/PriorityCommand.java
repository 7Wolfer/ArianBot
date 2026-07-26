package com.arian.bot.commands.music;

import com.arian.bot.music.GuildMusicManager;
import com.arian.bot.music.MusicManagers;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

/** Mueve una canción de la cola al frente, para que suene siguiente. Slash: /priority posicion:N · Prefix: a!priority N (alias: a!prio) */
public class PriorityCommand {

    public static void handleSlash(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("posicion");
        event.reply(prioritize(event.getGuild().getId(), option != null ? (int) option.getAsLong() : -1)).queue();
    }

    public static void handlePrefix(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("úsalo así: `a!priority <posición>` (mira los números con `a!queue`)").queue();
            return;
        }
        int position = parse(args[1]);
        event.getChannel().sendMessage(prioritize(event.getGuild().getId(), position)).queue();
    }

    private static String prioritize(String guildId, int position) {
        if (position < 1) return "Dame un número de posición válido (mira `/queue` para los números).";
        GuildMusicManager music = MusicManagers.getIfExists(guildId);
        if (music == null) return "No hay ninguna cola activa.";
        boolean moved = music.scheduler.moveToFront(position - 1);
        if (!moved) return "No hay ninguna canción en esa posición.";
        return "Listo, esa es la siguiente en sonar. ⏫";
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
