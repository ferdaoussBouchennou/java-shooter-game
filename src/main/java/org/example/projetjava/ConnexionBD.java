

package org.example.projetjava;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConnexionBD {
    private static final String URL = "jdbc:mysql://localhost:3306/jeu_tir_db";
    private static final String USER = "root"; // ou "root"
    private static final String PASSWORD = ""; // ou "" si pas de mot de passe

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

    public static void enregistrerJoueur(String nom, String niveau, String avion) throws SQLException {
        String sql = "INSERT INTO joueurs (nom, niveau, avion_choisi) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE niveau = VALUES(niveau), avion_choisi = VALUES(avion_choisi)";

        try (Connection conn = seconnecter();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            stmt.setString(2, niveau);
            stmt.setString(3, avion);
            stmt.executeUpdate();
        }
    }

    public static Avion getAvion(String nomAvion) throws SQLException {
        String sql = "SELECT * FROM avions WHERE nom = ?";
        try (Connection conn = seconnecter();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomAvion);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Avion(
                        rs.getString("nom"),
                        rs.getInt("vitesse"),
                        rs.getInt("puissance_tir"),
                        rs.getInt("points_vie"),
                        rs.getString("image_path")
                );
            }
        }
        return null;
    }
    public static List<String> getJoueursExistants() throws SQLException {
        List<String> joueurs = new ArrayList<>();
        String sql = "SELECT nom FROM joueurs";

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
        String sql = "SELECT * FROM joueurs WHERE nom = ?";
        try (Connection conn = seconnecter();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nom);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Joueur(
                        rs.getString("nom"),
                        rs.getString("niveau"),
                        rs.getString("avion_choisi")
                );
            }
        }
        return null;
    }
}
