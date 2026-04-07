package com.ab3d2.tools;

import java.io.IOException;
import java.nio.file.*;

/**
 * Copie les assets depuis les sources Amiga originales vers le projet Java.
 *
 * <h2>Usage</h2>
 * <pre>
 * java -cp out com.ab3d2.tools.AssetSetup [sourceRoot] [destRoot]
 * </pre>
 *
 * <h2>Comportement</h2>
 * Remplace TOUJOURS les fichiers existants (pas de skip).
 * Indispensable pour corriger les fichiers issus d'une ancienne version.
 */
public class AssetSetup {

    static final String DEFAULT_SRC =
        "C:/Users/guill/Downloads/alienbreed3d2-main/alienbreed3d2-main";
    static final String DEFAULT_DST =
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources";

    public static void main(String[] args) throws IOException {
        Path src = Path.of(args.length > 0 ? args[0] : DEFAULT_SRC);
        Path dst = Path.of(args.length > 1 ? args[1] : DEFAULT_DST);

        System.out.println("=== AB3D2 Asset Setup ===");
        System.out.println("Source : " + src.toAbsolutePath());
        System.out.println("Dest   : " + dst.toAbsolutePath());
        System.out.println();

        if (!Files.isDirectory(src)) {
            System.err.println("ERREUR : répertoire source introuvable : " + src);
            System.exit(1);
        }

        int total = 0;
        total += copyLevels(src, dst);
        total += copyWalls(src, dst);
        total += copySounds(src, dst);
        total += copyBigsine(src, dst);

        System.out.println();
        System.out.println("=== " + total + " fichiers copiés ===");
    }

    // ── Niveaux ───────────────────────────────────────────────────────────────

    static int copyLevels(Path src, Path dst) throws IOException {
        // Utiliser levels_editor_uncompressed — ce sont les fichiers éditeur corrects
        Path levSrc = src.resolve("media/levels_editor_uncompressed");
        Path levDst = dst.resolve("levels");

        System.out.println("── Niveaux (" + levSrc + ")");

        if (!Files.isDirectory(levSrc)) {
            System.out.println("   [skip] répertoire introuvable : " + levSrc);
            return 0;
        }

        int count = 0;
        try (var stream = Files.list(levSrc)) {
            for (Path levelDir : (Iterable<Path>) stream.sorted()::iterator) {
                if (!Files.isDirectory(levelDir)) continue;
                String name = levelDir.getFileName().toString();
                if (!name.startsWith("LEVEL_")) continue;

                Path destLevelDir = levDst.resolve(name);
                Files.createDirectories(destLevelDir);

                try (var files = Files.list(levelDir)) {
                    for (Path file : (Iterable<Path>) files::iterator) {
                        if (!Files.isRegularFile(file)) continue;
                        String fname = file.getFileName().toString();
                        if (!fname.startsWith("twolev.")
                                && !fname.equals("properties.dat")
                                && !fname.equals("errata.dat")) continue;
                        Path dest = destLevelDir.resolve(fname);
                        copyFile(file, dest);
                        count++;
                    }
                }
                System.out.println("   " + name + " → " + destLevelDir.getFileName());
            }
        }
        return count;
    }

    // ── Textures de murs ──────────────────────────────────────────────────────

    static int copyWalls(Path src, Path dst) throws IOException {
        Path wallSrc = src.resolve("media/wallinc");
        Path wallDst = dst.resolve("walls");

        System.out.println("── Textures murs (" + wallSrc + ")");

        if (!Files.isDirectory(wallSrc)) {
            System.out.println("   [skip] répertoire introuvable : " + wallSrc);
            return 0;
        }

        Files.createDirectories(wallDst);
        int count = 0;
        try (var stream = Files.list(wallSrc)) {
            for (Path file : (Iterable<Path>) stream.sorted()::iterator) {
                if (!Files.isRegularFile(file)) continue;
                if (!file.getFileName().toString().toLowerCase().endsWith(".256wad")) continue;
                copyFile(file, wallDst.resolve(file.getFileName()));
                count++;
            }
        }
        System.out.println("   " + count + " fichiers .256wad copiés");
        return count;
    }

    // ── Sons ──────────────────────────────────────────────────────────────────

    static int copySounds(Path src, Path dst) throws IOException {
        Path sndSrc = src.resolve("media/sounds");
        Path sndDst = dst.resolve("sounds/raw");

        System.out.println("── Sons bruts (" + sndSrc + ")");

        if (!Files.isDirectory(sndSrc)) {
            System.out.println("   [skip] répertoire introuvable : " + sndSrc);
            return 0;
        }

        Files.createDirectories(sndDst);
        int count = 0;
        try (var stream = Files.list(sndSrc)) {
            for (Path file : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(file)) continue;
                String fname = file.getFileName().toString();
                if (fname.endsWith(".info") || fname.endsWith(".lha")
                        || fname.endsWith(".ABK")) continue;
                copyFile(file, sndDst.resolve(file.getFileName()));
                count++;
            }
        }
        System.out.println("   " + count + " fichiers sons copiés");
        return count;
    }

    // ── bigsine ───────────────────────────────────────────────────────────────

    static int copyBigsine(Path src, Path dst) throws IOException {
        // Le fichier bigsine peut être à plusieurs endroits
        String[] candidates = {
            "media/includes/bigsine",
            "ab3d2_source/bigsine",
            "alienbreed3d2/bigsine",
        };

        System.out.println("── bigsine (table sinus)");

        for (String c : candidates) {
            Path srcFile = src.resolve(c);
            if (Files.exists(srcFile)) {
                Path destFile = dst.resolve("bigsine");
                copyFile(srcFile, destFile);
                System.out.println("   bigsine trouvé dans " + c);
                return 1;
            }
        }
        System.out.println("   [skip] bigsine introuvable (tables synthétiques seront utilisées)");
        return 0;
    }

    // ── Copie fichier — TOUJOURS remplacer ────────────────────────────────────

    static void copyFile(Path src, Path dst) throws IOException {
        long srcSize = Files.size(src);
        boolean existed = Files.exists(dst);
        long dstSize = existed ? Files.size(dst) : -1;

        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);

        if (existed && dstSize != srcSize) {
            System.out.printf("   [remplacé] %-40s (%d → %d bytes)%n",
                dst.getFileName(), dstSize, srcSize);
        } else if (!existed) {
            System.out.printf("   [copié]    %-40s (%d bytes)%n",
                dst.getFileName(), srcSize);
        } else {
            System.out.printf("   [ok]       %-40s (%d bytes)%n",
                dst.getFileName(), srcSize);
        }
    }
}
