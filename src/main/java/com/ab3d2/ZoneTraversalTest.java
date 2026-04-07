package com.ab3d2;

import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;

import java.nio.file.*;

/**
 * Test standalone du zone traversal.
 *
 * Convention AB3D2 (verifiee sur LEVEL_A) :
 *   sideOf < 0 (INSIDE)  = interieur de la zone
 *   sideOf > 0 (OUTSIDE) = exterieur / franchissement portail
 *
 * Lancement :
 *   gradle run --main-class=com.ab3d2.ZoneTraversalTest
 */
public class ZoneTraversalTest {

    static final String RESOURCES_ROOT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";

    public static void main(String[] args) throws Exception {
        banner("Zone Traversal Test - LEVEL_A");

        Path root = Path.of(RESOURCES_ROOT);
        Tables.initFromBytes(new byte[0]);

        LevelData level = new GraphicsBinaryParser().load(
            root.resolve("levels/LEVEL_A/twolev.bin"),
            root.resolve("levels/LEVEL_A/twolev.graph.bin"), "A");

        System.out.printf("Niveau : %d zones, %d edges%n%n",
            level.numZones(), level.numEdges());

        // ── 1. Zone de depart ─────────────────────────────────────────────────
        section("1. Position de depart (plr1Start)");
        float px = level.plr1StartX;
        float pz = level.plr1StartZ;
        int   expectedZone = level.plr1StartZoneId;

        System.out.printf("Position : (%.0f, %.0f)  zone attendue : %d%n", px, pz, expectedZone);
        System.out.println("Convention : sideOf < 0 = INSIDE, sideOf > 0 = OUTSIDE");

        boolean inside = ZoneTraversal.isInsideZone(level, expectedZone, px, pz);
        System.out.printf("isInsideZone(%d) = %s  %s%n",
            expectedZone, inside, inside ? "OK" : "FAIL");

        int found = ZoneTraversal.findZone(level, expectedZone, px, pz);
        System.out.printf("findZone(hint=%d) = %d  %s%n",
            expectedZone, found, found == expectedZone ? "OK" : "WARN (zone differente)");

        // ── 2. SideOf de toutes les aretes de la zone de depart ───────────────
        section("2. SideOf pour toutes les aretes de la zone " + expectedZone);
        ZoneData startZone = level.zone(expectedZone);
        if (startZone != null) {
            int insideCount = 0, outsideCount = 0, onEdge = 0;
            for (short eid : startZone.edgeIds) {
                ZEdge edge = level.edge(eid);
                if (edge == null) continue;
                int side = ZoneTraversal.sideOf(edge, px, pz);
                // INSIDE = negatif, OUTSIDE = positif
                String label = side < 0 ? "INSIDE" : side > 0 ? "OUTSIDE" : "ON    ";
                System.out.printf("  edge[%3d] %s  pos=(%5d,%5d) len=(%5d,%5d) %s%n",
                    eid, label,
                    edge.pos().xi(), edge.pos().zi(),
                    edge.len().xi(), edge.len().zi(),
                    edge.isPortal() ? "portail->zone" + (edge.joinZoneId() & 0xFFFF) : "mur");
                if (side < 0) insideCount++;
                else if (side > 0) outsideCount++;
                else onEdge++;
            }
            System.out.printf("  Resume : %d INSIDE, %d OUTSIDE, %d ON-EDGE%n",
                insideCount, outsideCount, onEdge);
            // OK = tous les edges donnent INSIDE (ou ON-EDGE)
            System.out.println(outsideCount == 0
                ? "  OK - position bien a l'interieur de la zone"
                : "  WARN - " + outsideCount + " aretes donnent OUTSIDE");
        }

        // ── 3. Test traversal ─────────────────────────────────────────────────
        section("3. Traversal : deplacements depuis la zone " + expectedZone);
        float[][] testMoves = {
            {px,      pz + 50},
            {px,      pz + 100},
            {px,      pz + 200},
            {px + 50, pz},
            {px - 50, pz},
            {px,      pz - 50},
        };
        String[] moveNames = {"+Z  50", "+Z 100", "+Z 200", "+X  50", "-X  50", "-Z  50"};

        int coherent = 0, diverge = 0;
        for (int i = 0; i < testMoves.length; i++) {
            float nx = testMoves[i][0], nz = testMoves[i][1];
            int newZone   = ZoneTraversal.updateZone(level, expectedZone, nx, nz);
            int foundZone = ZoneTraversal.findZone(level, expectedZone, nx, nz);
            boolean ok = newZone == foundZone;
            if (ok) coherent++; else diverge++;
            System.out.printf("  %-8s (%.0f,%.0f) updateZone=%3d  findZone=%3d  %s%n",
                moveNames[i], nx, nz, newZone, foundZone, ok ? "OK" : "DIVERGE");
        }
        System.out.printf("  Bilan : %d OK, %d DIVERGE%n", coherent, diverge);

        // ── 4. Analyse des zones ──────────────────────────────────────────────
        section("4. Analyse des zones");
        int withWalls = 0, onlyPortals = 0, withPortals = 0;
        for (int i = 0; i < level.numZones(); i++) {
            ZoneData z = level.zone(i);
            if (z == null || z.edgeIds.length == 0) continue;
            boolean hasWall = false, hasPortal = false;
            for (short eid : z.edgeIds) {
                ZEdge e = level.edge(eid);
                if (e == null) continue;
                if (!e.isPortal()) hasWall = true;
                else hasPortal = true;
            }
            if (hasWall)   withWalls++;
            if (hasPortal) withPortals++;
            if (hasPortal && !hasWall) onlyPortals++;
        }
        System.out.printf("  Zones avec murs         : %d%n", withWalls);
        System.out.printf("  Zones avec portails     : %d%n", withPortals);
        System.out.printf("  Zones portails only     : %d%n", onlyPortals);

        // ── 5. Graphe de connectivite ─────────────────────────────────────────
        section("5. Voisins de la zone " + expectedZone + " (profondeur 2)");
        printNeighbors(level, expectedZone, 0);

        banner("TEST TERMINE");
    }

    static void printNeighbors(LevelData level, int zoneId, int depth) {
        if (depth > 2) return;
        ZoneData z = level.zone(zoneId);
        if (z == null) return;
        String indent = "  ".repeat(depth + 1);
        for (short eid : z.edgeIds) {
            ZEdge e = level.edge(eid);
            if (e == null || !e.isPortal()) continue;
            int neighbor = e.joinZoneId() & 0xFFFF;
            System.out.printf("%szone[%d] --edge[%d]--> zone[%d]%n",
                indent, zoneId, eid, neighbor);
            if (depth < 1) printNeighbors(level, neighbor, depth + 1);
        }
    }

    static void section(String t) {
        System.out.println("\n" + "=".repeat(55) + "\n  " + t + "\n" + "=".repeat(55));
    }
    static void banner(String t) {
        System.out.println("\n" + "*".repeat(55) + "\n*  " + t + "\n" + "*".repeat(55) + "\n");
    }
}
