package ab3d2;

import ab3d2.modules.Player;

import static ab3d2.bss.PlayerBss.Plr1_Data;
import static ab3d2.bss.PlayerBss.Plr2_Data;

/**
 * Traduction littérale de ab3d2_source/plr2control.s — wrappers du joueur 2.
 * Quirk préservé : Plr2_FootstepFX passe Plr1_Data (et non Plr2_Data) comme dans
 * l'original.
 */
public final class Plr2control {

    private Plr2control() {
    }

    public static void Plr2_ShowGunName() {
        Player.plr_ShowGunName(Plr2_Data);             // lea Plr2_Data,a0 ; bra plr_ShowGunName
    }

    public static void Plr2_MouseControl() {
        Player.plr_MouseControl(Plr2_Data);            // lea Plr2_Data,a0 ; jsr plr_MouseControl
        Plr2_KeyboardControl();                         // fall through
    }

    public static void Plr2_KeyboardControl() {
        Player.plr_KeyboardControl(Plr2_Data);         // lea Plr2_Data,a0 ; jsr plr_KeyboardControl
        Plr2_Fall();                                   // jsr Plr2_Fall
    }

    public static void Plr2_JoystickControl() {
        Cd32joy._ReadJoy2();                           // jsr _ReadJoy2
        Plr2_KeyboardControl();                         // bra Plr2_KeyboardControl
    }

    public static void Plr2_FootstepFX() {
        Player.plr_DoFootstepFX(Plr1_Data);            // lea Plr1_Data,a0 (quirk) ; bra plr_DoFootstepFX
    }

    public static void Plr2_Fall() {
        Player.plr_Fall(Plr2_Data);                    // lea Plr2_Data,a0 ; bra plr_Fall
    }
}
