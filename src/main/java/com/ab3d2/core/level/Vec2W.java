package com.ab3d2.core.level;

/**
 * Point 2D en virgule fixe 16 bits.
 * Correspond exactement à {@code Vec2W} dans math25d.h :
 * <pre>
 * typedef struct {
 *     WORD v_X;  // 0, 2
 *     WORD v_Z;  // 2, 2
 * } Vec2W;        // total : 4 bytes, big-endian
 * </pre>
 *
 * Utilisé pour les points du plan XZ du niveau (pas de Y — la hauteur est dans Zone).
 * Les valeurs sont signées 16 bits, espace monde en virgule fixe.
 */
public record Vec2W(short x, short z) {

    /** Crée un Vec2W depuis des ints (tronqués à 16 bits). */
    public static Vec2W of(int x, int z) {
        return new Vec2W((short) x, (short) z);
    }

    /** Valeur X en int signé (pour les calculs sans masquage). */
    public int xi() { return x; }

    /** Valeur Z en int signé. */
    public int zi() { return z; }

    @Override
    public String toString() {
        return "(" + x + ", " + z + ")";
    }
}
