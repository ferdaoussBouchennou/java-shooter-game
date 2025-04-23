package org.example.projetjava.model;

public class Avion {
    private final String nom;
    private final int vitesse;
    private final int puissanceTir;
    private final int pointsVie;
    private final String imagePath;

    // Constructeur
    public Avion(String nom, int vitesse, int puissanceTir, int pointsVie, String imagePath) {
        this.nom = nom;
        this.vitesse = vitesse;
        this.puissanceTir = puissanceTir;
        this.pointsVie = pointsVie;
        this.imagePath = imagePath;
    }
    public String getNom() {
        return nom;
    }

    public int getVitesse() {
        return vitesse;
    }

    public int getPuissanceTir() {
        return puissanceTir;
    }

    public int getPointsVie() {
        return pointsVie;
    }

    public String getImagePath() {
        return imagePath;
    }

}
