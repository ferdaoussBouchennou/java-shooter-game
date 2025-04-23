package org.example.projetjava.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.projetjava.client.GameClient;
import org.example.projetjava.model.Avion;

import java.io.IOException;

public class MultiplayerController {
    @FXML private TextField playerNameField;
    @FXML private TextField serverAddressField;
    @FXML private ComboBox<String> avionComboBox;
    @FXML private Button connectButton;

    private GameClient client;

    @FXML
    public void initialize() {
        // Remplir la liste des avions
        avionComboBox.getItems().addAll("MiG-51S", "FIA-28A", "X-Wing", "DarkStar");
        avionComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleConnect() throws IOException {
        String playerName = playerNameField.getText().trim();
        String serverAddress = serverAddressField.getText().trim();
        String avion = avionComboBox.getValue();

        if (playerName.isEmpty() || serverAddress.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs");
            return;
        }

        try {
            GameClient client = new GameClient();
            client.connect(serverAddress, 5555);

            // Créer une NOUVELLE fenêtre sans fermer l'application
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/projetjava/MultiplayerGame.fxml"));
            Parent root = loader.load();

            Stage playerStage = new Stage();
            playerStage.setScene(new Scene(root, 800, 600));
            playerStage.setTitle(playerName + " (Joueur " + (client.getPlayerId()+1) + ")");
            playerStage.setOnCloseRequest(e -> {
                try {
                    client.disconnect();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            MultiplayerGameController controller = loader.getController();
            controller.initializeGame(playerName, avion, client.getPlayerId(), client);

            playerStage.show();

        } catch (Exception e) {
            showAlert("Erreur", "Impossible de se connecter: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void launchGame(String playerName, String avion, int playerId) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/projetjava/MultiplayerGame.fxml"));
        Parent root = loader.load();

        MultiplayerGameController controller = loader.getController();
        controller.initializeGame(playerName, avion, playerId, client);

        Stage stage = (Stage) connectButton.getScene().getWindow();
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}