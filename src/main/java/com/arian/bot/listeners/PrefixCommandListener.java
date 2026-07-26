package com.arian.bot.listeners;

import com.arian.bot.Main;
import com.arian.bot.commands.ChannelCommand;
import com.arian.bot.commands.DownloadCommand;
import com.arian.bot.commands.NewsChannelCommand;
import com.arian.bot.commands.OwnerCommand;
import com.arian.bot.commands.PingCommand;
import com.arian.bot.commands.music.PauseCommand;
import com.arian.bot.commands.music.PlayCommand;
import com.arian.bot.commands.music.PriorityCommand;
import com.arian.bot.commands.music.QueueCommand;
import com.arian.bot.commands.music.RemoveCommand;
import com.arian.bot.commands.music.SkipCommand;
import com.arian.bot.commands.music.StopCommand;
import com.arian.bot.commands.social.HitCommand;
import com.arian.bot.commands.social.HugCommand;
import com.arian.bot.commands.social.KissCommand;
import com.arian.bot.commands.social.PatCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class PrefixCommandListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignorar mensajes de bots (incluyendo a Arian)
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw().trim();

        // Verificar si el mensaje empieza con el prefijo
        if (!message.startsWith(Main.PREFIX)) return;

        // Extraer el comando (quitar el prefijo y separar por espacios)
        String[] args = message.substring(Main.PREFIX.length()).trim().split("\\s+");
        String commandName = args[0].toLowerCase();

        switch (commandName) {
            case "ping" -> PingCommand.handlePrefix(event);
            case "hug", "abrazo" -> HugCommand.handlePrefix(event);
            case "kiss", "beso" -> KissCommand.handlePrefix(event);
            case "hit", "golpe" -> HitCommand.handlePrefix(event);
            case "pat" -> PatCommand.handlePrefix(event);
            case "channel" -> ChannelCommand.handlePrefix(event, args);
            case "download", "dl", "descargar" -> DownloadCommand.handlePrefix(event, args);
            case "newschannel" -> NewsChannelCommand.handlePrefix(event, args);
            case "play", "p" -> PlayCommand.handlePrefix(event, args);
            case "skip", "s", "next" -> SkipCommand.handlePrefix(event);
            case "pause" -> PauseCommand.handlePrefix(event);
            case "queue", "q", "cola" -> QueueCommand.handlePrefix(event);
            case "remove", "rm" -> RemoveCommand.handlePrefix(event, args);
            case "priority", "prio" -> PriorityCommand.handlePrefix(event, args);
            case "stop" -> StopCommand.handlePrefix(event);
            case "guilds"  -> OwnerCommand.handleGuilds(event);
            case "leave"   -> OwnerCommand.handleLeave(event, args);
            case "testnews" -> OwnerCommand.handleTestNews(event);
        }
    }
}