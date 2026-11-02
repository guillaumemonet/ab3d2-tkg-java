package ab3d2.tools;

import ab3d2.Mem;
import ab3d2.bss.DrawBss;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève la TABLE DE PROFONDEUR que Draw_Objects construit, avant qu'elle ne serve.
 *
 * <p>Le tri des objets n'écrit rien à l'écran : il range, dans une table de quatre-vingts
 * cases, les objets du secteur en cours, du plus loin au plus proche. Ce qu'on relève ici est
 * donc cette table telle qu'elle est au moment où la boucle de dessin va la parcourir — et
 * seulement elle, car la capture ARRÊTE Draw_Objects avant le dessin.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class ObjectOrder {

    /** Arme la capture, et avec elle l'arrêt avant le dessin. */
    public static boolean capturing;

    /** Les cases relevées : {profondeur, rang de l'objet}, les quatre-vingts. */
    public static final List<int[]> slots = new ArrayList<>();

    private ObjectOrder() {
    }

    public static void reset() {
        slots.clear();
    }

    /** La table telle qu'elle est, cases vides comprises. */
    public static void capture() {
        for (int i = 0; i < 80; i++) {
            slots.add(new int[] {
                Mem.w(DrawBss.draw_DepthTable_vl + i * 4),
                Mem.w(DrawBss.draw_DepthTable_vl + i * 4 + 2),
            });
        }
    }
}
