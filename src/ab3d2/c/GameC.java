package ab3d2.c;

import ab3d2.Mem;

import static ab3d2.Defs.InvT_Items;
import static ab3d2.Defs.InvIT_JetPack_w;
import static ab3d2.Defs.InvCT_Health_w;
import static ab3d2.Defs.GModT_MaxInv;
import static ab3d2.Defs.GStatT_TotalHealthCollected_w;
import static ab3d2.Defs.NUM_INVENTORY_ITEMS;
import static ab3d2.Defs.NUM_INVENTORY_CONSUMABLES;
import static ab3d2.Defs.GAME_EVENTBIT_ADD_INVENTORY;

import static ab3d2.bss.TablesBss.game_ModProps;
import static ab3d2.bss.GameBss.game_PlayerProgression;
import static ab3d2.bss.GameBss.Game_ProgressSignal_l;
import static ab3d2.bss.PlayerBss.Plr_MultiplayerType_b;

/**
 * Traduction littérale de ab3d2_source/c/game_properties.c (fonctions inventaire
 * appelées par le moteur via CALLC). Les structures sont vues comme des tableaux
 * de UWORD (consommables) / UWORD (items) en mémoire plate ; les pointeurs C
 * deviennent des adresses Mem, l'accès tableau objInvPtr[n] devient Mem.uw(base + 2*n).
 *
 * BOOL : TRUE = 1, FALSE = 0 (SAS/C). Les comparaisons UWORD sont non signées
 * (valeurs 0..65535 stockées dans des int → comparaison int standard équivalente).
 *
 * Les fonctions de chargement (game_LoadModProperties, game_*AchievementsData)
 * dépendent de l'I/O DOS (Open/Read/Close) et restent à traduire avec la couche hôte.
 */
public final class GameC {

    private static final int TRUE = 1;
    private static final int FALSE = 0;

    private static final int GAME_MODE_SINGLE_PLAYER = 'n';

    private GameC() {
    }

    /**
     * Game_CheckInventoryLimits (CALLC) — a0 = inventaire joueur (InvT),
     * a1 = consommables de l'objet (InvCT), a2 = items de l'objet (InvIT) ;
     * renvoie TRUE (≠0) si au moins un élément peut être ramassé.
     */
    public static int Game_CheckInventoryLimits(int inventory, int consumables, int items) {
        int plrInvPtr = inventory + InvT_Items + InvIT_JetPack_w; // UWORD const *plrInvPtr = &inventory->inv_Items.ii_Jetpack;
        int objInvPtr = items + InvIT_JetPack_w;                  // UWORD const *objInvPtr = &items->ii_Jetpack;
        int givesAnything = 0;                                    // UWORD givesAnything = 0;

        if (Mem.b(Plr_MultiplayerType_b) == GAME_MODE_SINGLE_PLAYER) { // if (Plr_MultiplayerType_b == GAME_MODE_SINGLE_PLAYER)
            // En solo, on sort dès qu'un item est donné, même sans munitions.
            for (int n = 0; n < NUM_INVENTORY_ITEMS; ++n) {       // for (UWORD n = 0; n < sizeof(InventoryItems)/sizeof(UWORD); ++n)
                if (Mem.uw(objInvPtr + 2 * n) != 0) {             //   if (objInvPtr[n])
                    return TRUE;                                  //     return TRUE;
                }
            }
        } else {
            // En multijoueur, on ne ramasse pas un item déjà possédé, sauf munitions non saturées.
            for (int n = 0; n < NUM_INVENTORY_ITEMS; ++n) {       // for (UWORD n = 0; n < sizeof(InventoryItems)/sizeof(UWORD); ++n)
                givesAnything |= Mem.uw(objInvPtr + 2 * n);       //   givesAnything |= objInvPtr[n];
                if (Mem.uw(objInvPtr + 2 * n) != 0 && Mem.uw(plrInvPtr + 2 * n) == 0) { // if (objInvPtr[n] && !plrInvPtr[n])
                    return TRUE;                                  //     return TRUE;
                }
            }
        }

        // Si l'item donne une quantité de quelque chose dont on n'est pas au max, on peut le ramasser.
        plrInvPtr = inventory + InvCT_Health_w;                   // plrInvPtr = &inventory->inv_Consumables.ic_Health;
        objInvPtr = consumables + InvCT_Health_w;                 // objInvPtr = &consumables->ic_Health;
        int limPtr = game_ModProps + GModT_MaxInv + InvCT_Health_w; // UWORD const *limPtr = &game_ModProps.gmp_MaxInventory.ic_Health;
        for (int n = 0; n < NUM_INVENTORY_CONSUMABLES; ++n) {     // for (UWORD n = 0; n < sizeof(InventoryConsumables)/sizeof(UWORD); ++n)
            givesAnything = (givesAnything + Mem.uw(objInvPtr + 2 * n)) & 0xFFFF; // givesAnything += objInvPtr[n];
            if (Mem.uw(objInvPtr + 2 * n) > 0 && Mem.uw(plrInvPtr + 2 * n) < Mem.uw(limPtr + 2 * n)) { // if (objInvPtr[n] > 0 && plrInvPtr[n] < limPtr[n])
                return TRUE;                                      //     return TRUE;
            }
        }
        return givesAnything != 0 ? FALSE : TRUE;                 // return givesAnything ? FALSE : TRUE;
    }

    /**
     * Helper : addition saturée jusqu'à une limite. Si la somme déborde (UWORD)
     * ou dépasse la limite, renvoie la limite, sinon la somme.
     */
    private static int addSaturated(int a, int b, int limit) {
        int sum = (a + b) & 0xFFFF;                               // UWORD sum = a + b;
        return (sum < a || sum < b || sum > limit) ? limit : sum; // return (sum < a || sum < b || sum > limit) ? limit : sum;
    }

    /**
     * Game_AddToInventory (CALLC) — a0 = inventaire joueur, a1 = consommables,
     * a2 = items : applique le ramassage en respectant les bornes
     * (game_ModProps.gmp_MaxInventory) et met à jour les totaux de progression.
     */
    public static void Game_AddToInventory(int inventory, int consumables, int items) {
        int plrInvPtr = inventory + InvT_Items + InvIT_JetPack_w; // UWORD *plrInvPtr = &inventory->inv_Items.ii_Jetpack;
        int objInvPtr = items + InvIT_JetPack_w;                  // UWORD const *objInvPtr = &items->ii_Jetpack;

        // Ajoute tous les items.
        for (int n = 0; n < NUM_INVENTORY_ITEMS; ++n) {           // for (UWORD n = 0; n < sizeof(InventoryItems)/sizeof(UWORD); ++n)
            Mem.ww(plrInvPtr + 2 * n, Mem.uw(plrInvPtr + 2 * n) | Mem.uw(objInvPtr + 2 * n)); // plrInvPtr[n] |= objInvPtr[n];
        }

        plrInvPtr = inventory + InvCT_Health_w;                   // plrInvPtr = &inventory->inv_Consumables.ic_Health;
        objInvPtr = consumables + InvCT_Health_w;                 // objInvPtr = &consumables->ic_Health;

        int game_TotalCollectedPtr = game_PlayerProgression + GStatT_TotalHealthCollected_w; // ULONG* game_TotalCollectedPtr = &game_PlayerProgression.gs_TotalHealthCollected;

        int limInvPtr = game_ModProps + GModT_MaxInv + InvCT_Health_w; // UWORD const* limInvPtr = &game_ModProps.gmp_MaxInventory.ic_Health;

        // Ajoute tous les consommables.
        for (int n = 0; n < NUM_INVENTORY_CONSUMABLES; ++n) {     // for (UWORD n = 0; n < sizeof(InventoryConsumables)/sizeof(UWORD); ++n)
            int preInv = Mem.uw(plrInvPtr + 2 * n);               //   UWORD preInv = plrInvPtr[n];
            int newInv = addSaturated(                            //   plrInvPtr[n] = addSaturated(
                Mem.uw(plrInvPtr + 2 * n),                        //       plrInvPtr[n],
                Mem.uw(objInvPtr + 2 * n),                        //       objInvPtr[n],
                Mem.uw(limInvPtr + 2 * n)                         //       limInvPtr[n]
            );                                                    //   );
            Mem.ww(plrInvPtr + 2 * n, newInv);
            // Cumule le total collecté pour la progression (mêmes index, en 32 bits).
            Mem.wl(game_TotalCollectedPtr + 4 * n,                // game_TotalCollectedPtr[n] +=
                Mem.l(game_TotalCollectedPtr + 4 * n) + (newInv - preInv)); // plrInvPtr[n] - preInv;
        }
        Mem.wl(Game_ProgressSignal_l, Mem.l(Game_ProgressSignal_l) | (1 << GAME_EVENTBIT_ADD_INVENTORY)); // Game_ProgressSignal |= (1 << GAME_EVENTBIT_ADD_INVENTORY);
    }

    /**
     * Game_ApplyInventoryLimits — a0 = inventaire : applique les bornes courantes
     * à une partie chargée (clampe chaque consommable à sa limite).
     */
    public static void Game_ApplyInventoryLimits(int inventory) {
        int limPtr = game_ModProps + GModT_MaxInv + InvCT_Health_w; // UWORD const *limPtr = &game_ModProps.gmp_MaxInventory.ic_Health;
        int invPtr = inventory + InvCT_Health_w;                  // UWORD *invPtr = &inventory->inv_Consumables.ic_Health;
        for (int n = 0; n < NUM_INVENTORY_CONSUMABLES; ++n) {     // for (UWORD n = 0; n < sizeof(InventoryConsumables)/sizeof(UWORD); ++n)
            if (Mem.uw(invPtr + 2 * n) > Mem.uw(limPtr + 2 * n)) { // if (invPtr[n] > limPtr[n])
                Mem.ww(invPtr + 2 * n, Mem.uw(limPtr + 2 * n));   //   invPtr[n] = limPtr[n];
            }
        }
    }

    // ------------------------------------------------------------------
    // game.c — Game_Init / Game_Done (startup / shutdown).
    // ------------------------------------------------------------------

    /** Game_Init : charge mod properties, préférences et progression. */
    public static void Game_Init() {
        game_LoadModProperties();                          // game_LoadModProperties();
        GamePreferences.game_LoadPreferences();            // game_LoadPreferences();
        GameProgress.game_LoadPlayerProgression();         // game_LoadPlayerProgression();
    }

    /** Game_Done : persiste progression et préférences, puis libère les données. */
    public static void Game_Done() {
        GameProgress.game_SavePlayerProgression();         // game_SavePlayerProgression();
        GamePreferences.game_SavePreferences();            // game_SavePreferences();
        game_FreeAchievementsData();                       // game_FreeAchievementsData();
    }

    // ------------------------------------------------------------------
    // game_properties.c — loaders d'achievements/mod (DOS I/O, à traduire).
    // ------------------------------------------------------------------

    // game.h : limites d'inventaire par défaut.
    private static final int GAME_DEFAULT_AMMO_LIMIT = 10000;
    private static final int GAME_DEFAULT_HEALTH_LIMIT = 10000;
    private static final int GAME_DEFAULT_FUEL_LIMIT = 250;

    /**
     * game_LoadModProperties (game_properties.c:53) : initialise les limites d'inventaire
     * PAR DÉFAUT, puis (différé) chargerait les overrides depuis game_PropertiesFile.
     * CRITIQUE : sans ces défauts, gmp_MaxInventory reste à 0 → addSaturated clampe tout
     * à 0 → AUCUN pickup ne peut rien ajouter (« objets vides »). Le chargement DOS du
     * fichier d'overrides reste différé (fichier optionnel ; les défauts suffisent).
     */
    public static void game_LoadModProperties() {
        int maxInv = game_ModProps + GModT_MaxInv;                 // &game_ModProps.gmp_MaxInventory
        Mem.ww(maxInv + InvCT_Health_w, GAME_DEFAULT_HEALTH_LIMIT);       // ic_Health = 10000
        Mem.ww(maxInv + ab3d2.Defs.InvCT_JetpackFuel_w, GAME_DEFAULT_FUEL_LIMIT); // ic_JetpackFuel = 250
        for (int i = 0; i < ab3d2.Defs.NUM_BULLET_DEFS; ++i) {           // ic_AmmoCounts[i] = 10000
            Mem.ww(maxInv + ab3d2.Defs.InvCT_AmmoCounts_vw + i * 2, GAME_DEFAULT_AMMO_LIMIT);
        }
        // Overrides depuis game_PropertiesFile (DOS) : DIFFÉRÉ (fichier optionnel).
    }

    /** game_FreeAchievementsData (game_properties.c) : libère les données d'achievements (no-op si non chargées). */
    public static void game_FreeAchievementsData() {
        int ptr = Mem.l(ab3d2.bss.GameBss.game_AchievementsDataPtr_l); // if (game_AchievementsDataPtr)
        if (ptr != 0) {
            ab3d2.host.ExecLib.FreeVec(ptr);                  //   FreeVec(game_AchievementsDataPtr);
        }
        Mem.wl(ab3d2.bss.GameBss.game_AchievementsDataPtr_l, 0); // game_AchievementsDataPtr = 0;
        Mem.ww(game_ModProps + ab3d2.Defs.GModT_NumAchievements, 0); // gmp_NumAchievements = 0
        Mem.ww(game_ModProps + ab3d2.Defs.GModT_AchievementSize, 0); // gmp_AchievementSize = 0
    }
}
