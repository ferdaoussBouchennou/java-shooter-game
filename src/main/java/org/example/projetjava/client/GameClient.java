package org.example.projetjava.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private int playerId;
    private Consumer<Object> dataHandler;
    private Thread listenThread;

    public void connect(String host, int port) throws IOException, ClassNotFoundException {
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());

        // Recevoir l'ID du joueur
        playerId = (int) input.readObject();
        System.out.println("Connecté en tant que joueur " + playerId);

        // Démarrer un thread pour écouter les messages du serveur
        listenThread = new Thread(this::listenForMessages);
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void listenForMessages() {
        try {
            while (true) {
                Object data = input.readObject();
                if (dataHandler != null) {
                    dataHandler.accept(data);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Déconnecté du serveur");
        }
    }

    public void send(Object data) throws IOException {
        output.writeObject(data);
        output.flush();
    }

    public void setDataHandler(Consumer<Object> handler) {
        this.dataHandler = handler;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void disconnect() throws IOException {
        if (listenThread != null) {
            listenThread.interrupt();
        }
        if (socket != null) {
            socket.close();
        }
    }
}