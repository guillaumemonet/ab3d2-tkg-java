package com.ab3d2.assets;

import java.nio.ByteBuffer;

/**
 * Décodeur de bitplanes Amiga vers RGBA.
 *
 * Format Amiga : N plans de bits consécutifs.
 * Chaque plan = ROWSIZE * HEIGHT bytes, où ROWSIZE = WIDTH/8.
 * Le bit 7 de chaque byte correspond au pixel le plus à gauche.
 *
 * Pour N plans : index couleur = bit0_plan0 | (bit_plan1 << 1) | ... | (bit_planN-1 << N-1)
 */
public final class AmigaBitplaneDecoder {

    private AmigaBitplaneDecoder() {}

    /**
     * Décode N bitplanes séquentiels vers un tableau int[] ARGB.
     *
     * @param data      données brutes (N plans séquentiels)
     * @param width     largeur en pixels
     * @param height    hauteur en pixels
     * @param numPlanes nombre de plans (1..8)
     * @param palette   palette ARGB, doit avoir au moins 2^numPlanes entrées
     * @return          tableau int[] ARGB de width*height pixels
     */
    public static int[] decode(byte[] data, int width, int height, int numPlanes, int[] palette) {
        int rowBytes  = (width + 7) / 8;  // ROWSIZE = ceil(width/8)
        int planeSize = rowBytes * height;
        int[] pixels  = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int byteIdx = y * rowBytes + (x / 8);
                int bitIdx  = 7 - (x % 8);
                int colorIdx = 0;

                for (int p = 0; p < numPlanes; p++) {
                    int planeByte = data[p * planeSize + byteIdx] & 0xFF;
                    int bit = (planeByte >> bitIdx) & 1;
                    colorIdx |= (bit << p);
                }

                pixels[y * width + x] = palette[colorIdx];
            }
        }
        return pixels;
    }

    /**
     * Décode vers ByteBuffer RGBA (pour upload OpenGL direct).
     */
    public static ByteBuffer decodeToRGBA(byte[] data, int width, int height, int numPlanes, int[] palette) {
        int[] argb = decode(data, width, height, numPlanes, palette);
        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        for (int px : argb) {
            buf.put((byte) ((px >> 16) & 0xFF)); // R
            buf.put((byte) ((px >>  8) & 0xFF)); // G
            buf.put((byte) ( px        & 0xFF)); // B
            buf.put((byte) ((px >> 24) & 0xFF)); // A
        }
        buf.flip();
        return buf;
    }

    /**
     * Construit une palette ARGB depuis des données binaires format AB3D2.
     * Format : N entrées * 4 bytes = [0x00, R, G, B]
     */
    public static int[] loadPalette(byte[] data, int count) {
        int[] pal = new int[count];
        for (int i = 0; i < count && i * 4 + 3 < data.length; i++) {
            int r = data[i * 4 + 1] & 0xFF;
            int g = data[i * 4 + 2] & 0xFF;
            int b = data[i * 4 + 3] & 0xFF;
            pal[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return pal;
    }

    /**
     * Construit la palette 256 couleurs complète du menu AB3D2.
     * Reproduit exactement mnu_createpalette() du source C/ASM.
     *
     * Structure palette (index 0..255) :
     *   bits 5-7 != 0 (index 32..255) -> fontpal[bits5-7]
     *   bits 2-4 != 0 (index 4..31)   -> mélange firepal[bits2-4] + firepal[bits0-1]
     *   sinon   (index 0..3)           -> backpal[bits0-1]
     */
    public static int[] buildMenuPalette(int[] backpal, int[] firepal, int[] fontpal) {
        int[] palette = new int[256];

        for (int c = 0; c < 256; c++) {
            if ((c & 0xE0) != 0) {
                // bits 5-7 : font
                palette[c] = fontpal[(c >> 5) & 7];
            } else if ((c & 0x1C) != 0) {
                // bits 2-4 : feu (mélange de deux entrées firepal)
                int fi1 = (c & 0x1C) >> 2;  // bits 2-4
                int fi2 = (c & 0x03);        // bits 0-1
                int c1 = firepal[fi1];
                int c2 = firepal[fi2];

                // Rouge : somme simple, clampé
                int r = ((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF);
                if (r > 255) r = 255;

                // Vert : c1.g * 3/4 + c2.g, clampé  (atténuation comme dans l'original)
                int g = (((c1 >> 8) & 0xFF) * 3) / 4 + ((c2 >> 8) & 0xFF);
                if (g > 255) g = 255;

                // Bleu : somme simple, clampé
                int b = (c1 & 0xFF) + (c2 & 0xFF);
                if (b > 255) b = 255;

                palette[c] = 0xFF000000 | (r << 16) | (g << 8) | b;
            } else {
                // bits 0-1 : background
                palette[c] = backpal[c & 3];
            }
        }
        return palette;
    }
}
