package ab3d2.host;

/**
 * Équivalent Java du couple "move.l sys_RecoveryStack,a7 ; bra Game_Quit" de
 * Sys_FatalError (system.s) : l'original restaure la pile au point de
 * récupération (longjmp) puis saute dans Game_Quit. En Java, cette exception
 * déroule la pile jusqu'au point d'entrée (Game_Start), qui exécutera
 * Game_Quit puis Sys_DisplayError.
 */
public class FatalError extends RuntimeException {

    public FatalError() {
        super("Sys_FatalError");
    }
}
