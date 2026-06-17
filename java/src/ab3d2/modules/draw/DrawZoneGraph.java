package ab3d2.modules.draw;

import ab3d2.Hires;
import ab3d2.HiresData;
import ab3d2.Hireswall;
import ab3d2.HireswallData;
import ab3d2.Mem;
import ab3d2.Newanims;
import ab3d2.Objdrawhires;
import ab3d2.c.ZoneDebug;
import ab3d2.c.ZoneEdgePvs;
import ab3d2.modules.DevInst;
import ab3d2.modules.DevMacros;

import static ab3d2.Defs.PVST_ClipID_w;
import static ab3d2.Defs.PVST_SizeOf_l;
import static ab3d2.Defs.ZoneT_Floor_l;
import static ab3d2.Defs.ZoneT_ID_w;
import static ab3d2.Defs.ZoneT_Roof_l;
import static ab3d2.Defs.ZoneT_Unused_w;
import static ab3d2.Defs.ZoneT_UpperFloor_l;
import static ab3d2.Defs.ZoneT_UpperRoof_l;
import static ab3d2.Defs.ZoneT_Water_l;
import static ab3d2.M68k.lslw;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;
import static ab3d2.bss.DrawBss.Draw_AfterWaterBottom_l;
import static ab3d2.bss.DrawBss.Draw_AfterWaterTop_l;
import static ab3d2.bss.DrawBss.Draw_BeforeWaterBottom_l;
import static ab3d2.bss.DrawBss.Draw_BeforeWaterTop_l;
import static ab3d2.bss.DrawBss.Draw_BottomOfRoom_l;
import static ab3d2.bss.DrawBss.Draw_CurrentZone_w;
import static ab3d2.bss.DrawBss.Draw_DoUpper_b;
import static ab3d2.bss.DrawBss.Draw_ForceZoneSkip_b;
import static ab3d2.bss.DrawBss.Draw_InRootZone_b;
import static ab3d2.bss.DrawBss.Draw_TopOfRoom_l;
import static ab3d2.bss.DrawBss.Draw_ZoneClipL_w;
import static ab3d2.bss.DrawBss.Draw_ZoneClipR_w;
import static ab3d2.bss.DrawBss.draw_BackupRoomPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ClipsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_GraphicsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ListOfGraphRoomsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneGraphAddsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.TablesBss.CurrentPointBrights_vl;
import static ab3d2.bss.TablesBss.Lvl_BigMap_vl;
import static ab3d2.bss.TablesBss.Lvl_CompactMap_vl;
import static ab3d2.bss.ZoneBss.Zone_BrightTable_vl;
import static ab3d2.bss.ZoneBss.Zone_EndOfListPtr_l;

/**
 * Traduction littÃ©rale de ab3d2_source/modules/draw/draw_zone_graph.s
 *
 * Walk de la table d'ordre des zones (Ã  rebours), application des clips
 * gauche/droite par entrÃ©e PVS, puis rendu zone par zone via la boucle de
 * commandes taguÃ©es de draw_RenderCurrentZone (0=mur, 1/2=sol/plafond,
 * 4=objets, 7=eau, 12=backdrop, <0=fin).
 *
 * Build DEV : les DEV_ZDBG tracent si DEV_ZONE_TRACE est actif ;
 * DEV_CHECK_SET SKIP_EDGE_PVS court-circuite le clipping d'arÃªtes.
 */
public final class DrawZoneGraph {

    // draw_Root_Zone_w: dc.w 0
    public static final int draw_Root_Zone_w = Mem.dcW(0);

    private DrawZoneGraph() {
    }

    private static boolean zdbgEnabled() {
        return DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_ZONE_TRACE);
    }

    private static boolean devSkipEdgePvs() {
        return DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_EDGE_PVS);
    }

    /** Draw_Zone_Graph */
    public static void Draw_Zone_Graph() {
        if (zdbgEnabled()) {                           // DEV_ZDBG ZDbg_Init
            ZoneDebug.ZDbg_Init();
        }

        int a0 = Mem.l(Lvl_ListOfGraphRoomsPtr_l);     // move.l Lvl_ListOfGraphRoomsPtr_l,a0
        Mem.ww(draw_Root_Zone_w, Mem.uw(a0));          // move.w (a0),draw_Root_Zone_w

        a0 = Mem.l(Zone_EndOfListPtr_l);               // move.l Zone_EndOfListPtr_l,a0

        int d0, d1, d2, d7;
        int a1, a2;

        // 0xABADCAFE - This is where we process the visible zones and their content
        subroomloop:
        while (true) { // .subroomloop:
            a0 -= 2;                                   // move.w -(a0),d7
            d7 = Mem.w(a0);
            if (d7 < 0) {                              // blt .done_all_zones
                break;
            }

            Mem.ww(Draw_CurrentZone_w, d7);            // move.w d7,Draw_CurrentZone_w

            Mem.ww(Draw_ZoneClipL_w, 0);               // clr.w Draw_ZoneClipL_w
            Mem.ww(Draw_ZoneClipR_w, Mem.uw(HiresData.Vid_RightX_w)); // move.w Vid_RightX_w,Draw_ZoneClipR_w

            if (!devSkipEdgePvs()) {                   // DEV_CHECK_SET SKIP_EDGE_PVS,.no_edge_clip
                // move.l a0,-(sp) ; CALLC Zone_SetupEdgeClipping ; move.l (sp)+,a0
                ZoneEdgePvs.Zone_SetupEdgeClipping();

                if (Mem.b(Draw_ForceZoneSkip_b) != 0) { // tst.b Draw_ForceZoneSkip_b ; bne .subroomloop
                    continue;
                }
            }

            // .no_edge_clip:
            if (zdbgEnabled()) {                       // DEV_ZDBG ZDbg_First
                ZoneDebug.ZDbg_First();
            }

            a2 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a2
            a2 = Mem.l(a2 + ((short) d7) * 4);         // move.l (a2,d7.w*4),a2

            if (!devSkipEdgePvs()) {                   // DEV_CHECK_SET SKIP_EDGE_PVS,.no_edge_pvs
                // 0xABADCAFE - Quick Hack version of edge vis. If the zone is not tagged visible, skip.
                // ASM : tst.w ZoneT_Unused_w(a2) ; bne .no_edge_pvs (dessine si != 0 ; saute si == 0)
                if (Mem.w(a2 + ZoneT_Unused_w) == 0) { // beq → chemin de saut
                    if (zdbgEnabled()) {               // DEV_ZDBG ZDbg_SkipEdge
                        ZoneDebug.ZDbg_SkipEdge();
                    }
                    continue;                          // bra .subroomloop
                }
            }

            // .no_edge_pvs:
            int saved_a0 = a0;                         // move.l a0,-(a7)
            int za0 = a2;                              // move.l a2,a0

            Mem.wl(HiresData.Zone_SplitHeight_l, Mem.l(za0 + ZoneT_Roof_l)); // move.l ZoneT_Roof_l(a0),Zone_SplitHeight_l
            Mem.wl(draw_BackupRoomPtr_l, za0);         // move.l a0,draw_BackupRoomPtr_l

            int ga0 = Mem.l(Lvl_ZoneGraphAddsPtr_l);   // move.l Lvl_ZoneGraphAddsPtr_l,a0
            a2 = Mem.l(ga0 + ((short) d7) * 8 + 4);    // move.l 4(a0,d7.w*8),a2
            za0 = Mem.l(ga0 + ((short) d7) * 8);       // move.l (a0,d7.w*8),a0

            za0 += Mem.l(Lvl_GraphicsPtr_l);           // add.l Lvl_GraphicsPtr_l,a0
            a2 += Mem.l(Lvl_GraphicsPtr_l);            // add.l Lvl_GraphicsPtr_l,a2
            Mem.wl(HiresData.Draw_CurrentZonePtr_l + 4, a2); // move.l a2,Draw_CurrentZonePtr_l+4
            Mem.wl(HiresData.Draw_CurrentZonePtr_l, za0);    // move.l a0,Draw_CurrentZonePtr_l

            a1 = Mem.l(Lvl_ListOfGraphRoomsPtr_l);     // move.l Lvl_ListOfGraphRoomsPtr_l,a1

            finditit:
            while (true) { // .finditit:
                if (Mem.w(a1) < 0) {                   // tst.w (a1) ; blt .nomoretodoatall (PVST_Zone_w)
                    // .nomoretodoatall:
                    a0 = saved_a0;                     // move.l (a7)+,a0
                    continue subroomloop;              // bra .subroomloop
                }
                if (Mem.w(a1) == (short) d7) {         // cmp.w (a1),d7 ; beq .done_find
                    // .done_find: (le corps suit)
                } else {
                    a1 += PVST_SizeOf_l;               // adda.w #PVST_SizeOf_l,a1
                    continue;                          // bra .finditit
                }

                int saved_a1 = a1;                     // move.l a1,-(a7)

                // Set the initial clip extents.
                Mem.ww(HireswallData.Draw_LeftClip_w, Mem.uw(Draw_ZoneClipL_w));   // move.w Draw_ZoneClipL_w,Draw_LeftClip_w
                Mem.ww(HireswallData.Draw_RightClip_w, Mem.uw(Draw_ZoneClipR_w)); // move.w Draw_ZoneClipR_w,Draw_RightClip_w

                int d7c = 0;                           // moveq #0,d7
                d7c = setw(d7c, Mem.uw(a1 + PVST_ClipID_w)); // move.w PVST_ClipID_w(a1),d7
                if ((short) d7c >= 0) {                // blt.s .done_right_clip
                    int ca0 = Mem.l(Lvl_ClipsPtr_l);   // move.l Lvl_ClipsPtr_l,a0
                    ca0 = ca0 + d7c * 2;               // lea (a0,d7.l*2),a0
                    if (Mem.w(ca0) >= 0) {             // tst.w (a0) ; blt.s .done_left_clip
                        ca0 = DrawSetClip.Draw_SetLeftClip(ca0); // bsr Draw_SetLeftClip
                        // .left_clip:
                        while (Mem.w(ca0) >= 0) {      // tst.w (a0) ; blt.s .done_left_clip
                            ca0 = DrawSetClip.Draw_SetLeftClip(ca0); // bsr ; bra.s .left_clip
                        }
                    }
                    // .done_left_clip:
                    ca0 += 2;                          // addq #2,a0

                    if (Mem.w(ca0) >= 0) {             // tst.w (a0) ; blt .done_right_clip
                        ca0 = DrawSetClip.Draw_SetRightClip(ca0); // bsr Draw_SetRightClip
                        // .right_clip:
                        while (Mem.w(ca0) >= 0) {      // tst.w (a0) ; blt .done_right_clip
                            ca0 = DrawSetClip.Draw_SetRightClip(ca0); // bsr ; bra .right_clip
                        }
                    }
                }

                // .done_right_clip:
                // 0xABADCAFE - sign extensions and comparisons. Check these
                d0 = Mem.w(HireswallData.Draw_LeftClip_w); // move.w Draw_LeftClip_w,d0

                // .pass_left:
                // ext.l d0 ; why?
                Mem.wl(HireswallData.Draw_LeftClip_l, d0); // move.l d0,Draw_LeftClip_l

                d1 = Mem.w(HireswallData.Draw_RightClip_w); // move.w Draw_RightClip_w,d1

                // .pass_right:
                // ext.l d1
                Mem.wl(HireswallData.Draw_RightClip_l, d1); // move.l d1,Draw_RightClip_l
                boolean skipNotVisible = false;
                if (d1 < 0) {                          // blt .skip_not_visible
                    skipNotVisible = true;
                } else if ((short) d0 >= (short) d1) { // cmp.w d1,d0 ; bge .skip_not_visible
                    skipNotVisible = true;
                }

                if (!skipNotVisible) {
                    d0 = Mem.w(a1 + ZoneT_ID_w);       // move.w ZoneT_ID_w(a1),d0
                    // cmp.w draw_Root_Zone_w,d0 ; seq Draw_InRootZone_b
                    Mem.wb(Draw_InRootZone_b, (d0 == Mem.w(draw_Root_Zone_w)) ? 0xFF : 0);

                    int dy = Mem.l(HiresData.Plr_YOff_l); // move.l Plr_YOff_l,d0
                    if (dy < Mem.l(HiresData.Zone_SplitHeight_l)) { // cmp.l Zone_SplitHeight_l,d0 ; blt .lower_zone_first
                        lower_zone_first();
                    } else {
                        ready_upper();
                    }
                } else {
                    // .skip_not_visible:
                    if (zdbgEnabled()) {               // DEV_ZDBG ZDbg_Skip
                        ZoneDebug.ZDbg_Skip();
                    }
                }

                // .ready_next:
                a1 = saved_a1;                         // move.l (a7)+,a1
                int ra0 = Mem.l(HiresData.Draw_CurrentZonePtr_l); // move.l Draw_CurrentZonePtr_l,a0
                d7 = setw(d7, Mem.uw(ra0));            // move.w (a0),d7

                a1 += 8;                               // adda.w #8,a1
                // bra .finditit
            }
        }

        // .done_all_zones:
        if (zdbgEnabled()) {                           // DEV_ZDBG ZDbg_Done
            ZoneDebug.ZDbg_Done();
        }
        // rts
    }

    /** .ready_upper / .lower_zone_only â€” moitiÃ© haute d'abord (joueur sous le split). */
    private static void ready_upper() {
        // .ready_upper:
        int a0 = Mem.l(HiresData.Draw_CurrentZonePtr_l + 4); // move.l Draw_CurrentZonePtr_l+4,a0
        if (a0 != Mem.l(Lvl_GraphicsPtr_l)) {          // cmp.l Lvl_GraphicsPtr_l,a0 ; beq.s .lower_zone_only
            Mem.wb(Draw_DoUpper_b, 0xFF);              // st Draw_DoUpper_b

            int a1 = Mem.l(draw_BackupRoomPtr_l);      // move.l draw_BackupRoomPtr_l,a1
            Mem.wl(Draw_TopOfRoom_l, Mem.l(a1 + ZoneT_UpperRoof_l));     // move.l ZoneT_UpperRoof_l(a1),Draw_TopOfRoom_l
            Mem.wl(Draw_BottomOfRoom_l, Mem.l(a1 + ZoneT_UpperFloor_l)); // move.l ZoneT_UpperFloor_l(a1),Draw_BottomOfRoom_l

            Mem.wl(HireswallData.Draw_PointBrightsPtr_l, CurrentPointBrights_vl + 4); // move.l #CurrentPointBrights_vl+4,...
            draw_RenderCurrentZone(a0);                // bsr draw_RenderCurrentZone
        }

        // Room does not have an upper zone
        // .lower_zone_only:
        a0 = Mem.l(HiresData.Draw_CurrentZonePtr_l);   // move.l Draw_CurrentZonePtr_l,a0
        Mem.wb(Draw_DoUpper_b, 0);                     // clr.b Draw_DoUpper_b
        Mem.wl(HireswallData.Draw_PointBrightsPtr_l, CurrentPointBrights_vl); // move.l #CurrentPointBrights_vl,...

        int a1 = Mem.l(draw_BackupRoomPtr_l);          // move.l draw_BackupRoomPtr_l,a1
        int d0 = Mem.l(a1 + ZoneT_Roof_l);             // move.l ZoneT_Roof_l(a1),d0
        Mem.wl(Draw_TopOfRoom_l, d0);                  // move.l d0,Draw_TopOfRoom_l
        int d1 = Mem.l(a1 + ZoneT_Floor_l);            // move.l ZoneT_Floor_l(a1),d1
        Mem.wl(Draw_BottomOfRoom_l, d1);               // move.l d1,Draw_BottomOfRoom_l

        int d2 = Mem.l(a1 + ZoneT_Water_l);            // move.l ZoneT_Water_l(a1),d2
        if (d2 >= Mem.l(HiresData.Plr_YOff_l)) {       // cmp.l Plr_YOff_l,d2 ; blt.s .lzo_above_water_first
            Mem.wl(Draw_BeforeWaterTop_l, d2);         // move.l d2,Draw_BeforeWaterTop_l
            Mem.wl(Draw_BeforeWaterBottom_l, d1);      // move.l d1,Draw_BeforeWaterBottom_l
            Mem.wl(Draw_AfterWaterBottom_l, d2);       // move.l d2,Draw_AfterWaterBottom_l
            Mem.wl(Draw_AfterWaterTop_l, d0);          // move.l d0,Draw_AfterWaterTop_l
        } else {
            // .lzo_above_water_first:
            Mem.wl(Draw_BeforeWaterTop_l, d0);         // move.l d0,Draw_BeforeWaterTop_l
            Mem.wl(Draw_BeforeWaterBottom_l, d2);      // move.l d2,Draw_BeforeWaterBottom_l
            Mem.wl(Draw_AfterWaterBottom_l, d1);       // move.l d1,Draw_AfterWaterBottom_l
            Mem.wl(Draw_AfterWaterTop_l, d2);          // move.l d2,Draw_AfterWaterTop_l
        }
        // .lzo_below_water_first:
        draw_RenderCurrentZone(a0);                    // bsr draw_RenderCurrentZone
        // bra .ready_next
    }

    /** .lower_zone_first â€” moitiÃ© basse d'abord (joueur au-dessus du split). */
    private static void lower_zone_first() {
        int a0 = Mem.l(HiresData.Draw_CurrentZonePtr_l); // move.l Draw_CurrentZonePtr_l,a0
        Mem.wb(Draw_DoUpper_b, 0);                     // clr.b Draw_DoUpper_b
        Mem.wl(HireswallData.Draw_PointBrightsPtr_l, CurrentPointBrights_vl); // move.l #CurrentPointBrights_vl,...
        int a1 = Mem.l(draw_BackupRoomPtr_l);          // move.l draw_BackupRoomPtr_l,a1
        int d0 = Mem.l(a1 + ZoneT_Roof_l);             // move.l ZoneT_Roof_l(a1),d0
        Mem.wl(Draw_TopOfRoom_l, d0);                  // move.l d0,Draw_TopOfRoom_l
        int d1 = Mem.l(a1 + ZoneT_Floor_l);            // move.l ZoneT_Floor_l(a1),d1
        Mem.wl(Draw_BottomOfRoom_l, d1);               // move.l d1,Draw_BottomOfRoom_l
        int d2 = Mem.l(a1 + ZoneT_Water_l);            // move.l ZoneT_Water_l(a1),d2
        if (d2 >= Mem.l(HiresData.Plr_YOff_l)) {       // cmp.l Plr_YOff_l,d2 ; blt.s .lzf_above_water_first
            Mem.wl(Draw_BeforeWaterTop_l, d2);         // move.l d2,Draw_BeforeWaterTop_l
            Mem.wl(Draw_BeforeWaterBottom_l, d1);      // move.l d1,Draw_BeforeWaterBottom_l
            Mem.wl(Draw_AfterWaterBottom_l, d2);       // move.l d2,Draw_AfterWaterBottom_l
            Mem.wl(Draw_AfterWaterTop_l, d0);          // move.l d0,Draw_AfterWaterTop_l
        } else {
            // .lzf_above_water_first:
            Mem.wl(Draw_BeforeWaterTop_l, d0);         // move.l d0,Draw_BeforeWaterTop_l
            Mem.wl(Draw_BeforeWaterBottom_l, d2);      // move.l d2,Draw_BeforeWaterBottom_l
            Mem.wl(Draw_AfterWaterBottom_l, d1);       // move.l d1,Draw_AfterWaterBottom_l
            Mem.wl(Draw_AfterWaterTop_l, d2);          // move.l d2,Draw_AfterWaterTop_l
        }
        // .lzf_below_water_first:
        draw_RenderCurrentZone(a0);                    // bsr draw_RenderCurrentZone
        a0 = Mem.l(HiresData.Draw_CurrentZonePtr_l + 4); // move.l Draw_CurrentZonePtr_l+4,a0
        if (a0 == Mem.l(Lvl_GraphicsPtr_l)) {          // cmp.l Lvl_GraphicsPtr_l,a0 ; beq.s .noupperroom2
            return;
        }

        Mem.wl(HireswallData.Draw_PointBrightsPtr_l, CurrentPointBrights_vl + 4); // move.l #CurrentPointBrights_vl+4,...
        a1 = Mem.l(draw_BackupRoomPtr_l);              // move.l draw_BackupRoomPtr_l,a1
        Mem.wl(Draw_TopOfRoom_l, Mem.l(a1 + ZoneT_UpperRoof_l));     // move.l ZoneT_UpperRoof_l(a1),Draw_TopOfRoom_l
        Mem.wl(Draw_BottomOfRoom_l, Mem.l(a1 + ZoneT_UpperFloor_l)); // move.l ZoneT_UpperFloor_l(a1),Draw_BottomOfRoom_l

        Mem.wb(Draw_DoUpper_b, 0xFF);                  // st Draw_DoUpper_b
        draw_RenderCurrentZone(a0);                    // bsr draw_RenderCurrentZone
        // .noupperroom2: IFD ZONE_DEBUG â†’ bra .ready_next (Ã©quivalent au fallthrough)
    }

    /** draw_RenderCurrentZone â€” a0 pointe le flux zone-graph (premier mot = id de zone). */
    public static void draw_RenderCurrentZone(int a0) {
        int d0 = Mem.w(a0); a0 += 2;                   // move.w (a0)+,d0
        Mem.ww(Draw_CurrentZone_w, d0);                // move.w d0,Draw_CurrentZone_w

        if (zdbgEnabled()) {                           // DEV_ZDBG ZDbg_Enter
            ZoneDebug.ZDbg_Enter();
        }

        int d1 = d0;                                   // move.w d0,d1
        d1 = muls(d1, 40);                             // muls #40,d1
        d1 += Lvl_BigMap_vl;                           // add.l #Lvl_BigMap_vl,d1
        Mem.wl(HiresData.Lvl_BigMapPtr_l, d1);         // move.l d1,Lvl_BigMapPtr_l
        d1 = setw(d1, d0);                             // move.w d0,d1
        d1 = (short) d1;                               // ext.l d1
        d1 = lslw(d1, 2);                              // asl.w #2,d1 (mot faible seul)
        d1 += Lvl_CompactMap_vl;                       // add.l #Lvl_CompactMap_vl,d1
        Mem.wl(HiresData.Lvl_CompactMapPtr_l, d1);     // move.l d1,Lvl_CompactMapPtr_l
        d1 += 4;                                       // add.l #4,d1
        if (d1 > Mem.l(HiresData.LastZonePtr_l)) {     // cmp.l LastZonePtr_l,d1 ; ble.s .no_change
            Mem.wl(HiresData.LastZonePtr_l, d1);       // move.l d1,LastZonePtr_l
        }

        // .no_change:
        int a1 = Zone_BrightTable_vl;                  // move.l #Zone_BrightTable_vl,a1
        d1 = Mem.l(a1 + ((short) d0) * 4);             // move.l (a1,d0.w*4),d1
        if (Mem.b(Draw_DoUpper_b) == 0) {              // tst.b Draw_DoUpper_b ; bne.s .ok_bottom
            d1 = swap(d1);                             // swap d1
        }
        // .ok_bottom:
        Mem.ww(HiresData.Zone_Bright_w, d1);           // move.w d1,Zone_Bright_w

        while (true) { // .draw_loop:
            d0 = Mem.w(a0); a0 += 2;                   // move.w (a0)+,d0
            Mem.ww(HiresData.draw_WallID_w, d0);       // move.w d0,draw_WallID_w
            d0 = d0 & 0xFF;                            // and.w #$ff,d0

            // TODO (original) - this can be a regular jump table.
            // 0 => wall ; 1,2 => floor/ceiling ; 4 => object ; 7 => water ; 12 => backdrop ; <0 => end
            if ((byte) d0 < 0) {                       // tst.b d0 ; blt .end_draw_loop
                return;                                // .end_draw_loop: rts
            }
            if (d0 == 0) {                             // beq .itsawall
                // .itsawall:
                a0 = Hireswall.Draw_Wall(a0);          // jsr Draw_Wall
                continue;                              // bra .draw_loop
            }
            if (d0 < 3) {                              // cmp.w #3,d0 ; blt .itsafloor
                // .itsafloor:
                Mem.wl(HiresData.FloorPtBrightsPtr_l, Mem.l(HireswallData.Draw_PointBrightsPtr_l)); // move.l Draw_PointBrightsPtr_l,FloorPtBrightsPtr_l
                int dz = Mem.w(Draw_CurrentZone_w);    // move.w Draw_CurrentZone_w,d1
                dz = muls(dz, 80);                     // muls #80,d1
                if (d0 == 2) {                         // cmp.w #2,d0 ; bne.s .nfl
                    dz += 2;                           // add.l #2,d1
                }
                // .nfl:
                Mem.wl(HiresData.FloorPtBrightsPtr_l, Mem.l(HiresData.FloorPtBrightsPtr_l) + dz); // add.l d1,FloorPtBrightsPtr_l
                Mem.ww(HiresData.SMALLIT, 1);          // move.w #1,SMALLIT

                // * 1,2 = floor/roof
                Mem.wb(HiresData.draw_UseWater_b, 0);  // clr.b draw_UseWater_b
                Mem.wb(HiresData.draw_UseGouraudFlats_b, Mem.ub(HiresData.draw_GouraudFlatsSelected_b)); // move.b draw_GouraudFlatsSelected_b,draw_UseGouraudFlats_b
                a0 = Hires.Draw_Flats(a0, d0);         // jsr Draw_Flats
                continue;                              // bra .draw_loop
            }
            if (d0 == 4) {                             // cmp.w #4,d0 ; beq .itsanobject
                // .itsanobject:
                a0 = Objdrawhires.Draw_Objects(a0);    // jsr Draw_Objects
                continue;                              // bra .draw_loop
            }
            if (d0 == 7) {                             // cmp.w #7,d0 ; beq.s .itswater
                // .itswater:
                Mem.ww(HiresData.SMALLIT, 2);          // move.w #2,SMALLIT
                d0 = 3;                                // move.w #3,d0
                Mem.wb(HiresData.draw_UseGouraudFlats_b, 0); // clr.b draw_UseGouraudFlats_b
                Mem.wb(HiresData.draw_UseWater_b, 0xFF);     // st draw_UseWater_b
                a0 = Hires.Draw_Flats(a0, d0);         // jsr Draw_Flats
                continue;                              // bra .draw_loop
            }
            if (d0 == 12) {                            // cmp.w #12,d0 ; beq.s .itsbackdrop
                // .itsbackdrop:
                a0 = Newanims.Draw_SkyBackdrop(a0);    // jsr Draw_SkyBackdrop
                continue;                              // bra .draw_loop
            }
            // bra .draw_loop
        }
    }
}

