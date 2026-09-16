package ab3d2.host;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

/**
 * Récupération des disquettes au premier lancement.
 *
 * <p>Ce portage ne redistribue aucune donnée du jeu : il lui faut les cinq images {@code .adf}.
 * Quand elles manquent, on propose de les télécharger depuis <b>Dream17</b>, le site de
 * préservation du catalogue Amiga de Team17, qui les met à disposition sous forme d'une archive
 * ZIP. Rien n'est téléchargé sans un OUI explicite de l'utilisateur.
 *
 * <p>L'archive est décompressée dans le dossier {@code adf/} attendu par {@link AdfAssets} ; les
 * entrées sont aplaties (seul le nom de fichier compte) et seules celles en {@code .adf} sont
 * extraites.
 *
 * <p>Propriétés : {@code -Dab3d2.adfUrl=…} change la source, {@code -Dab3d2.noDownload} désactive
 * complètement la proposition (utile en CI).
 */
public final class AdfDownload {

    /** Page de téléchargement de Dream17 pour Alien Breed 3D II (redirige vers le ZIP). */
    public static final String DEFAULT_URL = "https://dream17.abime.net/download.php?id=50";

    private static final String UA = "AlienBreed3D2-TKG-Java (portage non commercial)";
    private static final int TIMEOUT_MS = 30_000;

    private AdfDownload() {
    }

    /** Y a-t-il au moins une image de disquette dans ce dossier ? */
    public static boolean hasDisks(Path adfDir) {
        if (!Files.isDirectory(adfDir)) {
            return false;
        }
        try (var s = Files.list(adfDir)) {
            return s.anyMatch(p -> p.getFileName().toString()
                    .toLowerCase(Locale.ROOT).endsWith(".adf"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * S'assure que {@code adfDir} contient les disquettes, en proposant de les télécharger si
     * besoin. Ne fait jamais rien sans accord de l'utilisateur.
     *
     * @return vrai si les disquettes sont là à la sortie
     */
    public static boolean ensureDisks(Path adfDir) {
        if (hasDisks(adfDir)) {
            return true;
        }
        if (System.getProperty("ab3d2.noDownload") != null) {
            return false;
        }
        if (!ask(adfDir)) {
            return false;
        }
        String url = System.getProperty("ab3d2.adfUrl", DEFAULT_URL);
        try {
            int n = download(url, adfDir);
            info(n + " disquette(s) installée(s) dans\n" + adfDir);
            return n > 0;
        } catch (IOException e) {
            error("Le téléchargement a échoué :\n" + e.getMessage()
                    + "\n\nPlace les fichiers .adf toi-même dans :\n" + adfDir);
            return false;
        }
    }

    /**
     * Télécharge l'archive et en extrait les {@code .adf}. Renvoie le nombre d'images écrites.
     */
    public static int download(String url, Path adfDir) throws IOException {
        Files.createDirectories(adfDir);
        System.out.println("[ADF] telechargement depuis " + url);
        HttpURLConnection c = open(url);
        List<String> written = new ArrayList<>();
        try (InputStream in = c.getInputStream();
             ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    continue;
                }
                // On aplatit : le ZIP peut ranger les images dans un sous-dossier.
                String name = e.getName().replace('\\', '/');
                name = name.substring(name.lastIndexOf('/') + 1);
                if (!name.toLowerCase(Locale.ROOT).endsWith(".adf")) {
                    continue;
                }
                Path out = adfDir.resolve(name);
                Files.copy(zip, out, StandardCopyOption.REPLACE_EXISTING);
                written.add(name);
                System.out.printf("[ADF]   %-60s %d octets%n", name, Files.size(out));
            }
        }
        if (written.isEmpty()) {
            throw new IOException("aucun fichier .adf dans l'archive telechargee");
        }
        return written.size();
    }

    /** Ouvre la connexion en suivant les redirections, y compris un changement de protocole. */
    private static HttpURLConnection open(String url) throws IOException {
        String current = url;
        for (int hop = 0; hop < 5; hop++) {
            URL u = URI.create(current).toURL();
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestProperty("User-Agent", UA);
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setInstanceFollowRedirects(false);
            int code = c.getResponseCode();
            if (code / 100 == 3) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) {
                    throw new IOException("redirection sans Location (HTTP " + code + ")");
                }
                // Location peut être relatif.
                current = URI.create(current).resolve(loc).toString();
                continue;
            }
            if (code != 200) {
                c.disconnect();
                throw new IOException("HTTP " + code + " sur " + current);
            }
            return c;
        }
        throw new IOException("trop de redirections depuis " + url);
    }

    // ------------------------------------------------------------------ dialogue

    private static final String MESSAGE = """
            Les données du jeu sont absentes.

            Ce portage ne contient aucune donnée d'Alien Breed 3D II : il lui faut les
            cinq disquettes du jeu (fichiers .adf).

            Les télécharger depuis Dream17, le site de préservation du catalogue Amiga
            de Team17 ? (environ 3,4 Mo)

            Elles seront installées dans :
            %s

            Si tu possèdes déjà les images, réponds NON et copie-les toi-même dans ce
            dossier.""";

    private static boolean ask(Path adfDir) {
        String msg = MESSAGE.formatted(adfDir.toAbsolutePath());
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println(msg);
            System.out.print("Telecharger ? [o/N] ");
            try {
                int ch = System.in.read();
                return ch == 'o' || ch == 'O' || ch == 'y' || ch == 'Y';
            } catch (IOException e) {
                return false;
            }
        }
        return JOptionPane.showConfirmDialog(null, msg,
                "Alien Breed 3D II — données du jeu",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    private static void info(String msg) {
        System.out.println("[ADF] " + msg.replace('\n', ' '));
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, msg, "Alien Breed 3D II",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void error(String msg) {
        System.err.println("[ADF] " + msg.replace('\n', ' '));
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, msg, "Alien Breed 3D II",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Outil : {@code gradle -p java fetchDisks}. */
    public static void main(String[] args) throws IOException {
        Path dir = Path.of(System.getProperty("ab3d2.adf", "adf"));
        if (hasDisks(dir)) {
            System.out.println("disquettes deja presentes dans " + dir.toAbsolutePath());
            return;
        }
        int n = download(System.getProperty("ab3d2.adfUrl", DEFAULT_URL), dir);
        System.out.println(n + " disquette(s) dans " + dir.toAbsolutePath());
    }
}
