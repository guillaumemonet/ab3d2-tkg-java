package ab3d2.data;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/data/system_data.s
 *
 * Statically initialised (non-zero) data.
 * Les macros DOSNAME/MISCNAME/POTGONAME/INTNAME/GRAFNAME émettent les noms
 * de bibliothèques AmigaOS terminés par 0.
 */
public final class SystemData {

    public static final int DosName;
    public static final int MiscResourceName;
    public static final int PotgoResourceName;
    public static final int IntuitionName;
    public static final int GraphicsName;
    public static final int TimerName;

    public static final int TempMessageBuffer_vb;

    public static final int sys_TimerFlag_l;

    public static final int INTUITION_REV = 31; // v1.1

    static {
        Mem.align(4);
        // Library and Resource Names
        DosName = Mem.dcStr("dos.library");
        Mem.dcB(0);
        MiscResourceName = Mem.dcStr("misc.resource");
        Mem.dcB(0);
        PotgoResourceName = Mem.dcStr("potgo.resource");
        Mem.dcB(0);
        IntuitionName = Mem.dcStr("intuition.library");
        Mem.dcB(0);
        GraphicsName = Mem.dcStr("graphics.library");
        Mem.dcB(0);
        TimerName = Mem.dcStr("timer.device");
        Mem.dcB(0);

        TempMessageBuffer_vb = Mem.dcbB(160, 32); // dcb.b 160,32

        Mem.align(4);
        sys_TimerFlag_l = Mem.dcL(-1);
    }

    private SystemData() {
    }
}
