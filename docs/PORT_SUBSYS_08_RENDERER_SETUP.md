# Sous-système 08 — Setup code des rasterizers

Pont algorithmique entre `Transform3D` (sous-système 04) et les rasterizers
(sous-système 07). Trois classes de setup : `WallSetup`, `FloorSetup`,
`ObjectSetup`.

**Status** : ✅ porté (squelette algorithmique), 19 tests
(cumul total : **220/220**).
**Package** : `com.team17.ab3d2.render`
**Tâches** : #45 → #48

---

## 1. Scope

Le setup code original (dispersé sur ~2000 lignes dans `hireswall.s`,
`hires.s`, `objdrawhires.s`) calcule, pour chaque mur/sol/objet :
- La position écran (projection perspective depuis position monde rotée)
- La taille écran (scaling par 1/Z)
- Les paramètres d'interpolation par colonne (mur) / par scanline (sol) /
  per-sprite (objet)

Ce sprint porte le **squelette algorithmique** en Java propre, sans les
optimisations CPU-spécifiques ni les variantes (Gouraud, water FX,
téléport, sub-pixel, animation par direction).

### 1.1 Livré

| Composant | Origine ASM | Rôle |
|---|---|---|
| `WallSetup.Wall` (record) | concept de la struct `WD` ({@code defs.i:643-680}) | Description d'un mur prêt à dessiner |
| `WallSetup.projectScreenY` | inline dans {@code hireswall.s} | Projection Y écran d'un point monde |
| `WallSetup.drawWall` | port simplifié de {@code Draw_Wall} ({@code hireswall.s:1944+}) | Itère les colonnes, interpole, appelle `WallRasterizer` |
| `FloorSetup.Floor` (record) | concept du contexte de rendu sol | Paramètres pour rendre un sol/plafond |
| `FloorSetup.drawFloor` | port de la section sol de {@code hires.s:3700+} | Itère scanlines, calcule worldXZ, appelle `FloorRasterizer` |
| `ObjectSetup.WorldObject` (record) | concept de l'entité runtime à projeter | Description d'un objet monde à projeter |
| `ObjectSetup.project` | port simplifié de {@code objdrawhires.s} projection | Projette en `ObjectRasterizer.Sprite` (ou null si culled) |
| `ObjectSetup.projectAndSort` | équivalent du depth-table fill | Projette une liste + tri painter back-to-front |

### 1.2 Simplifications

**WallSetup** :
- ❌ Pas de tests de visibilité avancés (`wallfacingaway`, clipping per-zone)
- ❌ Pas de variantes "good render" vs "fast render" basées sur la distance
- ❌ Pas de gestion des endpoints derrière la caméra avec interpolation
  (`cliptotestfirstbehind` / `cliptotestsecbehind`)
- ❌ Pas de Gouraud sur les côtés verticaux (top/bot ont la même brightness)
- ✅ Projection Y perspective fidèle (`screenY = centreY + relY * focal / rotZ`)
- ✅ Interpolation linéaire X par colonne (yTop, yBot, U, brightness)
- ✅ Affine V step (`(textureHeight << 16) / columnHeight`)

**FloorSetup** :
- ❌ Pas de table `OneOverN_vw` (division entière directe)
- ❌ Pas de Gouraud per-pixel (un brightness par scanline)
- ❌ Pas de water animation
- ❌ Pas de clipping per-edge
- ✅ Formule perspective inverse (`distance = playerHeight × focal / |relY|`)
- ✅ World X/Z par pixel via perpRight direction

**ObjectSetup** :
- ❌ Pas d'animation 8 directions
- ❌ Pas de scaling via `ConstantTable` précalc
- ❌ Pas de "glare" sub-palette
- ❌ Distinction `DRAW_VECTOR_NEAR_PLANE` (130) / `DRAW_BITMAP_NEAR_PLANE` (25)
  réduite : seul le bitmap near plane utilisé
- ✅ Projection écran XY (`centreXY + rot * focal / rotZ`)
- ✅ Scaling par 1/rotZ
- ✅ Near plane culling à `DRAW_BITMAP_NEAR_PLANE = 25`
- ✅ Tri painter back-to-front via `projectAndSort`

---

## 2. Formules clés

### 2.1 Projection écran (commune à walls + objects)

```
screenX = vidCentreX + (rotX * focalLength) / rotZ
screenY = vidCentreY + (worldYRelative * focalLength) / rotZ
```

Où `worldYRelative = worldY - playerY` (Y inversé : worldY < playerY → au-dessus → screenY < centreY).

### 2.2 Scaling sprite

```
screenSize = (spriteWorldSize * focalLength) / rotZ
```

Plus rotZ est grand (objet éloigné), plus screenSize est petit.

### 2.3 Distance scanline sol/plafond

```
relY = screenY - centreY                        // positif pour sol, négatif pour plafond
distance = playerHeight × focalLength / |relY|  // distance vers la scanline
```

### 2.4 World step par pixel (sol/plafond)

Convention engine (vérifiée dans `modules/transform.s` et
`c/zone_edge_pvs.c:697-708`) :
- **forward** = `(sin(angle), cos(angle))` — direction du regard
- **perpRight** = `(cos(angle), -sin(angle))` — vers la droite du joueur

```
worldX_centre  = playerX + sin × distance / Q15      // forward
worldZ_centre  = playerZ + cos × distance / Q15
dWorldX_per_px = (cos × distance / viewport_width) / Q15   // perpRight
dWorldZ_per_px = (-sin × distance / viewport_width) / Q15
```

Au début, j'avais inversé `cos` et `sin` (forward=(cos, sin) au lieu de
(sin, cos)) — corrigé après échec du test `differentScanlinesProduceDifferentPatterns`.

### 2.5 Affine V step (walls)

Pour une colonne mur visible de `yTop` à `yBot` :
```
columnHeight = yBot - yTop + 1
vStep_16_16 = (textureHeight << 16) / columnHeight
vStart_16_16 = 0   // début de texture en haut du mur
```

L'absence de correction perspective est **intentionnelle** — c'est le
comportement du moteur original (artefacts "wobble" caractéristiques sur
les murs longs ou obliques).

---

## 3. Tests (19)

### 3.1 `WallSetupTest` (6)
- `projectScreenYAtHorizonIsCentre` : worldYRel=0 → screenY=centreY
- `projectScreenYAboveEyeIsAboveHorizon` : worldYRel<0 → screenY<centreY
- `projectScreenYBelowEyeIsBelowHorizon` : worldYRel>0 → screenY>centreY
- `projectScreenYZeroZHandledSafely` : rotZ=0 ne crash pas
- `drawWallSimpleScenario` : mur uniforme rendu sur 10 colonnes
- `drawWallClipsToFramebuffer` : mur partiellement hors → clipping correct
- `drawWallPerspectiveShortensWhenFar` : ratio rotZ small/big → height ratio inversé

### 3.2 `FloorSetupTest` (4)
- `floorDrawsBelowHorizon` : pixels y > centreY uniformes (texture solide)
- `ceilingDrawsAboveHorizon` : pixels y < centreY pour isCeiling=true
- `floorRejectsBadParams` : validation playerHeight + texture size
- `differentScanlinesProduceDifferentPatterns` : perspective active

### 3.3 `ObjectSetupTest` (6)
- `nearPlaneCullsObjectsTooClose` : rotZ < 25 → null
- `objectAtCentreProjectsToCentre` : rotX=0 → screenX=centreX
- `objectOffCentreProjectsOffCentre` : rotX>0 → screenX>centreX
- `scalingShrinkWhenFarther` : rotZ ratio 5× → screenSize ratio 5× inverse
- `projectAndSortReturnsBackToFront` : tri painter respecté
- `projectAndSortSkipsCulledObjects` : near-plane filtré
- `zeroScreenSizeReturnsNull` : objets minuscules culled

---

## 4. Découverte importante

Lors de l'écriture du test `differentScanlinesProduceDifferentPatterns`,
j'avais initialement la convention **forward=(cos, sin)**. Le test échouait
parce que ça générait un seul step worldX/Z constant pour toute la scène.

La convention correcte (déduite de `transform.s:24-76` et
`zone_edge_pvs.c:697-708`) est **forward=(sin, cos)**, **perpRight=(cos, -sin)**.

À angle=0 (joueur face au Nord = +Z), forward=(0, 1) → on regarde dans
+Z, ce qui est cohérent avec les conventions Amiga (Z=Nord, X=Est).

Cette découverte est documentée dans le code et dans la doc d'architecture.

---

## 5. Risques de divergence

| Risque | Sévérité | Mitigation |
|---|---|---|
| Focal length choisie arbitrairement (256) | Élevée pour pixel-perfect, faible pour visuel | À calibrer empiriquement vs original |
| Pas de table `OneOverN_vw` (division entière par scanline) | Performance, pas correctness | Acceptable pour première itération |
| Pas d'interpolation perspective-correct des walls | **Intentionnel** — fidèle à l'original | Documenté |
| Pas d'animation par direction des objets | Visible (objet toujours face caméra) | TODO subsystem futur |
| Convention forward/perpRight | Critique | Vérifié par test, doc explicite |
| Endpoints derrière la caméra | Possible bug si non-clippé en amont | Caller responsability |

---

## 6. Récapitulatif cumulé

| Sous-système | Tests | Statut |
|---|---|---|
| 01 — Math fondamental | 39 | ✅ |
| 02 — Loader binaires | 66 | ✅ |
| 03 — Gameplay state runtime | 24 | ✅ |
| 04 — Transform3D | 11 | ✅ |
| 05 — PvsErrata | 6 | ✅ |
| 06 — Edge PVS | 17 | ✅ |
| 07 — Renderer (fondations) | 38 | ✅ |
| **08 — Setup code des rasterizers** | **19** | **✅** |
| **Total** | **220** | ✅ |

---

## 7. Pour la suite

Avec les setups en place, on peut maintenant assembler un **pipeline rendu
end-to-end minimal** :

1. **Zone graph walker** (`draw_zone_graph.s`) — itère les zones dans
   l'ordre painter, dispatche par tag (wall/floor/ceiling/object/water/
   backdrop)
2. **Integration end-to-end** : un Test qui construit un mini-niveau,
   appelle Transform3D, walks zones, appelle WallSetup/FloorSetup/
   ObjectSetup en cascade, et vérifie un framebuffer "raisonnable"
3. **Gouraud shading** : ajouter l'interpolation per-pixel pour walls + floors
4. **Per-edge clipping** (`draw_set_clip.s`) : raffinement des bornes
5. **Player physics + collision** : changement de domaine vers gameplay
6. **Audio Protracker** : changement vers son
