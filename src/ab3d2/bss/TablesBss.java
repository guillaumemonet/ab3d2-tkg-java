package ab3d2.bss;

import ab3d2.Defs;
import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/tables_bss.s
 *
 * "Ad hoc tables that we don't know where else to put yet."
 */
public final class TablesBss {

    private static final int _align0 = Mem.align(4);

    public static final int Lvl_CompactMap_vl = Mem.alloc(4 * 257);
    public static final int Lvl_BigMap_vl = Mem.alloc(4 * 256 * 10);

    public static final int PointBrightsPtr_l = Mem.alloc(4);
    public static final int CurrentPointBrights_vl = Mem.alloc(4 * 2 * 256 * 10);
    public static final int ClipsTable_vl = Mem.alloc(4 * 30);
    public static final int EndOfClipPtr_l = Mem.alloc(4);

    /** store rotated X and Z coordinates with Z scaling applied */
    public static final int Rotated_vl = Mem.alloc(4 * 2 * 800);

    public static final int ObjRotated_vl = Mem.alloc(4 * 2 * 500);

    /** store screen projected X coordinates for rotated points */
    public static final int OnScreen_vl = Mem.alloc(4 * 2 * 800);

    /** hires.s - may depend on position relative to ObjectWorkspace_vl */
    public static final int WorkspacePtr_l = Mem.alloc(4);
    public static final int ObjectWorkspace_vl = Mem.alloc(4 * 600); // hires.s

    /** 8192 pairs of long */
    public static final int ConstantTable_vl = Mem.alloc(4 * 8192 * 2);

    public static final int DataBuffer1_vl = Mem.alloc(4 * 1600); // wall drawing
    public static final int DataBuffer2_vl = Mem.alloc(4 * 1600); // wall drawing
    public static final int Storage_vl = Mem.alloc(4 * 500);      // drawing

    public static final int Aud_EmptyBuffer_vl = Mem.alloc(4 * 100); // hires.s - audio
    public static final int Aud_EmptyBufferEnd = Mem.allocTop();
    public static final int Aud_SampleList_vl = Mem.alloc(4 * Defs.NUM_SFX * 2); // {start,end}

    public static final int LeftSideTable_vw = Mem.alloc(2 * 512 * 2);
    public static final int RightSideTable_vw = Mem.alloc(2 * 512 * 2);
    public static final int LeftBrightTable_vw = Mem.alloc(2 * 512 * 2);
    public static final int RightBrightTable_vw = Mem.alloc(2 * 512 * 2);

    public static final int anim_LiftHeightTable_vw = Mem.alloc(2 * 40); // newanims.s
    public static final int anim_DoorOpenTimers_vw = Mem.alloc(2 * 40);  // newanims.s

    public static final int Obj_RoomPath_vw = Mem.alloc(2 * 100); // objmove.s

    public static final int game_ModProps = Mem.alloc(Defs.GModT_SizeOf_l);
    public static final int KeyMap_vb = Mem.alloc(256);

    private TablesBss() {
    }
}
