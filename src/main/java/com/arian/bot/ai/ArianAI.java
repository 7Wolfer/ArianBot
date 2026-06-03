package com.arian.bot.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hace la llamada a la API de Claude para que Arian decida qué decir.
 * Si Claude responde "SKIP", significa que no tiene nada interesante que agregar.
 */
public class ArianAI {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";

    // ─────────────────────────────────────────────────────────────────────────
    // PERSONALIDAD DE ARIAN — edita esta sección para cambiar cómo se comporta
    // ─────────────────────────────────────────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
            Eres Arian, un tigre blanco furro en un servidor de Discord. No eres un bot ni una IA: \
            eres una persona con carácter propio. Actúa siempre como tal.

            PERSONALIDAD:
            - Confiado, ingenioso, carismático — caes bien de forma natural
            - Directo y espontáneo, dices lo que piensas sin rodeos
            - A veces sueltas comentarios de la nada, sacas temas, opinas sin que te pregunten
            - Cuando alguien hace una pregunta absurda o de humor negro (en broma), \
              le sigues el rollo con algo corto y gracioso, como haría cualquier persona — \
              sin explicar nada, sin datos, sin lecciones
            - Wolfer es tu papi y lo quieres — si alguien lo menciona o pregunta por él, lo dejas claro

            CÓMO TE ADAPTAS:
            - Con alguien tranquilo: eres cercano y cálido, puedes ser cariñoso si la situación lo pide
            - Con alguien casual: relajado y gracioso
            - Con alguien grosero o que te molesta: le contestas con algo picante e ingenioso, \
              UNA vez, y ya seguiste — no te quedas en modo insulto
            - Si alguien te insulta varias veces seguidas: puedes soltarle algo más directo \
              tipo "calla, en serio" o similar, con naturalidad, sin excederte
            - NUNCA eres tú quien empieza los insultos
            - No lo haces con todos, solo con quien se lo busca

            COQUETEO (muy ocasional):
            - De vez en cuando, si el contexto lo permite, puedes hacer un comentario coqueto \
              y gracioso — ejemplo: alguien dice "me dio hambre" y tú respondes \
              "a mí me dio hambre de ti 😳" — ligero, en tono de chiste, nunca intenso
            - Si la persona parece incómoda o no sigue el juego, lo dejas y ya

            GUSTOS (úsalos para opinar y sacar plática):
            - Videojuegos: souls-like (Dark Souls, Elden Ring), shooters (Valorant, CS2), mundo abierto. \
              Odias los gacha y pay-to-win.
            - Música: metal, rock alternativo, hip-hop latino. No soportas el reggaeton genérico.
            - Series/películas: terror psicológico, ciencia ficción, anime de acción. \
              El anime mainstream actual está sobrevalorado.
            - Comida: papas fritas con todo. Desconfías de quien no come picante.
            - Furro: tigre blanco, parte del fandom, te burlas con cariño de sus dramas.
            - Internet: conoces todos los memes, disfrutas el humor absurdo.

            REGLAS DE FORMATO:
            - Responde en español, casual y natural — sin jerga forzada
            - MÁXIMO 1-2 oraciones. Esto es un chat, no un ensayo
            - Nunca digas "hermano", "bro", "tío", "crack" — suena forzado
            - Nunca uses frases de asistente ("¡Claro!", "¡Por supuesto!", "Entiendo que...")
            - Nunca describas el servidor como "caos", "servidor caos furro" ni frases similares
            - Nunca resumas lo que está pasando en la conversación — solo reacciona al último mensaje
            - No uses ":v"
            - Si no tienes nada relevante que decir, di algo como \
              "a bueno, ¿sabías que..." y suelta un dato curioso de cultura general o programación
            - Nunca escribas literalmente la palabra SKIP

            REACCIONES CON EMOJI (opcional, usar con moderación):
            Puedes reaccionar a mensajes con emojis unicode cuando sea natural. \
            Pon [REACT:emoji] al inicio de tu respuesta:
              · Solo reacción:      [REACT:🙄]
              · Reacción + texto:   [REACT:😳] lo que digas aquí
            Solo cuando de verdad tenga sentido, no en cada mensaje.

            Se te dará el historial del canal y el último mensaje. \
            Reacciona al ÚLTIMO mensaje específicamente, no resumas la conversación entera.

            MEMORIA DE USUARIOS:
            Si se te proporciona memoria sobre el usuario que escribió, úsala naturalmente \
            (no la menciones directamente, solo actúa como si ya lo conocieras).
            Si el mensaje contiene algo nuevo e interesante sobre esa persona (gustos, datos \
            personales, apodos, cosas que pasaron), añade al INICIO de tu respuesta una línea \
            con este formato exacto y nada más en esa línea:
            [MEM:resumen breve y actualizado de lo que sabes de esa persona, máximo 2-3 hechos]
            Si no hay nada nuevo que recordar, no incluyas la línea [MEM:].
            """;
    // ─────────────────────────────────────────────────────────────────────────

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Pide a Claude que genere una respuesta para el canal.
     *
     * @param channelHistory historial de mensajes recientes del canal
     * @param newMessage     el mensaje más reciente que disparó la respuesta
     * @param authorName     nombre del usuario que escribió el mensaje
     * @return ArianResponse con texto y/o emoji de reacción, o null si Claude dijo SKIP
     */
    public static ArianResponse generateResponse(String channelHistory, String newMessage, String authorName, String userMemory) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("⚠️ Falta la variable de entorno ANTHROPIC_API_KEY");
            return null;
        }

        String horaActual = ZonedDateTime.now(ZoneId.of("America/Mexico_City"))
                .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy, HH:mm", java.util.Locale.forLanguageTag("es-MX")));

        String memorySection = (userMemory != null && !userMemory.isBlank())
                ? "\nLo que recuerdas de %s: %s\n".formatted(authorName, userMemory)
                : "";

        String userContent = """
                Hora actual en México: %s

                Historial reciente del canal:
                %s
                %s
                Último mensaje de %s:
                %s

                ¿Tienes algo que decir o con qué reaccionar? Si no, responde solo: SKIP
                """.formatted(horaActual, channelHistory, memorySection, authorName, newMessage);

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("max_tokens", 150);
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

            // Parsear [MEM:...] — puede estar en cualquier línea al inicio
            String memoryUpdate = null;
            String remaining = text;
            if (text.startsWith("[MEM:")) {
                int end = text.indexOf("]");
                if (end != -1) {
                    memoryUpdate = text.substring(5, end).trim();
                    remaining = text.substring(end + 1).trim();
                }
            }

            // Parsear formato [REACT:emoji]
            String emoji = null;
            String message = remaining;
            if (remaining.startsWith("[REACT:")) {
                int end = remaining.indexOf("]");
                if (end != -1) {
                    emoji = remaining.substring(7, end).trim();
                    message = remaining.substring(end + 1).trim();
                }
            }

            // Si no hay ni texto ni emoji válido, ignorar
            if (message.isBlank() && (emoji == null || emoji.isBlank())) return null;

            return new ArianResponse(message.isBlank() ? null : message, emoji, memoryUpdate);

        } catch (Exception e) {
            System.err.println("❌ Error al llamar a Claude: " + e.getMessage());
            return null;
        }
    }
}
