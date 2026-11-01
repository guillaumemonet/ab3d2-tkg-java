package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève les PIXELS qu'une colonne de mur dépose, et ce qui les a décidés.
 *
 * <p>Même usage que {@link WallStrips} et {@link FloorSpans} : donner au portage de référence de
 * quoi se comparer à une réécriture. On se place à l'entrée des boucles texel, là où tout est
 * résolu — la colonne de texture, les deux rampes d'ombrage, le pas et le point de départ — et
 * on relève ces valeurs AVANT, les octets écrits APRÈS. Le dessin a bien lieu.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class WallPixels {

    /** Arme la capture. */
    public static boolean capturing;

    /**
     * Les colonnes relevées : {ligne, abscisse, hauteur, rampe claire, rampe sombre, format,
     * décalage du bloc, position de texture, pas entier, pas fractionnaire, masque de hauteur}.
     */
    public static final List<int[]> strips = new ArrayList<>();

    /** Les octets écrits par chaque colonne, de haut en bas. */
    public static final List<byte[]> pixels = new ArrayList<>();

    private static int at;
    private static int stride;
    private static int count;

    private WallPixels() {
    }

    public static void reset() {
        strips.clear();
        pixels.clear();
    }

    /** Ce que la boucle texel s'apprête à faire. */
    public static void before(int d0, int d2, int d3, int d4, int d6, int d7,
                              int a2, int a3, int a4, int a5, int strip) {
        at = a3;
        stride = (short) d0;
        count = (short) d6 + 1;
        int screen = Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l);
        int palette = Mem.l(ab3d2.bss.DrawBss.Draw_PalettePtr_l);
        int chunk = Mem.l(ab3d2.bss.DrawBss.Draw_ChunkPtr_l);
        int off = a3 - screen;
        strips.add(new int[] {
            Math.floorDiv(off, ab3d2.Hires.SCREEN_WIDTH),
            Math.floorMod(off, ab3d2.Hires.SCREEN_WIDTH),
            count,
            a2 - palette,
            a4 - palette,
            (byte) strip,
            a5 - chunk,
            d4,
            (short) d2,
            d3,
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
