package com.ab3d2;

import com.ab3d2.core.level.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public class TexDimFromWall {

    static final String R = "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";
    static final String[] N = {"stonewall", "brownpipes", "hullmetal", "technotritile",
        "brownspeakers", "chevrondoor", "technolights", "redhullmetal",
        "alienredwall", "gieger", "rocky", "steampunk", "brownstonestep"};

    public static void main(String[] a) throws Exception {
        Path root = Path.of(R);
        Map<Integer, Set<String>> m = new TreeMap<>();
        for (int i = 0; i < N.length; i++) {
            m.put(i, new TreeSet<>());
        }
        for (char c = 'A'; c <= 'P'; c++) {
            Path b = root.resolve("levels/LEVEL_" + c + "/twolev.bin");
            Path g = root.resolve("levels/LEVEL_" + c + "/twolev.graph.bin");
            if (!Files.exists(b) || !Files.exists(g)) {
                continue;
            }
            LevelData lv = new GraphicsBinaryParser().load(b, g, String.valueOf(c));
            byte[] raw = Files.readAllBytes(g);
            ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
            buf.getInt();
            buf.getInt();
            buf.getInt();
            int zga = buf.getInt();
            for (WallRenderEntry[] z : new ZoneGraphParser().parse(raw, lv.numZones(), zga)) {
                for (WallRenderEntry e : z) {
                    if (!e.isWall()) {
                        continue;
                    }
                    int i = e.texIndex;
                    if (i < 0 || i >= N.length) {
                        continue;
                    }
                    m.get(i).add(((e.wMask & 0xFF) + 1) + "x" + ((e.hMask & 0xFF) + 1));
                }
            }
        }
        System.out.println("Texture                  [idx]  wMask+1 x hMask+1");
        System.out.println("-".repeat(55));
        for (int i = 0; i < N.length; i++) {
            System.out.printf("%-24s [%2d]   %s%n", N[i], i, m.get(i).isEmpty() ? "(non utilisée)" : m.get(i));
        }
    }
}
