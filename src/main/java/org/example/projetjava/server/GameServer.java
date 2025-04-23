package org.example.projetjava.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class GameServer {
    private ServerSocket serverSocket;
    private Socket[] playerSockets = new Socket[2];
    private ObjectOutputStream[] outputs = new ObjectOutputStream[2];
    private ObjectInputStream[] inputs = new ObjectInputStream[2];
    private BlockingQueue<Object>[] playerQueues = new BlockingQueue[2];
    private boolean[] playersConnected = new boolean[2];
    private boolean gameStarted = false;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Serveur démarré sur le port " + port);

        // Initialiser les queues pour chaque joueur
        for (int i = 0; i < 2; i++) {
            playerQueues[i] = new ArrayBlockingQueue<>(100);
            playersConnected[i] = false;
        }

        // Accepter les deux joueurs
        acceptPlayers();

        // Démarrer la boucle de diffusion
        new Thread(this::broadcastLoop).start();
    }

    private void acceptPlayers() throws IOException {
        for (int i = 0; i < 2; i++) {
            playerSockets[i] = serverSocket.accept();
            outputs[i] = new ObjectOutputStream(playerSockets[i].getOutputStream());
            inputs[i] = new ObjectInputStream(playerSockets[i].getInputStream());
            playersConnected[i] = true;

            // Envoyer l'ID du joueur
            outputs[i].writeObject(i);

            // Envoyer le statut de connexion
            if (i == 0) {
                outputs[i].writeObject("WAITING_FOR_PLAYER_2");
            } else {
                outputs[i].writeObject("PLAYER_2_CONNECTED");
                // Notifier le premier joueur
                outputs[0].writeObject("PLAYER_2_CONNECTED");
                outputs[0].flush();
                gameStarted = true;
                System.out.println("Les deux joueurs sont connectés - La partie peut commencer");
            }
            outputs[i].flush();

            System.out.println("Joueur " + (i+1) + " connecté (ID: " + i + ")");

            // Démarrer un thread pour écouter les messages de ce joueur
            final int playerId = i;
            new Thread(() -> listenToPlayer(playerId)).start();
        }
    }

    private void listenToPlayer(int playerId) {
        try {
            while (playersConnected[playerId]) {
                Object data = inputs[playerId].readObject();

                if ("PLAYER_READY".equals(data)) {
                    // Ajouter la logique de readiness si nécessaire
                } else if ("DISCONNECT".equals(data)) {
                    handleDisconnection(playerId);
                    break;
                } else {
                    // Ajouter le message à la queue de l'autre joueur
                    int otherPlayer = 1 - playerId;
                    if (playersConnected[otherPlayer]) {
                        playerQueues[otherPlayer].put(data);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Joueur " + playerId + " déconnecté: " + e.getMessage());
            handleDisconnection(playerId);
        }
    }

    private void handleDisconnection(int playerId) {
        try {
            playersConnected[playerId] = false;
            if (playerSockets[playerId] != null) {
                playerSockets[playerId].close();
            }

            // Notifier l'autre joueur
            int otherPlayer = 1 - playerId;
            if (playersConnected[otherPlayer]) {
                outputs[otherPlayer].writeObject("PLAYER_DISCONNECTED");
                outputs[otherPlayer].flush();
            }

            System.out.println("Joueur " + playerId + " s'est déconnecté");
        } catch (IOException e) {
            System.err.println("Erreur lors de la déconnexion du joueur " + playerId + ": " + e.getMessage());
        }
    }

    private void broadcastLoop() {
        try {
            while (gameStarted) {
                // Envoyer les messages de chaque queue au joueur correspondant
                for (int i = 0; i < 2; i++) {
                    if (!playersConnected[i]) continue;

                    while (!playerQueues[i].isEmpty()) {
                        Object data = playerQueues[i].take();
                        outputs[i].writeObject(data);
                        outputs[i].flush();
                    }
                }
                Thread.sleep(16); // ~60 FPS
            }
        } catch (Exception e) {
            System.err.println("Erreur dans la boucle de diffusion: " + e.getMessage());
        } finally {
            try {
                close();
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture du serveur: " + e.getMessage());
            }
        }
    }

    public void close() throws IOException {
        gameStarted = false;

        // Fermer toutes les connexions
        for (int i = 0; i < 2; i++) {
            if (outputs[i] != null) outputs[i].close();
            if (inputs[i] != null) inputs[i].close();
            if (playerSockets[i] != null) playerSockets[i].close();
        }

        if (serverSocket != null) serverSocket.close();
        System.out.println("Serveur arrêté");
    }
}