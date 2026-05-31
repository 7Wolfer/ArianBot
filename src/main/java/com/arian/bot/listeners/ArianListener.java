package com.arian.bot.listeners;

import com.arian.bot.Main;
import com.arian.bot.ai.ArianAI;
import com.arian.bot.ai.ChannelContext;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Escucha todos los mensajes del servidor y decide cuándo Arian quiere hablar.
 *
 * Lógica de activación:
 *  - Siempre: si alguien responde directamente a un mensaje de Arian
 *  - 80%: si alguien menciona a Arian (@Arian)
 *  - 15%: en cualquier otro mensaje (azar), respetando un cooldown por canal
 *
 * Después de decidir intentarlo, se le pregunta a Claude si tiene algo que decir.
 * Si Claude responde "SKIP", Arian no habla.
 */
public class ArianListener extends ListenerAdapter {

    // Probabilidad base de intentar responder a un mensaje cualquiera (0.0 - 1.0)
    private static final double BASE_CHANCE = 0.35;

    // Cooldown mínimo entre respuestas de Arian en el mismo canal (en ms)
    private static final long COOLDOWN_MS = 25_000;

    private static final Random random = new Random();

    // Hilo separado para las llamadas a la API (evita bloquear el hilo de eventos de JDA)
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignorar bots
        if (event.getAuthor().isBot()) return;

        // Ignorar mensajes con el prefijo de comandos de Arian
        String content = event.getMessage().getContentRaw().trim();
        if (content.startsWith(Main.PREFIX)) return;

        String channelId = event.getChannel().getId();
        String authorName = event.getAuthor().getEffectiveName();

        // Guardar el mensaje en el historial del canal
        ChannelContext.addMessage(channelId, authorName, content);

        // Determinar si Arian debe intentar responder
        boolean mentionado = event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser());
        boolean nombreMencionado = content.toLowerCase().contains("arian");
        boolean esRespuestaArian = event.getMessage().getMessageReference() != null
                && isReplyToArian(event);

        boolean intentarResponder;
        if (esRespuestaArian) {
            intentarResponder = true;
        } else if (mentionado || nombreMencionado) {
            intentarResponder = random.nextDouble() < 0.80;
        } else {
            // Solo intentar si el cooldown pasó
            intentarResponder = ChannelContext.isCooldownOver(channelId, COOLDOWN_MS)
                    && random.nextDouble() < BASE_CHANCE;
        }

        if (!intentarResponder) return;

        // Hacer la llamada a la API en un hilo separado
        String history = ChannelContext.getFormattedHistory(channelId);
        executor.submit(() -> {
            String response = ArianAI.generateResponse(history, content, authorName);
            if (response == null) return;

            // Registrar cooldown y enviar
            ChannelContext.markReplied(channelId);
            event.getChannel().sendMessage(response).queue();
        });
    }

    /** Verifica si el mensaje es una respuesta a un mensaje enviado por Arian. */
    private boolean isReplyToArian(MessageReceivedEvent event) {
        try {
            var ref = event.getMessage().getMessageReference();
            if (ref == null) return false;
            var referencedMsg = ref.getMessage();
            if (referencedMsg == null) return false;
            return referencedMsg.getAuthor().equals(event.getJDA().getSelfUser());
        } catch (Exception e) {
            return false;
        }
    }
}
