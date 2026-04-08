# PROJECT STATUS — Alien Breed 3D II Java/JME Port

**Repo source :** https://github.com/guillaumemonet/alienbreed3d2  
**Stack :** Java 21 · JMonkeyEngine 3.7.0-stable · Bullet physics · SpiderMonkey network · Gradle 8  
**Lancement :** `./gradlew run`  
**Test niveau :** `./gradlew run --main-class=com.ab3d2.LevelDataTest`  
**Setup assets :** `./gradlew run --main-class=com.ab3d2.tools.AssetSetup`

> **Migration LWJGL → JME effective depuis session 6.**  
> JME 3.7.0-stable embarque LWJGL 3.3.3 en transitif — le code LWJGL legacy
> continue de compiler. Seul `lwjgl-stb` est redéclaré explicitement.

---

## Architecture actuelle (après session 6)

```
src/main/java/com/ab3d2/
│
├── Main.java                        ← extends SimpleApplication (JME)
├── LevelManager.java                ← cache niveaux A-P (inchangé)
│
├── app/                             ← AppStates JME (remplacent les Screen LWJGL)
│   ├── Ab3d2AppState.java           ← base AbstractAppState
│   ├── MenuAppState.java            ← menu principal (port de MainMenuScreen)
│   └── LevelSelectAppState.java     ← stub (TODO: port LevelSelectScreen)
│
├── input/
│   └── Ab3d2InputMapper.java        ← mappings JME depuis menunb.s / prefs_keys.h
│
├── core/
│   ├── GameState.java               ← sealed interface + records Java 21 (inchangé)
│   ├── GameContext.java             ← adapté JME (plus Window/Renderer2D/Audio LWJGL)
│   ├── PlayerState.java             ← virgule fixe 24.8, mouvements (inchangé)
│   └── level/                       ← parsers + structs (tous inchangés, pur Java)
│       ├── LevelBinaryParser.java
│       ├── GraphicsBinaryParser.java
│       ├── LevelData.java
│       ├── ZoneData.java, ZEdge.java, ZPVSRecord.java, Vec2W.java
│       ├── CollisionMap.java, ZoneTraversal.java
│       └── Level.java
│
├── core/math/
│   └── Tables.java                  ← SinCosTable + ConstantTable (inchangé)
│
├── assets/
│   ├── MenuAssets.java              ← + load(Path) sans GL (mode JME)
│   ├── WallTextureExtractor.java    ← .256wad PACK0/1/2 + shade table (inchangé)
│   ├── AmigaBitplaneDecoder.java    ← (inchangé)
│   └── WadTextureData.java          ← (inchangé)
│
├── menu/                            ← composants pure Java (logique inchangée)
│   ├── FireEffect.java              ← + initCPU() + computePixels() (mode JME)
│   ├── ScrollingBackground.java     ← + initCPU() + getScrolledPixels() (mode JME)
│   ├── MenuRenderer.java            ← (inchangé)
│   └── MenuCursor.java              ← (inchangé)
│
├── tools/
│   ├── AssetSetup.java, SBDepack.java, FloorIffLoader.java (inchangés)
│
└── [legacy LWJGL — compilent encore via transitifs JME, à supprimer après migration]
    ├── core/Window.java             ← remplacé par AppSettings JME
    ├── core/GameLoop.java           ← remplacé par simpleUpdate()
    ├── core/InputHandler.java       ← remplacé par Ab3d2InputMapper + ActionListener
    ├── core/Screen.java             ← remplacé par Ab3d2AppState
    ├── core/StateManager.java       ← remplacé par AppStateManager JME
    ├── render/Renderer2D.java       ← remplacé par guiNode + Texture2D dynamique
    ├── render/Camera.java           ← remplacé par Camera JME
    ├── render/WireRenderer3D.java   ← remplacé par LevelMeshBuilder (Phase 2)
    ├── render/TexturedRenderer3D.java
    ├── audio/AudioManager.java      ← remplacé par AudioNode JME (Phase 3)
    └── game/InGameScreen.java       ← remplacé par GameAppState (Phase 5)
```

---

## Flux de navigation

```
MenuAppState → "PLAY GAME" → LevelSelectAppState → ENTER → GameAppState (TODO)
MenuAppState → ESC → mode QUIT → "YES!" → app.stop()
```

---

## Stratégie migration composants menu

Les composants `FireEffect` et `ScrollingBackground` ont désormais **deux modes** :

| Mode     | Init                | Rendu               | Usage              |
|----------|---------------------|---------------------|--------------------|
| GL       | `init()`            | `getTexture()`      | Ancien LWJGL       |
| CPU/JME  | `initCPU()`         | `computePixels()`   | MenuAppState JME   |

`MenuAssets` a deux variantes de `load()` :
- `load(Path)` → pur Java, pas de GL (mode JME)
- `load(GameContext)` → crée aussi des textures GL (legacy, @Deprecated)

---

## Format twolev.bin (confirmé, inchangé)

```
[0..1599]  Messages (10 × 160 bytes)
[1600]     Header TLBT (54 bytes)
[1654]     ControlPoints
[Offsets]  Points, ZEdge array, Zone blobs, ObjectData…
```

**NOTE : NumZones stocke -1 dans le fichier (ajouter 1 au parsing)**

---

## Constantes critiques — NE PAS MODIFIER

### Palette menu
```java
c & 0xE0 != 0  → fontpal[(c>>5)&7]
c & 0x1C != 0  → firepal mix
sinon          → backpal[c&3]
```
### colorBits (MenuRenderer)
```java
int colorBits = (b0 << 3) | (b1 << 4) | (b2 << 5);
```
### Blitter feu (30 Hz)
- count=0 → LF=0x1f, count=1 → LF=0xff (décalage 16px), count=2,3 → LF=0x0f

### PlayerState — angles (defs.i PlrT_AngPos_w)
- 0..4095 = cycle complet, 0 = nord (+Z), 1024 = est (+X)

### Coordonnées JME
- Amiga X → JME X · Amiga Z → JME –Z (flip)
- Hauteurs LONG 24.8 inverted → `-(rawLong >> 8) * (1/256f)`

---

## COMPLÉTÉ ✓

| Session | Composant |
|---------|-----------|
| 1 | Infrastructure Gradle + LWJGL, Core engine, Renderer2D |
| 1 | Menu complet (fire, font, background, cursor) |
| 2 | Analyse formats binaires, CollisionMap (grille 100×100) |
| 3 | WallTextureExtractor (.256wad PACK0/1/2 + shade table) |
| 3 | Level binary structs (Vec2W, ZEdge, ZPVSRecord, ZoneData) |
| 3 | LevelBinaryParser (big-endian, dumpHex diagnostic) |
| 3 | Tables.java (SinCosTable + ConstantTable) |
| 3 | LevelManager, AssetSetup, LevelDataTest |
| 3 | PlayerState (virgule fixe 24.8, moveForward/Strafe/rotate) |
| 3 | LevelSelectScreen (police 5×7 intégrée, détection assets) |
| 3 | InGameScreen (vue top-down, mouvement clavier+souris) |
| 3 | TopDownRenderer (Bresenham edges, joueur, projection auto) |
| 4 | Format TLBT confirmé + LevelBinaryParser réécrit |
| 4 | GraphicsBinaryParser — TLGT + zones |
| 4 | VALIDE LEVEL_A : 134 zones, 635 edges, 348 pts |
| 5 | Camera.java, WireRenderer3D, ZoneTraversal, LevelObjExporter |
| 5 | VALIDE GÉOMÉTRIE : niveaux A-P exportés OBJ, conformes |
| **6** | **Migration officielle LWJGL → JME 3.7.0-stable** |
| **6** | **build.gradle : jme3-core/desktop/lwjgl3/effects/bullet/networking/plugins** |
| **6** | **Main.java → extends SimpleApplication** |
| **6** | **Ab3d2AppState (base), MenuAppState (JME), LevelSelectAppState (stub)** |
| **6** | **Ab3d2InputMapper (mappings depuis menunb.s / prefs_keys.h)** |
| **6** | **FireEffect.initCPU() + computePixels() — mode sans GL** |
| **6** | **ScrollingBackground.initCPU() + getScrolledPixels() — mode sans GL** |
| **6** | **MenuAssets.load(Path) — mode JME sans GL** |

---

## À FAIRE — ordre de priorité

### Priorité 1 — Vérifier que ça compile et tourne
```bash
./gradlew compileJava        # doit passer sans erreur
./gradlew run                # doit afficher le menu JME
```

### Priorité 2 — LevelSelectAppState
- Port de `LevelSelectScreen` (liste A-P, police 5×7, détection assets)
- Transition vers `GameAppState` sur ENTER

### Priorité 3 — Scène 3D (Phase 2)
- `LevelMeshBuilder` → zones + edges → `Geometry` JME (mesh statique)
- `LiftControl` → `AbstractControl` JME (portes/ascenseurs)
- Textures .256wad → `Material` JME

### Priorité 4 — Moteur core (Phase 3)
- `PlayerControl` → `CharacterControl` Bullet (StepUp natif)
- `ZoneLightBuilder` → `PointLight` par zone (z_Brightness)
- `AudioAppState` → AudioNode JME + sons raw, reverb depuis z_Echo

### Priorité 5 — Gameplay & IA (Phase 5)
- `AlienControl` (ItsAnAlien → BehaviorTree)
- `InventorySystem` (GLF data)
- `ProjectileControl` (ShotT → Bullet raycast)

### Priorité 6 — Multijoueur (Phase 6)
- SpiderMonkey Master/Slave
- GameSync (positions, tirs, état IA)

### Priorité 7 — Polish (Phase 7)
- `RetroFilter` : pixelisation 320×200 + palette EHB + Bayer + CRT
