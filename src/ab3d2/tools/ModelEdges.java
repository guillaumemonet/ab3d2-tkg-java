package ab3d2.tools;

import ab3d2.Mem;

/**
 * DIAG — relève les BORDS que chaque face laisse dans les deux tables de colonnes.
 *
 * <p>Pour chaque face arrivée jusqu'au tracé : où commence son dossier, si une arête a nourri
 * les tables, les deux bornes de l'objet, puis — si le découpage d'écran laisse quelque
 * chose — la première colonne à peindre, leur nombre moins une, et le contenu des deux tables
 * sur cette plage.
 *
 * <p>Les colonnes relevées sont exactement celles que les remplisseurs liront, ni plus ni
 * moins : celles d'avant ou d'après ne sont jamais regardées, et peuvent porter les restes
 * d'une face précédente.
 *
 * <p>Une face que le découpage écarte garde {@code -1} comme première colonne : son absence
 * est le relevé.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class ModelEdges {

    /** Arme la capture. */
    public static boolean capturing;

    /** Laisse de côté le remplissage lui-même, qui n'apprendrait rien de plus. */
    public static boolean stop = true;

    /**
     * Un relevé par face : où, tracée ou non, les deux bornes, la plage, puis les colonnes à
     * raison de huit valeurs chacune.
     */
    public static int[][] faces = new int[0][];

    private static final java.util.List<int[]> OUT = new java.util.ArrayList<>();

    private ModelEdges() {
    }

    public static void reset() {
        OUT.clear();
        faces = new int[0][];
    }

    /** Les bornes qu'une face a laissées, le découpage pas encore fait. */
    public static void bounds(int at, boolean drawn, int left, int right) {
        OUT.add(new int[] {at, drawn ? 1 : 0, left, right, -1, -1});
    }

    /**
     * La plage retenue par le découpage, et ce que les deux tables y portent.
     *
     * @param topTab l'adresse de la table du haut
     * @param botTab l'adresse de la table du bas
     */
    public static void span(int at, int first, int span, int topTab, int botTab) {
        int[] last = OUT.get(OUT.size() - 1);
        if (last[0] != at || last[4] != -1) {
            throw new IllegalStateException("face " + at + " : relevé désaccordé");
        }
        int[] full = new int[6 + 8 * (span + 1)];
        System.arraycopy(last, 0, full, 0, 6);
        full[4] = first;
        full[5] = span;
        int at2 = 6;
        for (int x = first; x <= first + span; x++) {
            int top = topTab + x * 16;
            int bot = botTab + x * 16;
            full[at2++] = Mem.w(top);
            full[at2++] = Mem.l(top + 2);
            full[at2++] = Mem.l(top + 6);
            full[at2++] = Mem.w(top + 10);
            full[at2++] = Mem.w(bot);
            full[at2++] = Mem.l(bot + 2);
            full[at2++] = Mem.l(bot + 6);
            full[at2++] = Mem.w(bot + 10);
        }
        OUT.set(OUT.size() - 1, full);
    }

    /** Clôt le relevé de l'objet. */
    public static void done() {
        faces = OUT.toArray(new int[0][]);
    }
}
