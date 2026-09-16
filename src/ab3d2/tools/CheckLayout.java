package ab3d2.tools;

import ab3d2.Mem;
import ab3d2.bss.Bss;
import ab3d2.data.DataSections;
import ab3d2.data.TablesData;
import ab3d2.data.TextData;
import ab3d2.data.VidData;

/**
 * Vérifications de cohérence de la disposition mémoire (tailles de tables,
 * contiguïté des sections) — outil de développement, pas une partie du portage.
 */
public final class CheckLayout {

    public static void main(String[] args) {
        Bss.init();
        DataSections.init();

        check("GammaIncTable2-1", VidData.Vid_GammaIncTable2_vb - VidData.Vid_GammaIncTable1_vb, 256);
        check("GammaIncTable3-2", VidData.Vid_GammaIncTable3_vb - VidData.Vid_GammaIncTable2_vb, 256);
        check("GammaIncTable4-3", VidData.Vid_GammaIncTable4_vb - VidData.Vid_GammaIncTable3_vb, 256);
        check("GammaIncTable5-4", VidData.Vid_GammaIncTable5_vb - VidData.Vid_GammaIncTable4_vb, 256);
        check("GammaIncTable6-5", VidData.Vid_GammaIncTable6_vb - VidData.Vid_GammaIncTable5_vb, 256);
        check("GammaIncTable7-6", VidData.Vid_GammaIncTable7_vb - VidData.Vid_GammaIncTable6_vb, 256);
        check("GammaIncTable8-7", VidData.Vid_GammaIncTable8_vb - VidData.Vid_GammaIncTable7_vb, 256);
        check("ContrastAdjust-Table8", VidData.Vid_ContrastAdjust_w - VidData.Vid_GammaIncTable8_vb, 256);
        check("ContrastAdjust valeur", Mem.uw(VidData.Vid_ContrastAdjust_w), 0x0100);

        check("DivThreeTable taille", endOfDivThree() - TablesData.DivThreeTable_vb, 1320);
        check("DivThree[659]", Mem.ub(TablesData.DivThreeTable_vb + 659 * 2), 219);
        check("DivThree[659] mod", Mem.ub(TablesData.DivThreeTable_vb + 659 * 2 + 1), 2);

        // Texte de fin : enregistrements de 82 octets
        int len = TextData.ENDENDGAMETEXT - TextData.Game_SinglePlayerVictoryText_vb;
        check("VictoryText multiple de 82", len % 82, 0);
        System.out.println("VictoryText: " + (len / 82) + " enregistrements de 82 octets");

        System.out.println("Mem.allocTop = " + Mem.allocTop() + " octets");
        System.out.println(failures == 0 ? "TOUT OK" : failures + " ECHEC(S)");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static int endOfDivThree() {
        // DivThreeTable est la dernière entrée de TablesData ; l'entrée suivante
        // dans l'ordre d'init est Game_SoundOptionsText_vb (text_data, align 4).
        return TextData.Game_SoundOptionsText_vb & ~3;
    }

    private static int failures;

    private static void check(String name, int actual, int expected) {
        if (actual != expected) {
            System.out.println("ECHEC " + name + ": " + actual + " != " + expected);
            failures++;
        } else {
            System.out.println("ok    " + name + " = " + actual);
        }
    }

    private CheckLayout() {
    }
}
