package ab3d2;

import static ab3d2.Defs.EntT_ZoneID_w;
import static ab3d2.Defs.ObjT_ZoneID_w;

/**
 * Traduction littérale de ab3d2_source/macros.i
 *
 * Trois familles de macros :
 *
 * 1. Macros matériel/OS (CACHE_*, BLIT_NASTY, WAIT_BLIT, SETCOPLOR0, DUGDOS,
 *    DataCacheOn/Off, CINIT/CMOVE/CWAIT/CEND, FLASHER, _break, FILTER...) :
 *    elles pilotent le blitter, le copper, les caches CPU ou appellent
 *    dos/req.library. Dans le port elles sont soit sans objet (caches,
 *    blitter-nasty), soit résolues au point d'usage vers la couche hôte.
 *    Beaucoup sont d'ailleurs déjà commentées dans l'original.
 *
 * 2. Macros de registres (SAVEREGS/GETREGS, QMOVE) : sans objet en Java —
 *    les registres sont des variables locales, movem devient... rien.
 *
 * 3. Macros de jeu (FREE_OBJ, FREE_ENT, SET_MEM_BIT, STATS_*) : traduites
 *    ci-dessous en méthodes statiques, sémantique mémoire identique.
 *
 * CALLC \1 → appel direct de la fonction C portée (ex. CALLC Game_LevelBegin
 * → Game.Game_LevelBegin()). DCLC définit un label double (_x/x) : sans objet.
 */
public final class Macros {

    private Macros() {
    }

    // FREE_OBJ \1 : move.w #-1,ObjT_ZoneID_w(\1)
    public static void FREE_OBJ(int aN) {
        Mem.ww(aN + ObjT_ZoneID_w, -1);
    }

    // FREE_OBJ_2 \1,\2 : move.w #-1,ObjT_ZoneID_w+\2(\1)
    public static void FREE_OBJ_2(int aN, int off) {
        Mem.ww(aN + ObjT_ZoneID_w + off, -1);
    }

    // FREE_ENT \1 : move.w #-1,ObjT_ZoneID_w(\1) ; move.w #-1,EntT_ZoneID_w(\1)
    public static void FREE_ENT(int aN) {
        Mem.ww(aN + ObjT_ZoneID_w, -1);
        Mem.ww(aN + EntT_ZoneID_w, -1);
    }

    // FREE_ENT_2 \1,\2
    public static void FREE_ENT_2(int aN, int off) {
        Mem.ww(aN + ObjT_ZoneID_w + off, -1);
        Mem.ww(aN + EntT_ZoneID_w + off, -1);
    }

    /**
     * SET_MEM_BIT \1,\2 : bset.b #(\1&7),\2+3-(\1>>3)
     * Positionne le bit n°bit d'un ULONG big-endian situé à addr.
     * (addr+3-(bit>>3) : l'octet de poids faible d'un long BE est à addr+3.)
     */
    public static void SET_MEM_BIT(int bit, int addr) {
        int a = addr + 3 - (bit >> 3);
        Mem.wb(a, Mem.ub(a) | (1 << (bit & 7)));
    }

    // STATS_PLAY  → CALLC Game_LevelBegin   (porté avec c/game_progress.c)
    // STATS_WON   → CALLC Game_LevelWon
    // STATS_DIED  → CALLC Game_LevelFailed
    // STATS_KILL  → add.w #1,(game_PlayerProgression+GStatT_AlienKills_vw, EntT_Type_b*2)
    //               move.l #1,Game_ProgressSignal_l
    //               SET_MEM_BIT GAME_EVENTBIT_KILL,Game_ProgressSignal_l
    // Ces quatre macros seront inlinées aux points d'usage quand les symboles
    // game_PlayerProgression / Game_ProgressSignal_l (c/game_progress.c) seront portés.
}
