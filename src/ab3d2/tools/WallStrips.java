package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — releve les BANDES VERTICALES qu'un mur produit, avant qu'elles ne soient texturees.
 *
 * <p>Meme usage que {@code Hires.dbgGeom*} : donner au portage de reference de quoi se comparer
 * a une reecriture. {@code screendivide} ecrit dans l'espace de travail un enregistrement par
 * colonne d'ecran — abscisse, abscisse de texture, distance, haut, bas, luminosite — puis passe
 * la main au dessin. On intercepte juste avant, ce qui donne la GEOMETRIE du mur sans dependre
 * d'un tampon d'ecran.
 *
 * <p>Inerte tant que {@link #capturing} est faux : une comparaison par image, rien de plus.
 */
public final class WallStrips {

    /** Taille d'un enregistrement de bande, en octets. */
    private static final int RECORD = 22;

    /** Arme la capture. Quand elle est armee, le dessin lui-meme n'a PAS lieu. */
    public static boolean capturing;

    /** Les bandes relevees : {x, bm, dist, top, bas, luminosite}, accumulateurs entiers. */
    public static final List<int[]> strips = new ArrayList<>();

    private WallStrips() {
    }

    public static void reset() {
        strips.clear();
    }

    /** Releve {@code count} enregistrements consecutifs a partir de {@code base}. */
    public static void take(int base, int count) {
        for (int i = 0; i < count; i++) {
            int at = base + i * RECORD;
            strips.add(new int[] {
                Mem.w(at),
                Mem.l(at + 2),
                Mem.l(at + 6),
                Mem.l(at + 10),
                Mem.l(at + 14),
                Mem.l(at + 18),
            });
        }
    }
}
