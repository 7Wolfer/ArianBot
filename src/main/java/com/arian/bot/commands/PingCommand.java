package com.arian.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PingCommand {

    public static void handleSlash(SlashCommandInteractionEvent event) {
        long latency = event.getJDA().getGatewayPing();
        event.reply("🏓 ¡Pong! Latencia: " + latency + "ms").queue();
    }

    public static void handlePrefix(MessageReceivedEvent event) {
        long latency = event.getJDA().getGatewayPing();
        event.getChannel().sendMessage("🏓 ¡Pong! Latencia: " + latency + "ms").queue();
    }
}