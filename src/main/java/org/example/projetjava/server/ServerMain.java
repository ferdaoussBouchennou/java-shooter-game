package org.example.projetjava.server;

import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            server.start(5555);
        } catch (IOException e) {
            System.err.println("Erreur du serveur: " + e.getMessage());
        }
    }
}