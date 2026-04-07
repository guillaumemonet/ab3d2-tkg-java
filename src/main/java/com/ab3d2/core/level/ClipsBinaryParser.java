package com.ab3d2.core.level;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;

/**
 * Parse le fichier {@code twolev.clips} — clips de sprites par zone.
 * <p>
 * Format : à documenter par reverse-engineering (TODO).
 * Ce fichier contient les données de découpe verticale des sprites
 * pour chaque zone du niveau.
 * </p>
 *
 * @deprecated Implémentation en cours — structure binaire non encore confirmée.
 */
public class ClipsBinaryParser {

    public ClipsBinaryParser() {}

    /**
     * Charge les clips depuis un fichier.
     * @param path chemin vers twolev.clips
     */
    public byte[] loadRaw(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * Charge depuis un InputStream (classpath).
     */
    public byte[] loadRaw(InputStream is) throws IOException {
        return is.readAllBytes();
    }
}
