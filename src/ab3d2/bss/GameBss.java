package ab3d2.bss;

import ab3d2.Defs;
import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/game_bss.s
 */
public final class GameBss {

    private static final int _align0 = Mem.align(4);

    /** cleared at the start of every frame and checked at the end (_Game_ProgressSignal::) */
    public static final int Game_ProgressSignal_l = Mem.alloc(4);

    /** see defs.i for structure (_game_PlayerProgression::) */
    public static final int game_PlayerProgression = Mem.alloc(Defs.GStatT_SizeOf_l);
    public static final int game_PlayerProgressionEnd = Mem.allocTop();

    /** _game_AchievementsDataPtr:: */
    public static final int game_AchievementsDataPtr_l = Mem.alloc(4);

    /** _game_BestLevelTimeBuffer:: */
    public static final int game_BestLevelTimeBuffer_vb = Mem.alloc(Defs.LVLT_MESSAGE_LENGTH);

    private GameBss() {
    }
}
