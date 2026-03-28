package com.ab3d2.menu;

/**
 * Curseur animé (mnu_cursanim: 130,129,128,127,126,125,124,123). Expose le
 * glyphe courant pour rendu direct.
 */
public class MenuCursor {

    // mnu_cursanim: dc.b 130,129,128,127,126,125,124,123,8
    private static final int[] ANIM = {130, 129, 128, 127, 126, 125, 124, 123};
    private static final double SPEED = 1.0 / 25.0;

    private int frame = 0;
    private double timer = 0;

    public void update(double dt) {
        timer += dt;
        if (timer >= SPEED) {
            timer -= SPEED;
            frame = (frame + 1) % ANIM.length;
        }
    }

    public void reset() {
        frame = 0;
        timer = 0;
    }

    /**
     * Glyphe ASCII courant (123-130).
     */
    public int getCurrentGlyph() {
        return ANIM[frame];
    }

    /**
     * Méthode statique utilisable depuis MainMenuScreen sans instance.
     */
    public static int getCurrentGlyph(MenuCursor c) {
        return c.getCurrentGlyph();
    }
}
