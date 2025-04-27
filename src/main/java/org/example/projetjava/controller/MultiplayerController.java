package org.example.projetjava.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.projetjava.client.GameClient;
import org.example.projetjava.manager.AvionManager;
import org.example.projetjava.model.Avion;
import org.example.projetjava.model.PlayerState;
import org.example.projetjava.network.GameServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiplayerController {
    @FXML private RadioButton radioHost;
    @FXML private RadioButton radioJoin;
    @FXML private ToggleGroup hostClientGroup;
    @FXML private TextField playerNameField;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button startButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> avionSelector;
    @FXML private ImageView avionPreview;
    @FXML private Label avionStatsLabel;

    private boolean isHost = true;
    private GameServer server;
    private GameClient client;
    private ExecutorService executorService;
    private String selectedAvion = "MiG-51S"; // Valeur par défaut

    @FXML
    public void initialize() {
        executorService = Executors.newSingleThreadExecutor();

        // Par défaut, sélectionner l'option d'hébergement
        radioHost.setSelected(true);
        handleHostClientSelection();

        // Remplir les champs par défaut
        ipField.setText("127.0.0.1");
        portField.setText("5555");

        // Configuration du sélecteur d'avions
        avionSelector.setItems(FXCollections.observableArrayList(AvionManager.getAvionNames()));
        avionSelector.setValue(selectedAvion);
        avionSelector.setOnAction(e -> updateAvionSelection());

        // Affichage initial de l'avion sélectionné
        updateAvionSelection();
    }

    private void updateAvionSelection() {
        selectedAvion = avionSelector.getValue();
        if (selectedAvion != null) {
            Avion avion = AvionManager.getAvion(selectedAvion);

            // Mettre à jour la prévisualisation
            try {
                InputStream is = getClass().getResourceAsStream(avion.getImagePath());
                if (is != null) {
                    Image image = new Image(is);
                    avionPreview.setImage(image);
                    avionPreview.setFitWidth(80);
                    avionPreview.setFitHeight(80);
                    avionPreview.setPreserveRatio(true);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
            }

            // Mettre à jour les stats
            avionStatsLabel.setText(String.format(
                    "Vitesse: %d\nPuissance: %d\nVie: %d",
                    avion.getVitesse(),
                    avion.getPuissanceTir(),
                    avion.getPointsVie()
            ));
        }
    }

    @FXML
    private void handleHostClientSelection() {
        isHost = radioHost.isSelected();
        ipField.setDisable(isHost);
        if (isHost) {
            statusLabel.setText("");
        }
    }

    @FXML
    private void handleStart() {
        String playerName = playerNameField.getText().trim();
        String ip = ipField.getText().trim();
        String portText = portField.getText().trim();

        // Validation des champs
        if (playerName.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer un nom de joueur.");
            return;
        }

        if (!isHost && ip.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer une adresse IP valide.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer un port valide (1024-65535).");
            return;
        }

        if (isHost) {
            // Démarrer le serveur
            startServer(port);
        }

        // Dans tous les cas, se connecter comme client
        boolean connected = connectAsClient(ip, port, playerName);
        if (connected) {
            statusLabel.setText("Connexion établie! En attente du second joueur...");
            startButton.setDisable(true);
            radioHost.setDisable(true);
            radioJoin.setDisable(true);
            playerNameField.setDisable(true);
            ipField.setDisable(true);
            portField.setDisable(true);
            avionSelector.setDisable(true);
        }
    }

    private void startServer(int port) {
        server = new GameServer(port);
        executorService.submit(() -> {
            System.out.println("Démarrage du serveur sur le port " + port + "...");
            server.start();
        });
        Platform.runLater(() -> {
            statusLabel.setText("Serveur démarré sur le port " + port);
        });
    }

    private boolean connectAsClient(String host, int port, String playerName) {
        client = new GameClient(host, port);
        client.setMessageHandler(new GameClient.MessageHandler() {
            @Override
            public void onPlayerUpdate(PlayerState state) {
                // Géré par MultiplayerGameController
            }

            @Override
            public void onEnemyHit(GameClient.EnemyHitData data) {
                // Géré par MultiplayerGameController
            }

            @Override
            public void onGameStart() {
                Platform.runLater(() -> {
                    System.out.println("Reçu GAME_START du serveur");
                    launchGame(playerName);
                });
            }

            @Override
            public void onGameOver(int clientId) {
                // Géré par MultiplayerGameController
            }
        });

        boolean connected = client.connect();
        if (connected) {
            System.out.println("Connecté au serveur en tant que " + (isHost ? "hôte" : "client"));
        }
        return connected;
    }

    private void launchGame(String playerName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/projetjava/MultiplayerGame.fxml"));
            Parent root = loader.load();

            MultiplayerGameController controller = loader.getController();
            controller.initializeGame(playerName, client, isHost, selectedAvion);

            Stage stage = (Stage) startButton.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600); // Taille fixe
            stage.setScene(scene);
            stage.setResizable(false); // Empêche le redimensionnement
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de lancer le jeu: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        cleanupResources();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/projetjava/Menu.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(loader.load()));
    }

    private void cleanupResources() {
        if (client != null) {
            client.disconnect();
        }

        if (server != null) {
            server.stop();
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}