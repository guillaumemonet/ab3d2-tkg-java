package ab3d2.modules;

import ab3d2.Mem;
import ab3d2.host.DosLib;

import static ab3d2.bss.LevelBss.Lvl_ModPropertiesPtr_l;
import static ab3d2.bss.ZoneBss.ZONE_BACKDROP_DISABLE_SIZE;
import static ab3d2.bss.ZoneBss.Zone_BackdropDisable_vb;

/**
 * Traduction littérale de ab3d2_source/modules/level.s
 */
public final class Level {

    // .backdrop_disable_dumpfile_vb (chaîne du bloc DEV)
    private static final int backdrop_disable_dumpfile_vb;

    static {
        Mem.align(4);
        backdrop_disable_dumpfile_vb = Mem.dcStr("ram:backdrop_disable.dat");
        Mem.dcB(0);
    }

    private Level() {
    }

    /**
     * Lvl_InitLevelMods — initialises the level mods data.
     * For now, this is just the sky backdrop override data.
     */
    public static void Lvl_InitLevelMods() {
        if (Mem.l(Lvl_ModPropertiesPtr_l) == 0) {      // tst.l Lvl_ModPropertiesPtr_l ; beq.s Lvl_ClearBackdropDisable
            Lvl_ClearBackdropDisable();
        } else {
            Lvl_FillBackdropDisable();                 // bra.s Lvl_FillBackdropDisable
        }
    }

    /**
     * Lvl_ClearBackdropDisable — clears out the Zone_BackdropDisable_vb data.
     * Preserves registers.
     */
    public static void Lvl_ClearBackdropDisable() {
        // movem.l d0/d1/a0,-(sp) — locaux en Java
        int a0 = Zone_BackdropDisable_vb;              // lea Zone_BackdropDisable_vb,a0
        int d0 = 0;                                    // clr.l d0
        int d1 = ZONE_BACKDROP_DISABLE_SIZE / 4;       // move.w #ZONE_BACKDROP_DISABLE_SIZE/4,d1
        Sys.Sys_MemFillLong(a0, d0, d1);               // bsr Sys_MemFillLong
        // movem.l (sp)+,d0/d1/a0 ; rts
    }

    /**
     * Lvl_FillBackdropDisable — fills the Zone_BackdropDisable_vb data from the
     * loaded properties data. Preserves registers.
     */
    public static void Lvl_FillBackdropDisable() {
        // movem.l d0/a0/a1,-(sp)
        int d0 = ZONE_BACKDROP_DISABLE_SIZE / 16 - 1;  // move.w #ZONE_BACKDROP_DISABLE_SIZE/16-1,d0
        int a0 = Mem.l(Lvl_ModPropertiesPtr_l);        // move.l Lvl_ModPropertiesPtr_l,a0
        int a1 = Zone_BackdropDisable_vb;              // lea Zone_BackdropDisable_vb,a1

        do { // .copy_loop:
            Mem.wl(a1, Mem.l(a0)); a0 += 4; a1 += 4;   // move.l (a0)+,(a1)+
            Mem.wl(a1, Mem.l(a0)); a0 += 4; a1 += 4;   // move.l (a0)+,(a1)+
            Mem.wl(a1, Mem.l(a0)); a0 += 4; a1 += 4;   // move.l (a0)+,(a1)+
            Mem.wl(a1, Mem.l(a0)); a0 += 4; a1 += 4;   // move.l (a0)+,(a1)+
            d0 -= 1;                                   // dbra d0,.copy_loop
        } while ((short) d0 != -1);
        // movem.l (sp)+,d0/a0/a1 ; rts
    }

    /**
     * IFD DEV : Lvl_DumpBackdropDisableData — in devmode, we can dump the
     * current sky disable table to ram disk. We do this so that we can quickly
     * edit the data in the game and incorporate later.
     */
    public static void Lvl_DumpBackdropDisableData() {
        // movem.l d0-d4/a0/a1/a6,-(a7)
        int d1 = backdrop_disable_dumpfile_vb;         // move.l #.backdrop_disable_dumpfile_vb,d1
        int d2 = DosLib.MODE_READWRITE;                // move.l #MODE_READWRITE,d2
        int d0 = DosLib.Open(d1, d2);                  // CALLDOS Open

        d1 = d0;                                       // move.l d0,d1 ; file handle in d1
        if (d1 == 0) {                                 // beq.s .io_error
            return;                                    // .io_error: rts
        }

        int d4 = d0;                                   // move.l d0,d4

        d2 = Zone_BackdropDisable_vb;                  // move.l #Zone_BackdropDisable_vb,d2
        int d3 = ZONE_BACKDROP_DISABLE_SIZE;           // move.l #ZONE_BACKDROP_DISABLE_SIZE,d3

        DosLib.Write(d1, d2, d3);                      // CALLDOS Write

        // move.l d0,d2 ; bytes written - what can we even do if this went wrong?

        d1 = d4;                                       // move.l d4,d1 ; d1 trashed by read
        DosLib.Close(d1);                              // CALLDOS Close
        // movem.l (a7)+,... ; rts
    }
}
