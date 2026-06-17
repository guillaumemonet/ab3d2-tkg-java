package ab3d2.bss;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/level_bss.s
 *
 * Floor lines:
 * A floor line is a line seperating two rooms.
 * The data for the line is therefore: x,y,dx,dy,Room1,Room2
 * For ease of editing the lines are initially stored in the form
 * startpt,endpt,Room1,Room2 and the program calculates x,y,dx and dy
 * from this information and stores it in a buffer.
 */
public final class LevelBss {

    private static final int _align0 = Mem.align(4);

    // long aligned data
    public static final int PointsToRotatePtr_l = Mem.alloc(4);

    public static final int Lvl_DataPtr_l = Mem.alloc(4);
    public static final int Lvl_ZEdgePVSHeaderPtrsPtr_l = Mem.alloc(4);

    // *************************************************************
    // * ROOM GRAPHICAL DESCRIPTIONS : WALLS AND FLOORS ************
    // *************************************************************

    public static final int Lvl_ZoneBorderPointsPtr_l = Mem.alloc(4);
    public static final int Lvl_ConnectTablePtr_l = Mem.alloc(4);

    /** points at the list of PVS zones for rendering */
    public static final int Lvl_ListOfGraphRoomsPtr_l = Mem.alloc(4);

    public static final int AI_AlienShotDataPtr_l = Mem.alloc(4);
    public static final int Lvl_ObjectPointsPtr_l = Mem.alloc(4);
    public static final int Lvl_ObjectDataPtr_l = Mem.alloc(4);

    public static final int Lvl_ZoneEdgePtr_l = Mem.alloc(4);
    /** Pointer to array of all 2D points in the world */
    public static final int Lvl_PointsPtr_l = Mem.alloc(4);

    public static final int Lvl_ZoneGraphAddsPtr_l = Mem.alloc(4);

    /** Zone* pZone = LvlDataPtr_l[Lvl_ZonePtrsPtr_l[zone_id]] */
    public static final int Lvl_ZonePtrsPtr_l = Mem.alloc(4);

    public static final int Lvl_LiftDataPtr_l = Mem.alloc(4);
    public static final int Lvl_DoorDataPtr_l = Mem.alloc(4);

    public static final int Lvl_SwitchDataPtr_l = Mem.alloc(4);
    public static final int Lvl_ControlPointCoordsPtr_l = Mem.alloc(4);
    public static final int Lvl_GraphicsPtr_l = Mem.alloc(4);

    /** Indexes into point data */
    public static final int Lvl_ClipsPtr_l = Mem.alloc(4);

    // For custom properties and/or errata
    public static final int Lvl_ModPropertiesPtr_l = Mem.alloc(4);
    public static final int Lvl_ErrataPtr_l = Mem.alloc(4);

    public static final int Lvl_EdgeCount_l = Mem.alloc(4);

    // Word aligned data
    public static final int Lvl_NumControlPoints_w = Mem.alloc(2);

    public static final int Lvl_NumPoints_w = Mem.alloc(2);

    public static final int Lvl_NumObjectPoints_w = Mem.alloc(2);

    public static final int Lvl_NumZones_w = Mem.alloc(2);

    private LevelBss() {
    }
}
