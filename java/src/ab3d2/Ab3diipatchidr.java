package ab3d2;

/**
 * Traduction de ab3d2_source/ab3diipatchidr.s.
 *
 * Patch qui annule tout requester d'erreur DOS en remplaçant la fonction
 * _LVOEasyRequestArgs d'intuition.library (SetFunction) par une fonction qui
 * renvoie 0. C'est du patching OS pur (sans rapport avec la logique de jeu) :
 * sans objet dans le portage Java (pas de requester DOS). MakePatch est donc un
 * no-op et NewFunction renvoie 0.
 */
public final class Ab3diipatchidr {

    private Ab3diipatchidr() {
    }

    /**
     * MakePatch : OpenLibrary("intuition.library",36) puis SetFunction sur
     * EasyRequestArgs → NewFunction. Sans effet ici.
     */
    public static void MakePatch() {
        // OpenLibrary intuition + SetFunction(EasyRequestArgs, NewFunction) — non pertinent (couche hôte).
    }

    /** NewFunction : move.l #0,d0 ; rts (supprime le requester en renvoyant 0). */
    public static int NewFunction() {
        return 0;                                          // move.l #0,d0 ; rts
    }
}
