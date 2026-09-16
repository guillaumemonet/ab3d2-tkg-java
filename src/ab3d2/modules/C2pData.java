package ab3d2.modules;

import ab3d2.Mem;

/**
 * Donnée de ab3d2_source/modules/c2p/c2p.s:164 — C2P_NeedsInit_b.
 *
 * Les routines C2P (chunky-to-planar) sont EXCLUES du portage (le rendu Java
 * est chunky nativement), mais ce drapeau est positionné par le code de jeu
 * (player.s, hires.s, teleport_fx) et consommé par la couche d'affichage hôte
 * pour réinitialiser le pipeline vidéo après un changement de mode.
 */
public final class C2pData {

    public static final int C2P_NeedsInit_b = Mem.dcB(0);
    /** Drapeaux C2P écrits par game_main_loop (téléport / changement de marge). Inertes en RTG. */
    public static final int C2P_Teleporting_b = Mem.dcB(0);
    public static final int C2P_NeedsSetParam_b = Mem.dcB(0);

    /**
     * Game_TeleportFrame_w (hires.s) — compteur de l'effet de « dématérialisation » du
     * téléporteur/fin de niveau. Téléporteur : posé à 8 (countdown), décrémenté à chaque
     * frame présentée. Sortie de niveau : compteur façon TELVAL (zone +2/frame, présentation
     * −1/frame). Pilote le remappage shimmer (shimmerfile) dans ScreenC.Vid_Present.
     */
    public static final int Game_TeleportFrame_w = Mem.dcW(0);

    private C2pData() {
    }
}
