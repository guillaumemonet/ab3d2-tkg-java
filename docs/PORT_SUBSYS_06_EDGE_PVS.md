# Sous-système 06 — Edge PVS

Port du load-time **per-edge PVS preprocessing** — la pièce la plus
algorithmiquement complexe du moteur. C'est ce sous-système qui calcule
quelles zones sont potentiellement visibles à travers chaque arête joignante,
avec gestion des masques de portes.

**Status** : ✅ porté, 17 nouveaux tests (cumul total : **163/163**).
**Packages** : `com.team17.ab3d2.pvs`, `com.team17.ab3d2.world` (ZEdgeInfo).
**Tâches** : #34 → #38.

---

## 1. Périmètre

| Classe | Origine | Rôle |
|---|---|---|
| `DoorRegistry` | `c/zone_liftable_pvs.c:76-124` | DoorList[16] + DoorMap bitmap + getDoorId/getInitialDoorMask |
| `ZoneCrossings` | `c/zone_edge_pvs.c:923-980` + `zone_inline.h:145-161` | levelOverlap + determineCrossing (10 constants de crossing) |
| `ZEdgeInfo` (record) | `c/zone.h:108-113` | 8 octets : edgeId + startPointId + endPointId + reserved |
| `EdgePvsVisibility` | `c/zone.h:155-173` | Constantes ZVIS_NONE/COND/DOOR/LIFT/DIRECT + helpers type/id |
| `ZEdgePVSHeader` | `c/zone.h:121-138` | Données per-zone : edgeInfoList + zoneMask 2D + doorMask 2D |
| `EdgePvsBuilder` | `c/zone_edge_pvs.c:242-625` | Algorithme 4-pass : counts → headers → recursion → pointIDs |

**Portée** : ce sous-système couvre le **load-time** (phases 1-6 du code C).
Le runtime (`Zone_CheckVisibleEdges`, `Zone_SetupEdgeClipping`) sera porté
avec le sous-système rendu — il consomme directement le résultat du
preprocessing.

---

## 2. Algorithme principal

### 2.1 Vue d'ensemble

L'algorithme construit pour chaque zone un `ZEdgePVSHeader` contenant :
- La liste des **edges joignants** (= arêtes vers une zone valide avec
  recouvrement de hauteur)
- Pour chaque tel edge, un **mask 2D** {edge × zone-in-PVS} indiquant si la
  zone est visible via cet edge, et à quel "niveau" (DIRECT, COND, DOOR…)
- Optionnellement un **mask de portes** {edge × zone-in-PVS} indiquant
  quelles portes doivent être ouvertes pour voir la zone

### 2.2 Récursion sur le graphe des zones

Pour chaque edge joignant `e` de la zone racine `R` :
1. Les **viewpoints** sont les deux extrémités de `e` (en coordonnées monde).
2. On entre dans la zone voisine `N` via `e` (la zone `N` est marquée
   ZVIS_DIRECT).
3. On examine chaque edge joignant `e'` de `N` :
   - Si l'edge mène vers une zone déjà visitée avec une visibilité ≥
     courante → skip
   - Sinon, on calcule `sideOfEdge(e', viewpoint1)` et
     `sideOfEdge(e', viewpoint2)`. Si au moins un est **< 0** ("facing
     towards"), on récurse dans la zone derrière `e'`.

### 2.3 Tracking des portes

Si `DoorRegistry` indique qu'au moins une zone du PVS est une porte, un
**masque cumulé** est maintenu pendant la récursion :
- Quand on entre dans une zone porte avec `doorIndex = k`, on ajoute
  `1 << k` au masque
- La visibilité de la zone est marquée :
  - `ZVIS_DOOR` si la zone elle-même est une porte
  - `ZVIS_COND` si on a traversé une porte pour y arriver (masque non nul)
  - `ZVIS_DIRECT` sinon

À runtime, une zone n'est visible que si toutes les portes de son masque
sont effectivement ouvertes.

### 2.4 Convention "side < 0 = facing"

Le moteur d'origine documente :
```c
// < 0 facing towards, > 0 facing away, 0 colinear with
// Only visit the adjoining zone if it's strictly facing
```

Avec la convention **clockwise polygon** du moteur (cf. `docs/PVS.md`), les
points d'une zone sont listés dans l'ordre qui place l'**intérieur** sur le
côté **négatif** des edges. Donc :
- Un viewpoint **à l'intérieur** d'une zone donne `sideOfEdge < 0` sur les
  edges de cette zone (de l'intérieur, on voit les edges « face à soi »).
- Un viewpoint **à l'extérieur** (= dans la zone voisine) donne
  `sideOfEdge > 0`.

Pendant la récursion, le viewpoint est un endpoint de l'edge initial de
la zone racine. Quand on regarde un edge `e'` de la zone voisine `N` vers
une troisième zone `N2`, le viewpoint est sur le côté de `N` (donc à
l'intérieur de `N` du point de vue d'`e'`). Cela donne `sideOfEdge < 0`,
ce qui valide la récursion vers `N2`.

Test concret (scénario chain A-B-C) :

```
A: (0,0)→(0,100)→(100,100)→(100,0)         [CW]
B: (100,0)→(100,100)→(200,100)→(200,0)     [CW]
C: (200,0)→(200,100)→(300,100)→(300,0)     [CW]

A's edge to B   : pos=(100,100), len=(0,-100), join=B
B's edge to A   : pos=(100,0),   len=(0,100),  join=A
B's edge to C   : pos=(200,100), len=(0,-100), join=C
C's edge to B   : pos=(200,0),   len=(0,100),  join=B

Test viewpoint=(100,100) sur B's edge to C :
  side = 0 * ((100)-100) - (-100) * (100-200)
       = 0 - (-100 * -100) = -10000 < 0  ✓ → recurse
```

### 2.5 Index 0 = "self" du root zone

Une convention héritée du code C : `zoneMask[0]` est toujours initialisé à
`ZVIS_DIRECT` au début de chaque itération d'edge. Le commentaire C dit
« Mark the root zone as already visited ».

Cette pré-initialisation est :
- **Active** au load-time : `zoneMask[0] = ZVIS_DIRECT` avant chaque récursion
- **Active** au runtime : `Zone_PVSMask_vb[0] = 0xFF` (root zone toujours
  marqué visible)
- **Cohérente** : le merge runtime (`zone_MergeEdgePVS`) saute l'index 0
  donc la valeur per-edge à l'index 0 n'a pas d'importance fonctionnelle

Le port Java reproduit cette convention fidèlement.

---

## 3. Layout des données

### 3.1 Modèle C (in-place packed)

```
ZEdgePVSHeader (fixed) :
  zep_ZoneID, zep_ListSize, zep_EdgeCount,
  zep_ZoneMaskOffset, zep_DoorMaskOffset, zep_LiftMaskOffset
ZEdgeInfo[zep_EdgeCount]  (8 bytes each)
zoneMask : byte[zep_EdgeCount][zep_ListSize]
doorMask : word[zep_EdgeCount][zep_ListSize]   (si doors présentes)
liftMask : word[zep_EdgeCount][zep_ListSize]   (si lifts présentes — TODO)
```

Tout en un bloc contigu, accédé via offsets.

### 3.2 Modèle Java (objets typés)

```java
class ZEdgePVSHeader {
    short zoneId, listSize, edgeCount;
    ZEdgeInfo[] edgeInfoList;       // taille edgeCount
    byte[][] zoneMaskPerEdge;        // [edgeIndex][pvsIndex]
    int[][] doorMaskPerEdge;         // null si pas de porte
    int[][] liftMaskPerEdge;         // null actuellement (TODO)
}
```

Les masques 2D Java remplacent les arrays packés C. Accès direct par
indices au lieu d'offsets byte.

---

## 4. Pipeline en 4 passes

| Pass | Fonction | Action |
|---|---|---|
| 1 | `buildAll` Pass 1 | Pour chaque zone : count PVS entries, count joining edges (filter via `determineCrossing != NO_PATH`), détecte doors → crée `Builder` + edgeInfoList rempli (edgeIds seulement) |
| 2 | `fillZoneListData` (`zone_FillZEdgePVSListData`) | Pour chaque zone, pour chaque edge joignant : récursion remplit zoneMask + doorMask |
| 3 | `buildAll` Pass 3 (`zone_FillEdgePointIndexes`) | Résout les startPointId / endPointId par lookup linéaire dans la table des points |
| 4 | `buildAll` Pass 4 | Finalise les `Builder` en `ZEdgePVSHeader` immutables |

### 4.1 Pourquoi 4 passes et pas 1 ?

La récursion (Pass 2) accède aux `edgeInfoList` des zones **voisines** pour
suivre le graphe. Donc TOUTES les zones doivent avoir leur edgeInfoList
construite AVANT de lancer la récursion sur l'une d'elles. D'où la
séparation Pass 1 / Pass 2.

Pass 3 (point IDs) est séparée car indépendante des masques.

---

## 5. Subtilités algorithmiques

### 5.1 Skip edgeCount > 10 (zone_edge_pvs.c:433)

Le C original a une garde :
```c
if (currentEdgePVSPtr->zep_EdgeCount > 10) {
    dprintf("Error: Zone %d reports %d edges, skip\n", ...);
    continue;
}
```

Reproduit en Java :
```java
if (builder.edgeCount > 10) {
    return;  // skip cette zone, ses masks restent à zéro
}
```

Garde défensive contre les zones aberrantes (un polygone réaliste a 3-10
sommets, donc au plus 10 edges).

### 5.2 Conditions de re-entrée d'une zone

Quand la récursion atteint une zone déjà visitée :
- Si `nextVis >= ZVIS_DOOR` (= 64) : **skip** — une porte/ascenseur peut
  toujours être obstruée, on ne peut pas "améliorer" sa visibilité.
- Si `nextVis > ZVIS_NONE` ET `visType < ZVIS_DIRECT` (la visibilité
  courante est moins bonne) : **skip** — pas d'upgrade depuis une visibilité
  inférieure.
- Sinon : **re-recurse** — peut améliorer la visibilité (e.g. passer de
  COND à DIRECT si on entre via un chemin sans porte).

### 5.3 Recherche de point par coordonnées

Le C utilise une astuce : comparer deux Vec2W comme un seul `ULONG`
(les deux WORDs concaténés) :
```c
ULONG match = *((ULONG const*)p);
for (...) {
    if (match == pointPtr[i]) return i;
}
```

Le Java compare champ par champ (`p.x() == x && p.z() == z`) — résultat
strictement identique (les Vec2W sont 4 octets en big-endian = ULONG en
m68k big-endian = même combinaison de bits).

---

## 6. Tests (17)

### 6.1 `ZoneCrossingsTest` (5 tests)
- Constantes (NO_PATH=0, LOWER_TO_LOWER=1, …, BOTH=9)
- `levelOverlap` : cas basique, disjoint, edge-touching
- `determineCrossing` : 2 zones lower-only, lower disjoints, both with upper
  (vérification fine du BOTH = 9)

### 6.2 `DoorRegistryTest` (6 tests)
- Registre vide
- Une seule porte
- Plusieurs portes : indexes séquentiels (0, 1, 2…) indépendamment du zoneId
- Zone-IDs invalides skippés
- DoorMap bitmap correct (bits 0, 7 dans byte 0, bits 0, 7 dans byte 1)
- Limite 16 portes max

### 6.3 `EdgePvsBuilderTest` (4 tests)
- **2 zones** : A et B partagent un edge → 1 joining edge chacune, B visible
  ZVIS_DIRECT via A's edge
- **Chain A-B-C** : A.PVS = [B, C], A's edge to B → B et C tous DIRECT (C
  atteint via récursion à travers B)
- **Door dans chain** : B est une porte → B est ZVIS_DOOR, C est ZVIS_COND
  avec doorMask = 1 (door index 0)
- **Mur plein** : zone isolée avec edges = -1 (SOLID_WALL) → 0 joining edges

---

## 7. Limitations laissées au futur

1. **Lift masks** : actuellement `liftMaskPerEdge = null`. Le C original a
   un TODO explicite (`zone_edge_pvs.c:154` : « Same again for lifts »).
   À implémenter avec le sous-système lifts.

2. **Runtime check** (`Zone_CheckVisibleEdges`, `Zone_SetupEdgeClipping`) :
   non porté ici. Sera intégré au sous-système rendu où il est consommé.

3. **FOV computation** (`Zone_UpdateVectors`) : idem, runtime.

4. **Optimisation `sideOfDirection`** : le runtime utilise une fonction
   similaire à `sideOfEdge` mais sur direction unitaire au lieu d'edge —
   à porter avec le runtime.

---

## 8. Risques de divergence

| Risque | Sévérité | Mitigation |
|---|---|---|
| Convention CW vs CCW des polygones | Élevée si mal interprétée | Test concret avec chaîne A-B-C validé byte-à-byte vérifie le comportement |
| Index 0 dans PVS (root zone vs first neighbor) | Faible | Algo robuste aux deux conventions ; runtime override [0]=0xFF de toute façon |
| Limite récursion (PVS_TRAVERSE_LIMIT=100) | Faible | Pas de borne explicite dans le port — la convergence du graphe finie suffit |
| Skip edgeCount > 10 | Faible | Reproduit ; les zones aberrantes ne polluent pas le résultat |
| Comparaison de Vec2W via ULONG (C) vs field-by-field (Java) | Aucune | Résultat strictement identique |
| Overflow `sideOfEdge` 16x16→32 | Aucune | `Edge.sideOfPoint` utilise `int` (testé en sub-system 02) |

---

## 9. Récapitulatif cumulé

| Sous-système | Tests | Statut |
|---|---|---|
| 01 — Math fondamental | 39 | ✅ |
| 02 — Loader binaires | 66 | ✅ |
| 03 — Gameplay state runtime | 24 | ✅ |
| 04 — Transform3D | 11 | ✅ |
| 05 — PvsErrata | 6 | ✅ |
| **06 — Edge PVS** | **17** | **✅** |
| **Total** | **163** | ✅ |

---

## 10. Prochain sous-système

Avec **Edge PVS** terminé, le moteur a maintenant toutes les pièces de
**preprocessing** des données de niveau. Les options pour la suite :

A) **Renderer walls/floors/objects** — consomme Transform3D + Zone +
   Edge PVS + textures. Très visuel, premier pixel à l'écran.

B) **Player physics + collision** — consomme GameObject (Plr1/2 entities)
   + Edge.sideOfPoint + Zone heights. Permet le mouvement du joueur dans
   le monde.

C) **AI state machine** (`modules/ai.s`) — modifie les EntityFields chaque
   frame. État ALIEN par alien.

D) **Audio Protracker replayer** (`modules/music.s`) — port du replayer
   MOD custom avec gestion des 4 canaux Paula.

E) **Runtime PVS check** (`Zone_CheckVisibleEdges`, `Zone_SetupEdgeClipping`)
   — utilise le résultat du sous-système 06 + FOV vectors. C'est le pont
   entre le preprocessing et le rendu.

À décider à la prochaine étape.
