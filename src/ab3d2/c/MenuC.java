package ab3d2.c;

import ab3d2.Mem;
import ab3d2.modules.Sys;

import static ab3d2.bss.VidBss.mnu_palette;
import static ab3d2.bss.MenunbBss.mnu_screen;
import static ab3d2.bss.MenunbBss.mnu_morescreen;
import static ab3d2.data.MenunbData.mnu_fontpal;
import static ab3d2.data.MenunbData.mnu_firepal;
import static ab3d2.data.MenunbData.mnu_backpal;
import static ab3d2.data.MenunbData.mnu_background;
import static ab3d2.data.MenunbData.mnu_count;
import static ab3d2.data.MenunbData.mnu_subtract;
import static ab3d2.data.MenunbData.mnu_sourceptrs;
import static ab3d2.data.MenunbData.mnu_rndptr;
import static ab3d2.data.MenunbData.mnu_rnd;
import static ab3d2.data.MenunbData.main_counter;
import static ab3d2.data.MenunbData.mnu_screenpos;
import static ab3d2.data.MenunbData.mnu_fadefactor;
import static ab3d2.data.MenunbData.mnu_timer;

/**
 * Traduction de ab3d2_source/c/menu.c.
 *
 * Effet de feu (blitter custom mnu_pass1..4), défilement du fond, fondus, et
 * bascule d'affichage. Le blitter Amiga est émulé en logiciel pour les blits
 * spécifiques du feu (minterm 0xF8 = D = A | (B & C), canal A décalé de ASH bits).
 * L'affichage RTG (BltBitMapRastPort de la tâche de blit) est remplacé par la
 * conversion planar→chunky de ScreenC.Vid_PresentMenu. Les attentes VBlank (WaitTOF)
 * deviennent une frame hôte (WaitTOF ci-dessous).
 */
public final class MenuC {

    private static final int SCREEN_WIDTH = 320;
    private static final int SCREEN_HEIGHT = 256;
    private static final int ROWSIZE = SCREEN_WIDTH / 8;            // 40
    private static final int PLANESIZE = ROWSIZE * SCREEN_HEIGHT;   // 10240
    private static final int mnu_speed = 1;
    private static final int mnu_size = 256;

    private static boolean mnu_Active = false;

    private MenuC() {
    }

    /**
     * mnu_createpalette — construit mnu_palette[256] : indices ≥0xE0 = couleurs de
     * police ; bits 0x1C = mélange de feu (clampé) ; sinon couleurs de fond.
     */
    public static void mnu_createpalette() {
        for (int c = 0; c < 256; ++c) {                          // for (WORD c = 0; c < 256; ++c)
            if ((c & 0xe0) != 0) {                               // if (c & 0xe0)
                Mem.wl(mnu_palette + c * 4, Mem.l(mnu_fontpal + (c >> 5) * 4)); // mnu_palette[c] = mnu_fontpal[c >> 5]
            } else {
                if ((c & 0x1c) != 0) {                           // if (c & 0x1c)
                    long c1 = Mem.l(mnu_firepal + ((c & 0x1c) >> 2) * 4) & 0xFFFFFFFFL; // ULONG c1 = mnu_firepal[(c & 0x1c) >> 2]
                    long c2 = Mem.l(mnu_firepal + (c & 3) * 4) & 0xFFFFFFFFL;           // ULONG c2 = mnu_firepal[c & 3]

                    long r = (c1 >> 16) + (c2 >> 16);            // ULONG r = (c1 >> 16) + (c2 >> 16)
                    if (r > 255) {
                        r = 255;
                    }
                    long g = (((c1 >> 8) & 0xFF) * 3) / 4 + ((c2 >> 8) & 0xFF); // g = (((c1>>8)&0xFF)*3)/4 + ((c2>>8)&0xFF)
                    if (g > 255) {
                        g = 255;
                    }
                    long b = (c1 & 0xFF) + (c2 & 0xFF);          // b = (c1 & 0xFF) + (c2 & 0xFF)
                    if (b > 255) {
                        b = 255;
                    }
                    Mem.wl(mnu_palette + c * 4, (int) ((r << 16) | (g << 8) | b)); // mnu_palette[c] = (r<<16)|(g<<8)|b
                } else {
                    Mem.wl(mnu_palette + c * 4, Mem.l(mnu_backpal + (c & 3) * 4)); // mnu_palette[c] = mnu_backpal[c & 3]
                }
            }
        }
    }

    /**
     * mnu_init — initialise l'écran du menu : duplique verticalement les 2 plans de
     * fond (pour le défilement) dans mnu_screen, et efface les 3 plans de feu.
     */
    public static void mnu_init() {
        ab3d2.MenuNb.mnu_initrnd();                              // CallAsm(&mnu_initrnd) (menunb.s, traduit)
        mnu_createpalette();                                     // mnu_createpalette()

        int planeSize = PLANESIZE;                               // 10240
        int planeSizeL = planeSize / 4;                          // 2560 (longs)

        int firstPlane = mnu_background;                         // (ULONG*)mnu_background
        int secondPlane = mnu_background + planeSize;            // (ULONG*)(mnu_background + planeSize)
        int outPlanes = mnu_screen;                              // (ULONG*)mnu_screen

        for (int j = 0; j < planeSizeL; ++j) {
            int x = Mem.l(firstPlane + j * 4);                   // ULONG x = firstPlane[j]
            Mem.wl(outPlanes + j * 4, x);                        // outPlanes[j] = x
            Mem.wl(outPlanes + (planeSizeL + j) * 4, x);         // outPlanes[planeSizeL + j] = x
            int y = Mem.l(secondPlane + j * 4);                  // ULONG x = secondPlane[j]
            Mem.wl(outPlanes + (planeSizeL * 2 + j) * 4, y);     // outPlanes[planeSizeL*2 + j] = x
            Mem.wl(outPlanes + (planeSizeL * 3 + j) * 4, y);     // outPlanes[planeSizeL*3 + j] = x
        }

        // memset(mnu_morescreen, 0, planeSize * 3)
        Sys.Sys_MemFillLong(mnu_morescreen, 0, planeSize * 3 / 4);
    }

    /**
     * mnu_setscreen (CALLC) : active l'affichage du menu. Hôte : init des buffers,
     * palette à 0, puis fondu d'entrée (le « blit task » RTG est remplacé par
     * Vid_PresentMenu, et main_vblint par l'appel direct de mnu_vblint dans WaitTOF).
     */
    public static void mnu_setscreen() {
        if (mnu_Active) {
            return;                                              // déjà actif
        }
        mnu_Active = true;
        mnu_init();
        mnu_fade(0);                                             // mnu_fade(0)
        mnu_fadein();                                            // mnu_fadein()
    }

    /** mnu_clearscreen (CALLC) : restaure l'écran de jeu (d0 = fondu de sortie). */
    public static void mnu_clearscreen(int fade) {
        if (!mnu_Active) {
            return;
        }
        mnu_Active = false;
        if (fade != 0) {
            mnu_fadeout();                                       // mnu_fadeout()
        }
        ScreenC.Vid_LoadMainPalette();                           // Vid_LoadMainPalette()
    }

    public static boolean mnu_isActive() {
        return mnu_Active;
    }

    /** mnu_movescreen : fait défiler le fond (avance mnu_screenpos ; l'offset est lu par Vid_PresentMenu). */
    public static void mnu_movescreen() {
        Mem.ww(mnu_screenpos, (Mem.uw(mnu_screenpos) + 1) & 0xFFFF); // mnu_screenpos++
    }

    /** mnu_fade : règle le facteur de fondu courant (0..256) appliqué par Vid_PresentMenu. */
    private static void mnu_fade(int fadeFactor) {
        Mem.ww(mnu_fadefactor, fadeFactor);                      // LoadRGB32(palette * fadeFactor) → différé à la présentation
    }

    private static final int mnu_fadespeed = 16;

    /** mnu_fadein : fondu d'entrée (16 pas de 16, puis facteur plein 256). */
    public static void mnu_fadein() {
        int fadefactor = 0;
        int steps = 256 / mnu_fadespeed;
        for (int i = 0; i < steps; ++i) {
            WaitTOF();
            mnu_fade(fadefactor);
            fadefactor += mnu_fadespeed;
        }
        WaitTOF();
        mnu_fade(256);
    }

    /** mnu_fadeout : fondu de sortie. */
    public static void mnu_fadeout() {
        int fadefactor = 256;
        int steps = 256 / mnu_fadespeed;
        for (int i = 0; i < steps; ++i) {
            WaitTOF();
            mnu_fade(fadefactor);
            fadefactor -= mnu_fadespeed;
        }
        WaitTOF();
        mnu_fade(0);
    }

    /**
     * mnu_dofire — effet de feu. S'exécute une frame sur deux (main_counter&1).
     * Fait tourner les 3 plans sources, choisit le décalage ASH/subtract selon la
     * passe, puis applique 3 blits émulés (D = A_décalé | (B & C)).
     */
    public static void mnu_dofire() {
        if ((Mem.l(main_counter) & 1) != 0) {                    // if (main_counter & 1) return
            return;
        }
        // mnu_rnd += vhposr : pas de faisceau vidéo hôte → substitut variant par frame
        // (ne change que la fenêtre de bruit échantillonnée, pas la logique du feu).
        Mem.ww(mnu_rnd, (Mem.uw(mnu_rnd) + 0xA5 + (Mem.l(main_counter) & 0xFF)) & 0xFFFF);

        // rotation des 3 plans sources
        int s0 = Mem.l(mnu_sourceptrs);
        int s1 = Mem.l(mnu_sourceptrs + 4);
        int s2 = Mem.l(mnu_sourceptrs + 8);
        Mem.wl(mnu_sourceptrs, s1);
        Mem.wl(mnu_sourceptrs + 4, s2);
        Mem.wl(mnu_sourceptrs + 8, s0);

        int cnt = Mem.uw(mnu_count) & 3;                         // cnt = mnu_count++ & 3
        Mem.ww(mnu_count, (Mem.uw(mnu_count) + 1) & 0xFFFF);
        int ash;
        int subtract = 0;
        switch (cnt) {
            case 0:  ash = 1;  break;                            // bltcon0 = 0x1ff8 → ASH=1
            case 1:  ash = 15; subtract = -2; break;             // 0xfff8 → ASH=15, A ptr +2
            default: ash = 0;  break;                            // 0x0ff8 → ASH=0
        }
        Mem.wl(mnu_subtract, subtract);

        // 3 passes : A = sourceptrs[i]-subtract, B = rndptr, C = plan i +1 ligne, D = plan i
        for (int i = 0; i < 3; ++i) {
            ab3d2.MenuNb.getrnd();                               // CallAsm(&getrnd) : avance mnu_rndptr
            int aBase = Mem.l(mnu_sourceptrs + i * 4) - Mem.l(mnu_subtract);
            int bBase = Mem.l(mnu_rndptr);
            int cBase = mnu_morescreen + i * PLANESIZE + mnu_speed * ROWSIZE;
            int dBase = mnu_morescreen + i * PLANESIZE;
            fireBlit(aBase, bBase, cBase, dBase, ash);
        }
    }

    /**
     * Émulation du blit du feu : 255 lignes × 20 mots, modulos nuls (mémoire contiguë).
     * Canal A décalé à droite de `ash` bits (barrel shift ascendant, report du mot
     * précédent) ; D = A_décalé | (B & C). bltafwm = 0xffffffff (pas de masquage).
     */
    private static void fireBlit(int aBase, int bBase, int cBase, int dBase, int ash) {
        int words = (mnu_size - mnu_speed) * (ROWSIZE / 2);      // 255 * 20 = 5100
        int prev = 0;                                            // mot A précédent (report du shift)
        for (int k = 0; k < words; ++k) {
            int curA = Mem.uw(aBase + k * 2);                    // A courant
            int aSh = (((prev << 16) | curA) >>> ash) & 0xFFFF;  // ((prev<<16|cur) >> ASH) & 0xFFFF
            prev = curA;
            int bW = Mem.uw(bBase + k * 2);                      // B (bruit)
            int cW = Mem.uw(cBase + k * 2);                      // C (plan feu +1 ligne)
            Mem.ww(dBase + k * 2, aSh | (bW & cW));              // D = A | (B & C)
        }
    }

    /**
     * WaitTOF — primitive de frame hôte (remplace l'attente VBlank). Avance le
     * compteur de frame et le timer, exécute mnu_vblint (movescreen+dofire+curseur)
     * puis présente l'écran du menu (planar→chunky).
     */
    public static void WaitTOF() {
        Mem.wl(main_counter, Mem.l(main_counter) + 1);           // main_counter++
        if (Mem.l(mnu_timer) > 0) {                              // décrémente le timer (typewriter / attentes)
            Mem.wl(mnu_timer, Mem.l(mnu_timer) - 1);
        }
        ab3d2.MenuNb.mnu_vblint();                               // movescreen + dofire + animcursor + plot
        ScreenC.Vid_PresentMenu();                               // BltBitMapRastPort → planar→chunky hôte
        // Fermeture de la fenêtre pendant un menu → demande de quitter (sortie propre).
        ab3d2.host.Display d = ScreenC.hostDisplay();
        if (d != null && d.shouldClose()) {
            Mem.wb(ab3d2.ControlloopData.Game_ShouldQuit_b, 0xFF);
        }
    }
}
