package com.ab3d2.core.level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;

/**
 * Parse twolev.graph.bin (TLGT) et assemble le {@link LevelData} complet.
 *
 * <h2>Ordre des passes</h2>
 * <ol>
 *   <li>Parse TLBT (twolev.bin) → positions joueur, comptes, offsets, control points, points</li>
 *   <li>Parse TLGT (graph.bin) → table de pointeurs de zones</li>
 *   <li>Parse zones (twolev.bin, via ptrs graph.bin)</li>
 *   <li>Calcule numEdges depuis les blobs de zones ({@code minBlobStart - FloorLineOffset) / 16})</li>
 *   <li>Parse arêtes</li>
 * </ol>
 */
public class GraphicsBinaryParser {

    private static final Logger log = LoggerFactory.getLogger(GraphicsBinaryParser.class);

    public static final int TLGT_HEADER_SIZE    = 20;  // 5 × ULONG
    public static final int TLGT_ZONE_TABLE_OFS = 16;  // TLGT_ZoneAddsOffset_l

    /** Résumé du header TLGT. */
    public static class GraphHeader {
        public int doorDataOffset;
        public int liftDataOffset;
        public int switchDataOffset;
        public int zoneGraphAddsOffset;
    }

    // ── API principale ────────────────────────────────────────────────────────

    public LevelData load(Path binPath, Path graphPath, String levelId) throws IOException {
        byte[] binRaw   = Files.readAllBytes(binPath);
        byte[] graphRaw = Files.readAllBytes(graphPath);
        log.debug("twolev.bin : {} bytes, graph.bin : {} bytes", binRaw.length, graphRaw.length);
        return assemble(binRaw, graphRaw, levelId);
    }

    public LevelData assemble(byte[] binRaw, byte[] graphRaw, String levelId) {

        // ── 1. Parse TLBT (passe 1 : header + ctrl pts + points) ─────────────
        LevelBinaryParser binParser = new LevelBinaryParser();
        LevelBinaryParser.BinData bd = binParser.parseBin(binRaw);

        // ── 2. Parse TLGT header ──────────────────────────────────────────────
        if (graphRaw.length < TLGT_HEADER_SIZE)
            throw new IllegalArgumentException("graph.bin trop petit : " + graphRaw.length);

        ByteBuffer gBuf = ByteBuffer.wrap(graphRaw).order(ByteOrder.BIG_ENDIAN);
        GraphHeader gh = new GraphHeader();
        gh.doorDataOffset      = gBuf.getInt();  //  0
        gh.liftDataOffset      = gBuf.getInt();  //  4
        gh.switchDataOffset    = gBuf.getInt();  //  8
        gh.zoneGraphAddsOffset = gBuf.getInt();  // 12
        // offset 16 = premier pointeur de la table de zones (TLGT_ZoneAddsOffset_l)

        log.info("TLGT : doors@0x{}, lifts@0x{}, switches@0x{}, zoneGraphAdds@0x{}",
            Integer.toHexString(gh.doorDataOffset),
            Integer.toHexString(gh.liftDataOffset),
            Integer.toHexString(gh.switchDataOffset),
            Integer.toHexString(gh.zoneGraphAddsOffset));

        // ── 3. Table de pointeurs de zones @ graph.bin+16 ─────────────────────
        int numZones   = bd.numZones;
        int tableStart = TLGT_ZONE_TABLE_OFS;
        int tableBytes = numZones * 4;

        if (tableStart + tableBytes > graphRaw.length) {
            int adjusted = (graphRaw.length - tableStart) / 4;
            log.warn("Table zones tronquée : {} → {}", numZones, adjusted);
            numZones = adjusted;
            tableBytes = numZones * 4;
        }

        gBuf.position(tableStart);
        int[] zonePtrs = new int[numZones];
        for (int i = 0; i < numZones; i++) zonePtrs[i] = gBuf.getInt();

        log.debug("Table {} ptrs zones lue @ graph+{} — ptr[0]=0x{}, ptr[last]=0x{}",
            numZones, tableStart,
            numZones > 0 ? Integer.toHexString(zonePtrs[0]) : "?",
            numZones > 0 ? Integer.toHexString(zonePtrs[numZones - 1]) : "?");

        // ── 4. Parse zones (passe 1) ──────────────────────────────────────────
        ZoneData[] zones = new ZoneData[numZones];
        int validZones = 0;
        for (int i = 0; i < numZones; i++) {
            int ptr = zonePtrs[i];
            if (ptr <= 0 || ptr + ZoneData.FIXED_SIZE > binRaw.length) {
                log.trace("Zone[{}] ptr invalide 0x{}", i, Integer.toHexString((int) (ptr & 0xFFFFFFFFL)));
                continue;
            }
            try {
                zones[i] = binParser.parseZoneAt(binRaw, ptr);
                validZones++;
            } catch (Exception e) {
                log.warn("Zone[{}] erreur @ 0x{}: {}", i, Integer.toHexString(ptr), e.getMessage());
            }
        }
        log.info("{}/{} zones valides", validZones, numZones);

        // ── 5. Parse arêtes (passe 2 — utilise les zones pour numEdges) ────────
        binParser.parseEdges(bd, zones, zonePtrs);

        // ── 6. Assembler LevelData ────────────────────────────────────────────
        return new LevelData(
            levelId,
            bd.plr1StartX, bd.plr1StartZ, bd.plr1StartZoneId,
            bd.plr2StartX, bd.plr2StartZ, bd.plr2StartZoneId,
            bd.controlPoints,
            bd.points,
            bd.edges,
            zones,
            bd.numObjects,
            binRaw.length, graphRaw.length
        );
    }

    // ── Accès sections graph.bin ──────────────────────────────────────────────

    public byte[] extractDoorData(byte[] graphRaw, GraphHeader gh) {
        return extractSection(graphRaw, gh.doorDataOffset, gh.liftDataOffset, "doors");
    }
    public byte[] extractLiftData(byte[] graphRaw, GraphHeader gh) {
        return extractSection(graphRaw, gh.liftDataOffset, gh.switchDataOffset, "lifts");
    }
    public byte[] extractSwitchData(byte[] graphRaw, GraphHeader gh) {
        return extractSection(graphRaw, gh.switchDataOffset, gh.zoneGraphAddsOffset, "switches");
    }

    private byte[] extractSection(byte[] raw, int from, int to, String name) {
        if (from <= 0 || from >= raw.length) return null;
        int end = (to > from && to <= raw.length) ? to : raw.length;
        byte[] result = new byte[end - from];
        System.arraycopy(raw, from, result, 0, result.length);
        log.debug("Section '{}' : {} bytes @ 0x{}", name, result.length, Integer.toHexString(from));
        return result;
    }

    // ── Résumé lisible ────────────────────────────────────────────────────────

    public static String summarize(LevelData lvl) {
        if (lvl == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Niveau %s ===%n", lvl.levelId));
        sb.append(String.format("Fichiers  : bin=%d bytes, graph=%d bytes%n",
            lvl.rawBinSize, lvl.rawGraphSize));
        sb.append(String.format("Joueur 1  : (%d, %d) zone=%d%n",
            lvl.plr1StartX, lvl.plr1StartZ, lvl.plr1StartZoneId));
        sb.append(String.format("Joueur 2  : (%d, %d) zone=%d%n",
            lvl.plr2StartX, lvl.plr2StartZ, lvl.plr2StartZoneId));
        sb.append(String.format("Zones     : %d / %d valides%n",
            lvl.numValidZones(), lvl.numZones()));
        sb.append(String.format("Aretes    : %d%n", lvl.numEdges()));
        sb.append(String.format("Points    : %d%n", lvl.numPoints()));
        sb.append(String.format("Ctrl pts  : %d%n", lvl.numControlPoints()));
        sb.append(String.format("Objets    : %d%n", lvl.numObjects));

        int shown = Math.min(lvl.numZones(), 8);
        sb.append(String.format("%nZones [0..%d] :%n", shown - 1));
        for (int i = 0; i < shown; i++) {
            ZoneData z = lvl.zone(i);
            if (z == null) {
                sb.append(String.format("  [%02d] null%n", i));
            } else {
                sb.append(String.format(
                    "  [%02d] id=%-3d floor=%-6d roof=%-6d bright=%-4d edges=%d pts=%d pvs=%d%s%n",
                    i, z.zoneId & 0xFFFF, z.floorHeight(), z.roofHeight(),
                    z.brightness & 0xFFFF,
                    z.edgeIds.length, z.pointIds.length, z.pvsRecords.length,
                    z.hasUpper() ? " [upper]" : ""));
            }
        }

        int edgeShown = Math.min(lvl.numEdges(), 5);
        if (edgeShown > 0) {
            sb.append(String.format("%nAretes [0..%d] :%n", edgeShown - 1));
            for (int i = 0; i < edgeShown; i++) {
                ZEdge e = lvl.edge(i);
                if (e == null) sb.append(String.format("  [%03d] null%n", i));
                else sb.append(String.format("  [%03d] pos=(%5d,%5d) len=(%5d,%5d) %s%n",
                    i, e.pos().xi(), e.pos().zi(), e.len().xi(), e.len().zi(),
                    e.isPortal() ? "-> zone " + e.joinZoneId() : "MUR"));
            }
        }

        int ptShown = Math.min(lvl.numPoints(), 5);
        sb.append(String.format("%nPoints [0..%d] :%n", ptShown - 1));
        for (int i = 0; i < ptShown; i++) {
            Vec2W p = lvl.point(i);
            if (p == null) sb.append(String.format("  [%03d] null%n", i));
            else sb.append(String.format("  [%03d] (%6d, %6d)%n", i, p.xi(), p.zi()));
        }

        return sb.toString();
    }
}
