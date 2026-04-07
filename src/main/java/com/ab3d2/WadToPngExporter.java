package com.ab3d2;

import com.ab3d2.assets.WallTextureExtractor;
import com.ab3d2.assets.WadTextureData;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;

/**
 * Exporte toutes les textures .256wad en PNG pour verification visuelle.
 *
 * Lancement :
 *   gradle run --main-class=com.ab3d2.WadToPngExporter
 *
 * Sortie : build/wad_png/
 *   - Une image PNG par texture, nom = nom du fichier .256wad
 *   - Image agrandie x4 pour mieux voir les details
 *   - Info : dimensions detectees affichees dans le nom de fichier
 *
 * Examine aussi la shade table : genere une image shade_table_NOM.png
 * montrant les 32 niveaux de luminosite x 32 entrees de couleur.
 */
public class WadToPngExporter {

    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String OUTPUT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/wad_png";

    static final int SCALE = 4;  // agrandissement x4 pour lisibilite

    public static void main(String[] args) throws Exception {
        System.out.println("=== WAD to PNG Exporter ===\n");

        Path wallsDir = Path.of(RESOURCES, "walls");
        Path outDir   = Path.of(OUTPUT);
        Files.createDirectories(outDir);

        // Charger la palette
        int[] palette = loadPalette(Path.of(RESOURCES));
        System.out.println("Palette : " + palette.length + " couleurs\n");

        WallTextureExtractor extractor = new WallTextureExtractor(palette);

        int ok = 0, fail = 0;

        try (var stream = Files.list(wallsDir)) {
            var files = stream
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".256wad"))
                .sorted()
                .toList();

            System.out.printf("%-32s  %-12s  %-10s  Status%n",
                "Fichier", "Dimensions", "Taille");
            System.out.println("-".repeat(75));

            for (Path wadPath : files) {
                String name = wadPath.getFileName().toString()
                    .replaceAll("(?i)\\.256wad$", "");
                long fileSize = Files.size(wadPath);

                try {
                    // Detecter les dimensions
                    int[] dims = WallTextureExtractor.detectDimensions((int) fileSize);
                    String detectedDims = dims != null
                        ? dims[0] + "x" + dims[1]
                        : "?x?";

                    // Decoder
                    WadTextureData tex = extractor.load(wadPath);

                    // Nom de sortie avec dimensions
                    String outName = String.format("%s_%dx%d.png",
                        name, tex.width(), tex.height());
                    Path outPath = outDir.resolve(outName);

                    // Sauvegarder la texture en PNG (x4)
                    saveTexturePng(tex, outPath);

                    // Sauvegarder aussi la shade table
                    Path shadePath = outDir.resolve("shade_" + name + ".png");
                    saveShadeTablePng(tex, shadePath);

                    System.out.printf("  %-30s  %-12s  %-10d  OK  -> %s%n",
                        name, tex.width() + "x" + tex.height(), fileSize, outName);
                    ok++;

                } catch (Exception e) {
                    System.out.printf("  %-30s  %-12s  %-10d  ERR: %s%n",
                        name, "?", fileSize, e.getMessage());
                    fail++;
                }
            }
        }

        System.out.println();
        System.out.printf("Exporte : %d OK, %d erreurs%n", ok, fail);
        System.out.printf("Dossier : %s%n", outDir.toAbsolutePath());
        System.out.println();
        System.out.println("Fichiers generes :");
        System.out.println("  NOM_WxH.png        - texture full-brightness x" + SCALE);
        System.out.println("  shade_NOM.png      - shade table (32 luminosites x 32 couleurs)");
    }

    // ── Export texture principale ─────────────────────────────────────────────

    static void saveTexturePng(WadTextureData tex, Path out) throws IOException {
        int w = tex.width(), h = tex.height();
        int[] src = tex.pixels();

        // Image agrandie x SCALE avec quadrillage optionnel
        BufferedImage img = new BufferedImage(w * SCALE, h * SCALE,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int col = src[y * w + x];
                g.setColor(new java.awt.Color(
                    (col >> 16) & 0xFF,
                    (col >>  8) & 0xFF,
                     col        & 0xFF));
                g.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
            }
        }
        g.dispose();
        ImageIO.write(img, "PNG", out.toFile());
    }

    // ── Export shade table ────────────────────────────────────────────────────

    /**
     * Genere une image de la shade table : 32 colonnes (texels 0-31) x 32 lignes (luminosite).
     * Ligne 0 = plus sombre, ligne 31 = full brightness.
     * Chaque cellule = SCALE x SCALE pixels.
     */
    static void saveShadeTablePng(WadTextureData tex, Path out) throws IOException {
        int rows    = WadTextureData.SHADE_ROWS;        // 32
        int entries = WadTextureData.ENTRIES_PER_ROW;   // 32
        int[] st    = tex.shadeTable();

        int cellW = SCALE * 4;  // cellules plus larges pour la shade table
        int cellH = SCALE * 2;
        BufferedImage img = new BufferedImage(entries * cellW, rows * cellH,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < entries; col++) {
                int argb = st[row * entries + col];
                g.setColor(new java.awt.Color(
                    (argb >> 16) & 0xFF,
                    (argb >>  8) & 0xFF,
                     argb        & 0xFF));
                g.fillRect(col * cellW, row * cellH, cellW, cellH);
            }
        }
        g.dispose();
        ImageIO.write(img, "PNG", out.toFile());
    }

    // ── Palette ───────────────────────────────────────────────────────────────

    static int[] loadPalette(Path root) throws IOException {
        // Priorite : 256pal.bin (vraie palette du jeu) > palette.bin
        for (String name : new String[]{"256pal.bin", "palette.bin", "pal.bin"}) {
            Path p = root.resolve(name);
            if (Files.exists(p)) {
                byte[] raw = Files.readAllBytes(p);
                int[] pal  = parsePaletteAmiga(raw);
                System.out.println("Palette : " + p.getFileName()
                    + " (" + raw.length + " bytes, " + pal.length + " couleurs)");
                return pal;
            }
        }
        // Fallback niveaux de gris
        System.out.println("WARN : palette.bin absent, niveaux de gris");
        int[] pal = new int[256];
        for (int i = 0; i < 256; i++)
            pal[i] = 0xFF000000 | (i << 16) | (i << 8) | i;
        return pal;
    }

    /**
     * Parse la palette Amiga AGA depuis draw_Palette_vw :
     *   1536 bytes = 768 WORDs big-endian = 256 couleurs x 3 composantes (R, G, B)
     *   screen.c : gun = draw_Palette_vw[c], couleur_8bit = gun >> 8 = HIGH byte du WORD
     *   Reference : screen.c Vid_LoadMainPalette()
     */
    static int[] parsePaletteAmiga(byte[] raw) {
        int[] pal = new int[256];
        if (raw.length < 6) return pal;

        if (raw.length == 1536) {
            // Format Amiga AGA : 256 x 3 WORDs big-endian (6 bytes/couleur)
            // WORD = 0x00CC : HIGH byte = 0x00 (toujours), LOW byte = valeur 8-bit couleur
            // Confirme par dump hex : HIGH bytes tous 0x00, LOW bytes = 0x00,0x08,0x10...
            for (int i = 0; i < 256; i++) {
                int base = i * 6;
                int r = raw[base + 1] & 0xFF;  // LOW byte du WORD R
                int g = raw[base + 3] & 0xFF;  // LOW byte du WORD G
                int b = raw[base + 5] & 0xFF;  // LOW byte du WORD B
                pal[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            return pal;
        }

        // Format PC : 256 x 3 bytes RGB bruts (768 bytes)
        if (raw.length >= 768) {
            for (int i = 0; i < 256; i++) {
                int r = raw[i*3    ] & 0xFF;
                int g = raw[i*3 + 1] & 0xFF;
                int b = raw[i*3 + 2] & 0xFF;
                pal[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            return pal;
        }

        // Fallback niveaux de gris
        for (int i = 0; i < 256; i++)
            pal[i] = 0xFF000000 | (i << 16) | (i << 8) | i;
        return pal;
    }
}
