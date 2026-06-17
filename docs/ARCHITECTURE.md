# Alien Breed 3D II: The Killing Grounds — Architecture du moteur original

**Document de référence pour le portage Java de préservation.**
Cible : reproduction strictement identique au binaire Amiga original (pixel-perfect, frame-perfect, deterministe).

Référence canonique du comportement : les sources release Team17 telles que présentes dans `./ab3d2_source/` (lignée karlos-tkg) et les médias originaux dans `./medias/original/`.

Ce document ne contient **aucun** code Java. Il décrit le moteur d'origine. Toute traduction Java fera l'objet d'un livrable séparé, sous-système par sous-système, après accord explicite.

---

## Table des matières

0. [Convention et terminologie](#0-convention-et-terminologie)
1. [Cartographie globale du moteur](#1-cartographie-globale-du-moteur)
2. [Conventions numériques et types fondamentaux](#2-conventions-numériques-et-types-fondamentaux)
3. [Boucle principale et pipeline par frame](#3-boucle-principale-et-pipeline-par-frame)
4. [Pipeline de rendu](#4-pipeline-de-rendu)
5. [Système de zones, carte, PVS](#5-système-de-zones-carte-pvs)
6. [Sous-systèmes gameplay](#6-sous-systèmes-gameplay)
7. [Audio, vidéo, interactions matériel Amiga](#7-audio-vidéo-interactions-matériel-amiga)
8. [Formats des médias et données disque](#8-formats-des-médias-et-données-disque)
9. [Points critiques pour le pixel-perfect & risques de divergence](#9-points-critiques-pour-le-pixel-perfect--risques-de-divergence)
10. [Inconnues, hypothèses, à explorer](#10-inconnues-hypothèses-à-explorer)

---

## 0. Convention et terminologie

- **m68k** : famille processeurs Motorola 680x0 (cibles 68030, 68040, 68060). Instructions citées en syntaxe Motorola/vasm.
- **Registres** : `d0..d7` (données 32-bit), `a0..a7` (adresses, `a7` = SP), `pc`, `sr`/`ccr`. Convention `move.b/w/l` = byte/word/long. `swap dN` échange les deux mots d'un long.
- **Chunky** : framebuffer linéaire à 1 octet par pixel (index palette). **Planar** : représentation Amiga native, N plans de bits parallèles.
- **C2P** : chunky-to-planar conversion (logicielle CPU ou via puce Akiko).
- **PVS** : Potentially Visible Set — ensemble des zones potentiellement visibles depuis une zone donnée.
- **Zone** : polygone convexe 3–10 côtés du monde 2.5D (cf. `docs/PVS.md`).
- **Edge** : arête d'une zone, peut être mur plein, joint vers une autre zone, porte ou ascenseur.
- **Fixed-point** : arithmétique en virgule fixe — pas de flottant dans le moteur sauf init.
- **GLF** : Game Link File — base de données globale (armes, balles, aliens, objets, animations, palettes ; cf. structure `GLFT` dans `defs.i`).
- **TKG** : The Killing Grounds (sous-titre d'AB3D II).
- **AGA** : Advanced Graphics Architecture (Amiga 1200/4000) — DMA planar, palette 24-bit.
- **RTG** : Re-Targetable Graphics — cartes graphiques tierces (Cybergraphics) en mode chunky framebuffer.
- **Akiko** : ASIC C2P matériel sur CD32 et certaines extensions.
- Pour les références de fichiers : `chemin/relatif.ext:ligne` ou `chemin/relatif.ext`.

---

## 1. Cartographie globale du moteur

### 1.1 Organisation des sources

```
ab3d2_source/
├── hires.s                # POINT D'ENTRÉE RÉEL (_startup), include monolithique
├── controlloop.s          # Boucle outer : menu/title/level transitions, Game_Start, Game_Begin
├── orderzones.s           # Tri des zones (painter's algorithm)
├── plr1control.s,         # Contrôle joueur 1 & 2
│   plr2control.s
├── newplayershoot.s       # Tir joueur, projectiles, ammo
├── newaliencontrol.s      # AI top-level dispatch
├── newanims.s             # Animation des sprites / brightness anim
├── objectmove.s           # Mouvement & collision des objets (joueur + monstres + projectiles)
├── objdrawhires.s         # Rendu des sprites/objets 3D
├── hireswall.s            # Code mur (largement remplacé par modules/draw/draw_wall*.s)
├── hiresgourwall.s        # Code Gouraud (encore utilisé pour les sols)
├── fall.s                 # Gravité, chute, saut
├── pauseopts.s            # Logique de pause
├── titlecop.s             # Copperlist du titre (incbin "includes/newtitlepal")
├── cd32joy.s              # Lecture pad CD32 (ReadJoyPort, lowlevel.library)
├── serial_nightmare.s     # Multijoueur série
├── ab3diipatchidr.s       # Patch d'Intuition (EasyRequestArgs neutralisé)
├── defs.i, system.i,      # Includes globaux : équates, structures, macros
│   macros.i, funcdef.i
│
├── bss/                   # Sections .bss (variables non initialisées)
│   ├── system_bss.s, io_bss.s, vid_bss.s
│   ├── level_bss.s, ai_bss.s, anim_bss.s
│   ├── player_bss.s, draw_bss.s
│   ├── zone_bss.s, tables_bss.s, game_bss.s
│
├── data/                  # Sections .data (variables initialisées)
│   ├── system_data.s (inclut version.i généré par bumprev.py)
│   ├── draw_data.s, level_data.s
│   ├── tables_data.s  (contient incbin "bigsine" → SinCosTable_vw)
│   ├── text_data.s, vid_data.s, game_data.s
│
├── modules/               # Modules « propres » (extraction progressive depuis hires.s)
│   ├── system.s           # Init lib, IRQ vectors, EClock, FPS
│   ├── file_io.s          # IO_QueueFile, IO_FlushQueue, décompression LHA (decomp4.raw)
│   ├── res.s              # Chargement ressources (Res_LoadObjects, Res_LoadSoundFx…)
│   ├── level.s            # Level loading & data unpacking
│   ├── ai.s               # AI state machine (AI_MainRoutine, ai_DoDefault…)
│   ├── player.s           # plr_KeyboardControl, plr_MouseControl
│   ├── music.s            # Replayer Protracker custom
│   ├── vid.s              # Setup vidéo, ajustement gamma/brightness/contraste
│   ├── transform.s        # Transformation 3D des points
│   ├── draw.s             # Glue rendu (Draw_Crosshair, DrawDisplay…)
│   ├── draw/              # Rendu : walls, floors, map, zone graph, clip
│   │   ├── draw_map.s, draw_zone_graph.s, draw_set_clip.s
│   │   ├── draw_wall.s, draw_wall_060.s
│   │   ├── draw_floor.s, draw_floor_060.s
│   ├── c2p/               # Chunky-to-planar par CPU
│   │   ├── c2p.s, 68030/, 68040/, akiko/, teleport_fx/
│   ├── dev_inst.s, dev_macros.i, dev_memtrack.s    # outils dev (ZONE_DEBUG, MEMTRACK)
│   ├── rawkey_macros.i                              # Macros pour KeyMap_vb
│
├── menu/                  # Menu / titre
│   ├── menunb.s           # Code menu (mnu_start, mnu_loop, mnu_printxy)
│   ├── back.pal, back2.raw, credits_only.raw, firepal.pal2,
│   │   font16x16.pal2, font16x16.raw2
│
├── c/                     # C shim — initialisation, prefs, achievements, PVS edge/door
│   ├── main.c, system.c, screen.c
│   ├── game.c, game.h, game_preferences.c,
│   │   game_progress.c, game_properties.c
│   ├── menu.c, menu.h, message.c, message.h
│   ├── draw.c, draw.h, draw_inline.h, math25d.h
│   ├── zone.h, zone_inline.h, zone_liftable.h,
│   │   zone_edge_pvs.c, zone_errata.c,
│   │   zone_liftable_pvs.c, zone_debug.c
│   ├── defs.h (mirror partiel de defs.i),
│   │   asm_align.h, multiplayer.h, key_defs.h,
│   │   player.h
│   ├── prefs_*.h, prefs_dev.h, prefs_gfx.h, prefs_keys.h,
│   │   prefs_misc.h, prefs_vid.h
│
├── docs/PVS.md, docs/README.md  # Notes Team17/karlos
└── decomp4.raw                  # Binaire dépacker LHA, embarqué via incbin
```

### 1.2 Frontière C ↔ ASM

- `main.c::main()` ouvre les libs Amiga, lit `SCREENMODE/N`, détecte RTG, puis appelle `startup()` (qui est `_startup` dans `hires.s`).
- **Toute la logique temps-réel est en ASM**. Le C n'est utilisé que pour :
  - Boot/teardown et libs (`Sys_OpenLibs`, `Sys_CloseLibs` dans `system.c`).
  - Préférences (`game_preferences.c`), progression/save (`game_progress.c`), achievements (`game_properties.c`).
  - PVS preprocessing au level load (`zone_edge_pvs.c`, `zone_errata.c`, `zone_liftable_pvs.c`).
  - Petites helpers de dessin et UI (`draw.c`, `menu.c`, `message.c`).
- Convention de liaison : l'ASM référence les symboles C avec un préfixe `_` (ex. `_Vid_Present`, `_Plr1_Control`, `_Zone_OrderZones`). Le C déclare l'ASM via `extern void startup(void);`.
- Les structures partagées C↔ASM sont définies **deux fois** : dans `defs.i` (côté ASM, via macros `STRUCTURE`/`ULONG`/`UWORD`/`UBYTE`/`STRUCT`/`LABEL`/`PADDING`) et dans `defs.h` + `player.h` + `zone.h` (côté C). Les offsets doivent rester strictement identiques byte-à-byte. Un commentaire en tête de `defs.i` rappelle : « DO NOT EDIT THIS FILE WITHOUT MAKING THE CORRESPONDING CHANGES IN c/defs.h ».
- Le C utilise des macros `REG(d0, …)` (de `SDI_compiler.h`) pour spécifier les conventions d'appel par registres.

### 1.3 Build flavors et CPU

- 3 flavors × 3 CPUs = 9 binaires :
  - Flavors : `dev` (debug + `-DDEV -DZONE_DEBUG`), `test` (release + symbols), `release`.
  - CPUs : 68030, 68040, 68060.
- Définitions `-DOPT040`, `-DOPT060` activées par le Makefile via `AFLAGS`/`CFLAGS`.
- Quand ni `OPT040` ni `OPT060` ne sont définis, `hires.s` définit `CPU_ALL` ce qui amène les chemins fallback (68030 + Akiko C2P).
- Le moteur a en plus une **détection runtime** dans `system.c` :
  - `Sys_Move16_b` (vrai sur 040/060)
  - `Sys_CPU_68060_b`, `Sys_CPU_68030_b`
  - `Sys_C2P_Akiko_b` (vrai si lecture de `AKIKO_IDENT_ADDR = 0xB80002` réussie et `GfxBase` version ≥ 40)
  - Permet à un binaire 030 de basculer sur Akiko si présent.

### 1.4 Vue d'ensemble des sous-systèmes

| Sous-système | Fichiers principaux | Rôle |
|---|---|---|
| Boot / Init | `hires.s` (_startup), `main.c`, `system.c`, `modules/system.s` | Ouvre libs, alloue buffers, charge GLF, installe IRQ |
| Boucle outer | `controlloop.s` (Game_Start), `menu/menunb.s`, `c/menu.c` | Titre, menus, sélection de niveau, sauvegardes |
| Boucle inner | `hires.s` (game_main_loop), `pauseopts.s` | Boucle de gameplay par frame |
| Input | `modules/player.s`, `cd32joy.s`, `key_interrupt` (asm), `KeyMap_vb` | Clavier (IRQ ports), souris (potgo), joystick, CD32 |
| Player | `plr1control.s`, `plr2control.s`, `c/player.h`, `bss/player_bss.s` | État joueur, vue, snapshot/commit input |
| Mouvement / collision | `objectmove.s`, `fall.s` | MoveObject, sliding, step-up, gravité |
| Armes / tir | `newplayershoot.s` | Hitscan, balistique, cooldown, ammo |
| Objets / projectiles | `objectmove.s`, `objdrawhires.s`, `defs.i` (ObjT/EntT/ShotT) | Mise à jour & rendu sprites |
| AI | `newaliencontrol.s`, `modules/ai.s`, `bss/ai_bss.s` | State machine alien (default/response/followup/retreat/damage/die) |
| Animation | `newanims.s`, `bss/anim_bss.s` | Frames sprites, lights, water |
| Zones / Map / PVS | `modules/level.s`, `c/zone_edge_pvs.c`, `c/zone_errata.c`, `c/zone_liftable_pvs.c`, `orderzones.s`, `docs/PVS.md` | Géométrie, visibilité, portes, ascenseurs |
| Rendu 3D | `modules/transform.s`, `modules/draw/*`, `objdrawhires.s`, `hiresgourwall.s` | Projection, walls, floors, objects, clipping |
| C2P | `modules/c2p/*` | Chunky → planar (CPU-spécifique ou Akiko) |
| Vidéo | `modules/vid.s`, `c/screen.c`, `hires.s` (display setup) | Mode écran, double buffer, copperlist double-height, palette, gamma |
| Copperlist | `titlecop.s`, `c/screen.c` (UCopList) | Title screen, modulo switch double-height |
| Audio music | `modules/music.s` (mt_init, mt_music, mt_speed) | Replayer Protracker custom, 4 canaux Paula |
| Audio SFX | `modules/res.s` (Aud_SampleList_vl), Paula directe | 59 SFX, déclenchement event-driven |
| Tables | `data/tables_data.s`, `bss/tables_bss.s` | SinCosTable, ConstantTable, DivThree, Side/Bright tables |
| HUD | `modules/draw.s` (Draw_Crosshair…) | Crosshair, FPS, ammo, health |
| Fichiers / décompression | `modules/file_io.s` (IO_QueueFile, unLHA, Fibonacci PCM) | DOS Open/Read + LHA in-memory + delta Fibonacci pour samples |
| Ressources | `modules/res.s` | Indexation Aud_SampleList_vl, draw_ObjectPtrs_vl, etc. |
| Multijoueur série | `serial_nightmare.s`, `c/multiplayer.h`, plr2control.s | SERSEND/SERREC, lock-step master/slave |
| Menu | `menu/menunb.s`, `c/menu.c` | Titre, options, sélection niveau, sélection difficulté |
| Préférences | `c/game_preferences.c`, `c/prefs_*.h` | Sensibilité, gamma, keys, oz_sensitivity |

### 1.5 Modèle mémoire global (BSS)

Les blocs BSS sont définis dans `bss/*_bss.s` et inclus en tête de `hires.s` dans cet ordre :
`system_bss → io_bss → vid_bss → level_bss → ai_bss → anim_bss → player_bss → draw_bss → zone_bss → tables_bss → game_bss`.
Suivis des blocs initialisés `data/*_data.s` puis `section .text,code`.

Les buffers majeurs (cf. `bss/tables_bss.s`) :

| Buffer | Taille | Usage |
|---|---|---|
| `SinCosTable_vw` | 8192 mots = 16 KB | Trigonométrie (chargé depuis `bigsine`) |
| `ConstantTable_vl` | 16384 longs = 64 KB | Table de division/réciproque construite au boot |
| `Rotated_vl` | 2×800 longs = 6.4 KB | Coordonnées rotées (X, Z scaled) pour 800 points |
| `OnScreen_vl` | 2×800 longs = 6.4 KB | Projection écran |
| `CurrentPointBrights_vl` | 2×256×10 longs = 20 KB | Brightness par point |
| `LeftSideTable_vw`, `RightSideTable_vw` | 2×512 mots chacun = 2 KB chacun | Edges murs précalculés |
| `LeftBrightTable_vw`, `RightBrightTable_vw` | 2×512 mots chacun = 2 KB chacun | Brightness murs |
| `KeyMap_vb` | 256 bytes | État clavier raw |
| `Aud_SampleList_vl` | 59 × 8 bytes | Adresses début/fin samples SFX |
| `anim_LiftHeightTable_vw` | 40 mots | Hauteurs lifts |
| `anim_DoorOpenTimers_vw` | 40 mots | Timers portes |
| `Obj_RoomPath_vw` | 100 mots | Path zones courantes |
| `Vid_Screen1Ptr_l`, `Vid_Screen2Ptr_l` | 2 framebuffers chunky 320×256 = 81 920 octets chacun | Double buffer |

---

## 2. Conventions numériques et types fondamentaux

### 2.1 Système de coordonnées monde

- **Repère 2.5D** : X = est/ouest (+ vers l'est), Z = nord/sud (+ vers le nord), Y = altitude **inversée** (+ vers le sol, plus la valeur est petite plus c'est haut). Cf. `docs/README.md` et `zone.h:43` : `#define DISABLED_HEIGHT 5000` (un niveau désactivé est "à 5000 de profondeur").
- **Échelle** :
  - 32 unités Y = 1 mètre (vertical).
  - 64 unités X/Z = 1 mètre (horizontal — résolution double sur le plan).
  - Texture murs : 128 Y × 256 X/Z = un tile.
  - Texture sols/plafonds : 64×64 toujours, échelle X/Z (tile deux fois plus serré qu'un mur de 64 de large).
- **Hauteur joueur** :
  - Debout : `PLR_STAND_HEIGHT = 12*1024 = 12288` (≈ 48 unités Y soit 1.5 m).
  - Accroupi : `PLR_CROUCH_HEIGHT = 8*1024 = 8192` (≈ 32 unités Y soit 1.0 m).
- Positions joueur stockées en **LONG** (`PlrT_XOff_l`, `_YOff_l`, `_ZOff_l`) avec une partie fractionnaire : interprétation `16.16` ou similaire (la doc Team17 reste imprécise ; le moteur les accède parfois comme `.w` aussi — cf. commentaires `TODO understand real size` dans `defs.i`).

### 2.2 Système angulaire

**Vérité de référence** : `c/math25d.h` + `ab3d2_source/docs/README.md`.

- `SINTAB_SIZE = 8192`. Table `SinCosTable_vw[8192]` (`WORD`, signed 16-bit). Chargée via `incbin "bigsine"` depuis `data/tables_data.s`.
- Convention des points cardinaux (`docs/README.md`) :

  | Direction | Angle | sin | cos |
  |---|---|---|---|
  | Nord | 0 | 0 | 32767 |
  | Est | 2048 | 32767 | 0 |
  | Sud | 4096 | 0 | −32767 |
  | Ouest | 6144 | −32767 | 0 |

  Le cycle complet vaut donc **8192 unités angulaires** (et non 4096 comme indiqué erronément en une phrase du README ; les directions le contredisent — voir §10 « Inconnues »).

- Macros C (`math25d.h:43-49`) :
  ```c
  sinw(a) = SinCosTable_vw[(a & 0x1FFE) >> 1];
  cosw(a) = SinCosTable_vw[((a + 2048) & 0x1FFE) >> 1];
  ```
  - Le masque `0x1FFE` (= `SINTAB_SIZE - 2`) écrase le bit 0 de l'angle.
  - Après `>>1`, l'index est compris dans `[0..4095]`. Seules les 4096 premières entrées de la table sont effectivement lues par `sinw` ; `cosw` ajoute 2048 (un quart de cycle) avant masquage. La taille « double » du tableau (8192) est conservée probablement pour absorber des accès non-modulo côté ASM, et/ou pour des artefacts historiques. Le port doit charger la table telle quelle depuis `bigsine`.
- Côté ASM : accès en bytes (`add.w angle,a0` après `lea SinCosTable_vw,a0`), ce qui correspond aux mêmes index puisque chaque entrée est 2 octets.
- **Implication port** : implémenter `sinw`/`cosw` strictement par lookup ; **interdire** `Math.sin`/`Math.cos` en hot path. Le `bigsine` doit être chargé tel quel (binaire 16384 octets).

### 2.3 Fixed-point

- **Positions monde** (XOff/YOff/ZOff) : 32-bit ; partie entière = 16 bits "monde", partie fractionnaire pour sub-step ; le moteur opère parfois en `.w` (poids fort uniquement).
- **Trig** : sin/cos = `int16_t`, échelle `Q15` (max ≈ 32767).
- **Rotation des points** (`modules/transform.s:47-76`) :
  ```
  viewX = pointX - Plr_XOff
  viewZ = pointZ - Plr_ZOff
  tx = viewX * cos - viewZ * sin
  tz = viewX * sin + viewZ * cos
  ; mise à l'échelle :
  x' = (tx << 16) >> 8        ; soit ~(tx * 256), conservé en 32-bit
  z' = (tz << 16) >> [8..15]  ; en fullscreen, multiplication par 1229 puis >>11 (facteur aspect 3/5)
  ```
- **Projection** (`transform.s:92-93`) : `screenX = divs.w z, tx + Vid_CentreX_w`. Division 32/16 signée Motorola — quotient en `.w`.
- **Reciprocals précalculés** (`ConstantTable_vl`, construit dans `hires.s:151-170`) : pour chaque diviseur `n` de 1 à 8192, deux longs sont stockés :
  - L1 = `16384 * 64 / n`
  - L2 = `64 * 64 * 65536 / n`
  Permet de remplacer une division par un `muls + asr.l`.
- **OneOverN** : utilisée dans le rendu sols pour `1/Y_écran` (avoidance de la division per-pixel) — cf. `objdrawhires.s:16-24` qui explicite la macro `mul; asr.l #14`.
- **Implication port** : reproduire les overflows et précisions exactement. Préférer `int`/`long` (Java) ; jamais de `double` dans une boucle pixel.

### 2.4 Structures fondamentales (offsets canoniques)

Tous les offsets viennent de `defs.i`. Toute modification doit être propagée dans `c/defs.h`/`c/player.h`/`c/zone.h`. Les tailles sont **fixes** — ne pas réordonner les champs en Java.

#### 2.4.1 `PlrT` — joueur runtime (taille totale ≈ 240 + tables)

Sections principales (extraits ; cf. `defs.i:37-137`) :

```
LONGS:
  0   PlrT_ObjectPtr_l           Pointeur vers l'entité associée
  4   PlrT_XOff_l                X monde (peut être accédé en .w)
  8   PlrT_YOff_l                Y monde
 12   PlrT_ZOff_l                Z monde
 16   PlrT_ZonePtr_l             Zone actuelle (pointeur ZoneT)
 20   PlrT_Height_l              Hauteur de vue (12*1024 ou 8*1024)
 24   PlrT_AimSpeed_l            Vitesse de visée verticale
 28-60 PlrT_Snap*_l              "Snapshot" : positions/vitesses committées
 64-72 PlrT_Tmp*_l               Buffers temporaires (input non encore committed)
 76   PlrT_ListOfGraphRoomsPtr_l Pointeur vers la liste des zones visibles
 80   PlrT_PointsToRotatePtr_l   Pointeur vers les points à transformer cette frame
 84   PlrT_BobbleY_l             Décalage Y du head-bob
 92-100 PlrT_OldX_l, _OldZ_l,    Position frame précédente
        _OldRoomPtr_l
 104  PlrT_SnapSquishedHeight_l
 108  PlrT_DefaultEnemyFlags_l

WORDS:
 112  PlrT_Energy_w              Santé "barre" (max 191)
 114-116 _CosVal_w, _SinVal_w    sin/cos précalc de l'angle
 118  PlrT_AngPos_w              Angle de vue (unités 0..8191)
 120  PlrT_Zone_w                Zone ID courant
 122  PlrT_FloorSpd_w            Vitesse footstep
 124  PlrT_RoomBright_w          Brightness ambiante (10 pts moyennés, HUD)
 126  PlrT_Bobble_w              Phase head-bob
 128-130 PlrT_SnapAngPos_w,      Snapshot angle + vitesse rotation
        _SnapAngSpd_w
 132  PlrT_TmpAngPos_w
 134  PlrT_TimeToShoot_w         Cooldown de tir (frames)

SAVE-GAME SECTION (commence à 136) :
 136  PlrT_Health_w
 138  PlrT_JetpackFuel_w
 140  PlrT_AmmoCounts_vw[20]     Munitions par type de balle
 180  PlrT_Shield_w
 182  PlrT_Jetpack_w
 184  PlrT_Weapons_vb[10]        Possession (en WORD malgré le suffixe _vb)
 204  PlrT_GunFrame_w
 206  PlrT_NoiseVol_w

PRIVATE WORDS :
 208  PlrT_TmpHoldDown_w         Durée maintien fire (charge weapons)
 210  PlrT_TmpBobble_w
 212-214 PlrT_SnapCosVal_w,      Snapshot trig
        PlrT_SnapSinVal_w
 216  PlrT_WalkSFXTime_w

BYTES :
 218  PlrT_Keys_b                clavier activé ?
 219  PlrT_Path_b
 220  PlrT_Mouse_b               souris activée ?
 221  PlrT_Joystick_b
 222  PlrT_GunSelected_b         arme courante (0..9)
 223  PlrT_StoodInTop_b          dans la moitié haute d'une zone bi-level ?
 224  PlrT_Ducked_b              accroupi ?
 225  PlrT_Squished_b
 226  PlrT_Echo_b
 227  PlrT_Fire_b
 228  PlrT_Clicked_b
 229  PlrT_Used_b
 230-233 PlrT_Tmp{Clicked,SpcTap,GunSelected,Fire}_b
 234  PlrT_Teleported_b
 235  PlrT_Dead_b
 236  PlrT_TmpDucked_b
 237  PlrT_StoodOnLift_b
 238  PlrT_InvMouse_b
 239  PlrT_Reserved2_b

TABLES :
 240  PlrT_ObjectDistances_vw[MAX_LEVEL_OBJ_DIST_COUNT=288]  (576 octets)
 816  PlrT_ObjectsInLine_vb[MAX_OBJS_IN_LINE_COUNT=400]      (400 octets)
TOTAL ≈ 1216 octets
```

Le pattern « Tmp → Snap → live » est central : l'**input et l'IA travaillent sur le buffer Tmp**, puis `Plr_Control()` **commit** au début de la phase physique (snapshot consistant pour le rendu). Cela permet un double buffering logique pour le serial multiplayer (échange Tmp côté slave/master).

#### 2.4.2 `ObjT` / `EntT` / `ShotT` — objets runtime (64 octets fixes)

`defs.i:220-314`. **Tous les objets actifs sont des structures de 64 octets** quelle que soit leur nature. Header commun (18 octets) :

```
  0   ObjT_XPos_l                X monde
  4   ObjT_ZPos_l                Z monde
  8   ObjT_YPos_l                Y monde
 12   ObjT_ZoneID_w              Zone (-1 = supprimé)
 14   (2 octets inconnus)
 16   ObjT_TypeID_b              OBJ_TYPE_ALIEN/OBJECT/PROJECTILE/AUX/PLAYER1/PLAYER2
 17   ObjT_SeePlayer_b           bitmap LOS vers joueurs
```

Le contenu après 18 dépend de `TypeID` :
- `EntT` (alien/objet destructible) : HP, dégâts pris, mode AI, control point cible, angle, timers (1..4), enemy flags, position d'impact, vélocité Y, type d'entité, n° d'animation.
- `ShotT` (projectile) : `VelocityX/Y/Z`, puissance, status, taille, gravité, lifetime, flags, "worry", "in upper zone".
- L'union des 46 octets restants entre `EntT` et `ShotT` est gérée par offsets *à la main*. **Aucun champ ne doit être déplacé** car les deux vues coexistent en mémoire.

Constantes :
- `OBJ_TYPE_ALIEN = 0`, `OBJECT = 1`, `PROJECTILE = 2`, `AUX = 3`, `PLAYER1 = 4`, `PLAYER2 = 5`.
- `ENT_TYPE_COLLECTABLE = 0`, `ACTIVATABLE = 1`, `DESTRUCTABLE = 2`, `DECORATION = 3`.
- Macros `OBJ_PREV/NEXT`, `ENT_PREV_2/PREV/NEXT/NEXT_2` permettent l'itération.

#### 2.4.3 `ZoneT` — zone (50 octets de header + PVS variable)

`defs.i:318-340` + `c/zone.h:84-106`.

```
  0   ZoneT_ID_w           (commentaire dit "2,2" — alignement bizarre — voir TODO dans defs.i)
  2   ZoneT_Floor_l        sol bas (4 octets, possible alignement 2)
  6   ZoneT_Roof_l         plafond bas
 10   ZoneT_UpperFloor_l   sol haut (zones bi-level)
 14   ZoneT_UpperRoof_l    plafond haut
 18   ZoneT_Water_l        niveau de l'eau
 22   ZoneT_Brightness_w
 24   ZoneT_UpperBrightness_w
 26   ZoneT_ControlPoint_w (UBYTE[2])
 28   ZoneT_BackSFXMask_w
 30   ZoneT_Unused_w
 32   ZoneT_EdgeListOffset_w     OFFSET NEGATIF vers la liste d'edges (précédant la struct)
 34   ZoneT_Points_w             OFFSET NEGATIF vers la liste de points
 36   ZoneT_DrawBackdrop_b       (sky enable)
 37   ZoneT_Echo_b
 38   ZoneT_TelZone_w            destination de téléport (-1 = pas téléporteur)
 40   ZoneT_TelX_w
 42   ZoneT_TelZ_w
 44   ZoneT_FloorNoise_w         (footstep / damage tile)
 46   ZoneT_UpperFloorNoise_w
 48   ZoneT_PotVisibleZoneList_vw  liste de longueur variable terminée par -1
```

**Layout disque** :
```
[edges list (words)]  [ZoneT structure]  [PVS records ZPVSRecord[]]
                           ^
                       pointeur Zone
```
- `z_EdgeListOffset` est *négatif* : `edges = ((WORD*)zone) + z_EdgeListOffset/2`.
- `z_Points` idem.
- Implication port : il faut soit conserver ce layout en mémoire, soit le « déplier » en pointeurs explicites lors du chargement (à privilégier en Java).

Constantes :
- `LVL_MAX_ZONE_COUNT = 256` (`LVL_EXPANDED_MAX_ZONE_COUNT = 512` non encore actif).
- `LVL_MAX_DOOR_ZONES = 16`, `LVL_MAX_LIFT_ZONES = 16`.
- `PVS_TRAVERSE_LIMIT = 100`, `EDGE_TRAVERSE_LIMIT = 16`.

#### 2.4.4 `EdgeT` (16 octets) — `defs.i:344-354`

```
  0   EdgeT_XPos_w        coord X point de départ
  2   EdgeT_ZPos_w        coord Z point de départ
  4   EdgeT_XLen_w        delta X
  6   EdgeT_ZLen_w        delta Z
  8   EdgeT_JoinZone_w    zone adjacente (-1 = mur plein)
 10   EdgeT_Word_5        TODO
 12   EdgeT_Byte_12       TODO
 13   EdgeT_Byte_13       TODO
 14   EdgeT_Flags_w       TODO (porte / ascenseur / etc.)
```

#### 2.4.5 `ZPVSRecord` / `PVST` (8 octets) — `defs.i:356-361` + `zone.h:55-60`

```
  0   pvs_ZoneID    -1 = fin de liste, -2 = retiré manuellement (errata), -3 = retiré automatiquement
  2   pvs_ClipID    ID du clip d'edge à utiliser
  4   pvs_Word2     TODO
  6   pvs_Word3     TODO
```

Constantes (`zone.h:32-37`) :
- `ZONE_ID_LIST_END = -1`
- `ZONE_ID_REMOVED_MANUAL = -2`
- `ZONE_ID_REMOVED_AUTO = -3`
- `EDGE_POINT_ID_LIST_END = -4`

#### 2.4.6 `BulT` (300 octets) — définition d'une balle/projectile

`defs.i:140-158`. Champs : `IsHitScan`, `Gravity`, `Lifetime`, `AmmoInClip`, `BounceHoriz/Vert`, `HitDamage`, `ExplosiveForce`, `Speed`, `AnimFrames`, `PopFrames`, `BounceSFX`, `ImpactSFX`, `GraphicType`, `ImpactGraphicType`, puis 120 octets d'`AnimData_vb` et 120 octets de `PopData_vb`. Indexé via `Plr_GunSelected_b` puis par le `ShootDefs` du GLF.

#### 2.4.7 `ShootT` (8 octets), `AlienT` (42 octets), `ODefT` (40 octets)

- `ShootT` (`defs.i:161-166`) : `BulType_w`, `Delay_w` (cooldown), `BulCount_w` (consommation ammo), `SFX_w`.
- `AlienT` (`defs.i:169-191`) : 21 mots définissant le comportement par mode (Default/Response/Followup/Retreat) avec `Behaviour`/`Speed`/`Timeout` pour chacun, + `BulType`, `HitPoints`, `Height`, `Girth`, `SplatType`, `Auxilliary`, `ReactionTime`, `DamageToRetreat`, `DamageToFollowup`.
- `ODefT` (`defs.i:194-209`) : définition d'objet — `Behaviour`, `GFXType`, `HitPoints`, `CollideRadius`, `CollideHeight`, etc.

#### 2.4.8 `GLFT` — Game Link File (master DB)

`defs.i:384-418`. Structure énorme et fixe, ordre canonique. **Taille totale : 86 268 octets** (corrigé après vérification par sous-système 02 ; cf. `docs/PORT_SUBSYS_02_IO.md` §4.1) :

```
+   0  (skip 64) ← header non documenté (signature/version)
+  64  LevelNames        16 × 40                  = 640      → 704
+ 704  ObjGfxNames       30 × 64                  = 1920     → 2624
+2624  SFXFilenames      64 × 60                  = 3840     → 6464
+6464  FloorFilename     64                                  → 6528
+6528  TextureFilename   192                                 → 6720
+6720  GunGFXFilename    64                                  → 6784
+6784  StoryFilename     64                                  → 6848
+6848  BulletDefs        20 × 300                 = 6000     → 12848
+12848 BulletNames       20 × 20                  = 400      → 13248
+13248 GunNames          10 × 20                  = 200      → 13448
+13448 ShootDefs         10 × 8                   = 80       → 13528
+13528 AlienNames        20 × 20                  = 400      → 13928
+13928 AlienDefs         20 × 42                  = 840      → 14768
+14768 FrameData                                    7680     → 22448
+22448 ObjectNames       30 × 20                  = 600      → 23048
+23048 ObjectDefs        30 × 40                  = 1200     → 24248
+24248 ObjectDefAnims    30 × 120                 = 3600     → 27848   (O_AnimSize = 120)
+27848 ObjectActAnims    30 × 120                 = 3600     → 31448
+31448 AmmoGive          30 × 44                  = 1320     → 32768   (AmmoGiveLen = 22*2)
+32768 GunGive           30 × 24                  = 720      → 33488
+33488 AlienAnims        20 × 2420                = 48400    → 81888   (A_AnimLen = 11*20*11 = 2420 !)
+81888 VectorNames       30 × 64                  = 1920     → 83808
+83808 WallGFXNames      16 × 64                  = 1024     → 84832
+84832 WallHeights       16 × 2                   = 32       → 84864
+84864 AlienBrights      20 × 2                   = 40       → 84904
+84904 GunObjects        10 × 2                   = 20       → 84924
+84924 Player1Graphic_w                             2        → 84926
+84926 Player2Graphic_w                             2        → 84928
+84928 FloorData         16 × 4                   = 64       → 84992   (MSW=damage, LSW=SFX)
+84992 AlienShootDefs    20 × 8                   = 160      → 85152
+85152 AmbientSFX        16 × 2                   = 32       → 85184
+85184 LevelMusic        16 × 64                  = 1024     → 86208
+86208 EchoTable                                    60       → 86268
       GLFT_SizeOf_l                                         = 86268
```

**Erratum** : une version antérieure de ce document indiquait par erreur
40 288 octets, en supposant `A_AnimLen = 11 × 11 = 121`. La valeur exacte
est `A_AnimLen = A_OptLen × 11 = (A_FrameLen × 20) × 11 = 11 × 20 × 11 = 2420`,
ce qui donne `AlienAnims = 20 × 2420 = 48 400` octets.

Le `GLF_DatabasePtr_l` (`bss/level_bss.s`) pointe vers ce blob ; toutes les définitions de gameplay sont accédées par offset constant. **Ce layout doit être reproduit exactement** pour pouvoir charger les fichiers existants.

#### 2.4.9 `LvlT` / `TLBT` (header de `twolev.bin`)

`defs.i:538-558` (TLBT, lu depuis disque) et `defs.i:572-601` (LvlT, structure runtime). Très similaires :

```
TLBT header (situé après LVLT_MESSAGE_LENGTH * LVLT_MESSAGE_COUNT = 160*10 = 1600 octets de messages texte) :
  0   Plr1_StartXPos_w
  2   Plr1_StartZPos_w
  4   Plr1_StartZoneID_w
  6   Plr2_StartXPos_w
  8   Plr2_StartZPos_w
 10   Plr2_StartZoneID_w
 12   NumControlPoints_w
 14   NumPoints_w
 16   NumZones_w
 18   Unknown_w
 20   NumObjects_w
 22   PointsOffset_l
 26   FloorLineOffset_l
 30   ObjectDataOffset_l
 34   ShotDataOffset_l
 38   AlienShotDataOffset_l
 42   ObjectPointsOffset_l
 46   Plr1ObjectOffset_l
 50   Plr2ObjectOffset_l
 54   ControlPointCoords_vw (suit immédiatement)
```

`TLGT` (header de `twolev.graph.bin`, `defs.i:562-569`) : 5 offsets — `DoorData`, `LiftData`, `SwitchData`, `ZoneGraphAdds`, `ZoneAdds`.

#### 2.4.10 `ZLiftableT` (36 octets) — portes & ascenseurs (`defs.i:604-623`)

```
  0   Bottom_w           hauteur fermée
  2   Top_w              hauteur ouverte
  4   OpeningSpeed_w     px/frame ouverture
  6   ClosingSpeed_w
  8   OpenDuration_w     frames maintenu ouvert
 10   OpeningSoundFX_w
 12   ClosingSoundFX_w
 14   OpenedSoundFX_w
 16   ClosedSoundFX_w
 18-24 Word9..12         (coord ?)
 26   GraphicsPtrOffset_l offset depuis Lvl_GraphicsPtr_l
 30   ZoneID_w
 32   Word16_w
 34   RaiseCondition_b   DR_Plr_SPC/Plr/Bul/Alien/Timeout/Never
 35   LowerCondition_b   DL_Timeout/Never
```

Constantes `defs.i:510-517` :
- `DR_Plr_SPC=0, DR_Plr=1, DR_Bul=2, DR_Alien=3, DR_Timeout=4, DR_Never=5`
- `DL_Timeout=0, DL_Never=1`

#### 2.4.11 Inventory & game stats

- `InvCT` (44 octets) : `Health_w`, `JetpackFuel_w`, `AmmoCounts_vw[20]`.
- `InvIT` (24 octets) : `Shield_w`, `JetPack_w`, `Weapons_vw[10]`.
- `InvT` (68 octets) = `InvCT` + `InvIT`.
- `GModT` (48 octets) : `MaxInv` + `NumAchievements` + `AchievementSize`.
- `GStatT` : meilleures times par niveau, plays/wins/fails counts, alien kills par type, ammo/health/fuel totaux collectés, bitmap d'achievements.

### 2.5 Macros assembleur globales (`macros.i`)

- **`QMOVE`** (l.105-121) : optimise les moves immédiats en `moveq` si possible (0..127 → moveq direct, 128..255 → `moveq #256-N + neg.b`, sinon `move.X #imm,Y`).
- **`SAVEREGS` / `GETREGS`** : `movem.l d0-d7/a0-a6,-(a7)` et son inverse — utilisés autour des appels critiques.
- **`WB`/`WBSLOW`** (`btst #6, dmaconr`) : attente blitter.
- **`WT`** : wait pour transfert (utilisé dans le replayer audio).
- **`CINIT`/`CMOVE`/`CWAIT`/`CEND`** : wrappers Intuition `UCopperListInit`/`CMove`/`CBump`/`CWait` pour construire des copperlists dynamiquement.
- **`CALLEXEC`/`CALLINT`/`CALLGRAF`/`CALLDOS`/`CALLPOTGO`** (`system.i:57-93`) : convention d'appel via le pointeur de lib (Exec=4.w, sinon `_IntuitionBase`/`_GfxBase`/`_DOSBase`/`_PotgoBase`).
- **`CALLC`** : `xref _\1 + jsr _\1` — appel d'une fonction C depuis l'ASM.
- **`FREE_OBJ` / `FREE_ENT`** : `move.w #-1, ObjT_ZoneID_w(\1)` — l'invariant « zone -1 = libre » est partout.
- **`STATS_PLAY`/`_WON`/`_DIED`/`_KILL`** : appels C pour la télémétrie/progression.
- **`SET_MEM_BIT`** : utilisé pour `Game_ProgressSignal_l`.
- **`DCLC`** : déclare un label accessible à la fois en ASM (`label`) et en C (`_label::`) — usage : `DCLC Vid_VBLCount_l, dc.l, 0`.
- Les macros `CACHE_*`, `DATA_CACHE_*` sont *toutes commentées* — le moteur tourne sans gestion explicite du cache 68k (les `movec CACR,…` ont été désactivés).

### 2.6 Constantes globales (`defs.i:24-34`, `hires.s:36-65`)

```
MAX_LEVEL_OBJ_DIST_COUNT = 288
MAX_OBJS_IN_LINE_COUNT   = 400
LVL_OBJ_DEFINITION_SIZE  = 64
NUM_LEVELS               = 16
NUM_BULLET_DEFS          = 20
NUM_GUN_DEFS             = 10
NUM_ALIEN_DEFS           = 20
NUM_OBJECT_DEFS          = 30
NUM_SFX                  = 64    (RES_NUM_SFX réel = 59 dans res.s)
NUM_WALL_TEXTURES        = 16
SCREEN_WIDTH             = 320
SCREEN_HEIGHT            = 256
FS_HEIGHT_HACK           = 1     (active)
FS_HEIGHT                = 240   (= SCREEN_HEIGHT - 16)
FS_HEIGHT_C2P_DIFF       = 8
SMALL_WIDTH              = 192
SMALL_HEIGHT             = 160
maxscrdiv                = 8
max3ddiv                 = 5
PLR_STAND_HEIGHT         = 12*1024 = 12288
PLR_CROUCH_HEIGHT        = 8*1024  =  8192
scrheight                = 80
intreqrl                 = $01f
PLR_MASTER               = 'm'
PLR_SLAVE                = 's'
PLR_SINGLE               = 'n'
QUIT_KEY                 = RAWKEY_NUM_ASTERISK
```

---

## 3. Boucle principale et pipeline par frame

### 3.1 Séquence de boot

1. **`main()`** (`c/main.c:12-54`)
   - `Sys_OpenLibs()` (`c/system.c`) ouvre `exec`, `graphics`, `intuition`, `dos`, `cybergraphics` (optionnel), `lowlevel`, etc.
   - Lit la ligne de commande Workbench : `ReadArgs("SCREENMODE/N", …)` ; si non spécifié, appelle `GetScreenMode()` qui ouvre un dialogue de sélection de mode.
   - Détecte RTG : `Vid_isRTG = IsCyberModeID(Vid_ScreenMode)` si `CyberGfxBase` est ouverte.
   - Appelle `startup()` (= `_startup` dans `hires.s`).
   - À la sortie : `Sys_CloseLibs()`, retour OS.

2. **`_startup`** (`hires.s:95-184`)
   - `movem.l d1-a6,-(sp)` — sauvegarde des registres.
   - Si `MEMTRACK` défini : `bsr Mem_TrackInit`.
   - `bsr Sys_Init` (`modules/system.s`) :
     - Détecte CPU (`Sys_Move16_b`, `Sys_CPU_68060_b`, `Sys_CPU_68030_b`, `Sys_C2P_Akiko_b`).
     - Initialise l'EClock (`Sys_FrameTimeECV_q`, `ReadEClock`).
     - Installe les IRQ (vblank `INTB_VERTB` priorité 9, ports `INTB_PORTS` priorité 127).
     - Si mode RTG : timer 50 Hz de remplacement pour la vblank.
   - Initialise l'état joueur par défaut : `Plr1_Mouse_b=1`, `Plr1_Energy_w=191`, `Plr2_Energy_w=191`, `draw_GouraudFlatsSelected_b`, etc.
   - **Construit `ConstantTable_vl`** (table de réciproques) : pour i = 1..8192, écrit `16384*64/i` et `64*64*65536/i`.
   - Sélectionne le mode de contrôle par défaut selon `CD32VER` (clavier+souris par défaut sur 1200/4000).
   - `bsr Game_Start`.
   - Si `Game_Start` retourne (`Game_ShouldQuit_b`) : `bsr Sys_Done`, rétablit les registres, `rts`.

3. **`Game_Start`** (`controlloop.s:44-219`)
   - Sauvegarde le stack pointer pour récupération d'erreur fatale.
   - Met `Plr_MultiplayerType_b = PLR_SINGLE` par défaut.
   - **Ouvre l'écran principal** (`Vid_OpenMainScreen`) — alloue les bitmaps double-buffer, installe la palette initiale, configure le copperlist double-height si applicable.
   - **Charge la base de données** : `Res_LoadGLFDatabase` lit le fichier global (.glf) dans `GLF_DatabasePtr_l`.
   - **Charge l'intro/story** dans `Game_StoryPtr_l`.
   - `_InitLowLevel()` (C) : init joypad CD32.
   - **Queue asynchrone des ressources** via `IO_QueueFile` :
     - `Res_LoadSoundFx` : 59 samples → `Aud_SampleList_vl`.
     - `Res_LoadWallTextures` : `0.256wad` à `N.256wad`.
     - `Res_LoadFloorsAndTextures` : `floortile` global + textures.
     - `Res_LoadObjects` : sprites/animations (.wad, .ptr, .256pal).
   - **`IO_FlushQueue`** : exécute toutes les requêtes (DOS Open + Read + décompression LHA).
   - `Res_PatchSoundFx` : convertit `{start, length}` en `{start, end}` pour Paula.
   - Initialise les noms de niveau du menu.
   - **Entre la boucle outer `game_BackToMenu`** :
     - Lit le menu (clavier/souris) — `game_ReadMainMenu`, `game_MasterMenu`, `game_SlaveMenu`.
     - Options : start, level select, difficulty, key binds, load/save.
     - Sur « Play » : `bra Game_Begin`.

4. **`Game_Begin`** (`hires.s:255-816`)
   - Charge `twolev.bin` → `Lvl_DataPtr_l`, `twolev.graph.bin` → `Lvl_GraphicsPtr_l`, `twolev.clips` → `Lvl_ClipsPtr_l`.
   - Calcule les pointeurs vers : portes (`Lvl_DoorDataPtr_l`), ascenseurs, interrupteurs, zones, edges, points, objets, balles.
   - Convertit la table d'offsets de zones en tableau de pointeurs (`Lvl_ZonePtrsPtr_l`).
   - **`Zone_ApplyPVSErrata(Lvl_ErrataPtr_l)`** (C) — si errata présente.
   - **`Zone_InitEdgePVS()`** (C) :
     - `Zone_InitDoorList()` — construit `Zone_DoorList_vw[16]` et `Zone_DoorMap_vb[]`.
     - `zone_AllocEdgePVS()` — alloue `Lvl_ZEdgePVSHeaderPtrsPtr_l`.
     - `zone_FillZEdgePVSHeaders()` puis `zone_FillZEdgePVSListData()` — calcul récursif des per-edge PVS avec door masks.
     - `zone_FillEdgePointIndexes()` — localise les indices des extrémités d'edges dans la table des points.
     - Reset des états : `Zone_CurrentDoorState_w = Zone_RenderDoorState_w = 0`.
   - Init des buffers audio (silencieux).
   - **Init double buffer écran** : `Vid_DisplayScreenPtr_l = Vid_Screen1Ptr_l`, `Vid_DrawScreenPtr_l = Vid_Screen2Ptr_l`.
   - `Sys_FrameNumber_l = 0`, `MarkFrameBegin()`.
   - `Plr_Initialise()`.
   - `AI_InitAlienWorkspace()`.
   - **Compactage de la carte** (lignes 660-724 de `hires.s`) — réorganise les données pour la cache.
   - Configure les paramètres de viewport (`Vid_CentreX_w`, `Vid_BottomY_w`) selon fullscreen ou small.
   - Initialise armes & santé.
   - **Saut sur `game_main_loop`**.

### 3.2 Pacing : VBL et FPS limiter

- **Source temporelle** : interruption vertical blank `INTB_VERTB` (50 Hz PAL, 60 Hz NTSC, ou 50 Hz simulé via timer en RTG).
- **Variables** :
  - `Vid_VBLCount_l` : incrémenté par l'ISR à chaque vblank.
  - `Vid_VBLCountLast_l` : snapshot de la frame précédente.
  - `Sys_FPSLimit_w` : intervalle cible en ticks (ou −1 = uncapped).
- **Boucle de pacing** (`hires.s:1006-1024`) :
  ```
  d2 = Sys_FPSLimit_w
  target = Vid_VBLCountLast_l + d2
  while (Vid_VBLCount_l < target) WaitTOF()
  Vid_VBLCountLast_l = Vid_VBLCount_l
  ```
- **EClock** : `Sys_FrameTimeECV_q` (timer haute précision exec) sert au `Sys_FrameLap()`, `Sys_EvalFPS()` (moyenne glissante 8 frames).

### 3.3 Ordre exact des phases par frame (`hires.s:818-2131`)

**Cette séquence est verrouillée**. Toute modification d'ordre peut casser le multijoueur lock-step ou produire du tearing.

1. **Read potgo** (joystick analog via `_PotgoBase`).
2. **Mort des joueurs en multi** : si Plr1 mort (slave) ou Plr2 mort (master), explosion + message de victoire.
3. **Toggle fullscreen** (key check).
4. **Pause logic** (`Game_Pause()` dans `pauseopts.s`) : single = touche P, multi = synchronisée master/slave.
5. **FPS limiter** : wait VBL (cf. §3.2).
6. **Screen buffer swap** : `swap(Vid_DrawScreenPtr_l, Vid_DisplayScreenPtr_l)`.
7. **Screen flip Intuition** (mode AGA) : attend `Vid_DisplayMsgPort_l`, appelle `ChangeScreenBuffer()`. En RTG : copie blit.
8. **Frame metrics** : `Sys_FrameLap()`, `PrintStats()`, `MarkFrameBegin()`, `Sys_ShowFPS()`.
9. **Copie `Anim_FramesToDraw_w → Anim_TempFrames_w`** (frame counter pour interpolation).
10. **Update Y offsets** : `Vid_CentreY_w`, `Vid_MiddleY_w` lus depuis `SMIDDLEY` selon mode.
11. **Animation eau** : rotation `draw_WaterFramePtr_l`, incrément `wtan`, `wateroff`.
12. **Sauvegarde des positions précédentes** :
    ```
    plr1_OldX_l = Plr1_XOff_l
    plr1_OldZ_l = Plr1_ZOff_l
    (idem plr2)
    ```
13. **Snapshot des "Tmp" → "Snap"** (single ou per-player en multi) — voir §3.4.
14. **`Plr1_Control()` (et `Plr2_Control()` en multi)** : commit Snap → live, collision, physique, zone change.
15. **Calcul brightness des zones visibles + interpolation par points** (`hires.s:1429-1603`) :
    - Pour chaque zone visible : lecture `ZoneT_Brightness_w` + scaling animation (×1.6 approximé).
    - Pour chaque point : interpolation depuis brightness précalc.
    - 10 points bordure → moyenne pour `PlrT_RoomBright_w` (HUD).
16. **LOS inter-joueur en multi** (`CanItBeSeen()`) → `ObjT_SeePlayer_b`.
17. **Hold-time de fire** (charge weapons) : `plr1_TmpHoldDown_w += 1` si fire maintenu.
18. **Delta de position** : `XDiff_w = (XOff - OldX) / Anim_TempFrames_w` — pour audio positionnel.
19. **`Plr1_Use()` / `Plr2_Use()`** : pickups, dégâts ennemis, rendu armes (FPV).
20. **Rendu** (pour chaque joueur) — voir §4 :
    a. Copie du player state vers les variables partagées de rendu (`Plr_XOff_l`, `Vis_AngPos_w`, `PointsToRotatePtr_l`, …).
    b. Check look-behind (P + key) : inverse l'angle de 180°.
    c. `Zone_OrderZones()` (ASM) : tri painter's depuis le viewpoint.
    d. `objmoveanim()` : update positions/anims aliens.
    e. Setup viewport (clip top/bottom).
    f. `DrawDisplay()` : walls, floors/ceilings, objects.
    g. (multi) Reprend a-f pour Plr2 sur l'autre demi-écran.
    h. Si `draw_RenderMap_b` : `DoTheMapWotNastyCharlesIsForcingMeToDo()` (debug map).
21. **`Draw_Crosshair()`** (si activé).
22. **`Sys_EvalFPS()`** : moyenne FPS glissante.
23. **Update palette** : si `Vid_UpdatePalette_b`, appel `Vid_LoadMainPalette()` (changement gamma/brightness/contrast).
24. **`Game_UpdatePlayerProgress()`** : si `Game_ProgressSignal_l`, sauvegarde stats/achievements.
25. **`Vid_Present()`** : C2P → planar (AGA) ou memcpy (RTG).
26. **Viewport resize hotkeys** : NUM_MINUS/PLUS pour réduire/augmenter la zone 3D, F9 pour double-height pixel toggle.
27. **Mark visible zones for AI** (`hires.s:1989-2061`) : OR'd des zones visibles des deux joueurs, set `Worry` sur les aliens en dehors si vus.
28. **Frame-end checks** :
    - ESC → `Game_MasterQuit_b` / `_SlaveQuit_b`. Si les deux → saut `endnomusic`.
    - Zone d'exit atteinte → `endlevel` (level won).
    - Plr Health ≤ 0 → `endlevel` (level lost).
    - Sinon : `bra game_main_loop`.

### 3.4 Pattern « Tmp / Snap / Live »

Crucial pour le multijoueur lock-step. Chaque frame :

```
Phase A (early frame, single OR master OR slave) :
  copie Plr1_Tmp* → Plr1_Snap*   ; capture l'input courant pour cette frame

Phase B (en master/slave) :
  serial : master envoie Plr1_Snap*, reçoit Plr2_Snap*
           slave envoie Plr1_Snap*, reçoit Plr2_Snap*

Phase C :
  Plr1_Control() : applique Plr1_Snap* → Plr1_* (commit physique)
  Plr2_Control() : applique Plr2_Snap* → Plr2_* (idem, du côté reçu en multi)

Phase D :
  Le rendu utilise Plr1_*/Plr2_* (live) — consistant des deux côtés
```

L'input asynchrone (clavier/souris IRQ) écrit dans `Tmp` ; le snapshot fige la frame ; les deux machines (master/slave) calculent la même physique sur les mêmes entrées → comportement déterministe et synchronisé.

### 3.5 Machine à états globale

Pas de FSM formelle — états dérivés de flags :

```
                  ┌─────────────────────────┐
                  ▼                         │
              TITLE_MENU ─[ESC]─► QUIT      │
                  │                         │
       [Play]     │                         │
                  ▼                         │
              LEVEL_SELECT/OPTIONS          │
                  │                         │
                  ▼                         │
              GAME_BEGIN (init level)       │
                  │                         │
                  ▼                         │
  ┌────────► IN_GAME ─[P]─► PAUSED          │
  │               │     ◄────[P]            │
  │               │                         │
  │ [Restart] [Exit zone reached]           │
  │               │                         │
  │               ▼                         │
  │           LEVEL_WON ────────────────────┤
  │               │                         │
  │           STATS / NEXT LEVEL            │
  │               ▼                         │
  └───── LEVEL_LOST ◄─[HP≤0 || ESC×2]───────┘
                  │
                  ▼
              TITLE_MENU
```

Flags :
- `Game_Running_b` : 1 en jeu, 0 en pause.
- `Game_FinishedLevel_b` : 1 si niveau terminé (gagné ou perdu).
- `Game_MasterQuit_b`, `Game_SlaveQuit_b` : flags ESC.
- `Game_MasterPaused_b`, `Game_SlavePaused_b` : pauses synchronisées en multi.
- `Game_ShouldQuit_b` : retour à l'OS.
- `Plr_MultiplayerType_b` : `'n'`/`'m'`/`'s'`.

### 3.6 Double buffer écran

```
allocations à Game_Begin :
  Vid_Screen1Ptr_l : framebuffer chunky 320×256
  Vid_Screen2Ptr_l : framebuffer chunky 320×256

au début de chaque frame :
  swap(Vid_DrawScreenPtr_l, Vid_DisplayScreenPtr_l)
  ; le moteur rend sur le NEW Draw pendant que Display est à l'écran

à Vid_Present() :
  C2P(Vid_DrawScreenPtr_l) → planar dans Vid_DrawPlanarPtr_l
  ChangeScreenBuffer(Vid_DrawPlanarPtr_l)
  ; Intuition queue le flip pour le prochain vblank
```

Le buffer chunky (1 octet/pixel) n'est *jamais* affiché directement : il est converti en planar 8 bitplanes (256 couleurs AGA) par C2P juste avant le flip.

---

## 4. Pipeline de rendu

Le moteur est un **renderer hybride PVS-portal / painter's**, software, à scanlines. Sortie : 8 bitplanes planar (256 couleurs) en AGA, framebuffer chunky 8bpp en RTG.

### 4.1 Vue d'ensemble du pipeline

```
[Plr_XOff, _ZOff, _YOff, _AngPos]
        │
        ▼
[1] Setup frame :
    Vis_SinVal_w  = sinw(AngPos)
    Vis_CosVal_w  = cosw(AngPos)
    draw_WallYOffset_w = (Plr_YOff_l - …) >> 6
    flooryoff = Plr_YOff_l >> 6
        │
        ▼
[2] PVS lookup :
    list = PlrT_ListOfGraphRoomsPtr_l (établi par Plr_Control)
    Zone_OrderZones() : tri painter par distance centre-zone
        │
        ▼
[3] Transformation 3D des points (modules/transform.s) :
    pour chaque point indexé dans PointsToRotatePtr_l :
      view = point - Plr_pos
      tx = view.x*cos - view.z*sin     ; rotation
      tz = view.x*sin + view.z*cos
      x' = (tx<<16) >> 8
      z' = (tz<<16) >> N               ; N = 8 (small) ou 11 avec *1229 (fullscreen)
      screenX = divs.w z', x' + Vid_CentreX_w
    écrit : Rotated_vl[i], OnScreen_vl[i]
        │
        ▼
[4] Walk zones (modules/draw/draw_zone_graph.s) :
    pour chaque zone dans l'ordre painter (back-to-front) :
      Zone_SetupEdgeClipping() :
        Draw_SetLeftClip / Draw_SetRightClip refinent Draw_LeftClip_w,
        Draw_RightClip_w à partir des edge clip points (pvs_ClipID)
      pour chaque élément graphique de la zone (tag-dispatched) :
        0 → wall          : Draw_Wall (affine texture)
        1,2 → floor/ceil  : Draw_Flats (scanline + 1/y LUT, Gouraud optional)
        4 → object        : Draw_Objects (depth-sort + painter)
        7 → water         : Draw_Flats avec draw_UseWater_b=1
       12 → backdrop      : sky/dégradé
        │
        ▼
[5] C2P (modules/c2p/) :
    chunky 8bpp → 8 bitplanes planar (mode 256)
    routine dispatchée par CPU : 68030 / 68040 (040+060) / Akiko / TeleportFX
        │
        ▼
[6] Vid_Present : ChangeScreenBuffer (AGA) ou copy (RTG)
```

### 4.2 Setup frame

`hires.s:3211-3250` :

```asm
  move.w  Vis_AngPos_w,d0
  lea     SinCosTable_vw,a0
  ; sin/cos via mask 0x1FFE + indexation byte
  move.w  (a0,d0.w),Vis_SinVal_w
  ; cos = sin offset par quart de cycle
  add.w   #2048,d0
  and.w   #$1FFE,d0
  move.w  (a0,d0.w),Vis_CosVal_w
  ; …
  move.l  Plr_YOff_l,d0
  asr.l   #6,d0                ; >>6
  move.l  d0,flooryoff
```

### 4.3 Transformation 3D (`modules/transform.s`)

- Entrée : `Lvl_PointsPtr_l[]` (Vec2W : `WORD v_X; WORD v_Z`).
- Pour chaque point listé dans `PointsToRotatePtr_l` :
  1. `viewX = pointX - Plr_XOff_l` (16-bit, après >>16 implicite).
  2. `viewZ = pointZ - Plr_ZOff_l`.
  3. Rotation 2D :
     - `swap d6` permet à un seul registre de tenir `sin (hi)` et `cos (lo)`.
     - `muls sin,viewX` et `muls cos,viewZ` → produits 32-bit.
     - `tx = viewX*cos - viewZ*sin`
     - `tz = viewX*sin + viewZ*cos`
  4. Mise à l'échelle :
     - Small : `x' = (tx << 16) >> 8`, `z' = (tz << 16) >> 8` (≈ ×256).
     - Fullscreen : facteur d'aspect 3/5 → `*1229 >> 11` au lieu de `>>8` sur l'une des composantes.
  5. Près de la caméra (z' ≤ 0) : `screenX = 0` ou clamp.
  6. Projection : `divs.w z',tx → tx/z'`, puis `screenX = tx/z' + Vid_CentreX_w`.
  7. Clamp à `Vid_RightX_w`.
- Sortie : `Rotated_vl[2*i]` = tx, `Rotated_vl[2*i+1]` = tz scaled ; `OnScreen_vl[2*i]` = screenX, `OnScreen_vl[2*i+1]` = mesure perspective (pour le clipping).
- **Près-plan** : `DRAW_BITMAP_NEAR_PLANE = 25`, `DRAW_VECTOR_NEAR_PLANE = 130` (cf. `objdrawhires.s:5-6` & `:214-215`).

### 4.4 Tri painter (`orderzones.s`)

- Optimisation : compare `(Plr_XOff_l & Zone_MovementMask_l, Plr_ZOff_l & Zone_MovementMask_l)` à `zone_LastPosition_vw`. Si inchangé, **ne re-trie pas**.
- `Zone_MovementMask_l` initialisé à `$FFF0FFF0` (4 bits LSB ignorés) ; `Prefs_OrderZoneSensitivity` (0..7) configure le nombre de bits écrasés.
- Sinon : pour chaque zone dans le PVS → `zone_ToDrawTable_vw[zoneID] = 1`. Puis insertion-sort utilisant cross-product 2D entre les edges et la position joueur pour déterminer l'ordre relatif.
- Sortie : `Zone_FinalOrderTable_vw[]` (zoneIDs, terminé par marker), `Zone_EndOfListPtr_l`.

### 4.5 Walk de zones et clipping per-edge (`draw_zone_graph.s`)

```
Draw_LeftClip_w  = 0
Draw_RightClip_w = Vid_RightX_w

pour chaque zoneID dans Zone_FinalOrderTable_vw (back-to-front) :
  charger ZoneT* = Lvl_ZonePtrsPtr_l[zoneID]

  ; clipping par edge si applicable :
  Zone_SetupEdgeClipping(zone) :
    pour les clipIDs de la PVS entry (pvs_ClipID) :
      fetcher l'edge clip point dans Lvl_ClipsPtr_l
      OnScreen[clipPointIdx] → screenX
      Draw_LeftClip_w  = max(Draw_LeftClip_w, screenX gauche)
      Draw_RightClip_w = min(Draw_RightClip_w, screenX droit)

  parcourir la liste graphique de la zone, dispatch par tag :
    tag = 0  → Draw_Wall (mur)
    tag = 1  → Draw_Flats (sol)
    tag = 2  → Draw_Flats (plafond)
    tag = 4  → Draw_Objects
    tag = 7  → Draw_Flats(water)
    tag = 12 → Draw_Backdrop
```

Le clipping per-edge utilise les données précomputées du « per-edge PVS » et de l'**adjacent zone clips enhancement** (cf. `docs/PVS.md` §144-156).

### 4.6 Rendu des murs

- Sources : `hireswall.s` (legacy, en grande partie remplacé), `hiresgourwall.s` (Gouraud), `modules/draw/draw_wall.s` + `draw_wall_060.s` (CPU-spécifiques).
- Pour chaque mur :
  1. Endpoints transformés : `Rotated_vl[edge.startPoint]`, `Rotated_vl[edge.endPoint]`, `OnScreen_vl[…]`.
  2. Clip horizontal : si entièrement hors `[Draw_LeftClip_w, Draw_RightClip_w]`, skip.
  3. Calcul des paramètres de la struct `WD` (Wall Data, `defs.i:643-680`) :
     - `WD_LeftX_w, WD_RightX_w` : extrémités écran.
     - `WD_LeftBM_w, WD_RightBM_w` : BitMap (position dans la texture).
     - `WD_LeftDist_w, WD_RightDist_w` : 1/Z pour stepping.
     - `WD_LeftTop_w, WD_RightTop_w` : Y haut écran (interpolé).
     - `WD_LeftBot_w, WD_RightBot_w` : Y bas écran.
     - `WD_LeftBright_w, WD_RightBright_w` (+ `WD_DHorizBright_l`) : brightness Gouraud.
  4. Itération colonne par colonne :
     - Step affine : `dU`, `dY_top`, `dY_bot`, `dBright` interpolés linéairement entre gauche et droite.
     - Pour chaque scanline de la colonne :
       - Index texel : `(V & mask) >> shift` → byte texture.
       - Lookup palette éclairée : `lit_byte = TexturePalette[bright_row * 256 + texel]`.
       - Écriture chunky : `(framebuffer + Y*320 + X) = lit_byte`.
- **Texture mapping affine** (sans correction perspective). Suffisant pour des murs verticaux courts axe-aligned ; produit des artefacts visibles sur les murs longs ou diagonaux — ces artefacts sont déterministes et **doivent être préservés**.
- Itération counts : `draw_IterationTable_vw` donne `(iterations, shift)` selon la largeur du mur écran (binning).

### 4.7 Rendu sol/plafond (`draw_floor.s`, `draw_floor_060.s`)

Approche scanline + 1/Y perspective :

```
pour Y_screen de top à bottom (clipé à TopClip/BottomClip) :
  ; perspective inverse :
  scanlineDist = floorY_const * OneOverN_vw[Y_screen - Vid_CentreY_w]
  ; world XZ debut/fin de scanline :
  worldX = Plr_X + cos*scanlineDist
  worldZ = Plr_Z + sin*scanlineDist
  dWorldX = cos*scanlineStep
  dWorldZ = sin*scanlineStep

  ; brightness Gouraud :
  bright = CurrentPointBrights interpolé entre coins
  dBright = brightspd

  pour X_screen de leftClip à rightClip :
    ; index texture 64x64 :
    texelIdx = (worldZ & 0x3F) * 64 + (worldX & 0x3F)
    texel = FloorTexture[texelIdx]
    ; lit via palette shading :
    lit = TexturePalette[bright_row * 256 + texel]
    framebuffer[Y_screen*320 + X_screen] = lit
    worldX += dWorldX
    worldZ += dWorldZ
    bright += dBright
```

- **`OneOverN_vw`** : table 16384/N ; multiplie + `asr.l #14`. Pas de division per-pixel.
- **`draw_floor_060.s`** : version unrolled pour les 040/060 (move16, parallélisme dual-pipe).
- **`draw_floor.s`** : version générique 030 avec loop `acrossscrngour`.
- Eau : même code, texture `draw_WaterFramePtr_l` (animation par rotation des frames).
- **Backdrop (ciel)** : `SKY_BACKDROP_W = 648`, `SKY_BACKDROP_H = 240` (`defs.i:638-639`). Probable scrolling horizontal selon `Vis_AngPos_w`.

### 4.8 Rendu objets / sprites (`objdrawhires.s`)

- Tri : `draw_DepthTable_vl[80]` (max 80 sprites visibles par frame).
- Insertion-sort par profondeur Z (back-to-front).
- Pour chaque sprite :
  1. Position transformée (`ObjRotated_vl`).
  2. Projection : `screenH = worldH / Z * scaleConstant` (via `ConstantTable_vl` pour éviter division).
  3. Clipping screen + near-plane (`DRAW_BITMAP_NEAR_PLANE`).
  4. Pour chaque colonne du sprite :
     - Frame bitmap : `Draw_ObjectPtrs_vl[type * N + frame]`.
     - Index texture par row : scaling via mul+shift.
     - Lookup palette : `Draw_TexturePalettePtr_l + glare_offset` (rang 0-31) ou non.
     - Skip pixels transparents (probable couleur 0 = transparent).
- Pas de Z-buffer. Painter's algorithm.

### 4.9 Lighting / palette shading

- **`Draw_TexturePalettePtr_l`** : table 64 × 256 = 16 384 octets.
  - Rangs 0-31 : « glare » (du blanc/saturé vers la couleur naturelle).
  - Rangs 32-63 : « ombre » (de la couleur naturelle vers le noir).
- **Application** :
  - Mur : `lit = TexturePalette[zone.brightness_row * 256 + texel_color]`.
  - Sol/plafond Gouraud : `bright_row` interpolé per-pixel.
  - Objet : flat shading via brightness moyen de la zone.
- **Brightness des points** : précomputé par zone, interpolé per-scanline pour les flats Gouraud. Variable d'animation `Anim_BrightnessAnimPtrs_vl[7]` (`bss/anim_bss.s:14-35`) module la brightness des zones (lampes clignotantes, etc.).

### 4.10 C2P (Chunky-to-Planar)

Localisation : `modules/c2p/c2p.s`, sous-dirs `68030/`, `68040/`, `akiko/`, `teleport_fx/`.

- **Dispatch** : à l'init, `Vid_C2PSetParamsPtr_l` et `Vid_C2PConvertPtr_l` sont positionnés sur la routine choisie selon (CPU détecté × Akiko présent × préférences user).
- **Routines disponibles** :
  - `c2p_ConvertFull1x1Opt030`, `c2p_ConvertFull1x2Opt030` (small / full + double-height).
  - `c2p_ConvertFull1x1Opt040`, `c2p_ConvertFull1x2Opt040`, `c2p_ConvertSmall1x1Opt040`.
  - Akiko : write byte par byte au registre Akiko, lit le résultat planar.
  - TeleportFX : effet de transition (mélange temporel).
- **Format chunky** : `SCREEN_WIDTH (320) × HEIGHT (160 small, 240 fullscreen)` bytes/pixel.
- **Format planar AGA** : 8 plans interleaved, `C2P_BPL_ROWBYTES = 40` octets/plan/ligne. Layout pixel (X, Y) → plane[p] bit position `(Y*40 + X/8, X%8)`.
- **Akiko (CD32)** : registres mappés à `$B80000-` ; écriture séquentielle de 8 octets → lecture de 8 longs planar.

### 4.11 Présentation

- **AGA** :
  1. Attend `Vid_DisplayMsgPort_l` (message « screen flipped » d'Intuition).
  2. `ChangeScreenBuffer(screen, newBitMap)`.
  3. Intuition queue le flip pour le prochain vblank.
- **RTG** :
  1. memcpy du chunky buffer vers le bitmap CyberGraphics.
  2. `WritePixelArray()` ou direct write si bitmap est lockable.

### 4.12 Tables précalculées critiques pour le rendu

Doivent être reproduites strictement (charger les binaires originaux + reconstruire celles bâties au runtime) :

| Table | Source | Taille | Construit où |
|---|---|---|---|
| `SinCosTable_vw` | `incbin "bigsine"` | 16 KB | Disque |
| `ConstantTable_vl` | runtime (`hires.s:151-170`) | 64 KB | Calculé via `divs.l` |
| `OneOverN_vw` | data | variable | Disque ou data inline |
| `DivThreeTable_vb` | `data/tables_data.s:34-42` | 660 octets | Inline |
| `Draw_TexturePalettePtr_l` | `incbin` palette de niveau + ramp | 16 KB | Composition runtime à partir de la palette |
| `LeftSideTable_vw`, `RightSideTable_vw` | runtime | 4 KB chacun | Pré-rendu mur |
| `LeftBrightTable_vw`, `RightBrightTable_vw` | runtime | 4 KB chacun | Pré-rendu mur |
| `anim_BrightnessAnimPtrs_vl[7]` | data | 7 × pointeurs | Patterns lumière |

---

## 5. Système de zones, carte, PVS

### 5.1 Concepts

Cf. `docs/PVS.md`. Un niveau = ensemble de **Zones** (polygones convexes 3-10 côtés) connectées par des **edges**. Chaque zone a un sol bas/plafond bas (lower level) et optionnellement un sol haut/plafond haut (upper level — zones à deux étages superposés).

- Hauteurs **Y inversé** : plus la valeur est petite, plus c'est haut. `DISABLED_HEIGHT = 5000` = pas de niveau supérieur.
- Edges :
  - Solide : `e_JoinZoneID = -1`.
  - Adjacent : `e_JoinZoneID >= 0`.
  - Porte : zone adjacente est une door zone (lookup via `Zone_IsDoor()` qui consulte `Zone_DoorMap_vb`).
  - Ascenseur : idem avec `Zone_LiftMap_vb`.

### 5.2 Layout disque

Le fichier `twolev.bin` contient (dans l'ordre, après 1600 octets de messages texte) :
```
[TLBT header (54 octets + ControlPointCoords_vw)]
[liste des points (Vec2W array, NumPoints_w entries)]
[edges, zones, PVS entremêlés : pour chaque zone,
    [liste d'edges (mots)][ZoneT][PVS records]
    avec les offsets z_EdgeListOffset et z_Points négatifs]
[FloorLines à offset FloorLineOffset_l]
[Objets à offset ObjectDataOffset_l]
[ShotData, AlienShotData]
[ObjectPoints, Plr1Object, Plr2Object]
```

Le fichier `twolev.graph.bin` (`TLGT`) :
```
[5 offsets : Door, Lift, Switch, ZoneGraphAdds, ZoneAdds]
[blocs correspondants]
```

Le fichier `twolev.clips` : edge clipping points (utilisés pour `pvs_ClipID`).

`*.map` et `*.flymap` : graphes pour pathfinding aliens (marche / vol).

### 5.3 PVS — listes potentiellement visibles

**Per-zone PVS** (calculé par l'éditeur, lu depuis disque) :
- Suivant la `ZoneT`, une suite de `ZPVSRecord` (8 octets chacun), terminée par `pvs_ZoneID = -1`.
- Max `PVS_TRAVERSE_LIMIT = 100` zones.
- Chaque entrée donne : `pvs_ZoneID` (à rendre), `pvs_ClipID` (edge clip à appliquer), plus 2 mots TODO.

**Per-edge PVS** (calculé au load via `Zone_InitEdgePVS`) — clé pour réduire l'overdraw :
- Pour chaque zone, on construit un `ZEdgePVSHeader` (`zone.h:121-138`) :
  ```
  zep_ZoneID
  zep_ListSize         taille PVS
  zep_EdgeCount        nombre d'edges joignants
  zep_ZoneMaskOffset   offset vers le mask de visibilité par edge
  zep_DoorMaskOffset   offset vers les masques de portes (0 si pas de porte)
  zep_LiftMaskOffset   offset vers les masques d'ascenseurs (0 si pas d'ascenseur)
  zep_EdgeInfoList[zep_EdgeCount]   ZEdgeInfo[] (8 octets chacun)
  --- data section ---
  [Zone mask byte per (edge, PVS zone)]
  [Door mask 2 bytes per (edge, PVS zone)]    si doors
  [Lift mask …]                                si lifts
  ```
- À l'exécution : pour chaque edge visible dans le FOV du joueur, on consulte le mask pour savoir quelles zones du PVS sont effectivement visibles via cet edge **et** quelle combinaison de portes/ascenseurs doit être ouverte.

**Encodage visibilité** (`zone.h:161-173`) :
```
[type:3][id:5]
  ZVIS_NONE   = 0          ; non visible
  ZVIS_COND   = 1<<5       ; visible si conditions remplies (porte/ascenseur ouvert)
  ZVIS_DOOR   = 2<<5       ; zone est une porte, ID dans les 5 bits bas
  ZVIS_LIFT   = 3<<5       ; zone est un ascenseur
  ZVIS_DIRECT = 4<<5       ; visibilité directe garantie
```

### 5.4 Errata PVS

Pour corriger les false-positives de l'éditeur :
- Stream binaire de mots dans `errata.dat` (cf. `docs/PVS.md` §97-115) :
  ```
  [sourceZoneID][targetID_to_remove ...][-1]
  [sourceZoneID2][...][-1]
  …
  [-1]   ; double -1 final
  ```
- `Zone_ApplyPVSErrata()` (`c/zone_errata.c:264-285`) :
  1. `zone_InitCurrentPVS()` : copie le PVS courant en working buffer, remplace les IDs à retirer par `ZONE_ID_REMOVED_MANUAL` (-2).
  2. `zone_BuildVisitedPVS()` : BFS depuis la source via les edges joignants → liste de zones effectivement atteignables.
  3. `zone_RebuildCurrentPVS()` : compactage — supprime les `-2` et les `ZONE_ID_REMOVED_AUTO (-3)` (zones devenues inaccessibles).
- Workspace alloué via `Sys_GetTemporaryWorkspace()` (2 KB current + 2 KB visited).

### 5.5 Portes (Door zones)

- 16 portes max par niveau (`LVL_MAX_DOOR_ZONES`).
- Indexées par bit dans `Zone_CurrentDoorState_w` et `Zone_RenderDoorState_w` (snapshot pour cohérence rendu).
- Données : flux dans `Lvl_DoorDataPtr_l` au format :
  ```
  [ZLiftableT (36 octets)][ZDoorWall][ZDoorWall][-1]
  …
  [999]   ; fin de liste de portes
  ```
- `Zone_InitDoorList()` (`c/zone_liftable_pvs.c:76-108`) :
  - `Zone_DoorList_vw[16]` : doorIndex → zoneID.
  - `Zone_DoorMap_vb[]` : bitmap pour `Zone_IsDoor(zoneID)` en O(1).
- État runtime : géré par newanims.s (animation de la porte) + zone_liftable_pvs.c (vérification visibilité PVS).
- Sons : `OpeningSoundFX`, `ClosingSoundFX`, `OpenedSoundFX`, `ClosedSoundFX` (4 SFX par porte).
- Trigger : `RaiseCondition_b` ∈ {`DR_Plr_SPC`, `DR_Plr`, `DR_Bul`, `DR_Alien`, `DR_Timeout`, `DR_Never`}.

### 5.6 Ascenseurs (Lift zones)

- Même structure `ZLiftableT` que les portes.
- 16 ascenseurs max (`LVL_MAX_LIFT_ZONES`).
- Différences sémantiques :
  - Manipulent le **sol** (entre `z_UpperFloor` et `z_Floor`), pas le plafond.
  - Peuvent être partiellement ouverts à la jonction (porte = binaire ouvert/fermé, lift = continu).
- État runtime géré pareillement (`Zone_LiftList_vw[]`, `Zone_LiftMap_vb[]`).
- L'implémentation des lift masks par-edge est marquée TODO (`zone_edge_pvs.c:154`).

### 5.7 Téléporteurs

- Définition directement dans la `ZoneT` : `z_TelZone`, `z_TelX`, `z_TelZ`.
- Si `z_TelZone >= 0` : à l'entrée du joueur, téléportation vers (`TelX`, `TelZ`) dans `TelZone`.
- Effet visuel : `modules/c2p/teleport_fx/routines.s` (transition particulière).
- Flag joueur : `PlrT_Teleported_b`.

### 5.8 Floor noise / damage

Pas de structure « zone de dégâts » dédiée. Au lieu de cela :
- `z_FloorNoise` et `z_UpperFloorNoise` (mots dans `ZoneT`) indexent dans `GLFT_FloorData_l[16]` :
  ```
  MSW = damage par tick
  LSW = SFX index (footstep)
  ```
- Le moteur applique périodiquement le damage au joueur si présent sur ces tiles.

### 5.9 Tri de zones (`orderzones.s`)

Optimisation discutée en §4.4. Variables clés :
- `Zone_MovementMask_l` = `$FFF0FFF0` par défaut, ajusté par `Prefs_OrderZoneSensitivity` (0..7).
- `zone_LastPosition_vw` = `(plr.X & mask) | (plr.Z & mask) << 16`.
- `zone_ToDrawTable_vw[]`, `zone_OrderTable_vw[]`, `Zone_FinalOrderTable_vw[]`.

### 5.10 Séquence complète de chargement de niveau

```
Stage 1 — Resource loading (modules/res.s:284-347) :
  IO_QueueFile + IO_FlushQueue :
    walkmap     → Lvl_WalkLinksPtr_l
    flymap      → Lvl_FlyLinksPtr_l
    twolev.bin  → Lvl_DataPtr_l        (CRITIQUE — contient zones/edges/points/objects)
    twolev.graph.bin → Lvl_GraphicsPtr_l
    twolev.clips → Lvl_ClipsPtr_l
    floortile (optionnel) → Draw_LevelFloorTexturesPtr_l
    0.256wad..N.256wad (optionnel) → Draw_LevelWallTexturesPtr_l[N]
    properties.dat (optionnel) → Lvl_ModPropertiesPtr_l
    errata.dat (optionnel) → Lvl_ErrataPtr_l

Stage 2 — Pointer patching (Game_Begin dans hires.s) :
  pour chaque zone : Lvl_ZonePtrsPtr_l[zoneID] = (octet base) + offset
  cf. TLBT header : PointsOffset_l, FloorLineOffset_l, ObjectDataOffset_l, etc.

Stage 3 — PVS preprocessing (zone_edge_pvs.c) :
  Zone_InitDoorList()         construit Zone_DoorList_vw, Zone_DoorMap_vb
  zone_AllocEdgePVS()         alloue Lvl_ZEdgePVSHeaderPtrsPtr_l[NumZones]
  zone_FillZEdgePVSHeaders()  remplit les headers
  zone_FillZEdgePVSListData() récursion d'exploration edge par edge,
                              construit zone masks + door masks + lift masks
  zone_FillEdgePointIndexes() localise les coordonnées d'edges dans la table de points

Stage 4 — Errata (si présent) :
  Zone_ApplyPVSErrata(Lvl_ErrataPtr_l)

Stage 5 — Mod properties (si présent) :
  apply backdrop disables, achievement defs, max inventory overrides

Stage 6 — Reset état :
  Zone_CurrentDoorState_w = 0
  Zone_RenderDoorState_w = 0
  Plr_Initialise()
  AI_InitAlienWorkspace()

Stage 7 — Entrée dans game_main_loop
```

---

## 6. Sous-systèmes gameplay

### 6.1 Input

#### 6.1.1 Sources matérielles

- **Clavier raw** : IRQ `INTB_PORTS` (priorité 127). Handler ASM `key_interrupt` écrit dans `KeyMap_vb[256]` (indexé par scan-code Amiga raw, 0x00..0x67).
- **Souris** : `Sys_ReadMouse()` lit les deltas X/Y depuis Intuition ; boutons via `potinp` (`$dff016`).
- **Joystick** : ports custom `$dff00c`, `$dff00d`, et `$bfe001` (CIA-A PRA).
- **CD32 pad** : `cd32joy.s` → `ReadJoyPort()` de lowlevel.library, retourne un mask `JPF_*`.

#### 6.1.2 Mapping `KeyMap_vb`

Les indices de touches sont **réassignables** (cf. `c/key_defs.h`) :
```
KEY_TURN_LEFT      = 0
KEY_TURN_RIGHT     = ?
KEY_FORWARDS       = 2
KEY_FIRE           = 4
KEY_RUN            = 6
KEY_DUCK           = 10
KEY_JUMP           = 12
KEY_NEXT_WEAPON    = 16
…
```
Le tableau `Plr_KeyAssignments_vb[]` (préfs) traduit ces indices logiques en scan-codes physiques. Sauvegardé dans la config.

#### 6.1.3 Routines d'input

- `plr_KeyboardControl` (`modules/player.s:181`) : lit `KeyMap_vb`, met à jour `Plr*_Tmp*` (positions, angle, fire, etc.).
- `plr_MouseControl` (`modules/player.s:73-172`) :
  - Delta X → `SnapAngSpd_w` (vitesse de rotation).
  - Delta Y → `AimSpeed_l` (visée verticale).
  - Bouton gauche (`potinp` bit) → `Fire_b`.
  - Bouton droit → `NextWeapon`.
  - Facteur d'échelle Y `85/128` en fullscreen.
- `cd32joy.s` mappe : GREEN→fire, YELLOW→use, RED→run, BLUE→duck, FORWARD→sidestep, REVERSE→jump, PLAY→next weapon.

### 6.2 Mouvement joueur (`modules/player.s`)

Vitesses (unités/frame) :
- Marche avant/arrière : 35.
- Course : 60.
- Sidestep : 2 (marche) / 3 (course).
- Rotation : `Prefs_TurnSpeed_w` (10 walk, 14 run par défaut), accélération sur appuis successifs jusqu'à ×2.

Pipeline par frame :
1. Lit touches → calcule direction logique.
2. Convertit en vecteur monde via `sin/cos` (`Plr_AngPos_w`).
3. Met à jour `SnapXSpdVal`, `SnapZSpdVal`.
4. Crouch toggle → `SnapTargHeight = PLR_CROUCH_HEIGHT (8192)` ou `STAND (12288)`.
5. Interpolation hauteur : `±1024 par frame` vers le target.
6. Appelle `MoveObject` (collision) avec `StepUpVal` (40×256 debout / 10×256 accroupi) et `StepDownVal` (grand).
7. Gravité/jump dans `fall.s`.

### 6.3 Collision (`objectmove.s`)

Itère les edges de la zone courante :
```
pour chaque edge :
  ; signed cross product :
  d = (newX - edge.XPos) * edge.ZLen - (newZ - edge.ZPos) * edge.XLen
  if |d| <= 32 * thickness : collision

  ; sliding :
  si collision : projeter le mouvement le long de l'edge

  ; zone crossing :
  si edge.JoinZone >= 0 :
    si abs(newZone.Floor - oldZone.Floor) > StepUpVal : bloquer
    si plafond - sol < height + clearance : bloquer
    sinon : passer dans newZone, mettre à jour PlrT_ZonePtr_l
```

### 6.4 Gravité et chute (`fall.s`)

- Accélération constante (gravity), appliquée à `SnapYVel`.
- Si fall depth > seuil : dégâts de chute (proportionnels à la vélocité).
- Saut : `SnapYVel = -jumpInitVel` au moment du jump (touche dédiée).
- Jetpack : si `Plr_Jetpack_w > 0` et fuel > 0, `SnapYVel` neutralisé tant que la touche est maintenue.

### 6.5 Armes et tir (`newplayershoot.s`)

```
chaque frame :
  decrement PlrT_TimeToShoot_w
  si TimeToShoot > 0 : skip

  si Plr_TmpFire_b :
    gunDef = GLFT_ShootDefs_l[Plr_GunSelected_b]
    bulType = gunDef.BulType_w
    bulDef  = GLFT_BulletDefs_l[bulType]
    ammoIdx = bulType
    si Plr_AmmoCounts_vw[ammoIdx] < gunDef.BulCount_w : skip

    si bulDef.IsHitScan_l :
      raycast depuis Plr_pos dans direction AngPos+AimSpeed
      pour chaque objet le long du ray : test distance, LOS via PVS
      target = plus proche dont EntT_HitPoints_b > 0
      EntT_DamageTaken_b += bulDef.HitDamage_l

    sinon :  ; projectile
      allouer un nouveau ShotT dans Lvl_ShotDataPtr_l[]
      initialiser pos/vel/lifetime/gravity selon bulDef
      VelocityX,Y,Z calculés depuis angle + AimSpeed

    Plr_AmmoCounts_vw[ammoIdx] -= gunDef.BulCount_w
    Plr_TimeToShoot_w = gunDef.Delay_w
    Aud_PlaySFX(gunDef.SFX_w)
```

### 6.6 Mise à jour des objets (`objectmove.s`)

Itère tous les objets actifs (ceux où `ObjT_ZoneID_w != -1`). Pour chaque :
1. Si projectile : `pos += vel`, `vel.Y += gravity`, decrement `Lifetime`.
2. Apply collision : si edge touchée → bounce/impact selon `BulT_BounceHoriz`/`Vert`.
3. Si impact : spawn `BulT_ImpactGraphicType_l` particle, jouer `BulT_ImpactSFX_l`.
4. Si entité : run `AI_MainRoutine` (cf. §6.7).
5. Si lifetime ≤ 0 ou hp ≤ 0 : `FREE_OBJ` (set `ObjT_ZoneID_w = -1`).

### 6.7 IA aliens (`modules/ai.s`, `newaliencontrol.s`)

State machine via `EntT_CurrentMode_b` :
```
0 = Default        patrouille via control points
1 = Response       a vu le joueur, se déplace vers lui ou tire
2 = Followup       poursuite après que le joueur s'éloigne
3 = Retreat        recul (animation hurt-back)
4 = TakeDamage     animation de douleur
5 = Die            animation de mort, spawn de gibs ou enfants
```

Dispatch (`AI_MainRoutine` ~ ligne 23 de `modules/ai.s`) :
```asm
  move.b EntT_CurrentMode_b(a0),d0
  cmp.b #5, d0
  beq ai_DoDie
  cmp.b #4, d0
  beq ai_DoTakeDamage
  cmp.b #3, d0
  beq ai_DoRetreat
  cmp.b #2, d0
  beq ai_DoFollowup
  cmp.b #1, d0
  beq ai_DoResponse
  bra ai_DoDefault
```

Détection joueur : via `ObjT_SeePlayer_b` (bitmap des joueurs vus), positionné ailleurs (LOS PVS-based).

Damage handling :
- `EntT_DamageTaken_b` accumulé chaque frame.
- Si damage ≥ HP/4 : passe en TakeDamage, sinon décrémente HP.
- Trigger response : `GetRand() & 3 == 0` avec certaines probas.

Paramètres par alien (depuis `AlienT` dans le GLF) :
- `DefaultBehaviour_w`, `ResponseBehaviour_w`, `FollowupBehaviour_w`, `RetreatBehaviour_w` : IDs de patterns.
- Speeds correspondants.
- `ReactionTime_w`, timeouts.
- `DamageToRetreat_w`, `DamageToFollowup_w` : seuils de transition.
- `BulType_w` : type de balle tirée.
- `SplatType_w` : entité spawnée à la mort (gibs / alien enfant).

### 6.8 Animation (`newanims.s`)

- `Anim_FramesToDraw_w` : compteur global frames.
- `Anim_TempFrames_w` : snapshot frame courante.
- Sprites :
  - Chaque alien a un set d'anims `GLFT_AlienAnims_l[alienType]` (121 octets/alien : 11 frames × 11 octets).
  - 8 directions (front, back, left, right, FL, FR, BL, BR — diagonales pas implémentées mais frame count partagé).
  - `EntT_WhichAnim_b` indexe l'anim courante.
  - `EntT_Timer1_w`, `EntT_Timer2_w` cadencent les frames.
- Lights : `anim_BrightnessAnimPtrs_vl[7]` : 7 patterns de clignotement (sinusoide, flicker, pulse).
- Water : rotation `draw_WaterFramePtr_l` chaque frame, `wateroff` accumulateur.
- Doors/lifts : `anim_DoorOpenTimers_vw[40]` et `anim_LiftHeightTable_vw[40]`.
- Head bob : `Plr_BobbleY_l` oscillation sinusoïdale modulée par vitesse.

### 6.9 Multijoueur série (`serial_nightmare.s`)

Architecture lock-step master/slave :
- **`Plr_MultiplayerType_b`** = `'m'` ou `'s'`.
- Le serial Amiga (registres SERDATR/SERDAT/SERPER à `$dff018`-`$dff032`, contrôle via `$bfd000`/`$bfe001`) est utilisé en mode bit-serial logiciel.
- Routines : `SERSEND`, `SERREC`, `SENDFIRST`, `RECFIRST`, `SENDLONG`, `RECEIVE`.
- Échange par frame :
  - Master envoie son `Plr1_Snap*` (pos, angle, fire, ammo, gunSelected, etc.).
  - Reçoit `Plr2_Snap*` du slave.
  - Le slave fait l'inverse.
  - Les deux côtés calculent `Plr1_Control()` ET `Plr2_Control()` sur les mêmes données → état déterministe.
- Quantités échangées (typique, ordre dans `hires.s:1199-1313`) :
  - Position X, Y, Z (longs).
  - Angle (word).
  - Health (word).
  - AmmoCounts (words).
  - AimSpeed (long).
  - Fire flags (bytes).
  - GunSelected.

### 6.10 `ab3diipatchidr.s`

Patch d'Intuition `EasyRequestArgs` pour empêcher les requesters système (« No Disk », etc.) de bloquer le jeu. Remplace la fonction par un stub retournant 0.

---

## 7. Audio, vidéo, interactions matériel Amiga

### 7.1 Replayer musical (`modules/music.s`)

Replayer **Protracker custom** — pas MED, pas un fichier MOD lu via OS, mais un parseur MOD reconstruit à la main.

#### 7.1.1 Données

- `mt_data` (`music.s:508`) : pointeur vers le fichier MOD chargé.
- Layout MOD standard :
  - 20 octets : nom du module.
  - 31 × 30 octets : sample headers (nom, length, finetune, volume, repeat start, repeat length).
  - 1 octet : nombre de positions dans la séquence.
  - 1 octet : restart byte (ignoré).
  - 128 octets : sequence table (position → pattern).
  - 4 octets : signature `M.K.` ou similaire.
  - N × 1024 octets : patterns (64 rows × 4 channels × 4 octets/note).
  - Suite : données de samples (PCM 8-bit signé, mono).

#### 7.1.2 État runtime

- `mt_speed` : ticks par row (défaut 6).
- `mt_counter` : tick counter.
- `mt_pattpos` : position dans le pattern (0..63).
- `mt_songpos` : position dans la sequence (masquée à `0x7F`).
- 4 voices (`mt_voice1..4`) : 16 mots chacune (sample ptr, length, period, volume, arpeggio, vibrato, portamento, …).

#### 7.1.3 Pilotage des canaux Paula

Adresses hardware (canaux audio) :
- AUD0 : `$dff0a0` (pos), `$dff0a4` (len), `$dff0a6` (period), `$dff0a8` (volume).
- AUD1..3 : pas `$dff0b0`, `$dff0c0`, `$dff0d0`.
- DMACON : `$dff096` ; bits 0-3 activent les canaux, bit 15 = SET, bit 14 = CLR.

Procédure par tick (`music.s:221-252`) :
```
1. Désactiver DMA : DMACON = 0x000F (clear bits 0-3)
2. Charger sample ptr, length, period, volume pour chaque voice modifiée
3. Attente ~250 cycles (sécurité Paula)
4. Activer DMA : DMACON = 0x800F (set bits 0-3 + master enable)
5. Attente ~250 cycles
6. Écrire les sample pointers/lengths pour la prochaine boucle (looping)
```

#### 7.1.4 Effets supportés

| Code | Effet | Implémentation |
|---|---|---|
| 0x0 | Arpeggio | Cycle 3 notes par tick (`music.s:85-118`) |
| 0x1 | Pitch slide up | Decrement period jusqu'à `$71` |
| 0x2 | Pitch slide down | Increment period jusqu'à `$358` |
| 0x3 | Portamento | Smooth slide vers note cible |
| 0x4 | Vibrato | Table sine 32-byte (`mt_sin`, `music.s:474-476`) |
| 0xA | Volume slide | ±/+ |
| 0xB | Position jump | `mt_songpos = ...` |
| 0xC | Set volume | direct 0..$40 |
| 0xD | Pattern break | next pattern |
| 0xE | Filter toggle | `$bfe001` bit 1 (filter LED) |
| 0xF | Set speed | change `mt_speed` |

Period table : 37 entrées PAL (`music.s:478-482`), couvrant `$358` (B-0) à `$71` (B-3).

#### 7.1.5 Synchronisation

- Le replayer est appelé une fois par vblank (ISR INTB_VERTB).
- Pas d'utilisation des IRQ audio Paula (les canaux sont en mode boucle libre).

### 7.2 Effets sonores

- `RES_NUM_SFX = 59` (≠ `NUM_SFX = 64` dans `defs.i` — `defs.i` est la borne haute, 59 le nombre actuellement utilisé).
- `Aud_SampleList_vl[59]` : pour chaque SFX, 8 octets `{startAddr, endAddr}`.
- Trigger : par game-event (touche fire → `Aud_PlaySFX(weapon.SFX_w)`, alien attack, door open, footstep, …).
- Pas de mixer logiciel — un SFX prend un canal Paula libre (priorité simple : channel-stealing par ordre).
- Canaux 1-2 prioritairement music, 3-4 prioritairement SFX (heuristique probable ; à confirmer).

### 7.3 Mode vidéo et double buffer

#### 7.3.1 Init (`modules/vid.s`, `c/screen.c:75-200`)

Mode AGA natif :
- `OpenScreenTags` avec le `Vid_ScreenMode` choisi.
- Allocation 2 bitmaps via `AllocRaster()` × 8 plans.
- `DBufInfo` avec `DisplayMsgPort` pour signal du flip.
- 8 bitplanes = mode 256 couleurs.

Mode RTG :
- `OpenScreenTags` avec `SA_DisplayID = Vid_ScreenMode` ; CyberGfx.
- Bitmap offscreen 8 bpp.
- `WritePixelArray()` pour blit.

#### 7.3.2 Modes d'affichage

- **Small** : 192 × 160 fenêtre 3D, le reste = HUD.
- **Fullscreen** : 320 × 240 (= SCREEN_HEIGHT - 16 grâce à `FS_HEIGHT_HACK = 1`), HUD inline.
- **Double-height** : mode à scanlines doublées pour les CPU lents — `FMODE = 0x4003` (bits BSCAN2 | BPAGEM | BLP32), `bpl1mod = -40`, `bpl2mod = -8`. Switch via copperlist mi-écran.

#### 7.3.3 Copperlist

- **Title screen** : `titlecop.s` incbinge `includes/newtitlepal` — copperlist statique avec palette et bitplane pointers.
- **Game double-height** : `c/screen.c:246-300` construit une `UCopList` :
  ```
  CINIT(copList, NUM_INSTR)
  CWAIT(startLine, 0)
  CMOVE(custom.bpl1mod, -40)       # répète odd lines
  CMOVE(custom.bpl2mod, -8)        # skip even lines
  CMOVE(custom.fmode, 0x4003)
  CWAIT(hudLine, 0)
  CMOVE(custom.bpl1mod, normalMod)
  CMOVE(custom.bpl2mod, normalMod)
  CMOVE(custom.fmode, 0x0003)
  CEND(copList)
  ```
- Pas de changement de palette per-scanline — palette unique par frame.

### 7.4 Palette

#### 7.4.1 Format

- `LoadRGB32Struct_vl` : 256 × 3 longwords (R, G, B en 24-bit chacun, formé 0x00RRGGBB en 32-bit). Préparé pour `LoadRGB32()` graphics.library.
- Source : palette globale 256 couleurs chargée du fichier `256pal` du niveau.

#### 7.4.2 Brightness/Contrast/Gamma (`modules/vid.s:49-151`)

- Hotkeys NUM_1..9 ajustent les paramètres :
  - NUM_1/2/3 : brightness offset ±128 (range `VID_BRIGHT_ADJ_MIN..MAX` = ±4096).
  - NUM_4/5/6 : contrast scale ±8 (80..512, default 256).
  - NUM_7/8/9 : gamma level 0..7 (8 tables `Vid_GammaIncTable1_vb..8_vb` couvrant exponentiations 0.5..0.9375).
- `Vid_UpdatePalette_b` : flag, rebuild palette si non-zéro.
- Recalcul : pour chaque entrée palette, `R' = gamma_table[R] * contrast/256 + brightness_offset`, clamp [0..255].

### 7.5 Blitter

**Inutilisé pour le rendu 3D**. La C2P est CPU-only. Le blitter pourrait être employé dans le menu ou le HUD pour des copies de gros blocs (à confirmer dans `menu/menunb.s`) mais pas dans le hot path du game.

Conséquence pour le port : pas besoin de simuler le blitter pour le rendu 3D.

### 7.6 Interruptions installées

D'après `c/system.c:335-356` :

| Vecteur | Priorité | Handler | Rôle |
|---|---|---|---|
| `INTB_VERTB` (vblank) | 9 | `VBLANKInt` (ASM) | Increment `Vid_VBLCount_l`, run music tick, screen flip signaling |
| `INTB_PORTS` | 127 | `KBInt` → `key_interrupt` (ASM) | Lecture clavier raw, écriture `KeyMap_vb[]` |

Pas d'IRQ audio (DMA continu), pas d'IRQ timer custom (sauf RTG fallback 50 Hz pour simuler vblank).

### 7.7 File I/O (`modules/file_io.s`)

#### 7.7.1 Queue asynchrone

- `IO_QueueFile(filename, dstPtrPtr, lenPtr, memType)` : ajoute à `Sys_Workspace_vl` (max 100 entries).
- `IO_FlushQueue` : itère et traite chaque entrée :
  - `CALLDOS Open(filename, MODE_OLDFILE)`.
  - Si file not found : prompt « Insérez disque ».
  - `CALLDOS Read(...)`.
  - Décompression in-memory (LHA + Fibonacci pour samples).
  - `CALLDOS Close()`.

#### 7.7.2 Décompression LHA

- `unLHA: incbin "decomp4.raw"` (`file_io.s`, blob 2508 octets).
- Algorithme classique LH5/LH4 (LHA — déflate-like avec Huffman).
- Décompresse en RAM allouée par AllocMem.

#### 7.7.3 Décompression PCM Fibonacci (samples `.fib`)

Table delta (`file_io.s`) :
```
fibonnaci_lookup_vb: dc.b -34,-21,-13,-8,-5,-3,-2,-1,0,1,2,3,5,8,13,21
```

Décodage : chaque nibble (4 bits) du flux compressé est un index dans la table ; on accumule les deltas dans un accumulateur 8-bit pour reconstruire le sample PCM. Compression ≈ 2× pour la voix.

### 7.8 Détection CPU et hardware

- `system.c:215-246` :
  - `ExecBase->AttnFlags` : `AFF_68040` → `Sys_Move16_b = 1`, `AFF_68060` → `Sys_CPU_68060_b = 1`.
  - 030 par défaut si ni 040 ni 060.
  - Akiko : lecture `*(UWORD*)0xB80002` ; si valide et `GfxBase->LibNode.lib_Version >= 40` → `Sys_C2P_Akiko_b = 1`.
- Le binaire est compilé pour un CPU minimal mais peut dispatcher dynamiquement vers une version optimisée (C2P, certaines routines de rendu).

### 7.9 Menu (`menu/menunb.s`)

- Render buffer dédié `mnu_morescreen`.
- Fonctions : `mnu_start`, `mnu_loop`, `mnu_domenu`, `mnu_printxy`, `mnu_animcursor`, `mnu_dofire`, `mnu_movescreen`.
- Police `mnu_font` 16×16, 3 plans, 176 lignes.
- Random : LFSR initial avec seed `'TBL!'` (0x54424C21).
- Palette `mnu_palette` séparée (256 entrées chunky → planar via C2P standard).
- Pas de copperlist séparée — utilise le même framebuffer que le jeu.

---

## 8. Formats des médias et données disque

### 8.1 Inventaire `medias/original/`

| Dossier | Type | Fichiers | Notes |
|---|---|---|---|
| `256pal` | Palette globale | 1 | 1536 octets — palette maître 256×6 octets (12-bit AGA, format brut) |
| `INCLUDES` | Assets compilés | 34 | Triplets `.256pal`/`.ptr`/`.wad` par objet ; `floortile`, `newtexturemaps`, `newtexturemaps.pal` |
| `levels` | Niveaux | (vide ici) | Les .bin/.graph.bin/.clips/.map/.flymap sont produits par l'éditeur |
| `menu` | UI graphics | 8 | `.raw`, `.pal`, `.pal2`, `.png` |
| `samples` | Audio | 51 | `.fib` (PCM 8-bit signé compressé Fibonacci) |
| `vectobj` | Objets vectoriels | 22 | Modèles polygonaux (plasma gun, mantis, etc.) |
| `walls` | Textures murs | 14 | `.256wad` |
| `hqn` | « HQ-N » | 15 | Triplets pour gros sprites/bosses (ASHNARG, GUARD, PRIEST, INSECT) |
| `DOCS` | Documentation Team17 | 33 | HOW2* guides AmigaGuide-like |
| `SBDepack` | Utilitaire | 1 | Dépacker (Stone Cracker / similar) |
| `decompress_all` | Tool | 1 | Script de décompression batch |
| `TEST.LNK` | Binaire linker | 1 | Compiled level/asset link records |

### 8.2 Palette (`256pal`)

- Fichier brut 1536 octets = 256 entrées × 6 octets.
- Chaque entrée : 3 × 16-bit big-endian (R, G, B) — format **24-bit AGA**.
- Exemples header : `00 00 00 00 00 00 | 08 00 08 00 08 00 | …` (graduations linéaires noir → gris).
- Utilisé par WALLCONVERT, FLOORCONVERT, OBJECTCONVERTOR comme palette de référence pour le quantization.

### 8.3 Wall textures (`*.256wad`)

- Format propriétaire Team17.
- Contraintes :
  - Hauteur **puissance de 2** : 16, 32, 64, 128.
  - Largeur arbitraire.
  - Source : IFF ILBM 32 couleurs → conversion WALLCONVERT.
- En-tête (probable, à vérifier) : table d'offsets de scanlines/colonnes en mots, puis données chunky.
- Tailles observées : 5.2 KB à 24 KB.
- Indexation : `Lvl_WallFilenameN_vb: '0.256wad'`, `'1.256wad'`, etc.

### 8.4 Floor tiles (`floortile`)

- 16 tuiles 64×64 (chunky 8-bit) concaténées = 16 × 4096 = 65 536 octets (`floortile` fait 64 KB dans `INCLUDES/`).
- Per-tile metadata : damage et SFX index dans `GLFT_FloorData_l[16]`.

### 8.5 Sound samples (`.fib`)

- Container IFF-like avec compression Fibonacci (cf. §7.7.3).
- PCM 8-bit signé mono, taux de lecture déterminé par la `period` Paula (8 kHz à 28 kHz typique).
- Stockés en Chip RAM (DMA Paula).

### 8.6 Objets / sprites / projectiles (`*.256pal` + `*.ptr` + `*.wad`)

Triplet par asset :
- `*.256pal` : palette spécifique réduite (2-8 KB).
- `*.ptr` : table de frames (offsets dans le `.wad`, dimensions scaled, vertical offset, FX index, action stars, next-frame).
- `*.wad` : données pixels (chunky avec index palette, compressé SBDepack).

Format de frame (HOW2DefineObjects, HOW2DefineAliens) :
```
GF : graphic file ID
FN : frame number (préfixé 'R' = horizontal flip)
SW, SH : scaled width / height (0..255)
VO : vertical offset (head-bob)
FX : sound effect index
AC : action star (déclenche événements gameplay)
NF : next frame ID
```

Aliens : 8 directions × N frames, contrainte : **toutes les directions ont le même frame count** (le moteur ne reset pas le compteur sur direction change).

### 8.7 Vector objects (`vectobj/`)

22 fichiers, pas d'extension. Modèles 3D polygonaux pour armes en main (vue FPS) et certaines decorations rotatives. `newtexturemaps` (128 KB) + `newtexturemaps.pal` (16 KB) : atlas de textures appliquées aux faces.

Format probable : header + liste de vertices (Vec3W) + liste de faces (indices vertex + texture map + brightness) + frames d'animation (rotation autour de Y, 8192 unités = 360° comme partout).

### 8.8 Menu graphics

- `back.raw`, `back2.raw` : 20 480 octets — 320×64 chunky 8-bit.
- `credits_only.raw` : 23 040 octets — 320×72 chunky.
- `font16x16.raw2` : 21 120 octets — bitmap police 3 plans 176 rows.
- `back.pal`, `firepal.pal`, `firepal.pal2`, `font16x16.pal2` : palettes (le `2` indique probablement une seconde palette pour animation).

### 8.9 Niveaux : `twolev.bin`, `twolev.graph.bin`, `twolev.clips`

Structures binaires (cf. §2.4.9, §5.2).
- Tous générés par l'éditeur Team17 (non fourni).
- Le moteur les charge en bloc puis patche les pointeurs via les offsets du header.

### 8.10 `TEST.LNK`

Linker output binaire. Contient des références :
- LEVEL A..LEVEL P (16 niveaux).
- Asset paths : `TKG1:INCLUDES/ALIEN2`, `TKG1:HQN/TRICLAW`, `TKG1:INCLUDES/EXPLOSION`.
- Sounds : `tkg1:sounds/scream`, `tkg1:sounds/door01`.
- Floor data : `TKG1:INCLUDES/FLOORTILE`.
- Texture maps : `TKG1:INCLUDES/NEWTEXTUREMAPS`.

Probable index global → produit le **GLF file** (`GLF_DatabasePtr_l`).

### 8.11 Décompression `SBDepack` / `decomp4.raw`

- `SBDepack` (3272 octets) : utilitaire de pre-decompression, probablement Stone Cracker.
- `decomp4.raw` (2508 octets) : runtime LHA decompressor embarqué dans le binaire via `incbin`.

Stratégie de chargement standard :
```
1. CALLDOS Open / Read le fichier compressé
2. Lire le header (taille compressée + décompressée)
3. AllocMem(taille décompressée)
4. unLHA(src, dst, srcLen, dstLen)
5. CALLDOS Close
```

---

## 9. Points critiques pour le pixel-perfect & risques de divergence

### 9.1 Reproductions strictement obligatoires

1. **`SinCosTable_vw`** : charger `bigsine` tel quel (16 384 octets). Implémenter `sinw`/`cosw` à l'identique (masque `0x1FFE`, shift `>> 1`, addition `2048` pour `cosw`). Toute table régénérée à partir de `Math.sin` produira une dérive de 1 unité Q15 ici et là → cumul d'erreurs visibles sur la projection des points lointains.
2. **`ConstantTable_vl`** : reconstruire avec **division entière 32-bit signée Motorola** (`divs.l`) — la troncation Java vers zéro est OK car la divs.l le fait aussi pour les valeurs positives en jeu. Vérifier les bornes (1..8192).
3. **Fixed-point shifts** : `asr.l #N` doit être traduit par `>> N` Java (arithmetic shift). Pour `int` Java c'est correct. Attention à `lsr`/`asr` quand le sign-bit doit être propagé.
4. **Wrap / overflow** : les `add.w` 16-bit wrap modulo 65536. Java `short` puis `& 0xFFFF` ou opérations explicitement masquées. Le wrap de `Plr_AngPos_w` est l'exemple central.
5. **Ordre des phases par frame** (cf. §3.3) : *ne pas* paralléliser AI/render ; *ne pas* interpoler entre frames ; *ne pas* changer l'ordre snapshot → physique → rendu.
6. **PVS errata** : appliquer **avant** le calcul du per-edge PVS, et préserver l'ordre exact de l'errata (collapse in-place).
7. **Tri painter via `orderzones.s`** : reproduire l'optimisation par quantization (`Zone_MovementMask_l`) — sinon les zones se ré-ordonneront plus souvent que dans l'original, ce qui peut changer l'ordre relatif pour les cas ambigus.
8. **Affine texture mapping** sans correction perspective : les murs et sols ont des artefacts de "wobble" caractéristiques. **Ils doivent être préservés**. Pas de bilinear filtering. Pas de mipmaps automatiques.
9. **Lookup palette shading** (64×256) : ne pas remplacer par un calcul (`color * brightness/64`) — la table contient des perturbations non-linéaires.
10. **C2P → planar AGA puis affichage** : pour rester strictement fidèle, **conserver le pipeline planar même en Java**. Sinon : émuler la palette indexée mais admettre que la C2P n'est plus visible. La sortie pixel-finale (palette index → couleur 24-bit via `LoadRGB32`) doit être bit-exacte.
11. **Replayer Protracker custom** : reproduire les effets 0x0-0xF avec leurs particularités (e.g. la limite `$71`/`$358` du pitch slide, la table sine 32-byte du vibrato). Ne pas utiliser une lib MOD générique.
12. **Décompression LHA et Fibonacci PCM** : implémentations exactes — la moindre erreur produit du bruit audio audible.
13. **Layout des structures** `PlrT`, `ObjT`, `EntT`, `ShotT`, `ZoneT`, `EdgeT`, `ZPVSRecord`, `GLFT` etc. : offsets identiques pour pouvoir charger les fichiers existants.
14. **Vitesse pacing** : 50 Hz PAL ou ce que l'écran d'origine produit. Le pacing par VBL est *adaptatif* via `Sys_FPSLimit_w`. Le moteur **n'est pas frame-rate-independent** : à 60 Hz les vitesses sont 20% plus rapides — comportement intentionnel à préserver pour le mode 60 Hz.
15. **Lock-step multijoueur** : la sérialisation `Plr*_Snap*` à chaque frame doit envoyer/recevoir **les mêmes champs dans le même ordre** que l'original — sinon désync immédiat.
16. **Génération de nombres aléatoires** : le moteur utilise un `GetRand()` (à localiser — probablement dans `modules/system.s` ou `tables_*`). **Reproduire l'algorithme exact** (probablement un LFSR). Le menu utilise un LFSR avec seed `'TBL!'`. Sans seed identique, les events RNG-driven (alien response chance, splat type, …) divergent.
17. **Angles : 8192 unités = 360°** (par convention `defs.i/docs`). Vérifier au moment du port que toute conversion C ↔ ASM ↔ docs converge sur cette valeur (point d'incohérence connu, §10.1).

### 9.2 Risques de divergence à surveiller

| Risque | Symptôme | Mitigation |
|---|---|---|
| Substitution `Math.sin` | Drift progressif de la rotation, artefacts mineurs | Lookup `bigsine` strict |
| Division `int / int` Java vs Motorola `divs.w` | Quotient différent quand `|dividend| > 0x7FFF * divisor` (Motorola génère overflow trap, Java truncate) | Tester explicitement les overflows, reproduire le clamp si nécessaire |
| Sign-extension implicite C vs Java | `byte` Java est signé, `UBYTE` C aussi mais souvent traité unsigned. `EntT_DamageTaken_b` etc. — `& 0xFF` quand utilisé arithmétiquement |
| Wrap angle Java | `angle & 0x1FFE` correct si `angle` est `int` ; si `short`, vérifier après `+ 2048` |
| Ordre flottant des sub-frames AI | AI prend des décisions différentes → comportement alien divergent | Verrouiller l'ordre d'itération de la liste `Lvl_ObjectDataPtr_l` |
| Cache CPU non-déterminisme | Le 040/060 avait des effets de cache subtils ; en Java JIT compilable autrement | Garantir le déterminisme algorithmique ; le cache n'affecte que la performance, pas le résultat |
| Frame timing variable | Vélocités/timers basés sur frame count divergent | Verrouiller le FPS cible (50 ou 60 Hz selon le mode) ; pas de `Δt` continu |
| Floating-point dans les LUT | Les LUT sont entières ; les regénérer en float puis cast = ±1 sur les bordures | Tout en entier |
| Couleurs RGB 24-bit vs 12-bit AGA | AGA stocke 8-bit par canal mais beaucoup de jeux ne remplissent que les 4 bits hauts | Vérifier le format de `256pal` (24-bit = 256×6 octets) et le respecter |
| Endianness des médias | Tous les fichiers sont big-endian (Amiga) | `ByteBuffer.order(BIG_ENDIAN)` partout |
| Ordre d'exécution C ↔ ASM | Les callbacks C depuis l'ASM doivent rester séquentiels | Pas de threading dans le hot loop |
| Détection RTG vs AGA | Le rendu final passe par C2P en AGA, direct chunky en RTG | Choisir un seul mode pour le port, idéalement « émuler AGA » pour la fidélité |

### 9.3 Stratégie d'auto-validation

Pour chaque sous-système, proposer un test de parité :

1. **Trig** : générer `sinw(i)` pour `i = 0..8191`, comparer byte-à-byte à `bigsine`.
2. **ConstantTable** : régénérer puis comparer à un dump du binaire 060 en mémoire.
3. **Rendu** : capturer le framebuffer chunky avant C2P à des frames déterministes (level 1, frame 0, 100, 500 avec input scripté). Comparer pixel à pixel.
4. **C2P** : nourrir un chunky test, comparer la sortie planar 8 plans avec une exécution UAE/FS-UAE de la routine ASM.
5. **PVS** : pour chaque zone du niveau 1, dumper la liste PVS après errata + edge-PVS, comparer.
6. **MOD replayer** : enregistrer 60 secondes de chaque musique, comparer waveform à la sortie d'un Protracker générique (en sachant que ce replayer est *custom* — les effets devraient quand même produire la même waveform pour les patterns standard).
7. **AI** : scripter un combat (1 alien, position fixe joueur, fire scripté) ; comparer la liste d'actions et damage frame par frame.
8. **Multijoueur** : faire tourner master Java vs slave Amiga (ou inversement) via UAE serial pipe ; ne pas désynchroniser sur 1000 frames.

---

## 10. Inconnues, hypothèses, à explorer

### 10.1 Incohérence sur l'unité angulaire

`ab3d2_source/docs/README.md` dit : « divides the full circle into 4096 ». Mais le même doc liste : Est = 2048, Sud = 4096, Ouest = 6144 — ce qui implique **8192 unités = cycle complet**. `c/math25d.h` définit `SINTAB_SIZE = 8192` et `cosw(a) = sin(a + 2048)` (un quart) → 8192 cohérent.

**Hypothèse retenue pour le port** : 8192 unités = cycle complet. Vérifier dans `modules/transform.s` et `orderzones.s` les masquages effectifs.

### 10.2 Champs marqués TODO dans `defs.i`

À résoudre par lecture des sites d'usage :
- `ZoneT_ControlPoint_w` : "really UBYTE[2]" — deux valeurs ? Lesquelles ?
- `ZoneT_BackSFXMask_w` (originellement LONG) : quels bits ?
- `EdgeT_Word_5`, `Byte_12`, `Byte_13`, `Flags_w` : tous TODO.
- `PVST_Word_2`, `Word_3` : TODO.
- `ObjT` octets 14-15 (PADDING 2) : « probably overwritten/repurposed for projectiles » dit le commentaire.
- `EntT_DoorsAndLiftsHeld_l` à offset 50 : union avec `Timer3_w` à 52 — comment l'union est-elle disambiguée ?
- `ShotT_AccYPos_w` union avec `ShotT_AuxOffsetX_w`, `_AuxOffsetY_w` à 44 : disambiguation ?

### 10.3 Format précis du `.256wad`

Le header semble être une table d'offsets word-aligned (cf. premier bytes observés). Mais le nombre exact d'entrées, la signification (offsets scanlines ? mipmap levels ?), et le format de la zone data restent à confirmer par lecture de `WALLCONVERT` (si disponible) ou de `Draw_Wall` côté ASM.

### 10.4 Format des `vectobj`

Aucun outil source. Il faut reverse-engineer le binaire en regardant comment `objdrawhires.s` (et probablement une routine dédiée aux poly-objects) le consomme. La hypothèse : header avec `numVertices`, `numFaces`, `numFrames`, puis arrays. À confirmer.

### 10.5 Format précis des `.fib`

Fibonacci delta encoding confirmé via la table `fibonnaci_lookup_vb`. Mais :
- Nombre de bits par sample (4 ou 5) ?
- Header : taille du sample décompressé en premier ?
- Padding ?

À confirmer par lecture de la routine décompression dans `file_io.s:246-372`.

### 10.6 LFSR du moteur principal

Le menu utilise un LFSR avec seed `'TBL!'`. Le moteur principal a forcément son propre `GetRand()` mais sa localisation et son algo restent à confirmer (probablement `bss/tables_bss.s` + une routine assembleur courte).

### 10.7 Couches HUD précises

- Format de la HUD (santé, ammo, weapon icons) ?
- Composition : copperlist, sprite hardware, ou simple chunky overlay ?
- Réponse probable : chunky overlay rendu après `DrawDisplay`, juste avant `Vid_Present`. Mais le format des assets HUD (probablement dans `GLFT_GunGFXFilename_l`) est à confirmer.

### 10.8 Switch zones, échos, ambient SFX

- `GLFT_AmbientSFX_l[16]` et `GLFT_EchoTable_l[60]` : appliqués par zone selon `z_BackSFXMask` et `z_Echo` — détails de la modulation audio (delay, gain) à confirmer.
- `TLGT_SwitchDataOffset_l` : format des interrupteurs (déclenchement de portes, téléporteurs) à reverse-engineer dans `modules/level.s`.

### 10.9 État précis du multijoueur slave/master

L'ordre exact des champs sérialisés sur le port série (`hires.s:1199-1313`) doit être reproduit byte-à-byte pour la rétro-compatibilité avec un Amiga original branché en série.

### 10.10 Editor data (level format complet)

Le moteur charge un binaire compilé. L'éditeur lui-même n'est pas dans `ab3d2_source/`. Pour produire de nouveaux niveaux il faudrait :
- Soit reverse-engineer le binaire éditeur (non fourni).
- Soit reconstruire un pipeline producteur de `twolev.bin`/`graph.bin`/`clips`.

**Pas une priorité immédiate** pour la préservation : on charge les niveaux existants.

---

## Annexe A — Glossaire des fichiers clés

| Fichier | Rôle |
|---|---|
| `hires.s` | Top-level ASM ; inclut BSS/DATA/modules, contient `_startup`, `Game_Begin`, `game_main_loop`, `Plr1_Use`, `Plr2_Use` |
| `controlloop.s` | Boucle outer (menus, level select), `Game_Start`, `Game_Begin` (init pré-jeu) |
| `c/main.c` | Entry point C — libs Amiga + screenmode + appel `startup()` |
| `c/system.c` | Init libs, CPU detection, IRQ install, EClock |
| `c/screen.c` | Setup mode écran (AGA/RTG), copperlist double-height |
| `modules/system.s` | `Sys_Init`, `Sys_Done`, helpers timing |
| `modules/transform.s` | Transformation 3D des points |
| `modules/draw/draw_zone_graph.s` | Walk des zones, dispatch des éléments graphiques |
| `modules/draw/draw_set_clip.s` | Clipping per-edge |
| `modules/draw/draw_wall*.s` | Rendu murs (générique + 060) |
| `modules/draw/draw_floor*.s` | Rendu sols Gouraud (générique + 060) |
| `modules/c2p/*` | Chunky-to-planar par CPU/Akiko |
| `modules/music.s` | Replayer Protracker custom |
| `modules/vid.s` | Setup vidéo, gamma/brightness/contrast |
| `modules/player.s` | `plr_KeyboardControl`, `plr_MouseControl` |
| `modules/ai.s` | State machine alien |
| `modules/level.s` | Loading & pointer patching du niveau |
| `modules/file_io.s` | DOS I/O + LHA + Fibonacci PCM |
| `modules/res.s` | Indexation des ressources (Aud_SampleList_vl etc.) |
| `c/zone_edge_pvs.c` | Per-edge PVS computation |
| `c/zone_errata.c` | Application des errata PVS |
| `c/zone_liftable_pvs.c` | Door/lift list init + masks |
| `c/game_preferences.c` | Préfs persistantes |
| `defs.i` | Structures + équates ASM (autorité layout mémoire) |
| `c/defs.h`, `c/zone.h`, `c/player.h` | Miroirs C des structures |
| `data/tables_data.s` | `incbin "bigsine"` + DivThreeTable |
| `bss/*_bss.s` | Variables uninit |
| `data/*_data.s` | Variables init |
| `docs/PVS.md` | Doc PVS Team17/karlos |
| `docs/README.md` | Doc coords / angles |

---

## Annexe B — Lecture suggérée avant tout portage Java

Dans l'ordre :

1. `docs/PVS.md` (déjà à jour). Le PVS est le concept architectural central.
2. `docs/README.md`. Coordonnées et angles.
3. `c/defs.h` puis `defs.i`. Layout mémoire.
4. `c/main.c` + `hires.s:1-220` (le bootstrap et les includes).
5. `controlloop.s` (Game_Start, Game_Begin).
6. `hires.s:818-2131` (la boucle principale — toute la séquence).
7. `modules/transform.s` (pipeline transform 3D).
8. `modules/draw/draw_zone_graph.s` (orchestration du rendu).
9. `modules/draw/draw_wall.s` + `draw_floor.s` (rasterizers).
10. `objdrawhires.s` (sprites).
11. `modules/c2p/c2p.s` (dispatch C2P).
12. `c/zone_edge_pvs.c`, `zone_errata.c` (PVS preprocessing).
13. `orderzones.s` (tri painter).
14. `modules/player.s` + `plr1control.s` (input & physique).
15. `objectmove.s` + `fall.s` (collision & gravité).
16. `newplayershoot.s` + `newaliencontrol.s` + `modules/ai.s` (gameplay).
17. `modules/music.s` (audio).
18. `modules/vid.s` + `c/screen.c` (vidéo).
19. `modules/file_io.s` + `modules/res.s` (chargement).

---

**Fin du document d'architecture**. Aucun code Java ne sera produit avant validation explicite de ce document, et chaque sous-système sera portré séparément en suivant le workflow prescrit (analyse ASM → pseudocode → traduction → vérification de parité → documentation).
