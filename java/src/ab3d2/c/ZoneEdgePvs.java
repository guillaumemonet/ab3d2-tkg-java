package ab3d2.c;

import ab3d2.Mem;
import ab3d2.host.ExecLib;

import static ab3d2.Defs.ZoneT_Floor_l;
import static ab3d2.Defs.ZoneT_UpperFloor_l;
import static ab3d2.Defs.ZoneT_UpperRoof_l;
import static ab3d2.Defs.ZoneT_Unused_w;
import static ab3d2.Defs.ZoneT_EdgeListOffset_w;
import static ab3d2.Defs.ZoneT_PotVisibleZoneList_vw;
import static ab3d2.Defs.ZPVSRecordT_ZoneID_w;
import static ab3d2.Defs.ZPVSRecordT_SizeOf_l;
import static ab3d2.Defs.EdgeT_XPos_w;
import static ab3d2.Defs.EdgeT_ZPos_w;
import static ab3d2.Defs.EdgeT_XLen_w;
import static ab3d2.Defs.EdgeT_ZLen_w;
import static ab3d2.Defs.EdgeT_JoinZone_w;
import static ab3d2.Defs.EdgeT_SizeOf_l;
import static ab3d2.Defs.Vec2W_X;
import static ab3d2.Defs.Vec2W_Z;
import static ab3d2.Defs.Vec2W_SizeOf;
import static ab3d2.Defs.ZEdgeInfoT_EdgeID_w;
import static ab3d2.Defs.ZEdgeInfoT_StartPointID_w;
import static ab3d2.Defs.ZEdgeInfoT_EndPointID_w;
import static ab3d2.Defs.ZEdgeInfoT_SizeOf_l;
import static ab3d2.Defs.ZEdgePVSHeaderT_ZoneID_w;
import static ab3d2.Defs.ZEdgePVSHeaderT_ListSize_w;
import static ab3d2.Defs.ZEdgePVSHeaderT_EdgeCount_w;
import static ab3d2.Defs.ZEdgePVSHeaderT_ZoneMaskOffset_w;
import static ab3d2.Defs.ZEdgePVSHeaderT_DoorMaskOffset_w;
import static ab3d2.Defs.ZEdgePVSHeaderT_LiftMaskOffset_w;
import static ab3d2.Defs.ZEdgePVSHeaderT_EdgeInfoList_v;
import static ab3d2.Defs.ZEdgePVSHeaderT_SizeOf_l;
import static ab3d2.Defs.ZDoorListMask_SizeOf;

import static ab3d2.bss.LevelBss.Lvl_NumZones_w;
import static ab3d2.bss.LevelBss.Lvl_NumPoints_w;
import static ab3d2.bss.LevelBss.Lvl_PointsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZoneEdgePtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZonePtrsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ZEdgePVSHeaderPtrsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ListOfGraphRoomsPtr_l;
import static ab3d2.bss.ZoneBss.Zone_DoorMap_vb;
import static ab3d2.bss.ZoneBss.Zone_CurrentDoorState_w;
import static ab3d2.bss.ZoneBss.Zone_RenderDoorState_w;
import static ab3d2.bss.ZoneBss.Zone_PVSList_vw;
import static ab3d2.bss.ZoneBss.Zone_PVSMask_vb;
import static ab3d2.bss.ZoneBss.Zone_EdgePointIndexes_vw;
import static ab3d2.bss.ZoneBss.Zone_VisJoinMask_w;
import static ab3d2.bss.ZoneBss.Zone_VisJoins_w;
import static ab3d2.bss.ZoneBss.Zone_TotJoins_w;
import static ab3d2.bss.SystemBss.Sys_Workspace_vl;
import static ab3d2.bss.DrawBss.Draw_CurrentZone_w;
import static ab3d2.bss.DrawBss.Draw_ForceZoneSkip_b;
import static ab3d2.bss.DrawBss.Draw_ZoneClipL_w;
import static ab3d2.bss.DrawBss.Draw_ZoneClipR_w;
import static ab3d2.bss.TablesBss.OnScreen_vl;
import static ab3d2.bss.PlayerBss.Plr_MultiplayerType_b;
import static ab3d2.bss.PlayerBss.Plr1_Position_vl;
import static ab3d2.bss.PlayerBss.Plr2_Position_vl;
import static ab3d2.data.TablesData.SinCosTable_vw;
import static ab3d2.HiresData.Vid_RightX_w;
import static ab3d2.HiresData.Vis_AngPos_w;
import static ab3d2.HireswallData.Vis_CosVal_w;
import static ab3d2.HireswallData.Vis_SinVal_w;

/**
 * Traduction littérale de ab3d2_source/c/zone_edge_pvs.c.
 *
 * Détermination, allocation et exploitation des données PVS « par arête » : pour
 * chaque zone et chacune de ses arêtes joignantes, on calcule l'ensemble des zones
 * potentiellement visibles (récursivement via les arêtes face au point de vue), avec
 * masques de portes optionnels. À l'exécution (Zone_CheckVisibleEdges), on fusionne
 * les PVS des arêtes effectivement dans le champ de vision.
 *
 * Conventions de pointeurs en mémoire plate :
 *   - Lvl_ZonePtrsPtr_l (Zone**)               → Mem.l(Mem.l(ptr) + 4*id)
 *   - Lvl_ZEdgePVSHeaderPtrsPtr_l (Header**)   → Mem.l(Mem.l(ptr) + 4*id)
 *   - Lvl_ZoneEdgePtr_l (ZEdge*)               → Mem.l(ptr) + id*EdgeT_SizeOf_l
 *   - Lvl_PointsPtr_l (Vec2W*)                 → Mem.l(ptr) + i*4
 *   - Lvl_ListOfGraphRoomsPtr_l (ZPVSRecord*)  → Mem.l(ptr)
 *
 * Vec2W (offsets 0/2) coïncide avec EdgeT_XPos_w/EdgeT_ZPos_w, donc &edge->e_Pos = edgeAddr.
 * Sys_GetTemporaryWorkspace() = Sys_Workspace_vl. AllocVec/FreeVec = couche hôte ExecLib.
 * dputs/dprintf = macros debug no-op. La struct module statique Zone_EdgePVSState et les
 * vecteurs statiques sont des champs ; les Vec2W passés par adresse sont alloués en Mem.
 */
public final class ZoneEdgePvs {

    // -- constantes (zone.h / defines locaux) --
    private static final int ZONE_ID_LIST_END = -1;
    private static final int NOT_A_DOOR = -1;              // zone_liftable.h
    private static final int EDGE_POINT_ID_LIST_END = -4;
    private static final int PVSCF_DOOR = 1;
    private static final int PVSCF_LIFT = 2;               // (réservé)
    private static final int DISABLED_HEIGHT = 5000;
    private static final int SINTAB_SIZE = 8192;
    private static final int FOV = 1800;                   // ~79° (2048 = 90°)
    private static final int MEMF_ANY = 0;
    private static final int MULTIPLAYER_SLAVE = 's';

    // ZVIS_* (visibilité)
    private static final int ZVIS_ID_BITS = 5;
    private static final int ZVIS_NONE = 0;
    private static final int ZVIS_COND = 1 << ZVIS_ID_BITS;   // 32
    private static final int ZVIS_DOOR = 2 << ZVIS_ID_BITS;   // 64
    private static final int ZVIS_DIRECT = 4 << ZVIS_ID_BITS; // 128

    // ZoneCrossing
    private static final int NO_PATH = 0;
    private static final int LOWER_TO_LOWER = 1;
    private static final int LOWER_TO_UPPER = 2;
    private static final int UPPER_TO_LOWER = LOWER_TO_LOWER << 2; // 4
    private static final int UPPER_TO_UPPER = LOWER_TO_UPPER << 2; // 8

    // Bits de visibilité d'arête (Zone_CheckVisibleEdges)
    private static final int BIT_FRONT = 1;
    private static final int BIT_LEFT = 2;
    private static final int BIT_RIGHT = 4;

    // Player position : WORD[] avec POS_X=0, POS_Z=4 (indices mots → octets ×2)
    private static final int POS_X = 0;
    private static final int POS_Z = 4;

    // ZPVSCount (struct locale au .c) : numZones,numJoins,features,dataSize
    private static final int ZPVSCOUNT_numZones = 0;
    private static final int ZPVSCOUNT_numJoins = 2;
    private static final int ZPVSCOUNT_features = 4;
    private static final int ZPVSCOUNT_dataSize = 6;
    private static final int ZPVSCOUNT_SIZEOF = 8;

    // Zone_LevelPair {zlp_Floor@0, zlp_Roof@4}
    private static final int ZLP_Floor = 0;
    private static final int ZLP_Roof = 4;

    /** Peut être surchargé par la config. */
    public static int Zone_PVSFieldOfView = FOV;

    // -- Zone_EdgePVSState (struct module statique, accédée par le code récursif) --
    private static int zre_rootZonePtr;
    private static int zre_FullPVSListPtr;
    private static int zre_EdgePVSList;     // UBYTE*
    private static int zre_DoorMaskList;    // ZDoorListMask* (0 = null)
    private static int zre_RecursionDepth;
    private static int zre_MaxRecursionDepth;
    private static int zre_OperationCount;
    private static final int zre_ViewPoint1 = Mem.alloc(Vec2W_SizeOf); // passés par adresse
    private static final int zre_ViewPoint2 = Mem.alloc(Vec2W_SizeOf);

    // -- vecteurs statiques du module (passés par adresse à sideOfDirection) --
    private static final int zone_ViewPoint = Mem.alloc(Vec2W_SizeOf);
    private static final int zone_PerpDir = Mem.alloc(Vec2W_SizeOf);
    private static final int zone_LeftFOVDir = Mem.alloc(Vec2W_SizeOf);
    private static final int zone_RightFOVDir = Mem.alloc(Vec2W_SizeOf);

    // -- scratch pour les Vec2W locaux passés par adresse --
    private static final int scratch_endPoint = Mem.alloc(Vec2W_SizeOf); // endPoint (Zone_CheckVisibleEdges)
    private static final int scratch_end = Mem.alloc(Vec2W_SizeOf);      // end (zone_FillEdgePointIndexes)

    private ZoneEdgePvs() {
    }

    // ===== helpers de déréférencement =====

    /** Lvl_ZoneEdgePtr_l[edgeID] (ZEdge*). */
    private static int edgeAddr(int edgeID) {
        return Mem.l(Lvl_ZoneEdgePtr_l) + edgeID * EdgeT_SizeOf_l;
    }

    /** Lvl_ZonePtrsPtr_l[zoneID] (Zone**). */
    private static int zoneAddr(int zoneID) {
        return Mem.l(Mem.l(Lvl_ZonePtrsPtr_l) + 4 * zoneID);
    }

    /** Lvl_ZEdgePVSHeaderPtrsPtr_l[zoneID] (ZEdgePVSHeader**). */
    private static int headerPtr(int zoneID) {
        return Mem.l(Mem.l(Lvl_ZEdgePVSHeaderPtrsPtr_l) + 4 * zoneID);
    }

    /** Adresse de zep_EdgeInfoList[i].<field>. */
    private static int zeiAddr(int edgePVSPtr, int i, int field) {
        return edgePVSPtr + ZEdgePVSHeaderT_EdgeInfoList_v + i * ZEdgeInfoT_SizeOf_l + field;
    }

    // ===== inlines (zone_inline.h) =====

    private static boolean Zone_IsValidZoneID(int id) {
        return id >= 0 && id < Mem.w(Lvl_NumZones_w);
    }

    private static boolean Zone_IsValidEdgeID(int id) {
        return id >= 0;
    }

    private static boolean Zone_IsDoor(int zoneID) {
        return Zone_IsValidZoneID(zoneID)
            && (Mem.ub(Zone_DoorMap_vb + (zoneID >> 3)) & (1 << (zoneID & 7))) != 0;
    }

    /** Zone_GetEdgeList : zonePtr + z_EdgeListOffset (offset négatif signé). */
    private static int Zone_GetEdgeList(int zonePtr) {
        return zonePtr + (short) Mem.w(zonePtr + ZoneT_EdgeListOffset_w);
    }

    /** Zone_SideOfEdge : produit en croix (e_Len x (coord - e_Pos)). */
    private static int Zone_SideOfEdge(int edgePtr, int coordPtr) {
        return Mem.w(edgePtr + EdgeT_XLen_w) * (Mem.w(coordPtr + Vec2W_Z) - Mem.w(edgePtr + EdgeT_ZPos_w))
             - Mem.w(edgePtr + EdgeT_ZLen_w) * (Mem.w(coordPtr + Vec2W_X) - Mem.w(edgePtr + EdgeT_XPos_w));
    }

    /** sideOfDirection : produit en croix (dir x (point - org)). */
    private static int sideOfDirection(int org, int dir, int point) {
        return Mem.w(dir + Vec2W_X) * (Mem.w(point + Vec2W_Z) - Mem.w(org + Vec2W_Z))
             - Mem.w(dir + Vec2W_Z) * (Mem.w(point + Vec2W_X) - Mem.w(org + Vec2W_X));
    }

    private static int sinw(int a) {
        return Mem.w(SinCosTable_vw + ((a & (SINTAB_SIZE - 2)) >> 1) * 2);
    }

    private static int cosw(int a) {
        return Mem.w(SinCosTable_vw + (((a + SINTAB_SIZE / 4) & (SINTAB_SIZE - 2)) >> 1) * 2);
    }

    /** heightOf : (WORD)(level >> 8). */
    private static int heightOf(int level) {
        return (short) (level >> 8);
    }

    private static boolean zone_HasUpper(int zone) {
        int floor = heightOf(Mem.l(zone + ZoneT_UpperFloor_l)); // heightOf(zone->z_UpperFloor)
        return floor < DISABLED_HEIGHT && floor > heightOf(Mem.l(zone + ZoneT_UpperRoof_l)); // floor > heightOf(z_UpperRoof)
    }

    private static int zone_GetLowerLevel(int zone) {
        return zone + ZoneT_Floor_l;        // &zone->z_Floor
    }

    private static int zone_GetUpperLevel(int zone) {
        return zone + ZoneT_UpperFloor_l;   // &zone->z_UpperFloor
    }

    private static boolean zone_LevelOverlap(int z1, int z2) {
        int floor = heightOf(Mem.l(z2 + ZLP_Floor)); // heightOf(z2->zlp_Floor)
        int roof = heightOf(Mem.l(z1 + ZLP_Roof));   // heightOf(z1->zlp_Roof)
        if (roof >= floor) {
            return false;
        }
        floor = heightOf(Mem.l(z1 + ZLP_Floor));     // heightOf(z1->zlp_Floor)
        roof = heightOf(Mem.l(z2 + ZLP_Roof));       // heightOf(z2->zlp_Roof)
        if (roof >= floor) {
            return false;
        }
        return true;
    }

    // ===== construction des données EdgePVS =====

    /**
     * zone_MakePVSZoneIDList — copie les Zone ID du ZPVSRecord set dans un buffer,
     * terminé par ZONE_ID_LIST_END. Renvoie l'adresse de fin de liste.
     */
    private static int zone_MakePVSZoneIDList(int zonePtr, int bufferPtr) {
        int pvsPtr = zonePtr + ZoneT_PotVisibleZoneList_vw;             // &zonePtr->z_PotVisibleZoneList[0]
        while (Zone_IsValidZoneID(Mem.w(pvsPtr + ZPVSRecordT_ZoneID_w))) { // while (Zone_IsValidZoneID(pvsPtr->pvs_ZoneID))
            Mem.ww(bufferPtr, Mem.w(pvsPtr + ZPVSRecordT_ZoneID_w));    // *bufferPtr++ = pvsPtr->pvs_ZoneID;
            bufferPtr += 2;
            pvsPtr += ZPVSRecordT_SizeOf_l;                            // ++pvsPtr;
        }
        Mem.ww(bufferPtr, ZONE_ID_LIST_END);                          // *bufferPtr++ = ZONE_ID_LIST_END;
        bufferPtr += 2;
        return bufferPtr;                                              // return bufferPtr;
    }

    /** zone_CountJoiningEdges — nombre d'arêtes joignantes valides (avec crossing). */
    private static int zone_CountJoiningEdges(int zonePtr) {
        int numEdges = 0;                                  // WORD numEdges = 0;
        int zEdgeList = Zone_GetEdgeList(zonePtr);         // WORD const* zEdgeList = Zone_GetEdgeList(zonePtr);
        int edgeID;
        while (true) {                                     // while (Zone_IsValidEdgeID((edgeID = *zEdgeList++)))
            edgeID = Mem.w(zEdgeList);
            zEdgeList += 2;
            if (!Zone_IsValidEdgeID(edgeID)) {
                break;
            }
            int nextZoneID = Mem.w(edgeAddr(edgeID) + EdgeT_JoinZone_w); // nextZoneID = Lvl_ZoneEdgePtr_l[edgeID].e_JoinZoneID
            if (Zone_IsValidZoneID(nextZoneID)) {          // if (Zone_IsValidZoneID(nextZoneID))
                int crossing = Zone_DetermineCrossing(zonePtr, zoneAddr(nextZoneID)); // Zone_DetermineCrossing(zonePtr, Lvl_ZonePtrsPtr_l[nextZoneID])
                if (crossing != NO_PATH) {                 // if (crossing != NO_PATH)
                    ++numEdges;                            //   ++numEdges;
                }
            }
        }
        return numEdges;
    }

    /** zone_CountPVS — remplit pvsCountPtr (numZones, numJoins, features, dataSize). */
    private static void zone_CountPVS(int zonePtr, int pvsCountPtr) {
        int start = zonePtr + ZoneT_PotVisibleZoneList_vw;  // &zonePtr->z_PotVisibleZoneList[0]
        int pvsPtr = start;
        Mem.ww(pvsCountPtr + ZPVSCOUNT_features, 0);        // pvsCountPtr->features = 0;
        while (Zone_IsValidZoneID(Mem.w(pvsPtr + ZPVSRecordT_ZoneID_w))) { // while (Zone_IsValidZoneID(pvsPtr->pvs_ZoneID))
            if ((Mem.uw(pvsCountPtr + ZPVSCOUNT_features) & PVSCF_DOOR) == 0
                && Zone_IsDoor(Mem.w(pvsPtr + ZPVSRecordT_ZoneID_w))) {    // if (!(features & PVSCF_DOOR) && Zone_IsDoor(...))
                Mem.ww(pvsCountPtr + ZPVSCOUNT_features,
                    Mem.uw(pvsCountPtr + ZPVSCOUNT_features) | PVSCF_DOOR); // features |= PVSCF_DOOR;
            }
            pvsPtr += ZPVSRecordT_SizeOf_l;                // ++pvsPtr;
        }
        int numZones = (pvsPtr - start) / ZPVSRecordT_SizeOf_l; // (WORD)(pvsPtr - &z_PotVisibleZoneList[0])
        Mem.ww(pvsCountPtr + ZPVSCOUNT_numZones, numZones);     // pvsCountPtr->numZones = ...
        int numJoins = zone_CountJoiningEdges(zonePtr);
        Mem.ww(pvsCountPtr + ZPVSCOUNT_numJoins, numJoins);     // pvsCountPtr->numJoins = ...
        int features = Mem.uw(pvsCountPtr + ZPVSCOUNT_features);

        // dataSize = sizeof(ZEdgePVSHeader) - sizeof(ZEdgeInfo) + numJoins*(sizeof(ZEdgeInfo)+numZones)
        long dataSize = (long) (ZEdgePVSHeaderT_SizeOf_l - ZEdgeInfoT_SizeOf_l)
            + (long) numJoins * (ZEdgeInfoT_SizeOf_l + (long) numZones);
        if ((features & PVSCF_DOOR) != 0) {                // if (features & PVSCF_DOOR)
            dataSize += (long) numZones * numJoins * ZDoorListMask_SizeOf; // += numZones*numJoins*sizeof(ZDoorListMask)
        }
        Mem.ww(pvsCountPtr + ZPVSCOUNT_dataSize, (int) ((dataSize + 1) & ~1L)); // dataSize = Sys_Round2(dataSize)
    }

    /** zone_CalcEdgePVSDataSize — taille totale + remplit pvsCountBuffer par zone. */
    private static long zone_CalcEdgePVSDataSize(int pvsCountBufferPtr) {
        long totalSize = (long) Mem.w(Lvl_NumZones_w) * 4; // Lvl_NumZones_w * sizeof(ZEdgePVSHeader*)
        for (int zoneID = 0; zoneID < Mem.w(Lvl_NumZones_w); ++zoneID) {
            int zonePtr = zoneAddr(zoneID);                // Lvl_ZonePtrsPtr_l[zoneID]
            zone_CountPVS(zonePtr, pvsCountBufferPtr);
            totalSize += Mem.uw(pvsCountBufferPtr + ZPVSCOUNT_dataSize); // totalSize += pvsCountBufferPtr->dataSize
            pvsCountBufferPtr += ZPVSCOUNT_SIZEOF;         // ++pvsCountBufferPtr;
        }
        return totalSize;
    }

    /** zone_AllocEdgePVS — calcule et alloue (AllocVec) la table + données. */
    private static int zone_AllocEdgePVS(int pvsCountBufferPtr) {
        long totalSize = zone_CalcEdgePVSDataSize(pvsCountBufferPtr);
        // dprintf no-op
        totalSize = (totalSize + 3) & ~3L;                 // Sys_Round4(totalSize)
        return ExecLib.AllocVec((int) totalSize, MEMF_ANY); // AllocVec(totalSize, MEMF_ANY)
    }

    /** zone_CalcZEdgePVSHeaderOffsets — calcule les offsets de masques de l'en-tête. */
    private static void zone_CalcZEdgePVSHeaderOffsets(int currentEdgePVSPtr, int features) {
        // offset = (sizeof(ZEdgePVSHeader) - sizeof(ZEdgeInfo)) + zep_EdgeCount * sizeof(ZEdgeInfo)
        long offset = (ZEdgePVSHeaderT_SizeOf_l - ZEdgeInfoT_SizeOf_l)
            + (long) Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_EdgeCount_w) * ZEdgeInfoT_SizeOf_l;
        Mem.ww(currentEdgePVSPtr + ZEdgePVSHeaderT_ZoneMaskOffset_w, (int) offset); // zep_ZoneMaskOffset = offset
        if ((features & PVSCF_DOOR) != 0) {                // if (features & PVSCF_DOOR)
            long t = (long) Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_ListSize_w)
                * Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_EdgeCount_w);          // zep_ListSize * zep_EdgeCount
            offset += (t + 1) & ~1L;                       // offset += Sys_Round2(...)
            Mem.ww(currentEdgePVSPtr + ZEdgePVSHeaderT_DoorMaskOffset_w, (int) offset); // zep_DoorMaskOffset = offset
        } else {
            Mem.ww(currentEdgePVSPtr + ZEdgePVSHeaderT_DoorMaskOffset_w, 0); // zep_DoorMaskOffset = 0
        }
        Mem.ww(currentEdgePVSPtr + ZEdgePVSHeaderT_LiftMaskOffset_w, 0); // zep_LiftMaskOffset = 0
    }

    /** zone_FillZEdgePVSHeaders — construit la table de pointeurs et les en-têtes. */
    private static void zone_FillZEdgePVSHeaders(int currentEdgePVSPtr, int pvsCountBufferPtr) {
        for (int zoneID = 0; zoneID < Mem.w(Lvl_NumZones_w); ++zoneID) {
            Mem.ww(currentEdgePVSPtr + ZEdgePVSHeaderT_ZoneID_w, zoneID);                                       // zep_ZoneID = zoneID
            Mem.ww(currentEdgePVSPtr + ZEdgePVSHeaderT_ListSize_w, Mem.w(pvsCountBufferPtr + ZPVSCOUNT_numZones)); // zep_ListSize = numZones
            Mem.ww(currentEdgePVSPtr + ZEdgePVSHeaderT_EdgeCount_w, Mem.w(pvsCountBufferPtr + ZPVSCOUNT_numJoins)); // zep_EdgeCount = numJoins

            zone_CalcZEdgePVSHeaderOffsets(currentEdgePVSPtr, Mem.uw(pvsCountBufferPtr + ZPVSCOUNT_features));

            Mem.wl(Mem.l(Lvl_ZEdgePVSHeaderPtrsPtr_l) + 4 * zoneID, currentEdgePVSPtr); // Lvl_ZEdgePVSHeaderPtrsPtr_l[zoneID] = currentEdgePVSPtr

            int zonePtr = zoneAddr(zoneID);                // Lvl_ZonePtrsPtr_l[zoneID]
            int zEdgeList = Zone_GetEdgeList(zonePtr);     // Zone_GetEdgeList(zonePtr)

            int edgeIndex = 0;                             // WORD edgeIndex = 0;
            int edgeID;
            while (true) {                                 // while (Zone_IsValidEdgeID((edgeID = *zEdgeList++)))
                edgeID = Mem.w(zEdgeList);
                zEdgeList += 2;
                if (!Zone_IsValidEdgeID(edgeID)) {
                    break;
                }
                int nextZoneID = Mem.w(edgeAddr(edgeID) + EdgeT_JoinZone_w); // Lvl_ZoneEdgePtr_l[edgeID].e_JoinZoneID
                if (Zone_IsValidZoneID(nextZoneID)) {
                    int crossing = Zone_DetermineCrossing(zonePtr, zoneAddr(nextZoneID));
                    if (crossing != NO_PATH) {
                        Mem.ww(zeiAddr(currentEdgePVSPtr, edgeIndex, ZEdgeInfoT_EdgeID_w), edgeID); // zep_EdgeInfoList[edgeIndex++].zei_EdgeID = edgeID
                        edgeIndex++;
                    }
                }
            }
            currentEdgePVSPtr += Mem.uw(pvsCountBufferPtr + ZPVSCOUNT_dataSize); // currentEdgePVSPtr += dataSize
            pvsCountBufferPtr += ZPVSCOUNT_SIZEOF;                               // ++pvsCountBufferPtr;
        }
    }

    /** zone_GetIndexInPVSList — index (en mots) d'une zone dans zre_FullPVSListPtr. */
    private static int zone_GetIndexInPVSList(int zoneID) {
        int nextIDPtr = zre_FullPVSListPtr;                // WORD *nextIDPtr = zre_FullPVSListPtr;
        while (Zone_IsValidZoneID(Mem.w(nextIDPtr))) {     // while (Zone_IsValidZoneID(*nextIDPtr))
            if (zoneID == Mem.w(nextIDPtr)) {              // if (zoneID == *nextIDPtr)
                return (nextIDPtr - zre_FullPVSListPtr) / 2; // return nextIDPtr - zre_FullPVSListPtr;
            }
            nextIDPtr += 2;                                // ++nextIDPtr;
        }
        return ZONE_ID_LIST_END;
    }

    /** zone_GetInitialDoorMask — 1<<doorIndex si zone porte, sinon 0. */
    private static int zone_GetInitialDoorMask(int zoneID) {
        int doorIndex = ZoneLiftablePvs.Zone_GetDoorID(zoneID); // Zone_GetDoorID(zoneID)
        if (doorIndex != NOT_A_DOOR) {
            return (1 << doorIndex) & 0xFFFF;              // return 1 << doorIndex;
        }
        return 0;
    }

    /** zone_RecurseEdgePVS — descente récursive du PVS via les arêtes face au point de vue. */
    private static void zone_RecurseEdgePVS(int indexInPVS, int doorMask) {
        if (++zre_RecursionDepth > zre_MaxRecursionDepth) {   // if (++zre_RecursionDepth > zre_MaxRecursionDepth)
            zre_MaxRecursionDepth = zre_RecursionDepth;       //   zre_MaxRecursionDepth = zre_RecursionDepth;
        }
        ++zre_OperationCount;                                 // ++zre_OperationCount;

        int zoneID = Mem.w(zre_FullPVSListPtr + indexInPVS * 2); // zoneID = zre_FullPVSListPtr[indexInPVS]
        int visType = ZVIS_DIRECT;                            // WORD visType = ZVIS_DIRECT;
        Mem.wb(zre_EdgePVSList + indexInPVS, ZVIS_DIRECT);    // zre_EdgePVSList[indexInPVS] = ZVIS_DIRECT;

        if (zre_DoorMaskList != 0) {                          // if (zre_DoorMaskList)
            int myDoorMask = zone_GetInitialDoorMask(zoneID); // ZDoorListMask myDoorMask = zone_GetInitialDoorMask(zoneID)
            if (myDoorMask != 0) {                            // if (myDoorMask)
                doorMask |= myDoorMask;                       //   doorMask |= myDoorMask;
                visType = ZVIS_DOOR;                          //   visType = ZVIS_DOOR;
            } else if (doorMask != 0) {                       // else if (doorMask)
                visType = ZVIS_COND;                          //   visType = ZVIS_COND;
            }
            Mem.ww(zre_DoorMaskList + indexInPVS * 2, doorMask); // zre_DoorMaskList[indexInPVS] = doorMask;
        }

        Mem.wb(zre_EdgePVSList + indexInPVS, visType);        // zre_EdgePVSList[indexInPVS] = visType;

        int currentEdgePVSPtr = headerPtr(zoneID);            // Lvl_ZEdgePVSHeaderPtrsPtr_l[zoneID]

        for (int edgeNum = 0; edgeNum < Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_EdgeCount_w); ++edgeNum) {
            int edgePtr = edgeAddr(Mem.w(zeiAddr(currentEdgePVSPtr, edgeNum, ZEdgeInfoT_EdgeID_w))); // &Lvl_ZoneEdgePtr_l[...zei_EdgeID]
            int nextZoneID = Mem.w(edgePtr + EdgeT_JoinZone_w); // edgePtr->e_JoinZoneID

            indexInPVS = zone_GetIndexInPVSList(nextZoneID);  // indexInPVS = zone_GetIndexInPVSList(nextZoneID)

            if (indexInPVS == ZONE_ID_LIST_END) {             // if (indexInPVS == ZONE_ID_LIST_END)
                continue;
            }

            int nextVis = Mem.ub(zre_EdgePVSList + indexInPVS); // WORD nextVis = zre_EdgePVSList[indexInPVS];
            if (nextVis >= ZVIS_DOOR) {                       // if (nextVis >= ZVIS_DOOR)
                continue;
            } else if (nextVis > ZVIS_NONE && visType < ZVIS_DIRECT) { // else if (nextVis > ZVIS_NONE && visType < ZVIS_DIRECT)
                continue;
            }

            // Le point de vue fait-il face à l'arête ? (test des deux extrémités)
            if (
                Zone_SideOfEdge(edgePtr, zre_ViewPoint1) < 0 ||  // Zone_SideOfEdge(edgePtr, &zre_ViewPoint1) < 0
                Zone_SideOfEdge(edgePtr, zre_ViewPoint2) < 0     // Zone_SideOfEdge(edgePtr, &zre_ViewPoint2) < 0
            ) {
                zone_RecurseEdgePVS(indexInPVS, doorMask);    // zone_RecurseEdgePVS(indexInPVS, doorMask)
            }
        }

        --zre_RecursionDepth;                                 // --zre_RecursionDepth;
    }

    /** zone_FillZEdgePVSListData — remplit les données PVS par arête (récursif). */
    private static void zone_FillZEdgePVSListData() {
        zre_FullPVSListPtr = Sys_Workspace_vl;   // Sys_GetTemporaryWorkspace()
        zre_RecursionDepth = 0;
        zre_MaxRecursionDepth = 0;
        zre_OperationCount = 0;
        for (int zoneID = 0; zoneID < Mem.w(Lvl_NumZones_w); ++zoneID) {

            zre_rootZonePtr = zoneAddr(zoneID);              // Lvl_ZonePtrsPtr_l[zoneID]

            zone_MakePVSZoneIDList(zre_rootZonePtr, zre_FullPVSListPtr);

            int currentEdgePVSPtr = headerPtr(zoneID);       // Lvl_ZEdgePVSHeaderPtrsPtr_l[zoneID]

            zre_EdgePVSList = currentEdgePVSPtr + Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_ZoneMaskOffset_w); // Zone_GetEdgePVSListBase
            int doorOff = Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_DoorMaskOffset_w);
            zre_DoorMaskList = doorOff != 0 ? currentEdgePVSPtr + doorOff : 0; // Zone_GetEdgePVSDoorListBase (null si 0)

            if (Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_EdgeCount_w) > 10) { // if (zep_EdgeCount > 10)
                continue;                                    // dprintf erreur + continue
            }

            int doorMask = zone_GetInitialDoorMask(zoneID);  // ZDoorListMask doorMask = zone_GetInitialDoorMask(zoneID)

            for (int edgeNum = 0; edgeNum < Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_EdgeCount_w); ++edgeNum) {
                int edgePtr = edgeAddr(Mem.w(zeiAddr(currentEdgePVSPtr, edgeNum, ZEdgeInfoT_EdgeID_w))); // &Lvl_ZoneEdgePtr_l[...zei_EdgeID]

                Mem.ww(zre_ViewPoint1 + Vec2W_X, Mem.w(edgePtr + EdgeT_XPos_w)); // zre_ViewPoint1.v_X = e_Pos.v_X
                Mem.ww(zre_ViewPoint1 + Vec2W_Z, Mem.w(edgePtr + EdgeT_ZPos_w)); // zre_ViewPoint1.v_Z = e_Pos.v_Z

                Mem.ww(zre_ViewPoint2 + Vec2W_X, Mem.w(edgePtr + EdgeT_XPos_w) + Mem.w(edgePtr + EdgeT_XLen_w)); // zre_ViewPoint2.v_X = e_Pos.v_X + e_Len.v_X
                Mem.ww(zre_ViewPoint2 + Vec2W_Z, Mem.w(edgePtr + EdgeT_ZPos_w) + Mem.w(edgePtr + EdgeT_ZLen_w)); // zre_ViewPoint2.v_Z = e_Pos.v_Z + e_Len.v_Z

                Mem.wb(zre_EdgePVSList, ZVIS_DIRECT);        // zre_EdgePVSList[0] = ZVIS_DIRECT;

                if (zre_DoorMaskList != 0) {                 // if (zre_DoorMaskList)
                    Mem.ww(zre_DoorMaskList, doorMask);      //   zre_DoorMaskList[0] = doorMask;
                }
                for (int i = 1; i < Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_ListSize_w); ++i) { // for (i=1; i<zep_ListSize; ++i)
                    Mem.wb(zre_EdgePVSList + i, 0);          //   zre_EdgePVSList[i] = 0;
                }

                int indexInPVS = zone_GetIndexInPVSList(Mem.w(edgePtr + EdgeT_JoinZone_w)); // zone_GetIndexInPVSList(edgePtr->e_JoinZoneID)

                if (indexInPVS > ZONE_ID_LIST_END) {         // if (indexInPVS > ZONE_ID_LIST_END)
                    zone_RecurseEdgePVS(indexInPVS, doorMask);
                }

                if (zre_DoorMaskList != 0) {                 // if (zre_DoorMaskList)
                    zre_DoorMaskList += Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_ListSize_w) * ZDoorListMask_SizeOf; // zre_DoorMaskList += zep_ListSize
                }
                zre_EdgePVSList += Mem.w(currentEdgePVSPtr + ZEdgePVSHeaderT_ListSize_w); // zre_EdgePVSList += zep_ListSize
            }
        }
        // dprintf récap (no-op)
    }

    /** zone_GetPointIndex — index du point (matching sur le long XZ), -1 si absent. */
    private static int zone_GetPointIndex(int p) {
        int pointPtr = Mem.l(Lvl_PointsPtr_l);             // ULONG const* pointPtr = (ULONG*)Lvl_PointsPtr_l;
        int match = Mem.l(p);                              // ULONG match = *((ULONG*)p);
        for (int i = 0; i < Mem.w(Lvl_NumPoints_w); ++i) { // for (i=0; i<Lvl_NumPoints_w; ++i)
            if (match == Mem.l(pointPtr + i * 4)) {        // if (match == pointPtr[i])
                return i;                                   //   return i;
            }
        }
        return -1;
    }

    /** zone_FillEdgePointIndexes — résout les index de points de début/fin d'arête. */
    private static void zone_FillEdgePointIndexes() {
        for (int zoneID = 0; zoneID < Mem.w(Lvl_NumZones_w); ++zoneID) {
            int edgePVSPtr = headerPtr(zoneID);            // Lvl_ZEdgePVSHeaderPtrsPtr_l[zoneID]
            for (int i = 0; i < Mem.w(edgePVSPtr + ZEdgePVSHeaderT_EdgeCount_w); ++i) {
                int edgePtr = edgeAddr(Mem.w(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_EdgeID_w))); // &Lvl_ZoneEdgePtr_l[...zei_EdgeID]
                Mem.ww(scratch_end + Vec2W_X, Mem.w(edgePtr + EdgeT_XPos_w) + Mem.w(edgePtr + EdgeT_XLen_w)); // end.v_X = e_Pos.v_X + e_Len.v_X
                Mem.ww(scratch_end + Vec2W_Z, Mem.w(edgePtr + EdgeT_ZPos_w) + Mem.w(edgePtr + EdgeT_ZLen_w)); // end.v_Z = e_Pos.v_Z + e_Len.v_Z
                Mem.ww(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_StartPointID_w), zone_GetPointIndex(edgePtr + EdgeT_XPos_w)); // zei_StartPointID = zone_GetPointIndex(&edgePtr->e_Pos)
                Mem.ww(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_EndPointID_w), zone_GetPointIndex(scratch_end));            // zei_EndPointID = zone_GetPointIndex(&end)
            }
        }
    }

    /** Zone_InitEdgePVS — alloue et initialise les données PVS par arête. */
    public static void Zone_InitEdgePVS() {
        ZoneLiftablePvs.Zone_InitDoorList();               // Zone_InitDoorList();
        // ULONG infoTupleBufferSize = Lvl_NumZones_w * 3 * sizeof(WORD);  (debug only)
        int pvsCountBufferPtr = Sys_Workspace_vl;          // (ZPVSCount*)Sys_GetTemporaryWorkspace()
        int alloc = zone_AllocEdgePVS(pvsCountBufferPtr);
        Mem.wl(Lvl_ZEdgePVSHeaderPtrsPtr_l, alloc);        // Lvl_ZEdgePVSHeaderPtrsPtr_l = zone_AllocEdgePVS(...)
        int base = Mem.l(Lvl_ZEdgePVSHeaderPtrsPtr_l) + Mem.w(Lvl_NumZones_w) * 4; // Zone_ZEdgePVSHeaderBase(...)
        zone_FillZEdgePVSHeaders(base, pvsCountBufferPtr);
        zone_FillZEdgePVSListData();
        zone_FillEdgePointIndexes();
        // Assume doors closed on level start
        Mem.ww(Zone_RenderDoorState_w, 0);                 // Zone_RenderDoorState_w =
        Mem.ww(Zone_CurrentDoorState_w, 0);                // Zone_CurrentDoorState_w = 0;
    }

    /** Zone_FreeEdgePVS — libère les données. */
    public static void Zone_FreeEdgePVS() {
        if (Mem.l(Lvl_ZEdgePVSHeaderPtrsPtr_l) != 0) {     // if (Lvl_ZEdgePVSHeaderPtrsPtr_l)
            ExecLib.FreeVec(Mem.l(Lvl_ZEdgePVSHeaderPtrsPtr_l)); // FreeVec(...)
            Mem.wl(Lvl_ZEdgePVSHeaderPtrsPtr_l, 0);        // Lvl_ZEdgePVSHeaderPtrsPtr_l = NULL;
        }
    }

    /** Zone_UpdateVectors — met à jour le point de vue et les vecteurs perp/FOV. */
    public static void Zone_UpdateVectors() {
        if (Mem.b(Plr_MultiplayerType_b) == MULTIPLAYER_SLAVE) { // if (Plr_MultiplayerType_b == MULTIPLAYER_SLAVE)
            Mem.ww(zone_ViewPoint + Vec2W_X, Mem.w(Plr2_Position_vl + POS_X * 2)); // zone_ViewPoint.v_X = Plr2_Position_vl[POS_X]
            Mem.ww(zone_ViewPoint + Vec2W_Z, Mem.w(Plr2_Position_vl + POS_Z * 2)); // zone_ViewPoint.v_Z = Plr2_Position_vl[POS_Z]
        } else {
            Mem.ww(zone_ViewPoint + Vec2W_X, Mem.w(Plr1_Position_vl + POS_X * 2)); // zone_ViewPoint.v_X = Plr1_Position_vl[POS_X]
            Mem.ww(zone_ViewPoint + Vec2W_Z, Mem.w(Plr1_Position_vl + POS_Z * 2)); // zone_ViewPoint.v_Z = Plr1_Position_vl[POS_Z]
        }
        Mem.ww(zone_PerpDir + Vec2W_X, -Mem.w(Vis_CosVal_w)); // zone_PerpDir.v_X = -Vis_CosVal_w
        Mem.ww(zone_PerpDir + Vec2W_Z, Mem.w(Vis_SinVal_w));  // zone_PerpDir.v_Z = Vis_SinVal_w

        int fovAngle = (short) (Mem.w(Vis_AngPos_w) - (Zone_PVSFieldOfView >> 1)); // WORD fovAngle = Vis_AngPos_w - (FOV>>1)
        Mem.ww(zone_LeftFOVDir + Vec2W_X, sinw(fovAngle));   // zone_LeftFOVDir.v_X = sinw(fovAngle)
        Mem.ww(zone_LeftFOVDir + Vec2W_Z, cosw(fovAngle));   // zone_LeftFOVDir.v_Z = cosw(fovAngle)

        fovAngle = (short) (fovAngle + Zone_PVSFieldOfView); // fovAngle += Zone_PVSFieldOfView
        Mem.ww(zone_RightFOVDir + Vec2W_X, sinw(fovAngle));  // zone_RightFOVDir.v_X = sinw(fovAngle)
        Mem.ww(zone_RightFOVDir + Vec2W_Z, cosw(fovAngle));  // zone_RightFOVDir.v_Z = cosw(fovAngle)
    }

    /** zone_ClearEdgePVSBuffer — Zone_PVSMask_vb[0]=0xFF, le reste 0. */
    public static void zone_ClearEdgePVSBuffer(int size) {
        Mem.wb(Zone_PVSMask_vb, 0xFF);                     // Zone_PVSMask_vb[0] = 0xFF;
        for (int i = 1; i < size; ++i) {                   // for (i=1; i<size; ++i)
            Mem.wb(Zone_PVSMask_vb + i, 0);                //   Zone_PVSMask_vb[i] = 0;
        }
    }

    /** zone_MergeEdgePVS — fusionne un masque de zone d'arête (filtré par les portes). */
    public static void zone_MergeEdgePVS(int zoneMaskPtr, int doorListMaskPtr, int size) {
        if (doorListMaskPtr != 0) {                        // if (doorListMaskPtr)
            int mask = Mem.uw(Zone_RenderDoorState_w);     // ZDoorListMask mask = Zone_RenderDoorState_w;
            for (int i = 1; i < size; ++i) {
                int dlm = Mem.uw(doorListMaskPtr + i * 2); // doorListMaskPtr[i]
                Mem.wb(Zone_PVSMask_vb + i,
                    Mem.ub(Zone_PVSMask_vb + i) | (((dlm & mask) == dlm) ? Mem.ub(zoneMaskPtr + i) : 0)); // |= ((dlm & mask) == dlm) ? zoneMaskPtr[i] : 0
            }
        } else {
            for (int i = 1; i < size; ++i) {
                Mem.wb(Zone_PVSMask_vb + i, Mem.ub(Zone_PVSMask_vb + i) | Mem.ub(zoneMaskPtr + i)); // |= zoneMaskPtr[i]
            }
        }
    }

    /** zone_MarkVisibleViaEdges — recopie le masque visible dans z_Unused de chaque zone du PVS. */
    public static void zone_MarkVisibleViaEdges(int size) {
        int zoneID = Mem.w(Mem.l(Lvl_ListOfGraphRoomsPtr_l) + ZPVSRecordT_ZoneID_w); // Lvl_ListOfGraphRoomsPtr_l->pvs_ZoneID
        zone_MakePVSZoneIDList(zoneAddr(zoneID), Zone_PVSList_vw);   // zone_MakePVSZoneIDList(Lvl_ZonePtrsPtr_l[zoneID], &Zone_PVSList_vw[0])
        for (int i = 0; i < size; ++i) {
            int z = Mem.w(Zone_PVSList_vw + i * 2);                  // Zone_PVSList_vw[i]
            Mem.ww(zoneAddr(z) + ZoneT_Unused_w, Mem.ub(Zone_PVSMask_vb + i)); // Lvl_ZonePtrsPtr_l[...]->z_Unused = Zone_PVSMask_vb[i]
        }
    }

    /** Zone_CheckVisibleEdges — détermine les arêtes joignantes effectivement visibles. */
    public static void Zone_CheckVisibleEdges() {
        int zoneID = Mem.w(Mem.l(Lvl_ListOfGraphRoomsPtr_l) + ZPVSRecordT_ZoneID_w); // Lvl_ListOfGraphRoomsPtr_l->pvs_ZoneID

        int edgePVSPtr = headerPtr(zoneID);                // Lvl_ZEdgePVSHeaderPtrsPtr_l[zoneID]
        int edgePVSListPtr = edgePVSPtr + Mem.w(edgePVSPtr + ZEdgePVSHeaderT_ZoneMaskOffset_w); // Zone_GetEdgePVSListBase
        int doorOff = Mem.w(edgePVSPtr + ZEdgePVSHeaderT_DoorMaskOffset_w);
        int doorListMaskPtr = doorOff != 0 ? edgePVSPtr + doorOff : 0; // Zone_GetEdgePVSDoorListBase (null si 0)
        int numVisible = 0;                                // WORD numVisible = 0;
        int edgeID;
        int visJoinMask = 0;                               // UWORD visJoinMask = 0;
        int listSize = Mem.w(edgePVSPtr + ZEdgePVSHeaderT_ListSize_w);
        int edgeCount = Mem.w(edgePVSPtr + ZEdgePVSHeaderT_EdgeCount_w);
        int doorListStep = doorListMaskPtr != 0 ? listSize : 0; // WORD doorListStep = doorListMaskPtr ? zep_ListSize : 0;

        Zone_UpdateVectors();
        zone_ClearEdgePVSBuffer(listSize);

        Mem.ww(Zone_RenderDoorState_w, Mem.uw(Zone_CurrentDoorState_w)); // Zone_RenderDoorState_w = Zone_CurrentDoorState_w;

        int edgePointIndex = Zone_EdgePointIndexes_vw;     // WORD* edgePointIndex = &Zone_EdgePointIndexes_vw[0];

        for (int i = 0; i < edgeCount; ++i, edgePVSListPtr += listSize, doorListMaskPtr += doorListStep * ZDoorListMask_SizeOf) {

            edgeID = Mem.w(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_EdgeID_w)); // zep_EdgeInfoList[i].zei_EdgeID

            int edgePtr = edgeAddr(edgeID);                // &Lvl_ZoneEdgePtr_l[edgeID]

            int startFlags = (sideOfDirection(zone_ViewPoint, zone_PerpDir, edgePtr) < 0) ? BIT_FRONT : 0;
            startFlags |= (sideOfDirection(zone_ViewPoint, zone_LeftFOVDir, edgePtr) <= 0) ? BIT_LEFT : 0;
            startFlags |= (sideOfDirection(zone_ViewPoint, zone_RightFOVDir, edgePtr) >= 0) ? BIT_RIGHT : 0;

            if (startFlags == (BIT_FRONT | BIT_LEFT | BIT_RIGHT)) {
                visJoinMask |= 1 << i;
                ++numVisible;
                zone_MergeEdgePVS(edgePVSListPtr, doorListMaskPtr, listSize);
                Mem.ww(edgePointIndex, Mem.w(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_StartPointID_w))); edgePointIndex += 2; // *edgePointIndex++ = zei_StartPointID
                Mem.ww(edgePointIndex, Mem.w(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_EndPointID_w))); edgePointIndex += 2;   // *edgePointIndex++ = zei_EndPointID
                continue;
            }

            Mem.ww(scratch_endPoint + Vec2W_X, Mem.w(edgePtr + EdgeT_XPos_w) + Mem.w(edgePtr + EdgeT_XLen_w)); // endPoint.v_X = e_Pos.v_X + e_Len.v_X
            Mem.ww(scratch_endPoint + Vec2W_Z, Mem.w(edgePtr + EdgeT_ZPos_w) + Mem.w(edgePtr + EdgeT_ZLen_w)); // endPoint.v_Z = e_Pos.v_Z + e_Len.v_Z

            int endFlags = (sideOfDirection(zone_ViewPoint, zone_PerpDir, scratch_endPoint) < 0) ? BIT_FRONT : 0;
            endFlags |= (sideOfDirection(zone_ViewPoint, zone_LeftFOVDir, scratch_endPoint) <= 0) ? BIT_LEFT : 0;
            endFlags |= (sideOfDirection(zone_ViewPoint, zone_RightFOVDir, scratch_endPoint) >= 0) ? BIT_RIGHT : 0;

            if (endFlags == (BIT_FRONT | BIT_LEFT | BIT_RIGHT)) {
                visJoinMask |= 1 << i;
                ++numVisible;
                zone_MergeEdgePVS(edgePVSListPtr, doorListMaskPtr, listSize);
                Mem.ww(edgePointIndex, Mem.w(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_StartPointID_w))); edgePointIndex += 2;
                Mem.ww(edgePointIndex, Mem.w(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_EndPointID_w))); edgePointIndex += 2;
                continue;
            }

            if (
                ((startFlags | endFlags) & BIT_FRONT) != 0 && // ((startFlags|endFlags) & BIT_FRONT)
                (startFlags & BIT_LEFT) == 0 &&               // (startFlags & BIT_LEFT) == 0
                (endFlags & BIT_RIGHT) == 0                   // (endFlags & BIT_RIGHT) == 0
            ) {
                visJoinMask |= 1 << i;
                ++numVisible;
                zone_MergeEdgePVS(edgePVSListPtr, doorListMaskPtr, listSize);
                Mem.ww(edgePointIndex, Mem.w(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_StartPointID_w))); edgePointIndex += 2;
                Mem.ww(edgePointIndex, Mem.w(zeiAddr(edgePVSPtr, i, ZEdgeInfoT_EndPointID_w))); edgePointIndex += 2;
                continue;
            }
        }

        Mem.ww(edgePointIndex, EDGE_POINT_ID_LIST_END);    // *edgePointIndex = EDGE_POINT_ID_LIST_END;

        Mem.ww(Zone_VisJoinMask_w, visJoinMask);           // Zone_VisJoinMask_w = visJoinMask;
        Mem.ww(Zone_VisJoins_w, numVisible);               // Zone_VisJoins_w = numVisible;
        Mem.ww(Zone_TotJoins_w, edgeCount);                // Zone_TotJoins_w = zep_EdgeCount;

        zone_MarkVisibleViaEdges(listSize);                // zone_MarkVisibleViaEdges(zep_ListSize)
    }

    /** Zone_SetupEdgeClipping — appelée depuis la boucle de sous-pièces (ASM). */
    public static void Zone_SetupEdgeClipping() {
        Mem.ww(Draw_ZoneClipL_w, 0);                       // Draw_ZoneClipL_w = 0;
        Mem.ww(Draw_ZoneClipR_w, Mem.w(Vid_RightX_w));     // Draw_ZoneClipR_w = Vid_RightX_w;
        Mem.wb(Draw_ForceZoneSkip_b, 0);                   // Draw_ForceZoneSkip_b = 0;

        int minL = Mem.w(Vid_RightX_w);                    // WORD minL = Vid_RightX_w;
        int maxR = 0;                                      // WORD maxR = 0;
        if (Mem.w(Zone_VisJoins_w) > 0
            && Mem.w(Mem.l(Lvl_ListOfGraphRoomsPtr_l) + ZPVSRecordT_ZoneID_w) != Mem.w(Draw_CurrentZone_w)) { // Zone_VisJoins_w > 0 && pvs_ZoneID != Draw_CurrentZone_w
            for (int i = 0; i < (Mem.w(Zone_VisJoins_w) << 1); i += 2) { // for (i=0; i<(Zone_VisJoins_w<<1); i+=2)
                int scrL = Mem.w(OnScreen_vl + Mem.w(Zone_EdgePointIndexes_vw + i * 2) * 2);       // OnScreen_vl[Zone_EdgePointIndexes_vw[i]]
                int scrR = Mem.w(OnScreen_vl + Mem.w(Zone_EdgePointIndexes_vw + (i + 1) * 2) * 2); // OnScreen_vl[Zone_EdgePointIndexes_vw[i+1]]

                if (scrL > scrR) {                         // if (scrL > scrR)
                    scrL = 0;                              //   scrL = 0;
                    scrR = Mem.w(Vid_RightX_w);            //   scrR = Vid_RightX_w;
                }

                if (scrL < minL) {                         // if (scrL < minL)
                    minL = scrL;
                }
                if (scrR > maxR) {                         // if (scrR > maxR)
                    maxR = scrR;
                }
            }
            if (minL < 0) {                                // if (minL < 0)
                minL = 0;
            }
            if (maxR > Mem.w(Vid_RightX_w)) {              // if (maxR > Vid_RightX_w)
                maxR = Mem.w(Vid_RightX_w);
            }

            Mem.ww(Draw_ZoneClipL_w, minL);                // Draw_ZoneClipL_w = minL;
            Mem.ww(Draw_ZoneClipR_w, maxR);                // Draw_ZoneClipR_w = maxR;
        }
    }

    /** Zone_DetermineCrossing — croisement possible entre deux zones selon leurs hauteurs. */
    public static int Zone_DetermineCrossing(int from, int to) {
        int result = zone_LevelOverlap(zone_GetLowerLevel(from), zone_GetLowerLevel(to)) ? LOWER_TO_LOWER : NO_PATH; // result = overlap(lower,lower) ? LOWER_TO_LOWER : NO_PATH

        int test = (zone_HasUpper(from) ? 1 : 0) | (zone_HasUpper(to) ? 2 : 0); // test = hasUpper(from)|hasUpper(to)<<1

        switch (test) {
            case 1:
                result |= zone_LevelOverlap(zone_GetUpperLevel(from), zone_GetLowerLevel(to)) ? UPPER_TO_LOWER : NO_PATH;
                break;

            case 2:
                result |= zone_LevelOverlap(zone_GetLowerLevel(from), zone_GetUpperLevel(to)) ? LOWER_TO_UPPER : NO_PATH;
                break;

            case 3:
                result |= zone_LevelOverlap(zone_GetUpperLevel(from), zone_GetLowerLevel(to)) ? UPPER_TO_LOWER : NO_PATH;
                result |= zone_LevelOverlap(zone_GetLowerLevel(from), zone_GetUpperLevel(to)) ? LOWER_TO_UPPER : NO_PATH;
                result |= zone_LevelOverlap(zone_GetUpperLevel(from), zone_GetUpperLevel(to)) ? UPPER_TO_UPPER : NO_PATH;
                break;

            default:
                break;
        }

        // if (result == NO_PATH) dprintf(...)  — no-op

        return result;
    }
}
