package ab3d2.c;

import ab3d2.Mem;
import ab3d2.host.Display;

import static ab3d2.data.DrawData.draw_Palette_vw;
import static ab3d2.bss.VidBss.Vid_LoadRGB32Struct_vl;
import static ab3d2.bss.VidBss.Vid_isRTG;
import static ab3d2.bss.VidBss.Vid_ScreenWidth;
import static ab3d2.bss.VidBss.Vid_ScreenHeight;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.data.VidData.Vid_GammaIncTables_vb;
import static ab3d2.data.VidData.Vid_ContrastAdjust_w;
import static ab3d2.data.VidData.Vid_BrightnessOffset_w;
import static ab3d2.data.VidData.Vid_GammaLevel_b;
import static ab3d2.data.VidData.SMALL_YPOS;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.VidBss.Vid_LetterBoxMarginHeight_w;
import static ab3d2.bss.VidBss.mnu_palette;
import static ab3d2.bss.MenunbBss.mnu_screen;
import static ab3d2.bss.MenunbBss.mnu_morescreen;
import static ab3d2.data.MenunbData.mnu_screenpos;
import static ab3d2.data.MenunbData.mnu_fadefactor;
import static ab3d2.modules.C2pData.Game_TeleportFrame_w;
import static ab3d2.modules.C2pData.C2P_Teleporting_b;
import static ab3d2.data.DrawData.draw_TeleportShimmerFXData_vb;
import static ab3d2.bss.TablesBss.KeyMap_vb;

/**
 * Traduction de ab3d2_source/c/screen.c.
 *
 * screen.c est la couche d'affichage AmigaOS : ouverture d'écran/fenêtre
 * (OpenScreenTags/AllocRaster), copper lists double-hauteur, requester ASL de
 * mode d'affichage, présentation/flip et C2P (EXCLU du portage). Ces fonctions
 * relèvent de la couche hôte et restent en stubs documentés.
 *
 * La LOGIQUE PURE est traduite : Vid_LoadMainPalette (pipeline palette gamma +
 * contraste + luminosité → Vid_LoadRGB32Struct_vl). Seul LoadRGB32 (upload au
 * ViewPort) est un no-op hôte. Les globaux C Vid_ScreenHeight/Width/ScreenMode/
 * isRTG/SMALL_YPOS sont dans VidBss/VidData.
 */
public final class ScreenC {

    /** screen.h : dimensions de l'écran custom (320x256x8 bits). */
    public static final int SCREEN_WIDTH = 320;
    public static final int SCREEN_HEIGHT = 256;

    /**
     * ModeID factice valide renvoyé par GetScreenMode (≠ INVALID_ID 0xFFFFFFFF).
     * Sur Amiga ce serait un ModeID DTAG ; ici l'afficheur LWJGL ignore sa valeur,
     * il sert juste à satisfaire le contrôle de MainC.run.
     */
    public static final int HOST_SCREEN_MODE = 0x00029000; // ~ PAL hires-laced, arbitraire

    /** Facteur d'agrandissement de la fenêtre hôte (320x256 → 960x768 par défaut). */
    private static int hostScale = 3;

    /** Afficheur hôte (null tant que Vid_OpenMainScreen n'a pas été appelé). */
    private static Display display;

    /** Palette hôte (256 couleurs ARGB), reconstruite par LoadRGB32. */
    private static final int[] hostPalette = new int[256];

    /** DIAG : couleur ARGB d'un index de palette hôte. */
    public static int hostColor(int idx) {
        return hostPalette[idx & 0xFF];
    }

    /** Tampon ARGB réutilisé par Vid_Present (évite une allocation par frame). */
    private static int[] presentBuffer;

    /** Tampon chunky pour l'effet de dématérialisation (FastBuffer distordu via shimmerfile). */
    private static final int Vid_ShimmerBuffer = Mem.alloc(SCREEN_WIDTH * SCREEN_HEIGHT);

    /** DIAG : force la frame de shimmer téléport à chaque présentation (-1 = off). */
    public static int dbgForceTeleFrame = -1;

    private ScreenC() {
    }

    /** Règle le facteur d'agrandissement de la fenêtre (à appeler avant Vid_OpenMainScreen). */
    public static void setHostScale(int scale) {
        hostScale = scale;
    }

    /** Accès à l'afficheur hôte (pour brancher l'entrée GLFW en Phase 3). */
    public static Display hostDisplay() {
        return display;
    }

    /** Capture le buffer chunky courant (Vid_FastBufferPtr_l) → PNG (diagnostic). */
    public static void saveScreenshot(String path) {
        // Frame composée (bordure HUD + vue 3D + HUD + messages) dans le bitmap écran hôte.
        int bm = ab3d2.c.DrawC.Vid_HostBitmap;
        if (bm == 0) {
            System.err.println("[saveScreenshot] bitmap écran non alloué");
            return;
        }
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, java.awt.image.BufferedImage.TYPE_INT_RGB);
        byte[] ram = Mem.RAM;
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            for (int x = 0; x < SCREEN_WIDTH; x++) {
                int idx = ram[bm + y * SCREEN_WIDTH + x] & 0xFF;
                img.setRGB(x, y, hostPalette[idx] & 0xFFFFFF);
            }
        }
        try {
            javax.imageio.ImageIO.write(img, "png", new java.io.File(path));
            System.out.println("[saveScreenshot] écrit " + path);
        } catch (java.io.IOException e) {
            System.err.println("[saveScreenshot] " + e);
        }
    }

    /**
     * Vid_LoadMainPalette — applique gamma/contraste/luminosité à draw_Palette_vw[768]
     * (3×256 guns RGB) et remplit Vid_LoadRGB32Struct_vl, puis upload (LoadRGB32, hôte).
     */
    public static void Vid_LoadMainPalette() {
        Mem.wl(Vid_LoadRGB32Struct_vl, (256 << 16) | 0);     // Vid_LoadRGB32Struct_vl[0] = (256 << 16) | 0
        int c = 0;
        long gun;

        if (Mem.ub(Vid_GammaLevel_b) > 0) {                  // if (Vid_GammaLevel_b > 0)
            int gamma = Vid_GammaIncTables_vb + (((Mem.ub(Vid_GammaLevel_b) - 1) & 7) << 8); // gamma = Vid_GammaIncTables_vb + ((GammaLevel-1)&7)<<8
            for (; c < 768; ++c) {
                gun = (long) Mem.ub(gamma + Mem.uw(draw_Palette_vw + c * 2)) * Mem.uw(Vid_ContrastAdjust_w) + Mem.w(Vid_BrightnessOffset_w); // gamma[draw_Palette_vw[c]] * Contrast + Brightness
                gun = gun < 0 ? 0 : (gun > 65535 ? 65535 : gun); // clamp 0..65535
                Mem.wl(Vid_LoadRGB32Struct_vl + (c + 1) * 4, (int) ((gun << 16) | gun)); // [c+1] = gun<<16 | gun
            }
        } else {
            for (; c < 768; ++c) {
                gun = (long) Mem.uw(draw_Palette_vw + c * 2) * Mem.uw(Vid_ContrastAdjust_w) + Mem.w(Vid_BrightnessOffset_w); // draw_Palette_vw[c] * Contrast + Brightness
                gun = gun < 0 ? 0 : (gun > 65535 ? 65535 : gun);
                Mem.wl(Vid_LoadRGB32Struct_vl + (c + 1) * 4, (int) ((gun << 16) | gun));
            }
        }

        Mem.wl(Vid_LoadRGB32Struct_vl + (c + 1) * 4, 0);     // Vid_LoadRGB32Struct_vl[c+1] = 0 (terminateur)
        LoadRGB32();                                         // LoadRGB32(ViewPortAddress(Vid_MainWindow_l), Vid_LoadRGB32Struct_vl)
    }

    /**
     * LoadRGB32 (graphics.library) : « upload » de la palette.
     *
     * Sur Amiga, copie Vid_LoadRGB32Struct_vl au ViewPort. Ici on en extrait la
     * palette hôte ARGB pour le mapping chunky→écran fait par Vid_Present.
     *
     * Format LoadRGB32 : [0] = (count<<16)|index, puis 3 longwords par couleur
     * (R, G, B). Chaque longword vaut gun<<16|gun (gun = 16 bits) ; la composante
     * 8 bits affichée est l'octet de poids fort = gun>>8.
     */
    private static void LoadRGB32() {
        for (int i = 0; i < 256; i++) {
            int base = Vid_LoadRGB32Struct_vl + 4 + i * 3 * 4; // saut de l'en-tête (+4)
            int r = (Mem.l(base) >>> 24) & 0xFF;       // R
            int g = (Mem.l(base + 4) >>> 24) & 0xFF;   // G
            int b = (Mem.l(base + 8) >>> 24) & 0xFF;   // B
            hostPalette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    // ------------------------------------------------------------------
    // Couche d'affichage AmigaOS — stubs hôte (OpenScreen/AllocRaster, copper,
    // ASL, présentation, C2P EXCLU).
    // ------------------------------------------------------------------

    /**
     * Vid_OpenMainScreen : ouvre l'écran/fenêtre.
     *
     * Chemin hôte RTG : on force Vid_isRTG=1 (rendu chunky, pas de planar/C2P),
     * fixe les dimensions 320x256 et ouvre la fenêtre LWJGL. Le buffer chunky
     * lui-même (Vid_FastBufferPtr_l) est alloué par Draw_Init.
     */
    public static void Vid_OpenMainScreen() {
        Mem.wl(Vid_isRTG, 1);                       // chemin RTG (chunky)
        Mem.ww(Vid_ScreenWidth, SCREEN_WIDTH);      // Vid_ScreenWidth = SCREEN_WIDTH
        Mem.ww(Vid_ScreenHeight, SCREEN_HEIGHT);    // Vid_ScreenHeight = SCREEN_HEIGHT

        if (display == null) {
            display = new Display(SCREEN_WIDTH, SCREEN_HEIGHT);
            display.open("AB3D2 / The Killing Grounds", hostScale);
        }
        Vid_LoadMainPalette();                       // palette initiale
    }

    /** Vid_CloseMainScreen : ferme la fenêtre hôte. */
    public static void Vid_CloseMainScreen() {
        if (display != null) {
            display.close();
            display = null;
        }
    }

    /** vid_SetupDoubleheightCopperlist : copper list AGA — sans objet en RTG (no-op). */
    public static void vid_SetupDoubleheightCopperlist() {
        // RTG : Vid_isRTG → la routine d'origine retourne immédiatement.
    }

    /** GetScreenMode : renvoie un ModeID hôte valide (pas de requester ASL). */
    public static int GetScreenMode() {
        return HOST_SCREEN_MODE;
    }

    // screen.h : géométrie de l'écran réduit (HUD/bordure autour de la vue 3D).
    private static final int SMALL_WIDTH = 192;
    private static final int SMALL_HEIGHT = 160;
    private static final int SMALL_XPOS = 64;
    // C2P_FS_HEIGHT = FS_HEIGHT - FS_HEIGHT_C2P_DIFF = (256-24) - 0 = 232.
    private static final int C2P_FS_HEIGHT = 232;

    /**
     * Vid_Present (screen.c, chemin RTG) : compose la frame dans le bitmap écran hôte
     * (Vid_HostBitmap) puis la présente.
     *
     * Le moteur 3D rend dans le FastBuffer (coin haut-gauche, largeur de la vue). Le
     * bitmap écran contient déjà la bordure HUD (copiée par Draw_ResetGameDisplay).
     * - Plein écran : on recopie toute la vue du FastBuffer dans le bitmap (pas de bordure) ;
     *   les messages sont d'abord rendus dans le FastBuffer (Msg_RenderFullsccreenBuffer).
     * - Écran réduit : on recopie la vue 3D (SMALL_WIDTH×height) à (SMALL_XPOS, SMALL_YPOS),
     *   on rend les messages dans la bordure (Msg_RenderSmallScreenRTG) et on met à jour le HUD.
     * Puis Draw_UpdateBorder_RTG réécrit les compteurs et on convertit le bitmap → ARGB.
     */
    public static void Vid_Present() {
        if (display == null) {
            return;
        }
        int fast = Mem.l(Vid_FastBufferPtr_l);
        if (fast == 0) {
            return; // buffer pas encore alloué (Draw_Init non exécuté)
        }
        boolean fullScreen = Mem.ub(Vid_FullScreen_b) != 0;
        int letterbox = Mem.w(Vid_LetterBoxMarginHeight_w);
        int bm = ab3d2.c.DrawC.Vid_HostBitmap;
        int bmBytesPerRow = SCREEN_WIDTH;
        byte[] ram = Mem.RAM;

        // Messages plein écran (full res) : rendus dans le FastBuffer avant la copie.
        if (fullScreen && ab3d2.c.Message.Msg_Enabled()) {
            ab3d2.c.Message.Msg_RenderFullsccreenBuffer();
        }

        // Effet de dématérialisation (téléporteur / fin de niveau) — réplique C2P_Convert
        // (chunky.s) en RTG : si téléport en cours, distord le FastBuffer via shimmerfile.
        if (dbgForceTeleFrame >= 0) {                         // DIAG : force le shimmer
            applyTeleportShimmer(fast, Vid_ShimmerBuffer, dbgForceTeleFrame);
            fast = Vid_ShimmerBuffer;
        } else {
        if (Mem.b(C2P_Teleporting_b) != 0) {                  // tst.b d5 ; bne → move.w #8,Game_TeleportFrame_w
            Mem.ww(Game_TeleportFrame_w, 8);
        }
        int teleFrame = Mem.uw(Game_TeleportFrame_w);
        if (teleFrame != 0) {                                 // tst.w Game_TeleportFrame_w ; bne
            teleFrame -= 1;                                   // sub.w #1
            Mem.ww(Game_TeleportFrame_w, teleFrame);
            applyTeleportShimmer(fast, Vid_ShimmerBuffer, teleFrame); // d5 = Game_TeleportFrame_w
            fast = Vid_ShimmerBuffer;                         // la présentation lit l'image distordue
        }
        }

        if (fullScreen) {
            int height = C2P_FS_HEIGHT - letterbox * 2;       // height = C2P_FS_HEIGHT - letterbox*2
            int src = fast + SCREEN_WIDTH * letterbox;
            int dst = bm + bmBytesPerRow * letterbox;
            System.arraycopy(ram, src, ram, dst, SCREEN_WIDTH * height); // CopyMemQuick (W==bytesPerRow)
        } else {
            int height = SMALL_HEIGHT - letterbox * 2;        // height = SMALL_HEIGHT - letterbox*2
            int src = fast + SCREEN_WIDTH * letterbox;
            int dst = bm + bmBytesPerRow * (letterbox + Mem.w(SMALL_YPOS)) + SMALL_XPOS;
            for (int y = 0; y < height; ++y) {                // CopyFrameBuffer (full res, pas de doublement)
                System.arraycopy(ram, src, ram, dst, SMALL_WIDTH);
                src += SCREEN_WIDTH;
                dst += bmBytesPerRow;
            }
            if (ab3d2.c.Message.Msg_Enabled() && ab3d2.c.Message.Msg_SmallScreenNeedsRedraw()) {
                ab3d2.c.Message.Msg_RenderSmallScreenRTG(bm, bmBytesPerRow);
            }
        }

        ab3d2.c.DrawC.Draw_UpdateBorder_RTG(bm, bmBytesPerRow); // Draw_UpdateBorder_RTG(bmPixelData, bmBytesPerRow)

        // Conversion du bitmap écran hôte → ARGB et présentation.
        final int n = SCREEN_WIDTH * SCREEN_HEIGHT;
        if (presentBuffer == null) {
            presentBuffer = new int[n];
        }
        for (int i = 0; i < n; i++) {
            presentBuffer[i] = hostPalette[ram[bm + i] & 0xFF];
        }
        display.present(presentBuffer);
        display.pollEvents();
    }

    /**
     * applyTeleportShimmer — effet de « dématérialisation » (c2p_Convert1xTeleFx / chunky.s
     * NEWCHUNKYTEL) porté en chunky→chunky pour le RTG. Distord srcChunky → dstChunky en
     * lisant les pixels à travers la table d'offsets shimmerfile. Par groupe de 8 px :
     * 4 px ← src[pos+offA], 4 px ← src[pos+4+offB] (offA = mot, offB = mot bas du long suivant).
     * La table avance +6 octets/groupe et s'enroule mod 512 par ligne ; la frame indexe le
     * bloc (frame<<10). Le bit-shuffle planar de l'original est omis (on reste chunky).
     *
     * <p>Le pointeur SOURCE avance de 8 par groupe ({@code addq #8,a0}), apres les deux lectures :
     * les decalages de la table sont donc RELATIFS au groupe courant, pas au debut de la ligne.
     * (Cet {@code addq} manquait : l'ecran se reduisait a des trainees horizontales.)
     */
    private static void applyTeleportShimmer(int srcChunky, int dstChunky, int frame) {
        byte[] ram = Mem.RAM;
        int shimBase = draw_TeleportShimmerFXData_vb + (frame << 10); // frame*1024
        int a6Off = 0;                                       // décalage courant dans la table
        final int total = SCREEN_WIDTH * SCREEN_HEIGHT;
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            int a0 = srcChunky + y * SCREEN_WIDTH;           // début de ligne source
            int d = dstChunky + y * SCREEN_WIDTH;            // début de ligne dest
            for (int g = 0; g < SCREEN_WIDTH / 8; g++) {     // groupes de 8 pixels
                int offA = (short) Mem.uw(shimBase + a6Off); a6Off += 2;  // move.w (a6)+,d0
                int rawB = Mem.l(shimBase + a6Off); a6Off += 4;           // move.l (a6)+,d1
                int offB = (short) (rawB & 0xFFFF);          // d1.w (mot bas)
                int srcA = a0 + offA;                        // (a0,d0.w) : 4 px
                int srcB = a0 + 4 + offB;                    // 4(a0,d1.w) : 4 px
                int o = d + g * 8;
                for (int i = 0; i < 4; i++) ram[o + i] = ram[clampIdx(srcA + i, srcChunky, total)];
                for (int i = 0; i < 4; i++) ram[o + 4 + i] = ram[clampIdx(srcB + i, srcChunky, total)];
                a0 += 8;                                     // addq #8,a0 (APRES les deux lectures)
            }
            a6Off &= 0x1FE;                                  // and.l #255*2 : enroulement par ligne
        }
    }

    /** Borne un index source dans [base, base+total). */
    private static int clampIdx(int idx, int base, int total) {
        int rel = idx - base;
        if (rel < 0) {
            rel = 0;
        } else if (rel >= total) {
            rel = total - 1;
        }
        return base + rel;
    }

    /** Tampon 640px pour l'écran de texte d'intro (TWEENTEXT) avant downscale 640→320. */
    private static final int INTRO_W = 640;
    private static final int introScratch = Mem.alloc(INTRO_W * SCREEN_HEIGHT);

    /**
     * Game_ShowIntroText (TWEENTEXT, hires.s/CheeseSauce 4000test.s:416) — affiche le texte
     * narratif du niveau courant avant de jouer. L'original rend 16 lignes (police
     * proportionnelle 16px ENDFONT0, octet0=police, octet1=centré) dans un écran 640px hires
     * (DRAWLINEOFTEXT via BFINS), fondu via couleur copper puis chargement du niveau par-dessus.
     * Ici : rendu en 640px chunky → downscale 640→320 → fondu d'entrée + attente Feu/Espace/
     * Entrée (ou timeout) puisque notre chargement est instantané. Solo uniquement.
     */
    public static void Game_ShowIntroText() {
        if (display == null) {
            return;
        }
        if (!renderIntroScratch(Mem.uw(ab3d2.ControlloopData.Game_LevelNumber_w))) {
            return;                                          // pas de TEXT_FILE chargé
        }
        introPresentLoop();
    }

    /**
     * Rend les 16 lignes du texte d'intro du niveau {@code lvl} dans {@link #introScratch}
     * (640px, 1 octet/px allumé). Retourne false si aucun TEXT_FILE n'est chargé.
     */
    private static boolean renderIntroScratch(int lvl) {
        int textBase = Mem.l(ab3d2.ControlloopData.Lvl_IntroTextPtr_l);
        if (textBase == 0) {
            return false;                                    // pas de TEXT_FILE chargé
        }
        int base = textBase + lvl * 82 * 16;                 // niveau * 82 car * 16 lignes
        int font = ab3d2.data.DrawData.draw_EndFont0_vb;     // ENDFONT0 (32 o/glyphe, 16 lignes)
        int cw = ab3d2.data.DrawData.draw_CharWidths0_vb;    // largeurs (indexé char-32)
        byte[] ram = Mem.RAM;
        java.util.Arrays.fill(ram, introScratch, introScratch + INTRO_W * SCREEN_HEIGHT, (byte) 0);

        for (int line = 0; line < 16; ++line) {              // 16 lignes
            int p = base + line * 82;
            p++;                                             // octet0 = index police (toujours 0)
            int centered = Mem.ub(p); p++;                   // octet1 = centré
            int y = line * 16;
            int x = 0;
            if (centered != 0) {                             // centrage : largeur jusqu'au dernier non-espace
                int w = 0, trimmed = 0;
                for (int i = 0; i < 80; ++i) {
                    int c = Mem.ub(p + i);
                    int cc = (c < 32 || c > 127) ? 32 : c;
                    w += Mem.ub(cw + (cc - 32));
                    if (cc != 32) trimmed = w;
                }
                x = 320 - trimmed / 2;                       // centre sur 640
                if (x < 0) x = 0;
            }
            for (int i = 0; i < 80; ++i) {
                int c = Mem.ub(p + i);
                if (c == 0) {
                    break;                                   // fin de ligne
                }
                if (c < 32 || c > 127) {                     // non imprimable → espace
                    x += Mem.ub(cw);                         // largeur de l'espace (char 32)
                    continue;
                }
                int gw = Mem.ub(cw + (c - 32));              // largeur proportionnelle
                int glyph = font + (c - 32) * 32;            // (c-32)*32
                for (int row = 0; row < 16; ++row) {
                    int word = Mem.uw(glyph + row * 2);
                    if (word == 0) {
                        continue;
                    }
                    int yy = y + row;
                    if (yy >= SCREEN_HEIGHT) {
                        break;
                    }
                    int rowbase = introScratch + yy * INTRO_W;
                    for (int px = 0; px < gw; ++px) {        // bits bas (BFINS) : px → bit (gw-1-px)
                        if (((word >> (gw - 1 - px)) & 1) != 0) {
                            int xx = x + px;
                            if (xx >= 0 && xx < INTRO_W) {
                                ram[rowbase + xx] = 1;
                            }
                        }
                    }
                }
                x += gw;
            }
        }
        return true;
    }

    /** Construit le tampon ARGB de présentation (downscale 640→320) à l'intensité {@code fade} (0..255). */
    private static void buildIntroArgb(int fade) {
        byte[] ram = Mem.RAM;
        if (presentBuffer == null) {
            presentBuffer = new int[SCREEN_WIDTH * SCREEN_HEIGHT];
        }
        for (int yy = 0; yy < SCREEN_HEIGHT; ++yy) {
            int srow = introScratch + yy * INTRO_W;
            int drow = yy * SCREEN_WIDTH;
            for (int xx = 0; xx < SCREEN_WIDTH; ++xx) {
                int lit = (ram[srow + xx * 2] | ram[srow + xx * 2 + 1]) & 1; // downscale OR
                int v = lit != 0 ? fade : 0;
                presentBuffer[drow + xx] = 0xFF000000 | (v << 16) | (v << 8) | v;
            }
        }
    }

    /** Affichage : fondu d'entrée puis attente Feu/Espace/Entrée (timeout ~8 s, chargement instantané). */
    private static void introPresentLoop() {
        int fk = Mem.ub(ab3d2.ControlloopData.fire_key);
        for (int frame = 0; frame < 400; ++frame) {
            buildIntroArgb(frame < 16 ? frame * 16 : 255);   // fondu sur 16 frames
            display.present(presentBuffer);
            display.pollEvents();
            if (frame > 16) {                                // après le fondu : attendre la validation
                if (ab3d2.host.CustomChips.mouseLeftPressed
                        || Mem.ub(KeyMap_vb + fk) != 0
                        || Mem.ub(KeyMap_vb + ab3d2.modules.RawKeyMacros.RAWKEY_SPACEBAR) != 0
                        || Mem.ub(KeyMap_vb + ab3d2.modules.RawKeyMacros.RAWKEY_ENTER) != 0
                        || display.shouldClose()) {
                    break;
                }
            }
        }
    }

    /** Hook de test : rend le texte d'intro du niveau et capture le PNG (sans boucle d'affichage). */
    public static void dumpIntroText(int level, String path) {
        if (!renderIntroScratch(level)) {
            System.err.println("[dumpIntroText] TEXT_FILE non chargé (Lvl_IntroTextPtr_l=0)");
            return;
        }
        buildIntroArgb(255);
        saveLastPresent(path);
    }

    /** Capture le dernier tampon présenté (presentBuffer ARGB) → PNG (diagnostic menu). */
    public static void saveLastPresent(String path) {
        if (presentBuffer == null) {
            System.err.println("[saveLastPresent] aucun tampon présenté");
            return;
        }
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            for (int x = 0; x < SCREEN_WIDTH; x++) {
                img.setRGB(x, y, presentBuffer[y * SCREEN_WIDTH + x] & 0xFFFFFF);
            }
        }
        try {
            javax.imageio.ImageIO.write(img, "png", new java.io.File(path));
            System.out.println("[saveLastPresent] écrit " + path);
        } catch (java.io.IOException e) {
            System.err.println("[saveLastPresent] " + e);
        }
    }

    /**
     * Vid_PresentMenu — présentation de l'écran du menu (remplace BltBitMapRastPort de
     * la tâche de blit RTG). Compose les 8 bitplanes du menu (plans 0,1 = fond scrollé
     * de mnu_screen ; plans 2..7 = mnu_morescreen 0..5 : feu + police) en chunky, mappe
     * via mnu_palette (0x00RRGGBB) en appliquant le facteur de fondu (0..256), présente.
     */
    public static void Vid_PresentMenu() {
        if (display == null) {
            return;
        }
        final int PS = 40 * SCREEN_HEIGHT;                    // PLANESIZE = 10240
        int scroll = (Mem.uw(mnu_screenpos) & 255) * 40;     // offset de défilement (octets)
        int p0 = mnu_screen + scroll;                        // plan 0 (bit 0)
        int p1 = mnu_screen + scroll + PS * 2;               // plan 1 (bit 1)
        int m = mnu_morescreen;                              // plans 2..7 = morescreen 0..5
        int factor = Mem.uw(mnu_fadefactor);
        byte[] ram = Mem.RAM;
        final int n = SCREEN_WIDTH * SCREEN_HEIGHT;
        if (presentBuffer == null) {
            presentBuffer = new int[n];
        }
        int pi = 0;
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            int ro = y * 40;
            for (int xb = 0; xb < 40; xb++) {
                int b0 = ram[p0 + ro + xb] & 0xFF;
                int b1 = ram[p1 + ro + xb] & 0xFF;
                int b2 = ram[m + ro + xb] & 0xFF;
                int b3 = ram[m + PS + ro + xb] & 0xFF;
                int b4 = ram[m + 2 * PS + ro + xb] & 0xFF;
                int b5 = ram[m + 3 * PS + ro + xb] & 0xFF;
                int b6 = ram[m + 4 * PS + ro + xb] & 0xFF;
                int b7 = ram[m + 5 * PS + ro + xb] & 0xFF;
                for (int bit = 7; bit >= 0; bit--) {
                    int ci = ((b0 >> bit) & 1)
                           | (((b1 >> bit) & 1) << 1)
                           | (((b2 >> bit) & 1) << 2)
                           | (((b3 >> bit) & 1) << 3)
                           | (((b4 >> bit) & 1) << 4)
                           | (((b5 >> bit) & 1) << 5)
                           | (((b6 >> bit) & 1) << 6)
                           | (((b7 >> bit) & 1) << 7);
                    int pal = Mem.l(mnu_palette + ci * 4);   // 0x00RRGGBB
                    int r = ((pal >> 16) & 0xFF) * factor >> 8;
                    int g = ((pal >> 8) & 0xFF) * factor >> 8;
                    int b = (pal & 0xFF) * factor >> 8;
                    presentBuffer[pi++] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
        display.present(presentBuffer);
        display.pollEvents();
    }
}
