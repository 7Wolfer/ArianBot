package com.arian.bot.news;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Trae papers recientes de arXiv para una categoría, ordenados por fecha de envío. */
public class ArxivSource {

    private static final String API_URL = "https://export.arxiv.org/api/query";
    // arXiv pide no hacer ráfagas de más de 1 solicitud cada 3 segundos; si no se respeta,
    // el servidor a veces ni siquiera responde con 429, simplemente deja la conexión colgada.
    private static final Duration MIN_INTERVAL = Duration.ofSeconds(4);
    private static volatile Instant lastRequest = Instant.EPOCH;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** @param searchQuery p. ej. "cat:cs.SE+OR+cat:cs.PL" o "cat:q-bio.NC" */
    public static List<NewsItem> fetch(String searchQuery, String category, int limit) {
        List<NewsItem> items = new ArrayList<>();
        try {
            respectRateLimit();
            String url = API_URL + "?search_query=" + searchQuery
                    + "&sortBy=submittedDate&sortOrder=descending&max_results=" + limit;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("❌ Error API arXiv: " + response.statusCode());
                return items;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8)));

            NodeList entries = doc.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                String id = text(entry, "id");
                String title = text(entry, "title");
                if (id == null || title == null) continue;
                title = title.replaceAll("\\s+", " ").trim();

                String arxivId = id.substring(id.lastIndexOf('/') + 1);
                String absUrl = "https://arxiv.org/abs/" + arxivId;

                NodeList authorNodes = entry.getElementsByTagName("author");
                List<String> authorNames = new ArrayList<>();
                for (int j = 0; j < authorNodes.getLength(); j++) {
                    String name = text((Element) authorNodes.item(j), "name");
                    if (name != null) authorNames.add(name);
                }

                String publishedRaw = text(entry, "published");
                String published = publishedRaw != null
                        ? ZonedDateTime.parse(publishedRaw).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-MX")))
                        : "";

                String summary = text(entry, "summary");
                String sourceText = summary == null ? null : truncate(summary.replaceAll("\\s+", " ").trim(), 600);

                items.add(new NewsItem(
                        "arxiv:" + arxivId,
                        title,
                        formatAuthors(authorNames),
                        "arXiv",
                        absUrl,
                        category,
                        published,
                        sourceText
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Error al consultar arXiv: " + e.getMessage());
        }
        return items;
    }

    private static synchronized void respectRateLimit() throws InterruptedException {
        Duration elapsed = Duration.between(lastRequest, Instant.now());
        if (elapsed.compareTo(MIN_INTERVAL) < 0) {
            Thread.sleep(MIN_INTERVAL.minus(elapsed).toMillis());
        }
        lastRequest = Instant.now();
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    private static String formatAuthors(List<String> names) {
        if (names.isEmpty()) return "Autor desconocido";
        if (names.size() <= 3) return String.join(", ", names);
        return String.join(", ", names.subList(0, 3)) + " et al.";
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
