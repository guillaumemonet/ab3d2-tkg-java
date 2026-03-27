package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;
import com.ab3d2.render.Renderer2D;

/**
 * Rendu de la police 16x16 originale d'AB3D2.
 *
 * Reproduit mnu_printxy du source ASM :
 *   - Police = image 320x176px (20 glyphes * 11 lignes)
 *   - Glyphe = char - 32 (ASCII 32 = espace = premier glyphe)
 *   - Position dans atlas : X = (c % 20) * 16, Y = (c / 20) * 16
 *   - Caractère < 32 = newline (descend de 20px, retour à la colonne de base)
 *
 * Dans l'original, le texte est rendu dans les bitplanes 3-5 de mnu_morescreen.
 * En Java, on rend directement dans le framebuffer via le sprite batch.
 */
public class Ab3dFont {

    private static final int GLYPH_W    = MenuAssets.FONT_GLYPH_W;  // 16
    private static final int GLYPH_H    = MenuAssets.FONT_GLYPH_H;  // 16
    private static final int FONT_COLS  = MenuAssets.FONT_COLS;      // 20
    private static final int ATLAS_W    = MenuAssets.FONT_W;         // 320
    private static final int ATLAS_H    = MenuAssets.FONT_H;         // 176
    private static final int FIRST_CHAR = MenuAssets.FONT_FIRST_CHAR; // 32

    private final int fontTexture;

    public Ab3dFont(int fontTexture) {
        this.fontTexture = fontTexture;
    }

    /**
     * Dessine une chaîne.
     * @param r      renderer
     * @param text   texte à afficher
     * @param x      position X en pixels (320x200)
     * @param y      position Y en pixels
     * @param scaleX facteur d'échelle X (1.0 = taille native)
     * @param scaleY facteur d'échelle Y
     */
    public void drawString(Renderer2D r, String text, float x, float y, float scaleX, float scaleY) {
        if (fontTexture < 0) return;

        float curX = x;
        float curY = y;
        float baseX = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Newline dans l'original = char < 32 (valeur 1 dans les données ASM)
            if (c < FIRST_CHAR) {
                curY += 20 * scaleY;
                curX = baseX;
                continue;
            }

            int idx = c - FIRST_CHAR;
            if (idx >= MenuAssets.FONT_NUM_CHARS) {
                curX += GLYPH_W * scaleX;
                continue;
            }

            // Position dans l'atlas (image 320x176)
            int atlasX = (idx % FONT_COLS) * GLYPH_W;
            int atlasY = (idx / FONT_COLS) * GLYPH_H;

            float u0 = (float) atlasX / ATLAS_W;
            float v0 = (float) atlasY / ATLAS_H;
            float u1 = (float)(atlasX + GLYPH_W) / ATLAS_W;
            float v1 = (float)(atlasY + GLYPH_H) / ATLAS_H;

            r.drawTexture(fontTexture, curX, curY, GLYPH_W * scaleX, GLYPH_H * scaleY,
                          u0, v0, u1, v1);

            curX += GLYPH_W * scaleX;
        }
    }

    /** Raccourci taille 1:1. */
    public void drawString(Renderer2D r, String text, float x, float y) {
        drawString(r, text, x, y, 1f, 1f);
    }

    /** Calcule la largeur en pixels d'une chaîne. */
    public float stringWidth(String text, float scaleX) {
        int maxWidth = 0, curWidth = 0;
        for (char c : text.toCharArray()) {
            if (c < FIRST_CHAR) { maxWidth = Math.max(maxWidth, curWidth); curWidth = 0; }
            else curWidth += GLYPH_W;
        }
        return Math.max(maxWidth, curWidth) * scaleX;
    }

    /** Centre horizontalement un texte dans une zone. */
    public float centerX(String text, float areaWidth, float scaleX) {
        return (areaWidth - stringWidth(text, scaleX)) / 2f;
    }

    public int getFontTexture() { return fontTexture; }
}