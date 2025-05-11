package org.example.projetjava.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConnexionBD {
    // Pour les connexions depuis la même machine
    // Sur les autres ordinateurs, utilisez l'adresse IP de votre serveur
    private static final String URL = "jdbc:mysql://192.168.1.37:3306/jeu_tir_db";
    private static final String USER = "jeu_user";
    private static final String PASSWORD = "votre_mot_de_passe_jeu";

    public static Connection seconnecter() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trouvé", e);
        }
    }

    public static boolean joueurExiste(String nom) throws SQLException {
        String sql = "SELECT nom FROM joueurs WHERE nom = ?";
        try (Connection conn = seconnecter();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            return stmt.executeQuery().next();
        }
    }

    public static void enregistrerJoueur(String nom, String niveau, String avion, int score) throws SQLException {
        String sql = "INSERT INTO joueurs (nom, niveau, avion_choisi, score) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE niveau = VALUES(niveau), avion_choisi = VALUES(avion_choisi), score = VALUES(score)";

        try (Connection conn = seconnecter();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            stmt.setString(2, niveau);
            stmt.setString(3, avion);
            stmt.setInt(4, score);
            stmt.executeUpdate();
        }
    }

    public static List<String> getJoueursExistants() throws SQLException {
        List<String> joueurs = new ArrayList<>();
        String sql = "SELECT nom FROM joueurs ORDER BY score DESC";
        try (Connection conn = seconnecter();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                joueurs.add(rs.getString("nom"));
            }
        }
        return joueurs;
    }

    public static Joueur getJoueur(String nom) throws SQLException {
        String sql = "SELECT nom, niveau, avion_choisi, score FROM joueurs WHERE nom = ?";
        try (Connection conn = seconnecter();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Joueur(
                        rs.getString("nom"),
                        rs.getString("niveau"),
                        rs.getString("avion_choisi"),
                        rs.getInt("score")
                );
            }
        }
        return null;
    }
}