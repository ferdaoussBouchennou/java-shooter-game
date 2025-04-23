package org.example.projetjava.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.example.projetjava.client.GameClient;
import org.example.projetjava.manager.AvionManager;
import org.example.projetjava.model.Avion;

import java.io.IOException;
import java.io.Serializable;

public class MultiplayerGameController {
    @FXML private AnchorPane rootPane;

    private Label scoreLabel;
    private Label vieLabel;
    private Label enemyScoreLabel;
    private Label enemyVieLabel;
    private ImageView playerShip;
    private ImageView enemyShip;
    private Avion avionData;
    private GameClient client;
    private int playerId;
    private int score = 0;
    private int pointsVieActuels;
    private long lastShotTime = 0;

    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean shooting = false;
    private AnimationTimer gameLoop;

    public void initializeGame(String playerName, String avionChoisi, int playerId, GameClient client) {
        this.playerId = playerId;
        this.client = client;
        this.avionData = AvionManager.getAvion(avionChoisi);
        this.pointsVieActuels = avionData.getPointsVie();

        initializeUI();
        initializePlayerShip();

        Label statusLabel = new Label(playerId == 0 ? "En attente du 2ème joueur..." : "Connecté au serveur");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24; -fx-font-weight: bold;");
        statusLabel.setLayoutX(250);
        statusLabel.setLayoutY(300);
        rootPane.getChildren().add(statusLabel);

        setupKeyListeners();

        client.setDataHandler(data -> {
            Platform.runLater(() -> {
                if (data instanceof String) {
                    String message = (String)data;
                    switch(message) {
                        case "PLAYER_2_CONNECTED":
                            statusLabel.setText("Partie commence!");
                            new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                                rootPane.getChildren().remove(statusLabel);
                                startGame();
                            })).play();
                            break;
                        case "GAME_OVER":
                            showGameOver(false);
                            break;
                    }
                } else if (data instanceof GameState) {
                    updateEnemyState((GameState) data);
                }
            });
        });
    }

    private void initializeUI() {
        scoreLabel = new Label("Score: 0");
        vieLabel = new Label("Vie: " + pointsVieActuels + "/" + avionData.getPointsVie());
        enemyScoreLabel = new Label("Ennemi: 0");
        enemyVieLabel = new Label("Vie Ennemi: ?");

        String labelStyle = "-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;";
        scoreLabel.setStyle(labelStyle);
        vieLabel.setStyle(labelStyle);
        enemyScoreLabel.setStyle(labelStyle);
        enemyVieLabel.setStyle(labelStyle);

        AnchorPane.setTopAnchor(scoreLabel, 10.0);
        AnchorPane.setLeftAnchor(scoreLabel, 10.0);
        AnchorPane.setTopAnchor(vieLabel, 40.0);
        AnchorPane.setLeftAnchor(vieLabel, 10.0);
        AnchorPane.setTopAnchor(enemyScoreLabel, 10.0);
        AnchorPane.setRightAnchor(enemyScoreLabel, 10.0);
        AnchorPane.setTopAnchor(enemyVieLabel, 40.0);
        AnchorPane.setRightAnchor(enemyVieLabel, 10.0);

        rootPane.getChildren().addAll(scoreLabel, vieLabel, enemyScoreLabel, enemyVieLabel);
    }

    private void initializePlayerShip() {
        playerShip = new ImageView(new Image(getClass().getResourceAsStream(avionData.getImagePath())));
        playerShip.setFitWidth(80);
        playerShip.setFitHeight(80);
        playerShip.setLayoutX(playerId == 0 ? 100 : 600);
        playerShip.setLayoutY(500);

        enemyShip = new ImageView(new Image(getClass().getResourceAsStream(
                playerId == 0 ? "/org/example/projetjava/kenney_space-shooter-redux/PNG/playerShip2_orange.png" :
                        "/org/example/projetjava/kenney_space-shooter-redux/PNG/playerShip1_blue.png")));
        enemyShip.setFitWidth(80);
        enemyShip.setFitHeight(80);
        enemyShip.setLayoutX(playerId == 0 ? 600 : 100);
        enemyShip.setLayoutY(500);
        enemyShip.setVisible(false);

        rootPane.getChildren().addAll(playerShip, enemyShip);
    }

    private void setupKeyListeners() {
        Scene scene = rootPane.getScene();

        scene.setOnKeyPressed(e -> {
            switch(e.getCode()) {
                case LEFT: movingLeft = true; break;
                case RIGHT: movingRight = true; break;
                case SPACE: shooting = true; break;
            }
        });

        scene.setOnKeyReleased(e -> {
            switch(e.getCode()) {
                case LEFT: movingLeft = false; break;
                case RIGHT: movingRight = false; break;
                case SPACE: shooting = false; break;
            }
        });
    }

    private void startGame() {
        enemyShip.setVisible(true);

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updatePlayerPosition();
                handleShooting();
                sendGameState();
            }
        };
        gameLoop.start();
    }

    private void updatePlayerPosition() {
        double speed = avionData.getVitesse();
        double currentX = playerShip.getLayoutX();

        if (movingLeft) {
            double newX = currentX - speed;
            if (newX > 0) playerShip.setLayoutX(newX);
        }

        if (movingRight) {
            double newX = currentX + speed;
            if (newX < rootPane.getWidth() - playerShip.getFitWidth()) playerShip.setLayoutX(newX);
        }
    }

    private void handleShooting() {
        if (shooting && System.currentTimeMillis() - lastShotTime > 300) {
            fireProjectile();
            lastShotTime = System.currentTimeMillis();
        }
    }

    private void fireProjectile() {
        // Implémentez la logique de tir ici
    }

    private void sendGameState() {
        try {
            GameState state = new GameState(
                    playerId,
                    playerShip.getLayoutX(),
                    playerShip.getLayoutY(),
                    score,
                    pointsVieActuels,
                    movingLeft,
                    movingRight,
                    shooting
            );
            client.send(state);
        } catch (IOException e) {
            System.err.println("Erreur d'envoi de l'état du jeu: " + e.getMessage());
        }
    }

    private void updateEnemyState(GameState state) {
        if (state.playerId != this.playerId) {
            enemyShip.setLayoutX(state.x);
            enemyShip.setLayoutY(state.y);
            enemyScoreLabel.setText("Ennemi: " + state.score);
            enemyVieLabel.setText("Vie Ennemi: " + state.vie);
            enemyShip.setRotate(state.movingLeft ? -15 : state.movingRight ? 15 : 0);
        }
    }

    private void showGameOver(boolean win) {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        Label gameOverLabel = new Label(win ? "VOUS AVEZ GAGNÉ!" : "GAME OVER");
        gameOverLabel.setStyle("-fx-text-fill: " + (win ? "green" : "red") + "; -fx-font-size: 48; -fx-font-weight: bold;");
        gameOverLabel.setLayoutX(250);
        gameOverLabel.setLayoutY(250);

        rootPane.getChildren().add(gameOverLabel);
    }

    public static class GameState implements Serializable {
        public int playerId;
        public double x, y;
        public int score;
        public int vie;
        public boolean movingLeft;
        public boolean movingRight;
        public boolean shooting;

        public GameState(int playerId, double x, double y, int score, int vie,
                         boolean movingLeft, boolean movingRight, boolean shooting) {
            this.playerId = playerId;
            this.x = x;
            this.y = y;
            this.score = score;
            this.vie = vie;
            this.movingLeft = movingLeft;
            this.movingRight = movingRight;
            this.shooting = shooting;
        }
    }
}