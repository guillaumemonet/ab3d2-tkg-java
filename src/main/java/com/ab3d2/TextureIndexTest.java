package com.ab3d2;

import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

/**
 * Test standalone : parse les ZoneGraphAdds de LEVEL_A..P et affiche
 * les indices de textures utilises, les dimensions des murs, etc.
 *
 * Lancement :
 *   gradle run --main-class=com.ab3d2.TextureIndexTest
 */
public class TextureIndexTest {

    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";

    public static void main(String[] args) throws Exception {
        banner("Texture Index Test");
        Tables.initFromBytes(new byte[0]);
        Path root = Path.of(RESOURCES);
        ZoneGraphParser parser = new ZoneGraphParser();

        // Stats globales
        Map<Integer, Integer> globalTexCount = new TreeMap<>();
        Map<Integer, Set<String>> texInLevels = new TreeMap<>();

        for (char letter = 'A'; letter <= 'P'; letter++) {
            Path binPath   = root.resolve("levels/LEVEL_" + letter + "/twolev.bin");
            Path graphPath = root.resolve("levels/LEVEL_" + letter + "/twolev.graph.bin");
            if (!Files.exists(binPath) || !Files.exists(graphPath)) continue;

            byte[] graphRaw = Files.readAllBytes(graphPath);

            // Parser le header TLGT pour obtenir zoneGraphAddsOffset
            ByteBuffer gBuf = ByteBuffer.wrap(graphRaw).order(ByteOrder.BIG_ENDIAN);
            gBuf.getInt(); // doors
            gBuf.getInt(); // lifts
            gBuf.getInt(); // switches
            int zoneGraphAddsOffset = gBuf.getInt(); // offset table GraphAdds

            // Charger le niveau pour avoir numZones
            LevelData level = new GraphicsBinaryParser().load(binPath, graphPath,
                String.valueOf(letter));

            // Parser les entries de rendu
            WallRenderEntry[][] entries = parser.parse(graphRaw,
                level.numZones(), zoneGraphAddsOffset);

            // Compter les indices de textures
            Set<Integer> levelIndices = ZoneGraphParser.collectTexIndices(entries);
            int wallCount = 0, floorCount = 0, objCount = 0;

            for (WallRenderEntry[] ze : entries) {
                for (WallRenderEntry e : ze) {
                    if (e.isWall())   wallCount++;
                    if (e.isFloor())  floorCount++;
                    if (e.isObject()) objCount++;
                }
            }

            System.out.printf("LEVEL_%c  zones=%3d  walls=%4d  floors=%3d  objs=%3d  texIndices=%s%n",
                letter, level.numZones(), wallCount, floorCount, objCount, levelIndices);

            // Accumuler stats globales
            for (int idx : levelIndices) {
                globalTexCount.merge(idx, 1, Integer::sum);
                texInLevels.computeIfAbsent(idx, k -> new TreeSet<>())
                           .add(String.valueOf(letter));
            }

            // Pour LEVEL_A, detail des dimensions par texture
            if (letter == 'A') {
                section("Detail LEVEL_A : dimensions des textures par index");
                Map<Integer, Set<String>> dimsByIdx = new TreeMap<>();
                for (WallRenderEntry[] ze : entries) {
                    for (WallRenderEntry e : ze) {
                        if (!e.isWall()) continue;
                        String dims = e.texWidth() + "x" + e.texHeight();
                        dimsByIdx.computeIfAbsent(e.texIndex, k -> new TreeSet<>()).add(dims);
                    }
                }
                dimsByIdx.forEach((idx, dims) ->
                    System.out.printf("  texIndex=%2d  dimensions=%s%n", idx, dims));

                section("Detail LEVEL_A : 5 premiers murs de zone 0");
                if (entries.length > 0) {
                    int shown = 0;
                    for (WallRenderEntry e : entries[0]) {
                        if (!e.isWall()) continue;
                        System.out.printf("  %s%n", e);
                        if (++shown >= 5) break;
                    }
                }
            }
        }

        section("Resume global : textures par index sur tous niveaux");
        System.out.printf("  %-10s  %-8s  %-40s%n",
            "texIndex", "nLevels", "Levels");
        globalTexCount.forEach((idx, count) ->
            System.out.printf("  %-10d  %-8d  %s%n",
                idx, count, texInLevels.get(idx)));

        System.out.println();
        System.out.println("Indices utilises : " + globalTexCount.keySet());
        System.out.println("Fichiers .256wad disponibles (wallinc/) :");
        Path wallinc = Path.of(RESOURCES).resolve("walls");
        if (Files.exists(wallinc)) {
            Files.list(wallinc).filter(p -> p.toString().endsWith(".256wad"))
                 .sorted().forEach(p -> System.out.println("  " + p.getFileName()));
        } else {
            System.out.println("  (dossier walls/ absent dans resources)");
        }

        banner("FIN");
    }

    static void section(String t) {
        System.out.println("\n--- " + t + " ---");
    }
    static void banner(String t) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  " + t);
        System.out.println("=".repeat(60) + "\n");
    }
}
