# Sous-système 18 — Moteur jouable : sliding, AI active, textures, game loop

## Vue d'ensemble

Batch qui transforme les briques isolées en moteur fonctionnel : le joueur
glisse le long des murs, les aliens chargent réellement le joueur, les murs
sont texturés, et une boucle de jeu unifiée orchestre tout de manière
déterministe.

| ID  | Livrable                                                     | Statut  |
|-----|--------------------------------------------------------------|---------|
| 18A | Sliding-along-wall + zone tracking                           | done    |
| 18B | AI charge/approach branché aux coordonnées joueur            | done    |
| 18C | Liaison zone-edge → index texture mur                        | done    |
| 18D | First-person render texturé                                  | done    |
| 18E | `GameLoop` intégré                                           | done    |

**Total tests : 365, 0 échec.**

## 18A — Sliding-along-wall + zone tracking

### Référence ASM

`objectmove.s:276-289` (`.calcalong`).

### Sliding

Quand un mouvement franchit un solid wall, au lieu de rejeter le déplacement,
on projette la position cible sur la ligne du mur (retire la composante
perpendiculaire) :

```
d0 = (newx-xPos)*zLen - (newz-zPos)*xLen   (= -sideOfPoint)
d7 = d0 / wallLength
slidX = newx - (d7 * zLen) / wallLength
slidZ = newz + (d7 * xLen) / wallLength
```

Implémenté dans `CollisionTester.slideAlongWall(edge, newx, newz)`. La position
glissée est re-testée ; si elle franchit encore un mur (coin), on rejette.

### Zone tracking

`CollisionTester.findCrossedEdge` retourne l'edge franchi. Sur un
`ENTERS_ZONE`, `PlayerController` met à jour `state.zoneId` avec le
`joinZoneId` de l'edge traversé.

## 18B — AI charge/approach branché au joueur

### Référence ASM

`ai_Charge` (`ai.s:700-789`), `ai_Approach` (`ai.s:1338-1437`).

### Implémentation

- `AiCore.setTarget(playerX, playerZ)` — équivalent de la lecture de
  `Plr1_XOff_l`/`Plr1_ZOff_l`.
- `moveTowardsTarget(e, speed, range)` — utilise `HeadTowardsAng` pour avancer
  l'entité vers la cible et fixer son `currentAngle`.
- `aiCharge` : speed = `responseSpeed`, range = `chargeRange` (160 par défaut,
  fidèle au `move.w #160,Range`).
- `aiApproach` : speed = `followupSpeed`.
- `aiAttackWithGun` : speed 0 (oriente sans avancer).

`AiConfig` étendu avec `responseSpeed`, `followupSpeed`, `chargeRange`.

## 18C — Liaison zone-edge → index texture mur

### Découverte ASM

`hireswall.s:1944-2003` (`Draw_Wall`) + `modules/res.s:352-379`.

Le record "wall" du graphics blob (`twolev.graph.bin`) référence sa texture par
un **index 0..15** (WORD à l'offset +12 du record). Cet index sélectionne un
slot dans `Draw_WallTexturePtrs_vl[16]`. Les noms par défaut viennent du GLF
database (`GLFT_WallGFXNames_l`, 16×64), avec override per-level via
`level_<hex>.256wad`.

### Classes

- **`WallTextureTable`** : 16 slots de `WallTextureDecoder`, chargeable depuis
  une liste de noms (`fromNames`, e.g. `glf.wallGfxNames()`) ou un répertoire
  (`fromDirectory`). Constante `CHUNK_OFFSET = 2048` (palette→chunk).
- **`GraphWallRecord`** : parser du record wall 28 octets (point indices,
  fromTile, **textureIndex**, height/width masks, top/bottom Y, otherZone).

## 18D — First-person render texturé

### Implémentation

`FirstPersonRenderer.renderTextured(...)` ajoute le texture-mapping :

- Pour chaque solid wall en vue, projette les deux endpoints.
- Interpolation affine de `1/rotZ` entre endpoints (depth).
- Pour chaque colonne écran : coordonnée U texture, pour chaque ligne :
  coordonnée V, échantillonnage via `WallTextureDecoder.readTexel(u, v, h)`.
- Texel 5-bit mappé sur la plage palette `[128..159]`.

Le mapping edge→slot texture est déterministe (`wallSeq % nbSlots`) faute
d'avoir câblé le graph blob complet — la vraie correspondance utiliserait
`GraphWallRecord.textureIndex`. C'est la prochaine étape.

### Validation

`build/level_a_textured.png` (320×240, ~30 kB — surfaces remplies vs ~7.6 kB
wireframe).

## 18E — GameLoop intégré

### Référence ASM

`controlloop.s` / `hires.s::MainLoop`.

### Implémentation

`com.team17.ab3d2.game.GameLoop` orchestre par tick (50 Hz) :

1. lecture input (`Supplier<PlayerInput>`)
2. `PlayerController.tick` (mouvement + collision + sliding)
3. tick de chaque `DoorRuntime` (+ `use` déclenche l'ouverture des portes de
   la zone courante)
4. `AiCore.setTarget(player)` puis tick de chaque `Entity` vivante
5. `FirstPersonRenderer.render` → `Display.present`

### Déterminisme

La boucle est dirigée par les ticks, pas l'horloge système. Le `Display` est
injecté (`InMemoryDisplay` en test). Test `identicalRunsProduceIdenticalFrames`
vérifie que deux runs avec la même séquence d'input produisent des frames
**byte-identiques** — critère essentiel du port de préservation.

### Validation

- `runsDeterministicallyForFixedTicks` : 10 ticks → 10 frames capturées
- `identicalRunsProduceIdenticalFrames` : reproductibilité bit-à-bit
- `aiEntityChargesPlayerOverTicks` : un alien à 1000 unités se rapproche du
  joueur sur 20 ticks

## Bilan

- **Classes ajoutées** : `WallTextureTable`, `GraphWallRecord`, `GameLoop`
  + extensions (`CollisionTester.slideAlongWall`/`findCrossedEdge`,
  `PlayerController` sliding/zone-tracking, `AiCore` charge/approach,
  `FirstPersonRenderer.renderTextured`)
- **Tests ajoutés** : 5 fichiers (Sliding, AiCharge, WallTextureTable,
  TexturedFirstPerson, GameLoop)
- **PNG produits** : `level_a_textured.png` (FP texturé)
- **365 tests, 0 échec**

## Correctif couleurs (post-18D)

Le premier PNG texturé sortait en **niveaux de gris**. Deux causes, corrigées :

1. **`AssetLoaders.loadPaletteRgb`** lisait l'octet HAUT de chaque WORD du
   `256pal` (toujours 0) — la valeur 8-bit est dans l'octet **BAS** (offsets
   +1/+3/+5). Vérifié : rampe de gris `00 08 / 00 10 / …` → 8, 16, 24…
2. **Chaîne de couleur du `.256wad`** : le rendu mappait le texel directement
   sur une palette debug grise. La vraie chaîne (cf. `hireswall.s:1656`,
   `move.w (a2,d1.w*2),(a3)`) :
   - section palette = lignes de brightness de 64 octets (32 couleurs × WORD)
   - l'**octet haut** du WORD = index dans la palette globale 256 couleurs
   - texel 0 = transparent (« holey »)

   Porté dans `WallTextureDecoder.globalPaletteIndex(texel, brightnessRow)`.
   `FirstPersonRenderer.renderTextured` écrit désormais l'index global réel +
   assombrissement par distance (`distanceToBrightnessRow`). Le PNG contient
   118 couleurs distinctes (brun, vert, gris) — vrai rendu coloré.

## Correctif géométrie/textures (post-couleurs)

Après le fix couleurs, les textures **ne correspondaient pas** au niveau (mapping
placeholder `wallSeq % nbSlots` + géométrie depuis les edges bruts). Corrigé en
parsant le graphics blob compilé :

- **`ZoneGraphParser`** walke `twolev.graph.bin` zone par zone. Format du flux
  (cf. `draw_zone_graph.s:306-440`) : `WORD zoneId` puis commandes taguées :
  - tag 0 → wall (28 octets, `GraphWallRecord`)
  - tags 1,2,7 → floor/water (`14 + 2*(sides-1)` octets)
  - tag 4 → object (2 octets), tag 12 → backdrop (0), tags 3/5/6/8-13 → no-op
  - octet bas signé < 0 → fin de flux
- **Validation forte** sur LEVEL_A : 134 zones, 449 walls, **0 textureIndex
  invalide, 0 point index invalide**, tous les flux terminent proprement —
  preuve que les tailles de record sont exactes (sinon le walk désynchroniserait).
- **`WallTextureTable.fromNames`** gère les chemins AmigaOS du GLF
  (`TKG2:WALLINC/STONEWALL.256WAD` → `STONEWALL`), résolution insensible à la
  casse. Le GLF (`INCLUDES/test.lnk`, 86268 o) donne le slot-mapping exact :
  slot 0 = STONEWALL, slot 8 = ALIENREDWALL, etc.
- **`FirstPersonRenderer.renderFromGraph`** : pour chaque wall record, géométrie
  depuis `level.points()[leftPointIndex/rightPointIndex]` (indices globaux) +
  `textures.get(record.textureIndex())`. PNG `level_a_faithful.png`, 101
  couleurs, 56% de couverture — textures correctes et niveau reconnaissable.

## Prochains chantiers naturels

1. ~~Câbler `GraphWallRecord` au rendu~~ → **fait** (`renderFromGraph`).
2. **Floors/ceilings texturés** dans le first-person render (tags 1,2 du
   draw loop).
3. **Shading par distance/brightness** via la shade table de la palette.
4. **Sprites/objets** (tag 4) : décoder et afficher les aliens/items.
5. **LWJGL réel** : brancher `GameLoop` sur `LwjglDisplay` + input clavier pour
   une démo jouable.
