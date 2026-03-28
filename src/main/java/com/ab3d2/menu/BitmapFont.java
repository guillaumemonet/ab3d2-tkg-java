package com.ab3d2.menu;

import com.ab3d2.core.GameContext;
import com.ab3d2.render.Renderer2D;
import org.lwjgl.stb.STBTruetype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * Police bitmap 8x8 pour l'UI.
 * Utilise une police intégrée (Commodore/Amiga-like) ou génère une grille ASCII.
 *
 * Les vraies polices AB3D2 sont des bitmaps 8x8 encodés dans les assets.
 * On fournit un fallback procédural en attendant.
 *
 * Couleurs prédéfinies (compatibles palette Amiga).
 */
public class BitmapFont {

    private static final Logger log = LoggerFactory.getLogger(BitmapFont.class);

    public static final int COLOR_WHITE     = 0xFFFFFFFF;
    public static final int COLOR_YELLOW    = 0xFFFFFF00;
    public static final int COLOR_RED       = 0xFFFF2020;
    public static final int COLOR_ORANGE    = 0xFFFF8000;
    public static final int COLOR_GREEN     = 0xFF00FF00;
    public static final int COLOR_DARK_GREY = 0xFF808080;
    public static final int COLOR_CYAN      = 0xFF00FFFF;

    private static final int CHAR_W   = 8;
    private static final int CHAR_H   = 8;
    private static final int COLS     = 16; // 16 chars par ligne dans l'atlas
    private static final int ROWS     = 8;  // 8 lignes = 128 chars (ASCII 32..159)
    private static final int ATLAS_W  = COLS * CHAR_W; // 128
    private static final int ATLAS_H  = ROWS * CHAR_H; // 64

    private int atlasTexture;
    private boolean initialized = false;

    // Cache des atlas colorés (pour éviter de re-coloriser à chaque frame)
    private final java.util.Map<Integer, Integer> colorCache = new java.util.HashMap<>();

    public BitmapFont(GameContext ctx) {
        atlasTexture = buildProceduralAtlas(ctx);
        initialized  = true;
    }

    /**
     * Dessine une chaîne dans le Renderer2D en pixel coords.
     * @param color couleur ARGB (ex: COLOR_WHITE)
     */
    public void drawString(Renderer2D r, String text, int x, int y, int color) {
        if (!initialized) return;

        // Pour l'instant on utilise l'atlas blanc et on colore via la couleur de tint
        // (quand on passera au sprite shader avec uniform color)
        // En attendant : atlas séparé par couleur (simplifié)
        int tex = getColoredAtlas(color);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c >= 32 + COLS * ROWS) continue;

            int idx = c - 32;
            int cx  = (idx % COLS) * CHAR_W;
            int cy  = (idx / COLS) * CHAR_H;

            float u0 = (float) cx / ATLAS_W;
            float v0 = (float) cy / ATLAS_H;
            float u1 = (float)(cx + CHAR_W) / ATLAS_W;
            float v1 = (float)(cy + CHAR_H) / ATLAS_H;

            r.drawTexture(tex, x + i * CHAR_W, y, CHAR_W, CHAR_H, u0, v0, u1, v1);
        }
    }

    /**
     * Construit un atlas 8x8 procédural.
     * On encode les glyphes de base en tableaux de bits (style Commodore 64).
     */
    private int buildProceduralAtlas(GameContext ctx) {
        // Atlas blanc sur fond transparent
        int[] pixels = new int[ATLAS_W * ATLAS_H]; // ARGB

        // On copie les glyphes C64/Amiga-like depuis les données intégrées
        int[][] glyphs = C64Font.GLYPHS;

        for (int charIdx = 0; charIdx < Math.min(glyphs.length, COLS * ROWS); charIdx++) {
            int[] glyph = glyphs[charIdx];
            int tileX = (charIdx % COLS) * CHAR_W;
            int tileY = (charIdx / COLS) * CHAR_H;

            for (int row = 0; row < 8 && row < glyph.length; row++) {
                int bits = glyph[row];
                for (int col = 0; col < 8; col++) {
                    boolean on = (bits & (0x80 >> col)) != 0;
                    pixels[(tileY + row) * ATLAS_W + (tileX + col)] =
                        on ? 0xFFFFFFFF : 0x00000000;
                }
            }
        }

        return ctx.assets().createTextureFromARGB(pixels, ATLAS_W, ATLAS_H);
    }

    /**
     * Retourne (ou crée) un atlas teinté pour la couleur donnée.
     * Simple : on tinte l'atlas blanc à la couleur voulue.
     */
    private int getColoredAtlas(int color) {
        if (color == COLOR_WHITE) return atlasTexture;
        return colorCache.computeIfAbsent(color, c -> {
            // Pour l'instant, retourne l'atlas blanc (tinting via shader à venir)
            // TODO : uniform color dans le sprite shader
            return atlasTexture;
        });
    }

    public void destroy() {
        glDeleteTextures(atlasTexture);
        colorCache.values().forEach(t -> { if (t != atlasTexture) glDeleteTextures(t); });
        colorCache.clear();
    }
}
