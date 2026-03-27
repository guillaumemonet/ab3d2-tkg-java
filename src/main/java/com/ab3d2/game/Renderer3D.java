package com.ab3d2.game;

import com.ab3d2.render.Renderer2D;

public class Renderer3D {

    public void render(Level level, Player player, Renderer2D r) {
        // Stub : pour l’instant, on remplit juste le fond
        // plus tard : vrai moteur 3D
        // (tu peux laisser vide si ton Renderer2D clear déjà le FBO)
    }

    public void destroy() {
        // plus tard : libérer les ressources GPU si besoin
    }
}
