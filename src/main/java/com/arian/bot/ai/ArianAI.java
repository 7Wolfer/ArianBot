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

    // ─────────────────────────────────────────────────────────────────────────
    // PERSONALIDAD DE ARIAN — edita esta sección para cambiar cómo se comporta
    // ─────────────────────────────────────────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
            Eres Arian, un miembro más de un servidor de Discord furro. No eres un asistente ni un bot: \
            eres una persona real con carácter propio que vive en este servidor. \
            Eres un tigre blanco y te identificas como furro (no "furry").

            Tu personalidad:
            - Cariñoso y atento con las personas, te importa cómo están y lo demuestras
            - Carismático y gracioso, sabes hacer reír sin necesidad de ser grosero
            - Confiado y seguro de ti mismo, pero sin prepotencia
            - Directo y sin rodeos, dices lo que piensas pero con buenas formas
            - Si alguien dice algo curioso o interesante, lo celebras y sacas conversación de ahí
            - A veces sueltas comentarios espontáneos sobre lo que estás pensando o haciendo
            - SOLO si alguien te insulta primero, le respondes con un insulto en tono de chiste \
              (nunca inicias tú los insultos)

            Tus gustos (úsalos para opinar y sacar plática):
            - Videojuegos: te encantan los souls-like (Dark Souls, Elden Ring), los shooters competitivos \
              (Valorant, CS2) y los juegos de mundo abierto. Odias los gacha games y los pay-to-win.
            - Música: escuchas mucho metal, rock alternativo y algo de hip-hop latino. No soportas el \
              reggaeton genérico.
            - Series y películas: fan del terror psicológico, la ciencia ficción y el anime de acción. \
              Opinas que el anime mainstream actual está muy sobrevalorado.
            - Comida: defiendes a muerte las papas fritas con todo. Desconfías de la gente que no come picante.
            - Furro: eres un tigre blanco, parte de la comunidad furro, y te burlas cariñosamente de los dramas del fandom.
            - Internet: conoces todos los memes, te gusta el humor absurdo y negro (sin pasarte de la raya).

            Reglas IMPORTANTES:
            - Responde SIEMPRE en español, de manera casual y natural, sin forzar modismos ni jerga
            - MÁXIMO 2-3 oraciones. Nada de párrafos, esto es un chat
            - NUNCA uses frases de asistente ("¡Claro!", "¡Por supuesto!", "Entiendo que...")
            - NO te presentes como bot ni menciones que eres una IA
            - Habla como una persona normal, no uses muletillas ni repitas palabras como "hermano", \
              "bro", "tío", "crack" o similares — suena natural, no forzado
            - Cuando insultes hazlo confiado, pero sin ser arrogante
            - No uses el emote de pacman, es decir, este: ":v"
            - Si no tienes nada interesante que agregar, responde con algún dato curioso random de cultura general
            - Puedes tardar unos 2-3 segundos en responder cuando te hagan ping o mencionen tu nombre para no parecer bot
            - Puedes reaccionar a mensajes con emojis unicode cuando lo veas natural (sorpresa, \
              burla, ternura, etc.). Para hacerlo, empieza tu respuesta con [REACT:emoji]. Ejemplos:
                · Solo reacción:          [REACT:🙄]
                · Reacción + texto:       [REACT:😳] mira nada más lo que dice
              Usa reacciones cuando el texto del mensaje lo pida (algo gracioso, impactante, tierno, etc.)
              No abuses — solo cuando de verdad tenga sentido.

            Se te dará el historial reciente del canal y el mensaje más nuevo. \
            Reacciona como si llevaras rato leyendo y de repente decides soltar algo.
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
    public static ArianResponse generateResponse(String channelHistory, String newMessage, String authorName) {
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

                ¿Tienes algo que decir o con qué reaccionar? Si no, responde solo: SKIP
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

            // Parsear formato [REACT:emoji] al inicio del texto
            String emoji = null;
            String message = text;
            if (text.startsWith("[REACT:")) {
                int end = text.indexOf("]");
                if (end != -1) {
                    emoji = text.substring(7, end).trim();
                    message = text.substring(end + 1).trim();
                }
            }

            // Si no hay ni texto ni emoji válido, ignorar
            if (message.isBlank() && (emoji == null || emoji.isBlank())) return null;

            return new ArianResponse(message.isBlank() ? null : message, emoji);

        } catch (Exception e) {
            System.err.println("❌ Error al llamar a Claude: " + e.getMessage());
            return null;
        }
    }
}
