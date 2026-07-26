package com.arian.bot.commands.music;

import com.arian.bot.music.GuildMusicManager;
import com.arian.bot.music.MusicManagers;
import com.arian.bot.music.QueuedTrack;
import com.arian.bot.music.YoutubeResolver;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pide una canción o playlist de YouTube y la agrega a la cola.
 *   Slash:  /play cancion:<nombre o link>
 *   Prefix: a!play <nombre o link>   (alias: a!p)
 */
public class PlayCommand {

    private static final int PLAYLIST_LIMIT = 100;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void handleSlash(SlashCommandInteractionEvent event) {
        String query = event.getOption("cancion").getAsString().trim();
        AudioChannel channel = memberChannel(event.getMember());
        if (channel == null) {
            event.reply("Tienes que estar en un canal de voz para pedirle música a Arian.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();
        String guildId = event.getGuild().getId();
        String requestedBy = event.getUser().getEffectiveName();
        executor.submit(() -> {
            String result = handle(guildId, channel, query, requestedBy);
            event.getHook().sendMessage(result).queue();
        });
    }

    public static void handlePrefix(MessageReceivedEvent event, String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            event.getChannel().sendMessage("úsalo así: `a!play <canción o link>`").queue();
            return;
        }
        AudioChannel channel = memberChannel(event.getMember());
        if (channel == null) {
            event.getChannel().sendMessage("Tienes que estar en un canal de voz para pedirle música a Arian.").queue();
            return;
        }

        String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String guildId = event.getGuild().getId();
        String requestedBy = event.getAuthor().getEffectiveName();
        event.getChannel().sendTyping().queue();
        executor.submit(() -> {
            String result = handle(guildId, channel, query, requestedBy);
            event.getChannel().sendMessage(result).queue();
        });
    }

    private static String handle(String guildId, AudioChannel channel, String query, String requestedBy) {
        GuildMusicManager music = MusicManagers.get(guildId);
        joinIfNeeded(channel, music);

        if (YoutubeResolver.isPlaylistUrl(query)) {
            List<QueuedTrack> tracks = YoutubeResolver.listPlaylist(query, requestedBy, PLAYLIST_LIMIT);
            if (tracks.isEmpty()) return "No pude leer esa playlist, revisa el link.";
            tracks.forEach(music.scheduler::queue);
            return "Agregué **" + tracks.size() + "** canciones de la playlist a la cola.";
        }

        QueuedTrack track = YoutubeResolver.resolve(query, requestedBy);
        if (track == null) return "No encontré esa canción (o YouTube la bloqueó), intenta con otro nombre o link.";
        music.scheduler.queue(track);
        return "Agregada a la cola: **" + track.title + "**"
                + (track.author.isBlank() || track.author.equals("Desconocido") ? "" : " — " + track.author);
    }

    private static void joinIfNeeded(AudioChannel channel, GuildMusicManager music) {
        Guild guild = channel.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(music.sendHandler);
        }
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(channel);
        }
    }

    private static AudioChannel memberChannel(Member member) {
        if (member == null) return null;
        var voiceState = member.getVoiceState();
        return voiceState != null ? voiceState.getChannel() : null;
    }
}
