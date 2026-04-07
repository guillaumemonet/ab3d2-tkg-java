package com.ab3d2.game;

import com.ab3d2.core.PlayerState;
import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;

import java.util.Arrays;

/**
 * Renderer vue de dessus (top-down) du niveau.
 * Produit un buffer ARGB 320×200 uploadable comme texture OpenGL.
 *
 * <h2>Utilisation</h2>
 * <pre>
 * TopDownRenderer r = new TopDownRenderer(320, 200);
 * r.update(level, player);
 * int[] pixels = r.getPixels(); // upload vers GPU
 * </pre>
 *
 * <h2>Espace monde → écran</h2>
 * Les coordonnées monde sont des WORD signés (−32768..+32767).
 * On calcule automatiquement les bornes depuis les points du niveau
 * et on scale pour tenir dans 320×200 avec une marge.
 */
public class TopDownRenderer {

    // ── Couleurs ──────────────────────────────────────────────────────────────
    static final int COL_BG          = 0xFF0A0A0A; // fond noir
    static final int COL_WALL        = 0xFF888888; // mur solide
    static final int COL_PORTAL      = 0xFF00AAFF; // arête portail
    static final int COL_PLAYER      = 0xFF00FF00; // joueur (vert)
    static final int COL_PLAYER_DIR  = 0xFFFFFF00; // flèche direction (jaune)
    static final int COL_ZONE_TEXT   = 0xFF444444; // centre zone
    static final int COL_GRID        = 0xFF1A1A1A; // grille légère

    // ── Dimensions écran ─────────────────────────────────────────────────────
    private final int screenW;
    private final int screenH;
    private final int[] pixels;

    // ── Projection monde → écran ──────────────────────────────────────────────
    private float scaleX, scaleY;
    private float offsetX, offsetY;
    private boolean boundsComputed = false;

    public TopDownRenderer(int w, int h) {
        this.screenW = w;
        this.screenH = h;
        this.pixels  = new int[w * h];
    }

    // ── Rendu principal ───────────────────────────────────────────────────────

    /**
     * Met à jour le buffer de pixels avec la vue courante du niveau.
     *
     * @param level  données du niveau (peut être null → écran vide)
     * @param player état du joueur
     */
    public void update(LevelData level, PlayerState player) {
        Arrays.fill(pixels, COL_BG);

        if (level == null) {
            drawString("NO LEVEL DATA", 10, screenH / 2, 0xFFFF0000);
            return;
        }

        // Calculer la projection une seule fois par niveau chargé
        if (!boundsComputed && level.numPoints() > 0) {
            computeProjection(level);
            boundsComputed = true;
        }

        // Grille légère (repères visuels)
        drawGrid(10);

        // Arêtes du niveau
        if (level.edges != null) {
            for (int i = 0; i < level.numEdges(); i++) {
                ZEdge e = level.edges[i];
                if (e == null) continue;
                drawEdge(e);
            }
        }

        // Joueur
        if (player != null) {
            drawPlayer(player);
        }

        // HUD texte basique
        drawString(String.format("Z=%d A=%d", player != null ? player.currentZoneId : -1,
            player != null ? player.angle : 0), 2, 2, 0xFF00FF00);
    }

    // ── Rendu des arêtes ─────────────────────────────────────────────────────

    private void drawEdge(ZEdge e) {
        int x1 = worldToScreenX(e.pos().xi());
        int y1 = worldToScreenY(e.pos().zi());
        int x2 = worldToScreenX(e.pos().xi() + e.len().xi());
        int y2 = worldToScreenY(e.pos().zi() + e.len().zi());
        int col = e.isPortal() ? COL_PORTAL : COL_WALL;
        drawLine(x1, y1, x2, y2, col);
    }

    // ── Rendu du joueur ───────────────────────────────────────────────────────

    private void drawPlayer(PlayerState p) {
        int px = worldToScreenX(p.worldX());
        int py = worldToScreenY(p.worldZ());

        // Croix 5×5
        fillRect(px - 2, py - 2, 5, 5, COL_PLAYER);

        // Flèche de direction (longueur 8 px)
        float angle = p.angle * (float)(2 * Math.PI) / PlayerState.ANGLE_MAX;
        int dx = (int)(Math.sin(angle) * 8);
        int dz = (int)(Math.cos(angle) * 8);
        drawLine(px, py, px + dx, py - dz, COL_PLAYER_DIR);
    }

    // ── Projection ────────────────────────────────────────────────────────────

    private void computeProjection(LevelData level) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (Vec2W pt : level.points) {
            if (pt == null) continue;
            if (pt.xi() < minX) minX = pt.xi();
            if (pt.xi() > maxX) maxX = pt.xi();
            if (pt.zi() < minZ) minZ = pt.zi();
            if (pt.zi() > maxZ) maxZ = pt.zi();
        }

        // Fallback si pas de points
        if (minX == Integer.MAX_VALUE) { minX = -1000; maxX = 1000; minZ = -1000; maxZ = 1000; }

        int margin  = 10;
        int usableW = screenW - margin * 2;
        int usableH = screenH - margin * 2;

        float rangeX = maxX - minX;
        float rangeZ = maxZ - minZ;
        if (rangeX < 1) rangeX = 1;
        if (rangeZ < 1) rangeZ = 1;

        // Conserver l'aspect ratio
        float sx = usableW / rangeX;
        float sz = usableH / rangeZ;
        scaleX = scaleY = Math.min(sx, sz);

        // Centrer
        offsetX = margin + (usableW - rangeX * scaleX) / 2 - minX * scaleX;
        offsetY = margin + (usableH - rangeZ * scaleY) / 2 - minZ * scaleY;
    }

    public void resetProjection() { boundsComputed = false; }

    int worldToScreenX(int wx) { return (int)(wx * scaleX + offsetX); }
    int worldToScreenY(int wz) { return (int)(wz * scaleY + offsetY); }

    // ── Primitives de dessin ──────────────────────────────────────────────────

    /** Algorithme de Bresenham. */
    private void drawLine(int x0, int y0, int x1, int y1, int col) {
        int dx  = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy  = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            setPixel(x0, y0, col);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { if (x0 == x1) break; err += dy; x0 += sx; }
            if (e2 <= dx) { if (y0 == y1) break; err += dx; y0 += sy; }
        }
    }

    private void fillRect(int x, int y, int w, int h, int col) {
        for (int dy = 0; dy < h; dy++)
            for (int dx = 0; dx < w; dx++)
                setPixel(x + dx, y + dy, col);
    }

    private void drawGrid(int step) {
        for (int x = 0; x < screenW; x += step)
            for (int y = 0; y < screenH; y++)
                blendPixel(x, y, COL_GRID);
        for (int y = 0; y < screenH; y += step)
            for (int x = 0; x < screenW; x++)
                blendPixel(x, y, COL_GRID);
    }

    /** Texte bitmap minimal (3×5 pixels, 4 chars/row dans la police intégrée). */
    private void drawString(String text, int x, int y, int col) {
        int cx = x;
        for (char c : text.toCharArray()) {
            drawChar(c, cx, y, col);
            cx += 5;
        }
    }

    private static final int[] FONT_3x5 = {
        // Space, !, ", #, $, %, &, ', (, ), *, +, ,, -, ., /
        0,0,0,0,0,  0b010_010_010_000_010,0,0,0,0,  0,0,0,0,0,  0,0,0,0,0,
        // 0
        0b111_101_101_101_111,
        // 1
        0b010_110_010_010_111,
        // 2
        0b111_001_111_100_111,
        // 3
        0b111_001_111_001_111,
        // 4
        0b101_101_111_001_001,
        // 5
        0b111_100_111_001_111,
        // 6
        0b111_100_111_101_111,
        // 7
        0b111_001_001_001_001,
        // 8
        0b111_101_111_101_111,
        // 9
        0b111_101_111_001_111,
    };

    private void drawChar(char c, int x, int y, int col) {
        // Police très simplifiée — 3×5 pixels pour les chiffres et quelques symboles
        int idx = -1;
        if (c >= '0' && c <= '9') idx = 16 + (c - '0');
        if (idx < 0 || idx >= FONT_3x5.length) { fillRect(x, y, 3, 5, 0xFF333333); return; }
        int pattern = FONT_3x5[idx];
        for (int row = 0; row < 5; row++) {
            for (int col2 = 0; col2 < 3; col2++) {
                int bit = (pattern >> ((4 - row) * 3 + (2 - col2))) & 1;
                if (bit != 0) setPixel(x + col2, y + row, col);
            }
        }
    }

    // ── Pixels ────────────────────────────────────────────────────────────────

    private void setPixel(int x, int y, int col) {
        if (x >= 0 && x < screenW && y >= 0 && y < screenH)
            pixels[y * screenW + x] = col;
    }

    private void blendPixel(int x, int y, int col) {
        if (x >= 0 && x < screenW && y >= 0 && y < screenH) {
            // Blend additive simple : R+R, G+G, B+B, clampé
            int dst = pixels[y * screenW + x];
            int r = Math.min(255, ((dst >> 16) & 0xFF) + ((col >> 16) & 0xFF));
            int g = Math.min(255, ((dst >>  8) & 0xFF) + ((col >>  8) & 0xFF));
            int b = Math.min(255, ( dst        & 0xFF) + ( col        & 0xFF));
            pixels[y * screenW + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public int[] getPixels() { return pixels; }
    public int   getWidth()  { return screenW; }
    public int   getHeight() { return screenH; }
}
