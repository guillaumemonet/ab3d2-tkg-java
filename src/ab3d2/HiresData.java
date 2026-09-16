package ab3d2;

// (Assets pour les incbin gameover/quietwelldone)

/**
 * Données embarquées de ab3d2_source/hires.s — TRADUCTION PARTIELLE.
 *
 * hires.s mélange code et données ; cette classe rassemble les blocs de
 * données déjà nécessaires aux modules traduits, chaque bloc étant copié
 * complet et dans l'ordre, avec sa ligne d'origine. Elle sera complétée au
 * fil de la traduction de hires.s (l'adjacence ENTRE blocs distincts n'est
 * pas garantie ici — seuls les labels d'un même bloc sont contigus, comme
 * dans l'original).
 */
public final class HiresData {

    // ---- hires.s:197-214 — Global data ----
    private static final int _a0 = Mem.align(4);
    public static final int LastZonePtr_l = Mem.dcL(0);
    public static final int xwobble = Mem.dcL(0);

    // Word aligned
    public static final int xwobxoff = Mem.dcW(0);
    public static final int xwobzoff = Mem.dcW(0);
    public static final int CollId = Mem.dcW(0);

    public static final int View_KeyLook_w = Mem.dcW(0);
    public static final int View_LookMin_w = Mem.dcW(0);
    public static final int View_LookMax_w = Mem.dcW(0);

    // Byte Aligned
    public static final int Game_MasterQuit_b = Mem.dcB(0);
    public static final int Game_SlaveQuit_b = Mem.dcB(0);
    public static final int Game_MasterPaused_b = Mem.dcB(0);
    public static final int Game_SlavePaused_b = Mem.dcB(0);

    // ---- hires.s:3391-3398 ----
    public static final int mang = Mem.dcW(0);
    public static final int Sys_OldMouseY = Mem.dcW(0);
    public static final int xmouse = Mem.dcW(0);
    public static final int Sys_MouseY = Mem.dcW(0);      // DCLC ; Pitch?
    public static final int MAPON = Mem.dcW(0);
    public static final int draw_RenderMap_b = Mem.dcW(0); // déclaré dc.w, testé en byte

    // ---- hires.s:3402 ----
    public static final int Game_Running_b = Mem.dcW(0);  // does main game run? (dc.w, accédé en byte)

    // ---- hires.s:8105-8121 ----
    /** pretab : table de bytes signés 0..127 puis -128..-1 (= identité 0..255 en non signé). */
    public static final int pretab = makePretab();
    public static final int tab = Mem.alloc(256 * 65);   // tab: ds.b 256*65
    public static final int test = Mem.dcL(0);           // test: dc.l 0
    private static final int _testPad = Mem.alloc(4 * 30); // ds.l 30

    private static int makePretab() {
        int a = Mem.alloc(256);
        int val = 0;
        for (int i = 0; i < 128; i++) { Mem.wb(a + i, val); val++; }        // REPT 128 dc.b val ; val 0..127
        val = -128;
        for (int i = 0; i < 128; i++) { Mem.wb(a + 128 + i, val); val++; }  // REPT 128 dc.b val ; val -128..-1
        return a;
    }

    // ---- hires.s:8124-8131 ----
    private static final int _a1 = Mem.align(4);
    public static final int Plr_XOff_l = Mem.dcL(0);
    public static final int Plr_ZOff_l = Mem.dcL(0);
    public static final int Plr_YOff_l = Mem.dcL(0);
    public static final int flooryoff = Mem.dcW(0);       // viewer y pos << 6
    public static final int XDiff_w = Mem.dcW(0);
    public static final int ZDiff_w = Mem.dcW(0);
    public static final int PlayEcho = Mem.dcW(0);        // accessed as byte

    // ---- hires.s:8142-8148 ----
    private static final int _a2 = Mem.align(4);
    public static final int ZonePtr_l = Mem.dcL(0);
    public static final int Lvl_WalkLinksPtr_l = Mem.dcL(0);
    public static final int Lvl_FlyLinksPtr_l = Mem.dcL(0);
    private static final int _anon0 = Mem.dcL(0);         // dc.l 0 anonyme
    public static final int Vid_CentreX_w = Mem.dcW(Hires.SMALL_WIDTH / 2);
    public static final int Vid_RightX_w = Mem.dcW(Hires.SMALL_WIDTH); // DCLC

    // ---- hires.s:3367-3374 ----
    private static final int _a3 = Mem.align(4);
    public static final int Lvl_CompactMapPtr_l = Mem.dcL(0);
    public static final int Lvl_BigMapPtr_l = Mem.dcL(0);
    public static final int Draw_CurrentZonePtr_l = Mem.dcL(0, 0); // dc.l 0,0 — paire {lower, upper}
    public static final int Zone_SplitHeight_l = Mem.dcL(0);
    public static final int draw_WallID_w = Mem.dcW(0);
    public static final int SMALLIT = Mem.dcW(0);
    public static final int draw_GouraudFlatsSelected_b = Mem.dcW(0); // dc.w, accédé en byte

    // ---- hires.s:3387-3389 ----
    public static final int lrs = Mem.dcW(0);
    public static final int Vis_AngPos_w = Mem.dcW(0);     // DCLC ; Yaw

    // ---- hires.s:3553 ----
    public static final int Zone_Bright_w = Mem.dcW(0);    // 0xABADCAFE - Is this an ambient term for the whole zone?

    // ---- hires.s:4046 ----
    public static final int FloorPtBrightsPtr_l = Mem.dcL(0);

    // ---- hires.s:5439 ----
    public static final int draw_UseGouraudFlats_b = Mem.dcW(0); // dc.w, accédé en byte

    // ---- données locales hires.s référencées par Game_Begin (Phase 2) ----
    public static final int LASTDH = Mem.dcB(0);             // hires.s:2207 dc.b 0
    public static final int LASTDW = Mem.dcB(0);             // hires.s:2208 dc.b 0
    public static final int Lvl_ExitZoneID_w = Mem.dcW(0);   // hires.s:2244 dc.w 0
    public static final int timetodamage = Mem.dcW(0);       // hires.s:6202 dc.w 0
    public static final int dosounds = Mem.dcW(0);           // hires.s:6857 dc.w 0 (accédé en byte)
    public static final int hitcol = Mem.dcL(0);             // hires.s:8161 dc.l 0
    public static final int Aud_Null1_vw = Mem.alloc(2 * 500); // hires.s:8187 ds.w 500
    public static final int Aud_Null2_vw = Mem.alloc(2 * 500); // hires.s:8188 ds.w 500
    public static final int Aud_Null3_vw = Mem.alloc(2 * 500); // hires.s:8189 ds.w 500
    public static final int Aud_Null4_vw = Mem.alloc(2 * 500); // hires.s:8190 ds.w 500
    public static final int READCONTROLS = Mem.dcW(0);       // hires.s:2253 dc.w 0
    public static final int Vid_VBLCount_l = Mem.dcL(0);     // hires.s:5950 dc.l 0 (compteur VBL host)
    public static final int Vid_VBLCountLast_l = Mem.dcL(0); // hires.s:5951 dc.l 0
    /** hires.s:2257 BollocksRoom : dc.w -1 ; ds.l 50 ; ds.l 4 (zone factice pour Plr2 en solo). */
    public static final int BollocksRoom = makeBollocksRoom();

    private static int makeBollocksRoom() {
        int a = Mem.dcW(-1);                                 // dc.w -1
        Mem.alloc(50 * 4);                                   // ds.l 50
        Mem.alloc(4 * 4);                                    // ds.l 4 (pad)
        return a;
    }

    // ---- hires.s:5497-5499 ----
    private static final int _a4 = Mem.align(4);
    public static final int leftbright = Mem.dcL(0);       // dc.l, lu en .w (mot fort = partie entière 8.8)
    public static final int brightspd = Mem.dcL(0);        // dc.l, lu en .w (mot fort)

    // ---- hires.s:5822 ----
    public static final int draw_UseWater_b = Mem.dcW(0);  // dc.w, accédé en byte

    // ---- hires.s:6382-6384 ----
    public static final int OLDRET = Mem.dcW(0);
    public static final int Plr_OldCentre_b = Mem.dcW(0); // dc.w, accédé en byte
    public static final int OLDGOOD = Mem.dcW(0);

    // ---- VBlank / dosomething : états locaux (hires.s, accédés en byte via dc.w) ----
    public static final int tabheld = Mem.dcW(0);            // hires.s:6008
    public static final int thistime = Mem.dcW(0);           // hires.s:6010 (diviseur anim DOALLANIMS)
    public static final int lasttogsound = Mem.dcW(0);       // hires.s:6304
    public static final int OLDLTOG = Mem.dcW(0);            // hires.s:6306
    // alan : cycle d'anim (8×0,8×1,8×2,8×3 = 32 longs), alframe = frame courante, alanptr = curseur
    public static final int alframe = Mem.dcL(0);            // hires.s:5837
    public static final int alan = allocAlanTable();         // hires.s:5839
    public static final int alanptr = Mem.dcL(alan);         // hires.s:5846
    // STEROPT : table stéréo (dc.b 0,4,$FF,4,0,8,$ff,8) — hires.s:6298
    public static final int STEROPT = allocSterOpt();

    private static int allocAlanTable() {
        int base = Mem.dcL(0);                               // 8×0
        for (int i = 1; i < 8; i++) Mem.dcL(0);
        for (int i = 0; i < 8; i++) Mem.dcL(1);
        for (int i = 0; i < 8; i++) Mem.dcL(2);
        for (int i = 0; i < 8; i++) Mem.dcL(3);
        return base;
    }

    private static int allocSterOpt() {
        int base = Mem.dcB(0); Mem.dcB(4);
        Mem.dcB(0xFF); Mem.dcB(4);
        Mem.dcB(0); Mem.dcB(8);
        Mem.dcB(0xFF); Mem.dcB(8);
        return base;
    }

    // ---- hires.s:6078 ----
    public static final int AUXOBJ = Mem.dcW(0);

    // ---- hires.s:7425-7430 ----
    public static final int backbeat = Mem.dcW(0);
    public static final int playnull0 = Mem.dcW(0);
    public static final int playnull1 = Mem.dcW(0);
    public static final int playnull2 = Mem.dcW(0);
    public static final int playnull3 = Mem.dcW(0);

    // ---- hires.s:7474-7507 — audio ("info needed for the sound player") ----
    public static final int Aud_SampleNum_w = Mem.dcW(0);
    public static final int Aud_NoiseX_w = Mem.dcW(0);
    public static final int Aud_NoiseZ_w = Mem.dcW(0);
    public static final int Aud_NoiseVol_w = Mem.dcW(0);
    public static final int Aud_ChannelPick_b = Mem.dcW(0); // dc.w, accédé en byte
    public static final int IDNUM = Mem.dcW(0);
    public static final int Aud_NeedLeft_b = Mem.dcB(0);
    public static final int Aud_NeedRight_b = Mem.dcB(0);
    public static final int Aud_Stereo_b = Mem.dcB(0xFF);
    private static final int _aAud = Mem.align(2); // even
    public static final int CHANNELDATA = Mem.allocTop();
    public static final int LEFTCHANDATA = Mem.dcL(0x00000000);
    private static final int _lc1 = Mem.dcL(0x00000000);
    private static final int _lc2 = Mem.dcL(0xFF000000);
    private static final int _lc3 = Mem.dcL(0xFF000000);
    public static final int RIGHTCHANDATA = Mem.dcL(0x00000000);
    private static final int _rc1 = Mem.dcL(0x00000000);
    private static final int _rc2 = Mem.dcL(0xFF000000);
    private static final int _rc3 = Mem.dcL(0xFF000000);
    private static final int _chanPad = Mem.alloc(4 * 8); // ds.l 8
    public static final int RIGHTPLAYEDTAB = Mem.alloc(4 * 20);
    public static final int LEFTPLAYEDTAB = Mem.alloc(4 * 20);
    public static final int SourceEcho = Mem.dcW(0);
    public static final int PLREcho = Mem.dcW(0);
    public static final int LEFTOFFSET = Mem.dcL(0);
    public static final int RIGHTOFFSET = Mem.dcL(0);

    // ---- hires.s:2810-2813 — chemin attract mode (incbin "testpath" commenté → vide) ----
    private static final int _pathAlign = Mem.align(4);
    public static final int Path = Mem.allocTop();     // Path: (vide)
    public static final int endpath = Path;            // endpath: (== Path)
    public static final int pathpt = Mem.dcL(Path);    // pathpt: dc.l Path

    // ---- hires.s — état des canaux audio (MakeSomeNoise / VBlank) ----
    public static final int noiseloud = Mem.dcW(0);    // hires.s:7566

    private static final int _audCh = Mem.align(4);
    public static final int Samp0endLEFT;              // dc.l Aud_EmptyBufferEnd
    public static final int Samp1endLEFT;
    public static final int Samp2endLEFT;
    public static final int Samp3endLEFT;
    public static final int Samp0endRIGHT;
    public static final int Samp1endRIGHT;
    public static final int Samp2endRIGHT;
    public static final int Samp3endRIGHT;
    public static final int pos0LEFT;                  // dc.l Aud_EmptyBuffer_vl
    public static final int pos1LEFT;
    public static final int pos2LEFT;
    public static final int pos3LEFT;
    public static final int pos0RIGHT;
    public static final int pos1RIGHT;
    public static final int pos2RIGHT;
    public static final int pos3RIGHT;

    public static final int vol0left = Mem.dcW(0);     // hires.s:8081-8088
    public static final int vol0right = Mem.dcW(0);
    public static final int vol1left = Mem.dcW(0);
    public static final int vol1right = Mem.dcW(0);
    public static final int vol2left = Mem.dcW(0);
    public static final int vol2right = Mem.dcW(0);
    public static final int vol3left = Mem.dcW(0);
    public static final int vol3right = Mem.dcW(0);

    // hires.s:7441-7448 — pointeurs double-buffer des 4 sorties Paula (mix logiciel newsampbitl).
    // Chaque buffer Aud_NullN_vw fait 1000 o ; moitié avant [0..500), moitié arrière [500..1000).
    public static final int Aupt0   = Mem.dcL(Aud_Null1_vw);          // Paula ch0 (gauche : soft 0L+2L)
    public static final int Auback0 = Mem.dcL(Aud_Null1_vw + 500);
    public static final int Aupt2   = Mem.dcL(Aud_Null3_vw);          // Paula ch2 (droite : soft 1R+3R)
    public static final int Auback2 = Mem.dcL(Aud_Null3_vw + 500);
    public static final int Aupt3   = Mem.dcL(Aud_Null4_vw);          // Paula ch3 (gauche : soft 1L+3L)
    public static final int Auback3 = Mem.dcL(Aud_Null4_vw + 500);
    public static final int Aupt1   = Mem.dcL(Aud_Null2_vw);          // Paula ch1 (droite : soft 0R+2R)
    public static final int Auback1 = Mem.dcL(Aud_Null2_vw + 500);

    public static final int NoiseMade0LEFT = Mem.dcB(0); // hires.s:7619-7634
    public static final int NoiseMade1LEFT = Mem.dcB(0);
    public static final int NoiseMade2LEFT = Mem.dcB(0);
    public static final int NoiseMade3LEFT = Mem.dcB(0);
    public static final int NoiseMade0pLEFT = Mem.dcB(0);
    public static final int NoiseMade1pLEFT = Mem.dcB(0);
    public static final int NoiseMade2pLEFT = Mem.dcB(0);
    public static final int NoiseMade3pLEFT = Mem.dcB(0);
    public static final int NoiseMade0RIGHT = Mem.dcB(0);
    public static final int NoiseMade1RIGHT = Mem.dcB(0);
    public static final int NoiseMade2RIGHT = Mem.dcB(0);
    public static final int NoiseMade3RIGHT = Mem.dcB(0);
    public static final int NoiseMade0pRIGHT = Mem.dcB(0);
    public static final int NoiseMade1pRIGHT = Mem.dcB(0);
    public static final int NoiseMade2pRIGHT = Mem.dcB(0);
    public static final int NoiseMade3pRIGHT = Mem.dcB(0);

    // ---- hires.s:8064-8079 ----
    public static final int saveinters = Mem.dcW(0);
    public static final int z = Mem.dcW(10);
    public static final int notifplaying = Mem.dcW(0);
    public static final int audpos1 = Mem.dcW(0);
    public static final int audpos1b = Mem.dcW(0);
    public static final int audpos2 = Mem.dcW(0);
    public static final int audpos2b = Mem.dcW(0);
    public static final int audpos3 = Mem.dcW(0);
    public static final int audpos3b = Mem.dcW(0);
    public static final int audpos4 = Mem.dcW(0);
    public static final int audpos4b = Mem.dcW(0);

    // ---- hires.s:8283 ----
    public static final int oktodisplay = Mem.dcB(0);

    // ---- hires.s:8330-8331 ----
    public static final int maxbot = Mem.dcW(0);
    public static final int tstneg = Mem.dcL(0);

    // ---- hires.s:8339-8342 (après include modules/music.s) ----
    public static final int UseAllChannels = Mem.dcW(0); // dc.w, accédé en byte
    public static final int Lvl_MusicPtr_l = Mem.dcL(0);

    // ---- hires.s:8344-8355 — section .datachip,data_c ----
    // "not sure what this is; it seems to be used as timing device.
    //  I.e. by accessing chipmem, we throttle the CPU"
    public static final int tstchip = Mem.dcL(0);
    public static final int testchip = Mem.dcW(0);

    public static final int nullsample = Mem.dcL(0);

    public static final int gameover;   // incbin "includes/gameover"
    public static final int welldone;   // incbin "includes/quietwelldone"

    // ---- hires.s:8154-8155 — Link file ----
    public static final int GLF_DatabasePtr_l = Mem.dcL(0);
    public static final int GLF_DatabaseName_vb;

    // ================================================================
    // Données locales de Draw_Flats (hires.s:2209..5826) — voir Hires.Draw_Flats
    // ================================================================

    // ---- hires.s:2209 ----
    public static final int DOANYWATER = Mem.dcW(0);

    // ---- hires.s:3205-3208 ----
    public static final int fillscrnwater = Mem.dcW(0);
    public static final int DONTDOGUN = Mem.dcW(0);

    // ---- hires.s:3572-3573 — floor polygon ----
    public static final int numsidestd = Mem.dcW(0);
    public static final int bottomline = Mem.dcW(0);

    // ---- hires.s:3596 ----
    public static final int CLRNOFLOOR = Mem.dcW(0);

    // ---- hires.s:4043-4045 (align 4) — fbr/sbr (FloorPtBrightsPtr_l déjà défini plus haut) ----
    private static final int _flatAlign0 = Mem.align(4);
    public static final int fbr = Mem.dcW(0);
    public static final int sbr = Mem.dcW(0);

    // ---- hires.s:4515-4530 (align 4) ----
    private static final int _flatAlign1 = Mem.align(4);
    public static final int ypos = Mem.dcL(0);
    public static final int minz = Mem.dcL(0);            // dc.l, souvent lu en .w
    public static final int top = Mem.dcW(0);
    public static final int bottom = Mem.dcW(0);
    public static final int nfloors = Mem.dcW(0);
    public static final int lighttype = Mem.dcW(0);
    public static final int above = Mem.dcW(0);
    public static final int linedir = Mem.dcW(0);
    public static final int View2FloorDist = Mem.dcW(0);
    public static final int movespd = Mem.dcW(0);
    public static final int largespd = Mem.dcL(0);        // unused?
    public static final int disttobot = Mem.dcW(0);

    // ---- hires.s:4533-4539 (align 4) — OneOverN_vw : 1/N * 16384 ----
    public static final int OneOverN_vw;                  // (rempli dans static{})

    // ---- hires.s:4855-4856 ----
    public static final int tonextline = Mem.dcW(0);
    public static final int anyclipping = Mem.dcW(0);

    // ---- hires.s:5045 ----
    private static final int _refAlign = Mem.align(4);
    public static final int REFPTR = Mem.dcL(0);

    // ---- hires.s:5134 ----
    public static final int drawit = Mem.dcW(0);

    // ---- hires.s:5148-5155 (align 4) ----
    public static final int tstwhich = Mem.dcW(0);
    public static final int whichtile = Mem.dcW(0);
    public static final int leftedge = Mem.dcW(0);
    public static final int rightedge = Mem.dcW(0);
    private static final int _distAlign = Mem.align(4);
    public static final int draw_Distance_l = Mem.dcL(0);

    // ---- hires.s:5224-5231 ----
    public static final int widthleft = Mem.dcW(0);
    public static final int scaleval = Mem.dcW(0);
    private static final int _sxAlign = Mem.align(4);
    public static final int sxoff = Mem.dcL(0);           // viewer position in floor texture space?
    public static final int szoff = Mem.dcL(0);
    public static final int xoff34 = Mem.dcW(0);
    public static final int zoff34 = Mem.dcW(0);
    public static final int scosval = Mem.dcW(0);
    public static final int ssinval = Mem.dcW(0);

    // ---- hires.s:5632-5652 (align 4) — water frame pointers ----
    public static final int draw_LastWaterFramePtr_l;
    public static final int draw_WaterFramePtrs_vl;
    public static final int draw_EndWaterFramePtrs_l;   // label après les 8 pointeurs
    public static final int draw_WaterFramePtr_l;
    public static final int wateroff;
    public static final int wtan;

    // ---- hires.s:5822-5826 — draw_UseWater_b déjà défini ; padding + startsmooth ----
    public static final int startsmoothx = Mem.dcW(0);
    public static final int startsmoothz = Mem.dcW(0);

    static {
        // état des canaux audio — Samp*end = Aud_EmptyBufferEnd, pos* = Aud_EmptyBuffer_vl
        Mem.align(4);
        int endBuf = ab3d2.bss.TablesBss.Aud_EmptyBufferEnd;
        int emptyBuf = ab3d2.bss.TablesBss.Aud_EmptyBuffer_vl;
        Samp0endLEFT = Mem.dcL(endBuf);
        Samp1endLEFT = Mem.dcL(endBuf);
        Samp2endLEFT = Mem.dcL(endBuf);
        Samp3endLEFT = Mem.dcL(endBuf);
        Samp0endRIGHT = Mem.dcL(endBuf);
        Samp1endRIGHT = Mem.dcL(endBuf);
        Samp2endRIGHT = Mem.dcL(endBuf);
        Samp3endRIGHT = Mem.dcL(endBuf);
        pos0LEFT = Mem.dcL(emptyBuf);
        pos1LEFT = Mem.dcL(emptyBuf);
        pos2LEFT = Mem.dcL(emptyBuf);
        pos3LEFT = Mem.dcL(emptyBuf);
        pos0RIGHT = Mem.dcL(emptyBuf);
        pos1RIGHT = Mem.dcL(emptyBuf);
        pos2RIGHT = Mem.dcL(emptyBuf);
        pos3RIGHT = Mem.dcL(emptyBuf);

        gameover = Assets.incbin("includes/gameover");
        welldone = Assets.incbin("includes/quietwelldone");

        GLF_DatabaseName_vb = Mem.dcStr("ab3:includes/test.lnk");
        Mem.dcB(0);
        Mem.align(4);

        // hires.s:4533-4539 — OneOverN_vw : dc.w 0 puis REPT MAX_ONE_OVER_N : dc.w 16384/val
        Mem.align(4);
        OneOverN_vw = Mem.allocTop();
        Mem.dcW(0);                                       // 16384/0 non défini
        for (int val = 1; val <= ab3d2.data.TablesData.MAX_ONE_OVER_N; val++) {
            Mem.dcW(16384 / val);
        }

        // hires.s:5632-5647 — water frame pointers
        Mem.align(4);
        int wf = ab3d2.data.DrawData.draw_WaterFrames_vb;
        draw_WaterFramePtrs_vl = Mem.allocTop();
        Mem.dcL(wf);                                      // draw_WaterFrames_vb
        Mem.dcL(wf + 2);
        Mem.dcL(wf + 256);
        Mem.dcL(wf + 256 + 2);
        Mem.dcL(wf + 512);
        Mem.dcL(wf + 512 + 2);
        Mem.dcL(wf + 768);
        Mem.dcL(wf + 768 + 2);
        draw_EndWaterFramePtrs_l = Mem.allocTop();        // label de fin des 8 pointeurs
        // draw_LastWaterFramePtr_l dc.l draw_WaterFramePtrs_vl (déclaré AVANT la table dans
        // l'original ; ici alloué séparément, valeur = adresse de la table)
        draw_LastWaterFramePtr_l = Mem.dcL(draw_WaterFramePtrs_vl);
        draw_WaterFramePtr_l = Mem.dcL(wf);              // dc.l draw_WaterFrames_vb
        wateroff = Mem.dcL(0);
        wtan = Mem.dcW(0);
    }

    private HiresData() {
    }
}
