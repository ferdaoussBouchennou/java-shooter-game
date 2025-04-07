package org.example.projetjava;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.animation.*;
import javafx.util.Duration;

public class JeuController {
    @FXML private AnchorPane rootPane;
    @FXML private StackPane transitionPane;
    @FXML private Label readyText;
    @FXML private ImageView backgroundImage;
    @FXML
    public void initialize() {
        backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
    }

    public void showTransition(String playerName) {
        // 1. Animation du texte
        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.7), readyText);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1.2);
        scale.setToY(1.2);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);

        // 2. Disparition progressive
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), transitionPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // 3. Séquence d'animations
        SequentialTransition sequence = new SequentialTransition(
                scale,
                new PauseTransition(Duration.seconds(1)), // Pause avant disparition
                fadeOut
        );

        sequence.setOnFinished(e -> {
            transitionPane.setVisible(false); // Masque complètement l'overlay
            startGame(); // Méthode pour démarrer le jeu
        });

        sequence.play();
        System.out.println("Transition pour: " + playerName);
    }

    private void startGame() {
        System.out.println("Le jeu commence maintenant !");
        // Ajoutez ici votre logique de démarrage du jeu
    }
}