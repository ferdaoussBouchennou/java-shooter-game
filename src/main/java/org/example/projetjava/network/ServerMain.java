package org.example.projetjava.network;

public class ServerMain {
    public static void main(String[] args) {
        GameServer server = new GameServer(5555);
        server.start();
    }
}