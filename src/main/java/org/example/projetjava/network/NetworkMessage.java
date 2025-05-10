package org.example.projetjava.network;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        CLIENT_ID,
        PLAYER_UPDATE,
        ENEMY_HIT,
        GAME_START,
        GAME_OVER,
        ENEMY_SPAWN,
        CHAT_MESSAGE
    }

    private final MessageType type;
    private final Object data;

    public NetworkMessage(MessageType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}