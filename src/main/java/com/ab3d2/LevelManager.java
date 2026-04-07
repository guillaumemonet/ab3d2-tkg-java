package com.ab3d2;

import com.ab3d2.core.level.GraphicsBinaryParser;
import com.ab3d2.core.level.LevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire de niveaux.
 *
 * <h2>Fichiers chargés par niveau</h2>
 * <pre>
 * resources/levels/LEVEL_X/
 *   twolev.bin        ← TLBT header (starts joueur), control points, edges, points
 *   twolev.graph.bin  ← TLGT header (portes/lifts/switches) + table pointeurs zones
 *   twolev.clips      ← clips sprites (parsé séparément)
 *   twolev.map        ← collision map sol (parsé séparément)
 *   twolev.flymap     ← collision map jetpack (parsé séparément)
 * </pre>
 */
public class LevelManager {

    private static final Logger log = LoggerFactory.getLogger(LevelManager.class);

    private final Path levelsRoot;
    private final GraphicsBinaryParser assembler = new GraphicsBinaryParser();
    private final Map<String, LevelData> cache = new HashMap<>();

    /**
     * @param assetsRoot répertoire racine des ressources (contient {@code levels/})
     */
    public LevelManager(Path assetsRoot) {
        this.levelsRoot = assetsRoot.resolve("levels");
        log.info("LevelManager root: {}", levelsRoot.toAbsolutePath());
    }

    /**
     * Charge un niveau par sa lettre ("A"..."P").
     * Met le résultat en cache (appels successifs retournent le même objet).
     *
     * @param levelLetter lettre du niveau, insensible à la casse
     * @return données du niveau complètement assemblées
     * @throws IOException si les fichiers sont introuvables ou illisibles
     */
    public LevelData load(String levelLetter) throws IOException {
        String key = levelLetter.toUpperCase();
        if (cache.containsKey(key)) {
            log.debug("Level {} trouvé dans le cache", key);
            return cache.get(key);
        }

        Path levelDir = levelsRoot.resolve("LEVEL_" + key);
        if (!Files.isDirectory(levelDir)) {
            throw new IOException("Répertoire de niveau introuvable : " + levelDir);
        }

        Path binPath   = levelDir.resolve("twolev.bin");
        Path graphPath = levelDir.resolve("twolev.graph.bin");

        if (!Files.exists(binPath))
            throw new IOException("twolev.bin introuvable : " + binPath);
        if (!Files.exists(graphPath))
            throw new IOException("twolev.graph.bin introuvable : " + graphPath);

        log.info("Chargement du niveau {} depuis {}", key, levelDir);
        LevelData data = assembler.load(binPath, graphPath, key);
        cache.put(key, data);
        log.info("Niveau {} chargé : {}", key, data);
        return data;
    }

    /** Retourne le niveau en cache sans le recharger, ou {@code null}. */
    public LevelData getCached(String levelLetter) {
        return cache.get(levelLetter.toUpperCase());
    }

    /** Vide le cache. */
    public void clearCache() {
        cache.clear();
        log.debug("Cache de niveaux vidé");
    }

    /** Liste les niveaux disponibles. */
    public String[] listAvailableLevels() {
        if (!Files.isDirectory(levelsRoot)) return new String[0];
        try (var stream = Files.list(levelsRoot)) {
            return stream
                .filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                .filter(n -> n.startsWith("LEVEL_"))
                .map(n -> n.substring("LEVEL_".length()))
                .sorted()
                .toArray(String[]::new);
        } catch (IOException e) {
            log.warn("Impossible de lister les niveaux : {}", e.getMessage());
            return new String[0];
        }
    }

    public Path getLevelsRoot() { return levelsRoot; }
}
