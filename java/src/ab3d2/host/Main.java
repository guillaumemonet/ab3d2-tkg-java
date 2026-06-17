package ab3d2.host;

import ab3d2.bss.Bss;
import ab3d2.c.MainC;
import ab3d2.data.DataSections;

/**
 * Point d'entrée du programme porté.
 *
 * Initialise la disposition mémoire (sections BSS/DATA, dans l'ordre fixé par
 * hires.s) puis appelle MainC.run, équivalent du main() C (c/main.c).
 *
 * Tant que la boucle de frame (Phase 2) n'est pas branchée, MainC.run lèvera
 * sur le stub hires.s::_startup ; utiliser ab3d2.host.DisplayTest pour valider
 * l'afficheur en attendant.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Bss.init();
        DataSections.init();
        int rval = MainC.run(args);
        System.exit(rval);
    }
}
