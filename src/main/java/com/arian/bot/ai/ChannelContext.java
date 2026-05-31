package com.arian.bot.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda los últimos mensajes de cada canal para dárselos a la IA como contexto.
 * Así Arian "sabe" de qué se está hablando antes de responder.
 */
public class ChannelContext {

    private static final int MAX_MESSAGES = 20;

    // channelId → lista de mensajes recientes
    private static final Map<String, Deque<String>> history = new ConcurrentHashMap<>();

    // channelId → timestamp del último mensaje que Arian envió (para cooldown)
    private static final Map<String, Long> lastReplyTime = new ConcurrentHashMap<>();

    /** Agrega un mensaje al historial del canal. */
    public static void addMessage(String channelId, String authorName, String content) {
        Deque<String> deque = history.computeIfAbsent(channelId, k -> new ArrayDeque<>());
        synchronized (deque) {
            if (deque.size() >= MAX_MESSAGES) deque.pollFirst();
            deque.addLast(authorName + ": " + content);
        }
    }

    /** Devuelve el historial formateado como texto para el prompt. */
    public static String getFormattedHistory(String channelId) {
        Deque<String> deque = history.get(channelId);
        if (deque == null || deque.isEmpty()) return "(sin mensajes previos)";
        synchronized (deque) {
            return String.join("\n", deque);
        }
    }

    /** Registra que Arian respondió en este canal ahora. */
    public static void markReplied(String channelId) {
        lastReplyTime.put(channelId, System.currentTimeMillis());
    }

    /** Devuelve true si pasaron al menos `cooldownMs` milisegundos desde la última respuesta de Arian. */
    public static boolean isCooldownOver(String channelId, long cooldownMs) {
        Long last = lastReplyTime.get(channelId);
        if (last == null) return true;
        return System.currentTimeMillis() - last >= cooldownMs;
    }
}
