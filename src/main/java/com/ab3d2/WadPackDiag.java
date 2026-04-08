package com.ab3d2;

import com.ab3d2.assets.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;

/**
 * Diagnostique le decodage des .256wad en comparant les 3 canaux PACK.
 *   gradle run --main-class=com.ab3d2.WadPackDiag
 *
 * Genere 4 PNG par texture :
 *   _normal.png    : decodage normal avec palette
 *   _pack0.png     : uniquement canal PACK0 (bits[4:0])  -> rouge
 *   _pack1.png     : uniquement canal PACK1 (bits[9:5])  -> vert
 *   _pack2.png     : uniquement canal PACK2 (bits[14:10]) -> bleu
 *   _gray.png      : brut 5-bit sans palette (verifie la distribution)
 */
public class WadPackDiag {

    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String OUTPUT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/obj/pack_diag";

    // Texture a diagnostiquer (changer ici si besoin)
    static final String[] TARGETS = { "brownspeakers", "chevrondoor", "stonewall", "hullmetal" };

    public static void main(String[] args) throws Exception {
        Path root   = Path.of(RESOURCES);
        Path outDir = Path.of(OUTPUT);
        Files.createDirectories(outDir);

        int[] palette = WadToPngExporter.loadPalette(root);

        for (String name : TARGETS) {
            Path wad = root.resolve("walls/" + name + ".256wad");
            if (!Files.exists(wad)) { System.out.println("SKIP " + name); continue; }

            byte[] raw  = Files.readAllBytes(wad);
            int[]  dims = WallTextureExtractor.detectDimensions(raw.length);
            if (dims == null) { System.out.println("SKIP " + name + " (dims)"); continue; }

            int texW = dims[0], texH = dims[1];
            System.out.printf("%-24s %4dx%-4d  %d bytes%n", name, texW, texH, raw.length);

            ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);

            // Lire shade table (2048 bytes)
            int SHADE_ROWS = 32, ENTRIES = 32;
            int[] shade = new int[SHADE_ROWS * ENTRIES];
            for (int r = 0; r < SHADE_ROWS; r++)
                for (int e = 0; e < ENTRIES; e++) {
                    int palIdx = buf.get() & 0xFF;
                    buf.get();  // LOW byte ignore
                    shade[r * ENTRIES + e] = palette[palIdx];
                }

            int[] normal = new int[texW * texH];
            int[] gray   = new int[texW * texH];
            int[] pack0  = new int[texW * texH]; // bits[4:0]
            int[] pack1  = new int[texW * texH]; // bits[9:5]
            int[] pack2  = new int[texW * texH]; // bits[14:10]

            int numGroups = (texW + 2) / 3;
            for (int g = 0; g < numGroups; g++) {
                int bx = g * 3;
                for (int y = 0; y < texH; y++) {
                    int word = buf.getShort() & 0xFFFF;
                    int t0   =  word        & 0x1F;
                    int t1   = (word >>  5) & 0x1F;
                    int t2   = (word >> 10) & 0x1F;

                    for (int p = 0; p < 3; p++) {
                        int x = bx + p;
                        if (x >= texW) break;
                        int t     = (p == 0) ? t0 : (p == 1) ? t1 : t2;
                        int argb  = shade[t];  // row 0 = brightest

                        normal[y * texW + x] = argb;
                        gray  [y * texW + x] = grayVal(t);

                        // Pack canaux isoles
                        int brightness = (argb >> 16 & 0xFF) + (argb >> 8 & 0xFF) + (argb & 0xFF);
                        brightness /= 3;
                        if (p == 0) pack0[y * texW + x] = 0xFF000000 | (brightness << 16);
                        if (p == 1) pack1[y * texW + x] = 0xFF000000 | (brightness << 8);
                        if (p == 2) pack2[y * texW + x] = 0xFF000000 | brightness;
                    }
                }
            }

            savePng(outDir.resolve(name + "_normal.png"), normal, texW, texH);
            savePng(outDir.resolve(name + "_gray.png"),   gray,   texW, texH);
            savePng(outDir.resolve(name + "_pack0_R.png"),pack0,  texW, texH);
            savePng(outDir.resolve(name + "_pack1_G.png"),pack1,  texW, texH);
            savePng(outDir.resolve(name + "_pack2_B.png"),pack2,  texW, texH);

            // Stats moyenne par pack
            long sum0=0, sum1=0, sum2=0;
            buf.position(2048);  // retour au debut des pixels
            ByteBuffer buf2 = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
            buf2.position(2048);
            for (int g = 0; g < numGroups; g++) {
                for (int y = 0; y < texH; y++) {
                    int word = buf2.getShort() & 0xFFFF;
                    sum0 +=  word        & 0x1F;
                    sum1 += (word >>  5) & 0x1F;
                    sum2 += (word >> 10) & 0x1F;
                }
            }
            long n = (long) numGroups * texH;
            System.out.printf("  Moyenne texels  PACK0=%.2f  PACK1=%.2f  PACK2=%.2f%n",
                (double)sum0/n, (double)sum1/n, (double)sum2/n);
        }
        System.out.println("\nImages dans " + outDir.toAbsolutePath());
        System.out.println("Si les 3 PACK ont des moyennes similaires -> pas d'artefact systeme.");
        System.out.println("Si PACK2 est systematiquement plus bas -> bug de decodage.");
    }

    static int grayVal(int t) {
        int v = t * 8; // 0-248
        return 0xFF000000 | (v << 16) | (v << 8) | v;
    }

    static void savePng(Path p, int[] pix, int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, pix[y * w + x]);
        ImageIO.write(img, "PNG", p.toFile());
    }
}
