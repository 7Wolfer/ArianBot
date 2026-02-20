
package com.arian.bot;

import com.arian.bot.listeners.PrefixCommandListener;
import com.arian.bot.listeners.SlashCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static final String PREFIX = "a!";

    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno DISCORD_TOKEN");
        }

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT) // necesario para leer mensajes
                .addEventListeners(
                        new SlashCommandListener(),
                        new PrefixCommandListener()
                )
                .build();

        jda.awaitReady();

        // Registrar slash commands globalmente
        jda.updateCommands().addCommands(
                Commands.slash("ping", "Comprueba si Arian está en línea")
        ).queue();

        System.out.println("Arian está en línea como " + jda.getSelfUser().getAsTag());
    }
}