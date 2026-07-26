package com.arian.bot.music;

import net.dv8tion.jda.api.audio.hooks.ConnectionListener;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;

/** Loggea los cambios de estado de la conexión de voz, para diagnosticar problemas de conexión (ej. UDP bloqueado). */
public class VoiceConnectionListener implements ConnectionListener {

    private final String guildId;

    public VoiceConnectionListener(String guildId) {
        this.guildId = guildId;
    }

    @Override
    public void onPing(long ping) {
        // no-op
    }

    @Override
    public void onStatusChange(ConnectionStatus status) {
        System.out.println("🔊 [voz " + guildId + "] " + status);
    }
}
