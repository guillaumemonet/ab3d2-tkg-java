package com.ab3d2.menu;

import com.ab3d2.render.Renderer2D;

/**
 * Curseur animé du menu AB3D2.
 *
 * Reproduit mnu_cursanim et mnu_animcursor de l'ASM : mnu_cursanim: dc.b
 * 130,129,128,127,126,125,124,123,8 -> séquence d'index de glyphes pour
 * l'animation du curseur flèche -> le dernier octet (8 = 0x08) est un code
 * spécial "retour"
 *
 * Les index 123-130 correspondent aux chars ASCII 123-130 (flèche animée dans
 * la police originale). Ici on simule avec une flèche ">" qui pulse.
 */
public class MenuCursor {

    // Séquence d'animation originale (index glyphe, pas ASCII)
    // mnu_cursanim: 130,129,128,127,126,125,124,123,8 (8=loop)
    // Ces glyphes sont dans la font à ces positions
    private static final int[] CURSOR_ANIM = {130, 129, 128, 127, 126, 125, 124, 123};

    private int animFrame = 0;
    private float animTimer = 0;
    private static final float ANIM_SPEED = 0.08f; // secondes par frame

    public void update(double deltaTime) {
        animTimer += deltaTime;
        if (animTimer >= ANIM_SPEED) {
            animTimer -= ANIM_SPEED;
            animFrame = (animFrame + 1) % CURSOR_ANIM.length;
        }
    }

    /**
     * Dessine le curseur à la position donnée. Utilise la font AB3D2 si
     * disponible, sinon ">" simple.
     *
     * @param r renderer
     * @param font font AB3D2 (peut être null)
     * @param x, y position en pixels
     */
    public void render(Renderer2D r, Ab3dFont font, float x, float y) {
        if (font == null) {
            return;
        }

        // Glyphe courant de l'animation
        // Les glyphes 123-130 sont dans la font originale
        // On utilise '>' (ASCII 62) comme fallback si hors range
        int glyphIdx = CURSOR_ANIM[animFrame];
        char c;
        if (glyphIdx >= 32 && glyphIdx < 32 + 220) {
            c = (char) glyphIdx;
        } else {
            c = '>';
        }

        font.drawString(r, String.valueOf(c), x, y);
    }

    public void reset() {
        animFrame = 0;
        animTimer = 0;
    }
}
