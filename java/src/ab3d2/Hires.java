package ab3d2;

import ab3d2.bss.PlayerBss;
import ab3d2.host.CustomChips;
import ab3d2.modules.RawKeyMacros;
import ab3d2.modules.DevInst;
import ab3d2.modules.DevMacros;
import ab3d2.modules.draw.DrawFloor;

import static ab3d2.HiresData.*;
import static ab3d2.HireswallData.*;
import static ab3d2.M68k.muls;
import static ab3d2.M68k.mulu;
import static ab3d2.M68k.divs;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.swap;
import static ab3d2.bss.DrawBss.Draw_TopOfRoom_l;
import static ab3d2.bss.DrawBss.Draw_BottomOfRoom_l;
import static ab3d2.bss.DrawBss.Draw_CurrentZone_w;
import static ab3d2.bss.DrawBss.Draw_TexturePalettePtr_l;
import static ab3d2.bss.DrawBss.Draw_FloorTexturesPtr_l;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.VidBss.Vid_DoubleHeight_b;
import static ab3d2.bss.VidBss.Vid_DoubleWidth_b;
import static ab3d2.bss.VidBss.Vid_LetterBoxMarginHeight_w;
import static ab3d2.M68k.asrw;
import static ab3d2.M68k.extw;
import static ab3d2.bss.TablesBss.Rotated_vl;
import static ab3d2.bss.TablesBss.OnScreen_vl;
import static ab3d2.bss.TablesBss.LeftSideTable_vw;
import static ab3d2.bss.TablesBss.RightSideTable_vw;
import static ab3d2.bss.TablesBss.LeftBrightTable_vw;
import static ab3d2.bss.TablesBss.RightBrightTable_vw;
import static ab3d2.data.TablesData.SinCosTable_vw;
import static ab3d2.data.TablesData.COSINE_OFS;
import static ab3d2.bss.TablesBss.Aud_SampleList_vl;
import static ab3d2.bss.PlayerBss.Plr1_Health_w;
import static ab3d2.bss.PlayerBss.Plr1_Fire_b;
import static ab3d2.bss.PlayerBss.Plr1_Clicked_b;
import static ab3d2.bss.PlayerBss.Plr1_SnapAngPos_w;
import static ab3d2.bss.PlayerBss.Plr1_SnapAngSpd_w;
import static ab3d2.bss.PlayerBss.Plr1_SnapXOff_l;
import static ab3d2.bss.PlayerBss.Plr1_SnapZOff_l;
import static ab3d2.bss.PlayerBss.Plr2_Health_w;
import static ab3d2.bss.PlayerBss.Plr2_SnapAngPos_w;
import static ab3d2.bss.PlayerBss.Plr2_SnapAngSpd_w;
import static ab3d2.bss.PlayerBss.Plr2_SnapXOff_l;
import static ab3d2.bss.PlayerBss.Plr2_SnapZOff_l;
import static ab3d2.bss.PlayerBss.Plr_AddToBobble_w;
import static ab3d2.bss.PlayerBss.Plr_Decelerate_b;
import static ab3d2.bss.PlayerBss.Plr1_GunSelected_b;
import static ab3d2.bss.PlayerBss.Plr1_GunFrame_w;
import static ab3d2.bss.PlayerBss.Plr2_GunSelected_b;
import static ab3d2.bss.PlayerBss.Plr2_GunFrame_w;
import static ab3d2.bss.AnimBss.Anim_TempFrames_w;
import static ab3d2.bss.TablesBss.ObjRotated_vl;
import static ab3d2.bss.TablesBss.CurrentPointBrights_vl;
import static ab3d2.bss.ZoneBss.Zone_BrightTable_vl;
import static ab3d2.bss.AiBss.AI_VecObj_w;
import static ab3d2.data.TablesData.SINE_SIZE;
import static ab3d2.bss.VidBss.Vid_FullScreenTemp_b;
import static ab3d2.bss.PlayerBss.plr1_OldX_l;
import static ab3d2.bss.PlayerBss.plr1_OldZ_l;
import static ab3d2.bss.PlayerBss.plr2_OldX_l;
import static ab3d2.bss.PlayerBss.plr2_OldZ_l;
import static ab3d2.bss.PlayerBss.plr1_Teleported_b;
import static ab3d2.bss.PlayerBss.plr2_Teleported_b;
import static ab3d2.bss.PlayerBss.plr_GunSelected_b;
import static ab3d2.bss.TablesBss.ConstantTable_vl;
import static ab3d2.bss.ZoneBss.Zone_OrderTable_Barrier_w;
import static ab3d2.bss.PlayerBss.Plr1_Mouse_b;
import static ab3d2.bss.PlayerBss.Plr2_Mouse_b;
import static ab3d2.bss.PlayerBss.Plr1_Energy_w;
import static ab3d2.bss.PlayerBss.Plr2_Energy_w;
import static ab3d2.bss.PlayerBss.Plr1_Keys_b;
import static ab3d2.bss.PlayerBss.Plr1_Path_b;
import static ab3d2.bss.PlayerBss.Plr1_Joystick_b;
import static ab3d2.bss.PlayerBss.Plr2_Keys_b;
import static ab3d2.bss.PlayerBss.Plr2_Path_b;
import static ab3d2.bss.PlayerBss.Plr2_Joystick_b;
// --- Game_Begin (prologue, P2c) ---
import static ab3d2.bss.PlayerBss.Plr_MultiplayerType_b;
import static ab3d2.bss.PlayerBss.Plr1_StoodInTop_b;
import static ab3d2.bss.PlayerBss.Plr1_SnapHeight_l;
import static ab3d2.bss.PlayerBss.Plr1_SnapTargHeight_l;
import static ab3d2.bss.PlayerBss.Plr2_SnapTargHeight_l;
import static ab3d2.bss.PlayerBss.Plr2_SnapHeight_l;
import static ab3d2.bss.PlayerBss.Plr1_Ducked_b;
import static ab3d2.bss.PlayerBss.Plr2_Ducked_b;
import static ab3d2.bss.PlayerBss.plr1_TmpDucked_b;
import static ab3d2.bss.PlayerBss.plr2_TmpDucked_b;
import static ab3d2.bss.PlayerBss.Plr1_Weapons_vb;
import static ab3d2.bss.PlayerBss.Plr2_Weapons_vb;
import static ab3d2.bss.PlayerBss.Plr1_AimSpeed_l;
import static ab3d2.bss.PlayerBss.Plr2_AimSpeed_l;
import static ab3d2.bss.PlayerBss.Plr1_ObjectPtr_l;
import static ab3d2.bss.PlayerBss.Plr2_ObjectPtr_l;
import static ab3d2.bss.PlayerBss.Plr_ShotDataPtr_l;
import static ab3d2.bss.PlayerBss.Plr2_Fire_b;
import static ab3d2.bss.PlayerBss.Plr2_TmpFire_b;
import static ab3d2.bss.PlayerBss.Plr2_Used_b;
import static ab3d2.bss.PlayerBss.Plr2_TmpSpcTap_b;
import static ab3d2.bss.PlayerBss.plr1_Dead_b;
import static ab3d2.bss.PlayerBss.plr2_Dead_b;
import static ab3d2.bss.PlayerBss.Plr1_SnapXSpdVal_l;
import static ab3d2.bss.PlayerBss.Plr1_SnapZSpdVal_l;
import static ab3d2.bss.PlayerBss.Plr1_SnapYVel_l;
import static ab3d2.bss.PlayerBss.Plr2_SnapXSpdVal_l;
import static ab3d2.bss.PlayerBss.Plr2_SnapZSpdVal_l;
import static ab3d2.bss.PlayerBss.Plr2_SnapYVel_l;
import static ab3d2.bss.LevelBss.Lvl_GraphicsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_DoorDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_LiftDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_SwitchDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneGraphAddsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_DataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ControlPointCoordsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_NumControlPoints_w;
import static ab3d2.bss.LevelBss.Lvl_NumPoints_w;
import static ab3d2.bss.LevelBss.Lvl_PointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_NumZones_w;
import static ab3d2.bss.LevelBss.Lvl_ZoneBorderPointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_EdgeCount_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneEdgePtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l;
import static ab3d2.bss.LevelBss.AI_AlienShotDataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_NumObjectPoints_w;
import static ab3d2.bss.LevelBss.Lvl_ClipsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ConnectTablePtr_l;
import static ab3d2.bss.LevelBss.Lvl_ErrataPtr_l;
import static ab3d2.bss.TablesBss.Lvl_CompactMap_vl;
import static ab3d2.bss.TablesBss.Lvl_BigMap_vl;
import static ab3d2.bss.TablesBss.Aud_EmptyBuffer_vl;
import static ab3d2.bss.TablesBss.Aud_EmptyBufferEnd;
import static ab3d2.bss.TablesBss.PointBrightsPtr_l;
import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.bss.AiBss.AI_Damaged_vw;
import static ab3d2.bss.AiBss.AI_OtherAlienDataPtrs_vl;
import static ab3d2.bss.SystemBss.Sys_FrameNumber_l;
import static ab3d2.bss.SystemBss.Sys_PrevFrameTimeECV_q;
import static ab3d2.bss.VidBss.Vid_Screen1Ptr_l;
import static ab3d2.bss.VidBss.Vid_Screen2Ptr_l;
import static ab3d2.bss.VidBss.Vid_DisplayScreenPtr_l;
import static ab3d2.bss.VidBss.Vid_DrawScreenPtr_l;
import static ab3d2.modules.Music.mt_data;

/**
 * Traduction littérale de ab3d2_source/hires.s — PARTIE 1 : équates globaux.
 *
 * hires.s est le point d'entrée réel (_startup) et le fichier monolithique qui
 * inclut les bss/, data/ et la majorité des modules. Le code (rendu des sols,
 * boucle frame, etc.) sera traduit dans cette même classe, section par section.
 */
public final class Hires {

    public static final int CD32VER = 0;

    /** Hook de test : si >= 0, game_main_loop s'arrête après ce nombre de frames (sinon infini). */
    public static int loopFrameLimit = -1;


    // hardware/dmabits.i — bits DMACON utilisés par Game_Begin.
    public static final int DMAF_AUDIO = 0x000F;   // canaux audio 0-3
    public static final int DMAF_SETCLR = 0x8000;  // bit set/clear
    public static final int DMAF_MASTER = 0x0200;  // DMA master enable

    public static final int FS_HEIGHT_HACK = 1;        // 0xABADCAFE - Fullscreen height hack, set non-zero to enable
    public static final int DISPLAYMSGPORT_HACK = 1;   // AL - Level restart freeze hack
    public static final int SCREEN_TITLEBAR_HACK = 1;  // AL - Stop title bar interactions hack

    public static final int SCREEN_WIDTH = 320;
    public static final int SCREEN_HEIGHT = 256;

    // IFNE FS_HEIGHT_HACK (actif)
    public static final int FS_HEIGHT = SCREEN_HEIGHT - 16;   // 240
    public static final int FS_HEIGHT_C2P_DIFF = 8;
    // ELSE : FS_HEIGHT = SCREEN_HEIGHT-24, FS_HEIGHT_C2P_DIFF = 0

    public static final int FS_WIDTH = SCREEN_WIDTH;
    public static final int SMALL_WIDTH = 192;
    public static final int SMALL_HEIGHT = 160;

    public static final int VID_FAST_BUFFER_SIZE = SCREEN_WIDTH * SCREEN_HEIGHT + 15; // screen size plus alignment

    public static final int maxscrdiv = 8;
    public static final int max3ddiv = 5;
    public static final int PLR_STAND_HEIGHT = 12 * 1024;
    public static final int PLR_CROUCH_HEIGHT = 8 * 1024;
    public static final int scrheight = 80;
    public static final int intreqrl = 0x01f;

    public static final int PLR_MASTER = 'm';  // two player master
    public static final int PLR_SLAVE = 's';   // two player slave
    public static final int PLR_SINGLE = 'n';  // Single player

    public static final int QUIT_KEY = RawKeyMacros.RAWKEY_NUM_ASTERISK;

    private Hires() {
    }

    // ------------------------------------------------------------------
    // STUBS de routines de hires.s appelées par les modules déjà traduits.
    // Elles seront remplacées par la traduction ligne à ligne de hires.s.
    // ------------------------------------------------------------------

    // ==================================================================
    //  Draw_Flats (hires.s:3598..5826) — rendu sols/plafonds/eau.
    //  Décomposé selon les frontières jsr/rts de l'original :
    //   - Draw_Flats         : corps principal (entrée, clip, edge-walk, dofloor)
    //   - sideLoopSimple     : edge-walk non gouraud (sideloop)
    //   - sideLoopGouraud    : edge-walk gouraud (sideloopGOUR)
    //   - draw_FloorLine     : jsr (a5) — brightness plate puis pastfloorbright
    //   - pastfloorbright    : setup coords texture, tail-call draw_GoraudFloor/Water
    //   - draw_WaterSurface  : boucle texel eau (acrossscrnw, simple largeur)
    //  La boucle texel sol gouraud est dans modules/draw/DrawFloor.draw_GoraudFloor.
    //
    //  CODE MORT (non traduit, dispatch Vid_DoubleWidth_b forcé off par
    //  `bra.s .nodoub`) : ordinary/acrossscrn/backbefore, gouraudfloorDOUB,
    //  acrossscrngourD, draw_WaterSurfaceDouble, acrossscrnwD, backbeforew.
    // ==================================================================

    // hires.s:5190-5222 — .floorbright : 512*0 .. 512*31 (table locale de draw_FloorLine)
    private static final int[] FLOORBRIGHT = {
            512 * 0, 512 * 1, 512 * 2, 512 * 3, 512 * 4, 512 * 5, 512 * 6, 512 * 7,
            512 * 8, 512 * 9, 512 * 10, 512 * 11, 512 * 12, 512 * 13, 512 * 14, 512 * 15,
            512 * 16, 512 * 17, 512 * 18, 512 * 19, 512 * 20, 512 * 21, 512 * 22, 512 * 23,
            512 * 24, 512 * 25, 512 * 26, 512 * 27, 512 * 28, 512 * 29, 512 * 30, 512 * 31
    };

    /**
     * draw_FloorLine (hires.s:5159) — appelée via jsr (a5), une fois par
     * scanline en mode non gouraud. Entrées : d0 = Z de la ligne, a3 = ptr écran.
     * Calcule une brightness "plate" depuis lighttype + distance puis tombe dans
     * pastfloorbright.
     */
    static void draw_FloorLine(int d0, int a3) {
        int a0 = Mem.l(Draw_FloorTexturesPtr_l);       // move.l Draw_FloorTexturesPtr_l,a0
        a0 += (short) Mem.uw(whichtile);               // adda.w whichtile,a0
        int d1 = Mem.uw(lighttype);                    // move.w lighttype,d1
        Mem.wl(draw_Distance_l, d0);                   // move.l d0,draw_Distance_l
        int d2 = d0;                                   // move.l d0,d2
        d2 = d2 >> 2;                                  // asr.l #2,d2
        d2 = d2 >> 8;                                  // asr.l #8,d2
        d1 = setw(d1, d1 + 5);                         // add.w #5,d1
        d1 = setw(d1, d1 + d2);                        // add.w d2,d1
        if ((short) d1 < 0) {                          // bge.s .fixedbright
            d1 = 0;                                    // moveq #0,d1
        }
        // .fixedbright
        if ((short) d1 > 28) {                         // cmp.w #28,d1 ; ble .smallbright
            d1 = setw(d1, 28);                         // move.w #28,d1
        }
        // .smallbright
        int a1 = Mem.l(Draw_TexturePalettePtr_l);      // move.l Draw_TexturePalettePtr_l,a1
        a1 += 256 * 32;                                // add.l #256*32,a1
        a1 += (short) FLOORBRIGHT[(short) d1];         // add.w .floorbright(pc,d1.w*2),a1
        pastfloorbright(d0, a0, a1, a3);               // bra pastfloorbright
    }

    /**
     * pastfloorbright (hires.s:5234) — calcule la position/pente dans l'espace
     * texture du sol depuis la direction du regard (Vis_SinVal/CosVal), applique
     * le multiplicateur fullscreen, puis tail-call la boucle texel.
     * Entrées : d0 = Z, a0 = texture, a1 = table de shading, a3 = ptr écran.
     */
    static void pastfloorbright(int d0, int a0, int a1, int a3) {
        int d1 = muls(d0, Mem.w(Vis_CosVal_w));        // move.l d0,d1 ; muls Vis_CosVal_w,d1
        int d2 = muls(d0, Mem.w(Vis_SinVal_w));        // move.l d0,d2 ; muls Vis_SinVal_w,d2
        d2 = -d2;                                      // neg.l d2
        d2 = d2 >> 2;                                  // asr.l #2,d2
        d1 = d1 >> 2;                                  // asr.l #2,d1

        // scaleprog
        int d3 = Mem.uw(scaleval);                     // move.w scaleval(pc),d3
        if ((short) d3 != 0) {                         // beq.s .samescale
            if ((short) d3 > 0) {                      // bgt.s .scaledown
                d1 = d1 << ((short) d3 & 31);          // asl.l d3,d1
                d2 = d2 << ((short) d3 & 31);          // asl.l d3,d2
            } else {
                int sh = -(short) d3 & 31;             // neg.w d3
                d1 = d1 >> sh;                         // asr.l d3,d1
                d2 = d2 >> sh;                         // asr.l d3,d2
            }
        }

        // .samescale
        d3 = d1;                                       // move.l d1,d3
        int d6 = d3;                                   // move.l d3,d6
        int d5 = d3;                                   // move.l d3,d5
        d6 = d6 >> 1;                                  // asr.l #1,d6
        d3 = d3 + d6;                                  // add.l d6,d3
        d3 = d3 >> 1;                                  // asr.l #1,d3   (cos*0.75)
        int d4 = d2;                                   // move.l d2,d4
        d6 = d4;                                       // move.l d4,d6
        d6 = d6 >> 1;                                  // asr.l #1,d6
        d6 = d6 + d4;                                  // add.l d4,d6
        d6 = d6 >> 1;                                  // asr.l #1,d6   (-sin*0.75)
        d4 = d4 + d3;                                  // add.l d3,d4
        d4 = -d4;                                      // neg.l d4
        d5 = d5 - d6;                                  // sub.l d6,d5
        d4 = d4 + Mem.l(sxoff);                        // add.l sxoff,d4
        d5 = d5 + Mem.l(szoff);                        // add.l szoff,d5

        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; beq.s .nob
            // --- FULLSCREEN ---
            d6 = setw(0, Mem.uw(leftedge));            // moveq #0,d6 ; move.w leftedge,d6
            if ((short) d6 != 0) {                     // beq.s .nomultleftB
                d6 = muls(d6, 1229);                   // muls #1229,d6
                d6 = d6 >> 8;                          // asr.l #8,d6
                d6 = d6 >> 3;                          // asr.l #3,d6
                int sa4 = d1;                          // move.l d1,a4
                int sa5 = d2;                          // move.l d2,a5
                d1 = d1 * d6;                          // muls.l d6,d1
                d1 = d1 >> 7;                          // asr.l #7,d1
                d4 = d4 + d1;                          // add.l d1,d4
                d2 = d2 * d6;                          // muls.l d6,d2
                d2 = d2 >> 7;                          // asr.l #7,d2
                d5 = d5 + d2;                          // add.l d2,d5
                d1 = sa4;                              // move.l a4,d1
                d2 = sa5;                              // move.l a5,d2
                d6 = setw(d6, Mem.uw(leftedge));       // move.w leftedge,d6
            }
            // .nomultleftB
            Mem.ww(startsmoothx, d4);                  // move.w d4,startsmoothx
            Mem.ww(startsmoothz, d5);                  // move.w d5,startsmoothz
            d4 = d4 >> 8;                              // asr.l #8,d4
            d5 = d5 << 8;                              // asl.l #8,d5
            d5 = setw(d5, d4);                         // move.w d4,d5
            d1 = d1 >> 6;                              // asr.l #6,d1
            d2 = d2 >> 6;                              // asr.l #6,d2
            d1 = d1 * 77;                              // muls.l #77,d1
            d2 = d2 * 77;                              // muls.l #77,d2
            d1 = d1 >> 8;                              // asr.l #8,d1
            d2 = d2 >> 8;                              // asr.l #8,d2
            // bra.s doneallmult
        } else {
            // --- .nob : smallscreen ---
            d6 = setw(0, Mem.uw(leftedge));            // moveq #0,d6 ; move.w leftedge,d6
            if ((short) d6 != 0) {                     // beq.s nomultleft
                int sa4 = d1;                          // move.l d1,a4
                int sa5 = d2;                          // move.l d2,a5
                d1 = d1 * d6;                          // muls.l d6,d1
                d1 = d1 >> 7;                          // asr.l #7,d1
                d4 = d4 + d1;                          // add.l d1,d4
                d2 = d2 * d6;                          // muls.l d6,d2
                d2 = d2 >> 7;                          // asr.l #7,d2
                d5 = d5 + d2;                          // add.l d2,d5
                d1 = sa4;                              // move.l a4,d1
                d2 = sa5;                              // move.l a5,d2
                d6 = setw(d6, Mem.uw(leftedge));       // move.w leftedge,d6
            }
            // nomultleft
            Mem.ww(startsmoothx, d4);                  // move.w d4,startsmoothx
            Mem.ww(startsmoothz, d5);                  // move.w d5,startsmoothz
            d4 = d4 >> 8;                              // asr.l #8,d4
            d5 = d5 << 8;                              // asl.l #8,d5
            d5 = setw(d5, d4);                         // move.w d4,d5
            d1 = d1 >> 7;                              // asr.l #7,d1
            d2 = d2 >> 7;                              // asr.l #7,d2
        }

        // doneallmult
        d2 = d2 << 8;                                  // asl.l #8,d2
        d1 = d1 >> 8;                                  // asr.l #8,d1
        d2 = setw(d2, d1);                             // move.w d1,d2  (incr UV : T<<16 | S)
        d1 = 0x3fff3fff;                               // move.l #$3fff3fff,d1  (masque UV)
        d5 = d5 & d1;                                  // and.l d1,d5
        // bra.s .nodoub
        int a2 = (short) d6;                           // move.w d6,a2  (leftedge)
        d3 = setw(d3, Mem.uw(rightedge));              // move.w rightedge(pc),d3
        a3 = a3 + a2;                                  // lea (a3,a2.w),a3
        int d7 = setw(0, d3);                          // move.w d3,d7
        d7 = setw(d7, d7 - a2);                        // sub.w a2,d7  (largeur)
        // intofirststrip / allintofirst
        // (move.w startsmoothx,d3 — d3 scratch pour les boucles texel)

        // tstwat
        if (Mem.b(draw_UseWater_b) != 0) {             // tst.b draw_UseWater_b ; bne draw_WaterSurface
            draw_WaterSurface(d1, d2, d5, d7, a3);
            return;
        }
        DrawFloor.draw_GoraudFloor(d1, d2, d5, d7, a0, a1, a3); // bra draw_GoraudFloor
    }

    /**
     * draw_WaterSurface (hires.s:5654) — boucle texel pour les surfaces d'eau
     * (largeur simple). Échantillonne un déplacement dans la frame d'eau, lit le
     * pixel de la ligne de référence décalée (a6) puis mappe via la table de
     * miroitement. Entrées : d1 = masque UV, d2 = incrément UV, d5 = UV, d7 =
     * largeur-1, a3 = ptr écran. CODE MORT non traduit : backbeforew.
     */
    static void draw_WaterSurface(int d1, int d2, int d5, int d7, int a3) {
        int d4 = d1;                                   // move.l d1,d4
        d5 = d5 + Mem.l(wateroff);                     // add.l wateroff,d5
        int a1 = Mem.l(Draw_TexturePalettePtr_l);      // move.l Draw_TexturePalettePtr_l,a1
        a1 += 256 * 16;                                // add.l #256*16,a1  (mi-chemin glare)
        int d0 = Mem.l(draw_Distance_l);               // move.l draw_Distance_l,d0
        d0 = d0 & 0x3f00;                              // and.l #$3f00,d0
        d0 = setw(d0, d0 + d0);                        // add.w d0,d0
        if ((short) d0 >= 5 * 512) {                   // cmp.w #5*512,d0 ; blt.s .notoowater
            d0 = setw(d0, 5 * 512);                    // move.w #5*512,d0
        }
        // .notoowater
        a1 += (short) d0;                              // adda.w d0,a1
        d0 = Mem.l(draw_Distance_l);                   // move.l draw_Distance_l,d0
        d0 = setw(d0, (d0 & 0xFFFF) << 7);             // asl.w #7,d0
        d0 = setw(d0, d0 + Mem.uw(wtan));              // add.w wtan,d0
        d0 = setw(d0, d0 & 8191);                      // AMOD_I d0 (and.w #SINTAB_MASK_IDX)
        int a0 = SinCosTable_vw;                       // move.l #SinCosTable_vw,a0
        d0 = setw(d0, Mem.uw(a0 + (d0 & 0xFFFF)));     // move.w (a0,d0.w),d0  (offset OCTET)
        d0 = (short) d0;                               // ext.l d0
        int d3 = Mem.l(draw_Distance_l);               // move.l draw_Distance_l,d3
        d3 = setw(d3, d3 + 300);                       // add.w #300,d3
        d0 = divs(d0, d3);                             // divs d3,d0
        d0 = setw(d0, ((short) d0) >> 5);              // asr.w #5,d0
        d0 = setw(d0, d0 + 4);                         // addq #4,d0
        if ((short) d0 >= (short) Mem.uw(disttobot)) { // cmp.w disttobot,d0 ; blt.s oknotoffbototot
            d0 = setw(d0, Mem.uw(disttobot));          // move.w disttobot,d0
            d0 = setw(d0, d0 - 1);                     // subq #1,d0
        }
        // oknotoffbototot
        if (Mem.b(Vid_DoubleHeight_b) != 0) {          // tst.b Vid_DoubleHeight_b ; beq.s .nodoub
            d0 = setb(d0, d0 & 0xFE);                  // and.b #$fe,d0
        }
        // .nodoub
        d0 = muls(d0, Hires.SCREEN_WIDTH);             // muls #SCREEN_WIDTH,d0
        if ((short) Mem.uw(above) != 0) {              // tst.w above ; beq.s nonnnnneg
            d0 = -d0;                                  // neg.l d0
        }
        // nonnnnneg
        int a6 = d0;                                   // move.l d0,a6
        a0 = Mem.l(draw_WaterFramePtr_l);              // move.l draw_WaterFramePtr_l,a0
        d3 = setw(d3, Mem.uw(startsmoothx));           // move.w startsmoothx,d3
        d7 = setw(d7, d7 - 1);                         // dbra d7,acrossscrnw
        while ((short) d7 != -1) {
            // acrossscrnw (5729) :
            d3 = setw(d3, d5);                         // move.w d5,d3
            int d6 = d5;                               // move.l d5,d6
            d3 = setw(d3, (d3 & 0xFFFF) >>> 8);        // lsr.w #8,d3
            d6 = swap(d6);                             // swap d6
            d6 = setb(d6, d3);                         // move.b d3,d6
            d0 = setw(d0, Mem.uw(a0 + ((short) d6) * 4)); // move.w (a0,d6.w*4),d0
            d0 = setw(d0, d0 + d0);                    // add.w d0,d0
            d5 += d2;                                  // add.l d2,d5
            d0 = setb(d0, Mem.ub(a3 + (short) a6));    // move.b (a3,a6.w),d0
            d5 &= d4;                                  // and.l d4,d5
            Mem.wb(a3, Mem.ub(a1 + (d0 & 0xFFFF)));    // move.b (a1,d0.w),(a3)+
            a3 += 1;
            d7 = setw(d7, d7 - 1);                     // dbra d7,acrossscrnw
        }
        // rts
    }

    /**
     * pastFlats (hires.s:4425 pastsides → 5132) — une fois les côtés walkés dans
     * les Left/Right(Side|Bright)Tables, calcule l'offset texture du joueur
     * (sxoff/szoff), clippe la portée verticale (pix1h/pix2h selon
     * Vid_DoubleHeight_b) puis dessine chaque scanline (dofloor/dofloornoclip/
     * dofloorGOUR/dofloornoclipGOUR). Entrée a0 = ptr sur l'index final ;
     * renvoie a0 après le record (= valeur empilée à pix1h/pix2h).
     */
    static int pastFlats(int a0) {
        int d0, d1, d3, d6, d7;
        int a2, a4, a6;

        // pastsides (4425)
        a0 += 2;                                       // addq #2,a0
        Mem.ww(linedir, Hires.SCREEN_WIDTH);           // move.w #SCREEN_WIDTH,linedir
        a6 = Mem.l(Vid_FastBufferPtr_l);               // move.l Vid_FastBufferPtr_l,a6
        a6 = a6 + Mem.l(BIGMIDDLEY);                    // add.l BIGMIDDLEY,a6
        d6 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d6  (floor scale)
        d6 = setw(d6, d6 + Mem.uw(SMALLIT));           // add.w SMALLIT,d6
        Mem.ww(scaleval, d6);                          // move.w d6,scaleval
        d6 = setw(d6, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d6
        Mem.ww(whichtile, d6);                         // move.w d6,whichtile
        d6 = setw(d6, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d6
        d6 = setw(d6, d6 + Mem.uw(Zone_Bright_w));     // add.w Zone_Bright_w,d6
        Mem.ww(lighttype, d6);                         // move.w d6,lighttype
        d6 = setw(d6, Mem.uw(above));                  // move.w above(pc),d6
        if ((short) d6 != 0) {                         // beq groundfloor
            Mem.ww(linedir, -Hires.SCREEN_WIDTH);      // move.w #-SCREEN_WIDTH,linedir
            a6 = a6 - Hires.SCREEN_WIDTH;              // suba.w #SCREEN_WIDTH,a6
        }

        // groundfloor (4452)
        d6 = setw(0, Mem.uw(Plr_XOff_l));              // move.w Plr_XOff_l,d6
        d7 = setw(0, Mem.uw(Plr_ZOff_l));              // move.w Plr_ZOff_l,d7
        d7 = setw(d7, d7 + Mem.uw(xwobxoff));          // add.w xwobxoff,d7
        d6 = setw(d6, d6 + Mem.uw(xwobzoff));          // add.w xwobzoff,d6
        d6 = swap(d6);                                 // swap d6
        d7 = swap(d7);                                 // swap d7
        d6 = setw(d6, 0);                              // clr.w d6
        d7 = setw(d7, 0);                              // clr.w d7
        d6 = d6 >> 1;                                  // asr.l #1,d6
        d7 = d7 >> 1;                                  // asr.l #1,d7

        // .donsht (4499)
        d3 = setw(0, Mem.uw(scaleval));                // move.w scaleval(pc),d3
        if ((short) d3 != 0) {                         // beq.s .samescale
            if ((short) d3 > 0) {                      // bgt.s .scaledown
                d6 = d6 << ((short) d3 & 31);          // asl.l d3,d6
                d7 = d7 << ((short) d3 & 31);          // asl.l d3,d7
            } else {
                int sh = -(short) d3 & 31;             // neg.w d3
                d6 = d6 >> sh;                         // asr.l d3,d6
                d7 = d7 >> sh;                         // asr.l d3,d7
            }
        }
        // .samescale (4510)
        Mem.wl(sxoff, d6);                             // move.l d6,sxoff
        Mem.wl(szoff, d7);                             // move.l d7,szoff

        // pastscale (4545)
        if (Mem.b(drawit) == 0) {                      // tst.b drawit(pc) ; beq dontdrawfloor
            return a0;                                 // dontdrawfloor: rts
        }

        int savedA0 = a0;                              // move.l a0,-(a7)  (pix1h/pix2h)
        a4 = LeftSideTable_vw;                         // move.l #LeftSideTable_vw,a4
        d1 = setw(0, Mem.uw(top));                     // move.w top(pc),d1

        if (Mem.b(Vid_DoubleHeight_b) != 0) {
            // ----- Vid_DoubleHeight_b rendering (4552) -----
            int dlin = setw(0, Mem.uw(linedir));       // move.w linedir,d1 -> dlin
            Mem.ww(linedir, Mem.uw(linedir) + dlin);   // add.w d1,linedir  (x2)

            if ((short) Mem.uw(above) != 0) {          // tst.w above ; beq.s .clipfloor
                // ceiling clip (4569)
                d7 = setw(0, Mem.uw(Vid_CentreY_w));   // move.w Vid_CentreY_w,d7
                if ((d7 & 1) == 0) {                   // btst #0,d7 ; bne.s .evenMiddleRoof
                    a6 = a6 - Hires.SCREEN_WIDTH;      // sub.w #SCREEN_WIDTH,a6
                }
                // .evenMiddleRoof
                d7 = setw(d7, d7 - 1);                 // subq #1,d7
                d7 = setw(d7, d7 - d1);                // sub.w d1,d7
                Mem.ww(disttobot, d7);                 // move.w d7,disttobot
                d7 = setw(0, Mem.uw(bottom));          // move.w bottom(pc),d7
                d3 = setw(0, Mem.uw(Vid_CentreY_w));   // move.w Vid_CentreY_w,d3
                int d4 = setw(0, d3);                  // move.w d3,d4
                d3 = setw(d3, d3 - Mem.uw(draw_TopClip_w));    // sub.w draw_TopClip_w,d3
                d4 = setw(d4, d4 - Mem.uw(draw_BottomClip_w)); // sub.w draw_BottomClip_w,d4
                if ((short) d1 >= (short) d3) return savedA0;  // cmp.w d3,d1 ; bge predontdrawfloor
                if ((short) d7 < (short) d4) return savedA0;   // cmp.w d4,d7 ; blt predontdrawfloor
                if ((short) d1 < (short) d4) d1 = setw(d1, d4); // cmp.w d4,d1 ; bge .nocliptoproof ; move.w d4,d1
                if ((short) d7 >= (short) d3) d7 = setw(d7, d3); // cmp.w d3,d7 ; blt .doneclip ; move.w d3,d7
            } else {
                // .clipfloor (4600)
                d7 = setw(0, Mem.uw(Vid_BottomY_w));   // move.w Vid_BottomY_w,d7
                int d4 = setw(0, Mem.uw(Vid_CentreY_w)); // move.w Vid_CentreY_w,d4
                if ((d4 & 1) != 0) {                   // btst #0,d4 ; beq.s .evenMiddleFloor
                    a6 = a6 + Hires.SCREEN_WIDTH;      // add.w #SCREEN_WIDTH,a6
                }
                // .evenMiddleFloor
                d7 = setw(d7, d7 - d4);                // sub.w d4,d7
                d7 = setw(d7, d7 - 1);                 // subq #1,d7
                d7 = setw(d7, d7 - d1);                // sub.w d1,d7
                Mem.ww(disttobot, d7);                 // move.w d7,disttobot
                d7 = setw(0, Mem.uw(bottom));          // move.w bottom(pc),d7
                d4 = setw(0, Mem.uw(draw_BottomClip_w)); // move.w draw_BottomClip_w,d4
                d4 = setw(d4, d4 - Mem.uw(Vid_CentreY_w)); // sub.w Vid_CentreY_w,d4
                if ((short) d1 >= (short) d4) return savedA0; // cmp.w d4,d1 ; bge predontdrawfloor
                d3 = setw(0, Mem.uw(draw_TopClip_w));  // move.w draw_TopClip_w,d3
                d3 = setw(d3, d3 - Mem.uw(Vid_CentreY_w)); // sub.w Vid_CentreY_w,d3
                if ((short) d1 < (short) d3) d1 = setw(d1, d3); // cmp.w d3,d1 ; bge .nocliptopfloor ; move.w d3,d1
                if ((short) d7 <= (short) d3) return savedA0; // cmp.w d3,d7 ; ble predontdrawfloor
                if ((short) d7 >= (short) d4) d7 = setw(d7, d4); // cmp.w d4,d7 ; blt .noclipbotfloor ; move.w d4,d7
            }
            // .doneclip (4634)
            a4 = a4 + (short) d1 * 2;                  // lea (a4,d1*2),a4
            d7 = setw(d7, d7 + 1);                     // addq #1,d7
            d7 = setw(d7, d7 - d1);                    // sub.w d1,d7
            d7 = setw(d7, ((short) d7) >> 1);          // asr.w #1,d7  (/2 doubleheight)
            if ((short) d7 <= 0) return savedA0;       // ble predontdrawfloor
            d1 = setw(d1, ((short) d1) >> 1);          // asr.w #1,d1  (top/2)
            d0 = setw(0, Mem.uw(View2FloorDist));      // move.w View2FloorDist,d0
            if (Mem.b(Vid_FullScreen_b) != 0) {        // tst.b Vid_FullScreen_b ; beq.s .smallscreen
                d0 = muls(d0, 107);                    // muls #107,d0
            } else {
                d0 = (short) d0;                       // ext.l d0
                d0 = d0 << 6;                          // lsl.l #6,d0
            }
            a2 = d0;                                    // move.l d0,a2
            d0 = setw(d0, d1);                         // move.w d1,d0
            if ((short) d0 == 0) d0 = 1;               // bne.s .notzero ; moveq #1,d0
            d0 = setw(d0, d0 + d0);                    // add.w d0,d0
            d1 = muls(Mem.uw(linedir), d1);            // muls.w linedir,d1
            a6 = a6 + d1;                              // add.l d1,a6
            Mem.ww(tonextline, 4);                     // move.w #4,tonextline
        } else {
            // ----- pix1h : regular Nx1 (4680) -----
            if ((short) Mem.uw(above) != 0) {          // tst.w above ; beq.s clipfloor
                // roof clip (4691)
                d7 = setw(0, Mem.uw(Vid_CentreY_w));   // move.w Vid_CentreY_w,d7
                d7 = setw(d7, d7 - 1);                 // subq #1,d7
                d7 = setw(d7, d7 - d1);                // sub.w d1,d7
                Mem.ww(disttobot, d7);                 // move.w d7,disttobot
                d7 = setw(0, Mem.uw(bottom));          // move.w bottom(pc),d7
                d3 = setw(0, Mem.uw(Vid_CentreY_w));   // move.w Vid_CentreY_w,d3
                int d4 = setw(0, d3);                  // move.w d3,d4
                d3 = setw(d3, d3 - Mem.uw(draw_TopClip_w));    // sub.w draw_TopClip_w,d3
                d4 = setw(d4, d4 - Mem.uw(draw_BottomClip_w)); // sub.w draw_BottomClip_w,d4
                if ((short) d1 >= (short) d3) return savedA0;  // bge predontdrawfloor
                if ((short) d7 < (short) d4) return savedA0;   // blt predontdrawfloor
                if ((short) d1 < (short) d4) d1 = setw(d1, d4); // bge .nocliptoproof ; move.w d4,d1
                if ((short) d7 >= (short) d3) d7 = setw(d7, d3); // blt doneclip ; move.w d3,d7
            } else {
                // clipfloor (4714)
                d7 = setw(0, Mem.uw(Vid_BottomY_w));   // move.w Vid_BottomY_w,d7
                d7 = setw(d7, d7 - Mem.uw(Vid_CentreY_w)); // sub.w Vid_CentreY_w,d7
                d7 = setw(d7, d7 - 1);                 // subq #1,d7
                d7 = setw(d7, d7 - d1);                // sub.w d1,d7
                Mem.ww(disttobot, d7);                 // move.w d7,disttobot
                d7 = setw(0, Mem.uw(bottom));          // move.w bottom(pc),d7
                int d4 = setw(0, Mem.uw(draw_BottomClip_w)); // move.w draw_BottomClip_w,d4
                d4 = setw(d4, d4 - Mem.uw(Vid_CentreY_w)); // sub.w Vid_CentreY_w,d4
                if ((short) d1 >= (short) d4) return savedA0; // bge predontdrawfloor
                d3 = setw(0, Mem.uw(draw_TopClip_w));  // move.w draw_TopClip_w,d3
                d3 = setw(d3, d3 - Mem.uw(Vid_CentreY_w)); // sub.w Vid_CentreY_w,d3
                if ((short) d1 < (short) d3) d1 = setw(d1, d3); // bge .nocliptopfloor ; move.w d3,d1
                if ((short) d7 <= (short) d3) return savedA0; // ble predontdrawfloor
                if ((short) d7 >= (short) d4) d7 = setw(d7, d4); // blt .noclipbotfloor ; move.w d4,d7
            }
            // doneclip (4739)
            a4 = a4 + (short) d1 * 2;                  // lea (a4,d1*2),a4
            d0 = setw(0, Mem.uw(View2FloorDist));      // move.w View2FloorDist,d0
            if (Mem.b(Vid_FullScreen_b) != 0) {        // tst.b Vid_FullScreen_b ; beq.s .smallscreen
                d0 = muls(d0, 107);                    // muls #107,d0
            } else {
                d0 = (short) d0;                       // ext.l d0
                d0 = d0 << 6;                          // lsl.l #6,d0
            }
            a2 = d0;                                    // move.l d0,a2
            d7 = setw(d7, d7 - d1);                    // sub.w d1,d7  (number of lines)
            if ((short) d7 <= 0) return savedA0;       // ble predontdrawfloor
            d0 = setw(d0, d1);                         // move.w d1,d0
            if ((short) d0 == 0) d0 = 1;               // bne.s .notzero ; moveq #1,d0
            d1 = muls(Mem.uw(linedir), d1);            // muls linedir,d1
            a6 = a6 + d1;                              // add.l d1,a6
            Mem.ww(tonextline, 2);                     // move.w #2,tonextline
        }

        // pix2h (4777) — dispatch boucle de lignes.
        // d7 = compteur de lignes (préservé par movem autour du jsr dans l'original ;
        // ici on passe le Z par valeur, donc d0/d7 ne sont jamais écrasés par l'appel).
        boolean gour = Mem.b(draw_UseGouraudFlats_b) != 0; // tst.b ; bne dogourfloor
        boolean clip = Mem.b(anyclipping) != 0;            // tst.b anyclipping ; beq dofloornoclip(GOUR)

        while (true) {
            int d2;
            boolean drawLine = true;

            if (!gour) {
                // dofloor (clip) / dofloornoclip
                if (clip) {
                    int d3c = setw(0, Mem.uw(Draw_LeftClip_w));  // move.w Draw_LeftClip_w,d3
                    int d4c = setw(0, Mem.uw(Draw_RightClip_w)); // move.w Draw_RightClip_w,d4
                    d2 = setw(0, Mem.uw(a4 + (RightSideTable_vw - LeftSideTable_vw))); // move.w Rt-Lt(a4),d2
                    d2 = setw(d2, d2 + 1);             // addq #1,d2
                    if ((short) d2 <= (short) d3c) {   // cmp.w d3,d2 ; ble nodrawline
                        drawLine = false;
                    } else {
                        if ((short) d2 > (short) d4c) d2 = setw(d2, d4c); // cmp.w d4,d2 ; ble noclipright ; move.w d4,d2
                        d1 = setw(0, Mem.uw(a4));      // move.w (a4),d1
                        if ((short) d1 >= (short) d4c) { // cmp.w d4,d1 ; bge nodrawline
                            drawLine = false;
                        } else {
                            if ((short) d1 < (short) d3c) d1 = setw(d1, d3c); // cmp.w d3,d1 ; bge noclipleft ; move.w d3,d1
                            if ((short) d2 <= (short) d1) { // cmp.w d1,d2 ; ble nodrawline
                                drawLine = false;
                            } else {
                                Mem.ww(leftedge, d1);  // move.w d1,leftedge
                                Mem.ww(rightedge, d2); // move.w d2,rightedge
                            }
                        }
                    }
                } else {
                    d2 = setw(0, Mem.uw(a4 + (RightSideTable_vw - LeftSideTable_vw))); // move.w Rt-Lt(a4),d2
                    d2 = setw(d2, d2 + 1);             // addq #1,d2
                    d1 = setw(0, Mem.uw(a4));          // move.w (a4),d1
                    Mem.ww(leftedge, d1);              // move.w d1,leftedge
                    Mem.ww(rightedge, d2);             // move.w d2,rightedge
                }
                if (drawLine) {
                    int a3 = a6;                       // move.l a6,a3
                    int zt = a2;                       // move.l a2,d7
                    zt = zt >>> 4;                     // lsr.l #4,d7
                    zt = mulu(zt, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2)); // mulu.w OneOverN(pc,d0*2),d7
                    zt = zt >>> 8;                     // lsr.l #8,d7
                    draw_FloorLine(zt, a3);            // move.l d7,d0 ; jsr (a5)
                }
            } else {
                // dofloorGOUR (clip) / dofloornoclipGOUR
                if (clip) {
                    int d3c = setw(0, Mem.uw(Draw_LeftClip_w));  // move.w Draw_LeftClip_w,d3
                    int d4c = setw(0, Mem.uw(Draw_RightClip_w)); // move.w Draw_RightClip_w,d4
                    d2 = setw(0, Mem.uw(a4 + (RightSideTable_vw - LeftSideTable_vw))); // move.w Rt-Lt(a4),d2
                    int d5g = setw(0, d2);             // move.w d2,d5
                    d5g = setw(d5g, d5g - Mem.uw(a4)); // sub.w (a4),d5
                    d5g = setw(d5g, d5g + 1);          // addq #1,d5
                    int d6g = 0;                       // moveq #0,d6
                    d2 = setw(d2, d2 + 1);             // addq #1,d2
                    if ((short) d2 <= (short) d3c) {   // cmp.w d3,d2 ; ble nodrawlineGOUR
                        drawLine = false;
                    } else {
                        if ((short) d2 > (short) d4c) d2 = setw(d2, d4c); // cmp.w d4,d2 ; ble nocliprightGOUR ; move.w d4,d2
                        d1 = setw(0, Mem.uw(a4));      // move.w (a4),d1
                        if ((short) d1 >= (short) d4c) { // cmp.w d4,d1 ; bge nodrawlineGOUR
                            drawLine = false;
                        } else {
                            if ((short) d1 < (short) d3c) { // cmp.w d3,d1 ; bge noclipleftGOUR
                                d6g = setw(d6g, d3c);  // move.w d3,d6
                                d6g = setw(d6g, d6g - 1); // subq #1,d6
                                d6g = setw(d6g, d6g - d1); // sub.w d1,d6
                                d1 = setw(d1, d3c);    // move.w d3,d1
                            }
                            if ((short) d2 <= (short) d1) { // cmp.w d1,d2 ; ble nodrawlineGOUR
                                drawLine = false;
                            } else {
                                Mem.ww(leftedge, d1);  // move.w d1,leftedge
                                Mem.ww(rightedge, d2); // move.w d2,rightedge
                                int d2d = a2;          // move.l a2,d2
                                d2d = d2d >>> 4;       // lsr.l #4,d2
                                d2d = mulu(d2d, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2)); // mulu.w OneOverN(pc,d0*2),d2
                                d2d = d2d >>> 8;       // lsr.l #8,d2
                                Mem.wl(draw_Distance_l, d2d); // move.l d2,draw_Distance_l
                                d2d = d2d >> 7;        // asr.l #7,d2
                                d2d = d2d >> 2;        // asr.l #2,d2
                                int d1b = 0, d3b = 0;  // moveq #0,d1 ; moveq #0,d3
                                d1b = setw(d1b, Mem.uw(a4 + (LeftBrightTable_vw - LeftSideTable_vw))); // move.w LBr(a4),d1
                                d1b = setw(d1b, d1b + d2d); // add.w d2,d1
                                if ((short) d1b < 0) d1b = 0;      // bge .okbl ; moveq #0,d1
                                if ((short) d1b > 30) d1b = setw(d1b, 30); // ble .okdl ; move.w #30,d1
                                d3b = setw(d3b, Mem.uw(a4 + (RightBrightTable_vw - LeftSideTable_vw))); // move.w RBr(a4),d3
                                d3b = setw(d3b, d3b + d2d); // add.w d2,d3
                                if ((short) d3b < 0) d3b = 0;      // bge .okbr ; moveq #0,d3
                                if ((short) d3b > 30) d3b = setw(d3b, 30); // ble .okdr ; move.w #30,d3
                                d3b = setw(d3b, d3b - d1b); // sub.w d1,d3
                                d1b = setw(d1b, (d1b & 0xFFFF) << 8); // asl.w #8,d1
                                Mem.ww(leftbright, d1b); // move.w d1,leftbright
                                d3b = swap(d3b);       // swap d3
                                if (d3b > 0) {         // tst.l d3 ; bgt .OKITSPOSALREADY
                                    d3b = d3b >> 6;    // asr.l #6,d3
                                    d3b = divs(d3b, d5g); // divs d5,d3
                                } else {
                                    d3b = -d3b;        // neg.l d3
                                    d3b = d3b >> 6;    // asr.l #6,d3
                                    d3b = divs(d3b, d5g); // divs d5,d3
                                    d3b = setw(d3b, -(short) d3b); // neg.w d3
                                }
                                // .OKNOWITSNEG
                                d6g = muls(d6g, d3b);  // muls d3,d6
                                d6g = setw(d6g, d6g + 256 * 4); // add.w #256*4,d6
                                d6g = setw(d6g, ((short) d6g) >> 2); // asr.w #2,d6
                                d6g = setb(d6g, 0);    // clr.b d6
                                d6g = setw(d6g, d6g + Mem.uw(leftbright)); // add.w leftbright,d6
                                if ((short) d6g < 0) d6g = 0; // bge .oklbnn ; moveq #0,d6
                                Mem.ww(leftbright, d6g); // move.w d6,leftbright
                                d3b = (short) d3b;     // ext.l d3
                                d3b = d3b >> 2;        // asr.l #2,d3
                                Mem.ww(brightspd, d3b); // move.w d3,brightspd
                                int a3 = a6;           // move.l a6,a3
                                int z = Mem.l(draw_Distance_l); // move.l draw_Distance_l,d0
                                int a1g = Mem.l(Draw_TexturePalettePtr_l) + 256 * 32; // a1
                                int a0g = Mem.l(Draw_FloorTexturesPtr_l) + (short) Mem.uw(whichtile); // a0
                                pastfloorbright(z, a0g, a1g, a3); // jsr pastfloorbright
                            }
                        }
                    }
                } else {
                    // dofloornoclipGOUR (5050)
                    d2 = setw(0, Mem.uw(a4 + (RightSideTable_vw - LeftSideTable_vw))); // move.w Rt-Lt(a4),d2
                    d2 = setw(d2, d2 + 1);             // addq #1,d2
                    d1 = setw(0, Mem.uw(a4));          // move.w (a4),d1
                    Mem.ww(leftedge, d1);              // move.w d1,leftedge
                    Mem.ww(rightedge, d2);             // move.w d2,rightedge
                    d2 = setw(d2, d2 - d1);            // sub.w d1,d2
                    int d6g = a2;                      // move.l a2,d6
                    d6g = d6g >> 4;                    // asr.l #4,d6
                    d6g = mulu(d6g, Mem.uw(OneOverN_vw + (d0 & 0xFFFF) * 2)); // mulu.w OneOverN(pc,d0*2),d6
                    d6g = d6g >> 8;                    // asr.l #8,d6
                    int d5g = d6g;                     // move.l d6,d5
                    d5g = d5g >> 7;                    // asr.l #7,d5
                    d5g = d5g >> 2;                    // asr.l #2,d5
                    int d1b = 0, d3b = 0;              // moveq #0,d1 ; moveq #0,d3
                    d1b = setw(d1b, Mem.uw(a4 + (LeftBrightTable_vw - LeftSideTable_vw))); // move.w LBr(a4),d1
                    d1b = setw(d1b, d1b + d5g);        // add.w d5,d1
                    if ((short) d1b < 0) d1b = 0;      // bge .okbl ; moveq #0,d1
                    if ((short) d1b > 30) d1b = setw(d1b, 30); // ble .okdl ; move.w #30,d1
                    d3b = setw(d3b, Mem.uw(a4 + (RightBrightTable_vw - LeftSideTable_vw))); // move.w RBr(a4),d3
                    d3b = setw(d3b, d3b + d5g);        // add.w d5,d3
                    if ((short) d3b < 0) d3b = 0;      // bge .okbr ; moveq #0,d3
                    if ((short) d3b > 30) d3b = setw(d3b, 30); // ble .okdr ; move.w #30,d3
                    d3b = setw(d3b, d3b - d1b);        // sub.w d1,d3
                    d1b = setw(d1b, (d1b & 0xFFFF) << 8); // asl.w #8,d1
                    Mem.ww(leftbright, d1b);           // move.w d1,leftbright
                    d3b = swap(d3b);                   // swap d3
                    d2 = (short) d2;                   // ext.l d2
                    d3b = d3b / d2;                    // divs.l d2,d3
                    d3b = d3b >> 8;                    // asr.l #8,d3
                    Mem.ww(brightspd, d3b);            // move.w d3,brightspd
                    int a3 = a6;                       // move.l a6,a3
                    int z = d6g;                       // move.l d6,d0
                    Mem.wl(draw_Distance_l, z);        // move.l d0,draw_Distance_l
                    int a1g = Mem.l(Draw_TexturePalettePtr_l) + 256 * 32; // a1
                    int a0g = Mem.l(Draw_FloorTexturesPtr_l) + (short) Mem.uw(whichtile); // a0
                    pastfloorbright(z, a0g, a1g, a3);  // jsr pastfloorbright
                    drawLine = false; // (déjà dessiné ; pas de bloc draw supplémentaire)
                }
            }

            // nodrawline / nodrawlineGOUR — avance ligne suivante
            Mem.ww(disttobot, Mem.uw(disttobot) - 1);  // sub.w #1,disttobot
            d3 = setw(0, Mem.uw(linedir));             // move.w linedir(pc),d3
            a6 = a6 + (short) d3;                      // adda.w d3,a6
            d3 = setw(d3, Mem.uw(tonextline));         // move.w tonextline,d3
            a4 = a4 + (short) d3;                      // add.w d3,a4
            d3 = setw(d3, ((short) d3) >> 1);          // asr.w #1,d3
            d0 = setw(d0, d0 + d3);                    // add.w d3,d0
            d7 = setw(d7, d7 - 1);                     // subq #1,d7
            if ((short) d7 > 0) continue;              // bgt dofloor*
            break;                                     // predontdrawfloor
        }
        return savedA0; // predontdrawfloor: move.l (a7)+,a0 ; rts
    }

    /**
     * sideLoopSimple (hires.s:3789) — edge-walk non gouraud. Projette chaque
     * arête du polygone sol, clippe au plan z=minz, et « dessine » la ligne
     * projetée dans LeftSideTable_vw/RightSideTable_vw (X par scanline), en
     * maintenant top/bottom et drawit. Entrée a0 = ptr sur (sides-1) ; renvoie
     * a0 après le dernier index lu (avant le addq #2,a0 de pastsides).
     */
    static int sideLoopSimple(int a0) {
        int d0 = 0, d1, d2 = 0, d3, d4, d5, d6, d7;
        int a1 = Rotated_vl, a2 = OnScreen_vl, a3;
        d7 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d7  (no of sides)

        while (true) {
            // sideloop:
            d6 = setw(0, Mem.uw(minz));                // move.w minz,d6
            d1 = setw(0, Mem.uw(a0));
            a0 += 2;                                   // move.w (a0)+,d1
            d3 = setw(0, Mem.uw(a0));                  // move.w (a0),d3
            d1 = setw(d1, d1 & 0xFFF);                 // and.w #$fff,d1
            d3 = setw(d3, d3 & 0xFFF);                 // and.w #$fff,d3
            d4 = setw(0, Mem.uw(a1 + (d1 & 0xFFFF) * 8 + 6)); // move.w 6(a1,d1*8),d4  (first z)

            boolean walk = true;
            a3 = RightSideTable_vw;                    // (lineclipped default)

            if ((short) d4 > (short) d6) {             // cmp.w d6,d4 ; bgt firstinfront
                // firstinfront (3834)
                d5 = setw(0, Mem.uw(a1 + (d3 & 0xFFFF) * 8 + 6)); // move.w 6(a1,d3*8),d5
                if ((short) d5 > (short) d6) {         // cmp.w d6,d5 ; bgt bothinfront
                    // bothinfront (3862)
                    d0 = setw(0, Mem.uw(a2 + (d1 & 0xFFFF) * 2)); // move.w (a2,d1*2),d0
                    d2 = setw(0, Mem.uw(a2 + (d3 & 0xFFFF) * 2)); // move.w (a2,d3*2),d2
                    d1 = Mem.l(ypos);                  // move.l ypos,d1
                    d3 = d1;                           // move.l d1,d3
                    d1 = divs(d1, d4);                 // divs d4,d1
                    d3 = divs(d3, d5);                 // divs d5,d3
                } else {
                    // line on right, partially behind (3839)
                    d5 = setw(d5, d5 - d4);            // sub.w d4,d5  (dz)
                    d2 = Mem.l(a1 + (d3 & 0xFFFF) * 8); // move.l (a1,d3*8),d2
                    d2 = d2 - Mem.l(a1 + (d1 & 0xFFFF) * 8); // sub.l (a1,d1*8),d2  (dx)
                    d6 = setw(d6, d6 - d4);            // sub.w d4,d6  (minz-z)
                    d2 = d2 >> 7;                      // asr.l #7,d2
                    d2 = muls(d2, d6);                 // muls d6,d2
                    d2 = divs(d2, d5);                 // divs d5,d2
                    d2 = (short) d2;                   // ext.l d2
                    d2 = d2 << 7;                      // asl.l #7,d2
                    d2 = d2 + Mem.l(a1 + (d1 & 0xFFFF) * 8); // add.l (a1,d1*8),d2
                    d5 = setw(d5, Mem.uw(minz));       // move.w minz,d5
                    d0 = setw(0, Mem.uw(a2 + (d1 & 0xFFFF) * 2)); // move.w (a2,d1*2),d0
                    d2 = divs(d2, d5);                 // divs d5,d2
                    d2 = setw(d2, d2 + Mem.uw(Vid_CentreX_w)); // add.w Vid_CentreX_w,d2
                    d1 = Mem.l(ypos);                  // move.l ypos,d1
                    d1 = divs(d1, d4);                 // divs d4,d1
                    d3 = setw(d3, Mem.uw(bottomline)); // move.w bottomline,d3
                }
            } else {
                d5 = setw(0, Mem.uw(a1 + (d3 & 0xFFFF) * 8 + 6)); // move.w 6(a1,d3*8),d5
                if ((short) d5 <= (short) d6) {        // cmp.w d6,d5 ; ble bothbehind
                    walk = false;                      // bothbehind
                } else {
                    // line on left, partially behind (3807)
                    d4 = setw(d4, d4 - d5);            // sub.w d5,d4  (dz)
                    d0 = Mem.l(a1 + (d1 & 0xFFFF) * 8); // move.l (a1,d1*8),d0
                    d0 = d0 - Mem.l(a1 + (d3 & 0xFFFF) * 8); // sub.l (a1,d3*8),d0  (dx)
                    d0 = d0 >> 7;                      // asr.l #7,d0
                    d6 = setw(d6, d6 - d5);            // sub.w d5,d6  (minz-secz)
                    d0 = muls(d0, d6);                 // muls d6,d0
                    d0 = divs(d0, d4);                 // divs d4,d0
                    d0 = (short) d0;                   // ext.l d0
                    d0 = d0 << 7;                      // asl.l #7,d0
                    d0 = d0 + Mem.l(a1 + (d3 & 0xFFFF) * 8); // add.l (a1,d3*8),d0
                    d4 = setw(d4, Mem.uw(minz));       // move.w minz,d4
                    d2 = setw(0, Mem.uw(a2 + (d3 & 0xFFFF) * 2)); // move.w (a2,d3*2),d2
                    d0 = divs(d0, d4);                 // divs d4,d0
                    d0 = setw(d0, d0 + Mem.uw(Vid_CentreX_w)); // add.w Vid_CentreX_w,d0
                    d3 = Mem.l(ypos);                  // move.l ypos,d3
                    d3 = divs(d3, d5);                 // divs d5,d3
                    d1 = setw(d1, Mem.uw(bottomline)); // move.w bottomline,d1
                }
            }

            // lineclipped (3877)
            if (walk) {
                a3 = RightSideTable_vw;                // move.l #RightSideTable_vw,a3
                if ((short) d3 == (short) d1) {        // cmp.w d1,d3 ; beq lineflat
                    walk = false;
                } else {
                    Mem.wb(drawit, 0xFF);              // st drawit
                    boolean onright = ((short) d3 > (short) d1); // bgt lineonright
                    if (!onright) {
                        a3 = LeftSideTable_vw;         // move.l #LeftSideTable_vw,a3
                        int t = d1; d1 = d3; d3 = t;   // exg d1,d3
                        t = d0; d0 = d2; d2 = t;       // exg d0,d2
                    }
                    a3 = a3 + (d1 & 0xFFFF) * 2;       // lea (a3,d1*2),a3
                    if ((short) d1 < (short) Mem.uw(top)) {    // cmp.w top(pc),d1 ; bge.s .no_new_top
                        Mem.ww(top, d1);               // move.w d1,top
                    }
                    if ((short) d3 > (short) Mem.uw(bottom)) { // cmp.w bottom(pc),d3 ; ble.s .no_new_bottom
                        Mem.ww(bottom, d3);            // move.w d3,bottom
                    }
                    d3 = setw(d3, d3 - d1);            // sub.w d1,d3  (dy)
                    d2 = setw(d2, d2 - d0);            // sub.w d0,d2  (dx)

                    if (!onright) {
                        // --- LEFT path ---
                        if ((short) d2 < 0) {          // blt .linegoingleft
                            // .linegoingleft (3934)
                            d2 = setw(d2, -(short) d2); // neg.w d2
                            d2 = (short) d2;           // ext.l d2
                            d2 = divs(d2, d3);         // divs d3,d2
                            d6 = setw(0, d2);          // move.w d2,d6
                            d2 = swap(d2);             // swap d2
                            d4 = setw(0, d3);          // move.w d3,d4
                            d5 = setw(0, d3);          // move.w d3,d5
                            d5 = setw(d5, d5 - 1);     // subq #1,d5
                            d1 = setw(0, d6);          // move.w d6,d1
                            d1 = setw(d1, d1 + 1);     // addq #1,d1
                            while (true) {             // .pixlopleft
                                d4 = setw(d4, d4 - d2); // sub.w d2,d4
                                if ((short) d4 >= 0) { // bge.s .nobigstepl
                                    d0 = setw(d0, d0 - d6); // sub.w d6,d0
                                    Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                                } else {
                                    d0 = setw(d0, d0 - d1); // sub.w d1,d0
                                    d4 = setw(d4, d4 + d3); // add.w d3,d4
                                    Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                                }
                                d5 = setw(d5, d5 - 1); // dbra d5
                                if ((short) d5 == -1) break;
                            }
                        } else {
                            // down-going (3906)
                            d2 = (short) d2;           // ext.l d2
                            d2 = divs(d2, d3);         // divs d3,d2
                            d6 = setw(0, d2);          // move.w d2,d6
                            d2 = swap(d2);             // swap d2
                            d4 = setw(0, d3);          // move.w d3,d4
                            d5 = setw(0, d3);          // move.w d3,d5
                            d5 = setw(d5, d5 - 1);     // subq #1,d5
                            d1 = setw(0, d6);          // move.w d6,d1
                            d1 = setw(d1, d1 + 1);     // addq #1,d1
                            while (true) {             // .pixlopright
                                Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                                d4 = setw(d4, d4 - d2); // sub.w d2,d4
                                if ((short) d4 >= 0) { // bge.s .nobigstep
                                    d0 = setw(d0, d0 + d6); // add.w d6,d0
                                } else {
                                    d0 = setw(d0, d0 + d1); // add.w d1,d0
                                    d4 = setw(d4, d4 + d3); // add.w d3,d4
                                }
                                d5 = setw(d5, d5 - 1); // dbra d5
                                if ((short) d5 == -1) break;
                            }
                        }
                    } else {
                        // --- RIGHT path (lineonright 3966) ---
                        if ((short) d2 < 0) {          // blt .linegoingleft
                            // .linegoingleft (4009)
                            d2 = setw(d2, -(short) d2); // neg.w d2
                            d2 = (short) d2;           // ext.l d2
                            d2 = divs(d2, d3);         // divs d3,d2
                            d6 = setw(0, d2);          // move.w d2,d6
                            d2 = swap(d2);             // swap d2
                            d4 = setw(0, d3);          // move.w d3,d4
                            d5 = setw(0, d3);          // move.w d3,d5
                            d5 = setw(d5, d5 - 1);     // subq #1,d5
                            d1 = setw(0, d6);          // move.w d6,d1
                            d1 = setw(d1, d1 + 1);     // addq #1,d1
                            while (true) {             // .pixlopleft
                                Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                                d4 = setw(d4, d4 - d2); // sub.w d2,d4
                                if ((short) d4 >= 0) { // bge.s .nobigstepl
                                    d0 = setw(d0, d0 - d6); // sub.w d6,d0
                                } else {
                                    d0 = setw(d0, d0 - d1); // sub.w d1,d0
                                    d4 = setw(d4, d4 + d3); // add.w d3,d4
                                }
                                d5 = setw(d5, d5 - 1); // dbra d5
                                if ((short) d5 == -1) break;
                            }
                        } else {
                            // down-going (3983)
                            d2 = (short) d2;           // ext.l d2
                            d2 = divs(d2, d3);         // divs d3,d2
                            d6 = setw(0, d2);          // move.w d2,d6
                            d2 = swap(d2);             // swap d2
                            d4 = setw(0, d3);          // move.w d3,d4
                            d5 = setw(0, d3);          // move.w d3,d5
                            d5 = setw(d5, d5 - 1);     // subq #1,d5
                            d1 = setw(0, d6);          // move.w d6,d1
                            d1 = setw(d1, d1 + 1);     // addq #1,d1
                            while (true) {             // .pixlopright
                                d4 = setw(d4, d4 - d2); // sub.w d2,d4
                                if ((short) d4 >= 0) { // bge.s .nobigstep
                                    d0 = setw(d0, d0 + d6); // add.w d6,d0
                                    Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                                } else {
                                    d0 = setw(d0, d0 + d1); // add.w d1,d0
                                    d4 = setw(d4, d4 + d3); // add.w d3,d4
                                    Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                                }
                                d5 = setw(d5, d5 - 1); // dbra d5
                                if ((short) d5 == -1) break;
                            }
                        }
                    }
                }
            }

            // lineflat / bothbehind (4037-4040)
            d7 = setw(d7, d7 - 1);                     // dbra d7,sideloop
            if ((short) d7 == -1) break;
        }
        // bra pastsides
        return a0;
    }

    /**
     * sideLoopGouraud (hires.s:4048 goursides/sideloopGOUR) — variante gouraud de
     * l'edge-walk : en plus du X par scanline, walk la brightness des coins
     * (fbr/sbr, lus depuis FloorPtBrightsPtr) dans LeftBrightTable_vw /
     * RightBrightTable_vw. Entrée a0 = ptr sur (sides-1) ; renvoie a0 après le
     * dernier index lu.
     */
    static int sideLoopGouraud(int a0) {
        int d0 = 0, d1, d2 = 0, d3, d4, d5, d6, d7;
        int a1 = Rotated_vl, a2 = OnScreen_vl, a3;
        d7 = setw(0, Mem.uw(a0));
        a0 += 2;                                       // move.w (a0)+,d7

        while (true) {
            // sideloopGOUR:
            d6 = setw(0, Mem.uw(minz));                // move.w minz,d6
            d1 = setw(0, Mem.uw(a0));
            a0 += 2;                                   // move.w (a0)+,d1
            d3 = setw(0, Mem.uw(a0));                  // move.w (a0),d3
            d4 = setw(0, d1);                          // move.w d1,d4
            d5 = setw(0, d3);                          // move.w d3,d5
            d1 = setw(d1, d1 & 0x0FFF);                // and.w #$0fff,d1
            d3 = setw(d3, d3 & 0x0FFF);                // and.w #$0fff,d3
            d4 = setw(d4, ((d4 & 0xFFFF) << 4 | (d4 & 0xFFFF) >>> 12) & 0xFFFF); // rol.w #4,d4
            d5 = setw(d5, ((d5 & 0xFFFF) << 4 | (d5 & 0xFFFF) >>> 12) & 0xFFFF); // rol.w #4,d5
            d4 = setw(d4, d4 & 0xF);                   // and.w #$f,d4
            d5 = setw(d5, d5 & 0xF);                   // and.w #$f,d5
            int a4f = Mem.l(FloorPtBrightsPtr_l);      // move.l FloorPtBrightsPtr_l,a4
            d4 = setw(d4, Mem.uw(a4f + (d4 & 0xFFFF) * 8)); // move.w (a4,d4.w*8),d4
            if ((short) d4 < 0) d4 = setw(d4, -(short) d4); // bge.s .okpos1 ; neg.w d4
            d4 = setw(d4, d4 - 300);                   // sub.w #300,d4
            Mem.ww(fbr, d4);                           // move.w d4,fbr
            d4 = setw(d4, Mem.uw(a4f + (d5 & 0xFFFF) * 8)); // move.w (a4,d5.w*8),d4
            if ((short) d4 < 0) d4 = setw(d4, -(short) d4); // bge.s .okpos2 ; neg.w d4
            d4 = setw(d4, d4 - 300);                   // sub.w #300,d4
            Mem.ww(sbr, d4);                           // move.w d4,sbr

            d4 = setw(d4, Mem.uw(a1 + (d1 & 0xFFFF) * 8 + 6)); // move.w 6(a1,d1*8),d4  (first z)
            boolean walk = true;

            if ((short) d4 > (short) d6) {             // cmp.w d6,d4 ; bgt firstinfrontGOUR
                // firstinfrontGOUR (4124)
                d5 = setw(0, Mem.uw(a1 + (d3 & 0xFFFF) * 8 + 6)); // move.w 6(a1,d3*8),d5
                if ((short) d5 > (short) d6) {         // cmp.w d6,d5 ; bgt bothinfrontGOUR
                    // bothinfrontGOUR (4157)
                    d0 = setw(0, Mem.uw(a2 + (d1 & 0xFFFF) * 2)); // move.w (a2,d1*2),d0
                    d2 = setw(0, Mem.uw(a2 + (d3 & 0xFFFF) * 2)); // move.w (a2,d3*2),d2
                    d1 = Mem.l(ypos);                  // move.l ypos,d1
                    d3 = d1;                           // move.l d1,d3
                    d1 = divs(d1, d4);                 // divs d4,d1
                    d3 = divs(d3, d5);                 // divs d5,d3
                } else {
                    // on right, partially behind (4128)
                    d5 = setw(d5, d5 - d4);            // sub.w d4,d5  (dz)
                    d2 = setw(0, Mem.uw(sbr));         // move.w sbr,d2
                    d2 = setw(d2, d2 - Mem.uw(fbr));   // sub.w fbr,d2
                    d6 = setw(d6, d6 - d4);            // sub.w d4,d6  (minz-firstz)
                    d2 = muls(d2, d6);                 // muls d6,d2
                    d2 = divs(d2, d5);                 // divs d5,d2
                    d2 = setw(d2, d2 + Mem.uw(fbr));   // add.w fbr,d2
                    Mem.ww(sbr, d2);                   // move.w d2,sbr
                    d2 = Mem.l(a1 + (d3 & 0xFFFF) * 8); // move.l (a1,d3*8),d2
                    d2 = d2 - Mem.l(a1 + (d1 & 0xFFFF) * 8); // sub.l (a1,d1*8),d2  (dx)
                    d2 = d2 >> 7;                      // asr.l #7,d2
                    d2 = muls(d2, d6);                 // muls d6,d2
                    d2 = divs(d2, d5);                 // divs d5,d2
                    d2 = (short) d2;                   // ext.l d2
                    d2 = d2 << 7;                      // asl.l #7,d2
                    d2 = d2 + Mem.l(a1 + (d1 & 0xFFFF) * 8); // add.l (a1,d1*8),d2
                    d5 = setw(d5, Mem.uw(minz));       // move.w minz,d5
                    d0 = setw(0, Mem.uw(a2 + (d1 & 0xFFFF) * 2)); // move.w (a2,d1*2),d0
                    d2 = divs(d2, d5);                 // divs d5,d2
                    d2 = setw(d2, d2 + Mem.uw(Vid_CentreX_w)); // add.w Vid_CentreX_w,d2
                    d1 = Mem.l(ypos);                  // move.l ypos,d1
                    d1 = divs(d1, d4);                 // divs d4,d1
                    d3 = setw(d3, Mem.uw(bottomline)); // move.w bottomline,d3
                }
            } else {
                d5 = setw(0, Mem.uw(a1 + (d3 & 0xFFFF) * 8 + 6)); // move.w 6(a1,d3*8),d5
                if ((short) d5 <= (short) d6) {        // cmp.w d6,d5 ; ble bothbehindGOUR
                    walk = false;
                } else {
                    // on left, partially behind (4092)
                    d4 = setw(d4, d4 - d5);            // sub.w d5,d4  (dz)
                    d0 = setw(0, Mem.uw(fbr));         // move.w fbr,d0
                    d0 = setw(d0, d0 - Mem.uw(sbr));   // sub.w sbr,d0
                    d6 = setw(d6, d6 - d5);            // sub.w d5,d6  (minz-secz)
                    d0 = muls(d0, d6);                 // muls d6,d0
                    d0 = divs(d0, d4);                 // divs d4,d0
                    d0 = setw(d0, d0 + Mem.uw(sbr));   // add.w sbr,d0
                    Mem.ww(fbr, d0);                   // move.w d0,fbr
                    d0 = Mem.l(a1 + (d1 & 0xFFFF) * 8); // move.l (a1,d1*8),d0
                    d0 = d0 - Mem.l(a1 + (d3 & 0xFFFF) * 8); // sub.l (a1,d3*8),d0
                    d0 = d0 >> 7;                      // asr.l #7,d0
                    d0 = muls(d0, d6);                 // muls d6,d0
                    d0 = divs(d0, d4);                 // divs d4,d0
                    d0 = (short) d0;                   // ext.l d0
                    d0 = d0 << 7;                      // asl.l #7,d0
                    d0 = d0 + Mem.l(a1 + (d3 & 0xFFFF) * 8); // add.l (a1,d3*8),d0
                    d4 = setw(d4, Mem.uw(minz));       // move.w minz,d4
                    d2 = setw(0, Mem.uw(a2 + (d3 & 0xFFFF) * 2)); // move.w (a2,d3*2),d2
                    d0 = divs(d0, d4);                 // divs d4,d0
                    d0 = setw(d0, d0 + Mem.uw(Vid_CentreX_w)); // add.w Vid_CentreX_w,d0
                    d3 = Mem.l(ypos);                  // move.l ypos,d3
                    d3 = divs(d3, d5);                 // divs d5,d3
                    d1 = setw(d1, Mem.uw(bottomline)); // move.w bottomline,d1
                }
            }

            // lineclippedGOUR (4171)
            if (walk) {
                if ((short) d3 == (short) d1) {        // cmp.w d1,d3 ; bne linenotflatGOUR ; bra lineflatGOUR
                    walk = false;
                } else {
                    Mem.wb(drawit, 0xFF);              // st drawit
                    boolean onright = ((short) d3 > (short) d1); // bgt lineonrightGOUR
                    if (!onright) {
                        int t = d1; d1 = d3; d3 = t;   // exg d1,d3
                        t = d0; d0 = d2; d2 = t;       // exg d0,d2
                        a3 = LeftSideTable_vw + (d1 & 0xFFFF) * 2;   // move.l #LeftSideTable_vw,a3 ; lea (a3,d1*2),a3
                    } else {
                        a3 = RightSideTable_vw + (d1 & 0xFFFF) * 2;  // lea (a3,d1*2),a3  (a3=RightSideTable_vw)
                    }
                    int a4 = onright
                            ? a3 + (RightBrightTable_vw - RightSideTable_vw)  // lea RightBrightTable-RightSideTable(a3),a4
                            : a3 + (LeftBrightTable_vw - LeftSideTable_vw);    // lea LeftBrightTable-LeftSideTable(a3),a4
                    if ((short) d1 < (short) Mem.uw(top)) Mem.ww(top, d1);     // cmp.w top,d1 ; bge .no_new_top ; move.w d1,top
                    if ((short) d3 > (short) Mem.uw(bottom)) Mem.ww(bottom, d3); // cmp.w bottom,d3 ; ble .no_new_bottom ; move.w d3,bottom
                    d3 = setw(d3, d3 - d1);            // sub.w d1,d3  (dy)
                    d2 = setw(d2, d2 - d0);            // sub.w d0,d2  (dx)

                    int brStart = onright ? Mem.uw(fbr) : Mem.uw(sbr); // RIGHT: fbr ; LEFT: sbr
                    int brOther = onright ? Mem.uw(sbr) : Mem.uw(fbr); // delta = other - start
                    boolean leftGoing = ((short) d2 < 0);  // blt .linegoingleft

                    if (leftGoing) d2 = setw(d2, -(short) d2); // neg.w d2
                    d2 = (short) d2;                   // ext.l d2
                    d2 = divs(d2, d3);                 // divs d3,d2
                    d6 = setw(0, d2);                  // move.w d2,d6  (quot)
                    d2 = swap(d2);                     // swap d2
                    int a5 = (short) d2;               // move.w d2,a5  (remainder)
                    d4 = setw(0, d3);                  // move.w d3,d4
                    d5 = setw(0, d3);                  // move.w d3,d5
                    d5 = setw(d5, d5 - 1);             // subq #1,d5
                    d1 = setw(0, d6);                  // move.w d6,d1
                    d1 = setw(d1, d1 + 1);             // addq #1,d1
                    int a6 = (short) d1;               // move.w d1,a6  (quot+1)
                    d1 = setw(0, brStart);             // moveq #0,d1 ; move.w fbr/sbr,d1
                    d2 = setw(d2, brOther);            // move.w sbr/fbr,d2
                    d2 = setw(d2, d2 - d1);            // sub.w d1,d2
                    d2 = (short) d2;                   // ext.l d2
                    d2 = setw(d2, (d2 & 0xFFFF) << 8); // asl.w #8,d2
                    d2 = setw(d2, (d2 & 0xFFFF) << 2); // asl.w #2,d2
                    d2 = divs(d2, d3);                 // divs d3,d2
                    d2 = (short) d2;                   // ext.l d2
                    d2 = d2 << 6;                      // asl.l #6,d2
                    d1 = swap(d1);                     // swap d1

                    if (!onright && !leftGoing) {
                        // LEFT down (.pixlopright 4229)
                        while (true) {
                            Mem.ww(a3, d0); a3 += 2;   // move.w d0,(a3)+
                            d1 = swap(d1);             // swap d1
                            Mem.ww(a4, d1); a4 += 2;   // move.w d1,(a4)+
                            d1 = swap(d1);             // swap d1
                            d1 = d1 + d2;              // add.l d2,d1
                            d4 = setw(d4, d4 - a5);    // sub.w a5,d4
                            if ((short) d4 >= 0) {     // bge.s .nobigstep
                                d0 = setw(d0, d0 + d6); // add.w d6,d0
                            } else {
                                d0 = setw(d0, d0 + a6); // add.w a6,d0
                                d4 = setw(d4, d4 + d3); // add.w d3,d4
                            }
                            d5 = setw(d5, d5 - 1);
                            if ((short) d5 == -1) break; // dbra d5
                        }
                    } else if (!onright && leftGoing) {
                        // LEFT going-left (.pixlopleft 4278)
                        while (true) {
                            d1 = swap(d1);             // swap d1
                            Mem.ww(a4, d1); a4 += 2;   // move.w d1,(a4)+
                            d1 = swap(d1);             // swap d1
                            d1 = d1 + d2;              // add.l d2,d1
                            d4 = setw(d4, d4 - a5);    // sub.w a5,d4
                            if ((short) d4 >= 0) {     // bge.s .nobigstepl
                                d0 = setw(d0, d0 - d6); // sub.w d6,d0
                                Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                            } else {
                                d0 = setw(d0, d0 - a6); // sub.w a6,d0
                                d4 = setw(d4, d4 + d3); // add.w d3,d4
                                Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                            }
                            d5 = setw(d5, d5 - 1);
                            if ((short) d5 == -1) break; // dbra d5
                        }
                    } else if (onright && !leftGoing) {
                        // RIGHT down (.pixlopright 4344)
                        while (true) {
                            d1 = swap(d1);             // swap d1
                            Mem.ww(a4, d1); a4 += 2;   // move.w d1,(a4)+
                            d1 = swap(d1);             // swap d1
                            d1 = d1 + d2;              // add.l d2,d1
                            d4 = setw(d4, d4 - a5);    // sub.w a5,d4
                            if ((short) d4 >= 0) {     // bge.s .nobigstep
                                d0 = setw(d0, d0 + d6); // add.w d6,d0
                                Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                            } else {
                                d0 = setw(d0, d0 + a6); // add.w a6,d0
                                d4 = setw(d4, d4 + d3); // add.w d3,d4
                                Mem.ww(a3, d0); a3 += 2; // move.w d0,(a3)+
                            }
                            d5 = setw(d5, d5 - 1);
                            if ((short) d5 == -1) break; // dbra d5
                        }
                    } else {
                        // RIGHT going-left (.pixlopleft 4394)
                        while (true) {
                            d1 = swap(d1);             // swap d1
                            Mem.ww(a4, d1); a4 += 2;   // move.w d1,(a4)+
                            d1 = swap(d1);             // swap d1
                            d1 = d1 + d2;              // add.l d2,d1
                            Mem.ww(a3, d0); a3 += 2;   // move.w d0,(a3)+
                            d4 = setw(d4, d4 - a5);    // sub.w a5,d4
                            if ((short) d4 >= 0) {     // bge.s .nobigstepl
                                d0 = setw(d0, d0 - d6); // sub.w d6,d0
                            } else {
                                d0 = setw(d0, d0 - a6); // sub.w a6,d0
                                d4 = setw(d4, d4 + d3); // add.w d3,d4
                            }
                            d5 = setw(d5, d5 - 1);
                            if ((short) d5 == -1) break; // dbra d5
                        }
                    }
                }
            }

            // lineflatGOUR / bothbehindGOUR (4413-4416)
            d7 = setw(d7, d7 - 1);                     // dbra d7,sideloopGOUR
            if ((short) d7 == -1) break;
        }
        // fall to pastsides
        return a0;
    }

    /**
     * Draw_Flats (hires.s:3598) — corps principal. Entrées : a0 = flux zone-graph
     * (pointe sur floorY du record), d0 = tag (1=floor, 2=roof). Renvoie a0
     * avancé après le record.
     */
    public static int Draw_Flats(int a0, int d0) {
        int d1 = 0, d3 = 0, d4 = 0, d5 = 0, d6 = 0, d7 = 0;
        int a1, a2;

        final int ENTRY = 0, DONTDRAWRETURN = 1, CHECKFORWATER = 2, ABOVEPLAYER = 3,
                BELOW = 4, NOTBELOW = 5, SOMEFLOORTODRAW = 6;
        int lbl = ENTRY;
        while (true) {
            switch (lbl) {
                case ENTRY: {
                    Mem.ww(above, 0);                          // move.w #0,above
                    d6 = setw(d6, Mem.uw(a0));
                    a0 += 2;                                   // move.w (a0)+,d6  (floorY)
                    if (Mem.b(draw_UseWater_b) != 0) {         // tst.b draw_UseWater_b ; beq.s .oknon
                        if (Mem.b(DOANYWATER) == 0) {          // tst.b DOANYWATER ; beq dontdrawreturn
                            lbl = DONTDRAWRETURN;
                            continue;
                        }
                    }
                    // .oknon
                    d7 = setw(d7, d6);                         // move.w d6,d7
                    d7 = (short) d7;                           // ext.l d7
                    d7 = d7 << 6;                              // asl.l #6,d7
                    if (d7 < Mem.l(Draw_TopOfRoom_l)) {        // cmp.l Draw_TopOfRoom_l,d7 ; blt checkforwater
                        lbl = CHECKFORWATER;
                        continue;
                    }
                    if (d7 > Mem.l(Draw_BottomOfRoom_l)) {     // cmp.l Draw_BottomOfRoom_l,d7 ; bgt dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    d7 = setw(d7, Mem.uw(Draw_LeftClip_w));    // move.w Draw_LeftClip_w,d7
                    if ((short) d7 >= Mem.w(Draw_RightClip_w)) { // cmp.w Draw_RightClip_w,d7 ; bge dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    d6 = setw(d6, d6 - Mem.uw(flooryoff));     // sub.w flooryoff,d6
                    if ((short) d6 > 0) {                      // bgt.s below
                        lbl = BELOW;
                        continue;
                    }
                    if ((short) d6 < 0) {                      // blt.s aboveplayer
                        lbl = ABOVEPLAYER;
                        continue;
                    }
                    // d6 == 0
                    if (Mem.b(draw_UseWater_b) != 0) {         // tst.b draw_UseWater_b ; beq.s .notwater
                        a1 = Mem.l(ZonePtr_l);                 // move.l ZonePtr_l,a1
                        d7 = setw(d7, Mem.uw(a1));             // move.w (a1),d7
                        if ((short) d7 == Mem.w(Draw_CurrentZone_w)) { // cmp.w Draw_CurrentZone_w,d7 ; bne.s .notwater
                            Mem.wb(fillscrnwater, 0xFF);       // st fillscrnwater
                        }
                    }
                    // .notwater → dontdrawreturn
                    lbl = DONTDRAWRETURN;
                    continue;
                }

                case DONTDRAWRETURN: {
                    d6 = setw(d6, Mem.uw(a0));
                    a0 += 2;                                   // move.w (a0)+,d6  (sides-1)
                    a0 = a0 + 10 + ((short) d6) * 2;           // lea 10(a0,d6.w*2),a0
                    return a0;                                 // rts (nofloor)
                }

                case CHECKFORWATER: {                          // hires.s:3575
                    if (Mem.b(draw_UseWater_b) != 0) {         // tst.b draw_UseWater_b ; beq.s .notwater
                        a1 = Mem.l(ZonePtr_l);                 // move.l ZonePtr_l,a1
                        d7 = setw(d7, Mem.uw(a1));             // move.w (a1),d7
                        if ((short) d7 == Mem.w(Draw_CurrentZone_w)) { // cmp.w Draw_CurrentZone_w,d7 ; bne.s .notwater
                            Mem.wb(fillscrnwater, 0x0F);       // move.b #$f,fillscrnwater
                        }
                    }
                    // .notwater
                    d6 = setw(d6, Mem.uw(a0));
                    a0 += 2;                                   // move.w (a0)+,d6  (sides-1)
                    a0 = a0 + 10 + ((short) d6) * 2;           // lea 10(a0,d6.w*2),a0
                    return a0;                                 // rts
                }

                case ABOVEPLAYER: {                            // hires.s:3651
                    if (Mem.b(draw_UseWater_b) != 0) {         // tst.b draw_UseWater_b ; beq.s .notwater
                        a1 = Mem.l(ZonePtr_l);                 // move.l ZonePtr_l,a1
                        d7 = setw(d7, Mem.uw(a1));             // move.w (a1),d7
                        if ((short) d7 == Mem.w(Draw_CurrentZone_w)) { // cmp.w Draw_CurrentZone_w,d7 ; bne.s .notwater
                            Mem.wb(fillscrnwater, 0x0F);       // move.b #$f,fillscrnwater
                        }
                    }
                    // .notwater
                    if ((d0 & 2) == 0) {                       // btst #1,d0 ; beq.s dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    // its a ceiling
                    d7 = setw(d7, Mem.uw(Vid_CentreY_w));      // move.w Vid_CentreY_w,d7
                    d7 = setw(d7, d7 - Mem.uw(draw_TopClip_w)); // sub.w draw_TopClip_w,d7
                    if ((short) d7 <= 0) {                     // ble.s dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    d0 = setw(d0, 1);                          // move.w #1,d0
                    Mem.ww(above, d0);                         // move.w d0,above
                    d6 = setw(d6, -(short) d6);                // neg.w d6
                    lbl = NOTBELOW;                            // bra.s notbelow
                    continue;
                }

                case BELOW: {                                  // hires.s:3677
                    d7 = setw(d7, Mem.uw(draw_BottomClip_w));  // move.w draw_BottomClip_w,d7
                    d7 = setw(d7, d7 - Mem.uw(Vid_CentreY_w)); // sub.w Vid_CentreY_w,d7
                    if ((short) d7 <= 0) {                     // ble.s dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    lbl = NOTBELOW;                            // fall to notbelow
                    continue;
                }

                case NOTBELOW: {                               // hires.s:3683
                    if ((d0 & 1) == 0) {                       // btst #0,d0 ; beq.s dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    Mem.ww(View2FloorDist, d6);                // move.w d6,View2FloorDist
                    d5 = setw(d5, d6);                         // move d6,d5
                    d5 = (short) d5;                           // ext.l d5
                    d5 = d5 << 6;                              // asl.l #6,d5
                    Mem.wl(ypos, d5);                          // move.l d5,ypos
                    d6 = mulu(d6, Mem.uw(OneOverN_vw + (d7 & 0xFFFF) * 2)); // mulu.w OneOverN_vw(pc,d7.w*2),d6
                    d6 = d6 >>> 8;                             // lsr.l #8,d6
                    if (d6 == 0) {                             // beq dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    if (d6 > 32767) {                          // cmp.l #32767,d6 ; bgt dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    Mem.ww(minz, d6);                          // move.w d6,minz
                    Mem.ww(bottomline, d7);                    // move.w d7,bottomline

                    int savedA0 = a0;                          // move.l a0,-(a7)
                    d7 = setw(d7, Mem.uw(a0));
                    a0 += 2;                                   // move.w (a0)+,d7  (sides-1)
                    a1 = Rotated_vl;                           // move.l #Rotated_vl,a1
                    a2 = OnScreen_vl;                          // move.l #OnScreen_vl,a2
                    d4 = 0;                                    // moveq #0,d4
                    d5 = 0;                                    // moveq #0,d5
                    d6 = 0;                                    // moveq #0,d6
                    Mem.wb(anyclipping, 0);                    // clr.b anyclipping

                    // cornerprocessloop (3738)
                    while (true) {
                        d0 = setw(d0, Mem.uw(a0));
                        a0 += 2;                               // move.w (a0)+,d0
                        d0 = setw(d0, d0 & 0xFFF);             // and.w #$fff,d0
                        d1 = setw(d1, Mem.uw(a1 + (d0 & 0xFFFF) * 8 + 6)); // move.w 6(a1,d0.w*8),d1
                        if ((short) d1 <= 0) {                 // ble .canttell
                            d5 = setb(d5, 0xFF);               // .canttell: st d5
                            Mem.wb(anyclipping, 0xFF);         // st anyclipping
                        } else {
                            d3 = setw(d3, Mem.uw(a2 + (d0 & 0xFFFF) * 2)); // move.w (a2,d0.w*2),d3
                            if ((short) d3 <= Mem.w(Draw_LeftClip_w)) {  // cmp.w Draw_LeftClip_w,d3 ; bgt.s .nol
                                d4 = setb(d4, 0xFF);           // st d4
                                Mem.wb(anyclipping, 0xFF);     // st anyclipping
                            } else if ((short) d3 >= Mem.w(Draw_RightClip_w)) { // cmp.w Draw_RightClip_w,d3 ; blt.s .nor
                                d6 = setb(d6, 0xFF);           // st d6
                                Mem.wb(anyclipping, 0xFF);     // st anyclipping
                            } else {
                                d5 = setb(d5, 0xFF);           // .nor: st d5
                            }
                        }
                        d7 = setw(d7, d7 - 1);                 // dbra d7,cornerprocessloop
                        if ((short) d7 == -1) break;
                    }

                    a0 = savedA0;                              // move.l (a7)+,a0
                    if ((d5 & 0xFF) != 0) {                    // tst.b d5 ; bne.s somefloortodraw
                        lbl = SOMEFLOORTODRAW;
                        continue;
                    }
                    d6 = setb(d6, (d6 ^ d4) & 0xFF);           // eor.b d4,d6
                    if ((d6 & 0xFF) != 0) {                    // bne dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    lbl = SOMEFLOORTODRAW;                     // fall
                    continue;
                }

                case SOMEFLOORTODRAW: {                        // hires.s:3778
                    if (DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_FLATS)) { // DEV_CHECK_SET SKIP_FLATS,dontdrawreturn
                        lbl = DONTDRAWRETURN;
                        continue;
                    }
                    // DEV_INC.w VisibleFlats
                    Mem.ww(DevInst.dev_VisibleFlats_w, Mem.uw(DevInst.dev_VisibleFlats_w) + 1);

                    int a0Edge;
                    if (Mem.b(draw_UseGouraudFlats_b) != 0) {  // tst.b draw_UseGouraudFlats_b ; bne goursides
                        Mem.ww(top, 300);                      // move.w #300,top
                        Mem.ww(bottom, -1);                    // move.w #-1,bottom
                        Mem.ww(drawit, 0);                     // move.w #0,drawit
                        a0Edge = sideLoopGouraud(a0);          // goursides / sideloopGOUR
                    } else {
                        Mem.ww(top, 300);                      // move.w #300,top
                        Mem.ww(bottom, -1);                    // move.w #-1,bottom
                        Mem.ww(drawit, 0);                     // move.w #0,drawit
                        a0Edge = sideLoopSimple(a0);           // sideloop
                    }
                    // pastsides → ... → predontdrawfloor / dontdrawfloor
                    return pastFlats(a0Edge);
                }

                default:
                    throw new IllegalStateException("Draw_Flats label " + lbl);
            }
        }
    }

    /**
     * MakeSomeNoise (hires.s:7517) — moteur audio logiciel (Paula).
     *
     * Entrées : Aud_SampleNum_w (sample), Aud_NoiseX/Z_w (position relative joueur),
     * Aud_NoiseVol_w (volume), IDNUM (identifiant), notifplaying (drapeau).
     * Décide si le nouveau son est plus « important » que ceux en cours (importance
     * = volume atténué par la distance), puis l'assigne au canal le moins important.
     * En stéréo : 4 canaux gauche + 4 droite ; sinon 8 canaux mono. Écrit les
     * états de canaux (pos, vol, Samp-end, NoiseMade, PLAYEDTAB) que la VBlank
     * envoie au DMA Paula.
     */
    public static void MakeSomeNoise() {
        int d0 = 0, d1 = 0, d2 = 0, d3 = 0, d4 = 0, d5 = 0, d6 = 0;
        int a1, a2, a3;

        if (Mem.b(notifplaying) != 0) {                // tst.b notifplaying ; beq.s dontworry
            d0 = setw(0, Mem.uw(IDNUM));               // move.w IDNUM,d0
            if ((d0 & 0xFFFF) != 0xFFFF) {             // cmp.w #$ffff,d0 ; beq.s dontworry
                d1 = setw(0, 7);                       // move.w #7,d1
                a3 = CHANNELDATA;                      // lea CHANNELDATA,a3
                while (true) {                         // findsameasme
                    if (Mem.b(a3) == 0) {              // tst.b (a3) ; bne.s notavail
                        if ((short) Mem.uw(a3 + 32) == (short) d0) return; // cmp.w 32(a3),d0 ; beq SameAsMe (rts)
                    }
                    // notavail
                    a3 += 4;                           // add.w #4,a3
                    d1 = setw(d1, d1 - 1);             // dbra d1,findsameasme
                    if ((short) d1 == -1) break;       // bra dontworry
                }
            }
        }

        // dontworry — calcule le volume gauche/droite depuis la position
        d1 = setw(0, Mem.uw(Aud_NoiseX_w));            // move.w Aud_NoiseX_w,d1
        d1 = muls(d1, d1);                             // muls d1,d1
        d2 = setw(0, Mem.uw(Aud_NoiseZ_w));            // move.w Aud_NoiseZ_w,d2
        d2 = muls(d2, d2);                             // muls d2,d2
        d3 = setw(0, Mem.uw(Aud_NoiseVol_w));          // move.w Aud_NoiseVol_w,d3
        Mem.ww(noiseloud, 32767);                      // move.w #32767,noiseloud
        d0 = 1;                                        // moveq #1,d0
        d2 = d1 + d2;                                  // add.l d1,d2
        if (d2 != 0) {                                 // beq pastcalc
            d0 = setw(0, 31);                          // move.w #31,d0
            while (true) {                             // .findhigh
                if ((d2 & (1 << (d0 & 31))) != 0) break; // btst d0,d2 ; bne .foundhigh
                d0 = setw(d0, d0 - 1);                 // dbra d0,.findhigh
                if ((short) d0 == -1) break;
            }
            // .foundhigh
            d0 = setw(d0, ((short) d0) >> 1);          // asr.w #1,d0
            d3 = (1 << (d0 & 31));                     // clr.l d3 ; bset d0,d3
            d0 = d3;                                   // move.l d3,d0
            for (int it = 0; it < 2; it++) {           // .stillnot0, .stillnot02
                d3 = setw(0, d0);                      // move.w d0,d3
                d3 = muls(d3, d3);                     // muls d3,d3  (x*x)
                d3 = d3 - d2;                          // sub.l d2,d3
                d3 = d3 >> 1;                          // asr.l #1,d3
                d3 = divs(d3, d0);                     // divs d0,d3
                d0 = setw(d0, d0 - d3);                // sub.w d3,d0
                if (!((short) d0 > 0)) d0 = setw(d0, 1); // bgt .stillnotX ; move.w #1,d0
            }
            d3 = setw(0, Mem.uw(Aud_NoiseVol_w));      // move.w Aud_NoiseVol_w,d3
            d3 = (short) d3;                           // ext.l d3
            d3 = d3 << 6;                              // asl.l #6,d3
            if (d3 > 32767) d3 = 32767;                // cmp.l #32767,d3 ; ble .nnnn ; move.l #32767,d3
            // .nnnn
            d0 = setw(d0, ((short) d0) >> 2);          // asr.w #2,d0
            d0 = setw(d0, d0 + 1);                     // addq #1,d0
            d3 = divs(d3, d0);                         // divs d0,d3
        }
        // pastcalc
        Mem.ww(noiseloud, d3);                         // move.w d3,noiseloud
        if ((short) d3 > 64) d3 = setw(d3, 64);        // cmp.w #64,d3 ; ble notooloud ; move.w #64,d3
        // notooloud — d3 = volume
        d4 = setw(0, d3);                              // move.w d3,d4

        if (Mem.b(Aud_Stereo_b) == 0) {                // tst.b Aud_Stereo_b ; beq NOSTEREO
            makeNoiseMono(d3, d4, d0);
            return;
        }

        // --- stéréo : répartition gauche/droite ---
        d2 = setw(0, d3);                              // move.w d3,d2
        d2 = muls(d2, Mem.uw(Aud_NoiseX_w));           // muls Aud_NoiseX_w,d2
        d0 = setw(d0, (d0 & 0xFFFF) << 2);             // asl.w #2,d0
        d2 = divs(d2, d0);                             // divs d0,d2
        if ((short) d2 > 0) {                          // bgt.s quietleft
            d3 = setw(d3, d3 - d2);                    // quietleft: sub.w d2,d3
            if ((short) d3 < 0) d3 = setw(d3, 0);      // bge.s donequiet ; move.w #0,d3
        } else {
            d4 = setw(d4, d4 + d2);                    // add.w d2,d4
            if ((short) d4 < 0) d4 = setw(d4, 0);      // bge.s donequiet ; move.w #0,d4
        }
        // donequiet — d3=gauche, d4=droite
        Mem.wb(Aud_NeedLeft_b, 0xFF);                  // move.w #$ffff,Aud_NeedLeft_b
        Mem.wl(RIGHTOFFSET, 0);                        // move.l #0,RIGHTOFFSET
        Mem.wl(LEFTOFFSET, 0);                         // move.l #0,LEFTOFFSET
        if ((byte) d4 > (byte) d3) {                   // cmp.b d3,d4 ; bgt.s RightLouder
            // RightLouder
            Mem.wl(RIGHTOFFSET, 4);                    // move.l #4,RIGHTOFFSET
            Mem.wb(Aud_NeedRight_b, 0xFF);             // st Aud_NeedRight_b
            d2 = setw(0, d4);                          // move.w d4,d2
            d2 = setw(d2, d2 - d3);                    // sub.w d3,d2
            Mem.wb(Aud_NeedLeft_b, ((short) d2 < 32) ? 0xFF : 0); // cmp.w #32,d2 ; slt Aud_NeedLeft_b
        } else if ((byte) d4 < (byte) d3) {            // beq.s NoLouder (sinon left louder)
            Mem.wl(LEFTOFFSET, 4);                     // move.l #4,LEFTOFFSET
            Mem.wb(Aud_NeedLeft_b, 0xFF);              // st Aud_NeedLeft_b
            d2 = setw(0, d3);                          // move.w d3,d2
            d2 = setw(d2, d2 - d4);                    // sub.w d4,d2
            Mem.wb(Aud_NeedRight_b, ((short) d2 < 32) ? 0xFF : 0); // cmp.w #32,d2 ; slt Aud_NeedRight_b
        }
        // aboutsame / NoLouder

        // FindLeftChannel
        a2 = 0;
        d5 = 0;
        d2 = setw(0, 32767);                           // move.w #32767,d2
        d0 = setw(0, Mem.uw(IDNUM));                   // move.w IDNUM,d0
        a3 = LEFTCHANDATA;                             // lea LEFTCHANDATA,a3
        d1 = setw(0, 3);                               // move.w #3,d1
        boolean foundLeft = false;
        while (true) {                                 // FindLeftChannel
            if (Mem.b(a3) == 0) {                      // tst.b (a3) ; bne.s .notactive
                if ((short) Mem.uw(a3 + 32) == (short) d0) { d6 = setw(0, d5); foundLeft = true; break; } // cmp.w 32(a3),d0 ; beq FOUNDLEFT
                if ((short) d2 >= (short) Mem.uw(a3 + 2)) { // cmp.w 2(a3),d2 ; blt.s .notactive (saute si d2<imp → traite si d2>=imp)
                    d2 = setw(d2, Mem.uw(a3 + 2));     // move.w 2(a3),d2
                    a2 = a3;                           // move.l a3,a2
                    d6 = setw(0, d5);                  // move.w d5,d6
                }
            }
            // .notactive
            a3 += 4;                                   // add.w #4,a3
            d5 = setw(d5, d5 + 1);                     // add.w #1,d5
            d1 = setw(d1, d1 - 1);                     // dbra d1,FindLeftChannel
            if ((short) d1 == -1) break;
        }
        if (!foundLeft) a3 = a2;                       // move.l a2,a3 (gopastleft)
        // gopastleft
        d5 = a3;                                       // move.l a3,d5
        if (d5 == 0) return;                           // tst.l d5 ; bne FOUNDALEFT ; NONOISE rts
        // FOUNDALEFT
        if ((short) d2 < (short) Mem.uw(noiseloud)) {  // cmp.w noiseloud,d2 ; bge dorightchan
            makeNoiseChannel(a3, d0);                  // pose IDNUM/importance (d6 = index canal conservé)
            d5 = setw(0, Mem.uw(Aud_SampleNum_w));     // move.w Aud_SampleNum_w,d5
            a3 = Aud_SampleList_vl;                    // move.l #Aud_SampleList_vl,a3
            a1 = Mem.l(a3 + (d5 & 0xFFFF) * 8) + Mem.l(LEFTOFFSET);     // move.l (a3,d5.w*8),a1 ; add.l LEFTOFFSET,a1
            a2 = Mem.l(a3 + (d5 & 0xFFFF) * 8 + 4) + Mem.l(LEFTOFFSET); // move.l 4(a3,d5.w*8),a2 ; add.l LEFTOFFSET,a2
            // tst.b d6 ; seq/slt/seq/st NoiseMadeXLEFT ; .chanN
            Mem.wb(NoiseMade0LEFT, (d6 == 0) ? 0xFF : 0); // seq NoiseMade0LEFT
            if (d6 == 0) writeLeftChan(0, d5, d3, d4, a1, a2);
            else {
                Mem.wb(NoiseMade1LEFT, (d6 < 2) ? 0xFF : 0); // slt NoiseMade1LEFT
                if (d6 < 2) writeLeftChan(1, d5, d3, d4, a1, a2);
                else {
                    Mem.wb(NoiseMade2LEFT, (d6 == 2) ? 0xFF : 0); // seq NoiseMade2LEFT
                    if (d6 == 2) writeLeftChan(2, d5, d3, d4, a1, a2);
                    else {
                        Mem.wb(NoiseMade3LEFT, 0xFF);  // st NoiseMade3LEFT
                        writeLeftChan(3, d5, d3, d4, a1, a2);
                    }
                }
            }
        }

        // dorightchan — FindRightChannel
        a2 = 0;
        d5 = 0;
        d2 = setw(0, 10000);                           // move.w #10000,d2
        d0 = setw(0, Mem.uw(IDNUM));                   // move.w IDNUM,d0
        a3 = RIGHTCHANDATA;                            // lea RIGHTCHANDATA,a3
        d1 = setw(0, 3);                               // move.w #3,d1
        boolean foundRight = false;
        while (true) {                                 // FindRightChannel
            if (Mem.b(a3) == 0) {                      // tst.b (a3) ; bne.s .notactive
                if ((short) Mem.uw(a3 + 32) == (short) d0) { d6 = setw(0, d5); foundRight = true; break; } // beq FOUNDRIGHT
                if ((short) d2 >= (short) Mem.uw(a3 + 2)) { // cmp.w 2(a3),d2 ; blt.s .notactive (saute si d2<imp → traite si d2>=imp)
                    d2 = setw(d2, Mem.uw(a3 + 2));
                    a2 = a3;
                    d6 = setw(0, d5);
                }
            }
            // .notactive
            a3 += 4;
            d5 = setw(d5, d5 + 1);
            d1 = setw(d1, d1 - 1);
            if ((short) d1 == -1) break;
        }
        if (!foundRight) a3 = a2;                       // move.l a2,a3 (gopastright)
        // gopastright
        d5 = a3;                                       // move.l a3,d5
        if (d5 == 0) return;                            // tst.l d5 ; bne FOUNDARIGHT ; tototot rts
        // FOUNDARIGHT
        if ((short) d2 > (short) Mem.uw(noiseloud)) return; // cmp.w noiseloud,d2 ; bgt.s tototot (rts)
        makeNoiseChannel(a3, d0);                       // set IDNUM/importance
        d5 = setw(0, Mem.uw(Aud_SampleNum_w));          // move.w Aud_SampleNum_w,d5
        a3 = Aud_SampleList_vl;                         // move.l #Aud_SampleList_vl,a3
        a1 = Mem.l(a3 + (d5 & 0xFFFF) * 8) + Mem.l(RIGHTOFFSET);     // (a3,d5.w*8) + RIGHTOFFSET
        a2 = Mem.l(a3 + (d5 & 0xFFFF) * 8 + 4) + Mem.l(RIGHTOFFSET); // 4(a3,d5.w*8) + RIGHTOFFSET
        Mem.wb(NoiseMade0RIGHT, (d6 == 0) ? 0xFF : 0);  // seq NoiseMade0RIGHT
        if (d6 == 0) writeRightChan(0, d5, d3, d4, a1, a2);
        else {
            Mem.wb(NoiseMade1RIGHT, (d6 < 2) ? 0xFF : 0); // slt NoiseMade1RIGHT
            if (d6 < 2) writeRightChan(1, d5, d3, d4, a1, a2);
            else {
                Mem.wb(NoiseMade2RIGHT, (d6 == 2) ? 0xFF : 0); // seq NoiseMade2RIGHT
                if (d6 == 2) writeRightChan(2, d5, d3, d4, a1, a2);
                else {
                    Mem.wb(NoiseMade3RIGHT, 0xFF);     // st NoiseMade3RIGHT
                    writeRightChan(3, d5, d3, d4, a1, a2);
                }
            }
        }
    }

    /** Commun aux assignations : pose IDNUM (32(a3)) et l'importance (2(a3)). Renvoie d6 inchangé. */
    private static int makeNoiseChannel(int a3, int d0) {
        if ((d0 & 0xFFFF) == 0xFFFF) d0 = setw(d0, 0xFFFE); // cmp.w #$ffff,d0 ; bne .noche ; move.w #$fffe,d0
        Mem.ww(a3 + 32, d0);                           // move.w d0,32(a3)
        Mem.ww(a3 + 2, Mem.uw(noiseloud));             // move.w noiseloud,2(a3)
        return d0;
    }

    /** DIAG : log une seule fois un échantillon hors RAM (sécurité hôte). */
    private static boolean dbgBadSampleLogged = false;

    /** Sécurité hôte : adresse d'échantillon valide dans la RAM plate (marge pour la lecture de 200 octets). */
    private static boolean sampleAddrOK(int addr) {
        return addr >= 0 && addr < Mem.RAM.length - 256;
    }

    /** Si l'adresse d'échantillon (pos/fin) est hors RAM (n° de SFX hors plage / entrée corrompue),
     *  bascule sur le buffer vide (silence) au lieu de planter le mixer. Renvoie [pos, fin] sûrs. */
    private static int[] guardSample(int a1, int a2, int sampleNum) {
        if (!sampleAddrOK(a1) || !sampleAddrOK(a2)) {
            if (!dbgBadSampleLogged) {
                System.err.println("[audio] échantillon hors RAM ignoré : Aud_SampleNum=" + sampleNum
                        + " pos=" + a1 + " fin=" + a2 + " (canal silencé)");
                dbgBadSampleLogged = true;
            }
            return new int[] { ab3d2.bss.TablesBss.Aud_EmptyBuffer_vl, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd };
        }
        return new int[] { a1, a2 };
    }

    /** Écrit l'état d'un canal gauche (chan 0-3). vol gauche = d3 pour tous. */
    private static void writeLeftChan(int chan, int d5, int d3, int d4, int a1, int a2) {
        int[] g = guardSample(a1, a2, d5); a1 = g[0]; a2 = g[1]; // sécurité hôte (adresse hors RAM)
        int t = LEFTPLAYEDTAB + chan * 3;
        Mem.wb(t, d5);
        Mem.wb(t + 1, d3);
        Mem.wb(t + 2, d4);
        switch (chan) {                                    // ASM : move.b d3,volXleft (octet, pas mot)
            case 0: Mem.wl(pos0LEFT, a1); Mem.wl(Samp0endLEFT, a2); Mem.wb(vol0left, d3); break;
            case 1: Mem.wb(vol1left, d3); Mem.wl(pos1LEFT, a1); Mem.wl(Samp1endLEFT, a2); break;
            case 2: Mem.wl(pos2LEFT, a1); Mem.wl(Samp2endLEFT, a2); Mem.wb(vol2left, d3); break;
            default: Mem.wb(vol3left, d3); Mem.wl(pos3LEFT, a1); Mem.wl(Samp3endLEFT, a2); break;
        }
    }

    /** Écrit l'état d'un canal droit (chan 0-3). vol : chan0/3 = d4, chan1/2 = d3. */
    private static void writeRightChan(int chan, int d5, int d3, int d4, int a1, int a2) {
        int[] g = guardSample(a1, a2, d5); a1 = g[0]; a2 = g[1]; // sécurité hôte (adresse hors RAM)
        int t = RIGHTPLAYEDTAB + chan * 3;
        Mem.wb(t, d5);
        Mem.wb(t + 1, d3);
        Mem.wb(t + 2, d4);
        switch (chan) {                                    // ASM : move.b dN,volXright (octet)
            case 0: Mem.wl(pos0RIGHT, a1); Mem.wl(Samp0endRIGHT, a2); Mem.wb(vol0right, d4); break;
            case 1: Mem.wb(vol1right, d3); Mem.wl(pos1RIGHT, a1); Mem.wl(Samp1endRIGHT, a2); break;
            case 2: Mem.wl(pos2RIGHT, a1); Mem.wl(Samp2endRIGHT, a2); Mem.wb(vol2right, d3); break;
            default: Mem.wb(vol3right, d4); Mem.wl(pos3RIGHT, a1); Mem.wl(Samp3endRIGHT, a2); break;
        }
    }

    /** NOSTEREO (hires.s:7898) — 8 canaux mono ; chan 0-3 → gauche, 4-7 → droite. */
    private static void makeNoiseMono(int d3, int d4, int d0prev) {
        int a2 = 0;
        int d5 = -1;                                   // move.l #-1,d5
        int d2 = setw(0, 32767);                       // move.w #32767,d2
        int d0 = setw(0, Mem.uw(IDNUM));               // move.w IDNUM,d0
        int a3 = CHANNELDATA;                          // lea CHANNELDATA,a3
        int d1 = setw(0, 7);                           // move.w #7,d1
        int d6 = -1;                                   // moveq #-1,d6
        boolean foundMine = false;
        while (true) {                                 // FindChannel
            if (Mem.b(a3) == 0) {                      // tst.b (a3) ; bne.s .notactive
                if ((short) Mem.uw(a3 + 32) == (short) d0) { foundMine = true; break; } // cmp.w 32(a3),d0 ; beq FOUNDMYCHAN
                if ((short) Mem.uw(a3 + 2) >= (short) d2) { // cmp.w 2(a3),d2 ; blt.s .notactive
                    d2 = setw(d2, Mem.uw(a3 + 2));     // move.w 2(a3),d2
                    a2 = a3;                           // move.l a3,a2
                    d6 = setw(d5, d5 + 1);             // move.w d5,d6 ; add.w #1,d6
                }
            }
            // .notactive
            a3 += 4;                                   // add.w #4,a3
            d5 = setw(d5, d5 + 1);                     // add.w #1,d5
            d1 = setw(d1, d1 - 1);                     // dbra d1,FindChannel
            if ((short) d1 == -1) break;
        }
        if (foundMine) {                               // FOUNDMYCHAN
            d2 = setw(d2, Mem.uw(a3 + 2));             // move.w 2(a3),d2
            // FOUNDCHAN
            d6 = setw(d5, d5 + 1);                     // move.w d5,d6 ; add.w #1,d6
        } else {
            a3 = a2;                                   // move.l a2,a3
        }
        // gopastchan
        if ((short) d6 < 0) return;                    // tst.w d6 ; bge.s FOUNDACHAN ; tooquiet rts
        // FOUNDACHAN
        if ((short) d2 > (short) Mem.uw(noiseloud)) return; // cmp.w noiseloud,d2 ; bgt.s tooquiet (rts)
        makeNoiseChannel(a3, d0);                       // set IDNUM/importance
        d5 = setw(0, Mem.uw(Aud_SampleNum_w));          // move.w Aud_SampleNum_w,d5
        a3 = Aud_SampleList_vl;                         // move.l #Aud_SampleList_vl,a3
        int a1 = Mem.l(a3 + (d5 & 0xFFFF) * 8);         // move.l (a3,d5.w*8),a1
        a2 = Mem.l(a3 + (d5 & 0xFFFF) * 8 + 4);         // move.l 4(a3,d5.w*8),a2
        // dispatch d6 0-7 (chan0-2 L, chan3 L, chan4-6 R, chan7 R)
        switch ((byte) d6) {
            case 0: Mem.wb(NoiseMade0LEFT, 0xFF); writeLeftChan(0, d5, d3, d4, a1, a2); break;
            case 1: Mem.wb(NoiseMade1LEFT, 0xFF); writeLeftChan(1, d5, d3, d4, a1, a2); break;
            case 2: Mem.wb(NoiseMade2LEFT, 0xFF); writeLeftChan(2, d5, d3, d4, a1, a2); break;
            case 3: Mem.wb(NoiseMade3LEFT, 0xFF); writeLeftChan(3, d5, d3, d4, a1, a2); break;
            case 4: Mem.wb(NoiseMade0RIGHT, 0xFF); writeRightChan(0, d5, d3, d4, a1, a2); break;
            case 5: Mem.wb(NoiseMade1RIGHT, 0xFF); writeRightChan(1, d5, d3, d4, a1, a2); break;
            case 6: Mem.wb(NoiseMade2RIGHT, 0xFF); writeRightChan(2, d5, d3, d4, a1, a2); break;
            default: Mem.wb(NoiseMade3RIGHT, 0xFF); writeRightChan(3, d5, d3, d4, a1, a2); break;
        }
    }

    // ==================================================================
    // hires.s — bring-up / boucle de frame / interruptions (COUCHE HÔTE).
    //
    // Le pipeline de RENDU de hires.s est traduit ci-dessus (RotateLevelPts,
    // Draw_Zone_Graph, etc.). Restent les entrypoints d'INTÉGRATION, qui sont
    // fondamentalement host/hardware (bring-up OS, boucle VBlank, présentation
    // écran + C2P exclu, interruptions clavier/VBlank) : conservés en stubs
    // documentés, à implémenter avec la couche hôte (prochaine session).
    // ==================================================================

    /**
     * _startup (hires.s 95-184) : point d'entrée moteur.
     *
     * Sys_Init (bring-up), init des états joueurs (énergie, méthode de contrôle
     * par défaut = souris en build non-CD32), remplissage de ConstantTable_vl
     * (table de mise à l'échelle des objets), puis Game_Start ; enfin Sys_Done.
     */
    public static void startup() {
        // movem.l d1-a6,-(sp) : sauvegarde de registres — sans objet en Java.
        // IFD MEMTRACK : non défini.

        int d0 = ab3d2.c.SystemC.Sys_Init();                 // CALLC Sys_Init
        if (d0 == 0) {                                        // tst.l d0 ; beq .startup_fail
            startup_fail();
            return;
        }

        // since these moved to bss, they need explicit initialisation
        // XXX les deux not.b suivants sont des NOPs (écrasés par le st plus bas)
        Mem.wb(Plr1_Mouse_b, ~Mem.b(Plr1_Mouse_b));          // not.b Plr1_Mouse_b
        Mem.wb(Plr2_Mouse_b, ~Mem.b(Plr2_Mouse_b));          // not.b Plr2_Mouse_b
        Mem.ww(Plr1_Energy_w, 191);                          // move.w #191,Plr1_Energy_w
        Mem.ww(Plr2_Energy_w, 191);                          // move.w #191,Plr2_Energy_w
        Mem.ww(Zone_OrderTable_Barrier_w, ~Mem.w(Zone_OrderTable_Barrier_w)); // not.w Zone_OrderTable_Barrier_w
        Mem.wb(draw_GouraudFlatsSelected_b, 0xFF);           // st draw_GouraudFlatsSelected_b

        // AddIntServer VBLANKInt : commenté dans l'original (boucle de frame hôte).

        DevInst.Dev_Init();                                  // CALLDEV Init

        // KEYInt AddIntServer : commenté dans l'original.

        // init default control method — IFNE CD32VER faux (CD32VER=0) → branche ELSE
        Mem.wb(Plr1_Keys_b, 0);                              // clr.b Plr1_Keys_b
        Mem.wb(Plr1_Path_b, 0);                              // clr.b Plr1_Path_b
        Mem.wb(Plr1_Mouse_b, 0xFF);                          // st Plr1_Mouse_b
        Mem.wb(Plr1_Joystick_b, 0);                          // clr.b Plr1_Joystick_b
        Mem.wb(Plr2_Keys_b, 0);                              // clr.b Plr2_Keys_b
        Mem.wb(Plr2_Path_b, 0);                              // clr.b Plr2_Path_b
        Mem.wb(Plr2_Mouse_b, 0xFF);                          // st Plr2_Mouse_b
        Mem.wb(Plr2_Joystick_b, 0);                          // clr.b Plr2_Joystick_b

        // Setup constant table
        int a0 = ConstantTable_vl;                           // move.l #ConstantTable_vl,a0
        d0 = 1;                                              // moveq #1,d0
        int d1 = 8191;                                       // move.w #8191,d1
        do {                                                 // .fill_const:
            int d2 = 16384 * 64;                            // move.l #16384*64,d2  (= 1<<20)
            d2 = d2 / d0;                                    // divs.l d0,d2
            int d3 = 64 * 64 * 65536;                       // move.l #64*64*65536,d3
            d3 = d3 / d2;                                    // divs.l d2,d3
            Mem.wl(a0, d3); a0 += 4;                        // move.l d3,(a0)+   (e#)
            d2 = d2 >> 1;                                   // asr.l #1,d2  (c#/2.0)
            d2 = d2 - 40 * 64;                             // sub.l #40*64,d2  (d#)
            d2 = d2 * d3;                                   // muls.l d3,d2  (d#*e#)
            d2 = d2 >> 6;                                  // asr.l #6,d2
            Mem.wl(a0, d2); a0 += 4;                        // move.l d2,(a0)+
            d0 += 1;                                        // addq #1,d0
        } while (--d1 != -1);                               // dbra d1,.fill_const

        // CALLC Game_Init : commenté dans l'original.

        Controlloop.Game_Start();                           // jsr Game_Start

        startup_fail();                                      // .startup_fail (chute)
    }

    /** .startup_fail : épilogue commun de _startup (Sys_Done). */
    private static void startup_fail() {
        ab3d2.c.SystemC.Sys_Done();                          // CALLC Sys_Done
        // IFD MEMTRACK : non défini. movem.l (sp)+,d1-a6 ; rts.
    }

    /**
     * Game_Begin (hires.s 255-817 = PROLOGUE) : initialise un niveau puis entre
     * dans la boucle de frame (game_main_loop, P2d).
     *
     * Prologue : SETPLAYERS, Res_LoadLevelData, Msg_Init, Game_LevelBegin, mise en
     * place des pointeurs Lvl_* depuis les offsets TLGT_/TLBT_, assignation des
     * clips PVS, Zone_ApplyPVSErrata/InitEdgePVS, init audio (CustomChips Paula/
     * CIA), table de volume `tab`, musique, Plr_Initialise, init zones/murs,
     * Draw_ResetGameDisplay, init FPS/joueurs.
     *
     * Bouts matériels remplacés par l'hôte : ChangeScreenBuffer (court-circuité par
     * `bra .skipChangeScreen` dans l'original), potgo/adkcon (entrée/modulation audio),
     * DataCacheOn (cache CPU) → no-op.
     */
    public static void Game_Begin() {
        // move.l #_custom,a6 : base custom — sans objet (écritures via CustomChips).
        // PLAYTHEGAME (hires.s) : en solo, texte narratif du niveau (TWEENTEXT) avant le jeu.
        if (Mem.b(Plr_MultiplayerType_b) == PLR_SINGLE) {
            ab3d2.c.ScreenC.Game_ShowIntroText();
        }
        Controlloop.SETPLAYERS();                            // jsr SETPLAYERS

        ab3d2.modules.Res.Res_LoadLevelData();               // jsr Res_LoadLevelData

        // noload:
        // IFNE CD32VER (faux) : CALLDOS Delay — omis.
        DevInst.Dev_DataReset();                             // CALLDEV DataReset
        ab3d2.c.Message.Msg_Init();                          // CALLC Msg_Init
        ab3d2.c.GameProgress.Game_LevelBegin();              // STATS_PLAY → Game_LevelBegin

        // *** Initialize level : pointeurs Lvl_* depuis le header graph (TLGT_) ***
        int a0 = Mem.l(Lvl_GraphicsPtr_l);                   // move.l Lvl_GraphicsPtr_l,a0

        int a1 = Mem.l(a0 + Defs.TLGT_DoorDataOffset_l) + a0;        // +a0
        Mem.wl(Lvl_DoorDataPtr_l, a1);
        a1 = Mem.l(a0 + Defs.TLGT_LiftDataOffset_l) + a0;
        Mem.wl(Lvl_LiftDataPtr_l, a1);
        a1 = Mem.l(a0 + Defs.TLGT_SwitchDataOffset_l) + a0;
        Mem.wl(Lvl_SwitchDataPtr_l, a1);
        a1 = Mem.l(a0 + Defs.TLGT_ZoneGraphAddsOffset_l) + a0;
        Mem.wl(Lvl_ZoneGraphAddsPtr_l, a1);

        a0 = a0 + (short) Defs.TLGT_ZoneAddsOffset_l;        // adda.w #TLGT_ZoneAddsOffset_l,a0
        Mem.wl(Lvl_ZonePtrsPtr_l, a0);                       // move.l a0,Lvl_ZonePtrsPtr_l

        // *** pointeurs Lvl_* depuis le header bin (TLBT_) ***
        int a4 = Mem.l(Lvl_DataPtr_l);                       // move.l Lvl_DataPtr_l,a4 (twolev.bin)
        a1 = a4 + Defs.LVLT_MESSAGE_LENGTH * Defs.LVLT_MESSAGE_COUNT; // lea (1600 octets de messages)

        int a2 = a1 + Defs.TLBT_SizeOf_l;                    // lea TLBT_SizeOf_l(a1),a2
        Mem.wl(Lvl_ControlPointCoordsPtr_l, a2);
        Mem.ww(Lvl_NumControlPoints_w, Mem.uw(a1 + Defs.TLBT_NumControlPoints_w));
        Mem.ww(Lvl_NumPoints_w, Mem.uw(a1 + Defs.TLBT_NumPoints_w));

        a2 = Mem.l(a1 + Defs.TLBT_PointsOffset_l) + a4;
        Mem.wl(Lvl_PointsPtr_l, a2);

        int d0 = Mem.uw(a1 + Defs.TLBT_NumPoints_w);         // move.w TLBT_NumPoints_w(a1),d0
        a2 = a2 + 4 + d0 * 4;                                // lea 4(a2,d0.w*4),a2
        Mem.wl(PointBrightsPtr_l, a2);

        d0 = Mem.uw(a1 + Defs.TLBT_NumZones_w);              // 1 de moins que le nb de zones
        d0 = d0 + 1;                                         // addq #1,d0
        Mem.ww(Lvl_NumZones_w, d0);

        d0 = muls(d0, 80);                                   // muls #80,d0
        a2 = a2 + d0;
        Mem.wl(Lvl_ZoneBorderPointsPtr_l, a2);

        a2 = Mem.l(a1 + Defs.TLBT_FloorLineOffset_l);        // move.l TLBT_FloorLineOffset_l(a1),a2
        d0 = Mem.l(a1 + Defs.TLBT_ObjectDataOffset_l);
        d0 = d0 - a2;                                        // sub.l a2,d0
        Mem.wl(Lvl_EdgeCount_l, d0);

        a2 = a2 + a4;
        Mem.wl(Lvl_ZoneEdgePtr_l, a2);

        Mem.ww(Lvl_ExitZoneID_w, Mem.w(a2 - 2));             // move.w -2(a2),Lvl_ExitZoneID_w
        a2 = Mem.l(a1 + Defs.TLBT_ObjectDataOffset_l) + a4;
        Mem.wl(Lvl_ObjectDataPtr_l, a2);
        if ("1".equals(System.getProperty("dumpRawObj"))) { // DIAG : instances brutes AVANT tout traitement
            int o = a2 - 64;
            for (int n = 0; n < 130; n++) { o += 64; if (Mem.w(o) < 0) break;
                if (Mem.ub(o + 16) == 1 && Mem.ub(o + 54) == 9) { // type OBJECT, def 9 (Passkey)
                    StringBuilder sb = new StringBuilder("[rawObj] Passkey #" + n + " bytes:");
                    for (int b = 0; b < 64; b++) sb.append(' ').append(b).append('=').append(Mem.ub(o + b));
                    System.out.println(sb);
                } }
        }

        a2 = Mem.l(a1 + Defs.TLBT_ShotDataOffset_l) + a4;
        Mem.wl(Plr_ShotDataPtr_l, a2);

        a2 = Mem.l(a1 + Defs.TLBT_AlienShotDataOffset_l) + a4;
        Mem.wl(AI_AlienShotDataPtr_l, a2);

        a2 = a2 + Defs.ShotT_SizeOf_l * Defs.NUM_ALIEN_SHOT_DATA;
        Mem.wl(AI_OtherAlienDataPtrs_vl, a2);

        a2 = Mem.l(a1 + Defs.TLBT_ObjectPointsOffset_l) + a4;
        Mem.wl(Lvl_ObjectPointsPtr_l, a2);

        a2 = Mem.l(a1 + Defs.TLBT_Plr1ObjectOffset_l) + a4;
        Mem.wl(Plr1_ObjectPtr_l, a2);

        a2 = Mem.l(a1 + Defs.TLBT_Plr2ObjectOffset_l) + a4;
        Mem.wl(Plr2_ObjectPtr_l, a2);

        Mem.ww(Lvl_NumObjectPoints_w, Mem.uw(a1 + Defs.TLBT_NumObjects_w));

        // *** Assignation des clips PVS (préconversion offsets→pointeurs) ***
        a2 = Mem.l(Lvl_ClipsPtr_l);                          // move.l Lvl_ClipsPtr_l,a2
        d0 = 0;                                              // moveq #0,d0
        int d7 = Mem.uw(a1 + Defs.TLBT_NumZones_w);          // move.w TLBT_NumZones_w(a1),d7
        Mem.ww(NewanimsData.Zone_Count_w, d7);              // move.w d7,Zone_Count_w

        do {                                                 // .assign_clips:
            int a3 = Mem.l(a0) + a4;                         // move.l (a0),a3 ; add.l a4,a3
            Mem.wl(a0, a3); a0 += 4;                         // move.l a3,(a0)+
            a3 = a3 + Defs.ZoneT_PotVisibleZoneList_vw;      // adda.w #ZoneT_PotVisibleZoneList_vw,a3

            // .do_whole_zone:
            while (Mem.w(a3) >= 0) {                         // tst.w (a3) ; blt .no_more_this_zone
                if (Mem.w(a3 + Defs.PVST_ClipID_w) >= 0) {   // tst.w PVST_ClipID_w(a3) ; blt .this_one_null
                    int d1 = d0 >> 1;                        // move.l d0,d1 ; asr.l #1,d1
                    Mem.ww(a3 + Defs.PVST_ClipID_w, d1);     // move.w d1,PVST_ClipID_w(a3)
                    while (Mem.w(a2 + d0) != -2) {           // .find_next_clip: cmp.w #-2,(a2,d0.l) ; bne
                        d0 += 2;                             // addq.l #2,d0
                    }
                    d0 += 2;                                 // .found_next_clip: addq.l #2,d0
                }
                a3 += Defs.PVST_SizeOf_l;                    // .this_one_null: addq #PVST_SizeOf_l,a3
            }
            // .no_more_this_zone
        } while (--d7 != -1);                                // dbra d7,.assign_clips

        a2 = a2 + d0;                                        // lea (a2,d0.l),a2
        Mem.wl(Lvl_ConnectTablePtr_l, a2);                  // move.l a2,Lvl_ConnectTablePtr_l

        // PVS errata + edge PVS (DEV_CHECK_SET SKIP_PVS_AMEND → ignore si flag)
        if (!DevMacros.DEV_TEST(DevInst.Dev_DebugFlags_l, DevMacros.DEV_SKIP_PVS_AMEND)) {
            if (Mem.l(Lvl_ErrataPtr_l) != 0) {               // tst.l Lvl_ErrataPtr_l ; beq .done_errata
                ab3d2.c.ZoneErrata.Zone_ApplyPVSErrata(Mem.l(Lvl_ErrataPtr_l)); // CALLC Zone_ApplyPVSErrata
            }
        }
        ab3d2.c.ZoneEdgePvs.Zone_InitEdgePVS();              // CALLC Zone_InitEdgePVS

        // .noclips:
        Mem.wb(Plr1_StoodInTop_b, 0);                        // clr.b Plr1_StoodInTop_b
        Mem.wl(Plr1_SnapHeight_l, PLR_STAND_HEIGHT);         // move.l #PLR_STAND_HEIGHT,Plr1_SnapHeight_l

        // init des pointeurs de lecture audio sur le buffer vide
        Mem.wl(pos1LEFT, Aud_EmptyBuffer_vl);
        Mem.wl(pos2LEFT, Aud_EmptyBuffer_vl);
        Mem.wl(pos1RIGHT, Aud_EmptyBuffer_vl);
        Mem.wl(pos2RIGHT, Aud_EmptyBuffer_vl);
        Mem.wl(pos0LEFT, Aud_EmptyBuffer_vl);
        Mem.wl(pos3LEFT, Aud_EmptyBuffer_vl);
        Mem.wl(pos0RIGHT, Aud_EmptyBuffer_vl);
        Mem.wl(pos3RIGHT, Aud_EmptyBuffer_vl);
        Mem.wl(Samp0endLEFT, Aud_EmptyBufferEnd);
        Mem.wl(Samp1endLEFT, Aud_EmptyBufferEnd);
        Mem.wl(Samp0endRIGHT, Aud_EmptyBufferEnd);
        Mem.wl(Samp1endRIGHT, Aud_EmptyBufferEnd);
        Mem.wl(Samp2endLEFT, Aud_EmptyBufferEnd);
        Mem.wl(Samp3endLEFT, Aud_EmptyBufferEnd);
        Mem.wl(Samp2endRIGHT, Aud_EmptyBufferEnd);
        Mem.wl(Samp3endRIGHT, Aud_EmptyBufferEnd);

        CustomChips.ciaBset(1);                              // bset.b #1,$bfe001 (filtre/LED audio)
        // move.w #$00ff,_custom+adkcon : modulation audio off — no-op host.

        // bra.s .skipChangeScreen : le bloc ChangeScreenBuffer/DISPLAYMSGPORT_HACK
        // est court-circuité dans l'original → omis (présentation via l'hôte).

        // .skipChangeScreen:
        ab3d2.modules.Player.Plr_Initialise();               // jsr Plr_Initialise

        // setup audio channels (Paula AUDxLC/LEN/PER/VOL → CustomChips)
        CustomChips.write32(0xdff0a0, Aud_Null1_vw);
        CustomChips.write16(0xdff0a4, 100);
        CustomChips.write16(0xdff0a6, 443);
        CustomChips.write16(0xdff0a8, 63);
        CustomChips.write32(0xdff0b0, Aud_Null2_vw);
        CustomChips.write16(0xdff0b4, 100);
        CustomChips.write16(0xdff0b6, 443);
        CustomChips.write16(0xdff0b8, 63);
        CustomChips.write32(0xdff0c0, Aud_Null4_vw);
        CustomChips.write16(0xdff0c4, 100);
        CustomChips.write16(0xdff0c6, 443);
        CustomChips.write16(0xdff0c8, 63);
        CustomChips.write32(0xdff0d0, Aud_Null3_vw);
        CustomChips.write16(0xdff0d4, 100);
        CustomChips.write16(0xdff0d6, 443);
        CustomChips.write16(0xdff0d8, 63);

        // table de mise à l'échelle du volume : tab[d6][i] = (pretab[i] * d6) >> 6
        int aTab = tab;                                      // move.l #tab,a1
        d7 = 64;                                             // move.w #64,d7
        int d6 = 0;                                          // move.w #0,d6
        do {                                                 // outerlop:
            int aPre = pretab;                              // move.l #pretab,a0
            int d5 = 255;                                   // move.w #255,d5
            do {                                            // scaledownlop:
                d0 = Mem.b(aPre); aPre++;                   // move.b (a0)+,d0 ; ext.w ; ext.l
                d0 = muls(d0, d6);                          // muls d6,d0
                d0 = d0 >> 6;                               // asr.l #6,d0
                Mem.wb(aTab, d0); aTab++;                   // move.b d0,(a1)+
            } while (--d5 != -1);                           // dbra d5,scaledownlop
            d6 += 1;                                        // addq #1,d6
        } while (--d7 != -1);                               // dbra d7,outerlop

        // DMA audio : disable puis enable (set/clear via CustomChips)
        CustomChips.write16(0xdff096, DMAF_AUDIO);                       // disable audio dma
        CustomChips.write16(0xdff096, DMAF_SETCLR | DMAF_MASTER | DMAF_AUDIO); // enable
        // move.w #$0,potgo : entrée pot (boutons) — host no-op.
        Mem.ww(NewanimsData.Conditions, 0);                  // move.w #0,Conditions

        if (Mem.b(Plr_MultiplayerType_b) != PLR_SINGLE) {    // cmp.b #PLR_SINGLE ; beq .nokeys
            Mem.ww(NewanimsData.Conditions, 0xFFF);          // move.w #%111111111111,Conditions
        }
        // .nokeys:
        Mem.wb(KeyMap_vb + RawKeyMacros.RAWKEY_ESC, 0);      // clr.b RAWKEY_ESC(a5)

        Mem.wl(mt_data, Mem.l(Lvl_MusicPtr_l));              // move.l Lvl_MusicPtr_l,mt_data
        Mem.wb(UseAllChannels, 0);                           // clr.b UseAllChannels

        Mem.wb(CHANNELDATA, 0xFF);                           // st CHANNELDATA
        ab3d2.modules.Music.mt_init();                       // jsr mt_init

        Mem.wb(CHANNELDATA, 0xFF);                           // st CHANNELDATA
        Mem.wb(CHANNELDATA + 8, 0xFF);                       // st CHANNELDATA+8

        Mem.wl(pos0LEFT, Mem.l(Aud_SampleList_vl + 6 * 8));          // move.l Aud_SampleList_vl+6*8,pos0LEFT
        Mem.wl(Samp0endLEFT, Mem.l(Aud_SampleList_vl + 6 * 8 + 4));  // move.l Aud_SampleList_vl+6*8+4,Samp0endLEFT
        Mem.wl(Plr1_SnapTargHeight_l, PLR_STAND_HEIGHT);
        Mem.wl(Plr1_SnapHeight_l, PLR_STAND_HEIGHT);
        Mem.wl(Plr2_SnapTargHeight_l, PLR_STAND_HEIGHT);
        Mem.wl(Plr2_SnapHeight_l, PLR_STAND_HEIGHT);

        ab3d2.c.SystemC.Sys_ClearKeyboard();                 // CALLC Sys_ClearKeyboard

        Mem.wb(Game_MasterQuit_b, 0);                        // clr.b Game_MasterQuit_b
        Mem.wb(Game_SlaveQuit_b, Mem.b(Plr_MultiplayerType_b) == PLR_SINGLE ? 0xFF : 0); // seq Game_SlaveQuit_b

        // DataCacheOn : cache CPU — no-op host.
        Mem.wl(hitcol, 0);                                   // move.l #0,hitcol

        // NOCLTXT:
        Mem.wb(Plr1_Ducked_b, 0);                            // clr.b Plr1_Ducked_b
        Mem.wb(Plr2_Ducked_b, 0);                            // clr.b Plr2_Ducked_b
        Mem.wb(plr1_TmpDucked_b, 0);                         // clr.b plr1_TmpDucked_b
        Mem.wb(plr2_TmpDucked_b, 0);                         // clr.b plr2_TmpDucked_b

        Mem.wb(Game_Running_b, 0xFF);                        // st Game_Running_b
        Mem.wb(dosounds, 0xFF);                              // st dosounds

        ab3d2.modules.Ai.AI_InitAlienWorkspace();            // jsr AI_InitAlienWorkspace

        a0 = Lvl_CompactMap_vl;                              // move.l #Lvl_CompactMap_vl,a0
        Mem.wl(LastZonePtr_l, a0);                           // move.l a0,LastZonePtr_l
        ab3d2.modules.Sys.Sys_MemFillLong(a0, 0, 256);       // clr d0 ; move.w #256,d1 ; bsr Sys_MemFillLong

        // a0 = Lvl_CompactMap_vl ; a1 = Lvl_BigMap_vl ; bra NOALLWALLS (DOALLWALLS = unreachable)

        // NOALLWALLS:
        Mem.ww(Vid_CentreX_w, SMALL_WIDTH / 2);              // move.w #SMALL_WIDTH/2,Vid_CentreX_w
        Mem.ww(Vid_RightX_w, SMALL_WIDTH);                   // move.w #SMALL_WIDTH,Vid_RightX_w
        Mem.ww(Vid_BottomY_w, SMALL_HEIGHT);                 // move.w #SMALL_HEIGHT,Vid_BottomY_w
        Mem.ww(TOTHEMIDDLE, SMALL_HEIGHT / 2);               // move.w #SMALL_HEIGHT/2,TOTHEMIDDLE
        Mem.wb(Vid_FullScreen_b, 0);                         // clr.b Vid_FullScreen_b
        ab3d2.c.DrawC.Draw_ResetGameDisplay();               // CALLC Draw_ResetGameDisplay

        Mem.wb(Plr1_Weapons_vb + 1, 0xFF);                   // st Plr1_Weapons_vb+1
        Mem.wb(Plr2_Weapons_vb + 1, 0xFF);                   // st Plr2_Weapons_vb+1
        Mem.ww(timetodamage, 100);                           // move.w #100,timetodamage
        d0 = 299;                                            // move.w #299,d0
        a0 = AI_Damaged_vw;                                  // move.l #AI_Damaged_vw,a0
        do {                                                 // CLRDAM:
            Mem.ww(a0, 0); a0 += 2;                          // move.w #0,(a0)+
        } while (--d0 != -1);                                // dbra d0,CLRDAM

        d0 = 0;                                              // moveq #0,d0
        Mem.ww(STOPOFFSET, d0);                              // move.w d0,STOPOFFSET
        d0 = -d0;                                            // neg.w d0
        d0 = (short) (d0 + Mem.w(TOTHEMIDDLE));              // add.w TOTHEMIDDLE,d0
        Mem.ww(SMIDDLEY, d0);                                // move.w d0,SMIDDLEY
        d0 = muls(d0, SCREEN_WIDTH);                         // muls #SCREEN_WIDTH,d0
        Mem.wl(SBIGMIDDLEY, d0);                             // move.l d0,SBIGMIDDLEY

        Mem.ww(Plr1_AimSpeed_l, 0);                          // move.w #0,Plr1_AimSpeed_l
        Mem.ww(Plr2_AimSpeed_l, 0);                          // move.w #0,Plr2_AimSpeed_l

        // init pointeurs render buffers chipmem (planar) — sans effet en RTG
        Mem.wl(Vid_DisplayScreenPtr_l, Mem.l(Vid_Screen1Ptr_l)); // move.l Vid_Screen1Ptr_l,Vid_DisplayScreenPtr_l
        Mem.wl(Vid_DrawScreenPtr_l, Mem.l(Vid_Screen2Ptr_l));    // move.l Vid_Screen2Ptr_l,Vid_DrawScreenPtr_l

        // init FPS
        Mem.wl(Sys_FrameNumber_l, 0);                        // clr.l Sys_FrameNumber_l
        ab3d2.c.SystemC.Sys_MarkTime(Sys_PrevFrameTimeECV_q);// lea Sys_PrevFrameTimeECV_q,a0 ; CALLC Sys_MarkTime

        Mem.wb(Plr2_Fire_b, 0);                              // clr.b Plr2_Fire_b
        Mem.wb(Plr2_TmpFire_b, 0);                           // clr.b Plr2_TmpFire_b
        Mem.wb(Plr2_Used_b, 0);                              // clr.b Plr2_Used_b
        Mem.wb(Plr2_TmpSpcTap_b, 0);                         // clr.b Plr2_TmpSpcTap_b

        Mem.wb(plr1_Dead_b, 0);                              // clr.b plr1_Dead_b
        Mem.wb(plr2_Dead_b, 0);                              // clr.b plr2_Dead_b

        a0 = Mem.l(Plr1_ObjectPtr_l);                        // move.l Plr1_ObjectPtr_l,a0
        a1 = Mem.l(Plr2_ObjectPtr_l);                        // move.l Plr2_ObjectPtr_l,a1
        Mem.ww(a0 + Defs.EntT_ImpactX_w, 0);                 // clr.w EntT_ImpactX_w(a0)
        Mem.ww(a0 + Defs.EntT_ImpactY_w, 0);                 // clr.w EntT_ImpactY_w(a0)
        Mem.ww(a0 + Defs.EntT_ImpactZ_w, 0);                 // clr.w EntT_ImpactZ_w(a0)
        Mem.ww(a1 + Defs.EntT_ImpactX_w, 0);                 // clr.w EntT_ImpactX_w(a1)
        Mem.ww(a1 + Defs.EntT_ImpactY_w, 0);                 // clr.w EntT_ImpactY_w(a1)
        Mem.ww(a1 + Defs.EntT_ImpactZ_w, 0);                 // clr.w EntT_ImpactZ_w(a1)

        Mem.wl(Plr1_SnapXSpdVal_l, 0);                       // clr.l Plr1_SnapXSpdVal_l
        Mem.wl(Plr1_SnapZSpdVal_l, 0);                       // clr.l Plr1_SnapZSpdVal_l
        Mem.wl(Plr1_SnapYVel_l, 0);                          // clr.l Plr1_SnapYVel_l
        Mem.wl(Plr2_SnapXSpdVal_l, 0);                       // clr.l Plr2_SnapXSpdVal_l
        Mem.wl(Plr2_SnapZSpdVal_l, 0);                       // clr.l Plr2_SnapZSpdVal_l
        Mem.wl(Plr2_SnapYVel_l, 0);                          // clr.l Plr2_SnapYVel_l

        // réglage du regard vertical selon plein écran/petit écran
        if (Mem.b(Vid_FullScreen_b) != 0) {                  // tst.b Vid_FullScreen_b ; beq .small
            Mem.ww(View_KeyLook_w, 6);                       // move.w #6,View_KeyLook_w
            d0 = FS_HEIGHT / 2;                              // move.w #FS_HEIGHT/2,d0
            Mem.ww(View_LookMin_w, d0);                      // move.w d0,View_LookMin_w
            d0 = -d0;                                        // neg.w d0
            Mem.ww(View_LookMax_w, d0);                      // move.w d0,View_LookMax_w
        } else {                                             // .small
            Mem.ww(View_KeyLook_w, 4);                       // move.w #4,View_KeyLook_w
            d0 = SMALL_HEIGHT / 2;                           // move.w #SMALL_HEIGHT/2,d0
            Mem.ww(View_LookMin_w, d0);                      // move.w d0,View_LookMin_w
            d0 = -d0;                                        // neg.w d0
            Mem.ww(View_LookMax_w, d0);                      // move.w d0,View_LookMax_w
        }
        // .big

        // mode 1x2 (AGA) si Vid_DoubleHeight_b — sans objet en RTG
        if (Mem.b(Vid_DoubleHeight_b) != 0) {                // tst Vid_DoubleHeight_b ; beq .skipDH
            Mem.wb(LASTDH, 0xFF);                            // st LASTDH
            startCopper();                                   // bsr startCopper
            ab3d2.c.ScreenC.vid_SetupDoubleheightCopperlist(); // CALLC vid_SetupDoubleheightCopperlist
        }
        // .skipDH:

        game_main_loop();                                    // chute dans game_main_loop (P2d)
    }

    /**
     * game_main_loop (hires.s 818-2133) : boucle de frame principale. À TRADUIRE
     * (P2d) : entrée, pause, série (solo = no-op), présentation, Sys_FrameLap,
     * eau/anim, visibilité IA, Plr_Use, Zone_OrderZones, objmoveanim, DrawDisplay,
     * carte, viseur, FPS, palette, progression, Vid_Present, fin de niveau.
     */
    private static void game_main_loop() {
        // a2 = registre HÉRITÉ consommé par Plr1/2_Control→Obj_DoCollision. En régime
        // permanent il vaut AI_AlienTeamWorkspace_vl (fixé en fin de boucle, bloc worry).
        int a2 = ab3d2.bss.AiBss.AI_AlienTeamWorkspace_vl;

        while (true) {                                       // game_main_loop:
            if (loopFrameLimit >= 0 && loopFrameLimit-- == 0) return; // hook de test (arrêt après N frames)
            if (loopFrameLimit < 0) {                        // mode interactif (hôte) : sortie sur fermeture fenêtre
                ab3d2.host.Display d = ab3d2.c.ScreenC.hostDisplay();
                if (d != null && d.shouldClose()) return;
                // ESC est géré nativement : en solo Game_SlaveQuit_b est déjà =$FF (Game_Begin),
                // donc ESC pose Game_MasterQuit_b → (Master && Slave) → endnomusic → sortie propre.
            }
            VBlankInterrupt();                               // hôte : tick VBL par frame (entrée + MAJ jeu)
            // move.w #%110000000000,_custom+potgo : lecture pot (boutons) — no-op host.

            // --- messages de mort 2 joueurs (master tue J2, slave tue J1) ---
            if (Mem.b(Plr_MultiplayerType_b) == PLR_MASTER && Mem.b(plr2_Dead_b) == 0
                    && !((short) Mem.w(PlayerBss.Plr2_Health_w) > 0)) {  // bgt .notmess
                Mem.wb(plr2_Dead_b, 0xFF);                   // st plr2_Dead_b
                int d0 = Objectmove.GetRand();               // jsr GetRand
                d0 = swap(d0); d0 = setw(d0, 0); d0 = swap(d0); // swap;clr.w;swap (garde mot fort)
                d0 = divs(d0, 9); d0 = swap(d0);             // divs #9 ; swap (reste→mot bas)
                d0 = muls(d0, Defs.GAME_DM_VICTORY_MESSAGE_LENGTH); // muls #...,d0
                d0 = d0 + ab3d2.data.TextData.Game_TwoPlayerVictoryMessages_vb; // add.l #...,d0
                int aMsg = d0;                               // move.l d0,a0
                ab3d2.c.Message.Msg_PushLine(aMsg, Defs.GAME_DM_VICTORY_MESSAGE_LENGTH); // CALLC Msg_PushLine
                int a0 = Mem.l(PlayerBss.Plr2_ObjectPtr_l);  // move.l Plr2_ObjectPtr_l,a0
                int a6 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_Player2Graphic_w;
                int d7 = Mem.w(a6); int d1 = d7;
                a6 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_AlienDefs_l;
                d1 = muls(d1, Defs.AlienT_SizeOf_l); a6 = a6 + d1;
                Mem.wb(ab3d2.bss.AnimBss.Anim_SplatType_w, Mem.b(a6 + Defs.AlienT_SplatType_w + 1)); // move.b SplatType+1
                int a1 = Mem.l(PlayerBss.Plr2_ZonePtr_l);
                Mem.ww(a0 + Defs.ObjT_ZoneID_w, Mem.w(a1));
                Mem.ww(ObjectmoveData.newx, Mem.w(PlayerBss.Plr2_TmpXOff_l));
                Mem.ww(ObjectmoveData.newz, Mem.w(PlayerBss.Plr2_TmpZOff_l));
                int d2 = 7;
                Newanims.Anim_ExplodeIntoBits(0, d2, /*d3*/0, a0); // jsr Anim_ExplodeIntoBits (d0=GetRand? voir note)
                ab3d2.Macros.FREE_OBJ(a0);
            }
            // .notmess:
            if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE && Mem.b(plr1_Dead_b) == 0
                    && !((short) Mem.w(PlayerBss.Plr1_Health_w) > 0)) {  // bgt .notmess2
                Mem.wb(plr1_Dead_b, 0xFF);
                int d0 = Objectmove.GetRand();
                d0 = swap(d0); d0 = setw(d0, 0); d0 = swap(d0);
                d0 = divs(d0, 9); d0 = swap(d0);
                d0 = muls(d0, Defs.GAME_DM_VICTORY_MESSAGE_LENGTH);
                d0 = d0 + ab3d2.data.TextData.Game_TwoPlayerVictoryMessages_vb;
                ab3d2.c.Message.Msg_PushLine(d0, Defs.GAME_DM_VICTORY_MESSAGE_LENGTH);
                int a0 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);
                int a6 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_Player1Graphic_w;
                int d7 = Mem.w(a6); int d1 = d7;
                a6 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_AlienDefs_l;
                d1 = muls(d1, Defs.AlienT_SizeOf_l); a6 = a6 + d1;
                Mem.wb(ab3d2.bss.AnimBss.Anim_SplatType_w, Mem.b(a6 + Defs.AlienT_SplatType_w + 1));
                int a1 = Mem.l(PlayerBss.Plr1_ZonePtr_l);
                Mem.ww(a0 + Defs.ObjT_ZoneID_w, Mem.w(a1));
                Mem.ww(ObjectmoveData.newx, Mem.w(PlayerBss.Plr1_TmpXOff_l));
                Mem.ww(ObjectmoveData.newz, Mem.w(PlayerBss.Plr1_TmpZOff_l));
                Newanims.Anim_ExplodeIntoBits(0, 7, 0, a0);
                ab3d2.Macros.FREE_OBJ(a0);
            }
            // .notmess2: potgo no-op
            Mem.wb(draw_RenderMap_b, Mem.b(MAPON));          // move.b MAPON,draw_RenderMap_b

            // --- bascule plein écran ---
            if (((Mem.b(Vid_FullScreenTemp_b) ^ Mem.b(Vid_FullScreen_b)) & 0xFF) != 0) { // eor.b ; beq .noFullscreenSwitch
                Mem.wb(Vid_FullScreen_b, Mem.b(Vid_FullScreenTemp_b));
                SetupRenderbufferSize();                     // bsr SetupRenderbufferSize
                ab3d2.c.ScreenC.vid_SetupDoubleheightCopperlist();
            }
            // .noFullscreenSwitch:
            // --- pause solo (touche P) ---
            if (Mem.b(Plr_MultiplayerType_b) == PLR_SINGLE && Mem.b(KeyMap_vb + RawKeyMacros.RAWKEY_P) != 0) {
                Mem.wb(Game_Running_b, 0);                   // clr.b Game_Running_b
                do {                                         // .waitrel:
                    if (Mem.b(Plr1_Joystick_b) != 0) Cd32joy._ReadJoy1(); // tst Joystick ; jsr _ReadJoy1
                } while (Mem.b(KeyMap_vb + RawKeyMacros.RAWKEY_P) != 0); // tst RAWKEY_P ; bne .waitrel
                Pauseopts.Game_Pause();                      // bsr Game_Pause
                Mem.wb(Game_Running_b, 0xFF);                // st Game_Running_b
            }
            // .nopause / nofadedownhc:
            Mem.wb(READCONTROLS, 0xFF);                      // st READCONTROLS
            // a6=$dff000 (sans objet)

            // --- pause / synchronisation 2 joueurs ---
            if (Mem.b(Plr_MultiplayerType_b) != PLR_SINGLE) { // cmp #PLR_SINGLE ; beq .nopause
                int d0 = (Mem.b(Game_SlavePaused_b) | Mem.b(Game_MasterPaused_b)) & 0xFF; // or.b
                if (d0 != 0) {                               // beq .nopause
                    Mem.wb(Game_Running_b, 0);
                    do {                                     // .waitrel:
                        if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) {
                            if (Mem.b(Plr2_Joystick_b) != 0) Cd32joy._ReadJoy2();
                        } else if (Mem.b(Plr1_Joystick_b) != 0) {
                            Cd32joy._ReadJoy1();
                        }
                    } while (Mem.b(KeyMap_vb + RawKeyMacros.RAWKEY_P) != 0);
                    Pauseopts.Game_Pause();
                    if (Mem.b(Plr_MultiplayerType_b) == PLR_MASTER) SerialNightmare.SENDFIRST(0); // jsr SENDFIRST
                    else SerialNightmare.RECFIRST(0);        // jsr RECFIRST
                    Mem.wb(Game_SlavePaused_b, 0);
                    Mem.wb(Game_MasterPaused_b, 0);
                    Mem.wb(Game_Running_b, 0xFF);
                }
            }
            // .nopause: --- limiteur FPS (attente VBL) ---
            int fpsLimit = Mem.w(SystemBss_Sys_FPSLimit_w()); // move.w Sys_FPSLimit_w,d2 ; bmi .no_vbl
            if (fpsLimit >= 0) {
                int d2 = (fpsLimit & 0xFFFF) + Mem.l(Vid_VBLCountLast_l); // add.l Vid_VBLCountLast_l,d2
                while (!(d2 < Mem.l(Vid_VBLCount_l))) {      // .waitvbl: cmp.l Vid_VBLCount_l,d2 ; blt .skipWaitTOF
                    WaitTOF();                               // CALLGRAF WaitTOF
                }
                Mem.wl(Vid_VBLCountLast_l, Mem.l(Vid_VBLCount_l)); // .skipWaitTOF: move.l Vid_VBLCount_l,Vid_VBLCountLast_l
            }
            // .no_vbl: --- échange des pointeurs d'écran (planar ; inerte en RTG) ---
            int swp = Mem.l(ab3d2.bss.VidBss.Vid_DrawScreenPtr_l);
            Mem.wl(ab3d2.bss.VidBss.Vid_DrawScreenPtr_l, Mem.l(ab3d2.bss.VidBss.Vid_DisplayScreenPtr_l));
            Mem.wl(ab3d2.bss.VidBss.Vid_DisplayScreenPtr_l, swp);

            if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) { // cmp PLR_SLAVE ; beq nowaitslave
                Mem.wb(plr_GunSelected_b, Mem.b(PlayerBss.Plr2_GunSelected_b)); // nowaitslave
            } else {
                Mem.wb(plr_GunSelected_b, Mem.b(PlayerBss.Plr1_GunSelected_b)); // waitmaster (J1)
            }
            // waitmaster: tst _Vid_isRTG bne .screenSwapDone → RTG : on saute le flip ChangeScreenBuffer.

            // .screenSwapDone:
            ab3d2.c.SystemC.Sys_FrameLap();                  // CALLC Sys_FrameLap
            DevInst.Dev_PrintStats();                        // CALLDEV PrintStats
            DevInst.Dev_MarkFrameBegin();                    // CALLDEV MarkFrameBegin
            // IFND DEV Sys_ShowFPS : build DEV → non appelé.

            // SMIDDLEY/SBIGMIDDLEY → Vid_CentreY_w (copie 8 octets via movem)
            int d0 = Mem.l(SMIDDLEY);                         // movem.l (a0)+,d0/d1
            int d1 = Mem.l(SMIDDLEY + 4);
            Mem.wl(Vid_CentreY_w, d0);                        // move.l d0,Vid_CentreY_w
            Mem.wl(Vid_CentreY_w + 4, d1);                    // move.l d1,Vid_CentreY_w+4

            // animation de l'eau
            int wp = Mem.l(draw_LastWaterFramePtr_l);         // move.l draw_LastWaterFramePtr_l,a0
            Mem.wl(draw_WaterFramePtr_l, Mem.l(wp)); wp += 4; // move.l (a0)+,draw_WaterFramePtr_l
            if (wp >= draw_EndWaterFramePtrs_l) {             // cmp.l #...,a0 ; blt okwat
                wp = draw_WaterFramePtrs_vl;                  // move.l #draw_WaterFramePtrs_vl,a0
            }
            // okwat:
            Mem.wl(draw_LastWaterFramePtr_l, wp);             // move.l a0,draw_LastWaterFramePtr_l
            Mem.ww(wtan, ab3d2.data.TablesData.AMOD_I(setw(Mem.w(wtan), Mem.w(wtan) + 640))); // add.w #640,wtan ; AMOD_I
            Mem.wl(wateroff, (Mem.l(wateroff) + 1) & 0x3fff3fff); // add.l #1,wateroff ; and.l #$3fff3fff

            Mem.wl(plr1_OldX_l, Mem.l(PlayerBss.Plr1_XOff_l)); // move.l Plr1_XOff_l,plr1_OldX_l
            Mem.wl(plr1_OldZ_l, Mem.l(PlayerBss.Plr1_ZOff_l));
            Mem.wl(plr2_OldX_l, Mem.l(PlayerBss.Plr2_XOff_l));
            Mem.wl(plr2_OldZ_l, Mem.l(PlayerBss.Plr2_ZOff_l));

            // --- traitement joueur (solo / master / slave) ---
            int mpt = Mem.b(Plr_MultiplayerType_b);
            if (mpt == PLR_SLAVE) {                           // cmp PLR_SLAVE ; beq ASlaveShouldWaitOnHisMaster
                loop_SlaveBlock();                            // (2 joueurs, série)
            } else if (mpt == PLR_SINGLE) {                   // cmp PLR_SINGLE ; bne NotOnePlayer
                // SAVEREGS/GETREGS : ammo affichée
                int gd0 = Mem.ub(plr_GunSelected_b);          // move.b plr_GunSelected_b,d0
                int a6 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_ShootDefs_l;
                gd0 = Mem.w(a6 + gd0 * 8);                    // move.w (a6,d0.w*8),d0
                a6 = PlayerBss.Plr1_AmmoCounts_vw;
                gd0 = Mem.w(a6 + gd0 * 2);                    // move.w (a6,d0.w*2),d0
                Mem.ww(ab3d2.bss.DrawBss.draw_DisplayAmmoCount_w, gd0);
                Mem.ww(ab3d2.bss.DrawBss.draw_DisplayEnergyCount_w, Mem.w(PlayerBss.Plr1_Health_w));
                loop_FrameClampAndSnapshot1();                // frames + copie Snap→Tmp (J1)
                Plr1_Control(a2);                             // bsr Plr1_Control
                int a0 = Mem.l(PlayerBss.Plr1_ZonePtr_l);
                Mem.wl(Zone_SplitHeight_l, Mem.l(a0 + Defs.ZoneT_Roof_l)); // move.l ZoneT_Roof_l(a0),Zone_SplitHeight_l
                Mem.ww(NewaliencontrolData.THISPLRxoff, Mem.w(PlayerBss.Plr1_TmpXOff_l));
                Mem.ww(NewaliencontrolData.THISPLRzoff, Mem.w(PlayerBss.Plr1_TmpZOff_l));
                Mem.wl(PlayerBss.Plr2_TmpYOff_l, 0x60000);    // move.l #$60000,Plr2_TmpYOff_l
                a0 = Mem.l(PlayerBss.Plr2_ObjectPtr_l);
                ab3d2.Macros.FREE_ENT(a0);                    // FREE_ENT a0
                Mem.wb(a0 + Defs.ObjT_SeePlayer_b, 0);        // move.b #0,ObjT_SeePlayer_b(a0)
                Mem.wl(PlayerBss.Plr2_ZonePtr_l, BollocksRoom); // move.l #BollocksRoom,Plr2_ZonePtr_l
            } else {                                          // NotOnePlayer (master)
                loop_MasterBlock();
            }

            // donetalking: tables de luminosité par zone/point
            int potVis = (mpt == PLR_SLAVE)
                    ? Mem.l(PlayerBss.Plr2_PotVisibleZoneListPtr_l)
                    : Mem.l(PlayerBss.Plr1_PotVisibleZoneListPtr_l);
            computeZoneBrightness(potVis);

            // whythehell: luminosité moyenne de la salle du joueur 1
            int a0 = Mem.l(PlayerBss.Plr1_ZonePtr_l);         // move.l Plr1_ZonePtr_l,a0
            int a1 = CurrentPointBrights_vl;                  // move.l #CurrentPointBrights_vl,a1
            int a2b = Mem.l(LevelBss_Lvl_ZoneBorderPointsPtr_l()); // move.l Lvl_ZoneBorderPointsPtr_l,a2
            int wd0 = Mem.w(a0);                              // move.w (a0),d0
            wd0 = muls(wd0, 10);                              // muls #10,d0
            a2b = a2b + (short) wd0 * 2;                      // lea (a2,d0.w*2),a2
            a1 = a1 + (short) wd0 * 8;                        // lea (a1,d0.w*8),a1
            int wd7 = 9; int sumIdx = 0; int sumBr = 0;       // moveq #9,d7 ; #0,d0 ; #0,d1
            do {                                              // findaverage:
                int v = Mem.w(a2b); a2b += 2;                 // tst.w (a2)+
                if (v < 0) break;                             // blt .foundaverage
                sumIdx += 1;                                  // addq #1,d0
                int br = Mem.w(a1); a1 += 2;                  // move.w (a1)+,d2
                if (br < 0) br = -(short) br;                 // bge .okpos ; neg.w d2
                sumBr = setw(sumBr, sumBr + br);              // add.w d2,d1
            } while (--wd7 != -1);                            // dbra d7,findaverage
            // .foundaverage:
            int avg = sumBr;                                  // ext.l d1
            avg = avg / (sumIdx == 0 ? 1 : sumIdx);           // divs d0,d1 (d0 = nb pts)
            avg = setw(avg, avg - 300);                       // sub.w #300,d1
            Mem.ww(PlayerBss.Plr1_RoomBright_w, avg);         // move.w d1,Plr1_RoomBright_w

            if (mpt != PLR_SINGLE) {                          // cmp PLR_SINGLE ; beq nosee
                Mem.wl(ObjectmoveData.Obj_FromZonePtr_l, Mem.l(PlayerBss.Plr1_ZonePtr_l));
                Mem.wl(ObjectmoveData.Obj_ToZonePtr_l, Mem.l(PlayerBss.Plr2_ZonePtr_l));
                Mem.ww(ObjectmoveData.Viewerx, Mem.w(PlayerBss.Plr1_TmpXOff_l));
                Mem.ww(ObjectmoveData.Viewerz, Mem.w(PlayerBss.Plr1_TmpZOff_l));
                Mem.ww(ObjectmoveData.Viewery, Mem.l(PlayerBss.Plr1_TmpYOff_l) >> 7); // asr.l #7
                Mem.ww(ObjectmoveData.Targetx, Mem.w(PlayerBss.Plr2_TmpXOff_l));
                Mem.ww(ObjectmoveData.Targetz, Mem.w(PlayerBss.Plr2_TmpZOff_l));
                Mem.ww(ObjectmoveData.Targety, Mem.l(PlayerBss.Plr2_TmpYOff_l) >> 7);
                Mem.wb(ObjectmoveData.ViewerTop, Mem.b(PlayerBss.Plr1_StoodInTop_b));
                Mem.wb(ObjectmoveData.TargetTop, Mem.b(PlayerBss.Plr2_StoodInTop_b));
                Objectmove.CanItBeSeen();                     // jsr CanItBeSeen
                int o1 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);
                Mem.wb(o1 + Defs.ObjT_SeePlayer_b, Mem.b(ObjectmoveData.CanSee) & 2); // and.b #2
                int o2 = Mem.l(PlayerBss.Plr2_ObjectPtr_l);
                Mem.wb(o2 + Defs.ObjT_SeePlayer_b, Mem.b(ObjectmoveData.CanSee) & 1); // and.b #1
            }
            // nosee: timers de maintien du tir + XDiff/ZDiff
            loop_HoldAndDiffs();

            // Plr_Use selon le joueur
            if (mpt == PLR_SLAVE) Plr2_Use(); else Plr1_Use(); // bsr Plr1_Use / Plr2_Use

            // IWasPlayer1 / drawplayer2 : préparation de la vue + DrawDisplay
            if (mpt == PLR_SLAVE) {
                loop_DrawPlayer2();
            } else {
                loop_DrawPlayer1();
            }

            // nodrawp2:
            if (Mem.b(draw_RenderMap_b) != 0) {              // tst.b draw_RenderMap_b ; beq .nomap
                ab3d2.modules.draw.DrawMap.DoTheMapWotNastyCharlesIsForcingMeToDo();
            }
            int d5 = (Mem.b(plr1_Teleported_b) | Mem.b(plr2_Teleported_b)) & 0xFF; // or.b
            Mem.wb(ab3d2.modules.C2pData.C2P_Teleporting_b, d5);
            Mem.wb(ab3d2.modules.C2pData.C2P_NeedsInit_b, Mem.b(ab3d2.modules.C2pData.C2P_NeedsInit_b) | d5);
            Mem.wb(plr1_Teleported_b, 0);                     // clr.b plr1_Teleported_b (x2 dans l'original — quirk)
            Mem.wb(plr1_Teleported_b, 0);
            ab3d2.modules.Draw.Draw_Crosshair();              // jsr Draw_Crosshair
            ab3d2.c.SystemC.Sys_EvalFPS();                    // CALLC Sys_EvalFPS
            DevInst.Dev_MarkDrawDone();                       // CALLDEV MarkDrawDone
            DevInst.Dev_DrawGraph();                          // CALLDEV DrawGraph
            if (Mem.b(ab3d2.data.VidData.Vid_UpdatePalette_b) == 0) { // tst.b ; bne .no_palette_update
                ab3d2.c.ScreenC.Vid_LoadMainPalette();        // CALLC Vid_LoadMainPalette
            }
            if (Mem.l(ab3d2.bss.GameBss.Game_ProgressSignal_l) != 0) { // tst.l ; beq .no_update_progress
                ab3d2.c.GameProgress.Game_UpdatePlayerProgress();
            }
            ab3d2.c.ScreenC.Vid_Present();                    // CALLC Vid_Present

            // --- touches de taille d'écran (NUM-/NUM+/F9/F8) ---
            loop_ScreenSizeKeys();

            // --- marque les zones PVS visibles + worry des aliens vus ---
            markVisibleAndWorry();
            a2 = ab3d2.bss.AiBss.AI_AlienTeamWorkspace_vl;    // a2 = AI_AlienTeamWorkspace_vl (pour Plr_Control suivant)

            // --- ESC : quitter ---
            if (Mem.b(KeyMap_vb + RawKeyMacros.RAWKEY_ESC) != 0) { // beq noend
                if (mpt == PLR_SLAVE) Mem.wb(Game_SlaveQuit_b, 0xFF);
                else Mem.wb(Game_MasterQuit_b, 0xFF);
            }
            // noend:
            if (Mem.b(Game_MasterQuit_b) != 0 && Mem.b(Game_SlaveQuit_b) != 0) { // both quit
                endnomusic();
                return;
            }
            if (mpt == PLR_SINGLE) {                           // exit zone
                int zp = Mem.l(PlayerBss.Plr1_ZonePtr_l);
                if (Mem.w(zp) == Mem.w(Lvl_ExitZoneID_w)) {    // cmp.w Lvl_ExitZoneID_w,d0 ; beq
                    // Dématérialisation de fin de niveau (mécanique TELVAL) : zone +2/frame,
                    // présentation -1/frame (net +1) → l'écran se brouille de plus en plus ;
                    // à >=9 → endlevel (musique welldone). Cf. ScreenC.applyTeleportShimmer.
                    int tv = Mem.uw(ab3d2.modules.C2pData.Game_TeleportFrame_w) + 2; // add.w #2,TELVAL
                    Mem.ww(ab3d2.modules.C2pData.Game_TeleportFrame_w, tv);
                    if (tv >= 9) {                             // cmp.w #9 ; bge end
                        endlevel();
                        return;
                    }
                    // sinon : continuer la boucle (le shimmer se rend, -1 à la présentation)
                }
            }
            // noexit:
            if (!((short) Mem.w(PlayerBss.Plr1_Health_w) > 0)) { endlevel(); return; } // tst Plr1_Health bgt
            if (!((short) Mem.w(PlayerBss.Plr2_Health_w) > 0)) { endlevel(); return; }
            // bra game_main_loop (boucle)
        }
    }

    // ===== helpers de game_main_loop =====

    private static int SystemBss_Sys_FPSLimit_w() { return ab3d2.bss.SystemBss.Sys_FPSLimit_w; }
    private static int LevelBss_Lvl_ZoneBorderPointsPtr_l() { return ab3d2.bss.LevelBss.Lvl_ZoneBorderPointsPtr_l; }

    /** WaitTOF (graphics.library) : attend le top-of-frame. Host : avance le compteur VBL. */
    private static void WaitTOF() {
        Mem.wl(Vid_VBLCount_l, Mem.l(Vid_VBLCount_l) + 1);   // placeholder cadence (vsync réel via Vid_Present)
    }

    /** frames clamp + copie Plr1_Snap*→Tmp* (hires.s 1151-1170, chemin solo). */
    private static void loop_FrameClampAndSnapshot1() {
        Mem.ww(Anim_TempFrames_w, Mem.w(ab3d2.bss.AnimBss.Anim_FramesToDraw_w)); // move.w FramesToDraw,TempFrames
        if ((short) Mem.w(Anim_TempFrames_w) >= 15) Mem.ww(Anim_TempFrames_w, 15); // cmp #15 ; blt .okframe ; #15
        Mem.ww(ab3d2.bss.AnimBss.Anim_FramesToDraw_w, 0);    // move.w #0,Anim_FramesToDraw_w
        Mem.wl(PlayerBss.Plr1_TmpXOff_l, Mem.l(PlayerBss.Plr1_SnapXOff_l));
        Mem.wl(PlayerBss.Plr1_TmpZOff_l, Mem.l(PlayerBss.Plr1_SnapZOff_l));
        Mem.wl(PlayerBss.Plr1_TmpYOff_l, Mem.l(PlayerBss.Plr1_SnapYOff_l));
        Mem.wl(PlayerBss.plr1_TmpHeight_l, Mem.l(PlayerBss.Plr1_SnapHeight_l));
        Mem.ww(PlayerBss.Plr1_TmpAngPos_w, Mem.w(PlayerBss.Plr1_SnapAngPos_w));
        Mem.ww(PlayerBss.plr1_TmpBobble_w, Mem.w(PlayerBss.Plr1_Bobble_w));
        Mem.wb(PlayerBss.Plr1_TmpClicked_b, Mem.b(PlayerBss.Plr1_Clicked_b));
        Mem.wb(PlayerBss.Plr1_TmpFire_b, Mem.b(PlayerBss.Plr1_Fire_b));
        Mem.wb(PlayerBss.Plr1_Clicked_b, 0);                 // clr.b Plr1_Clicked_b
        Mem.wb(PlayerBss.Plr1_TmpSpcTap_b, Mem.b(PlayerBss.Plr1_Used_b));
        Mem.wb(PlayerBss.Plr1_Used_b, 0);                    // clr.b Plr1_Used_b
        Mem.wb(PlayerBss.plr1_TmpDucked_b, Mem.b(PlayerBss.Plr1_Ducked_b));
        Mem.wb(PlayerBss.Plr1_TmpGunSelected_b, Mem.b(PlayerBss.Plr1_GunSelected_b));
    }

    /** timers de maintien du tir P1/P2 + XDiff_w/ZDiff_w (hires.s 1606-1665). */
    private static void loop_HoldAndDiffs() {
        int d0 = Mem.w(Anim_TempFrames_w);                   // move.w Anim_TempFrames_w,d0
        Mem.ww(PlayerBss.plr1_TmpHoldDown_w, Mem.w(PlayerBss.plr1_TmpHoldDown_w) + d0); // add.w d0,...
        if ((short) Mem.w(PlayerBss.plr1_TmpHoldDown_w) >= 30) Mem.ww(PlayerBss.plr1_TmpHoldDown_w, 30); // clamp 30
        if (Mem.b(PlayerBss.Plr1_TmpFire_b) == 0) {          // tst.b Plr1_TmpFire_b ; bne okstillheld
            int v = (short) (Mem.w(PlayerBss.plr1_TmpHoldDown_w) - d0); // sub.w d0
            if (v < 0) v = 0;                                // bge okstillheld ; #0
            Mem.ww(PlayerBss.plr1_TmpHoldDown_w, v);
        }
        // okstillheld:
        Mem.ww(PlayerBss.plr2_TmpHoldDown_w, Mem.w(PlayerBss.plr2_TmpHoldDown_w) + d0);
        if ((short) Mem.w(PlayerBss.plr2_TmpHoldDown_w) >= 30) Mem.ww(PlayerBss.plr2_TmpHoldDown_w, 30);
        if (Mem.b(PlayerBss.Plr2_TmpFire_b) == 0) {
            int v = (short) (Mem.w(PlayerBss.plr2_TmpHoldDown_w) - d0);
            if (v < 0) v = 0;
            Mem.ww(PlayerBss.plr2_TmpHoldDown_w, v);
        }
        // okstillheld2:
        int d1 = Mem.w(Anim_TempFrames_w);                   // move.w Anim_TempFrames_w,d1
        if (!((short) d1 > 0)) d1 = 1;                       // bgt noze ; moveq #1,d1
        // XDiff/ZDiff = (XOff-OldX)<<4 / d1
        int dd = (short) (Mem.w(PlayerBss.Plr1_XOff_l) - Mem.w(plr1_OldX_l)); // sub.w
        dd = setw(dd, dd << 4); dd = (short) dd; dd = dd / d1; // asl.w#4 ; ext.l ; divs d1
        Mem.ww(XDiff_w, dd);
        dd = (short) (Mem.w(PlayerBss.Plr2_XOff_l) - Mem.w(plr2_OldX_l));
        dd = setw(dd, dd << 4); dd = (short) dd; dd = dd / d1;  // (résultat d0 mort)
        dd = (short) (Mem.w(PlayerBss.Plr1_ZOff_l) - Mem.w(plr1_OldZ_l));
        dd = setw(dd, dd << 4); dd = (short) dd; dd = dd / d1;
        Mem.ww(ZDiff_w, dd);
        dd = (short) (Mem.w(PlayerBss.Plr2_ZOff_l) - Mem.w(plr2_OldZ_l));
        dd = setw(dd, dd << 4); dd = (short) dd; dd = dd / d1;  // (résultat mort)
    }

    /** Préparation vue J1 + rendu (hires.s 1678-1783). */
    private static void loop_DrawPlayer1() {
        Mem.ww(scaleval, 0);                                 // move.w #0,scaleval
        Mem.wl(Plr_XOff_l, Mem.l(PlayerBss.Plr1_XOff_l));
        Mem.wl(Plr_YOff_l, Mem.l(PlayerBss.Plr1_YOff_l));
        Mem.wl(Plr_ZOff_l, Mem.l(PlayerBss.Plr1_ZOff_l));
        Mem.ww(Vis_AngPos_w, Mem.w(PlayerBss.Plr1_AngPos_w));
        Mem.ww(Vis_CosVal_w, Mem.w(PlayerBss.Plr1_CosVal_w));
        Mem.ww(Vis_SinVal_w, Mem.w(PlayerBss.Plr1_SinVal_w));
        Mem.wl(ab3d2.bss.LevelBss.Lvl_ListOfGraphRoomsPtr_l, Mem.l(PlayerBss.Plr1_PotVisibleZoneListPtr_l));
        Mem.wl(ab3d2.bss.LevelBss.PointsToRotatePtr_l, Mem.l(PlayerBss.plr1_PointsToRotatePtr_l));
        Mem.wb(PLREcho, Mem.b(PlayerBss.Plr1_Echo_b));
        Mem.wl(ZonePtr_l, Mem.l(PlayerBss.Plr1_ZonePtr_l));
        int d5 = Mem.ub(ControlloopData.look_behind_key);    // move.b look_behind_key,d5
        if (Mem.b(KeyMap_vb + d5) != 0) {                    // tst.b (a5,d5.w) ; beq .nolookback
            int a0 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);
            ab3d2.Macros.FREE_OBJ_2(a0, Defs.ENT_NEXT_2);    // arme en main
            Mem.ww(Vis_AngPos_w, Mem.w(Vis_AngPos_w) ^ SINE_SIZE); // eor.w #SINE_SIZE
            Mem.ww(Vis_CosVal_w, -Mem.w(Vis_CosVal_w));      // neg.w
            Mem.ww(Vis_SinVal_w, -Mem.w(Vis_SinVal_w));
        }
        // .nolookback:
        if (Mem.b(ControlloopData.Prefs_ShowWeapon_b) != 0) { // tst.b ; beq .showWeapon ; sinon FREE arme
            ab3d2.Macros.FREE_OBJ_2(Mem.l(PlayerBss.Plr1_ObjectPtr_l), Defs.ENT_NEXT_2);
        }
        // .showWeapon:
        Orderzones.Zone_OrderZones();                        // jsr Zone_OrderZones
        Newanims.objmoveanim();                              // jsr objmoveanim
        Mem.wb(DOANYWATER, 0xFF);                            // st DOANYWATER
        Mem.wl(Plr_YOff_l, Mem.l(PlayerBss.Plr1_YOff_l));    // move.l Plr1_YOff_l,Plr_YOff_l
        loop_SetClips();
        DrawDisplay();                                       // bsr DrawDisplay
    }

    /** Préparation vue J2 + rendu (hires.s 1785-1835). */
    private static void loop_DrawPlayer2() {
        Mem.ww(scaleval, 0);
        Mem.wl(Plr_XOff_l, Mem.l(PlayerBss.Plr2_XOff_l));
        Mem.wl(Plr_YOff_l, Mem.l(PlayerBss.Plr2_YOff_l));
        Mem.wl(Plr_ZOff_l, Mem.l(PlayerBss.Plr2_ZOff_l));
        Mem.ww(Vis_AngPos_w, Mem.w(PlayerBss.Plr2_AngPos_w));
        Mem.ww(Vis_CosVal_w, Mem.w(PlayerBss.Plr2_CosVal_w));
        Mem.ww(Vis_SinVal_w, Mem.w(PlayerBss.Plr2_SinVal_w));
        Mem.wl(ab3d2.bss.LevelBss.Lvl_ListOfGraphRoomsPtr_l, Mem.l(PlayerBss.Plr2_PotVisibleZoneListPtr_l));
        Mem.wl(ab3d2.bss.LevelBss.PointsToRotatePtr_l, Mem.l(PlayerBss.plr2_PointsToRotatePtr_l));
        Mem.wb(PLREcho, Mem.b(PlayerBss.Plr2_Echo_b));
        Mem.wl(ZonePtr_l, Mem.l(PlayerBss.Plr2_ZonePtr_l));
        int d5 = Mem.ub(ControlloopData.look_behind_key);
        if (Mem.b(KeyMap_vb + d5) != 0) {                    // .nolookback
            int a0 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);      // QUIRK : Plr1_ObjectPtr (comme l'original)
            ab3d2.Macros.FREE_OBJ_2(a0, Defs.ENT_NEXT_2);
            Mem.ww(Vis_AngPos_w, Mem.w(Vis_AngPos_w) ^ SINE_SIZE);
            Mem.ww(Vis_CosVal_w, -Mem.w(Vis_CosVal_w));
            Mem.ww(Vis_SinVal_w, -Mem.w(Vis_SinVal_w));
        }
        if (Mem.b(ControlloopData.Prefs_ShowWeapon_b) != 0) {
            ab3d2.Macros.FREE_OBJ_2(Mem.l(PlayerBss.Plr1_ObjectPtr_l), Defs.ENT_NEXT_2);
        }
        Orderzones.Zone_OrderZones();
        Newanims.objmoveanim();
        loop_SetClips();
        Mem.wb(DOANYWATER, 0xFF);                            // st DOANYWATER (après les clips pour J2)
        DrawDisplay();
    }

    /** Réglage des clips de rendu (commun aux deux vues, hires.s 1763-1775 / 1823-1833). */
    private static void loop_SetClips() {
        int d0 = Mem.w(Vid_LetterBoxMarginHeight_w);         // move.w Vid_LetterBoxMarginHeight_w,d0
        Mem.ww(Draw_LeftClip_w, 0);                          // move.w #0,Draw_LeftClip_w
        Mem.ww(Draw_RightClip_w, Mem.w(Vid_RightX_w));       // move.w Vid_RightX_w,Draw_RightClip_w
        Mem.ww(draw_TopClip_w, 0 + d0);                      // move.w #0,draw_TopClip_w ; add.w d0
        Mem.ww(draw_BottomClip_w, (short) (Mem.w(Vid_BottomY_w) - d0)); // Vid_BottomY_w ; sub.w d0
    }

    /** Tables de luminosité par zone et par point (donetalking, hires.s 1428-1545). */
    private static void computeZoneBrightness(int listPtr) {
        int a1 = Zone_BrightTable_vl;                        // move.l #Zone_BrightTable_vl,a1
        int a2 = Mem.l(Lvl_ZonePtrsPtr_l);                   // move.l Lvl_ZonePtrsPtr_l,a2 (table de ptrs)
        int a0 = listPtr;                                    // move.l Plr*_PotVisibleZoneListPtr_l,a0
        int a5 = a0;                                         // move.l a0,a5
        // doallz:
        int d0;
        while ((d0 = Mem.w(a0)) >= 0) {                      // move.w (a0),d0 ; blt doneallz
            a0 += 8;                                         // add.w #8,a0
            int a3 = Mem.l(a2 + (short) d0 * 4);             // move.l (a2,d0.w*4),a3
            int d2 = Mem.w(a3 + Defs.ZoneT_Brightness_w);    // move.w ZoneT_Brightness_w(a3),d2
            if (d2 >= 0) {                                   // blt justbright
                int d3 = (d2 & 0xFFFF) >>> 8;                // move.w d2,d3 ; lsr.w #8,d3
                if ((byte) d3 != 0) {                        // tst.b d3 ; beq justbright
                    d2 = Mem.w(ab3d2.bss.AnimBss.Anim_BrightTable_vw + (short) d3 * 2 - 2); // -2(a4,d3.w*2)
                }
            }
            // justbright:
            d2 = muls(d2, 410); d2 = d2 >> 8;                // muls #410,d2 ; asr.l #8,d2
            Mem.ww(a1 + (short) d0 * 4, d2);                 // move.w d2,(a1,d0.w*4)
            d2 = Mem.w(a3 + Defs.ZoneT_UpperBrightness_w);   // move.w ZoneT_UpperBrightness_w(a3),d2
            if (d2 >= 0) {                                   // blt justbright2
                int d3 = (d2 & 0xFFFF) >>> 8;
                if ((byte) d3 != 0) {
                    d2 = Mem.w(ab3d2.bss.AnimBss.Anim_BrightTable_vw + (short) d3 * 2 - 2);
                }
            }
            // justbright2:
            d2 = muls(d2, 410); d2 = d2 >> 8;
            Mem.ww(a1 + (short) d0 * 4 + 2, d2);             // move.w d2,2(a1,d0.w*4)
        }
        // doneallz:
        int a2p = Mem.l(PointBrightsPtr_l);                  // move.l PointBrightsPtr_l,a2
        int a3 = CurrentPointBrights_vl;                     // move.l #CurrentPointBrights_vl,a3
        // justtheone:
        while ((d0 = Mem.w(a5)) >= 0) {                      // move.w (a5),d0 ; blt whythehell
            a5 += 8;                                         // addq #8,a5
            d0 = muls(d0, 40);                               // muls #40,d0
            int d7 = 39;                                     // move.w #39,d7
            do {                                             // allinzone:
                int d2 = Mem.w(a2p + (short) d0 * 2);        // move.w (a2,d0.w*2),d2
                if (!((byte) d2 < 0)) {                      // tst.b d2 ; blt .justbright
                    int d3 = (d2 & 0xFFFF) >>> 8;            // move.w d2,d3 ; lsr.w #8,d3
                    if ((byte) d3 != 0) {                    // tst.b d3 ; beq .justbright
                        int d4 = d3;                         // move.w d3,d4
                        d3 = d3 & 0xf;                       // and.w #$f,d3
                        d4 = (d4 & 0xFFFF) >>> 4;            // lsr.w #4,d4
                        d4 = d4 + 1;                         // add.w #1,d4
                        d3 = Mem.w(ab3d2.bss.AnimBss.Anim_BrightTable_vw + (short) d3 * 2 - 2); // -2(a0,d3.w*2)
                        d2 = extw(d2);                       // ext.w d2 (étend l'octet faible)
                        d3 = setw(d3, d3 - d2);              // sub.w d2,d3
                        d3 = muls(d3, d4);                   // muls d4,d3
                        d3 = asrw(d3, 4);                    // asr.w #4,d3
                        d2 = setw(d2, d2 + d3);              // add.w d3,d2
                    }
                }
                // .justbright:
                d2 = extw(d2);                               // ext.w d2 (étend l'octet faible)
                d2 = muls(d2, 397); d2 = d2 >> 8;            // muls #397,d2 ; asr.l #8,d2
                if (!((short) d2 >= 0)) d2 = setw(d2, d2 - 600); // bge .itspos ; sub.w #600,d2
                // .itspos:
                d2 = setw(d2, d2 + 300);                     // add.w #300,d2
                Mem.ww(a3 + (short) d0 * 2, d2);             // move.w d2,(a3,d0.w*2)
                d0 = d0 + 1;                                 // addq #1,d0
            } while (--d7 != -1);                            // dbra d7,allinzone
        }
        // whythehell : traité dans game_main_loop
    }

    /** Marque les zones PVS visibles (bitmask) + worry des aliens vus (hires.s 1988-2061). */
    private static void markVisibleAndWorry() {
        int a0 = Mem.l(PlayerBss.Plr2_ZonePtr_l);            // move.l Plr2_ZonePtr_l,a0
        int a1 = ab3d2.bss.SystemBss.Sys_Workspace_vl;       // move.l #Sys_Workspace_vl,a1
        for (int i = 0; i < 32; i += 4) Mem.wl(a1 + i, 0);   // clr.l (a1)..28(a1)

        int d0, d1;
        if (Mem.b(Plr_MultiplayerType_b) != PLR_SINGLE) {    // cmp PLR_SINGLE ; beq plr1only
            a0 = a0 + Defs.ZoneT_PotVisibleZoneList_vw;      // lea ZoneT_PotVisibleZoneList_vw(a0),a0
            while ((d0 = Mem.w(a0)) >= 0) {                  // .doallrooms: move.w (a0),d0 ; blt .allroomsdone
                a0 += Defs.PVST_SizeOf_l;                    // addq #PVST_SizeOf_l,a0
                d1 = d0; d0 = asrw(d0, 3);                   // move.w d0,d1 ; asr.w #3,d0
                int adr = a1 + (short) d0;                   // (a1,d0.w)
                Mem.wb(adr, Mem.ub(adr) | (1 << (d1 & 7)));  // bset d1,(a1,d0.w)
            }
        }
        // plr1only:
        a0 = Mem.l(PlayerBss.Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a0
        a0 = a0 + Defs.ZoneT_PotVisibleZoneList_vw;
        while ((d0 = Mem.w(a0)) >= 0) {                      // .doallrooms2
            a0 += Defs.PVST_SizeOf_l;
            d1 = d0; d0 = asrw(d0, 3);
            int adr = a1 + (short) d0;
            Mem.wb(adr, Mem.ub(adr) | (1 << (d1 & 7)));
        }
        // .allroomsdone2:
        int d7 = 0b000001;                                   // move.l #%000001,d7
        int a2 = ab3d2.bss.AiBss.AI_AlienTeamWorkspace_vl;   // lea AI_AlienTeamWorkspace_vl,a2
        a0 = Mem.l(PlayerBss_Lvl_ObjectDataPtr_l());         // move.l Lvl_ObjectDataPtr_l,a0
        a0 = a0 - Defs.ObjT_SizeOf_l;                        // sub.w #ObjT_SizeOf_l,a0
        while (true) {                                       // .doallobs:
            a0 += Defs.ObjT_SizeOf_l;                        // NEXT_OBJ a0
            d0 = Mem.w(a0);                                  // move.w (a0),d0
            if (d0 < 0) break;                               // blt .allobsdone
            d0 = Mem.w(a0 + Defs.ObjT_ZoneID_w);             // move.w ObjT_ZoneID_w(a0),d0
            if (d0 < 0) continue;                            // blt .doallobs
            d1 = d0; d0 = asrw(d0, 3);                       // move.w d0,d1 ; asr.w #3,d0
            boolean worry = (Mem.ub(a1 + (short) d0) & (1 << (d1 & 7))) != 0; // btst d1,(a1,d0.w)
            if (!worry) {                                    // bne .worryobj
                int typ = Mem.ub(a0 + Defs.ObjT_TypeID_b);   // move.b ObjT_TypeID_b(a0),d0
                if ((d7 & (1 << (typ & 31))) == 0) continue; // btst d0,d7 ; beq .doallobs
                int team = Mem.b(a0 + Defs.EntT_TeamNumber_b); // move.b EntT_TeamNumber_b(a0),d0
                if (team < 0) continue;                      // blt .doallobs
                team = setw(team, team << 4);                // asl.w #4,d0
                if ((short) Mem.w(a2 + (short) team + ab3d2.modules.Ai.AI_WorkT_SeenBy_w) < 0) continue; // tst.w SeenBy ; blt
            }
            // .worryobj:
            Mem.wb(a0 + Defs.ShotT_Worry_b, Mem.b(a0 + Defs.ShotT_Worry_b) | 127); // or.b #127,ShotT_Worry_b(a0)
        }
        // .allobsdone:
    }

    private static int PlayerBss_Lvl_ObjectDataPtr_l() { return ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l; }

    /** Touches de redimensionnement / modes (hires.s 1889-1986). */
    private static void loop_ScreenSizeKeys() {
        int a5 = KeyMap_vb;
        // NUM- : réduit la vue verticale
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_MINUS) != 0) { // beq .nosmallscr
            int d0 = 100;                                    // max plein écran
            if (Mem.b(Vid_FullScreen_b) == 0) d0 = 60;       // tst FullScreen ; bne .isFullscreen ; #60
            // .isFullscreen:
            if (!((short) d0 < Mem.w(Vid_LetterBoxMarginHeight_w))) { // cmp LetterBox,d0 ; blt .clamped
                Mem.ww(Vid_LetterBoxMarginHeight_w, Mem.w(Vid_LetterBoxMarginHeight_w) + 2); // add.w #2
                Mem.wb(ab3d2.modules.C2pData.C2P_NeedsSetParam_b, 0xFF); // st C2P_NeedsSetParam_b
                ab3d2.c.DrawC.Draw_ResetGameDisplay();
                ab3d2.c.ScreenC.vid_SetupDoubleheightCopperlist();
            }
        }
        // .clamped/.nosmallscr: NUM+ : agrandit
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_PLUS) != 0) {  // beq .nobigscr
            if ((short) Mem.w(Vid_LetterBoxMarginHeight_w) > 0) { // tst ; ble .nobigscr
                Mem.ww(Vid_LetterBoxMarginHeight_w, Mem.w(Vid_LetterBoxMarginHeight_w) - 2);
                Mem.wb(ab3d2.modules.C2pData.C2P_NeedsSetParam_b, 0xFF);
                ab3d2.c.ScreenC.vid_SetupDoubleheightCopperlist();
            }
        }
        // .nobigscr: F9 = bascule double hauteur
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_F9) != 0) {        // beq .skip_double_height
            Mem.wb(a5 + RawKeyMacros.RAWKEY_F9, 0);          // clr.b RAWKEY_F9
            if (Mem.b(LASTDH) == 0) {                         // tst.b LASTDH ; bne .not_double_height
                Mem.wb(LASTDH, 0xFF);                        // st LASTDH
                Mem.wb(Vid_DoubleHeight_b, ~Mem.b(Vid_DoubleHeight_b)); // not.b
                Mem.wb(ab3d2.modules.C2pData.C2P_NeedsInit_b, 0xFF);   // st C2P_NeedsInit_b
                SetupRenderbufferSize();
                ab3d2.c.ScreenC.vid_SetupDoubleheightCopperlist();
            }
        } else {
            Mem.wb(LASTDH, 0);                                // .skip_double_height: clr.b LASTDH
        }
        // .not_double_height: F8 = bascule murs simples
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_F8) != 0) {        // beq .skip_double_width
            Mem.wb(a5 + RawKeyMacros.RAWKEY_F8, 0);          // clr.b RAWKEY_F8
            if (Mem.b(LASTDW) == 0) {                         // tst.b LASTDW ; bne .not_double_width
                Mem.wb(ab3d2.bss.DrawBss.Draw_ForceSimpleWalls_b, ~Mem.b(ab3d2.bss.DrawBss.Draw_ForceSimpleWalls_b)); // not.b
            }
        } else {
            Mem.wb(LASTDW, 0);                                // .skip_double_width: clr.b LASTDW
        }
        // .not_double_width:
    }

    /** Bloc joueur master (2 joueurs, série) — hires.s 1190-1313. À finaliser (SENDFIRST host). */
    private static void loop_MasterBlock() {
        throw new UnsupportedOperationException("hires.s NotOnePlayer (master, série SENDFIRST host)");
    }

    /** Bloc joueur slave (2 joueurs, série) — hires.s 1315-1427. À finaliser (RECFIRST host). */
    private static void loop_SlaveBlock() {
        throw new UnsupportedOperationException("hires.s ASlaveShouldWaitOnHisMaster (slave, série RECFIRST host)");
    }

    /** Garde-fou hôte : borne les boucles d'attente musique de fin. Les modules gameover/
     *  quietwelldone SONT présents (HiresData incbin) et se terminent via reachedend (commande
     *  Bxx position-jump, Music.java:612) → la boucle sort normalement ; le plafond n'est qu'un
     *  filet de sécurité si un module bouclait sans Bxx. */
    private static final int MUSIC_END_LOOP_CAP = 5000;

    /**
     * endlevel (hires.s 3404-3495) : fin de niveau. Coupe le son/jeu, lit la santé du
     * joueur ; si morte → musique gameover + STATS_DIED ; sinon → victoire (compteur de
     * niveau++, musique welldone, STATS_WON). Puis closeeverything. La musique de fin
     * (gameover/quietwelldone) joue via mt_init + boucle WaitTOF/mt_music jusqu'à reachedend.
     */
    private static void endlevel() {
        Mem.wb(dosounds, 0);                                 // clr.b dosounds
        Mem.wb(Game_Running_b, 0);                           // clr.b Game_Running_b

        Mem.ww(ab3d2.bss.DrawBss.draw_DisplayEnergyCount_w, Mem.w(Plr1_Health_w)); // move.w Plr1_Health_w,...
        if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) {     // cmp PLR_SLAVE ; bne .notsl
            Mem.ww(ab3d2.bss.DrawBss.draw_DisplayEnergyCount_w, Mem.w(Plr2_Health_w));
        }
        // .notsl:
        if ((short) Mem.w(ab3d2.bss.DrawBss.draw_DisplayEnergyCount_w) > 0) { // bgt wevewon
            // wevewon:
            ab3d2.c.GameProgress.Game_LevelWon();            // STATS_WON
            CustomChips.write16(0xdff000 + 0x096, 0xf);      // move.w #$f,dmacon (audio off)
            if (Mem.b(Plr_MultiplayerType_b) == PLR_SINGLE) { // cmp PLR_SINGLE ; bne .nonextlev
                Mem.ww(ab3d2.ControlloopData.Game_LevelCounter_w, Mem.w(ab3d2.ControlloopData.Game_LevelCounter_w) + 1);
                Mem.wb(ab3d2.ControlloopData.Game_FinishedLevel_b, 0xFF); // st Game_FinishedLevel_b
            }
            // .nonextlev:
            playEndMusic(HiresData.welldone);                // move.l #welldone,mt_data ; mt_init ; playwelldone
            if (Mem.b(Plr_MultiplayerType_b) == PLR_SINGLE   // cmp PLR_SINGLE ; bne wevelost
                && Mem.w(ab3d2.ControlloopData.Game_LevelCounter_w) == Defs.NUM_LEVELS) { // cmp NUM_LEVELS
                Mem.ww(ab3d2.ControlloopData.Game_LevelCounter_w, 0); // clr.w (retour au début)
            }
            // bra wevelost
        } else {
            Mem.ww(ab3d2.bss.DrawBss.draw_DisplayEnergyCount_w, 0); // move.w #0
            ab3d2.c.GameProgress.Game_LevelFailed();         // STATS_DIED
            playEndMusic(HiresData.gameover);                // move.l #gameover,mt_data ; mt_init ; playgameover
            // bra wevelost
        }
        // wevelost:
        CustomChips.write16(0xdff000 + 0x096, 0xf);          // move.w #$f,dmacon (audio off)
        closeeverything();                                   // jmp closeeverything
    }

    /**
     * Joue une musique de fin (welldone / gameover) jusqu'à reachedend.
     *
     * L'ASM original fait {@code mt_init} puis une boucle {@code WaitTOF ; mt_music} : sur Amiga
     * WaitTOF attend le vsync pendant que Paula joue en DMA, et l'IRQ mixe. Ici WaitTOF est un
     * simple compteur → la boucle d'origine s'exécutait instantanément (musique muette + retour
     * menu immédiat). On reconstitue donc une vraie frame hôte : on cadence à ~50 Hz (PAL), on
     * mixe les registres Paula vers OpenAL ({@link #pumpAudioToHost}, car dosounds=0 ici) et on
     * présente l'écran (events + fenêtre). Borné par {@link #MUSIC_END_LOOP_CAP}.
     */
    private static void playEndMusic(int musicData) {
        Mem.wl(ab3d2.modules.Music.mt_data, musicData);      // move.l #music,mt_data
        Mem.wb(UseAllChannels, 0xFF);                        // st UseAllChannels
        Mem.wb(ab3d2.modules.Music.reachedend, 0);           // clr.b reachedend
        ab3d2.modules.Music.mt_init();                       // jsr mt_init
        final long frameNs = 20_000_000L;                    // ~50 Hz (cadence PAL de la musique)
        long next = System.nanoTime() + frameNs;
        for (int g = 0; Mem.b(ab3d2.modules.Music.reachedend) == 0 && g < MUSIC_END_LOOP_CAP; g++) {
            ab3d2.host.Display d = ab3d2.c.ScreenC.hostDisplay();
            if (d != null && d.shouldClose()) {              // fermeture fenêtre pendant la musique
                break;
            }
            pumpAudioToHost();                               // mixe Paula → OpenAL (musique courante)
            ab3d2.c.ScreenC.Vid_Present();                   // présente + pollEvents (le sleep ci-dessous cadence)
            ab3d2.modules.Music.mt_music();                  // jsr mt_music (avance la chanson)
            long sleepNs = next - System.nanoTime();
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                next += frameNs;
            } else {
                next = System.nanoTime() + frameNs;          // retard → resync
            }
        }
    }

    /** endnomusic (hires.s 3497-3501) : fin sans musique (quit 2J). */
    private static void endnomusic() {
        Mem.wb(Game_Running_b, 0);                           // clr.b Game_Running_b
        closeeverything();                                   // jmp closeeverything
    }

    /**
     * closeeverything (hires.s 8205-8267) : arrêt musique + libération des données de
     * niveau et de la mémoire écran. Les retraits d'IntServer/libs sont sans objet en
     * hôte. En RTG on saute le nettoyage du msgport ; on remet juste l'index de buffer.
     */
    private static void closeeverything() {
        ab3d2.host.Audio.stop();                             // hôte : arrêt du backend audio
        ab3d2.modules.Music.mt_end();                        // jsr mt_end
        ab3d2.modules.Res.Res_FreeLevelData();               // jsr Res_FreeLevelData
        ab3d2.modules.Res.Res_ReleaseScreenMemory();         // jsr Res_ReleaseScreenMemory
        if (Mem.w(ab3d2.bss.VidBss.Vid_isRTG) == 0) {        // tst.w _Vid_isRTG ; bne .skipClear
            // DISPLAYMSGPORT_HACK clrMsgPort : sans objet en hôte
            Mem.ww(ab3d2.bss.VidBss.Vid_ScreenBufferIndex_w, 0); // clr.w Vid_ScreenBufferIndex_w
        }
        // .skipClear: rts
    }

    /**
     * SetupRenderbufferSize (hires.s 2134-2167) : ajuste le regard vertical
     * (View_KeyLook/LookMin/LookMax) et STOPOFFSET selon plein écran/petit écran
     * (sauf si LASTDH). Tombe ensuite dans startCopper.
     */
    static void SetupRenderbufferSize() {
        if (Mem.b(LASTDH) != 0) {                            // tst.b LASTDH ; bne .big
            startCopper();
            return;
        }
        if (Mem.b(Vid_FullScreen_b) == 0) {                  // tst.b Vid_FullScreen_b ; beq .small
            // .small
            Mem.ww(View_KeyLook_w, 4);                       // move.w #4,View_KeyLook_w
            int d0 = SMALL_HEIGHT / 2;                       // move.w #SMALL_HEIGHT/2,d0
            Mem.ww(View_LookMin_w, d0);                      // move.w d0,View_LookMin_w
            d0 = -d0;                                        // neg.w d0
            Mem.ww(View_LookMax_w, d0);                      // move.w d0,View_LookMax_w
            d0 = Mem.w(STOPOFFSET);                          // move.w STOPOFFSET,d0
            d0 = muls(d0, 2);                                // muls.w #2,d0
            d0 = divs(d0, 3);                                // divs.w #3,d0 (quotient = mot faible)
            Mem.ww(STOPOFFSET, d0);                          // move.w d0,STOPOFFSET
        } else {
            Mem.ww(View_KeyLook_w, 6);                       // move.w #6,View_KeyLook_w
            int d0 = FS_HEIGHT / 2;                          // move.w #FS_HEIGHT/2,d0
            Mem.ww(View_LookMin_w, d0);                      // move.w d0,View_LookMin_w
            d0 = -d0;                                        // neg.w d0
            Mem.ww(View_LookMax_w, d0);                      // move.w d0,View_LookMax_w
            d0 = Mem.w(STOPOFFSET);                          // move.w STOPOFFSET,d0
            int d1 = d0;                                     // move.w d0,d1
            d1 = asrw(d1, 1);                                // asr.w #1,d1 (STOPOFFSET*0.5)
            d0 = (short) (d0 + d1);                          // add.w d1,d0 (STOPOFFSET*1.5)
            Mem.ww(STOPOFFSET, d0);                          // move.w d0,STOPOFFSET
        }
        // .big → chute dans startCopper
        startCopper();
    }

    /**
     * startCopper (hires.s 2169-2205) : malgré son nom, ne touche PAS au copper
     * dans cette version — fixe les dimensions du renderbuffer (Vid_RightX/CentreX/
     * BottomY/TOTHEMIDDLE) selon plein écran/petit écran et double largeur, puis
     * efface l'écran via Draw_ResetGameDisplay. La copper AGA réelle est dans
     * vid_SetupDoubleheightCopperlist (no-op RTG).
     */
    private static void startCopper() {
        if (Mem.w(Vid_LetterBoxMarginHeight_w) >= 100) {     // cmp.w #100,... ; blt .wideScreenOk
            Mem.ww(Vid_LetterBoxMarginHeight_w, 100);        // move.w #100,Vid_LetterBoxMarginHeight_w
        }
        // .wideScreenOk:
        int d0;
        if (Mem.b(Vid_FullScreen_b) != 0) {                  // tst.b Vid_FullScreen_b ; beq .setupSmallScreen
            d0 = FS_WIDTH;                                   // move.w #FS_WIDTH,d0
            if (Mem.b(Vid_DoubleWidth_b) != 0) {             // tst.b Vid_DoubleWidth_b ; beq .noDoubleWidth
                d0 = (d0 & 0xFFFF) >>> 1;                    // lsr.w #1,d0
            }
            Mem.ww(Vid_RightX_w, d0);                        // move.w d0,Vid_RightX_w
            d0 = (d0 & 0xFFFF) >>> 1;                        // lsr.w #1,d0
            Mem.ww(Vid_CentreX_w, d0);                       // move.w d0,Vid_CentreX_w
            Mem.ww(Vid_BottomY_w, FS_HEIGHT);                // move.w #FS_HEIGHT,Vid_BottomY_w
            Mem.ww(TOTHEMIDDLE, FS_HEIGHT / 2);              // move.w #FS_HEIGHT/2,TOTHEMIDDLE
        } else {
            // .setupSmallScreen:
            d0 = SMALL_WIDTH;                                // move.w #SMALL_WIDTH,d0
            if (Mem.b(Vid_DoubleWidth_b) != 0) {             // tst.b Vid_DoubleWidth_b ; beq .noDoubleWidth2
                d0 = (d0 & 0xFFFF) >>> 1;                    // lsr.w #1,d0
            }
            Mem.ww(Vid_RightX_w, d0);                        // move.w d0,Vid_RightX_w
            d0 = (d0 & 0xFFFF) >>> 1;                        // lsr.w #1,d0
            Mem.ww(Vid_CentreX_w, d0);                       // move.w d0,Vid_CentreX_w
            Mem.ww(Vid_BottomY_w, SMALL_HEIGHT);             // move.w #SMALL_HEIGHT,Vid_BottomY_w
            Mem.ww(TOTHEMIDDLE, SMALL_HEIGHT / 2);           // move.w #SMALL_HEIGHT/2,TOTHEMIDDLE
        }
        // .wipeScreen:
        ab3d2.c.DrawC.Draw_ResetGameDisplay();               // CALLC Draw_ResetGameDisplay
    }

    /**
     * DrawDisplay (hires.s 3211-3363) : rend une vue complète.
     *
     * Calcule sin/cos de l'angle de vue, les offsets de mur/sol/×0.75, fait tourner
     * les points du niveau et des objets (RotateLevelPts/RotateObjectPts), calcule
     * les objets en ligne de mire (CalcPLR1InLine ; CalcPLR2InLine en 2 joueurs),
     * dessine la scène (Draw_Zone_Graph), décompte les frames d'arme, et applique
     * éventuellement la teinte d'eau plein écran (DOANYWATER + fillscrnwater).
     *
     * Chemin SOLO fidèle. Branches 2 joueurs (CalcPLR2InLine, gun esclave) traduites
     * mais non empruntées en solo ; voir QUIRKs a6/d2 (registres hérités, sans effet solo).
     */
    static void DrawDisplay() {
        Mem.wb(fillscrnwater, 0);                            // clr.b fillscrnwater

        int a0 = SinCosTable_vw;                             // move.l #SinCosTable_vw,a0
        int d0 = Mem.w(Vis_AngPos_w);                        // move.w Vis_AngPos_w,d0
        int d6 = Mem.w(a0 + d0);                             // move.w (a0,d0.w),d6
        a0 = a0 + (short) COSINE_OFS;                        // adda.w #COSINE_OFS,a0 (+90°)
        int d7 = Mem.w(a0 + d0);                             // move.w (a0,d0.w),d7
        Mem.ww(Vis_SinVal_w, d6);                            // move.w d6,Vis_SinVal_w
        Mem.ww(Vis_CosVal_w, d7);                            // move.w d7,Vis_CosVal_w

        d0 = Mem.l(Plr_YOff_l) >> 8;                         // move.l Plr_YOff_l,d0 ; asr.l #8,d0
        int d1 = d0;                                         // move.w d0,d1
        d1 = (d1 + (256 - 32)) & 255;                        // add.w #224,d1 ; and.w #255,d1
        Mem.ww(draw_WallYOffset_w, d1);                      // move.w d1,draw_WallYOffset_w

        d0 = Mem.l(Plr_YOff_l) >> 6;                         // move.l Plr_YOff_l,d0 ; asr.l #6,d0
        Mem.ww(flooryoff, d0);                               // move.w d0,flooryoff

        d6 = Mem.w(Plr_XOff_l);                              // move.w Plr_XOff_l,d6
        int d3 = asrw(d6, 1);                                // move.w d6,d3 ; asr.w #1,d3
        d6 = (short) (d6 + d3);                              // add.w d3,d6 (×1.5)
        d6 = asrw(d6, 1);                                    // asr.w #1,d6 (×0.75)
        Mem.ww(xoff34, d6);                                  // move.w d6,xoff34

        d6 = Mem.w(Plr_ZOff_l);                              // move.w Plr_ZOff_l,d6
        d3 = asrw(d6, 1);                                    // move.w d6,d3 ; asr.w #1,d3
        d6 = (short) (d6 + d3);                              // add.w d3,d6
        d6 = asrw(d6, 1);                                    // asr.w #1,d6
        Mem.ww(zoff34, d6);                                  // move.w d6,zoff34

        ab3d2.modules.Transform.RotateLevelPts();            // bsr RotateLevelPts
        ab3d2.modules.Transform.RotateObjectPts();           // bsr RotateObjectPts
        ab3d2.modules.Transform.CalcPLR1InLine();            // bsr CalcPLR1InLine

        if (Mem.b(Plr_MultiplayerType_b) == PLR_SINGLE) {    // cmp #PLR_SINGLE ; bne doplr2too
            a0 = Mem.l(Plr2_ObjectPtr_l);                    // move.l Plr2_ObjectPtr_l,a0
            ab3d2.Macros.FREE_ENT(a0);                       // FREE_ENT a0
            // bra noplr2either
        } else {                                             // doplr2too:
            // QUIRK 2J : CalcPLR2InLine lit (a6) = registre hérité de CalcPLR1InLine.
            // Jamais emprunté en solo ; a6 à câbler pour le 2 joueurs.
            ab3d2.modules.Transform.CalcPLR2InLine(0);       // bsr CalcPLR2InLine
        }
        // noplr2either:
        ab3d2.modules.draw.DrawZoneGraph.Draw_Zone_Graph();  // bsr Draw_Zone_Graph

        if (Mem.b(DONTDOGUN) == 0) {                         // tst.b DONTDOGUN ; bne NOGUNLOOK
            if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) { // cmp #PLR_SLAVE ; beq drawslavegun
                d0 = Mem.ub(Plr2_GunSelected_b);             // (sélection arme — registres morts)
                d1 = Mem.ub(Plr2_GunFrame_w);
            } else {
                d0 = Mem.ub(Plr1_GunSelected_b);
                d1 = Mem.ub(Plr1_GunFrame_w);
            }
            // drawngun: (d0/d1 du select sont morts : NOGUNLOOK réécrit d1)
        }
        // NOGUNLOOK: décompte des frames d'arme P1
        d1 = Mem.ub(Plr1_GunFrame_w);                        // moveq#0,d1 ; move.b Plr1_GunFrame_w,d1
        d1 = (short) (d1 - Mem.w(Anim_TempFrames_w));        // sub.w Anim_TempFrames_w,d1
        if (!(d1 > 0)) {                                     // bgt .nn
            d1 = 0;                                          // moveq #0,d1
        }
        Mem.wb(Plr1_GunFrame_w, d1);                         // .nn: move.b d1,Plr1_GunFrame_w
        if ((byte) d1 > 0) {                                 // ble .donefire ; sinon sub.b #1
            Mem.wb(Plr1_GunFrame_w, Mem.b(Plr1_GunFrame_w) - 1);
        }
        // .donefire:

        d1 = Mem.ub(Plr2_GunFrame_w);                        // moveq#0,d1 ; move.b Plr2_GunFrame_w,d1
        d1 = (short) (d1 - Mem.w(Anim_TempFrames_w));        // sub.w Anim_TempFrames_w,d1
        if (!(d1 > 0)) {                                     // bgt .nn2
            d1 = 0;                                          // moveq #0,d1
        }
        // QUIRK ORIGINAL : move.b d2 (et non d1) → Plr2_GunFrame_w. d2 = registre hérité
        // indéterminé, sans effet en solo (Plr2 libéré). Modélisé d2 = 0.
        int d2 = 0;
        Mem.wb(Plr2_GunFrame_w, d2);                         // move.b d2,Plr2_GunFrame_w
        if ((byte) d2 > 0) {                                 // ble .donefire2 ; sinon sub.b #1
            Mem.wb(Plr2_GunFrame_w, Mem.b(Plr2_GunFrame_w) - 1);
        }
        // .donefire2:

        if (Mem.b(DOANYWATER) == 0) {                        // tst.b DOANYWATER ; beq nowaterfull
            nowaterfull();
            return;
        }
        d0 = FS_HEIGHT - 1;                                  // move.w #FS_HEIGHT-1,d0
        a0 = Mem.l(Vid_FastBufferPtr_l);                     // move.l Vid_FastBufferPtr_l,a0
        int fsw = Mem.b(fillscrnwater);                      // tst.b fillscrnwater
        if (fsw == 0) {                                      // beq nowaterfull
            nowaterfull();
            return;
        }
        if (!(fsw > 0)) {                                    // bgt oknothalf ; sinon (fsw<0) moitié basse
            d0 = FS_HEIGHT / 2 - 1;                          // moveq #FS_HEIGHT/2-1,d0
            a0 = a0 + SCREEN_WIDTH * FS_HEIGHT / 2;          // add.l #SCREEN_WIDTH*FS_HEIGHT/2,a0
        }
        // oknothalf:
        CustomChips.ciaAnd(~2);                              // bclr.b #1,$bfe001
        int a2 = Mem.l(Draw_TexturePalettePtr_l) + 256 * 40;// move.l Draw_TexturePalettePtr_l,a2 ; add #256*40
        int d2w;                                             // (d2 réutilisé comme index de teinte)
        if (Mem.b(Vid_FullScreen_b) != 0) {                  // tst.b Vid_FullScreen_b ; bne DOALLSCREEN
            // DOALLSCREEN:
            do {                                             // fw:
                d1 = FS_WIDTH - 1;                           // move.w #FS_WIDTH-1,d1
                do {                                         // fwa:
                    d2w = Mem.ub(a0);                        // move.b (a0),d2
                    Mem.wb(a0, Mem.ub(a2 + d2w)); a0++;      // move.b (a2,d2.w),(a0)+
                } while (--d1 != -1);                        // dbra d1,fwa
                a0 = a0 + (SCREEN_WIDTH - FS_WIDTH);         // add.w #(SCREEN_WIDTH-FS_WIDTH),a0
            } while (--d0 != -1);                            // dbra d0,fw
            return;                                          // rts
        }
        // DOSOMESCREEN:
        d0 = SMALL_HEIGHT - 1;                               // move.w #SMALL_HEIGHT-1,d0
        do {                                                 // .fw:
            d1 = SMALL_WIDTH - 1;                            // move.w #SMALL_WIDTH-1,d1
            do {                                             // .fwa:
                d2w = Mem.ub(a0);                            // move.b (a0),d2
                Mem.wb(a0, Mem.ub(a2 + d2w)); a0++;          // move.b (a2,d2.w),(a0)+
            } while (--d1 != -1);                            // dbra d1,.fwa
            a0 = a0 + (SCREEN_WIDTH - SMALL_WIDTH);          // add.w #(SCREEN_WIDTH-SMALL_WIDTH),a0
        } while (--d0 != -1);                                // dbra d0,.fw
        // rts
    }

    /** nowaterfull (hires.s 3361) : pas d'effet d'eau plein écran → restaure le filtre. */
    private static void nowaterfull() {
        CustomChips.ciaBset(1);                              // bset.b #1,$bfe001
    }

    /**
     * Plr1_Control (hires.s 2817-3009) : APPLIQUE le mouvement du joueur 1 sur la
     * frame (ce n'est PAS la lecture d'entrée souris/clavier, qui est dans les
     * wrappers Plr1control et tournait dans la VBL).
     *
     * Prend un instantané (Tmp→actuel), calcule sin/cos d'angle et le bobble,
     * gère le téléport (ZoneT_Tel*), la collision (Obj_DoCollision), le déplacement
     * (MoveObject), puis met en place SnapTYOff/PointsToRotate/Echo/PotVisibleZone
     * et le backdrop. a2 = registre hérité passé à Obj_DoCollision (cf. quirk).
     */
    static void Plr1_Control(int a2) {
        // Take a snapshot of everything.
        int d2 = Mem.l(PlayerBss.Plr1_XOff_l);               // move.l Plr1_XOff_l,d2
        Mem.wl(ObjectmoveData.oldx, d2);                     // move.l d2,oldx
        int d3 = Mem.l(PlayerBss.Plr1_ZOff_l);               // move.l Plr1_ZOff_l,d3
        Mem.wl(ObjectmoveData.oldz, d3);                     // move.l d3,oldz
        int d0 = Mem.l(PlayerBss.Plr1_TmpXOff_l);            // move.l Plr1_TmpXOff_l,d0
        Mem.wl(PlayerBss.Plr1_XOff_l, d0);                   // move.l d0,Plr1_XOff_l
        Mem.wl(ObjectmoveData.newx, d0);                     // move.l d0,newx
        int d1 = Mem.l(PlayerBss.Plr1_TmpZOff_l);            // move.l Plr1_TmpZOff_l,d1
        Mem.wl(ObjectmoveData.newz, d1);                     // move.l d1,newz
        Mem.wl(PlayerBss.Plr1_ZOff_l, d1);                   // move.l d1,Plr1_ZOff_l
        Mem.wl(PlayerBss.Plr1_Height_l, Mem.l(PlayerBss.plr1_TmpHeight_l)); // move.l plr1_TmpHeight_l,Plr1_Height_l
        d0 = d0 - d2;                                        // sub.l d2,d0
        d1 = d1 - d3;                                        // sub.l d3,d1
        Mem.wl(ObjectmoveData.xdiff, d0);                   // move.l d0,xdiff
        Mem.wl(ObjectmoveData.zdiff, d1);                   // move.l d1,zdiff
        d0 = Mem.w(PlayerBss.Plr1_TmpAngPos_w);              // move.w Plr1_TmpAngPos_w,d0
        Mem.ww(PlayerBss.Plr1_AngPos_w, d0);                // move.w d0,Plr1_AngPos_w
        int a1 = SinCosTable_vw;                             // move.l #SinCosTable_vw,a1
        Mem.ww(PlayerBss.Plr1_SinVal_w, Mem.w(a1 + d0));     // move.w (a1,d0.w),Plr1_SinVal_w
        d0 = setw(d0, d0 + COSINE_OFS);                      // add.w #COSINE_OFS,d0
        d0 = ab3d2.data.TablesData.AMOD_A(d0);              // AMOD_A d0
        Mem.ww(PlayerBss.Plr1_CosVal_w, Mem.w(a1 + (d0 & 0xFFFF))); // move.w (a1,d0.w),Plr1_CosVal_w
        d0 = Mem.l(PlayerBss.Plr1_TmpYOff_l);                // move.l Plr1_TmpYOff_l,d0
        d1 = Mem.w(PlayerBss.plr1_TmpBobble_w);              // move.w plr1_TmpBobble_w,d1
        d1 = Mem.w(a1 + d1);                                 // move.w (a1,d1.w),d1
        d3 = d1;                                             // move.w d1,d3
        if (!((short) d1 <= 0)) {                            // ble.s .notnegative
            d1 = setw(d1, -(short) d1);                      // neg.w d1
        }
        // .notnegative:
        d1 = setw(d1, (short) d1 + 16384);                  // add.w #16384,d1
        d1 = asrw(d1, 4);                                    // asr.w #4,d1
        if (Mem.b(PlayerBss.Plr1_Ducked_b) == 0              // tst.b Plr1_Ducked_b ; bne .notdouble
                && Mem.b(PlayerBss.Plr1_Squished_b) == 0) {  // tst.b Plr1_Squished_b ; bne .notdouble
            d1 = setw(d1, (short) d1 + (short) d1);          // add.w d1,d1
        }
        // .notdouble:
        d1 = (short) d1;                                     // ext.l d1
        Mem.wl(PlayerBss.plr1_BobbleY_l, d1);               // move.l d1,plr1_BobbleY_l
        int d4 = Mem.l(PlayerBss.Plr1_Height_l);            // move.l Plr1_Height_l,d4
        d4 = d4 - d1;                                        // sub.l d1,d4
        d0 = d0 + d1;                                        // add.l d1,d0
        if (Mem.b(Plr_MultiplayerType_b) != PLR_SLAVE) {     // cmp #PLR_SLAVE ; beq .otherwob
            d3 = asrw(d3, 6);                                // asr.w #6,d3
            d3 = (short) d3;                                 // ext.l d3
            Mem.wl(xwobble, d3);                            // move.l d3,xwobble
            d1 = muls(Mem.w(PlayerBss.Plr1_SinVal_w), d3);   // move.w Plr1_SinVal_w,d1 ; muls d3,d1
            d2 = muls(Mem.w(PlayerBss.Plr1_CosVal_w), d3);   // move.w Plr1_CosVal_w,d2 ; muls d3,d2
            d1 = swap(d1);                                   // swap d1
            d2 = swap(d2);                                   // swap d2
            d1 = asrw(d1, 6);                                // asr.w #6,d1
            Mem.ww(xwobxoff, d1);                           // move.w d1,xwobxoff
            d2 = asrw(d2, 6);                                // asr.w #6,d2
            d2 = setw(d2, -(short) d2);                      // neg.w d2
            Mem.ww(xwobzoff, d2);                           // move.w d2,xwobzoff
        }
        // .otherwob:
        Mem.wl(PlayerBss.Plr1_YOff_l, d0);                  // move.l d0,Plr1_YOff_l
        Mem.wl(ObjectmoveData.newy, d0);                    // move.l d0,newy
        Mem.wl(ObjectmoveData.oldy, d0);                    // move.l d0,oldy
        Mem.wl(ObjectmoveData.thingheight, d4);             // move.l d4,thingheight
        Mem.wl(ObjectmoveData.StepUpVal, 40 * 256);         // move.l #40*256,StepUpVal
        if (Mem.b(PlayerBss.Plr1_Squished_b) != 0           // tst Squished bne .smallstep
                || Mem.b(PlayerBss.Plr1_Ducked_b) != 0) {    // tst Ducked beq .okbigstep ; sinon .smallstep
            Mem.wl(ObjectmoveData.StepUpVal, 10 * 256);     // .smallstep: move.l #10*256,StepUpVal
        }
        // .okbigstep:
        Mem.wl(ObjectmoveData.StepDownVal, 0x1000000);      // move.l #$1000000,StepDownVal
        int a0 = Mem.l(PlayerBss.Plr1_ZonePtr_l);           // move.l Plr1_ZonePtr_l,a0
        d0 = Mem.w(a0 + Defs.ZoneT_TelZone_w);              // move.w ZoneT_TelZone_w(a0),d0
        boolean cantmove = false;
        if (d0 >= 0) {                                       // blt .noteleport
            Mem.ww(ObjectmoveData.newx, Mem.w(a0 + Defs.ZoneT_TelX_w)); // move.w ZoneT_TelX_w(a0),newx
            Mem.ww(ObjectmoveData.newz, Mem.w(a0 + Defs.ZoneT_TelZ_w)); // move.w ZoneT_TelZ_w(a0),newz
            a0 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);         // move.l Plr1_ObjectPtr_l,a0
            Mem.ww(CollId, Mem.w(a0));                      // move.w (a0),CollId
            Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b111111111111111111); // move.l #...,Obj_CollideFlags_l
            Objectmove.Obj_DoCollision(a2);                 // jsr Obj_DoCollision
            if (Mem.b(ObjectmoveData.hitwall) != 0) {       // tst.b hitwall ; beq .teleport
                Mem.ww(ObjectmoveData.newx, Mem.w(PlayerBss.Plr1_XOff_l)); // move.w Plr1_XOff_l,newx
                Mem.ww(ObjectmoveData.newz, Mem.w(PlayerBss.Plr1_ZOff_l)); // move.w Plr1_ZOff_l,newz
                // bra .noteleport
            } else {
                // .teleport:
                Mem.wb(PlayerBss.plr1_Teleported_b, 0xFF);  // st plr1_Teleported_b
                a0 = Mem.l(PlayerBss.Plr1_ZonePtr_l);       // move.l Plr1_ZonePtr_l,a0
                d0 = Mem.w(a0 + Defs.ZoneT_TelZone_w);      // move.w ZoneT_TelZone_w(a0),d0
                Mem.ww(PlayerBss.Plr1_XOff_l, Mem.w(a0 + Defs.ZoneT_TelX_w)); // move.w ZoneT_TelX_w(a0),Plr1_XOff_l
                Mem.ww(PlayerBss.Plr1_ZOff_l, Mem.w(a0 + Defs.ZoneT_TelZ_w)); // move.w ZoneT_TelZ_w(a0),Plr1_ZOff_l
                d1 = Mem.l(PlayerBss.Plr1_YOff_l);          // move.l Plr1_YOff_l,d1
                d1 = d1 - Mem.l(a0 + Defs.ZoneT_Floor_l);   // sub.l ZoneT_Floor_l(a0),d1
                a0 = Mem.l(Lvl_ZonePtrsPtr_l);              // move.l Lvl_ZonePtrsPtr_l,a0
                a0 = Mem.l(a0 + (short) d0 * 4);            // move.l (a0,d0.w*4),a0
                Mem.wl(PlayerBss.Plr1_ZonePtr_l, a0);       // move.l a0,Plr1_ZonePtr_l
                d1 = d1 + Mem.l(a0 + Defs.ZoneT_Floor_l);   // add.l ZoneT_Floor_l(a0),d1
                Mem.wl(PlayerBss.Plr1_SnapYOff_l, d1);      // move.l d1,Plr1_SnapYOff_l
                Mem.wl(PlayerBss.Plr1_YOff_l, d1);          // move.l d1,Plr1_YOff_l
                Mem.wl(PlayerBss.Plr1_SnapTYOff_l, d1);     // move.l d1,Plr1_SnapTYOff_l
                Mem.wl(PlayerBss.Plr1_SnapXOff_l, Mem.l(PlayerBss.Plr1_XOff_l)); // move.l Plr1_XOff_l,Plr1_SnapXOff_l
                Mem.wl(PlayerBss.Plr1_SnapZOff_l, Mem.l(PlayerBss.Plr1_ZOff_l)); // move.l Plr1_ZOff_l,Plr1_SnapZOff_l
                // SAVEREGS / GETREGS : sauvegarde registres (no-op Java)
                Mem.ww(Aud_NoiseX_w, 0);                    // move.w #0,Aud_NoiseX_w
                Mem.ww(Aud_NoiseZ_w, 0);                    // move.w #0,Aud_NoiseZ_w
                Mem.ww(Aud_SampleNum_w, 26);               // move.w #26,Aud_SampleNum_w
                Mem.ww(Aud_NoiseVol_w, 100);               // move.w #100,Aud_NoiseVol_w
                Mem.ww(IDNUM, 0xfff9);                      // move.w #$fff9,IDNUM
                MakeSomeNoise();                            // jsr MakeSomeNoise
                cantmove = true;                            // bra .cantmove
            }
        }
        if (!cantmove) {
            // .noteleport:
            Mem.wl(ObjectmoveData.Obj_ZonePtr_l, Mem.l(PlayerBss.Plr1_ZonePtr_l)); // move.l Plr1_ZonePtr_l,Obj_ZonePtr_l
            Mem.ww(ObjectmoveData.wallflags, 0b100000000);  // move.w #%100000000,wallflags
            Mem.wb(ObjectmoveData.StoodInTop, Mem.b(PlayerBss.Plr1_StoodInTop_b)); // move.b Plr1_StoodInTop_b,StoodInTop
            Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b1011111110111000011); // move.l #...,Obj_CollideFlags_l
            a0 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);         // move.l Plr1_ObjectPtr_l,a0
            Mem.ww(CollId, Mem.w(a0));                      // move.w (a0),CollId
            Objectmove.Obj_DoCollision(a2);                 // jsr Obj_DoCollision
            if (Mem.b(ObjectmoveData.hitwall) != 0) {       // tst.b hitwall ; beq .nothitanything
                Mem.ww(PlayerBss.Plr1_XOff_l, Mem.w(ObjectmoveData.oldx)); // move.w oldx,Plr1_XOff_l
                Mem.ww(PlayerBss.Plr1_ZOff_l, Mem.w(ObjectmoveData.oldz)); // move.w oldz,Plr1_ZOff_l
                Mem.wl(PlayerBss.Plr1_SnapXOff_l, Mem.l(PlayerBss.Plr1_XOff_l)); // move.l Plr1_XOff_l,Plr1_SnapXOff_l
                Mem.wl(PlayerBss.Plr1_SnapZOff_l, Mem.l(PlayerBss.Plr1_ZOff_l)); // move.l Plr1_ZOff_l,Plr1_SnapZOff_l
                // bra .cantmove
            } else {
                // .nothitanything:
                Mem.ww(ObjectmoveData.Obj_ExtLen_w, 40);    // move.w #40,Obj_ExtLen_w
                Mem.wb(ObjectmoveData.Obj_AwayFromWall_b, 0); // move.b #0,Obj_AwayFromWall_b
                Mem.wb(ObjectmoveData.exitfirst, 0);        // clr.b exitfirst
                Mem.wb(ObjectmoveData.Obj_WallBounce_b, 0); // clr.b Obj_WallBounce_b
                Objectmove.MoveObject();                    // bsr MoveObject
                Mem.wb(PlayerBss.Plr1_StoodInTop_b, Mem.b(ObjectmoveData.StoodInTop)); // move.b StoodInTop,Plr1_StoodInTop_b
                Mem.wl(PlayerBss.Plr1_ZonePtr_l, Mem.l(ObjectmoveData.Obj_ZonePtr_l)); // move.l Obj_ZonePtr_l,Plr1_ZonePtr_l
                Mem.ww(PlayerBss.Plr1_XOff_l, Mem.w(ObjectmoveData.newx)); // move.w newx,Plr1_XOff_l
                Mem.ww(PlayerBss.Plr1_ZOff_l, Mem.w(ObjectmoveData.newz)); // move.w newz,Plr1_ZOff_l
                Mem.wl(PlayerBss.Plr1_SnapXOff_l, Mem.l(PlayerBss.Plr1_XOff_l)); // move.l Plr1_XOff_l,Plr1_SnapXOff_l
                Mem.wl(PlayerBss.Plr1_SnapZOff_l, Mem.l(PlayerBss.Plr1_ZOff_l)); // move.l Plr1_ZOff_l,Plr1_SnapZOff_l
            }
        }
        // .cantmove:
        a0 = Mem.l(PlayerBss.Plr1_ZonePtr_l);               // move.l Plr1_ZonePtr_l,a0
        d0 = Mem.l(a0 + Defs.ZoneT_Floor_l);               // move.l ZoneT_Floor_l(a0),d0
        if (Mem.b(PlayerBss.Plr1_StoodInTop_b) != 0) {      // tst.b Plr1_StoodInTop_b ; beq notintop
            d0 = Mem.l(a0 + Defs.ZoneT_UpperFloor_l);       // move.l ZoneT_UpperFloor_l(a0),d0
        }
        // notintop:
        a0 = a0 + Defs.ZoneT_Points_w;                      // adda.w #ZoneT_Points_w,a0
        d0 = d0 - Mem.l(PlayerBss.Plr1_Height_l);          // sub.l Plr1_Height_l,d0
        Mem.wl(PlayerBss.Plr1_SnapTYOff_l, d0);            // move.l d0,Plr1_SnapTYOff_l
        Mem.ww(NewanimsData.tmpangpos, Mem.w(PlayerBss.Plr1_TmpAngPos_w)); // move.w Plr1_TmpAngPos_w,tmpangpos
        d1 = Mem.w(a0); a0 += 2;                            // move.w (a0)+,d1
        d1 = (short) d1;                                    // ext.l d1
        d1 = d1 + Mem.l(PlayerBss.Plr1_ZonePtr_l);         // add.l Plr1_ZonePtr_l,d1
        Mem.wl(PlayerBss.plr1_PointsToRotatePtr_l, d1);    // move.l d1,plr1_PointsToRotatePtr_l
        int back = Mem.b(a0); a0 += 1;                      // tst.b (a0)+
        if (back != 0                                       // beq nobackgraphics
                && Mem.b(Plr_MultiplayerType_b) != PLR_SLAVE) { // cmp #PLR_SLAVE ; beq nobackgraphics
            a0 = Newanims.Draw_SkyBackdrop(a0);            // jsr Draw_SkyBackdrop (préserve a0)
        }
        // nobackgraphics:
        Mem.wb(PlayerBss.Plr1_Echo_b, Mem.b(a0)); a0 += 1; // move.b (a0)+,Plr1_Echo_b
        a0 = a0 + 10;                                       // adda.w #10,a0
        Mem.wl(PlayerBss.Plr1_PotVisibleZoneListPtr_l, a0);// move.l a0,Plr1_PotVisibleZoneListPtr_l
        // rts
    }

    /**
     * Plr2_Control (hires.s 3014-3202) : analogue de Plr1_Control pour le joueur 2.
     * Diffèrences : wallflags=%100000000000, Obj_CollideFlags noteleport différent,
     * et les tests PLR_SLAVE inversés (le wobble et le backdrop du J2 ne s'appliquent
     * qu'en mode esclave). a2 = registre hérité passé à Obj_DoCollision.
     */
    static void Plr2_Control(int a2) {
        int d2 = Mem.l(PlayerBss.Plr2_XOff_l);               // move.l Plr2_XOff_l,d2
        Mem.wl(ObjectmoveData.oldx, d2);
        int d3 = Mem.l(PlayerBss.Plr2_ZOff_l);               // move.l Plr2_ZOff_l,d3
        Mem.wl(ObjectmoveData.oldz, d3);
        int d0 = Mem.l(PlayerBss.Plr2_TmpXOff_l);            // move.l Plr2_TmpXOff_l,d0
        Mem.wl(PlayerBss.Plr2_XOff_l, d0);
        Mem.wl(ObjectmoveData.newx, d0);
        int d1 = Mem.l(PlayerBss.Plr2_TmpZOff_l);            // move.l Plr2_TmpZOff_l,d1
        Mem.wl(ObjectmoveData.newz, d1);
        Mem.wl(PlayerBss.Plr2_ZOff_l, d1);
        Mem.wl(PlayerBss.Plr2_Height_l, Mem.l(PlayerBss.plr2_TmpHeight_l)); // move.l plr2_TmpHeight_l,Plr2_Height_l
        d0 = d0 - d2;
        d1 = d1 - d3;
        Mem.wl(ObjectmoveData.xdiff, d0);
        Mem.wl(ObjectmoveData.zdiff, d1);
        d0 = Mem.w(PlayerBss.Plr2_TmpAngPos_w);              // move.w Plr2_TmpAngPos_w,d0
        Mem.ww(PlayerBss.Plr2_AngPos_w, d0);
        int a1 = SinCosTable_vw;                             // move.l #SinCosTable_vw,a1
        Mem.ww(PlayerBss.Plr2_SinVal_w, Mem.w(a1 + d0));     // move.w (a1,d0.w),Plr2_SinVal_w
        d0 = setw(d0, d0 + COSINE_OFS);                      // add.w #COSINE_OFS,d0
        d0 = ab3d2.data.TablesData.AMOD_A(d0);              // AMOD_A d0
        Mem.ww(PlayerBss.Plr2_CosVal_w, Mem.w(a1 + (d0 & 0xFFFF))); // move.w (a1,d0.w),Plr2_CosVal_w
        d0 = Mem.l(PlayerBss.Plr2_TmpYOff_l);                // move.l Plr2_TmpYOff_l,d0
        d1 = Mem.w(PlayerBss.plr2_TmpBobble_w);              // move.w plr2_TmpBobble_w,d1
        d1 = Mem.w(a1 + d1);                                 // move.w (a1,d1.w),d1
        d3 = d1;                                             // move.w d1,d3
        if (!((short) d1 <= 0)) {                            // ble.s .notnegative
            d1 = setw(d1, -(short) d1);                      // neg.w d1
        }
        // .notnegative:
        d1 = setw(d1, (short) d1 + 16384);                  // add.w #16384,d1
        d1 = asrw(d1, 4);                                    // asr.w #4,d1
        if (Mem.b(PlayerBss.Plr2_Ducked_b) == 0             // tst.b Plr2_Ducked_b ; bne .notdouble
                && Mem.b(PlayerBss.Plr2_Squished_b) == 0) {  // tst.b Plr2_Squished_b ; bne .notdouble
            d1 = setw(d1, (short) d1 + (short) d1);          // add.w d1,d1
        }
        // .notdouble:
        d1 = (short) d1;                                     // ext.l d1
        Mem.wl(PlayerBss.plr2_BobbleY_l, d1);               // move.l d1,plr2_BobbleY_l
        int d4 = Mem.l(PlayerBss.Plr2_Height_l);            // move.l Plr2_Height_l,d4
        d4 = d4 - d1;                                        // sub.l d1,d4
        d0 = d0 + d1;                                        // add.l d1,d0
        if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) {     // cmp #PLR_SLAVE ; bne .otherwob (J2 : wobble si SLAVE)
            d3 = asrw(d3, 6);                                // asr.w #6,d3
            d3 = (short) d3;                                 // ext.l d3
            Mem.wl(xwobble, d3);                            // move.l d3,xwobble
            d1 = muls(Mem.w(PlayerBss.Plr2_SinVal_w), d3);   // move.w Plr2_SinVal_w,d1 ; muls d3,d1
            d2 = muls(Mem.w(PlayerBss.Plr2_CosVal_w), d3);   // move.w Plr2_CosVal_w,d2 ; muls d3,d2
            d1 = swap(d1);                                   // swap d1
            d2 = swap(d2);                                   // swap d2
            d1 = asrw(d1, 6);                                // asr.w #6,d1
            Mem.ww(xwobxoff, d1);                           // move.w d1,xwobxoff
            d2 = asrw(d2, 6);                                // asr.w #6,d2
            d2 = setw(d2, -(short) d2);                      // neg.w d2
            Mem.ww(xwobzoff, d2);                           // move.w d2,xwobzoff
        }
        // .otherwob:
        Mem.wl(PlayerBss.Plr2_YOff_l, d0);                  // move.l d0,Plr2_YOff_l
        Mem.wl(ObjectmoveData.newy, d0);
        Mem.wl(ObjectmoveData.oldy, d0);
        Mem.wl(ObjectmoveData.thingheight, d4);
        Mem.wl(ObjectmoveData.StepUpVal, 40 * 256);         // move.l #40*256,StepUpVal
        if (Mem.b(PlayerBss.Plr2_Squished_b) != 0
                || Mem.b(PlayerBss.Plr2_Ducked_b) != 0) {
            Mem.wl(ObjectmoveData.StepUpVal, 10 * 256);     // .smallstep
        }
        // .okbigstep:
        Mem.wl(ObjectmoveData.StepDownVal, 0x1000000);      // move.l #$1000000,StepDownVal
        int a0 = Mem.l(PlayerBss.Plr2_ZonePtr_l);           // move.l Plr2_ZonePtr_l,a0
        d0 = Mem.w(a0 + Defs.ZoneT_TelZone_w);             // move.w ZoneT_TelZone_w(a0),d0
        boolean cantmove = false;
        if (d0 >= 0) {                                       // blt .noteleport
            Mem.ww(ObjectmoveData.newx, Mem.w(a0 + Defs.ZoneT_TelX_w));
            Mem.ww(ObjectmoveData.newz, Mem.w(a0 + Defs.ZoneT_TelZ_w));
            a0 = Mem.l(PlayerBss.Plr2_ObjectPtr_l);         // move.l Plr2_ObjectPtr_l,a0
            Mem.ww(CollId, Mem.w(a0));
            Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b111111111111111111);
            Objectmove.Obj_DoCollision(a2);
            if (Mem.b(ObjectmoveData.hitwall) != 0) {       // tst.b hitwall ; beq .teleport
                Mem.ww(ObjectmoveData.newx, Mem.w(PlayerBss.Plr2_XOff_l));
                Mem.ww(ObjectmoveData.newz, Mem.w(PlayerBss.Plr2_ZOff_l));
            } else {
                // .teleport:
                Mem.wb(PlayerBss.plr2_Teleported_b, 0xFF);  // st plr2_Teleported_b
                a0 = Mem.l(PlayerBss.Plr2_ZonePtr_l);
                d0 = Mem.w(a0 + Defs.ZoneT_TelZone_w);
                Mem.ww(PlayerBss.Plr2_XOff_l, Mem.w(a0 + Defs.ZoneT_TelX_w));
                Mem.ww(PlayerBss.Plr2_ZOff_l, Mem.w(a0 + Defs.ZoneT_TelZ_w));
                d1 = Mem.l(PlayerBss.Plr2_YOff_l);
                d1 = d1 - Mem.l(a0 + Defs.ZoneT_Floor_l);
                a0 = Mem.l(Lvl_ZonePtrsPtr_l);
                a0 = Mem.l(a0 + (short) d0 * 4);
                Mem.wl(PlayerBss.Plr2_ZonePtr_l, a0);
                d1 = d1 + Mem.l(a0 + Defs.ZoneT_Floor_l);
                Mem.wl(PlayerBss.Plr2_SnapYOff_l, d1);
                Mem.wl(PlayerBss.Plr2_YOff_l, d1);
                Mem.wl(PlayerBss.Plr2_SnapTYOff_l, d1);
                Mem.wl(PlayerBss.Plr2_SnapXOff_l, Mem.l(PlayerBss.Plr2_XOff_l));
                Mem.wl(PlayerBss.Plr2_SnapZOff_l, Mem.l(PlayerBss.Plr2_ZOff_l));
                Mem.ww(Aud_NoiseX_w, 0);
                Mem.ww(Aud_NoiseZ_w, 0);
                Mem.ww(Aud_SampleNum_w, 26);
                Mem.ww(Aud_NoiseVol_w, 100);
                Mem.ww(IDNUM, 0xfff9);
                MakeSomeNoise();
                cantmove = true;                            // bra .cantmove
            }
        }
        if (!cantmove) {
            // .noteleport:
            Mem.wl(ObjectmoveData.Obj_ZonePtr_l, Mem.l(PlayerBss.Plr2_ZonePtr_l));
            Mem.ww(ObjectmoveData.wallflags, 0b100000000000); // move.w #%100000000000,wallflags
            Mem.wb(ObjectmoveData.StoodInTop, Mem.b(PlayerBss.Plr2_StoodInTop_b));
            Mem.wl(ObjectmoveData.Obj_CollideFlags_l, 0b1011111010111100011); // move.l #...,Obj_CollideFlags_l
            a0 = Mem.l(PlayerBss.Plr2_ObjectPtr_l);
            Mem.ww(CollId, Mem.w(a0));
            Objectmove.Obj_DoCollision(a2);
            if (Mem.b(ObjectmoveData.hitwall) != 0) {       // tst.b hitwall ; beq .nothitanything
                Mem.ww(PlayerBss.Plr2_XOff_l, Mem.w(ObjectmoveData.oldx));
                Mem.ww(PlayerBss.Plr2_ZOff_l, Mem.w(ObjectmoveData.oldz));
                Mem.wl(PlayerBss.Plr2_SnapXOff_l, Mem.l(PlayerBss.Plr2_XOff_l));
                Mem.wl(PlayerBss.Plr2_SnapZOff_l, Mem.l(PlayerBss.Plr2_ZOff_l));
            } else {
                // .nothitanything:
                Mem.ww(ObjectmoveData.Obj_ExtLen_w, 40);
                Mem.wb(ObjectmoveData.Obj_AwayFromWall_b, 0);
                Mem.wb(ObjectmoveData.exitfirst, 0);
                Mem.wb(ObjectmoveData.Obj_WallBounce_b, 0);
                Objectmove.MoveObject();
                Mem.wb(PlayerBss.Plr2_StoodInTop_b, Mem.b(ObjectmoveData.StoodInTop));
                Mem.wl(PlayerBss.Plr2_ZonePtr_l, Mem.l(ObjectmoveData.Obj_ZonePtr_l));
                Mem.ww(PlayerBss.Plr2_XOff_l, Mem.w(ObjectmoveData.newx));
                Mem.ww(PlayerBss.Plr2_ZOff_l, Mem.w(ObjectmoveData.newz));
                Mem.wl(PlayerBss.Plr2_SnapXOff_l, Mem.l(PlayerBss.Plr2_XOff_l));
                Mem.wl(PlayerBss.Plr2_SnapZOff_l, Mem.l(PlayerBss.Plr2_ZOff_l));
            }
        }
        // .cantmove:
        a0 = Mem.l(PlayerBss.Plr2_ZonePtr_l);
        d0 = Mem.l(a0 + Defs.ZoneT_Floor_l);
        if (Mem.b(PlayerBss.Plr2_StoodInTop_b) != 0) {      // tst.b Plr2_StoodInTop_b ; beq .notintop
            d0 = Mem.l(a0 + Defs.ZoneT_UpperFloor_l);
        }
        // .notintop:
        a0 = a0 + Defs.ZoneT_Points_w;
        d0 = d0 - Mem.l(PlayerBss.Plr2_Height_l);
        Mem.wl(PlayerBss.Plr2_SnapTYOff_l, d0);
        Mem.ww(NewanimsData.tmpangpos, Mem.w(PlayerBss.Plr2_TmpAngPos_w));
        d1 = Mem.w(a0); a0 += 2;                            // move.w (a0)+,d1
        d1 = (short) d1;
        d1 = d1 + Mem.l(PlayerBss.Plr2_ZonePtr_l);
        Mem.wl(PlayerBss.plr2_PointsToRotatePtr_l, d1);
        int back = Mem.b(a0); a0 += 1;                      // tst.b (a0)+
        if (back != 0                                       // beq .nobackgraphics
                && Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) { // cmp #PLR_SLAVE ; bne .nobackgraphics (J2 : backdrop si SLAVE)
            a0 = Newanims.Draw_SkyBackdrop(a0);
        }
        // .nobackgraphics:
        Mem.wb(PlayerBss.Plr2_Echo_b, Mem.b(a0)); a0 += 1;
        a0 = a0 + 10;
        Mem.wl(PlayerBss.Plr2_PotVisibleZoneListPtr_l, a0);
        // rts
    }

    /**
     * Plr1_Use (hires.s 2263-2536) : prépare les ENTITÉS à dessiner pour la vue du
     * joueur 1 — l'objet J1 (lui-même), l'objet J2 (l'autre joueur, visible) et
     * l'arme en main de J1 (entité ENT_NEXT_2). Applique le recul (impacts → Snap*),
     * la luminosité de zone, l'angle, la frame d'animation et la santé.
     *
     * QUIRKs préservés (asymétries de l'original) : le bloc J1 fait SnapYVel + clear
     * impacts ; le bloc J2 aussi (mais via une variante sans le « twist » d4).
     */
    static void Plr1_Use() {
        int a0 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);          // move.l Plr1_ObjectPtr_l,a0
        Mem.wb(a0 + Defs.ObjT_TypeID_b, Defs.OBJ_TYPE_PLAYER1); // move.b #OBJ_TYPE_PLAYER1,ObjT_TypeID_b(a0)
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);               // move.l Lvl_ObjectPointsPtr_l,a1
        // a2 = #ObjRotated_vl (mort) ; référence symbole conservée :
        if (ObjRotated_vl == 0) { /* no-op : garde le symbole */ }
        int d0 = Mem.w(a0);                                  // move.w (a0),d0
        Mem.wl(a1 + (short) d0 * 8, Mem.l(PlayerBss.Plr1_XOff_l));     // move.l Plr1_XOff_l,(a1,d0.w*8)
        Mem.wl(a1 + (short) d0 * 8 + 4, Mem.l(PlayerBss.Plr1_ZOff_l)); // move.l Plr1_ZOff_l,4(a1,d0.w*8)
        a1 = Mem.l(PlayerBss.Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a1
        int d2 = Mem.ub(a0 + Defs.EntT_DamageTaken_b);       // moveq#0,d2 ; move.b EntT_DamageTaken_b(a0),d2
        if (d2 != 0) {                                       // beq .notbeenshot
            int d4 = 0;                                      // moveq #0,d4
            int d3 = Mem.w(a0 + Defs.EntT_ImpactX_w);        // move.w EntT_ImpactX_w(a0),d3
            if (d3 != 0) d4 = d2;                            // beq .notwist ; move.w d2,d4
            Mem.ww(PlayerBss.Plr1_SnapXSpdVal_l, Mem.w(PlayerBss.Plr1_SnapXSpdVal_l) + d3); // add.w d3,Plr1_SnapXSpdVal_l
            d3 = Mem.w(a0 + Defs.EntT_ImpactZ_w);            // move.w EntT_ImpactZ_w(a0),d3
            if (d3 != 0) d4 = d2;                            // beq .notwist2 ; move.w d2,d4
            Mem.ww(PlayerBss.Plr1_SnapZSpdVal_l, Mem.w(PlayerBss.Plr1_SnapZSpdVal_l) + d3); // add.w d3,Plr1_SnapZSpdVal_l
            d3 = Mem.w(a0 + Defs.EntT_ImpactY_w);            // move.w EntT_ImpactY_w(a0),d3
            d3 = (short) d3;                                 // ext.l d3
            d3 = d3 << 8;                                    // asl.l #8,d3
            Mem.wl(PlayerBss.Plr1_SnapYVel_l, Mem.l(PlayerBss.Plr1_SnapYVel_l) + d3); // add.l d3,Plr1_SnapYVel_l
            Mem.ww(a0 + Defs.EntT_ImpactX_w, 0);             // move.w #0,EntT_ImpactX_w(a0)
            Mem.ww(a0 + Defs.EntT_ImpactY_w, 0);             // move.w #0,EntT_ImpactY_w(a0)
            Mem.ww(a0 + Defs.EntT_ImpactZ_w, 0);             // move.w #0,EntT_ImpactZ_w(a0)
            d0 = Objectmove.GetRand();                       // jsr GetRand
            d0 = muls(d0, d4);                               // muls d4,d0
            d0 = d0 >> 8;                                    // asr.l #8,d0
            d0 = d0 >> 4;                                    // asr.l #4,d0
            Mem.ww(PlayerBss.Plr1_SnapAngSpd_w, Mem.w(PlayerBss.Plr1_SnapAngSpd_w) + d0); // add.w d0,Plr1_SnapAngSpd_w
            Mem.wl(hitcol, 7 * 2116);                        // move.l #7*2116,hitcol
            Mem.ww(PlayerBss.Plr1_Health_w, Mem.w(PlayerBss.Plr1_Health_w) - d2); // sub.w d2,Plr1_Health_w
            // SAVEREGS / GETREGS (no-op)
            Mem.ww(IDNUM, 0xfffa);                           // move.w #$fffa,IDNUM
            Mem.ww(Aud_SampleNum_w, 19);                     // move.w #19,Aud_SampleNum_w
            Mem.wb(notifplaying, 0);                         // clr.b notifplaying
            Mem.ww(Aud_NoiseX_w, 0);                         // move.w #0,Aud_NoiseX_w
            Mem.ww(Aud_NoiseZ_w, 0);                         // move.w #0,Aud_NoiseZ_w
            Mem.ww(Aud_NoiseVol_w, 60);                      // move.w #60,Aud_NoiseVol_w
            MakeSomeNoise();                                 // jsr MakeSomeNoise
        }
        // .notbeenshot:
        Mem.wb(a0 + Defs.EntT_DamageTaken_b, 0);             // move.b #0,EntT_DamageTaken_b(a0)
        Mem.wb(a0 + Defs.EntT_HitPoints_b, 10);              // move.b #10,EntT_HitPoints_b(a0)
        Mem.ww(a0 + Defs.EntT_CurrentAngle_w, Mem.w(PlayerBss.Plr1_TmpAngPos_w)); // move.w Plr1_TmpAngPos_w,EntT_CurrentAngle_w(a0)
        Mem.wb(a0 + Defs.ShotT_InUpperZone_b, Mem.b(PlayerBss.Plr1_StoodInTop_b)); // move.b Plr1_StoodInTop_b,ShotT_InUpperZone_b(a0)
        Mem.ww(a0 + Defs.ObjT_ZoneID_w, Mem.w(a1));          // move.w (a1),ObjT_ZoneID_w(a0)
        d2 = Mem.w(a1);                                      // move.w (a1),d2
        a1 = Zone_BrightTable_vl;                            // move.l #Zone_BrightTable_vl,a1
        d2 = Mem.l(a1 + (short) d2 * 4);                     // move.l (a1,d2.w*4),d2
        if (Mem.b(PlayerBss.Plr1_StoodInTop_b) == 0) {       // tst.b Plr1_StoodInTop_b ; bne .okinbott
            d2 = swap(d2);                                   // swap d2
        }
        // .okinbott:
        Mem.ww(a0 + 2, d2);                                  // move.w d2,2(a0)
        d0 = Mem.l(PlayerBss.Plr1_TmpYOff_l);                // move.l Plr1_TmpYOff_l,d0
        int d1 = Mem.l(PlayerBss.plr1_TmpHeight_l);          // move.l plr1_TmpHeight_l,d1
        d1 = d1 >> 1;                                        // asr.l #1,d1
        d0 = d0 + d1;                                        // add.l d1,d0
        d0 = d0 >> 7;                                        // asr.l #7,d0
        Mem.ww(a0 + 4, d0);                                  // move.w d0,4(a0)
        if (!((short) Mem.w(PlayerBss.Plr1_Health_w) > 0)) { // tst.w Plr1_Health_w ; bgt .okh1
            ab3d2.Macros.FREE_OBJ(a0);                       // FREE_OBJ a0
        }
        // .okh1: ====== objet J2 (l'autre joueur, visible) ======
        a0 = Mem.l(PlayerBss.Plr2_ObjectPtr_l);              // move.l Plr2_ObjectPtr_l,a0
        Mem.wb(a0 + Defs.ObjT_TypeID_b, Defs.OBJ_TYPE_PLAYER2); // move.b #OBJ_TYPE_PLAYER2,ObjT_TypeID_b(a0)
        d0 = Mem.w(PlayerBss.Plr2_TmpAngPos_w);              // move.w Plr2_TmpAngPos_w,d0
        d0 = ab3d2.data.TablesData.AMOD_A(d0);              // AMOD_A d0
        Mem.ww(a0 + Defs.EntT_CurrentAngle_w, d0);           // move.w d0,EntT_CurrentAngle_w(a0)
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);                   // move.l Lvl_ObjectPointsPtr_l,a1
        d0 = Mem.w(a0);                                      // move.w (a0),d0
        Mem.wl(a1 + (short) d0 * 8, Mem.l(PlayerBss.Plr2_XOff_l));     // move.l Plr2_XOff_l,(a1,d0.w*8)
        Mem.wl(a1 + (short) d0 * 8 + 4, Mem.l(PlayerBss.Plr2_ZOff_l)); // move.l Plr2_ZOff_l,4(a1,d0.w*8)
        a1 = Mem.l(PlayerBss.Plr2_ZonePtr_l);                // move.l Plr2_ZonePtr_l,a1
        d2 = Mem.ub(a0 + Defs.EntT_DamageTaken_b);           // moveq#0,d2 ; move.b EntT_DamageTaken_b(a0),d2
        if (d2 != 0) {                                       // beq .notbeenshot2
            int d3 = Mem.w(a0 + Defs.EntT_ImpactX_w);        // move.w EntT_ImpactX_w(a0),d3
            Mem.ww(PlayerBss.Plr2_SnapXSpdVal_l, Mem.w(PlayerBss.Plr2_SnapXSpdVal_l) + d3); // add.w d3,Plr2_SnapXSpdVal_l
            d3 = Mem.w(a0 + Defs.EntT_ImpactZ_w);            // move.w EntT_ImpactZ_w(a0),d3
            Mem.ww(PlayerBss.Plr2_SnapZSpdVal_l, Mem.w(PlayerBss.Plr2_SnapZSpdVal_l) + d3); // add.w d3,Plr2_SnapZSpdVal_l
            d3 = Mem.w(a0 + Defs.EntT_ImpactY_w);            // move.w EntT_ImpactY_w(a0),d3
            d3 = (short) d3;                                 // ext.l d3
            d3 = d3 << 8;                                    // asl.l #8,d3
            Mem.wl(PlayerBss.Plr2_SnapYVel_l, Mem.l(PlayerBss.Plr2_SnapYVel_l) + d3); // add.l d3,Plr2_SnapYVel_l
            Mem.ww(a0 + Defs.EntT_ImpactX_w, 0);             // move.w #0,EntT_ImpactX_w(a0)
            Mem.ww(a0 + Defs.EntT_ImpactY_w, 0);             // move.w #0,EntT_ImpactY_w(a0)
            Mem.ww(a0 + Defs.EntT_ImpactZ_w, 0);             // move.w #0,EntT_ImpactZ_w(a0)
            Mem.ww(PlayerBss.Plr2_Health_w, Mem.w(PlayerBss.Plr2_Health_w) - d2); // sub.w d2,Plr2_Health_w
        }
        // .notbeenshot2:
        Mem.wb(a0 + Defs.EntT_DamageTaken_b, 0);             // move.b #0,EntT_DamageTaken_b(a0)
        Mem.wb(a0 + Defs.EntT_HitPoints_b, 10);              // move.b #10,EntT_HitPoints_b(a0)
        Mem.wb(a0 + Defs.ShotT_InUpperZone_b, Mem.b(PlayerBss.Plr2_StoodInTop_b)); // move.b Plr2_StoodInTop_b,ShotT_InUpperZone_b(a0)
        Mem.ww(a0 + Defs.ObjT_ZoneID_w, Mem.w(a1));          // move.w (a1),ObjT_ZoneID_w(a0)
        d2 = Mem.w(a1);                                      // move.w (a1),d2
        a1 = Zone_BrightTable_vl;                            // move.l #Zone_BrightTable_vl,a1
        d2 = Mem.l(a1 + (short) d2 * 4);                     // move.l (a1,d2.w*4),d2
        if (Mem.b(PlayerBss.Plr2_StoodInTop_b) == 0) {       // tst.b Plr2_StoodInTop_b ; bne .okinbott2
            d2 = swap(d2);                                   // swap d2
        }
        // .okinbott2:
        Mem.ww(a0 + 2, d2);                                  // move.w d2,2(a0)
        d0 = Mem.l(PlayerBss.Plr2_TmpYOff_l);                // move.l Plr2_TmpYOff_l,d0
        d1 = Mem.l(PlayerBss.plr2_TmpHeight_l);              // move.l plr2_TmpHeight_l,d1
        d1 = d1 >> 1;                                        // asr.l #1,d1
        d0 = d0 + d1;                                        // add.l d1,d0
        d0 = d0 >> 7;                                        // asr.l #7,d0
        Mem.ww(a0 + 4, d0);                                  // move.w d0,4(a0)
        d0 = Newaliencontrol.ViewpointToDraw(a0);            // jsr ViewpointToDraw
        d0 = d0 + d0;                                        // add.l d0,d0
        plr_Use_AnimSetup(a0, d0, Defs.GLFT_Player2Graphic_w); // bloc GFX/anim partagé (Player2Graphic)
        if (!((short) Mem.w(PlayerBss.Plr2_Health_w) > 0)) { // tst.w Plr2_Health_w ; bgt .ddone→.okh
            ab3d2.Macros.FREE_OBJ(a0);                       // FREE_OBJ a0
        }
        // .okh: ====== arme en main de J1 (entité ENT_NEXT_2) ======
        a0 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);              // move.l Plr1_ObjectPtr_l,a0
        if (!((short) Mem.w(PlayerBss.Plr1_Health_w) > 0)) { // tst.w Plr1_Health_w ; bgt .notdead
            ab3d2.Macros.FREE_OBJ_2(a0, Defs.ENT_NEXT_2);    // FREE_OBJ_2 a0,ENT_NEXT_2
            return;                                          // rts
        }
        // .notdead:
        a1 = Mem.l(PlayerBss.Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a1
        d0 = Mem.w(a0 + Defs.EntT_CurrentAngle_w);           // move.w EntT_CurrentAngle_w(a0),d0
        d0 = setw(d0, d0 + SINE_SIZE);                       // add.w #SINE_SIZE,d0
        d0 = ab3d2.data.TablesData.AMOD_A(d0);              // AMOD_A d0
        Mem.ww(a0 + Defs.EntT_CurrentAngle_w + Defs.ENT_NEXT_2, d0); // move.w d0,EntT_CurrentAngle_w+ENT_NEXT_2(a0)
        Mem.ww(a0 + Defs.ObjT_ZoneID_w + Defs.ENT_NEXT_2, Mem.w(a1)); // move.w (a1),ObjT_ZoneID_w+ENT_NEXT_2(a0)
        Mem.ww(a0 + Defs.EntT_ZoneID_w + Defs.ENT_NEXT_2, Mem.w(a1)); // move.w (a1),EntT_ZoneID_w+ENT_NEXT_2(a0)
        d0 = Mem.ub(PlayerBss.Plr1_TmpGunSelected_b);        // moveq#0,d0 ; move.b Plr1_TmpGunSelected_b,d0
        a1 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_GunObjects_l; // GLF_DatabasePtr + GLFT_GunObjects_l
        d0 = Mem.w(a1 + (short) d0 * 2);                     // move.w (a1,d0.w*2),d0
        Mem.wb(a0 + Defs.EntT_Type_b + Defs.ENT_NEXT_2, d0); // move.b d0,EntT_Type_b+ENT_NEXT_2(a0)
        Mem.wb(a0 + Defs.ObjT_TypeID_b + Defs.ENT_NEXT_2, Defs.OBJ_TYPE_OBJECT); // move.b #OBJ_TYPE_OBJECT,...
        d0 = Mem.w(a0);                                      // move.w (a0),d0
        d1 = Mem.w(a0 + Defs.ENT_NEXT_2);                    // move.w ENT_NEXT_2(a0),d1
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);                   // move.l Lvl_ObjectPointsPtr_l,a1
        Mem.wl(a1 + (short) d1 * 8, Mem.l(a1 + (short) d0 * 8));         // move.l (a1,d0.w*8),(a1,d1.w*8)
        Mem.wl(a1 + (short) d1 * 8 + 4, Mem.l(a1 + (short) d0 * 8 + 4)); // move.l 4(a1,d0.w*8),4(a1,d1.w*8)
        Mem.wb(a0 + Defs.EntT_WhichAnim_b + Defs.ENT_NEXT_2, 0xFF); // st EntT_WhichAnim_b+ENT_NEXT_2(a0)
        d0 = Mem.l(PlayerBss.Plr1_TmpYOff_l);                // move.l Plr1_TmpYOff_l,d0
        d1 = Mem.l(PlayerBss.plr1_TmpHeight_l);              // move.l plr1_TmpHeight_l,d1
        d1 = d1 >> 2;                                        // asr.l #2,d1
        d1 = d1 + 10 * 128;                                  // add.l #10*128,d1
        d0 = d0 + d1;                                        // add.l d1,d0
        d0 = d0 >> 7;                                        // asr.l #7,d0
        Mem.ww(a0 + 4 + Defs.ENT_NEXT_2, d0);                // move.w d0,4+ENT_NEXT_2(a0)
        d1 = Mem.l(PlayerBss.plr1_BobbleY_l);                // move.l plr1_BobbleY_l,d1
        d1 = d1 >> 8;                                        // asr.l #8,d1
        d0 = d1;                                             // move.l d1,d0
        d0 = d0 >> 1;                                        // asr.l #1,d0
        d1 = d1 + d0;                                        // add.l d0,d1
        Mem.ww(a0 + 4 + Defs.ENT_NEXT_2, Mem.w(a0 + 4 + Defs.ENT_NEXT_2) + d1); // add.w d1,4+ENT_NEXT_2(a0)
        Mem.wb(a0 + Defs.ShotT_InUpperZone_b + Defs.ENT_NEXT_2, Mem.b(a0 + Defs.ShotT_InUpperZone_b)); // move.b ...
        // rts
    }

    /**
     * plr_Use_AnimSetup : bloc commun de sélection GFX/animation pour l'objet
     * "autre joueur" (hires.s 2409-2479 / 2670-2746). a0 = objet, d0 = option*2
     * (depuis ViewpointToDraw), graphicOfs = GLFT_Player1/2Graphic_w.
     */
    private static void plr_Use_AnimSetup(int a0, int d0, int graphicOfs) {
        int a6 = Mem.l(GLF_DatabasePtr_l) + graphicOfs;      // GLF_DatabasePtr + GLFT_PlayerNGraphic_w
        int d7 = Mem.w(a6);                                  // move.w (a6),d7
        int d1 = d7;                                         // move.w d7,d1
        a6 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_AlienDefs_l; // GLF_DatabasePtr + GLFT_AlienDefs_l
        d1 = muls(d1, Defs.AlienT_SizeOf_l);                 // muls #AlienT_SizeOf_l,d1
        a6 = a6 + d1;                                        // add.l d1,a6
        Mem.wb(AI_VecObj_w, Mem.b(a6 + Defs.AlienT_GFXType_w + 1)); // move.b AlienT_GFXType_w+1(a6),AI_VecObj_w
        if (Mem.w(a6 + Defs.AlienT_GFXType_w) == 1) {        // cmp.w #1,AlienT_GFXType_w(a6) ; bne .NOSIDES2
            d0 = 0;                                          // moveq #0,d0
        }
        // .NOSIDES2:
        a6 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_AlienAnims_l; // GLF_DatabasePtr + GLFT_AlienAnims_l
        d1 = d7;                                             // move.w d7,d1
        d1 = muls(d1, Defs.A_AnimLen);                       // muls #A_AnimLen,d1
        a6 = a6 + d1;                                        // add.l d1,a6
        d0 = muls(d0, Defs.A_OptLen);                        // muls #A_OptLen,d0
        a6 = a6 + (short) d0;                                // add.w d0,a6
        d1 = Mem.w(a0 + Defs.EntT_Timer2_w);                 // move.w EntT_Timer2_w(a0),d1
        int d2 = d1;                                         // move.w d1,d2
        d1 = muls(d1, Defs.A_FrameLen);                      // muls #A_FrameLen,d1
        d2 = setw(d2, d2 + 1);                               // addq #1,d2
        int d3 = d2;                                         // move.w d2,d3
        d3 = muls(d3, Defs.A_FrameLen);                      // muls #A_FrameLen,d3
        if ((byte) Mem.b(a6 + (short) d3) < 0) {             // tst.b (a6,d3.w) ; bge .noendanim
            d2 = setw(d2, 0);                                // move.w #0,d2
        }
        // .noendanim:
        Mem.ww(a0 + Defs.EntT_Timer2_w, d2);                 // move.w d2,EntT_Timer2_w(a0)
        d1 = d2;                                             // move.w d2,d1
        d1 = muls(d1, Defs.A_FrameLen);                      // muls #A_FrameLen,d1
        Mem.wl(a0 + 8, 0);                                  // move.l #0,8(a0)
        Mem.wb(a0 + 9, Mem.b(a6 + (short) d1));             // move.b (a6,d1.w),9(a0)
        d0 = Mem.b(a6 + (short) d1 + 1);                    // move.b 1(a6,d1.w),d0
        d0 = (short) d0;                                    // ext.w d0
        if (!((short) d0 > 0)) {                            // bgt .noflip
            Mem.wb(a0 + 10, 128);                           // move.b #128,10(a0)
            d0 = setw(d0, -(short) d0);                     // neg.w d0
        }
        // .noflip:
        d0 = setw(d0, (short) d0 - 1);                      // sub.w #1,d0
        Mem.wb(a0 + 11, d0);                                // move.b d0,11(a0)
        Mem.ww(a0 + 6, -1);                                 // move.w #-1,6(a0)
        int vec = Mem.b(AI_VecObj_w);                       // cmp.b #1,AI_VecObj_w
        if (vec == 1) {                                     // beq .nosize
            // .nosize: bra .ddone (rien)
        } else if (vec > 1) {                              // bgt .setlight
            // .setlight:
            Mem.ww(a0 + 6, Mem.w(a6 + (short) d1 + 2));    // move.w 2(a6,d1.w),6(a0)
            int dl = Mem.b(AI_VecObj_w);                   // move.b AI_VecObj_w,d1
            Mem.wb(a0 + 10, Mem.b(a0 + 10) | dl);          // or.b d1,10(a0)
        } else {                                           // (vec < 1)
            Mem.ww(a0 + 6, Mem.w(a6 + (short) d1 + 2));    // move.w 2(a6,d1.w),6(a0)
        }
        // .ddone:
    }

    /**
     * Plr2_Use (hires.s 2539-2806) : symétrique de Plr1_Use pour la vue du joueur 2
     * (objet J2 lui-même, objet J1 visible, arme en main J2 = ENT_NEXT). QUIRKs :
     * le bloc J2 (self) ne fait PAS SnapYVel ni clear impacts ; l'ordre des registres
     * audio diffère ; le bloc J1 (autre) utilise Plr1_AngPos (avec AMOD) et pas de Y/clear.
     */
    static void Plr2_Use() {
        int a0 = Mem.l(PlayerBss.Plr2_ObjectPtr_l);          // move.l Plr2_ObjectPtr_l,a0
        Mem.wb(a0 + Defs.ObjT_TypeID_b, Defs.OBJ_TYPE_PLAYER2); // move.b #OBJ_TYPE_PLAYER2,ObjT_TypeID_b(a0)
        int a1 = Mem.l(Lvl_ObjectPointsPtr_l);               // move.l Lvl_ObjectPointsPtr_l,a1
        int d0 = Mem.w(a0);                                  // move.w (a0),d0
        Mem.wl(a1 + (short) d0 * 8, Mem.l(PlayerBss.Plr2_XOff_l));     // move.l Plr2_XOff_l,(a1,d0.w*8)
        Mem.wl(a1 + (short) d0 * 8 + 4, Mem.l(PlayerBss.Plr2_ZOff_l)); // move.l Plr2_ZOff_l,4(a1,d0.w*8)
        a1 = Mem.l(PlayerBss.Plr2_ZonePtr_l);                // move.l Plr2_ZonePtr_l,a1
        int d2 = Mem.ub(a0 + Defs.EntT_DamageTaken_b);       // moveq#0,d2 ; move.b EntT_DamageTaken_b(a0),d2
        if (d2 != 0) {                                       // beq .notbeenshot
            int d4 = 0;                                      // moveq #0,d4
            int d3 = Mem.w(a0 + Defs.EntT_ImpactX_w);        // move.w EntT_ImpactX_w(a0),d3
            if (d3 != 0) d4 = d2;                            // beq .notwist ; move.w d2,d4
            Mem.ww(PlayerBss.Plr2_SnapXSpdVal_l, Mem.w(PlayerBss.Plr2_SnapXSpdVal_l) + d3); // add.w d3,Plr2_SnapXSpdVal_l
            d3 = Mem.w(a0 + Defs.EntT_ImpactZ_w);            // move.w EntT_ImpactZ_w(a0),d3
            if (d3 != 0) d4 = d2;                            // beq .notwist2 ; move.w d2,d4
            Mem.ww(PlayerBss.Plr2_SnapZSpdVal_l, Mem.w(PlayerBss.Plr2_SnapZSpdVal_l) + d3); // add.w d3,Plr2_SnapZSpdVal_l
            d0 = Objectmove.GetRand();                       // jsr GetRand
            d0 = muls(d0, d4);                               // muls d4,d0
            d0 = d0 >> 8;                                    // asr.l #8,d0
            d0 = d0 >> 4;                                    // asr.l #4,d0
            Mem.ww(PlayerBss.Plr2_SnapAngSpd_w, Mem.w(PlayerBss.Plr2_SnapAngSpd_w) + d0); // add.w d0,Plr2_SnapAngSpd_w
            Mem.wl(hitcol, 7 * 2116);                        // move.l #7*2116,hitcol
            Mem.ww(PlayerBss.Plr2_Health_w, Mem.w(PlayerBss.Plr2_Health_w) - d2); // sub.w d2,Plr2_Health_w
            // SAVEREGS / GETREGS (no-op)
            Mem.ww(Aud_SampleNum_w, 19);                     // move.w #19,Aud_SampleNum_w
            Mem.wb(notifplaying, 0);                         // clr.b notifplaying
            Mem.ww(IDNUM, 0xfffa);                           // move.w #$fffa,IDNUM
            Mem.ww(Aud_NoiseX_w, 0);                         // move.w #0,Aud_NoiseX_w
            Mem.ww(Aud_NoiseZ_w, 0);                         // move.w #0,Aud_NoiseZ_w
            Mem.ww(Aud_NoiseVol_w, 60);                      // move.w #60,Aud_NoiseVol_w
            MakeSomeNoise();                                 // jsr MakeSomeNoise
        }
        // .notbeenshot:
        Mem.wb(a0 + Defs.EntT_DamageTaken_b, 0);             // move.b #0,EntT_DamageTaken_b(a0)
        Mem.wb(a0 + Defs.EntT_HitPoints_b, 10);              // move.b #10,EntT_HitPoints_b(a0)
        Mem.ww(a0 + Defs.EntT_CurrentAngle_w, Mem.w(PlayerBss.Plr2_TmpAngPos_w)); // move.w Plr2_TmpAngPos_w,EntT_CurrentAngle_w(a0)
        Mem.wb(a0 + Defs.ShotT_InUpperZone_b, Mem.b(PlayerBss.Plr2_StoodInTop_b)); // move.b Plr2_StoodInTop_b,ShotT_InUpperZone_b(a0)
        Mem.ww(a0 + Defs.ObjT_ZoneID_w, Mem.w(a1));          // move.w (a1),ObjT_ZoneID_w(a0)
        d2 = Mem.w(a1);                                      // move.w (a1),d2
        a1 = Zone_BrightTable_vl;                            // move.l #Zone_BrightTable_vl,a1
        d2 = Mem.l(a1 + (short) d2 * 4);                     // move.l (a1,d2.w*4),d2
        if (Mem.b(PlayerBss.Plr2_StoodInTop_b) == 0) {       // tst.b Plr2_StoodInTop_b ; bne .okinbott
            d2 = swap(d2);                                   // swap d2
        }
        // .okinbott:
        Mem.ww(a0 + 2, d2);                                  // move.w d2,2(a0)
        d0 = Mem.l(PlayerBss.Plr2_YOff_l);                   // move.l Plr2_YOff_l,d0
        int d1 = Mem.l(PlayerBss.plr2_TmpHeight_l);          // move.l plr2_TmpHeight_l,d1
        d1 = d1 >> 1;                                        // asr.l #1,d1
        d0 = d0 + d1;                                        // add.l d1,d0
        d0 = d0 >> 7;                                        // asr.l #7,d0
        Mem.ww(a0 + 4, d0);                                  // move.w d0,4(a0)
        if (!((short) Mem.w(PlayerBss.Plr2_Health_w) > 0)) { // tst.w Plr2_Health_w ; bgt .okh55
            ab3d2.Macros.FREE_OBJ(a0);                       // FREE_OBJ a0
        }
        // .okh55: ====== objet J1 (l'autre joueur, visible) ======
        a0 = Mem.l(PlayerBss.Plr1_ObjectPtr_l);              // move.l Plr1_ObjectPtr_l,a0
        Mem.wb(a0 + Defs.ObjT_TypeID_b, Defs.OBJ_TYPE_PLAYER1); // move.b #OBJ_TYPE_PLAYER1,ObjT_TypeID_b(a0)
        d0 = Mem.w(PlayerBss.Plr1_AngPos_w);                 // move.w Plr1_AngPos_w,d0
        d0 = ab3d2.data.TablesData.AMOD_A(d0);              // AMOD_A d0
        Mem.ww(a0 + Defs.EntT_CurrentAngle_w, d0);           // move.w d0,EntT_CurrentAngle_w(a0)
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);                   // move.l Lvl_ObjectPointsPtr_l,a1
        d0 = Mem.w(a0);                                      // move.w (a0),d0
        Mem.wl(a1 + (short) d0 * 8, Mem.l(PlayerBss.Plr1_XOff_l));     // move.l Plr1_XOff_l,(a1,d0.w*8)
        Mem.wl(a1 + (short) d0 * 8 + 4, Mem.l(PlayerBss.Plr1_ZOff_l)); // move.l Plr1_ZOff_l,4(a1,d0.w*8)
        a1 = Mem.l(PlayerBss.Plr1_ZonePtr_l);                // move.l Plr1_ZonePtr_l,a1
        d2 = Mem.ub(a0 + Defs.EntT_DamageTaken_b);           // moveq#0,d2 ; move.b EntT_DamageTaken_b(a0),d2
        if (d2 != 0) {                                       // beq .notbeenshot2
            int d3 = Mem.w(a0 + Defs.EntT_ImpactX_w);        // move.w EntT_ImpactX_w(a0),d3
            Mem.ww(PlayerBss.Plr1_SnapXSpdVal_l, Mem.w(PlayerBss.Plr1_SnapXSpdVal_l) + d3); // add.w d3,Plr1_SnapXSpdVal_l
            d3 = Mem.w(a0 + Defs.EntT_ImpactZ_w);            // move.w EntT_ImpactZ_w(a0),d3
            Mem.ww(PlayerBss.Plr1_SnapZSpdVal_l, Mem.w(PlayerBss.Plr1_SnapZSpdVal_l) + d3); // add.w d3,Plr1_SnapZSpdVal_l
            Mem.ww(PlayerBss.Plr1_Health_w, Mem.w(PlayerBss.Plr1_Health_w) - d2); // sub.w d2,Plr1_Health_w
        }
        // .notbeenshot2:
        Mem.wb(a0 + Defs.EntT_DamageTaken_b, 0);             // move.b #0,EntT_DamageTaken_b(a0)
        Mem.wb(a0 + Defs.EntT_HitPoints_b, 10);              // move.b #10,EntT_HitPoints_b(a0)
        Mem.wb(a0 + Defs.ShotT_InUpperZone_b, Mem.b(PlayerBss.Plr1_StoodInTop_b)); // move.b Plr1_StoodInTop_b,ShotT_InUpperZone_b(a0)
        Mem.ww(a0 + Defs.ObjT_ZoneID_w, Mem.w(a1));          // move.w (a1),ObjT_ZoneID_w(a0)
        d2 = Mem.w(a1);                                      // move.w (a1),d2
        a1 = Zone_BrightTable_vl;                            // move.l #Zone_BrightTable_vl,a1
        d2 = Mem.l(a1 + (short) d2 * 4);                     // move.l (a1,d2.w*4),d2
        if (Mem.b(PlayerBss.Plr1_StoodInTop_b) == 0) {       // tst.b Plr1_StoodInTop_b ; bne .okinbott2
            d2 = swap(d2);                                   // swap d2
        }
        // .okinbott2:
        Mem.ww(a0 + 2, d2);                                  // move.w d2,2(a0)
        d0 = Mem.l(PlayerBss.Plr1_TmpYOff_l);                // move.l Plr1_TmpYOff_l,d0
        d1 = Mem.l(PlayerBss.plr1_TmpHeight_l);              // move.l plr1_TmpHeight_l,d1
        d1 = d1 >> 1;                                        // asr.l #1,d1
        d0 = d0 + d1;                                        // add.l d1,d0
        d0 = d0 >> 7;                                        // asr.l #7,d0
        Mem.ww(a0 + 4, d0);                                  // move.w d0,4(a0)
        d0 = Newaliencontrol.ViewpointToDraw(a0);            // jsr ViewpointToDraw
        d0 = d0 + d0;                                        // add.l d0,d0
        plr_Use_AnimSetup(a0, d0, Defs.GLFT_Player1Graphic_w); // bloc GFX/anim (Player1Graphic)
        if (!((short) Mem.w(PlayerBss.Plr1_Health_w) > 0)) { // tst.w Plr1_Health_w ; bgt .okh
            ab3d2.Macros.FREE_OBJ(a0);                       // FREE_OBJ a0
        }
        // .okh: ====== arme en main de J2 (entité ENT_NEXT) ======
        a0 = Mem.l(PlayerBss.Plr2_ObjectPtr_l);              // move.l Plr2_ObjectPtr_l,a0
        if (!((short) Mem.w(PlayerBss.Plr2_Health_w) > 0)) { // tst.w Plr2_Health_w ; bgt .notdead
            ab3d2.Macros.FREE_OBJ_2(a0, Defs.ENT_NEXT);      // FREE_OBJ_2 a0,ENT_NEXT
            return;                                          // rts
        }
        // .notdead:
        a1 = Mem.l(PlayerBss.Plr2_ZonePtr_l);                // move.l Plr2_ZonePtr_l,a1
        d0 = Mem.w(a0 + Defs.EntT_CurrentAngle_w);           // move.w EntT_CurrentAngle_w(a0),d0
        d0 = setw(d0, d0 + SINE_SIZE);                       // add.w #SINE_SIZE,d0
        d0 = ab3d2.data.TablesData.AMOD_A(d0);              // AMOD_A d0
        Mem.ww(a0 + Defs.EntT_CurrentAngle_w + Defs.ENT_NEXT, d0); // move.w d0,EntT_CurrentAngle_w+ENT_NEXT(a0)
        Mem.ww(a0 + Defs.ObjT_ZoneID_w + Defs.ENT_NEXT, Mem.w(a1)); // move.w (a1),ObjT_ZoneID_w+ENT_NEXT(a0)
        Mem.ww(a0 + Defs.EntT_ZoneID_w + Defs.ENT_NEXT, Mem.w(a1)); // move.w (a1),EntT_ZoneID_w+ENT_NEXT(a0)
        d0 = Mem.ub(PlayerBss.Plr2_TmpGunSelected_b);        // moveq#0,d0 ; move.b Plr2_TmpGunSelected_b,d0
        a1 = Mem.l(GLF_DatabasePtr_l) + Defs.GLFT_GunObjects_l; // GLF_DatabasePtr + GLFT_GunObjects_l
        d0 = Mem.w(a1 + (short) d0 * 2);                     // move.w (a1,d0.w*2),d0
        Mem.wb(a0 + Defs.EntT_Type_b + Defs.ENT_NEXT, d0);   // move.b d0,EntT_Type_b+ENT_NEXT(a0)
        Mem.wb(a0 + Defs.ObjT_TypeID_b + Defs.ENT_NEXT, Defs.OBJ_TYPE_OBJECT); // move.b #OBJ_TYPE_OBJECT,...
        d0 = Mem.w(a0);                                      // move.w (a0),d0
        d1 = Mem.w(a0 + Defs.ENT_NEXT);                      // move.w ENT_NEXT(a0),d1
        a1 = Mem.l(Lvl_ObjectPointsPtr_l);                   // move.l Lvl_ObjectPointsPtr_l,a1
        Mem.wl(a1 + (short) d1 * 8, Mem.l(a1 + (short) d0 * 8));         // move.l (a1,d0.w*8),(a1,d1.w*8)
        Mem.wl(a1 + (short) d1 * 8 + 4, Mem.l(a1 + (short) d0 * 8 + 4)); // move.l 4(a1,d0.w*8),4(a1,d1.w*8)
        Mem.wb(a0 + Defs.EntT_WhichAnim_b + Defs.ENT_NEXT, 0xFF); // st EntT_WhichAnim_b+ENT_NEXT(a0)
        d0 = Mem.l(PlayerBss.Plr2_TmpYOff_l);                // move.l Plr2_TmpYOff_l,d0
        d1 = Mem.l(PlayerBss.plr2_TmpHeight_l);              // move.l plr2_TmpHeight_l,d1
        d1 = d1 >> 2;                                        // asr.l #2,d1
        d1 = d1 + 10 * 128;                                  // add.l #10*128,d1
        d0 = d0 + d1;                                        // add.l d1,d0
        d0 = d0 >> 7;                                        // asr.l #7,d0
        Mem.ww(a0 + 4 + Defs.ENT_NEXT, d0);                  // move.w d0,4+ENT_NEXT(a0)
        d1 = Mem.l(PlayerBss.plr2_BobbleY_l);                // move.l plr2_BobbleY_l,d1
        d1 = d1 >> 8;                                        // asr.l #8,d1
        d0 = d1;                                             // move.l d1,d0
        d0 = d0 >> 1;                                        // asr.l #1,d0
        d1 = d1 + d0;                                        // add.l d0,d1
        Mem.ww(a0 + 4 + Defs.ENT_NEXT, Mem.w(a0 + 4 + Defs.ENT_NEXT) + d1); // add.w d1,4+ENT_NEXT(a0)
        Mem.wb(a0 + Defs.ShotT_InUpperZone_b + Defs.ENT_NEXT, Mem.b(a0 + Defs.ShotT_InUpperZone_b)); // move.b ...
        // rts
    }

    /**
     * DOALLANIMS (hires.s 6012-6200) — avance les animations d'aliens « inquiets »
     * (ShotT_Worry) une frame sur 5 (diviseur thistime). Pour chaque objet vivant de
     * type ALIEN, lit la table d'anim GLF (GLFT_AlienAnims) selon WhichAnim/Timer2,
     * déclenche les sons (MakeSomeNoise), gère les actions/spéciaux et écrit l'état
     * d'anim dans ObjectWorkspace (lu par Objdrawhires).
     */
    private static void DOALLANIMS() {
        Mem.wb(HiresData.thistime, Mem.ub(HiresData.thistime) - 1); // subq.b #1,thistime
        if ((byte) Mem.b(HiresData.thistime) > 0) {                 // ble.s .okdosome
            return;                                                 // rts
        }
        Mem.wb(HiresData.thistime, 5);                              // move.b #5,thistime
        int a5 = ab3d2.bss.TablesBss.ObjectWorkspace_vl;            // move.l #ObjectWorkspace_vl,a5
        int a0 = Mem.l(ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l);     // move.l Lvl_ObjectDataPtr_l,a0

        // Objectloop2:
        while ((short) Mem.w(a0) >= 0) {                            // tst.w (a0) ; blt doneallobj2
            int d0 = Mem.w(a0 + Defs.ObjT_ZoneID_w);               // move.w ObjT_ZoneID_w(a0),d0
            if (d0 >= 0) {                                          // blt doneobj2
                Mem.ww(a0 + Defs.EntT_ZoneID_w, d0);               // move.w d0,EntT_ZoneID_w(a0)
                if (Mem.b(a0 + Defs.ShotT_Worry_b) != 0) {         // tst.b ShotT_Worry_b(a0) ; beq doneobj2
                    if ((byte) Mem.ub(a0 + Defs.ObjT_TypeID_b) < 1) { // cmp.b #1,d0 ; blt JUMPALIENANIM
                        doAlienAnim(a0, a5);                       // (type>=1 : objet/bullet anim non portée -> doneobj2)
                    }
                }
            }
            // doneobj2:
            a0 += Defs.ENT_NEXT;                                   // adda.w #ENT_NEXT,a0
            a5 += 8;                                               // addq #8,a5
        }
        // doneallobj2: rts
    }

    /** JUMPALIENANIM (hires.s 6047-6196) — anim d'un alien. */
    private static void doAlienAnim(int a0, int a5) {
        int d0 = Mem.ub(a0 + Defs.EntT_WhichAnim_b);               // 0=walk 1=attack 2=gethit 3=dying
        if ((byte) d0 > 3) return;                                 // (>3 -> bra doneobj2)
        switch (d0) {                                              // cmp/beq dispatch
            case 1: d0 = 8;  break;                                // ALATTACK
            case 2: d0 = 9;  break;                                // ALGETHIT
            case 3: d0 = 10; break;                                // ALDIE
            default: d0 = 0; break;                                // ALWALK (0)
        }
        // intowalk / NOSIDES2:
        Mem.wb(a5 + 2, d0);                                        // move.b d0,2(a5)
        int a6 = Mem.l(HiresData.GLF_DatabasePtr_l) + Defs.GLFT_AlienAnims_l; // GLF + AlienAnims
        int type = Mem.ub(a0 + Defs.EntT_Type_b);                 // EntT_Type_b
        a6 += Defs.A_AnimLen * type;                              // .valtables+4 = A_AnimLen*type
        a6 += Defs.A_OptLen * d0;                                 // .valtables+2 = A_OptLen*animindex
        int d1 = Mem.uw(a0 + Defs.EntT_Timer2_w);                // move.w EntT_Timer2_w(a0),d1
        int d2 = d1;                                              // move.w d1,d2
        int frame = Defs.A_FrameLen * d1;                        // .valtables = A_FrameLen*timer2

        d0 = Mem.ub(a6 + 5 + frame);                              // move.b 5(a6,d1.w),d0 ; son
        if (d0 != 0) {                                            // beq .nosoundmake
            Mem.ww(Aud_SampleNum_w, d0 - 1);                     // subq #1,d0 ; move.w d0,Aud_SampleNum_w
            Mem.wb(notifplaying, 0);                             // clr.b notifplaying
            Mem.ww(IDNUM, Mem.uw(a0));                           // move.w (a0),IDNUM
            Mem.ww(Aud_NoiseVol_w, 80);                          // move.w #80,Aud_NoiseVol_w
            int a1 = ObjRotated_vl + Mem.uw(a0) * 8;             // lea (ObjRotated_vl,d0.w*8),a1
            Mem.wl(Aud_NoiseX_w, Mem.l(a1));                     // move.l (a1),Aud_NoiseX_w
            MakeSomeNoise();                                     // jsr MakeSomeNoise
        }
        // .nosoundmake:
        d0 = Mem.ub(a6 + 6 + frame);                             // move.b 6(a6,d1.w),d0 ; action
        if (d0 != 0) {                                           // beq .noaction
            Mem.wb(a5, Mem.ub(a5) + 1);                          // add.b #1,(a5)
            Mem.wb(a5 + 1, d2);                                 // move.b d2,1(a5)
        }
        // .noaction:
        d2 = (d2 + 1) & 0xFFFF;                                  // addq #1,d2
        d0 = Mem.ub(a6 + 7 + frame);                             // move.b 7(a6,d1.w),d0 ; spécial
        if (d0 != 0) {                                           // beq .nospecial
            // .special:
            int d3 = d0 & 63;                                   // and.w #63,d3
            d0 = (d0 & 0xFFFF) >>> 6;                           // lsr.w #6,d0
            boolean storeval;
            if (d0 < 2) {                                       // blt .storeval
                storeval = true;
            } else if (d0 == 2) {                               // beq .randval
                int rnd = Objectmove.GetRand();                // jsr GetRand
                int rem = rnd % d3;                            // divs d3,d0 ; swap d0 -> reste
                d3 = rem & 0xFFFF;                             // move.w d0,d3
                storeval = true;
            } else {                                            // d0==3
                Mem.wb(a5 + 4, Mem.ub(a5 + 4) - 1);            // sub.b #1,4(a5)
                if ((Mem.ub(a5 + 4) & 0xFF) == 0) {            // beq.s .nospecial
                    storeval = false;
                } else {
                    d2 = d3 & 0xFFFF;                          // move.w d3,d2
                    storeval = false;
                }
            }
            if (storeval) {                                     // .storeval:
                Mem.wb(a5 + 4, d3);                            // move.b d3,4(a5)
            }
        }
        // .nospecial:
        int d3f = Defs.A_FrameLen * d2;                         // .valtables2 = A_FrameLen*d2
        if ((byte) Mem.b(a6 + d3f) < 0) {                       // tst.b (a6,d3.w) ; bge .noendanim
            Mem.wb(a5 + 3, 0xFF);                              // st 3(a5)
            d2 = 0;                                            // move.w #0,d2
        }
        // .noendanim:
        Mem.ww(a0 + Defs.EntT_Timer2_w, d2);                   // move.w d2,EntT_Timer2_w(a0)
        // bra doneobj2
    }

    /** Dégâts de sol/liquide toxique pour un joueur (hires.s 6217-6266, factorisé J1/J2). */
    private static void floorDamage(int zonePtrL, int stoodTopB, int snapYOffL, int snapTYOffL, int objPtrL) {
        int a0 = Mem.l(zonePtrL);
        int d2 = Mem.l(a0 + Defs.ZoneT_Water_l);                // water depth
        int d0 = Mem.w(a0 + Defs.ZoneT_FloorNoise_w);
        if (Mem.b(stoodTopB) != 0) {                            // tst.b StoodInTop ; beq .okinbot
            d0 = Mem.w(a0 + Defs.ZoneT_UpperFloorNoise_w);
        }
        boolean apply;
        if (d2 < Mem.l(snapYOffL)) {                            // cmp.l SnapYOff,d2 ; blt .in_toxic_liquid
            apply = true;
        } else {
            apply = Mem.l(snapTYOffL) <= Mem.l(snapYOffL);      // cmp.l SnapYOff,SnapTYOff ; bgt .not_on_floor
        }
        if (apply) {                                            // .in_toxic_liquid:
            int fa = Mem.l(HiresData.GLF_DatabasePtr_l) + Defs.GLFT_FloorData_l;
            d0 = Mem.w(fa + (d0 & 0xFFFF) * 4);                // move.w (a0,d0.w*4),d0 ; floor damage (MSW)
            int obj = Mem.l(objPtrL);
            Mem.wb(obj + Defs.EntT_DamageTaken_b, Mem.ub(obj + Defs.EntT_DamageTaken_b) + d0); // add.b d0,EntT_DamageTaken_b
        }
    }

    /**
     * dosomething (hires.s 6205-6831) — corps de MAJ de jeu de la VBL : anim aliens
     * (DOALLANIMS), dégâts de sol, raccourcis clavier d'options (F3 son / F4 lumière /
     * F6 qualité / TAB carte / pavé num. scroll), tick musique (mt_music), puis
     * dispatch ENTRÉE+PHYSIQUE joueur (Plr1/2_MouseControl/KeyboardControl/Joystick +
     * Plr_Fall + friction), enfin l'audio Paula. Tombe dans JUSTSOUNDS.
     */
    private static void dosomething() {
        Mem.ww(ab3d2.bss.AnimBss.Anim_FramesToDraw_w, Mem.w(ab3d2.bss.AnimBss.Anim_FramesToDraw_w) + 1); // addq.w #1,Anim_FramesToDraw_w
        DOALLANIMS();                                           // bsr DOALLANIMS

        Mem.ww(HiresData.timetodamage, Mem.w(HiresData.timetodamage) - 1); // sub.w #1,timetodamage
        if ((short) Mem.w(HiresData.timetodamage) <= 0) {       // bgt .skip_damage
            Mem.ww(HiresData.timetodamage, 100);               // move.w #100,timetodamage
            floorDamage(ab3d2.bss.PlayerBss.Plr1_ZonePtr_l, ab3d2.bss.PlayerBss.Plr1_StoodInTop_b,
                        ab3d2.bss.PlayerBss.Plr1_SnapYOff_l, ab3d2.bss.PlayerBss.Plr1_SnapTYOff_l,
                        ab3d2.bss.PlayerBss.Plr1_ObjectPtr_l);
            floorDamage(ab3d2.bss.PlayerBss.Plr2_ZonePtr_l, ab3d2.bss.PlayerBss.Plr2_StoodInTop_b,
                        ab3d2.bss.PlayerBss.Plr2_SnapYOff_l, ab3d2.bss.PlayerBss.Plr2_SnapTYOff_l,
                        ab3d2.bss.PlayerBss.Plr2_ObjectPtr_l);
        }
        // .skip_damage:
        int a5 = KeyMap_vb;                                     // move.l #KeyMap_vb,a5

        // --- F3 : bascule son (stéréo / 4-canaux) ---
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_F3) != 0) {          // tst.b RAWKEY_F3(a5) ; beq notogglesound
            if (Mem.b(HiresData.lasttogsound) == 0) {           // tst.b lasttogsound ; bne notogglesound2
                Mem.wb(HiresData.lasttogsound, 0xFF);          // st lasttogsound
                int d0 = (Mem.w(ab3d2.Pauseopts.TOPPOPT) + 1) & 3; // move.w TOPPOPT ; addq #1 ; and #3
                Mem.ww(ab3d2.Pauseopts.TOPPOPT, d0);
                Mem.wb(Aud_Stereo_b, Mem.ub(HiresData.STEROPT + d0 * 2)); // STEROPT(pc,d0*2)
                int d1 = Mem.ub(HiresData.STEROPT + d0 * 2 + 1); // STEROPT+1(pc,d0*2)
                int aMsg = ab3d2.data.TextData.Game_SoundOptionsText_vb + d0 * Defs.OPTS_MESSAGE_LENGTH;
                ab3d2.c.Message.Msg_PushLine(aMsg, Defs.OPTS_MESSAGE_LENGTH | Defs.MSG_TAG_OPTIONS);
                Mem.wb(ab3d2.ControlloopData.Prefsfile + 1, d1); // move.b d1,Prefsfile+1
                // pastster:
                int chv = (d1 == '4') ? 0xFF : 0;              // cmp.b #'4',d1 ; seq ...
                Mem.wb(CHANNELDATA + 8, chv);
                Mem.wb(CHANNELDATA + 12, chv);
                Mem.wb(CHANNELDATA + 24, chv);
                Mem.wb(CHANNELDATA + 28, chv);
                Mem.wb(CHANNELDATA + 8, 0xFF);                 // st CHANNELDATA+8 (mt_init)
                Mem.wb(CHANNELDATA, 0xFF);                     // st CHANNELDATA
                CustomChips.write16(0xdff000 + 0x096, 0xf);    // move.w #$f,dmacon
                CustomChips.write32(0xdff0a0, Aud_Null1_vw); CustomChips.write16(0xdff0a4, 100); CustomChips.write16(0xdff0a6, 443); CustomChips.write16(0xdff0a8, 63);
                CustomChips.write32(0xdff0b0, Aud_Null2_vw); CustomChips.write16(0xdff0b4, 100); CustomChips.write16(0xdff0b6, 443); CustomChips.write16(0xdff0b8, 63);
                CustomChips.write32(0xdff0c0, Aud_Null4_vw); CustomChips.write16(0xdff0c4, 100); CustomChips.write16(0xdff0c6, 443); CustomChips.write16(0xdff0c8, 63);
                CustomChips.write32(0xdff0d0, Aud_Null3_vw); CustomChips.write16(0xdff0d4, 100); CustomChips.write16(0xdff0d6, 443); CustomChips.write16(0xdff0d8, 63);
                Mem.wl(pos0LEFT, Aud_EmptyBuffer_vl);  Mem.wl(pos1LEFT, Aud_EmptyBuffer_vl);  Mem.wl(pos2LEFT, Aud_EmptyBuffer_vl);  Mem.wl(pos3LEFT, Aud_EmptyBuffer_vl);
                Mem.wl(pos0RIGHT, Aud_EmptyBuffer_vl); Mem.wl(pos1RIGHT, Aud_EmptyBuffer_vl); Mem.wl(pos2RIGHT, Aud_EmptyBuffer_vl); Mem.wl(pos3RIGHT, Aud_EmptyBuffer_vl);
                Mem.wl(Samp0endLEFT, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd);  Mem.wl(Samp1endLEFT, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd);  Mem.wl(Samp2endLEFT, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd);  Mem.wl(Samp3endLEFT, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd);
                Mem.wl(Samp0endRIGHT, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd); Mem.wl(Samp1endRIGHT, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd); Mem.wl(Samp2endRIGHT, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd); Mem.wl(Samp3endRIGHT, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd);
                // move.w #10,d3 ; (sériel, ignoré) ; bra notogglesound2
            }
        } else {
            Mem.wb(HiresData.lasttogsound, 0);                 // notogglesound: clr.b lasttogsound
        }

        // --- F4 : bascule éclairage ---
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_F4) != 0) {          // tst.b RAWKEY_F4(a5) ; beq nolighttoggle
            if (Mem.b(HiresData.OLDLTOG) == 0) {                // tst.b OLDLTOG ; bne nolighttoggle2
                Mem.wb(HiresData.OLDLTOG, 0xFF);               // st OLDLTOG
                int d0 = ab3d2.data.TextData.Game_LightingOptionsText_vb;
                Mem.wb(ab3d2.NewanimsData.Anim_LightingEnabled_b, ~Mem.b(ab3d2.NewanimsData.Anim_LightingEnabled_b)); // not.b
                if (Mem.b(ab3d2.NewanimsData.Anim_LightingEnabled_b) != 0) { // beq .noon
                    d0 += Defs.OPTS_MESSAGE_LENGTH;            // add.l #OPTS_MESSAGE_LENGTH,d0
                }
                // pastlighttext:
                ab3d2.c.Message.Msg_PushLine(d0, Defs.OPTS_MESSAGE_LENGTH | Defs.MSG_TAG_OPTIONS);
            }
        } else {
            Mem.wb(HiresData.OLDLTOG, 0);                      // nolighttoggle: clr.b OLDLTOG
        }

        // nolighttoggle2:
        if (Mem.b(draw_RenderMap_b) == 0) {                     // tst.b draw_RenderMap_b ; bne .no_vid_adjust
            ab3d2.modules.Vid.Vid_CheckSettingsAdjust(a5);     // bsr Vid_CheckSettingsAdjust
        }

        // --- F6 : bascule qualité de rendu ---
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_F6) != 0) {          // tst.b RAWKEY_F6(a5) ; beq .nogood
            if (Mem.b(HiresData.OLDGOOD) == 0) {                // tst.b OLDGOOD ; bne .nogood2
                Mem.wb(HiresData.OLDGOOD, 0xFF);               // st OLDGOOD
                int d0 = ab3d2.data.TextData.Game_DrawHighQualityText_vb;
                Mem.wb(ab3d2.HireswallData.Draw_GoodRender_b, ~Mem.b(ab3d2.HireswallData.Draw_GoodRender_b)); // not.b
                if (Mem.b(ab3d2.HireswallData.Draw_GoodRender_b) == 0) { // bne .okgood
                    d0 = ab3d2.data.TextData.Game_DrawLowQualityText_vb;
                }
                ab3d2.c.Message.Msg_PushLine(d0, Defs.OPTS_MESSAGE_LENGTH | Defs.MSG_TAG_OPTIONS);
            }
        } else {
            Mem.wb(HiresData.OLDGOOD, 0);                      // .nogood: clr.b OLDGOOD
        }

        // --- TAB : bascule carte ---
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_TAB) != 0) {         // .tabprsd
            if (Mem.b(HiresData.tabheld) == 0) {                // tst.b tabheld ; bne .noswitch
                Mem.wb(MAPON, ~Mem.b(MAPON));                  // not.b MAPON
                Mem.wb(HiresData.tabheld, 0xFF);               // st tabheld
            }
        } else {
            Mem.wb(HiresData.tabheld, 0);                      // clr.b tabheld
        }

        // --- défilement carte (pavé numérique) ---
        loop_MapScroll(a5);

        // .nomapcentre / justshake:
        ab3d2.modules.Music.mt_music();                         // jsr mt_music
        // (bloc d'affichage du timer 6510-6584 = code mort, sauté par bra dontshowtime)

        // dontshowtime: cycle d'anim "alan"
        int ap = Mem.l(HiresData.alanptr);                      // move.l alanptr,a0
        Mem.wl(HiresData.alframe, Mem.l(ap));                  // move.l (a0)+,alframe
        ap += 4;
        int endalan = HiresData.alan + 32 * 4;                 // cmp.l #endalan,a0
        if (ap >= endalan) ap = HiresData.alan;                // blt nostartalan ; move.l #alan,a0
        Mem.wl(HiresData.alanptr, ap);                         // move.l a0,alanptr

        // --- ENTRÉE / PHYSIQUE joueur ---
        if (Mem.b(READCONTROLS) != 0) {                         // tst.b READCONTROLS ; beq nocontrols
            if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) { // cmp PLR_SLAVE ; beq control2
                doPlayer2Controls();
            } else {
                doPlayer1Controls();
            }
        }

        // nocontrols: muckabout audio (dosounds gated) — host : dosounds=0
        if (Mem.b(dosounds) != 0 && Mem.b(ab3d2.ControlloopData.Prefsfile + 1) == '4') {
            int d0 = 0;
            if (Mem.b(NoiseMade0LEFT) != 0) d0 = 1;            // seq logic
            if (Mem.b(NoiseMade0RIGHT) != 0) d0 |= 2;
            if (Mem.b(NoiseMade1RIGHT) != 0) d0 |= 4;
            if (Mem.b(NoiseMade1LEFT) != 0) d0 |= 8;
            d0 &= 0xfffe;                                       // and.w #$fffe,d0
            CustomChips.write16(0xdff000 + 0x096, d0);         // move.w d0,dmacon
        }
        // firenownotpressed2 / firenotpressed2 / dointer : labels vides
        JUSTSOUNDS();                                            // tombe dans JUSTSOUNDS
    }

    /** Bloc joueur 1 (hires.s 6601-6689) : mort -> chute+friction, sinon dispatch entrée. */
    private static void doPlayer1Controls() {
        if ((short) Mem.w(Plr1_Health_w) > 0) {                 // tst.w Plr1_Health_w ; bgt .propercontrol
            if (Mem.b(Plr1_Mouse_b) != 0)    Plr1control.Plr1_MouseControl();
            if (Mem.b(Plr1_Keys_b) != 0)     Plr1control.Plr1_KeyboardControl();
            if (Mem.b(Plr1_Joystick_b) != 0) Plr1control.Plr1_JoystickControl();
            return;
        }
        // joueur 1 mort : chute + friction
        Mem.wl(hitcol, 7 * 2116);                               // move.l #7*2116,hitcol
        ab3d2.Macros.FREE_OBJ_2(Mem.l(Plr1_ObjectPtr_l), Defs.ENT_NEXT_2);
        Mem.wb(Plr1_Fire_b, 0); Mem.wb(Plr1_Clicked_b, 0);
        Mem.ww(Plr_AddToBobble_w, 0);
        Mem.wl(Plr1_SnapHeight_l, PLR_CROUCH_HEIGHT);
        deadViewSetup();
        Plr1control.Plr1_Fall();                                // jsr Plr1_Fall
        applyDeadFriction(Plr1_SnapXSpdVal_l, Plr1_SnapZSpdVal_l, Plr1_SnapXOff_l, Plr1_SnapZOff_l,
                          Plr1_SnapAngSpd_w, Plr1_SnapAngPos_w);
    }

    /** Bloc joueur 2 (hires.s 6691-6787). */
    private static void doPlayer2Controls() {
        if ((short) Mem.w(Plr2_Health_w) > 0) {                 // tst.w Plr2_Health_w ; bgt .propercontrol
            if (Mem.b(Plr2_Mouse_b) != 0)    Plr2control.Plr2_MouseControl();
            if (Mem.b(Plr2_Keys_b) != 0)     Plr2control.Plr2_KeyboardControl();
            if (Mem.b(Plr2_Joystick_b) != 0) Plr2control.Plr2_JoystickControl();
            return;
        }
        Mem.wl(hitcol, 7 * 2116);
        ab3d2.Macros.FREE_OBJ_2(Mem.l(Plr1_ObjectPtr_l), Defs.ENT_NEXT_2); // (l'ASM libère Plr1_ObjectPtr ici aussi)
        Mem.wb(Plr2_Fire_b, 0);
        Mem.ww(Plr_AddToBobble_w, 0);
        Mem.wl(Plr2_SnapHeight_l, PLR_CROUCH_HEIGHT);
        deadViewSetup();
        Plr2control.Plr2_Fall();
        applyDeadFriction(Plr2_SnapXSpdVal_l, Plr2_SnapZSpdVal_l, Plr2_SnapXOff_l, Plr2_SnapZOff_l,
                          Plr2_SnapAngSpd_w, Plr2_SnapAngPos_w);
    }

    /** Réglage vue pour joueur mort (View_LookMax -> STOPOFFSET/SMIDDLEY/SBIGMIDDLEY). */
    private static void deadViewSetup() {
        int d0 = Mem.w(View_LookMax_w);                         // move.w View_LookMax_w,d0
        Mem.ww(STOPOFFSET, d0);                                 // move.w d0,STOPOFFSET
        d0 = -d0;                                               // neg.w d0
        d0 = (short) (d0 + Mem.w(TOTHEMIDDLE));                 // add.w TOTHEMIDDLE,d0
        Mem.ww(SMIDDLEY, d0);                                   // move.w d0,SMIDDLEY
        Mem.wl(SBIGMIDDLEY, d0 * SCREEN_WIDTH);                 // muls #SCREEN_WIDTH,d0 ; move.l d0,SBIGMIDDLEY
    }

    /** Friction joueur mort (spd *= ~7/8 vers 0, angle *= ~3/4). */
    private static void applyDeadFriction(int xSpd, int zSpd, int xOff, int zOff, int angSpd, int angPos) {
        int d6 = Mem.l(xSpd);
        int d7 = Mem.l(zSpd);
        if (Mem.b(Plr_Decelerate_b) != 0) {                     // tst.b Plr_Decelerate_b ; beq .skip_friction
            d6 = -d6; d6 = (d6 > 0) ? (d6 >> 3) + 1 : (d6 >> 3); // neg ; ble .nobug : asr#3 ; sinon asr#3+1
            d7 = -d7; d7 = (d7 > 0) ? (d7 >> 3) + 1 : (d7 >> 3);
            Mem.wl(xSpd, Mem.l(xSpd) + d6);
            Mem.wl(zSpd, Mem.l(zSpd) + d7);
        }
        // .skip_friction:
        d6 = Mem.l(xSpd); d7 = Mem.l(zSpd);
        Mem.wl(xOff, Mem.l(xOff) + d6);                         // add.l d6,SnapXOff
        Mem.wl(zOff, Mem.l(zOff) + d7);
        int d3 = Mem.w(angSpd);                                 // move.w SnapAngSpd,d3
        if (Mem.b(Plr_Decelerate_b) != 0) {                     // beq .nofric
            d3 = d3 >> 2;                                       // asr.w #2,d3
            if (d3 < 0) d3 += 1;                                // bge .nneg ; addq #1,d3
        }
        Mem.ww(angSpd, d3);                                     // move.w d3,SnapAngSpd
        int ap = Mem.w(angPos) + d3 + d3;                       // add.w d3 ×2,SnapAngPos
        Mem.ww(angPos, ab3d2.data.TablesData.AMOD_A(ap));      // AMOD_A SnapAngPos
    }

    /** Défilement de la carte par le pavé numérique (hires.s 6441-6503). */
    private static void loop_MapScroll(int a5) {
        boolean up    = Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_8) != 0;
        boolean down  = Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_2) != 0;
        boolean left  = Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_4) != 0;
        boolean right = Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_6) != 0;
        boolean ul = Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_7) != 0;
        boolean ur = Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_9) != 0;
        boolean dl = Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_1) != 0;
        boolean dr = Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_3) != 0;
        boolean d0 = up || ul || ur;            // scroll up
        boolean d1 = down || dl || dr;          // scroll down
        boolean d2 = left || ul || dl;          // scroll left
        boolean d3 = right || dr || ur;         // scroll right
        int zoom = Mem.w(ab3d2.modules.draw.DrawMap.Draw_MapZoomLevel_w) + 2; // Draw_MapZoomLevel_w+2
        int step = 1 << (zoom & 31);            // bset d4,d5
        if (d0) Mem.ww(ab3d2.modules.draw.DrawMap.draw_MapZOffset_w, Mem.w(ab3d2.modules.draw.DrawMap.draw_MapZOffset_w) - step);
        if (d1) Mem.ww(ab3d2.modules.draw.DrawMap.draw_MapZOffset_w, Mem.w(ab3d2.modules.draw.DrawMap.draw_MapZOffset_w) + step);
        if (d2) Mem.ww(ab3d2.modules.draw.DrawMap.draw_MapXOffset_w, Mem.w(ab3d2.modules.draw.DrawMap.draw_MapXOffset_w) + step);
        if (d3) Mem.ww(ab3d2.modules.draw.DrawMap.draw_MapXOffset_w, Mem.w(ab3d2.modules.draw.DrawMap.draw_MapXOffset_w) - step);
        if (Mem.b(a5 + RawKeyMacros.RAWKEY_NUM_5) != 0) {       // centre
            Mem.ww(ab3d2.modules.draw.DrawMap.draw_MapXOffset_w, 0);
            Mem.ww(ab3d2.modules.draw.DrawMap.draw_MapZOffset_w, 0);
        }
    }

    // Sorties de mixPaulaChan (positions finales des deux flux mixés).
    private static int mpcA0, mpcA1;

    /**
     * Mixe deux voies logicielles dans un buffer de sortie Paula (200 octets = 50 longs).
     * Échange double-buffer (Aupt/Auback) + LC Paula, échelle la voie la plus faible via
     * la table de volume `tab`, écrit le volume Paula (= voie la plus forte), additionne
     * (add.l : 4 échantillons par long, avec propagation de retenue — artefact d'origine).
     */
    private static void mixPaulaChan(int posA, int posB, int volAsym, int volBsym,
                                     int paulaVolReg, int aubufFrontSym, int aubufBackSym, int paulaLCReg) {
        int a3 = Mem.l(aubufFrontSym);                        // move.l Aupt,a3
        CustomChips.write32(paulaLCReg, a3);                  // move.l a3,$dffXX0
        Mem.wl(aubufFrontSym, Mem.l(aubufBackSym));           // move.l Auback,Aupt
        Mem.wl(aubufBackSym, a3);                             // move.l a3,Auback

        int a0 = posA, a1 = posB, a2 = HiresData.tab;
        int d0v = Mem.ub(volAsym), d1v = Mem.ub(volBsym);     // move.b volX (octet)
        boolean swapped = d0v < d1v;                          // slt swappedem ; bge fbig
        if (!swapped) {                                       // fbig : volA >= volB
            if (d0v != 0) {                                   // tst d0 ; beq donechan (saute le réglage)
                a2 += ((d1v << 6) / d0v) << 8;                // asl#6 ; divs d0,d1 ; lsl#8 ; adda
                CustomChips.write16(paulaVolReg, d0v);
            }
        } else {                                              // exg a0,a1 ; échelle volA par volB
            int t = a0; a0 = a1; a1 = t;
            a2 += ((d0v << 6) / d1v) << 8;
            CustomChips.write16(paulaVolReg, d1v);
        }
        for (int i = 0; i < 50; i++) {                        // 50 longs = 200 échantillons
            int dd = Mem.l(a0); a0 += 4;                      // move.l (a0)+,d0 (4 octets voie forte)
            int b1 = Mem.ub(a1++), b2 = Mem.ub(a1++), b3 = Mem.ub(a1++), b4 = Mem.ub(a1++);
            int d5 = (Mem.ub(a2 + b1) << 24) | (Mem.ub(a2 + b2) << 16)
                   | (Mem.ub(a2 + b3) << 8) | Mem.ub(a2 + b4); // 4 octets voie faible échelonnés via tab
            Mem.wl(a3, dd + d5); a3 += 4;                     // add.l d5,d0 ; move.l d0,(a3)+
        }
        if (swapped) { int t = a0; a0 = a1; a1 = t; }         // tst swappedem ; exg back
        mpcA0 = a0; mpcA1 = a1;
    }

    /** Fin de sample atteinte → repointe sur le buffer vide + coupe le volume/CHANDATA. */
    private static int wrapSampEnd(int pos, int sampEndSym, int volSym, int chanData32, int chanData2) {
        if (pos >= Mem.l(sampEndSym)) {                       // cmp.l Samp,a ; blt .notoff
            pos = ab3d2.bss.TablesBss.Aud_EmptyBuffer_vl;
            Mem.wl(sampEndSym, ab3d2.bss.TablesBss.Aud_EmptyBufferEnd);
            Mem.wb(volSym, 0);                                // move.b #0,vol
            Mem.ww(chanData32, 0);                            // clr.w CHANDATA+32+off
            Mem.ww(chanData2, 0);                             // move.w #0,CHANDATA+2+off
        }
        return pos;
    }

    /**
     * newsampbitl (hires.s 6861-7239) — mixer logiciel stéréo : combine les 8 voies
     * logicielles (0-3 GAUCHE, 0-3 DROITE) en 4 buffers de sortie Paula. Joué par l'hôte
     * (Audio) : ch0=$a0 (G:0L+2L), ch1=$b0 (D:0R+2R), ch3=$d0 (G:1L+3L), ch2=$c0 (D:1R+3R).
     */
    public static int dbgMaxSoftVol = 0;   // DIAG : max des 8 volumes de voies soft vus
    public static int dbgMaxSfxPaulaVol = 0; // DIAG : max audVOL[1..3] (canaux SFX)

    static void newsampbitl() {
        CustomChips.write16(0xdff09c, 0x200);                 // move.w #$200,intreq (clr IRQ audio, no-op hôte)
        if (dbgTestSfx >= 0 || dbgForceFire) {                // DIAG : trace l'activité SFX
            int m = Math.max(Math.max(Math.max(Mem.ub(vol0left), Mem.ub(vol0right)), Math.max(Mem.ub(vol1left), Mem.ub(vol1right))),
                             Math.max(Math.max(Mem.ub(vol2left), Mem.ub(vol2right)), Math.max(Mem.ub(vol3left), Mem.ub(vol3right))));
            if (m > dbgMaxSoftVol) dbgMaxSoftVol = m;
            int pv = Math.max(ab3d2.host.CustomChips.audVOL[1], Math.max(ab3d2.host.CustomChips.audVOL[2], ab3d2.host.CustomChips.audVOL[3]));
            if (pv > dbgMaxSfxPaulaVol) dbgMaxSfxPaulaVol = pv;
        }

        if (Mem.b(CHANNELDATA) == 0) {                        // tst.b CHANNELDATA ; bne nochannel0
            mixPaulaChan(Mem.l(pos0LEFT), Mem.l(pos2LEFT), vol0left, vol2left, 0xdff0a8, Aupt0, Auback0, 0xdff0a0);
            Mem.wl(pos0LEFT, wrapSampEnd(mpcA0, Samp0endLEFT, vol0left, LEFTCHANDATA + 32, LEFTCHANDATA + 2));
            Mem.wl(pos2LEFT, wrapSampEnd(mpcA1, Samp2endLEFT, vol2left, LEFTCHANDATA + 32 + 8, LEFTCHANDATA + 2 + 8));
        }
        if (Mem.b(CHANNELDATA + 16) == 0) {                   // tst.b CHANNELDATA+16 ; bne nochannel1
            mixPaulaChan(Mem.l(pos0RIGHT), Mem.l(pos2RIGHT), vol0right, vol2right, 0xdff0b8, Aupt1, Auback1, 0xdff0b0);
            Mem.wl(pos0RIGHT, wrapSampEnd(mpcA0, Samp0endRIGHT, vol0right, RIGHTCHANDATA + 32, RIGHTCHANDATA + 2));
            Mem.wl(pos2RIGHT, wrapSampEnd(mpcA1, Samp2endRIGHT, vol2right, RIGHTCHANDATA + 32 + 8, RIGHTCHANDATA + 2 + 8));
        }
        // Other two channels (non gardées)
        mixPaulaChan(Mem.l(pos1LEFT), Mem.l(pos3LEFT), vol1left, vol3left, 0xdff0d8, Aupt2, Auback2, 0xdff0d0);
        Mem.wl(pos1LEFT, wrapSampEnd(mpcA0, Samp1endLEFT, vol1left, LEFTCHANDATA + 32 + 4, LEFTCHANDATA + 2 + 4));
        Mem.wl(pos3LEFT, wrapSampEnd(mpcA1, Samp3endLEFT, vol3left, LEFTCHANDATA + 32 + 12, LEFTCHANDATA + 2 + 12));

        mixPaulaChan(Mem.l(pos1RIGHT), Mem.l(pos3RIGHT), vol1right, vol3right, 0xdff0c8, Aupt3, Auback3, 0xdff0c0);
        Mem.wl(pos1RIGHT, wrapSampEnd(mpcA0, Samp1endRIGHT, vol1right, RIGHTCHANDATA + 32 + 4, RIGHTCHANDATA + 2 + 4));
        Mem.wl(pos3RIGHT, wrapSampEnd(mpcA1, Samp3endRIGHT, vol3right, RIGHTCHANDATA + 32 + 12, RIGHTCHANDATA + 2 + 12));

        CustomChips.write16(0xdff096, 0x820f);                // move.w #$820f,dmacon (active DMA audio)
    }

    /**
     * JUSTSOUNDS (hires.s 6833-6850) — audio Paula par frame. Gardé par dosounds.
     * Sur Amiga, le mixer (newsampbitl) est déclenché par l'IRQ « canal audio vide ».
     * En hôte : pas d'IRQ Paula → c'est le backend Audio (OpenAL) qui TIRE l'audio :
     * tant que sa file de lecture n'est pas pleine, on mixe une trame (newsampbitl) et on
     * la pousse. Cela auto-cadence le mixage au débit de lecture (≈ période Paula).
     */
    private static boolean dbgAudioCrashLogged = false;

    /** Réinitialise les 8 voies logicielles sur le buffer vide (silence) — récupération après crash audio. */
    private static void resetSoftAudioChannels() {
        int e = ab3d2.bss.TablesBss.Aud_EmptyBuffer_vl, ee = ab3d2.bss.TablesBss.Aud_EmptyBufferEnd;
        Mem.wl(pos0LEFT, e); Mem.wl(pos1LEFT, e); Mem.wl(pos2LEFT, e); Mem.wl(pos3LEFT, e);
        Mem.wl(pos0RIGHT, e); Mem.wl(pos1RIGHT, e); Mem.wl(pos2RIGHT, e); Mem.wl(pos3RIGHT, e);
        Mem.wl(Samp0endLEFT, ee); Mem.wl(Samp1endLEFT, ee); Mem.wl(Samp2endLEFT, ee); Mem.wl(Samp3endLEFT, ee);
        Mem.wl(Samp0endRIGHT, ee); Mem.wl(Samp1endRIGHT, ee); Mem.wl(Samp2endRIGHT, ee); Mem.wl(Samp3endRIGHT, ee);
    }

    private static void JUSTSOUNDS() {
        if (Mem.b(dosounds) == 0) return;                     // tst.b dosounds ; beq .notthing
        pumpAudioToHost();
    }

    /**
     * Mixe les registres Paula courants → buffers hôte (newsampbitl) → OpenAL, pour une frame.
     * Indépendant de {@code dosounds} : utilisé par JUSTSOUNDS (jeu) ET par la musique de fin
     * ({@link #playEndMusic}, où dosounds=0 mais la musique doit quand même sortir).
     */
    static void pumpAudioToHost() {
        try {                                                 // filet de sécurité : une exception audio ne doit pas planter le jeu
            if (ab3d2.host.Audio.dbgCapture) {                // DIAG : capture WAV (1 trame/frame, hors OpenAL)
                newsampbitl();
                ab3d2.host.Audio.captureFrame();
                return;
            }
            if (!ab3d2.host.Audio.ensureStarted()) return;    // pas de périphérique audio → silencieux
            int guard = 0;
            while (ab3d2.host.Audio.needsBuffer() && guard++ < 8) { // borne par frame (anti-emballement)
                newsampbitl();                                // produit les 4 buffers Paula (rôle de l'IRQ)
                ab3d2.host.Audio.queueFromPaula();            // lit CustomChips.audLC/VOL/PER → stéréo → OpenAL
            }
        } catch (Throwable t) {                               // récupération : silence ce canal/trame + log unique
            if (!dbgAudioCrashLogged) {
                System.err.println("[audio] exception de mixage capturée (canaux réinitialisés, jeu poursuivi) : " + t);
                dbgAudioCrashLogged = true;
            }
            resetSoftAudioChannels();
        }
    }

    /**
     * VBlankInterrupt (hires.s 5963) — tick par frame (l'hôte n'a pas d'IRQ matérielle ;
     * appelée par game_main_loop). Compteurs + .routine : si Game_Running -> dosomething
     * (anim/physique/entrée/son), sinon JUSTSOUNDS.
     */
    public static int dbgTestSfx = -1;  // DIAG : si >=0, injecte ce sample SFX ~1×/s (test chemin SFX)
    public static boolean dbgForceFire = false; // DIAG : simule le clic gauche (tir) chaque frame
    public static boolean dbgWeaponTest = false; // DIAG : cycle les 10 armes (15 frames chacune) + tir
    public static final int DBG_FRAMES_PER_GUN = 8;
    public static boolean dbgSwitchTest = false; // DIAG : simule la droite souris (changement d'arme)
    public static boolean dbgTestMsg = false;    // DIAG : pousse un message de test (frame 10)

    public static void VBlankInterrupt() {
        if (dbgTestMsg && Mem.l(Vid_VBLCount_l) == 10) {     // DIAG : pousse un message à la frame 10
            int s = ab3d2.bss.TablesBss.ObjectWorkspace_vl;
            String t = "PORTAGE JAVA - MESSAGE DE TEST 123";
            for (int i = 0; i < t.length(); i++) Mem.wb(s + i, t.charAt(i));
            Mem.wb(s + t.length(), 0);
            ab3d2.c.Message.Msg_PushLine(s, t.length());
        }
        if (dbgSwitchTest) {                                  // DIAG : toggle droite souris (press 4 / release 4)
            ab3d2.host.CustomChips.mouseRightPressed = (Mem.l(Vid_VBLCount_l) % 8) < 4;
        }
        if (dbgWeaponTest) {                                  // DIAG : sélectionne l'arme courante + tir
            long vbl = Mem.l(Vid_VBLCount_l);
            int gun = (int) ((vbl / DBG_FRAMES_PER_GUN) % ab3d2.Defs.NUM_GUN_DEFS);
            if (vbl % DBG_FRAMES_PER_GUN == 0) Mem.ww(PlayerBss.Plr1_TimeToShoot_w, 0); // reset cooldown au switch
            Mem.wb(PlayerBss.Plr1_GunSelected_b, gun);
            Mem.ww(PlayerBss.Plr1_Health_w, 200);            // invincible (évite la mort par auto-dégâts roquettes/grenades)
            Mem.ww(PlayerBss.Plr2_Health_w, 200);            // la boucle teste les 2 santés → évite endlevel
            // un seul tir par arme (clic bref) → évite l'accumulation de projectiles qui ralentit la sim
            ab3d2.host.CustomChips.mouseLeftPressed = (vbl % DBG_FRAMES_PER_GUN) < 2;
        }
        if (dbgForceFire) ab3d2.host.CustomChips.mouseLeftPressed = true; // simule clic gauche (batch)
        Mem.wl(Vid_VBLCount_l, Mem.l(Vid_VBLCount_l) + 1);   // addq.l #1,Vid_VBLCount_l
        Mem.ww(ab3d2.bss.AnimBss.Anim_Timer_w, Mem.w(ab3d2.bss.AnimBss.Anim_Timer_w) - 1); // subq.w #1,Anim_Timer_w
        if (dbgTestSfx >= 0 && (Mem.l(Vid_VBLCount_l) % 50) == 10) { // DIAG : déclenche un SFX de test
            Mem.ww(Aud_SampleNum_w, dbgTestSfx);
            Mem.ww(IDNUM, 0x1234);
            Mem.wb(notifplaying, 0xFF);
            Mem.wl(Aud_NoiseX_w, 0);                          // centré (X+Z=0)
            Mem.wb(PlayEcho, 0);
            Mem.wb(Aud_ChannelPick_b, 1);
            Mem.ww(Aud_NoiseVol_w, 300);
            MakeSomeNoise();
        }
        // counter/main_counter/timer/button/button1 (délais menu) : inactifs en jeu.
        // .routine :
        if (Mem.b(Game_Running_b) != 0) {                    // tst.b Game_Running_b ; bne dosomething
            dosomething();                                   // (tombe dans JUSTSOUNDS)
        } else {
            JUSTSOUNDS();                                    // bra JUSTSOUNDS
        }
        // main_vblint : aucun en hôte.
    }

    /** OtherInter : interruption secondaire. Hôte. */
    public static void OtherInter() {
        throw new UnsupportedOperationException("hires.s::OtherInter (interruption, couche hôte)");
    }

    /** key_interrupt : interruption clavier (met à jour KeyMap_vb depuis le matériel). Hôte. */
    public static void key_interrupt() {
        throw new UnsupportedOperationException("hires.s::key_interrupt (interruption clavier, couche hôte)");
    }
}
