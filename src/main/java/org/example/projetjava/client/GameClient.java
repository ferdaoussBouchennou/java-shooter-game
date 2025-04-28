package org.example.projetjava.client;

import java.io.*;
import java.net.Socket;
import org.example.projetjava.model.PlayerState;
import org.example.projetjava.network.NetworkMessage;

public class GameClient {
    private String host;
    private int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = false;
    private ClientListener listener;
    private int clientId = -1;

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            // Démarrer le listener avant toute autre opération
            listener = new ClientListener();
            new Thread(listener).start();

            return true;
        } catch (IOException e) {
            System.err.println("Erreur de connexion: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            connected = false;
            if (listener != null) {
                listener.stop();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erreur de déconnexion: " + e.getMessage());
        }
    }

    public void sendPlayerUpdate(PlayerState state) {
        sendMessage(new NetworkMessage(NetworkMessage.MessageType.PLAYER_UPDATE, state));
    }

    public void sendEnemyHit(int enemyId, int points) {
        EnemyHitData data = new EnemyHitData(enemyId, points, clientId);
        sendMessage(new NetworkMessage(NetworkMessage.MessageType.ENEMY_HIT, data));
    }

    public void sendGameOver() {
        sendMessage(new NetworkMessage(NetworkMessage.MessageType.GAME_OVER, clientId));
    }

    private void sendMessage(NetworkMessage message) {
        if (!connected) return;

        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erreur d'envoi: " + e.getMessage());
            disconnect();
        }
    }

    public int getClientId() {
        return clientId;
    }

    private void setClientId(int id) {
        this.clientId = id;
        System.out.println("ID client attribué: " + id);
    }

    private class ClientListener implements Runnable {
        private boolean running = true;


        @Override
        public void run() {
            try {
                while (running && connected) {
                    NetworkMessage message = (NetworkMessage) in.readObject();
                    handleMessage(message);
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur de réception IO: " + e.getMessage());
                    e.printStackTrace(); // Print the full stack trace
                    disconnect();
                }
            } catch (ClassNotFoundException e) {
                if (running) {
                    System.err.println("Erreur de réception ClassNotFound: " + e.getMessage());
                    e.printStackTrace(); // Print the full stack trace
                    disconnect();
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("Erreur inattendue: " + e.getMessage());
                    e.printStackTrace(); // Print the full stack trace
                    disconnect();
                }
            }
        }

        private void handleMessage(NetworkMessage message) {
            switch (message.getType()) {
                case CLIENT_ID:
                    setClientId((Integer) message.getData());
                    System.out.println("ID client reçu: " + message.getData());
                    break;
                case PLAYER_UPDATE:
                    if (messageHandler != null) {
                        messageHandler.onPlayerUpdate((PlayerState) message.getData());
                    }
                    break;
                case ENEMY_HIT:
                    if (messageHandler != null) {
                        messageHandler.onEnemyHit((EnemyHitData) message.getData());
                    }
                    break;
                case GAME_START:
                    System.out.println("Message GAME_START reçu");
                    if (messageHandler != null) {
                        messageHandler.onGameStart();
                    }
                    break;
                case GAME_OVER:
                    if (messageHandler != null) {
                        messageHandler.onGameOver((Integer) message.getData());
                    }
                    break;
                case ENEMY_SPAWN:
                    if (messageHandler != null) {
                        messageHandler.onEnemySpawn((EnemySpawnData) message.getData());
                    }
                    break;
                default:
                    break;
            }
        }

        public void stop() {
            running = false;
        }
    }

    // Interface pour gérer les messages reçus du serveur
    private MessageHandler messageHandler;

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public interface MessageHandler {
        void onPlayerUpdate(PlayerState state);
        void onEnemyHit(EnemyHitData data);
        void onEnemySpawn(EnemySpawnData data);
        void onGameStart();
        void onGameOver(int clientId);
    }
    public static class EnemySpawnData implements Serializable {
        private static final long serialVersionUID = 1L;
        private int enemyId;
        private String enemyType;
        private double xPosition;
        private int playerId;

        public EnemySpawnData(int enemyId, String enemyType, double xPosition, int playerId) {
            this.enemyId = enemyId;
            this.enemyType = enemyType;
            this.xPosition = xPosition;
            this.playerId = playerId;
        }

        // For client-to-server messages, where playerId will be set by the server
        public EnemySpawnData(int enemyId, String enemyType, double xPosition) {
            this(enemyId, enemyType, xPosition, -1);
        }

        public int getEnemyId() {
            return enemyId;
        }

        public String getEnemyType() {
            return enemyType;
        }

        public double getXPosition() {
            return xPosition;
        }

        public int getPlayerId() {
            return playerId;
        }

        public void setPlayerId(int playerId) {
            this.playerId = playerId;
        }
    }
    public void sendEnemySpawn(EnemySpawnData data) {
        sendMessage(new NetworkMessage(NetworkMessage.MessageType.ENEMY_SPAWN, data));
    }
    public static class EnemyHitData implements Serializable {
        private static final long serialVersionUID = 1L;
        private int enemyId;
        private int points;
        private int playerId;

        public EnemyHitData(int enemyId, int points, int playerId) {
            this.enemyId = enemyId;
            this.points = points;
            this.playerId = playerId;
        }

        public int getEnemyId() {
            return enemyId;
        }

        public int getPoints() {
            return points;
        }

        public int getPlayerId() {
            return playerId;
        }
    }
}