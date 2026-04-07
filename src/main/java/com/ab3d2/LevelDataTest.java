package com.ab3d2;

import com.ab3d2.core.level.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;

/**
 * Test standalone — valide le parsing complet de twolev.bin + twolev.graph.bin.
 *
 * Lancement :
 *   gradle run --main-class=com.ab3d2.LevelDataTest
 */
public class LevelDataTest {

    static final String RESOURCES_ROOT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";

    public static void main(String[] args) throws Exception {
        banner("AB3D2 Level Data Test - LEVEL_A");
        Path root = Path.of(RESOURCES_ROOT);

        // ── 1. Fichiers ───────────────────────────────────────────────────────
        section("1. FICHIERS");
        Path levelDir = root.resolve("levels/LEVEL_A");
        Path binPath  = levelDir.resolve("twolev.bin");
        Path gfxPath  = levelDir.resolve("twolev.graph.bin");
        checkFile(binPath, "twolev.bin");
        checkFile(gfxPath, "twolev.graph.bin");

        byte[] binRaw = Files.readAllBytes(binPath);
        byte[] gfxRaw = Files.readAllBytes(gfxPath);
        System.out.printf("  twolev.bin      : %d bytes (0x%X)%n", binRaw.length, binRaw.length);
        System.out.printf("  twolev.graph.bin: %d bytes (0x%X)%n", gfxRaw.length, gfxRaw.length);

        // ── 2. TLBT manuel ────────────────────────────────────────────────────
        section("2. PARSING TLBT (verification manuelle)");
        dumpTLBT(binRaw, gfxRaw);

        // ── 3. TLGT manuel ────────────────────────────────────────────────────
        section("3. PARSING TLGT (verification manuelle)");
        dumpTLGT(gfxRaw, binRaw);

        // ── 4. Assemblage complet ─────────────────────────────────────────────
        section("4. ASSEMBLAGE (GraphicsBinaryParser)");
        LevelData level = null;
        try {
            level = new GraphicsBinaryParser().assemble(binRaw, gfxRaw, "A");
            System.out.println(GraphicsBinaryParser.summarize(level));
        } catch (Exception e) {
            System.out.println("  ERREUR : " + e.getMessage());
            e.printStackTrace(System.out);
        }

        if (level == null) { System.out.println("Assemblage echoue."); return; }

        // ── 5. Coherence ──────────────────────────────────────────────────────
        section("5. COHERENCE");
        verifyConsistency(level);

        // ── 6. Resume ─────────────────────────────────────────────────────────
        section("6. RESUME");
        System.out.println("  " + level);
        System.out.printf("  Plr1 : (%d, %d) zone=%d%n",
            level.plr1StartX, level.plr1StartZ, level.plr1StartZoneId);
        System.out.printf("  Plr2 : (%d, %d) zone=%d%n",
            level.plr2StartX, level.plr2StartZ, level.plr2StartZoneId);
        System.out.printf("  Zones  : %d / %d valides%n", level.numValidZones(), level.numZones());
        System.out.printf("  Edges  : %d%n", level.numEdges());
        System.out.printf("  Points : %d%n", level.numPoints());

        banner("TEST TERMINE");
    }

    // ─────────────────────────────────────────────────────────────────────────

    static void dumpTLBT(byte[] binRaw, byte[] gfxRaw) {
        // Messages
        String[] msgs = LevelBinaryParser.extractTextMessages(binRaw);
        System.out.println("  Messages texte :");
        for (int i = 0; i < msgs.length; i++)
            if (!msgs[i].isEmpty()) System.out.printf("    [%d] \"%s\"%n", i, msgs[i]);
        System.out.println();

        System.out.println("  Hex dump TLBT @ 1600 (54 bytes) :");
        System.out.println(hexDump(binRaw, 1600, 54));

        ByteBuffer b = ByteBuffer.wrap(binRaw, 1600, binRaw.length - 1600)
                                  .order(ByteOrder.BIG_ENDIAN);

        // COORDS SIGNEES (short +-32767, pas & 0xFFFF)
        short plr1x   = b.getShort();
        short plr1z   = b.getShort();
        int   plr1zn  = b.getShort() & 0xFFFF;
        short plr2x   = b.getShort();
        short plr2z   = b.getShort();
        int   plr2zn  = b.getShort() & 0xFFFF;
        int   nCtrl   = b.getShort() & 0xFFFF;
        int   nPts    = b.getShort() & 0xFFFF;
        int   nZRaw   = b.getShort() & 0xFFFF;
        int   unk     = b.getShort() & 0xFFFF;
        int   nObjs   = b.getShort() & 0xFFFF;
        int   ofsPoints    = b.getInt();
        int   ofsFloorLine = b.getInt();
        int   ofsObjData   = b.getInt();
        int   ofsShot      = b.getInt();
        int   ofsAlienShot = b.getInt();
        int   ofsObjPts    = b.getInt();
        int   ofsPlr1Obj   = b.getInt();
        int   ofsPlr2Obj   = b.getInt();

        int nZones = nZRaw + 1;

        System.out.printf("    Plr1 start      : (%d, %d) zone=%d  [SIGNES]%n", plr1x, plr1z, plr1zn);
        System.out.printf("    Plr2 start      : (%d, %d) zone=%d%n", plr2x, plr2z, plr2zn);
        System.out.printf("    NumControlPts   : %d%n", nCtrl);
        System.out.printf("    NumPoints       : %d%n", nPts);
        System.out.printf("    NumZones raw    : %d -> reel : %d (+1)%n", nZRaw, nZones);
        System.out.printf("    Unknown         : 0x%04X%n", unk);
        System.out.printf("    NumObjects      : %d%n", nObjs);
        System.out.printf("    PointsOffset    : 0x%X (%d)%n", ofsPoints, ofsPoints);
        System.out.printf("    FloorLineOffset : 0x%X (%d)  <- debut edges%n", ofsFloorLine, ofsFloorLine);
        System.out.printf("    ObjectDataOffset: 0x%X (%d)  <- AVANT FloorLine !%n", ofsObjData, ofsObjData);
        System.out.printf("    ShotOffset      : 0x%X%n", ofsShot);
        System.out.printf("    AlienShotOffset : 0x%X%n", ofsAlienShot);
        System.out.printf("    ObjPtsOffset    : 0x%X%n", ofsObjPts);
        System.out.printf("    Plr1ObjOffset   : 0x%X%n", ofsPlr1Obj);
        System.out.printf("    Plr2ObjOffset   : 0x%X%n", ofsPlr2Obj);
        System.out.println();

        // Layout du fichier (trie par offset)
        System.out.println("  Layout fichier (offset croissant) :");
        int[][] layout = {
            {0,           0},
            {1600,        1},
            {LevelBinaryParser.GEOM_DATA_START, 2},
            {ofsObjPts,   3},
            {ofsObjData,  4},
            {ofsShot,     5},
            {ofsAlienShot,6},
            {ofsPlr1Obj,  7},
            {ofsPlr2Obj,  8},
            {ofsPoints,   9},
            {ofsFloorLine,10},
        };
        java.util.Arrays.sort(layout, (a, c) -> Integer.compare(a[0], c[0]));
        String[] labels = {"Messages","TLBT","ControlPts","ObjPoints","ObjData",
                           "ShotData","AlienShot","Plr1Obj","Plr2Obj","Points","FloorLines/Edges"};
        for (int[] entry : layout)
            System.out.printf("    0x%05X (%6d) : %s%n", entry[0], entry[0], labels[entry[1]]);
        System.out.println();

        // Validation
        boolean ok = assertTrue("FloorLine > ObjData (layout attendu)", ofsFloorLine > ofsObjData)
                   & assertTrue("FloorLine > Points",  ofsFloorLine > ofsPoints)
                   & assertTrue("PointsOffset > 1654", ofsPoints >= LevelBinaryParser.GEOM_DATA_START)
                   & assertTrue("numZones > 0",         nZones > 0)
                   & assertTrue("numPoints > 0",        nPts > 0);
        System.out.println("  Validation TLBT : " + (ok ? "OK" : "ERREURS"));
    }

    static void dumpTLGT(byte[] gfxRaw, byte[] binRaw) {
        System.out.println("  Hex dump TLGT @ 0 (20 bytes) :");
        System.out.println(hexDump(gfxRaw, 0, 20));

        ByteBuffer g = ByteBuffer.wrap(gfxRaw).order(ByteOrder.BIG_ENDIAN);
        int doorOfs      = g.getInt();
        int liftOfs      = g.getInt();
        int switchOfs    = g.getInt();
        int zoneGraphOfs = g.getInt();
        // offset 16 = premier pointeur de zone
        int firstPtr = g.getInt();

        System.out.printf("    DoorDataOffset      : 0x%X (%d)%n", doorOfs, doorOfs);
        System.out.printf("    LiftDataOffset      : 0x%X (%d)%n", liftOfs, liftOfs);
        System.out.printf("    SwitchDataOffset    : 0x%X (%d)%n", switchOfs, switchOfs);
        System.out.printf("    ZoneGraphAddsOffset : 0x%X (%d)%n", zoneGraphOfs, zoneGraphOfs);
        System.out.printf("    ptr[0] @ graph[16]  : 0x%X (%d) -> ZoneT dans twolev.bin%n",
            firstPtr, firstPtr);
        System.out.println();

        // Lire numZones depuis TLBT pour dimensionner la table
        ByteBuffer bTlbt = ByteBuffer.wrap(binRaw, 1600 + 16, 2).order(ByteOrder.BIG_ENDIAN);
        int nZones = (bTlbt.getShort() & 0xFFFF) + 1;

        System.out.printf("  Table de pointeurs (%d zones, 16 premiers) :%n", nZones);
        g.position(16);
        int maxShow = Math.min(nZones, 16);
        boolean allValid = true;
        for (int i = 0; i < maxShow; i++) {
            int ptr = g.getInt();
            boolean valid = ptr > 0 && ptr + ZoneData.FIXED_SIZE <= binRaw.length;
            if (!valid) allValid = false;
            System.out.printf("    ptr[%02d] = 0x%05X (%6d) %s%n",
                i, ptr, ptr, valid ? "OK" : "INVALIDE");
        }
        if (nZones > maxShow)
            System.out.printf("    ... (%d zones au total)%n", nZones);
        System.out.println("  Tous les pointeurs valides : " + (allValid ? "OUI" : "NON"));

        // Verifier que les ptrs sont croissants
        g.position(16);
        int prev = 0; int nonMono = 0;
        for (int i = 0; i < nZones && g.remaining() >= 4; i++) {
            int ptr = g.getInt();
            if (ptr < prev) nonMono++;
            prev = ptr;
        }
        System.out.println("  Pointeurs croissants : " + (nonMono == 0 ? "OUI" : "NON (" + nonMono + " ruptures)"));
    }

    // ── Coherence ─────────────────────────────────────────────────────────────

    static void verifyConsistency(LevelData lvl) {
        int edgeWarns = 0;

        // Edge IDs dans les zones
        for (int i = 0; i < lvl.numZones(); i++) {
            ZoneData z = lvl.zone(i);
            if (z == null) continue;
            for (short eid : z.edgeIds) {
                if (eid < 0 || eid >= lvl.numEdges()) {
                    System.out.printf("  WARN Zone[%d] edge ID %d hors limites [0..%d]%n",
                        i, eid & 0xFFFF, lvl.numEdges() - 1);
                    if (++edgeWarns >= 5) { System.out.println("  ... (limite atteinte)"); break; }
                }
            }
            if (edgeWarns >= 5) break;
        }

        // Portails valides
        int badPortals = 0;
        for (int i = 0; i < lvl.numEdges(); i++) {
            ZEdge e = lvl.edge(i);
            if (e != null && e.isPortal() && e.joinZoneId() >= lvl.numZones()) badPortals++;
        }

        // PVS valides
        int badPvs = 0;
        for (int i = 0; i < lvl.numZones(); i++) {
            ZoneData z = lvl.zone(i);
            if (z == null) continue;
            for (ZPVSRecord pvs : z.pvsRecords)
                if (pvs.zoneId() >= lvl.numZones()) badPvs++;
        }

        // Stats edges
        int walls = 0, portals = 0;
        for (int i = 0; i < lvl.numEdges(); i++) {
            ZEdge e = lvl.edge(i);
            if (e == null) continue;
            if (e.isPortal()) portals++; else walls++;
        }

        // Stats PVS
        int totalPvs = 0, maxPvs = 0;
        for (int i = 0; i < lvl.numZones(); i++) {
            ZoneData z = lvl.zone(i);
            if (z == null) continue;
            totalPvs += z.pvsRecords.length;
            if (z.pvsRecords.length > maxPvs) maxPvs = z.pvsRecords.length;
        }

        // Affichage
        if (edgeWarns == 0 && badPortals == 0 && badPvs == 0)
            System.out.println("  Coherence OK - aucun probleme detecte");
        else {
            if (edgeWarns > 0)   System.out.printf("  %d edge IDs hors limites%n", edgeWarns);
            if (badPortals > 0)  System.out.printf("  %d portails vers zones invalides%n", badPortals);
            if (badPvs > 0)      System.out.printf("  %d entrees PVS hors limites%n", badPvs);
        }

        System.out.printf("  Edges  : %d murs + %d portails = %d total%n", walls, portals, walls + portals);
        System.out.printf("  PVS    : %d entrees total, max %d par zone%n", totalPvs, maxPvs);
        System.out.printf("  CtrlPt[0] : %s%n", lvl.numControlPoints() > 0 && lvl.controlPoints[0] != null
            ? "(" + lvl.controlPoints[0].xi() + ", " + lvl.controlPoints[0].zi() + ")" : "N/A");

        // Spot-check : zone de depart du joueur
        ZoneData startZone = lvl.zone(lvl.plr1StartZoneId);
        if (startZone != null) {
            System.out.printf("  Zone depart plr1 (id=%d) : %d edges, %d pvs%n",
                lvl.plr1StartZoneId, startZone.edgeIds.length, startZone.pvsRecords.length);
        } else {
            System.out.printf("  WARN zone depart plr1 (id=%d) introuvable%n", lvl.plr1StartZoneId);
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    static void checkFile(Path p, String name) {
        boolean exists = java.nio.file.Files.exists(p);
        try {
            System.out.printf("  %s %-30s%s%n",
                exists ? "OK" : "MANQUANT",
                name,
                exists ? " " + java.nio.file.Files.size(p) + " bytes" : " : " + p);
        } catch (IOException e) {
            System.out.printf("  %s %s%n", exists ? "OK" : "MANQUANT", name);
        }
    }

    static boolean assertTrue(String label, boolean cond) {
        if (!cond) System.out.printf("  FAIL %s%n", label);
        return cond;
    }

    static String hexDump(byte[] data, int offset, int count) {
        count = Math.min(count, data.length - offset);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i % 16 == 0) sb.append(String.format("    %04X : ", offset + i));
            else if (i % 8 == 0) sb.append(" ");
            sb.append(String.format("%02X ", data[offset + i] & 0xFF));
            if ((i + 1) % 16 == 0 || i == count - 1) sb.append("\n");
        }
        return sb.toString();
    }

    static void section(String title) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("  " + title);
        System.out.println("============================================================");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("************************************************************");
        System.out.printf( "*  %-56s*%n", title);
        System.out.println("************************************************************");
        System.out.println();
    }
}
