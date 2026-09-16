package ab3d2.c;

import ab3d2.Mem;

import static ab3d2.Defs.ZoneT_ID_w;
import static ab3d2.Defs.ZoneT_EdgeListOffset_w;
import static ab3d2.Defs.ZoneT_PotVisibleZoneList_vw;
import static ab3d2.Defs.ZPVSRecordT_ZoneID_w;
import static ab3d2.Defs.ZPVSRecordT_SizeOf_l;
import static ab3d2.Defs.EdgeT_JoinZone_w;
import static ab3d2.Defs.EdgeT_SizeOf_l;
import static ab3d2.bss.LevelBss.Lvl_NumZones_w;
import static ab3d2.bss.LevelBss.Lvl_ZoneEdgePtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.SystemBss.Sys_Workspace_vl;

/**
 * Traduction littérale de ab3d2_source/c/zone_errata.c.
 *
 * Application des errata de PVS par zone : on retire manuellement certaines zones
 * de la liste PVS d'une zone, puis on reconstruit l'ensemble connexe par parcours
 * des arêtes, et enfin on réécrit le tableau ZPVSRecord en collapsant les entrées
 * supprimées.
 *
 * Buffers de travail (Sys_GetTemporaryWorkspace = Sys_Workspace_vl) :
 *   - current PVS : Sys_Workspace_vl
 *   - visited PVS : Sys_Workspace_vl + 1024 mots (= +2048 octets)
 *
 * Pointeurs C → adresses en mémoire plate. L'union DoorDataPtr/ZPVSRecord* etc.
 * deviennent des int. Lvl_ZonePtrsPtr_l (Zone**) et Lvl_ZoneEdgePtr_l (ZEdge*)
 * sont des variables pointeur → double indirection (Mem.l du pointeur puis index).
 *
 * dputs/dprintf/dputchar sont des macros de debug (no-op hors ZONE_DEBUG) : les
 * traces sont sans effet, mais les `return` des branches runaway sont conservés.
 */
public final class ZoneErrata {

    private static final int ZONE_ID_LIST_END = -1;        // zone.h
    private static final int ZONE_ID_REMOVED_MANUAL = -2;  // zone.h
    private static final int ZONE_ID_REMOVED_AUTO = -3;    // zone.h
    private static final int PVS_TRAVERSE_LIMIT = 100;     // #define
    private static final int EDGE_TRAVERSE_LIMIT = 16;     // #define

    private static final boolean TRUE = true;
    private static final boolean FALSE = false;

    private ZoneErrata() {
    }

    // -- buffers de travail (static inline) --

    /** zone_GetCurrentPVSBuffer : liste des Zone ID du PVS courant. */
    private static int zone_GetCurrentPVSBuffer() {
        return Sys_Workspace_vl;                            // return (WORD*)Sys_GetTemporaryWorkspace();
    }

    /** zone_GetVisitedPVSBuffer : liste des Zone ID visités (workspace + 1024 mots). */
    private static int zone_GetVisitedPVSBuffer() {
        return Sys_Workspace_vl + 1024 * 2;                 // return ((WORD*)Sys_GetTemporaryWorkspace()) + 1024;
    }

    // -- inline helpers (zone_inline.h) --

    /** Zone_IsValidZoneID : id >= 0 && id < Lvl_NumZones_w. */
    private static boolean Zone_IsValidZoneID(int id) {
        return id >= 0 && id < Mem.w(Lvl_NumZones_w);
    }

    /** Zone_IsValidEdgeID : id >= 0. */
    private static boolean Zone_IsValidEdgeID(int id) {
        return id >= 0;
    }

    /** Zone_GetEdgeList : zonePtr + z_EdgeListOffset (offset NÉGATIF signé). */
    private static int Zone_GetEdgeList(int zonePtr) {
        return zonePtr + (short) Mem.w(zonePtr + ZoneT_EdgeListOffset_w); // (WORD*)(((BYTE*)zonePtr) + zonePtr->z_EdgeListOffset)
    }

    /**
     * zone_InitCurrentPVS — copie les Zone ID du tableau ZPVSRecord dans le buffer
     * courant, en remplaçant par ZONE_ID_REMOVED_MANUAL ceux présents dans la
     * liste de suppression. Liste terminée par ZONE_ID_LIST_END.
     */
    public static void zone_InitCurrentPVS(int zonePtr, int removeListPtr) {
        int zonePVSPtr = zonePtr + ZoneT_PotVisibleZoneList_vw;  // ZPVSRecord const* zonePVSPtr = zonePtr->z_PotVisibleZoneList;
        int pvsCurrentZonePtr = zone_GetCurrentPVSBuffer();      // WORD* pvsCurrentZonePtr = zone_GetCurrentPVSBuffer();
        int zoneID;

        int runaway = PVS_TRAVERSE_LIMIT;                       // int runaway = PVS_TRAVERSE_LIMIT;
        while ((zoneID = Mem.w(zonePVSPtr + ZPVSRecordT_ZoneID_w)) > ZONE_ID_LIST_END && runaway-- > 0) { // while ((zoneID = zonePVSPtr->pvs_ZoneID) > ZONE_ID_LIST_END && runaway-- > 0)
            zonePVSPtr += ZPVSRecordT_SizeOf_l;                 // ++zonePVSPtr;
            int removePtr = removeListPtr;                      // WORD const* removePtr = removeListPtr;
            while (removePtr != 0 && Zone_IsValidZoneID(Mem.w(removePtr))) { // while (removePtr && Zone_IsValidZoneID(*removePtr))
                int v = Mem.w(removePtr);                       // (*removePtr...
                removePtr += 2;                                 //  ...++)
                if (v == zoneID) {                              // if (*removePtr++ == zoneID)
                    zoneID = ZONE_ID_REMOVED_MANUAL;            //   zoneID = ZONE_ID_REMOVED_MANUAL;
                    break;                                      //   break;
                }
            }
            Mem.ww(pvsCurrentZonePtr, zoneID);                 // *pvsCurrentZonePtr++ = zoneID;
            pvsCurrentZonePtr += 2;
        }
        Mem.ww(pvsCurrentZonePtr, ZONE_ID_LIST_END);           // *pvsCurrentZonePtr = ZONE_ID_LIST_END;
    }

    /**
     * zone_CheckInCurrentPVSList — la zone est-elle dans la liste PVS courante ?
     */
    public static boolean zone_CheckInCurrentPVSList(int zoneID) {
        int pvsCurrentZonePtr = zone_GetCurrentPVSBuffer();    // WORD const* pvsCurrentZonePtr = zone_GetCurrentPVSBuffer();
        int nextZoneID;

        int runaway = PVS_TRAVERSE_LIMIT;

        while (true) {                                         // while ((nextZoneID = *pvsCurrentZonePtr++) != ZONE_ID_LIST_END && runaway-- > 0)
            nextZoneID = Mem.w(pvsCurrentZonePtr);
            pvsCurrentZonePtr += 2;
            if (nextZoneID == ZONE_ID_LIST_END) {
                break;
            }
            if (!(runaway-- > 0)) {
                break;
            }
            if (zoneID == nextZoneID) {                        // if (zoneID == nextZoneID)
                return TRUE;                                   //   return TRUE;
            }
        }
        return FALSE;                                          // return FALSE;
    }

    /**
     * zone_CheckIsNotInVisitedPVSList — utilisé pendant la construction de la liste
     * visited (pas encore terminée), testée jusqu'à endPtr seulement.
     */
    public static boolean zone_CheckIsNotInVisitedPVSList(int zoneID, int endPtr) {
        int visitedPVSPtr = zone_GetVisitedPVSBuffer();        // WORD* visitedPVSPtr = zone_GetVisitedPVSBuffer();

        int runaway = PVS_TRAVERSE_LIMIT;

        while (visitedPVSPtr < endPtr && runaway-- > 0) {      // while (visitedPVSPtr < endPtr && runaway-- > 0)
            int v = Mem.w(visitedPVSPtr);                      // (*visitedPVSPtr...
            visitedPVSPtr += 2;                                //  ...++)
            if (zoneID == v) {                                 // if (zoneID == *visitedPVSPtr++)
                return FALSE;                                  //   return FALSE;
            }
        }
        return TRUE;                                           // return TRUE;
    }

    /**
     * zone_GetVisitedZoneID — pour une zone valide, renvoie le même ID s'il est
     * dans la liste visited, sinon ZONE_ID_REMOVED_AUTO.
     */
    public static int zone_GetVisitedZoneID(int zoneID) {
        if (Zone_IsValidZoneID(zoneID)) {                      // if (Zone_IsValidZoneID(zoneID))
            int visitedPVSPtr = zone_GetVisitedPVSBuffer();    // WORD const* visitedPVSPtr = zone_GetVisitedPVSBuffer();
            int visitedZoneID;
            while (true) {                                     // while ((visitedZoneID = *visitedPVSPtr++) != ZONE_ID_LIST_END)
                visitedZoneID = Mem.w(visitedPVSPtr);
                visitedPVSPtr += 2;
                if (visitedZoneID == ZONE_ID_LIST_END) {
                    break;
                }
                if (zoneID == visitedZoneID) {                 // if (zoneID == visitedZoneID)
                    return zoneID;                             //   return zoneID;
                }
            }
            return ZONE_ID_REMOVED_AUTO;                       // return ZONE_ID_REMOVED_AUTO;
        }
        return zoneID;                                         // return zoneID;
    }

    /**
     * zone_BuildVisitedPVS — parcourt l'ensemble des zones connectées via leurs
     * arêtes pour construire la liste des zones du PVS qui restent connexes.
     */
    public static void zone_BuildVisitedPVS(int zonePtr) {
        int visitedPVSPtr = zone_GetVisitedPVSBuffer();        // WORD* visitedPVSPtr = zone_GetVisitedPVSBuffer();
        int nextZoneID;

        // À partir de la zone courante, ajoute celles accessibles par les arêtes.
        Mem.ww(visitedPVSPtr, Mem.w(zonePtr + ZoneT_ID_w));    // *visitedPVSPtr++ = zonePtr->z_ZoneID;
        visitedPVSPtr += 2;
        Mem.ww(visitedPVSPtr, ZONE_ID_LIST_END);               // *visitedPVSPtr = ZONE_ID_LIST_END;

        int nextPVSPtr = visitedPVSPtr;                        // WORD* nextPVSPtr = visitedPVSPtr;
        do {
            int edgeIndexPtr = Zone_GetEdgeList(zonePtr);      // WORD const* edgeIndexPtr = Zone_GetEdgeList(zonePtr);
            int edgeID;
            int zonesAdded;
            int runaway = PVS_TRAVERSE_LIMIT;

            do {
                zonesAdded = 0;                                // zonesAdded = 0;

                int runaway2 = EDGE_TRAVERSE_LIMIT;

                while (true) {                                 // while (Zone_IsValidEdgeID((edgeID = *edgeIndexPtr++)) && runaway2-- > 0)
                    edgeID = Mem.w(edgeIndexPtr);
                    edgeIndexPtr += 2;
                    if (!(Zone_IsValidEdgeID(edgeID) && runaway2-- > 0)) {
                        break;
                    }
                    // Si l'arête joint une zone, e_JoinZoneID l'indique (sinon négatif).
                    int joinZoneID = Mem.w(Mem.l(Lvl_ZoneEdgePtr_l) + edgeID * EdgeT_SizeOf_l + EdgeT_JoinZone_w); // WORD joinZoneID = Lvl_ZoneEdgePtr_l[edgeID].e_JoinZoneID;
                    if (
                        Zone_IsValidZoneID(joinZoneID) &&                              // Zone_IsValidZoneID(joinZoneID) &&
                        zone_CheckIsNotInVisitedPVSList(joinZoneID, visitedPVSPtr) &&  // zone_CheckIsNotInVisitedPVSList(joinZoneID, visitedPVSPtr) &&
                        zone_CheckInCurrentPVSList(joinZoneID)                         // zone_CheckInCurrentPVSList(joinZoneID)
                    ) {
                        Mem.ww(visitedPVSPtr, joinZoneID);     // *visitedPVSPtr++ = joinZoneID;
                        visitedPVSPtr += 2;
                        Mem.ww(visitedPVSPtr, ZONE_ID_LIST_END); // *visitedPVSPtr = ZONE_ID_LIST_END;
                        ++zonesAdded;                          // ++zonesAdded;
                    }
                }

                if (runaway2 <= 0) {                           // if (runaway2 <= 0)
                    // dputs("zone_BuildVisitedPVS() Runaway 2");
                    return;                                    //   return;
                }

            } while (zonesAdded != 0 && runaway-- > 0);        // while (zonesAdded && runaway-- > 0);

            if (runaway <= 0) {                                // if (runaway <= 0)
                // dputs("zone_BuildVisitedPVS() Runaway");
                return;                                        //   return;
            }

            zonePtr = 0;                                       // zonePtr = NULL;
            nextZoneID = Mem.w(nextPVSPtr);                    // nextZoneID = *nextPVSPtr++;
            nextPVSPtr += 2;
            if (Zone_IsValidZoneID(nextZoneID)) {              // if (Zone_IsValidZoneID(nextZoneID))
                zonePtr = Mem.l(Mem.l(Lvl_ZonePtrsPtr_l) + 4 * nextZoneID); // zonePtr = Lvl_ZonePtrsPtr_l[nextZoneID];
            }

        } while (zonePtr != 0);                                // while (zonePtr);

        Mem.ww(visitedPVSPtr, ZONE_ID_LIST_END);               // *visitedPVSPtr = ZONE_ID_LIST_END;
    }

    /**
     * zone_RebuildCurrentPVS — réécrit le tableau ZPVSRecord de la zone en
     * collapsant les entrées qui ne sont plus dans l'ensemble amendé.
     */
    public static void zone_RebuildCurrentPVS(int zonePtr) {
        int pvsCurrentZonePtr = zone_GetCurrentPVSBuffer();    // WORD* pvsCurrentZonePtr = zone_GetCurrentPVSBuffer();
        int nextZoneID;
        do {
            nextZoneID = zone_GetVisitedZoneID(Mem.w(pvsCurrentZonePtr)); // nextZoneID = zone_GetVisitedZoneID(*pvsCurrentZonePtr);
            Mem.ww(pvsCurrentZonePtr, nextZoneID);             // *pvsCurrentZonePtr++ = nextZoneID;
            pvsCurrentZonePtr += 2;
        } while (nextZoneID != ZONE_ID_LIST_END);              // while (nextZoneID != ZONE_ID_LIST_END);

        pvsCurrentZonePtr = zone_GetCurrentPVSBuffer();        // pvsCurrentZonePtr = zone_GetCurrentPVSBuffer();
        int pvsWritePtr = zonePtr + ZoneT_PotVisibleZoneList_vw; // ZPVSRecord* pvsWritePtr = zonePtr->z_PotVisibleZoneList;
        int pvsReadPtr = pvsWritePtr;                          // ZPVSRecord const* pvsReadPtr = pvsWritePtr;

        // Collapse de la liste PVS originale.
        while (true) {                                         // while ((nextZoneID = *pvsCurrentZonePtr++) != ZONE_ID_LIST_END)
            nextZoneID = Mem.w(pvsCurrentZonePtr);
            pvsCurrentZonePtr += 2;
            if (nextZoneID == ZONE_ID_LIST_END) {
                break;
            }
            if (nextZoneID >= 0) {                             // if (nextZoneID >= 0)
                if (pvsReadPtr != pvsWritePtr) {               // if (pvsReadPtr != pvsWritePtr)
                    // *pvsWritePtr = *pvsReadPtr;  (copie ZPVSRecord, 8 octets)
                    Mem.wl(pvsWritePtr, Mem.l(pvsReadPtr));
                    Mem.wl(pvsWritePtr + 4, Mem.l(pvsReadPtr + 4));
                }
                pvsWritePtr += ZPVSRecordT_SizeOf_l;           // ++pvsWritePtr;
            }
            pvsReadPtr += ZPVSRecordT_SizeOf_l;                // ++pvsReadPtr;
        }
        Mem.ww(pvsWritePtr + ZPVSRecordT_ZoneID_w, ZONE_ID_LIST_END); // pvsWritePtr->pvs_ZoneID = ZONE_ID_LIST_END;
    }

    /**
     * Zone_ApplyPVSErrata (CALLC) — a0 = flux d'errata PVS. Pour chaque zone du
     * flux, retire les zones listées de son PVS puis reconstruit/réécrit la liste.
     * Flux : suite de listes [zoneID, idsÀRetirer..., ZONE_ID_LIST_END], terminée
     * par un ZONE_ID_LIST_END supplémentaire (double -1 final).
     */
    public static void Zone_ApplyPVSErrata(int zonePVSErrataPtr) {
        if (zonePVSErrataPtr != 0) {                           // if (zonePVSErrataPtr)
            // dputs("Zone_ApplyPVSErrata()...");
            int numZones = 0;                                  // WORD numZones = 0;
            int zoneID;
            while (true) {                                     // while (Zone_IsValidZoneID(zoneID = *zonePVSErrataPtr++))
                zoneID = Mem.w(zonePVSErrataPtr);
                zonePVSErrataPtr += 2;
                if (!Zone_IsValidZoneID(zoneID)) {
                    break;
                }
                int zonePtr = Mem.l(Mem.l(Lvl_ZonePtrsPtr_l) + 4 * zoneID); // Zone* zonePtr = Lvl_ZonePtrsPtr_l[zoneID];
                int removeListPtr = zonePVSErrataPtr;          // WORD const* removeListPtr = zonePVSErrataPtr;
                while (true) {                                 // while (Zone_IsValidZoneID((zoneID = *zonePVSErrataPtr++))) {}
                    zoneID = Mem.w(zonePVSErrataPtr);
                    zonePVSErrataPtr += 2;
                    if (!Zone_IsValidZoneID(zoneID)) {
                        break;
                    }
                }
                zone_InitCurrentPVS(zonePtr, removeListPtr);   // zone_InitCurrentPVS(zonePtr, removeListPtr);
                zone_BuildVisitedPVS(zonePtr);                 // zone_BuildVisitedPVS(zonePtr);
                zone_RebuildCurrentPVS(zonePtr);               // zone_RebuildCurrentPVS(zonePtr);
                ++numZones;                                    // ++numZones;
            }
            // dprintf("\tDone. %d PVS lists amended\n", numZones);
        }
    }
}
