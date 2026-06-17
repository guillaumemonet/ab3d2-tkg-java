# Sous-système 17 — AI mouvement, portes, joueur & rendu first-person

## Vue d'ensemble

Batch consolidant l'AI vers du comportement réel, la state machine des
portes/ascenseurs, le contrôle joueur avec collision, et un premier render
first-person depuis un vrai niveau.

| ID  | Livrable                                                     | Statut  |
|-----|--------------------------------------------------------------|---------|
| 17A | `HeadTowardsAng` + primitives math AI                        | done    |
| 17B | AI handlers complets (DEFAULT/RESPONSE/FOLLOWUP/RETREAT)     | done    |
| 17C | Doors/Lifts state machine                                    | done    |
| 17D | `Player` controls + collision                                | done    |
| 17E | First-person render LEVEL_A → PNG                            | done    |

## 17A — HeadTowardsAng

### Référence ASM

`ab3d2_source/objectmove.s:1024-1188`.

### Rôle

Calcule l'angle pour aller de `(oldx, oldz)` vers `(newx, newz)` en avançant
d'au plus `speed` unités par tick, en restant à au moins `range` unités de la
cible. Met à jour `newx`/`newz` en place avec la nouvelle position.

### Algorithme porté

1. `dx, dz` = delta vers la cible
2. `dist² = dx² + dz²` (32-bit)
3. Si `dist² == 0` → `gotThere = true`, sortie
4. `dist = sqrt(dist²)` par **Newton 3 itérations** (init = bit le plus haut de
   `dist²` divisé par 2, puis `x' = x - (x²-dist²)/(2x)`)
5. Si `dist <= range` → assez proche, pas de mouvement
6. Sinon → avance de `speed` le long du vecteur (capé à `dist`)
7. `sinRet/cosRet` = composantes proportionnelles à `dx/dist`, `dz/dist`
8. **Binary search 4 itérations** dans `SinCosTable` (cross-product
   `cos·sinRet - sin·cosRet`) pour trouver l'angle
9. `angRet = bestIndex * 2`

### Classe

`com.team17.ab3d2.ai.HeadTowardsAng` — globals ASM exposés en champs publics
(inputs : newx/newz/oldx/oldz/range/speed ; outputs : angRet/gotThere/
sinRet/cosRet/perpDist + newx/newz modifiés).

## 17B — AI handlers complets

### Référence ASM

`modules/ai.s:277-315` — les handlers sont eux-mêmes des **dispatchers** qui
branchent sur des sous-modes configurés au niveau du level.

### Découverte importante

`ai_DoRetreat:` est un simple `rts` dans l'original (`ai.s:277-278`) — porté
**verbatim** comme no-op (`handleRetreat`). `ai_DoDefault`/`ai_DoResponse`/
`ai_DoFollowup` dispatchent sur `AI_DefaultMode_w`/`AI_ResponseMode_w`/
`AI_FollowupMode_w`.

### Classes

- **`AiSubMode`** : 3 enums (`Default`, `Response`, `Followup`) avec mapping
  fidèle des comparaisons signées (`blt`/`beq`) de l'ASM.
- **`AiConfig`** : config level-wide (quels sous-modes les aliens adoptent).
- **`AiCore`** étendu : dispatch complet + sub-handlers (`aiProwlRandom`,
  `aiCharge`, `aiPauseBriefly`, etc.).

Les sub-handlers de mouvement complet (charge, approach) restent des stubs
documentés — ils dépendent de `PlayerState` + `Level` pour les coordonnées
cibles et seront branchés dans la boucle de jeu.

## 17C — Doors/Lifts state machine

### Référence ASM

State machine implicite dans `modules/level.s` + `newanims.s` autour de
`ZLiftableT` (`defs.i:604-623`), déjà parsé en `Liftable` (sub-system 2).

### Classes

- **`DoorState`** : enum explicite (CLOSED/OPENING/OPEN/CLOSING/BLOCKED).
- **`DoorRuntime`** : wraps un `Liftable`, position courante + state machine.

### Transitions portées

```
CLOSED  --trigger()-->  OPENING
OPENING --position>=top--> OPEN (openTimer = openDuration)
OPEN    --timer expiré + DL_TIMEOUT--> CLOSING
OPEN    --DL_NEVER--> reste OPEN
CLOSING --position<=bottom--> CLOSED
CLOSING --blocked--> BLOCKED --unblocked--> CLOSING
```

`trigger()` respecte `DR_NEVER` (porte verrouillée jamais ouvrable).
`isPassable()` : ≥ 25% ouverte (heuristique pour décision PVS).

## 17D — Player controls + collision

### Référence ASM

`modules/player.s::plr_KeyboardControl` (181+) + `MoveObject` d'`objectmove.s`.

### Classes

- **`PlayerInput`** (record) : input abstrait neutralisant la source
  (clavier/joy/AI/replay). Champs : `forward/strafe/turn` (∈ [-1,1]),
  `duck/fire/use`.
- **`PlayerController`** : traduit input → mutations `PlayerState` :
  - rotation (`angle += turn * TURN_STEP`, wrap modulo 8192)
  - duck toggle (cible `PLR_CROUCH_HEIGHT`/`PLR_STAND_HEIGHT`)
  - déplacement projeté selon angle (`sinw`/`cosw`) en forward + strafe
  - collision via `CollisionTester.tryMove` — mouvement rejeté si
    `BLOCKED_BY_WALL` (pas de sliding pour l'instant)

Réutilise `PlayerState`, `CollisionTester`, `PhysicsConstants` (sub-systems
antérieurs).

## 17E — First-person render LEVEL_A → PNG

### Pipeline end-to-end

1. **Load** : `LevelLoader.load(twolev.bin, twolev.graph.bin)`
2. **Spawn** : premier control point du level
3. **Transform** : pour chaque zone, chaque edge → `Transform3D.rotateFullScreen`
4. **Render** : projection verticale des hauteurs sol/plafond via `rotZ`,
   tracé wireframe des walls
5. **Export** : `FramebufferPng.writePng` → `build/level_a_first_person.png`

### Classe

`com.team17.ab3d2.render.FirstPersonRenderer` — wireframe (pas de PVS /
clipping / texturing, ces étapes viendront). Couleurs : solid wall blanc,
joining edge vert.

### Validation

- `rendersFirstPersonFromControlPoint` → PNG 320×240 (≈7.6 kB) sur LEVEL_A
- `rendersAtMultipleAngles` → rotation 360° en 8 directions sans crash

## Bilan

- **Classes ajoutées** : 9 (HeadTowardsAng, AiSubMode, AiConfig, DoorState,
  DoorRuntime, PlayerInput, PlayerController, FirstPersonRenderer + AiCore
  étendu)
- **Tests ajoutés** : 5 fichiers (HeadTowardsAng, AiHandlers, DoorRuntime,
  PlayerController, FirstPersonPipeline)
- **PNG produits** : `build/level_a_first_person.png` (wireframe FP)
- **Tous tests verts** (BUILD SUCCESSFUL)

## Prochains chantiers naturels

1. **Sliding-along-wall** dans `PlayerController` (le moteur projette le
   mouvement restant le long de l'edge en cas de collision — cf. la branche
   `.faraway`/`shove` de `HeadTowardsAng`).
2. **Zone tracking** : déterminer la zone courante du joueur après mouvement
   (passage par `ENTERS_ZONE`) pour mettre à jour `state.zoneId`.
3. **Brancher AiCore aux coordonnées joueur** : implémenter `aiCharge`/
   `aiApproach` réels avec `HeadTowardsAng`.
4. **Texturing du first-person render** : utiliser `WallTextureDecoder` +
   `WallSetup` pour remplir les walls au lieu du wireframe.
