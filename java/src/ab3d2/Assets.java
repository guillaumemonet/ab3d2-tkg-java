package ab3d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Résolution des fichiers d'assets pour les directives incbin et les
 * chargements à l'exécution.
 *
 * Le build original (Makefile) résout les incbin avec -I../media et
 * -I../media/includes ; à l'exécution le jeu ouvre des chemins préfixés
 * "ab3:" (assign AmigaOS vers la racine du jeu). Ici les deux sont mappés
 * sur le dossier des médias dépackés (par défaut medias/original), avec
 * recherche insensible à la casse (le FS Amiga l'était).
 */
public final class Assets {

    /** Racine des assets — modifiable avant l'init (option de lancement). */
    public static Path root = Path.of("medias", "original");

    /**
     * Mode tolérant : si un fichier incbin est introuvable, émet un avertissement
     * et n'alloue rien (adresse valide mais bloc vide) au lieu de stopper.
     * Permet de démarrer le port avant que tous les assets soient en place.
     */
    public static boolean lenient = true;

    private Assets() {
    }

    /**
     * incbin "path" : charge le fichier dans Mem à l'adresse courante
     * d'allocation et renvoie cette adresse.
     */
    public static int incbin(String path) {
        Path p = resolve(path);
        if (p == null) {
            String msg = "incbin introuvable: " + path + " (racine " + root + ")";
            if (!lenient) {
                throw new IllegalStateException(msg);
            }
            System.err.println("[Assets] AVERTISSEMENT " + msg);
            return Mem.alloc(0);
        }
        try {
            byte[] data = Files.readAllBytes(p);
            int addr = Mem.alloc(data.length);
            Mem.load(addr, data);
            return addr;
        } catch (IOException e) {
            throw new IllegalStateException("incbin: erreur de lecture " + p, e);
        }
    }

    /** Charge un fichier (chemin style Amiga, ex. "ab3:levels/level_a/twolev.bin"). */
    public static byte[] read(String amigaPath) throws IOException {
        Path p = resolve(amigaPath);
        if (p == null) {
            throw new IOException("fichier introuvable: " + amigaPath + " (racine " + root + ")");
        }
        return Files.readAllBytes(p);
    }

    /**
     * Résout un chemin Amiga/incbin vers un fichier réel :
     *  - retire le préfixe d'assign "ab3:" ;
     *  - essaie root/chemin, puis root/includes/nom (équivalent -I media/includes) ;
     *  - chaque segment est comparé sans tenir compte de la casse.
     */
    public static Path resolve(String path) {
        String clean = path;
        int colon = clean.indexOf(':');
        if (colon >= 0) {
            clean = clean.substring(colon + 1); // retire l'assign (ab3:)
        }
        clean = clean.replace('\\', '/');

        Path direct = findCaseInsensitive(root, clean.split("/"));
        if (direct != null) {
            return direct;
        }
        // -I../media/includes : un incbin "waterfile" peut être dans includes/
        String base = clean.substring(clean.lastIndexOf('/') + 1);
        Path inIncludes = findCaseInsensitive(root, new String[]{"includes", base});
        if (inIncludes != null) {
            return inIncludes;
        }
        // Les assets dépackés sont réorganisés en sous-dossiers (walls/, floors/, …) qui ne
        // correspondent pas aux assigns Amiga d'origine (WALLINC:, etc.). Dernier recours :
        // chercher le fichier par son NOM DE BASE n'importe où sous root.
        return findByBasename(base);
    }

    private static Map<String, Path> basenameIndex; // index paresseux nom-de-base → fichier

    private static Path findByBasename(String base) {
        if (basenameIndex == null) {
            basenameIndex = new HashMap<>();
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile).forEach(p ->
                        basenameIndex.putIfAbsent(p.getFileName().toString().toLowerCase(), p));
            } catch (IOException e) {
                // index vide : on retombera sur le mode tolérant
            }
        }
        return basenameIndex.get(base.toLowerCase());
    }

    private static Path findCaseInsensitive(Path dir, String[] segments) {
        Path current = dir;
        for (String seg : segments) {
            if (seg.isEmpty()) {
                continue;
            }
            if (!Files.isDirectory(current)) {
                return null;
            }
            Path next = null;
            try (var stream = Files.newDirectoryStream(current)) {
                for (Path candidate : stream) {
                    if (candidate.getFileName().toString().equalsIgnoreCase(seg)) {
                        next = candidate;
                        break;
                    }
                }
            } catch (IOException e) {
                return null;
            }
            if (next == null) {
                return null;
            }
            current = next;
        }
        return Files.isRegularFile(current) ? current : null;
    }
}
