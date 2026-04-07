package com.ab3d2.core.level;

/**
 * Toutes les données d'un niveau parsé depuis les fichiers binaires.
 *
 * <h2>Sources</h2>
 * <ul>
 *   <li>{@code twolev.bin}       — TLBT header (starts joueur, signés), control points,
 *                                   points 2D, arêtes (edges via FloorLineOffset)</li>
 *   <li>{@code twolev.graph.bin} — TLGT header, table de pointeurs de zones → ZoneT</li>
 * </ul>
 *
 * <h2>Coordonnées de départ joueur</h2>
 * Stockées comme WORD signés dans TLBT. Exemple LEVEL_A :
 * Plr1 = (-808, 184) — les valeurs négatives sont normales (coordonnées monde).
 */
public class LevelData {

    public final String levelId;

    // ── Positions de départ (WORD signés depuis TLBT) ─────────────────────────
    public final short plr1StartX, plr1StartZ;
    public final int   plr1StartZoneId;   // unsigned (zone ID)
    public final short plr2StartX, plr2StartZ;
    public final int   plr2StartZoneId;

    // ── Géométrie ─────────────────────────────────────────────────────────────
    public final Vec2W[]   controlPoints;
    public final Vec2W[]   points;
    public final ZEdge[]   edges;
    public final ZoneData[] zones;

    // ── Compteurs ─────────────────────────────────────────────────────────────
    public final int numObjects;

    // ── Tailles fichiers (debug) ──────────────────────────────────────────────
    public final int rawBinSize;
    public final int rawGraphSize;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public LevelData(String levelId,
                     short plr1StartX, short plr1StartZ, int plr1StartZoneId,
                     short plr2StartX, short plr2StartZ, int plr2StartZoneId,
                     Vec2W[] controlPoints, Vec2W[] points, ZEdge[] edges, ZoneData[] zones,
                     int numObjects, int rawBinSize, int rawGraphSize) {
        this.levelId         = levelId;
        this.plr1StartX      = plr1StartX;
        this.plr1StartZ      = plr1StartZ;
        this.plr1StartZoneId = plr1StartZoneId;
        this.plr2StartX      = plr2StartX;
        this.plr2StartZ      = plr2StartZ;
        this.plr2StartZoneId = plr2StartZoneId;
        this.controlPoints   = controlPoints;
        this.points          = points;
        this.edges           = edges;
        this.zones           = zones;
        this.numObjects      = numObjects;
        this.rawBinSize      = rawBinSize;
        this.rawGraphSize    = rawGraphSize;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public int numZones()         { return zones         != null ? zones.length         : 0; }
    public int numEdges()         { return edges         != null ? edges.length         : 0; }
    public int numPoints()        { return points        != null ? points.length        : 0; }
    public int numControlPoints() { return controlPoints != null ? controlPoints.length : 0; }

    public int numValidZones() {
        int c = 0;
        if (zones != null) for (ZoneData z : zones) if (z != null) c++;
        return c;
    }

    public ZoneData zone(int id) {
        return (id >= 0 && id < numZones()) ? zones[id] : null;
    }

    public ZEdge edge(int id) {
        return (id >= 0 && id < numEdges()) ? edges[id] : null;
    }

    public Vec2W point(int id) {
        return (id >= 0 && id < numPoints()) ? points[id] : null;
    }

    public Vec2W controlPoint(int id) {
        return (id >= 0 && id < numControlPoints()) ? controlPoints[id] : null;
    }

    @Override
    public String toString() {
        return String.format(
            "LevelData{id=%s, zones=%d/%d, edges=%d, points=%d, ctrlPts=%d, " +
            "plr1=(%d,%d) z%d, binSize=%d, graphSize=%d}",
            levelId, numValidZones(), numZones(), numEdges(), numPoints(),
            numControlPoints(), (int) plr1StartX, (int) plr1StartZ, plr1StartZoneId,
            rawBinSize, rawGraphSize);
    }
}
