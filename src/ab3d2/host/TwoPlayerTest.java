package ab3d2.host;

import ab3d2.Controlloop;
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
 * Harnais de LANCEMENT 2 JOUEURS (lien TCP) — court-circuite le menu.
 *
 * Réplique le bring-up de {@link LevelTest} (Sys_Init, inits _startup, ouverture écran,
 * chargement GLF + assets), fixe {@code Plr_MultiplayerType_b} en master/slave, établit le
 * lien {@link SerialLink} (master = écoute, slave = connexion), puis lance {@link Hires#Game_Begin}
 * en INTERACTIF. Le handshake (Plr_InitMaster/Slave → n° de niveau + graine Rand1 + TWOPLAYER)
 * a lieu dans Game_Begin via SETPLAYERS ; le reste (~11 SENDFIRST/RECFIRST par frame) tourne
 * ensuite en lock-step. Chaque instance rend SA vue et pilote SON joueur (master = J1, slave = J2).
 *
 * <pre>
 *   # Terminal 1 (master, choisit le niveau) :
 *   gradle -p java twoPlayer -PnetArgs="master 0"
 *   # Terminal 2 (slave, se connecte au master) :
 *   gradle -p java twoPlayer -PnetArgs="slave 0 localhost"
 * </pre>
 *
 * Lancer le MASTER en premier (il écoute) ; le slave réessaie la connexion pendant 2 min.
 * Le n° de niveau du master fait foi (transmis au slave par le handshake).
 */
public final class TwoPlayerTest {

    private TwoPlayerTest() {
    }

    public static void main(String[] args) {
        String role = args.length > 0 ? args[0].toLowerCase() : "master";
        boolean master = role.startsWith("m");
        int level = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        String host = args.length > 2 ? args[2] : "localhost";
        int port = Integer.getInteger("netPort", SerialLink.DEFAULT_PORT);

        Bss.init();
        DataSections.init();
        ScreenC.setHostScale(3);

        try {
            int ok = SystemC.Sys_Init();
            System.out.println("[2P] Sys_Init = " + ok + " (rôle=" + (master ? "MASTER (J1)" : "SLAVE (J2)")
                    + (master ? ", mode=" + (Controlloop.coopMode ? "CO-OP" : "VERSUS")
                              : ", mode reçu du master au handshake") + ")");

            // --- inits de _startup (hors Game_Start) ---
            Mem.ww(Plr1_Energy_w, 191);
            Mem.ww(Plr2_Energy_w, 191);
            Mem.ww(Zone_OrderTable_Barrier_w, 0xFFFF);
            Mem.wb(draw_GouraudFlatsSelected_b, 0xFF);
            // contrôle par défaut = souris pour les DEUX joueurs (la lecture est routée par
            // master/slave dans Hires : master lit J1, slave lit J2 — cf. doPlayer1/2Controls).
            Mem.wb(Plr1_Keys_b, 0); Mem.wb(Plr1_Path_b, 0); Mem.wb(Plr1_Mouse_b, 0xFF); Mem.wb(Plr1_Joystick_b, 0);
            Mem.wb(Plr2_Keys_b, 0); Mem.wb(Plr2_Path_b, 0); Mem.wb(Plr2_Mouse_b, 0xFF); Mem.wb(Plr2_Joystick_b, 0);
            fillConstantTable();

            // --- mode 2 joueurs ---
            Mem.wb(Plr_MultiplayerType_b, master ? 'm' : 's');   // PLR_MASTER / PLR_SLAVE
            ScreenC.Vid_OpenMainScreen();
            System.out.println("[2P] écran ouvert");

            long r = FileIo.IO_LoadFile(GLF_DatabaseName_vb);    // base GLF (test.lnk)
            Mem.wl(GLF_DatabasePtr_l, FileIo.addr(r));

            try {
                FileIo.IO_InitQueue();
                Res.Res_LoadSoundFx();
                Res.Res_LoadWallTextures();
                Res.Res_LoadFloorsAndTextures();
                Res.Res_LoadObjects();
                FileIo.IO_QueueFile(ab3d2.data.DrawData.draw_BackdropImageName_vb,
                        ab3d2.bss.DrawBss.Draw_BackdropImagePtr_l, 0); // backdrop ciel
                FileIo.IO_FlushQueue();
                Res.Res_PatchSoundFx();
                System.out.println("[2P] assets chargés");
            } catch (Throwable t) {
                System.out.println("[2P] chargement assets partiel (" + t + ") — on continue");
            }

            // --- niveau + dotation de base (le handshake TWOPLAYER raffine la dotation 2J,
            //     identique des deux côtés car même Rand1 partagé) ---
            Mem.ww(ab3d2.ControlloopData.Game_LevelNumber_w, level);
            Controlloop.DEFAULTGAME();
            for (int i = 0; i < 11; i++) {                       // template Plr_* → Plr1/Plr2 (game_DoneMenu)
                int v = Mem.l(Plr_Health_w + i * 4);
                Mem.wl(Plr1_Health_w + i * 4, v);
                Mem.wl(Plr2_Health_w + i * 4, v);
            }
            for (int i = 0; i < 6; i++) {
                int v = Mem.l(Plr_Shield_w + i * 4);
                Mem.wl(Plr1_Shield_w + i * 4, v);
                Mem.wl(Plr2_Shield_w + i * 4, v);
            }
            Mem.ww(ab3d2.HiresData.MAPON, 0);                    // automap off au départ

            // --- établir le lien AVANT Game_Begin (le handshake SETPLAYERS y échange niveau+graine) ---
            if (master) {
                System.out.println("[2P] MASTER : écoute sur le port " + port + " — LANCEZ LE SLAVE MAINTENANT…");
                SerialLink.startMaster(port, 120_000);
                System.out.println("[2P] MASTER : slave connecté.");
            } else {
                System.out.println("[2P] SLAVE : connexion à " + host + ":" + port + " (réessais 2 min)…");
                SerialLink.startSlave(host, port, 120_000);
                System.out.println("[2P] SLAVE : connecté au master.");
            }

            // Smoke-test borné : -Dnet.frames=N arrête après N frames (défaut : interactif, illimité).
            int nf = Integer.getInteger("net.frames", -1);
            if (nf >= 0) {
                Hires.loopFrameLimit = nf;
                System.out.println("[2P] limite de " + nf + " frames (smoke-test)");
            }
            // Hook test : -Dnet.walk maintient « avancer » enfoncé (réinjecté chaque frame dans
            // VBlankInterrupt) → les 2 joueurs marchent, aggro les aliens, déclenchent FindCloseRoom.
            if (Boolean.getBoolean("net.walk")) {
                Hires.dbgForceWalk = true;
                System.out.println("[2P] hook : avancer maintenu (aggro aliens)");
            }

            System.out.println("[2P] Game_Begin (niveau " + level + ", interactif) — ESC pour quitter…");
            Hires.Game_Begin();
            System.out.println("[2P] Game_Begin terminé.");
            if (nf >= 0) {
                // Positions joueurs : DOIVENT être identiques master↔slave (forcées-synchro chaque frame).
                // (Le déterminisme des aliens en CO-OP se vérifie en jeu réel — non reproductible en headless.)
                System.out.printf("[2P] après %d frames : P1=(%d,%d)  P2=(%d,%d)%n",
                        nf, Mem.l(Plr1_XOff_l), Mem.l(Plr1_ZOff_l), Mem.l(Plr2_XOff_l), Mem.l(Plr2_ZOff_l));
            }
        } catch (Throwable t) {
            System.out.println("[2P] ÉCHEC : " + t);
            for (StackTraceElement e : t.getStackTrace()) {
                System.out.println("    at " + e);
            }
        } finally {
            ab3d2.host.Audio.stop();          // arrêt propre OpenAL
            SerialLink.close();               // ferme le lien TCP
            ScreenC.Vid_CloseMainScreen();
        }
    }

    /** Remplissage de ConstantTable_vl (mise à l'échelle des objets), cf. _startup. */
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
