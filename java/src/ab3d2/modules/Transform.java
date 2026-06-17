package ab3d2.modules;

import ab3d2.HiresData;
import ab3d2.HireswallData;
import ab3d2.Mem;

import static ab3d2.Defs.ObjT_SizeOf_l;
import static ab3d2.Defs.ObjT_TypeID_b;
import static ab3d2.Defs.ObjT_ZoneID_w;
import static ab3d2.Defs.OBJ_TYPE_AUX;
import static ab3d2.M68k.divs;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;
import static ab3d2.bss.LevelBss.Lvl_NumObjectPoints_w;
import static ab3d2.bss.LevelBss.Lvl_NumPoints_w;
import static ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_PointsPtr_l;
import static ab3d2.bss.LevelBss.PointsToRotatePtr_l;
import static ab3d2.bss.PlayerBss.Plr1_CosVal_w;
import static ab3d2.bss.PlayerBss.Plr1_ObjectDistances_vw;
import static ab3d2.bss.PlayerBss.Plr1_ObsInLine_vb;
import static ab3d2.bss.PlayerBss.Plr1_SinVal_w;
import static ab3d2.bss.PlayerBss.Plr1_XOff_l;
import static ab3d2.bss.PlayerBss.Plr1_ZOff_l;
import static ab3d2.bss.PlayerBss.Plr2_CosVal_w;
import static ab3d2.bss.PlayerBss.Plr2_ObjectDistances_vw;
import static ab3d2.bss.PlayerBss.Plr2_ObsInLine_vb;
import static ab3d2.bss.PlayerBss.Plr2_SinVal_w;
import static ab3d2.bss.PlayerBss.Plr2_XOff_l;
import static ab3d2.bss.PlayerBss.Plr2_ZOff_l;
import static ab3d2.bss.TablesBss.ObjRotated_vl;
import static ab3d2.bss.TablesBss.OnScreen_vl;
import static ab3d2.bss.TablesBss.Rotated_vl;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.ZoneBss.EDGE_POINT_ID_LIST_END;
import static ab3d2.bss.ZoneBss.Zone_EdgePointIndexes_vw;

/**
 * Traduction littérale de ab3d2_source/modules/transform.s
 *
 * "Routines relating to coordinate transformation."
 *
 * Conventions : dN/aN sont des int locaux ; les accès mémoire passent par Mem
 * (big-endian) ; muls/divs/swap par M68k. Chaque instruction d'origine est
 * reportée, dans l'ordre, avec ses commentaires.
 */
public final class Transform {

    private Transform() {
    }

    /** RotateLevelPts: Does this rotate ALL points in the level EVERY frame? */
    public static void RotateLevelPts() {
        // tst.b draw_RenderMap_b ; beq xform_pvs_subset
        if (Mem.b(HiresData.draw_RenderMap_b) == 0) {
            // When REALMAP is on, we apparently need to transform all level points,
            // otherwise only the visible subset
            xform_pvs_subset();
            return;
        }

        // Rotate all level points
        int d6 = Mem.uw(HireswallData.Vis_SinVal_w);  // move.w Vis_SinVal_w,d6
        d6 = swap(d6);                                 // swap d6
        d6 = setw(d6, Mem.uw(HireswallData.Vis_CosVal_w)); // move.w Vis_CosVal_w,d6

        int a3 = Mem.l(Lvl_PointsPtr_l);               // move.l Lvl_PointsPtr_l,a3
        int a1 = Rotated_vl;                           // move.l #Rotated_vl,a1 ; stores only 2x800 points
        int a2 = OnScreen_vl;                          // move.l #OnScreen_vl,a2
        int d4 = Mem.w(HiresData.Plr_XOff_l);          // move.w Plr_XOff_l,d4
        int d5 = Mem.w(HiresData.Plr_ZOff_l);          // move.w Plr_ZOff_l,d5

        int d7 = Mem.w(Lvl_NumPoints_w);               // move.w Lvl_NumPoints_w,d7
        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; bne xform_all_fs
            xform_all_fs(d4, d5, d6, d7, a1, a2, a3);
            return;
        }

        // rotate all level points, small screen
        int d0, d1, d2, d3;
        do { // pointrotlop2:
            d0 = Mem.w(a3); a3 += 2;                   // move.w (a3)+,d0
            d0 = setw(d0, d0 - d4);                    // sub.w d4,d0
            d2 = d0;                                   // move.w d0,d2 ; view X (mot faible significatif)

            d1 = Mem.w(a3); a3 += 2;                   // move.w (a3)+,d1
            d1 = setw(d1, d1 - d5);                    // sub.w d5,d1 ; view Z

            d2 = muls(d2, d6);                         // muls d6,d2 ; x' = (cos*viewX)<<16
            d6 = swap(d6);                             // swap d6
            d3 = d1;                                   // move.w d1,d3
            d3 = muls(d3, d6);                         // muls d6,d3 ; z' = (sin*viewZ) << 16

            d2 -= d3;                                  // sub.l d3,d2 ; x' = (cos*viewX - sin*viewZ) << 16

            d2 >>= 8;                                  // asr.l #8,d2 ; x' = int(2*x') << 8

            d2 += Mem.l(HiresData.xwobble);            // add.l xwobble,d2
            Mem.wl(a1, d2); a1 += 4;                   // move.l d2,(a1)+ ; store rotated x'

            d0 = muls(d0, d6);                         // muls d6,d0
            d6 = swap(d6);                             // swap d6
            d1 = muls(d1, d6);                         // muls d6,d1
            d1 += d0;                                  // add.l d0,d1

            d1 >>= 8;                                  // asr.l #8,d1
            d1 >>= 7;                                  // asr.l #7,d1 ; z' = int(z') * 2

            Mem.wl(a1, d1); a1 += 4;                   // move.l d1,(a1)+ ; store rotated z'

            if (d1 > 0) {                              // tst.l d1 ; bgt.s ptnotbehind
                // ptnotbehind:
                d2 = divs(d2, d1);                     // divs.w d1,d2 ; x / z perspective projection
                d2 = setw(d2, d2 + Mem.w(HiresData.Vid_CentreX_w)); // add.w Vid_CentreX_w,d2
            } else if (d2 > 0) {                       // tst.l d2 ; bgt.s onrightsomewhere
                // onrightsomewhere:
                d2 = setw(d2, Mem.uw(HiresData.Vid_RightX_w)); // move.w Vid_RightX_w,d2
            } else {
                d2 = setw(d2, 0);                      // move.w #0,d2
            }
            // store_point:
            Mem.ww(a2, d2); a2 += 2;                   // move.w d2,(a2)+ ; store to OnScreen_vl
        } while (--d7 != -1);                          // dbra d7,pointrotlop2
        // outofpointrot: rts
    }

    /** xform_all_fs / pointrotlop2B — rotation de tous les points, plein écran. */
    private static void xform_all_fs(int d4, int d5, int d6, int d7, int a1, int a2, int a3) {
        int d0, d1, d2, d3;
        do { // pointrotlop2B:
            d0 = Mem.w(a3); a3 += 2;                   // move.w (a3)+,d0 ; x
            d0 = setw(d0, d0 - d4);                    // sub.w d4,d0 ; x/2 - Plr_XOff_l
            d2 = d0;                                   // move.w d0,d2

            d1 = Mem.w(a3); a3 += 2;                   // move.w (a3)+,d1 ; z
            d1 = setw(d1, d1 - d5);                    // sub.w d5,d1

            d2 = muls(d2, d6);                         // muls.w d6,d2 ; x*cos<<16
            d6 = swap(d6);                             // swap d6
            d3 = d1;                                   // move.w d1,d3
            d3 = muls(d3, d6);                         // muls.w d6,d3 ; z*sin<<16
            d2 -= d3;                                  // sub.l d3,d2 ; x' = (x*cos - z*sin)<<16

            d2 >>= 8;                                  // asr.l #8,d2

            d2 += Mem.l(HiresData.xwobble);            // add.l xwobble,d2 ; could the wobble be a shake or some underwater effect?
            Mem.wl(a1, d2); a1 += 4;                   // move.l d2,(a1)+ ; store x'<<16

            d0 = muls(d0, d6);                         // muls d6,d0 ; x * sin<<16
            d6 = swap(d6);                             // swap d6
            d1 = muls(d1, d6);                         // muls d6,d1 ; z * cos<<16
            d1 += d0;                                  // add.l d0,d1 ; z' = (x*sin + z*cos)<<16

            // 0xABADCAFE
            // Use 3/5 rather than 2/3 here for 320 wide - z' * 2 * 3/5 -> z' * 6/5
            // Shift d1 to get 2 extra input bits for our scale by 3/5 approximation
            d1 <<= 2;                                  // lsl.l #2,d1
            d1 = swap(d1);                             // swap d1
            d1 = muls(d1, 1229);                       // muls #1229,d1 ; 1229/2048 = 0.600097
            d1 >>= 8;                                  // asr.l #8,d1
            d1 >>= 4;                                  // asr.l #4,d1 ; z' * 6/5

            Mem.wl(a1, d1); a1 += 4;                   // move.l d1,(a1)+ ; stores the rotated points (aspect appliqué)

            if (d1 > 0) {                              // tst.l d1 ; bgt.s ptnotbehindB
                // ptnotbehindB:
                d2 = divs(d2, d1);                     // divs.w d1,d2
                d2 = setw(d2, d2 + Mem.w(HiresData.Vid_CentreX_w)); // add.w Vid_CentreX_w,d2
            } else if (d2 > 0) {                       // tst.l d2 ; bgt.s onrightsomewhereB
                // onrightsomewhereB:
                d2 = setw(d2, Mem.uw(HiresData.Vid_RightX_w)); // move.w Vid_RightX_w,d2
            } else {
                d2 = 0;                                // moveq.l #0,d2
            }
            // putinB:
            Mem.ww(a2, d2); a2 += 2;                   // move.w d2,(a2)+ ; store fully projected X
        } while (--d7 != -1);                          // dbra d7,pointrotlop2B
        // rts
    }

    /**
     * xform_pvs_subset (;ONLYTHELONELY:)
     * This only rotates a subset of the points, with indices pointed to at PointsToRotatePtr_l
     */
    private static void xform_pvs_subset() {
        int d6 = Mem.uw(HireswallData.Vis_SinVal_w);   // move.w Vis_SinVal_w,d6
        d6 = swap(d6);                                 // swap d6
        d6 = setw(d6, Mem.uw(HireswallData.Vis_CosVal_w)); // move.w Vis_CosVal_w,d6

        int a0 = Mem.l(PointsToRotatePtr_l);           // move.l PointsToRotatePtr_l,a0 ; -1 terminated array of point indices
        int a3 = Mem.l(Lvl_PointsPtr_l);               // move.l Lvl_PointsPtr_l,a3
        int a1 = Rotated_vl;                           // move.l #Rotated_vl,a1
        int a2 = OnScreen_vl;                          // move.l #OnScreen_vl,a2
        int d4 = Mem.w(HiresData.Plr_XOff_l);          // move.w Plr_XOff_l,d4
        int d5 = Mem.w(HiresData.Plr_ZOff_l);          // move.w Plr_ZOff_l,d5

        boolean fullScreen = Mem.b(Vid_FullScreen_b) != 0; // tst.b Vid_FullScreen_b ; bne xform_pvs_subset_fs

        int d0, d1, d2, d3, d7;
        while (true) { // .point_rotate_loop:
            d7 = Mem.w(a0); a0 += 2;                   // move.w (a0)+,d7
            if (d7 < 0) {                              // blt .check_vis_edge_points
                // Make sure we rotate the points of the shared edges, no matter what.
                // .check_vis_edge_points:
                if (d7 == EDGE_POINT_ID_LIST_END) {    // cmp.w #EDGE_POINT_ID_LIST_END,d7 ; beq.s .done
                    return;                            // .done: rts
                }
                // One more pass
                a0 = Zone_EdgePointIndexes_vw;         // move.l #Zone_EdgePointIndexes_vw,a0
                continue;                              // bra .point_rotate_loop
            }

            d0 = Mem.w(a3 + d7 * 4);                   // move.w (a3,d7*4),d0
            d0 = setw(d0, d0 - d4);                    // sub.w d4,d0
            d2 = d0;                                   // move.w d0,d2
            d1 = Mem.w(a3 + d7 * 4 + 2);               // move.w 2(a3,d7*4),d1
            d1 = setw(d1, d1 - d5);                    // sub.w d5,d1
            d2 = muls(d2, d6);                         // muls d6,d2
            d6 = swap(d6);                             // swap d6
            d3 = d1;                                   // move.w d1,d3
            d3 = muls(d3, d6);                         // muls d6,d3
            d2 -= d3;                                  // sub.l d3,d2

            d2 >>= 8;                                  // asr.l #8,d2 ; x' = int(2*x') << 8
            d2 += Mem.l(HiresData.xwobble);            // add.l xwobble,d2
            Mem.wl(a1 + d7 * 8, d2);                   // move.l d2,(a1,d7*8)
            d0 = muls(d0, d6);                         // muls d6,d0
            d6 = swap(d6);                             // swap d6
            d1 = muls(d1, d6);                         // muls d6,d1
            d1 += d0;                                  // add.l d0,d1

            if (!fullScreen) {
                d1 >>= 8;                              // asr.l #8,d1
                d1 >>= 7;                              // asr.l #7,d1 ; z' = int(z') * 2
            } else {
                // 0xABADCAFE
                // Use 3/5 rather than 2/3 here for 320 wide - z' * 2 * 3/5 -> z' * 6/5
                d1 <<= 2;                              // lsl.l #2,d1
                d1 = swap(d1);                         // swap d1
                d1 = muls(d1, 1229);                   // muls #1229,d1 ; 1229/2048 = 0.600097
                d1 >>= 8;                              // asr.l #8,d1
                d1 >>= 4;                              // asr.l #4,d1 ; z' * 6/5
            }
            Mem.wl(a1 + d7 * 8 + 4, d1);               // move.l d1,4(a1,d7*8)

            if (d1 > 0) {                              // tst.l d1 ; bgt.s .ptnotbehind
                // .ptnotbehind:
                d2 = divs(d2, d1);                     // divs d1,d2
                d2 = setw(d2, d2 + Mem.w(HiresData.Vid_CentreX_w)); // add.w Vid_CentreX_w,d2
            } else if (d2 > 0) {                       // tst.l d2 ; bgt.s .onrightsomewhere
                // .onrightsomewhere:
                d2 = setw(d2, Mem.uw(HiresData.Vid_RightX_w)); // move.w Vid_RightX_w,d2
            } else {
                d2 = setw(d2, 0);                      // move.w #0,d2
            }
            // .store_point:
            Mem.ww(a2 + d7 * 2, d2);                   // move.w d2,(a2,d7*2) ; écrit de façon éparse
            // bra .point_rotate_loop
        }
    }

    /** CalcPLR1InLine */
    public static void CalcPLR1InLine() {
        int d5 = Mem.uw(Plr1_SinVal_w);                // move.w Plr1_SinVal_w,d5
        int d6 = Mem.uw(Plr1_CosVal_w);                // move.w Plr1_CosVal_w,d6
        int a4 = Mem.l(Lvl_ObjectDataPtr_l);           // move.l Lvl_ObjectDataPtr_l,a4
        int a0 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a0
        int d7 = Mem.w(Lvl_NumObjectPoints_w);         // move.w Lvl_NumObjectPoints_w,d7
        int a2 = Plr1_ObsInLine_vb;                    // move.l #Plr1_ObsInLine_vb,a2
        int a3 = Plr1_ObjectDistances_vw;              // move.l #Plr1_ObjectDistances_vw,a3

        int d0, d1, d2, d3;
        while (true) { // .objpointrotlop:
            if (Mem.ub(a4 + ObjT_TypeID_b) == OBJ_TYPE_AUX) { // cmp.b #OBJ_TYPE_AUX,ObjT_TypeID_b(a4) ; beq.s .itaux
                a4 += ObjT_SizeOf_l;                   // .itaux: NEXT_OBJ a4
                continue;                              // bra .objpointrotlop
            }

            d0 = Mem.w(a0);                            // move.w (a0),d0
            d0 = setw(d0, d0 - Mem.w(Plr1_XOff_l));    // sub.w Plr1_XOff_l,d0
            d1 = Mem.w(a0 + 4);                        // move.w 4(a0),d1
            a0 += 8;                                   // addq #8,a0

            if (Mem.w(a4 + ObjT_ZoneID_w) < 0) {       // tst.w ObjT_ZoneID_w(a4) ; blt .noworkout
                // .noworkout:
                Mem.wb(a2, 0); a2 += 1;                // move.b #0,(a2)+
                Mem.ww(a3, 0); a3 += 2;                // move.w #0,(a3)+
                a4 += ObjT_SizeOf_l;                   // NEXT_OBJ a4
                if (--d7 == -1) {                      // dbra d7,.objpointrotlop
                    return;                            // rts
                }
                continue;
            }

            d2 = Mem.ub(a4 + ObjT_TypeID_b);           // moveq #0,d2 ; move.b ObjT_TypeID_b(a4),d2 (écrasé juste après)

            d1 = setw(d1, d1 - Mem.w(Plr1_ZOff_l));    // sub.w Plr1_ZOff_l,d1
            d2 = setw(d2, d0);                         // move.w d0,d2
            d2 = muls(d2, d6);                         // muls d6,d2
            d3 = d1;                                   // move.w d1,d3
            d3 = muls(d3, d5);                         // muls d5,d3
            d2 -= d3;                                  // sub.l d3,d2
            d2 += d2;                                  // add.l d2,d2

            if (d2 <= 0) {                             // bgt.s .okh
                d2 = -d2;                              // neg.l d2
            }
            // .okh:
            d2 = swap(d2);                             // swap d2

            d0 = muls(d0, d5);                         // muls d5,d0
            d1 = muls(d1, d6);                         // muls d6,d1
            d1 += d0;                                  // add.l d0,d1
            d1 <<= 2;                                  // asl.l #2,d1
            d1 = swap(d1);                             // swap d1
            d3 = 0;                                    // moveq #0,d3

            if ((short) d1 > 0) {                      // tst.w d1 ; ble.s .notinline
                d2 = setw(d2, ((short) d2) >> 1);      // asr.w #1,d2
                if ((short) d2 <= 80) {                // cmp.w #80,d2 ; bgt.s .notinline
                    d3 = (d3 & ~0xFF) | 0xFF;          // st d3 (octet faible à $FF)
                }
            }
            // .notinline:
            Mem.wb(a2, d3); a2 += 1;                   // move.b d3,(a2)+
            Mem.ww(a3, d1); a3 += 2;                   // move.w d1,(a3)+
            a4 += ObjT_SizeOf_l;                       // NEXT_OBJ a4
            if (--d7 == -1) {                          // dbra d7,.objpointrotlop
                return;                                // rts
            }
        }
    }

    /**
     * CalcPLR2InLine
     * NB: l'original compare la distance latérale à (a6) — un registre hérité de
     * l'appelant — là où CalcPLR1InLine compare à #80. a6 est donc un paramètre.
     */
    public static void CalcPLR2InLine(int a6) {
        int d5 = Mem.uw(Plr2_SinVal_w);                // move.w Plr2_SinVal_w,d5
        int d6 = Mem.uw(Plr2_CosVal_w);                // move.w Plr2_CosVal_w,d6
        int a4 = Mem.l(Lvl_ObjectDataPtr_l);           // move.l Lvl_ObjectDataPtr_l,a4
        int a0 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a0
        int d7 = Mem.w(Lvl_NumObjectPoints_w);         // move.w Lvl_NumObjectPoints_w,d7
        int a2 = Plr2_ObsInLine_vb;                    // move.l #Plr2_ObsInLine_vb,a2
        int a3 = Plr2_ObjectDistances_vw;              // move.l #Plr2_ObjectDistances_vw,a3

        int d0, d1, d2, d3;
        while (true) { // .objpointrotlop:
            if (Mem.ub(a4 + ObjT_TypeID_b) == OBJ_TYPE_AUX) { // beq.s .itaux
                a4 += ObjT_SizeOf_l;                   // .itaux: NEXT_OBJ a4
                continue;                              // bra .objpointrotlop
            }

            d0 = Mem.w(a0);                            // move.w (a0),d0
            d0 = setw(d0, d0 - Mem.w(Plr2_XOff_l));    // sub.w Plr2_XOff_l,d0
            d1 = Mem.w(a0 + 4);                        // move.w 4(a0),d1
            a0 += 8;                                   // addq #8,a0

            if (Mem.w(a4 + ObjT_ZoneID_w) < 0) {       // tst.w ObjT_ZoneID_w(a4) ; blt .noworkout
                // .noworkout: (ordre inversé par rapport à PLR1, conservé)
                Mem.ww(a3, 0); a3 += 2;                // move.w #0,(a3)+
                Mem.wb(a2, 0); a2 += 1;                // move.b #0,(a2)+
                a4 += ObjT_SizeOf_l;                   // NEXT_OBJ a4
                if (--d7 == -1) {                      // dbra d7,.objpointrotlop
                    return;                            // rts
                }
                continue;
            }

            d2 = Mem.ub(a4 + ObjT_TypeID_b);           // moveq #0,d2 ; move.b ObjT_TypeID_b(a4),d2 (écrasé juste après)

            d1 = setw(d1, d1 - Mem.w(Plr2_ZOff_l));    // sub.w Plr2_ZOff_l,d1
            d2 = setw(d2, d0);                         // move.w d0,d2
            d2 = muls(d2, d6);                         // muls d6,d2
            d3 = d1;                                   // move.w d1,d3
            d3 = muls(d3, d5);                         // muls d5,d3
            d2 -= d3;                                  // sub.l d3,d2
            d2 += d2;                                  // add.l d2,d2

            if (d2 <= 0) {                             // bgt.s .okh
                d2 = -d2;                              // neg.l d2
            }
            // .okh:
            d2 = swap(d2);                             // swap d2

            d0 = muls(d0, d5);                         // muls d5,d0
            d1 = muls(d1, d6);                         // muls d6,d1
            d1 += d0;                                  // add.l d0,d1
            d1 <<= 2;                                  // asl.l #2,d1
            d1 = swap(d1);                             // swap d1
            d3 = 0;                                    // moveq #0,d3

            if ((short) d1 > 0) {                      // tst.w d1 ; ble.s .notinline
                d2 = setw(d2, ((short) d2) >> 1);      // asr.w #1,d2
                if ((short) d2 <= Mem.w(a6)) {         // cmp.w (a6),d2 ; bgt.s .notinline
                    d3 = (d3 & ~0xFF) | 0xFF;          // st d3
                }
            }
            // .notinline:
            Mem.wb(a2, d3); a2 += 1;                   // move.b d3,(a2)+
            Mem.ww(a3, d1); a3 += 2;                   // move.w d1,(a3)+
            a4 += ObjT_SizeOf_l;                       // NEXT_OBJ a4
            if (--d7 == -1) {                          // dbra d7,.objpointrotlop
                return;                                // rts
            }
        }
    }

    /** RotateObjectPts */
    public static void RotateObjectPts() {
        int d5 = Mem.uw(HireswallData.Vis_SinVal_w);   // move.w Vis_SinVal_w,d5 ; fetch sine of rotation
        int d6 = Mem.uw(HireswallData.Vis_CosVal_w);   // move.w Vis_CosVal_w,d6 ; cosine

        int a4 = Mem.l(Lvl_ObjectDataPtr_l);           // move.l Lvl_ObjectDataPtr_l,a4
        int a0 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a0
        int d7 = Mem.w(Lvl_NumObjectPoints_w);         // move.w Lvl_NumObjectPoints_w,d7
        int a1 = ObjRotated_vl;                        // move.l #ObjRotated_vl,a1

        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; bne RotateObjectPtsFullScreen
            RotateObjectPtsFullScreen(d5, d6, d7, a0, a1, a4);
            return;
        }

        int d0, d1, d2, d3;
        while (true) { // .objpointrotlop:
            if (Mem.ub(a4 + ObjT_TypeID_b) == OBJ_TYPE_AUX) { // beq.s .itaux
                a4 += ObjT_SizeOf_l;                   // .itaux: NEXT_OBJ a4
                continue;                              // bra .objpointrotlop
            }

            d0 = Mem.w(a0);                            // move.w (a0),d0 ; x of object point
            d0 = setw(d0, d0 - Mem.w(HiresData.Plr_XOff_l)); // sub.w Plr_XOff_l,d0 ; viewX = X - cam X
            d1 = Mem.w(a0 + 4);                        // move.w 4(a0),d1 ; z of object point
            a0 += 8;                                   // addq #8,a0 ; next point? or next object?

            if (Mem.w(a4 + ObjT_ZoneID_w) < 0) {       // tst.w ObjT_ZoneID_w(a4) ; blt .noworkout
                // .noworkout:
                Mem.wl(a1, 0); a1 += 4;                // clr.l (a1)+
                Mem.wl(a1, 0); a1 += 4;                // clr.l (a1)+
                a4 += ObjT_SizeOf_l;                   // NEXT_OBJ a4
                if (--d7 == -1) {                      // dbra d7,.objpointrotlop
                    return;                            // rts
                }
                continue;
            }

            d1 = setw(d1, d1 - Mem.w(HiresData.Plr_ZOff_l)); // sub.w Plr_ZOff_l,d1 ; viewZ = Z - cam Z

            d2 = d0;                                   // move.w d0,d2
            d2 = muls(d2, d6);                         // muls d6,d2 ; cosx = viewX * (cos << 16)
            d3 = d1;                                   // move.w d1,d3
            d3 = muls(d3, d5);                         // muls d5,d3 ; sinz = viewZ * (sin << 16)

            d2 -= d3;                                  // sub.l d3,d2 ; x' = cosx - sinz
            d2 += d2;                                  // add.l d2,d2 ; x'*2
            d2 = swap(d2);                             // swap d2 ; x' >> 16
            Mem.ww(a1, d2); a1 += 2;                   // move.w d2,(a1)+ ; finished rotated x'

            d0 = muls(d0, d5);                         // muls d5,d0 ; sinx = viewX * sin<<16
            d1 = muls(d1, d6);                         // muls d6,d1 ; cosz = viewZ * cos<<16
            d1 += d0;                                  // add.l d0,d1 ; z' = sinx + cosz
            d1 += d1;                                  // add.l d1,d1 ; *2
            d1 = swap(d1);                             // swap d1 ; >> 16
            d3 = 0;                                    // moveq #0,d3 ; FIMXE: why?

            Mem.ww(a1, d1); a1 += 2;                   // move.w d1,(a1)+ ; finished rotated z'

            d2 = (short) d2;                           // ext.l d2 ; whats the wobble about?
            d2 <<= 7;                                  // asl.l #7,d2
            d2 += Mem.l(HiresData.xwobble);            // add.l xwobble,d2
            Mem.wl(a1, d2); a1 += 4;                   // move.l d2,(a1)+ ; no clue

            if (--d7 == -1) {                          // dbra d7,.objpointrotlop
                return;                                // rts
            }
        }
    }

    /** RotateObjectPtsFullScreen */
    private static void RotateObjectPtsFullScreen(int d5, int d6, int d7, int a0, int a1, int a4) {
        int d0, d1, d2, d3;
        while (true) { // .objpointrotlop:
            if (Mem.ub(a4 + ObjT_TypeID_b) == OBJ_TYPE_AUX) { // beq.s .itaux
                a4 += ObjT_SizeOf_l;                   // .itaux: NEXT_OBJ a4
                continue;                              // bra .objpointrotlop
            }

            d0 = Mem.w(a0);                            // move.w (a0),d0
            d0 = setw(d0, d0 - Mem.w(HiresData.Plr_XOff_l)); // sub.w Plr_XOff_l,d0
            d1 = Mem.w(a0 + 4);                        // move.w 4(a0),d1
            a0 += 8;                                   // addq #8,a0

            if (Mem.w(a4 + ObjT_ZoneID_w) < 0) {       // tst.w ObjT_ZoneID_w(a4) ; blt .noworkout
                // .noworkout:
                Mem.wl(a1, 0); a1 += 4;                // clr.l (a1)+
                Mem.wl(a1, 0); a1 += 4;                // clr.l (a1)+
                a4 += ObjT_SizeOf_l;                   // NEXT_OBJ a4
                if (--d7 == -1) {                      // dbra d7,.objpointrotlop
                    return;                            // rts
                }
                continue;
            }

            d1 = setw(d1, d1 - Mem.w(HiresData.Plr_ZOff_l)); // sub.w Plr_ZOff_l,d1
            d2 = d0;                                   // move.w d0,d2
            d2 = muls(d2, d6);                         // muls d6,d2
            d3 = d1;                                   // move.w d1,d3
            d3 = muls(d3, d5);                         // muls d5,d3
            d2 -= d3;                                  // sub.l d3,d2
            d0 = muls(d0, d5);                         // muls d5,d0
            d2 += d2;                                  // add.l d2,d2
            d2 = swap(d2);                             // swap d2
            Mem.ww(a1, d2); a1 += 2;                   // move.w d2,(a1)+
            d1 = muls(d1, d6);                         // muls d6,d1
            d1 += d0;                                  // add.l d0,d1
            d1 <<= 2;                                  // asl.l #2,d1
            d1 = swap(d1);                             // swap d1
            d1 = (short) d1;                           // ext.l d1

            d1 = muls(d1, 85);                         // muls #85,d1 (au lieu de divs #3,d1)
            d3 = Mem.l(HiresData.xwobble);             // move.l xwobble,d3
            d1 >>= 8;                                  // asr.l #8,d1 ; 85/256 approximation of division by 3

            Mem.ww(a1, d1); a1 += 2;                   // move.w d1,(a1)+
            d2 = (short) d2;                           // ext.l d2
            d2 <<= 7;                                  // asl.l #7,d2
            d2 += d3;                                  // add.l d3,d2
            Mem.wl(a1, d2); a1 += 4;                   // move.l d2,(a1)+
            d2 -= d3;                                  // sub.l d3,d2

            a4 += ObjT_SizeOf_l;                       // NEXT_OBJ a4
            if (--d7 == -1) {                          // dbra d7,.objpointrotlop
                return;                                // rts
            }
        }
    }
}
