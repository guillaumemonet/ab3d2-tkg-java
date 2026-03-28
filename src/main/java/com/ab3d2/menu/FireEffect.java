package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.lwjgl.opengl.GL33.*;

/**
 * Simulation fidèle du blitter feu AB3D2 sur 6 plans binaires.
 *
 * Plans 0,1,2 : feu (destination des passes blitter, modifié chaque frame)
 * Plans 3,4,5 : texte source (mnu_printxy, jamais modifié par le blitter)
 *
 * Blitter (mnu_pass1/2/3), 3 passes par frame à 30 Hz :
 *   ROTATION sourceptrs avant les passes (mnu_dofire) :
 *     tmp=src[0]; src[0]=src[1]; src[1]=src[2]; src[2]=tmp
 *
 *   pass p (p=0,1,2) :
 *     A = plan[src[p]][y+1]   (sourceptrs[p], plan texte décalé +1 ligne)
 *     B = random 1-bit        (mnu_rndptr, getrnd)
 *     C = plan[p][y+1]        (plan feu courant, décalé +1 ligne)
 *     D = plan[p][y]          (plan feu destination, ligne courante)
 *     LF selon count&3 :
 *       0 -> 0x1f : D = !A | (A & !B & !C)
 *       1 -> 0xff : D = true  (+ A lu 16px à gauche pour diffusion horiz)
 *       2 -> 0x0f : D = !A    (cas le plus fréquent)
 *       3 -> 0x0f : D = !A
 *
 * Plans 0,1,2 modifiés aussi par mnu_plot (bset direct -> points Lissajous).
 *
 * Index couleur = bit0*plan0 | bit1*plan1 | ... | bit5*plan5 :
 *   plans 0-2 seuls (feu) -> index 7 -> palette[7] = rouge-brun (154,32,0)
 *   plans 3-5 seuls (texte) -> index 56 -> palette[56] = vert fontpal[1]
 *   index 0 (tout à 0) -> transparent (fond = background)
 */
public class FireEffect {

    private static final Logger log = LoggerFactory.getLogger(FireEffect.class);

    public static final int W = MenuAssets.SCREEN_W;  // 320
    public static final int H = MenuAssets.SCREEN_H;  // 256

    // 6 plans binaires
    private final boolean[][] plan = new boolean[6][W * H];

    // Indices des sourceptrs rotatifs (pointent vers plans 3, 4 ou 5)
    // Initialement : src[0]=3, src[1]=4, src[2]=5
    private final int[] src = { 3, 4, 5 };

    private final FirePlots plots;

    // LFSR (mnu_rnd, getrnd)
    private int  rnd        = 0x54424C21;
    private int  frameCount = 0;

    private final ByteBuffer rgba;
    private int texture = -1;
    private final int[] palette;

    public FireEffect(int[] menuPalette) {
        this.palette = menuPalette;
        this.rgba    = ByteBuffer.allocateDirect(W * H * 4).order(ByteOrder.nativeOrder());
        this.plots   = new FirePlots();
    }

    public void init() {
        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, W, H, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
        upload();
        log.info("FireEffect initialized");
    }

    /**
     * Injecte le texte dans les plans 3,4,5.
     * textBits[i] = (b0<<3)|(b1<<4)|(b2<<5) depuis MenuRenderer.
     */
    public void setTextLayer(int[] textBits) {
        for (int i = 0; i < W * H; i++) {
            plan[3][i] = (textBits[i] & 0x08) != 0;
            plan[4][i] = (textBits[i] & 0x10) != 0;
            plan[5][i] = (textBits[i] & 0x20) != 0;
        }
    }

    public void update() {
        frameCount++;
        plots.update();

        // Feu à 30 Hz (frames paires)
        if ((frameCount & 1) == 0) {
            rotateSrc();
            propagate();
        }

        // Points Lissajous chaque frame
        plotPoints();
        upload();
    }

    /** Rotation des sourceptrs (mnu_dofire avant les passes). */
    private void rotateSrc() {
        int tmp = src[0];
        src[0]  = src[1];
        src[1]  = src[2];
        src[2]  = tmp;
    }

    /** 3 passes blitter. */
    private void propagate() {
        int count = (frameCount >> 1) & 3;

        for (int p = 0; p < 3; p++) {
            boolean[] A    = plan[src[p]]; // sourceptrs[p] -> plan texte (3,4 ou 5)
            boolean[] C    = plan[p];      // plan feu courant
            boolean[] dest = plan[p];      // même tableau, on écrit ligne y depuis y+1

            // Copie temporaire pour éviter d'écraser pendant le calcul
            // (le blitter lit et écrit en même temps mais direction normale = pas de conflit
            //  car on lit y+1 et écrit y, pas d'overlap)
            for (int y = 0; y < H - 1; y++) {
                for (int x = 0; x < W; x++) {
                    int here  = y * W + x;
                    int below = (y + 1) * W + x;

                    boolean a = A[below];
                    boolean b = (rnd() & 1) == 1;
                    boolean c = C[below];

                    boolean d;
                    switch (count) {
                        case 0 ->
                            // LF=0x1f : D = !A | (A & !B & !C)
                            d = !a || (!b && !c);
                        case 1 -> {
                            // LF=0xff : D=1 + A décalé -2 bytes (16px gauche)
                            // subtract=-2 => adresse A = sourceptrs - (-2) = +2 bytes = +16px
                            // -> pour chaque pixel, A est lu 16px à droite
                            int ax = Math.min(W - 1, x + 16);
                            boolean aShifted = A[(y + 1) * W + ax];
                            d = true; // LF=0xff -> D=1 toujours
                        }
                        default ->
                            // LF=0x0f : D = !A
                            d = !a;
                    }

                    dest[here] = d;
                }
            }
            // Ligne H-1 (bas) : non touchée par le blitter (bltsize = H-1 lignes)
            // Elle reste inchangée -> le texte en bas reste visible
        }
    }

    /**
     * mnu_plot : bset dans plans 0,1,2 pour les 50 points.
     * -> index = 7 (bits 0-2 tous à 1) -> palette[7] = rouge-brun
     */
    private void plotPoints() {
        int[] pos = plots.getPositions();
        for (int i = 0; i < FirePlots.NUM_POINTS; i++) {
            bset012(pos[i*2], pos[i*2+1]);
        }
    }

    private void bset012(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H) return;
        int i = y * W + x;
        plan[0][i] = true;
        plan[1][i] = true;
        plan[2][i] = true;
    }

    private void upload() {
        rgba.clear();
        for (int i = 0; i < W * H; i++) {
            // Reconstruit l'index couleur depuis les 6 plans
            int idx = 0;
            if (plan[0][i]) idx |= 1;
            if (plan[1][i]) idx |= 2;
            if (plan[2][i]) idx |= 4;
            if (plan[3][i]) idx |= 8;
            if (plan[4][i]) idx |= 16;
            if (plan[5][i]) idx |= 32;

            int argb  = palette[idx];
            // Index 0 = fond (tout à 0) = transparent -> laisse voir le background
            int alpha = (idx == 0) ? 0 : 0xFF;
            rgba.put((byte)((argb >> 16) & 0xFF));
            rgba.put((byte)((argb >>  8) & 0xFF));
            rgba.put((byte)( argb        & 0xFF));
            rgba.put((byte) alpha);
        }
        rgba.flip();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, W, H, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // LFSR 32-bit (mnu_rnd, bits 0,2,3,5)
    private int rnd() {
        int b = ((rnd) ^ (rnd >> 2) ^ (rnd >> 3) ^ (rnd >> 5)) & 1;
        rnd = (rnd >>> 1) | (b << 31);
        return rnd & 0x7FFF;
    }

    public void destroy() {
        if (texture >= 0) { glDeleteTextures(texture); texture = -1; }
    }
    public int getTexture() { return texture; }
}
