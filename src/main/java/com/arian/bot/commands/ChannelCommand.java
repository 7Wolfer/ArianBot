package com.arian.bot.commands;

import com.arian.bot.DataBaseManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class ChannelCommand {

    public static void handlePrefix(MessageReceivedEvent event, String[] args) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.getChannel().sendMessage("Este comando es solo para administradores.").queue();
            return;
        }

        // Sin argumentos → listar canales activos
        if (args.length < 2) {
            event.getChannel().sendMessage(buildList(event.getGuild())).queue();
            return;
        }

        // Extraer ID del canal mencionado (#canal → <#123456>)
        String raw = args[1];
        String channelId = raw.replaceAll("[^0-9]", "");
        if (channelId.isBlank()) {
            event.getChannel().sendMessage("Menciona un canal válido, por ejemplo: `a!channel #general`").queue();
            return;
        }

        boolean activado = DataBaseManager.toggleArianChannel(channelId);
        String nombre = resolveChannelName(event.getGuild(), channelId);
        event.getChannel().sendMessage(
                activado ? "Arian ahora hablará en " + nombre + "."
                         : "Arian ya no hablará en " + nombre + "."
        ).queue();
    }

    public static void handleSlash(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Este comando es solo para administradores.").setEphemeral(true).queue();
            return;
        }

        var option = event.getOption("canal");

        // Sin argumento → listar
        if (option == null) {
            event.reply(buildList(event.getGuild())).setEphemeral(true).queue();
            return;
        }

        TextChannel canal = option.getAsChannel().asTextChannel();
        boolean activado = DataBaseManager.toggleArianChannel(canal.getId());
        event.reply(
                activado ? "Arian ahora hablará en " + canal.getAsMention() + "."
                         : "Arian ya no hablará en " + canal.getAsMention() + "."
        ).setEphemeral(true).queue();
    }

    private static String buildList(net.dv8tion.jda.api.entities.Guild guild) {
        List<String> ids = DataBaseManager.getArianChannels();
        if (ids.isEmpty()) return "Arian no tiene ningún canal activo todavía.";

        StringBuilder sb = new StringBuilder("Canales donde Arian puede hablar:\n");
        for (String id : ids) sb.append("• ").append(resolveChannelName(guild, id)).append("\n");
        return sb.toString().trim();
    }

    private static String resolveChannelName(net.dv8tion.jda.api.entities.Guild guild, String id) {
        if (guild == null) return "<#" + id + ">";
        var ch = guild.getTextChannelById(id);
        return ch != null ? ch.getAsMention() : "<#" + id + ">";
    }
}
