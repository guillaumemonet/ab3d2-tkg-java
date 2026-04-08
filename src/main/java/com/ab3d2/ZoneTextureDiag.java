package com.ab3d2;

import com.ab3d2.core.level.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

/**
 * Diagnostique les textures utilisées par zone dans LEVEL_A (après fix stride-8).
 * gradle run --main-class=com.ab3d2.ZoneTextureDiag
 */
public class ZoneTextureDiag {

    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";

    public static void main(String[] args) throws Exception {
        Path root    = Path.of(RESOURCES);
        Path binPath = root.resolve("levels/LEVEL_A/twolev.bin");
        Path gPath   = root.resolve("levels/LEVEL_A/twolev.graph.bin");

        LevelData level = new GraphicsBinaryParser().load(binPath, gPath, "A");
        byte[] graphRaw = Files.readAllBytes(gPath);
        ByteBuffer gBuf = ByteBuffer.wrap(graphRaw).order(ByteOrder.BIG_ENDIAN);
        gBuf.getInt(); gBuf.getInt(); gBuf.getInt();
        int zgaOffset = gBuf.getInt();

        WallRenderEntry[][] entries = new ZoneGraphParser().parse(graphRaw, level.numZones(), zgaOffset);

        System.out.println("=== Textures par zone (LEVEL_A, après fix stride-8) ===");
        System.out.println("Joueur démarre en zone " + level.plr1StartZoneId);
        System.out.println();

        // Zone de départ + ses voisins PVS
        int startZone = level.plr1StartZoneId;
        Set<Integer> zonesToShow = new LinkedHashSet<>();
        zonesToShow.add(startZone);
        ZoneData z = level.zone(startZone);
        if (z != null) for (ZPVSRecord pvs : z.pvsRecords)
            zonesToShow.add(pvs.zoneId() & 0xFFFF);

        System.out.printf("%-8s  %-6s  %-6s  %s%n", "Zone", "Murs", "Portes", "TexIndices");
        System.out.println("-".repeat(60));

        for (int zid : zonesToShow) {
            if (zid >= entries.length) continue;
            WallRenderEntry[] ze = entries[zid];
            if (ze == null || ze.length == 0) { System.out.printf("  z%3d  (vide)%n", zid); continue; }

            Set<Integer> texIds = new TreeSet<>();
            int walls = 0, doors = 0;
            for (WallRenderEntry e : ze) {
                if (!e.isWall()) continue;
                walls++;
                texIds.add(e.texIndex);
                if (e.texIndex == 5) doors++; // chevrondoor
            }
            System.out.printf("  z%3d  %-6d  %-6d  %s%n", zid, walls, doors, texIds);
        }

        // Résumé global
        System.out.println();
        System.out.println("=== Distribution globale ===");
        int[] count = new int[256];
        for (WallRenderEntry[] ze : entries)
            for (WallRenderEntry e : ze)
                if (e.isWall()) count[Math.min(e.texIndex & 0xFFFF, 255)]++;

        System.out.printf("%-8s  %-8s  %s%n", "TexIdx", "Nb murs", "Nom");
        for (int i = 0; i < 16; i++)
            if (count[i] > 0)
                System.out.printf("  [%2d]   %-8d  %s%n", i, count[i], texName(i));
        // hors range
        for (int i = 16; i < 256; i++)
            if (count[i] > 0)
                System.out.printf("  [%d]  %-8d  (hors-range -> skipped)%n", i, count[i]);
    }

    static String texName(int i) {
        String[] NAMES = {
            "stonewall","brownpipes","hullmetal","technotritile",
            "brownspeakers","chevrondoor","technolights","redhullmetal",
            "alienredwall","gieger","rocky","steampunk","brownstonestep",
            "(vide)","(vide)","(vide)"
        };
        return i < NAMES.length ? NAMES[i] : "?";
    }
}
