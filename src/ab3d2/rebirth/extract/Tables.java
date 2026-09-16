package ab3d2.rebirth.extract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Tables numériques d'origine nécessaires à la SIMULATION du remake (pas au rendu).
 *
 * <p>{@code bigsine} = SinCosTable_vw (data/tables_data.s) : 8192 mots big-endian, soit deux
 * cycles complets de 4096 entrées ; valeur = {@code round(32767 * sin(2π·i/4096))}. Le remake
 * doit utiliser CETTE table (et pas un {@code Math.sin}) : la trajectoire du joueur dépend de
 * ses arrondis exacts. Copiée telle quelle en {@code assets/tables/sincos.bin}.
 */
public final class Tables {

    private Tables() {
    }

    public static void extract(Path out) throws IOException {
        Path dir = out.resolve("tables");
        Files.createDirectories(dir);
        Path src = ab3d2.Assets.root.resolve("bigsine");
        Path dst = dir.resolve("sincos.bin");
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[tables] sincos.bin (" + Files.size(dst) + " o) ← " + src);

        // guff : 16 positions verticales × 7 rangées × 16 directions — la grille de luminosité
        // appliquée aux sprites éclairés (drawBitmapLighted, Objdrawhires.java:1162).
        Path guff = ab3d2.Assets.root.resolve("INCLUDES").resolve("guff");
        if (Files.isRegularFile(guff)) {
            Path gdst = dir.resolve("guff.bin");
            Files.copy(guff, gdst, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[tables] guff.bin (" + Files.size(gdst) + " o) ← " + guff);
        }
    }
}
