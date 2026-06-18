package ab3d2.host;

import ab3d2.Hires;
import ab3d2.Mem;
import ab3d2.Controlloop;
import ab3d2.MenuNb;
import ab3d2.bss.Bss;
import ab3d2.data.DataSections;
import ab3d2.c.ScreenC;
import ab3d2.c.SystemC;
import ab3d2.c.MenuC;
import ab3d2.modules.FileIo;
import ab3d2.modules.Res;

import static ab3d2.bss.PlayerBss.*;
import static ab3d2.bss.ZoneBss.Zone_OrderTable_Barrier_w;
import static ab3d2.bss.TablesBss.ConstantTable_vl;
import static ab3d2.HiresData.GLF_DatabaseName_vb;
import static ab3d2.HiresData.GLF_DatabasePtr_l;
import static ab3d2.HiresData.draw_GouraudFlatsSelected_b;
import static ab3d2.ControlloopData.Game_ShouldQuit_b;
import static ab3d2.ControlloopData.Game_LevelNumber_w;
import static ab3d2.ControlloopData.Game_LevelCounter_w;
import static ab3d2.data.MenunbData.mnu_MYMAINMENU;

/**
 * Harnais du MENU (controlloop.s::Game_Start). Réplique le bring-up runnable
 * (Sys_Init + ouverture écran + chargement base GLF/assets) puis lance le menu :
 *  - défaut : boucle interactive (mnu_setscreen + game_ReadMainMenu) ; PLAY lance
 *    le niveau (Game_Begin) et revient au menu ; EXIT quitte.
 *  - `-PmenuFrames=N` : rend N frames du menu et capture menu_screenshot.png (test).
 */
public final class MenuTest {

    private MenuTest() {
    }

    public static void main(String[] args) {
        Bss.init();
        DataSections.init();
        int touch = mnu_MYMAINMENU;                          // force l'init de MenunbData

        if (System.getProperty("prefsTest") != null) {       // test persistance prefs (sans fenêtre)
            prefsRoundTrip();
            return;
        }

        ScreenC.setHostScale(3);

        SystemC.Sys_Init();                                  // matériel + Draw_Init + Game_Init

        // --- inits de _startup ---
        Mem.ww(Plr1_Energy_w, 191);
        Mem.ww(Plr2_Energy_w, 191);
        Mem.ww(Zone_OrderTable_Barrier_w, 0xFFFF);
        Mem.wb(draw_GouraudFlatsSelected_b, 0xFF);
        Mem.wb(Plr1_Keys_b, 0); Mem.wb(Plr1_Path_b, 0); Mem.wb(Plr1_Mouse_b, 0xFF); Mem.wb(Plr1_Joystick_b, 0);
        Mem.wb(Plr2_Keys_b, 0); Mem.wb(Plr2_Path_b, 0); Mem.wb(Plr2_Mouse_b, 0xFF); Mem.wb(Plr2_Joystick_b, 0);
        fillConstantTable();

        // --- préambule Game_Start ---
        Mem.wb(Plr_MultiplayerType_b, 'n');                  // PLR_SINGLE
        ScreenC.Vid_OpenMainScreen();                        // ouvre l'écran + installe l'entrée GLFW
        System.out.println("[MenuTest] écran ouvert");

        long r = FileIo.IO_LoadFile(GLF_DatabaseName_vb);
        Mem.wl(GLF_DatabasePtr_l, FileIo.addr(r));
        loadLevelAssets();

        Controlloop.DEFAULTGAME();
        MenuNb.mnu_copycredz();                              // crédits dans les plans de police
        MenuC.mnu_setscreen();                               // active le menu + fondu d'entrée
        Controlloop.game_SetMenuLevelNames();                // noms de niveaux depuis le GLF

        // --- capture du texte d'intro d'un niveau (TWEENTEXT) ---
        String introLvl = System.getProperty("introText");
        if (introLvl != null) {
            try {
                long st = FileIo.IO_LoadFile(ab3d2.ControlloopData.Game_StoryFile_vb);
                Mem.wl(ab3d2.ControlloopData.Lvl_IntroTextPtr_l, FileIo.addr(st));
                System.out.println("[introText] TEXT_FILE chargé");
            } catch (Throwable t) {
                System.out.println("[introText] échec chargement TEXT_FILE : " + t);
            }
            int lvl = Integer.parseInt(introLvl);
            ScreenC.dumpIntroText(lvl, "intro_screenshot.png");
            System.out.println("[introText] niveau " + lvl + " → intro_screenshot.png");
            ScreenC.Vid_CloseMainScreen();
            return;
        }

        String mf = System.getProperty("menuFrames");
        if (mf != null) {                                    // --- mode capture ---
            String show = System.getProperty("menuShow", "main");
            int which = switch (show) {
                case "custom"   -> ab3d2.data.MenunbData.mnu_MYCUSTOMOPTSMENU;
                case "controls" -> ab3d2.data.MenunbData.mnu_MYCONTROLSONE;
                case "level"    -> ab3d2.data.MenunbData.mnu_MYLEVELMENU;
                case "load"     -> ab3d2.data.MenunbData.mnu_MYLOADMENU;
                case "save"     -> ab3d2.data.MenunbData.mnu_MYSAVEMENU;
                case "demo"     -> ab3d2.data.MenunbData.mnu_DEMOMENU;
                default          -> mnu_MYMAINMENU;
            };
            MenuNb.mnu_openmenu(which);
            int frames = Integer.parseInt(mf);
            for (int i = 0; i < frames; i++) {
                MenuNb.mnu_docursor();
                MenuC.WaitTOF();
            }
            ScreenC.saveLastPresent("menu_screenshot.png");
            ScreenC.Vid_CloseMainScreen();
            return;
        }

        // --- démo interactive du moteur cycler (mnu_domenu) ---
        if (System.getProperty("menuDemo") != null) {
            ab3d2.menu.Menunb.mnu_domenu(ab3d2.data.MenunbData.mnu_DEMOMENU); // ← cycle avec gauche/droite, EXIT pour sortir
            ScreenC.Vid_CloseMainScreen();
            return;
        }

        // --- mode interactif : boucle Game_Start (menu → niveau → menu) ---
        Hires.loopFrameLimit = -1;                           // jeu sans limite de frames
        while (Mem.b(Game_ShouldQuit_b) == 0) {
            Controlloop.game_ReadMainMenu();                 // bloque jusqu'à PLAY/EXIT
            if (Mem.b(Game_ShouldQuit_b) != 0) {
                break;
            }
            // game_DoneMenu : copie le template Plr_* → Plr1/Plr2 puis lance le niveau.
            playSelectedLevel();
            MenuC.mnu_setscreen();                           // retour menu
        }
        ScreenC.Vid_CloseMainScreen();
    }

    /** Charge les assets du niveau courant (textures/sons/objets) selon les noms de fichiers. */
    private static void loadLevelAssets() {
        try {
            FileIo.IO_InitQueue();
            Res.Res_LoadSoundFx();
            Res.Res_LoadWallTextures();
            Res.Res_LoadFloorsAndTextures();
            Res.Res_LoadObjects();
            FileIo.IO_QueueFile(ab3d2.data.DrawData.draw_BackdropImageName_vb,
                    ab3d2.bss.DrawBss.Draw_BackdropImagePtr_l, 0); // backdrop ciel (controlloop.s:79)
            FileIo.IO_FlushQueue();
            Res.Res_PatchSoundFx();
        } catch (Throwable t) {
            System.out.println("[MenuTest] chargement assets partiel (" + t + ")");
        }
    }

    /** game_DoneMenu (simplifié) : distribue le template Plr_*, recharge le niveau, joue. */
    private static void playSelectedLevel() {
        playgameNumber();                                    // Game_LevelNumber = counter
        // REPT 11 / REPT 6 : Plr_Health.. → Plr1/Plr2 (consommables + items)
        for (int i = 0; i < 11; i++) {
            int v = Mem.l(ab3d2.bss.PlayerBss.Plr_Health_w + i * 4);
            Mem.wl(Plr1_Health_w + i * 4, v);
            Mem.wl(Plr2_Health_w + i * 4, v);
        }
        for (int i = 0; i < 6; i++) {
            int v = Mem.l(ab3d2.bss.PlayerBss.Plr_Shield_w + i * 4);
            Mem.wl(Plr1_Shield_w + i * 4, v);
            Mem.wl(Plr2_Shield_w + i * 4, v);
        }
        Controlloop.SETPLAYERS();                            // noms de fichiers du niveau choisi
        loadLevelAssets();                                   // recharge les assets de ce niveau
        FileIo.IO_InitQueue();
        ab3d2.c.DrawC.Draw_ResetGameDisplay();
        Hires.Game_Begin();                                  // joue le niveau (revient à la mort/fin)
    }

    private static void playgameNumber() {
        Mem.ww(Game_LevelNumber_w, Mem.w(Game_LevelCounter_w));
    }

    /** Test : sauvegarde des prefs, corruption en mémoire, rechargement, vérification. */
    private static void prefsRoundTrip() {
        Mem.wl(ab3d2.bss.VidBss.Vid_isRTG, 1);
        // valeurs distinctives à persister
        Mem.wb(ab3d2.ControlloopData.Prefs_OriginalMouse_b, 0xFF);
        Mem.wb(ab3d2.bss.VidBss.Vid_FullScreen_b, 0xFF);
        Mem.ww(ab3d2.data.VidData.Vid_ContrastAdjust_w, 0x0150);
        Mem.wb(ab3d2.ControlloopData.fire_key, 0x42);        // touche remappée
        ab3d2.c.GamePreferences.game_SavePreferences();       // → run/prefs.cfg

        // corruption en mémoire
        Mem.wb(ab3d2.ControlloopData.Prefs_OriginalMouse_b, 0);
        Mem.wb(ab3d2.bss.VidBss.Vid_FullScreen_b, 0);
        Mem.ww(ab3d2.data.VidData.Vid_ContrastAdjust_w, 0x0100);
        Mem.wb(ab3d2.ControlloopData.fire_key, 0x63);

        ab3d2.c.GamePreferences.game_LoadPreferences();       // relit + applique

        boolean ok = Mem.ub(ab3d2.ControlloopData.Prefs_OriginalMouse_b) == 0xFF
                && Mem.ub(ab3d2.bss.VidBss.Vid_FullScreen_b) == 0xFF
                && Mem.uw(ab3d2.data.VidData.Vid_ContrastAdjust_w) == 0x0150
                && Mem.ub(ab3d2.ControlloopData.fire_key) == 0x42;
        System.out.println("[prefsTest] OriginalMouse=" + Mem.ub(ab3d2.ControlloopData.Prefs_OriginalMouse_b)
                + " FullScreen=" + Mem.ub(ab3d2.bss.VidBss.Vid_FullScreen_b)
                + " Contrast=0x" + Integer.toHexString(Mem.uw(ab3d2.data.VidData.Vid_ContrastAdjust_w))
                + " fire_key=0x" + Integer.toHexString(Mem.ub(ab3d2.ControlloopData.fire_key)));
        System.out.println("[prefsTest] " + (ok ? "OK — préférences persistées et rechargées" : "ÉCHEC"));
    }

    /** Remplissage de ConstantTable_vl (table de mise à l'échelle des objets), cf. _startup. */
    private static void fillConstantTable() {
        int a0 = ConstantTable_vl;
        int d0 = 1;
        int d1 = 8191;
        do {
            int d2 = 16384 * 64;
            d2 = d2 / d0;
            int d3 = 64 * 64 * 65536;
            d3 = d3 / d2;
            Mem.wl(a0, d3); a0 += 4;
            d2 = d2 >> 1;
            d2 = d2 - 40 * 64;
            d2 = d2 * d3;
            d2 = d2 >> 6;
            Mem.wl(a0, d2); a0 += 4;
            d0 += 1;
        } while (--d1 != -1);
    }
}
