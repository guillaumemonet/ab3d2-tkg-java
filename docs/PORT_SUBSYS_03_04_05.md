# Sous-systèmes 03/04/05 — Gameplay state, Transform3D, PvsErrata

Trois sous-systèmes portés en séquence (A → B → C). Doc combinée pour
éviter la duplication.

**Status** : ✅ tous portés. **41 nouveaux tests** (cumul total : 146/146).
**Tâches** : #26 → #33 (terminées).

---

## Sous-système 03 (A) — Gameplay state runtime

**Package** : `com.team17.ab3d2.runtime` + extensions de `world` et `gamedata`.

### 03.1 Périmètre

| Classe | Origine | Rôle |
|---|---|---|
| `GameObject` (mutable) | `defs.i:220-234` (ObjT) | Objet runtime 64 octets : header typé + tail de 46 octets brut |
| `GameObjectType` enum | `defs.i:211-216` | ALIEN/OBJECT/PROJECTILE/AUX/PLAYER1/PLAYER2 |
| `EntityType` enum | `defs.i:279-282` | COLLECTABLE/ACTIVATABLE/DESTRUCTABLE/DECORATION |
| `EntityFields` record | `defs.i:248-277` (EntT) | Vue typée du tail quand `typeId ∈ {ALIEN, OBJECT}` |
| `ShotFields` record | `defs.i:290-314` (ShotT) | Vue typée du tail quand `typeId == PROJECTILE` |
| `ZoneIndexLists` record | `c/zone_inline.h:37-45` | Résolution des offsets négatifs `z_EdgeListOffset` / `z_Points` |
| `DoorWall` record | `c/zone_liftable.h:58-62` | 10 octets : `edgeId`, `graphicsOffset`, `long1` |
| `DoorEntry` record | `c/zone_liftable.h:64-71` | `Liftable + List<DoorWall>` + parser stream |
| `Zone.withPvs(...)` | nouveau | Crée une copie avec PVS modifiée (pour PvsErrata) |
| `Level` étendu | — | Inclut maintenant doors, lifts, gameObjects, plr1/2, shots, indexLists |
| `LevelLoader` étendu | `hires.s:255-490` | Parse doors stream, lifts stream, GameObjects array, shot slots |

### 03.2 Stratégie d'union ObjT / EntT / ShotT

L'ASM réutilise les 46 octets après le header ObjT pour différents schémas
selon `typeId`. Le port Java reproduit cette sémantique :

- `GameObject` stocke le tail comme `byte[46]` mutable et **ne l'interprète
  pas**.
- Pour accéder aux champs typés, l'appelant fait `obj.entityFields()` ou
  `obj.shotFields()` → record snapshot.
- Pour modifier, il fait `obj.writeEntityFields(newFields)` qui écrit dans
  le tail.

Avantage : pas de surcoût mémoire, sémantique d'union préservée, byte-perfect
roundtrip via `toBytes()`/`parse()`.

### 03.3 Macros FREE_OBJ / FREE_ENT

`macros.i:357-373` définit deux macros ASM pour libérer un slot d'objet :

- `FREE_OBJ a4` → `move.w #-1, ObjT_ZoneID_w(a4)` (juste le header)
- `FREE_ENT a4` → idem + `move.w #-1, EntT_ZoneID_w(a4)` (header + tail offset 8)

Reproduit en Java par `GameObject.free()` et `GameObject.freeEntity()`. Le
flag `isFree()` teste `zoneId == FREE_ZONE_ID (-1)`.

### 03.4 ZoneIndexLists — résolution des offsets négatifs

Le binaire `twolev.bin` stocke chaque zone précédée par sa liste d'edge-IDs
(terminée `-1`), suivie d'une liste de shared-edges (terminée `-2`), et
sa liste de point-IDs (terminée `-1`). Le `z_EdgeListOffset` (signed 16-bit
négatif) est l'offset depuis le début du `ZoneT` pour atteindre cette liste.

Le port :
- `ZoneIndexLists.resolve(bin, zoneAbsOffset, edgeListOffset, pointsOffset)`
  calcule les addresses absolues, lit jusqu'aux sentinelles, retourne le
  tuple `(edges, sharedEdges, points)`.
- Bornes défensives : `MAX_LIST_LENGTH = 1024` pour éviter une lecture
  infinie sur un fichier corrompu.

### 03.5 Doors / Lifts stream

Format (`zone_liftable.h:64-71`) :
```
[ZLiftable (36)][ZDoorWall (10) × 2N][-1 (2)]   ← une porte
[ZLiftable][ZDoorWall × 2N][-1]                 ← porte suivante
...
[999 (2)]                                        ← END_OF_DOOR_LIST
```

Le `DoorEntry.readStream(reader)` parse en boucle jusqu'au 999. Chaque
itération lit un `Liftable`, puis des `DoorWall` jusqu'à la sentinelle `-1`
de la liste de walls.

Les ascenseurs (lifts) utilisent **exactement le même format** — le
LevelLoader appelle `DoorEntry.readStream` deux fois (à `doorDataOffset` et
`liftDataOffset`).

### 03.6 Tests (24)

- `GameObjectTest` (9) : taille, constantes types, free macros, parse/toBytes roundtrip, copy.
- `EntityFieldsTest` (3) : parsing de chaque champ aux bons offsets, unions, write roundtrip.
- `ShotFieldsTest` (2) : idem pour Shot.
- `ZoneIndexListsTest` (4) : résolution edges + shared + points, listes vides, offsets positifs, hors-bornes.
- `DoorStreamTest` (5) : taille DoorWall, parsing, constantes, stream vide (juste 999), single door, multiple doors.
- Tous passent ✅.

---

## Sous-système 04 (B) — Transform3D

**Package** : `com.team17.ab3d2.render`.

### 04.1 Périmètre

Port de `modules/transform.s` : rotation 2D et projection perspective.
Deux modes (small screen / fullscreen) qui diffèrent par le scaling de Z.

### 04.2 Algorithme

Pour chaque point monde `(pointX, pointZ)` :

```
viewX = (short)(pointX - playerX)
viewZ = (short)(pointZ - playerZ)

; rotation 2D (sin, cos = Q15)
tx = viewX * cos - viewZ * sin   ; 32-bit signed product
tz = viewX * sin + viewZ * cos

; scaling de tx (commun aux deux modes)
tx = tx >> 8 + xwobble

; scaling de tz selon mode :
;   small :  tz = tz >> 15  (équiv. >> 8 >> 7)
;   fs    :  tz = ((tz << 2) high16 signé) * 1229 >> 12

; projection
if tz > 0 :
    screenX = (tx / tz) clamp short range + vidCentreX
else if tx > 0 :
    screenX = vidRightX  ; off-screen droite
else :
    screenX = 0          ; off-screen gauche
```

### 04.3 Aspect ratio 3/5

Le commentaire ASM dit « z' * 6/5 » mais l'opération réelle multiplie tz
par **3/5 = 0.6** (vérifié byte-à-byte). C'est correct : le screen 320×... est
5/3 fois plus large que 192×..., donc pour qu'un point à l'edge du small
écran apparaisse à l'edge du fullscreen, `screenX` doit scaler par 5/3.
Comme `screenX = tx / tz`, il faut tz / (5/3) = tz × 3/5.

Trace numérique pour un point à `(0, 1000)`, angle 0 :
- `d1 = 1000 × 32767 = 32 767 000`
- `small_tz = 32767000 >> 15 = 999`
- `fs_tz = ((d1 << 2) >> 16, signed) × 1229 >> 12 = 1999 × 1229 >> 12 = 2 456 771 >> 12 = 599`
- Ratio = 599/999 ≈ 0.6 ✅

### 04.4 Subtilités numériques

| Aspect | Java fidèle | Pitfall évité |
|---|---|---|
| `muls.w` 16×16→32 signed | `(int)(short) a * (int)(short) b` | Java auto-promotion vers int préserve le signe |
| `swap d1 + muls.w #1229, d1` | `(short)(tz >> 16) * 1229` | Extraction du high word signé via cast |
| `divs.w` overflow | clamp défensif à `[Short.MIN, Short.MAX]` | divs.w peut trapper sur m68k ; comportement undefined |
| Java `>>` floor pour positifs | aligné avec ASR Motorola | Pas de différence pour les valeurs typiques |
| Point derrière caméra (`tz <= 0`) | clamp `screenX` à 0 ou `vidRightX` | Évite division par 0 |

### 04.5 Tests (11)

- `pointStraightAheadProjectsAtCenter_smallScreen` + `_fullScreen` : valeurs canoniques au centre.
- `pointBehindCameraClampsToEdge_left` + `_right` : clamping derrière caméra.
- `rotation90EastSwapsAxes` : test rotation 90° (sin=32767, cos=0).
- `pointToTheRightOfCenter` / `_leftOfCenter` : signe de la projection.
- `playerOffsetTranslatesView` : invariance par translation.
- `xwobbleAddedToRotatedX` : effet wobble.
- `fullScreenZHasAspectRatioApplied` : ratio 599/999 exact (3/5).
- `exactSequenceMatchesAsmReimplementation` : oracle de référence indépendant.

Tous passent ✅.

---

## Sous-système 05 (C) — PvsErrata

**Package** : `com.team17.ab3d2.pvs`.

### 05.1 Périmètre

Port direct de `c/zone_errata.c` : application des PVS errata, algorithme
en 3 passes.

### 05.2 Format du flux d'errata

```
[zoneID_1] [removeID_1a] [removeID_1b] ... [-1]
[zoneID_2] [removeID_2a] ... [-1]
...
[-1]                                            ; double -1 final
```

Le mot de tête (zoneID) est lu ; si valide, on lit ensuite les IDs à retirer
de sa PVS jusqu'à une sentinelle `-1`. La boucle externe s'arrête quand le
mot de tête est invalide (négatif), ce qui correspond au double `-1` final
qui termine le flux complet.

### 05.3 Algorithme en 3 passes

**Passe 1 — `initCurrentPVS`** (`c/zone_errata.c:41-75`) :
- Copie la PVS list de la zone vers un buffer de travail.
- Marque chaque entrée listée dans `removeList` avec
  `ZONE_ID_REMOVED_MANUAL (-2)`.
- Préserve les autres entries.

**Passe 2 — `buildVisitedPVS`** (`c/zone_errata.c:152-218`) :
- BFS depuis la zone source.
- À chaque étape, examine les edges joignants (via `ZoneIndexLists`).
- Une zone est ajoutée à `visited` si :
  1. Elle est un zoneID valide
  2. Pas déjà dans `visited`
  3. Présente dans `currentPvs` (pas marquée `-2`)
- Continue jusqu'à épuisement.

**Passe 3 — `rebuildCurrentPVS`** (`c/zone_errata.c:225-252`) :
- Pour chaque entrée de `currentPvs` :
  - Si valeur ≥ 0 et dans `visited` → conserver
  - Si valeur ≥ 0 mais pas dans `visited` → marquer `-3` (REMOVED_AUTO)
  - Si valeur déjà négative (-2) → laisser
- Collapse : nouvelle PVS = entries ≥ 0 dans l'ordre original.

### 05.4 Résultat fonctionnel vs mutation in-place

L'ASM/C modifie la PVS de la Zone in-place. Le port Java retourne un
`PvsErrata.Result` contenant une `Map<Integer, Zone>` de zones modifiées
(via `Zone.withPvs(newPvs)`). L'appelant fusionne avec la liste de zones
originale pour obtenir le `Level` post-errata.

Cette approche fonctionnelle préserve l'immutabilité des records Java sans
sacrifier la fidélité du résultat.

### 05.5 Tests (6)

- `emptyErratumDoesNothing` / `nullErratumDoesNothing` : pas-modifications.
- `manualRemovalDoesNotAutoRemoveConnected` : retirer B → C et D deviennent
  aussi inaccessibles (cascade via connectivité).
- `removingMiddleAlsoDisconnectsLater` : retirer C → D devient auto-removed,
  B reste.
- `multipleZonesInOneErratum` : application en série, plusieurs entrées.
- `initCurrentPVSMarksRemoved` : test unitaire de la passe 1.

Tous passent ✅.

### 05.6 Scénario test "chaîne" A-B-C-D

```
A (0) ──edge0/1── B (1) ──edge2/3── C (2) ──edge4/5── D (3)
```

PVS initiale : chaque zone voit toutes les autres. Edges en paires (par
exemple edge 0 dans A→B et edge 1 dans B→A car le format engine stocke
un edge par "côté de zone propriétaire").

Tests valident que :
- Retirer C de la PVS de A → A.PVS = [B] seulement (D auto-retiré car
  inatteignable sans C).
- Retirer B → A.PVS = [] (C et D auto-retirés).

---

## Risques de divergence (cumulés sur 03/04/05)

| Risque | Sévérité | Mitigation |
|---|---|---|
| Format des Object Points (Vec2W vs Vec2L) | Moyenne | Choix Vec2L (8 octets) ; à confirmer avec binaire réel |
| ControlPointCoords format (8 octets supposé Vec2W) | Moyenne | Tests synthétiques avec count=0 ; flag à exposer |
| `divs.w` overflow sur projection | Faible | Clamp défensif à short range |
| Union `EntT_DoorsAndLiftsHeld_l` / `Timer3_w` | Faible | Champs exposés séparément, doc explicite sur l'overlap |
| Union `ShotT_AccYPos_w` / `AuxOffsetX_w` | Faible | Idem, helper Java pour les deux interprétations |
| Liste shared-edges format (sentinelle -2 ?) | Moyenne | Documenté ; vérifier avec binaire réel |
| Edge `joinZoneId` interprétation cross-zone | Faible | Test fonctionnel passe ; à valider sur cas réel |
| Comportement Motorola `divs.w` sur 0 | Faible | Java throws ArithmeticException ; check `tz > 0` avant |

---

## Limitations laissées au futur

Tous des sous-systèmes 06+ :

1. **PVS edges per-edge** (`c/zone_edge_pvs.c`) : calcul de la visibilité
   par-edge avec masques de portes/ascenseurs. Consomme `Zone` + `Edge`
   + `DoorEntry`. **Critique pour le rendu**.

2. **Rendu walls / floors / objects** : consomme `Transform3D` + `Zone`
   + textures.

3. **Player physics + collision** : modifie `GameObject` (Plr1, Plr2)
   chaque frame. Utilise `Edge.sideOfPoint`.

4. **AI state machine** (`modules/ai.s`) : modifie les `EntityFields`
   chaque frame.

5. **Switches data stream** (`twolev.graph.bin` switchDataOffset) :
   format non documenté dans `defs.i` ou `zone_liftable.h`. À explorer.

6. **Zone graph adds** (`twolev.graph.bin` zoneGraphAddsOffset) : idem,
   format à déterminer par inspection du runtime.

---

## Récapitulatif cumulé du portage

| Sous-système | Tests | Statut |
|---|---|---|
| 01 — Math fondamental | 39 | ✅ |
| 02 — Loader binaires | 66 | ✅ |
| 03 — Gameplay state runtime | 24 | ✅ |
| 04 — Transform3D | 11 | ✅ |
| 05 — PvsErrata | 6 | ✅ |
| **Total** | **146** | ✅ |
