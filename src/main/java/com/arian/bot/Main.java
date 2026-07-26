
package com.arian.bot;

import com.arian.bot.listeners.ArianListener;
import com.arian.bot.listeners.ButtonListener;
import com.arian.bot.listeners.PrefixCommandListener;
import com.arian.bot.listeners.SlashCommandListener;
import com.arian.bot.music.MusicManagers;
import com.arian.bot.news.NewsScheduler;
import dev.arbjerg.lavalink.client.Helpers;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static final String PREFIX = "a!";

    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno DISCORD_TOKEN");
        }
        String lavalinkPassword = System.getenv("LAVALINK_PASSWORD");
        if (lavalinkPassword == null || lavalinkPassword.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno LAVALINK_PASSWORD");
        }

        DataBaseManager.initialize();

        // El cliente de Lavalink se crea antes que JDA porque JDABuilder necesita el
        // interceptor de voz (JDAVoiceUpdateListener) para reenviarle los eventos de voz.
        LavalinkClient lavalinkClient = MusicManagers.init(Helpers.getUserIdFromToken(token), lavalinkPassword);

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES)
                .setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(lavalinkClient))
                .addEventListeners(
                        new SlashCommandListener(),
                        new PrefixCommandListener(),
                        new ButtonListener(),
                        new ArianListener()
                )
                .build();

        jda.awaitReady();

        // Registrar slash commands globalmente
        jda.updateCommands().addCommands(
                Commands.slash("ping", "Comprueba si Arian está en línea"),
                Commands.slash("hug", "Dale un abrazo a alguien")
                        .addOption(OptionType.USER, "usuario", "¿A quién quieres abrazar?", true),
                Commands.slash("kiss", "Dale un beso a alguien")
                        .addOption(OptionType.USER, "usuario", "¿A quién quieres besar?", true),
                Commands.slash("hit", "Golpea a alguien")
                        .addOption(OptionType.USER, "usuario", "¿A quién quieres golpear?", true),
                Commands.slash("pat", "Dale un pat a alguien")
                        .addOption(OptionType.USER, "usuario", "¿A quién quieres hacerle un pat?", true),
                Commands.slash("channel", "Activa o desactiva un canal para que Arian hable en él")
                        .addOption(OptionType.CHANNEL, "canal", "Canal a activar/desactivar (vacío para listar)", false),
                Commands.slash("download", "Descarga un video de TikTok, Instagram, YouTube, etc.")
                        .addOption(OptionType.STRING, "url", "El link del video a descargar", true),
                Commands.slash("newschannel", "Configura el canal donde Arian postea noticias de programación y neurociencia")
                        .addOption(OptionType.CHANNEL, "canal", "Canal para las noticias (vacío para ver el actual)", false),
                Commands.slash("play", "Pon una canción o playlist de YouTube en tu canal de voz")
                        .addOption(OptionType.STRING, "cancion", "Nombre de la canción o link de YouTube", true),
                Commands.slash("skip", "Salta la canción actual"),
                Commands.slash("pause", "Pausa o reanuda la música"),
                Commands.slash("queue", "Muestra la cola de reproducción"),
                Commands.slash("remove", "Quita una canción de la cola")
                        .addOption(OptionType.INTEGER, "posicion", "Posición en la cola (mira /queue)", true),
                Commands.slash("priority", "Pone una canción de la cola como la siguiente en sonar")
                        .addOption(OptionType.INTEGER, "posicion", "Posición en la cola (mira /queue)", true),
                Commands.slash("stop", "Para la música y saca a Arian del canal de voz")
        ).queue();

        NewsScheduler.start(jda);

        System.out.println("Arian está en línea como " + jda.getSelfUser().getAsTag());
    }

}