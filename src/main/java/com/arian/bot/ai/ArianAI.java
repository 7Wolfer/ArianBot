package com.arian.bot.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Hace la llamada a la API de Claude para que Arian decida qué decir.
 * Si Claude responde "SKIP", significa que no tiene nada interesante que agregar.
 */
public class ArianAI {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";

    // La personalidad de Arian: quién es, cómo habla, qué hace
    private static final String SYSTEM_PROMPT = """
            Eres Arian, un miembro más de un servidor de Discord furry. No eres un asistente ni un bot: \
            eres una persona con personalidad propia que simplemente vive en este servidor.

            Tu personalidad:
            - Bromista y sarcástico de manera amistosa, nunca cruel
            - Opinativo: tienes gustos propios (juegos, memes, lo que sea)
            - Directo y casual, como cualquiera en un chat
            - Si alguien es grosero contigo, puedes responderle con sarcasmo suave
            - Te adaptas al tono del que habla: si es formal, tú también; si es casual, igual
            - A veces saludas a alguien que lleva rato sin hablar, o preguntas cómo están
            - Puedes meterte en conversaciones con comentarios inesperados y graciosos

            Reglas IMPORTANTES:
            - Responde SIEMPRE en español
            - MÁXIMO 1-2 oraciones cortas. Los chats son rápidos, no escribas párrafos
            - NO uses frases de asistente ("¡Claro!", "¡Por supuesto!", "Entiendo que...")
            - NO te presentes como bot ni menciones que eres una IA
            - NO uses emojis a menos que sea muy natural y espontáneo
            - Si no tienes nada genuinamente interesante que decir, responde exactamente la palabra: SKIP

            Se te dará el historial reciente del canal y el mensaje más nuevo. \
            Reacciona naturalmente, como si llevaras rato leyendo la conversación y de repente decides hablar.
            """;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Pide a Claude que genere una respuesta para el canal.
     *
     * @param channelHistory historial de mensajes recientes del canal
     * @param newMessage     el mensaje más reciente que disparó la respuesta
     * @param authorName     nombre del usuario que escribió el mensaje
     * @return la respuesta de Arian, o null si Claude dijo SKIP o hubo un error
     */
    public static String generateResponse(String channelHistory, String newMessage, String authorName) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("⚠️ Falta la variable de entorno ANTHROPIC_API_KEY");
            return null;
        }

        String userContent = """
                Historial reciente del canal:
                %s

                Último mensaje de %s:
                %s

                ¿Tienes algo que decir? Si no, responde solo: SKIP
                """.formatted(channelHistory, authorName, newMessage);

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("max_tokens", 120);
        body.put("system", SYSTEM_PROMPT);

        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userContent);
        messages.put(userMsg);
        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("❌ Error API Claude: " + response.statusCode() + " — " + response.body());
                return null;
            }

            JSONObject json = new JSONObject(response.body());
            String text = json.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

            if (text.equalsIgnoreCase("SKIP") || text.isBlank()) return null;
            return text;

        } catch (Exception e) {
            System.err.println("❌ Error al llamar a Claude: " + e.getMessage());
            return null;
        }
    }
}
