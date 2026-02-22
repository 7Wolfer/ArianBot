package com.arian.bot.commands.social;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;

public class PatCommand {

    private static final List<String> IMAGES = List.of(
            "https://i.imgur.com/uJHzC9A.png",
            "https://i.imgur.com/2sGSsjD.png",
            "https://i.imgur.com/tqGzS6m.gif"
    );

    private static final String ACTION = "le da un pat a";
    private static final String EMOJI = "🙏";
    private static final Color COLOR = new Color(135, 206, 235); // Azul cielo

    public static void handlePrefix(MessageReceivedEvent event) {
        SocialCommand.handlePrefix(event, ACTION, EMOJI, COLOR, IMAGES);
    }

    public static void handleSlash(SlashCommandInteractionEvent event) {
        SocialCommand.handleSlash(event, ACTION, EMOJI, COLOR, IMAGES);
    }
}