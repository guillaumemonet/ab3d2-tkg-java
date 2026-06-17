package ab3d2.menu;

import ab3d2.Mem;

/**
 * Données de ab3d2_source/menu/menunb.s — TRADUCTION PARTIELLE.
 *
 * Bloc menunb.s:1637-1642 (texte "insérer le disque" + ligne de volume
 * réécrite par IO_FlushQueue). Le reste (définitions de menus, code) suivra
 * avec la traduction de menunb.s.
 */
public final class MenunbData {

    public static final int mnu_askfordisktext;
    public static final int mnu_diskline;

    static {
        //                              "12345678901234567890"
        mnu_askfordisktext = Mem.dcStr("Please Insert Volume");
        Mem.dcB(1);
        Mem.dcB(1);
        mnu_diskline = Mem.dcStr("                    ");
        Mem.dcB(0);
    }

    private MenunbData() {
    }
}
