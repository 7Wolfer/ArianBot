package com.arian.bot.news;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le pide a Claude que escriba, en español y con la voz de Arian, un resumen corto de cada noticia
 * y una intro para el post. Todo grounded en el título y el abstract real (si lo hay) para no inventar datos.
 */
public class NewsSummarizer {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";

    private static final String SYSTEM_PROMPT = """
            Eres Arian, un tigre blanco furro carismático y con mucha confianza, escribiendo el \
            resumen semanal de noticias de programación y neurociencia para un canal de Discord.

            Aquí NO estás en modo chat casual: es un post serio pero con energía, como alguien que \
            de verdad está emocionado de compartir algo importante — directo, con personalidad propia, \
            sin sonar a nota de prensa aburrida ni a robot.

            Para cada noticia te dan: id, categoría, título y (si hay) un fragmento del abstract real.

            Para cada una, escribe en español 1-2 frases explicando de qué trata y por qué importa \
            o es interesante. Reglas estrictas:
            - NUNCA inventes datos, cifras, hallazgos o afirmaciones que no estén en el título o el \
              fragmento dado.
            - Si no hay abstract, comenta sobre el TEMA del título sin inventar detalles específicos \
              del contenido (no te inventes resultados ni conclusiones).
            - Nada de groserías ni humor absurdo — aquí el tono es serio-emocionante, no de chat casual.
            - No traduzcas el título literalmente ni lo repitas tal cual, coméntalo con tus palabras.

            También escribe una intro de 1 frase para abrir el post completo, con energía, anunciando \
            que llegó el resumen de la semana (puede llevar un emoji).

            Responde SOLO con este JSON, sin texto extra, sin markdown, sin backticks:
            {"intro": "...", "items": {"<id>": "...", "<id>": "..."}}
            """;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record Digest(String intro, Map<String, String> summaries) {
    }

    public static Digest summarize(List<NewsItem> items) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("⚠️ Falta ANTHROPIC_API_KEY, se postea el digest sin resúmenes.");
            return fallback();
        }

        JSONArray itemsJson = new JSONArray();
        for (NewsItem item : items) {
            JSONObject o = new JSONObject();
            o.put("id", item.id());
            o.put("categoria", item.category());
            o.put("titulo", item.title());
            o.put("abstract", item.sourceText() == null ? JSONObject.NULL : item.sourceText());
            itemsJson.put(o);
        }

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("max_tokens", 800);
        body.put("system", SYSTEM_PROMPT);
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", "Noticias:\n" + itemsJson);
        messages.put(userMsg);
        body.put("messages", messages);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("❌ Error API Claude (resumen de noticias): " + response.statusCode() + " — " + response.body());
                return fallback();
            }

            String text = new JSONObject(response.body())
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

            // Por si Claude envuelve el JSON en ```json ... ```
            text = text.replaceAll("(?s)^```(json)?\\s*|```\\s*$", "").trim();

            JSONObject parsed = new JSONObject(text);
            String intro = parsed.optString("intro", fallback().intro());
            JSONObject itemsObj = parsed.optJSONObject("items");
            Map<String, String> summaries = new HashMap<>();
            if (itemsObj != null) {
                for (String key : itemsObj.keySet()) {
                    summaries.put(key, itemsObj.optString(key, ""));
                }
            }
            return new Digest(intro, summaries);
        } catch (Exception e) {
            System.err.println("❌ Error al generar resúmenes con Claude: " + e.getMessage());
            return fallback();
        }
    }

    private static Digest fallback() {
        return new Digest("📰 **Resumen de la semana — Programación y Neurociencia**", new HashMap<>());
    }
}
