package ab3d2;

import static ab3d2.Defs.*;
import static ab3d2.bss.PlayerBss.*;
import static ab3d2.ObjectmoveData.*;
import static ab3d2.NewplayershootData.*;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.swap;
import static ab3d2.M68k.divs;
import static ab3d2.HiresData.GLF_DatabasePtr_l;
import static ab3d2.HiresData.Aud_NoiseX_w;
import static ab3d2.HiresData.Aud_NoiseVol_w;
import static ab3d2.HiresData.Aud_SampleNum_w;
import static ab3d2.HiresData.Aud_ChannelPick_b;
import static ab3d2.HiresData.IDNUM;
import static ab3d2.HiresData.notifplaying;
import static ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l;
import static ab3d2.bss.TablesBss.ObjRotated_vl;
import static ab3d2.data.TablesData.SinCosTable_vw;
import static ab3d2.data.TablesData.COSINE_OFS;
import static ab3d2.data.TablesData.SINTAB_MASK_ADR;
import static ab3d2.NewanimsData.tempxoff;
import static ab3d2.NewanimsData.tempzoff;
import static ab3d2.NewanimsData.tempRoompt;
import static ab3d2.NewanimsData.bulyspd;
import static ab3d2.NewanimsData.closedist;
import static ab3d2.ControlloopData.Prefs_NoAutoAim_b;
import static ab3d2.Hires.PLR_SLAVE;

/**
 * Traduction littérale de ab3d2_source/newplayershoot.s — tir des joueurs.
 *
 * Plr1_Shot / Plr2_Shot partagent plrShot(p) (p∈{1,2}). Différences P1/P2 portées
 * fidèlement : masque EntT_EnemyFlags d7 (%100011 vs %10011), bit ligne-de-vue
 * (#0 ObjT_SeePlayer_b vs #1 de 17(a0)), logique souris/auto-aim (P1 seulement),
 * volumes Aud_NoiseVol, sens du test PLR_SLAVE. plrX_HitscanSucceded est identique
 * pour les deux (hitscanSucceeded). AMOD_A = and.w #SINTAB_MASK_ADR.
 */
public final class Newplayershoot {

    private Newplayershoot() {
    }

    public static int dbgLastTestGun = -1;  // DIAG : dernière arme loggée (test armes)

    public static void Plr1_Shot() {
        plrShot(1);
    }

    public static void Plr2_Shot() {
        plrShot(2);
    }

    private static void plrShot(int p) {
        boolean p1 = (p == 1);
        int timeToShoot = p1 ? Plr1_TimeToShoot_w : Plr2_TimeToShoot_w;
        int tmpGunSel = p1 ? Plr1_TmpGunSelected_b : Plr2_TmpGunSelected_b;
        int ammoCounts = p1 ? Plr1_AmmoCounts_vw : Plr2_AmmoCounts_vw;
        int tmpFire = p1 ? Plr1_TmpFire_b : Plr2_TmpFire_b;
        int angPos = p1 ? Plr1_AngPos_w : Plr2_AngPos_w;
        int xoff = p1 ? Plr1_XOff_l : Plr2_XOff_l;
        int zoff = p1 ? Plr1_ZOff_l : Plr2_ZOff_l;
        int yoff = p1 ? Plr1_YOff_l : Plr2_YOff_l;
        int stoodInTop = p1 ? Plr1_StoodInTop_b : Plr2_StoodInTop_b;
        int zonePtr = p1 ? Plr1_ZonePtr_l : Plr2_ZonePtr_l;
        int obsInLine = p1 ? Plr1_ObsInLine_vb : Plr2_ObsInLine_vb;
        int objDist = p1 ? Plr1_ObjectDistances_vw : Plr2_ObjectDistances_vw;
        int height = p1 ? Plr1_Height_l : Plr2_Height_l;
        int noiseVol = p1 ? Plr1_NoiseVol_w : Plr2_NoiseVol_w;
        int aimSpeed = p1 ? Plr1_AimSpeed_l : Plr2_AimSpeed_l;
        int sinVal = p1 ? Plr1_SinVal_w : Plr2_SinVal_w;
        int cosVal = p1 ? Plr1_CosVal_w : Plr2_CosVal_w;
        int objectPtr = p1 ? Plr1_ObjectPtr_l : Plr2_ObjectPtr_l;
        int gunFrame = p1 ? Plr1_GunFrame_w : Plr2_GunFrame_w;
        int d7mask = p1 ? 0b100011 : 0b10011;

        // tst.w PlrX_TimeToShoot_w ; beq.s .can_fire
        if ((short) Mem.uw(timeToShoot) != 0) {
            Mem.ww(timeToShoot, Mem.uw(timeToShoot) - Mem.uw(ab3d2.bss.AnimBss.Anim_TempFrames_w)); // sub.w Anim_TempFrames_w
            if ((short) Mem.uw(timeToShoot) < 0) Mem.ww(timeToShoot, 0); // bge .no_fire ; move.w #0
            return;                                    // rts
        }
        // .can_fire
        int d0 = setb(0, Mem.ub(tmpGunSel));           // moveq #0,d0 ; move.b PlrX_TmpGunSelected_b,d0
        Mem.wb(tempgun, d0);                           // move.b d0,tempgun
        int a6 = Mem.l(GLF_DatabasePtr_l) + GLFT_ShootDefs_l; // move.l GLF_DatabasePtr_l,a6 ; lea GLFT_ShootDefs_l(a6),a6
        int a5 = a6 + (GLFT_BulletDefs_l - GLFT_ShootDefs_l); // lea GLFT_BulletDefs_l-GLFT_ShootDefs_l(a6),a5
        a6 = a6 + (d0 & 0xFFFF) * 8;                    // lea (a6,d0.w*8),a6
        d0 = setw(0, Mem.uw(a6 + ShootT_BulType_w));    // move.w ShootT_BulType_w(a6),d0
        Mem.ww(BULTYPE, d0);                            // move.w d0,BULTYPE
        Mem.ww(AmmoInMyGun, Mem.uw(ammoCounts + (d0 & 0xFFFF) * 2)); // move.w (a0,d0.w*2),AmmoInMyGun
        d0 = muls(d0, BulT_SizeOf_l);                   // muls #BulT_SizeOf_l,d0
        a5 = a5 + (short) d0;                           // add.w d0,a5
        Mem.ww(BulletSpd, Mem.uw(a5 + BulT_Speed_l + 2)); // move.w BulT_Speed_l+2(a5),BulletSpd
        if (Mem.b(tmpFire) == 0) return;               // tst.b PlrX_TmpFire_b ; beq .no_fire (rts)

        d0 = setw(0, Mem.uw(angPos));                   // move.w PlrX_AngPos_w,d0
        Mem.ww(tempangpos, d0);                         // move.w d0,tempangpos
        int a0t = SinCosTable_vw + (d0 & 0xFFFF);       // move.l #SinCosTable_vw,a0 ; lea (a0,d0.w),a0
        Mem.ww(tempxdir, Mem.uw(a0t));                  // move.w (a0),tempxdir
        Mem.ww(tempzdir, Mem.uw(a0t + COSINE_OFS));     // move.w COSINE_OFS(a0),tempzdir
        Mem.ww(tempxoff, Mem.uw(xoff));                 // move.w PlrX_XOff_l,tempxoff
        Mem.ww(tempzoff, Mem.uw(zoff));                 // move.w PlrX_ZOff_l,tempzoff
        Mem.wl(tempyoff, Mem.l(yoff));                  // move.l PlrX_YOff_l,tempyoff
        Mem.wl(tempyoff, Mem.l(tempyoff) + 10 * 128);   // add.l #10*128,tempyoff
        Mem.wb(tempStoodInTop, Mem.ub(stoodInTop));     // move.b PlrX_StoodInTop_b,tempStoodInTop
        Mem.wl(tempRoompt, Mem.l(zonePtr));             // move.l PlrX_ZonePtr_l,tempRoompt
        int d7 = d7mask;                                // move.l #%100011,d7
        d0 = setw(0, -1);                               // move.w #-1,d0
        Mem.wl(targetydiff, 0);                         // move.l #0,targetydiff
        int d1 = 0x7fff;                                // move.l #$7fff,d1
        int a1 = obsInLine;                             // move.l #PlrX_ObsInLine_vb,a1
        int a0 = Mem.l(Lvl_ObjectDataPtr_l);            // move.l Lvl_ObjectDataPtr_l,a0
        int a2 = objDist;                               // move.l #PlrX_ObjectDistances_vw,a2
        int a4 = 0;                                     // (closest enemy ptr)

        while (true) {                                  // .find_closest_in_line
            if ((short) Mem.uw(a0) < 0) break;          // tst.w (a0) ; blt .out_of_line
            boolean skip = false;
            if (Mem.ub(a0 + ObjT_TypeID_b) == OBJ_TYPE_AUX) { // cmp.b #AUX ; beq .not_lined_up (a1 PAS avancé)
                skip = true;
            } else {
                int v = Mem.ub(a1);
                a1 += 1;                                // tst.b (a1)+
                if (v == 0) skip = true;                // beq.s .not_lined_up
                else {
                    boolean los = p1
                            ? (Mem.ub(a0 + ObjT_SeePlayer_b) & 1) != 0  // btst #0,ObjT_SeePlayer_b(a0)
                            : (Mem.ub(a0 + 17) & 2) != 0;               // btst #1,17(a0)
                    if (!los) skip = true;              // beq.s .not_lined_up
                    else if ((short) Mem.uw(a0 + ObjT_ZoneID_w) < 0) skip = true; // tst.w ObjT_ZoneID_w(a0) ; blt
                    else {
                        int d6 = Mem.ub(a0 + ObjT_TypeID_b); // move.b ObjT_TypeID_b(a0),d6
                        if ((d7 & (1 << (d6 & 31))) == 0) skip = true; // btst d6,d7 ; beq
                        else if (Mem.b(a0 + EntT_HitPoints_b) == 0) skip = true; // tst.b EntT_HitPoints_b(a0) ; beq
                        else {
                            int d5 = setw(0, Mem.uw(a0));            // move.w (a0),d5
                            int d6b = setw(0, Mem.uw(a2 + (d5 & 0xFFFF) * 2)); // move.w (a2,d5.w*2),d6
                            int d2v = setw(0, Mem.uw(a0 + 4));       // move.w 4(a0),d2
                            d2v = (short) d2v;          // ext.l d2
                            d2v = d2v << 7;             // asl.l #7,d2
                            d2v = d2v - Mem.l(yoff);    // sub.l PlrX_YOff_l,d2
                            int d3 = d2v;               // move.l d2,d3
                            if (d2v < 0) d2v = -d2v;    // bge .not_negative ; neg.l d2
                            d2v = muls(d2v, 93);        // muls #93,d2
                            d2v = d2v >> 8;             // asr.l #8,d2
                            d2v = d2v >> 4;             // asr.l #4,d2
                            if ((short) d2v > (short) d6b) skip = true; // cmp.w d6,d2 ; bgt .not_lined_up
                            else if ((short) d1 < (short) d6b) skip = true; // cmp.w d6,d1 ; blt .not_lined_up
                            else {
                                d1 = setw(d1, d6b);     // move.w d6,d1
                                a4 = a0;                // move.l a0,a4
                                Mem.wl(targetydiff, d3); // move.l d3,targetydiff
                                d0 = setw(d0, d5);      // move.w d5,d0
                            }
                        }
                    }
                }
            }
            // .not_lined_up
            a0 += ENT_NEXT;                             // add.w #ENT_NEXT,a0
        }

        // .out_of_line
        Mem.ww(targdist, d1);                           // move.w d1,targdist
        int d5 = Mem.l(targetydiff);                    // move.l targetydiff,d5
        d5 = d5 - Mem.l(height);                        // sub.l PlrX_Height_l,d5
        d5 = d5 + 18 * 256;                             // add.l #18*256,d5
        Mem.ww(closedist, d1);                          // move.w d1,closedist
        int d2 = setw(0, Mem.uw(BulletSpd));            // move.w BulletSpd,d2
        d1 = setw(d1, ((short) d1) >> (d2 & 31));       // asr.w d2,d1
        if ((short) d1 <= 0) d1 = setw(d1, 1);          // tst.w d1 ; bgt .distance_ok ; moveq #1,d1
        // .distance_ok
        d5 = divs(d5, d1);                              // divs d1,d5
        Mem.ww(bulyspd, d5);                            // move.w d5,bulyspd
        d2 = setw(0, Mem.uw(AmmoInMyGun));              // move.w AmmoInMyGun,d2
        d1 = setw(0, Mem.uw(a6 + ShootT_BulCount_w));   // move.w ShootT_BulCount_w(a6),d1
        if ((short) d2 < (short) d1) {                  // cmp.w d1,d2 ; bge .okcanshoot
            // pas assez de munitions
            a2 = Mem.l(objectPtr);                      // move.l PlrX_ObjectPtr_l,a2
            d0 = setw(0, Mem.uw(a2));                   // move.w (a2),d0
            a2 = ObjRotated_vl;                         // move.l #ObjRotated_vl,a2
            Mem.wl(Aud_NoiseX_w, Mem.l(a2 + (d0 & 0xFFFF) * 8)); // move.l (a2,d0.w*8),Aud_NoiseX_w
            Mem.ww(Aud_NoiseVol_w, p1 ? 100 : 300);    // move.w #100/#300,Aud_NoiseVol_w
            Mem.ww(noiseVol, 100);                     // move.w #100,PlrX_NoiseVol_w
            Mem.ww(Aud_SampleNum_w, 12);               // move.w #12,Aud_SampleNum_w
            Mem.wb(notifplaying, 0);                   // clr.b notifplaying
            Mem.wb(IDNUM, 0xFB);                       // move.b #$fb,IDNUM
            Hires.MakeSomeNoise();                     // jsr MakeSomeNoise
            return;                                     // rts
        }
        // .okcanshoot
        if (Hires.dbgWeaponTest && Mem.ub(tempgun) != dbgLastTestGun) { // DIAG : log par arme (1×)
            dbgLastTestGun = Mem.ub(tempgun);
            int glf = Mem.l(GLF_DatabasePtr_l);
            StringBuilder nm = new StringBuilder();
            for (int k = 0; k < 20; k++) { int c = Mem.ub(glf + ab3d2.Defs.GLFT_GunNames_l + dbgLastTestGun * 20 + k); if (c == 0) break; nm.append((char) c); }
            boolean hitscan = Mem.w(a5 + ab3d2.Defs.BulT_IsHitScan_l + 2) != 0;
            System.out.println("[arme " + dbgLastTestGun + "] \"" + nm + "\" BulType=" + Mem.uw(BULTYPE)
                + " ammo=" + Mem.uw(AmmoInMyGun) + " BulCount=" + Mem.uw(a6 + ShootT_BulCount_w)
                + " " + (hitscan ? "HITSCAN" : "PROJECTILE") + " SFX=" + Mem.uw(a6 + ab3d2.Defs.ShootT_SFX_w) + " → TIRE");
        }
        if (p1) {                                       // cmp.b #PLR_SLAVE ; beq .notplr1 (skip si ==SLAVE)
            if (Mem.ub(Plr_MultiplayerType_b) != PLR_SLAVE) {
                Mem.ww(Mem.l(Plr1_ObjectPtr_l) + ENT_NEXT_2 + EntT_Timer1_w, 1); // move.w #1,ENT_NEXT_2+EntT_Timer1_w(a2)
            }
        } else {                                        // cmp.b #PLR_SLAVE ; bne .notplr2 (skip si !=SLAVE)
            if (Mem.ub(Plr_MultiplayerType_b) == PLR_SLAVE) {
                Mem.ww(Mem.l(Plr1_ObjectPtr_l) + ENT_NEXT_2 + EntT_Timer1_w, 1);
            }
        }
        Mem.ww(timeToShoot, Mem.uw(a6 + ShootT_Delay_w)); // move.w ShootT_Delay_w(a6),PlrX_TimeToShoot_w
        Mem.wb(gunFrame, Mem.ub(MaxFrame));            // move.b MaxFrame,PlrX_GunFrame_w
        d2 = setw(d2, d2 - d1);                         // sub.w d1,d2
        a2 = ammoCounts;                                // move.l #PlrX_AmmoCounts_vw,a2
        a2 = a2 + (short) Mem.uw(BULTYPE);             // add.w BULTYPE,a2
        a2 = a2 + (short) Mem.uw(BULTYPE);             // add.w BULTYPE,a2
        Mem.ww(a2, d2);                                 // move.w d2,(a2)
        a2 = Mem.l(objectPtr);                          // move.l PlrX_ObjectPtr_l,a2
        d2 = setw(0, Mem.uw(a2));                       // move.w (a2),d2
        a2 = ObjRotated_vl;                             // move.l #ObjRotated_vl,a2
        Mem.wl(Aud_NoiseX_w, Mem.l(a2 + (d2 & 0xFFFF) * 8)); // move.l (a2,d2.w*8),Aud_NoiseX_w
        Mem.ww(noiseVol, 100);                          // move.w #100,PlrX_NoiseVol_w
        Mem.ww(Aud_NoiseVol_w, 300);                    // move.w #300,Aud_NoiseVol_w
        Mem.ww(Aud_SampleNum_w, Mem.uw(a6 + ShootT_SFX_w)); // move.w ShootT_SFX_w(a6),Aud_SampleNum_w
        Mem.wb(Aud_ChannelPick_b, 2);                  // move.b #2,Aud_ChannelPick_b
        Mem.wb(notifplaying, 0);                        // clr.b notifplaying
        Mem.wb(IDNUM, 0xFB);                            // move.b #$fb,IDNUM
        Hires.MakeSomeNoise();                          // jsr MakeSomeNoise (movem préserve d0/a4/a5/a6/d5/d6/d7)

        if ((short) d0 < 0) {                           // tst.w d0 ; blt .nothing_to_shoot
            nothingToShoot(p, a5, a6, xoff, zoff, yoff, sinVal, cosVal, zonePtr);
            return;
        }
        // aim (auto/gravity)
        boolean doAim;
        if (p1) {
            if (Mem.b(Plr1_Mouse_b) != 0 && Mem.b(Prefs_NoAutoAim_b) != 0) doAim = true; // mouse && NoAutoAim → .no_auto_aim
            else doAim = (Mem.l(a5 + BulT_Gravity_l) != 0); // .not_mouse: tst.l Gravity ; beq .skip_aim
        } else {
            doAim = (Mem.l(a5 + BulT_Gravity_l) != 0);  // tst.l Gravity ; beq .skip_aim
        }
        if (doAim) {                                    // .no_auto_aim
            int dd2 = setw(0, Mem.uw(aimSpeed));        // move.w PlrX_AimSpeed_l,d2
            int dd1 = setw(0, 8);                       // move.w #8,d1
            dd1 = setw(dd1, dd1 - Mem.uw(BulletSpd));   // sub.w BulletSpd,d1
            dd2 = setw(dd2, ((short) dd2) >> (dd1 & 31)); // asr.w d1,d2
            Mem.ww(bulyspd, dd2);                       // move.w d2,bulyspd
        }
        // .skip_aim
        if (Mem.w(a5 + BulT_IsHitScan_l + 2) == 0) {    // tst.w BulT_IsHitScan_l+2(a5) ; beq plrX_FireProjectile
            fireProjectile(p, a5, a6);
            return;
        }
        // hitscan
        d7 = setw(0, Mem.uw(a6 + ShootT_BulCount_w));   // move.w ShootT_BulCount_w(a6),d7
        while (true) {                                  // .fire_hitscanned_bullets
            int rt = Objectmove.GetRand();              // jsr GetRand
            int ta1 = Mem.l(Lvl_ObjectPointsPtr_l);     // move.l Lvl_ObjectPointsPtr_l,a1
            int td1 = setw(0, Mem.uw(a4));              // move.w (a4),d1
            ta1 = ta1 + (td1 & 0xFFFF) * 8;            // lea (a1,d1.w*8),a1
            rt = setw(rt, rt & 0x7fff);                // and.w #$7fff,d0
            td1 = setw(0, Mem.uw(ta1));                // move.w (a1),d1
            td1 = setw(td1, td1 - Mem.uw(xoff));        // sub.w PlrX_XOff_l,d1
            td1 = muls(td1, td1);                       // muls d1,d1
            int td2 = setw(0, Mem.uw(ta1 + 4));         // move.w 4(a1),d2
            td2 = setw(td2, td2 - Mem.uw(zoff));        // sub.w PlrX_ZOff_l,d2
            td2 = muls(td2, td2);                       // muls d2,d2
            td1 = td1 + td2;                            // add.l d2,d1
            td1 = td1 >> 6;                             // asr.l #6,d1
            rt = (short) rt;                            // ext.l d0
            rt = rt << 1;                               // asl.l #1,d0
            if (rt > td1) hitscanSucceeded(d0, a4, a5); // cmp.l d1,d0 ; bgt .hit ; bsr plrX_HitscanSucceded
            else hitscanFailed(p, a4, a0, xoff, zoff, yoff, zonePtr); // bsr plrX_HitscanFailed
            d7 = setw(d7, d7 - 1);                      // subq #1,d7
            if (!((short) d7 > 0)) break;               // bgt .fire_hitscanned_bullets
        }
        // rts
    }

    /** .nothing_to_shoot (newplayershoot.s:257) — aucun ennemi aligné : tir tout droit. */
    private static void nothingToShoot(int p, int a5, int a6, int xoff, int zoff, int yoff,
                                       int sinVal, int cosVal, int zonePtr) {
        int d0 = setw(0, Mem.uw(p == 1 ? Plr1_AimSpeed_l : Plr2_AimSpeed_l)); // move.w PlrX_AimSpeed_l,d0
        int d1 = setw(0, 8);                            // move.w #8,d1
        d1 = setw(d1, d1 - Mem.uw(BulletSpd));          // sub.w BulletSpd,d1
        d0 = setw(d0, ((short) d0) >> (d1 & 31));       // asr.w d1,d0
        Mem.ww(bulyspd, d0);                            // move.w d0,bulyspd
        if (Mem.w(a5 + BulT_IsHitScan_l + 2) == 0) {    // tst.w BulT_IsHitScan_l+2(a5) ; beq plrX_FireProjectile
            fireProjectile(p, a5, a6);
            return;
        }
        Mem.ww(bulyspd, 0);                             // move.w #0,bulyspd
        Mem.ww(oldx, Mem.uw(xoff));                     // move.w PlrX_XOff_l,oldx
        Mem.ww(oldz, Mem.uw(zoff));                     // move.w PlrX_ZOff_l,oldz
        d0 = setw(0, Mem.uw(sinVal));                   // move.w PlrX_SinVal_w,d0
        d0 = setw(d0, ((short) d0) >> 7);               // asr.w #7,d0
        d0 = setw(d0, d0 + Mem.uw(oldx));               // add.w oldx,d0
        Mem.ww(newx, d0);                               // move.w d0,newx
        d0 = setw(0, Mem.uw(cosVal));                   // move.w PlrX_CosVal_w,d0
        d0 = setw(d0, ((short) d0) >> 7);               // asr.w #7,d0
        d0 = setw(d0, d0 + Mem.uw(oldz));               // add.w oldz,d0
        Mem.ww(newz, d0);                               // move.w d0,newz
        int d0l = Mem.l(yoff);                          // move.l PlrX_YOff_l,d0
        d0l = d0l + 10 * 128;                           // add.l #10*128,d0
        Mem.wl(oldy, d0l);                              // move.l d0,oldy
        int d1l = d0l;                                  // move.l d0,d1
        d0l = Objectmove.GetRand();                     // jsr GetRand
        d0l = setw(d0l, d0l & 0xfff);                   // and.w #$fff,d0
        d0l = setw(d0l, d0l - 0x800);                   // sub.w #$800,d0
        d0l = (short) d0l;                              // ext.l d0
        d1l = d1l + d0l;                                // add.l d0,d1
        Mem.wl(newy, d1l);                              // move.l d1,newy
        setupBulletMove(zonePtr);                       // exitfirst..Obj_ZonePtr_l
        // .again (MoveObject loop)
        while (true) {
            Objectmove.MoveObject();                    // jsr MoveObject
            if (Mem.b(hitwall) != 0) break;             // tst.b hitwall ; bne .nofurther
            advanceMove();                              // (new-old)*2 sur x/z/y
        }
        // .nofurther — spawn shot
        int a0 = Mem.l(Plr_ShotDataPtr_l);              // move.l Plr_ShotDataPtr_l,a0
        d1 = setw(0, NUM_PLR_SHOT_DATA - 1);            // move.w #NUM_PLR_SHOT_DATA-1,d1
        while (true) {                                  // .findonefree2
            if ((short) Mem.uw(a0 + ObjT_ZoneID_w) < 0) break; // move.w ObjT_ZoneID_w(a0),d0 ; blt
            a0 += ObjT_SizeOf_l;                        // NEXT_OBJ a0
            d1 = setw(d1, d1 - 1);                      // dbra d1,.findonefree2
            if ((short) d1 == -1) return;              // rts
        }
        // .foundonefree2
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);          // move.l Lvl_ObjectPointsPtr_l,a1
        int d2 = setw(0, Mem.uw(a0));                   // move.w (a0),d2
        Mem.ww(a1 + (d2 & 0xFFFF) * 8, Mem.uw(newx));   // move.w newx,(a1,d2.w*8)
        Mem.ww(a1 + (d2 & 0xFFFF) * 8 + 4, Mem.uw(newz)); // move.w newz,4(a1,d2.w*8)
        Mem.wb(a0 + ShotT_Status_b, 1);                 // move.b #1,ShotT_Status_b(a0)
        Mem.ww(a0 + ShotT_Gravity_w, 0);                // move.w #0,ShotT_Gravity_w(a0)
        Mem.wb(a0 + ShotT_Size_b, Mem.ub(BULTYPE + 1)); // move.b BULTYPE+1,ShotT_Size_b(a0)
        Mem.wb(a0 + ShotT_Anim_b, 0);                   // move.b #0,ShotT_Anim_b(a0)
        a1 = Mem.l(Obj_ZonePtr_l);                       // move.l Obj_ZonePtr_l,a1
        Mem.ww(a0 + ObjT_ZoneID_w, Mem.uw(a1));         // move.w (a1),ObjT_ZoneID_w(a0)
        Mem.wb(a0 + ShotT_Worry_b, 0xFF);               // st ShotT_Worry_b(a0)
        int d0w = Mem.l(wallhitheight);                 // move.l wallhitheight,d0
        Mem.wl(a0 + ShotT_AccYPos_w, d0w);              // move.l d0,ShotT_AccYPos_w(a0)
        d0w = d0w >> 7;                                 // asr.l #7,d0
        Mem.ww(a0 + 4, d0w);                            // move.w d0,4(a0)
    }

    /** plrX_FireProjectile + firefive (newplayershoot.s:694/719). */
    private static void fireProjectile(int p, int a5, int a6) {
        int d7 = p == 1 ? 0b100011 : 0b10011;           // move.l #%100011/%10011,d7
        Mem.wb(p == 1 ? Plr1_GunFrame_w : Plr2_GunFrame_w, Mem.ub(MaxFrame)); // move.b MaxFrame,PlrX_GunFrame_w
        int d5 = setw(0, Mem.uw(a6 + ShootT_BulCount_w)); // move.w ShootT_BulCount_w(a6),d5
        int d6 = setw(0, d5);                           // move.w d5,d6
        d6 = setw(d6, d6 - 1);                          // subq #1,d6
        d6 = setw(d6, (d6 & 0xFFFF) << 7);              // asl.w #7,d6
        d6 = setw(d6, -(short) d6);                     // neg.w d6
        d6 = setw(d6, d6 + Mem.uw(tempangpos));         // add.w tempangpos,d6
        d6 = setw(d6, d6 & SINTAB_MASK_ADR);            // AMOD_A d6

        // firefive
        while (true) {
            int a0 = Mem.l(Plr_ShotDataPtr_l);          // move.l Plr_ShotDataPtr_l,a0
            int d1 = setw(0, NUM_PLR_SHOT_DATA - 1);    // move.w #NUM_PLR_SHOT_DATA-1,d1
            while (true) {                              // .findonefree
                if ((short) Mem.uw(a0 + ObjT_ZoneID_w) < 0) break; // move.w ObjT_ZoneID_w(a0),d0 ; blt
                a0 += ObjT_SizeOf_l;                    // NEXT_OBJ a0
                d1 = setw(d1, d1 - 1);                  // dbra d1
                if ((short) d1 == -1) return;          // rts
            }
            // .foundonefree
            Mem.ww(a0 + ShotT_Gravity_w, Mem.uw(a5 + BulT_Gravity_l + 2)); // move.w BulT_Gravity_l+2(a5),ShotT_Gravity_w(a0)
            Mem.wb(a0 + ShotT_Flags_w, Mem.ub(a5 + BulT_BounceHoriz_l + 3)); // move.b BulT_BounceHoriz_l+3(a5),ShotT_Flags_w(a0)
            Mem.wb(a0 + ShotT_Flags_w + 1, Mem.ub(a5 + BulT_BounceVert_l + 3)); // move.b BulT_BounceVert_l+3(a5),ShotT_Flags_w+1(a0)
            int d0 = setw(0, Mem.uw(bulyspd));          // move.w bulyspd,d0
            if ((short) d0 >= 20 * 128) d0 = setw(d0, 20 * 128);   // cmp.w #20*128,d0 ; blt .okdownspd ; move.w #20*128,d0
            if ((short) d0 <= -20 * 128) d0 = setw(d0, -20 * 128); // cmp.w #-20*128,d0 ; bgt .okupspd ; move.w #-20*128,d0
            Mem.ww(bulyspd, d0);                        // move.w d0,bulyspd
            // a2 = ObjRotated_vl (inutilisé ensuite)
            Mem.wb(a0 + ShotT_Size_b, Mem.ub(BULTYPE + 1)); // move.b BULTYPE+1,ShotT_Size_b(a0)
            Mem.wb(a0 + ShotT_Power_w, Mem.ub(a5 + BulT_HitDamage_l + 3)); // move.b BulT_HitDamage_l+3(a5),ShotT_Power_w(a0)
            int a1 = Mem.l(Lvl_ObjectPointsPtr_l);      // move.l Lvl_ObjectPointsPtr_l,a1
            int d1b = setw(0, Mem.uw(a0));              // move.w (a0),d1
            a1 = a1 + (d1b & 0xFFFF) * 8;              // lea (a1,d1.w*8),a1
            Mem.ww(a1, Mem.uw(tempxoff));              // move.w tempxoff,(a1)
            Mem.ww(a1 + 4, Mem.uw(tempzoff));          // move.w tempzoff,4(a1)
            int as1 = SinCosTable_vw;                   // move.l #SinCosTable_vw,a1
            d0 = setw(0, Mem.uw(as1 + (d6 & 0xFFFF)));  // move.w (a1,d6.w),d0
            d0 = (short) d0;                            // ext.l d0
            as1 = as1 + COSINE_OFS;                     // add.w #COSINE_OFS,a1
            int d2 = setw(0, Mem.uw(as1 + (d6 & 0xFFFF))); // move.w (a1,d6.w),d2
            d2 = (short) d2;                            // ext.l d2
            d6 = setw(d6, d6 + 256);                    // add.w #256,d6
            d6 = setw(d6, d6 & SINTAB_MASK_ADR);        // AMOD_A d6
            d1b = setw(0, Mem.uw(BulletSpd));           // move.w BulletSpd,d1
            d0 = d0 << (d1b & 31);                      // asl.l d1,d0
            Mem.wl(a0 + ShotT_VelocityX_w, d0);         // move.l d0,ShotT_VelocityX_w(a0)
            d2 = (short) d2;                            // ext.l d2
            d2 = d2 << (d1b & 31);                      // asl.l d1,d2
            Mem.wb(a0 + ObjT_TypeID_b, OBJ_TYPE_PROJECTILE); // move.b #OBJ_TYPE_PROJECTILE,ObjT_TypeID_b(a0)
            Mem.wl(a0 + ShotT_VelocityZ_w, d2);         // move.l d2,ShotT_VelocityZ_w(a0)
            Mem.ww(a0 + ShotT_VelocityY_w, Mem.uw(bulyspd)); // move.w bulyspd,ShotT_VelocityY_w(a0)
            Mem.wb(a0 + ShotT_InUpperZone_b, Mem.ub(tempStoodInTop)); // move.b tempStoodInTop,ShotT_InUpperZone_b(a0)
            Mem.ww(a0 + ShotT_Lifetime_w, 0);           // move.w #0,ShotT_Lifetime_w(a0)
            Mem.wl(a0 + EntT_EnemyFlags_l, d7);         // move.l d7,EntT_EnemyFlags_l(a0)
            int a2 = Mem.l(tempRoompt);                 // move.l tempRoompt,a2
            Mem.ww(a0 + ObjT_ZoneID_w, Mem.uw(a2));     // move.w (a2),ObjT_ZoneID_w(a0)
            d0 = Mem.l(tempyoff);                       // move.l tempyoff,d0
            d0 = d0 + 20 * 128;                         // add.l #20*128,d0
            Mem.wl(a0 + ShotT_AccYPos_w, d0);           // move.l d0,ShotT_AccYPos_w(a0)
            Mem.wb(a0 + ShotT_Worry_b, 0xFF);           // st ShotT_Worry_b(a0)
            d0 = d0 >> 7;                               // asr.l #7,d0
            Mem.ww(a0 + 4, d0);                         // move.w d0,4(a0)
            d5 = setw(d5, d5 - 1);                       // sub.w #1,d5
            if (!((short) d5 > 0)) return;              // bgt firefive
        }
    }

    /** plrX_HitscanSucceded (newplayershoot.s:798/940) — identique P1/P2. */
    private static void hitscanSucceeded(int d0, int a4, int a5) {
        int a0 = Mem.l(Plr_ShotDataPtr_l);              // move.l Plr_ShotDataPtr_l,a0
        int d1 = setw(0, NUM_PLR_SHOT_DATA - 1);        // move.w #NUM_PLR_SHOT_DATA-1,d1
        while (true) {                                  // .findonefree
            if ((short) Mem.uw(a0 + ObjT_ZoneID_w) < 0) break; // move.w ObjT_ZoneID_w(a0),d2 ; blt
            a0 += ObjT_SizeOf_l;                        // NEXT_OBJ a0
            d1 = setw(d1, d1 - 1);                      // dbra d1
            if ((short) d1 == -1) return;              // rts
        }
        // .foundonefree
        Mem.wb(a0 + ObjT_TypeID_b, OBJ_TYPE_PROJECTILE); // move.b #OBJ_TYPE_PROJECTILE,ObjT_TypeID_b(a0)
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);          // move.l Lvl_ObjectPointsPtr_l,a1
        int d2 = setw(0, Mem.uw(a0));                   // move.w (a0),d2
        Mem.wl(a1 + (d2 & 0xFFFF) * 8, Mem.l(a1 + (d0 & 0xFFFF) * 8)); // move.l (a1,d0.w*8),(a1,d2.w*8)
        Mem.wl(a1 + (d2 & 0xFFFF) * 8 + 4, Mem.l(a1 + (d0 & 0xFFFF) * 8 + 4)); // move.l 4(a1,d0.w*8),4(a1,d2.w*8)
        Mem.wb(a0 + ShotT_Status_b, 1);                 // move.b #1,ShotT_Status_b(a0)
        Mem.ww(a0 + ShotT_Gravity_w, 0);                // move.w #0,ShotT_Gravity_w(a0)
        Mem.wb(a0 + ShotT_Size_b, Mem.ub(BULTYPE + 1)); // move.b BULTYPE+1,ShotT_Size_b(a0)
        Mem.wb(a0 + ShotT_Anim_b, 0);                   // move.b #0,ShotT_Anim_b(a0)
        int d1b = setw(0, Mem.uw(a4 + 4));              // move.w 4(a4),d1
        d1b = (short) d1b;                              // ext.l d1
        d1b = d1b << 7;                                 // asl.l #7,d1
        Mem.wl(a0 + ShotT_AccYPos_w, d1b);              // move.l d1,ShotT_AccYPos_w(a0)
        Mem.ww(a0 + ObjT_ZoneID_w, Mem.uw(a4 + ObjT_ZoneID_w)); // move.w ObjT_ZoneID_w(a4),ObjT_ZoneID_w(a0)
        Mem.wb(a0 + ShotT_Worry_b, 0xFF);               // st ShotT_Worry_b(a0)
        Mem.ww(a0 + 4, Mem.uw(a4 + 4));                 // move.w 4(a4),4(a0)
        int dmg = setw(0, Mem.uw(a5 + BulT_HitDamage_l + 2)); // move.w BulT_HitDamage_l+2(a5),d0
        Mem.wb(a4 + EntT_DamageTaken_b, Mem.ub(a4 + EntT_DamageTaken_b) + dmg); // add.b d0,EntT_DamageTaken_b(a4)
        int dd1 = setw(0, Mem.uw(tempxdir));            // move.w tempxdir,d1
        dd1 = (short) dd1;                              // ext.l d1
        dd1 = dd1 << 3;                                 // asl.l #3,d1
        dd1 = swap(dd1);                                // swap d1
        Mem.ww(a4 + EntT_ImpactX_w, dd1);               // move.w d1,EntT_ImpactX_w(a4)
        dd1 = setw(0, Mem.uw(tempzdir));                // move.w tempzdir,d1
        dd1 = (short) dd1;                              // ext.l d1
        dd1 = dd1 << 3;                                 // asl.l #3,d1
        dd1 = swap(dd1);                                // swap d1
        Mem.ww(a4 + EntT_ImpactZ_w, dd1);               // move.w d1,EntT_ImpactZ_w(a4)
    }

    /** plrX_HitscanFailed (newplayershoot.s:847/990) — diffère par les offsets joueur. */
    private static void hitscanFailed(int p, int a4, int a0term, int xoff, int zoff, int yoff, int zonePtr) {
        Mem.ww(oldx, Mem.uw(xoff));                     // move.w PlrX_XOff_l,oldx
        Mem.ww(oldz, Mem.uw(zoff));                     // move.w PlrX_ZOff_l,oldz
        int d1 = Mem.l(yoff);                           // move.l PlrX_YOff_l,d1
        d1 = d1 + 10 * 128;                             // add.l #10*128,d1
        Mem.wl(oldy, d1);                               // move.l d1,oldy
        int d0 = setw(0, Mem.uw(a4));                   // move.w (a4),d0
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);          // move.l Lvl_ObjectPointsPtr_l,a1
        int d2 = setw(0, Mem.uw(a1 + (d0 & 0xFFFF) * 8)); // move.w (a1,d0.w*8),d2
        d2 = setw(d2, d2 - Mem.uw(oldx));               // sub.w oldx,d2
        d2 = setw(d2, ((short) d2) >> 1);               // asr.w #1,d2
        d2 = setw(d2, d2 + Mem.uw(oldx));               // add.w oldx,d2
        Mem.ww(newx, d2);                               // move.w d2,newx
        d2 = setw(0, Mem.uw(a1 + (d0 & 0xFFFF) * 8 + 4)); // move.w 4(a1,d0.w*8),d2
        d2 = setw(d2, d2 - Mem.uw(oldz));               // sub.w oldz,d2
        d2 = setw(d2, ((short) d2) >> 1);               // asr.w #1,d2
        d2 = setw(d2, d2 + Mem.uw(oldz));               // add.w oldz,d2
        Mem.ww(newz, d2);                               // move.w d2,newz
        d2 = setw(0, Mem.uw(a0term + 4));               // move.w 4(a0),d2
        d2 = (short) d2;                                // ext.l d2
        d2 = d2 << 7;                                   // asl.l #7,d2
        Mem.wl(newy, d2);                               // move.l d2,newy
        setupBulletMove(zonePtr);
        while (true) {                                  // .again
            Objectmove.MoveObject();                    // jsr MoveObject
            if (Mem.b(hitwall) != 0) break;             // tst.b hitwall ; bne .nofurther
            advanceMove();
        }
        // .nofurther
        int a0 = Mem.l(Plr_ShotDataPtr_l);              // move.l Plr_ShotDataPtr_l,a0
        d1 = setw(0, NUM_PLR_SHOT_DATA - 1);            // move.w #NUM_PLR_SHOT_DATA-1,d1
        while (true) {                                  // .findonefree2
            if ((short) Mem.uw(a0 + ObjT_ZoneID_w) < 0) break; // blt
            a0 += ObjT_SizeOf_l;                        // NEXT_OBJ a0
            d1 = setw(d1, d1 - 1);                      // dbra d1
            if ((short) d1 == -1) return;              // rts
        }
        // .foundonefree2
        Mem.wb(a0 + ObjT_TypeID_b, OBJ_TYPE_PROJECTILE); // move.b #OBJ_TYPE_PROJECTILE,ObjT_TypeID_b(a0)
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);              // move.l Lvl_ObjectPointsPtr_l,a1
        d2 = setw(0, Mem.uw(a0));                       // move.w (a0),d2
        Mem.ww(a1 + (d2 & 0xFFFF) * 8, Mem.uw(newx));   // move.w newx,(a1,d2.w*8)
        Mem.ww(a1 + (d2 & 0xFFFF) * 8 + 4, Mem.uw(newz)); // move.w newz,4(a1,d2.w*8)
        Mem.wb(a0 + ShotT_Status_b, 1);                 // move.b #1,ShotT_Status_b(a0)
        Mem.ww(a0 + ShotT_Gravity_w, 0);                // move.w #0,ShotT_Gravity_w(a0)
        Mem.wb(a0 + ShotT_Size_b, Mem.ub(BULTYPE + 1)); // move.b BULTYPE+1,ShotT_Size_b(a0)
        Mem.wb(a0 + ShotT_Anim_b, 0);                   // move.b #0,ShotT_Anim_b(a0)
        a1 = Mem.l(Obj_ZonePtr_l);                       // move.l Obj_ZonePtr_l,a1
        Mem.ww(a0 + ObjT_ZoneID_w, Mem.uw(a1));         // move.w (a1),ObjT_ZoneID_w(a0)
        Mem.wb(a0 + ShotT_Worry_b, 0xFF);               // st ShotT_Worry_b(a0)
        d1 = Mem.l(newy);                               // move.l newy,d1
        Mem.wl(a0 + ShotT_AccYPos_w, d1);               // move.l d1,ShotT_AccYPos_w(a0)
        d1 = d1 >> 7;                                   // asr.l #7,d1
        Mem.ww(a0 + 4, d1);                             // move.w d1,4(a0)
    }

    /** Bloc commun d'init avant MoveObject (exitfirst..Obj_ZonePtr_l). */
    private static void setupBulletMove(int zonePtr) {
        Mem.wb(exitfirst, 0xFF);                        // st exitfirst
        Mem.wb(Obj_WallBounce_b, 0);                    // clr.b Obj_WallBounce_b
        Mem.ww(Obj_ExtLen_w, 0);                        // move.w #0,Obj_ExtLen_w
        Mem.wb(Obj_AwayFromWall_b, 0xFF);               // move.b #$ff,Obj_AwayFromWall_b
        Mem.ww(wallflags, 0b0000010000000000);          // move.w #%0000010000000000,wallflags
        Mem.wl(StepUpVal, 0);                           // move.l #0,StepUpVal
        Mem.wl(StepDownVal, 0x1000000);                 // move.l #$1000000,StepDownVal
        Mem.wl(thingheight, 0);                         // move.l #0,thingheight
        Mem.wl(Obj_ZonePtr_l, Mem.l(zonePtr));          // move.l PlrX_ZonePtr_l,Obj_ZonePtr_l
    }

    /** Avance (new-old)*2 sur x/z (word) et y (long) entre deux passes de MoveObject. */
    private static void advanceMove() {
        int d0 = setw(0, Mem.uw(newx));                 // move.w newx,d0
        d0 = setw(d0, d0 - Mem.uw(oldx));               // sub.w oldx,d0
        Mem.ww(oldx, Mem.uw(oldx) + d0);                // add.w d0,oldx
        Mem.ww(newx, Mem.uw(newx) + d0);                // add.w d0,newx
        d0 = setw(0, Mem.uw(newz));                     // move.w newz,d0
        d0 = setw(d0, d0 - Mem.uw(oldz));               // sub.w oldz,d0
        Mem.ww(oldz, Mem.uw(oldz) + d0);                // add.w d0,oldz
        Mem.ww(newz, Mem.uw(newz) + d0);                // add.w d0,newz
        int d0l = Mem.l(newy);                          // move.l newy,d0
        d0l = d0l - Mem.l(oldy);                        // sub.l oldy,d0
        Mem.wl(oldy, Mem.l(oldy) + d0l);                // add.l d0,oldy
        Mem.wl(newy, Mem.l(newy) + d0l);                // add.l d0,newy
    }
}
