package ab3d2.modules;

/**
 * Traduction de ab3d2_source/modules/dev_memtrack.s
 *
 * Outillage de debug mémoire conditionné par IFD MEMTRACK (désactivé dans le
 * Makefile : "#AFLAGS += -DMEMTRACK = 1"). L'original :
 *  - mem_serputch / SERPRINTF : printf vers le port série matériel (SERSEND) ;
 *  - mem_PrintLibNodes : parcourt la liste des bibliothèques d'exec
 *    (LibList+LH_HEAD) et imprime nom + compteur d'ouverture ;
 *  - Mem_TrackInit/Done : PATCHE les vecteurs _LVOAllocMem/_LVOFreeMem
 *    d'exec.library pour intercepter les allocations de la tâche du jeu ;
 *  - mem_TrackAlloc : trace taille/flags/résultat et SCANNE LA PILE à la
 *    recherche d'une adresse de retour vraisemblable (entre __stext et la fin
 *    du hunk) ;
 *  - mem_TrackFree : trace adresse/taille puis chaîne vers l'original.
 *
 * Rien de tout cela n'a d'équivalent dans le port (pas de vecteurs exec à
 * patcher, pas de pile 68k à scanner, pas de port série) : les points
 * d'entrée sont conservés comme no-ops journalisés pour que les sites
 * d'appel (hires.s : IFD MEMTRACK → bsr Mem_TrackInit) restent traduisibles.
 */
public final class DevMemtrack {

    private DevMemtrack() {
    }

    /** Mem_TrackInit (no-op portage — voir javadoc de classe). */
    public static void Mem_TrackInit() {
        System.err.println("[DevMemtrack] Mem_TrackInit (no-op dans le port)");
    }

    /** Mem_TrackDone (no-op portage). */
    public static void Mem_TrackDone() {
        System.err.println("[DevMemtrack] Mem_TrackDone (no-op dans le port)");
    }
}
