package com.arian.bot.news;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Trae artículos recientes de neurociencia publicados en PubMed (E-utilities de NCBI). */
public class PubMedSource {

    private static final String ESEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    private static final String ESUMMARY_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi";
    private static final String EFETCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static List<NewsItem> fetch(int limit) {
        List<NewsItem> items = new ArrayList<>();
        try {
            String term = URLEncoder.encode(
                    "neuroscience[Title/Abstract] AND (brain OR cognition OR neural)", StandardCharsets.UTF_8);
            String searchUrl = ESEARCH_URL + "?db=pubmed&retmode=json&sort=date&datetype=pdat"
                    + "&reldate=7&retmax=" + limit + "&term=" + term + "&tool=ArianBot";

            JSONObject searchJson = getJson(searchUrl);
            if (searchJson == null) return items;
            JSONArray idArray = searchJson.getJSONObject("esearchresult").getJSONArray("idlist");
            if (idArray.length() == 0) return items;

            StringBuilder ids = new StringBuilder();
            for (int i = 0; i < idArray.length(); i++) {
                if (i > 0) ids.append(",");
                ids.append(idArray.getString(i));
            }

            String summaryUrl = ESUMMARY_URL + "?db=pubmed&retmode=json&id=" + ids + "&tool=ArianBot";
            JSONObject summaryJson = getJson(summaryUrl);
            if (summaryJson == null) return items;
            JSONObject result = summaryJson.getJSONObject("result");

            List<String> pmids = new ArrayList<>();
            for (int i = 0; i < idArray.length(); i++) pmids.add(idArray.getString(i));
            Map<String, String> abstracts = fetchAbstracts(pmids);

            for (int i = 0; i < idArray.length(); i++) {
                String pmid = idArray.getString(i);
                if (!result.has(pmid)) continue;
                JSONObject doc = result.getJSONObject(pmid);

                String title = doc.optString("title", "").replaceAll("<[^>]*>", "").trim();
                if (title.isBlank()) continue;

                List<String> authorNames = new ArrayList<>();
                JSONArray authorsArr = doc.optJSONArray("authors");
                if (authorsArr != null) {
                    for (int j = 0; j < authorsArr.length(); j++) {
                        authorNames.add(authorsArr.getJSONObject(j).optString("name"));
                    }
                }
                String authors = authorNames.isEmpty() ? "Autor desconocido"
                        : authorNames.size() <= 3 ? String.join(", ", authorNames)
                        : String.join(", ", authorNames.subList(0, 3)) + " et al.";

                String journal = doc.optString("source", "PubMed");
                String pubDate = doc.optString("pubdate", "");

                items.add(new NewsItem(
                        "pubmed:" + pmid,
                        title,
                        authors,
                        journal,
                        "https://pubmed.ncbi.nlm.nih.gov/" + pmid + "/",
                        "Neurociencia",
                        pubDate,
                        abstracts.get(pmid)
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Error al consultar PubMed: " + e.getMessage());
        }
        return items;
    }

    /** Trae los abstracts reales vía efetch (esummary no los incluye). Devuelve pmid -> abstract. */
    private static Map<String, String> fetchAbstracts(List<String> pmids) {
        Map<String, String> abstracts = new HashMap<>();
        if (pmids.isEmpty()) return abstracts;
        try {
            String ids = String.join(",", pmids);
            String url = EFETCH_URL + "?db=pubmed&retmode=xml&rettype=abstract&id=" + ids + "&tool=ArianBot";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("❌ Error API PubMed (efetch): " + response.statusCode());
                return abstracts;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // El XML de efetch SÍ trae un DOCTYPE legítimo (su propio DTD), así que a diferencia de
            // arXiv no lo podemos bloquear por completo; en vez de eso evitamos que resuelva entidades
            // externas (mitigación estándar contra XXE que sí tolera el DOCTYPE).
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8)));

            NodeList articles = doc.getElementsByTagName("PubmedArticle");
            for (int i = 0; i < articles.getLength(); i++) {
                Element article = (Element) articles.item(i);
                NodeList pmidNodes = article.getElementsByTagName("PMID");
                if (pmidNodes.getLength() == 0) continue;
                String pmid = pmidNodes.item(0).getTextContent();

                NodeList abstractTexts = article.getElementsByTagName("AbstractText");
                if (abstractTexts.getLength() == 0) continue;
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < abstractTexts.getLength(); j++) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(abstractTexts.item(j).getTextContent());
                }
                String text = sb.toString().trim();
                abstracts.put(pmid, text.length() <= 600 ? text : text.substring(0, 600) + "…");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al consultar abstracts de PubMed: " + e.getMessage());
        }
        return abstracts;
    }

    private static JSONObject getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("❌ Error API PubMed: " + response.statusCode());
            return null;
        }
        return new JSONObject(response.body());
    }
}
