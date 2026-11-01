package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève les TRANCHES HORIZONTALES qu'un sol produit, avant qu'elles ne soient texturées.
 *
 * <p>Même usage que {@link WallStrips} : donner au portage de référence de quoi se comparer à
 * une réécriture. {@code pastfloorbright} finit par tenir, pour une ligne d'écran, ses deux
 * bords, sa distance, sa table d'ombrage et les deux accumulateurs de texture — puis passe la
 * main à la boucle texel. On intercepte juste avant, ce qui donne la GÉOMÉTRIE du sol sans
 * dépendre d'un tampon d'écran.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class FloorSpans {

    /** Arme la capture. Quand elle est armée, le dessin lui-même n'a PAS lieu. */
    public static boolean capturing;

    /** Les tranches relevées : {y, gauche, droite, distance, luminosité, texture, pas}. */
    public static final List<int[]> spans = new ArrayList<>();

    private FloorSpans() {
    }

    public static void reset() {
        spans.clear();
    }

    /**
     * Relève la tranche que {@code pastfloorbright} vient de calculer.
     *
     * @param screenPtr le pointeur d'écran, d'où se déduit la ligne
     * @param shadePtr  la table d'ombrage, d'où se déduit le rang de luminosité
     * @param texture   l'accumulateur de texture, ordonnée en haut et abscisse en bas
     * @param step      son incrément par pixel, rangé de même
     */
    public static void take(int screenPtr, int shadePtr, int texture, int step) {
        int left = Mem.w(ab3d2.HiresData.leftedge);
        int right = Mem.w(ab3d2.HiresData.rightedge);
        int row = screenPtr - left - Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l);
        int shade = shadePtr - Mem.l(ab3d2.bss.DrawBss.Draw_TexturePalettePtr_l) - 256 * 32;
        spans.add(new int[] {
            Math.floorDiv(row, ab3d2.Hires.SCREEN_WIDTH),
            left,
            right,
            Mem.l(ab3d2.HiresData.draw_Distance_l),
            shade / 512,
            texture,
            step,
        });
    }
}
