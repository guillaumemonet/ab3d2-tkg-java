package com.ab3d2;

import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;
import com.ab3d2.render.Camera;
import com.ab3d2.render.WireRenderer3D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

/**
 * Test standalone du renderer 3D filaire.
 * Genere plusieurs PNG de test depuis differentes positions/angles.
 *
 * Lancement :
 *   gradle run --main-class=com.ab3d2.Renderer3DTest
 *
 * Sorties : build/renderer_test_*.png
 */
public class Renderer3DTest {

    static final String RESOURCES_ROOT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String OUTPUT_DIR =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/renderer_test";

    public static void main(String[] args) throws Exception {
        banner("Renderer3D Test - LEVEL_A");

        // ── 1. Charger les tables maths ───────────────────────────────────────
        Path root = Path.of(RESOURCES_ROOT);
        Path bigsine = root.resolve("bigsine");
        if (Files.exists(bigsine)) {
            Tables.init(bigsine);
            System.out.println("Tables chargees depuis bigsine (" + Files.size(bigsine) + " bytes)");
        } else {
            Tables.initFromBytes(new byte[0]);
            System.out.println("Tables synthetiques (bigsine absent)");
        }
        System.out.println(Tables.dumpValidation());

        // ── 2. Charger le niveau ─────────────────────────────────────────────
        LevelData level = new GraphicsBinaryParser().load(
            root.resolve("levels/LEVEL_A/twolev.bin"),
            root.resolve("levels/LEVEL_A/twolev.graph.bin"),
            "A");
        System.out.printf("Niveau A charge : %d zones, %d edges, %d pts%n",
            level.numZones(), level.numEdges(), level.numPoints());

        // ── 3. Repertoire de sortie ───────────────────────────────────────────
        Path outDir = Path.of(OUTPUT_DIR);
        Files.createDirectories(outDir);
        System.out.println("Sortie : " + outDir.toAbsolutePath());
        System.out.println();

        // ── 4. Creer le renderer ─────────────────────────────────────────────
        WireRenderer3D renderer = new WireRenderer3D(Camera.SCREEN_W, Camera.SCREEN_H);

        // ── 5. Generer des vues de test ───────────────────────────────────────
        int errors = 0;

        // Position de depart officielle (plr1StartX/Z)
        float plrX = level.plr1StartX;
        float plrZ = level.plr1StartZ;
        int   plrZone = level.plr1StartZoneId;

        // Hauteur oeil : zone de depart
        ZoneData startZone = level.zone(plrZone);
        float eyeH = (startZone != null)
            ? startZone.floorHeight() - Camera.PLR_EYE_ABOVE_FLOOR
            : -Camera.PLR_EYE_ABOVE_FLOOR;

        System.out.printf("Position depart : (%.0f, %.0f) zone=%d eyeH=%.1f%n",
            plrX, plrZ, plrZone, eyeH);
        System.out.println();

        // Test 1 : 8 angles (Nord, NE, Est, SE, Sud, SO, Ouest, NO)
        section("TEST 1 : 8 vues depuis position de depart");
        int[] testAngles = {0, 512, 1024, 1536, 2048, 2560, 3072, 3584};
        String[] dirNames = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        for (int i = 0; i < testAngles.length; i++) {
            Camera cam = new Camera(plrX, plrZ, eyeH, testAngles[i]);
            renderer.render(level, cam, plrZone);
            String name = String.format("test1_%s_a%04d", dirNames[i], testAngles[i]);
            errors += savePNG(renderer.getPixels(), Camera.SCREEN_W, Camera.SCREEN_H,
                outDir.resolve(name + ".png"), cam);
        }

        // Test 2 : avancer dans la direction Nord (angle=0) sur 5 positions
        section("TEST 2 : avancer vers le Nord (5 positions)");
        for (int step = 0; step < 5; step++) {
            float posZ = plrZ + step * 50;  // avance de 50 unites
            // Trouver la zone pour cette position (approximatif)
            Camera cam = new Camera(plrX, posZ, eyeH, 0);
            renderer.render(level, cam, plrZone);
            String name = String.format("test2_north_step%d_z%d", step, (int)posZ);
            errors += savePNG(renderer.getPixels(), Camera.SCREEN_W, Camera.SCREEN_H,
                outDir.resolve(name + ".png"), cam);
        }

        // Test 3 : validation de la projection (point unique)
        section("TEST 3 : validation mathematique");
        validateProjection(level);

        // ── Bilan ─────────────────────────────────────────────────────────────
        section("BILAN");
        System.out.printf("Erreurs : %d%n", errors);
        System.out.println("PNG generes dans : " + outDir.toAbsolutePath());
        if (errors == 0) System.out.println("Tous les PNG generes avec succes !");

        banner("TEST TERMINE");
    }

    // ── Validation math ───────────────────────────────────────────────────────

    static void validateProjection(LevelData level) {
        ZoneData z3 = level.zone(3);
        if (z3 == null) { System.out.println("Zone 3 introuvable"); return; }

        float roofH  = z3.roofHeight();
        float floorH = z3.floorHeight();
        float eyeH   = floorH - Camera.PLR_EYE_ABOVE_FLOOR;

        System.out.printf("Zone 3 : floor=%.0f, roof=%.0f, eyeH=%.0f%n", floorH, roofH, eyeH);
        System.out.println();
        System.out.printf("%-10s  %-12s  %-12s  %-8s%n",
            "Dist(Z)", "screenY_top", "screenY_bot", "height_px");

        for (float d : new float[]{50, 100, 150, 200, 300, 500}) {
            float yTop = Camera.projectY(roofH, eyeH, d);
            float yBot = Camera.projectY(floorH, eyeH, d);
            int heightPx = Math.max(0, (int)(yBot - yTop));
            System.out.printf("%-10.0f  %-12.1f  %-12.1f  %-8d%n",
                d, yTop, yBot, heightPx);
        }
        System.out.println();
        System.out.println("(screenY < 80 = au-dessus du centre, > 80 = en-dessous)");
    }

    // ── Sauvegarde PNG ────────────────────────────────────────────────────────

    static int savePNG(int[] pixels, int w, int h, Path path, Camera cam) {
        try {
            BufferedImage img = new BufferedImage(
                w * 3, h * 3, BufferedImage.TYPE_INT_ARGB);  // x3 pour lisibilite
            // Upscale x3
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int col = pixels[y * w + x];
                    for (int dy = 0; dy < 3; dy++)
                        for (int dx = 0; dx < 3; dx++)
                            img.setRGB(x*3+dx, y*3+dy, col);
                }
            }
            ImageIO.write(img, "PNG", path.toFile());
            System.out.printf("  OK  %-50s  [cam: %s]%n", path.getFileName(), cam);
            return 0;
        } catch (IOException e) {
            System.out.printf("  ERR %-50s  %s%n", path.getFileName(), e.getMessage());
            return 1;
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    static void section(String title) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("  " + title);
        System.out.println("=".repeat(60));
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("*".repeat(60));
        System.out.printf("*  %-56s*%n", title);
        System.out.println("*".repeat(60));
        System.out.println();
    }
}
