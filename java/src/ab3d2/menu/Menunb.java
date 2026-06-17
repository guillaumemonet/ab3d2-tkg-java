package ab3d2.menu;

import ab3d2.Mem;
import ab3d2.MenuNb;

import static ab3d2.data.MenunbData.mnu_row;
import static ab3d2.data.MenunbData.mnu_currentsel;
import static ab3d2.data.MenunbData.mnu_frameptr;
import static ab3d2.data.MenunbData.mnu_errcursanim;

/**
 * Traduction de la boucle de menu interne menu/menunb.s::mnu_domenu (moteur à types
 * d'options : 0=rien, 1=sous-menu, 2=sortie, 3=routine, 4=slider, 5=cycler, 6=saut,
 * 7=changement de menu, 8=touche brute, 9/10=load/save niveau).
 *
 * NB : le jeu ACTIF n'utilise pas mnu_domenu (controlloop passe par game_OpenMenu/
 * game_CheckMenu et recalcule la sélection). mnu_domenu sert aux menus legacy (avec
 * cyclers/sliders), désactivés dans l'original (« mnu_start shows the wrong menu »).
 * Porté pour la complétude + démonstration des cyclers/sliders.
 */
public final class Menunb {

    /** Adresse de la définition du menu "ask for disk" (menunb.s:1527) — posée lors de la traduction. */
    public static int mnu_askfordisk;

    private Menunb() {
    }

    /** mnu_domenu : boucle d'affichage/interaction d'un menu (a0 = définition). */
    public static void mnu_domenu(int a0) {
        while (true) {                                       // .redraw
            MenuNb.mnu_openmenu(a0);
            boolean redraw = false;
            while (true) {                                   // .loop
                MenuNb.mnu_update(a0);
                int d0 = MenuNb.mnu_waitmenu();              // d0 = sélection ; mnu_waitFlag = d1
                int d1 = MenuNb.mnu_waitFlag;
                int items = Mem.uw(a0 + 14);
                int sel = items == 0 ? 0 : Integer.remainderUnsigned(Mem.uw(mnu_row), items);
                Mem.ww(mnu_currentsel, sel);                 // mnu_currentsel = mnu_row % items

                if (d1 != 0) {                               // flèche gauche/droite (slider/cycler)
                    if (d1 == 42) { adjust(a0, d0, +1); continue; } // .left
                    if (d1 == 41) { adjust(a0, d0, -1); continue; } // .right
                }
                if (d0 == -1) {                              // Esc → .exit
                    return;
                }
                int type = Mem.l(a0 + 16 + d0 * 8);          // 16(a0,d0*8) = type d'option
                if (type == 0) {                             // rien
                    continue;
                } else if (type == 1) {                      // sous-menu
                    mnu_domenu(Mem.l(a0 + 20 + d0 * 8));
                    redraw = true; break;
                } else if (type == 2) {                      // sortie
                    return;
                } else if (type == 4) {                      // slider (gauche = +pas)
                    adjust(a0, d0, +1); continue;
                } else if (type == 5) {                      // cycler (gauche = +1)
                    adjust(a0, d0, +1); continue;
                } else if (type == 7) {                      // changement de menu
                    a0 = Mem.l(a0 + 20 + d0 * 8);
                    redraw = true; break;
                }
                // types 3 (routine), 6 (saut), 8 (touche brute), 9/10 (load/save niveau) :
                // pointent vers des routines ASM / handlers non utilisés par les menus
                // démo → non portés ici (le flux controlloop les gère séparément).
                Mem.wl(mnu_frameptr, mnu_errcursanim);       // .wrong
            }
            if (!redraw) {
                return;
            }
        }
    }

    /**
     * Ajuste le slider/cycler de l'item sélectionné. dir=+1 (gauche, .left/.leftsl),
     * -1 (droite, .right/.rightsl). Slider : valeur(10) ± pas(8). Cycler : valeur(6) ± 1.
     */
    private static void adjust(int a0, int sel, int dir) {
        int type = Mem.l(a0 + 16 + sel * 8);
        int data = Mem.l(a0 + 20 + sel * 8);
        if (type == 4) {                                     // slider
            int valPtr = Mem.l(data + 10);
            int step = Mem.uw(data + 8);
            Mem.ww(valPtr, Mem.uw(valPtr) + dir * step);
        } else if (type == 5) {                              // cycler
            int valPtr = Mem.l(data + 6);
            Mem.ww(valPtr, Mem.uw(valPtr) + dir);
        } else {
            Mem.wl(mnu_frameptr, mnu_errcursanim);           // .wrong
        }
    }
}
