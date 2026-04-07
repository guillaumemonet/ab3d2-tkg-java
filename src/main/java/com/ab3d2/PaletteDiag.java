package com.ab3d2;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;

/**
 * Diagnostic palette : essaie les 4 combinaisons HIGH/LOW pour shade table et palette,
 * plus mode "direct screen pixel" et niveaux de gris bruts.
 * Sauvegarde une image composite pour comparaison visuelle.
 *
 *   gradle run --main-class=com.ab3d2.PaletteDiag
 *   Sortie : build/wad_png/diag_NOMTEXTURE.png
 */
public class PaletteDiag {

    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String OUTPUT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/wad_png";

    static final int SHADE_ROWS    = 32;
    static final int SHADE_ENTRIES = 32;
    static final int SHADE_BYTES   = SHADE_ROWS * SHADE_ENTRIES * 2; // 2048

    public static void main(String[] args) throws Exception {
        Path root   = Path.of(RESOURCES);
        Path outDir = Path.of(OUTPUT);
        Files.createDirectories(outDir);

        byte[] pal = Files.readAllBytes(root.resolve("palette.bin"));
        System.out.println("palette.bin : " + pal.length + " bytes");

        // Dump premiers bytes de la palette
        System.out.println("palette.bin premiers bytes (hex) :");
        for (int i = 0; i < Math.min(48, pal.length); i++) {
            System.out.printf("%02X ", pal[i] & 0xFF);
            if ((i+1) % 6 == 0) System.out.printf(" <- color[%d]%n", i/6);
        }
        System.out.println();

        // Tester sur stonewall (petite taille)
        String[] testFiles = {"stonewall", "hullmetal", "brownpipes"};

        for (String name : testFiles) {
            Path wadPath = root.resolve("walls/" + name + ".256wad");
            if (!Files.exists(wadPath)) continue;
            byte[] wad = Files.readAllBytes(wadPath);
            System.out.println("=== " + name + " (" + wad.length + " bytes) ===");

            // Dump shade table row 31 (brightest)
            System.out.println("Shade table row 31 (brightest) - premiers WORDs :");
            int rowOff = 31 * SHADE_ENTRIES * 2;
            for (int e = 0; e < 16; e++) {
                int off = rowOff + e * 2;
                int h = wad[off] & 0xFF, l = wad[off+1] & 0xFF;
                System.out.printf("  [%2d] %02X %02X (high=%3d low=%3d)%n", e, h, l, h, l);
            }
            System.out.println();

            // Detecter les dimensions
            int[] dims = detectDims(wad.length);
            if (dims == null) { System.out.println("  skip: dims inconnues"); continue; }
            int W = dims[0], H = dims[1];

            // Decoder le chunk data en texels 5-bit bruts
            int numGroups = (W + 2) / 3;
            int[][] texels = new int[H][W]; // texels[y][x] = valeur 0-31
            int pos = SHADE_BYTES;
            for (int g = 0; g < numGroups && pos + 1 < wad.length - 2; g++) {
                int baseX = g * 3;
                for (int y = 0; y < H && pos + 1 < wad.length - 2; y++, pos += 2) {
                    int word = ((wad[pos] & 0xFF) << 8) | (wad[pos+1] & 0xFF);
                    if (baseX     < W) texels[y][baseX    ] =  word        & 0x1F;
                    if (baseX + 1 < W) texels[y][baseX + 1] = (word >>  5) & 0x1F;
                    if (baseX + 2 < W) texels[y][baseX + 2] = (word >> 10) & 0x1F;
                }
            }

            // Generer 6 variantes
            int SCALE = 3;
            int iW = W * SCALE, iH = H * SCALE;
            String[] labels = {
                "gray_raw",          // 0: texels bruts en gris
                "shade_H_pal_H",     // 1: shade HIGH byte -> palette HIGH byte
                "shade_H_pal_L",     // 2: shade HIGH byte -> palette LOW byte
                "shade_L_pal_H",     // 3: shade LOW byte  -> palette HIGH byte
                "shade_L_pal_L",     // 4: shade LOW byte  -> palette LOW byte
                "direct_LOW",        // 5: shade LOW byte direct comme niveau de gris
            };
            int N = labels.length;

            // Image composite : N colonnes cote a cote
            BufferedImage composite = new BufferedImage(iW * N + (N-1)*4, iH + 20,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = composite.createGraphics();
            gc.setColor(Color.DARK_GRAY);
            gc.fillRect(0, 0, composite.getWidth(), composite.getHeight());

            for (int v = 0; v < N; v++) {
                BufferedImage img = new BufferedImage(iW, iH, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < H; y++) {
                    for (int x = 0; x < W; x++) {
                        int t = texels[y][x]; // 0-31
                        int argb;
                        switch (v) {
                            case 0: { // gray raw
                                int g2 = t * 8;
                                argb = 0xFF000000 | (g2<<16) | (g2<<8) | g2;
                                break;
                            }
                            case 1: case 2: case 3: case 4: {
                                boolean shadeHigh = (v == 1 || v == 2);
                                boolean palHigh   = (v == 1 || v == 3);
                                int sOff = 31 * SHADE_ENTRIES * 2 + t * 2;
                                int palIdx = shadeHigh
                                    ? (wad[sOff]   & 0xFF)
                                    : (wad[sOff+1] & 0xFF);
                                int pBase = palIdx * 6;
                                if (pBase + 4 < pal.length) {
                                    int r = palHigh ? (pal[pBase+0]&0xFF) : (pal[pBase+1]&0xFF);
                                    int g2= palHigh ? (pal[pBase+2]&0xFF) : (pal[pBase+3]&0xFF);
                                    int b = palHigh ? (pal[pBase+4]&0xFF) : (pal[pBase+5]&0xFF);
                                    argb = 0xFF000000 | (r<<16) | (g2<<8) | b;
                                } else {
                                    argb = 0xFFFF00FF; // magenta = index hors limites
                                }
                                break;
                            }
                            case 5: default: { // direct LOW byte
                                int sOff = 31 * SHADE_ENTRIES * 2 + t * 2;
                                int val = wad[sOff+1] & 0xFF;
                                argb = 0xFF000000 | (val<<16) | (val<<8) | val;
                                break;
                            }
                        }
                        // Dessiner pixel (SCALE x SCALE)
                        for (int py = 0; py < SCALE; py++)
                            for (int px = 0; px < SCALE; px++)
                                img.setRGB(x*SCALE+px, y*SCALE+py, argb);
                    }
                }
                int xOff = v * (iW + 4);
                gc.drawImage(img, xOff, 20, null);
                // Label
                gc.setColor(Color.WHITE);
                gc.setFont(new Font("Monospaced", Font.PLAIN, 9));
                gc.drawString(labels[v], xOff + 2, 13);
            }
            gc.dispose();

            Path outPath = outDir.resolve("diag_" + name + ".png");
            ImageIO.write(composite, "PNG", outPath.toFile());
            System.out.println("  -> " + outPath);
            System.out.println();
        }

        System.out.println("Ouvrez les images 'diag_*.png' dans build/wad_png/");
        System.out.println("La variante qui montre la texture = bonne combinaison HIGH/LOW");
    }

    static int[] detectDims(int size) {
        int chunk = size - SHADE_BYTES - 2;
        if (chunk <= 0) chunk = size - SHADE_BYTES;
        int[][] candidates = {{256,128},{256,64},{128,32},{128,128},{64,64},{96,128}};
        for (int[] d : candidates) {
            int g = (d[0]+2)/3;
            if (g * d[1] * 2 == chunk) return d;
        }
        // Brute force
        for (int w = 8; w <= 512; w++) {
            int g = (w+2)/3;
            if (chunk % (g*2) == 0) {
                int h = chunk / (g*2);
                if (h >= 8 && h <= 512) return new int[]{w, h};
            }
        }
        return null;
    }
}
