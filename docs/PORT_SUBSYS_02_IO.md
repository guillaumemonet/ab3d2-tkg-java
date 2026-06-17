# Sous-système 02 — Loader binaires

Deuxième sous-système porté. Lecture stricte des fichiers binaires Amiga
Team17 vers des objets Java immutables.

**Status** : ✅ porté, 66 tests de parité ajoutés (cumul total : 105/105).
**Packages** : `com.team17.ab3d2.io`, `com.team17.ab3d2.world`, `com.team17.ab3d2.gamedata`
**Tâches** : #17 → #25 (terminées).

---

## 1. Périmètre

Le sous-système 02 fournit :

| Couche | Composant | Origine ASM/C | Taille canonique |
|---|---|---|---|
| Infra | `BinaryReader` | — | API m68k-friendly |
| Infra | `TestBinaryWriter` (test) | — | construction de fixtures |
| Monde | `Vec2W`, `Vec2L` | `c/math25d.h:10-21` | 4 / 8 octets |
| Monde | `Edge` (ZEdge) | `defs.i:344-354` | 16 octets |
| Monde | `ZPVSRecord` (PVST) | `defs.i:356-361` | 8 octets |
| Monde | `Zone` (ZoneT) | `defs.i:316-340` | 50 octets + PVS variable |
| Monde | `LevelHeader` (TLBT) | `defs.i:538-558` | 54 octets |
| Monde | `LevelGraphicsHeader` (TLGT) | `defs.i:562-569` | 16 octets |
| Monde | `Level` (agrégateur) | runtime | — |
| Gameplay | `Bullet` (BulT) | `defs.i:140-158` | 300 octets |
| Gameplay | `ShootDef` (ShootT) | `defs.i:161-166` | 8 octets |
| Gameplay | `Alien` (AlienT) | `defs.i:169-191` | 42 octets |
| Gameplay | `ObjectDef` (ODefT) | `defs.i:194-209` | 40 octets |
| Gameplay | `Liftable` (ZLiftableT) | `defs.i:604-623` | 36 octets |
| Gameplay | `GameLinkFile` (GLFT) | `defs.i:384-418` | **86 268 octets** |
| Loader | `LevelLoader` | `hires.s:255-490` | parse twolev.bin + twolev.graph.bin |

---

## 2. Convention de lecture

### 2.1 Endianness

**Big-endian** systématique. C'est la convention m68k Amiga. Le `BinaryReader`
configure `ByteBuffer.order(ByteOrder.BIG_ENDIAN)` à la création.

### 2.2 Signed vs unsigned

Le moteur ASM distingue rigoureusement :
- Lecture signée : `move.w (a0),d0` avec sign-extension implicite.
- Lecture non-signée : utilisation directe du WORD comme entier 0..65535.

Côté Java, `ByteBuffer.getShort()` est signé. Pour récupérer la valeur
unsigned il faut `getShort() & 0xFFFF`. Le `BinaryReader` expose les deux
API distinctement (`readWord` / `readUWord`, `readLong` / `readULong`,
`readByte` / `readUByte`) pour éviter les bugs de sign-extension qui
seraient invisibles en C mais critiques en Java.

### 2.3 Listes terminées par sentinelle

Plusieurs structures utilisent une liste de mots terminée par une valeur
sentinel (e.g. `ZONE_ID_LIST_END = -1` pour les PVS). Helper :
`readWordsUntil(sentinel, maxWords)` qui borne défensivement la lecture.

### 2.4 Chaînes ASCII zéro-padded

Les noms (level, alien, gun, …) sont stockés à taille fixe avec zéro-padding.
`readFixedAsciiZ(length)` retourne la portion avant le premier zéro et
avance toujours de `length`.

---

## 3. Structures de monde

### 3.1 Layout `Zone` (`defs.i:316-340`)

```
Offset  Type   Champ                         Notes
   +0   WORD   z_ZoneID
   +2   LONG   z_Floor                       16.8 fixed-point (heightOf = >>8)
   +6   LONG   z_Roof
  +10   LONG   z_UpperFloor                  5000 = DISABLED_HEIGHT
  +14   LONG   z_UpperRoof
  +18   LONG   z_Water
  +22   WORD   z_Brightness
  +24   WORD   z_UpperBrightness
  +26   WORD   z_ControlPoint                UBYTE[2]
  +28   WORD   z_BackSFXMask                 originellement LONG, accédé WORD
  +30   WORD   z_Unused
  +32   WORD   z_EdgeListOffset              OFFSET NÉGATIF (vers liste d'edge-IDs précédant la struct)
  +34   WORD   z_Points                       OFFSET NÉGATIF (vers liste de point-IDs)
  +36   UBYTE  z_DrawBackdrop
  +37   UBYTE  z_Echo
  +38   WORD   z_TelZone                     téléporteur destination (-1 = none)
  +40   WORD   z_TelX
  +42   WORD   z_TelZ
  +44   WORD   z_FloorNoise
  +46   WORD   z_UpperFloorNoise
  +48   PVST[] z_PotVisibleZoneList           variable, sentinelle = -1
```

**Particularité importante** : les offsets `z_EdgeListOffset` et `z_Points`
sont **négatifs** ; le code ASM les ajoute à l'adresse de la zone pour
remonter en mémoire vers les listes qui **précèdent** physiquement la
struct (cf. `c/zone_inline.h:37-45` `Zone_GetEdgeList`).

**Port Java** : le record Java {@code Zone} expose ces offsets bruts ET
les listes résolues (`edgeIds`, `pointIds`) — la résolution des listes
n'est PAS faite par {@code LevelLoader} dans cette passe (laissée vide
{@code new short[0]}). À implémenter dans le sous-système 03 quand le
mapping mémoire complet sera porté.

### 3.2 Heights : 16.8 fixed-point

Toutes les hauteurs (floor, roof, upperFloor, upperRoof, water) sont
**LONG** mais le moteur n'utilise que les 24 bits hauts en pratique.
{@code c/zone_inline.h:116-119} :

```c
static inline WORD heightOf(LONG level) {
    return (WORD)(level >> 8);
}
```

Le port Java fournit {@code Zone.heightOf(int)} avec la même sémantique
(`>> 8`, arithmétique pour préserver le signe).

### 3.3 PVS list

La liste suit immédiatement le header dans le binaire. Termine à
{@code ZPVSRecord.zoneId() == ZONE_ID_LIST_END (-1)}. Borne défensive
{@code PVS_TRAVERSE_LIMIT = 100} (cf. `c/zone.h:39`) — si dépassée, le
loader lève {@code IllegalStateException}.

Valeurs sentinelles (cf. `c/zone.h:32-37`) :
- `-1` : `ZONE_ID_LIST_END` — fin de liste
- `-2` : `ZONE_ID_REMOVED_MANUAL` — retiré via errata
- `-3` : `ZONE_ID_REMOVED_AUTO` — retiré par traversal de connectivité
- `-4` : `EDGE_POINT_ID_LIST_END` — autre liste

### 3.4 Layout `LevelHeader` (TLBT) — `defs.i:538-558`

Le bloc {@code twolev.bin} a la structure :

```
Offset  Contenu
   0    Messages texte (10 × 160 octets = 1600 octets)
+1600   TLBT header (54 octets)
+1654   ControlPointCoords (NumControlPoints × Vec2W ?)
        Points (à TLBT.pointsOffset)
        ZoneBorderPoints (NumZones × 80 octets)
        ZoneT structs (référencées par twolev.graph.bin ZoneAdds)
        Edges (entre FloorLineOffset et ObjectDataOffset)
        ObjectData (à ObjectDataOffset)
        ShotData, AlienShotData, ObjectPoints, Plr1Object, Plr2Object
```

**Particularité** : `TLBT.numZonesMinusOne` est **1 de moins** que le
nombre réel de zones. {@code hires.s:353-356} :
```
move.w TLBT_NumZones_w(a1),d0  ; actually 1 less than the zone count, because reasons.
addq   #1,d0
move.w d0,Lvl_NumZones_w
```

Le port Java expose `numZones()` qui ajoute 1 automatiquement.

**Quirk** : `Lvl_ExitZoneID_w` = mot juste **avant** le début des edges
({@code hires.s:374} `move.w -2(a2),Lvl_ExitZoneID_w`). C'est typiquement
le dernier mot des ZoneBorderPoints — un overlap intentionnel, historique.

### 3.5 Layout `LevelGraphicsHeader` (TLGT) — `defs.i:562-569`

```
Offset  Contenu
   0    TLGT.doorDataOffset (LONG)
   4    TLGT.liftDataOffset (LONG)
   8    TLGT.switchDataOffset (LONG)
  12    TLGT.zoneGraphAddsOffset (LONG)
+16     ZoneAdds[] : pour chaque zone, un LONG = offset (depuis le début
        de twolev.bin) vers la ZoneT correspondante
```

**Patching** : {@code hires.s:428-432} :
```
move.l (a0),a3              ; Lvl_ZonePtrsPtr_l[i] = offset
add.l  a4,a3                ; + base
move.l a3,(a0)+             ; in-place patching → maintenant pointeur absolu
```

Le port Java ne patche pas in-place mais conserve les offsets bruts
({@code Level.zoneOffsets()}) et résout en index Java pour les structures
typées (la liste {@code Level.zones()}).

---

## 4. Game Link File (GLF)

Le {@code GLFT} est la base de données globale chargée une fois au boot.
Taille totale **86 268 octets**.

### 4.1 Erratum sur la doc d'architecture

Le document `docs/ARCHITECTURE.md` (§2.4.8) indique une taille de **40 288 octets**
pour `GLFT_SizeOf_l`. C'est **incorrect**. La vraie taille est 86 268 octets.

**Origine de l'erreur** : sous-estimation de `A_AnimLen`. La doc supposait
implicitement `A_AnimLen = 11 × 11 = 121` octets. La valeur correcte
({@code defs.i:374-376}) est :
```
A_FrameLen = 11
A_OptLen   = A_FrameLen * 20 = 220
A_AnimLen  = A_OptLen * 11   = 2420
```

Donc {@code AlienAnims = NUM_ALIEN_DEFS × A_AnimLen = 20 × 2420 = 48 400}
octets (et non 2420 comme dans la doc erronée).

Recalcul cumulé complet (vérifié par le test {@code offsetsCumulativeMatch}) :
```
+   64  LevelNames        16 × 40        =      640   →   704
+  704  ObjGfxNames       30 × 64        =    1 920   →  2 624
+ 2624  SFXFilenames      64 × 60        =    3 840   →  6 464
+ 6464  FloorFilename     64                       64   →  6 528
+ 6528  TextureFilename   192                     192   →  6 720
+ 6720  GunGFXFilename    64                       64   →  6 784
+ 6784  StoryFilename     64                       64   →  6 848
+ 6848  BulletDefs        20 × 300       =    6 000   → 12 848
+12848  BulletNames       20 × 20        =      400   → 13 248
+13248  GunNames          10 × 20        =      200   → 13 448
+13448  ShootDefs         10 × 8         =       80   → 13 528
+13528  AlienNames        20 × 20        =      400   → 13 928
+13928  AlienDefs         20 × 42        =      840   → 14 768
+14768  FrameData                              7 680   → 22 448
+22448  ObjectNames       30 × 20        =      600   → 23 048
+23048  ObjectDefs        30 × 40        =    1 200   → 24 248
+24248  ObjectDefAnims    30 × 120       =    3 600   → 27 848
+27848  ObjectActAnims    30 × 120       =    3 600   → 31 448
+31448  AmmoGive          30 × 44        =    1 320   → 32 768
+32768  GunGive           30 × 24        =      720   → 33 488
+33488  AlienAnims        20 × 2420      =   48 400   → 81 888  ← erreur précédente ici
+81888  VectorNames       30 × 64        =    1 920   → 83 808
+83808  WallGFXNames      16 × 64        =    1 024   → 84 832
+84832  WallHeights       16 × 2         =       32   → 84 864
+84864  AlienBrights      20 × 2         =       40   → 84 904
+84904  GunObjects        10 × 2         =       20   → 84 924
+84924  Player1Graphic    UWORD          =        2   → 84 926
+84926  Player2Graphic    UWORD          =        2   → 84 928
+84928  FloorData         16 × 4         =       64   → 84 992
+84992  AlienShootDefs    20 × 8         =      160   → 85 152
+85152  AmbientSFX        16 × 2         =       32   → 85 184
+85184  LevelMusic        16 × 64        =    1 024   → 86 208
+86208  EchoTable                                60   → 86 268
        GLFT_SizeOf_l                                = 86 268
```

Le doc d'architecture sera mis à jour à la prochaine itération.

### 4.2 Header de 64 octets

La directive {@code STRUCTURE GLFT,64} fait débuter le premier champ à
l'offset 64. Les 64 premiers octets du fichier GLF ne sont pas documentés
dans {@code defs.i} — probablement une signature et/ou version. Le port
les conserve dans {@code rawData} pour préservation byte-à-byte.

### 4.3 Stratégie de parsing Java

{@code GameLinkFile} parse **éagerly** les structures typées (Bullets,
Aliens, ObjectDefs, ShootDefs, noms…) et conserve **lazily** les blocs
dont le format n'est pas encore décodé : `FrameData` (7680 octets, todo
dans `defs.i:398`), `AlienAnims` (48 KB, format complexe), `ObjectDefAnims`,
`ObjectActAnims`, `AmmoGive`, `GunGive` — exposés comme `byte[]` via
accesseurs dédiés.

Le {@code rawData()} entier est conservé pour debug et round-trip
strict.

---

## 5. `LevelLoader` — état actuel

### 5.1 Ce qui est parsé

1. Bloc messages (10 × 160 octets, ASCII zéro-padded)
2. Header TLBT (54 octets, tous champs)
3. Header TLGT (16 octets, tous champs)
4. Array de control points (NumControlPoints × Vec2W)
5. Array de points (NumPoints × Vec2W)
6. Array d'edges (entre FloorLineOffset et ObjectDataOffset)
7. ExitZoneID (mot juste avant FloorLineOffset)
8. Table ZoneAdds (NumZones × LONG offsets vers les zones)
9. Pour chaque zone : header + PVS list

### 5.2 Ce qui n'est PAS encore parsé

Conservé en `rawTwolevBin`/`rawTwolevGraphBin`/`rawTwolevClips` pour passes
ultérieures :

| Bloc | Localisation | Sous-système prévu |
|---|---|---|
| Objects runtime (mutables) | `twolev.bin` ObjectDataOffset | 03 — Gameplay state |
| Player shots, Alien shots | `twolev.bin` *ShotDataOffset | 03 |
| Plr1Object, Plr2Object | `twolev.bin` PlrNObjectOffset | 03 |
| ObjectPoints | `twolev.bin` ObjectPointsOffset | 03 |
| Doors raw | `twolev.graph.bin` DoorDataOffset | 03 — utilise {@code Liftable} déjà porté |
| Lifts raw | `twolev.graph.bin` LiftDataOffset | 03 |
| Switches raw | `twolev.graph.bin` SwitchDataOffset | 03 |
| Zone graph adds | `twolev.graph.bin` ZoneGraphAddsOffset | 03 |
| Edge clips | `twolev.clips` complet | 04 — PVS preprocessing |
| Zone edge-ID lists | offsets négatifs dans `twolev.bin` | 03 |
| Zone point-ID lists | idem | 03 |

### 5.3 Limitations connues

- **Format des control points** : inféré comme {@code Vec2W} (4 octets).
  À confirmer par lecture du code de consommation. Si la réalité est
  {@code Vec3W} (6 octets) ou autre, l'offset cumulé sera erroné dès la
  prochaine entrée.
- **Liste d'edge-IDs / point-IDs par zone** : pas résolue. La doc PVS
  parle d'une liste terminée par `-1` suivie d'une autre terminée par `-2`,
  mais sans confirmation du format précis dans le code C. À explorer
  par lecture des sites de consommation ({@code Zone_GetEdgeList}, etc.)
  dans le sous-système 03.

---

## 6. Conformité aux contraintes du projet

| Règle | Statut |
|---|---|
| Préservation byte-à-byte | ✅ — chaque struct est byte-parfait |
| Big-endian respecté | ✅ — vérifié par tests dédiés |
| Signed/unsigned explicite | ✅ — API distincte dans `BinaryReader` |
| Pas de framework moderne | ✅ — JDK pur |
| Immutabilité | ✅ — toutes les structures sont records ou classes immutables avec copies défensives |
| Tests de parité | ✅ — 66 tests, byte-à-byte sur fixtures synthétiques |
| Pas d'allocation cachée | ⚠ — chaque accesseur fait `.clone()` ; à optimiser si profilage le justifie |

---

## 7. Risques de divergence

| Risque | Sévérité | Mitigation |
|---|---|---|
| Format précis des ControlPointCoords (Vec2W ? Vec3W ?) | Moyenne | Tests synthétiques avec NumControlPoints=0 actuellement ; vérifier avec un binaire réel |
| Endianness inattendue sur RTG/non-Amiga | Faible | Si un fichier non big-endian arrivait, la lecture serait silencieusement fausse. Pas de protection. Acceptable pour fichiers d'origine Amiga. |
| Offset cumulé GLF | Moyenne (corrigé) | Test {@code offsetsCumulativeMatch} vérifie chaque cumul |
| Sign-extension implicite Java | Faible | API explicite read* vs readU* + tests de parité |
| Padding de structures C | Aucune | Toutes les structures `defs.i` sont packed word-aligned ; les structures Java reproduisent les offsets explicites |
| Sentinelle PVS dépassant la limite | Faible | {@code PVS_TRAVERSE_LIMIT = 100} défensif, lève une exception |
| Layout endian de struct C avec champs de tailles mixtes | Très faible | Reproduit byte par byte via `BinaryReader` séquentiel ; pas d'assumption d'alignement |

---

## 8. Hypothèses non vérifiées

1. **ControlPointCoords sont Vec2W** (4 octets chacun) — inféré, à confirmer.
2. **Tous les champs `*Offset_l` du TLBT sont signed 32-bit positifs** (pas
   d'offsets négatifs). Cohérent avec l'usage (`add.l a4,a2`), mais le
   parser Java les lit en `int` signé — si une valeur > 2^31 apparaissait,
   elle serait interprétée comme négative.
3. **Le header de 64 octets du GLF n'est jamais lu par le moteur**. Pour
   l'instant on le préserve sans le décoder.
4. **`twolev.clips` est facultatif** ou peut être null — accepté par
   {@code LevelLoader.load(...)}.

---

## 9. Tests de parité — récapitulatif (66 nouveaux tests)

### `BinaryReaderTest` (16 tests)
- Lecture signed/unsigned 8/16/32 bits big-endian
- Position, seek, skip, remaining
- Peek non destructif (byte / word / long)
- `readBytes` avec copie défensive
- `readFixedAsciiZ` (cut at nul, no nul, empty)
- `readWordsUntil` (sentinelle, liste vide, sentinelle absente)
- Constructeur sliced avec offset/length

### `StructLayoutTest` (10 tests)
- Tailles Vec2W=4, Vec2L=8, Edge=16, ZPVSRecord=8
- Parsing big-endian de chaque champ
- {@code Edge.sideOfPoint} fidèle à {@code Zone_SideOfEdge}, no-overflow
- Constantes sentinelles PVS (-1, -2, -3, -4)

### `ZoneTest` (6 tests)
- Taille header = 50
- {@code heightOf} = `>>8` (avec valeurs positives et négatives)
- {@code hasUpper} (3 cas : disabled, equal, valid)
- Parsing complet header + PVS list
- {@code isTeleport} quand telZoneId >= 0

### `LevelHeaderTest` (5 tests)
- Taille = 54, message block = 1600
- Parsing des 19 champs
- `numZones()` = `numZonesMinusOne + 1`
- `numEdges()` = `(objDataOff - floorLineOff) / 16`

### `LevelGraphicsHeaderTest` (2 tests)
- Taille = 16, ZONE_ADDS_OFFSET = 16
- Parsing des 4 offsets

### `GameDataTest` (13 tests)
- `ShootDef` (taille + parsing)
- `Bullet` (taille, parsing, validation des animArrays)
- `Alien` (taille + parsing)
- `ObjectDef` (taille + parsing + padding)
- `Liftable` (taille + parsing + constantes DR_*/DL_*)

### `GameLinkFileTest` (5 tests)
- Cardinalités (NUM_LEVELS, NUM_BULLET_DEFS, …)
- Constantes d'animation (O_AnimSize, A_AnimLen, …)
- **Cumul exact des 32 offsets** (vérification structurelle byte-à-byte)
- Total = 86 268
- Parsing d'une fixture complète avec valeurs marker

### `LevelLoaderTest` (3 tests)
- Fixture complète twolev.bin + twolev.graph.bin (carré 4 points)
- `zoneById` hors plage → null
- Blobs raw préservés

---

## 10. Prochain sous-système

Deux options pour le sous-système 03 :

A) **Gameplay state mutable** : porter `ObjT`/`EntT`/`ShotT` (entities
   runtime mutables 64 octets), les listes par zone (edge-IDs, point-IDs
   via offsets négatifs), et les états de portes/ascenseurs/switches.
   Permet de finaliser le chargement d'un niveau jouable côté données.

B) **Sous-système rendu / transformation 3D** : porter `modules/transform.s`
   (rotation + projection des points). Très visuel, consomme directement
   `SinCosTable` du sous-système 01. Permet d'afficher des points
   projetés à l'écran sans encore avoir tout le niveau parsé.

C) **PVS preprocessing** (`zone_edge_pvs.c`, `zone_errata.c`) : porter
   la logique C de calcul du per-edge PVS et de l'application des
   errata. Consomme `Zone` + `Edge` déjà portés.

À décider à la prochaine étape.
