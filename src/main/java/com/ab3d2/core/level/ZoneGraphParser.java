package com.ab3d2.core.level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Parse la section ZoneGraphAdds de twolev.graph.bin.
 *
 * <h2>Structure du flux de rendu par zone</h2>
 * Le flux commence par un WORD zone_id puis une sequence de records :
 * <pre>
 * WORD [wallId_high | typeByte_low] :
 *   typeByte < 0  (>= 128) → fin de flux, juste ce WORD (2 bytes)
 *   typeByte = 0            → mur : 28 bytes supplementaires = 30 bytes total
 *   typeByte = 1 ou 2       → sol/plafond (variable) :
 *                               WORD floorY
 *                               WORD sides_minus_1  (N = sides_minus_1 + 1)
 *                               N * WORD side_indices
 *                               WORD skip (inconnu)
 *                               WORD scaleval
 *                               WORD whichtile  ← IMPORTANT pour la texture sol
 *                               WORD lighttype
 *                             = 14 + N*2 bytes apres le type WORD
 *   typeByte = 4            → objet
 *   typeByte = 7            → water
 *   typeByte = 12           → backdrop
 *   (autres types) → inconnus, on tente de continuer
 * </pre>
 *
 * <h2>ZoneGraphAdds (stride 8)</h2>
 * Confirme par draw_zone_graph.s : move.l (a0,d7.w*8),a0
 * LONG[2*i]   = offset lower zone i
 * LONG[2*i+1] = offset upper zone i (0 si absent)
 */
public class ZoneGraphParser {

    private static final Logger log = LoggerFactory.getLogger(ZoneGraphParser.class);

    private static final int MAX_RECORDS_PER_ZONE  = 512;
    private static final int MAX_FLOOR_SIDES       = 32;

    /** Type byte values in the render stream. */
    public static final int TYPE_WALL    = 0;
    public static final int TYPE_FLOOR   = 1;   // regular floor
    public static final int TYPE_CEILING = 2;   // ceiling / upper floor
    public static final int TYPE_OBJECT  = 4;
    public static final int TYPE_WATER   = 7;
    public static final int TYPE_BACKDROP= 12;
    public static final int TYPE_END_MIN = 128; // typeByte >= 128 → end

    /**
     * Parse et retourne les WallRenderEntry par zone.
     * Gere correctement le flux mixte (murs + sols + objets).
     */
    public WallRenderEntry[][] parse(byte[] graphRaw, int numZones, int zoneGraphAddsOffset) {
        WallRenderEntry[][] result = new WallRenderEntry[numZones][];
        for (int i = 0; i < numZones; i++) result[i] = new WallRenderEntry[0];

        if (zoneGraphAddsOffset <= 0 ||
            zoneGraphAddsOffset + numZones * 8 > graphRaw.length) {
            log.warn("ZoneGraphAdds offset invalide : 0x{}", Integer.toHexString(zoneGraphAddsOffset));
            return result;
        }

        ByteBuffer buf = ByteBuffer.wrap(graphRaw).order(ByteOrder.BIG_ENDIAN);

        // Table d'offsets (stride 8 : 2 LONGs par zone)
        buf.position(zoneGraphAddsOffset);
        int[] lowerOfs = new int[numZones];
        for (int i = 0; i < numZones; i++) {
            lowerOfs[i] = buf.getInt();
            buf.getInt(); // upper offset (ignore pour les murs)
        }

        log.debug("ZoneGraphAdds : {} zones @ 0x{}, ofs[0]=0x{}, ofs[last]=0x{}",
            numZones,
            Integer.toHexString(zoneGraphAddsOffset),
            Integer.toHexString(lowerOfs[0]),
            Integer.toHexString(lowerOfs[numZones - 1]));

        int wallZones = 0, wallEntries = 0;

        for (int zi = 0; zi < numZones; zi++) {
            int ofs = lowerOfs[zi];
            if (ofs <= 0 || ofs + 2 > graphRaw.length) continue;

            List<WallRenderEntry> entries = new ArrayList<>();
            buf.position(ofs);
            buf.getShort(); // zone_id WORD

            for (int k = 0; k < MAX_RECORDS_PER_ZONE; k++) {
                if (buf.remaining() < 2) break;

                int wallId   = buf.get() & 0xFF; // HIGH byte
                int typeByte = buf.get() & 0xFF; // LOW byte

                // ── Fin de flux ────────────────────────────────────────────
                if (typeByte >= TYPE_END_MIN) break;

                // ── Mur (type 0) : 28 bytes supplementaires ─────────────
                if (typeByte == TYPE_WALL) {
                    if (buf.remaining() < 28) break;
                    int leftPt   = buf.getShort() & 0xFFFF;
                    int rightPt  = buf.getShort() & 0xFFFF;
                    int whichL   = buf.get() & 0xFF;
                    int whichR   = buf.get() & 0xFF;
                    int flags    = buf.getShort() & 0xFFFF;
                    int fromTile = buf.getShort() & 0xFFFF;
                    int yOffset  = buf.getShort() & 0xFFFF;
                    int texIdx   = buf.getShort() & 0xFFFF;
                    int hMask    = buf.get() & 0xFF;
                    int hShift   = buf.get() & 0xFF;
                    int wMask    = buf.get() & 0xFF;
                    int whichPbr = buf.get() & 0xFF;
                    int topWall  = buf.getInt();
                    int botWall  = buf.getInt();
                    int brightO  = buf.get();
                    int otherZ   = buf.get() & 0xFF;

                    entries.add(new WallRenderEntry(
                        typeByte, wallId,
                        leftPt, rightPt,
                        whichL, whichR,
                        flags, fromTile, yOffset,
                        texIdx, hMask, hShift, wMask, whichPbr,
                        topWall, botWall,
                        brightO, otherZ));
                    wallEntries++;
                    continue;
                }

                // ── Sol / plafond (type 1 ou 2) : taille variable ────────
                // Structure apres le type WORD :
                //   WORD  floorY
                //   WORD  sides_minus_1  (N = sides_minus_1 + 1)
                //   N * WORD  side_indices
                //   WORD  skip (inconnu)
                //   WORD  scaleval
                //   WORD  whichtile
                //   WORD  lighttype
                // Total apres type WORD = 12 + N*2 bytes
                if (typeByte == TYPE_FLOOR || typeByte == TYPE_CEILING) {
                    if (buf.remaining() < 4) break;
                    buf.getShort(); // floorY (ignore)
                    int sidesMinus1 = buf.getShort() & 0xFFFF;
                    int N = sidesMinus1 + 1;

                    // Sanite check sur N
                    if (N < 1 || N > MAX_FLOOR_SIDES) {
                        log.debug("Zone {} : floor record N={} suspect, arret", zi, N);
                        break;
                    }
                    if (buf.remaining() < N * 2 + 8) break;

                    // Sauter les N indices de points
                    buf.position(buf.position() + N * 2);

                    // 10 bytes restants : [2 skip][2 scaleval][2 whichtile][2 lighttype][2?]
                    // En pratique : [2 skip][2 scaleval][2 whichtile][2 lighttype] = 8 bytes
                    // On skippe 2 + scaleval (2) = 4 puis lit whichtile (2) puis skip lighttype (2)
                    buf.getShort(); // skip
                    buf.getShort(); // scaleval (ignore pour l'instant)
                    int whichTile = buf.getShort() & 0xFFFF; // whichtile ← valeur cle
                    buf.getShort(); // lighttype (ignore)

                    if (typeByte == TYPE_FLOOR) {
                        entries.add(WallRenderEntry.makeFloor(whichTile));
                    } else {
                        entries.add(WallRenderEntry.makeCeil(whichTile));
                    }
                    continue;
                }

                // ── Types inconnus (objet, eau, backdrop...) ─────────────
                // On ne connait pas leur taille → on arrete le parsing de cette zone
                // (ils apparaissent generalement apres tous les murs et sols)
                log.debug("Zone {} : type inconnu {} @ 0x{}, arret parsing",
                    zi, typeByte, Integer.toHexString(buf.position() - 2));
                break;
            }

            if (!entries.isEmpty()) {
                result[zi] = entries.toArray(new WallRenderEntry[0]);
                wallZones++;
            }
        }

        log.info("ZoneGraphAdds parse : {} zones, {} entrees mur", wallZones, wallEntries);
        return result;
    }

    /** Extrait le whichTile floor (type=1) par zone. */
    public static int[] extractFloorWhichTiles(WallRenderEntry[][] zoneEntries) {
        int[] result = new int[zoneEntries.length];
        Arrays.fill(result, -1);
        for (int zi = 0; zi < zoneEntries.length; zi++)
            for (WallRenderEntry e : zoneEntries[zi])
                if (e.isFloorRecord()) { result[zi] = e.floorWhichTile; break; }
        return result;
    }

    /** Extrait le whichTile ceil (type=2) par zone. */
    public static int[] extractCeilWhichTiles(WallRenderEntry[][] zoneEntries) {
        int[] result = new int[zoneEntries.length];
        Arrays.fill(result, -1);
        for (int zi = 0; zi < zoneEntries.length; zi++)
            for (WallRenderEntry e : zoneEntries[zi])
                if (e.isCeilRecord()) { result[zi] = e.floorWhichTile; break; }
        return result;
    }

    /** Collecte les tex indices uniques utilises. */
    public static Set<Integer> collectTexIndices(WallRenderEntry[][] zoneEntries) {
        Set<Integer> indices = new TreeSet<>();
        for (WallRenderEntry[] entries : zoneEntries)
            for (WallRenderEntry e : entries)
                if (e.isWall() && e.texIndex >= 0 && e.texIndex < 16)
                    indices.add(e.texIndex);
        return indices;
    }
}
