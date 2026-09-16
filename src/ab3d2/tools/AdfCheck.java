package ab3d2.tools;

import ab3d2.host.Adf;
import ab3d2.host.AdfAssets;
import ab3d2.host.SbDepack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Valide le dépacking {@code =SB=} : monte les disquettes, dépacke tout, et compare au corpus
 * de référence déjà dépacké.
 *
 * <pre>
 * gradle -p java adfCheck                       # disquettes = ../adf, référence = ../medias/original
 * gradle -p java adfCheck -Padf=… -Pref=…
 * </pre>
 *
 * <p>Deux niveaux de vérité, et il faut les distinguer :
 * <ul>
 *   <li><b>tout doit décoder</b> — chaque flux doit produire exactement le nombre d'octets que
 *       son en-tête annonce, sans exception. C'est le vrai signal.</li>
 *   <li><b>comparaison au corpus</b> — seulement là où la MÊME variante existe des deux côtés.
 *       Un même nom de fichier existe souvent en version 2 Mo et 4 Mo (disquettes 1 et 3) : un
 *       écart n'est alors pas un défaut de décodage mais deux fichiers différents. On le vérifie
 *       en signalant à part les tailles qui diffèrent.</li>
 * </ul>
 */
public final class AdfCheck {

    private AdfCheck() {
    }

    public static void main(String[] args) throws Exception {
        Path adfDir = Path.of(System.getProperty("ab3d2.adf", "../adf"));
        Path ref = Path.of(System.getProperty("ab3d2.ref", "../medias/original"));
        System.out.println("=== AdfCheck : " + adfDir + "  vs  " + ref + " ===");

        Map<String, List<Path>> byName = indexByName(ref);

        int total = 0;
        int packed = 0;
        int decoded = 0;
        List<String> problems = new ArrayList<>();
        // La VUE MONTEE : une disquette montee plus tard ecrase le meme chemin. AmigaDOS
        // IGNORE LA CASSE — /includes/ (disquette 1) et /Includes/ (disquette 3) sont le meme
        // repertoire — donc la cle est en minuscules, sinon la variante 4 Mo n'ecrase rien.
        Map<String, byte[]> view = new java.util.LinkedHashMap<>();

        for (Path disk : AdfAssets.disks(adfDir)) {
            Adf adf = new Adf(disk);
            for (Adf.Entry e : adf.list()) {
                if (e.dir()) {
                    continue;
                }
                total++;
                byte[] raw = adf.read(e);
                byte[] data = raw;
                if (SbDepack.isPacked(raw)) {
                    packed++;
                    int want = SbDepack.unpackedLength(raw);
                    try {
                        data = SbDepack.unpack(raw);
                    } catch (RuntimeException ex) {
                        problems.add("DECODE " + e.path() + " : " + ex);
                        continue;
                    }
                    if (data.length != want) {
                        problems.add("TAILLE " + e.path() + " : " + data.length + " != " + want);
                        continue;
                    }
                    decoded++;
                }
                view.put(e.path().toLowerCase(Locale.ROOT), data);
            }
        }

        int same = 0;
        int variant = 0;
        int contentDiff = 0;
        int noRef = 0;
        for (var en : view.entrySet()) {
            String path = en.getKey();
            // Deux sous-arbres ne sont PAS ceux que le jeu 4 Mo utilise : la reserve 2 Mo et
            // les donnees de l'editeur. Ils portent les memes noms avec un autre contenu.
            if (path.startsWith("/tkg-2mb/") || path.startsWith("/edit/")) {   // cles en minuscules
                continue;
            }
            byte[] data = en.getValue();
            String base = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            List<Path> cands = byName.get(base);
            if (cands == null) {
                noRef++;
                continue;
            }
            boolean matched = false;
            boolean sizeSeen = false;
            for (Path c : cands) {
                byte[] r = Files.readAllBytes(c);
                if (r.length == data.length) {
                    sizeSeen = true;
                    if (java.util.Arrays.equals(r, data)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched) {
                same++;
            } else if (sizeSeen) {
                contentDiff++;
                problems.add("CONTENU " + path);
            } else {
                variant++;
            }
        }

        System.out.printf("%nfichiers sur les disquettes  : %d%n", total);
        System.out.printf("  packes =SB=                : %d%n", packed);
        System.out.printf("  decodes sans erreur        : %d   <- l'invariant%n", decoded);
        System.out.printf("%nvue montee (hors TKG-2MB et EDIT) : %d fichiers%n",
                same + variant + contentDiff + noRef);
        System.out.printf("  identiques a la reference  : %d%n", same);
        System.out.printf("  autre version (taille !=)  : %d%n", variant);
        System.out.printf("  meme taille, contenu !=    : %d   <- anormal%n", contentDiff);
        System.out.printf("  absents de la reference    : %d%n", noRef);
        for (String p : problems) {
            System.out.println("  ! " + p);
        }
        boolean ok = decoded == packed && problems.isEmpty();
        System.out.println(ok ? "-> PASS" : "-> ECHEC");
        if (!ok) {
            System.exit(1);
        }
    }

    /** Index nom-de-fichier (minuscules) → tous les fichiers de la référence qui le portent. */
    private static Map<String, List<Path>> indexByName(Path root) throws Exception {
        Map<String, List<Path>> out = new HashMap<>();
        if (!Files.isDirectory(root)) {
            System.out.println("(pas de reference en " + root + " : comparaison desactivee)");
            return out;
        }
        try (var s = Files.walk(root)) {
            s.filter(Files::isRegularFile).forEach(p ->
                    out.computeIfAbsent(p.getFileName().toString().toLowerCase(Locale.ROOT),
                            k -> new ArrayList<>()).add(p));
        }
        return out;
    }
}
