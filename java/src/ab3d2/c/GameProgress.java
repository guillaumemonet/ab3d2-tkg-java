package ab3d2.c;

import ab3d2.Mem;
import ab3d2.host.ExecLib;
import ab3d2.host.SysTimer;
import ab3d2.host.DosLib;

import static ab3d2.Defs.AchievementT_Name_l;
import static ab3d2.Defs.AchievementT_RewardDesc_l;
import static ab3d2.Defs.AchievementT_RuleMask_w;
import static ab3d2.Defs.AchievementT_RuleId_w;
import static ab3d2.Defs.AchievementT_RuleParams_vb;
import static ab3d2.Defs.AchievementT_HealthCapBonus_w;
import static ab3d2.Defs.AchievementT_HealthBonus_w;
import static ab3d2.Defs.AchievementT_FuelCapBonus_w;
import static ab3d2.Defs.AchievementT_AmmoType_w;
import static ab3d2.Defs.AchievementT_AmmoTypeCapBonus_w;
import static ab3d2.Defs.AchievementT_AmmoTypeBonus_w;
import static ab3d2.Defs.AchievementT_SizeOf_l;
import static ab3d2.Defs.GStatT_MaxInv;
import static ab3d2.Defs.GStatT_LevelBestTimes_vl;
import static ab3d2.Defs.GStatT_LevelPlayCounts_vw;
import static ab3d2.Defs.GStatT_LevelWonCounts_vw;
import static ab3d2.Defs.GStatT_LevelFailCounts_vw;
import static ab3d2.Defs.GStatT_LevelImprovedTimeCounts_vw;
import static ab3d2.Defs.GStatT_AlienKills_vw;
import static ab3d2.Defs.GStatT_TotalHealthCollected_w;
import static ab3d2.Defs.GStatT_Achieved_vb;
import static ab3d2.Defs.GStatT_SizeOf_l;
import static ab3d2.Defs.GModT_MaxInv;
import static ab3d2.Defs.GModT_NumAchievements;
import static ab3d2.Defs.InvT_Consumables;
import static ab3d2.Defs.InvCT_Health_w;
import static ab3d2.Defs.InvCT_JetpackFuel_w;
import static ab3d2.Defs.InvCT_AmmoCounts_vw;
import static ab3d2.Defs.InvCT_SizeOf_l;
import static ab3d2.Defs.MSG_TAG_OPTIONS;
import static ab3d2.Defs.NUM_ALIEN_DEFS;
import static ab3d2.Defs.NUM_LEVELS;
import static ab3d2.Defs.NUM_BULLET_DEFS;
import static ab3d2.Defs.GAME_EVENTBIT_KILL;
import static ab3d2.Defs.GAME_EVENTBIT_ZONE_CHANGE;
import static ab3d2.Defs.GAME_EVENTBIT_LEVEL_START;
import static ab3d2.Defs.GAME_EVENTBIT_ADD_INVENTORY;

import static ab3d2.bss.GameBss.game_PlayerProgression;
import static ab3d2.bss.GameBss.Game_ProgressSignal_l;
import static ab3d2.bss.GameBss.game_AchievementsDataPtr_l;
import static ab3d2.bss.GameBss.game_BestLevelTimeBuffer_vb;
import static ab3d2.bss.TablesBss.game_ModProps;
import static ab3d2.bss.PlayerBss.Plr1_Invetory_vw;
import static ab3d2.bss.PlayerBss.Plr1_Zone_w;
import static ab3d2.ControlloopData.Game_LevelNumber_w;
import static ab3d2.data.GameData.game_ProgressFile_vb;

/**
 * Traduction littérale de ab3d2_source/c/game_progress.c.
 *
 * Progression du joueur et achievements. Les règles d'achievement, stockées sous
 * forme de pointeurs de fonction dans game_AchievementRules[], sont rendues par un
 * dispatch sur ac_RuleId (callRule). Les paramètres de règle (ac_RuleParams, union
 * de 8 octets) sont lus selon leur type (UWORD/ULONG) comme dans l'original.
 *
 * Primitives hôte : CopyMem (ExecLib), GetSysTime/SubTime (SysTimer, timer.device),
 * Open/Read/Write/Close (DosLib), RawDoFmt (ExecLib). Msg_PushLine = c/message.c.
 * Game_ApplyInventoryLimits = c/game_properties.c (GameC).
 *
 * Les struct timeval statiques (game_LevelBegin/End) et le tampon data[5] de
 * Game_LevelBegin sont alloués en Mem (passés par adresse aux primitives).
 * DOSFALSE = 0.
 */
public final class GameProgress {

    private static final int TRUE = 1;
    private static final int FALSE = 0;
    private static final int DOSFALSE = 0;

    // struct timeval game_LevelBegin / game_LevelEnd (8 octets : tv_secs@0, tv_micro@4)
    private static final int game_LevelBegin = Mem.alloc(8);
    private static final int game_LevelEnd = Mem.alloc(8);

    // tampon data[5] (UWORD) de Game_LevelBegin + chaîne de format RawDoFmt
    private static final int lvlBegin_data = Mem.alloc(5 * 2);
    private static final int lvlBegin_fmt = Mem.dcStr("Level %c: Best %dh %02dm %02d.%02ds");

    /**
     * Masques de bits d'événement. Chaque entrée correspond, dans le même ordre, à
     * game_AchievementRules[] ; un résultat non nul contre Game_ProgressSignal
     * déclenche l'évaluation de la règle. (Référencé par game_InitAchievementsData.)
     */
    public static final int[] game_AchievementRuleMask = {
        1 << GAME_EVENTBIT_KILL,           // game_AchievementRuleKillCount
        1 << GAME_EVENTBIT_KILL,           // game_AchievementRuleGroupKillCount
        1 << GAME_EVENTBIT_ZONE_CHANGE,    // game_AchievementRuleZoneFound
        1 << GAME_EVENTBIT_LEVEL_START,    // game_AchievementRuleLevelTimeImproved
        1 << GAME_EVENTBIT_LEVEL_START,    // game_AchievementRuleTimesDied
        1 << GAME_EVENTBIT_ADD_INVENTORY,  // game_AchievementRuleStuffCollected
    };

    private GameProgress() {
    }

    /**
     * game_LoadPlayerProgression — charge la progression sauvegardée si elle existe.
     */
    public static void game_LoadPlayerProgression() {
        int gameProgressFH = DosLib.Open(game_ProgressFile_vb, DosLib.MODE_OLDFILE); // Open(game_ProgressFile, MODE_OLDFILE)
        if (DOSFALSE == gameProgressFH) {                                            // if (DOSFALSE == gameProgressFH)
            // État initial du cap inventaire = défaut du mod.
            ExecLib.CopyMem(                                                         // CopyMem(
                game_ModProps + GModT_MaxInv,                                        //   &game_ModProps.gmp_MaxInventory,
                game_PlayerProgression + GStatT_MaxInv,                              //   &game_PlayerProgression.gs_MaxInventory,
                InvCT_SizeOf_l);                                                     //   sizeof(InventoryConsumables))
            return;
        }

        int read = DosLib.Read(gameProgressFH, game_PlayerProgression, GStatT_SizeOf_l); // Read(fh, &game_PlayerProgression, sizeof(...))
        if (read == GStatT_SizeOf_l) {                                               // if (read == sizeof(Game_PlayerProgression))
            // Écrase le cap inventaire avec la version progressée du joueur.
            ExecLib.CopyMem(
                game_PlayerProgression + GStatT_MaxInv,                              //   &game_PlayerProgression.gs_MaxInventory,
                game_ModProps + GModT_MaxInv,                                        //   &game_ModProps.gmp_MaxInventory,
                InvCT_SizeOf_l);
        }
        DosLib.Close(gameProgressFH);
    }

    /**
     * game_SavePlayerProgression — persiste la progression du joueur.
     */
    public static void game_SavePlayerProgression() {
        int gameProgressFH = DosLib.Open(game_ProgressFile_vb, DosLib.MODE_READWRITE); // Open(game_ProgressFile, MODE_READWRITE)
        if (DOSFALSE == gameProgressFH) {
            return;
        }
        // Persiste les limites d'inventaire progressées.
        ExecLib.CopyMem(
            game_ModProps + GModT_MaxInv,
            game_PlayerProgression + GStatT_MaxInv,
            InvCT_SizeOf_l);
        DosLib.Write(gameProgressFH, game_PlayerProgression, GStatT_SizeOf_l);       // Write(fh, &game_PlayerProgression, sizeof(...))
        DosLib.Close(gameProgressFH);
    }

    private static boolean game_CheckAchieved(int i) {
        return (Mem.ub(game_PlayerProgression + GStatT_Achieved_vb + (i >> 3)) & (1 << (i & 7))) != 0; // gs_Achieved[i>>3] & (1<<(i&7))
    }

    private static void game_MarkAchieved(int i) {
        int a = game_PlayerProgression + GStatT_Achieved_vb + (i >> 3);
        Mem.wb(a, Mem.ub(a) | (1 << (i & 7)));                                       // gs_Achieved[i>>3] |= (1<<(i&7))
    }

    // ===== règles d'achievement (game_AchievementRules[]) =====

    /** params { UWORD alienClass; UWORD countLimit; } */
    private static boolean game_AchievementRuleKillCount(int achievement) {
        int p = achievement + AchievementT_RuleParams_vb;                           // ac_Params = (UWORD*)&ac_RuleParams[0]
        return Mem.uw(game_PlayerProgression + GStatT_AlienKills_vw + Mem.uw(p) * 2) // gs_AlienKills[ac_Params[0]]
            >= Mem.uw(p + 2);                                                        //   >= ac_Params[1]
    }

    /** params { ULONG totalCount; UWORD consumable; } */
    private static boolean game_AchievementRuleStuffCollected(int achievement) {
        int p = achievement + AchievementT_RuleParams_vb;
        long totalCount = Mem.l(p) & 0xFFFFFFFFL;                                    // *(ULONG*)&ac_RuleParams[0]
        int consumable = Mem.uw(p + 4);                                             // *(UWORD*)&ac_RuleParams[sizeof(ULONG)]
        int consumables = game_PlayerProgression + GStatT_TotalHealthCollected_w;   // &gs_TotalHealthCollected
        return (Mem.l(consumables + consumable * 4) & 0xFFFFFFFFL) >= totalCount;    // consumables[consumable] >= totalCount
    }

    /** params { ULONG alienMask; UWORD countLimit; } */
    private static boolean game_AchievementRuleGroupKillCount(int achievement) {
        int p = achievement + AchievementT_RuleParams_vb;
        int enemyMask = Mem.l(p) & ((1 << NUM_ALIEN_DEFS) - 1);                      // *(ULONG*)&ac_RuleParams[0] & ((1<<NUM_ALIEN_DEFS)-1)
        int countLimit = Mem.uw(p + 4);                                             // *(UWORD*)&ac_RuleParams[sizeof(ULONG)]
        int count = 0;                                                              // UWORD count = 0;
        for (int id = 0; id < NUM_ALIEN_DEFS; ++id) {
            if ((enemyMask & (1 << id)) != 0) {                                     // if (enemyMask & (1 << id))
                count = (count + Mem.uw(game_PlayerProgression + GStatT_AlienKills_vw + id * 2)) & 0xFFFF; // count += gs_AlienKills[id]
            }
            if (count >= countLimit) {                                             // if (count >= countLimit)
                return TRUE != 0;                                                   //   return TRUE;
            }
        }
        return false;
    }

    /** params { UWORD levelNumber; UWORD zoneID; } (comparé comme un ULONG) */
    private static boolean game_AchievementRuleZoneFound(int achievement) {
        int p = achievement + AchievementT_RuleParams_vb;
        int levelAndZone = (Mem.uw(Game_LevelNumber_w) << 16) | Mem.uw(Plr1_Zone_w); // ((ULONG)Game_LevelNumber << 16) | Plr1_Zone
        return levelAndZone == Mem.l(p);                                            // == *(ULONG*)&ac_RuleParams[0]
    }

    /** params { ULONG levelMask; UWORD countLimit; BOOL overall; } */
    private static boolean game_AchievementRuleLevelTimeImproved(int achievement) {
        int p = achievement + AchievementT_RuleParams_vb;                           // ac_Params = (UWORD*)&ac_RuleParams[0]
        int levelMask = Mem.l(p);                                                   // *((ULONG*)(&ac_Params[0]))
        int countLimit = Mem.uw(p + 4);                                            // ac_Params[2]
        int count = 0;                                                              // UWORD count = 0;
        boolean overall = Mem.uw(p + 6) != 0;                                       // ac_Params[3]
        for (int levelNum = 0; levelNum < NUM_LEVELS; ++levelNum) {
            if ((levelMask & (1 << levelNum)) != 0) {                              // if (levelMask & (1 << levelNum))
                if (overall) {                                                     // if (overall)
                    count = (count + Mem.uw(game_PlayerProgression + GStatT_LevelImprovedTimeCounts_vw + levelNum * 2)) & 0xFFFF; // count += gs_LevelImprovedTimeCounts[levelNum]
                } else {
                    count = Mem.uw(game_PlayerProgression + GStatT_LevelImprovedTimeCounts_vw + levelNum * 2); // count = gs_LevelImprovedTimeCounts[levelNum]
                }
                if (count >= countLimit) {                                         // if (count >= countLimit)
                    return TRUE != 0;
                }
            }
        }
        return false;
    }

    /** params { ULONG levelMask; UWORD countLimit; BOOL overall; } */
    private static boolean game_AchievementRuleTimesDied(int achievement) {
        int p = achievement + AchievementT_RuleParams_vb;
        int levelMask = Mem.l(p);                                                   // *((ULONG*)(&ac_Params[0]))
        int countLimit = Mem.uw(p + 4);                                            // ac_Params[2]
        int count = 0;
        boolean overall = Mem.uw(p + 6) != 0;                                       // ac_Params[3]
        for (int levelNum = 0; levelNum < NUM_LEVELS; ++levelNum) {
            if ((levelMask & (1 << levelNum)) != 0) {
                if (overall) {
                    count = (count + Mem.uw(game_PlayerProgression + GStatT_LevelFailCounts_vw + levelNum * 2)) & 0xFFFF; // count += gs_LevelFailCounts[levelNum]
                } else {
                    count = Mem.uw(game_PlayerProgression + GStatT_LevelFailCounts_vw + levelNum * 2); // count = gs_LevelFailCounts[levelNum]
                }
                if (count >= countLimit) {
                    return TRUE != 0;
                }
            }
        }
        return false;
    }

    /**
     * Dispatch équivalent à game_AchievementRules[ruleId](achievement).
     * Ordre : KillCount, GroupKillCount, ZoneFound, LevelTimeImproved, TimesDied, StuffCollected.
     */
    private static boolean callRule(int ruleId, int achievement) {
        switch (ruleId) {
            case 0: return game_AchievementRuleKillCount(achievement);
            case 1: return game_AchievementRuleGroupKillCount(achievement);
            case 2: return game_AchievementRuleZoneFound(achievement);
            case 3: return game_AchievementRuleLevelTimeImproved(achievement);
            case 4: return game_AchievementRuleTimesDied(achievement);
            case 5: return game_AchievementRuleStuffCollected(achievement);
            default: return false;
        }
    }

    /**
     * game_ApplyAchievementReward — applique la récompense (caps puis bonus instantanés).
     */
    private static void game_ApplyAchievementReward(int achievement) {
        Message.Msg_PushLine(Mem.l(achievement + AchievementT_RewardDesc_l), MSG_TAG_OPTIONS | 80); // Msg_PushLine(ac_RewardDesc, MSG_TAG_OPTIONS|80)

        int player_ic = Plr1_Invetory_vw + InvT_Consumables;                        // &Plr1_Inventory.inv_Consumables

        // D'abord, modifications de cap.
        int capH = game_ModProps + GModT_MaxInv + InvCT_Health_w;
        Mem.ww(capH, Mem.uw(capH) + Mem.uw(achievement + AchievementT_HealthCapBonus_w)); // gmp_MaxInventory.ic_Health += ac_HealthCapBonus
        int capF = game_ModProps + GModT_MaxInv + InvCT_JetpackFuel_w;
        Mem.ww(capF, Mem.uw(capF) + Mem.uw(achievement + AchievementT_FuelCapBonus_w));    // gmp_MaxInventory.ic_JetpackFuel += ac_FuelCapBonus

        // Bonus instantanés.
        int plrH = player_ic + InvCT_Health_w;
        Mem.ww(plrH, Mem.uw(plrH) + Mem.uw(achievement + AchievementT_HealthBonus_w));     // player_ic->ic_Health += ac_HealthBonus

        int ammoType = Mem.w(achievement + AchievementT_AmmoType_w);                // ac_AmmoType (WORD signé)
        if (ammoType > -1 && ammoType < NUM_BULLET_DEFS) {                          // if (ac_AmmoType > -1 && ac_AmmoType < NUM_BULLET_DEFS)
            int capA = game_ModProps + GModT_MaxInv + InvCT_AmmoCounts_vw + ammoType * 2;
            Mem.ww(capA, Mem.uw(capA) + Mem.uw(achievement + AchievementT_AmmoTypeCapBonus_w)); // gmp.ic_AmmoCounts[ammoType] += ac_AmmoTypeCapBonus
            int plrA = player_ic + InvCT_AmmoCounts_vw + ammoType * 2;
            Mem.ww(plrA, Mem.uw(plrA) + Mem.uw(achievement + AchievementT_AmmoTypeBonus_w));    // player_ic->ic_AmmoCounts[ammoType] += ac_AmmoTypeBonus
        }

        // Applique les caps (mis à jour) à l'inventaire.
        GameC.Game_ApplyInventoryLimits(Plr1_Invetory_vw);                          // Game_ApplyInventoryLimits(&Plr1_Inventory)
    }

    /**
     * Game_UpdatePlayerProgress — appelé en fin de frame quand Game_ProgressSignal != 0.
     */
    public static void Game_UpdatePlayerProgress() {
        int achievements = Mem.l(game_AchievementsDataPtr_l);                       // game_AchievementsDataPtr
        for (int id = 0; id < Mem.uw(game_ModProps + GModT_NumAchievements); ++id) { // for (id=0; id<gmp_NumAchievements; ++id)
            int ach = achievements + id * AchievementT_SizeOf_l;                    // &achievements[id]
            // Early-out si le masque de règle n'intersecte pas le signal, ou déjà obtenu.
            if (
                (Mem.l(Game_ProgressSignal_l) & Mem.uw(ach + AchievementT_RuleMask_w)) == 0 // !(Game_ProgressSignal & ac_RuleMask)
                || game_CheckAchieved(id)                                           // || game_CheckAchieved(id)
            ) {
                continue;
            }

            if (callRule(Mem.uw(ach + AchievementT_RuleId_w), ach)) {               // if (game_AchievementRules[ac_RuleId](&achievements[id]))
                Message.Msg_PushLine(Mem.l(ach + AchievementT_Name_l), MSG_TAG_OPTIONS | 80); // Msg_PushLine(ac_Name, MSG_TAG_OPTIONS|80)
                if (Mem.l(ach + AchievementT_RewardDesc_l) != 0) {                  // if (ac_RewardDesc)
                    game_ApplyAchievementReward(ach);                              //   game_ApplyAchievementReward(&achievements[id])
                }
                game_MarkAchieved(id);                                              // game_MarkAchieved(id)
            }
        }
        Mem.wl(Game_ProgressSignal_l, 0);                                          // Game_ProgressSignal = 0
    }

    /**
     * Game_LevelBegin — début de niveau : compte la tentative, affiche le meilleur
     * temps si déjà joué, et déclenche la mise à jour de progression.
     */
    public static void Game_LevelBegin() {
        SysTimer.GetSysTime(game_LevelBegin);                                       // GetSysTime(&game_LevelBegin)
        int lvl = Mem.uw(Game_LevelNumber_w);
        int play = game_PlayerProgression + GStatT_LevelPlayCounts_vw + lvl * 2;
        Mem.ww(play, Mem.uw(play) + 1);                                             // ++gs_LevelPlayCounts[Game_LevelNumber]

        if (Mem.l(game_PlayerProgression + GStatT_LevelBestTimes_vl + lvl * 4) != 0) { // if (gs_LevelBestTimes[Game_LevelNumber])
            long time = Mem.l(game_PlayerProgression + GStatT_LevelBestTimes_vl + lvl * 4) & 0xFFFFFFFFL; // ULONG time = gs_LevelBestTimes[...]

            // UWORD data[5] = {0,0,0,0,0};
            Mem.ww(lvlBegin_data + 8, (int) (time % 100)); time /= 100;            // data[4] = time % 100; time /= 100;
            Mem.ww(lvlBegin_data + 6, (int) (time % 60));  time /= 60;             // data[3] = time % 60;  time /= 60;
            Mem.ww(lvlBegin_data + 4, (int) (time % 60));  time /= 60;             // data[2] = time % 60;  time /= 60;
            Mem.ww(lvlBegin_data + 2, (int) time);                                 // data[1] = time;
            Mem.ww(lvlBegin_data + 0, 'A' + lvl);                                  // data[0] = (UWORD)('A' + Game_LevelNumber);

            int[] outPtr = { game_BestLevelTimeBuffer_vb };                        // char* outPtr = game_BestLevelTimeBuffer;
            ExecLib.RawDoFmt(lvlBegin_fmt, lvlBegin_data, (ch) -> {                // RawDoFmt(fmt, &data, PutChProc, &outPtr)
                Mem.wb(outPtr[0], ch);
                outPtr[0]++;
            });

            Message.Msg_PushLine(game_BestLevelTimeBuffer_vb, MSG_TAG_OPTIONS | (outPtr[0] - game_BestLevelTimeBuffer_vb)); // Msg_PushLine(buf, MSG_TAG_OPTIONS|(outPtr - buf))
        }

        Mem.wl(Game_ProgressSignal_l, Mem.l(Game_ProgressSignal_l) | (1 << GAME_EVENTBIT_LEVEL_START)); // Game_ProgressSignal |= (1 << GAME_EVENTBIT_LEVEL_START)
        Game_UpdatePlayerProgress();                                               // Game_UpdatePlayerProgress()
    }

    /**
     * Game_LevelWon — niveau réussi : calcule la durée, met à jour le meilleur temps.
     */
    public static void Game_LevelWon() {
        SysTimer.GetSysTime(game_LevelEnd);                                        // GetSysTime(&game_LevelEnd)
        SysTimer.SubTime(game_LevelEnd, game_LevelBegin);                          // SubTime(&game_LevelEnd, &game_LevelBegin)
        int lvl = Mem.uw(Game_LevelNumber_w);
        int won = game_PlayerProgression + GStatT_LevelWonCounts_vw + lvl * 2;
        Mem.ww(won, Mem.uw(won) + 1);                                              // ++gs_LevelWonCounts[Game_LevelNumber]

        long secs = Mem.l(game_LevelEnd + 0) & 0xFFFFFFFFL;                        // game_LevelEnd.tv_sec
        long micro = Mem.l(game_LevelEnd + 4) & 0xFFFFFFFFL;                       // game_LevelEnd.tv_usec
        long elapsedCentis = (secs * 100) + (micro / 10000);                       // (tv_sec*100) + (tv_usec/10000)

        int bestPtr = game_PlayerProgression + GStatT_LevelBestTimes_vl + lvl * 4;
        long best = Mem.l(bestPtr) & 0xFFFFFFFFL;
        if (0 == best) {                                                           // if (0 == gs_LevelBestTimes[...])
            Mem.wl(bestPtr, (int) elapsedCentis);                                  // gs_LevelBestTimes[...] = elapsedCentis
        } else if (elapsedCentis < best) {                                         // else if (elapsedCentis < gs_LevelBestTimes[...])
            Mem.wl(bestPtr, (int) elapsedCentis);                                  // gs_LevelBestTimes[...] = elapsedCentis
            int imp = game_PlayerProgression + GStatT_LevelImprovedTimeCounts_vw + lvl * 2;
            Mem.ww(imp, Mem.uw(imp) + 1);                                          // ++gs_LevelImprovedTimeCounts[...]
        }
    }

    /**
     * Game_LevelFailed — niveau échoué : incrémente le compteur d'échecs.
     */
    public static void Game_LevelFailed() {
        int lvl = Mem.uw(Game_LevelNumber_w);
        int fail = game_PlayerProgression + GStatT_LevelFailCounts_vw + lvl * 2;
        Mem.ww(fail, Mem.uw(fail) + 1);                                            // ++gs_LevelFailCounts[Game_LevelNumber]
    }
}
