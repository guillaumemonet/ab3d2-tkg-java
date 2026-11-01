package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève les PIXELS qu'une tranche de sol dépose, et ce qui les décide.
 *
 * <p>Pendant de {@link WallPixels} pour les surfaces horizontales. {@link FloorSpans} relève la
 * GÉOMÉTRIE d'une tranche et renonce à la dessiner ; celui-ci la laisse se dessiner et relève
 * les octets. On se place juste avant la boucle texel, là où tout est résolu : la rangée
 * d'ombrage, la tuile, la position dans la texture et son pas.
 *
 * <p>Les surfaces d'EAU ne passent pas par là : elles ont leur propre boucle.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class FloorPixels {

    /** Arme la capture. */
    public static boolean capturing;

    /** Les tranches relevées : {ligne, gauche, largeur, rangée d'ombrage, tuile, position, pas}. */
    public static final List<int[]> spans = new ArrayList<>();

    /** Les octets écrits par chaque tranche, de gauche à droite. */
    public static final List<byte[]> pixels = new ArrayList<>();

    private static int at;
    private static int count;

    private FloorPixels() {
    }

    public static void reset() {
        spans.clear();
        pixels.clear();
    }

    /** Ce que la boucle texel s'apprête à faire. */
    public static void before(int a3, int d7, int a1, int a0, int d5, int d2) {
        at = a3;
        count = (short) d7;
        int screen = Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l);
        int shade = Mem.l(ab3d2.bss.DrawBss.Draw_TexturePalettePtr_l);
        int floors = Mem.l(ab3d2.bss.DrawBss.Draw_FloorTexturesPtr_l);
        int off = a3 - screen;
        spans.add(new int[] {
            Math.floorDiv(off, ab3d2.Hires.SCREEN_WIDTH),
            Math.floorMod(off, ab3d2.Hires.SCREEN_WIDTH),
            count,
            a1 - shade,
            a0 - floors,
            d5,
            d2,
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
