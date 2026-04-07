package com.ab3d2.assets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Extrait les textures de murs depuis les fichiers {@code .256wad} d'AB3D2.
 *
 * <h2>Format binaire .256wad</h2>
 * <pre>
 * Offset    Taille    Description
 * ------   --------   -------------------------------------------------------
 * 0         2048      Shade table
 *                     32 lignes × 32 entrées × 2 bytes (big-endian UWORD)
 *                     ┌─ high byte : non utilisé
 *                     └─ low byte  : index 8 bits dans la palette écran globale
 *                     Ligne  0 = plus sombre
 *                     Ligne 31 = full brightness
 *
 * 2048      var       Chunk data — pixels 5 bits compressés, column-major
 *                     Groupes de 3 colonnes verticales :
 *                       numGroups  = ⌈texWidth / 3⌉
 *                       Pour chaque groupe g, pour chaque ligne y :
 *                         word = big-endian UWORD
 *                         bits [ 4: 0] → texel colonne g×3+0  (PACK0)
 *                         bits [ 9: 5] → texel colonne g×3+1  (PACK1)
 *                         bits [14:10] → texel colonne g×3+2  (PACK2)
 *                         bit  [15]    → toujours 0 (padding)
 * </pre>
 *
 * <h2>Référence ASM</h2>
 * <pre>
 * ; hireswall.s — setup avant le rendu d'un mur
 * add.l #64*32, a3           ; sauter la shade table (2048 bytes)
 * move.l a3, Draw_ChunkPtr_l
 *
 * ; draw_wall.s — inner loop PACK0
 * move.b 1(a5, d4.w*2), d1  ; low byte du word[texV] = index 5 bits
 * and.b  #31, d1             ; masquer bits [4:0]
 * move.b (a2, d1.w*2), (a3)  ; lookup shade table → pixel 8 bits
 * </pre>
 */
public class WallTextureExtractor {

    private static final Logger log = LoggerFactory.getLogger(WallTextureExtractor.class);

    // ── Constantes format .256wad ─────────────────────────────────────────────

    static final int SHADE_ROWS         = WadTextureData.SHADE_ROWS;        // 32
    static final int ENTRIES_PER_ROW    = WadTextureData.ENTRIES_PER_ROW;   // 32
    static final int BYTES_PER_ENTRY    = 2;   // UWORD big-endian, on ne lit que le low byte
    static final int SHADE_TABLE_BYTES  = SHADE_ROWS * ENTRIES_PER_ROW * BYTES_PER_ENTRY; // 2048

    /**
     * Tailles de texture connues (valides dans AB3D2), les plus frequentes en premier.
     * Verifiees sur les 14 fichiers .256wad du jeu original.
     *
     * Rappel : {w,h} → groups=(w+2)/3, chunk=groups*h*2, total=chunk+2048
     *   {256,128} → 24064 bytes (23.50 KB) — alienredwall, brownpipes, etc.
     *   {256, 64} → 13056 bytes (12.75 KB) — brownspeakers, chevrondoor, redhullmetal
     *   {128,128} → 13056 bytes (meme taille que 256x64)
     */
    private static final int[][] CANDIDATE_SIZES = {
        // Tailles confirmees sur les fichiers AB3D2
        {256, 128},   // 24064 bytes — 6 textures standard (alienredwall, hullmetal...)
        {256,  64},   // 13056 bytes — 3 textures (brownspeakers, chevrondoor, redhullmetal)
        {128, 128},   // 13056 bytes — idem (meme taille totale)
        {128,  32},   //  2752 bytes chunk — brownstonestep
        {128,  64},   //  7552 bytes
        {256, 256},   // 46080 bytes (proche rocky/steampunk)
        // Tailles generiques
        {64,  64},
        {32,  64},
        {64,  32},
        {64, 128},
        {32,  32},
        {32, 128},
        {16,  64},
        {96,  64},
        {64,  96},
        {96, 128},
        {128, 256},
    };

    // ── État ──────────────────────────────────────────────────────────────────

    /** Palette globale 256 couleurs, ARGB int[]. */
    private final int[] palette;

    // ── Constructeur ──────────────────────────────────────────────────────────

    /**
     * @param palette palette 256 couleurs ARGB (issue de {@link AssetManager#getPalette()}).
     */
    public WallTextureExtractor(int[] palette) {
        if (palette == null || palette.length < 256)
            throw new IllegalArgumentException("Palette must have 256 entries");
        this.palette = palette;
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Charge et décode un fichier {@code .256wad} depuis le disque.
     * Les dimensions sont auto-détectées depuis la taille du fichier.
     *
     * @param path chemin vers le fichier .256wad
     * @return données de la texture décodée
     * @throws IOException en cas d'erreur de lecture
     * @throws IllegalArgumentException si les dimensions ne peuvent pas être détectées
     */
    public WadTextureData load(Path path) throws IOException {
        byte[] raw = Files.readAllBytes(path);
        String name = path.getFileName().toString()
                         .replaceAll("(?i)\\.256wad$", "");
        int[] dims = detectDimensions(raw.length);
        if (dims == null) {
            throw new IllegalArgumentException(String.format(
                "%s : impossible de détecter les dimensions (taille=%d bytes). " +
                "Utilisez load(path, width, height).", path.getFileName(), raw.length));
        }
        log.debug("Loading {} ({}x{}, {} bytes)", name, dims[0], dims[1], raw.length);
        return decode(name, raw, dims[0], dims[1]);
    }

    /**
     * Charge et décode un fichier {@code .256wad} avec dimensions explicites.
     *
     * @param path   chemin vers le fichier .256wad
     * @param width  largeur de la texture en pixels
     * @param height hauteur de la texture en pixels
     */
    public WadTextureData load(Path path, int width, int height) throws IOException {
        byte[] raw = Files.readAllBytes(path);
        String name = path.getFileName().toString()
                         .replaceAll("(?i)\\.256wad$", "");
        log.debug("Loading {} ({}x{}, {} bytes)", name, width, height, raw.length);
        return decode(name, raw, width, height);
    }

    /**
     * Charge tous les {@code .256wad} d'un répertoire.
     * Les fichiers dont les dimensions ne peuvent pas être détectées sont ignorés
     * avec un warning.
     *
     * @param dir répertoire à scanner
     * @return map nom→texture (nom = nom de fichier sans extension, en minuscules)
     */
    public Map<String, WadTextureData> loadAll(Path dir) throws IOException {
        Map<String, WadTextureData> result = new LinkedHashMap<>();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().toLowerCase().endsWith(".256wad"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          WadTextureData tex = load(p);
                          result.put(tex.name().toLowerCase(), tex);
                          log.info("Loaded wall texture: {} ({}x{})",
                              tex.name(), tex.width(), tex.height());
                      } catch (Exception e) {
                          log.warn("Skipped {} : {}", p.getFileName(), e.getMessage());
                      }
                  });
        }
        log.info("Loaded {}/{} wall textures from {}", result.size(),
            countWads(dir), dir.getFileName());
        return result;
    }

    // ── Décodage ──────────────────────────────────────────────────────────────

    /**
     * Décode un buffer .256wad brut.
     *
     * @param name   nom de la texture (pour les logs/cache)
     * @param raw    contenu brut du fichier
     * @param texW   largeur en pixels
     * @param texH   hauteur en pixels
     */
    public WadTextureData decode(String name, byte[] raw, int texW, int texH) {
        if (raw.length < SHADE_TABLE_BYTES)
            throw new IllegalArgumentException(
                name + " : fichier trop petit pour contenir la shade table (" +
                raw.length + " bytes, minimum " + SHADE_TABLE_BYTES + ")");

        ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);

        // ── 1. Shade table ────────────────────────────────────────────────────
        // 32 lignes × 32 entrées, chaque entrée = UWORD big-endian
        // On ne garde que le low byte = index dans la palette globale.
        // On résout immédiatement palette[index] → ARGB pour les deux usages :
        //   a) export PNG (pixels full-brightness)
        //   b) shade table compacte pour le renderer

        // shadeTable[row * 32 + entry] = ARGB
        int[] shadeTable = new int[SHADE_ROWS * ENTRIES_PER_ROW];

        for (int row = 0; row < SHADE_ROWS; row++) {
            for (int entry = 0; entry < ENTRIES_PER_ROW; entry++) {
                // draw_wall.s : move.b (a2, d1.w*2), (a3)
                // Lit le HIGH byte du WORD (offset pair) comme index palette
                // PAS le low byte comme on pensait initialement
                int palIdx = buf.get() & 0xFF;  // HIGH byte = index palette
                buf.get();                        // LOW byte  = ignore
                shadeTable[row * ENTRIES_PER_ROW + entry] = palette[palIdx];
            }
        }

        // AB3D2 convention : row 0 = FULL BRIGHTNESS, row 31 = darkest
        // (inverse de notre hypothese initiale)
        int brightRow  = 0;
        int brightBase = brightRow * ENTRIES_PER_ROW;

        // ── DEBUG : export texel brut en niveaux de gris (sans palette) ─────
        // Activer cette ligne pour verifier le chunk data sans dependre de la palette :
        // boolean rawGrayscale = true;
        boolean rawGrayscale = false;  // mode normal avec palette

        // ── 2. Chunk data (pixels 5 bits, column-major) ───────────────────────
        // numGroups = ⌈texW / 3⌉
        // Pour chaque groupe g → pour chaque ligne y → UWORD big-endian
        //   PACK0 : bits [ 4: 0] → colonne g*3
        //   PACK1 : bits [ 9: 5] → colonne g*3+1
        //   PACK2 : bits [14:10] → colonne g*3+2

        int numGroups  = (texW + 2) / 3;
        int chunkBytes = numGroups * texH * 2;

        if (raw.length < SHADE_TABLE_BYTES + chunkBytes) {
            // Tolerance : decoder autant de donnees que possible
            int availableChunk = raw.length - SHADE_TABLE_BYTES;
            if (availableChunk < 4) {
                throw new IllegalArgumentException(String.format(
                    "%s : fichier trop petit pour une texture %dx%d (besoin %d, dispo %d).",
                    name, texW, texH, SHADE_TABLE_BYTES + chunkBytes, raw.length));
            }
            // Ajuster numGroups et texH au maximum faisable
            log.warn("{} : taille insuffisante ({} vs {}) — decodage partiel",
                name, raw.length, SHADE_TABLE_BYTES + chunkBytes);
            int fullGroups = (texW + 2) / 3;
            numGroups = Math.min(numGroups, availableChunk / (texH * 2));
            if (numGroups <= 0) numGroups = 1;
        }

        int[] pixels = new int[texW * texH];

        for (int g = 0; g < numGroups; g++) {
            int baseX = g * 3;
            for (int y = 0; y < texH; y++) {
                int word = buf.getShort() & 0xFFFF;

                int t0 =  word        & 0x1F;  // PACK0
                int t1 = (word >>  5) & 0x1F;  // PACK1
                int t2 = (word >> 10) & 0x1F;  // PACK2

                // Écriture en row-major (y * texW + x) pour compatibilité OpenGL
                if (baseX     < texW) pixels[y * texW + baseX    ] = rawGrayscale ? gray(t0) : shadeTable[brightBase + t0];
                if (baseX + 1 < texW) pixels[y * texW + baseX + 1] = rawGrayscale ? gray(t1) : shadeTable[brightBase + t1];
                if (baseX + 2 < texW) pixels[y * texW + baseX + 2] = rawGrayscale ? gray(t2) : shadeTable[brightBase + t2];
            }
        }

        return new WadTextureData(name, texW, texH, pixels, shadeTable);
    }

    /** Convertit une valeur texel 5-bit (0-31) en couleur ARGB niveaux de gris. */
    private static int gray(int texel5bit) {
        int v = (texel5bit & 0x1F) * 8;  // 0-248
        return 0xFF000000 | (v << 16) | (v << 8) | v;
    }

    // ── Détection automatique des dimensions ──────────────────────────────────

    /**
     * Infere les dimensions d'une texture depuis la taille du fichier.
     *
     * <h3>Strategie</h3>
     * <ol>
     *   <li>Essaie d'abord les tailles connues de CANDIDATE_SIZES (exact)</li>
     *   <li>Recherche exhaustive : tous les W de 1..512, calcule H = chunkBytes/(groups*2),
     *       prend le resultat le plus proche d'un carre</li>
     * </ol>
     *
     * @param fileSize taille en bytes
     * @return {width, height} ou {@code null} si vraiment aucune solution
     */
    public static int[] detectDimensions(int fileSize) {
        // Les fichiers .256wad ont 2 bytes de terminateur a la fin
        // Format reel : shade(2048) + chunk_data + terminator(2)
        // Donc chunkBytes = fileSize - 2048 - 2 = fileSize - 2050
        int chunkBytes = fileSize - SHADE_TABLE_BYTES - 2;
        if (chunkBytes <= 0) {
            // Fallback sans terminateur (ancien format)
            chunkBytes = fileSize - SHADE_TABLE_BYTES;
            if (chunkBytes <= 0) return null;
        }

        // 1. Essai exact sur les candidates connues
        for (int[] dim : CANDIDATE_SIZES) {
            int groups = (dim[0] + 2) / 3;
            if (groups * dim[1] * 2 == chunkBytes)
                return dim.clone();
        }

        // 2. Recherche exhaustive : trouver (w, h) tel que groups*h*2 == chunkBytes
        //    Priorite : aspect ratio proche de 1:1 ou 2:1, dimensions >= 8
        int bestW = 0, bestH = 0;
        double bestScore = Double.MAX_VALUE;

        for (int w = 4; w <= 512; w++) {
            int g = (w + 2) / 3;
            if (g == 0) continue;
            int remainder = chunkBytes % (g * 2);
            if (remainder != 0) continue;
            int h = chunkBytes / (g * 2);
            if (h < 4 || h > 512) continue;
            // Preferer les dimensions proches de puissances de 2
            // et aspect ratio <= 4:1
            double ratio = (double) Math.max(w, h) / Math.min(w, h);
            if (ratio > 8) continue;
            // Score : preferer les largeurs puissances de 2 et hauteurs raisonnables
            double score = ratio + (isPowerOf2(w) ? 0 : 0.5) + (isPowerOf2(h) ? 0 : 0.5);
            if (score < bestScore) {
                bestScore = score;
                bestW = w; bestH = h;
            }
        }

        if (bestW > 0) {
            return new int[]{ bestW, bestH };
        }

        // 3. Dernier recours : truncation au plus pres
        //    Essayer de trouver des dimensions meme avec 1-4 bytes de tolerance
        for (int delta = 1; delta <= 8; delta++) {
            for (int sign : new int[]{-1, +1}) {
                int tryChunk = chunkBytes + sign * delta;
                if (tryChunk <= 0) continue;
                for (int[] dim : CANDIDATE_SIZES) {
                    int groups = (dim[0] + 2) / 3;
                    if (groups * dim[1] * 2 == tryChunk)
                        return dim.clone();
                }
            }
        }

        return null;
    }

    private static boolean isPowerOf2(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    /**
     * Décrit toutes les tailles candidates connues avec leur taille de fichier totale.
     * Utile pour diagnostiquer un fichier non reconnu.
     */
    public static String describeKnownSizes() {
        var sb = new StringBuilder("Tailles connues (.256wad) :\n");
        for (int[] d : CANDIDATE_SIZES) {
            int groups = (d[0] + 2) / 3;
            int total  = SHADE_TABLE_BYTES + groups * d[1] * 2;
            sb.append(String.format("  %4dx%-4d  → %6d bytes%n", d[0], d[1], total));
        }
        return sb.toString();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private static long countWads(Path dir) {
        try (var s = Files.list(dir)) {
            return s.filter(p -> p.toString().toLowerCase().endsWith(".256wad")).count();
        } catch (IOException e) {
            return -1;
        }
    }
}
