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
    public static boolean post(GuildMessageChannel channel, List<NewsItem> items) {
        List<MessageEmbed> embeds = items.stream().map(NewsPoster::buildEmbed).toList();
        try {
            channel.sendMessage("📰 **Resumen de la semana — Programación y Neurociencia**")
                    .addEmbeds(embeds)
                    .complete();
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error al postear noticias en el canal " + channel.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private static MessageEmbed buildEmbed(NewsItem item) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(truncate(item.title(), 256), item.url());
        eb.setColor("Neurociencia".equals(item.category()) ? COLOR_NEUROCIENCIA : COLOR_PROGRAMACION);
        eb.addField("Autores", item.authors() == null || item.authors().isBlank() ? "—" : item.authors(), false);
        eb.addField("Fuente", item.published().isBlank() ? item.source() : item.source() + " · " + item.published(), true);
        eb.addField("Categoría", item.category(), true);
        return eb.build();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
