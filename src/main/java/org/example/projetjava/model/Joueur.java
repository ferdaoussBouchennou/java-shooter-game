package org.example.projetjava.model;

public class Joueur {
    private final String nom;
    private final String niveau;
    private final String avion;
    private final int score;

    public Joueur(String nom, String niveau, String avion, int score) {
        this.nom = nom;
        this.niveau = niveau;
        this.avion = avion;
        this.score = score;
    }

    public String getNom() {
        return nom;
    }

    public String getNiveau() {
        return niveau;
    }

    public String getAvion() {
        return avion;
    }

    public int getScore() { return score; }
}