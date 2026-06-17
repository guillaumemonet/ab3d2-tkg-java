package ab3d2;

import ab3d2.modules.DevInst;
import ab3d2.modules.DevMacros;
import ab3d2.modules.draw.DrawWall;

import static ab3d2.Defs.*;
import static ab3d2.HireswallData.*;
import static ab3d2.HiresData.Vid_CentreX_w;
import static ab3d2.HiresData.Vid_RightX_w;
import static ab3d2.M68k.extw;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;
import static ab3d2.bss.TablesBss.ConstantTable_vl;
import static ab3d2.bss.TablesBss.CurrentPointBrights_vl;
import static ab3d2.bss.TablesBss.DataBuffer1_vl;
import static ab3d2.bss.TablesBss.DataBuffer2_vl;
import static ab3d2.bss.TablesBss.OnScreen_vl;
import static ab3d2.bss.TablesBss.Rotated_vl;
import static ab3d2.bss.TablesBss.Storage_vl;
import static ab3d2.bss.SystemBss.Sys_Workspace_vl;
import static ab3d2.bss.VidBss.Vid_DoubleHeight_b;
import static ab3d2.bss.VidBss.Vid_DoubleWidth_b;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.DrawBss.Draw_CurrentZone_w;
import static ab3d2.bss.DrawBss.Draw_DoUpper_b;
import static ab3d2.bss.DrawBss.Draw_ForceSimpleWalls_b;
import static ab3d2.data.TablesData.DivThreeTable_vb;

/**
 * Traduction littérale de ab3d2_source/hireswall.s (le code ; les données
 * sont dans HireswallData).
 *
 * Rasterizer de murs texturés : Draw_Wall parse le record de mur, calcule les
 * brightnesses de coins (CurrentPointBrights), clippe les murs derrière la
 * caméra, choisit le renderer (simple/gouraud) puis subdivise le segment en
 * bandes verticales via draw_WallSimpleShaded → Doleftend/screendivide (qui
 * remplit Sys_Workspace_vl de records de 22 octets {x, bm, dist, top, bot,
 * bright}) → scrdrawlop* qui dessine chaque bande (draw_ScreenWallStrip →
 * gotoend → drawwallPACK0/1/2 de DrawWall, ou simplewall* locaux).
 *
 * Conventions : registres dN/aN = int locaux ; les "PACK" = 3 formats de
 * texel (octet impair&31, mot>>5&31, octet>>2). draw_StripData_b ∈ {0,1,2}
 * via DivThreeTable. Le dispatch dbge/dbne/dble (cmp.b #1) : strip<1→PACK0,
 * ==1→PACK1, >1→PACK2, avec une décrémentation de d6 avant l'entrée de boucle.
 */
public final class Hireswall {

    private Hireswall() {
    }

    // ------------------------------------------------------------------
    // Doleftend / sometodraw / screendivide — subdivision d'une bande de mur
    // en bandes-écran écrites dans Sys_Workspace_vl.
    // ------------------------------------------------------------------

    /** Doleftend — a0 = structure WD. */
    private static void Doleftend(int a0) {
        int d0 = Mem.uw(Draw_LeftClip_w);              // move.w Draw_LeftClip_w,d0
        d0 = setw(d0, d0 - 1);                         // sub.w #1,d0
        Mem.ww(Draw_LeftClipAndLast_w, d0);            // move.w d0,Draw_LeftClipAndLast_w
        d0 = Mem.uw(a0);                               // move.w (a0),d0 ; WD_LeftX_w
        int d1 = Mem.uw(a0 + WD_RightX_w);             // move.w WD_RightX_w(a0),d1
        d1 = setw(d1, d1 - d0);                        // sub.w d0,d1
        if ((short) d1 < 0) {                          // bge.s sometodraw
            return;                                    // rts
        }
        sometodraw(a0, d0, d1);
    }

    /** sometodraw — d0 = leftX, d1 = width (right-left). */
    private static void sometodraw(int a0, int d0, int d1) {
        // lit les deux mots de la table d'itération en un long
        int d7 = Mem.l(draw_IterationTable_vw + (short) d1 * 4); // move.l draw_IterationTable_vw(pc,d1.w*4),d7
        d0 = swap(d0);                                 // swap d0
        int d6 = setw(0, d7);                          // move.w d7,d6 ; d6 = shift (mot faible)
        d0 = d0 & 0xFFFF0000;                          // clr.w d0 ; d0 = leftX<<16 (accumulateur)
        d1 = swap(d1);                                 // swap d1
        d7 = swap(d7);                                 // swap d7 ; d7 low = count
        d1 = d1 & 0xFFFF0000;                          // clr.w d1
        d1 = d1 >> (d6 & 63);                          // asr.l d6,d1 ; dwidth step
        Mem.wl(a0, d1);                                // move.l d1,(a0) ; WD_DWidth_l

        d1 = 0;                                        // moveq #0,d1
        d1 = setw(d1, Mem.uw(a0 + WD_LeftBM_w));       // move.w WD_LeftBM_w(a0),d1
        int d2 = 0;                                    // moveq #0,d2
        d2 = setw(d2, Mem.uw(a0 + WD_RightBM_w));      // move.w WD_RightBM_w(a0),d2
        d2 = setw(d2, d2 - d1);                        // sub.w d1,d2
        d1 = swap(d1);                                 // swap d1 ; d1 = leftBM<<16 (accumulateur)
        d2 = swap(d2);                                 // swap d2
        d2 = d2 >> (d6 & 63);                          // asr.l d6,d2 ; dbm step
        Mem.wl(a0 + WD_DBM_l, d2);                     // move.l d2,WD_DBM_l(a0)

        d2 = 0;                                        // moveq #0,d2
        d2 = setw(d2, Mem.uw(a0 + WD_LeftDist_w));     // move.w WD_LeftDist_w(a0),d2
        int d3 = 0;                                    // moveq #0,d3
        d3 = setw(d3, Mem.uw(a0 + WD_RightDist_w));    // move.w WD_RightDist_w(a0),d3
        d3 = setw(d3, d3 - d2);                        // sub.w d2,d3
        d2 = swap(d2);                                 // swap d2 ; d2 = leftDist<<16
        d3 = swap(d3);                                 // swap d3
        d3 = d3 >> (d6 & 63);                          // asr.l d6,d3 ; ddist step
        Mem.wl(a0 + WD_DDist_l, d3);                   // move.l d3,WD_DDist_l(a0)

        d3 = 0;                                        // moveq #0,d3
        d3 = setw(d3, Mem.uw(a0 + WD_LeftTop_w));      // move.w WD_LeftTop_w(a0),d3
        int d4 = 0;                                    // moveq #0,d4
        d4 = setw(d4, Mem.uw(a0 + WD_RightTop_w));     // move.w WD_RightTop_w(a0),d4
        d4 = setw(d4, d4 - d3);                        // sub.w d3,d4
        d3 = swap(d3);                                 // swap d3 ; d3 = leftTop<<16
        d4 = swap(d4);                                 // swap d4
        d4 = d4 >> (d6 & 63);                          // asr.l d6,d4 ; dtop step
        Mem.wl(a0 + WD_DTop_l, d4);                    // move.l d4,WD_DTop_l(a0)

        d4 = 0;                                        // moveq #0,d4
        d4 = setw(d4, Mem.uw(a0 + WD_LeftBot_w));      // move.w WD_LeftBot_w(a0),d4
        int d5 = 0;                                    // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0 + WD_RightBot_w));     // move.w WD_RightBot_w(a0),d5
        d5 = setw(d5, d5 - d4);                        // sub.w d4,d5
        d4 = swap(d4);                                 // swap d4 ; d4 = leftBot<<16
        d5 = swap(d5);                                 // swap d5
        d5 = d5 >> (d6 & 63);                          // asr.l d6,d5 ; dbot step
        Mem.wl(a0 + WD_DBot_l, d5);                    // move.l d5,WD_DBot_l(a0)

        // *** Gouraud shading ***
        d5 = 0;                                        // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0 + WD_RightBright_w));  // move.w WD_RightBright_w(a0),d5
        d5 = setw(d5, d5 - Mem.uw(a0 + WD_LeftBright_w)); // sub.w WD_LeftBright_w(a0),d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d5 = swap(d5);                                 // swap d5
        d5 = d5 >> (d6 & 63);                          // asr.l d6,d5 ; dbright step
        Mem.wl(a0 + WD_DHorizBright_l, d5);            // move.l d5,WD_DHorizBright_l(a0)
        d5 = 0;                                        // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0 + WD_LeftBright_w));   // move.w WD_LeftBright_w(a0),d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d5 = swap(d5);                                 // swap d5 ; d5 = (leftBright*2)<<16

        screendivide(a0, d0, d1, d2, d3, d4, d5, d7);  // bra screendivide
    }

    /**
     * screendivide — marche depuis le clip gauche et écrit un record de 22
     * octets {x(w), bm(l), dist(l), top(l), bot(l), bright(l)} par bande
     * visible dans Sys_Workspace_vl, puis lance le dessin.
     * d0..d5 = accumulateurs (leftX/BM/Dist/Top/Bot/Bright << 16), d7 = count.
     */
    private static void screendivide(int wd, int d0, int d1, int d2, int d3, int d4, int d5, int d7) {
        d7 = d7 | 0xFFFF0000;                          // or.l #$ffff0000,d7 ; high = compteur de records (-1)
        int d6 = Mem.w(Draw_LeftClipAndLast_w);        // move.w Draw_LeftClipAndLast_w(pc),d6
        int a2 = Sys_Workspace_vl;                     // move.l #Sys_Workspace_vl,a2

        int a3 = Mem.l(wd);                            // move.l (a0),a3 ; WD_DWidth_l (dwidth)
        int a4 = Mem.l(wd + WD_DBM_l);                 // move.l WD_DBM_l(a0),a4
        int a5 = Mem.l(wd + WD_DDist_l);               // move.l WD_DDist_l(a0),a5
        int a6 = Mem.l(wd + WD_DTop_l);                // move.l WD_DTop_l(a0),a6
        int a1 = Mem.l(wd + WD_DBot_l);                // move.l WD_DBot_l(a0),a1
        int a0d = Mem.l(wd + WD_DHorizBright_l);       // move.l WD_DHorizBright_l(a0),a0 ; dbright (a0 = data)

        while (true) { // scrdivlop:
            d0 = swap(d0);                             // swap d0 ; x dans le mot faible
            if ((short) d0 > (short) d6) {             // cmp.w d6,d0 ; bgt scrnotoffleft
                // scrnotoffleft:
                d6 = setw(d6, d0);                     // move.w d0,d6
                if ((short) d0 >= Mem.w(Draw_RightClip_w)) { // cmp.w Draw_RightClip_w(pc),d0 ; bge.s outofcalc
                    break;                             // outofcalc
                }
                // scrnotoffright:
                Mem.ww(a2, d0); a2 += 2;               // move.w d0,(a2)+
                Mem.wl(a2, d1); a2 += 4;               // move.l d1,(a2)+
                Mem.wl(a2, d2); a2 += 4;               // move.l d2,(a2)+
                Mem.wl(a2, d3); a2 += 4;               // move.l d3,(a2)+
                Mem.wl(a2, d4); a2 += 4;               // move.l d4,(a2)+
                Mem.wl(a2, d5); a2 += 4;               // move.l d5,(a2)+
                d0 = swap(d0);                         // swap d0
                d0 += a3;                              // add.l a3,d0
                d1 += a4;                              // add.l a4,d1
                d2 += a5;                              // add.l a5,d2
                d3 += a6;                              // add.l a6,d3
                d4 += a1;                              // add.l a1,d4
                d5 += a0d;                             // add.l a0,d5
                d7 += 0x10000;                         // add.l #$10000,d7
                d7 = setw(d7, d7 - 1);                 // dbra d7,scrdivlop
                if ((short) d7 == -1) {
                    break;                             // (épuisement du compteur de boucle)
                }
                continue;
            }
            d0 = swap(d0);                             // swap d0
            d1 += a4;                                  // add.l a4,d1
            d2 += a5;                                  // add.l a5,d2
            d3 += a6;                                  // add.l a6,d3
            d4 += a1;                                  // add.l a1,d4
            d0 += a3;                                  // add.l a3,d0
            d5 += a0d;                                 // add.l a0,d5
            d7 = setw(d7, d7 - 1);                     // dbra d7,scrdivlop
            if ((short) d7 == -1) {
                return;                                // rts
            }
        }

        // outofcalc:
        d7 = swap(d7);                                 // swap d7
        if ((short) d7 < 0) {                          // tst.w d7 ; bge.s .something_to_draw
            return;                                    // rts
        }
        // .something_to_draw:
        // a1 = ConstantTable_vl (chargé mais non utilisé par scrdrawlop), a0 = Sys_Workspace_vl
        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; bne screendivideFULL
            screendivideFULL(Sys_Workspace_vl, d7);
            return;
        }
        scrdrawlop(Sys_Workspace_vl, d7);              // bra scrdrawlop
    }

    // ------------------------------------------------------------------
    // Boucles de lecture des bandes-écran et dessin (normal / DOUB).
    // a0 = Sys_Workspace_vl (records 22o), d7 = compteur (mot fort = nb-1).
    // ------------------------------------------------------------------

    /** scrdrawlop (petit écran, simple largeur). */
    private static void scrdrawlop(int a0, int d7) {
        while (true) {
            int d0 = Mem.uw(a0); a0 += 2;              // move.w (a0)+,d0
            if ((short) d0 == Mem.w(draw_WallLastStripX_w)) { // cmp.w draw_WallLastStripX_w,d0 ; beq.s thislinedone
                // thislinedone:
                a0 += 4 + 4 + 4 + 4 + 4;               // add.w #20,a0
                d7 = setw(d7, d7 - 1);                 // dbra d7,scrdrawlop
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

            int d5 = Mem.l(a0); a0 += 4;               // move.l (a0)+,d5
            d5 = swap(d5);                             // swap d5
            d5 = extw(d5);                             // ext.w d5 (étend l'octet bas)
            d6 = setw(d6, d6 + d5);                    // add.w d5,d6
            if ((short) d6 < 0) {                      // bge.s .br_not_negative
                d6 = setw(d6, 0);                      // moveq #0,d6
            }
            // .br_not_negative:
            if ((short) d6 >= 64) {                    // cmp.w #64,d6 ; blt.s .br_not_positive
                d6 = setw(d6, 64);                     // move.w #64,d6
            }
            // .br_not_positive:
            int a2 = Mem.l(Draw_PalettePtr_l());       // move.l Draw_PalettePtr_l,a2
            int a4 = a2;                               // move.l a2,a4
            a2 += Mem.uw(draw_BrightnessScaleTable_vw + ((short) d6) * 2); // add.w table(pc,d6*2),a2

            // dithering
            d6 = setb(d6, d6 & 0xfe);                  // and.b #$fe,d6
            a4 += Mem.uw(draw_BrightnessScaleTable_vw + ((short) d6) * 2); // add.w table(pc,d6*2),a4

            if ((d0 & 1) != 0) {                       // btst #0,d0 ; beq .nobrightswap
                int t = a2; a2 = a4; a4 = t;           // exg a2,a4
            }
            // .nobrightswap:
            draw_ScreenWallStrip(d0, d2, d3, d4, d5, d6, a2, a3, a4, a5); // bsr draw_ScreenWallStrip

            // toosmall:
            d7 = setw(d7, d7 - 1);                     // dbra d7,scrdrawlop
            if ((short) d7 == -1) {
                return;
            }
        }
    }

    /** screendivideFULL — choisit la variante plein écran (simple ou double largeur). */
    private static void screendivideFULL(int a0, int d7) {
        if (Mem.b(Vid_DoubleWidth_b) != 0) {           // tst.b Vid_DoubleWidth_b ; bne scrdrawlopFULLDOUB
            scrdrawlopFULL(a0, d7, true);
        } else {
            scrdrawlopFULL(a0, d7, false);
        }
    }

    /**
     * scrdrawlopFULL / scrdrawlopFULLDOUB — variantes plein écran.
     * doub=true saute les bandes d'abscisse impaire (btst #0,d0).
     */
    private static void scrdrawlopFULL(int a0, int d7, boolean doub) {
        while (true) {
            int d0 = Mem.uw(a0); a0 += 2;              // move.w (a0)+,d0
            if (doub && (d0 & 1) != 0) {               // btst #0,d0 ; bne.s itsanoddone
                a0 += 4 + 4 + 4 + 4 + 4;               // add.w #20,a0
                d7 = setw(d7, d7 - 1);                 // dbra d7,scrdrawlopFULLDOUB
                if ((short) d7 == -1) {
                    return;
                }
                continue;
            }
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
            d5 = extw(d5);                             // ext.w d5 (étend l'octet bas)
            d6 = setw(d6, d6 + d5);                    // add.w d5,d6
            if ((short) d6 < 0) {                      // bge.s .br_not_negative
                d6 = setw(d6, 0);                      // moveq #0,d6
            }
            // .br_not_negative:
            if ((short) d6 >= 64) {                    // cmp.w #64,d6 ; blt.s .br_not_positive
                d6 = setw(d6, 64);                     // move.w #64,d6
            }
            // .br_not_positive:
            int a2 = Mem.l(Draw_PalettePtr_l());       // move.l Draw_PalettePtr_l,a2
            int a4 = a2;                               // move.l a2,a4
            a2 += Mem.uw(draw_BrightnessScaleTable_vw + ((short) d6) * 2); // add.w table,a2
            d6 = setb(d6, d6 & 0xfe);                  // and.b #$fe,d6
            a4 += Mem.uw(draw_BrightnessScaleTable_vw + ((short) d6) * 2); // add.w table,a4
            if ((d0 & 1) != 0) {                       // btst #0,d0 ; beq .nobrightswap
                int t = a2; a2 = a4; a4 = t;           // exg a2,a4
            }
            // .nobrightswap:
            ScreenWallstripdrawBIG(d0, d2, d3, d4, d5, d6, a2, a3, a4, a5); // bsr ScreenWallstripdrawBIG

            d7 = setw(d7, d7 - 1);                     // dbra d7,scrdrawlopFULL(DOUB)
            if ((short) d7 == -1) {
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Dispatch d'une bande : clip vertical haut/bas puis gotoend (drawwallPACK).
    // Entrée : d0=x, d2=dist, d3=stripBottom, d4=stripTop, d5=bright, d6=texelBright,
    //          a2=light pal, a3=screen, a4=dim pal, a5=texture column.
    // ------------------------------------------------------------------

    /** draw_ScreenWallStrip (petit écran). */
    private static void draw_ScreenWallStrip(int d0, int d2, int d3, int d4, int d5, int d6,
                                             int a2, int a3, int a4, int a5) {
        d6 = setw(d6, d4);                             // move.w d4,d6
        if ((short) d6 < Mem.w(draw_TopClip_w)) {      // cmp.w draw_TopClip_w(pc),d6 ; blt.s nostripq
            return;                                    // nostripq: rts
        }
        if ((short) d3 > Mem.w(draw_BottomClip_w)) {   // cmp.w draw_BottomClip_w(pc),d3 ; bgt.s nostripq
            return;
        }
        if ((short) d6 > Mem.w(draw_BottomClip_w)) {   // cmp.w draw_BottomClip_w(pc),d6 ; ble.s noclipbot
            d6 = setw(d6, Mem.uw(draw_BottomClip_w));  // move.w draw_BottomClip_w(pc),d6
        }
        // noclipbot:
        d5 = setw(d5, d3);                             // move.w d3,d5
        if ((short) d5 < Mem.w(draw_TopClip_w)) {      // cmp.w draw_TopClip_w(pc),d5 ; bge.s nocliptop
            d5 = setw(d5, Mem.uw(draw_TopClip_w));     // move.w draw_TopClip_w(pc),d5
            if ((d5 & 1) != 0) {                       // btst #0,d5 ; beq.s .nsbd
                int t = a2; a2 = a4; a4 = t;           // exg a2,a4
            }
            // .nsbd: bra gotoend
        } else {
            // nocliptop:
            if ((d5 & 1) != 0) {                       // btst #0,d5 ; beq.s .nsbd
                int t = a2; a2 = a4; a4 = t;           // exg a2,a4
            }
            // .nsbd: bra gotoend
        }
        gotoend(d2, d3, d4, d5, d6, a2, a3, a4, a5);   // bra gotoend
    }

    /** gotoend / doubwall (petit écran). */
    private static void gotoend(int d2, int d3, int d4, int d5, int d6, int a2, int a3, int a4, int a5) {
        if (Mem.b(Vid_DoubleHeight_b) != 0) {          // tst.b Vid_DoubleHeight_b ; bne doubwall
            doubwall(d2, d3, d4, d5, d6, a2, a3, a4, a5);
            return;
        }
        d6 = setw(d6, d6 - d5);                        // sub.w d5,d6 ; height to draw
        if ((short) d6 <= 0) {                         // ble nostripq
            return;
        }
        int a1 = ConstantTable_vl;                     // (a1 = ConstantTable_vl posé par .something_to_draw)
        a3 += Mem.l(draw_LineOffsetBuffer_vl + ((short) d5) * 4); // add.l draw_LineOffsetBuffer_vl(pc,d5.w*4),a3
        d2 = setw(d2, d2 + d2);                        // add.w d2,d2
        int d0 = Mem.l(a1 + ((short) d2) * 8 + 4);     // move.l 4(a1,d2.w*8),d0
        d5 = setw(d5, d5 + Mem.uw(TOPOFFSET));         // add.w TOPOFFSET(pc),d5
        d4 = setw(d4, d5);                             // move.w d5,d4
        d2 = Mem.l(a1 + ((short) d2) * 4);             // move.l (a1,d2.w*4),d2
        d5 = (short) d5;                               // ext.l d5
        d4 = d2;                                       // move.l d2,d4
        d4 = (int) ((long) d4 * d5);                   // muls.l d5,d4
        d4 += d0;                                      // add.l d0,d4
        d4 = swap(d4);                                 // swap d4 ; texY integer
        d4 = setw(d4, d4 + Mem.uw(draw_TotalYOffset_w)); // add.w draw_TotalYOffset_w(pc),d4
        int d7 = Mem.uw(draw_WallTextureHeightMask_w); // move.w draw_WallTextureHeightMask_w,d7
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        d0 = setw(0, Hires.SCREEN_WIDTH);              // move.w #SCREEN_WIDTH,d0
        int d1 = 0;                                    // moveq #0,d1
        d2 = swap(d2);                                 // swap d2 ; frac en haut
        d3 = d2;                                       // move.l d2,d3
        d3 = d3 & 0xFFFF0000;                          // clr.w d3 ; d3 = frac<<16
        dispatchPack(d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5); // cmp.b #1,draw_StripData_b ; dbge/dbne/dble
    }

    /** doubwall (petit écran, double hauteur). */
    private static void doubwall(int d2, int d3, int d4, int d5, int d6, int a2, int a3, int a4, int a5) {
        int d0 = 0;                                    // moveq #0,d0
        boolean x = ((short) d5 & 1) != 0;             // asr.w #1,d5 (retenue X = bit 0)
        d5 = setw(d5, ((short) d5) >> 1);
        d5 = setw(d5, d5 + d0 + (x ? 1 : 0));          // addx.w d0,d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5 ; d5*3
        d6 = setw(d6, d6 - d5);                        // sub.w d5,d6
        d6 = setw(d6, ((short) d6) >> 1);              // asr.w #1,d6
        if ((short) d6 <= 0) {                         // ble nostripq
            return;
        }
        int a1 = ConstantTable_vl;
        a3 += Mem.l(draw_LineOffsetBuffer_vl + ((short) d5) * 4); // add.l draw_LineOffsetBuffer_vl(pc,d5.w*4),a3
        d2 = setw(d2, d2 + d2);                        // add.w d2,d2
        d0 = Mem.l(a1 + ((short) d2) * 8 + 4);         // move.l 4(a1,d2.w*8),d0
        d5 = setw(d5, d5 + Mem.uw(TOPOFFSET));         // add.w TOPOFFSET(pc),d5
        d4 = setw(d4, d5);                             // move.w d5,d4
        d2 = Mem.l(a1 + ((short) d2) * 4);             // move.l (a1,d2.w*4),d2
        d5 = (short) d5;                               // ext.l d5
        d4 = d2;                                       // move.l d2,d4
        d4 = (int) ((long) d4 * d5);                   // muls.l d5,d4
        d4 += d0;                                      // add.l d0,d4
        d4 = swap(d4);                                 // swap d4
        d4 = setw(d4, d4 + Mem.uw(draw_TotalYOffset_w)); // add.w draw_TotalYOffset_w(pc),d4
        int d7 = Mem.uw(draw_WallTextureHeightMask_w); // move.w draw_WallTextureHeightMask_w,d7
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        d0 = setw(0, 640);                             // move.w #640,d0
        int d1 = 0;                                    // moveq #0,d1
        d2 += d2;                                      // add.l d2,d2
        d2 = swap(d2);                                 // swap d2
        d3 = d2;                                       // move.l d2,d3
        d3 = d3 & 0xFFFF0000;                          // clr.w d3
        dispatchPack(d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    /** ScreenWallstripdrawBIG (plein écran) — clip puis gotoendBIG/doubwallBIG. */
    private static void ScreenWallstripdrawBIG(int d0, int d2, int d3, int d4, int d5, int d6,
                                               int a2, int a3, int a4, int a5) {
        d6 = setw(d6, d4);                             // move.w d4,d6
        if ((short) d6 < Mem.w(draw_TopClip_w)) {      // cmp.w draw_TopClip_w(pc),d6 ; blt nostripq
            return;
        }
        if ((short) d3 > Mem.w(draw_BottomClip_w)) {   // cmp.w draw_BottomClip_w(pc),d3 ; bgt nostripq
            return;
        }
        if ((short) d6 > Mem.w(draw_BottomClip_w)) {   // cmp.w draw_BottomClip_w(pc),d6 ; ble.s .noclipbot
            d6 = setw(d6, Mem.uw(draw_BottomClip_w));  // move.w draw_BottomClip_w(pc),d6
        }
        // .noclipbot:
        d5 = setw(d5, d3);                             // move.w d3,d5
        if ((short) d5 < Mem.w(draw_TopClip_w)) {      // cmp.w draw_TopClip_w(pc),d5 ; bge.s .nocliptop
            d5 = setw(d5, Mem.uw(draw_TopClip_w));     // move.w draw_TopClip_w(pc),d5
            if ((d5 & 1) != 0) {                       // btst #0,d5 ; beq.s .nsbd
                int t = a2; a2 = a4; a4 = t;           // exg a2,a4
            }
        } else {
            // .nocliptop:
            if ((d5 & 1) != 0) {                       // btst #0,d5 ; beq.s .nsbd2
                int t = a2; a2 = a4; a4 = t;           // exg a2,a4
            }
        }
        gotoendBIG(d2, d3, d4, d5, d6, a2, a3, a4, a5); // gotoendBIG
    }

    /** gotoendBIG / doubwallBIG (plein écran). */
    private static void gotoendBIG(int d2, int d3, int d4, int d5, int d6, int a2, int a3, int a4, int a5) {
        if (Mem.b(Vid_DoubleHeight_b) != 0) {          // tst.b Vid_DoubleHeight_b ; bne doubwallBIG
            doubwallBIG(d2, d3, d4, d5, d6, a2, a3, a4, a5);
            return;
        }
        d6 = setw(d6, d6 - d5);                        // sub.w d5,d6 ; height to draw
        if ((short) d6 <= 0) {                         // ble nostripq
            return;
        }
        int a1 = ConstantTable_vl;
        a3 += Mem.l(draw_LineOffsetBuffer_vl + ((short) d5) * 4); // add.l draw_LineOffsetBuffer_vl(pc,d5.w*4),a3
        d4 = setw(d4, d2);                             // move.w d2,d4
        d2 = setw(d2, d2 + d2);                        // add.w d2,d2
        d4 = setw(d4, d4 + d2);                        // add.w d2,d4 ; d2*3
        int d0 = Mem.l(a1 + ((short) d4) * 8 + 4);     // move.l 4(a1,d4.w*8),d0 ; d2*24
        d5 = setw(d5, d5 + Mem.uw(TOPOFFSET));         // add.w TOPOFFSET(pc),d5
        d4 = setw(d4, d5);                             // move.w d5,d4
        d2 = Mem.l(a1 + ((short) d2) * 4);             // move.l (a1,d2.w*4),d2
        d5 = (short) d5;                               // ext.l d5
        d4 = d2;                                       // move.l d2,d4
        d4 = (int) ((long) d4 * d5);                   // muls.l d5,d4
        d4 += d0;                                      // add.l d0,d4
        d4 = swap(d4);                                 // swap d4
        d4 = setw(d4, d4 + Mem.uw(draw_TotalYOffset_w)); // add.w draw_TotalYOffset_w(pc),d4
        int d7 = Mem.uw(draw_WallTextureHeightMask_w); // move.w draw_WallTextureHeightMask_w,d7
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        d0 = setw(0, Hires.SCREEN_WIDTH);              // move.w #SCREEN_WIDTH,d0
        int d1 = 0;                                    // moveq #0,d1
        d2 = swap(d2);                                 // swap d2
        d3 = d2;                                       // move.l d2,d3
        d3 = d3 & 0xFFFF0000;                          // clr.w d3
        dispatchPack(d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    /** doubwallBIG (plein écran, double hauteur). */
    private static void doubwallBIG(int d2, int d3, int d4, int d5, int d6, int a2, int a3, int a4, int a5) {
        int d0 = 0;                                    // moveq #0,d0
        boolean x = ((short) d5 & 1) != 0;             // asr.w #1,d5
        d5 = setw(d5, ((short) d5) >> 1);
        d5 = setw(d5, d5 + d0 + (x ? 1 : 0));          // addx.w d0,d5
        d5 = setw(d5, d5 + d5);                        // add.w d5,d5
        d6 = setw(d6, d6 - d5);                        // sub.w d5,d6 ; height to draw
        d6 = setw(d6, ((short) d6) >> 1);              // asr.w #1,d6
        if ((short) d6 <= 0) {                         // ble nostripq
            return;
        }
        int a1 = ConstantTable_vl;
        a3 += Mem.l(draw_LineOffsetBuffer_vl + ((short) d5) * 4); // add.l draw_LineOffsetBuffer_vl(pc,d5.w*4),a3
        d4 = setw(d4, d2);                             // move.w d2,d4
        d2 = setw(d2, d2 + d2);                        // add.w d2,d2
        d4 = setw(d4, d4 + d2);                        // add.w d2,d4
        d0 = Mem.l(a1 + ((short) d4) * 8 + 4);         // move.l 4(a1,d4.w*8),d0
        d5 = setw(d5, d5 + Mem.uw(TOPOFFSET));         // add.w TOPOFFSET(pc),d5
        d4 = setw(d4, d5);                             // move.w d5,d4
        d2 = Mem.l(a1 + ((short) d2) * 4);             // move.l (a1,d2.w*4),d2
        d5 = (short) d5;                               // ext.l d5
        d4 = d2;                                       // move.l d2,d4
        d4 = (int) ((long) d4 * d5);                   // muls.l d5,d4
        d4 += d0;                                      // add.l d0,d4
        d4 = swap(d4);                                 // swap d4
        d4 = setw(d4, d4 + Mem.uw(draw_TotalYOffset_w)); // add.w draw_TotalYOffset_w(pc),d4
        int d7 = Mem.uw(draw_WallTextureHeightMask_w); // move.w draw_WallTextureHeightMask_w,d7
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        d0 = setw(0, 640);                             // move.w #640,d0
        int d1 = 0;                                    // moveq #0,d1
        d2 += d2;                                      // add.l d2,d2
        d2 = swap(d2);                                 // swap d2
        d3 = d2;                                       // move.l d2,d3
        d3 = d3 & 0xFFFF0000;                          // clr.w d3
        dispatchPack(d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    /**
     * Dispatch dbge/dbne/dble (cmp.b #1,draw_StripData_b) vers les strip-drawers
     * de DrawWall, avec la décrémentation de d6 du dbcc qui branche.
     */
    private static void dispatchPack(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                     int a2, int a3, int a4, int a5) {
        d6 = setw(d6, d6 - 1);                         // (le dbcc qui branche décrémente d6 une fois)
        if ((short) d6 == -1) {
            return;                                    // rts
        }
        int strip = Mem.b(draw_StripData_b);           // cmp.b #1,draw_StripData_b
        if ((byte) strip < 1) {                        // dbge d6,drawwallPACK0
            DrawWall.drawwallPACK0(d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
        } else if ((byte) strip == 1) {                // dbne d6,drawwallPACK1
            DrawWall.drawwallPACK1(d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
        } else {                                       // dble d6,drawwallPACK2
            DrawWall.drawwallPACK2(d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
        }
    }

    // Accesseurs vers les variables de hires.s (data partielle dans HiresData).
    private static int Draw_ChunkPtr_l() {
        return ab3d2.bss.DrawBss.Draw_ChunkPtr_l;
    }

    private static int Draw_PalettePtr_l() {
        return ab3d2.bss.DrawBss.Draw_PalettePtr_l;
    }

    // ------------------------------------------------------------------
    // draw_WallSimpleShaded — subdivise le mur (DataBuffer1/2) puis dessine.
    // Registres d'entrée : a0=x1, d1=z1, a2=x2, d3=z2, d4=startLen, d5=endLen,
    //   d6=draw_WallIterations, d7=count (depuis Draw_Wall).
    // ------------------------------------------------------------------

    /** draw_WallSimpleShaded */
    static void draw_WallSimpleShaded(int a0in, int d1, int a2in, int d3, int d4, int d5, int d6, int d7) {
        // DEV_INC.w VisibleSimpleWalls
        Mem.ww(DevInst.dev_VisibleSimpleWalls_w, Mem.uw(DevInst.dev_VisibleSimpleWalls_w) + 1);

        Mem.ww(draw_WallIterations_w, d6);             // move.w d6,draw_WallIterations_w
        d7 = setw(d7, d7 - 1);                         // subq #1,d7
        Mem.ww(draw_MultCount_w, d7);                  // move.w d7,draw_MultCount_w
        int a3 = DataBuffer1_vl;                       // move.l #DataBuffer1_vl,a3
        int d0 = a0in;                                 // move.l a0,d0
        int d2 = a2in;                                 // move.l a2,d2
        Mem.wl(a3, d0); a3 += 4;                       // move.l d0,(a3)+
        d0 += d2;                                      // add.l d2,d0
        Mem.ww(a3, d1); a3 += 2;                       // move.w d1,(a3)+
        d0 >>= 1;                                      // asr.l #1,d0
        Mem.ww(a3, d4); a3 += 2;                       // move.w d4,(a3)+
        d6 = Mem.uw(draw_LeftWallBright_w);            // move.w draw_LeftWallBright_w,d6
        Mem.ww(a3, d6); a3 += 2;                       // move.w d6,(a3)+
        d4 = setw(d4, d4 + d5);                        // add.w d5,d4
        Mem.wl(a3, d0); a3 += 4;                       // move.l d0,(a3)+
        d1 = setw(d1, d1 + d3);                        // add.w d3,d1
        d1 = setw(d1, ((short) d1) >> 1);             // asr.w #1,d1
        Mem.ww(a3, d1); a3 += 2;                       // move.w d1,(a3)+
        d4 = setw(d4, ((short) d4) >> 1);             // asr.w #1,d4
        Mem.ww(a3, d4); a3 += 2;                       // move.w d4,(a3)+
        d6 = setw(d6, d6 + Mem.uw(draw_RightWallBright_w)); // add.w draw_RightWallBright_w,d6
        d6 = setw(d6, ((short) d6) >> 1);             // asr.w #1,d6
        Mem.ww(a3, d6); a3 += 2;                       // move.w d6,(a3)+
        Mem.wl(a3, d2); a3 += 4;                       // move.l d2,(a3)+
        Mem.ww(a3, d3); a3 += 2;                       // move.w d3,(a3)+
        Mem.ww(a3, d5); a3 += 2;                       // move.w d5,(a3)+
        Mem.ww(a3, Mem.uw(draw_RightWallBright_w)); a3 += 2; // move.w draw_RightWallBright_w,(a3)+

        // Subdivision adaptative entre DataBuffer1 et DataBuffer2.
        int a0 = DataBuffer1_vl;                       // move.l #DataBuffer1_vl,a0
        int a1 = DataBuffer2_vl;                       // move.l #DataBuffer2_vl,a1

        d6 = Mem.w(draw_WallIterations_w);             // move.w draw_WallIterations_w,d6
        if (d6 >= 0) {                                 // blt .no_iterations
            int a2 = 1;                                // move.l #1,a2

            do { // .iteration_loop:
                int a3i = a0;                          // move.l a0,a3
                int a4 = a1;                           // move.l a1,a4
                d7 = setw(0, a2);                      // move.w a2,d7
                int t = a0; a0 = a1; a1 = t;           // exg a0,a1

                d0 = Mem.l(a3i); a3i += 4;             // move.l (a3)+,d0
                d1 = Mem.uw(a3i); a3i += 2;            // move.w (a3)+,d1
                d2 = Mem.l(a3i); a3i += 4;             // move.l (a3)+,d2

                while (true) { // .middle_loop:
                    Mem.wl(a4, d0); a4 += 4;           // move.l d0,(a4)+
                    d3 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d3
                    d0 += d3;                          // add.l d3,d0
                    Mem.ww(a4, d1); a4 += 2;           // move.w d1,(a4)+
                    d0 >>= 1;                          // asr.l #1,d0
                    d4 = Mem.uw(a3i); a3i += 2;        // move.w (a3)+,d4
                    d1 = setw(d1, d1 + d4);            // add.w d4,d1
                    Mem.wl(a4, d2); a4 += 4;           // move.l d2,(a4)+
                    d1 = setw(d1, ((short) d1) >> 1);  // asr.w #1,d1
                    d5 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d5
                    d2 += d5;                          // add.l d5,d2
                    Mem.wl(a4, d0); a4 += 4;           // move.l d0,(a4)+
                    d2 >>= 1;                          // asr.l #1,d2
                    Mem.ww(a4, d1); a4 += 2;           // move.w d1,(a4)+
                    Mem.wl(a4, d2); a4 += 4;           // move.l d2,(a4)+

                    Mem.wl(a4, d3); a4 += 4;           // move.l d3,(a4)+
                    d0 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d0
                    d3 += d0;                          // add.l d0,d3

                    Mem.ww(a4, d4); a4 += 2;           // move.w d4,(a4)+
                    d3 >>= 1;                          // asr.l #1,d3
                    d1 = Mem.uw(a3i); a3i += 2;        // move.w (a3)+,d1
                    d4 = setw(d4, d4 + d1);            // add.w d1,d4
                    Mem.wl(a4, d5); a4 += 4;           // move.l d5,(a4)+
                    d4 = setw(d4, ((short) d4) >> 1);  // asr.w #1,d4
                    d2 = Mem.l(a3i); a3i += 4;         // move.l (a3)+,d2
                    d5 += d2;                          // add.l d2,d5
                    Mem.wl(a4, d3); a4 += 4;           // move.l d3,(a4)+
                    d5 >>= 1;                          // asr.l #1,d5
                    Mem.ww(a4, d4); a4 += 2;           // move.w d4,(a4)+
                    Mem.wl(a4, d5); a4 += 4;           // move.l d5,(a4)+

                    d7 = setw(d7, d7 - 1);             // subq #1,d7
                    if ((short) d7 <= 0) {             // bgt.s .middle_loop
                        break;
                    }
                }
                Mem.wl(a4, d0); a4 += 4;               // move.l d0,(a4)+
                Mem.ww(a4, d1); a4 += 2;               // move.w d1,(a4)+
                Mem.wl(a4, d2); a4 += 4;               // move.l d2,(a4)+

                a2 += a2;                              // add.w a2,a2
                d6 = setw(d6, d6 - 1);                 // dbra d6,.iteration_loop
            } while ((short) d6 != -1);
        }

        // .no_iterations: / CalcAndDraw
        a1 = a0;                                       // move.l a0,a1
        d7 = Mem.w(draw_MultCount_w);                  // move.w draw_MultCount_w,d7
        calcAndDraw(a1, d7);
    }

    /** Boucle .find_first_in_front / .compute_loop de draw_WallSimpleShaded. */
    private static void calcAndDraw(int a1, int d7) {
        int d0, d1, d3, d4, d5;
        // .find_first_in_front:
        while (true) {
            d1 = Mem.l(a1); a1 += 4;                   // move.l (a1)+,d1
            d0 = Mem.uw(a1); a1 += 2;                  // move.w (a1)+,d0
            if ((short) d0 > 0) {                      // bgt.s .found_in_front
                break;
            }
            d4 = Mem.l(a1); a1 += 4;                   // move.l (a1)+,d4
            d7 = setw(d7, d7 - 1);                     // dbra d7,.find_first_in_front
            if ((short) d7 == -1) {
                return;                                // no two points in front: rts
            }
        }

        // .found_in_front:
        d0 = (short) d0;                               // ext.l d0
        d4 = Mem.uw(a1); a1 += 2;                      // move.w (a1)+,d4
        Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2;       // move.w (a1)+,draw_LBR_w

        // d1=left x, d4=left end, d0=left dist
        d1 = (int) ((long) d1 / d0);                   // divs.l d0,d1
        d5 = 0;                                        // moveq #0,d5
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

        // .compute_loop:
        computeLoopFirst(a1, d0, d1, d4, d7);
    }

    /** .compute_loop (première moitié) + bascule vers OTHERHALF/computeloop2. */
    private static void computeLoopFirst(int a1, int d0, int d1, int d4, int d7) {
        while (true) {
            int d2 = Mem.w(a1 + 4);                    // move.w 4(a1),d2
            if ((short) d2 <= 0) {                     // bgt.s .in_front ; bra .all_off_left
                // .all_off_left:
                d1 = Mem.l(a1); a1 += 4;               // move.l (a1)+,d1
                d0 = Mem.uw(a1); a1 += 2;              // move.w (a1)+,d0
                d4 = Mem.uw(a1); a1 += 2;              // move.w (a1)+,d4
                Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2; // move.w (a1)+,draw_LBR_w
                d7 = setw(d7, d7 - 1);                 // dbra d7,.compute_loop
                if ((short) d7 == -1) {
                    return;                            // rts
                }
                continue;
            }

            // .in_front:
            d2 = (short) d2;                           // ext.l d2
            int a0 = Storage_vl;                       // move.l #Storage_vl,a0
            int d3 = Mem.l(a1);                        // move.l (a1),d3
            d3 = (int) ((long) d3 / d2);               // divs.l d2,d3
            int d5 = 0;                                // moveq #0,d5
            d5 = setw(d5, Mem.uw(Vid_CentreX_w));      // move.w Vid_CentreX_w,d5
            d3 += d5;                                  // add.l d5,d3
            d5 = Mem.uw(a1 + 6);                       // move.w 6(a1),d5
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
                d1 = Mem.l(a1); a1 += 4;               // (chute dans .all_off_left)
                d0 = Mem.uw(a1); a1 += 2;
                d4 = Mem.uw(a1); a1 += 2;
                Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2;
                d7 = setw(d7, d7 - 1);
                if ((short) d7 == -1) {
                    return;
                }
                continue;
            }
            if (d1 >= Mem.l(Draw_RightClip_l)) {       // cmp.l Draw_RightClip_l(pc),d1 ; bge .all_off_right
                return;                                // .all_off_right: rts
            }

            // movem.l d0/d1/d2/d3/a0,-(a7) ; put into 2D map (mode REALMAP)
            putInMap();
            // movem.l (a7)+,...

            // bra OTHERHALF — exécute la moitié droite puis poursuit computeloop2
            OTHERHALF(a1, a0, d0, d1, d2, d3, d4, d5, d7);
            return;
        }
    }

    /** Bloc d'inscription dans la carte 2D (Lvl_CompactMap/BigMap) — REALMAP. */
    private static void putInMap() {
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
        d0 = setw(d0, d0 + d2);                        // add.w d2,d0 ; d0*3
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
        // .no_put_in_map:
    }

    /** OTHERHALF — remplit le record Storage et appelle Doleftend, puis computeloop2. */
    private static void OTHERHALF(int a1, int a0, int d0, int d1, int d2, int d3, int d4, int d5, int d7) {
        Mem.ww(a0, d1);                                // move.w d1,(a0)
        Mem.ww(a0 + 2, d3);                            // move.w d3,2(a0)
        Mem.ww(a0 + 4, d4);                            // move.w d4,4(a0)
        Mem.ww(a0 + 6, d5);                            // move.w d5,6(a0)
        Mem.ww(a0 + 8, d0);                            // move.w d0,8(a0)
        Mem.ww(a0 + 10, d2);                           // move.w d2,10(a0)

        d5 = Mem.uw(draw_LBR_w);                       // move.w draw_LBR_w,d5
        d5 = setw(d5, d5 - 300);                       // sub.w #300,d5
        d5 = extw(d5);                                 // ext.w d5 : étend l'octet bas → nettoie la corruption asr.l de la subdivision
        Mem.ww(a0 + 24, d5);                           // move.w d5,24(a0)
        d5 = Mem.uw(a1 + 8);                           // move.w 8(a1),d5
        d5 = setw(d5, d5 - 300);                       // sub.w #300,d5
        d5 = extw(d5);                                 // ext.w d5 : étend l'octet bas
        Mem.ww(a0 + 26, d5);                           // move.w d5,26(a0)

        // movem.l d7/a1,-(a7) ; move.w #maxscrdiv,d7 ; bsr Doleftend ; movem.l (a7)+,d7/a1
        Doleftend(a0);                                 // (a0 = Storage record/WD)

        // alloffleft2:
        d1 = Mem.l(a1); a1 += 4;                       // move.l (a1)+,d1
        d0 = Mem.uw(a1); a1 += 2;                      // move.w (a1)+,d0
        d4 = Mem.uw(a1); a1 += 2;                      // move.w (a1)+,d4
        Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2;       // move.w (a1)+,draw_LBR_w
        d7 = setw(d7, d7 - 1);                         // dbra d7,computeloop2
        if ((short) d7 == -1) {
            return;                                    // rts
        }
        computeloop2(a1, d0, d1, d4, d7);
    }

    /** computeloop2 — moitié droite répétée (après la première bande). */
    private static void computeloop2(int a1, int d0, int d1, int d4, int d7) {
        while (true) {
            int d2 = Mem.w(a1 + 4);                    // move.w 4(a1),d2
            if ((short) d2 <= 0) {                     // bgt.s .in_front ; bra alloffleft2
                // alloffleft2:
                d1 = Mem.l(a1); a1 += 4;               // move.l (a1)+,d1
                d0 = Mem.uw(a1); a1 += 2;              // move.w (a1)+,d0
                d4 = Mem.uw(a1); a1 += 2;              // move.w (a1)+,d4
                Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2; // move.w (a1)+,draw_LBR_w
                d7 = setw(d7, d7 - 1);                 // dbra d7,computeloop2
                if ((short) d7 == -1) {
                    return;
                }
                continue;
            }

            // .in_front:
            d2 = (short) d2;                           // ext.l d2
            int a0 = Storage_vl;                       // move.l #Storage_vl,a0
            int d3 = Mem.l(a1);                        // move.l (a1),d3
            d3 = (int) ((long) d3 / d2);               // divs.l d2,d3
            int d5 = 0;                                // moveq #0,d5
            d5 = setw(d5, Mem.uw(Vid_CentreX_w));      // move.w Vid_CentreX_w,d5
            d3 += d5;                                  // add.l d5,d3
            d5 = Mem.uw(a1 + 6);                       // move.w 6(a1),d5
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
            if (d3 < Mem.l(Draw_LeftClip_l)) {         // cmp.l Draw_LeftClip_l(pc),d3 ; blt.s alloffleft2
                d1 = Mem.l(a1); a1 += 4;
                d0 = Mem.uw(a1); a1 += 2;
                d4 = Mem.uw(a1); a1 += 2;
                Mem.ww(draw_LBR_w, Mem.uw(a1)); a1 += 2;
                d7 = setw(d7, d7 - 1);
                if ((short) d7 == -1) {
                    return;
                }
                continue;
            }
            if (d1 >= Mem.l(Draw_RightClip_l)) {       // cmp.l Draw_RightClip_l(pc),d1 ; bge.s alloffright2
                return;                                // alloffright2: rts
            }

            // OTHERHALF:
            OTHERHALF(a1, a0, d0, d1, d2, d3, d4, d5, d7);
            return;
        }
    }

    // ------------------------------------------------------------------
    // Draw_Wall — point d'entrée.
    // ------------------------------------------------------------------

    /** Draw_Wall — a0 = flux zone-graph (record de mur) ; renvoie a0 avancé. */
    public static int Draw_Wall(int a0) {
        int a5 = Rotated_vl;                           // move.l #Rotated_vl,a5
        int a6 = OnScreen_vl;                          // move.l #OnScreen_vl,a6

        int d0 = Mem.w(a0); a0 += 2;                   // move.w (a0)+,d0 ; left point index
        int d2 = Mem.w(a0); a0 += 2;                   // move.w (a0)+,d2 ; right point index

        Mem.ww(draw_WallLeftPoint_w, d0);              // move.w d0,draw_WallLeftPoint_w
        Mem.ww(draw_WallRightPoint_w, d2);             // move.w d2,draw_WallRightPoint_w

        Mem.wb(draw_WhichLeftPoint_w + 1, Mem.ub(a0)); a0 += 1;  // move.b (a0)+,draw_WhichLeftPoint_w+1
        Mem.wb(draw_WhichRightPoint_w + 1, Mem.ub(a0)); a0 += 1; // move.b (a0)+,draw_WhichRightPoint_w+1

        int d5 = 0;                                    // moveq #0,d5
        d5 = setw(d5, Mem.uw(a0)); a0 += 2;            // move.w (a0)+,d5 ; longueur du mur (registre préservé)
        d5wall = d5;
        int d1 = Mem.uw(a0); a0 += 2;                  // move.w (a0)+,d1
        d1 = setw(d1, d1 << 4);                        // asl.w #4,d1
        Mem.ww(draw_FromTile_w, d1);                   // move.w d1,draw_FromTile_w

        d1 = Mem.uw(a0); a0 += 2;                      // move.w (a0)+,d1
        Mem.ww(draw_TotalYOffset_w, d1);               // move.w d1,draw_TotalYOffset_w

        d1 = Mem.uw(a0); a0 += 2;                      // move.w (a0)+,d1
        int a3 = Draw_WallTexturePtrs_vl();            // move.l #Draw_WallTexturePtrs_vl,a3
        a3 = Mem.l(a3 + ((short) d1) * 4);             // move.l (a3,d1.w*4),a3
        Mem.wl(Draw_PalettePtr_l(), a3);               // move.l a3,Draw_PalettePtr_l
        a3 += 64 * 32;                                 // add.l #64*32,a3
        Mem.wl(Draw_ChunkPtr_l(), a3);                 // move.l a3,Draw_ChunkPtr_l

        Mem.ww(draw_AngleBright_w, Mem.uw(HiresData.Zone_Bright_w)); // move.w Zone_Bright_w,draw_AngleBright_w
        int d6 = Mem.l(HiresData.Plr_YOff_l);          // move.l Plr_YOff_l,d6

        d1 = 0;                                        // moveq #0,d1
        d1 = setb(d1, Mem.ub(a0)); a0 += 1;            // move.b (a0)+,d1
        Mem.ww(draw_WallTextureHeightMask_w, d1);      // move.w d1,draw_WallTextureHeightMask_w
        d1 = 0;                                        // moveq #0,d1
        d1 = setb(d1, Mem.ub(a0)); a0 += 1;            // move.b (a0)+,d1
        Mem.ww(draw_WallTextureHeightShift_w, d1);     // move.w d1,draw_WallTextureHeightShift_w
        d1 = 0;                                        // moveq #0,d1
        d1 = setb(d1, Mem.ub(a0)); a0 += 1;            // move.b (a0)+,d1 ; texture width
        Mem.ww(draw_WallTextureWidthMask_w, d1);       // move.w d1,draw_WallTextureWidthMask_w
        Mem.wb(draw_WhichPBR_w, Mem.ub(a0)); a0 += 1;  // move.b (a0)+,draw_WhichPBR_w
        d1 = Mem.uw(draw_TotalYOffset_w);              // move.w draw_TotalYOffset_w,d1
        d1 = setw(d1, d1 + Mem.uw(draw_WallYOffset_w)); // add.w draw_WallYOffset_w,d1
        d1 = setw(d1, d1 & Mem.uw(draw_WallTextureHeightMask_w)); // and.w draw_WallTextureHeightMask_w,d1
        Mem.ww(draw_TotalYOffset_w, d1);               // move.w d1,draw_TotalYOffset_w
        Mem.wl(draw_TopOfWall_l, Mem.l(a0)); a0 += 4;  // move.l (a0)+,draw_TopOfWall_l
        Mem.wl(draw_TopOfWall_l, Mem.l(draw_TopOfWall_l) - d6); // sub.l d6,draw_TopOfWall_l
        Mem.wl(draw_BottomOfWall_l, Mem.l(a0)); a0 += 4; // move.l (a0)+,draw_BottomOfWall_l
        Mem.wl(draw_BottomOfWall_l, Mem.l(draw_BottomOfWall_l) - d6); // sub.l d6,draw_BottomOfWall_l

        int d3 = Mem.b(a0); a0 += 1;                   // move.b (a0)+,d3 ; ext.w d3
        Mem.ww(draw_WallBrightOffset_w, d3);           // move.w d3,draw_WallBrightOffset_w
        Mem.wb(draw_OtherZone_w + 1, Mem.ub(a0)); a0 += 1; // move.b (a0)+,draw_OtherZone_w+1

        d3 = Mem.l(draw_TopOfWall_l);                  // move.l draw_TopOfWall_l,d3
        if (d3 >= Mem.l(draw_BottomOfWall_l)) {        // cmp.l draw_BottomOfWall_l,d3 ; bge wallfacingaway
            return a0;                                 // wallfacingaway: rts
        }

        // Détermination si le mur est visible (devant la caméra).
        if (Mem.w(a5 + d0 * 8 + 6) > 0) {              // tst.w 6(a5,d0*8) ; bgt.s cantell
            // cantell:
            if (Mem.w(a5 + d2 * 8 + 6) <= 0) {         // tst.w 6(a5,d2*8) ; ble.s cliptotestsecbehind
                if (cliptotestsecbehind(a5, d0, d2)) {
                    return a0;                         // wallfacingaway
                }
                // bra cant_tell
            } else {
                // bra pastclip
                if (pastclip(a5, d0, d2)) {
                    return a0;                         // wallfacingaway
                }
            }
        } else {
            if (Mem.w(a5 + d2 * 8 + 6) <= 0) {         // tst.w 6(a5,d2*8) ; ble wallfacingaway
                return a0;
            }
            // bra cliptotestfirstbehind
            if (cliptotestfirstbehind(a5, d0, d2)) {
                return a0;                             // wallfacingaway
            }
            // bra cant_tell
        }

        // cant_tell:
        cantTell(a5, d0, d2);
        return a0;
    }

    /** cliptotestfirstbehind — renvoie true si wallfacingaway. */
    private static boolean cliptotestfirstbehind(int a5, int d0, int d2) {
        int d3 = Mem.l(a5 + d0 * 8);                   // move.l (a5,d0*8),d3
        d3 -= Mem.l(a5 + d2 * 8);                      // sub.l (a5,d2*8),d3
        int d6 = Mem.w(a5 + d0 * 8 + 6);               // move.w 6(a5,d0*8),d6
        d6 = setw(d6, d6 - Mem.uw(a5 + d2 * 8 + 6));   // sub.w 6(a5,d2*8),d6
        d6 = (short) d6;                               // ext.l d6
        d3 = (int) ((long) d3 / d6);                   // divs.l d6,d3
        d6 = Mem.w(a5 + d2 * 8 + 6);                   // move.w 6(a5,d2.w*8),d6 ; ext.l d6
        d3 = (int) ((long) d3 * d6);                   // muls.l d6,d3
        d3 = -d3;                                      // neg.l d3
        d3 += Mem.l(a5 + d2 * 8);                      // add.l (a5,d2*8),d3
        int d6b = Mem.l(a5 + d2 * 8);                  // move.l (a5,d2*8),d6
        int d4 = Mem.w(a5 + d2 * 8 + 6);               // move.w 6(a5,d2*8),d4 ; ext.l d4
        d6b = (int) ((long) d6b / d4);                 // divs.l d4,d6
        return d3 >= d6b;                              // cmp.l d6,d3 ; bge wallfacingaway ; bra cant_tell
    }

    /** cliptotestsecbehind — renvoie true si wallfacingaway. */
    private static boolean cliptotestsecbehind(int a5, int d0, int d2) {
        int d3 = Mem.l(a5 + d2 * 8);                   // move.l (a5,d2*8),d3
        d3 -= Mem.l(a5 + d0 * 8);                      // sub.l (a5,d0*8),d3
        int d6 = Mem.w(a5 + d2 * 8 + 6);               // move.w 6(a5,d2*8),d6
        d6 = setw(d6, d6 - Mem.uw(a5 + d0 * 8 + 6));   // sub.w 6(a5,d0*8),d6
        d6 = (short) d6;                               // ext.l d6
        d3 = (int) ((long) d3 / d6);                   // divs.l d6,d3
        d6 = Mem.w(a5 + d0 * 8 + 6);                   // move.w 6(a5,d0.w*8),d6 ; ext.l d6
        d3 = (int) ((long) d3 * d6);                   // muls.l d6,d3
        d3 = -d3;                                      // neg.l d3
        d3 += Mem.l(a5 + d0 * 8);                      // add.l (a5,d0*8),d3
        int d6b = Mem.l(a5 + d0 * 8);                  // move.l (a5,d0*8),d6
        int d4 = Mem.w(a5 + d0 * 8 + 6);               // move.w 6(a5,d0*8),d4 ; ext.l d4
        d6b = (int) ((long) d6b / d4);                 // divs.l d4,d6
        return d3 <= d6b;                              // cmp.l d6,d3 ; ble wallfacingaway ; bra cant_tell
    }

    /** pastclip — renvoie true si wallfacingaway. */
    private static boolean pastclip(int a5, int d0, int d2) {
        int d3 = Mem.l(a5 + d0 * 8);                   // move.l (a5,d0*8),d3
        int d4 = Mem.w(a5 + d0 * 8 + 6);               // move.w 6(a5,d0*8),d4 ; ext.l d4
        d3 = (int) ((long) d3 / d4);                   // divs.l d4,d3
        d4 = (short) Mem.uw(Vid_CentreX_w);            // move.w Vid_CentreX_w,d4 ; ext.l d4
        d3 += d4;                                      // add.l d4,d3
        int d6 = Mem.l(a5 + d2 * 8);                   // move.l (a5,d2*8),d6
        d4 = Mem.w(a5 + d2 * 8 + 6);                   // move.w 6(a5,d2*8),d4 ; ext.l d4
        d6 = (int) ((long) d6 / d4);                   // divs.l d4,d6
        d4 = (short) Mem.uw(Vid_CentreX_w);            // move.w Vid_CentreX_w,d4 ; ext.l d4
        d6 += d4;                                      // add.l d4,d6
        d4 = (short) Mem.uw(Vid_RightX_w);             // move.w Vid_RightX_w,d4 ; ext.l d4
        if (d3 >= d4) {                                // cmp.l d4,d3 ; bge wallfacingaway
            return true;
        }
        if (d3 >= d6) {                                // cmp.l d6,d3 ; bge wallfacingaway
            return true;
        }
        return d6 < 0;                                 // tst.l d6 ; blt wallfacingaway
    }

    /** cant_tell — calcule les brightnesses de coins, choisit le renderer, dessine. */
    private static void cantTell(int a5, int d0, int d2) {
        // movem.l d7/a0/a5/a6,-(a7)
        int a0 = Mem.l(a5 + d0 * 8);                   // move.l (a5,d0*8),a0 ; left rotated X
        int d1 = Mem.w(a5 + d0 * 8 + 6);               // move.w 6(a5,d0*8),d1 ; left dist
        int a2 = Mem.l(a5 + d2 * 8);                   // move.l (a5,d2*8),a2 ; right rotated X
        int d3 = Mem.w(a5 + d2 * 8 + 6);               // move.w 6(a5,d2*8),d3 ; right dist

        int pb = CurrentPointBrights_vl;               // move.l #CurrentPointBrights_vl,a5
        if (Mem.b(Draw_DoUpper_b) != 0) {              // tst.b Draw_DoUpper_b ; beq.s .notupper
            pb += 4;                                   // add.w #4,a5
        }

        // .notupper: — brightness gauche/droite (bas)
        int d4z = Mem.uw(Draw_CurrentZone_w);          // move.w Draw_CurrentZone_w,d0
        int d4b = Mem.ub(draw_WhichPBR_w);             // move.b draw_WhichPBR_w,d4
        if ((d4b & (1 << 3)) != 0) {                   // btst #3,d4 ; beq.s .nototherzone
            d4z = Mem.uw(draw_OtherZone_w);            // move.w draw_OtherZone_w,d0
        }
        // .nototherzone:
        d4b = setw(d4b, d4b & 7);                      // and.w #7,d4
        d4z = muls(d4z, 40);                           // muls #40,d0
        d4b = setw(d4b, d4b + d4z);                    // add.w d0,d4

        int dl = Mem.uw(draw_WhichLeftPoint_w);        // move.w draw_WhichLeftPoint_w,d0
        dl = setw(dl, dl << 2);                        // asl.w #2,d0
        dl = setw(dl, dl + d4b);                       // add.w d4,d0
        dl = Mem.w(pb + ((short) dl) * 2);             // move.w (a5,d0.w*2),d0
        if ((short) dl < 0) {                          // bge.s .okpos1
            dl = setw(dl, -(short) dl);                // neg.w d0
        }
        // .okpos1:
        dl = setw(dl, dl + Mem.uw(draw_WallBrightOffset_w)); // add.w draw_WallBrightOffset_w,d0
        Mem.ww(draw_LeftWallBright_w, dl);             // move.w d0,draw_LeftWallBright_w

        int dr = Mem.uw(draw_WhichRightPoint_w);       // move.w draw_WhichRightPoint_w,d0
        dr = setw(dr, dr << 2);                        // asl.w #2,d0
        dr = setw(dr, dr + d4b);                       // add.w d4,d0
        dr = Mem.w(pb + ((short) dr) * 2);             // move.w (a5,d0.w*2),d0
        if ((short) dr < 0) {                          // bge.s .okpos2
            dr = setw(dr, -(short) dr);                // neg.w d0
        }
        // .okpos2:
        dr = setw(dr, dr + Mem.uw(draw_WallBrightOffset_w)); // add.w draw_WallBrightOffset_w,d0
        Mem.ww(draw_RightWallBright_w, dr);            // move.w d0,draw_RightWallBright_w

        // brightness gauche/droite (haut)
        d4z = Mem.uw(Draw_CurrentZone_w);              // move.w Draw_CurrentZone_w,d0
        d4b = Mem.ub(draw_WhichPBR_w);                 // move.b draw_WhichPBR_w,d4
        d4b = (d4b & 0xFFFF) >>> 4;                    // lsr.w #4,d4
        if ((d4b & (1 << 3)) != 0) {                   // btst #3,d4 ; beq.s .nototherzone2
            d4z = Mem.uw(draw_OtherZone_w);            // move.w draw_OtherZone_w,d0
        }
        // .nototherzone2:
        d4b = setw(d4b, d4b & 7);                      // and.w #7,d4
        d4z = muls(d4z, 40);                           // muls #40,d0
        d4b = setw(d4b, d4b + d4z);                    // add.w d0,d4

        dl = Mem.uw(draw_WhichLeftPoint_w);            // move.w draw_WhichLeftPoint_w,d0
        dl = setw(dl, dl << 2);                        // asl.w #2,d0
        dl = setw(dl, dl + d4b);                       // add.w d4,d0
        dl = Mem.w(pb + ((short) dl) * 2);             // move.w (a5,d0.w*2),d0
        if ((short) dl < 0) {                          // bge.s .okpos3
            dl = setw(dl, -(short) dl);                // neg.w d0
        }
        // .okpos3:
        dl = setw(dl, dl + Mem.uw(draw_WallBrightOffset_w)); // add.w draw_WallBrightOffset_w,d0
        Mem.ww(draw_LeftWallTopBright_w, dl);          // move.w d0,draw_LeftWallTopBright_w

        dr = Mem.uw(draw_WhichRightPoint_w);           // move.w draw_WhichRightPoint_w,d0
        dr = setw(dr, dr << 2);                        // asl.w #2,d0
        dr = setw(dr, dr + d4b);                       // add.w d4,d0
        dr = Mem.w(pb + ((short) dr) * 2);             // move.w (a5,d0.w*2),d0
        if ((short) dr < 0) {                          // bge.s .okpos4
            dr = setw(dr, -(short) dr);                // neg.w d0
        }
        // .okpos4:
        dr = setw(dr, dr + Mem.uw(draw_WallBrightOffset_w)); // add.w draw_WallBrightOffset_w,d0
        Mem.ww(draw_RightWallTopBright_w, dr);         // move.w d0,draw_RightWallTopBright_w

        int d4 = 0;                                    // move.w #0,d4 (draw_WallLeftEnd_w)
        int d7 = max3ddiv();                           // move.l #max3ddiv,d7

        Mem.ww(draw_WallLastStripX_w, -1);             // move.w #-1,draw_WallLastStripX_w

        // .test_one_in_front:
        if ((short) d1 <= 0 && (short) d3 <= 0) {      // tst.w d1 ; bgt .one_in_front ; tst.w d3 ; bgt ; bra .function_done
            return;                                    // .function_done
        }

        // .one_in_front: — détermine la finesse de subdivision (d7=iterations, d6=count)
        d7 = 16;                                       // move.w #16,d7
        int d6 = 2;                                    // move.w #2,d6
        if (Mem.b(Draw_GoodRender_b) != 0) {           // tst.b Draw_GoodRender_b ; beq.s .not_good
            d7 = 64;                                   // move.w #64,d7
            d6 = 4;                                    // move.w #4,d6
            // bra .is_good
        } else {
            // .not_good:
            int dd = a2 - a0;                          // move.l a2,d0 ; sub.l a0,d0
            if (dd < 0) {                              // bge.s .okpos
                dd = -dd;                              // neg.l d0
            }
            // .okpos:
            if (dd >= 256 * 128) {                     // cmp.l #256*128,d0 ; blt.s .is_good
                d7 += d7;                              // add.w d7,d7
                d6 += 1;                               // addq #1,d6
                if (dd >= 512 * 128) {                 // cmp.l #512*128,d0 ; blt.s .is_good
                    d7 += d7;                          // add.w d7,d7
                    d6 += 1;                           // addq #1,d6
                }
            }
        }

        // .is_good:
        int d0d = setw(0, d3);                         // move.w d3,d0
        d0d = setw(d0d, d0d - d1);                     // sub.w d1,d0
        if ((short) d0d < 0) {                         // bge.s .not_negative_z_difference
            d0d = setw(d0d, -(short) d0d);             // neg.w d0
        }
        // .not_negative_z_difference:
        if ((short) d0d >= 512 && Mem.b(Draw_GoodRender_b) != 0) { // cmp.w #512,d0 ; blt.s .nd0 ; tst.b Draw_GoodRender_b ; beq.s .nd0
            d7 += d7;                                  // add.w d7,d7
            d6 = setw(d6, d6 + 1);                     // add.w #1,d6
            // bra .nha
        } else {
            // .nd0:
            if ((short) d0d <= 256) {                  // cmp.w #256,d0 ; bgt.s .nh1
                d7 = setw(d7, ((short) d7) >> 1);      // asr.w #1,d7
                d6 = setw(d6, d6 - 1);                 // subq #1,d6
            }
            // .nh1:
            if ((short) d0d <= 128) {                  // cmp.w #128,d0 ; bgt.s .nh2
                d7 = setw(d7, ((short) d7) >> 1);      // asr.w #1,d7
                d6 = setw(d6, d6 - 1);                 // subq #1,d6
            }
            // .nh2: .nha:
        }

        d0d = setw(d0d, d3);                           // move.w d3,d0
        if ((short) d3 < (short) d1) {                 // cmp.w d1,d3 ; blt.s .right_nearest
            d0d = setw(d0d, d1);                       // move.w d1,d0
        }
        // .right_nearest:
        if ((short) d0d <= 32) {                       // cmp.w #32,d0 ; bgt.s .ndd0
            d6 = setw(d6, d6 + 1);                     // addq #1,d6
            d7 += d7;                                  // add.w d7,d7
        }
        // .ndd0:
        if ((short) d0d <= 64) {                       // cmp.w #64,d0 ; bgt.s .nd1
            d6 = setw(d6, d6 + 1);                     // addq #1,d6
            d7 += d7;                                  // add.w d7,d7
        }
        // .nd1:
        boolean nh4 = false;
        if ((short) d0d >= 128) {                      // cmp.w #128,d0 ; blt.s .nh3
            d7 = setw(d7, ((short) d7) >> 1);          // asr.w #1,d7
            d6 = setw(d6, d6 - 1);                     // subq #1,d6
            if ((short) d6 < 0) {                      // blt.s .nh4
                nh4 = true;
            } else if ((short) d0d >= 256) {           // cmp.w #256,d0 ; blt.s .nh3
                d7 = setw(d7, ((short) d7) >> 1);      // asr.w #1,d7
                d6 = setw(d6, d6 - 1);                 // subq #1,d6
                if ((short) d6 < 0) {                  // blt.s .nh4
                    nh4 = true;
                }
            }
        }
        // .nh3:
        if (!nh4 && (short) d0d >= 512) {              // cmp.w #512,d0 ; blt.s .nh4
            d7 = setw(d7, ((short) d7) >> 1);          // asr.w #1,d7
            d6 = setw(d6, d6 - 1);                     // subq #1,d6
        }
        // .nh4:
        if ((short) d7 > 128) {                        // cmp.w #128,d7 ; ble.s .choose_renderer
            d7 = 128;                                  // move.w #128,d7
            d6 = 5;                                    // move.w #5,d6
        }

        // .choose_renderer:
        if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_LIGHTING)) { // DEV_CHECK_SET SKIP_LIGHTING,.dev_draw_fullbright
            // .dev_draw_fullbright:
            Mem.ww(draw_LeftWallBright_w, 1);          // move.w #1,draw_LeftWallBright_w
            Mem.ww(draw_LeftWallTopBright_w, 1);       // move.w #1,draw_LeftWallTopBright_w
            Mem.ww(draw_RightWallBright_w, 1);         // move.w #1,draw_RightWallBright_w
            Mem.ww(draw_RightWallTopBright_w, 1);      // move.w #1,draw_RightWallTopBright_w
            draw_WallSimpleShaded(a0, d1, a2, d3, d4, d5current(), d6, d7); // bsr draw_WallSimpleShaded
            return;                                    // .function_done
        }

        boolean doSimple;
        if (Mem.b(Draw_ForceSimpleWalls_b) != 0) {     // tst.b Draw_ForceSimpleWalls_b ; beq.s .check_corners
            int dd = Mem.uw(draw_LeftWallBright_w);    // move.w draw_LeftWallBright_w,d0
            dd = setw(dd, dd + Mem.uw(draw_LeftWallTopBright_w)); // add.w draw_LeftWallTopBright_w,d0
            dd = setw(dd, ((short) dd) >> 1);          // asr.w #1,d0
            Mem.ww(draw_LeftWallBright_w, dd);         // move.w d0,draw_LeftWallBright_w
            dd = Mem.uw(draw_RightWallBright_w);       // move.w draw_RightWallBright_w,d0
            dd = setw(dd, dd + Mem.uw(draw_RightWallTopBright_w)); // add.w draw_RightWallTopBright_w,d0
            dd = setw(dd, ((short) dd) >> 1);          // asr.w #1,d0
            Mem.ww(draw_RightWallBright_w, dd);        // move.w d0,draw_RightWallBright_w
            doSimple = true;                           // bra.s .do_simple_shaded
        } else {
            // .check_corners: — différence de brightness gauche/droite < 2 → simple
            int dd = Mem.uw(draw_LeftWallBright_w);    // move.w draw_LeftWallBright_w,d0
            dd = setw(dd, dd - Mem.uw(draw_LeftWallTopBright_w)); // sub.w draw_LeftWallTopBright_w,d0
            if ((short) dd < 0) {                      // bge.s .left_delta_positive
                dd = setw(dd, -(short) dd);            // neg.w d0
            }
            // .left_delta_positive:
            if ((short) dd > 2) {                      // cmp.w #2,d0 ; bgt.s .do_gouraud_shaded
                doSimple = false;
            } else {
                dd = Mem.uw(draw_RightWallBright_w);   // move.w draw_RightWallBright_w,d0
                dd = setw(dd, dd - Mem.uw(draw_RightWallTopBright_w)); // sub.w draw_RightWallTopBright_w,d0
                if ((short) dd < 0) {                  // bge.s .right_delta_positive
                    dd = setw(dd, -(short) dd);        // neg.w d0
                }
                // .right_delta_positive:
                doSimple = (short) dd <= 2;            // cmp.w #2,d0 ; bgt.s .do_gouraud_shaded
            }
        }

        if (doSimple) {
            // .do_simple_shaded:
            if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_SIMPLE_WALLS)) { // DEV_CHECK_SET SKIP_SIMPLE_WALLS,.function_done
                return;
            }
            draw_WallSimpleShaded(a0, d1, a2, d3, d4, d5current(), d6, d7); // bsr draw_WallSimpleShaded
        } else {
            // .do_gouraud_shaded:
            if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_SHADED_WALLS)) { // DEV_CHECK_SET SKIP_SHADED_WALLS,.function_done
                return;
            }
            Hiresgourwall.draw_WallGouraudShaded(a0, d1, a2, d3, d4, d5current(), d6, d7); // bsr draw_WallGouraudShaded
        }
        // .function_done: movem.l (a7)+,... ; rts
    }

    // d5 (endLen) au point cant_tell : c'est le 5e mot lu du record de mur
    // (draw d5 dans Draw_Wall, conservé via le flux). Comme cant_tell est
    // appelée après les calculs de brightness sans toucher d5, on le relit du
    // contexte. Ici d5 = la longueur de fin parsée plus haut ; on le passe via
    // un champ statique pour rester fidèle au registre d5 d'origine.
    private static int d5wall;

    private static int d5current() {
        return d5wall;
    }

    // ------------------------------------------------------------------
    // CODE MORT dans ce build (non traduit car jamais exécuté) :
    //  - usesimple / cliptopusesimple / simplewalliPACK0-2 / simplewallPACK0-2
    //    (hireswall.s:1323-1410) : aucune instruction ne branche vers usesimple.
    //  - Tout le sous-système see-through : screendividethru, scrdrawlop(thru),
    //    ScreenWallstripdrawthru, gotoendthru, usesimplethru/cliptopusesimplethru,
    //    simplewallthru*/simplewallhole*, drawwallthru*/drawwalldimthru*
    //    (hireswall.s:264-321, 1613-1926). Le seul point d'entrée est
    //    `bne screendividethru` (ligne 382) et `tst.b wall_SeeThrough_b` (384),
    //    tous deux COMMENTÉS dans l'original → murs transparents désactivés.
    // Ces routines seront portées si la fonctionnalité see-through est réactivée.
    // ------------------------------------------------------------------

    // Accesseurs / constantes
    private static int Draw_WallTexturePtrs_vl() {
        return ab3d2.bss.DrawBss.Draw_WallTexturePtrs_vl;
    }

    private static final int max3ddiv = Hires.max3ddiv;

    private static int max3ddiv() {
        return max3ddiv;
    }
}
