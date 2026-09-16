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
    public static Path root = findRoot();

    /**
     * Où sont les assets d'origine. Ils ne font plus partie du dépôt : la seule source est le
     * jeu de DISQUETTES ({@code adf/}), et le dossier lu ici n'est qu'un cache d'extraction.
     *
     * <p>Dans l'ordre : {@code -Dab3d2.assets} ; sinon un {@code medias/original} déjà présent
     * en remontant depuis le répertoire courant ; sinon un dossier {@code adf/} dont on EXTRAIT
     * les assets (dépacking {@code =SB=} compris) vers {@code medias/original}, une fois pour
     * toutes. {@code -Dab3d2.adf} force le dossier des images.
     */
    private static Path findRoot() {
        String prop = System.getProperty("ab3d2.assets");
        if (prop != null && !prop.isBlank()) {
            return Path.of(prop);
        }
        Path start = Path.of("").toAbsolutePath();
        String adfProp = System.getProperty("ab3d2.adf");
        Path adfDir = adfProp != null && !adfProp.isBlank() ? Path.of(adfProp) : null;
        Path cache = null;
        for (Path dir = start; dir != null; dir = dir.getParent()) {
            Path c = dir.resolve("medias").resolve("original");
            if (Files.isDirectory(c)) {
                return c;                              // cache déjà là
            }
            if (cache == null && Files.isDirectory(dir.resolve("adf"))) {
                adfDir = adfDir != null ? adfDir : dir.resolve("adf");
                cache = c;                             // on extraira à côté des disquettes
            }
        }
        if (adfDir != null && Files.isDirectory(adfDir)) {
            Path dest = cache != null ? cache
                    : adfDir.getParent().resolve("medias").resolve("original");
            return ab3d2.host.AdfAssets.ensureExtractedUnchecked(adfDir, dest);
        }
        return Path.of("medias", "original");          // défaut historique
    }

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
            byte[] res = fromClasspath(path);
            if (res != null) {
                int a = Mem.alloc(res.length);
                Mem.load(a, res);
                return a;
            }
        }
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

    /**
     * Les {@code incbin} EMBARQUÉS dans le dépôt, sous {@code resources/incbin/}.
     *
     * <p>Vingt et un des vingt-deux {@code incbin} du moteur ne sont sur AUCUNE disquette et ne
     * sont référencés nulle part dans {@code test.lnk} (la base GLF) : tables du rasterizer
     * ({@code bigsine}, {@code iterfile}, {@code guff}, {@code waterfile}, {@code shimmerfile}),
     * polices et chiffres, bordure d'écran, écran de menu, et les deux modules de fin. C'est
     * normal : l'assembleur les incorporait au binaire, ils n'ont jamais été livrés en fichiers.
     * Ils font donc partie du PROGRAMME, pas des données du jeu, et vivent dans le dépôt.
     *
     * <p>Seul {@code 256pal} est sur les disquettes ; {@code includes/newtitlepal} n'existe
     * nulle part et reste absent (le mode indulgent l'accepte).
     */
    private static byte[] fromClasspath(String path) {
        String clean = path.replace('\\', '/');
        int colon = clean.indexOf(':');
        if (colon >= 0) {
            clean = clean.substring(colon + 1);
        }
        try (var in = Assets.class.getResourceAsStream("/incbin/" + clean)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Le contenu d'un fichier, par la MÊME résolution que {@link #incbin} : le dossier d'assets
     * d'abord, puis les {@code incbin} embarqués dans le dépôt. Renvoie {@code null} si le
     * fichier est introuvable — à l'appelant de décider si c'est fatal.
     */
    public static byte[] bytes(String path) throws IOException {
        Path p = resolve(path);
        return p != null ? Files.readAllBytes(p) : fromClasspath(path);
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
