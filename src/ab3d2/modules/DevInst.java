package ab3d2.modules;

import ab3d2.Mem;

import static ab3d2.modules.DevMacros.DEV_GRAPH_BUFFER_SIZE;

/**
 * Traduction de ab3d2_source/modules/dev_inst.s — PARTIE 1 : données.
 *
 * "DEVMODE INSTRUMENTATION"
 *
 * Le port correspond au build DEV (cf. Makefile : flavor dev → -DDEV=1
 * -DZONE_DEBUG=1) : les compteurs/timestamps existent. Les routines de code
 * (Dev_Init, métriques, graphe de temps) seront ajoutées dans cette classe
 * lors de la traduction complète du fichier.
 */
public final class DevInst {

    // align 4 ; section .data
    private static final int _a0 = Mem.align(4);
    public static final int Dev_RegStatePtr_l = Mem.dcL(0);
    public static final int Dev_DebugFlags_l = Mem.dcL(0);

    // IFD DEV : section .bss
    private static final int _a1 = Mem.align(4);

    /** array of times */
    public static final int dev_GraphBuffer_vb = Mem.alloc(DEV_GRAPH_BUFFER_SIZE * 2);

    // EClockVal stamps
    public static final int dev_ECVDrawDone_q = Mem.alloc(4 * 2);      // timestamp at the end of drawing
    public static final int dev_ECVChunkyDone_q = Mem.alloc(4 * 2);    // timestamp at the end of chunky to planar

    public static final int dev_ECVInterruptBegin_q = Mem.alloc(4 * 2); // start of the (in game) interrupt
    public static final int dev_ECVInterruptDone_q = Mem.alloc(4 * 2);  // end of the (in game) interrupt

    public static final int dev_InterruptCount_l = Mem.alloc(4);       // number of (in game) interrupts
    public static final int dev_InterruptClocks_l = Mem.alloc(4);      // total clocks of the interrupts

    // Counters
    public static final int dev_Counters_vw = Mem.allocTop();
    public static final int dev_VisibleSimpleWalls_w = Mem.alloc(2);   // simple walls drawn this frame
    public static final int dev_VisibleShadedWalls_w = Mem.alloc(2);   // shaded walls drawn this frame

    public static final int dev_VisibleModelCount_w = Mem.alloc(2);    // visible polygon models this frame
    public static final int dev_VisibleGlareCount_w = Mem.alloc(2);    // visible glare bitmaps this frame

    public static final int dev_VisibleLightMapCount_w = Mem.alloc(2); // visible lightsource bitmaps this frame
    public static final int dev_VisibleAdditiveCount_w = Mem.alloc(2); // visible additive bitmaps this frame

    public static final int dev_VisibleBitmapCount_w = Mem.alloc(2);   // visible bitmaps this frame
    public static final int dev_Reserved0_w = Mem.alloc(2);

    public static final int dev_TotalCounters_vw = Mem.allocTop();
    public static final int dev_VisibleWalls_w = Mem.alloc(2);         // Total visible walls this frame
    public static final int dev_VisibleFlats_w = Mem.alloc(2);         // Total visible flats this frame

    public static final int dev_VisibleObjectCount_w = Mem.alloc(2);   // Total visible objects this frame
    public static final int dev_DrawObjectCallCount_w = Mem.alloc(2);  // Number of calls to Draw_Object

    public static final int dev_DrawTimeMsAvg_w = Mem.alloc(2);        // two frame average of draw time
    public static final int dev_FPSIntAvg_w = Mem.alloc(2);

    public static final int dev_FPSFracAvg_w = Mem.alloc(2);
    public static final int dev_FPSLimit_w = Mem.alloc(2);
    public static final int dev_Reserved1_w = Mem.alloc(2);

    public static final int dev_Reserved2_w = Mem.alloc(2);
    public static final int dev_Reserved3_w = Mem.alloc(2);
    public static final int dev_Reserved4_w = Mem.alloc(2);
    public static final int dev_Reserved5_w = Mem.alloc(2);
    // Not cleared per frame
    public static final int dev_FrameIndex_w = Mem.alloc(2);           // frame number % DEV_GRAPH_BUFFER_SIZE

    /** Character buffer for printing */
    public static final int dev_CharBuffer_vb = Mem.alloc(64);

    // ---- Données embarquées dans le code (Dev_PrintF / Dev_PrintStats) ----
    public static final int dev_Length;             // .dev_Length: dc.w 0
    public static final int dev_fs_stats_tpl_vb;
    public static final int dev_ss_stats_wall_simple_vb;
    public static final int dev_ss_stats_wall_shaded_vb;
    public static final int dev_ss_stats_obj_poly_vb;
    public static final int dev_ss_stats_obj_glare_vb;
    public static final int dev_ss_stats_obj_lightmap_vb;
    public static final int dev_ss_stats_obj_additive_vb;
    public static final int dev_ss_stats_obj_bitmap_vb;
    public static final int dev_ss_stats_order_zones_vb;
    public static final int dev_ss_stats_zone_vb;
    public static final int dev_ss_stats_dir_vb;
    public static final int dev_ss_stats_pos_vb;
    public static final int dev_ss_stats_join_vis_vb;
    public static final int dev_ss_stats_edge_clips_vb;
    public static final int dev_bool_off_vb;
    public static final int dev_bool_on_vb;
    public static final int dev_ss_door_mask_vb;
    public static final int dev_strptr_bool_off;
    public static final int dev_strptr_bool_on;

    static {
        dev_Length = Mem.dcW(0);
        dev_fs_stats_tpl_vb = cstr("W:%2d F:%2d O:%2d/%2d D:%2dms %2d.%d %d");
        dev_ss_stats_wall_simple_vb = cstr("WS:%3d");
        dev_ss_stats_wall_shaded_vb = cstr("WG:%3d");
        dev_ss_stats_obj_poly_vb = cstr("OP:%3d");
        dev_ss_stats_obj_glare_vb = cstr("OG:%3d");
        dev_ss_stats_obj_lightmap_vb = cstr("OL:%3d");
        dev_ss_stats_obj_additive_vb = cstr("OA:%3d");
        dev_ss_stats_obj_bitmap_vb = cstr("OB:%3d");
        dev_ss_stats_order_zones_vb = cstr("OZ:%3d");
        dev_ss_stats_zone_vb = cstr("ZI:%3d");
        dev_ss_stats_dir_vb = cstr("C:%6d S:%6d A:%5d  ");
        dev_ss_stats_pos_vb = cstr("X:%5d Z:%5d");
        dev_ss_stats_join_vis_vb = cstr("JE:%3d/%3d [%04X]");
        dev_ss_stats_edge_clips_vb = cstr("L:%4d, R:%4d");
        dev_bool_off_vb = cstr("off");
        dev_bool_on_vb = cstr("on");
        dev_ss_door_mask_vb = cstr("DM: %04X");
        Mem.align(4);
        dev_strptr_bool_off = Mem.dcL(dev_bool_off_vb);
        dev_strptr_bool_on = Mem.dcL(dev_bool_on_vb);
        Mem.align(4);
    }

    private static int cstr(String s) {
        int a = Mem.dcStr(s);
        Mem.dcB(0);
        return a;
    }

    private DevInst() {
    }

    /** Dev_Init: rts — "Initialise developer options (timer stuff has moved to system.s)" */
    public static void Dev_Init() {
    }

    /** Dev_DataReset — Reset Metrics Data */
    public static void Dev_DataReset() {
        if (!DevMacros.DEV_TEST(Dev_DebugFlags_l, DevMacros.DEV_SKIP_OVERLAY)) { // DEV_CHECK_SET SKIP_OVERLAY,.done
            int a0 = dev_GraphBuffer_vb;               // lea dev_GraphBuffer_vb,a0
            int d0 = (DEV_GRAPH_BUFFER_SIZE / 16) - 1; // move.l #(DEV_GRAPH_BUFFER_SIZE/16)-1,d0
            do { // .loop:
                ab3d2.Mem.wl(a0, 0); a0 += 4;          // clr.l (a0)+
                ab3d2.Mem.wl(a0, 0); a0 += 4;          // clr.l (a0)+
                ab3d2.Mem.wl(a0, 0); a0 += 4;          // clr.l (a0)+
                ab3d2.Mem.wl(a0, 0); a0 += 4;          // clr.l (a0)+
                d0 -= 1;                               // dbra d0,.loop
            } while ((short) d0 != -1);
        }
        // .done:
        ab3d2.Mem.ww(dev_DrawTimeMsAvg_w, 0);          // clr.w dev_DrawTimeMsAvg_w
        // rts
    }

    /** Dev_ClearFastBuffer — clears the chunky draw buffer to mid grey */
    public static void Dev_ClearFastBuffer() {
        int a0 = ab3d2.Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l); // move.l Vid_FastBufferPtr_l,a0
        int d0 = (ab3d2.Hires.VID_FAST_BUFFER_SIZE / 16) - 1; // move.l #(VID_FAST_BUFFER_SIZE/16)-1,d0
        int d1 = 0x0A0A0A0A;                           // move.l #$0A0A0A0A,d1
        do { // .loop:
            ab3d2.Mem.wl(a0, d1); a0 += 4;             // move.l d1,(a0)+
            ab3d2.Mem.wl(a0, d1); a0 += 4;             // move.l d1,(a0)+
            ab3d2.Mem.wl(a0, d1); a0 += 4;             // move.l d1,(a0)+
            ab3d2.Mem.wl(a0, d1); a0 += 4;             // move.l d1,(a0)+
            d0 -= 1;                                   // dbra d0,.loop
        } while ((short) d0 != -1);
        // rts
    }

    /** Dev_MarkFrameBegin — mark frame beginning for instrumentation */
    public static void Dev_MarkFrameBegin() {
        int d0 = ab3d2.Mem.uw(dev_FrameIndex_w);       // move.w dev_FrameIndex_w,d0
        d0 += 1;                                       // addq.w #1,d0
        d0 &= DevMacros.DEV_GRAPH_BUFFER_MASK;         // and.w #DEV_GRAPH_BUFFER_MASK,d0
        ab3d2.Mem.ww(dev_FrameIndex_w, d0);            // move.w d0,dev_FrameIndex_w

        int a0 = dev_Counters_vw;                      // lea dev_Counters_vw,a0
        for (int i = 0; i < 9; i++) {                  // clr.l (a0)+ ×9
            ab3d2.Mem.wl(a0, 0); a0 += 4;
        }

        // Check if the current skip flags require the fast buffer to be cleared
        if (!DevMacros.DEV_TEST(Dev_DebugFlags_l, DevMacros.DEV_SKIP_FASTBUFFER_CLEAR)) { // DEV_CHECK_SET SKIP_FASTBUFFER_CLEAR,.no_clear
            int dd = ab3d2.Mem.l(Dev_DebugFlags_l);    // move.l Dev_DebugFlags_l,d0
            dd &= DevMacros.DEV_CLEAR_FASTBUFFER_MASK; // and.l #DEV_CLEAR_FASTBUFFER_MASK,d0
            if (dd != 0) {                             // beq.s .no_clear
                Dev_ClearFastBuffer();                 // bsr Dev_ClearFastBuffer
            }
        }
        // .no_clear: rts
    }

    /** Dev_MarkDrawDone — mark end of drawing for instrumentation */
    public static void Dev_MarkDrawDone() {
        // sum up the different rendered object types this frame
        int a0 = dev_Counters_vw;                      // lea dev_Counters_vw,a0

        // Walls
        int d0 = ab3d2.Mem.uw(a0); a0 += 2;            // move.w (a0)+,d0 ; simple walls
        d0 = ab3d2.M68k.setw(d0, d0 + ab3d2.Mem.uw(a0)); a0 += 2; // add.w (a0)+,d0 ; shaded walls
        ab3d2.Mem.ww(dev_VisibleWalls_w, d0);          // move.w d0,dev_VisibleWalls_w

        // Objects
        d0 = ab3d2.Mem.uw(a0); a0 += 2;                // move.w (a0)+,d0 ; models
        d0 = ab3d2.M68k.setw(d0, d0 + ab3d2.Mem.uw(a0)); a0 += 2; // add.w (a0)+,d0 ; glare
        d0 = ab3d2.M68k.setw(d0, d0 + ab3d2.Mem.uw(a0)); a0 += 2; // add.w (a0)+,d0 ; lightmapped
        d0 = ab3d2.M68k.setw(d0, d0 + ab3d2.Mem.uw(a0)); a0 += 2; // add.w (a0)+,d0 ; additive
        d0 = ab3d2.M68k.setw(d0, d0 + ab3d2.Mem.uw(a0)); a0 += 2; // add.w (a0)+,d0 ; bitmaps
        ab3d2.Mem.ww(dev_VisibleObjectCount_w, d0);    // move.w d0,dev_VisibleObjectCount_w
        ab3d2.c.SystemC.Sys_MarkTime(dev_ECVDrawDone_q); // lea dev_ECVDrawDone_q,a0 ; CALLC Sys_MarkTime
        // rts
    }

    /** Dev_MarkChunkyDone — mark end of c2p/transfer for instrumentation */
    public static void Dev_MarkChunkyDone() {
        ab3d2.c.SystemC.Sys_MarkTime(dev_ECVChunkyDone_q); // lea dev_ECVChunkyDone_q,a0 ; CALLC Sys_MarkTime
        // rts
    }

    /**
     * Dev_PrintF — basic printf() based on exec/RawDoFmt(). Keep the data size
     * shorter than dev_CharBuffer_vb or expect overflow.
     * d0 = coordinate pair (x16:y16), a0 = format template, a1 = data stream.
     */
    public static void Dev_PrintF(int d0, int a0, int a1) {
        int d2 = d0;                                   // move.l d0,d2 ; coordinate pair
        ab3d2.Mem.ww(dev_Length, 0);                   // move.w #0,.dev_Length
        // lea .dev_PutChar(pc),a2 ; lea dev_CharBuffer_vb,a3 ; CALLEXEC RawDoFmt
        final int[] a3 = {dev_CharBuffer_vb};
        ab3d2.host.ExecLib.RawDoFmt(a0, a1, ch -> {
            // .dev_PutChar: move.b d0,(a3)+ ; add.w #1,.dev_Length
            ab3d2.Mem.wb(a3[0], ch); a3[0] += 1;
            ab3d2.Mem.ww(dev_Length, ab3d2.Mem.uw(dev_Length) + 1);
        });

        int ra1 = ab3d2.Mem.l(ab3d2.bss.VidBss.Vid_MainScreen_l); // move.l Vid_MainScreen_l,a1
        ra1 += ab3d2.host.IntuitionLib.sc_RastPort;    // lea sc_RastPort(a1),a1
        int dy = d2 & 0xFFFF;                          // clr.l d1 ; move.w d2,d1 ; y coordinate
        int dx = (ab3d2.M68k.swap(d2)) & 0xFFFF;       // clr.l d0 ; swap d2 ; move.w d2,d0 ; x coordinate
        ab3d2.host.GraphicsLib.Move(ra1, dx, dy);      // CALLGRAF Move

        int len = ab3d2.Mem.uw(dev_Length);            // move.w .dev_Length(pc),d0
        len -= 1;                                      // subq #1,d0
        ab3d2.host.GraphicsLib.Text(ra1, dev_CharBuffer_vb, len); // lea dev_CharBuffer_vb,a0 ; jsr _LVOText(a6)
        // rts
    }

    /** Dev_PrintStats — display developer instrumentation. */
    public static void Dev_PrintStats() {
        if (DevMacros.DEV_TEST(Dev_DebugFlags_l, DevMacros.DEV_SKIP_DUMP_BG_DISABLE)) { // DEV_CHECK_SET SKIP_DUMP_BG_DISABLE,...
            // dev_DumpLevelBackdropErrata:
            DevMacros.DEV_DISABLE(Dev_DebugFlags_l, DevMacros.DEV_SKIP_DUMP_BG_DISABLE); // DEV_DISABLE SKIP_DUMP_BG_DISABLE
            Level.Lvl_DumpBackdropDisableData();       // jsr Lvl_DumpBackdropDisableData
            // (chute dans Dev_PrintStats comme l'original)
        }
        if (DevMacros.DEV_TEST(Dev_DebugFlags_l, DevMacros.DEV_SKIP_OVERLAY)) { // DEV_CHECK_SET SKIP_OVERLAY,dev_SkipStats
            return;                                    // dev_SkipStats: rts
        }

        // Use the system recorded FPS average
        ab3d2.Mem.wl(dev_FPSIntAvg_w, ab3d2.Mem.l(ab3d2.bss.SystemBss.Sys_FPSIntAvg_w)); // move.l Sys_FPSIntAvg_w,dev_FPSIntAvg_w
        ab3d2.Mem.ww(dev_FPSLimit_w, ab3d2.Mem.uw(ab3d2.bss.SystemBss.Sys_FPSLimit_w));  // move.w Sys_FPSLimit_w,dev_FPSLimit_w

        if (ab3d2.Mem.b(ab3d2.bss.VidBss.Vid_FullScreen_b) != 0) { // tst.b Vid_FullScreen_b ; bne .fullscreen_stats
            // .fullscreen_stats:
            Dev_PrintF(((ab3d2.Hires.SCREEN_WIDTH - 240) << 16) | (ab3d2.Hires.SCREEN_HEIGHT - 24),
                    dev_fs_stats_tpl_vb, dev_TotalCounters_vw); // bsr Dev_PrintF
            return;                                    // rts
        }

        // smallscreen
        Dev_PrintF(ab3d2.Hires.SCREEN_HEIGHT - 24, dev_fs_stats_tpl_vb, dev_TotalCounters_vw);

        // Door mask
        Dev_PrintF(24, dev_ss_door_mask_vb, ab3d2.bss.ZoneBss.Zone_RenderDoorState_w);

        // Player1 Zone ID
        Dev_PrintF(136 + 48, dev_ss_stats_zone_vb, ab3d2.bss.PlayerBss.Plr1_Zone_w);

        // Edges Vis
        Dev_PrintF(136 + 64, dev_ss_stats_join_vis_vb, ab3d2.bss.ZoneBss.Zone_VisJoins_w); // close enough

        // Player 1 Directions
        Dev_PrintF(136 + 80, dev_ss_stats_dir_vb, ab3d2.bss.PlayerBss.Plr1_CosVal_w);
        // rts
    }

    /**
     * dev_ECVDiffToMs — difference between a pair of ECV timestamps in ms.
     * a0 = start timestamp, a1 = end timestamp ; renvoie d0 (16 bits).
     */
    private static int dev_ECVDiffToMs(int a0, int a1) {
        int d0 = ab3d2.Mem.l(a1 + 4);                  // move.l 4(a1),d0
        d0 -= ab3d2.Mem.l(a0 + 4);                     // sub.l 4(a0),d0
        d0 = d0 * ab3d2.Mem.l(ab3d2.bss.SystemBss.Sys_ECVToMsFactor_l); // mulu.l Sys_ECVToMsFactor_l,d0 (32 bits bas)
        d0 = d0 & 0xFFFF0000;                          // clr.w d0
        d0 = ab3d2.M68k.swap(d0);                      // swap d0
        return d0;                                     // rts
    }

    /** Dev_DrawGraph — calculate the times and store in the graph data buffer, then draw it. */
    public static void Dev_DrawGraph() {
        if (DevMacros.DEV_TEST(Dev_DebugFlags_l, DevMacros.DEV_SKIP_OVERLAY)) { // DEV_CHECK_SET SKIP_OVERLAY,dev_SkipGraph
            return;                                    // dev_SkipGraph: rts
        }
        // movem.l d0/d1/d2/a0/a1/a2,-(sp)
        int d0 = dev_ECVDiffToMs(ab3d2.bss.SystemBss.Sys_FrameTimeECV_q, dev_ECVDrawDone_q); // bsr.s dev_ECVDiffToMs
        int a0 = dev_GraphBuffer_vb;                   // lea dev_GraphBuffer_vb,a0 ; ms value into the graph buffer
        int d1 = ab3d2.Mem.uw(dev_FrameIndex_w);       // move.w dev_FrameIndex_w,d1
        d0 = ab3d2.M68k.setw(d0, d0 + ab3d2.Mem.uw(dev_DrawTimeMsAvg_w)); // add.w dev_DrawTimeMsAvg_w,d0
        d0 = ab3d2.M68k.setw(d0, (d0 & 0xFFFF) >>> 1); // lsr.w #1,d0
        ab3d2.Mem.ww(dev_DrawTimeMsAvg_w, d0);         // move.w d0,dev_DrawTimeMsAvg_w
        d0 = ab3d2.M68k.setb(d0, (d0 & 0xFF) >>> 1);   // lsr.b #1,d0
        ab3d2.Mem.wb(a0 + ((short) d1) * 2, d0);       // move.b d0,(a0,d1.w*2)
        ab3d2.Mem.wb(a0 + ((short) d1) * 2 + 1, ab3d2.Mem.ub(dev_VisibleObjectCount_w + 1)); // move.b dev_VisibleObjectCount_w+1,1(a0,d1.w*2)

        // Now draw it...
        a0 = ab3d2.Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l); // move.l Vid_FastBufferPtr_l,a0

        // In fullscreen, we need to make a small adjustment (IFNE FS_HEIGHT_C2P_DIFF, actif)
        int d2 = ab3d2.Hires.FS_HEIGHT_C2P_DIFF;       // moveq #FS_HEIGHT_C2P_DIFF,d2
        d2 = ab3d2.M68k.setb(d2, d2 & ab3d2.Mem.ub(ab3d2.bss.VidBss.Vid_FullScreen_b)); // and.b Vid_FullScreen_b,d2

        d0 = ab3d2.Mem.uw(ab3d2.HireswallData.Vid_BottomY_w); // move.w Vid_BottomY_w,d0
        d0 = ab3d2.M68k.setw(d0, d0 - d2);             // sub.w d2,d0

        d0 = ab3d2.M68k.setw(d0, d0 - ab3d2.Mem.w(ab3d2.bss.VidBss.Vid_LetterBoxMarginHeight_w)); // sub.w Vid_LetterBoxMarginHeight_w,d0
        d0 = ab3d2.M68k.mulu(d0, ab3d2.Hires.SCREEN_WIDTH); // mulu.w #SCREEN_WIDTH,d0
        a0 += d0;                                      // add.l d0,a0 ; lower left of render area
        int a1 = dev_GraphBuffer_vb;                   // lea dev_GraphBuffer_vb,a1

        // draw buffer position should be one ahead of the write position.
        d1 = ab3d2.M68k.setw(d1, d1 + 1);              // addq.w #1,d1
        d1 = ab3d2.M68k.setw(d1, d1 & DevMacros.DEV_GRAPH_BUFFER_MASK); // and.w #DEV_GRAPH_BUFFER_MASK,d1

        // Draw loop
        d0 = DevMacros.DEV_GRAPH_BUFFER_SIZE - 1;      // move.l #DEV_GRAPH_BUFFER_SIZE-1,d0
        do { // .loop:
            // plot draw time average
            int dp = ab3d2.Mem.ub(a1 + ((short) d1) * 2); // clr.l d2 ; move.b (a1,d1.w*2),d2
            dp = (dp & ~0xFF) | ((dp & 0xFF) >>> 1);   // lsr.b #1,d2 ; restrict the maximum deflection
            dp = ab3d2.M68k.muls(dp, -ab3d2.Hires.SCREEN_WIDTH); // muls.w #-SCREEN_WIDTH,d2
            ab3d2.Mem.wb(a0 + dp, DevMacros.DEV_GRAPH_DRAW_TIME_COLOUR); // move.b #DEV_GRAPH_DRAW_TIME_COLOUR,(a0,d2)

            // plot object count
            dp = ab3d2.Mem.ub(a1 + ((short) d1) * 2 + 1); // clr.l d2 ; move.b 1(a1,d1.w*2),d2
            dp = ab3d2.M68k.muls(dp, -ab3d2.Hires.SCREEN_WIDTH); // muls.w #-SCREEN_WIDTH,d2
            ab3d2.Mem.wb(a0 + dp, 31);                 // move.b #31,(a0,d2)

            d1 += 1;                                   // addq.l #1,d1
            d1 &= DevMacros.DEV_GRAPH_BUFFER_MASK;     // and.l #DEV_GRAPH_BUFFER_MASK,d1
            a0 += 1;                                   // addq.l #1,a0
            d0 = ab3d2.M68k.setw(d0, d0 - 1);          // dbra d0,.loop
        } while ((short) d0 != -1);
        // movem.l (sp)+,... ; rts
    }

    /**
     * Dev_Dump — dump raw memory to a file.
     * a0 = filename, a1 = memory location, d0 = bytes.
     */
    public static void Dev_Dump(int a0, int a1, int d0) {
        // movem.l d1-d4/a2/a6,-(sp)
        int a2 = a1;                                   // move.l a1,a2 ; buffer
        int d3 = d0;                                   // move.l d0,d3 ; length
        int d1 = a0;                                   // move.l a0,d1 ; filename
        int d2 = ab3d2.host.DosLib.MODE_READWRITE;     // move.l #MODE_READWRITE,d2
        int h = ab3d2.host.DosLib.Open(d1, d2);        // CALLDOS Open

        d1 = h;                                        // move.l d0,d1 ; handle
        if (d1 == 0) {                                 // beq.s .open_fail
            return;                                    // .open_fail: rts
        }
        int d4 = d1;                                   // move.l d1,d4 ; back up as read/write clobber
        d2 = a2;                                       // move.l a2,d2 ; buffer location, size still in d3
        ab3d2.host.DosLib.Write(d1, d2, d3);           // CALLDOS Write

        d1 = d4;                                       // move.l d4,d1 ; handle
        ab3d2.host.DosLib.Close(d1);                   // CALLDOS Close
        // movem.l (sp)+,... ; rts
    }
}
