package com.arian.bot.news;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.awt.Color;
import java.util.List;

/** Construye y envía el embed del digest de noticias a un canal (de texto o de anuncios). */
public class NewsPoster {

    private static final Color COLOR_PROGRAMACION = new Color(88, 166, 255);
    private static final Color COLOR_NEUROCIENCIA = new Color(163, 113, 247);

    /** Envía el digest al canal. Devuelve true si se mandó correctamente (bloquea hasta confirmarlo). */
    public static boolean post(GuildMessageChannel channel, List<NewsItem> items, NewsSummarizer.Digest digest) {
        List<MessageEmbed> embeds = items.stream()
                .map(item -> buildEmbed(item, digest.summaries().get(item.id())))
                .toList();
        try {
            channel.sendMessage(digest.intro())
                    .addEmbeds(embeds)
                    .complete();
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error al postear noticias en el canal " + channel.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private static MessageEmbed buildEmbed(NewsItem item, String summary) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(truncate(item.title(), 256), item.url());
        if (summary != null && !summary.isBlank()) {
            eb.setDescription(truncate(summary, 2048));
        }
        eb.setColor("Neurociencia".equals(item.category()) ? COLOR_NEUROCIENCIA : COLOR_PROGRAMACION);
        eb.addField("Autores", item.authors() == null || item.authors().isBlank() ? "—" : item.authors(), true);
        eb.addField("Fuente", item.published().isBlank() ? item.source() : item.source() + " · " + item.published(), true);
        eb.setFooter(item.category());
        return eb.build();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
