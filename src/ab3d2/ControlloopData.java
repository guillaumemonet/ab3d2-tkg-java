package ab3d2;

import ab3d2.modules.RawKeyMacros;

/**
 * Données de ab3d2_source/controlloop.s — TRADUCTION PARTIELLE.
 *
 * Bloc PREFERENCES (controlloop.s:223-290) : ce bloc est PERSISTÉ tel quel
 * dans ab3:prefs.cfg (_Prefs_Persisted → _Prefs_PersistedEnd), son layout
 * octet par octet est donc contractuel — traduit d'une pièce, contigu.
 * Le reste de controlloop.s (code + autres données) sera ajouté lors de la
 * traduction du fichier.
 */
public final class ControlloopData {

    // TODO (original) - this should be a structure definition.
    private static final int _a0 = Mem.align(4);

    /** _Prefs_Persisted:: / Prefsfile: dc.b 'k8nx' (magic du fichier prefs) */
    public static final int Prefsfile = Mem.dcStr("k8nx");

    // DCLC Prefs_AssignableKeys_vb (label C) / AssignableKeys_vb:
    public static final int Prefs_AssignableKeys_vb = Mem.allocTop();
    public static final int AssignableKeys_vb = Mem.allocTop();
    public static final int turn_left_key = Mem.dcB(RawKeyMacros.RAWKEY_LEFT);
    public static final int turn_right_key = Mem.dcB(RawKeyMacros.RAWKEY_RIGHT);
    public static final int forward_key = Mem.dcB(RawKeyMacros.RAWKEY_W);
    public static final int backward_key = Mem.dcB(RawKeyMacros.RAWKEY_S);
    public static final int fire_key = Mem.dcB(RawKeyMacros.RAWKEY_CTRL);
    public static final int operate_key = Mem.dcB(RawKeyMacros.RAWKEY_F);
    public static final int run_key = Mem.dcB(RawKeyMacros.RAWKEY_LSHIFT);
    public static final int force_sidestep_key = Mem.dcB(RawKeyMacros.RAWKEY_LALT);
    public static final int sidestep_left_key = Mem.dcB(RawKeyMacros.RAWKEY_A);
    public static final int sidestep_right_key = Mem.dcB(RawKeyMacros.RAWKEY_D);
    public static final int duck_key = Mem.dcB(RawKeyMacros.RAWKEY_C);
    public static final int look_behind_key = Mem.dcB(RawKeyMacros.RAWKEY_L);
    public static final int jump_key = Mem.dcB(RawKeyMacros.RAWKEY_SPACEBAR);
    public static final int look_up_key = Mem.dcB(RawKeyMacros.RAWKEY_EQUAL);
    public static final int look_down_key = Mem.dcB(RawKeyMacros.RAWKEY_UNDERSCORE);
    public static final int centre_view_key = Mem.dcB(RawKeyMacros.RAWKEY_SEMICOLON);
    public static final int next_weapon_key = Mem.dcB(RawKeyMacros.RAWKEY_BSLASH);
    public static final int spare_key = Mem.dcB(0);

    public static final int Prefs_FullScreen_b = Mem.dcB(1); // défaut = grande vue 3D (Vid_FullScreen)
    public static final int Prefs_PixelMode_b = Mem.dcB(0);
    public static final int Prefs_VertMargin_b = Mem.dcB(0);
    public static final int Prefs_SimpleLighting_b = Mem.dcB(0);
    public static final int Prefs_FPSLimit_b = Mem.dcB(0);
    public static final int Prefs_DynamicLights_b = Mem.dcB(255);
    public static final int Prefs_RenderQuality_b = Mem.dcB(255);

    // Padding
    public static final int Prefs_Unused_b = Mem.dcB(0);

    public static final int Prefs_ContrastAdjust_AGA_w = Mem.dcW(0x0100);
    public static final int Prefs_ContrastAdjust_RTG_w = Mem.dcW(0x0100);
    public static final int Prefs_BrightnessOffset_AGA_w = Mem.dcW(0);
    public static final int Prefs_BrightnessOffset_RTG_w = Mem.dcW(0);
    public static final int Prefs_GammaLevel_AGA_b = Mem.dcB(0);
    public static final int Prefs_GammaLevel_RTG_b = Mem.dcB(0);

    // Moved here to be included in the persisted preferences
    public static final int Prefs_CustomOptionsBuffer_vb = Mem.allocTop();
    public static final int Prefs_OriginalMouse_b = Mem.dcB(0);
    public static final int Prefs_AlwaysRun_b = Mem.dcB(0);
    public static final int Prefs_ShowMessages_b = Mem.dcB(255);
    public static final int Prefs_NoAutoAim_b = Mem.dcB(0);
    public static final int Prefs_DisplayFPS_b = Mem.dcB(0);
    public static final int Prefs_ShowWeapon_b = Mem.dcB(0);
    public static final int Prefs_PlayMusic_b = Mem.dcB(255);
    public static final int Prefs_CrossHairColour_b = Mem.dcB(1);

    private static final int _a1 = Mem.align(4);

    /** _Prefs_PersistedEnd:: / PrefsfileEnd: */
    public static final int PrefsfileEnd = Mem.allocTop();

    public static final int templeftkey = Mem.dcB(0);
    public static final int temprightkey = Mem.dcB(0);
    public static final int tempslkey = Mem.dcB(0);
    public static final int tempsrkey = Mem.dcB(0);

    private static final int _a2 = Mem.align(2); // even

    // ---- controlloop.s:509-514 ----
    public static final int Lvl_DefFilename_vb = Mem.dcStr("ab3:levels/level_");
    public static final int Lvl_DefFilenameX_vb;
    public static final int LOADEXT;
    private static final int _a3;
    public static final int DEFGAMEPOS;
    public static final int DEFGAMELEN;

    // ---- controlloop.s:642-645 ----
    public static final int Game_ShouldQuit_b;       // dc.w 0, accédé en byte
    public static final int game_LevelSelected_w;

    // ---- controlloop.s:1227-1236 ----
    public static final int FADEAMOUNT;
    public static final int FADEVAL;
    public static final int Game_StoryFile_vb;       // dc.b 'ab3:includes/TEXT_FILE' (SANS terminateur, suivi de even)
    private static final int _a4;
    public static final int Lvl_IntroTextPtr_l;

    /** controlloop.s:1224 — _Game_LevelNumber:: / Game_LevelNumber_w: dc.w 0 */
    public static final int Game_LevelNumber_w;

    /** controlloop.s:968 — Game_LevelCounter_w: dc.w 0 (nombre de niveaux débloqués). */
    public static final int Game_LevelCounter_w;

    /** controlloop.s:39 — Game_FinishedLevel_b: dc.w 0 (drapeau, accédé en byte ; posé par endlevel). */
    public static final int Game_FinishedLevel_b;

    static {
        Lvl_DefFilenameX_vb = Mem.dcStr("a/deflev.dat");
        Mem.dcB(0);
        LOADEXT = Mem.dcB(0);
        _a3 = Mem.align(2); // even
        DEFGAMEPOS = Mem.dcL(0);
        DEFGAMELEN = Mem.dcL(0);

        Game_ShouldQuit_b = Mem.dcW(0);
        game_LevelSelected_w = Mem.dcW(0);

        FADEAMOUNT = Mem.dcW(0);
        FADEVAL = Mem.dcW(0);
        Game_StoryFile_vb = Mem.dcStr("ab3:includes/TEXT_FILE");
        _a4 = Mem.align(2); // even (l'octet de padding sert de terminateur)
        Lvl_IntroTextPtr_l = Mem.dcL(0);

        Game_LevelNumber_w = Mem.dcW(0);
        Game_LevelCounter_w = Mem.dcW(0);
        Game_FinishedLevel_b = Mem.dcW(0);
    }

    private ControlloopData() {
    }
}
