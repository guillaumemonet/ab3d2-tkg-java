package com.ab3d2.assets;

import com.ab3d2.core.math.Tables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Charge et gere les textures de murs AB3D2 (.256wad).
 *
 * <h2>Mapping texIndex → fichier</h2>
 * Les 16 slots (indices 0-15) correspondent dans l'ordre alphabetique aux 14
 * fichiers disponibles dans le dossier wallinc/ :
 * <pre>
 *  0 = alienredwall
 *  1 = brownpipes
 *  2 = brownspeakers
 *  3 = brownstonestep
 *  4 = brownwithyellowstripes
 *  5 = chevrondoor
 *  6 = gieger
 *  7 = hullmetal
 *  8 = redhullmetal
 *  9 = rocky
 * 10 = steampunk
 * 11 = stonewall
 * 12 = technolights
 * 13 = technotritile
 * 14 = (slot vide → fallback)
 * 15 = (slot vide → fallback)
 * </pre>
 *
 * Note : le mapping exact est dans le GLFT (test.lnk), non disponible. Cet
 * ordre alphabetique est une approximation.
 */
public class WallTextureManager {

    private static final Logger log = LoggerFactory.getLogger(WallTextureManager.class);

    public static final int NUM_WALL_TEXTURES = 16;

    // Ordre EXACT depuis le GLFT (test.lnk, champ GLFT_WallGFXNames_l)
    // Confirme en lisant les noms dans le fichier binaire test.lnk :
    //  0 = STONEWALL        4 = BROWNSPEAKERS   8 = ALIENREDWALL
    //  1 = BROWNPIPES       5 = CHEVRONDOOR     9 = GIEGER
    //  2 = HULLMETAL        6 = TECHNOLIGHTS   10 = ROCKY
    //  3 = TECHNOTRITILE    7 = REDHULLMETAL   11 = STEAMPUNK
    //                                          12 = BROWNSTONESTEP
    private static final String[] TEXTURE_NAMES = {
        "stonewall", //  0
        "brownpipes", //  1
        "hullmetal", //  2
        "technotritile", //  3
        "brownspeakers", //  4
        "chevrondoor", //  5
        "technolights", //  6
        "redhullmetal", //  7
        "alienredwall", //  8
        "gieger", //  9
        "rocky", // 10
        "steampunk", // 11
        "brownstonestep", // 12
        null, // 13 - vide (confirme GLFT)
        null, // 14 - vide
        null, // 15 - vide
    };

    // ── Etat ─────────────────────────────────────────────────────────────────
    private final WadTextureData[] textures = new WadTextureData[NUM_WALL_TEXTURES];
    private WadTextureData fallbackTexture;

    // ── Chargement ────────────────────────────────────────────────────────────
    /**
     * Charge toutes les textures depuis le dossier walls/ des resources.
     *
     * @param wallsDir chemin vers le dossier contenant les .256wad
     * @param palette palette 256 couleurs ARGB (depuis AssetManager ou
     * palette.bin)
     */
    public void loadAll(Path wallsDir, int[] palette) throws IOException {
        WallTextureExtractor extractor = new WallTextureExtractor(palette);

        // Texture de fallback : damier magenta/noir pour diagnostiquer les slots manquants
        fallbackTexture = buildFallback();

        int loaded = 0;
        for (int i = 0; i < TEXTURE_NAMES.length; i++) {
            if (TEXTURE_NAMES[i] == null) {
                textures[i] = fallbackTexture;
                continue;
            }
            Path p = wallsDir.resolve(TEXTURE_NAMES[i] + ".256wad");
            if (!Files.exists(p)) {
                log.warn("Texture manquante : {} (slot {})", p.getFileName(), i);
                textures[i] = fallbackTexture;
                continue;
            }
            try {
                textures[i] = extractor.load(p);
                loaded++;
                log.debug("Texture {} : {} ({}x{})", i, textures[i].name(),
                        textures[i].width(), textures[i].height());
            } catch (Exception e) {
                log.warn("Erreur chargement texture {} : {}", TEXTURE_NAMES[i], e.getMessage());
                textures[i] = fallbackTexture;
            }
        }
        log.info("WallTextureManager : {}/{} textures chargees depuis {}", loaded, 14, wallsDir);
    }

    // ── Acces ─────────────────────────────────────────────────────────────────
    /**
     * Retourne la texture pour un indice donne (0-15). Retourne la texture de
     * fallback si l'indice est invalide ou manquant.
     */
    public WadTextureData get(int index) {
        if ((index & 0x8000) != 0) {
            return null;      // portail -> skip
        }
        if (index >= NUM_WALL_TEXTURES) {
            return fallbackTexture;  // per-level -> gris
        }
        WadTextureData t = textures[index];
        return (t != null) ? t : fallbackTexture;
    }

    /**
     * Nom de la texture pour un indice donne.
     */
    public String getName(int index) {
        if (index < 0 || index >= TEXTURE_NAMES.length) {
            return "unknown";
        }
        String n = TEXTURE_NAMES[index];
        return (n != null) ? n : "fallback_" + index;
    }

    public boolean isLoaded() {
        return textures[0] != null;
    }

    // ── Fallback ──────────────────────────────────────────────────────────────
    private static WadTextureData buildFallback() {
        int w = 32, h = 32;
        int[] px = new int[w * h];
        int[] st = new int[WadTextureData.SHADE_TABLE_ENTRIES];
        // Damier magenta/sombre
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean checker = ((x / 4) + (y / 4)) % 2 == 0;
                px[y * w + x] = 0xFF505050;  // gris neutre
            }
        }
        Arrays.fill(st, 0xFFFF00FF);
        return new WadTextureData("fallback", w, h, px, st);
    }

    /**
     * Dump du contenu pour debug.
     */
    public String dump() {
        StringBuilder sb = new StringBuilder("WallTextureManager:\n");
        for (int i = 0; i < NUM_WALL_TEXTURES; i++) {
            WadTextureData t = textures[i];
            if (t == null || t == fallbackTexture) {
                sb.append(String.format("  [%2d] FALLBACK%n", i));
            } else {
                sb.append(String.format("  [%2d] %-30s  %dx%d%n", i, t.name(),
                        t.width(), t.height()));
            }
        }
        return sb.toString();
    }
}
