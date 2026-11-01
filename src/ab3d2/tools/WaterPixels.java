package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève les PIXELS qu'une tranche d'EAU dépose, et ce qui les décide.
 *
 * <p>L'eau n'a pas de texture : elle relit l'ÉCRAN quelques lignes plus loin et repasse l'octet
 * trouvé par une rampe de miroitement. Ce qu'on relève ici n'est donc pas une tuile mais le
 * décalage vertical du reflet, le miroitement que la distance choisit, et l'image d'ondulation
 * en cours.
 *
 * <p>Comme la boucle LIT le tampon, le relevé n'a de sens que si le tampon est connu : l'oracle
 * y pose un motif avant de dessiner, et la vérification pose le même.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class WaterPixels {

    /** Arme la capture. */
    public static boolean capturing;

    /**
     * Les tranches relevées : {ligne, gauche, largeur, miroitement, décalage du reflet en
     * octets, départ de l'image d'ondulation, position, pas, distance, lignes restantes}.
     */
    public static final List<int[]> spans = new ArrayList<>();

    /** Les octets écrits par chaque tranche, de gauche à droite. */
    public static final List<byte[]> pixels = new ArrayList<>();

    private static int at;
    private static int count;

    private WaterPixels() {
    }

    public static void reset() {
        spans.clear();
        pixels.clear();
    }

    /** Ce que la boucle texel s'apprête à faire. */
    public static void before(int a3, int d7, int a1, int a6, int a0, int d5, int d2) {
        at = a3;
        count = (short) d7;
        int screen = Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l);
        int shade = Mem.l(ab3d2.bss.DrawBss.Draw_TexturePalettePtr_l);
        int frames = ab3d2.data.DrawData.draw_WaterFrames_vb;
        int off = a3 - screen;
        spans.add(new int[] {
            Math.floorDiv(off, ab3d2.Hires.SCREEN_WIDTH),
            Math.floorMod(off, ab3d2.Hires.SCREEN_WIDTH),
            count,
            a1 - shade - 256 * 16,
            (short) a6,
            a0 - frames,
            d5,
            d2,
            Mem.l(ab3d2.HiresData.draw_Distance_l),
            Mem.w(ab3d2.HiresData.disttobot),
        });
    }

    /** Les octets que la boucle texel vient d'écrire. */
    public static void after() {
        byte[] out = new byte[Math.max(count, 0)];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Mem.ub(at + i);
        }
        pixels.add(out);
    }
}
