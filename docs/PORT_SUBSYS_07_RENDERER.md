# Sous-système 07 — Renderer (walls/floors/objects, fondations)

Premier livrable du **renderer software 2.5D**. Trois rasterizers + infrastructure :
`Framebuffer`, `Viewport`, `Palette` (avec shade table 64×256), `WallRasterizer`,
`FloorRasterizer`, `ObjectRasterizer`.

**Status** : ✅ porté en version <b>simplifiée</b>, 38 tests pixel-level
(cumul total : **201/201**).
**Package** : `com.team17.ab3d2.render`
**Tâches** : #39 → #44.

---

## 1. Scope de ce sprint

Le renderer original (`hires.s`, `modules/draw.s`, `modules/draw/draw_*.s`,
`objdrawhires.s`, `hireswall.s`, `hiresgourwall.s`) totalise **>4500 lignes
d'ASM** réparties sur 7 fichiers. Le port complet pixel-perfect prendrait
plusieurs sprints. Ce sprint pose les **fondations** :

### 1.1 Livré

| Composant | Origine ASM/C | Fidélité |
|---|---|---|
| `Framebuffer` | `Vid_FastBufferPtr_l` (chunky 8-bit linéaire) | Identique |
| `Viewport` | `Vid_CentreX_w`, `Vid_BottomY_w`, `Vid_RightX_w` | Identique |
| `Palette` (RGB + shade table 64×256) | `Vid_RGBPalette_vl`, `Draw_TexturePalettePtr_l` | Identique |
| `WallRasterizer.drawColumn` | `draw_wall.s` inner loop ({@code drawwallPACK0}) | **Simplifié** (cf. §1.2) |
| `FloorRasterizer.drawScanline` | `draw_floor.s` inner loop ({@code acrossscrngour}) | **Simplifié** |
| `ObjectRasterizer.drawSprite` + `depthSort` | `objdrawhires.s` (3655 lignes !) | **Très simplifié** |

### 1.2 Simplifications par rapport à l'original

**WallRasterizer** :
- ❌ Pas de **format texture packé** : l'original stocke 3 texels par 2 octets
  (PACK0/1/2 — 5 bits/texel). Ce port utilise 1 octet par texel (les valeurs
  restent dans `[0..31]` pour préserver la sémantique). La valeur produite à
  l'écran est identique tant que la texture en entrée a les bonnes valeurs.
- ❌ Pas d'**alternance dim/bright per scanline** : l'original alterne entre
  deux palettes (a2/a4) pour faire du dithering 1-ligne. Ce port utilise une
  seule palette (paramètre `brightRow`). Le dithering sera ajouté dans un
  futur sub-system.
- ❌ Pas de **variantes par largeur d'écran** : l'original a
  `draw_IterationTable_vw` pour spécialiser la boucle. Ici une boucle unique.
- ✅ **Affine V step** en 16.16 fixed-point : identique à l'ASM.
- ✅ **Mask V** (texture height − 1) : identique.

**FloorRasterizer** :
- ❌ Pas de **Gouraud per-pixel** (interpolation de brightness). Single
  `brightRow` par scanline. L'original interpole via `leftbright`/`brightspd`.
- ✅ **Pas world XZ par pixel** en 24.8 fixed-point : identique.
- ✅ **Mask 64×64** (`(world &gt;&gt; 8) &amp; 63`) : identique.

**ObjectRasterizer** :
- ❌ Pas de **depth table de 80 entrées préallouée** : sort externalisé via
  `depthSort()` qui retourne un `List<Sprite>` trié.
- ❌ Pas de **scaling via `ConstantTable`** précalc : on calcule
  `(srcSize << 16) / destSize` à chaque sprite.
- ❌ Pas de **per-zone clipping** : seul le clipping écran est appliqué.
- ✅ **Painter's algorithm back-to-front** : identique.
- ✅ **Transparence couleur 0** : identique (`if (texel != 0) write;`).
- ✅ **Scaling par fixed-point 16.16 step** : identique.

### 1.3 Renvoyé aux sous-systèmes futurs

- **Gouraud shading** sur walls et floors (interpolation per-pixel)
- **Per-edge clipping** (`Draw_LeftClip_w`, `Draw_RightClip_w` raffinés)
- **C2P** (chunky to planar) pour l'affichage AGA natif
- **Backdrop/sky** (`Draw_Backdrop`, `SKY_BACKDROP_W=648`)
- **Water rendering** (palette animation + tile cycle)
- **Teleport FX** (effets spéciaux)
- **Multi-CPU dispatch** (variants `_060.s` optimisés)
- **Setup code** (les milliers de lignes dans `hires.s` qui calculent les
  paramètres des rasterizers à partir des points transformés)

---

## 2. Architecture

```
com.team17.ab3d2.render/
├── Transform3D.java          (sous-système 04)
├── Framebuffer.java          (chunky 8-bit linéaire)
├── Viewport.java             (centre + bornes clip)
├── Palette.java              (RGB + shade table 64×256)
├── WallRasterizer.java       (drawColumn)
├── FloorRasterizer.java      (drawScanline)
└── ObjectRasterizer.java     (drawSprite + depthSort + drawAll)
```

### 2.1 Framebuffer

Linear chunky 8-bit. Constantes canoniques du moteur :
- `SCREEN_WIDTH = 320`, `SCREEN_HEIGHT = 256`
- `FS_HEIGHT = 240` (avec FS_HEIGHT_HACK=1)
- `SMALL_WIDTH = 192`, `SMALL_HEIGHT = 160`

Le buffer est alloué `byte[stride * height]` avec stride configurable (peut
être > width pour padding aligné). L'accès est `data[y * stride + x]`.

### 2.2 Viewport

Conteneur immutable des bornes de rendu : `leftX`, `rightX` (exclusive),
`topY`, `bottomY` (exclusive), `centreX`, `centreY`. Deux factories :
- `Viewport.fullScreen()` → 320×240, centre (160, 120)
- `Viewport.small()` → 192×160, centre (96, 80)

### 2.3 Palette

Deux composants :
1. **RGB array** (768 octets, R/G/B par couleur 0..255) — pour la sortie
   finale vers RTG ou pour le viewer de debug.
2. **Shade table** (16 384 octets = 64×256) — table de lookup pour le
   shading.

Lookup principal : `shade(brightRow, color) = shadeTable[(brightRow & 0x3F) * 256 + color]`.

L'accès direct via `shadeTableRaw()` + `shadeRowOffset(row)` est optimisé
pour les boucles inner :

```java
byte[] shade = palette.shadeTableRaw();
int rowOff = palette.shadeRowOffset(brightRow);
// boucle inner :
int litColor = shade[rowOff + texelColor] & 0xFF;
```

### 2.4 WallRasterizer

Signature :
```java
drawColumn(fb, x, yTop, yBot, texture, twidth, theight, u, vStart, vStep, brightRow, palette)
```

Pseudocode interne :
```
vMask = textureHeight - 1   // doit être power-of-2
v = vStart                  // 32-bit signé, 16.16 fixed-point
pour y = actualTop à actualBot :
    texelV = (v >>> 16) & vMask
    texelColor = texture[texelV * textureWidth + u]
    litColor = shadeTable[shadeRowOff + texelColor]
    framebuffer[y * stride + x] = litColor
    v += vStep
```

Le V wrap implicite via `>>> 16` + mask reproduit fidèlement le
`add.l d3,d4 / addx.w d2,d4 / and.w d7,d4` de l'ASM.

### 2.5 FloorRasterizer

Signature :
```java
drawScanline(fb, y, xLeft, xRight, worldXStart, worldZStart, dWorldX, dWorldZ, texture, brightRow, palette)
```

Pseudocode :
```
worldX = worldXStart  // 24.8 fixed-point
worldZ = worldZStart
pour x = actualLeft à actualRight :
    texelS = (worldX >> 8) & 63
    texelT = (worldZ >> 8) & 63
    texelColor = texture[texelT * 64 + texelS]
    litColor = shadeTable[shadeRowOff + texelColor]
    framebuffer[y * stride + x] = litColor
    worldX += dWorldX
    worldZ += dWorldZ
```

Format texture : `byte[64 * 64]` row-major (T puis S). Taille fixe = 64
(constante moteur `TILE_SIZE`).

### 2.6 ObjectRasterizer

API à 3 niveaux :
- `Sprite` (record) : descripteur d'un sprite avec position écran, taille
  écran, données source, zDepth.
- `depthSort(List<Sprite>)` : retourne une copie triée par `zDepth` décroissant
  (back-to-front).
- `drawSprite(fb, sprite, palette)` : rasterise un seul sprite.
- `drawAll(fb, sortedSprites, palette)` : itère et dessine.

Pseudocode `drawSprite` :
```
dSrcX = (sourceWidth << 16) / screenWidth   // fixed-point step
dSrcY = (sourceHeight << 16) / screenHeight
clip à [xLeft..xRight] × [yTop..yBot]
srcY = (yTop - screenY) * dSrcY
pour y = yTop à yBot :
    sy = srcY >>> 16
    srcX = (xLeft - screenX) * dSrcX
    pour x = xLeft à xRight :
        sx = srcX >>> 16
        texel = source[sy * sourceWidth + sx]
        si texel != 0 :    // 0 = transparent
            framebuffer[y * stride + x] = shadeTable[rowOff + texel]
        srcX += dSrcX
    srcY += dSrcY
```

---

## 3. Tests (38)

### 3.1 FramebufferTest (9)
- Constantes (320×256, 240, 192×160)
- Allocation + dimensions
- get/setPixel
- pixelOffset linéaire
- Stride > width
- Clear
- Hors-bornes
- Dimensions invalides

### 3.2 PaletteTest (7)
- Tailles canoniques (256, 64, 32)
- Identity palette (RGB grayscale, shade identity)
- shadeRowOffset linéaire
- Clamp row à `0x3F` (row 64 → 0)
- Rejet des tailles fausses
- Shade table custom (glare max / normal / ombre max)

### 3.3 WallRasterizerTest (7)
- Dessin vertical de base
- Step V fractionnaire 0.5 (chaque texel répété 2×)
- Mask V (wrap quand y > textureHeight)
- Sélection de colonne via U dans texture multi-colonnes
- Clipping vertical haut + bas
- Rejet hauteur non-power-of-2
- BrightRow change l'output

### 3.4 FloorRasterizerTest (6)
- TILE_SIZE = 64
- Scanline texel uniforme
- worldX step → S avance
- worldZ step → T avance
- Wrap à 64
- Clip horizontal

### 3.5 ObjectRasterizerTest (7)
- Sprite 1:1 scale (motif 3×3 préservé)
- Couleur 0 = transparent
- Sprite scaled 2× (chaque source pixel couvre 2×2 destination)
- Sprite hors écran → skip silencieux
- Sprite partiellement clippé
- depthSort back-to-front
- drawAll : painter's (front overwrite back)

---

## 4. Risques de divergence

| Risque | Sévérité | Mitigation |
|---|---|---|
| Format texture packé vs unpacked | Élevée pour pixel-perfect, faible pour "correctness" | Documenté ; sera traité quand les assets seront convertis |
| Dithering alternance dim/bright | Visible (1-line halftone manquant) | TODO subsystem futur |
| Gouraud floor manquant | Visible (sols flat-shaded) | TODO subsystem futur |
| Per-edge clipping manquant | Plus d'overdraw qu'attendu | OK fonctionnellement, perf-only |
| C2P manquant | Sortie reste en chunky | Conscient — la couche RTG/AGA est séparée |
| `divs.w` vs `int / int` Java | Aucune en pratique | Les tests vérifient |
| `int >>> 16` vs Motorola `swap` | Aucune | Sémantique identique pour positive ints |

---

## 5. Risques de divergence numériques détaillés

Pour WallRasterizer, le V step est en 16.16 fixed-point :
- Si V = 0x00010000 (1.0) et vStep = 0x00008000 (0.5), après 2 itérations
  V = 0x00020000 (2.0). Java `int v += vStep` donne exactement ça pour `int`
  avec overflow modulo 2^32 — identique au comportement Motorola
  `add.l d3, d4`.
- Le `>>> 16` (logical right shift) vs `>> 16` (arithmetic) : pour V positif,
  identique. Pour V négatif, `>>> 16` produit une grande valeur positive.
  Le code utilise `>>>` pour correspondre au fait que V est traité comme
  unsigned 32-bit dans l'ASM (l'index doit toujours être positif).

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
| **07 — Renderer (fondations)** | **38** | **✅** |
| **Total** | **201** | ✅ |

---

## 7. Prochaines étapes naturelles

Maintenant que les rasterizers existent, plusieurs directions sont possibles :

A) **Setup code des walls/floors/objects** : porter `hires.s` qui calcule
   les paramètres des rasterizers (yTop, yBot, vStart, vStep, etc.) à partir
   des points transformés. C'est le pont entre `Transform3D` (sous-système 04)
   et les rasterizers.

B) **Zone graph walker** : porter `draw_zone_graph.s` (440 lignes) qui itère
   les zones dans l'ordre painter et dispatche par tag (wall/floor/ceiling/
   object/water/backdrop).

C) **Per-edge clipping** : porter `draw_set_clip.s` (162 lignes) pour
   raffiner les bornes horizontales par edge.

D) **Gouraud floor** (`draw_floor.s` complet) + dithering walls : ajouter
   les variantes manquantes aux rasterizers existants.

E) **Audio Protracker** : changement de domaine, sub-system audio
   (`modules/music.s`).

F) **Player physics + collision** : `objectmove.s` + `fall.s` pour le
   gameplay.

À décider à la prochaine étape.
