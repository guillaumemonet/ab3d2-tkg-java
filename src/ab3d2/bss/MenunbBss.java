package ab3d2.bss;

import ab3d2.Mem;

/**
 * BSS chip de ab3d2_source/menu/menunb.s nécessaire à c/menu.c (mnu_init).
 * menunb.s (code) reste différé.
 */
public final class MenunbBss {

    /** mnu_screen : ds.b 2*40*512 (fond 4 couleurs, 320x512). */
    public static final int mnu_screen = Mem.alloc(2 * 40 * 512);       // 40960

    /** mnu_morescreen : ds.b 8*40*SCREEN_HEIGHT (8 bitplanes 320x256, effet de feu). */
    public static final int mnu_morescreen = Mem.alloc(8 * 40 * 256);   // 81920

    private MenunbBss() {
    }
}
