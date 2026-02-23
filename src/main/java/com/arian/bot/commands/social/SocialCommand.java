package com.arian.bot.commands.social;

import com.arian.bot.DataBaseManager;
import com.arian.bot.DataBaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class SocialCommand {

    private static final Random RANDOM = new Random();

    public static void handlePrefix(
            MessageReceivedEvent event,
            String action,
            String actionKey,
            String emoji,
            Color color,
            List<String> images,
            boolean isPair,
            boolean hasReturnButton,
            String returnButtonLabel
    ) {
        if (event.getMessage().getMentions().getMembers().isEmpty()) {
            event.getChannel().sendMessage("⚠️ Debes mencionar a alguien. Ejemplo: `a!" + actionKey + " @usuario`").queue();
            return;
        }

        Member author = event.getMember();
        Member target = event.getMessage().getMentions().getMembers().get(0);

        if (author.getId().equals(target.getId())) {
            event.getChannel().sendMessage("⚠️ ¡No puedes hacerte eso a ti mismo!").queue();
            return;
        }

        String imageUrl = images.get(RANDOM.nextInt(images.size()));
        int count;

        if (isPair) {
            count = DataBaseManager.incrementPairCount(author.getId(), target.getId(), actionKey);
        } else {
            count = DataBaseManager.incrementReceivedCount(target.getId(), actionKey);
        }

        String countText = buildCountText(isPair, actionKey, author, target, count);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " " + author.getEffectiveName() + " " + action + " " + target.getEffectiveName() + "!")
                .setDescription(countText)
                .setImage(imageUrl)
                .setColor(color)
                .setFooter("Powered by Arian 🐾");

        if (hasReturnButton) {
            // El ID del botón lleva la info necesaria para saber quién puede usarlo
            String buttonId = actionKey + ":" + target.getId() + ":" + author.getId();
            event.getChannel().sendMessageEmbeds(embed.build())
                    .addActionRow(Button.primary(buttonId, returnButtonLabel))
                    .queue();
        } else {
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        }
    }

    public static void handleSlash(
            SlashCommandInteractionEvent event,
            String action,
            String actionKey,
            String emoji,
            Color color,
            List<String> images,
            boolean isPair,
            boolean hasReturnButton,
            String returnButtonLabel
    ) {
        Member author = event.getMember();
        Member target = event.getOption("usuario").getAsMember();

        if (target == null) {
            event.reply("⚠️ No pude encontrar a ese usuario.").setEphemeral(true).queue();
            return;
        }

        if (author.getId().equals(target.getId())) {
            event.reply("⚠️ ¡No puedes hacerte eso a ti mismo!").setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        String imageUrl = images.get(RANDOM.nextInt(images.size()));
        int count;

        if (isPair) {
            count = DataBaseManager.incrementPairCount(author.getId(), target.getId(), actionKey);
        } else {
            count = DataBaseManager.incrementReceivedCount(target.getId(), actionKey);
        }

        String countText = buildCountText(isPair, actionKey, author, target, count);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " " + author.getEffectiveName() + " " + action + " " + target.getEffectiveName() + "!")
                .setDescription(countText)
                .setImage(imageUrl)
                .setColor(color)
                .setFooter("Powered by Arian 🐾");

        if (hasReturnButton) {
            String buttonId = actionKey + ":" + target.getId() + ":" + author.getId();
            event.getHook().sendMessageEmbeds(embed.build())
                    .addActionRow(Button.primary(buttonId, returnButtonLabel))
                    .queue();
        } else {
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        }
    }

    private static String buildCountText(boolean isPair, String actionKey, Member author, Member target, int count) {
        if (isPair) {
            String actionPast = switch (actionKey) {
                case "kiss" -> "se han besado";
                case "hit" -> "se han golpeado";
                default -> "han interactuado";
            };
            return author.getEffectiveName() + " y " + target.getEffectiveName() + " " + actionPast + " **" + count + "** " + (count == 1 ? "vez" : "veces") + ".";
        } else {
            String actionPast = switch (actionKey) {
                case "hug" -> "abrazos";
                case "pat" -> "pats";
                default -> "interacciones";
            };
            return target.getEffectiveName() + " ha recibido **" + count + "** " + actionPast + ".";
        }
    }
    public static void handleButton(
            ButtonInteractionEvent event,
            String action,
            String actionKey,
            String emoji,
            Color color,
            List<String> images,
            boolean isPair,
            boolean hasReturnButton,
            String returnButtonLabel,
            Member target
    ) {
        Member author = event.getMember();
        String imageUrl = images.get(RANDOM.nextInt(images.size()));
        int count;

        if (isPair) {
            count = DataBaseManager.incrementPairCount(author.getId(), target.getId(), actionKey);
        } else {
            count = DataBaseManager.incrementReceivedCount(target.getId(), actionKey);
        }

        String countText = buildCountText(isPair, actionKey, author, target, count);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " " + author.getEffectiveName() + " " + action + " " + target.getEffectiveName() + "!")
                .setDescription(countText)
                .setImage(imageUrl)
                .setColor(color)
                .setFooter("Powered by Arian 🐾");

        if (hasReturnButton) {
            String buttonId = actionKey + ":" + target.getId() + ":" + author.getId();
            event.reply("").addEmbeds(embed.build())
                    .addActionRow(Button.primary(buttonId, returnButtonLabel))
                    .queue();
        } else {
            event.replyEmbeds(embed.build()).queue();
        }
    }

}