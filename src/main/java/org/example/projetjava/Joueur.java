package org.example.projetjava;

public class Joueur {
    private final String nom;
    private final String niveau;
    private final String avion;

    public Joueur(String nom, String niveau, String avion) {
        this.nom = nom;
        this.niveau = niveau;
        this.avion = avion;
    }

    // Getters
    public String getNom() { return nom; }
    public String getNiveau() { return niveau; }
    public String getAvion() { return avion; }
}
