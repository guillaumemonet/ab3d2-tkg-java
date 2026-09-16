package ab3d2.bss;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/ai_bss.s
 */
public final class AiBss {

    private static final int _align0 = Mem.align(4);

    public static final int ai_AlienWorkspace_vl = Mem.alloc(4 * 4 * 300);
    public static final int AI_AlienTeamWorkspace_vl = Mem.alloc(4 * 4 * 30);
    public static final int AI_OtherAlienDataPtrs_vl = Mem.alloc(4 * 20);
    public static final int AI_Damaged_vw = Mem.alloc(2 * 300);
    public static final int AI_DamagePtr_l = Mem.alloc(4);
    public static final int AI_BoredomPtr_l = Mem.alloc(4);
    public static final int AI_BoredomSpace_vl = Mem.alloc(4 * 2 * 300);
    public static final int AI_FlyABit_w = Mem.alloc(2);
    public static final int AI_DefaultMode_w = Mem.alloc(2);
    public static final int AI_ResponseMode_w = Mem.alloc(2);
    public static final int AI_FollowupMode_w = Mem.alloc(2);
    public static final int AI_RetreatMode_w = Mem.alloc(2);
    public static final int AI_CurrentMode_w = Mem.alloc(2);  // unused ?
    public static final int AI_ProwlSpeed_w = Mem.alloc(2);
    public static final int AI_ResponseSpeed_w = Mem.alloc(2);
    public static final int AI_RetreatSpeed_w = Mem.alloc(2);
    public static final int AI_FollowupSpeed_w = Mem.alloc(2);
    public static final int AI_FollowupTimer_w = Mem.alloc(2);
    public static final int AI_ReactionTime_w = Mem.alloc(2);
    public static final int AI_VecObj_w = Mem.alloc(2);

    public static final int ai_MiddleCPT_w = Mem.alloc(2);
    public static final int ai_GetOut_w = Mem.alloc(2);
    public static final int ai_ToSide_w = Mem.alloc(2);
    public static final int ai_AnimFacing_w = Mem.alloc(2);
    public static final int ai_DoAction_b = Mem.alloc(1);
    public static final int ai_FinishedAnim_b = Mem.alloc(1);
    public static final int AI_NoEnemies_b = Mem.alloc(1);

    private AiBss() {
    }
}
