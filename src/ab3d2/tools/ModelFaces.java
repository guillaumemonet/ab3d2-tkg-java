package ab3d2.tools;

/**
 * DIAG — relève le sort de chaque FACE d'un modèle en relief.
 *
 * <p>Pour chacune, dans l'ordre où le jeu les visite : où commence son dossier, ce qu'il en a
 * fait — jetée par le garde-fou, jetée parce qu'elle tourne le dos, ou gardée —, et, pour
 * celles qui sont allées jusqu'au remplissage, quelle texture et quelle palette.
 *
 * <p>Le déplacement du dossier compte depuis {@code draw_StartOfObjPtr_l}, c'est-à-dire depuis
 * le deuxième mot du fichier : c'est ce que le lecteur de modèles range aussi.
 *
 * <p>La texture et la palette ne sont relevées que pour les faces qui ont passé le découpage
 * d'écran, parce que c'est seulement là que le jeu les calcule. Les autres gardent
 * {@code -1} : leur absence est le relevé.
 *
 * <p>{@link #stop} empêche le remplissage lui-même : il n'apprendrait rien de plus et coûterait
 * tout le temps du relevé.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class ModelFaces {

    /** Jetée par le garde-fou. */
    public static final int GUARDED = 0;

    /** Elle tourne le dos. */
    public static final int TURNED = 1;

    /** Gardée. */
    public static final int SHOWN = 2;

    /** Arme la capture. */
    public static boolean capturing;

    /** Laisse de côté le remplissage lui-même. */
    public static boolean stop = true;

    /** Quatre valeurs par face : où, ce qu'il en a fait, la texture, la palette. */
    public static int[][] faces = new int[0][];

    private static final java.util.List<int[]> OUT = new java.util.ArrayList<>();

    private ModelFaces() {
    }

    public static void reset() {
        OUT.clear();
        faces = new int[0][];
    }

    /** Ce qu'il est advenu d'une face. */
    public static void outcome(int at, int what) {
        OUT.add(new int[] {at, what, -1, -1});
    }

    /** La texture et la palette d'une face qui va jusqu'au remplissage. */
    public static void shade(int at, int texture, int palette) {
        int[] last = OUT.get(OUT.size() - 1);
        if (last[0] != at || last[1] != SHOWN) {
            throw new IllegalStateException("face " + at + " : relevé désaccordé");
        }
        last[2] = texture;
        last[3] = palette;
    }

    /** Clôt le relevé de l'objet. */
    public static void done() {
        faces = OUT.toArray(new int[0][]);
    }
}
