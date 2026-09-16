package ab3d2.modules.draw;

import ab3d2.Hires;
import ab3d2.HiresData;
import ab3d2.HireswallData;
import ab3d2.Mem;
import ab3d2.modules.RawKeyMacros;

import static ab3d2.M68k.divs;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.negw;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.bss.DrawBss.Draw_TexturePalettePtr_l;
import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.bss.TablesBss.Lvl_BigMap_vl;
import static ab3d2.bss.TablesBss.Lvl_CompactMap_vl;
import static ab3d2.bss.TablesBss.Rotated_vl;
import static ab3d2.bss.VidBss.Vid_DoubleWidth_b;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;

/**
 * Traduction littérale de ab3d2_source/modules/draw/draw_map.s
 *
 * Rendu de la carte 2D (mode REALMAP) : parcours de Lvl_CompactMap_vl
 * (bitmap d'état des murs, 3 bits par mur, 10 murs par zone) +
 * Lvl_BigMap_vl (paires d'indices de points), clipping de segments puis
 * tracé Bresenham (4 octants), avec mode transparent optionnel.
 */
public final class DrawMap {

    public static final int MAP_SOLID_WALL_PEN = 255;
    public static final int MAP_STEP_WALL_PEN = 254;

    private static final int _a0 = Mem.align(4);
    public static final int draw_BaseMapTransparencyPtr_l = Mem.alloc(4); // ds.l 1

    // (déclarés plus bas dans le fichier source, entre done_bottom_clip et map_offscreen)
    public static final int Draw_MapZoomLevel_w = Mem.dcW(3);   // DCLC dc.w 3
    public static final int Draw_MapTransparent_b = Mem.dcW(0); // DCLC dc.w 0 (accédé en byte)
    public static final int draw_MapXOffset_w = Mem.dcW(0);
    public static final int draw_MapZOffset_w = Mem.dcW(0);

    private DrawMap() {
    }

    /** DoTheMapWotNastyCharlesIsForcingMeToDo */
    public static void DoTheMapWotNastyCharlesIsForcingMeToDo() {
        // 0xABADCAFE - Fixme - make these assignable and remember to clear the keys
        // as the zoom speed is insane under emulations

        int a4 = Mem.l(Draw_TexturePalettePtr_l);      // move.l Draw_TexturePalettePtr_l,a4
        a4 += 256 * 25;                                // add.l #256*25,a4 ; glare offset
        Mem.wl(draw_BaseMapTransparencyPtr_l, a4);     // move.l a4,draw_BaseMapTransparencyPtr_l

        int a5 = KeyMap_vb;                            // move.l #KeyMap_vb,a5

        if (Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_ENTER) != 0) { // tst.b RAWKEY_NUM_ENTER(a5) ; beq.s .skip_render_toggle
            Mem.wb(a5 + RawKeyMacros.RAWKEY_NUM_ENTER, 0);    // clr.b RAWKEY_NUM_ENTER(a5)
            Mem.wb(Draw_MapTransparent_b, ~Mem.ub(Draw_MapTransparent_b)); // not.b Draw_MapTransparent_b
        }

        // .skip_render_toggle:
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_F1) != 0) { // tst.b RAWKEY_F1(a5) ; Zoom In ; beq.s .skip_zoom_in
            Mem.wb(a5 + RawKeyMacros.RAWKEY_F1, 0);    // clr.b RAWKEY_F1(a5)
            if (Mem.w(Draw_MapZoomLevel_w) != 0) {     // tst.w Draw_MapZoomLevel_w ; beq.s .skip_zoom_in
                Mem.ww(Draw_MapZoomLevel_w, Mem.w(Draw_MapZoomLevel_w) - 1); // sub.w #1,Draw_MapZoomLevel_w
            }
        }

        // .skip_zoom_in:
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_F2) != 0) { // tst.b RAWKEY_F2(a5) ; Zoom Out ; beq.s .skip_zoom_out
            Mem.wb(a5 + RawKeyMacros.RAWKEY_F2, 0);    // clr.b RAWKEY_F2(a5)
            if (Mem.w(Draw_MapZoomLevel_w) < 7) {      // cmp.w #7,Draw_MapZoomLevel_w ; bge.s .skip_zoom_out
                Mem.ww(Draw_MapZoomLevel_w, Mem.w(Draw_MapZoomLevel_w) + 1); // add.w #1,Draw_MapZoomLevel_w
            }
        }

        // .skip_zoom_out:
        int a1 = Rotated_vl;                           // move.l #Rotated_vl,a1
        int a2 = Lvl_CompactMap_vl;                    // move.l #Lvl_CompactMap_vl,a2
        int a3 = Lvl_BigMap_vl - 40;                   // move.l #Lvl_BigMap_vl-40,a3

        int d0, d1, d2, d3, d4 = 0, d5, d6, d7;

        pre_show:
        while (true) { // pre_show:
            a3 += 40;                                  // add.w #40,a3

            show_map:
            while (true) { // show_map:
                d5 = Mem.l(a2); a2 += 4;               // move.l (a2)+,d5
                int d7a = a2;                          // move.l a2,d7
                if (d7a > Mem.l(HiresData.LastZonePtr_l)) { // cmp.l LastZonePtr_l,d7 ; bgt shown_map
                    shown_map(a4); // a4 hérité de la boucle (base ou base+512 après une porte)
                    return;
                }

                if (d5 == 0) {                         // tst.l d5 ; beq.s pre_show
                    continue pre_show;
                }

                d7 = 9;                                // move.w #9,d7

                do { // walls_of_zone:
                    boolean carry;
                    carry = (d5 & 1) != 0;             // asr.l #1,d5 ; bcs.s wall_seen
                    d5 >>= 1;
                    if (carry) {
                        // wall_seen:
                        a4 = Mem.l(draw_BaseMapTransparencyPtr_l); // move.l draw_BaseMapTransparencyPtr_l,a4
                        d4 = setw(d4, MAP_SOLID_WALL_PEN);         // move.w #MAP_SOLID_WALL_PEN,d4
                        carry = (d5 & 2) != 0;         // asr.l #2,d5 (retenue = dernier bit sorti)
                        d5 >>= 2;
                        if (carry) {                   // bcc.s .not_a_door
                            d4 = setw(d4, MAP_STEP_WALL_PEN);      // move.w #MAP_STEP_WALL_PEN,d4
                            a4 += 256 * 2;             // add.w #256*2,a4 ; 2 steps more transparent
                        }
                        // .not_a_door: → decided_colour
                    } else {
                        carry = (d5 & 1) != 0;         // asr.l #1,d5 ; bcs.s wall_mapped
                        d5 >>= 1;
                        if (carry) {
                            // wall_mapped:
                            d4 = setw(d4, 0xb00);      // move.w #$b00,d4
                            carry = (d5 & 1) != 0;     // asr.l #1,d5 ; bcc.s .not_a_door
                            d5 >>= 1;
                            if (carry) {
                                d4 = setw(d4, 0xe00);  // move.w #$e00,d4
                            }
                            // .not_a_door: → decided_colour
                        } else {
                            d5 >>= 1;                  // asr.l #1,d5
                            a3 += 4;                   // addq #4,a3
                            // bra decided_wall
                            d7 = setw(d7, d7 - 1);     // dbra d7,walls_of_zone
                            if ((short) d7 == -1) {
                                continue show_map;     // bra show_map
                            }
                            continue;
                        }
                    }

                    // decided_colour:
                    d6 = Mem.w(a3); a3 += 2;           // move.w (a3)+,d6
                    d0 = Mem.l(a1 + ((short) d6) * 8); // move.l (a1,d6.w*8),d0
                    d0 >>= 7;                          // asr.l #7,d0
                    // movem.l d7/d5,-(a7) — d7/d5 sont des locaux, préservés naturellement
                    int s5 = Mem.w(draw_MapXOffset_w); // move.w draw_MapXOffset_w,d5 ; ext.l d5
                    d0 += s5;                          // add.l d5,d0
                    d1 = Mem.l(a1 + ((short) d6) * 8 + 4); // move.l 4(a1,d6.w*8),d1
                    s5 = Mem.w(draw_MapZOffset_w);     // move.w draw_MapZOffset_w,d5 ; ext.l d5
                    d1 += s5;                          // add.l d5,d1
                    d6 = Mem.w(a3); a3 += 2;           // move.w (a3)+,d6
                    d2 = Mem.l(a1 + ((short) d6) * 8); // move.l (a1,d6.w*8),d2
                    s5 = Mem.w(draw_MapXOffset_w);     // move.w draw_MapXOffset_w,d5 ; ext.l d5
                    d2 >>= 7;                          // asr.l #7,d2
                    d2 += s5;                          // add.l d5,d2
                    d3 = Mem.l(a1 + ((short) d6) * 8 + 4); // move.l 4(a1,d6.w*8),d3
                    s5 = Mem.w(draw_MapZOffset_w);     // move.w draw_MapZOffset_w,d5 ; ext.l d5
                    d3 += s5;                          // add.l d5,d3
                    d1 = -d1;                          // neg.l d1
                    d3 = -d3;                          // neg.l d3
                    draw_MapClipAndDraw(d0, d1, d2, d3, d4, a4); // bsr draw_MapClipAndDraw
                    // movem.l (a7)+,d7/d5

                    // 0xABADCAFE - TODO - Knowing if a wall is a door may help PVS in future
                    // decided_wall:
                    d7 = setw(d7, d7 - 1);             // dbra d7,walls_of_zone
                } while ((short) d7 != -1);
                // bra show_map (boucle)
            }
        }
    }

    /** shown_map — dessine la flèche du joueur. FIXME (original): why does map rendering have an effect on wall rendering? */
    private static void shown_map(int a4) {
        int d0, d1, d2, d3, d4;

        d0 = setw(0, Mem.uw(draw_MapXOffset_w));       // move.w draw_MapXOffset_w,d0
        d1 = setw(0, Mem.uw(draw_MapZOffset_w));       // move.w draw_MapZOffset_w,d1
        d1 = negw(d1);                                 // neg.w d1
        d2 = setw(0, d0);                              // move.w d0,d2
        d3 = setw(0, d1);                              // move.w d1,d3
        d1 = setw(d1, d1 - (64 - 32));                 // sub.w #64-32,d1
        d3 = setw(d3, d3 - (32 - 32));                 // sub.w #32-32,d3
        d2 = setw(d2, d2 - 64);                        // sub.w #64,d2
        d4 = 250;                                      // move.w #250,d4
        draw_MapClipAndDraw(d0, d1, d2, d3, d4, a4);   // bsr draw_MapClipAndDraw

        d0 = setw(0, Mem.uw(draw_MapXOffset_w));       // move.w draw_MapXOffset_w,d0
        d1 = setw(0, Mem.uw(draw_MapZOffset_w));       // move.w draw_MapZOffset_w,d1
        d1 = negw(d1);                                 // neg.w d1
        d2 = setw(0, d0);                              // move.w d0,d2
        d3 = setw(0, d1);                              // move.w d1,d3
        d1 = setw(d1, d1 - (64 - 32));                 // sub.w #64-32,d1
        d3 = setw(d3, d3 - (32 - 32));                 // sub.w #32-32,d3
        d2 = setw(d2, d2 + 64);                        // add.w #64,d2
        d4 = 250;                                      // move.w #250,d4
        draw_MapClipAndDraw(d0, d1, d2, d3, d4, a4);   // bsr draw_MapClipAndDraw
        // rts
    }

    /**
     * draw_MapClipAndDraw — d0=x1, d1=y1, d2=x2, d3=y2, d4=pen,
     * a4=table de transparence (mode transparent).
     */
    public static void draw_MapClipAndDraw(int d0, int d1, int d2, int d3, int d4, int a4) {
        int d5, d6;

        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; beq.s .nodov
            // This is scaling the coordinates by 3/5 for Fullscreen (1229/2048)
            // move.l d1,-(sp) ; todo - find a free register
            int saved_d1 = d1;
            d1 = 1229;                                 // move.w #1229,d1
            d0 = muls(d0, d1);                         // muls d1,d0 ; 320 * 3/5 = 192
            d2 = muls(d2, d1);                         // muls d1,d2
            d1 = 11;                                   // move.l #11,d1
            d0 >>= d1;                                 // asr.l d1,d0
            d2 >>= d1;                                 // asr.l d1,d2
            d1 = saved_d1;                             // move.l (sp)+,d1
        }

        // .nodov:
        if (Mem.b(Vid_DoubleWidth_b) != 0) {           // tst.b Vid_DoubleWidth_b ; beq.s .no_double_width
            d0 = setw(d0, ((short) d0) >> 1);          // asr.w #1,d0 ; correct aspect ratio for DW/DH
            d2 = setw(d2, ((short) d2) >> 1);          // asr.w #1,d2
        }

        // .no_double_width:
        // tst.b Vid_DoubleHeight_b (branche et asr commentés dans l'original)

        // .no_double_height:
        d5 = setw(0, Mem.uw(Draw_MapZoomLevel_w));     // move.w Draw_MapZoomLevel_w,d5 ; is this the map zoom?
        int sh = d5 & 63;                              // (asr.w dN : compte modulo 64 ; zoom 0..7)
        d0 = setw(d0, ((short) d0) >> sh);             // asr.w d5,d0
        d1 = setw(d1, ((short) d1) >> sh);             // asr.w d5,d1
        d2 = setw(d2, ((short) d2) >> sh);             // asr.w d5,d2
        d3 = setw(d3, ((short) d3) >> sh);             // asr.w d5,d3

        // no_scaling:
        d0 = setw(d0, d0 + Mem.w(HiresData.Vid_CentreX_w)); // add.w Vid_CentreX_w,d0
        if ((short) d0 < 0) {                          // bge p1xpos
            d2 = setw(d2, d2 + Mem.w(HiresData.Vid_CentreX_w)); // add.w Vid_CentreX_w,d2
            if ((short) d2 < 0) {                      // blt map_offscreen
                return;
            }
            // x1nx2p: X1<0, X2>0, clip against X=0
            d6 = setw(0, d2);                          // move.w d2,d6
            d6 = setw(d6, d6 - d0);                    // sub.w d0,d6 ; dx
            if ((short) d6 == 0) {                     // beq map_offscreen ; dx == 0?
                return;
            }
            d5 = setw(0, d3);                          // move.w d3,d5
            d5 = setw(d5, d5 - d1);                    // sub.w d1,d5 ; dy
            d5 = muls(d5, d0);                         // muls.w d0,d5 ; x1 * dy
            d5 = divs(d5, d6);                         // divs.w d6,d5 ; x1 * dy / dx
            d1 = setw(d1, d1 - d5);                    // sub.w d5,d1 ; y1 = y1 - x * dy / dx
            d0 = 0;                                    // moveq.l #0,d0 ; x1 = 0
        } else {
            // p1xpos:
            d2 = setw(d2, d2 + Mem.w(HiresData.Vid_CentreX_w)); // add.w Vid_CentreX_w,d2
            if ((short) d2 < 0) {                      // bge done_left_clip
                d6 = setw(0, d0);                      // move.w d0,d6
                d6 = setw(d6, d6 - d2);                // sub.w d2,d6 ; dx
                if ((short) d6 <= 0) {                 // ble map_offscreen ; dx == 0?
                    return;
                }
                d5 = setw(0, d1);                      // move.w d1,d5
                d5 = setw(d5, d5 - d3);                // sub.w d3,d5 ; dy
                d5 = muls(d5, d2);                     // muls.w d2,d5 ; x2 * dy
                d5 = divs(d5, d6);                     // divs.w d6,d5 ; x2 * dy / dx
                d3 = setw(d3, d3 - d5);                // sub.w d5,d3 ; y2 = y2 - x2 * dy / dx
                d2 = 0;                                // moveq.l #0,d2 ; x2 == 0
            }
        }

        // done_left_clip:
        if ((short) d0 >= Mem.w(HiresData.Vid_RightX_w)) { // cmp.w Vid_RightX_w,d0 ; blt p1xneg
            if ((short) d2 >= Mem.w(HiresData.Vid_RightX_w)) { // cmp.w Vid_RightX_w,d2 ; bge map_offscreen
                return;
            }
            d6 = setw(0, d0);                          // move.w d0,d6
            d6 = setw(d6, d6 - d2);                    // sub.w d2,d6 ; dx
            if ((short) d6 == 0) {                     // beq map_offscreen
                return;
            }
            d5 = setw(0, d3);                          // move.w d3,d5
            d5 = setw(d5, d5 - d1);                    // sub.w d1,d5 ; dy

            d0 = setw(d0, d0 - Mem.w(HiresData.Vid_RightX_w)); // sub.w Vid_RightX_w,d0
            d0 = setw(d0, d0 + 1);                     // addq.w #1,d0

            d0 = muls(d0, d5);                         // muls.w d5,d0 ; dy * (rightx - x1)
            d0 = divs(d0, d6);                         // divs.w d6,d0
            d1 = setw(d1, d1 + d0);                    // add.w d0,d1 ; y1 + dy/dx * (rightx - x1)
            d0 = setw(d0, Mem.uw(HiresData.Vid_RightX_w)); // move.w Vid_RightX_w,d0
            d0 = setw(d0, d0 - 1);                     // subq.w #1,d0
        } else {
            // p1xneg:
            if ((short) d2 >= Mem.w(HiresData.Vid_RightX_w)) { // cmp.w Vid_RightX_w,d2 ; blt done_right_clip
                d6 = setw(0, d2);                      // move.w d2,d6
                d6 = setw(d6, d6 - d0);                // sub.w d0,d6
                if ((short) d6 <= 0) {                 // ble map_offscreen
                    return;
                }
                d2 = setw(d2, d2 - Mem.w(HiresData.Vid_RightX_w)); // sub.w Vid_RightX_w,d2
                d2 = setw(d2, d2 + 1);                 // addq.w #1,d2
                d5 = setw(0, d1);                      // move.w d1,d5
                d5 = setw(d5, d5 - d3);                // sub.w d3,d5

                d2 = muls(d2, d5);                     // muls.w d5,d2
                d2 = divs(d2, d6);                     // divs.w d6,d2
                d3 = setw(d3, d3 + d2);                // add.w d2,d3
                d2 = setw(d2, Mem.uw(HiresData.Vid_RightX_w)); // move.w Vid_RightX_w,d2
                d2 = setw(d2, d2 - 1);                 // subq.w #1,d2
            }
        }

        // done_right_clip:
        d1 = setw(d1, d1 + Mem.w(HireswallData.TOTHEMIDDLE)); // add.w TOTHEMIDDLE,d1
        if ((short) d1 < 0) {                          // bge p1ypos
            d3 = setw(d3, d3 + Mem.w(HireswallData.TOTHEMIDDLE)); // add.w TOTHEMIDDLE,d3
            if ((short) d3 < 0) {                      // blt map_offscreen
                return;
            }
            d6 = setw(0, d3);                          // move.w d3,d6
            d6 = setw(d6, d6 - d1);                    // sub.w d1,d6
            if ((short) d6 <= 0) {                     // ble map_offscreen
                return;
            }
            d5 = setw(0, d2);                          // move.w d2,d5
            d5 = setw(d5, d5 - d0);                    // sub.w d0,d5
            d5 = muls(d5, d1);                         // muls.w d1,d5
            d5 = divs(d5, d6);                         // divs.w d6,d5
            d0 = setw(d0, d0 - d5);                    // sub.w d5,d0
            d1 = 0;                                    // moveq.l #0,d1
        } else {
            // p1ypos:
            d3 = setw(d3, d3 + Mem.w(HireswallData.TOTHEMIDDLE)); // add.w TOTHEMIDDLE,d3
            if ((short) d3 < 0) {                      // bge done_top_clip ; (Vid_CentreY_w)
                d6 = setw(0, d1);                      // move.w d1,d6
                d6 = setw(d6, d6 - d3);                // sub.w d3,d6
                if ((short) d6 <= 0) {                 // ble map_offscreen
                    return;
                }
                d5 = setw(0, d0);                      // move.w d0,d5
                d5 = setw(d5, d5 - d2);                // sub.w d2,d5
                d5 = muls(d5, d3);                     // muls.w d3,d5
                d5 = divs(d5, d6);                     // divs.w d6,d5
                d2 = setw(d2, d2 - d5);                // sub.w d5,d2
                d3 = 0;                                // moveq.l #0,d3
            }
        }

        // done_top_clip:
        if ((short) d1 >= Mem.w(HireswallData.Vid_BottomY_w)) { // cmp.w Vid_BottomY_w,d1 ; blt p1yneg
            if ((short) d3 >= Mem.w(HireswallData.Vid_BottomY_w)) { // bge map_offscreen
                return;
            }
            d6 = setw(0, d1);                          // move.w d1,d6
            d6 = setw(d6, d6 - d3);                    // sub.w d3,d6
            if ((short) d6 <= 0) {                     // ble map_offscreen
                return;
            }
            d1 = setw(d1, d1 - Mem.w(HireswallData.Vid_BottomY_w)); // sub.w Vid_BottomY_w,d1
            d1 = setw(d1, d1 + 1);                     // addq.w #1,d1
            d5 = setw(0, d2);                          // move.w d2,d5
            d5 = setw(d5, d5 - d0);                    // sub.w d0,d5
            d1 = muls(d1, d5);                         // muls.w d5,d1
            d1 = divs(d1, d6);                         // divs.w d6,d1
            d0 = setw(d0, d0 + d1);                    // add.w d1,d0
            d1 = setw(d1, Mem.uw(HireswallData.Vid_BottomY_w)); // move.w Vid_BottomY_w,d1
            d1 = setw(d1, d1 - 1);                     // subq.w #1,d1
        } else {
            // p1yneg:
            if ((short) d3 >= Mem.w(HireswallData.Vid_BottomY_w)) { // cmp.w Vid_BottomY_w,d3 ; blt done_bottom_clip
                d6 = setw(0, d3);                      // move.w d3,d6
                d6 = setw(d6, d6 - d1);                // sub.w d1,d6
                if ((short) d6 <= 0) {                 // ble map_offscreen
                    return;
                }
                d3 = setw(d3, d3 - Mem.w(HireswallData.Vid_BottomY_w)); // sub.w Vid_BottomY_w,d3
                d3 = setw(d3, d3 + 1);                 // addq.w #1,d3
                d5 = setw(0, d0);                      // move.w d0,d5
                d5 = setw(d5, d5 - d2);                // sub.w d2,d5
                d3 = muls(d3, d5);                     // muls.w d5,d3
                d3 = divs(d3, d6);                     // divs.w d6,d3
                d2 = setw(d2, d2 + d3);                // add.w d3,d2
                d3 = setw(d3, Mem.uw(HireswallData.Vid_BottomY_w)); // move.w Vid_BottomY_w,d3
                d3 = setw(d3, d3 - 1);                 // subq.w #1,d3
            }
        }

        // done_bottom_clip:
        // Transparent drawing does work, but is somewhat broken (TODO original)
        draw_MapLine(d0, d1, d2, d3, d4, a4);          // bra draw_MapLine
    }

    /** draw_MapLine — tracé Bresenham, 4 octants, mode solide ou transparent. */
    public static void draw_MapLine(int d0, int d1, int d2, int d3, int d4, int a4) {
        int a0 = Mem.l(Vid_FastBufferPtr_l);           // move.l Vid_FastBufferPtr_l,a0 ; screen to render to.
        if ((short) d3 <= (short) d1) {                // cmp.w d1,d3 ; bgt.s .okdown
            if ((short) d3 == (short) d1) {            // bne.s .aline
                if ((short) d2 == (short) d0) {        // cmp.w d0,d2 ; beq.s no_line
                    return;                            // no_line: rts
                }
            }
            // .aline:
            int t = d0; d0 = d2; d2 = t;               // exg d0,d2
            t = d1; d1 = d3; d3 = t;                   // exg d1,d3
        }

        // .okdown:
        int d5 = setw(0, d1);                          // move.w d1,d5
        d5 = muls(d5, Hires.SCREEN_WIDTH);             // muls #SCREEN_WIDTH,d5
        a0 += d5;                                      // add.l d5,a0
        a0 += (short) d0;                              // lea (a0,d0.w),a0

        d3 = setw(d3, d3 - d1);                        // sub.w d1,d3

        d2 = setw(d2, d2 - d0);                        // sub.w d0,d2
        int d6, d7;
        if ((short) d2 >= 0) {                         // bge.s down_right
            // down_right:
            if ((short) d3 > (short) d2) {             // cmp.w d2,d3 ; bgt.s down_more_right
                // down_more_right:
                d6 = Hires.SCREEN_WIDTH;               // move.w #SCREEN_WIDTH,d6
                d0 = setw(d0, d3);                     // move.w d3,d0
                d7 = setw(0, d3);                      // move.w d3,d7
                if (Mem.b(Draw_MapTransparent_b) == 0) { // tst.b Draw_MapTransparent_b ; bne.s .line_loop_transparent
                    do { // .line_loop: regular solid colour mode
                        Mem.wb(a0, d4);                // move.b d4,(a0)
                        a0 += (short) d6;              // add.w d6,a0
                        d0 = setw(d0, d0 - d2);        // sub.w d2,d0
                        if ((short) d0 <= 0) {         // bgt.s .no_extra
                            d0 = setw(d0, d0 + d3);    // add.w d3,d0
                            a0 += 1;                   // addq #1,a0
                        }
                        // .no_extra:
                        d7 = setw(d7, d7 - 1);         // dbra d7,.line_loop
                    } while ((short) d7 != -1);
                    return;                            // rts
                }
                do { // .line_loop_transparent:
                    d4 = setb(d4, Mem.ub(a0));         // move.b (a0),d4 ; read chunky buffer
                    Mem.wb(a0, Mem.ub(a4 + (short) d4)); // move.b (a4,d4.w),(a0) ; Replace and write back
                    a0 += (short) d6;                  // add.w d6,a0
                    d0 = setw(d0, d0 - d2);            // sub.w d2,d0
                    if ((short) d0 <= 0) {             // bgt.s .no_extra_transparent
                        d0 = setw(d0, d0 + d3);        // add.w d3,d0
                        a0 += 1;                       // addq #1,a0
                    }
                    // .no_extra_transparent:
                    d7 = setw(d7, d7 - 1);             // dbra d7,.line_loop_transparent
                } while ((short) d7 != -1);
                return;                                // rts
            }
            // down_right_more:
            d6 = Hires.SCREEN_WIDTH;                   // move.w #SCREEN_WIDTH,d6
            d0 = setw(d0, d2);                         // move.w d2,d0
            d7 = setw(0, d2);                          // move.w d2,d7
            if (Mem.b(Draw_MapTransparent_b) == 0) {   // tst.b Draw_MapTransparent_b
                do { // .line_loop: regular solid colour mode
                    Mem.wb(a0, d4); a0 += 1;           // move.b d4,(a0)+
                    d0 = setw(d0, d0 - d3);            // sub.w d3,d0
                    if ((short) d0 <= 0) {             // bgt.s .no_extra
                        d0 = setw(d0, d0 + d2);        // add.w d2,d0
                        a0 += (short) d6;              // add.w d6,a0
                    }
                    // .no_extra:
                    d7 = setw(d7, d7 - 1);             // dbra d7,.line_loop
                } while ((short) d7 != -1);
                return;                                // rts
            }
            do { // .line_loop_transparent:
                d4 = setb(d4, Mem.ub(a0));             // move.b (a0),d4 ; read chunky buffer
                Mem.wb(a0, Mem.ub(a4 + (short) d4)); a0 += 1; // move.b (a4,d4.w),(a0)+ ; Replace and write back
                d0 = setw(d0, d0 - d3);                // sub.w d3,d0
                if ((short) d0 <= 0) {                 // bgt.s .no_extra_transparent
                    d0 = setw(d0, d0 + d2);            // add.w d2,d0
                    a0 += (short) d6;                  // add.w d6,a0
                }
                // .no_extra_transparent:
                d7 = setw(d7, d7 - 1);                 // dbra d7,.line_loop_transparent
            } while ((short) d7 != -1);
            return;                                    // rts
        }

        // down_left:
        d2 = negw(d2);                                 // neg.w d2
        if ((short) d3 > (short) d2) {                 // cmp.w d2,d3 ; bgt.s down_more_left
            // down_more_left:
            d6 = Hires.SCREEN_WIDTH;                   // move.w #SCREEN_WIDTH,d6
            d0 = setw(d0, d3);                         // move.w d3,d0
            d7 = setw(0, d3);                          // move.w d3,d7
            if (Mem.b(Draw_MapTransparent_b) == 0) {   // tst.b Draw_MapTransparent_b
                do { // .line_loop: regular solid colour mode
                    Mem.wb(a0, d4);                    // move.b d4,(a0)
                    a0 += (short) d6;                  // add.w d6,a0
                    d0 = setw(d0, d0 - d2);            // sub.w d2,d0
                    if ((short) d0 <= 0) {             // bgt.s .no_extra
                        d0 = setw(d0, d0 + d3);        // add.w d3,d0
                        a0 -= 1;                       // subq #1,a0
                    }
                    // .no_extra:
                    d7 = setw(d7, d7 - 1);             // dbra d7,.line_loop
                } while ((short) d7 != -1);
                return;                                // rts
            }
            do { // .line_loop_transparent:
                d4 = setb(d4, Mem.ub(a0));             // move.b (a0),d4 ; read chunky buffer
                Mem.wb(a0, Mem.ub(a4 + (short) d4));   // move.b (a4,d4.w),(a0) ; Replace and write back
                a0 += (short) d6;                      // add.w d6,a0
                d0 = setw(d0, d0 - d2);                // sub.w d2,d0
                if ((short) d0 <= 0) {                 // bgt.s .no_extra_transparent
                    d0 = setw(d0, d0 + d3);            // add.w d3,d0
                    a0 -= 1;                           // subq #1,a0
                }
                // .no_extra_transparent:
                d7 = setw(d7, d7 - 1);                 // dbra d7,.line_loop_transparent
            } while ((short) d7 != -1);
            return;                                    // rts
        }
        // down_left_more:
        d6 = Hires.SCREEN_WIDTH;                       // move.w #SCREEN_WIDTH,d6
        d0 = setw(d0, d2);                             // move.w d2,d0
        d7 = setw(0, d2);                              // move.w d2,d7
        a0 += 1;                                       // addq #1,a0
        if (Mem.b(Draw_MapTransparent_b) == 0) {       // tst.b Draw_MapTransparent_b ; bne.s .line_loop_transparent
            do { // .line_loop: regular solid colour mode
                a0 -= 1; Mem.wb(a0, d4);               // move.b d4,-(a0)
                d0 = setw(d0, d0 - d3);                // sub.w d3,d0
                if ((short) d0 <= 0) {                 // bgt.s .no_extra
                    d0 = setw(d0, d0 + d2);            // add.w d2,d0
                    a0 += (short) d6;                  // add.w d6,a0
                }
                // .no_extra:
                d7 = setw(d7, d7 - 1);                 // dbra d7,.line_loop
            } while ((short) d7 != -1);
            return;                                    // rts
        }
        do { // .line_loop_transparent:
            a0 -= 1; d4 = setb(d4, Mem.ub(a0));        // move.b -(a0),d4 ; read chunky buffer
            Mem.wb(a0, Mem.ub(a4 + (short) d4));       // move.b (a4,d4.w),(a0) ; Replace and write back
            d0 = setw(d0, d0 - d3);                    // sub.w d3,d0
            if ((short) d0 <= 0) {                     // bgt.s .no_extra_transparent
                d0 = setw(d0, d0 + d2);                // add.w d2,d0
                a0 += (short) d6;                      // add.w d6,a0
            }
            // .no_extra_transparent:
            d7 = setw(d7, d7 - 1);                     // dbra d7,.line_loop_transparent
        } while ((short) d7 != -1);
        // rts
    }

    /**
     * draw_MapLineDoubleWidth — variante double largeur (3 écritures par pixel).
     * Non appelée dans la version actuelle (le branchement est commenté dans
     * draw_MapLine) ; conservée comme l'original ("retained for posterity").
     */
    public static void draw_MapLineDoubleWidth(int d0, int d1, int d2, int d3, int d4) {
        int a0 = Mem.l(Vid_FastBufferPtr_l);           // move.l Vid_FastBufferPtr_l,a0
        if ((short) d3 <= (short) d1) {                // cmp.w d1,d3 ; bgt.s .okdown
            if ((short) d3 == (short) d1) {            // bne.s .aline
                if ((short) d2 == (short) d0) {        // cmp.w d0,d2 ; beq no_line
                    return;
                }
            }
            // .aline:
            int t = d0; d0 = d2; d2 = t;               // exg d0,d2
            t = d1; d1 = d3; d3 = t;                   // exg d1,d3
        }

        // .okdown:
        int d5 = setw(0, d1);                          // move.w d1,d5
        d5 = muls(d5, Hires.SCREEN_WIDTH);             // muls #SCREEN_WIDTH,d5
        a0 += d5;                                      // add.l d5,a0
        a0 += (short) d0;                              // lea (a0,d0.w),a0
        d3 = setw(d3, d3 - d1);                        // sub.w d1,d3
        d2 = setw(d2, d2 - d0);                        // sub.w d0,d2
        int d6, d7;
        if ((short) d2 >= 0) {                         // bge down_right_dw
            // downrightFAT: / down_right_dw:
            if ((short) d3 > (short) d2) {             // cmp.w d2,d3 ; bgt.s down_more_right_dw
                // down_more_right_dw:
                d6 = Hires.SCREEN_WIDTH;               // move.w #SCREEN_WIDTH,d6
                d0 = setw(d0, d3);                     // move.w d3,d0
                d7 = setw(0, d3);                      // move.w d3,d7
                do { // .line_loop:
                    Mem.wb(a0 + Hires.SCREEN_WIDTH, d4); // move.b d4,SCREEN_WIDTH(a0)
                    Mem.wb(a0 + 1, d4);                // move.b d4,1(a0)
                    Mem.wb(a0, d4);                    // move.b d4,(a0)
                    a0 += (short) d6;                  // add.w d6,a0
                    d0 = setw(d0, d0 - d2);            // sub.w d2,d0
                    if ((short) d0 <= 0) {             // bgt.s .no_extra
                        d0 = setw(d0, d0 + d3);        // add.w d3,d0
                        a0 += 1;                       // addq #1,a0
                    }
                    // .no_extra:
                    d7 = setw(d7, d7 - 1);             // dbra d7,.line_loop
                } while ((short) d7 != -1);
                return;                                // rts
            }
            // down_right_more_dw:
            d6 = Hires.SCREEN_WIDTH;                   // move.w #SCREEN_WIDTH,d6
            d0 = setw(d0, d2);                         // move.w d2,d0
            d7 = setw(0, d2);                          // move.w d2,d7
            do { // .line_loop:
                Mem.wb(a0 + Hires.SCREEN_WIDTH, d4);   // move.b d4,SCREEN_WIDTH(a0)
                Mem.wb(a0, d4); a0 += 1;               // move.b d4,(a0)+
                Mem.wb(a0, d4);                        // move.b d4,(a0)
                d0 = setw(d0, d0 - d3);                // sub.w d3,d0
                if ((short) d0 <= 0) {                 // bgt.s .no_extra
                    d0 = setw(d0, d0 + d2);            // add.w d2,d0
                    a0 += (short) d6;                  // add.w d6,a0
                }
                // .no_extra:
                d7 = setw(d7, d7 - 1);                 // dbra d7,.line_loop
            } while ((short) d7 != -1);
            return;                                    // rts
        }

        // down_left_dw:
        d2 = negw(d2);                                 // neg.w d2
        if ((short) d3 > (short) d2) {                 // cmp.w d2,d3 ; bgt.s down_more_left_dw
            // down_more_left_dw:
            d6 = Hires.SCREEN_WIDTH;                   // move.w #SCREEN_WIDTH,d6
            d0 = setw(d0, d3);                         // move.w d3,d0
            d7 = setw(0, d3);                          // move.w d3,d7
            do { // .line_loop:
                Mem.wb(a0 + Hires.SCREEN_WIDTH, d4);   // move.b d4,SCREEN_WIDTH(a0)
                Mem.wb(a0 + 1, d4);                    // move.b d4,1(a0)
                Mem.wb(a0, d4);                        // move.b d4,(a0)
                a0 += (short) d6;                      // add.w d6,a0
                d0 = setw(d0, d0 - d2);                // sub.w d2,d0
                if ((short) d0 <= 0) {                 // bgt.s .no_extra
                    d0 = setw(d0, d0 + d3);            // add.w d3,d0
                    a0 -= 1;                           // subq #1,a0
                }
                // .no_extra:
                d7 = setw(d7, d7 - 1);                 // dbra d7,.line_loop
            } while ((short) d7 != -1);
            return;                                    // rts
        }
        // down_left_more_dw:
        d6 = Hires.SCREEN_WIDTH;                       // move.w #SCREEN_WIDTH,d6
        d0 = setw(d0, d2);                             // move.w d2,d0
        d7 = setw(0, d2);                              // move.w d2,d7
        a0 += 1;                                       // addq #1,a0
        do { // .line_loop:
            Mem.wb(a0 + Hires.SCREEN_WIDTH - 1, d4);   // move.b d4,SCREEN_WIDTH-1(a0)
            Mem.wb(a0, d4);                            // move.b d4,(a0)
            a0 -= 1; Mem.wb(a0, d4);                   // move.b d4,-(a0)
            d0 = setw(d0, d0 - d3);                    // sub.w d3,d0
            if ((short) d0 <= 0) {                     // bgt.s .no_extra
                d0 = setw(d0, d0 + d2);                // add.w d2,d0
                a0 += (short) d6;                      // add.w d6,a0
            }
            // .no_extra:
            d7 = setw(d7, d7 - 1);                     // dbra d7,.line_loop
        } while ((short) d7 != -1);
        // rts
    }
}
