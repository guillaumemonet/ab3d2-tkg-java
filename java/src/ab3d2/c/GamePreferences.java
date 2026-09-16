package ab3d2.c;

import ab3d2.Mem;

import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.VidBss.Vid_FullScreenTemp_b;
import static ab3d2.bss.VidBss.Vid_DoubleHeight_b;
import static ab3d2.bss.VidBss.Vid_LetterBoxMarginHeight_w;
import static ab3d2.bss.VidBss.Vid_isRTG;
import static ab3d2.data.VidData.Vid_ContrastAdjust_w;
import static ab3d2.data.VidData.Vid_BrightnessOffset_w;
import static ab3d2.data.VidData.Vid_GammaLevel_b;
import static ab3d2.bss.DrawBss.Draw_ForceSimpleWalls_b;
import static ab3d2.bss.SystemBss.Sys_FPSLimit_w;
import static ab3d2.NewanimsData.Anim_LightingEnabled_b;
import static ab3d2.HireswallData.Draw_GoodRender_b;
import static ab3d2.modules.draw.DrawMap.Draw_MapZoomLevel_w;
import static ab3d2.Orderzones.Zone_MovementMask_l;
import static ab3d2.ControlloopData.Prefs_FullScreen_b;
import static ab3d2.ControlloopData.Prefs_PixelMode_b;
import static ab3d2.ControlloopData.Prefs_SimpleLighting_b;
import static ab3d2.ControlloopData.Prefs_FPSLimit_b;
import static ab3d2.ControlloopData.Prefs_VertMargin_b;
import static ab3d2.ControlloopData.Prefs_DynamicLights_b;
import static ab3d2.ControlloopData.Prefs_RenderQuality_b;
import static ab3d2.ControlloopData.Prefs_ContrastAdjust_AGA_w;
import static ab3d2.ControlloopData.Prefs_ContrastAdjust_RTG_w;
import static ab3d2.ControlloopData.Prefs_BrightnessOffset_AGA_w;
import static ab3d2.ControlloopData.Prefs_BrightnessOffset_RTG_w;
import static ab3d2.ControlloopData.Prefs_GammaLevel_AGA_b;
import static ab3d2.ControlloopData.Prefs_GammaLevel_RTG_b;
import static ab3d2.data.GameData.game_PreferencesFile_vb;
import static ab3d2.ControlloopData.Prefsfile;
import static ab3d2.ControlloopData.PrefsfileEnd;

/**
 * Traduction de ab3d2_source/c/game_preferences.c.
 *
 * game_ApplyPreferences (logique pure : applique les Prefs_* aux variables runtime
 * Vid_*, Draw_*, etc.) est TRADUIT intégralement.
 *
 * Le sous-système de configuration texte (Cfg_ParsePreferencesFile / Cfg_WritePreferencesFile,
 * la table cfg_options[] des ~50 réglages issus des includes prefs_*.h, les tables de
 * pointeurs de fonction parsers/setters, les tables de touches char_keys/special_keys, et
 * l'I/O texte fopen/fscanf/fprintf de la stdlib C) relève de la couche hôte d'I/O texte et
 * reste en stub documenté (à porter avec cette couche).
 */
public final class GamePreferences {

    /** static UBYTE Prefs_OrderZoneSensitivity = 4; (variable module). */
    private static int Prefs_OrderZoneSensitivity = 4;

    private GamePreferences() {
    }

    /**
     * game_ApplyPreferences — applique les préférences chargées aux variables runtime.
     */
    public static void game_ApplyPreferences() {
        int fullScreen = Mem.ub(Prefs_FullScreen_b);
        Mem.wb(Vid_FullScreen_b, fullScreen);              // Vid_FullScreen_b = Prefs_FullScreen_b
        Mem.wb(Vid_FullScreenTemp_b, fullScreen);          // Vid_FullScreenTemp_b = Vid_FullScreen_b = ...
        Mem.wb(Vid_DoubleHeight_b, Mem.ub(Prefs_PixelMode_b)); // Vid_DoubleHeight_b = Prefs_PixelMode_b

        if (Mem.l(Vid_isRTG) != 0) {                       // if (Vid_isRTG)
            Mem.ww(Vid_ContrastAdjust_w, Mem.uw(Prefs_ContrastAdjust_RTG_w));   // Vid_ContrastAdjust_w = Prefs_ContrastAdjust_RTG_w
            Mem.ww(Vid_BrightnessOffset_w, Mem.w(Prefs_BrightnessOffset_RTG_w)); // Vid_BrightnessOffset_w = Prefs_BrightnessOffset_RTG_w
            Mem.wb(Vid_GammaLevel_b, Mem.ub(Prefs_GammaLevel_RTG_b));           // Vid_GammaLevel_b = Prefs_GammaLevel_RTG_b
        } else {
            Mem.ww(Vid_ContrastAdjust_w, Mem.uw(Prefs_ContrastAdjust_AGA_w));   // Vid_ContrastAdjust_w = Prefs_ContrastAdjust_AGA_w
            Mem.ww(Vid_BrightnessOffset_w, Mem.w(Prefs_BrightnessOffset_AGA_w)); // Vid_BrightnessOffset_w = Prefs_BrightnessOffset_AGA_w
            Mem.wb(Vid_GammaLevel_b, Mem.ub(Prefs_GammaLevel_AGA_b));           // Vid_GammaLevel_b = Prefs_GammaLevel_AGA_b
        }
        Mem.wb(Draw_ForceSimpleWalls_b, Mem.ub(Prefs_SimpleLighting_b));   // Draw_ForceSimpleWalls_b = Prefs_SimpleLighting_b
        Mem.ww(Sys_FPSLimit_w, Mem.b(Prefs_FPSLimit_b));                  // Sys_FPSLimit_w = Prefs_FPSLimit_b (BYTE→WORD, signe)
        Mem.ww(Vid_LetterBoxMarginHeight_w, Mem.ub(Prefs_VertMargin_b));  // Vid_LetterBoxMarginHeight_w = Prefs_VertMargin_b (UBYTE→WORD)
        Mem.wb(Anim_LightingEnabled_b, Mem.ub(Prefs_DynamicLights_b));    // Anim_LightingEnabled_b = Prefs_DynamicLights_b
        Mem.wb(Draw_GoodRender_b, Mem.ub(Prefs_RenderQuality_b));         // Draw_GoodRender_b = Prefs_RenderQuality_b

        // Map zoom est 0-7.
        Mem.ww(Draw_MapZoomLevel_w, Mem.uw(Draw_MapZoomLevel_w) & 7);     // Draw_MapZoomLevel_w &= 7

        // Sensibilité d'ordonnancement des zones
        Prefs_OrderZoneSensitivity &= 7;                                  // Prefs_OrderZoneSensitivity &= 7

        int mask = (~((1 << Prefs_OrderZoneSensitivity) - 1)) & 0xFFFF;   // UWORD mask = ~((1 << Prefs_OrderZoneSensitivity) - 1)

        Mem.wl(Zone_MovementMask_l, (mask << 16) | mask);                 // Zone_MovementMask_l = ((ULONG)mask << 16) | mask
    }

    /**
     * game_LoadPreferences — analyse le fichier de préférences texte.
     */
    public static void game_LoadPreferences() {
        Cfg_ParsePreferencesFile(game_PreferencesFile_vb); // Cfg_ParsePreferencesFile(game_PreferencesFile)
    }

    /**
     * game_SavePreferences — écrit le fichier de préférences texte.
     */
    public static void game_SavePreferences() {
        Cfg_WritePreferencesFile(game_PreferencesFile_vb); // Cfg_WritePreferencesFile(game_PreferencesFile)
    }

    // ------------------------------------------------------------------
    // Persistance des préférences — ADAPTATION HÔTE.
    //
    // L'original sérialise/parse un .cfg TEXTE via la table cfg_options[] (~50
    // réglages, fopen/fscanf/fprintf, tables de touches). Le bloc Prefs est
    // toutefois un RÉGION CONTIGUË [Prefsfile..PrefsfileEnd) à layout contractuel,
    // précédé du magic "k8nx". On persiste donc ce BLOC BINAIRE directement
    // (plus simple, robuste) au lieu du format texte. La logique métier — sync
    // runtime→Prefs avant écriture, et game_ApplyPreferences après lecture valide —
    // est conservée fidèlement. Fichier : "ab3:prefs.cfg" → overlay run/.
    // ------------------------------------------------------------------

    /**
     * Cfg_ParsePreferencesFile (game_preferences.c) — lit le fichier de prefs ; si présent
     * ET magic "k8nx" valide, recopie le bloc et applique (game_ApplyPreferences). Absent/
     * invalide → ne rien faire (défauts conservés ; plein écran 060 préservé), comme
     * l'original qui n'applique que `if ((fp = fopen(file,"rb")))`.
     */
    public static void Cfg_ParsePreferencesFile(int file) {
        int h = ab3d2.host.DosLib.Open(file, ab3d2.host.DosLib.MODE_OLDFILE);
        if (h == 0) {                                        // pas de fichier → applique les défauts Prefs_*
            game_ApplyPreferences();                         // cible 68060/RTG : plein écran par défaut préservé
            return;
        }
        int size = PrefsfileEnd - Prefsfile;
        int scratch = ab3d2.host.ExecLib.AllocVec(size, ab3d2.host.ExecLib.MEMF_PUBLIC);
        int n = ab3d2.host.DosLib.Read(h, scratch, size);
        ab3d2.host.DosLib.Close(h);
        boolean ok = (n == size)                             // taille + magic "k8nx"
                && Mem.ub(scratch) == 'k' && Mem.ub(scratch + 1) == '8'
                && Mem.ub(scratch + 2) == 'n' && Mem.ub(scratch + 3) == 'x';
        if (ok) {
            System.arraycopy(Mem.RAM, scratch, Mem.RAM, Prefsfile, size); // recopie le bloc Prefs
            game_ApplyPreferences();                         // game_ApplyPreferences()
        }
        ab3d2.host.ExecLib.FreeVec(scratch);
    }

    /**
     * Cfg_WritePreferencesFile (game_preferences.c) — synchronise les variables runtime
     * vers les Prefs_* (lignes 417-433) puis écrit le bloc Prefs binaire [Prefsfile..PrefsfileEnd).
     */
    public static void Cfg_WritePreferencesFile(int file) {
        // sync runtime → Prefs (game_preferences.c:417-433)
        Mem.wb(Prefs_FullScreen_b, Mem.b(Vid_FullScreen_b));
        Mem.wb(Prefs_PixelMode_b, Mem.b(Vid_DoubleHeight_b));
        Mem.wb(Prefs_SimpleLighting_b, Mem.b(Draw_ForceSimpleWalls_b));
        Mem.wb(Prefs_FPSLimit_b, Mem.w(Sys_FPSLimit_w));            // (BYTE)Sys_FPSLimit_w
        Mem.wb(Prefs_VertMargin_b, Mem.w(Vid_LetterBoxMarginHeight_w)); // (UBYTE)Vid_LetterBoxMarginHeight_w
        Mem.wb(Prefs_DynamicLights_b, Mem.b(Anim_LightingEnabled_b));
        Mem.wb(Prefs_RenderQuality_b, Mem.b(Draw_GoodRender_b));
        if (Mem.l(Vid_isRTG) != 0) {
            Mem.ww(Prefs_ContrastAdjust_RTG_w, Mem.uw(Vid_ContrastAdjust_w));
            Mem.ww(Prefs_BrightnessOffset_RTG_w, Mem.w(Vid_BrightnessOffset_w));
            Mem.wb(Prefs_GammaLevel_RTG_b, Mem.b(Vid_GammaLevel_b));
        } else {
            Mem.ww(Prefs_ContrastAdjust_AGA_w, Mem.uw(Vid_ContrastAdjust_w));
            Mem.ww(Prefs_BrightnessOffset_AGA_w, Mem.w(Vid_BrightnessOffset_w));
            Mem.wb(Prefs_GammaLevel_AGA_b, Mem.b(Vid_GammaLevel_b));
        }
        // écrit le bloc binaire [Prefsfile, PrefsfileEnd) (magic "k8nx" inclus en tête)
        int size = PrefsfileEnd - Prefsfile;
        int h = ab3d2.host.DosLib.Open(file, ab3d2.host.DosLib.MODE_NEWFILE);
        if (h != 0) {
            ab3d2.host.DosLib.Write(h, Prefsfile, size);
            ab3d2.host.DosLib.Close(h);
        }
    }
}
