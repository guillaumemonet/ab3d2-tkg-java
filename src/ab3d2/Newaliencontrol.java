package ab3d2;

import ab3d2.c.GameC;
import ab3d2.c.Message;
import ab3d2.data.TablesData;
import ab3d2.modules.Ai;

import static ab3d2.Defs.*;
import static ab3d2.M68k.divs;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;
import static ab3d2.NewaliencontrolData.*;
import static ab3d2.ObjectmoveData.*;
import static ab3d2.bss.AiBss.AI_DefaultMode_w;
import static ab3d2.bss.AiBss.AI_FollowupMode_w;
import static ab3d2.bss.AiBss.AI_FollowupSpeed_w;
import static ab3d2.bss.AiBss.AI_FollowupTimer_w;
import static ab3d2.bss.AiBss.AI_NoEnemies_b;
import static ab3d2.bss.AiBss.AI_ProwlSpeed_w;
import static ab3d2.bss.AiBss.AI_ReactionTime_w;
import static ab3d2.bss.AiBss.AI_ResponseMode_w;
import static ab3d2.bss.AiBss.AI_ResponseSpeed_w;
import static ab3d2.bss.AiBss.AI_RetreatMode_w;
import static ab3d2.bss.AiBss.AI_RetreatSpeed_w;
import static ab3d2.bss.AiBss.AI_VecObj_w;
import static ab3d2.bss.AnimBss.Anim_DoorAndLiftLocks_l;
import static ab3d2.bss.AnimBss.Anim_TempFrames_w;
import static ab3d2.bss.LevelBss.AI_AlienShotDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_DataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.PlayerBss.*;
import static ab3d2.bss.TablesBss.ObjRotated_vl;
import static ab3d2.data.TablesData.COSINE_OFS;
import static ab3d2.data.TablesData.SinCosTable_vw;
import static ab3d2.data.TextData.Game_CantCollectItemText_vb;

/**
 * Traduction littérale de ab3d2_source/newaliencontrol.s
 *
 * Dispatch haut niveau des entités :
 *  - ItsAnAlien : charge tous les paramètres AI depuis le GLF (AlienT) puis
 *    appelle AI_MainRoutine — laisse a2 = GLF+AlienShootDefs (hérité ensuite,
 *    cf. Objectmove.Obj_DoCollision) ;
 *  - ItsAnObject : dispatch ENT_TYPE_* (Collectable/Activatable/Destructable/
 *    Decoration) avec ramassage d'inventaire (Game_*Inventory*), animations
 *    DEFANIMOBJ/ACTANIMOBJ (records de 6 octets : frame, taille, aux, dy,
 *    frame suivante) ;
 *  - ViewpointToDraw : choix de la vue (face/droite/dos/gauche) par produit
 *    sin/cos ; RunAround : décalage latéral des charges ;
 *  - SHOOTPLAYER1/2 : tir hitscan raté → trace un impact mural (MoveObject
 *    en boucle exitfirst jusqu'au mur, slot dans Plr_ShotData/AlienShotData) ;
 *  - FireAtPlayer1/2 : tir de projectile avec anticipation (CalcDist +
 *    XDiff_w/ZDiff_w), offset latéral SHOTOFFMULT, vitesse SHOTSPEED,
 *    montée/descente vers la tête du joueur (SHOTSHIFT).
 */
public final class Newaliencontrol {

    private Newaliencontrol() {
    }

    public static boolean dbgCollect = false;  // DIAG : trace la collecte des objets ramassables

    /** ItsAnAlien — a0 = entité alien. */
    public static void ItsAnAlien(int a0) {
        if (Mem.b(AI_NoEnemies_b) == 0) {              // tst.b AI_NoEnemies_b ; beq.s .no_enemies (saut pris si ==0)
            // .no_enemies:
            Mem.ww(a0 + ObjT_ZoneID_w, -1);            // move.w #-1,ObjT_ZoneID_w(a0)
            return;                                    // rts
        }

        Mem.wl(StepUpVal, 32 * 256);                   // move.l #32*256,StepUpVal
        Mem.wl(StepDownVal, 32 * 256);                 // move.l #32*256,StepDownVal
        Mem.ww(a0 + EntT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),EntT_ZoneID_w(a0)
        int d2 = Mem.w(a0 + ObjT_ZoneID_w);            // move.w ObjT_ZoneID_w(a0),d2
        if (d2 < 0) {                                  // bge.s .ok_alive
            return;                                    // rts
        }

        // .ok_alive:
        int a5 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a5
        int d0 = Mem.l(a5 + ((short) d2) * 4);         // move.l (a5,d2.w*4),d0
        Mem.wl(Obj_ZonePtr_l, d0);                     // move.l d0,Obj_ZonePtr_l
        int a6 = d0;                                   // move.l d0,a6
        Mem.wb(ALIENECHO, Mem.ub(a6 + ZoneT_Echo_b));  // move.b ZoneT_Echo_b(a6),ALIENECHO
        d0 = 0;                                        // moveq #0,d0
        a6 = Mem.l(HiresData.GLF_DatabasePtr_l);       // move.l GLF_DatabasePtr_l,a6
        a5 = a6;                                       // move.l a6,a5
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        a5 += GLFT_AlienBrights_l;                     // add.l #GLFT_AlienBrights_l,a5
        int d1 = Mem.uw(a5 + ((short) d0) * 2);        // move.w (a5,d0.w*2),d1
        d1 = setw(d1, -(short) d1);                    // neg.w d1
        Mem.ww(ALIENBRIGHT, d1);                       // move.w d1,ALIENBRIGHT
        d0 = muls(d0, A_AnimLen);                      // muls #A_AnimLen,d0
        a6 += GLFT_AlienAnims_l;                       // add.l #GLFT_AlienAnims_l,a6
        a6 += d0;                                      // add.l d0,a6
        Mem.wl(AlienAnimPtr_l, a6);                    // move.l a6,AlienAnimPtr_l
        int a1 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a1
        int a2 = a1;                                   // move.l a1,a2
        a2 += GLFT_AlienShootDefs_l;                   // add.l #GLFT_AlienShootDefs_l,a2
        a1 = a1 + GLFT_AlienDefs_l;                    // lea GLFT_AlienDefs_l(a1),a1
        d0 = 0;                                        // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        d1 = Mem.l(a2 + ((short) d0) * 8);             // move.l (a2,d0.w*8),d1
        d1 <<= 7;                                      // asl.l #7,d1
        Mem.wl(SHOTYOFF, d1);                          // move.l d1,SHOTYOFF
        d1 = Mem.uw(a2 + ((short) d0) * 8 + 6);        // move.w 6(a2,d0.w*8),d1
        d1 = setw(d1, -(short) d1);                    // neg.w d1
        d1 = setw(d1, d1 << 2);                        // asl.w #2,d1
        Mem.ww(SHOTOFFMULT, d1);                       // move.w d1,SHOTOFFMULT
        d0 = muls(d0, AlienT_SizeOf_l);                // muls #AlienT_SizeOf_l,d0
        a1 += (short) d0;                              // add.w d0,a1 ; ptr to alien stats
        d0 = Mem.w(a1 + AlienT_Height_w);              // move.w AlienT_Height_w(a1),d0 ; ext.l d0
        d0 <<= 7;                                      // asl.l #7,d0
        Mem.wl(ObjectmoveData.thingheight, d0);        // move.l d0,thingheight
        Mem.ww(HiresData.AUXOBJ, Mem.uw(a1 + AlienT_Auxilliary_w)); // move.w AlienT_Auxilliary_w(a1),AUXOBJ
        Mem.ww(HiresData.CollId, Mem.uw(a0));          // move.w (a0),CollId
        Mem.wb(AI_VecObj_w, Mem.ub(a1 + 1));           // move.b 1(a1),AI_VecObj_w
        Mem.ww(AI_ReactionTime_w, Mem.uw(a1 + AlienT_ReactionTime_w));        // move.w ...,AI_ReactionTime_w
        Mem.ww(AI_DefaultMode_w, Mem.uw(a1 + AlienT_DefaultBehaviour_w));     // move.w ...,AI_DefaultMode_w
        Mem.ww(AI_ResponseMode_w, Mem.uw(a1 + AlienT_ResponseBehaviour_w));   // move.w ...,AI_ResponseMode_w
        Mem.ww(AI_RetreatMode_w, Mem.uw(a1 + AlienT_RetreatBehaviour_w));     // move.w ...,AI_RetreatMode_w
        Mem.ww(AI_FollowupMode_w, Mem.uw(a1 + AlienT_FollowupBehaviour_w));   // move.w ...,AI_FollowupMode_w
        Mem.ww(AI_ProwlSpeed_w, Mem.uw(a1 + AlienT_DefaultSpeed_w));          // move.w ...,AI_ProwlSpeed_w
        Mem.ww(AI_ResponseSpeed_w, Mem.uw(a1 + AlienT_ResponseSpeed_w));      // move.w ...,AI_ResponseSpeed_w
        Mem.ww(AI_RetreatSpeed_w, Mem.uw(a1 + AlienT_RetreatSpeed_w));        // move.w ...,AI_RetreatSpeed_w
        Mem.ww(AI_FollowupSpeed_w, Mem.uw(a1 + AlienT_FollowupSpeed_w));      // move.w ...,AI_FollowupSpeed_w
        Mem.ww(AI_FollowupTimer_w, Mem.uw(a1 + AlienT_FollowupTimeout_w));    // move.w ...,AI_FollowupTimer_w
        d0 = Mem.uw(a1 + AlienT_Girth_w);              // move.w AlienT_Girth_w(a1),d0
        Mem.wb(Obj_AwayFromWall_b, Mem.ub(diststowall + ((short) d0) * 4 + 1)); // move.b diststowall+1(pc,d0.w*4),...
        Mem.ww(Obj_ExtLen_w, Mem.uw(diststowall + ((short) d0) * 4 + 2));       // move.w diststowall+2(pc,d0.w*4),...
        Ai.AI_MainRoutine(a0);                         // jsr AI_MainRoutine
        // rts
    }

    /** ItsAnObject — a0 = entité objet ; dispatch ENT_TYPE_*. */
    public static void ItsAnObject(int a0) {
        int a1 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a1
        a1 = a1 + GLFT_ObjectDefs;                     // lea GLFT_ObjectDefs(a1),a1
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        d0 = muls(d0, ODefT_SizeOf_l);                 // muls #ODefT_SizeOf_l,d0
        a1 += (short) d0;                              // add.w d0,a1 ; pointer to obj stats.
        Mem.wl(obj_StatPtr_l, a1);                     // move.l a1,obj_StatPtr_l
        d0 = Mem.w(a1);                                // move.w (a1),d0
        if (d0 < ENT_TYPE_ACTIVATABLE) {               // cmp.w #ENT_TYPE_ACTIVATABLE,d0 ; blt Collectable
            Collectable(a0, a1);
            return;
        }
        if (d0 == ENT_TYPE_ACTIVATABLE) {              // beq Activatable
            Activatable(a0, a1);
            return;
        }
        if (d0 < ENT_TYPE_DECORATION) {                // cmp.w #ENT_TYPE_DECORATION,d0 ; blt Destructable
            Destructable(a0, a1);
            return;
        }
        if (d0 == ENT_TYPE_DECORATION) {               // beq Decoration
            Decoration(a0, a1);
        }
        // rts
    }

    /** Motif partagé "worry_about" : hauteur sol/plafond → 4(a0)/ZPos ; renvoie l'altitude calculée. */
    private static int worryHeight(int a0, int a2stats) {
        int d0 = Mem.w(a0 + ObjT_ZoneID_w);
        int a1 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a1
        a1 = Mem.l(a1 + ((short) d0) * 4);             // move.l (a1,d0.w*4),a1
        int h;
        if (Mem.w(a2stats + ODefT_FloorCeiling_w) != 0) { // tst.w ODefT_FloorCeiling_w(a2) ; beq.s .on_floor
            h = Mem.l(a1 + ZoneT_Roof_l);              // move.l ZoneT_Roof_l(a1),d0
            if (Mem.b(a0 + ShotT_InUpperZone_b) != 0) { // tst.b ShotT_InUpperZone_b(a0) ; beq.s .in_lower_zonec
                h = Mem.l(a1 + ZoneT_UpperRoof_l);     // move.l ZoneT_UpperRoof_l(a1),d0
            }
            // .in_lower_zonec: bra.s .on_ceiling
        } else {
            // .on_floor:
            h = Mem.l(a1 + ZoneT_Floor_l);             // move.l ZoneT_Floor_l(a1),d0
            if (Mem.b(a0 + ShotT_InUpperZone_b) != 0) { // tst.b ShotT_InUpperZone_b(a0) ; beq.s .in_lower_zone
                h = Mem.l(a1 + ZoneT_UpperFloor_l);    // move.l ZoneT_UpperFloor_l(a1),d0
            }
        }
        // .in_lower_zone: / .on_ceiling:
        return h >> 7;                                 // asr.l #7,d0
    }

    /** GUNHELD — this is a player gun in his hand (a1 = stats). */
    private static void GUNHELD(int a0, int a1) {
        int a2 = a1;                                   // move.l a1,a2
        ACTANIMOBJ(a0, a2);                            // jsr ACTANIMOBJ
        // rts
    }

    /** Collectable */
    private static void Collectable(int a0, int a1) {
        int d0 = Mem.w(a0 + ObjT_ZoneID_w);            // move.w ObjT_ZoneID_w(a0),d0
        if (d0 < 0) {                                  // bge.s .ok_in_room
            return;                                    // rts
        }

        // .ok_in_room:
        if (Mem.b(a0 + EntT_WhichAnim_b) != 0) {       // tst.b EntT_WhichAnim_b(a0) ; bne.s GUNHELD
            GUNHELD(a0, a1);
            return;
        }

        Mem.ww(a0 + EntT_ZoneID_w, d0);                // move.w d0,EntT_ZoneID_w(a0)
        if (Mem.b(AI_NoEnemies_b) != 0) {              // tst.b AI_NoEnemies_b ; beq.s .no_locks
            int d1 = Mem.l(a0 + EntT_DoorsAndLiftsHeld_l); // move.l EntT_DoorsAndLiftsHeld_l(a0),d1
            Mem.wl(Anim_DoorAndLiftLocks_l, Mem.l(Anim_DoorAndLiftLocks_l) | d1); // or.l d1,Anim_DoorAndLiftLocks_l
        }

        // .no_locks:
        if (Mem.b(a0 + ShotT_Worry_b) == 0) {          // tst.b ShotT_Worry_b(a0) ; bne.s .worry_about
            return;                                    // rts
        }

        // .worry_about:
        Mem.wb(a0 + ShotT_Worry_b, Mem.ub(a0 + ShotT_Worry_b) & 0x80); // and.b #$80,ShotT_Worry_b(a0)
        int a2 = a1;                                   // move.l a1,a2
        Mem.ww(a0 + 4, worryHeight(a0, a2));           // ... ; asr.l #7,d0 ; move.w d0,4(a0)
        DEFANIMOBJ(a0, a2);                            // bsr DEFANIMOBJ

        d0 = Plr1_CheckObjectCollide(a0);              // bsr Plr1_CheckObjectCollide
        if (dbgCollect) System.out.println("[collect] def=" + Mem.ub(a0 + EntT_Type_b)
            + " worry collide=" + ((byte) d0 != 0));
        if ((byte) d0 != 0) {                          // tst.b d0 ; beq.s .NotCollected1
            d0 = Plr1_CollectItem(a0);                 // bsr Plr1_CollectItem
            if (dbgCollect) System.out.println("[collect]   CollectItem=" + ((short) d0));
            if ((short) d0 != 0) {                     // tst.w d0 ; beq.s .NotCollected1
                Mem.ww(a0 + ObjT_ZoneID_w, -1);        // move.w #-1,ObjT_ZoneID_w(a0)
                Mem.wb(a0 + ShotT_Worry_b, 0);         // clr.b ShotT_Worry_b(a0)
            }
        }

        // .NotCollected1:
        if ((byte) Mem.b(Plr_MultiplayerType_b) != Hires.PLR_SINGLE) { // cmp.b #PLR_SINGLE,... ; beq.s .NotCollected2
            d0 = Plr2_CheckObjectCollide(a0);          // bsr Plr2_CheckObjectCollide
            if ((byte) d0 != 0) {                      // tst.b d0 ; beq.s .NotCollected2
                d0 = Plr2_CollectItem(a0);             // bsr Plr2_CollectItem
                if ((short) d0 != 0) {                 // tst.w d0 ; beq.s .NotCollected2
                    // todo (original) - is this what is removing the item?
                    Mem.ww(a0 + ObjT_ZoneID_w, -1);    // move.w #-1,ObjT_ZoneID_w(a0)
                    Mem.wb(a0 + ShotT_Worry_b, 0);     // clr.b ShotT_Worry_b(a0)
                }
            }
        }
        // .NotCollected2: rts
    }

    /** Activatable */
    private static void Activatable(int a0, int a1) {
        int d0 = Mem.w(a0 + ObjT_ZoneID_w);            // move.w ObjT_ZoneID_w(a0),d0
        if (d0 < 0) {                                  // bge.s .ok_in_room
            return;                                    // rts
        }

        // .ok_in_room:
        if (Mem.b(a0 + EntT_WhichAnim_b) != 0) {       // tst.b EntT_WhichAnim_b(a0) ; bne ACTIVATED
            ACTIVATED(a0, a1, d0);
            return;
        }

        Mem.ww(a0 + EntT_ZoneID_w, d0);                // move.w d0,EntT_ZoneID_w(a0)
        if (Mem.b(AI_NoEnemies_b) != 0) {              // tst.b AI_NoEnemies_b ; beq.s .no_locks
            int d1 = Mem.l(a0 + EntT_DoorsAndLiftsHeld_l); // move.l EntT_DoorsAndLiftsHeld_l(a0),d1
            Mem.wl(Anim_DoorAndLiftLocks_l, Mem.l(Anim_DoorAndLiftLocks_l) | d1); // or.l d1,Anim_DoorAndLiftLocks_l
        }

        // .no_locks:
        if (Mem.b(a0 + ShotT_Worry_b) == 0) {          // tst.b ShotT_Worry_b(a0) ; bne.s .worry_about
            return;                                    // rts
        }

        // .worry_about:
        Mem.wb(a0 + ShotT_Worry_b, Mem.ub(a0 + ShotT_Worry_b) & 0x80); // and.b #$80,ShotT_Worry_b(a0)
        int a2 = a1;                                   // move.l a1,a2
        Mem.ww(a0 + 4, worryHeight(a0, a2));           // ... ; move.w d0,4(a0)

        DEFANIMOBJ(a0, a2);                            // bsr DEFANIMOBJ

        d0 = Plr1_CheckObjectCollide(a0);              // bsr Plr1_CheckObjectCollide
        if ((byte) d0 != 0                             // tst.b d0 ; beq.s .NotActivated1
                && Mem.b(Plr1_TmpSpcTap_b) != 0) {     // tst.b Plr1_TmpSpcTap_b ; beq.s .NotActivated1
            // The player has pressed the activation button within range of the object.
            Plr1_CollectItem(a0);                      // bsr Plr1_CollectItem
            Mem.ww(a0 + EntT_Timer1_w, 0);             // move.w #0,EntT_Timer1_w(a0)
            Mem.wb(a0 + EntT_WhichAnim_b, 0xFF);       // st EntT_WhichAnim_b(a0)
            Mem.ww(a0 + EntT_Timer2_w, 0);             // move.w #0,EntT_Timer2_w(a0)
            return;                                    // rts
        }

        // .NotActivated1:
        if ((byte) Mem.b(Plr_MultiplayerType_b) != Hires.PLR_SINGLE) { // cmp.b #PLR_SINGLE,... ; beq .NotActivated2
            d0 = Plr2_CheckObjectCollide(a0);          // bsr Plr2_CheckObjectCollide
            if ((byte) d0 != 0                         // tst.b d0 ; beq.s .NotActivated2
                    && Mem.b(Plr2_TmpSpcTap_b) != 0) { // tst.b Plr2_TmpSpcTap_b ; beq.s .NotActivated2
                // The player has pressed the spacebar within range of the object.
                Plr2_CollectItem(a0);                  // bsr Plr2_CollectItem
                Mem.ww(a0 + EntT_Timer1_w, 0);         // move.w #0,EntT_Timer1_w(a0)
                Mem.wb(a0 + EntT_WhichAnim_b, 0xFF);   // st EntT_WhichAnim_b(a0)
                Mem.ww(a0 + EntT_Timer2_w, 0);         // move.w #0,EntT_Timer2_w(a0)
            }
        }
        // .NotActivated2: rts
    }

    /** ACTIVATED (d0 = zone id à l'entrée) */
    private static void ACTIVATED(int a0, int a1, int d0) {
        Mem.ww(a0 + EntT_ZoneID_w, d0);                // move.w d0,EntT_ZoneID_w(a0)
        if (Mem.b(a0 + ShotT_Worry_b) == 0) {          // tst.b ShotT_Worry_b(a0) ; bne.s .worry_about
            return;                                    // rts
        }

        // .worry_about:
        Mem.wb(a0 + ShotT_Worry_b, Mem.ub(a0 + ShotT_Worry_b) & 0x80); // and.b #$80,ShotT_Worry_b(a0)
        int a2 = a1;                                   // move.l a1,a2
        Mem.ww(a0 + 4, worryHeight(a0, a2));           // ... ; move.w d0,4(a0)
        ACTANIMOBJ(a0, a2);                            // bsr ACTANIMOBJ

        d0 = Mem.uw(Anim_TempFrames_w);                // move.w Anim_TempFrames_w,d0
        Mem.ww(a0 + EntT_Timer2_w, Mem.uw(a0 + EntT_Timer2_w) + d0); // add.w d0,EntT_Timer2_w(a0)
        d0 = Mem.w(a2 + ODefT_ActiveTimeout_w);        // move.w ODefT_ActiveTimeout_w(a2),d0
        if (d0 >= 0                                    // blt.s .nottimeout
                && d0 <= Mem.w(a0 + EntT_Timer2_w)) {  // cmp.w EntT_Timer2_w(a0),d0 ; ble.s .DEACTIVATE
            // .DEACTIVATE:
            Mem.ww(a0 + EntT_Timer1_w, 0);             // move.w #0,EntT_Timer1_w(a0)
            Mem.wb(a0 + EntT_WhichAnim_b, 0);          // clr.b EntT_WhichAnim_b(a0)
            return;                                    // rts
        }

        // .nottimeout:
        d0 = Plr1_CheckObjectCollide(a0);              // bsr Plr1_CheckObjectCollide
        if ((byte) d0 != 0                             // tst.b d0 ; beq.s .NotDeactivated1
                && Mem.b(Plr1_TmpSpcTap_b) != 0) {     // tst.b Plr1_TmpSpcTap_b ; beq.s .NotDeactivated1
            // .DEACTIVATE:
            Mem.ww(a0 + EntT_Timer1_w, 0);             // move.w #0,EntT_Timer1_w(a0)
            Mem.wb(a0 + EntT_WhichAnim_b, 0);          // clr.b EntT_WhichAnim_b(a0)
            return;                                    // rts
        }

        // .NotDeactivated1:
        if ((byte) Mem.b(Plr_MultiplayerType_b) != Hires.PLR_SINGLE) { // cmp.b #PLR_SINGLE,... ; beq.s .NotDeactivated2
            d0 = Plr2_CheckObjectCollide(a0);          // bsr Plr2_CheckObjectCollide
            if ((byte) d0 != 0                         // tst.b d0 ; beq.s .NotDeactivated2
                    && Mem.b(Plr2_TmpSpcTap_b) != 0) { // tst.b Plr2_TmpSpcTap_b ; beq.s .NotDeactivated2
                Mem.ww(a0 + EntT_Timer1_w, 0);         // move.w #0,EntT_Timer1_w(a0)
                Mem.wb(a0 + EntT_WhichAnim_b, 0);      // clr.b EntT_WhichAnim_b(a0)
            }
        }
        // .NotDeactivated2: rts
    }

    /** Destructable */
    private static void Destructable(int a0, int a1) {
        int a3 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a3
        a3 += GLFT_ObjectDefs;                         // add.l #GLFT_ObjectDefs,a3
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        d0 = muls(d0, ODefT_SizeOf_l);                 // muls #ODefT_SizeOf_l,d0
        a3 += d0;                                      // add.l d0,a3
        d0 = 0;                                        // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_DamageTaken_b)); // move.b EntT_DamageTaken_b(a0),d0
        if ((short) d0 < Mem.w(a3 + ODefT_HitPoints_w)) { // cmp.w ODefT_HitPoints_w(a3),d0 ; blt StillHere
            StillHere(a0, a1);
            return;
        }

        if (Mem.b(a0 + EntT_HitPoints_b) != 0) {       // tst.b EntT_HitPoints_b(a0) ; beq.s .alreadydead
            if ((byte) Mem.b(Plr_MultiplayerType_b) == Hires.PLR_SINGLE) { // cmp.b #PLR_SINGLE,... ; bne.s .notext
                d0 = Mem.w(a0 + EntT_DisplayText_w);   // move.w EntT_DisplayText_w(a0),d0
                if (d0 >= 0) {                         // blt.s .notext
                    d0 = muls(d0, LVLT_MESSAGE_LENGTH); // muls #LVLT_MESSAGE_LENGTH,d0
                    d0 += Mem.l(Lvl_DataPtr_l);        // add.l Lvl_DataPtr_l,d0
                    // move.l a0,-(sp) ; ... ; move.l (sp)+,a0
                    Message.Msg_PushLine(d0, LVLT_MESSAGE_LENGTH | MSG_TAG_NARRATIVE); // CALLC Msg_PushLine
                }
            }
            // .notext:
            Mem.ww(a0 + EntT_Timer1_w, 0);             // move.w #0,EntT_Timer1_w(a0)
        }

        // .alreadydead:
        Mem.wb(a0 + EntT_HitPoints_b, 0);              // move.b #0,EntT_HitPoints_b(a0)
        d0 = Mem.w(a0 + ObjT_ZoneID_w);                // move.w ObjT_ZoneID_w(a0),d0
        if (d0 < 0) {                                  // bge.s .ok_in_room
            return;                                    // rts
        }

        // .ok_in_room:
        if (Mem.b(a0 + ShotT_Worry_b) == 0) {          // tst.b ShotT_Worry_b(a0) ; bne.s .worry_about
            return;                                    // rts
        }

        // .worry_about: (pas de and.b #$80 ici, contrairement aux autres — conservé)
        int a2 = a1;                                   // move.l a1,a2
        Mem.ww(a0 + 4, worryHeight(a0, a2));           // ... ; move.w d0,4(a0)

        ACTANIMOBJ(a0, a2);                            // bsr ACTANIMOBJ
        // rts
    }

    /** StillHere — l'objet destructible est encore vivant ; CHUTE dans Decoration comme l'original. */
    private static void StillHere(int a0, int a1) {
        int d0 = Mem.w(a0 + ObjT_ZoneID_w);            // move.w ObjT_ZoneID_w(a0),d0
        if (d0 < 0) {                                  // bge.s .ok_in_room
            return;                                    // rts
        }

        // .ok_in_room:
        Mem.wb(a0 + EntT_HitPoints_b, 1);              // move.b #1,EntT_HitPoints_b(a0)
        if (Mem.b(AI_NoEnemies_b) != 0) {              // tst.b AI_NoEnemies_b ; beq.s .no_locks
            int d1 = Mem.l(a0 + EntT_DoorsAndLiftsHeld_l); // move.l EntT_DoorsAndLiftsHeld_l(a0),d1
            Mem.wl(Anim_DoorAndLiftLocks_l, Mem.l(Anim_DoorAndLiftLocks_l) | d1); // or.l d1,Anim_DoorAndLiftLocks_l
        }

        // .no_locks:
        if (Mem.b(a0 + ShotT_Worry_b) == 0) {          // tst.b ShotT_Worry_b(a0) ; bne.s .worry_about
            return;                                    // rts
        }

        // .worry_about:
        // SAVEREGS
        int d2 = Mem.uw(a0 + ObjT_ZoneID_w);           // move.w ObjT_ZoneID_w(a0),d2
        int a5 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a5
        d0 = Mem.l(a5 + ((short) d2) * 4);             // move.l (a5,d2.w*4),d0
        Mem.wl(Obj_ZonePtr_l, d0);                     // move.l d0,Obj_ZonePtr_l
        d0 = Mem.uw(a0);                               // move.w (a0),d0
        int pa1 = Mem.l(Lvl_ObjectPointsPtr_l);        // move.l Lvl_ObjectPointsPtr_l,a1
        Mem.ww(newx, Mem.uw(pa1 + ((short) d0) * 8));  // move.w (a1,d0.w*8),newx
        Mem.ww(newz, Mem.uw(pa1 + ((short) d0) * 8 + 4)); // move.w 4(a1,d0.w*8),newz
        Ai.AI_LookForPlayer1(a0);                      // jsr AI_LookForPlayer1
        // GETREGS
        Decoration(a0, a1);                            // (chute dans Decoration)
    }

    /** Decoration */
    private static void Decoration(int a0, int a1) {
        int d0 = Mem.w(a0 + ObjT_ZoneID_w);            // move.w ObjT_ZoneID_w(a0),d0
        if (d0 < 0) {                                  // bge.s .ok_in_room
            return;                                    // rts
        }

        // .ok_in_room:
        if (Mem.b(a0 + ShotT_Worry_b) == 0) {          // tst.b ShotT_Worry_b(a0) ; bne.s .worry_about
            return;                                    // rts
        }

        // .worry_about: / intodeco:
        int a2 = a1;                                   // move.l a1,a2
        Mem.ww(a0 + ObjT_ZPos_l, worryHeight(a0, a2)); // ... ; move.w d0,ObjT_ZPos_l(a0)
        DEFANIMOBJ(a0, a2);                            // bsr DEFANIMOBJ
        // rts
    }

    /**
     * obj_SetInventoryPointers — a0 = objet ; pose obj_ConsumablePtr_l (a1)
     * et obj_ItemsPtr_l (a2).
     */
    private static void obj_SetInventoryPointers(int a0) {
        int a2 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a2
        int a1 = a2 + GLFT_AmmoGive_l;                 // lea GLFT_AmmoGive_l(a2),a1
        a2 += GLFT_GunGive_l;                          // add.l #GLFT_GunGive_l,a2
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        int d1 = setw(0, d0);                          // move.w d0,d1
        d0 = muls(d0, AmmoGiveLen);                    // muls #AmmoGiveLen,d0
        d1 = muls(d1, GunGiveLen);                     // muls #GunGiveLen,d1
        a2 += (short) d1;                              // add.w d1,a2
        a1 += (short) d0;                              // add.w d0,a1

        // Save recalculating these later...
        Mem.wl(obj_ConsumablePtr_l, a1);               // move.l a1,obj_ConsumablePtr_l
        Mem.wl(obj_ItemsPtr_l, a2);                    // move.l a2,obj_ItemsPtr_l
        // rts
    }

    /** Plr1_CollectItem — a0 = objet ; renvoie d0 (1 = ramassé). */
    public static int Plr1_CollectItem(int a0) {
        // move.l a0,a3 ; back up the object pointer (a0 local en Java)
        obj_SetInventoryPointers(a0);                  // bsr obj_SetInventoryPointers

        // If the item is a lock, it should always be collectable.
        if (Mem.l(a0 + EntT_DoorsAndLiftsHeld_l) == 0) { // tst.l EntT_DoorsAndLiftsHeld_l(a0) ; bne.s .can_collect
            // Perform the inventory checks
            int d0 = GameC.Game_CheckInventoryLimits(Plr1_Invetory_vw,
                    Mem.l(obj_ConsumablePtr_l), Mem.l(obj_ItemsPtr_l)); // lea Plr1_Invetory_vw,a0 ; CALLC ...
            if ((short) d0 == 0) {                     // tst.w d0 ; bne.s .can_collect
                // don't show the "cant collect" in multiplayer
                if ((byte) Mem.b(Plr_MultiplayerType_b) == Hires.PLR_SINGLE) { // cmp.b #PLR_SINGLE,... ; bne.s .skip_no_collect_quiet
                    if (Mem.w(a0 + EntT_Timer2_w) <= 0) { // tst.w EntT_Timer2_w(a0) ; bgt .skip_no_collect_message
                        Message.Msg_PushLineDedupLast(Game_CantCollectItemText_vb,
                                LVLT_MESSAGE_LENGTH | MSG_TAG_NARRATIVE); // lea ...,a0 ; CALLC Msg_PushLineDedupLast
                        Mem.ww(a0 + EntT_Timer2_w, 200); // move.w #200,EntT_Timer2_w(a0)
                    }
                    // .skip_no_collect_message:
                    Mem.ww(a0 + EntT_Timer2_w, Mem.uw(a0 + EntT_Timer2_w) - 1); // sub.w #1,EntT_Timer2_w(a0)
                }
                // .skip_no_collect_quiet:
                return 0;                              // moveq #0,d0 ; rts
            }
        }

        // .can_collect:
        if ((byte) Mem.b(Plr_MultiplayerType_b) == Hires.PLR_SINGLE) { // cmp.b #PLR_SINGLE,... ; bne.s .nodeftext
            int d0 = Mem.w(a0 + EntT_DisplayText_w);   // move.w EntT_DisplayText_w(a0),d0
            if (d0 >= 0) {                             // blt.s .notext
                d0 = muls(d0, LVLT_MESSAGE_LENGTH);    // muls #LVLT_MESSAGE_LENGTH,d0
                d0 += Mem.l(Lvl_DataPtr_l);            // add.l Lvl_DataPtr_l,d0
                Message.Msg_PushLine(d0, LVLT_MESSAGE_LENGTH | MSG_TAG_NARRATIVE); // CALLC Msg_PushLine
                // bra .nodeftext
            } else {
                // .notext:
                if ((byte) Mem.b(Plr_MultiplayerType_b) != Hires.PLR_SLAVE) { // cmp.b #PLR_SLAVE,... ; beq.s .nodeftext
                    d0 = 0;                            // moveq #0,d0
                    d0 = setb(d0, Mem.ub(a0 + EntT_Type_b)); // move.b EntT_Type_b(a0),d0
                    d0 = muls(d0, GLFT_OBJ_NAME_LENGTH); // muls #GLFT_OBJ_NAME_LENGTH,d0
                    int na0 = Mem.l(HiresData.GLF_DatabasePtr_l); // move.l GLF_DatabasePtr_l,a0
                    d0 += GLFT_ObjectNames_l;          // add.l #GLFT_ObjectNames_l,d0
                    na0 += d0;                         // add.l d0,a0
                    Message.Msg_PushLine(na0, GLFT_OBJ_NAME_LENGTH | MSG_TAG_DEFAULT); // CALLC Msg_PushLine
                }
            }
        }

        // .nodeftext:
        GameC.Game_AddToInventory(Plr1_Invetory_vw,
                Mem.l(obj_ConsumablePtr_l), Mem.l(obj_ItemsPtr_l)); // lea Plr1_Invetory_vw,a0 ; CALLC Game_AddToInventory

        int a3 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a3
        a3 += GLFT_ObjectDefs;                         // add.l #GLFT_ObjectDefs,a3
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        d0 = muls(d0, ODefT_SizeOf_l);                 // muls #ODefT_SizeOf_l,d0
        a3 += d0;                                      // add.l d0,a3
        d0 = Mem.w(a3 + ODefT_SFX_w);                  // move.w ODefT_SFX_w(a3),d0
        if (d0 >= 0) {                                 // blt.s .nosoundmake
            // SAVEREGS
            Mem.ww(HiresData.Aud_SampleNum_w, d0);     // move.w d0,Aud_SampleNum_w
            Mem.wb(HiresData.notifplaying, 0);         // clr.b notifplaying
            Mem.ww(HiresData.IDNUM, Mem.uw(a0));       // move.w (a0),IDNUM
            Mem.ww(HiresData.Aud_NoiseVol_w, 80);      // move.w #80,Aud_NoiseVol_w
            int a1 = ObjRotated_vl;                    // move.l #ObjRotated_vl,a1
            d0 = Mem.uw(a0);                           // move.w (a0),d0
            a1 = a1 + ((short) d0) * 8;                // lea (a1,d0.w*8),a1
            Mem.wl(HiresData.Aud_NoiseX_w, Mem.l(a1)); // move.l (a1),Aud_NoiseX_w
            Hires.MakeSomeNoise();                     // jsr MakeSomeNoise
            // GETREGS
        }

        // .nosoundmake:
        return 1;                                      // moveq #1,d0 ; we collected the item ; rts
    }

    /** Plr2_CollectItem — a0 = objet ; renvoie d0 (1 = ramassé). */
    public static int Plr2_CollectItem(int a0) {
        obj_SetInventoryPointers(a0);                  // bsr obj_SetInventoryPointers

        // If the item is a lock, it should always be collectable.
        if (Mem.l(a0 + EntT_DoorsAndLiftsHeld_l) == 0) { // tst.l EntT_DoorsAndLiftsHeld_l(a0) ; bne.s .can_collect
            int d0 = GameC.Game_CheckInventoryLimits(Plr2_Invetory_vw,
                    Mem.l(obj_ConsumablePtr_l), Mem.l(obj_ItemsPtr_l)); // lea Plr2_Invetory_vw,a0 ; CALLC ...
            if ((short) d0 == 0) {                     // tst.w d0 ; bne.s .can_collect
                return 0;                              // rts (d0 = 0)
            }
        }

        // .can_collect:
        GameC.Game_AddToInventory(Plr2_Invetory_vw,
                Mem.l(obj_ConsumablePtr_l), Mem.l(obj_ItemsPtr_l)); // CALLC Game_AddToInventory

        int a3 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a3
        a3 += GLFT_ObjectDefs;                         // add.l #GLFT_ObjectDefs,a3
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        d0 = muls(d0, ODefT_SizeOf_l);                 // muls #ODefT_SizeOf_l,d0
        a3 += d0;                                      // add.l d0,a3

        d0 = Mem.w(a3 + ODefT_SFX_w);                  // move.w ODefT_SFX_w(a3),d0
        if (d0 >= 0) {                                 // blt.s .nosoundmake
            // SAVEREGS
            Mem.ww(HiresData.Aud_SampleNum_w, d0);     // move.w d0,Aud_SampleNum_w
            Mem.wb(HiresData.notifplaying, 0);         // clr.b notifplaying
            Mem.ww(HiresData.IDNUM, Mem.uw(a0));       // move.w (a0),IDNUM
            Mem.ww(HiresData.Aud_NoiseVol_w, 80);      // move.w #80,Aud_NoiseVol_w
            int a1 = ObjRotated_vl;                    // move.l #ObjRotated_vl,a1
            d0 = Mem.uw(a0);                           // move.w (a0),d0
            a1 = a1 + ((short) d0) * 8;                // lea (a1,d0.w*8),a1
            Mem.wl(HiresData.Aud_NoiseX_w, Mem.l(a1)); // move.l (a1),Aud_NoiseX_w
            Mem.wb(HiresData.PlayEcho, 0);             // move.b #0,PlayEcho
            Hires.MakeSomeNoise();                     // jsr MakeSomeNoise
            // GETREGS
        }

        // .nosoundmake:
        Mem.wb(a0 + ShotT_Worry_b, 0);                 // clr.b ShotT_Worry_b(a0) ; why ?
        return 1;                                      // moveq #1,d0 ; rts
    }

    /** Plr1_CheckObjectCollide — a0 = objet ; renvoie d0 (octet hitwall). */
    public static int Plr1_CheckObjectCollide(int a0) {
        int a2 = Mem.l(obj_StatPtr_l);                 // move.l obj_StatPtr_l,a2
        int d0 = Mem.ub(Plr1_StoodInTop_b);            // move.b Plr1_StoodInTop_b,d0
        int d1 = Mem.ub(a0 + ShotT_InUpperZone_b);     // move.b ShotT_InUpperZone_b(a0),d1
        if (((d0 ^ d1) & 0xFF) != 0) {                 // eor.b d0,d1 ; bne .NotSameZone
            return 0;                                  // .NotSameZone: moveq #0,d0 ; rts
        }

        Mem.ww(oldx, Mem.uw(Plr1_XOff_l));             // move.w Plr1_XOff_l,oldx
        Mem.ww(oldz, Mem.uw(Plr1_ZOff_l));             // move.w Plr1_ZOff_l,oldz
        int d7 = Mem.uw(Plr1_Zone_w);                  // move.w Plr1_Zone_w,d7
        if ((short) d7 != Mem.w(a0 + ObjT_ZoneID_w)) { // cmp.w ObjT_ZoneID_w(a0),d7 ; bne .NotSameZone
            return 0;
        }

        d7 = Mem.l(Plr1_YOff_l);                       // move.l Plr1_YOff_l,d7
        int d6 = Mem.l(Plr1_Height_l);                 // move.l Plr1_Height_l,d6
        d6 >>= 1;                                      // asr.l #1,d6
        d7 += d6;                                      // add.l d6,d7
        d7 >>= 7;                                      // asr.l #7,d7
        d7 = setw(d7, d7 - Mem.uw(a0 + 4));            // sub.w 4(a0),d7
        if ((short) d7 <= 0) {                         // bgt.s .okpos
            d7 = setw(d7, -(short) d7);                // neg.w d7
        }
        // .okpos:
        if ((short) d7 > Mem.w(a2 + ODefT_CollideHeight_w)) { // cmp.w ODefT_CollideHeight_w(a2),d7 ; bgt .NotSameZone
            return 0;
        }

        d0 = Mem.uw(a0);                               // move.w (a0),d0
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        Mem.ww(newx, Mem.uw(a1 + ((short) d0) * 8));   // move.w (a1,d0.w*8),newx
        Mem.ww(newz, Mem.uw(a1 + ((short) d0) * 8 + 4)); // move.w 4(a1,d0.w*8),newz
        int d2 = Mem.uw(a2 + ODefT_CollideRadius_w);   // move.w ODefT_CollideRadius_w(a2),d2
        d2 = muls(d2, d2);                             // muls d2,d2
        Objectmove.CheckHit(d2);                       // jsr CheckHit
        return Mem.ub(hitwall);                        // move.b hitwall,d0 ; rts
    }

    /** Plr2_CheckObjectCollide — a0 = objet ; renvoie d0 (octet hitwall). */
    public static int Plr2_CheckObjectCollide(int a0) {
        int a2 = Mem.l(obj_StatPtr_l);                 // move.l obj_StatPtr_l,a2
        int d0 = Mem.ub(Plr2_StoodInTop_b);            // move.b Plr2_StoodInTop_b,d0
        int d1 = Mem.ub(a0 + ShotT_InUpperZone_b);     // move.b ShotT_InUpperZone_b(a0),d1
        if (((d0 ^ d1) & 0xFF) != 0) {                 // eor.b d0,d1 ; bne .NotSameZone
            return 0;                                  // .NotSameZone: moveq #0,d0 ; rts
        }

        Mem.ww(oldx, Mem.uw(Plr2_XOff_l));             // move.w Plr2_XOff_l,oldx
        Mem.ww(oldz, Mem.uw(Plr2_ZOff_l));             // move.w Plr2_ZOff_l,oldz
        int d7 = Mem.uw(Plr2_Zone_w);                  // move.w Plr2_Zone_w,d7
        if ((short) d7 != Mem.w(a0 + ObjT_ZoneID_w)) { // cmp.w ObjT_ZoneID_w(a0),d7 ; bne .NotSameZone
            return 0;
        }

        d7 = Mem.l(Plr2_YOff_l);                       // move.l Plr2_YOff_l,d7
        int d6 = Mem.l(Plr2_Height_l);                 // move.l Plr2_Height_l,d6
        d6 >>= 1;                                      // asr.l #1,d6
        d7 += d6;                                      // add.l d6,d7
        d7 >>= 7;                                      // asr.l #7,d7
        d7 = setw(d7, d7 - Mem.uw(a0 + 4));            // sub.w 4(a0),d7
        if ((short) d7 <= 0) {                         // bgt.s .okpos
            d7 = setw(d7, -(short) d7);                // neg.w d7
        }
        // .okpos:
        if ((short) d7 > Mem.w(a2 + ODefT_CollideHeight_w)) { // cmp.w ODefT_CollideHeight_w(a2),d7 ; bgt .NotSameZone
            return 0;
        }

        d0 = Mem.uw(a0);                               // move.w (a0),d0
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        Mem.ww(newx, Mem.uw(a1 + ((short) d0) * 8));   // move.w (a1,d0.w*8),newx
        Mem.ww(newz, Mem.uw(a1 + ((short) d0) * 8 + 4)); // move.w 4(a1,d0.w*8),newz
        int d2 = Mem.uw(a2 + ODefT_CollideRadius_w);   // move.w ODefT_CollideRadius_w(a2),d2
        d2 = muls(d2, d2);                             // muls d2,d2
        Objectmove.CheckHit(d2);                       // jsr CheckHit
        return Mem.ub(hitwall);                        // move.b hitwall,d0 ; rts
    }

    /** Corps partagé DEFANIMOBJ/ACTANIMOBJ (record d'anim 6 octets, type GFX bitmap/vector/glare). */
    private static void animObj(int a0, int a2, int animTableOffset) {
        int a3 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a3
        a3 = a3 + animTableOffset;                     // lea GLFT_Object(Def|Act)Anims_l(a3),a3
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a0 + EntT_Type_b));       // move.b EntT_Type_b(a0),d0
        d0 = muls(d0, O_AnimSize);                     // muls #O_AnimSize,d0
        a3 += (short) d0;                              // add.w d0,a3
        d0 = Mem.uw(a0 + EntT_Timer1_w);               // move.w EntT_Timer1_w(a0),d0
        int d1 = setw(0, d0);                          // move.w d0,d1
        d0 = setw(d0, d0 + d0);                        // add.w d0,d0
        d1 = setw(d1, d1 << 2);                        // asl.w #2,d1
        d0 = setw(d0, d0 + d1);                        // add.w d1,d0 ; *6
        int gfx = Mem.w(a2 + ODefT_GFXType_w);         // cmp.w #1,ODefT_GFXType_w(a2)
        if (gfx < 1) {                                 // blt.s .bitmap
            // .bitmap:
            Mem.wl(a0 + 8, 0);                         // move.l #0,8(a0)
            Mem.wb(a0 + 9, Mem.ub(a3 + (short) d0));   // move.b (a3,d0.w),9(a0)
            Mem.wb(a0 + 11, Mem.ub(a3 + (short) d0 + 1)); // move.b 1(a3,d0.w),11(a0)
            Mem.ww(a0 + 6, Mem.uw(a3 + (short) d0 + 2)); // move.w 2(a3,d0.w),6(a0)
            d1 = Mem.b(a3 + (short) d0 + 4);           // move.b 4(a3,d0.w),d1 ; ext.w d1
            d1 = d1 + d1;                              // add.w d1,d1
            Mem.ww(a0 + 4, Mem.uw(a0 + 4) + d1);       // add.w d1,4(a0)
            d1 = Mem.ub(a3 + (short) d0 + 5);          // moveq #0,d1 ; move.b 5(a3,d0.w),d1
            Mem.ww(a0 + EntT_Timer1_w, d1);            // move.w d1,EntT_Timer1_w(a0)
        } else if (gfx == 1) {                         // beq.s .vector
            // .vector:
            Mem.wl(a0 + 8, 0);                         // move.l #0,8(a0)
            Mem.wb(a0 + 9, Mem.ub(a3 + (short) d0));   // move.b (a3,d0.w),9(a0)
            Mem.wb(a0 + 11, Mem.ub(a3 + (short) d0 + 1)); // move.b 1(a3,d0.w),11(a0)
            Mem.ww(a0 + 6, 0xffff);                    // move.w #$ffff,6(a0)
            d1 = Mem.b(a3 + (short) d0 + 4);           // move.b 4(a3,d0.w),d1 ; ext.w d1
            d1 = d1 + d1;                              // add.w d1,d1
            Mem.ww(a0 + 4, Mem.uw(a0 + 4) + d1);       // add.w d1,4(a0)
            d1 = Mem.uw(a3 + (short) d0 + 2);          // move.w 2(a3,d0.w),d1
            Mem.ww(a0 + EntT_CurrentAngle_w, Mem.uw(a0 + EntT_CurrentAngle_w) + d1); // add.w d1,EntT_CurrentAngle_w(a0)
            d1 = Mem.ub(a3 + (short) d0 + 5);          // moveq #0,d1 ; move.b 5(a3,d0.w),d1
            Mem.ww(a0 + EntT_Timer1_w, d1);            // move.w d1,EntT_Timer1_w(a0)
        } else {
            // .glare:
            Mem.wl(a0 + 8, 0);                         // move.l #0,8(a0)
            d1 = Mem.b(a3 + (short) d0);               // move.b (a3,d0.w),d1 ; ext.w d1
            d1 = -d1;                                  // neg.w d1
            Mem.ww(a0 + 8, d1);                        // move.w d1,8(a0)
            Mem.wb(a0 + 11, Mem.ub(a3 + (short) d0 + 1)); // move.b 1(a3,d0.w),11(a0)
            Mem.ww(a0 + 6, Mem.uw(a3 + (short) d0 + 2)); // move.w 2(a3,d0.w),6(a0)
            d1 = Mem.b(a3 + (short) d0 + 4);           // move.b 4(a3,d0.w),d1 ; ext.w d1
            d1 = d1 + d1;                              // add.w d1,d1
            Mem.ww(a0 + 4, Mem.uw(a0 + 4) + d1);       // add.w d1,4(a0)
            d1 = Mem.ub(a3 + (short) d0 + 5);          // moveq #0,d1 ; move.b 5(a3,d0.w),d1
            Mem.ww(a0 + EntT_Timer1_w, d1);            // move.w d1,EntT_Timer1_w(a0)
        }
        // rts
    }

    /** DEFANIMOBJ — animation par défaut (GLFT_ObjectDefAnims_l). */
    public static void DEFANIMOBJ(int a0, int a2) {
        animObj(a0, a2, GLFT_ObjectDefAnims_l);
    }

    /** ACTANIMOBJ — animation active (GLFT_ObjectActAnims_l). */
    public static void ACTANIMOBJ(int a0, int a2) {
        animObj(a0, a2, GLFT_ObjectActAnims_l);
    }

    /** ViewpointToDraw — a0 = entité ; renvoie d0 = 0 face / 1 droite / 2 dos / 3 gauche. */
    public static int ViewpointToDraw(int a0) {
        int d3 = Mem.uw(a0 + EntT_CurrentAngle_w);     // move.w EntT_CurrentAngle_w(a0),d3
        d3 = setw(d3, d3 - Mem.uw(HiresData.Vis_AngPos_w)); // sub.w Vis_AngPos_w,d3
        d3 = TablesData.AMOD_A(d3);                    // AMOD_A d3
        int a2 = SinCosTable_vw;                       // move.l #SinCosTable_vw,a2
        int d2 = Mem.w(a2 + (short) d3);               // move.w (a2,d3.w),d2
        a2 += COSINE_OFS;                              // adda.w #COSINE_OFS,a2
        d3 = Mem.w(a2 + (short) d3);                   // move.w (a2,d3.w),d3
        // ext.l d2 ; ext.l d3 (déjà signés)
        int d0 = d3;                                   // move.l d3,d0
        int d4 = d2;                                   // move.l d2,d4
        d0 = -d0;                                      // neg.l d0
        if (d0 > 0) {                                  // tst.l d0 ; bgt.s FacingTowardsPlayer
            // FacingTowardsPlayer:
            if (d4 <= 0) {                             // tst.l d4 ; bgt.s FTPR
                d4 = -d4;                              // neg.l d4
                if (d4 > d0) {                         // cmp.l d0,d4 ; bgt.s LEFTFRAME
                    return 3;                          // LEFTFRAME: move.l #3,d0 ; rts
                }
                return 0;                              // bra.s TOWARDSFRAME
            }
            // FTPR:
            if (d4 > d0) {                             // cmp.l d0,d4 ; bgt.s RIGHTFRAME
                return 1;                              // RIGHTFRAME: move.l #1,d0 ; rts
            }
            return 0;                                  // TOWARDSFRAME: move.l #0,d0 ; rts
        }
        // FAP:
        if (d4 <= 0) {                                 // tst.l d4 ; bgt.s FAPR
            if (d0 > d4) {                             // cmp.l d4,d0 ; bgt.s LEFTFRAME
                return 3;                              // LEFTFRAME
            }
            return 2;                                  // bra.s AWAYFRAME
        }
        // FAPR:
        d0 = -d0;                                      // neg.l d0
        if (d4 > d0) {                                 // cmp.l d0,d4 ; bgt.s RIGHTFRAME
            return 1;                                  // RIGHTFRAME
        }
        return 2;                                      // AWAYFRAME: move.l #2,d0 ; rts
    }

    /** RunAround — a0 = entité ; décale (newx,newz) latéralement. */
    public static void RunAround(int a0) {
        // movem.l d0/d1/d2/d3/a0/a1,-(a7) — locaux en Java
        int d0 = Mem.uw(oldx);                         // move.w oldx,d0
        d0 = setw(d0, d0 - Mem.uw(newx));              // sub.w newx,d0 ; dx
        d0 = setw(d0, ((short) d0) >> 1);              // asr.w #1,d0
        int d1 = Mem.uw(oldz);                         // move.w oldz,d1
        d1 = setw(d1, d1 - Mem.uw(newz));              // sub.w newz,d1 ; dz
        d1 = setw(d1, ((short) d1) >> 1);              // asr.w #1,d1
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        int d2 = Mem.uw(a0);                           // move.w (a0),d2
        a1 = a1 + ((short) d2) * 8;                    // lea (a1,d2.w*8),a1
        d2 = Mem.uw(a1);                               // move.w (a1),d2
        d2 = setw(d2, d2 - Mem.uw(tempx));             // sub.w tempx,d2
        int d3 = Mem.uw(a1 + 4);                       // move.w 4(a1),d3
        d3 = setw(d3, d3 - Mem.uw(tempz));             // sub.w tempz,d3
        d2 = muls(d2, Mem.w(tempcos));                 // muls tempcos,d2
        d3 = muls(d3, Mem.w(tempsin));                 // muls tempsin,d3
        d2 -= d3;                                      // sub.l d3,d2
        if (d2 >= 0) {                                 // blt.s headleft
            d0 = setw(d0, -(short) d0);                // neg.w d0
            d1 = setw(d1, -(short) d1);                // neg.w d1
        }
        // headleft:
        Mem.ww(newx, Mem.uw(newx) - d1);               // sub.w d1,newx
        Mem.ww(newz, Mem.uw(newz) + d0);               // add.w d0,newz
        // movem.l (a7)+,... ; rts
    }

    /** SHOOTPLAYER1 — a0 = entité, a1 = entrée du point objet (tireur). */
    public static void SHOOTPLAYER1(int a0, int a1) {
        Mem.ww(tsx, Mem.uw(oldx));                     // move.w oldx,tsx
        Mem.ww(tsz, Mem.uw(oldz));                     // move.w oldz,tsz
        Mem.ww(fsx, Mem.uw(newx));                     // move.w newx,fsx
        Mem.ww(fsz, Mem.uw(newz));                     // move.w newz,fsz
        Mem.ww(newx, Mem.uw(Plr1_TmpXOff_l));          // move.w Plr1_TmpXOff_l,newx
        Mem.ww(newz, Mem.uw(Plr1_TmpZOff_l));          // move.w Plr1_TmpZOff_l,newz
        Mem.ww(oldx, Mem.uw(a1));                      // move.w (a1),oldx
        Mem.ww(oldz, Mem.uw(a1 + 4));                  // move.w 4(a1),oldz
        int d1 = Mem.uw(newx);                         // move.w newx,d1
        d1 = setw(d1, d1 - Mem.uw(oldx));              // sub.w oldx,d1
        int d2 = Mem.uw(newz);                         // move.w newz,d2
        d2 = setw(d2, d2 - Mem.uw(oldz));              // sub.w oldz,d2
        int d0 = Objectmove.GetRand();                 // jsr GetRand

        d0 = setw(d0, ((short) d0) >> 4);              // asr.w #4,d0 (dispersion)
        d1 = muls(d1, d0);                             // muls d0,d1
        d2 = muls(d2, d0);                             // muls d0,d2
        d1 = swap(d1);                                 // swap d1
        d2 = swap(d2);                                 // swap d2
        Mem.ww(newz, Mem.uw(newz) + d1);               // add.w d1,newz
        Mem.ww(newx, Mem.uw(newx) - d2);               // sub.w d2,newx
        d1 = Mem.l(Plr1_TmpYOff_l);                    // move.l Plr1_TmpYOff_l,d1
        d1 += 15 * 128;                                // add.l #15*128,d1
        d1 >>= 7;                                      // asr.l #7,d1
        d2 = setw(0, d1);                              // move.w d1,d2
        d2 = muls(d2, d0);                             // muls d0,d2
        d2 = swap(d2);                                 // swap d2
        d1 = setw(d1, d1 + d2);                        // add.w d2,d1
        d1 = (short) d1;                               // ext.l d1
        d1 <<= 7;                                      // asl.l #7,d1
        Mem.wl(newy, d1);                              // move.l d1,newy
        d1 = Mem.w(a0 + 4);                            // move.w 4(a0),d1 ; ext.l d1
        d1 <<= 7;                                      // asl.l #7,d1
        Mem.wl(oldy, d1);                              // move.l d1,oldy
        Mem.wb(StoodInTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a0),StoodInTop
        Mem.wb(exitfirst, 0xFF);                       // st exitfirst
        Mem.ww(Obj_ExtLen_w, 0);                       // move.w #0,Obj_ExtLen_w
        Mem.wb(Obj_AwayFromWall_b, 0xFF);              // move.b #$ff,Obj_AwayFromWall_b
        Mem.ww(wallflags, 0b0000010000000000);         // move.w #%0000010000000000,wallflags
        Mem.wl(StepUpVal, 0);                          // move.l #0,StepUpVal
        Mem.wl(StepDownVal, 0x1000000);                // move.l #$1000000,StepDownVal
        Mem.wl(ObjectmoveData.thingheight, 0);         // move.l #0,thingheight
        int savedZone = Mem.l(Obj_ZonePtr_l);          // move.l Obj_ZonePtr_l,-(a7)

        // SAVEREGS
        while (true) { // .again:
            Objectmove.MoveObject();                   // jsr MoveObject
            if (Mem.b(hitwall) != 0) {                 // tst.b hitwall ; bne.s .nofurther
                break;
            }
            int dd = Mem.uw(newx);                     // move.w newx,d0
            dd = setw(dd, dd - Mem.uw(oldx));          // sub.w oldx,d0
            Mem.ww(oldx, Mem.uw(oldx) + dd);           // add.w d0,oldx
            Mem.ww(newx, Mem.uw(newx) + dd);           // add.w d0,newx
            dd = Mem.uw(newz);                         // move.w newz,d0
            dd = setw(dd, dd - Mem.uw(oldz));          // sub.w oldz,d0
            Mem.ww(oldz, Mem.uw(oldz) + dd);           // add.w d0,oldz
            Mem.ww(newz, Mem.uw(newz) + dd);           // add.w d0,newz
            int dl = Mem.l(newy);                      // move.l newy,d0
            dl -= Mem.l(oldy);                         // sub.l oldy,d0
            Mem.wl(oldy, Mem.l(oldy) + dl);            // add.l d0,oldy
            Mem.wl(newy, Mem.l(newy) + dl);            // add.l d0,newy
        }                                              // bra .again

        // .nofurther:
        Mem.wl(backupZonePtr_l, Mem.l(Obj_ZonePtr_l)); // move.l Obj_ZonePtr_l,backupZonePtr_l
        // GETREGS
        Mem.wl(Obj_ZonePtr_l, savedZone);              // move.l (a7)+,Obj_ZonePtr_l
        int sa0 = Mem.l(Plr_ShotDataPtr_l);            // move.l Plr_ShotDataPtr_l,a0
        d1 = NUM_PLR_SHOT_DATA - 1;                    // move.w #NUM_PLR_SHOT_DATA-1,d1

        do { // .findonefree2:
            if (Mem.w(sa0 + ObjT_ZoneID_w) < 0) {      // move.w ObjT_ZoneID_w(a0),d2 ; blt.s .foundonefree2
                // .foundonefree2:
                int pa1 = Mem.l(Lvl_ObjectPointsPtr_l); // move.l Lvl_ObjectPointsPtr_l,a1
                d2 = Mem.uw(sa0);                      // move.w (a0),d2
                Mem.ww(pa1 + ((short) d2) * 8, Mem.uw(newx));     // move.w newx,(a1,d2.w*8)
                Mem.ww(pa1 + ((short) d2) * 8 + 4, Mem.uw(newz)); // move.w newz,4(a1,d2.w*8)
                Mem.wb(sa0 + ShotT_Status_b, 1);       // move.b #1,ShotT_Status_b(a0)
                Mem.ww(sa0 + ShotT_Gravity_w, 0);      // move.w #0,ShotT_Gravity_w(a0)
                Mem.wb(sa0 + ShotT_Size_b, 0);         // move.b #0,ShotT_Size_b(a0)
                Mem.wb(sa0 + ShotT_Anim_b, 0);         // move.b #0,ShotT_Anim_b(a0)
                int ba1 = Mem.l(backupZonePtr_l);      // move.l backupZonePtr_l,a1
                Mem.ww(sa0 + ObjT_ZoneID_w, Mem.uw(ba1)); // move.w (a1),ObjT_ZoneID_w(a0)
                Mem.wb(sa0 + ShotT_Worry_b, 0xFF);     // st ShotT_Worry_b(a0)
                int dwh = Mem.l(wallhitheight);        // move.l wallhitheight,d0
                Mem.wl(sa0 + ShotT_AccYPos_w, dwh);    // move.l d0,ShotT_AccYPos_w(a0)
                dwh >>= 7;                             // asr.l #7,d0
                Mem.ww(sa0 + 4, dwh);                  // move.w d0,4(a0)
                break;
            }
            sa0 += ObjT_SizeOf_l;                      // NEXT_OBJ a0
            d1 = setw(d1, d1 - 1);                     // dbra d1,.findonefree2
        } while ((short) d1 != -1);

        Mem.ww(oldx, Mem.uw(tsx));                     // move.w tsx,oldx
        Mem.ww(oldz, Mem.uw(tsz));                     // move.w tsz,oldz
        Mem.ww(newx, Mem.uw(fsx));                     // move.w fsx,newx
        Mem.ww(newz, Mem.uw(fsz));                     // move.w fsz,newz
        // rts
    }

    /** FireAtPlayer1 — a0 = entité tireuse. */
    public static void FireAtPlayer1(int a0) {
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        int d1 = Mem.uw(a0);                           // move.w (a0),d1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1
        int a5 = Mem.l(AI_AlienShotDataPtr_l);         // move.l AI_AlienShotDataPtr_l,a5
        d1 = NUM_ALIEN_SHOT_DATA - 1;                  // move.w #NUM_ALIEN_SHOT_DATA-1,d1

        while (true) { // .findonefree:
            if (Mem.w(a5 + ObjT_ZoneID_w) < 0) {       // move.w ObjT_ZoneID_w(a5),d0 ; blt.s .foundonefree
                break;
            }
            a5 += ObjT_SizeOf_l;                       // NEXT_OBJ a5
            d1 = setw(d1, d1 - 1);                     // dbra d1,.findonefree
            if ((short) d1 == -1) {
                return;                                // bra .cantshoot ; rts
            }
        }

        // .foundonefree:
        Mem.wb(a5 + ObjT_TypeID_b, OBJ_TYPE_PROJECTILE); // move.b #OBJ_TYPE_PROJECTILE,ObjT_TypeID_b(a5)
        int a6 = ObjRotated_vl;                        // move.l #ObjRotated_vl,a6
        int d0 = Mem.uw(a0);                           // move.w (a0),d0
        a6 = a6 + ((short) d0) * 8;                    // lea (a6,d0.w*8),a6
        Mem.wl(HiresData.Aud_NoiseX_w, Mem.l(a6));     // move.l (a6),Aud_NoiseX_w
        Mem.ww(HiresData.Aud_NoiseVol_w, 100);         // move.w #100,Aud_NoiseVol_w
        Mem.wb(HiresData.Aud_ChannelPick_b, 1);        // move.b #1,Aud_ChannelPick_b
        Mem.wb(HiresData.notifplaying, 0);             // clr.b notifplaying
        d0 = Mem.ub(SHOTTYPE);                         // move.b SHOTTYPE,d0
        Mem.ww(a5 + ShotT_Lifetime_w, 0);              // move.w #0,ShotT_Lifetime_w(a5)
        Mem.wb(a5 + ShotT_Size_b, d0);                 // move.b d0,ShotT_Size_b(a5)
        Mem.wb(HiresData.PlayEcho, Mem.ub(ALIENECHO)); // move.b ALIENECHO,PlayEcho
        Mem.wb(a5 + ShotT_Power_w, Mem.ub(SHOTPOWER)); // move.b SHOTPOWER,ShotT_Power_w(a5)
        // movem.l a5/a1/a0,-(a7)
        Mem.ww(HiresData.IDNUM, Mem.uw(a0));           // move.w (a0),IDNUM
        Hires.MakeSomeNoise();                         // jsr MakeSomeNoise
        // movem.l (a7)+,a5/a1/a0

        int a2 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a2
        d1 = Mem.uw(a5);                               // move.w (a5),d1
        a2 = a2 + ((short) d1) * 8;                    // lea (a2,d1.w*8),a2
        Mem.ww(oldx, Mem.uw(a1));                      // move.w (a1),oldx
        Mem.ww(oldz, Mem.uw(a1 + 4));                  // move.w 4(a1),oldz
        Mem.ww(newx, Mem.uw(Plr1_XOff_l));             // move.w Plr1_XOff_l,newx
        Mem.ww(newz, Mem.uw(Plr1_ZOff_l));             // move.w Plr1_ZOff_l,newz

        Objectmove.CalcDist();                         // jsr CalcDist

        // Anticipation : vise la position future du joueur
        int d6 = Mem.uw(HiresData.XDiff_w);            // move.w XDiff_w,d6
        d6 = muls(d6, Mem.w(distaway));                // muls distaway,d6
        d6 = divs(d6, Mem.w(SHOTSPEED));               // divs SHOTSPEED,d6
        d6 = setw(d6, ((short) d6) >> 4);              // asr.w #4,d6
        Mem.ww(newx, Mem.uw(newx) + d6);               // add.w d6,newx
        d6 = Mem.uw(HiresData.ZDiff_w);                // move.w ZDiff_w,d6
        d6 = muls(d6, Mem.w(distaway));                // muls distaway,d6
        d6 = divs(d6, Mem.w(SHOTSPEED));               // divs SHOTSPEED,d6
        d6 = setw(d6, ((short) d6) >> 4);              // asr.w #4,d6
        Mem.ww(newz, Mem.uw(newz) + d6);               // add.w d6,newz
        Mem.ww(futurex, Mem.uw(newx));                 // move.w newx,futurex
        Mem.ww(futurez, Mem.uw(newz));                 // move.w newz,futurez

        Mem.ww(speed, Mem.uw(SHOTSPEED));              // move.w SHOTSPEED,speed
        Mem.ww(Range, 0);                              // move.w #0,Range
        Objectmove.HeadTowards();                      // jsr HeadTowards

        d0 = Mem.uw(newx);                             // move.w newx,d0
        d0 = setw(d0, d0 - Mem.uw(oldx));              // sub.w oldx,d0
        d1 = Mem.uw(newz);                             // move.w newz,d1
        d1 = setw(d1, d1 - Mem.uw(oldz));              // sub.w oldz,d1
        int d2 = Mem.uw(SHOTOFFMULT);                  // move.w SHOTOFFMULT,d2
        if ((short) d2 != 0) {                         // beq.s .nooffset
            d0 = muls(d0, d2);                         // muls d2,d0
            d1 = muls(d1, d2);                         // muls d2,d1
            d0 >>= 8;                                  // asr.l #8,d0
            d1 >>= 8;                                  // asr.l #8,d1
            Mem.ww(oldx, Mem.uw(oldx) + d1);           // add.w d1,oldx
            Mem.ww(oldz, Mem.uw(oldz) - d0);           // sub.w d0,oldz
            Mem.ww(newx, Mem.uw(futurex));             // move.w futurex,newx
            Mem.ww(newz, Mem.uw(futurez));             // move.w futurez,newz
            Objectmove.HeadTowards();                  // jsr HeadTowards
        }

        // .nooffset:
        d0 = Mem.uw(newx);                             // move.w newx,d0
        Mem.ww(a2, d0);                                // move.w d0,(a2)
        d0 = setw(d0, d0 - Mem.uw(oldx));              // sub.w oldx,d0
        Mem.ww(a5 + ShotT_VelocityX_w, d0);            // move.w d0,ShotT_VelocityX_w(a5)
        d0 = Mem.uw(newz);                             // move.w newz,d0
        Mem.ww(a2 + 4, d0);                            // move.w d0,4(a2)
        d0 = setw(d0, d0 - Mem.uw(oldz));              // sub.w oldz,d0
        Mem.ww(a5 + ShotT_VelocityZ_w, d0);            // move.w d0,ShotT_VelocityZ_w(a5)

        Mem.wl(a5 + EntT_EnemyFlags_l, 0b110010);      // move.l #%110010,EntT_EnemyFlags_l(a5)
        Mem.ww(a5 + ObjT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),ObjT_ZoneID_w(a5)
        d0 = Mem.uw(a0 + 4);                           // move.w 4(a0),d0
        Mem.ww(a5 + 4, d0);                            // move.w d0,4(a5)
        d0 = (short) d0;                               // ext.l d0
        d0 <<= 7;                                      // asl.l #7,d0
        d0 += Mem.l(SHOTYOFF);                         // add.l SHOTYOFF,d0
        Mem.wl(a5 + ShotT_AccYPos_w, d0);              // move.l d0,ShotT_AccYPos_w(a5)
        Mem.wb(a5 + ShotT_InUpperZone_b, Mem.ub(SHOTINTOP)); // move.b SHOTINTOP,ShotT_InUpperZone_b(a5)
        a2 = Mem.l(Plr1_ObjectPtr_l);                  // move.l Plr1_ObjectPtr_l,a2
        d1 = Mem.uw(a2 + 4);                           // move.w 4(a2),d1
        d1 = setw(d1, d1 - 20);                        // sub.w #20,d1
        d1 = (short) d1;                               // ext.l d1
        d1 <<= 7;                                      // asl.l #7,d1
        d1 -= d0;                                      // sub.l d0,d1
        d1 += d1;                                      // add.l d1,d1
        d0 = Mem.uw(distaway);                         // move.w distaway,d0
        d2 = Mem.uw(SHOTSHIFT);                        // move.w SHOTSHIFT,d2
        d0 = setw(d0, ((short) d0) >> (d2 & 63));      // asr.w d2,d0
        if ((short) d0 <= 0) {                         // tst.w d0 ; bgt.s .okokokok
            d0 = 1;                                    // moveq #1,d0
        }
        // .okokokok:
        d1 = divs(d1, d0);                             // divs d0,d1
        Mem.ww(a5 + ShotT_VelocityY_w, d1);            // move.w d1,ShotT_VelocityY_w(a5)
        Mem.wb(a5 + ShotT_Worry_b, 0xFF);              // st ShotT_Worry_b(a5)
        // [bloc Plr_GunDataPtr_l commenté dans l'original — FIXME Enforcer hits]
        // .cantshoot: rts
    }

    /** SHOOTPLAYER2 — a0 = entité, a1 = entrée du point objet (NB original : fsz reçoit oldx, et la restauration écrit fsz dans oldx). */
    public static void SHOOTPLAYER2(int a0, int a1) {
        Mem.ww(tsx, Mem.uw(oldx));                     // move.w oldx,tsx
        Mem.ww(tsz, Mem.uw(oldz));                     // move.w oldz,tsz
        Mem.ww(fsx, Mem.uw(newx));                     // move.w newx,fsx
        Mem.ww(fsz, Mem.uw(oldx));                     // move.w oldx,fsz (coquille de l'original conservée)
        Mem.ww(newx, Mem.uw(Plr2_TmpXOff_l));          // move.w Plr2_TmpXOff_l,newx
        Mem.ww(newz, Mem.uw(Plr2_TmpZOff_l));          // move.w Plr2_TmpZOff_l,newz
        Mem.ww(oldx, Mem.uw(a1));                      // move.w (a1),oldx
        Mem.ww(oldz, Mem.uw(a1 + 4));                  // move.w 4(a1),oldz
        int d1 = Mem.uw(newx);                         // move.w newx,d1
        d1 = setw(d1, d1 - Mem.uw(oldx));              // sub.w oldx,d1
        int d2 = Mem.uw(newz);                         // move.w newz,d2
        d2 = setw(d2, d2 - Mem.uw(oldz));              // sub.w oldz,d2
        int d0 = Objectmove.GetRand();                 // jsr GetRand

        d0 = setw(d0, ((short) d0) >> 4);              // asr.w #4,d0
        d1 = muls(d1, d0);                             // muls d0,d1
        d2 = muls(d2, d0);                             // muls d0,d2
        d1 = swap(d1);                                 // swap d1
        d2 = swap(d2);                                 // swap d2
        Mem.ww(newz, Mem.uw(newz) + d1);               // add.w d1,newz
        Mem.ww(newx, Mem.uw(newx) - d2);               // sub.w d2,newx
        d1 = Mem.l(Plr2_TmpYOff_l);                    // move.l Plr2_TmpYOff_l,d1
        d1 += 15 * 128;                                // add.l #15*128,d1
        d1 >>= 7;                                      // asr.l #7,d1
        d2 = setw(0, d1);                              // move.w d1,d2
        d2 = muls(d2, d0);                             // muls d0,d2
        d2 = swap(d2);                                 // swap d2
        d1 = setw(d1, d1 + d2);                        // add.w d2,d1
        d1 = (short) d1;                               // ext.l d1
        d1 <<= 7;                                      // asl.l #7,d1
        Mem.wl(newy, d1);                              // move.l d1,newy
        d1 = Mem.w(a0 + 4);                            // move.w 4(a0),d1 ; ext.l d1
        d1 <<= 7;                                      // asl.l #7,d1
        Mem.wl(oldy, d1);                              // move.l d1,oldy
        Mem.wb(StoodInTop, Mem.ub(a0 + ShotT_InUpperZone_b)); // move.b ShotT_InUpperZone_b(a0),StoodInTop

        Mem.wb(exitfirst, 0xFF);                       // st exitfirst
        Mem.ww(Obj_ExtLen_w, 0);                       // move.w #0,Obj_ExtLen_w
        Mem.wb(Obj_AwayFromWall_b, 0xFF);              // move.b #$ff,Obj_AwayFromWall_b
        Mem.ww(wallflags, 0b0000010000000000);         // move.w #%0000010000000000,wallflags
        Mem.wl(StepUpVal, 0);                          // move.l #0,StepUpVal
        Mem.wl(StepDownVal, 0x1000000);                // move.l #$1000000,StepDownVal
        Mem.wl(ObjectmoveData.thingheight, 0);         // move.l #0,thingheight
        int savedZone = Mem.l(Obj_ZonePtr_l);          // move.l Obj_ZonePtr_l,-(a7)

        // SAVEREGS
        while (true) { // .again:
            Objectmove.MoveObject();                   // jsr MoveObject
            if (Mem.b(hitwall) != 0) {                 // tst.b hitwall ; bne.s .nofurther
                break;
            }
            int dd = Mem.uw(newx);                     // move.w newx,d0
            dd = setw(dd, dd - Mem.uw(oldx));          // sub.w oldx,d0
            Mem.ww(oldx, Mem.uw(oldx) + dd);           // add.w d0,oldx
            Mem.ww(newx, Mem.uw(newx) + dd);           // add.w d0,newx
            dd = Mem.uw(newz);                         // move.w newz,d0
            dd = setw(dd, dd - Mem.uw(oldz));          // sub.w oldz,d0
            Mem.ww(oldz, Mem.uw(oldz) + dd);           // add.w d0,oldz
            Mem.ww(newz, Mem.uw(newz) + dd);           // add.w d0,newz
            int dl = Mem.l(newy);                      // move.l newy,d0
            dl -= Mem.l(oldy);                         // sub.l oldy,d0
            Mem.wl(oldy, Mem.l(oldy) + dl);            // add.l d0,oldy
            Mem.wl(newy, Mem.l(newy) + dl);            // add.l d0,newy
        }                                              // bra .again

        // .nofurther:
        Mem.wl(backupZonePtr_l, Mem.l(Obj_ZonePtr_l)); // move.l Obj_ZonePtr_l,backupZonePtr_l
        // GETREGS
        Mem.wl(Obj_ZonePtr_l, savedZone);              // move.l (a7)+,Obj_ZonePtr_l
        int sa0 = Mem.l(AI_AlienShotDataPtr_l);        // move.l AI_AlienShotDataPtr_l,a0
        d1 = NUM_ALIEN_SHOT_DATA - 1;                  // move.w #NUM_ALIEN_SHOT_DATA-1,d1

        do { // .findonefree2:
            if (Mem.w(sa0 + ObjT_ZoneID_w) < 0) {      // move.w ObjT_ZoneID_w(a0),d2 ; blt.s .foundonefree2
                // .foundonefree2:
                int pa1 = Mem.l(Lvl_ObjectPointsPtr_l); // move.l Lvl_ObjectPointsPtr_l,a1
                d2 = Mem.uw(sa0);                      // move.w (a0),d2
                Mem.ww(pa1 + ((short) d2) * 8, Mem.uw(newx));     // move.w newx,(a1,d2.w*8)
                Mem.ww(pa1 + ((short) d2) * 8 + 4, Mem.uw(newz)); // move.w newz,4(a1,d2.w*8)
                Mem.wb(sa0 + ShotT_Status_b, 1);       // move.b #1,ShotT_Status_b(a0)
                Mem.ww(sa0 + ShotT_Gravity_w, 0);      // move.w #0,ShotT_Gravity_w(a0)
                Mem.wb(sa0 + ShotT_Size_b, 0);         // move.b #0,ShotT_Size_b(a0)
                Mem.wb(sa0 + ShotT_Anim_b, 0);         // move.b #0,ShotT_Anim_b(a0)
                int ba1 = Mem.l(backupZonePtr_l);      // move.l backupZonePtr_l,a1
                Mem.ww(sa0 + ObjT_ZoneID_w, Mem.uw(ba1)); // move.w (a1),ObjT_ZoneID_w(a0)
                Mem.wb(sa0 + ShotT_Worry_b, 0xFF);     // st ShotT_Worry_b(a0)
                int dwh = Mem.l(wallhitheight);        // move.l wallhitheight,d0
                Mem.wl(sa0 + ShotT_AccYPos_w, dwh);    // move.l d0,ShotT_AccYPos_w(a0)
                dwh >>= 7;                             // asr.l #7,d0
                Mem.ww(sa0 + 4, dwh);                  // move.w d0,4(a0)
                break;
            }
            sa0 += ObjT_SizeOf_l;                      // NEXT_OBJ a0
            d1 = setw(d1, d1 - 1);                     // dbra d1,.findonefree2
        } while ((short) d1 != -1);

        Mem.ww(oldx, Mem.uw(tsx));                     // move.w tsx,oldx
        Mem.ww(oldz, Mem.uw(tsz));                     // move.w tsz,oldz
        Mem.ww(newx, Mem.uw(fsx));                     // move.w fsx,newx
        Mem.ww(oldx, Mem.uw(fsz));                     // move.w fsz,oldx (coquille de l'original conservée)
        // rts
    }

    /** FireAtPlayer2 — a0 = entité tireuse, a1 = entrée du point objet du tireur. */
    public static void FireAtPlayer2(int a0, int a1) {
        int a5 = Mem.l(AI_AlienShotDataPtr_l);         // move.l AI_AlienShotDataPtr_l,a5
        int d1 = NUM_ALIEN_SHOT_DATA - 1;              // move.w #NUM_ALIEN_SHOT_DATA-1,d1

        while (true) { // .findonefree:
            if (Mem.w(a5 + ObjT_ZoneID_w) < 0) {       // move.w ObjT_ZoneID_w(a5),d0 ; blt.s .foundonefree
                break;
            }
            a5 += ObjT_SizeOf_l;                       // NEXT_OBJ a5
            d1 = setw(d1, d1 - 1);                     // dbra d1,.findonefree
            if ((short) d1 == -1) {
                return;                                // bra .cantshoot ; rts
            }
        }

        // .foundonefree:
        Mem.wb(a5 + ObjT_TypeID_b, OBJ_TYPE_PROJECTILE); // move.b #OBJ_TYPE_PROJECTILE,ObjT_TypeID_b(a5)
        int a6 = ObjRotated_vl;                        // move.l #ObjRotated_vl,a6
        int d0 = Mem.uw(a0);                           // move.w (a0),d0
        a6 = a6 + ((short) d0) * 8;                    // lea (a6,d0.w*8),a6
        Mem.wl(HiresData.Aud_NoiseX_w, Mem.l(a6));     // move.l (a6),Aud_NoiseX_w
        Mem.ww(HiresData.Aud_NoiseVol_w, 100);         // move.w #100,Aud_NoiseVol_w
        Mem.wb(HiresData.Aud_ChannelPick_b, 1);        // move.b #1,Aud_ChannelPick_b
        Mem.wb(HiresData.notifplaying, 0);             // clr.b notifplaying
        d0 = Mem.ub(SHOTPOWER);                        // move.b SHOTPOWER,d0 (≠ FireAtPlayer1 qui lit SHOTTYPE — conservé)
        Mem.ww(a5 + ShotT_Lifetime_w, 0);              // move.w #0,ShotT_Lifetime_w(a5)
        Mem.wb(a5 + ShotT_Size_b, d0);                 // move.b d0,ShotT_Size_b(a5)
        Mem.wb(a5 + ShotT_Power_w, Mem.ub(SHOTPOWER)); // move.b SHOTPOWER,ShotT_Power_w(a5)
        // movem.l a5/a1/a0,-(a7)
        Mem.ww(HiresData.IDNUM, Mem.uw(a0));           // move.w (a0),IDNUM
        Mem.wb(HiresData.PlayEcho, Mem.ub(ALIENECHO)); // move.b ALIENECHO,PlayEcho
        Hires.MakeSomeNoise();                         // jsr MakeSomeNoise
        // movem.l (a7)+,a5/a1/a0

        int a2 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a2
        d1 = Mem.uw(a5);                               // move.w (a5),d1
        a2 = a2 + ((short) d1) * 8;                    // lea (a2,d1.w*8),a2
        Mem.ww(oldx, Mem.uw(a1));                      // move.w (a1),oldx
        Mem.ww(oldz, Mem.uw(a1 + 4));                  // move.w 4(a1),oldz
        Mem.ww(newx, Mem.uw(Plr2_XOff_l));             // move.w Plr2_XOff_l,newx
        Mem.ww(newz, Mem.uw(Plr2_ZOff_l));             // move.w Plr2_ZOff_l,newz
        Mem.ww(speed, Mem.uw(SHOTSPEED));              // move.w SHOTSPEED,speed
        Mem.ww(Range, 0);                              // move.w #0,Range
        Objectmove.HeadTowards();                      // jsr HeadTowards

        d0 = Mem.uw(newx);                             // move.w newx,d0
        d0 = setw(d0, d0 - Mem.uw(oldx));              // sub.w oldx,d0
        d1 = Mem.uw(newz);                             // move.w newz,d1
        d1 = setw(d1, d1 - Mem.uw(oldz));              // sub.w oldz,d1
        int d2 = Mem.uw(SHOTOFFMULT);                  // move.w SHOTOFFMULT,d2
        if ((short) d2 != 0) {                         // beq.s .nooffset
            d0 = muls(d0, d2);                         // muls d2,d0
            d1 = muls(d1, d2);                         // muls d2,d1
            d0 >>= 8;                                  // asr.l #8,d0
            d1 >>= 8;                                  // asr.l #8,d1
            Mem.ww(oldx, Mem.uw(oldx) + d1);           // add.w d1,oldx
            Mem.ww(oldz, Mem.uw(oldz) - d0);           // sub.w d0,oldz
            Mem.ww(newx, Mem.uw(Plr2_XOff_l));         // move.w Plr2_XOff_l,newx
            Mem.ww(newz, Mem.uw(Plr2_ZOff_l));         // move.w Plr2_ZOff_l,newz
            Objectmove.HeadTowards();                  // jsr HeadTowards
        }

        // .nooffset:
        d0 = Mem.uw(newx);                             // move.w newx,d0
        Mem.ww(a2, d0);                                // move.w d0,(a2)
        d0 = setw(d0, d0 - Mem.uw(oldx));              // sub.w oldx,d0
        Mem.ww(a5 + ShotT_VelocityX_w, d0);            // move.w d0,ShotT_VelocityX_w(a5)
        d0 = Mem.uw(newz);                             // move.w newz,d0
        Mem.ww(a2 + 4, d0);                            // move.w d0,4(a2)
        d0 = setw(d0, d0 - Mem.uw(oldz));              // sub.w oldz,d0
        Mem.ww(a5 + ShotT_VelocityZ_w, d0);            // move.w d0,ShotT_VelocityZ_w(a5)
        Mem.wl(a5 + EntT_EnemyFlags_l, 0b110010);      // move.l #%110010,EntT_EnemyFlags_l(a5)
        Mem.ww(a5 + ObjT_ZoneID_w, Mem.uw(a0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0),ObjT_ZoneID_w(a5)
        d0 = Mem.uw(a0 + 4);                           // move.w 4(a0),d0
        Mem.ww(a5 + 4, d0);                            // move.w d0,4(a5)
        d0 = (short) d0;                               // ext.l d0
        d0 <<= 7;                                      // asl.l #7,d0
        d0 += Mem.l(SHOTYOFF);                         // add.l SHOTYOFF,d0
        Mem.wl(a5 + ShotT_AccYPos_w, d0);              // move.l d0,ShotT_AccYPos_w(a5)
        Mem.wb(a5 + ShotT_InUpperZone_b, Mem.ub(SHOTINTOP)); // move.b SHOTINTOP,ShotT_InUpperZone_b(a5)
        a2 = Mem.l(Plr2_ObjectPtr_l);                  // move.l Plr2_ObjectPtr_l,a2
        d1 = Mem.uw(a2 + 4);                           // move.w 4(a2),d1
        d1 = setw(d1, d1 - 20);                        // sub.w #20,d1
        d1 = (short) d1;                               // ext.l d1
        d1 <<= 7;                                      // asl.l #7,d1
        d1 -= d0;                                      // sub.l d0,d1
        d1 += d1;                                      // add.l d1,d1
        d0 = Mem.uw(distaway);                         // move.w distaway,d0
        d2 = Mem.uw(SHOTSHIFT);                        // move.w SHOTSHIFT,d2
        d0 = setw(d0, ((short) d0) >> (d2 & 63));      // asr.w d2,d0
        if ((short) d0 <= 0) {                         // tst.w d0 ; bgt.s .okokokok
            d0 = 1;                                    // moveq #1,d0
        }
        // .okokokok:
        d1 = divs(d1, d0);                             // divs d0,d1
        Mem.ww(a5 + ShotT_VelocityY_w, d1);            // move.w d1,ShotT_VelocityY_w(a5)
        Mem.wb(a5 + ShotT_Worry_b, 0xFF);              // st ShotT_Worry_b(a5)
        Mem.ww(a5 + ShotT_Gravity_w, 0);               // move.w #0,ShotT_Gravity_w(a5)
        // .cantshoot: rts
    }
}
