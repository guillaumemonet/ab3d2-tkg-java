package com.ab3d2.tools;

import java.nio.file.*;

/**
 * Copie les assets binaires depuis le dossier original vers les resources.
 * gradle run --main-class=com.ab3d2.tools.AssetSetup
 */
public class AssetSetup {

    static final String SRC  = "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-original/media/includes";
    static final String DEST = "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";

    public static void main(String[] args) throws Exception {
        // Textures sol/plafond
        Path floorDir = Path.of(DEST, "floors");
        Files.createDirectories(floorDir);
        copy(SRC + "/floortile",      DEST + "/floors/floortile");
        copy(SRC + "/floor256pal",    DEST + "/floors/floor256pal");
        copy(SRC + "/newtexturemaps", DEST + "/floors/newtexturemaps");

        System.out.println("Done!");
    }

    static void copy(String src, String dst) throws Exception {
        Path s = Path.of(src), d = Path.of(dst);
        if (!Files.exists(s)) { System.out.println("MANQUANT: " + s); return; }
        Files.copy(s, d, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("OK: " + d.getFileName() + " (" + Files.size(d) + " bytes)");
    }
}
