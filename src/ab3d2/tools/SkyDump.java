package ab3d2.tools;

import ab3d2.Mem;
import ab3d2.bss.Bss;
import ab3d2.data.DataSections;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Outil de diagnostic : rend le backdrop ciel (ab3:includes/rawbackpacked) en PNG, avec la
 * palette globale 256pal, dans les deux interprétations de layout :
 *   - sky_colmajor.png : column-major data[x*H + y] (ce que lit Newanims.Draw_SkyBackdrop)
 *   - sky_rowmajor.png : row-major data[y*W + x] (comparaison)
 *
 * But : distinguer une DONNÉE d'image corrompue (image incohérente) d'un bug de ROUTINE
 * d'affichage (image cohérente ici mais mal affichée en jeu).
 *
 * Lancement : gradle -p java skyDump   (workingDir = racine projet → medias/original).
 * NB : le ciel en jeu est rendu avec la palette du NIVEAU courant ; ici on utilise la palette
 * globale 256pal — les couleurs peuvent différer légèrement, mais la STRUCTURE de l'image est
 * révélatrice.
 */
public final class SkyDump {

    private static final int W = 648;   // SKY_BACKDROP_W
    private static final int H = 240;   // SKY_BACKDROP_H

    public static void main(String[] args) throws Exception {
        Bss.init();
        DataSections.init();                                 // charge 256pal → draw_Palette_vw

        int pal = ab3d2.data.DrawData.draw_Palette_vw;       // 256 couleurs × 3 guns (words, octet utile)
        int[] rgb = new int[256];
        for (int i = 0; i < 256; i++) {
            int r = Mem.uw(pal + (i * 3 + 0) * 2) & 0xFF;
            int g = Mem.uw(pal + (i * 3 + 1) * 2) & 0xFF;
            int b = Mem.uw(pal + (i * 3 + 2) * 2) & 0xFF;
            rgb[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        byte[] data = ab3d2.Assets.bytes("includes/rawbackpacked");
        System.out.println("rawbackpacked : " + data.length + " o (attendu " + (W * H) + ")");

        BufferedImage colImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        BufferedImage rowImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                int ci = x * H + y;                          // column-major (moteur)
                int ri = y * W + x;                          // row-major
                colImg.setRGB(x, y, rgb[ci < data.length ? (data[ci] & 0xFF) : 0]);
                rowImg.setRGB(x, y, rgb[ri < data.length ? (data[ri] & 0xFF) : 0]);
            }
        }
        ImageIO.write(colImg, "png", new File("sky_colmajor.png"));
        ImageIO.write(rowImg, "png", new File("sky_rowmajor.png"));
        System.out.println("écrit sky_colmajor.png (layout moteur) + sky_rowmajor.png (comparaison)");
    }
}
