package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève les PIXELS qu'une tranche de sol DÉGRADÉE dépose, et ce qui les décide.
 *
 * <p>Même boucle texel que {@link FloorPixels} : le code d'origine n'en a qu'une pour les sols,
 * et elle lit toujours la luminosité interpolée. Ce qui change est d'où vient la rangée
 * d'ombrage. Le rendu plat la range dans le pointeur de table et donne une pente nulle ; le
 * rendu dégradé laisse le pointeur au début du bloc et fait tout varier. On relève donc ici le
 * DÉPART et le PAS de la luminosité, là où l'autre relevait un décalage de table.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class GourFloorPixels {

    /** Arme la capture. */
    public static boolean capturing;

    /**
     * Les tranches relevées : {ligne, gauche, largeur, départ de la luminosité, son pas,
     * décalage du bloc d'ombrage, tuile, position, pas de texture}.
     */
    public static final List<int[]> spans = new ArrayList<>();

    /** Les octets écrits par chaque tranche, de gauche à droite. */
    public static final List<byte[]> pixels = new ArrayList<>();

    private static int at;
    private static int count;

    private GourFloorPixels() {
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
            Mem.w(ab3d2.HiresData.leftbright),
            Mem.w(ab3d2.HiresData.brightspd),
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
