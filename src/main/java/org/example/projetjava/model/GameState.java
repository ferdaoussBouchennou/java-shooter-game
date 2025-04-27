package org.example.projetjava.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, PlayerState> players = new HashMap<>();

    public void updatePlayer(int clientId, PlayerState state) {
        players.put(clientId, state);
    }

    public PlayerState getPlayer(int clientId) {
        return players.get(clientId);
    }

    public Map<Integer, PlayerState> getPlayers() {
        return new HashMap<>(players);
    }
}