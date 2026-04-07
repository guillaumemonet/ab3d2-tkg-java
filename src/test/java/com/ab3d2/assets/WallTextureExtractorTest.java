package com.ab3d2.assets;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour WallTextureExtractor.
 *
 * Exécutables sans GPU (pas de contexte OpenGL requis).
 * Pointe directement sur les assets originaux dans les sources Amiga.
 *
 * Chemin configuré via la propriété système :
 *   -Dab3d2.src.wallinc=/chemin/vers/media/wallinc
 * ou détecté automatiquement depuis les chemins relatifs courants.
 */
class WallTextureExtractorTest {

    private static final String WALLINC_PROP = "ab3d2.src.wallinc";
    private static final String[] WALLINC_CANDIDATES = {
        System.getProperty(WALLINC_PROP, ""),
        "C:/Users/guill/Downloads/alienbreed3d2-main/alienbreed3d2-main/media/wallinc",
        "../../../Downloads/alienbreed3d2-main/alienbreed3d2-main/media/wallinc",
    };
    private static final String[] PALETTE_CANDIDATES = {
        "src/main/resources/palette.bin",
        "C:/Users/guill/Documents/NetBeansProjects/ab3d2-tkg-java/src/main/resources/palette.bin",
    };

    private static Path wallinc;
    private static int[] palette;
    private static WallTextureExtractor extractor;

    @BeforeAll
    static void setUp() throws IOException {
        wallinc = null;
        for (String c : WALLINC_CANDIDATES) {
            if (c == null || c.isBlank()) continue;
            Path p = Path.of(c);
            if (Files.isDirectory(p)) { wallinc = p; break; }
        }
        if (wallinc == null)
            System.err.println("[skip] wallinc introuvable. Définir -D" + WALLINC_PROP);

        palette = new int[256];
        Path palPath = null;
        for (String c : PALETTE_CANDIDATES) {
            Path p = Path.of(c);
            if (Files.exists(p)) { palPath = p; break; }
        }
        if (palPath != null) {
            byte[] data = Files.readAllBytes(palPath);
            if (data.length == 768) {
                for (int i = 0; i < 256; i++) {
                    int r = data[i*3]   & 0xFF;
                    int g = data[i*3+1] & 0xFF;
                    int b = data[i*3+2] & 0xFF;
                    palette[i] = 0xFF000000 | (r<<16) | (g<<8) | b;
                }
            } else if (data.length == 1536) {
                var buf = java.nio.ByteBuffer.wrap(data)
                              .order(java.nio.ByteOrder.BIG_ENDIAN);
                for (int i = 0; i < 256; i++) {
                    int r = buf.getShort() & 0xFF;
                    int g = buf.getShort() & 0xFF;
                    int b = buf.getShort() & 0xFF;
                    palette[i] = 0xFF000000 | (r<<16) | (g<<8) | b;
                }
            } else {
                for (int i = 0; i < 256; i++) palette[i] = 0xFF000000|(i<<16)|(i<<8)|i;
            }
        } else {
            System.err.println("[warn] palette.bin introuvable, greyscale.");
            for (int i = 0; i < 256; i++) palette[i] = 0xFF000000|(i<<16)|(i<<8)|i;
        }
        extractor = new WallTextureExtractor(palette);
    }

    // ── Détection des dimensions ──────────────────────────────────────────────

    @Test
    void detectDimensions_64x64() {
        // 2048 + ceil(64/3)*64*2 = 2048 + 22*64*2 = 4864
        int[] dims = WallTextureExtractor.detectDimensions(4864);
        assertNotNull(dims);
        assertEquals(64, dims[0]);
        assertEquals(64, dims[1]);
    }

    @Test
    void detectDimensions_32x64() {
        // 2048 + 11*64*2 = 3456
        int[] dims = WallTextureExtractor.detectDimensions(3456);
        assertNotNull(dims);
        assertEquals(32, dims[0]);
        assertEquals(64, dims[1]);
    }

    @Test
    void detectDimensions_unknown_returnsNull() {
        assertNull(WallTextureExtractor.detectDimensions(1234));
    }

    @Test
    void shadeTableSize_isCorrect() {
        assertEquals(2048, WallTextureExtractor.SHADE_TABLE_BYTES);
        assertEquals(32,   WallTextureExtractor.SHADE_ROWS);
        assertEquals(32,   WallTextureExtractor.ENTRIES_PER_ROW);
    }

    // ── Décodage synthétique ──────────────────────────────────────────────────

    @Test
    void decode_syntheticTexture_correctPixelCount() {
        int texW = 32, texH = 32;
        int numGroups  = (texW + 2) / 3;
        int chunkBytes = numGroups * texH * 2;
        byte[] raw = new byte[WallTextureExtractor.SHADE_TABLE_BYTES + chunkBytes];
        // Shade table : palIdx=1 pour toutes les entrées
        for (int i = 0; i < WallTextureExtractor.SHADE_TABLE_BYTES; i += 2) {
            raw[i] = 0; raw[i+1] = 1;
        }

        WadTextureData tex = extractor.decode("test_synthetic", raw, texW, texH);

        assertEquals("test_synthetic", tex.name());
        assertEquals(texW,            tex.width());
        assertEquals(texH,            tex.height());
        assertEquals(texW * texH,     tex.pixels().length);
        assertEquals(WadTextureData.SHADE_TABLE_ENTRIES, tex.shadeTable().length);
    }

    @Test
    void decode_pack0_pack1_pack2_distinct() {
        // Texture 3x1 : 1 groupe, 1 ligne
        // word = PACK0=5, PACK1=10, PACK2=15
        int texW = 3, texH = 1;
        byte[] raw = new byte[WallTextureExtractor.SHADE_TABLE_BYTES + 2];

        // Shade table row r, entry e → palIdx = e (pour vérification directe)
        for (int row = 0; row < 32; row++)
            for (int e = 0; e < 32; e++) {
                raw[(row*32 + e)*2]     = 0;
                raw[(row*32 + e)*2 + 1] = (byte) e;
            }

        // Chunk : PACK0=5, PACK1=10, PACK2=15
        // word big-endian = 5 | (10<<5) | (15<<10)
        int word = 5 | (10 << 5) | (15 << 10);
        int off  = WallTextureExtractor.SHADE_TABLE_BYTES;
        raw[off]   = (byte)(word >> 8);
        raw[off+1] = (byte)(word & 0xFF);

        WadTextureData tex = extractor.decode("test_pack", raw, texW, texH);

        // Shade row 31 → shadeTable[31*32 + texel] = palette[texel]
        assertEquals(palette[5],  tex.pixels()[0], "PACK0");
        assertEquals(palette[10], tex.pixels()[1], "PACK1");
        assertEquals(palette[15], tex.pixels()[2], "PACK2");
    }

    @Test
    void shadeColor_rowBounds() {
        int texW = 3, texH = 1;
        byte[] raw = new byte[WallTextureExtractor.SHADE_TABLE_BYTES + 2];
        // row r, entry 0 → palIdx = r
        for (int row = 0; row < 32; row++) {
            raw[row * 32 * 2]     = 0;
            raw[row * 32 * 2 + 1] = (byte) row;
        }
        WadTextureData tex = extractor.decode("test_shade", raw, texW, texH);
        assertEquals(palette[0],  tex.shadeColor(0,  0));
        assertEquals(palette[31], tex.shadeColor(31, 0));
    }

    // ── Fichiers réels ────────────────────────────────────────────────────────

    @Test
    void load_stonewall_realFile() throws IOException {
        Assumptions.assumeTrue(wallinc != null, "wallinc non disponible");
        Path f = wallinc.resolve("stonewall.256wad");
        Assumptions.assumeTrue(Files.exists(f), "stonewall.256wad introuvable");

        WadTextureData tex = extractor.load(f);

        assertEquals("stonewall",          tex.name());
        assertTrue(tex.width()  > 0,       "width > 0");
        assertTrue(tex.height() > 0,       "height > 0");
        assertEquals(tex.width() * tex.height(), tex.pixels().length);
        assertEquals(WadTextureData.SHADE_TABLE_ENTRIES, tex.shadeTable().length);

        System.out.printf("[ok] stonewall → %dx%d (%d pixels)%n",
            tex.width(), tex.height(), tex.pixels().length);
    }

    @Test
    void loadAll_wallinc() throws IOException {
        Assumptions.assumeTrue(wallinc != null, "wallinc non disponible");

        var textures = extractor.loadAll(wallinc);

        assertFalse(textures.isEmpty(), "Doit charger au moins une texture");
        textures.forEach((name, tex) -> {
            assertNotNull(tex.pixels(), name + " : pixels null");
            assertEquals(tex.width() * tex.height(), tex.pixels().length,
                name + " : taille pixels incorrecte");
            System.out.printf("[ok] %-35s → %dx%d%n", name, tex.width(), tex.height());
        });
    }

    @Test
    void describeKnownSizes_notEmpty() {
        String desc = WallTextureExtractor.describeKnownSizes();
        assertFalse(desc.isBlank());
        assertTrue(desc.contains("64x64"));
    }
}
