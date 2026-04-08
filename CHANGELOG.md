# CHANGELOG — Alien Breed 3D II Java Port

Chaque entrée marque une étape majeure du portage.

---

## [2025-04-08] Session 6 — Migration officielle LWJGL → JMonkeyEngine 3.7.0-stable

### Contexte
Après validation de la géométrie (sessions 1-5 : parsers, wireframe, OBJ export),
décision de migrer vers JMonkeyEngine pour bénéficier de la physique Bullet,
de SpiderMonkey (réseau), de l'audio 3D, et des filtres post-process natifs.

### Changements

**Infrastructure**
- `build.gradle` : LWJGL direct → jme3-core, jme3-desktop, jme3-lwjgl3, jme3-effects,
  jme3-bullet, jme3-bullet-native, jme3-networking, jme3-plugins.
  LWJGL reste disponible en transitif (même version 3.3.3 qu'interne JME).
  Seul `lwjgl-stb` redéclaré explicitement (non inclus dans transitifs JME).

**Bootstrap JME**
- `Main.java` : `extends SimpleApplication` — remplace l'ancien trio Main + Window + GameLoop.
  Résolution 960×600 (320×200 × 3), VSync, pas de dialog JME au démarrage.

**Architecture AppState**
- `app/Ab3d2AppState.java` : classe de base `AbstractAppState` (remplace interface `Screen`)
- `app/MenuAppState.java` : menu principal porté vers JME
  - `FireEffect.initCPU()` + `computePixels()` → texture dynamique JME sans GL direct
  - `ScrollingBackground.initCPU()` + `getScrolledPixels()` → idem
  - Rendu via deux `Texture2D` dynamiques dans `guiNode`
  - Fade via alpha du matériau (plus de `glBlend` manuel)
- `app/LevelSelectAppState.java` : stub (TODO Phase 2)

**Input**
- `input/Ab3d2InputMapper.java` : mappings JME depuis `menunb.s` / `prefs_keys.h`
  - Tous les 16 controls originaux (turn, strafe, duck, jump, look, weapons…)
  - Actions menu (up/down/ok/back)
  - JME `KeyInput` + `MouseButtonTrigger`

**Composants menu refactorisés** (mode CPU sans GL)
- `menu/FireEffect.java` : +`initCPU()`, +`computePixels()` — retourne `int[]` ARGB
- `menu/ScrollingBackground.java` : +`initCPU()`, +`getScrolledPixels(int)` — idem
- `assets/MenuAssets.java` : +`load(Path)` sans GL (mode JME),
  +`decodeBackground()`, +`decodeFont()`, +`decodeCredits()`

**Core adapté**
- `core/GameContext.java` : record JME — plus Window/Renderer2D/AudioManager LWJGL.
  Champs : `jmeApp`, `ab3dAssets`, `levelManager`, `assetRoot`.

**Stubs legacy** (gardés pour compilation, ne doivent plus être instanciés)
- `menu/MainMenuScreen.java` : stub `@Deprecated` → `throw UnsupportedOperationException`
- `game/InGameScreen.java` : stub `@Deprecated`
- `game/LevelSelectScreen.java` : stub `@Deprecated`

### Inchangés (pur Java, compilent sans modification)
- Tous les parsers : `LevelBinaryParser`, `GraphicsBinaryParser`, `ZoneData`, `ZEdge`…
- `core/GameState.java`, `core/PlayerState.java`, `core/math/Tables.java`
- `assets/WallTextureExtractor.java`, `assets/AmigaBitplaneDecoder.java`
- `game/TopDownRenderer.java`, `render/Camera.java`
- Outils standalone : `LevelDataTest`, `Renderer3DTest`, `TexturedRenderTest`…

---

## [2025-03-xx] Session 5 — Géométrie validée visuellement

- `LevelObjExporter` : export OBJ Blender-compatible avec Z négatif
- Validation géométrie niveaux A-P : coordonnées et orientations conformes
- `WireRenderer3D`, `ZoneTraversal`, `Camera`

---

## [2025-02-xx] Session 4 — Format binaire TLBT confirmé

- `LevelBinaryParser` réécrit depuis `defs.i` / `hires.s`
- `GraphicsBinaryParser` : TLGT header + table pointeurs zones
- BUG FIX : coords joueur signées, numEdges, terminateurs de listes
- VALIDE LEVEL_A : 134 zones, 635 edges (301 murs + 334 portails), 348 points

---

## [2025-01-xx] Sessions 1-3 — Infrastructure initiale

- Infrastructure Gradle + LWJGL, boucle de jeu fixed 60 Hz
- Menu complet : effet feu blitter, police 16×16, background scrollant, curseur animé
- Parsers binaires : `Vec2W`, `ZEdge`, `ZPVSRecord`, `ZoneData`
- `WallTextureExtractor` : décodage `.256wad` PACK0/1/2 + shade table
- `Tables.java` : SinCosTable depuis bigsine Amiga
- `PlayerState` : virgule fixe 24.8, mouvements, angles 0-4095
- `LevelSelectScreen` : liste A-P, police 5×7 intégrée, détection assets
- `InGameScreen` : vue top-down, mouvement clavier+souris
