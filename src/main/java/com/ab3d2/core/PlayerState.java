package com.ab3d2.core;

import com.ab3d2.core.math.Tables;

/**
 * État du joueur en espace monde AB3D2.
 *
 * <h2>Coordonnées</h2>
 * Toutes les positions sont en virgule fixe 32 bits, format {@code 24.8}
 * (les 8 bits de poids faible = fraction, bits 8-31 = entier monde).
 * Correspond à {@code PlrT_XOff_l / PlrT_ZOff_l} dans defs.i.
 *
 * <h2>Angle</h2>
 * {@code angle} ∈ [0..4095] = un cycle complet (4096 unités = 360°).
 * Correspond à {@code PlrT_AngPos_w}.
 * 0 = regardant vers +Z (Nord), 1024 = regardant vers +X (Est).
 *
 * <h2>Hauteur</h2>
 * {@code yOff} = hauteur des yeux du joueur.
 * {@code PLR_STAND_HEIGHT = 12*1024 = 12288} en virgule fixe.
 */
public class PlayerState {

    // ── Constantes (depuis defs.i / hires.s) ─────────────────────────────────

    public static final int PLR_STAND_HEIGHT  = 12 * 1024;  // 12288
    public static final int PLR_CROUCH_HEIGHT = 8  * 1024;  // 8192
    public static final int ANGLE_MAX         = 8192;        // un cycle complet (confirme : bigsine = 8192 entrees)
    public static final int ANGLE_QUARTER     = ANGLE_MAX / 4; // 2048 = 90°
    public static final int INITIAL_ENERGY    = 191;
    public static final float WORLD_SPEED     = 128.0f;      // pixels/sec en virgule fixe

    // ── Position et orientation ───────────────────────────────────────────────

    /** Position X en virgule fixe 24.8 (bits 8+ = entier monde). */
    public int xOff;
    /** Position Z en virgule fixe 24.8. */
    public int zOff;
    /** Hauteur des yeux en virgule fixe. */
    public int yOff = PLR_STAND_HEIGHT;

    /** Angle de vue en [0..4095]. 0=Nord (+Z), 1024=Est (+X). */
    public int angle;

    /** ID de la zone courante. */
    public short currentZoneId;

    // ── Vie et statut ─────────────────────────────────────────────────────────

    public int energy = INITIAL_ENERGY;
    public boolean ducked  = false;
    public boolean dead    = false;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public PlayerState() {}

    public PlayerState(int xWorld, int zWorld, int angle, short zoneId) {
        this.xOff          = xWorld << 8;
        this.zOff          = zWorld << 8;
        this.angle         = angle & (ANGLE_MAX - 1);
        this.currentZoneId = zoneId;
    }

    // ── Accesseurs monde (entiers) ────────────────────────────────────────────

    /** Coordonnée X en unités monde (partie entière). */
    public int worldX() { return xOff >> 8; }
    /** Coordonnée Z en unités monde. */
    public int worldZ() { return zOff >> 8; }

    // ── Mouvement ─────────────────────────────────────────────────────────────

    /**
     * Avance/recule selon l'angle courant.
     * @param speed vitesse en unités monde (peut être négatif = reculer)
     */
    public void moveForward(float speed) {
        if (!Tables.isInitialized()) return;
        float cos = Tables.cosf(angle);
        float sin = Tables.sinf(angle);
        // Dans AB3D2 : avancer = +Z cosinus, +X sinus (angle=0 → vers +Z)
        xOff += (int)(sin * speed * 256);
        zOff += (int)(cos * speed * 256);
    }

    /**
     * Strafe gauche/droite.
     * @param speed vitesse (positif = droite)
     */
    public void moveStrafe(float speed) {
        if (!Tables.isInitialized()) return;
        float cos = Tables.cosf(angle);
        float sin = Tables.sinf(angle);
        xOff += (int)( cos * speed * 256);
        zOff += (int)(-sin * speed * 256);
    }

    /**
     * Tourne à gauche (delta négatif) ou à droite.
     * @param delta delta d'angle en unités AB3D2 (1 = ~0.088°)
     */
    public void rotate(int delta) {
        angle = (angle + delta + ANGLE_MAX) & (ANGLE_MAX - 1);
    }

    // ── Valeurs dérivées ──────────────────────────────────────────────────────

    /** Cosinus de l'angle courant (signé 16 bits depuis la table). */
    public short cosVal() {
        return Tables.isInitialized() ? Tables.cosw(angle) : 32767;
    }

    /** Sinus de l'angle courant. */
    public short sinVal() {
        return Tables.isInitialized() ? Tables.sinw(angle) : 0;
    }

    @Override
    public String toString() {
        return String.format("Player{x=%d, z=%d, angle=%d, zone=%d, energy=%d}",
            worldX(), worldZ(), angle, currentZoneId, energy);
    }
}
