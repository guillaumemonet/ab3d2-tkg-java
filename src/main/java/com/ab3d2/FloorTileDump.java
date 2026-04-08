package com.ab3d2;

import com.ab3d2.assets.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

/**
 * Exporte les tiles de sol en PNG pour verification visuelle.
 *   gradle run --main-class=com.ab3d2.FloorTileDump
 */
public class FloorTileDump {
    static final String R = "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String OUT = "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/obj/floortiles";

    public static void main(String[] args) throws Exception {
        Path root = Path.of(R);
        Path outDir = Path.of(OUT);
        Files.createDirectories(outDir);

        int[] palette = WadToPngExporter.loadPalette(root);
        FloorTextureLoader loader = new FloorTextureLoader();
        loader.load(root.resolve("floors"), palette);

        // Exporter les tiles avec les vrais offsets de LEVEL_A : 1, 257, 513, 769
        int[] testOffsets = {1, 257, 513, 769, 0, 2, 3, 4};
        for (int w : testOffsets) {
            int[] px = loader.getTile(w);
            int ww = FloorTextureLoader.TILE_W;
            int hh = FloorTextureLoader.TILE_H;
            BufferedImage img = new BufferedImage(ww, hh, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < hh; y++)
                for (int x = 0; x < ww; x++)
                    img.setRGB(x, y, px[y * ww + x]);
            Path out = outDir.resolve("tile_w" + w + ".png");
            ImageIO.write(img, "PNG", out.toFile());
            System.out.printf("tile_w%-5d.png -> premier pixel ARGB=0x%08X%n",
                w, px[0]);
        }
    }
}
