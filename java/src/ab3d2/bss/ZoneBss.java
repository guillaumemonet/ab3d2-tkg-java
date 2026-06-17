package ab3d2.bss;

import ab3d2.Defs;
import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/zone_bss.s
 */
public final class ZoneBss {

    private static final int _align0 = Mem.align(4);

    public static final int Zone_BrightTable_vl = Mem.alloc(4 * 300);

    // Zone ordering
    public static final int Zone_EndOfListPtr_l = Mem.alloc(4);
    /** originally declared as 400 long, accessed as word */
    public static final int zone_ToDrawTable_vw = Mem.alloc(2 * 400 * 2);
    /** originally declared as 400 long, accessed as word */
    public static final int zone_OrderTable_vw = Mem.alloc(2 * 400 * 2);
    /** needs initialisation to -1 */
    public static final int Zone_OrderTable_Barrier_w = Mem.alloc(2);
    public static final int Zone_FinalOrderTable_vw = Mem.alloc(2 * 400 * 2);
    /** deliniates end of table */
    public static final int zone_FinalOrderTableBarrier_w = Mem.alloc(2);

    /** The nuber of visible joins in the current zone */
    public static final int Zone_VisJoins_w = Mem.alloc(2);
    /** The total number joins in the current zone */
    public static final int Zone_TotJoins_w = Mem.alloc(2);
    /** Bitmap of the visible joining edges */
    public static final int Zone_VisJoinMask_w = Mem.alloc(2);

    /**
     * For the shared edges in a zone, space to hold the indexes of the start/end
     * points to add to the subset of points that need to be rotated. This is because
     * the original code does not apply clips to the current zone, only the immediately
     * adjacent and beyond. This allows us to determine our own clip extents to the
     * root zone's shared edges.
     */
    public static final int Zone_EdgePointIndexes_vw = Mem.alloc(2 * 32);

    // For each of the doors and lifts, the Zone ID for each (or -1 if not associated)
    public static final int Zone_DoorList_vw = Mem.alloc(2 * Defs.LVL_MAX_DOOR_ZONES);
    public static final int Zone_LiftList_vw = Mem.alloc(2 * Defs.LVL_MAX_LIFT_ZONES);

    /**
     * The immediate global door state. This is updated during game logic when doors
     * open and close. The state may change unexpectedly as the logic runs in an interrupts
     */
    public static final int Zone_CurrentDoorState_w = Mem.alloc(2);

    /**
     * A snapshot of the Zone_CurrentDoorState_w taken at the beginning of rendering
     * each frame. This value will be used by the Edge PVS visibility at runtime
     */
    public static final int Zone_RenderDoorState_w = Mem.alloc(2);

    /**
     * Bitmap of the zones that contain doors. Since there are only 16 doors, except for
     * very small levels, most zones will not be a door. [...] This bitmap prevents an
     * exhaustive check across the door list by allowing us to test first if a zone is
     * a door, before we then look up which one it is.
     */
    public static final int Zone_DoorMap_vb = Mem.alloc(Defs.LVL_MAX_ZONE_COUNT / 8);

    /** As above, for lift zones */
    public static final int Zone_LiftMap_vb = Mem.alloc(Defs.LVL_MAX_ZONE_COUNT / 8);

    private static final int _align1 = Mem.align(4);

    /** worst case sizes. We don't expect all zones visible */
    public static final int Zone_PVSList_vw = Mem.alloc(2 * Defs.LVL_MAX_ZONE_COUNT);
    public static final int Zone_PVSMask_vb = Mem.alloc(Defs.LVL_MAX_ZONE_COUNT);
    private static final int _align2 = Mem.align(4);

    // Bitmask
    public static final int EDGE_POINT_ID_LIST_END = -4;
    public static final int ZONE_BACKDROP_DISABLE_SIZE = Defs.LVL_EXPANDED_MAX_ZONE_COUNT / 8;

    public static final int Zone_BackdropDisable_vb = Mem.alloc(ZONE_BACKDROP_DISABLE_SIZE);

    private ZoneBss() {
    }
}
