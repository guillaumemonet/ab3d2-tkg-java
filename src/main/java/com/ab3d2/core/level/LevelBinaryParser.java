package com.ab3d2.core.level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse {@code twolev.bin} d'un niveau AB3D2.
 *
 * <h2>Terminateurs de listes (zone.h, zone_inline.h)</h2>
 * {@code Zone_IsValidEdgeID(id) = id >= 0} — tout ID < 0 est un terminateur :
 * -1 = fin liste principale, -2 = fin liste partagee, -4 = fin liste de points.
 *
 * <h2>numEdges (hires.s ligne 363 : "this is probably…")</h2>
 * La formule ObjectDataOffset - FloorLineOffset est fausse pour LEVEL_A
 * (ObjectDataOffset=3014 < FloorLineOffset=25494).
 * Methode correcte : max(edge_id dans toutes les zones) + 1.
 */
public class LevelBinaryParser {

    private static final Logger log = LoggerFactory.getLogger(LevelBinaryParser.class);

    public static final int MSG_MAX_LENGTH   = 160;
    public static final int MSG_MAX_CUSTOM   = 10;
    public static final int TEXT_HEADER_SIZE = MSG_MAX_CUSTOM * MSG_MAX_LENGTH;  // 1600
    public static final int TLBT_SIZE        = 54;
    public static final int GEOM_DATA_START  = TEXT_HEADER_SIZE + TLBT_SIZE;    // 1654

    // ── BinData ───────────────────────────────────────────────────────────────

    public static class BinData {
        // Coords joueur : WORDs SIGNES Amiga (ex : -808, 184)
        public short plr1StartX, plr1StartZ, plr1StartZoneId;
        public short plr2StartX, plr2StartZ, plr2StartZoneId;

        public int numControlPoints, numPoints, numZones, numObjects;

        // Offsets absolus depuis debut du fichier
        public int pointsOffset, floorLineOffset, objectDataOffset;
        public int shotDataOffset, alienShotDataOffset;
        public int objectPointsOffset, plr1ObjectOffset, plr2ObjectOffset;

        // Donnees parsees
        public Vec2W[]  controlPoints;
        public ZEdge[]  edges = new ZEdge[0];   // rempli par parseEdges()
        public Vec2W[]  points;
        public String[] messages;
        public byte[]   raw;
    }

    // ── Passe 1 : TLBT + points + control points ──────────────────────────────

    public BinData parseBin(Path p) throws IOException {
        log.info("parseBin : {}", p);
        return parseBin(Files.readAllBytes(p));
    }

    public BinData parseBin(byte[] raw) {
        if (raw.length < GEOM_DATA_START)
            throw new IllegalArgumentException("Fichier trop petit : " + raw.length);

        ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        BinData d = new BinData();
        d.raw      = raw;
        d.messages = extractTextMessages(raw);

        b.position(TEXT_HEADER_SIZE);
        // Coords SIGNEES — pas de & 0xFFFF
        d.plr1StartX       = b.getShort();                 // +0
        d.plr1StartZ       = b.getShort();                 // +2
        d.plr1StartZoneId  = (short)(b.getShort() & 0xFFFF); // +4 zone ID (toujours >= 0)
        d.plr2StartX       = b.getShort();                 // +6
        d.plr2StartZ       = b.getShort();                 // +8
        d.plr2StartZoneId  = (short)(b.getShort() & 0xFFFF); // +10
        d.numControlPoints = b.getShort() & 0xFFFF;        // +12
        d.numPoints        = b.getShort() & 0xFFFF;        // +14
        d.numZones         = (b.getShort() & 0xFFFF) + 1;  // +16  stocke -1 !
        b.getShort();                                       // +18  inconnu
        d.numObjects       = b.getShort() & 0xFFFF;        // +20
        d.pointsOffset        = b.getInt();                 // +22
        d.floorLineOffset     = b.getInt();                 // +26
        d.objectDataOffset    = b.getInt();                 // +30
        d.shotDataOffset      = b.getInt();                 // +34
        d.alienShotDataOffset = b.getInt();                 // +38
        d.objectPointsOffset  = b.getInt();                 // +42
        d.plr1ObjectOffset    = b.getInt();                 // +46
        d.plr2ObjectOffset    = b.getInt();                 // +50

        log.info("TLBT: plr1=({},{}) zone={}, zones={}, pts={}, ctrl={}, objs={}",
            d.plr1StartX, d.plr1StartZ, d.plr1StartZoneId & 0xFFFF,
            d.numZones, d.numPoints, d.numControlPoints, d.numObjects);
        log.info("Offsets: pts=0x{}, floorLines=0x{}, objData=0x{}",
            Integer.toHexString(d.pointsOffset),
            Integer.toHexString(d.floorLineOffset),
            Integer.toHexString(d.objectDataOffset));

        // Control points @ GEOM_DATA_START
        d.controlPoints = new Vec2W[d.numControlPoints];
        b.position(GEOM_DATA_START);
        for (int i = 0; i < d.numControlPoints; i++) {
            if (b.remaining() < 4) break;
            d.controlPoints[i] = new Vec2W(b.getShort(), b.getShort());
        }

        // Points 2D
        d.points = new Vec2W[d.numPoints];
        if (d.pointsOffset > 0 && d.pointsOffset + (long)d.numPoints * 4 <= raw.length) {
            b.position(d.pointsOffset);
            for (int i = 0; i < d.numPoints; i++)
                d.points[i] = new Vec2W(b.getShort(), b.getShort());
            log.debug("{} points @ 0x{}", d.numPoints, Integer.toHexString(d.pointsOffset));
        } else {
            log.warn("pointsOffset invalide 0x{}", Integer.toHexString(d.pointsOffset));
        }

        return d;
    }

    // ── Passe 2 : edges (apres avoir parse les zones pour connaitre numEdges) ──

    /**
     * Calcule numEdges = max(edge_id dans toutes les zones) + 1,
     * puis lit le tableau ZEdge depuis FloorLineOffset.
     *
     * Deux estimations sont calculees et le max est pris :
     * - maxIdFromZones : exact si toutes les zones ont ete parsees
     * - fromBlobStart  : min(ptr + edgeListOffset) - floorLineOffset (peut sur-estimer)
     */
    public void parseEdges(BinData d, ZoneData[] zones, int[] zonePtrs) {
        if (d.floorLineOffset <= 0 || d.floorLineOffset >= d.raw.length) {
            log.warn("floorLineOffset invalide");
            d.edges = new ZEdge[0];
            return;
        }

        // Estimation 1 : max edge ID depuis les zones
        int maxId = -1;
        for (ZoneData z : zones) {
            if (z == null) continue;
            for (short eid : z.edgeIds)
                if (eid > maxId) maxId = eid;
        }
        int numEdgesFromIds = maxId >= 0 ? maxId + 1 : 0;

        // Estimation 2 : blob start = min(ptr + edgeListOffset) parmi les zones valides
        int minBlob = Integer.MAX_VALUE;
        for (int i = 0; i < zones.length; i++) {
            ZoneData z = zones[i];
            if (z == null || i >= zonePtrs.length) continue;
            int ptr = zonePtrs[i];
            if (ptr <= 0 || ptr >= d.raw.length) continue;
            if (z.edgeListOffset < 0) {
                int s = ptr + z.edgeListOffset;
                if (s > 0 && s < minBlob) minBlob = s;
            }
        }
        int numEdgesFromBlob = (minBlob != Integer.MAX_VALUE && minBlob > d.floorLineOffset)
            ? (minBlob - d.floorLineOffset) / ZEdge.BINARY_SIZE : 0;

        // On prend le plus grand (ne pas manquer des edges)
        int numEdges = Math.max(numEdgesFromIds, numEdgesFromBlob);

        log.info("Edges: floorLine=0x{}, maxId={} (-> {}), blobStart=0x{} (-> {}), choix={}",
            Integer.toHexString(d.floorLineOffset),
            maxId, numEdgesFromIds,
            minBlob != Integer.MAX_VALUE ? Integer.toHexString(minBlob) : "N/A",
            numEdgesFromBlob, numEdges);

        if (numEdges <= 0) { d.edges = new ZEdge[0]; return; }

        // Ajuster si depasse la fin du fichier
        int avail = (d.raw.length - d.floorLineOffset) / ZEdge.BINARY_SIZE;
        if (numEdges > avail) {
            log.warn("numEdges {} > {} disponibles, ajustement", numEdges, avail);
            numEdges = avail;
        }

        ByteBuffer buf = ByteBuffer.wrap(d.raw).order(ByteOrder.BIG_ENDIAN);
        buf.position(d.floorLineOffset);
        d.edges = new ZEdge[numEdges];
        for (int i = 0; i < numEdges; i++)
            d.edges[i] = parseEdge(buf);
    }

    // ── Zone parsing ─────────────────────────────────────────────────────────

    /**
     * Parse une ZoneT depuis un offset absolu dans twolev.bin.
     * L'offset pointe sur le debut de la struct (48 bytes).
     */
    public ZoneData parseZoneAt(byte[] raw, int off) {
        if (off < 0 || off + ZoneData.FIXED_SIZE > raw.length)
            throw new IllegalArgumentException("Zone offset invalide : " + off);

        ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        b.position(off);

        short zoneId          = b.getShort();  //  0
        int   floor           = b.getInt();     //  2
        int   roof            = b.getInt();     //  6
        int   upperFloor      = b.getInt();     // 10
        int   upperRoof       = b.getInt();     // 14
        int   water           = b.getInt();     // 18
        short brightness      = b.getShort();  // 22
        short upperBrightness = b.getShort();  // 24
        short controlPoint    = b.getShort();  // 26
        short backSFXMask     = b.getShort();  // 28
        short unused          = b.getShort();  // 30
        short edgeListOffset  = b.getShort();  // 32  signe negatif
        short pointsOffset    = b.getShort();  // 34  signe negatif
        byte  drawBackdrop    = b.get();        // 36
        byte  echo            = b.get();        // 37
        short telZone         = b.getShort();  // 38
        short telX            = b.getShort();  // 40
        short telZ            = b.getShort();  // 42
        short floorNoise      = b.getShort();  // 44
        short upperFloorNoise = b.getShort();  // 46
        // b.position() == off + 48  (FIXED_SIZE)

        short[] edgeIds  = readIds(raw, off, edgeListOffset);
        short[] pointIds = readIds(raw, off, pointsOffset);

        // PVS list apres la struct, terminee par pvs_ZoneID < 0
        List<ZPVSRecord> pvs = new ArrayList<>();
        while (b.position() + 2 <= raw.length) {
            short pid = b.getShort();
            if (pid < 0) break;
            if (b.remaining() < 6) break;
            pvs.add(new ZPVSRecord(pid, b.getShort(), b.getShort(), b.getShort()));
        }

        return new ZoneData(zoneId, floor, roof, upperFloor, upperRoof, water,
            brightness, upperBrightness, controlPoint, backSFXMask, unused,
            edgeListOffset, pointsOffset, drawBackdrop, echo,
            telZone, telX, telZ, floorNoise, upperFloorNoise,
            edgeIds, pointIds, pvs.toArray(new ZPVSRecord[0]));
    }

    /** Lit les IDs >= 0 depuis baseOffset + negOffset jusqu'au premier ID < 0. */
    private short[] readIds(byte[] raw, int baseOffset, short negOffset) {
        if (negOffset >= 0) return new short[0];
        int start = baseOffset + negOffset;
        if (start < 0) return new short[0];
        List<Short> ids = new ArrayList<>();
        ByteBuffer tmp = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        tmp.position(start);
        while (tmp.position() < baseOffset && tmp.remaining() >= 2) {
            short v = tmp.getShort();
            if (v < 0) break;  // -1, -2, -4... tous terminateurs
            ids.add(v);
        }
        short[] a = new short[ids.size()];
        for (int i = 0; i < a.length; i++) a[i] = ids.get(i);
        return a;
    }

    private ZEdge parseEdge(ByteBuffer b) {
        short px = b.getShort(), pz = b.getShort();
        short lx = b.getShort(), lz = b.getShort();
        short join = b.getShort(), w5 = b.getShort();
        byte b12 = b.get(), b13 = b.get();
        return new ZEdge(new Vec2W(px, pz), new Vec2W(lx, lz), join, w5, b12, b13, b.getShort());
    }

    // ── Messages texte ────────────────────────────────────────────────────────

    public static String[] extractTextMessages(byte[] raw) {
        String[] msgs = new String[MSG_MAX_CUSTOM];
        for (int i = 0; i < MSG_MAX_CUSTOM; i++) {
            int s = i * MSG_MAX_LENGTH;
            if (s + MSG_MAX_LENGTH > raw.length) { msgs[i] = ""; continue; }
            int len = 0;
            while (len < MSG_MAX_LENGTH && raw[s + len] != 0) len++;
            msgs[i] = new String(raw, s, len, java.nio.charset.StandardCharsets.ISO_8859_1).trim();
        }
        return msgs;
    }

    public static String dumpHex(byte[] data, int offset, int count) {
        count = Math.min(count, data.length - offset);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i % 16 == 0) sb.append(String.format("%n  %04X : ", offset + i));
            else if (i % 8 == 0) sb.append(" ");
            sb.append(String.format("%02X ", data[offset + i] & 0xFF));
        }
        return sb.toString();
    }
}
