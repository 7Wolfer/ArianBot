package com.arian.bot.listeners;

import com.arian.bot.Main;
import com.arian.bot.commands.PingCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class PrefixCommandListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignorar mensajes de bots (incluyendo el propio Arian)
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw().trim();

        // Verificar si el mensaje empieza con el prefijo
        if (!message.startsWith(Main.PREFIX)) return;

        // Extraer el comando (quitar el prefijo y separar por espacios)
        String[] args = message.substring(Main.PREFIX.length()).trim().split("\\s+");
        String commandName = args[0].toLowerCase();

        switch (commandName) {
            case "ping" -> PingCommand.handlePrefix(event);
        }
    }
}