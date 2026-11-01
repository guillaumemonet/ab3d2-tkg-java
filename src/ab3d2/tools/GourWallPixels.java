package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève les PIXELS qu'une colonne de mur DÉGRADÉE dépose, et ce qui les a décidés.
 *
 * <p>Même usage que {@link WallPixels}, pour l'autre rastériseur : celui où la luminosité varie
 * du haut de la colonne à son bas. Là où le rendu simple nomme deux rampes d'ombrage et les
 * alterne, celui-ci part d'une rampe et avance d'un pas à chaque pixel — ce sont donc ces deux
 * valeurs-là, le départ et le pas, qu'on relève à la place.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class GourWallPixels {

    /** Arme la capture. */
    public static boolean capturing;

    /**
     * Les colonnes relevées : {ligne, abscisse, hauteur, départ du dégradé, pas du dégradé,
     * format, décalage du bloc, position de texture, pas de texture, masque de hauteur}.
     */
    public static final List<int[]> strips = new ArrayList<>();

    /** Les octets écrits par chaque colonne, de haut en bas. */
    public static final List<byte[]> pixels = new ArrayList<>();

    private static int at;
    private static int stride;
    private static int count;

    private GourWallPixels() {
    }

    public static void reset() {
        strips.clear();
        pixels.clear();
    }

    /** Ce que la boucle texel s'apprête à faire. */
    public static void before(int d0, int d3, int d4, int d5, int d6, int d7,
                              int a2, int a3, int a5, int strip) {
        at = a3;
        stride = (short) d0;
        count = (short) d6 + 1;
        int screen = Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l);
        int chunk = Mem.l(ab3d2.bss.DrawBss.Draw_ChunkPtr_l);
        int off = a3 - screen;
        strips.add(new int[] {
            Math.floorDiv(off, ab3d2.Hires.SCREEN_WIDTH),
            Math.floorMod(off, ab3d2.Hires.SCREEN_WIDTH),
            count,
            d3,
            d5,
            (byte) strip,
            a5 - chunk,
            d4,
            a2,
            (short) d7,
        });
    }

    /** Les octets que la boucle texel vient d'écrire. */
    public static void after() {
        byte[] out = new byte[count];
        for (int i = 0; i < count; i++) {
            out[i] = (byte) Mem.ub(at + i * stride);
        }
        pixels.add(out);
    }
}
