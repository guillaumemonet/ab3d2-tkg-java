# Sous-système 16 — Pipeline complet & AI core

## Vue d'ensemble

Batch composé de 5 livrables (A→E) consolidant les acquis des sous-systèmes
précédents en pipeline opérationnel.

| ID  | Livrable                                                     | Statut  |
|-----|--------------------------------------------------------------|---------|
| 16A | LWJGL deps + impls natives (audio + display)                 | done    |
| 16B | Décodage chunk data des `.256wad` (PACK0/1/2)                | done    |
| 16C | Pipeline complet `LEVEL_A` → PNG (top-down map render)       | done    |
| 16D | AI state machine core (`modules/ai.s`)                       | done    |
| 16E | Effets MOD complets : arpeggio, slides, portamento, vibrato  | done    |

## 16A — LWJGL dependencies

### Modifications `build.gradle.kts`

Le build embarque maintenant LWJGL 3.3.4 avec détection automatique de l'OS
hôte :

```kotlin
val lwjglNatives: String = run {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    when {
        osName.contains("win")   -> "natives-windows"
        osName.contains("mac")   -> if (osArch.contains("aarch64")) "natives-macos-arm64" else "natives-macos"
        osName.contains("linux") -> if (osArch.contains("aarch64")) "natives-linux-arm64" else "natives-linux"
        else -> throw GradleException("OS non supporté")
    }
}
```

Composants intégrés :

- `org.lwjgl:lwjgl` (core)
- `org.lwjgl:lwjgl-openal` (audio)
- `org.lwjgl:lwjgl-glfw` (window + input)
- `org.lwjgl:lwjgl-opengl` (présentation framebuffer)

### Impls natives

Deux nouvelles classes dans `com.team17.ab3d2.platform` :

- **`LwjglDisplay`** : impl `Display` via GLFW + OpenGL 2.1. Upload chunky → RGB
  via `palette.rgbArray()`, blit fullscreen via texture 2D + quad immediate
  mode. Lifecycle : `open()` → `present()` → `shouldClose()` → `close()`.

- **`LwjglAudioOutput`** : impl `AudioOutput` via OpenAL (4 sources, une par
  canal Paula). Conversion period → pitch via constante `PAULA_CLOCK_PAL`
  (`3546894.6 Hz`). Conversion 8-bit signed → 16-bit signed mono.

Ces classes ne sont pas testables en CI (nécessitent display / device audio) ;
les tests unitaires continuent d'utiliser `InMemoryDisplay` et
`InMemoryAudioOutput`.

## 16B — Décodage chunk data des `.256wad`

### Format observé

Référence ASM : `hireswall.s:270-292` (dupliqué aux lignes 410, 491, 563, 638).

```
+0      Draw_PalettePtr_l : palette/shade table (2048 octets)
+2048   Draw_ChunkPtr_l   : chunk data
```

### Accès à un texel

```
U                 = (column rotated/interp) & textureWidthMask
index             = U + draw_FromTile_w
{quotient, remainder} = DivThreeTable[index]   // depuis sub-system 1
columnOffset      = quotient * 2 * textureHeight
chunk_addr        = Draw_ChunkPtr_l + columnOffset

pour chaque V :
    texel = pack_lookup(chunk_addr + V*2, remainder)
    ; PACK0 (rem 0) : low 5 bits of byte at offset V*2 + 1
    ; PACK1 (rem 1) : bits 9..5 of WORD at offset V*2
    ; PACK2 (rem 2) : bits 7..2 of byte at offset V*2
```

### API portée

`WallTextureDecoder.readTexel(u, v, textureHeight)` retourne l'index 5-bit qui
sert d'entrée à la shade table.

`guessTextureHeight()` essaie 16/32/64/128 avec et sans overhead 2 octets pour
trouver une hauteur cohérente avec la taille du chunk. Sur les vrais walls
testés, l'overhead de 2 octets est nécessaire (STONEWALL : 8322 octets de
chunk, modulo 128 = 0 → textureHeight 64).

## 16C — Pipeline LEVEL_A → PNG

### Étapes

1. **Load** : `Files.readAllBytes(twolev.bin)` + `twolev.graph.bin` →
   `LevelLoader.load()` → `Level` instance.
2. **Render** : `LevelMapRenderer.render(lvl, 512, 512)` → `Framebuffer` 8-bit.
3. **Export** : `FramebufferPng.writePng(fb, palette, outPath)` → fichier PNG.

### `LevelMapRenderer`

Rendu top-down 2D avec :

- **Edges pleines** (joinZoneId = -1) en rouge
- **Edges joining** (zones connectées) en vert
- **Points/vertices** en blanc
- **Control points** en cyan (3×3 carrés)

Auto-scaling sur la bounding box des points avec une marge de 5%.

### Validation

Test `LevelMapPipelineTest.rendersRealLevelAToPng()` produit
`build/level_a_map.png` (≈11 kB) avec succès, validant le pipeline end-to-end.

## 16D — AI state machine core

### Référence ASM

`modules/ai.s:23-37` — `AI_MainRoutine` dispatch sur `EntT_CurrentMode_b` :

```
cmp.b   #1,EntT_CurrentMode_b(a0)
blt     ai_DoDefault            ; mode 0
beq     ai_DoResponse           ; mode 1

cmp.b   #3,EntT_CurrentMode_b(a0)
blt     ai_DoFollowup           ; mode 2
beq     ai_DoRetreat            ; mode 3

cmp.b   #5,EntT_CurrentMode_b(a0)
beq     ai_DoDie                ; mode 5

ai_DoTakeDamage:                ; mode 4 (ou autre)
```

### Classes Java

- **`AiState`** (enum) : 6 valeurs avec valeurs numériques fidèles ASM
  (DEFAULT=0, RESPONSE=1, FOLLOWUP=2, RETREAT=3, TAKE_DAMAGE=4, DIE=5).
- **`Entity`** : struct avec champs accédés par AI (`currentMode`, `hitPoints`,
  `timer1/2`, `whichAnim`, `displayText`, `zoneId`…).
- **`AiCore`** : dispatch + `receiveDamage()` (port de `ai_TakeDamage:`).

### `receiveDamage` — port fidèle ASM

```
damage = damageTaken >>> 2         // asr.w #2 d0
if (hitPoints <= damage)
    → DIE
else
    hitPoints -= damage
    r = rng.next() & 3             // and.w #3,d0
    if (r == 0)                    // beq.s .dodododo
        → TAKE_DAMAGE (anim 2)
    else
        → RESPONSE (charge joueur, anim 1)
```

### `Rng` — port de `GetRand` (`objectmove.s:1637-1644`)

```
state = (state ROL 3 + 0x2343) & 0xFFFF
```

Seed initial : 234 (compatible ASM). Tests vérifient la séquence par valeurs
calculées manuellement.

## 16E — Effets MOD complets

### Référence ASM

`modules/music.s:270-423` — table `mt_arpeggio_tab`, état par voice avec
period/volume.

### Effets portés

Ajout de 5 effets continus (per-tick) au `ProtrackerReplayer` existant :

| Cmd   | Nom               | Comportement                              |
|-------|-------------------|-------------------------------------------|
| `0x0` | Arpeggio          | period oscille entre base/base+semi1/base+semi2 |
| `0x1` | Pitch slide up    | period -= param chaque tick               |
| `0x2` | Pitch slide down  | period += param chaque tick (capé à 0x6B0)|
| `0x3` | Portamento        | period glisse vers target en steps        |
| `0x4` | Vibrato           | period modulé par table sine 32-byte      |
| `0xA` | Volume slide      | volume += up - down chaque tick           |

### `VoiceState` étendu

Ajout de champs pour state d'effets continus :

```java
int portTarget, portStep, portDirection;
int vibratoParam, vibratoPos;
int arpeggioParam, arpeggioTick, arpeggioBasePeriod;
```

### Table sine vibrato

Reconstituée depuis la documentation Protracker (`modules/music.s:474-476`) :
32 entrées quart de cycle, amplitude 0..255 :

```java
for (int i = 0; i < 32; i++) {
    VIBRATO_SINE[i] = (byte) Math.round(Math.sin(i * Math.PI / 32) * 255);
}
```

(L'usage de `Math.sin` ici est dans la phase d'init statique, pas dans le hot
path — conforme à la règle de port.)

### Tests

`ProtrackerEffectsTest` valide chaque effet :

- pitchSlideUp : period 856 → 852 → 848
- pitchSlideDown : period 856 → 860
- volumeSlide : volume 64 → 60 → 56
- portamento : glisse depuis 856 vers 480
- vibrato : modulation autour du basePeriod

Tous les tests passent (BUILD SUCCESSFUL).

## Bilan

- **Code ajouté** : 9 classes principales + 3 tests
- **PNG produit** : `build/level_a_map.png` (top-down du vrai LEVEL_A)
- **Tests** : tous verts (300+ tests cumulés)
- **Couverture** : pipeline data → rendu / state machine AI dispatch / effets
  audio temps réel — prêts pour intégration dans une boucle de jeu

## Prochains chantiers naturels

1. **Sub-systems AI handlers complets** : implémenter `handleDefault`,
   `handleResponse`, `handleFollowup`, `handleRetreat` (nécessitent
   `HeadTowardsAng`, structure `EntT_*` complète, animations).
2. **Renderer first-person depuis LEVEL_A** : assembler les zones + textures
   pour produire un vrai screenshot du jeu, pas une minimap.
3. **Boucle de jeu intégrée** : tick AI + replayer + render dans une boucle
   GLFW unique.
