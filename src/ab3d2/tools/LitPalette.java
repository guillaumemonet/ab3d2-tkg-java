package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève la PALETTE qu'une image éclairée se fabrique, et ce qui l'a décidée.
 *
 * <p>Un sprite éclairé ne lit aucune palette toute faite : il en construit une, de deux cent
 * trente-deux octets, juste avant de poser ses pixels. On se place à la fin de cette
 * construction, quand le tampon est rempli et que rien n'a encore été dessiné.
 *
 * <p>On relève les OCTETS du tampon, et séparément ce qui a servi à les obtenir : où la chose
 * se tient, dans quel secteur, sous quel angle de regard, et avec quelle luminosité de
 * distance. La vérification refait le calcul à partir de ces seules entrées — les anneaux
 * d'éclairage, eux, se recalculent et sont éprouvés ailleurs.
 *
 * <p>Le tampon fait trois cent quatre-vingt-douze octets alors que la boucle n'en remplit que
 * deux cent trente-deux. On relève les trois cent quatre-vingt-douze : la traînée laissée par
 * le sprite précédent fait partie de ce qu'il faut reproduire.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class LitPalette {

    /** Arme la capture. */
    public static boolean capturing;

    /**
     * Par image éclairée : {rang de la feuille, secteur, étage haut, x, z, angle du regard,
     * image retournée, luminosité de distance, laquelle des quatre palettes}.
     */
    public static final List<int[]> builds = new ArrayList<>();

    /** Les trois cent quatre-vingt-douze octets du tampon, une fois la palette construite. */
    public static final List<byte[]> pals = new ArrayList<>();

    /** Les soixante-quatre octets des anneaux d'éclairage, tels que le calcul les a lus. */
    public static final List<byte[]> rings = new ArrayList<>();

    /** Où, dans le fichier de la grille, la tache a été puisée. */
    public static final List<Integer> bases = new ArrayList<>();

    /** Les vingt-neuf clartés retenues, après bornage entre zéro et trente et un. */
    public static final List<byte[]> ramps = new ArrayList<>();

    /** Posés au vol par la routine. */
    public static int base;

    /** Les vingt-neuf clartés, en cours de relevé. */
    public static final byte[] ramp = new byte[29];
    private static int rampAt;

    /** Le rang de la feuille de sprites, pose par le repartiteur juste avant. */
    public static int sheet;

    private LitPalette() {
    }

    public static void reset() {
        builds.clear();
        pals.clear();
        rings.clear();
        bases.clear();
        ramps.clear();
        rampAt = 0;
    }

    /** Une clarté retenue par la boucle des rampes. */
    public static void clarte(int v) {
        if (rampAt < ramp.length) {
            ramp[rampAt] = (byte) v;
        }
        rampAt++;
    }

    /** Ce qui a servi, et ce qui en est sorti. */
    public static void built(int at, int size) {
        builds.add(new int[] {
            sheet,
            Mem.uw(ab3d2.bss.DrawBss.Draw_CurrentZone_w),
            Mem.ub(ab3d2.bss.DrawBss.draw_InUpperZone_b),
            (short) Mem.uw(ab3d2.bss.DrawBss.draw_Obj_XPos_w),
            (short) Mem.uw(ab3d2.bss.DrawBss.draw_Obj_ZPos_w),
            Mem.uw(ab3d2.bss.PlayerBss.Plr1_TmpAngPos_w),
            Mem.ub(ab3d2.ObjdrawhiresData.draw_FlipIt_b),
            (short) Mem.uw(ab3d2.bss.DrawBss.draw_BrightToAdd_w),
            Mem.ub(ab3d2.ObjdrawhiresData.draw_WhichLightPal_b),
        });
        byte[] out = new byte[size];
        for (int i = 0; i < size; i++) {
            out[i] = (byte) Mem.ub(at + i);
        }
        pals.add(out);
        byte[] anneaux = new byte[64];
        for (int i = 0; i < 64; i++) {
            anneaux[i] = (byte) Mem.ub(ab3d2.bss.DrawBss.draw_AngleBrights_vl + i);
        }
        rings.add(anneaux);
        bases.add(base);
        ramps.add(ramp.clone());
        rampAt = 0;
    }
}
