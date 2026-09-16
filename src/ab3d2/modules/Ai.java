package ab3d2.modules;

import ab3d2.Hires;
import ab3d2.HiresData;
import ab3d2.Macros;
import ab3d2.Mem;
import ab3d2.Newaliencontrol;
import ab3d2.NewaliencontrolData;
import ab3d2.Newanims;
import ab3d2.Objectmove;
import ab3d2.ObjectmoveData;
import ab3d2.bss.GameBss;
import ab3d2.c.Message;
import ab3d2.data.TablesData;

import static ab3d2.Defs.*;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;
import static ab3d2.bss.AiBss.*;
import static ab3d2.bss.AnimBss.Anim_BrightY_l;
import static ab3d2.bss.AnimBss.Anim_SplatType_w;
import static ab3d2.bss.AnimBss.Anim_TempFrames_w;
import static ab3d2.bss.LevelBss.Lvl_ControlPointCoordsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_DataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_NumControlPoints_w;
import static ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l;
import static ab3d2.bss.PlayerBss.*;
import static ab3d2.bss.TablesBss.ObjRotated_vl;
import static ab3d2.bss.TablesBss.WorkspacePtr_l;
import static ab3d2.data.TablesData.COSINE_OFS;
import static ab3d2.data.TablesData.SinCosTable_vw;

/**
 * Traduction littérale de ab3d2_source/modules/ai.s
 *
 * "Definitions specific to game AI — Refactored from airoutine.s"
 *
 * a0 = entité alien (ObjT/EntT, 64 octets). Le premier mot de l'entité est
 * l'index de son point dans Lvl_ObjectPointsPtr_l (entrées de 8 octets),
 * 4(a0) = altitude (word), 12(a0) = ObjT_ZoneID_w.
 *
 * Workspace (WorkspacePtr_l, posé par newaliencontrol.s) :
 *   (a5)   = action à exécuter (morsure/tir) pour la frame d'anim courante
 *   1(a5)  = frame spéciale forcée (-1/$FF = aucune ; st = consommée)
 *   2(a5)  = viewpoint forcé
 *   3(a5)  = drapeau fin d'animation
 */
public final class Ai {

    // Workspace structure — STRUCTURE AI_WorkT,0
    public static final int AI_WorkT_LastX_w = 0;
    public static final int AI_WorkT_LastY_w = 2;
    public static final int AI_WorkT_LastZone_w = 4;
    public static final int AI_WorkT_LastControlPoint_w = 6;
    public static final int AI_WorkT_SeenBy_w = 8;
    public static final int AI_WorkT_DamageDone_w = 10;
    public static final int AI_WorkT_DamageTaken_w = 12;

    private Ai() {
    }

    /** AI_MainRoutine — dispatch selon EntT_CurrentMode_b. */
    public static void AI_MainRoutine(int a0) {
        Mem.ww(a0 + 2, -20);                           // move.w #-20,2(a0)

        int mode = Mem.b(a0 + EntT_CurrentMode_b);
        if ((byte) mode < 1) {                         // cmp.b #1,EntT_CurrentMode_b(a0) ; blt ai_DoDefault
            ai_DoDefault(a0);
            return;
        }
        if ((byte) mode == 1) {                        // beq ai_DoResponse
            ai_DoResponse(a0);
            return;
        }
        if ((byte) mode < 3) {                         // cmp.b #3,... ; blt ai_DoFollowup
            ai_DoFollowup(a0);
            return;
        }
        if ((byte) mode == 3) {                        // beq ai_DoRetreat
            ai_DoRetreat(a0);
            return;
        }
        if ((byte) mode == 5) {                        // cmp.b #5,... ; beq ai_DoDie
            ai_DoDie(a0);
            return;
        }
        ai_DoTakeDamage(a0);                           // (chute : mode 4)
    }

    /** ai_DoTakeDamage */
    private static void ai_DoTakeDamage(int a0) {
        ai_DoWalkAnim(a0);                             // jsr ai_DoWalkAnim
        int saved4 = Mem.uw(a0 + 4);                   // move.w 4(a0),-(a7)
        ai_GetRoomStatsStill(a0);                      // bsr ai_GetRoomStatsStill

        int d0 = saved4;                               // move.w (a7)+,d0
        if (Mem.w(AI_DefaultMode_w) >= 1) {            // cmp.w #1,AI_DefaultMode_w ; blt .not_flying
            Mem.ww(a0 + 4, d0);                        // move.w d0,4(a0)
        }

        // .not_flying:
        if (Mem.b(ai_FinishedAnim_b) != 0) {           // tst.b ai_FinishedAnim_b ; beq.s .still_hurting
            Mem.wb(a0 + EntT_CurrentMode_b, 0);        // move.b #0,EntT_CurrentMode_b(a0)
            Mem.wb(a0 + EntT_WhichAnim_b, 0);          // move.b #0,EntT_WhichAnim_b(a0)
            Mem.ww(a0 + EntT_Timer2_w, 0);             // move.w #0,EntT_Timer2_w(a0)
        }

        // .still_hurting:
        ai_DoTorch(a0);                                // bsr ai_DoTorch

        if (Mem.w(a0 + ObjT_ZoneID_w + ENT_PREV) >= 0) { // tst.w ObjT_ZoneID_w+ENT_PREV(a0) ; blt.s .no_copy_in
            Mem.ww(a0 + ObjT_ZoneID_w + ENT_PREV, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),...
            Mem.ww(a0 + EntT_ZoneID_w + ENT_PREV, Mem.uw(a0 + EntT_ZoneID_w)); // move.w EntT_ZoneID_w(a0),...
        }

        // .no_copy_in:
        // SAVEREGS — locaux en Java
        Mem.ww(ObjectmoveData.newx, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,newz
        int d1 = Mem.uw(a0);                           // move.w (a0),d1
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1
        Mem.ww(ObjectmoveData.oldx, Mem.uw(a1));       // move.w (a1),oldx
        Mem.ww(ObjectmoveData.oldz, Mem.uw(a1 + 4));   // move.w 4(a1),oldz
        Mem.ww(ObjectmoveData.Range, -20);             // move.w #-20,Range
        Mem.ww(ObjectmoveData.speed, 20);              // move.w #20,speed
        Objectmove.HeadTowardsAng();                   // jsr HeadTowardsAng

        d0 = Mem.uw(ObjectmoveData.AngRet);            // move.w AngRet,d0
        d0 = setw(d0, d0 + Mem.uw(ai_AnimFacing_w));   // add.w ai_AnimFacing_w,d0
        Mem.ww(a0 + EntT_CurrentAngle_w, d0);          // move.w d0,EntT_CurrentAngle_w(a0)
        // GETREGS ; rts
    }

    /** ai_DoDie */
    private static void ai_DoDie(int a0) {
        ai_DoWalkAnim(a0);                             // jsr ai_DoWalkAnim
        ai_GetRoomStatsStill(a0);                      // bsr ai_GetRoomStatsStill

        if (Mem.b(ai_FinishedAnim_b) != 0) {           // tst.b ai_FinishedAnim_b ; beq.s .still_dying
            Macros.FREE_ENT(a0);                       // FREE_ENT a0
            Mem.wb(a0 + ObjT_TypeID_b, OBJ_TYPE_ALIEN); // move.b #OBJ_TYPE_ALIEN,ObjT_TypeID_b(a0)
            Mem.wb(a0 + ShotT_Worry_b, 0);             // clr.b ShotT_Worry_b(a0)
            Mem.wb(ai_GetOut_w, 0xFF);                 // st ai_GetOut_w
        }

        // .still_dying:
        Mem.wb(a0 + EntT_HitPoints_b, 0);              // move.b #0,EntT_HitPoints_b(a0)
        if (Mem.w(a0 + ObjT_ZoneID_w + ENT_PREV) >= 0) { // tst.w ObjT_ZoneID_w+ENT_PREV(a0) ; blt.s .no_copy_in
            Mem.ww(a0 + ObjT_ZoneID_w + ENT_PREV, Mem.uw(a0 + ObjT_ZoneID_w));
            Mem.ww(a0 + EntT_ZoneID_w + ENT_PREV, Mem.uw(a0 + EntT_ZoneID_w));
        }
        // .no_copy_in: rts
    }

    /** ai_TakeDamage — l'appelant teste ai_GetOut_w au retour. */
    private static void ai_TakeDamage(int a0) {
        Mem.wb(ai_GetOut_w, 0);                        // clr.b ai_GetOut_w
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_DamageTaken_b)); // move.b EntT_DamageTaken_b(a0),d0
        int a2 = Mem.l(AI_DamagePtr_l);                // move.l AI_DamagePtr_l,a2
        Mem.ww(a2, Mem.uw(a2) + d0);                   // add.w d0,(a2)
        d0 = setw(d0, Mem.uw(a2));                     // move.w (a2),d0
        d0 = setw(d0, ((short) d0) >> 2);              // asr.w #2,d0 ; divide by 4
        int d1 = 0;                                    // moveq #0,d1
        d1 = setb(d1, Mem.ub(a0 + EntT_HitPoints_b));  // move.b EntT_HitPoints_b(a0),d1
        Mem.wb(a0 + EntT_DamageTaken_b, 0);            // move.b #0,EntT_DamageTaken_b(a0)
        if ((short) d1 <= (short) d0) {                // cmp.w d0,d1 ; ble ai_JustDied
            ai_JustDied(a0);
            return;
        }

        Mem.ww(a0 + EntT_Timer1_w, 0);                 // move.w #0,EntT_Timer1_w(a0)
        Mem.ww(a0 + EntT_Timer2_w, 0);                 // move.w #0,EntT_Timer2_w(a0)
        d0 = Objectmove.GetRand();                     // jsr GetRand

        if ((d0 & 3) != 0) {                           // and.w #3,d0 ; beq.s .dodododo
            int a5 = Mem.l(WorkspacePtr_l);            // move.l WorkspacePtr_l,a5
            Mem.wb(a5 + 1, 0xFF);                      // st 1(a5)
            Mem.wb(a0 + EntT_CurrentMode_b, 1);        // move.b #1,EntT_CurrentMode_b(a0)
            Mem.ww(a0 + EntT_Timer2_w, 0);             // move.w #0,EntT_Timer2_w(a0)
            Mem.ww(a0 + EntT_Timer1_w, 0);             // move.w #0,EntT_Timer1_w(a0)
            Mem.wb(a0 + EntT_WhichAnim_b, 1);          // move.b #1,EntT_WhichAnim_b(a0)
            d0 = setw(d0, Mem.uw(a0));                 // move.w (a0),d0
            int a1 = Mem.l(Lvl_ObjectPointsPtr_l);     // move.l Lvl_ObjectPointsPtr_l,a1
            Mem.ww(ObjectmoveData.oldx, Mem.uw(a1 + ((short) d0) * 8));     // move.w (a1,d0.w*8),oldx
            Mem.ww(ObjectmoveData.oldz, Mem.uw(a1 + ((short) d0) * 8 + 4)); // move.w 4(a1,d0.w*8),oldz
            Mem.ww(ObjectmoveData.newx, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,newx
            Mem.ww(ObjectmoveData.newz, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,newz
            Mem.ww(ObjectmoveData.speed, 100);         // move.w #100,speed
            Mem.ww(ObjectmoveData.Range, -20);         // move.w #-20,Range
            Objectmove.HeadTowardsAng();               // jsr HeadTowardsAng

            Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(ObjectmoveData.AngRet)); // move.w AngRet,EntT_CurrentAngle_w(a0)
            Mem.wb(ai_GetOut_w, 0xFF);                 // st ai_GetOut_w
            return;                                    // rts
        }

        // .dodododo:
        Mem.wb(a0 + EntT_CurrentMode_b, 4);            // move.b #4,EntT_CurrentMode_b(a0) ; do take damage.
        Mem.wb(a0 + EntT_WhichAnim_b, 2);              // move.b #2,EntT_WhichAnim_b(a0) ; get hit anim.
        int a5 = Mem.l(WorkspacePtr_l);                // move.l WorkspacePtr_l,a5
        Mem.wb(a5 + 1, 0xFF);                          // st 1(a5)
        Mem.wb(ai_GetOut_w, 0xFF);                     // st ai_GetOut_w
        // rts
    }

    /** ai_JustDied */
    private static void ai_JustDied(int a0) {
        Mem.wb(a0 + EntT_HitPoints_b, 0);              // move.b #0,EntT_HitPoints_b(a0)
        int d0 = Mem.w(a0 + EntT_DisplayText_w);       // move.w EntT_DisplayText_w(a0),d0
        if (d0 >= 0) {                                 // blt.s .no_text
            d0 = muls(d0, LVLT_MESSAGE_LENGTH);        // muls #LVLT_MESSAGE_LENGTH,d0
            d0 += Mem.l(Lvl_DataPtr_l);                // add.l Lvl_DataPtr_l,d0
            // move.l a0,-(sp) ; ... ; move.l (sp)+,a0
            Message.Msg_PushLine(d0, LVLT_MESSAGE_LENGTH | MSG_TAG_NARRATIVE); // CALLC Msg_PushLine
        }

        // .no_text:
        int a2 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a2
        int d3 = Mem.uw(a0);                           // move.w (a0),d3
        Mem.ww(ObjectmoveData.newx, Mem.uw(a2 + ((short) d3) * 8));     // move.w (a2,d3.w*8),newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(a2 + ((short) d3) * 8 + 4)); // move.w 4(a2,d3.w*8),newz
        d0 = 0;                                        // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0

        // Record the (messy) kill... — STATS_KILL (expects EntT_Type_b in d0, trashes a1)
        int sa1 = GameBss.game_PlayerProgression + GStatT_AlienKills_vw; // move.l #...,a1
        Mem.ww(sa1 + ((short) d0) * 2, Mem.uw(sa1 + ((short) d0) * 2) + 1); // add.w #1,(a1,d0.w*2)
        Mem.wl(GameBss.Game_ProgressSignal_l, 1);      // move.l #1,Game_ProgressSignal_l
        Macros.SET_MEM_BIT(GAME_EVENTBIT_KILL, GameBss.Game_ProgressSignal_l); // SET_MEM_BIT

        d0 = muls(d0, AlienT_SizeOf_l);                // muls #AlienT_SizeOf_l,d0
        a2 = Mem.l(HiresData.GLF_DatabasePtr_l);       // move.l GLF_DatabasePtr_l,a2
        a2 = a2 + GLFT_AlienDefs_l;                    // lea GLFT_AlienDefs_l(a2),a2
        a2 += d0;                                      // add.l d0,a2
        d0 = setb(d0, Mem.ub(a2 + AlienT_SplatType_w + 1)); // move.b AlienT_SplatType_w+1(a2),d0
        Mem.wb(Anim_SplatType_w, d0);                  // move.b d0,Anim_SplatType_w
        if ((byte) d0 < NUM_BULLET_DEFS) {             // cmp.b #NUM_BULLET_DEFS,d0 ; blt .go_splutch
            // .go_splutch:
            int d2 = 8;                                // move.w #8,d2
            Newanims.Anim_ExplodeIntoBits(d0, d2, d3, a0); // jsr Anim_ExplodeIntoBits (a0 = entité)
        } else {
            // Spawned alien type 0-19
            Mem.wb(Anim_SplatType_w, Mem.ub(Anim_SplatType_w) - NUM_BULLET_DEFS); // sub.b #NUM_BULLET_DEFS,Anim_SplatType_w
            d0 = setb(d0, d0 - NUM_BULLET_DEFS);       // sub.b #NUM_BULLET_DEFS,d0
            d0 = setw(d0, (byte) d0);                  // ext.w d0
            a2 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a2
            a2 += GLFT_AlienDefs_l;                    // add.l #GLFT_AlienDefs_l,a2
            d0 = muls(d0, AlienT_SizeOf_l);            // muls #AlienT_SizeOf_l,d0
            a2 += d0;                                  // add.l d0,a2
            int a4 = a2;                               // move.l a2,a4

            // * Spawn some smaller aliens...
            int d7 = 2;                                // move.w #2,d7 ; number to do.
            a2 = Mem.l(AI_OtherAlienDataPtrs_vl);      // move.l AI_OtherAlienDataPtrs_vl,a2
            a2 += ObjT_SizeOf_l;                       // NEXT_OBJ a2
            int a1 = Mem.l(Lvl_ObjectPointsPtr_l);     // move.l Lvl_ObjectPointsPtr_l,a1
            int d1 = Mem.uw(a0);                       // move.w (a0),d1
            d0 = Mem.l(a1 + ((short) d1) * 8);         // move.l (a1,d1.w*8),d0
            d1 = Mem.l(a1 + ((short) d1) * 8 + 4);     // move.l 4(a1,d1.w*8),d1
            d3 = 9;                                    // move.w #9,d3

            spawn_loop:
            while (true) { // .spawn_loop: / .find_one_free:
                while (true) {
                    int d2 = Mem.w(a2 + ObjT_ZoneID_w); // move.w ObjT_ZoneID_w(a2),d2
                    if (d2 < 0) {                      // blt.s .found_one_free
                        break;
                    }
                    if (Mem.b(a2 + EntT_HitPoints_b) == 0) { // tst.b EntT_HitPoints_b(a2) ; beq.s .found_one_free
                        break;
                    }
                    a2 += ENT_NEXT_2;                  // adda.w #ENT_NEXT_2,a2 ; todo - why two slots here?
                    d3 = setw(d3, d3 - 1);             // dbra d3,.find_one_free
                    if ((short) d3 == -1) {
                        break spawn_loop;              // bra .cant_shoot
                    }
                }

                // .found_one_free:
                Mem.wb(a2 + EntT_HitPoints_b, Mem.ub(a4 + AlienT_HitPoints_w + 1)); // move.b AlienT_HitPoints_w+1(a4),EntT_HitPoints_b(a2)
                Mem.wb(a2 + EntT_Type_b, Mem.ub(Anim_SplatType_w)); // move.b Anim_SplatType_w,EntT_Type_b(a2)
                Mem.wb(a2 + EntT_DisplayText_w, -1);   // move.b #-1,EntT_DisplayText_w(a2)
                Mem.wb(a2 + 16, 0);                    // move.b #0,16(a2)
                int d4 = Mem.uw(a2);                   // move.w (a2),d4
                Mem.wl(a1 + ((short) d4) * 8, d0);     // move.l d0,(a1,d4.w*8)
                Mem.wl(a1 + ((short) d4) * 8 + 4, d1); // move.l d1,4(a1,d4.w*8)
                Mem.ww(a2 + 4, Mem.uw(a0 + 4));        // move.w 4(a0),4(a2)
                Mem.ww(a2 + ObjT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),ObjT_ZoneID_w(a2)
                Mem.ww(a2 + EntT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),EntT_ZoneID_w(a2)
                Mem.ww(a2 + ObjT_ZoneID_w + ENT_PREV, -1); // move.w #-1,ObjT_ZoneID_w+ENT_PREV(a2)
                Mem.ww(a2 + EntT_CurrentControlPoint_w, Mem.uw(a0 + EntT_CurrentControlPoint_w)); // move.w ...
                Mem.ww(a2 + EntT_TargetControlPoint_w, Mem.uw(a0 + EntT_CurrentControlPoint_w));  // move.w ...
                Mem.wb(a2 + EntT_TeamNumber_b, -1);    // move.b #-1,EntT_TeamNumber_b(a2)
                Mem.ww(a2 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w)); // move.w ...
                Mem.wb(a2 + EntT_CurrentMode_b, 0);    // move.b #0,EntT_CurrentMode_b(a2)
                Mem.wb(a2 + EntT_WhichAnim_b, 0);      // move.b #0,EntT_WhichAnim_b(a2)
                Mem.ww(a2 + EntT_Timer2_w, 0);         // move.w #0,EntT_Timer2_w(a2)
                Mem.wb(a2 + EntT_DamageTaken_b, 0);    // move.b #0,EntT_DamageTaken_b(a2)
                Mem.ww(a2 + EntT_Timer1_w, 0);         // move.w #0,EntT_Timer1_w(a2)
                Mem.ww(a2 + EntT_ImpactX_w, 0);        // move.w #0,EntT_ImpactX_w(a2)
                Mem.ww(a2 + EntT_ImpactZ_w, 0);        // move.w #0,EntT_ImpactZ_w(a2)
                Mem.ww(a2 + EntT_ImpactY_w, 0);        // move.w #0,EntT_ImpactY_w(a2)
                Mem.wb(a2 + EntT_HitPoints_b, Mem.ub(a4 + AlienT_HitPoints_w + 1)); // move.b ... (doublon de l'original)
                Mem.wb(a2 + EntT_DamageTaken_b, 0);    // move.b #0,EntT_DamageTaken_b(a2)
                Mem.wl(a2 + EntT_DoorsAndLiftsHeld_l, Mem.l(a0 + EntT_DoorsAndLiftsHeld_l)); // move.l ...
                Mem.wb(a2 + ShotT_InUpperZone_b, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ...
                Mem.wb(a2 + ObjT_TypeID_b + ENT_PREV, OBJ_TYPE_AUX); // move.b #OBJ_TYPE_AUX,ObjT_TypeID_b+ENT_PREV(a2)
                d7 = setw(d7, d7 - 1);                 // dbra d7,.spawn_loop
                if ((short) d7 == -1) {
                    break;
                }
            }
            // .cant_shoot: bra .spawned
        }

        // .spawned:
        Mem.wb(a0 + EntT_CurrentMode_b, 5);            // move.b #5,EntT_CurrentMode_b(a0)
        Mem.wb(a0 + EntT_WhichAnim_b, 3);              // move.b #3,EntT_WhichAnim_b(a0)
        Mem.ww(a0 + EntT_Timer2_w, 0);                 // move.w #0,EntT_Timer2_w(a0)
        int a5 = Mem.l(WorkspacePtr_l);                // move.l WorkspacePtr_l,a5
        Mem.wb(a5 + 1, 0xFF);                          // st 1(a5)
        Mem.wb(ai_GetOut_w, 0xFF);                     // st ai_GetOut_w
        // rts
    }

    /** ai_DoRetreat: rts */
    private static void ai_DoRetreat(int a0) {
    }

    /** ai_DoDefault */
    private static void ai_DoDefault(int a0) {
        if (Mem.w(AI_DefaultMode_w) < 1) {             // cmp.w #1,AI_DefaultMode_w ; blt ai_ProwlRandom
            ai_ProwlRandom(a0);
            return;
        }
        if (Mem.w(AI_DefaultMode_w) == 1) {            // beq ai_ProwlRandomFlying
            ai_ProwlRandomFlying(a0);
        }
        // rts
    }

    /** ai_DoResponse */
    private static void ai_DoResponse(int a0) {
        if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_AI_ATTACK)) { // DEV_CHECK_SET SKIP_AI_ATTACK,ai_DoDefault
            ai_DoDefault(a0);
            return;
        }
        int m = Mem.w(AI_ResponseMode_w);
        if (m < 1) {                                   // cmp.w #1,AI_ResponseMode_w ; blt ai_Charge
            ai_Charge(a0);
            return;
        }
        if (m == 1) {                                  // beq ai_ChargeToSide
            ai_ChargeToSide(a0);
            return;
        }
        if (m < 3) {                                   // cmp.w #3,... ; blt ai_AttackWithGun
            ai_AttackWithGun(a0);
            return;
        }
        if (m == 3) {                                  // beq ai_ChargeFlying
            ai_ChargeFlying(a0);
            return;
        }
        if (m < 5) {                                   // cmp.w #5,... ; blt ai_ChargeToSideFlying
            ai_ChargeToSideFlying(a0);
            return;
        }
        if (m == 5) {                                  // beq ai_AttackWithGunFlying
            ai_AttackWithGunFlying(a0);
        }
        // rts
    }

    /** ai_DoFollowup */
    private static void ai_DoFollowup(int a0) {
        int m = Mem.w(AI_FollowupMode_w);
        if (m < 1) {                                   // cmp.w #1,AI_FollowupMode_w ; blt ai_PauseBriefly
            ai_PauseBriefly(a0);
            return;
        }
        if (m == 1) {                                  // beq ai_Approach
            ai_Approach(a0);
            return;
        }
        if (m < 3) {                                   // blt ai_ApproachToSide
            ai_ApproachToSide(a0);
            return;
        }
        if (m == 3) {                                  // beq ai_ApproachFlying
            ai_ApproachFlying(a0);
            return;
        }
        if (m < 5) {                                   // blt ai_ApproachToSideFlying
            ai_ApproachToSideFlying(a0);
        }
        // rts
    }

    // ***************************************************
    // *** DEFAULT MOVEMENTS *****************************
    // ***************************************************

    /** ai_ProwlRandomFlying */
    private static void ai_ProwlRandomFlying(int a0) {
        Mem.wl(ObjectmoveData.StepDownVal, 1000 * 256); // move.l #1000*256,StepDownVal
        Mem.wb(AI_FlyABit_w, 0xFF);                    // st AI_FlyABit_w
        ai_ProwlFly(a0);                               // bra ai_ProwlFly
    }

    /** ai_ProwlRandom */
    private static void ai_ProwlRandom(int a0) {
        Mem.wb(AI_FlyABit_w, 0);                       // clr.b AI_FlyABit_w
        Mem.wl(ObjectmoveData.StepDownVal, 30 * 256);  // move.l #30*256,StepDownVal
        ai_ProwlFly(a0);
    }

    /** ai_ProwlFly */
    private static void ai_ProwlFly(int a0) {
        Mem.wl(ObjectmoveData.StepUpVal, 20 * 256);    // move.l #20*256,StepUpVal
        if (Mem.b(a0 + EntT_DamageTaken_b) != 0) {     // tst.b EntT_DamageTaken_b(a0) ; beq.s .no_damage
            ai_TakeDamage(a0);                         // bsr ai_TakeDamage
            if (Mem.b(ai_GetOut_w) != 0) {             // tst.b ai_GetOut_w ; beq.s .no_damage
                return;                                // rts
            }
        }

        // .no_damage:
        ai_DoWalkAnim(a0);                             // jsr ai_DoWalkAnim

        int a1 = Mem.l(AI_BoredomPtr_l);               // move.l AI_BoredomPtr_l,a1
        int d1 = Mem.uw(a1 + 2);                       // move.w 2(a1),d1
        int d2 = Mem.uw(a1 + 4);                       // move.w 4(a1),d2
        int a2 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a2
        int d3 = Mem.uw(a0);                           // move.w (a0),d3
        int d4 = Mem.uw(a2 + ((short) d3) * 8 + 4);    // move.w 4(a2,d3.w*8),d4
        d3 = setw(d3, Mem.uw(a2 + ((short) d3) * 8));  // move.w (a2,d3.w*8),d3
        int d5 = setw(0, d3);                          // move.w d3,d5
        int d6 = setw(0, d4);                          // move.w d4,d6
        d3 = setw(d3, d3 - d1);                        // sub.w d1,d3
        if ((short) d3 < 0) {                          // bge.s .okp1
            d3 = setw(d3, -(short) d3);                // neg.w d3
        }
        // .okp1:
        d4 = setw(d4, d4 - d2);                        // sub.w d2,d4
        if ((short) d4 < 0) {                          // bge.s .okp2
            d4 = setw(d4, -(short) d4);                // neg.w d4
        }
        // .okp2:
        d4 = setw(d4, d4 + d3);                        // add.w d3,d4 ; dist away
        if ((short) d4 >= 50) {                        // cmp.w #50,d4 ; blt.s .no_new_store
            Mem.ww(a1 + 2, d5);                        // move.w d5,2(a1)
            Mem.ww(a1 + 4, d6);                        // move.w d6,4(a1)
            Mem.ww(a1, 100);                           // move.w #100,(a1)
            // bra .new_store
        } else {
            // .no_new_store:
            Mem.ww(a1, Mem.uw(a1) - 1);                // sub.w #1,(a1)
            if (Mem.w(a1) <= 0) {                      // bgt.s .new_store
                ai_GetRoomCPT(a0);                     // bsr ai_GetRoomCPT
                int d0 = Objectmove.GetRand();         // jsr GetRand
                d1 = 0;                                // moveq #0,d1
                d1 = setw(d1, d0);                     // move.w d0,d1
                d1 = M68kDivsMem(d1, Lvl_NumControlPoints_w); // divs.w Lvl_NumControlPoints_w,d1
                d1 = swap(d1);                         // swap d1 (reste de division = cpt aléatoire)
                int d7 = 7;                            // move.w #7,d7

                while (true) { // .try_again:
                    Mem.ww(a0 + EntT_TargetControlPoint_w, d1); // move.w d1,EntT_TargetControlPoint_w(a0)
                    d0 = Mem.uw(a0 + EntT_CurrentControlPoint_w); // move.w EntT_CurrentControlPoint_w(a0),d0
                    d0 = Objectmove.GetNextCPt(d0, d1);  // jsr GetNextCPt
                    if ((short) d0 != Mem.w(a0 + EntT_CurrentControlPoint_w) // cmp.w ... ; beq.s .plus_again
                            && (byte) d0 != 0x7f) {    // cmp.b #$7f,d0 ; bne.s .okaway2
                        break;                         // .okaway2
                    }
                    // .plus_again:
                    d1 = setw(d1, Mem.uw(a0 + EntT_TargetControlPoint_w)); // move.w EntT_TargetControlPoint_w(a0),d1
                    d1 = setw(d1, d1 + 1);             // add.w #1,d1
                    if ((short) d1 >= Mem.w(Lvl_NumControlPoints_w)) { // cmp.w Lvl_NumControlPoints_w,d1 ; blt .no_bin
                        d1 = 0;                        // moveq #0,d1
                    }
                    // .no_bin:
                    d7 = setw(d7, d7 - 1);             // dbra d7,.try_again
                    if ((short) d7 == -1) {
                        break;
                    }
                }
                // .okaway2:
                Mem.ww(a1, 50);                        // move.w #50,(a1)
            }
        }

        // .new_store: / ai_Widget:
        if (Mem.w(Plr1_NoiseVol_w) != 0) {             // tst.w Plr1_NoiseVol_w ; beq.s .no_player_noise
            a1 = Mem.l(Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a1
            if (Mem.b(Plr1_StoodInTop_b) != 0) {       // tst.b Plr1_StoodInTop_b ; beq.s .player_not_in_top
                a1 += 1;                               // addq #1,a1
            }
            // .player_not_in_top:
            d1 = 0;                                    // moveq #0,d1
            d1 = setb(d1, Mem.ub(a1 + ZoneT_ControlPoint_w)); // move.b ZoneT_ControlPoint_w(a1),d1

            int d0 = Mem.uw(a0 + EntT_CurrentControlPoint_w); // move.w EntT_CurrentControlPoint_w(a0),d0
            d0 = Objectmove.GetNextCPt(d0, d1);        // jsr GetNextCPt

            if ((byte) d0 == 0x7f) {                   // cmp.b #$7f,d0 ; bne.s .okaway
                d0 = setw(d0, Mem.uw(a0 + EntT_CurrentControlPoint_w)); // move.w EntT_CurrentControlPoint_w(a0),d0
            }
            // .okaway:
            Mem.ww(a0 + EntT_TargetControlPoint_w, d0); // move.w d0,EntT_TargetControlPoint_w(a0)
        }

        // .no_player_noise:
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_TeamNumber_b)); // move.b EntT_TeamNumber_b(a0),d0
        boolean notSeenDone = false;
        if ((byte) d0 >= 0) {                          // blt.s .no_team
            a2 = AI_AlienTeamWorkspace_vl;             // move.l #AI_AlienTeamWorkspace_vl,a2
            d0 = setw(d0, d0 << 4);                    // asl.w #4,d0
            a2 += (short) d0;                          // add.w d0,a2
            if (Mem.w(a2 + AI_WorkT_SeenBy_w) >= 0) {  // tst.w AI_WorkT_SeenBy_w(a2) ; blt.s .no_team
                d0 = setw(d0, Mem.uw(a0));             // move.w (a0),d0
                if ((short) d0 == Mem.w(a2 + AI_WorkT_SeenBy_w)) { // cmp.w AI_WorkT_SeenBy_w(a2),d0 ; bne.s .no_remove
                    Mem.ww(a2 + AI_WorkT_SeenBy_w, -1); // move.w #-1,AI_WorkT_SeenBy_w(a2)
                    // bra.s .no_team (chute dans .no_team ci-dessous)
                } else {
                    // .no_remove:
                    d0 = setw(d0, d0 << 4);            // asl.w #4,d0
                    a1 = ai_AlienWorkspace_vl;         // move.l #ai_AlienWorkspace_vl,a1
                    a1 += (short) d0;                  // add.w d0,a1
                    Mem.ww(a1 + AI_WorkT_DamageDone_w, 0);  // move.w #0,AI_WorkT_DamageDone_w(a1)
                    Mem.ww(a1 + AI_WorkT_DamageTaken_w, 0); // move.w #0,AI_WorkT_DamageTaken_w(a1)
                    Mem.wl(a1, Mem.l(a2));             // move.l (a2),(a1)
                    Mem.wl(a1 + 4, Mem.l(a2 + 4));     // move.l 4(a2),4(a1)
                    Mem.wl(a1 + 8, Mem.l(a2 + 8));     // move.l 8(a2),8(a1)
                    Mem.wl(a1 + 12, Mem.l(a2 + 12));   // move.l 12(a2),12(a1)
                    Mem.ww(a0 + EntT_TargetControlPoint_w, Mem.uw(a1 + AI_WorkT_LastControlPoint_w)); // move.w ...
                    Mem.ww(a1 + AI_WorkT_LastZone_w, -1); // move.w #-1,AI_WorkT_LastZone_w(a1)
                    notSeenDone = true;                // bra.s .not_seen
                }
            }
        }
        if (!notSeenDone) {
            // .no_team:
            d0 = setw(d0, Mem.uw(a0));                 // move.w (a0),d0
            d0 = setw(d0, d0 << 4);                    // asl.w #4,d0
            a1 = ai_AlienWorkspace_vl;                 // move.l #ai_AlienWorkspace_vl,a1
            a1 += (short) d0;                          // add.w d0,a1
            Mem.ww(a1 + AI_WorkT_DamageDone_w, 0);     // move.w #0,AI_WorkT_DamageDone_w(a1)
            Mem.ww(a1 + AI_WorkT_DamageTaken_w, 0);    // move.w #0,AI_WorkT_DamageTaken_w(a1)
            if (Mem.w(a1 + AI_WorkT_LastZone_w) >= 0) { // tst.w AI_WorkT_LastZone_w(a1) ; blt.s .not_seen
                Mem.ww(a0 + EntT_TargetControlPoint_w, Mem.uw(a1 + AI_WorkT_LastControlPoint_w)); // move.w ...
                Mem.ww(a1 + AI_WorkT_LastZone_w, -1);  // move.w #-1,AI_WorkT_LastZone_w(a1)
            }
        }

        // .not_seen:
        d0 = Mem.uw(a0 + EntT_CurrentControlPoint_w);  // move.w EntT_CurrentControlPoint_w(a0),d0 ; position actuelle
        d1 = Mem.uw(a0 + EntT_TargetControlPoint_w);   // move.w EntT_TargetControlPoint_w(a0),d1
        d0 = Objectmove.GetNextCPt(d0, d1);            // jsr GetNextCPt
        boolean yesRand;
        if ((byte) d0 == 0x7f) {                       // cmp.b #$7f,d0 ; beq.s .yes_rand
            yesRand = true;
        } else if (Mem.b(AI_FlyABit_w) != 0) {         // tst.b AI_FlyABit_w ; bne.s .no_rand
            yesRand = false;
        } else {
            yesRand = Mem.b(ObjectmoveData.ONLYSEE) != 0; // tst.b ONLYSEE ; beq.s .no_rand
        }
        if (yesRand) {
            // .yes_rand:
            d0 = Objectmove.GetRand();                 // jsr GetRand
            d1 = 0;                                    // moveq #0,d1
            d1 = setw(d1, d0);                         // move.w d0,d1
            d1 = M68kDivsMem(d1, Lvl_NumControlPoints_w); // divs.w Lvl_NumControlPoints_w,d1
            d1 = swap(d1);                             // swap d1
            int d7 = 7;                                // move.w #7,d7

            while (true) { // .try_again:
                Mem.ww(a0 + EntT_TargetControlPoint_w, d1); // move.w d1,EntT_TargetControlPoint_w(a0)
                d0 = Mem.uw(a0 + EntT_CurrentControlPoint_w); // move.w EntT_CurrentControlPoint_w(a0),d0
                d0 = Objectmove.GetNextCPt(d0, d1);    // jsr GetNextCPt
                if ((short) d0 != Mem.w(a0 + EntT_CurrentControlPoint_w) // beq.s .plus_again
                        && (byte) d0 != 0x7f) {        // cmp.b #$7f,d0 ; bne.s .okaway2
                    break;
                }
                // .plus_again:
                d1 = setw(d1, Mem.uw(a0 + EntT_TargetControlPoint_w)); // move.w ...,d1
                d1 = setw(d1, d1 + 1);                 // add.w #1,d1
                if ((short) d1 >= Mem.w(Lvl_NumControlPoints_w)) { // blt .no_bin
                    d1 = 0;                            // moveq #0,d1
                }
                // .no_bin:
                d7 = setw(d7, d7 - 1);                 // dbra d7,.try_again
                if ((short) d7 == -1) {
                    break;
                }
            }
        }
        // .okaway2: / .no_rand:
        Mem.ww(ai_MiddleCPT_w, d0);                    // move.w d0,ai_MiddleCPT_w

        a1 = Mem.l(Lvl_ControlPointCoordsPtr_l);       // move.l Lvl_ControlPointCoordsPtr_l,a1
        Mem.ww(ObjectmoveData.newx, Mem.uw(a1 + ((short) d0) * 8));     // move.w (a1,d0.w*8),newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(a1 + ((short) d0) * 8 + 2)); // move.w 2(a1,d0.w*8),newz

        // wobble pseudo-aléatoire autour du point de contrôle
        d0 = setw(d0, d0 << 2);                        // asl.w #2,d0
        d0 = setw(d0, d0 + Mem.uw(a0));                // add.w (a0),d0
        d0 = muls(d0, 0x1347);                         // muls #$1347,d0
        d0 = d0 & 4095;                                // and.w #4095,d0 (résultat long de muls, and.w sur mot faible — bits hauts nuls ensuite via indexation .w)
        a1 = SinCosTable_vw;                           // move.l #SinCosTable_vw,a1
        d1 = Mem.w(a1 + ((short) d0) * 2);             // move.w (a1,d0.w*2),d1
        a1 = SinCosTable_vw + 2048;                    // move.l #SinCosTable_vw+2048,a1
        d2 = Mem.w(a1 + ((short) d0) * 2);             // move.w (a1,d0.w*2),d2
        // ext.l d1 ; ext.l d2 (déjà signés)
        d2 = d2 << 4;                                  // asl.l #4,d2
        d2 = swap(d2);                                 // swap d2
        d1 = d1 << 4;                                  // asl.l #4,d1
        d1 = swap(d1);                                 // swap d1
        Mem.ww(ObjectmoveData.newx, Mem.uw(ObjectmoveData.newx) + d1); // add.w d1,newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(ObjectmoveData.newz) + d2); // add.w d2,newz

        d1 = Mem.uw(a0);                               // move.w (a0),d1
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);             // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1
        // lea (a6,d1.w*8),a6 (ObjRotated — non utilisé ensuite ici)
        Mem.ww(ObjectmoveData.oldx, Mem.uw(a1));       // move.w (a1),oldx
        Mem.ww(ObjectmoveData.oldz, Mem.uw(a1 + 4));   // move.w 4(a1),oldz
        Mem.ww(ObjectmoveData.speed, 0);               // move.w #0,speed
        if (Mem.b(ai_DoAction_b) != 0) {               // tst.b ai_DoAction_b ; beq.s .no_speed
            d2 = 0;                                    // moveq #0,d2
            d2 = setb(d2, Mem.ub(ai_DoAction_b));      // move.b ai_DoAction_b,d2
            d2 = setw(d2, d2 << 2);                    // asl.w #2,d2
            d2 = muls(d2, Mem.w(AI_ProwlSpeed_w));     // muls.w AI_ProwlSpeed_w,d2
            Mem.ww(ObjectmoveData.speed, d2);          // move.w d2,speed
        }

        // .no_speed:
        Mem.ww(ObjectmoveData.Range, 40);              // move.w #40,Range
        d0 = Mem.w(a0 + 4);                            // move.w 4(a0),d0 ; ext.l d0
        d0 <<= 7;                                      // asl.l #7,d0
        d2 = Mem.l(ObjectmoveData.thingheight);        // move.l thingheight,d2
        d2 >>= 1;                                      // asr.l #1,d2
        d0 -= d2;                                      // sub.l d2,d0
        Mem.wl(ObjectmoveData.newy, d0);               // move.l d0,newy
        Mem.wl(ObjectmoveData.oldy, d0);               // move.l d0,oldy

        Mem.wb(ObjectmoveData.StoodInTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a0),StoodInTop
        // movem.l d0/a0/a1/a3/a4/d7,-(a7) — locaux en Java
        Mem.wb(ObjectmoveData.canshove, 0);            // clr.b canshove
        Mem.wb(ObjectmoveData.GotThere, 0);            // clr.b GotThere
        Objectmove.HeadTowardsAng();                   // jsr HeadTowardsAng
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(ObjectmoveData.AngRet)); // move.w AngRet,EntT_CurrentAngle_w(a0)

        if (Mem.b(ObjectmoveData.GotThere) != 0) {     // tst.b GotThere ; beq.s .not_next_cpt
            d0 = Mem.uw(ai_MiddleCPT_w);               // move.w ai_MiddleCPT_w,d0
            Mem.ww(a0 + EntT_CurrentControlPoint_w, d0); // move.w d0,EntT_CurrentControlPoint_w(a0)
            if ((short) d0 == Mem.w(a0 + EntT_TargetControlPoint_w)) { // cmp.w ... ; bne .not_next_cpt
                // We have arrived at the target control pt. Pick a random one and go to that...
                d0 = Objectmove.GetRand();             // jsr GetRand
                d1 = 0;                                // moveq #0,d1
                d1 = setw(d1, d0);                     // move.w d0,d1
                d1 = M68kDivsMem(d1, Lvl_NumControlPoints_w); // divs.w Lvl_NumControlPoints_w,d1
                d1 = swap(d1);                         // swap d1
                Mem.ww(a0 + EntT_TargetControlPoint_w, d1); // move.w d1,EntT_TargetControlPoint_w(a0)
            }
        }

        // .not_next_cpt:
        Mem.ww(ObjectmoveData.wallflags, 0b1000000000); // move.w #%1000000000,wallflags
        Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b00001000110010000010); // move.l #...,Obj_CollideFlags_l
        Objectmove.Obj_DoCollision(a2);                // jsr Obj_DoCollision (a2 hérité : object points ou team workspace)
        if (Mem.b(ObjectmoveData.hitwall) != 0) {      // tst.b hitwall ; beq.s .can_move
            Mem.ww(ObjectmoveData.newx, Mem.uw(ObjectmoveData.oldx)); // move.w oldx,newx
            Mem.ww(ObjectmoveData.newz, Mem.uw(ObjectmoveData.oldz)); // move.w oldz,newz
            // movem.l (a7)+,... ; bra .hit_something
        } else {
            // .can_move:
            Mem.wb(ObjectmoveData.Obj_WallBounce_b, 0); // clr.b Obj_WallBounce_b
            Objectmove.MoveObject();                   // jsr MoveObject
            // movem.l (a7)+,...
            Mem.wb(a0 + ShotT_InUpperZone_b, Mem.ub(ObjectmoveData.StoodInTop)); // move.b StoodInTop,ShotT_InUpperZone_b(a0)
        }

        // .hit_something:
        if (Mem.w(a0 + ObjT_ZoneID_w + ENT_PREV) >= 0) { // tst.w ObjT_ZoneID_w+ENT_PREV(a0) ; blt.s .no_copy_in
            Mem.ww(a0 + ObjT_ZoneID_w + ENT_PREV, Mem.uw(a0 + ObjT_ZoneID_w));
            Mem.ww(a0 + EntT_ZoneID_w + ENT_PREV, Mem.uw(a0 + EntT_ZoneID_w));
        }

        // .no_copy_in:
        int saved4 = Mem.uw(a0 + 4);                   // move.w 4(a0),-(a7)
        ai_GetRoomStats(a0);                           // bsr ai_GetRoomStats
        d0 = saved4;                                   // move.w (a7)+,d0

        if (Mem.b(AI_FlyABit_w) != 0) {                // tst.b AI_FlyABit_w ; beq.s .noflymove
            Mem.ww(a0 + 4, d0);                        // move.w d0,4(a0)
            ai_FlyToCPTHeight(a0);                     // bsr ai_FlyToCPTHeight
        }

        // .noflymove:
        ai_DoTorch(a0);                                // bsr ai_DoTorch

        Mem.wb(a0 + EntT_CurrentMode_b, 0);            // move.b #0,EntT_CurrentMode_b(a0)
        AI_LookForPlayer1(a0);                         // bsr AI_LookForPlayer1
        Mem.wb(a0 + EntT_WhichAnim_b, 0);              // move.b #0,EntT_WhichAnim_b(a0)
        if (Mem.b(a0 + ObjT_SeePlayer_b) != 0) {       // tst.b ObjT_SeePlayer_b(a0) ; beq.s .cant_see_player
            d0 = ai_CheckInFront(a0);                  // bsr ai_CheckInFront
            if ((byte) d0 != 0) {                      // tst.b d0 ; beq.s .cant_see_player
                d0 = Mem.uw(Anim_TempFrames_w);        // move.w Anim_TempFrames_w,d0
                Mem.ww(a0 + EntT_Timer1_w, Mem.w(a0 + EntT_Timer1_w) - d0); // sub.w d0,EntT_Timer1_w(a0)
                if (Mem.w(a0 + EntT_Timer1_w) <= 0) {  // bgt.s .notreacted
                    d0 = ai_CheckForDark(a0);          // bsr ai_CheckForDark
                    if ((byte) d0 != 0) {              // tst.b d0 ; beq.s .cant_see_player
                        // We have seen the player and reacted; can we attack him?
                        boolean attack;
                        if (Mem.b(AI_FlyABit_w) != 0) { // tst.b AI_FlyABit_w ; bne.s .attack_player
                            attack = true;
                        } else if (Mem.w(AI_ResponseMode_w) == 2   // cmp.w #2,AI_ResponseMode_w ; beq.s .attack_player
                                || Mem.w(AI_ResponseMode_w) == 5) { // cmp.w #5,... ; beq.s .attack_player
                            attack = true;
                        } else {
                            d0 = ai_CheckAttackOnGround(a0); // bsr ai_CheckAttackOnGround
                            attack = (byte) d0 != 0;   // tst.b d0 ; bne.s .attack_player
                        }
                        if (attack) {
                            // .attack_player:
                            Mem.ww(a0 + EntT_Timer2_w, 0);      // move.w #0,EntT_Timer2_w(a0)
                            Mem.wb(a0 + EntT_CurrentMode_b, 1); // move.b #1,EntT_CurrentMode_b(a0)
                            Mem.wb(a0 + EntT_WhichAnim_b, 1);   // move.b #1,EntT_WhichAnim_b(a0)
                        } else {
                            // We can see the player
                            ai_StorePlayerPosition(a0); // bsr ai_StorePlayerPosition
                            // but we can't get to him — bra.s .cant_see_player
                            Mem.ww(a0 + EntT_Timer1_w, Mem.uw(AI_ReactionTime_w)); // move.w AI_ReactionTime_w,EntT_Timer1_w(a0)
                        }
                        // .notreacted:
                        d0 = Mem.uw(ai_AnimFacing_w);  // move.w ai_AnimFacing_w,d0
                        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                        return;                        // rts
                    }
                } else {
                    // .notreacted:
                    d0 = Mem.uw(ai_AnimFacing_w);      // move.w ai_AnimFacing_w,d0
                    Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                    return;                            // rts
                }
            }
        }
        // .cant_see_player:
        Mem.ww(a0 + EntT_Timer1_w, Mem.uw(AI_ReactionTime_w)); // move.w AI_ReactionTime_w,EntT_Timer1_w(a0)
        d0 = Mem.uw(ai_AnimFacing_w);                  // move.w ai_AnimFacing_w,d0
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,EntT_CurrentAngle_w(a0)
        // rts
    }

    /** divs.w avec source mémoire (helper local : d = divs(d, w(addr))). */
    private static int M68kDivsMem(int d, int addr) {
        return ab3d2.M68k.divs(d, Mem.w(addr));
    }

    // ***********************************************
    // ** RESPONSE MOVEMENTS *************************
    // ***********************************************

    /** ai_ChargeToSide */
    private static void ai_ChargeToSide(int a0) {
        Mem.wb(AI_FlyABit_w, 0);                       // clr.b AI_FlyABit_w
        Mem.wb(ai_ToSide_w, 0xFF);                     // st ai_ToSide_w
        Mem.wl(ObjectmoveData.StepDownVal, 30 * 256);  // move.l #30*256,StepDownVal
        ai_ChargeCommon(a0);                           // bra ai_ChargeCommon
    }

    /** ai_Charge */
    private static void ai_Charge(int a0) {
        Mem.wb(AI_FlyABit_w, 0);                       // clr.b AI_FlyABit_w
        Mem.wb(ai_ToSide_w, 0);                        // clr.b ai_ToSide_w
        Mem.wl(ObjectmoveData.StepDownVal, 30 * 256);  // move.l #30*256,StepDownVal
        ai_ChargeCommon(a0);
    }

    /** ai_ChargeCommon */
    private static void ai_ChargeCommon(int a0) {
        if (Mem.b(a0 + EntT_DamageTaken_b) != 0) {     // tst.b EntT_DamageTaken_b(a0) ; beq.s .no_damage
            ai_TakeDamage(a0);                         // bsr ai_TakeDamage
            if (Mem.b(ai_GetOut_w) != 0) {             // tst.b ai_GetOut_w ; beq.s .no_damage
                return;                                // rts
            }
        }

        // .no_damage:
        ai_DoAttackAnim(a0);                           // jsr ai_DoAttackAnim

        Mem.ww(ObjectmoveData.FromZone, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),FromZone
        int a2t = Objectmove.CheckTeleport();   // jsr CheckTeleport (a2 = pointeur de zone en sortie)
        boolean gotThere = false;
        boolean noMunch = false;
        if (Mem.b(ObjectmoveData.OKTEL) != 0) {        // tst.b OKTEL ; beq.s .no_teleport
            int d0 = Mem.l(ObjectmoveData.floortemp);  // move.l floortemp,d0
            d0 >>= 7;                                  // asr.l #7,d0
            Mem.ww(a0 + 4, Mem.uw(a0 + 4) + d0);       // add.w d0,4(a0)
            noMunch = true;                            // bra .no_munch
        } else {
            // .no_teleport:
            Mem.ww(ObjectmoveData.newx, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,newx
            Mem.ww(ObjectmoveData.newz, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,newz
            Mem.ww(NewaliencontrolData.tempsin, Mem.uw(Plr1_SinVal_w)); // move.w Plr1_SinVal_w,tempsin
            Mem.ww(NewaliencontrolData.tempcos, Mem.uw(Plr1_CosVal_w)); // move.w Plr1_CosVal_w,tempcos
            Mem.ww(NewaliencontrolData.tempx, Mem.uw(Plr1_TmpXOff_l));  // move.w Plr1_TmpXOff_l,tempx
            Mem.ww(NewaliencontrolData.tempz, Mem.uw(Plr1_TmpZOff_l));  // move.w Plr1_TmpZOff_l,tempz
            if (Mem.b(ai_ToSide_w) != 0) {             // tst.b ai_ToSide_w ; beq.s .no_side
                Newaliencontrol.RunAround(a0);         // jsr RunAround
            }

            // .no_side:
            int d1 = Mem.uw(a0);                       // move.w (a0),d1
            int a1 = Mem.l(Lvl_ObjectPointsPtr_l);     // move.l Lvl_ObjectPointsPtr_l,a1
            a1 = a1 + ((short) d1) * 8;                // lea (a1,d1.w*8),a1
            Mem.ww(ObjectmoveData.oldx, Mem.uw(a1));   // move.w (a1),oldx
            Mem.ww(ObjectmoveData.oldz, Mem.uw(a1 + 4)); // move.w 4(a1),oldz
            int d2 = Mem.uw(AI_ResponseSpeed_w);       // move.w AI_ResponseSpeed_w,d2
            d2 = muls(d2, Mem.uw(Anim_TempFrames_w));  // muls.w Anim_TempFrames_w,d2
            Mem.ww(ObjectmoveData.speed, d2);          // move.w d2,speed
            Mem.ww(ObjectmoveData.Range, 160);         // move.w #160,Range
            int d0 = Mem.w(a0 + 4);                    // move.w 4(a0),d0 ; ext.l d0
            d0 <<= 7;                                  // asl.l #7,d0
            d2 = Mem.l(ObjectmoveData.thingheight);    // move.l thingheight,d2
            d2 >>= 1;                                  // asr.l #1,d2
            d0 -= d2;                                  // sub.l d2,d0
            Mem.wl(ObjectmoveData.newy, d0);           // move.l d0,newy
            Mem.wl(ObjectmoveData.oldy, d0);           // move.l d0,oldy

            Mem.wb(ObjectmoveData.StoodInTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ...,StoodInTop
            // movem.l d0/a0/a1/a3/a4/d7,-(a7)
            Mem.wb(ObjectmoveData.canshove, 0);        // clr.b canshove
            Mem.wb(ObjectmoveData.GotThere, 0);        // clr.b GotThere
            Objectmove.HeadTowardsAng();               // jsr HeadTowardsAng
            Mem.ww(ObjectmoveData.wallflags, 0b1000000000); // move.w #%1000000000,wallflags

            Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b100000); // move.l #%100000,Obj_CollideFlags_l
            Objectmove.Obj_DoCollision(a2t);       // jsr Obj_DoCollision (a2 hérité de CheckTeleport)
            if (Mem.b(ObjectmoveData.hitwall) != 0) {  // tst.b hitwall ; beq.s .not_hit_player
                Mem.ww(ObjectmoveData.newx, Mem.uw(ObjectmoveData.oldx)); // move.w oldx,newx
                Mem.ww(ObjectmoveData.newz, Mem.uw(ObjectmoveData.oldz)); // move.w oldz,newz
                Mem.wb(ObjectmoveData.GotThere, 0xFF); // st GotThere
                // movem.l (a7)+,... ; bra .hit_something
            } else {
                // .not_hit_player:
                Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b11111111110111000010); // move.l #...,Obj_CollideFlags_l
                Objectmove.Obj_DoCollision(a2t);       // jsr Obj_DoCollision (a2 hérité de CheckTeleport)
                if (Mem.b(ObjectmoveData.hitwall) != 0) { // tst.b hitwall ; beq.s .can_move
                    Mem.ww(ObjectmoveData.newx, Mem.uw(ObjectmoveData.oldx)); // move.w oldx,newx
                    Mem.ww(ObjectmoveData.newz, Mem.uw(ObjectmoveData.oldz)); // move.w oldz,newz
                    // movem.l (a7)+,... ; bra .hit_something
                } else {
                    // .can_move:
                    Mem.wb(ObjectmoveData.Obj_WallBounce_b, 0); // clr.b Obj_WallBounce_b
                    Objectmove.MoveObject();           // jsr MoveObject
                    // movem.l (a7)+,...
                    Mem.wb(a0 + ShotT_InUpperZone_b, Mem.ub(ObjectmoveData.StoodInTop)); // move.b StoodInTop,...
                    Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(ObjectmoveData.AngRet)); // move.w AngRet,...
                }
            }

            // .hit_something:
            if (Mem.w(a0 + ObjT_ZoneID_w + ENT_PREV) >= 0) { // tst.w ... ; blt.s .no_copy_in
                Mem.ww(a0 + ObjT_ZoneID_w + ENT_PREV, Mem.uw(a0 + ObjT_ZoneID_w));
                Mem.ww(a0 + EntT_ZoneID_w + ENT_PREV, Mem.uw(a0 + EntT_ZoneID_w));
            }
            // .no_copy_in:
            gotThere = Mem.b(ObjectmoveData.GotThere) != 0;
        }

        if (!noMunch && gotThere                       // tst.b GotThere ; beq.s .no_munch
                && Mem.b(ai_DoAction_b) != 0) {        // tst.b ai_DoAction_b ; beq.s .no_munch
            int a5 = Mem.l(Plr1_ObjectPtr_l);          // move.l Plr1_ObjectPtr_l,a5
            int d0 = Mem.ub(ai_DoAction_b);            // move.b ai_DoAction_b,d0
            d0 = setw(d0, d0 << 1);                    // asl.w #1,d0
            Mem.wb(a5 + EntT_DamageTaken_b, Mem.ub(a5 + EntT_DamageTaken_b) + d0); // add.b d0,EntT_DamageTaken_b(a5)
            d0 = Mem.uw(ObjectmoveData.newx);          // move.w newx,d0
            d0 = setw(d0, d0 - Mem.uw(ObjectmoveData.oldx)); // sub.w oldx,d0
            d0 = (short) d0;                           // ext.l d0
            d0 = ab3d2.M68k.divs(d0, Mem.w(Anim_TempFrames_w)); // divs Anim_TempFrames_w,d0
            Mem.ww(a5 + EntT_ImpactX_w, Mem.uw(a5 + EntT_ImpactX_w) + d0); // add.w d0,EntT_ImpactX_w(a5)
            d0 = Mem.uw(ObjectmoveData.newz);          // move.w newz,d0
            d0 = setw(d0, d0 - Mem.uw(ObjectmoveData.oldz)); // sub.w oldz,d0
            d0 = (short) d0;                           // ext.l d0
            d0 = ab3d2.M68k.divs(d0, Mem.w(Anim_TempFrames_w)); // divs Anim_TempFrames_w,d0
            Mem.ww(a5 + EntT_ImpactZ_w, Mem.uw(a5 + EntT_ImpactZ_w) + d0); // add.w d0,EntT_ImpactZ_w(a5)
        }

        // .no_munch:
        ai_StorePlayerPosition(a0);                    // bsr ai_StorePlayerPosition
        ai_GetRoomStats(a0);                           // bsr ai_GetRoomStats
        ai_GetRoomCPT(a0);                             // bsr ai_GetRoomCPT
        ai_DoTorch(a0);                                // bsr ai_DoTorch
        AI_LookForPlayer1(a0);                         // bsr AI_LookForPlayer1
        Mem.wb(a0 + EntT_CurrentMode_b, 0);            // move.b #0,EntT_CurrentMode_b(a0)
        if (Mem.b(a0 + ObjT_SeePlayer_b) != 0) {       // tst.b ObjT_SeePlayer_b(a0) ; beq.s .cant_see_player
            int d0 = ai_CheckInFront(a0);              // bsr ai_CheckInFront
            if ((byte) d0 != 0) {                      // tst.b d0 ; beq.s .cant_see_player
                boolean attack;
                if (Mem.b(AI_FlyABit_w) != 0) {        // tst.b AI_FlyABit_w ; bne.s .attack_player
                    attack = true;
                } else {
                    d0 = ai_CheckAttackOnGround(a0);   // bsr ai_CheckAttackOnGround
                    attack = (byte) d0 != 0;           // tst.b d0 ; bne.s .attack_player
                }
                if (attack) {
                    // .attack_player:
                    d0 = Mem.uw(ai_AnimFacing_w);      // move.w ai_AnimFacing_w,d0
                    Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                    Mem.wb(a0 + EntT_CurrentMode_b, 1); // move.b #1,EntT_CurrentMode_b(a0)
                    Mem.wb(a0 + EntT_WhichAnim_b, 1);  // move.b #1,EntT_WhichAnim_b(a0)
                    return;                            // rts
                }
                // bra.s .cant_see_player
            }
        }
        // .cant_see_player:
        Mem.wb(a0 + EntT_WhichAnim_b, 0);              // move.b #0,EntT_WhichAnim_b(a0)
        Mem.ww(a0 + EntT_Timer2_w, 0);                 // move.w #0,EntT_Timer2_w(a0)
        int d0 = Mem.uw(ai_AnimFacing_w);              // move.w ai_AnimFacing_w,d0
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,EntT_CurrentAngle_w(a0)
        // rts
    }

    /** ai_AttackWithGunFlying */
    private static void ai_AttackWithGunFlying(int a0) {
        Mem.wb(AI_FlyABit_w, 0xFF);                    // st AI_FlyABit_w
        ai_AttackCommon(a0);                           // bra ai_AttackCommon
    }

    /** ai_AttackWithGun */
    private static void ai_AttackWithGun(int a0) {
        Mem.wb(AI_FlyABit_w, 0);                       // clr.b AI_FlyABit_w
        ai_AttackCommon(a0);
    }

    /** ai_AttackCommon — prépare SHOTTYPE/POWER/SPEED/SHIFT depuis le GLF. */
    private static void ai_AttackCommon(int a0) {
        int a1 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a1
        a1 = a1 + GLFT_AlienDefs_l;                    // lea GLFT_AlienDefs_l(a1),a1
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        d0 = muls(d0, AlienT_SizeOf_l);                // muls #AlienT_SizeOf_l,d0
        a1 += (short) d0;                              // add.w d0,a1
        d0 = setw(d0, Mem.uw(a1 + AlienT_BulType_w));  // move.w AlienT_BulType_w(a1),d0
        Mem.wb(NewaliencontrolData.SHOTTYPE, d0);      // move.b d0,SHOTTYPE
        a1 = Mem.l(HiresData.GLF_DatabasePtr_l);       // move.l GLF_DatabasePtr_l,a1
        a1 = a1 + GLFT_BulletDefs_l;                   // lea GLFT_BulletDefs_l(a1),a1
        d0 = muls(d0, BulT_SizeOf_l);                  // muls #BulT_SizeOf_l,d0
        a1 += d0;                                      // add.l d0,a1
        d0 = Mem.l(a1 + BulT_HitDamage_l);             // move.l BulT_HitDamage_l(a1),d0
        Mem.wb(NewaliencontrolData.SHOTPOWER, d0);     // move.b d0,SHOTPOWER
        int d1 = 0;                                    // clr.l d1
        d0 = Mem.l(a1 + BulT_Speed_l);                 // move.l BulT_Speed_l(a1),d0
        d1 |= 1 << (d0 & 31);                          // bset d0,d1
        Mem.ww(NewaliencontrolData.SHOTSPEED, d1);     // move.w d1,SHOTSPEED
        d0 -= 1;                                       // sub.w #1,d0 (valeur petite, mot)
        Mem.ww(NewaliencontrolData.SHOTSHIFT, d0);     // move.w d0,SHOTSHIFT
        if (Mem.l(a1 + BulT_IsHitScan_l) == 0) {       // tst.l BulT_IsHitScan_l(a1) ; beq ai_AttackWithProjectile
            ai_AttackWithProjectile(a0);
            return;
        }
        ai_AttackWithHitScan(a0);
    }

    /** ai_AttackWithHitScan */
    private static void ai_AttackWithHitScan(int a0) {
        if (Mem.b(a0 + EntT_DamageTaken_b) != 0) {     // tst.b EntT_DamageTaken_b(a0) ; beq.s .no_damage
            Mem.wb(a0 + EntT_CurrentMode_b, 4);        // move.b #4,EntT_CurrentMode_b(a0)
            ai_TakeDamage(a0);                         // bsr ai_TakeDamage
            if (Mem.b(ai_GetOut_w) != 0) {             // tst.b ai_GetOut_w ; beq.s .no_damage
                return;                                // rts
            }
        }

        // .no_damage:
        ai_DoAttackAnim(a0);                           // jsr ai_DoAttackAnim

        // SAVEREGS
        Mem.ww(ObjectmoveData.newx, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,newz
        int d1 = Mem.uw(a0);                           // move.w (a0),d1
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1
        Mem.ww(ObjectmoveData.oldx, Mem.uw(a1));       // move.w (a1),oldx
        Mem.ww(ObjectmoveData.oldz, Mem.uw(a1 + 4));   // move.w 4(a1),oldz
        Mem.ww(ObjectmoveData.Range, -20);             // move.w #-20,Range
        Mem.ww(ObjectmoveData.speed, 20);              // move.w #20,speed
        Objectmove.HeadTowardsAng();                   // jsr HeadTowardsAng
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(ObjectmoveData.AngRet)); // move.w AngRet,EntT_CurrentAngle_w(a0)
        // GETREGS

        ai_StorePlayerPosition(a0);                    // bsr ai_StorePlayerPosition
        AI_LookForPlayer1(a0);                         // bsr AI_LookForPlayer1

        Mem.wb(a0 + EntT_CurrentMode_b, 0);            // move.b #0,EntT_CurrentMode_b(a0)
        int d0;
        boolean canSee = false;
        if (Mem.b(a0 + ObjT_SeePlayer_b) != 0) {       // tst.b ObjT_SeePlayer_b(a0) ; beq.s .cant_see_player
            d0 = ai_CheckInFront(a0);                  // bsr ai_CheckInFront
            if ((byte) d0 != 0) {                      // tst.b d0 ; beq.s .cant_see_player
                Mem.wb(a0 + EntT_CurrentMode_b, 1);    // move.b #1,EntT_CurrentMode_b(a0)
                Mem.wb(a0 + EntT_WhichAnim_b, 1);      // move.b #1,EntT_WhichAnim_b(a0)
                d0 = Mem.uw(ai_AnimFacing_w);          // move.w ai_AnimFacing_w,d0
                Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                canSee = true;                         // bra .can_see_player
            }
        }
        if (!canSee) {
            // .cant_see_player:
            Mem.wb(a0 + EntT_WhichAnim_b, 0);          // move.b #0,EntT_WhichAnim_b(a0)
            Mem.ww(a0 + EntT_Timer2_w, 0);             // move.w #0,EntT_Timer2_w(a0)
            Mem.ww(a0 + EntT_Timer1_w, Mem.uw(AI_FollowupTimer_w)); // move.w AI_FollowupTimer_w,EntT_Timer1_w(a0)
            Mem.ww(a0 + EntT_Timer2_w, 0);             // move.w #0,EntT_Timer2_w(a0)
            d0 = Mem.uw(ai_AnimFacing_w);              // move.w ai_AnimFacing_w,d0
            Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
            return;                                    // rts
        }

        // .can_see_player:
        if (Mem.b(ai_DoAction_b) != 0) {               // tst.b ai_DoAction_b ; beq .no_shooty_thang
            d1 = Mem.uw(a0);                           // move.w (a0),d1
            int a6 = ObjRotated_vl;                    // move.l #ObjRotated_vl,a6
            a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
            a1 = a1 + ((short) d1) * 8;                // lea (a1,d1.w*8),a1
            a6 = a6 + ((short) d1) * 8;                // lea (a6,d1.w*8),a6

            // movem.l a0/a1,-(a7)
            d0 = Objectmove.GetRand();                 // jsr GetRand

            a6 = ObjRotated_vl;                        // move.l #ObjRotated_vl,a6
            d1 = Mem.uw(a0);                           // move.w (a0),d1
            a6 = a6 + ((short) d1) * 8;                // lea (a6,d1.w*8),a6

            d0 = d0 & 0x7fff;                          // and.w #$7fff,d0
            d1 = Mem.uw(a6);                           // move.w (a6),d1
            d1 = muls(d1, d1);                         // muls d1,d1
            int d2 = Mem.uw(a6 + 2);                   // move.w 2(a6),d2
            d2 = muls(d2, d2);                         // muls d2,d2
            d1 += d2;                                  // add.l d2,d1
            d1 >>= 6;                                  // asr.l #6,d1
            d0 = (short) d0;                           // ext.l d0
            d0 <<= 2;                                  // asl.l #2,d0
            if (d0 > d1) {                             // cmp.l d1,d0 ; bgt.s .hit_player
                // .hit_player:
                a1 = Mem.l(Plr1_ObjectPtr_l);          // move.l Plr1_ObjectPtr_l,a1
                d0 = Mem.ub(NewaliencontrolData.SHOTPOWER); // move.b SHOTPOWER,d0
                Mem.wb(a1 + EntT_DamageTaken_b, Mem.ub(a1 + EntT_DamageTaken_b) + d0); // add.b d0,EntT_DamageTaken_b(a1)

                a6 -= ObjRotated_vl;                   // sub.l #ObjRotated_vl,a6
                a6 += Mem.l(Lvl_ObjectPointsPtr_l);    // add.l Lvl_ObjectPointsPtr_l,a6
                d0 = Mem.uw(a6);                       // move.w (a6),d0
                d0 = setw(d0, d0 - Mem.uw(Plr1_TmpXOff_l)); // sub.w Plr1_TmpXOff_l,d0 ; dx
                d1 = Mem.uw(a6 + 4);                   // move.w 4(a6),d1
                d1 = setw(d1, d1 - Mem.uw(Plr1_TmpZOff_l)); // sub.w Plr1_TmpZOff_l,d1 ; dz

                d2 = setw(0, d0);                      // move.w d0,d2
                int d3 = setw(0, d1);                  // move.w d1,d3
                d2 = muls(d2, d2);                     // muls d2,d2
                d3 = muls(d3, d3);                     // muls d3,d3
                d2 += d3;                              // add.l d3,d2
                d2 = ai_CalcSqrt(d2);                  // jsr ai_CalcSqrt
                d2 += d2;                              // add.l d2,d2

                d3 = 0;                                // moveq #0,d3
                d3 = setb(d3, Mem.ub(NewaliencontrolData.SHOTPOWER)); // move.b SHOTPOWER,d3

                d0 = muls(d0, d3);                     // muls d3,d0
                d0 = ab3d2.M68k.divs(d0, d2);          // divs d2,d0
                d1 = muls(d1, d3);                     // muls d3,d1
                d1 = ab3d2.M68k.divs(d1, d2);          // divs d2,d1

                Mem.ww(a1 + EntT_ImpactX_w, Mem.uw(a1 + EntT_ImpactX_w) - d0); // sub.w d0,EntT_ImpactX_w(a1)
                Mem.ww(a1 + EntT_ImpactZ_w, Mem.uw(a1 + EntT_ImpactZ_w) - d1); // sub.w d1,EntT_ImpactZ_w(a1)
            } else {
                Newaliencontrol.SHOOTPLAYER1(a0, a1);  // jsr SHOOTPLAYER1 (a1 = point objet de l'alien)
                // bra.s .missed_player
            }
            // .missed_player: movem.l (a7)+,a0/a1
        }

        // .no_shooty_thang:
        d1 = Mem.uw(a0);                               // move.w (a0),d1
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);             // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1

        Mem.ww(ObjectmoveData.newx, Mem.uw(a1));       // move.w (a1),newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(a1 + 4));   // move.w 4(a1),newz

        ai_DoTorch(a0);                                // bsr ai_DoTorch

        if (Mem.b(ai_FinishedAnim_b) != 0) {           // tst.b ai_FinishedAnim_b ; beq.s .not_finished_attacking
            Mem.wb(a0 + EntT_WhichAnim_b, 0);          // move.b #0,EntT_WhichAnim_b(a0)
            Mem.wb(a0 + EntT_CurrentMode_b, 2);        // move.b #2,EntT_CurrentMode_b(a0)
            Mem.ww(a0 + EntT_Timer1_w, Mem.uw(AI_FollowupTimer_w)); // move.w AI_FollowupTimer_w,EntT_Timer1_w(a0)
            Mem.ww(a0 + EntT_Timer2_w, 0);             // move.w #0,EntT_Timer2_w(a0)
            d0 = Mem.uw(ai_AnimFacing_w);              // move.w ai_AnimFacing_w,d0
            Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
        }
        // .not_finished_attacking: rts
    }

    /** ai_AttackWithProjectile */
    private static void ai_AttackWithProjectile(int a0) {
        if (Mem.b(a0 + EntT_DamageTaken_b) != 0) {     // tst.b EntT_DamageTaken_b(a0) ; beq.s .no_damage
            Mem.wb(a0 + EntT_CurrentMode_b, 4);        // move.b #4,EntT_CurrentMode_b(a0)
            ai_TakeDamage(a0);                         // bsr ai_TakeDamage
            if (Mem.b(ai_GetOut_w) != 0) {             // tst.b ai_GetOut_w ; beq.s .no_damage
                return;                                // rts
            }
        }

        // .no_damage:
        ai_DoAttackAnim(a0);                           // jsr ai_DoAttackAnim

        // SAVEREGS
        Mem.ww(ObjectmoveData.newx, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,newz
        int d1 = Mem.uw(a0);                           // move.w (a0),d1
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1
        Mem.ww(ObjectmoveData.oldx, Mem.uw(a1));       // move.w (a1),oldx
        Mem.ww(ObjectmoveData.oldz, Mem.uw(a1 + 4));   // move.w 4(a1),oldz
        Mem.ww(ObjectmoveData.Range, -20);             // move.w #-20,Range
        Mem.ww(ObjectmoveData.speed, 20);              // move.w #20,speed
        Objectmove.HeadTowardsAng();                   // jsr HeadTowardsAng
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(ObjectmoveData.AngRet)); // move.w AngRet,EntT_CurrentAngle_w(a0)
        // GETREGS

        ai_StorePlayerPosition(a0);                    // bsr ai_StorePlayerPosition

        if (Mem.b(ai_DoAction_b) != 0) {               // tst.b ai_DoAction_b ; beq.s .no_shooty_thang
            // SAVEREGS
            // move.w (a0),d1 ; lea (a1,d1.w*8),a1 (recharge le point — registres locaux)
            Mem.wb(NewaliencontrolData.SHOTINTOP, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a0),SHOTINTOP
            Newaliencontrol.FireAtPlayer1(a0);         // jsr FireAtPlayer1
            // GETREGS
        }

        // .no_shooty_thang:
        d1 = Mem.uw(a0);                               // move.w (a0),d1
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);             // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1

        Mem.ww(ObjectmoveData.newx, Mem.uw(a1));       // move.w (a1),newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(a1 + 4));   // move.w 4(a1),newz

        ai_DoTorch(a0);                                // bsr ai_DoTorch

        int d0;
        if (Mem.b(ai_FinishedAnim_b) != 0) {           // tst.b ai_FinishedAnim_b ; beq.s .not_finished_attacking
            Mem.wb(a0 + EntT_WhichAnim_b, 0);          // move.b #0,EntT_WhichAnim_b(a0)
            Mem.wb(a0 + EntT_CurrentMode_b, 2);        // move.b #2,EntT_CurrentMode_b(a0)
            Mem.ww(a0 + EntT_Timer1_w, Mem.uw(AI_FollowupTimer_w)); // move.w AI_FollowupTimer_w,EntT_Timer1_w(a0)
            Mem.ww(a0 + EntT_Timer2_w, 0);             // move.w #0,EntT_Timer2_w(a0)
            d0 = Mem.uw(ai_AnimFacing_w);              // move.w ai_AnimFacing_w,d0
            Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
            return;                                    // rts
        }

        // .not_finished_attacking:
        AI_LookForPlayer1(a0);                         // bsr AI_LookForPlayer1
        Mem.wb(a0 + EntT_CurrentMode_b, 0);            // move.b #0,EntT_CurrentMode_b(a0)
        if (Mem.b(a0 + ObjT_SeePlayer_b) != 0) {       // tst.b ObjT_SeePlayer_b(a0) ; beq.s .cant_see_player
            d0 = ai_CheckInFront(a0);                  // bsr ai_CheckInFront
            if ((byte) d0 != 0) {                      // tst.b d0 ; beq.s .cant_see_player
                Mem.wb(a0 + EntT_WhichAnim_b, 1);      // move.b #1,EntT_WhichAnim_b(a0)
                Mem.wb(a0 + EntT_CurrentMode_b, 1);    // move.b #1,EntT_CurrentMode_b(a0)
                d0 = Mem.uw(ai_AnimFacing_w);          // move.w ai_AnimFacing_w,d0
                Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                return;                                // rts
            }
        }
        // .cant_see_player:
        Mem.wb(a0 + EntT_WhichAnim_b, 0);              // move.b #0,EntT_WhichAnim_b(a0)
        Mem.ww(a0 + EntT_Timer2_w, 0);                 // move.w #0,EntT_Timer2_w(a0)
        Mem.ww(a0 + EntT_Timer1_w, Mem.uw(AI_FollowupTimer_w)); // move.w AI_FollowupTimer_w,EntT_Timer1_w(a0)
        Mem.ww(a0 + EntT_Timer2_w, 0);                 // move.w #0,EntT_Timer2_w(a0)
        d0 = Mem.uw(ai_AnimFacing_w);                  // move.w ai_AnimFacing_w,d0
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,EntT_CurrentAngle_w(a0)
        // rts
    }

    /** ai_ChargeToSideFlying */
    private static void ai_ChargeToSideFlying(int a0) {
        Mem.wb(AI_FlyABit_w, 0xFF);                    // st AI_FlyABit_w
        Mem.wb(ai_ToSide_w, 0xFF);                     // st ai_ToSide_w
        Mem.wl(ObjectmoveData.StepDownVal, 1000 * 256); // move.l #1000*256,StepDownVal
        ai_ChargeFlyingCommon(a0);                     // bra ai_ChargeFlyingCommon
    }

    /** ai_ChargeFlying */
    private static void ai_ChargeFlying(int a0) {
        Mem.wb(ai_ToSide_w, 0);                        // clr.b ai_ToSide_w
        Mem.wb(AI_FlyABit_w, 0xFF);                    // st AI_FlyABit_w
        Mem.wl(ObjectmoveData.StepDownVal, 1000 * 256); // move.l #1000*256,StepDownVal
        ai_ChargeFlyingCommon(a0);
    }

    /** ai_ChargeFlyingCommon */
    private static void ai_ChargeFlyingCommon(int a0) {
        if (Mem.b(a0 + EntT_DamageTaken_b) != 0) {     // tst.b EntT_DamageTaken_b(a0) ; beq.s .no_damage
            ai_TakeDamage(a0);                         // bsr ai_TakeDamage
            if (Mem.b(ai_GetOut_w) != 0) {             // tst.b ai_GetOut_w ; beq.s .no_damage
                return;                                // rts
            }
        }

        // .no_damage:
        ai_DoAttackAnim(a0);                           // jsr ai_DoAttackAnim

        Mem.ww(ObjectmoveData.FromZone, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),FromZone
        int a2t = Objectmove.CheckTeleport();   // jsr CheckTeleport (a2 = pointeur de zone en sortie)

        boolean noMunch = false;
        boolean gotThere = false;
        if (Mem.b(ObjectmoveData.OKTEL) != 0) {        // tst.b OKTEL ; beq.s .no_teleport
            int d0 = Mem.l(ObjectmoveData.floortemp);  // move.l floortemp,d0
            d0 >>= 7;                                  // asr.l #7,d0
            Mem.ww(a0 + 4, Mem.uw(a0 + 4) + d0);       // add.w d0,4(a0)
            noMunch = true;                            // bra .no_munch
        } else {
            // .no_teleport:
            int d1 = Mem.uw(a0);                       // move.w (a0),d1
            int a1 = Mem.l(Lvl_ObjectPointsPtr_l);     // move.l Lvl_ObjectPointsPtr_l,a1
            a1 = a1 + ((short) d1) * 8;                // lea (a1,d1.w*8),a1
            Mem.ww(ObjectmoveData.oldx, Mem.uw(a1));   // move.w (a1),oldx
            Mem.ww(ObjectmoveData.oldz, Mem.uw(a1 + 4)); // move.w 4(a1),oldz
            Mem.ww(ObjectmoveData.newx, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,newx
            Mem.ww(ObjectmoveData.newz, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,newz
            Mem.ww(NewaliencontrolData.tempsin, Mem.uw(Plr1_SinVal_w)); // move.w Plr1_SinVal_w,tempsin
            Mem.ww(NewaliencontrolData.tempcos, Mem.uw(Plr1_CosVal_w)); // move.w Plr1_CosVal_w,tempcos
            Mem.ww(NewaliencontrolData.tempx, Mem.uw(Plr1_TmpXOff_l));  // move.w Plr1_TmpXOff_l,tempx
            Mem.ww(NewaliencontrolData.tempz, Mem.uw(Plr1_TmpZOff_l));  // move.w Plr1_TmpZOff_l,tempz
            if (Mem.b(ai_ToSide_w) != 0) {             // tst.b ai_ToSide_w ; beq.s .no_side
                Newaliencontrol.RunAround(a0);         // jsr RunAround
            }

            // .no_side:
            int d2 = Mem.uw(AI_ResponseSpeed_w);       // move.w AI_ResponseSpeed_w,d2
            d2 = muls(d2, Mem.uw(Anim_TempFrames_w));  // muls.w Anim_TempFrames_w,d2
            Mem.ww(ObjectmoveData.speed, d2);          // move.w d2,speed
            Mem.ww(ObjectmoveData.Range, 160);         // move.w #160,Range
            int d0 = Mem.w(a0 + 4);                    // move.w 4(a0),d0 ; ext.l d0
            d0 <<= 7;                                  // asl.l #7,d0
            d2 = Mem.l(ObjectmoveData.thingheight);    // move.l thingheight,d2
            d2 >>= 1;                                  // asr.l #1,d2
            d0 -= d2;                                  // sub.l d2,d0
            Mem.wl(ObjectmoveData.newy, d0);           // move.l d0,newy
            Mem.wl(ObjectmoveData.oldy, d0);           // move.l d0,oldy

            Mem.wb(ObjectmoveData.StoodInTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ...,StoodInTop
            // movem.l d0/a0/a1/a3/a4/d7,-(a7)
            Mem.wb(ObjectmoveData.canshove, 0);        // clr.b canshove
            Mem.wb(ObjectmoveData.GotThere, 0);        // clr.b GotThere
            Objectmove.HeadTowardsAng();               // jsr HeadTowardsAng
            Mem.ww(ObjectmoveData.wallflags, 0b1000000000); // move.w #%1000000000,wallflags

            Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b100000); // move.l #%100000,Obj_CollideFlags_l
            Objectmove.Obj_DoCollision(a2t);       // jsr Obj_DoCollision (a2 hérité de CheckTeleport)
            if (Mem.b(ObjectmoveData.hitwall) != 0) {  // tst.b hitwall ; beq.s .not_hit_player
                Mem.ww(ObjectmoveData.newx, Mem.uw(ObjectmoveData.oldx)); // move.w oldx,newx
                Mem.ww(ObjectmoveData.newz, Mem.uw(ObjectmoveData.oldz)); // move.w oldz,newz
                Mem.wb(ObjectmoveData.GotThere, 0xFF); // st GotThere
                // bra .hit_something
            } else {
                // .not_hit_player:
                Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b11111111110111000010); // move.l #...,Obj_CollideFlags_l
                Objectmove.Obj_DoCollision(a2t);       // jsr Obj_DoCollision (a2 hérité de CheckTeleport)
                if (Mem.b(ObjectmoveData.hitwall) != 0) { // tst.b hitwall ; beq.s .can_move
                    Mem.ww(ObjectmoveData.newx, Mem.uw(ObjectmoveData.oldx)); // move.w oldx,newx
                    Mem.ww(ObjectmoveData.newz, Mem.uw(ObjectmoveData.oldz)); // move.w oldz,newz
                    // bra .hit_something
                } else {
                    // .can_move:
                    Mem.wb(ObjectmoveData.Obj_WallBounce_b, 0); // clr.b Obj_WallBounce_b
                    Objectmove.MoveObject();           // jsr MoveObject
                    Mem.wb(a0 + ShotT_InUpperZone_b, Mem.ub(ObjectmoveData.StoodInTop)); // move.b StoodInTop,...
                    Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(ObjectmoveData.AngRet)); // move.w AngRet,...
                }
            }
            // .hit_something:
            gotThere = Mem.b(ObjectmoveData.GotThere) != 0;
        }

        if (!noMunch && gotThere                       // tst.b GotThere ; beq.s .no_munch
                && Mem.b(ai_DoAction_b) != 0) {        // tst.b ai_DoAction_b ; beq.s .no_munch
            int a5 = Mem.l(Plr1_ObjectPtr_l);          // move.l Plr1_ObjectPtr_l,a5
            int d0 = Mem.ub(ai_DoAction_b);            // move.b ai_DoAction_b,d0
            d0 = setw(d0, d0 << 1);                    // asl.w #1,d0
            Mem.wb(a5 + EntT_DamageTaken_b, Mem.ub(a5 + EntT_DamageTaken_b) + d0); // add.b d0,EntT_DamageTaken_b(a5)
        }

        // .no_munch:
        ai_StorePlayerPosition(a0);                    // bsr ai_StorePlayerPosition

        int saved4 = Mem.uw(a0 + 4);                   // move.w 4(a0),-(a7)
        ai_GetRoomStats(a0);                           // bsr ai_GetRoomStats
        Mem.ww(a0 + 4, saved4);                        // move.w (a7)+,4(a0)

        ai_GetRoomCPT(a0);                             // bsr ai_GetRoomCPT
        ai_FlyToPlayerHeight(a0);                      // bsr ai_FlyToPlayerHeight
        ai_DoTorch(a0);                                // bsr ai_DoTorch
        AI_LookForPlayer1(a0);                         // bsr AI_LookForPlayer1
        Mem.wb(a0 + EntT_CurrentMode_b, 0);            // move.b #0,EntT_CurrentMode_b(a0)
        int d0;
        if (Mem.b(a0 + ObjT_SeePlayer_b) != 0) {       // tst.b ObjT_SeePlayer_b(a0) ; beq.s .cant_see_player
            d0 = ai_CheckInFront(a0);                  // bsr ai_CheckInFront
            if ((byte) d0 != 0) {                      // tst.b d0 ; beq.s .cant_see_player
                Mem.wb(a0 + EntT_CurrentMode_b, 1);    // move.b #1,EntT_CurrentMode_b(a0)
                Mem.wb(a0 + EntT_WhichAnim_b, 1);      // move.b #1,EntT_WhichAnim_b(a0)
                d0 = Mem.uw(ai_AnimFacing_w);          // move.w ai_AnimFacing_w,d0
                Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                return;                                // rts
            }
        }
        // .cant_see_player:
        Mem.wb(a0 + EntT_WhichAnim_b, 0);              // move.b #0,EntT_WhichAnim_b(a0)
        Mem.ww(a0 + EntT_Timer2_w, 0);                 // move.w #0,EntT_Timer2_w(a0)
        d0 = Mem.uw(ai_AnimFacing_w);                  // move.w ai_AnimFacing_w,d0
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,EntT_CurrentAngle_w(a0)
        // rts
    }

    // ***********************************************
    // ** Followup Movements *************************
    // ***********************************************

    /** ai_PauseBriefly */
    private static void ai_PauseBriefly(int a0) {
        if (Mem.b(a0 + EntT_DamageTaken_b) != 0) {     // tst.b EntT_DamageTaken_b(a0) ; beq.s .no_damage
            ai_TakeDamage(a0);                         // bsr ai_TakeDamage
            if (Mem.b(ai_GetOut_w) != 0) {             // tst.b ai_GetOut_w ; beq.s .no_damage
                return;                                // rts
            }
        }

        // .no_damage:
        Mem.ww(a0 + EntT_Timer2_w, 0);                 // move.w #0,EntT_Timer2_w(a0)
        ai_DoWalkAnim(a0);                             // jsr ai_DoWalkAnim

        int d0 = Mem.uw(Anim_TempFrames_w);            // move.w Anim_TempFrames_w,d0
        Mem.ww(a0 + EntT_Timer1_w, Mem.w(a0 + EntT_Timer1_w) - d0); // sub.w d0,EntT_Timer1_w(a0)
        if (Mem.w(a0 + EntT_Timer1_w) > 0) {           // bgt.s .stillwaiting
            // .stillwaiting:
            d0 = Mem.uw(ai_AnimFacing_w);              // move.w ai_AnimFacing_w,d0
            Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
            return;                                    // rts
        }

        int d1 = Mem.uw(a0);                           // move.w (a0),d1
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1

        Mem.ww(ObjectmoveData.newx, Mem.uw(a1));       // move.w (a1),newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(a1 + 4));   // move.w 4(a1),newz
        ai_DoTorch(a0);                                // bsr ai_DoTorch

        AI_LookForPlayer1(a0);                         // bsr AI_LookForPlayer1

        Mem.wb(a0 + EntT_CurrentMode_b, 0);            // move.b #0,EntT_CurrentMode_b(a0)
        if (Mem.b(a0 + ObjT_SeePlayer_b) != 0) {       // tst.b ObjT_SeePlayer_b(a0) ; beq.s .cant_see_player
            d0 = ai_CheckInFront(a0);                  // bsr ai_CheckInFront
            if ((byte) d0 != 0) {                      // tst.b d0 ; beq.s .cant_see_player
                d0 = ai_CheckForDark(a0);              // bsr ai_CheckForDark
                if ((byte) d0 != 0) {                  // tst.b d0 ; beq.s .cant_see_player
                    Mem.wb(a0 + EntT_WhichAnim_b, 1);  // move.b #1,EntT_WhichAnim_b(a0)
                    Mem.wb(a0 + EntT_CurrentMode_b, 1); // move.b #1,EntT_CurrentMode_b(a0)
                    d0 = Mem.uw(ai_AnimFacing_w);      // move.w ai_AnimFacing_w,d0
                    Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                    return;                            // rts
                }
            }
        }
        // .cant_see_player:
        Mem.wb(a0 + EntT_WhichAnim_b, 0);              // move.b #0,EntT_WhichAnim_b(a0)
        d0 = Mem.uw(ai_AnimFacing_w);                  // move.w ai_AnimFacing_w,d0
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,EntT_CurrentAngle_w(a0)
        // rts
    }

    /** ai_Approach */
    private static void ai_Approach(int a0) {
        Mem.wb(AI_FlyABit_w, 0);                       // clr.b AI_FlyABit_w
        Mem.wl(ObjectmoveData.StepDownVal, 30 * 256);  // move.l #30*256,StepDownVal
        Mem.wb(ai_ToSide_w, 0);                        // clr.b ai_ToSide_w
        ai_ApproachCommon(a0);                         // bra ai_ApproachCommon
    }

    /** ai_ApproachFlying */
    private static void ai_ApproachFlying(int a0) {
        Mem.wb(AI_FlyABit_w, 0xFF);                    // st AI_FlyABit_w
        Mem.wl(ObjectmoveData.StepDownVal, 1000 * 256); // move.l #1000*256,StepDownVal
        Mem.wb(ai_ToSide_w, 0);                        // clr.b ai_ToSide_w
        ai_ApproachCommon(a0);                         // bra ai_ApproachCommon
    }

    /** ai_ApproachToSideFlying */
    private static void ai_ApproachToSideFlying(int a0) {
        Mem.wb(AI_FlyABit_w, 0xFF);                    // st AI_FlyABit_w
        Mem.wl(ObjectmoveData.StepDownVal, 1000 * 256); // move.l #1000*256,StepDownVal
        Mem.wb(ai_ToSide_w, 0xFF);                     // st ai_ToSide_w
        ai_ApproachCommon(a0);                         // bra ai_ApproachCommon
    }

    /** ai_ApproachToSide */
    private static void ai_ApproachToSide(int a0) {
        Mem.wb(ai_ToSide_w, 0xFF);                     // st ai_ToSide_w
        Mem.wb(AI_FlyABit_w, 0);                       // clr.b AI_FlyABit_w
        Mem.wl(ObjectmoveData.StepDownVal, 30 * 256);  // move.l #30*256,StepDownVal
        ai_ApproachCommon(a0);
    }

    /** ai_ApproachCommon */
    private static void ai_ApproachCommon(int a0) {
        if (Mem.b(a0 + EntT_DamageTaken_b) != 0) {     // tst.b EntT_DamageTaken_b(a0) ; beq.s .no_damage
            ai_TakeDamage(a0);                         // bsr ai_TakeDamage
            if (Mem.b(ai_GetOut_w) != 0) {             // tst.b ai_GetOut_w ; beq.s .no_damage
                return;                                // rts
            }
        }

        // .no_damage:
        ai_DoWalkAnim(a0);                             // jsr ai_DoWalkAnim

        Mem.ww(ObjectmoveData.FromZone, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),FromZone
        int a2t = Objectmove.CheckTeleport();   // jsr CheckTeleport (a2 = pointeur de zone en sortie)

        if (Mem.b(ObjectmoveData.OKTEL) != 0) {        // tst.b OKTEL ; beq.s .no_teleport
            int d0 = Mem.l(ObjectmoveData.floortemp);  // move.l floortemp,d0
            d0 >>= 7;                                  // asr.l #7,d0
            Mem.ww(a0 + 4, Mem.uw(a0 + 4) + d0);       // add.w d0,4(a0)
            // bra .no_munch
        } else {
            // .no_teleport:
            int d1 = Mem.uw(a0);                       // move.w (a0),d1
            int a1 = Mem.l(Lvl_ObjectPointsPtr_l);     // move.l Lvl_ObjectPointsPtr_l,a1
            a1 = a1 + ((short) d1) * 8;                // lea (a1,d1.w*8),a1
            Mem.ww(ObjectmoveData.oldx, Mem.uw(a1));   // move.w (a1),oldx
            Mem.ww(ObjectmoveData.oldz, Mem.uw(a1 + 4)); // move.w 4(a1),oldz

            Mem.ww(ObjectmoveData.newx, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,newx
            Mem.ww(ObjectmoveData.newz, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,newz
            Mem.ww(NewaliencontrolData.tempsin, Mem.uw(Plr1_SinVal_w)); // move.w Plr1_SinVal_w,tempsin
            Mem.ww(NewaliencontrolData.tempcos, Mem.uw(Plr1_CosVal_w)); // move.w Plr1_CosVal_w,tempcos
            Mem.ww(NewaliencontrolData.tempx, Mem.uw(Plr1_TmpXOff_l));  // move.w Plr1_TmpXOff_l,tempx
            Mem.ww(NewaliencontrolData.tempz, Mem.uw(Plr1_TmpZOff_l));  // move.w Plr1_TmpZOff_l,tempz
            if (Mem.b(ai_ToSide_w) != 0) {             // tst.b ai_ToSide_w ; beq.s .no_side
                Newaliencontrol.RunAround(a0);         // jsr RunAround
            }

            // .no_side:
            Mem.ww(ObjectmoveData.speed, 0);           // move.w #0,speed
            if (Mem.b(ai_DoAction_b) != 0) {           // tst.b ai_DoAction_b ; beq.s .no_speed
                int d2 = 0;                            // moveq #0,d2
                d2 = setb(d2, Mem.ub(ai_DoAction_b));  // move.b ai_DoAction_b,d2
                d2 = setw(d2, d2 << 2);                // asl.w #2,d2
                d2 = muls(d2, Mem.w(AI_FollowupSpeed_w)); // muls.w AI_FollowupSpeed_w,d2
                Mem.ww(ObjectmoveData.speed, d2);      // move.w d2,speed
            }

            // .no_speed:
            Mem.ww(ObjectmoveData.Range, 160);         // move.w #160,Range
            int d0 = Mem.w(a0 + 4);                    // move.w 4(a0),d0 ; ext.l d0
            d0 <<= 7;                                  // asl.l #7,d0
            int d2 = Mem.l(ObjectmoveData.thingheight); // move.l thingheight,d2
            d2 >>= 1;                                  // asr.l #1,d2
            d0 -= d2;                                  // sub.l d2,d0
            Mem.wl(ObjectmoveData.newy, d0);           // move.l d0,newy
            Mem.wl(ObjectmoveData.oldy, d0);           // move.l d0,oldy

            Mem.wb(ObjectmoveData.StoodInTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ...,StoodInTop
            // movem.l d0/a0/a1/a3/a4/d7,-(a7)
            Mem.wb(ObjectmoveData.canshove, 0);        // clr.b canshove
            Mem.wb(ObjectmoveData.GotThere, 0);        // clr.b GotThere
            Objectmove.HeadTowardsAng();               // jsr HeadTowardsAng
            Mem.ww(ObjectmoveData.wallflags, 0b1000000000); // move.w #%1000000000,wallflags

            Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b100000); // move.l #%100000,Obj_CollideFlags_l
            Objectmove.Obj_DoCollision(a2t);       // jsr Obj_DoCollision (a2 hérité de CheckTeleport)
            if (Mem.b(ObjectmoveData.hitwall) != 0) {  // tst.b hitwall ; beq.s .not_hit_player
                Mem.ww(ObjectmoveData.newx, Mem.uw(ObjectmoveData.oldx)); // move.w oldx,newx
                Mem.ww(ObjectmoveData.newz, Mem.uw(ObjectmoveData.oldz)); // move.w oldz,newz
                Mem.wb(ObjectmoveData.GotThere, 0xFF); // st GotThere
                // bra .hit_something
            } else {
                // .not_hit_player:
                Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b11111111110111000010); // move.l #...,Obj_CollideFlags_l
                Objectmove.Obj_DoCollision(a2t);       // jsr Obj_DoCollision (a2 hérité de CheckTeleport)
                if (Mem.b(ObjectmoveData.hitwall) != 0) { // tst.b hitwall ; beq.s .can_move
                    Mem.ww(ObjectmoveData.newx, Mem.uw(ObjectmoveData.oldx)); // move.w oldx,newx
                    Mem.ww(ObjectmoveData.newz, Mem.uw(ObjectmoveData.oldz)); // move.w oldz,newz
                    // bra .hit_something
                } else {
                    // .can_move:
                    Mem.wb(ObjectmoveData.Obj_WallBounce_b, 0); // clr.b Obj_WallBounce_b
                    Objectmove.MoveObject();           // jsr MoveObject
                    Mem.wb(a0 + ShotT_InUpperZone_b, Mem.ub(ObjectmoveData.StoodInTop)); // move.b StoodInTop,...
                    Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(ObjectmoveData.AngRet)); // move.w AngRet,...
                }
            }

            // .hit_something:
            if (Mem.w(a0 + ObjT_ZoneID_w + ENT_PREV) >= 0) { // tst.w ... ; blt.s .no_copy_in
                Mem.ww(a0 + ObjT_ZoneID_w + ENT_PREV, Mem.uw(a0 + ObjT_ZoneID_w));
                Mem.ww(a0 + EntT_ZoneID_w + ENT_PREV, Mem.uw(a0 + EntT_ZoneID_w));
            }
            // .no_copy_in:
        }

        // .no_munch:
        ai_StorePlayerPosition(a0);                    // bsr ai_StorePlayerPosition

        if (Mem.b(AI_FlyABit_w) != 0) {                // tst.b AI_FlyABit_w ; beq.s .notfl
            ai_FlyToPlayerHeight(a0);                  // bsr ai_FlyToPlayerHeight
        }

        // .notfl:
        int saved4 = Mem.uw(a0 + 4);                   // move.w 4(a0),-(a7)
        ai_GetRoomStats(a0);                           // bsr ai_GetRoomStats
        int d0 = saved4;                               // move.w (a7)+,d0
        if (Mem.b(AI_FlyABit_w) != 0) {                // tst.b AI_FlyABit_w ; beq.s .not_flying
            Mem.ww(a0 + 4, d0);                        // move.w d0,4(a0)
        }

        // .not_flying:
        ai_GetRoomCPT(a0);                             // bsr ai_GetRoomCPT
        ai_DoTorch(a0);                                // bsr ai_DoTorch

        Mem.wb(a0 + EntT_CurrentMode_b, 0);            // move.b #0,EntT_CurrentMode_b(a0)
        if (Mem.b(AI_FlyABit_w) == 0) {                // tst.b AI_FlyABit_w ; bne.s .is_flying
            d0 = ai_CheckAttackOnGround(a0);           // bsr ai_CheckAttackOnGround
            if ((byte) d0 == 0) {                      // tst.b d0 ; beq .cant_see_player
                // .cant_see_player:
                Mem.wb(a0 + EntT_WhichAnim_b, 0);      // move.b #0,EntT_WhichAnim_b(a0)
                d0 = Mem.uw(ai_AnimFacing_w);          // move.w ai_AnimFacing_w,d0
                Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                return;                                // rts
            }
        }

        // .is_flying:
        AI_LookForPlayer1(a0);                         // bsr AI_LookForPlayer1
        if (Mem.b(a0 + ObjT_SeePlayer_b) != 0) {       // tst.b ObjT_SeePlayer_b(a0) ; beq.s .cant_see_player
            d0 = ai_CheckInFront(a0);                  // bsr ai_CheckInFront
            if ((byte) d0 != 0) {                      // tst.b d0 ; beq.s .cant_see_player
                Mem.wb(a0 + EntT_CurrentMode_b, 2);    // move.b #2,EntT_CurrentMode_b(a0)
                d0 = Mem.uw(Anim_TempFrames_w);        // move.w Anim_TempFrames_w,d0
                Mem.ww(a0 + EntT_Timer1_w, Mem.w(a0 + EntT_Timer1_w) - d0); // sub.w d0,EntT_Timer1_w(a0)
                if (Mem.w(a0 + EntT_Timer1_w) <= 0) {  // bgt.s .cant_see_player
                    d0 = ai_CheckForDark(a0);          // bsr ai_CheckForDark
                    if ((byte) d0 != 0) {              // tst.b d0 ; beq.s .cant_see_player
                        Mem.wb(a0 + EntT_CurrentMode_b, 1); // move.b #1,EntT_CurrentMode_b(a0)
                        Mem.ww(a0 + EntT_Timer2_w, 0);      // move.w #0,EntT_Timer2_w(a0)
                        Mem.wb(a0 + EntT_WhichAnim_b, 1);   // move.b #1,EntT_WhichAnim_b(a0)
                        d0 = Mem.uw(ai_AnimFacing_w);       // move.w ai_AnimFacing_w,d0
                        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,...
                        return;                        // rts
                    }
                }
            }
        }
        // .cant_see_player (fin):
        Mem.wb(a0 + EntT_WhichAnim_b, 0);              // move.b #0,EntT_WhichAnim_b(a0)
        d0 = Mem.uw(ai_AnimFacing_w);                  // move.w ai_AnimFacing_w,d0
        Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d0); // add.w d0,EntT_CurrentAngle_w(a0)
        // rts
    }

    // ***********************************************
    // ** GENERIC ROUTINES ***************************
    // ***********************************************

    /** ai_FlyToCPTHeight */
    private static void ai_FlyToCPTHeight(int a0) {
        int d0 = Mem.uw(ai_MiddleCPT_w);               // move.w ai_MiddleCPT_w,d0
        int a1 = Mem.l(Lvl_ControlPointCoordsPtr_l);   // move.l Lvl_ControlPointCoordsPtr_l,a1
        int d1 = Mem.uw(a1 + ((short) d0) * 8 + 4);    // move.w 4(a1,d0.w*8),d1
        ai_FlyToHeightCommon(a0, d1);                  // bra ai_FlyToHeightCommon
    }

    /** ai_FlyToPlayerHeight */
    private static void ai_FlyToPlayerHeight(int a0) {
        int d1 = Mem.l(Plr1_YOff_l);                   // move.l Plr1_YOff_l,d1
        d1 >>= 7;                                      // asr.l #7,d1
        ai_FlyToHeightCommon(a0, d1);
    }

    /** ai_FlyToHeightCommon */
    private static void ai_FlyToHeightCommon(int a0, int d1) {
        int d0 = Mem.uw(a0 + 4);                       // move.w 4(a0),d0
        int d2;
        if ((short) d1 > (short) d0) {                 // cmp.w d0,d1 ; bgt.s .fly_down
            // .fly_down:
            d2 = Mem.uw(a0 + EntT_VelocityY_w);        // move.w EntT_VelocityY_w(a0),d2
            d2 = setw(d2, d2 + 2);                     // add.w #2,d2
            if ((short) d2 >= 32) {                    // cmp.w #32,d2 ; blt.s .no_fast_down
                d2 = setw(d2, 32);                     // move.w #32,d2
            }
            // .no_fast_down:
            Mem.ww(a0 + EntT_VelocityY_w, d2);         // move.w d2,EntT_VelocityY_w(a0)
            Mem.ww(a0 + 4, Mem.uw(a0 + 4) + d2);       // add.w d2,4(a0)
        } else {
            d2 = Mem.uw(a0 + EntT_VelocityY_w);        // move.w EntT_VelocityY_w(a0),d2
            d2 = setw(d2, d2 - 2);                     // sub.w #2,d2
            if ((short) d2 <= -32) {                   // cmp.w #-32,d2 ; bgt.s .no_fast_up
                d2 = setw(d2, -32);                    // move.w #-32,d2
            }
            // .no_fast_up:
            Mem.ww(a0 + EntT_VelocityY_w, d2);         // move.w d2,EntT_VelocityY_w(a0)
            Mem.ww(a0 + 4, Mem.uw(a0 + 4) + d2);       // add.w d2,4(a0)
        }
        ai_CheckFloorCeiling(a0);                      // bra ai_CheckFloorCeiling
    }

    /** ai_CheckFloorCeiling */
    private static void ai_CheckFloorCeiling(int a0) {
        int d2 = Mem.uw(a0 + 4);                       // move.w 4(a0),d2
        int d4 = Mem.l(ObjectmoveData.thingheight);    // move.l thingheight,d4
        d4 >>= 8;                                      // asr.l #8,d4
        int d3 = setw(0, d2);                          // move.w d2,d3
        d2 = setw(d2, d2 - d4);                        // sub.w d4,d2
        d3 = setw(d3, d3 + d4);                        // add.w d4,d3

        int a2 = Mem.l(ObjectmoveData.Obj_ZonePtr_l);  // move.l Obj_ZonePtr_l,a2

        int d0 = Mem.l(a2 + ZoneT_Floor_l);            // move.l ZoneT_Floor_l(a2),d0
        int d1 = Mem.l(a2 + ZoneT_Roof_l);             // move.l ZoneT_Roof_l(a2),d1
        if (Mem.b(a0 + ShotT_InUpperZone_b) != 0) {    // tst.b ShotT_InUpperZone_b(a0) ; beq.s .not_in_top
            d0 = Mem.l(a2 + ZoneT_UpperFloor_l);       // move.l ZoneT_UpperFloor_l(a2),d0
            d1 = Mem.l(a2 + ZoneT_UpperRoof_l);        // move.l ZoneT_UpperRoof_l(a2),d1
        }

        // .not_in_top:
        d0 >>= 7;                                      // asr.l #7,d0
        d1 >>= 7;                                      // asr.l #7,d1
        if ((short) d3 >= (short) d0) {                // cmp.w d0,d3 ; blt.s .bottom_no_hit
            d3 = setw(d3, d0);                         // move.w d0,d3
            d2 = setw(d2, d3);                         // move.w d3,d2
            d2 = setw(d2, d2 - d4);                    // sub.w d4,d2
            d2 = setw(d2, d2 - d4);                    // sub.w d4,d2
        }
        // .bottom_no_hit:
        if ((short) d2 <= (short) d1) {                // cmp.w d1,d2 ; bgt.s .top_no_hit
            d2 = setw(d2, d1);                         // move.w d1,d2
            d3 = setw(d3, d2);                         // move.w d2,d3
            d3 = setw(d3, d3 + d4);                    // add.w d4,d3
            d3 = setw(d3, d3 + d4);                    // add.w d4,d3
        }
        // .top_no_hit:
        d3 = setw(d3, d3 - d4);                        // sub.w d4,d3
        Mem.ww(a0 + 4, d3);                            // move.w d3,4(a0)
        // rts
    }

    /** ai_StorePlayerPosition */
    private static void ai_StorePlayerPosition(int a0) {
        int d0 = Mem.uw(a0);                           // move.w (a0),d0
        int a2 = ai_AlienWorkspace_vl;                 // move.l #ai_AlienWorkspace_vl,a2
        d0 = setw(d0, d0 << 4);                        // asl.w #4,d0
        a2 += (short) d0;                              // add.w d0,a2
        Mem.ww(a2 + AI_WorkT_LastX_w, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,AI_WorkT_LastX_w(a2)
        Mem.ww(a2 + AI_WorkT_LastY_w, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,AI_WorkT_LastY_w(a2)
        int a3 = Mem.l(Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a3
        Mem.ww(a2 + AI_WorkT_LastZone_w, Mem.uw(a3));  // move.w (a3),AI_WorkT_LastZone_w(a2)
        d0 = 0;                                        // moveq #0,d0
        d0 = setb(d0, Mem.ub(a3 + ZoneT_ControlPoint_w)); // move.b ZoneT_ControlPoint_w(a3),d0
        if (Mem.b(Plr1_StoodInTop_b) != 0) {           // tst.b Plr1_StoodInTop_b ; beq.s .player_not_in_top
            d0 = setb(d0, Mem.ub(a3 + ZoneT_ControlPoint_w + 1)); // move.b ZoneT_ControlPoint_w+1(a3),d0
        }
        // .player_not_in_top:
        Mem.ww(a2 + AI_WorkT_LastControlPoint_w, d0);  // move.w d0,AI_WorkT_LastControlPoint_w(a2)
        d0 = setb(d0, Mem.ub(a0 + EntT_TeamNumber_b)); // move.b EntT_TeamNumber_b(a0),d0
        if ((byte) d0 >= 0) {                          // blt.s .no_team
            a2 = AI_AlienTeamWorkspace_vl;             // move.l #AI_AlienTeamWorkspace_vl,a2
            d0 = setw(d0, d0 << 4);                    // asl.w #4,d0
            a2 += (short) d0;                          // add.w d0,a2
            Mem.ww(a2 + AI_WorkT_LastX_w, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,...
            Mem.ww(a2 + AI_WorkT_LastY_w, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,...
            a3 = Mem.l(Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a3
            Mem.ww(a2 + AI_WorkT_LastZone_w, Mem.uw(a3)); // move.w (a3),...
            d0 = 0;                                    // moveq #0,d0
            d0 = setb(d0, Mem.ub(a3 + ZoneT_ControlPoint_w)); // move.b ZoneT_ControlPoint_w(a3),d0
            if (Mem.b(Plr1_StoodInTop_b) != 0) {       // tst.b Plr1_StoodInTop_b ; beq.s .player_not_in_top2
                d0 = setb(d0, Mem.ub(a3 + ZoneT_ControlPoint_w + 1)); // move.b ...+1(a3),d0
            }
            // .player_not_in_top2:
            Mem.ww(a2 + AI_WorkT_LastControlPoint_w, d0); // move.w d0,AI_WorkT_LastControlPoint_w(a2)
            Mem.ww(a2 + AI_WorkT_SeenBy_w, Mem.uw(a0)); // move.w (a0),AI_WorkT_SeenBy_w(a2)
        }
        // .no_team: rts
    }

    /** ai_GetRoomStats — écrit newx/newz dans le point objet puis chute dans ai_GetRoomStatsStill. */
    private static void ai_GetRoomStats(int a0) {
        int d0 = Mem.uw(a0);                           // move.w (a0),d0
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d0) * 8;                    // lea (a1,d0.w*8),a1
        Mem.ww(a1, Mem.uw(ObjectmoveData.newx));       // move.w newx,(a1)
        Mem.ww(a1 + 4, Mem.uw(ObjectmoveData.newz));   // move.w newz,4(a1)
        ai_GetRoomStatsStill(a0);
    }

    /** ai_GetRoomStatsStill */
    private static void ai_GetRoomStatsStill(int a0) {
        int a2 = Mem.l(ObjectmoveData.Obj_ZonePtr_l);  // move.l Obj_ZonePtr_l,a2
        Mem.ww(a0 + 12, Mem.uw(a2));                   // move.w (a2),12(a0)

        int d0 = Mem.l(a2 + ZoneT_Floor_l);            // move.l ZoneT_Floor_l(a2),d0
        if (Mem.b(a0 + ShotT_InUpperZone_b) != 0) {    // tst.b ShotT_InUpperZone_b(a0) ; beq.s .not_in_top2
            d0 = Mem.l(a2 + ZoneT_UpperFloor_l);       // move.l ZoneT_UpperFloor_l(a2),d0
        }

        // .not_in_top2:
        int d2 = Mem.l(ObjectmoveData.thingheight);    // move.l thingheight,d2
        d2 >>= 1;                                      // asr.l #1,d2
        d0 -= d2;                                      // sub.l d2,d0
        d0 >>= 7;                                      // asr.l #7,d0
        Mem.ww(a0 + 4, d0);                            // move.w d0,4(a0)
        Mem.ww(a0 + EntT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),EntT_ZoneID_w(a0)
        // rts
    }

    /** ai_CheckForDark — renvoie d0 : 0 = dans le noir (invisible), -1 sinon. */
    private static int ai_CheckForDark(int a0) {
        int d0 = Mem.uw(a0);                           // move.w (a0),d0
        int a3 = Mem.l(Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a3
        int d1 = Mem.uw(a3);                           // move.w (a3),d1
        if ((short) d1 != (short) d0) {                // cmp.w d1,d0 ; beq.s .not_in_dark
            d0 = Objectmove.GetRand();                 // jsr GetRand
            d0 = d0 & 31;                              // and.w #31,d0
            if ((short) d0 < Mem.w(Plr1_RoomBright_w)) { // cmp.w Plr1_RoomBright_w,d0 ; bge.s .not_in_dark
                // .in_dark:
                return 0;                              // moveq #0,d0 ; rts
            }
        }
        // .not_in_dark:
        return -1;                                     // moveq #-1,d0 ; rts
    }

    /** ai_CheckInFront — renvoie d0 (octet faible $FF si le joueur est devant). */
    private static int ai_CheckInFront(int a0) {
        int d0 = Mem.uw(a0);                           // move.w (a0),d0
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        Mem.ww(ObjectmoveData.newx, Mem.uw(a1 + ((short) d0) * 8));     // move.w (a1,d0.w*8),newx
        Mem.ww(ObjectmoveData.newz, Mem.uw(a1 + ((short) d0) * 8 + 4)); // move.w 4(a1,d0.w*8),newz

        d0 = Mem.uw(Plr1_TmpXOff_l);                   // move.w Plr1_TmpXOff_l,d0
        d0 = setw(d0, d0 - Mem.uw(ObjectmoveData.newx)); // sub.w newx,d0
        int d1 = Mem.uw(Plr1_TmpZOff_l);               // move.w Plr1_TmpZOff_l,d1
        d1 = setw(d1, d1 - Mem.uw(ObjectmoveData.newz)); // sub.w newz,d1

        int d2 = Mem.uw(a0 + EntT_CurrentAngle_w);     // move.w EntT_CurrentAngle_w(a0),d2
        d2 = TablesData.AMOD_A(d2);                    // AMOD_A d2
        int a3 = SinCosTable_vw;                       // move.l #SinCosTable_vw,a3
        int d3 = Mem.w(a3 + (short) d2);               // move.w (a3,d2.w),d3
        d2 += COSINE_OFS;                              // add.l #COSINE_OFS,d2
        int d4 = Mem.w(a3 + (short) d2);               // move.w (a3,d2.w),d4

        d0 = muls(d0, d3);                             // muls d3,d0
        d1 = muls(d1, d4);                             // muls d4,d1
        d1 += d0;                                      // add.l d0,d1
        return setb(d0, (d1 > 0) ? 0xFF : 0);          // sgt d0 ; rts
    }

    /** AI_LookForPlayer1 */
    public static void AI_LookForPlayer1(int a0) {
        Mem.wb(a0 + ObjT_SeePlayer_b, 0);              // clr.b ObjT_SeePlayer_b(a0)
        Mem.wb(ObjectmoveData.CanSee, 0);              // clr.b CanSee
        Mem.wb(ObjectmoveData.ViewerTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a0),ViewerTop
        Mem.wb(ObjectmoveData.TargetTop, Mem.ub(Plr1_StoodInTop_b)); // move.b Plr1_StoodInTop_b,TargetTop
        Mem.wl(ObjectmoveData.Obj_ToZonePtr_l, Mem.l(Plr1_ZonePtr_l)); // move.l Plr1_ZonePtr_l,Obj_ToZonePtr_l
        Mem.wl(ObjectmoveData.Obj_FromZonePtr_l, Mem.l(ObjectmoveData.Obj_ZonePtr_l)); // move.l Obj_ZonePtr_l,Obj_FromZonePtr_l
        Mem.ww(ObjectmoveData.Viewerx, Mem.uw(ObjectmoveData.newx)); // move.w newx,Viewerx
        Mem.ww(ObjectmoveData.Viewerz, Mem.uw(ObjectmoveData.newz)); // move.w newz,Viewerz
        Mem.ww(ObjectmoveData.Targetx, Mem.uw(Plr1_XOff_l)); // move.w Plr1_XOff_l,Targetx
        Mem.ww(ObjectmoveData.Targetz, Mem.uw(Plr1_ZOff_l)); // move.w Plr1_ZOff_l,Targetz
        int d0 = Mem.l(Plr1_YOff_l);                   // move.l Plr1_YOff_l,d0
        d0 >>= 7;                                      // asr.l #7,d0
        Mem.ww(ObjectmoveData.Targety, d0);            // move.w d0,Targety
        Mem.ww(ObjectmoveData.Viewery, Mem.uw(a0 + 4)); // move.w 4(a0),Viewery
        Objectmove.CanItBeSeen();                      // jsr CanItBeSeen

        if (Mem.b(ObjectmoveData.CanSee) != 0) {       // tst.b CanSee ; beq .carryonprowling
            Mem.wb(a0 + ObjT_SeePlayer_b, 1);          // move.b #1,ObjT_SeePlayer_b(a0)
        }
        // .carryonprowling: rts
    }

    /** AI_InitAlienWorkspace */
    public static void AI_InitAlienWorkspace() {
        int a0 = ai_AlienWorkspace_vl;                 // move.l #ai_AlienWorkspace_vl,a0
        int d0 = 299;                                  // move.l #299,d0
        do { // .loop:
            Mem.wl(a0, 0);                             // move.l #0,(a0)
            Mem.wl(a0 + 4, -1);                        // move.l #-1,4(a0)
            Mem.wl(a0 + 8, -1);                        // move.l #-1,8(a0)
            a0 += 16;                                  // add.w #16,a0
            d0 = setw(d0, d0 - 1);                     // dbra d0,.loop
        } while ((short) d0 != -1);

        a0 = AI_AlienTeamWorkspace_vl;                 // move.l #AI_AlienTeamWorkspace_vl,a0
        d0 = 29;                                       // move.l #29,d0
        do { // .loop2:
            Mem.wl(a0, 0);                             // move.l #0,(a0)
            Mem.wl(a0 + 4, -1);                        // move.l #-1,4(a0)
            Mem.wl(a0 + 8, -1);                        // move.l #-1,8(a0)
            a0 += 16;                                  // add.w #16,a0
            d0 = setw(d0, d0 - 1);                     // dbra d0,.loop2
        } while ((short) d0 != -1);
        // rts
    }

    /** ai_CheckDamage — a0 = entité, a1 = entrée du point objet (posé par l'appelant). */
    public static void ai_CheckDamage(int a0, int a1) {
        int d2 = 0;                                    // moveq #0,d2
        d2 = setb(d2, Mem.ub(a0 + EntT_DamageTaken_b)); // move.b EntT_DamageTaken_b(a0),d2
        if ((byte) d2 == 0) {                          // beq .noscream
            return;                                    // .noscream: rts
        }

        Mem.wb(a0 + EntT_HitPoints_b, Mem.ub(a0 + EntT_HitPoints_b) - d2); // sub.b d2,EntT_HitPoints_b(a0)
        if ((byte) Mem.b(a0 + EntT_HitPoints_b) <= 0) { // bgt .not_dead_yet
            int d0 = 0;                                // moveq #0,d0
            d0 = setb(d0, Mem.ub(a0 + EntT_TeamNumber_b)); // move.b EntT_TeamNumber_b(a0),d0
            if ((byte) d0 >= 0) {                      // blt.s .no_team
                int a2 = AI_AlienTeamWorkspace_vl;     // move.l #AI_AlienTeamWorkspace_vl,a2
                d0 = setw(d0, d0 << 4);                // asl.w #4,d0
                a2 += (short) d0;                      // add.w d0,a2
                d0 = setw(d0, Mem.uw(a0));             // move.w (a0),d0
                if ((short) d0 == Mem.w(a2 + AI_WorkT_SeenBy_w)) { // cmp.w AI_WorkT_SeenBy_w(a2),d0 ; bne.s .no_team
                    Mem.ww(a2 + AI_WorkT_SeenBy_w, -1); // move.w #-1,AI_WorkT_SeenBy_w(a2)
                }
            }

            // .no_team:
            if ((byte) d2 > 1) {                       // cmp.b #1,d2 ; ble .noexplode
                // SAVEREGS
                int la1 = a1;
                la1 -= Mem.l(Lvl_ObjectPointsPtr_l);   // sub.l Lvl_ObjectPointsPtr_l,a1
                la1 += ObjRotated_vl;                  // add.l #ObjRotated_vl,a1
                Mem.wl(HiresData.Aud_NoiseX_w, Mem.l(la1)); // move.l (a1),Aud_NoiseX_w (couvre NoiseX+NoiseZ)
                Mem.ww(HiresData.Aud_NoiseVol_w, 400); // move.w #400,Aud_NoiseVol_w
                Mem.ww(HiresData.Aud_SampleNum_w, 14); // move.w #14,Aud_SampleNum_w
                Mem.wb(HiresData.Aud_ChannelPick_b, 1); // move.b #1,Aud_ChannelPick_b
                Mem.wb(HiresData.notifplaying, 0);     // clr.b notifplaying
                Mem.wb(HiresData.backbeat, 0xFF);      // st backbeat
                Mem.ww(HiresData.IDNUM, Mem.uw(a0));   // move.w (a0),IDNUM
                Mem.wb(HiresData.PlayEcho, Mem.ub(NewaliencontrolData.ALIENECHO)); // move.b ALIENECHO,PlayEcho
                Hires.MakeSomeNoise();                 // jsr MakeSomeNoise
                // GETREGS
                // TODO (original) - just adjust the pointer, this is silly
                // SAVEREGS (d2 sera restauré par GETREGS — copie locale td2)
                int td0 = 0;                           // move.w #0,d0
                int td2 = setw(d2, ((short) d2) >> 2); // asr.w #2,d2
                if ((short) td2 <= 0) {                // tst.w d2 ; bgt.s .ko
                    td2 = setw(td2, 1);                // moveq #1,d2
                }
                // .ko:
                int td3 = 31;                          // move.w #31,d3
                Newanims.Anim_ExplodeIntoBits(td0, td2, td3, a0); // jsr Anim_ExplodeIntoBits (a0 = entité)
                // GETREGS (d2 restauré à la valeur des dégâts)

                if ((byte) d2 >= 40) {                 // cmp.b #40,d2 ; blt .noexplode
                    Macros.FREE_ENT(a0);               // FREE_ENT a0
                    return;                            // rts
                }
            }

            // .noexplode:
            // SAVEREGS
            int la1 = a1;
            la1 -= Mem.l(Lvl_ObjectPointsPtr_l);       // sub.l Lvl_ObjectPointsPtr_l,a1
            la1 += ObjRotated_vl;                      // add.l #ObjRotated_vl,a1
            Mem.wl(HiresData.Aud_NoiseX_w, Mem.l(la1)); // move.l (a1),Aud_NoiseX_w
            Mem.ww(HiresData.Aud_NoiseVol_w, 200);     // move.w #200,Aud_NoiseVol_w
            Mem.ww(HiresData.Aud_SampleNum_w, Mem.uw(NewaliencontrolData.screamsound)); // move.w screamsound,Aud_SampleNum_w
            Mem.wb(HiresData.Aud_ChannelPick_b, 1);    // move.b #1,Aud_ChannelPick_b
            Mem.wb(HiresData.notifplaying, 0);         // clr.b notifplaying
            Mem.wb(HiresData.backbeat, 0xFF);          // st backbeat
            Mem.ww(HiresData.IDNUM, Mem.uw(a0));       // move.w (a0),IDNUM
            Mem.wb(HiresData.PlayEcho, Mem.ub(NewaliencontrolData.ALIENECHO)); // move.b ALIENECHO,PlayEcho
            Hires.MakeSomeNoise();                     // jsr MakeSomeNoise
            // GETREGS

            Mem.ww(a0 + EntT_Timer3_w, 25);            // move.w #25,EntT_Timer3_w(a0)
            Mem.ww(a0 + EntT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),EntT_ZoneID_w(a0)
            return;                                    // rts
        }

        // .not_dead_yet:
        Mem.wb(a0 + EntT_DamageTaken_b, 0);            // clr.b EntT_DamageTaken_b(a0)
        // SAVEREGS
        int la1 = a1;
        la1 -= Mem.l(Lvl_ObjectPointsPtr_l);           // sub.l Lvl_ObjectPointsPtr_l,a1
        la1 += ObjRotated_vl;                          // add.l #ObjRotated_vl,a1
        Mem.wl(HiresData.Aud_NoiseX_w, Mem.l(la1));    // move.l (a1),Aud_NoiseX_w
        Mem.ww(HiresData.Aud_NoiseVol_w, 200);         // move.w #200,Aud_NoiseVol_w
        Mem.ww(HiresData.Aud_SampleNum_w, Mem.uw(NewaliencontrolData.screamsound)); // move.w screamsound,Aud_SampleNum_w
        Mem.wb(HiresData.Aud_ChannelPick_b, 1);        // move.b #1,Aud_ChannelPick_b
        Mem.wb(HiresData.notifplaying, 0);             // clr.b notifplaying
        Mem.ww(HiresData.IDNUM, Mem.uw(a0));           // move.w (a0),IDNUM
        Mem.wb(HiresData.backbeat, 0xFF);              // st backbeat
        Mem.wb(HiresData.PlayEcho, Mem.ub(NewaliencontrolData.ALIENECHO)); // move.b ALIENECHO,PlayEcho
        Hires.MakeSomeNoise();                         // jsr MakeSomeNoise
        // GETREGS
        // .noscream: rts
    }

    /** ai_DoTorch */
    private static void ai_DoTorch(int a0) {
        int d0 = Mem.w(NewaliencontrolData.ALIENBRIGHT); // move.w ALIENBRIGHT,d0
        if ((short) d0 >= 0) {                         // bge.s .nobright
            return;                                    // .nobright: rts
        }
        int d1 = Mem.uw(ObjectmoveData.newx);          // move.w newx,d1
        int d2 = Mem.uw(ObjectmoveData.newz);          // move.w newz,d2
        int d3 = Mem.w(a0 + 4);                        // move.w 4(a0),d3 ; ext.l d3
        d3 <<= 7;                                      // asl.l #7,d3
        Mem.wl(Anim_BrightY_l, d3);                    // move.l d3,Anim_BrightY_l
        int d4 = Mem.uw(a0 + EntT_CurrentAngle_w);     // move.w EntT_CurrentAngle_w(a0),d4
        d3 = Mem.uw(a0 + ObjT_ZoneID_w);               // move.w ObjT_ZoneID_w(a0),d3
        Newanims.Anim_BrightenPointsAngle(d0, d1, d2, d3, d4); // jsr Anim_BrightenPointsAngle
        // rts
    }

    /** ai_DoWalkAnim (= ai_DoAttackAnim — même point d'entrée dans l'original). */
    private static void ai_DoWalkAnim(int a0) {
        ai_DoAttackAnim(a0);
    }

    /** ai_DoAttackAnim */
    private static void ai_DoAttackAnim(int a0) {
        // move.l d0,-(a7) — d0 local
        int a6 = Mem.l(NewaliencontrolData.AlienAnimPtr_l); // move.l AlienAnimPtr_l,a6
        int a5 = Mem.l(WorkspacePtr_l);                // move.l WorkspacePtr_l,a5
        int d1 = 0;                                    // moveq #0,d1
        d1 = setb(d1, Mem.ub(a5 + 2));                 // move.b 2(a5),d1
        if ((byte) d1 == 0) {                          // bne.s .notview
            d1 = 0;                                    // moveq #0,d1
            if ((byte) Mem.b(AI_VecObj_w) != 1) {      // cmp.b #1,AI_VecObj_w ; beq.s .notview
                int d0 = Newaliencontrol.ViewpointToDraw(a0); // jsr ViewpointToDraw
                d0 = setw(d0, d0 + d0);                // add.w d0,d0
                d1 = setw(0, d0);                      // move.w d0,d1
            }
        }
        // .notview:
        d1 = muls(d1, A_OptLen);                       // muls #A_OptLen,d1
        a6 += d1;                                      // add.l d1,a6
        d1 = Mem.uw(a0 + EntT_Timer2_w);               // move.w EntT_Timer2_w(a0),d1
        if (Mem.b(a5 + 1) >= 0) {                      // tst.b 1(a5) ; blt.s .nospec
            d1 = setb(d1, Mem.ub(a5 + 1));             // move.b 1(a5),d1 (octet faible seul, comme move.b)
        }
        // .nospec:
        d1 = muls(d1, A_FrameLen);                     // muls #A_FrameLen,d1
        Mem.wb(a5 + 1, 0xFF);                          // st 1(a5)
        Mem.wb(ai_DoAction_b, Mem.ub(a5));             // move.b (a5),ai_DoAction_b
        Mem.wb(a5, 0);                                 // clr.b (a5)
        Mem.wb(ai_FinishedAnim_b, Mem.ub(a5 + 3));     // move.b 3(a5),ai_FinishedAnim_b
        Mem.wb(a5 + 3, 0);                             // clr.b 3(a5)
        Mem.wl(a0 + 8, 0);                             // move.l #0,8(a0)
        Mem.wb(a0 + 9, Mem.ub(a6 + (short) d1));       // move.b (a6,d1.w),9(a0)
        int d0 = Mem.b(a6 + (short) d1 + 1);           // move.b 1(a6,d1.w),d0 ; ext.w d0
        if (d0 <= 0) {                                 // bgt.s .noflip
            Mem.wb(a0 + 10, 128);                      // move.b #128,10(a0)
            d0 = -d0;                                  // neg.w d0
        }
        // .noflip:
        d0 = setw(d0, d0 - 1);                         // sub.w #1,d0
        Mem.wb(a0 + 11, d0);                           // move.b d0,11(a0)
        Mem.ww(ai_AnimFacing_w, 0);                    // move.w #0,ai_AnimFacing_w
        if ((byte) Mem.b(AI_VecObj_w) == 1) {          // cmp.b #1,AI_VecObj_w ; bne.s .noanimface
            Mem.ww(ai_AnimFacing_w, Mem.uw(a6 + (short) d1 + 2)); // move.w 2(a6,d1.w),ai_AnimFacing_w
        }

        // .noanimface:
        Macros.FREE_ENT_2(a0, ENT_PREV);               // FREE_ENT_2 a0,ENT_PREV

        int d3 = Mem.w(HiresData.AUXOBJ);              // move.w AUXOBJ,d3
        if (d3 >= 0) {                                 // blt .noaux
            d0 = Mem.b(a6 + (short) d1 + 8);           // move.b 8(a6,d1.w),d0
            if (d0 >= 0) {                             // blt .noaux
                Mem.ww(a0 + ObjT_ZoneID_w + ENT_PREV, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),...
                Mem.ww(a0 + EntT_ZoneID_w + ENT_PREV, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),...
                Mem.ww(a0 + 4 + ENT_PREV, Mem.uw(a0 + 4)); // move.w 4(a0),4+ENT_PREV(a0)
                Mem.wb(a0 + ShotT_InUpperZone_b + ENT_PREV, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ...
                int d4 = Mem.b(a6 + (short) d1 + 9);   // move.b 9(a6,d1.w),d4 ; ext.w d4
                int d5 = Mem.b(a6 + (short) d1 + 10);  // move.b 10(a6,d1.w),d5 ; ext.w d5
                d4 = d4 + d4;                          // add.w d4,d4
                d5 = d5 + d5;                          // add.w d5,d5
                Mem.ww(a0 + ShotT_AuxOffsetX_w + ENT_PREV, d4); // move.w d4,ShotT_AuxOffsetX_w+ENT_PREV(a0)
                Mem.ww(a0 + ShotT_AuxOffsetY_w + ENT_PREV, d5); // move.w d5,ShotT_AuxOffsetY_w+ENT_PREV(a0)
                int a4 = Mem.l(HiresData.GLF_DatabasePtr_l); // move.l GLF_DatabasePtr_l,a4
                int a2 = a4;                           // move.l a4,a2
                a4 += GLFT_ObjectDefAnims_l;           // add.l #GLFT_ObjectDefAnims_l,a4
                a2 += GLFT_ObjectDefs;                 // add.l #GLFT_ObjectDefs,a2
                int d4b = setw(0, d3);                 // move.w d3,d4
                d3 = muls(d3, O_AnimSize);             // muls #O_AnimSize,d3
                d4b = muls(d4b, ODefT_SizeOf_l);       // muls #ODefT_SizeOf_l,d4
                a2 += d4b;                             // add.l d4,a2
                a4 += d3;                              // add.l d3,a4
                d0 = muls(d0, O_FrameStoreSize);       // muls #O_FrameStoreSize,d0
                int gfx = Mem.w(a2 + ODefT_GFXType_w); // cmp.w #1,ODefT_GFXType_w(a2)
                if (gfx < 1) {                         // blt.s .bitmap
                    // .bitmap:
                    Mem.wl(a0 + OBJ_PREV + ObjT_YPos_l, 0); // move.l #0,OBJ_PREV+ObjT_YPos_l(a0)
                    Mem.wb(a0 + OBJ_PREV + 9, Mem.ub(a4 + (short) d0)); // move.b (a4,d0.w),OBJ_PREV+9(a0)
                    Mem.wb(a0 + OBJ_PREV + 11, Mem.ub(a4 + (short) d0 + 1)); // move.b 1(a4,d0.w),OBJ_PREV+11(a0)
                    Mem.ww(a0 + OBJ_PREV + 6, Mem.uw(a4 + (short) d0 + 2)); // move.w 2(a4,d0.w),OBJ_PREV+6(a0)
                } else if (gfx == 1) {                 // beq.s .vector
                    // .vector:
                    Mem.wl(a0 + OBJ_PREV + ObjT_YPos_l, 0); // move.l #0,OBJ_PREV+ObjT_YPos_l(a0)
                    Mem.wb(a0 + OBJ_PREV + 9, Mem.ub(a4 + (short) d0)); // move.b (a4,d0.w),OBJ_PREV+9(a0) ; hacks?
                    Mem.wb(a0 + OBJ_PREV + 11, Mem.ub(a4 + (short) d0 + 1)); // move.b 1(a4,d0.w),OBJ_PREV+11(a0)
                    Mem.ww(a0 + OBJ_PREV + 6, 0xffff); // move.w #$ffff,OBJ_PREV+6(a0)
                } else {
                    // .glare:
                    Mem.wl(a0 + OBJ_PREV + ObjT_YPos_l, 0); // move.l #0,... ; why if we are overwriting later?
                    int td3 = Mem.b(a4 + (short) d0);  // move.b (a4,d0.w),d3 ; ext.w d3
                    td3 = -td3;                        // neg.w d3
                    Mem.ww(a0 + OBJ_PREV + ObjT_YPos_l, td3); // move.w d3,OBJ_PREV+ObjT_YPos_l(a0)
                    Mem.wb(a0 + OBJ_PREV + 11, Mem.ub(a4 + (short) d0 + 1)); // move.b 1(a4,d0.w),OBJ_PREV+11(a0) ; hacks ?
                    Mem.ww(a0 + OBJ_PREV + 6, Mem.uw(a4 + (short) d0 + 2)); // move.w 2(a4,d0.w),OBJ_PREV+6(a0) ; hacks ?
                }
            }
        }

        // .noaux:
        Mem.ww(a0 + 6, -1);                            // move.w #-1,6(a0)
        int vec = (byte) Mem.b(AI_VecObj_w);
        if (vec == 1) {                                // cmp.b #1,AI_VecObj_w ; beq.s .nosize
            // .nosize: move.l (a7)+,d0 ; rts
            return;
        }
        if (vec > 1) {                                 // bgt.s .setlight
            // .setlight:
            Mem.ww(a0 + 6, Mem.uw(a6 + (short) d1 + 2)); // move.w 2(a6,d1.w),6(a0)
            d1 = Mem.ub(AI_VecObj_w);                  // move.b AI_VecObj_w,d1
            Mem.wb(a0 + 10, Mem.ub(a0 + 10) | d1);     // or.b d1,10(a0)
            return;                                    // move.l (a7)+,d0 ; rts
        }
        Mem.ww(a0 + 6, Mem.uw(a6 + (short) d1 + 2));   // move.w 2(a6,d1.w),6(a0)
        // move.l (a7)+,d0 ; rts
    }

    /** ai_CheckAttackOnGround — renvoie d0 (octet $FF si l'attaque au sol est possible). */
    private static int ai_CheckAttackOnGround(int a0) {
        int a3 = Mem.l(Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a3
        int d1 = 0;                                    // moveq #0,d1
        d1 = setb(d1, Mem.ub(a3 + ZoneT_ControlPoint_w)); // move.b ZoneT_ControlPoint_w(a3),d1
        if (Mem.b(Plr1_StoodInTop_b) != 0) {           // tst.b Plr1_StoodInTop_b ; beq.s .player_not_in_top
            d1 = setb(d1, Mem.ub(a3 + ZoneT_ControlPoint_w + 1)); // move.b ZoneT_ControlPoint_w+1(a3),d1
        }

        // .player_not_in_top:
        int d3 = setw(0, d1);                          // move.w d1,d3
        int d0 = Mem.uw(a0 + EntT_CurrentControlPoint_w); // move.w EntT_CurrentControlPoint_w(a0),d0
        if ((short) d1 == (short) d0) {                // cmp.w d0,d1 ; beq.s .attack_player
            return setb(d0, 0xFF);                     // .attack_player: st d0 ; rts
        }

        d0 = Objectmove.GetNextCPt(d0, d1);            // jsr GetNextCPt

        if ((short) d3 == (short) d0) {                // cmp.w d0,d3 ; beq.s .attack_player
            return setb(d0, 0xFF);                     // .attack_player: st d0 ; rts
        }
        // .dont_attack_player:
        return setb(d0, 0);                            // clr.b d0 ; rts
    }

    /** ai_GetRoomCPT */
    private static void ai_GetRoomCPT(int a0) {
        int a2 = Mem.l(ObjectmoveData.Obj_ZonePtr_l);  // move.l Obj_ZonePtr_l,a2
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a2 + ZoneT_ControlPoint_w)); // move.b ZoneT_ControlPoint_w(a2),d0
        if (Mem.b(a0 + ShotT_InUpperZone_b) != 0) {    // tst.b ShotT_InUpperZone_b(a0) ; beq.s .player_not_in_top
            d0 = setb(d0, Mem.ub(a2 + ZoneT_ControlPoint_w + 1)); // move.b ZoneT_ControlPoint_w+1(a2),d0
        }
        // .player_not_in_top:
        Mem.ww(a0 + EntT_CurrentControlPoint_w, d0);   // move.w d0,EntT_CurrentControlPoint_w(a0)
        // rts
    }

    /** ai_CalcSqrt — d2 = sqrt(d2) (3 itérations de Newton). */
    public static int ai_CalcSqrt(int d2) {
        if (d2 == 0) {                                 // tst.l d2 ; beq .oksqr
            return d2;                                 // .oksqr: rts
        }
        // movem.l ...,-(a7) — locaux en Java
        int d0 = 31;                                   // move.w #31,d0
        while ((d2 & (1 << (d0 & 31))) == 0) {         // .findhigh: btst d0,d2 ; bne .foundhigh
            d0 = setw(d0, d0 - 1);                     // dbra d0,.findhigh
            if ((short) d0 == -1) {
                break;
            }
        }
        // .foundhigh:
        d0 = setw(d0, ((short) d0) >> 1);              // asr.w #1,d0
        int d3 = 0;                                    // clr.l d3
        d3 |= 1 << (d0 & 31);                          // bset d0,d3
        d0 = d3;                                       // move.l d3,d0

        int d1 = setw(0, d0);                          // move.w d0,d1
        d1 = muls(d1, d1);                             // muls d1,d1 ; x*x
        d1 -= d2;                                      // sub.l d2,d1 ; x*x-a
        d1 >>= 1;                                      // asr.l #1,d1 ; (x*x-a)/2
        d1 = ab3d2.M68k.divs(d1, d0);                  // divs d0,d1 ; (x*x-a)/2x
        d0 = setw(d0, d0 - d1);                        // sub.w d1,d0 ; second approx
        if ((short) d0 <= 0) {                         // bgt .stillnot0
            d0 = setw(d0, 1);                          // move.w #1,d0
        }
        // .stillnot0:
        d1 = setw(d1, d0);                             // move.w d0,d1
        d1 = muls(d1, d1);                             // muls d1,d1
        d1 -= d2;                                      // sub.l d2,d1
        d1 >>= 1;                                      // asr.l #1,d1
        d1 = ab3d2.M68k.divs(d1, d0);                  // divs d0,d1
        d0 = setw(d0, d0 - d1);                        // sub.w d1,d0 ; second approx
        if ((short) d0 <= 0) {                         // bgt .stillnot02
            d0 = setw(d0, 1);                          // move.w #1,d0
        }
        // .stillnot02:
        d1 = setw(d1, d0);                             // move.w d0,d1
        d1 = muls(d1, d1);                             // muls d1,d1
        d1 -= d2;                                      // sub.l d2,d1
        d1 >>= 1;                                      // asr.l #1,d1
        d1 = ab3d2.M68k.divs(d1, d0);                  // divs d0,d1
        d0 = setw(d0, d0 - d1);                        // sub.w d1,d0 ; second approx
        if ((short) d0 <= 0) {                         // bgt .stillnot03
            d0 = setw(d0, 1);                          // move.w #1,d0
        }
        // .stillnot03:
        d2 = (short) d0;                               // move.w d0,d2 ; ext.l d2
        // movem.l (a7)+,...
        // .oksqr:
        return d2;                                     // rts
    }
}
