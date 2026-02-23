package com.arian.bot.commands.social;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
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
    private static final String ACTION_KEY = "hug";
    private static final String EMOJI = "🤗";
    private static final Color COLOR = new Color(255, 182, 193);

    public static void handlePrefix(MessageReceivedEvent event) {
        SocialCommand.handlePrefix(event, ACTION, ACTION_KEY, EMOJI, COLOR, IMAGES, false, true, "🤗 Abrazar de vuelta");
    }

    public static void handleSlash(SlashCommandInteractionEvent event) {
        SocialCommand.handleSlash(event, ACTION, ACTION_KEY, EMOJI, COLOR, IMAGES, false, true, "🤗 Abrazar de vuelta");
    }

    public static void handleButton(ButtonInteractionEvent event, String targetId) {
        event.getGuild().retrieveMemberById(targetId).queue(target -> {
            Member author = event.getMember();
            SocialCommand.handleButton(event, ACTION, ACTION_KEY, EMOJI, COLOR, IMAGES, false, true, "🤗 Abrazar de vuelta", target);
        });
    }
}