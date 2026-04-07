package com.ab3d2.core.level;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Stub — remplacé par {@link CollisionMap#load(Path)}.
 * @deprecated Utiliser {@code CollisionMap.load(path)} directement.
 */
@Deprecated
public class CollisionMapParser {

    public static CollisionMap parse(byte[] data) {
        return new CollisionMap(data);
    }

    public static CollisionMap load(Path path) throws IOException {
        return CollisionMap.load(path);
    }
}
