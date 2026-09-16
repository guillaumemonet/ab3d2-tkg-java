package ab3d2;

/**
 * Données embarquées de ab3d2_source/newanims.s (traduction partielle ; complétée
 * au fil de la traduction du code de newanims.s). Voir Newanims pour le code.
 */
public final class NewanimsData {

    // ---- newanims.s:3-8 — byte data ----
    private static final int _a0 = Mem.align(2);
    public static final int Anim_LightingEnabled_b = Mem.dcB(0xFF); // _Anim_LightingEnabled_b::
    public static final int anim_LiftAtTop_b = Mem.dcB(0);
    public static final int anim_LiftAtBottom_b = Mem.dcB(0);
    public static final int anim_DoorOpen_b = Mem.dcB(0);
    public static final int anim_DoorClosed_b = Mem.dcB(0);

    // ---- newanims.s:603 ----
    private static final int _a1 = Mem.align(2);
    public static final int anim_ExpRadius_w = Mem.dcW(0);

    // ---- newanims.s:1761-1766 ----
    private static final int _a3 = Mem.align(4);
    public static final int tempxoff = Mem.dcW(0);
    public static final int tempzoff = Mem.dcW(0);
    public static final int tempRoompt = Mem.dcL(0);
    public static final int bulyspd = Mem.dcW(0);
    public static final int closedist = Mem.dcW(0);

    // ---- newanims.s:2691 — (bloc données handler de balles) ----
    private static final int _a2 = Mem.align(4);
    public static final int tmpangpos = Mem.dcL(0);

    // ---- newanims.s:1963-1964, 2074 ----
    private static final int _a4 = Mem.align(2);
    public static final int timeout = Mem.dcW(0);            // écrit en byte
    public static final int anim_Brightness_w = Mem.dcW(0);
    public static final int BLOODYGREATBOMB = Mem.dcW(0);    // écrit en byte

    // ---- newanims.s:2686-2694 ----
    private static final int _a5 = Mem.align(2);
    public static final int MOVING = Mem.dcW(0);
    private static final int _a6 = Mem.align(4);
    public static final int tmpnewx = Mem.dcL(0);
    public static final int tmpnewz = Mem.dcL(0);
    public static final int hithit = Mem.dcL(0);
    public static final int sqrnum = Mem.dcL(0);
    public static final int allbars = Mem.dcL(0);
    public static final int backrout = Mem.alloc(800);      // ds.b 800
    public static final int NUMTOCHECK = Mem.dcW(0);
    public static final int Zone_Count_w = Mem.dcW(0);      // newanims.s:1771 dc.w 0

    // ---- newanims.s:1952-1954 ----
    public static final int RipTear = Mem.dcL(256 * 17 * 65536);
    public static final int otherrip = Mem.dcL(256 * 18 * 65536);
    public static final int Conditions = Mem.dcL(0);

    // ---- newanims.s:10-11 — EQU ----
    public static final int BRIGHT_ANIM_LIST_END = -1;
    public static final int BRIGHT_ANIM_END = 999;

    // ---- newanims.s:14-98 — tables d'animation de brightness ----
    public static final int anim_BrightessAnimPtrs_vl;
    public static final int anim_BrightnessAnimStartPtrs_vl;
    public static final int anim_BrightPulse1_vw;
    public static final int anim_BrightPulse2_vw;
    public static final int anim_BrightPulse3_vw;
    public static final int anim_BrightPulse4_vw;
    public static final int anim_BrightPulse5_vw;
    public static final int anim_BrightFlicker1_vw;
    public static final int anim_BrightFlicker2_vw;

    static {
        Mem.align(4);
        anim_BrightPulse1_vw = emit(seq(1, 20), seq(20, 1), end());
        anim_BrightPulse2_vw = emit(seq(9, 20), seq(20, 1), seq(1, 8), end());
        anim_BrightPulse3_vw = emit(seq(17, 20), seq(20, 1), seq(1, 16), end());
        anim_BrightPulse4_vw = emit(seq(16, 1), seq(1, 20), seq(20, 17), end());
        anim_BrightPulse5_vw = emit(seq(8, 1), seq(1, 20), seq(20, 9), end());
        anim_BrightFlicker1_vw = emit(rep(20, 20), new int[]{1}, rep(30, 20), new int[]{1}, rep(5, 20), new int[]{1}, end());
        anim_BrightFlicker2_vw = emit(new int[]{
                -10, -9, -6, -10, -6, -5, -5, -7, -5, -10, -9, -8, -7, -5, -5, -5, -5,
                -5, -5, -5, -5, -6, -7, -8, -9, -5, -10, -9, -10, -6, -5, -5, -5, -5, -5,
                -5, -5}, end());

        Mem.align(4);
        anim_BrightessAnimPtrs_vl = Mem.dcL(
                anim_BrightPulse1_vw, anim_BrightPulse2_vw, anim_BrightPulse3_vw,
                anim_BrightPulse4_vw, anim_BrightPulse5_vw,
                anim_BrightFlicker1_vw, anim_BrightFlicker2_vw,
                BRIGHT_ANIM_LIST_END);

        anim_BrightnessAnimStartPtrs_vl = Mem.dcL(
                anim_BrightPulse1_vw, anim_BrightPulse2_vw, anim_BrightPulse3_vw,
                anim_BrightPulse4_vw, anim_BrightPulse5_vw,
                anim_BrightFlicker1_vw, anim_BrightFlicker2_vw);
    }

    private static int[] seq(int from, int to) {
        int n = Math.abs(to - from) + 1;
        int step = to >= from ? 1 : -1;
        int[] r = new int[n];
        for (int i = 0, v = from; i < n; i++, v += step) r[i] = v;
        return r;
    }

    private static int[] rep(int n, int v) {
        int[] r = new int[n];
        java.util.Arrays.fill(r, v);
        return r;
    }

    private static int[] end() {
        return new int[]{BRIGHT_ANIM_END};
    }

    private static int emit(int[]... parts) {
        int base = Mem.allocTop();
        for (int[] p : parts) for (int v : p) Mem.dcW(v);
        return base;
    }

    private NewanimsData() {
    }
}
