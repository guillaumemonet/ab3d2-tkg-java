package com.ab3d2;

import com.ab3d2.assets.*;
import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * Exporte les niveaux AB3D2 au format Wavefront OBJ avec textures.
 *
 * Lancement :
 *   gradle run --main-class=com.ab3d2.LevelObjExporter
 *
 * Sortie : build/obj/
 *   level_A.obj, level_A.mtl
 *   textures/wall_0_stonewall.png  ... wall_12_brownstonestep.png
 *   textures/floor.png  (placeholder)
 *
 * Les murs utilisent les WallRenderEntry (topWall/botWall + texIndex + UV).
 * Les zones geometriques ZEdge sont exportees en fallback si pas de WallRenderEntry.
 */
public class LevelObjExporter {

    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String OUTPUT_DIR =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/obj";

    // Mapping texIndex -> nom fichier (depuis GLFT test.lnk)
    static final String[] TEX_NAMES = {
        "stonewall", "brownpipes", "hullmetal", "technotritile",
        "brownspeakers", "chevrondoor", "technolights", "redhullmetal",
        "alienredwall", "gieger", "rocky", "steampunk", "brownstonestep",
    };

    // Echelle texture (units monde par repetition)
    static final float TEX_SCALE_H = 256.0f;
    static final float TEX_SCALE_V = 128.0f;

    public static void main(String[] args) throws Exception {
        System.out.println("=== AB3D2 Level OBJ Exporter (avec textures) ===\n");

        Path root   = Path.of(RESOURCES);
        Path outDir = Path.of(OUTPUT_DIR);
        Path texDir = outDir.resolve("textures");
        Files.createDirectories(texDir);
        Tables.initFromBytes(new byte[0]);

        // ── Exporter les textures en PNG ──────────────────────────────────────
        int[] palette = WadToPngExporter.loadPalette(root);
        WallTextureExtractor extractor = new WallTextureExtractor(palette);
        Map<Integer, WadTextureData> textures = new LinkedHashMap<>();

        System.out.println("Export des textures PNG...");
        for (int i = 0; i < TEX_NAMES.length; i++) {
            Path wadPath = root.resolve("walls/" + TEX_NAMES[i] + ".256wad");
            if (!Files.exists(wadPath)) continue;
            try {
                WadTextureData tex = extractor.load(wadPath);
                textures.put(i, tex);
                Path pngPath = texDir.resolve("wall_" + i + "_" + TEX_NAMES[i] + ".png");
                saveTexturePng(tex, pngPath);
                System.out.printf("  [%2d] %s -> %dx%d%n", i, TEX_NAMES[i], tex.width(), tex.height());
            } catch (Exception e) {
                System.out.printf("  [%2d] %s ERR: %s%n", i, TEX_NAMES[i], e.getMessage());
            }
        }

        // Texture sol/plafond placeholder (gris)
        saveGrayPng(texDir.resolve("floor.png"), 64, 64, 0xFF404040);
        saveGrayPng(texDir.resolve("ceil.png"),  64, 64, 0xFF303030);
        System.out.println();

        // ── Exporter les niveaux ──────────────────────────────────────────────
        int exported = 0, skipped = 0;
        for (char letter = 'A'; letter <= 'P'; letter++) {
            Path binPath   = root.resolve("levels/LEVEL_" + letter + "/twolev.bin");
            Path graphPath = root.resolve("levels/LEVEL_" + letter + "/twolev.graph.bin");
            if (!Files.exists(binPath) || !Files.exists(graphPath)) { skipped++; continue; }
            try {
                LevelData level = new GraphicsBinaryParser().load(binPath, graphPath,
                    String.valueOf(letter));

                // Parser ZoneGraphAdds
                byte[] graphRaw = Files.readAllBytes(graphPath);
                ByteBuffer gBuf = ByteBuffer.wrap(graphRaw).order(ByteOrder.BIG_ENDIAN);
                gBuf.getInt(); gBuf.getInt(); gBuf.getInt();
                int zgaOffset = gBuf.getInt();
                WallRenderEntry[][] zoneEntries =
                    new ZoneGraphParser().parse(graphRaw, level.numZones(), zgaOffset);

                Path objPath = outDir.resolve("level_" + letter + ".obj");
                Path mtlPath = outDir.resolve("level_" + letter + ".mtl");
                exportLevel(level, zoneEntries, textures, objPath, mtlPath);
                System.out.printf("  OK  LEVEL_%c%n", letter);
                exported++;
            } catch (Exception e) {
                System.out.printf("  ERR LEVEL_%c : %s%n", letter, e.getMessage());
                e.printStackTrace();
                skipped++;
            }
        }
        System.out.printf("%nExporte:%d  Saute:%d  Dossier: %s%n",
            exported, skipped, outDir.toAbsolutePath());
    }

    // =========================================================================
    // Export principal
    // =========================================================================

    static void exportLevel(LevelData level, WallRenderEntry[][] zoneEntries,
                            Map<Integer, WadTextureData> textures,
                            Path objPath, Path mtlPath) throws IOException {

        writeMtl(mtlPath, textures);

        try (PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(objPath.toFile())))) {
            w.println("# AB3D2 Level " + level.levelId + " - OBJ avec textures");
            w.println("mtllib " + mtlPath.getFileName());
            w.println();

            // Compteur de vertex global
            // OBJ indexe a partir de 1
            int[] vi = {1};  // vertex positions
            int[] ti = {1};  // vertex texcoords

            // ── Murs textures (WallRenderEntry) ───────────────────────────────
            w.println("# === MURS TEXTURES ===");
            for (int zi = 0; zi < level.numZones(); zi++) {
                WallRenderEntry[] entries = zoneEntries[zi];
                if (entries == null || entries.length == 0) continue;

                boolean hasWalls = false;
                for (WallRenderEntry e : entries) {
                    if (!e.isWall()) continue;
                    if ((e.texIndex & 0x8000) != 0) continue;
                    if (e.leftPt >= level.numPoints() || e.rightPt >= level.numPoints()) continue;
                    hasWalls = true; break;
                }
                if (!hasWalls) continue;

                w.println();
                w.println("g z" + zi + "_walls");

                for (WallRenderEntry e : entries) {
                    if (!e.isWall()) continue;
                    if ((e.texIndex & 0x8000) != 0) continue;  // portail

                    int leftIdx  = e.leftPt  & 0xFFFF;
                    int rightIdx = e.rightPt & 0xFFFF;
                    if (leftIdx >= level.numPoints() || rightIdx >= level.numPoints()) continue;

                    Vec2W lp = level.point(leftIdx);
                    Vec2W rp = level.point(rightIdx);
                    if (lp == null || rp == null) continue;

                    float topH = e.topWall  / 256.0f;
                    float botH = e.botWall  / 256.0f;
                    if (topH >= botH) continue;
                    // Rejeter les murs aux hauteurs aberrantes (portails, donnees invalides)
                    float wallH = botH - topH;
                    if (wallH > 2048.0f || wallH < 0.1f) continue;
                    if (Math.abs(topH) > 8192.0f || Math.abs(botH) > 8192.0f) continue;

                    // Coordonnees monde
                    float wx1 = lp.xi(), wz1 = lp.zi();
                    float wx2 = rp.xi(), wz2 = rp.zi();
                    float wallLen = (float) Math.sqrt(
                        (wx2-wx1)*(wx2-wx1) + (wz2-wz1)*(wz2-wz1));

                    // Coordonnees OBJ (Y up, Z negated)
                    float ox1 = ox(wx1), oz1 = oz(wz1);
                    float ox2 = ox(wx2), oz2 = oz(wz2);
                    float yTop = oy(topH), yBot = oy(botH);

                    // UV coords
                    // U : 0 a wallLen/TEX_SCALE_H (repetition horizontale)
                    // V : 0 = haut du mur, 1 = bas (OBJ V=0 en bas, on inverse)
                    WadTextureData tex = textures.get(e.texIndex);
                    float uMax = wallLen / TEX_SCALE_H;
                    float vMax  = wallH / TEX_SCALE_V;

                    // fromTile offset texture
                    float uOff = (e.fromTile & 0xFFFF) < 32768
                        ? (e.fromTile & 0xFFFF) / 16.0f / (tex != null ? tex.width() : 256.0f)
                        : 0.0f;
                    float vOff = (e.yOffset & 0xFFFF) < 32768
                        ? (e.yOffset & 0xFFFF) / 256.0f / (tex != null ? tex.height() : 128.0f)
                        : 0.0f;

                    // Material
                    String matName = (e.texIndex >= 0 && e.texIndex < TEX_NAMES.length)
                        ? "wall_" + e.texIndex
                        : "wall_fallback";
                    w.println("usemtl " + matName);

                    // 4 vertices du quad mur (BL, BR, TR, TL)
                    w.printf("v %.3f %.3f %.3f%n", ox1, yBot, oz1);   // vi+0 BL
                    w.printf("v %.3f %.3f %.3f%n", ox2, yBot, oz2);   // vi+1 BR
                    w.printf("v %.3f %.3f %.3f%n", ox2, yTop, oz2);   // vi+2 TR
                    w.printf("v %.3f %.3f %.3f%n", ox1, yTop, oz1);   // vi+3 TL

                    // 4 UV coords
                    // BL=(0,0)  BR=(uMax,0)  TR=(uMax,vMax)  TL=(0,vMax)
                    // OBJ V=0 en bas : vMax en haut -> inverser : vOff+vMax en bas
                    w.printf("vt %.4f %.4f%n", uOff,        vOff);
                    w.printf("vt %.4f %.4f%n", uOff + uMax, vOff);
                    w.printf("vt %.4f %.4f%n", uOff + uMax, vOff + vMax);
                    w.printf("vt %.4f %.4f%n", uOff,        vOff + vMax);

                    int v0 = vi[0], t0 = ti[0];
                    // Face : vertex/texcoord
                    w.printf("f %d/%d %d/%d %d/%d %d/%d%n",
                        v0,   t0,
                        v0+1, t0+1,
                        v0+2, t0+2,
                        v0+3, t0+3);

                    vi[0] += 4;
                    ti[0] += 4;
                }
            }

            // ── Sols par zone (depuis pointIds) ────────────────────────────────
            w.println();
            w.println("# === SOLS ===");
            for (int zi = 0; zi < level.numZones(); zi++) {
                ZoneData zone = level.zone(zi);
                if (zone == null || zone.pointIds.length < 3) continue;

                float yFloor = oy(zone.floorHeight());
                w.println();
                w.printf("g z%d_floor%n", zi);
                w.println("usemtl floor");

                // Points du sol
                int vStart = vi[0];
                int tStart = ti[0];
                int ptCount = 0;
                for (short ptId : zone.pointIds) {
                    Vec2W pt = level.point(ptId & 0xFFFF);
                    if (pt == null) continue;
                    w.printf("v %.3f %.3f %.3f%n", ox(pt.xi()), yFloor, oz(pt.zi()));
                    // UV simple base sur position monde normalisee
                    w.printf("vt %.4f %.4f%n",
                        (float) pt.xi() / TEX_SCALE_H,
                        (float) pt.zi() / TEX_SCALE_H);
                    vi[0]++;
                    ti[0]++;
                    ptCount++;
                }
                if (ptCount >= 3) {
                    StringBuilder face = new StringBuilder("f");
                    for (int k = 0; k < ptCount; k++)
                        face.append(" ").append(vStart+k).append("/").append(tStart+k);
                    w.println(face);
                }

                // Plafond
                float yRoof = oy(zone.roofHeight());
                if (yRoof != yFloor) {
                    w.printf("g z%d_ceil%n", zi);
                    w.println("usemtl ceil");
                    int vCeilStart = vi[0];
                    int tCeilStart = ti[0];
                    int ceilCount = 0;
                    for (short ptId : zone.pointIds) {
                        Vec2W pt = level.point(ptId & 0xFFFF);
                        if (pt == null) continue;
                        w.printf("v %.3f %.3f %.3f%n", ox(pt.xi()), yRoof, oz(pt.zi()));
                        w.printf("vt %.4f %.4f%n",
                            (float) pt.xi() / TEX_SCALE_H,
                            (float) pt.zi() / TEX_SCALE_H);
                        vi[0]++;
                        ti[0]++;
                        ceilCount++;
                    }
                    if (ceilCount >= 3) {
                        StringBuilder face = new StringBuilder("f");
                        // Inverser l'ordre pour que la normale pointe vers le bas
                        for (int k = ceilCount - 1; k >= 0; k--)
                            face.append(" ").append(vCeilStart+k).append("/").append(tCeilStart+k);
                        w.println(face);
                    }
                }
            }

            // ── Marqueur position depart joueur ───────────────────────────────
            w.println();
            w.println("# === SPAWN JOUEUR ===");
            w.println("g player_start");
            w.println("usemtl player");
            {
                ZoneData pz = level.zone(level.plr1StartZoneId);
                float yBase = (pz != null) ? oy(pz.floorHeight()) : 0f;
                float px = ox(level.plr1StartX);
                float pzz = oz(level.plr1StartZ);
                float s = 30f;
                w.printf("v %.1f %.1f %.1f%n", px,   yBase,      pzz);
                w.printf("v %.1f %.1f %.1f%n", px,   yBase+96f,  pzz);
                w.printf("v %.1f %.1f %.1f%n", px-s, yBase+48f,  pzz);
                w.printf("v %.1f %.1f %.1f%n", px+s, yBase+48f,  pzz);
                w.printf("v %.1f %.1f %.1f%n", px,   yBase+48f,  pzz-s);
                w.printf("v %.1f %.1f %.1f%n", px,   yBase+48f,  pzz+s);
                int v0 = vi[0];
                w.printf("f %d %d %d%n", v0, v0+1, v0+2);
                w.printf("f %d %d %d%n", v0, v0+3, v0+1);
                w.printf("f %d %d %d%n", v0, v0+4, v0+5);
                vi[0] += 6;
            }

            if (w.checkError()) throw new IOException("OBJ write error");
        }
    }

    // =========================================================================
    // MTL avec textures
    // =========================================================================

    static void writeMtl(Path mtlPath, Map<Integer, WadTextureData> textures) throws IOException {
        try (PrintWriter m = new PrintWriter(new BufferedWriter(new FileWriter(mtlPath.toFile())))) {
            m.println("# AB3D2 Level Materials avec textures");
            m.println("# Genere automatiquement par LevelObjExporter");
            m.println();

            // Materiaux murs textures
            for (int i = 0; i < TEX_NAMES.length; i++) {
                m.println("newmtl wall_" + i);
                m.println("Kd 1.0 1.0 1.0");
                m.println("Ka 0.2 0.2 0.2");
                m.println("Ks 0.05 0.05 0.05");
                m.println("Ns 5");
                m.println("d 1.0");
                if (textures.containsKey(i)) {
                    m.println("map_Kd textures/wall_" + i + "_" + TEX_NAMES[i] + ".png");
                }
                m.println();
            }

            // Fallback mur (per-level, pas de texture)
            m.println("newmtl wall_fallback");
            m.println("Kd 0.31 0.31 0.31");
            m.println("Ka 0.1 0.1 0.1");
            m.println("d 1.0\n");

            // Sol
            m.println("newmtl floor");
            m.println("Kd 1.0 1.0 1.0");
            m.println("Ka 0.15 0.15 0.15");
            m.println("map_Kd textures/floor.png");
            m.println("d 1.0\n");

            // Plafond
            m.println("newmtl ceil");
            m.println("Kd 1.0 1.0 1.0");
            m.println("Ka 0.10 0.10 0.10");
            m.println("map_Kd textures/ceil.png");
            m.println("d 1.0\n");

            // Portail
            m.println("newmtl portal");
            m.println("Kd 0.10 0.40 0.90");
            m.println("d 0.30\nillum 2\n");

            // Marqueur joueur
            m.println("newmtl player");
            m.println("Kd 0.00 1.00 0.00");
            m.println("Ka 0.00 0.40 0.00");
            m.println("Ks 0.80 0.80 0.80\nNs 80\nd 1.0");

            if (m.checkError()) throw new IOException("MTL write error");
        }
    }

    // =========================================================================
    // Helpers coords OBJ
    // =========================================================================

    static float ox(float worldX) { return worldX; }
    static float oy(float heightUnits) { return -heightUnits; }
    static float oz(float worldZ)  { return -worldZ; }

    // =========================================================================
    // Export texture PNG
    // =========================================================================

    static void saveTexturePng(WadTextureData tex, Path out) throws IOException {
        int w = tex.width(), h = tex.height();
        int[] src = tex.pixels();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, src[y * w + x]);
        ImageIO.write(img, "PNG", out.toFile());
    }

    static void saveGrayPng(Path out, int w, int h, int argb) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, argb);
        // Grille pour rendre le sol/plafond reconnaissable
        for (int i = 0; i < w; i++) {
            img.setRGB(i, 0, 0xFF606060);
            img.setRGB(i, h-1, 0xFF606060);
            img.setRGB(0, i < h ? i : h-1, 0xFF606060);
            img.setRGB(w-1, i < h ? i : h-1, 0xFF606060);
        }
        ImageIO.write(img, "PNG", out.toFile());
    }
}
