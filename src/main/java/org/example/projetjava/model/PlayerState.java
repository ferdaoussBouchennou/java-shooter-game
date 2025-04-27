package org.example.projetjava.model;

import java.io.Serializable;

public class PlayerState implements Serializable {
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;
    private String avionType;
    private int score;
    private int health;
    private boolean shooting;

    public PlayerState(double x, double y, String avionType, int score, int health, boolean shooting) {
        this.x = x;
        this.y = y;
        this.avionType = avionType;
        this.score = score;
        this.health = health;
        this.shooting = shooting;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getAvionType() {
        return avionType;
    }

    public int getScore() {
        return score;
    }

    public int getHealth() {
        return health;
    }

    public boolean isShooting() {
        return shooting;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void setShooting(boolean shooting) {
        this.shooting = shooting;
    }
}