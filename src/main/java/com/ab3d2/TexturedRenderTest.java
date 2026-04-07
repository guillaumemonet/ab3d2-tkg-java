package com.ab3d2;

import com.ab3d2.assets.*;
import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;
import com.ab3d2.render.Camera;
import com.ab3d2.render.TexturedRenderer3D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * Test standalone : genere des PNG avec textures pour LEVEL_A.
 *
 * Lancement :
 *   gradle run --main-class=com.ab3d2.TexturedRenderTest
 *
 * Sorties : build/textured_test/
 */
public class TexturedRenderTest {

    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String OUTPUT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/textured_test";

    public static void main(String[] args) throws Exception {
        banner("Textured Render Test - LEVEL_A");

        Path root   = Path.of(RESOURCES);
        Path outDir = Path.of(OUTPUT);
        Files.createDirectories(outDir);

        // ── Tables ────────────────────────────────────────────────────────────
        Path bigsine = root.resolve("bigsine");
        if (Files.exists(bigsine)) Tables.init(bigsine);
        else Tables.initFromBytes(new byte[0]);

        // ── Niveau ───────────────────────────────────────────────────────────
        Path binPath   = root.resolve("levels/LEVEL_A/twolev.bin");
        Path graphPath = root.resolve("levels/LEVEL_A/twolev.graph.bin");
        LevelData level = new GraphicsBinaryParser().load(binPath, graphPath, "A");

        System.out.printf("Niveau A : %d zones, %d edges, %d pts%n%n",
            level.numZones(), level.numEdges(), level.numPoints());

        // ── ZoneGraphAdds ────────────────────────────────────────────────────
        byte[] graphRaw = Files.readAllBytes(graphPath);
        ByteBuffer gBuf = ByteBuffer.wrap(graphRaw).order(java.nio.ByteOrder.BIG_ENDIAN);
        gBuf.getInt(); gBuf.getInt(); gBuf.getInt();
        int zoneGraphAddsOffset = gBuf.getInt();

        ZoneGraphParser zgp = new ZoneGraphParser();
        WallRenderEntry[][] entries = zgp.parse(graphRaw, level.numZones(), zoneGraphAddsOffset);

        // Statistiques
        Set<Integer> usedIndices = ZoneGraphParser.collectTexIndices(entries);
        int totalWalls = 0;
        for (WallRenderEntry[] ze : entries)
            for (WallRenderEntry e : ze) if (e.isWall()) totalWalls++;
        System.out.printf("ZoneGraphAdds : %d entrees mur, texIndices=%s%n%n",
            totalWalls, usedIndices);

        // Afficher les 5 premiers murs de la zone de depart
        int startZone = level.plr1StartZoneId;
        System.out.printf("Zone %d (depart joueur) - premiers murs :%n", startZone);
        if (startZone < entries.length) {
            int n = 0;
            for (WallRenderEntry e : entries[startZone]) {
                if (!e.isWall()) continue;
                System.out.printf("  %s%n", e);
                if (++n >= 5) break;
            }
        }
        System.out.println();

        // ── Palette ──────────────────────────────────────────────────────────
        int[] palette = loadPalette(root);
        System.out.printf("Palette : %d couleurs chargee%n", palette.length);

        // ── Textures ─────────────────────────────────────────────────────────
        WallTextureManager texMgr = new WallTextureManager();
        Path wallsDir = root.resolve("walls");
        if (Files.exists(wallsDir)) {
            texMgr.loadAll(wallsDir, palette);
            System.out.println(texMgr.dump());
        } else {
            System.out.println("WARN: dossier walls/ absent, textures de fallback");
        }

        // ── Renderer ─────────────────────────────────────────────────────────
        TexturedRenderer3D renderer = new TexturedRenderer3D(
            Camera.SCREEN_W, Camera.SCREEN_H, texMgr);

        ZoneData startZoneData = level.zone(startZone);
        float eyeH = (startZoneData != null)
            ? startZoneData.floorHeight() - Camera.PLR_EYE_ABOVE_FLOOR
            : -Camera.PLR_EYE_ABOVE_FLOOR;

        float plrX = level.plr1StartX;
        float plrZ = level.plr1StartZ;

        // ── Vues de test ─────────────────────────────────────────────────────
        section("Rendu de 8 vues depuis la position de depart");

        // Note : ANGLE_MAX reel = 8192 (North=0, East=2048, South=4096, West=6144)
        // PlayerState.ANGLE_MAX=4096 est probablement faux -> a corriger separement
        // Pour ce test on utilise les angles 0..4095 qui donnent deja des vues coherentes
        int[] angles = {0, 512, 1024, 1536, 2048, 2560, 3072, 3584};
        String[] dirs = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

        int ok = 0, err = 0;
        for (int i = 0; i < angles.length; i++) {
            Camera cam = new Camera(plrX, plrZ, eyeH, angles[i]);
            renderer.render(level, entries, cam, startZone);
            String name = String.format("view_%s_a%04d", dirs[i], angles[i]);
            if (savePNG(renderer.getPixels(), Camera.SCREEN_W, Camera.SCREEN_H,
                        outDir.resolve(name + ".png"), cam)) ok++; else err++;
        }

        // Vue d'avance en ligne droite
        section("5 positions en avancant vers le Nord");
        for (int step = 0; step < 5; step++) {
            float posZ = plrZ + step * 60;
            Camera cam = new Camera(plrX, posZ, eyeH, 0);
            renderer.render(level, entries, cam, startZone);
            String name = String.format("advance_step%d_z%d", step, (int)posZ);
            if (savePNG(renderer.getPixels(), Camera.SCREEN_W, Camera.SCREEN_H,
                        outDir.resolve(name + ".png"), cam)) ok++; else err++;
        }

        section("Bilan");
        System.out.printf("PNG generes : %d OK, %d erreurs%n", ok, err);
        System.out.printf("Dossier     : %s%n", outDir.toAbsolutePath());
        banner("FIN");
    }

    // ── Charger la palette ────────────────────────────────────────────────────

    static int[] loadPalette(Path root) throws IOException {
        for (String candidate : new String[]{"palette.bin", "pal.bin", "palette"}) {
            Path p = root.resolve(candidate);
            if (Files.exists(p)) {
                byte[] raw = Files.readAllBytes(p);
                int[] pal  = parsePaletteAmiga(raw);
                System.out.printf("Palette : %d couleurs depuis %s (%d bytes)%n",
                    pal.length, p.getFileName(), raw.length);
                return pal;
            }
        }
        System.out.println("WARN: palette.bin introuvable, utilisation de niveaux de gris");
        int[] pal = new int[256];
        for (int i = 0; i < 256; i++) pal[i] = 0xFF000000 | (i << 16) | (i << 8) | i;
        return pal;
    }

    /**
     * Parse la palette Amiga AGA :
     *   1536 bytes = 256 couleurs x 3 WORDs big-endian (R, G, B)
     *   screen.c : gun = draw_Palette_vw[c], couleur_8bit = gun >> 8 = HIGH byte
     */
    static int[] parsePaletteAmiga(byte[] raw) {
        int[] pal = new int[256];
        if (raw.length == 1536) {
            // WORD = 0x00CC : HIGH byte = 0x00, LOW byte = valeur couleur 8-bit
            for (int i = 0; i < 256; i++) {
                int base = i * 6;
                int r = raw[base + 1] & 0xFF;  // LOW byte WORD R
                int g = raw[base + 3] & 0xFF;  // LOW byte WORD G
                int b = raw[base + 5] & 0xFF;  // LOW byte WORD B
                pal[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        } else if (raw.length >= 768) {
            for (int i = 0; i < 256; i++) {
                int r = raw[i*3    ] & 0xFF;
                int g = raw[i*3 + 1] & 0xFF;
                int b = raw[i*3 + 2] & 0xFF;
                pal[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        } else {
            for (int i = 0; i < 256; i++)
                pal[i] = 0xFF000000 | (i << 16) | (i << 8) | i;
        }
        return pal;
    }

    static int[] parsePaletteRGB(byte[] raw) {
        return parsePaletteAmiga(raw);
    }

    // ── Sauvegarde PNG ────────────────────────────────────────────────────────

    static boolean savePNG(int[] pixels, int w, int h, Path path, Camera cam) {
        try {
            // Upscale ×2 pour lisibilite
            BufferedImage img = new BufferedImage(w*2, h*2, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int col = pixels[y * w + x];
                    img.setRGB(x*2,   y*2,   col);
                    img.setRGB(x*2+1, y*2,   col);
                    img.setRGB(x*2,   y*2+1, col);
                    img.setRGB(x*2+1, y*2+1, col);
                }
            ImageIO.write(img, "PNG", path.toFile());
            System.out.printf("  OK   %-40s  [cam: %s]%n", path.getFileName(), cam);
            return true;
        } catch (IOException e) {
            System.out.printf("  ERR  %s : %s%n", path.getFileName(), e.getMessage());
            return false;
        }
    }

    static void section(String t) { System.out.println("\n--- " + t + " ---"); }
    static void banner(String t) {
        System.out.println("\n" + "=".repeat(55) + "\n  " + t + "\n" + "=".repeat(55) + "\n");
    }
}
