package com.ab3d2.core.level;

import java.util.Arrays;

/**
 * Données complètes d'une zone du niveau.
 *
 * <h2>Structure binaire source (zone.h)</h2>
 * <pre>
 * typedef struct {
 *     WORD  z_ZoneID;          //  0, 2
 *     LONG  z_Floor;           //  2, 4   (2-byte aligned !)
 *     LONG  z_Roof;            //  6, 4
 *     LONG  z_UpperFloor;      // 10, 4
 *     LONG  z_UpperRoof;       // 14, 4
 *     LONG  z_Water;           // 18, 4
 *     WORD  z_Brightness;      // 22, 2
 *     WORD  z_UpperBrightness; // 24, 2
 *     WORD  z_ControlPoint;    // 26, 2
 *     WORD  z_BackSFXMask;     // 28, 2
 *     WORD  z_Unused;          // 30, 2   ← ZoneT_Unused_w (edge PVS tag)
 *     WORD  z_EdgeListOffset;  // 32, 2   (offset NÉGATIF vers la liste d'edge IDs)
 *     WORD  z_Points;          // 34, 2   (offset NÉGATIF vers la liste de point IDs)
 *     UBYTE z_DrawBackdrop;    // 36, 1
 *     UBYTE z_Echo;            // 37, 1
 *     WORD  z_TelZone;         // 38, 2
 *     WORD  z_TelX;            // 40, 2
 *     WORD  z_TelZ;            // 42, 2
 *     WORD  z_FloorNoise;      // 44, 2
 *     WORD  z_UpperFloorNoise; // 46, 2
 *     ZPVSRecord z_PVSList[1]; // 48+, variable, terminé par pvs_ZoneID=-1
 * } Zone;                       // taille minimale : 56 bytes (header 48 + 1 PVS record)
 * </pre>
 *
 * <h2>Note hauteurs</h2>
 * Les valeurs de hauteur sont INVERSÉES : plus petit = plus haut.
 * Utiliser {@code heightOf(floor)} pour obtenir la hauteur en unités normales.
 * {@code DISABLED_HEIGHT = 5000} signifie "pas d'étage supérieur".
 */
public class ZoneData {

    /** Taille fixe du bloc Zone en bytes (sans les listes variables). */
    public static final int FIXED_SIZE = 48;
    /** Hauteur désactivée — indique l'absence d'un étage supérieur. */
    public static final int DISABLED_HEIGHT = 5000;
    /** Terminateur de liste de zones. */
    public static final short ZONE_ID_LIST_END = -1;
    /** Terminateur de liste d'edge IDs. */
    public static final short EDGE_POINT_ID_LIST_END = -4;

    // ── Champs lus depuis le binaire ──────────────────────────────────────────

    public final short zoneId;
    public final int   floor;           // hauteur sol (virgule fixe, inversée)
    public final int   roof;            // hauteur plafond
    public final int   upperFloor;      // hauteur sol étage supérieur
    public final int   upperRoof;       // hauteur plafond étage supérieur
    public final int   water;           // hauteur eau (0 = pas d'eau)
    public final short brightness;      // luminosité ambiante [0..63]
    public final short upperBrightness;
    public final short controlPoint;
    public final short backSFXMask;
    public final short unused;          // ZoneT_Unused_w — utilisé comme tag de visibilité PVS
    public final short edgeListOffset;  // offset négatif depuis la Zone struct vers la liste d'edge IDs
    public final short pointsOffset;    // offset négatif depuis la Zone struct vers la liste de point IDs
    public final byte  drawBackdrop;
    public final byte  echo;
    public final short telZone;         // zone de téléportation (-1 = aucune)
    public final short telX;
    public final short telZ;
    public final short floorNoise;
    public final short upperFloorNoise;

    // ── Listes décodées (postfixes à la struct en mémoire) ───────────────────

    /** IDs des arêtes délimitant cette zone. */
    public final short[] edgeIds;
    /** IDs des points 2D de cette zone. */
    public final short[] pointIds;
    /** Liste PVS : zones potentiellement visibles depuis cette zone. */
    public final ZPVSRecord[] pvsRecords;

    // ── Constructeur (utilisé par le parser) ─────────────────────────────────

    public ZoneData(short zoneId, int floor, int roof, int upperFloor, int upperRoof,
                    int water, short brightness, short upperBrightness,
                    short controlPoint, short backSFXMask, short unused,
                    short edgeListOffset, short pointsOffset,
                    byte drawBackdrop, byte echo,
                    short telZone, short telX, short telZ,
                    short floorNoise, short upperFloorNoise,
                    short[] edgeIds, short[] pointIds, ZPVSRecord[] pvsRecords) {
        this.zoneId           = zoneId;
        this.floor            = floor;
        this.roof             = roof;
        this.upperFloor       = upperFloor;
        this.upperRoof        = upperRoof;
        this.water            = water;
        this.brightness       = brightness;
        this.upperBrightness  = upperBrightness;
        this.controlPoint     = controlPoint;
        this.backSFXMask      = backSFXMask;
        this.unused           = unused;
        this.edgeListOffset   = edgeListOffset;
        this.pointsOffset     = pointsOffset;
        this.drawBackdrop     = drawBackdrop;
        this.echo             = echo;
        this.telZone          = telZone;
        this.telX             = telX;
        this.telZ             = telZ;
        this.floorNoise       = floorNoise;
        this.upperFloorNoise  = upperFloorNoise;
        this.edgeIds          = edgeIds;
        this.pointIds         = pointIds;
        this.pvsRecords       = pvsRecords;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Retourne la hauteur canonique depuis une valeur raw de zone.
     * Voir {@code heightOf()} dans zone_inline.h.
     */
    public static int heightOf(int raw) { return raw >> 8; }

    /** Vrai si la zone a un étage supérieur. */
    public boolean hasUpper() {
        int uf = heightOf(upperFloor);
        return uf < DISABLED_HEIGHT && uf > heightOf(upperRoof);
    }

    /** Hauteur sol en unités normalisées. */
    public int floorHeight()      { return heightOf(floor); }
    /** Hauteur plafond en unités normalisées. */
    public int roofHeight()       { return heightOf(roof); }
    /** Hauteur sol étage sup en unités normalisées. */
    public int upperFloorHeight() { return heightOf(upperFloor); }
    /** Hauteur plafond étage sup en unités normalisées. */
    public int upperRoofHeight()  { return heightOf(upperRoof); }

    @Override
    public String toString() {
        return String.format(
            "Zone{id=%d, floor=%d, roof=%d, brightness=%d, edges=%d, pvs=%d}",
            zoneId, floorHeight(), roofHeight(),
            brightness, edgeIds.length, pvsRecords.length);
    }
}
