package ab3d2.c;

import ab3d2.Mem;
import ab3d2.host.ExecLib;

import static ab3d2.data.DrawData.draw_ScrollChars_vb;
import static ab3d2.data.DrawData.draw_GlyphSpacing_vb;
import static ab3d2.data.DrawData.draw_BorderChars_vb;
import static ab3d2.data.DrawData.draw_BorderPacked_vb;
import static ab3d2.bss.DrawBss.draw_DisplayAmmoCount_w;
import static ab3d2.bss.DrawBss.draw_DisplayEnergyCount_w;
import static ab3d2.bss.DrawBss.draw_LastDisplayAmmoCount_w;
import static ab3d2.bss.DrawBss.draw_LastDisplayEnergyCount_w;
import static ab3d2.bss.PlayerBss.Plr_MultiplayerType_b;
import static ab3d2.bss.PlayerBss.Plr1_Weapons_vb;
import static ab3d2.bss.PlayerBss.Plr2_Weapons_vb;
import static ab3d2.bss.PlayerBss.Plr1_TmpGunSelected_b;
import static ab3d2.bss.PlayerBss.Plr2_TmpGunSelected_b;
import static ab3d2.bss.VidBss.Vid_ScreenWidth;
import static ab3d2.bss.VidBss.Vid_ScreenHeight;
import static ab3d2.bss.VidBss.Vid_Screen1Ptr_l;
import static ab3d2.bss.VidBss.Vid_Screen2Ptr_l;
import static ab3d2.bss.VidBss.Vid_DisplayScreenPtr_l;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_FastBufferAllocPtr_l;

/**
 * Traduction de ab3d2_source/c/draw.c — EN SECTIONS (fichier de ~1109 lignes,
 * essentiellement du rendu).
 *
 * SECTION 1 (ce fichier, faite) : cœur TEXTE/glyphes — métriques de texte
 * proportionnel et rendu de glyphes en buffer chunky (RTG/fullscreen) et planar
 * (AGA). C'est ce dont message.c a besoin pour fonctionner.
 *
 * À SUIVRE : HUD/bordure/compteurs (draw_UpdateCounter/Items/Border_RTG/Planar,
 * digits, PlanarToChunky/ConvertBorderDigits/Reorder) et le bring-up OS
 * (Draw_Init/Shutdown/ResetGameDisplay/ConfigureTextPlane : graphics.library,
 * CyberGfx, AllocVec). unLHA n'est PAS nécessaire (assets déjà dépackés).
 *
 * Conventions : drawPtr/textPtr = adresses Mem ; les fonctions renvoyant
 * `const char*` renvoient une adresse (0 = NULL). draw_ScrollChars_vb = police
 * 8x8 (8 octets/glyphe) ; draw_GlyphSpacing_vb[c] = (largeur<<4)|margeGauche.
 */
public final class DrawC {

    private static final int DRAW_MSG_CHAR_W = 8;
    private static final int DRAW_MSG_CHAR_H = 8;
    private static final int SCREEN_WIDTH = 320;          // screen.h
    private static final int SCREEN_HEIGHT = 256;         // screen.h
    private static final int SCREEN_DEPTH = 8;            // screen.h
    private static final int SCREEN_DEPTH_EXP = 3;        // screen.h
    private static final int DISPLAY_COUNT_LIMIT = 999;   // draw.h

    // -- constantes HUD (draw.h) --
    private static final int DRAW_HUD_AMMO_COUNT_X = 160;
    private static final int DRAW_HUD_AMMO_COUNT_Y = -18;
    private static final int DRAW_HUD_ENERGY_COUNT_X = 272;
    private static final int DRAW_HUD_ENERGY_COUNT_Y = -18;
    private static final int DRAW_HUD_ITEM_SLOTS_X = 24;
    private static final int DRAW_HUD_ITEM_SLOTS_Y = -16;
    private static final int DRAW_HUD_CHAR_W = 8;
    private static final int DRAW_HUD_CHAR_H = 7;
    private static final int DRAW_HUD_CHAR_SMALL_W = 8;
    private static final int DRAW_HUD_CHAR_SMALL_H = 5;
    private static final int DRAW_COUNT_W = 3 * DRAW_HUD_CHAR_W; // 24
    private static final int DRAW_NUM_WEAPON_SLOTS = 10;
    private static final int LOW_AMMO_COUNT_WARN_LIMIT = 9;
    private static final int LOW_ENERGY_COUNT_WARN_LIMIT = 9;
    private static final int MULTIPLAYER_SLAVE = 's';

    /** Taille d'un bitplane (octets) : PLANE_OFFSET(n) = n * PLANE_PLANE_SIZE. */
    private static final int PLANE_PLANE_SIZE = SCREEN_WIDTH * SCREEN_HEIGHT / SCREEN_DEPTH; // 10240

    // -- buffers statiques des digits de bordure (remplis par Draw_Init, §3) --
    private static final int draw_BorderDigitsGood = Mem.alloc(DRAW_HUD_CHAR_W * DRAW_HUD_CHAR_H * 10);          // 560
    private static final int draw_BorderDigitsWarn = Mem.alloc(DRAW_HUD_CHAR_W * DRAW_HUD_CHAR_H * 10);          // 560
    private static final int draw_BorderDigitsItem = Mem.alloc(DRAW_HUD_CHAR_SMALL_W * DRAW_HUD_CHAR_SMALL_H * 10); // 400
    private static final int draw_BorderDigitsItemFound = Mem.alloc(DRAW_HUD_CHAR_SMALL_W * DRAW_HUD_CHAR_SMALL_H * 10);
    private static final int draw_BorderDigitsItemSelected = Mem.alloc(DRAW_HUD_CHAR_SMALL_W * DRAW_HUD_CHAR_SMALL_H * 10);
    private static final int draw_BorderDigitsBuffer = Mem.alloc(DRAW_HUD_CHAR_SMALL_H * DRAW_HUD_CHAR_SMALL_W * 10); // 400

    /** draw.c : static UBYTE draw_Border[SCREEN_WIDTH*SCREEN_HEIGHT] — bordure HUD chunky (construite par Draw_Init §3). */
    public static final int draw_Border = Mem.alloc(SCREEN_WIDTH * SCREEN_HEIGHT); // 81920
    /**
     * Bitmap écran hôte (surface présentée) — équivalent du BitMap du ViewPort RTG
     * que Vid_Present verrouille (LockBitMapTags). Distinct du FastBuffer (cible 3D) :
     * la bordure y est copiée une fois (Draw_ResetGameDisplay), puis chaque frame la vue
     * 3D du FastBuffer y est recopiée à (SMALL_XPOS, SMALL_YPOS) et le HUD réécrit dessus.
     */
    public static final int Vid_HostBitmap = Mem.alloc(SCREEN_WIDTH * SCREEN_HEIGHT); // 81920

    private static int draw_LastItemList = 0xFFFF;       // static UWORD = 0xFFFF
    private static int draw_LastItemSelected = 0xFFFF;   // static UWORD = 0xFFFF

    private DrawC() {
    }

    /** ror16 : rotation droite d'une valeur 16 bits de n bits (intrinsèque du C original). */
    private static int ror16(int v, int n) {
        v &= 0xFFFF;
        return ((v >>> n) | (v << (16 - n))) & 0xFFFF;
    }

    /** Draw_IsPrintable (draw.h inline) : imprimable ECMA-94 Latin-1. */
    public static boolean Draw_IsPrintable(int charCode) {
        charCode &= 0xFF;
        return (charCode > 0x20 && charCode < 0x7F) || (charCode > 0xA0); // (c>0x20 && c<0x7F) || (c>0xA0)
    }

    /**
     * Draw_CalcPropWidth — largeur pixel d'une chaîne (jusqu'à maxLen ou NUL) en
     * rendu proportionnel.
     */
    public static int Draw_CalcPropWidth(int textPtr, int maxLen) {
        int width = 0;                                    // ULONG width = 0;
        int maxL = maxLen & 0xFFFF;
        while (true) {                                    // while ((charCode = *textPtr++) && maxLen-- > 0)
            int charCode = Mem.ub(textPtr);
            textPtr++;
            if (charCode == 0) {
                break;
            }
            if (!(maxL-- > 0)) {
                break;
            }
            width += Mem.ub(draw_GlyphSpacing_vb + charCode) >> 4; // width += draw_GlyphSpacing_vb[charCode] >> 4;
        }
        return width;
    }

    /**
     * Draw_CalcPropTextSplit — nombre de caractères d'une chaîne rendables
     * proportionnellement dans une largeur donnée ; met à jour nextTextPtr[0]
     * (in/out, 0 = NULL = plus rien à traiter). Renvoie la largeur du segment.
     */
    public static int Draw_CalcPropTextSplit(int[] nextTextPtr, int txtLength, int fitWidth) {
        if ((txtLength * DRAW_MSG_CHAR_W) <= fitWidth) {  // if ((txtLength * DRAW_MSG_CHAR_W) <= fitWidth)
            nextTextPtr[0] = 0;                           // *nextTextPtr = NULL;
            return txtLength;                             // return txtLength;
        }

        int textPtr = nextTextPtr[0];                     // const char* textPtr = *nextTextPtr;
        int lastNonPrintingPtr = 0;                       // const char* lastNonPrintingPtr = NULL;
        int width = 0;
        int charsLeft = txtLength;
        int charCode = 0;

        // Additionne les largeurs jusqu'à épuisement, dépassement, ou NUL.
        while (charsLeft != 0 && width < fitWidth && (charCode = Mem.ub(textPtr)) != 0) { // while (charsLeft && width < fitWidth && (charCode = *textPtr))
            if (!Draw_IsPrintable(charCode)) {            // if (!Draw_IsPrintable(charCode))
                lastNonPrintingPtr = textPtr;             //   lastNonPrintingPtr = textPtr;
            }
            width += Mem.ub(draw_GlyphSpacing_vb + charCode) >> 4; // width += draw_GlyphSpacing_vb[charCode] >> 4;
            --charsLeft;
            ++textPtr;
        }

        // Dépassement de largeur : reculer d'un caractère.
        if (width > fitWidth) {                           // if (width > fitWidth)
            --textPtr;                                    // charCode = *(--textPtr);
            charCode = Mem.ub(textPtr);
            ++charsLeft;
        }

        // Éviter de couper un mot.
        if (lastNonPrintingPtr != 0 && charsLeft > 1 && Draw_IsPrintable(charCode)) { // if (lastNonPrintingPtr && charsLeft > 1 && Draw_IsPrintable(charCode))
            textPtr = lastNonPrintingPtr + 1;
        }

        width = (textPtr - nextTextPtr[0]) & 0xFFFF;      // width = (UWORD)(textPtr - *nextTextPtr);

        if (charCode == 0 || charsLeft == 0) {            // if (!charCode || !charsLeft)
            textPtr = 0;                                  //   textPtr = NULL;
        }

        // Éviter une espace en tête du prochain segment.
        if (textPtr != 0) {                               // if (textPtr)
            while (charsLeft > 0 && !Draw_IsPrintable(Mem.ub(textPtr))) {
                ++textPtr;
                --charsLeft;
            }
        }

        nextTextPtr[0] = textPtr;                         // *nextTextPtr = textPtr;
        return width;
    }

    /**
     * draw_ChunkyGlyph — dessine un glyphe (pixels positionnés) dans un buffer
     * chunky avec le pen donné. La marge gauche du glyphe sert d'entrée de switch
     * à chute (Duff-like).
     */
    private static void draw_ChunkyGlyph(int drawPtr, int drawSpan, int charCode, int pen) {
        int glyphPtr = draw_ScrollChars_vb + (charCode << 3);       // &draw_ScrollChars_vb[charCode << 3]
        int glyphLMargin = Mem.ub(draw_GlyphSpacing_vb + charCode) & 0x7;       // spacing & 0x7
        int glyphWidth = ((Mem.ub(draw_GlyphSpacing_vb + charCode) >> 4) - 1) & 0xFF; // (spacing >> 4) - 1 (UBYTE)
        for (int row = 0; row < DRAW_MSG_CHAR_H; ++row) {
            int plane = Mem.ub(glyphPtr);                 // UBYTE plane = *glyphPtr++;
            glyphPtr++;
            int width = glyphWidth;
            if (plane != 0) {
                switch (glyphLMargin) {                   // chute volontaire entre les cas
                    case 0: if ((plane & 128) != 0) Mem.wb(drawPtr + 0, pen); width = (width - 1) & 0xFF; if (width == 0) break;
                    case 1: if ((plane & 64) != 0)  Mem.wb(drawPtr + 1, pen); width = (width - 1) & 0xFF; if (width == 0) break;
                    case 2: if ((plane & 32) != 0)  Mem.wb(drawPtr + 2, pen); width = (width - 1) & 0xFF; if (width == 0) break;
                    case 3: if ((plane & 16) != 0)  Mem.wb(drawPtr + 3, pen); width = (width - 1) & 0xFF; if (width == 0) break;
                    case 4: if ((plane & 8) != 0)   Mem.wb(drawPtr + 4, pen); width = (width - 1) & 0xFF; if (width == 0) break;
                    case 5: if ((plane & 4) != 0)   Mem.wb(drawPtr + 5, pen); width = (width - 1) & 0xFF; if (width == 0) break;
                    case 6: if ((plane & 2) != 0)   Mem.wb(drawPtr + 6, pen); width = (width - 1) & 0xFF; if (width == 0) break;
                    case 7: if ((plane & 1) != 0)   Mem.wb(drawPtr + 7, pen); width = (width - 1) & 0xFF; if (width == 0) break;
                    default: break;
                }
            }
            drawPtr += drawSpan;
        }
    }

    /**
     * Draw_ChunkyText — chaîne de glyphes fixes (largeur DRAW_MSG_CHAR_W) dans un
     * buffer chunky. Renvoie l'adresse du caractère suivant, ou 0 (NULL).
     */
    public static int Draw_ChunkyText(int drawPtr, int drawSpan, int maxLen, int textPtr, int xPos, int yPos, int pen) {
        drawPtr += drawSpan * yPos + xPos;                // drawPtr += drawSpan * yPos + xPos;
        int maxL = maxLen & 0xFFFF;
        int charCode = 0;
        while (true) {                                    // while ((charCode = *textPtr++) && maxLen-- > 0)
            charCode = Mem.ub(textPtr);
            textPtr++;
            if (charCode == 0) {
                break;
            }
            if (!(maxL-- > 0)) {
                break;
            }
            if (Draw_IsPrintable(charCode)) {             // if (Draw_IsPrintable(charCode))
                draw_ChunkyGlyph(drawPtr, drawSpan, charCode, pen);
            }
            drawPtr += DRAW_MSG_CHAR_W;                   // drawPtr += DRAW_MSG_CHAR_W;
        }
        return charCode != 0 ? textPtr : 0;               // return charCode ? textPtr : NULL;
    }

    /**
     * Draw_ChunkyTextProp — chaîne de glyphes proportionnels dans un buffer chunky.
     */
    public static int Draw_ChunkyTextProp(int drawPtr, int drawSpan, int maxLen, int textPtr, int xPos, int yPos, int pen) {
        drawPtr += drawSpan * yPos + xPos;
        int maxL = maxLen & 0xFFFF;
        int charCode = 0;
        while (true) {
            charCode = Mem.ub(textPtr);
            textPtr++;
            if (charCode == 0) {
                break;
            }
            if (!(maxL-- > 0)) {
                break;
            }
            int glyphSpacing = Mem.ub(draw_GlyphSpacing_vb + charCode); // UBYTE glyphSpacing = draw_GlyphSpacing_vb[charCode];
            if (Draw_IsPrintable(charCode)) {
                draw_ChunkyGlyph(drawPtr - (glyphSpacing & 0xF), drawSpan, charCode, pen); // draw_ChunkyGlyph(drawPtr - (glyphSpacing & 0xF), ...)
            }
            drawPtr += glyphSpacing >> 4;                 // drawPtr += glyphSpacing >> 4;
        }
        return charCode != 0 ? textPtr : 0;
    }

    /**
     * draw_PlanarGlyph — dessine un glyphe proportionnel dans UN bitplane (AGA).
     */
    private static void draw_PlanarGlyph(int drawPtr, int xPos, int charCode) {
        int glyphPtr = draw_ScrollChars_vb + (charCode << 3);
        int glyphLMargin = Mem.ub(draw_GlyphSpacing_vb + charCode) & 0x7;  // BYTE glyphLMargin = spacing & 0x7
        int glyphWidth = Mem.ub(draw_GlyphSpacing_vb + charCode) >> 4;     // BYTE glyphWidth = spacing >> 4

        drawPtr += (xPos >> 3);                           // drawPtr += (xPos >> 3);
        xPos = (xPos & 7) - glyphLMargin;                 // xPos = (xPos & 7) - glyphLMargin;
        if (xPos < 0) {                                   // if (xPos < 0)
            xPos = -xPos;
            for (int k = 0; k < 8; ++k) {                 // drawPtr[k*SW/8] |= glyphPtr[k] << xPos
                int a = drawPtr + k * (SCREEN_WIDTH >> 3);
                Mem.wb(a, Mem.ub(a) | ((Mem.ub(glyphPtr + k) << xPos) & 0xFF));
            }
        } else if (xPos == 0) {                           // else if (xPos == 0)
            for (int k = 0; k < 8; ++k) {                 // drawPtr[k*SW/8] |= glyphPtr[k]
                int a = drawPtr + k * (SCREEN_WIDTH >> 3);
                Mem.wb(a, Mem.ub(a) | Mem.ub(glyphPtr + k));
            }
        } else if (xPos + glyphWidth >= DRAW_MSG_CHAR_W) { // else if (xPos + glyphWidth >= DRAW_MSG_CHAR_W)
            for (int row = 0; row < DRAW_MSG_CHAR_H; ++row) {
                int rot = ror16(Mem.ub(glyphPtr + row), xPos); // UWORD rot = ror16(glyphPtr[row], xPos)
                Mem.wb(drawPtr + 0, Mem.ub(drawPtr + 0) | (rot & 0xFF));        // drawPtr[0] |= rot
                Mem.wb(drawPtr + 1, Mem.ub(drawPtr + 1) | ((rot >> 8) & 0xFF)); // drawPtr[1] |= rot >> 8
                drawPtr += SCREEN_WIDTH >> 3;             // drawPtr += SCREEN_WIDTH >> 3
            }
        } else {                                          // else
            for (int k = 0; k < 8; ++k) {                 // drawPtr[k*SW/8] |= glyphPtr[k] >> xPos
                int a = drawPtr + k * (SCREEN_WIDTH >> 3);
                Mem.wb(a, Mem.ub(a) | ((Mem.ub(glyphPtr + k) >> xPos) & 0xFF));
            }
        }
    }

    /**
     * Draw_PlanarTextProp — chaîne de glyphes proportionnels dans un bitplane (AGA).
     */
    public static int Draw_PlanarTextProp(int drawPtr, int maxLen, int textPtr, int xPos, int yPos) {
        drawPtr += yPos * (SCREEN_WIDTH >> 3);            // drawPtr += yPos * (SCREEN_WIDTH >> 3);
        int maxL = maxLen & 0xFFFF;
        int charCode = 0;
        while (true) {
            charCode = Mem.ub(textPtr);
            textPtr++;
            if (charCode == 0) {
                break;
            }
            if (!(maxL-- > 0)) {
                break;
            }
            int glyphSpacing = Mem.ub(draw_GlyphSpacing_vb + charCode);
            if (Draw_IsPrintable(charCode)) {
                draw_PlanarGlyph(drawPtr, xPos, charCode);
            }
            xPos += glyphSpacing >> 4;                    // xPos += glyphSpacing >> 4;
        }
        return charCode != 0 ? textPtr : 0;
    }

    /** draw_ValueToDigits — décompose une valeur (≤999) en 3 chiffres. */
    private static void draw_ValueToDigits(int value, int[] digits) {
        if (value > DISPLAY_COUNT_LIMIT) {                // if (value > DISPLAY_COUNT_LIMIT)
            value = DISPLAY_COUNT_LIMIT;
        }
        digits[2] = value % 10; value /= 10;             // digits[2] = value % 10; value /= 10;
        digits[1] = value % 10; value /= 10;             // digits[1] = value % 10; value /= 10;
        digits[0] = value;                               // digits[0] = value;
    }

    /** Draw_LineOfText — relique, vide dans l'original. */
    public static void Draw_LineOfText(int ptr, int screenPointer, int xxxx) {
        // (vide)
    }

    // ------------------------------------------------------------------
    // SECTIONS À SUIVRE — rendu HUD/bordure + bring-up OS (graphics.library,
    // CyberGfx, AllocVec). Stubs documentés.
    // ------------------------------------------------------------------

    /**
     * Draw_ClearRect (draw.c) : SetAPen(0)/RectFill(x1,y1,x2,y2)/SetAPen(255) sur le
     * RastPort de l'écran. Hôte chunky : remplit le rectangle (inclusif) du bitmap
     * écran (Vid_HostBitmap) avec le pen 0. Le SetAPen(255) final est sans objet en
     * chunky (Draw_ChunkyTextProp reçoit son pen explicitement).
     */
    public static void Draw_ClearRect(int x1, int y1, int x2, int y2) {
        for (int y = y1; y <= y2; ++y) {
            int row = Vid_HostBitmap + y * SCREEN_WIDTH;
            for (int x = x1; x <= x2; ++x) {
                Mem.wb(row + x, 0);
            }
        }
    }

    /** draw.c : VID_FAST_BUFFER_SIZE = SCREEN_WIDTH*SCREEN_HEIGHT + 4095 (taille + alignement). */
    private static final int VID_FAST_BUFFER_SIZE = ScreenC.SCREEN_WIDTH * ScreenC.SCREEN_HEIGHT + 4095;

    /** PLANESIZE (screen.h) : taille d'un bitplane (octets) — alias de PLANE_PLANE_SIZE. */
    private static final int PLANESIZE = PLANE_PLANE_SIZE; // 10240

    /**
     * draw_PlanarToChunky (draw.c) — convertit numPixels pixels de 8 bitplanes
     * (planePtrs[0..7]) vers le chunky (1 octet/pixel) en chunkyPtr.
     */
    private static void draw_PlanarToChunky(int chunkyPtr, int[] planePtrs, int numPixels) {
        int[] pptr = new int[8];                             // BitPlanes pptr;
        for (int p = 0; p < 8; ++p) {                        // for (p<8) pptr[p] = planePtrs[p];
            pptr[p] = planePtrs[p];
        }
        for (int x = 0; x < numPixels / 8; ++x) {            // for (x < numPixels/8)
            for (int p = 0; p < 8; ++p) {                    //   for (p<8)
                int c = 0;                                   //     chunkyPtr[p] = 0;
                int bit = 1 << (7 - p);                      //     UBYTE bit = 1 << (7-p);
                for (int b = 0; b < 8; ++b) {                //     for (b<8)
                    if ((Mem.ub(pptr[b]) & bit) != 0) {      //       if (*pptr[b] & bit)
                        c |= 1 << b;                         //         chunkyPtr[p] |= 1<<b;
                    }
                }
                Mem.wb(chunkyPtr + p, c);
            }
            chunkyPtr += 8;                                  //   chunkyPtr += 8;
            for (int p = 0; p < 8; ++p) {                    //   for (p<8) pptr[p]++;
                pptr[p]++;
            }
        }
    }

    /**
     * draw_ConvertBorderDigitsToChunky (draw.c) — convertit les 10 digits planar de
     * draw_BorderChars_vb (entrelacés, 10 octets/plan) en 10 digits chunky consécutifs.
     */
    private static void draw_ConvertBorderDigitsToChunky(int chunkyPtr, int planarBasePtr, int width, int height) {
        int[] planes = new int[8];                           // BitPlanes planes;
        int outDigit = chunkyPtr;                            // UBYTE *out_digit = chunkyPtr;
        for (int d = 0; d < 10; ++d) {                       // for (d<10)
            int digit = planarBasePtr + d;                   //   const UBYTE *digit = base_digit + d;
            for (int p = 0; p < 8; ++p) {                    //   for (p<8) planes[p] = digit + p*10;
                planes[p] = digit + p * 10;
            }
            for (int y = 0; y < height; ++y) {               //   for (y<height)
                draw_PlanarToChunky(outDigit, planes, width); //     draw_PlanarToChunky(out_digit, planes, width);
                for (int p = 0; p < 8; ++p) {                //     for (p<8) planes[p] += width*10;
                    planes[p] += width * 10;
                }
                outDigit += width;                           //     out_digit += width;
            }
        }
    }

    /**
     * Draw_Init (draw.c) — alloue le buffer chunky de rendu (Vid_FastBufferPtr_l) et,
     * en RTG, construit la bordure HUD : décompresse draw_BorderPacked_vb (déjà dépacké,
     * 8 plans × 320×256) dans le FastBuffer (scratch), planar→chunky → draw_Border, puis
     * convertit les 5 jeux de digits (ammo/énergie « bons » et « bas », items dispo/trouvé/
     * sélectionné) en chunky. Réinitialise enfin les compteurs HUD.
     */
    public static int Draw_Init() {
        int alloc = ExecLib.AllocVec(VID_FAST_BUFFER_SIZE, ExecLib.MEMF_PUBLIC); // FastBufferAllocPtr = AllocVec(...)
        if (alloc == 0) {                                    // if (!FastBufferAllocPtr) goto fail;
            Draw_Shutdown();
            return 0;                                        // return FALSE;
        }
        Mem.wl(Vid_FastBufferAllocPtr_l, alloc);
        int fast = (alloc + 15) & ~15;                       // Vid_FastBufferPtr_l = (FastBufferAllocPtr + 15) & ~15;
        Mem.wl(Vid_FastBufferPtr_l, fast);

        // Vid_isRTG forcé (chemin RTG, chunky).
        // unLHA(Vid_FastBufferPtr_l, draw_BorderPacked_vb, ...) : newborderpacked est déjà
        // dépacké (81920 = 8×PLANESIZE), donc unLHA dégénère en copie identité.
        for (int i = 0; i < SCREEN_DEPTH * PLANESIZE; ++i) {
            Mem.wb(fast + i, Mem.ub(draw_BorderPacked_vb + i));
        }
        int[] planes = new int[SCREEN_DEPTH];                // BitPlanes planes;
        for (int p = 0; p < SCREEN_DEPTH; ++p) {             // for (p<SCREEN_DEPTH) planes[p] = FastBuffer + PLANESIZE*p;
            planes[p] = fast + PLANESIZE * p;
        }
        draw_PlanarToChunky(draw_Border, planes, SCREEN_WIDTH * SCREEN_HEIGHT); // taille fixe

        // Digits compteur « bas » (ammo/énergie faibles)
        draw_ConvertBorderDigitsToChunky(draw_BorderDigitsWarn,
                draw_BorderChars_vb + 15 * DRAW_HUD_CHAR_W * 10, DRAW_HUD_CHAR_W, DRAW_HUD_CHAR_H);
        // Digits compteur normaux
        draw_ConvertBorderDigitsToChunky(draw_BorderDigitsGood,
                draw_BorderChars_vb + 15 * DRAW_HUD_CHAR_W * 10 + DRAW_HUD_CHAR_H * DRAW_HUD_CHAR_W * 10,
                DRAW_HUD_CHAR_W, DRAW_HUD_CHAR_H);
        // Digits items indisponibles
        draw_ConvertBorderDigitsToChunky(draw_BorderDigitsItem,
                draw_BorderChars_vb, DRAW_HUD_CHAR_SMALL_W, DRAW_HUD_CHAR_SMALL_H);
        // Digits items trouvés
        draw_ConvertBorderDigitsToChunky(draw_BorderDigitsItemFound,
                draw_BorderChars_vb + DRAW_HUD_CHAR_SMALL_H * 10 * DRAW_HUD_CHAR_SMALL_W,
                DRAW_HUD_CHAR_SMALL_W, DRAW_HUD_CHAR_SMALL_H);
        // Digits item sélectionné
        draw_ConvertBorderDigitsToChunky(draw_BorderDigitsItemSelected,
                draw_BorderChars_vb + DRAW_HUD_CHAR_SMALL_H * 10 * DRAW_HUD_CHAR_SMALL_W * 2,
                DRAW_HUD_CHAR_SMALL_W, DRAW_HUD_CHAR_SMALL_H);

        draw_ResetHUDCounters();                             // draw_ResetHUDCounters();
        return 1;                                            // return TRUE;
    }

    /** Draw_Shutdown : libère le buffer chunky (FreeVec = no-op bump). */
    public static void Draw_Shutdown() {
        int alloc = Mem.l(Vid_FastBufferAllocPtr_l);
        if (alloc != 0) {                                    // if (FastBufferAllocPtr)
            ExecLib.FreeVec(alloc);                          //   FreeVec(FastBufferAllocPtr);
            Mem.wl(Vid_FastBufferAllocPtr_l, 0);             //   FastBufferAllocPtr = NULL;
        }
        Mem.wl(Vid_FastBufferPtr_l, 0);
    }

    /**
     * Draw_ResetGameDisplay (draw.c) — VERSION MINIMALE (P2c, chemin RTG).
     *
     * Réamorce les compteurs HUD et efface le buffer chunky de rendu. La copie de
     * la bordure (draw_Border → bitmap écran + Draw_UpdateBorder_RTG) est DIFFÉRÉE
     * en Phase 5 (draw_Border est construit par Draw_Init §3, assets absents).
     */
    /** Octet de remplissage du clear (0 normal ; sentinelle de debug via -PclearByte). */
    public static int dbgClearByte = 0;

    public static void Draw_ResetGameDisplay() {
        draw_ResetHUDCounters();                             // draw_ResetHUDCounters();
        // Vid_isRTG forcé true (chemin RTG).
        int b = dbgClearByte & 0xFF;
        int fill = (b << 24) | (b << 16) | (b << 8) | b;     // octet sentinelle répliqué sur le long
        ab3d2.modules.Sys.Sys_MemFillLong(Mem.l(Vid_FastBufferPtr_l), fill,
                (ScreenC.SCREEN_WIDTH * ScreenC.SCREEN_HEIGHT) >> 2); // Sys_MemFillLong(Vid_FastBufferPtr_l, 0, (W*H)>>2);

        // « Lock » du bitmap écran hôte (bmBytesPerRow == SCREEN_WIDTH) et copie de la
        // bordure draw_Border dedans, puis première mise à jour du HUD.
        int bmBaseAddress = Vid_HostBitmap;
        int bmBytesPerRow = SCREEN_WIDTH;
        int src = draw_Border;                               // const UBYTE *src = draw_Border;
        int height = Mem.w(Vid_ScreenHeight) < SCREEN_HEIGHT ? Mem.w(Vid_ScreenHeight) : SCREEN_HEIGHT;
        src += (SCREEN_HEIGHT - height) * SCREEN_WIDTH;      // src += (SCREEN_HEIGHT - height) * SCREEN_WIDTH;
        // bmBytesPerRow == SCREEN_WIDTH → copie contiguë.
        System.arraycopy(Mem.RAM, src, Mem.RAM, bmBaseAddress, SCREEN_WIDTH * height); // COPY(src, bm, W*height)
        Draw_UpdateBorder_RTG(bmBaseAddress, bmBytesPerRow); // Draw_UpdateBorder_RTG(bmBaseAddress, bmBytesPerRow)
    }

    /** Draw_RepairTextPlaneBorders : restaure les bords du plan de texte. */
    public static void Draw_RepairTextPlaneBorders() {
        throw new UnsupportedOperationException("draw.c::Draw_RepairTextPlaneBorders (rendu, à suivre)");
    }

    // ==================================================================
    // SECTION 2 — rendu HUD/bordure (RTG + AGA planar). GFX_LONG_ALIGNED
    // est défini (screen.h) → chemin 32 bits + CopyMem. Les buffers de
    // digits sont remplis par Draw_Init (§3) ; le rendu est faithful même
    // si vides tant que §3 n'est pas fait.
    // ==================================================================

    // -- inlines draw_inline.h --

    /** draw_ScreenXPos : xPos>=0 ? xPos : Vid_ScreenWidth + xPos. */
    private static int draw_ScreenXPos(int xPos) {
        return xPos >= 0 ? xPos : Mem.w(Vid_ScreenWidth) + xPos;
    }

    /** draw_ScreenYPos : yPos>=0 ? yPos : Vid_ScreenHeight + yPos. */
    private static int draw_ScreenYPos(int yPos) {
        return yPos >= 0 ? yPos : Mem.w(Vid_ScreenHeight) + yPos;
    }

    /** draw_PackItemSlots : bitmask des slots d'armes non nuls. */
    private static int draw_PackItemSlots(int itemSlots) {
        int itemList = 0;                                   // UWORD itemList = 0
        for (int i = 0; i < DRAW_NUM_WEAPON_SLOTS; ++i) {
            itemList |= (Mem.uw(itemSlots + i * 2) != 0) ? (1 << i) : 0; // itemList |= itemSlots[i] ? (1<<i) : 0
        }
        return itemList & 0xFFFF;
    }

    /** draw_ResetHUDCounters : force le redraw complet du HUD. */
    public static void draw_ResetHUDCounters() {
        draw_LastItemList = 0xFFFF;
        draw_LastItemSelected = 0xFFFF;
        Mem.ww(draw_LastDisplayAmmoCount_w, 0xFFFF);
        Mem.ww(draw_LastDisplayEnergyCount_w, 0xFFFF);
    }

    // -- RTG --

    /** draw_RenderCounterDigit_RTG (GFX_LONG_ALIGNED) — copie un digit 8x7 (longs). */
    private static void draw_RenderCounterDigit_RTG(int drawPtr, int glyphPtr, int digit, int span) {
        int digitPtr = glyphPtr + digit * DRAW_HUD_CHAR_W * DRAW_HUD_CHAR_H; // &glyphPtr[digit*W*H]
        int drawPtr32 = drawPtr;
        int spanL = span >> 2;                              // span >>= 2 (en longs)
        for (int y = 0; y < DRAW_HUD_CHAR_H; ++y) {
            for (int x = 0; x < DRAW_HUD_CHAR_W / 4; ++x) { // W/sizeof(ULONG)
                Mem.wl(drawPtr32 + x * 4, Mem.l(digitPtr)); // drawPtr32[x] = *digitPtr++
                digitPtr += 4;
            }
            drawPtr32 += spanL * 4;                         // drawPtr32 += span (longs)
        }
    }

    /** draw_UpdateCounter_RTG — rend un compteur 3 chiffres dans le bitmap. */
    private static void draw_UpdateCounter_RTG(int bmBaseAddress, int bmBytesPerRow, int count, int limit, int xPos, int yPos) {
        int[] digits = new int[3];
        draw_ValueToDigits(count, digits);

        int glyphPtr = count > limit ? draw_BorderDigitsGood : draw_BorderDigitsWarn; // count > limit ? Good : Warn

        int bufferPtr = draw_BorderDigitsBuffer;
        for (int d = 0; d < 3; ++d, bufferPtr += DRAW_HUD_CHAR_W) {
            draw_RenderCounterDigit_RTG(bufferPtr, glyphPtr, digits[d], DRAW_COUNT_W);
        }

        int drawPtr = bmBaseAddress + xPos + yPos * bmBytesPerRow;
        bufferPtr = draw_BorderDigitsBuffer;
        for (int y = 0; y < DRAW_HUD_CHAR_H; ++y, drawPtr += bmBytesPerRow, bufferPtr += DRAW_COUNT_W) {
            ExecLib.CopyMem(bufferPtr, drawPtr, DRAW_COUNT_W); // COPY(bufferPtr, drawPtr, DRAW_COUNT_W)
        }
    }

    /** draw_RenderItemDigit_RTG (GFX_LONG_ALIGNED) — copie un digit 8x5 (longs). */
    private static void draw_RenderItemDigit_RTG(int drawPtr, int glyphPtr, int digit, int span) {
        int digitPtr = glyphPtr + digit * DRAW_HUD_CHAR_SMALL_W * DRAW_HUD_CHAR_SMALL_H;
        int drawPtr32 = drawPtr;
        int spanL = span >> 2;
        for (int y = 0; y < DRAW_HUD_CHAR_SMALL_H; ++y) {
            for (int x = 0; x < DRAW_HUD_CHAR_SMALL_W / 4; ++x) {
                Mem.wl(drawPtr32 + x * 4, Mem.l(digitPtr));
                digitPtr += 4;
            }
            drawPtr32 += spanL * 4;
        }
    }

    /** draw_UpdateItems_RTG — rend la barre d'inventaire (10 slots). */
    private static void draw_UpdateItems_RTG(int bmBaseAddress, int bmBytesPerRow, int itemSlots, int itemSelected, int xPos, int yPos) {
        int drawPtr = bmBaseAddress + xPos + yPos * bmBytesPerRow;

        int bufferPtr = draw_BorderDigitsBuffer;
        for (int i = 0; i < DRAW_NUM_WEAPON_SLOTS; ++i, bufferPtr += DRAW_HUD_CHAR_SMALL_W) {
            int glyphPtr = (itemSelected == i) ? draw_BorderDigitsItemSelected
                : (Mem.uw(itemSlots + i * 2) != 0 ? draw_BorderDigitsItemFound : draw_BorderDigitsItem);
            draw_RenderItemDigit_RTG(bufferPtr, glyphPtr, i, DRAW_HUD_CHAR_SMALL_W * 10);
        }

        bufferPtr = draw_BorderDigitsBuffer;
        for (int i = 0; i < DRAW_HUD_CHAR_SMALL_H; ++i, drawPtr += bmBytesPerRow, bufferPtr += DRAW_HUD_CHAR_SMALL_W * 10) {
            ExecLib.CopyMem(bufferPtr, drawPtr, DRAW_HUD_CHAR_SMALL_W * 10);
        }
    }

    /** Draw_UpdateBorder_RTG — met à jour HUD/bordure dans le bitmap verrouillé (RTG). */
    public static void Draw_UpdateBorder_RTG(int bmBaseAddress, int bmBytesPerRow) {
        // INIT_ITEMS()
        int itemSlots;
        int itemSelected;
        if (Mem.b(Plr_MultiplayerType_b) == MULTIPLAYER_SLAVE) {
            itemSlots = Plr2_Weapons_vb;
            itemSelected = Mem.ub(Plr2_TmpGunSelected_b);
        } else {
            itemSlots = Plr1_Weapons_vb;
            itemSelected = Mem.ub(Plr1_TmpGunSelected_b);
        }
        int itemList = draw_PackItemSlots(itemSlots);

        if (itemSelected != draw_LastItemSelected || itemList != draw_LastItemList) {
            draw_LastItemSelected = itemSelected;
            draw_LastItemList = itemList;
            draw_UpdateItems_RTG(bmBaseAddress, bmBytesPerRow, itemSlots, itemSelected,
                draw_ScreenXPos(DRAW_HUD_ITEM_SLOTS_X), draw_ScreenYPos(DRAW_HUD_ITEM_SLOTS_Y));
        }
        if (Mem.uw(draw_LastDisplayAmmoCount_w) != Mem.uw(draw_DisplayAmmoCount_w)) {
            Mem.ww(draw_LastDisplayAmmoCount_w, Mem.uw(draw_DisplayAmmoCount_w));
            draw_UpdateCounter_RTG(bmBaseAddress, bmBytesPerRow, Mem.uw(draw_DisplayAmmoCount_w),
                LOW_AMMO_COUNT_WARN_LIMIT, draw_ScreenXPos(DRAW_HUD_AMMO_COUNT_X), draw_ScreenYPos(DRAW_HUD_AMMO_COUNT_Y));
        }
        if (Mem.uw(draw_LastDisplayEnergyCount_w) != Mem.uw(draw_DisplayEnergyCount_w)) {
            Mem.ww(draw_LastDisplayEnergyCount_w, Mem.uw(draw_DisplayEnergyCount_w));
            draw_UpdateCounter_RTG(bmBaseAddress, bmBytesPerRow, Mem.uw(draw_DisplayEnergyCount_w),
                LOW_ENERGY_COUNT_WARN_LIMIT, draw_ScreenXPos(DRAW_HUD_ENERGY_COUNT_X), draw_ScreenYPos(DRAW_HUD_ENERGY_COUNT_Y));
        }
    }

    // -- Planar (AGA) --

    /** draw_RenderItemDigit_Planar — écrit un digit 8x5 dans les 2 buffers, 8 plans. */
    private static void draw_RenderItemDigit_Planar(int offset, int glyphPtr, int digit, int bytesPerRow) {
        int digitPtrBase = glyphPtr + digit * DRAW_HUD_CHAR_SMALL_W * DRAW_HUD_CHAR_SMALL_H;
        int[] planes = { Mem.l(Vid_Screen1Ptr_l), Mem.l(Vid_Screen2Ptr_l) };
        for (int p = 0; p < 2; ++p) {
            int drawPtr = planes[p] + offset;
            int digitPtr = digitPtrBase;
            for (int y = 0; y < DRAW_HUD_CHAR_SMALL_H; ++y) {
                for (int n = 0; n < 8; ++n) {               // drawPtr[PLANE_OFFSET(n)] = *digitPtr++
                    Mem.wb(drawPtr + n * PLANE_PLANE_SIZE, Mem.ub(digitPtr));
                    digitPtr++;
                }
                drawPtr += bytesPerRow;
            }
        }
    }

    /** draw_UpdateItems_Planar — rend l'inventaire (rend toujours dans les 2 buffers). */
    private static void draw_UpdateItems_Planar(int planes, int bytesPerRow, int itemSlots, int itemSelected, int xPos, int yPos) {
        int offset = (xPos + yPos * SCREEN_WIDTH) >> SCREEN_DEPTH_EXP;
        for (int i = 0; i < DRAW_NUM_WEAPON_SLOTS; ++i, offset += DRAW_HUD_CHAR_SMALL_W >> SCREEN_DEPTH_EXP) {
            int glyphPtr = (itemSelected == i) ? draw_BorderDigitsItemSelected
                : (Mem.uw(itemSlots + i * 2) != 0 ? draw_BorderDigitsItemFound : draw_BorderDigitsItem);
            draw_RenderItemDigit_Planar(offset, glyphPtr, i, bytesPerRow);
        }
    }

    /** draw_RenderCounterDigit_Planar — écrit un digit 8x7 dans les 2 buffers, 8 plans. */
    private static void draw_RenderCounterDigit_Planar(int offset, int glyphPtr, int digit, int bytesPerRow) {
        int digitPtrBase = glyphPtr + digit * DRAW_HUD_CHAR_W * DRAW_HUD_CHAR_H;
        int[] planes = { Mem.l(Vid_Screen1Ptr_l), Mem.l(Vid_Screen2Ptr_l) };
        for (int p = 0; p < 2; ++p) {
            int drawPtr = planes[p] + offset;
            int digitPtr = digitPtrBase;
            for (int y = 0; y < DRAW_HUD_CHAR_H; ++y) {
                for (int n = 0; n < 8; ++n) {
                    Mem.wb(drawPtr + n * PLANE_PLANE_SIZE, Mem.ub(digitPtr));
                    digitPtr++;
                }
                drawPtr += bytesPerRow;
            }
        }
    }

    /** draw_UpdateCounter_Planar — rend un compteur 3 chiffres (planar). */
    private static void draw_UpdateCounter_Planar(int planes, int bytesPerRow, int count, int limit, int xPos, int yPos) {
        int[] digits = new int[3];
        draw_ValueToDigits(count, digits);

        int glyphPtr = count > limit ? draw_BorderDigitsGood : draw_BorderDigitsWarn;

        int offset = (xPos + yPos * SCREEN_WIDTH) >> SCREEN_DEPTH_EXP;
        for (int d = 0; d < 3; ++d, offset += DRAW_HUD_CHAR_W >> SCREEN_DEPTH_EXP) {
            draw_RenderCounterDigit_Planar(offset, glyphPtr, digits[d], bytesPerRow);
        }
    }

    /** Draw_UpdateBorder_Planar — met à jour HUD/bordure (AGA). */
    public static void Draw_UpdateBorder_Planar() {
        // INIT_ITEMS()
        int itemSlots;
        int itemSelected;
        if (Mem.b(Plr_MultiplayerType_b) == MULTIPLAYER_SLAVE) {
            itemSlots = Plr2_Weapons_vb;
            itemSelected = Mem.ub(Plr2_TmpGunSelected_b);
        } else {
            itemSlots = Plr1_Weapons_vb;
            itemSelected = Mem.ub(Plr1_TmpGunSelected_b);
        }
        int itemList = draw_PackItemSlots(itemSlots);

        if (itemSelected != draw_LastItemSelected || itemList != draw_LastItemList) {
            draw_LastItemSelected = itemSelected;
            draw_LastItemList = itemList;
            draw_UpdateItems_Planar(Mem.l(Vid_DisplayScreenPtr_l), SCREEN_WIDTH / SCREEN_DEPTH, itemSlots, itemSelected,
                draw_ScreenXPos(DRAW_HUD_ITEM_SLOTS_X), draw_ScreenYPos(DRAW_HUD_ITEM_SLOTS_Y));
        }
        if (Mem.uw(draw_LastDisplayAmmoCount_w) != Mem.uw(draw_DisplayAmmoCount_w)) {
            Mem.ww(draw_LastDisplayAmmoCount_w, Mem.uw(draw_DisplayAmmoCount_w));
            draw_UpdateCounter_Planar(Mem.l(Vid_DisplayScreenPtr_l), SCREEN_WIDTH / SCREEN_DEPTH, Mem.uw(draw_DisplayAmmoCount_w),
                LOW_AMMO_COUNT_WARN_LIMIT, draw_ScreenXPos(DRAW_HUD_AMMO_COUNT_X), draw_ScreenYPos(DRAW_HUD_AMMO_COUNT_Y));
        }
        if (Mem.uw(draw_LastDisplayEnergyCount_w) != Mem.uw(draw_DisplayEnergyCount_w)) {
            Mem.ww(draw_LastDisplayEnergyCount_w, Mem.uw(draw_DisplayEnergyCount_w));
            draw_UpdateCounter_Planar(Mem.l(Vid_DisplayScreenPtr_l), SCREEN_WIDTH / SCREEN_DEPTH, Mem.uw(draw_DisplayEnergyCount_w),
                LOW_ENERGY_COUNT_WARN_LIMIT, draw_ScreenXPos(DRAW_HUD_ENERGY_COUNT_X), draw_ScreenYPos(DRAW_HUD_ENERGY_COUNT_Y));
        }
    }
}
