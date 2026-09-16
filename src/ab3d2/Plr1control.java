package ab3d2;

import ab3d2.modules.Player;

import static ab3d2.M68k.setw;
import static ab3d2.bss.PlayerBss.Plr1_Data;
import static ab3d2.bss.PlayerBss.Plr1_SnapXOff_l;
import static ab3d2.bss.PlayerBss.Plr1_SnapZOff_l;
import static ab3d2.bss.PlayerBss.Plr1_AngPos_w;
import static ab3d2.bss.AnimBss.Anim_TempFrames_w;
import static ab3d2.HiresData.Path;
import static ab3d2.HiresData.endpath;
import static ab3d2.HiresData.pathpt;
import static ab3d2.data.TablesData.SINTAB_MASK_ADR;

/**
 * Traduction littérale de ab3d2_source/plr1control.s — wrappers du joueur 1.
 * Le code a été généralisé dans modules/player.s (plr_*), ces routines ne font
 * que passer a0 = Plr1_Data.
 */
public final class Plr1control {

    private Plr1control() {
    }

    public static void Plr1_ShowGunName() {
        Player.plr_ShowGunName(Plr1_Data);             // lea Plr1_Data,a0 ; bra plr_ShowGunName
    }

    public static void Plr1_MouseControl() {
        Player.plr_MouseControl(Plr1_Data);            // lea Plr1_Data,a0 ; jsr plr_MouseControl
        Plr1_KeyboardControl();                         // fall through
    }

    public static void Plr1_KeyboardControl() {
        Player.plr_KeyboardControl(Plr1_Data);         // lea Plr1_Data,a0 ; jsr plr_KeyboardControl
        Plr1_Fall();                                   // jsr Plr1_Fall
    }

    public static void Plr1_JoystickControl() {
        Cd32joy._ReadJoy1();                           // jsr _ReadJoy1
        Plr1_KeyboardControl();                         // bra Plr1_KeyboardControl
    }

    public static void Plr1_FootstepFX() {
        Player.plr_DoFootstepFX(Plr1_Data);            // lea Plr1_Data,a0 ; bra plr_DoFootstepFX
    }

    public static void Plr1_Fall() {
        Player.plr_Fall(Plr1_Data);                    // lea Plr1_Data,a0 ; bra plr_Fall
    }

    /** Plr1_FollowPath (attract mode) — chemin caméra (vide : testpath commenté). */
    public static void Plr1_FollowPath() {
        int a0 = Mem.l(pathpt);                        // move.l pathpt,a0
        int d1 = setw(0, Mem.uw(a0));                  // move.w (a0),d1
        Mem.ww(Plr1_SnapXOff_l, d1);                   // move.w d1,Plr1_SnapXOff_l
        d1 = setw(d1, Mem.uw(a0 + 2));                 // move.w 2(a0),d1
        Mem.ww(Plr1_SnapZOff_l, d1);                   // move.w d1,Plr1_SnapZOff_l
        int d0 = setw(0, Mem.uw(a0 + 4));              // move.w 4(a0),d0
        d0 = setw(d0, d0 + d0);                        // add.w d0,d0
        d0 = setw(d0, d0 & SINTAB_MASK_ADR);           // AMOD_A d0
        Mem.ww(Plr1_AngPos_w, d0);                     // move.w d0,Plr1_AngPos_w
        d0 = setw(0, Mem.uw(Anim_TempFrames_w));       // move.w Anim_TempFrames_w,d0
        d0 = setw(d0, (d0 & 0xFFFF) << 3);             // asl.w #3,d0
        a0 = a0 + (short) d0;                          // adda.w d0,a0
        if (a0 >= endpath) a0 = Path;                  // cmp.l #endpath,a0 ; blt .notrestartpath ; move.l #Path,a0
        Mem.wl(pathpt, a0);                            // move.l a0,pathpt
    }
}
