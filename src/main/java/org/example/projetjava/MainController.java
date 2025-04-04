package org.example.projetjava;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

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

    @FXML
    public void initialize() {
        loadExistingPlayers(); // Charger les joueurs existants au démarrage
        setupPlayerSelection();
        setupNiveauSelection();
        setupAvionSelection();
        loadAvionImages();
        setupButtonActions();
    }
    private void loadExistingPlayers() {
        try {
            // Charger depuis la base de données
            playerComboBox.getItems().clear();
            playerComboBox.getItems().add("Nouveau joueur");
            playerComboBox.getItems().add("DefaultPlayer");

            // Ajouter les joueurs existants depuis la BD
            ConnexionBD.getJoueursExistants().forEach(joueur -> {
                if (!playerComboBox.getItems().contains(joueur)) {
                    playerComboBox.getItems().add(joueur);
                }
            });

        } catch (SQLException e) {
            showAlert("Erreur BD", "Impossible de charger les joueurs");
        }
    }

    private void setupPlayerSelection() {
        playerComboBox.setOnAction(event -> {
            String selected = playerComboBox.getValue();
            boolean isNewPlayer = "Nouveau joueur".equals(selected);

            playerNameField.setDisable(!isNewPlayer);

            if (!isNewPlayer) {
                playerNameField.clear();
                // Charger les préférences du joueur existant
                loadPlayerPreferences(selected);
            }
        });
    }
    private void loadPlayerPreferences(String playerName) {
        try {
            Joueur joueur = ConnexionBD.getJoueur(playerName);
            if (joueur != null) {
                // Mettre à jour la sélection du niveau
                niveauGroup.getToggles().forEach(toggle -> {
                    if (((RadioButton)toggle).getText().equals(joueur.getNiveau())) {
                        niveauGroup.selectToggle(toggle);
                    }
                });

                // Mettre à jour la sélection de l'avion
                avionGroup.getToggles().forEach(toggle -> {
                    if (((RadioButton)toggle).getText().equals(joueur.getAvion())) {
                        avionGroup.selectToggle(toggle);
                    }
                });
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

        avionGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateAvionStats(((RadioButton)newVal).getId());
            }
        });
    }

    private void loadAvionImages() {
        // Charger les images correspondantes
        image1.setImage(loadImage("playerShip1_blue.png"));
        image2.setImage(loadImage("playerShip2_orange.png"));
        image3.setImage(loadImage("playerShip3_green.png"));
        image4.setImage(loadImage("enemyRed3.png"));

        // Associer chaque RadioButton à un ID d'avion
        button1.setId("MIC-51S");
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
        start.setOnAction(event -> handleStart());
        quit.setOnAction(event -> System.exit(0));
    }

    private void updateAvionStats(String avionId) {
        try {
            Avion avion = ConnexionBD.getAvion(avionId);
            if (avion != null) {
                prog1.setProgress(avion.getVitesse() / 10.0);
                prog2.setProgress(avion.getPuissanceTir() / 5.0);
                prog3.setProgress(avion.getPointsVie() / 100.0);

                vitesseLabel.setText("Vitesse: " + avion.getVitesse());
                puissanceLabel.setText("Puissance: " + avion.getPuissanceTir());
                vieLabel.setText("Vie: " + avion.getPointsVie());
            }
        } catch (SQLException e) {
            showAlert("Erreur BD", "Impossible de charger les stats de l'avion");
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
                ConnexionBD.enregistrerJoueur(nomJoueur, niveau, avion);
                playerComboBox.getItems().add(nomJoueur);
            }

            // Lancer la transition au lieu du jeu directement
            launchTransition(nomJoueur, niveau, avion);

        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private boolean confirmUpdatePlayer(String playerName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Joueur existant");
        alert.setContentText("Voulez-vous mettre à jour les préférences pour " + playerName + "?");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
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
            // 1. Charger le FXML de transition
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/projetjava/Jeu.fxml"));
            Parent root = loader.load();

            // 2. Initialiser le contrôleur de transition
            JeuController transitionController = loader.getController();
            transitionController.showTransition(playerName);

            // 3. Afficher la scène de transition
            Stage stage = (Stage) start.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.show();

        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger l'écran de transition");
            e.printStackTrace();
        }
    }
}