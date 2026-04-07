package com.ab3d2;

import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;

import java.io.*;
import java.nio.file.*;

/**
 * Exporte les niveaux AB3D2 au format Wavefront OBJ.
 *
 * Lancement :
 *   gradle run --main-class=com.ab3d2.LevelObjExporter
 *
 * Sortie : build/obj/level_A.obj + level_A.mtl  (un fichier par niveau)
 *
 * Axes dans le fichier OBJ (compatibles import Blender par defaut) :
 *   X =  monde_X   (Est)
 *   Y = -heightOf() (positif = haut)
 *   Z = -monde_Z   (Z negated pour Blender "Forward:-Z, Up:Y")
 *
 * Dans Blender, apres import : le Nord AB3D2 (+Z monde) pointe vers +Y Blender.
 */
public class LevelObjExporter {

    static final String RESOURCES_ROOT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String OUTPUT_DIR =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/build/obj";

    public static void main(String[] args) throws Exception {
        System.out.println("=== AB3D2 Level OBJ Exporter ===\n");

        Path root   = Path.of(RESOURCES_ROOT);
        Path outDir = Path.of(OUTPUT_DIR);
        Files.createDirectories(outDir);
        Tables.initFromBytes(new byte[0]);

        int exported = 0, skipped = 0;

        for (char letter = 'A'; letter <= 'P'; letter++) {
            Path binPath   = root.resolve("levels/LEVEL_" + letter + "/twolev.bin");
            Path graphPath = root.resolve("levels/LEVEL_" + letter + "/twolev.graph.bin");

            if (!Files.exists(binPath) || !Files.exists(graphPath)) {
                System.out.printf("  SKIP  LEVEL_%c%n", letter);
                skipped++;
                continue;
            }
            try {
                LevelData level = new GraphicsBinaryParser().load(binPath, graphPath,
                    String.valueOf(letter));
                Path objPath = outDir.resolve("level_" + letter + ".obj");
                Path mtlPath = outDir.resolve("level_" + letter + ".mtl");
                exportLevel(level, objPath, mtlPath);
                System.out.printf("  OK    LEVEL_%c -> %s  (%d zones, %d edges, %d pts)%n",
                    letter, objPath.getFileName(),
                    level.numValidZones(), level.numEdges(), level.numPoints());
                exported++;
            } catch (Exception e) {
                System.out.printf("  ERR   LEVEL_%c  %s%n", letter, e.getMessage());
                e.printStackTrace();
                skipped++;
            }
        }

        System.out.printf("%nExporte : %d  Saute : %d%n", exported, skipped);
        System.out.printf("Dossier : %s%n", outDir.toAbsolutePath());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transform de coordonnees monde → OBJ
    //   objX(wx)    = wx          (X inchange)
    //   objY(wh)    = -wh         (hauteur inversee : smaller=higher → Y up)
    //   objZ(wz)    = -wz         (Z negated pour Blender convention)
    // ─────────────────────────────────────────────────────────────────────────

    static float ox(float worldX) { return worldX; }
    static float oy(int heightUnits) { return -(float) heightUnits; }
    static float oz(float worldZ)  { return -worldZ; }

    static String v(float x, float y, float z) {
        return "v " + fmt(x) + " " + fmt(y) + " " + fmt(z);
    }

    // ─────────────────────────────────────────────────────────────────────────

    static void exportLevel(LevelData level, Path objPath, Path mtlPath) throws IOException {

        writeMtl(mtlPath, level.numZones());

        try (PrintWriter w = new PrintWriter(
                new BufferedWriter(new FileWriter(objPath.toFile())))) {

            w.println("# AB3D2 Level " + level.levelId + " - OBJ Export");
            w.println("# Zones:" + level.numValidZones()
                + "  Edges:" + level.numEdges()
                + "  Points:" + level.numPoints());
            w.println("# Plr1 : (" + level.plr1StartX + ", " + level.plr1StartZ
                + ") zone=" + level.plr1StartZoneId);
            w.println("# Axes OBJ : X=mondeX  Y=-heightOf()  Z=-mondeZ (Blender-compatible)");
            w.println();
            w.println("mtllib " + mtlPath.getFileName());
            w.println();

            int vi = 1;

            // ── Murs par zone ─────────────────────────────────────────────────
            for (int zi = 0; zi < level.numZones(); zi++) {
                ZoneData zone = level.zone(zi);
                if (zone == null) continue;

                float yFloor = oy(zone.floorHeight());
                float yRoof  = oy(zone.roofHeight());
                if (yFloor == yRoof) continue;

                w.println();
                w.println("# Zone " + zi
                    + " floorH=" + zone.floorHeight()
                    + " roofH=" + zone.roofHeight());
                w.println("g zone_" + zi);
                w.println("usemtl zone_" + zi);

                for (short edgeId : zone.edgeIds) {
                    if (edgeId < 0 || edgeId >= level.numEdges()) continue;
                    ZEdge edge = level.edge(edgeId);
                    if (edge == null) continue;

                    float x1 = ox((float) edge.pos().xi());
                    float z1 = oz((float) edge.pos().zi());
                    float x2 = ox((float)(edge.pos().xi() + edge.len().xi()));
                    float z2 = oz((float)(edge.pos().zi() + edge.len().zi()));

                    // Quad mur (bas-gauche, bas-droit, haut-droit, haut-gauche)
                    w.println(v(x1, yFloor, z1));
                    w.println(v(x2, yFloor, z2));
                    w.println(v(x2, yRoof,  z2));
                    w.println(v(x1, yRoof,  z1));

                    if (edge.isPortal()) w.println("usemtl portal");
                    w.println("f " + vi + " " + (vi+1) + " " + (vi+2) + " " + (vi+3));
                    if (edge.isPortal()) w.println("usemtl zone_" + zi);
                    vi += 4;
                }

                // Sol (si des pointIds sont disponibles)
                if (zone.pointIds.length >= 3) {
                    w.println("g zone_" + zi + "_floor");
                    w.println("usemtl floor_" + zi);
                    int ptStart = vi;
                    for (short ptId : zone.pointIds) {
                        Vec2W pt = level.point(ptId & 0xFFFF);
                        if (pt == null) continue;
                        w.println(v(ox((float) pt.xi()), yFloor, oz((float) pt.zi())));
                        vi++;
                    }
                    StringBuilder face = new StringBuilder("f");
                    for (int k = ptStart; k < vi; k++) face.append(" ").append(k);
                    w.println(face);
                }
            }

            // ── Marqueur joueur (croix verte) ─────────────────────────────────
            w.println();
            w.println("# Joueur 1 depart");
            w.println("g player_start");
            w.println("usemtl player");
            {
                ZoneData pZone = level.zone(level.plr1StartZoneId);
                float yBase = (pZone != null) ? oy(pZone.floorHeight()) : 0f;
                float yEye  = yBase + 48f;
                float px = ox((float) level.plr1StartX);
                float pz = oz((float) level.plr1StartZ);
                float s  = 20f;

                w.println(v(px,   yBase,    pz));      // 0 pied
                w.println(v(px,   yEye+32f, pz));      // 1 tete
                w.println(v(px-s, yEye,     pz));      // 2 gauche
                w.println(v(px+s, yEye,     pz));      // 3 droite
                w.println(v(px,   yEye,     pz-s));    // 4 avant
                w.println(v(px,   yEye,     pz+s));    // 5 arriere
                w.println("f " + vi + " " + (vi+1) + " " + (vi+2));
                w.println("f " + vi + " " + (vi+3) + " " + (vi+4));
                w.println("f " + vi + " " + (vi+5) + " " + (vi+1));
                vi += 6;
            }

            // ── Points 2D (reperes geometrie) ─────────────────────────────────
            w.println();
            w.println("# Points 2D monde");
            w.println("g world_points");
            w.println("usemtl point_marker");
            for (int pi = 0; pi < level.numPoints(); pi++) {
                Vec2W pt = level.point(pi);
                if (pt == null) continue;
                float px = ox((float) pt.xi());
                float pz = oz((float) pt.zi());
                float r  = 10f;
                w.println(v(px,   0f, pz+r));
                w.println(v(px+r, 0f, pz-r));
                w.println(v(px-r, 0f, pz-r));
                w.println("f " + vi + " " + (vi+1) + " " + (vi+2));
                vi += 3;
            }

            w.println();
            w.println("# End of level " + level.levelId);

            if (w.checkError())
                throw new IOException("PrintWriter error !");
        }
    }

    // ── MTL ──────────────────────────────────────────────────────────────────

    static void writeMtl(Path mtlPath, int numZones) throws IOException {
        try (PrintWriter m = new PrintWriter(
                new BufferedWriter(new FileWriter(mtlPath.toFile())))) {

            m.println("# AB3D2 Level Materials");
            m.println();

            for (int i = 0; i < numZones; i++) {
                float[] rgb = hsvToRgb((i * 137.5f) % 360f, 0.65f, 0.82f);
                m.println("newmtl zone_" + i);
                m.println("Kd " + fmt(rgb[0]) + " " + fmt(rgb[1]) + " " + fmt(rgb[2]));
                m.println("Ka " + fmt(rgb[0]*0.25f) + " " + fmt(rgb[1]*0.25f) + " " + fmt(rgb[2]*0.25f));
                m.println("Ks 0.10 0.10 0.10\nNs 15\nd 1.0\n");

                float[] rgbf = hsvToRgb((i * 137.5f) % 360f, 0.35f, 0.40f);
                m.println("newmtl floor_" + i);
                m.println("Kd " + fmt(rgbf[0]) + " " + fmt(rgbf[1]) + " " + fmt(rgbf[2]));
                m.println("Ka " + fmt(rgbf[0]*0.2f) + " " + fmt(rgbf[1]*0.2f) + " " + fmt(rgbf[2]*0.2f));
                m.println("d 1.0\n");
            }

            m.println("newmtl portal");
            m.println("Kd 0.10 0.40 0.90\nKa 0.00 0.10 0.30");
            m.println("Ks 0.50 0.50 0.80\nNs 30\nd 0.35\nillum 2\n");

            m.println("newmtl player");
            m.println("Kd 0.00 1.00 0.00\nKa 0.00 0.40 0.00");
            m.println("Ks 0.80 0.80 0.80\nNs 80\nd 1.0\n");

            m.println("newmtl point_marker");
            m.println("Kd 1.00 1.00 0.00\nKa 0.40 0.40 0.00\nd 1.0");

            if (m.checkError()) throw new IOException("MTL write error !");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static String fmt(float v) { return String.format("%.2f", v); }

    static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s, x = c * (1f - Math.abs((h / 60f) % 2 - 1)), m = v - c;
        float r, g, b;
        if      (h < 60)  { r=c; g=x; b=0; }
        else if (h < 120) { r=x; g=c; b=0; }
        else if (h < 180) { r=0; g=c; b=x; }
        else if (h < 240) { r=0; g=x; b=c; }
        else if (h < 300) { r=x; g=0; b=c; }
        else              { r=c; g=0; b=x; }
        return new float[]{ r+m, g+m, b+m };
    }
}
