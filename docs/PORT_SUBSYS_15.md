# Sous-système 15 — Validation niveau réel / décodage assets / PNG output / LWJGL stubs

Sprint combiné A B C D E. Le travail le plus important : la **validation
du LevelLoader contre un vrai niveau Team17 (LEVEL_A)** qui a forcé un
refactor critique basé sur la relecture précise de l'ASM.

**Status** : ✅ porté, **16 nouveaux tests** (cumul total : **280/280**).
**Tâches** : #58 → #62.

---

## A — Décodage .256wad (partiel)

**Découvertes** d'après `hireswall.s:1968-1972` :

```asm
move.w  (a0)+,d1                          ; texture index from level data
move.l  #Draw_WallTexturePtrs_vl,a3
move.l  (a3,d1.w*4),a3
move.l  a3,Draw_PalettePtr_l               ; ← début du fichier = palette
add.l   #64*32,a3                          ; + 2048 octets
move.l  a3,Draw_ChunkPtr_l                 ; ← bytes 2048+ = chunk data
```

→ **structure** :
- bytes 0..2047 : palette/shade table (64 × 32 = 2048 octets, format à confirmer)
- bytes 2048..: chunk data (texels packed)

**Header observé** (premier 16 octets de STONEWALL.256wad lus en WORDs BE) :
`0000 0100 0200 0300 0400 0500 2000 4600` — chaque WORD = `(byte_value << 8)`.
C'est un array de bytes avec padding 0x00, lu comme WORDs où la valeur est
dans le high byte.

**Loader livré** (`AssetLoaders.loadWallTexture`) : retourne le binaire raw.
Le format complet (décodage du chunk data → texture 2D) nécessite plus de
reverse engineering de `Draw_Wall` et reste pour un sous-système futur.

---

## B — Render PNG avec vrais assets

`FramebufferPng` (Java natif, pas de LWJGL) :
- `toBufferedImage(fb, palette)` : convertit framebuffer chunky 8-bit + palette → BufferedImage RGB
- `writePng(fb, palette, path)` : écrit le PNG via `ImageIO`

Tests : 2 tests valident la conversion + le round-trip écriture/lecture.

---

## C+D — LWJGL stubs (audio + display)

**Approche** : fournir des **interfaces** + **implémentations in-memory** qui
fonctionnent sans natives. L'intégration LWJGL réelle est une couche
ultérieure facile à ajouter sans changer le code utilisant ces interfaces.

### `com.team17.ab3d2.platform`

- `Display` (interface) : `open()`, `present(fb, palette)`, `shouldClose()`, `close()`
- `InMemoryDisplay` : capture les frames en mémoire pour tests/offline
- `AudioOutput` (interface) : `init()`, `pushVoiceState(channel, period, volume, sampleData, samplePos)`, `advance(numSamples)`, `close()`
- `InMemoryAudioOutput` : capture l'état des 4 voices + samples advanced count

**Pourquoi stubs et pas LWJGL direct** :
- LWJGL ajoute des natives par OS qui changent le profil de build
- Tests CI sans GPU/sound risqueraient de bloquer
- Couche d'abstraction permet de tester la LOGIQUE de présentation/audio
  sans dépendre du matériel

L'intégration LWJGL réelle (`LwjglDisplay` + `LwjglAudioOutput`) est un
projet downstream : ajouter `org.lwjgl:lwjgl-glfw`, `org.lwjgl:lwjgl-opengl`,
`org.lwjgl:lwjgl-openal` aux deps Gradle + natives par OS + impl des
interfaces.

Tests : 5 tests valident le cycle de vie + capture des states.

---

## E — Validation LevelLoader sur LEVEL_A réel

**LE moment critique** du sprint. Tentative initiale de charger
`medias/original/levels/LEVEL_A/twolev.bin` → **crash** :
```
java.lang.IllegalArgumentException: Illegal Capacity: -1405
```

### Diagnostic

`numEdges = (objectDataOffset - floorLineOffset) / 16` retourne **-1405**.

Sur LEVEL_A :
- `floorLineOffset = 25494`
- `objectDataOffset = 3014` ← inférieur à floorLineOffset !

Mon hypothèse précédente (objects après edges en mémoire) était **fausse**.

### Relecture précise de l'ASM (rappel utilisateur : "base toi sur l'ASM, n'assume pas")

`hires.s:363-372` :
```asm
; todo - Determine the number of edges. This is probably
; (TLBT_ObjectDataOffset_l - TLBT_FloorLineOffset_l) / EdgeT_SizeOf_l
move.l TLBT_FloorLineOffset_l(a1),a2
move.l TLBT_ObjectDataOffset_l(a1),d0
sub.l  a2,d0
move.l d0,Lvl_EdgeCount_l       ; ← stocke BYTES count, peut être négatif !
```

**Trois faits critiques** que le commentaire ASM lui-même indique :

1. **"This is probably"** — le développeur de karlos-tkg n'était pas sûr de
   la formule. Ce n'est PAS une certitude.
2. **`Lvl_EdgeCount_l`** stocke un **byte count**, pas un nombre d'edges
   (pas divisé par 16).
3. La valeur peut être **négative** dans la pratique sur de vrais niveaux.

**Conclusion** : le moteur original n'utilise PAS `Lvl_EdgeCount_l` pour
itérer une liste d'edges. Les edges sont accédés **par-zone** via
`Zone_GetEdgeList(zone)` qui utilise `z_EdgeListOffset` négatif. Il n'y a
PAS de liste globale d'edges itérée.

### Refactor du LevelLoader

Au lieu d'utiliser `numEdges()` (peu fiable), on calcule la borne d'edges
depuis le **minimum des `zoneOffsets`** :

```java
// Les edges occupent l'espace entre FloorLineOffset et le PREMIER ZoneT
// en mémoire (= min(zoneOffsets) qui sont après floorLineOffset).
int minZoneOffset = Integer.MAX_VALUE;
for (int off : zoneOffsets) {
    if (off > header.floorLineOffset() && off < minZoneOffset) {
        minZoneOffset = off;
    }
}
int maxEdges = (minZoneOffset - header.floorLineOffset()) / Edge.SIZE_BYTES;
```

Cette approche est **structurellement correcte** : on lit jusqu'à où les
zones commencent, pas selon une formule arbitraire du header.

### Découvertes par-zone sur LEVEL_A

Après le refactor, LEVEL_A charge complètement :
```
zones=134 points=348 edges=3512
doors=11 lifts=1
joining_edges=274 solid_walls=226 (sur 500 premiers)
exitZoneId=131
```

Confirmations :
- 134 zones (= `TLBT_NumZones_w + 1` = 133+1 ✓)
- 348 points
- 11 portes, 1 ascenseur
- exitZoneId = 131

### Edges : valeurs négatives autres que -1

L'inspection des edges réels révèle plusieurs valeurs spéciales :
- `joinZoneId = -1` : solid wall classique
- `joinZoneId = -7`, `-3`, etc. : **autres sentinelles** dont la sémantique
  n'est pas documentée dans defs.i

Les tests les ignorent et ne comptent que les edges plausibles
(`joinZoneId == -1` pour solid wall, `[0, numZones[` pour adjacent).

---

## Tests réels (9 sur LEVEL_A)

`RealLevelValidationTest` (tous {@code @EnabledIf("realLevelAvailable")}):

- `realLevelAFileSizes` : twolev.bin = 81 690, twolev.graph.bin = 23 914
- `parsesRealLevelWithoutError` : `LevelLoader.load()` ne crash pas ✓
- `realLevelHasMessages` : 10 messages texte, premier = "This key will open..."
- `realLevelHasValidHeader` : 134 zones, 348 points, edges > 0
- `realLevelZonesParseCorrectly` : 134 zones non-vides, <= 256
- `realLevelEdgesAreValid` : ≥ 274 joining edges + 226 solid walls dans les 500 premiers
- `realLevelPointsAreInRange` : points distincts (pas tous identiques)
- `realLevelHasDoorOrLiftEntries` : 11 doors, 1 lift parsés
- `realLevelExitZoneIdIsValid` : exitZoneId lu (= 131 pour LEVEL_A)

---

## Recap structurel : ce qu'on a appris sur l'ASM

1. **`TLBT_NumZones_w` est `réel - 1`** (`hires.s:353`)
2. **`Lvl_EdgeCount_l` est unreliable** (commentaire "todo - probably")
3. **Edges sont accédés par-zone** (jamais globalement)
4. **GLF header = 64 bytes de zéros** pure padding
5. **Strings GLF = space-paddées** (0x20), pas zero-terminées
6. **`.256wad`** : 2048 octets header palette, puis chunk data
7. **Edges peuvent avoir `joinZoneId` négatif autre que -1** (sentinelles
   non documentées)

Toutes ces découvertes sont issues de la **relecture systématique de l'ASM**
et de la **validation contre vrais fichiers** — pas d'hypothèses.

---

## Récap cumulé

| Sous-système | Tests | Statut |
|---|---|---|
| 01 — Math fondamental | 39 | ✅ |
| 02 — Loader binaires | 66 | ✅ |
| 03 — Gameplay state runtime | 24 | ✅ |
| 04 — Transform3D | 11 | ✅ |
| 05 — PvsErrata | 6 | ✅ |
| 06 — Edge PVS | 17 | ✅ |
| 07 — Renderer fondations | 38 | ✅ |
| 08 — Setup code rasterizers | 19 | ✅ |
| 09 — Zone graph walker | 4 | ✅ |
| 10 — Integration end-to-end | 2 | ✅ |
| 11 — Gouraud + dithering | 3 | ✅ |
| 12 — Physics + collision | 10 | ✅ |
| 13 — Audio Protracker | 9 | ✅ |
| 14 — Real assets + GLF validation | 16 | ✅ |
| **15 — Real level validation + PNG + LWJGL stubs** | **16** | **✅** |
| **Total** | **280** | ✅ |

---

## Note importante (rappel utilisateur)

Lors de ce sprint l'utilisateur a souligné : *"attention n'assume pas, il
faut que tu te base sur l'asm"*. Cette directive a directement résolu le
problème de chargement de LEVEL_A. Pratiquement :

- **Avant** : j'avais supposé que `(objectDataOffset - floorLineOffset)` était
  un count valide d'edges (par lecture rapide du code C/ASM).
- **Après relecture précise** : le commentaire ASM dit "todo - probably",
  la valeur stockée est en bytes (pas divisée par 16), et peut être
  négative. Le moteur n'utilise pas cette valeur.

Cette discipline (revenir au code source pour chaque détail) sera maintenue
pour la suite. Tout nouveau découverte sur l'engine doit être citée avec
ligne ASM précise.

---

## Pour la suite

Avec un vrai niveau qui charge, et des stubs LWJGL en place, plusieurs
options :

A) **Intégration LWJGL réelle** : ajouter deps Gradle + implémenter
   `LwjglDisplay` et `LwjglAudioOutput` → exécutable visuel/audio.

B) **Décodage .256wad complet** : reverse engineer le chunk data format
   en croisant hireswall.s et les bytes des fichiers walls.

C) **Charger LEVEL_A puis rendre en PNG** : test d'intégration totale
   (vrais assets + rendu PNG → inspection visuelle).

D) **Continuer la profondeur algorithmique** : effets MOD (arpeggio,
   slides, vibrato), Gouraud 4-corners, dithering complet, etc.

E) **AI + animation + gameplay** : porter modules/ai.s, newanims.s,
   newplayershoot.s.
