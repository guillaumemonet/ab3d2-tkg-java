package com.ab3d2;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;

/**
 * Compare palette.bin (grise) vs 256pal (vraie palette jeu)
 * et genere les textures avec la bonne palette.
 *
 *   gradle run --main-class=com.ab3d2.PaletteDiag
 */
public class PaletteDiag {

    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String ORIGINAL =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-original/media/includes";
    static final String OUTPUT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/wad_png";

    static final int SHADE_ROWS    = 32;
    static final int SHADE_ENTRIES = 32;
    static final int SHADE_BYTES   = 2048;

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of(OUTPUT);
        Files.createDirectories(outDir);

        // Charger les deux palettes
        byte[] palOld = Files.readAllBytes(Path.of(RESOURCES, "palette.bin"));
        byte[] palNew = Files.readAllBytes(Path.of(ORIGINAL, "256pal"));

        System.out.println("palette.bin : " + palOld.length + " bytes");
        System.out.println("256pal      : " + palNew.length + " bytes");

        // Dump premiers bytes des deux pour comparer
        System.out.println("\nComparaison premiers bytes (color[0..7], format LOW byte) :");
        System.out.printf("%-6s  %-22s  %-22s%n", "Index", "palette.bin RGB", "256pal RGB");
        System.out.println("-".repeat(60));
        for (int i = 0; i < 16 && i*6+5 < Math.min(palOld.length, palNew.length); i++) {
            int base = i * 6;
            int rO = palOld[base+1]&0xFF, gO = palOld[base+3]&0xFF, bO = palOld[base+5]&0xFF;
            int rN = palNew[base+1]&0xFF, gN = palNew[base+3]&0xFF, bN = palNew[base+5]&0xFF;
            System.out.printf("  [%3d]  (%3d,%3d,%3d)              (%3d,%3d,%3d)%n",
                i, rO, gO, bO, rN, gN, bN);
        }

        // Textures a tester
        String[] testFiles = {"brownspeakers", "hullmetal", "brownpipes", "alienredwall"};
        Path wallsDir = Path.of(RESOURCES, "walls");

        for (String name : testFiles) {
            Path wadPath = wallsDir.resolve(name + ".256wad");
            if (!Files.exists(wadPath)) continue;
            byte[] wad = Files.readAllBytes(wadPath);

            int[] dims = detectDims(wad.length);
            if (dims == null) { System.out.println("Skip " + name + " (dims inconnues)"); continue; }
            int W = dims[0], H = dims[1];

            // Decoder texels bruts
            int numGroups = (W + 2) / 3;
            int[][] texels = new int[H][W];
            int pos = SHADE_BYTES;
            for (int g = 0; g < numGroups && pos+1 < wad.length-2; g++) {
                int baseX = g * 3;
                for (int y = 0; y < H && pos+1 < wad.length-2; y++, pos += 2) {
                    int word = ((wad[pos]&0xFF)<<8)|(wad[pos+1]&0xFF);
                    if (baseX   < W) texels[y][baseX  ] =  word       &0x1F;
                    if (baseX+1 < W) texels[y][baseX+1] = (word>>5)   &0x1F;
                    if (baseX+2 < W) texels[y][baseX+2] = (word>>10)  &0x1F;
                }
            }

            // Generer 3 images : gris brut | avec palette.bin | avec 256pal
            int SCALE = 3;
            int iW = W * SCALE, iH = H * SCALE;
            String[] labels = {"gray_raw", "palette.bin", "256pal (ORIG)"};
            byte[][] pals = {null, palOld, palNew};

            int GAP = 6;
            BufferedImage composite = new BufferedImage(iW*3 + GAP*2, iH + 18,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = composite.createGraphics();
            gc.setColor(new Color(30,30,30));
            gc.fillRect(0, 0, composite.getWidth(), composite.getHeight());

            for (int v = 0; v < 3; v++) {
                BufferedImage img = new BufferedImage(iW, iH, BufferedImage.TYPE_INT_ARGB);
                byte[] palBytes = pals[v];

                for (int y = 0; y < H; y++) {
                    for (int x = 0; x < W; x++) {
                        int t = texels[y][x]; // 0-31
                        int argb;
                        if (v == 0) {
                            // Gris brut
                            int g2 = t * 8;
                            argb = 0xFF000000 | (g2<<16) | (g2<<8) | g2;
                        } else {
                            // Row 0 = BRIGHTEST (confirme par shade table image)
                            int sOff = 0 * SHADE_ENTRIES * 2 + t * 2;
                            int palIdx = wad[sOff] & 0xFF; // HIGH byte
                            int pBase = palIdx * 6;
                            if (pBase+5 < palBytes.length) {
                                int r = palBytes[pBase+1]&0xFF; // LOW byte WORD R
                                int g2= palBytes[pBase+3]&0xFF; // LOW byte WORD G
                                int b = palBytes[pBase+5]&0xFF; // LOW byte WORD B
                                argb = 0xFF000000 | (r<<16) | (g2<<8) | b;
                            } else {
                                argb = 0xFFFF00FF;
                            }
                        }
                        for (int py = 0; py < SCALE; py++)
                            for (int px = 0; px < SCALE; px++)
                                img.setRGB(x*SCALE+px, y*SCALE+py, argb);
                    }
                }

                int xOff = v * (iW + GAP);
                gc.drawImage(img, xOff, 18, null);
                gc.setColor(Color.WHITE);
                gc.setFont(new Font("Monospaced", Font.BOLD, 11));
                gc.drawString(labels[v], xOff + 3, 12);
            }
            gc.dispose();

            Path out = outDir.resolve("compare_" + name + ".png");
            ImageIO.write(composite, "PNG", out.toFile());
            System.out.println("-> " + out.getFileName());
        }

        // Si 256pal est meilleure, la copier vers resources
        System.out.println();
        System.out.println("Si les couleurs 256pal sont correctes, copier avec :");
        System.out.println("  cp media/includes/256pal src/main/resources/palette.bin");
        System.out.println("  (ou renommer en 256pal.bin et charger depuis le code)");

        // Copie automatique si les deux fichiers different
        if (!java.util.Arrays.equals(palOld, palNew)) {
            System.out.println();
            System.out.println("Les fichiers sont DIFFERENTS. Copie de 256pal -> resources/256pal.bin");
            Path dest = Path.of(RESOURCES, "256pal.bin");
            Files.copy(Path.of(ORIGINAL, "256pal"), dest,
                StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copie effectuee : " + dest);
        } else {
            System.out.println();
            System.out.println("Les fichiers sont IDENTIQUES - palette.bin est deja la bonne.");
        }
    }

    static int[] detectDims(int size) {
        int chunk = size - SHADE_BYTES - 2;
        if (chunk <= 0) chunk = size - SHADE_BYTES;
        int[][] cands = {{256,128},{256,64},{128,32},{128,128},{64,64}};
        for (int[] d : cands) {
            int g = (d[0]+2)/3;
            if (g*d[1]*2 == chunk) return d;
        }
        for (int w = 8; w <= 512; w++) {
            int g = (w+2)/3;
            if (g > 0 && chunk%(g*2)==0) {
                int h = chunk/(g*2);
                if (h>=8 && h<=512) return new int[]{w,h};
            }
        }
        return null;
    }
}
