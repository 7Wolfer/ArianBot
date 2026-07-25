package com.arian.bot.news;

import com.arian.bot.DataBaseManager;

import java.util.ArrayList;
import java.util.List;

/** Arma el digest: 3 noticias de programación (2 Hacker News + 1 arXiv) y 2 de neurociencia (1 PubMed + 1 arXiv). */
public class NewsFetcher {

    public static List<NewsItem> buildDigest() {
        List<NewsItem> digest = new ArrayList<>();

        digest.addAll(pickUnposted(HackerNewsSource.fetch(20), 2));
        digest.addAll(pickUnposted(ArxivSource.fetch("cat:cs.SE+OR+cat:cs.PL+OR+cat:cs.DC", "Programación", 10), 1));
        digest.addAll(pickUnposted(PubMedSource.fetch(10), 1));
        digest.addAll(pickUnposted(ArxivSource.fetch("cat:q-bio.NC", "Neurociencia", 10), 1));

        return digest;
    }

    /** Marca noticias como posteadas. Llamar solo después de confirmar que se enviaron a algún canal. */
    public static void markPosted(List<NewsItem> items) {
        for (NewsItem item : items) {
            DataBaseManager.markNewsPosted(item.id());
        }
    }

    private static List<NewsItem> pickUnposted(List<NewsItem> candidates, int need) {
        List<NewsItem> picked = new ArrayList<>();
        for (NewsItem candidate : candidates) {
            if (picked.size() >= need) break;
            if (!DataBaseManager.isNewsPosted(candidate.id())) picked.add(candidate);
        }
        return picked;
    }
}
