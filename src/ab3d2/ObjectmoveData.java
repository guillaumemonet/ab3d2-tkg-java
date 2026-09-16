package ab3d2;

/**
 * Données de ab3d2_source/objectmove.s — TRADUCTION PARTIELLE
 * (blocs complets requis par ai.s ; le code suivra avec objectmove.s).
 */
public final class ObjectmoveData {

    // ---- objectmove.s:1-9 ----
    private static final int _a0 = Mem.align(4);
    public static final int obj_RoomPathPtr_l = Mem.dcL(0);
    public static final int Obj_ExtLen_w = Mem.dcW(0);
    public static final int WallXSize_w = Mem.dcW(0);
    public static final int WallZSize_w = Mem.dcW(0);
    public static final int WallLength_w = Mem.dcW(0);
    public static final int obj_QuitLimit_w = Mem.dcW(0);
    public static final int Obj_AwayFromWall_b = Mem.dcB(0); // accessed as byte
    public static final int Obj_WallBounce_b = Mem.dcB(0);   // accessed as byte

    // ---- objectmove.s:825-870 ----
    private static final int _a1 = Mem.align(4);
    public static final int oldx = Mem.dcL(0);
    public static final int oldz = Mem.dcL(0);
    public static final int oldy = Mem.dcL(0);
    public static final int newx = Mem.dcL(0);
    public static final int newz = Mem.dcL(0);
    public static final int newy = Mem.dcL(0);

    public static final int xdiff = Mem.dcL(0);
    public static final int zdiff = Mem.dcL(0);
    public static final int Obj_ZonePtr_l = Mem.dcL(0);
    public static final int obj_ZoneBackupPtr_l = Mem.dcL(0);

    public static final int deltax = Mem.dcW(0);
    public static final int speed = Mem.dcW(0);
    public static final int wallflags = Mem.dcW(0);
    public static final int distaway = Mem.dcW(0);

    public static final int thingheight = Mem.dcL(0);
    public static final int StepUpVal = Mem.dcL(0);
    public static final int StepDownVal = Mem.dcL(0);
    public static final int wallhitheight = Mem.dcL(0);

    public static final int LowerFloorHeight = Mem.dcL(0);
    public static final int LowerRoofHeight = Mem.dcL(0);
    public static final int UpperFloorHeight = Mem.dcL(0);
    public static final int UpperRoofHeight = Mem.dcL(0);
    public static final int billy = Mem.dcL(0, 0);

    public static final int StoodInTop = Mem.dcB(0);
    public static final int hitwall = Mem.dcB(0);
    public static final int exitfirst = Mem.dcB(0);
    private static final int _e0 = Mem.align(2); // even

    // ---- objectmove.s:1020-1022 ----
    public static final int counterer = Mem.dcW(0);
    public static final int CosRet = Mem.dcW(0);
    public static final int SinRet = Mem.dcW(0);

    // ---- objectmove.s:1190-1204 ----
    private static final int _a2 = Mem.align(4);
    public static final int AngRet = Mem.dcW(0);
    public static final int Range = Mem.dcW(0);
    public static final int GotThere = Mem.dcW(0);
    public static final int shovex = Mem.dcW(0);
    public static final int shovez = Mem.dcW(0);
    public static final int canshove = Mem.dcW(0);
    public static final int PLR2_pushx = Mem.dcL(0);
    public static final int PLR2_pushz = Mem.dcL(0);
    public static final int PLR2_opushx = Mem.dcL(0);
    public static final int PLR2_opushz = Mem.dcL(0);
    public static final int PLR1_pushx = Mem.dcL(0);
    public static final int PLR1_pushz = Mem.dcL(0);
    public static final int PLR1_opushx = Mem.dcL(0);
    public static final int PLR1_opushz = Mem.dcL(0);

    // ---- objectmove.s:1218 ----
    public static final int ONLYSEE = Mem.dcW(0);

    // ---- objectmove.s:1247-1252 ----
    private static final int _a3 = Mem.align(4);
    public static final int Obj_FromZonePtr_l = Mem.dcL(0);
    public static final int Obj_ToZonePtr_l = Mem.dcL(0);
    public static final int CanSee = Mem.dcW(0);
    public static final int Facedir = Mem.dcW(0);

    // ---- objectmove.s:1283-1291 ----
    private static final int _a4 = Mem.align(4);
    public static final int Viewerx = Mem.dcL(0);
    public static final int Viewerz = Mem.dcL(0);
    public static final int Targetx = Mem.dcL(0);
    public static final int Targetz = Mem.dcL(0);
    public static final int ViewerTop = Mem.dcB(0);
    public static final int TargetTop = Mem.dcB(0);
    public static final int Viewery = Mem.dcW(0);
    public static final int Targety = Mem.dcW(0);
    private static final int _e1 = Mem.align(2); // even

    // ---- objectmove.s:1517-1518 ----
    public static final int clipstocheck = Mem.dcL(0);
    public static final int donessomething = Mem.dcW(0);

    // ---- objectmove.s:1644 ----
    public static final int Rand1 = Mem.dcW(234);

    // ---- objectmove.s:1663 ----
    public static final int Obj_CollideFlags_l = Mem.dcL(0);

    // ---- objectmove.s:1789-1794 ----
    public static final int tmp_zone_id_w = Mem.dcW(0); // .tmp_zone_id_w (local à Obj_DoCollision)
    public static final int FromZone = Mem.dcW(0);
    public static final int OKTEL = Mem.dcW(0);
    public static final int floortemp = Mem.dcL(0);

    // ---- objectmove.s:1939-1943 ----
    public static final int xd = Mem.dcW(0);
    public static final int zd = Mem.dcW(0);
    public static final int possclose = Mem.alloc(2 * 100); // ds.w 100

    private ObjectmoveData() {
    }
}
