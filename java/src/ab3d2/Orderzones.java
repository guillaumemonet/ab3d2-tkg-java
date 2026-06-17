package ab3d2;

import ab3d2.c.ZoneEdgePvs;
import ab3d2.modules.DevInst;
import ab3d2.modules.Sys;

import static ab3d2.Defs.EdgeT_JoinZone_w;
import static ab3d2.Defs.EdgeT_XLen_w;
import static ab3d2.Defs.EdgeT_XPos_w;
import static ab3d2.Defs.EdgeT_ZLen_w;
import static ab3d2.Defs.EdgeT_ZPos_w;
import static ab3d2.Defs.ZoneT_EdgeListOffset_w;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;
import static ab3d2.bss.LevelBss.Lvl_GraphicsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ListOfGraphRoomsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneEdgePtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneGraphAddsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.SystemBss.Sys_Workspace_vl;
import static ab3d2.bss.ZoneBss.Zone_EndOfListPtr_l;
import static ab3d2.bss.ZoneBss.Zone_FinalOrderTable_vw;
import static ab3d2.bss.ZoneBss.zone_OrderTable_vw;
import static ab3d2.bss.ZoneBss.zone_ToDrawTable_vw;

/**
 * Traduction littérale de ab3d2_source/orderzones.s
 *
 * Tri des zones visibles (painter's algorithm) : à partir de la liste des
 * zones graphiques visibles (Lvl_ListOfGraphRoomsPtr_l), construit une liste
 * chaînée doublement ordonnée (zone_OrderTable_vw, tuples de 8 octets :
 * {prev, zoneId, next, clipId}) en insérant chaque zone du bon côté de ses
 * voisines selon le côté du joueur par rapport aux arêtes joignantes
 * (produit en croix), puis aplatit en Zone_FinalOrderTable_vw.
 */
public final class Orderzones {

    private static final int _a0 = Mem.align(4);

    public static final int Zone_MovementMask_l = Mem.dcL(0xFFF0FFF0);

    public static final int tmp_ListOfGraphRoomsPtr_l = Mem.dcL(0);

    /** basically a short coordinate pair */
    public static final int zone_LastPosition_vw = Mem.dcL(-1);

    private Orderzones() {
    }

    /** Zone_OrderZones */
    public static void Zone_OrderZones() {
        ZoneEdgePvs.Zone_CheckVisibleEdges();          // CALLC Zone_CheckVisibleEdges

        int d0 = Mem.uw(HiresData.Plr_XOff_l);         // move.w Plr_XOff_l,d0
        d0 = swap(d0);                                 // swap d0
        d0 = setw(d0, Mem.uw(HiresData.Plr_ZOff_l));   // move.w Plr_ZOff_l,d0 ; short coord du joueur
        d0 &= Mem.l(Zone_MovementMask_l);              // and.l Zone_MovementMask_l,d0 ; réduit la sensibilité
        if (d0 == Mem.l(zone_LastPosition_vw)) {       // cmp.l zone_LastPosition_vw,d0 ; bne .continue
            return;                                    // rts
        }

        // .continue:
        Mem.wl(zone_LastPosition_vw, d0);              // move.l d0,zone_LastPosition_vw

        // prepare to clear out zone_ToDrawTable_vw
        // @todo (original) - zone_ToDrawTable_vw is 400 words, this only clears 100 longs (half).
        Sys.Sys_MemFillLong(zone_ToDrawTable_vw, 0, 100); // move.l #...,a0 ; moveq #0,d0 ; moveq #100,d1 ; bsr Sys_MemFillLong

        int a1 = Mem.l(Lvl_ListOfGraphRoomsPtr_l);     // move.l Lvl_ListOfGraphRoomsPtr_l,a1 ; liste des zones à dessiner
        Mem.wl(tmp_ListOfGraphRoomsPtr_l, a1);         // move.l a1,tmp_ListOfGraphRoomsPtr_l
        int a3 = zone_ToDrawTable_vw;                  // move.l #zone_ToDrawTable_vw,a3
        int a4 = Sys_Workspace_vl;                     // move.l #Sys_Workspace_vl,a4
        int a5 = zone_OrderTable_vw;                   // move.l #zone_OrderTable_vw,a5

        while (true) { // .set_to_draw:
            d0 = Mem.w(a1);                            // move.w (a1),d0
            if (d0 < 0) {                              // blt.s .no_more_set
                break;
            }
            Mem.wb(a3 + (short) d0, 0xFF);             // st (a3,d0.w)
            Mem.wl(a4 + ((short) d0) * 4, Mem.l(a1 + 4)); // move.l 4(a1),(a4,d0.w*4)
            a1 += 8;                                   // adda.w #8,a1
        }                                              // bra.s .set_to_draw

        // .no_more_set: table avec $ff par zone à dessiner.
        int a0 = Mem.l(tmp_ListOfGraphRoomsPtr_l);     // move.l tmp_ListOfGraphRoomsPtr_l,a0
        int a2 = zone_OrderTable_vw;                   // move.l #zone_OrderTable_vw,a2
        d0 = 0;                                        // moveq #0,d0
        int d1 = 2;                                    // moveq #2,d1

        while (true) { // .put_in_n:
            int d2 = Mem.w(a0);                        // move.w (a0),d2
            if (d2 < 0) {                              // blt.s .put_all_in
                break;
            }
            a1 = Mem.l(Lvl_ZoneGraphAddsPtr_l);        // move.l Lvl_ZoneGraphAddsPtr_l,a1
            a1 = Mem.l(a1 + ((short) d2) * 4);         // move.l (a1,d2.w*4),a1
            a1 += Mem.l(Lvl_GraphicsPtr_l);            // add.l Lvl_GraphicsPtr_l,a1 (a1 calculé, non utilisé ensuite)
            a2 += 8;                                   // addq #8,a2
            Mem.ww(a2 + 2, d2);                        // move.w d2,2(a2)
            Mem.ww(a2, d0);                            // move.w d0,(a2)
            Mem.ww(a2 + 4, d1);                        // move.w d1,4(a2)
            d0 += 1;                                   // addq #1,d0
            d1 += 1;                                   // addq #1,d1
            a0 += 8;                                   // adda.w #8,a0
        }                                              // bra .put_in_n

        // .put_all_in:
        Mem.ww(a2 + 4, -1);                            // move.w #-1,4(a2)
        Mem.ww(zone_OrderTable_vw + 4, 1);             // move.w #1,zone_OrderTable_vw+4
        Mem.ww(zone_OrderTable_vw, -1);               // move.w #-1,zone_OrderTable_vw
        Mem.ww(zone_OrderTable_vw + 2, -1);           // move.w #-1,zone_OrderTable_vw+2
        int d5 = 2;                                    // move.w #2,d5 ; off end of list.
        int d7 = 100;                                  // move.w #100,d7 ; which ones to look at.

        a5 = a2;                                       // move.l a2,a5

        while (true) { // .run_through_list:
            // DEV_INC.w Reserved2
            Mem.ww(DevInst.dev_Reserved2_w, Mem.uw(DevInst.dev_Reserved2_w) + 1); // addq.w #1,dev_Reserved2_w
            a1 = Mem.l(Lvl_ZoneEdgePtr_l);             // move.l Lvl_ZoneEdgePtr_l,a1
            d0 = Mem.uw(a5 + 2);                       // move.w 2(a5),d0
            int a6 = Sys_Workspace_vl;                 // move.l #Sys_Workspace_vl,a6
            a6 = a6 + ((short) d0) * 4;                // lea (a6,d0.w*4),a6
            int d6 = Mem.l(a6);                        // move.l (a6),d6
            a0 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a0
            a0 = Mem.l(a0 + ((short) d0) * 4);         // move.l (a0,d0.w*4),a0
            a0 = a0 + Mem.w(a0 + ZoneT_EdgeListOffset_w); // adda.w ZoneT_EdgeListOffset_w(a0),a0
            int a4cur = a5;                            // move.l a5,a4

            d0 = Mem.w(a5);                            // move.w (a5),d0
            if (d0 < 0) {                              // blt.s .done_all_this_pass
                break;
            }
            a5 = zone_OrderTable_vw;                   // move.l #zone_OrderTable_vw,a5
            a5 = a5 + ((short) d0) * 8;                // lea (a5,d0.w*8),a5

            d6 = zone_InsertList(a0, a1, a4cur, a6, d6); // bsr zone_InsertList (d6 = masque mis à jour, réécrit dans (a6))

            d7 = setw(d7, d7 - 1);                     // dbra d7,.run_through_list
            if ((short) d7 == -1) {
                break;
            }
        }

        // .done_all_this_pass: / .dont_order:
        a5 = zone_OrderTable_vw;                       // move.l #zone_OrderTable_vw,a5
        d0 = Mem.w(a5 + 4);                            // move.w 4(a5),d0
        a5 = a5 + ((short) d0) * 8;                    // lea (a5,d0.w*8),a5
        a0 = Zone_FinalOrderTable_vw;                  // move.l #Zone_FinalOrderTable_vw,a0

        while (true) { // .show_order:
            Mem.ww(a0, Mem.uw(a5 + 2)); a0 += 2;       // move.w 2(a5),(a0)+
            d0 = Mem.w(a5 + 4);                        // move.w 4(a5),d0
            if (d0 < 0) {                              // blt.s .done_order
                break;
            }
            a5 = zone_OrderTable_vw;                   // move.l #zone_OrderTable_vw,a5
            a5 = a5 + ((short) d0) * 8;                // lea (a5,d0.w*8),a5
        }                                              // bra.s .show_order

        // .done_order:
        Mem.wl(Zone_EndOfListPtr_l, a0);              // move.l a0,Zone_EndOfListPtr_l
        // rts
    }

    /**
     * zone_InsertList — insère/réordonne les voisines de la zone courante.
     * a0 = liste d'arêtes (terminée <0), a1 = Lvl_ZoneEdgePtr_l,
     * a4 = entrée courante dans zone_OrderTable_vw, a6 = slot du masque,
     * d6 = masque de bits par arête. Renvoie d6 (réécrit aussi dans (a6)).
     */
    private static int zone_InsertList(int a0, int a1, int a4, int a6, int d6) {
        // move.l d7,-(a7) — d7 sauvegardé (le caller le réutilise via dbra)
        int d7 = 0;                                    // moveq #0,d7

        while (true) { // .insert_loop:
            int d0 = Mem.w(a0); a0 += 2;               // move.w (a0)+,d0 ; floor line
            if (d0 < 0) {                              // blt .all_in_list
                break;
            }
            d0 = setw(d0, d0 << 4);                    // asl.w #4,d0

            int d1 = 0;                                // moveq #0,d1
            d1 = setw(d1, Mem.uw(a1 + (short) d0 + EdgeT_JoinZone_w)); // move.w EdgeT_JoinZone_w(a1,d0.w),d1
            if ((short) d1 < 0) {                      // blt.s .buggergerger
                // .buggergerger:
                d7 = setw(d7, d7 + 3);                 // addq #3,d7
                continue;                              // bra.s .insert_loop
            }
            if ((d6 & (1 << (d7 & 31))) == 0) {        // btst d7,d6 ; bne.s .in_draw_list
                // .buggergerger:
                d7 = setw(d7, d7 + 3);                 // addq #3,d7
                continue;                              // bra.s .insert_loop
            }

            // .in_draw_list:
            d7 = setw(d7, d7 + 1);                     // addq #1,d7

            boolean mustDo;
            if ((d6 & (1 << (d7 & 31))) == 0) {        // btst d7,d6 ; bne.s .we_already_know
                // Here is a room in the draw list. Is it closer than this one?
                d6 |= 1 << (d7 & 31);                  // bset d7,d6
                int d2 = Mem.uw(HiresData.Plr_XOff_l); // move.w Plr_XOff_l,d2
                int d3 = Mem.uw(HiresData.Plr_ZOff_l); // move.w Plr_ZOff_l,d3
                d2 = setw(d2, d2 - Mem.uw(a1 + (short) d0 + EdgeT_XPos_w)); // sub.w EdgeT_XPos_w,d2
                d3 = setw(d3, d3 - Mem.uw(a1 + (short) d0 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w,d3
                d2 = muls(d2, Mem.w(a1 + (short) d0 + EdgeT_ZLen_w)); // muls EdgeT_ZLen_w,d2
                d3 = muls(d3, Mem.w(a1 + (short) d0 + EdgeT_XLen_w)); // muls EdgeT_XLen_w,d3
                d7 = setw(d7, d7 + 1);                 // addq #1,d7
                d2 -= d3;                              // sub.l d3,d2
                if (d2 <= 0) {                         // ble.s .put_done
                    // .put_done:
                    d7 = setw(d7, d7 + 1);             // addq #1,d7
                    continue;                          // .not_in_draw_list: bra .insert_loop
                }
                d6 |= 1 << (d7 & 31);                  // bset d7,d6
                mustDo = true;                         // bra.s .must_do
            } else {
                // .we_already_know:
                d7 = setw(d7, d7 + 1);                 // addq #1,d7
                if ((d6 & (1 << (d7 & 31))) == 0) {    // btst d7,d6 ; beq.s .put_done
                    // .put_done:
                    d7 = setw(d7, d7 + 1);             // addq #1,d7
                    continue;                          // bra .insert_loop
                }
                // Si la zone connectée est censée être plus proche, on l'ignore.
                mustDo = true;                         // (chute dans .must_do)
            }

            // .must_do: la zone connectée (plus lointaine) est devant dans la
            // liste → il faut passer de l'autre côté.
            if (mustDo) {
                int a3 = zone_OrderTable_vw;           // move.l #zone_OrderTable_vw,a3
                d0 = Mem.w(a4);                        // move.w (a4),d0
                boolean isCloser = false;
                if (d0 >= 0) {                         // blt.s .not_closer
                    while (true) { // .check_closer:
                        if ((short) d1 == Mem.w(a3 + ((short) d0) * 8 + 2)) { // cmp.w 2(a3,d0.w*8),d1 ; beq.s .is_closer
                            isCloser = true;
                            break;
                        }
                        d0 = Mem.w(a3 + ((short) d0) * 8); // move.w (a3,d0.w*8),d0
                        if (d0 < 0) {                  // bge.s .check_closer ; bra.s .not_closer
                            break;
                        }
                    }
                }

                if (isCloser) {
                    // .is_closer: la zone plus lointaine est dans la partie
                    // proche de la liste ; on la déplace devant. a3,d0.w*8 = la fautive.
                    int d2 = Mem.w(a4);                // move.w (a4),d2
                    int d5 = Mem.uw(a3 + ((short) d2) * 8 + 4); // move.w 4(a3,d2.w*8),d5 ; this place
                    int d3 = Mem.w(a4 + 4);            // move.w 4(a4),d3
                    if (d3 >= 0) {                     // blt.s .from_end
                        Mem.ww(a3 + ((short) d3) * 8, d2); // move.w d2,(a3,d3.w*8)
                    }
                    // .from_end:
                    Mem.ww(a3 + ((short) d2) * 8 + 4, d3); // move.w d3,4(a3,d2.w*8)
                    d2 = Mem.w(a3 + ((short) d0) * 8);  // move.w (a3,d0.w*8),d2
                    int d4 = Mem.uw(a3 + ((short) d2) * 8 + 4); // move.w 4(a3,d2.w*8),d4
                    Mem.ww(a3 + ((short) d5) * 8, d2);  // move.w d2,(a3,d5.w*8)
                    Mem.ww(a3 + ((short) d5) * 8 + 4, d4); // move.w d4,4(a3,d5.w*8)
                    Mem.ww(a3 + ((short) d4) * 8, d5);  // move.w d5,(a3,d4.w*8)
                    Mem.ww(a3 + ((short) d2) * 8 + 4, d5); // move.w d5,4(a3,d2.w*8)
                }
            }
            // .not_closer: / .put_done:
            d7 = setw(d7, d7 + 1);                     // addq #1,d7
            // .not_in_draw_list: bra .insert_loop
        }

        // .all_in_list:
        Mem.wl(a6, d6);                                // move.l d6,(a6)
        // move.l (a7)+,d7 ; rts
        return d6;
    }
}
