package com.ab3d2;

import com.ab3d2.core.level.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * Affiche les floorNoise (ZoneData) vs whichtile (zone graph) pour LEVEL_A.
 *   gradle run --main-class=com.ab3d2.FloorNoiseDiag
 */
public class FloorNoiseDiag {
    static final String R =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";

    public static void main(String[] a) throws Exception {
        Path root = Path.of(R);
        Path bin  = root.resolve("levels/LEVEL_A/twolev.bin");
        Path gph  = root.resolve("levels/LEVEL_A/twolev.graph.bin");
        LevelData lv = new GraphicsBinaryParser().load(bin, gph, "A");

        byte[] graphRaw = Files.readAllBytes(gph);
        ByteBuffer gBuf = ByteBuffer.wrap(graphRaw).order(ByteOrder.BIG_ENDIAN);
        gBuf.getInt(); gBuf.getInt(); gBuf.getInt();
        int zga = gBuf.getInt();
        WallRenderEntry[][] entries = new ZoneGraphParser().parse(graphRaw, lv.numZones(), zga);
        int[] whichTiles     = ZoneGraphParser.extractFloorWhichTiles(entries);
        int[] ceilWhichTiles = ZoneGraphParser.extractCeilWhichTiles(entries);

        System.out.println("=== FloorNoise vs whichTile (floor+ceil) pour LEVEL_A ===");
        System.out.printf("Joueur demarre zone %d%n%n", lv.plr1StartZoneId);
        System.out.printf("%-6s  %-12s  %-12s  %-12s%n",
            "Zone", "floorNoise", "floorTile", "ceilTile");
        System.out.println("-".repeat(50));

        for (int i = 0; i < lv.numZones(); i++) {
            ZoneData z = lv.zone(i);
            if (z == null) continue;
            int ft = whichTiles[i];
            int ct = ceilWhichTiles[i];
            if (ft >= 0 || ct >= 0 || i == lv.plr1StartZoneId) {
                System.out.printf("%-6d  %-12d  %-12s  %-12s%n",
                    i, z.floorNoise,
                    ft >= 0 ? String.valueOf(ft) : "(none)",
                    ct >= 0 ? String.valueOf(ct) : "(none)");
            }
        }

        ZoneData start = lv.zone(lv.plr1StartZoneId);
        if (start != null) {
            System.out.printf("%nZone %d : floorNoise=%d  whichTile=%s  floor=%d  roof=%d%n",
                lv.plr1StartZoneId,
                start.floorNoise,
                whichTiles[lv.plr1StartZoneId] >= 0
                    ? String.valueOf(whichTiles[lv.plr1StartZoneId]) : "(aucun)",
                start.floorHeight(), start.roofHeight());
        }
    }
}
