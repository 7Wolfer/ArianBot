package com.arian.bot.ai;

/**
 * Resultado que devuelve ArianAI: puede tener texto, emoji de reacción, o ambos.
 */
public class ArianResponse {
    public final String text;          // null si no hay texto que enviar
    public final String emoji;         // null si no hay reacción
    public final String memoryUpdate;  // null si no hay nada nuevo que recordar

    public ArianResponse(String text, String emoji, String memoryUpdate) {
        this.text = text;
        this.emoji = emoji;
        this.memoryUpdate = memoryUpdate;
    }

    public boolean hasText()         { return text         != null && !text.isBlank(); }
    public boolean hasEmoji()        { return emoji        != null && !emoji.isBlank(); }
    public boolean hasMemoryUpdate() { return memoryUpdate != null && !memoryUpdate.isBlank(); }
}
