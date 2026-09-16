package ab3d2;

import static ab3d2.ControlloopData.Game_LevelNumber_w;
import static ab3d2.ControlloopData.Game_LevelCounter_w;
import static ab3d2.ControlloopData.Game_ShouldQuit_b;
import static ab3d2.HiresData.GLF_DatabasePtr_l;
import static ab3d2.Defs.GLFT_ShootDefs_l;
import static ab3d2.Defs.GLFT_LevelNames_l;
import static ab3d2.Defs.NUM_BULLET_DEFS;
import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.modules.RawKeyMacros.RAWKEY_SPACEBAR;
import static ab3d2.modules.RawKeyMacros.RAWKEY_ENTER;
import static ab3d2.modules.RawKeyMacros.RAWKEY_UP;
import static ab3d2.modules.RawKeyMacros.RAWKEY_DOWN;
import static ab3d2.data.MenunbData.mnu_MYMAINMENU;
import static ab3d2.data.MenunbData.mnu_MYLEVELMENU;
import static ab3d2.data.MenunbData.mnu_MYLEVELMENU2;
import static ab3d2.data.MenunbData.mnu_currentsel;
import static ab3d2.data.MenunbData.mnu_row;
import static ab3d2.data.MenunbData.mnu_CURRENTLEVELLINE;
import static ab3d2.data.MenunbData.mnu_MYMASTERMENU;
import static ab3d2.data.MenunbData.mnu_CURRENTLEVELLINEM;
import static ab3d2.data.MenunbData.mnu_MASTERMODELINE;
import static ab3d2.data.MenunbData.mnu_MYSLAVEMENU;
import static ab3d2.ControlloopData.game_LevelSelected_w;
import static ab3d2.data.MenunbData.mnu_LevelAName_vb;
import static ab3d2.data.MenunbData.mnu_LevelIName_vb;
import static ab3d2.data.MenunbData.mnu_MYCUSTOMOPTSMENU;
import static ab3d2.data.MenunbData.optionLines;
import static ab3d2.data.MenunbData.mnu_MYCONTROLSONE;
import static ab3d2.data.MenunbData.mnu_MYCONTROLSTWO;
import static ab3d2.data.MenunbData.KEY_LINES;
import static ab3d2.data.MenunbData.KEY_LINES2;
import static ab3d2.data.MenunbData.mnu_frameptr;
import static ab3d2.data.MenunbData.mnu_buttonanim;
import static ab3d2.data.MenunbData.mnu_cursanim;
import static ab3d2.ControlloopData.AssignableKeys_vb;
import static ab3d2.ControlloopData.Prefs_CustomOptionsBuffer_vb;
import static ab3d2.ControlloopData.Prefs_OriginalMouse_b;
import static ab3d2.ControlloopData.Prefs_AlwaysRun_b;
import static ab3d2.ControlloopData.Prefs_ShowMessages_b;
import static ab3d2.ControlloopData.Prefs_NoAutoAim_b;
import static ab3d2.ControlloopData.Prefs_DisplayFPS_b;
import static ab3d2.ControlloopData.Prefs_ShowWeapon_b;
import static ab3d2.ControlloopData.Prefs_PlayMusic_b;
import static ab3d2.data.MenunbData.mnu_MYLOADMENU;
import static ab3d2.data.MenunbData.mnu_LSLOTA;
import static ab3d2.data.MenunbData.mnu_MYSAVEMENU;
import static ab3d2.data.MenunbData.mnu_SSLOTA;
import static ab3d2.bss.PlayerBss.Plr_Health_w;
import static ab3d2.bss.PlayerBss.Plr_Shield_w;
import static ab3d2.bss.PlayerBss.Plr_Weapons_vw;
import static ab3d2.bss.PlayerBss.Plr_AmmoCounts_vw;
import static ab3d2.bss.PlayerBss.Plr_MultiplayerType_b;
import static ab3d2.bss.PlayerBss.Plr1_Health_w;
import static ab3d2.bss.PlayerBss.Plr2_Health_w;
import static ab3d2.bss.PlayerBss.Plr1_JetpackFuel_w;
import static ab3d2.bss.PlayerBss.Plr2_JetpackFuel_w;
import static ab3d2.bss.PlayerBss.Plr1_Jetpack_w;
import static ab3d2.bss.PlayerBss.Plr2_Jetpack_w;
import static ab3d2.bss.PlayerBss.Plr1_Weapons_vb;
import static ab3d2.bss.PlayerBss.Plr2_Weapons_vb;
import static ab3d2.bss.PlayerBss.Plr1_AmmoCounts_vw;
import static ab3d2.bss.PlayerBss.Plr2_AmmoCounts_vw;
import static ab3d2.bss.AiBss.AI_NoEnemies_b;
import static ab3d2.data.LevelData.Lvl_BinFilenameX_vb;
import static ab3d2.data.LevelData.Lvl_GfxFilenameX_vb;
import static ab3d2.data.LevelData.Lvl_ClipsFilenameX_vb;
import static ab3d2.data.LevelData.Lvl_MapFilenameX_vb;
import static ab3d2.data.LevelData.Lvl_FlyMapFilenameX_vb;
import static ab3d2.data.LevelData.Lvl_FloorFilenameX_vb;
import static ab3d2.data.LevelData.Lvl_WallFilenameX_vb;
import static ab3d2.data.LevelData.Lvl_ModPropsFilenameX_vb;
import static ab3d2.data.LevelData.Lvl_ErrataFilenameX_vb;

/**
 * Traduction de ab3d2_source/controlloop.s — la boucle de contrôle / le flux de jeu.
 *
 * controlloop.s `include`s menu/menunb.s : c'est une seule unité d'assemblage. Les
 * boucles de menu reposent sur les primitives de menunb.s (mnu_openmenu/update/
 * waitmenu/redraw/getrawvalue + données mnu_MY*MENU) qui sont du rendu matériel/OS
 * (blitter, copper, sprites) non porté, sur le lien série 2 joueurs
 * (serial_nightmare.s) et l'entrée lowlevel (cd32joy.s). Ces orchestrations restent
 * donc en stubs documentés.
 *
 * Les fonctions de LOGIQUE D'ÉTAT autonomes sont traduites fidèlement :
 *   GETSTATS, SETPLAYERS, DEFAULTGAME, TWOPLAYER, playgame, game_SetMenuLevelName.
 *
 * Données (bloc PREFERENCES, Game_LevelNumber/Counter_w, etc.) dans ControlloopData.
 */
public final class Controlloop {

    private static final int PLR_SINGLE = 'n';
    private static final int PLR_MASTER = 'm';
    private static final int PLR_SLAVE = 's';

    /**
     * Mode 2 joueurs : {@code false} = VERSUS (deathmatch fidèle à ab3d2_source : pas d'aliens,
     * portes déverrouillées) ; {@code true} = CO-OP (restaure le jeu à deux contre les ennemis
     * de la version finale : aliens + portes à clé actifs). Choisi par le MASTER (menu ou
     * propriété {@code ab3d2.coop}) et transmis au slave par le handshake (synchro déterministe).
     */
    public static boolean coopMode = Boolean.getBoolean("ab3d2.coop");

    private Controlloop() {
    }

    /** GETSTATS — (vide dans l'original : « CHANGE PASSWORD INTO RAW DATA »). */
    public static void GETSTATS() {
        // rts
    }

    /**
     * SETPLAYERS — fixe les noms de fichiers de niveau (lettre = LevelNumber + 'a'),
     * puis initialise selon le mode (solo : AI_NoEnemies ; sinon master/slave série).
     */
    public static void SETPLAYERS() {
        int ch = (Mem.w(Game_LevelNumber_w) + 'a') & 0xFF;  // move.w Game_LevelNumber_w,d0 ; add.b #'a',d0
        Mem.wb(Lvl_BinFilenameX_vb, ch);                    // move.b d0,Lvl_BinFilenameX_vb
        Mem.wb(Lvl_GfxFilenameX_vb, ch);
        Mem.wb(Lvl_ClipsFilenameX_vb, ch);
        Mem.wb(Lvl_MapFilenameX_vb, ch);
        Mem.wb(Lvl_FlyMapFilenameX_vb, ch);
        Mem.wb(Lvl_FloorFilenameX_vb, ch);                  // fichiers optionnels (floor/wall/props/errata)
        Mem.wb(Lvl_WallFilenameX_vb, ch);
        Mem.wb(Lvl_ModPropsFilenameX_vb, ch);
        Mem.wb(Lvl_ErrataFilenameX_vb, ch);

        if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) {    // cmp #PLR_SLAVE ; beq Plr_InitSlave
            Plr_InitSlave();
            return;
        }
        if (Mem.b(Plr_MultiplayerType_b) == PLR_MASTER) {   // cmp #PLR_MASTER ; beq Plr_InitMaster
            Plr_InitMaster();
            return;
        }
        Mem.wb(AI_NoEnemies_b, 0xFF);                       // st AI_NoEnemies_b (onepla: rts)
    }

    /**
     * DEFAULTGAME — réinitialise les stats joueur (niveau 0, santé 200, arme de base,
     * munitions de l'arme par défaut).
     */
    public static void DEFAULTGAME() {
        Mem.ww(Game_LevelCounter_w, 0);                     // move.w #0,Game_LevelCounter_w

        int a0 = Plr_Health_w;
        int a1 = Plr_Shield_w;
        for (int i = 0; i < 11; ++i) { Mem.wl(a0, 0); a0 += 4; } // REPT 11 clr.l (a0)+
        for (int i = 0; i < 6; ++i) { Mem.wl(a1, 0); a1 += 4; }  // REPT 6 clr.l (a1)+

        Mem.ww(Plr_Health_w, 200);                          // move.w #200,Plr_Health_w
        Mem.ww(Plr_Weapons_vw, 0xff);                       // move.w #$ff,Plr_Weapons_vw

        int a5 = Mem.l(GLF_DatabasePtr_l) + GLFT_ShootDefs_l; // GLF_DatabasePtr_l + GLFT_ShootDefs_l
        int d0 = Mem.w(a5);                                 // move.w (a5),d0
        Mem.ww(Plr_AmmoCounts_vw + d0 * 2, 20);             // move.w #20,(Plr_AmmoCounts_vw,d0.w*2)
    }

    /**
     * TWOPLAYER — initialise les deux joueurs pour une partie 2J (santé, toutes les
     * armes, jetpack, munitions aléatoires).
     */
    public static void TWOPLAYER() {
        Mem.ww(Plr1_Health_w, 200);                         // move.w #200,Plr1_Health_w
        Mem.ww(Plr2_Health_w, 200);                         // move.w #200,Plr2_Health_w

        Mem.ww(Plr1_JetpackFuel_w, 0);                      // move.w #0,Plr1_JetpackFuel_w
        for (int o = 1; o <= 19; o += 2) {                  // st.b Plr1_Weapons_vb+1,+3,...,+19
            Mem.wb(Plr1_Weapons_vb + o, 0xFF);
        }
        Mem.wb(Plr1_Jetpack_w + 1, 0xFF);                   // st.b Plr1_Jetpack_w+1

        for (int o = 1; o <= 19; o += 2) {                  // st.b Plr2_Weapons_vb+1,...,+19
            Mem.wb(Plr2_Weapons_vb + o, 0xFF);
        }
        Mem.ww(Plr2_JetpackFuel_w, 0);                      // move.w #0,Plr2_JetpackFuel_w
        Mem.wb(Plr2_Jetpack_w + 1, 0xFF);                   // st.b Plr2_Jetpack_w+1

        int a0 = Plr1_AmmoCounts_vw;
        int a1 = Plr2_AmmoCounts_vw;
        for (int d1 = NUM_BULLET_DEFS - 1; d1 >= 0; --d1) { // move.w #NUM_BULLET_DEFS-1,d1 ; dbra
            int d0 = Objectmove.GetRand();                  // jsr GetRand
            d0 &= 63;                                       // and.w #63,d0
            d0 += 5;                                        // add.w #5,d0
            Mem.ww(a0, d0); a0 += 2;                        // move.w d0,(a0)+
            Mem.ww(a1, d0); a1 += 2;                        // move.w d0,(a1)+
        }
    }

    /** playgame — valide le niveau choisi (compteur → numéro courant). */
    public static void playgame() {
        Mem.ww(Game_LevelNumber_w, Mem.w(Game_LevelCounter_w)); // move.w Game_LevelCounter_w,Game_LevelNumber_w
    }

    /** game_SetMenuLevelName — copie LEVELNAME_DISPLAY_LEN(20) octets de a0 vers a1. */
    public static void game_SetMenuLevelName(int a0, int a1) {
        for (int d0 = 20 - 1; d0 >= 0; --d0) {              // moveq #LEVELNAME_DISPLAY_LEN-1,d0 ; dbra
            Mem.wb(a1, Mem.ub(a0));                         // move.b (a0)+,(a1)+
            a0++;
            a1++;
        }
    }

    // ------------------------------------------------------------------
    // Init 2 joueurs — handshake de départ sur le lien TCP (SerialNightmare/SerialLink).
    // Le master IMPOSE le n° de niveau et la graine RNG (Rand1) au slave → sims identiques.
    // ------------------------------------------------------------------

    /** Plr_InitMaster : envoie n° de niveau + graine (Rand1) + mode au slave, puis TWOPLAYER. */
    public static void Plr_InitMaster() {
        ensureMasterLink();                                 // établit le lien TCP (écoute) si pas déjà connecté
        // VERSUS (fidèle) : AI_NoEnemies_b=0 → aliens supprimés + portes déverrouillées.
        // CO-OP (restauré) : AI_NoEnemies_b=$FF → aliens + verrous de porte actifs (jeu à deux).
        Mem.wb(AI_NoEnemies_b, coopMode ? 0xFF : 0);
        SerialNightmare.SENDFIRST(Mem.w(Game_LevelNumber_w)); // move.w Game_LevelNumber_w,d0 ; jsr SENDFIRST
        SerialNightmare.SENDFIRST(Mem.w(ObjectmoveData.Rand1)); // move.w Rand1,d0 ; jsr SENDFIRST
        SerialNightmare.SENDFIRST(coopMode ? 1 : 0);        // transmet le mode au slave (ajout : synchro déterministe)
        TWOPLAYER();                                        // bsr TWOPLAYER
    }

    /**
     * Établit le lien 2 joueurs (transport TCP, absent de l'ASM = câble physique).
     * MASTER = écoute, SLAVE = se connecte. Hôte/port : propriétés {@code ab3d2.netHost}
     * (défaut localhost) / {@code ab3d2.netPort} (défaut {@link ab3d2.host.SerialLink#DEFAULT_PORT}).
     * Ne fait rien si le lien est déjà ouvert (réutilisé d'un niveau à l'autre).
     */
    private static void ensureMasterLink() {
        if (ab3d2.host.SerialLink.isConnected()) return;
        int port = Integer.getInteger("ab3d2.netPort", ab3d2.host.SerialLink.DEFAULT_PORT);
        try {
            System.out.println("[2P] MASTER : écoute sur le port " + port + " — en attente du slave…");
            ab3d2.host.SerialLink.startMaster(port, 120_000);
            System.out.println("[2P] MASTER : slave connecté.");
        } catch (java.io.IOException e) {
            throw new RuntimeException("lien 2 joueurs (master) : " + e.getMessage(), e);
        }
    }

    private static void ensureSlaveLink() {
        if (ab3d2.host.SerialLink.isConnected()) return;
        String host = System.getProperty("ab3d2.netHost", "localhost");
        int port = Integer.getInteger("ab3d2.netPort", ab3d2.host.SerialLink.DEFAULT_PORT);
        try {
            System.out.println("[2P] SLAVE : connexion à " + host + ":" + port + "…");
            ab3d2.host.SerialLink.startSlave(host, port, 120_000);
            System.out.println("[2P] SLAVE : connecté au master.");
        } catch (java.io.IOException e) {
            throw new RuntimeException("lien 2 joueurs (slave) : " + e.getMessage(), e);
        }
    }

    /** Plr_InitSlave : reçoit n° de niveau + graine (Rand1) + mode du master, puis TWOPLAYER. */
    public static void Plr_InitSlave() {
        ensureSlaveLink();                                  // établit le lien TCP (connexion) si pas déjà connecté
        int d0 = SerialNightmare.RECFIRST(0);               // jsr RECFIRST → d0 = n° de niveau du master
        Mem.ww(Game_LevelNumber_w, d0);                     // move.w d0,Game_LevelNumber_w
        int ch = (d0 + 'a') & 0xFF;                         // add.b #'a',d0
        Mem.wb(Lvl_BinFilenameX_vb, ch);                    // move.b d0,Lvl_BinFilenameX_vb
        Mem.wb(Lvl_GfxFilenameX_vb, ch);
        Mem.wb(Lvl_ClipsFilenameX_vb, ch);
        Mem.wb(Lvl_MapFilenameX_vb, ch);
        Mem.wb(Lvl_FlyMapFilenameX_vb, ch);
        d0 = SerialNightmare.RECFIRST(0);                   // jsr RECFIRST → d0 = graine Rand1 du master
        Mem.ww(ObjectmoveData.Rand1, d0);                   // move.w d0,Rand1
        coopMode = SerialNightmare.RECFIRST(0) != 0;        // reçoit le mode du master (ajout : synchro déterministe)
        Mem.wb(AI_NoEnemies_b, coopMode ? 0xFF : 0);        // même effet que le master (aliens+portes en CO-OP)
        TWOPLAYER();                                        // bsr TWOPLAYER
    }

    // ------------------------------------------------------------------
    // Boucles de menu / flux de jeu — reposent sur menunb.s (rendu menu matériel),
    // le lien série, l'entrée lowlevel et la couche d'affichage hôte. Stubs.
    // ------------------------------------------------------------------

    /**
     * Game_Start (controlloop.s) — ouvre l'écran, charge la base GLF + les assets,
     * affiche le menu (crédits + mnu_setscreen), puis boucle : menu principal →
     * Game_Begin (un niveau) → retour menu, jusqu'à EXIT. Appelé par Hires.startup
     * (qui a déjà fait Sys_Init + la table de constantes). Les détails hôte/OS non
     * essentiels (story file, _InitLowLevel, backdrop, FILTER LED) sont omis.
     */
    public static void Game_Start() {
        Mem.wb(Plr_MultiplayerType_b, PLR_SINGLE);          // move.b #PLR_SINGLE,Plr_MultiplayerType_b
        ab3d2.c.ScreenC.Vid_OpenMainScreen();               // CALLC Vid_OpenMainScreen (ouvre écran + entrée)

        long r = ab3d2.modules.FileIo.IO_LoadFile(ab3d2.HiresData.GLF_DatabaseName_vb); // base GLF
        Mem.wl(GLF_DatabasePtr_l, ab3d2.modules.FileIo.addr(r));

        // Texte d'intro (narratif) des niveaux : ab3:includes/TEXT_FILE → Lvl_IntroTextPtr_l
        // (move.l #Game_StoryFile_vb,a0 ; IO_LoadFile ; move.l d0,Lvl_IntroTextPtr_l)
        try {
            long st = ab3d2.modules.FileIo.IO_LoadFile(ab3d2.ControlloopData.Game_StoryFile_vb);
            Mem.wl(ab3d2.ControlloopData.Lvl_IntroTextPtr_l, ab3d2.modules.FileIo.addr(st));
        } catch (Throwable t) {
            System.out.println("[Game_Start] TEXT_FILE absent (" + t + ") — pas de texte d'intro");
        }

        ab3d2.MenuNb.mnu_copycredz();                       // jsr mnu_copycredz
        ab3d2.c.MenuC.mnu_setscreen();                      // CALLC mnu_setscreen (fondu d'entrée)

        try {                                               // Res_Load* (assets GLF : sons/textures/objets)
            ab3d2.modules.FileIo.IO_InitQueue();
            ab3d2.modules.Res.Res_LoadSoundFx();
            ab3d2.modules.Res.Res_LoadWallTextures();
            ab3d2.modules.Res.Res_LoadFloorsAndTextures();
            ab3d2.modules.Res.Res_LoadObjects();
            // Backdrop ciel (controlloop.s:79-82) — OMIS dans le portage git → Draw_BackdropImagePtr_l
            // restait à 0/garbage et Draw_SkyBackdrop lisait n'importe quoi (ciel corrompu).
            ab3d2.modules.FileIo.IO_QueueFile(ab3d2.data.DrawData.draw_BackdropImageName_vb,
                    ab3d2.bss.DrawBss.Draw_BackdropImagePtr_l, 0); // a0=nom, d0=&ptr, d1=0
            ab3d2.modules.FileIo.IO_FlushQueue();
            ab3d2.modules.Res.Res_PatchSoundFx();
        } catch (Throwable t) {
            System.out.println("[Game_Start] chargement assets partiel (" + t + ")");
        }

        game_SetMenuLevelNames();                           // jsr game_SetMenuLevelNames
        DEFAULTGAME();                                      // bsr DEFAULTGAME

        while (true) {                                      // game_BackToMenu
            ab3d2.c.SystemC.Sys_ClearKeyboard();            // CALLC Sys_ClearKeyboard
            ab3d2.host.Input.clearKeyQueue();

            // game_BackToMenu : retour au menu du mode courant (solo / master / slave)
            if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) {        // beq game_BackToSlave
                game_SlaveMenu();
            } else if (Mem.b(Plr_MultiplayerType_b) == PLR_MASTER) { // beq game_BackToMaster
                game_MasterMenu();
            } else {
                game_ReadMainMenu();                        // bsr game_ReadMainMenu (solo)
            }

            // game_DoneMenu :
            if (Mem.b(Game_ShouldQuit_b) != 0) {            // tst Game_ShouldQuit_b ; bne Game_Quit
                Game_Quit();
                return;
            }
            ab3d2.c.MenuC.mnu_clearscreen(1);               // mnu_clearscreen(fade=1)

            Mem.wb(ab3d2.ControlloopData.Game_FinishedLevel_b, 0); // clr.b Game_FinishedLevel_b
            Mem.ww(ab3d2.bss.PlayerBss.Plr1_SnapAngPos_w, 0);
            Mem.ww(ab3d2.bss.PlayerBss.Plr2_SnapAngPos_w, 0);
            Mem.ww(ab3d2.bss.PlayerBss.Plr1_AngPos_w, 0);
            Mem.ww(ab3d2.bss.PlayerBss.Plr2_AngPos_w, 0);
            Mem.wb(ab3d2.bss.PlayerBss.Plr1_GunSelected_b, 0);
            Mem.wb(ab3d2.bss.PlayerBss.Plr2_GunSelected_b, 0);
            Mem.wb(AI_NoEnemies_b, 0);                      // clr.b AI_NoEnemies_b

            // REPT 11 / REPT 6 : template Plr_* → Plr1/Plr2
            for (int i = 0; i < 11; i++) {
                int v = Mem.l(Plr_Health_w + i * 4);
                Mem.wl(Plr1_Health_w + i * 4, v);
                Mem.wl(Plr2_Health_w + i * 4, v);
            }
            for (int i = 0; i < 6; i++) {
                int v = Mem.l(Plr_Shield_w + i * 4);
                Mem.wl(ab3d2.bss.PlayerBss.Plr1_Shield_w + i * 4, v);
                Mem.wl(ab3d2.bss.PlayerBss.Plr2_Shield_w + i * 4, v);
            }

            ab3d2.modules.FileIo.IO_InitQueue();            // jsr IO_InitQueue
            ab3d2.c.DrawC.Draw_ResetGameDisplay();          // CALLC Draw_ResetGameDisplay

            Hires.loopFrameLimit = -1;                      // niveau interactif (sans limite)
            Hires.Game_Begin();                             // jsr Game_Begin (SETPLAYERS + Res_LoadLevelData inclus)

            // après le niveau : si terminé, recopie Plr1 → template
            if (Mem.b(ab3d2.ControlloopData.Game_FinishedLevel_b) != 0) {
                for (int i = 0; i < 11; i++) Mem.wl(Plr_Health_w + i * 4, Mem.l(Plr1_Health_w + i * 4));
                for (int i = 0; i < 6; i++) Mem.wl(Plr_Shield_w + i * 4, Mem.l(ab3d2.bss.PlayerBss.Plr1_Shield_w + i * 4));
            }
            ab3d2.c.MenuC.mnu_setscreen();                  // CALLC mnu_setscreen (retour menu)
        }
    }

    /** Game_Quit : restaure l'écran de jeu et ferme l'écran (libération Res/Vid : hôte). */
    public static int Game_Quit() {
        ab3d2.c.MenuC.mnu_clearscreen(0);                   // mnu_clearscreen(fade=0)
        ab3d2.c.ScreenC.Vid_CloseMainScreen();              // CALLC Vid_CloseMainScreen
        return 0;
    }

    /**
     * game_ReadMainMenu : boucle du menu principal (solo). Dispatch sur l'item choisi :
     * 0=PLAY GAME, 2=sélection de niveau, 8=EXIT. Les items secondaires (1 player,
     * options de contrôle, crédits, load/save, custom) redessinent simplement le menu
     * (handlers différés : 2 joueurs / config touches / save-load / custom options).
     */
    public static void game_ReadMainMenu() {
        Mem.wb(Plr_MultiplayerType_b, PLR_SINGLE);          // move.b #PLR_SINGLE,Plr_MultiplayerType_b
        refreshLevelName();                                 // game_SetMenuLevelName(courant → mnu_CURRENTLEVELLINE)
        game_OpenMenu(mnu_MYMAINMENU);                      // bsr game_OpenMenu
        while (true) {                                      // .rdlop
            int d0 = game_CheckMenu(mnu_MYMAINMENU);        // bsr game_CheckMenu
            if (Mem.b(Game_ShouldQuit_b) != 0) {            // fenêtre fermée → sortie propre
                return;
            }
            if (d0 == 0) {                                  // tst.w d0 ; beq playgame
                playgame();
                return;
            }
            if (d0 == 1) {                                  // 2 JOUEURS (→ menu master)
                game_MasterMenu();                          // bra game_MasterMenu (tail)
                return;
            }
            if (d0 == 2) {                                  // sélection de niveau
                levelMenu();
                refreshLevelName();
                game_OpenMenu(mnu_MYMAINMENU);
                game_WaitForMenuKey();
                continue;                                   // bra game_ReadMainMenu (redraw)
            }
            if (d0 == 3) {                                  // CONTROL OPTIONS
                CHANGECONTROLS();
                game_OpenMenu(mnu_MYMAINMENU);
                game_WaitForMenuKey();
                continue;
            }
            if (d0 == 5) {                                  // LOAD POSITION
                game_LoadPosition();
                game_OpenMenu(mnu_MYMAINMENU);
                game_WaitForMenuKey();
                continue;
            }
            if (d0 == 6) {                                  // SAVE POSITION
                game_WaitForMenuKey();
                game_SavePosition();
                game_OpenMenu(mnu_MYMAINMENU);
                game_WaitForMenuKey();
                continue;
            }
            if (d0 == 7) {                                  // CUSTOM OPTIONS
                game_WaitForMenuKey();
                customOptions();
                game_OpenMenu(mnu_MYMAINMENU);
                game_WaitForMenuKey();
                continue;
            }
            if (d0 == 8) {                                  // EXIT
                Mem.wb(Game_ShouldQuit_b, 0xFF);            // st Game_ShouldQuit_b
                return;                                     // → Game_Quit (flux appelant)
            }
            // item 4 = GAME CREDITS (`;jsr mnu_viewcredz` commenté dans l'original → no-op) : redraw.
            game_OpenMenu(mnu_MYMAINMENU);
        }
    }

    /**
     * game_MasterMenu (controlloop.s:647) — menu 2 joueurs côté MASTER. Fixe PLR_MASTER, laisse
     * le master choisir le niveau (item 1 = niveau suivant, borné à Game_LevelCounter_w), et sur
     * PLAY (item 2) fixe Game_LevelNumber_w et rend la main → Game_Begin (handshake). Item 0 (titre)
     * bascule vers le menu slave ; item 3 = config touches. Le lien TCP est établi au handshake.
     */
    public static void game_MasterMenu() {
        Mem.wb(Plr_MultiplayerType_b, PLR_MASTER);          // move.b #PLR_MASTER,Plr_MultiplayerType_b
        Mem.ww(game_LevelSelected_w, 0);                    // move.w #0,game_LevelSelected_w
        setMasterLevelName(0);                              // nom du niveau 0 → mnu_CURRENTLEVELLINEM
        setMasterModeLine();                                // affiche VERSUS / CO-OP (ligne mode)
        game_OpenMenu(mnu_MYMASTERMENU);                    // bsr game_OpenMenu
        while (true) {                                      // .rdlop
            int d0 = game_CheckMenu(mnu_MYMASTERMENU);      // bsr game_CheckMenu
            if (Mem.b(Game_ShouldQuit_b) != 0) {            // fenêtre fermée → sortie propre
                return;
            }
            if (d0 == 1) {                                  // niveau suivant (borné, wrap à 0)
                int lvl = Mem.w(game_LevelSelected_w) + 1;  // add.w #1,d0
                if (lvl >= (short) Mem.w(Game_LevelCounter_w)) lvl = 0; // cmp Game_LevelCounter_w ; blt ; #0
                Mem.ww(game_LevelSelected_w, lvl);
                setMasterLevelName(lvl);
                MenuNb.mnu_redraw(mnu_MYMASTERMENU);        // jsr mnu_redraw
                continue;                                   // bra .rdlop
            }
            if (d0 == 2) {                                  // MODE : bascule VERSUS ↔ CO-OP (ajout 2J)
                coopMode = !coopMode;
                setMasterModeLine();
                MenuNb.mnu_redraw(mnu_MYMASTERMENU);
                continue;
            }
            if (d0 == 3) {                                  // PLAY GAME
                Mem.ww(Game_LevelNumber_w, Mem.w(game_LevelSelected_w)); // move.w game_LevelSelected_w,Game_LevelNumber_w
                return;                                     // rts → game_DoneMenu → Game_Begin
            }
            if (d0 == 0) {                                  // titre → bascule menu SLAVE
                game_SlaveMenu();                           // bra game_SlaveMenu (tail)
                return;
            }
            if (d0 == 4) {                                  // CONTROL OPTIONS
                CHANGECONTROLS();
                game_OpenMenu(mnu_MYMASTERMENU);
                continue;                                   // bra .rdlop
            }
            // d0<0 (aucune sélection) : reboucle
        }
    }

    /** Affiche le mode 2 joueurs courant (VERSUS / CO-OP) sur la ligne mode du menu master. */
    private static void setMasterModeLine() {
        writeMenuLine(mnu_MASTERMODELINE, coopMode ? "     COOP  MODE" : "    VERSUS  MODE");
    }

    /** Écrit une chaîne dans une ligne de menu (20 octets, complétée par des espaces). */
    private static void writeMenuLine(int addr, String s) {
        for (int i = 0; i < 20; i++) {
            Mem.wb(addr + i, i < s.length() ? s.charAt(i) : ' ');
        }
    }

    /**
     * game_SlaveMenu (controlloop.s:718) — menu 2 joueurs côté SLAVE. Fixe PLR_SLAVE ; sur PLAY
     * (item 1) rend la main → Game_Begin (le niveau vient du master par le handshake). Item 0
     * (titre) revient au menu principal ; item 2 = config touches.
     */
    public static void game_SlaveMenu() {
        Mem.wb(Plr_MultiplayerType_b, PLR_SLAVE);           // move.b #PLR_SLAVE,Plr_MultiplayerType_b
        game_OpenMenu(mnu_MYSLAVEMENU);                     // bsr game_OpenMenu
        while (true) {                                      // .rdlop
            int d0 = game_CheckMenu(mnu_MYSLAVEMENU);       // bsr game_CheckMenu
            if (Mem.b(Game_ShouldQuit_b) != 0) {
                return;
            }
            if ((short) d0 < 0) {                           // tst.w d0 ; blt .rdlop
                continue;
            }
            game_WaitForMenuKey();                          // bsr game_WaitForMenuKey
            if (d0 == 1) {                                  // PLAY GAME
                return;                                     // rts → game_DoneMenu → Game_Begin
            }
            if (d0 == 0) {                                  // titre → retour menu principal
                game_ReadMainMenu();                        // bra game_ReadMainMenu (tail)
                return;
            }
            if (d0 == 2) {                                  // CONTROL OPTIONS
                CHANGECONTROLS();
                game_OpenMenu(mnu_MYSLAVEMENU);
                continue;                                   // bra .rdlop
            }
        }
    }

    /** Nom du niveau {@code lvl} (depuis le GLF) → ligne mnu_CURRENTLEVELLINEM du menu master. */
    private static void setMasterLevelName(int lvl) {
        int a0 = Mem.l(GLF_DatabasePtr_l) + GLFT_LevelNames_l + lvl * 40; // GLF + LevelNames + d0*40
        game_SetMenuLevelName(a0, mnu_CURRENTLEVELLINEM);
    }

    /** Remplit mnu_CURRENTLEVELLINE avec le nom du niveau courant (depuis le GLF). */
    private static void refreshLevelName() {
        int d0 = Mem.w(Game_LevelCounter_w);                // move.w Game_LevelCounter_w,d0
        int a0 = Mem.l(GLF_DatabasePtr_l) + GLFT_LevelNames_l + d0 * 40; // GLF + LevelNames + d0*40
        game_SetMenuLevelName(a0, mnu_CURRENTLEVELLINE);
    }

    /** levelMenu : menu de sélection de niveau (page 1 = A..H, item 8 = page suivante). */
    private static void levelMenu() {
        game_OpenMenu(mnu_MYLEVELMENU);
        int d0 = game_CheckMenu(mnu_MYLEVELMENU);
        if (d0 == 8) {                                      // NEXT PAGE
            levelMenu2();
            return;
        }
        // DEFGAME (chargement de la sauvegarde par défaut du niveau) : différé.
        Mem.ww(Game_LevelCounter_w, d0);                    // move d0,Game_LevelCounter_w
    }

    /** levelMenu2 : page 2 (I..P, item 8 = retour menu principal). */
    private static void levelMenu2() {
        game_OpenMenu(mnu_MYLEVELMENU2);
        int d0 = game_CheckMenu(mnu_MYLEVELMENU2);
        if (d0 == 8) {                                      // MAIN MENU
            return;
        }
        Mem.ww(Game_LevelCounter_w, d0 + 8);               // niveaux I..P = 8..15
    }

    /** not.b : inverse un octet (toggle d'un booléen 0/0xFF). */
    private static void notB(int addr) {
        Mem.wb(addr, ~Mem.b(addr));
    }

    /**
     * customOptions (controlloop.s) — menu des options : affiche Y/N pour chaque
     * réglage (copié dans le texte avant ouverture), bascule le Prefs_* sélectionné,
     * redessine. Item 8 = MAIN MENU (sortie).
     */
    public static void customOptions() {
        while (true) {                                      // .redraw
            // copie les 6 réglages → 'Y'/'N' à l'offset 17 des lignes d'option (+21 chacune)
            int a0 = Prefs_CustomOptionsBuffer_vb;
            int a1 = optionLines + 17;
            for (int d1 = 0; d1 <= 5; ++d1) {               // moveq #5,d1 ; .copyOpts
                int v = Mem.ub(a0); a0++;
                Mem.wb(a1, v != 0 ? 'Y' : 'N');
                a1 += 21;
            }
            game_OpenMenu(mnu_MYCUSTOMOPTSMENU);
            int d0 = game_CheckMenu(mnu_MYCUSTOMOPTSMENU);
            if (d0 == 8) {                                  // MAIN MENU
                return;
            }
            switch (d0) {                                   // not.b du réglage choisi
                case 0: notB(Prefs_OriginalMouse_b); break;
                case 1: notB(Prefs_AlwaysRun_b);     break;
                case 2: notB(Prefs_ShowMessages_b);  break;
                case 3: notB(Prefs_NoAutoAim_b);     break;
                case 4: notB(Prefs_DisplayFPS_b);    break;
                case 5: notB(Prefs_ShowWeapon_b);    break;
                case 6: notB(Prefs_PlayMusic_b);     break;
                default: break;                             // 7 = sans effet
            }
            // bra .redraw (re-copie Y/N + réouverture)
        }
    }

    /**
     * CHANGECONTROLS (controlloop.s) — reconfiguration des touches, page 1 (11 touches).
     * Affiche le glyphe de chaque touche, puis sur sélection : efface l'affichage,
     * attend une touche (mnu_getrawvalue), la stocke dans AssignableKeys_vb. Item 11 = page 2.
     */
    public static void CHANGECONTROLS() {
        // copie les assignations courantes dans le texte (glyphe = rawkey + 132)
        int a0 = AssignableKeys_vb;
        int a1 = KEY_LINES + 17;
        for (int d1 = 0; d1 <= 10; ++d1) {                  // moveq #10,d1 (11 touches)
            int v = Mem.ub(a0); a0++;
            Mem.wb(a1, (v + 132) & 0xFF);
            a1 += 21;
        }
        a1 = KEY_LINES2 + 17;
        for (int d1 = 0; d1 <= 5; ++d1) {                   // moveq #5,d1 (6 touches page 2)
            int v = Mem.ub(a0); a0++;
            Mem.wb(a1, (v + 132) & 0xFF);
            a1 += 21;
        }

        game_OpenMenu(mnu_MYCONTROLSONE);
        while (true) {                                      // .rdlop
            int d0 = game_CheckMenu(mnu_MYCONTROLSONE);
            if (d0 == 11) {                                 // MORE → page 2
                CHANGECONTROLS2();
                return;
            }
            int la = KEY_LINES + d0 * 21 + 16;              // emplacement du glyphe (offset 16/17)
            Mem.ww(la, 0x2020);                             // efface (2 espaces)
            MenuNb.mnu_redraw(mnu_MYCONTROLSONE);
            Mem.wl(mnu_frameptr, mnu_buttonanim);           // anime le « bouton »
            int newkey = MenuNb.mnu_getrawvalue();          // attend une touche
            Mem.wl(mnu_frameptr, mnu_cursanim);
            Mem.wb(AssignableKeys_vb + d0, newkey);         // stocke la nouvelle touche
            Mem.wb(la + 1, (newkey + 132) & 0xFF);          // affiche son glyphe (offset 17)
            MenuNb.mnu_redraw(mnu_MYCONTROLSONE);
        }
    }

    /** CHANGECONTROLS2 : page 2 des contrôles (6 touches). Item 6 = retour menu principal. */
    private static void CHANGECONTROLS2() {
        game_OpenMenu(mnu_MYCONTROLSTWO);
        while (true) {
            int d0 = game_CheckMenu(mnu_MYCONTROLSTWO);
            if (d0 == 6) {                                  // MAIN MENU
                return;
            }
            int la = KEY_LINES2 + d0 * 21 + 16;
            Mem.ww(la, 0x2020);
            MenuNb.mnu_redraw(mnu_MYCONTROLSTWO);
            Mem.wl(mnu_frameptr, mnu_buttonanim);
            int newkey = MenuNb.mnu_getrawvalue();
            Mem.wl(mnu_frameptr, mnu_cursanim);
            Mem.wb(AssignableKeys_vb + 11 + d0, newkey);    // touches page 2 = offset +11
            Mem.wb(la + 1, (newkey + 132) & 0xFF);
            MenuNb.mnu_redraw(mnu_MYCONTROLSTWO);
        }
    }

    /** DEFGAME : charge la sauvegarde par défaut (IO). */
    public static void DEFGAME(int d0) {
        throw new UnsupportedOperationException("controlloop.s::DEFGAME (chargement sauvegarde, à compléter)");
    }

    // Format de slot de sauvegarde : WORD niveau + 11 longs (Plr_Health..) + 6 longs (Plr_Shield..).
    private static final int SAVE_SLOT_SIZE = 2 + (22 * 2) + (12 * 2);   // 70
    private static final int NUM_SAVE_SLOTS = 6;
    private static final int SAVE_FILE_SIZE = NUM_SAVE_SLOTS * SAVE_SLOT_SIZE; // 420

    /**
     * Charge le fichier de sauvegardes ("ab3:boot.dat" → overlay run/) dans un buffer
     * AllocVec (zéros si absent). Renvoie l'adresse du buffer (à libérer par FreeVec).
     * NB : l'original passe par IO_QueueFile, mais celui-ci affiche « insert disk » +
     * retry si le fichier manque ; on lit donc directement via DosLib (robuste).
     */
    private static int loadSaveFile() {
        int ptr = ab3d2.host.ExecLib.AllocVec(SAVE_FILE_SIZE, ab3d2.host.ExecLib.MEMF_PUBLIC);
        ab3d2.modules.Sys.Sys_MemFillLong(ptr, 0, SAVE_FILE_SIZE / 4);   // slots vides par défaut
        int h = ab3d2.host.DosLib.Open(ab3d2.data.GameData.Game_SavedGamesName_vb, ab3d2.host.DosLib.MODE_OLDFILE);
        if (h != 0) {
            ab3d2.host.DosLib.Read(h, ptr, SAVE_FILE_SIZE);
            ab3d2.host.DosLib.Close(h);
        }
        return ptr;
    }

    /**
     * game_LoadPosition (controlloop.s) — affiche les 5 slots (NEW GAME + sauvegardes),
     * charge l'inventaire du slot choisi dans Plr_Health_w (17 longs : santé+munitions+
     * bouclier+armes), applique les bornes d'inventaire. Item 6 = CANCEL.
     */
    public static void game_LoadPosition() {
        int ptr = loadSaveFile();
        int a4 = mnu_LSLOTA + 21;                            // item 1 (slot 1)
        int a3 = ptr + SAVE_SLOT_SIZE;                       // slot 1 dans le buffer
        for (int d7 = 0; d7 <= 4; ++d7) {                    // 5 slots affichés
            int lvl = Mem.uw(a3);
            int a0 = Mem.l(GLF_DatabasePtr_l) + GLFT_LevelNames_l + lvl * 40;
            game_SetMenuLevelName(a0, a4);
            a4 += 21;
            a3 += SAVE_SLOT_SIZE;
        }
        game_OpenMenu(mnu_MYLOADMENU);
        int d0 = game_CheckMenu(mnu_MYLOADMENU);
        if (d0 != 6) {                                       // 6 = CANCEL
            int s = ptr + d0 * SAVE_SLOT_SIZE;
            Mem.ww(Game_LevelCounter_w, Mem.uw(s)); s += 2;
            for (int i = 0; i < 17; ++i) {                   // 11 (santé/munitions) + 6 (bouclier/armes)
                Mem.wl(Plr_Health_w + i * 4, Mem.l(s)); s += 4;
            }
            ab3d2.c.GameC.Game_ApplyInventoryLimits(Plr_Health_w); // borne l'inventaire chargé
            refreshLevelName();                              // met à jour la ligne niveau du menu principal
        }
        ab3d2.host.ExecLib.FreeVec(ptr);
    }

    /**
     * game_SavePosition (controlloop.s) — affiche 5 slots ; écrit l'inventaire courant
     * (Plr_Health_w, 17 longs) dans le slot choisi puis réécrit le fichier complet.
     * Item 5 = CANCEL.
     */
    public static void game_SavePosition() {
        int ptr = loadSaveFile();                            // charge l'existant (préserve les autres slots)
        int a4 = mnu_SSLOTA;                                 // item 0 (slot 1)
        int a3 = ptr + SAVE_SLOT_SIZE;                       // slot 1
        for (int d7 = 0; d7 <= 4; ++d7) {
            int lvl = Mem.uw(a3);
            int a0 = Mem.l(GLF_DatabasePtr_l) + GLFT_LevelNames_l + lvl * 40;
            game_SetMenuLevelName(a0, a4);
            a4 += 21;
            a3 += SAVE_SLOT_SIZE;
        }
        game_OpenMenu(mnu_MYSAVEMENU);
        int d0 = game_CheckMenu(mnu_MYSAVEMENU);
        if (d0 != 5) {                                       // 5 = CANCEL
            int s = ptr + (d0 + 1) * SAVE_SLOT_SIZE;         // slots 1..5
            Mem.ww(s, Mem.uw(Game_LevelCounter_w)); s += 2;
            for (int i = 0; i < 17; ++i) {
                Mem.wl(s, Mem.l(Plr_Health_w + i * 4)); s += 4;
            }
            int h = ab3d2.host.DosLib.Open(ab3d2.data.GameData.Game_SavedGamesName_vb, ab3d2.host.DosLib.MODE_NEWFILE);
            if (h != 0) {
                ab3d2.host.DosLib.Write(h, ptr, SAVE_FILE_SIZE);
                ab3d2.host.DosLib.Close(h);
            }
        }
        ab3d2.host.ExecLib.FreeVec(ptr);
    }

    /**
     * game_WaitForMenuKey : attend que les entrées de validation (souris gauche /
     * Espace / Entrée / Haut / Bas) soient RELÂCHÉES (anti-rebond). Pompe une frame
     * de menu par itération (met à jour l'entrée + anime le feu).
     */
    public static void game_WaitForMenuKey() {
        while (true) {
            ab3d2.c.MenuC.WaitTOF();                         // pompe une frame (pollEvents)
            if (Mem.b(Game_ShouldQuit_b) != 0) {            // fenêtre fermée → ne pas boucler
                return;
            }
            if (ab3d2.host.CustomChips.mouseLeftPressed) {   // btst #7,$bfe001 (souris active bas)
                continue;
            }
            if (Mem.ub(KeyMap_vb + RAWKEY_SPACEBAR) != 0) continue;
            if (Mem.ub(KeyMap_vb + RAWKEY_ENTER) != 0) continue;
            if (Mem.ub(KeyMap_vb + RAWKEY_UP) != 0) continue;
            if (Mem.ub(KeyMap_vb + RAWKEY_DOWN) != 0) continue;
            return;
        }
    }

    /** game_SetMenuLevelNames : remplit les noms de niveau des 2 pages depuis le GLF. */
    public static void game_SetMenuLevelNames() {
        int a0 = Mem.l(GLF_DatabasePtr_l) + GLFT_LevelNames_l; // GLF + GLFT_LevelNames_l
        int a1 = mnu_LevelAName_vb;
        for (int i = 0; i < 8; ++i) {                        // page 1 : A..H
            game_SetMenuLevelName(a0, a1);
            a0 += 40;                                        // (20 copiés + 20) — nom suivant
            a1 += 21;                                        // (20 copiés + 1 séparateur)
        }
        a1 = mnu_LevelIName_vb;
        for (int i = 0; i < 8; ++i) {                        // page 2 : I..P
            game_SetMenuLevelName(a0, a1);
            a0 += 40;
            a1 += 21;
        }
    }

    /** game_OpenMenu : ouvre/dessine un menu (mnu_openmenu). */
    public static void game_OpenMenu(int a0) {
        MenuNb.mnu_openmenu(a0);                             // jsr mnu_openmenu
    }

    /** game_CheckMenu : met à jour + attend la sélection ; renvoie l'item (= mnu_row % items). */
    public static int game_CheckMenu(int a0) {
        ab3d2.host.Input.lastpressed = 0;                    // move.b #0,lastpressed
        MenuNb.mnu_update(a0);                               // jsr mnu_update
        MenuNb.mnu_waitmenu();                               // jsr mnu_waitmenu (valeur ignorée)
        int items = Mem.uw(a0 + 14);
        int d2 = items == 0 ? 0 : Integer.remainderUnsigned(Mem.uw(mnu_row), items); // mnu_row / items → reste
        Mem.ww(mnu_currentsel, d2);                          // move.w d2,mnu_currentsel
        return d2;                                           // d0 = option number
    }
}
