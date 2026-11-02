package ab3d2.tools;

import ab3d2.Mem;

import java.util.ArrayList;
import java.util.List;

/**
 * DIAG — relève la MISE EN SCÈNE : ce que la boucle maîtresse décide de dessiner, dans
 * l'ordre, et dans quel contexte.
 *
 * <p>La boucle maîtresse ne dessine rien elle-même. Elle marche la liste des secteurs
 * visibles à rebours, resserre la fenêtre à chaque ouverture, choisit lequel des deux étages
 * vient en premier, pose les quatre bornes du plan d'eau, et appelle le dessin commande par
 * commande. Tout cela tient en une poignée de variables globales — et c'est leur suite de
 * valeurs, au moment de chaque appel, qui dit si la marche est fidèle.
 *
 * <p>Un relevé par commande : le secteur et son étage, la fenêtre horizontale, le haut et le
 * bas de la pièce, les bornes d'eau, la luminosité, et l'offset du record dans le fichier
 * d'habillage — c'est lui qui dit DE QUEL mur ou de QUELLE surface il s'agit.
 *
 * <p>Inerte tant que {@link #capturing} est faux.
 */
public final class ZonePlan {

    /** Arme la capture. */
    public static boolean capturing;

    /**
     * Fait SAUTER le dessin : la boucle décide, mais ne peint pas.
     *
     * <p>Le relevé ne porte que sur les décisions, et aucune routine de dessin ne touche aux
     * variables dont la boucle se sert — ni la fenêtre horizontale, ni le haut et le bas de la
     * pièce, ni les bornes d'eau. Les sauter ne change donc rien à ce qu'on relève, et évite
     * d'exiger que tout le rendu tienne debout pour éprouver la marche.
     */
    public static boolean stop;

    /** Les commandes relevées, dans l'ordre où la boucle les a données. */
    public static final List<int[]> plan = new ArrayList<>();

    private ZonePlan() {
    }

    public static void reset() {
        plan.clear();
    }

    /**
     * Une commande que la boucle maîtresse s'apprête à exécuter.
     *
     * @param op l'octet bas du mot de tête : 0 mur, 1 sol, 2 plafond, 4 objets, 7 eau,
     *           12 ciel
     * @param at l'adresse du mot de tête, d'où se tire l'offset dans le fichier d'habillage
     */
    public static void capture(int op, int at) {
        plan.add(new int[] {
            Mem.w(ab3d2.bss.DrawBss.Draw_CurrentZone_w),
            Mem.b(ab3d2.bss.DrawBss.Draw_DoUpper_b) != 0 ? 1 : 0,
            Mem.w(ab3d2.HiresData.Zone_Bright_w),
            Mem.w(ab3d2.HireswallData.Draw_LeftClip_w),
            Mem.w(ab3d2.HireswallData.Draw_RightClip_w),
            Mem.l(ab3d2.bss.DrawBss.Draw_TopOfRoom_l),
            Mem.l(ab3d2.bss.DrawBss.Draw_BottomOfRoom_l),
            Mem.l(ab3d2.bss.DrawBss.Draw_BeforeWaterTop_l),
            Mem.l(ab3d2.bss.DrawBss.Draw_BeforeWaterBottom_l),
            Mem.l(ab3d2.bss.DrawBss.Draw_AfterWaterTop_l),
            Mem.l(ab3d2.bss.DrawBss.Draw_AfterWaterBottom_l),
            Mem.b(ab3d2.bss.DrawBss.Draw_InRootZone_b) != 0 ? 1 : 0,
            op,
            at - Mem.l(ab3d2.bss.LevelBss.Lvl_GraphicsPtr_l),
        });
    }

    /**
     * Où finit le record d'une commande, pour l'enjamber sans la dessiner.
     *
     * <p>Chaque routine de dessin rend l'adresse où elle s'est arrêtée ; ne pas l'appeler
     * oblige à refaire ce calcul. Il est court : un pan occupe vingt-huit octets fixes, une
     * surface horizontale en occupe autant que son contour a de sommets, les objets un seul
     * mot, et le ciel rien du tout.
     *
     * @param op l'octet bas du mot de tête
     * @param at l'adresse qui suit ce mot de tête
     */
    public static int skip(int op, int at) {
        return switch (op) {
            case 0 -> at + 28;                           // Draw_Wall
            case 1, 2, 7 -> {                            // Draw_Flats
                int sides = Mem.w(at + 2) + 1;
                yield at + 4 + (sides + 1) * 2 + 6;
            }
            case 4 -> at + 2;                            // Draw_Objects
            default -> at;                               // le ciel, et ce qu'on ne connaît pas
        };
    }
}
