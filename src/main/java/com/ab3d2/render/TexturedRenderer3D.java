package com.ab3d2.render;

import com.ab3d2.assets.FloorTextureLoader;
import com.ab3d2.assets.WadTextureData;
import com.ab3d2.assets.WallTextureManager;
import com.ab3d2.core.level.*;

import java.util.Arrays;

/**
 * Renderer 3D software : murs textures + sol/plafond perspective-correct.
 *
 * <h2>Floor casting</h2>
 * Pour chaque ligne y, on calcule la distance au plan horizontal :
 *   dist = (planeH - eyeH) * FOCAL / (y - centreY)
 * Puis pour chaque pixel x de cette ligne, la position monde :
 *   wx = cam.x + dist*sin + (x-centreX) * dist*cos/FOCAL
 *   wz = cam.z + dist*cos - (x-centreX) * dist*sin/FOCAL
 * et on sample la texture de sol (tile 64x64) avec texU=wx%64, texV=wz%64.
 */
public class TexturedRenderer3D {

    private static final float TEX_SCALE_H = 256.0f;

    // Hauteurs monde par tile (GLFT WallHeights confirme)
    private static final float[] GLFT_TEX_HEIGHTS = {
         64,  // 0  stonewall
        128,  // 1  brownpipes
        128,  // 2  hullmetal
        128,  // 3  technotritile
        128,  // 4  brownspeakers
        128,  // 5  chevrondoor
        128,  // 6  technolights
        128,  // 7  redhullmetal
        128,  // 8  alienredwall
        128,  // 9  gieger
        128,  // 10 rocky
        128,  // 11 steampunk
         32,  // 12 brownstonestep
    };

    private final int   W, H;
    private final int[] pixels;
    private final float[] depthBuf;   // Z-buffer 2D par pixel

    private final WallTextureManager texMgr;
    private final FloorTextureLoader floorLoader;

    public TexturedRenderer3D(int w, int h, WallTextureManager texMgr,
                               FloorTextureLoader floorLoader) {
        this.W           = w;
        this.H           = h;
        this.pixels      = new int[w * h];
        this.depthBuf    = new float[w * h];
        this.texMgr      = texMgr;
        this.floorLoader = floorLoader;
    }

    // ── Rendu principal ───────────────────────────────────────────────────────

    public void render(LevelData level, WallRenderEntry[][] zoneEntries,
                       Camera camera, int zoneId, int[] floorWhichTiles, int[] ceilWhichTiles) {

        Arrays.fill(depthBuf, Float.MAX_VALUE);
        Arrays.fill(pixels, 0xFF000000);

        ZoneData curZone = level.zone(zoneId);
        float floorH  = curZone != null ? curZone.floorHeight()  : 0;
        float ceilH   = curZone != null ? curZone.roofHeight()   : floorH - 128;

        int floorTile = (floorWhichTiles != null && zoneId < floorWhichTiles.length
                         && floorWhichTiles[zoneId] >= 0)
                        ? floorWhichTiles[zoneId]
                        : (curZone != null ? (curZone.floorNoise & 0xFF) : 0);
        int ceilTile  = (ceilWhichTiles != null && zoneId < ceilWhichTiles.length
                         && ceilWhichTiles[zoneId] >= 0)
                        ? ceilWhichTiles[zoneId]
                        : floorTile;

        // Rendre d'abord les sols/plafonds de TOUTES les zones visibles
        // du plus loin au plus proche (le depthBuf gere l'occlusion correcte)
        // Zones PVS en premier (generalement plus loin)
        if (curZone != null) {
            for (ZPVSRecord pvs : curZone.pvsRecords) {
                int vid = pvs.zoneId() & 0xFFFF;
                if (vid == zoneId || vid >= level.numZones()) continue;
                ZoneData pvsZone = level.zone(vid);
                if (pvsZone == null) continue;
                float pFloor = pvsZone.floorHeight();
                float pCeil  = pvsZone.roofHeight();
                int pFT = (floorWhichTiles != null && vid < floorWhichTiles.length
                           && floorWhichTiles[vid] >= 0)
                          ? floorWhichTiles[vid] : floorTile;
                int pCT = (ceilWhichTiles != null && vid < ceilWhichTiles.length
                           && ceilWhichTiles[vid] >= 0)
                          ? ceilWhichTiles[vid] : ceilTile;
                renderFloorCeiling(camera, pFloor, pCeil, pFT, pCT);
            }
        }
        // Zone courante en dernier (plus proche = ecrase les zones lointaines)
        renderFloorCeiling(camera, floorH, ceilH, floorTile, ceilTile);

        drawZone(level, zoneEntries, camera, zoneId);
        if (curZone != null) {
            for (ZPVSRecord pvs : curZone.pvsRecords) {
                int vid = pvs.zoneId() & 0xFFFF;
                if (vid != zoneId && vid < level.numZones())
                    drawZone(level, zoneEntries, camera, vid);
            }
        }
    }

    // ── Sol et plafond (floor casting perspective-correct) ────────────────────

    private void renderFloorCeiling(Camera cam, float floorH, float ceilH,
                                      int floorWhichTile, int ceilWhichTile) {
        final float sinV  = cam.getSin();
        final float cosV  = cam.getCos();
        final float focal = Camera.FOCAL;
        final float centX = Camera.CENTRE_X;
        final float centY = Camera.CENTRE_Y;

        int[] floorTile = floorLoader.getTile(floorWhichTile);
        int[] ceilTile  = floorLoader.getTile(ceilWhichTile);
        final int TW = FloorTextureLoader.TILE_W;
        final int TH = FloorTextureLoader.TILE_H;

        for (int y = 0; y < H; y++) {
            float dY = y - centY;
            if (dY == 0f) continue;
            boolean isFloor = dY > 0;
            float   planeH  = isFloor ? floorH : ceilH;
            float   dist    = (planeH - cam.eyeH) * focal / dY;
            if (dist <= 0f) continue;

            float stepX =  dist * cosV / focal;
            float stepZ = -dist * sinV / focal;
            float wx = cam.x + dist * sinV + (0 - centX) * stepX;
            float wz = cam.z + dist * cosV + (0 - centX) * stepZ;

            int[] tile = isFloor ? floorTile : ceilTile;
            float fog  = Math.max(0f, Math.min(1f, 1f - (dist - 50f) / 1200f));
            if (!isFloor) fog *= 0.75f;

            int rowOff = y * W;
            for (int x = 0; x < W; x++) {
                int idx = rowOff + x;
                // Z-buffer : n'ecrire que si ce pixel est vide ou plus loin
                if (dist < depthBuf[idx]) {
                    depthBuf[idx] = dist;
                    int texU = Math.floorMod((int) wx, TW);
                    int texV = Math.floorMod((int) wz, TH);
                    int raw  = tile[texV * TW + texU];
                    int r = (int)(((raw >> 16) & 0xFF) * fog);
                    int g = (int)(((raw >>  8) & 0xFF) * fog);
                    int b = (int)(( raw        & 0xFF) * fog);
                    pixels[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
                wx += stepX;
                wz += stepZ;
            }
        }
    }

    // ── Rendu d'une zone ─────────────────────────────────────────────────────

    private void drawZone(LevelData level, WallRenderEntry[][] zoneEntries,
                          Camera camera, int zoneId) {
        if (zoneId < 0 || zoneId >= zoneEntries.length) return;
        WallRenderEntry[] entries = zoneEntries[zoneId];
        if (entries == null) return;

        for (WallRenderEntry entry : entries) {
            if (!entry.isWall()) continue;
            if ((entry.texIndex & 0x8000) != 0) continue;
            if (entry.texIndex >= WallTextureManager.NUM_WALL_TEXTURES) continue;

            int li = entry.leftPt  & 0xFFFF;
            int ri = entry.rightPt & 0xFFFF;
            if (li >= level.numPoints() || ri >= level.numPoints()) continue;

            Vec2W lp = level.point(li), rp = level.point(ri);
            if (lp == null || rp == null) continue;

            float topH = entry.topWall / 256.0f;
            float botH = entry.botWall / 256.0f;
            if (topH >= botH) continue;
            float wh = botH - topH;
            if (wh > 2048f || Math.abs(topH) > 8192f) continue;

            WadTextureData tex = texMgr.get(entry.texIndex);
            if (tex == null) continue;

            drawWall(camera, lp, rp, topH, botH, entry, tex);
        }
    }

    // ── Rendu d'un mur texturé ────────────────────────────────────────────────

    private void drawWall(Camera camera, Vec2W left, Vec2W right,
                          float topH, float botH,
                          WallRenderEntry entry, WadTextureData tex) {

        float wx1 = left.xi(), wz1 = left.zi();
        float wx2 = right.xi(), wz2 = right.zi();

        float cx1 = camera.camX(wx1,wz1), cz1 = camera.camZ(wx1,wz1);
        float cx2 = camera.camX(wx2,wz2), cz2 = camera.camZ(wx2,wz2);
        if (cz1 <= Camera.NEAR_Z && cz2 <= Camera.NEAR_Z) return;

        float dx = wx2-wx1, dz = wz2-wz1;
        float wallLen = (float)Math.sqrt(dx*dx + dz*dz);
        int texW = Math.max(1, tex.width());
        int texH = Math.max(1, tex.height());

        float texOffX = (entry.fromTile & 0xFFFF) < 32768
            ? (entry.fromTile & 0xFFFF) / 16.0f : 0.0f;
        float texOffY = (entry.yOffset & 0xFFFF) < 32768
            ? (entry.yOffset & 0xFFFF) / 256.0f : 0.0f;

        float u1 = texOffX, u2 = texOffX + wallLen / TEX_SCALE_H * texW;

        // Near-plane
        if (cz1 <= Camera.NEAR_Z) {
            float a=(Camera.NEAR_Z-cz1)/(cz2-cz1);
            cx1+=a*(cx2-cx1); cz1=Camera.NEAR_Z; u1+=a*(u2-u1);
        } else if (cz2 <= Camera.NEAR_Z) {
            float a=(Camera.NEAR_Z-cz2)/(cz1-cz2);
            cx2+=a*(cx1-cx2); cz2=Camera.NEAR_Z; u2+=a*(u1-u2);
        }

        // Frustum L/R
        float sL1=cx1+cz1, sL2=cx2+cz2;
        float sR1=cz1-cx1, sR2=cz2-cx2;
        if (sL1<0&&sL2<0) return;
        if (sR1<0&&sR2<0) return;
        if (sL1<0){float a=sL1/(sL1-sL2);cx1+=a*(cx2-cx1);cz1+=a*(cz2-cz1);u1+=a*(u2-u1);}
        else if(sL2<0){float a=sL2/(sL2-sL1);cx2+=a*(cx1-cx2);cz2+=a*(cz1-cz2);u2+=a*(u1-u2);}
        sR1=cz1-cx1; sR2=cz2-cx2;
        if (sR1<0){float a=sR1/(sR1-sR2);cx1+=a*(cx2-cx1);cz1+=a*(cz2-cz1);u1+=a*(u2-u1);}
        else if(sR2<0){float a=sR2/(sR2-sR1);cx2+=a*(cx1-cx2);cz2+=a*(cz1-cz2);u2+=a*(u1-u2);}
        if (cz1<=0||cz2<=0) return;

        float sx1 = Camera.projectX(cx1,cz1);
        float sx2 = Camera.projectX(cx2,cz2);
        if (sx1>=sx2||sx2<0||sx1>=W) return;

        int xStart=Math.max(0,(int)sx1), xEnd=Math.min(W-1,(int)sx2);
        if (xStart>xEnd) return;

        float invZ1=1f/cz1, invZ2=1f/cz2;
        float uoz1=u1*invZ1, uoz2=u2*invZ2;
        float span=Math.max(0.5f,sx2-sx1);

        float worldScale=(entry.texIndex>=0&&entry.texIndex<GLFT_TEX_HEIGHTS.length)
            ?GLFT_TEX_HEIGHTS[entry.texIndex]:128f;
        float wallWorldH=botH-topH;
        int[] tp=tex.pixels();

        for (int col=xStart;col<=xEnd;col++) {
            float t=    (col-sx1)/span;
            float invZ= invZ1+t*(invZ2-invZ1);
            if (invZ<=0) continue;
            float cz=1f/invZ;

            float u=(uoz1+t*(uoz2-uoz1))*cz;
            int texU=Math.floorMod((int)u,texW);

            float sTop=Camera.projectY(topH,camera.eyeH,cz);
            float sBot=Camera.projectY(botH,camera.eyeH,cz);
            int yTop=Camera.clampY(sTop), yBot=Camera.clampY(sBot);
            if (yTop>=yBot) continue;

            float pixH=sBot-sTop;
            if (pixH<0.5f) continue;
            float vScale=wallWorldH*texH/(pixH*worldScale);

            for (int row=yTop;row<yBot;row++) {
                int idx=row*W+col;
                if (cz>=depthBuf[idx]) continue;
                depthBuf[idx]=cz;
                float v=(row-sTop)*vScale+texOffY;
                int texV=Math.floorMod((int)v,texH);
                pixels[idx]=fogColor(tp[texV*texW+texU],cz);
            }
        }
    }

    private static int fogColor(int color, float depth) {
        float fog=Math.max(0f,Math.min(1f,1f-(depth-50f)/1200f));
        int r=(int)(((color>>16)&0xFF)*fog);
        int g=(int)(((color>> 8)&0xFF)*fog);
        int b=(int)(( color    &0xFF)*fog);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }

    public int[]  getPixels() { return pixels; }
    public int    getWidth()  { return W; }
    public int    getHeight() { return H; }
}
