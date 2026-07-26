package com.arian.bot.listeners;

import com.arian.bot.commands.ChannelCommand;
import com.arian.bot.commands.DownloadCommand;
import com.arian.bot.commands.NewsChannelCommand;
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
            case "channel" -> ChannelCommand.handleSlash(event);
            case "download" -> DownloadCommand.handleSlash(event);
            case "newschannel" -> NewsChannelCommand.handleSlash(event);
            case "play" -> PlayCommand.handleSlash(event);
            case "skip" -> SkipCommand.handleSlash(event);
            case "pause" -> PauseCommand.handleSlash(event);
            case "queue" -> QueueCommand.handleSlash(event);
            case "remove" -> RemoveCommand.handleSlash(event);
            case "priority" -> PriorityCommand.handleSlash(event);
            case "stop" -> StopCommand.handleSlash(event);
        }
    }
}