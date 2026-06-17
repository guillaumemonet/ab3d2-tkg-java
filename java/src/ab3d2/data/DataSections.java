package ab3d2.data;

/**
 * Force l'initialisation des sections .data dans l'ordre d'inclusion de hires.s :
 *
 *   include "data/system_data.s"
 *   include "data/draw_data.s"
 *   include "data/level_data.s"
 *   include "data/tables_data.s"
 *   include "data/text_data.s"
 *   include "data/game_data.s"
 *   include "data/vid_data.s"
 *
 * À appeler après Bss.init() (la section .data suit la .bss dans notre
 * allocateur ; sur Amiga ce sont des hunks distincts, leurs adresses
 * relatives n'ont pas d'importance sémantique).
 */
public final class DataSections {

    private static boolean done;

    private DataSections() {
    }

    public static void init() {
        if (done) {
            return;
        }
        done = true;
        touch(SystemData.DosName);
        touch(DrawData.draw_TeleportShimmerFXData_vb);
        touch(LevelData.Lvl_BinFilename_vb);
        touch(TablesData.SinCosTable_vw);
        touch(TextData.Game_SoundOptionsText_vb);
        touch(GameData.game_PropertiesFile_vb);
        touch(VidData.Vid_GammaIncTables_vb);
    }

    private static void touch(int addr) {
        // no-op : le simple accès au champ initialise la classe
    }
}
