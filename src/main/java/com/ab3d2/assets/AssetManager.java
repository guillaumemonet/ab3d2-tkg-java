package com.ab3d2.assets;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Gestionnaire d'assets pour AB3D2.
 * Formats supportés :
 *  - PNG/JPG/BMP  : via STB (pour assets de dev/test)
 *  - .256wad      : texture indexée 256 couleurs (format AB3D2 natif)
 *  - palette.bin  : palette 256x3 RGB (extraite des assets Amiga)
 *
 * Les assets sont cherchés dans ce chemin (configurable) :
 *   assets/  (à côté du jar) ou src/main/resources/assets/ en dev
 */
public class AssetManager {

    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);

    private final Path assetRoot;
    private final Map<String, Integer> textureCache = new HashMap<>();

    // Palette 256 couleurs (RGB) issue du jeu original
    private int[] palette = new int[256]; // ARGB packed

    public AssetManager(Path assetRoot) {
        this.assetRoot = assetRoot;
        log.info("AssetManager root: {}", assetRoot.toAbsolutePath());
    }

    public void init() {
        loadDefaultPalette();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Palette
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * La palette par défaut est une palette grise si aucun fichier n'existe.
     * Elle sera remplacée par la vraie palette AB3D2 une fois les assets chargés.
     */
    private void loadDefaultPalette() {
        Path palFile = assetRoot.resolve("palette.bin");
        if (Files.exists(palFile)) {
            try {
                byte[] data = Files.readAllBytes(palFile);
                // Format : 256 entrées × 3 bytes RGB
                for (int i = 0; i < 256 && i * 3 + 2 < data.length; i++) {
                    int r = data[i * 3]     & 0xFF;
                    int g = data[i * 3 + 1] & 0xFF;
                    int b = data[i * 3 + 2] & 0xFF;
                    palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
                log.info("Loaded palette from {}", palFile);
            } catch (IOException e) {
                log.warn("Failed to load palette, using greyscale fallback", e);
                buildGreyscalePalette();
            }
        } else {
            log.warn("No palette.bin found, using greyscale fallback");
            buildGreyscalePalette();
        }
    }

    private void buildGreyscalePalette() {
        for (int i = 0; i < 256; i++) {
            palette[i] = 0xFF000000 | (i << 16) | (i << 8) | i;
        }
        // Index 0 = transparent (convention AB3D2)
        palette[0] = 0x00000000;
    }

    public void loadPaletteFromAmiga(byte[] rawPalette) {
        // Format Amiga OCS : 16-bit par couleur (4 bits par composant, 0xRGB)
        // Format Amiga AGA : 24-bit (via color registers étendu)
        // Ici on suppose RGB 8-bit direct (déjà converti)
        for (int i = 0; i < Math.min(256, rawPalette.length / 3); i++) {
            int r = rawPalette[i * 3]     & 0xFF;
            int g = rawPalette[i * 3 + 1] & 0xFF;
            int b = rawPalette[i * 3 + 2] & 0xFF;
            palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        palette[0] = 0x00000000; // index 0 transparent
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Chargement textures
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Charge une texture PNG/JPG/BMP via STB.
     * @param path chemin relatif depuis assetRoot
     */
    public int loadTexture(String path) {
        return textureCache.computeIfAbsent(path, k -> {
            Path file = assetRoot.resolve(k);
            if (!Files.exists(file)) {
                log.error("Texture not found: {}", file);
                return createMagentaTexture();
            }
            return loadTextureSTB(file);
        });
    }

    /**
     * Charge une texture indexée .256wad (format AB3D2).
     * @param path chemin relatif
     * @param width  largeur en pixels
     * @param height hauteur en pixels
     */
    public int load256Wad(String path, int width, int height) {
        String key = "256wad:" + path;
        return textureCache.computeIfAbsent(key, k -> {
            Path file = assetRoot.resolve(path);
            if (!Files.exists(file)) {
                log.error("256wad not found: {}", file);
                return createMagentaTexture();
            }
            try {
                byte[] indexed = Files.readAllBytes(file);
                return createTextureFromIndexed(indexed, width, height);
            } catch (IOException e) {
                log.error("Failed to load 256wad: {}", path, e);
                return createMagentaTexture();
            }
        });
    }

    /**
     * Convertit une image indexée (1 byte/pixel) en texture RGBA via palette.
     */
    public int createTextureFromIndexed(byte[] indexed, int width, int height) {
        ByteBuffer rgba = ByteBuffer.allocateDirect(width * height * 4);
        for (int i = 0; i < Math.min(indexed.length, width * height); i++) {
            int idx  = indexed[i] & 0xFF;
            int argb = palette[idx];
            rgba.put((byte)((argb >> 16) & 0xFF)); // R
            rgba.put((byte)((argb >>  8) & 0xFF)); // G
            rgba.put((byte)( argb        & 0xFF)); // B
            rgba.put((byte)((argb >> 24) & 0xFF)); // A
        }
        rgba.flip();
        return uploadTexture(rgba, width, height, false);
    }

    private int loadTextureSTB(Path file) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(false);
            ByteBuffer data = stbi_load(file.toString(), w, h, c, 4);
            if (data == null) {
                log.error("STB failed to load {}: {}", file, stbi_failure_reason());
                return createMagentaTexture();
            }

            int tex = uploadTexture(data, w.get(0), h.get(0), false);
            stbi_image_free(data);
            log.debug("Loaded texture: {} ({}x{})", file.getFileName(), w.get(0), h.get(0));
            return tex;
        }
    }

    private int uploadTexture(ByteBuffer data, int width, int height, boolean mipmap) {
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        // Nearest neighbor pour pixel art authentique
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        if (mipmap) glGenerateMipmap(GL_TEXTURE_2D);
        return tex;
    }

    /** Texture rose flashy = asset manquant, facile à débugger. */
    private int createMagentaTexture() {
        ByteBuffer buf = ByteBuffer.allocateDirect(4);
        buf.put((byte)0xFF).put((byte)0x00).put((byte)0xFF).put((byte)0xFF);
        buf.flip();
        return uploadTexture(buf, 1, 1, false);
    }

    /**
     * Crée une texture depuis un tableau ARGB (int[], format Java standard).
     */
    public int createTextureFromARGB(int[] argb, int width, int height) {
        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        for (int px : argb) {
            buf.put((byte)((px >> 16) & 0xFF));
            buf.put((byte)((px >>  8) & 0xFF));
            buf.put((byte)( px        & 0xFF));
            buf.put((byte)((px >> 24) & 0xFF));
        }
        buf.flip();
        return uploadTexture(buf, width, height, false);
    }

    public void freeTexture(String path) {
        Integer tex = textureCache.remove(path);
        if (tex != null) glDeleteTextures(tex);
    }

    public void destroy() {
        textureCache.values().forEach(id -> glDeleteTextures(id));
        textureCache.clear();
    }

    public int[] getPalette() { return palette; }
    public Path  getRoot()    { return assetRoot; }
}
