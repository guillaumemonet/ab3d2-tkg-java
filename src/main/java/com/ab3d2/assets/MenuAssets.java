package com.ab3d2.assets;

import com.ab3d2.core.GameContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Charge et prépare tous les assets du menu AB3D2.
 *
 * Assets requis (dans assets/menu/) :
 *   back2.raw        - background 2 bitplanes 320x256
 *   firepal.pal2     - palette feu 8 couleurs (format 00RRGGBB * 8)
 *   font16x16.raw2   - police 16x16, 3 bitplanes, 20 chars/ligne, 11 lignes
 *   back.pal         - palette background 4 couleurs
 *   credits_only.raw - frame credits 3 bitplanes 320x192
 *
 * Optionnel : font16x16.pal2 (si absent, palette verte par défaut)
 */
public class MenuAssets {

    private static final Logger log = LoggerFactory.getLogger(MenuAssets.class);

    // Dimensions
    public static final int SCREEN_W   = 320;
    public static final int SCREEN_H   = 256;
    public static final int ROWSIZE    = SCREEN_W / 8;   // 40 bytes
    public static final int PLANE_SIZE = ROWSIZE * SCREEN_H; // 10240

    public static final int FONT_GLYPH_W  = 16;
    public static final int FONT_GLYPH_H  = 16;
    public static final int FONT_COLS     = 20;
    public static final int FONT_ROWS     = 11;
    public static final int FONT_W        = FONT_COLS * FONT_GLYPH_W; // 320
    public static final int FONT_H        = FONT_ROWS * FONT_GLYPH_H; // 176
    public static final int FONT_PLANES   = 3;

    // ASCII 32 = premier glyphe (espace)
    public static final int FONT_FIRST_CHAR = 32;
    public static final int FONT_NUM_CHARS  = FONT_COLS * FONT_ROWS; // 220

    // Données brutes
    private byte[] back2Raw;
    private byte[] fontRaw;
    private byte[] creditsRaw;

    // Palettes décodées (ARGB int[])
    private int[] backpal;     // 4 couleurs background
    private int[] firepal;     // 8 couleurs feu
    private int[] fontpal;     // 8 couleurs police
    private int[] menuPalette; // 256 couleurs fusionnées

    // Textures OpenGL
    private int backgroundTexture = -1;
    private int fontTexture        = -1;
    private int creditsTexture     = -1;

    private boolean loaded = false;

    public void load(GameContext ctx) {
        Path root = ctx.assets().getRoot().resolve("menu");
        log.info("Loading menu assets from {}", root);

        // ── Palettes ──────────────────────────────────────────────────────────
        backpal  = loadPalFromFile(root.resolve("back.pal"),         4);
        firepal  = loadPalFromFile(root.resolve("firepal.pal2"),     8);
        fontpal  = loadPalFromFile(root.resolve("font16x16.pal2"),   8);

        // Fallback si fichiers manquants
        if (backpal == null)  backpal  = buildDefaultBackpal();
        if (firepal == null)  firepal  = buildDefaultFirepal();
        if (fontpal == null)  fontpal  = buildDefaultFontpal();

        // Palette complète 256 couleurs (fusion)
        menuPalette = AmigaBitplaneDecoder.buildMenuPalette(backpal, firepal, fontpal);

        // ── Background ────────────────────────────────────────────────────────
        back2Raw = loadRaw(root.resolve("back2.raw"));
        if (back2Raw != null) {
            // 2 bitplanes, 4 couleurs -> utilise backpal
            int[] bgPixels = AmigaBitplaneDecoder.decode(
                back2Raw, SCREEN_W, SCREEN_H, 2, backpal
            );
            backgroundTexture = ctx.assets().createTextureFromARGB(bgPixels, SCREEN_W, SCREEN_H);
            log.info("Background texture loaded ({}x{})", SCREEN_W, SCREEN_H);
        } else {
            log.warn("back2.raw not found, using solid background");
        }

        // ── Font ──────────────────────────────────────────────────────────────
        fontRaw = loadRaw(root.resolve("font16x16.raw2"));
        if (fontRaw != null) {
            // 3 bitplanes séquentiels, image 320x176
            int[] fontPixels = AmigaBitplaneDecoder.decode(
                fontRaw, FONT_W, FONT_H, FONT_PLANES, buildFontPaletteWithAlpha()
            );
            fontTexture = ctx.assets().createTextureFromARGB(fontPixels, FONT_W, FONT_H);
            log.info("Font texture loaded ({} glyphs 16x16)", FONT_NUM_CHARS);
        } else {
            log.warn("font16x16.raw2 not found");
        }

        // ── Credits frame ─────────────────────────────────────────────────────
        creditsRaw = loadRaw(root.resolve("credits_only.raw"));
        if (creditsRaw != null) {
            // 3 bitplanes, 320x192
            // Utilise les plans 3-5 de la palette complète (fontpal)
            int[] credPalette = buildCreditsPalette();
            int[] credPixels  = AmigaBitplaneDecoder.decode(
                creditsRaw, SCREEN_W, 192, FONT_PLANES, credPalette
            );
            creditsTexture = ctx.assets().createTextureFromARGB(credPixels, SCREEN_W, 192);
            log.info("Credits texture loaded");
        }

        loaded = true;
        log.info("Menu assets loaded OK");
    }

    // ── Palette helpers ───────────────────────────────────────────────────────

    /**
     * Palette font avec index 0 = transparent (couleur de fond = alpha 0).
     */
    private int[] buildFontPaletteWithAlpha() {
        int[] pal = new int[8];
        for (int i = 0; i < 8; i++) {
            pal[i] = fontpal[i];
        }
        pal[0] = 0x00000000; // index 0 transparent
        return pal;
    }

    private int[] buildCreditsPalette() {
        // Les credits sont dans les plans 3-5 du morescreen
        // Ils utilisent la même palette que la font (fontpal pour bits 5-7)
        int[] pal = new int[8];
        for (int i = 0; i < 8; i++) {
            pal[i] = fontpal[i];
        }
        pal[0] = 0x00000000;
        return pal;
    }

    private int[] loadPalFromFile(Path path, int count) {
        if (!Files.exists(path)) {
            log.debug("Palette file not found: {}", path.getFileName());
            return null;
        }
        try {
            byte[] data = Files.readAllBytes(path);
            int[] pal = AmigaBitplaneDecoder.loadPalette(data, count);
            // Toutes les entrées sont opaques
            for (int i = 0; i < pal.length; i++) {
                pal[i] |= 0xFF000000;
            }
            log.debug("Loaded palette {} ({} colors)", path.getFileName(), count);
            return pal;
        } catch (IOException e) {
            log.warn("Failed to load palette {}: {}", path.getFileName(), e.getMessage());
            return null;
        }
    }

    private byte[] loadRaw(Path path) {
        if (!Files.exists(path)) {
            log.debug("Raw file not found: {}", path.getFileName());
            return null;
        }
        try {
            byte[] data = Files.readAllBytes(path);
            log.debug("Loaded raw {} ({} bytes)", path.getFileName(), data.length);
            return data;
        } catch (IOException e) {
            log.warn("Failed to load {}: {}", path.getFileName(), e.getMessage());
            return null;
        }
    }

    // ── Palettes par défaut (si assets manquants) ─────────────────────────────

    private static int[] buildDefaultBackpal() {
        return new int[]{
            0xFF131317, 0xFF232B27, 0xFF1F231B, 0xFF131B1B
        };
    }

    private static int[] buildDefaultFirepal() {
        return new int[]{
            0xFF170B00, 0xFF321200, 0xFF4D1500, 0xFF681300,
            0xFF820C00, 0xFF933F05, 0xFFA1710B, 0xFFB2AC12
        };
    }

    private static int[] buildDefaultFontpal() {
        return new int[]{
            0xFF000000, 0xFF9BBF9B, 0xFF154B25, 0xFF2A674A,
            0xFF3E836E, 0xFF539F93, 0xFF002F00, 0xFFF7EFCF
        };
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int   getBackgroundTexture() { return backgroundTexture; }
    public int   getFontTexture()       { return fontTexture; }
    public int   getCreditsTexture()    { return creditsTexture; }
    public int[] getMenuPalette()       { return menuPalette; }
    public int[] getFirepal()           { return firepal; }
    public int[] getBackpal()           { return backpal; }
    public int[] getFontpal()           { return fontpal; }
    public byte[] getFontRaw()          { return fontRaw; }
    public boolean isLoaded()           { return loaded; }
}
