package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève OÙ un sprite de MIROITEMENT se pose, juste avant que ses pixels ne partent.
 *
 * <p>Le miroitement n'est pas une variante de {@link BitmapPlace} : c'est une routine entière
 * à part, avec sa propre pose. Elle ne calcule aucune luminosité, ne retourne jamais l'image,
 * et ne choisit pas de rampe — sa table est la même pour tous, et c'est celle des TEXTURES.
 * Huit valeurs suffisent donc là où le sprite ordinaire en demandait quatorze.
 *
 * <p>Comme pour le sprite ordinaire, un miroitement que la routine écarte ne passe jamais par
 * ici : son absence est le relevé.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class GlarePlace {

    /** Arme la capture. */
    public static boolean capturing;

    /** Arrete la routine apres la capture, au lieu de la laisser dessiner. */
    public static boolean stop = true;

    /** Les poses relevées, une par miroitement effectivement dessiné. */
    public static final List<int[]> placed = new ArrayList<>();

    private GlarePlace() {
    }

    public static void reset() {
        placed.clear();
    }

    /**
     * Ce que la routine s'apprête à dessiner.
     *
     * @param d3 la largeur moins une
     * @param d4 la hauteur moins une
     * @param d5 le départ dans la colonne de texture
     * @param d2 le pas par ligne, les deux moitiés échangées
     * @param a2 le pas par colonne, en bandes
     */
    public static void capture(int d3, int d4, int d5, int d2, int a2) {
        int buffer = Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l);
        int off = Mem.l(ab3d2.bss.DrawBss.toppt_l) - buffer;
        int strip = Mem.l(ab3d2.bss.DrawBss.midobj_l) - Mem.l(ab3d2.bss.DrawBss.draw_PtrPtr_l);
        placed.add(new int[] {
            Math.floorDiv(off, ab3d2.Hires.SCREEN_WIDTH),
            Math.floorMod(off, ab3d2.Hires.SCREEN_WIDTH),
            (short) d3 + 1,
            (short) d4 + 1,
            strip / 4,
            a2,
            d5,
            d2,
        });
    }
}
