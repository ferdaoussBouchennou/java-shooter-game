package org.example.projetjava.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.projetjava.client.ChatMessage;
import org.example.projetjava.client.GameClient;
import org.example.projetjava.model.Avion;
import org.example.projetjava.model.PlayerState;
import org.example.projetjava.model.ConnexionBD;
import org.example.projetjava.manager.AvionManager;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MultiplayerGameController {
    @FXML private AnchorPane rootPane;
    @FXML private Label scoreLabel;
    @FXML private Label vieLabel;
    @FXML private Label otherPlayerScoreLabel;
    @FXML private Label otherPlayerVieLabel;
    @FXML private javafx.scene.control.TextArea chatArea;
    @FXML private javafx.scene.control.TextField chatInput;
    @FXML private javafx.scene.control.Button sendButton;
    @FXML private VBox chatPane;
    @FXML private Button toggleChatButton;
    @FXML private Button hideChatButton;
    @FXML private Label chatNotificationBadge;

    private ImageView playerShip;
    private ImageView otherPlayerShip;
    private Avion avionData;
    private String playerName;
    private GameClient client;
    private boolean isHost;

    private final List<ImageView> projectiles = new ArrayList<>();
    private final List<ImageView> otherPlayerProjectiles = new ArrayList<>(); // Projectiles de l'autre joueur
    private final List<ImageView> ennemis = new ArrayList<>();
    private boolean movingLeft = false, movingRight = false, shooting = false;
    private AnimationTimer gameLoop;
    private int pointsVieActuels, score = 0;
    private long lastShotTime = 0;
    private ScheduledExecutorService gameExecutor;
    private boolean gameRunning = true;

    private int otherPlayerScore = 0;
    private int otherPlayerHealth = 0;
    private double otherPlayerX = 0;
    private boolean otherPlayerShooting = false;
    private long otherPlayerLastShotTime = 0;
    private Avion otherPlayerAvionData; // Données de l'avion de l'autre joueur
    private int nextEnemyId = 0;
    private int unreadMessageCount = 0;
    private boolean chatVisible = false;

    public void initializeGame(String playerName, GameClient client, boolean isHost, String avionChoisi) {
        this.playerName = playerName;
        this.client = client;
        this.isHost = isHost;

        // Récupérer l'avion sélectionné parmi ceux disponibles
        this.avionData = AvionManager.getAvion(avionChoisi);

        // Si l'avion n'est pas trouvé, prendre le premier avion disponible
        if (this.avionData == null) {
            System.err.println("Avion '" + avionChoisi + "' non trouvé, utilisation du premier avion disponible");
            Set<String> nomsAvions = AvionManager.getAvionNames();
            if (!nomsAvions.isEmpty()) {
                this.avionData = AvionManager.getAvion(nomsAvions.iterator().next());
            }
        }

        // Vérification finale
        if (this.avionData == null) {
            throw new RuntimeException("Aucun avion disponible dans AvionManager - vérifiez l'initialisation");
        }

        this.pointsVieActuels = avionData.getPointsVie();

        setupClientHandler();
        initializePlayerShip();
        initializeOtherPlayer();
        initializeUI();
        initializeChat();

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(this::handleKeyPressed);
                newScene.setOnKeyReleased(this::handleKeyReleased);
                System.out.println("Écouteurs clavier ajoutés à la scène");
            }
        });

        Platform.runLater(() -> {
            rootPane.requestFocus();  // Demander le focus explicitement
            System.out.println("Focus demandé pour rootPane");
        });

        System.out.println("Contrôleur initialisé et écouteurs clavier ajoutés");

        startGameThreads();
    }

    private void setupClientHandler() {
        client.setMessageHandler(new GameClient.MessageHandler() {
            @Override
            public void onPlayerUpdate(PlayerState state) {
                Platform.runLater(() -> {
                    // Mettre à jour la position et l'état de l'autre joueur
                    otherPlayerX = state.getX();
                    otherPlayerShip.setLayoutX(otherPlayerX);
                    otherPlayerScore = state.getScore();
                    otherPlayerHealth = state.getHealth();

                    // Mise à jour de l'état de tir de l'autre joueur
                    otherPlayerShooting = state.isShooting();

                    // Si le type d'avion a changé, mettre à jour les données de l'avion
                    String avionType = state.getAvionType();
                    if (otherPlayerAvionData == null || !otherPlayerAvionData.getNom().equals(avionType)) {
                        otherPlayerAvionData = AvionManager.getAvion(avionType);
                        // Mettre à jour l'image de l'avion
                        updateOtherPlayerShipImage(avionType);
                    }

                    otherPlayerShip.setVisible(true); // S'assurer que le vaisseau est visible
                    System.out.println("Mise à jour reçue du joueur: x=" + state.getX() + ", score=" + state.getScore() + ", shooting=" + state.isShooting());
                    updateOtherPlayerDisplay();
                });
            }

            @Override
            public void onEnemyHit(GameClient.EnemyHitData data) {
                Platform.runLater(() -> {
                    // Find the enemy by ID and remove it
                    ImageView enemyToRemove = null;

                    for (ImageView enemy : ennemis) {
                        Integer enemyId = (Integer) enemy.getUserData();
                        if (enemyId != null && enemyId == data.getEnemyId()) {
                            enemyToRemove = enemy;
                            break;
                        }
                    }

                    if (enemyToRemove != null) {
                        rootPane.getChildren().remove(enemyToRemove);
                        ennemis.remove(enemyToRemove);

                        // Update other player's score if it's from them
                        if (data.getPlayerId() != client.getClientId()) {
                            otherPlayerScore = data.getPoints();
                            updateOtherPlayerDisplay();
                        }
                    }
                });
            }

            @Override
            public void onEnemySpawn(GameClient.EnemySpawnData data) {
                Platform.runLater(() -> {
                    // Only create enemy if we didn't create it ourselves
                    // (i.e., if we're not the host or if the message is from another client)
                    if (!isHost || data.getPlayerId() != client.getClientId()) {
                        createEnemy(data.getEnemyId(), data.getEnemyType(), data.getXPosition());
                        System.out.println("Enemy spawned from network: ID=" + data.getEnemyId());
                    }
                });}

            @Override
            public void onGameStart() {
                Platform.runLater(() -> {
                    // Le jeu commence
                    System.out.println("Jeu démarré!");
                });
            }

            @Override
            public void onGameOver(int clientId) {
                Platform.runLater(() -> {
                    // Fin de partie
                    if (clientId != client.getClientId()) {
                        // L'autre joueur a perdu, donc nous avons gagné
                        showGameOver(true); // Vous avez gagné
                    } else {
                        // Nous avons perdu
                        showGameOver(false); // Game over
                    }
                });
            }
            @Override
            public void onChatMessage(ChatMessage message) {
                Platform.runLater(() -> {
                    String formattedMessage = String.format("[%s]: %s\n",
                            message.getSenderName(),
                            message.getMessage());
                    chatArea.appendText(formattedMessage);

                    // Si le message vient de l'autre joueur et que le chat est caché, incrémenter le compteur
                    if (message.getSenderId() != client.getClientId() && !chatVisible) {
                        unreadMessageCount++;
                        chatNotificationBadge.setText(String.valueOf(unreadMessageCount));
                        chatNotificationBadge.setVisible(true);
                    }
                });
            }

        });
    }

    private void initializePlayerShip() {
        try {
            String imagePath = avionData.getImagePath();
            InputStream is = getClass().getResourceAsStream(imagePath);
            if (is == null) {
                throw new IOException("Image non trouvée: " + imagePath);
            }

            Image image = new Image(is);
            playerShip = new ImageView(image);
            playerShip.setFitWidth(80);
            playerShip.setFitHeight(80);
            playerShip.setPreserveRatio(true);

            Platform.runLater(() -> {
                double initialX = isHost ? 200 : 600; // Positions fixes au lieu de relatives
                playerShip.setLayoutX(initialX - playerShip.getFitWidth()/2);
                playerShip.setLayoutY(rootPane.getHeight() - playerShip.getFitHeight() - 20);
                rootPane.getChildren().add(playerShip);
                playerShip.toFront(); // Met au premier plan
                System.out.println("Vaisseau joueur initialisé à x=" + playerShip.getLayoutX());
            });
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'avion: " + e.getMessage());
            // Solution de repli
            Image image = new Image(getClass().getResourceAsStream(
                    "/org/example/projetjava/kenney_space-shooter-redux/PNG/playerShip1_blue.png"));
            playerShip = new ImageView(image);
            Platform.runLater(() -> {
                rootPane.getChildren().add(playerShip);
                playerShip.toFront();
            });
        }
    }

    private void initializeOtherPlayer() {
        // Image temporaire par défaut
        Image defaultImage = new Image(getClass().getResourceAsStream(
                "/org/example/projetjava/kenney_space-shooter-redux/PNG/playerShip1_green.png"));
        otherPlayerShip = new ImageView(defaultImage);
        otherPlayerShip.setFitWidth(80);
        otherPlayerShip.setFitHeight(80);
        otherPlayerShip.setPreserveRatio(true);

        Platform.runLater(() -> {
            double initialX = isHost ? 600 : 200;
            otherPlayerShip.setLayoutX(initialX - otherPlayerShip.getFitWidth()/2);
            otherPlayerShip.setLayoutY(rootPane.getHeight() - otherPlayerShip.getFitHeight() - 20);
            rootPane.getChildren().add(otherPlayerShip);
            otherPlayerShip.toFront();
            System.out.println("Vaisseau adversaire initialisé temporairement à x=" + otherPlayerShip.getLayoutX());
        });
    }
    private void updateOtherPlayerShipImage(String avionType) {
        if (avionType == null || otherPlayerAvionData == null || otherPlayerShip == null) {
            return;
        }

        try {
            // Charger l'image de l'avion choisi par l'autre joueur
            String imagePath = otherPlayerAvionData.getImagePath();
            InputStream is = getClass().getResourceAsStream(imagePath);
            if (is == null) {
                System.err.println("Image de l'avion adverse non trouvée: " + imagePath);
                return;
            }

            Image newImage = new Image(is);
            otherPlayerShip.setImage(newImage);
            System.out.println("Image de l'avion adverse mise à jour: " + avionType);
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour de l'image de l'avion adverse: " + e.getMessage());
        }
    }

    private void initializeUI() {
        // Mise à jour des étiquettes avec texte formaté
        vieLabel.setText("Vie: " + pointsVieActuels + "/" + avionData.getPointsVie());
        scoreLabel.setText("Score: " + score);

        otherPlayerScoreLabel.setText("Score: " + otherPlayerScore);
        otherPlayerVieLabel.setText("Vie: " + otherPlayerHealth);

        // Appliquer des effets de transition aux labels pour les mises à jour
        setupLabelTransition(scoreLabel);
        setupLabelTransition(vieLabel);
        setupLabelTransition(otherPlayerScoreLabel);
        setupLabelTransition(otherPlayerVieLabel);
    }
    private void setupLabelTransition(Label label) {
        label.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(100), label);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.8);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(100), label);
                fadeIn.setFromValue(0.8);
                fadeIn.setToValue(1.0);

                fadeOut.setOnFinished(e -> fadeIn.play());
                fadeOut.play();
            }
        });
    }

    private void updateOtherPlayerDisplay() {
        otherPlayerScoreLabel.setText("Score: " + otherPlayerScore);
        otherPlayerVieLabel.setText("Vie: " + otherPlayerHealth);
        scoreLabel.setText("Score: " + score);
    }

    private void startGameThreads() {
        gameExecutor = Executors.newScheduledThreadPool(3);
        startEnemyGenerator();
        startEnemyMovement();
        startCollisionDetection();
        startOtherPlayerShooting(); // Démarrer le thread qui gère les tirs de l'autre joueur

        startMainGameLoop();
    }

    private void startMainGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            private long lastSentUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 16_000_000) { // ~60 FPS
                    updatePlayerPosition();
                    updateProjectiles();
                    updateOtherPlayerProjectiles(); // Mettre à jour les projectiles de l'autre joueur
                    handleShooting();

                    // Envoyer la mise à jour moins fréquemment pour réduire la charge réseau
                    if (now - lastSentUpdate >= 50_000_000) { // ~20 fois par seconde
                        sendPlayerUpdate();
                        lastSentUpdate = now;
                    }

                    lastUpdate = now;
                }
            }
        };
        gameLoop.start();
    }

    private void sendPlayerUpdate() {
        if (client != null && playerShip != null) {
            PlayerState state = new PlayerState(
                    playerShip.getLayoutX(),
                    playerShip.getLayoutY(),
                    avionData.getNom(),
                    score,
                    pointsVieActuels,
                    shooting
            );
            client.sendPlayerUpdate(state);
        }
    }

    private void updatePlayerPosition() {
        if (playerShip == null) return;

        double speed = avionData.getVitesse();
        double currentX = playerShip.getLayoutX();

        if (movingLeft) {
            double newX = currentX - speed;
            if (newX > 0) {
                playerShip.setLayoutX(newX);
            }
        }

        if (movingRight) {
            double newX = currentX + speed;
            if (newX < rootPane.getWidth() - playerShip.getFitWidth()) {
                playerShip.setLayoutX(newX);
            }
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

    private void fireProjectile() {
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

    // Gérer les tirs de l'autre joueur
    private void startOtherPlayerShooting() {
        gameExecutor.scheduleAtFixedRate(() -> {
            if (gameRunning && otherPlayerShooting && otherPlayerShip != null && otherPlayerShip.isVisible()) {
                long currentTime = System.currentTimeMillis();
                // Utiliser les caractéristiques de l'avion de l'autre joueur pour calculer le délai de tir
                long delay = 300;
                if (otherPlayerAvionData != null) {
                    delay = (long)(300 / (1 + otherPlayerAvionData.getPuissanceTir() * 0.2));
                }

                if (currentTime - otherPlayerLastShotTime > delay) {
                    Platform.runLater(() -> {
                        fireOtherPlayerProjectile();
                    });
                    otherPlayerLastShotTime = currentTime;
                }
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    private void fireOtherPlayerProjectile() {
        int puissanceTir = otherPlayerAvionData != null ? otherPlayerAvionData.getPuissanceTir() : 2;
        String projectileImage = getProjectileImage(puissanceTir);
        ImageView projectile = new ImageView(new Image(getClass().getResourceAsStream(projectileImage)));

        double size = 8 + puissanceTir * 2;
        projectile.setFitWidth(size);
        projectile.setPreserveRatio(true);

        projectile.setLayoutX(otherPlayerShip.getLayoutX() + otherPlayerShip.getFitWidth()/2 - size/2);
        projectile.setLayoutY(otherPlayerShip.getLayoutY() - 10);

        rootPane.getChildren().add(projectile);
        otherPlayerProjectiles.add(projectile);
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

    private void updateOtherPlayerProjectiles() {
        List<ImageView> toRemove = new ArrayList<>();

        double speed = 5;
        if (otherPlayerAvionData != null) {
            speed = 5 + otherPlayerAvionData.getVitesse() / 5.0;
        }

        for (ImageView projectile : otherPlayerProjectiles) {
            projectile.setLayoutY(projectile.getLayoutY() - speed);

            if (projectile.getLayoutY() + projectile.getFitHeight() < 0) {
                toRemove.add(projectile);
                rootPane.getChildren().remove(projectile);
            }
        }
        otherPlayerProjectiles.removeAll(toRemove);
    }

    private void startEnemyGenerator() {
        gameExecutor.scheduleAtFixedRate(() -> {
            if (gameRunning && ennemis.size() < 10 && isHost) { // Only host generates enemies
                Platform.runLater(() -> {
                    if (Math.random() < 0.1) { // Spawn rate
                        generateEnnemi();
                    }
                });
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

    private void generateEnnemi() {
        String[] enemyTypes = {
                "enemyRed1.png", "enemyBlack1.png",
                "enemyRed3.png", "enemyBlack3.png"
        };

        String enemyType = enemyTypes[(int)(Math.random() * enemyTypes.length)];
        String imagePath = "/org/example/projetjava/kenney_space-shooter-redux/PNG/Enemies/" + enemyType;

        double margin = 40 * 0.5;
        double xPos = margin + Math.random() * (rootPane.getWidth() - 40 - margin*2);

        int enemyId = nextEnemyId++;
        // Create enemy locally
        createEnemy(enemyId, enemyType, xPos);

        // Send to server
        if (client != null) {
            GameClient.EnemySpawnData spawnData = new GameClient.EnemySpawnData(enemyId, enemyType, xPos);
            client.sendEnemySpawn(spawnData);
        }
    }
    private void createEnemy(int enemyId, String enemyType, double xPos) {
        try {
            String imagePath = "/org/example/projetjava/kenney_space-shooter-redux/PNG/Enemies/" + enemyType;
            ImageView ennemi = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
            ennemi.setFitWidth(40);
            ennemi.setFitHeight(40);
            ennemi.setLayoutX(xPos);
            ennemi.setLayoutY(-40);
            ennemi.setUserData(enemyId); // Store ID for reference - critical for synchronization

            rootPane.getChildren().add(ennemi);
            ennemis.add(ennemi);
            System.out.println("Enemy created with ID: " + enemyId);
        } catch (Exception e) {
            System.err.println("Erreur création ennemi: " + e.getMessage());
        }
    }

    private void startEnemyMovement() {
        gameExecutor.scheduleAtFixedRate(() -> {
            if (gameRunning) {
                Platform.runLater(() -> {
                    updateEnnemis();
                });
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    private void updateEnnemis() {
        List<ImageView> toRemove = new ArrayList<>();

        for (ImageView ennemi : ennemis) {
            ennemi.setLayoutY(ennemi.getLayoutY() + 2); // Vitesse de base

            if (ennemi.getLayoutY() > rootPane.getHeight()) {
                toRemove.add(ennemi);
                rootPane.getChildren().remove(ennemi);
                prendreDegats(1);
            }
        }

        ennemis.removeAll(toRemove);
    }

    private void startCollisionDetection() {
        gameExecutor.scheduleAtFixedRate(() -> {
            if (gameRunning) {
                Platform.runLater(() -> {
                    checkCollisions();
                });
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    private void checkCollisions() {
        // Vérifier les collisions avec les projectiles du joueur
        checkPlayerProjectileCollisions();

        // Vérifier les collisions avec les projectiles de l'autre joueur
        checkOtherPlayerProjectileCollisions();

        // Vérifier les collisions des ennemis avec les vaisseaux
        checkShipCollisions();
    }

    private void checkPlayerProjectileCollisions() {
        List<ImageView> toRemoveProjectiles = new ArrayList<>();
        List<ImageView> toRemoveEnnemis = new ArrayList<>();

        for (ImageView projectile : projectiles) {
            for (int i = 0; i < ennemis.size(); i++) {
                ImageView ennemi = ennemis.get(i);
                if (checkCollision(projectile, ennemi)) {
                    toRemoveProjectiles.add(projectile);
                    toRemoveEnnemis.add(ennemi);
                    score += 10;
                    updateScoreDisplay();

                    // Get the actual enemy ID from its userData
                    int enemyId = (Integer) ennemi.getUserData();

                    // Send the info to the server
                    if (client != null) {
                        client.sendEnemyHit(enemyId, score);
                    }
                    break; // A projectile can only hit one enemy
                }
            }
        }

        projectiles.removeAll(toRemoveProjectiles);
        ennemis.removeAll(toRemoveEnnemis);
        rootPane.getChildren().removeAll(toRemoveProjectiles);
        rootPane.getChildren().removeAll(toRemoveEnnemis);
    }

    private void checkOtherPlayerProjectileCollisions() {
        List<ImageView> toRemoveProjectiles = new ArrayList<>();
        List<ImageView> toRemoveEnnemis = new ArrayList<>();

        for (ImageView projectile : otherPlayerProjectiles) {
            for (ImageView ennemi : ennemis) {
                if (checkCollision(projectile, ennemi)) {
                    toRemoveProjectiles.add(projectile);
                    toRemoveEnnemis.add(ennemi);
                    // Le score est géré côté serveur pour l'autre joueur
                }
            }
        }

        otherPlayerProjectiles.removeAll(toRemoveProjectiles);
        ennemis.removeAll(toRemoveEnnemis);
        rootPane.getChildren().removeAll(toRemoveProjectiles);
        rootPane.getChildren().removeAll(toRemoveEnnemis);
    }

    private void checkShipCollisions() {
        List<ImageView> toRemoveEnnemis = new ArrayList<>();

        for (ImageView ennemi : ennemis) {
            if (checkCollision(playerShip, ennemi)) {
                toRemoveEnnemis.add(ennemi);
                prendreDegats(1);
            }

            // Vérifier aussi les collisions avec le vaisseau de l'autre joueur
            if (otherPlayerShip.isVisible() && checkCollision(otherPlayerShip, ennemi)) {
                toRemoveEnnemis.add(ennemi);
                // Les dégâts pour l'autre joueur sont gérés côté serveur
            }
        }

        ennemis.removeAll(toRemoveEnnemis);
        rootPane.getChildren().removeAll(toRemoveEnnemis);
    }

    private boolean checkCollision(ImageView node1, ImageView node2) {
        return node1.getBoundsInParent().intersects(node2.getBoundsInParent());
    }

    private void prendreDegats(int degats) {
        pointsVieActuels -= degats;
        vieLabel.setText("Vie: " + pointsVieActuels + "/" + avionData.getPointsVie());

        if (pointsVieActuels <= 0) {
            gameOver();
        }
    }

    private void updateScoreDisplay() {
        scoreLabel.setText("Vous: " + score);
    }

    private void gameOver() {
        gameRunning = false;

        if (gameExecutor != null) {
            gameExecutor.shutdownNow();
        }

        if (gameLoop != null) {
            gameLoop.stop();
        }

        // Envoyer l'info au serveur
        if (client != null) {
            client.sendGameOver();
        }

        try {
            ConnexionBD.enregistrerJoueur(playerName, "Multijoueur", avionData.getNom(), score);
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde score: " + e.getMessage());
        }

        showGameOver(false);
    }

    private void showGameOver(boolean won) {
        Platform.runLater(() -> {
            Label gameOverLabel = new Label(won ? "VOUS AVEZ GAGNÉ!\nScore: " + score : "GAME OVER\nScore: " + score);
            gameOverLabel.setStyle("-fx-font-size: 40px; -fx-text-fill: " + (won ? "green" : "red") + "; -fx-background-color: rgba(0,0,0,0.7);");

            rootPane.getChildren().add(gameOverLabel);
            gameOverLabel.setLayoutX(rootPane.getWidth()/2 - 150);
            gameOverLabel.setLayoutY(rootPane.getHeight()/2 - 50);

            // Si le jeu n'est pas déjà terminé
            if (gameRunning) {
                gameRunning = false;

                if (gameLoop != null) {
                    gameLoop.stop();
                }

                if (gameExecutor != null && !gameExecutor.isShutdown()) {
                    gameExecutor.shutdownNow();
                }

                // Sauvegarder le score uniquement si nous sommes en game over (pas si nous avons gagné)
                if (!won) {
                    try {
                        ConnexionBD.enregistrerJoueur(playerName, "Multijoueur", avionData.getNom(), score);
                    } catch (SQLException e) {
                        System.err.println("Erreur sauvegarde score: " + e.getMessage());
                    }
                }
            }
        });
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        System.out.println("Touche pressée: " + event.getCode());
        switch(event.getCode()) {
            case LEFT:
                movingLeft = true;
                break;
            case RIGHT:
                movingRight = true;
                break;
            case SPACE:
                shooting = true;
                break;
        }
    }

    @FXML
    private void handleKeyReleased(KeyEvent event) {
        System.out.println("Touche relâchée: " + event.getCode());
        switch(event.getCode()) {
            case LEFT:
                movingLeft = false;
                break;
            case RIGHT:
                movingRight = false;
                break;
            case SPACE:
                shooting = false;
                break;
        }
    }
    private void initializeChat() {
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        // Rendre le panneau de chat invisible au démarrage
        chatPane.setVisible(false);
        chatVisible = false;

        // Initialiser le compteur de notifications
        unreadMessageCount = 0;
        chatNotificationBadge.setVisible(false);

        // Configurer les boutons du chat
        toggleChatButton.setOnAction(event -> toggleChatPane());
        hideChatButton.setOnAction(event -> toggleChatPane());

        // Configurer les actions pour l'envoi de messages
        sendButton.setOnAction(event -> sendChatMessage());
        chatInput.setOnAction(event -> sendChatMessage());

        // Message de bienvenue
        chatArea.appendText("Chat du jeu initialisé. Bienvenue " + playerName + "!\n");
    }
    // 4. Ajouter cette méthode pour envoyer des messages
    private void sendChatMessage() {
        String message = chatInput.getText();
        if (message != null && !message.trim().isEmpty()) {
            client.sendChatMessage(message);
            chatInput.clear();
            chatInput.requestFocus();
        }
    }
    private void toggleChatPane() {
        chatVisible = !chatVisible;
        chatPane.setVisible(chatVisible);

        // Si le chat devient visible, réinitialiser les notifications
        if (chatVisible) {
            unreadMessageCount = 0;
            chatNotificationBadge.setVisible(false);
        }

        // Remettre le focus sur la zone de jeu pour continuer à recevoir les événements clavier
        rootPane.requestFocus();
    }
}