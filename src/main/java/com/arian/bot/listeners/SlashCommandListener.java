package com.arian.bot.listeners;

import com.arian.bot.commands.PingCommand;
import com.arian.bot.commands.social.HitCommand;
import com.arian.bot.commands.social.HugCommand;
import com.arian.bot.commands.social.KissCommand;
import com.arian.bot.commands.social.PatCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping" -> PingCommand.handleSlash(event);
            case "hug" -> HugCommand.handleSlash(event);
            case "kiss" -> KissCommand.handleSlash(event);
            case "hit" -> HitCommand.handleSlash(event);
            case "pat" -> PatCommand.handleSlash(event);
        }
    }
}