package org.example.projetjava;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.animation.*;
import javafx.util.Duration;



public class JeuController {
    @FXML private StackPane rootPane;
    @FXML private Label readyText;

    public void showTransition(String playerName) {
        // Animation du texte
        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.7), readyText);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1.2);
        scale.setToY(1.2);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);

        // Disparition progressive après l'animation
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.seconds(1), rootPane);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(ev -> rootPane.setVisible(false));
            fade.play();
        });

        scale.play();
        pause.play();

        System.out.println("Transition pour: " + playerName);
    }
}