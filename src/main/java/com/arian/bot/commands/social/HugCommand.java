package com.arian.bot.commands.social;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;

public class HugCommand {

    private static final List<String> IMAGES = List.of(
            "https://i.imgur.com/Y5TtAvN.png",
            "https://i.imgur.com/7XQE5Mx.png",
            "https://i.imgur.com/J30RK2Z.png"
    );

    private static final String ACTION = "le da un abrazo a";
    private static final String EMOJI = "🤗";
    private static final Color COLOR = new Color(255, 182, 193); // Rosa suave

    public static void handlePrefix(MessageReceivedEvent event) {
        SocialCommand.handlePrefix(event, ACTION, EMOJI, COLOR, IMAGES);
    }

    public static void handleSlash(SlashCommandInteractionEvent event) {
        SocialCommand.handleSlash(event, ACTION, EMOJI, COLOR, IMAGES);
    }
}
