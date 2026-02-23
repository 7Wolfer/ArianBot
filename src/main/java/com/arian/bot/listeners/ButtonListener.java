package com.arian.bot.listeners;

import com.arian.bot.commands.social.HugCommand;
import com.arian.bot.commands.social.KissCommand;
import com.arian.bot.commands.social.HitCommand;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        if (parts.length != 3) return;

        String actionKey = parts[0];
        String expectedUserId = parts[1]; // quien debe presionar el botón
        String originalAuthorId = parts[2];

        // Solo la persona a quien va dirigida la acción puede responder
        if (!event.getUser().getId().equals(expectedUserId)) {
            event.reply("⚠️ ¡Este botón no es para ti!").setEphemeral(true).queue();
            return;
        }

        // Deshabilitar el botón después de usarlo
        event.getMessage().editMessageComponents().queue();

        switch (actionKey) {
            case "hug" -> HugCommand.handleButton(event, originalAuthorId);
            case "kiss" -> KissCommand.handleButton(event, originalAuthorId);
            case "hit" -> HitCommand.handleButton(event, originalAuthorId);
        }
    }
}