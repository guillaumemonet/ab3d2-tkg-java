package com.ab3d2;

import java.nio.file.*;

/**
 * Lit test.lnk et extrait les 16 noms de textures murales.
 *   gradle run --main-class=com.ab3d2.GlftTextureDump
 */
public class GlftTextureDump {

    static final String GLFT =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-original/media/includes/test.lnk";

    public static void main(String[] args) throws Exception {
        byte[] d = Files.readAllBytes(Path.of(GLFT));
        System.out.println("test.lnk : " + d.length + " bytes\n");

        // Chercher "WALLINC/STONEWALL" (debut de la section)
        int start = findStr(d, "WALLINC/STONEWALL");
        if (start < 0) { System.out.println("WALLINC/STONEWALL non trouve"); return; }

        // Reculer jusqu'au debut de l'entree de 64 bytes (null precedent)
        while (start > 0 && d[start - 1] != 0) start--;
        System.out.println("WallGFXNames @ 0x" + Integer.toHexString(start) + "\n");

        System.out.printf("%-4s  %-45s  %s%n", "Idx", "Nom GLFT", "Fichier");
        System.out.println("-".repeat(70));
        for (int i = 0; i < 16; i++) {
            int off = start + i * 64;
            String full = readStr(d, off, 64);
            String file = full.isEmpty() ? "(vide)" : basename(full).toLowerCase().replace(".256wad","");
            System.out.printf("[%2d]  %-45s  \"%s\"%n", i, full, file);
        }

        // Heights
        int hOff = start + 16 * 64;
        System.out.println("\nWallHeights :");
        for (int i = 0; i < 16; i++) {
            int off = hOff + i * 2;
            if (off + 1 >= d.length) break;
            int h = ((d[off] & 0xFF) << 8) | (d[off+1] & 0xFF);
            if (h != 0) System.out.printf("  [%2d] %d (0x%04X)%n", i, h, h);
        }
    }

    static int findStr(byte[] d, String s) {
        outer: for (int i = 0; i <= d.length - s.length(); i++) {
            for (int j = 0; j < s.length(); j++) {
                if (Character.toUpperCase(d[i+j] & 0xFF) != Character.toUpperCase(s.charAt(j)))
                    continue outer;
            }
            return i;
        }
        return -1;
    }

    static String readStr(byte[] d, int off, int maxLen) {
        var sb = new StringBuilder();
        for (int i = 0; i < maxLen && off+i < d.length && d[off+i] != 0; i++)
            sb.append((char)(d[off+i] & 0xFF));
        return sb.toString();
    }

    static String basename(String path) {
        int s = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return s >= 0 ? path.substring(s+1) : path;
    }
}
