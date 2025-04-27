package org.example.projetjava.controller;

import javafx.animation.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.projetjava.manager.AvionManager;
import org.example.projetjava.model.Avion;
import org.example.projetjava.model.ConnexionBD;
import org.example.projetjava.model.Joueur;

import java.io.IOException;
import java.sql.SQLException;

public class MainController {
    // Éléments FXML
    @FXML private ComboBox<String> playerComboBox;
    @FXML private TextField playerNameField;
    @FXML private RadioButton button1, button2, button3, button4;
    @FXML private ProgressBar prog1, prog2, prog3;
    @FXML private Button start, quit;
    @FXML private ImageView image1, image2, image3, image4;
    @FXML private ToggleGroup niveauGroup, avionGroup;
    @FXML private RadioButton niveauDebutant, niveauIntermediaire, niveauHaut;
    @FXML private Label vitesseLabel, puissanceLabel, vieLabel;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        animateWelcomeTitle();
        loadExistingPlayers();
        setupPlayerSelection();
        setupNiveauSelection();
        setupAvionSelection();
        loadAvionImages();
        setupButtonActions();
    }
    private void animateWelcomeTitle() {
        welcomeLabel.setOpacity(0);
        welcomeLabel.setTranslateY(-20);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.01), welcomeLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideDown = new TranslateTransition(Duration.seconds(1.5), welcomeLabel);
        slideDown.setFromY(-20);
        slideDown.setToY(0);
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(0.5), welcomeLabel);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(3);
        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(fadeIn, slideDown),
                pulse
        );

        sequence.play();
    }
    private void loadExistingPlayers() {
        try {
            playerComboBox.getItems().clear();
            playerComboBox.getItems().add("Nouveau joueur");
            playerComboBox.getItems().add("DefaultPlayer");

            for (String joueur : ConnexionBD.getJoueursExistants()) {
                if (!playerComboBox.getItems().contains(joueur)) {
                    playerComboBox.getItems().add(joueur);
                }
            }
        } catch (SQLException e) {
            showAlert("Erreur BD", "Impossible de charger les joueurs");
        }
    }
    private void setupPlayerSelection() {
        playerComboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String selected = playerComboBox.getValue();
                boolean isNewPlayer = "Nouveau joueur".equals(selected);

                playerNameField.setDisable(!isNewPlayer);

                if (!isNewPlayer) {
                    playerNameField.clear();
                    loadPlayerPreferences(selected);
                }
            }
        });
    }
    private void loadPlayerPreferences(String playerName) {
        try {
            Joueur joueur = ConnexionBD.getJoueur(playerName);
            if (joueur != null) {
                for (javafx.scene.control.Toggle toggle : niveauGroup.getToggles()) {
                    if (((RadioButton)toggle).getText().equals(joueur.getNiveau())) {
                        niveauGroup.selectToggle(toggle);
                    }
                }
                for (javafx.scene.control.Toggle toggle : avionGroup.getToggles()) {
                    if (((RadioButton)toggle).getText().equals(joueur.getAvion())) {
                        avionGroup.selectToggle(toggle);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur de chargement des préférences: " + e.getMessage());
        }
    }
    private void setupNiveauSelection() {
        niveauGroup = new ToggleGroup();
        niveauDebutant.setToggleGroup(niveauGroup);
        niveauIntermediaire.setToggleGroup(niveauGroup);
        niveauHaut.setToggleGroup(niveauGroup);
        niveauGroup.selectToggle(niveauDebutant);
    }
    private void setupAvionSelection() {
        avionGroup = new ToggleGroup();
        button1.setToggleGroup(avionGroup);
        button2.setToggleGroup(avionGroup);
        button3.setToggleGroup(avionGroup);
        button4.setToggleGroup(avionGroup);

        avionGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> obs, Toggle oldVal, Toggle newVal) {
                if (newVal != null) {
                    updateAvionStats(((RadioButton)newVal).getId());
                }
            }
        });
    }
    private void loadAvionImages() {
        image1.setImage(loadImage("playerShip1_blue.png"));
        image2.setImage(loadImage("playerShip2_orange.png"));
        image3.setImage(loadImage("playerShip3_green.png"));
        image4.setImage(loadImage("playerShip2_red.png"));

        // Associer chaque RadioButton à un ID d'avion
        button1.setId("MiG-51S");
        button2.setId("FIA-28A");
        button3.setId("X-Wing");
        button4.setId("DarkStar");
    }
    private Image loadImage(String filename) {
        try {
            return new Image(getClass().getResourceAsStream(
                    "/org/example/projetjava/kenney_space-shooter-redux/PNG/" + filename));
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image: " + filename);
            return null;
        }
    }

    private void setupButtonActions() {
        start.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleStart();
            }
        });
        quit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.exit(0);
            }
        });
    }

    private void updateAvionStats(String avionId) {
        Avion avion = AvionManager.getAvion(avionId);
        if (avion != null) {
            prog1.setProgress(avion.getVitesse() / 10.0);
            prog2.setProgress(avion.getPuissanceTir() / 5.0);
            prog3.setProgress(avion.getPointsVie() / 10.0);

            vitesseLabel.setText("Vitesse: " + avion.getVitesse());
            puissanceLabel.setText("Puissance: " + avion.getPuissanceTir());
            vieLabel.setText("Vie: " + avion.getPointsVie());
        }
    }

    @FXML
    private void handleStart() {
        try {
            String nomJoueur = validatePlayerName();
            String niveau = getSelectedNiveau();
            String avion = getSelectedAvion();

            // Enregistrement en base si nouveau joueur
            if ("Nouveau joueur".equals(playerComboBox.getValue())) {
                ConnexionBD.enregistrerJoueur(nomJoueur, niveau, avion, 0);
                playerComboBox.getItems().add(nomJoueur);
            }
            launchTransition(nomJoueur, niveau, avion);

        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }
    private String validatePlayerName() throws Exception {
        if ("Nouveau joueur".equals(playerComboBox.getValue())) {
            String nom = playerNameField.getText().trim();
            if (nom.isEmpty()) throw new Exception("Veuillez entrer un nom");
            if (ConnexionBD.joueurExiste(nom)) throw new Exception("Ce joueur existe déjà");
            return nom;
        }
        return playerComboBox.getValue();
    }
    private String getSelectedNiveau() throws Exception {
        if (niveauGroup.getSelectedToggle() == null) {
            throw new Exception("Veuillez sélectionner un niveau");
        }
        return ((RadioButton)niveauGroup.getSelectedToggle()).getText();
    }

    private String getSelectedAvion() throws Exception {
        if (avionGroup.getSelectedToggle() == null) {
            throw new Exception("Veuillez sélectionner un avion");
        }
        return ((RadioButton)avionGroup.getSelectedToggle()).getText();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void launchTransition(String playerName, String niveau, String avion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/projetjava/Jeu.fxml"));
            Parent root = loader.load();

            JeuController jeuController = loader.getController();
            jeuController.initializeGame(playerName, niveau, avion);

            Stage stage = (Stage) start.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}