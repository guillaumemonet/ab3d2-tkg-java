package com.ab3d2.core.level;

/**
 * Entrée de la liste PVS (Potentially Visible Set) d'une zone.
 * Correspond exactement à {@code ZPVSRecord} dans zone.h :
 * <pre>
 * typedef struct {
 *     WORD pvs_ZoneID;  // 0, 2  (-1 = fin de liste)
 *     WORD pvs_ClipID;  // 2, 2
 *     WORD pvs_Word2;   // 4, 2
 *     WORD pvs_Word3;   // 6, 2
 * } ZPVSRecord;          // total : 8 bytes
 * </pre>
 *
 * La liste de ZPVSRecord suit immédiatement la Zone struct en mémoire.
 * Elle est terminée par un enregistrement dont {@code pvs_ZoneID == -1}.
 */
public record ZPVSRecord(
        short zoneId,   // -1 = ZONE_ID_LIST_END (fin de liste)
        short clipId,
        short word2,    // usage inconnu
        short word3     // usage inconnu
) {
    public static final short ZONE_ID_LIST_END = -1;

    public boolean isEnd() { return zoneId == ZONE_ID_LIST_END; }

    @Override
    public String toString() {
        return "PVS{zone=" + zoneId + ", clip=" + clipId + "}";
    }
}
