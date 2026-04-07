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
 * Gestionnaire d'assets central pour AB3D2.
 *
 * <h2>Formats supportés</h2>
 * <ul>
 *   <li>PNG/JPG/BMP — via STB (assets de dev/test)</li>
 *   <li>.256wad — textures de murs format AB3D2 natif (via {@link WallTextureExtractor})</li>
 *   <li>palette.bin — palette 256 couleurs extraite des assets Amiga</li>
 * </ul>
 *
 * <h2>Format palette.bin</h2>
 * Deux formats sont acceptés automatiquement :
 * <ul>
 *   <li>768 bytes  — raw RGB8 (256 × 3 bytes)</li>
 *   <li>1536 bytes — {@code draw_Palette_vw} dump (256 × 3 UWORDs big-endian,
 *       valeur 8 bits dans le low byte de chaque UWORD)</li>
 * </ul>
 */
public class AssetManager {

    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);

    private final Path assetRoot;
    private final Map<String, Integer>        textureCache = new HashMap<>();
    private final Map<String, WadTextureData> wadCache     = new HashMap<>();

    /** Palette 256 couleurs ARGB. Index 0 = transparent par convention AB3D2. */
    private int[] palette = new int[256];

    /** Extracteur de textures .256wad, initialisé après le chargement de la palette. */
    private WallTextureExtractor wallExtractor;

    public AssetManager(Path assetRoot) {
        this.assetRoot = assetRoot;
        log.info("AssetManager root: {}", assetRoot.toAbsolutePath());
    }

    public void init() {
        loadDefaultPalette();
        wallExtractor = new WallTextureExtractor(palette);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Palette
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Charge la palette depuis {@code palette.bin}.
     * Deux formats sont détectés automatiquement :
     * <ul>
     *   <li>768 bytes  → raw RGB8 (256 × 3 bytes)</li>
     *   <li>1536 bytes → draw_Palette_vw (256 × 3 UWORDs big-endian, low byte = canal)</li>
     * </ul>
     */
    private void loadDefaultPalette() {
        // Priorite : 256pal.bin (vraie palette jeu, incbin "256pal" dans draw_data.s)
        // Fallback : palette.bin (peut etre une rampe de gris)
        Path palFile = assetRoot.resolve("256pal.bin");
        if (!Files.exists(palFile)) palFile = assetRoot.resolve("palette.bin");
        if (Files.exists(palFile)) {
            try {
                byte[] data = Files.readAllBytes(palFile);
                switch (data.length) {
                    case 768  -> loadPaletteRGB8(data);
                    case 1536 -> loadPaletteDrawPaletteVw(data);
                    default   -> {
                        log.warn("palette.bin : taille inattendue ({} bytes). " +
                                 "Formats acceptés : 768 (RGB8) ou 1536 (draw_Palette_vw). " +
                                 "Fallback greyscale.", data.length);
                        buildGreyscalePalette();
                    }
                }
                // Index 0 = transparent (convention AB3D2 sprites)
                palette[0] = 0x00000000;
                log.info("Palette chargée depuis {} ({} bytes)", palFile.getFileName(), data.length);
            } catch (IOException e) {
                log.warn("Impossible de charger palette.bin, fallback greyscale", e);
                buildGreyscalePalette();
            }
        } else {
            log.warn("palette.bin introuvable, fallback greyscale");
            buildGreyscalePalette();
        }
    }

    /**
     * Format raw RGB8 : 256 entrées × 3 bytes {R, G, B}.
     */
    private void loadPaletteRGB8(byte[] data) {
        for (int i = 0; i < 256; i++) {
            int r = data[i * 3]     & 0xFF;
            int g = data[i * 3 + 1] & 0xFF;
            int b = data[i * 3 + 2] & 0xFF;
            palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    /**
     * Format draw_Palette_vw : 256 entrées × 3 UWORDs big-endian.
     * La valeur 8 bits utile est dans le LOW byte de chaque UWORD.
     * Correspond exactement à {@code draw_Palette_vw[3 × 256]} du source ASM.
     */
    private void loadPaletteDrawPaletteVw(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < 256; i++) {
            int r = buf.getShort() & 0xFF;  // low byte du UWORD R
            int g = buf.getShort() & 0xFF;  // low byte du UWORD G
            int b = buf.getShort() & 0xFF;  // low byte du UWORD B
            palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    private void buildGreyscalePalette() {
        for (int i = 0; i < 256; i++) {
            palette[i] = 0xFF000000 | (i << 16) | (i << 8) | i;
        }
        palette[0] = 0x00000000;
    }

    /**
     * Remplace la palette depuis des données raw Amiga (appelé après chargement du niveau).
     */
    public void loadPaletteFromAmiga(byte[] rawPalette) {
        switch (rawPalette.length) {
            case 768  -> loadPaletteRGB8(rawPalette);
            case 1536 -> loadPaletteDrawPaletteVw(rawPalette);
            default   -> {
                // Tentative en raw RGB8 si taille inconnue
                for (int i = 0; i < Math.min(256, rawPalette.length / 3); i++) {
                    int r = rawPalette[i * 3]     & 0xFF;
                    int g = rawPalette[i * 3 + 1] & 0xFF;
                    int b = rawPalette[i * 3 + 2] & 0xFF;
                    palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
        palette[0] = 0x00000000;
        // Réinitialiser l'extracteur avec la nouvelle palette
        wallExtractor = new WallTextureExtractor(palette);
        // Vider le cache WAD (les textures doivent être rechargées avec la nouvelle palette)
        wadCache.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Textures .256wad (murs AB3D2)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Charge une texture de mur depuis un fichier {@code .256wad}.
     * La texture est décodée, uploadée sur le GPU et mise en cache.
     *
     * @param relativePath chemin relatif depuis assetRoot (ex. "walls/stonewall.256wad")
     * @return ID de texture OpenGL
     */
    public int loadWallTexture(String relativePath) {
        String cacheKey = "wad:" + relativePath.toLowerCase();
        return textureCache.computeIfAbsent(cacheKey, k -> {
            WadTextureData data = loadWadData(relativePath);
            return createTextureFromARGB(data.pixels(), data.width(), data.height());
        });
    }

    /**
     * Charge et décode un {@code .256wad} mais ne crée PAS de texture OpenGL.
     * Utile pour le renderer software ou les outils de conversion.
     *
     * @param relativePath chemin relatif depuis assetRoot
     * @return données de texture décodées avec shade table complète
     */
    public WadTextureData loadWadData(String relativePath) {
        String cacheKey = relativePath.toLowerCase();
        return wadCache.computeIfAbsent(cacheKey, k -> {
            Path file = assetRoot.resolve(relativePath);
            if (!Files.exists(file)) {
                log.error("Fichier .256wad introuvable : {}", file);
                return buildMagentaWad(relativePath);
            }
            try {
                return wallExtractor.load(file);
            } catch (Exception e) {
                log.error("Erreur lors du chargement de {} : {}", relativePath, e.getMessage());
                return buildMagentaWad(relativePath);
            }
        });
    }

    /**
     * Charge toutes les textures .256wad d'un répertoire et les uploade sur le GPU.
     *
     * @param relativeDir chemin relatif depuis assetRoot (ex. "walls/")
     * @return map nom→texID OpenGL
     */
    public Map<String, Integer> loadAllWallTextures(String relativeDir) {
        Path dir = assetRoot.resolve(relativeDir);
        if (!Files.exists(dir)) {
            log.warn("Répertoire de textures introuvable : {}", dir);
            return Map.of();
        }
        Map<String, Integer> result = new java.util.LinkedHashMap<>();
        try {
            Map<String, WadTextureData> wads = wallExtractor.loadAll(dir);
            wads.forEach((name, data) -> {
                int texId = createTextureFromARGB(data.pixels(), data.width(), data.height());
                result.put(name, texId);
                // Mettre aussi en cache WAD pour accès à la shade table
                wadCache.put(name, data);
                textureCache.put("wad:" + name + ".256wad", texId);
            });
        } catch (IOException e) {
            log.error("Erreur lors du chargement des textures depuis {} : {}", dir, e.getMessage());
        }
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Textures PNG/JPG/BMP (via STB)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Charge une texture PNG/JPG/BMP via STBImage.
     * @param path chemin relatif depuis assetRoot
     */
    public int loadTexture(String path) {
        return textureCache.computeIfAbsent(path, k -> {
            Path file = assetRoot.resolve(k);
            if (!Files.exists(file)) {
                log.error("Texture introuvable : {}", file);
                return createMagentaTexture();
            }
            return loadTextureSTB(file);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Création de textures OpenGL
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Crée une texture OpenGL depuis un tableau ARGB int[].
     * Utilise GL_NEAREST (pixel art authentique).
     */
    public int createTextureFromARGB(int[] argb, int width, int height) {
        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        for (int px : argb) {
            buf.put((byte) ((px >> 16) & 0xFF)); // R
            buf.put((byte) ((px >>  8) & 0xFF)); // G
            buf.put((byte) ( px        & 0xFF)); // B
            buf.put((byte) ((px >> 24) & 0xFF)); // A
        }
        buf.flip();
        return uploadTexture(buf, width, height);
    }

    /**
     * Crée une texture depuis une image indexée 1 byte/pixel en appliquant la palette.
     */
    public int createTextureFromIndexed(byte[] indexed, int width, int height) {
        ByteBuffer rgba = ByteBuffer.allocateDirect(width * height * 4);
        for (int i = 0; i < Math.min(indexed.length, width * height); i++) {
            int argb = palette[indexed[i] & 0xFF];
            rgba.put((byte) ((argb >> 16) & 0xFF));
            rgba.put((byte) ((argb >>  8) & 0xFF));
            rgba.put((byte) ( argb        & 0xFF));
            rgba.put((byte) ((argb >> 24) & 0xFF));
        }
        rgba.flip();
        return uploadTexture(rgba, width, height);
    }

    private int loadTextureSTB(Path file) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(false);
            ByteBuffer data = stbi_load(file.toString(), w, h, c, 4);
            if (data == null) {
                log.error("STB impossible de charger {} : {}", file, stbi_failure_reason());
                return createMagentaTexture();
            }
            int tex = uploadTexture(data, w.get(0), h.get(0));
            stbi_image_free(data);
            log.debug("Texture chargée : {} ({}x{})", file.getFileName(), w.get(0), h.get(0));
            return tex;
        }
    }

    private int uploadTexture(ByteBuffer data, int width, int height) {
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, data);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        return tex;
    }

    /** Texture magenta 1×1 = asset manquant, facile à repérer visuellement. */
    private int createMagentaTexture() {
        ByteBuffer buf = ByteBuffer.allocateDirect(4);
        buf.put((byte) 0xFF).put((byte) 0x00).put((byte) 0xFF).put((byte) 0xFF);
        buf.flip();
        return uploadTexture(buf, 1, 1);
    }

    /** WadTextureData magenta de substitution (pour les cas d'erreur). */
    private static WadTextureData buildMagentaWad(String name) {
        int magenta = 0xFFFF00FF;
        int[] pixels = new int[64 * 64];
        java.util.Arrays.fill(pixels, magenta);
        int[] shade = new int[WadTextureData.SHADE_TABLE_ENTRIES];
        java.util.Arrays.fill(shade, magenta);
        return new WadTextureData(name, 64, 64, pixels, shade);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Gestion du cycle de vie
    // ──────────────────────────────────────────────────────────────────────────

    public void freeTexture(String path) {
        Integer tex = textureCache.remove(path);
        if (tex != null) glDeleteTextures(tex);
    }

    public void destroy() {
        textureCache.values().forEach(id -> glDeleteTextures(id));
        textureCache.clear();
        wadCache.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Accesseurs
    // ──────────────────────────────────────────────────────────────────────────

    public int[]                getPalette()       { return palette; }
    public Path                 getRoot()          { return assetRoot; }
    public WallTextureExtractor getWallExtractor() { return wallExtractor; }
}
