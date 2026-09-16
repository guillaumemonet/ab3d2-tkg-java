package ab3d2.c;

import ab3d2.Mem;

import static ab3d2.bss.VidBss.Vid_ScreenMode;
import static ab3d2.bss.VidBss.Vid_isRTG;

/**
 * Traduction de ab3d2_source/c/main.c — point d'entrée du programme.
 *
 * main() est de l'orchestration de bring-up OS : ouverture des librairies,
 * détermination du mode d'affichage (CLI SCREENMODE/N ou ASL), détection RTG,
 * puis appel de startup() (le moteur, hires.s _startup), enfin fermeture.
 *
 * Les primitives non portées (Sys_OpenLibs/CloseLibs = SystemC stubs ; ReadArgs/
 * FreeArgs/PutStr = dos.library ; GetScreenMode = screen.c ; IsCyberModeID +
 * CyberGfxBase = cybergraphics.library ; startup = hires.s) sont conservées en
 * stubs documentés. Vid_ScreenMode/Vid_isRTG sont des globaux C de screen.c.
 *
 * __nocommandline=1 dans l'original (pas de ligne de commande) : ReadArgs renvoie
 * donc « aucun argument » et le mode est choisi par GetScreenMode().
 */
public final class MainC {

    /** graphics/modeid.h : INVALID_ID = ~0 (0xFFFFFFFF). */
    private static final int INVALID_ID = 0xFFFFFFFF;

    private static final int OPT_SCREENMODE = 0;
    private static final int OPT_COUNT = 1;

    /** Base cybergraphics.library (0 = indisponible dans le portage → AGA). */
    private static int CyberGfxBase = 0;

    private MainC() {
    }

    /**
     * Équivalent de `int main(int argc, char* argv[])`. Renvoie le code de retour.
     */
    public static int run(String[] argv) {
        int rval = 10;                                       // int rval = 10;
        if (!SystemC_Sys_OpenLibs()) {                       // if (!Sys_OpenLibs())
            // goto fail;
            Sys_CloseLibs();
            return rval;
        }

        Mem.wl(Vid_ScreenMode, INVALID_ID);                  // Vid_ScreenMode = INVALID_ID;

        int[] options = new int[OPT_COUNT];                  // LONG options[OPT_COUNT] = { 0 };
        int args = ReadArgs("SCREENMODE/N", options);        // args = ReadArgs("SCREENMODE/N", options, NULL)
        if (args != 0) {                                     // if (args != NULL)
            if (options[OPT_SCREENMODE] != 0) {              // if (options[OPT_SCREENMODE])
                Mem.wl(Vid_ScreenMode, Mem.l(options[OPT_SCREENMODE])); // Vid_ScreenMode = *(int*)options[OPT_SCREENMODE]
            }
            FreeArgs(args);                                  // FreeArgs(args);
        }

        if (Mem.l(Vid_ScreenMode) == INVALID_ID) {           // if (Vid_ScreenMode == INVALID_ID)
            Mem.wl(Vid_ScreenMode, ScreenC.GetScreenMode()); //   Vid_ScreenMode = GetScreenMode();
        }

        if (Mem.l(Vid_ScreenMode) == INVALID_ID) {           // if (Vid_ScreenMode == INVALID_ID)
            PutStr("Invalid Screenmode\n");                  //   PutStr("Invalid Screenmode\n");
            Sys_CloseLibs();                                 //   goto fail;
            return rval;
        }
        if (CyberGfxBase != 0) {                             // if (CyberGfxBase)
            Mem.wl(Vid_isRTG, IsCyberModeID(Mem.l(Vid_ScreenMode))); // Vid_isRTG = IsCyberModeID(Vid_ScreenMode);
        }

        rval = 0;                                            // rval = 0;

        startup();                                           // startup();

        // fail:
        Sys_CloseLibs();                                     // Sys_CloseLibs();
        return rval;                                         // return rval;
    }

    // ------------------------------------------------------------------
    // Externes non portés — stubs documentés (bring-up OS / moteur).
    // ------------------------------------------------------------------

    /** Sys_OpenLibs (system.c → SystemC, stub hôte). */
    private static boolean SystemC_Sys_OpenLibs() {
        return SystemC.Sys_OpenLibs() != 0;                  // BOOL Sys_OpenLibs(void)
    }

    /** Sys_CloseLibs (system.c → SystemC, stub hôte). */
    private static void Sys_CloseLibs() {
        SystemC.Sys_CloseLibs();
    }

    /** ReadArgs (dos.library) : pas de ligne de commande (__nocommandline) → aucun argument. */
    private static int ReadArgs(String template, int[] options) {
        return 0; // RDArgs* NULL — à traduire avec dos.library si CLI requis
    }

    /** FreeArgs (dos.library). */
    private static void FreeArgs(int args) {
        // no-op — à traduire avec dos.library
    }

    /** PutStr (dos.library) : écrit une chaîne sur la sortie standard. */
    private static void PutStr(String s) {
        System.out.print(s);
    }

    /** IsCyberModeID (cybergraphics.library) : le ModeID est-il un mode RTG ? */
    private static int IsCyberModeID(int modeID) {
        throw new UnsupportedOperationException("c/main.c::IsCyberModeID (cybergraphics.library hôte)");
    }

    /** startup (hires.s _startup) : entrée du moteur (init + boucle de frame). */
    private static void startup() {
        ab3d2.Hires.startup();
    }
}
