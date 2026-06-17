package ab3d2;

/**
 * Données embarquées de ab3d2_source/objdrawhires.s (interspersées dans le code
 * de rendu d'objets). Voir Objdrawhires pour le code.
 */
public final class ObjdrawhiresData {

    // ---- objdrawhires.s:73-75 ----
    private static final int _a0 = Mem.align(4);
    public static final int draw_TopY_3D_l = Mem.dcL(-100 * 1024);
    public static final int draw_BottomY_3D_l = Mem.dcL(1 * 1024);

    // ---- objdrawhires.s:647-679 — draw_ObjScaleCols_vw ----
    // dcb.w 1,64*0 ; dcb.w 2,64*n (n=1..30) ; dcb.w 20,64*31  → 1 + 2*30 + 20 = 81 mots
    private static final int _a1 = Mem.align(4);
    public static final int draw_ObjScaleCols_vw;

    // ---- objdrawhires.s:681-685 ----
    public static final int draw_BasePalPtr_l;
    public static final int draw_WhichLightPal_b;
    public static final int draw_FlipIt_b;
    public static final int draw_LightIt_b;
    public static final int draw_Additive_b;

    // ---- objdrawhires.s:1377 ----
    public static final int draw_TempPtr_l;

    // ---- objdrawhires.s:2207-2210 (align 4) ----
    public static final int polybright;
    public static final int firstpt;
    public static final int PolyAng;

    // ---- objdrawhires.s:2733-2737 ----
    public static final int tstdca;
    public static final int offtopby;
    public static final int LinesPtr;
    public static final int PtsPtr;

    // ---- objdrawhires.s:3081-3088 (EVEN) ----
    // draw_PreGouraud_b écrit en word (positionne aussi draw_Gouraud_b adjacent) ; idem Holes.
    public static final int draw_PreGouraud_b;
    public static final int draw_Gouraud_b;
    public static final int draw_PreHoles_b;
    public static final int draw_Holes_b;

    // ---- objdrawhires.s:2726, 2889, 3073 — ontoscrGL/ontoscrg/ontoscrh (256 longs y*SCREEN_WIDTH) ----
    public static final int ontoscrGL;
    public static final int ontoscrg;
    public static final int ontoscrh;

    // objdrawhires.s:2215 — GUARDBAND EQU
    public static final int GUARDBAND = 8191;

    static {
        draw_ObjScaleCols_vw = Mem.allocTop();
        Mem.dcW(64 * 0);                                // dcb.w 1,64*0
        for (int n = 1; n <= 30; n++) {                 // dcb.w 2,64*n
            Mem.dcW(64 * n);
            Mem.dcW(64 * n);
        }
        for (int i = 0; i < 20; i++) {                  // dcb.w 20,64*31
            Mem.dcW(64 * 31);
        }

        draw_BasePalPtr_l = Mem.dcL(0);
        draw_WhichLightPal_b = Mem.dcB(0);
        draw_FlipIt_b = Mem.dcB(0);
        draw_LightIt_b = Mem.dcB(0);
        draw_Additive_b = Mem.dcB(0);

        Mem.align(4);
        draw_TempPtr_l = Mem.dcL(0);

        Mem.align(4);
        polybright = Mem.dcL(0);
        firstpt = Mem.dcW(0);
        PolyAng = Mem.dcW(0);

        Mem.align(4);
        tstdca = Mem.dcL(0);
        Mem.dcW(0);                                     // dc.w 0 anonyme
        offtopby = Mem.dcW(0);
        LinesPtr = Mem.dcL(0);
        PtsPtr = Mem.dcL(0);

        Mem.align(2);                                   // EVEN
        draw_PreGouraud_b = Mem.dcB(0);
        draw_Gouraud_b = Mem.dcB(0);
        draw_PreHoles_b = Mem.dcB(0);
        draw_Holes_b = Mem.dcB(0);

        Mem.align(4);
        ontoscrGL = newYOffsetTable();
        ontoscrg = newYOffsetTable();
        ontoscrh = newYOffsetTable();
    }

    private static int newYOffsetTable() {
        int base = Mem.allocTop();
        int v = 0;
        for (int i = 0; i < 256; i++) {                 // REPT 256 : dc.l val ; val += SCREEN_WIDTH
            Mem.dcL(v);
            v += ab3d2.Hires.SCREEN_WIDTH;
        }
        return base;
    }

    private ObjdrawhiresData() {
    }
}
