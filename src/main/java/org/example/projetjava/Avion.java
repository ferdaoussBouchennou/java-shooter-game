package org.example.projetjava;

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

    // Getters
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

    // Méthode pour afficher les caractéristiques (utile pour le débogage)
    @Override
    public String toString() {
        return String.format(
                "Avion [nom=%s, vitesse=%d, puissanceTir=%d, pointsVie=%d, imagePath=%s]",
                nom, vitesse, puissanceTir, pointsVie, imagePath
        );
    }

    // Méthode pour obtenir les caractéristiques sous forme normalisée (0-1)
    public double getVitesseNormalisee() {
        return vitesse / 10.0;
    }

    public double getPuissanceNormalisee() {
        return puissanceTir / 5.0;
    }

    public double getVieNormalisee() {
        return pointsVie / 100.0;
    }
}
