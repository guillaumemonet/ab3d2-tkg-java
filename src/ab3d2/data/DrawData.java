package ab3d2.data;

import ab3d2.Assets;
import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/data/draw_data.s
 */
public final class DrawData {

    public static final int draw_TeleportShimmerFXData_vb;
    public static final int draw_WaterFrames_vb;
    public static final int draw_Palette_vw;          // _draw_Palette_vw::

    public static final int draw_EndFont0_vb;
    public static final int draw_CharWidths0_vb;
    public static final int ENDFONT1;
    public static final int CHARWIDTHS1;
    public static final int ENDFONT2;
    public static final int CHARWIDTHS2;

    public static final int draw_FontPtrs_vl;         // _draw_FontPtrs_vl::

    public static final int draw_BorderChars_vb;      // _draw_BorderChars_vb::
    public static final int draw_ScrollChars_vb;      // _draw_ScrollChars_vb::

    /** IFND GEN_GLYPH_DATA : we are using precalculated glyph spacing data (_draw_GlyphSpacing_vb::) */
    public static final int draw_GlyphSpacing_vb;

    public static final int draw_Digits_vb;

    public static final int draw_BackdropImageName_vb;

    public static final int draw_BorderPacked_vb;     // _draw_BorderPacked_vb::

    public static final int draw_Brights_vw;
    public static final int draw_Brights2_vw;
    public static final int willy;
    public static final int willybright;
    public static final int draw_XZAngs_vw;

    /** todo (original) - what is this? */
    public static final int guff;

    /** objdrawhires.s:2569 — ontoscr : REPT 256 dc.l val (val += SCREEN_WIDTH). y → offset écran. */
    public static final int ontoscr;

    static {
        Mem.align(4);
        draw_TeleportShimmerFXData_vb = Assets.incbin("includes/shimmerfile");

        Mem.align(4);
        draw_WaterFrames_vb = Assets.incbin("waterfile");

        Mem.align(4);
        draw_Palette_vw = Assets.incbin("256pal");

        Mem.align(4);
        draw_EndFont0_vb = Assets.incbin("endfont0");
        draw_CharWidths0_vb = Assets.incbin("charwidths0");
        ENDFONT1 = Mem.allocTop();
        Mem.align(4);
        CHARWIDTHS1 = Mem.allocTop();
        ENDFONT2 = Mem.allocTop();
        CHARWIDTHS2 = Mem.allocTop();

        draw_FontPtrs_vl = Mem.dcL(
                draw_EndFont0_vb,
                draw_CharWidths0_vb,
                ENDFONT1, CHARWIDTHS1,
                ENDFONT2, CHARWIDTHS2);

        Mem.align(4);
        draw_BorderChars_vb = Assets.incbin("includes/bordercharsraw");

        Mem.align(4);
        draw_ScrollChars_vb = Assets.incbin("includes/scrollfont");

        // IFND GEN_GLYPH_DATA (non défini → inclus)
        draw_GlyphSpacing_vb = Assets.incbin("includes/glyph_spacing.bin");

        Mem.align(4);
        draw_Digits_vb = Assets.incbin("numbers.inc");

        Mem.align(4);
        draw_BackdropImageName_vb = Mem.dcStr("ab3:includes/rawbackpacked");
        Mem.dcB(0);
        Mem.align(4);

        draw_BorderPacked_vb = Assets.incbin("includes/newborderpacked");
        Mem.alloc(16); // ds.b 16 ; safety for unLha overrun

        Mem.align(4);
        draw_Brights_vw = Mem.dcW(3);
        Mem.dcW(8, 9, 10, 11, 12);
        Mem.dcW(15, 16, 17, 18, 19);
        Mem.dcW(21, 22, 23, 24, 25, 26, 27);
        Mem.dcW(29, 30, 31, 32, 33);
        Mem.dcW(36, 37, 38, 39, 40);
        Mem.dcW(45);

        draw_Brights2_vw = Mem.dcW(3);
        Mem.dcW(12, 11, 10, 9, 8);
        Mem.dcW(19, 18, 17, 16, 15);
        Mem.dcW(27, 26, 25, 24, 23, 22, 21);
        Mem.dcW(33, 32, 31, 30, 29);
        Mem.dcW(40, 39, 38, 37, 36);
        Mem.dcW(45);

        willy = Mem.dcW(0, 0, 0, 0, 0, 0, 0);
        Mem.dcW(5, 5, 5, 5, 5, 5, 5);
        Mem.dcW(10, 10, 10, 10, 10, 10, 10);
        Mem.dcW(15, 15, 15, 15, 15, 15, 15);
        Mem.dcW(20, 20, 20, 20, 20, 20, 20);
        Mem.dcW(25, 25, 25, 25, 25, 25, 25);
        Mem.dcW(30, 30, 30, 30, 30, 30, 30);

        willybright = Mem.dcW(30, 30, 30, 30, 30, 30, 30);
        Mem.dcW(30, 20, 20, 20, 20, 20, 30);
        Mem.dcW(30, 20, 6, 3, 6, 20, 30);
        Mem.dcW(30, 20, 6, 0, 6, 20, 30);
        Mem.dcW(30, 20, 6, 6, 6, 20, 30);
        Mem.dcW(30, 20, 20, 20, 20, 20, 30);
        Mem.dcW(30, 30, 30, 30, 30, 30, 30);

        draw_XZAngs_vw = Mem.dcW(0, 23, 10, 20, 16, 16, 20, 10);
        Mem.dcW(23, 0, 20, -10, 16, -16, 10, -20);
        Mem.dcW(0, -23, -10, -20, -16, -16, -20, -10);
        Mem.dcW(-23, 0, -20, 10, -16, 16, -10, 20);

        guff = Assets.incbin("includes/guff");

        // objdrawhires.s:2569 — ontoscr : 256 longs y*SCREEN_WIDTH
        Mem.align(4);
        ontoscr = Mem.allocTop();
        int v = 0;
        for (int i = 0; i < 256; i++) {
            Mem.dcL(v);
            v += ab3d2.Hires.SCREEN_WIDTH;
        }
    }

    private DrawData() {
    }
}
