package ab3d2;

/**
 * Traduction de ab3d2_source/system.i et ab3d2_source/funcdef.i
 *
 * system.i : uniquement des includes des headers AmigaOS (exec, graphics,
 * intuition, dos, hardware...) et les macros d'appel de bibliothèques :
 *
 *   CALLEXEC \1  : move.l 4.w,a6           ; jsr _LVO\1(a6)
 *   CALLINT  \1  : move.l _IntuitionBase,a6; jsr _LVO\1(a6)
 *   CALLGRAF \1  : move.l _GfxBase,a6      ; jsr _LVO\1(a6)
 *   CALLDOS  \1  : move.l _DOSBase,a6      ; jsr _LVO\1(a6)
 *   CALLMISC \1  : move.l _MiscBase,a6     ; jsr _LVO\1(a6)
 *   CALLPOTGO\1  : move.l _PotgoBase,a6    ; jsr _LVO\1(a6)
 *   INTNAME/GRAFNAME : chaînes "intuition.library"/"graphics.library"
 *
 * funcdef.i : la macro FUNCDEF génère les offsets _LVOxxx (vecteurs négatifs
 * de bibliothèque, -30, -36, -42...). C'est le mécanisme de liaison dynamique
 * AmigaOS — sans objet en Java.
 *
 * Dans le port, chaque CALLxxx Yyy au point d'usage devient un appel direct
 * vers la couche hôte (ab3d2.host.*) qui implémente les fonctions OS
 * réellement utilisées par le jeu (AllocMem, OpenScreen, LoadRGB4, DoIO...).
 */
public final class SystemInc {

    public static final String INTNAME = "intuition.library";
    public static final String GRAFNAME = "graphics.library";

    // Tailles de structures AmigaOS référencées par les sections BSS :
    /** dos/dosextens.i : taille de struct FileInfoBlock. */
    public static final int fib_SIZEOF = 260;
    /** devices/timer.i : taille de struct timerequest (IORequest 32 + timeval 8). */
    public static final int IOTV_SIZE = 40;

    private SystemInc() {
    }
}
