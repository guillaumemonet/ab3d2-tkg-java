package com.ab3d2.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

/**
 * Lit les fichiers floor IFF ILBM Amiga et les convertit en PNG.
 * Ces fichiers sont des textures de sol individuelles en format Amiga IFF/ILBM.
 *
 * Usage: gradle run --main-class=com.ab3d2.tools.FloorIffLoader
 */
public class FloorIffLoader {

    static final String FLOORS_SRC =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-original/media/graphics/floors";
    static final String OUT_DIR =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources/floors/iff";
    static final String RESOURCES =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";

    public static void main(String[] args) throws Exception {
        Path src = Path.of(FLOORS_SRC);
        Path out = Path.of(OUT_DIR);
        Files.createDirectories(out);

        // Charger la palette principale 256pal.bin
        int[] mainPal = loadPalette(Path.of(RESOURCES, "256pal.bin"));

        System.out.printf("%-20s  %5s  %-10s  %s%n",
            "Fichier", "Taille", "Dims", "Info");
        System.out.println("-".repeat(65));

        List<Path> files = Files.list(src)
            .filter(p -> !p.getFileName().toString().endsWith(".info"))
            .sorted()
            .toList();

        for (Path fp : files) {
            byte[] data = Files.readAllBytes(fp);
            String fname = fp.getFileName().toString();

            if (data.length >= 12 && new String(data, 0, 4).equals("FORM")
                    && new String(data, 8, 4).equals("ILBM")) {
                try {
                    IffImage img = parseIlbm(data);
                    String info = String.format("IFF %dx%d %dbpp comp=%d col=%d",
                        img.w, img.h, img.depth, img.compress, img.palette.length);
                    System.out.printf("%-20s  %5d  %-10s  %s%n",
                        fname, data.length, img.w + "x" + img.h, info);

                    // Sauvegarder le PNG
                    BufferedImage png = toImage(img);
                    ImageIO.write(png, "PNG",
                        out.resolve(fname + ".png").toFile());

                    // Sauvegarder aussi en raw 8-bit si 64x64
                    if (img.w == 64 && img.h == 64) {
                        byte[] raw = toRaw8bit(img);
                        Files.write(out.resolve(fname + ".raw"), raw);
                    }
                } catch (Exception e) {
                    System.out.printf("%-20s  %5d  ERR: %s%n",
                        fname, data.length, e.getMessage());
                }
            } else {
                System.out.printf("%-20s  %5d  format inconnu %s%n",
                    fname, data.length, bytesToHex(data, 8));
            }
        }

        System.out.println("\nPNG exportes dans: " + out);
    }

    // ── Parser IFF ILBM ──────────────────────────────────────────────────────

    static class IffImage {
        int w, h, depth, compress;
        byte[] pixels;   // chunky 8-bit palette indices
        int[] palette;   // ARGB
        int nColors;
    }

    static IffImage parseIlbm(byte[] data) throws Exception {
        IffImage img = new IffImage();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        buf.position(12); // skip FORM+size+ILBM

        while (buf.remaining() >= 8) {
            byte[] idBytes = new byte[4];
            buf.get(idBytes);
            String id = new String(idBytes);
            int size = buf.getInt();
            int dataStart = buf.position();

            if (id.equals("BMHD")) {
                img.w       = buf.getShort() & 0xFFFF;
                img.h       = buf.getShort() & 0xFFFF;
                buf.getShort(); buf.getShort(); // x, y origin
                img.depth   = buf.get() & 0xFF;
                buf.get();  // masking
                img.compress= buf.get() & 0xFF;
            } else if (id.equals("CMAP")) {
                img.nColors = size / 3;
                img.palette = new int[img.nColors];
                for (int i = 0; i < img.nColors; i++) {
                    int r = buf.get() & 0xFF;
                    int g = buf.get() & 0xFF;
                    int b = buf.get() & 0xFF;
                    img.palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            } else if (id.equals("BODY")) {
                byte[] bodyData = new byte[size];
                buf.get(bodyData);
                img.pixels = decodePlanar(bodyData, img.w, img.h,
                    img.depth, img.compress);
            }

            // Avancer au prochain chunk (aligne sur 2)
            int nextPos = dataStart + size + (size & 1);
            if (nextPos <= data.length) buf.position(nextPos);
            else break;
        }

        if (img.pixels == null)
            throw new Exception("BODY manquant");
        if (img.palette == null) {
            // Fallback : niveaux de gris
            img.palette = new int[256];
            for (int i = 0; i < 256; i++)
                img.palette[i] = 0xFF000000 | (i << 16) | (i << 8) | i;
        }
        return img;
    }

    /**
     * Decode les bitplanes IFF ILBM en pixels chunky 8-bit.
     * compress=0 : raw, compress=1 : PackBits RLE
     */
    static byte[] decodePlanar(byte[] body, int w, int h,
                                int depth, int compress) {
        // Largeur en bytes par bitplane (arrondie sur 16 bits)
        int rowBytes = ((w + 15) / 16) * 2;
        byte[] planes = new byte[h * depth * rowBytes];

        if (compress == 0) {
            System.arraycopy(body, 0, planes, 0,
                Math.min(body.length, planes.length));
        } else {
            // PackBits decompression
            int src = 0, dst = 0;
            while (src < body.length && dst < planes.length) {
                int n = body[src++];
                if (n >= 0) {
                    // Copier n+1 bytes
                    int count = n + 1;
                    for (int i = 0; i < count && src < body.length
                         && dst < planes.length; i++) {
                        planes[dst++] = body[src++];
                    }
                } else if (n != -128) {
                    // Repeter 1 byte (-n+1) fois
                    int count = -n + 1;
                    byte val = body[src++];
                    for (int i = 0; i < count && dst < planes.length; i++) {
                        planes[dst++] = val;
                    }
                }
            }
        }

        // Convertir bitplanes -> chunky
        byte[] pixels = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = 0;
                for (int p = 0; p < depth; p++) {
                    int planeOff = (y * depth + p) * rowBytes;
                    int byteIdx  = planeOff + x / 8;
                    int bitMask  = 0x80 >> (x & 7);
                    if (byteIdx < planes.length &&
                        (planes[byteIdx] & bitMask) != 0) {
                        pixel |= (1 << p);
                    }
                }
                pixels[y * w + x] = (byte) pixel;
            }
        }
        return pixels;
    }

    static BufferedImage toImage(IffImage img) {
        BufferedImage bi = new BufferedImage(img.w, img.h,
            BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.h; y++)
            for (int x = 0; x < img.w; x++) {
                int idx = img.pixels[y * img.w + x] & 0xFF;
                bi.setRGB(x, y, idx < img.palette.length
                    ? img.palette[idx] : 0xFF808080);
            }
        return bi;
    }

    static byte[] toRaw8bit(IffImage img) {
        // Retourne les indices palette bruts (pas les couleurs ARGB)
        byte[] raw = new byte[img.w * img.h];
        System.arraycopy(img.pixels, 0, raw, 0, raw.length);
        return raw;
    }

    static int[] loadPalette(Path p) throws Exception {
        byte[] raw = Files.readAllBytes(p);
        int[] pal = new int[256];
        if (raw.length == 1536) {
            for (int i = 0; i < 256; i++) {
                int r = raw[i*6+1] & 0xFF;
                int g = raw[i*6+3] & 0xFF;
                int b = raw[i*6+5] & 0xFF;
                pal[i] = 0xFF000000 | (r<<16) | (g<<8) | b;
            }
        }
        return pal;
    }

    static String bytesToHex(byte[] b, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, b.length); i++)
            sb.append(String.format("%02X ", b[i]));
        return sb.toString().trim();
    }
}
