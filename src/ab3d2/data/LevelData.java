package ab3d2.data;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/data/level_data.s
 *
 * Level data filenames. These are null terminated strings that are split on
 * the character for the name. This is poked in during loading.
 * (la lettre de niveau 'a' à l'offset _X est remplacée à chaud)
 */
public final class LevelData {

    public static final int Lvl_BinFilename_vb;
    public static final int Lvl_BinFilenameX_vb;
    public static final int Lvl_GfxFilename_vb;
    public static final int Lvl_GfxFilenameX_vb;
    public static final int Lvl_ClipsFilename_vb;
    public static final int Lvl_ClipsFilenameX_vb;
    public static final int Lvl_MapFilename_vb;
    public static final int Lvl_MapFilenameX_vb;
    public static final int Lvl_FlyMapFilename_vb;
    public static final int Lvl_FlyMapFilenameX_vb;

    // For per-level floor overrides (_Lvl_FloorFilename_s:: pour le C)
    public static final int Lvl_FloorFilename_vb;
    public static final int Lvl_FloorFilenameX_vb;

    // For per-level wall overrides
    public static final int Lvl_WallFilename_vb;
    public static final int Lvl_WallFilenameX_vb;
    public static final int Lvl_WallFilenameN_vb;

    // For per-level modifications (_Lvl_ModPropsFilename_s::)
    public static final int Lvl_ModPropsFilename_vb;
    public static final int Lvl_ModPropsFilenameX_vb;

    // For per-level modifications (_Lvl_ErrataFilename_s::)
    public static final int Lvl_ErrataFilename_vb;
    public static final int Lvl_ErrataFilenameX_vb;

    static {
        Mem.align(4);
        Lvl_BinFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_BinFilenameX_vb = Mem.dcStr("a/twolev.bin");
        Mem.dcB(0);
        Lvl_GfxFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_GfxFilenameX_vb = Mem.dcStr("a/twolev.graph.bin");
        Mem.dcB(0);
        Lvl_ClipsFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_ClipsFilenameX_vb = Mem.dcStr("a/twolev.clips");
        Mem.dcB(0);
        Lvl_MapFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_MapFilenameX_vb = Mem.dcStr("a/twolev.map");
        Mem.dcB(0);
        Lvl_FlyMapFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_FlyMapFilenameX_vb = Mem.dcStr("a/twolev.flymap");
        Mem.dcB(0);

        Lvl_FloorFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_FloorFilenameX_vb = Mem.dcStr("a/floortile");
        Mem.dcB(0);

        Lvl_WallFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_WallFilenameX_vb = Mem.dcStr("a/wall_");
        Lvl_WallFilenameN_vb = Mem.dcStr("0.256wad");
        Mem.dcB(0);

        Lvl_ModPropsFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_ModPropsFilenameX_vb = Mem.dcStr("a/properties.dat");
        Mem.dcB(0);

        Lvl_ErrataFilename_vb = Mem.dcStr("ab3:levels/level_");
        Lvl_ErrataFilenameX_vb = Mem.dcStr("a/errata.dat");
        Mem.dcB(0);
    }

    private LevelData() {
    }
}
