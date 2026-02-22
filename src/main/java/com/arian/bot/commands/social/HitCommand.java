package com.arian.bot.commands.social;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;

public class HitCommand {

    private static final List<String> IMAGES = List.of(
            "https://i.imgur.com/aDv5oa5.png",
            "https://i.imgur.com/C11A39h.png",
            "https://i.imgur.com/yu8Ffro.png"
    );

    private static final String ACTION = "le da un golpe a";
    private static final String EMOJI = "👊";
    private static final Color COLOR = new Color(255, 80, 80); // Rojo

    public static void handlePrefix(MessageReceivedEvent event) {
        SocialCommand.handlePrefix(event, ACTION, EMOJI, COLOR, IMAGES);
    }

    public static void handleSlash(SlashCommandInteractionEvent event) {
        SocialCommand.handleSlash(event, ACTION, EMOJI, COLOR, IMAGES);
    }
}