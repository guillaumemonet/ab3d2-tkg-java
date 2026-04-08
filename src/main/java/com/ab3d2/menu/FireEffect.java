package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.GL33.*;

/**
 * Simulation du blitter feu AB3D2 sur 6 plans binaires.
 *
 * Plans 0,1,2 : feu (modifié par le blitter chaque frame)
 * Plans 3,4,5 : texte (injecté via setTextLayer, jamais écrasé par le blitter)
 *
 * LF = 0xf8 → D = A | (B & C)
 *
 * ── Dégradé de couleur ──────────────────────────────────────────────────────
 *
 * Les 3 plans feu ont des probabilités B DIFFÉRENTES, ce qui crée des zones
 * d'altitude différente pour chaque plan → indices 1-7 tous produits → dégradé.
 *
 *   Plan 0 (bit 0 de l'index) : B = 1 bit  → P=50%  → meurt proche du texte
 *   Plan 1 (bit 1)            : B = 2 bits OR → P=75%  → hauteur intermédiaire
 *   Plan 2 (bit 2)            : B = 3 bits OR → P=87.5% → monte le plus haut
 *
 * Index couleur selon les plans allumés :
 *   7 (111) → near text    → firepal mix chaud (rouge/orange)
 *   6 (110) → un peu plus  → firepal mix tiède
 *   4 (100) → plus haut    → firepal mix froid
 *   0 (000) → tout en haut → transparent
 *
 * → gradient naturel chaud→froid en montant depuis le texte.
 */
public class FireEffect {

    private static final Logger log = LoggerFactory.getLogger(FireEffect.class);

    public static final int W = MenuAssets.SCREEN_W;   // 320
    public static final int H = MenuAssets.SCREEN_H;   // 256

    private final boolean[][] plan = new boolean[6][W * H];
    private final int[] src = { 3, 4, 5 };

    private final FirePlots plots;

    private int rndState   = 0x54424C21;
    private int frameCount = 0;

    private final ByteBuffer pixelBuf;
    private int texture = -1;
    private final int[] palette;

    public FireEffect(int[] menuPalette) {
        this.palette  = menuPalette;
        this.pixelBuf = ByteBuffer.allocateDirect(W * H * 4).order(ByteOrder.nativeOrder());
        this.plots    = new FirePlots();
    }

    // ── Init GL ───────────────────────────────────────────────────────────────

    public void init() {
        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, W, H, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
        upload();
        log.info("FireEffect initialized — texture id={}", texture);
    }

    // ── Injection texte ───────────────────────────────────────────────────────

    public void setTextLayer(int[] textBits) {
        for (int i = 0; i < W * H; i++) {
            plan[3][i] = (textBits[i] & 0x08) != 0;
            plan[4][i] = (textBits[i] & 0x10) != 0;
            plan[5][i] = (textBits[i] & 0x20) != 0;
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update() {
        frameCount++;
        plots.update();

        // Feu à 30 Hz — 2 passes par cycle pour la hauteur du feu
        if ((frameCount & 1) == 0) {
            rotateSrc();
            propagate();
            propagate();
        }

        plotPoints();
        upload();
    }

    // ── Rotation sourceptrs ───────────────────────────────────────────────────

    private void rotateSrc() {
        int tmp = src[0];
        src[0]  = src[1];
        src[1]  = src[2];
        src[2]  = tmp;
    }

    // ── Propagation blitter ────────────────────────────────────────────────────
    /**
     * LF=0xf8 → D = A | (B & C)
     *
     * B : probabilité DIFFÉRENTE par plan pour créer le dégradé de couleur.
     *   Plan 0 : 1 bit  LFSR → P(B=1) = 50%   → meurt vite → index bas (rouge vif)
     *   Plan 1 : 2 bits OR   → P(B=1) = 75%   → hauteur moyenne
     *   Plan 2 : 3 bits OR   → P(B=1) = 87.5% → monte le plus haut (couleurs plus froides)
     *
     * C : voisinage horizontal 3 pixels (x-1, x, x+1 à y+1)
     *   → diffusion latérale naturelle du feu.
     *
     * Décalage de A selon count & 3 (fidèle blitter original) :
     *   0 → x-1 (ASH=1), 1 → x+1 (ASH=15+ptr+2), 2,3 → x
     */
    private void propagate() {
        int count = (frameCount >> 1) & 3;

        for (int p = 0; p < 3; p++) {
            boolean[] A    = plan[src[p]];
            boolean[] fire = plan[p];

            for (int y = 0; y < H - 1; y++) {
                int rowCurr  = y       * W;
                int rowBelow = (y + 1) * W;

                for (int x = 0; x < W; x++) {
                    // Source A avec décalage
                    final int ax = switch (count) {
                        case 0  -> (x > 0)     ? x - 1 : 0;
                        case 1  -> (x < W - 1) ? x + 1 : W - 1;
                        default -> x;
                    };
                    final boolean a = A[rowBelow + ax];

                    // Source B : probabilité croissante selon le plan
                    // → les 3 plans ont des hauteurs de feu différentes → dégradé
                    final boolean b = switch (p) {
                        case 0  -> nextRndBit();                                     // 50%
                        case 1  -> nextRndBit() | nextRndBit();                      // 75%
                        default -> nextRndBit() | nextRndBit() | nextRndBit();       // 87.5%
                    };

                    // Source C : voisinage horizontal 3 pixels → diffusion latérale
                    boolean c = fire[rowBelow + x];
                    if (!c && x > 0)     c = fire[rowBelow + x - 1];
                    if (!c && x < W - 1) c = fire[rowBelow + x + 1];

                    // LF = 0xf8 : D = A | (B & C)
                    fire[rowCurr + x] = a || (b && c);
                }
            }
        }
    }

    // ── Plots Lissajous ───────────────────────────────────────────────────────

    private void plotPoints() {
        int[] pos = plots.getPositions();
        for (int i = 0; i < FirePlots.NUM_POINTS; i++) {
            int px = pos[i * 2];
            int py = pos[i * 2 + 1];
            if (px >= 0 && px < W && py >= 0 && py < H) {
                int idx = py * W + px;
                plan[0][idx] = true;
                plan[1][idx] = true;
                plan[2][idx] = true;
            }
        }
    }

    // ── Upload GL ─────────────────────────────────────────────────────────────

    private void upload() {
        if (texture < 0) return;
        pixelBuf.clear();
        for (int i = 0; i < W * H; i++) {
            int idx = 0;
            if (plan[0][i]) idx |=  1;
            if (plan[1][i]) idx |=  2;
            if (plan[2][i]) idx |=  4;
            if (plan[3][i]) idx |=  8;
            if (plan[4][i]) idx |= 16;
            if (plan[5][i]) idx |= 32;
            int argb  = palette[idx];
            int alpha = (idx == 0) ? 0 : 0xFF;
            pixelBuf.put((byte) ((argb >> 16) & 0xFF));
            pixelBuf.put((byte) ((argb >>  8) & 0xFF));
            pixelBuf.put((byte) ( argb         & 0xFF));
            pixelBuf.put((byte)  alpha);
        }
        pixelBuf.flip();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, W, H, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuf);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // ── LFSR ─────────────────────────────────────────────────────────────────

    private boolean nextRndBit() {
        int bit = rndState & 1;
        rndState >>>= 1;
        if (bit == 1) rndState ^= 0xB4BCD35C;
        return bit == 1;
    }

    // ── Destroy ───────────────────────────────────────────────────────────────

    public void destroy() {
        if (texture >= 0) { glDeleteTextures(texture); texture = -1; }
    }

    public int getTexture() { return texture; }
}
