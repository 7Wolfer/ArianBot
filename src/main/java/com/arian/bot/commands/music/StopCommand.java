package com.arian.bot.commands.music;

import com.arian.bot.music.MusicManagers;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** Para la música, vacía la cola y desconecta a Arian del canal de voz. Slash: /stop · Prefix: a!stop */
public class StopCommand {

    public static void handleSlash(SlashCommandInteractionEvent event) {
        stop(event.getGuild());
        event.reply("Listo, salgo de voz. 👋").queue();
    }

    public static void handlePrefix(MessageReceivedEvent event) {
        stop(event.getGuild());
        event.getChannel().sendMessage("Listo, salgo de voz. 👋").queue();
    }

    private static void stop(net.dv8tion.jda.api.entities.Guild guild) {
        var music = MusicManagers.getIfExists(guild.getId());
        if (music != null) music.scheduler.stopAndClear();
        guild.getAudioManager().closeAudioConnection();
        MusicManagers.remove(guild.getId());
    }
}
