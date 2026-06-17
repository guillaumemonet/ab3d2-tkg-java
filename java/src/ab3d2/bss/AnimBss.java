package ab3d2.bss;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/anim_bss.s
 */
public final class AnimBss {

    private static final int _align0 = Mem.align(4);

    public static final int Anim_BrightY_l = Mem.alloc(4);
    public static final int anim_MiddleRoom_l = Mem.alloc(4);

    // Union (les deux mots consécutifs sont aussi accédés comme un long via
    // Anim_DoorAndLiftLocks_l)
    public static final int Anim_DoorAndLiftLocks_l = Mem.alloc(2); // MSW accessed as long
    public static final int anim_LiftOnlyLocks_w = Mem.alloc(2);    // LSW accessed independently as word

    // Word data
    public static final int Anim_SplatType_w = Mem.alloc(2);
    public static final int anim_MiddleX_w = Mem.alloc(2);
    public static final int anim_MiddleZ_w = Mem.alloc(2);
    public static final int anim_DoneFlames_w = Mem.alloc(2);

    public static final int Anim_FramesToDraw_w = Mem.alloc(2);
    public static final int Anim_TempFrames_w = Mem.alloc(2);
    public static final int anim_TimeToNoise_w = Mem.alloc(2);
    public static final int anim_OddEven_w = Mem.alloc(2);
    public static final int anim_FloorMoveSpeed_w = Mem.alloc(2);
    public static final int Anim_BrightTable_vw = Mem.alloc(2 * 20);
    public static final int Anim_Timer_w = Mem.alloc(2);
    public static final int anim_CurrentLiftable_w = Mem.alloc(2);
    public static final int anim_OpeningSpeed_w = Mem.alloc(2);
    public static final int anim_ClosingSpeed_w = Mem.alloc(2);
    public static final int anim_OpenDuration_w = Mem.alloc(2);
    public static final int anim_OpeningSoundFX_w = Mem.alloc(2);
    public static final int anim_ClosingSoundFX_w = Mem.alloc(2);
    public static final int anim_OpenedSoundFX_w = Mem.alloc(2);
    public static final int anim_ClosedSoundFX_w = Mem.alloc(2);
    public static final int anim_ActionSoundFX_w = Mem.alloc(2);
    public static final int anim_MaxDamage_w = Mem.alloc(2);

    private AnimBss() {
    }
}
