package ab3d2.modules;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/modules/dev_macros.i
 *
 * "Developer mode instrumentation"
 *
 * Les EQU DEV_* sont des numéros de bit dans Dev_DebugFlags_l (ULONG).
 * Les macros DEV_CHECK_SET/CLR, DEV_ENABLE/DISABLE/TOGGLE testent/positionnent
 * ces bits sur l'octet addr+3-(bit>>3) (long big-endian).
 *
 * En build release, toutes ces macros sont vides ; en build DEV elles
 * instrumentent le code. Le port garde les helpers actifs — l'équivalent du
 * build DEV — et les points d'usage (DEV_INC, CALLDEV...) seront traduits
 * en appels vers ab3d2.modules.DevInst (modules/dev_inst.s).
 */
public final class DevMacros {

    // DEVMODE INSTRUMENTATION MACROS
    public static final int DEV_SKIP_FLATS = 0;
    public static final int DEV_SKIP_SIMPLE_WALLS = 1;
    public static final int DEV_SKIP_SHADED_WALLS = 2;
    public static final int DEV_SKIP_BITMAPS = 3;
    public static final int DEV_SKIP_GLARE_BITMAPS = 4;
    public static final int DEV_SKIP_ADDITIVE_BITMAPS = 5;
    public static final int DEV_SKIP_LIGHTSOURCED_BITMAPS = 6;
    public static final int DEV_SKIP_POLYGON_MODELS = 7;
    public static final int DEV_SKIP_FASTBUFFER_CLEAR = 8;
    public static final int DEV_SKIP_AI_ATTACK = 9;
    public static final int DEV_SKIP_TIMEGRAPH = 10;
    public static final int DEV_SKIP_LIGHTING = 11;
    public static final int DEV_SKIP_DUMP_BG_DISABLE = 12;
    public static final int DEV_ZONE_TRACE = 13;
    public static final int DEV_SKIP_PVS_AMEND = 14;
    public static final int DEV_SKIP_EDGE_PVS = 15;

    // Skip rendering the overlay completely
    public static final int DEV_SKIP_OVERLAY = 31;

    // When any of the level geometry is skipped, we need to make sure the fast buffer gets cleared
    public static final int DEV_CLEAR_FASTBUFFER_MASK =
            (1 << DEV_SKIP_FLATS) | (1 << DEV_SKIP_SIMPLE_WALLS) | (1 << DEV_SKIP_SHADED_WALLS);

    // IFD DEV
    public static final int DEV_GRAPH_BUFFER_DIM = 6;
    public static final int DEV_GRAPH_BUFFER_SIZE = 64;
    public static final int DEV_GRAPH_BUFFER_MASK = 63;
    public static final int DEV_GRAPH_DRAW_TIME_COLOUR = 255;
    public static final int DEV_GRAPH_OBJECT_COUNT_COLOUR = 31;

    private DevMacros() {
    }

    /**
     * DEV_CHECK_SET/DEV_SNE : btst.b #(DEV_x)&7,flagsAddr+3-(DEV_x>>3)
     * Renvoie true si le bit est positionné (l'appelant fait le bne/sne).
     */
    public static boolean DEV_TEST(int flagsAddr, int bit) {
        return (Mem.ub(flagsAddr + 3 - (bit >> 3)) & (1 << (bit & 7))) != 0;
    }

    /** DEV_ENABLE : bset.b */
    public static void DEV_ENABLE(int flagsAddr, int bit) {
        int a = flagsAddr + 3 - (bit >> 3);
        Mem.wb(a, Mem.ub(a) | (1 << (bit & 7)));
    }

    /** DEV_DISABLE : bclr.b */
    public static void DEV_DISABLE(int flagsAddr, int bit) {
        int a = flagsAddr + 3 - (bit >> 3);
        Mem.wb(a, Mem.ub(a) & ~(1 << (bit & 7)));
    }

    /** DEV_TOGGLE : bchg.b */
    public static void DEV_TOGGLE(int flagsAddr, int bit) {
        int a = flagsAddr + 3 - (bit >> 3);
        Mem.wb(a, Mem.ub(a) ^ (1 << (bit & 7)));
    }

    // DEV_INC/DEV_DEC/DEV_INCN : addq/subq sur un compteur dev_xxx — inlinés
    //   aux points d'usage (Mem.ww(addr, Mem.w(addr)+1) etc.).
    // DEV_SAVE/DEV_RESTORE : movem — sans objet en Java.
    // DEV_CHECK_KEY : tst.b key(a5) / clr.b / DEV_TOGGLE — inliné au point d'usage.
    // DEV_CHECK_DIVISOR : min/max dans dev_Reserved4_w/dev_Reserved5_w — inliné.
    // DEV_ZDBG \1 : si DEV_ZONE_TRACE actif, sauvegarde les registres puis CALLC \1
    //   (zone_debug.c). DEV_ZDBG_CLIP : move.w #\1,SetClipStage_w.
}
