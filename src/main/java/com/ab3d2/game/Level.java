package com.ab3d2.game;

public class Level {

    private final String name;

    public Level(String name) {
        this.name = name;
    }

    public void init() {
        // plus tard : chargement du niveau (fichier, BSP, etc.)
    }

    public void update(double dt) {
        // plus tard : scripts, triggers, etc.
    }

    public void destroy() {
        // libérer les ressources si besoin
    }

    public String getName() {
        return name;
    }
}
