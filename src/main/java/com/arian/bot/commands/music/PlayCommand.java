package com.arian.bot.commands.music;

import com.arian.bot.music.MusicManagers;
import com.arian.bot.music.QueuedTrack;
import com.arian.bot.music.SpotifyResolver;
import com.arian.bot.music.TrackScheduler;
import com.arian.bot.music.YoutubeResolver;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
        JDA jda = event.getJDA();
        executor.submit(() -> {
            String result = handle(guildId, channel, jda, query, requestedBy);
            event.getHook().sendMessage(result).queue();
        });
    }

    /** Sugerencias de autocompletar para la opción "cancion" del slash command, buscando en Spotify. */
    public static void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        String typed = event.getFocusedOption().getValue().trim();
        if (typed.isBlank()) {
            event.replyChoiceStrings(List.of()).queue();
            return;
        }
        executor.submit(() -> {
            List<String> suggestions = SpotifyResolver.search(typed, 10);
            event.replyChoiceStrings(suggestions).queue(v -> {}, err -> {});
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
        JDA jda = event.getJDA();
        event.getChannel().sendTyping().queue();
        executor.submit(() -> {
            String result = handle(guildId, channel, jda, query, requestedBy);
            event.getChannel().sendMessage(result).queue();
        });
    }

    private static String handle(String guildId, AudioChannel channel, JDA jda, String query, String requestedBy) {
        AudioChannel currentChannel = currentVoiceChannel(jda, guildId);
        if (currentChannel != null && currentChannel.getIdLong() != channel.getIdLong() && hasListeners(currentChannel)) {
            return "Arian ya está sonando en " + currentChannel.getAsMention() + ", únete ahí para escucharlo.";
        }

        TrackScheduler scheduler = MusicManagers.get(guildId);
        jda.getDirectAudioController().connect(channel);

        if (SpotifyResolver.isSpotifyUrl(query)) {
            if (SpotifyResolver.isTrackUrl(query)) {
                QueuedTrack track = SpotifyResolver.resolveTrack(query, requestedBy);
                if (track == null) return "No pude leer esa canción de Spotify, revisa el link (o Spotify no está configurado).";
                scheduler.queue(track);
                return "Agregada a la cola (Spotify → YouTube): **" + track.title + "** — " + track.author;
            }
            List<QueuedTrack> tracks = SpotifyResolver.listPlaylistOrAlbum(query, requestedBy, PLAYLIST_LIMIT);
            if (tracks.isEmpty()) return "No pude leer esa playlist/álbum de Spotify (revisa que sea público, o que Spotify esté configurado).";
            tracks.forEach(scheduler::queue);
            return "Agregué **" + tracks.size() + "** canciones de Spotify a la cola (se buscan en YouTube).";
        }

        if (YoutubeResolver.isPlaylistUrl(query)) {
            List<QueuedTrack> tracks = YoutubeResolver.listPlaylist(query, requestedBy, PLAYLIST_LIMIT);
            if (tracks.isEmpty()) return "No pude leer esa playlist, revisa el link.";
            tracks.forEach(scheduler::queue);
            return "Agregué **" + tracks.size() + "** canciones de la playlist a la cola.";
        }

        QueuedTrack track = YoutubeResolver.resolve(scheduler.link(), query, requestedBy);
        if (track == null) return "No encontré esa canción (o YouTube la bloqueó), intenta con otro nombre o link.";
        scheduler.queue(track);
        return "Agregada a la cola: **" + track.title + "**"
                + (track.author.isBlank() || track.author.equals("Desconocido") ? "" : " — " + track.author);
    }

    private static AudioChannel memberChannel(Member member) {
        if (member == null) return null;
        var voiceState = member.getVoiceState();
        return voiceState != null ? voiceState.getChannel() : null;
    }

    /** Canal de voz donde Arian ya está conectado en este servidor, o null si no está en ninguno. */
    private static AudioChannel currentVoiceChannel(JDA jda, String guildId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return null;
        var voiceState = guild.getSelfMember().getVoiceState();
        return voiceState != null ? voiceState.getChannel() : null;
    }

    private static boolean hasListeners(AudioChannel channel) {
        return channel.getMembers().stream().anyMatch(m -> !m.getUser().isBot());
    }
}
