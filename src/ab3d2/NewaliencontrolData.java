package ab3d2;

/**
 * Données de ab3d2_source/newaliencontrol.s — TRADUCTION PARTIELLE
 * (blocs requis par ai.s ; le code suivra avec newaliencontrol.s).
 */
public final class NewaliencontrolData {

    // ---- newaliencontrol.s:2-5 ----
    private static final int _a0 = Mem.align(4);
    public static final int AlienAnimPtr_l = Mem.dcL(0);
    public static final int ALIENBRIGHT = Mem.dcW(0);

    // ---- newaliencontrol.s:80-85 ----
    public static final int ALIENECHO = Mem.dcW(0);

    /** diststowall : table {AwayFromWall, ExtLen} indexée par Girth*4. */
    public static final int diststowall = Mem.dcW(0, 40);
    private static final int _dtw1 = Mem.dcW(1, 80);
    private static final int _dtw2 = Mem.dcW(2, 160);

    // ---- newaliencontrol.s:525-526 ----
    private static final int _a2 = Mem.align(4);
    public static final int obj_ConsumablePtr_l = Mem.dcL(0);
    public static final int obj_ItemsPtr_l = Mem.dcL(0);

    // ---- newaliencontrol.s:806 ----
    public static final int obj_StatPtr_l = Mem.dcL(0);

    // ---- newaliencontrol.s:936-937 ----
    public static final int THISPLRxoff = Mem.dcW(0);
    public static final int THISPLRzoff = Mem.dcW(0);

    // ---- newaliencontrol.s:996-1002 ----
    public static final int deadframe = Mem.dcL(0);
    public static final int screamsound = Mem.dcW(0);
    public static final int nasheight = Mem.dcW(0);
    public static final int tempcos = Mem.dcW(0);
    public static final int tempsin = Mem.dcW(0);
    public static final int tempx = Mem.dcW(0);
    public static final int tempz = Mem.dcW(0);

    // ---- newaliencontrol.s:1033-1037 ----
    public static final int bbbb = Mem.dcW(0);
    public static final int tsx = Mem.dcW(0);
    public static final int tsz = Mem.dcW(0);
    public static final int fsx = Mem.dcW(0);
    public static final int fsz = Mem.dcW(0);

    // ---- newaliencontrol.s:1152-1153 ----
    public static final int futurex = Mem.dcW(0);
    public static final int futurez = Mem.dcW(0);

    // ---- newaliencontrol.s:1508-1517 ----
    private static final int _a1 = Mem.align(4);
    public static final int backupZonePtr_l = Mem.dcL(0);
    public static final int SHOTYOFF = Mem.dcL(0);
    public static final int SHOTTYPE = Mem.dcW(0);
    public static final int SHOTPOWER = Mem.dcW(0);
    public static final int SHOTSPEED = Mem.dcW(0);
    public static final int SHOTOFFMULT = Mem.dcW(0);
    public static final int SHOTSHIFT = Mem.dcW(0);
    public static final int SHOTINTOP = Mem.dcW(0);

    private NewaliencontrolData() {
    }
}
