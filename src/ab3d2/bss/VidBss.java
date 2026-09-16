package ab3d2.bss;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/vid_bss.s
 */
public final class VidBss {

    private static final int _align0 = Mem.align(4);

    public static final int Vid_C2PSetParamsPtr_l = Mem.alloc(4);   // Points to 1x1 C2P Initialisation for Fullscreen
    public static final int Vid_C2PConvertPtr_l = Mem.alloc(4);     // Points to 1x1 C2P Conversion for Fullscreen

    public static final int Vid_FastBufferPtr_l = Mem.alloc(4);     // aligned address
    public static final int Vid_FastBufferAllocPtr_l = Mem.alloc(4); // allocated address
    public static final int Vid_Screen1Ptr_l = Mem.alloc(4);
    public static final int Vid_Screen2Ptr_l = Mem.alloc(4);
    public static final int Vid_DrawScreenPtr_l = Mem.alloc(4);

    public static final int Vid_DisplayScreenPtr_l = Mem.alloc(4);

    public static final int Vid_ScreenBuffers_vl = Mem.alloc(4 * 2);
    public static final int Vid_MainScreen_l = Mem.alloc(4);
    public static final int Vid_MyRaster0_l = Mem.alloc(4);
    public static final int Vid_MyRaster1_l = Mem.alloc(4);
    /** this message port receives messages when the current screen has been scanned out */
    public static final int Vid_DisplayMsgPort_l = Mem.alloc(4);
    public static final int vid_MainWindow_l = Mem.alloc(4);
    // vid_SafeMsgPort_l ds.l 1 (commenté dans l'original)

    // Palette data to be submitted to LoadRGB32 calls
    public static final int Vid_LoadRGB32Struct_vl = Mem.alloc(4);
    public static final int vid_LoadRGB32Data_vl = Mem.alloc(4 * 256 * 3); // 32-bit R, B, G
    public static final int vid_LoadRGB32End_l = Mem.alloc(4);

    /**
     * Index (0/1) of current screen buffer displayed.
     * FIXME: unify the buffer index handling with Vid_DrawScreenPtr_l/Vid_DisplayScreenPtr_l
     */
    public static final int Vid_ScreenBufferIndex_w = Mem.alloc(2);

    /** Letter box rendering, height of black border */
    public static final int Vid_LetterBoxMarginHeight_w = Mem.alloc(2);

    public static final int Vid_FullScreen_b = Mem.alloc(1);
    public static final int Vid_FullScreenTemp_b = Mem.alloc(1);
    public static final int Vid_DoubleHeight_b = Mem.alloc(1);   // Double Height Pixel Mode
    public static final int Vid_DoubleWidth_b = Mem.alloc(1);    // Double Width Pixel Mode
    public static final int Vid_WaitForDisplayMsg_b = Mem.alloc(1);
    public static final int Vid_ResolutionOption_b = Mem.alloc(1); // cycles between pixel modes

    // Menu
    public static final int mnu_palette = Mem.alloc(4 * 256);    // 4byte per pixel, 24bit used

    /** Global C de screen.c : BOOL Vid_isRTG (vrai si mode CyberGfx/RTG). LONG=4o. */
    public static final int Vid_isRTG = Mem.alloc(4);

    /** Global C de screen.c : WORD Vid_ScreenHeight (hauteur écran courante). */
    public static final int Vid_ScreenHeight = Mem.alloc(2);

    /** Global C de screen.c : WORD Vid_ScreenWidth (largeur écran courante). */
    public static final int Vid_ScreenWidth = Mem.alloc(2);

    /** Global C de screen.c : ULONG Vid_ScreenMode (ModeID de l'écran). LONG=4o. */
    public static final int Vid_ScreenMode = Mem.alloc(4);

    private VidBss() {
    }
}
