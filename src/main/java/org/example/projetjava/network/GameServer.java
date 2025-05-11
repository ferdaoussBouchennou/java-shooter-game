package org.example.projetjava.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.projetjava.client.GameClient;
import org.example.projetjava.model.ConnexionBD;
import org.example.projetjava.model.GameState;
import org.example.projetjava.model.PlayerState;

public class GameServer {
    private ServerSocket serverSocket;
    private final int port;
    private boolean running = false;
    private ExecutorService executor;
    private final Map<Integer, ClientHandler> clients = new HashMap<>();
    private GameState gameState = new GameState();
    private int nextClientId = 1;


    public GameServer(int port) {
        this.port = port;
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Serveur démarré sur le port " + port);
            System.out.println("En attente de connexions...");

            // Attendre deux clients au maximum
            while (running && clients.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                int clientId = nextClientId++;
                ClientHandler handler = new ClientHandler(clientId, clientSocket, this);
                clients.put(clientId, handler);
                executor.execute(handler);
                System.out.println("Client #" + clientId + " connecté. Nombre de joueurs: " + clients.size());

                // Si nous avons deux joueurs, commencer la partie
                if (clients.size() == 2) {
                    startGame();
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur: " + e.getMessage());
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (executor != null) {
                executor.shutdown();
            }
            for (ClientHandler handler : clients.values()) {
                handler.close();
            }
            clients.clear();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'arrêt du serveur: " + e.getMessage());
        }
    }

    private void startGame() {
        System.out.println("Début de la partie avec 2 joueurs!");
        broadcast(new NetworkMessage(NetworkMessage.MessageType.GAME_START, null));
    }

    public void broadcast(NetworkMessage message) {
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(message);
        }
    }

    public void broadcastExcept(int excludeClientId, NetworkMessage message) {
        for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) {
            if (entry.getKey() != excludeClientId) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    public void updatePlayerState(int clientId, PlayerState state) {
        gameState.updatePlayer(clientId, state);
        // Envoyer à tous les autres clients
        broadcastExcept(clientId, new NetworkMessage(
                NetworkMessage.MessageType.PLAYER_UPDATE, state));
    }

    public void handleClientDisconnect(int clientId) {
        clients.remove(clientId);
        System.out.println("Client #" + clientId + " déconnecté. Nombre de joueurs restants: " + clients.size());
        if (clients.isEmpty()) {
            stop();
        }
    }

    public static void main(String[] args) {
        // Pour tester indépendamment
        GameServer server = new GameServer(5555);
        server.start();
    }

    private class ClientHandler implements Runnable {
        private final int clientId;
        private final Socket socket;
        private final GameServer server;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private boolean running = true;

        public ClientHandler(int clientId, Socket socket, GameServer server) {
            this.clientId = clientId;
            this.socket = socket;
            this.server = server;

            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Envoyer l'ID au client
                sendMessage(new NetworkMessage(NetworkMessage.MessageType.CLIENT_ID, clientId));
            } catch (IOException e) {
                System.err.println("Erreur d'initialisation pour le client #" + clientId + ": " + e.getMessage());
                running = false;
            }
        }

        @Override
        public void run() {
            try {
                while (running) {
                    NetworkMessage message = (NetworkMessage) in.readObject();
                    handleMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erreur pour le client #" + clientId + ": " + e.getMessage());
            } finally {
                close();
                server.handleClientDisconnect(clientId);
            }
        }

        private void handleMessage(NetworkMessage message) {
            switch (message.getType()) {
                case PLAYER_UPDATE:
                    PlayerState state = (PlayerState) message.getData();
                    server.updatePlayerState(clientId, state);
                    break;
                case ENEMY_HIT:
                    // Add player ID to the hit data so receiver knows who hit it
                    GameClient.EnemyHitData hitData = (GameClient.EnemyHitData) message.getData();
                    // Forward the hit info to all clients (including sender for confirmation)
                    server.broadcast(message);
                    break;
                case ENEMY_SPAWN:
                    // When a host spawns an enemy, add sender's client ID and broadcast to others
                    GameClient.EnemySpawnData spawnData = (GameClient.EnemySpawnData) message.getData();
                    spawnData.setPlayerId(clientId); // Set who created this enemy
                    server.broadcast(new NetworkMessage(NetworkMessage.MessageType.ENEMY_SPAWN, spawnData));
                    break;
                case GAME_OVER:
                    // A player lost, inform the other
                    server.broadcastExcept(clientId, message);
                    PlayerState player = (PlayerState) message.getData();
                    try {
                        ConnexionBD.enregistrerJoueur(player.getAvionType(), player.getAvionType(), player.getAvionType(), player.getScore());
                    } catch (SQLException e) {
                        System.err.println("Erreur BD : " + e.getMessage());
                    }
                    server.broadcastExcept(clientId, message);
                    break;
                case CHAT_MESSAGE:
                    // Forward the chat message to all clients
                    server.broadcast(message);
                    break;
                default:
                    break;
            }
        }

        public void sendMessage(NetworkMessage message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("Erreur d'envoi au client #" + clientId + ": " + e.getMessage());
                running = false;
            }
        }

        public void close() {
            running = false;
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Erreur de fermeture pour le client #" + clientId + ": " + e.getMessage());
            }
        }
    }
}