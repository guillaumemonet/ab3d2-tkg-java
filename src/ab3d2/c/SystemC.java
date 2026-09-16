package ab3d2.c;

import ab3d2.Mem;
import ab3d2.host.CustomChips;
import ab3d2.host.SysTimer;
import ab3d2.modules.Sys;

import static ab3d2.HiresData.Vis_AngPos_w;
import static ab3d2.HiresData.Sys_MouseY;
import static ab3d2.bss.SystemBss.Sys_FrameTimeECV_q;
import static ab3d2.bss.SystemBss.Sys_PrevFrameTimeECV_q;
import static ab3d2.bss.SystemBss.Sys_FrameTimes_vl;
import static ab3d2.bss.SystemBss.Sys_FrameNumber_l;
import static ab3d2.bss.SystemBss.Sys_ECVToMsFactor_l;
import static ab3d2.bss.SystemBss.Sys_FPSIntAvg_w;
import static ab3d2.bss.SystemBss.Sys_FPSFracAvg_w;
import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.ControlloopData.Prefs_DisplayFPS_b;

/**
 * Traduction littérale de ab3d2_source/c/system.c.
 *
 * system.c est en grande partie du bring-up AmigaOS (ouverture de librairies,
 * serveurs d'interruptions VBlank/clavier, timer.device, détection CPU/Akiko,
 * port série, potgo). Ces fonctions relèvent de la couche hôte et ne sont pas
 * du code de jeu : elles sont conservées en stubs documentés (voir bas de
 * fichier), comme cd32joy.s.
 *
 * Les fonctions de LOGIQUE PURE (souris, FPS, chrono image, nettoyage clavier)
 * sont traduites fidèlement, en préservant les quirks (émulation add.b, division
 * 32/16 non signée, masque 11 bits, etc.).
 *
 * EClockVal = struct {ULONG ev_hi; ULONG ev_lo;} (8 octets big-endian). Le
 * tableau Sys_FrameTimeECV_q[2] est ici {Sys_FrameTimeECV_q, Sys_PrevFrameTimeECV_q}
 * (labels contigus en bss). ev_lo est à l'offset +4.
 */
public final class SystemC {

    // -- statics de Sys_ReadMouse (variables locales `static` du C) --
    private static int oldMouseY;    // WORD oldMouseY    (motif 16 bits 0..0xFFFF)
    private static int oldCounterX;  // UBYTE oldCounterX (0..0xFF)
    private static int oldMouseX2;   // UWORD oldMouseX2  (0..0xFFFF, usage masqué 2047)

    /**
     * Global C de system.c : ULONG Sys_EClockRate (fréquence horloge E, fixée par
     * Sys_Init). Par défaut = fréquence du timer hôte.
     */
    public static int Sys_EClockRate = SysTimer.ECLOCK_RATE;

    private SystemC() {
    }

    /**
     * Sys_CheckTimeGE (system.h inline) — vrai si now >= mark (EClockVal 64 bits non signés).
     */
    public static boolean Sys_CheckTimeGE(int now, int mark) {
        long nowHi = Mem.l(now) & 0xFFFFFFFFL;       // now->ev_hi
        long markHi = Mem.l(mark) & 0xFFFFFFFFL;     // mark->ev_hi
        long nowLo = Mem.l(now + 4) & 0xFFFFFFFFL;   // now->ev_lo
        long markLo = Mem.l(mark + 4) & 0xFFFFFFFFL; // mark->ev_lo
        return nowHi > markHi || (nowHi == markHi && nowLo >= markLo); // now->ev_hi > mark->ev_hi || (== && ev_lo >= ev_lo)
    }

    /**
     * Sys_AddTime (system.h inline) — *(uint64_t*)mark += ticks.
     */
    public static void Sys_AddTime(int mark, int ticks) {
        long v = ((long) Mem.l(mark) << 32) | (Mem.l(mark + 4) & 0xFFFFFFFFL);
        v += (ticks & 0xFFFFFFFFL);
        Mem.wl(mark, (int) (v >>> 32));
        Mem.wl(mark + 4, (int) v);
    }

    /**
     * Sys_MarkTime (CALLC) — a0 = EClockVal* dest : marque l'instant et renvoie
     * la fréquence de l'horloge E.
     */
    public static int Sys_MarkTime(int dest) {
        return SysTimer.readEClock(dest);                    // return ReadEClock(dest);
    }

    /**
     * Sys_TimeDiff (CALLC) — a0 = start, a1 = end : différence 64 bits end-start.
     */
    public static long Sys_TimeDiff(int start, int end) {
        long end64 = ((long) Mem.l(end) << 32) | (Mem.l(end + 4) & 0xFFFFFFFFL);     // *(uint64_t*)end
        long start64 = ((long) Mem.l(start) << 32) | (Mem.l(start + 4) & 0xFFFFFFFFL); // *(uint64_t*)start
        long diff = end64 - start64;                         // uint64_t diff = *(uint64_t*)end - *(uint64_t*)start;
        return diff;                                         // return diff;
    }

    /**
     * Sys_FrameLap (CALLC) — relève le temps de l'image courante dans le buffer
     * circulaire des 8 dernières durées.
     */
    public static void Sys_FrameLap() {
        SysTimer.readEClock(Sys_FrameTimeECV_q);             // ReadEClock(&Sys_FrameTimeECV_q[0]);
        int frameTime = Mem.l(Sys_FrameTimeECV_q + 4) - Mem.l(Sys_PrevFrameTimeECV_q + 4); // ULONG frameTime = [0].ev_lo - [1].ev_lo;
        Mem.wl(Sys_PrevFrameTimeECV_q, Mem.l(Sys_FrameTimeECV_q));         // Sys_FrameTimeECV_q[1] = Sys_FrameTimeECV_q[0];
        Mem.wl(Sys_PrevFrameTimeECV_q + 4, Mem.l(Sys_FrameTimeECV_q + 4)); //   (copie des 8 octets)
        Mem.wl(Sys_FrameTimes_vl + 4 * (Mem.l(Sys_FrameNumber_l) & 7), frameTime); // Sys_FrameTimes_vl[Sys_FrameNumber_l & 7] = frameTime;
        Mem.wl(Sys_FrameNumber_l, Mem.l(Sys_FrameNumber_l) + 1);          // ++Sys_FrameNumber_l;
    }

    /**
     * Sys_EvalFPS (CALLC) — moyenne les 8 dernières durées d'image et en déduit
     * les FPS *10 (parties entière et décimale). Division 32/16 non signée.
     */
    public static void Sys_EvalFPS() {
        if (Mem.b(Prefs_DisplayFPS_b) == 0) {                // if (!Prefs_DisplayFPS_b) return;  (build non-DEV)
            return;
        }
        long avg = 0;                                        // ULONG avg = 0;
        for (int x = 0; x < 8; ++x) {                        // for (int x = 0; x < 8; ++x)
            avg = (avg + (Mem.l(Sys_FrameTimes_vl + 4 * x) & 0xFFFFFFFFL)) & 0xFFFFFFFFL; // avg += Sys_FrameTimes_vl[x];
        }
        avg = ((avg * (Mem.l(Sys_ECVToMsFactor_l) & 0xFFFFFFFFL)) & 0xFFFFFFFFL) >>> 19;  // avg = (avg * Sys_ECVToMsFactor_l) >> 19;
        if (avg == 0) {                                      // if (!avg) return;
            return;
        }
        int fps10x = (10000 / (int) (avg & 0xFFFF)) & 0xFFFF; // UWORD fps10x = (UWORD)10000 / (UWORD)avg;
        Mem.ww(Sys_FPSFracAvg_w, fps10x % 10);               // Sys_FPSFracAvg_w = fps10x % 10;
        Mem.ww(Sys_FPSIntAvg_w, fps10x / 10);                // Sys_FPSIntAvg_w  = fps10x / 10;
    }

    /**
     * Sys_ReadMouse (CALLC) — lit le compteur souris matériel (joy0dat) et met
     * à jour Sys_MouseY (pitch) et Vis_AngPos_w (yaw). Préserve l'émulation
     * « add.b » de oldMouseY et le wrap des compteurs comme le code original.
     */
    public static void Sys_ReadMouse() {
        int joy = CustomChips.joy0dat & 0xFFFF;              // volatile UWORD joy0dat

        int diffY = (short) (joy >>> 8);                     // WORD diffY = joy0dat >> 8;
        diffY = (short) (diffY - (short) oldMouseY);         // diffY -= oldMouseY;
        if (diffY >= 127) {                                  // if (diffY >= 127)
            diffY = (short) (diffY - 255);                   //   diffY -= 255;
        } else if (diffY < -127) {                           // else if (diffY < -127)
            diffY = (short) (diffY + 255);                   //   diffY += 255;
        }

        // Émule le bizarre add.b sur l'octet bas du code original.
        oldMouseY = (oldMouseY & 0xff00) | ((diffY + oldMouseY) & 0xff); // oldMouseY = (oldMouseY & 0xff00) | ((diffY+oldMouseY) & 0xff);
        Mem.ww(Sys_MouseY, Mem.uw(Sys_MouseY) + diffY);      // Sys_MouseY += diffY;

        int counterX = joy & 0xff;                           // UWORD counterX = joy0dat & 0xff;
        int diffX = (short) (counterX - oldCounterX);        // WORD diffX = counterX - oldCounterX;
        if (diffX >= 127) {                                  // if (diffX >= 127)
            diffX = (short) (diffX - 255);                   //   diffX -= 255;
        } else if (diffX < -127) {                           // else if (diffX < -127)
            diffX = (short) (diffX + 255);                   //   diffX += 255;
        }

        int newMouseX = (short) (diffX + oldCounterX);       // WORD newMouseX = diffX + oldCounterX;
        oldCounterX = newMouseX & 0xff;                      // oldCounterX = newMouseX;  (UBYTE)

        oldMouseX2 = (oldMouseX2 + diffX) & 2047;            // oldMouseX2 = (oldMouseX2 + diffX) & 2047;

        // Pilote directement la rotation du joueur (sensibilité = diffX << 2).
        Mem.ww(Vis_AngPos_w, Mem.uw(Vis_AngPos_w) + (diffX << 2)); // Vis_AngPos_w += (diffX << 2);
    }

    /**
     * Sys_ClearKeyboard (CALLC) — efface la table clavier (256 octets = 64 longs).
     */
    public static void Sys_ClearKeyboard() {
        Sys.Sys_MemFillLong(KeyMap_vb, 0, 256 / 4);          // Sys_MemFillLong(KeyMap_vb, 0, 256/sizeof(ULONG));
    }

    // ------------------------------------------------------------------
    // Bring-up — couche hôte.
    //
    // L'original ouvre graphics/intuition, les resources misc/potgo, le
    // timer.device et installe des serveurs d'interruptions VBlank/clavier.
    // Côté hôte : les librairies OS deviennent des no-op, le timer est SysTimer
    // (System.nanoTime), et les « interruptions » sont pilotées par la boucle de
    // frame (Hires.VBlankInterrupt appelée une fois par image). La LOGIQUE de
    // Sys_Init (chrono initial + facteur ECV→ms, Draw_Init, Game_Init) est
    // conservée fidèlement.
    // ------------------------------------------------------------------

    /** Sys_Init : init matériel (host no-op), chrono initial, Draw_Init, Game_Init. */
    public static int Sys_Init() {
        // sys_InitHardware() : détection CPU (system.c:219). Cible = 68060 (move16 dispo).
        // Sur 040/060 : Sys_Move16_b = Vid_FullScreenTemp_b = 0xFF (→ plein écran par défaut).
        // Sys_CPU_68060_b : remis à 0 si move16 (logique buggée d'origine = « est-ce un 030 »).
        Mem.wb(ab3d2.bss.SystemBss.Sys_Move16_b, 0xFF);
        Mem.wb(ab3d2.bss.VidBss.Vid_FullScreenTemp_b, 0xFF);
        Mem.wb(ab3d2.bss.SystemBss.Sys_CPU_68060_b, 0);
        Sys_EClockRate = Sys_MarkTime(Sys_PrevFrameTimeECV_q); // Sys_EClockRate = Sys_MarkTime(&Sys_FrameTimeECV_q[1]);
        Mem.wl(Sys_ECVToMsFactor_l,
                (int) ((1000L << 16) / (Sys_EClockRate & 0xFFFFFFFFL))); // Sys_ECVToMsFactor_l = (1000<<16) / Sys_EClockRate;

        // sys_InstallInterrupts() : pilotées par la boucle de frame hôte (no-op).

        if (DrawC.Draw_Init() == 0) {                        // if (!Draw_Init()) goto fail;
            sys_ReleaseHardware();
            return 0;                                        // return FALSE;
        }

        GameC.Game_Init();                                   // Game_Init();
        return 1;                                            // return TRUE;
    }

    /** Sys_Done : Draw_Shutdown, retire interruptions (no-op), libère matériel, Game_Done. */
    public static void Sys_Done() {
        DrawC.Draw_Shutdown();                               // Draw_Shutdown();
        // sys_RemoveInterrupts() / sys_ReleaseHardware() : host no-op.
        sys_ReleaseHardware();
        GameC.Game_Done();                                   // Game_Done();
        // pr_WindowPtr restauré / Sys_DisplayError() : sans objet côté hôte.
    }

    /** Sys_OpenLibs : ouverture des librairies OS — host no-op (toujours succès). */
    public static int Sys_OpenLibs() {
        return 1;                                            // return TRUE;
    }

    /** Sys_CloseLibs : fermeture des librairies OS — host no-op. */
    public static void Sys_CloseLibs() {
        // rien à fermer côté hôte.
    }

    /** sys_ReleaseHardware : libération matériel — host no-op. */
    private static void sys_ReleaseHardware() {
        // rien à libérer côté hôte.
    }

    /**
     * Sys_ShowFPS : sur Amiga, RawDoFmt + Move/Text vers le RastPort. Côté hôte,
     * le FPS sera affiché dans le titre de la fenêtre (branché en Phase 2/loop) ;
     * no-op pour l'instant. Respecte Prefs_DisplayFPS_b.
     */
    public static void Sys_ShowFPS() {
        if (Mem.b(Prefs_DisplayFPS_b) == 0) {                // if (!Prefs_DisplayFPS_b) return;
            return;
        }
        // Affichage FPS hôte (titre fenêtre) — à brancher avec la boucle de frame.
    }
}
