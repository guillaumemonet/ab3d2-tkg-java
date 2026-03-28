package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;

/**
 * Rendu du texte dans le buffer bitplane (plans 3-5) du feu.
 *
 * Dans l'original (mnu_printxy) : - Le texte est écrit dans mnu_morescreen +
 * 3*40*256 (plan 3) - Chaque glyphe 16x16 est copié en 3 plans simultanément -
 * Les bits 3-5 d'un pixel texte sont tous à 1 -> index = 0b00111000 = 56 ->
 * mnu_createpalette(56) = firepal[6] = orange chaud - Le feu propage ces pixels
 * vers le haut -> lettres brûlent par le bas
 *
 * Ici on génère le buffer textLayer[W*H] avec bits 3-5 = 0x38 pour les pixels
 * actifs, 0 pour les pixels vides (transparent). Ce buffer est injecté dans
 * FireEffect.
 */
public class MenuRenderer {

    private static final int W = MenuAssets.SCREEN_W;   // 320
    private static final int H = MenuAssets.SCREEN_H;   // 256
    private static final int GLYPH_W = MenuAssets.FONT_GLYPH_W; // 16
    private static final int GLYPH_H = MenuAssets.FONT_GLYPH_H; // 16
    private static final int FONT_COLS = MenuAssets.FONT_COLS;    // 20
    // FONT_PLANES = 3 plans séquentiels dans font16x16.raw2
    private static final int FONT_PLANE_SIZE = (W / 8) * MenuAssets.FONT_H; // 40*176 = 7040

    // Buffer texte : bits 3-5 = 0x38 si pixel actif
    private final int[] textLayer = new int[W * H];

    // Données brutes de la font (3 plans séquentiels 320x176)
    private final byte[] fontRaw;

    public MenuRenderer(byte[] fontRaw) {
        this.fontRaw = fontRaw;
    }

    /**
     * Efface le buffer texte.
     */
    public void clear() {
        java.util.Arrays.fill(textLayer, 0);
    }

    /**
     * Dessine une chaîne dans le buffer texte. Reproduit mnu_printxy : xPos en
     * bytes (1 byte = 8 pixels), yPos en pixels.
     *
     * @param text texte en majuscules
     * @param xBytes position X en bytes (comme xPos ASM)
     * @param yPx position Y en pixels
     */
    public void drawString(String text, int xBytes, int yPx) {
        if (fontRaw == null) {
            return;
        }

        int curXPx = xBytes * 8;  // conversion bytes -> pixels
        int curYPx = yPx;
        int baseXPx = curXPx;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Char < 32 = newline (descend de 20px, retour X de base)
            if (c < 32) {
                curYPx += 20;
                curXPx = baseXPx;
                continue;
            }

            int idx = c - 32;
            if (idx >= MenuAssets.FONT_NUM_CHARS) {
                curXPx += GLYPH_W;
                continue;
            }

            drawGlyph(idx, curXPx, curYPx);
            curXPx += GLYPH_W;
        }
    }

    /**
     * Dessine un glyphe dans le buffer textLayer. Reproduit les 3 moves de
     * mnu_printxy : move.w (a3,d3.l),(a6) plan 0 de la font -> plan 3 de
     * morescreen move.w (a4,d3.l),40*256(a6) plan 1 -> plan 4 move.w
     * (a5,d3.l),40*256*2(a6) plan 2 -> plan 5
     *
     * Un pixel actif dans les 3 plans -> bits 3,4,5 = 1 -> index 0x38 = 56 Un
     * pixel actif dans plan 0 seulement -> bit 3 = 1 -> index 0x08 = 8 etc. ->
     * couleurs variées dans la zone firepal
     */
    private void drawGlyph(int idx, int dstX, int dstY) {
        if (fontRaw == null) {
            return;
        }

        // Position dans l'atlas font (image 320x176, 20 glyphes par ligne)
        int atlasCol = idx % FONT_COLS;
        int atlasRow = idx / FONT_COLS;
        int atlasX = atlasCol * GLYPH_W;   // pixel X dans l'atlas
        int atlasY = atlasRow * GLYPH_H;   // pixel Y dans l'atlas

        // Pour chaque ligne du glyphe
        for (int row = 0; row < GLYPH_H; row++) {
            int dstPixY = dstY + row;
            if (dstPixY < 0 || dstPixY >= H) {
                continue;
            }

            int atlasLineY = atlasY + row;

            // 2 bytes par ligne dans la font (16 pixels = 2 bytes)
            // Plan 0 à offset 0, Plan 1 à offset FONT_PLANE_SIZE, Plan 2 à offset 2*FONT_PLANE_SIZE
            int byteOffset = atlasLineY * (W / 8) + atlasX / 8;

            int p0 = (byteOffset < fontRaw.length) ? (fontRaw[byteOffset] & 0xFF) : 0;
            int p1 = (byteOffset + FONT_PLANE_SIZE < fontRaw.length) ? (fontRaw[byteOffset + FONT_PLANE_SIZE] & 0xFF) : 0;
            int p2 = (byteOffset + FONT_PLANE_SIZE * 2 < fontRaw.length) ? (fontRaw[byteOffset + FONT_PLANE_SIZE * 2] & 0xFF) : 0;

            // Aussi le byte suivant (le glyphe fait 16px = 2 bytes)
            int p0b = ((byteOffset + 1) < fontRaw.length) ? (fontRaw[byteOffset + 1] & 0xFF) : 0;
            int p1b = ((byteOffset + 1 + FONT_PLANE_SIZE) < fontRaw.length) ? (fontRaw[byteOffset + 1 + FONT_PLANE_SIZE] & 0xFF) : 0;
            int p2b = ((byteOffset + 1 + FONT_PLANE_SIZE * 2) < fontRaw.length) ? (fontRaw[byteOffset + 1 + FONT_PLANE_SIZE * 2] & 0xFF) : 0;

            // Combine les 2 bytes en 16 bits pour les 3 plans
            int bits0 = (p0 << 8) | p0b;
            int bits1 = (p1 << 8) | p1b;
            int bits2 = (p2 << 8) | p2b;

            for (int col = 0; col < GLYPH_W; col++) {
                int dstPixX = dstX + col;
                if (dstPixX < 0 || dstPixX >= W) {
                    continue;
                }

                int bit = 15 - col;  // MSB first (Amiga bitplane)
                int b0 = (bits0 >> bit) & 1;
                int b1 = (bits1 >> bit) & 1;
                int b2 = (bits2 >> bit) & 1;

                // Dans l'original: plans 0,1,2 de la font -> plans 3,4,5 de morescreen
                // -> bits 3,4,5 de l'index couleur
                int colorBits = (b0 << 5) | (b1 << 6) | (b2 << 7);

                if (colorBits != 0) {
                    textLayer[dstPixY * W + dstPixX] = colorBits;
                }
            }
        }
    }

    /**
     * Retourne le buffer texte (prêt pour FireEffect.setTextLayer).
     */
    public int[] getTextLayer() {
        return textLayer;
    }
}
