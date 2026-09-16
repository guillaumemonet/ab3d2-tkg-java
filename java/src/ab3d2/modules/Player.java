package ab3d2.modules;

import ab3d2.ControlloopData;
import ab3d2.Hires;
import ab3d2.HiresData;
import ab3d2.HireswallData;
import ab3d2.Mem;
import ab3d2.bss.PlayerBss;
import ab3d2.c.Message;
import ab3d2.c.SystemC;
import ab3d2.data.TablesData;
import ab3d2.data.TextData;
import ab3d2.host.CustomChips;

import static ab3d2.Defs.*;
import static ab3d2.Hires.PLR_CROUCH_HEIGHT;
import static ab3d2.Hires.PLR_STAND_HEIGHT;
import static ab3d2.Hires.QUIT_KEY;
import static ab3d2.Hires.SCREEN_WIDTH;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.negw;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.bss.LevelBss.Lvl_DataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.SystemBss.Sys_FPSLimit_w;
import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.bss.VidBss.Vid_FullScreenTemp_b;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.ZoneBss.ZONE_BACKDROP_DISABLE_SIZE;
import static ab3d2.bss.ZoneBss.Zone_BackdropDisable_vb;
import static ab3d2.data.TablesData.COSINE_OFS;
import static ab3d2.data.TablesData.SinCosTable_vw;
import static ab3d2.modules.RawKeyMacros.*;

/**
 * Traduction littérale de ab3d2_source/modules/player.s
 *
 * "Common code for player control."
 *
 * Notes (original): The entity references for player 1, player 2 and the
 * weapon view model are stored contiguously:
 *   Plr1_Data->ObjectPtr_l + EntT_SizeOf_l == Plr2_Data->ObjectPtr_l
 *   Plr2_Data->ObjectPtr_l + EntT_SizeOf_l == player weapon entity
 *
 * a0 = pointeur sur les données joueur (structure PlrT).
 */
public final class Player {

    private Player() {
    }

    // TEST (harnais) : override du spawn J1. dbgSpawnZone<0 → désactivé.
    // dbgSpawnX/Z = coordonnées MONDE (word), placées dans le mot fort de Plr1_XOff/ZOff.
    public static int dbgSpawnZone = -1, dbgSpawnX, dbgSpawnZ;
    /** DIAG : angle de depart force (unites de la table sinus), -1 = celui du niveau. */
    public static int dbgSpawnAng = -1;

    // ******************************************************************************
    // * Initialise player positions
    // ******************************************************************************

    /** Plr_Initialise */
    public static void Plr_Initialise() {
        int a1 = Mem.l(Lvl_DataPtr_l);                 // move.l Lvl_DataPtr_l,a1
        a1 += LVLT_MESSAGE_LENGTH * LVLT_MESSAGE_COUNT; // add.l #LVLT_MESSAGE_LENGTH*LVLT_MESSAGE_COUNT,a1

        // player 1 : pZone = Lvl_DataPtr_l[Lvl_ZonePtrsPtr_l[zoneId_w]]
        int d0 = Mem.w(a1 + TBLT_Plr1_StartZoneID_w);  // move.w TBLT_Plr1_StartZoneID_w(a1),d0
        int a0 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a0
        d0 = Mem.l(a0 + ((short) d0) * 4);             // move.l (a0,d0.w*4),d0
        Mem.wl(PlayerBss.Plr1_ZonePtr_l, d0);          // move.l d0,Plr1_ZonePtr_l
        a0 = Mem.l(PlayerBss.Plr1_ZonePtr_l);          // move.l Plr1_ZonePtr_l,a0
        d0 = Mem.l(a0 + ZoneT_Floor_l);                // move.l ZoneT_Floor_l(a0),d0
        d0 -= PLR_STAND_HEIGHT;                        // sub.l #PLR_STAND_HEIGHT,d0
        Mem.wl(PlayerBss.Plr1_SnapYOff_l, d0);         // move.l d0,Plr1_SnapYOff_l
        Mem.wl(PlayerBss.Plr1_YOff_l, d0);             // move.l d0,Plr1_YOff_l
        Mem.wl(PlayerBss.Plr1_SnapTYOff_l, d0);        // move.l d0,Plr1_SnapTYOff_l
        Mem.wl(PlayerBss.plr1_OldRoomPtr_l, Mem.l(PlayerBss.Plr1_ZonePtr_l)); // move.l Plr1_ZonePtr_l,plr1_OldRoomPtr_l
        Mem.ww(PlayerBss.Plr1_SnapXOff_l, Mem.uw(a1 + TBLT_Plr1_StartXPos_w)); // move.w ...,Plr1_SnapXOff_l (mot fort)
        Mem.ww(PlayerBss.Plr1_SnapZOff_l, Mem.uw(a1 + TBLT_Plr1_StartZPos_w)); // move.w ...,Plr1_SnapZOff_l
        Mem.ww(PlayerBss.Plr1_XOff_l, Mem.uw(a1 + TBLT_Plr1_StartXPos_w));     // move.w ...,Plr1_XOff_l
        Mem.ww(PlayerBss.Plr1_ZOff_l, Mem.uw(a1 + TBLT_Plr1_StartZPos_w));     // move.w ...,Plr1_ZOff_l
        Mem.wl(PlayerBss.plr1_DefaultEnemyFlags_l, 0b100011); // move.l #%100011,plr1_DefaultEnemyFlags_l

        if (dbgSpawnZone >= 0) {                        // TEST : relocalise le spawn J1 (harnais)
            int zp = Mem.l(Mem.l(Lvl_ZonePtrsPtr_l) + dbgSpawnZone * 4);
            Mem.wl(PlayerBss.Plr1_ZonePtr_l, zp);
            Mem.wl(PlayerBss.plr1_OldRoomPtr_l, zp);
            int fl = Mem.l(zp + ZoneT_Floor_l) - PLR_STAND_HEIGHT;
            Mem.wl(PlayerBss.Plr1_SnapYOff_l, fl);
            Mem.wl(PlayerBss.Plr1_YOff_l, fl);
            Mem.wl(PlayerBss.Plr1_SnapTYOff_l, fl);
            Mem.ww(PlayerBss.Plr1_SnapXOff_l, dbgSpawnX);
            Mem.ww(PlayerBss.Plr1_XOff_l, dbgSpawnX);
            Mem.ww(PlayerBss.Plr1_SnapZOff_l, dbgSpawnZ);
            Mem.ww(PlayerBss.Plr1_ZOff_l, dbgSpawnZ);
            if (dbgSpawnAng >= 0) {                    // DIAG : oriente la vue (comparaison remake)
                Mem.ww(PlayerBss.Plr1_AngPos_w, dbgSpawnAng);
                Mem.ww(PlayerBss.Plr1_SnapAngPos_w, dbgSpawnAng);
            }
        }

        // player 2
        d0 = Mem.w(a1 + TBLT_Plr2_StartZoneID_w);      // move.w TBLT_Plr2_StartZoneID_w(a1),d0
        a0 = Mem.l(Lvl_ZonePtrsPtr_l);                 // move.l Lvl_ZonePtrsPtr_l,a0
        d0 = Mem.l(a0 + ((short) d0) * 4);             // move.l (a0,d0.w*4),d0
        Mem.wl(PlayerBss.Plr2_ZonePtr_l, d0);          // move.l d0,Plr2_ZonePtr_l
        a0 = Mem.l(PlayerBss.Plr2_ZonePtr_l);          // move.l Plr2_ZonePtr_l,a0
        d0 = Mem.l(a0 + ZoneT_Floor_l);                // move.l ZoneT_Floor_l(a0),d0
        d0 -= PLR_STAND_HEIGHT;                        // sub.l #PLR_STAND_HEIGHT,d0
        Mem.wl(PlayerBss.Plr2_SnapYOff_l, d0);         // move.l d0,Plr2_SnapYOff_l
        Mem.wl(PlayerBss.Plr2_YOff_l, d0);             // move.l d0,Plr2_YOff_l
        Mem.wl(PlayerBss.Plr2_SnapTYOff_l, d0);        // move.l d0,Plr2_SnapTYOff_l
        Mem.wl(PlayerBss.Plr2_YOff_l, d0);             // move.l d0,Plr2_YOff_l (doublon de l'original)
        Mem.wl(PlayerBss.plr2_OldRoomPtr_l, Mem.l(PlayerBss.Plr2_ZonePtr_l)); // move.l Plr2_ZonePtr_l,plr2_OldRoomPtr_l
        Mem.ww(PlayerBss.Plr2_SnapXOff_l, Mem.uw(a1 + TBLT_Plr2_StartXPos_w)); // move.w ...
        Mem.ww(PlayerBss.Plr2_SnapZOff_l, Mem.uw(a1 + TBLT_Plr2_StartZPos_w));
        Mem.ww(PlayerBss.Plr2_XOff_l, Mem.uw(a1 + TBLT_Plr2_StartXPos_w));
        Mem.ww(PlayerBss.Plr2_ZOff_l, Mem.uw(a1 + TBLT_Plr2_StartZPos_w));
        Mem.wl(PlayerBss.plr2_DefaultEnemyFlags_l, 0b010011); // move.l #%010011,plr2_DefaultEnemyFlags_l
        // rts
    }

    // ******************************************************************************
    // * Common mouse control — pointer to player data in a0
    // ******************************************************************************

    /** plr_MouseControl */
    public static void plr_MouseControl(int a0) {
        // move.l a0,-(a7) ; CALLC Sys_ReadMouse ; move.l (a7)+,a0
        SystemC.Sys_ReadMouse();

        int a1 = SinCosTable_vw;                       // move.l #SinCosTable_vw,a1
        int d1 = Mem.uw(a0 + PlrT_SnapAngSpd_w);       // move.w PlrT_SnapAngSpd_w(a0),d1 (chargé, non utilisé)
        int d0 = Mem.uw(HiresData.Vis_AngPos_w);       // move.w Vis_AngPos_w,d0
        d0 = TablesData.AMOD_A(d0);                    // AMOD_A d0
        Mem.ww(a0 + PlrT_SnapAngPos_w, d0);            // move.w d0,PlrT_SnapAngPos_w(a0)
        Mem.ww(a0 + PlrT_SnapSinVal_w, Mem.uw(a1 + (short) d0)); // move.w (a1,d0.w),PlrT_SnapSinVal_w(a0)
        a1 += COSINE_OFS;                              // adda.w #COSINE_OFS,a1
        Mem.ww(a0 + PlrT_SnapCosVal_w, Mem.uw(a1 + (short) d0)); // move.w (a1,d0.w),PlrT_SnapCosVal_w(a0)
        int d6 = Mem.l(a0 + PlrT_SnapXSpdVal_l);       // move.l PlrT_SnapXSpdVal_l(a0),d6
        int d7 = Mem.l(a0 + PlrT_SnapZSpdVal_l);       // move.l PlrT_SnapZSpdVal_l(a0),d7
        d6 = -d6;                                      // neg.l d6
        if (d6 > 0) {                                  // ble.s .nobug1
            d6 >>= 1;                                  // asr.l #1,d6
            d6 += 1;                                   // add.l #1,d6
        } else {
            // .nobug1:
            d6 >>= 1;                                  // asr.l #1,d6
        }
        // .bug1:
        d7 = -d7;                                      // neg.l d7
        if (d7 > 0) {                                  // ble.s .nobug2
            d7 >>= 1;                                  // asr.l #1,d7
            d7 += 1;                                   // add.l #1,d7
        } else {
            // .nobug2:
            d7 >>= 1;                                  // asr.l #1,d7
        }
        // .bug2: (d6/d7 calculés mais non réutilisés ensuite — conservé)

        int d3 = Mem.uw(HiresData.Sys_MouseY);         // move.w Sys_MouseY,d3
        if (Mem.b(a0 + PlrT_InvMouse_b) != 0) {        // tst.b PlrT_InvMouse_b(a0) ; beq.s .no_invert
            d3 = negw(d3);                             // neg.w d3
        }
        // .no_invert:
        d3 = setw(d3, d3 - Mem.w(HiresData.Sys_OldMouseY)); // sub.w Sys_OldMouseY,d3
        Mem.ww(HiresData.Sys_OldMouseY, Mem.uw(HiresData.Sys_OldMouseY) + d3); // add.w d3,Sys_OldMouseY
        d0 = Mem.uw(HireswallData.STOPOFFSET);         // move.w STOPOFFSET,d0
        int d2 = setw(0, d3);                          // move.w d3,d2
        // ***************************************************************
        // shoehorned this in here due to the projectiles not having the same
        // trajectory in full screen compared to small screen
        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; beq.s .small
            d2 = muls(d2, 85);                         // muls.w #85,d2 ; multiply by 2/3 of 128
        } else {
            // .small:
            d2 = setw(d2, d2 << 7);                    // asl.w #7,d2 ; multiply by 128
        }
        // .big:
        // ***************************************************************
        Mem.ww(a0 + PlrT_AimSpeed_l, Mem.uw(a0 + PlrT_AimSpeed_l) + d2); // add.w d2,PlrT_AimSpeed_l(a0) (mot fort)
        d0 = setw(d0, d0 + d3);                        // add.w d3,d0
        if ((short) d0 <= Mem.w(HiresData.View_LookMax_w)) { // cmp.w View_LookMax_w,d0 ; bgt.s .skip_look_up
            Mem.ww(a0 + PlrT_AimSpeed_l, -512 * 20);   // move.w #-512*20,PlrT_AimSpeed_l(a0)
            d0 = setw(d0, Mem.uw(HiresData.View_LookMax_w)); // move.w View_LookMax_w,d0
        }
        // .skip_look_up:
        if ((short) d0 >= Mem.w(HiresData.View_LookMin_w)) { // cmp.w View_LookMin_w,d0 ; blt.s .skip_look_down
            Mem.ww(a0 + PlrT_AimSpeed_l, 512 * 20);    // move.w #512*20,PlrT_AimSpeed_l(a0)
            d0 = setw(d0, Mem.uw(HiresData.View_LookMin_w)); // move.w View_LookMin_w,d0
        }
        // .skip_look_down:
        Mem.ww(HireswallData.STOPOFFSET, d0);          // move.w d0,STOPOFFSET
        d0 = negw(d0);                                 // neg.w d0
        d0 = setw(d0, d0 + Mem.w(HireswallData.TOTHEMIDDLE)); // add.w TOTHEMIDDLE,d0
        Mem.ww(HireswallData.SMIDDLEY, d0);            // move.w d0,SMIDDLEY
        d0 = muls(d0, SCREEN_WIDTH);                   // muls #SCREEN_WIDTH,d0
        Mem.wl(HireswallData.SBIGMIDDLEY, d0);         // move.l d0,SBIGMIDDLEY
        int a5 = KeyMap_vb;                            // move.l #KeyMap_vb,a5
        d7 = 0;                                        // moveq #0,d7

        // The right mouse button triggers the next_weapon key
        d7 = setb(d7, Mem.ub(ControlloopData.next_weapon_key)); // move.b next_weapon_key,d7
        if (Mem.b(ControlloopData.Prefs_OriginalMouse_b) != 0) { // tst.b Prefs_OriginalMouse_b ; beq.s .notQuake
            d7 = setb(d7, Mem.ub(ControlloopData.forward_key));  // move.b forward_key,d7
        }
        // .notQuake:
        // btst #2,$dff000+potinp ; right button ; seq (a5,d7.w)
        Mem.wb(a5 + (short) d7, CustomChips.potinpBit2() ? 0 : 0xFF);

        // The left mouse button triggers the fire key
        d7 = setb(d7, Mem.ub(ControlloopData.fire_key)); // move.b fire_key,d7
        // btst #CIAB_GAMEPORT0,$bfe001+ciapra ; left button ; seq (a5,d7.w)
        Mem.wb(a5 + (short) d7, CustomChips.ciaaPraBit6() ? 0 : 0xFF);
        // rts
    }

    // ******************************************************************************
    // * Common keyboard control — pointer to player data in a0
    // ******************************************************************************

    /** plr_KeyboardControl */
    public static void plr_KeyboardControl(int a0) {
        int a5 = KeyMap_vb;                            // move.l #KeyMap_vb,a5
        int d7 = 0;                                    // moveq #0,d7
        int d0, d1, d2, d3, d4, d5, d6;
        int a1, a2, a3, a4;

        // Check for quit
        if (Mem.b(a5 + QUIT_KEY) != 0) {               // tst.b QUIT_KEY(a5) ; beq.s .no_quit
            Mem.wb(ControlloopData.Game_ShouldQuit_b, 0xFF); // st Game_ShouldQuit_b
            Mem.wb(HiresData.Game_MasterQuit_b, 0xFF); // st Game_MasterQuit_b
        }
        // .no_quit:

        d7 = setb(d7, Mem.ub(ControlloopData.next_weapon_key)); // move.b next_weapon_key,d7
        if (Mem.b(a5 + (short) d7) != 0) {             // tst.b (a5,d7.w) ; beq.s .no_next_weapon_pre
            if (Mem.b(PlayerBss.plr_PrevNextWeaponKeyState_b) == 0) { // tst.b ... ; bne.s .no_next_weapon
                Mem.wb(PlayerBss.plr_PrevNextWeaponKeyState_b, 0xFF); // st plr_PrevNextWeaponKeyState_b
                d0 = 0;                                // moveq #0,d0
                d0 = setb(d0, Mem.ub(a0 + PlrT_GunSelected_b)); // move.b PlrT_GunSelected_b(a0),d0
                a1 = a0 + PlrT_Weapons_vb;             // lea PlrT_Weapons_vb(a0),a1

                do { // .find_next_weapon:
                    d0 += 1;                           // addq #1,d0
                    if ((short) d0 > 9) {              // cmp.w #9,d0 ; ble.s .weapon_found
                        d0 = 0;                        // moveq #0,d0
                    }
                    // .weapon_found:
                } while (Mem.w(a1 + ((short) d0) * 2) == 0); // tst.w (a1,d0.w*2) ; beq.s .find_next_weapon

                Mem.wb(a0 + PlrT_GunSelected_b, d0);   // move.b d0,PlrT_GunSelected_b(a0)
                if (ab3d2.Hires.dbgSwitchTest) System.out.println("[switch] -> arme " + d0); // DIAG
                plr_ShowGunName(a0);                   // bsr plr_ShowGunName
                // bra.s .no_next_weapon
            }
        } else {
            // .no_next_weapon_pre:
            Mem.wb(PlayerBss.plr_PrevNextWeaponKeyState_b, 0); // clr.b plr_PrevNextWeaponKeyState_b
        }

        // Sélection directe d'arme par les touches 1..9,0 (extension : absente de l'original
        // qui n'avait que next_weapon_key). Code raw : touche '1'=0x01→arme 0 … '9'=0x09→arme 8,
        // '0'=0x0A→arme 9. On ne bascule que vers une arme possédée et différente de l'actuelle
        // (Plr_Weapons[g]!=0), ce qui évite de réinitialiser le nom d'arme tant que la touche est tenue.
        for (int g = 0; g <= 9; ++g) {
            if (Mem.b(a5 + (g + 1)) != 0) {            // touche chiffre enfoncée (rawkey g+1)
                if (Mem.w(a0 + PlrT_Weapons_vb + g * 2) != 0
                        && Mem.ub(a0 + PlrT_GunSelected_b) != g) {
                    Mem.wb(a0 + PlrT_GunSelected_b, g);
                    plr_ShowGunName(a0);
                }
            }
        }

        // .no_next_weapon:
        d7 = setb(d7, Mem.ub(ControlloopData.operate_key)); // move.b operate_key,d7
        d1 = Mem.ub(a5 + (short) d7);                  // move.b (a5,d7.w),d1
        if ((byte) d1 != 0) {                          // beq.s .nottapped
            if (Mem.b(PlayerBss.plr_PrevUseKeyState_b) == 0) { // tst.b ... ; bne.s .nottapped
                Mem.wb(a0 + PlrT_Used_b, 0xFF);        // st PlrT_Used_b(a0)
            }
        }
        // .nottapped:
        Mem.wb(PlayerBss.plr_PrevUseKeyState_b, d1);   // move.b d1,plr_PrevUseKeyState_b
        d7 = setb(d7, Mem.ub(ControlloopData.duck_key)); // move.b duck_key,d7
        if (Mem.b(a5 + (short) d7) != 0) {             // tst.b (a5,d7.w) ; beq.s .notduck
            Mem.wb(a5 + (short) d7, 0);                // clr.b (a5,d7.w)
            Mem.wl(a0 + PlrT_SnapTargHeight_l, PLR_STAND_HEIGHT); // move.l #PLR_STAND_HEIGHT,PlrT_SnapTargHeight_l(a0)
            int nb = (~Mem.ub(a0 + PlrT_Ducked_b)) & 0xFF; // not.b PlrT_Ducked_b(a0)
            Mem.wb(a0 + PlrT_Ducked_b, nb);
            if (nb != 0) {                             // beq.s .notduck
                Mem.wl(a0 + PlrT_SnapTargHeight_l, PLR_CROUCH_HEIGHT); // move.l #PLR_CROUCH_HEIGHT,...
            }
        }

        // .notduck:
        a4 = Mem.l(a0 + PlrT_ZonePtr_l);               // move.l PlrT_ZonePtr_l(a0),a4
        d0 = Mem.l(a4 + ZoneT_Floor_l);                // move.l ZoneT_Floor_l(a4),d0
        d0 -= Mem.l(a4 + ZoneT_Roof_l);                // sub.l ZoneT_Roof_l(a4),d0
        if (Mem.b(a0 + PlrT_StoodInTop_b) != 0) {      // tst.b PlrT_StoodInTop_b(a0) ; beq.s .use_bottom
            d0 = Mem.l(a4 + ZoneT_UpperFloor_l);       // move.l ZoneT_UpperFloor_l(a4),d0
            d0 -= Mem.l(a4 + ZoneT_UpperRoof_l);       // sub.l ZoneT_UpperRoof_l(a4),d0
        }
        // .use_bottom:
        Mem.wb(a0 + PlrT_Squished_b, 0);               // clr.b PlrT_Squished_b(a0)
        Mem.wl(a0 + PlrT_SnapSquishedHeight_l, PLR_STAND_HEIGHT); // move.l #PLR_STAND_HEIGHT,...
        if (d0 <= PLR_STAND_HEIGHT + 3 * 1024) {       // cmp.l #PLR_STAND_HEIGHT+3*1024,d0 ; bgt.s .oktostand
            Mem.wb(a0 + PlrT_Squished_b, 0xFF);        // st PlrT_Squished_b(a0)
            Mem.wl(a0 + PlrT_SnapSquishedHeight_l, PLR_CROUCH_HEIGHT); // move.l #PLR_CROUCH_HEIGHT,...
        }

        // .oktostand:
        d1 = Mem.l(a0 + PlrT_SnapTargHeight_l);        // move.l PlrT_SnapTargHeight_l(a0),d1
        d0 = Mem.l(a0 + PlrT_SnapSquishedHeight_l);    // move.l PlrT_SnapSquishedHeight_l(a0),d0
        if (d1 >= d0) {                                // cmp.l d0,d1 ; blt.s .notsqu
            d1 = d0;                                   // move.l d0,d1
        }
        // .notsqu:
        d0 = Mem.l(a0 + PlrT_SnapHeight_l);            // move.l PlrT_SnapHeight_l(a0),d0
        if (d0 != d1) {                                // cmp.l d1,d0 ; beq.s .noupordown
            if (d0 > d1) {                             // bgt.s .crouch
                d0 -= 1024;                            // .crouch: sub.l #1024,d0
            } else {
                d0 += 1024;                            // add.l #1024,d0
            }
        }
        // .noupordown:
        Mem.wl(a0 + PlrT_SnapHeight_l, d0);            // move.l d0,PlrT_SnapHeight_l(a0)
        if (Mem.b(a5 + RAWKEY_K) != 0) {               // tst.b RAWKEY_K(a5) ; beq.s .notselkey
            Mem.wb(a5 + RAWKEY_K, 0);                  // clr.b RAWKEY_K(a5)
            Mem.wb(a0 + PlrT_InvMouse_b, 0xFF);        // st PlrT_InvMouse_b(a0) ; hack pour la re-sélection souris
            // move.l a0,-(sp) ; CALLC Msg_PushLine ; move.l (sp)+,a0
            Message.Msg_PushLine(TextData.Game_InputKeyboard_vb,
                    OPTS_MESSAGE_LENGTH | MSG_TAG_OPTIONS); // move.l #Game_InputKeyboard_vb,a0 ; move.w #...,d0
            Mem.wb(a0 + PlrT_Keys_b, 0xFF);            // st PlrT_Keys_b(a0)
            Mem.wb(a0 + PlrT_Path_b, 0);               // clr.b PlrT_Path_b(a0)
            Mem.wb(a0 + PlrT_Mouse_b, 0);              // clr.b PlrT_Mouse_b(a0)
            Mem.wb(a0 + PlrT_Joystick_b, 0);           // clr.b PlrT_Joystick_b(a0)
        }

        // .notselkey:
        if (Mem.b(a5 + RAWKEY_J) != 0) {               // tst.b RAWKEY_J(a5) ; beq.s .notseljoy
            Mem.wb(a0 + PlrT_InvMouse_b, 0xFF);        // st PlrT_InvMouse_b(a0)
            Mem.wb(a5 + RAWKEY_J, 0);                  // clr.b RAWKEY_J(a5)
            Mem.wb(a0 + PlrT_Keys_b, 0);               // clr.b PlrT_Keys_b(a0)
            Mem.wb(a0 + PlrT_Path_b, 0);               // clr.b PlrT_Path_b(a0)
            Mem.wb(a0 + PlrT_Mouse_b, 0);              // clr.b PlrT_Mouse_b(a0)
            Mem.wb(a0 + PlrT_Joystick_b, 0xFF);        // st PlrT_Joystick_b(a0)
            Message.Msg_PushLine(TextData.Game_InputJoystick_vb,
                    OPTS_MESSAGE_LENGTH | MSG_TAG_OPTIONS); // CALLC Msg_PushLine
        }

        // .notseljoy:
        if (Mem.b(a5 + RAWKEY_M) != 0) {               // tst.b RAWKEY_M(a5) ; beq.s .notselmouse
            Mem.wb(a5 + RAWKEY_M, 0);                  // clr.b RAWKEY_M(a5)
            Mem.wb(a0 + PlrT_Keys_b, 0);               // clr.b PlrT_Keys_b(a0)
            Mem.wb(a0 + PlrT_Path_b, 0);               // clr.b PlrT_Path_b(a0)
            Mem.wb(a0 + PlrT_Mouse_b, 0xFF);           // st PlrT_Mouse_b(a0)
            Mem.wb(a0 + PlrT_Joystick_b, 0);           // clr.b PlrT_Joystick_b(a0)
            int v = (Mem.ub(a0 + PlrT_InvMouse_b) ^ 0xFF); // eor.b #-1,PlrT_InvMouse_b(a0)
            Mem.wb(a0 + PlrT_InvMouse_b, v);
            if (v != 0) {                              // beq.s .mouse_normal
                Message.Msg_PushLine(TextData.Game_InputMouseInv_vb,
                        OPTS_MESSAGE_LENGTH | MSG_TAG_OPTIONS); // move.l #Game_InputMouseInv_vb,a0
            } else {
                // .mouse_normal:
                Message.Msg_PushLine(TextData.Game_InputMouse_vb,
                        OPTS_MESSAGE_LENGTH | MSG_TAG_OPTIONS); // move.l #Game_InputMouse_vb,a0
            }
        }

        // .notselmouse:
        a4 = a5 + 1;                                   // lea 1(a5),a4 (touches 1..0 = rawkeys $01..$0A)
        a2 = a0 + PlrT_Weapons_vb;                     // lea PlrT_Weapons_vb(a0),a2
        a3 = Mem.l(a0 + PlrT_ObjectPtr_l);             // move.l PlrT_ObjectPtr_l(a0),a3
        d1 = 9;                                        // move.w #9,d1
        d2 = 0;                                        // move.w #0,d2

        do { // .pickweap:
            d0 = Mem.uw(a2); a2 += 2;                  // move.w (a2)+,d0
            d0 = setb(d0, d0 & Mem.ub(a4)); a4 += 1;   // and.b (a4)+,d0
            if ((byte) d0 != 0) {                      // beq.s .notgotweap
                Mem.wb(a0 + PlrT_GunSelected_b, d2);   // move.b d2,PlrT_GunSelected_b(a0)
                // d2 = number of gun.
                // NB: cmp.l Plr1_Data,a0 compare a0 au CONTENU de Plr1_Data
                // (adressage absolu, pas #immédiat) — conservé tel quel.
                if (a0 == Mem.l(PlayerBss.Plr1_Data)) { // cmp.l Plr1_Data,a0 ; bne.s .use_player_2_timer
                    // .use_player_1_timer: for player1, the viewport weapon entity is two along
                    Mem.ww(a3 + ENT_NEXT_2 + EntT_Timer1_w, 0); // move.w #0,ENT_NEXT_2+EntT_Timer1_w(a3)
                    plr_ShowGunName(a0);               // bsr plr_ShowGunName
                } else {
                    // .use_player_2_timer: for player2, the viewport weapon entity is one along
                    Mem.ww(a3 + ENT_NEXT + EntT_Timer1_w, 0); // move.w #0,ENT_NEXT+EntT_Timer1_w(a3)
                    plr_ShowGunName(a0);               // bsr plr_ShowGunName
                }
                break;                                 // bra.s .go
            }
            // .notgotweap:
            d2 += 1;                                   // addq #1,d2
            d1 = setw(d1, d1 - 1);                     // dbra d1,.pickweap
        } while ((short) d1 != -1);

        // .go:
        if (Mem.b(a5 + RAWKEY_F10) != 0) {             // tst.b RAWKEY_F10(a5) ; beq.s .notswapscr
            if (Mem.b(PlayerBss.lastscr) == 0) {       // tst.b lastscr ; bne.s .notswapscr2
                Mem.wb(PlayerBss.lastscr, 0xFF);       // st lastscr
                Mem.wb(Vid_FullScreenTemp_b, ~Mem.ub(Vid_FullScreenTemp_b)); // not.b Vid_FullScreenTemp_b
                Mem.wb(C2pData.C2P_NeedsInit_b, 0xFF); // st C2P_NeedsInit_b
            }
        } else {
            // .notswapscr:
            Mem.wb(PlayerBss.lastscr, 0);              // clr.b lastscr
        }

        // .notswapscr2:
        if (Mem.b(a5 + RAWKEY_F7) != 0) {              // tst.b RAWKEY_F7(a5) ; beq.s .noframelimit
            Mem.wb(a5 + RAWKEY_F7, 0);                 // clr.b RAWKEY_F7(a5)
            if (Mem.w(Sys_FPSLimit_w) == 5) {          // cmp.w #5,Sys_FPSLimit_w ; beq.s .resetfpslimit
                // .resetfpslimit:
                Mem.ww(Sys_FPSLimit_w, -1);            // move.w #-1,Sys_FPSLimit_w
            } else {
                Mem.ww(Sys_FPSLimit_w, Mem.w(Sys_FPSLimit_w) + 1); // addq.w #1,Sys_FPSLimit_w
            }
        }

        // .noframelimit:
        if (Mem.b(a5 + RAWKEY_NUM_DOT) != 0) {         // tst.b RAWKEY_NUM_DOT(a5) ; beq.b .done_normal_keys
            Mem.wb(ControlloopData.Prefs_CrossHairColour_b,
                    Mem.ub(ControlloopData.Prefs_CrossHairColour_b) + 1); // add.b #1,Prefs_CrossHairColour_b
            Mem.wb(ControlloopData.Prefs_CrossHairColour_b,
                    Mem.ub(ControlloopData.Prefs_CrossHairColour_b) & 7); // and.b #7,Prefs_CrossHairColour_b
            Mem.wb(a5 + RAWKEY_NUM_DOT, 0);            // clr.b RAWKEY_NUM_DOT(a5)
        }

        // .done_normal_keys:
        // IFD DEV (build DEV actif dans le port)
        // .toggle_skip_sky_for_zone: X toggles the sky background visibility for this zone
        if (Mem.b(a5 + RAWKEY_X) != 0) {               // tst.b RAWKEY_X(a5) ; beq.s .clear_zone_data
            Mem.wb(a5 + RAWKEY_X, 0);                  // clr.b RAWKEY_X(a5)
            a1 = Zone_BackdropDisable_vb;              // lea Zone_BackdropDisable_vb,a1
            d0 = Mem.uw(a0 + PlrT_Zone_w);             // move.w PlrT_Zone_w(a0),d0
            d1 = setw(0, d0);                          // move.w d0,d1
            d0 = setw(d0, (d0 & 0xFFFF) >>> 3);        // lsr.w #3,d0 ; byte offset into backdrop disable table
            a1 += (short) d0;                          // add.w d0,a1
            Mem.wb(a1, Mem.ub(a1) ^ (1 << (d1 & 7)));  // bchg.b d1,(a1) ; d1 modulo 8, toggle the bit
        }

        // .clear_zone_data:
        if (Mem.b(a5 + RAWKEY_Z) != 0) {               // tst.b RAWKEY_Z(a5) ; beq.s .dev_toggles
            Mem.wb(a5 + RAWKEY_Z, 0);                  // clr.b RAWKEY_Z(a5)
            d0 = ZONE_BACKDROP_DISABLE_SIZE / 16 - 1;  // move.w #ZONE_BACKDROP_DISABLE_SIZE/16-1,d0
            a1 = Zone_BackdropDisable_vb;              // lea Zone_BackdropDisable_vb,a1
            do { // .clear_loop:
                Mem.wl(a1, 0); a1 += 4;                // clr.l (a1)+
                Mem.wl(a1, 0); a1 += 4;                // clr.l (a1)+
                Mem.wl(a1, 0); a1 += 4;                // clr.l (a1)+
                Mem.wl(a1, 0); a1 += 4;                // clr.l (a1)+
                d0 = setw(d0, d0 - 1);                 // dbra d0,.clear_loop
            } while ((short) d0 != -1);
        }

        // .dev_toggles: (DEV_CHECK_KEY key,flag : tst ; clr ; DEV_TOGGLE)
        devCheckKey(a5, RAWKEY_V, DevMacros.DEV_SKIP_DUMP_BG_DISABLE);
        devCheckKey(a5, RAWKEY_E, DevMacros.DEV_SKIP_SIMPLE_WALLS);
        devCheckKey(a5, RAWKEY_R, DevMacros.DEV_SKIP_SHADED_WALLS);
        devCheckKey(a5, RAWKEY_T, DevMacros.DEV_SKIP_BITMAPS);
        devCheckKey(a5, RAWKEY_Y, DevMacros.DEV_SKIP_GLARE_BITMAPS);
        devCheckKey(a5, RAWKEY_U, DevMacros.DEV_SKIP_ADDITIVE_BITMAPS);
        devCheckKey(a5, RAWKEY_I, DevMacros.DEV_SKIP_LIGHTSOURCED_BITMAPS);
        devCheckKey(a5, RAWKEY_O, DevMacros.DEV_SKIP_POLYGON_MODELS);
        devCheckKey(a5, RAWKEY_G, DevMacros.DEV_SKIP_FLATS);
        devCheckKey(a5, RAWKEY_Q, DevMacros.DEV_SKIP_FASTBUFFER_CLEAR);
        devCheckKey(a5, RAWKEY_N, DevMacros.DEV_SKIP_AI_ATTACK);
        devCheckKey(a5, RAWKEY_B, DevMacros.DEV_SKIP_LIGHTING);
        devCheckKey(a5, RAWKEY_H, DevMacros.DEV_SKIP_OVERLAY);
        devCheckKey(a5, RAWKEY_COMMA, DevMacros.DEV_ZONE_TRACE);
        devCheckKey(a5, RAWKEY_LSQR_BRKT, DevMacros.DEV_SKIP_EDGE_PVS);

        a1 = SinCosTable_vw;                           // move.l #SinCosTable_vw,a1
        a5 = KeyMap_vb;                                // move.l #KeyMap_vb,a5
        d0 = Mem.uw(HireswallData.STOPOFFSET);         // move.w STOPOFFSET,d0
        d7 = 0;                                        // moveq #0,d7
        d7 = setb(d7, Mem.ub(ControlloopData.look_up_key)); // move.b look_up_key,d7
        if (Mem.b(a5 + (short) d7) != 0) {             // tst.b (a5,d7.w) ; beq.s .skip_look_up
            Mem.ww(a0 + PlrT_AimSpeed_l, Mem.uw(a0 + PlrT_AimSpeed_l) - 512); // sub.w #512,PlrT_AimSpeed_l(a0)
            d0 = setw(d0, d0 - Mem.w(HiresData.View_KeyLook_w)); // sub.w View_KeyLook_w,d0
            if ((short) d0 <= Mem.w(HiresData.View_LookMax_w)) { // cmp.w View_LookMax_w,d0 ; bgt.s .skip_look_up
                Mem.ww(a0 + PlrT_AimSpeed_l, -512 * 20); // move.w #-512*20,PlrT_AimSpeed_l(a0)
                d0 = setw(d0, Mem.uw(HiresData.View_LookMax_w)); // move.w View_LookMax_w,d0
            }
        }
        // .skip_look_up:
        d7 = 0;                                        // moveq #0,d7
        d7 = setb(d7, Mem.ub(ControlloopData.look_down_key)); // move.b look_down_key,d7
        if (Mem.b(a5 + (short) d7) != 0) {             // tst.b (a5,d7.w) ; beq.s .skip_look_down
            Mem.ww(a0 + PlrT_AimSpeed_l, Mem.uw(a0 + PlrT_AimSpeed_l) + 512); // add.w #512,PlrT_AimSpeed_l(a0)
            d0 = setw(d0, d0 + Mem.w(HiresData.View_KeyLook_w)); // add.w View_KeyLook_w,d0
            if ((short) d0 >= Mem.w(HiresData.View_LookMin_w)) { // cmp.w View_LookMin_w,d0 ; blt.s .skip_look_down
                Mem.ww(a0 + PlrT_AimSpeed_l, 512 * 20); // move.w #512*20,PlrT_AimSpeed_l(a0)
                d0 = setw(d0, Mem.uw(HiresData.View_LookMin_w)); // move.w View_LookMin_w,d0
            }
        }
        // .skip_look_down:
        d7 = setb(d7, Mem.ub(ControlloopData.centre_view_key)); // move.b centre_view_key,d7
        if (Mem.b(a5 + (short) d7) != 0) {             // tst.b (a5,d7.w) ; beq.s .skip_centre_look
            if (Mem.b(HiresData.Plr_OldCentre_b) == 0) { // tst.b Plr_OldCentre_b ; bne.s .skip_centre_look_2
                Mem.wb(HiresData.Plr_OldCentre_b, 0xFF); // st Plr_OldCentre_b
                d0 = setw(d0, 0);                      // move.w #0,d0
                Mem.ww(a0 + PlrT_AimSpeed_l, 0);       // move.w #0,PlrT_AimSpeed_l(a0)
            }
        } else {
            // .skip_centre_look:
            Mem.wb(HiresData.Plr_OldCentre_b, 0);      // clr.b Plr_OldCentre_b
        }

        // .skip_centre_look_2:
        Mem.ww(HireswallData.STOPOFFSET, d0);          // move.w d0,STOPOFFSET
        d0 = negw(d0);                                 // neg.w d0
        d0 = setw(d0, d0 + Mem.w(HireswallData.TOTHEMIDDLE)); // add.w TOTHEMIDDLE,d0
        Mem.ww(HireswallData.SMIDDLEY, d0);            // move.w d0,SMIDDLEY
        d0 = muls(d0, SCREEN_WIDTH);                   // muls.w #SCREEN_WIDTH,d0
        Mem.wl(HireswallData.SBIGMIDDLEY, d0);         // move.l d0,SBIGMIDDLEY
        d0 = Mem.uw(a0 + PlrT_SnapAngPos_w);           // move.w PlrT_SnapAngPos_w(a0),d0
        d3 = Mem.uw(a0 + PlrT_SnapAngSpd_w);           // move.w PlrT_SnapAngSpd_w(a0),d3
        if (Mem.b(ControlloopData.Prefs_AlwaysRun_b) == 0) { // tst.b Prefs_AlwaysRun_b ; bne.s .always_run
            d1 = 35;                                   // move.w #35,d1
            d2 = 2;                                    // move.w #2,d2
            Mem.ww(PlayerBss.Plr_TurnSpeed_w, 10);     // move.w #10,Plr_TurnSpeed_w
            d7 = 0;                                    // moveq #0,d7
            d7 = setb(d7, Mem.ub(ControlloopData.run_key)); // move.b run_key,d7
            if (Mem.b(a5 + (short) d7) != 0) {         // tst.b (a5,d7.w) ; beq.s .nofaster
                d1 = 60;                               // move.w #60,d1
                d2 = 3;                                // move.w #3,d2
                Mem.ww(PlayerBss.Plr_TurnSpeed_w, 14); // move.w #14,Plr_TurnSpeed_w
            }
            // .nofaster: bra .faster
        } else {
            // .always_run:
            d1 = 60;                                   // move.w #60,d1
            d2 = 3;                                    // move.w #3,d2
            Mem.ww(PlayerBss.Plr_TurnSpeed_w, 14);     // move.w #14,Plr_TurnSpeed_w
            d7 = 0;                                    // moveq #0,d7
            d7 = setb(d7, Mem.ub(ControlloopData.run_key)); // move.b run_key,d7
            if (Mem.b(a5 + (short) d7) != 0) {         // tst.b (a5,d7.w) ; beq.s .faster
                d1 = 35;                               // move.w #35,d1
                d2 = 2;                                // move.w #2,d2
                Mem.ww(PlayerBss.Plr_TurnSpeed_w, 10); // move.w #10,Plr_TurnSpeed_w
            }
        }
        // .faster:
        // ***************************************************************
        if (Mem.b(a0 + PlrT_Squished_b) != 0           // tst.b PlrT_Squished_b(a0) ; bne.s .crouch_2
                || Mem.b(a0 + PlrT_Ducked_b) != 0) {   // tst.b PlrT_Ducked_b(a0) ; beq.s .skip_crouch
            // .crouch_2:
            d2 = setw(d2, ((short) d2) >> 1);          // asr.w #1,d2
        }

        // .skip_crouch:
        d4 = 0;                                        // moveq #0,d4
        if (Mem.b(PlayerBss.Plr_Decelerate_b) != 0) {  // tst.b Plr_Decelerate_b ; beq.s .nofric
            d5 = setw(0, d3);                          // move.w d3,d5
            d5 = setw(d5, d5 + d5);                    // add.w d5,d5
            d3 = setw(d3, d3 + d5);                    // add.w d5,d3
            d3 = setw(d3, ((short) d3) >> 2);          // asr.w #2,d3
            if ((short) d3 < 0) {                      // bge.s .nneg
                d3 = setw(d3, d3 + 1);                 // addq #1,d3
            }
            // .nneg:
        }
        // .nofric:
        Mem.wb(ControlloopData.templeftkey, Mem.ub(ControlloopData.turn_left_key));      // move.b turn_left_key,templeftkey
        Mem.wb(ControlloopData.temprightkey, Mem.ub(ControlloopData.turn_right_key));    // move.b turn_right_key,temprightkey
        Mem.wb(ControlloopData.tempslkey, Mem.ub(ControlloopData.sidestep_left_key));    // move.b sidestep_left_key,tempslkey
        Mem.wb(ControlloopData.tempsrkey, Mem.ub(ControlloopData.sidestep_right_key));   // move.b sidestep_right_key,tempsrkey
        d7 = setb(d7, Mem.ub(ControlloopData.force_sidestep_key)); // move.b force_sidestep_key,d7
        if (Mem.b(a5 + (short) d7) != 0) {             // tst.b (a5,d7.w) ; beq .skip_force_sidestep
            Mem.wb(ControlloopData.tempslkey, Mem.ub(ControlloopData.templeftkey));   // move.b templeftkey,tempslkey
            Mem.wb(ControlloopData.tempsrkey, Mem.ub(ControlloopData.temprightkey)); // move.b temprightkey,tempsrkey
            Mem.wb(ControlloopData.templeftkey, 255);  // move.b #255,templeftkey
            Mem.wb(ControlloopData.temprightkey, 255); // move.b #255,temprightkey
        }

        // .skip_force_sidestep:
        if (Mem.b(PlayerBss.Plr_Decelerate_b) != 0) {  // tst.b Plr_Decelerate_b ; beq.s .turn_not_possible
            d7 = setb(d7, Mem.ub(ControlloopData.templeftkey)); // move.b templeftkey,d7
            if (Mem.b(a5 + (short) d7) != 0) {         // tst.b (a5,d7.w) ; beq.s .skip_turn_left
                d3 = setw(d3, d3 - Mem.w(PlayerBss.Plr_TurnSpeed_w)); // sub.w Plr_TurnSpeed_w,d3
            }
            // .skip_turn_left:
            a5 = KeyMap_vb;                            // move.l #KeyMap_vb,a5
            d7 = setb(d7, Mem.ub(ControlloopData.temprightkey)); // move.b temprightkey,d7
            if (Mem.b(a5 + (short) d7) != 0) {         // tst.b (a5,d7.w) ; beq.s .skip_turn_right
                d3 = setw(d3, d3 + Mem.w(PlayerBss.Plr_TurnSpeed_w)); // add.w Plr_TurnSpeed_w,d3
            }
            // .skip_turn_right:
            if ((short) d3 > (short) d1) {             // cmp.w d1,d3 ; ble.s .right_speed_ok
                d3 = setw(d3, d1);                     // move.w d1,d3
            }
            // .right_speed_ok:
            d1 = negw(d1);                             // neg.w d1
            if ((short) d3 < (short) d1) {             // cmp.w d1,d3 ; bge.s .left_speed_ok
                d3 = setw(d3, d1);                     // move.w d1,d3
            }
            // .left_speed_ok:
        }
        // .turn_not_possible:
        d0 = setw(d0, d0 + d3);                        // add.w d3,d0
        d0 = setw(d0, d0 + d3);                        // add.w d3,d0
        Mem.ww(a0 + PlrT_SnapAngSpd_w, d3);            // move.w d3,PlrT_SnapAngSpd_w(a0)
        d7 = setb(d7, Mem.ub(ControlloopData.tempslkey)); // move.b tempslkey,d7
        if (Mem.b(a5 + (short) d7) != 0) {             // tst.b (a5,d7.w) ; beq.s .skip_step_left
            d4 = setw(d4, d4 + d2);                    // add.w d2,d4
            d4 = setw(d4, d4 + d2);                    // add.w d2,d4
            d4 = setw(d4, ((short) d4) >> 1);          // asr.w #1,d4
        }
        // .skip_step_left:
        a5 = KeyMap_vb;                                // move.l #KeyMap_vb,a5
        d7 = setb(d7, Mem.ub(ControlloopData.tempsrkey)); // move.b tempsrkey,d7
        if (Mem.b(a5 + (short) d7) != 0) {             // tst.b (a5,d7.w) ; beq.s .skip_step_right
            d4 = setw(d4, d4 + d2);                    // add.w d2,d4
            d4 = setw(d4, d4 + d2);                    // add.w d2,d4
            d4 = setw(d4, ((short) d4) >> 1);          // asr.w #1,d4
            d4 = negw(d4);                             // neg.w d4
        }
        // .skip_step_right:
        d0 = TablesData.AMOD_I(d0);                    // AMOD_I d0
        Mem.ww(a0 + PlrT_SnapAngPos_w, d0);            // move.w d0,PlrT_SnapAngPos_w(a0)
        Mem.ww(a0 + PlrT_SnapSinVal_w, Mem.uw(a1 + (short) d0)); // move.w (a1,d0.w),PlrT_SnapSinVal_w(a0)
        a1 += COSINE_OFS;                              // adda.w #COSINE_OFS,a1
        Mem.ww(a0 + PlrT_SnapCosVal_w, Mem.uw(a1 + (short) d0)); // move.w (a1,d0.w),PlrT_SnapCosVal_w(a0)
        d6 = Mem.l(a0 + PlrT_SnapXSpdVal_l);           // move.l PlrT_SnapXSpdVal_l(a0),d6
        d7 = Mem.l(a0 + PlrT_SnapZSpdVal_l);           // move.l PlrT_SnapZSpdVal_l(a0),d7
        if (Mem.b(PlayerBss.Plr_Decelerate_b) != 0) {  // tst.b Plr_Decelerate_b ; beq.s .skip_friction
            d6 = -d6;                                  // neg.l d6
            if (d6 > 0) {                              // ble.s .nobug1
                d6 >>= 3;                              // asr.l #3,d6
                d6 += 1;                               // add.l #1,d6
            } else {
                // .nobug1:
                d6 >>= 3;                              // asr.l #3,d6
            }
            // .bug1:
            d7 = -d7;                                  // neg.l d7
            if (d7 > 0) {                              // ble.s .nobug2
                d7 >>= 3;                              // asr.l #3,d7
                d7 += 1;                               // add.l #1,d7
            } else {
                // .nobug2:
                d7 >>= 3;                              // asr.l #3,d7
            }
            // .bug2:
        }
        // .skip_friction:
        d3 = 0;                                        // moveq #0,d3
        d5 = 0;                                        // moveq #0,d5
        d5 = setb(d5, Mem.ub(ControlloopData.forward_key)); // move.b forward_key,d5
        if (Mem.b(a5 + (short) d5) != 0) {             // tst.b (a5,d5.w) ; beq.s .noforward
            d2 = negw(d2);                             // neg.w d2
            d3 = setw(d3, d2);                         // move.w d2,d3
        }
        // .noforward:
        d5 = setb(d5, Mem.ub(ControlloopData.backward_key)); // move.b backward_key,d5
        if (Mem.b(a5 + (short) d5) != 0) {             // tst.b (a5,d5.w) ; beq.s .nobackward
            d3 = setw(d3, d2);                         // move.w d2,d3
        }
        // .nobackward:
        d2 = setw(d2, d3);                             // move.w d3,d2
        d2 = setw(d2, d2 << 6);                        // asl.w #6,d2
        d1 = setw(0, d2);                              // move.w d2,d1
        Mem.ww(PlayerBss.Plr_AddToBobble_w, d1);       // move.w d1,Plr_AddToBobble_w
        d1 = Mem.uw(a0 + PlrT_SnapSinVal_w);           // move.w PlrT_SnapSinVal_w(a0),d1
        d1 = muls(d1, d3);                             // muls.w d3,d1
        d2 = Mem.uw(a0 + PlrT_SnapCosVal_w);           // move.w PlrT_SnapCosVal_w(a0),d2
        d2 = muls(d2, d3);                             // muls.w d3,d2
        d6 -= d1;                                      // sub.l d1,d6
        d7 -= d2;                                      // sub.l d2,d7
        d1 = Mem.uw(a0 + PlrT_SnapSinVal_w);           // move.w PlrT_SnapSinVal_w(a0),d1
        d1 = muls(d1, d4);                             // muls.w d4,d1
        d2 = Mem.uw(a0 + PlrT_SnapCosVal_w);           // move.w PlrT_SnapCosVal_w(a0),d2
        d2 = muls(d2, d4);                             // muls.w d4,d2
        d6 -= d2;                                      // sub.l d2,d6
        d7 += d1;                                      // add.l d1,d7
        if (Mem.b(PlayerBss.Plr_Decelerate_b) != 0) {  // tst.b Plr_Decelerate_b ; beq.s .no_control_possible
            Mem.wl(a0 + PlrT_SnapXSpdVal_l, Mem.l(a0 + PlrT_SnapXSpdVal_l) + d6); // add.l d6,PlrT_SnapXSpdVal_l(a0)
            Mem.wl(a0 + PlrT_SnapZSpdVal_l, Mem.l(a0 + PlrT_SnapZSpdVal_l) + d7); // add.l d7,PlrT_SnapZSpdVal_l(a0)
        }
        // .no_control_possible:
        d6 = Mem.l(a0 + PlrT_SnapXSpdVal_l);           // move.l PlrT_SnapXSpdVal_l(a0),d6
        d7 = Mem.l(a0 + PlrT_SnapZSpdVal_l);           // move.l PlrT_SnapZSpdVal_l(a0),d7
        Mem.wl(a0 + PlrT_SnapXOff_l, Mem.l(a0 + PlrT_SnapXOff_l) + d6); // add.l d6,PlrT_SnapXOff_l(a0)
        Mem.wl(a0 + PlrT_SnapZOff_l, Mem.l(a0 + PlrT_SnapZOff_l) + d7); // add.l d7,PlrT_SnapZOff_l(a0)
        d5 = setb(d5, Mem.ub(ControlloopData.fire_key)); // move.b fire_key,d5
        if (Mem.b(a0 + PlrT_Fire_b) != 0) {            // tst.b PlrT_Fire_b(a0) ; beq.s .firenotpressed
            // fire was pressed last time.
            if (Mem.b(a5 + (short) d5) != 0) {         // tst.b (a5,d5.w) ; beq.s .firenownotpressed
                // fire is still pressed this time.
                Mem.wb(a0 + PlrT_Fire_b, 0xFF);        // st PlrT_Fire_b(a0)
                return;                                // bra .done
            }
            // .firenownotpressed: fire has been released.
            Mem.wb(a0 + PlrT_Fire_b, 0);               // clr.b PlrT_Fire_b(a0)
            return;                                    // bra .done
        }
        // .firenotpressed: fire was not pressed last frame...
        if (Mem.b(a5 + (short) d5) == 0) {             // tst.b (a5,d5.w) ; beq.s .firenownotpressed
            Mem.wb(a0 + PlrT_Fire_b, 0);               // clr.b PlrT_Fire_b(a0)
            return;
        }
        // fire was not pressed last time, and was this time, so has been clicked.
        Mem.wb(a0 + PlrT_Clicked_b, 0xFF);             // st PlrT_Clicked_b(a0)
        Mem.wb(a0 + PlrT_Fire_b, 0xFF);                // st PlrT_Fire_b(a0)
        // .done: rts
    }

    /** DEV_CHECK_KEY \1,\2 : tst.b key(a5) ; beq skip ; clr.b ; DEV_TOGGLE flag */
    private static void devCheckKey(int a5, int rawkey, int devBit) {
        if (Mem.b(a5 + rawkey) != 0) {
            Mem.wb(a5 + rawkey, 0);
            DevMacros.DEV_TOGGLE(DevInst.Dev_DebugFlags_l, devBit);
        }
    }

    // ******************************************************************************
    // * Show gun name — pointer to player data in a0
    // ******************************************************************************

    /** plr_ShowGunName */
    public static void plr_ShowGunName(int a0) {
        // moveq #0,d2 (non utilisé ensuite)
        int d0 = Mem.ub(a0 + PlrT_GunSelected_b);      // move.b PlrT_GunSelected_b(a0),d0
        int a4 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a4
        a4 += GLFT_GunNames_l;                         // add.l #GLFT_GunNames_l,a4
        d0 = muls(d0, GLFT_GUN_NAME_LENGTH);           // muls #GLFT_GUN_NAME_LENGTH,d0
        a4 += d0;                                      // add.l d0,a4
        // exg a0,a4 ; CALLC Msg_PushLineDedupLast ; move.l a4,a0
        Message.Msg_PushLineDedupLast(a4, GLFT_GUN_NAME_LENGTH | MSG_TAG_OTHER); // move.w #...,d0
        // .done: rts
    }

    // ******************************************************************************
    // * Falling down... — pointer to player data in a0
    // ******************************************************************************

    /** plr_Fall */
    public static void plr_Fall(int a0) {
        int d0 = Mem.l(a0 + PlrT_SnapTYOff_l);         // move.l PlrT_SnapTYOff_l(a0),d0
        int d1 = Mem.l(a0 + PlrT_SnapYOff_l);          // move.l PlrT_SnapYOff_l(a0),d1
        int d2 = Mem.l(a0 + PlrT_SnapYVel_l);          // move.l PlrT_SnapYVel_l(a0),d2
        int d3, d4, a2, a4;

        if (d0 < d1) {                                 // cmp.l d1,d0 ; bgt .above_ground ; beq.s .on_ground
            // we are under the ground.
            Mem.wb(PlayerBss.Plr_Decelerate_b, 0xFF);  // st Plr_Decelerate_b
            d0 -= d1;                                  // sub.l d1,d0
            if (d0 < -512) {                           // cmp.l #-512,d0 ; bge.s .not_too_big
                d0 = -512;                             // move.l #-512,d0
            }
            // .not_too_big:
            d1 += d0;                                  // add.l d0,d1
            proceed(a0, d1, d2);                       // bra .proceed
            return;
        }
        if (d0 == d1) {
            // .on_ground:
            d2 = Mem.w(a0 + PlrT_FloorSpd_w);          // move.w PlrT_FloorSpd_w(a0),d2 ; ext.l d2
            d2 <<= 6;                                  // asl.l #6,d2
            a4 = Mem.l(a0 + PlrT_ObjectPtr_l);         // move.l PlrT_ObjectPtr_l(a0),a4
            d3 = Mem.w(PlayerBss.plr_FallDamage_w);    // move.w plr_FallDamage_w,d3
            d3 = setw(d3, d3 - 100);                   // sub.w #100,d3 ; TODO - should depend on the distance fallen.
            if ((short) d3 > 0) {                      // ble.s .skip_damage
                Mem.wb(a4 + EntT_DamageTaken_b, Mem.ub(a4 + EntT_DamageTaken_b) + d3); // add.b d3,EntT_DamageTaken_b(a4)
            }
            // .skip_damage:
            Mem.wb(PlayerBss.Plr_Decelerate_b, 0xFF);  // st Plr_Decelerate_b
            Mem.ww(PlayerBss.plr_FallDamage_w, 0);     // move.w #0,plr_FallDamage_w
            d3 = Mem.uw(PlayerBss.Plr_AddToBobble_w);  // move.w Plr_AddToBobble_w,d3
            d4 = setw(0, d3);                          // move.w d3,d4
            d3 = setw(d3, d3 + Mem.uw(a0 + PlrT_Bobble_w)); // add.w PlrT_Bobble_w(a0),d3
            d3 = TablesData.AMOD_A(d3);                // AMOD_A d3
            Mem.ww(a0 + PlrT_Bobble_w, d3);            // move.w d3,PlrT_Bobble_w(a0)
            d4 = setw(d4, d4 + Mem.uw(a0 + PlrT_WalkSFXTime_w)); // add.w PlrT_WalkSFXTime_w(a0),d4
            d3 = setw(d3, d4);                         // move.w d4,d3
            d4 = setw(d4, d4 & 4095);                  // and.w #4095,d4
            Mem.ww(a0 + PlrT_WalkSFXTime_w, d4);       // move.w d4,PlrT_WalkSFXTime_w(a0)
            d3 = setw(d3, d3 & -4096);                 // and.w #-4096,d3
            if ((short) d3 != 0) {                     // beq.s .skip_footstep_fx
                plr_DoFootstepFX(a0);                  // bsr plr_DoFootstepFX
            }
            // .skip_footstep_fx:
            Mem.wl(PlayerBss.plr_JumpSpeed_l, -1024);  // move.l #-1024,plr_JumpSpeed_l

            a2 = Mem.l(a0 + PlrT_ZonePtr_l);           // move.l PlrT_ZonePtr_l(a0),a2
            int dw = Mem.l(a2 + ZoneT_Water_l);        // move.l ZoneT_Water_l(a2),d0
            if (d1 >= dw) {                            // cmp.l d0,d1 ; blt.s .not_in_water
                Mem.wl(PlayerBss.plr_JumpSpeed_l, -512); // move.l #-512,plr_JumpSpeed_l
            }
            // .not_in_water:
            if (Mem.w(a0 + PlrT_Health_w) > 0) {       // tst.w PlrT_Health_w(a0) ; ble.s .no_thrust ; dead dudes don't jump
                int a5 = KeyMap_vb;                    // move.l #KeyMap_vb,a5
                int d7 = 0;                            // moveq #0,d7
                d7 = setb(d7, Mem.ub(ControlloopData.jump_key)); // move.b jump_key,d7
                if (Mem.b(a5 + (short) d7) != 0) {     // tst.b (a5,d7.w) ; beq.s .no_thrust
                    d2 = Mem.l(PlayerBss.plr_JumpSpeed_l); // move.l plr_JumpSpeed_l,d2
                }
            }
            // .no_thrust:
            if (d2 > 0) {                              // tst.l d2 ; ble.s .no_down
                d2 = 0;                                // moveq #0,d2
            }
            // .no_down:
            d1 += d2;                                  // add.l d2,d1
            proceed(a0, d1, d2);                       // bra .proceed
            return;
        }

        // .above_ground:
        Mem.wb(PlayerBss.Plr_Decelerate_b, 0);         // clr.b Plr_Decelerate_b
        if (Mem.w(a0 + PlrT_Jetpack_w) != 0            // tst.w PlrT_Jetpack_w(a0) ; beq.s .not_flying
                && Mem.w(a0 + PlrT_JetpackFuel_w) != 0) { // tst.w PlrT_JetpackFuel_w(a0) ; beq.s .not_flying
            // TODO (original) - Apply the active jetpack fuel cap here (mod configurable)
            if (Mem.w(a0 + PlrT_JetpackFuel_w) > 250) { // cmp.w #250,PlrT_JetpackFuel_w(a0) ; ble.s .have_jetpack_fuel
                Mem.ww(a0 + PlrT_JetpackFuel_w, 250);  // move.w #250,PlrT_JetpackFuel_w(a0)
            }
            // .have_jetpack_fuel:
            Mem.wb(PlayerBss.Plr_Decelerate_b, 0xFF);  // st Plr_Decelerate_b
            Mem.wl(PlayerBss.plr_JumpSpeed_l, -128);   // move.l #-128,plr_JumpSpeed_l
            int a5 = KeyMap_vb;                        // move.l #KeyMap_vb,a5
            int d7 = 0;                                // moveq #0,d7
            d7 = setb(d7, Mem.ub(ControlloopData.jump_key)); // move.b jump_key,d7
            if (Mem.b(a5 + (short) d7) != 0) {         // tst.b (a5,d7.w) ; beq.s .not_flying
                Mem.ww(a0 + PlrT_JetpackFuel_w, Mem.w(a0 + PlrT_JetpackFuel_w) - 1); // sub.w #1,PlrT_JetpackFuel_w(a0)
                d2 += Mem.l(PlayerBss.plr_JumpSpeed_l); // add.l plr_JumpSpeed_l,d2
                Mem.ww(PlayerBss.plr_FallDamage_w, 0); // move.w #0,plr_FallDamage_w
                d3 = 40;                               // move.w #40,d3
                d3 = setw(d3, d3 + Mem.uw(a0 + PlrT_Bobble_w)); // add.w PlrT_Bobble_w(a0),d3
                d3 = TablesData.AMOD_A(d3);            // AMOD_A d3
                Mem.ww(a0 + PlrT_Bobble_w, d3);        // move.w d3,PlrT_Bobble_w(a0)
            }
        }

        // .not_flying:
        d3 = d0;                                       // move.l d0,d3
        d3 -= d1;                                      // sub.l d1,d3
        if (d3 <= 16 * 64) {                           // cmp.l #16*64,d3 ; bgt.s .nonearmove
            Mem.wb(PlayerBss.Plr_Decelerate_b, 0xFF);  // st Plr_Decelerate_b
        }
        // .nonearmove: need to fall down (possibly).
        d1 += d2;                                      // add.l d2,d1
        if (d0 <= d1) {                                // cmp.l d1,d0 ; bgt.s .still_above
            d3 = Mem.w(PlayerBss.plr_FallDamage_w);    // move.w plr_FallDamage_w,d3
            d3 = setw(d3, d3 - 100);                   // sub.w #100,d3
            if ((short) d3 > 0) {                      // ble.s .skip_damage_2
                a4 = Mem.l(a0 + PlrT_ObjectPtr_l);     // move.l PlrT_ObjectPtr_l(a0),a4
                Mem.wb(a4 + EntT_DamageTaken_b, Mem.ub(a4 + EntT_DamageTaken_b) + d3); // add.b d3,EntT_DamageTaken_b(a4)
            }
            // .skip_damage_2:
            Mem.ww(PlayerBss.plr_FallDamage_w, 0);     // move.w #0,plr_FallDamage_w
            d2 = Mem.w(a0 + PlrT_FloorSpd_w);          // move.w PlrT_FloorSpd_w(a0),d2 ; ext.l d2
            d2 <<= 6;                                  // asl.l #6,d2
            proceed(a0, d1, d2);                       // bra .proceed
            return;
        }

        // .still_above:
        d2 += 64;                                      // add.l #64,d2
        Mem.ww(PlayerBss.plr_FallDamage_w, Mem.uw(PlayerBss.plr_FallDamage_w) + 1); // add.w #1,plr_FallDamage_w

        a2 = Mem.l(a0 + PlrT_ZonePtr_l);               // move.l PlrT_ZonePtr_l(a0),a2
        d0 = Mem.l(a2 + ZoneT_Water_l);                // move.l ZoneT_Water_l(a2),d0
        if (d1 >= d0) {                                // cmp.l d0,d1 ; blt.s .proceed
            if (d0 >= Mem.l(PlayerBss.plr_OldHeight_l)) { // cmp.l plr_OldHeight_l,d0 ; blt.s .no_splash_fx
                // SAVEREGS
                Mem.ww(HiresData.Aud_SampleNum_w, 6);  // move.w #6,Aud_SampleNum_w ; todo define a constant
                Mem.ww(HiresData.Aud_NoiseX_w, 0);     // move.w #0,Aud_NoiseX_w
                Mem.ww(HiresData.Aud_NoiseZ_w, 100);   // move.w #100,Aud_NoiseZ_w
                Mem.ww(HiresData.Aud_NoiseVol_w, 80);  // move.w #80,Aud_NoiseVol_w
                Mem.ww(HiresData.IDNUM, 0xfff8);       // move.w #$fff8,IDNUM
                Mem.wb(HiresData.notifplaying, 0);     // clr.b notifplaying
                Hires.MakeSomeNoise();                 // jsr MakeSomeNoise
                // GETREGS
            }
            // .no_splash_fx:
            Mem.wb(PlayerBss.Plr_Decelerate_b, 0xFF);  // st Plr_Decelerate_b
            Mem.ww(PlayerBss.plr_FallDamage_w, 0);     // move.w #0,plr_FallDamage_w
            if (d2 >= 512) {                           // cmp.l #512,d2 ; blt.s .proceed
                d2 = 512;                              // move.l #512,d2 ; reached terminal velocity.
            }
        }
        // .proceed:
        proceed(a0, d1, d2);
    }

    /** .proceed (fin de plr_Fall) */
    private static void proceed(int a0, int d1, int d2) {
        int a2 = Mem.l(a0 + PlrT_ZonePtr_l);           // move.l PlrT_ZonePtr_l(a0),a2
        int d3 = Mem.l(a2 + ZoneT_Roof_l);             // move.l ZoneT_Roof_l(a2),d3
        if (Mem.b(a0 + PlrT_StoodInTop_b) != 0) {      // tst.b PlrT_StoodInTop_b(a0) ; beq.s .ok_bottom
            d3 = Mem.l(a2 + ZoneT_UpperRoof_l);        // move.l ZoneT_UpperRoof_l(a2),d3
        }
        // .ok_bottom:
        d3 += 10 * 256;                                // add.l #10*256,d3
        if (d3 >= d1) {                                // cmp.l d1,d3 ; blt.s .ok_ceiling
            d1 = d3;                                   // move.l d3,d1
            if (d2 < 0) {                              // tst.l d2 ; bge.s .ok_ceiling
                d2 = 0;                                // moveq #0,d2
            }
        }
        // .ok_ceiling:
        Mem.wl(a0 + PlrT_SnapYVel_l, d2);              // move.l d2,PlrT_SnapYVel_l(a0)
        Mem.wl(a0 + PlrT_SnapYOff_l, d1);              // move.l d1,PlrT_SnapYOff_l(a0)
        // rts
    }

    // ******************************************************************************
    // * Do footstep sounds — pointer to player data in a0
    // ******************************************************************************

    /** plr_DoFootstepFX */
    public static void plr_DoFootstepFX(int a0) {
        // SAVEREGS
        int a1 = Mem.l(a0 + PlrT_ZonePtr_l);           // move.l PlrT_ZonePtr_l(a0),a1
        int d0 = Mem.uw(a1 + ZoneT_FloorNoise_w);      // move.w ZoneT_FloorNoise_w(a1),d0
        int d1 = Mem.l(a1 + ZoneT_Water_l);            // move.l ZoneT_Water_l(a1),d1
        boolean haveWater = false;
        if (d1 < Mem.l(a1 + ZoneT_Floor_l)             // cmp.l ZoneT_Floor_l(a1),d1 ; bge.s .no_water
                && d1 >= Mem.l(a0 + PlrT_YOff_l)       // cmp.l PlrT_YOff_l(a0),d1 ; blt.s .no_water
                && Mem.b(a0 + PlrT_StoodInTop_b) == 0) { // tst.b PlrT_StoodInTop_b(a0) ; bne.s .no_water
            d0 = 6;                                    // move.w #6,d0
            haveWater = true;                          // bra.s .have_water
        }

        if (!haveWater) {
            // .no_water:
            if (Mem.b(a0 + PlrT_StoodInTop_b) != 0) {  // tst.b PlrT_StoodInTop_b(a0) ; beq.s .okinbot
                d0 = Mem.uw(a1 + ZoneT_UpperFloorNoise_w); // move.w ZoneT_UpperFloorNoise_w(a1),d0
            }
            // .okinbot:
            a1 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a1
            a1 += GLFT_FloorData_l;                    // add.l #GLFT_FloorData_l,a1
            d0 = Mem.uw(a1 + ((short) d0) * 4 + 2);    // move.w 2(a1,d0.w*4),d0 ; sample number.

            d0 = setw(d0, d0 - 1);                     // subq #1,d0
            if ((short) d0 < 0) {                      // blt.s .no_foot_sound
                // .no_foot_sound: GETREGS ; rts
                return;
            }
        }
        // .have_water:
        Mem.ww(HiresData.Aud_SampleNum_w, d0);         // move.w d0,Aud_SampleNum_w
        Mem.ww(HiresData.Aud_NoiseX_w, 0);             // move.w #0,Aud_NoiseX_w
        Mem.ww(HiresData.Aud_NoiseZ_w, 100);           // move.w #100,Aud_NoiseZ_w
        Mem.ww(HiresData.Aud_NoiseVol_w, 80);          // move.w #80,Aud_NoiseVol_w
        Mem.ww(HiresData.IDNUM, 0xfff8);               // move.w #$fff8,IDNUM
        Mem.wb(HiresData.notifplaying, 0);             // clr.b notifplaying
        Mem.wb(HiresData.SourceEcho, Mem.ub(a0 + PlrT_Echo_b)); // move.b PlrT_Echo_b(a0),SourceEcho
        Hires.MakeSomeNoise();                         // jsr MakeSomeNoise
        // GETREGS ; rts
    }
}
