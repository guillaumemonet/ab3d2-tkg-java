package ab3d2.tools;

import ab3d2.Mem;

/**
 * DIAG — relève les points d'un MODÈLE en relief, une fois tournés, projetés et éclairés.
 *
 * <p>Trois tables d'un coup, parce qu'une seule boucle les remplit et qu'aucune n'a de sens
 * sans les autres : les points en trois dimensions ({@code draw_3DPointsRotated_vl}), les
 * mêmes projetés à l'écran ({@code draw_2DPointsProjected_vl}), et la luminosité de chacun
 * ({@code boxbrights_vw}).
 *
 * <p>On relève aussi la GRILLE d'éclairage, seize par seize, telle qu'elle vient d'être
 * fabriquée depuis les anneaux. C'est l'entrée de la luminosité des points, et son rang quinze
 * n'est jamais écrit — il faut voir que ce qu'on y lit est bien ce qu'y a laissé l'objet
 * d'avant, c'est-à-dire zéro depuis le début.
 *
 * <p>Un modèle abandonné en cours de projection — un point au-delà de la distance maximale —
 * ne passe jamais par ici : son absence est le relevé.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class ModelPoints {

    /** Arme la capture. */
    public static boolean capturing;

    /** Arrête la routine après la capture, au lieu de la laisser dessiner. */
    public static boolean stop = true;

    /** Vrai dès qu'une capture a eu lieu. */
    public static boolean captured;

    /** Combien de points porte le modèle relevé. */
    public static int points;

    /** S'il faut trier ses faces. */
    public static int sortIt;

    /** Les drapeaux de parties de la pose. */
    public static int onOff;

    /** Six valeurs par point : x, y (mots doubles), z, colonne, ligne, luminosité. */
    public static int[][] perPoint = new int[0][];

    /** Les deux cent cinquante-six octets de la grille. */
    public static final int[] grid = new int[256];

    private ModelPoints() {
    }

    public static void reset() {
        captured = false;
        points = 0;
        perPoint = new int[0][];
    }

    /** Ce que la routine vient de calculer, avant qu'elle ne trie les parties. */
    public static void capture() {
        captured = true;
        points = Mem.uw(ab3d2.bss.DrawBss.draw_NumPoints_w);
        sortIt = Mem.w(ab3d2.bss.DrawBss.draw_SortIt_w);
        onOff = Mem.l(ab3d2.bss.DrawBss.draw_ObjectOnOff_l);
        int rot = ab3d2.bss.DrawBss.draw_3DPointsRotated_vl;
        int proj = ab3d2.bss.DrawBss.draw_2DPointsProjected_vl;
        int lum = ab3d2.bss.DrawBss.boxbrights_vw;
        perPoint = new int[points][6];
        for (int i = 0; i < points; i++) {
            perPoint[i][0] = Mem.l(rot + i * 10);
            perPoint[i][1] = Mem.l(rot + i * 10 + 4);
            perPoint[i][2] = Mem.w(rot + i * 10 + 8);
            perPoint[i][3] = Mem.w(proj + i * 4);
            perPoint[i][4] = Mem.w(proj + i * 4 + 2);
            perPoint[i][5] = Mem.w(lum + i * 2);
        }
        for (int i = 0; i < grid.length; i++) {
            grid[i] = (byte) Mem.ub(ab3d2.bss.DrawBss.draw_PointAndPolyBrights_vl + i);
        }
    }
}
