# rebirth/game — le remake full 3D (jMonkeyEngine)

Moteur du remake moderne d'*Alien Breed 3D II : The Killing Grounds*.

- **jMonkeyEngine 3.9.0-stable**, JDK 21, backend LWJGL3.
- **Autonome** : ne compile pas le portage fidèle (`../../java/src`). Il ne lit que les assets
  modernes de `../assets` (PNG / JSON / OBJ) produits par `gradle -p rebirth extract`.
- Le portage `java/` reste l'**ORACLE** : c'est là qu'on relit l'algorithme d'origine avant de le
  porter ici (règle du projet : portage littéral, jamais d'approximation).

## Lancer

```
gradle -p rebirth/game run                # niveau A
gradle -p rebirth/game run -Plevel=c      # autre niveau (lettre du fichier levels/<X>.json)
gradle -p rebirth/game run -Pshot=90      # capture rebirth_shot.png à la frame 90 puis quitte
gradle -p rebirth/game run -Pdemo -Pangle=4096 -Pshot=260   # marche scriptée + capture
gradle -p rebirth/game run -Pfreecam      # caméra libre (debug géométrie)
gradle -p rebirth/game moveTest           # validation headless (doit dire PASS)
gradle -p rebirth/game moveTest -Plevel=all   # les 16 niveaux
gradle -p rebirth/game lightTest -Pring=-808,184,3   # anneau d'éclairage d'un objet
gradle -p rebirth/game run -Pspawn=1344,512,25       # départ forcé « x,z,zone » (debug)
gradle -p rebirth/game run -Ppitch=0.3    # inclinaison de vue de départ (debug)
```

Jeu : ZQSD/WASD pour bouger, souris pour regarder, Maj pour courir, Espace pour sauter,
E (ou F) pour l'action (portes manuelles), Échap pour quitter.
Caméra libre (`-Pfreecam`) : ZQSD/WASD, clic gauche maintenu pour regarder.

## Contenu

| Classe | Rôle |
|---|---|
| `Main` | application jME : charge le niveau, caméra libre, capture d'écran |
| `Assets` | accès aux assets extraits (`../assets`), cache images/textures, filtre *nearest* |
| `LevelData` / `GlfData` | modèle des JSON extraits (niveaux, base GLF) |
| `LevelBuilder` | géométrie : murs (sous-tuiles), sols/plafonds, sprites billboards, modèles vectoriels |
| `ObjModels` | parseur OBJ des modèles vectoriels (v/vt/f) |
| `Poly2` | triangulation *ear clipping* des contours de secteur |
| `MeshBuilder` | accumulateur de triangles → `Mesh` jME |
| `sim/PlayerSim` | joueur : `plr_KeyboardControl` (vitesses/frottement) + `plr_Fall` + `Plr1_Control` |
| `sim/Move` | `Objectmove.MoveObject` spécialisé joueur : collision glissante + marche de portails |
| `sim/Anims` | `DoorRoutine` + `LiftRoutine` (portes et ascenseurs, handshake d'arêtes) |
| `sim/LevelSim` | état runtime du niveau (arêtes, `edgeFlags`, hauteurs de zones) |
| `sim/SinCos` | table sinus d'origine (`bigsine`) — pas de `Math.sin` : les arrondis comptent |
| `sim/M68k` | sémantique 68k (mot signé, `muls`, `divs`) |
| `sim/MoveTest` | validation headless (traversée de zones, murs, portes) |

## Conventions de repère (vérifiées contre le port)

- X/Z : `/64`, **Z inversé** (`z_jme = -z_amiga`).
- Y : « plus négatif = plus haut » → `y_jme = -h/32` ; hauteurs de zone (.8) → `-h/8192` ;
  hauteurs de flat (`<<6`) → `-y/128`.
- Textures murales : bande de sous-tuiles, `[fromTile, fromTile+texW)` répétée ;
  `U = 0..wallLen/texW` ; `V = (Yéditeur + yOffset)/texH` **ancrée au monde**.
- Textures chargées avec `flipY=false` → `v=0` en haut de l'image (convention du port).

## Simulation

La logique portée tourne à **pas fixe de 50 Hz** (cadence PAL : toutes les constantes de vitesse et
de gravité sont « par frame »), le rendu jME tournant librement par-dessus. Ordre d'une frame,
identique au jeu d'origine : `plr_MouseControl` (angle) → `plr_KeyboardControl` (intention) →
`plr_Fall` (Y) → `Plr1_Control` (collision + zone) → `objmoveanim` (`LiftRoutine`, `DoorRoutine`).

Unités conservées : positions en 16.16 (la partie entière est la coordonnée vue par la collision),
hauteurs en virgule fixe (8192 = 1 m), angle en unités de la table sinus (8192 = tour complet).

Non porté faute de données ou de systèmes : eau, jetpack, dégâts de chute appliqués à l'entité,
bruits de pas et sons de portes, téléporteurs, collision objet-objet, accroupissement, joueur 2.
Chaque emplacement est marqué `TODO` dans le code.

## Éclairage

Les **murs** sont éclairés exactement comme dans le jeu d'origine, par pixel :

- extraction : `<TEXTURE>.idx.png` (le sélecteur de rampe 5 bits de chaque texel) et
  `<TEXTURE>.lut.png` (32 rampes × 32 blocs de luminosité) ;
- luminosité d'un coin : `|CurrentPointBrights[zone*40 + point*4 + slot]| + brightOffset`, puis
  `b = ext.w(coin - 300)`, puis `2*b` dans l'accumulateur gouraud ;
- par pixel : `niveau = clamp(b + dist>>7, 0, 64)`, `bloc = min(ceil(niveau/2), 31)`,
  couleur = `LUT[bloc][sr]`.

Vérifié contre le port (zone 2, `whichPBR=16` : coin 324 → b=24 → 48 → niveau 50 → bloc 25).
`-Pfullbright` désactive l'éclairage pour comparer. La correction gamma est désactivée : la carte
d'indices transporte des entiers, pas des couleurs.

Les **sols et plafonds** suivent leur propre modèle, lui aussi porté :

- `coin = |CurrentPointBrights[zone*40 + point*4 + slot]| - 300`, slot = 0 pour un sol, 1 pour un
  plafond ; le « point » est le rang du sommet dans le contour de la zone ;
- par pixel : `niveau = clamp(coin + dist>>9, 0, 30)`, couleur = `LUT[niveau][texel]` où la LUT est
  `shade[256*32 + niveau*256 + texel]` extraite en `floor_shade.lut.png` (256×31).

Vérifié : l'histogramme des couleurs de sol du remake est identique à celui de la capture du port.

Les **sprites ordinaires** (pickups, objets) ne sont **pas** éclairés dans le jeu d'origine : ils
sont dessinés tels quels (`pastobjscale` ne passe par le chemin éclairé que pour les objets marqués
« lumière », c'est-à-dire les monstres, et par le chemin additif pour les glares). Le rendu à plat
qu'on en fait est donc déjà fidèle.

Les **modèles vectoriels** sont éclairés par l'**anneau directionnel** d'origine
(`LightRings`, port de `draw_CalcBrightRings`) : autour de chaque objet, un anneau de 16 secteurs
reçoit la luminosité des points de bordure de sa zone et de ses voisines, 48 (sombre) pour chaque
mur plein, puis les trous sont interpolés. Le niveau d'une face vaut
`clamp(base + bas[s] + ((haut[s]-bas[s])*(vpos+73)>>10), 0, 31)`.

Vérification : `gradle -p rebirth/game lightTest -Pring=-808,184,3` reproduit exactement l'anneau
calculé par le portage.

Les **lumières animées** (`brightanim`) sont portées : sept séquences codées en dur dans le jeu
(cinq pulsations, deux scintillements) avancent d'un cran par frame ; les points de niveau qui les
suivent voient leur luminosité recalculée, et murs, sols et modèles sont re-éclairés à la volée
(`LevelBuilder.refreshLighting`). C'est ce qui manquait pour que l'anneau directionnel du niveau A
tombe exactement sur la valeur du portage.

Les **sprites de monstres** sont éclairés eux aussi (`SpriteLight`) : direction de lumière moyenne
déduite de l'anneau, grille 7×7 lue dans `guff` et tournée avec la vue, puis un niveau par groupe de
huit couleurs — la palette d'un sprite HQN est en effet organisée en 32 niveaux × 8 teintes, et
l'éclairage ne fait que choisir le niveau de chaque groupe. Les sprites utilisent l'anneau
« zone seule » (`LightRings.zoneOnly`), pas l'anneau complet des modèles.

Un détail du jeu qu'il faut reproduire : la liste de points de bordure d'une zone **déborde** sur la
zone suivante (elle est lue jusqu'au premier mot négatif, sans borne), et l'index de luminosité
déborde avec elle. Sans ça l'anneau des sprites diverge.

Parité : anneau et 29 niveaux **identiques au portage** sur le cas testé
(`gradle -p rebirth/game lightTest -Pring=5280,1600,47 -Pspr=0,-19`).

## Animation des objets

Chaque définition d'objet porte un script de 20 pas de 6 octets `{gfx, frame, mot2, delta, suivant}`
(`DEFANIMOBJ`). La durée n'est pas un compteur : elle est encodée par répétition des pas, et un pas
qui pointe sur lui-même fige l'objet. `ObjectAnim` avance le pas courant une fois par frame de
simulation et `LevelBuilder.stepObjectAnims()` applique le nouveau graphique.

Cinq définitions s'animent par défaut (Ventfan, Lampglare, RoofGlare, KnifeSwitch, GunGlares), soit
46 objets sur les 16 niveaux — le niveau B, avec ses 21 RoofGlare, est le bon banc d'essai. Le jeu
n'anime que les objets visibles ; faute de PVS on les anime tous, ce qui ne change que leur phase.

## Ramassage et clés

`sim/Inventory` + `sim/Pickups` portent l'inventaire (22 consommables, 12 items) et le ramassage.
Deux règles non évidentes du jeu :

- les **clés verrouillent** : `Anim_DoorAndLiftLocks_l` est reconstruit à chaque frame et tout
  objet-clé encore au sol y ajoute ses bits — la porte s'ouvre donc dès que la clé est ramassée
  (et `DoorRoutine` lit ce long en **mot**, ce sont les bits 16-31 qui portent les portes) ;
- la **hauteur d'un objet** vaut `(sol ou plafond de sa zone) >> 7 + 2 × delta du pas d'animation`,
  et sert à la fois au test de ramassage et à sa hauteur de dessin.

Les **interrupteurs** (`Activatable`) suivent la même logique inversée : au repos ils verrouillent
leurs portes ; actionnés (à portée + touche action), ils jouent leur animation active et relâchent
leurs verrous, jusqu'à expiration de leur `activeTimeout` ou une nouvelle pression.

Validation : `moveTest -Plevel=all` pose le joueur sur chaque objet ramassable puis sur chaque
interrupteur — **100 % pris sur les 16 niveaux**, et tous les verrous de portes retombent à zéro.

## Reste à faire

1. Le jeu proprement dit : ennemis (IA), armes, HUD, audio.
2. Murs des zones-ascenseurs : ils devraient s'étirer avec le sol (aujourd'hui seul le sol bouge).

Écart connu (extraction, pas rendu) : `PICKUPS/frame_28.png` n'est pas extrait — 12 objets
`def=0` sans nom du niveau A le référencent et ne sont donc pas affichés.
