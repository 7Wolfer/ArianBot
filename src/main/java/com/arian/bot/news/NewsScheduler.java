package com.arian.bot.news;

import com.arian.bot.DataBaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Programa el envío del digest de noticias los martes y viernes a las 10:00 (hora CDMX). */
public class NewsScheduler {

    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void start(JDA jda) {
        scheduleNext(jda);
    }

    private static void scheduleNext(JDA jda) {
        ZonedDateTime next = nextRunTime();
        long delayMs = Duration.between(ZonedDateTime.now(ZONE), next).toMillis();
        executor.schedule(() -> {
            try {
                runDigest(jda);
            } catch (Exception e) {
                System.err.println("❌ Error al postear el digest de noticias: " + e.getMessage());
            } finally {
                scheduleNext(jda);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        System.out.println("📅 Próximo digest de noticias programado para: " + next);
    }

    private static ZonedDateTime nextRunTime() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime candidate = now.withHour(10).withMinute(0).withSecond(0).withNano(0);
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
        while (candidate.getDayOfWeek() != DayOfWeek.TUESDAY && candidate.getDayOfWeek() != DayOfWeek.FRIDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    /** Arma y postea el digest ya mismo, en todos los canales de noticias configurados. */
    public static void runDigest(JDA jda) {
        List<NewsItem> items = NewsFetcher.buildDigest();
        if (items.isEmpty()) {
            System.out.println("⚠️ No hay noticias nuevas para postear.");
            return;
        }
        for (String channelId : DataBaseManager.getAllNewsChannels()) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) NewsPoster.post(channel, items);
        }
    }
}
