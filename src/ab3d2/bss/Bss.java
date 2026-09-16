package ab3d2.bss;

/**
 * Force l'initialisation des sections BSS dans l'ordre d'inclusion de hires.s :
 *
 *   include "bss/system_bss.s"
 *   include "bss/io_bss.s"
 *   include "bss/vid_bss.s"
 *   include "bss/level_bss.s"
 *   include "bss/ai_bss.s"
 *   include "bss/anim_bss.s"
 *   include "bss/player_bss.s"
 *   include "bss/draw_bss.s"
 *   include "bss/zone_bss.s"
 *   include "bss/tables_bss.s"
 *   include "bss/game_bss.s"
 *
 * Les adresses sont allouées par les initialiseurs statiques de chaque classe ;
 * appeler init() au démarrage (avant tout accès à Mem) garantit une disposition
 * mémoire déterministe, identique à celle de l'assembleur.
 */
public final class Bss {

    private static boolean done;

    private Bss() {
    }

    public static void init() {
        if (done) {
            return;
        }
        done = true;
        // Toucher un champ de chaque classe déclenche son initialisation statique,
        // dans l'ordre d'inclusion de l'original.
        touch(SystemBss._GfxBase);
        touch(IoBss.IO_MemType_l);
        touch(VidBss.Vid_C2PSetParamsPtr_l);
        touch(LevelBss.PointsToRotatePtr_l);
        touch(AiBss.ai_AlienWorkspace_vl);
        touch(AnimBss.Anim_BrightY_l);
        touch(PlayerBss.Plr1_ObjectPtr_l);
        touch(DrawBss.draw_DepthTable_vl);
        touch(ZoneBss.Zone_BrightTable_vl);
        touch(TablesBss.Lvl_CompactMap_vl);
        touch(GameBss.Game_ProgressSignal_l);
    }

    private static void touch(int addr) {
        // no-op : le simple accès au champ initialise la classe
    }
}
