package com.ab3d2.core.level;

/**
 * Arête (edge) du monde 3D, partagée entre deux zones adjacentes.
 * Correspond exactement à {@code ZEdge} dans zone.h :
 * <pre>
 * typedef struct {
 *     Vec2W e_Pos;         //  0, 4  — point de départ de l'arête (x, z)
 *     Vec2W e_Len;         //  4, 4  — vecteur longueur (dx, dz)
 *     WORD  e_JoinZoneID;  //  8, 2  — zone adjacente (-1 = mur solide)
 *     WORD  e_Word_5;      // 10, 2  — inconnu
 *     BYTE  e_Byte_12;     // 12, 1  — inconnu
 *     BYTE  e_Byte_13;     // 13, 1  — inconnu
 *     UWORD e_Flags;       // 14, 2
 * } ZEdge;                  // total : 16 bytes, big-endian
 * </pre>
 *
 * <h2>Géométrie</h2>
 * L'arête part de {@code pos} et se termine à {@code pos + len}.
 * Le test "de quel côté est un point P" est :
 * <pre>
 *   side = len.x * (P.z - pos.z) - len.z * (P.x - pos.x)
 * </pre>
 * Voir {@code Zone_SideOfEdge()} dans zone_inline.h.
 *
 * <h2>Flags connus</h2>
 * (non documentés exhaustivement dans les sources)
 */
public record ZEdge(
        Vec2W pos,          // point de départ
        Vec2W len,          // vecteur longueur (dx, dz)
        short joinZoneId,   // -1 = mur solide, ≥0 = ID de la zone adjacente
        short word5,        // usage inconnu
        byte  byte12,       // usage inconnu
        byte  byte13,       // usage inconnu
        short flags
) {
    /** Taille binaire en bytes (constante). */
    public static final int BINARY_SIZE = 16;

    /** Mur solide : pas de zone adjacente. */
    public static final short SOLID_WALL = -1;

    /** Vrai si cette arête ouvre sur une autre zone (portail). */
    public boolean isPortal() { return joinZoneId >= 0; }

    /**
     * Détermine le côté d'un point par rapport à l'arête.
     * Valeur positive = côté frontal, négative = côté arrière, 0 = sur la ligne.
     */
    public int sideOf(int px, int pz) {
        return (int) len.xi() * (pz - (int) pos.zi())
             - (int) len.zi() * (px - (int) pos.xi());
    }

    @Override
    public String toString() {
        return String.format("ZEdge{pos=%s, len=%s, join=%d, flags=0x%04X}",
            pos, len, joinZoneId, flags & 0xFFFF);
    }
}
