package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

/**
 * Simulation de l'effet feu du menu AB3D2.
 *
 * L'original utilise le blitter Amiga avec 3 passes pour faire remonter les
 * pixels vers le haut avec du bruit. On reproduit ça en Java pur sur un
 * framebuffer 320x256 indexé (1 byte/pixel = index palette).
 *
 * Architecture originale (3 bitplanes dans mnu_morescreen) : - 3 bitplanes de
 * 320x256 = index couleur 3 bits (0..7 -> firepal) - Chaque frame : les pixels
 * montent d'une ligne (mnu_speed=1) - Bruit aléatoire (LFSR) mélangé aux pixels
 * - 3 "source planes" rotatives pour variation temporelle
 *
 * On simplifie : buffer indexé 1 octet/pixel, chaque pixel = index 0..31 (les 5
 * bits bas de la palette : bits 0-4 -> backpal+firepal)
 */
public class FireEffect {

    private static final Logger log = LoggerFactory.getLogger(FireEffect.class);

    private static final int W = MenuAssets.SCREEN_W;   // 320
    private static final int H = MenuAssets.SCREEN_H;   // 256
    private static final int FIRE_H = H;                     // hauteur feu = écran entier
    private static final int BUF_SIZE = W * FIRE_H;

    // Buffer indexé : valeur = index feu (0..7 mapé vers firepal)
    private final int[] fireBuffer = new int[BUF_SIZE];

    // Buffer RGBA pour upload GPU
    private final int[] rgbaBuffer = new int[BUF_SIZE];

    // Texture OpenGL
    private int texture = -1;

    // LFSR state (reproduit mnu_rnd + getrnd du jeu original)
    private int rnd = 0x544C2100; // seed 'TBL!'
    private int rndState = 12345;

    // Compteur frame (mnu_count dans l'original)
    private int count = 0;

    // Palette feu -> ARGB (32 entrées : 4 back + 28 fire)
    private int[] firePalARGB;

    public FireEffect(int[] backpal, int[] firepal) {
        buildFirePalette(backpal, firepal);
    }

    /**
     * Construit la sous-palette pour les 32 premières entrées (bits 0-4). Index
     * 0..3 -> backpal (bits 0-1) Index 4..31 -> firepal mélangé (bits 2-4 +
     * bits 0-1)
     */
    private void buildFirePalette(int[] backpal, int[] firepal) {
        firePalARGB = new int[32];
        for (int c = 0; c < 32; c++) {
            if ((c & 0x1C) != 0) {
                int fi1 = (c & 0x1C) >> 2;
                int fi2 = c & 0x03;
                int c1 = firepal[fi1];
                int c2 = firepal[fi2];
                int r = Math.min(255, ((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF));
                int g = Math.min(255, (((c1 >> 8) & 0xFF) * 3) / 4 + ((c2 >> 8) & 0xFF));
                int b = Math.min(255, (c1 & 0xFF) + (c2 & 0xFF));
                firePalARGB[c] = 0xFF000000 | (r << 16) | (g << 8) | b;
            } else {
                firePalARGB[c] = backpal[c & 3];
            }
        }
    }

    public void init() {
        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, W, FIRE_H, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Initialise le bas de l'écran avec des valeurs max (source du feu)
        seedFireBase();
        log.info("FireEffect initialized");
    }

    /**
     * Met une ligne de feu en bas (source chaude). Dans l'original, la source
     * est dans mnu_sourceptrs (bitplanes rotatifs). On simule avec une ligne de
     * valeurs élevées + variation aléatoire.
     */
    private void seedFireBase() {
        // Lignes du bas = source chaude (valeurs hautes = couleurs chaudes)
        for (int x = 0; x < W; x++) {
            // Ligne du bas : intensité max (index 28..31 = couleur la plus chaude)
            fireBuffer[(H - 1) * W + x] = 28 + (rndNext() & 3);
            fireBuffer[(H - 2) * W + x] = 24 + (rndNext() & 7);
        }
    }

    /**
     * Met à jour l'effet feu - appelé chaque frame logique (60Hz). Reproduit le
     * comportement du blitter Amiga (mnu_dofire + mnu_pass1/2/3).
     *
     * L'effet : chaque pixel monte d'une ligne avec atténuation, du bruit est
     * ajouté pour la variation latérale.
     */
    public void update() {
        count++;

        // L'original ne fait rien sur les frames impaires (main_counter & 1)
        // On garde à 60fps donc on skip une frame sur deux
        if ((count & 1) != 0) {
            return;
        }

        // Re-seed le bas (source de chaleur permanente)
        for (int x = 0; x < W; x++) {
            int noise = rndNext() & 7;
            // Intensité de base selon x pour variation (comme les sourceptrs rotatifs)
            int base = 20 + noise;
            fireBuffer[(H - 1) * W + x] = base;
            if (x > 0 && x < W - 1) {
                fireBuffer[(H - 2) * W + x] = Math.max(0, base - 4 + (rndNext() & 7));
            }
        }

        // Propagation : chaque pixel = moyenne de ses voisins du bas, avec atténuation
        // Reproduit les 3 passes blitter (D = A + B*C avec bruit)
        for (int y = 0; y < H - 2; y++) {
            for (int x = 0; x < W; x++) {
                // Pixel source = y+1 (monte d'une ligne)
                int src = fireBuffer[(y + 1) * W + x];

                // Diffusion latérale légère (reproduit le bruit blitter)
                int noise = (rndNext() & 3);

                // Atténuation : selon le cycle (3 passes : +B, +B+A, +A)
                int attenuation;
                switch (count & 3) {
                    case 0 ->
                        attenuation = 1; // pass 1 : D=A+BC (légère)
                    case 1 ->
                        attenuation = 0; // pass 2 : D=A+BC avec subtract=-2 (plus fort)
                    default ->
                        attenuation = 1; // pass 3 : normal
                }

                int val = src - attenuation;
                if (noise == 0) {
                    val--; // bruit supplémentaire occasionnel
                }
                // Diffusion latérale : emprunte légèrement aux voisins
                if (x > 0 && x < W - 1) {
                    int left = fireBuffer[(y + 1) * W + x - 1];
                    int right = fireBuffer[(y + 1) * W + x + 1];
                    val = (val * 2 + left + right) >> 2; // moyenne pondérée
                }

                fireBuffer[y * W + x] = Math.max(0, Math.min(31, val));
            }
        }

        uploadTexture();
    }

    private void uploadTexture() {
        // Convertit buffer indexé -> RGBA
        ByteBuffer buf = ByteBuffer.allocateDirect(W * FIRE_H * 4);
        for (int i = 0; i < BUF_SIZE; i++) {
            int argb = firePalARGB[fireBuffer[i]];
            buf.put((byte) ((argb >> 16) & 0xFF));
            buf.put((byte) ((argb >> 8) & 0xFF));
            buf.put((byte) (argb & 0xFF));
            buf.put((byte) ((argb >> 24) & 0xFF));
        }
        buf.flip();

        glBindTexture(GL_TEXTURE_2D, texture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, W, FIRE_H, GL_RGBA, GL_UNSIGNED_BYTE, buf);
    }

    // ── LFSR / générateur aléatoire ───────────────────────────────────────────
    // Reproduit getrnd + mnu_initrnd de l'ASM Amiga
    // L'original est un LFSR 32 bits avec polynôme basé sur bits 0,2,3,5
    private int rndNext() {
        // LFSR simple équivalent au mnu_rnd Amiga
        rndState ^= (rndState << 13);
        rndState ^= (rndState >> 17);
        rndState ^= (rndState << 5);
        return rndState & 0x7FFF;
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
