package com.arian.bot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class OwnerCommand {

    private static final String OWNER_ID = "607845736016773131";

    public static void handleGuilds(MessageReceivedEvent event) {
        if (!event.getAuthor().getId().equals(OWNER_ID)) return;

        List<Guild> guilds = event.getJDA().getGuilds();
        StringBuilder sb = new StringBuilder("**Servidores donde está Arian (" + guilds.size() + "):**\n");
        for (Guild g : guilds) {
            sb.append("• `").append(g.getId()).append("` — **").append(g.getName()).append("**")
              .append(" (").append(g.getMemberCount()).append(" miembros)\n");
        }
        event.getChannel().sendMessage(sb.toString()).queue();
    }

    public static void handleLeave(MessageReceivedEvent event, String[] args) {
        if (!event.getAuthor().getId().equals(OWNER_ID)) return;

        if (args.length < 2) {
            event.getChannel().sendMessage("Uso: `a!leave <id del servidor>`").queue();
            return;
        }

        String guildId = args[1].trim();
        Guild guild = event.getJDA().getGuildById(guildId);
        if (guild == null) {
            event.getChannel().sendMessage("No encontré ningún servidor con ese ID.").queue();
            return;
        }

        String nombre = guild.getName();
        guild.leave().queue(
            success -> event.getChannel().sendMessage("Salí del servidor **" + nombre + "**.").queue(),
            error   -> event.getChannel().sendMessage("No pude salir: " + error.getMessage()).queue()
        );
    }
}
