package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève OÙ un sprite se pose, juste avant que ses pixels ne partent.
 *
 * <p>Tout ce que draw_Bitmap calcule tient, au bout du compte, en une dizaine de valeurs : le
 * coin où l'image commence, sa taille après rognage, la bande de texture de départ et le pas
 * qui la fait avancer, et la rampe de couleurs que la distance a choisie. C'est là, au moment
 * où la routine se branche vers l'une de ses quatre boucles de pixels, qu'on les prend.
 *
 * <p>Un sprite que la routine écarte — derrière l'œil, hors de la fenêtre, hauteur nulle — ne
 * passe jamais par ici : son absence est le relevé.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class BitmapPlace {

    /** Arme la capture. */
    public static boolean capturing;

    /** Arrete la routine apres la capture, au lieu de la laisser dessiner. */
    public static boolean stop = true;

    /** Les poses relevées, une par sprite effectivement dessiné. */
    public static final List<int[]> placed = new ArrayList<>();

    private BitmapPlace() {
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
     * @param a4 la rampe de couleurs, pointeur absolu
     */
    public static void capture(int d3, int d4, int d5, int d2, int a2, int a4) {
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
            a4 - Mem.l(ab3d2.ObjdrawhiresData.draw_BasePalPtr_l),
            Mem.b(ab3d2.ObjdrawhiresData.draw_FlipIt_b) != 0 ? 1 : 0,
            Mem.b(ab3d2.ObjdrawhiresData.draw_LightIt_b) != 0 ? 1 : 0,
            Mem.b(ab3d2.ObjdrawhiresData.draw_Additive_b) != 0 ? 1 : 0,
            Mem.ub(ab3d2.ObjdrawhiresData.draw_WhichLightPal_b),
            Mem.w(ab3d2.bss.DrawBss.draw_BrightToAdd_w),
        });
    }
}
