package com.arian.bot.commands.music;

import com.arian.bot.music.GuildMusicManager;
import com.arian.bot.music.MusicManagers;
import com.arian.bot.music.QueuedTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

/** Muestra qué está sonando y la cola. Slash: /queue · Prefix: a!queue (alias: a!q, a!cola) */
public class QueueCommand {

    private static final int MAX_SHOWN = 15;

    public static void handleSlash(SlashCommandInteractionEvent event) {
        event.reply(build(event.getGuild().getId())).queue();
    }

    public static void handlePrefix(MessageReceivedEvent event) {
        event.getChannel().sendMessage(build(event.getGuild().getId())).queue();
    }

    private static String build(String guildId) {
        GuildMusicManager music = MusicManagers.getIfExists(guildId);
        if (music == null || (music.scheduler.nowPlaying() == null && music.scheduler.snapshot().isEmpty())) {
            return "No hay nada en la cola. Pide algo con `/play` o `a!play <canción>`.";
        }

        StringBuilder sb = new StringBuilder();
        QueuedTrack current = music.scheduler.nowPlaying();
        if (current != null) {
            sb.append("🎶 Sonando ahora: **").append(current.title).append("** (")
              .append(current.formattedDuration()).append(") — pedida por ").append(current.requestedBy).append("\n\n");
        }

        List<QueuedTrack> queue = music.scheduler.snapshot();
        if (queue.isEmpty()) {
            sb.append("La cola está vacía.");
            return sb.toString();
        }

        sb.append("**En cola:**\n");
        int shown = Math.min(queue.size(), MAX_SHOWN);
        for (int i = 0; i < shown; i++) {
            QueuedTrack t = queue.get(i);
            sb.append(i + 1).append(". ").append(t.title).append(" (").append(t.formattedDuration())
              .append(") — ").append(t.requestedBy).append("\n");
        }
        if (queue.size() > MAX_SHOWN) {
            sb.append("… y ").append(queue.size() - MAX_SHOWN).append(" más.");
        }
        return sb.toString();
    }
}
