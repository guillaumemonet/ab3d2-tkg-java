# Sous-systèmes 09–13 — Walker / Intégration / Gouraud / Physics / Audio

Cinq sous-systèmes livrés en un sprint combiné. Chacun est intentionnellement
**moins étendu** que les sous-systèmes individuels précédents — l'objectif
est d'établir une couverture large pour permettre l'intégration end-to-end
ultérieure.

**Status global** : ✅ 28 nouveaux tests (cumul total : **248/248**).
**Tâches** : #49 → #54

| # | Sous-système | Package | Tests |
|---|---|---|---|
| 09 | Zone graph walker | `com.team17.ab3d2.render` | 4 |
| 10 | Integration end-to-end | `com.team17.ab3d2.render` | 2 |
| 11 | Gouraud + dithering | `com.team17.ab3d2.render` | 3 |
| 12 | Player physics + collision | `com.team17.ab3d2.physics` | 10 |
| 13 | Audio Protracker replayer | `com.team17.ab3d2.audio` | 9 |

---

## Sous-système 09 — Zone graph walker

**Origine** : `modules/draw/draw_zone_graph.s` (440 lignes).

**Livré** :
- `ZoneGraphWalker.walkBackToFront(sortedZoneIds, leftClip, rightClip, renderer)`
  — itère les zones en marche-arrière (painter's back-to-front), invoque le
  callback `ZoneRenderer` pour chaque.
- `ZoneGraphWalker.reverseForPainter(frontToBack)` — utilitaire de tri.

**Simplifications** :
- Pas de format "graphics command stream" porté (les tags wall/floor/object
  sont délégués au callback) — l'engine ASM lit des items tagués depuis
  `Lvl_ZoneGraphAddsPtr_l` qu'on ne reproduit pas.
- Pas de `Zone_SetupEdgeClipping` raffiné per-edge.
- Pas de `Draw_ForceZoneSkip_b` (skip basé sur PVS runtime).
- Pas de zones bi-level (upper/lower splits).

**Tests** :
- `walkBackToFrontInvertsOrder` : [0,1,2] → callbacks dans l'ordre 2,1,0
- `walkPassesClipBoundsToRenderer` : les bornes sont passées au callback
- `emptyListProducesNoRenderCalls`
- `reverseForPainterReverses`

---

## Sous-système 10 — Integration end-to-end

**Livré** : un test d'intégration qui :
1. Crée un framebuffer 80×60 + palette identity
2. Place un joueur regardant +Z
3. Rend un sol uniforme (FloorSetup → FloorRasterizer)
4. Rend un mur (WallSetup → WallRasterizer)
5. Rend un sprite damier (ObjectSetup → ObjectRasterizer)
6. Vérifie que le framebuffer contient les 3 composants

Un second test valide l'intégration `ZoneGraphWalker` + callback de rendu.

**Importance** : ce test prouve que la chaîne complète `Transform3D → Setup →
Rasterizer` fonctionne. Pas de comparaison pixel-perfect avec l'original
(nécessiterait des dumps de référence), mais validation structurelle.

---

## Sous-système 11 — Gouraud + dithering

**Origine** : `hireswall.s` (`draw_WallGouraudShaded`), `modules/draw/draw_floor.s`
(`draw_GoraudFloor`), `modules/draw/draw_wall.s` (alternance dim/bright).

**Livré (ajouts aux rasterizers existants)** :

- `WallRasterizer.drawColumnGouraud(...)` — brightness interpolée per-pixel
  en 16.16 fixed-point via `brightStart` + `brightStep`. Le rang dans la
  shade table est extrait avec `(bright >>> 16) & 0x3F`.

- `WallRasterizer.drawColumnDithered(...)` — alterne entre deux rows
  (`brightRowBright` pour `y` pair, `brightRowDim` pour `y` impair). Reproduit
  le pattern `drawwalldimPACK0` / `drawwallPACK0` de l'ASM.

- `FloorRasterizer.drawScanlineGouraud(...)` — brightness interpolée
  horizontalement per-pixel. Reproduit le pattern `acrossscrngour` de
  `draw_floor.s` (sans toutefois unroller la boucle 4×).

**Tests** :
- `wallGouraudInterpolatesBrightness` : brightness 0→63 → couleurs croissantes
- `wallDitheredAlternatesBetweenRows` : y pair=row 10, y impair=row 50
- `floorGouraudInterpolatesAcrossScanline` : brightness 0→63 monotone

**Simplifications restantes** :
- Pas de variant 060-tuned (boucle unrolled)
- Pas de Gouraud combiné top/bot vertical sur les walls (le moteur a 4 brightness
  par mur : top-left, top-right, bot-left, bot-right)

---

## Sous-système 12 — Player physics + collision

**Origine** : `objectmove.s` (MoveObject), `fall.s` (gravity).

**Livré** :

### `PhysicsConstants`
- `PLR_STAND_HEIGHT = 12288` (`hires.s:55`)
- `PLR_CROUCH_HEIGHT = 8192`
- `STEP_UP_STAND = 10240` (40 × 256)
- `STEP_UP_CROUCH = 2560` (10 × 256)
- `GRAVITY = 128` (à calibrer vs original)
- `MAX_FALL_VELOCITY = 4096`
- `JUMP_INITIAL_VELOCITY = -1024` (Y inversé : négatif = vers le haut)
- `FALL_DAMAGE_THRESHOLD = 2048`

### `PlayerState` (mutable)
Sous-ensemble de `PlrT` pour la physique : `xOff/yOff/zOff`, `yVelocity`,
`height`/`targetHeight`, `angle`, `zoneId`, `ducked`, `squished`, `energy`.

### `CollisionTester.tryMove(...)`
Teste si un mouvement franchit un edge de la zone courante. Retourne :
- `FREE` : pas de collision
- `BLOCKED_BY_WALL` : solid wall franchi
- `ENTERS_ZONE` : adjacent zone (à gérer par le caller)

### `CollisionTester.canStepUp(fromFloor, toFloor, ducked)`
Vérifie que la différence de hauteur ne dépasse pas `STEP_UP_*`.

### `Gravity.applyGravity(yOff, yVelocity, floorY)`
Retourne `(yOff, yVelocity, damage, onGround)`. Applique gravité avec cap,
clamp au sol, calcule fall damage si vélocité > seuil.

### `Gravity.tryJump(velocity, onGround)`
Retourne `JUMP_INITIAL_VELOCITY` si au sol, sinon `velocity` inchangé.

**Simplifications** :
- Pas de sliding-along-edge en cas de collision
- Pas de jetpack (modulation de gravité)
- Pas de squish damage
- Pas de water gravity
- Pas de step-up/down détaillé (déjà partiellement géré par `canStepUp`)

**Tests** (10) :
- Constantes engine
- État par défaut joueur
- Collision solid wall détectée
- Adjacent zone crossing (vs solid)
- Step-up respecte limite crouched vs debout
- Gravité accélère
- Cap à MAX_FALL_VELOCITY
- Atterrissage trigger onGround
- Fall damage au-delà du seuil
- Saut seulement au sol

---

## Sous-système 13 — Audio Protracker replayer

**Origine** : `modules/music.s` (509 lignes).

**Livré** :

### `ModSample` (record, 30 octets binaire)
Nom, length (en mots × 2 = octets), finetune (signed nibble), volume (0..64),
repeat start, repeat length.

### `ModNote` (record, 4 octets binaire)
Parse les 4 octets d'une cellule MOD :
- byte 0 high nibble + byte 2 high nibble = sampleNumber (0..31)
- byte 0 low nibble + byte 1 = period
- byte 2 low nibble = effect command (0x0..0xF)
- byte 3 = effect parameter

### `ModFile` (record)
Conteneur : nom, 31 samples, songLength, sequence[128], patterns (1024 octets
chacun), sample data brute.

Méthode `noteAt(pattern, row, channel)` extrait une note typée.

### `ModParser.parse(byte[])`
Parser complet :
- 20 octets nom
- 31 × 30 octets sample headers
- 1 octet songLength + 1 octet restart
- 128 octets sequence
- 4 octets signature (M.K.)
- N × 1024 octets patterns (N = max(sequence[0..songLength-1]) + 1)
- Sample data PCM

### `ProtrackerReplayer`
État mutable des 4 voices Paula (sample, period, volume) + position courante
(songPos, row) + tickCounter + speed.

Méthode `tick()` à appeler à 50 Hz :
- Si `tickCounter == 0` : process row (applique notes + effets)
- Increment `tickCounter`
- Si `tickCounter >= speed` : advance row (avec pattern-break / position-jump
  deferred)

**Effets implémentés** :
- 0xC : set volume
- 0xF : set speed (param < 32 ; BPM mode non implémenté)
- 0xB : position jump (deferred)
- 0xD : pattern break (deferred, target row en décimal-comme-hex)

**Effets non implémentés** (à porter dans futur sub-system) :
- 0x0 arpeggio
- 0x1/0x2 pitch slide up/down
- 0x3 portamento
- 0x4 vibrato (avec table sine 32-byte)
- 0xA volume slide
- 0xE filter / sub-effects

### Pas implémenté
- **Génération audio réelle** : les voices maintiennent l'état (sample/period/
  volume) mais aucun PCM n'est généré. La couche audio externe (LWJGL OpenAL,
  Java Sound API) consommera l'état pour synthétiser.
- **DMA Paula control** (`DMACON` writes) : non simulé.
- **Sound effects mixing** sur canaux 3-4 : pas distingué de la musique.

**Tests** (9) :
- Tailles ModSample/ModNote/ModFile constantes
- Parsing d'une note depuis 4 octets
- ModParser construit un ModFile valide
- Replayer avance row après `speed` ticks
- Set speed effect change la cadence
- Set volume effect change le voice volume
- Replayer loope en fin de séquence
- Parser rejette les fichiers tronqués

---

## Récap cumulé global

| Sous-système | Tests | Statut |
|---|---|---|
| 01 — Math fondamental | 39 | ✅ |
| 02 — Loader binaires | 66 | ✅ |
| 03 — Gameplay state runtime | 24 | ✅ |
| 04 — Transform3D | 11 | ✅ |
| 05 — PvsErrata | 6 | ✅ |
| 06 — Edge PVS | 17 | ✅ |
| 07 — Renderer (fondations) | 38 | ✅ |
| 08 — Setup code des rasterizers | 19 | ✅ |
| 09 — Zone graph walker | 4 | ✅ |
| 10 — Integration end-to-end | 2 | ✅ |
| 11 — Gouraud + dithering | 3 | ✅ |
| 12 — Player physics + collision | 10 | ✅ |
| 13 — Audio Protracker | 9 | ✅ |
| **Total** | **248** | ✅ |

---

## Limitations cumulées à traiter dans le futur

Le port a une couverture large mais des points spécifiques restent simplifiés :

### Renderer
- Format de texture packed (PACK0/1/2) — currently unpacked
- Gouraud complet wall (4 brightness corners)
- Per-edge clipping refinement (`draw_set_clip.s`)
- Graphics command stream (tags wall/floor/object dans `Lvl_ZoneGraphAddsPtr_l`)
- C2P chunky → planar
- Backdrop/sky rendering
- Water animation
- Multi-CPU variants (`_060.s` optimisés)

### Physics
- Sliding along edge en cas de collision
- Jetpack, water gravity, squish damage
- Input acquisition (clavier raw, mouse, joystick, CD32)
- Snapshot/commit pattern (`Tmp` → `Snap` → live)

### Audio
- Génération PCM réelle (couplage à une lib audio Java)
- Effets complexes (arpeggio, slides, vibrato, portamento)
- Mixing SFX vs music
- DMA Paula control simulation

### Loader
- ControlPointCoords format précis
- Zone graph adds format
- Switches data stream
- Lift masks per-edge (pour PVS edge)

### Gameplay (non porté)
- AI state machine (`modules/ai.s`)
- Animation system (`newanims.s`)
- Weapon system (`newplayershoot.s`)
- Two-player serial sync (`serial_nightmare.s`)
- Door/lift state machine runtime
- Errata application en runtime

### Engine plumbing (non porté)
- Boucle principale (game_main_loop)
- VBL pacing
- IRQ handlers
- File I/O + LHA decompression
- Resource registry
- Screen buffer flipping
- Menu system

---

## Pour la suite

Le port couvre maintenant un **large périmètre** structurel. Pour la suite,
plusieurs directions possibles :

A) **Profondeur — pixel-perfect** : reprendre chaque sous-système et le porter
   plus fidèlement à l'ASM (format texture packed, effets MOD complets, etc.).

B) **Largeur — gameplay et input** : porter AI, animation, weapons, input
   acquisition pour avoir un gameplay jouable.

C) **Engine plumbing** : porter la boucle principale, VBL pacing, file I/O
   pour avoir un binaire exécutable end-to-end.

D) **Audio output** : intégrer LWJGL OpenAL pour la sortie audio réelle.

E) **Renderer display** : intégrer LWJGL pour afficher le framebuffer dans
   une fenêtre.

À décider à la prochaine étape — chaque direction est un projet substantiel
en soi.
