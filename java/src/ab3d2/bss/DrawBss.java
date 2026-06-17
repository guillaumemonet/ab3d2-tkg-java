package ab3d2.bss;

import ab3d2.Defs;
import ab3d2.Hires;
import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/draw_bss.s
 *
 * TODO (original) - Gather by access patterns and group into cache lines for
 * hot/nearby data and consolidated blocks by size/alignment for everything else.
 */
public final class DrawBss {

    private static final int _align0 = Mem.align(4);

    public static final int DRAW_MAX_POLY_OBJECTS = 40;
    public static final int DRAW_MAX_OBJECTS = 38;

    public static final int draw_DepthTable_vl = Mem.alloc(4 * 80);
    public static final int draw_DepthTableEnd = Mem.allocTop();

    public static final int Draw_TopOfRoom_l = Mem.alloc(4);
    public static final int Draw_BottomOfRoom_l = Mem.alloc(4);
    public static final int Draw_AfterWaterTop_l = Mem.alloc(4);
    public static final int Draw_AfterWaterBottom_l = Mem.alloc(4);
    public static final int Draw_BeforeWaterTop_l = Mem.alloc(4);
    public static final int Draw_BeforeWaterBottom_l = Mem.alloc(4);
    public static final int draw_BackupRoomPtr_l = Mem.alloc(4);
    public static final int draw_ObjectOnOff_l = Mem.alloc(4);

    public static final int draw_PointAndPolyBrights_vl = Mem.alloc(4 * 4 * 16);
    public static final int draw_PointerTablePtr_l = Mem.alloc(4);
    public static final int draw_StartOfObjPtr_l = Mem.alloc(4);
    public static final int Draw_PolyObjects_vl = Mem.alloc(4 * DRAW_MAX_POLY_OBJECTS);

    // FIMXE: screenconv stores word sized points, why are they using ds.l here?
    /** projected 2D points in screenspace */
    public static final int draw_2DPointsProjected_vl = Mem.alloc(4 * 250 * 2);
    /** rotated 3D points in X/Z plane (y pointing up) */
    public static final int draw_3DPointsRotated_vl = Mem.alloc(4 * 250 * 3);

    public static final int Draw_WallTexturePtrs_vl = Mem.alloc(4 * Defs.NUM_WALL_TEXTURES);
    public static final int Draw_ObjectPtrs_vl = Mem.alloc(4 * DRAW_MAX_OBJECTS * 4);
    public static final int Draw_TextureMapsPtr_l = Mem.alloc(4);

    // Shade Tables, 64x256
    // The first 32 begin with pure white and gradually saturating towards the palette at row 32.
    // Used for glare/specular. The remaining rows are increasingly darkened towards black and are
    // used for general lighting. Each entry is byte index to the nearest palette match.
    public static final int Draw_TexturePalettePtr_l = Mem.alloc(4);
    public static final int Draw_BackdropImagePtr_l = Mem.alloc(4);
    /**
     * this will be a copy of either Draw_GlobalFloorTexturesPtr_l
     * or Draw_LevelFloorTexturesPtr_l if the level has an override.
     */
    public static final int Draw_FloorTexturesPtr_l = Mem.alloc(4);
    public static final int Draw_PalettePtr_l = Mem.alloc(4);
    public static final int Draw_ChunkPtr_l = Mem.alloc(4);

    // Allow levels to override the floor texture tiles
    /** this is the pointer to the global set of floor tiles */
    public static final int Draw_GlobalFloorTexturesPtr_l = Mem.alloc(4);
    /** this is the pointer to the current level override, if any. */
    public static final int Draw_LevelFloorTexturesPtr_l = Mem.alloc(4);

    // Allow levels to override individual wall textures
    public static final int Draw_GlobalWallTexturePtrs_vl = Mem.alloc(4 * Defs.NUM_WALL_TEXTURES);
    public static final int Draw_LevelWallTexturePtrs_vl = Mem.alloc(4 * Defs.NUM_WALL_TEXTURES);

    public static final int draw_AngleBrights_vl = Mem.alloc(4 * 8 * 2);
    public static final int draw_Pals_vl = Mem.alloc(4 * 2 * 49);
    public static final int draw_WADPtr_l = Mem.alloc(4);
    public static final int draw_PtrPtr_l = Mem.alloc(4);   // todo - find what this actually points to
    public static final int draw_PolyAngPtr_l = Mem.alloc(4);
    public static final int draw_PointAngPtr_l = Mem.alloc(4);

    public static final int toppt_l = Mem.alloc(4);
    public static final int midobj_l = Mem.alloc(4);
    public static final int boxbrights_vw = Mem.alloc(2 * 250);
    // boxang: ds.w 1 (commenté dans l'original)

    // draw_PolyBotTab_vw has negative offsets
    private static final int _pad0 = Mem.alloc(2 * Hires.SCREEN_WIDTH * 4);
    public static final int draw_PolyBotTab_vw = Mem.alloc(2 * Hires.SCREEN_WIDTH * 8);
    private static final int _pad1 = Mem.alloc(2 * Hires.SCREEN_WIDTH * 4);
    public static final int draw_PolyTopTab_vw = Mem.alloc(2 * Hires.SCREEN_WIDTH * 8);
    private static final int _pad2 = Mem.alloc(2 * Hires.SCREEN_WIDTH * 4);

    public static final int draw_PartBuffer_vw = Mem.alloc(2 * 4 * 32);
    public static final int draw_PartBufferEnd = Mem.allocTop();

    public static final int Draw_CurrentZone_w = Mem.alloc(2);
    public static final int Draw_ZoneClipL_w = Mem.alloc(2);
    public static final int Draw_ZoneClipR_w = Mem.alloc(2);

    public static final int draw_SortIt_w = Mem.alloc(2);
    public static final int draw_ObjectBright_w = Mem.alloc(2);
    public static final int draw_ObjectAng_w = Mem.alloc(2);
    public static final int draw_PolygonCentreY_w = Mem.alloc(2);
    public static final int draw_ObjClipT_w = Mem.alloc(2);
    public static final int draw_ObjClipB_w = Mem.alloc(2);
    public static final int draw_RightClipB_w = Mem.alloc(2);
    public static final int draw_LeftClipB_w = Mem.alloc(2);
    public static final int draw_AuxX_w = Mem.alloc(2);
    public static final int draw_AuxY_w = Mem.alloc(2);
    public static final int draw_BrightToAdd_w = Mem.alloc(2);
    public static final int draw_Obj_XPos_w = Mem.alloc(2);
    public static final int draw_Obj_ZPos_w = Mem.alloc(2);
    public static final int draw_NumPoints_w = Mem.alloc(2);
    public static final int draw_OffLeftBy_w = Mem.alloc(2);
    public static final int draw_Left_w = Mem.alloc(2);
    public static final int draw_Right_w = Mem.alloc(2);
    public static final int draw_DownStrip_w = Mem.alloc(2);

    // Border Ammo/Energy
    public static final int draw_DisplayEnergyCount_w = Mem.alloc(2);
    public static final int draw_DisplayAmmoCount_w = Mem.alloc(2);
    public static final int draw_LastDisplayEnergyCount_w = Mem.alloc(2);
    public static final int draw_LastDisplayAmmoCount_w = Mem.alloc(2);

    public static final int draw_WhichDoing_b = Mem.alloc(1);
    public static final int draw_InUpperZone_b = Mem.alloc(1);
    public static final int Draw_DoUpper_b = Mem.alloc(1);
    public static final int Draw_InRootZone_b = Mem.alloc(1);

    public static final int Draw_ForceSimpleWalls_b = Mem.alloc(1);
    public static final int Draw_ForceZoneSkip_b = Mem.alloc(1);

    // IFD GEN_GLYPH_DATA (non défini dans le build standard) :
    // DCLC draw_GlyphSpacing_vb, ds.b, 256

    private DrawBss() {
    }
}
