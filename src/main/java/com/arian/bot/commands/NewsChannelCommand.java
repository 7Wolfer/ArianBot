package com.arian.bot.commands;

import com.arian.bot.DataBaseManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** Configura el canal donde Arian postea el digest de noticias (programación y neurociencia). */
public class NewsChannelCommand {

    public static void handlePrefix(MessageReceivedEvent event, String[] args) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.getChannel().sendMessage("Este comando es solo para administradores.").queue();
            return;
        }

        if (args.length < 2) {
            String current = DataBaseManager.getNewsChannel(event.getGuild().getId());
            event.getChannel().sendMessage(current == null
                    ? "No hay canal de noticias configurado. Úsalo así: `a!newschannel #canal`"
                    : "El canal de noticias actual es " + resolveChannelName(event.getGuild(), current) + ".").queue();
            return;
        }

        String channelId = args[1].replaceAll("[^0-9]", "");
        if (channelId.isBlank()) {
            event.getChannel().sendMessage("Menciona un canal válido, por ejemplo: `a!newschannel #noticias`").queue();
            return;
        }

        DataBaseManager.setNewsChannel(event.getGuild().getId(), channelId);
        event.getChannel().sendMessage("Listo, ahora posteo noticias en " + resolveChannelName(event.getGuild(), channelId) + ".").queue();
    }

    public static void handleSlash(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Este comando es solo para administradores.").setEphemeral(true).queue();
            return;
        }

        var option = event.getOption("canal");
        if (option == null) {
            String current = DataBaseManager.getNewsChannel(event.getGuild().getId());
            event.reply(current == null
                    ? "No hay canal de noticias configurado todavía."
                    : "El canal de noticias actual es " + resolveChannelName(event.getGuild(), current) + ".").setEphemeral(true).queue();
            return;
        }

        TextChannel canal = option.getAsChannel().asTextChannel();
        DataBaseManager.setNewsChannel(event.getGuild().getId(), canal.getId());
        event.reply("Listo, ahora posteo noticias en " + canal.getAsMention() + ".").setEphemeral(true).queue();
    }

    private static String resolveChannelName(Guild guild, String id) {
        TextChannel ch = guild.getTextChannelById(id);
        return ch != null ? ch.getAsMention() : "<#" + id + ">";
    }
}
