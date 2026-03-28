package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.GL33.*;

/**
 * Effet feu AB3D2 — simulation exacte du buffer mnu_morescreen.
 *
 * Architecture mémoire originale (8 bitplanes 320x256) : Plans 0,1,2 : points
 * Lissajous (mnu_plot bset dans plans 0-2) Plans 3,4,5 : FEU + TEXTE (même zone
 * ! sourceptrs = plan3-5 + 1 ligne) Plans 6,7 : buffer random / overflow
 *
 * Index couleur d'un pixel = bit0_plan0 | bit1_plan1 | ... | bit7_plan7 Texte
 * (plans 3-5 actifs) -> index = bits 3-5 -> zone firepal (couleur chaude)
 * Points (plans 0-2 actifs) -> index = bits 0-2 -> zone firepal basse Feu
 * (plans 3-5 propagés) -> mélange firepal
 *
 * Le blitter lit plans 3-5 décalés d'une ligne vers le haut et les recopie dans
 * plans 3-5 -> les pixels du TEXTE alimentent le feu par le bas. Chaque lettre
 * brûle par le bas, les flammes montent.
 *
 * Ici on simule avec un buffer unifié [W*H] d'index 0..255, mais on ne
 * travaille qu'avec les bits pertinents : bits 0-2 : composante points (plans
 * 0-2) bits 3-5 : composante feu+texte (plans 3-5)
 *
 * La palette 256 couleurs (buildMenuPalette) gère tout ça correctement.
 */
public class FireEffect {

    private static final Logger log = LoggerFactory.getLogger(FireEffect.class);

    public static final int W = MenuAssets.SCREEN_W;  // 320
    public static final int H = MenuAssets.SCREEN_H;  // 256

    // Buffer principal : index couleur 0..255 par pixel
    // bits 0-2 = composante points (plans 0-2)
    // bits 3-5 = composante feu/texte (plans 3-5)
    final int[] pixels = new int[W * H];

    // Buffer texte statique (plans 3-5) : régénéré quand le texte change
    // bits 3-5 seulement (0x08, 0x10, 0x20, 0x38...)
    private final int[] textLayer = new int[W * H];
    private boolean textDirty = true;

    // Points Lissajous (plans 0-2)
    private final FirePlots plots;

    // LFSR
    private int rnd = 0x54424C21;
    private int frameCount = 0;

    // ByteBuffer RGBA réutilisé
    private final ByteBuffer rgba;
    private int texture = -1;

    // Palette complète 256 couleurs
    private final int[] palette;

    public FireEffect(int[] menuPalette) {
        this.palette = menuPalette;
        this.rgba = ByteBuffer.allocateDirect(W * H * 4).order(ByteOrder.nativeOrder());
        this.plots = new FirePlots();
    }

    public void init() {
        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, W, H,
                0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
        upload();
        log.info("FireEffect initialized");
    }

    /**
     * Fournit le buffer texte depuis l'extérieur (rendu des glyphes dans plans
     * 3-5). Appelé par MenuRenderer quand le texte change.
     *
     * @param textBits buffer W*H, chaque entrée = bits 3-5 actifs selon les
     * plans de la font
     */
    public void setTextLayer(int[] textBits) {
        System.arraycopy(textBits, 0, textLayer, 0, W * H);
        textDirty = true;
    }

    /**
     * Appelé à 60 Hz.
     */
    public void update() {
        frameCount++;
        plots.update();

        // Feu à 30 Hz (skip frames impaires)
        if ((frameCount & 1) == 0) {
            propagateFire();
        }
        for (int i = 0; i < W * H; i++) {

            int fire = pixels[i] & 0x38;
            int text = textLayer[i] & 0xC0;

            pixels[i] = (pixels[i] & 0x07) | fire | text;
        }
        plotPoints();
        upload();
    }

    /**
     * Propagation du feu dans les bits 3-5 (plans 3-5).
     *
     * Reproduit les 3 passes blitter : Source A = pixels[ligne+1], bits 3-5,
     * décalé d'une ligne vers le haut (mnu_speed=1) Source B = bruit LFSR (1
     * bit) Source C = pixels[ligne+1] de la rotation suivante D = f(A,B,C)
     * selon bltcon0
     *
     * Le texte est dans les bits 3-5 -> ses pixels alimentent le feu par le
     * bas.
     */
    private void propagateFire() {

        for (int y = 0; y < H - 1; y++) {

            int row = y * W;
            int rowBelow = (y + 1) * W;

            for (int x = 1; x < W - 1; x++) {

                int below = (pixels[rowBelow + x] >> 3) & 7;
                int belowLeft = (pixels[rowBelow + x - 1] >> 3) & 7;
                int belowRight = (pixels[rowBelow + x + 1] >> 3) & 7;

                int val = (below + belowLeft + belowRight) / 3;

                val -= (rnd() & 1);   // dissipation

                if (val < 0) {
                    val = 0;
                }
                if (val > 7) {
                    val = 7;
                }

                pixels[row + x]
                        = (pixels[row + x] & ~0x38)
                        | (val << 3);
            }
        }

        // base du feu (ligne du bas)
        int base = (H - 1) * W;

        for (int x = 0; x < W; x++) {

            int heat = rnd() & 7;

            pixels[base + x]
                    = (pixels[base + x] & ~0x38)
                    | (heat << 3);
        }

        // Réapplique le texte par-dessus (les lettres restent visibles)
        for (int i = 0; i < W * H; i++) {
            if ((textLayer[i] & 0x38) != 0) {
                // Pixel de texte : force les bits 3-5
                pixels[i] = (pixels[i] & 0x07) | (textLayer[i] & 0x38);
            }
        }
    }

    /**
     * Trace les 50 points Lissajous dans les bits 0-2 (plans 0-2). Index
     * résultant : bits 0-2 = 7 (111) -> dans la palette firepal zone basse.
     */
    private void plotPoints() {
        // Efface les bits 0-2 de tous les pixels d'abord
        for (int i = 0; i < W * H; i++) {
            pixels[i] &= ~0x07;
        }

        int[] pos = plots.getPositions();
        for (int i = 0; i < FirePlots.NUM_POINTS; i++) {
            int px = pos[i * 2];
            int py = pos[i * 2 + 1];
            // bset dans plans 0,1,2 -> bits 0,1,2 = 1 -> index bits 0-2 = 7
            setPoint(px, py, 7);
            setPoint(px - 1, py, 6);
            setPoint(px + 1, py, 6);
            setPoint(px, py - 1, 5);
        }
    }

    private void setPoint(int x, int y, int val) {
        if (x < 0 || x >= W || y < 0 || y >= H) {
            return;
        }
        int idx = y * W + x;
        pixels[idx] = (pixels[idx] & ~0x07) | (val & 0x07);
    }

    private void upload() {
        rgba.clear();
        for (int i = 0; i < W * H; i++) {
            int idx = pixels[i] & 0xFF;
            int argb = palette[idx];
            // Index 0 (fond) = transparent pour blend additif
            int alpha = (idx == 0) ? 0 : (argb >>> 24);
            rgba.put((byte) ((argb >> 16) & 0xFF));
            rgba.put((byte) ((argb >> 8) & 0xFF));
            rgba.put((byte) (argb & 0xFF));
            rgba.put((byte) alpha);
        }
        rgba.flip();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, W, H, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private int rnd() {
        int b = ((rnd) ^ (rnd >> 2) ^ (rnd >> 3) ^ (rnd >> 5)) & 1;
        rnd = (rnd >>> 1) | (b << 31);
        return rnd & 0x7FFF;
    }

    public void destroy() {
        if (texture >= 0) {
            glDeleteTextures(texture);
            texture = -1;
        }
    }

    public int getTexture() {
        return texture;
    }
}
