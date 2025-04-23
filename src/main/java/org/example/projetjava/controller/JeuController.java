package org.example.projetjava.controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.example.projetjava.model.Avion;
import org.example.projetjava.manager.AvionManager;
import org.example.projetjava.model.ConnexionBD;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class JeuController {
    @FXML private AnchorPane rootPane;
    @FXML private StackPane transitionPane;
    @FXML private Label readyText;
    @FXML private ImageView backgroundImage;
    @FXML private Label scoreLabel;
    @FXML private Label vieLabel;

    private ImageView playerShip;
    private Avion avionData;
    private String playerName;
    private final List<ImageView> projectiles = new ArrayList<>();
    private final List<ImageView> ennemis = new ArrayList<>();
    private boolean movingLeft = false, movingRight = false, shooting = false;
    private AnimationTimer gameLoop;
    private int pointsVieActuels, score = 0;
    private long lastShotTime = 0;
    private ScheduledExecutorService gameExecutor;
    private boolean gameRunning = true;
    private String currentNiveau;
    private DifficultySettings difficulty;
    @FXML
    private void initialize() {
        backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
        rootPane.setFocusTraversable(true);
        rootPane.requestFocus();
    }
    public void showTransition(String playerName) {
        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.7), readyText);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1.2);
        scale.setToY(1.2);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), transitionPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        SequentialTransition sequence = new SequentialTransition(
                scale,
                new PauseTransition(Duration.seconds(1)),
                fadeOut
        );
        sequence.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                transitionPane.setVisible(false);
                startGame();
            }
        });
        sequence.play();
    }
    private void setupDifficulty() {
        switch(currentNiveau) {
            case "Intermédiaire":
                difficulty = new DifficultySettings(0.15, 3, 1.2, 10, 8, 1.0);
                break;
            case "Haut niveau":
                difficulty = new DifficultySettings(0.25, 5, 1.5, 8, 12, 1.2);
                break;
            default:
                difficulty = new DifficultySettings(0.08, 2, 0.8, 15, 5, 0.8);
        }
    }
    public void initializeGame(String playerName, String niveau, String avionChoisi) {
        this.playerName = playerName;  // Initialiser le champ playerName
        this.currentNiveau = niveau;
        setupDifficulty();
        this.avionData = AvionManager.getAvion(avionChoisi);
        if (this.avionData == null) {
            throw new RuntimeException("Avion non trouvé: " + avionChoisi);
        }
        this.pointsVieActuels = avionData.getPointsVie();
        showTransition(playerName);
    }
    private void startGame() {
        initializePlayerShip();
        initializeUI();
        startGameThreads();
        rootPane.requestFocus();
    }
    private void initializePlayerShip() {
        if (avionData == null) {
            System.err.println("Aucune donnée d'avion disponible");
            return;
        }

        try {
            String imagePath = avionData.getImagePath();
            System.out.println("Tentative de chargement: " + imagePath); // Debug

            InputStream is = getClass().getResourceAsStream(imagePath);
            if (is == null) {
                throw new RuntimeException("Fichier image introuvable: " + imagePath);
            }

            Image image = new Image(is);
            playerShip = new ImageView(image);
            playerShip.setFitWidth(80);
            playerShip.setFitHeight(80);
            playerShip.setPreserveRatio(true);

            playerShip.setLayoutX(rootPane.getWidth()/2 - playerShip.getFitWidth()/2);
            playerShip.setLayoutY(rootPane.getHeight() - playerShip.getFitHeight() - 20);

            rootPane.getChildren().add(playerShip);
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'avion: " + e.getMessage());
            // Solution de repli
            playerShip = new ImageView(new Image(
                    getClass().getResourceAsStream(
                            "/org/example/projetjava/kenney_space-shooter-redux/PNG/playerShip1_blue.png")));
            rootPane.getChildren().add(playerShip);
        }
    }
    private void initializeUI() {

        vieLabel = new Label("Vie: " + pointsVieActuels + "/" + avionData.getPointsVie());
        vieLabel.setFont(new Font("Arial", 16));
        vieLabel.setTextFill(Color.WHITE);
        vieLabel.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 5px;");
        AnchorPane.setTopAnchor(vieLabel, 10.0);
        AnchorPane.setLeftAnchor(vieLabel, 10.0);
        rootPane.getChildren().add(vieLabel);


        scoreLabel = new Label("Score: 0");
        scoreLabel.setFont(new Font("Arial", 16));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 5px;");
        AnchorPane.setTopAnchor(scoreLabel, 10.0);
        AnchorPane.setRightAnchor(scoreLabel, 10.0);
        rootPane.getChildren().add(scoreLabel);
    }

    private void startGameThreads() {

        gameExecutor = Executors.newScheduledThreadPool(3);
        startEnemyGenerator();
        startEnemyMovement();
        startCollisionDetection();


        startMainGameLoop();
    }
    private void startEnemyGenerator() {
        gameExecutor.scheduleAtFixedRate(new EnemyGeneratorTask(), 0, 200, TimeUnit.MILLISECONDS);
    }

    private class EnemyGeneratorTask implements Runnable {
        @Override
        public void run() {
            if (gameRunning && ennemis.size() < difficulty.maxEnemies) {
                double adjustedtauxApparition = difficulty.tauxApparition *
                        (1 + (difficulty.maxEnemies - ennemis.size()) / 2.0);
                if (Math.random() < adjustedtauxApparition) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            generateEnnemi();
                            if (Math.random() < 0.3 && ennemis.size() < difficulty.maxEnemies) {
                                generateEnnemi();
                            }
                        }
                    });
                }
            }
        }
    }
    private void updatePlayerPosition() {
        if (playerShip == null) return;

        double speed = avionData.getVitesse() / 2.0;
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
        if (shooting && playerShip != null) {
            long currentTime = System.currentTimeMillis();

            long delay = (long)(300 / (1 + avionData.getPuissanceTir() * 0.2));

            if (currentTime - lastShotTime > delay) {
                fireProjectile();
                lastShotTime = currentTime;
            }
        }
    }
    private void startEnemyMovement() {
        gameExecutor.scheduleAtFixedRate(new EnemyMovementTask(), 0, 16, TimeUnit.MILLISECONDS);
    }

    private class EnemyMovementTask implements Runnable {
        @Override
        public void run() {
            if (gameRunning) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        updateEnnemis();
                    }
                });
            }
        }
    }
    private void startCollisionDetection() {
        gameExecutor.scheduleAtFixedRate(new CollisionDetectionTask(), 0, 16, TimeUnit.MILLISECONDS);
    }

    private class CollisionDetectionTask implements Runnable {
        @Override
        public void run() {
            if (gameRunning) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        checkCollisions();
                    }
                });
            }
        }
    }
    private void startMainGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updatePlayerPosition();
                updateProjectiles();
                handleShooting();
            }
        };
        gameLoop.start();
    }
    private String getProjectileImage(int puissanceTir) {
        if (puissanceTir >= 4) {
            return "/org/example/projetjava/kenney_space-shooter-redux/PNG/Lasers/laserRed06.png";
        } else if (puissanceTir >= 2) {
            return "/org/example/projetjava/kenney_space-shooter-redux/PNG/Lasers/laserGreen01.png";
        } else {
            return "/org/example/projetjava/kenney_space-shooter-redux/PNG/Lasers/laserBlue03.png";
        }
    }
    private void fireProjectile() {
        if (avionData == null) return;
        String projectileImage = getProjectileImage(avionData.getPuissanceTir());
        ImageView projectile = new ImageView(new Image(getClass().getResourceAsStream(projectileImage)));
        double size = 8 + avionData.getPuissanceTir() * 2;
        projectile.setFitWidth(size);
        projectile.setPreserveRatio(true);

        projectile.setLayoutX(playerShip.getLayoutX() + playerShip.getFitWidth()/2 - size/2);
        projectile.setLayoutY(playerShip.getLayoutY() - 10);

        rootPane.getChildren().add(projectile);
        projectiles.add(projectile);
    }
    private void updateProjectiles() {
        List<ImageView> toRemove = new ArrayList<>();

        for (ImageView projectile : projectiles) {
            double speed = 5 + avionData.getVitesse() / 5.0;
            projectile.setLayoutY(projectile.getLayoutY() - speed);

            if (projectile.getLayoutY() + projectile.getFitHeight() < 0) {
                toRemove.add(projectile);
                rootPane.getChildren().remove(projectile);
            }
        }
        projectiles.removeAll(toRemove);
    }
    private String[] getEnemyTypes() {
        switch(currentNiveau) {
            case "Intermédiaire":
                return new String[]{"enemyRed3.png", "enemyBlack3.png"};
            case "Haut niveau":
                return new String[]{"enemyRed5.png", "enemyBlack5.png"};
            default:
                return new String[]{"enemyRed1.png", "enemyBlack1.png"};
        }
    }
    private void generateEnnemi() {
        if (ennemis.size() >= difficulty.maxEnemies) return;

        String[] enemyTypes = getEnemyTypes();
        String imagePath = "/org/example/projetjava/kenney_space-shooter-redux/PNG/Enemies/" +
                enemyTypes[(int)(Math.random() * enemyTypes.length)];
        try {
            ImageView ennemi = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));

            double size = 40 * difficulty.enemySizeMultiplier;
            ennemi.setFitWidth(size);
            ennemi.setFitHeight(size);


            double margin = size * 0.5;
            double xPos = margin + Math.random() * (rootPane.getWidth() - size - margin*2);

            ennemi.setLayoutX(xPos);
            ennemi.setLayoutY(-size);

            rootPane.getChildren().add(ennemi);
            ennemis.add(ennemi);


            FadeTransition ft = new FadeTransition(Duration.millis(300), ennemi);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            System.err.println("Erreur création ennemi: " + e.getMessage());
        }
    }

    private void updateEnnemis() {
        List<ImageView> toRemove = new ArrayList<>();
        double dangerZone = rootPane.getHeight() - 150;

        for (ImageView ennemi : ennemis) {
            double speedVariation = 0.5 + Math.random() * 0.5;
            ennemi.setLayoutY(ennemi.getLayoutY() + difficulty.speed * speedVariation);

            if (ennemi.getLayoutY() > rootPane.getHeight()) {
                toRemove.add(ennemi);
                rootPane.getChildren().remove(ennemi);

                prendreDegats((int)(15 * difficulty.damageMultiplier));
            }
            else if (ennemi.getLayoutY() > dangerZone) {
                ennemi.setEffect(new Glow(0.3 + (ennemi.getLayoutY() - dangerZone) / 150 * 0.7));
            }
        }
        ennemis.removeAll(toRemove);
    }
    private void prendreDegats(double baseDamage) {

        pointsVieActuels -=1;
        updateVieDisplay();

        FadeTransition ft = new FadeTransition(Duration.millis(200), playerShip);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(4);
        ft.setAutoReverse(true);
        ft.play();

        if (pointsVieActuels <= 0) {
            gameOver();
        }
    }
    private boolean checkCollision(ImageView node1, ImageView node2) {
        return node1.getBoundsInParent().intersects(node2.getBoundsInParent());
    }
    private void checkCollisions() {
        List<ImageView> toRemoveProjectiles = new ArrayList<>();
        List<ImageView> toRemoveEnnemis = new ArrayList<>();


        for (ImageView projectile : projectiles) {
            for (ImageView ennemi : ennemis) {
                if (checkCollision(projectile, ennemi)) {
                    toRemoveProjectiles.add(projectile);
                    toRemoveEnnemis.add(ennemi);
                    score += difficulty.scorePerEnemy;
                    updateScoreDisplay();
                }
            }
        }

        for (ImageView ennemi : ennemis) {
            if (checkCollision(playerShip, ennemi)) {
                toRemoveEnnemis.add(ennemi);
                prendreDegats(25);
            }
        }


        projectiles.removeAll(toRemoveProjectiles);
        ennemis.removeAll(toRemoveEnnemis);
        rootPane.getChildren().removeAll(toRemoveProjectiles);
        rootPane.getChildren().removeAll(toRemoveEnnemis);
    }
    private void updateVieDisplay() {
        vieLabel.setText("Vie: " + pointsVieActuels + "/" + avionData.getPointsVie());

        double pourcentage = (double)pointsVieActuels / avionData.getPointsVie();
        if (pourcentage < 0.3) {
            vieLabel.setTextFill(Color.RED);
        } else if (pourcentage < 0.6) {
            vieLabel.setTextFill(Color.ORANGE);
        }
    }
    private void gameOver() {
        gameRunning = false;
        if (gameExecutor != null) {
            gameExecutor.shutdownNow();
        }

        try {
            ConnexionBD.enregistrerJoueur(this.playerName, currentNiveau, avionData.getNom(), score);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du score: " + e.getMessage());
        }

        if (gameLoop != null) {
            gameLoop.stop();
        }

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Label gameOverLabel = new Label("GAME OVER\nScore: " + score);
                gameOverLabel.setFont(new Font("Arial", 40));
                gameOverLabel.setTextFill(Color.RED);
                gameOverLabel.setTextAlignment(TextAlignment.CENTER);
                gameOverLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 20px;");

                gameOverLabel.setLayoutX(rootPane.getWidth()/2 - 150);
                gameOverLabel.setLayoutY(rootPane.getHeight()/2 - 50);

                rootPane.getChildren().add(gameOverLabel);
            }
        });
    }

    private void updateScoreDisplay() {
        scoreLabel.setText("Score: " + score);
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        switch(event.getCode()) {
            case LEFT: movingLeft = true; break;
            case RIGHT: movingRight = true; break;
            case SPACE: shooting = true; break;
        }
    }

    @FXML
    private void handleKeyReleased(KeyEvent event) {
        switch(event.getCode()) {
            case LEFT: movingLeft = false; break;
            case RIGHT: movingRight = false; break;
            case SPACE: shooting = false; break;
        }
    }

    private static class DifficultySettings {
        final double tauxApparition;
        final int speed;
        final double damageMultiplier;
        final int scorePerEnemy;
        final int maxEnemies;
        final double enemySizeMultiplier;

        DifficultySettings(double tauxApparition, int speed, double damageMultiplier,
                           int scorePerEnemy, int maxEnemies, double sizeMultiplier) {
            this.tauxApparition = tauxApparition;
            this.speed = speed;
            this.damageMultiplier = damageMultiplier;
            this.scorePerEnemy = scorePerEnemy;
            this.maxEnemies = maxEnemies;
            this.enemySizeMultiplier = sizeMultiplier;
        }
    }
}