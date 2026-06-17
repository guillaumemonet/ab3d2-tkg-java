package ab3d2.data;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/data/game_data.s
 */
public final class GameData {

    public static final int game_PropertiesFile_vb;   // _game_PropertiesFile::
    public static final int game_PreferencesFile_vb;  // _game_PreferencesFile::
    public static final int game_ProgressFile_vb;     // _game_ProgressFile::
    public static final int Game_SavedGamesName_vb;
    public static final int game_Version_vb;          // include "data/version.i"

    static {
        Mem.align(4);
        game_PropertiesFile_vb = Mem.dcStr("ab3:Includes/game.props");
        Mem.dcB(0);
        game_PreferencesFile_vb = Mem.dcStr("ab3:prefs.cfg");
        Mem.dcB(0);
        game_ProgressFile_vb = Mem.dcStr("ab3:game.stats");
        Mem.dcB(0);
        Game_SavedGamesName_vb = Mem.dcStr("ab3:boot.dat");
        Mem.dcB(0);

        // data/version.i : dc.b 0,"$VER: ...",0 (variante release 68030+, cf. Version.java)
        game_Version_vb = Mem.dcB(0);
        Mem.dcStr(Version.VER_030);
        Mem.dcB(0);
    }

    private GameData() {
    }
}
