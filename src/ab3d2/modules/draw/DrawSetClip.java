package ab3d2.modules.draw;

import ab3d2.HireswallData;
import ab3d2.Mem;
import ab3d2.c.ZoneDebug;
import ab3d2.modules.DevInst;
import ab3d2.modules.DevMacros;

import static ab3d2.bss.LevelBss.Lvl_ConnectTablePtr_l;
import static ab3d2.bss.LevelBss.Lvl_PointsPtr_l;
import static ab3d2.bss.TablesBss.OnScreen_vl;
import static ab3d2.bss.TablesBss.Rotated_vl;

/**
 * Traduction littérale de ab3d2_source/modules/draw/draw_set_clip.s
 *
 * "Offset into Lvl_ClipsPtr_l data is in a0"
 *
 * Conventions : a0 (curseur dans les données de clips) est passé en paramètre
 * et la nouvelle valeur est renvoyée. a6 n'est qu'avancé de 8 dans l'original
 * (toutes ses écritures sont commentées) — vestigial, non reproduit.
 * Build DEV+ZONE_DEBUG : DEV_ZDBG_CLIP n → écrit SetClipStage_w ; DEV_ZDBG →
 * trace si le bit DEV_ZONE_TRACE est actif.
 */
public final class DrawSetClip {

    // _SetClipStage_w:: / SetClipStage_w: dc.w 0
    public static final int SetClipStage_w = Mem.dcW(0);
    // _SetClipTmpL:: / SetClipTmpL: dc.w 0
    public static final int SetClipTmpL = Mem.dcW(0);
    // _SetClipTmpR:: / SetClipTmpR: dc.w 0
    public static final int SetClipTmpR = Mem.dcW(0);

    private DrawSetClip() {
    }

    /** DEV_ZDBG_CLIP n : move.w #n,SetClipStage_w */
    private static void DEV_ZDBG_CLIP(int n) {
        Mem.ww(SetClipStage_w, n);
    }

    /** DEV_ZDBG : trace si DEV_ZONE_TRACE actif (registres non transmis, cf. ZoneDebug). */
    private static boolean zdbgEnabled() {
        return DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_ZONE_TRACE);
    }

    /** Draw_SetLeftClip — a0 pointe l'entrée de clip courante ; renvoie a0 avancé. */
    public static int Draw_SetLeftClip(int a0) {
        DEV_ZDBG_CLIP(0);
        if (zdbgEnabled()) {
            ZoneDebug.ZDbg_LeftClip();
        }

        int a1 = OnScreen_vl;                          // move.l #OnScreen_vl,a1
        int a2 = Rotated_vl;                           // move.l #Rotated_vl,a2
        int a3 = Mem.l(Lvl_ConnectTablePtr_l);         // move.l Lvl_ConnectTablePtr_l,a3
        int a4 = Mem.l(Lvl_PointsPtr_l);               // move.l Lvl_PointsPtr_l,a4 (non utilisé ensuite)
        int d0 = Mem.w(a0);                            // move.w (a0),d0
        if (d0 < 0) {                                  // bge.s .dont_ignore_left
            DEV_ZDBG_CLIP(1);
            if (zdbgEnabled()) {
                ZoneDebug.ZDbg_LeftClip();
            }
            // move.l #0,(a6) (commenté dans l'original)
            // bra .left_not_ok_to_clip
            DEV_ZDBG_CLIP(7);                          // .left_not_ok_to_clip:
            if (zdbgEnabled()) {
                ZoneDebug.ZDbg_LeftClip();
            }
            a0 += 2;                                   // .done_left_clip: addq #2,a0
            return a0;                                 // rts
        }

        // .dont_ignore_left:
        DEV_ZDBG_CLIP(2);
        if (zdbgEnabled()) {
            ZoneDebug.ZDbg_LeftClip();
        }

        int d3 = Mem.w(a2 + d0 * 8 + 6);               // move.w 6(a2,d0*8),d3 ; left z val
        if (d3 <= 0) {                                 // bgt.s .left_clip_infront
            DEV_ZDBG_CLIP(3);
            if (zdbgEnabled()) {
                ZoneDebug.ZDbg_LeftClip();
            }
            a0 += 2;                                   // addq #2,a0
            return a0;                                 // rts
        }
        // [code inatteignable dans l'original après ce rts :
        //   tst.w 6(a2,d0*8) ; bgt .left_not_ok_to_clip
        //  .ignore_both:
        //   move.w #0,Draw_LeftClip_w ; move.w Vid_RightX_w,Draw_RightClip_w
        //   addq #8,a6 ; addq #2,a0 ; rts]

        // .left_clip_infront:
        DEV_ZDBG_CLIP(5);
        if (zdbgEnabled()) {
            ZoneDebug.ZDbg_LeftClip();
        }

        // d0 is Lvl_ClipsPtr_l[ClipID], so the Clip value is a point index into the onscreen array
        int d1 = Mem.w(a1 + d0 * 2);                   // move.w (a1,d0*2),d1 ; left x on screen
        int d2 = Mem.w(a0);                            // move.w (a0),d2
        d2 = Mem.w(a3 + ((short) d2) * 4 + 2);         // move.w 2(a3,d2.w*4),d2
        d2 = Mem.w(a1 + ((short) d2) * 2);             // move.w (a1,d2.w*2),d2
        if (d2 <= d1) {                                // cmp.w d1,d2 ; bgt.s .left_not_ok_to_clip
            // move.w d1,(a6) ; move.w d3,2(a6) (commentés)
            if (d1 > Mem.w(HireswallData.Draw_LeftClip_w)) { // cmp.w Draw_LeftClip_w,d1 ; ble.s .left_not_ok_to_clip
                Mem.ww(HireswallData.Draw_LeftClip_w, d1);   // move.w d1,Draw_LeftClip_w
                DEV_ZDBG_CLIP(6);
                if (zdbgEnabled()) {
                    ZoneDebug.ZDbg_LeftClip();
                }
                // bra.s .done_left_clip
                a0 += 2;                               // .done_left_clip: addq #2,a0
                return a0;                             // rts
            }
        }
        // .left_not_ok_to_clip:
        DEV_ZDBG_CLIP(7);
        if (zdbgEnabled()) {
            ZoneDebug.ZDbg_LeftClip();
        }
        // .done_left_clip:
        a0 += 2;                                       // addq #2,a0
        return a0;                                     // rts
    }

    /** Draw_SetRightClip — a0 pointe l'entrée de clip courante ; renvoie a0 avancé (addq #8,a6 vestigial). */
    public static int Draw_SetRightClip(int a0) {
        DEV_ZDBG_CLIP(0);
        if (zdbgEnabled()) {
            ZoneDebug.ZDbg_RightClip();
        }

        int a1 = OnScreen_vl;                          // move.l #OnScreen_vl,a1
        int a2 = Rotated_vl;                           // move.l #Rotated_vl,a2
        int a3 = Mem.l(Lvl_ConnectTablePtr_l);         // move.l Lvl_ConnectTablePtr_l,a3
        int d0 = Mem.w(a0);                            // move.w (a0),d0
        int d4;
        if (d0 < 0) {                                  // bge.s .dont_ignore_right
            DEV_ZDBG_CLIP(1);
            if (zdbgEnabled()) {
                ZoneDebug.ZDbg_RightClip();
            }
            // move.w #96,4(a6) ; move.w #0,6(a6) (commentés)
            d4 = 0;                                    // move.w #0,d4
            // bra .right_not_ok_to_clip
            DEV_ZDBG_CLIP(7);                          // .right_not_ok_to_clip:
            if (zdbgEnabled()) {
                ZoneDebug.ZDbg_RightClip();
            }
            a0 += 2;                                   // .done_right_clip: addq #8,a6 ; addq #2,a0
            return a0;                                 // rts
        }

        // .dont_ignore_right:
        DEV_ZDBG_CLIP(2);
        if (zdbgEnabled()) {
            ZoneDebug.ZDbg_RightClip();
        }

        d4 = Mem.w(a2 + d0 * 8 + 6);                   // move.w 6(a2,d0*8),d4 ; right z val
        if (d4 <= 0) {                                 // bgt.s .right_clip_infront
            DEV_ZDBG_CLIP(3);
            if (zdbgEnabled()) {
                ZoneDebug.ZDbg_RightClip();
            }
            // move.w #96,4(a6) ; move.w #0,6(a6) (commentés)
            // bra .right_not_ok_to_clip
            DEV_ZDBG_CLIP(7);                          // .right_not_ok_to_clip:
            if (zdbgEnabled()) {
                ZoneDebug.ZDbg_RightClip();
            }
            a0 += 2;                                   // .done_right_clip:
            return a0;                                 // rts
        }

        // .right_clip_infront:
        DEV_ZDBG_CLIP(4);
        if (zdbgEnabled()) {
            ZoneDebug.ZDbg_RightClip();
        }

        int d1 = Mem.w(a1 + d0 * 2);                   // move.w (a1,d0*2),d1 ; right x on screen
        int d2 = Mem.w(a0);                            // move.w (a0),d2
        d2 = Mem.w(a3 + ((short) d2) * 4);             // move.w (a3,d2.w*4),d2
        d2 = Mem.w(a1 + ((short) d2) * 2);             // move.w (a1,d2.w*2),d2
        if (d2 >= d1) {                                // cmp.w d1,d2 ; blt.s .right_not_ok_to_clip
            DEV_ZDBG_CLIP(5);
            if (zdbgEnabled()) {
                ZoneDebug.ZDbg_RightClip();
            }
            // move.w d1,4(a6) ; move.w d4,6(a6) (commentés)
            if (d1 < Mem.w(HireswallData.Draw_RightClip_w)) { // cmp.w Draw_RightClip_w,d1 ; bge.s .right_not_ok_to_clip
                DEV_ZDBG_CLIP(6);
                if (zdbgEnabled()) {
                    ZoneDebug.ZDbg_RightClip();
                }
                d1 += 1;                               // addq #1,d1
                Mem.ww(HireswallData.Draw_RightClip_w, d1); // move.w d1,Draw_RightClip_w
                // bra.s .done_right_clip
                a0 += 2;                               // .done_right_clip:
                return a0;                             // rts
            }
        }
        // .right_not_ok_to_clip:
        DEV_ZDBG_CLIP(7);
        if (zdbgEnabled()) {
            ZoneDebug.ZDbg_RightClip();
        }
        // .done_right_clip:
        a0 += 2;                                       // addq #2,a0 (addq #8,a6 vestigial)
        return a0;                                     // rts
    }
}
