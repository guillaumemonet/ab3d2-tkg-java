package ab3d2;

import ab3d2.modules.DevInst;

import static ab3d2.Defs.*;
import static ab3d2.HireswallData.*;
import static ab3d2.HiresData.Vid_CentreX_w;
import static ab3d2.M68k.extw;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;
import static ab3d2.bss.TablesBss.ConstantTable_vl;
import static ab3d2.bss.TablesBss.DataBuffer1_vl;
import static ab3d2.bss.TablesBss.DataBuffer2_vl;
import static ab3d2.bss.TablesBss.Storage_vl;
import static ab3d2.bss.SystemBss.Sys_Workspace_vl;
import static ab3d2.bss.VidBss.Vid_DoubleHeight_b;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.data.TablesData.DivThreeTable_vb;

/**
 * Traduction littérale de ab3d2_source/hiresgourwall.s
 *
 * Variante gouraud du rasterizer de murs (hireswall.s) : la brightness varie
 * verticalement, différemment en haut (Upper*) et en bas (Lower*) de chaque
 * bande. La luminosité est interpolée le long de la bande (gouraud horizontal)
 * ET le long de la hauteur (draw_GouraudStep/Start, table de scale via la
 * palette a4). Aussi utilisé pour les sols.
 *
 * Records de 26 octets dans Sys_Workspace : {x(w), bm(l), dist(l), top(l),
 * bot(l), leftBrightScaled(l), upperLeftBrightScaled(l)}.
 * Strip drawers gouraud locaux : drawwallPACK0G/1G/2G (variante générique 030+
 * ; la macro OPT060 n'est pas portée — build 030+).
 *
 * CODE MORT (jamais exécuté, dispatch Vid_DoubleWidth_b commenté) :
 * scrdrawlopGDOUB, scrdrawlopGBDOUB, usesimpleG/cliptopusesimpleG/simplewall*G,
 * et la version macro OPT060 de drawwallPACKnG — non traduits.
 */
public final class Hiresgourwall {

    private static final int _a0 = Mem.align(4);
    public static final int draw_GouraudStep_l = Mem.dcL(0);
    public static final int draw_GouraudStart_l = Mem.dcL(0);

    private Hiresgourwall() {
    }

    public static int dbgForceWallPen = 0; // TMP : si >0, force la couleur écrite par les pack writers gouraud

    private static int Draw_ChunkPtr_l() {
        return ab3d2.bss.DrawBss.Draw_ChunkPtr_l;
    }

    private static int Draw_PalettePtr_l() {
        return ab3d2.bss.DrawBss.Draw_PalettePtr_l;
    }

    private static int Draw_WallTexturePtrs_vl() {
        return ab3d2.bss.DrawBss.Draw_WallTexturePtrs_vl;
    }

    // ------------------------------------------------------------------
    // DoleftendGOUR / screendivide — subdivision (records 26o).
    // a0 = structure WD (conservée tout au long : accumulateurs brightness
    // en mémoire à WD_LeftBrightScaled_l/WD_UpperLeftBrightScaled_l).
    // ------------------------------------------------------------------

    /** DoleftendGOUR — a0 = WD. */
    private static void DoleftendGOUR(int a0) {
        int d0 = Mem.uw(Draw_LeftClip_w);              // move.w Draw_LeftClip_w,d0
        d0 = setw(d0, d0 - 1);                         // sub.w #1,d0
        Mem.ww(Draw_LeftClipAndLast_w, d0);            // move.w d0,Draw_LeftClipAndLast_w
        d0 = Mem.uw(a0);                               // move.w (a0),d0 ; WD_LeftX_w
        int d1 = Mem.uw(a0 + WD_RightX_w);             // move.w WD_RightX_w(a0),d1
        d1 = setw(d1, d1 - d0);                        // sub.w d0,d1
        if ((short) d1 < 0) {                          // bge.s .some_to_draw
            return;                                    // rts
        }

        // .some_to_draw:
        int d7 = Mem.l(draw_IterationTable_vw + (short) d1 * 4); // move.l draw_IterationTable_vw(pc,d1.w*4),d7
        d0 = swap(d0);                                 // swap d0 ; leftx in high word
        int d6 = setw(0, d7);                          // move.w d7,d6 ; shift
        d0 = d0 & 0xFFFF0000;                          // clr.w d0
        d1 = swap(d1);                                 // swap d1 ; width in high
        d7 = swap(d7);                                 // swap d7
        d1 = d1 & 0xFFFF0000;                          // clr.w d1
        d1 = d1 >> (d6 & 63);                          // asr.l d6,d1
        Mem.wl(a0, d1);                                // move.l d1,(a0) ; WD_DWidth_l

        d1 = 0;                                        // moveq #0,d1
        d1 = setw(d1, Mem.uw(a0 + WD_LeftBM_w));       // move.w WD_LeftBM_w(a0),d1
        int d2 = 0;                                    // moveq #0,d2
        d2 = setw(d2, Mem.uw(a0 + WD_RightBM_w));      // move.w WD_RightBM_w(a0),d2
        d2 = setw(d2, d2 - d1);                        // sub.w d1,d2
        d1 = swap(d1);                                 // swap d1
        d2 = swap(d2);                                 // swap d2
        d2 = d2 >> (d6 & 63);                          // asr.l d6,d2
        Mem.wl(a0 + WD_DBM_l, d2);                     // move.l d2,WD_DBM_l(a0)

        d2 = 0;                                        // moveq #0,d2
        d2 = setw(d2, Mem.uw(a0 + WD_LeftDist_w));     // move.w WD_LeftDist_w(a0),d2
        int d3 = 0;                                    // moveq #0,d3
        d3 = setw(d3, Mem.uw(a0 + WD_RightDist_w));    // move.w WD_RightDist_w(a0),d3
        d3 = setw(d3, d3 - d2);                        // sub.w d2,d3
        d2 = swap(d2);                                 // swap d2
        d3 = swap(d3);                                 // swap d3
        d3 = d3 >> (d6 & 63);                          // asr.l d6,d3
        Mem.wl(a0 + WD_DDist_l, d3);                   // move.l d3,WD_DDist_l(a0)

        d3 = 0;                                        // moveq #0,d3
        d3 = setw(d3, Mem.uw(a0 + WD_LeftTop_w));      // move.w WD_LeftTop_w(a0),d3
        int d4 = 0;                                    // moveq #0,d4
        d4 = setw(d4, Mem.uw(a0 + WD_RightTop_w));     // move.w WD_RightTop_w(a0),d4
        d4 = setw(d4, d4 - d3);                        // sub.w d3,d4
        d3 = swap(d3);                                 // swap d3
        d4 = swap(d4);                                 // swap d4
        d4 = d4 >> (d6 & 63);                          // asr.l d6,d4
        Mem.wl(a0 + WD_DTop_l, d4);                    // move.l d4,WD_DTop_l(a0)

        d4 = 0;                                        // moveq #0,d4
        d4 = setw(d4, Mem.uw(a0 + WD_LeftBot_w));      // move.w WD_LeftBot_w(a0),d4
        int d5 = 0;                                    // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0 + WD_RightBot_w));     // move.w WD_RightBot_w(a0),d5
        d5 = setw(d5, d5 - d4);                        // sub.w d4,d5
        d4 = swap(d4);                                 // swap d4
        d5 = swap(d5);                                 // swap d5
        d5 = d5 >> (d6 & 63);                          // asr.l d6,d5
        Mem.wl(a0 + WD_DBot_l, d5);                    // move.l d5,WD_DBot_l(a0)

        // *** Gouraud shading *** (bas)
        d5 = 0;                                        // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0 + WD_RightBright_w));  // move.w WD_RightBright_w(a0),d5
        d5 = setw(d5, d5 - Mem.uw(a0 + WD_LeftBright_w)); // sub.w WD_LeftBright_w(a0),d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d5 = swap(d5);                                 // swap d5
        d5 = d5 >> (d6 & 63);                          // asr.l d6,d5
        Mem.wl(a0 + WD_DHorizBright_l, d5);            // move.l d5,WD_DHorizBright_l(a0)

        d5 = 0;                                        // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0 + WD_LeftBright_w));   // move.w WD_LeftBright_w(a0),d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d5 = swap(d5);                                 // swap d5
        Mem.wl(a0 + WD_LeftBrightScaled_l, d5);        // move.l d5,WD_LeftBrightScaled_l(a0) ; 24(a0)

        // *** Extra Gouraud Shading *** (haut)
        d5 = 0;                                        // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0 + WD_UpperRightBright_w)); // move.w WD_UpperRightBright_w(a0),d5
        d5 = setw(d5, d5 - Mem.uw(a0 + WD_UpperLeftBright_w)); // sub.w WD_UpperLeftBright_w(a0),d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d5 = swap(d5);                                 // swap d5
        d5 = d5 >> (d6 & 63);                          // asr.l d6,d5
        Mem.wl(a0 + WD_DUpperHorizBright_l, d5);       // move.l d5,WD_DUpperHorizBright_l(a0)

        d5 = 0;                                        // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0 + WD_UpperLeftBright_w)); // move.w WD_UpperLeftBright_w(a0),d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d5 = swap(d5);                                 // swap d5
        Mem.wl(a0 + WD_UpperLeftBrightScaled_l, d5);   // move.l d5,WD_UpperLeftBrightScaled_l(a0) ; 32(a0)

        // Préparation des bandes
        d7 = d7 | 0xFFFF0000;                          // or.l #$ffff0000,d7
        d6 = Mem.w(Draw_LeftClipAndLast_w);            // move.w Draw_LeftClipAndLast_w(pc),d6
        int a2 = Sys_Workspace_vl;                     // move.l #Sys_Workspace_vl,a2

        int a3 = Mem.l(a0);                            // move.l (a0),a3 ; dwidth
        int a4 = Mem.l(a0 + WD_DBM_l);                 // move.l WD_DBM_l(a0),a4
        int a5 = Mem.l(a0 + WD_DDist_l);               // move.l WD_DDist_l(a0),a5
        int a6 = Mem.l(a0 + WD_DTop_l);                // move.l WD_DTop_l(a0),a6
        int a1 = Mem.l(a0 + WD_DBot_l);                // move.l WD_DBot_l(a0),a1
        // (a0 reste = WD struct : brightness accumulés en mémoire)

        while (true) { // .scr_divide_loop:
            d0 = swap(d0);                             // swap d0
            if ((short) d0 > (short) d6) {             // cmp.w d6,d0 ; bgt .scr_not_off_left
                // .scr_not_off_left:
                d6 = setw(d6, d0);                     // move.w d0,d6
                if ((short) d0 >= Mem.w(Draw_RightClip_w)) { // cmp.w Draw_RightClip_w(pc),d0 ; bge.s .out_of_calc
                    break;
                }
                // .scr_not_off_right: stocke la bande gauche courante
                Mem.ww(a2, d0); a2 += 2;               // move.w d0,(a2)+
                Mem.wl(a2, d1); a2 += 4;               // move.l d1,(a2)+
                Mem.wl(a2, d2); a2 += 4;               // move.l d2,(a2)+
                Mem.wl(a2, d3); a2 += 4;               // move.l d3,(a2)+
                Mem.wl(a2, d4); a2 += 4;               // move.l d4,(a2)+
                Mem.wl(a2, Mem.l(a0 + WD_LeftBrightScaled_l)); a2 += 4;      // move.l WD_LeftBrightScaled_l(a0),(a2)+
                Mem.wl(a2, Mem.l(a0 + WD_UpperLeftBrightScaled_l)); a2 += 4; // move.l WD_UpperLeftBrightScaled_l(a0),(a2)+

                d0 = swap(d0);                         // swap d0
                d0 += a3;                              // add.l a3,d0
                d1 += a4;                              // add.l a4,d1
                d2 += a5;                              // add.l a5,d2
                d3 += a6;                              // add.l a6,d3
                d4 += a1;                              // add.l a1,d4
                d5 = Mem.l(a0 + WD_DHorizBright_l);    // move.l WD_DHorizBright_l(a0),d5
                Mem.wl(a0 + WD_LeftBrightScaled_l, Mem.l(a0 + WD_LeftBrightScaled_l) + d5); // add.l d5,WD_LeftBrightScaled_l(a0)
                d5 = Mem.l(a0 + WD_DUpperHorizBright_l); // move.l WD_DUpperHorizBright_l(a0),d5
                Mem.wl(a0 + WD_UpperLeftBrightScaled_l, Mem.l(a0 + WD_UpperLeftBrightScaled_l) + d5); // add.l d5,WD_UpperLeftBrightScaled_l(a0)
                d7 += 0x10000;                         // add.l #$10000,d7
                d7 = setw(d7, d7 - 1);                 // dbra d7,.scr_divide_loop
                if ((short) d7 == -1) {
                    break;
                }
                continue;
            }
            d0 = swap(d0);                             // swap d0
            d1 += a4;                                  // add.l a4,d1
            d2 += a5;                                  // add.l a5,d2
            d3 += a6;                                  // add.l a6,d3
            d4 += a1;                                  // add.l a1,d4
            d0 += a3;                                  // add.l a3,d0
            d5 = Mem.l(a0 + WD_DHorizBright_l);        // move.l WD_DHorizBright_l(a0),d5
            Mem.wl(a0 + WD_LeftBrightScaled_l, Mem.l(a0 + WD_LeftBrightScaled_l) + d5); // add.l d5,WD_LeftBrightScaled_l(a0)
            d5 = Mem.l(a0 + WD_DUpperHorizBright_l);   // move.l WD_DUpperHorizBright_l(a0),d5
            Mem.wl(a0 + WD_UpperLeftBrightScaled_l, Mem.l(a0 + WD_UpperLeftBrightScaled_l) + d5); // add.l d5,...
            d7 = setw(d7, d7 - 1);                     // dbra d7,.scr_divide_loop
            if ((short) d7 == -1) {
                return;                                // rts
            }
        }

        // .out_of_calc:
        d7 = swap(d7);                                 // swap d7
        if ((short) d7 < 0) {                          // tst.w d7 ; bge.s .something_to_draw
            return;                                    // rts
        }
        // .something_to_draw: a1 = ConstantTable_vl, a0 = Sys_Workspace_vl
        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; bne scrdrawlopGB
            scrdrawlopGB(Sys_Workspace_vl, d7);
            return;
        }
        scrDrawLoop(Sys_Workspace_vl, d7);             // bra .scr_draw_loop
    }

    /** .scr_draw_loop — petit écran. */
    private static void scrDrawLoop(int a0, int d7) {
        while (true) {
            int d0 = Mem.uw(a0); a0 += 2;              // move.w (a0)+,d0
            if ((short) d0 == Mem.w(draw_WallLastStripX_w)) { // cmp.w draw_WallLastStripX_w,d0 ; beq.s .this_line_done
                // .this_line_done:
                a0 += 4 + 4 + 4 + 4 + 4 + 4;           // add.w #24,a0
                d7 = setw(d7, d7 - 1);                 // dbra d7,.scr_draw_loop
                if ((short) d7 == -1) {
                    return;
                }
                continue;
            }
            Mem.ww(draw_WallLastStripX_w, d0);         // move.w d0,draw_WallLastStripX_w
            int a3 = Mem.l(Vid_FastBufferPtr_l);       // move.l Vid_FastBufferPtr_l,a3
            a3 = a3 + (short) d0;                      // lea (a3,d0.w),a3
            int d1 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d1
            d1 = swap(d1);                             // swap d1
            int d6 = setw(0, d1);                      // move.w d1,d6
            d6 = setw(d6, d6 & Mem.uw(draw_WallTextureWidthMask_w)); // and.w draw_WallTextureWidthMask_w,d6
            int d2 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d2
            d2 = swap(d2);                             // swap d2
            d6 = setw(d6, d6 + Mem.uw(draw_FromTile_w)); // add.w draw_FromTile_w(pc),d6
            d6 = setw(d6, d6 + d6);                    // add.w d6,d6
            int a5 = (short) d6;                       // move.w d6,a5
            int d3 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d3
            d3 = swap(d3);                             // swap d3
            a5 += DivThreeTable_vb;                    // add.l #DivThreeTable_vb,a5
            Mem.ww(draw_StripData_w, Mem.uw(a5));      // move.w (a5),draw_StripData_w
            a5 = Mem.l(Draw_ChunkPtr_l());             // move.l Draw_ChunkPtr_l,a5
            d6 = 0;                                    // moveq #0,d6
            d6 = setb(d6, Mem.ub(draw_StripData_w));   // move.b draw_StripData_w,d6
            d6 = setw(d6, d6 + d6);                    // add.w d6,d6
            int d4 = Mem.uw(draw_WallTextureHeightShift_w); // move.w draw_WallTextureHeightShift_w,d4
            d6 = d6 << (d4 & 63);                      // asl.l d4,d6
            a5 += d6;                                  // add.l d6,a5
            d4 = Mem.l(a0); a0 += 4;                   // move.l (a0)+,d4
            d4 = swap(d4);                             // swap d4
            d4 = setw(d4, d4 + 1);                     // addq #1,d4
            d6 = setw(d6, d2);                         // move.w d2,d6

            d6 = setw(d6, ((short) d6) >> 7);          // asr.w #7,d6 ; old version

            int d5 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d5 ; lower bright
            d5 = swap(d5);                             // swap d5
            // move.w d7,-(a7) ; save (d7 = compteur, géré localement)
            int d7save = d7;
            d5 = extw(d5);                             // ext.w d5 (étend l'octet bas)
            d7 = setw(d7, d6);                         // move.w d6,d7
            d7 = setw(d7, d7 + d5);                    // add.w d5,d7
            if ((short) d7 < 0) {                      // bge.s .br_not_negative
                d7 = setw(d7, 0);                      // moveq #0,d7
            }
            // .br_not_negative:
            if ((short) d7 >= 62) {                    // cmp.w #62,d7 ; blt.s .br_not_positive
                d7 = setw(d7, 62);                     // move.w #62,d7
            }
            // .br_not_positive:
            d5 = Mem.l(a0); a0 += 4;                   // move.l (a0)+,d5 ; upper bright
            d5 = swap(d5);                             // swap d5
            d5 = extw(d5);                             // ext.w d5 (étend l'octet bas)
            d6 = setw(d6, d6 + d5);                    // add.w d5,d6
            if ((short) d6 < 0) {                      // bge.s .br_not_negative_2
                d6 = setw(d6, 0);                      // moveq #0,d6
            }
            // .br_not_negative_2:
            if ((short) d6 >= 62) {                    // cmp.w #62,d6 ; blt.s .br_not_positive_2
                d6 = setw(d6, 62);                     // move.w #62,d6
            }
            // .br_not_positive_2:
            d6 = setw(d6, ((short) d6) >> 1);          // asr.w #1,d6
            d7 = setw(d7, ((short) d7) >> 1);          // asr.w #1,d7
            d7 = setw(d7, d7 - d6);                    // sub.w d6,d7
            int a4 = Mem.l(Draw_PalettePtr_l());       // move.l Draw_PalettePtr_l,a4
            draw_ScreenWallStripGouraud(d2, d3, d4, d6, d7, a3, a4, a5); // bsr draw_ScreenWallStripGouraud

            d7 = d7save;                               // move.w (a7)+,d7
            // .too_small:
            d7 = setw(d7, d7 - 1);                     // dbra d7,.scr_draw_loop
            if ((short) d7 == -1) {
                return;
            }
        }
    }

    /** scrdrawlopGB — plein écran. */
    private static void scrdrawlopGB(int a0, int d7) {
        while (true) {
            int d0 = Mem.uw(a0); a0 += 2;              // move.w (a0)+,d0
            int a3 = Mem.l(Vid_FastBufferPtr_l);       // move.l Vid_FastBufferPtr_l,a3
            a3 = a3 + (short) d0;                      // lea (a3,d0.w),a3
            int d1 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d1
            d1 = swap(d1);                             // swap d1
            int d6 = setw(0, d1);                      // move.w d1,d6
            d6 = setw(d6, d6 & Mem.uw(draw_WallTextureWidthMask_w)); // and.w draw_WallTextureWidthMask_w,d6
            int d2 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d2
            d2 = swap(d2);                             // swap d2
            d6 = setw(d6, d6 + Mem.uw(draw_FromTile_w)); // add.w draw_FromTile_w(pc),d6
            d6 = setw(d6, d6 + d6);                    // add.w d6,d6
            int a5 = (short) d6;                       // move.w d6,a5
            int d3 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d3
            d3 = swap(d3);                             // swap d3
            a5 += DivThreeTable_vb;                    // add.l #DivThreeTable_vb,a5
            Mem.ww(draw_StripData_w, Mem.uw(a5));      // move.w (a5),draw_StripData_w
            a5 = Mem.l(Draw_ChunkPtr_l());             // move.l Draw_ChunkPtr_l,a5
            d6 = 0;                                    // moveq #0,d6
            d6 = setb(d6, Mem.ub(draw_StripData_w));   // move.b draw_StripData_w,d6
            d6 = setw(d6, d6 + d6);                    // add.w d6,d6
            int d4 = Mem.uw(draw_WallTextureHeightShift_w); // move.w draw_WallTextureHeightShift_w,d4
            d6 = d6 << (d4 & 63);                      // asl.l d4,d6
            a5 += d6;                                  // add.l d6,a5
            d4 = Mem.l(a0); a0 += 4;                   // move.l (a0)+,d4
            d4 = swap(d4);                             // swap d4
            d4 = setw(d4, d4 + 1);                     // addq #1,d4
            d6 = setw(d6, d2);                         // move.w d2,d6

            d6 = setw(d6, ((short) d6) >> 7);          // asr.w #7,d6

            int d5 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d5
            d5 = swap(d5);                             // swap d5
            int d7save = d7;                           // move.w d7,-(a7)
            d5 = extw(d5);                             // ext.w d5 (étend l'octet bas)
            d7 = setw(d7, d6);                         // move.w d6,d7
            d7 = setw(d7, d7 + d5);                    // add.w d5,d7
            if ((short) d7 < 0) {                      // bge.s .br_not_negative
                d7 = setw(d7, 0);                      // moveq #0,d7
            }
            // .br_not_negative:
            if ((short) d7 >= 62) {                    // cmp.w #62,d7 ; blt.s .br_not_positive
                d7 = setw(d7, 62);                     // move.w #62,d7
            }
            // .br_not_positive:
            d5 = Mem.l(a0); a0 += 4;                   // move.l (a0)+,d5
            d5 = swap(d5);                             // swap d5
            d5 = extw(d5);                             // ext.w d5 (étend l'octet bas)
            d6 = setw(d6, d6 + d5);                    // add.w d5,d6
            if ((short) d6 < 0) {                      // bge.s .br_not_negative_2
                d6 = setw(d6, 0);                      // moveq #0,d6
            }
            // .br_not_negative_2:
            if ((short) d6 >= 62) {                    // cmp.w #62,d6 ; blt.s .br_not_positive_2
                d6 = setw(d6, 62);                     // move.w #62,d6
            }
            // .br_not_positive_2:
            d6 = setw(d6, ((short) d6) >> 1);          // asr.w #1,d6
            d7 = setw(d7, ((short) d7) >> 1);          // asr.w #1,d7
            d7 = setw(d7, d7 - d6);                    // sub.w d6,d7
            int a4 = Mem.l(Draw_PalettePtr_l());       // move.l Draw_PalettePtr_l,a4
            ScreenWallstripdrawGOURB(d2, d3, d4, d6, d7, a3, a4, a5); // bsr ScreenWallstripdrawGOURB

            d7 = d7save;                               // move.w (a7)+,d7
            d7 = setw(d7, d7 - 1);                     // dbra d7,scrdrawlopGB
            if ((short) d7 == -1) {
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // draw_WallGouraudShaded — entrée (subdivision DataBuffer, records 28o).
    // ------------------------------------------------------------------

    /** draw_WallGouraudShaded — a0=x1, d1=z1, a2=x2, d3=z2, d4=startLen, d5=endLen, d6=count, d7=iterations. */
    public static void draw_WallGouraudShaded(int a0in, int d1, int a2in, int d3, int d4, int d5, int d6, int d7) {
        // DEV_INC.w VisibleShadedWalls
        Mem.ww(DevInst.dev_VisibleShadedWalls_w, Mem.uw(DevInst.dev_VisibleShadedWalls_w) + 1);

        Mem.ww(draw_WallIterations_w, d6);             // move.w d6,draw_WallIterations_w
        d7 = setw(d7, d7 - 1);                         // subq #1,d7
        Mem.ww(draw_MultCount_w, d7);                  // move.w d7,draw_MultCount_w
        int a3 = DataBuffer1_vl;                       // move.l #DataBuffer1_vl,a3
        int d0 = a0in;                                 // move.l a0,d0
        int d2 = a2in;                                 // move.l a2,d2
        Mem.wl(a3, d0); a3 += 4;                       // move.l d0,(a3)+
        d0 += d2;                                      // add.l d2,d0
        Mem.ww(a3, d1); a3 += 2;                       // move.w d1,(a3)+
        d7 = setw(d7, Mem.uw(draw_LeftWallTopBright_w)); // move.w draw_LeftWallTopBright_w,d7
        Mem.ww(a3, d7); a3 += 2;                       // move.w d7,(a3)+
        d0 >>= 1;                                      // asr.l #1,d0
        Mem.ww(a3, d4); a3 += 2;                       // move.w d4,(a3)+
        d6 = Mem.uw(draw_LeftWallBright_w);            // move.w draw_LeftWallBright_w,d6
        Mem.ww(a3, d6); a3 += 2;                       // move.w d6,(a3)+
        d4 = setw(d4, d4 + d5);                        // add.w d5,d4
        Mem.wl(a3, d0); a3 += 4;                       // move.l d0,(a3)+
        d1 = setw(d1, d1 + d3);                        // add.w d3,d1
        d1 = setw(d1, ((short) d1) >> 1);             // asr.w #1,d1
        Mem.ww(a3, d1); a3 += 2;                       // move.w d1,(a3)+
        d7 = setw(d7, d7 + Mem.uw(draw_RightWallTopBright_w)); // add.w draw_RightWallTopBright_w,d7
        d7 = setw(d7, ((short) d7) >> 1);             // asr.w #1,d7
        Mem.ww(a3, d7); a3 += 2;                       // move.w d7,(a3)+
        d4 = setw(d4, ((short) d4) >> 1);             // asr.w #1,d4
        Mem.ww(a3, d4); a3 += 2;                       // move.w d4,(a3)+
        d6 = setw(d6, d6 + Mem.uw(draw_RightWallBright_w)); // add.w draw_RightWallBright_w,d6
        d6 = setw(d6, ((short) d6) >> 1);             // asr.w #1,d6
        Mem.ww(a3, d6); a3 += 2;                       // move.w d6,(a3)+
        Mem.wl(a3, d2); a3 += 4;                       // move.l d2,(a3)+
        Mem.ww(a3, d3); a3 += 2;                       // move.w d3,(a3)+
        Mem.ww(a3, Mem.uw(draw_RightWallTopBright_w)); a3 += 2; // move.w draw_RightWallTopBright_w,(a3)+
        Mem.ww(a3, d5); a3 += 2;                       // move.w d5,(a3)+
        Mem.ww(a3, Mem.uw(draw_RightWallBright_w)); a3 += 2; // move.w draw_RightWallBright_w,(a3)+

        // Subdivision DataBuffer1/2 (record de 14 octets : x(l),bm(w),tbr(w),end(w),lbr(w))
        int a0 = DataBuffer1_vl;                       // move.l #DataBuffer1_vl,a0
        int a1 = DataBuffer2_vl;                       // move.l #DataBuffer2_vl,a1
        d7 = swap(d7);                                 // swap d7
        d7 = setw(d7, Mem.uw(draw_WallIterations_w));  // move.w draw_WallIterations_w,d7
        if ((short) d7 >= 0) {                         // blt .no_iterations : teste le MOT (drapeau N de move.w), PAS le long
            int a2 = 1;                                // move.l #1,a2

            do { // .iteration_loop:
                int a3i = a0;                          // move.l a0,a3
                int a4 = a1;                           // move.l a1,a4
                d7 = swap(d7);                         // swap d7
                d7 = setw(d7, a2);                     // move.w a2,d7
                int t = a0; a0 = a1; a1 = t;           // exg a0,a1
                d0 = Mem.l(a3i); a3i += 4;             // move.l (a3)+,d0
                d1 = Mem.l(a3i); a3i += 4;             // move.l (a3)+,d1
                d2 = Mem.l(a3i); a3i += 4;             // move.l (a3)+,d2

                while (true) { // .middle_loop:
                    Mem.wl(a4, d0); a4 += 4;           // move.l d0,(a4)+
                    d3 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d3
                    d0 += d3;                          // add.l d3,d0
                    Mem.wl(a4, d1); a4 += 4;           // move.l d1,(a4)+
                    d0 >>= 1;                          // asr.l #1,d0
                    d4 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d4
                    d1 += d4;                          // add.l d4,d1
                    Mem.wl(a4, d2); a4 += 4;           // move.l d2,(a4)+
                    d1 >>= 1;                          // asr.l #1,d1
                    d1 = setw(d1, d1 & 0x7fff);        // and.w #$7fff,d1
                    d5 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d5
                    d2 += d5;                          // add.l d5,d2
                    Mem.wl(a4, d0); a4 += 4;           // move.l d0,(a4)+
                    d2 >>= 1;                          // asr.l #1,d2
                    Mem.wl(a4, d1); a4 += 4;           // move.l d1,(a4)+
                    Mem.wl(a4, d2); a4 += 4;           // move.l d2,(a4)+
                    Mem.wl(a4, d3); a4 += 4;           // move.l d3,(a4)+
                    d0 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d0
                    d3 += d0;                          // add.l d0,d3
                    Mem.wl(a4, d4); a4 += 4;           // move.l d4,(a4)+
                    d3 >>= 1;                          // asr.l #1,d3
                    d1 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d1
                    d4 += d1;                          // add.l d1,d4
                    Mem.wl(a4, d5); a4 += 4;           // move.l d5,(a4)+
                    d4 >>= 1;                          // asr.l #1,d4
                    d4 = setw(d4, d4 & 0x7fff);        // and.w #$7fff,d4
                    d2 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d2
                    d5 += d2;                          // add.l d2,d5
                    Mem.wl(a4, d3); a4 += 4;           // move.l d3,(a4)+
                    d5 >>= 1;                          // asr.l #1,d5
                    Mem.wl(a4, d4); a4 += 4;           // move.l d4,(a4)+
                    Mem.wl(a4, d5); a4 += 4;           // move.l d5,(a4)+
                    d7 = setw(d7, d7 - 1);             // subq #1,d7
                    if ((short) d7 <= 0) {             // bgt.s .middle_loop
                        break;
                    }
                }
                Mem.wl(a4, d0); a4 += 4;               // move.l d0,(a4)+
                Mem.wl(a4, d1); a4 += 4;               // move.l d1,(a4)+
                Mem.wl(a4, d2); a4 += 4;               // move.l d2,(a4)+
                a2 += a2;                              // add.w a2,a2
                d7 = swap(d7);                         // swap d7
                d7 = setw(d7, d7 - 1);                 // dbra d7,.iteration_loop
            } while ((short) d7 != -1);
        }

        // .no_iterations: / CalcAndDrawG
        a1 = a0;                                       // move.l a0,a1
        d7 = Mem.w(draw_MultCount_w);                  // move.w draw_MultCount_w,d7
        calcAndDrawG(a1, d7);
    }

    /** .find_first_in_front + .compute_loop de draw_WallGouraudShaded. */
    private static void calcAndDrawG(int a1, int d7) {
        int d0, d1, d4;
        while (true) { // .find_first_in_front:
            d1 = Mem.l(a1); a1 += 4;                   // move.l (a1)+,d1
            d0 = Mem.uw(a1); a1 += 2;                  // move.w (a1)+,d0
            if ((short) d0 > 0) {                      // bgt.s .found_in_front
                break;
            }
            d4 = Mem.l(a1); a1 += 4;                   // move.l (a1)+,d4
            d4 = Mem.uw(a1); a1 += 2;                  // move.w (a1)+,d4
            d7 = setw(d7, d7 - 1);                     // dbra d7,.find_first_in_front
            if ((short) d7 == -1) {
                return;                                // rts
            }
        }

        // .found_in_front:
        Mem.ww(draw_TLBR_w, Mem.uw(a1)); a1 += 2;      // move.w (a1)+,draw_TLBR_w
        d4 = Mem.uw(a1); a1 += 2;                      // move.w (a1)+,d4
        Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2;       // move.w (a1)+,draw_LBR_w

        // d1=left x, d4=left end, d0=left dist
        d0 = (short) d0;                               // ext.l d0
        d1 = (int) ((long) d1 / d0);                   // divs.l d0,d1
        int d5 = 0;                                    // moveq #0,d5
        d5 = setw(d5, Mem.uw(Vid_CentreX_w));          // move.w Vid_CentreX_w,d5
        d1 += d5;                                      // add.l d5,d1
        d5 = Mem.l(draw_TopOfWall_l);                  // move.l draw_TopOfWall_l(pc),d5
        d5 = M68k.divs(d5, d0);                        // divs d0,d5
        d5 = setw(d5, d5 + Mem.uw(Vid_CentreY_w));     // add.w Vid_CentreY_w,d5
        Mem.ww(draw_StripTop_w, d5);                   // move.w d5,draw_StripTop_w
        d5 = Mem.l(draw_BottomOfWall_l);               // move.l draw_BottomOfWall_l(pc),d5
        d5 = M68k.divs(d5, d0);                        // divs d0,d5
        d5 = setw(d5, d5 + Mem.uw(Vid_CentreY_w));     // add.w Vid_CentreY_w,d5
        Mem.ww(draw_StripBottom_w, d5);                // move.w d5,draw_StripBottom_w

        computeLoopFirstG(a1, d0, d1, d4, d7);         // .compute_loop
    }

    /** .compute_loop (gouraud, première moitié). */
    private static void computeLoopFirstG(int a1, int d0, int d1, int d4, int d7) {
        while (true) {
            int d2 = Mem.w(a1 + 4);                    // move.w 4(a1),d2
            if ((short) d2 <= 0) {                     // bgt.s .in_front ; rts
                return;                                // rts
            }

            // .in_front:
            int a0 = Storage_vl;                       // move.l #Storage_vl,a0
            int d3 = Mem.l(a1);                        // move.l (a1),d3
            d2 = (short) d2;                           // ext.l d2
            d3 = (int) ((long) d3 / d2);               // divs.l d2,d3
            int d5 = 0;                                // moveq #0,d5
            d5 = setw(d5, Mem.uw(Vid_CentreX_w));      // move.w Vid_CentreX_w,d5
            d3 += d5;                                  // add.l d5,d3
            d5 = Mem.uw(a1 + 8);                       // move.w 8(a1),d5
            Mem.ww(a0 + 12, Mem.uw(draw_StripTop_w));  // move.w draw_StripTop_w(pc),12(a0)
            int d6 = Mem.l(draw_TopOfWall_l);          // move.l draw_TopOfWall_l(pc),d6
            d6 = M68k.divs(d6, d2);                    // divs d2,d6
            Mem.ww(a0 + 16, Mem.uw(draw_StripBottom_w)); // move.w draw_StripBottom_w(pc),16(a0)
            d6 = setw(d6, d6 + Mem.uw(Vid_CentreY_w)); // add.w Vid_CentreY_w,d6
            Mem.ww(draw_StripTop_w, d6);               // move.w d6,draw_StripTop_w
            Mem.ww(a0 + 14, d6);                       // move.w d6,14(a0)
            d6 = Mem.l(draw_BottomOfWall_l);           // move.l draw_BottomOfWall_l(pc),d6
            d6 = M68k.divs(d6, d2);                    // divs d2,d6
            d6 = setw(d6, d6 + Mem.uw(Vid_CentreY_w)); // add.w Vid_CentreY_w,d6
            Mem.ww(draw_StripBottom_w, d6);            // move.w d6,draw_StripBottom_w
            Mem.ww(a0 + 18, d6);                       // move.w d6,18(a0)
            Mem.wl(a1, d3);                            // move.l d3,(a1)
            if (d3 < Mem.l(Draw_LeftClip_l)) {         // cmp.l Draw_LeftClip_l(pc),d3 ; blt .all_off_left
                // .all_off_left:
                d1 = Mem.l(a1); a1 += 4;               // move.l (a1)+,d1
                d0 = Mem.uw(a1); a1 += 2;              // move.w (a1)+,d0
                Mem.ww(draw_TLBR_w, Mem.uw(a1)); a1 += 2; // move.w (a1)+,draw_TLBR_w
                d4 = Mem.uw(a1); a1 += 2;              // move.w (a1)+,d4
                Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2; // move.w (a1)+,draw_LBR_w
                d7 = setw(d7, d7 - 1);                 // dbra d7,.compute_loop
                if ((short) d7 == -1) {
                    return;
                }
                continue;
            }
            if (d1 >= Mem.l(Draw_RightClip_l)) {       // cmp.l Draw_RightClip_l(pc),d1 ; bge .all_off_right
                return;                                // .all_off_right: rts
            }

            putInMapG();                               // (REALMAP, mêmes regs)
            OTHERHALFG(a1, a0, d0, d1, d2, d3, d4, d5, d7); // bra OTHERHALFG
            return;
        }
    }

    /** Bloc carte 2D — identique à Hireswall.putInMap. */
    private static void putInMapG() {
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(HiresData.draw_WallID_w)); // move.b draw_WallID_w,d0
        if ((byte) d0 < 0) {                           // blt.s .no_put_in_map
            return;
        }
        int d3 = setb(0, d0);                          // move.b d0,d3
        d0 = setb(d0, d0 & 15);                        // and.b #15,d0
        int a0 = Mem.l(HiresData.Lvl_CompactMapPtr_l); // move.l Lvl_CompactMapPtr_l,a0
        int d1 = 0;                                    // moveq #0,d1
        int d2 = setw(0, d0);                          // move.w d0,d2
        d0 = setw(d0, d0 + d0);                        // add.w d0,d0
        d0 = setw(d0, d0 + d2);                        // add.w d2,d0
        d1 |= 1 << (d0 & 31);                          // bset d0,d1
        if ((d3 & (1 << 4)) != 0) {                    // btst #4,d3 ; beq.s .no_door
            d0 = setw(d0, d0 + 2);                     // addq #2,d0
            d1 |= 1 << (d0 & 31);                      // bset d0,d1
        }
        // .no_door:
        Mem.wl(a0, Mem.l(a0) | d1);                    // or.l d1,(a0)
        a0 = Mem.l(HiresData.Lvl_BigMapPtr_l);         // move.l Lvl_BigMapPtr_l,a0
        Mem.ww(a0 + ((short) d2) * 4, Mem.uw(draw_WallLeftPoint_w));      // move.w draw_WallLeftPoint_w,(a0,d2.w*4)
        Mem.ww(a0 + ((short) d2) * 4 + 2, Mem.uw(draw_WallRightPoint_w)); // move.w draw_WallRightPoint_w,2(a0,d2.w*4)
    }

    /** OTHERHALFG — remplit le record Storage (WD) gouraud + DoleftendGOUR, puis computeloop2G. */
    private static void OTHERHALFG(int a1, int a0, int d0, int d1, int d2, int d3, int d4, int d5, int d7) {
        Mem.ww(a0, d1);                                // move.w d1,(a0) ; WD_LeftX_w
        Mem.ww(a0 + WD_RightX_w, d3);                  // move.w d3,WD_RightX_w(a0)
        Mem.ww(a0 + WD_LeftBM_w, d4);                  // move.w d4,WD_LeftBM_w(a0)
        Mem.ww(a0 + WD_RightBM_w, d5);                 // move.w d5,WD_RightBM_w(a0)
        Mem.ww(a0 + WD_LeftDist_w, d0);                // move.w d0,WD_LeftDist_w(a0)
        Mem.ww(a0 + WD_RightDist_w, d2);               // move.w d2,WD_RightDist_w(a0)
        d5 = Mem.uw(draw_LBR_w);                       // move.w draw_LBR_w,d5
        d5 = setw(d5, d5 - 300);                       // sub.w #300,d5
        d5 = extw(d5);                                 // ext.w d5 : étend l'octet bas → nettoie la corruption asr.l
        Mem.ww(a0 + 24, d5);                           // move.w d5,24(a0) ; WD_LeftBright_w (bas gauche)
        d5 = Mem.uw(a1 + 10);                          // move.w 10(a1),d5
        d5 = setw(d5, d5 - 300);                       // sub.w #300,d5
        d5 = extw(d5);                                 // ext.w d5
        Mem.ww(a0 + 26, d5);                           // move.w d5,26(a0) ; WD_RightBright_w (bas droite)

        d5 = Mem.uw(draw_TLBR_w);                      // move.w draw_TLBR_w,d5
        d5 = setw(d5, d5 - 300);                       // sub.w #300,d5
        d5 = extw(d5);                                 // ext.w d5
        Mem.ww(a0 + 32, d5);                           // move.w d5,32(a0) ; WD_UpperLeftBright_w
        d5 = Mem.uw(a1 + 6);                           // move.w 6(a1),d5
        d5 = setw(d5, d5 - 300);                       // sub.w #300,d5
        d5 = extw(d5);                                 // ext.w d5
        Mem.ww(a0 + 34, d5);                           // move.w d5,34(a0) ; WD_UpperRightBright_w
        // movem.l d7/a1,-(a7) ; move.w #maxscrdiv,d7 ; bsr DoleftendGOUR ; movem.l (a7)+,d7/a1
        DoleftendGOUR(a0);

        // alloffleft2G:
        d1 = Mem.l(a1); a1 += 4;                       // move.l (a1)+,d1
        d0 = Mem.uw(a1); a1 += 2;                      // move.w (a1)+,d0
        Mem.ww(draw_TLBR_w, Mem.uw(a1)); a1 += 2;      // move.w (a1)+,draw_TLBR_w
        d4 = Mem.uw(a1); a1 += 2;                      // move.w (a1)+,d4
        Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2;       // move.w (a1)+,draw_LBR_w
        d7 = setw(d7, d7 - 1);                         // dbra d7,computeloop2G
        if ((short) d7 == -1) {
            return;                                    // rts
        }
        computeloop2G(a1, d0, d1, d4, d7);
    }

    /** computeloop2G — moitié droite répétée (gouraud). */
    private static void computeloop2G(int a1, int d0, int d1, int d4, int d7) {
        while (true) {
            int d2 = Mem.w(a1 + 4);                    // move.w 4(a1),d2
            if ((short) d2 <= 0) {                     // bgt.s .in_front ; rts
                return;                                // rts
            }

            // .in_front:
            d2 = (short) d2;                           // ext.l d2
            int a0 = Storage_vl;                       // move.l #Storage_vl,a0
            int d3 = Mem.l(a1);                        // move.l (a1),d3
            d3 = (int) ((long) d3 / d2);               // divs.l d2,d3
            int d5 = 0;                                // moveq #0,d5
            d5 = setw(d5, Mem.uw(Vid_CentreX_w));      // move.w Vid_CentreX_w,d5
            d3 += d5;                                  // add.l d5,d3
            d5 = Mem.uw(a1 + 8);                       // move.w 8(a1),d5
            Mem.ww(a0 + 12, Mem.uw(draw_StripTop_w));  // move.w draw_StripTop_w(pc),12(a0)
            int d6 = Mem.l(draw_TopOfWall_l);          // move.l draw_TopOfWall_l(pc),d6
            d6 = M68k.divs(d6, d2);                    // divs d2,d6
            Mem.ww(a0 + 16, Mem.uw(draw_StripBottom_w)); // move.w draw_StripBottom_w(pc),16(a0)
            d6 = setw(d6, d6 + Mem.uw(Vid_CentreY_w)); // add.w Vid_CentreY_w,d6
            Mem.ww(draw_StripTop_w, d6);               // move.w d6,draw_StripTop_w
            Mem.ww(a0 + 14, d6);                       // move.w d6,14(a0)
            d6 = Mem.l(draw_BottomOfWall_l);           // move.l draw_BottomOfWall_l(pc),d6
            d6 = M68k.divs(d6, d2);                    // divs d2,d6
            d6 = setw(d6, d6 + Mem.uw(Vid_CentreY_w)); // add.w Vid_CentreY_w,d6
            Mem.ww(draw_StripBottom_w, d6);            // move.w d6,draw_StripBottom_w
            Mem.ww(a0 + 18, d6);                       // move.w d6,18(a0)
            Mem.wl(a1, d3);                            // move.l d3,(a1)
            if ((short) d3 < Mem.w(Draw_LeftClip_w)) { // cmp.w Draw_LeftClip_w(pc),d3 ; blt.s alloffleft2G
                d1 = Mem.l(a1); a1 += 4;               // move.l (a1)+,d1
                d0 = Mem.uw(a1); a1 += 2;              // move.w (a1)+,d0
                Mem.ww(draw_TLBR_w, Mem.uw(a1)); a1 += 2; // move.w (a1)+,draw_TLBR_w
                d4 = Mem.uw(a1); a1 += 2;              // move.w (a1)+,d4
                Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2; // move.w (a1)+,draw_LBR_w
                d7 = setw(d7, d7 - 1);                 // dbra d7,computeloop2G
                if ((short) d7 == -1) {
                    return;
                }
                continue;
            }
            if ((short) d1 >= Mem.w(Draw_RightClip_w)) { // cmp.w Draw_RightClip_w(pc),d1 ; bge alloffright2G
                return;                                // alloffright2G: rts
            }

            // OTHERHALFG:
            OTHERHALFG(a1, a0, d0, d1, d2, d3, d4, d5, d7);
            return;
        }
    }

    // ------------------------------------------------------------------
    // Dispatch d'une bande gouraud : calcule le pas de luminosité puis dessine.
    // Entrée : d2=dist, d3=stripTop, d4=stripBottom, d6=upperBright/2,
    //          d7=(lower-upper)/2 (delta), a3=screen, a4=palette, a5=texture.
    // ------------------------------------------------------------------

    /** draw_ScreenWallStripGouraud (petit écran). */
    private static void draw_ScreenWallStripGouraud(int d2, int d3, int d4, int d6, int d7,
                                                    int a3, int a4, int a5) {
        d6 = swap(d6);                                 // swap d6
        d6 = d6 & 0xFFFF0000;                          // clr.w d6
        Mem.wl(draw_GouraudStart_l, d6);               // move.l d6,draw_GouraudStart_l
        d7 = swap(d7);                                 // swap d7
        d7 = d7 & 0xFFFF0000;                          // clr.w d7
        d6 = setw(d6, d4);                             // move.w d4,d6
        d6 = setw(d6, d6 - d3);                        // sub.w d3,d6
        if ((short) d6 == 0) {                         // beq.s nostripqG
            return;                                    // nostripqG: rts
        }
        d6 = (short) d6;                               // ext.l d6
        d7 = (int) ((long) d7 / d6);                   // divs.l d6,d7 ; pas de gouraud
        d6 = setw(d6, d4);                             // move.w d4,d6
        if ((short) d6 < Mem.w(draw_TopClip_w)) {      // cmp.w draw_TopClip_w(pc),d6 ; blt.s nostripqG
            return;
        }
        if ((short) d3 > Mem.w(draw_BottomClip_w)) {   // cmp.w draw_BottomClip_w(pc),d3 ; bgt.s nostripqG
            return;
        }
        if ((short) d6 > Mem.w(draw_BottomClip_w)) {   // cmp.w draw_BottomClip_w(pc),d6 ; ble.s noclipbotG
            d6 = setw(d6, Mem.uw(draw_BottomClip_w));  // move.w draw_BottomClip_w(pc),d6
        }
        // noclipbotG:
        int d5 = setw(0, d3);                          // move.w d3,d5
        if ((short) d5 < Mem.w(draw_TopClip_w)) {      // cmp.w draw_TopClip_w(pc),d5 ; bge.s nocliptopG
            d5 = setw(d5, d5 - Mem.uw(draw_TopClip_w)); // sub.w draw_TopClip_w(pc),d5
            d5 = setw(d5, -(short) d5);                // neg.w d5
            d5 = (short) d5;                           // ext.l d5
            int d0 = d7;                               // move.l d7,d0
            d0 = (int) ((long) d0 * d5);               // muls.l d5,d0
            Mem.wl(draw_GouraudStart_l, Mem.l(draw_GouraudStart_l) + d0); // add.l d0,draw_GouraudStart_l
            d5 = setw(d5, Mem.uw(draw_TopClip_w));     // move.w draw_TopClip_w(pc),d5
        }
        // nocliptopG:
        gotoendG(d2, d3, d4, d5, d6, d7, a3, a4, a5);  // bra gotoendG
    }

    /** gotoendG / doubwallGOUR (petit écran). */
    private static void gotoendG(int d2, int d3, int d4, int d5, int d6, int d7, int a3, int a4, int a5) {
        if (Mem.b(Vid_DoubleHeight_b) != 0) {          // tst.b Vid_DoubleHeight_b ; bne doubwallGOUR
            doubwallGOUR(d2, d3, d4, d5, d6, d7, a3, a4, a5);
            return;
        }
        d6 = setw(d6, d6 - d5);                        // sub.w d5,d6 ; height to draw
        if ((short) d6 <= 0) {                         // ble nostripqG
            return;
        }
        Mem.wl(draw_GouraudStep_l, d7);                // move.l d7,draw_GouraudStep_l
        int a1 = ConstantTable_vl;
        a3 += Mem.l(draw_LineOffsetBuffer_vl + ((short) d5) * 4); // add.l draw_LineOffsetBuffer_vl(pc,d5.w*4),a3
        d2 = setw(d2, d2 + d2);                        // add.w d2,d2
        int d0 = Mem.l(a1 + ((short) d2) * 8 + 4);     // move.l 4(a1,d2.w*8),d0
        d5 = setw(d5, d5 + Mem.uw(TOPOFFSET));         // add.w TOPOFFSET(pc),d5
        d4 = setw(d4, d5);                             // move.w d5,d4
        d2 = Mem.l(a1 + ((short) d2) * 4);             // move.l (a1,d2.w*4),d2
        d3 = 0;                                        // moveq #0,d3
        d5 = (short) d5;                               // ext.l d5
        d4 = d2;                                       // move.l d2,d4
        d4 = (int) ((long) d4 * d5);                   // muls.l d5,d4
        d4 += d0;                                      // add.l d0,d4
        d4 = swap(d4);                                 // swap d4
        d4 = setw(d4, d4 + Mem.uw(draw_TotalYOffset_w)); // add.w draw_TotalYOffset_w(pc),d4
        // cliptopG:
        d7 = Mem.uw(draw_WallTextureHeightMask_w);     // move.w draw_WallTextureHeightMask_w,d7
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        d0 = setw(0, Hires.SCREEN_WIDTH);              // move.w #SCREEN_WIDTH,d0
        int d1 = 0;                                    // moveq #0,d1
        int a2 = d2;                                   // move.l d2,a2 ; vstep
        d4 = swap(d4);                                 // swap d4 ; texY int dans le mot fort
        d5 = Mem.l(draw_GouraudStep_l);                // move.l draw_GouraudStep_l,d5
        d5 = d5 << 5;                                  // asl.l #5,d5
        d3 = Mem.l(draw_GouraudStart_l);               // move.l draw_GouraudStart_l,d3
        d3 = d3 << 5;                                  // asl.l #5,d3
        dispatchPackG(d0, d1, d2, d3, d4, d5, d6, d7, a2, a3, a4, a5);
    }

    /** doubwallGOUR (petit écran, double hauteur). */
    private static void doubwallGOUR(int d2, int d3, int d4, int d5, int d6, int d7, int a3, int a4, int a5) {
        int d0 = 0;                                    // moveq #0,d0
        boolean x = ((short) d5 & 1) != 0;             // asr.w #1,d5
        d5 = setw(d5, ((short) d5) >> 1);
        d5 = setw(d5, d5 + d0 + (x ? 1 : 0));          // addx.w d0,d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d6 = setw(d6, d6 - d5);                        // sub.w d5,d6 ; height to draw
        d6 = setw(d6, ((short) d6) >> 1);              // asr.w #1,d6
        if ((short) d6 <= 0) {                         // ble nostripqG
            return;
        }
        Mem.wl(draw_GouraudStep_l, d7);                // move.l d7,draw_GouraudStep_l
        int a1 = ConstantTable_vl;
        a3 += Mem.l(draw_LineOffsetBuffer_vl + ((short) d5) * 4); // add.l draw_LineOffsetBuffer_vl(pc,d5.w*4),a3
        d2 = setw(d2, d2 + d2);                        // add.w d2,d2
        d0 = Mem.l(a1 + ((short) d2) * 8 + 4);         // move.l 4(a1,d2.w*8),d0
        d5 = setw(d5, d5 + Mem.uw(TOPOFFSET));         // add.w TOPOFFSET(pc),d5
        d4 = setw(d4, d5);                             // move.w d5,d4
        d2 = Mem.l(a1 + ((short) d2) * 4);             // move.l (a1,d2.w*4),d2
        d3 = 0;                                        // moveq #0,d3
        d5 = (short) d5;                               // ext.l d5
        d4 = d2;                                       // move.l d2,d4
        d4 = (int) ((long) d4 * d5);                   // muls.l d5,d4
        d4 += d0;                                      // add.l d0,d4
        d4 = swap(d4);                                 // swap d4
        d4 = setw(d4, d4 + Mem.uw(draw_TotalYOffset_w)); // add.w draw_TotalYOffset_w(pc),d4
        d7 = Mem.uw(draw_WallTextureHeightMask_w);     // move.w draw_WallTextureHeightMask_w,d7
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        d0 = setw(0, 640);                             // move.w #640,d0
        int d1 = 0;                                    // moveq #0,d1
        d2 += d2;                                      // add.l d2,d2
        int a2 = d2;                                   // move.l d2,a2
        d4 = swap(d4);                                 // swap d4
        d5 = Mem.l(draw_GouraudStep_l);                // move.l draw_GouraudStep_l,d5
        d5 = d5 << 6;                                  // asl.l #6,d5
        d3 = Mem.l(draw_GouraudStart_l);               // move.l draw_GouraudStart_l,d3
        d3 = d3 << 5;                                  // asl.l #5,d3
        dispatchPackG(d0, d1, d2, d3, d4, d5, d6, d7, a2, a3, a4, a5);
    }

    /** ScreenWallstripdrawGOURB (plein écran). */
    private static void ScreenWallstripdrawGOURB(int d2, int d3, int d4, int d6, int d7,
                                                 int a3, int a4, int a5) {
        d6 = swap(d6);                                 // swap d6
        d6 = d6 & 0xFFFF0000;                          // clr.w d6
        Mem.wl(draw_GouraudStart_l, d6);               // move.l d6,draw_GouraudStart_l
        d7 = swap(d7);                                 // swap d7
        d7 = d7 & 0xFFFF0000;                          // clr.w d7
        d6 = setw(d6, d4);                             // move.w d4,d6
        d6 = setw(d6, d6 - d3);                        // sub.w d3,d6
        if ((short) d6 == 0) {                         // beq nostripqG
            return;
        }
        d6 = (short) d6;                               // ext.l d6
        d7 = (int) ((long) d7 / d6);                   // divs.l d6,d7
        d6 = setw(d6, d4);                             // move.w d4,d6
        if ((short) d6 < Mem.w(draw_TopClip_w)) {      // cmp.w draw_TopClip_w(pc),d6 ; blt nostripqG
            return;
        }
        if ((short) d3 > Mem.w(draw_BottomClip_w)) {   // cmp.w draw_BottomClip_w(pc),d3 ; bgt nostripqG
            return;
        }
        if ((short) d6 > Mem.w(draw_BottomClip_w)) {   // cmp.w draw_BottomClip_w(pc),d6 ; ble.s noclipbotGb
            d6 = setw(d6, Mem.uw(draw_BottomClip_w));  // move.w draw_BottomClip_w(pc),d6
        }
        // noclipbotGb:
        int d5 = setw(0, d3);                          // move.w d3,d5
        if ((short) d5 < Mem.w(draw_TopClip_w)) {      // cmp.w draw_TopClip_w(pc),d5 ; bge.s nocliptopGB
            d5 = setw(d5, d5 - Mem.uw(draw_TopClip_w)); // sub.w draw_TopClip_w(pc),d5
            d5 = setw(d5, -(short) d5);                // neg.w d5
            d5 = (short) d5;                           // ext.l d5
            int d0 = d7;                               // move.l d7,d0
            d0 = (int) ((long) d0 * d5);               // muls.l d5,d0
            Mem.wl(draw_GouraudStart_l, Mem.l(draw_GouraudStart_l) + d0); // add.l d0,draw_GouraudStart_l
            d5 = setw(d5, Mem.uw(draw_TopClip_w));     // move.w draw_TopClip_w(pc),d5
        }
        // nocliptopGB: / gotoendGB:
        gotoendGB(d2, d3, d4, d5, d6, d7, a3, a4, a5);
    }

    /** gotoendGB / doubwallGOURBIG (plein écran). */
    private static void gotoendGB(int d2, int d3, int d4, int d5, int d6, int d7, int a3, int a4, int a5) {
        if (Mem.b(Vid_DoubleHeight_b) != 0) {          // tst.b Vid_DoubleHeight_b ; bne doubwallGOURBIG
            doubwallGOURBIG(d2, d3, d4, d5, d6, d7, a3, a4, a5);
            return;
        }
        d6 = setw(d6, d6 - d5);                        // sub.w d5,d6 ; height to draw
        if ((short) d6 <= 0) {                         // ble nostripqG
            return;
        }
        Mem.wl(draw_GouraudStep_l, d7);                // move.l d7,draw_GouraudStep_l
        int a1 = ConstantTable_vl;
        a3 += Mem.l(draw_LineOffsetBuffer_vl + ((short) d5) * 4); // add.l draw_LineOffsetBuffer_vl(pc,d5.w*4),a3
        d4 = setw(d4, d2);                             // move.w d2,d4
        d2 = setw(d2, d2 + d2);                        // add.w d2,d2
        d4 = setw(d4, d4 + d2);                        // add.w d2,d4
        int d0 = Mem.l(a1 + ((short) d4) * 8 + 4);     // move.l 4(a1,d4.w*8),d0
        d5 = setw(d5, d5 + Mem.uw(TOPOFFSET));         // add.w TOPOFFSET(pc),d5
        d4 = setw(d4, d5);                             // move.w d5,d4
        d2 = Mem.l(a1 + ((short) d2) * 4);             // move.l (a1,d2.w*4),d2
        d3 = 0;                                        // moveq #0,d3
        d5 = (short) d5;                               // ext.l d5
        d4 = d2;                                       // move.l d2,d4
        d4 = (int) ((long) d4 * d5);                   // muls.l d5,d4
        d4 += d0;                                      // add.l d0,d4
        d4 = swap(d4);                                 // swap d4
        d4 = setw(d4, d4 + Mem.uw(draw_TotalYOffset_w)); // add.w draw_TotalYOffset_w(pc),d4
        d7 = Mem.uw(draw_WallTextureHeightMask_w);     // move.w draw_WallTextureHeightMask_w,d7
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        d0 = setw(0, Hires.SCREEN_WIDTH);              // move.w #SCREEN_WIDTH,d0
        int d1 = 0;                                    // moveq #0,d1
        int a2 = d2;                                   // move.l d2,a2
        d4 = swap(d4);                                 // swap d4
        d5 = Mem.l(draw_GouraudStep_l);                // move.l draw_GouraudStep_l,d5
        d5 = d5 << 5;                                  // asl.l #5,d5
        d3 = Mem.l(draw_GouraudStart_l);               // move.l draw_GouraudStart_l,d3
        d3 = d3 << 5;                                  // asl.l #5,d3
        dispatchPackG(d0, d1, d2, d3, d4, d5, d6, d7, a2, a3, a4, a5);
    }

    /** doubwallGOURBIG (plein écran, double hauteur). */
    private static void doubwallGOURBIG(int d2, int d3, int d4, int d5, int d6, int d7, int a3, int a4, int a5) {
        int d0 = 0;                                    // moveq #0,d0
        boolean x = ((short) d5 & 1) != 0;             // asr.w #1,d5
        d5 = setw(d5, ((short) d5) >> 1);
        d5 = setw(d5, d5 + d0 + (x ? 1 : 0));          // addx.w d0,d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d6 = setw(d6, d6 - d5);                        // sub.w d5,d6 ; height to draw
        d6 = setw(d6, ((short) d6) >> 1);              // asr.w #1,d6
        if ((short) d6 <= 0) {                         // ble nostripqG
            return;
        }
        Mem.wl(draw_GouraudStep_l, d7);                // move.l d7,draw_GouraudStep_l
        int a1 = ConstantTable_vl;
        a3 += Mem.l(draw_LineOffsetBuffer_vl + ((short) d5) * 4); // add.l draw_LineOffsetBuffer_vl(pc,d5.w*4),a3
        d4 = setw(d4, d2);                             // move.w d2,d4
        d2 = setw(d2, d2 + d2);                        // add.w d2,d2
        d4 = setw(d4, d4 + d2);                        // add.w d2,d4
        d0 = Mem.l(a1 + ((short) d4) * 8 + 4);         // move.l 4(a1,d4.w*8),d0
        d5 = setw(d5, d5 + Mem.uw(TOPOFFSET));         // add.w TOPOFFSET(pc),d5
        d4 = setw(d4, d5);                             // move.w d5,d4
        d2 = Mem.l(a1 + ((short) d2) * 4);             // move.l (a1,d2.w*4),d2
        d3 = 0;                                        // moveq #0,d3
        d5 = (short) d5;                               // ext.l d5
        d4 = d2;                                       // move.l d2,d4
        d4 = (int) ((long) d4 * d5);                   // muls.l d5,d4
        d4 += d0;                                      // add.l d0,d4
        d4 = swap(d4);                                 // swap d4
        d4 = setw(d4, d4 + Mem.uw(draw_TotalYOffset_w)); // add.w draw_TotalYOffset_w(pc),d4
        d7 = Mem.uw(draw_WallTextureHeightMask_w);     // move.w draw_WallTextureHeightMask_w,d7
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        d0 = setw(0, 640);                             // move.w #640,d0
        int d1 = 0;                                    // moveq #0,d1
        d2 += d2;                                      // add.l d2,d2
        int a2 = d2;                                   // move.l d2,a2
        d4 = swap(d4);                                 // swap d4
        d5 = Mem.l(draw_GouraudStep_l);                // move.l draw_GouraudStep_l,d5
        d5 = d5 << 6;                                  // asl.l #6,d5
        d3 = Mem.l(draw_GouraudStart_l);               // move.l draw_GouraudStart_l,d3
        d3 = d3 << 5;                                  // asl.l #5,d3
        dispatchPackG(d0, d1, d2, d3, d4, d5, d6, d7, a2, a3, a4, a5);
    }

    /** Dispatch dbge/dbne/dble → drawwallPACK0G/1G/2G (avec décrément d6). */
    private static void dispatchPackG(int d0, int d1, int d2, int d3, int d4, int d5, int d6, int d7,
                                      int a2, int a3, int a4, int a5) {
        d6 = setw(d6, d6 - 1);                         // (décrément du dbcc qui branche)
        if ((short) d6 == -1) {
            return;                                    // rts
        }
        int strip = Mem.b(draw_StripData_b);           // cmp.b #1,draw_StripData_b
        if ((byte) strip < 1) {                        // dbge d6,drawwallPACK0G
            drawwallPACK0G(d0, d1, d3, d4, d5, d6, d7, a2, a3, a4, a5);
        } else if ((byte) strip == 1) {                // dbne d6,drawwallPACK1G
            drawwallPACK1G(d0, d1, d3, d4, d5, d6, d7, a2, a3, a4, a5);
        } else {                                       // dble d6,drawwallPACK2G
            drawwallPACK2G(d0, d1, d3, d4, d5, d6, d7, a2, a3, a4, a5);
        }
    }

    // ------------------------------------------------------------------
    // Strip drawers gouraud (générique 030+). Inner loop : texel + brightness
    // interpolée (d3 accumulateur, d5 pas), palette a4, texY d4 (16.16 int en
    // mot fort), vstep a2, écran a3, stride d0, masque d7.
    // ------------------------------------------------------------------

    /** drawwallPACK0G */
    private static void drawwallPACK0G(int d0, int d1, int d3, int d4, int d5, int d6, int d7,
                                       int a2, int a3, int a4, int a5) {
        while (true) {
            d4 = swap(d4);                             // swap d4
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4
            int d2 = d3;                               // move.l d3,d2
            d2 = swap(d2);                             // swap d2
            d1 = setb(d1, Mem.ub(a5 + ((short) d4) * 2 + 1)); // move.b 1(a5,d4.w*2),d1
            d2 = setw(d2, d2 & 0b1111111111100000);    // and.w #%1111111111100000,d2
            d4 = swap(d4);                             // swap d4
            d1 = setb(d1, d1 & 31);                    // and.b #31,d1
            d2 = setw(d2, d2 + d1);                    // add.w d1,d2
            Mem.wb(a3, dbgForceWallPen > 0 ? dbgForceWallPen : Mem.ub(a4 + ((short) d2) * 2)); // move.b (a4,d2.w*2),(a3)
            a3 += (short) d0;                          // adda.w d0,a3
            d3 += d5;                                  // add.l d5,d3
            d4 += a2;                                  // add.l a2,d4
            d6 = setw(d6, d6 - 1);                     // dbra d6,drawwallPACK0G
            if ((short) d6 == -1) {
                return;                                // rts
            }
        }
    }

    /** drawwallPACK1G */
    private static void drawwallPACK1G(int d0, int d1, int d3, int d4, int d5, int d6, int d7,
                                       int a2, int a3, int a4, int a5) {
        while (true) {
            d4 = swap(d4);                             // swap d4
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4
            int d2 = d3;                               // move.l d3,d2
            d2 = swap(d2);                             // swap d2
            d1 = setw(d1, Mem.uw(a5 + ((short) d4) * 2)); // move.w (a5,d4.w*2),d1
            d2 = setw(d2, d2 & 0b1111111111100000);    // and.w #%1111111111100000,d2
            d4 = swap(d4);                             // swap d4
            d1 = setw(d1, (d1 & 0xFFFF) >>> 5);        // lsr.w #5,d1
            d1 = setw(d1, d1 & 31);                    // and.w #31,d1
            d2 = setb(d2, d2 + d1);                    // add.b d1,d2
            Mem.wb(a3, dbgForceWallPen > 0 ? dbgForceWallPen : Mem.ub(a4 + ((short) d2) * 2)); // move.b (a4,d2.w*2),(a3)
            a3 += (short) d0;                          // adda.w d0,a3
            d3 += d5;                                  // add.l d5,d3
            d4 += a2;                                  // add.l a2,d4
            d6 = setw(d6, d6 - 1);                     // dbra d6,drawwallPACK1G
            if ((short) d6 == -1) {
                return;
            }
        }
    }

    /** drawwallPACK2G */
    private static void drawwallPACK2G(int d0, int d1, int d3, int d4, int d5, int d6, int d7,
                                       int a2, int a3, int a4, int a5) {
        while (true) {
            d4 = swap(d4);                             // swap d4
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4
            int d2 = d3;                               // move.l d3,d2
            d2 = swap(d2);                             // swap d2
            d1 = setb(d1, Mem.ub(a5 + ((short) d4) * 2)); // move.b (a5,d4.w*2),d1
            d2 = setw(d2, d2 & 0b1111111111100000);    // and.w #%1111111111100000,d2
            d4 = swap(d4);                             // swap d4
            d1 = setb(d1, (d1 & 0xFF) >>> 2);          // lsr.b #2,d1
            d1 = setw(d1, d1 & 31);                    // and.w #31,d1
            d2 = setb(d2, d2 + d1);                    // add.b d1,d2
            Mem.wb(a3, dbgForceWallPen > 0 ? dbgForceWallPen : Mem.ub(a4 + ((short) d2) * 2)); // move.b (a4,d2.w*2),(a3)
            a3 += (short) d0;                          // adda.w d0,a3
            d3 += d5;                                  // add.l d5,d3
            d4 += a2;                                  // add.l a2,d4
            d6 = setw(d6, d6 - 1);                     // dbra d6,drawwallPACK2G
            if ((short) d6 == -1) {
                return;
            }
        }
    }

    // CODE MORT (dispatch Vid_DoubleWidth_b commenté) : scrdrawlopGDOUB,
    // scrdrawlopGBDOUB, usesimpleG/cliptopusesimpleG/simplewall*G — non traduits.
    // Version macro OPT060 de drawwallPACKnG : non portée (build 030+).
}
