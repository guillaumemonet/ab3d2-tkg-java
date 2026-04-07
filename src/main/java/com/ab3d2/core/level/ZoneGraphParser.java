package com.ab3d2.core.level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Parse la section ZoneGraphAdds de twolev.graph.bin pour extraire
 * les donnees de rendu par zone (textures, hauteurs, points des murs).
 *
 * <h2>Localisation dans graph.bin</h2>
 * <pre>
 * graph.bin+0   : Header TLGT (16 bytes : 4 × ULONG)
 * graph.bin+16  : Table de pointeurs de zones (numZones × 4 bytes)
 *                 → offsets dans twolev.bin pour les ZoneT structs
 * graph.bin+ZoneGraphAddsOffset (= 16 + numZones*4) :
 *                 Table ZoneGraphAdds (numZones × 4 bytes)
 *                 → offsets dans graph.bin pour les donnees de rendu
 * graph.bin+renderData :
 *                 Sequences de 30-byte WallRenderEntry par zone
 *                 Terminees quand type_byte >= 128
 * </pre>
 *
 * <h2>Acces (depuis orderzones.s)</h2>
 * <pre>
 *   move.l Lvl_ZoneGraphAddsPtr_l, a1
 *   move.l (a1, zone_id*4), a1     ; offset dans graph.bin
 *   add.l Lvl_GraphicsPtr_l, a1    ; + base graph.bin
 *   ; a1 pointe maintenant sur les WallRenderEntry de cette zone
 * </pre>
 */
public class ZoneGraphParser {

    private static final Logger log = LoggerFactory.getLogger(ZoneGraphParser.class);

    /** Nombre maximum d'entrees par zone (securite contre boucle infinie). */
    private static final int MAX_ENTRIES_PER_ZONE = 256;

    /**
     * Parse la ZoneGraphAdds depuis graph.bin et retourne les listes
     * de WallRenderEntry indexees par zone_id.
     *
     * @param graphRaw    contenu brut de twolev.graph.bin
     * @param numZones    nombre de zones (depuis TLBT)
     * @param zoneGraphAddsOffset  offset dans graph.bin de la table ZoneGraphAdds
     *                             (= TLGT_ZoneGraphAddsOffset_l, ex. 0x228 pour LEVEL_A)
     * @return tableau de listes : result[zone_id] = liste de WallRenderEntry (jamais null)
     */
    public WallRenderEntry[][] parse(byte[] graphRaw, int numZones, int zoneGraphAddsOffset) {

        WallRenderEntry[][] result = new WallRenderEntry[numZones][];
        for (int i = 0; i < numZones; i++) result[i] = new WallRenderEntry[0];

        if (zoneGraphAddsOffset <= 0 || zoneGraphAddsOffset + numZones * 4 > graphRaw.length) {
            log.warn("ZoneGraphAdds offset invalide : 0x{} (graphRaw={}b)",
                Integer.toHexString(zoneGraphAddsOffset), graphRaw.length);
            return result;
        }

        ByteBuffer buf = ByteBuffer.wrap(graphRaw).order(ByteOrder.BIG_ENDIAN);

        // ── Lire la table d'offsets ─────────────────────────────────────────
        buf.position(zoneGraphAddsOffset);
        int[] renderOffsets = new int[numZones];
        for (int i = 0; i < numZones; i++) {
            renderOffsets[i] = buf.getInt();
        }

        log.debug("ZoneGraphAdds : {} zones @ 0x{}, renderOfs[0]=0x{}, renderOfs[last]=0x{}",
            numZones,
            Integer.toHexString(zoneGraphAddsOffset),
            Integer.toHexString(renderOffsets[0]),
            Integer.toHexString(renderOffsets[numZones - 1]));

        // ── Parser les entrees de rendu pour chaque zone ────────────────────
        int wallZones = 0, wallEntries = 0;

        for (int zi = 0; zi < numZones; zi++) {
            int ofs = renderOffsets[zi];
            if (ofs <= 0 || ofs + WallRenderEntry.BYTE_SIZE > graphRaw.length) {
                continue;
            }

            List<WallRenderEntry> entries = new ArrayList<>();
            buf.position(ofs);
            // Les donnees de rendu commencent par un WORD zone_id
            // (lu par draw_RenderCurrentZone avant la boucle d'entrees)
            // Il faut le sauter pour pointer sur la premiere entree
            buf.getShort(); // zone_id word (ignore ici)

            for (int k = 0; k < MAX_ENTRIES_PER_ZONE; k++) {
                if (buf.remaining() < WallRenderEntry.BYTE_SIZE) break;

                int typeByte = buf.get() & 0xFF;   // +0 type (unsigned)
                int wallId   = buf.get() & 0xFF;   // +1 wall_id

                // Fin de liste : type_byte >= 128
                if (typeByte >= WallRenderEntry.TYPE_END) {
                    // Consommer le reste de l'entree de 30b (28 octets restants)
                    if (buf.remaining() >= 28) buf.position(buf.position() + 28);
                    break;
                }

                // Lire les 28 bytes restants de l'entree
                int leftPt   = buf.getShort() & 0xFFFF;  // +2
                int rightPt  = buf.getShort() & 0xFFFF;  // +4
                int whichL   = buf.get() & 0xFF;          // +6
                int whichR   = buf.get() & 0xFF;          // +7
                int flags    = buf.getShort() & 0xFFFF;  // +8
                int fromTile = buf.getShort() & 0xFFFF;  // +10
                int yOffset  = buf.getShort() & 0xFFFF;  // +12
                int texIdx   = buf.getShort() & 0xFFFF;  // +14
                int hMask    = buf.get() & 0xFF;          // +16
                int hShift   = buf.get() & 0xFF;          // +17
                int wMask    = buf.get() & 0xFF;          // +18
                int whichPbr = buf.get() & 0xFF;          // +19
                int topWall  = buf.getInt();               // +20
                int botWall  = buf.getInt();               // +24
                int brightO  = buf.get();                  // +28 (signed pour brightness)
                int otherZ   = buf.get() & 0xFF;          // +29

                WallRenderEntry entry = new WallRenderEntry(
                    typeByte, wallId,
                    leftPt, rightPt,
                    whichL, whichR,
                    flags, fromTile, yOffset,
                    texIdx,
                    hMask, hShift, wMask, whichPbr,
                    topWall, botWall,
                    brightO, otherZ);

                entries.add(entry);
                if (typeByte == WallRenderEntry.TYPE_WALL) wallEntries++;
            }

            if (!entries.isEmpty()) {
                result[zi] = entries.toArray(new WallRenderEntry[0]);
                wallZones++;
            }
        }

        log.info("ZoneGraphAdds parse : {} zones avec donnees, {} entrees mur",
            wallZones, wallEntries);
        return result;
    }

    /**
     * Collecte les indices de textures uniques utilises dans un niveau.
     * Utile pour diagnostiquer le mapping texture_index → fichier.
     */
    public static Set<Integer> collectTexIndices(WallRenderEntry[][] zoneEntries) {
        Set<Integer> indices = new TreeSet<>();
        for (WallRenderEntry[] entries : zoneEntries) {
            for (WallRenderEntry e : entries) {
                if (e.isWall() && e.texIndex >= 0 && e.texIndex < 16) {
                    indices.add(e.texIndex);
                }
            }
        }
        return indices;
    }
}
