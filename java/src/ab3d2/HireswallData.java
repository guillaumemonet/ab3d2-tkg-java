package ab3d2;

/**
 * Données de tête de ab3d2_source/hireswall.s — TRADUCTION PARTIELLE
 * (lignes 1-60 ; le reste suivra avec la traduction du code de hireswall.s).
 *
 * "todo - these can probably be BSS'd" (commentaire d'origine)
 */
public final class HireswallData {

    private static final int _a0 = Mem.align(4);

    // Beware - these are unions of word|long. Do not separate!
    public static final int Draw_LeftClip_l = Mem.dcW(0);   // long (couvre les 2 mots suivants)
    public static final int Draw_LeftClip_w = Mem.dcW(0);   // lsw

    // Beware - these are unions of word|long. Do not separate!
    public static final int Draw_RightClip_l = Mem.dcW(0);  // long
    public static final int Draw_RightClip_w = Mem.dcW(0);  // lsw

    public static final int Draw_LeftClipAndLast_w = Mem.dcW(0);

    // Beware - these are unions of word|byte. Do not separate!
    public static final int draw_StripData_w = Mem.dcB(0);  // word (couvre les 2 octets)
    public static final int draw_StripData_b = Mem.dcB(0);  // lsb

    // TODO (original) - this buffer is just a lookup table of y * SCREEN_WIDTH.
    private static final int _a1 = Mem.align(4);
    public static final int draw_LineOffsetBuffer_vl;

    private static final int _a2;
    public static final int Draw_PointBrightsPtr_l;
    public static final int draw_WallTextureHeightMask_w;
    public static final int draw_WallTextureHeightShift_w;
    public static final int draw_WallTextureWidthMask_w;

    // TODO (original) - these probably belong somewhere else
    public static final int Vis_SinVal_w;  // somewhat universal
    public static final int Vis_CosVal_w;  // somewhat universal

    public static final int draw_TopClip_w;
    public static final int draw_BottomClip_w;

    private static final int _a3;
    public static final int draw_TopOfWall_l;
    public static final int draw_BottomOfWall_l;

    public static final int draw_LBR_w;                  // todo - define
    public static final int draw_TLBR_w;                 // todo - define
    public static final int draw_LeftWallBright_w;
    public static final int draw_RightWallBright_w;
    public static final int draw_LeftWallTopBright_w;
    public static final int draw_RightWallTopBright_w;
    public static final int draw_StripTop_w;
    public static final int draw_StripBottom_w;

    public static final int draw_WallLastStripX_w;

    public static final int draw_FromTile_w;             // declared long, all but one accesses as word
    public static final int draw_AngleBright_w;

    public static final int draw_WallIterations_w;
    public static final int draw_MultCount_w;

    // ---- hireswall.s:62 ---- (accessed as byte all over the code)
    public static final int Draw_GoodRender_b;           // DCLC dc.w $ff00

    // ---- hireswall.s:175-180 ----
    public static final int draw_IterationTable_vw;      // incbin "includes/iterfile"
    public static final int draw_BrightnessScaleTable_vw; // SCALE macro (128 words)

    // ---- hireswall.s:1921-1940 ----
    public static final int draw_TimesLargeThru_vl;      // REPT 80 : dc.l 104*4*(i+1)
    public static final int draw_TotalYOffset_w;
    public static final int draw_WallYOffset_w;
    public static final int draw_WallBrightOffset_w;
    public static final int draw_WallLeftPoint_w;
    public static final int draw_WallRightPoint_w;
    public static final int draw_WhichPBR_w;             // accessed as byte (PBR = point brightness?)
    public static final int draw_WhichLeftPoint_w;       // written as byte
    public static final int draw_WhichRightPoint_w;      // written as byte
    public static final int draw_OtherZone_w;            // written as byte

    /**
     * SCALE (hireswall.s:64-146) : 81 mots. 64*0, puis les paires 64*n pour
     * n=1..31, puis 18 répétitions supplémentaires de 64*31 (soit 20 au total).
     */
    private static final int[] SCALE = {
            64 * 0,
            64 * 1, 64 * 1, 64 * 2, 64 * 2, 64 * 3, 64 * 3, 64 * 4, 64 * 4, 64 * 5, 64 * 5,
            64 * 6, 64 * 6, 64 * 7, 64 * 7, 64 * 8, 64 * 8, 64 * 9, 64 * 9, 64 * 10, 64 * 10,
            64 * 11, 64 * 11, 64 * 12, 64 * 12, 64 * 13, 64 * 13, 64 * 14, 64 * 14, 64 * 15, 64 * 15,
            64 * 16, 64 * 16, 64 * 17, 64 * 17, 64 * 18, 64 * 18, 64 * 19, 64 * 19, 64 * 20, 64 * 20,
            64 * 21, 64 * 21, 64 * 22, 64 * 22, 64 * 23, 64 * 23, 64 * 24, 64 * 24, 64 * 25, 64 * 25,
            64 * 26, 64 * 26, 64 * 27, 64 * 27, 64 * 28, 64 * 28, 64 * 29, 64 * 29, 64 * 30, 64 * 30,
            64 * 31, 64 * 31,
            64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31,
            64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31, 64 * 31
    };

    // ---- hireswall.s:1412-1422 ----
    // ATTENTION: for some reason the order of these variables is important
    // There's code that expects these in the right order to allow for movem
    private static final int _a4;
    public static final int TOTHEMIDDLE;
    public static final int Vid_BottomY_w;   // also accessed as long
    public static final int Vid_CentreY_w;
    public static final int TOPOFFSET;
    public static final int BIGMIDDLEY;
    public static final int SMIDDLEY;
    public static final int STOPOFFSET;
    public static final int SBIGMIDDLEY;     // renderbuffer offset to middle line

    static {
        // REPT 256 : dc.l val ; val += SCREEN_WIDTH
        draw_LineOffsetBuffer_vl = Mem.allocTop();
        int val = 0;
        for (int i = 0; i < 256; i++) {
            Mem.dcL(val);
            val += Hires.SCREEN_WIDTH;
        }

        _a2 = Mem.align(4);
        Draw_PointBrightsPtr_l = Mem.dcL(0);
        draw_WallTextureHeightMask_w = Mem.dcW(0);
        draw_WallTextureHeightShift_w = Mem.dcW(0);
        draw_WallTextureWidthMask_w = Mem.dcW(0);

        Vis_SinVal_w = Mem.dcW(0);
        Vis_CosVal_w = Mem.dcW(0);

        draw_TopClip_w = Mem.dcW(0);
        draw_BottomClip_w = Mem.dcW(0);

        _a3 = Mem.align(4);
        draw_TopOfWall_l = Mem.dcL(0);
        draw_BottomOfWall_l = Mem.dcL(0);

        draw_LBR_w = Mem.dcW(0);
        draw_TLBR_w = Mem.dcW(0);
        draw_LeftWallBright_w = Mem.dcW(0);
        draw_RightWallBright_w = Mem.dcW(0);
        draw_LeftWallTopBright_w = Mem.dcW(0);
        draw_RightWallTopBright_w = Mem.dcW(0);
        draw_StripTop_w = Mem.dcW(0);
        draw_StripBottom_w = Mem.dcW(0);

        // middleline: dc.w 0 ; unused (commenté dans l'original)
        draw_WallLastStripX_w = Mem.dcW(0);

        draw_FromTile_w = Mem.dcL(0);
        draw_AngleBright_w = Mem.dcW(0);

        draw_WallIterations_w = Mem.dcW(0);
        draw_MultCount_w = Mem.dcW(0);

        // hireswall.s:1412-1422
        _a4 = Mem.align(4);
        TOTHEMIDDLE = Mem.dcW(0);
        Vid_BottomY_w = Mem.dcW(0);
        Vid_CentreY_w = Mem.dcW(Hires.FS_HEIGHT / 2);
        TOPOFFSET = Mem.dcW(0);
        BIGMIDDLEY = Mem.dcL(Hires.SCREEN_WIDTH * Hires.FS_HEIGHT / 2);
        SMIDDLEY = Mem.dcW(Hires.FS_HEIGHT / 2);
        STOPOFFSET = Mem.dcW(0);
        SBIGMIDDLEY = Mem.dcL(Hires.SCREEN_WIDTH * Hires.FS_HEIGHT / 2);

        // hireswall.s:62 — Draw_GoodRender_b dc.w $ff00 (accédé en byte)
        Draw_GoodRender_b = Mem.dcW(0xff00);

        // hireswall.s:175-176 — draw_IterationTable_vw incbin "includes/iterfile"
        Mem.align(4);
        draw_IterationTable_vw = Assets.incbin("includes/iterfile");

        // hireswall.s:179-180 — draw_BrightnessScaleTable_vw SCALE
        draw_BrightnessScaleTable_vw = Mem.allocTop();
        for (int s : SCALE) {
            Mem.dcW(s);
        }

        // hireswall.s:1921-1926 — draw_TimesLargeThru_vl REPT 80 (val=104*4, +104*4)
        draw_TimesLargeThru_vl = Mem.allocTop();
        int v = 104 * 4;
        for (int i = 0; i < 80; i++) {
            Mem.dcL(v);
            v += 104 * 4;
        }

        draw_TotalYOffset_w = Mem.dcW(0);
        draw_WallYOffset_w = Mem.dcW(0);

        // hireswall.s:1934-1940 — Wall polygon
        draw_WallBrightOffset_w = Mem.dcW(0);
        draw_WallLeftPoint_w = Mem.dcW(0);
        draw_WallRightPoint_w = Mem.dcW(0);
        draw_WhichPBR_w = Mem.dcW(0);
        draw_WhichLeftPoint_w = Mem.dcW(0);
        draw_WhichRightPoint_w = Mem.dcW(0);
        draw_OtherZone_w = Mem.dcW(0);
    }

    private HireswallData() {
    }
}
