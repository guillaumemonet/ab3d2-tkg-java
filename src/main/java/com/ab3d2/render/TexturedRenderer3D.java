package com.ab3d2.render;

import com.ab3d2.assets.WadTextureData;
import com.ab3d2.assets.WallTextureManager;
import com.ab3d2.core.level.*;

import java.util.Arrays;

/**
 * Renderer 3D software avec textures de murs (colonne par colonne).
 *
 * <h2>Algorithme de rendu par colonne</h2>
 * Pour chaque mur visible (depuis la zone courante + PVS) :
 * <ol>
 *   <li>Projeter les deux extremites en espace ecran (sx1, sx2)</li>
 *   <li>Pour chaque colonne x entre sx1 et sx2 :</li>
 *   <li>  Calculer la profondeur perspective-correcte cam_z(x)</li>
 *   <li>  Calculer la coordonnee texture U (horizontal)</li>
 *   <li>  Projeter les hauteurs top/bottom en screen y</li>
 *   <li>  Pour chaque pixel y de screenTop a screenBot :</li>
 *   <li>    Calculer V (vertical), echantillonner la texture, ecrire le pixel</li>
 * </ol>
 *
 * <h2>Echelle des textures (depuis le README)</h2>
 * <pre>
 * Horizontale : 256 unites monde = 1 largeur texture complete
 * Verticale   : 128 unites monde = 1 hauteur texture complete
 * </pre>
 *
 * <h2>Coordonnees ecran</h2>
 * <pre>
 * Largeur : 192  Centre X = 96
 * Hauteur : 160  Centre Y = 80
 * FOCAL   =  96  (FOV horizontal 90°)
 * </pre>
 */
public class TexturedRenderer3D {

    // ── Constantes echelle texture (README) ───────────────────────────────────
    /**
     * 256 unites monde pour une repetition complete horizontale.
     * texU = (worldOffset / TEX_SCALE_H) & widthMask
     */
    private static final float TEX_SCALE_H = 256.0f;

    /**
     * 128 unites monde pour une repetition complete verticale.
     * texV = (heightOffset / TEX_SCALE_V) & heightMask
     */
    private static final float TEX_SCALE_V = 128.0f;

    // ── Taille ecran ──────────────────────────────────────────────────────────
    private final int   W;
    private final int   H;
    private final int[] pixels;
    private final float[] depthBuf; // z-buffer de profondeur (cam_z par colonne)

    // ── Dependances ───────────────────────────────────────────────────────────
    private final WallTextureManager texMgr;

    // ── Constructeur ──────────────────────────────────────────────────────────

    public TexturedRenderer3D(int w, int h, WallTextureManager texMgr) {
        this.W      = w;
        this.H      = h;
        this.pixels  = new int[w * h];
        this.depthBuf = new float[w];
        this.texMgr  = texMgr;
    }

    // ── Rendu principal ───────────────────────────────────────────────────────

    /**
     * Dessine une frame complete.
     *
     * @param level          donnees du niveau
     * @param zoneEntries    WallRenderEntry[][] parse de ZoneGraphAdds (par zone_id)
     * @param camera         camera (position, angle, eyeH)
     * @param zoneId         zone courante du joueur
     */
    public void render(LevelData level, WallRenderEntry[][] zoneEntries,
                       Camera camera, int zoneId) {
        // Fond
        Arrays.fill(pixels, 0xFF1A1A2E);             // plafond
        for (int y = H / 2; y < H; y++)
            Arrays.fill(pixels, y * W, y * W + W, 0xFF0D0D1F);  // sol

        // Z-buffer : infinity
        Arrays.fill(depthBuf, Float.MAX_VALUE);

        // Dessiner la zone courante d'abord
        drawZone(level, zoneEntries, camera, zoneId);

        // Puis les zones PVS
        ZoneData curZone = level.zone(zoneId);
        if (curZone != null) {
            for (ZPVSRecord pvs : curZone.pvsRecords) {
                int vid = pvs.zoneId() & 0xFFFF;
                if (vid != zoneId) drawZone(level, zoneEntries, camera, vid);
            }
        }
    }

    // ── Rendu d'une zone ─────────────────────────────────────────────────────

    private void drawZone(LevelData level, WallRenderEntry[][] zoneEntries,
                          Camera camera, int zoneId) {
        if (zoneId < 0 || zoneId >= zoneEntries.length) return;
        WallRenderEntry[] entries = zoneEntries[zoneId];
        if (entries == null || entries.length == 0) return;

        for (WallRenderEntry entry : entries) {
            if (!entry.isWall()) continue;

            // Recuperer les points 2D des extremites du mur
            int leftIdx  = entry.leftPt  & 0xFFFF;
            int rightIdx = entry.rightPt & 0xFFFF;
            if (leftIdx  >= level.numPoints()) continue;
            if (rightIdx >= level.numPoints()) continue;

            Vec2W leftPt  = level.point(leftIdx);
            Vec2W rightPt = level.point(rightIdx);
            if (leftPt == null || rightPt == null) continue;

            // Hauteurs du mur (raw → height units)
            float topH = (float) entry.topWall   / 256.0f;
            float botH = (float) entry.botWall   / 256.0f;
            if (topH >= botH) continue;  // mur sans hauteur

            // Texture
            WadTextureData tex = texMgr.get(entry.texIndex);

            drawWall(camera, leftPt, rightPt, topH, botH, entry, tex);
        }
    }

    // ── Rendu d'un mur texturé entre deux points ──────────────────────────────

    private void drawWall(Camera camera, Vec2W left, Vec2W right,
                          float topH, float botH,
                          WallRenderEntry entry, WadTextureData tex) {

        float wx1 = left.xi(),  wz1 = left.zi();
        float wx2 = right.xi(), wz2 = right.zi();

        // Transform camera space
        float cx1 = camera.camX(wx1, wz1), cz1 = camera.camZ(wx1, wz1);
        float cx2 = camera.camX(wx2, wz2), cz2 = camera.camZ(wx2, wz2);

        // Rejeter si les deux points sont derriere
        if (cz1 <= Camera.NEAR_Z && cz2 <= Camera.NEAR_Z) return;

        // Near-plane clipping
        if (cz1 <= Camera.NEAR_Z) { float[] c = clipNear(cx1, cz1, cx2, cz2); cx1=c[0]; cz1=c[1]; }
        else if (cz2 <= Camera.NEAR_Z) { float[] c = clipNear(cx2, cz2, cx1, cz1); cx2=c[0]; cz2=c[1]; }

        // Projeter les X ecran
        float sx1 = Camera.projectX(cx1, cz1);
        float sx2 = Camera.projectX(cx2, cz2);
        if (sx1 > sx2) {
            // Swap pour toujours aller gauche → droite
            float t; t=sx1; sx1=sx2; sx2=t; t=cx1; cx1=cx2; cx2=t; t=cz1; cz1=cz2; cz2=t;
            t=wx1; wx1=wx2; wx2=t; t=wz1; wz1=wz2; wz2=t;
        }

        // Rejeter si completement hors ecran
        if (sx2 < 0 || sx1 >= W) return;

        int xStart = Math.max(0, (int) sx1);
        int xEnd   = Math.min(W - 1, (int) sx2);
        if (xStart > xEnd) return;

        // Longueur du mur en unites monde (pour le mapping U)
        float wallLenX = wx2 - wx1;
        float wallLenZ = wz2 - wz1;
        float wallLenWorld = (float) Math.sqrt(wallLenX * wallLenX + wallLenZ * wallLenZ);

        // Texture
        int texW = tex.width();
        int texH = tex.height();
        int wMask = texW - 1;
        int hMask = texH - 1;

        // Offset texture depuis l'entree (from_tile = decalage horizontal)
        float texOffX = (entry.fromTile & 0xFFFF) / 16.0f;  // fromTile est << 4 dans l'ASM
        float texOffY = entry.yOffset & 0xFFFF;

        // Facteur de repetition horizontal : 1 repetition = TEX_SCALE_H monde
        // texU avance de (wallLenWorld / TEX_SCALE_H * texW) sur toute la largeur
        float totalU = wallLenWorld / TEX_SCALE_H * texW;

        // Interpolation perspective-correcte : on travaille avec 1/z
        float invZ1 = 1.0f / cz1;
        float invZ2 = 1.0f / cz2;
        float totalSX = sx2 - sx1;
        if (Math.abs(totalSX) < 0.5f) totalSX = 0.5f;

        for (int col = xStart; col <= xEnd; col++) {
            float t = (col - sx1) / totalSX;  // 0..1 le long du mur

            // Profondeur perspective-correcte
            float invZ = invZ1 + t * (invZ2 - invZ1);
            float cz   = 1.0f / invZ;

            // Z-buffer : ne dessiner que si plus proche
            if (cz >= depthBuf[col]) continue;
            depthBuf[col] = cz;

            // Coordonnee texture U (perspective-correct)
            float u = (invZ1 / invZ) * 0 + (1 - invZ1 / invZ) * totalU; // t*totalU perspective
            // Version perspective-correcte : lerp de 0 a totalU en espace perspective
            float uPersp = ((t * invZ2 + (1 - t) * invZ1) == 0) ? 0
                : (t * invZ2 / invZ) * totalU;
            // Simpler: u = t * totalU (approximation lineaire ok pour petits murs)
            u = t * totalU + texOffX;
            int texU = ((int) u) % Math.max(1, texW);

            // Projeter les hauteurs du mur a cette profondeur
            float screenTop = Camera.projectY(topH, camera.eyeH, cz);
            float screenBot = Camera.projectY(botH, camera.eyeH, cz);

            int yTop = Camera.clampY(screenTop);
            int yBot = Camera.clampY(screenBot);
            if (yTop >= yBot) continue;

            // Hauteur en pixels du mur a cette profondeur
            float wallPixelH = screenBot - screenTop;
            if (wallPixelH < 0.5f) continue;

            // Echelle V : TEX_SCALE_V unites monde = 1 texture hauteur
            // wallHeightWorld = (botH - topH) unites monde
            float wallHeightWorld = botH - topH;
            float texVscale = texH / (wallPixelH);  // texels par pixel

            // Dessiner la colonne
            int[] tpixels = tex.pixels();
            int safeTexW = Math.max(1, texW);
            int safeTexH = Math.max(1, texH);
            for (int row = yTop; row < yBot; row++) {
                // V en fonction de la position dans la colonne
                float dy   = row - screenTop;
                float v    = dy * texVscale + texOffY;
                // Utiliser % pour eviter ArrayIndexOutOfBounds
                // (les dimensions detectees peuvent differer de wMask/hMask)
                int texV   = ((int) v % safeTexH + safeTexH) % safeTexH;
                int texU2  = texU % safeTexW;
                int texPx  = tpixels[texV * safeTexW + texU2];

                // Fog de profondeur simple
                pixels[row * W + col] = fogColor(texPx, cz);
            }
        }
    }

    // ── Near-plane clipping ───────────────────────────────────────────────────

    private static float[] clipNear(float cx1, float cz1, float cx2, float cz2) {
        float t = (Camera.NEAR_Z - cz1) / (cz2 - cz1);
        return new float[]{ cx1 + t * (cx2 - cx1), Camera.NEAR_Z };
    }

    // ── Fog de distance ───────────────────────────────────────────────────────

    private static int fogColor(int color, float depth) {
        float fog = Math.max(0f, Math.min(1f, 1f - depth / 500f));
        int r = (int)(((color >> 16) & 0xFF) * fog);
        int g = (int)(((color >>  8) & 0xFF) * fog);
        int b = (int)(( color        & 0xFF) * fog);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public int[] getPixels() { return pixels; }
    public int   getWidth()  { return W; }
    public int   getHeight() { return H; }
}
