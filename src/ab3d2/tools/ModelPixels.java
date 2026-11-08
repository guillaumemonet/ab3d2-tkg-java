package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève les PIXELS qu'un modèle en relief dépose, colonne par colonne.
 *
 * <p>Les quatre remplisseurs de polygones peignent chacun une colonne verticale à la fois. On
 * se place de part et d'autre de leur boucle : avant, pour noter où elle commence et combien
 * de lignes elle couvre ; après, pour relever les octets qu'elle laisse.
 *
 * <p>Relever l'ÉTAT de la colonne, et non les écritures, est délibéré : les faces à trous et
 * celles qui miroitent sautent des lignes, et c'est le fond qu'on doit alors retrouver. Le
 * fond est posé par l'appelant avant chaque objet, et il n'est pas uniforme — sans quoi le
 * miroitement, qui lit le pixel déjà présent, ne serait éprouvé sur rien.
 *
 * <p>Les colonnes sont relevées en vrac pour tout l'objet, dans l'ordre où elles sont peintes.
 * C'est cet ordre qui les apparie à la vérification.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class ModelPixels {

    /** Arme la capture. */
    public static boolean capturing;

    /** Par colonne : {abscisse, première ligne, nombre de lignes}. */
    public static final List<int[]> columns = new ArrayList<>();

    /** Les octets de chaque colonne, de haut en bas, après le passage du remplisseur. */
    public static final List<byte[]> bytes = new ArrayList<>();

    private static int at;
    private static int count;

    private ModelPixels() {
    }

    public static void reset() {
        columns.clear();
        bytes.clear();
    }

    /** Ce que la boucle de pixels s'apprête à couvrir. */
    public static void before(int a3, int lines) {
        at = a3;
        count = lines;
        int screen = Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l);
        int off = a3 - screen;
        columns.add(new int[] {
            Math.floorMod(off, ab3d2.Hires.SCREEN_WIDTH),
            Math.floorDiv(off, ab3d2.Hires.SCREEN_WIDTH),
            lines,
        });
    }

    /** Ce qu'elle a laissé. */
    public static void after() {
        byte[] out = new byte[count];
        for (int i = 0; i < count; i++) {
            out[i] = (byte) Mem.ub(at + i * ab3d2.Hires.SCREEN_WIDTH);
        }
        bytes.add(out);
    }
}
