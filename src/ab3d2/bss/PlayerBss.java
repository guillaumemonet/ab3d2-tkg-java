package ab3d2.bss;

import ab3d2.Defs;
import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/bss/player_bss.s
 *
 * Globals used in more than one source file are considered public and are
 * capitalised. Globals used in one file is considered private and is not
 * capitalised. (commentaire d'origine)
 */
public final class PlayerBss {

    private static final int _align0 = Mem.align(4);

    // READY PLAYER ONE !

    // Long data
    public static final int Plr1_Data = Mem.allocTop();          // DCLC Plr1_Data (label sans stockage)
    public static final int Plr1_ObjectPtr_l = Mem.alloc(4);

    public static final int Plr1_Position_vl = Mem.allocTop();   // DCLC Plr1_Position_vl (label)
    public static final int Plr1_XOff_l = Mem.alloc(4);          // sometimes accessed as w
    public static final int Plr1_YOff_l = Mem.alloc(4);
    public static final int Plr1_ZOff_l = Mem.alloc(4);          // sometimes accessed as w
    public static final int Plr1_ZonePtr_l = Mem.alloc(4);
    public static final int Plr1_Height_l = Mem.alloc(4);
    public static final int Plr1_AimSpeed_l = Mem.alloc(4);

    public static final int Plr1_SnapXOff_l = Mem.alloc(4);
    public static final int Plr1_SnapYOff_l = Mem.alloc(4);
    public static final int Plr1_SnapYVel_l = Mem.alloc(4);
    public static final int Plr1_SnapZOff_l = Mem.alloc(4);
    public static final int Plr1_SnapTYOff_l = Mem.alloc(4);
    public static final int Plr1_SnapXSpdVal_l = Mem.alloc(4);
    public static final int Plr1_SnapZSpdVal_l = Mem.alloc(4);
    public static final int Plr1_SnapHeight_l = Mem.alloc(4);
    public static final int Plr1_SnapTargHeight_l = Mem.alloc(4);
    public static final int Plr1_TmpXOff_l = Mem.alloc(4);       // also accessed as w
    public static final int Plr1_TmpZOff_l = Mem.alloc(4);       // suspect 16:16
    public static final int Plr1_TmpYOff_l = Mem.alloc(4);

    // Private fields
    public static final int Plr1_PotVisibleZoneListPtr_l = Mem.alloc(4); // hires.s
    public static final int plr1_PointsToRotatePtr_l = Mem.alloc(4);     // hires.s
    public static final int plr1_BobbleY_l = Mem.alloc(4);               // hires.s
    public static final int plr1_TmpHeight_l = Mem.alloc(4);             // hires.s
    public static final int plr1_OldX_l = Mem.alloc(4);                  // hires.s
    public static final int plr1_OldZ_l = Mem.alloc(4);                  // hires.s
    public static final int plr1_OldRoomPtr_l = Mem.alloc(4);            // leveldata2.s - write once?
    public static final int plr1_SnapSquishedHeight_l = Mem.alloc(4);    // plr1control.s
    public static final int plr1_DefaultEnemyFlags_l = Mem.alloc(4);

    // Word data
    public static final int Plr1_Energy_w = Mem.alloc(2);

    public static final int Plr1_Direction_vw = Mem.allocTop();  // DCLC Plr1_Direction_vw (label)
    public static final int Plr1_CosVal_w = Mem.alloc(2);
    public static final int Plr1_SinVal_w = Mem.alloc(2);
    public static final int Plr1_AngPos_w = Mem.alloc(2);

    public static final int Plr1_Zone_w = Mem.alloc(2);          // _Plr1_Zone::
    public static final int Plr1_FloorSpd_w = Mem.alloc(2);
    public static final int Plr1_RoomBright_w = Mem.alloc(2);
    public static final int Plr1_Bobble_w = Mem.alloc(2);
    public static final int Plr1_SnapAngPos_w = Mem.alloc(2);
    public static final int Plr1_SnapAngSpd_w = Mem.alloc(2);
    public static final int Plr1_TmpAngPos_w = Mem.alloc(2);
    public static final int Plr1_TimeToShoot_w = Mem.alloc(2);

    // CAUTION This section is loaded/saved and must not be reordered
    public static final int Plr1_Invetory_vw = Mem.allocTop();   // _Plr1_Inventory:: (label)
    public static final int Plr1_Health_w = Mem.alloc(2);
    public static final int Plr1_JetpackFuel_w = Mem.alloc(2);
    public static final int Plr1_AmmoCounts_vw = Mem.alloc(2 * 20);
    public static final int Plr1_Shield_w = Mem.alloc(2);
    public static final int Plr1_Jetpack_w = Mem.alloc(2);

    public static final int Plr1_Weapons_vb = Mem.alloc(2 * 10); // todo - convert to bytes or bitfield

    public static final int Plr1_GunFrame_w = Mem.alloc(2);
    public static final int Plr1_NoiseVol_w = Mem.alloc(2);

    // Private fields
    public static final int plr1_TmpHoldDown_w = Mem.alloc(2);   // hires.s
    public static final int plr1_TmpBobble_w = Mem.alloc(2);     // hires.s

    public static final int plr1_SnapCosVal_w = Mem.alloc(2);    // plr1control.s
    public static final int plr1_SnapSinVal_w = Mem.alloc(2);    // plr1control.s

    public static final int plr1_WalkSFXTime_w = Mem.alloc(2);   // fall.s

    // Byte data
    public static final int Plr1_Keys_b = Mem.alloc(1);
    public static final int Plr1_Path_b = Mem.alloc(1);

    public static final int Plr1_Mouse_b = Mem.alloc(1);
    public static final int Plr1_Joystick_b = Mem.alloc(1);
    public static final int Plr1_GunSelected_b = Mem.alloc(1);
    public static final int Plr1_StoodInTop_b = Mem.alloc(1);

    public static final int Plr1_Ducked_b = Mem.alloc(1);
    public static final int Plr1_Squished_b = Mem.alloc(1);
    public static final int Plr1_Echo_b = Mem.alloc(1);
    public static final int Plr1_Fire_b = Mem.alloc(1);

    public static final int Plr1_Clicked_b = Mem.alloc(1);
    public static final int Plr1_Used_b = Mem.alloc(1);
    public static final int Plr1_TmpClicked_b = Mem.alloc(1);
    public static final int Plr1_TmpSpcTap_b = Mem.alloc(1);

    public static final int Plr1_TmpGunSelected_b = Mem.alloc(1);

    public static final int Plr1_TmpFire_b = Mem.alloc(1);

    // Private fields
    public static final int plr1_Teleported_b = Mem.alloc(1);    // hires.s
    public static final int plr1_Dead_b = Mem.alloc(1);          // hires.s

    public static final int plr1_TmpDucked_b = Mem.alloc(1);     // hires.s
    public static final int plr1_StoodOnLift_b = Mem.alloc(1);   // newanims.s
    public static final int plr1_Reserved1_b = Mem.alloc(1);
    public static final int plr1_Reserved2_b = Mem.alloc(1);

    // aligned 4
    public static final int Plr1_ObjectDistances_vw = Mem.alloc(2 * Defs.MAX_LEVEL_OBJ_DIST_COUNT);
    public static final int Plr1_ObsInLine_vb = Mem.alloc(Defs.MAX_OBJS_IN_LINE_COUNT);

    // READY PLAYER TWO !

    private static final int _align1 = Mem.align(4);

    // Long data
    public static final int Plr2_Data = Mem.allocTop();          // DCLC Plr2_Data (label)

    public static final int Plr2_ObjectPtr_l = Mem.alloc(4);

    public static final int Plr2_Position_vl = Mem.allocTop();   // DCLC Plr2_Position_vl (label)
    public static final int Plr2_XOff_l = Mem.alloc(4);
    public static final int Plr2_YOff_l = Mem.alloc(4);
    public static final int Plr2_ZOff_l = Mem.alloc(4);
    public static final int Plr2_ZonePtr_l = Mem.alloc(4);
    public static final int Plr2_Height_l = Mem.alloc(4);
    public static final int Plr2_AimSpeed_l = Mem.alloc(4);
    public static final int Plr2_SnapXOff_l = Mem.alloc(4);
    public static final int Plr2_SnapYOff_l = Mem.alloc(4);
    public static final int Plr2_SnapYVel_l = Mem.alloc(4);
    public static final int Plr2_SnapZOff_l = Mem.alloc(4);
    public static final int Plr2_SnapTYOff_l = Mem.alloc(4);
    public static final int Plr2_SnapXSpdVal_l = Mem.alloc(4);
    public static final int Plr2_SnapZSpdVal_l = Mem.alloc(4);
    public static final int Plr2_SnapHeight_l = Mem.alloc(4);
    public static final int Plr2_SnapTargHeight_l = Mem.alloc(4);
    public static final int Plr2_TmpXOff_l = Mem.alloc(4);
    public static final int Plr2_TmpZOff_l = Mem.alloc(4);
    public static final int Plr2_TmpYOff_l = Mem.alloc(4);

    // Private fields
    public static final int Plr2_PotVisibleZoneListPtr_l = Mem.alloc(4); // hires.s
    public static final int plr2_PointsToRotatePtr_l = Mem.alloc(4);     // hires.s
    public static final int plr2_BobbleY_l = Mem.alloc(4);               // hires.s
    public static final int plr2_TmpHeight_l = Mem.alloc(4);             // hires.s
    public static final int plr2_OldX_l = Mem.alloc(4);                  // hires.s
    public static final int plr2_OldZ_l = Mem.alloc(4);                  // hires.s
    public static final int plr2_OldRoomPtr_l = Mem.alloc(4);            // leveldata2.s
    public static final int plr2_SnapSquishedHeight_l = Mem.alloc(4);    // plr2control.s
    public static final int plr2_DefaultEnemyFlags_l = Mem.alloc(4);

    // Word Data
    public static final int Plr2_Energy_w = Mem.alloc(2);
    public static final int Plr2_CosVal_w = Mem.alloc(2);
    public static final int Plr2_SinVal_w = Mem.alloc(2);
    public static final int Plr2_AngPos_w = Mem.alloc(2);
    public static final int Plr2_Zone_w = Mem.alloc(2);
    public static final int Plr2_FloorSpd_w = Mem.alloc(2);      // newanim.s
    public static final int Plr2_RoomBright_w = Mem.alloc(2);
    public static final int Plr2_Bobble_w = Mem.alloc(2);
    public static final int Plr2_SnapAngPos_w = Mem.alloc(2);
    public static final int Plr2_SnapAngSpd_w = Mem.alloc(2);
    public static final int Plr2_TmpAngPos_w = Mem.alloc(2);     // hires.s
    public static final int Plr2_TimeToShoot_w = Mem.alloc(2);

    public static final int Plr2_Invetory_vw = Mem.allocTop();   // _Plr2_Inventory:: (label)
    public static final int Plr2_Health_w = Mem.alloc(2);
    public static final int Plr2_JetpackFuel_w = Mem.alloc(2);
    public static final int Plr2_AmmoCounts_vw = Mem.alloc(2 * 20);
    public static final int Plr2_Shield_w = Mem.alloc(2);
    public static final int Plr2_Jetpack_w = Mem.alloc(2);

    public static final int Plr2_Weapons_vb = Mem.alloc(2 * 10); // todo - convert to bytes or bitfield

    public static final int Plr2_GunFrame_w = Mem.alloc(2);
    public static final int Plr2_NoiseVol_w = Mem.alloc(2);

    // Private
    public static final int plr2_TmpHoldDown_w = Mem.alloc(2);   // hires.s
    public static final int plr2_TmpBobble_w = Mem.alloc(2);     // hires.s

    public static final int plr2_SnapCosVal_w = Mem.alloc(2);    // plr2control.s
    public static final int plr2_SnapSinVal_w = Mem.alloc(2);    // plr2control.s

    public static final int plr2_WalkSFXTime_w = Mem.alloc(2);   // fall.s

    // Byte Data
    public static final int Plr2_Keys_b = Mem.alloc(1);
    public static final int Plr2_Path_b = Mem.alloc(1);

    public static final int Plr2_Mouse_b = Mem.alloc(1);
    public static final int Plr2_Joystick_b = Mem.alloc(1);
    public static final int Plr2_GunSelected_b = Mem.alloc(1);
    public static final int Plr2_StoodInTop_b = Mem.alloc(1);

    public static final int Plr2_Ducked_b = Mem.alloc(1);
    public static final int Plr2_Squished_b = Mem.alloc(1);
    public static final int Plr2_Echo_b = Mem.alloc(1);
    public static final int Plr2_Fire_b = Mem.alloc(1);

    public static final int Plr2_Clicked_b = Mem.alloc(1);
    public static final int Plr2_Used_b = Mem.alloc(1);
    public static final int Plr2_TmpClicked_b = Mem.alloc(1);
    public static final int Plr2_TmpSpcTap_b = Mem.alloc(1);

    public static final int Plr2_TmpGunSelected_b = Mem.alloc(1); // _Plr2_TmpGunSelected_b::
    public static final int Plr2_TmpFire_b = Mem.alloc(1);

    // Private fields
    public static final int plr2_Teleported_b = Mem.alloc(1);    // hires.s
    public static final int plr2_Dead_b = Mem.alloc(1);          // hires.s

    public static final int plr2_TmpDucked_b = Mem.alloc(1);     // hires.s
    public static final int plr2_StoodOnLift_b = Mem.alloc(1);   // newanims.s

    public static final int plr2_Reserved1_b = Mem.alloc(1);
    public static final int plr2_Reserved2_b = Mem.alloc(1);

    // aligned 4
    public static final int Plr2_ObjectDistances_vw = Mem.alloc(2 * Defs.MAX_LEVEL_OBJ_DIST_COUNT);
    public static final int Plr2_ObsInLine_vb = Mem.alloc(Defs.MAX_OBJS_IN_LINE_COUNT);

    // READY PLAYER whoever...
    private static final int _align2 = Mem.align(4);
    public static final int Plr_ShotDataPtr_l = Mem.alloc(4);

    // Private fields
    public static final int plr_JumpSpeed_l = Mem.alloc(4);      // fall.s
    public static final int plr_OldHeight_l = Mem.alloc(4);      // fall.s

    public static final int Plr_AddToBobble_w = Mem.alloc(2);

    // Word fields
    // Private
    public static final int plr_FallDamage_w = Mem.alloc(2);     // fall.s

    /** CHAR enum - m(aster), s(lave), n(either) — _Plr_MultiplayerType_b:: */
    public static final int Plr_MultiplayerType_b = Mem.alloc(1);
    public static final int Plr_Decelerate_b = Mem.alloc(1);

    // Byte fields
    // Private
    public static final int plr_CanJump_b = Mem.alloc(1);
    public static final int plr_GunSelected_b = Mem.alloc(1);
    public static final int plr_PrevNextWeaponKeyState_b = Mem.alloc(1);
    public static final int plr_PrevUseKeyState_b = Mem.alloc(1);

    public static final int Plr_Health_w = Mem.alloc(2 * 2);
    public static final int Plr_AmmoCounts_vw = Mem.alloc(2 * 20);
    public static final int Plr_Shield_w = Mem.alloc(2 * 2);
    public static final int Plr_Weapons_vw = Mem.alloc(2 * 10);
    public static final int Plr_TurnSpeed_w = Mem.alloc(2);
    public static final int BIGsmall = Mem.alloc(1);
    public static final int lastscr = Mem.alloc(1);

    private PlayerBss() {
    }
}
