package ab3d2;

import ab3d2.host.CustomChips;

import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.bss.PlayerBss.Plr_MultiplayerType_b;
import static ab3d2.bss.PlayerBss.Plr1_Joystick_b;
import static ab3d2.bss.PlayerBss.Plr2_Joystick_b;
import static ab3d2.modules.RawKeyMacros.RAWKEY_P;

/**
 * Traduction littérale de ab3d2_source/pauseopts.s.
 *
 * Game_Pause : boucle de pause — attend l'appui (touche P ou bouton de tir port 1),
 * puis attend le relâchement. a5 = base de la table clavier (KeyMap_vb) ; RAWKEY_P(a5)
 * = octet d'état de la touche P ; btst #7,$bfe001 = bit 7 de CIA-A PRA (tir port 1,
 * actif bas) via CustomChips.ciaaPraBit7(). _ReadJoy1/2 = Cd32joy (stubs).
 *
 * NB : c'est une boucle d'attente d'entrée ; en jeu réel le clavier/CIA sont mis à
 * jour par interruption pendant la boucle.
 */
public final class Pauseopts {

    private static final int PLR_SLAVE = 's';   // PLR_SLAVE (two player slave)

    /** PAUSETEXT: dc.b '...* PAUSED *...' (80 caractères, sans terminateur). */
    public static final int PAUSETEXT;
    public static final int ENDPAUSETEXT;
    public static final int TOPPOPT;

    static {
        // 34 espaces + "* PAUSED *" + 36 espaces = 80 caractères
        String s = "                                  * PAUSED *                                    ";
        PAUSETEXT = Mem.allocTop();
        for (int i = 0; i < s.length(); ++i) {
            Mem.dcB(s.charAt(i));
        }
        ENDPAUSETEXT = Mem.allocTop();
        TOPPOPT = Mem.dcW(0);                              // TOPPOPT: dc.w 0
    }

    private Pauseopts() {
    }

    /** Lit la manette du joueur courant si activée (prologue de chaque tour de boucle). */
    private static void readPauseJoy() {
        if (Mem.b(Plr_MultiplayerType_b) == PLR_SLAVE) {   // cmp.b #PLR_SLAVE,Plr_MultiplayerType_b ; beq .otherk
            if (Mem.b(Plr2_Joystick_b) != 0) {             // tst.b Plr2_Joystick_b ; beq .NOJOY
                Cd32joy._ReadJoy2();                       // jsr _ReadJoy2
            }
        } else {
            if (Mem.b(Plr1_Joystick_b) != 0) {             // tst.b Plr1_Joystick_b ; beq .NOJOY
                Cd32joy._ReadJoy1();                       // jsr _ReadJoy1
            }
        }
    }

    /** Game_Pause — boucle de pause (attente appui puis relâchement). */
    public static void Game_Pause() {
        // .waitpress : attend l'appui (touche P OU tir)
        while (true) {
            readPauseJoy();
            if (Mem.b(KeyMap_vb + RAWKEY_P) != 0) {        // tst.b RAWKEY_P(a5) ; bne.s .unp
                break;
            }
            if (CustomChips.ciaaPraBit7()) {               // btst #7,$bfe001 ; bne.s .waitpress (bit positionné → boucle)
                continue;
            }
            break;                                         // bit à 0 (tir appuyé) → .unp
        }

        // .unp / .wr2 : attend le relâchement (P ET tir relâchés)
        while (true) {
            readPauseJoy();
            if (Mem.b(KeyMap_vb + RAWKEY_P) != 0) {        // tst.b RAWKEY_P(a5) ; bne.s .wr2 (P toujours appuyée)
                continue;
            }
            if (!CustomChips.ciaaPraBit7()) {              // btst #7,$bfe001 ; beq.s .wr2 (bit à 0 = tir appuyé)
                continue;
            }
            break;                                         // tout relâché → rts
        }
    }
}
