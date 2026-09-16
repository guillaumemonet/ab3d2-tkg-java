package ab3d2;

import static ab3d2.Defs.*;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.mulu;
import static ab3d2.M68k.divs;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.swap;
import static ab3d2.M68k.extw;
import static ab3d2.HireswallData.Vid_CentreY_w;
import static ab3d2.HireswallData.Vis_CosVal_w;
import static ab3d2.HireswallData.Vis_SinVal_w;
import static ab3d2.bss.AnimBss.anim_TimeToNoise_w;
import static ab3d2.bss.AnimBss.anim_OddEven_w;
import static ab3d2.bss.AnimBss.Anim_TempFrames_w;
import static ab3d2.bss.AnimBss.anim_CurrentLiftable_w;
import static ab3d2.bss.AnimBss.anim_LiftOnlyLocks_w;
import static ab3d2.bss.AnimBss.anim_OpeningSpeed_w;
import static ab3d2.bss.AnimBss.anim_ClosingSpeed_w;
import static ab3d2.bss.AnimBss.anim_OpenDuration_w;
import static ab3d2.bss.AnimBss.anim_OpeningSoundFX_w;
import static ab3d2.bss.AnimBss.anim_ClosingSoundFX_w;
import static ab3d2.bss.AnimBss.anim_OpenedSoundFX_w;
import static ab3d2.bss.AnimBss.anim_ClosedSoundFX_w;
import static ab3d2.bss.AnimBss.anim_ActionSoundFX_w;
import static ab3d2.bss.AnimBss.anim_FloorMoveSpeed_w;
import static ab3d2.bss.AnimBss.Anim_DoorAndLiftLocks_l;
import static ab3d2.bss.AnimBss.Anim_BrightTable_vw;
import static ab3d2.bss.LevelBss.Lvl_LiftDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_DoorDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_SwitchDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_GraphicsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneEdgePtr_l;
import static ab3d2.bss.TablesBss.anim_LiftHeightTable_vw;
import static ab3d2.bss.TablesBss.anim_DoorOpenTimers_vw;
import static ab3d2.bss.ZoneBss.Zone_CurrentDoorState_w;
import static ab3d2.bss.PlayerBss.Plr1_TmpXOff_l;
import static ab3d2.bss.PlayerBss.Plr1_TmpZOff_l;
import static ab3d2.bss.PlayerBss.Plr2_TmpXOff_l;
import static ab3d2.bss.PlayerBss.Plr2_TmpZOff_l;
import static ab3d2.bss.PlayerBss.Plr1_TmpSpcTap_b;
import static ab3d2.bss.PlayerBss.Plr2_TmpSpcTap_b;
import static ab3d2.bss.PlayerBss.plr1_StoodOnLift_b;
import static ab3d2.bss.PlayerBss.plr2_StoodOnLift_b;
import static ab3d2.bss.PlayerBss.Plr1_ZonePtr_l;
import static ab3d2.bss.PlayerBss.Plr2_ZonePtr_l;
import static ab3d2.bss.PlayerBss.Plr1_FloorSpd_w;
import static ab3d2.bss.PlayerBss.Plr2_FloorSpd_w;
import static ab3d2.bss.PlayerBss.Plr1_Zone_w;
import static ab3d2.bss.PlayerBss.Plr2_Zone_w;
import static ab3d2.HiresData.ZonePtr_l;
import static ab3d2.HiresData.GLF_DatabasePtr_l;
import static ab3d2.HiresData.Aud_SampleNum_w;
import static ab3d2.HiresData.Aud_NoiseX_w;
import static ab3d2.HiresData.Aud_NoiseZ_w;
import static ab3d2.HiresData.Aud_NoiseVol_w;
import static ab3d2.HiresData.Aud_ChannelPick_b;
import static ab3d2.HiresData.IDNUM;
import static ab3d2.HiresData.notifplaying;
import static ab3d2.HiresData.PlayEcho;
import static ab3d2.NewanimsData.anim_LiftAtTop_b;
import static ab3d2.NewanimsData.anim_LiftAtBottom_b;
import static ab3d2.NewanimsData.anim_DoorOpen_b;
import static ab3d2.NewanimsData.anim_DoorClosed_b;
import static ab3d2.NewanimsData.Conditions;
import static ab3d2.NewanimsData.BRIGHT_ANIM_END;
import static ab3d2.NewanimsData.anim_BrightessAnimPtrs_vl;
import static ab3d2.NewanimsData.anim_BrightnessAnimStartPtrs_vl;
import static ab3d2.NewanimsData.timeout;
import static ab3d2.NewanimsData.anim_Brightness_w;
import static ab3d2.NewanimsData.BLOODYGREATBOMB;
import static ab3d2.NewanimsData.MOVING;
import static ab3d2.NewanimsData.sqrnum;
import static ab3d2.NewanimsData.RipTear;
import static ab3d2.NewanimsData.otherrip;
import static ab3d2.ObjectmoveData.oldx;
import static ab3d2.ObjectmoveData.oldy;
import static ab3d2.ObjectmoveData.oldz;
import static ab3d2.ObjectmoveData.newy;
import static ab3d2.ObjectmoveData.Viewerx;
import static ab3d2.ObjectmoveData.Viewery;
import static ab3d2.ObjectmoveData.Viewerz;
import static ab3d2.ObjectmoveData.ViewerTop;
import static ab3d2.ObjectmoveData.StoodInTop;
import static ab3d2.ObjectmoveData.Range;
import static ab3d2.ObjectmoveData.xdiff;
import static ab3d2.ObjectmoveData.zdiff;
import static ab3d2.ObjectmoveData.WallXSize_w;
import static ab3d2.ObjectmoveData.WallZSize_w;
import static ab3d2.ObjectmoveData.WallLength_w;
import static ab3d2.ObjectmoveData.Obj_ExtLen_w;
import static ab3d2.ObjectmoveData.Obj_AwayFromWall_b;
import static ab3d2.ObjectmoveData.Obj_WallBounce_b;
import static ab3d2.ObjectmoveData.exitfirst;
import static ab3d2.ObjectmoveData.wallflags;
import static ab3d2.ObjectmoveData.StepUpVal;
import static ab3d2.ObjectmoveData.StepDownVal;
import static ab3d2.ObjectmoveData.thingheight;
import static ab3d2.ObjectmoveData.Obj_ZonePtr_l;
import static ab3d2.ObjectmoveData.hitwall;
import static ab3d2.ObjectmoveData.wallhitheight;
import static ab3d2.bss.AnimBss.Anim_Timer_w;
import static ab3d2.bss.AnimBss.anim_DoneFlames_w;
import static ab3d2.bss.AnimBss.anim_MaxDamage_w;
import static ab3d2.bss.AnimBss.anim_MiddleRoom_l;
import static ab3d2.bss.AnimBss.anim_MiddleX_w;
import static ab3d2.bss.AnimBss.anim_MiddleZ_w;
import static ab3d2.ObjectmoveData.Obj_FromZonePtr_l;
import static ab3d2.ObjectmoveData.Obj_ToZonePtr_l;
import static ab3d2.ObjectmoveData.Targetx;
import static ab3d2.ObjectmoveData.Targety;
import static ab3d2.ObjectmoveData.Targetz;
import static ab3d2.ObjectmoveData.TargetTop;
import static ab3d2.ObjectmoveData.CanSee;
import static ab3d2.bss.PlayerBss.Plr_ShotDataPtr_l;
import static ab3d2.bss.TablesBss.ObjectWorkspace_vl;
import static ab3d2.bss.TablesBss.WorkspacePtr_l;
import static ab3d2.bss.TablesBss.ObjRotated_vl;
import static ab3d2.bss.AiBss.AI_Damaged_vw;
import static ab3d2.bss.AiBss.AI_DamagePtr_l;
import static ab3d2.bss.AiBss.AI_BoredomPtr_l;
import static ab3d2.bss.AiBss.AI_BoredomSpace_vl;
import static ab3d2.bss.GameBss.Game_ProgressSignal_l;
import static ab3d2.bss.PlayerBss.Plr_MultiplayerType_b;
import static ab3d2.bss.PlayerBss.Plr1_NoiseVol_w;
import static ab3d2.bss.PlayerBss.Plr2_NoiseVol_w;
import static ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l;
import static ab3d2.Hires.PLR_SINGLE;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.DrawBss.Draw_BackdropImagePtr_l;
import static ab3d2.bss.LevelBss.Lvl_ListOfGraphRoomsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_PointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneBorderPointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l;
import static ab3d2.bss.LevelBss.AI_AlienShotDataPtr_l;
import static ab3d2.bss.ZoneBss.Zone_BackdropDisable_vb;
import static ab3d2.bss.TablesBss.CurrentPointBrights_vl;
import static ab3d2.bss.ZoneBss.Zone_BrightTable_vl;
import static ab3d2.bss.AnimBss.Anim_BrightY_l;
import static ab3d2.bss.AnimBss.Anim_SplatType_w;
import static ab3d2.data.TablesData.SinCosTable_vw;
import static ab3d2.data.TablesData.COSINE_OFS;
import static ab3d2.data.TablesData.SINTAB_MASK_ADR;
import static ab3d2.NewanimsData.Anim_LightingEnabled_b;
import static ab3d2.NewanimsData.anim_ExpRadius_w;
import static ab3d2.NewanimsData.tmpangpos;
import static ab3d2.ObjectmoveData.newx;
import static ab3d2.ObjectmoveData.newz;
import static ab3d2.Hires.SCREEN_WIDTH;

/**
 * Traduction littérale de ab3d2_source/newanims.s — PARTIE 1.
 *
 * Routines appelées depuis le code déjà traduit :
 *   - Draw_SkyBackdrop (depuis draw_zone_graph) : remplit le fond de ciel.
 *   - Anim_BrightenPointsAngle (depuis ai.s) : éclaire les points autour d'un
 *     angle (torche des aliens).
 *   - Anim_ExplodeIntoBits (depuis ai.s) : projette des débris.
 * + sous-système brightness (anim_BrightenPoints/darken_points/Flash).
 *
 * QUIRKS de l'original préservés littéralement :
 *  - room_point_loop_A finit par `bra bright_points` (non-angle) : à partir de la
 *    2e zone, l'éclairage angulaire retombe sur le chemin non-angulaire.
 *  - behind_point fait `dbra d7` (d7 = distance Z, pas le compteur d3) — compteur
 *    « corrompu » pour les points derrière la direction de lumière.
 *
 * FICHIER COMPLET : rendu (Draw_SkyBackdrop), éclairage (Anim_BrightenPoints*),
 * Anim_ExplodeIntoBits, mécanismes (DoWaterAnims, LiftRoutine, DoorRoutine,
 * SwitchRoutine, brightanim, BACKSFX), driver objmoveanim, ObjectHandler, ItsABullet
 * (physique des balles), ComputeBlast + DOFLAMES (explosion + flammes).
 * MAKEBACKROUT = rts (no-op, non porté).
 */
public final class Newanims {


    private Newanims() {
    }

    // ==================================================================
    //  Sous-système brightness (newanims.s:101-556)
    // ==================================================================
    private static final int BP = 0, RPL = 1, BPA = 2, RPLA = 3;

    /** Anim_BrightenPointsAngle (newanims.s:287). d0=bright, d1=X, d2=Z, d3=zone, d4=angle. */
    public static void Anim_BrightenPointsAngle(int d0, int d1, int d2, int d3, int d4) {
        if (Mem.b(Anim_LightingEnabled_b) == 0) return; // tst.b ; bne .dolight ; rts
        // SAVEREGS
        int a6 = SinCosTable_vw + (d4 & 0xFFFF);        // move.l #SinCosTable_vw,a0 ; lea (a0,d4.w),a6
        brightenCore(BPA, d0, d1, d2, d3, a6);
    }

    /** anim_BrightenPoints (newanims.s:101). d0=bright, d1=X, d2=Z, d3=zone. */
    public static void anim_BrightenPoints(int d0, int d1, int d2, int d3) {
        if (Mem.b(Anim_LightingEnabled_b) == 0) return; // tst.b ; bne .dolight ; rts
        if ((short) d0 > 0) {                           // tst.w d0 ; bgt darken_points
            darkenPoints(d0, d1, d2, d3);
            return;
        }
        // SAVEREGS
        brightenCore(BP, d0, d1, d2, d3, 0);
    }

    private static void brightenCore(int lbl, int d0, int d1, int d2, int d3, int a6) {
        int d4 = 0, d5 = 0, d6, d7 = 0;
        int a0, a1, a2, a3, a4, a5;

        int zp = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a0
        a0 = Mem.l(zp + (d3 & 0xFFFF) * 4);            // move.l (a0,d3.w*4),a0
        a2 = CurrentPointBrights_vl;                   // move.l #CurrentPointBrights_vl,a2
        a3 = Mem.l(Lvl_PointsPtr_l);                   // move.l Lvl_PointsPtr_l,a3
        a4 = Mem.l(Lvl_ZoneBorderPointsPtr_l);         // move.l Lvl_ZoneBorderPointsPtr_l,a4
        a1 = a0 + ZoneT_PotVisibleZoneList_vw;         // lea ZoneT_PotVisibleZoneList_vw(a0),a1
        a5 = 0;

        while (true) {
            switch (lbl) {
                case BP:        // bright_points
                case BPA: {     // bright_points_A
                    d4 = Mem.w(a1);                    // move.w (a1),d4
                    if (d4 < 0) return;                // blt bright_all[_A] ; GETREGS ; rts
                    a0 = Mem.l(zp + (d4 & 0xFFFF) * 4); // move.l (a0,d4.w*4),a0
                    a1 += PVST_SizeOf_l;               // add.w #PVST_SizeOf_l,a1
                    if (lbl == BP) d7 = 9; else d3 = 9; // moveq #9,d7 / moveq #9,d3
                    d4 = muls(d4, 20);                 // muls #20,d4
                    a5 = a4 + (short) d4;              // lea (a4,d4.w),a5
                    a2 = CurrentPointBrights_vl + (d4 & 0xFFFF) * 4; // move.l #...,a2 ; lea (a2,d4.w*4),a2
                    lbl = (lbl == BP) ? RPL : RPLA;
                    continue;
                }

                case RPL: {     // room_point_loop (non-angle)
                    d4 = Mem.w(a5);
                    a5 += 2;                           // move.w (a5)+,d4
                    if (d4 < 0) { lbl = BP; continue; } // blt bright_points
                    int idx = d4 & 0xFFFF;
                    d5 = setw(d5, Mem.uw(a3 + idx * 4));     // move.w (a3,d4.w*4),d5  (x)
                    d6 = setw(0, Mem.uw(a3 + idx * 4 + 2));  // move.w 2(a3,d4.w*4),d6 (z)
                    d5 = setw(d5, d5 - d1);            // sub.w d1,d5
                    if ((short) d5 <= 0) d5 = setw(d5, -(short) d5); // bgt .okpos1 ; neg.w d5
                    d6 = setw(d6, d6 - d2);            // sub.w d2,d6
                    if ((short) d6 <= 0) d6 = setw(d6, -(short) d6); // bgt .okpos2 ; neg.w d6
                    d5 = setw(d5, d5 + d6);            // add.w d6,d5
                    applyBright(a0, a2, d0, d5);       // [.noBRIGHT1-4]
                    a2 += 8;                           // addq #8,a2
                    d7 = setw(d7, d7 - 1);             // dbra d7,room_point_loop
                    if ((short) d7 != -1) { lbl = RPL; continue; }
                    lbl = BP; continue;                // bra bright_points
                }

                case RPLA: {    // room_point_loop_A (angle)
                    d4 = Mem.w(a5);
                    a5 += 2;                           // move.w (a5)+,d4
                    if (d4 < 0) { lbl = BPA; continue; } // blt bright_points_A
                    int idx = d4 & 0xFFFF;
                    d5 = setw(d5, Mem.uw(a3 + idx * 4 + 2)); // move.w 2(a3,d4.w*4),d5  (z)
                    d4 = setw(d4, Mem.uw(a3 + idx * 4));     // move.w (a3,d4.w*4),d4   (x)
                    d4 = setw(d4, d4 - d1);            // sub.w d1,d4
                    d6 = setw(0, d4);                  // move.w d4,d6
                    if ((short) d4 <= 0) d4 = setw(d4, -(short) d4); // bgt .okpos1 ; neg.w d4
                    d5 = setw(d5, d5 - d2);            // sub.w d2,d5
                    d7 = setw(0, d5);                  // move.w d5,d7
                    if ((short) d5 <= 0) d5 = setw(d5, -(short) d5); // bgt .okpos2 ; neg.w d5
                    // movem.l d0/d1/d2/d3/d4/d5,-(a7)
                    int p0 = d0, p1 = d1, p2 = d2, p3 = d3, p4 = d4, p5 = d5;
                    d0 = setw(0, Mem.uw(a6));          // move.w (a6),d0  (sin)
                    d1 = setw(0, Mem.uw(a6 + COSINE_OFS)); // move.w COSINE_OFS(a6),d1 (cos)
                    d1 = muls(d1, d7);                 // muls d7,d1
                    d0 = muls(d0, d6);                 // muls d6,d0
                    d1 = d1 + d0;                      // add.l d0,d1
                    if (d1 <= 0) {                     // ble behind_point
                        d0 = p0; d1 = p1; d2 = p2; d3 = p3; d4 = p4; d5 = p5; // movem (a7)+
                        a2 += 8;                       // addq #8,a2
                        d7 = setw(d7, d7 - 1);         // dbra d7,room_point_loop_A  (QUIRK : d7 = dist Z)
                        if ((short) d7 != -1) { lbl = RPLA; continue; }
                        lbl = BPA; continue;           // bra bright_points_A
                    }
                    d5 = d1;                           // move.l d1,d5
                    d5 = -d5;                          // neg.l d5
                    d5 = d5 + 30 * 65536;              // add.l #30*65536,d5
                    if (d5 < 0) d5 = 0;                // bge .okkkkk ; moveq #0,d5
                    d0 = setw(0, Mem.uw(a6));          // move.w (a6),d0
                    d1 = setw(0, Mem.uw(a6 + COSINE_OFS)); // move.w COSINE_OFS(a6),d1
                    d7 = muls(d7, d0);                 // muls d0,d7
                    d6 = muls(d6, d1);                 // muls d1,d6
                    d7 = d7 - d6;                      // sub.l d6,d7
                    if (d7 <= 0) d7 = -d7;             // bgt .okkk ; neg.l d7
                    d7 = d7 + d5;                      // add.l d5,d7
                    d7 = d7 << 2;                      // asl.l #2,d7
                    d7 = swap(d7);                     // swap d7
                    d0 = p0; d1 = p1; d2 = p2; d3 = p3; d4 = p4; d5 = p5; // movem (a7)+
                    d5 = setw(d5, d5 + d7);            // add.w d7,d5
                    d5 = setw(d5, d5 + d4);            // add.w d4,d5
                    applyBright(a0, a2, d0, d5);       // [.noBRIGHT1-4]
                    a2 += 8;                           // addq #8,a2
                    d3 = setw(d3, d3 - 1);             // dbra d3,room_point_loop_A
                    if ((short) d3 != -1) { lbl = RPLA; continue; }
                    lbl = BP; continue;                // bra bright_points  (QUIRK : non-angle)
                }

                default:
                    return;
            }
        }
    }

    /** Les 4 blocs .noBRIGHT1-4 partagés par room_point_loop[_A] (newanims.s:156-275). */
    private static void applyBright(int a0, int a2, int d0, int d5) {
        int y = Mem.l(Anim_BrightY_l);                 // move.l Anim_BrightY_l,d4
        if (!(y > Mem.l(a0 + ZoneT_Floor_l)) && !(y < Mem.l(a0 + ZoneT_Roof_l))) { // cmp Floor bgt / cmp Roof blt → .noBRIGHT1
            brightOne(a2 + 2, Mem.l(a0 + ZoneT_Roof_l), true, d0, d5);  // .noBRIGHT2 (Roof)
            brightOne(a2, Mem.l(a0 + ZoneT_Floor_l), false, d0, d5);    // .noBRIGHT1 (Floor)
        }
        // .noBRIGHT1
        y = Mem.l(Anim_BrightY_l);                     // move.l Anim_BrightY_l,d4
        if (!(y > Mem.l(a0 + ZoneT_UpperFloor_l)) && !(y < Mem.l(a0 + ZoneT_UpperRoof_l))) { // → .noBRIGHT4
            brightOne(a2 + 4, Mem.l(a0 + ZoneT_UpperFloor_l), false, d0, d5); // .noBRIGHT3 (UpperFloor)
            brightOne(a2 + 6, Mem.l(a0 + ZoneT_UpperRoof_l), true, d0, d5);   // .noBRIGHT4 (UpperRoof)
        }
        // .noBRIGHT4
    }

    /** Un bloc de brightening sur l'octet-paire (a2off). yval = Floor/Roof long ; negd4 = neg.l d4. */
    private static void brightOne(int a2off, int yval, boolean negd4, int d0, int d5) {
        int d6 = setw(0, d5);                          // move.w d5,d6
        int d4 = yval - Mem.l(Anim_BrightY_l);         // move.l Y(a0),d4 ; sub.l Anim_BrightY_l,d4
        if (negd4) {                                   // (Roof/UpperRoof)
            if (d4 > 0) return;                        // bgt .noBRIGHTx
            d4 = -d4;                                  // neg.l d4
        } else {                                       // (Floor/UpperFloor)
            if (d4 < 0) return;                        // blt .noBRIGHTx
        }
        d4 = d4 >> 7;                                  // asr.l #7,d4
        d6 = setw(d6, d6 + d4);                        // add.w d4,d6
        d6 = setw(d6, ((short) d6) >> 5);              // asr.w #5,d6
        d6 = setw(d6, d6 + d0);                        // add.w d0,d6
        if ((short) d6 >= 0) return;                   // bge .noBRIGHTx
        int v = Mem.w(a2off);                          // tst.w off(a2)
        if (v < 0) Mem.ww(a2off, -v);                  // bge .okbr ; neg.w off(a2)
        d6 = setw(d6, d6 + Mem.uw(a2off));             // add.w off(a2),d6
        if ((short) d6 < 300) d6 = setw(d6, 300);      // cmp.w #300,d6 ; bge .notoobr ; move.w #300,d6
        Mem.ww(a2off, d6);                             // move.w d6,off(a2)
    }

    /** darken_points (newanims.s:516). */
    private static void darkenPoints(int d0, int d1, int d2, int d3) {
        // SAVEREGS
        int zp = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a0
        int a0 = Mem.l(zp + (d3 & 0xFFFF) * 4);        // move.l (a0,d3.w*4),a0
        int a2 = CurrentPointBrights_vl;               // move.l #CurrentPointBrights_vl,a2
        int a3 = Mem.l(Lvl_PointsPtr_l);               // move.l Lvl_PointsPtr_l,a3
        int a1 = a0 + (short) Mem.uw(a0 + ZoneT_Points_w); // move.l a0,a1 ; add.w ZoneT_Points_w(a0),a1
        while (true) {                                 // dark_points
            int d4 = Mem.w(a1);
            a1 += 2;                                   // move.w (a1)+,d4
            if (d4 < 0) return;                        // blt dark_all ; GETREGS ; rts
            int idx = d4 & 0xFFFF;
            int d5 = setw(0, Mem.uw(a3 + idx * 4));     // move.w (a3,d4.w*4),d5
            int d6 = setw(0, Mem.uw(a3 + idx * 4 + 2)); // move.w 2(a3,d4.w*4),d6
            d5 = setw(d5, d5 - d1);                    // sub.w d1,d5
            if ((short) d5 <= 0) d5 = setw(d5, -(short) d5); // bgt .okpos1 ; neg.w d5
            d6 = setw(d6, d6 - d2);                    // sub.w d2,d6
            if ((short) d6 <= 0) d6 = setw(d6, -(short) d6); // bgt .okpos2 ; neg.w d6
            d6 = setw(d6, d6 + d5);                    // add.w d5,d6
            d6 = setw(d6, ((short) d6) >> 5);          // asr.w #5,d6
            d6 = setw(d6, d6 + d0);                    // add.w d0,d6
            if ((short) d6 <= 0) continue;             // ble dark_points
            Mem.ww(a2 + idx * 4, Mem.uw(a2 + idx * 4) + d6);         // add.w d6,(a2,d4.w*4)
            Mem.ww(a2 + idx * 4 + 2, Mem.uw(a2 + idx * 4 + 2) + d6); // add.w d6,2(a2,d4.w*4)
        }
    }

    /** Flash (newanims.s:558). d0=zone, d1=brightness change. */
    public static void Flash(int d0, int d1) {
        if ((short) d1 <= -20) d1 = setw(d1, -20);     // cmp.w #-20,d1 ; bgt .okflash ; move.w #-20,d1
        // movem.l d0/a0/a1,-(a7)
        int a1 = CurrentPointBrights_vl;               // move.l #CurrentPointBrights_vl,a1
        int zp = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a0
        int a0 = Mem.l(zp + (d0 & 0xFFFF) * 4);        // move.l (a0,d0.w*4),a0
        int savedA0 = a0;                              // move.l a0,-(a7)
        a0 = a0 + (short) Mem.uw(a0 + ZoneT_Points_w); // add.w ZoneT_Points_w(a0),a0
        while (true) {                                 // flashpts
            int d2 = Mem.w(a0);
            a0 += 2;                                   // move.w (a0)+,d2
            if (d2 < 0) break;                         // blt flashedall
            int i = d2 & 0xFFFF;
            Mem.ww(a1 + i * 4, Mem.uw(a1 + i * 4) + d1);         // add.w d1,(a1,d2.w*4)
            Mem.ww(a1 + i * 4 + 2, Mem.uw(a1 + i * 4 + 2) + d1); // add.w d1,2(a1,d2.w*4)
        }
        // flashedall
        a0 = savedA0;                                  // move.l (a7)+,a0
        a1 = Zone_BrightTable_vl;                       // move.l #Zone_BrightTable_vl,a1
        Mem.ww(a1 + (d0 & 0xFFFF) * 4, Mem.uw(a1 + (d0 & 0xFFFF) * 4) + d1);         // add.w d1,(a1,d0.w*4)
        Mem.ww(a1 + (d0 & 0xFFFF) * 4 + 2, Mem.uw(a1 + (d0 & 0xFFFF) * 4 + 2) + d1); // add.w d1,2(a1,d0.w*4)
        a0 = a0 + ZoneT_PotVisibleZoneList_vw;         // add.l #ZoneT_PotVisibleZoneList_vw,a0
        while (true) {                                 // doemall
            int dz = Mem.w(a0);                        // move.w (a0),d0
            if (dz < 0) break;                         // blt doneemall
            Mem.ww(a1 + (dz & 0xFFFF) * 4, Mem.uw(a1 + (dz & 0xFFFF) * 4) + d1);         // add.w d1,(a1,d0.w*4)
            Mem.ww(a1 + (dz & 0xFFFF) * 4 + 2, Mem.uw(a1 + (dz & 0xFFFF) * 4 + 2) + d1); // add.w d1,2(a1,d0.w*4)
            a0 += 8;                                   // addq #8,a0
        }
        // doneemall: movem ; rts
    }

    // ==================================================================
    //  Anim_ExplodeIntoBits (newanims.s:605)
    //  d0/d2/d3 entrées ; position dans newx/newz ; objet source dans a0.
    // ==================================================================
    public static void Anim_ExplodeIntoBits(int d0, int d2, int d3, int a0) {
        int d1, d4;
        Mem.ww(anim_ExpRadius_w, d3);                  // move.w d3,anim_ExpRadius_w
        if ((short) d2 > 7) d2 = setw(d2, 7);          // cmp.w #7,d2 ; ble .oksplut ; move.w #7,d2
        int a5 = Mem.l(AI_AlienShotDataPtr_l);         // move.l AI_AlienShotDataPtr_l,a5
        d1 = setw(0, NUM_ALIEN_SHOT_DATA - 1);         // move.w #NUM_ALIEN_SHOT_DATA-1,d1
        // .findeight
        while (true) {
            if ((short) Mem.uw(a5 + ObjT_ZoneID_w) < 0) break; // move.w ObjT_ZoneID_w(a5),d0 ; blt .gotonehere
            a5 += ObjT_SizeOf_l;                       // adda.w #ObjT_SizeOf_l,a5
            d1 = setw(d1, d1 - 1);                     // dbra d1,.findeight
            if ((short) d1 == -1) return;              // rts
        }

        // .gotonehere — boucle de remplissage des débris
        while (true) {
            Mem.wb(a5 + ShotT_Power_w, 0);             // move.b #0,ShotT_Power_w(a5)
            int a2 = Mem.l(Lvl_ObjectPointsPtr_l);     // move.l Lvl_ObjectPointsPtr_l,a2
            d3 = setw(0, Mem.uw(a5));                   // move.w (a5),d3
            a2 = a2 + (d3 & 0xFFFF) * 8;               // lea (a2,d3.w*8),a2
            d0 = setw(0, Mem.uw(newx));                 // move.w newx,d0
            Mem.ww(a2, d0);                            // move.w d0,(a2)
            d0 = setw(0, Mem.uw(newz));                 // move.w newz,d0
            Mem.ww(a2 + 4, d0);                        // move.w d0,4(a2)
            Mem.wb(a2 + 16, 2);                        // move.b #2,16(a2)
            d0 = Objectmove.GetRand();                 // jsr GetRand
            d0 = setw(d0, d0 & SINTAB_MASK_ADR);       // AMOD_A d0
            int a2s = SinCosTable_vw + (d0 & 0xFFFF);  // move.l #SinCosTable_vw,a2 ; adda.w d0,a2
            d3 = setw(0, Mem.uw(a2s));                 // move.w (a2),d3
            d4 = setw(0, Mem.uw(a2s + COSINE_OFS));    // move.w COSINE_OFS(a2),d4
            d0 = Objectmove.GetRand();                 // jsr GetRand
            d0 = setw(d0, d0 & 3);                     // and.w #3,d0
            d0 = setw(d0, d0 + 1);                     // add.w #1,d0
            d3 = (short) d3;                           // ext.l d3
            d4 = (short) d4;                           // ext.l d4
            d3 = d3 << ((short) d0 & 31);              // asl.l d0,d3
            d4 = d4 << ((short) d0 & 31);              // asl.l d0,d4
            d0 = Mem.l(a0 + EntT_ImpactX_w);           // move.l EntT_ImpactX_w(a0),d0
            d4 = swap(d4);                             // swap d4
            d0 = setw(d0, ((short) d0) >> 1);          // asr.w #1,d0
            d4 = setw(d4, d4 + d0);                    // add.w d0,d4
            d0 = swap(d0);                             // swap d0
            Mem.ww(a5 + ShotT_VelocityZ_w, d4);        // move.w d4,ShotT_VelocityZ_w(a5)
            d3 = swap(d3);                             // swap d3
            d0 = setw(d0, ((short) d0) >> 1);          // asr.w #1,d0
            d3 = setw(d3, d3 + d0);                    // add.w d0,d3
            Mem.ww(a5 + ShotT_VelocityX_w, d3);        // move.w d3,ShotT_VelocityX_w(a5)
            d0 = Objectmove.GetRand();                 // jsr GetRand
            d0 = setw(d0, d0 & 1023);                  // and.w #1023,d0
            d0 = setw(d0, d0 + 2 * 128);               // add.w #2*128,d0
            d0 = setw(d0, -(short) d0);                // neg.w d0
            Mem.ww(a5 + ShotT_VelocityY_w, d0);        // move.w d0,ShotT_VelocityY_w(a5)
            Mem.wl(a5 + EntT_EnemyFlags_l, 0);         // move.l #0,EntT_EnemyFlags_l(a5)
            Mem.ww(a5 + ObjT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),ObjT_ZoneID_w(a5)
            d0 = setw(0, Mem.uw(a0 + 4));              // move.w 4(a0),d0
            Mem.ww(a5 + 4, d0);                        // move.w d0,4(a5)
            d0 = setw(d0, d0 + 6);                     // add.w #6,d0
            d0 = (short) d0;                           // ext.l d0
            d0 = d0 << 7;                              // asl.l #7,d0
            Mem.wl(a5 + ShotT_AccYPos_w, d0);          // move.l d0,ShotT_AccYPos_w(a5)
            Mem.wb(a5 + ShotT_Size_b, Mem.ub(Anim_SplatType_w)); // move.b Anim_SplatType_w,ShotT_Size_b(a5)
            Mem.ww(a5 + ShotT_Flags_w, 0);             // move.w #0,ShotT_Flags_w(a5)
            Mem.ww(a5 + ShotT_Lifetime_w, 0);          // move.w #0,ShotT_Lifetime_w(a5)
            Mem.wb(a5 + ShotT_Status_b, 0);            // clr.b ShotT_Status_b(a5)
            Mem.wb(a5 + ShotT_InUpperZone_b, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ...InUpperZone...
            Mem.wb(a5 + ShotT_Worry_b, 0xFF);          // st ShotT_Worry_b(a5)
            a5 += 64;                                  // adda.w #64,a5
            d2 = setw(d2, d2 - 1);                     // sub.w #1,d2
            if ((short) d2 < 0) return;                // blt .gotemall ; rts
            d1 = setw(d1, d1 - 1);                     // dbra d1,.findeight
            if ((short) d1 == -1) return;
            // .findeight (depuis .gotonehere : recherche du prochain slot libre)
            boolean found = false;
            while (true) {
                if ((short) Mem.uw(a5 + ObjT_ZoneID_w) < 0) { found = true; break; } // blt .gotonehere
                a5 += ObjT_SizeOf_l;                   // adda.w #ObjT_SizeOf_l,a5
                d1 = setw(d1, d1 - 1);                 // dbra d1,.findeight
                if ((short) d1 == -1) break;
            }
            if (!found) return;                        // rts
        }
    }

    // ==================================================================
    //  brightanim (newanims.s:690) — avance les animations de luminosité.
    // ==================================================================
    static void brightanim() {
        int a1 = Anim_BrightTable_vw;                  // move.l #Anim_BrightTable_vw,a1
        int a3 = anim_BrightessAnimPtrs_vl;            // move.l #anim_BrightessAnimPtrs_vl,a3
        int a4 = anim_BrightnessAnimStartPtrs_vl;      // move.l #anim_BrightnessAnimStartPtrs_vl,a4
        while (true) {                                 // .dobrightanims
            int d0 = Mem.l(a3);                        // move.l (a3),d0
            if (d0 < 0) return;                        // blt .nomoreanims
            int a2 = d0;                               // move.l d0,a2
            d0 = setw(0, Mem.uw(a2));
            a2 += 2;                                   // move.w (a2)+,d0
            if ((short) d0 == BRIGHT_ANIM_END) {       // cmp.w #BRIGHT_ANIM_END,d0 ; bne .itsabright
                a2 = Mem.l(a4);                        // move.l (a4),a2
                d0 = setw(0, Mem.uw(a2));
                a2 += 2;                               // move.w (a2)+,d0
            }
            // .itsabright
            Mem.wl(a3, a2);
            a3 += 4;                                   // move.l a2,(a3)+
            a4 += 4;                                   // addq #4,a4
            Mem.ww(a1, d0);
            a1 += 2;                                   // move.w d0,(a1)+
        }
    }

    // ==================================================================
    //  BACKSFX (newanims.s:716) — joue un SFX d'ambiance aléatoire.
    // ==================================================================
    static void BACKSFX() {
        int d0 = setw(0, Mem.uw(Anim_TempFrames_w));   // move.w Anim_TempFrames_w,d0
        Mem.ww(anim_TimeToNoise_w, Mem.uw(anim_TimeToNoise_w) - d0); // sub.w d0,anim_TimeToNoise_w
        if ((short) Mem.uw(anim_TimeToNoise_w) > 0) return; // bgt .nosfx
        d0 = Objectmove.GetRand();                     // jsr GetRand
        d0 = setw(d0, (d0 & 0xFFFF) >>> 3);            // lsr.w #3,d0
        d0 = setw(d0, d0 & 127);                       // and.w #127,d0
        d0 = setw(d0, d0 + 100);                       // add.w #100,d0
        Mem.ww(anim_TimeToNoise_w, d0);                // move.w d0,anim_TimeToNoise_w
        int a0 = Mem.l(ZonePtr_l);                     // move.l ZonePtr_l,a0
        a0 = a0 + (short) Mem.uw(anim_OddEven_w);      // add.w anim_OddEven_w,a0
        d0 = setw(0, 2);                               // move.w #2,d0
        d0 = setw(d0, d0 - Mem.uw(anim_OddEven_w));    // sub.w anim_OddEven_w,d0
        Mem.ww(anim_OddEven_w, d0);                    // move.w d0,anim_OddEven_w
        int d1 = setw(0, Mem.uw(a0 + ZoneT_BackSFXMask_w)); // move.w ZoneT_BackSFXMask_w(a0),d1
        if ((short) d1 == 0) return;                   // beq .nosfx
        d0 = Objectmove.GetRand();                     // jsr GetRand
        d0 = setw(d0, (d0 & 0xFFFF) >>> 3);            // lsr.w #3,d0
        do {                                           // .notfound
            d0 = setw(d0, d0 + 1);                     // addq #1,d0
            d0 = setw(d0, d0 & 15);                    // and.w #15,d0
        } while ((d1 & (1 << (d0 & 15))) == 0);        // btst d0,d1 ; beq .notfound
        a0 = Mem.l(GLF_DatabasePtr_l);                 // move.l GLF_DatabasePtr_l,a0
        a0 = a0 + GLFT_AmbientSFX_l;                   // add.l #GLFT_AmbientSFX_l,a0
        Mem.ww(Aud_SampleNum_w, Mem.uw(a0 + (d0 & 0xFFFF) * 2)); // move.w (a0,d0.w*2),Aud_SampleNum_w
        Mem.ww(IDNUM, 0xFFF0);                         // move.w #$fff0,IDNUM
        Mem.wb(notifplaying, 0xFF);                    // st.b notifplaying
        Mem.wl(Aud_NoiseX_w, 0);                       // move.l #0,Aud_NoiseX_w (efface X+Z)
        Mem.wb(PlayEcho, 0);                           // move.b #0,PlayEcho
        d0 = Objectmove.GetRand();                     // jsr GetRand
        d0 = setw(d0, d0 & 15);                        // and.w #15,d0
        d0 = setw(d0, d0 + 32);                        // add.w #32,d0
        Mem.ww(Aud_NoiseVol_w, d0);                    // move.w d0,Aud_NoiseVol_w
        Hires.MakeSomeNoise();                         // jsr MakeSomeNoise
    }

    // ==================================================================
    //  DoWaterAnims (newanims.s:815) — anime le niveau d'eau des zones.
    //  a0 = pointeur sur les données d'eau (après le sentinelle des lifts).
    // ==================================================================
    static int DoWaterAnims(int a0) {
        int d0 = setw(0, 20);                          // move.w #20,d0
        while (true) {                                 // wateranimlop
            int d1 = Mem.l(a0);
            a0 += 4;                                   // move.l (a0)+,d1  (top)
            int d2 = Mem.l(a0);
            a0 += 4;                                   // move.l (a0)+,d2  (bottom)
            int d3 = Mem.l(a0);                        // move.l (a0),d3   (level)
            int d4 = setw(0, Mem.uw(a0 + 4));          // move.w 4(a0),d4  (speed)
            int d5 = setw(0, d4);                      // move.w d4,d5
            d5 = muls(d5, Mem.uw(Anim_TempFrames_w));  // muls Anim_TempFrames_w,d5
            d3 = d3 + d5;                              // add.l d5,d3
            if (d3 > d1) {                             // cmp.l d1,d3 ; bgt waternotattop
                if (d3 >= d2) {                        // cmp.l d2,d3 ; blt waterdone
                    d3 = d2;                           // move.l d2,d3
                    d4 = setw(d4, -(short) d4);        // neg.w d4
                }
            } else {
                d3 = d1;                               // move.l d1,d3
                d4 = setw(d4, -(short) d4);            // neg.w d4
            }
            // waterdone
            Mem.wl(a0, d3);
            a0 += 4;                                   // move.l d3,(a0)+
            Mem.ww(a0, d4);
            a0 += 2;                                   // move.w d4,(a0)+
            d1 = d3;                                   // move.l d3,d1
            // morezones
            while (true) {
                int d2z = setw(0, Mem.uw(a0));
                a0 += 2;                               // move.w (a0)+,d2
                if ((short) d2z < 0) break;            // blt → dbra d0,wateranimlop
                // okzone
                int a1 = Mem.l(a0);
                a0 += 4;                               // move.l (a0)+,a1
                a1 = a1 + Mem.l(Lvl_GraphicsPtr_l);    // add.l Lvl_GraphicsPtr_l,a1
                int d3b = d1 >> 6;                     // move.l d1,d3 ; asr.l #6,d3
                Mem.ww(a1 + 2, d3b);                   // move.w d3,2(a1)
                int a1z = Mem.l(Lvl_ZonePtrsPtr_l);    // move.l Lvl_ZonePtrsPtr_l,a1
                a1z = Mem.l(a1z + (d2z & 0xFFFF) * 4); // move.l (a1,d2.w*4),a1
                Mem.wl(a1z + ZoneT_Water_l, d1);       // move.l d1,ZoneT_Water_l(a1)
            }
            d0 = setw(d0, d0 - 1);                     // dbra d0,wateranimlop
            if ((short) d0 == -1) return a0;           // rts
        }
    }

    // ==================================================================
    //  SwitchRoutine (newanims.s:1585) — interrupteurs actionnés par espace.
    // ==================================================================
    private static final int CHECKSWITCHES = 0, BACKTOP2 = 1, BACKTOEND = 2, NOBUTT = 3;

    static void SwitchRoutine() {
        int a0 = Mem.l(Lvl_SwitchDataPtr_l);           // move.l Lvl_SwitchDataPtr_l,a0
        int d0 = setw(0, 7);                           // move.w #7,d0
        int a1 = Mem.l(Lvl_PointsPtr_l);               // move.l Lvl_PointsPtr_l,a1
        int lbl = CHECKSWITCHES;
        while (true) {
            switch (lbl) {
                case CHECKSWITCHES: {
                    if (Mem.b(Plr1_TmpSpcTap_b) != 0) { // tst.b Plr1_TmpSpcTap_b ; bne p1_SpaceIsPressed
                        switchSpace(a0, a1, d0, Plr1_TmpXOff_l, Plr1_TmpZOff_l, false);
                    }
                    lbl = BACKTOP2;
                    continue;
                }
                case BACKTOP2: {
                    if (Mem.b(Plr2_TmpSpcTap_b) != 0) { // tst.b Plr2_TmpSpcTap_b ; bne p2_SpaceIsPressed
                        switchSpace(a0, a1, d0, Plr2_TmpXOff_l, Plr2_TmpZOff_l, true);
                    }
                    lbl = BACKTOEND;
                    continue;
                }
                case BACKTOEND: {
                    if (Mem.b(a0 + 2) == 0) { lbl = NOBUTT; continue; } // tst.b 2(a0) ; beq nobutt
                    if (Mem.b(a0 + 10) == 0) { lbl = NOBUTT; continue; } // tst.b 10(a0) ; beq nobutt
                    int d1 = setw(0, Mem.uw(Anim_TempFrames_w)); // move.w Anim_TempFrames_w,d1
                    d1 = setw(d1, d1 + d1);            // add.w d1,d1
                    d1 = setw(d1, d1 + d1);            // add.w d1,d1
                    Mem.wb(a0 + 3, Mem.ub(a0 + 3) - d1); // sub.b d1,3(a0)
                    if ((byte) Mem.ub(a0 + 3) != 0) { lbl = NOBUTT; continue; } // bne nobutt
                    Mem.wb(a0 + 10, 0);                // move.b #0,10(a0)
                    int a3 = Mem.l(a0 + 6) + Mem.l(Lvl_GraphicsPtr_l); // move.l 6(a0),a3 ; add.l Lvl_GraphicsPtr_l,a3
                    Mem.ww(a3 + 4, 11);                // move.w #11,4(a3)
                    int d3 = setw(0, Mem.uw(a3));      // move.w (a3),d3
                    d3 = setw(d3, d3 & 0b00000111100); // and.w #%00000111100,d3
                    Mem.ww(a3, d3);                    // move.w d3,(a3)
                    d3 = setw(0, 7);                   // move.w #7,d3
                    d3 = setw(d3, d3 - d0);            // sub.w d0,d3
                    d3 = setw(d3, d3 + 4);             // addq #4,d3
                    int d4 = setw(0, Mem.uw(Conditions)); // move.w Conditions,d4
                    d4 = setw(d4, d4 & ~(1 << (d3 & 15))); // bclr d3,d4
                    Mem.ww(Conditions, d4);            // move.w d4,Conditions
                    Mem.ww(Aud_NoiseX_w, 0);           // move.w #0,Aud_NoiseX_w
                    Mem.ww(Aud_NoiseZ_w, 0);           // move.w #0,Aud_NoiseZ_w
                    Mem.ww(Aud_NoiseVol_w, 50);        // move.w #50,Aud_NoiseVol_w
                    Mem.ww(Aud_SampleNum_w, 10);       // move.w #10,Aud_SampleNum_w
                    Mem.wb(Aud_ChannelPick_b, 1);      // move.b #1,Aud_ChannelPick_b
                    Mem.wb(notifplaying, 0xFF);        // st notifplaying
                    Mem.ww(IDNUM, 0xFFFC);             // move.w #$fffc,IDNUM
                    Hires.MakeSomeNoise();             // jsr MakeSomeNoise
                    lbl = NOBUTT;
                    continue;
                }
                case NOBUTT: {
                    a0 = a0 + 14;                      // adda.w #14,a0
                    d0 = setw(d0, d0 - 1);             // dbra d0,CheckSwitches
                    if ((short) d0 == -1) return;      // rts
                    lbl = CHECKSWITCHES;
                    continue;
                }
                default:
                    return;
            }
        }
    }

    /** p1_SpaceIsPressed / p2_SpaceIsPressed (newanims.s:1643/1701) — toggle si proche. */
    private static void switchSpace(int a0, int a1, int d0, int xoffSym, int zoffSym, boolean plr2) {
        int d1 = setw(0, Mem.uw(xoffSym));             // move.w PlrX_TmpXOff_l,d1
        int d2 = setw(0, Mem.uw(zoffSym));             // move.w PlrX_TmpZOff_l,d2
        int d3 = setw(0, Mem.uw(a0));                  // move.w (a0),d3
        if ((short) d3 < 0) return;                    // blt .NotCloseEnough
        d3 = setw(0, Mem.uw(a0 + 4));                  // move.w 4(a0),d3
        int a2 = a1 + (d3 & 0xFFFF) * 4;               // lea (a1,d3.w*4),a2
        d3 = setw(0, Mem.uw(a2));                      // move.w (a2),d3
        d3 = setw(d3, d3 + Mem.uw(a2 + 4));            // add.w 4(a2),d3
        d3 = setw(d3, ((short) d3) >> 1);              // asr.w #1,d3
        int d4 = setw(0, Mem.uw(a2 + 2));              // move.w 2(a2),d4
        d4 = setw(d4, d4 + Mem.uw(a2 + 6));            // add.w 6(a2),d4
        d4 = setw(d4, ((short) d4) >> 1);              // asr.w #1,d4
        d3 = setw(d3, d3 - d1);                        // sub.w d1,d3
        d3 = muls(d3, d3);                             // muls d3,d3
        d4 = setw(d4, d4 - d2);                        // sub.w d2,d4
        d4 = muls(d4, d4);                             // muls d4,d4
        d4 = d4 + d3;                                  // add.l d3,d4
        if (d4 >= 60 * 60) return;                     // cmp.l #60*60,d4 ; bge .NotCloseEnough
        int a3 = Mem.l(a0 + 6) + Mem.l(Lvl_GraphicsPtr_l); // move.l 6(a0),a3 ; add.l Lvl_GraphicsPtr_l,a3
        Mem.ww(a3 + 4, 11);                            // move.w #11,4(a3)
        d3 = setw(0, Mem.uw(a3));                      // move.w (a3),d3
        d3 = setw(d3, d3 & 0b00000111100);             // and.w #%00000111100,d3
        Mem.wb(a0 + 10, ~Mem.ub(a0 + 10));             // not.b 10(a0)
        if ((byte) Mem.ub(a0 + 10) != 0) {             // beq.s .switchoff
            d3 = setw(d3, d3 | 2);                     // or.w #2,d3
        }
        // .switchoff
        Mem.ww(a3, d3);                                // move.w d3,(a3)
        d3 = setw(0, 7);                               // move.w #7,d3
        d3 = setw(d3, d3 - d0);                        // sub.w d0,d3
        d3 = setw(d3, d3 + 4);                         // addq #4,d3
        int d4c = setw(0, Mem.uw(Conditions));         // move.w Conditions,d4
        d4c = setw(d4c, d4c ^ (1 << (d3 & 15)));       // bchg d3,d4
        Mem.ww(Conditions, d4c);                       // move.w d4,Conditions
        if (!plr2) Mem.wb(a0 + 3, 0);                  // (p1 seulement) move.b #0,3(a0)
        Mem.ww(Aud_NoiseX_w, 0);                       // move.w #0,Aud_NoiseX_w
        Mem.ww(Aud_NoiseZ_w, 0);                       // move.w #0,Aud_NoiseZ_w
        Mem.ww(Aud_NoiseVol_w, 50);                    // move.w #50,Aud_NoiseVol_w
        Mem.ww(Aud_SampleNum_w, 10);                   // move.w #10,Aud_SampleNum_w
        Mem.wb(Aud_ChannelPick_b, 1);                  // move.b #1,Aud_ChannelPick_b
        Mem.wb(notifplaying, 0xFF);                    // st notifplaying
        Mem.ww(IDNUM, 0xFFFC);                         // move.w #$fffc,IDNUM
        Hires.MakeSomeNoise();                         // jsr MakeSomeNoise
    }

    // ==================================================================
    //  LiftRoutine (newanims.s:869) — machine à états des ascenseurs.
    // ==================================================================
    private static final int DOALIFT = 0, BACKFROMLIFT = 1, LIFT_SIMPLE = 2;

    static void LiftRoutine() {
        int d0 = 0, d1 = 0, d2, d3 = 0, d4 = 0, d5 = 0, d6, d7 = 0;
        int a0, a1, a3 = 0, a5 = 0;
        int a6 = anim_LiftHeightTable_vw;              // move.l #anim_LiftHeightTable_vw,a6
        Mem.ww(anim_CurrentLiftable_w, -1);            // move.w #-1,anim_CurrentLiftable_w
        a0 = Mem.l(Lvl_LiftDataPtr_l);                 // move.l Lvl_LiftDataPtr_l,a0

        int lbl = DOALIFT;
        while (true) {
            switch (lbl) {
                case DOALIFT: {
                    Mem.ww(anim_CurrentLiftable_w, Mem.uw(anim_CurrentLiftable_w) + 1); // add.w #1,anim_CurrentLiftable_w
                    d0 = setw(0, Mem.uw(a0));
                    a0 += 2;                           // move.w (a0)+,d0  (bottom of movement)
                    if ((short) d0 == 999) {           // cmp.w #999,d0 ; bne notallliftsdone
                        Mem.ww(a6, 999);               // move.w #999,(a6)
                        Mem.ww(anim_LiftOnlyLocks_w, 0); // move.w #0,anim_LiftOnlyLocks_w
                        DoWaterAnims(a0);              // bsr DoWaterAnims
                        return;                        // rts
                    }
                    // notallliftsdone (886)
                    d1 = setw(0, Mem.uw(a0));
                    a0 += 2;                           // move.w (a0)+,d1  (top of movement)
                    Mem.ww(anim_OpeningSpeed_w, Mem.uw(a0));
                    a0 += 2;                           // move.w (a0)+,anim_OpeningSpeed_w
                    Mem.ww(anim_OpeningSpeed_w, -(short) Mem.uw(anim_OpeningSpeed_w)); // neg.w anim_OpeningSpeed_w
                    Mem.ww(anim_ClosingSpeed_w, Mem.uw(a0)); a0 += 2; // move.w (a0)+,anim_ClosingSpeed_w
                    Mem.ww(anim_OpenDuration_w, Mem.uw(a0)); a0 += 2; // move.w (a0)+,anim_OpenDuration_w
                    Mem.ww(anim_OpeningSoundFX_w, Mem.uw(a0)); a0 += 2; // (a0)+
                    Mem.ww(anim_ClosingSoundFX_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_OpenedSoundFX_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_ClosedSoundFX_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_OpeningSoundFX_w, Mem.uw(anim_OpeningSoundFX_w) - 1); // subq #1
                    Mem.ww(anim_ClosingSoundFX_w, Mem.uw(anim_ClosingSoundFX_w) - 1);
                    Mem.ww(anim_OpenedSoundFX_w, Mem.uw(anim_OpenedSoundFX_w) - 1);
                    Mem.ww(anim_ClosedSoundFX_w, Mem.uw(anim_ClosedSoundFX_w) - 1);
                    d2 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d2  (18: world X)
                    d3 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d3  (20: world Z)
                    d2 = setw(d2, d2 - Mem.uw(Plr1_TmpXOff_l)); // sub.w Plr1_TmpXOff_l,d2
                    d3 = setw(d3, d3 - Mem.uw(Plr1_TmpZOff_l)); // sub.w Plr1_TmpZOff_l,d3
                    d4 = muls(Mem.w(Vis_CosVal_w), d2); // move.w Vis_CosVal_w,d4 ; muls d2,d4
                    d5 = muls(Mem.w(Vis_SinVal_w), d3); // move.w Vis_SinVal_w,d5 ; muls d3,d5
                    d4 = d4 - d5;                      // sub.l d5,d4
                    d4 = d4 + d4;                      // add.l d4,d4
                    d4 = swap(d4);                     // swap d4
                    Mem.ww(Aud_NoiseX_w, d4);          // move.w d4,Aud_NoiseX_w
                    d4 = muls(Mem.w(Vis_SinVal_w), d2); // move.w Vis_SinVal_w,d4 ; muls d2,d4
                    d5 = muls(Mem.w(Vis_CosVal_w), d3); // move.w Vis_CosVal_w,d5 ; muls d3,d5
                    d4 = d4 - d5;                      // sub.l d5,d4
                    d4 = d4 + d4;                      // add.l d4,d4
                    d4 = swap(d4);                     // swap d4
                    Mem.ww(Aud_NoiseZ_w, d4);          // move.w d4,Aud_NoiseZ_w
                    d3 = setw(0, Mem.uw(a0));          // move.w (a0),d3  (22: current pos)
                    Mem.ww(a6, d3);
                    a6 += 2;                           // move.w d3,(a6)+
                    d2 = setw(0, Mem.uw(a0 + 2));      // move.w 2(a0),d2  (24: speed)
                    d7 = setw(0, Mem.uw(a0 + 8));      // move.w 8(a0),d7  (30: zone id)
                    a1 = Mem.l(Lvl_ZonePtrsPtr_l);     // move.l Lvl_ZonePtrsPtr_l,a1
                    a1 = Mem.l(a1 + (d7 & 0xFFFF) * 4); // move.l (a1,d7.w*4),a1
                    Mem.wb(PlayEcho, Mem.ub(a1 + ZoneT_Echo_b)); // move.b ZoneT_Echo_b(a1),PlayEcho
                    d7 = setw(d7, d2);                 // move.w d2,d7
                    Mem.ww(anim_FloorMoveSpeed_w, d2); // move.w d2,anim_FloorMoveSpeed_w
                    d2 = muls(d2, Mem.uw(Anim_TempFrames_w)); // muls Anim_TempFrames_w,d2
                    d3 = setw(d3, d3 + d2);            // add.w d2,d3
                    d2 = setw(d2, d7);                 // move.w d7,d2
                    Mem.wb(anim_LiftAtBottom_b, ((short) d0 <= (short) d3) ? 0xFF : 0); // cmp d3,d0 ; sle anim_LiftAtBottom_b
                    if ((short) d0 > (short) d3) {     // bgt.s .nolower
                        // (skip lower noise)
                    } else {
                        if ((short) d2 != 0) {         // tst.w d2 ; beq .nonoise3
                            Mem.ww(Aud_NoiseVol_w, 50); // move.w #50,Aud_NoiseVol_w
                            Mem.ww(Aud_SampleNum_w, Mem.uw(anim_ClosedSoundFX_w)); // move.w anim_ClosedSoundFX_w,Aud_SampleNum_w
                            if ((short) Mem.uw(anim_ClosedSoundFX_w) >= 0) { // blt .nonoise3
                                Mem.wb(Aud_ChannelPick_b, 1); // move.b #1,Aud_ChannelPick_b
                                Mem.wb(notifplaying, 0);  // clr.b notifplaying
                                Mem.ww(IDNUM, 0xFFFD);    // move.w #$fffd,IDNUM
                                Hires.MakeSomeNoise();    // jsr MakeSomeNoise
                            }
                        }
                        // .nonoise3
                        d2 = 0;                        // moveq #0,d2
                        d3 = setw(d3, d0);             // move.w d0,d3
                    }
                    // .nolower
                    Mem.wb(anim_LiftAtTop_b, ((short) d1 >= (short) d3) ? 0xFF : 0); // cmp d3,d1 ; sge anim_LiftAtTop_b
                    if ((short) d1 < (short) d3) {     // blt.s .noraise
                        // (skip raise noise)
                    } else {
                        if ((short) d2 != 0) {         // tst.w d2 ; beq .nonoise
                            Mem.ww(a6, 0);             // move.w #0,(a6)
                            Mem.ww(Aud_NoiseVol_w, 50);
                            Mem.ww(Aud_SampleNum_w, Mem.uw(anim_OpenedSoundFX_w));
                            if ((short) Mem.uw(anim_OpenedSoundFX_w) >= 0) { // blt .nonoise
                                Mem.wb(Aud_ChannelPick_b, 1);
                                Mem.wb(notifplaying, 0);
                                Mem.ww(IDNUM, 0xFFFD);
                                Hires.MakeSomeNoise();
                            }
                        }
                        // .nonoise
                        d2 = 0;                        // moveq #0,d2
                        d3 = setw(d3, d1);             // move.w d1,d3
                    }
                    // .noraise
                    d0 = setw(d0, d0 - d3);            // sub.w d3,d0
                    d6 = ((short) d0 < 15 * 16) ? 0xFF : 0; // cmp.w #15*16,d0 ; slt d6
                    Mem.ww(a0, d3); a0 += 2;           // move.w d3,(a0)+
                    a5 = a0;                           // move.l a0,a5
                    Mem.ww(a0, d2); a0 += 2;           // move.w d2,(a0)+
                    d7 = setw(d7, d2);                 // move.w d2,d7
                    a1 = Mem.l(a0); a0 += 4;           // move.l (a0)+,a1
                    a1 = a1 + Mem.l(Lvl_GraphicsPtr_l); // add.l Lvl_GraphicsPtr_l,a1
                    d3 = setw(d3, ((short) d3) >> 2);  // asr.w #2,d3
                    d0 = setw(d0, d3);                 // move.w d3,d0
                    d0 = setw(d0, (d0 & 0xFFFF) << 2); // asl.w #2,d0
                    Mem.ww(a1 + 2, d0);                // move.w d0,2(a1)
                    d0 = setw(d0, d3);                 // move.w d3,d0
                    d3 = (short) d3;                   // ext.l d3
                    d3 = d3 << 8;                      // asl.l #8,d3
                    d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5  (zone id)
                    a1 = Mem.l(Lvl_ZonePtrsPtr_l);     // move.l Lvl_ZonePtrsPtr_l,a1
                    a1 = Mem.l(a1 + (d5 & 0xFFFF) * 4); // move.l (a1,d5.w*4),a1
                    d5 = setw(0, Mem.uw(a1));          // move.w (a1),d5
                    a3 = Mem.l(Plr1_ZonePtr_l);        // move.l Plr1_ZonePtr_l,a3
                    Mem.wl(a1 + 2, d3);                // move.l d3,2(a1)
                    d0 = setw(d0, -(short) d0);        // neg.w d0
                    Mem.wb(plr1_StoodOnLift_b, ((short) Mem.uw(a3) == (short) d5) ? 0xFF : 0); // cmp (a3),d5 ; seq plr1_StoodOnLift_b
                    if ((short) Mem.uw(a3) == (short) d5) { // bne .nosetfloorspd1
                        Mem.ww(Plr1_FloorSpd_w, Mem.uw(anim_FloorMoveSpeed_w)); // move.w anim_FloorMoveSpeed_w,Plr1_FloorSpd_w
                    }
                    // .nosetfloorspd1
                    a3 = Mem.l(Plr2_ZonePtr_l);        // move.l Plr2_ZonePtr_l,a3
                    Mem.wb(plr2_StoodOnLift_b, ((short) Mem.uw(a3) == (short) d5) ? 0xFF : 0); // cmp (a3),d5 ; seq plr2_StoodOnLift_b
                    if ((short) Mem.uw(a3) == (short) d5) { // bne .nosetfloorspd2
                        Mem.ww(Plr2_FloorSpd_w, Mem.uw(anim_FloorMoveSpeed_w)); // move.w anim_FloorMoveSpeed_w,Plr2_FloorSpd_w
                    }
                    // .nosetfloorspd2
                    a0 += 2;                           // move.w (a0)+,d2  (conditions, valeur ignorée)
                    d2 = setw(0, Mem.uw(anim_CurrentLiftable_w)); // move.w anim_CurrentLiftable_w,d2
                    d5 = setw(0, Mem.uw(anim_LiftOnlyLocks_w));   // move.w anim_LiftOnlyLocks_w,d5
                    if ((d5 & (1 << (d2 & 31))) == 0) { // btst d2,d5 (registre → mod 32 ; d5=word donc bits 16-31=0 → lift ≥16 toujours libre)
                        // .satisfied (1051)
                        a3 = Mem.l(Lvl_ZoneEdgePtr_l); // move.l Lvl_ZoneEdgePtr_l,a3
                        d4 = setb(0, Mem.ub(a0)); a0 += 1; // moveq #0,d4 ; move.b (a0)+,d4  (raise bits)
                        d5 = setb(0, Mem.ub(a0)); a0 += 1; // moveq #0,d5 ; move.b (a0)+,d5  (lower bits)
                        if (Mem.b(anim_LiftAtTop_b) != 0) {        // tst.b anim_LiftAtTop_b ; bne tstliftlower
                            liftSpeed = d7;                        // d7 par défaut (cas sans tap : inchangé)
                            d1 = liftLowerMask(d5);                // tstliftlower → d1, sets d7/ActionSoundFX
                            d7 = liftSpeed;
                        } else if (Mem.b(anim_LiftAtBottom_b) != 0) { // tst.b anim_LiftAtBottom_b ; bne tstliftraise
                            liftSpeed = d7;
                            d1 = liftRaiseMask(d4);                // tstliftraise → d1
                            d7 = liftSpeed;
                        } else {
                            d1 = setw(d1, 0);          // move.w #0,d1
                        }
                        lbl = BACKFROMLIFT;
                        continue;
                    }
                    // (lock path) move.w (a0)+,d5
                    d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5
                    lbl = LIFT_SIMPLE;
                    continue;
                }

                case BACKFROMLIFT: {                   // backfromlift (1065)
                    d0 = setw(d0, d0 & 255);           // and.w #255,d0
                    a3 = Mem.l(Lvl_ZoneEdgePtr_l);     // (a3 = ZoneEdge ; déjà chargé en .satisfied)
                    while (true) {                     // liftwalls
                        d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5
                        if ((short) d5 < 0) break;     // blt nomoreliftwalls
                        d5 = setw(d5, (d5 & 0xFFFF) << 4); // asl.w #4,d5
                        int a4 = a3 + (short) d5;      // lea (a3,d5.w),a4
                        d4 = setw(0, Mem.uw(a4 + 14));  // move.w 14(a4),d4
                        Mem.ww(a4 + 14, 0x8000);       // move.w #$8000,14(a4)
                        d4 = setw(d4, d4 & d1);        // and.w d1,d4
                        if ((short) d4 != 0) {         // beq.s .nothinghit
                            Mem.ww(a5, d7);            // move.w d7,(a5)
                            Mem.ww(Aud_NoiseVol_w, 50); // move.w #50,Aud_NoiseVol_w
                            Mem.ww(Aud_SampleNum_w, Mem.uw(anim_ActionSoundFX_w)); // move.w anim_ActionSoundFX_w,Aud_SampleNum_w
                            if ((short) Mem.uw(anim_ActionSoundFX_w) >= 0) { // blt.s .nothinghit
                                Mem.wb(Aud_ChannelPick_b, 1); // move.b #1,Aud_ChannelPick_b
                                Mem.wb(notifplaying, 0xFF);   // st notifplaying
                                Mem.ww(IDNUM, 0xFFFE);        // move.w #$fffe,IDNUM
                                Hires.MakeSomeNoise();        // jsr MakeSomeNoise
                            }
                        }
                        // .nothinghit
                        a1 = Mem.l(a0); a0 += 4;       // move.l (a0)+,a1
                        a1 = a1 + Mem.l(Lvl_GraphicsPtr_l); // add.l Lvl_GraphicsPtr_l,a1
                        int a2 = Mem.l(a0); a0 += 4;   // move.l (a0)+,a2
                        a2 = a2 + (short) d0;          // adda.w d0,a2
                        Mem.ww(a1 + 12, a2);           // move.w a2,12(a1)
                        Mem.wl(a1 + 20, d3);           // move.l d3,20(a1)
                    }
                    // nomoreliftwalls → doalift
                    lbl = DOALIFT;
                    continue;
                }

                case LIFT_SIMPLE: {                    // .dothesimplething/.simplecheck (1031)
                    a3 = Mem.l(Lvl_ZoneEdgePtr_l);     // move.l Lvl_ZoneEdgePtr_l,a3
                    while (true) {                     // .simplecheck
                        d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5
                        if ((short) d5 < 0) break;     // blt nomoreliftwalls
                        d5 = setw(d5, (d5 & 0xFFFF) << 4); // asl.w #4,d5
                        int a4 = a3 + (short) d5;      // lea (a3,d5.w),a4
                        Mem.ww(a4 + 14, 0);            // move.w #0,14(a4)
                        a1 = Mem.l(a0); a0 += 4;       // move.l (a0)+,a1
                        a1 = a1 + Mem.l(Lvl_GraphicsPtr_l); // add.l Lvl_GraphicsPtr_l,a1
                        int a2 = Mem.l(a0); a0 += 4;   // move.l (a0)+,a2
                        a2 = a2 + (short) d0;          // adda.w d0,a2
                        Mem.ww(a1 + 12, a2);           // move.w a2,12(a1)
                        Mem.wl(a1 + 20, d3);           // move.l d3,20(a1)
                    }
                    // nomoreliftwalls → doalift
                    lbl = DOALIFT;
                    continue;
                }
                default:
                    return;
            }
        }
    }

    // liftSpeed : d7 calculé par les sous-cas lift0-3 / rlift0-3 (champ partagé).
    private static int liftSpeed;

    /** tstliftlower (newanims.s:1107) — d5 = lower bits ; renvoie d1 (masque), pose liftSpeed. */
    private static int liftLowerMask(int d5) {
        Mem.ww(anim_ActionSoundFX_w, Mem.uw(anim_ClosingSoundFX_w)); // move.w anim_ClosingSoundFX_w,anim_ActionSoundFX_w
        int d1;
        if ((byte) d5 < 1) {                           // cmp.b #1,d5 ; blt lift0
            // lift0
            d1 = 0;                                    // moveq #0,d1
            if (Mem.b(Plr1_TmpSpcTap_b) != 0) {        // tst.b Plr1_TmpSpcTap_b ; beq .noplr1
                d1 = setw(d1, 0b100000000);            // move.w #%100000000,d1
                liftSpeed = setw(0, Mem.uw(anim_ClosingSpeed_w)); // move.w anim_ClosingSpeed_w,d7
                if (Mem.b(plr1_StoodOnLift_b) != 0) {  // tst.b plr1_StoodOnLift_b ; beq .noplr1
                    return setw(d1, 0x8000);           // move.w #$8000,d1 ; bra backfromlift
                }
            }
            // .noplr1
            if (Mem.b(Plr2_TmpSpcTap_b) != 0) {        // tst.b Plr2_TmpSpcTap_b ; beq .noplr2
                d1 = setw(d1, d1 | 0b100000000000);    // or.w #%100000000000,d1
                liftSpeed = setw(0, Mem.uw(anim_ClosingSpeed_w)); // move.w anim_ClosingSpeed_w,d7
                if (Mem.b(plr2_StoodOnLift_b) != 0) {  // tst.b plr2_StoodOnLift_b ; beq .noplr2
                    return setw(d1, 0x8000);           // move.w #$8000,d1
                }
            }
            return d1;                                 // .noplr2 → bra backfromlift
        } else if ((byte) d5 == 1) {                   // beq lift1
            liftSpeed = setw(0, Mem.uw(anim_ClosingSpeed_w)); // move.w anim_ClosingSpeed_w,d7
            if (Mem.b(plr1_StoodOnLift_b) != 0 || Mem.b(plr2_StoodOnLift_b) != 0) { // bne lift1b / bne lift1b
                return 0x8000;                         // lift1b: move.w #$8000,d1
            }
            return 0b100100000000;                     // move.w #%100100000000,d1
        } else if ((byte) d5 < 3) {                    // cmp.b #3,d5 ; blt lift2
            liftSpeed = setw(0, Mem.uw(anim_ClosingSpeed_w)); // lift2: move.w anim_ClosingSpeed_w,d7
            return 0x8000;                             // move.w #$8000,d1
        } else {                                       // lift3 (d5 >= 3)
            return 0;                                  // move.w #$0,d1
        }
    }

    /** tstliftraise (newanims.s:1172) — d4 = raise bits ; renvoie d1, pose liftSpeed. */
    private static int liftRaiseMask(int d4) {
        Mem.ww(anim_ActionSoundFX_w, Mem.uw(anim_OpeningSoundFX_w)); // move.w anim_OpeningSoundFX_w,anim_ActionSoundFX_w
        int d1;
        if ((byte) d4 < 1) {                           // cmp.b #1,d4 ; blt rlift0
            d1 = 0;                                    // moveq #0,d1
            if (Mem.b(Plr1_TmpSpcTap_b) != 0) {        // tst.b Plr1_TmpSpcTap_b ; beq .noplr1
                d1 = setw(d1, 0b100000000);            // move.w #%100000000,d1
                liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w)); // move.w anim_OpeningSpeed_w,d7
                if (Mem.b(plr1_StoodOnLift_b) != 0) {  // beq .noplr1
                    return setw(d1, 0x8000);           // move.w #$8000,d1
                }
            }
            if (Mem.b(Plr2_TmpSpcTap_b) != 0) {        // tst.b Plr2_TmpSpcTap_b ; beq .noplr2
                d1 = setw(d1, d1 | 0b100000000000);    // or.w #%100000000000,d1
                liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w)); // move.w anim_OpeningSpeed_w,d7
                if (Mem.b(plr2_StoodOnLift_b) != 0) {  // beq .noplr2
                    return setw(d1, 0x8000);           // move.w #$8000,d1
                }
            }
            return d1;                                 // bra backfromlift
        } else if ((byte) d4 == 1) {                   // beq rlift1
            liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w)); // move.w anim_OpeningSpeed_w,d7
            if (Mem.b(plr1_StoodOnLift_b) != 0 || Mem.b(plr2_StoodOnLift_b) != 0) { // bne rlift1b
                return 0x8000;                         // rlift1b: move.w #$8000,d1
            }
            return 0b100100000000;                     // move.w #%100100000000,d1
        } else if ((byte) d4 < 3) {                    // cmp.b #3,d4 ; blt rlift2
            liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w)); // rlift2: move.w anim_OpeningSpeed_w,d7
            return 0x8000;                             // move.w #$8000,d1
        } else {                                       // rlift3
            return 0;                                  // move.w #$0,d1
        }
    }

    // ==================================================================
    //  DoorRoutine (newanims.s:1237) — machine à états des portes.
    // ==================================================================
    private static final int DOADOOR = 0, BACKFROMTST = 1, DOOR_SIMPLE = 2;

    static void DoorRoutine() {
        int d0 = 0, d1 = 0, d2, d3 = 0, d4 = 0, d5 = 0, d6, d7 = 0;
        int a0, a1, a3 = 0, a5 = 0;
        int a6 = anim_DoorOpenTimers_vw;               // move.l #anim_DoorOpenTimers_vw,a6
        a0 = Mem.l(Lvl_DoorDataPtr_l);                 // move.l Lvl_DoorDataPtr_l,a0
        Mem.ww(anim_CurrentLiftable_w, -1);            // move.w #-1,anim_CurrentLiftable_w

        int lbl = DOADOOR;
        while (true) {
            switch (lbl) {
                case DOADOOR: {
                    Mem.ww(anim_CurrentLiftable_w, Mem.uw(anim_CurrentLiftable_w) + 1); // add.w #1,anim_CurrentLiftable_w
                    d0 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d0  (bottom)
                    if ((short) d0 == 999) {           // cmp.w #999,d0 ; bne notalldoorsdone
                        Mem.ww(a6, 999);               // move.w #999,(a6)
                        Mem.ww(Anim_DoorAndLiftLocks_l, 0); // move.w #0,Anim_DoorAndLiftLocks_l (word = mot fort)
                        return;                        // rts
                    }
                    // notalldoorsdone (1256)
                    d1 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d1  (top)
                    Mem.ww(anim_OpeningSpeed_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_OpeningSpeed_w, -(short) Mem.uw(anim_OpeningSpeed_w)); // neg
                    Mem.ww(anim_ClosingSpeed_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_OpenDuration_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_OpeningSoundFX_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_ClosingSoundFX_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_OpenedSoundFX_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_ClosedSoundFX_w, Mem.uw(a0)); a0 += 2;
                    Mem.ww(anim_OpeningSoundFX_w, Mem.uw(anim_OpeningSoundFX_w) - 1);
                    Mem.ww(anim_ClosingSoundFX_w, Mem.uw(anim_ClosingSoundFX_w) - 1);
                    Mem.ww(anim_OpenedSoundFX_w, Mem.uw(anim_OpenedSoundFX_w) - 1);
                    Mem.ww(anim_ClosedSoundFX_w, Mem.uw(anim_ClosedSoundFX_w) - 1);
                    d2 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d2  (18)
                    d3 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d3  (20)
                    d2 = setw(d2, d2 - Mem.uw(Plr1_TmpXOff_l));
                    d3 = setw(d3, d3 - Mem.uw(Plr1_TmpZOff_l));
                    d4 = muls(Mem.w(Vis_CosVal_w), d2);
                    d5 = muls(Mem.w(Vis_SinVal_w), d3);
                    d4 = d4 - d5; d4 = d4 + d4; d4 = swap(d4);
                    Mem.ww(Aud_NoiseX_w, d4);          // Aud_NoiseX
                    d4 = muls(Mem.w(Vis_SinVal_w), d2);
                    d5 = muls(Mem.w(Vis_CosVal_w), d3);
                    d4 = d4 - d5; d4 = d4 + d4; d4 = swap(d4);
                    Mem.ww(Aud_NoiseZ_w, d4);          // Aud_NoiseZ
                    d3 = setw(0, Mem.uw(a0));          // move.w (a0),d3   (22 pos)
                    d2 = setw(0, Mem.uw(a0 + 2));      // move.w 2(a0),d2  (24 speed)
                    d7 = setw(0, Mem.uw(a0 + 8));      // move.w 8(a0),d7  (30 zone)
                    a1 = Mem.l(Lvl_ZonePtrsPtr_l);
                    a1 = Mem.l(a1 + (d7 & 0xFFFF) * 4);
                    Mem.wb(PlayEcho, Mem.ub(a1 + ZoneT_Echo_b));
                    d2 = muls(d2, Mem.uw(Anim_TempFrames_w)); // muls Anim_TempFrames_w,d2
                    d3 = setw(d3, d3 + d2);            // add.w d2,d3
                    d2 = setw(0, Mem.uw(a0 + 2));      // move.w 2(a0),d2  (24 re-read speed)
                    Mem.wb(anim_DoorClosed_b, ((short) d0 <= (short) d3) ? 0xFF : 0); // cmp d3,d0 ; sle anim_DoorClosed_b
                    if ((short) d0 > (short) d3) {     // bgt.s nolower
                    } else {
                        if ((short) d2 != 0) {         // tst.w d2 ; beq .nonoise
                            Mem.ww(Aud_NoiseVol_w, 50);
                            Mem.ww(Aud_SampleNum_w, Mem.uw(anim_ClosedSoundFX_w));
                            if ((short) Mem.uw(anim_ClosedSoundFX_w) >= 0) {
                                Mem.wb(Aud_ChannelPick_b, 1);
                                Mem.wb(notifplaying, 0);
                                Mem.ww(IDNUM, 0xFFFD);
                                Hires.MakeSomeNoise();
                            }
                        }
                        // .nonoise
                        d2 = 0;                        // moveq #0,d2
                        d0 = setw(d0, d3);             // move.w d3,d0
                    }
                    // nolower (1322)
                    Mem.wb(anim_DoorOpen_b, ((short) d1 >= (short) d3) ? 0xFF : 0); // cmp d3,d1 ; sge anim_DoorOpen_b
                    if ((short) d1 < (short) d3) {     // blt.s noraise
                    } else {
                        if ((short) d2 != 0) {         // tst.w d2 ; beq .nonoise
                            Mem.ww(a6, 0);             // move.w #0,(a6)
                            Mem.ww(Aud_NoiseVol_w, 50);
                            Mem.ww(Aud_SampleNum_w, Mem.uw(anim_OpenedSoundFX_w));
                            if ((short) Mem.uw(anim_OpenedSoundFX_w) >= 0) {
                                Mem.wb(Aud_ChannelPick_b, 1);
                                Mem.wb(notifplaying, 0);
                                Mem.ww(IDNUM, 0xFFFD);
                                Hires.MakeSomeNoise();
                            }
                        }
                        // .nonoise
                        d3 = setw(d3, d1);             // move.w d1,d3
                        d2 = 0;                        // moveq #0,d2
                    }
                    // noraise / NOTMOVING (1347) — état des portes pour la visibilité
                    d6 = setw(0, Mem.uw(Zone_CurrentDoorState_w)); // move.w Zone_CurrentDoorState_w,d6
                    d7 = setw(0, Mem.uw(anim_CurrentLiftable_w));  // move.w anim_CurrentLiftable_w,d7
                    if (Mem.b(anim_DoorClosed_b) != 0) {           // tst.b anim_DoorClosed_b ; bne .clear_door_state
                        d6 = d6 & ~(1 << (d7 & 31));    // bclr d7,d6
                    } else {
                        d6 = d6 | (1 << (d7 & 31));     // bset d7,d6
                    }
                    Mem.ww(Zone_CurrentDoorState_w, d6); // move.w d6,Zone_CurrentDoorState_w
                    d0 = setw(d0, d0 - d3);            // sub.w d3,d0
                    d6 = ((short) d0 >= 15 * 16) ? 0xFF : 0; // cmp.w #15*16,d0 ; sge d6
                    Mem.ww(a0, d3); a0 += 2;           // move.w d3,(a0)+  (22)
                    a5 = a0;                           // move.l a0,a5
                    Mem.ww(a0, d2); a0 += 2;           // move.w d2,(a0)+  (24)
                    d7 = setw(d7, d2);                 // move.w d2,d7
                    a1 = Mem.l(a0); a0 += 4;           // move.l (a0)+,a1  (26 graphics)
                    a1 = a1 + Mem.l(Lvl_GraphicsPtr_l);
                    Mem.ww(a1 + 2, d3);                // move.w d3,2(a1)
                    d3 = setw(d3, ((short) d3) >> 2);  // asr.w #2,d3
                    d0 = setw(d0, d3);                 // move.w d3,d0
                    d3 = muls(d3, 256);                // muls #256,d3
                    a1 = Mem.l(Lvl_ZonePtrsPtr_l);     // move.l Lvl_ZonePtrsPtr_l,a1
                    d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5  (30 zone)
                    a1 = Mem.l(a1 + (d5 & 0xFFFF) * 4); // move.l (a1,d5.w*4),a1
                    Mem.wl(a1 + 6, d3);                // move.l d3,6(a1)
                    d0 = setw(d0, -(short) d0);        // neg.w d0
                    d0 = setw(d0, d0 & 255);           // and.w #255,d0
                    boolean goBackUp = ((short) d5 == (short) Mem.uw(Plr2_Zone_w)) // cmp Plr2_Zone_w,d5 ; beq .gobackup
                            || ((short) d5 == (short) Mem.uw(Plr1_Zone_w));        // cmp Plr1_Zone_w,d5 ; bne NotGoBackUp
                    boolean toBackfromtst = false;
                    if (goBackUp) {
                        // .gobackup
                        if (Mem.b(anim_DoorOpen_b) == 0 && (short) d2 >= 0) { // tst.b anim_DoorOpen_b ; bne NotGoBackUp ; tst.w d2 ; blt NotGoBackUp
                            d7 = setw(d7, -16);        // move.w #-16,d7
                            d1 = setw(d1, 0x8000);     // move.w #$8000,d1
                            d2 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d2  (32)
                            d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5  (34 conditions)
                            toBackfromtst = true;      // bra backfromtst
                        }
                    }
                    if (!toBackfromtst) {
                        // NotGoBackUp (1416)
                        a0 += 2;                       // move.w (a0)+,d2  (32, valeur ignorée)
                        d2 = setw(0, Mem.uw(anim_CurrentLiftable_w)); // move.w anim_CurrentLiftable_w,d2
                        d5 = setw(0, Mem.uw(Anim_DoorAndLiftLocks_l)); // move.w Anim_DoorAndLiftLocks_l,d5
                        if ((d5 & (1 << (d2 & 31))) != 0) { // btst d2,d5 (registre → mod 32 ; d5=word donc bits 16-31=0 → porte ≥16 toujours ouverte)
                            // (lock path) move.w (a0)+,d5
                            d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5  (34)
                            lbl = DOOR_SIMPLE;
                            continue;
                        }
                        // satisfied (1447)
                        d5 = setb(0, Mem.ub(a0)); a0 += 1; // moveq #0,d5 ; move.b (a0)+,d5  (open bits)
                        d4 = setb(0, Mem.ub(a0)); a0 += 1; // moveq #0,d4 ; move.b (a0)+,d4  (close bits)
                        if (Mem.b(anim_DoorOpen_b) != 0) {        // tst.b anim_DoorOpen_b ; bne tstdoortoclose
                            liftSpeed = d7;
                            d1 = doorCloseMask(a6);    // tstdoortoclose → d1
                            d7 = liftSpeed;
                        } else if (Mem.b(anim_DoorClosed_b) != 0) { // tst.b anim_DoorClosed_b ; bne tstdoortoopen
                            liftSpeed = d7;
                            d1 = doorOpenMask(d5);     // tstdoortoopen → d1
                            d7 = liftSpeed;
                        } else {
                            d1 = setw(d1, 0);          // move.w #$0,d1
                        }
                    }
                    lbl = BACKFROMTST;                 // backfromtst
                    continue;
                }

                case BACKFROMTST: {                    // backfromtst (1460)
                    a3 = Mem.l(Lvl_ZoneEdgePtr_l);     // move.l Lvl_ZoneEdgePtr_l,a3
                    while (true) {                     // doorwalls
                        d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5
                        if ((short) d5 < 0) break;     // blt.s nomoredoorwalls
                        d5 = setw(d5, (d5 & 0xFFFF) << 4); // asl.w #4,d5
                        int a4 = a3 + (short) d5;      // lea (a3,d5.w),a4
                        d4 = setw(0, Mem.uw(a4 + 14));  // move.w 14(a4),d4
                        Mem.ww(a4 + 14, 0x8000);       // move.w #$8000,14(a4)
                        d4 = setw(d4, d4 & d1);        // and.w d1,d4
                        if ((short) d4 != 0) {         // beq.s nothinghit
                            Mem.ww(a5, d7);            // move.w d7,(a5)
                            Mem.ww(Aud_NoiseVol_w, 50);
                            Mem.ww(Aud_SampleNum_w, Mem.uw(anim_ActionSoundFX_w));
                            if ((short) Mem.uw(anim_ActionSoundFX_w) >= 0) { // blt.s nothinghit
                                Mem.wb(Aud_ChannelPick_b, 1);
                                Mem.wb(notifplaying, 0); // clr.b notifplaying
                                Mem.ww(IDNUM, 0xFFFD);
                                Hires.MakeSomeNoise();
                            }
                        }
                        // nothinghit
                        a1 = Mem.l(a0); a0 += 4;       // move.l (a0)+,a1
                        a1 = a1 + Mem.l(Lvl_GraphicsPtr_l);
                        int a2 = Mem.l(a0); a0 += 4;   // move.l (a0)+,a2
                        a2 = a2 + (short) d0;          // adda.w d0,a2
                        Mem.ww(a1 + 12, a2);           // move.w a2,12(a1)
                        Mem.wl(a1 + 24, d3);           // move.l d3,24(a1)
                    }
                    // nomoredoorwalls (1496)
                    a6 += 2;                           // addq #2,a6
                    lbl = DOADOOR;
                    continue;
                }

                case DOOR_SIMPLE: {                    // dothesimplething/simplecheck (1426)
                    a3 = Mem.l(Lvl_ZoneEdgePtr_l);     // move.l Lvl_ZoneEdgePtr_l,a3
                    while (true) {                     // simplecheck
                        d5 = setw(0, Mem.uw(a0)); a0 += 2; // move.w (a0)+,d5  (36)
                        if ((short) d5 < 0) break;     // blt nomoredoorwalls
                        d5 = setw(d5, (d5 & 0xFFFF) << 4); // asl.w #4,d5
                        int a4 = a3 + (short) d5;      // lea (a3,d5.w),a4
                        Mem.ww(a4 + EdgeT_Flags_w, 0); // move.w #0,EdgeT_Flags_w(a4)
                        a1 = Mem.l(a0); a0 += 4;       // move.l (a0)+,a1
                        a1 = a1 + Mem.l(Lvl_GraphicsPtr_l);
                        int a2 = Mem.l(a0); a0 += 4;   // move.l (a0)+,a2
                        a2 = a2 + (short) d0;          // adda.w d0,a2
                        Mem.ww(a1 + 12, a2);           // move.w a2,12(a1)
                        Mem.wl(a1 + 24, d3);           // move.l d3,24(a1)
                    }
                    a6 += 2;                           // (nomoredoorwalls) addq #2,a6
                    lbl = DOADOOR;
                    continue;
                }
                default:
                    return;
            }
        }
    }

    /** tstdoortoopen (newanims.s:1502) — d5 = open bits ; renvoie d1, pose liftSpeed (d7). */
    private static int doorOpenMask(int d5) {
        Mem.ww(anim_ActionSoundFX_w, Mem.uw(anim_OpeningSoundFX_w)); // move.w anim_OpeningSoundFX_w,anim_ActionSoundFX_w
        if ((short) d5 < 1) {                          // cmp.w #1,d5 ; blt door0
            int d1 = 0;                                // door0: move.w #$0,d1
            if (Mem.b(Plr1_TmpSpcTap_b) != 0) d1 = setw(d1, 0b100000000);       // tst Plr1 ; or %100000000
            if (Mem.b(Plr2_TmpSpcTap_b) != 0) d1 = setw(d1, d1 | 0b100000000000); // tst Plr2 ; or %100000000000
            liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w)); // move.w anim_OpeningSpeed_w,d7
            return d1;
        } else if ((short) d5 == 1) {                  // beq door1
            liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w));
            return 0b100100000000;                     // move.w #%100100000000,d1
        } else if ((short) d5 < 3) {                   // cmp.w #3,d5 ; blt door2
            liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w));
            return 0b10000000000;                      // door2: move.w #%10000000000,d1
        } else if ((short) d5 == 3) {                  // beq door3
            liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w));
            return 0b1000000000;                       // door3: move.w #%1000000000,d1
        } else if ((short) d5 < 5) {                   // cmp.w #5,d5 ; blt door4
            liftSpeed = setw(0, Mem.uw(anim_OpeningSpeed_w));
            return 0x8000;                             // door4: move.w #$8000,d1
        } else {                                       // door5 (d5 >= 5)
            return 0;                                  // move.w #$0,d1 (d7 inchangé)
        }
    }

    /** tstdoortoclose (newanims.s:1560) — timer d'ouverture ; renvoie d1, pose liftSpeed (d7). a6 = timer. */
    private static int doorCloseMask(int a6) {
        int d1 = setw(0, Mem.uw(Anim_TempFrames_w));   // move.w Anim_TempFrames_w,d1
        d1 = setw(d1, d1 + Mem.uw(a6));                // add.w (a6),d1
        Mem.ww(a6, d1);                                // move.w d1,(a6)
        int d4 = 0;
        if ((short) d1 < (short) Mem.uw(anim_OpenDuration_w)) { // cmp.w anim_OpenDuration_w,d1 ; bge .oktoclose
            d4 = setw(0, 1);                           // move.w #1,d4
        }
        // .oktoclose
        Mem.ww(anim_ActionSoundFX_w, Mem.uw(anim_ClosingSoundFX_w)); // move.w anim_ClosingSoundFX_w,anim_ActionSoundFX_w
        if ((short) d4 == 0) {                         // tst.w d4 ; beq dclose0
            liftSpeed = setw(0, Mem.uw(anim_ClosingSpeed_w)); // dclose0: move.w anim_ClosingSpeed_w,d7
            return 0x8000;                             // move.w #$8000,d1
        }
        // dclose1
        return 0;                                      // move.w #$0,d1 (d7 inchangé)
    }

    // ==================================================================
    //  objmoveanim (newanims.s:764) — driver per-frame des objets/anims.
    // ==================================================================
    static void objmoveanim() {
        int d0 = setw(0, Mem.uw(Plr1_Zone_w));         // move.w Plr1_Zone_w,d0
        int a0 = Mem.l(Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a0
        Mem.ww(Plr1_Zone_w, Mem.uw(a0));               // move.w (a0),Plr1_Zone_w
        if ((short) Mem.uw(Plr1_Zone_w) != (short) d0) { // cmp Plr1_Zone_w,d0 ; beq .not_changed
            Macros.SET_MEM_BIT(GAME_EVENTBIT_ZONE_CHANGE, Game_ProgressSignal_l); // SET_MEM_BIT
        }
        // .not_changed
        a0 = Mem.l(Plr2_ZonePtr_l);                    // move.l Plr2_ZonePtr_l,a0
        Mem.ww(Plr2_Zone_w, Mem.uw(a0));               // move.w (a0),Plr2_Zone_w
        if (Mem.ub(Plr_MultiplayerType_b) == PLR_SINGLE) { // cmp.b #PLR_SINGLE ; bne .okp2
            Mem.ww(Plr2_Zone_w, -5);                   // move.w #-5,Plr2_Zone_w
        }
        // .okp2
        Mem.ww(Plr1_NoiseVol_w, 0);                    // move.w #0,Plr1_NoiseVol_w
        Mem.ww(Plr2_NoiseVol_w, 0);                    // move.w #0,Plr2_NoiseVol_w
        Mem.wl(AI_BoredomPtr_l, AI_BoredomSpace_vl);   // move.l #AI_BoredomSpace_vl,AI_BoredomPtr_l
        BACKSFX();                                     // bsr BACKSFX
        Newplayershoot.Plr1_Shot();                    // bsr Plr1_Shot
        Newplayershoot.Plr2_Shot();                    // bsr Plr2_Shot
        ObjectHandler();                               // bsr ObjectHandler
        DoorRoutine();                                 // bsr DoorRoutine
        Mem.ww(Plr1_FloorSpd_w, 0);                    // move.w #0,Plr1_FloorSpd_w
        Mem.ww(Plr2_FloorSpd_w, 0);                    // move.w #0,Plr2_FloorSpd_w
        LiftRoutine();                                 // bsr LiftRoutine
        if ((short) Mem.uw(Anim_Timer_w) <= 0) {       // cmp #0,Anim_Timer_w ; bgt .notzero
            brightanim();                              // bsr brightanim
            Mem.ww(Anim_Timer_w, 5);                   // move.w #5,Anim_Timer_w
            int dr = Mem.l(otherrip);                  // move.l otherrip,d0
            Mem.wl(otherrip, Mem.l(RipTear));          // move.l RipTear,otherrip
            Mem.wl(RipTear, dr);                       // move.l d0,RipTear
        }
        // .notzero: rts
    }

    // ==================================================================
    //  ObjectHandler (newanims.s:1773) — itère les objets, dispatch alien/objet/balle.
    // ==================================================================
    static void ObjectHandler() {
        Mem.wl(WorkspacePtr_l, ObjectWorkspace_vl);    // move.l #ObjectWorkspace_vl,WorkspacePtr_l
        Mem.wl(AI_DamagePtr_l, AI_Damaged_vw);         // move.l #AI_Damaged_vw,AI_DamagePtr_l
        int a0 = Mem.l(Lvl_ObjectDataPtr_l);           // move.l Lvl_ObjectDataPtr_l,a0
        while (true) {                                 // Objectloop
            if ((short) Mem.uw(a0) < 0) return;        // tst.w (a0) ; blt doneallobj
            Mem.ww(a0 + EntT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),EntT_ZoneID_w(a0)
            if ((short) Mem.uw(a0 + ObjT_ZoneID_w) >= 0) { // blt doneobj
                int d0 = Mem.ub(a0 + ObjT_TypeID_b);   // move.b ObjT_TypeID_b(a0),d0
                if ((byte) d0 < 1) {                   // cmp.b #1,d0 ; blt JUMPALIEN
                    // JUMPALIEN
                    if ((short) Mem.uw(a0 + ObjT_ZoneID_w) >= 0) { // tst.w ObjT_ZoneID_w(a0) ; blt .dontworry
                        if (Mem.b(a0 + EntT_HitPoints_b) != 0) { // tst.b EntT_HitPoints_b(a0) ; beq .nolock
                            Mem.wl(Anim_DoorAndLiftLocks_l,
                                    Mem.l(Anim_DoorAndLiftLocks_l) | Mem.l(a0 + EntT_DoorsAndLiftsHeld_l)); // or.l
                        }
                        // .nolock
                        if (Mem.b(a0 + ShotT_Worry_b) != 0) { // tst.b ShotT_Worry_b(a0) ; beq .dontworry
                            Newaliencontrol.ItsAnAlien(a0); // jsr ItsAnAlien
                            if ((short) Mem.uw(a0 + ObjT_ZoneID_w - ObjT_SizeOf_l) >= 0) { // tst.w ObjT_ZoneID_w-ObjT_SizeOf_l(a0) ; blt .not_aux
                                Mem.ww(a0 + ObjT_ZoneID_w - ObjT_SizeOf_l, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w
                                Mem.ww(a0 + EntT_ZoneID_w - ObjT_SizeOf_l, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w
                            }
                        }
                    }
                } else if ((byte) d0 == 1) {           // beq JUMPOBJECT
                    if ((short) Mem.uw(a0 + ObjT_ZoneID_w) >= 0) { // tst.w ; blt .dontworry
                        Newaliencontrol.ItsAnObject(a0); // jsr ItsAnObject
                    }
                } else if ((byte) d0 == 2) {           // cmp.b #2,d0 ; beq JUMPBULLET
                    ItsABullet(a0);                    // jsr ItsABullet
                }
            }
            // doneobj
            a0 += ObjT_SizeOf_l;                        // adda.w #ObjT_SizeOf_l,a0
            Mem.wl(WorkspacePtr_l, Mem.l(WorkspacePtr_l) + 8);   // add.l #8,WorkspacePtr_l
            Mem.wl(AI_DamagePtr_l, Mem.l(AI_DamagePtr_l) + 2);   // add.l #2,AI_DamagePtr_l
            Mem.wl(AI_BoredomPtr_l, Mem.l(AI_BoredomPtr_l) + 8); // add.l #8,AI_BoredomPtr_l
        }
    }

    // ==================================================================
    //  ComputeBlast (newanims.s:2845) — dégâts de zone (splash) + flammes.
    //  d0 = force explosive ; a0 = objet source (la balle).
    // ==================================================================
    static void ComputeBlast(int d0, int a0) {
        Mem.ww(anim_DoneFlames_w, 0);                  // clr.w anim_DoneFlames_w
        int d6 = setw(0, d0);                          // move.w d0,d6
        Mem.ww(anim_MaxDamage_w, d0);                  // move.w d0,anim_MaxDamage_w
        int d1 = setw(0, d0);                          // move.w d0,d1
        d6 = (short) d6;                               // ext.l d6
        d1 = setw(d1, -(short) d1);                    // neg.w d1
        d0 = setw(0, Mem.uw(a0 + ObjT_ZoneID_w));      // move.w ObjT_ZoneID_w(a0),d0
        int a2 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a2
        a2 = Mem.l(a2 + (d0 & 0xFFFF) * 4);            // move.l (a2,d0.w*4),a2
        Mem.wl(anim_MiddleRoom_l, a2);                 // move.l a2,anim_MiddleRoom_l
        a2 = Mem.l(Lvl_ObjectDataPtr_l);               // move.l Lvl_ObjectDataPtr_l,a2
        a2 -= ObjT_SizeOf_l;                           // PREV_OBJ a2
        d6 = (short) d6;                               // ext.l d6 (re-sign-extend, no-op)
        final int savedA0 = a0;                        // move.l a0,-(a7)

        int d2, d3, d4, d5, d7;
        while (true) {                                 // HitObjLoop
            Mem.wl(Obj_FromZonePtr_l, Mem.l(anim_MiddleRoom_l)); // move.l anim_MiddleRoom_l,Obj_FromZonePtr_l
            a2 += ObjT_SizeOf_l;                        // NEXT_OBJ a2
            d0 = setw(0, Mem.uw(a2));                   // move.w (a2),d0
            if ((short) d0 < 0) break;                 // blt CheckedEmAll
            if ((short) Mem.uw(a2 + ObjT_ZoneID_w) < 0) continue; // tst.w ObjT_ZoneID_w(a2) ; blt HitObjLoop
            d1 = setb(0, Mem.ub(a2 + ObjT_TypeID_b));  // moveq #0,d1 ; move.b ObjT_TypeID_b(a2),d1
            boolean checkAlien;
            if ((byte) d1 == OBJ_TYPE_OBJECT) continue; // cmp.b #OBJ_TYPE_OBJECT,d1 ; beq HitObjLoop
            else if ((byte) d1 < OBJ_TYPE_OBJECT) checkAlien = true; // blt .checkalien
            else if ((byte) d1 == OBJ_TYPE_AUX) continue; // cmp.b #OBJ_TYPE_AUX,d1 ; beq HitObjLoop
            else if ((byte) d1 > OBJ_TYPE_AUX) checkAlien = true; // bgt .checkalien
            else {
                // bullet (OBJECT < d1 < AUX)
                d7 = setb(0, Mem.ub(a2 + ShotT_Size_b)); // moveq #0,d7 ; move.b ShotT_Size_b(a2),d7
                int a3 = Mem.l(GLF_DatabasePtr_l);     // move.l GLF_DatabasePtr_l,a3
                d7 = muls(d7, BulT_SizeOf_l);          // muls #BulT_SizeOf_l,d7
                a3 = a3 + GLFT_BulletDefs_l + d7;      // add.l #GLFT_BulletDefs_l,a3 ; add.l d7,a3
                if (Mem.l(a3 + BulT_Gravity_l) == 0) continue; // tst.l BulT_Gravity_l(a3) ; beq HitObjLoop
                checkAlien = false;                    // bra .okblast
            }
            if (checkAlien) {                          // .checkalien
                if (Mem.b(a2 + EntT_HitPoints_b) == 0) continue; // tst.b EntT_HitPoints_b(a2) ; beq HitObjLoop
            }
            // .okblast
            d1 = setw(0, Mem.uw(a2 + ObjT_ZoneID_w));  // move.w ObjT_ZoneID_w(a2),d1
            int a3 = Mem.l(Lvl_ZonePtrsPtr_l);         // move.l Lvl_ZonePtrsPtr_l,a3
            a3 = Mem.l(a3 + (d1 & 0xFFFF) * 4);        // move.l (a3,d1.w*4),a3
            Mem.wl(Obj_ToZonePtr_l, a3);               // move.l a3,Obj_ToZonePtr_l
            a3 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a3
            Mem.ww(Targetx, Mem.uw(a3 + (d0 & 0xFFFF) * 8));     // move.w (a3,d0.w*8),Targetx
            Mem.ww(Targetz, Mem.uw(a3 + (d0 & 0xFFFF) * 8 + 4)); // move.w 4(a3,d0.w*8),Targetz
            Mem.ww(Targety, Mem.uw(a2 + 4));           // move.w 4(a2),Targety
            Mem.wb(TargetTop, Mem.ub(a2 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a2),TargetTop
            Objectmove.CanItBeSeen();                  // jsr CanItBeSeen
            if (Mem.b(CanSee) == 0) continue;          // tst.b CanSee ; beq HitObjLoop

            d0 = setw(0, Mem.uw(Targetx));             // move.w Targetx,d0
            d0 = setw(d0, d0 - Mem.uw(Viewerx));       // sub.w Viewerx,d0
            d2 = setw(0, d0);                          // move.w d0,d2
            d1 = setw(0, Mem.uw(Targetz));             // move.w Targetz,d1
            d1 = setw(d1, d1 - Mem.uw(Viewerz));       // sub.w Viewerz,d1
            d3 = setw(0, d1);                          // move.w d1,d3
            d2 = muls(d2, d2);                         // muls d2,d2
            d3 = muls(d3, d3);                         // muls d3,d3
            d4 = setw(0, 1);                           // move.w #1,d4
            d2 = d2 + d3;                              // add.l d3,d2
            if (d2 != 0) {                             // beq .oksqr
                d4 = setw(0, 31);                      // move.w #31,d4
                while (true) {                         // .findhigh (dbne)
                    if ((d2 & (1 << (d4 & 31))) != 0) break; // btst d4,d2 ; dbne (exit si bit posé)
                    d4 = setw(d4, d4 - 1);
                    if ((short) d4 == -1) break;
                }
                // .foundhigh
                d4 = setw(d4, ((short) d4) >> 1);      // asr.w #1,d4
                d3 = (1 << (d4 & 31));                 // clr.l d3 ; bset d4,d3
                d4 = d3;                               // move.l d3,d4
                d3 = setw(0, d4);                      // move.w d4,d3
                d3 = muls(d3, d3);                     // muls d3,d3
                d3 = d3 - d2;                          // sub.l d2,d3
                d3 = d3 >> 1;                          // asr.l #1,d3
                d3 = divs(d3, d4);                     // divs d4,d3
                d4 = setw(d4, d4 - d3);                // sub.w d3,d4
                if (!((short) d4 > 0)) d4 = setw(d4, 1); // bgt .stillnot0 ; move.w #1,d4
                for (int it = 0; it < 2; it++) {       // .stillnot02, .stillnot03
                    d3 = setw(0, d4);                  // move.w d4,d3
                    if (it == 0) d3 = muls(d3, d1);    // (1re itér : muls d1,d3 — quirk de l'original)
                    else d3 = muls(d3, d3);            // muls d3,d3
                    d3 = d3 - d2;                      // sub.l d2,d3
                    d3 = d3 >> 1;                      // asr.l #1,d3
                    d3 = divs(d3, d4);                 // divs d4,d3
                    d4 = setw(d4, d4 - d3);            // sub.w d3,d4
                    if (!((short) d4 > 0)) d4 = setw(d4, 1);
                }
            }
            // .oksqr
            d3 = setw(0, d4);                          // move.w d4,d3
            d7 = setw(0, d3);                          // move.w d3,d7
            if ((short) d7 < 256) d7 = setw(d7, 256);  // cmp.w #256,d7 ; bge .okd ; move.w #256,d7
            // .okd
            d3 = setw(d3, ((short) d3) >> 3);          // asr.w #3,d3
            d3 = setw(d3, d3 - 4);                     // sub.w #4,d3
            if ((short) d3 < 0) d3 = setw(d3, 0);      // bge OkItsnotzero ; moveq #0,d3
            // OkItsnotzero
            if ((short) d3 > 64) continue;             // cmp.w #64,d3 ; bgt HitObjLoop
            d3 = setw(d3, -(short) d3);                // neg.w d3
            d3 = setw(d3, d3 + 64);                    // add.w #64,d3
            d5 = setw(0, d6);                          // move.w d6,d5
            d5 = muls(d5, d3);                         // muls d3,d5
            d5 = d5 >> 5;                              // asr.l #5,d5
            if ((short) d5 >= (short) Mem.uw(anim_MaxDamage_w)) d5 = setw(d5, Mem.uw(anim_MaxDamage_w)); // cmp.w anim_MaxDamage_w,d5 ; blt okdamage ; move.w
            // okdamage
            Mem.wb(a2 + EntT_DamageTaken_b, Mem.ub(a2 + EntT_DamageTaken_b) + d5); // add.b d5,EntT_DamageTaken_b(a2)
            d0 = (short) d0;                           // ext.l d0
            d1 = (short) d1;                           // ext.l d1
            d0 = d0 * d6;                              // muls.l d6,d0
            d1 = d1 * d6;                              // muls.l d6,d1
            d0 = divs(d0, d7);                         // divs d7,d0
            d1 = divs(d1, d7);                         // divs d7,d1
            d2 = setb(0, Mem.ub(a2 + ObjT_TypeID_b));  // move.b ObjT_TypeID_b(a2),d2
            if ((byte) d2 == 2) {                      // cmp.b #2,d2 ; bne .impactalien
                // bullet impact (knockback velocity)
                Mem.ww(a2 + ShotT_VelocityX_w, Mem.uw(a2 + ShotT_VelocityX_w) + d0); // add.w d0,ShotT_VelocityX_w(a2)
                Mem.ww(a2 + ShotT_VelocityZ_w, Mem.uw(a2 + ShotT_VelocityZ_w) + d1); // add.w d1,ShotT_VelocityZ_w(a2)
                int dy = d6;                           // move.l d6,d1
                dy = dy << 8;                          // asl.l #8,d1
                dy = dy << 4;                          // asl.l #4,d1
                dy = divs(dy, d7);                     // divs d7,d1
                dy = setw(dy, -(short) dy);            // neg.w d1
                if ((short) dy < -8 * 256) dy = setw(dy, -8 * 256); // cmp.w #-8*256,d1 ; bge .okbl ; move.w
                Mem.ww(a2 + ShotT_VelocityY_w, Mem.uw(a2 + ShotT_VelocityY_w) + dy); // add.w d1,ShotT_VelocityY_w(a2)
            } else {
                // .impactalien
                Mem.ww(a2 + EntT_ImpactX_w, d0);       // move.w d0,EntT_ImpactX_w(a2)
                Mem.ww(a2 + EntT_ImpactZ_w, d1);       // move.w d1,EntT_ImpactZ_w(a2)
                int dy = d6;                           // move.l d6,d1
                dy = dy << 4;                          // asl.l #4,d1
                dy = divs(dy, d7);                     // divs d7,d1
                dy = setw(dy, -(short) dy);            // neg.w d1
                if ((short) dy < -8) dy = setw(dy, -8); // cmp.w #-8,d1 ; bge .okbl2 ; move.w #-8,d1
                Mem.ww(a2 + EntT_ImpactY_w, dy);       // move.w d1,EntT_ImpactY_w(a2)
            }
            // .impactedbul → HitObjLoop
        }

        // CheckedEmAll — pose les flammes
        a0 = savedA0;                                  // move.l (a7)+,a0
        d0 = setw(0, Mem.uw(a0));                      // move.w (a0),d0
        a2 = Mem.l(Lvl_ObjectPointsPtr_l);             // move.l Lvl_ObjectPointsPtr_l,a2
        d1 = setw(0, Mem.uw(a2 + (d0 & 0xFFFF) * 8));  // move.w (a2,d0.w*8),d1
        d2 = setw(0, Mem.uw(a2 + (d0 & 0xFFFF) * 8 + 4)); // move.w 4(a2,d0.w*8),d2
        Mem.ww(anim_MiddleX_w, d1);                    // move.w d1,anim_MiddleX_w
        Mem.ww(anim_MiddleZ_w, d2);                    // move.w d2,anim_MiddleZ_w
        d7 = setw(0, 9);                               // move.w #9,d7
        Mem.wb(exitfirst, 0);                          // clr.b exitfirst
        Mem.wb(Obj_WallBounce_b, 0xFF);                // st.b Obj_WallBounce_b
        d0 = setw(0, Mem.uw(a0 + 12));                 // move.w 12(a0),d0
        int a3 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a3
        a3 = Mem.l(a3 + (d0 & 0xFFFF) * 4);            // move.l (a3,d0.w*4),a3
        Mem.wl(anim_MiddleRoom_l, a3);                 // move.l a3,anim_MiddleRoom_l
        a3 = Mem.l(Plr_ShotDataPtr_l);                 // move.l Plr_ShotDataPtr_l,a3
        d0 = setw(0, Mem.uw(a0 + 4));                  // move.w 4(a0),d0
        d0 = (short) d0;                               // ext.l d0
        d0 = d0 << 7;                                  // asl.l #7,d0
        Mem.wl(oldy, d0);                              // move.l d0,oldy
        d5 = 2;                                        // moveq #2,d5
        Mem.ww(NewanimsData.NUMTOCHECK, NUM_PLR_SHOT_DATA - 1); // move.w #NUM_PLR_SHOT_DATA-1,NUMTOCHECK
        d6 = setw(0, 2);                               // move.w #2,d6

        while (true) {                                 // radiusloop
            d7 = setw(0, 1);                           // move.w #1,d7
            boolean noMore = false;
            while (true) {                             // DOFLAMES
                d1 = setw(0, Mem.uw(NewanimsData.NUMTOCHECK)); // move.w NUMTOCHECK,d1
                // .findonefree
                boolean found = false;
                while (true) {
                    if ((short) Mem.uw(a3 + ObjT_ZoneID_w) < 0) { found = true; break; } // move.w ObjT_ZoneID_w(a3),d2 ; blt .foundonefree
                    a3 += ObjT_SizeOf_l;               // adda.w #ObjT_SizeOf_l,a3
                    d1 = setw(d1, d1 - 1);             // dbra d1,.findonefree
                    if ((short) d1 == -1) break;
                }
                if (!found) return;                    // rts
                // .foundonefree
                Mem.wb(a3 + 16, 2);                    // move.b #2,16(a3)
                Mem.ww(NewanimsData.NUMTOCHECK, d1);   // move.w d1,NUMTOCHECK
                Mem.ww(anim_DoneFlames_w, Mem.uw(anim_DoneFlames_w) + 1); // add.w #1,anim_DoneFlames_w
                d1 = setw(0, Mem.uw(anim_MiddleX_w));  // move.w anim_MiddleX_w,d1
                d2 = setw(0, Mem.uw(anim_MiddleZ_w));  // move.w anim_MiddleZ_w,d2
                Mem.ww(oldx, d1);                      // move.w d1,oldx
                Mem.ww(oldz, d2);                      // move.w d2,oldz
                Mem.wb(StoodInTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a0),StoodInTop
                d0 = Objectmove.GetRand();             // jsr GetRand
                d0 = extw(d0);                         // ext.w d0 (étend l'octet bas)
                d0 = muls(d0, d5);                     // muls d5,d0
                d0 = setw(d0, ((short) d0) >> 1);      // asr.w #1,d0
                if ((short) d0 == 0) d0 = setw(d0, 2); // bne .xnz ; moveq #2,d0
                // .xnz
                d1 = setw(d1, d1 + d0);                // add.w d0,d1
                d0 = Objectmove.GetRand();             // jsr GetRand
                d0 = extw(d0);                         // ext.w d0 (étend l'octet bas)
                d0 = muls(d0, d5);                     // muls d5,d0
                d0 = setw(d0, ((short) d0) >> 1);      // asr.w #1,d0
                if ((short) d0 == 0) d0 = setw(d0, 2); // bne .znz ; moveq #2,d0
                // .znz
                d2 = setw(d2, d2 + d0);                // add.w d0,d2
                int d3f = Mem.l(oldy);                 // move.l oldy,d3
                d0 = Objectmove.GetRand();             // jsr GetRand
                d0 = muls(d0, d5);                     // muls d5,d0
                d0 = d0 >> 3;                          // asr.l #3,d0
                d3f = d3f + d0;                        // add.l d0,d3
                Mem.wl(newy, d3f);                     // move.l d3,newy
                Mem.ww(newx, d1);                      // move.w d1,newx
                Mem.ww(newz, d2);                      // move.w d2,newz
                Mem.wl(Obj_ZonePtr_l, Mem.l(anim_MiddleRoom_l)); // move.l anim_MiddleRoom_l,Obj_ZonePtr_l
                // movem save d5/d6/a0/a1/a3/d7/a6 (pass by value)
                Mem.ww(Obj_ExtLen_w, 80);              // move.w #80,Obj_ExtLen_w
                Mem.wb(Obj_AwayFromWall_b, 1);         // move.b #1,Obj_AwayFromWall_b
                Objectmove.MoveObject();               // jsr MoveObject
                int a2f = Mem.l(Obj_ZonePtr_l);        // move.l Obj_ZonePtr_l,a2
                Mem.ww(a3 + 12, Mem.uw(a2f));          // move.w (a2),12(a3)
                d0 = Mem.l(newy);                      // move.l newy,d0
                int dfloor = Mem.l(a2f + ZoneT_Floor_l); // move.l ZoneT_Floor_l(a2),d1
                int droof = Mem.l(a2f + ZoneT_Roof_l); // move.l ZoneT_Roof_l(a2),d2
                if (Mem.b(a0 + ShotT_InUpperZone_b) != 0) { // tst.b ShotT_InUpperZone_b(a0) ; beq .okinbot
                    dfloor = Mem.l(a2f + ZoneT_UpperFloor_l); // move.l ZoneT_UpperFloor_l(a2),d1
                    droof = Mem.l(a2f + ZoneT_UpperRoof_l);   // move.l ZoneT_UpperRoof_l(a2),d2
                }
                // .okinbot
                if (dfloor > d0) d0 = dfloor;          // cmp.l d0,d1 ; bgt .abovefloor ; move.l d1,d0
                if (droof < d0) d0 = droof;            // cmp.l d0,d2 ; blt .belowroof ; move.l d2,d0
                // .belowroof
                Mem.wl(a3 + ShotT_AccYPos_w, d0);      // move.l d0,ShotT_AccYPos_w(a3)
                d0 = d0 >> 7;                          // asr.l #7,d0
                Mem.ww(a3 + 4, d0);                    // move.w d0,4(a3)
                Mem.wb(a3 + 16, 2);                    // move.b #2,16(a3)
                Mem.wb(a3 + ShotT_Anim_b, 0);          // move.b #0,ShotT_Anim_b(a3)
                Mem.wb(a3 + ShotT_Status_b, 0xFF);     // st ShotT_Status_b(a3)
                Mem.wb(a3 + ShotT_InUpperZone_b, Mem.ub(StoodInTop)); // move.b StoodInTop,ShotT_InUpperZone_b(a3)
                Mem.wb(a3 + ShotT_Size_b, Mem.ub(BLOODYGREATBOMB)); // move.b BLOODYGREATBOMB,ShotT_Size_b(a3)
                Mem.wb(a3 + ShotT_Worry_b, 0xFF);      // st ShotT_Worry_b(a3)
                d0 = setw(0, Mem.uw(a3));              // move.w (a3),d0
                a2f = Mem.l(Lvl_ObjectPointsPtr_l);    // move.l Lvl_ObjectPointsPtr_l,a2
                Mem.ww(a2f + (d0 & 0xFFFF) * 8, Mem.uw(newx));     // move.w newx,(a2,d0.w*8)
                Mem.ww(a2f + (d0 & 0xFFFF) * 8 + 4, Mem.uw(newz)); // move.w newz,4(a2,d0.w*8)
                a3 += 64;                              // adda.w #64,a3
                Mem.ww(NewanimsData.NUMTOCHECK, Mem.uw(NewanimsData.NUMTOCHECK) - 1); // sub.w #1,NUMTOCHECK
                if ((short) Mem.uw(NewanimsData.NUMTOCHECK) < 0) { noMore = true; break; } // blt .nomore
                d7 = setw(d7, d7 - 1);                 // dbra d7,DOFLAMES
                if ((short) d7 == -1) break;
            }
            if (noMore) return;                        // .nomore: rts
            d5 = setw(d5, d5 + 2);                     // add.w #2,d5
            d6 = setw(d6, d6 - 1);                     // dbra d6,radiusloop
            if ((short) d6 == -1) return;              // (fin) rts
        }
    }

    // ==================================================================
    //  ItsABullet (newanims.s:1966) — physique d'une balle/projectile (a0=balle).
    //  Status!=0 : animation d'impact ("popping"). Status==0 : vol (anim, collision
    //  sol/plafond/mur avec rebond, gravité, MoveObject, explosion, hit ennemis).
    // ==================================================================
    static void ItsABullet(int a0) {
        Mem.wb(timeout, 0);                            // move.b #0,timeout
        int d0 = setw(0, Mem.uw(a0 + ObjT_ZoneID_w));  // move.w ObjT_ZoneID_w(a0),d0
        Mem.ww(a0 + EntT_ZoneID_w, d0);                // move.w d0,EntT_ZoneID_w(a0)
        if ((short) d0 < 0) return;                    // blt doneshot (rts)

        int d1 = setb(0, Mem.ub(a0 + ShotT_Size_b));   // moveq #0,d1 ; move.b ShotT_Size_b(a0),d1
        d1 = muls(d1, BulT_SizeOf_l);                  // muls #BulT_SizeOf_l,d1
        int a6 = Mem.l(GLF_DatabasePtr_l) + GLFT_BulletDefs_l + d1; // move.l GLF...a6 ; lea GLFT_BulletDefs_l(a6),a6 ; add.l d1,a6

        if (Mem.b(a0 + ShotT_Status_b) == 0) {         // tst.b ShotT_Status_b(a0) ; bne noworrylife
            // lifetime (vivant)
            int d2 = setw(0, Mem.uw(a0 + ShotT_Lifetime_w)); // move.w ShotT_Lifetime_w(a0),d2
            if ((short) d2 >= 0) {                     // blt.s infinite
                int dlt = Mem.l(a6 + BulT_Lifetime_l); // move.l BulT_Lifetime_l(a6),d1
                if (dlt >= 0) {                        // blt.s infinite
                    if ((short) dlt >= (short) d2) {   // cmp.w d2,d1 ; bge.s notdone
                        // notdone : add frames
                        Mem.ww(a0 + ShotT_Lifetime_w, Mem.uw(a0 + ShotT_Lifetime_w) + Mem.uw(Anim_TempFrames_w)); // add.w Anim_TempFrames_w
                    } else {
                        Mem.wb(timeout, 0xFF);         // st timeout
                    }
                }
            }
        }
        // infinite / noworrylife
        Mem.ww(Obj_ExtLen_w, 0);                       // move.w #0,Obj_ExtLen_w
        Mem.wb(Obj_AwayFromWall_b, 0xFF);              // move.b #$ff,Obj_AwayFromWall_b

        if (Mem.b(a0 + ShotT_Status_b) != 0) {         // tst.b ShotT_Status_b(a0) ; beq notpopping
            // ---- popping (animation d'impact) ----
            int a1 = a6 + BulT_PopData_vb;             // lea BulT_PopData_vb(a6),a1
            d1 = setb(0, Mem.ub(a0 + ShotT_Anim_b));   // moveq #0,d1 ; move.b ShotT_Anim_b(a0),d1
            int d2 = setw(0, d1);                      // move.w d1,d2
            d1 = setw(d1, d1 + d1);                    // add.w d1,d1
            d1 = setw(d1, d1 + d2);                    // add.w d2,d1
            d1 = setw(d1, d1 + d1);                    // add.w d1,d1  (d1 = anim*6)
            Mem.wl(a0 + 8, 0);                         // move.l #0,8(a0)
            int gt = Mem.l(a6 + BulT_ImpactGraphicType_l); // cmp.l #1,BulT_ImpactGraphicType_l(a6)
            int idx = d1 & 0xFFFF;
            if (gt < 1) {                              // blt .bitmapgraph
                Mem.wb(a0 + 9, Mem.ub(a1 + idx));      // move.b (a1,d1.w),9(a0)
                Mem.wb(a0 + 11, Mem.ub(a1 + 1 + idx)); // move.b 1(a1,d1.w),11(a0)
                Mem.ww(a0 + 6, Mem.uw(a1 + 2 + idx));  // move.w 2(a1,d1.w),6(a0)
                Mem.wb(anim_Brightness_w, Mem.ub(a1 + 5 + idx)); // move.b 5(a1,d1.w),anim_Brightness_w
            } else if (gt == 1) {                      // beq .glaregraph
                int dg = (short) (byte) Mem.ub(a1 + idx); // move.b (a1,d1.w),d0 ; ext.w d0
                dg = -dg;                              // neg.w d0
                Mem.ww(a0 + 8, dg);                    // move.w d0,8(a0)
                Mem.wb(a0 + 11, Mem.ub(a1 + 1 + idx)); // move.b 1(a1,d1.w),11(a0)
                Mem.ww(a0 + 6, Mem.uw(a1 + 2 + idx));  // move.w 2(a1,d1.w),6(a0)
                Mem.wb(anim_Brightness_w, Mem.ub(a1 + 5 + idx));
            } else {                                   // .additivegraph (gt>1)
                Mem.wb(a0 + 9, Mem.ub(a1 + idx));
                Mem.wb(a0 + 11, Mem.ub(a1 + 1 + idx));
                Mem.wb(a0 + 10, 6);                    // move.b #6,10(a0)
                Mem.ww(a0 + 6, Mem.uw(a1 + 2 + idx));
                Mem.wb(anim_Brightness_w, Mem.ub(a1 + 5 + idx));
            }
            // .donegraph
            d2 = setw(d2, d2 + 1);                     // addq #1,d2
            if ((short) d2 > (short) Mem.uw(a6 + BulT_PopFrames_l + 2)) { // cmp.w BulT_PopFrames_l+2(a6),d2 ; ble notdonepopping
                Macros.FREE_ENT(a0);                   // FREE_ENT a0
                Mem.wb(a0 + ShotT_Status_b, 0);        // clr.b ShotT_Status_b(a0)
                Mem.wb(a0 + ShotT_Anim_b, 0);          // move.b #0,ShotT_Anim_b(a0)
                return;                                // rts
            }
            // notdonepopping
            Mem.wb(a0 + ShotT_Anim_b, d2);             // move.b d2,ShotT_Anim_b(a0)
            int db = setb(0, Mem.ub(anim_Brightness_w)); // moveq #0,d0 ; move.b anim_Brightness_w,d0
            if ((byte) db != 0) {                      // beq.s .nobright
                db = setw(db, -(short) db);            // neg.w d0
                int d2p = setw(0, Mem.uw(a0));         // move.w (a0),d2
                int a2 = Mem.l(Lvl_ObjectPointsPtr_l); // move.l Lvl_ObjectPointsPtr_l,a2
                int dx = setw(0, Mem.uw(a2 + (d2p & 0xFFFF) * 8)); // move.w (a2,d2.w*8),d1
                int dz = setw(0, Mem.uw(a2 + (d2p & 0xFFFF) * 8 + 4)); // move.w 4(a2,d2.w*8),d2
                int dy = setw(0, Mem.uw(a0 + 4));      // move.w 4(a0),d3
                dy = (short) dy;                       // ext.l d3
                dy = dy << 7;                          // asl.l #7,d3
                Mem.wl(Anim_BrightY_l, dy);            // move.l d3,Anim_BrightY_l
                int dzone = setw(0, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),d3
                anim_BrightenPoints(db, dx, dz, dzone); // jsr anim_BrightenPoints
            }
            // .nobright
            return;                                    // rts
        }

        // ---- notpopping (vol) ----
        Mem.wb(BLOODYGREATBOMB, Mem.ub(a0 + ShotT_Size_b)); // move.b ShotT_Size_b(a0),BLOODYGREATBOMB
        int a1 = a6 + BulT_AnimData_vb;                // lea BulT_AnimData_vb(a6),a1
        d1 = setb(0, Mem.ub(a0 + ShotT_Anim_b));       // moveq #0,d1 ; move.b ShotT_Anim_b(a0),d1
        d1 = setw(d1, d1 + d1);                        // add.w d1,d1
        int d2 = setw(0, d1);                          // move.w d1,d2
        d1 = setw(d1, d1 + d1);                        // add.w d1,d1
        d1 = setw(d1, d1 + d2);                        // add.w d2,d1  (d1 = anim*6)
        Mem.wl(a0 + 8, 0);                             // move.l #0,8(a0)
        int gt = Mem.l(a6 + BulT_GraphicType_l);       // cmp.l #1,BulT_GraphicType_l(a6)
        int idx = d1 & 0xFFFF;
        if (gt < 1) {                                  // blt .bitmapgraph
            Mem.wb(a0 + 9, Mem.ub(a1 + idx));
            Mem.wb(a0 + 11, Mem.ub(a1 + 1 + idx));
            Mem.ww(a0 + 6, Mem.uw(a1 + 2 + idx));
            Mem.wb(anim_Brightness_w, Mem.ub(a1 + 5 + idx));
        } else if (gt == 1) {                          // beq .glaregraph
            int dg = (short) (byte) Mem.ub(a1 + idx);  // move.b (a1,d1.w),d0 ; ext.w ; neg.w
            dg = -dg;
            d0 = setw(d0, dg);                         // (d0 clobbé par glare — quirk préservé)
            Mem.ww(a0 + 8, dg);                        // move.w d0,8(a0)
            Mem.wb(a0 + 11, Mem.ub(a1 + 1 + idx));
            Mem.ww(a0 + 6, Mem.uw(a1 + 2 + idx));
            Mem.wb(anim_Brightness_w, Mem.ub(a1 + 5 + idx));
        } else {                                       // .additivegraph
            Mem.wb(a0 + 9, Mem.ub(a1 + idx));
            Mem.wb(a0 + 11, Mem.ub(a1 + 1 + idx));
            Mem.wb(a0 + 10, 6);
            Mem.ww(a0 + 6, Mem.uw(a1 + 2 + idx));
            Mem.wb(anim_Brightness_w, Mem.ub(a1 + 5 + idx));
        }
        // .donegraph
        d2 = setw(d2, d2 + 1);                         // addq #1,d2  (NB: d2 = anim*2 +1 ici)
        if ((short) d2 > (short) Mem.uw(a6 + BulT_AnimFrames_l + 2)) { // cmp.w BulT_AnimFrames_l+2(a6),d2 ; ble notdoneanim
            d2 = setw(d2, 0);                          // move.w #0,d2
        }
        // notdoneanim
        Mem.wb(a0 + ShotT_Anim_b, d2);                // move.b d2,ShotT_Anim_b(a0)
        int a2 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a2
        d0 = Mem.l(a2 + (d0 & 0xFFFF) * 4);            // move.l (a2,d0.w*4),d0  (d0 = zoneid, sauf glare)
        Mem.wl(Obj_ZonePtr_l, d0);                     // move.l d0,Obj_ZonePtr_l
        int a3 = Mem.l(Obj_ZonePtr_l);                 // move.l Obj_ZonePtr_l,a3
        Mem.wb(PlayEcho, Mem.ub(a3 + ZoneT_Echo_b));   // move.b ZoneT_Echo_b(a3),PlayEcho
        if (Mem.b(a0 + ShotT_InUpperZone_b) != 0) {    // tst.b ShotT_InUpperZone_b(a0) ; beq .notintop
            a3 = a3 + 8;                               // adda.w #8,a3
        }
        // .notintop — collision plafond
        d0 = Mem.l(a3 + 6);                            // move.l 6(a3),d0
        d0 = d0 - Mem.l(a0 + ShotT_AccYPos_w);         // sub.l ShotT_AccYPos_w(a0),d0
        if (d0 >= 10 * 128) {                          // cmp.l #10*128,d0 ; blt .nohitroof
            if ((Mem.ub(a0 + ShotT_Flags_w + 1) & 1) != 0) { // btst #0,ShotT_Flags_w+1(a0) ; beq .nobounce
                Mem.ww(a0 + ShotT_VelocityY_w, -(short) Mem.uw(a0 + ShotT_VelocityY_w)); // neg.w ShotT_VelocityY_w
                int dr = Mem.l(a3 + 6) + 10 * 128;     // move.l 6(a3),d0 ; add.l #10*128,d0
                Mem.wl(a0 + ShotT_AccYPos_w, dr);      // move.l d0,ShotT_AccYPos_w(a0)
                if (Mem.l(a6 + BulT_Gravity_l) != 0) { // tst.l BulT_Gravity_l(a6) ; beq .nohitroof
                    Mem.wl(a0 + ShotT_VelocityX_w, Mem.l(a0 + ShotT_VelocityX_w) >> 1); // asr.l #1
                    Mem.wl(a0 + ShotT_VelocityZ_w, Mem.l(a0 + ShotT_VelocityZ_w) >> 1); // asr.l #1
                }
            } else {
                // .nobounce — impact plafond
                Mem.wb(a0 + ShotT_Anim_b, 0);          // move.b #0,ShotT_Anim_b(a0)
                Mem.wb(a0 + ShotT_Status_b, 1);        // move.b #1,ShotT_Status_b(a0)
                bulletImpactFX(a0, a6, true);          // noise + explosion
            }
        }
        // .nohitroof — collision sol
        d0 = Mem.l(a3 + 2);                            // move.l 2(a3),d0
        d0 = d0 - Mem.l(a0 + ShotT_AccYPos_w);         // sub.l ShotT_AccYPos_w(a0),d0
        if (d0 <= 10 * 128) {                          // cmp.l #10*128,d0 ; bgt .nohitfloor
            boolean bouncedUp = false;
            if (Mem.l(a6 + BulT_BounceVert_l) != 0) {  // tst.l BulT_BounceVert_l(a6) ; beq .nobounceup
                if ((short) Mem.uw(a0 + ShotT_VelocityY_w) >= 0) { // tst.w ShotT_VelocityY_w(a0) ; blt .nohitfloor
                    int dv = setw(0, Mem.uw(a0 + ShotT_VelocityY_w)); // moveq #0,d0 ; move.w VelocityY,d0
                    dv = setw(dv, ((short) dv) >> 1);  // asr.w #1,d0
                    dv = setw(dv, -(short) dv);        // neg.w d0
                    Mem.ww(a0 + ShotT_VelocityY_w, dv); // move.w d0,ShotT_VelocityY_w(a0)
                    Mem.wl(a0 + ShotT_AccYPos_w, Mem.l(a3 + 2) - 10 * 128); // move.l 2(a3),d0 ; sub.l #10*128,d0 ; move.l d0,AccYPos
                    if (Mem.l(a6 + BulT_Gravity_l) != 0) { // tst.l BulT_Gravity_l(a6) ; beq .nohitfloor
                        Mem.wl(a0 + ShotT_VelocityX_w, Mem.l(a0 + ShotT_VelocityX_w) >> 1);
                        Mem.wl(a0 + ShotT_VelocityZ_w, Mem.l(a0 + ShotT_VelocityZ_w) >> 1);
                    }
                    bouncedUp = true;                  // (a sauté ou est sorti par blt .nohitfloor — géré ci-dessous)
                } else {
                    bouncedUp = true;                  // blt .nohitfloor (VelY<0 : pas d'impact)
                }
            }
            if (!bouncedUp) {
                // .nobounceup — impact sol
                Mem.wb(a0 + ShotT_Anim_b, 0);
                Mem.wb(a0 + ShotT_Status_b, 1);
                bulletImpactFX(a0, a6, true);
            }
        }
        // .nohitfloor — mouvement
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);             // move.l Lvl_ObjectPointsPtr_l,a1
        d1 = setw(0, Mem.uw(a0));                      // move.w (a0),d1
        a1 = a1 + (d1 & 0xFFFF) * 8;                   // lea (a1,d1.w*8),a1
        d2 = Mem.l(a1);                                // move.l (a1),d2
        Mem.wl(oldx, d2);                              // move.l d2,oldx
        int d3 = Mem.l(a0 + ShotT_VelocityX_w);        // move.l ShotT_VelocityX_w(a0),d3
        int d4 = setw(0, d3);                          // move.w d3,d4
        d3 = swap(d3);                                 // swap d3
        int d5 = setw(0, Mem.uw(Anim_TempFrames_w));   // move.w Anim_TempFrames_w,d5
        d3 = muls(d3, d5);                             // muls d5,d3
        d4 = M68k.mulu(d4, d5);                        // mulu d5,d4
        d3 = swap(d3);                                 // swap d3
        d3 = setw(d3, 0);                              // clr.w d3
        d3 = d3 + d4;                                  // add.l d4,d3
        d2 = d2 + d3;                                  // add.l d3,d2
        Mem.wl(newx, d2);                              // move.l d2,newx
        d2 = Mem.l(a1 + 4);                            // move.l 4(a1),d2
        Mem.wl(oldz, d2);                              // move.l d2,oldz
        d3 = Mem.l(a0 + ShotT_VelocityZ_w);            // move.l ShotT_VelocityZ_w(a0),d3
        d4 = setw(0, d3);                              // move.w d3,d4
        d3 = swap(d3);                                 // swap d3
        d3 = muls(d3, d5);                             // muls d5,d3
        d4 = M68k.mulu(d4, d5);                        // mulu d5,d4
        d3 = swap(d3);                                 // swap d3
        d3 = setw(d3, 0);                              // clr.w d3
        d3 = d3 + d4;                                  // add.l d4,d3
        d2 = d2 + d3;                                  // add.l d3,d2
        Mem.wl(newz, d2);                              // move.l d2,newz
        Mem.wl(oldy, Mem.l(a0 + ShotT_AccYPos_w));     // move.l ShotT_AccYPos_w(a0),oldy
        d3 = muls(Mem.uw(a0 + ShotT_VelocityY_w), Mem.uw(Anim_TempFrames_w)); // move.w VelocityY,d3 ; muls Anim_TempFrames_w,d3
        d5 = Mem.l(a6 + BulT_Gravity_l);               // move.l BulT_Gravity_l(a6),d5
        if (d5 != 0) {                                 // beq.s nograv
            d5 = muls(d5, Mem.uw(Anim_TempFrames_w));  // muls Anim_TempFrames_w,d5
            d3 = d3 + d5;                              // add.l d5,d3
            int d6 = setw(0, Mem.uw(a0 + ShotT_VelocityY_w)); // move.w VelocityY,d6
            d6 = (short) d6;                           // ext.l d6
            d6 = d6 + d5;                              // add.l d5,d6
            if (d6 >= 10 * 256) d6 = 10 * 256;         // cmp.l #10*256,d6 ; blt okgrav ; move.l #10*256,d6
            Mem.ww(a0 + ShotT_VelocityY_w, d6);        // move.w d6,ShotT_VelocityY_w(a0)
        }
        // nograv
        d4 = Mem.l(a0 + ShotT_AccYPos_w);              // move.l ShotT_AccYPos_w(a0),d4
        d4 = d4 + d3;                                  // add.l d3,d4
        Mem.wl(a0 + ShotT_AccYPos_w, d4);              // move.l d4,ShotT_AccYPos_w(a0)
        d4 = d4 - 5 * 128;                             // sub.l #5*128,d4
        Mem.wl(newy, d4);                              // move.l d4,newy
        d4 = d4 + 5 * 128;                             // add.l #5*128,d4
        d4 = d4 >> 7;                                  // asr.l #7,d4
        Mem.ww(a0 + 4, d4);                            // move.w d4,4(a0)
        Mem.wb(Obj_WallBounce_b, Mem.l(a6 + BulT_BounceHoriz_l) != 0 ? 0xFF : 0); // tst.l BulT_BounceHoriz_l(a6) ; sne Obj_WallBounce_b
        Mem.wb(exitfirst, Mem.l(a6 + BulT_BounceHoriz_l) == 0 ? 0xFF : 0); // seq exitfirst
        Mem.wb(MOVING, 0);                             // clr.b MOVING
        Mem.wb(hitwall, 0);                            // clr.b hitwall
        Mem.wb(StoodInTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a0),StoodInTop
        Mem.ww(wallflags, 0b0000010000000000);         // move.w #%0000010000000000,wallflags
        Mem.wl(StepUpVal, 0);                          // move.l #0,StepUpVal
        Mem.wl(StepDownVal, 0x1000000);                // move.l #$1000000,StepDownVal
        Mem.wl(thingheight, 10 * 128);                 // move.l #10*128,thingheight
        boolean noMove = false;
        if ((short) Mem.uw(oldx) == (short) Mem.uw(newx)) { // move.w oldx,d0 ; cmp.w newx,d0 ; bne lalal
            if ((short) Mem.uw(oldz) == (short) Mem.uw(newz)) { // move.w oldz,d0 ; cmp.w newz,d0 ; beq nomovebul
                noMove = true;
            }
        }
        if (!noMove) {
            if (!((short) Mem.uw(oldx) == (short) Mem.uw(newx))) {
                // (chemin lalal direct sans set WallLength) — l'ASM ne pose WallLength que si oldz==newz...
            }
            // (oldx!=newx OU oldz!=newz)
            if ((short) Mem.uw(oldx) == (short) Mem.uw(newx)) {
                Mem.ww(WallLength_w, 1);               // move.w #1,WallLength_w  (cas oldx==newx, oldz!=newz)
            }
            // lalal
            Mem.wb(MOVING, 0xFF);                      // st MOVING
            // movem save d0/d7/a0/a1/a2/a4/a5/a6 (pass by value)
            Objectmove.MoveObject();                   // jsr MoveObject
            int db = setb(0, Mem.ub(anim_Brightness_w)); // moveq #0,d0 ; move.b anim_Brightness_w,d0
            if ((byte) db != 0) {                      // beq.s .nobright
                db = setw(db, -(short) db);            // neg.w d0
                int dx = setw(0, Mem.uw(newx));        // move.w newx,d1
                int dz = setw(0, Mem.uw(newz));        // move.w newz,d2
                Mem.wl(Anim_BrightY_l, Mem.l(newy));   // move.l newy,Anim_BrightY_l
                int az = Mem.l(Obj_ZonePtr_l);         // move.l Obj_ZonePtr_l,a0
                int dzone = setw(0, Mem.uw(az));       // move.w (a0),d3
                anim_BrightenPoints(db, dx, dz, dzone); // jsr anim_BrightenPoints
            }
            // .nobright ; movem restore
        }
        // nomovebul
        Mem.wb(a0 + ShotT_InUpperZone_b, Mem.ub(StoodInTop)); // move.b StoodInTop,ShotT_InUpperZone_b(a0)
        boolean hit = false;
        if (Mem.b(Obj_WallBounce_b) != 0) {            // tst.b Obj_WallBounce_b ; beq .notabouncything
            if (Mem.b(hitwall) != 0) {                 // tst.b hitwall ; beq .nothitwall
                // rebond mur : réflexion de la vitesse
                int dv = muls(Mem.uw(a0 + ShotT_VelocityZ_w), Mem.uw(WallXSize_w)); // move.w VelZ,d0 ; muls WallXSize_w,d0
                int d1b = muls(Mem.uw(a0 + ShotT_VelocityX_w), Mem.uw(WallZSize_w)); // move.w VelX,d1 ; muls WallZSize_w,d1
                dv = dv - d1b;                         // sub.l d1,d0
                dv = divs(dv, Mem.uw(WallLength_w));   // divs WallLength_w,d0
                d1b = setw(0, Mem.uw(a0 + ShotT_VelocityX_w)); // move.w VelX,d1
                int d2b = setw(0, Mem.uw(WallZSize_w)); // move.w WallZSize_w,d2
                d2b = setw(d2b, d2b + d2b);            // add.w d2,d2
                d2b = muls(d2b, dv);                   // muls d0,d2
                d2b = divs(d2b, Mem.uw(WallLength_w)); // divs WallLength_w,d2
                d1b = setw(d1b, d1b + d2b);            // add.w d2,d1
                Mem.ww(a0 + ShotT_VelocityX_w, d1b);   // move.w d1,ShotT_VelocityX_w(a0)
                d1b = setw(0, Mem.uw(a0 + ShotT_VelocityZ_w)); // move.w VelZ,d1
                d2b = setw(0, Mem.uw(WallXSize_w));    // move.w WallXSize_w,d2
                d2b = setw(d2b, d2b + d2b);            // add.w d2,d2
                d2b = muls(d2b, dv);                   // muls d0,d2
                d2b = divs(d2b, Mem.uw(WallLength_w)); // divs WallLength_w,d2
                d1b = setw(d1b, d1b - d2b);            // sub.w d2,d1
                Mem.ww(a0 + ShotT_VelocityZ_w, d1b);   // move.w d1,ShotT_VelocityZ_w(a0)
                if (Mem.l(a6 + BulT_Gravity_l) != 0) { // tst.l BulT_Gravity_l(a6) ; beq .nothitwall
                    Mem.wl(a0 + ShotT_VelocityX_w, Mem.l(a0 + ShotT_VelocityX_w) >> 1);
                    Mem.wl(a0 + ShotT_VelocityZ_w, Mem.l(a0 + ShotT_VelocityZ_w) >> 1);
                }
            }
            // → .nothitwall (test timeout)
            if (Mem.b(timeout) != 0) hit = true;
        } else {
            // .notabouncything
            if (Mem.b(hitwall) != 0) {                 // tst.b hitwall ; beq .nothitwall
                int d4w = Mem.l(wallhitheight);        // move.l wallhitheight,d4
                Mem.wl(a0 + ShotT_AccYPos_w, d4w);     // move.l d4,ShotT_AccYPos_w(a0)
                d4w = d4w >> 7;                        // asr.l #7,d4
                Mem.ww(a0 + 4, d4w);                   // move.w d4,4(a0)
                hit = true;                            // → .hitsomething
            } else {
                if (Mem.b(timeout) != 0) hit = true;   // .nothitwall: tst.b timeout ; bne .hitsomething
            }
        }
        if (hit) {
            // .hitsomething
            Mem.wb(timeout, 0);                        // clr.b timeout
            Mem.wb(a0 + ShotT_Anim_b, 0);              // move.b #0,ShotT_Anim_b(a0)
            Mem.wb(a0 + ShotT_Status_b, 1);            // move.b #1,ShotT_Status_b(a0)
            bulletImpactFX(a0, a6, true);             // noise + explosion
        }

        // lab — pose la position, puis détection de hit ennemi
        a3 = Mem.l(Obj_ZonePtr_l);                     // move.l Obj_ZonePtr_l,a3
        Mem.ww(a0 + ObjT_ZoneID_w, Mem.uw(a3));        // move.w (a3),ObjT_ZoneID_w(a0)
        Mem.ww(a0 + EntT_ZoneID_w, Mem.uw(a3));        // move.w (a3),EntT_ZoneID_w(a0)
        Mem.wl(a1, Mem.l(newx));                       // move.l newx,(a1)
        Mem.wl(a1 + 4, Mem.l(newz));                   // move.l newz,4(a1)
        if (Mem.l(a0 + EntT_EnemyFlags_l) == 0) return; // tst.l EntT_EnemyFlags_l(a0) ; bne notasplut ; rts

        // notasplut — recherche d'ennemi touché
        a3 = Mem.l(Lvl_ObjectDataPtr_l);               // move.l Lvl_ObjectDataPtr_l,a3
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);             // move.l Lvl_ObjectPointsPtr_l,a1
        d2 = setw(0, Mem.uw(newx));                    // move.w newx,d2
        d2 = setw(d2, d2 - Mem.uw(oldx));              // sub.w oldx,d2
        Mem.ww(xdiff, d2);                             // move.w d2,xdiff
        d1 = setw(0, Mem.uw(newz));                    // move.w newz,d1
        d1 = setw(d1, d1 - Mem.uw(oldz));              // sub.w oldz,d1
        Mem.ww(zdiff, d1);                             // move.w d1,zdiff
        d3 = setw(0, d1);                              // move.w d1,d3
        d4 = setw(0, d2);                              // move.w d2,d4
        d2 = muls(d2, d2);                             // muls d2,d2
        d1 = muls(d1, d1);                             // muls d1,d1
        d0 = 1;                                        // move.l #1,d0
        d2 = d2 + d1;                                  // add.l d1,d2
        if (d2 != 0) {                                 // beq .oksqr
            d0 = setw(0, 31);                          // move.w #31,d0
            while ((d2 & (1 << (d0 & 31))) == 0) {     // .findhigh: btst d0,d2 ; bne .foundhigh ; dbra
                d0 = setw(d0, d0 - 1);
                if ((short) d0 == -1) break;
            }
            // .foundhigh
            d0 = setw(d0, ((short) d0) >> 1);          // asr.w #1,d0
            d3 = (1 << (d0 & 31));                     // clr.l d3 ; bset d0,d3
            d0 = d3;                                   // move.l d3,d0
            for (int it = 0; it < 3; it++) {           // 3× Newton
                d1 = setw(0, d0);                      // move.w d0,d1
                d1 = muls(d1, d1);                     // muls d1,d1
                d1 = d1 - d2;                          // sub.l d2,d1
                d1 = d1 >> 1;                          // asr.l #1,d1
                d1 = divs(d1, d0);                     // divs d0,d1
                d0 = setw(d0, d0 - d1);                // sub.w d1,d0
                if (!((short) d0 > 0)) d0 = setw(d0, 1); // bgt .stillnot ; move.w #1,d0
            }
        }
        // .oksqr
        Mem.ww(Range, d0);                             // move.w d0,Range
        d0 = setw(d0, d0 + 80);                        // add.w #80,d0
        d0 = muls(d0, d0);                             // muls d0,d0
        Mem.wl(sqrnum, d0);                            // move.l d0,sqrnum

        while (true) {                                 // .checkloop
            if ((short) Mem.uw(a3) < 0) break;         // tst.w (a3) ; blt .checkedall
            boolean nasty = true;
            if ((short) Mem.uw(a3 + ObjT_ZoneID_w) < 0) nasty = false; // tst.w ObjT_ZoneID_w(a3) ; blt .notanasty
            else {
                int d1c = Mem.ub(a0 + ShotT_InUpperZone_b); // move.b ShotT_InUpperZone_b(a0),d1
                int d2c = Mem.ub(a3 + ShotT_InUpperZone_b); // move.b ShotT_InUpperZone_b(a3),d2
                if (((d1c ^ d2c) & 0xFF) != 0) nasty = false; // eor.b d2,d1 ; bne .notanasty
                else {
                    int d1d = Mem.ub(a3 + 16);          // moveq #0,d1 ; move.b 16(a3),d1
                    int d7 = Mem.l(a0 + EntT_EnemyFlags_l); // move.l EntT_EnemyFlags_l(a0),d7
                    if ((d7 & (1 << (d1d & 31))) == 0) nasty = false; // btst d1,d7 ; beq .notanasty
                    else {
                        if (d1d != 1) {                 // cmp.b #1,d1 ; bne .notanobj
                            // (passe)
                        } else {
                            int a4 = Mem.l(GLF_DatabasePtr_l) + GLFT_ObjectDefs; // move.l GLF...a4 ; add.l #GLFT_ObjectDefs,a4
                            int d1e = Mem.ub(a3 + EntT_Type_b); // move.b EntT_Type_b(a3),d1
                            d1e = muls(d1e, ODefT_SizeOf_l); // muls #ODefT_SizeOf_l,d1
                            if (Mem.w(a4 + (d1e & 0xFFFF) + ODefT_Behaviour_w) != 2) nasty = false; // cmp.w #2,Behaviour ; bne .notanasty
                        }
                        // .notanobj
                        if (nasty && Mem.b(a3 + EntT_HitPoints_b) == 0) nasty = false; // tst.b EntT_HitPoints_b(a3) ; beq .notanasty
                        if (nasty) {
                            int d2h = setw(0, Mem.uw(a3 + 4));  // move.w 4(a3),d1
                            int d2v = setw(0, Mem.uw(a0 + 4));  // move.w 4(a0),d2
                            d2v = setw(d2v, d2v - d2h);         // sub.w d1,d2
                            if ((short) d2v < 0) d2v = setw(d2v, -(short) d2v); // bge .okh ; neg.w d2
                            // .okh
                            boolean ignoreHeight = (Mem.b(MOVING) == 0); // tst.b MOVING ; beq .ignoreheight
                            if (!ignoreHeight && (short) d2v > 50) nasty = false; // cmp.w #50,d2 ; bgt .notanasty
                            if (nasty) {
                                // .ignoreheight — test de proximité à la trajectoire
                                int d1p = setw(0, Mem.uw(a3));  // move.w (a3),d1
                                int dpx = setw(0, Mem.uw(a1 + (d1p & 0xFFFF) * 8)); // move.w (a1,d1.w*8),d2
                                d4 = setw(0, dpx);              // move.w d2,d4
                                int dpz = setw(0, Mem.uw(a1 + (d1p & 0xFFFF) * 8 + 4)); // move.w 4(a1,d1.w*8),d3
                                d5 = setw(0, dpz);             // move.w d3,d5
                                d4 = setw(d4, d4 - Mem.uw(newx)); // sub.w newx,d4
                                dpx = setw(dpx, dpx - Mem.uw(oldx)); // sub.w oldx,d2
                                int d6 = setw(0, dpx);         // move.w d2,d6
                                d5 = setw(d5, d5 - Mem.uw(newz)); // sub.w newz,d5
                                dpz = setw(dpz, dpz - Mem.uw(oldz)); // sub.w oldz,d3
                                d7 = setw(0, dpz);             // move.w d3,d7
                                d6 = muls(d6, Mem.uw(zdiff));  // muls zdiff,d6
                                d7 = muls(d7, Mem.uw(xdiff));  // muls xdiff,d7
                                d6 = d6 - d7;                  // sub.l d7,d6
                                if (d6 <= 0) d6 = -d6;         // bgt .pos ; neg.l d6
                                // .pos
                                d6 = divs(d6, Mem.uw(Range));  // divs Range,d6
                                int d7t = setw(0, 80);         // move.w #80,d7
                                if ((byte) Mem.ub(a3 + 16) > 1) d7t = setw(d7t, 40); // cmp.b #1,16(a3) ; ble .okbig ; move.w #40,d7
                                // .okbig
                                if ((short) d6 > (short) d7t) nasty = false; // cmp.w d7,d6 ; bgt .stillgoing
                                else {
                                    dpx = muls(dpx, dpx);      // muls d2,d2
                                    dpz = muls(dpz, dpz);      // muls d3,d3
                                    dpx = dpx + dpz;           // add.l d3,d2
                                    if (dpx > Mem.l(sqrnum)) nasty = false; // cmp.l sqrnum,d2 ; bgt .stillgoing
                                    else {
                                        d4 = muls(d4, d4);     // muls d4,d4
                                        d5 = muls(d5, d5);     // muls d5,d5
                                        d4 = d4 + d5;          // add.l d5,d4
                                        if (d4 > Mem.l(sqrnum)) nasty = false; // cmp.l sqrnum,d4 ; bgt .stillgoing
                                        else {
                                            // HIT ennemi
                                            int d6d = Mem.ub(a0 + ShotT_Power_w); // move.b ShotT_Power_w(a0),d6
                                            Mem.wb(a3 + EntT_DamageTaken_b, Mem.ub(a3 + EntT_DamageTaken_b) + d6d); // add.b
                                            Mem.ww(a3 + EntT_ImpactX_w, Mem.uw(a0 + ShotT_VelocityX_w)); // move.w VelX,ImpactX
                                            Mem.ww(a3 + EntT_ImpactZ_w, Mem.uw(a0 + ShotT_VelocityZ_w)); // move.w VelZ,ImpactZ
                                            Mem.wb(a0 + ShotT_Anim_b, 0); // move.b #0,ShotT_Anim_b(a0)
                                            Mem.wb(a0 + ShotT_Status_b, 1); // move.b #1,ShotT_Status_b(a0)
                                            bulletImpactFX(a0, a6, false); // noise + explosion (sans ViewerTop)
                                            return;            // bra .hitnasty (→ doneshot rts)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // .stillgoing / .notanasty
            a3 = a3 + 64;                              // add.w #64,a3
        }
        // .checkedall / doneshot: rts
    }

    /** Bruit d'impact + explosion (newanims.s, blocs .nohitnoise/.noexplosion). */
    private static void bulletImpactFX(int a0, int a6, boolean setViewerTop) {
        int d0 = Mem.l(a6 + BulT_ImpactSFX_l);         // move.l BulT_ImpactSFX_l(a6),d0
        d0 = d0 - 1;                                   // subq.l #1,d0
        if (d0 >= 0) {                                 // blt .nohitnoise
            int a1 = ObjRotated_vl;                    // move.l #ObjRotated_vl,a1
            int d1 = setw(0, Mem.uw(a0));              // move.w (a0),d1
            Mem.wl(ab3d2.HiresData.Aud_NoiseX_w, Mem.l(a1 + (d1 & 0xFFFF) * 8)); // move.l (a1,d1.w*8),Aud_NoiseX_w
            Mem.ww(ab3d2.HiresData.Aud_NoiseVol_w, 200); // move.w #200,Aud_NoiseVol_w
            Mem.ww(Aud_SampleNum_w, d0);               // move.w d0,Aud_SampleNum_w
            Mem.ww(IDNUM, d1);                         // move.w d1,IDNUM
            Hires.MakeSomeNoise();                     // SAVEREGS ; jsr MakeSomeNoise ; GETREGS
        }
        // .nohitnoise
        int force = Mem.l(a6 + BulT_ExplosiveForce_l); // move.l BulT_ExplosiveForce_l(a6),d0
        if (force != 0) {                              // beq .noexplosion
            Mem.ww(Viewerx, Mem.uw(newx));             // move.w newx,Viewerx
            Mem.ww(Viewerz, Mem.uw(newz));             // move.w newz,Viewerz
            Mem.ww(Viewery, Mem.uw(a0 + 4));           // move.w 4(a0),Viewery
            if (setViewerTop) Mem.wb(ViewerTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b InUpperZone,ViewerTop
            ComputeBlast(force, a0);                   // SAVEREGS ; bsr ComputeBlast ; GETREGS
        }
        // .noexplosion
    }

    // ==================================================================
    //  Draw_SkyBackdrop (newanims.s:2708) — préserve a0 (renvoyé inchangé).
    // ==================================================================
    public static int Draw_SkyBackdrop(int a0) {
        int d0, d1, d2, d3, d4, d5, d6, d7;
        int a1, a2, a3, a4, a5;
        final int entryA0 = a0;                        // a0 préservé (push/pop autour de la routine)

        a5 = Mem.l(Lvl_ListOfGraphRoomsPtr_l);         // move.l Lvl_ListOfGraphRoomsPtr_l,a5
        d5 = setw(0, Mem.uw(a5 + PVST_Zone_w));        // move.w PVST_Zone_w(a5),d5
        a5 = Zone_BackdropDisable_vb;                  // lea Zone_BackdropDisable_vb,a5
        d3 = setw(0, d5);                              // move.w d5,d3
        d5 = setw(d5, (d5 & 0xFFFF) >>> 3);            // lsr.w #3,d5
        a5 = a5 + (short) d5;                          // add.w d5,a5
        if ((Mem.ub(a5) & (1 << (d3 & 7))) != 0) return a0; // btst.b d3,(a5) ; bne sky_early_exit

        // move.l a0,-(a7)  (a0 préservé : renvoyé à la fin)
        d5 = setw(0, Mem.uw(tmpangpos));               // move.w tmpangpos,d5
        d5 = setw(d5, d5 & 4095);                      // and.w #4095,d5
        d5 = muls(d5, SKY_BACKDROP_W);                 // muls #SKY_BACKDROP_W,d5
        d5 = d5 >> 8;                                  // asr.l #8,d5
        d5 = d5 >> 4;                                  // asr.l #4,d5
        d5 = muls(d5, SKY_BACKDROP_H);                 // muls #SKY_BACKDROP_H,d5

        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; bne draw_SkyBackDropFullscreen
            // ---- draw_SkyBackDropFullscreen (2797) ----
            a0 = Mem.l(Vid_FastBufferPtr_l);           // move.l Vid_FastBufferPtr_l,a0  (note : a0 réutilisé localement)
            a5 = Mem.l(Draw_BackdropImagePtr_l);       // move.l Draw_BackdropImagePtr_l,a5
            a3 = a5;                                   // move.l a5,a3
            a3 = a3 + (SKY_BACKDROP_W * SKY_BACKDROP_H); // add.l #(W*H),a3
            a5 = a5 + SKY_BACKDROP_H;                  // add.l #SKY_BACKDROP_H,a5
            a1 = Mem.l(Draw_BackdropImagePtr_l);       // move.l Draw_BackdropImagePtr_l,a1
            a1 = a1 + d5;                              // add.l d5,a1
            a1 = a1 + SKY_BACKDROP_H;                  // add.w #SKY_BACKDROP_H,a1
            d7 = setw(0, Mem.uw(Vid_CentreY_w));       // move.w Vid_CentreY_w,d7
            d6 = setw(0, d7);                          // move.w d7,d6
            a1 = a1 - (short) d6;                      // sub.w d6,a1
            a5 = a5 - (short) d6;                      // sub.w d6,a5
            d7 = setw(d7, ((short) d7) >> 2);          // asr.w #2,d7
            d4 = setw(0, Hires.FS_WIDTH - 1);          // move.w #FS_WIDTH-1,d4
            while (true) {                             // .horline
                d3 = setw(0, d7);                      // move.w d7,d3
                a2 = a0;                               // move.l a0,a2
                a4 = a1;                               // move.l a1,a4
                while (true) {                         // .vertline
                    d0 = Mem.l(a4);
                    a4 += 4;                           // move.l (a4)+,d0
                    Mem.wb(a2 + SCREEN_WIDTH * 3, d0); // move.b d0,SCREEN_WIDTH*3(a2)
                    d0 = swap(d0);                     // swap d0
                    Mem.wb(a2 + SCREEN_WIDTH, d0);     // move.b d0,SCREEN_WIDTH(a2)
                    d0 = d0 >>> 8;                     // lsr.l #8,d0
                    Mem.wb(a2, d0);                    // move.b d0,(a2)
                    d0 = swap(d0);                     // swap d0
                    Mem.wb(a2 + SCREEN_WIDTH * 2, d0); // move.b d0,SCREEN_WIDTH*2(a2)
                    a2 = a2 + SCREEN_WIDTH * 4;        // adda.w #SCREEN_WIDTH*4,a2
                    d3 = setw(d3, d3 - 1);             // dbra d3,.vertline
                    if ((short) d3 == -1) break;
                }
                a1 = a1 + SKY_BACKDROP_H;              // add.w #SKY_BACKDROP_H,a1
                if (!(a3 > a1)) a1 = a5;               // cmp.l a1,a3 ; bgt.s .noend ; move.l a5,a1
                a0 += 1;                               // addq.w #1,a0
                d4 = setw(d4, d4 - 1);                 // dbra d4,.horline
                if ((short) d4 == -1) break;
            }
            return entryA0;                            // move.l (a7)+,a0 ; rts (a0 d'origine)
        }

        // ---- small display (2740) ----
        a0 = Mem.l(Vid_FastBufferPtr_l);               // move.l Vid_FastBufferPtr_l,a0
        a5 = Mem.l(Draw_BackdropImagePtr_l);           // move.l Draw_BackdropImagePtr_l,a5
        a3 = a5;                                       // move.l a5,a3
        a3 = a3 + (SKY_BACKDROP_W * SKY_BACKDROP_H);   // add.l #(W*H),a3
        a5 = a5 + SKY_BACKDROP_H;                      // add.l #SKY_BACKDROP_H,a5
        a1 = Mem.l(Draw_BackdropImagePtr_l);           // move.l Draw_BackdropImagePtr_l,a1
        a1 = a1 + d5;                                  // add.l d5,a1
        a1 = a1 + SKY_BACKDROP_H;                      // add.w #SKY_BACKDROP_H,a1
        d7 = setw(0, Mem.uw(Vid_CentreY_w));           // move.w Vid_CentreY_w,d7
        d6 = setw(0, d7);                              // move.w d7,d6
        d5 = setw(0, d6);                              // move.w d6,d5
        d5 = setw(d5, ((short) d5) >> 1);              // asr.w #1,d5
        d6 = setw(d6, d6 + d5);                        // add.w d5,d6
        a1 = a1 - (short) d6;                          // sub.w d6,a1
        a5 = a5 - (short) d6;                          // sub.w d6,a5
        d7 = setw(d7, ((short) d7) >> 2);              // asr.w #2,d7
        d1 = setw(0, SKY_BACKDROP_H);                  // move.w #SKY_BACKDROP_H,d1
        d2 = setw(0, SKY_BACKDROP_H);                  // move.w #SKY_BACKDROP_H,d2
        d5 = setw(0, SKY_BACKDROP_H * 2);              // move.w #(SKY_BACKDROP_H*2),d5
        d4 = setw(0, 191);                             // move.w #191,d4
        while (true) {                                 // .horline
            d3 = setw(0, d7);                          // move.w d7,d3
            a2 = a0;                                   // move.l a0,a2
            a4 = a1;                                   // move.l a1,a4
            while (true) {                             // .vertline
                d0 = setw(0, Mem.uw(a4));
                a4 += 2;                               // move.w (a4)+,d0
                Mem.wb(a2, d0);                        // move.b d0,(a2)
                d0 = setb(d0, Mem.ub(a4));
                a4 += 1;                               // move.b (a4)+,d0
                Mem.wb(a2 + SCREEN_WIDTH, d0);         // move.b d0,SCREEN_WIDTH(a2)
                a4 += 1;                               // addq #1,a4
                d0 = setb(d0, Mem.ub(a4));
                a4 += 1;                               // move.b (a4)+,d0
                Mem.wb(a2 + SCREEN_WIDTH * 2, d0);     // move.b d0,SCREEN_WIDTH*2(a2)
                d0 = setb(d0, Mem.ub(a4));
                a4 += 1;                               // move.b (a4)+,d0
                Mem.wb(a2 + SCREEN_WIDTH * 3, d0);     // move.b d0,SCREEN_WIDTH*3(a2)
                a2 = a2 + SCREEN_WIDTH * 4;            // adda.w #SCREEN_WIDTH*4,a2
                d3 = setw(d3, d3 - 1);                 // dbra d3,.vertline
                if ((short) d3 == -1) break;
            }
            a1 = a1 + (short) d1;                      // add.w d1,a1
            if (!(a3 > a1)) a1 = a5;                   // cmp.l a1,a3 ; bgt.s .noend ; move.l a5,a1
            // .noend — exg d1,d2 ; exg d2,d5  (rotation d1→d2→d5→d1)
            int t = d1; d1 = setw(d1, d2); d2 = setw(d2, t); // exg d1,d2
            t = d2; d2 = setw(d2, d5); d5 = setw(d5, t);     // exg d2,d5
            a0 += 1;                                    // addq.w #1,a0
            d4 = setw(d4, d4 - 1);                      // dbra d4,.horline
            if ((short) d4 == -1) break;
        }
        return entryA0;                                // move.l (a7)+,a0 ; rts
    }
}
