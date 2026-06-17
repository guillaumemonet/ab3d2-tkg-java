package ab3d2.host;

import ab3d2.Hires;
import ab3d2.Mem;
import ab3d2.bss.Bss;
import ab3d2.data.DataSections;
import ab3d2.c.ScreenC;
import ab3d2.c.SystemC;
import ab3d2.modules.FileIo;
import ab3d2.modules.Res;

import static ab3d2.bss.PlayerBss.*;
import static ab3d2.bss.ZoneBss.Zone_OrderTable_Barrier_w;
import static ab3d2.bss.TablesBss.ConstantTable_vl;
import static ab3d2.HiresData.GLF_DatabaseName_vb;
import static ab3d2.HiresData.GLF_DatabasePtr_l;
import static ab3d2.HiresData.draw_GouraudFlatsSelected_b;

/**
 * Harnais d'INTÉGRATION (hors jeu) — Phase 2 : tente de charger et d'afficher un
 * niveau réel en court-circuitant le menu (Game_Start est un stub Phase 7).
 *
 * Réplique la séquence runnable de hires.s::_startup + controlloop.s::Game_Start
 * (bring-up, ouverture écran, chargement base GLF + assets niveau) puis appelle
 * Hires.Game_Begin() directement. Limité à un nombre de frames pour ne pas boucler.
 *
 * NB : le joueur reste statique (VBlankInterrupt::dosomething — entrée/physique —
 * est différé). On veut surtout vérifier que le niveau se charge et se rend.
 */
public final class LevelTest {

    private LevelTest() {
    }

    /** Lit une chaîne C de longueur fixe (tronquée au premier 0). */
    private static String cstr(int addr, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            int c = Mem.ub(addr + i);
            if (c == 0) break;
            sb.append((char) c);
        }
        return sb.toString().trim();
    }

    /** DIAG : dump des défs d'objets (GLF) + instances du niveau ; repère les modèles vectoriels (relief). */
    private static void dumpObjects() {
        int glf = Mem.l(GLF_DatabasePtr_l);
        System.out.println("\n========== TEXTURES DE MUR (GLFT_WallGFXNames) ==========");
        for (int i = 0; i < ab3d2.Defs.NUM_WALL_TEXTURES; i++) {
            String wname = cstr(glf + ab3d2.Defs.GLFT_WallGFXNames_l + i * 64, 64);
            int gptr = Mem.l(ab3d2.bss.DrawBss.Draw_GlobalWallTexturePtrs_vl + i * 4);
            if (wname.isEmpty() && gptr == 0) continue;
            System.out.printf("  wall %2d : nom=\"%s\" chargé=%s (ptr=%d)%n",
                i, wname, (gptr != 0 ? "OUI" : "### NON"), gptr);
        }
        System.out.println("\n========== DÉFINITIONS D'OBJETS (GLF, vectoriel = en relief) ==========");
        for (int i = 0; i < ab3d2.Defs.NUM_OBJECT_DEFS; i++) {
            int def = glf + ab3d2.Defs.GLFT_ObjectDefs + i * ab3d2.Defs.ODefT_SizeOf_l;
            int gfx = Mem.w(def + ab3d2.Defs.ODefT_GFXType_w);
            int beh = Mem.w(def + ab3d2.Defs.ODefT_Behaviour_w);
            int fc  = Mem.w(def + ab3d2.Defs.ODefT_FloorCeiling_w);
            int lw  = Mem.w(def + ab3d2.Defs.ODefT_LockToWall_w);
            String oname = cstr(glf + ab3d2.Defs.GLFT_ObjectNames_l + i * 20, 20);
            String vname = cstr(glf + ab3d2.Defs.GLFT_VectorNames_l + i * 64, 64);
            String sname = cstr(glf + ab3d2.Defs.GLFT_ObjGfxNames_l + i * 64, 64);
            if (oname.isEmpty() && vname.isEmpty() && sname.isEmpty() && gfx == 0 && beh == 0) continue;
            System.out.printf("  def %2d : nom=\"%s\" GFXType=%d %s beh=%d FloorCeil=%d LockWall=%d  sprite=\"%s\" vecteur=\"%s\"%n",
                i, oname, gfx, (gfx == 1 ? "[VECTEUR/RELIEF]" : "[sprite]"), beh, fc, lw, sname, vname);
        }

        System.out.println("\n========== DÉFINITIONS D'ALIENS (GLF) ==========");
        for (int i = 0; i < ab3d2.Defs.NUM_ALIEN_DEFS; i++) {
            int def = glf + ab3d2.Defs.GLFT_AlienDefs_l + i * ab3d2.Defs.AlienT_SizeOf_l;
            int gfx = Mem.w(def + ab3d2.Defs.AlienT_GFXType_w);
            String aname = cstr(glf + ab3d2.Defs.GLFT_AlienNames_l + i * 20, 20);
            if (aname.isEmpty() && gfx == 0) continue;
            System.out.printf("  alien %2d : nom=\"%s\" GFXType=%d %s%n",
                i, aname, gfx, (gfx == 1 ? "[VECTEUR/RELIEF]" : "[sprite]"));
        }

        System.out.println("\n========== INSTANCES DU NIVEAU (objets placés) ==========");
        int objData = Mem.l(ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l);
        int objPts  = Mem.l(ab3d2.bss.LevelBss.Lvl_ObjectPointsPtr_l);
        int n = 0, vectorObjs = 0;
        int a0 = objData - ab3d2.Defs.ObjT_SizeOf_l;
        while (n < 2000) {
            a0 += ab3d2.Defs.ObjT_SizeOf_l;
            int pidx = Mem.w(a0);                       // move.w (a0),d0 ; blt → fin
            if (pidx < 0) break;
            n++;
            int zone = Mem.w(a0 + ab3d2.Defs.ObjT_ZoneID_w);
            int typeID = Mem.ub(a0 + ab3d2.Defs.ObjT_TypeID_b);
            int defIdx = Mem.ub(a0 + ab3d2.Defs.EntT_Type_b);
            int x = Mem.w(objPts + (pidx & 0xFFFF) * 8);
            int z = Mem.w(objPts + (pidx & 0xFFFF) * 8 + 4);
            int gfx = -1, lw = 0, fc = 0; String name = "";
            if (typeID == 1) { // OBJ_TYPE_OBJECT
                int def = glf + ab3d2.Defs.GLFT_ObjectDefs + defIdx * ab3d2.Defs.ODefT_SizeOf_l;
                gfx = Mem.w(def + ab3d2.Defs.ODefT_GFXType_w);
                lw  = Mem.w(def + ab3d2.Defs.ODefT_LockToWall_w);
                fc  = Mem.w(def + ab3d2.Defs.ODefT_FloorCeiling_w);
                name = cstr(glf + ab3d2.Defs.GLFT_ObjectNames_l + defIdx * 20, 20);
            } else if (typeID == 0) { // OBJ_TYPE_ALIEN
                int def = glf + ab3d2.Defs.GLFT_AlienDefs_l + defIdx * ab3d2.Defs.AlienT_SizeOf_l;
                gfx = Mem.w(def + ab3d2.Defs.AlienT_GFXType_w);
                name = cstr(glf + ab3d2.Defs.GLFT_AlienNames_l + defIdx * 20, 20);
            }
            String tag = (gfx == 1 ? " [VECTEUR/RELIEF]" : "") + (lw != 0 ? " <<< SUR MUR" : "") + (fc != 0 ? " (plafond)" : "");
            if (gfx == 1) vectorObjs++;
            String typeName = (typeID == 0 ? "ALIEN" : typeID == 1 ? "OBJET" : typeID == 2 ? "PROJ" : typeID == 4 ? "PLR1" : typeID == 5 ? "PLR2" : ("T" + typeID));
            String vf = "";
            if (gfx == 1) {
                StringBuilder raw = new StringBuilder(" rawObjT[");
                for (int b = 0; b < 64; b++) raw.append(Mem.ub(a0 + b)).append(b == 63 ? "" : " ");
                vf = String.format(" [mark6=%d model8=%d frame10=%d Timer1(34)=%d Type(54)=%d]%s]",
                    Mem.ub(a0+6), Mem.uw(a0+8), Mem.uw(a0+10), Mem.uw(a0+34), Mem.ub(a0+54), raw);
            }
            System.out.printf("  #%3d type=%-5s def=%2d nom=\"%s\" GFX=%d LockWall=%d zone=%d X=%d Z=%d%s%s%n",
                n, typeName, defIdx, name, gfx, lw, zone, x, z, tag, vf);
        }
        System.out.println("  → " + n + " instances, dont " + vectorObjs + " en VECTEUR/RELIEF.");
    }

    public static void main(String[] args) {
        Bss.init();
        DataSections.init();
        ScreenC.setHostScale(3);

        int level = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        int frames = args.length > 1 ? Integer.parseInt(args[1]) : 600;

        try {
            // --- bring-up (Sys_Init : Draw_Init + Game_Init) ---
            int ok = SystemC.Sys_Init();
            System.out.println("[LevelTest] Sys_Init = " + ok);

            // --- inits de _startup (hors Game_Start) ---
            Mem.ww(Plr1_Energy_w, 191);
            Mem.ww(Plr2_Energy_w, 191);
            Mem.ww(Zone_OrderTable_Barrier_w, 0xFFFF);     // not.w de 0
            Mem.wb(draw_GouraudFlatsSelected_b, 0xFF);
            // méthode de contrôle par défaut = souris (build non-CD32)
            Mem.wb(Plr1_Keys_b, 0); Mem.wb(Plr1_Path_b, 0); Mem.wb(Plr1_Mouse_b, 0xFF); Mem.wb(Plr1_Joystick_b, 0);
            Mem.wb(Plr2_Keys_b, 0); Mem.wb(Plr2_Path_b, 0); Mem.wb(Plr2_Mouse_b, 0xFF); Mem.wb(Plr2_Joystick_b, 0);
            fillConstantTable();

            // --- préambule de Game_Start (sans menu) ---
            Mem.wb(Plr_MultiplayerType_b, 'n');             // PLR_SINGLE
            ScreenC.Vid_OpenMainScreen();
            System.out.println("[LevelTest] écran ouvert");

            long r = FileIo.IO_LoadFile(GLF_DatabaseName_vb); // base GLF (test.lnk)
            Mem.wl(GLF_DatabasePtr_l, FileIo.addr(r));
            System.out.println("[LevelTest] GLF database @ " + FileIo.addr(r) + " (len " + FileIo.len(r) + ")");

            try {
                FileIo.IO_InitQueue();
                Res.Res_LoadSoundFx();
                Res.Res_LoadWallTextures();
                Res.Res_LoadFloorsAndTextures();
                Res.Res_LoadObjects();
                FileIo.IO_FlushQueue();
                Res.Res_PatchSoundFx();
                System.out.println("[LevelTest] assets chargés");
            } catch (Throwable t) {
                System.out.println("[LevelTest] chargement assets partiel (" + t + ") — on continue");
            }

            if (System.getProperty("altTex") != null) {     // TEST : remplace le fichier texture maps chargé
                try {
                    byte[] alt = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(System.getProperty("altTex")));
                    int dst = Mem.l(ab3d2.bss.DrawBss.Draw_TextureMapsPtr_l);
                    System.arraycopy(alt, 0, Mem.RAM, dst, alt.length);
                    System.out.println("[LevelTest] texture maps remplacé par " + System.getProperty("altTex") + " (" + alt.length + " octets @ " + dst + ")");
                } catch (Exception e) { System.out.println("[LevelTest] altTex échec : " + e); }
            }

            // diagnostic : nom du fichier musique attendu (GLF + GLFT_LevelMusic_l)
            System.out.println("[LevelTest] musique attendue = \"" + Mem.cstr(FileIo.addr(r) + 85184) + "\"");

            // --- niveau + lancement ---
            Mem.ww(ab3d2.ControlloopData.Game_LevelNumber_w, level);
            ab3d2.Controlloop.DEFAULTGAME();                // santé 200, armes, munitions (template Plr_*)
            // Réplique game_DoneMenu (controlloop.s:152-167) : distribue le template Plr_* vers
            // Plr1_*/Plr2_* (REPT 11 = consommables Santé/Fuel/Ammo[20] ; REPT 6 = items Shield/
            // Jetpack/Weapons[10]). Le harnais court-circuite le menu, donc cette copie manquait
            // → Plr1_AmmoCounts restait à 0 → arme « vide » (le tir jouait le son « pas de munition »).
            for (int i = 0; i < 11; i++) {                  // REPT 11 : Plr_Health.. → Plr1/Plr2
                int v = Mem.l(ab3d2.bss.PlayerBss.Plr_Health_w + i * 4);
                Mem.wl(Plr1_Health_w + i * 4, v);
                Mem.wl(Plr2_Health_w + i * 4, v);
            }
            for (int i = 0; i < 6; i++) {                   // REPT 6 : Plr_Shield.. → Plr1/Plr2
                int v = Mem.l(ab3d2.bss.PlayerBss.Plr_Shield_w + i * 4);
                Mem.wl(Plr1_Shield_w + i * 4, v);
                Mem.wl(Plr2_Shield_w + i * 4, v);
            }
            Mem.ww(ab3d2.HiresData.MAPON, 0);               // désactive l'automap (isole la scène 3D)
            Mem.ww(Plr1_Health_w, 200);                     // force les 2 santés (la boucle teste Plr1 ET Plr2)
            Mem.ww(Plr2_Health_w, 200);
            // TEST orientation : forcer un angle de départ (4096 = 180°) pour regarder la pièce
            int ang = args.length > 2 ? Integer.parseInt(args[2]) : 0;
            if (ang != 0) {
                Mem.ww(Plr1_SnapAngPos_w, ang);
                Mem.ww(Plr1_AngPos_w, ang);
                System.out.println("[LevelTest] angle forcé = " + ang);
            }
            if (System.getProperty("spawnAt") != null) {    // test : "X Z zone" — relocalise le spawn J1 (coords monde)
                String[] sp = System.getProperty("spawnAt").split(" ");
                ab3d2.modules.Player.dbgSpawnX = Integer.parseInt(sp[0]);
                ab3d2.modules.Player.dbgSpawnZ = Integer.parseInt(sp[1]);
                ab3d2.modules.Player.dbgSpawnZone = Integer.parseInt(sp[2]);
                System.out.println("[LevelTest] spawn relocalisé : X=" + sp[0] + " Z=" + sp[1] + " zone=" + sp[2]);
            }
            if (System.getProperty("fullScreen") != null) { // test : force le mode (1=plein écran, 0=petit écran HUD)
                int fs = "1".equals(System.getProperty("fullScreen")) ? 0xFF : 0;
                Mem.wb(ab3d2.bss.VidBss.Vid_FullScreen_b, fs);
                Mem.wb(ab3d2.bss.VidBss.Vid_FullScreenTemp_b, fs);
                System.out.println("[LevelTest] mode " + (fs != 0 ? "PLEIN ÉCRAN" : "PETIT ÉCRAN HUD") + " forcé");
            }
            if ("1".equals(System.getProperty("showMap"))) { // test : active l'automap (TAB en jeu réel)
                Mem.wb(ab3d2.HiresData.MAPON, 0xFF);            // MAPON lu en octet (move.b MAPON)
                System.out.println("[LevelTest] automap ACTIVÉE (test)");
            }
            if (System.getProperty("forceWall") != null) {  // test : force la couleur des murs gouraud
                ab3d2.Hiresgourwall.dbgForceWallPen = Integer.parseInt(System.getProperty("forceWall"));
            }
            if (System.getProperty("clearByte") != null) {  // test : couleur sentinelle du clear (trous de couverture)
                ab3d2.c.DrawC.dbgClearByte = Integer.parseInt(System.getProperty("clearByte"));
            }
            if ("1".equals(System.getProperty("fullbright"))) { // test : plein-éclat (DEV_SKIP_LIGHTING bit 11)
                Mem.wl(ab3d2.modules.DevInst.Dev_DebugFlags_l, Mem.l(ab3d2.modules.DevInst.Dev_DebugFlags_l) | (1 << 11));
                System.out.println("[LevelTest] plein-éclat activé");
            }
            if (System.getProperty("fov") != null) {       // test : élargir le FOV edge-PVS
                ab3d2.c.ZoneEdgePvs.Zone_PVSFieldOfView = Integer.parseInt(System.getProperty("fov"));
                System.out.println("[LevelTest] Zone_PVSFieldOfView = " + ab3d2.c.ZoneEdgePvs.Zone_PVSFieldOfView);
            }
            Hires.loopFrameLimit = frames;
            if ("1".equals(System.getProperty("killPlayer"))) { // test : déclenche endlevel (mort)
                Mem.ww(Plr1_Health_w, 0);
                System.out.println("[LevelTest] santé J1 forcée à 0 (test endlevel)");
            }
            if (System.getProperty("wavOut") != null) {     // DIAG : capture le mix audio en WAV
                ab3d2.host.Audio.dbgCapture = true;
                System.out.println("[LevelTest] capture audio WAV → " + System.getProperty("wavOut"));
            }
            if (System.getProperty("testSfx") != null) {    // DIAG : injecte un SFX de test (numéro de sample)
                Hires.dbgTestSfx = Integer.parseInt(System.getProperty("testSfx"));
                System.out.println("[LevelTest] SFX de test injecté = sample " + Hires.dbgTestSfx);
            }
            if (System.getProperty("telefxFrame") != null) { // DIAG : force l'effet shimmer téléport
                ScreenC.dbgForceTeleFrame = Integer.parseInt(System.getProperty("telefxFrame"));
                System.out.println("[LevelTest] shimmer téléport forcé frame " + ScreenC.dbgForceTeleFrame);
            }
            if ("1".equals(System.getProperty("testMsg"))) { // DIAG : pousse un message de test (frame 10)
                Hires.dbgTestMsg = true;
                System.out.println("[LevelTest] message de test poussé à la frame 10");
            }
            if ("1".equals(System.getProperty("testFire"))) { // DIAG : simule le clic gauche (tir) chaque frame
                Hires.dbgForceFire = true;
                System.out.println("[LevelTest] tir (clic gauche) simulé chaque frame");
            }
            if ("1".equals(System.getProperty("testSwitch"))) { // DIAG : teste le changement d'arme
                for (int o = 1; o <= 19; o += 2) Mem.wb(Plr1_Weapons_vb + o, 0xFF); // toutes les armes possédées
                Hires.dbgSwitchTest = true;
                System.out.println("[LevelTest] test changement d'arme (droite souris simulée)");
            }
            if ("1".equals(System.getProperty("testWeapons"))) { // DIAG : teste les 10 armes
                for (int o = 1; o <= 19; o += 2) Mem.wb(Plr1_Weapons_vb + o, 0xFF); // toutes les armes
                for (int b = 0; b < ab3d2.Defs.NUM_BULLET_DEFS; b++) Mem.ww(Plr1_AmmoCounts_vw + b * 2, 1000); // munitions
                Hires.dbgWeaponTest = true;
                Hires.loopFrameLimit = ab3d2.Defs.NUM_GUN_DEFS * Hires.DBG_FRAMES_PER_GUN + 5; // ~155 frames
                System.out.println("[LevelTest] test des " + ab3d2.Defs.NUM_GUN_DEFS + " armes (cycle + tir)");
            }
            if ("1".equals(System.getProperty("testPickup"))) { // DIAG : trace la collecte d'objets
                ab3d2.Newaliencontrol.dbgCollect = true;
                System.out.println("[LevelTest] trace collecte (pickup) activée");
            }
            if ("1".equals(System.getProperty("cheesy"))) { // TEST : atlas objet 1 octet/texel (CHEESEY=1)
                ab3d2.Objdrawhires.dbgCheesy = true;
                System.out.println("[LevelTest] atlas objet CHEESEY=1 (1 octet/texel)");
            }
            if (System.getProperty("texLog") != null) { // DIAG : trace offset texture d'un modèle vectoriel
                ab3d2.Objdrawhires.dbgTex = true;
                ab3d2.Objdrawhires.dbgTexModel = Integer.parseInt(System.getProperty("texLog"));
            }
            if (System.getProperty("forceFrame") != null) { // TEST : force la frame du modèle 11 (passkey)
                ab3d2.Objdrawhires.dbgTexModel = 11;
                ab3d2.Objdrawhires.dbgForceFrame = Integer.parseInt(System.getProperty("forceFrame"));
                System.out.println("[LevelTest] frame du modèle 11 forcée à " + ab3d2.Objdrawhires.dbgForceFrame);
            }
            System.out.println("[LevelTest] Game_Begin (niveau " + level + ", " + frames + " frames)...");
            Hires.Game_Begin();
            System.out.println("[LevelTest] Game_Begin terminé.");
            if (System.getProperty("animDump") != null) { // DIAG : anim DEFANIMOBJ d'un def objet
                int def = Integer.parseInt(System.getProperty("animDump"));
                int glfb = Mem.l(GLF_DatabasePtr_l);
                int a3 = glfb + ab3d2.Defs.GLFT_ObjectDefAnims_l + def * ab3d2.Defs.O_AnimSize;
                System.out.println("[animDump] DEFANIMOBJ def " + def + " (frame: graphic angleDelta nextTimer) :");
                for (int f = 0; f < 20; f++) {
                    int g = Mem.ub(a3 + f * 6), b1 = Mem.ub(a3 + f * 6 + 1), angD = Mem.w(a3 + f * 6 + 2),
                        b4 = Mem.ub(a3 + f * 6 + 4), nt = Mem.ub(a3 + f * 6 + 5);
                    System.out.printf("    frame %2d : graphic(b0)=%3d FRAME(b1)=%3d angleDelta(b2)=%6d b4=%3d nextTimer(b5)=%3d%n",
                        f, g, b1, angD, b4, nt);
                    if (nt == 0 && f > 0) break;
                }
            }
            if (ab3d2.Objdrawhires.dbgTex) {
                System.out.println("[texLog] offsets texture du modèle " + ab3d2.Objdrawhires.dbgTexModel + " :");
                for (String s : ab3d2.Objdrawhires.dbgTexLog) System.out.println("    " + s);
                int glfb = Mem.l(GLF_DatabasePtr_l);
                System.out.println("[texLog] GLFT_TextureFilename = \"" + cstr(glfb + 6528, 192) + "\"");
                System.out.println("[texLog] GLFT_FloorFilename = \"" + cstr(glfb + 6464, 64) + "\"");
                int maps = Mem.l(ab3d2.bss.DrawBss.Draw_TextureMapsPtr_l);
                for (int off : new int[]{0, 257, 65536, 66307, 131072}) {
                    StringBuilder b = new StringBuilder("[texLog] maps+" + off + " : ");
                    for (int i = 0; i < 16; i++) b.append(Mem.ub(maps + off + i)).append(' ');
                    System.out.println(b);
                }
            }
            if ("1".equals(System.getProperty("dumpObjects"))) dumpObjects();
            // diagnostic audio (arbitrage musique/SFX)
            System.out.println("[diag-audio] UseAllChannels=" + Mem.ub(ab3d2.HiresData.UseAllChannels)
                + " CHANNELDATA[0]=" + Mem.ub(ab3d2.HiresData.CHANNELDATA)
                + " CHANNELDATA+16=" + Mem.ub(ab3d2.HiresData.CHANNELDATA + 16)
                + " | Paula PER[0..3]=" + ab3d2.host.CustomChips.audPER[0] + "," + ab3d2.host.CustomChips.audPER[1]
                + "," + ab3d2.host.CustomChips.audPER[2] + "," + ab3d2.host.CustomChips.audPER[3]
                + " VOL=" + ab3d2.host.CustomChips.audVOL[0] + "," + ab3d2.host.CustomChips.audVOL[1]
                + "," + ab3d2.host.CustomChips.audVOL[2] + "," + ab3d2.host.CustomChips.audVOL[3]);
            System.out.println("[diag-audio] audLC=" + ab3d2.host.CustomChips.audLC[0] + "," + ab3d2.host.CustomChips.audLC[1]
                + "," + ab3d2.host.CustomChips.audLC[2] + "," + ab3d2.host.CustomChips.audLC[3]
                + " audLEN=" + ab3d2.host.CustomChips.audLEN[0] + "," + ab3d2.host.CustomChips.audLEN[1]
                + "," + ab3d2.host.CustomChips.audLEN[2] + "," + ab3d2.host.CustomChips.audLEN[3]);
            if ("1".equals(System.getProperty("testFire"))) { // DIAG tir : état + projectiles actifs
                System.out.println("[diag-fire] Plr1_Fire_b=" + Mem.ub(ab3d2.bss.PlayerBss.Plr1_Fire_b)
                    + " Plr1_TmpFire_b=" + Mem.ub(ab3d2.bss.PlayerBss.Plr1_TmpFire_b)
                    + " Plr1_Clicked_b=" + Mem.ub(ab3d2.bss.PlayerBss.Plr1_Clicked_b)
                    + " ciaaPraBit6(libre)=" + ab3d2.host.CustomChips.ciaaPraBit6()
                    + " Aud_SampleNum=" + Mem.uw(ab3d2.HiresData.Aud_SampleNum_w));
                int objData = Mem.l(ab3d2.bss.LevelBss.Lvl_ObjectDataPtr_l);
                int proj = 0, a0 = objData;
                for (int k = 0; k < 200; k++, a0 += ab3d2.Defs.ObjT_SizeOf_l) {
                    int pidx = Mem.w(a0); if (pidx < 0) break;
                    if (Mem.ub(a0 + ab3d2.Defs.ObjT_TypeID_b) == 2 && Mem.w(a0 + ab3d2.Defs.ObjT_ZoneID_w) >= 0) proj++;
                }
                System.out.println("[diag-fire] projectiles actifs (PROJ zone>=0) = " + proj);
            }
            if (Hires.dbgTestSfx >= 0 || "1".equals(System.getProperty("testFire")))
                System.out.println("[diag-sfx] maxSoftVol(voies soft armées par MakeSomeNoise)=" + Hires.dbgMaxSoftVol
                    + " maxSfxPaulaVol(ch1-3)=" + Hires.dbgMaxSfxPaulaVol);
            // diagnostics rendu
            int zp = Mem.l(Plr1_ZonePtr_l);
            System.out.println("[diag] Lvl_NumZones=" + Mem.uw(ab3d2.bss.LevelBss.Lvl_NumZones_w)
                + " Plr1_ZonePtr=" + zp + " (zoneID=" + (zp != 0 ? Mem.w(zp) : -999) + ")"
                + " XOff=" + Mem.l(Plr1_XOff_l) + " ZOff=" + Mem.l(Plr1_ZOff_l)
                + " YOff=" + Mem.l(Plr1_YOff_l) + " AngPos=" + Mem.uw(Plr1_AngPos_w));
            int pvs = Mem.l(Plr1_PotVisibleZoneListPtr_l);
            System.out.println("[diag] PotVisListPtr=" + pvs
                + " entry0=" + (pvs != 0 ? Mem.w(pvs) : -999));
            if (pvs != 0) {
                StringBuilder zlist = new StringBuilder();
                int p = pvs, cnt = 0;
                while (cnt < 200 && Mem.w(p) >= 0) { zlist.append(Mem.w(p)).append(' '); p += 8; cnt++; }
                System.out.println("[diag] PotVisList (" + cnt + " zones visibles) : " + zlist);
            }
            int fast = Mem.l(ab3d2.bss.VidBss.Vid_FastBufferPtr_l);
            int nonzero = 0;
            for (int i = 0; i < ScreenC.SCREEN_WIDTH * ScreenC.SCREEN_HEIGHT; i++) if (Mem.RAM[fast + i] != 0) nonzero++;
            System.out.println("[diag] pixels non-noirs dans le buffer = " + nonzero);
            if (System.getProperty("sampleCenter") != null) { // DIAG : couleurs au centre (objet visé)
                int W = ScreenC.SCREEN_WIDTH, H = ScreenC.SCREEN_HEIGHT;
                int rad = Integer.parseInt(System.getProperty("sampleCenter"));
                int cy = System.getProperty("sampleY") != null ? Integer.parseInt(System.getProperty("sampleY")) : H/2;
                java.util.Map<Integer,Integer> counts = new java.util.TreeMap<>();
                for (int y = cy - rad; y < cy + rad; y++)
                    for (int x = W/2 - rad; x < W/2 + rad; x++) {
                        int idx = Mem.RAM[fast + y * W + x] & 0xFF;
                        counts.merge(idx, 1, Integer::sum);
                    }
                System.out.println("[sample] index palette (×count) RGB dans " + (2*rad) + "² au centre :");
                counts.entrySet().stream().sorted((a,b)->b.getValue()-a.getValue()).limit(16).forEach(e -> {
                    int argb = ScreenC.hostColor(e.getKey());
                    System.out.printf("    idx=%3d ×%-4d  R=%3d G=%3d B=%3d%n",
                        e.getKey(), e.getValue(), (argb>>16)&0xFF, (argb>>8)&0xFF, argb&0xFF);
                });
            }
            if (System.getProperty("wavOut") != null) ab3d2.host.Audio.dumpWav(System.getProperty("wavOut"));
            ScreenC.saveScreenshot("level_screenshot.png");
        } catch (Throwable t) {
            ScreenC.saveScreenshot("level_screenshot.png");  // capture la dernière frame rendue
            System.out.println("[LevelTest] ÉCHEC : " + t);
            for (StackTraceElement e : t.getStackTrace()) {
                System.out.println("    at " + e);
            }
        } finally {
            ab3d2.host.Audio.stop();          // arrêt propre OpenAL avant teardown natif
            ScreenC.Vid_CloseMainScreen();
        }
    }

    /** Remplissage de ConstantTable_vl (table de mise à l'échelle des objets), cf. _startup. */
    private static void fillConstantTable() {
        int a0 = ConstantTable_vl;
        int d0 = 1;
        int d1 = 8191;
        do {
            int d2 = 16384 * 64;
            d2 = d2 / d0;
            int d3 = 64 * 64 * 65536;
            d3 = d3 / d2;
            Mem.wl(a0, d3); a0 += 4;
            d2 = d2 >> 1;
            d2 = d2 - 40 * 64;
            d2 = d2 * d3;
            d2 = d2 >> 6;
            Mem.wl(a0, d2); a0 += 4;
            d0 += 1;
        } while (--d1 != -1);
    }
}
