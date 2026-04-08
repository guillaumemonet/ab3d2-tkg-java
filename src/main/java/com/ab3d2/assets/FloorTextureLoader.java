package com.ab3d2.assets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge les textures de sol/plafond.
 *
 * <h2>Source prioritaire : IFF ILBM exportés (floors/iff/)</h2>
 * Les fichiers floor.1 à floor.16 sont des textures IFF Amiga avec leur
 * propre palette CMAP. Ils sont exportés en PNG par FloorIffLoader.
 * L'index floorNoise (1-16) correspond directement au numéro de fichier.
 *
 * <h2>Mapping floorNoise → whichTile</h2>
 * <pre>
 * C       = ((floorNoise - 1) % 4) + 1          ∈ {1,2,3,4}
 * S_start = ((floorNoise - 1) / 4) * 64          ∈ {0,64,128,192}
 * whichTile = C + S_start * 4
 * Ex : floorNoise=9 → C=1, S_start=128 → whichTile=513
 * </pre>
 *
 * <h2>Fallback : floortile binaire (floors/floortile + newtexturemaps)</h2>
 * Si les IFF ne sont pas disponibles, utilise le pipeline binaire.
 */
public class FloorTextureLoader {

    private static final Logger log = LoggerFactory.getLogger(FloorTextureLoader.class);

    public static final int TILE_W = 64;
    public static final int TILE_H = 64;

    // Shade table : rows 0-31 = glare, row 32+ = general
    private static final int SHADE_FLOOR_OFFSET = 256 * 32;

    // Données floortile binaire (fallback)
    private byte[] tileRaw;
    private int[]  shadeToArgb;

    private boolean iffAvailable = false;
    private boolean loaded       = false;

    private Path iffDir;  // dossier floors/iff/

    /** Cache whichTile → pixels ARGB 64x64 (floortile binaire) */
    private final Map<Integer, int[]> tileCache = new HashMap<>();

    public void load(Path floorsDir, int[] palette) throws IOException {
        // ── 1. Chercher les IFF exportés ────────────────────────────────────
        iffDir = floorsDir.resolve("iff");
        if (Files.exists(iffDir)) {
            // Compter combien de fichiers PNG de sol existent
            long count = Files.list(iffDir)
                .filter(p -> p.getFileName().toString().toLowerCase()
                    .startsWith("floor") && p.toString().endsWith(".png"))
                .count();
            if (count > 0) {
                iffAvailable = true;
        log.info("FloorTextureLoader: IFF disponibles. Mapping whichTile->floor.N : "
            + "wt=1→{}  wt=257→{}  wt=513→{}  wt=769→{}",
            whichTileToFloorNoise(1), whichTileToFloorNoise(257),
            whichTileToFloorNoise(513), whichTileToFloorNoise(769));
            }
        }

        // ── 2. Charger floortile binaire (fallback) ─────────────────────────
        Path tilePath  = floorsDir.resolve("floortile");
        Path shadePath = floorsDir.resolve("newtexturemaps");

        if (Files.exists(tilePath)) {
            tileRaw = Files.readAllBytes(tilePath);
            log.info("floortile charge : {} bytes", tileRaw.length);

            shadeToArgb = new int[256];
            if (Files.exists(shadePath)) {
                byte[] shadeRaw = Files.readAllBytes(shadePath);
                for (int i = 0; i < 256; i++) {
                    int idx    = SHADE_FLOOR_OFFSET + i;
                    int palIdx = (idx < shadeRaw.length) ? (shadeRaw[idx] & 0xFF) : i;
                    shadeToArgb[i] = (palIdx < palette.length) ? palette[palIdx] : 0xFF808080;
                }
            } else {
                for (int i = 0; i < 256; i++)
                    shadeToArgb[i] = (i < palette.length) ? palette[i] : 0xFF808080;
            }
        }

        loaded = true;
    }

    /**
     * Retourne les pixels ARGB (64×64) pour le vrai offset whichTile.
     *
     * Tente d'abord de trouver le fichier IFF correspondant
     * (via la conversion inverse whichTile → floorNoise).
     *
     * @param whichTile  offset brut extrait du zone graph (ex: 513)
     */
    public int[] getTile(int whichTile) {
        // 1. Essayer IFF via conversion whichTile → floorNoise
        if (iffAvailable) {
            int floorNoise = whichTileToFloorNoise(whichTile);
            if (floorNoise >= 1) {
                int[] px = loadIffTile(floorNoise);
                if (px != null) return px;
            }
        }

        // 2. Fallback floortile binaire (avec cache)
        int[] cached = tileCache.get(whichTile);
        if (cached != null) return cached;

        int[] px = decodeBinaryTile(whichTile);
        tileCache.put(whichTile, px);
        return px;
    }

    /**
     * Convertit un offset whichTile en numero de fichier floor.N (1-16).
     *
     * Formule derivee des donnees observees dans LEVEL_A :
     *   C       = (wt & 3) + 1            ∈ {1,2,3,4}  (canal)
     *   S_start = (wt - C + 1) >> 2       (en colonnes)
     *   section = S_start / 64            ∈ {0,1,2,3}
     *   floor.N = section * 2 + C + 5
     *
     * Verification :
     *   wt=1   : C=1, S_start=0,   section=0 → N = 0*2+1+5 = 6  ✓ (step)
     *   wt=513 : C=1, S_start=128, section=2 → N = 2*2+1+5 = 10 ✓ (zone3)
     */
    private static int whichTileToFloorNoise(int wt) {
        if (wt < 0) return -1;
        int C       = wt & 3;              // 0,1,2,3  (wt mod 4)
        int S_start = wt >> 2;             // 0,64,128,192
        int section = S_start / 64;        // 0..3
        int N       = section * 2 + C + 5; // ex wt=1:N=6, wt=513:N=10
        if (N < 1 || N > 16) return -1;
        return N;
    }

    /** Cache des PNG IFF (floorNoise → ARGB) */
    private final Map<Integer, int[]> iffCache = new HashMap<>();

    private int[] loadIffTile(int floorNoise) {
        int[] cached = iffCache.get(floorNoise);
        if (cached != null) return cached;

        // Chercher floor.N.png ou Floor.N.png
        String[] names = {
            "floor." + floorNoise + ".png",
            "Floor." + floorNoise + ".png",
            "floor." + floorNoise + ".plain.png",
        };
        for (String name : names) {
            Path p = iffDir.resolve(name);
            if (Files.exists(p)) {
                try {
                    BufferedImage img = ImageIO.read(p.toFile());
                    int w = img.getWidth();
                    int h = img.getHeight();
                    int[] px = new int[TILE_W * TILE_H];
                    for (int y = 0; y < TILE_H; y++)
                        for (int x = 0; x < TILE_W; x++)
                            px[y * TILE_W + x] = img.getRGB(
                                x % w, y % h);
                    iffCache.put(floorNoise, px);
                    log.debug("IFF tile {} chargee depuis {}", floorNoise, name);
                    return px;
                } catch (IOException e) {
                    log.warn("Erreur lecture IFF {}: {}", p, e.getMessage());
                }
            }
        }
        return null;  // fichier IFF non disponible
    }

    private int[] decodeBinaryTile(int whichTile) {
        int[] px = new int[TILE_W * TILE_H];
        if (tileRaw == null) {
            // Damier fallback
            for (int ty = 0; ty < TILE_H; ty++)
                for (int tx = 0; tx < TILE_W; tx++)
                    px[ty * TILE_W + tx] = (((tx>>3)+(ty>>3))%2==0)
                        ? 0xFF2A2820 : 0xFF181412;
        } else {
            for (int ty = 0; ty < TILE_H; ty++) {
                for (int tx = 0; tx < TILE_W; tx++) {
                    int d6      = ty * 256 + tx;
                    int byteOff = whichTile + d6 * 4;
                    int rawTexel = (byteOff >= 0 && byteOff < tileRaw.length)
                                   ? (tileRaw[byteOff] & 0xFF) : 0;
                    px[ty * TILE_W + tx] = (shadeToArgb != null)
                                           ? shadeToArgb[rawTexel] : 0xFF808080;
                }
            }
        }
        return px;
    }

    public boolean isLoaded()     { return loaded; }
    public boolean isIffAvailable() { return iffAvailable; }
}
