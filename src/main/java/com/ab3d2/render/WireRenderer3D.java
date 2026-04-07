package com.ab3d2.render;

import com.ab3d2.core.level.*;

import java.util.Arrays;

/**
 * Renderer 3D filaire — première étape.
 *
 * <h2>Ce que ce renderer fait</h2>
 * <ul>
 *   <li>Parcourt les zones visibles via PVS depuis la zone du joueur</li>
 *   <li>Projette chaque ZEdge en perspective</li>
 *   <li>Dessine des colonnes de couleur unie (pas de texture)</li>
 *   <li>Portails = affichage en bleu semi-transparent, murs = couleur de zone</li>
 * </ul>
 *
 * <h2>Limitations (étape 1)</h2>
 * <ul>
 *   <li>Pas de clipping near-plane correct (saut si un point est derrière)</li>
 *   <li>Pas de tri des zones (overlapping possible)</li>
 *   <li>Pas de textures</li>
 *   <li>Rendu de GAUCHE à DROITE colonne par colonne</li>
 * </ul>
 */
public class WireRenderer3D {

    // ── Couleurs ──────────────────────────────────────────────────────────────
    private static final int COL_SKY     = 0xFF1A1A2E;  // bleu nuit (plafond)
    private static final int COL_FLOOR   = 0xFF16213E;  // bleu sombre (sol)
    private static final int COL_PORTAL  = 0xFF005588;  // bleu portail
    private static final int COL_PORTAL2 = 0xFF0088AA;  // portail zone adjacente
    private static final int COL_WALL_BASE = 0xFF444444; // mur de base

    // Palette de 16 couleurs pour distinguer les zones
    private static final int[] ZONE_COLORS = {
        0xFF8B0000, 0xFF006400, 0xFF00008B, 0xFF8B8B00,
        0xFF8B4500, 0xFF4B0082, 0xFF228B22, 0xFF8B0057,
        0xFF556B2F, 0xFF8B6914, 0xFF2F4F4F, 0xFF8B2500,
        0xFF3D005E, 0xFF00688B, 0xFF8B3A62, 0xFF5E4C0A
    };

    // ── Buffers ───────────────────────────────────────────────────────────────
    private final int   w;
    private final int   h;
    private final int[] pixels;
    /** Colonne la plus basse déjà dessinée (évite de redessiner le sol). */
    private final int[] floorY;   // par colonne : Y à partir duquel c'est sol
    /** Colonne la plus haute déjà dessinée (évite de redessiner le plafond). */
    private final int[] ceilY;    // par colonne : Y jusqu'auquel c'est plafond

    // ── Constructeur ─────────────────────────────────────────────────────────

    public WireRenderer3D(int w, int h) {
        this.w      = w;
        this.h      = h;
        this.pixels = new int[w * h];
        this.floorY = new int[w];
        this.ceilY  = new int[w];
    }

    // ── Rendu principal ───────────────────────────────────────────────────────

    /**
     * Dessine une frame complète.
     *
     * @param level   données du niveau
     * @param camera  caméra (position, angle, hauteur œil)
     * @param zoneId  zone courante du joueur
     */
    public void render(LevelData level, Camera camera, int zoneId) {
        // Fond plein
        Arrays.fill(pixels, COL_SKY);
        // Moitié basse = sol
        for (int y = h / 2; y < h; y++)
            Arrays.fill(pixels, y * w, y * w + w, COL_FLOOR);

        // Colonnes : floor et ceil pas encore remplis
        Arrays.fill(floorY, h);    // descend jusqu'à h
        Arrays.fill(ceilY, 0);     // monte jusqu'à 0

        // Récupérer la zone courante
        ZoneData startZone = level.zone(zoneId);
        if (startZone == null) {
            drawNoZone();
            return;
        }

        // 1. Dessiner la zone courante
        drawZone(level, camera, startZone, 0);

        // 2. Dessiner les zones PVS
        for (ZPVSRecord pvs : startZone.pvsRecords) {
            int vid = pvs.zoneId() & 0xFFFF;
            if (vid == zoneId) continue;
            ZoneData visZone = level.zone(vid);
            if (visZone == null) continue;
            drawZone(level, camera, visZone, 1);
        }
    }

    // ── Rendu d'une zone ─────────────────────────────────────────────────────

    private void drawZone(LevelData level, Camera camera, ZoneData zone, int depth) {
        int wallColor = ZONE_COLORS[Math.abs(zone.zoneId) % ZONE_COLORS.length];
        // Assombrir les zones PVS lointaines
        if (depth > 0) wallColor = darken(wallColor, depth * 40);

        float roofH  = zone.roofHeight();
        float floorH = zone.floorHeight();

        for (short edgeId : zone.edgeIds) {
            if (edgeId < 0 || edgeId >= level.numEdges()) continue;
            ZEdge edge = level.edge(edgeId);
            if (edge == null) continue;

            // Endpoints de l'arête
            float x1 = edge.pos().xi();
            float z1 = edge.pos().zi();
            float x2 = x1 + edge.len().xi();
            float z2 = z1 + edge.len().zi();

            // Couleur selon type
            int col = edge.isPortal() ? (depth == 0 ? COL_PORTAL : COL_PORTAL2) : wallColor;

            drawWall(camera, x1, z1, x2, z2, roofH, floorH, col, edge.isPortal());
        }
    }

    // ── Rendu d'un mur entre deux points ─────────────────────────────────────

    private void drawWall(Camera camera,
                          float wx1, float wz1, float wx2, float wz2,
                          float roofH, float floorH,
                          int color, boolean isPortal) {

        // Transformer en espace caméra
        float cx1 = camera.camX(wx1, wz1);
        float cz1 = camera.camZ(wx1, wz1);
        float cx2 = camera.camX(wx2, wz2);
        float cz2 = camera.camZ(wx2, wz2);

        // Clipping simple : rejeter si les deux points sont derrière
        if (cz1 <= Camera.NEAR_Z && cz2 <= Camera.NEAR_Z) return;

        // Clipping d'un point derrière la caméra → interpolation sur near plane
        if (cz1 <= Camera.NEAR_Z) {
            float[] clipped = clipNearPlane(cx1, cz1, cx2, cz2);
            cx1 = clipped[0]; cz1 = clipped[1];
        } else if (cz2 <= Camera.NEAR_Z) {
            float[] clipped = clipNearPlane(cx2, cz2, cx1, cz1);
            cx2 = clipped[0]; cz2 = clipped[1];
        }

        // Projeter les extrémités
        float sx1 = Camera.projectX(cx1, cz1);
        float sx2 = Camera.projectX(cx2, cz2);

        // Rejeter si complètement hors écran
        if (sx1 >= w && sx2 >= w) return;
        if (sx1 < 0  && sx2 < 0)  return;

        // Assurer sx1 < sx2
        if (sx1 > sx2) {
            float tmp; tmp=sx1; sx1=sx2; sx2=tmp;
            tmp=cx1; cx1=cx2; cx2=tmp;
            tmp=cz1; cz1=cz2; cz2=tmp;
        }

        // Clamp à l'écran
        int xStart = Math.max(0, (int) sx1);
        int xEnd   = Math.min(w - 1, (int) sx2);
        if (xStart > xEnd) return;

        // Pour chaque colonne entre xStart et xEnd
        float totalW = sx2 - sx1;
        if (totalW < 0.001f) totalW = 0.001f;

        for (int col = xStart; col <= xEnd; col++) {
            // Interpolation de la profondeur (perspective)
            float t = (col - sx1) / totalW;
            // Interpolation perspective-correcte de 1/z
            float invZ1 = 1.0f / cz1;
            float invZ2 = 1.0f / cz2;
            float invZ  = invZ1 + t * (invZ2 - invZ1);
            float cz    = 1.0f / invZ;

            // Hauteurs écran à cette colonne
            float yTop = Camera.projectY(roofH,  camera.eyeH, cz);
            float yBot = Camera.projectY(floorH, camera.eyeH, cz);

            int yTopI = Camera.clampY(yTop);
            int yBotI = Camera.clampY(yBot);

            if (yTopI >= yBotI) continue;

            // Dessiner la colonne
            for (int y = yTopI; y < yBotI; y++) {
                int idx = y * w + col;
                if (!isPortal) {
                    pixels[idx] = shadeByDepth(color, cz);
                } else {
                    // Portail : colorer légèrement mais laisser voir à travers
                    pixels[idx] = blendPortal(pixels[idx], color, cz);
                }
            }

            // Trait de bordure (haut et bas du mur)
            if (!isPortal) {
                setPixelSafe(col, yTopI, brighten(color, 60));
                setPixelSafe(col, Math.min(yBotI, h - 1), darken(color, 40));
            }
        }
    }

    // ── Clipping near-plane ───────────────────────────────────────────────────

    /**
     * Retourne le point clipé sur le near plane pour le segment (p1_behind, p2_front).
     * p1 est derrière la caméra (cz1 <= NEAR_Z), p2 est devant.
     */
    private static float[] clipNearPlane(float cx1, float cz1, float cx2, float cz2) {
        float t = (Camera.NEAR_Z - cz1) / (cz2 - cz1);
        float newCx = cx1 + t * (cx2 - cx1);
        return new float[]{ newCx, Camera.NEAR_Z };
    }

    // ── Utilitaires de couleur ────────────────────────────────────────────────

    /** Assombrit une couleur selon la profondeur (fog simple). */
    private static int shadeByDepth(int color, float depth) {
        // Plus loin = plus sombre
        float fogFactor = Math.min(1.0f, Math.max(0.0f, 1.0f - depth / 400.0f));
        int r = (int) (((color >> 16) & 0xFF) * fogFactor);
        int g = (int) (((color >>  8) & 0xFF) * fogFactor);
        int b = (int) (( color        & 0xFF) * fogFactor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color, int amount) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - amount);
        int g = Math.max(0, ((color >>  8) & 0xFF) - amount);
        int b = Math.max(0, ( color        & 0xFF) - amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >>  8) & 0xFF) + amount);
        int b = Math.min(255, ( color        & 0xFF) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int blendPortal(int dst, int portalColor, float depth) {
        // 80% original, 20% portail
        int r = (((dst >> 16) & 0xFF) * 4 + ((portalColor >> 16) & 0xFF)) / 5;
        int g = (((dst >>  8) & 0xFF) * 4 + ((portalColor >>  8) & 0xFF)) / 5;
        int b = (( dst        & 0xFF) * 4 + ( portalColor        & 0xFF)) / 5;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void setPixelSafe(int x, int y, int color) {
        if (x >= 0 && x < w && y >= 0 && y < h)
            pixels[y * w + x] = color;
    }

    private void drawNoZone() {
        for (int i = 0; i < pixels.length; i++) pixels[i] = 0xFF330000;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public int[] getPixels() { return pixels; }
    public int   getWidth()  { return w; }
    public int   getHeight() { return h; }
}
