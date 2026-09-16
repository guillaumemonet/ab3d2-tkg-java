package ab3d2.tools;

import ab3d2.host.AdfAssets;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extrait les assets des disquettes vers un dossier, dépacking {@code =SB=} compris.
 *
 * <p>Le jeu le fait tout seul au premier lancement (cf. {@code ab3d2.Assets}) ; cet outil sert
 * à le forcer, par exemple pour reconstruire le cache après l'avoir supprimé.
 *
 * <pre>
 * gradle -p java depack                 # ../adf -> ../medias/original
 * gradle -p java depack -Pout=/tmp/x    # ailleurs
 * </pre>
 */
public final class Depack {

    private Depack() {
    }

    public static void main(String[] args) throws Exception {
        Path adf = Path.of(System.getProperty("ab3d2.adf", "adf"));
        String out = System.getProperty("ab3d2.assets");
        Path dest = out != null && !out.isBlank()
                ? Path.of(out)
                : adf.toAbsolutePath().getParent().resolve("medias").resolve("original");
        Files.createDirectories(dest);
        AdfAssets.ensureExtracted(adf, dest);
        System.out.println("assets prets dans " + dest.toAbsolutePath());
    }
}
