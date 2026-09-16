package ab3d2;

import ab3d2.host.CustomChips;
import ab3d2.host.LowLevelLib;

import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.HiresData.Game_Running_b;
import static ab3d2.ControlloopData.forward_key;
import static ab3d2.ControlloopData.backward_key;
import static ab3d2.ControlloopData.turn_left_key;
import static ab3d2.ControlloopData.turn_right_key;
import static ab3d2.ControlloopData.fire_key;
import static ab3d2.ControlloopData.operate_key;
import static ab3d2.ControlloopData.run_key;
import static ab3d2.ControlloopData.duck_key;
import static ab3d2.ControlloopData.force_sidestep_key;
import static ab3d2.ControlloopData.jump_key;
import static ab3d2.bss.PlayerBss.Plr1_GunFrame_w;
import static ab3d2.bss.PlayerBss.Plr2_GunFrame_w;
import static ab3d2.bss.PlayerBss.Plr1_GunSelected_b;
import static ab3d2.bss.PlayerBss.Plr2_GunSelected_b;
import static ab3d2.bss.PlayerBss.Plr1_Weapons_vb;
import static ab3d2.bss.PlayerBss.Plr2_Weapons_vb;

/**
 * Traduction littérale de ab3d2_source/cd32joy.s.
 *
 * Lecture manette CD32/joystick via lowlevel.library (ReadJoyPort, couche hôte
 * LowLevelLib). La lecture matérielle renvoie l'état du port (type + boutons +
 * directions) ; le MAPPING de ces bits vers la table clavier (KeyMap_vb, via les
 * touches configurées d'AssignableKeys) est traduit fidèlement, avec :
 *   - auto-répétition en mode menu (compteurs button/button1 PARTAGÉS) ;
 *   - mode en-jeu (sne direct) ;
 *   - edge-detect du bouton « duck » (.ducklast) et du cyclage d'arme PLAY (.heldlast),
 *     ces drapeaux étant LOCAUX à chaque _ReadJoy (donc dédoublés rj1/rj2) ;
 *   - handler Joystick lisant joy1dat ($dff00c/d) + tir ($bfe001 bit 7).
 *
 * Constantes lowlevel.i : JP_TYPE_* = (n<<28) ; boutons bits 17-23 ; joy bits 0-3.
 * NB QUIRK : _ReadJoy1 ET _ReadJoy2 lisent ReadJoyPort(1) dans l'original (littéral).
 */
public final class Cd32joy {

    // -- constantes lowlevel.i --
    private static final int JP_TYPE_NOTAVAIL = 0 << 28;
    private static final int JP_TYPE_GAMECTLR = 1 << 28;
    private static final int JP_TYPE_MOUSE    = 2 << 28;
    private static final int JP_TYPE_JOYSTK   = 3 << 28;
    private static final int JP_TYPE_MASK     = 15 << 28;

    private static final int JPF_BUTTON_BLUE    = 1 << 23;
    private static final int JPF_BUTTON_RED     = 1 << 22;
    private static final int JPF_BUTTON_YELLOW  = 1 << 21;
    private static final int JPF_BUTTON_GREEN   = 1 << 20;
    private static final int JPF_BUTTON_FORWARD = 1 << 19;
    private static final int JPF_BUTTON_REVERSE = 1 << 18;
    private static final int JPF_BUTTON_PLAY    = 1 << 17;
    private static final int JPF_JOY_UP    = 1 << 3;
    private static final int JPF_JOY_DOWN  = 1 << 2;
    private static final int JPF_JOY_LEFT  = 1 << 1;
    private static final int JPF_JOY_RIGHT = 1 << 0;

    // -- compteurs auto-répétition (PARTAGÉS, button:/button1: dc.l 0) --
    private static int button;
    private static int button1;

    // -- drapeaux edge-detect locaux à chaque _ReadJoy (.ducklast/.heldlast) --
    private static int rj1_ducklast;
    private static int rj1_heldlast;
    private static int rj2_ducklast;
    private static int rj2_heldlast;

    private Cd32joy() {
    }

    /** _InitLowLevel : ouvre lowlevel.library (couche hôte). */
    public static int _InitLowLevel() {
        LowLevelLib.LowBase = 1;                            // OpenLibrary("lowlevel.library",1) (hôte : succès)
        return 0;                                           // moveq #0,d0 (succès) / -1 si échec
    }

    /** _CloseLowLevel : ferme lowlevel.library. */
    public static void _CloseLowLevel() {
        LowLevelLib.LowBase = 0;                            // CloseLibrary
    }

    /** Écrit KeyMap[<key>] = (joy & jpfBit) ? $FF : 0 (move.b key,d5 ; and #JPF,d0 ; sne (a5,d5.w)). */
    private static void sneKey(int keyAddr, int joy, int jpfBit) {
        Mem.wb(KeyMap_vb + Mem.ub(keyAddr), (joy & jpfBit) != 0 ? 0xFF : 0x00);
    }

    /** _ReadJoy1 : lit le port 1 et applique le mapping joueur 1. */
    public static void _ReadJoy1() {
        int d0 = LowLevelLib.ReadJoyPort(1);                // move.l #1,d0 ; ReadJoyPort
        int d1 = d0 & JP_TYPE_MASK;                         // move.l d0,d1 ; and.l #JP_TYPE_MASK,d1

        if (d1 == JP_TYPE_NOTAVAIL) {                       // cmp #NOTAVAIL ; beq .Empty
            return;
        }
        if (d1 == JP_TYPE_GAMECTLR) {                       // .GameCtrl
            gameCtrl1(d0);
            return;
        }
        if (d1 == JP_TYPE_MOUSE) {                          // .Mouse (rts)
            return;
        }
        if (d1 == JP_TYPE_JOYSTK) {                         // .Joystick
            joystick();
            return;
        }
        // type inconnu → rts
    }

    /** _ReadJoy2 : lit le port 1 (quirk original) et applique le mapping joueur 2. */
    public static void _ReadJoy2() {
        int d0 = LowLevelLib.ReadJoyPort(1);                // move.l #1,d0 ; ReadJoyPort (port 1 comme l'original)
        int d1 = d0 & JP_TYPE_MASK;

        if (d1 == JP_TYPE_NOTAVAIL) {
            return;
        }
        if (d1 == JP_TYPE_GAMECTLR) {
            gameCtrl2(d0);
            return;
        }
        if (d1 == JP_TYPE_MOUSE) {
            return;
        }
        if (d1 == JP_TYPE_JOYSTK) {
            joystick();
            return;
        }
    }

    /** Handler manette CD32 — joueur 1. */
    private static void gameCtrl1(int d0) {
        if (Mem.b(Game_Running_b) == 0) {                   // tst.b Game_Running_b ; beq .inGame
            // -- mode menu : auto-répétition UP/DOWN --
            // UP
            if ((d0 & JPF_JOY_UP) == 0) {                   // and #JPF_JOY_UP ; beq .up
                button = 0;                                 // .up : button = 0
                Mem.wb(KeyMap_vb + Mem.ub(forward_key), 0); // .upup : clr (a5,d5.w)
            } else if (button != 0) {                       // tst button ; bne .upup
                Mem.wb(KeyMap_vb + Mem.ub(forward_key), 0); // .upup : clr
            } else {
                Mem.wb(KeyMap_vb + Mem.ub(forward_key), 0xFF); // st (a5,d5.w)
                button = 15;                                // move.l #15,button
            }
            // DOWN
            if ((d0 & JPF_JOY_DOWN) == 0) {                 // and #JPF_JOY_DOWN ; beq .up1
                button1 = 0;
                Mem.wb(KeyMap_vb + Mem.ub(backward_key), 0);
            } else if (button1 != 0) {
                Mem.wb(KeyMap_vb + Mem.ub(backward_key), 0);
            } else {
                Mem.wb(KeyMap_vb + Mem.ub(backward_key), 0xFF);
                button1 = 15;
            }
        } else {
            // -- mode en-jeu : sne direct --
            sneKey(forward_key, d0, JPF_JOY_UP);            // .inGame
            sneKey(backward_key, d0, JPF_JOY_DOWN);
        }

        // .morebuttons (commun)
        sneKey(turn_left_key, d0, JPF_JOY_LEFT);
        sneKey(turn_right_key, d0, JPF_JOY_RIGHT);
        sneKey(fire_key, d0, JPF_BUTTON_GREEN);
        sneKey(operate_key, d0, JPF_BUTTON_YELLOW);
        sneKey(run_key, d0, JPF_BUTTON_RED);

        // duck (edge-detect .ducklast)
        if ((d0 & JPF_BUTTON_BLUE) != 0) {                  // and #JPF_BUTTON_BLUE ; beq .notduckbutpre
            if (rj1_ducklast == 0) {                        // tst .ducklast ; bne .notduckbut
                Mem.wb(KeyMap_vb + Mem.ub(duck_key), 0xFF); // st (a5,d5.w)
                rj1_ducklast = 0xFF;                        // st .ducklast
            }
        } else {
            rj1_ducklast = 0;                               // clr.b .ducklast
        }

        // _ReadJoy1 : ordre force_sidestep puis jump
        sneKey(force_sidestep_key, d0, JPF_BUTTON_FORWARD);
        sneKey(jump_key, d0, JPF_BUTTON_REVERSE);

        // weapon cycle (PLAY, edge-detect .heldlast)
        if ((d0 & JPF_BUTTON_PLAY) == 0 || Mem.b(Plr1_GunFrame_w) != 0) { // beq .nonextweappre / tst.b Plr1_GunFrame_w bne
            rj1_heldlast = 0;                               // .nonextweappre : clr.b .heldlast
        } else if (rj1_heldlast == 0) {                     // tst .heldlast ; bne .nonextweap
            rj1_heldlast = 0xFF;                            // st .heldlast
            int g = Mem.ub(Plr1_GunSelected_b);             // move.b Plr1_GunSelected_b,d0
            do {                                            // .findnext
                g += 1;                                     // addq #1,d0
                if (g > 9) g = 0;                           // cmp #9 ; ble .okgun ; moveq #0,d0
            } while (Mem.uw(Plr1_Weapons_vb + g * 2) == 0); // tst.w (a0,d0*2) ; beq .findnext
            Mem.wb(Plr1_GunSelected_b, g);                  // move.b d0,Plr1_GunSelected_b
            Plr1control.Plr1_ShowGunName();                 // jsr Plr1_ShowGunName
        }
    }

    /** Handler manette CD32 — joueur 2. */
    private static void gameCtrl2(int d0) {
        if (Mem.b(Game_Running_b) == 0) {
            if ((d0 & JPF_JOY_UP) == 0) {
                button = 0;
                Mem.wb(KeyMap_vb + Mem.ub(forward_key), 0);
            } else if (button != 0) {
                Mem.wb(KeyMap_vb + Mem.ub(forward_key), 0);
            } else {
                Mem.wb(KeyMap_vb + Mem.ub(forward_key), 0xFF);
                button = 15;
            }
            if ((d0 & JPF_JOY_DOWN) == 0) {
                button1 = 0;
                Mem.wb(KeyMap_vb + Mem.ub(backward_key), 0);
            } else if (button1 != 0) {
                Mem.wb(KeyMap_vb + Mem.ub(backward_key), 0);
            } else {
                Mem.wb(KeyMap_vb + Mem.ub(backward_key), 0xFF);
                button1 = 15;
            }
        } else {
            sneKey(forward_key, d0, JPF_JOY_UP);
            sneKey(backward_key, d0, JPF_JOY_DOWN);
        }

        sneKey(turn_left_key, d0, JPF_JOY_LEFT);
        sneKey(turn_right_key, d0, JPF_JOY_RIGHT);
        sneKey(fire_key, d0, JPF_BUTTON_GREEN);
        sneKey(operate_key, d0, JPF_BUTTON_YELLOW);
        sneKey(run_key, d0, JPF_BUTTON_RED);

        // duck (_ReadJoy2 : st .ducklast PUIS st key)
        if ((d0 & JPF_BUTTON_BLUE) != 0) {
            if (rj2_ducklast == 0) {
                rj2_ducklast = 0xFF;                        // st .ducklast
                Mem.wb(KeyMap_vb + Mem.ub(duck_key), 0xFF); // st (a5,d5.w)
            }
        } else {
            rj2_ducklast = 0;
        }

        // _ReadJoy2 : ordre jump puis force_sidestep
        sneKey(jump_key, d0, JPF_BUTTON_REVERSE);
        sneKey(force_sidestep_key, d0, JPF_BUTTON_FORWARD);

        if ((d0 & JPF_BUTTON_PLAY) == 0 || Mem.b(Plr2_GunFrame_w) != 0) {
            rj2_heldlast = 0;
        } else if (rj2_heldlast == 0) {
            rj2_heldlast = 0xFF;
            int g = Mem.ub(Plr2_GunSelected_b);
            do {
                g += 1;
                if (g > 9) g = 0;
            } while (Mem.uw(Plr2_Weapons_vb + g * 2) == 0);
            Mem.wb(Plr2_GunSelected_b, g);
            Plr2control.Plr2_ShowGunName();
        }
    }

    /**
     * Handler joystick analogique (port lu via joy1dat $dff00c/d + tir $bfe001 bit 7).
     * Décodage standard Amiga (eor des bits) pour les 4 directions.
     */
    private static void joystick() {
        int hi = (CustomChips.joy1dat >> 8) & 0xFF;         // octet @ $dff00c
        int lo = CustomChips.joy1dat & 0xFF;                // octet @ $dff00d

        int d0 = ((hi >> 1) & 1) != 0 ? 0xFF : 0x00;        // btst #1,$dff00c ; sne d0
        int d1 = ((lo >> 1) & 1) != 0 ? 0xFF : 0x00;        // btst #1,$dff00d ; sne d1
        int d2 = ((hi >> 0) & 1) != 0 ? 0xFF : 0x00;        // btst #0,$dff00c ; sne d2
        int d3 = ((lo >> 0) & 1) != 0 ? 0xFF : 0x00;        // btst #0,$dff00d ; sne d3

        // fire = bit 7 de $bfe001 (actif bas) ; seq → $FF si bit clair (tir appuyé)
        Mem.wb(KeyMap_vb + Mem.ub(fire_key), CustomChips.ciaaPraBit7() ? 0x00 : 0xFF); // btst #7,$bfe001 ; seq

        Mem.wb(KeyMap_vb + Mem.ub(turn_left_key), d0);      // move.b d0,(a5,d5.w)
        Mem.wb(KeyMap_vb + Mem.ub(turn_right_key), d1);     // move.b d1,(a5,d5.w)
        d2 ^= d0;                                           // eor.b d0,d2
        Mem.wb(KeyMap_vb + Mem.ub(forward_key), d2);        // move.b d2,(a5,d5.w)
        d3 ^= d1;                                           // eor.b d1,d3
        Mem.wb(KeyMap_vb + Mem.ub(backward_key), d3);       // move.b d3,(a5,d5.w)
    }
}
