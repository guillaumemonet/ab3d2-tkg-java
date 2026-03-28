package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;
import java.util.Arrays;

/**
 * Rendu des glyphes dans le buffer texte (plans 3,4,5 de mnu_morescreen).
 *
 * Un pixel actif dans les 3 plans de la font -> plans 3,4,5 tous à 1
 * -> colorBits = (b0<<3)|(b1<<4)|(b2<<5)
 * -> index 56 (0b00111000) -> palette[56] = fontpal[1] = vert
 *
 * setTextLayer dans FireEffect lit les bits 3,4,5 :
 *   plan[3] = bit3 (0x08)
 *   plan[4] = bit4 (0x10)
 *   plan[5] = bit5 (0x20)
 */
public class MenuRenderer {

    private static final int W             = MenuAssets.SCREEN_W;
    private static final int H             = MenuAssets.SCREEN_H;
    private static final int GLYPH_W       = MenuAssets.FONT_GLYPH_W;   // 16
    private static final int GLYPH_H       = MenuAssets.FONT_GLYPH_H;   // 16
    private static final int FONT_COLS     = MenuAssets.FONT_COLS;       // 20
    private static final int ATLAS_ROW     = W / 8;                      // 40 bytes/ligne atlas
    private static final int FONT_PLANE_SZ = ATLAS_ROW * MenuAssets.FONT_H; // 40*176

    // textLayer[i] : bits 3,4,5 si pixel de glyphe actif
    private final int[] textLayer = new int[W * H];

    private final byte[] fontRaw;

    public MenuRenderer(byte[] fontRaw) {
        this.fontRaw = fontRaw;
    }

    public void clear() {
        Arrays.fill(textLayer, 0);
    }

    /**
     * Dessine une chaîne.
     * @param text     texte (majuscules)
     * @param xBytes   X en bytes (1 byte = 8 pixels)
     * @param yPx      Y en pixels
     */
    public void drawString(String text, int xBytes, int yPx) {
        if (fontRaw == null) return;
        int curX = xBytes * 8;
        int baseX = curX;
        int curY = yPx;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32) {          // newline (char < 32 dans mnu_printxy)
                curY += 20;
                curX  = baseX;
                continue;
            }
            int idx = c - 32;
            if (idx >= MenuAssets.FONT_NUM_CHARS) { curX += GLYPH_W; continue; }
            drawGlyph(idx, curX, curY);
            curX += GLYPH_W;
        }
    }

    /**
     * Reproduit mnu_printxy :
     *   move.w (a3,d3.l),(a6)           <- plan0 font -> plan3 morescreen
     *   move.w (a4,d3.l),40*256(a6)     <- plan1 font -> plan4
     *   move.w (a5,d3.l),40*256*2(a6)   <- plan2 font -> plan5
     *
     * colorBits = (b0<<3)|(b1<<4)|(b2<<5)
     *   plan font 0 -> bit3 de l'index
     *   plan font 1 -> bit4
     *   plan font 2 -> bit5
     */
    private void drawGlyph(int idx, int dstX, int dstY) {
        int atlasX = (idx % FONT_COLS) * GLYPH_W;
        int atlasY = (idx / FONT_COLS) * GLYPH_H;

        for (int row = 0; row < GLYPH_H; row++) {
            int py = dstY + row;
            if (py < 0 || py >= H) continue;

            int lineY   = atlasY + row;
            int byteOff = lineY * ATLAS_ROW + atlasX / 8;

            // 2 bytes par ligne (glyphe 16px = 2 bytes)
            int p0  = safe(byteOff,                  fontRaw);
            int p0b = safe(byteOff + 1,               fontRaw);
            int p1  = safe(byteOff     + FONT_PLANE_SZ, fontRaw);
            int p1b = safe(byteOff + 1 + FONT_PLANE_SZ, fontRaw);
            int p2  = safe(byteOff     + FONT_PLANE_SZ*2, fontRaw);
            int p2b = safe(byteOff + 1 + FONT_PLANE_SZ*2, fontRaw);

            int bits0 = (p0 << 8) | p0b;
            int bits1 = (p1 << 8) | p1b;
            int bits2 = (p2 << 8) | p2b;

            for (int col = 0; col < GLYPH_W; col++) {
                int px = dstX + col;
                if (px < 0 || px >= W) continue;
                int bit = 15 - col;   // MSB en premier (Amiga bitplane)
                int b0 = (bits0 >> bit) & 1;
                int b1 = (bits1 >> bit) & 1;
                int b2 = (bits2 >> bit) & 1;
                // plan font 0 -> plan3 (bit3), plan1 -> plan4 (bit4), plan2 -> plan5 (bit5)
                int colorBits = (b0 << 3) | (b1 << 4) | (b2 << 5);
                if (colorBits != 0) {
                    textLayer[py * W + px] = colorBits;
                }
            }
        }
    }

    private int safe(int off, byte[] data) {
        return (off >= 0 && off < data.length) ? (data[off] & 0xFF) : 0;
    }

    public int[] getTextLayer() { return textLayer; }
}
