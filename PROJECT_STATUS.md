# PROJECT STATUS — Alien Breed 3D II Java/LWJGL Port

**Repo source :** https://github.com/guillaumemonet/alienbreed3d2  
**Stack :** Java 21, LWJGL 3.3.3, Gradle 8, OpenGL (FBO), OpenAL, STB  
**Lancement :** `./gradlew run`  
**Test niveau :** `./gradlew run --main-class=com.ab3d2.LevelDataTest`  
**Setup assets :** `./gradlew run --main-class=com.ab3d2.tools.AssetSetup`

---

## Architecture actuelle du projet

```
src/main/java/com/ab3d2/
├── Main.java                        ← point d'entrée
├── LevelManager.java                ← chargement niveaux avec cache
├── LevelDataTest.java               ← main standalone (valide parsing niveau)
│
├── tools/
│   └── AssetSetup.java              ← copie assets Amiga → resources/
│
├── core/
│   ├── GameState.java               ← sealed interface + records Java 21
│   ├── GameContext.java             ← record passé à tous les screens
│   ├── GameLoop.java                ← fixed timestep 60Hz
│   ├── StateManager.java            ← transitions entre screens (factories)
│   ├── Screen.java                  ← interface init/update/render/destroy
│   ├── Window.java                  ← GLFW, 320x200 upscalé, letterbox
│   ├── InputHandler.java            ← clavier/souris edge+level triggered
│   ├── PlayerState.java             ← position, angle, zone, virgule fixe 24.8
│   └── level/
│       ├── Vec2W.java               ← point 2D (record, 4 bytes big-endian)
│       ├── ZEdge.java               ← arête monde (record, 16 bytes, sideOf())
│       ├── ZPVSRecord.java          ← enregistrement PVS (record, 8 bytes)
│       ├── ZoneData.java            ← zone complète + listes edge/point/PVS
│       ├── LevelData.java           ← holder : zones[], edges[], points[]
│       ├── LevelBinaryParser.java   ← parser twolev.bin big-endian + dumpHex()
│       ├── CollisionMap.java        ← grille collision 100×100 (twolev.map)
│       └── Level.java               ← métadonnées niveau (id, index)
│
├── core/math/
│   └── Tables.java                  ← SinCosTable (bigsine) + ConstantTable
│
├── render/
│   └── Renderer2D.java              ← FBO 320x200, sprite batch, fade overlay
│
├── assets/
│   ├── AssetManager.java            ← textures PNG/WAD, palette draw_Palette_vw
│   ├── AmigaBitplaneDecoder.java    ← décodeur bitplanes + buildMenuPalette()
│   ├── MenuAssets.java              ← assets menu (back2.raw, font, palettes)
│   ├── WadTextureData.java          ← données texture .256wad décodée + shade table
│   └── WallTextureExtractor.java    ← décodage .256wad → ARGB (PACK0/1/2 + shade)
│
├── audio/
│   └── AudioManager.java            ← OpenAL init (stub, sons non implémentés)
│
├── menu/
│   ├── MainMenuScreen.java          ← menu MAIN/QUIT/CREDITS
│   ├── FireEffect.java              ← simulation blitter feu (6 plans binaires)
│   ├── FirePlots.java               ← 50 points Lissajous
│   ├── MenuRenderer.java            ← rendu glyphes dans buffer bitplane
│   ├── MenuCursor.java              ← curseur animé glyphes 123-130
│   ├── ScrollingBackground.java     ← background scrollant vertical
│   ├── Ab3dFont.java                ← police 16x16 via texture atlas
│   ├── BitmapFont.java
│   ├── C64Font.java
│   ├── MenuDefinition.java
│   └── MenuRenderer.java
│
└── game/
    ├── InGameScreen.java            ← vue top-down, chargement niveau, mouvement
    ├── LevelSelectScreen.java       ← liste A-P, police 5x7 intégrée
    └── TopDownRenderer.java         ← rendu top-down soft → int[] ARGB
```

---

## Flux de navigation

```
MainMenuScreen → "PLAY GAME" → LevelSelectScreen → ENTER → InGameScreen
                                                  → ESC   → MainMenuScreen
InGameScreen   → ESC → MainMenuScreen
```

---

## Assets attendus (resources/)

```
resources/
├── palette.bin           ← 768 bytes (RGB8) ou 1536 bytes (draw_Palette_vw)
├── bigsine               ← 16384 bytes (table sinus binaire)
├── levels/
│   ├── LEVEL_A/
│   │   ├── twolev.bin    ← géométrie zones/edges/points (big-endian)
│   │   ├── twolev.map    ← grille collision 100×100
│   │   ├── twolev.dat    ← données jeu (objets, IA)
│   │   ├── twolev.clips  ← clips sprites
│   │   ├── twolev.flymap
│   │   └── twolev.graph.bin
│   └── LEVEL_B/ … LEVEL_P/
├── walls/
│   ├── stonewall.256wad
│   ├── hullmetal.256wad
│   └── … (14 textures)
└── menu/
    ├── back2.raw / back.pal
    ├── firepal.pal2 / font16x16.pal2
    ├── font16x16.raw2
    └── credits_only.raw
```

**Copie des assets :** lancer `AssetSetup` une fois depuis l'IDE ou :
```bash
./gradlew run --main-class=com.ab3d2.tools.AssetSetup
```

---

## Format twolev.bin (confirmé)

```
Offset    Type   Description
──────    ────   ──────────────────────────────────────────────
0         WORD   numZones
2         LONG   numEdges
6         WORD   numPoints
8         LONG×N table pointeurs zones (N=numZones, offset depuis début fichier)
8+4N      var    blob zones : [edge IDs,-4][point IDs,-4][Zone 48b][ZPVSRecord,-1]
var       16×E   ZEdge array (E=numEdges)
var       4×P    Vec2W points array (P=numPoints)
```

Toutes les valeurs big-endian. Hauteurs inversées (plus petit = plus haut).

---

## Constantes critiques — NE PAS MODIFIER

### Palette menu (buildMenuPalette)
```
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

### PlayerState — angles
- 0..4095 = cycle complet, 0 = nord (+Z), 1024 = est (+X)

---

## COMPLÉTÉ ✓

| Session | Composant |
|---------|-----------|
| 1       | Infrastructure Gradle + LWJGL, Core engine, Renderer2D |
| 1       | Menu complet (fire, font, background, cursor) |
| 2       | Analyse formats binaires, CollisionMap (grille 100×100) |
| 3       | WallTextureExtractor (.256wad PACK0/1/2 + shade table) |
| 3       | Level binary structs (Vec2W, ZEdge, ZPVSRecord, ZoneData) |
| 3       | LevelBinaryParser (big-endian, dumpHex diagnostic) |
| 3       | Tables.java (SinCosTable + ConstantTable) |
| 3       | LevelManager, AssetSetup, LevelDataTest |
| 3       | PlayerState (virgule fixe 24.8, moveForward/Strafe/rotate) |
| 3       | LevelSelectScreen (police 5×7 intégrée, détection assets) |
| 3       | InGameScreen (vue top-down, mouvement clavier+souris) |
| 3       | TopDownRenderer (Bresenham edges, joueur, projection auto) |
| 3       | StateManager refactorisé (factories paramétriques) |
| 4       | Format TLBT confirmé depuis defs.i + hires.s (lignes 338-413) |
| 4       | LevelBinaryParser réécrit — parse TLBT complet (starts, comptes, offsets) |
| 4       | GraphicsBinaryParser — TLGT header + table ptrs zones → ZoneData[] |
| 4       | LevelData mis à jour — plr1/plr2 starts depuis TLBT, plus de startX/Z TODO |
| 4       | LevelManager mis à jour — assemble bin + graph.bin via GraphicsBinaryParser |
| 4       | InGameScreen — spawn joueur sur plr1StartX/Z/ZoneId réels TLBT |
| 4       | LevelDataTest — test standalone complet avec hex dump + cohérence |
| 4       | BUG FIX coords joueur signées (-808,184) au lieu de (64728,184) |
| 4       | BUG FIX numEdges : max(edge_id)+1 au lieu de ObjData-FloorLine (négatif!) |
| 4       | BUG FIX terminateurs listes : tout id<0 (pas seulement -4) |
| 4       | VALIDE LEVEL_A : 134 zones, 635 edges (301 murs+334 portails), 348 pts |
| 5       | Camera.java — transform monde→cam, projection perspective (FOCAL=96) |
| 5       | WireRenderer3D — renderer filaire colore par zone (PVS traversal) |
| 5       | Renderer3DTest — genere PNG de test depuis 8 angles + 5 positions |
| 5       | ZoneTraversal.java — sideOf(), findZone(), updateZone() |
| 5       | ZoneTraversalTest — test standalone connectivite zones |
| 5       | LevelObjExporter — export OBJ Blender-compatible (Z negated), valide visuellement |
| 5       | VALIDE GEOMETRIE : niveaux A-P exportes, coordonnees et orientations conformes au jeu |

---

## Formats binaires (confirmes + valides sur LEVEL_A)

### twolev.bin

- **[0..1599]** Messages texte (10 x 160 bytes)
- **[1600]** Header TLBT (54 bytes) : Plr1/2 StartX/Z/Zone (WORDs SIGNES !), NumCtrl, NumPts, NumZones-1, NumObjs, 8 offsets ULONG
- **[1654]** ControlPoints (NumControlPoints x 4 bytes)
- **[ObjPtsOfs]** ObjectPoints; **[ObjDataOfs]** ObjectData (x64b); **[ShotOfs]** Shots...
- **[PointsOfs]** Vec2W points (x4b)
- **[FloorLineOfs]** ZEdge array (x16b) — **numEdges = max(edge_id dans zones) + 1**
- **[ptr[i]]** Zone blobs : `[edge_ids,-1][shared_ids,-2][pt_ids,-4][ZoneT 48b][PVS,-1]`

**NOTE : NumZones stocke -1 dans le fichier (ajouter 1 au parsing)**
**NOTE : ObjectDataOffset < FloorLineOffset — la formule hires.s ligne 363 est fausse**

### twolev.graph.bin

- **[0..15]** Header TLGT (5 x ULONG) : DoorOfs, LiftOfs, SwitchOfs, ZoneGraphAddsOfs, (puis table)
- **[16+]** Table N x ULONG : chaque ptr = offset absolu dans twolev.bin vers ZoneT

### Terminateurs
- Edge/point ID lists : tout valeur **< 0** (Zone_IsValidEdgeID = id >= 0)
- PVS list : pvs_ZoneID **< 0** (= -1)

---

## A FAIRE (priorite ordre)

1. **Renderer 3D textures** — charger les .256wad, mapper sur les murs.

2. **Zone traversal affine** — gestion des hauteurs (upper zones, water).

3. **Audio** — charger les sons raw PCM, initialiser OpenAL sources.

4. **HUD** — energie, munitions, arme selectionnee.

5. **Objets / IA** — ObjectDataOffset dans twolev.bin, format ObjT 64 bytes.
