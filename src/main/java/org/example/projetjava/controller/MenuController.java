package org.example.projetjava.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class MenuController {
    @FXML
    private Button local;

    @FXML
    private Button multi;

    @FXML
    private Button exit;

    @FXML
    private void initialize() {
        // Ajouter un effet de survol pour les boutons
        setupButtonHoverEffect(local);
        setupButtonHoverEffect(multi);
        setupButtonHoverEffect(exit);
    }

    private void setupButtonHoverEffect(Button button) {
        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "; -fx-scale-x: 1.05; -fx-scale-y: 1.05;");
        });

        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle().replace("; -fx-scale-x: 1.05; -fx-scale-y: 1.05;", ""));
        });
    }

    @FXML
    private void handleSinglePlayer(ActionEvent event) throws IOException {
        changeScene(event, "/org/example/projetjava/Main.fxml");
    }

    @FXML
    private void handleMultiplayer(ActionEvent event) throws IOException {
        changeScene(event, "/org/example/projetjava/Multiplayer.fxml");
    }

    @FXML
    private void handleExit(ActionEvent event) {
        System.exit(0);
    }

    private void changeScene(ActionEvent event, String fxmlPath) throws IOException {
        // Transition de fondu pour un changement de scène plus fluide
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), ((Node)event.getSource()).getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                Scene scene = new Scene(loader.load());
                stage.setScene(scene);

                // Transition de fondu à l'entrée
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), scene.getRoot());
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        fadeOut.play();
    }
}