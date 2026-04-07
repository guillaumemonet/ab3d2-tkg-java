package com.ab3d2.render;

import com.ab3d2.core.math.Tables;

/**
 * Transforme des coordonnées monde en espace caméra et projette sur l'écran.
 *
 * <h2>Conventions AB3D2</h2>
 * <ul>
 *   <li>Axe X = Est, Axe Z = Nord (+Z = direction de départ)</li>
 *   <li>angle=0 → face +Z (Nord), angle=1024 → face +X (Est)</li>
 *   <li>Hauteurs : "smaller = higher" — valeurs négatives = plus haut</li>
 *   <li>PLR_STAND_HEIGHT = 12*1024 → œil du joueur à floorHeight()-48</li>
 * </ul>
 *
 * <h2>Projection (depuis hireswall.s)</h2>
 * <pre>
 *   cam_x = (dx * cos - dz * sin)
 *   cam_z = (dx * sin + dz * cos)      positif = devant la caméra
 *
 *   screenX = cam_x / cam_z * FOCAL + centreX
 *   screenY = (worldH - eyeH) / cam_z * FOCAL + centreY
 * </pre>
 */
public class Camera {

    // ── Constantes écran (SMALL_WIDTH=192, SMALL_HEIGHT=160) ─────────────────
    public static final int SCREEN_W  = 192;
    public static final int SCREEN_H  = 160;
    public static final int CENTRE_X  = SCREEN_W / 2;   // 96
    public static final int CENTRE_Y  = SCREEN_H / 2;   // 80

    /**
     * Distance focale — détermine le FOV.
     * FOCAL=96 donne un FOV horizontal de 90°.
     * (même valeur pour X et Y → pixels carrés)
     */
    public static final float FOCAL   = 96.0f;

    /**
     * Hauteur des yeux du joueur AU-DESSUS du sol (en "height units" = après >>8).
     * PLR_STAND_HEIGHT / 256 = 12*1024 / 256 = 48.
     */
    public static final float PLR_EYE_ABOVE_FLOOR = 48.0f;

    /**
     * Distance minimum de rendu (near plane).
     * Les points à cam_z <= NEAR_Z sont derrière ou trop proches.
     */
    public static final float NEAR_Z  = 0.5f;

    // ── Etat caméra ───────────────────────────────────────────────────────────

    public float x;        // position monde X (unités entières)
    public float z;        // position monde Z
    public float eyeH;     // hauteur des yeux (height units, typiquement floorH - 48)
    public int   angle;    // angle 0..4095

    // Valeurs dérivées (pré-calculées à chaque setAngle/update)
    private float sinVal;
    private float cosVal;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    public Camera(float x, float z, float eyeH, int angle) {
        this.x    = x;
        this.z    = z;
        this.eyeH = eyeH;
        setAngle(angle);
    }

    public void setAngle(int angle) {
        this.angle  = angle & 4095;
        this.sinVal = Tables.sinf(this.angle);
        this.cosVal = Tables.cosf(this.angle);
    }

    public void update() {
        this.sinVal = Tables.sinf(this.angle);
        this.cosVal = Tables.cosf(this.angle);
    }

    // ── Transform monde → espace caméra ──────────────────────────────────────

    /** Composante X caméra d'un point monde (+ = droite de la caméra). */
    public float camX(float worldX, float worldZ) {
        return (worldX - x) * cosVal - (worldZ - z) * sinVal;
    }

    /** Composante Z caméra d'un point monde (+ = devant la caméra). */
    public float camZ(float worldX, float worldZ) {
        return (worldX - x) * sinVal + (worldZ - z) * cosVal;
    }

    /** Vrai si un point monde est devant la caméra (cam_z > NEAR_Z). */
    public boolean isInFront(float worldX, float worldZ) {
        return camZ(worldX, worldZ) > NEAR_Z;
    }

    // ── Projection espace caméra → écran ─────────────────────────────────────

    /**
     * Projette la coordonnée X caméra sur l'écran.
     * @param cX  camera-space X
     * @param cZ  camera-space Z (depth)
     */
    public static float projectX(float cX, float cZ) {
        if (cZ <= 0) return Float.NaN;
        return cX / cZ * FOCAL + CENTRE_X;
    }

    /**
     * Projette une hauteur monde sur Y écran.
     * Renvoie un screenY < centreY si la hauteur est au-dessus de l'œil.
     *
     * @param worldH  hauteur monde (height units, "smaller = higher")
     * @param eyeH    hauteur des yeux (height units)
     * @param cZ      camera-space Z (depth)
     */
    public static float projectY(float worldH, float eyeH, float cZ) {
        if (cZ <= 0) return Float.NaN;
        // (worldH - eyeH) < 0  quand worldH est AU-DESSUS de eyeH
        // → screenY < centreY (au-dessus du centre) ✓
        return (worldH - eyeH) / cZ * FOCAL + CENTRE_Y;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /** Vrai si un screenX est dans les limites de l'écran. */
    public static boolean onScreen(float screenX) {
        return screenX >= 0 && screenX < SCREEN_W;
    }

    /** Vrai si un screenY est dans les limites de l'écran. */
    public static boolean onScreenY(float screenY) {
        return screenY >= 0 && screenY < SCREEN_H;
    }

    /** Clamp un screenX à [0..SCREEN_W-1]. */
    public static int clampX(float sx) {
        return Math.max(0, Math.min(SCREEN_W - 1, (int) sx));
    }

    /** Clamp un screenY à [0..SCREEN_H-1]. */
    public static int clampY(float sy) {
        return Math.max(0, Math.min(SCREEN_H - 1, (int) sy));
    }

    @Override
    public String toString() {
        return String.format("Camera{x=%.1f, z=%.1f, eyeH=%.1f, angle=%d, sin=%.3f, cos=%.3f}",
            x, z, eyeH, angle, sinVal, cosVal);
    }
}
