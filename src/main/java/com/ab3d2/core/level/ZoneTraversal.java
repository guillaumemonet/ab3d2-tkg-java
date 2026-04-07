package com.ab3d2.core.level;

/**
 * Traversal de zones — detecte dans quelle zone se trouve le joueur
 * et met a jour sa zone quand il franchit un portail.
 *
 * <h2>Convention de signe (etablie depuis objectmove.s + test reel)</h2>
 * La formule Zone_SideOfEdge(zone_inline.h) :
 * <pre>
 *   result = lenX * (pz - posZ) - lenZ * (px - posX)
 * </pre>
 * Donne le resultat OPPOSE de la formule de franchissement d'objectmove.s :
 * <pre>
 *   objectmove : (px-posX)*lenZ - (pz-posZ)*lenX  ≤ 0 → franchissement
 *   sideOf     : lenX*(pz-posZ) - lenZ*(px-posX) = -objectmove
 * </pre>
 *
 * <b>Donc : sideOf < 0 = interieur de la zone, sideOf >= 0 = exterieur.</b>
 *
 * Verifie experimentalement : la position de depart (-808, 184) en zone 3
 * donne sideOf < 0 pour toutes les aretes de zone 3 apres correction.
 *
 * <h2>Algorithme de traversal</h2>
 * Depuis une zone connue Z :
 * <ol>
 *   <li>Pour chaque arete portail de Z (joinZoneId >= 0)</li>
 *   <li>Calculer sideOf(position_joueur, arete)</li>
 *   <li>Si sideOf >= 0 : le joueur est passe de l'autre cote → nouvelle zone</li>
 *   <li>Recommencer recursivement depuis la nouvelle zone (max MAX_DEPTH)</li>
 * </ol>
 */
public class ZoneTraversal {

    private static final int MAX_DEPTH = 8;

    // ── API principale ────────────────────────────────────────────────────────

    /**
     * Met a jour la zone courante apres un mouvement.
     * Commence depuis la zone courante, teste seulement les portails.
     */
    public static int updateZone(LevelData level, int currentZoneId, float newX, float newZ) {
        return traverse(level, currentZoneId, newX, newZ, 0);
    }

    /**
     * Trouve la zone contenant la position (x, z), en partant d'une zone hint.
     */
    public static int findZone(LevelData level, int hintZoneId, float x, float z) {
        if (isInsideZone(level, hintZoneId, x, z)) return hintZoneId;

        // Verifier les zones PVS de la hint (rapide)
        ZoneData hint = level.zone(hintZoneId);
        if (hint != null) {
            for (ZPVSRecord pvs : hint.pvsRecords) {
                int vid = pvs.zoneId() & 0xFFFF;
                if (isInsideZone(level, vid, x, z)) return vid;
            }
        }

        // Fallback : cherche dans toutes les zones
        for (int i = 0; i < level.numZones(); i++) {
            if (i == hintZoneId) continue;
            if (isInsideZone(level, i, x, z)) return i;
        }
        return hintZoneId;
    }

    /**
     * Teste si la position (x, z) est a l'interieur d'une zone.
     * Interieur = sideOf < 0 pour toutes les aretes de la zone.
     */
    public static boolean isInsideZone(LevelData level, int zoneId, float x, float z) {
        ZoneData zone = level.zone(zoneId);
        if (zone == null || zone.edgeIds.length == 0) return false;

        for (short edgeId : zone.edgeIds) {
            if (edgeId < 0 || edgeId >= level.numEdges()) continue;
            ZEdge edge = level.edge(edgeId);
            if (edge == null) continue;
            // Exterieur = sideOf > 0 → pas dans cette zone
            if (sideOf(edge, x, z) > 0) return false;
        }
        return true;
    }

    // ── Recursion de traversal ────────────────────────────────────────────────

    private static int traverse(LevelData level, int zoneId, float x, float z, int depth) {
        if (depth >= MAX_DEPTH) return zoneId;

        ZoneData zone = level.zone(zoneId);
        if (zone == null) return zoneId;

        for (short edgeId : zone.edgeIds) {
            if (edgeId < 0 || edgeId >= level.numEdges()) continue;
            ZEdge edge = level.edge(edgeId);
            if (edge == null || !edge.isPortal()) continue;

            // Franchissement : sideOf >= 0 = le joueur est sorti de cette zone
            if (sideOf(edge, x, z) >= 0) {
                int nextZone = edge.joinZoneId() & 0xFFFF;
                if (nextZone >= level.numZones()) continue;
                return traverse(level, nextZone, x, z, depth + 1);
            }
        }
        return zoneId;
    }

    // ── sideOf (zone_inline.h) ────────────────────────────────────────────────

    /**
     * result = lenX*(pz - posZ) - lenZ*(px - posX)
     * Negatif = interieur zone, Positif = exterieur.
     */
    public static int sideOf(ZEdge edge, float px, float pz) {
        long lenX = edge.len().xi();
        long lenZ = edge.len().zi();
        long posX = edge.pos().xi();
        long posZ = edge.pos().zi();
        long result = (long) (lenX * (pz - posZ) - lenZ * (px - posX));
        return Long.signum(result);
    }

    public static int sideOfInt(ZEdge edge, int px, int pz) {
        long lenX = edge.len().xi();
        long lenZ = edge.len().zi();
        long posX = edge.pos().xi();
        long posZ = edge.pos().zi();
        long result = lenX * (pz - posZ) - lenZ * (px - posX);
        return Long.signum(result);
    }
}
