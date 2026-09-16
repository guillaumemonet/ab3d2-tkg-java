package ab3d2.bss;

import ab3d2.Mem;
import ab3d2.SystemInc;

/**
 * Traduction littérale de ab3d2_source/bss/system_bss.s
 *
 * Chaque label devient une adresse (int) allouée séquentiellement dans Mem —
 * les initialisations de champs s'exécutent dans l'ordre de déclaration, donc
 * la disposition mémoire est identique à celle de l'assembleur (contiguë,
 * directives align traduites par Mem.align). Voir Bss.init() pour l'ordre
 * d'inclusion des sections.
 *
 * NB: xref _DOSBase — _DOSBase est externe (auto-open C), pas alloué ici.
 */
public final class SystemBss {

    private static final int _align0 = Mem.align(4);

    // System resource pointers
    public static final int _GfxBase = Mem.alloc(4);
    public static final int _IntuitionBase = Mem.alloc(4);
    public static final int _MiscBase = Mem.alloc(4);
    public static final int _PotgoBase = Mem.alloc(4);
    public static final int _TimerBase = Mem.alloc(4);

    // Chunks of statically allocated data for various calculations
    public static final int Sys_Workspace_vl = Mem.alloc(4 * 8192);
    public static final int Sys_SerialBuffer_vl = Mem.alloc(4 * 2000);
    public static final int sys_OldWindowPtr = Mem.alloc(4);

    public static final int sys_RecoveryStack = Mem.alloc(4);
    public static final int sys_ErrorBuffer_vb = Mem.alloc(256);       // 255
    public static final int sys_ErrorHeight_b = Mem.alloc(2);          // ds.w 1 ! Also signals that an error occured

    // System FPS
    private static final int _align1 = Mem.align(4);

    // Keep this pair together
    public static final int Sys_FrameTimeECV_q = Mem.alloc(4 * 2);     // EClock of Lap
    public static final int Sys_PrevFrameTimeECV_q = Mem.alloc(4 * 2); // EClock of last Lap

    public static final int Sys_FrameTimes_vl = Mem.alloc(4 * 8);      // last 8 frame times, in ms
    public static final int Sys_FrameNumber_l = Mem.alloc(4);          // monotonically increasing frame number
    public static final int Sys_ECVToMsFactor_l = Mem.alloc(4);        // factor for converting EClock diffs to ms

    // Keep these values together
    public static final int Sys_FPSIntAvg_w = Mem.alloc(2);
    public static final int Sys_FPSFracAvg_w = Mem.alloc(2);
    public static final int Sys_FPSLimit_w = Mem.alloc(2);

    private static final int _align2 = Mem.align(4);
    public static final int sys_TimerRequest = Mem.alloc(SystemInc.IOTV_SIZE); // TimeRequest structure

    public static final int Sys_Move16_b = Mem.alloc(1);     // Set if we have move16 available (060, 040)
    public static final int Sys_FPU_b = Mem.alloc(1);        // Set if we have FPU available
    public static final int Sys_CPU_68060_b = Mem.alloc(1);  // Set if we have a 68060 specifically
    public static final int Sys_C2P_Akiko_b = Mem.alloc(1);  // Set if we have Akiko available
    public static final int Sys_CPU_68030_b = Mem.alloc(1);  // Set if we have 68030 specifically

    private static final int _align3 = Mem.align(4);

    private SystemBss() {
    }
}
