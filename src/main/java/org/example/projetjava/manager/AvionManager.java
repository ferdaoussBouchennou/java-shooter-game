package org.example.projetjava.manager;

import org.example.projetjava.model.Avion;

import java.util.HashMap;
import java.util.Map;

public class AvionManager {
    private static final Map<String, Avion> avions = new HashMap<>();

    static {
        // Utilisez des chemins absolus avec le préfixe standard
        avions.put("MiG-51S", new Avion("MiG-51S", 8, 3, 8,
                "/org/example/projetjava/kenney_space-shooter-redux/PNG/playerShip1_blue.png"));

        avions.put("FIA-28A", new Avion("FIA-28A", 6, 4, 10,
                "/org/example/projetjava/kenney_space-shooter-redux/PNG/playerShip2_orange.png"));

        avions.put("X-Wing", new Avion("X-Wing", 9, 2, 7,
                "/org/example/projetjava/kenney_space-shooter-redux/PNG/playerShip3_green.png"));

        avions.put("DarkStar", new Avion("DarkStar", 7, 5, 9,
                "/org/example/projetjava/kenney_space-shooter-redux/PNG/Enemies/enemyRed3.png"));
    }

    public static Avion getAvion(String nom) {
        return avions.get(nom);
    }
}
