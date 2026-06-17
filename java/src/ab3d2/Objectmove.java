package ab3d2;

import ab3d2.bss.ZoneBss;

import static ab3d2.Defs.*;
import static ab3d2.M68k.divs;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;
import static ab3d2.ObjectmoveData.*;
import static ab3d2.bss.AiBss.AI_FlyABit_w;
import static ab3d2.bss.LevelBss.Lvl_ClipsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_GraphicsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_PointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneEdgePtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneGraphAddsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.TablesBss.Obj_RoomPath_vw;
import static ab3d2.data.TablesData.COSINE_OFS;
import static ab3d2.data.TablesData.SinCosTable_vw;

/**
 * Traduction littérale de ab3d2_source/objectmove.s
 *
 * Déplacement/collision des objets (joueurs, monstres, projectiles) :
 *  - MoveObject : avance (oldx,oldz,oldy)→(newx,newz,newy) à travers les
 *    arêtes de zones (marche/portes/step-up/step-down), met à jour
 *    Obj_ZonePtr_l et Obj_RoomPath_vw, clippe contre les murs (hitwall) ;
 *  - HeadTowards(Ang) : avance vers la cible à `speed`, portée `Range`,
 *    angle de marche dans AngRet (recherche binaire 4 étapes sur la table
 *    sinus), GotThere si arrivé ;
 *  - CanItBeSeen(Ang) : ligne de vue via PVS + clips + hauteurs des arêtes ;
 *  - Obj_DoCollision : collision contre les autres objets de la zone —
 *    ATTENTION : lit 2(a2,d3.w*8)/4(a2,d3.w*8) avec a2 HÉRITÉ de l'appelant
 *    (registre non initialisé localement dans l'original) → a2 est un
 *    paramètre explicite ici, chaque site d'appel passe sa valeur réelle ;
 *  - CheckTeleport : téléporte via ZoneT_TelZone/TelX/TelZ — renvoie la
 *    valeur de sortie du registre a2 (pointeur de zone), héritée ensuite
 *    par les appels Obj_DoCollision des routines AI ;
 *  - GetRand : générateur (rol.w #3 + $2343) ; GoInDirection ; CalcDist ;
 *    GetNextCPt (liens walk/fly entre points de contrôle) ; FindCloseRoom.
 *
 * Les blocs marqués "Unreachable" dans le source (code mort après bra/rts)
 * sont conservés en commentaire.
 */
public final class Objectmove {

    private Objectmove() {
    }

    /** MoveObject */
    public static void MoveObject() {
        Mem.wl(obj_ZoneBackupPtr_l, Mem.l(Obj_ZonePtr_l)); // move.l Obj_ZonePtr_l,obj_ZoneBackupPtr_l
        Mem.ww(obj_QuitLimit_w, 50);                   // move.w #50,obj_QuitLimit_w
        Mem.wl(obj_RoomPathPtr_l, Obj_RoomPath_vw);    // move.l #Obj_RoomPath_vw,obj_RoomPathPtr_l
        Mem.wb(hitwall, 0);                            // clr.b hitwall
        int d0 = Mem.uw(newx);                         // move.w newx,d0
        d0 = setw(d0, d0 - Mem.uw(oldx));              // sub.w oldx,d0
        Mem.ww(xdiff, d0);                             // move.w d0,xdiff
        d0 = Mem.uw(newz);                             // move.w newz,d0
        d0 = setw(d0, d0 - Mem.uw(oldz));              // sub.w oldz,d0
        Mem.ww(zdiff, d0);                             // move.w d0,zdiff
        if (Mem.w(xdiff) == 0 && Mem.w(zdiff) == 0) {  // tst.w xdiff ; bne.s .moving ; tst.w zdiff ; bne.s .moving
            return;                                    // rts
        }

        // .moving:
        Mem.wl(wallhitheight, Mem.l(newy));            // move.l newy,wallhitheight
        int a0 = Mem.l(Obj_ZonePtr_l);                 // move.l Obj_ZonePtr_l,a0

        int a5;
        gobackanddoitallagain:
        while (true) { // gobackanddoitallagain:
            a5 = a0;                                   // move.l a0,a5
            a0 = a0 + Mem.w(a5 + ZoneT_EdgeListOffset_w); // adda.w ZoneT_EdgeListOffset_w(a5),a0
            Mem.wl(HiresData.test, a0);                // move.l a0,test
            int a1 = Mem.l(Lvl_ZoneEdgePtr_l);         // move.l Lvl_ZoneEdgePtr_l,a1

            int d1, d2, d3, d4, d5, d6, d7, a2, a4, a6;

            checkwalls:
            while (true) { // checkwalls:
                d0 = Mem.w(a0); a0 += 2;               // move.w (a0)+,d0
                if (d0 < 0) {                          // blt no_more_walls
                    break;
                }
                d0 = setw(d0, d0 << 4);                // asl.w #4,d0
                a2 = a1 + (short) d0;                  // lea (a1,d0.w),a2

                // *********************************
                // * Check if we are within exit limits of zone.
                // *********************************
                // A2 contains current EdgeT address
                d0 = 0xFF000000;                       // move.l #-65536*256,d0
                Mem.wl(LowerRoofHeight, d0);           // move.l d0,LowerRoofHeight
                Mem.wl(UpperRoofHeight, d0);           // move.l d0,UpperRoofHeight
                Mem.wl(LowerFloorHeight, d0);          // move.l d0,LowerFloorHeight
                Mem.wl(UpperFloorHeight, d0);          // move.l d0,UpperFloorHeight
                d1 = 0;                                // moveq #0,d1
                d1 = setw(d1, Mem.uw(a2 + EdgeT_JoinZone_w)); // move.w EdgeT_JoinZone_w(a2),d1
                if ((short) d1 >= 0) {                 // blt thisisawall2
                    int a4z = Mem.l(Lvl_ZonePtrsPtr_l); // move.l Lvl_ZonePtrsPtr_l,a4
                    a4z = Mem.l(a4z + ((short) d1) * 4); // move.l (a4,d1.w*4),a4
                    d1 = Mem.l(a4z + ZoneT_Floor_l);   // move.l ZoneT_Floor_l(a4),d1
                    Mem.wl(LowerFloorHeight, d1);      // move.l d1,LowerFloorHeight
                    d2 = Mem.l(a4z + ZoneT_Roof_l);    // move.l ZoneT_Roof_l(a4),d2
                    Mem.wl(LowerRoofHeight, d2);       // move.l d2,LowerRoofHeight
                    // bra thisisawall1 [bloc 70-96 "Unreachable code?" omis]
                    // thisisawall1:
                    d1 = Mem.l(a4z + ZoneT_UpperFloor_l); // move.l ZoneT_UpperFloor_l(a4),d1
                    Mem.wl(UpperFloorHeight, d1);      // move.l d1,UpperFloorHeight
                    d2 = Mem.l(a4z + ZoneT_UpperRoof_l); // move.l ZoneT_UpperRoof_l(a4),d2
                    d1 -= d2;                          // sub.l d2,d1 (résultat mort)
                    Mem.wl(UpperRoofHeight, d2);       // move.l d2,UpperRoofHeight
                    // bra thisisawall2 [bloc 106-132 "Unreachable ?" omis]
                }

                // thisisawall2:
                a4 = 0;                                // move.l #0,a4
                a6 = 0;                                // move.l #0,a6
                d3 = Mem.b(Obj_AwayFromWall_b);        // move.b Obj_AwayFromWall_b,d3
                if ((byte) d3 >= 0) {                  // blt.s .notomatoes
                    d2 = Mem.b(a2 + EdgeT_Byte_12);    // move.b EdgeT_Byte_12(a2),d2 ; ext.w d2
                    d4 = Mem.b(a2 + EdgeT_Byte_13);    // move.b EdgeT_Byte_13(a2),d4 ; ext.w d4
                    if ((byte) d3 != 0) {              // tst.b d3 ; beq.s .noshift
                        d2 = setw(d2, d2 << (d3 & 63)); // asl.w d3,d2
                        d4 = setw(d4, d4 << (d3 & 63)); // asl.w d3,d4
                    }
                    // .noshift:
                    a4 = (short) d2;                   // move.w d2,a4
                    a6 = (short) d4;                   // move.w d4,a6
                }

                // .notomatoes:
                d0 = Mem.uw(newx);                     // move.w newx,d0
                d1 = Mem.uw(newz);                     // move.w newz,d1
                d0 = setw(d0, d0 - Mem.uw(a2));        // sub.w (a2),d0 ; EdgeT_XPos_w
                d1 = setw(d1, d1 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d1
                d0 = setw(d0, d0 - a4);                // sub.w a4,d0
                d1 = setw(d1, d1 - a6);                // sub.w a6,d1
                d2 = Mem.uw(a2 + EdgeT_XLen_w);        // move.w EdgeT_XLen_w(a2),d2
                d2 = setw(d2, d2 - a4);                // sub.w a4,d2
                d2 = setw(d2, d2 - a6);                // sub.w a6,d2
                d1 = muls(d1, d2);                     // muls d2,d1
                d5 = Mem.uw(a2 + EdgeT_ZLen_w);        // move.w EdgeT_ZLen_w(a2),d5
                d5 = setw(d5, d5 + a4);                // add.w a4,d5
                d5 = setw(d5, d5 - a6);                // sub.w a6,d5
                d0 = muls(d0, d5);                     // muls d5,d0
                d0 -= d1;                              // sub.l d1,d0
                if (d0 > 0) {                          // ble chkhttt
                    d3 = Mem.uw(a2 + EdgeT_Word_5);    // move.w EdgeT_Word_5(a2),d3
                    d3 = setw(d3, d3 + Mem.uw(Obj_ExtLen_w)); // add.w Obj_ExtLen_w,d3
                    d0 = divs(d0, d3);                 // divs d3,d0
                    if ((short) d0 < 32) {             // cmp.w #32,d0 ; bge oknothitwall
                        Mem.ww(a2 + EdgeT_Flags_w,
                                Mem.uw(a2 + EdgeT_Flags_w) | Mem.uw(wallflags)); // or.w wallflags,EdgeT_Flags_w(a2)
                    }
                    continue;                          // bra oknothitwall → checkwalls
                }

                // chkhttt:
                d7 = d0;                               // move.l d0,d7
                d3 = Mem.uw(a2 + EdgeT_Word_5);        // move.w EdgeT_Word_5(a2),d3
                d3 = setw(d3, d3 + Mem.uw(Obj_ExtLen_w)); // add.w Obj_ExtLen_w,d3
                d7 = divs(d7, d3);                     // divs d3,d7 ; d

                d4 = Mem.l(newy);                      // move.l newy,d4
                d4 -= Mem.l(oldy);                     // sub.l oldy,d4

                d0 = Mem.uw(oldx);                     // move.w oldx,d0
                d1 = Mem.uw(oldz);                     // move.w oldz,d1
                d0 = setw(d0, d0 - Mem.uw(a2));        // sub.w (a2),d0
                d1 = setw(d1, d1 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d1
                d0 = setw(d0, d0 - a4);                // sub.w a4,d0
                d1 = setw(d1, d1 - a6);                // sub.w a6,d1
                d1 = muls(d1, d2);                     // muls d2,d1
                d0 = muls(d0, d5);                     // muls d5,d0
                d0 -= d1;                              // sub.l d1,d0
                d0 = divs(d0, d3);                     // divs d3,d0 ; otherd
                d0 = setw(d0, d0 - d7);                // sub.w d7,d0 ; total distance travelled across wall
                if ((short) d0 <= 0) {                 // bgt.s .ohbugger
                    d0 = 1;                            // moveq #1,d0
                }

                // .ohbugger:
                // We now have ratio to multiply x,z and y differences by. Check y=0.
                d1 = d4;                               // move.l d4,d1
                if (d1 != 0) {                         // beq.s .dontworryhit
                    d1 = divs(d1, d0);                 // divs d0,d1
                    d1 = muls(d1, d7);                 // muls d7,d1
                }

                // .dontworryhit:
                d1 += Mem.l(newy);                     // add.l newy,d1 ; height at point of crossing wall.
                d6 = d1;                               // move.l d1,d6
                d6 += Mem.l(ObjectmoveData.thingheight); // add.l thingheight,d6
                d6 -= Mem.l(StepUpVal);                // sub.l StepUpVal,d6
                boolean yeshit;
                if (d6 >= Mem.l(LowerFloorHeight)) {   // cmp.l LowerFloorHeight,d6 ; bge.s .yeshit
                    yeshit = true;
                } else if (d1 > Mem.l(LowerRoofHeight)) { // cmp.l LowerRoofHeight,d1 ; bgt oknothitwall
                    continue;
                } else if (d1 < Mem.l(UpperRoofHeight)) { // cmp.l UpperRoofHeight,d1 ; blt.s .yeshit
                    yeshit = true;
                } else if (d6 < Mem.l(UpperFloorHeight)) { // cmp.l UpperFloorHeight,d6 ; blt oknothitwall
                    continue;
                } else {
                    yeshit = true;
                }

                // .yeshit:
                Mem.wl(wallhitheight, d1);             // move.l d1,wallhitheight
                boolean calcwherehit;
                if (Mem.b(Obj_WallBounce_b) != 0) {    // tst.b Obj_WallBounce_b ; bne.s .calcbounce
                    // .calcbounce: place the object at wall contact point, supply wall data
                    Mem.ww(WallXSize_w, d2);           // move.w d2,WallXSize_w
                    Mem.ww(WallZSize_w, d5);           // move.w d5,WallZSize_w
                    Mem.ww(WallLength_w, d3);          // move.w d3,WallLength_w
                    calcwherehit = true;
                } else {
                    calcwherehit = Mem.b(exitfirst) != 0; // tst.b exitfirst ; beq.s .calcalong ; bne.s .calcwherehit
                }

                if (calcwherehit) {
                    // .calcwherehit:
                    d6 = Mem.uw(newx);                 // move.w newx,d6
                    d6 = setw(d6, d6 - Mem.uw(oldx));  // sub.w oldx,d6
                    d6 = muls(d6, d7);                 // muls d7,d6
                    d6 = divs(d6, d0);                 // divs d0,d6
                    d6 = setw(d6, d6 + Mem.uw(newx));  // add.w newx,d6
                    d1 = Mem.uw(newz);                 // move.w newz,d1
                    d1 = setw(d1, d1 - Mem.uw(oldz));  // sub.w oldz,d1
                    d1 = muls(d1, d7);                 // muls d7,d1
                    d1 = divs(d1, d0);                 // divs d0,d1
                    d1 = setw(d1, d1 + Mem.uw(newz));  // add.w newz,d1
                    d0 = setw(d0, d6);                 // move.w d6,d0
                    d7 = setw(d7, d1);                 // move.w d1,d7
                    // bra.s .calcedhit
                    // .calcedhit: (le segment a-t-il vraiment été traversé ?)
                    d6 = Mem.uw(newx);                 // move.w newx,d6
                    d7 = setw(d7, Mem.uw(newz));       // move.w newz,d7
                    d6 = setw(d6, d6 - Mem.uw(oldx));  // sub.w oldx,d6
                    d7 = setw(d7, d7 - Mem.uw(oldz));  // sub.w oldz,d7
                    d4 = Mem.uw(a2);                   // move.w (a2),d4 ; EdgeT_XPos_w
                    d4 = setw(d4, d4 + a4);            // add.w a4,d4
                    d4 = setw(d4, d4 - Mem.uw(oldx));  // sub.w oldx,d4
                    d7 = muls(d7, d4);                 // muls d4,d7 ; negative if on left
                    d4 = Mem.uw(a2 + EdgeT_ZPos_w);    // move.w EdgeT_ZPos_w(a2),d4
                    d4 = setw(d4, d4 + a6);            // add.w a6,d4
                    d4 = setw(d4, d4 - Mem.uw(oldz));  // sub.w oldz,d4
                    d6 = muls(d6, d4);                 // muls d4,d6
                    d7 -= d6;                          // sub.l d6,d7
                    if (d7 > 0) {                      // bgt oknothitwall
                        continue;
                    }
                    // (move.w d0,d6 ; move.w d1,d7 — aussitôt écrasés, conservés)
                    d6 = Mem.uw(newx);                 // move.w newx,d6
                    d7 = setw(d7, Mem.uw(newz));       // move.w newz,d7
                    d6 = setw(d6, d6 - Mem.uw(oldx));  // sub.w oldx,d6
                    d7 = setw(d7, d7 - Mem.uw(oldz));  // sub.w oldz,d7
                    d4 = Mem.uw(a2);                   // move.w (a2),d4
                    d4 = setw(d4, d4 + a4);            // add.w a4,d4
                    d4 = setw(d4, d4 + d2);            // add.w d2,d4
                    d4 = setw(d4, d4 - Mem.uw(oldx));  // sub.w oldx,d4
                    d7 = muls(d7, d4);                 // muls d4,d7
                    d4 = Mem.uw(a2 + EdgeT_ZPos_w);    // move.w EdgeT_ZPos_w(a2),d4
                    d4 = setw(d4, d4 + a6);            // add.w a6,d4
                    d4 = setw(d4, d4 + d5);            // add.w d5,d4
                    d4 = setw(d4, d4 - Mem.uw(oldz));  // sub.w oldz,d4
                    d6 = muls(d6, d4);                 // muls d4,d6
                    d7 -= d6;                          // sub.l d6,d7
                    if (d7 < 0) {                      // blt oknothitwall
                        continue;
                    }
                    // bra hitthewall — d0/d1 contiennent... NB : d0 a été setw(d6) AVANT .calcedhit,
                    // mais d1 garde la coordonnée Z calculée (d1h) — l'original réutilise d0/d1 d'avant.
                    // hitthewall: (chute plus bas)
                } else {
                    // .calcalong:
                    d6 = setw(0, d7);                  // move.w d7,d6
                    d6 = muls(d6, d5);                 // muls d5,d6
                    d7 = muls(d7, d2);                 // muls d2,d7
                    d6 = divs(d6, d3);                 // divs d3,d6
                    d7 = divs(d7, d3);                 // divs d3,d7
                    d6 = setw(d6, -(short) d6);        // neg.w d6
                    d6 = setw(d6, d6 + Mem.uw(newx));  // add.w newx,d6 ; point on wall
                    d7 = setw(d7, d7 + Mem.uw(newz));  // add.w newz,d7
                    d0 = setw(d0, d6);                 // move.w d6,d0
                    d1 = setw(d1, d7);                 // move.w d7,d1
                    // bra.s othercheck
                    // othercheck:
                    d6 = setw(d6, d6 - Mem.uw(a2));    // sub.w (a2),d6 ; EdgeT_XPos_w
                    d7 = setw(d7, d7 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d7
                    d6 = setw(d6, d6 - a4);            // sub.w a4,d6
                    d7 = setw(d7, d7 - a6);            // sub.w a6,d7
                    d4 = setw(0, d2);                  // move.w d2,d4
                    if ((short) d4 < 0) {              // bge.s okplus1
                        d4 = setw(d4, -(short) d4);    // neg.w d4
                    }
                    // okplus1:
                    d3 = setw(d3, d5);                 // move.w d5,d3
                    if ((short) d3 < 0) {              // bge.s okplus2
                        d3 = setw(d3, -(short) d3);    // neg.w d3
                    }
                    // okplus2:
                    if ((short) d3 > (short) d4) {     // cmp.w d4,d3 ; bgt.s UseZ
                        // UseZ:
                        if ((short) d7 <= 0) {         // tst.w d7 ; bgt.s zispos
                            d6 = setw(d6, d5);         // move.w d5,d6
                            if ((short) d6 > 4) {      // cmp.w #4,d6 ; bgt.s oknothitwall
                                continue;
                            }
                            d6 = setw(d6, d6 - 4);     // sub.w #4,d6
                            if ((short) d7 < (short) d6) { // cmp.w d6,d7 ; blt.s oknothitwall
                                continue;
                            }
                            // bra.s hitthewall
                        } else {
                            // zispos:
                            d6 = setw(d6, d5);         // move.w d5,d6
                            if ((short) d6 < -4) {     // cmp.w #-4,d6 ; blt.s oknothitwall
                                continue;
                            }
                            d6 = setw(d6, d6 + 4);     // add.w #4,d6
                            if ((short) d7 > (short) d6) { // cmp.w d6,d7 ; bgt.s oknothitwall
                                continue;
                            }
                        }
                    } else {
                        // Use the x coord!
                        if ((short) d6 <= 0) {         // tst.w d6 ; bgt.s xispos
                            d7 = setw(d7, d2);         // move.w d2,d7
                            if ((short) d7 > 4) {      // cmp.w #4,d7 ; bgt.s oknothitwall
                                continue;
                            }
                            d7 = setw(d7, d7 - 4);     // sub.w #4,d7
                            if ((short) d6 < (short) d7) { // cmp.w d7,d6 ; blt.s oknothitwall
                                continue;
                            }
                            // bra.s hitthewall
                        } else {
                            // xispos:
                            d7 = setw(d7, d2);         // move.w d2,d7
                            if ((short) d7 < -4) {     // cmp.w #-4,d7 ; blt.s oknothitwall
                                continue;
                            }
                            d7 = setw(d7, d7 + 4);     // add.w #4,d7
                            if ((short) d6 > (short) d7) { // cmp.w d7,d6 ; bgt.s oknothitwall
                                continue;
                            }
                        }
                    }
                }

                // hitthewall:
                Mem.ww(newx, d0);                      // move.w d0,newx
                Mem.ww(newz, d1);                      // move.w d1,newz
                Mem.ww(a2 + EdgeT_Flags_w,
                        Mem.uw(a2 + EdgeT_Flags_w) | Mem.uw(wallflags)); // or.w wallflags,EdgeT_Flags_w(a2)
                Mem.wb(hitwall, 0xFF);                 // st hitwall
                if (Mem.b(exitfirst) != 0) {           // tst.b exitfirst ; bne stopandleave
                    // stopandleave:
                    int rp = Mem.l(obj_RoomPathPtr_l); // move.l obj_RoomPathPtr_l,a0
                    Mem.ww(rp, -1);                    // move.w #-1,(a0)+
                    return;                            // rts
                }
                // oknothitwall: bra checkwalls
            }

            // no_more_walls:
            if (Mem.uw(Obj_ExtLen_w) != 0) {           // tst.w Obj_ExtLen_w ; beq NOOTHERWALLSNEEDED
                if (Mem.w(xdiff) == 0 && Mem.w(zdiff) == 0) { // tst.w xdiff ; bne.s notstill ; tst.w zdiff ; bne.s notstill
                    // a0 = Obj_ZonePtr (mort) ; bra mustbeinsameroom
                    int rp = Mem.l(obj_RoomPathPtr_l); // move.l obj_RoomPathPtr_l,a0
                    Mem.ww(rp, -1);                    // move.w #-1,(a0)+
                    return;                            // rts
                }

                // notstill:
                a0 = a5 + Mem.w(a5 + ZoneT_EdgeListOffset_w); // move.l a5,a0 ; add.w ZoneT_EdgeListOffset_w(a0),a0

                checkotherwalls:
                while (true) { // checkotherwalls:
                    d0 = Mem.w(a0); a0 += 2;           // move.w (a0)+,d0
                    if (d0 < 0) {                      // bge anotherwalls
                        if (d0 == -2) {                // cmp.w #-2,d0 ; beq nomoreotherwalls
                            break;
                        }
                        continue;                      // bra checkotherwalls
                    }

                    // anotherwalls:
                    d0 = setw(d0, d0 << 4);            // asl.w #4,d0
                    a2 = a1 + (short) d0;              // lea (a1,d0.w),a2

                    // Check if we are within exit limits of zone.
                    d1 = 0;                            // moveq #0,d1
                    d1 = setw(d1, Mem.uw(a2 + EdgeT_JoinZone_w)); // move.w EdgeT_JoinZone_w(a2),d1
                    if ((short) d1 >= 0) {             // blt .thisisawall2
                        int a4z = Mem.l(Lvl_ZonePtrsPtr_l); // move.l Lvl_ZonePtrsPtr_l,a4
                        a4z = Mem.l(a4z + ((short) d1) * 4); // move.l (a4,d1.w*4),a4
                        d1 = Mem.l(a4z + ZoneT_Floor_l); // move.l ZoneT_Floor_l(a4),d1
                        d1 -= Mem.l(a4z + ZoneT_Roof_l); // sub.l ZoneT_Roof_l(a4),d1
                        boolean wall1 = false;
                        if (d1 > Mem.l(ObjectmoveData.thingheight)) { // cmp.l thingheight,d1 ; ble .thisisawall1
                            d0 = Mem.l(newy);          // move.l newy,d0
                            d1 = d0;                   // move.l d0,d1
                            d1 += Mem.l(ObjectmoveData.thingheight); // add.l thingheight,d1
                            d1 -= Mem.l(a4z + ZoneT_Floor_l); // sub.l ZoneT_Floor_l(a4),d1
                            boolean botinside;
                            if (d1 > 0) {              // bgt.s .chkstepup
                                // .chkstepup:
                                botinside = d1 < Mem.l(StepUpVal); // cmp.l StepUpVal,d1 ; blt.s .botinsidebot
                            } else {
                                d1 = -d1;              // neg.l d1
                                botinside = d1 < Mem.l(StepDownVal); // cmp.l StepDownVal,d1 ; blt.s .botinsidebot
                            }
                            if (botinside) {
                                // .botinsidebot:
                                d0 -= Mem.l(a4z + ZoneT_Roof_l); // sub.l ZoneT_Roof_l(a4),d0
                                if (d0 >= 0) {         // blt.s .thisisawall1
                                    continue;          // bra checkotherwalls (passable)
                                }
                            }
                            // We have a wall! → .thisisawall1
                        }
                        // .thisisawall1:
                        d1 = Mem.l(a4z + ZoneT_UpperFloor_l); // move.l ZoneT_UpperFloor_l(a4),d1
                        d1 -= Mem.l(a4z + ZoneT_UpperRoof_l); // sub.l ZoneT_UpperRoof_l(a4),d1
                        if (d1 > Mem.l(ObjectmoveData.thingheight)) { // cmp.l thingheight,d1 ; ble .thisisawall2
                            d0 = Mem.l(newy);          // move.l newy,d0
                            d1 = d0;                   // move.l d0,d1
                            d1 += Mem.l(ObjectmoveData.thingheight); // add.l thingheight,d1
                            d1 -= Mem.l(a4z + ZoneT_UpperFloor_l); // sub.l ZoneT_UpperFloor_l(a4),d1
                            boolean botinside;
                            if (d1 > 0) {              // bgt.s .chkstepup2
                                botinside = d1 < Mem.l(StepUpVal); // cmp.l StepUpVal,d1 ; blt.s .botinsidebot2
                            } else {
                                d1 = -d1;              // neg.l d1
                                botinside = d1 < Mem.l(StepDownVal); // cmp.l StepDownVal,d1 ; blt.s .botinsidebot2
                            }
                            if (botinside) {
                                // .botinsidebot2:
                                d0 -= Mem.l(a4z + ZoneT_UpperRoof_l); // sub.l ZoneT_UpperRoof_l(a4),d0
                                if (d0 >= 0) {         // blt.s .thisisawall2
                                    continue;          // bra checkotherwalls (passable)
                                }
                            }
                            // We have a wall! → .thisisawall2
                        }
                    }

                    // .thisisawall2:
                    a4 = 0;                            // move.l #0,a4
                    a6 = 0;                            // move.l #0,a6
                    d3 = Mem.b(Obj_AwayFromWall_b);    // move.b Obj_AwayFromWall_b,d3
                    if ((byte) d3 >= 0) {              // blt.s .notomatoes
                        d2 = Mem.b(a2 + EdgeT_Byte_12); // move.b EdgeT_Byte_12(a2),d2 ; ext.w
                        d4 = Mem.b(a2 + EdgeT_Byte_13); // move.b EdgeT_Byte_13(a2),d4 ; ext.w
                        if ((byte) d3 != 0) {          // tst.b d3 ; beq.s .noshift
                            d2 = setw(d2, d2 << (d3 & 63)); // asl.w d3,d2
                            d4 = setw(d4, d4 << (d3 & 63)); // asl.w d3,d4
                        }
                        // .noshift:
                        a4 = (short) d2;               // move.w d2,a4
                        a6 = (short) d4;               // move.w d4,a6
                    }

                    // .notomatoes:
                    d2 = Mem.uw(a2 + EdgeT_XLen_w);    // move.w EdgeT_XLen_w(a2),d2
                    d2 = setw(d2, d2 - a4);            // sub.w a4,d2
                    d2 = setw(d2, d2 - a6);            // sub.w a6,d2
                    Mem.ww(deltax, d2);                // move.w d2,deltax
                    d5 = Mem.uw(a2 + EdgeT_ZLen_w);    // move.w EdgeT_ZLen_w(a2),d5
                    d5 = setw(d5, d5 + a4);            // add.w a4,d5
                    d5 = setw(d5, d5 - a6);            // sub.w a6,d5
                    d0 = Mem.uw(newx);                 // move.w newx,d0
                    d1 = Mem.uw(newz);                 // move.w newz,d1
                    d0 = setw(d0, d0 - Mem.uw(a2));    // sub.w (a2),d0
                    d1 = setw(d1, d1 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d1
                    d0 = setw(d0, d0 - a4);            // sub.w a4,d0
                    d1 = setw(d1, d1 - a6);            // sub.w a6,d1
                    d1 = muls(d1, Mem.w(deltax));      // muls deltax,d1
                    d0 = muls(d0, d5);                 // muls d5,d0
                    d0 -= d1;                          // sub.l d1,d0
                    if (d0 >= 0) {                     // bge .oknothitwall
                        continue;
                    }

                    d7 = d0;                           // move.l d0,d7
                    d1 = Mem.uw(oldx);                 // move.w oldx,d1
                    d3 = Mem.uw(newx);                 // move.w newx,d3
                    d3 = setw(d3, d3 - d1);            // sub.w d1,d3
                    d1 = setw(d1, d1 - Mem.uw(a2));    // sub.w (a2),d1
                    d1 = setw(d1, d1 - a4);            // sub.w a4,d1 ; e-a=d1
                    d2 = Mem.uw(a2 + EdgeT_ZPos_w);    // move.w EdgeT_ZPos_w(a2),d2
                    d2 = setw(d2, d2 + a6);            // add.w a6,d2
                    d2 = setw(d2, d2 - Mem.uw(oldz));  // sub.w oldz,d2 ; b-f=d2
                    d4 = Mem.uw(newz);                 // move.w newz,d4
                    d4 = setw(d4, d4 - Mem.uw(oldz));  // sub.w oldz,d4
                    d1 = muls(d1, d4);                 // muls d4,d1
                    d2 = muls(d2, d3);                 // muls d3,d2
                    d1 += d2;                          // add.l d2,d1 ; h(e-a)+g(b-f)
                    d4 = muls(d4, Mem.w(deltax));      // muls deltax,d4
                    d3 = muls(d3, d5);                 // muls d5,d3
                    d4 -= d3;                          // sub.l d3,d4
                    if (d4 == 0) {                     // beq .oknothitwall
                        continue;
                    }
                    if (d4 > 0) {                      // bgt.s .botpos
                        // .botpos:
                        if (d1 < 0) {                  // tst.l d1 ; blt .oknothitwall
                            continue;
                        }
                        if (d4 < d1) {                 // cmp.l d1,d4 ; blt .oknothitwall
                            continue;
                        }
                    } else {
                        // .botneg:
                        if (d1 > 0) {                  // tst.l d1 ; bgt .oknothitwall
                            continue;
                        }
                        if (d4 > d1) {                 // cmp.l d1,d4 ; ble .mighthit ; bra .oknothitwall
                            continue;
                        }
                    }

                    // .mighthit:
                    d0 = Mem.uw(a2 + EdgeT_Word_5);    // move.w EdgeT_Word_5(a2),d0
                    d0 = setw(d0, d0 + Mem.uw(Obj_ExtLen_w)); // add.w Obj_ExtLen_w,d0
                    d7 = divs(d7, d0);                 // divs d0,d7 ; d
                    d7 = setw(d7, d7 - 3);             // sub.w #3,d7
                    d6 = setw(0, d7);                  // move.w d7,d6
                    d6 = muls(d6, d5);                 // muls d5,d6
                    d7 = muls(d7, Mem.w(deltax));      // muls deltax,d7
                    d6 = divs(d6, d0);                 // divs d0,d6
                    d7 = divs(d7, d0);                 // divs d0,d7
                    d6 = setw(d6, -(short) d6);        // neg.w d6
                    d6 = setw(d6, d6 + Mem.uw(newx));  // add.w newx,d6 ; point on wall
                    d7 = setw(d7, d7 + Mem.uw(newz));  // add.w newz,d7
                    d0 = Mem.uw(oldx);                 // move.w oldx,d0
                    d1 = Mem.uw(oldz);                 // move.w oldz,d1
                    d0 = setw(d0, d0 - Mem.uw(a2));    // sub.w (a2),d0
                    d1 = setw(d1, d1 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d1
                    d0 = setw(d0, d0 - a4);            // sub.w a4,d0
                    d1 = setw(d1, d1 - a6);            // sub.w a6,d1
                    d1 = muls(d1, Mem.w(deltax));      // muls deltax,d1
                    d0 = muls(d0, d5);                 // muls d5,d0
                    d0 -= d1;                          // sub.l d1,d0
                    if (d0 < 0) {                      // blt .oknothitwall
                        continue;
                    }

                    d0 = setw(d0, d6);                 // move.w d6,d0
                    d1 = setw(d1, d7);                 // move.w d7,d1
                    // bra .hitthewall [bloc 608-662 "Unreachable ?" omis]

                    // .hitthewall:
                    Mem.ww(newx, d0);                  // move.w d0,newx
                    Mem.ww(newz, d1);                  // move.w d1,newz
                    Mem.ww(a2 + EdgeT_Flags_w,
                            Mem.uw(a2 + EdgeT_Flags_w) | Mem.uw(wallflags)); // or.w wallflags,EdgeT_Flags_w(a2)
                    Mem.wb(hitwall, 0xFF);             // st hitwall
                    if (Mem.b(exitfirst) != 0) {       // tst.b exitfirst ; bne stopandleave
                        int rp = Mem.l(obj_RoomPathPtr_l);
                        Mem.ww(rp, -1);                // move.w #-1,(a0)+
                        return;                        // rts
                    }
                    // .oknothitwall: bra checkotherwalls
                }
                // nomoreotherwalls:
            }

            // NOOTHERWALLSNEEDED:
            // *****************************************************
            // * FIND ROOM WE'RE STANDING IN ***********************
            // *****************************************************
            a0 = a5 + Mem.w(a5 + ZoneT_EdgeListOffset_w); // move.l a5,a0 ; adda.w ZoneT_EdgeListOffset_w(a5),a0
            a1 = Mem.l(Lvl_ZoneEdgePtr_l);             // move.l Lvl_ZoneEdgePtr_l,a1

            while (true) { // CheckMoreFloorLines:
                d0 = Mem.w(a0); a0 += 2;               // move.w (a0)+,d0 ; either a floor line or -1
                if (d0 < 0) {                          // blt NoMoreFloorLines
                    // NoMoreFloorLines:
                    Mem.wl(Obj_ZonePtr_l, a5);         // move.l a5,a0 ; move.l a5,Obj_ZonePtr_l
                    // mustbeinsameroom: / stopandleave:
                    int rp = Mem.l(obj_RoomPathPtr_l); // move.l obj_RoomPathPtr_l,a0
                    Mem.ww(rp, -1);                    // move.w #-1,(a0)+
                    return;                            // rts
                }

                d0 = setw(d0, d0 << 4);                // asl.w #4,d0
                a2 = a1 + (short) d0;                  // lea (a1,d0.w),a2

                if (Mem.w(a2 + EdgeT_JoinZone_w) < 0) { // tst.w EdgeT_JoinZone_w(a2) ; blt.s CheckMoreFloorLines
                    continue;
                }

                d1 = 0;                                // moveq #0,d1
                d1 = setw(d1, Mem.uw(a2 + EdgeT_JoinZone_w)); // move.w EdgeT_JoinZone_w(a2),d1
                int a4z = Mem.l(Lvl_ZonePtrsPtr_l);    // move.l Lvl_ZonePtrsPtr_l,a4
                a4z = Mem.l(a4z + ((short) d1) * 4);   // move.l (a4,d1.w*4),a4
                Mem.wl(LowerRoofHeight, Mem.l(a4z + ZoneT_Roof_l)); // move.l ZoneT_Roof_l(a4),LowerRoofHeight

                // okthebottom:
                d0 = Mem.uw(newx);                     // move.w newx,d0
                d1 = Mem.uw(newz);                     // move.w newz,d1
                d0 = setw(d0, d0 - Mem.uw(a2));        // sub.w (a2),d0 ; a
                d1 = setw(d1, d1 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d1 ; b
                d1 = muls(d1, Mem.w(a2 + EdgeT_XLen_w)); // muls EdgeT_XLen_w(a2),d1
                d0 = muls(d0, Mem.w(a2 + EdgeT_ZLen_w)); // muls EdgeT_ZLen_w(a2),d0
                d3 = 0;                                // moveq #0,d3
                d3 = setw(d3, Mem.uw(a2 + EdgeT_JoinZone_w)); // move.w EdgeT_JoinZone_w(a2),d3
                int a3 = Mem.l(Lvl_ZonePtrsPtr_l);     // move.l Lvl_ZonePtrsPtr_l,a3
                a3 = Mem.l(a3 + ((short) d3) * 4);     // move.l (a3,d3.w*4),a3
                d0 -= d1;                              // sub.l d1,d0
                if (d0 >= 0) {                         // bge StillSameSide
                    continue;                          // → CheckMoreFloorLines
                }

                // checkifcrossed: player used to be on other side of this line.
                Mem.wl(billy, d0);                     // move.l d0,billy
                d6 = Mem.uw(newx);                     // move.w newx,d6
                d7 = Mem.uw(newz);                     // move.w newz,d7
                d6 = setw(d6, d6 - Mem.uw(oldx));      // sub.w oldx,d6
                d7 = setw(d7, d7 - Mem.uw(oldz));      // sub.w oldz,d7
                d4 = Mem.uw(a2);                       // move.w (a2),d4
                d4 = setw(d4, d4 - Mem.uw(oldx));      // sub.w oldx,d4
                d7 = muls(d7, d4);                     // muls d4,d7 ; negative if on left
                d4 = Mem.uw(a2 + EdgeT_ZPos_w);        // move.w EdgeT_ZPos_w(a2),d4
                d4 = setw(d4, d4 - Mem.uw(oldz));      // sub.w oldz,d4
                d6 = muls(d6, d4);                     // muls d4,d6
                d7 -= d6;                              // sub.l d6,d7
                if (d7 > 0) {                          // bgt StillSameSide
                    continue;
                }

                // (move.w d0,d6 ; move.w d1,d7 — écrasés aussitôt, conservés)
                d6 = Mem.uw(newx);                     // move.w newx,d6
                d7 = Mem.uw(newz);                     // move.w newz,d7
                d6 = setw(d6, d6 - Mem.uw(oldx));      // sub.w oldx,d6
                d7 = setw(d7, d7 - Mem.uw(oldz));      // sub.w oldz,d7
                d4 = Mem.uw(a2);                       // move.w (a2),d4
                d4 = setw(d4, d4 + Mem.uw(a2 + EdgeT_XLen_w)); // add.w EdgeT_XLen_w(a2),d4
                d4 = setw(d4, d4 - Mem.uw(oldx));      // sub.w oldx,d4
                d7 = muls(d7, d4);                     // muls d4,d7
                d4 = Mem.uw(a2 + EdgeT_ZPos_w);        // move.w EdgeT_ZPos_w(a2),d4
                d4 = setw(d4, d4 + Mem.uw(a2 + EdgeT_ZLen_w)); // add.w EdgeT_ZLen_w(a2),d4
                d4 = setw(d4, d4 - Mem.uw(oldz));      // sub.w oldz,d4
                d6 = muls(d6, d4);                     // muls d4,d6
                d7 -= d6;                              // sub.l d6,d7
                if (d7 < 0) {                          // blt StillSameSide
                    continue;
                }

                // Find height at crossing point:
                d7 = Mem.l(billy);                     // move.l billy,d7
                d7 = divs(d7, Mem.w(a2 + EdgeT_Word_5)); // divs EdgeT_Word_5(a2),d7
                d0 = Mem.uw(oldx);                     // move.w oldx,d0
                d1 = Mem.uw(oldz);                     // move.w oldz,d1
                d0 = setw(d0, d0 - Mem.uw(a2));        // sub.w (a2),d0
                d1 = setw(d1, d1 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d1
                d1 = muls(d1, Mem.w(a2 + EdgeT_XLen_w)); // muls EdgeT_XLen_w(a2),d1
                d0 = muls(d0, Mem.w(a2 + EdgeT_ZLen_w)); // muls EdgeT_ZLen_w(a2),d0
                d0 -= d1;                              // sub.l d1,d0
                d0 = divs(d0, Mem.w(a2 + EdgeT_Word_5)); // divs EdgeT_Word_5(a2),d0
                d0 = setw(d0, d0 - d7);                // sub.w d7,d0
                if ((short) d0 <= 0) {                 // bgt.s .ohbugger
                    d0 = 1;                            // moveq #1,d0
                }

                // .ohbugger:
                d4 = Mem.l(newy);                      // move.l newy,d4
                d4 -= Mem.l(oldy);                     // sub.l oldy,d4
                d4 = divs(d4, d0);                     // divs d0,d4
                d4 = muls(d4, d7);                     // muls d7,d4
                d4 += Mem.l(newy);                     // add.l newy,d4
                Mem.wb(StoodInTop, (d4 < Mem.l(LowerRoofHeight)) ? 0xFF : 0); // cmp.l LowerRoofHeight,d4 ; slt StoodInTop
                a5 = a3;                               // move.l a3,a5
                int rp = Mem.l(obj_RoomPathPtr_l);     // move.l obj_RoomPathPtr_l,a0
                Mem.ww(rp, Mem.uw(a3)); rp += 2;       // move.w (a3),(a0)+
                Mem.wl(obj_RoomPathPtr_l, rp);         // move.l a0,obj_RoomPathPtr_l
                a0 = a3;                               // move.l a3,a0
                Mem.wl(Obj_ZonePtr_l, a5);             // move.l a5,Obj_ZonePtr_l
                d0 = Mem.uw(obj_QuitLimit_w);          // move.w obj_QuitLimit_w,d0
                d0 = setw(d0, d0 - 1);                 // sub.w #1,d0
                if ((short) d0 == 0) {                 // beq.s ERRORINMOVEMENT
                    // ERRORINMOVEMENT:
                    Mem.ww(newx, Mem.uw(oldx));        // move.w oldx,newx
                    Mem.ww(newz, Mem.uw(oldz));        // move.w oldz,newz
                    Mem.wl(newy, Mem.l(oldy));         // move.l oldy,newy
                    Mem.wl(Obj_ZonePtr_l, Mem.l(obj_ZoneBackupPtr_l)); // move.l obj_ZoneBackupPtr_l,Obj_ZonePtr_l
                    Mem.wb(hitwall, 0xFF);             // st hitwall
                    return;                            // rts
                }
                Mem.ww(obj_QuitLimit_w, d0);           // move.w d0,obj_QuitLimit_w
                continue gobackanddoitallagain;        // bra gobackanddoitallagain
            }
        }
    }

    /** Racine carrée Newton (motif partagé) : renvoie d0 (word) ≈ sqrt(d2), n itérations. */
    private static int sqrtNewton(int d2, int iterations) {
        int d0 = 31;                                   // move.w #31,d0
        while ((d2 & (1 << (d0 & 31))) == 0) {         // .findhigh: btst d0,d2 ; bne .foundhigh
            d0 = setw(d0, d0 - 1);                     // dbra d0,.findhigh
            if ((short) d0 == -1) {
                break;
            }
        }
        // .foundhigh:
        d0 = setw(d0, ((short) d0) >> 1);              // asr.w #1,d0
        int d3 = 1 << (d0 & 31);                       // clr.l d3 ; bset d0,d3
        d0 = d3;                                       // move.l d3,d0
        for (int i = 0; i < iterations; i++) {
            int d1 = setw(0, d0);                      // move.w d0,d1
            d1 = muls(d1, d1);                         // muls d1,d1 ; x*x
            d1 -= d2;                                  // sub.l d2,d1 ; x*x-a
            d1 >>= 1;                                  // asr.l #1,d1 ; (x*x-a)/2
            d1 = divs(d1, d0);                         // divs d0,d1 ; (x*x-a)/2x
            d0 = setw(d0, d0 - d1);                    // sub.w d1,d0 ; second approx
            if ((short) d0 <= 0) {                     // bgt .stillnotN
                d0 = setw(d0, 1);                      // move.w #1,d0
            }
        }
        return d0;
    }

    /** HeadTowards */
    public static void HeadTowards() {
        int d1 = Mem.uw(newx);                         // move.w newx,d1
        d1 = setw(d1, d1 - Mem.uw(oldx));              // sub.w oldx,d1
        Mem.ww(xdiff, d1);                             // move.w d1,xdiff
        int d2 = Mem.uw(newz);                         // move.w newz,d2
        d2 = setw(d2, d2 - Mem.uw(oldz));              // sub.w oldz,d2
        Mem.ww(zdiff, d2);                             // move.w d2,zdiff
        d1 = muls(d1, d1);                             // muls d1,d1
        d2 = muls(d2, d2);                             // muls d2,d2
        Mem.ww(distaway, 0);                           // move.w #0,d0 ; move.w d0,distaway
        d2 += d1;                                      // add.l d1,d2
        if (d2 == 0) {                                 // beq nochange
            return;                                    // nochange: rts
        }

        int d0 = sqrtNewton(d2, 2);                    // .findhigh / .stillnot0 / .stillnot02
        Mem.ww(distaway, d0);                          // move.w d0,distaway

        // d0=perpdist
        Mem.wb(GotThere, ((short) d0 <= Mem.w(Range)) ? 0xFF : 0); // cmp.w Range,d0 ; sle GotThere
        if ((short) d0 <= Mem.w(Range)) {              // bgt.s faraway
            d1 = Mem.uw(xdiff);                        // move.w xdiff,d1
            d2 = Mem.uw(zdiff);                        // move.w zdiff,d2
            d1 = muls(d1, Mem.w(Range));               // muls Range,d1
            d2 = muls(d2, Mem.w(Range));               // muls Range,d2
            d1 = divs(d1, d0);                         // divs d0,d1
            d2 = divs(d2, d0);                         // divs d0,d2
            d1 = setw(d1, -(short) d1);                // neg.w d1
            d2 = setw(d2, -(short) d2);                // neg.w d2
            Mem.ww(newx, Mem.uw(newx) + d1);           // add.w d1,newx
            Mem.ww(newz, Mem.uw(newz) + d2);           // add.w d2,newz
            return;                                    // bra nochange
        }

        // faraway:
        int d3 = Mem.uw(speed);                        // move.w speed,d3
        d3 = setw(d3, d3 + Mem.uw(Range));             // add.w Range,d3
        if ((short) d3 >= (short) d0) {                // cmp.w d0,d3 ; blt.s .notoofast
            d3 = setw(d3, d0);                         // move.w d0,d3
            Mem.wb(GotThere, 0xFF);                    // st GotThere
        }
        // .notoofast:
        d3 = setw(d3, d3 - Mem.uw(Range));             // sub.w Range,d3
        d1 = Mem.uw(xdiff);                            // move.w xdiff,d1
        d1 = muls(d1, d3);                             // muls d3,d1
        d1 = divs(d1, d0);                             // divs d0,d1
        d2 = Mem.uw(zdiff);                            // move.w zdiff,d2
        d2 = muls(d2, d3);                             // muls d3,d2
        d2 = divs(d2, d0);                             // divs d0,d2
        d1 = setw(d1, d1 + Mem.uw(oldx));              // add.w oldx,d1
        Mem.ww(newx, d1);                              // move.w d1,newx
        d2 = setw(d2, d2 + Mem.uw(oldz));              // add.w oldz,d2
        Mem.ww(newz, d2);                              // move.w d2,newz
        // nochange: rts
    }

    /** CalcDist — distance (oldx,oldz)→(newx,newz) dans distaway. */
    public static void CalcDist() {
        int d1 = Mem.uw(newx);                         // move.w newx,d1
        d1 = setw(d1, d1 - Mem.uw(oldx));              // sub.w oldx,d1
        Mem.ww(xdiff, d1);                             // move.w d1,xdiff
        int d2 = Mem.uw(newz);                         // move.w newz,d2
        d2 = setw(d2, d2 - Mem.uw(oldz));              // sub.w oldz,d2
        Mem.ww(zdiff, d2);                             // move.w d2,zdiff
        d1 = muls(d1, d1);                             // muls d1,d1
        d2 = muls(d2, d2);                             // muls d2,d2
        Mem.ww(distaway, 0);                           // move.w #0,d0 ; move.w d0,distaway
        d2 += d1;                                      // add.l d1,d2
        if (d2 == 0) {                                 // beq .nochange
            return;
        }
        int d0 = sqrtNewton(d2, 2);
        Mem.ww(distaway, d0);                          // move.w d0,distaway
        // .nochange: rts
    }

    /** HeadTowardsAng */
    public static void HeadTowardsAng() {
        int d1 = Mem.uw(newx);                         // move.w newx,d1
        d1 = setw(d1, d1 - Mem.uw(oldx));              // sub.w oldx,d1
        Mem.ww(xdiff, d1);                             // move.w d1,xdiff
        int d2 = Mem.uw(newz);                         // move.w newz,d2
        d2 = setw(d2, d2 - Mem.uw(oldz));              // sub.w oldz,d2
        Mem.ww(zdiff, d2);                             // move.w d2,zdiff
        d1 = muls(d1, d1);                             // muls d1,d1
        d2 = muls(d2, d2);                             // muls d2,d2
        int d0 = 0;                                    // move.w #0,d0
        d2 += d1;                                      // add.l d1,d2
        Mem.wb(GotThere, (d2 == 0) ? 0xFF : 0);        // seq GotThere
        if (d2 != 0) {                                 // beq .nochange
            d0 = sqrtNewton(d2, 3);                    // .findhigh / .stillnot0 / 02 / 03

            // .stillnot03: d0=perpdist
            Mem.wb(GotThere, ((short) d0 <= Mem.w(Range)) ? 0xFF : 0); // cmp.w Range,d0 ; sle GotThere
            if ((short) d0 <= Mem.w(Range)) {          // bgt .faraway
                Mem.ww(newx, Mem.uw(oldx));            // move.w oldx,newx
                Mem.ww(newz, Mem.uw(oldz));            // move.w oldz,newz
                // bra .nochange [bloc 1091-1122 "Unreachable ?" (poussée canshove) omis]
            } else {
                // .faraway:
                int d3 = Mem.uw(speed);                // move.w speed,d3
                d3 = setw(d3, d3 + Mem.uw(Range));     // add.w Range,d3
                if ((short) d3 >= (short) d0) {        // cmp.w d0,d3 ; blt.s .notoofast
                    d3 = setw(d3, d0);                 // move.w d0,d3
                    Mem.wb(GotThere, 0xFF);            // st GotThere
                }
                // .notoofast:
                d3 = setw(d3, d3 - Mem.uw(Range));     // sub.w Range,d3
                d1 = Mem.uw(xdiff);                    // move.w xdiff,d1
                d1 = muls(d1, d3);                     // muls d3,d1
                d1 = divs(d1, d0);                     // divs d0,d1
                d2 = Mem.uw(zdiff);                    // move.w zdiff,d2
                d2 = muls(d2, d3);                     // muls d3,d2
                d2 = divs(d2, d0);                     // divs d0,d2
                d1 = setw(d1, d1 + Mem.uw(oldx));      // add.w oldx,d1
                Mem.ww(newx, d1);                      // move.w d1,newx
                d2 = setw(d2, d2 + Mem.uw(oldz));      // add.w oldz,d2
                Mem.ww(newz, d2);                      // move.w d2,newz
            }
        }

        // .nochange:
        if ((short) d0 == 0) {                         // tst.w d0 ; beq.s nocossin
            return;                                    // nocossin: rts
        }

        d0 = setw(d0, d0 + 1);                         // add.w #1,d0
        d1 = Mem.uw(xdiff);                            // move.w xdiff,d1
        d1 = swap(d1);                                 // swap d1
        d1 = d1 & 0xFFFF0000;                          // clr.w d1
        d1 >>= 1;                                      // asr.l #1,d1
        d1 = divs(d1, d0);                             // divs d0,d1
        Mem.ww(SinRet, d1);                            // move.w d1,SinRet
        d1 = Mem.uw(zdiff);                            // move.w zdiff,d1
        d1 = swap(d1);                                 // swap d1
        d1 = d1 & 0xFFFF0000;                          // clr.w d1
        d1 >>= 1;                                      // asr.l #1,d1
        d1 = divs(d1, d0);                             // divs d0,d1
        Mem.ww(CosRet, d1);                            // move.w d1,CosRet
        d0 = Mem.uw(SinRet);                           // move.w SinRet,d0
        int d2a = 0;                                   // move.w #0,d2
        int a2 = SinCosTable_vw;                       // move.l #SinCosTable_vw,a2
        int a3 = a2 + COSINE_OFS;                      // lea COSINE_OFS(a2),a3
        int d5 = 3;                                    // move.w #3,d5
        int d6 = COSINE_OFS;                           // move.w #COSINE_OFS,d6

        do { // findanglop:
            int d3 = Mem.w(a2 + ((short) d2a) * 2);    // move.w (a2,d2.w*2),d3
            int d4 = Mem.w(a3 + ((short) d2a) * 2);    // move.w (a3,d2.w*2),d4
            d4 = muls(d4, d0);                         // muls d0,d4
            d3 = muls(d3, d1);                         // muls d1,d3
            d4 -= d3;                                  // sub.l d3,d4
            if (d4 >= 0) {                             // blt.s subang
                d2a = setw(d2a, d2a + d6);             // add.w d6,d2
                d2a = setw(d2a, d2a + d6);             // add.w d6,d2
            }
            // subang:
            d2a = setw(d2a, d2a - d6);                 // sub.w d6,d2
            d2a = setw(d2a, d2a & 4095);               // and.w #4095,d2
            d6 = setw(d6, ((short) d6) >> 1);          // asr.w #1,d6
            d5 = setw(d5, d5 - 1);                     // dbra d5,findanglop
        } while ((short) d5 != -1);
        d2a = setw(d2a, d2a + d2a);                    // add.w d2,d2
        Mem.ww(AngRet, d2a);                           // move.w d2,AngRet
        // nocossin: rts
    }

    /** CheckHit — d2 = distance² limite ; hitwall = (dist parcourue)² < d2. */
    public static void CheckHit(int d2) {
        int d0 = Mem.uw(newx);                         // move.w newx,d0
        d0 = setw(d0, d0 - Mem.uw(oldx));              // sub.w oldx,d0
        int d1 = Mem.uw(newz);                         // move.w newz,d1
        d1 = setw(d1, d1 - Mem.uw(oldz));              // sub.w oldz,d1
        d1 = muls(d1, d1);                             // muls d1,d1
        d0 = muls(d0, d0);                             // muls d0,d0
        d1 += d0;                                      // add.l d0,d1
        Mem.wb(hitwall, (d1 < d2) ? 0xFF : 0);         // cmp.l d2,d1 ; slt hitwall
        // rts
    }

    /**
     * GetNextCPt — d0 = point de contrôle courant, d1 = cible ;
     * renvoie d0 = prochain point ($7f = aucun) ; positionne ONLYSEE.
     */
    public static int GetNextCPt(int d0, int d1) {
        Mem.wb(ONLYSEE, 0);                            // clr.b ONLYSEE
        if ((short) d0 == (short) d1) {                // cmp.w d0,d1 ; beq.s noneedforhassle
            return d0;                                 // noneedforhassle: rts
        }

        d0 = muls(d0, 100);                            // muls.w #100,d0
        d1 = (short) d1;                               // ext.l d1
        d0 += d1;                                      // add.l d1,d0
        // move.l a0,-(a7)
        int a0 = Mem.l(HiresData.Lvl_WalkLinksPtr_l);  // move.l Lvl_WalkLinksPtr_l,a0
        if (Mem.b(AI_FlyABit_w) != 0) {                // tst.b AI_FlyABit_w ; beq.s .walklink
            a0 = Mem.l(HiresData.Lvl_FlyLinksPtr_l);   // move.l Lvl_FlyLinksPtr_l,a0
        }
        // .walklink:
        d0 = setb(d0, Mem.ub(a0 + (short) d0));        // move.b (a0,d0.w),d0
        d1 = setb(d1, d0);                             // move.b d0,d1
        d0 = setb(d0, d0 & 0x7f);                      // and.b #$7f,d0
        d1 = setb(d1, d1 & 0x80);                      // and.b #$80,d1
        Mem.wb(ONLYSEE, ((byte) d1 != 0) ? 0xFF : 0);  // sne ONLYSEE
        d0 = setw(d0, (byte) d0);                      // ext.w d0
        // move.l (a7)+,a0
        return d0;                                     // noneedforhassle: rts
    }

    /**
     * CanItBeSeenAng — comme CanItBeSeen mais teste d'abord que la cible est
     * devant l'angle Facedir.
     */
    public static void CanItBeSeenAng() {
        // SAVEREGS
        int d0 = Mem.uw(Facedir);                      // move.w Facedir,d0
        int a0 = SinCosTable_vw;                       // move.l #SinCosTable_vw,a0
        a0 += (short) d0;                              // add.w d0,a0
        d0 = Mem.uw(a0);                               // move.w (a0),d0 ; SINE_OFS
        int d1 = Mem.uw(a0 + COSINE_OFS);              // move.w COSINE_OFS(a0),d1
        int d2 = Mem.uw(Targetx);                      // move.w Targetx,d2
        d2 = setw(d2, d2 - Mem.uw(Viewerx));           // sub.w Viewerx,d2
        int d3 = Mem.uw(Targetz);                      // move.w Targetz,d3
        d3 = setw(d3, d3 - Mem.uw(Viewerz));           // sub.w Viewerz,d3
        d2 = muls(d2, d1);                             // muls d1,d2
        d3 = muls(d3, d0);                             // muls d0,d3
        d2 -= d3;                                      // sub.l d3,d2
        if (d2 <= 0) {                                 // bgt.s ItMightBeSeen
            Mem.wb(CanSee, 0);                         // clr.b CanSee
            return;                                    // GETREGS ; rts
        }
        // ItMightBeSeen:
        int a0t = Mem.l(Obj_ToZonePtr_l);              // move.l Obj_ToZonePtr_l,a0
        d0 = Mem.uw(a0t);                              // move.w (a0),d0
        a0 = Mem.l(Obj_FromZonePtr_l);                 // move.l Obj_FromZonePtr_l,a0
        a0 += ZoneT_PotVisibleZoneList_vw;             // adda.w #ZoneT_PotVisibleZoneList_vw,a0
        inList(a0, d0);                                // bra.s InList
    }

    /** CanItBeSeen — ligne de vue Viewer*→Target* ; résultat dans CanSee. */
    public static void CanItBeSeen() {
        // SAVEREGS
        int a1 = Mem.l(Obj_ToZonePtr_l);               // move.l Obj_ToZonePtr_l,a1
        int d0 = Mem.uw(a1);                           // move.w (a1),d0
        int a0 = Mem.l(Obj_FromZonePtr_l);             // move.l Obj_FromZonePtr_l,a0
        if (a0 == a1) {                                // cmp.l a0,a1 ; beq.s insameroom
            // insameroom:
            Mem.wb(CanSee, 0xFF);                      // st CanSee
            int td0 = Mem.ub(ViewerTop);               // move.b ViewerTop,d0
            int td1 = Mem.ub(TargetTop);               // move.b TargetTop,d1
            if (((td0 ^ td1) & 0xFF) != 0) {           // eor.b d0,d1 ; bne outlist
                Mem.wb(CanSee, 0);                     // outlist: clr.b CanSee
            }
            return;                                    // GETREGS ; rts
        }

        a0 += ZoneT_PotVisibleZoneList_vw;             // adda.w #ZoneT_PotVisibleZoneList_vw,a0
        inList(a0, d0);
    }

    /** InList / isinlist / GoThroughZones — corps partagé par CanItBeSeen(Ang). */
    private static void inList(int a0, int d0) {
        int a1, d1, d2;
        while (true) { // InList:
            d1 = Mem.w(a0);                            // move.w (a0),d1 ; PVST_Zone_w
            if (d1 < 0) {                              // tst.w d1 ; blt outlist
                Mem.wb(CanSee, 0);                     // outlist: clr.b CanSee
                return;                                // GETREGS ; rts
            }
            a1 = Mem.l(Lvl_ZoneGraphAddsPtr_l);        // move.l Lvl_ZoneGraphAddsPtr_l,a1
            a1 = Mem.l(a1 + ((short) d1) * 8);         // move.l (a1,d1.w*8),a1
            a1 += Mem.l(Lvl_GraphicsPtr_l);            // add.l Lvl_GraphicsPtr_l,a1
            a0 += PVST_SizeOf_l;                       // adda.w #PVST_SizeOf_l,a0
            if (Mem.w(a1) == (short) d0) {             // cmp.w (a1),d0 ; beq isinlist
                break;
            }
        }                                              // bra.s InList

        // isinlist: we have found the dest room in the list of rooms visible
        // from the source room. Do line of sight!
        Mem.wb(CanSee, 0xFF);                          // st CanSee
        int a2 = Mem.l(Lvl_PointsPtr_l);               // move.l Lvl_PointsPtr_l,a2
        d1 = Mem.uw(Targetx);                          // move.w Targetx,d1
        d2 = Mem.uw(Targetz);                          // move.w Targetz,d2
        d1 = setw(d1, d1 - Mem.uw(Viewerx));           // sub.w Viewerx,d1
        d2 = setw(d2, d2 - Mem.uw(Viewerz));           // sub.w Viewerz,d2
        int d3 = 0;                                    // moveq #0,d3
        d3 = setw(d3, Mem.uw(a0 - 6));                 // move.w -6(a0),d3 (PVST_ClipID_w de l'entrée trouvée)
        if ((short) d3 >= 0) {                         // blt nomorerclips
            a1 = Mem.l(Lvl_ClipsPtr_l);                // move.l Lvl_ClipsPtr_l,a1
            a1 = a1 + d3 * 2;                          // lea (a1,d3.l*2),a1
            Mem.wl(clipstocheck, a1);                  // move.l a1,clipstocheck

            while (true) { // checklcliploop:
                if (Mem.w(a1) < 0) {                   // tst.w (a1) ; blt nomorelclips
                    break;
                }
                int dd0 = Mem.w(a1);                   // move.w (a1),d0
                if (dd0 >= 0) {                        // blt.s noleftone
                    d3 = Mem.l(a2 + ((short) dd0) * 4); // move.l (a2,d0.w*4),d3
                    int d4 = (short) d3;               // move.w d3,d4
                    d4 = setw(d4, d4 - Mem.uw(Viewerz)); // sub.w Viewerz,d4
                    d3 = swap(d3);                     // swap d3
                    d3 = setw(d3, d3 - Mem.uw(Viewerx)); // sub.w Viewerx,d3
                    d3 = muls(d3, d2);                 // muls d2,d3
                    d4 = muls(d4, d1);                 // muls d1,d4
                    d4 -= d3;                          // sub.l d3,d4
                    if (d4 <= 0) {                     // ble outlist
                        Mem.wb(CanSee, 0);
                        return;
                    }
                }
                // noleftone:
                a1 += 2;                               // addq #2,a1
            }                                          // bra checklcliploop

            // nomorelclips:
            a1 += 2;                                   // addq #2,a1

            while (true) { // checkrcliploop:
                if (Mem.w(a1) < 0) {                   // tst.w (a1) ; blt nomorerclips
                    break;
                }
                int dd0 = Mem.w(a1);                   // move.w (a1),d0
                if (dd0 >= 0) {                        // blt.s norightone
                    d3 = Mem.l(a2 + ((short) dd0) * 4); // move.l (a2,d0.w*4),d3
                    int d4 = (short) d3;               // move.w d3,d4
                    d4 = setw(d4, d4 - Mem.uw(Viewerz)); // sub.w Viewerz,d4
                    d3 = swap(d3);                     // swap d3
                    d3 = setw(d3, d3 - Mem.uw(Viewerx)); // sub.w Viewerx,d3
                    d3 = muls(d3, d2);                 // muls d2,d3
                    d4 = muls(d4, d1);                 // muls d1,d4
                    d4 -= d3;                          // sub.l d3,d4
                    if (d4 >= 0) {                     // bge outlist
                        Mem.wb(CanSee, 0);
                        return;
                    }
                }
                // norightone:
                a1 += 2;                               // addq #2,a1
            }                                          // bra checkrcliploop
        }

        // nomorerclips: no clipping points in the way; vertical working out now.
        int d0v = Mem.uw(Targetx);                     // move.w Targetx,d0
        int d1v = Mem.uw(Targetz);                     // move.w Targetz,d1
        d0v = setw(d0v, d0v - Mem.uw(Viewerx));        // sub.w Viewerx,d0
        d1v = setw(d1v, d1v - Mem.uw(Viewerz));        // sub.w Viewerz,d1
        int a5 = Mem.l(Obj_FromZonePtr_l);             // move.l Obj_FromZonePtr_l,a5
        a1 = Mem.l(Lvl_ZoneEdgePtr_l);                 // move.l Lvl_ZoneEdgePtr_l,a1
        d2 = Mem.ub(ViewerTop);                        // move.b ViewerTop,d2
        int d7 = Mem.uw(Targety);                      // move.w Targety,d7
        d7 = setw(d7, d7 - Mem.uw(Viewery));           // sub.w Viewery,d7

        GoThroughZones:
        while (true) { // GoThroughZones:
            int a0e = a5 + Mem.w(a5 + ZoneT_EdgeListOffset_w); // move.l a5,a0 ; adda.w ZoneT_EdgeListOffset_w(a0),a0

            while (true) { // FindWayOut:
                int d5 = Mem.w(a0e); a0e += 2;         // move.w (a0)+,d5
                if (d5 < 0) {                          // blt outlist
                    Mem.wb(CanSee, 0);
                    return;
                }
                d5 = setw(d5, d5 << 4);                // asl.w #4,d5
                a2 = a1 + (short) d5;                  // lea (a1,d5.w),a2
                d3 = Mem.uw(a2);                       // move.w (a2),d3 ; EdgeT_XPos_w
                int d4 = Mem.uw(a2 + EdgeT_ZPos_w);    // move.w EdgeT_ZPos_w(a2),d4
                d3 = setw(d3, d3 - Mem.uw(Viewerx));   // sub.w Viewerx,d3
                d4 = setw(d4, d4 - Mem.uw(Viewerz));   // sub.w Viewerz,d4
                d5 = setw(d5, d3);                     // move.w d3,d5
                int d6 = setw(0, d4);                  // move.w d4,d6
                d3 = muls(d3, d1v);                    // muls d1,d3
                d4 = muls(d4, d0v);                    // muls d0,d4
                d4 -= d3;                              // sub.l d3,d4
                if (d4 <= 0) {                         // ble FindWayOut
                    continue;
                }

                d5 = setw(d5, d5 + Mem.uw(a2 + EdgeT_XLen_w)); // add.w EdgeT_XLen_w(a2),d5
                d6 = setw(d6, d6 + Mem.uw(a2 + EdgeT_ZLen_w)); // add.w EdgeT_ZLen_w(a2),d6
                d6 = muls(d6, d0v);                    // muls d0,d6
                d5 = muls(d5, d1v);                    // muls d1,d5
                d6 -= d5;                              // sub.l d5,d6
                if (d6 >= 0) {                         // bge FindWayOut
                    continue;
                }

                if (Mem.w(a2 + EdgeT_JoinZone_w) < 0) { // tst.w EdgeT_JoinZone_w(a2) ; blt outlist
                    Mem.wb(CanSee, 0);
                    return;
                }

                // Here is the exit from the room. Calculate the height at which we meet it.
                d3 = Mem.uw(Targetx);                  // move.w Targetx,d3
                d4 = Mem.uw(Targetz);                  // move.w Targetz,d4
                d3 = setw(d3, d3 - Mem.uw(a2));        // sub.w (a2),d3
                d4 = setw(d4, d4 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d4
                d4 = muls(d4, Mem.w(a2 + EdgeT_XLen_w)); // muls EdgeT_XLen_w(a2),d4
                d3 = muls(d3, Mem.w(a2 + EdgeT_ZLen_w)); // muls EdgeT_ZLen_w(a2),d3
                d4 -= d3;                              // sub.l d3,d4 ; positive
                d5 = Mem.uw(Viewerx);                  // move.w Viewerx,d5
                d6 = Mem.uw(Viewerz);                  // move.w Viewerz,d6
                d5 = setw(d5, d5 - Mem.uw(a2));        // sub.w (a2),d5
                d6 = setw(d6, d6 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d6
                d6 = muls(d6, Mem.w(a2 + EdgeT_XLen_w)); // muls EdgeT_XLen_w(a2),d6
                d5 = muls(d5, Mem.w(a2 + EdgeT_ZLen_w)); // muls EdgeT_ZLen_w(a2),d5
                d5 -= d6;                              // sub.l d6,d5 ; positive
                d4 = divs(d4, Mem.w(a2 + EdgeT_Word_5)); // divs EdgeT_Word_5(a2),d4
                d5 = divs(d5, Mem.w(a2 + EdgeT_Word_5)); // divs EdgeT_Word_5(a2),d5
                d4 = setw(d4, d4 + d5);                // add.w d5,d4
                if ((short) d4 != 0) {                 // beq.s sameheight
                    d5 = muls(d5, d7);                 // muls d7,d5
                    d5 = divs(d5, d4);                 // divs d4,d5
                }

                // sameheight:
                d5 = setw(d5, d5 + Mem.uw(Viewery));   // add.w Viewery,d5 ; height at which we cross wall
                d5 = (short) d5;                       // ext.l d5
                d5 <<= 7;                              // asl.l #7,d5
                if ((byte) d2 != 0) {                  // tst.b d2 ; beq.s comparewithbottom
                    if (d5 < Mem.l(a5 + ZoneT_UpperRoof_l)) { // cmp.l ZoneT_UpperRoof_l(a5),d5 ; blt outlist
                        Mem.wb(CanSee, 0);
                        return;
                    }
                    if (d5 > Mem.l(a5 + ZoneT_UpperFloor_l)) { // cmp.l ZoneT_UpperFloor_l(a5),d5 ; bgt outlist
                        Mem.wb(CanSee, 0);
                        return;
                    }
                    // bra.s madeit
                } else {
                    // comparewithbottom:
                    if (d5 < Mem.l(a5 + ZoneT_Roof_l)) { // cmp.l ZoneT_Roof_l(a5),d5 ; blt outlist
                        Mem.wb(CanSee, 0);
                        return;
                    }
                    if (d5 > Mem.l(a5 + ZoneT_Floor_l)) { // cmp.l ZoneT_Floor_l(a5),d5 ; bgt outlist
                        Mem.wb(CanSee, 0);
                        return;
                    }
                }

                // madeit:
                Mem.wb(donessomething, 0xFF);          // st donessomething
                d3 = 0;                                // moveq #0,d3
                d3 = setw(d3, Mem.uw(a2 + EdgeT_JoinZone_w)); // move.w EdgeT_JoinZone_w(a2),d3
                int a3 = Mem.l(Lvl_ZonePtrsPtr_l);     // move.l Lvl_ZonePtrsPtr_l,a3
                a5 = Mem.l(a3 + ((short) d3) * 4);     // move.l (a3,d3.w*4),a5
                d2 = setb(d2, 0);                      // clr.b d2
                if (d5 > Mem.l(a5 + ZoneT_Floor_l)) {  // cmp.l ZoneT_Floor_l(a5),d5 ; bgt outlist
                    Mem.wb(CanSee, 0);
                    return;
                }
                if (d5 <= Mem.l(a5 + ZoneT_Roof_l)) {  // cmp.l ZoneT_Roof_l(a5),d5 ; bgt.s GotIn
                    d2 = setb(d2, 0xFF);               // st d2
                    if (d5 > Mem.l(a5 + ZoneT_UpperFloor_l)) { // cmp.l ZoneT_UpperFloor_l(a5),d5 ; bgt outlist
                        Mem.wb(CanSee, 0);
                        return;
                    }
                    if (d5 < Mem.l(a5 + ZoneT_UpperRoof_l)) { // cmp.l ZoneT_UpperRoof_l(a5),d5 ; blt outlist
                        Mem.wb(CanSee, 0);
                        return;
                    }
                }

                // GotIn:
                if (a5 != Mem.l(Obj_ToZonePtr_l)) {    // cmp.l Obj_ToZonePtr_l,a5 ; bne GoThroughZones
                    continue GoThroughZones;
                }

                int td3 = Mem.ub(TargetTop);           // move.b TargetTop,d3
                if (((td3 ^ d2) & 0xFF) != 0) {        // eor.b d2,d3 ; bne outlist
                    Mem.wb(CanSee, 0);
                    return;
                }
                return;                                // GETREGS ; rts (CanSee reste st)
            }
        }
    }

    /** GetRand — générateur pseudo-aléatoire ; renvoie d0 (mot, 0..65535). */
    public static int GetRand() {
        int d0 = Mem.uw(Rand1);                        // move.w Rand1,d0
        d0 = ((d0 << 3) | (d0 >>> 13)) & 0xFFFF;       // rol.w #3,d0
        d0 = (d0 + 0x2343) & 0xFFFF;                   // add.w #$2343,d0
        Mem.ww(Rand1, d0);                             // move.w d0,Rand1
        return d0;                                     // rts
    }

    /** GoInDirection — d0 = angle (offset adresse) ; avance (oldx,oldz) de `speed` vers cet angle. */
    public static void GoInDirection(int d0) {
        int a0 = SinCosTable_vw;                       // move.l #SinCosTable_vw,a0
        a0 += (short) d0;                              // lea (a0,d0.w),a0
        int d1 = Mem.w(a0);                            // move.w (a0),d1
        int d2 = Mem.w(a0 + COSINE_OFS);               // move.w COSINE_OFS(a0),d2
        d1 = muls(d1, Mem.w(speed));                   // muls speed,d1
        d1 += d1;                                      // add.l d1,d1
        d2 = muls(d2, Mem.w(speed));                   // muls speed,d2
        d2 += d2;                                      // add.l d2,d2
        d1 = swap(d1);                                 // swap d1
        d2 = swap(d2);                                 // swap d2
        d1 = setw(d1, d1 + Mem.uw(oldx));              // add.w oldx,d1
        d2 = setw(d2, d2 + Mem.uw(oldz));              // add.w oldz,d2
        Mem.ww(newx, d1);                              // move.w d1,newx
        Mem.ww(newz, d2);                              // move.w d2,newz
        // rts
    }

    /**
     * Obj_DoCollision — collision contre les objets de la zone CollId.
     * a2 = registre hérité de l'appelant (cf. javadoc de classe) : les
     * extents Y sont lus à 2(a2,TypeID*8) / 4(a2,TypeID*8).
     */
    public static void Obj_DoCollision(int a2) {
        int a0 = Mem.l(Lvl_ObjectDataPtr_l);           // move.l Lvl_ObjectDataPtr_l,a0
        int d0 = Mem.uw(HiresData.CollId);             // move.w CollId,d0
        d0 = setw(d0, d0 << 6);                        // asl.w #6,d0
        Mem.ww(tmp_zone_id_w, Mem.uw(a0 + (short) d0 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a0,d0.w),.tmp_zone_id_w
        d0 = Mem.b(a0 + (short) d0 + ObjT_TypeID_b);   // move.b ObjT_TypeID_b(a0,d0.w),d0 ; ext.w d0 (valeur morte)

        a0 -= ObjT_SizeOf_l;                           // PREV_OBJ a0

        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        int d7 = Mem.l(Obj_CollideFlags_l);            // move.l Obj_CollideFlags_l,d7 (utilisé seulement par le btst commenté)
        int d6 = Mem.ub(StoodInTop);                   // move.b StoodInTop,d6
        int d4 = Mem.l(newy);                          // move.l newy,d4
        int d5 = d4;                                   // move.l d4,d5
        d5 += Mem.l(ObjectmoveData.thingheight);       // add.l thingheight,d5
        d4 >>= 7;                                      // asr.l #7,d4
        d5 >>= 7;                                      // asr.l #7,d5
        Mem.wb(hitwall, 0);                            // clr.b hitwall

        int d1, d2, d3;
        while (true) { // .check_collide:
            a0 += ObjT_SizeOf_l;                       // NEXT_OBJ a0
            d0 = Mem.w(a0);                            // move.w (a0),d0
            if (d0 < 0) {                              // blt .checked_all_collide
                return;                                // .checked_all_collide: rts
            }

            if ((short) d0 == Mem.w(HiresData.CollId)) { // cmp.w CollId,d0 ; beq.s .check_collide
                continue;
            }
            if (Mem.w(a0 + ObjT_ZoneID_w) < 0) {       // tst.w ObjT_ZoneID_w(a0) ; blt.s .check_collide
                continue;
            }
            d1 = Mem.uw(tmp_zone_id_w);                // move.w .tmp_zone_id_w,d1
            if ((short) d1 != Mem.w(a0 + ObjT_ZoneID_w)) { // cmp.w ObjT_ZoneID_w(a0),d1 ; bne.s .check_collide
                continue;
            }
            if (Mem.b(a0 + EntT_HitPoints_b) == 0) {   // tst.b EntT_HitPoints_b(a0) ; beq.s .check_collide
                continue;
            }
            d1 = Mem.ub(a0 + ShotT_InUpperZone_b);     // move.b ShotT_InUpperZone_b(a0),d1
            if (((d1 ^ d6) & 0xFF) != 0) {             // eor.b d6,d1 ; bne .check_collide
                continue;
            }

            d3 = 0;                                    // moveq #0,d3
            d3 = setb(d3, Mem.ub(a0 + ObjT_TypeID_b)); // move.b ObjT_TypeID_b(a0),d3
            if ((byte) d3 < 0) {                       // blt .check_collide
                continue;
            }
            if ((byte) d3 != 0) {                      // beq .ycol
                if ((byte) d3 != 1) {                  // cmp.b #1,d3 ; bne .check_collide
                    continue;
                }
                int a4 = Mem.l(HiresData.GLF_DatabasePtr_l); // move.l GLF_DatabasePtr_l,a4
                a4 += GLFT_ObjectDefs;                 // add.l #GLFT_ObjectDefs,a4
                d1 = 0;                                // moveq #0,d1
                d1 = setb(d1, Mem.ub(a0 + EntT_Type_b)); // move.b EntT_Type_b(a0),d1
                d1 = muls(d1, ODefT_SizeOf_l);         // muls #ODefT_SizeOf_l,d1
                int beh = Mem.w(a4 + (short) d1 + ODefT_Behaviour_w); // cmp.w #2,ODefT_Behaviour_w(a4,d1.w)
                if (beh < 2) {                         // blt .check_collide
                    continue;
                }
                if (beh == 2) {                        // bgt .ycol
                    if ((byte) Mem.b(a0 + EntT_HitPoints_b) <= 0) { // tst.b EntT_HitPoints_b(a0) ; ble .check_collide
                        continue;
                    }
                }
            }

            // .ycol:
            d1 = Mem.uw(a0 + ObjT_ZPos_l);             // move.w ObjT_ZPos_l(a0),d1
            d1 = setw(d1, d1 - Mem.uw(a2 + ((short) d3) * 8 + 2)); // sub.w 2(a2,d3.w*8),d1
            if ((short) d5 < (short) d1) {             // cmp.w d1,d5 ; blt .check_collide
                continue;
            }
            d1 = setw(d1, d1 + Mem.uw(a2 + ((short) d3) * 8 + 4)); // add.w 4(a2,d3.w*8),d1
            if ((short) d4 > (short) d1) {             // cmp.w d1,d4 ; bgt .check_collide
                continue;
            }

            d1 = Mem.uw(a1 + ((short) d0) * 8);        // move.w (a1,d0.w*8),d1
            d2 = Mem.uw(a1 + ((short) d0) * 8 + 4);    // move.w 4(a1,d0.w*8),d2
            d1 = setw(d1, d1 - Mem.uw(newx));          // sub.w newx,d1
            if ((short) d1 < 0) {                      // bge.s .xnoneg
                d1 = setw(d1, -(short) d1);            // neg.w d1
            }
            // .xnoneg:
            d2 = setw(d2, d2 - Mem.uw(newz));          // sub.w newz,d2
            if ((short) d2 < 0) {                      // bge.s .znoneg
                d2 = setw(d2, -(short) d2);            // neg.w d2
            }
            // .znoneg:
            if ((short) d2 > (short) d1) {             // cmp.w d1,d2 ; ble.s .checkx
                d2 = setw(d2, d2 - 80);                // sub.w #80,d2
                if ((short) d2 > 80) {                 // cmp.w #80,d2 ; bgt .check_collide
                    continue;
                }
                Mem.wb(hitwall, 0xFF);                 // st hitwall
                return;                                // bra .checked_all_collide
            }
            // .checkx:
            d1 = setw(d1, d1 - 80);                    // sub.w #80,d1
            if ((short) d1 > 80) {                     // cmp.w #80,d1 ; bgt .check_collide
                continue;
            }

            d1 = Mem.uw(a1 + ((short) d0) * 8);        // move.w (a1,d0.w*8),d1
            d2 = Mem.uw(a1 + ((short) d0) * 8 + 4);    // move.w 4(a1,d0.w*8),d2
            d6 = setw(d6, d1);                         // move.w d1,d6
            d7 = setw(d7, d2);                         // move.w d2,d7
            d6 = setw(d6, d6 - Mem.uw(newx));          // sub.w newx,d6
            d7 = setw(d7, d7 - Mem.uw(newz));          // sub.w newz,d7
            d6 = muls(d6, d6);                         // muls d6,d6
            d7 = muls(d7, d7);                         // muls d7,d7
            d7 += d6;                                  // add.l d6,d7
            d1 = setw(d1, d1 - Mem.uw(oldx));          // sub.w oldx,d1
            d2 = setw(d2, d2 - Mem.uw(oldz));          // sub.w oldz,d2
            d1 = muls(d1, d1);                         // muls d1,d1
            d2 = muls(d2, d2);                         // muls d2,d2
            d2 += d1;                                  // add.l d1,d2
            if (d7 > d2) {                             // cmp.l d2,d7 ; bgt .check_collide
                continue;                              // NB : d6/d7 restent écrasés pour les tours suivants (comme l'original)
            }

            Mem.wb(hitwall, 0xFF);                     // st hitwall
            return;                                    // (chute dans .checked_all_collide)
        }
    }

    /**
     * CheckTeleport — téléporte via la zone FromZone si elle a un TelZone.
     * Renvoie la valeur de SORTIE du registre a2 (pointeur de zone), héritée
     * par les Obj_DoCollision suivants des routines AI.
     */
    public static int CheckTeleport() {
        Mem.wb(OKTEL, 0);                              // clr.b OKTEL
        int d0 = Mem.uw(FromZone);                     // move.w FromZone,d0
        int a2 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a2
        a2 = Mem.l(a2 + ((short) d0) * 4);             // move.l (a2,d0.w*4),a2
        if (Mem.w(a2 + ZoneT_TelZone_w) < 0) {         // tst.w ZoneT_TelZone_w(a2) ; bge.s ITSATEL
            return a2;                                 // rts
        }

        // ITSATEL:
        Mem.wl(floortemp, Mem.l(a2 + ZoneT_Floor_l));  // move.l ZoneT_Floor_l(a2),floortemp
        d0 = Mem.uw(a2 + ZoneT_TelZone_w);             // move.w ZoneT_TelZone_w(a2),d0
        int a3 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a3
        a3 = Mem.l(a3 + ((short) d0) * 4);             // move.l (a3,d0.w*4),a3
        d0 = Mem.l(a3 + ZoneT_Floor_l);                // move.l ZoneT_Floor_l(a3),d0
        d0 -= Mem.l(floortemp);                        // sub.l floortemp,d0
        Mem.wl(floortemp, d0);                         // move.l d0,floortemp
        Mem.wl(newy, Mem.l(newy) + d0);                // add.l d0,newy
        Mem.ww(newx, Mem.uw(a2 + ZoneT_TelX_w));       // move.w ZoneT_TelX_w(a2),newx
        Mem.ww(newz, Mem.uw(a2 + ZoneT_TelZ_w));       // move.w ZoneT_TelZ_w(a2),newz
        Mem.wl(Obj_CollideFlags_l, 0b1111111111111111111); // move.l #%1111111111111111111,Obj_CollideFlags_l
        // movem.l a0/a1/a2,-(a7)
        Obj_DoCollision(a2);                           // bsr Obj_DoCollision (a2 = zone de départ)
        // movem.l (a7)+,a0/a1/a2
        d0 = Mem.l(floortemp);                         // move.l floortemp,d0
        Mem.wl(newy, Mem.l(newy) - d0);                // sub.l d0,newy
        boolean hit = Mem.b(hitwall) != 0;             // tst.b hitwall
        Mem.wb(OKTEL, hit ? 0 : 0xFF);                 // seq OKTEL
        if (hit) {                                     // beq.s .teleport
            return a2;                                 // rts
        }

        // .teleport:
        d0 = Mem.uw(a2 + ZoneT_TelZone_w);             // move.w ZoneT_TelZone_w(a2),d0
        a2 = Mem.l(Lvl_ZonePtrsPtr_l);                 // move.l Lvl_ZonePtrsPtr_l,a2
        a2 = Mem.l(a2 + ((short) d0) * 4);             // move.l (a2,d0.w*4),a2
        Mem.wl(Obj_ZonePtr_l, a2);                     // move.l a2,Obj_ZonePtr_l
        return a2;                                     // rts
    }

    /** FindCollisionPt — comme la passe verticale de CanItBeSeen, sans test PVS. */
    public static void FindCollisionPt() {
        // SAVEREGS
        int d0 = Mem.uw(Targetx);                      // move.w Targetx,d0
        int d1 = Mem.uw(Targetz);                      // move.w Targetz,d1
        d0 = setw(d0, d0 - Mem.uw(Viewerx));           // sub.w Viewerx,d0
        d1 = setw(d1, d1 - Mem.uw(Viewerz));           // sub.w Viewerz,d1
        int a5 = Mem.l(Obj_FromZonePtr_l);             // move.l Obj_FromZonePtr_l,a5
        int a1 = Mem.l(Lvl_ZoneEdgePtr_l);             // move.l Lvl_ZoneEdgePtr_l,a1
        int d2 = Mem.ub(ViewerTop);                    // move.b ViewerTop,d2
        int d7 = Mem.uw(Targety);                      // move.w Targety,d7
        d7 = setw(d7, d7 - Mem.uw(Viewery));           // sub.w Viewery,d7

        GoThroughZones:
        while (true) { // .GoThroughZones:
            int a0 = a5 + Mem.w(a5 + ZoneT_EdgeListOffset_w); // move.l a5,a0 ; adda.w ...

            while (true) { // .FindWayOut:
                int d5 = Mem.w(a0); a0 += 2;           // move.w (a0)+,d5
                if (d5 < 0) {                          // blt outlist
                    Mem.wb(CanSee, 0);                 // outlist: clr.b CanSee
                    return;                            // GETREGS ; rts
                }
                d5 = setw(d5, d5 << 4);                // asl.w #4,d5
                int a2 = a1 + (short) d5;              // lea (a1,d5.w),a2
                int d3 = Mem.uw(a2);                   // move.w (a2),d3
                int d4 = Mem.uw(a2 + EdgeT_ZPos_w);    // move.w EdgeT_ZPos_w(a2),d4
                d3 = setw(d3, d3 - Mem.uw(Viewerx));   // sub.w Viewerx,d3
                d4 = setw(d4, d4 - Mem.uw(Viewerz));   // sub.w Viewerz,d4
                d5 = setw(d5, d3);                     // move.w d3,d5
                int d6 = setw(0, d4);                  // move.w d4,d6
                d3 = muls(d3, d1);                     // muls d1,d3
                d4 = muls(d4, d0);                     // muls d0,d4
                d4 -= d3;                              // sub.l d3,d4
                if (d4 <= 0) {                         // ble .FindWayOut
                    continue;
                }

                d5 = setw(d5, d5 + Mem.uw(a2 + EdgeT_XLen_w)); // add.w EdgeT_XLen_w(a2),d5
                d6 = setw(d6, d6 + Mem.uw(a2 + EdgeT_ZLen_w)); // add.w EdgeT_ZLen_w(a2),d6
                d6 = muls(d6, d0);                     // muls d0,d6
                d5 = muls(d5, d1);                     // muls d1,d5
                d6 -= d5;                              // sub.l d5,d6
                if (d6 >= 0) {                         // bge .FindWayOut
                    continue;
                }

                // Here is the exit from the room. Calculate the height at which we meet it.
                d3 = Mem.uw(Targetx);                  // move.w Targetx,d3
                d4 = Mem.uw(Targetz);                  // move.w Targetz,d4
                d3 = setw(d3, d3 - Mem.uw(a2));        // sub.w (a2),d3
                d4 = setw(d4, d4 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d4
                d4 = muls(d4, Mem.w(a2 + EdgeT_XLen_w)); // muls EdgeT_XLen_w(a2),d4
                d3 = muls(d3, Mem.w(a2 + EdgeT_ZLen_w)); // muls EdgeT_ZLen_w(a2),d3
                d4 -= d3;                              // sub.l d3,d4 ; positive
                d5 = Mem.uw(Viewerx);                  // move.w Viewerx,d5
                d6 = Mem.uw(Viewerz);                  // move.w Viewerz,d6
                d5 = setw(d5, d5 - Mem.uw(a2));        // sub.w (a2),d5
                d6 = setw(d6, d6 - Mem.uw(a2 + EdgeT_ZPos_w)); // sub.w EdgeT_ZPos_w(a2),d6
                d6 = muls(d6, Mem.w(a2 + EdgeT_XLen_w)); // muls EdgeT_XLen_w(a2),d6
                d5 = muls(d5, Mem.w(a2 + EdgeT_ZLen_w)); // muls EdgeT_ZLen_w(a2),d5
                d5 -= d6;                              // sub.l d6,d5 ; positive
                d4 = divs(d4, Mem.w(a2 + EdgeT_Word_5)); // divs EdgeT_Word_5(a2),d4
                d5 = divs(d5, Mem.w(a2 + EdgeT_Word_5)); // divs EdgeT_Word_5(a2),d5
                d6 = setw(d6, d5);                     // move.w d5,d6
                d4 = setw(d4, d4 + d5);                // add.w d5,d4
                if ((short) d4 != 0) {                 // beq.s .sameheight
                    d5 = muls(d5, d7);                 // muls d7,d5
                    d5 = divs(d5, d4);                 // divs d4,d5
                }

                // .sameheight:
                d5 = setw(d5, d5 + Mem.uw(Viewery));   // add.w Viewery,d5 ; height at which we cross wall
                d5 = (short) d5;                       // ext.l d5
                d5 <<= 7;                              // asl.l #7,d5
                d3 = 0;                                // moveq #0,d3
                d3 = setw(d3, Mem.uw(a2 + EdgeT_JoinZone_w)); // move.w EdgeT_JoinZone_w(a2),d3
                if ((short) d3 < 0) {                  // blt foundpt
                    return;                            // foundpt: GETREGS ; rts
                }

                int a3 = Mem.l(Lvl_ZonePtrsPtr_l);     // move.l Lvl_ZonePtrsPtr_l,a3
                a5 = Mem.l(a3 + ((short) d3) * 4);     // move.l (a3,d3.w*4),a5
                d2 = setb(d2, 0);                      // clr.b d2
                if (d5 > Mem.l(a5 + ZoneT_Floor_l)) {  // cmp.l ZoneT_Floor_l(a5),d5 ; bgt foundpt
                    return;
                }
                if (d5 <= Mem.l(a5 + ZoneT_Roof_l)) {  // cmp.l ZoneT_Roof_l(a5),d5 ; bgt.s .GotIn
                    d2 = setb(d2, 0xFF);               // st d2
                    if (d5 > Mem.l(a5 + ZoneT_UpperFloor_l)) { // cmp.l ZoneT_UpperFloor_l(a5),d5 ; bgt foundpt
                        return;
                    }
                    if (d5 < Mem.l(a5 + ZoneT_UpperRoof_l)) { // cmp.l ZoneT_UpperRoof_l(a5),d5 ; blt foundpt
                        return;
                    }
                }
                // .GotIn: bra .GoThroughZones [bloc 1619-1630 "Unreachable ?" omis]
                continue GoThroughZones;
            }
        }
    }

    /** FindCloseRoom — d0 = distance, a0 = entité ; recale ObjT_ZoneID_w sur la zone la plus proche. */
    public static void FindCloseRoom(int d0, int a0) {
        int d1 = Mem.uw(a0 + ObjT_ZPos_l);             // move.w ObjT_ZPos_l(a0),d1 ; ext.l d1
        d1 = (short) d1;
        d1 <<= 7;                                      // asl.l #7,d1
        Mem.wl(oldy, d1);                              // move.l d1,oldy
        Mem.wl(newy, d1);                              // move.l d1,newy
        d1 = Mem.uw(a0);                               // move.w (a0),d1
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);         // move.l Lvl_ObjectPointsPtr_l,a1
        a1 = a1 + ((short) d1) * 8;                    // lea (a1,d1.w*8),a1
        Mem.ww(oldx, Mem.uw(a1));                      // move.w (a1),oldx
        Mem.ww(oldz, Mem.uw(a1 + ObjT_ZPos_l));        // move.w ObjT_ZPos_l(a1),oldz
        int d2 = Mem.uw(a0 + ObjT_ZoneID_w);           // move.w ObjT_ZoneID_w(a0),d2
        int a5 = Mem.l(Lvl_ZonePtrsPtr_l);             // move.l Lvl_ZonePtrsPtr_l,a5
        d2 = Mem.l(a5 + ((short) d2) * 4);             // move.l (a5,d2.w*4),d2
        Mem.wl(Obj_ZonePtr_l, d2);                     // move.l d2,Obj_ZonePtr_l
        Mem.ww(newx, Mem.uw(NewaliencontrolData.THISPLRxoff)); // move.w THISPLRxoff,newx
        Mem.ww(newz, Mem.uw(NewaliencontrolData.THISPLRzoff)); // move.w THISPLRzoff,newz
        Mem.ww(speed, d0);                             // move.w d0,speed
        // movem.l a0/a1,-(a7)
        HeadTowards();                                 // jsr HeadTowards
        // movem.l (a7)+,a0/a1
        d0 = Mem.uw(newx);                             // move.w newx,d0
        d0 = setw(d0, d0 - Mem.uw(oldx));              // sub.w oldx,d0
        d1 = Mem.uw(oldz);                             // move.w oldz,d1
        d1 = setw(d1, d1 - Mem.uw(newz));              // sub.w newz,d1
        Mem.ww(xd, d1);                                // move.w d1,xd
        Mem.ww(zd, d0);                                // move.w d0,zd
        Mem.wl(StepUpVal, 100000);                     // move.l #100000,StepUpVal
        Mem.wl(StepDownVal, 100000);                   // move.l #100000,StepDownVal
        Mem.ww(ObjectmoveData.thingheight, 0);         // move.w #0,thingheight (mot fort du long !)
        Mem.wb(exitfirst, 0xFF);                       // st exitfirst
        d1 = setw(d1, d1 + Mem.uw(oldx));              // add.w oldx,d1
        d0 = setw(d0, d0 + Mem.uw(oldz));              // add.w oldz,d0
        Mem.ww(newx, d1);                              // move.w d1,newx
        Mem.ww(newz, d0);                              // move.w d0,newz

        // SAVEREGS
        Mem.wb(Obj_WallBounce_b, 0);                   // clr.b Obj_WallBounce_b
        MoveObject();                                  // jsr MoveObject
        // GETREGS

        int a2 = Obj_RoomPath_vw;                      // move.l #Obj_RoomPath_vw,a2
        int a3 = possclose;                            // move.l #possclose,a3
        Mem.ww(a3, Mem.uw(a0 + ObjT_ZoneID_w)); a3 += 2; // move.w ObjT_ZoneID_w(a0),(a3)+

        int v;
        do { // putinmore:
            v = Mem.w(a2); a2 += 2;                    // move.w (a2)+,(a3)+
            Mem.ww(a3, v); a3 += 2;
        } while (v >= 0);                              // bge.s putinmore

        a3 -= 2;                                       // subq #2,a3
        d0 = Mem.uw(oldx);                             // move.w oldx,d0
        d0 = setw(d0, d0 - Mem.uw(xd));                // sub.w xd,d0
        d1 = Mem.uw(oldz);                             // move.w oldz,d1
        d1 = setw(d1, d1 - Mem.uw(zd));                // sub.w zd,d1
        Mem.ww(newx, d0);                              // move.w d0,newx
        Mem.ww(newz, d1);                              // move.w d1,newz

        // SAVEREGS
        Mem.wb(Obj_WallBounce_b, 0);                   // clr.b Obj_WallBounce_b
        MoveObject();                                  // jsr MoveObject
        // GETREGS

        a2 = Obj_RoomPath_vw;                          // move.l #Obj_RoomPath_vw,a2

        do { // putinmore2:
            v = Mem.w(a2); a2 += 2;                    // move.w (a2)+,d0
            Mem.ww(a3, v); a3 += 2;                    // move.w d0,(a3)+
        } while (v >= 0);                              // tst.w d0 ; bge.s putinmore2

        // ok a3 points at list of rooms passed through.
        Mem.ww(a3, -1);                                // move.w #-1,(a3)+
        int d7 = Mem.uw(a0 + ObjT_ZoneID_w);           // move.w ObjT_ZoneID_w(a0),d7
        a3 = Mem.l(ZoneBss.Zone_EndOfListPtr_l);       // move.l Zone_EndOfListPtr_l,a3

        while (true) { // FINDCLOSELOOP:
            a2 = possclose;                            // move.l #possclose,a2
            a3 -= 2;                                   // move.w -(a3),d0
            d0 = Mem.w(a3);
            if (d0 < 0) {                              // blt foundclose
                break;
            }
            while (true) { // findinner:
                d1 = Mem.w(a2); a2 += 2;               // move.w (a2)+,d1
                if (d1 < 0) {                          // blt.s outin
                    break;
                }
                if ((short) d1 == (short) d0) {        // cmp.w d0,d1 ; bne.s findinner
                    d7 = setw(d7, d0);                 // move.w d0,d7
                    break;
                }
            }
            // outin: bra.s FINDCLOSELOOP
        }
        // foundclose:
        Mem.ww(a0 + ObjT_ZoneID_w, d7);                // move.w d7,ObjT_ZoneID_w(a0)
        // rts
    }
}
