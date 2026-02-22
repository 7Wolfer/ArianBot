package com.arian.bot.commands.social;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class SocialCommand {

    private static final Random RANDOM = new Random();

    // Método que construye y envía el embed, es el corazón de todos los comandos sociales
    private static void sendEmbed(
            String authorName,
            String authorAvatar,
            String targetName,
            String action,
            String emoji,
            Color color,
            List<String> images,
            Object channel
    ) {
        String imageUrl = images.get(RANDOM.nextInt(images.size()));

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " " + authorName + " " + action + " " + targetName + "!")
                .setImage(imageUrl)
                .setColor(color)
                .setFooter("Powered by Arian 🐾");

        if (channel instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel messageChannel) {
            messageChannel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    // Maneja el comando cuando viene de prefix (a!hug @usuario)
    public static void handlePrefix(
            MessageReceivedEvent event,
            String action,
            String emoji,
            Color color,
            List<String> images
    ) {
        // Verificar que mencionaron a alguien
        if (event.getMessage().getMentions().getMembers().isEmpty()) {
            event.getChannel().sendMessage("⚠️ Debes mencionar a alguien. Ejemplo: `a!" + action + " @usuario`").queue();
            return;
        }

        Member author = event.getMember();
        Member target = event.getMessage().getMentions().getMembers().get(0);

        // Evitar que se hagan la acción a sí mismos
        if (author.getId().equals(target.getId())) {
            event.getChannel().sendMessage("⚠️ ¡No puedes hacerte eso a ti mismo!").queue();
            return;
        }

        sendEmbed(
                author.getEffectiveName(),
                author.getEffectiveAvatarUrl(),
                target.getEffectiveName(),
                action,
                emoji,
                color,
                images,
                event.getChannel()
        );
    }

    // Maneja el comando cuando viene de slash (/hug @usuario)
    public static void handleSlash(
            SlashCommandInteractionEvent event,
            String action,
            String emoji,
            Color color,
            List<String> images
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

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " " + author.getEffectiveName() + " " + action + " " + target.getEffectiveName() + "!")
                .setImage(images.get(new Random().nextInt(images.size())))
                .setColor(color)
                .setFooter("Powered by Arian 🐾");

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}