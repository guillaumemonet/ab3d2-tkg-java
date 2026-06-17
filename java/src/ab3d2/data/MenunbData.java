package ab3d2.data;

import ab3d2.Mem;
import ab3d2.Assets;
import ab3d2.bss.MenunbBss;

/**
 * Données de ab3d2_source/menu/menunb.s.
 *
 * Palettes/fond/police sont des incbin (couleurs ULONG 0x00RRGGBB ; police 3 plans
 * 16x16). Les structures de menu (mnu_MY*MENU) suivent le layout ASM : en-tête 16
 * octets [X.w,Y.w,textPtr.l,curX.w,curY.w,spread.w,items.w] puis 7 paires
 * (type.l,data.l). Les textes sont des lignes de 20 caractères terminées par 1
 * (saut de ligne pour mnu_printxy), la dernière par 0.
 */
public final class MenunbData {

    public static final int mnu_fontpal;   // ULONG[8] - menu/font16x16.pal2
    public static final int mnu_firepal;   // ULONG[8] - menu/firepal.pal2
    public static final int mnu_backpal;   // ULONG[4] - menu/back.pal
    public static final int mnu_background; // menu/back2.raw (2x320x256 bitplanes)
    public static final int mnu_font;       // menu/font16x16.raw2 (3 plans, 176x40 chacun)
    public static final int mnu_frame;      // menu/credits_only.raw (3 plans 320x192)

    public static final int main_counter;   // ULONG (dc.l 0) — compteur de frame
    public static final int mnu_rnd;         // WORD — index aléatoire courant
    public static final int mnu_rndptr;      // ULONG — pointeur source aléatoire

    // -- structures de menu (controlloop) --
    public static final int mnu_MYMAINMENU;
    public static final int mnu_MYMAINMENUTEXT;
    public static final int mnu_CURRENTLEVELLINE;  // ligne 4 de MYMAINMENUTEXT (nom du niveau courant)
    public static final int mnu_MYLEVELMENU;
    public static final int mnu_MYLEVELMENUTEXT;
    public static final int mnu_LevelAName_vb;     // 8 noms A..H (21 octets d'écart : 20 + sep)
    public static final int mnu_MYLEVELMENU2;
    public static final int mnu_MYLEVELMENUTEXT2;
    public static final int mnu_LevelIName_vb;     // 8 noms I..P

    // -- menus options / contrôles --
    public static final int mnu_MYCUSTOMOPTSMENU;
    public static final int optionLines;           // ligne 2 du texte custom (Y/N à l'offset 17)
    public static final int mnu_MYCONTROLSONE;
    public static final int KEY_LINES;             // texte contrôles page 1 (glyphe touche à l'offset 17)
    public static final int mnu_MYCONTROLSTWO;
    public static final int KEY_LINES2;            // texte contrôles page 2

    // -- menus load/save --
    public static final int mnu_MYLOADMENU;
    public static final int mnu_LSLOTA;            // ligne « NEW GAME » (item 0) ; slots 1..5 suivent (+21)
    public static final int mnu_MYSAVEMENU;
    public static final int mnu_SSLOTA;            // slot 1 (item 0) ; slots 2..5 suivent (+21)

    // -- variables runtime --
    public static final int mnu_currentsel;
    public static final int mnu_currentlevel;      // = mnu_level (0=A,1=B,...)
    public static final int mnu_curx, mnu_cury, mnu_spread, mnu_items;
    public static final int mnu_arrow, mnu_cleararrow;
    public static final int mnu_row, mnu_oldrow;
    public static final int mnu_screenpos;
    public static final int mnu_printdelay;
    public static final int mnu_timer;             // 'timer' : compte à rebours VBlank
    public static final int mnu_frameptr;          // ptr courant dans l'anim curseur
    public static final int mnu_count;             // compteur de passe du feu
    public static final int mnu_subtract;          // décalage A du feu (LONG)
    public static final int mnu_sourceptrs;        // 3 longs (plans sources du feu)
    public static final int mnu_mainleveltext;     // octet poké par mnu_openmenu (legacy)
    public static final int mnu_fadefactor;        // facteur de fondu courant (0..256)

    // -- sliders / cyclers --
    public static final int mnu_sliderwidth;
    public static final int mnu_sliddat;           // graphique du curseur de slider (glyphe police)
    public static final int mnu_leftslider, mnu_sliderspace, mnu_rightslider;
    public static final int mnu_playercycler;      // cycler type de joueur (1P/2P master/slave)
    public static final int mnu_playtype, mnu_2plevel;
    // menu démo (slider + cycler) — non présent dans le jeu d'origine (démonstration moteur)
    public static final int mnu_DEMOMENU;
    public static final int mnu_demoSliderVal;

    // -- mnu_plot : étincelles sinusoïdales du feu --
    public static final int mnu_sines;             // table de 512 mots (sinus, amplitude ~0x3ff)
    public static final int mnu_xsine0;            // 4 mots contigus : xsine0, xsine1, ysine0, ysine1

    // -- animations curseur --
    public static final int mnu_cursanim;
    public static final int mnu_errcursanim;
    public static final int mnu_buttonanim;
    public static final int mnu_errbutanim;

    /** Pad/tronque une chaîne à 20 caractères (espaces). */
    private static String pad20(String s) {
        if (s.length() > 20) return s.substring(0, 20);
        StringBuilder b = new StringBuilder(s);
        while (b.length() < 20) b.append(' ');
        return b.toString();
    }

    /** Écrit une ligne de menu (20 octets) + octet séparateur ; renvoie l'adresse de la ligne. */
    private static int line(String s, int term) {
        int a = Mem.dcStr(pad20(s));   // 20 octets
        Mem.dcB(term);                 // séparateur (1 = saut de ligne, 0 = fin)
        return a;
    }

    /** Ligne « contrôle » : libellé (17 car) + glyphe touche + 2 espaces (=20) + séparateur. */
    private static int lineKey(String label, int glyph) {
        StringBuilder b = new StringBuilder(label);
        while (b.length() < 17) b.append(' ');
        int a = Mem.dcStr(b.substring(0, 17));  // 17 octets de libellé
        Mem.dcB(glyph);                         // glyphe de la touche (offset 17)
        Mem.dcStr("  ");                        // 2 espaces
        Mem.dcB(1);                             // séparateur
        return a;
    }

    /** Réserve n longs à zéro (ds.l n) — paires (type,data) du menu, toutes nulles. */
    private static void zerosL(int n) {
        for (int i = 0; i < n; i++) Mem.dcL(0);
    }

    static {
        mnu_fontpal = Assets.incbin("menu/font16x16.pal2");
        mnu_firepal = Assets.incbin("menu/firepal.pal2");
        mnu_backpal = Assets.incbin("menu/back.pal");
        mnu_background = Assets.incbin("menu/back2.raw");
        mnu_font = Assets.incbin("menu/font16x16.raw2");
        mnu_frame = Assets.incbin("menu/credits_only.raw");

        main_counter = Mem.dcL(0);
        mnu_rnd = Mem.dcW(0);
        mnu_rndptr = Mem.dcL(MenunbBss.mnu_morescreen + 6 * 40 * 256);

        // ---- texte du menu principal (lignes contiguës) ----
        mnu_MYMAINMENUTEXT = line("", 1);
        line("", 1);
        line("     PLAY  GAME", 1);
        line("      1 PLAYER", 1);
        mnu_CURRENTLEVELLINE = line("", 1);          // rempli par game_SetMenuLevelName
        line("  CONTROL  OPTIONS", 1);
        line("    GAME CREDITS", 1);
        line("   LOAD  POSITION", 1);
        line("   SAVE  POSITION", 1);
        line("   CUSTOM OPTIONS", 1);
        line("        EXIT", 1);
        line("", 0);

        // ---- structure du menu principal ----
        mnu_MYMAINMENU = Mem.dcW(0, 0);              // X=0, Y=0
        Mem.dcL(mnu_MYMAINMENUTEXT);                 // textPtr
        Mem.dcW(0, 40);                              // curX=0, curY=40
        Mem.dcW(20);                                 // spread=20
        Mem.dcW(9);                                  // items=9
        for (int i = 0; i < 7; i++) Mem.dcL(0, 0);   // 7 paires (type,data) = 0

        // ---- texte du menu de niveaux (page 1) ----
        mnu_MYLEVELMENUTEXT = line("", 1);
        line("", 1);
        mnu_LevelAName_vb = line("      LEVEL  A", 1);
        line("      LEVEL  B", 1);
        line("      LEVEL  C", 1);
        line("      LEVEL  D", 1);
        line("      LEVEL  E", 1);
        line("      LEVEL  F", 1);
        line("      LEVEL  G", 1);
        line("      LEVEL  H", 1);
        line("     NEXT  PAGE", 1);
        line("", 0);

        mnu_MYLEVELMENU = Mem.dcW(0, 0);
        Mem.dcL(mnu_MYLEVELMENUTEXT);
        Mem.dcW(0, 40);
        Mem.dcW(20);
        Mem.dcW(9);
        for (int i = 0; i < 7; i++) Mem.dcL(0, 0);

        // ---- texte du menu de niveaux (page 2) ----
        mnu_MYLEVELMENUTEXT2 = line("", 1);
        line("", 1);
        mnu_LevelIName_vb = line("      LEVEL  I", 1);
        line("      LEVEL  J", 1);
        line("      LEVEL  K", 1);
        line("      LEVEL  L", 1);
        line("      LEVEL  M", 1);
        line("      LEVEL  N", 1);
        line("      LEVEL  O", 1);
        line("      LEVEL  P", 1);
        line("     MAIN  MENU", 1);
        line("", 0);

        mnu_MYLEVELMENU2 = Mem.dcW(0, 0);
        Mem.dcL(mnu_MYLEVELMENUTEXT2);
        Mem.dcW(0, 40);
        Mem.dcW(20);
        Mem.dcW(9);
        for (int i = 0; i < 7; i++) Mem.dcL(0, 0);

        // ---- menu CUSTOM OPTIONS (toggles Y/N) ----
        int customText = line("", 1);
        line("", 1);
        optionLines = line("  ORIGINAL MOUSE", 1);   // Y/N écrit à l'offset 17
        line("  ALWAYS RUN", 1);
        line("  SHOW MESSAGES", 1);
        line("  NO AUTO AIM", 1);
        line("  SHOW FPS", 1);
        line("  HIDE WEAPON", 1);
        line("  OPTION 7", 1);
        line("  OPTION 8", 1);
        line("     MAIN  MENU", 0);
        mnu_MYCUSTOMOPTSMENU = Mem.dcW(0, 0);
        Mem.dcL(customText);
        Mem.dcW(0, 40);
        Mem.dcW(20);
        Mem.dcW(9);
        zerosL(18);                                  // ds.l 18 (9 paires type/data nulles)

        // ---- menu CONTROL OPTIONS page 1 (11 touches + MORE) ----
        KEY_LINES = lineKey("  TURN LEFT", 132 + 0x4f);
        lineKey("  TURN RIGHT", 132 + 0x4e);
        lineKey("  FORWARDS", 132 + 0x4c);
        lineKey("  BACKWARDS", 132 + 0x4d);
        lineKey("  FIRE", 132 + 0x65);
        lineKey("  OPERATE", 132 + 0x40);
        lineKey("  RUN", 132 + 0x61);
        lineKey("  FORCE S/S", 132 + 0x67);
        lineKey("  S/S LEFT", 132 + 0x39);
        lineKey("  S/S RIGHT", 132 + 0x3a);
        lineKey("  CROUCH", 132 + 0x22);
        line("        MORE", 0);
        mnu_MYCONTROLSONE = Mem.dcW(0, 0);
        Mem.dcL(KEY_LINES);
        Mem.dcW(0, 0);
        Mem.dcW(20);
        Mem.dcW(12);
        zerosL(24);                                  // ds.l 24 (12 paires)

        // ---- menu CONTROL OPTIONS page 2 (6 touches + MAIN MENU) ----
        KEY_LINES2 = lineKey("  LOOK BEHIND", 132 + 0x28);
        lineKey("  JUMP", 132 + 0x0f);
        lineKey("  LOOK UP", 132 + 27);
        lineKey("  LOOK DOWN", 132 + 42);
        lineKey("  CENTRE VIEW", 132 + 41);
        lineKey("  NEXT WEAPON", 132 + 68);
        line("     MAIN  MENU", 0);
        mnu_MYCONTROLSTWO = Mem.dcW(0, 40);
        Mem.dcL(KEY_LINES2);
        Mem.dcW(0, 40);
        Mem.dcW(20);
        Mem.dcW(7);
        zerosL(14);                                  // ds.l 14 (7 paires)

        // ---- menu LOAD POSITION (NEW GAME + 5 slots + CANCEL) ----
        int loadText = line("   LOAD  POSITION", 1);   // titre (Y=40)
        mnu_LSLOTA = line("      NEW GAME", 1);         // item 0
        line("", 1); line("", 1); line("", 1); line("", 1); line("", 1); // slots 1..5 (remplis au runtime)
        line("       CANCEL", 0);                       // item 6
        mnu_MYLOADMENU = Mem.dcW(0, 40);
        Mem.dcL(loadText);
        Mem.dcW(0, 60);
        Mem.dcW(20);
        Mem.dcW(7);
        zerosL(14);

        // ---- menu SAVE POSITION (5 slots + CANCEL) ----
        int saveText = line("   SAVE  POSITION", 1);   // titre (Y=40)
        mnu_SSLOTA = line("", 1);                       // item 0 = slot 1
        line("", 1); line("", 1); line("", 1); line("", 1); // slots 2..5
        line("       CANCEL", 0);                       // item 5
        mnu_MYSAVEMENU = Mem.dcW(0, 40);
        Mem.dcL(saveText);
        Mem.dcW(0, 60);
        Mem.dcW(20);
        Mem.dcW(6);
        zerosL(14);

        // ---- mnu_plot : oscillateurs + table de sinus (512 mots, menunb.s:1146) ----
        mnu_xsine0 = Mem.dcW(0, 0, 0, 0);            // xsine0, xsine1, ysine0, ysine1
        mnu_sines = Mem.dcW(
            0x0006,0x0013,0x001f,0x002c,0x0038,0x0045,0x0052,0x005e,0x006b,0x0077,0x0083,0x0090,0x009c,0x00a9,0x00b5,0x00c1,
            0x00ce,0x00da,0x00e6,0x00f2,0x00ff,0x010b,0x0117,0x0123,0x012f,0x013b,0x0147,0x0153,0x015f,0x016a,0x0176,0x0182,
            0x018d,0x0199,0x01a4,0x01b0,0x01bb,0x01c6,0x01d2,0x01dd,0x01e8,0x01f3,0x01fe,0x0209,0x0213,0x021e,0x0229,0x0233,
            0x023e,0x0248,0x0252,0x025c,0x0266,0x0270,0x027a,0x0284,0x028e,0x0297,0x02a1,0x02aa,0x02b4,0x02bd,0x02c6,0x02cf,
            0x02d8,0x02e1,0x02e9,0x02f2,0x02fa,0x0303,0x030b,0x0313,0x031b,0x0323,0x032a,0x0332,0x0339,0x0341,0x0348,0x034f,
            0x0356,0x035d,0x0364,0x036a,0x0371,0x0377,0x037d,0x0383,0x0389,0x038f,0x0395,0x039a,0x039f,0x03a5,0x03aa,0x03af,
            0x03b4,0x03b8,0x03bd,0x03c1,0x03c5,0x03c9,0x03cd,0x03d1,0x03d5,0x03d8,0x03dc,0x03df,0x03e2,0x03e5,0x03e7,0x03ea,
            0x03ed,0x03ef,0x03f1,0x03f3,0x03f5,0x03f7,0x03f8,0x03f9,0x03fb,0x03fc,0x03fd,0x03fd,0x03fe,0x03ff,0x03ff,0x03ff,
            0x03ff,0x03ff,0x03ff,0x03fe,0x03fd,0x03fd,0x03fc,0x03fb,0x03f9,0x03f8,0x03f7,0x03f5,0x03f3,0x03f1,0x03ef,0x03ed,
            0x03ea,0x03e7,0x03e5,0x03e2,0x03df,0x03dc,0x03d8,0x03d5,0x03d1,0x03cd,0x03c9,0x03c5,0x03c1,0x03bd,0x03b8,0x03b4,
            0x03af,0x03aa,0x03a5,0x039f,0x039a,0x0395,0x038f,0x0389,0x0383,0x037d,0x0377,0x0371,0x036a,0x0364,0x035d,0x0356,
            0x034f,0x0348,0x0341,0x0339,0x0332,0x032a,0x0323,0x031b,0x0313,0x030b,0x0303,0x02fa,0x02f2,0x02e9,0x02e1,0x02d8,
            0x02cf,0x02c6,0x02bd,0x02b4,0x02aa,0x02a1,0x0297,0x028e,0x0284,0x027a,0x0270,0x0266,0x025c,0x0252,0x0248,0x023e,
            0x0233,0x0229,0x021e,0x0213,0x0209,0x01fe,0x01f3,0x01e8,0x01dd,0x01d2,0x01c6,0x01bb,0x01b0,0x01a4,0x0199,0x018d,
            0x0182,0x0176,0x016a,0x015f,0x0153,0x0147,0x013b,0x012f,0x0123,0x0117,0x010b,0x00ff,0x00f2,0x00e6,0x00da,0x00ce,
            0x00c1,0x00b5,0x00a9,0x009c,0x0090,0x0083,0x0077,0x006b,0x005e,0x0052,0x0045,0x0038,0x002c,0x001f,0x0013,0x0006,
            0xfffa,0xffed,0xffe1,0xffd4,0xffc8,0xffbb,0xffae,0xffa2,0xff95,0xff89,0xff7d,0xff70,0xff64,0xff57,0xff4b,0xff3f,
            0xff32,0xff26,0xff1a,0xff0e,0xff01,0xfef5,0xfee9,0xfedd,0xfed1,0xfec5,0xfeb9,0xfead,0xfea1,0xfe96,0xfe8a,0xfe7e,
            0xfe73,0xfe67,0xfe5c,0xfe50,0xfe45,0xfe3a,0xfe2e,0xfe23,0xfe18,0xfe0d,0xfe02,0xfdf7,0xfded,0xfde2,0xfdd7,0xfdcd,
            0xfdc2,0xfdb8,0xfdae,0xfda4,0xfd9a,0xfd90,0xfd86,0xfd7c,0xfd72,0xfd69,0xfd5f,0xfd56,0xfd4c,0xfd43,0xfd3a,0xfd31,
            0xfd28,0xfd1f,0xfd17,0xfd0e,0xfd06,0xfcfd,0xfcf5,0xfced,0xfce5,0xfcdd,0xfcd6,0xfcce,0xfcc7,0xfcbf,0xfcb8,0xfcb1,
            0xfcaa,0xfca3,0xfc9c,0xfc96,0xfc8f,0xfc89,0xfc83,0xfc7d,0xfc77,0xfc71,0xfc6b,0xfc66,0xfc61,0xfc5b,0xfc56,0xfc51,
            0xfc4c,0xfc48,0xfc43,0xfc3f,0xfc3b,0xfc37,0xfc33,0xfc2f,0xfc2b,0xfc28,0xfc24,0xfc21,0xfc1e,0xfc1b,0xfc18,0xfc16,
            0xfc13,0xfc11,0xfc0f,0xfc0d,0xfc0b,0xfc09,0xfc08,0xfc07,0xfc05,0xfc04,0xfc03,0xfc03,0xfc02,0xfc01,0xfc01,0xfc01,
            0xfc01,0xfc01,0xfc01,0xfc02,0xfc03,0xfc03,0xfc04,0xfc05,0xfc07,0xfc08,0xfc09,0xfc0b,0xfc0d,0xfc0f,0xfc11,0xfc13,
            0xfc16,0xfc19,0xfc1b,0xfc1e,0xfc21,0xfc24,0xfc28,0xfc2b,0xfc2f,0xfc33,0xfc37,0xfc3b,0xfc3f,0xfc43,0xfc48,0xfc4c,
            0xfc51,0xfc56,0xfc5b,0xfc61,0xfc66,0xfc6b,0xfc71,0xfc77,0xfc7d,0xfc83,0xfc89,0xfc8f,0xfc96,0xfc9c,0xfca3,0xfcaa,
            0xfcb1,0xfcb8,0xfcbf,0xfcc7,0xfcce,0xfcd6,0xfcdd,0xfce5,0xfced,0xfcf5,0xfcfd,0xfd06,0xfd0e,0xfd17,0xfd1f,0xfd28,
            0xfd31,0xfd3a,0xfd43,0xfd4c,0xfd56,0xfd5f,0xfd69,0xfd72,0xfd7c,0xfd86,0xfd90,0xfd9a,0xfda4,0xfdae,0xfdb8,0xfdc2,
            0xfdcd,0xfdd7,0xfde2,0xfded,0xfdf7,0xfe02,0xfe0d,0xfe18,0xfe23,0xfe2e,0xfe3a,0xfe45,0xfe50,0xfe5c,0xfe67,0xfe73,
            0xfe7e,0xfe8a,0xfe96,0xfea1,0xfead,0xfeb9,0xfec5,0xfed1,0xfedd,0xfee9,0xfef5,0xff01,0xff0e,0xff1a,0xff26,0xff32,
            0xff3f,0xff4b,0xff57,0xff64,0xff70,0xff7d,0xff89,0xff95,0xffa2,0xffae,0xffbb,0xffc8,0xffd4,0xffe1,0xffed,0xfffa);

        // ---- graphiques slider + cyclers ----
        mnu_sliderwidth = Mem.dcW(6);
        mnu_sliddat = Mem.dcL(mnu_font + 40 * 16 + 7 * 2);
        mnu_leftslider = Mem.dcB(58, 0);
        mnu_sliderspace = Mem.dcbB(20, 0);
        mnu_rightslider = Mem.dcB(60, 0);
        mnu_2plevel = Mem.dcW(0);
        mnu_playtype = Mem.dcW(0);
        int pt0 = Mem.dcStr("1 Player       "); Mem.dcB(0);   // mnu_playtype0
        int pt1 = Mem.dcStr("2 Player master"); Mem.dcB(0);   // mnu_playtype1
        int pt2 = Mem.dcStr("2 Player slave "); Mem.dcB(0);   // mnu_playtype2
        // cycler type de joueur : Xadd,Yadd ; #items ; ptr valeur ; ptr textes[]
        mnu_playercycler = Mem.dcW(2, 2);
        Mem.dcW(3);
        Mem.dcL(mnu_playtype);
        Mem.dcL(pt0, pt1, pt2);

        // menu DÉMO (cycler) — pas dans le jeu d'origine, pour démontrer le moteur
        mnu_demoSliderVal = Mem.dcW(32);
        int demoText = line("   CYCLER  DEMO", 1);
        line("", 1);
        line("  PLAYER:", 1);                        // item 0 (cycler dessiné par-dessus)
        line("       EXIT", 1);                      // item 1
        line("", 0);
        mnu_DEMOMENU = Mem.dcW(0, 0);
        Mem.dcL(demoText);
        Mem.dcW(0, 40);                              // curX=0, curY=40
        Mem.dcW(20);                                 // spread
        Mem.dcW(2);                                  // items
        Mem.dcL(5, mnu_playercycler);               // item 0 : cycler
        Mem.dcL(2, 0);                              // item 1 : EXIT

        // ---- variables runtime (valeurs initiales de menunb.s) ----
        mnu_currentsel = Mem.dcW(0);
        mnu_currentlevel = Mem.dcW(0);
        mnu_curx = Mem.dcW(5);
        mnu_cury = Mem.dcW(78);
        mnu_spread = Mem.dcW(40);
        mnu_items = Mem.dcW(3);
        mnu_arrow = Mem.dcB(' ', 0);
        mnu_cleararrow = Mem.dcB(' ', 0);
        mnu_row = Mem.dcW(30000);
        mnu_oldrow = Mem.dcW(30000);
        mnu_screenpos = Mem.dcW(0);
        mnu_printdelay = Mem.dcL(0);
        mnu_timer = Mem.dcL(0);
        mnu_count = Mem.dcW(0);
        mnu_subtract = Mem.dcL(0);
        mnu_mainleveltext = Mem.dcB(0);
        mnu_fadefactor = Mem.dcW(0);

        // ---- animations curseur (octets de glyphe ; suffixe <=40 = boucle arrière) ----
        mnu_cursanim = Mem.dcB(130, 129, 128, 127, 126, 125, 124, 123, 8);
        mnu_errcursanim = Mem.dcB(
            240, 240, 241, 241, 242, 242, 243, 243,
            240, 240, 241, 241, 242, 242, 243, 243,
            240, 240, 241, 241, 242, 242, 243, 243,
            240, 240, 241, 241, 242, 242, 243, 243);
        mnu_buttonanim = Mem.dcB(
            236, 236, 236, 236, 237, 237, 237, 237,
            238, 238, 238, 238, 239, 239, 239, 239,
            238, 238, 238, 238, 237, 237, 237, 237, 24);
        mnu_errbutanim = Mem.dcB(
            240, 240, 241, 241, 242, 242, 243, 243,
            240, 240, 241, 241, 242, 242, 243, 243,
            240, 240, 241, 241, 242, 242, 243, 243,
            240, 240, 241, 241, 242, 242, 243, 243);

        // frameptr initial = mnu_cursanim
        mnu_frameptr = Mem.dcL(mnu_cursanim);
        // sourceptrs initiaux = morescreen + (3,4,5)*PLANESIZE + speed*ROWSIZE
        mnu_sourceptrs = Mem.dcL(
            MenunbBss.mnu_morescreen + 3 * 40 * 256 + 40,
            MenunbBss.mnu_morescreen + 4 * 40 * 256 + 40,
            MenunbBss.mnu_morescreen + 5 * 40 * 256 + 40);
    }

    private MenunbData() {
    }
}
