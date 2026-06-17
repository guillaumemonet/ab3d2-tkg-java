package ab3d2.c;

/**
 * Traduction de ab3d2_source/c/zone_debug.c.
 *
 * IMPORTANT : tout le fichier zone_debug.c est encadré par #ifdef ZONE_DEBUG.
 * En build RELEASE (ZONE_DEBUG NON défini), aucune de ces fonctions ne produit
 * de code — ce ne sont que des traces printf de débogage du parcours de zones/PVS.
 * Ces no-op sont donc la traduction FIDÈLE du build release, pas des stubs en
 * attente. (Pour reconstituer le build ZONE_DEBUG il faudrait porter les printf
 * + ZDbg_ShowRegs/ZDbg_DumpZone, qui dépendent de Dev_RegStatePtr_l et de la
 * sortie console hôte.)
 */
public final class ZoneDebug {

    private ZoneDebug() {
    }

    public static void ZDbg_Init() {
    }

    public static void ZDbg_First() {
    }

    public static void ZDbg_SkipEdge() {
    }

    public static void ZDbg_Enter() {
    }

    public static void ZDbg_Skip() {
    }

    public static void ZDbg_Done() {
    }

    public static void ZDbg_LeftClip() {
    }

    public static void ZDbg_RightClip() {
    }

    /** ZDbg_DumpZone(a0=Zone*) — déclaré dans zone_debug.h, no-op en release. */
    public static void ZDbg_DumpZone(int zonePtr) {
    }
}
