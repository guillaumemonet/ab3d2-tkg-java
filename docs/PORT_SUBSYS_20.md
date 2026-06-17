# Sous-système 20 — Spawn réel + sols/plafonds

## Contexte

Après le rendu fidèle des murs (sub-system 19), deux problèmes restaient :
le point de spawn ne ressemblait pas à l'original, et il manquait les sols et
plafonds.

| ID  | Livrable                                         | Statut |
|-----|--------------------------------------------------|--------|
| 20A | Spawn joueur réel (header TLBT)                  | done   |
| 20B | Parser des floor/ceiling records                 | done   |
| 20C | Rendu sols/plafonds (polygones remplis)          | done   |

**372 tests, 0 échec.**

## 20A — Spawn joueur réel

### Découverte

Le spawn ne venait PAS de `controlPoints[0]` (qui sont des points de patrol AI).
Le vrai départ est dans le header TLBT (`defs.i:538-541`, lu dans
`modules/player.s:28-42`) :

```
TBLT_Plr1_StartXPos_w   ; +0
TBLT_Plr1_StartZPos_w   ; +2
TBLT_Plr1_StartZoneID_w ; +4
```

Et l'altitude : `eyeY = ZoneT_Floor_l - PLR_STAND_HEIGHT`.

Ces champs étaient déjà parsés dans `LevelHeader` (`plr1StartX/Z/ZoneId`) mais
non utilisés. Pour LEVEL_A : **X=-808, Z=184, zone 3, eyeY=-48**. Pas d'angle
de départ dans TLBT → angle 0.

### Calibration des unités verticales

Toutes les hauteurs sont ramenées en **unités Y entières** (= full précision
`>> 8`) :
- mur : `topOfWall >> 8`, `bottomOfWall >> 8` (zone 3 mur : top=-128, bottom=0)
- sol/plafond : `floorY << 6` puis `>> 8` = `floorY >> 2`
  (sol `floorY=0` → 0, plafond `floorY=-512` → -128)
- œil joueur : `(zoneFloor >> 8) - (PLR_STAND_HEIGHT >> 8)` = `0 - 48 = -48`

Cohérent : œil à -48, entre sol (0) et plafond (-128).

## 20B — Parser floor/ceiling records

### Format (`hires.s:3598+`, `Draw_Flats`)

```
WORD floorY                  ; hauteur du plan
WORD numSides
numSides × WORD pointIndex   ; & 0xFFF (les 4 bits hauts sont des flags)
10 octets trailing           ; texture/brightness/origine tile
```

Tag distingue le type (`draw_zone_graph.s:345`) : 1=sol, 2=plafond, 7=eau.

### Classes

- **`GraphFloorRecord`** : floorY, tag, point indices masqués.
- **`ZoneGraphParser.floorsForZone(zone)`** + méthode `collect` unifiée
  (walls et/ou floors).

### Validation sur LEVEL_A

268 floor records (134 sols + 134 plafonds — un de chaque par zone),
**0 point index invalide**. Les point indices masqués `& 0xFFF` correspondent
aux points des murs de la même zone (cohérence géométrique confirmée).

## 20C — Rendu sols/plafonds

### `FirstPersonRenderer.renderFromGraph(..., FloorTileSet)`

- Sols/plafonds dessinés **en premier** (polygones remplis), puis murs
  par-dessus.
- **Couleur** : index palette représentatif (mode) d'une tuile du `floortile`
  (16 tuiles 64×64 d'indices 256pal). Sol = tuile 0, plafond = tuile 1.
- **Géométrie** : chaque sommet du polygone projeté via `Transform3D`,
  coordonnée Y écran via `projectVertical(eyeY - planeY, rotZ)`. Remplissage
  scanline (zones convexes).
- Clipping simple : si un sommet est derrière la caméra, le polygone est
  ignoré (pas de near-plane clipping pour l'instant).

### Résultat

`build/level_a_floors.png` (320×240, depuis le vrai spawn) : 75% de couverture,
111 couleurs, sol brun (100,80,60), murs texturés par-dessus. La scène est
maintenant lisible : sol en bas, plafond en haut, murs texturés.

## Correctif projection fidèle (post-feedback « murs en bazar »)

Le rendu était incohérent car j'avais **approximé** la projection au lieu de la
porter. Reprise ligne par ligne de `hireswall.s:1095-1114` :

```
divs.l d0,d1            ; screenX      = x' / z'      + CentreX
divs   d0,d5 (TopOfWall); screenYtop   = TopOfWall/z' + CentreY
divs   d0,d5 (BotOfWall); screenYbot   = BotOfWall/z' + CentreY
```

Trois erreurs corrigées :

1. **Focal length inventé** : j'utilisais `projectVertical = centreY - viewY*256/rotZ`.
   Le moteur n'a **pas** de focale séparée — elle est encodée dans les shifts du
   transform (`x' >> 8`, `z' >> 15`, cf. `transform.s:59,73-74`). La formule
   exacte est `screenY = (worldY - playerY) / z' + CentreY`, avec le **même z'**
   que pour screenX.
2. **Mauvaise échelle Y** : j'utilisais `topOfWall >> 8`. Le moteur divise la
   valeur en **pleine précision monde** (`topOfWall - Plr_YOff`) par z'. Le
   `playerY` est donc maintenant en pleine précision
   (`zoneFloor - PLR_STAND_HEIGHT`, et non `>>8`).
3. **Interpolation** : le moteur interpole les bords top/bottom **linéairement
   en espace écran** entre les deux extrémités (`screendivide`), pas par
   re-projection par colonne. Le U de texture reste perspective-correct.

**Near-plane clipping** ajouté (port du principe de `cliptotestfirstbehind`) :
les murs et polygones sol/plafond traversant le plan caméra étaient
entièrement rejetés (sommets derrière la caméra), laissant le bas de l'écran
vide. Désormais on clippe à `z' = NEAR` (segment pour les murs,
Sutherland-Hodgman pour les polygones), donc l'environnement immédiat du joueur
s'affiche jusqu'aux bords. Résultat : la bande basse de l'écran passe de 20% à
93% remplie (sol proche visible).

## Limites connues / prochains chantiers

1. **Texture-mapping perspective des sols** : actuellement flat-shaded (couleur
   dominante d'une tuile). Le vrai moteur mappe la tuile 64×64 en perspective
   par scanline (`Draw_Flats`, table `OneOverN`). Gros morceau.
2. **Sélection de tuile par record** : les 10 octets trailing du floor record
   contiennent l'index de tuile + origine + brightness — à décoder pour
   utiliser la bonne tuile par sol (actuellement tuile 0/1 fixes).
3. **Near-plane clipping** : les polygones avec un sommet derrière la caméra
   sont entièrement ignorés (devrait clipper au plan near).
4. **PVS** : on rend toutes les zones ; le moteur ne rend que la zone courante
   + son PVS via le zone graph walker.
