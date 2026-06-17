package ab3d2.c;

import ab3d2.Mem;

import static ab3d2.Defs.LVL_MAX_DOOR_ZONES;
import static ab3d2.Defs.ZLiftableT_ZoneID_w;
import static ab3d2.Defs.ZLiftableT_SizeOf_l;
import static ab3d2.bss.LevelBss.Lvl_NumZones_w;
import static ab3d2.bss.LevelBss.Lvl_DoorDataPtr_l;
import static ab3d2.bss.ZoneBss.Zone_DoorMap_vb;
import static ab3d2.bss.ZoneBss.Zone_DoorList_vw;

/**
 * Traduction littérale de ab3d2_source/c/zone_liftable_pvs.c.
 *
 * Construit la liste compacte des portes du niveau et fournit la recherche
 * d'ID de porte par zone.
 *
 * Le flux de données portes est : [{ ZLiftable, ZDoorWall[2N], -1 }, ...] terminé
 * par le mot magique 999 (END_OF_DOOR_LIST). Chaque liste de murs est terminée par
 * un mot -1 (END_OF_DOOR_WALL_LIST). L'union DoorDataPtr (marker/door/wall) devient
 * une simple adresse en mémoire plate.
 *
 * dprintf() est un macro de debug (no-op en build release) : zone_DumpLiftable et
 * les traces sont donc sans effet ici.
 */
public final class ZoneLiftablePvs {

    private static final int NOT_A_DOOR = -1;             // #define NOT_A_DOOR -1
    private static final int END_OF_DOOR_LIST = 999;      // #define END_OF_DOOR_LIST 999
    private static final int END_OF_DOOR_WALL_LIST = -1;  // #define END_OF_DOOR_WALL_LIST -1
    private static final int ZONE_ID_LIST_END = -1;       // zone.h : ZONE_ID_LIST_END = -1

    /** Number of doors in the level (static WORD zone_NumDoorDefs). */
    private static int zone_NumDoorDefs = 0;

    private ZoneLiftablePvs() {
    }

    // -- inline helpers (zone_inline.h) --

    /** Zone_IsValidZoneID : id >= 0 && id < Lvl_NumZones_w. */
    private static boolean Zone_IsValidZoneID(int id) {
        return id >= 0 && id < Mem.w(Lvl_NumZones_w);     // return id >= 0 && id < Lvl_NumZones_w;
    }

    /** Zone_IsDoor : zone valide ET bit positionné dans Zone_DoorMap_vb. */
    private static boolean Zone_IsDoor(int zoneID) {
        return Zone_IsValidZoneID(zoneID)                 // Zone_IsValidZoneID(zoneID) &&
            && (Mem.ub(Zone_DoorMap_vb + (zoneID >> 3)) & (1 << (zoneID & 7))) != 0; // ( Zone_DoorMap_vb[zoneID>>3] & (1<<(zoneID&7)) )
    }

    /** zone_DumpLiftable : trace de debug (dprintf), no-op en release. */
    public static void zone_DumpLiftable(int liftable, int index, int type) {
        // dprintf(...) — désactivé hors build debug.
    }

    /**
     * Zone_InitDoorList — construit Zone_DoorList_vw / Zone_DoorMap_vb à partir du
     * flux de données portes du niveau et fixe zone_NumDoorDefs.
     */
    public static void Zone_InitDoorList() {
        // dprintf("Zone_InitDoorList()\n");
        for (int i = 0; i < Mem.w(Lvl_NumZones_w) / 8; ++i) { // for (WORD i = 0; i < Lvl_NumZones_w/8; ++i)
            Mem.wb(Zone_DoorMap_vb + i, 0);                   //   Zone_DoorMap_vb[i] = 0;
        }

        int doorDataPtr = Mem.l(Lvl_DoorDataPtr_l);           // DoorDataPtr doorDataPtr; doorDataPtr.marker = Lvl_DoorDataPtr_l;

        int doorIndex = 0;                                    // WORD doorIndex = 0;
        while (Mem.w(doorDataPtr) != END_OF_DOOR_LIST && doorIndex < LVL_MAX_DOOR_ZONES) { // while (*marker != END_OF_DOOR_LIST && doorIndex < LVL_MAX_DOOR_ZONES)
            int zoneID = Mem.w(doorDataPtr + ZLiftableT_ZoneID_w); // WORD zoneID = doorDataPtr.door->zl_ZoneID;
            if (Zone_IsValidZoneID(zoneID)) {                 // if (Zone_IsValidZoneID(zoneID))
                // zone_DumpLiftable(doorDataPtr.door, doorIndex, "Door");
                // dprintf("Door %2d => Zone %3d\n", ...);
                Mem.ww(Zone_DoorList_vw + 2 * doorIndex, zoneID); // Zone_DoorList_vw[doorIndex++] = zoneID;
                doorIndex++;
                Mem.wb(Zone_DoorMap_vb + (zoneID >> 3),       // Zone_DoorMap_vb[zoneID >> 3] |=
                    Mem.ub(Zone_DoorMap_vb + (zoneID >> 3)) | (1 << (zoneID & 7))); // (1 << (zoneID & 7));
            }
            doorDataPtr += ZLiftableT_SizeOf_l;               // doorDataPtr.door++;
            while (true) {                                    // while (*doorDataPtr.marker++ != END_OF_DOOR_WALL_LIST) {}
                int v = Mem.w(doorDataPtr);                   //   (lit *marker,
                doorDataPtr += 2;                             //    marker++,
                if (v == END_OF_DOOR_WALL_LIST) {             //    boucle tant que != -1)
                    break;
                }
            }
        }

        zone_NumDoorDefs = doorIndex;                         // zone_NumDoorDefs = doorIndex;

        while (doorIndex < LVL_MAX_DOOR_ZONES) {              // while (doorIndex < LVL_MAX_DOOR_ZONES)
            Mem.ww(Zone_DoorList_vw + 2 * doorIndex, ZONE_ID_LIST_END); // Zone_DoorList_vw[doorIndex++] = ZONE_ID_LIST_END;
            doorIndex++;
        }
    }

    /**
     * Zone_GetDoorID — renvoie l'index de porte pour une zone, ou NOT_A_DOOR.
     * Recherche linéaire après élimination rapide via la door map.
     */
    public static int Zone_GetDoorID(int zoneID) {
        if (Zone_IsDoor(zoneID)) {                            // if (Zone_IsDoor(zoneID))
            for (int i = 0; i < zone_NumDoorDefs; ++i) {      // for (WORD i = 0; i < zone_NumDoorDefs; ++i)
                if (zoneID == Mem.w(Zone_DoorList_vw + 2 * i)) { // if (zoneID == Zone_DoorList_vw[i])
                    return i;                                 //   return i;
                }
            }
        }
        return NOT_A_DOOR;                                    // return NOT_A_DOOR;
    }
}
