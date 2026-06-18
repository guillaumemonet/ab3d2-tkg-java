package ab3d2;

import ab3d2.modules.DevInst;
import ab3d2.modules.DevMacros;

import static ab3d2.Defs.*;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.mulu;
import static ab3d2.M68k.divs;
import static ab3d2.M68k.divu;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.swap;
import static ab3d2.HireswallData.Vid_CentreY_w;
import static ab3d2.HireswallData.draw_TopClip_w;
import static ab3d2.HireswallData.draw_BottomClip_w;
import static ab3d2.HireswallData.Draw_LeftClip_w;
import static ab3d2.HireswallData.Draw_RightClip_w;
import static ab3d2.ObjdrawhiresData.draw_TopY_3D_l;
import static ab3d2.ObjdrawhiresData.draw_BottomY_3D_l;
import static ab3d2.ObjdrawhiresData.draw_ObjScaleCols_vw;
import static ab3d2.ObjdrawhiresData.draw_BasePalPtr_l;
import static ab3d2.ObjdrawhiresData.draw_WhichLightPal_b;
import static ab3d2.ObjdrawhiresData.draw_FlipIt_b;
import static ab3d2.ObjdrawhiresData.draw_LightIt_b;
import static ab3d2.ObjdrawhiresData.draw_Additive_b;
import static ab3d2.ObjdrawhiresData.draw_TempPtr_l;
import static ab3d2.ObjdrawhiresData.polybright;
import static ab3d2.ObjdrawhiresData.firstpt;
import static ab3d2.ObjdrawhiresData.PolyAng;
import static ab3d2.ObjdrawhiresData.tstdca;
import static ab3d2.ObjdrawhiresData.offtopby;
import static ab3d2.ObjdrawhiresData.LinesPtr;
import static ab3d2.ObjdrawhiresData.PtsPtr;
import static ab3d2.ObjdrawhiresData.draw_PreGouraud_b;
import static ab3d2.ObjdrawhiresData.draw_Gouraud_b;
import static ab3d2.ObjdrawhiresData.draw_PreHoles_b;
import static ab3d2.ObjdrawhiresData.draw_Holes_b;
import static ab3d2.ObjdrawhiresData.ontoscrGL;
import static ab3d2.ObjdrawhiresData.ontoscrg;
import static ab3d2.ObjdrawhiresData.ontoscrh;
import static ab3d2.ObjdrawhiresData.GUARDBAND;
import static ab3d2.HiresData.drawit;
import static ab3d2.HiresData.linedir;
import static ab3d2.bss.PlayerBss.Plr1_ObjectPtr_l;
import static ab3d2.HiresData.Vid_CentreX_w;
import static ab3d2.HiresData.Plr_YOff_l;
import static ab3d2.HiresData.GLF_DatabasePtr_l;
import static ab3d2.HiresData.Vis_AngPos_w;
import static ab3d2.bss.DrawBss.*;
import static ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_PointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneEdgePtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneBorderPointsPtr_l;
import static ab3d2.bss.TablesBss.ObjRotated_vl;
import static ab3d2.bss.TablesBss.ConstantTable_vl;
import static ab3d2.bss.TablesBss.CurrentPointBrights_vl;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.PlayerBss.Plr1_TmpAngPos_w;
import static ab3d2.data.DrawData.willy;
import static ab3d2.data.DrawData.willybright;
import static ab3d2.data.DrawData.guff;
import static ab3d2.data.DrawData.draw_Brights_vw;
import static ab3d2.data.DrawData.draw_Brights2_vw;
import static ab3d2.data.DrawData.draw_XZAngs_vw;
import static ab3d2.data.DrawData.ontoscr;
import static ab3d2.data.TablesData.SinCosTable_vw;
import static ab3d2.data.TablesData.SINE_SIZE;
import static ab3d2.data.TablesData.COSINE_OFS;
import static ab3d2.HiresData.OneOverN_vw;
import static ab3d2.data.TablesData.MAX_ONE_OVER_N;
import static ab3d2.ObjectmoveData.oldx;
import static ab3d2.ObjectmoveData.oldz;
import static ab3d2.ObjectmoveData.newx;
import static ab3d2.ObjectmoveData.newz;
import static ab3d2.ObjectmoveData.speed;
import static ab3d2.ObjectmoveData.Range;
import static ab3d2.ObjectmoveData.AngRet;

/**
 * Traduction littérale de ab3d2_source/objdrawhires.s — rendu des objets/sprites.
 *
 * Draw_Objects trie en profondeur les objets de la zone courante (insertion sort
 * dans draw_DepthTable_vl) puis appelle draw_Object pour chacun, du plus loin au
 * plus proche. draw_Object dispatche : si l'octet 6 de l'objet == $ff →
 * draw_PolygonModel (modèle vectoriel), sinon draw_Bitmap (sprite scalé).
 *
 * drawBitmap regroupe draw_Bitmap + draw_bitmap_glare/additive/lighted + pastobjscale
 * + draw_right_side(/glare/additive) + object_behind (toute la routine « dessine un
 * bitmap » qui se termine par rts à object_behind). Les sprites sont stockés en
 * colonnes verticales 5 bits (3 colonnes par mot 16 bits → PACK0/1/2).
 *
 * AMOD_I = and.w #SINTAB_MASK_IDX (=SINE_SIZE*2-1). Les divs.l approchées par
 * 1/N (OneOverN_vw) de l'ASM d'origine ne sont PAS portées ici : on garde les
 * divisions exactes (USE_16X16_TEXEL_MULS non défini → branche MUL_INV 32 bits ;
 * mais Draw_Objects utilise des divs.w/divu.w directs, traduits tels quels).
 */
public final class Objdrawhires {

    private Objdrawhires() {
    }

    // objdrawhires.s:2-5 — EQU
    public static final int DRAW_BITMAP_NEAR_PLANE = 25;   // distances < this : derrière l'observateur (bitmaps)
    public static final int DRAW_VECTOR_NEAR_PLANE = 130;  // idem pour vecteurs
    public static final int DRAW_VECTOR_MAX_Z = 16383;     // points vecteurs au-delà : cullés

    // ==================================================================
    //  Draw_Objects (objdrawhires.s:80)
    // ==================================================================
    public static int Draw_Objects(int a0) {
        int d0 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d0
        if ((short) d0 < 1) {                          // cmp.w #1,d0 ; blt .before_water
            Mem.wl(draw_TopY_3D_l, Mem.l(Draw_BeforeWaterTop_l));     // .before_water
            Mem.wl(draw_BottomY_3D_l, Mem.l(Draw_BeforeWaterBottom_l));
            Mem.wb(draw_WhichDoing_b, 1);              // move.b #1,draw_WhichDoing_b
        } else if ((short) d0 == 1) {                  // beq .after_water
            Mem.wl(draw_TopY_3D_l, Mem.l(Draw_AfterWaterTop_l));
            Mem.wl(draw_BottomY_3D_l, Mem.l(Draw_AfterWaterBottom_l));
            Mem.wb(draw_WhichDoing_b, 0);
        } else {                                       // .full_room
            Mem.wl(draw_TopY_3D_l, Mem.l(Draw_TopOfRoom_l));
            Mem.wl(draw_BottomY_3D_l, Mem.l(Draw_BottomOfRoom_l));
            Mem.wb(draw_WhichDoing_b, 0);
        }

        // .done_top_bot — movem sauve d0-d7/a1-a6 (a0 préservé par valeur en Java)
        int d = setw(0, Mem.uw(Draw_RightClip_w));     // move.w Draw_RightClip_w,d0
        d = setw(d, d - Mem.uw(Draw_LeftClip_w));      // sub.w Draw_LeftClip_w,d0
        d = setw(d, d - 1);                            // subq #1,d0
        if ((short) d <= 0) return a0;                 // ble .done_all_in_front

        int a1 = Mem.l(Lvl_ObjectDataPtr_l);           // move.l Lvl_ObjectDataPtr_l,a1
        int a2 = ObjRotated_vl;                        // move.l #ObjRotated_vl,a2
        int a3 = draw_DepthTable_vl;                   // move.l #draw_DepthTable_vl,a3
        int d7 = setw(0, 79);                          // move.w #79,d7
        while (true) {                                 // .empty_tab
            Mem.wl(a3, 0x80010000);
            a3 += 4;                                   // move.l #$80010000,(a3)+
            d7 = setw(d7, d7 - 1);
            if ((short) d7 == -1) break;               // dbra d7,.empty_tab
        }

        int d0n = 0;                                   // moveq #0,d0
        while (true) {                                 // .insert_an_object
            int d1 = setw(0, Mem.uw(a1));              // move.w (a1),d1
            if ((short) d1 < 0) break;                 // blt .sorted_all
            int d2 = setw(0, Mem.uw(a1 + EntT_ZoneID_w)); // move.w EntT_ZoneID_w(a1),d2
            boolean inZone = ((short) d2 == Mem.w(Draw_CurrentZone_w)); // cmp Draw_CurrentZone_w,d2 ; beq .in_this_zone
            if (inZone) {
                int d4 = Mem.ub(Draw_DoUpper_b);       // move.b Draw_DoUpper_b,d4
                int d3 = Mem.ub(a1 + ShotT_InUpperZone_b); // move.b ShotT_InUpperZone_b(a1),d3
                d3 = (d3 ^ d4) & 0xFF;                 // eor.b d4,d3
                if (d3 != 0) inZone = false;           // bne .not_in_this_zone
            }
            if (!inZone) {                             // .not_in_this_zone
                a1 += ObjT_SizeOf_l;                   // NEXT_OBJ a1
                d0n++;                                 // addq #1,d0
                continue;                              // bra .insert_an_object
            }
            // .in_this_zone (passe les tests)
            d1 = setw(d1, Mem.uw(a2 + (d1 & 0xFFFF) * 8 + 2)); // move.w 2(a2,d1.w*8),d1  (zpos)
            int a4 = draw_DepthTable_vl - 4;           // move.l #draw_DepthTable_vl-4,a4
            do {                                       // .still_in_front
                a4 += 4;                               // addq #4,a4
            } while ((short) d1 < Mem.w(a4));          // cmp.w (a4),d1 ; blt .still_in_front
            int a5 = draw_DepthTableEnd - 4;           // move.l #draw_DepthTableEnd-4,a5
            while (true) {                             // .finished_shift
                a5 -= 4;                               // -(a5)
                Mem.wl(a5 + 4, Mem.l(a5));             // move.l -(a5),4(a5)
                if (!(a5 > a4)) break;                 // cmp.l a4,a5 ; bgt .finished_shift
            }
            Mem.ww(a4, d1);                            // move.w d1,(a4)
            Mem.ww(a4 + 2, d0n);                       // move.w d0,2(a4)
            a1 += ObjT_SizeOf_l;                       // NEXT_OBJ a1
            d0n++;                                     // addq #1,d0
        }

        // .sorted_all
        a3 = draw_DepthTable_vl;                       // move.l #draw_DepthTable_vl,a3
        while (true) {                                 // .go_back_and_do_another
            int dz = setw(0, Mem.uw(a3));
            a3 += 2;                                   // move.w (a3)+,d0
            if ((short) dz < 0) break;                 // blt .done_all_in_front
            int dobj = setw(0, Mem.uw(a3));
            a3 += 2;                                   // move.w (a3)+,d0
            draw_Object(dobj);                         // bsr draw_Object
        }
        return a0;                                     // .done_all_in_front: rts
    }

    // ==================================================================
    //  draw_Object (objdrawhires.s:184)
    // ==================================================================
    static void draw_Object(int d0) {
        // DEV_INC.w DrawObjectCallCount
        Mem.ww(DevInst.dev_DrawObjectCallCount_w, Mem.uw(DevInst.dev_DrawObjectCallCount_w) + 1);

        int a0 = Mem.l(Lvl_ObjectDataPtr_l);           // move.l Lvl_ObjectDataPtr_l,a0
        int a1 = ObjRotated_vl;                        // move.l #ObjRotated_vl,a1
        d0 = setw(d0, (d0 & 0xFFFF) << 6);             // asl.w #6,d0
        a0 = a0 + (short) d0;                          // adda.w d0,a0
        Mem.wb(draw_InUpperZone_b, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a0),draw_InUpperZone_b
        d0 = setw(d0, Mem.uw(a0));                     // move.w (a0),d0
        int d1 = setw(0, Mem.uw(a1 + (d0 & 0xFFFF) * 8 + 2)); // move.w 2(a1,d0.w*8),d1  (z pos)

        Mem.ww(draw_LeftClipB_w, Mem.uw(Draw_LeftClip_w));   // move.w Draw_LeftClip_w,draw_LeftClipB_w
        Mem.ww(draw_RightClipB_w, Mem.uw(Draw_RightClip_w)); // move.w Draw_RightClip_w,draw_RightClipB_w

        if (Mem.ub(a0 + 6) != 0xFF) {                  // cmp.b #$ff,6(a0) ; bne draw_Bitmap
            drawBitmap(a0, a1, d0, d1);                // draw_Bitmap
            return;                                    // (object_behind rts → GETREGS rts)
        }
        if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_POLYGON_MODELS)) { // DEV_CHECK_SET SKIP_POLYGON_MODELS,.done
            return;
        }
        draw_PolygonModel(a0, a1, d0, d1);             // bsr draw_PolygonModel
        // .done: GETREGS ; rts
    }

    // ==================================================================
    //  draw_FindRoughAngle (objdrawhires.s:1339)
    //  Entrées d4 (cos), d5 (sin) ; renvoie l'angle approché (16 secteurs) dans d4.
    // ==================================================================
    static final int[] draw_MapToAng_vw = {
            3, 2, 0, 1, 4, 5, 7, 6,
            12, 13, 15, 14, 11, 10, 8, 9
    };

    static int draw_FindRoughAngle(int d4, int d5) {
        d5 = -d5;                                      // neg.l d5
        int d7 = 0;                                    // moveq #0,d7
        if (d4 < 0) {                                  // tst.l d4 ; bge .no8
            d7 += 8;                                   // add.w #8,d7
            d4 = -d4;                                  // neg.l d4
        }
        if (d5 < 0) {                                  // tst.l d5 ; bge .no4
            d5 = -d5;                                  // neg.l d5
            d7 += 4;                                   // add.w #4,d7
        }
        if (d4 < d5) {                                 // cmp.l d5,d4 ; bge .no2
            d7 += 2;                                   // addq #2,d7
            int t = d4; d4 = d5; d5 = t;               // exg d4,d5
        }
        d4 = d4 >> 1;                                  // asr.l #1,d4
        if (d4 < d5) {                                 // cmp.l d5,d4 ; bge .no1
            d7 += 1;                                   // addq #1,d7
        }
        return setw(d4, draw_MapToAng_vw[d7 & 0xFFFF] & 0xFFFF); // move.w draw_MapToAng_vw(pc,d7.w*2),d4
    }

    // ==================================================================
    //  draw_ResetAngleBrights (objdrawhires.s:1383)
    //  Renvoie a2 = draw_AngleBrights_vl+32 (comme l'ASM laisse a2).
    // ==================================================================
    static int draw_ResetAngleBrights() {
        int a2 = draw_AngleBrights_vl;                 // move.l #draw_AngleBrights_vl,a2
        for (int i = 0; i < 16; i++) {                 // 16 × move.l #$80808080,(a2)+
            Mem.wl(a2, 0x80808080);
            a2 += 4;
        }
        a2 -= 64;                                      // sub.w #64,a2
        int d0 = setw(0, Mem.uw(Draw_CurrentZone_w));  // move.w Draw_CurrentZone_w,d0
        draw_CalcBrightsInZone(d0, a2);                // bsr draw_CalcBrightsInZone
        return draw_AngleBrights_vl + 32;              // move.l #draw_AngleBrights_vl+32,a2 ; rts
    }

    // ==================================================================
    //  draw_CalcBrightsInZone (objdrawhires.s:1592)
    //  Entrées d0 = zone, a2 = ptr table de brightness d'angle.
    // ==================================================================
    static void draw_CalcBrightsInZone(int d0, int a2) {
        int d1 = muls(d0, 20);                         // move.w d0,d1 ; muls #20,d1
        int a1 = Mem.l(Lvl_ZoneBorderPointsPtr_l) + d1; // move.l Lvl_ZoneBorderPointsPtr_l,a1 ; add.l d1,a1
        int a0 = CurrentPointBrights_vl + d1 * 4;      // move.l #CurrentPointBrights_vl,a0 ; lea (a0,d1.l*4),a0
        if (Mem.b(draw_InUpperZone_b) != 0) {          // tst.b draw_InUpperZone_b ; beq .not_in_upper_zone
            a0 += 4;                                   // adda.w #4,a0
        }
        // .not_in_upper_zone
        int a3 = Mem.l(Lvl_PointsPtr_l);               // move.l Lvl_PointsPtr_l,a3
        Mem.ww(oldx, Mem.uw(draw_Obj_XPos_w));         // move.w draw_Obj_XPos_w,oldx
        Mem.ww(oldz, Mem.uw(draw_Obj_ZPos_w));         // move.w draw_Obj_ZPos_w,oldz
        Mem.ww(speed, 10);                             // move.w #10,speed
        Mem.ww(Range, 0);                              // move.w #0,Range
        while (true) {                                 // .do_point_bright
            int d0b = setw(0, Mem.uw(a1));
            a1 += 2;                                    // move.w (a1)+,d0
            if ((short) d0b < 0) break;                // blt .done_point_bright
            Mem.ww(newx, Mem.uw(a3 + (d0b & 0xFFFF) * 4));     // move.w (a3,d0.w*4),newx
            Mem.ww(newz, Mem.uw(a3 + (d0b & 0xFFFF) * 4 + 2)); // move.w 2(a3,d0.w*4),newz
            Objectmove.HeadTowardsAng();               // jsr HeadTowardsAng
            int d1b = setw(0, Mem.uw(AngRet));         // move.w AngRet,d1
            d1b = setw(d1b, -(short) d1b);             // neg.w d1
            d1b = setw(d1b, d1b & (SINE_SIZE * 2 - 1));// AMOD_I d1
            d1b = setw(d1b, ((short) d1b) >> 8);       // asr.w #8,d1
            d1b = setw(d1b, ((short) d1b) >> 1);       // asr.w #1,d1
            int d0c = setw(0, Mem.uw(a0));             // move.w (a0),d0
            if ((short) d0c < 0) {                     // bge .okpos
                d0c = setw(d0c, d0c + 332);            // add.w #332,d0
                d0c = setw(d0c, ((short) d0c) >> 2);   // asr.w #2,d0
                d0c = setw(d0c, -(short) d0c);         // neg.w d0
                d0c = setw(d0c, d0c + 332);            // add.w #332,d0
            }
            // .okpos
            d0c = setw(d0c, d0c - 300);                // sub.w #300,d0
            if ((short) d0c < 0) d0c = setw(d0c, 0);   // bge .okpos3 ; move.w #0,d0
            // .okpos3
            int d2 = setb(d0c, d0c);                   // move.b d0,d2
            d2 = setb(d2, ((byte) d2) >> 1);           // asr.b #1,d2
            d0c = setb(d0c, d0c + d2);                 // add.b d2,d0
            Mem.wb(a2 + (d1b & 0xFFFF), d0c);          // move.b d0,(a2,d1.w)
            d0c = setw(d0c, Mem.uw(a0 + 2));           // move.w 2(a0),d0
            if ((short) d0c < 0) {                     // bge .okpos2
                d0c = setw(d0c, d0c + 332);            // add.w #332,d0
                d0c = setw(d0c, ((short) d0c) >> 2);   // asr.w #2,d0
                d0c = setw(d0c, -(short) d0c);         // neg.w d0
                d0c = setw(d0c, d0c + 332);            // add.w #332,d0
            }
            // .okpos2
            d0c = setw(d0c, d0c - 300);                // sub.w #300,d0
            if ((short) d0c < 0) d0c = setw(d0c, 0);   // bge .okpos4 ; move.w #0,d0
            // .okpos4
            d2 = setb(d0c, d0c);                       // move.b d0,d2
            d2 = setb(d2, ((byte) d2) >> 1);           // asr.b #1,d2
            d0c = setb(d0c, d0c + d2);                 // add.b d2,d0
            Mem.wb(a2 + (d1b & 0xFFFF) + 16, d0c);     // move.b d0,16(a2,d1.w)
            a0 += 8;                                   // adda.w #8,a0
        }
        // .done_point_bright: rts
    }

    // ==================================================================
    //  draw_TweenBrights (objdrawhires.s:1521)
    //  Interpole les valeurs entre les angles connus (16 entrées, octets ; -128 = vide).
    // ==================================================================
    static void draw_TweenBrights(int a0) {
        int d0 = 0;                                    // moveq #0,d0
        while ((byte) Mem.ub(a0 + (d0 & 0xFFFF)) == -128) { // .backinto: cmp.b #-128,(a0,d0.w) ; bne .okbr
            d0 = setw(d0, d0 + 1);                     // addq #1,d0
            // GARDE anti-freeze (l'ASM original ne borne pas .backinto) : si les 16 octets de
            // l'anneau sont tous vides (-128), d0 dépasse 15 → d7 (pos. départ) ≥ 16, que la
            // boucle .findnext (masquée &15) ne peut jamais ré-atteindre → boucle externe
            // infinie. Anneau vide = rien à interpoler : on sort. Identique à l'original pour
            // des données valides (où .backinto s'arrête à d0 < 16). Donnée dégénérée signalée.
            if ((d0 & 0xFFFF) >= 16) {
                warnEmptyBrightRing(a0);
                return;
            }
        }
        // .okbr
        int d7 = setb(0, d0);                          // move.b d0,d7  (starting pos)
        int d1 = setb(0, d0);                          // move.b d0,d1  (previous pos)
        while (true) {
            // .findnext
            do {
                d0 = setw(d0, d0 + 1);                 // addq #1,d0
                d0 = setw(d0, d0 & 15);                // and.w #15,d0
            } while ((byte) Mem.ub(a0 + (d0 & 0xFFFF)) == -128); // cmp.b #-128,(a0,d0.w) ; beq .findnext
            int d2 = setb(0, Mem.ub(a0 + (d1 & 0xFFFF))); // move.b (a0,d1.w),d2
            int d3 = setb(0, Mem.ub(a0 + (d0 & 0xFFFF))); // move.b (a0,d0.w),d3
            d3 = setw(d3, d3 - d2);                    // sub.w d2,d3
            int d4 = setw(0, d0);                      // move.w d0,d4
            d4 = setw(d4, d4 - d1);                    // sub.w d1,d4
            if ((short) d4 <= 0) d4 = setw(d4, d4 + 16); // bgt .okpos ; add.w #16,d4
            // .okpos
            d2 = swap(d2);                             // swap d2
            d3 = swap(d3);                             // swap d3
            if (d3 != 0) {                             // beq .skip_zero_dividend  (tst.l d3 implicite via swap result)
                int d4saveW = d4 & 0xFFFF;             // move.w d4,-2(sp)  ← sauve l'ÉCART (avant écrasement)
                d4 = setw(d4, Mem.uw(OneOverN_vw + (d4 & 0xFFFF) * 2)); // move.w OneOverN_vw(pc,d4.w*2),d4
                d4 = (short) d4;                       // ext.l d4
                d3 = d3 >> 7;                          // asr.l #7,d3
                d3 = d3 * d4;                          // muls.l d4,d3
                d3 = d3 >> 7;                          // asr.l #7,d3
                d4 = setw(d4, d4saveW);                // move.w -2(sp),d4 (restaure l'écart → compteur de tweens)
            }
            // .skip_zero_dividend
            d4 = setw(d4, d4 - 1);                     // subq #1,d4  (number of tweens)
            while (true) {                             // .put_in_tween_loop
                d2 = swap(d2);                         // swap d2
                Mem.wb(a0 + (d1 & 0xFFFF), d2);        // move.b d2,(a0,d1.w)
                d2 = swap(d2);                         // swap d2
                d2 = d2 + d3;                          // add.l d3,d2
                d1 = setw(d1, d1 + 1);                 // addq #1,d1
                d1 = setw(d1, d1 & 15);                // and.w #15,d1
                d4 = setw(d4, d4 - 1);
                if ((short) d4 == -1) break;           // dbra d4,.put_in_tween_loop
            }
            if (((d0 ^ d7) & 0xFF) == 0) break;        // cmp.b d0,d7 ; beq .done_all
            d1 = setw(d1, d0);                         // move.w d0,d1
            // bra .findnext
        }
        // .done_all: rts
    }

    private static boolean warnedEmptyBrightRing = false;

    /** Signale (une fois) un anneau de luminosité vide — donnée dégénérée déclenchant la garde anti-freeze. */
    private static void warnEmptyBrightRing(int a0) {
        if (warnedEmptyBrightRing) {
            return;
        }
        warnedEmptyBrightRing = true;
        int ring = (a0 - draw_AngleBrights_vl);
        System.err.println("[draw_TweenBrights] anneau de luminosité vide (offset +" + ring
                + ", zone " + Mem.uw(Draw_CurrentZone_w) + ", modèle " + dbgCurModel
                + ") — interpolation ignorée (garde anti-freeze). Donnée probablement corrompue.");
    }

    // ==================================================================
    //  draw_CalcBrightRings (objdrawhires.s:1415)
    // ==================================================================
    static void draw_CalcBrightRings() {
        int a2 = draw_ResetAngleBrights();             // bsr draw_ResetAngleBrights  (a2 = draw_AngleBrights_vl+32)

        int a1 = Mem.l(Lvl_ZoneEdgePtr_l);             // move.l Lvl_ZoneEdgePtr_l,a1
        int d0 = setw(0, Mem.uw(Draw_CurrentZone_w));  // move.w Draw_CurrentZone_w,d0
        int a4 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a4
        a4 = Mem.l(a4 + (d0 & 0xFFFF) * 4);            // move.l (a4,d0.w*4),a4
        int a5 = a4;                                   // move.l a4,a5
        a5 = a5 + (short) Mem.uw(a4 + ZoneT_EdgeListOffset_w); // adda.w ZoneT_EdgeListOffset_w(a4),a5

        while (true) {                                 // .do_all_walls
            d0 = setw(0, Mem.uw(a5));
            a5 += 2;                                   // move.w (a5)+,d0
            if ((short) d0 < 0) break;                 // blt .no_more_walls
            d0 = setw(d0, (d0 & 0xFFFF) << 4);         // asl.w #4,d0
            int a3 = a1 + (short) d0;                  // lea (a1,d0.w),a3
            d0 = setw(d0, Mem.uw(a3 + EdgeT_JoinZone_w)); // move.w EdgeT_JoinZone_w(a3),d0
            if ((short) d0 >= 0) {                     // bge .solid_wall  (blt.s .solid_wall : non-exit si <0)
                // exit (join zone) : recurse
                draw_CalcBrightsInZone(d0, a2);        // bsr draw_CalcBrightsInZone (a1/a4/a5 sauvés par movem)
                continue;                              // bra .do_all_walls
            }
            // .solid_wall (a wall, not an exit)
            int d1 = setw(0, Mem.uw(a3 + EdgeT_XLen_w)); // move.w EdgeT_XLen_w(a3),d1
            int d2 = setw(0, Mem.uw(a3 + EdgeT_ZLen_w)); // move.w EdgeT_ZLen_w(a3),d2
            Mem.ww(newx, Mem.uw(oldx));                // move.w oldx,newx
            Mem.ww(newz, Mem.uw(oldz));                // move.w oldz,newz
            Mem.ww(newx, Mem.uw(newx) - d2);           // sub.w d2,newx
            Mem.ww(newz, Mem.uw(newz) + d1);           // add.w d1,newz
            Objectmove.HeadTowardsAng();               // jsr HeadTowardsAng
            d1 = setw(0, Mem.uw(AngRet));              // move.w AngRet,d1
            d1 = setw(d1, -(short) d1);                // neg.w d1
            d1 = setw(d1, d1 & (SINE_SIZE * 2 - 1));   // AMOD_I d1
            d1 = setw(d1, ((short) d1) >> 8);          // asr.w #8,d1
            d1 = setw(d1, ((short) d1) >> 1);          // asr.w #1,d1
            Mem.wb(a2 + (d1 & 0xFFFF), 48);            // move.b #48,(a2,d1.w)
            Mem.wb(a2 + (d1 & 0xFFFF) + 16, 48);       // move.b #48,16(a2,d1.w)
            // bra .do_all_walls
        }

        // .no_more_walls
        draw_TweenBrights(draw_AngleBrights_vl);       // move.l #draw_AngleBrights_vl,a0 ; bsr draw_TweenBrights
        draw_TweenBrights(draw_AngleBrights_vl + 16);
        draw_TweenBrights(draw_AngleBrights_vl + 32);
        draw_TweenBrights(draw_AngleBrights_vl + 48);

        int a0 = draw_AngleBrights_vl;                 // move.l #draw_AngleBrights_vl,a0
        int d0s = setb(0, 15);                         // move.b #15,d0
        while (true) {                                 // .sum_brights_loop
            int d3 = setb(0, Mem.ub(a0 + 32));         // move.b 32(a0),d3
            int d4 = setb(0, Mem.ub(a0 + 48));         // move.b 48(a0),d4
            d3 = setw(d3, -(byte) d3);                 // neg.w d3  (d3 chargé en octet → étendu)
            d3 = setw(d3, d3 + 48);                    // add.w #48,d3
            d4 = setw(d4, -(byte) d4);                 // neg.w d4
            d4 = setw(d4, d4 + 48);                    // add.w #48,d4
            d4 = setw(d4, ((short) d4) >> 1);          // asr.w #1,d4
            d3 = setw(d3, ((short) d3) >> 1);          // asr.w #1,d3
            int d5 = setb(0, Mem.ub(a0 + 16));         // move.b 16(a0),d5
            d4 = setb(d4, d4 - d5);                    // sub.b d5,d4
            if ((byte) d4 > 0) d4 = 0;                 // ble .ok2 ; moveq #0,d4
            // .ok2
            d5 = setb(d5, Mem.ub(a0));                 // move.b (a0),d5
            d3 = setb(d3, d3 - d5);                    // sub.b d5,d3
            if ((byte) d3 > 0) d3 = 0;                 // ble .ok1 ; moveq #0,d3
            // .ok1
            d3 = setb(d3, -(byte) d3);                 // neg.b d3
            d4 = setb(d4, -(byte) d4);                 // neg.b d4
            Mem.wb(a0 + 16, d4);                       // move.b d4,16(a0)
            Mem.wb(a0, d3);
            a0 += 1;                                    // move.b d3,(a0)+
            d0s = setb(d0s, d0s - 1);                  // dbra d0
            if ((byte) d0s == -1) break;
        }
        // rts
    }

    // ==================================================================
    //  draw_Bitmap (objdrawhires.s:563) + pastobjscale + draw_right_side
    //  Entrées : a0 = objet, a1 = ObjRotated_vl, d0 = pt num, d1 = z.
    // ==================================================================
    static void drawBitmap(int a0, int a1, int d0, int d1) {
        int d2, d3, d4, d5, d6, d7;
        int a2, a3, a4, a5, a6;

        Mem.wl(draw_AuxX_w, 0);                         // move.l #0,draw_AuxX_w (clear AuxX+AuxY)
        if (Mem.ub(a0 + ObjT_TypeID_b) == OBJ_TYPE_AUX) { // cmp.b #OBJ_TYPE_AUX,ObjT_TypeID_b(a0) ; bne .not_auxilliary_object
            Mem.ww(draw_AuxX_w, Mem.uw(a0 + ShotT_AuxOffsetX_w)); // move.w ShotT_AuxOffsetX_w(a0),draw_AuxX_w
            Mem.ww(draw_AuxY_w, Mem.uw(a0 + ShotT_AuxOffsetY_w)); // move.w ShotT_AuxOffsetY_w(a0),draw_AuxY_w
        }
        // .not_auxilliary_object
        if (Mem.l(a0 + ObjT_YPos_l) < 0) {             // tst.l ObjT_YPos_l(a0) ; blt draw_bitmap_glare
            drawBitmapGlare(a0, a1);
            return;
        }

        d0 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d0  (pt num)
        a4 = Mem.l(Lvl_ObjectPointsPtr_l);             // move.l Lvl_ObjectPointsPtr_l,a4
        Mem.ww(draw_Obj_XPos_w, Mem.uw(a4 + (d0 & 0xFFFF) * 8));     // move.w (a4,d0.w*8),draw_Obj_XPos_w
        Mem.ww(draw_Obj_ZPos_w, Mem.uw(a4 + (d0 & 0xFFFF) * 8 + 4)); // move.w 4(a4,d0.w*8),draw_Obj_ZPos_w
        d1 = setw(d1, Mem.uw(a1 + (d0 & 0xFFFF) * 8 + 2)); // move.w 2(a1,d0.w*8),d1
        if ((short) d1 <= DRAW_BITMAP_NEAR_PLANE) return; // cmp.w #DRAW_BITMAP_NEAR_PLANE,d1 ; ble object_behind

        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; beq.s .no_depth_adjust
            d1 = muls(d1, 927);                        // muls #927,d1
            d1 = d1 >> 8;                              // asr.l #8,d1
            d1 = d1 >> 2;                              // asr.l #2,d1
        }
        // .no_depth_adjust
        d2 = setw(0, Mem.uw(draw_TopClip_w));          // move.w draw_TopClip_w,d2
        d3 = setw(0, Mem.uw(draw_BottomClip_w));       // move.w draw_BottomClip_w,d3
        d6 = Mem.l(draw_TopY_3D_l);                    // move.l draw_TopY_3D_l,d6
        d6 = d6 - Mem.l(Plr_YOff_l);                   // sub.l Plr_YOff_l,d6
        d6 = divs(d6, d1);                             // divs d1,d6
        d6 = setw(d6, d6 + Mem.uw(Vid_CentreY_w));     // add.w Vid_CentreY_w,d6
        if ((short) d6 >= (short) d3) return;          // cmp.w d3,d6 ; bge object_behind
        if ((short) d6 < (short) d2) d6 = setw(d6, d2); // cmp.w d2,d6 ; bge.s .okobtc ; move.w d2,d6
        // .okobtc
        Mem.ww(draw_ObjClipT_w, d6);                   // move.w d6,draw_ObjClipT_w
        d6 = Mem.l(draw_BottomY_3D_l);                 // move.l draw_BottomY_3D_l,d6
        d6 = d6 - Mem.l(Plr_YOff_l);                   // sub.l Plr_YOff_l,d6
        d6 = divs(d6, d1);                             // divs d1,d6
        d6 = setw(d6, d6 + Mem.uw(Vid_CentreY_w));     // add.w Vid_CentreY_w,d6
        if ((short) d6 <= (short) d2) return;          // cmp.w d2,d6 ; ble object_behind
        if ((short) d6 > (short) d3) d6 = setw(d6, d3); // cmp.w d3,d6 ; ble.s .okobbc ; move.w d3,d6
        // .okobbc
        Mem.ww(draw_ObjClipB_w, d6);                   // move.w d6,draw_ObjClipB_w
        d0 = Mem.l(a1 + (d0 & 0xFFFF) * 8 + 4);        // move.l 4(a1,d0.w*8),d0
        d2 = setw(0, Mem.uw(draw_AuxX_w));             // move.w draw_AuxX_w,d2
        d2 = (short) d2;                               // ext.l d2
        d2 = d2 << 7;                                  // asl.l #7,d2
        d0 = d0 + d2;                                  // add.l d2,d0
        d6 = setw(d1, d1);                             // move.w d1,d6
        d6 = setw(d6, ((short) d6) >> 6);              // asr.w #6,d6
        d6 = setw(d6, d6 + Mem.uw(a0));
        a0 += 2;                                       // add.w (a0)+,d6
        Mem.ww(draw_BrightToAdd_w, d6);                // move.w d6,draw_BrightToAdd_w
        if ((short) d6 < 0) d6 = 0;                    // bge.s .brighttoonot ; moveq #0,d6
        // .brighttoonot
        a4 = 0;                                        // sub.l a4,a4
        a4 = (short) Mem.uw(draw_ObjScaleCols_vw + (d6 & 0xFFFF) * 2); // move.w draw_ObjScaleCols_vw(pc,d6.w*2),a4
        // bra pastobjscale

        // ---- pastobjscale (687) ----
        d2 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d2  (height)
        d2 = setw(d2, d2 + Mem.uw(draw_AuxY_w));       // add.w draw_AuxY_w,d2
        d2 = (short) d2;                               // ext.l d2
        d2 = d2 << 7;                                  // asl.l #7,d2
        d2 = d2 - Mem.l(Plr_YOff_l);                   // sub.l Plr_YOff_l,d2
        d2 = divs(d2, d1);                             // divs d1,d2
        d2 = setw(d2, d2 + Mem.uw(Vid_CentreY_w));     // add.w Vid_CentreY_w,d2
        d0 = divs(d0, d1);                             // divs d1,d0
        d0 = setw(d0, d0 + Mem.uw(Vid_CentreX_w));     // add.w Vid_CentreX_w,d0  (x pos of middle)

        a6 = Mem.l(GLF_DatabasePtr_l);                 // move.l GLF_DatabasePtr_l,a6
        a6 = a6 + GLFT_FrameData_l;                    // lea GLFT_FrameData_l(a6),a6
        a5 = Draw_ObjectPtrs_vl;                       // move.l #Draw_ObjectPtrs_vl,a5
        d7 = setw(0, Mem.uw(a0 + 2));                  // move.w 2(a0),d7
        d7 = setw(d7, (d7 & 0xFFFF) << 4);             // asl.w #4,d7
        a5 = a5 + (short) d7;                          // adda.w d7,a5
        d7 = setw(d7, (d7 & 0xFFFF) << 4);             // asl.w #4,d7
        a6 = a6 + (short) d7;                          // adda.w d7,a6
        Mem.wb(draw_LightIt_b, 0);                     // clr.b draw_LightIt_b
        Mem.wb(draw_Additive_b, 0);                    // clr.b draw_Additive_b
        d7 = setb(0, Mem.ub(a0 + 4));                  // move.b 4(a0),d7
        Mem.wb(draw_FlipIt_b, (d7 & 0x80) != 0 ? 0xFF : 0); // btst #7,d7 ; sne draw_FlipIt_b
        d7 = setb(d7, d7 & 127);                       // and.b #127,d7
        d7 = setb(d7, d7 - 2);                         // sub.b #2,d7
        if ((byte) d7 >= 0) {                          // blt.s .not_a_light
            if ((byte) d7 < 4) {                       // cmp.b #4,d7 ; blt.s .is_a_light
                // .is_a_light
                Mem.wb(draw_LightIt_b, 0xFF);          // st draw_LightIt_b
                Mem.wb(draw_WhichLightPal_b, d7);      // move.b d7,draw_WhichLightPal_b
            } else {
                Mem.wb(draw_Additive_b, 0xFF);         // st draw_Additive_b
            }
        }
        // .not_a_light
        d7 = setb(0, Mem.ub(a0 + 5));                  // moveq #0,d7 ; move.b 5(a0),d7  (current frame)
        a6 = a6 + (d7 & 0xFFFF) * 8;                   // lea (a6,d7.w*8),a6
        a3 = ConstantTable_vl;                         // move.l #ConstantTable_vl,a3
        d3 = setb(0, Mem.ub(a0));
        a0 += 1;                                       // moveq #0,d3 ; move.b (a0)+,d3
        d4 = setb(0, Mem.ub(a0));
        a0 += 1;                                       // moveq #0,d4 ; move.b (a0)+,d4
        d3 = d3 << 7;                                  // lsl.l #7,d3
        d4 = d4 << 7;                                  // lsl.l #7,d4
        d3 = divs(d3, d1);                             // divs d1,d3  (width in pixels)
        d4 = divs(d4, d1);                             // divs d1,d4  (height in pixels)
        d2 = setw(d2, d2 - d4);                        // sub.w d4,d2
        d0 = setw(d0, d0 - d3);                        // sub.w d3,d0
        if ((short) d0 >= (short) Mem.uw(draw_RightClipB_w)) return; // cmp.w draw_RightClipB_w,d0 ; bge object_behind
        d3 = setw(d3, d3 + d3);                        // add.w d3,d3
        if ((short) d2 >= (short) Mem.uw(draw_ObjClipB_w)) return;   // cmp.w draw_ObjClipB_w,d2 ; bge object_behind
        d4 = setw(d4, d4 + d4);                        // add.w d4,d4

        Mem.wl(draw_WADPtr_l, Mem.l(a5));
        a5 += 4;                                       // move.l (a5)+,draw_WADPtr_l
        Mem.wl(draw_PtrPtr_l, Mem.l(a5));
        a5 += 4;                                       // move.l (a5)+,draw_PtrPtr_l
        a4 = a4 + Mem.l(a5 + 4);                       // add.l 4(a5),a4
        Mem.wl(draw_BasePalPtr_l, Mem.l(a5 + 4));      // move.l 4(a5),draw_BasePalPtr_l
        d7 = Mem.l(a6);                                // move.l (a6),d7  (pointer to current frame)
        Mem.ww(draw_DownStrip_w, d7);                  // move.w d7,draw_DownStrip_w
        a5 = Mem.l(draw_PtrPtr_l);                     // move.l draw_PtrPtr_l,a5
        if (Mem.b(draw_FlipIt_b) != 0) {               // tst.b draw_FlipIt_b ; beq.s .no_flip
            d6 = setw(0, Mem.uw(a6 + 4));              // move.w 4(a6),d6
            d6 = setw(d6, d6 + d6);                    // add.w d6,d6
            d6 = setw(d6, d6 - 1);                     // subq #1,d6
            a5 = a5 + (short) d6 * 4;                  // lea (a5,d6.w*4),a5
        }
        // .no_flip
        d7 = swap(d7);                                 // swap d7
        d7 = setw(d7, (d7 & 0xFFFF) << 2);             // asl.w #2,d7
        a5 = a5 + (short) d7;                          // adda.w d7,a5
        d7 = setw(d1, d1);                             // move.w d1,d7
        d6 = setw(0, Mem.uw(a6 + 4));                  // moveq #0,d6 ; move.w 4(a6),d6
        d6 = setw(d6, d6 + d6);                        // add.w d6,d6
        d6 = setw(d6, d6 - 1);                         // subq #1,d6
        d7 = mulu(d7, d6);                             // mulu d6,d7
        d6 = setb(0, Mem.ub(a0 - 2));                  // moveq #0,d6 ; move.b -2(a0),d6
        if ((byte) d6 == 0) return;                    // beq object_behind
        d7 = divu(d7, d6);                             // divu d6,d7
        d7 = swap(d7);                                 // swap d7
        d7 = setw(d7, 0);                              // clr.w d7
        d7 = swap(d7);                                 // swap d7
        a2 = a3 + d7 * 8;                              // lea (a3,d7.l*8),a2  (horiz const)
        d7 = setw(d1, d1);                             // move.w d1,d7
        d6 = setw(0, Mem.uw(a6 + 6));                  // move.w 6(a6),d6
        d6 = setw(d6, d6 + d6);                        // add.w d6,d6
        d6 = setw(d6, d6 - 1);                         // subq #1,d6
        d7 = mulu(d7, d6);                             // mulu d6,d7
        d6 = setb(0, Mem.ub(a0 - 1));                  // moveq #0,d6 ; move.b -1(a0),d6
        if ((byte) d6 == 0) return;                    // beq object_behind
        d7 = divu(d7, d6);                             // divu d6,d7
        d7 = swap(d7);                                 // swap d7
        d7 = setw(d7, 0);                              // clr.w d7
        d7 = swap(d7);                                 // swap d7
        a3 = a3 + d7 * 8;                              // lea (a3,d7.l*8),a3  (vertical c.)

        // * CLIP OBJECT TO TOP AND BOTTOM
        d7 = 0;                                        // moveq #0,d7
        if ((short) d2 < (short) Mem.uw(draw_ObjClipT_w)) { // cmp.w draw_ObjClipT_w,d2 ; bge.s .object_fits_on_top
            d2 = setw(d2, d2 - Mem.uw(draw_ObjClipT_w)); // sub.w draw_ObjClipT_w,d2
            d4 = setw(d4, d4 + d2);                    // add.w d2,d4  (new height)
            if ((short) d4 <= 0) return;               // ble object_behind
            d7 = setw(d7, d2);                         // move.w d2,d7
            d7 = setw(d7, -(short) d7);                // neg.w d7
            d2 = setw(d2, Mem.uw(draw_ObjClipT_w));    // move.w draw_ObjClipT_w,d2
        }
        // .object_fits_on_top
        d6 = setw(0, Mem.uw(draw_ObjClipB_w));         // move.w draw_ObjClipB_w,d6
        d6 = setw(d6, d6 - d2);                        // sub.w d2,d6
        if ((short) d4 > (short) d6) d4 = setw(d4, d6); // cmp.w d6,d4 ; ble.s .object_fits_on_bottom ; move.w d6,d4
        // .object_fits_on_bottom
        d4 = setw(d4, d4 - 1);                         // subq #1,d4
        if ((short) d4 < 0) return;                    // blt object_behind

        d2 = Mem.l(ontoscr + (d2 & 0xFFFF) * 4);       // move.l #ontoscr,a6 ; move.l (a6,d2.w*4),d2
        d2 = d2 + Mem.l(Vid_FastBufferPtr_l);          // add.l Vid_FastBufferPtr_l,d2
        Mem.wl(toppt_l, d2);                           // move.l d2,toppt_l
        if ((short) d0 < (short) Mem.uw(draw_LeftClipB_w)) { // cmp.w draw_LeftClipB_w,d0 ; bge.s .ok_on_left
            d0 = setw(d0, d0 - Mem.uw(draw_LeftClipB_w)); // sub.w draw_LeftClipB_w,d0
            d3 = setw(d3, d3 + d0);                    // add.w d0,d3
            if ((short) d3 <= 0) return;               // ble object_behind
            d1 = setw(d1, Mem.uw(a2));                 // move.w (a2),d1
            d2 = setw(d2, Mem.uw(a2 + 2));             // move.w 2(a2),d2
            d0 = setw(d0, -(short) d0);                // neg.w d0
            d1 = muls(d1, d0);                         // muls d0,d1
            d2 = mulu(d2, d0);                         // mulu d0,d2
            d2 = swap(d2);                             // swap d2
            d1 = setw(d1, d1 + d2);                    // add.w d2,d1
            d0 = setw(d0, Mem.uw(draw_LeftClipB_w));   // move.w draw_LeftClipB_w,d0
            d1 = setw(d1, (d1 & 0xFFFF) << 2);         // asl.w #2,d1
            if (Mem.b(draw_FlipIt_b) != 0) {           // tst.b draw_FlipIt_b ; beq.s .no_flip_2
                a5 = a5 - (short) d1;                  // suba.w d1,a5
                a5 = a5 - (short) d1;                  // suba.w d1,a5
            }
            // .no_flip_2
            a5 = a5 + (short) d1;                      // adda.w d1,a5
        }
        // .ok_on_left
        d6 = setw(0, d0);                              // move.w d0,d6
        d6 = setw(d6, d6 + d3);                        // add.w d3,d6
        d6 = setw(d6, d6 - Mem.uw(draw_RightClipB_w)); // sub.w draw_RightClipB_w,d6
        if ((short) d6 >= 0) {                         // blt.s .ok_right_side
            d3 = setw(d3, d3 - 1);                     // sub.w #1,d3
            d3 = setw(d3, d3 - d6);                    // sub.w d6,d3
        }
        // .ok_right_side
        d0 = (short) d0;                               // ext.l d0
        Mem.wl(toppt_l, Mem.l(toppt_l) + d0);          // add.l d0,toppt_l
        d5 = setw(0, Mem.uw(a3));                      // move.w (a3),d5
        d6 = setw(0, Mem.uw(a3 + 2));                  // move.w 2(a3),d6
        d5 = muls(d5, d7);                             // muls d7,d5
        d6 = mulu(d6, d7);                             // mulu d7,d6
        d6 = swap(d6);                                 // swap d6
        d5 = setw(d5, d5 + d6);                        // add.w d6,d5
        d5 = setw(d5, d5 + Mem.uw(draw_DownStrip_w));  // add.w draw_DownStrip_w,d5
        d5 = d5 + 0x80000000;                          // add.l #$80000000,d5
        d7 = Mem.l(a2);                                // move.l (a2),d7
        if (Mem.b(draw_FlipIt_b) != 0) d7 = -d7;       // tst.b draw_FlipIt_b ; beq.s .no_flip_3 ; neg.l d7
        // .no_flip_3
        a2 = d7;                                       // move.l d7,a2  (fractional column offset)
        d7 = 0;                                        // moveq.l #0,d7
        Mem.wl(midobj_l, a5);                          // move.l a5,midobj_l
        d2 = Mem.l(a3);                                // move.l (a3),d2
        d2 = swap(d2);                                 // swap d2
        a1 = 0;                                        // move.l #0,a1
        if (Mem.b(draw_LightIt_b) != 0) {              // tst.b draw_LightIt_b ; bne draw_bitmap_lighted
            drawBitmapLighted(d2, d3, d4, d5, d7, a1, a2);
            return;
        }
        if (Mem.b(draw_Additive_b) != 0) {             // tst.b draw_Additive_b ; bne draw_bitmap_additive
            drawBitmapAdditive(d2, d3, d4, d5, d7, a1, a2);
            return;
        }
        if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_BITMAPS)) return; // DEV_CHECK_SET SKIP_BITMAPS,object_behind
        // DEV_INC.w VisibleBitmapCount
        Mem.ww(DevInst.dev_VisibleBitmapCount_w, Mem.uw(DevInst.dev_VisibleBitmapCount_w) + 1);

        // ---- draw_right_side (924) ----
        while (true) {
            d7 = swap(d7);                             // swap d7
            a5 = Mem.l(midobj_l);                      // move.l midobj_l,a5
            a5 = a5 + (short) d7 * 4;                  // lea (a5,d7.w*4),a5
            d7 = swap(d7);                             // swap d7
            d7 = d7 + a2;                              // add.l a2,d7
            a0 = Mem.l(draw_WADPtr_l);                 // move.l draw_WADPtr_l,a0
            a6 = Mem.l(toppt_l);                       // move.l toppt_l,a6
            a6 = a6 + (short) a1;                      // adda.w a1,a6
            a1 += 1;                                   // addq #1,a1
            d1 = Mem.l(a5);                            // move.l (a5),d1
            if (d1 != 0) {                             // beq .blank_strip
                d1 = d1 & 0xFFFFFF;                    // and.l #$ffffff,d1
                a0 = a0 + d1;                          // add.l d1,a0
                int fmt = (byte) Mem.ub(a5);           // move.b (a5),d1 ; cmp.b #1,d1
                int savedHeight = d4;                  // move.w d4,-(a7)
                d6 = d5;                               // move.l d5,d6
                d1 = d5;                               // move.l d5,d1
                while (true) {                         // .draw_vertical_strip_n
                    int tx;
                    if (fmt > 1) {                     // bgt .third_third  (fmt 2)
                        tx = (Mem.ub(a0 + (d1 & 0xFFFF) * 2) >>> 2) & 0x1F; // move.b (a0,d1.w*2),d0 ; lsr.b #2 ; and #%11111
                    } else if (fmt == 1) {             // beq .second_third
                        tx = (Mem.uw(a0 + (d1 & 0xFFFF) * 2) >>> 5) & 0x1F; // move.w (a0,d1.w*2),d0 ; lsr.w #5 ; and #%11111
                    } else {                           // first_third (fmt 0)
                        tx = Mem.ub(a0 + 1 + (d1 & 0xFFFF) * 2) & 0x1F;     // move.b 1(a0,d1.w*2),d0 ; and.b #%00011111
                    }
                    if (tx != 0) {                     // beq.s .skip_black
                        Mem.wb(a6, Mem.ub(a4 + (tx & 0xFFFF) * 2)); // move.b (a4,d0.w*2),(a6)
                    }
                    // .skip_black
                    a6 = a6 + Hires.SCREEN_WIDTH;      // adda.w #SCREEN_WIDTH,a6
                    long s = (d6 & 0xFFFFFFFFL) + (d2 & 0xFFFFFFFFL); // add.l d2,d6 (X = carry)
                    int x = (int) (s >>> 32);
                    d6 = (int) s;
                    d1 = setw(d1, (d1 & 0xFFFF) + (d2 & 0xFFFF) + x); // addx.w d2,d1
                    d4 = setw(d4, d4 - 1);             // dbra d4,.draw_vertical_strip_n
                    if ((short) d4 == -1) break;
                }
                d4 = savedHeight;                      // move.w (a7)+,d4
            }
            // .blank_strip
            d3 = setw(d3, d3 - 1);                     // dbra d3,draw_right_side
            if ((short) d3 == -1) break;
        }
        // object_behind: rts
    }

    // ==================================================================
    //  draw_bitmap_glare (objdrawhires.s:210) + draw_right_side_glare
    //  Entrées : a0 = objet, a1 = ObjRotated_vl.
    // ==================================================================
    static void drawBitmapGlare(int a0, int a1) {
        int d0, d1, d2, d3, d4, d5, d6, d7;
        int a2, a3, a4, a5, a6;

        if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_GLARE_BITMAPS)) return; // DEV_CHECK_SET SKIP_GLARE_BITMAPS,object_behind
        d0 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d0  (point number)
        d1 = setw(0, Mem.uw(a1 + (d0 & 0xFFFF) * 8 + 2)); // move.w 2(a1,d0.w*8),d1  (depth)
        if ((short) d1 <= DRAW_BITMAP_NEAR_PLANE) return; // cmp.w #DRAW_BITMAP_NEAR_PLANE,d1 ; ble object_behind
        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; beq.s .no_depth_adjust
            d1 = muls(d1, 927);                        // muls #927,d1
            d1 = d1 >> 8;                              // asr.l #8,d1
            d1 = d1 >> 2;                              // asr.l #2,d1
        }
        // .no_depth_adjust
        d2 = setw(0, Mem.uw(draw_TopClip_w));          // move.w draw_TopClip_w,d2
        d3 = setw(0, Mem.uw(draw_BottomClip_w));       // move.w draw_BottomClip_w,d3
        d6 = Mem.l(draw_TopY_3D_l);                    // move.l draw_TopY_3D_l,d6
        d6 = d6 - Mem.l(Plr_YOff_l);                   // sub.l Plr_YOff_l,d6
        d6 = divs(d6, d1);                             // divs d1,d6
        d6 = setw(d6, d6 + Mem.uw(Vid_CentreY_w));     // add.w Vid_CentreY_w,d6
        if ((short) d6 >= (short) d3) return;          // cmp.w d3,d6 ; bge object_behind
        if ((short) d6 < (short) d2) d6 = setw(d6, d2); // cmp.w d2,d6 ; bge.s .okobtc ; move.w d2,d6
        // .okobtc
        Mem.ww(draw_ObjClipT_w, d6);                   // move.w d6,draw_ObjClipT_w
        d6 = Mem.l(draw_BottomY_3D_l);                 // move.l draw_BottomY_3D_l,d6
        d6 = d6 - Mem.l(Plr_YOff_l);                   // sub.l Plr_YOff_l,d6
        d6 = divs(d6, d1);                             // divs d1,d6
        d6 = setw(d6, d6 + Mem.uw(Vid_CentreY_w));     // add.w Vid_CentreY_w,d6
        if ((short) d6 <= (short) d2) return;          // cmp.w d2,d6 ; ble object_behind
        if ((short) d6 > (short) d3) d6 = setw(d6, d3); // cmp.w d3,d6 ; ble.s .okobbc ; move.w d3,d6
        // .okobbc
        Mem.ww(draw_ObjClipB_w, d6);                   // move.w d6,draw_ObjClipB_w
        d0 = Mem.l(a1 + (d0 & 0xFFFF) * 8 + 4);        // move.l 4(a1,d0.w*8),d0
        d2 = setw(0, Mem.uw(draw_AuxX_w));             // move.w draw_AuxX_w,d2
        d2 = (short) d2;                               // ext.l d2
        d2 = d2 << 7;                                  // asl.l #7,d2
        d0 = d0 + d2;                                  // add.l d2,d0
        a0 += 2;                                        // addq #2,a0
        a4 = Mem.l(Draw_TexturePalettePtr_l);          // move.l Draw_TexturePalettePtr_l,a4
        a4 = a4 - 512;                                 // sub.l #512,a4
        d2 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d2  (height)
        d2 = setw(d2, d2 + Mem.uw(draw_AuxY_w));       // add.w draw_AuxY_w,d2
        d2 = (short) d2;                               // ext.l d2
        d2 = d2 << 7;                                  // asl.l #7,d2
        d2 = d2 - Mem.l(Plr_YOff_l);                   // sub.l Plr_YOff_l,d2
        d2 = divs(d2, d1);                             // divs d1,d2
        d2 = setw(d2, d2 + Mem.uw(Vid_CentreY_w));     // add.w Vid_CentreY_w,d2
        d0 = divs(d0, d1);                             // divs d1,d0
        d0 = setw(d0, d0 + Mem.uw(Vid_CentreX_w));     // add.w Vid_CentreX_w,d0

        a6 = Mem.l(GLF_DatabasePtr_l);                 // move.l GLF_DatabasePtr_l,a6
        a6 = a6 + GLFT_FrameData_l;                    // lea GLFT_FrameData_l(a6),a6
        a5 = Draw_ObjectPtrs_vl;                       // move.l #Draw_ObjectPtrs_vl,a5
        d7 = setw(0, Mem.uw(a0 + 2));                  // move.w 2(a0),d7
        d7 = setw(d7, -(short) d7);                    // neg.w d7
        d7 = setw(d7, (d7 & 0xFFFF) << 4);             // asl.w #4,d7
        a5 = a5 + (short) d7;                          // adda.w d7,a5
        d7 = setw(d7, (d7 & 0xFFFF) << 4);             // asl.w #4,d7
        a6 = a6 + (short) d7;                          // adda.w d7,a6
        d7 = setw(0, Mem.uw(a0 + 4));                  // move.w 4(a0),d7
        a6 = a6 + (d7 & 0xFFFF) * 8;                   // lea (a6,d7.w*8),a6
        a3 = ConstantTable_vl;                         // move.l #ConstantTable_vl,a3
        d3 = setb(0, Mem.ub(a0));
        a0 += 1;                                       // moveq #0,d3 ; move.b (a0)+,d3
        d4 = setb(0, Mem.ub(a0));
        a0 += 1;                                       // moveq #0,d4 ; move.b (a0)+,d4
        d3 = d3 << 7;                                  // lsl.l #7,d3
        d4 = d4 << 7;                                  // lsl.l #7,d4
        d3 = divs(d3, d1);                             // divs d1,d3
        d4 = divs(d4, d1);                             // divs d1,d4
        d2 = setw(d2, d2 - d4);                        // sub.w d4,d2
        d0 = setw(d0, d0 - d3);                        // sub.w d3,d0
        if ((short) d0 >= (short) Mem.uw(draw_RightClipB_w)) return; // cmp.w draw_RightClipB_w,d0 ; bge object_behind
        d3 = setw(d3, d3 + d3);                        // add.w d3,d3
        if ((short) d2 >= (short) Mem.uw(draw_ObjClipB_w)) return;   // cmp.w draw_ObjClipB_w,d2 ; bge object_behind
        d4 = setw(d4, d4 + d4);                        // add.w d4,d4

        Mem.wl(draw_WADPtr_l, Mem.l(a5));
        a5 += 4;                                       // move.l (a5)+,draw_WADPtr_l
        Mem.wl(draw_PtrPtr_l, Mem.l(a5));
        a5 += 4;                                       // move.l (a5)+,draw_PtrPtr_l
        d7 = Mem.l(a6);                                // move.l (a6),d7
        Mem.ww(draw_DownStrip_w, d7);                  // move.w d7,draw_DownStrip_w
        a5 = Mem.l(draw_PtrPtr_l);                     // move.l draw_PtrPtr_l,a5
        d7 = swap(d7);                                 // swap d7
        d7 = setw(d7, (d7 & 0xFFFF) << 2);             // asl.w #2,d7
        a5 = a5 + (short) d7;                          // adda.w d7,a5
        d7 = setw(d1, d1);                             // move.w d1,d7
        d6 = setw(0, Mem.uw(a6 + 4));                  // moveq #0,d6 ; move.w 4(a6),d6
        d6 = setw(d6, d6 + d6);                        // add.w d6,d6
        d6 = setw(d6, d6 - 1);                         // subq #1,d6
        d7 = mulu(d7, d6);                             // mulu d6,d7
        d6 = setb(0, Mem.ub(a0 - 2));                  // moveq #0,d6 ; move.b -2(a0),d6
        if ((byte) d6 == 0) return;                    // beq object_behind
        d7 = divu(d7, d6);                             // divu d6,d7
        d7 = swap(d7);                                 // swap d7
        d7 = setw(d7, 0);                              // clr.w d7
        d7 = swap(d7);                                 // swap d7
        a2 = a3 + d7 * 8;                              // lea (a3,d7.l*8),a2  (horiz const)
        d7 = setw(d1, d1);                             // move.w d1,d7
        d6 = setw(0, Mem.uw(a6 + 6));                  // move.w 6(a6),d6
        d6 = setw(d6, d6 + d6);                        // add.w d6,d6
        d6 = setw(d6, d6 - 1);                         // subq #1,d6
        d7 = mulu(d7, d6);                             // mulu d6,d7
        d6 = setb(0, Mem.ub(a0 - 1));                  // moveq #0,d6 ; move.b -1(a0),d6
        if ((byte) d6 == 0) return;                    // beq object_behind
        d7 = divu(d7, d6);                             // divu d6,d7
        d7 = swap(d7);                                 // swap d7
        d7 = setw(d7, 0);                              // clr.w d7
        d7 = swap(d7);                                 // swap d7
        a3 = a3 + d7 * 8;                              // lea (a3,d7.l*8),a3  (vert const)

        d7 = 0;                                        // moveq #0,d7
        if ((short) d2 < (short) Mem.uw(draw_ObjClipT_w)) { // cmp.w draw_ObjClipT_w,d2 ; bge.s .object_fits_on_top
            d2 = setw(d2, d2 - Mem.uw(draw_ObjClipT_w)); // sub.w draw_ObjClipT_w,d2
            d4 = setw(d4, d4 + d2);                    // add.w d2,d4
            if ((short) d4 <= 0) return;               // ble object_behind
            d7 = setw(d7, d2);                         // move.w d2,d7
            d7 = setw(d7, -(short) d7);                // neg.w d7
            d2 = setw(d2, Mem.uw(draw_ObjClipT_w));    // move.w draw_ObjClipT_w,d2
        }
        // .object_fits_on_top
        d6 = setw(0, Mem.uw(draw_ObjClipB_w));         // move.w draw_ObjClipB_w,d6
        d6 = setw(d6, d6 - d2);                        // sub.w d2,d6
        if ((short) d4 > (short) d6) d4 = setw(d4, d6); // cmp.w d6,d4 ; ble.s .object_fits_on_bottom ; move.w d6,d4
        // .object_fits_on_bottom
        d4 = setw(d4, d4 - 1);                         // subq #1,d4
        if ((short) d4 < 0) return;                    // blt object_behind
        d2 = Mem.l(ontoscr + (d2 & 0xFFFF) * 4);       // move.l #ontoscr,a6 ; move.l (a6,d2.w*4),d2
        d2 = d2 + Mem.l(Vid_FastBufferPtr_l);          // add.l Vid_FastBufferPtr_l,d2
        Mem.wl(toppt_l, d2);                           // move.l d2,toppt_l
        if ((short) d0 < (short) Mem.uw(draw_LeftClipB_w)) { // cmp.w draw_LeftClipB_w,d0 ; bge.s .ok_on_left
            d0 = setw(d0, d0 - Mem.uw(draw_LeftClipB_w)); // sub.w draw_LeftClipB_w,d0
            d3 = setw(d3, d3 + d0);                    // add.w d0,d3
            if ((short) d3 <= 0) return;               // ble object_behind
            d1 = setw(d1, Mem.uw(a2));                 // move.w (a2),d1
            d2 = setw(d2, Mem.uw(a2 + 2));             // move.w 2(a2),d2
            d0 = setw(d0, -(short) d0);                // neg.w d0
            d1 = muls(d1, d0);                         // muls d0,d1
            d2 = mulu(d2, d0);                         // mulu d0,d2
            d2 = swap(d2);                             // swap d2
            d1 = setw(d1, d1 + d2);                    // add.w d2,d1
            a5 = a5 + (short) d1 * 4;                  // lea (a5,d1.w*4),a5
            d0 = setw(d0, Mem.uw(draw_LeftClipB_w));   // move.w draw_LeftClipB_w,d0
        }
        // .ok_on_left
        d6 = setw(0, d0);                              // move.w d0,d6
        d6 = setw(d6, d6 + d3);                        // add.w d3,d6
        d6 = setw(d6, d6 - Mem.uw(draw_RightClipB_w)); // sub.w draw_RightClipB_w,d6
        if ((short) d6 >= 0) {                         // blt.s .ok_right_side
            d3 = setw(d3, d3 - 1);                     // sub.w #1,d3
            d3 = setw(d3, d3 - d6);                    // sub.w d6,d3
        }
        // .ok_right_side
        d0 = (short) d0;                               // ext.l d0
        Mem.wl(toppt_l, Mem.l(toppt_l) + d0);          // add.l d0,toppt_l
        d5 = setw(0, Mem.uw(a3));                      // move.w (a3),d5
        d6 = setw(0, Mem.uw(a3 + 2));                  // move.w 2(a3),d6
        d5 = muls(d5, d7);                             // muls d7,d5
        d6 = mulu(d6, d7);                             // mulu d7,d6
        d6 = swap(d6);                                 // swap d6
        d5 = setw(d5, d5 + d6);                        // add.w d6,d5
        d5 = setw(d5, d5 + Mem.uw(draw_DownStrip_w));  // add.w draw_DownStrip_w,d5
        d5 = d5 + 0x80000000;                          // add.l #$80000000,d5
        a2 = Mem.l(a2);                                // move.l (a2),a2  (fractional column step)
        d7 = 0;                                        // moveq.l #0,d7
        Mem.wl(midobj_l, a5);                          // move.l a5,midobj_l
        d2 = Mem.l(a3);                                // move.l (a3),d2
        d2 = swap(d2);                                 // swap d2
        a1 = 0;                                        // move.l #0,a1
        // DEV_INC.w VisibleGlareCount
        Mem.ww(DevInst.dev_VisibleGlareCount_w, Mem.uw(DevInst.dev_VisibleGlareCount_w) + 1);

        // ---- draw_right_side_glare (464) ----
        while (true) {
            d7 = swap(d7);                             // swap d7
            a5 = Mem.l(midobj_l);                      // move.l midobj_l,a5
            a5 = a5 + (short) d7 * 4;                  // lea (a5,d7.w*4),a5
            d7 = swap(d7);                             // swap d7
            d7 = d7 + a2;                              // add.l a2,d7
            a0 = Mem.l(draw_WADPtr_l);                 // move.l draw_WADPtr_l,a0
            a6 = Mem.l(toppt_l);                       // move.l toppt_l,a6
            a6 = a6 + (short) a1;                      // adda.w a1,a6
            a1 += 1;                                   // addq #1,a1
            d1 = Mem.l(a5);                            // move.l (a5),d1
            if (d1 != 0) {                             // beq .blank_strip
                d1 = d1 & 0xFFFFFF;                    // and.l #$ffffff,d1
                a0 = a0 + d1;                          // add.l d1,a0
                int fmt = (byte) Mem.ub(a5);           // move.b (a5),d1 ; cmp.b #1,d1
                int savedHeight = d4;                  // move.w d4,-(a7)
                d6 = d5;                               // move.l d5,d6
                d1 = d5;                               // move.l d5,d1
                while (true) {                         // .draw_vertical_strip_n
                    int tx;
                    if (fmt > 1) {                     // bgt .third_third (fmt 2)
                        tx = (Mem.ub(a0 + (d1 & 0xFFFF) * 2) >>> 2) & 0x1F; // move.b (a0,d1.w*2),d0 ; lsr.b #2 ; and #%11111
                    } else if (fmt == 1) {             // beq .second_third
                        tx = (Mem.uw(a0 + (d1 & 0xFFFF) * 2) >>> 5) & 0x1F; // move.w (a0,d1.w*2),d0 ; lsr.w #5 ; and #%11111
                    } else {                           // first
                        tx = Mem.ub(a0 + 1 + (d1 & 0xFFFF) * 2) & 0x1F;     // move.b 1(a0,d1.w*2),d0 ; and.b #%00011111
                    }
                    if (tx != 0) {                     // beq.s .skip_black
                        int dd = setw(0, (tx & 0xFFFF) << 8); // lsl.w #8,d0
                        dd = setw(dd, dd + dd);        // add.w d0,d0
                        dd = setb(dd, Mem.ub(a6));     // move.b (a6),d0
                        Mem.wb(a6, Mem.ub(a4 + (dd & 0xFFFF))); // move.b (a4,d0.w),(a6)
                    }
                    // .skip_black
                    a6 = a6 + Hires.SCREEN_WIDTH;      // adda.w #SCREEN_WIDTH,a6
                    long s = (d6 & 0xFFFFFFFFL) + (d2 & 0xFFFFFFFFL); // add.l d2,d6 (X=carry)
                    int x = (int) (s >>> 32);
                    d6 = (int) s;
                    d1 = setw(d1, (d1 & 0xFFFF) + (d2 & 0xFFFF) + x); // addx.w d2,d1
                    d4 = setw(d4, d4 - 1);             // dbra d4,...
                    if ((short) d4 == -1) break;
                }
                d4 = savedHeight;                      // move.w (a7)+,d4
            }
            // .blank_strip
            d3 = setw(d3, d3 - 1);                     // dbra d3,draw_right_side_glare
            if ((short) d3 == -1) break;
        }
        // object_behind
    }

    // ==================================================================
    //  draw_bitmap_additive (objdrawhires.s:1015) + draw_right_side_additive
    //  Blend additif : a4 = draw_BasePalPtr_l (32 jeux de 256 valeurs blend).
    // ==================================================================
    static void drawBitmapAdditive(int d2, int d3, int d4, int d5, int d7, int a1, int a2) {
        int d0, d1, d6;
        int a0, a4, a5, a6;
        if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_ADDITIVE_BITMAPS)) return; // DEV_CHECK_SET SKIP_ADDITIVE_BITMAPS,object_behind
        Mem.ww(DevInst.dev_VisibleAdditiveCount_w, Mem.uw(DevInst.dev_VisibleAdditiveCount_w) + 1); // DEV_INC.w VisibleAdditiveCount
        a4 = Mem.l(draw_BasePalPtr_l);                 // move.l draw_BasePalPtr_l,a4

        while (true) {                                 // draw_right_side_additive
            d7 = swap(d7);                             // swap d7
            a5 = Mem.l(midobj_l);                      // move.l midobj_l,a5
            a5 = a5 + (short) d7 * 4;                  // lea (a5,d7.w*4),a5
            d7 = swap(d7);                             // swap d7
            d7 = d7 + a2;                              // add.l a2,d7
            a0 = Mem.l(draw_WADPtr_l);                 // move.l draw_WADPtr_l,a0
            a6 = Mem.l(toppt_l);                       // move.l toppt_l,a6
            a6 = a6 + (short) a1;                      // adda.w a1,a6
            a1 += 1;                                   // addq #1,a1
            d1 = Mem.l(a5);                            // move.l (a5),d1
            if (d1 != 0) {                             // beq .blank_strip_additive
                d1 = d1 & 0xFFFFFF;                    // and.l #$ffffff,d1
                a0 = a0 + d1;                          // add.l d1,a0
                int fmt = (byte) Mem.ub(a5);           // move.b (a5),d1 ; cmp.b #1,d1
                int savedHeight = d4;                  // move.w d4,-(a7)
                d6 = d5;                               // move.l d5,d6
                d1 = d5;                               // move.l d5,d1
                while (true) {                         // .draw_vertical_strip_n
                    int tx;
                    if (fmt > 1) {                     // bgt .third_third_additive
                        tx = (Mem.ub(a0 + (d1 & 0xFFFF) * 2) >>> 2) & 0x1F; // move.b ; lsr.b #2 ; and #%11111
                    } else if (fmt == 1) {             // beq .second_third_additive
                        tx = (Mem.uw(a0 + (d1 & 0xFFFF) * 2) >>> 5) & 0x1F; // move.w ; lsr.w #5 ; and #%11111
                    } else {                           // first
                        tx = Mem.ub(a0 + 1 + (d1 & 0xFFFF) * 2) & 0x1F;     // move.b 1(a0,d1.w*2) ; and #%00011111
                    }
                    // additif : pas de skip-if-zero
                    int dd = setw(0, (tx & 0xFFFF) << 8); // lsl.w #8,d0
                    dd = setb(dd, Mem.ub(a6));         // move.b (a6),d0
                    Mem.wb(a6, Mem.ub(a4 + (dd & 0xFFFF))); // move.b (a4,d0.w),(a6)
                    a6 = a6 + Hires.SCREEN_WIDTH;      // adda.w #SCREEN_WIDTH,a6
                    long s = (d6 & 0xFFFFFFFFL) + (d2 & 0xFFFFFFFFL); // add.l d2,d6
                    int x = (int) (s >>> 32);
                    d6 = (int) s;
                    d1 = setw(d1, (d1 & 0xFFFF) + (d2 & 0xFFFF) + x); // addx.w d2,d1
                    d4 = setw(d4, d4 - 1);             // dbra d4,...
                    if ((short) d4 == -1) break;
                }
                d4 = savedHeight;                      // move.w (a7)+,d4
            }
            // .blank_strip_additive
            d3 = setw(d3, d3 - 1);                     // dbra d3,draw_right_side_additive
            if ((short) d3 == -1) break;
        }
        // object_behind
    }

    // ==================================================================
    //  draw_bitmap_lighted (objdrawhires.s:1107) — bitmaps « lightsourced ».
    //  Construit une palette dynamique draw_Pals_vl selon l'éclairage directionnel
    //  (anneaux de brightness + willy/willybright + guff), puis dessine.
    //  SAVEREGS/GETREGS de l'ASM préservent d2/d3/d4/d5/d7/a1/a2 pendant le calcul
    //  d'éclairage : ici ce sont des params, donc naturellement préservés.
    // ==================================================================
    static void drawBitmapLighted(int d2, int d3, int d4, int d5, int d7, int a1in, int a2in) {
        if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_LIGHTSOURCED_BITMAPS)) return; // DEV_CHECK_SET SKIP_LIGHTSOURCED_BITMAPS,object_behind
        Mem.ww(DevInst.dev_VisibleLightMapCount_w, Mem.uw(DevInst.dev_VisibleLightMapCount_w) + 1); // DEV_INC.w VisibleLightMapCount

        // SAVEREGS : d2/d3/d4/d5/d7 sont scratch dans le calcul d'éclairage,
        // restaurés par GETREGS avant la boucle de dessin.
        final int sd2 = d2, sd3 = d3, sd4 = d4, sd5 = d5, sd7 = d7;

        // --- calcul d'éclairage (SAVEREGS) ---
        draw_ResetAngleBrights();                      // bsr draw_ResetAngleBrights

        int a0 = draw_XZAngs_vw;                       // move.l #draw_XZAngs_vw,a0
        int a1 = draw_AngleBrights_vl;                 // move.l #draw_AngleBrights_vl,a1
        int d7l = setw(0, 15);                         // move.w #15,d7
        int a2 = 0, a3 = 0, a4 = 0, a5 = 0;            // sub.l a2..a5
        int d0 = 0, d1 = 0;                            // moveq #0,d0 ; moveq #0,d1
        while (true) {                                 // average_angle
            int d4l = setb(0, Mem.ub(a1 + 16));        // moveq #0,d4 ; move.b 16(a1),d4
            if ((byte) d4l != (byte) 0x80) {           // cmp.b #$80,d4 ; beq .nobright
                d4l = setw(d4l, -(short) d4l);         // neg.w d4
                d4l = setw(d4l, d4l + 48);             // add.w #48,d4
                if ((byte) d4l > (byte) d1) d1 = setb(d1, d4l); // cmp.b d1,d4 ; ble .no_brightest ; move.b d4,d1
                int d5l = setw(0, Mem.uw(a0));         // move.w (a0),d5
                int d6l = setw(0, Mem.uw(a0 + 2));     // move.w 2(a0),d6
                d5l = muls(d5l, d4l);                  // muls d4,d5
                d6l = muls(d6l, d4l);                  // muls d4,d6
                a2 = a2 + d5l;                         // add.l d5,a2
                a3 = a3 + d6l;                         // add.l d6,a3
            }
            // .nobright / BOTTYL
            int d4b = setb(0, Mem.ub(a1));             // moveq #0,d4 ; move.b (a1),d4
            if ((byte) d4b != (byte) 0x80) {           // cmp.b #$80,d4 ; beq .nobright
                d4b = setw(d4b, -(short) d4b);         // neg.w d4
                d4b = setw(d4b, d4b + 48);             // add.w #48,d4
                if ((byte) d4b >= (byte) d0) d0 = setb(d0, d4b); // cmp.b d0,d4 ; blt .no_brightest ; move.b d4,d0
                int d5l = setw(0, Mem.uw(a0));         // move.w (a0),d5
                int d6l = setw(0, Mem.uw(a0 + 2));     // move.w 2(a0),d6
                d5l = muls(d5l, d4b);                  // muls d4,d5
                d6l = muls(d6l, d4b);                  // muls d4,d6
                a4 = a4 + d5l;                         // add.l d5,a4
                a5 = a5 + d6l;                         // add.l d6,a5
            }
            // .nobright
            a0 += 4;                                   // addq #4,a0
            a1 += 1;                                   // addq #1,a1
            d7l = setw(d7l, d7l - 1);                  // dbra d7,average_angle
            if ((short) d7l == -1) break;
        }

        d2 = a2;                                       // move.l a2,d2
        d3 = a3;                                       // move.l a3,d3
        d4 = a4;                                       // move.l a4,d4
        d5 = a5;                                       // move.l a5,d5
        d4 = d4 + d2;                                  // add.l d2,d4
        d5 = d5 + d3;                                  // add.l d3,d5  (bright dir)
        d4 = draw_FindRoughAngle(d4, d5);              // bsr draw_FindRoughAngle  (renvoie angle dans d4)

        // foundang
        d2 = setw(0, 7);                               // move.w #7,d2
        d3 = setw(0, d1);                              // move.w d1,d3
        if ((short) d1 != (short) d0) {                // cmp.w d0,d1 ; beq INMIDDLE
            if (!((short) d1 > (short) d0)) {          // bgt.s .okpicked
                d3 = setw(d3, d0);                     // move.w d0,d3
            }
            // .okpicked
            d2 = setw(d2, d0);                         // move.w d0,d2
            d2 = setw(d2, d2 + d1);                    // add.w d1,d2  (total brightness)
            d1 = setw(d1, (d1 & 0xFFFF) << 4);         // asl.w #4,d1
            d1 = setw(d1, d1 - 1);                     // subq #1,d1
            d1 = divs(d1, d2);                         // divs d2,d1
            d2 = setw(d2, d1);                         // move.w d1,d2
        }
        // INMIDDLE — d2=y dist du centre du pt le + brillant ; d3=brightness
        d3 = setw(d3, -(short) d3);                    // neg.w d3
        d3 = setw(d3, d3 + 48);                        // add.w #48,d3
        a0 = willy;                                    // move.l #willy,a0
        a1 = guff;                                     // move.l #guff,a1
        a1 = a1 + Mem.l(draw_TempPtr_l);               // add.l draw_TempPtr_l,a1
        d2 = muls(d2, 7 * 16);                         // muls #7*16,d2
        a1 = a1 + d2;                                  // add.l d2,a1
        d0 = setw(0, Mem.uw(Plr1_TmpAngPos_w));        // move.w Plr1_TmpAngPos_w,d0
        d0 = setw(d0, -(short) d0);                    // neg.w d0
        d0 = setw(d0, d0 + SINE_SIZE);                 // add.w #SINE_SIZE,d0
        d0 = setw(d0, d0 & (SINE_SIZE * 2 - 1));       // AMOD_I d0
        d0 = setw(d0, ((short) d0) >> 8);              // asr.w #8,d0
        d0 = setw(d0, ((short) d0) >> 1);              // asr.w #1,d0
        d0 = setb(d0, d0 - 3);                         // sub.b #3,d0
        d0 = setb(d0, d0 + d4);                        // add.b d4,d0
        d0 = setw(d0, d0 & 15);                        // and.w #15,d0
        d1 = setw(0, 6);                               // move.w #6,d1
        while (true) {                                 // .across_loop
            d2 = setw(0, 6);                           // move.w #6,d2
            d5 = setw(0, d0);                          // move.w d0,d5
            while (true) {                             // .down_loop
                d4 = setb(0, Mem.ub(a1 + (d5 & 0xFFFF))); // move.b (a1,d5),d4
                d4 = setb(d4, d4 + d3);                // add.b d3,d4
                d4 = (short) (byte) d4;                // ext.w d4
                Mem.ww(a0, d4);
                a0 += 2;                               // move.w d4,(a0)+
                d5 = setw(d5, d5 + 1);                 // addq #1,d5
                d5 = setw(d5, d5 & 15);                // and.w #15,d5
                d2 = setw(d2, d2 - 1);                 // dbra d2,.down_loop
                if ((short) d2 == -1) break;
            }
            a1 = a1 + 16;                              // add.w #16,a1
            d1 = setw(d1, d1 - 1);                     // dbra d1,.across_loop
            if ((short) d1 == -1) break;
        }

        d0 = setw(0, Mem.uw(draw_BrightToAdd_w));      // move.w draw_BrightToAdd_w,d0
        a0 = willy;                                    // move.l #willy,a0
        a1 = willybright;                              // move.l #willybright,a1
        d1 = setw(0, 48);                              // move.w #48,d1
        while (true) {                                 // .add_it_in
            d2 = setw(0, d0);                          // move.w d0,d2
            d2 = setw(d2, d2 + Mem.uw(a1));
            a1 += 2;                                   // add.w (a1)+,d2
            if ((short) d2 <= 0) d2 = 0;               // ble.s .nopos ; moveq #0,d2
            // .nopos
            Mem.ww(a0, Mem.uw(a0) + d2);
            a0 += 2;                                   // add.w d2,(a0)+
            d1 = setw(d1, d1 - 1);                     // dbra d1,.add_it_in
            if ((short) d1 == -1) break;
        }

        if (Mem.b(draw_FlipIt_b) != 0) {               // tst.b draw_FlipIt_b ; beq.s .left_or_right
            a0 = draw_Brights2_vw;                     // move.l #draw_Brights2_vw,a0
        } else {
            a0 = draw_Brights_vw;                      // move.l #draw_Brights_vw,a0
        }
        // .done_right_to_left
        a2 = willy;                                    // move.l #willy,a2
        a1 = Mem.l(draw_BasePalPtr_l);                 // move.l draw_BasePalPtr_l,a1
        d0 = setb(0, Mem.ub(draw_WhichLightPal_b));    // move.b draw_WhichLightPal_b,d0
        d0 = setw(d0, (d0 & 0xFFFF) << 8);             // asl.w #8,d0
        a1 = a1 + (short) d0;                          // add.w d0,a1
        a3 = draw_Pals_vl;                             // move.l #draw_Pals_vl,a3
        d0 = setw(0, 28);                              // move.w #28,d0
        while (true) {                                 // .make_pals_loop
            d1 = setw(0, Mem.uw(a0));
            a0 += 2;                                   // move.w (a0)+,d1
            d1 = setw(d1, Mem.uw(a2 + (d1 & 0xFFFF) * 2)); // move.w (a2,d1.w*2),d1
            if ((short) d1 < 0) d1 = 0;                // bge.s .okpos ; moveq #0,d1
            // .okpos
            if ((short) d1 >= 31) d1 = setw(d1, 31);   // cmp.w #31,d1 ; blt.s .okneg ; move.w #31,d1
            // .okneg
            Mem.wl(a3, Mem.l(a1 + (d1 & 0xFFFF) * 8));
            a3 += 4;                                   // move.l (a1,d1.w*8),(a3)+
            Mem.wb(a3 - 4, 0);                         // move.b #0,-4(a3)
            Mem.wl(a3, Mem.l(a1 + (d1 & 0xFFFF) * 8 + 4));
            a3 += 4;                                   // move.l 4(a1,d1.w*8),(a3)+
            d0 = setw(d0, d0 - 1);                     // dbra d0,.make_pals_loop
            if ((short) d0 == -1) break;
        }
        // --- GETREGS : restaure d2/d3/d4/d5/d7 (et a1/a2 = a1in/a2in) ---
        d2 = sd2;
        d3 = sd3;
        d4 = sd4;
        d5 = sd5;
        d7 = sd7;

        a4 = draw_Pals_vl;                             // move.l #draw_Pals_vl,a4
        // clr.w d0  (inutilisé ensuite)
        int a1c = a1in;                                // a1 (compteur de colonne) restauré
        int a2c = a2in;                                // a2 (pas fractionnaire) restauré
        while (true) {                                 // .draw_light_loop
            d7 = swap(d7);                             // swap d7
            a5 = Mem.l(midobj_l);                      // move.l midobj_l,a5
            a5 = a5 + (short) d7 * 4;                  // lea (a5,d7.w*4),a5
            d7 = swap(d7);                             // swap d7
            d7 = d7 + a2c;                             // add.l a2,d7
            a0 = Mem.l(draw_WADPtr_l);                 // move.l draw_WADPtr_l,a0
            int a6 = Mem.l(toppt_l);                   // move.l toppt_l,a6
            a6 = a6 + (short) a1c;                     // adda.w a1,a6
            a1c += 1;                                  // addq #1,a1
            d1 = Mem.l(a5);                            // move.l (a5),d1
            if (d1 != 0) {                             // beq .blank_strip
                a0 = a0 + d1;                          // add.l d1,a0
                int savedHeight = d4;                  // move.w d4,-(a7)
                int d6 = d5;                           // move.l d5,d6
                d1 = d5;                               // move.l d5,d1
                while (true) {                         // .draw_vertical_strip
                    int dd = setb(0, Mem.ub(a0 + (d1 & 0xFFFF))); // move.b (a0,d1.w),d0
                    if ((byte) dd != 0) {              // beq.s .skip_black
                        Mem.wb(a6, Mem.ub(a4 + (dd & 0xFFFF))); // move.b (a4,d0.w),(a6)
                    }
                    // .skip_black
                    a6 = a6 + Hires.SCREEN_WIDTH;      // adda.w #SCREEN_WIDTH,a6
                    long s = (d6 & 0xFFFFFFFFL) + (d2 & 0xFFFFFFFFL); // add.l d2,d6
                    int x = (int) (s >>> 32);
                    d6 = (int) s;
                    d1 = setw(d1, (d1 & 0xFFFF) + (d2 & 0xFFFF) + x); // addx.w d2,d1
                    d4 = setw(d4, d4 - 1);             // dbra d4,.draw_vertical_strip
                    if ((short) d4 == -1) break;
                }
                d4 = savedHeight;                      // move.w (a7)+,d4
            }
            // .blank_strip
            d3 = setw(d3, d3 - 1);                     // dbra d3,.draw_light_loop
            if ((short) d3 == -1) break;
        }
        // object_behind
    }

    // ==================================================================
    //  draw_PolygonModel (objdrawhires.s:1685) — modèles vectoriels.
    //  Entrées : a0 = objet, a1 = ObjRotated_vl, d0 = pt num, d1 = z.
    //  NB : les divs.l de projection/edge-walk d'origine sont conservées exactes ;
    //  les fillers polygone utilisent OneOverN_vw + MUL_INV (code 0xABADCAFE actif).
    // ==================================================================
    public static boolean dbgTex;            // DIAG
    public static int dbgTexModel = -1;      // DIAG : modèle à tracer
    public static int dbgForceFrame = -1;    // DIAG : force la frame du modèle dbgTexModel
    public static boolean dbgCheesy = false; // TEST : atlas objet 1 octet/texel (CHEESEY=1) au lieu de 4
    static int dbgCurModel;                  // DIAG : modèle en cours
    public static final java.util.List<String> dbgTexLog = new java.util.ArrayList<>(); // DIAG

    static void draw_PolygonModel(int a0, int a1, int d0, int d1) {
        int d2, d3, d4, d5, d6, d7;
        int a2, a3, a4, a5, a6;

        Mem.ww(draw_ObjectAng_w, Mem.uw(a0 + EntT_CurrentAngle_w)); // move.w EntT_CurrentAngle_w(a0),draw_ObjectAng_w
        Mem.ww(draw_PolygonCentreY_w, Mem.uw(Vid_CentreY_w));       // move.w Vid_CentreY_w,draw_PolygonCentreY_w
        d0 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d0  (object Id)
        a4 = Mem.l(Lvl_ObjectPointsPtr_l);             // move.l Lvl_ObjectPointsPtr_l,a4
        Mem.ww(draw_Obj_XPos_w, Mem.uw(a4 + (d0 & 0xFFFF) * 8));     // move.w (a4,d0.w*8),draw_Obj_XPos_w
        Mem.ww(draw_Obj_ZPos_w, Mem.uw(a4 + (d0 & 0xFFFF) * 8 + 4)); // move.w 4(a4,d0.w*8),draw_Obj_ZPos_w
        d1 = setw(d1, Mem.uw(a1 + (d0 & 0xFFFF) * 8 + 2)); // move.w 2(a1,d0.w*8),d1  (zpos of mid)
        if ((short) d1 < 0) return;                    // blt polybehind
        if ((short) d1 == 0) {                         // bgt.s .okinfront ; else (==0) :
            a3 = a0;                                   // move.l a0,a3
            a3 = a3 - Mem.l(Plr1_ObjectPtr_l);         // sub.l Plr1_ObjectPtr_l,a3
            if (a3 != DRAW_VECTOR_NEAR_PLANE) return;  // cmp.l #DRAW_VECTOR_NEAR_PLANE,a3 ; bne polybehind
            if (Mem.b(draw_WhichDoing_b) != 0) return; // tst.b draw_WhichDoing_b ; bne polybehind
            d1 = setw(d1, 1);                          // move.w #1,d1
            Mem.ww(draw_PolygonCentreY_w, Hires.SMALL_HEIGHT / 2); // move.w #SMALL_HEIGHT/2,draw_PolygonCentreY_w
            if (Mem.b(Vid_FullScreen_b) != 0) {        // tst.b Vid_FullScreen_b ; beq.s .okinfront
                Mem.ww(draw_PolygonCentreY_w, Hires.FS_HEIGHT / 2); // move.w #FS_HEIGHT/2,draw_PolygonCentreY_w
            }
        }
        // .okinfront — SAVEREGS (a0/d0/d1 préservés : params/locals)
        final int sa0 = a0, sd0 = d0, sd1 = d1;
        Mem.ww(DevInst.dev_VisibleModelCount_w, Mem.uw(DevInst.dev_VisibleModelCount_w) + 1); // DEV_INC.w VisibleModelCount
        draw_CalcBrightRings();                        // jsr draw_CalcBrightRings

        a0 = draw_AngleBrights_vl;                      // move.l #draw_AngleBrights_vl,a0
        a1 = draw_PointAndPolyBrights_vl;               // move.l #draw_PointAndPolyBrights_vl,a1
        d7 = setw(0, 15);                               // move.w #15,d7
        d6 = setw(0, 8);                                // move.w #8,d6
        while (true) {                                  // MYacross
            // (centre)
            d3 = 0;
            d4 = setb(0, Mem.ub(a0 + 16 + (d6 & 0xFFFF))); // move.b 16(a0,d6.w),d4
            if ((byte) d4 < 0) d4 = 0;                  // bge.s .okp2 ; moveq #0,d4
            d3 = setb(0, Mem.ub(a0 + (d6 & 0xFFFF)));   // move.b (a0,d6.w),d3
            if ((byte) d3 < 0) d3 = 0;                  // bge.s .okp1 ; moveq #0,d3
            d4 = setw(d4, d4 - d3);                     // sub.w d3,d4
            d3 = swap(d3);                              // swap d3
            d4 = swap(d4);                              // swap d4
            d4 = d4 >> 3;                               // asr.l #3,d4
            d2 = setw(0, 7);                            // moveq #7,d2
            d5 = 3 * 16;                                // moveq #3*16,d5
            while (true) {                              // .down
                d3 = swap(d3);
                Mem.wb(a1 + (d5 & 0xFFFF), d3);         // move.b d3,(a1,d5.w)
                d3 = swap(d3);
                d5 = setw(d5, d5 + 16);                 // add.w #16,d5
                d3 = d3 + d4;                           // add.l d4,d3
                d2 = setw(d2, d2 - 1);
                if ((short) d2 == -1) break;            // dbra d2,.down
            }
            // TOPPART
            d3 = 0;
            d4 = 0;
            d6 = d6 ^ 8;                                // bchg #3,d6
            d4 = setb(0, Mem.ub(a0 + (d6 & 0xFFFF)));   // move.b (a0,d6.w),d4
            if ((byte) d4 < 0) d4 = 0;                  // bge .okp2 ; moveq #0,d4
            d6 = d6 ^ 8;                                // bchg #3,d6
            d3 = setb(0, Mem.ub(a0 + (d6 & 0xFFFF)));   // move.b (a0,d6.w),d3
            if ((byte) d3 < 0) d3 = 0;                  // bge .okp1 ; moveq #0,d3
            d4 = setw(d4, d4 - d3);                     // sub.w d3,d4
            d3 = swap(d3);
            d4 = swap(d4);
            d4 = d4 >> 4;                               // asr.l #4,d4
            d2 = setw(0, 3);                            // moveq #3,d2
            d5 = 3 * 16;                                // moveq #3*16,d5
            while (true) {                              // .down
                d3 = swap(d3);
                Mem.wb(a1 + (d5 & 0xFFFF), d3);
                d3 = swap(d3);
                d5 = setw(d5, d5 - 16);                 // sub.w #16,d5
                d3 = d3 + d4;
                d2 = setw(d2, d2 - 1);
                if ((short) d2 == -1) break;
            }
            // BOTPART
            d3 = 0;
            d4 = 0;
            d6 = d6 ^ 8;                                // bchg #3,d6
            d4 = setb(0, Mem.ub(a0 + 16 + (d6 & 0xFFFF))); // move.b 16(a0,d6.w),d4
            if ((byte) d4 < 0) d4 = 0;                  // bge .okp2 ; moveq #0,d4
            d6 = d6 ^ 8;                                // bchg #3,d6
            d3 = setb(0, Mem.ub(a0 + 16 + (d6 & 0xFFFF))); // move.b 16(a0,d6.w),d3
            if ((byte) d3 < 0) d3 = 0;                  // bge .okp1 ; moveq #0,d3
            d4 = setw(d4, d4 - d3);                     // sub.w d3,d4
            d3 = swap(d3);
            d4 = swap(d4);
            d4 = d4 >> 4;                               // asr.l #4,d4
            d2 = setw(0, 3);                            // moveq #3,d2
            d5 = setw(0, 11 * 16);                      // move.w #11*16,d5
            while (true) {                              // .down
                d3 = swap(d3);
                Mem.wb(a1 + (d5 & 0xFFFF), d3);
                d3 = swap(d3);
                d5 = setw(d5, d5 + 16);                 // add.w #16,d5
                d3 = d3 + d4;
                d2 = setw(d2, d2 - 1);
                if ((short) d2 == -1) break;
            }
            d6 = setw(d6, d6 - 1);                      // subq #1,d6
            d6 = setw(d6, d6 & 0xF);                    // and.w #$f,d6
            a1 += 1;                                    // addq #1,a1
            d7 = setw(d7, d7 - 1);                      // dbra d7,MYacross
            if ((short) d7 == -1) break;
        }
        // GETREGS — restaure a0 (objet), d0 (objId), d1 (z)
        a0 = sa0;
        d0 = sd0;
        d1 = sd1;

        d2 = setw(0, Mem.uw(a0));                       // move.w (a0),d2
        d3 = setw(0, d1);                               // move.w d1,d3
        d3 = setw(d3, ((short) d3) >> 7);               // asr.w #7,d3
        d2 = setw(d2, d2 + d3);                         // add.w d3,d2
        Mem.ww(draw_ObjectBright_w, d2);                // move.w d2,draw_ObjectBright_w
        d2 = setw(0, Mem.uw(draw_TopClip_w));           // move.w draw_TopClip_w,d2
        d3 = setw(0, Mem.uw(draw_BottomClip_w));        // move.w draw_BottomClip_w,d3
        Mem.ww(draw_ObjClipT_w, d2);                    // move.w d2,draw_ObjClipT_w
        Mem.ww(draw_ObjClipB_w, d3);                    // move.w d3,draw_ObjClipB_w

        d5 = setw(0, Mem.uw(a0 + 6));                   // move.w 6(a0),d5
        dbgCurModel = d5 & 0xFFFF;                      // DIAG
        a3 = Draw_PolyObjects_vl;                       // move.l #Draw_PolyObjects_vl,a3
        a3 = Mem.l(a3 + (d5 & 0xFFFF) * 4);             // move.l (a3,d5.w*4),a3
        Mem.ww(draw_SortIt_w, Mem.uw(a3));
        a3 += 2;                                        // move.w (a3)+,draw_SortIt_w
        Mem.wl(draw_StartOfObjPtr_l, a3);               // move.l a3,draw_StartOfObjPtr_l
        Mem.ww(draw_NumPoints_w, Mem.uw(a3));
        a3 += 2;                                        // move.w (a3)+,draw_NumPoints_w
        d6 = setw(0, Mem.uw(a3));
        a3 += 2;                                        // move.w (a3)+,d6  (num_frames)
        Mem.wl(draw_PointerTablePtr_l, a3);             // move.l a3,draw_PointerTablePtr_l
        a3 = a3 + (d6 & 0xFFFF) * 4;                    // lea (a3,d6.w*4),a3
        Mem.wl(LinesPtr, a3);                           // move.l a3,LinesPtr
        d5 = setw(0, Mem.uw(a0 + 8));                   // moveq #0,d5 ; move.w 8(a0),d5
        if (dbgForceFrame >= 0 && dbgCurModel == dbgTexModel) d5 = setw(0, dbgForceFrame); // TEST : force la frame
        d2 = 0;                                         // moveq #0,d2
        a4 = Mem.l(draw_PointerTablePtr_l);             // move.l draw_PointerTablePtr_l,a4
        d2 = setw(d2, Mem.uw(a4 + (d5 & 0xFFFF) * 4));  // move.w (a4,d5.w*4),d2
        d2 = d2 + Mem.l(draw_StartOfObjPtr_l);          // add.l draw_StartOfObjPtr_l,d2
        Mem.wl(PtsPtr, d2);                             // move.l d2,PtsPtr
        d5 = setw(d5, Mem.uw(a4 + (d5 & 0xFFFF) * 4 + 2)); // move.w 2(a4,d5.w*4),d5
        d5 = d5 + Mem.l(draw_StartOfObjPtr_l);          // add.l draw_StartOfObjPtr_l,d5
        Mem.wl(draw_PolyAngPtr_l, d5);                  // move.l d5,draw_PolyAngPtr_l
        a3 = d2;                                        // move.l d2,a3
        d5 = setw(0, Mem.uw(draw_NumPoints_w));         // move.w draw_NumPoints_w,d5
        if (dbgTex && dbgCurModel == dbgTexModel && dbgTexLog.size() < 30) // DIAG
            dbgTexLog.add("frame=" + Mem.uw(a0 + 8) + " ObjectOnOff=0x" + Integer.toHexString(Mem.l(a3)) + " PtsOff=" + (d2 - Mem.l(draw_StartOfObjPtr_l)));
        Mem.wl(draw_ObjectOnOff_l, Mem.l(a3));
        a3 += 4;                                        // move.l (a3)+,draw_ObjectOnOff_l
        Mem.wl(draw_PointAngPtr_l, a3);                 // move.l a3,draw_PointAngPtr_l
        d2 = setw(d2, d5);                              // move.w d5,d2
        d3 = 0;                                         // moveq #0,d3
        {                                               // lsr.w #1,d2 ; addx.w d3,d2
            int lo = (d2 & 0xFFFF);
            int x = lo & 1;                             // bit shifté → X
            d2 = setw(d2, lo >>> 1);
            d2 = setw(d2, (d2 & 0xFFFF) + (d3 & 0xFFFF) + x); // addx.w d3,d2
        }
        d2 = setw(d2, d2 + d2);                         // add.w d2,d2
        a3 = a3 + (short) d2;                           // add.w d2,a3
        d5 = setw(d5, d5 - 1);                          // subq #1,d5
        a4 = draw_3DPointsRotated_vl;                   // move.l #draw_3DPointsRotated_vl,a4
        d2 = setw(0, Mem.uw(draw_ObjectAng_w));         // move.w draw_ObjectAng_w,d2
        d2 = setw(d2, d2 - 2048);                       // sub.w #2048,d2
        d2 = setw(d2, d2 - Mem.uw(Vis_AngPos_w));       // sub.w Vis_AngPos_w,d2
        d2 = setw(d2, d2 & (SINE_SIZE * 2 - 1));        // AMOD_I d2
        a5 = SinCosTable_vw + (d2 & 0xFFFF);            // move.l #SinCosTable_vw,a2 ; lea (a2,d2.w),a5
        a6 = boxbrights_vw;                             // move.l #boxbrights_vw,a6
        d6 = setw(0, Mem.uw(a5));                       // move.w (a5),d6  (sine)
        d7 = setw(0, Mem.uw(a5 + COSINE_OFS));          // move.w COSINE_OFS(a5),d7  (cosine)

        while (true) {                                  // rotate_object
            d2 = setw(0, Mem.uw(a3));                   // move.w (a3),d2   (xpt)
            d3 = setw(0, Mem.uw(a3 + 2));               // move.w 2(a3),d3  (ypt)
            d4 = setw(0, Mem.uw(a3 + 4));               // move.w 4(a3),d4  (zpt)
            d4 = muls(d4, d7);                          // muls d7,d4  (z*cos)
            d2 = muls(d2, d6);                          // muls d6,d2  (x*sin)
            d2 = d2 - d4;                               // sub.l d4,d2
            d2 = d2 >> 8;                               // asr.l #8,d2
            d2 = d2 >> 1;                               // asr.l #1,d2
            Mem.wl(a4, d2);
            a4 += 4;                                    // move.l d2,(a4)+  (x')
            d3 = (short) d3;                            // ext.l d3
            d3 = d3 << 6;                               // asl.l #6,d3
            Mem.wl(a4, d3);
            a4 += 4;                                    // move.l d3,(a4)+  (y')
            d2 = setw(0, Mem.uw(a3));                   // move.w (a3),d2
            d4 = setw(0, Mem.uw(a3 + 4));               // move.w 4(a3),d4
            d4 = muls(d4, d6);                          // muls d6,d4  (z*sin)
            d2 = muls(d2, d7);                          // muls d7,d2  (x*cos)
            d4 = d4 + d2;                               // add.l d2,d4
            d4 = swap(d4);                              // swap d4
            Mem.ww(a4, d4);
            a4 += 2;                                    // move.w d4,(a4)+  (z')
            a3 += 6;                                    // addq #6,a3
            d5 = setw(d5, d5 - 1);                      // dbra d5,rotate_object
            if ((short) d5 == -1) break;
        }

        // move.l 4(a1,d0.w*8),d0 — l'ASM lit le centre objet depuis ObjRotated (a1 y a été restauré
        // par GETREGS, objdrawhires.s:1824). Mon port avait clobbé a1 en draw_PointAndPolyBrights_vl
        // (brightness, ~1318) sans le restaurer → on relit donc depuis ObjRotated_vl. (a1 est de
        // toute façon réassigné à LinesPtr juste après, donc seul ce point dépendait d'ObjRotated.)
        d0 = Mem.l(ObjRotated_vl + (d0 & 0xFFFF) * 8 + 4);
        d7 = setw(0, Mem.uw(draw_NumPoints_w));         // move.w draw_NumPoints_w,d7
        a2 = draw_3DPointsRotated_vl;                   // move.l #draw_3DPointsRotated_vl,a2
        a3 = draw_2DPointsProjected_vl;                 // move.l #draw_2DPointsProjected_vl,a3
        a6 = boxbrights_vw;                             // move.l #boxbrights_vw,a6
        d2 = setw(0, Mem.uw(a0 + 2));                   // move.w 2(a0),d2  (object y pos)
        d7 = setw(d7, d7 - 1);                          // subq #1,d7
        d0 = d0 + d0;                                   // add.l d0,d0  (*2)

        boolean fullscreen = Mem.b(Vid_FullScreen_b) != 0; // tst.b Vid_FullScreen_b ; beq smallscreen_conv
        if (fullscreen) {
            d3 = setw(0, d1);                           // move.w d1,d3
            d1 = setw(d1, d1 + d1);                     // add.w d1,d1
            d1 = setw(d1, d1 + d3);                     // add.w d3,d1  (d1*3)
            d2 = (short) d2;                            // ext.l d2
            d2 = d2 << 7;                               // asl.l #7,d2
            d2 = d2 - Mem.l(Plr_YOff_l);                // sub.l Plr_YOff_l,d2
            d2 = d2 + d2;                               // add.l d2,d2
        } else {
            d1 = setw(d1, d1 + d1);                     // add.w d1,d1  (d1*2)
            d2 = (short) d2;                            // ext.l d2
            d2 = d2 << 7;                               // asl.l #7,d2
            d2 = d2 - Mem.l(Plr_YOff_l);                // sub.l Plr_YOff_l,d2
            d2 = d2 + d2;                               // add.l d2,d2
        }

        boolean tooFar = false;                         // flag « bra no_more_parts »
        while (true) {                                  // .convert_to_screen
            d3 = Mem.l(a2);                             // move.l (a2),d3
            d3 = d3 + d0;                               // add.l d0,d3
            Mem.wl(a2, d3);
            a2 += 4;                                    // move.l d3,(a2)+
            d4 = Mem.l(a2);                             // move.l (a2),d4
            d4 = d4 + d2;                               // add.l d2,d4
            Mem.wl(a2, d4);
            a2 += 4;                                    // move.l d4,(a2)+
            d5 = setw(0, Mem.uw(a2));                   // move.w (a2),d5
            d5 = setw(d5, d5 + d1);                     // add.w d1,d5
            if ((short) d5 <= 0) {                      // ble .point_behind
                Mem.ww(a2, d5);
                a2 += 2;                                // move.w d5,(a2)+
                Mem.wl(a3, 0x7fff7fff);
                a3 += 4;                                // move.l #$7fff7fff,(a3)+
            } else {
                if ((short) d5 > DRAW_VECTOR_MAX_Z) {   // cmp.w #DRAW_VECTOR_MAX_Z,d5 ; bgt no_more_parts
                    tooFar = true;
                    break;
                }
                if (fullscreen && (short) d5 <= 32767 / 3) { // cmp.w #32767/3,d5 ; bgt .old_scaler
                    Mem.ww(a2, d5);
                    a2 += 2;                            // move.w d5,(a2)+
                    d6 = setw(0, d5);                   // move.w d5,d6
                    d5 = setw(d5, d5 + d5);             // add.w d5,d5  (z*2)
                    d6 = setw(d6, d6 + d5);             // add.w d5,d6  (z*3)
                    d4 = d4 * 5;                        // muls.l #5,d4
                    d4 = divs(d4, d6);                  // divs d6,d4
                    d3 = d3 * 5;                        // muls.l #5,d3
                    d3 = divs(d3, d6);                  // divs d6,d3
                } else {
                    // .old_scaler (fullscreen overflow OU smallscreen)
                    Mem.ww(a2, d5);
                    a2 += 2;                            // move.w d5,(a2)+
                    if (fullscreen) {
                        d5 = setw(d5, d5 + d5);         // add.w d5,d5
                        d6 = 3413;                      // move.l #3413,d6
                        d4 = d4 >> 2;                   // asr.l #2,d4
                        d4 = d4 * d6;                   // muls.l d6,d4
                        d4 = d4 >> 8;                   // asr.l #8,d4
                        d4 = divs(d4, d5);              // divs d5,d4
                        d3 = d3 >> 2;                   // asr.l #2,d3
                        d3 = d3 * d6;                   // muls.l d6,d3
                        d3 = d3 >> 8;                   // asr.l #8,d3
                        d3 = divs(d3, d5);              // divs d5,d3
                    } else {
                        // smallscreen_conv : divs d5,d3 ; divs d5,d4
                        d3 = divs(d3, d5);              // divs d5,d3
                        d4 = divs(d4, d5);              // divs d5,d4
                    }
                }
                // .done_scaler
                d3 = setw(d3, d3 + Mem.uw(Vid_CentreX_w));         // add.w Vid_CentreX_w,d3
                d4 = setw(d4, d4 + Mem.uw(draw_PolygonCentreY_w)); // add.w draw_PolygonCentreY_w,d4
                Mem.ww(a3, d3);
                a3 += 2;                                // move.w d3,(a3)+
                Mem.ww(a3, d4);
                a3 += 2;                                // move.w d4,(a3)+
            }
            d7 = setw(d7, d7 - 1);                      // dbra d7,.convert_to_screen
            if ((short) d7 == -1) break;
        }

        if (!tooFar) {
            // done_conv (2061)
            d7 = setw(0, Mem.uw(draw_NumPoints_w));     // move.w draw_NumPoints_w,d7
            a6 = boxbrights_vw;                         // move.l #boxbrights_vw,a6
            d7 = setw(d7, d7 - 1);                      // subq #1,d7
            a0 = Mem.l(draw_PointAngPtr_l);             // move.l draw_PointAngPtr_l,a0
            a2 = draw_PointAndPolyBrights_vl;           // move.l #draw_PointAndPolyBrights_vl,a2
            d2 = setw(0, Mem.uw(draw_ObjectAng_w));     // move.w draw_ObjectAng_w,d2
            d2 = setw(d2, ((short) d2) >> 8);           // asr.w #8,d2
            d2 = setw(d2, ((short) d2) >> 1);           // asr.w #1,d2
            d5 = setb(0, 0xFF);                         // st d5  (inutilisé ensuite)
            while (true) {                              // .calc_point_angle_brightness_loop
                d0 = setb(0, Mem.ub(a0));
                a0 += 1;                                // moveq #0,d0 ; move.b (a0)+,d0
                d3 = setb(0, d0);                       // move.b d0,d3
                d3 = setw(d3, d3 + d2);                 // add.w d2,d3
                d3 = setw(d3, d3 & 0xF);                // and.w #$f,d3
                d0 = setw(d0, d0 & 0xF0);               // and.w #$f0,d0
                d0 = setw(d0, d0 + d3);                 // add.w d3,d0
                d1 = setb(0, Mem.ub(a2 + (d0 & 0xFFFF))); // moveq #0,d1 ; move.b (a2,d0.w),d1
                if ((byte) d1 < 0) d1 = 0;              // bge.s .okpos ; moveq #0,d1
                if ((short) d1 > 31) d1 = setw(d1, 31); // cmp.w #31,d1 ; ble.s .oksmall ; move.w #31,d1
                Mem.ww(a6, d1);
                a6 += 2;                                // move.w d1,(a6)+
                d7 = setw(d7, d7 - 1);                  // dbra d7,...
                if ((short) d7 == -1) break;
            }

            // tri des parts (2097)
            a1 = Mem.l(LinesPtr);                       // move.l LinesPtr,a1
            a0 = draw_PartBuffer_vw;                    // move.l #draw_PartBuffer_vw,a0
            a2 = a0;                                    // move.l a0,a2
            d0 = setw(0, 63);                           // move.w #63,d0
            while (true) {                              // clrpartbuff
                Mem.wl(a2, 0x80000001);                 // move.l #$80000001,(a2)
                a2 += 4;                                // addq #4,a2
                d0 = setw(d0, d0 - 1);
                if ((short) d0 == -1) break;            // dbra d0
            }
            a2 = draw_3DPointsRotated_vl;               // move.l #draw_3DPointsRotated_vl,a2
            d5 = Mem.l(draw_ObjectOnOff_l);             // move.l draw_ObjectOnOff_l,d5
            boolean sortit = Mem.w(draw_SortIt_w) != 0; // tst.w draw_SortIt_w ; bne PutinParts

            while (true) {                              // putinunsorted / PutinParts
                d7 = setw(0, Mem.uw(a1));
                a1 += 2;                                // move.w (a1)+,d7
                if ((short) d7 < 0) break;              // blt doneallparts
                int bit = d5 & 1;                       // lsr.l #1,d5 ; bcs
                d5 = d5 >>> 1;
                if (bit == 0) {                         // bcc → skip
                    a1 += 2;                            // addq #2,a1
                    continue;                           // bra ...
                }
                // .yeson
                d6 = setw(0, Mem.uw(a1));
                a1 += 2;                                // move.w (a1)+,d6
                if (!sortit) {
                    // putinunsorted
                    Mem.wl(a0, 0);
                    a0 += 4;                            // move.l #0,(a0)+
                    Mem.ww(a0, d7);                     // move.w d7,(a0)
                    a0 += 4;                            // addq #4,a0
                } else {
                    // PutinParts (depth sort)
                    d0 = Mem.l(a2 + (d6 & 0xFFFF));     // move.l (a2,d6.w),d0
                    d0 = d0 >> 7;                       // asr.l #7,d0
                    d0 = muls(d0, d0);                  // muls d0,d0
                    d2 = Mem.l(a2 + (d6 & 0xFFFF) + 4); // move.l 4(a2,d6.w),d2
                    d2 = d2 >> 7;                       // asr.l #7,d2
                    d2 = muls(d2, d2);                  // muls d2,d2
                    d0 = d0 + d2;                       // add.l d2,d0
                    d2 = setw(0, Mem.uw(a2 + (d6 & 0xFFFF) + 8)); // move.w 8(a2,d6.w),d2
                    d2 = muls(d2, d2);                  // muls d2,d2
                    d0 = d0 + d2;                       // add.l d2,d0
                    a0 = draw_PartBuffer_vw - 8;        // move.l #draw_PartBuffer_vw-8,a0
                    do {                                // stillfront
                        a0 += 8;                        // addq #8,a0
                    } while (d0 < Mem.l(a0));           // cmp.l (a0),d0 ; blt stillfront
                    a5 = draw_PartBufferEnd - 8;        // move.l #draw_PartBufferEnd-8,a5
                    while (true) {                      // domoreshift
                        Mem.wl(a5, Mem.l(a5 - 8));      // move.l -8(a5),(a5)
                        Mem.wl(a5 + 4, Mem.l(a5 - 4));  // move.l -4(a5),4(a5)
                        a5 -= 8;                        // subq #8,a5
                        if (!(a5 > a0)) break;          // cmp.l a0,a5 ; bgt domoreshift
                    }
                    Mem.wl(a0, d0);                     // move.l d0,(a0)
                    Mem.ww(a0 + 4, d7);                 // move.w d7,4(a0)
                }
            }

            // doneallparts (2175)
            a0 = draw_PartBuffer_vw;                    // move.l #draw_PartBuffer_vw,a0
            while (true) {                              // .part_loop
                d7 = Mem.l(a0);
                a0 += 4;                                // move.l (a0)+,d7
                if (d7 < 0) break;                      // blt no_more_parts
                d0 = setw(0, Mem.uw(a0));               // moveq #0,d0 ; move.w (a0),d0
                a0 += 4;                                // addq #4,a0
                d0 = d0 + Mem.l(draw_StartOfObjPtr_l);  // add.l draw_StartOfObjPtr_l,d0
                a1 = d0;                                // move.l d0,a1
                Mem.ww(firstpt, 0);                     // move.w #0,firstpt
                while (true) {                          // .polygon_loop
                    if ((short) Mem.uw(a1) < 0) break;  // tst.w (a1) ; blt .no_more_polygons
                    // movem.l a0/a1/d7 ; bsr doapoly ; movem
                    doapoly(a1);
                    d0 = setw(0, Mem.uw(a1));           // move.w (a1),d0
                    a1 = a1 + 18 + (d0 & 0xFFFF) * 4;   // lea 18(a1,d0.w*4),a1
                }
                // .no_more_polygons → .part_loop
            }
        }
        // no_more_parts: rts
    }

    // ==================================================================
    //  doapoly (objdrawhires.s:2220) — dessine un polygone.
    //  a1 = ptr données polygone. Termine à polybehind (rts).
    // ==================================================================
    static void doapoly(int a1) {
        int d0, d1, d2, d3, d4, d5, d6, d7;
        int a0, a2, a3, a4, a5, a6;

        Mem.ww(draw_Left_w, 960);                      // move.w #960,draw_Left_w
        Mem.ww(draw_Right_w, -10);                     // move.w #-10,draw_Right_w
        d7 = setw(0, Mem.uw(a1));
        a1 += 2;                                       // move.w (a1)+,d7  (lines to draw)
        Mem.ww(draw_PreHoles_b, Mem.uw(a1));
        a1 += 2;                                       // move.w (a1)+,draw_PreHoles_b (word→PreHoles+Holes)
        Mem.ww(draw_PreGouraud_b, Mem.uw(a1 + 12 + (d7 & 0xFFFF) * 4)); // move.w 12(a1,d7.w*4),draw_PreGouraud_b
        a3 = draw_2DPointsProjected_vl;                // move.l #draw_2DPointsProjected_vl,a3

        // SAVEREGS — checkbeh : guard-band clip. a1/d7 restaurés par GETREGS.
        final int sa1 = a1, sd7 = d7;
        {
            int ca1 = a1, cd7 = d7;
            while (true) {                             // checkbeh
                d0 = setw(0, Mem.uw(ca1));             // move.w (a1),d0
                d0 = Mem.l(a3 + (d0 & 0xFFFF) * 4);    // move.l (a3,d0.w*4),d0
                boolean clip = false;
                if ((short) d0 < -GUARDBAND) clip = true;          // cmp.w #-GUARDBAND,d0 ; blt .guard_clip
                else if ((short) d0 > GUARDBAND) clip = true;      // cmp.w #GUARDBAND,d0 ; bgt .guard_clip
                else {
                    d0 = swap(d0);                     // swap d0
                    if ((short) d0 < -GUARDBAND) clip = true;      // blt .guard_clip
                    else if ((short) d0 > GUARDBAND) clip = false; // ble .notbeh (else fall to clip)
                    else clip = false;                 // (>GUARDBAND déjà traité ; ici dans [-GB,GB])
                    if ((short) d0 > GUARDBAND) clip = true;       // (cmp.w #GUARDBAND,d0 ; ble .notbeh)
                }
                if (clip) return;                      // .guard_clip: GETREGS ; bra polybehind
                ca1 += 4;                              // addq #4,a1
                cd7 = setw(cd7, cd7 - 1);              // dbra d7,checkbeh
                if ((short) cd7 == -1) break;
            }
        }
        a1 = sa1;                                       // GETREGS
        d7 = sd7;

        // backface cull (2275) — produit screenspace cross-product
        d0 = setw(0, Mem.uw(a1));                       // move.w (a1),d0
        d1 = setw(0, Mem.uw(a1 + 4));                   // move.w 4(a1),d1
        d2 = setw(0, Mem.uw(a1 + 8));                   // move.w 8(a1),d2
        d3 = setw(0, Mem.uw(a3 + (d0 & 0xFFFF) * 4 + 2)); // move.w 2(a3,d0.w*4),d3
        d4 = setw(0, Mem.uw(a3 + (d1 & 0xFFFF) * 4 + 2)); // move.w 2(a3,d1.w*4),d4
        d5 = setw(0, Mem.uw(a3 + (d2 & 0xFFFF) * 4 + 2)); // move.w 2(a3,d2.w*4),d5
        d0 = setw(d0, Mem.uw(a3 + (d0 & 0xFFFF) * 4));  // move.w (a3,d0.w*4),d0
        d1 = setw(d1, Mem.uw(a3 + (d1 & 0xFFFF) * 4));  // move.w (a3,d1.w*4),d1
        d2 = setw(d2, Mem.uw(a3 + (d2 & 0xFFFF) * 4));  // move.w (a3,d2.w*4),d2
        d0 = setw(d0, d0 - d1);                         // sub.w d1,d0  (x1)
        d2 = setw(d2, d2 - d1);                         // sub.w d1,d2  (x2)
        d3 = setw(d3, d3 - d4);                         // sub.w d4,d3  (y1)
        d5 = setw(d5, d5 - d4);                         // sub.w d4,d5  (y2)
        d2 = muls(d2, d3);                              // muls d3,d2
        d0 = muls(d0, d5);                              // muls d5,d0
        d2 = d2 - d0;                                   // sub.l d0,d2
        if (d2 <= 0) return;                            // ble polybehind

        // polybright (3D cross product) (2292)
        a3 = draw_3DPointsRotated_vl;                   // move.l #draw_3DPointsRotated_vl,a3
        d0 = setw(0, Mem.uw(a1));                       // move.w (a1),d0
        d1 = setw(0, d0);                               // move.w d0,d1
        d0 = setw(d0, (d0 & 0xFFFF) << 2);              // asl.w #2,d0
        d0 = setw(d0, d0 + d1);                         // add.w d1,d0  (pt0*5)
        d1 = setw(0, Mem.uw(a1 + 4));                   // move.w 4(a1),d1
        d2 = d1;                                        // move.l d1,d2
        d1 = setw(d1, (d1 & 0xFFFF) << 2);              // asl.w #2,d1
        d1 = setw(d1, d1 + d2);                         // add.w d2,d1  (pt1*5)
        d2 = setw(0, Mem.uw(a1 + 8));                   // move.w 8(a1),d2
        d3 = setw(0, d2);                               // move.w d2,d3
        d2 = setw(d2, (d2 & 0xFFFF) << 2);              // asl.w #2,d2
        d2 = setw(d2, d2 + d3);                         // add.w d3,d2  (pt2*5)
        d3 = Mem.l(a3 + (d0 & 0xFFFF) * 2 + 4);         // move.l 4(a3,d0.w*2),d3
        d4 = Mem.l(a3 + (d1 & 0xFFFF) * 2 + 4);         // move.l 4(a3,d1.w*2),d4
        d5 = Mem.l(a3 + (d2 & 0xFFFF) * 2 + 4);         // move.l 4(a3,d2.w*2),d5
        d0 = Mem.l(a3 + (d0 & 0xFFFF) * 2);             // move.l (a3,d0.w*2),d0
        d1 = Mem.l(a3 + (d1 & 0xFFFF) * 2);             // move.l (a3,d1.w*2),d1
        d2 = Mem.l(a3 + (d2 & 0xFFFF) * 2);             // move.l (a3,d2.w*2),d2
        d0 = d0 - d1;                                   // sub.l d1,d0  (x1)
        d2 = d2 - d1;                                   // sub.l d1,d2  (x2)
        d3 = d3 - d4;                                   // sub.l d4,d3  (y1)
        d5 = d5 - d4;                                   // sub.l d4,d5  (y2)
        d0 = d0 >> 7;                                   // asr.l #7,d0
        d2 = d2 >> 7;                                   // asr.l #7,d2
        d3 = d3 >> 7;                                   // asr.l #7,d3
        d5 = d5 >> 7;                                   // asr.l #7,d5
        d2 = muls(d2, d3);                              // muls d3,d2
        d0 = muls(d0, d5);                              // muls d5,d0
        d2 = d2 - d0;                                   // sub.l d0,d2
        Mem.wl(polybright, d2);                         // move.l d2,polybright

        a3 = draw_2DPointsProjected_vl;                 // move.l #draw_2DPointsProjected_vl,a3
        Mem.wb(drawit, 0);                              // clr.b drawit
        if (Mem.b(draw_Gouraud_b) != 0) {              // tst.b draw_Gouraud_b ; bne usegour
            a1 = draw_PutInLinesGouraud(a1, d7);       // bsr draw_PutInLinesGouraud
        } else {
            a1 = draw_PutInLines(a1, d7);              // bsr draw_PutInLines
        }
        // dontusegour
        Mem.ww(linedir, Hires.SCREEN_WIDTH);           // move.w #SCREEN_WIDTH,linedir
        a6 = Mem.l(Vid_FastBufferPtr_l);               // move.l Vid_FastBufferPtr_l,a6
        if (Mem.b(drawit) == 0) return;                // tst.b drawit(pc) ; beq polybehind

        a4 = draw_PolyTopTab_vw;                        // move.l #draw_PolyTopTab_vw,a4
        d1 = setw(0, Mem.uw(draw_Left_w));              // move.w draw_Left_w,d1
        d7 = setw(0, Mem.uw(draw_Right_w));             // move.w draw_Right_w,d7
        d3 = setw(0, Mem.uw(draw_LeftClipB_w));         // move.w draw_LeftClipB_w,d3
        d4 = setw(0, Mem.uw(draw_RightClipB_w));        // move.w draw_RightClipB_w,d4
        if ((short) d7 <= (short) d3) return;           // cmp.w d3,d7 ; ble polybehind
        if ((short) d1 >= (short) d4) return;           // cmp.w d4,d1 ; bge polybehind
        if ((short) d1 < (short) d3) d1 = setw(d1, d3); // cmp.w d3,d1 ; bge.s .notop ; move.w d3,d1
        if ((short) d7 > (short) d4) d7 = setw(d7, d4); // cmp.w d4,d7 ; ble.s .nobot ; move.w d4,d7
        d1 = setw(d1, d1 + d1);                         // add.w d1,d1
        a4 = a4 + (short) d1 * 8;                       // lea (a4,d1.w*8),a4
        d1 = setw(d1, ((short) d1) >> 1);               // asr.w #1,d1
        d7 = setw(d7, d7 - d1);                         // sub.w d1,d7
        if ((short) d7 <= 0) return;                    // ble polybehind
        a2 = (short) d1;                                // move.w d1,a2
        d0 = 0;                                         // moveq #0,d0
        a0 = Mem.l(Draw_TextureMapsPtr_l);              // move.l Draw_TextureMapsPtr_l,a0
        d0 = setw(0, Mem.uw(a1));
        a1 += 2;                                        // move.w (a1)+,d0
        if ((short) d0 < 0) {                           // bge.s .notsec
            d0 = setw(d0, d0 & 0x7FFF);                 // and.w #$7fff,d0
            a0 = a0 + 65536;                            // add.l #65536,a0
        }
        // .notsec
        a0 = a0 + (short) d0;                           // add.w d0,a0
        if (dbgTex && dbgCurModel == dbgTexModel && dbgTexLog.size() < 30) // DIAG
            dbgTexLog.add("model=" + dbgCurModel + " texOff=" + (a0 - Mem.l(Draw_TextureMapsPtr_l)) + " (maps@" + Mem.l(Draw_TextureMapsPtr_l) + ")");
        d0 = 0;                                         // moveq #0,d0
        d1 = setb(0, Mem.ub(a1));
        a1 += 1;                                        // moveq #0,d1 ; move.b (a1)+,d1
        d1 = setw(d1, (d1 & 0xFFFF) << 5);              // asl.w #5,d1
        d1 = muls(d1, 41);                              // muls.w #41,d1
        d1 = d1 >> 8;                                   // asr.l #8,d1
        d1 = d1 >> 4;                                   // asr.l #4,d1
        d1 = setw(d1, -(short) d1);                     // neg.w d1
        d1 = setw(d1, d1 + 31);                         // add.w #31,d1

        if (Mem.b(draw_Holes_b) != 0) {                // tst.b draw_Holes_b ; bne gotholesin
            // gotholesin (2896)
            d4 = setw(0, Mem.uw(draw_ObjectAng_w));     // move.w draw_ObjectAng_w,d4
            d4 = setw(d4, ((short) d4) >> 8);           // asr.w #8,d4
            d4 = setw(d4, ((short) d4) >> 1);           // asr.w #1,d4
            d2 = 0;                                     // moveq #0,d2
            d3 = 0;                                     // moveq #0,d3
            d2 = setb(0, Mem.ub(a1));
            a1 += 1;                                    // move.b (a1)+,d2
            a1 = Mem.l(draw_PolyAngPtr_l);              // move.l draw_PolyAngPtr_l,a1
            d2 = setb(d2, Mem.ub(a1 + (d2 & 0xFFFF)));  // move.b (a1,d2.w),d2
            d3 = setb(d3, d2);                          // move.b d2,d3
            d3 = setb(d3, (d3 & 0xFF) >>> 4);           // lsr.b #4,d3  (d3 = vertical pos)
            d2 = setb(d2, d2 + d4);                     // add.b d4,d2
            d2 = setw(d2, d2 & 0xF);                    // and.w #$f,d2
            a1 = draw_AngleBrights_vl;                  // move.l #draw_AngleBrights_vl,a1
            d4 = 0;                                     // moveq #0,d4
            d5 = 0;                                     // moveq #0,d5
            d4 = setb(0, Mem.ub(a1 + (d2 & 0xFFFF)));   // move.b (a1,d2.w),d4  (top)
            d5 = setb(0, Mem.ub(a1 + (d2 & 0xFFFF) + 16)); // move.b 16(a1,d2.w),d5  (bottom)
            d5 = setw(d5, d5 - d4);                     // sub.w d4,d5
            d3 = setw(d3, d3 + 73);                     // add.w #73,d3
            d5 = muls(d5, d3);                          // muls d3,d5
            d5 = d5 >> 8;                               // asr.l #8,d5
            d5 = d5 >> 2;                               // asr.l #2,d5
            d5 = setw(d5, d5 + d4);                     // add.w d4,d5
            d1 = setw(d1, d1 + d5);                     // add.w d5,d1
            // (move.l #draw_ObjScaleCols_vw,a1 — inutilisé)
            if ((short) d1 < 0) d1 = setw(d1, 0);       // tst.w d1 ; bge.s toobrighth ; move.w #0,d1
            if ((short) d1 > 31) d1 = setw(d1, 31);     // cmp.w #31,d1 ; ble toodimh ; move.w #31,d1
            d1 = setw(d1, (d1 & 0xFFFF) << 8);          // asl.w #8,d1
            a1 = Mem.l(Draw_TexturePalettePtr_l) + 256 * 32; // move.l ...,a1 ; add.l #256*32,a1
            a1 = a1 + (short) d1;                       // add.w d1,a1
            fillPolyHoles(a0, a1, a2, a4, a6, d7);      // dopolyh
            return;
        }
        if (Mem.b(draw_Gouraud_b) != 0) {              // tst.b draw_Gouraud_b(pc) ; bne gotlurvelyshading
            // gotlurvelyshading (2739)
            a1 = Mem.l(Draw_TexturePalettePtr_l) + 256 * 32; // move.l ...,a1 ; add.l #256*32,a1
            fillPolyGouraud(a0, a1, a2, a4, a6, d7);    // dopolyg
            return;
        }

        // flat brightness path (2402)
        d4 = setw(0, Mem.uw(draw_ObjectAng_w));         // move.w draw_ObjectAng_w,d4
        d4 = setw(d4, ((short) d4) >> 8);               // asr.w #8,d4
        d4 = setw(d4, ((short) d4) >> 1);               // asr.w #1,d4
        d2 = 0;                                         // moveq #0,d2
        d3 = 0;                                         // moveq #0,d3
        d2 = setb(0, Mem.ub(a1));
        a1 += 1;                                        // move.b (a1)+,d2
        a1 = Mem.l(draw_PolyAngPtr_l);                  // move.l draw_PolyAngPtr_l,a1
        d2 = setb(d2, Mem.ub(a1 + (d2 & 0xFFFF)));      // move.b (a1,d2.w),d2
        d3 = setb(d3, d2);                              // move.b d2,d3
        d3 = setw(d3, d3 + d4);                         // add.w d4,d3
        d3 = setw(d3, d3 & 0xF);                        // and.w #$f,d3
        d2 = setw(d2, d2 & 0xF0);                       // and.w #$f0,d2
        d2 = setb(d2, d2 + d3);                         // add.b d3,d2
        a1 = draw_PointAndPolyBrights_vl;               // move.l #draw_PointAndPolyBrights_vl,a1
        d5 = 0;                                         // moveq #0,d5
        d5 = setb(0, Mem.ub(a1 + (d2 & 0xFFFF)));       // move.b (a1,d2.w),d5
        d1 = setw(d1, d1 + d5);                         // add.w d5,d1
        // (move.l #draw_ObjScaleCols_vw,a1 — inutilisé)
        if ((short) d1 < 0) d1 = setw(d1, 0);           // tst.w d1 ; bge toobright ; move.w #0,d1
        if ((short) d1 > 31) d1 = setw(d1, 31);         // cmp.w #31,d1 ; blt .toodark ; moveq #31,d1
        d1 = setw(d1, (d1 & 0xFFFF) << 8);              // asl.w #8,d1
        a1 = Mem.l(Draw_TexturePalettePtr_l) + 256 * 32; // move.l ...,a1 ; add.l #256*32,a1
        a1 = a1 + (short) d1;                           // lea (a1,d1.w),a1
        if (Mem.b(draw_PreGouraud_b) != 0) {           // tst.b draw_PreGouraud_b ; bne predoglare
            // predoglare (2576)
            a1 = Mem.l(Draw_TexturePalettePtr_l) - 512; // move.l Draw_TexturePalettePtr_l,a1 ; sub.w #512,a1
            fillPolyGlare(a0, a1, a2, a4, a6, d7);      // DOGLAREPOLY
            return;
        }
        fillPolyFlat(a0, a1, a2, a4, a6, d7);           // dopoly
    }

    // --- MUL_INV_PAIR (USE_16X16_TEXEL_MULS non défini) : modifie {d3,d4} via réciproque d0 ---
    // Implémenté inline aux points d'usage (ext.l d0 ; asr.l#8 ; muls.l ; asr.l#6).

    // Offset constant entre les tables top/bot (records de 16 octets par colonne).
    private static final int POLYBOT_OFS = draw_PolyBotTab_vw - draw_PolyTopTab_vw;

    // dopoly (objdrawhires.s:2442) — remplissage plat texturé.
    static void fillPolyFlat(int a0, int a1, int a2, int a4, int a6, int d7) {
        int d0, d1, d2, d3, d4, d5, d6, a3, a5;
        while (true) {                                 // dopoly
            Mem.ww(offtopby, 0);                       // move.w #0,offtopby
            a3 = a6 + (short) a2;                      // move.l a6,a3 ; adda.w a2,a3
            a2 += 1;                                   // addq #1,a2
            d1 = setw(0, Mem.uw(a4));                  // move.w (a4),d1
            boolean draw = true;
            if ((short) d1 >= (short) Mem.uw(draw_ObjClipB_w)) draw = false; // cmp.w draw_ObjClipB_w,d1 ; bge nodl
            else {
                d2 = setw(0, Mem.uw(a4 + POLYBOT_OFS)); // move.w PolyBot-PolyTop(a4),d2
                if ((short) d2 <= (short) Mem.uw(draw_ObjClipT_w)) draw = false; // ble nodl
                else {
                    if ((short) d1 < (short) Mem.uw(draw_ObjClipT_w)) { // bge.s nocl
                        d3 = setw(0, Mem.uw(draw_ObjClipT_w));
                        d3 = setw(d3, d3 - d1);
                        Mem.ww(offtopby, d3);          // move.w d3,offtopby
                        d1 = setw(d1, Mem.uw(draw_ObjClipT_w));
                    }
                    // nocl
                    d0 = setw(0, d2);                  // move.w d2,d0
                    if ((short) d2 > (short) Mem.uw(draw_ObjClipB_w)) d2 = setw(d2, Mem.uw(draw_ObjClipB_w)); // ble nocr
                    // nocr
                    d3 = Mem.l(a4 + 2 + POLYBOT_OFS);  // move.l 2+PolyBot-PolyTop(a4),d3
                    d4 = Mem.l(a4 + 6 + POLYBOT_OFS);  // move.l 6+PolyBot-PolyTop(a4),d4
                    d5 = Mem.l(a4 + 2);                // move.l 2(a4),d5
                    d6 = Mem.l(a4 + 6);                // move.l 6(a4),d6
                    d3 = d3 - d5;                      // sub.l d5,d3
                    d4 = d4 - d6;                      // sub.l d6,d4
                    d2 = setw(d2, d2 - d1);            // sub.w d1,d2
                    if ((short) d2 <= 0) draw = false; // ble nodl
                    else {
                        Mem.ww(tstdca, 0);             // move.w #0,tstdca
                        d0 = setw(d0, d0 - d1);        // sub.w d1,d0
                        if (Mem.w(offtopby) != 0) {    // tst.w offtopby ; beq.s .notofftop
                            int sd3 = d3, sd4 = d4;    // move.l d3/d4,-(a7)
                            d0 = setw(d0, d0 + Mem.uw(offtopby)); // add.w offtopby,d0
                            d3 = d3 * Mem.l(offtopby - 2); // muls.l offtopby-2,d3
                            d4 = d4 * Mem.l(offtopby - 2); // muls.l offtopby-2,d4
                            int sdiv = d0;             // move.w d0,-2(sp)
                            if ((d0 & 0xFFFF) > MAX_ONE_OVER_N) d0 = setw(d0, MAX_ONE_OVER_N);
                            d0 = setw(d0, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2));
                            d0 = (short) d0; d3 = (d3 >> 8) * d0; d4 = (d4 >> 8) * d0; d3 >>= 6; d4 >>= 6; // MUL_INV_PAIR
                            d0 = setw(d0, sdiv);       // restore divisor
                            d5 = d5 + d3;              // add.l d3,d5
                            d6 = d6 + d4;              // add.l d4,d6
                            d4 = sd4; d3 = sd3;        // pop
                        }
                        // .notofftop
                        if ((d0 & 0xFFFF) > MAX_ONE_OVER_N) d0 = setw(d0, MAX_ONE_OVER_N);
                        d0 = setw(d0, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2));
                        d0 = (short) d0; d3 = (d3 >> 8) * d0; d4 = (d4 >> 8) * d0; d3 >>= 6; d4 >>= 6; // MUL_INV_PAIR
                        a3 = a3 + Mem.l(ontoscr + (d1 & 0xFFFF) * 4); // add.l ontoscr(pc,d1.w*4),a3
                        d1 = dbgCheesy ? 0x1fffff : 0x3fffff; // move.l #$3fffff,d1 (CHEESEY=0) / #$1fffff (CHEESEY=1)
                        a5 = d3;                       // move.l d3,a5
                        d3 = 0;                        // moveq #0,d3
                        d2 = setw(d2, d2 - 1);         // subq #1,d2
                        while (true) {                 // drawpol
                            d5 = d5 & d1;              // and.l d1,d5
                            d6 = d6 & d1;              // and.l d1,d6
                            d0 = d6 >> 8;              // move.l d6,d0 ; asr.l #8,d0
                            d5 = swap(d5);             // swap d5
                            d0 = setb(d0, d5);         // move.b d5,d0
                            d3 = setb(d3, Mem.ub(a0 + (d0 & 0xFFFF) * (dbgCheesy ? 1 : 4))); // move.b (a0,d0.w*4),d3 (CHEESEY=0) / (a0,d0.w) (CHEESEY=1)
                            d5 = swap(d5);             // swap d5
                            d5 = d5 + a5;              // add.l a5,d5
                            d6 = d6 + d4;              // add.l d4,d6
                            Mem.wb(a3, Mem.ub(a1 + (d3 & 0xFFFF))); // move.b (a1,d3.w),(a3)
                            a3 = a3 + Hires.SCREEN_WIDTH; // adda.w #SCREEN_WIDTH,a3
                            d2 = setw(d2, d2 - 1);
                            if ((short) d2 == -1) break; // dbra d2,drawpol
                        }
                    }
                }
            }
            // pastit / nodl
            a4 = a4 + 16;                              // adda.w #16,a4
            d7 = setw(d7, d7 - 1);                     // dbra d7,dopoly
            if ((short) d7 == -1) break;
        }
        // rts
    }

    // DOGLAREPOLY (objdrawhires.s:2580) — remplissage glare (texel<<9|pixel via a1).
    static void fillPolyGlare(int a0, int a1, int a2, int a4, int a6, int d7) {
        int d0, d1, d2, d3, d4, d5, d6, a3, a5;
        while (true) {                                 // DOGLAREPOLY
            Mem.ww(offtopby, 0);
            a3 = a6 + (short) a2;
            a2 += 1;
            d1 = setw(0, Mem.uw(a4));
            boolean draw = true;
            if ((short) d1 >= (short) Mem.uw(draw_ObjClipB_w)) draw = false; // bge nodlGL
            else {
                d2 = setw(0, Mem.uw(a4 + POLYBOT_OFS));
                if ((short) d2 <= (short) Mem.uw(draw_ObjClipT_w)) draw = false; // ble nodlGL
                else {
                    if ((short) d1 < (short) Mem.uw(draw_ObjClipT_w)) {
                        d3 = setw(0, Mem.uw(draw_ObjClipT_w));
                        d3 = setw(d3, d3 - d1);
                        Mem.ww(offtopby, d3);
                        d1 = setw(d1, Mem.uw(draw_ObjClipT_w));
                    }
                    d0 = setw(0, d2);
                    if ((short) d2 > (short) Mem.uw(draw_ObjClipB_w)) d2 = setw(d2, Mem.uw(draw_ObjClipB_w));
                    d3 = Mem.l(a4 + 2 + POLYBOT_OFS);
                    d4 = Mem.l(a4 + 6 + POLYBOT_OFS);
                    d5 = Mem.l(a4 + 2);
                    d6 = Mem.l(a4 + 6);
                    d3 = d3 - d5;
                    d4 = d4 - d6;
                    d2 = setw(d2, d2 - d1);
                    if ((short) d2 <= 0) draw = false; // ble nodlGL
                    else {
                        Mem.ww(tstdca, 0);
                        d0 = setw(d0, d0 - d1);
                        if (Mem.w(offtopby) != 0) {
                            int sd3 = d3, sd4 = d4;
                            d0 = setw(d0, d0 + Mem.uw(offtopby));
                            int sdiv = d0;
                            if ((d0 & 0xFFFF) > MAX_ONE_OVER_N) d0 = setw(d0, MAX_ONE_OVER_N);
                            d3 = d3 * Mem.l(offtopby - 2);
                            d4 = d4 * Mem.l(offtopby - 2);
                            d0 = setw(d0, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2));
                            d0 = (short) d0; d3 = (d3 >> 8) * d0; d4 = (d4 >> 8) * d0; d3 >>= 6; d4 >>= 6;
                            d0 = setw(d0, sdiv);
                            d5 = d5 + d3;
                            d6 = d6 + d4;
                            d4 = sd4; d3 = sd3;
                        }
                        if ((d0 & 0xFFFF) > MAX_ONE_OVER_N) d0 = setw(d0, MAX_ONE_OVER_N);
                        d0 = setw(d0, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2));
                        d0 = (short) d0; d3 = (d3 >> 8) * d0; d4 = (d4 >> 8) * d0; d3 >>= 6; d4 >>= 6;
                        a3 = a3 + Mem.l(ontoscrGL + (d1 & 0xFFFF) * 4); // add.l ontoscrGL(pc,d1.w*4),a3
                        d1 = dbgCheesy ? 0x1fffff : 0x3fffff;
                        a5 = d3;
                        d3 = 0;
                        d2 = setw(d2, d2 - 1);
                        while (true) {                 // drawpolGL
                            d5 = d5 & d1;
                            d6 = d6 & d1;
                            d0 = d6 >> 8;
                            d5 = swap(d5);
                            d0 = setb(d0, d5);
                            d3 = setb(d3, Mem.ub(a0 + (d0 & 0xFFFF) * (dbgCheesy ? 1 : 4))); // move.b (a0,d0.w*4),d3
                            if ((d3 & 0xFF) != 0) {    // beq.s itsblack
                                d3 = setw(d3, (d3 & 0xFFFF) << 8); // lsl.w #8,d3
                                d3 = setw(d3, d3 + d3); // add.w d3,d3
                                d3 = setb(d3, Mem.ub(a3)); // move.b (a3),d3
                                d5 = swap(d5);         // swap d5
                                d5 = d5 + a5;          // add.l a5,d5
                                d6 = d6 + d4;          // add.l d4,d6
                                Mem.wb(a3, Mem.ub(a1 + (d3 & 0xFFFF))); // move.b (a1,d3.w),(a3)
                            } else {
                                // itsblack
                                d5 = swap(d5);         // swap d5
                                d5 = d5 + a5;          // add.l a5,d5
                                d6 = d6 + d4;          // add.l d4,d6
                            }
                            a3 = a3 + Hires.SCREEN_WIDTH; // adda.w #SCREEN_WIDTH,a3
                            d2 = setw(d2, d2 - 1);
                            if ((short) d2 == -1) break; // dbra d2,drawpolGL
                        }
                    }
                }
            }
            a4 = a4 + 16;
            d7 = setw(d7, d7 - 1);
            if ((short) d7 == -1) break;               // dbra d7,DOGLAREPOLY
        }
    }

    // dopolyg (objdrawhires.s:2744) — remplissage gouraud (brightness verticale).
    static void fillPolyGouraud(int a0, int a1, int a2, int a4, int a6, int d7Col) {
        int d0, d1, d2, d3, d4, d5, d6, d7, a3, a5;
        while (true) {                                 // dopolyg
            int savedCol = d7Col;                      // move.l d7,-(a7)
            Mem.ww(offtopby, 0);
            a3 = a6 + (short) a2;
            a2 += 1;
            d1 = setw(0, Mem.uw(a4));
            boolean draw = true;
            if ((short) d1 >= (short) Mem.uw(draw_ObjClipB_w)) draw = false; // bge nodlg
            else {
                d2 = setw(0, Mem.uw(a4 + POLYBOT_OFS));
                if ((short) d2 <= (short) Mem.uw(draw_ObjClipT_w)) draw = false; // ble nodlg
                else {
                    if ((short) d1 < (short) Mem.uw(draw_ObjClipT_w)) {
                        d3 = setw(0, Mem.uw(draw_ObjClipT_w));
                        d3 = setw(d3, d3 - d1);
                        Mem.ww(offtopby, d3);
                        d1 = setw(d1, Mem.uw(draw_ObjClipT_w));
                    }
                    d0 = setw(0, d2);
                    if ((short) d2 > (short) Mem.uw(draw_ObjClipB_w)) d2 = setw(d2, Mem.uw(draw_ObjClipB_w));
                    d3 = Mem.l(a4 + 2 + POLYBOT_OFS);
                    d4 = Mem.l(a4 + 6 + POLYBOT_OFS);
                    d5 = Mem.l(a4 + 2);
                    d6 = Mem.l(a4 + 6);
                    d3 = d3 - d5;
                    d4 = d4 - d6;
                    d2 = setw(d2, d2 - d1);
                    if ((short) d2 <= 0) draw = false; // ble nodlg
                    else {
                        Mem.ww(tstdca, 0);
                        d0 = setw(d0, d0 - d1);
                        if (Mem.w(offtopby) != 0) {
                            int sd3 = d3, sd4 = d4;
                            d0 = setw(d0, d0 + Mem.uw(offtopby));
                            int sdiv = d0;
                            if ((d0 & 0xFFFF) > MAX_ONE_OVER_N) d0 = setw(d0, MAX_ONE_OVER_N);
                            d3 = d3 * Mem.l(offtopby - 2);
                            d4 = d4 * Mem.l(offtopby - 2);
                            d0 = setw(d0, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2));
                            d0 = (short) d0; d3 = (d3 >> 8) * d0; d4 = (d4 >> 8) * d0; d3 >>= 6; d4 >>= 6;
                            d0 = setw(d0, sdiv);
                            d5 = d5 + d3;
                            d6 = d6 + d4;
                            d4 = sd4; d3 = sd3;
                        }
                        if ((d0 & 0xFFFF) > MAX_ONE_OVER_N) d0 = setw(d0, MAX_ONE_OVER_N);
                        d0 = setw(d0, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2));
                        d0 = (short) d0; d3 = (d3 >> 8) * d0; d4 = (d4 >> 8) * d0; d3 >>= 6; d4 >>= 6;
                        a3 = a3 + Mem.l(ontoscrg + (d1 & 0xFFFF) * 4); // add.l ontoscrg(pc,d1.w*4),a3
                        d1 = setw(0, Mem.uw(a4 + 10 + POLYBOT_OFS)); // move.w 10+PolyBot-PolyTop(a4),d1
                        d7 = setw(0, Mem.uw(a4 + 10)); // move.w 10(a4),d7
                        d1 = setw(d1, d1 - d7);        // sub.w d7,d1
                        d7 = setw(d7, (d7 & 0xFFFF) << 8); // asl.w #8,d7
                        d1 = swap(d1);                 // swap d1
                        d1 = setw(d1, 0);              // clr.w d1
                        d1 = (d1 >> 8) * d0; d1 >>= 6; // MUL_INV d0,d1
                        d1 = d1 >> 8;                  // asr.l #8,d1
                        a5 = d3;                       // move.l d3,a5
                        d3 = 0;                        // moveq #0,d3
                        d2 = swap(d2);                 // swap d2
                        d2 = setw(d2, d1);             // move.w d1,d2
                        d2 = swap(d2);                 // swap d2
                        d1 = dbgCheesy ? 0x1fffff : 0x3fffff; // move.l #$3fffff,d1
                        d2 = setw(d2, d2 - 1);         // subq.w #1,d2
                        while (true) {                 // drawpolg
                            d5 = d5 & d1;
                            d6 = d6 & d1;
                            d0 = d6 >> 8;
                            d5 = swap(d5);
                            d0 = setb(d0, d5);
                            d3 = setw(d3, d7);         // move.w d7,d3  (brightness<<8)
                            d3 = setb(d3, Mem.ub(a0 + (d0 & 0xFFFF) * (dbgCheesy ? 1 : 4))); // move.b (a0,d0.w*4),d3
                            d2 = swap(d2);             // swap d2
                            d5 = swap(d5);             // swap d5
                            d5 = d5 + a5;              // add.l a5,d5
                            d6 = d6 + d4;              // add.l d4,d6
                            d7 = setw(d7, d7 + d2);    // add.w d2,d7
                            d2 = swap(d2);             // swap d2
                            Mem.wb(a3, Mem.ub(a1 + (d3 & 0xFFFF))); // move.b (a1,d3.w),(a3)
                            a3 = a3 + Hires.SCREEN_WIDTH; // adda.w #SCREEN_WIDTH,a3
                            d2 = setw(d2, d2 - 1);
                            if ((short) d2 == -1) break; // dbra d2,drawpolg
                        }
                    }
                }
            }
            // nodlg
            d7Col = savedCol;                          // move.l (a7)+,d7
            a4 = a4 + 16;                              // adda.w #16,a4
            d7Col = setw(d7Col, d7Col - 1);            // dbra d7,dopolyg
            if ((short) d7Col == -1) break;
        }
    }

    // dopolyh (objdrawhires.s:2952) — remplissage avec trous (skip texel 0).
    static void fillPolyHoles(int a0, int a1, int a2, int a4, int a6, int d7) {
        int d0, d1, d2, d3, d4, d5, d6, a3, a5;
        while (true) {                                 // dopolyh
            Mem.ww(offtopby, 0);
            a3 = a6 + (short) a2;
            a2 += 1;
            d1 = setw(0, Mem.uw(a4));
            boolean draw = true;
            if ((short) d1 >= (short) Mem.uw(draw_ObjClipB_w)) draw = false; // bge nodlh
            else {
                d2 = setw(0, Mem.uw(a4 + POLYBOT_OFS));
                if ((short) d2 <= (short) Mem.uw(draw_ObjClipT_w)) draw = false; // ble nodlh
                else {
                    if ((short) d1 < (short) Mem.uw(draw_ObjClipT_w)) {
                        d3 = setw(0, Mem.uw(draw_ObjClipT_w));
                        d3 = setw(d3, d3 - d1);
                        Mem.ww(offtopby, d3);
                        d1 = setw(d1, Mem.uw(draw_ObjClipT_w));
                    }
                    d0 = setw(0, d2);
                    if ((short) d2 > (short) Mem.uw(draw_ObjClipB_w)) d2 = setw(d2, Mem.uw(draw_ObjClipB_w));
                    d3 = Mem.l(a4 + 2 + POLYBOT_OFS);
                    d4 = Mem.l(a4 + 6 + POLYBOT_OFS);
                    d5 = Mem.l(a4 + 2);
                    d6 = Mem.l(a4 + 6);
                    d3 = d3 - d5;
                    d4 = d4 - d6;
                    d2 = setw(d2, d2 - d1);
                    if ((short) d2 <= 0) draw = false; // ble nodlh
                    else {
                        Mem.ww(tstdca, 0);
                        d0 = setw(d0, d0 - d1);
                        if (Mem.w(offtopby) != 0) {
                            int sd3 = d3, sd4 = d4;
                            d0 = setw(d0, d0 + Mem.uw(offtopby));
                            int sdiv = d0;
                            if ((d0 & 0xFFFF) > MAX_ONE_OVER_N) d0 = setw(d0, MAX_ONE_OVER_N);
                            d3 = d3 * Mem.l(offtopby - 2);
                            d4 = d4 * Mem.l(offtopby - 2);
                            d0 = setw(d0, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2));
                            d0 = (short) d0; d3 = (d3 >> 8) * d0; d4 = (d4 >> 8) * d0; d3 >>= 6; d4 >>= 6;
                            d0 = setw(d0, sdiv);
                            d5 = d5 + d3;
                            d6 = d6 + d4;
                            d4 = sd4; d3 = sd3;
                        }
                        if ((d0 & 0xFFFF) > MAX_ONE_OVER_N) d0 = setw(d0, MAX_ONE_OVER_N);
                        d0 = setw(d0, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2));
                        d0 = (short) d0; d3 = (d3 >> 8) * d0; d4 = (d4 >> 8) * d0; d3 >>= 6; d4 >>= 6;
                        a3 = a3 + Mem.l(ontoscrh + (d1 & 0xFFFF) * 4); // add.l ontoscrh(pc,d1.w*4),a3
                        d1 = dbgCheesy ? 0x1fffff : 0x3fffff;
                        a5 = d3;
                        d3 = 0;
                        d2 = setw(d2, d2 - 1);
                        while (true) {                 // drawpolh
                            d5 = d5 & d1;
                            d6 = d6 & d1;
                            d0 = d6 >> 8;
                            d5 = swap(d5);
                            d0 = setb(d0, d5);
                            d5 = swap(d5);             // swap d5 (note : avant le fetch texel ici)
                            d5 = d5 + a5;              // add.l a5,d5
                            d6 = d6 + d4;              // add.l d4,d6
                            d3 = setb(d3, Mem.ub(a0 + (d0 & 0xFFFF) * (dbgCheesy ? 1 : 4))); // move.b (a0,d0.w*4),d3
                            if ((d3 & 0xFF) != 0) {    // beq.s .dontplot
                                Mem.wb(a3, Mem.ub(a1 + (d3 & 0xFFFF))); // move.b (a1,d3.w),(a3)
                            }
                            // .dontplot
                            a3 = a3 + Hires.SCREEN_WIDTH; // adda.w #SCREEN_WIDTH,a3
                            d2 = setw(d2, d2 - 1);
                            if ((short) d2 == -1) break; // dbra d2,drawpolh
                        }
                    }
                }
            }
            // pastith / nodlh
            a4 = a4 + 16;
            d7 = setw(d7, d7 - 1);
            if ((short) d7 == -1) break;               // dbra d7,dopolyh
        }
    }

    // ==================================================================
    //  draw_PutInLines (objdrawhires.s:3090) — edge-walk : pour chaque arête,
    //  remplit draw_PolyTopTab_vw / draw_PolyBotTab_vw (par colonne x : y, u, v).
    //  Records de 16 octets : {y:w@0, u:l@2, v:l@6, (gouraud bright:w@10)}.
    //  a1 = liste de points ; d7 = nb arêtes-1. Renvoie a1 avancé.
    // ==================================================================
    static int draw_PutInLines(int a1, int d7) {
        int d0, d1, d2, d3, d4, d5, d6;
        int a3 = draw_2DPointsProjected_vl, a4, a5, a6;
        while (true) {                                 // draw_PutInLines
            d0 = setw(0, Mem.uw(a1));                  // move.w (a1),d0
            d1 = setw(0, Mem.uw(a1 + 4));              // move.w 4(a1),d1
            d2 = setw(0, Mem.uw(a3 + (d0 & 0xFFFF) * 4));     // move.w (a3,d0.w*4),d2  (x1)
            d3 = setw(0, Mem.uw(a3 + (d0 & 0xFFFF) * 4 + 2)); // move.w 2(a3,d0.w*4),d3 (y1)
            d4 = setw(0, Mem.uw(a3 + (d1 & 0xFFFF) * 4));     // move.w (a3,d1.w*4),d4  (x2)
            d5 = setw(0, Mem.uw(a3 + (d1 & 0xFFFF) * 4 + 2)); // move.w 2(a3,d1.w*4),d5 (y2)

            boolean flat = false;
            boolean onTop;
            if ((short) d4 == (short) d2) { flat = true; onTop = false; } // cmp.w d2,d4 ; beq this_line_flat
            else if ((short) d4 > (short) d2) { onTop = true; }           // bgt this_line_on_top
            else {                                                        // bottom edge
                onTop = false;
                a4 = draw_PolyBotTab_vw;                // move.l #draw_PolyBotTab_vw,a4
                int t = d2; d2 = d4; d4 = t;            // exg d2,d4
                t = d3; d3 = d5; d5 = t;                // exg d3,d5
                if ((short) d2 >= (short) Mem.uw(draw_RightClipB_w)) flat = true; // bge this_line_flat
                else if ((short) d4 <= (short) Mem.uw(draw_LeftClipB_w)) flat = true; // ble this_line_flat
                else {
                    int pushClip;                       // (a7) : montant de clip droit
                    d6 = setw(0, Mem.uw(draw_RightClipB_w));
                    d6 = setw(d6, d6 - d4);             // sub.w d4,d6
                    if ((short) d6 <= 0) {              // .clip_right
                        pushClip = d6;                  // move.w d6,-(a7)
                        Mem.ww(draw_Right_w, Mem.uw(draw_RightClipB_w)); // move.w draw_RightClipB_w,draw_Right_w
                        Mem.ww(draw_Right_w, Mem.uw(draw_Right_w) - 1);  // sub.w #1,draw_Right_w
                    } else {
                        pushClip = 0;                   // move.w #0,-(a7)
                        if ((short) d4 > (short) Mem.uw(draw_Right_w)) Mem.ww(draw_Right_w, d4); // ble .no_new_bottom ; move.w d4,draw_Right_w
                    }
                    // .no_new_bottom
                    Mem.ww(draw_OffLeftBy_w, 0);        // move.w #0,draw_OffLeftBy_w
                    d6 = setw(0, d2);                   // move.w d2,d6
                    if ((short) d6 < (short) Mem.uw(draw_LeftClipB_w)) { // bge .okt
                        d6 = setw(0, Mem.uw(draw_LeftClipB_w));
                        d6 = setw(d6, d6 - d2);
                        Mem.ww(draw_OffLeftBy_w, d6);
                        d6 = setw(d6, d6 + d2);
                    }
                    // .okt
                    Mem.wb(drawit, 0xFF);               // st drawit
                    d6 = setw(d6, d6 + d6);             // add.w d6,d6
                    a4 = a4 + (short) d6 * 8;           // lea (a4,d6.w*8),a4
                    d6 = setw(d6, ((short) d6) >> 1);   // asr.w #1,d6
                    if ((short) d6 < (short) Mem.uw(draw_Left_w)) Mem.ww(draw_Left_w, d6); // bge .no_new_top ; move.w d6,draw_Left_w
                    // .no_new_top
                    d5 = setw(d5, d5 - d3);             // sub.w d3,d5  (dy)
                    d3 = swap(d3);                      // swap d3
                    d3 = setw(d3, 0);                   // clr.w d3  (y accumulator init = y2<<16)
                    d4 = setw(d4, d4 - d2);             // sub.w d2,d4  (dx > 0)
                    d4 = (short) d4;                    // ext.l d4
                    d5 = swap(d5);                      // swap d5
                    d5 = setw(d5, 0);                   // clr.w d5
                    d5 = d5 / d4;                       // divs.l d4,d5  (dy step)
                    d2 = setb(0, Mem.ub(a1 + 2));       // moveq #0,d2 ; move.b 2(a1),d2
                    d6 = setb(0, Mem.ub(a1 + 6));       // moveq #0,d6 ; move.b 6(a1),d6
                    d2 = setw(d2, d2 - d6);             // sub.w d6,d2
                    d2 = swap(d2);                      // swap d2
                    d6 = swap(d6);                      // swap d6
                    d2 = setw(d2, 0);                   // clr.w d2
                    d6 = setw(d6, 0);                   // clr.w d6  (u accumulator init)
                    d2 = d2 / d4;                       // divs.l d4,d2  (du step)
                    a5 = d5;                            // move.l d5,a5  (dy const)
                    a6 = d2;                            // move.l d2,a6  (du const)
                    d5 = setb(0, Mem.ub(a1 + 3));       // moveq #0,d5 ; move.b 3(a1),d5
                    d2 = setb(0, Mem.ub(a1 + 7));       // moveq #0,d2 ; move.b 7(a1),d2
                    d5 = setw(d5, d5 - d2);             // sub.w d2,d5
                    d2 = swap(d2);                      // swap d2
                    d5 = swap(d5);                      // swap d5
                    d2 = setw(d2, 0);                   // clr.w d2  (v accumulator init)
                    d5 = setw(d5, 0);                   // clr.w d5
                    d5 = d5 / d4;                       // divs.l d4,d5  (dv step)
                    d4 = setw(d4, d4 + pushClip);       // add.w (a7)+,d4
                    d4 = setw(d4, d4 - Mem.uw(draw_OffLeftBy_w)); // sub.w draw_OffLeftBy_w,d4
                    if ((short) d4 < 0) flat = true;    // blt this_line_flat
                    else {
                        if (Mem.w(draw_OffLeftBy_w) != 0) { // tst.w draw_OffLeftBy_w ; beq .none_off_left
                            int sd4 = d4;               // move.w d4,-(a7)
                            d4 = setw(0, Mem.uw(draw_OffLeftBy_w)); // move.w draw_OffLeftBy_w,d4
                            d4 = setw(d4, d4 - 1);      // dbra d4,.calc_no_draw (pré-déc)
                            while ((short) d4 != -1) {  // .calc_no_draw
                                d3 = d3 + a5;           // add.l a5,d3
                                d6 = d6 + a6;           // add.l a6,d6
                                d2 = d2 + d5;           // add.l d5,d2
                                d4 = setw(d4, d4 - 1);  // dbra d4
                            }
                            d4 = setw(sd4, sd4);        // move.w (a7)+,d4
                        }
                        // .none_off_left / .put_in_line
                        while (true) {
                            d3 = swap(d3);              // swap d3
                            Mem.ww(a4, d3);
                            a4 += 2;                    // move.w d3,(a4)+
                            d3 = swap(d3);              // swap d3
                            Mem.wl(a4, d6);
                            a4 += 4;                    // move.l d6,(a4)+
                            Mem.wl(a4, d2);
                            a4 += 4;                    // move.l d2,(a4)+
                            a4 += 6;                    // addq #6,a4
                            d3 = d3 + a5;               // add.l a5,d3
                            d6 = d6 + a6;               // add.l a6,d6
                            d2 = d2 + d5;               // add.l d5,d2
                            d4 = setw(d4, d4 - 1);      // dbra d4,.put_in_line
                            if ((short) d4 == -1) break;
                        }
                        flat = true;                    // bra this_line_flat
                    }
                }
            }

            if (!flat && onTop) {
                // this_line_on_top (3229)
                a4 = draw_PolyTopTab_vw;                // move.l #draw_PolyTopTab_vw,a4
                if ((short) d2 >= (short) Mem.uw(draw_RightClipB_w)) flat = true; // bge this_line_flat
                else if ((short) d4 <= (short) Mem.uw(draw_LeftClipB_w)) flat = true; // ble this_line_flat
                else {
                    int pushClip;
                    d6 = setw(0, Mem.uw(draw_RightClipB_w));
                    d6 = setw(d6, d6 - d4);
                    if ((short) d6 <= 0) {              // .clip_right
                        pushClip = d6;
                        Mem.ww(draw_Right_w, Mem.uw(draw_RightClipB_w));
                        Mem.ww(draw_Right_w, Mem.uw(draw_Right_w) - 1);
                    } else {
                        pushClip = 0;
                        if ((short) d4 > (short) Mem.uw(draw_Right_w)) Mem.ww(draw_Right_w, d4);
                    }
                    // .no_new_bottom
                    Mem.ww(draw_OffLeftBy_w, 0);
                    d6 = setw(0, d2);
                    if ((short) d6 < (short) Mem.uw(draw_LeftClipB_w)) {
                        d6 = setw(0, Mem.uw(draw_LeftClipB_w));
                        d6 = setw(d6, d6 - d2);
                        Mem.ww(draw_OffLeftBy_w, d6);
                        d6 = setw(d6, d6 + d2);
                    }
                    // .okt
                    Mem.wb(drawit, 0xFF);
                    d6 = setw(d6, d6 + d6);
                    a4 = a4 + (short) d6 * 8;
                    d6 = setw(d6, ((short) d6) >> 1);
                    if ((short) d6 < (short) Mem.uw(draw_Left_w)) Mem.ww(draw_Left_w, d6);
                    // .no_new_top
                    d5 = setw(d5, d5 - d3);             // sub.w d3,d5  (dy)
                    d3 = swap(d3);
                    d3 = setw(d3, 0);                   // d3 = y accumulator
                    d4 = setw(d4, d4 - d2);             // dx > 0
                    d4 = (short) d4;
                    d5 = swap(d5);
                    d5 = setw(d5, 0);
                    d5 = d5 / d4;                       // divs.l d4,d5
                    d2 = setb(0, Mem.ub(a1 + 6));       // move.b 6(a1),d2  (u order inversé vs bottom)
                    d6 = setb(0, Mem.ub(a1 + 2));       // move.b 2(a1),d6
                    d2 = setw(d2, d2 - d6);
                    d2 = swap(d2);
                    d6 = swap(d6);
                    d2 = setw(d2, 0);
                    d6 = setw(d6, 0);
                    d2 = d2 / d4;                       // divs.l d4,d2
                    a5 = d5;                            // a5 = dy const
                    a6 = d2;                            // a6 = du const
                    d5 = setb(0, Mem.ub(a1 + 7));       // move.b 7(a1),d5
                    d2 = setb(0, Mem.ub(a1 + 3));       // move.b 3(a1),d2
                    d5 = setw(d5, d5 - d2);
                    d2 = swap(d2);
                    d5 = swap(d5);
                    d2 = setw(d2, 0);
                    d5 = setw(d5, 0);
                    d5 = d5 / d4;                       // divs.l d4,d5
                    d4 = setw(d4, d4 + pushClip);       // add.w (a7)+,d4
                    d4 = setw(d4, d4 - Mem.uw(draw_OffLeftBy_w));
                    if ((short) d4 < 0) flat = true;    // blt this_line_flat
                    else {
                        if (Mem.w(draw_OffLeftBy_w) != 0) {
                            int sd4 = d4;
                            d4 = setw(0, Mem.uw(draw_OffLeftBy_w));
                            d4 = setw(d4, d4 - 1);
                            while ((short) d4 != -1) {
                                d3 = d3 + a5;
                                d6 = d6 + a6;
                                d2 = d2 + d5;
                                d4 = setw(d4, d4 - 1);
                            }
                            d4 = setw(sd4, sd4);
                        }
                        while (true) {                  // .put_in_line
                            d3 = swap(d3);
                            Mem.ww(a4, d3);
                            a4 += 2;
                            d3 = swap(d3);
                            Mem.wl(a4, d6);
                            a4 += 4;
                            Mem.wl(a4, d2);
                            a4 += 4;
                            a4 += 6;
                            d3 = d3 + a5;
                            d6 = d6 + a6;
                            d2 = d2 + d5;
                            d4 = setw(d4, d4 - 1);
                            if ((short) d4 == -1) break;
                        }
                        flat = true;
                    }
                }
            }

            // this_line_flat
            a1 += 4;                                    // addq #4,a1
            d7 = setw(d7, d7 - 1);                      // dbra d7,draw_PutInLines
            if ((short) d7 == -1) break;
        }
        a1 += 4;                                        // addq #4,a1
        return a1;                                      // rts
    }

    // ==================================================================
    //  draw_PutInLinesGouraud (objdrawhires.s:3359) — comme PutInLines + interpole
    //  la brightness des sommets (boxbrights_vw) → champ @10 du record.
    // ==================================================================
    static int draw_PutInLinesGouraud(int a1, int d7) {
        int d0, d1, d2, d3, d4, d5, d6;
        int a2 = boxbrights_vw, a3 = draw_2DPointsProjected_vl, a4, a5, a6;
        while (true) {                                 // piglloop
            d0 = setw(0, Mem.uw(a1));                  // move.w (a1),d0
            d1 = setw(0, Mem.uw(a1 + 4));              // move.w 4(a1),d1
            d2 = setw(0, Mem.uw(a3 + (d0 & 0xFFFF) * 4));     // x1
            d3 = setw(0, Mem.uw(a3 + (d0 & 0xFFFF) * 4 + 2)); // y1
            d4 = setw(0, Mem.uw(a3 + (d1 & 0xFFFF) * 4));     // x2
            d5 = setw(0, Mem.uw(a3 + (d1 & 0xFFFF) * 4 + 2)); // y2

            boolean flat = false;
            boolean onTop;
            if ((short) d4 == (short) d2) { flat = true; onTop = false; } // beq this_line_flat_gouraud
            else if ((short) d4 > (short) d2) { onTop = true; }           // bgt this_line_on_top_gouraud
            else {                                                        // bottom edge
                onTop = false;
                a4 = draw_PolyBotTab_vw;
                int t = d2; d2 = d4; d4 = t;            // exg d2,d4
                t = d3; d3 = d5; d5 = t;                // exg d3,d5
                if ((short) d2 >= (short) Mem.uw(draw_RightClipB_w)) flat = true;
                else if ((short) d4 <= (short) Mem.uw(draw_LeftClipB_w)) flat = true;
                else {
                    int pushClip;
                    d6 = setw(0, Mem.uw(draw_RightClipB_w));
                    d6 = setw(d6, d6 - d4);
                    if ((short) d6 <= 0) {
                        pushClip = d6;
                        Mem.ww(draw_Right_w, Mem.uw(draw_RightClipB_w));
                        Mem.ww(draw_Right_w, Mem.uw(draw_Right_w) - 1);
                    } else {
                        pushClip = 0;
                        if ((short) d4 > (short) Mem.uw(draw_Right_w)) Mem.ww(draw_Right_w, d4);
                    }
                    Mem.ww(draw_OffLeftBy_w, 0);
                    d6 = setw(0, d2);
                    if ((short) d6 < (short) Mem.uw(draw_LeftClipB_w)) {
                        d6 = setw(0, Mem.uw(draw_LeftClipB_w));
                        d6 = setw(d6, d6 - d2);
                        Mem.ww(draw_OffLeftBy_w, d6);
                        d6 = setw(d6, d6 + d2);
                    }
                    Mem.wb(drawit, 0xFF);
                    d6 = setw(d6, d6 + d6);
                    a4 = a4 + (short) d6 * 8;
                    d6 = setw(d6, ((short) d6) >> 1);
                    if ((short) d6 < (short) Mem.uw(draw_Left_w)) Mem.ww(draw_Left_w, d6);
                    d5 = setw(d5, d5 - d3);             // dy
                    d3 = swap(d3);
                    d3 = setw(d3, 0);
                    d4 = setw(d4, d4 - d2);             // dx
                    d4 = (short) d4;
                    d5 = swap(d5);
                    d5 = setw(d5, 0);
                    d5 = d5 / d4;                       // divs.l d4,d5
                    d2 = setb(0, Mem.ub(a1 + 2));       // move.b 2(a1),d2
                    d6 = setb(0, Mem.ub(a1 + 6));       // move.b 6(a1),d6
                    d2 = setw(d2, d2 - d6);
                    d2 = swap(d2);
                    d6 = swap(d6);
                    d2 = setw(d2, 0);
                    d6 = setw(d6, 0);
                    d2 = d2 / d4;                       // divs.l d4,d2
                    a5 = d5;                            // a5 = dy const
                    a6 = d2;                            // a6 = du const
                    d5 = setb(0, Mem.ub(a1 + 3));       // move.b 3(a1),d5
                    d2 = setb(0, Mem.ub(a1 + 7));       // move.b 7(a1),d2
                    d5 = setw(d5, d5 - d2);
                    d2 = swap(d2);
                    d5 = swap(d5);
                    d2 = setw(d2, 0);
                    d5 = setw(d5, 0);
                    d5 = d5 / d4;                       // divs.l d4,d5
                    d1 = setw(0, Mem.uw(a2 + (d1 & 0xFFFF) * 2)); // move.w (a2,d1.w*2),d1  (bright2)
                    d0 = setw(0, Mem.uw(a2 + (d0 & 0xFFFF) * 2)); // move.w (a2,d0.w*2),d0  (bright1)
                    d0 = setw(d0, d0 - d1);             // sub.w d1,d0
                    d0 = swap(d0);
                    d1 = swap(d1);
                    d0 = setw(d0, 0);
                    d1 = setw(d1, 0);                   // d1 = bright accumulator
                    d0 = d0 / d4;                       // divs.l d4,d0  (dbright step)
                    d4 = setw(d4, d4 + pushClip);       // add.w (a7)+,d4
                    d4 = setw(d4, d4 - Mem.uw(draw_OffLeftBy_w));
                    if ((short) d4 < 0) flat = true;    // blt this_line_flat_gouraud
                    else {
                        if (Mem.w(draw_OffLeftBy_w) != 0) {
                            int sd4 = d4;
                            d4 = setw(0, Mem.uw(draw_OffLeftBy_w));
                            d4 = setw(d4, d4 - 1);
                            while ((short) d4 != -1) {  // .calc_no_draw
                                d1 = d1 + d0;           // add.l d0,d1
                                d3 = d3 + a5;
                                d6 = d6 + a6;
                                d2 = d2 + d5;
                                d4 = setw(d4, d4 - 1);
                            }
                            d4 = setw(sd4, sd4);
                        }
                        while (true) {                  // .put_in_line
                            d3 = swap(d3);
                            Mem.ww(a4, d3);
                            a4 += 2;
                            d3 = swap(d3);
                            Mem.wl(a4, d6);
                            a4 += 4;
                            Mem.wl(a4, d2);
                            a4 += 4;
                            d1 = swap(d1);
                            Mem.ww(a4, d1);             // move.w d1,(a4)  (brightness @10)
                            a4 += 6;                    // addq #6,a4
                            d1 = swap(d1);
                            d1 = d1 + d0;               // add.l d0,d1
                            d3 = d3 + a5;
                            d6 = d6 + a6;
                            d2 = d2 + d5;
                            d4 = setw(d4, d4 - 1);
                            if ((short) d4 == -1) break;
                        }
                        flat = true;
                    }
                }
            }

            if (!flat && onTop) {
                // this_line_on_top_gouraud (3516)
                a4 = draw_PolyTopTab_vw;
                if ((short) d2 >= (short) Mem.uw(draw_RightClipB_w)) flat = true;
                else if ((short) d4 <= (short) Mem.uw(draw_LeftClipB_w)) flat = true;
                else {
                    int pushClip;
                    d6 = setw(0, Mem.uw(draw_RightClipB_w));
                    d6 = setw(d6, d6 - d4);
                    if ((short) d6 <= 0) {
                        pushClip = d6;
                        Mem.ww(draw_Right_w, Mem.uw(draw_RightClipB_w));
                        Mem.ww(draw_Right_w, Mem.uw(draw_Right_w) - 1);
                    } else {
                        pushClip = 0;
                        if ((short) d4 > (short) Mem.uw(draw_Right_w)) Mem.ww(draw_Right_w, d4);
                    }
                    Mem.ww(draw_OffLeftBy_w, 0);
                    d6 = setw(0, d2);
                    if ((short) d6 < (short) Mem.uw(draw_LeftClipB_w)) {
                        d6 = setw(0, Mem.uw(draw_LeftClipB_w));
                        d6 = setw(d6, d6 - d2);
                        Mem.ww(draw_OffLeftBy_w, d6);
                        d6 = setw(d6, d6 + d2);
                    }
                    Mem.wb(drawit, 0xFF);
                    d6 = setw(d6, d6 + d6);
                    a4 = a4 + (short) d6 * 8;
                    d6 = setw(d6, ((short) d6) >> 1);
                    if ((short) d6 < (short) Mem.uw(draw_Left_w)) Mem.ww(draw_Left_w, d6);
                    d5 = setw(d5, d5 - d3);             // dy
                    d3 = swap(d3);
                    d3 = setw(d3, 0);
                    d4 = setw(d4, d4 - d2);             // dx
                    d4 = (short) d4;
                    d5 = swap(d5);
                    d5 = setw(d5, 0);
                    d5 = d5 / d4;
                    d2 = setb(0, Mem.ub(a1 + 6));       // move.b 6(a1),d2
                    d6 = setb(0, Mem.ub(a1 + 2));       // move.b 2(a1),d6
                    d2 = setw(d2, d2 - d6);
                    d2 = swap(d2);
                    d6 = swap(d6);
                    d2 = setw(d2, 0);
                    d6 = setw(d6, 0);
                    d2 = d2 / d4;
                    a5 = d5;
                    a6 = d2;
                    d5 = setb(0, Mem.ub(a1 + 7));       // move.b 7(a1),d5
                    d2 = setb(0, Mem.ub(a1 + 3));       // move.b 3(a1),d2
                    d5 = setw(d5, d5 - d2);
                    d2 = swap(d2);
                    d5 = swap(d5);
                    d2 = setw(d2, 0);
                    d5 = setw(d5, 0);
                    d5 = d5 / d4;
                    d1 = setw(0, Mem.uw(a2 + (d1 & 0xFFFF) * 2)); // move.w (a2,d1.w*2),d1
                    d0 = setw(0, Mem.uw(a2 + (d0 & 0xFFFF) * 2)); // move.w (a2,d0.w*2),d0
                    d1 = setw(d1, d1 - d0);             // sub.w d0,d1  (ordre inversé vs bottom)
                    d0 = swap(d0);
                    d1 = swap(d1);
                    d0 = setw(d0, 0);
                    d1 = setw(d1, 0);
                    d1 = d1 / d4;                       // divs.l d4,d1  (dbright step dans d1)
                    d4 = setw(d4, d4 + pushClip);       // add.w (a7)+,d4
                    d4 = setw(d4, d4 - Mem.uw(draw_OffLeftBy_w));
                    if ((short) d4 < 0) flat = true;
                    else {
                        if (Mem.w(draw_OffLeftBy_w) != 0) {
                            int sd4 = d4;
                            d4 = setw(0, Mem.uw(draw_OffLeftBy_w));
                            d4 = setw(d4, d4 - 1);
                            while ((short) d4 != -1) {  // .calc_no_draw
                                d0 = d0 + d1;           // add.l d1,d0
                                d3 = d3 + a5;
                                d6 = d6 + a6;
                                d2 = d2 + d5;
                                d4 = setw(d4, d4 - 1);
                            }
                            d4 = setw(sd4, sd4);
                        }
                        while (true) {                  // .put_in_line
                            d3 = swap(d3);
                            Mem.ww(a4, d3);
                            a4 += 2;
                            d3 = swap(d3);
                            Mem.wl(a4, d6);
                            a4 += 4;
                            Mem.wl(a4, d2);
                            a4 += 4;
                            d0 = swap(d0);
                            Mem.ww(a4, d0);             // move.w d0,(a4)  (brightness @10)
                            a4 += 6;
                            d0 = swap(d0);
                            d0 = d0 + d1;               // add.l d1,d0
                            d3 = d3 + a5;
                            d6 = d6 + a6;
                            d2 = d2 + d5;
                            d4 = setw(d4, d4 - 1);
                            if ((short) d4 == -1) break;
                        }
                        flat = true;
                    }
                }
            }

            // this_line_flat_gouraud
            a1 += 4;
            d7 = setw(d7, d7 - 1);
            if ((short) d7 == -1) break;               // dbra d7,piglloop
        }
        a1 += 4;
        return a1;                                      // rts
    }
}
