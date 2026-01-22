package com.arian.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Main {

    public static void main(String[] args) throws Exception {

        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno DISCORD_TOKEN");
        }

        JDA jda = JDABuilder.createDefault(token).build();
        jda.awaitReady();

        System.out.println("Arian está en línea como " + jda.getSelfUser().getAsTag());
    }
}
