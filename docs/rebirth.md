# rebirth

Réécriture moderne d'Alien Breed 3D II (full 3D). **Étape en cours : extraction des assets
d'origine vers des formats modernes** — le portage fidèle (`../java`) sert de LECTEUR
autoritatif des formats d'origine (il les décode déjà correctement), on sérialise en PNG/JSON/OBJ.

## Principe

```
medias/original (assets d'origine)
        │  chargés + décodés par le portage fidèle (boot headless : Bss + DataSections + GLF)
        ▼
  rebirth/src (extracteurs)  →  rebirth/assets (formats modernes)
```

Aucune fenêtre : la palette (256pal) et les données statiques sont montées par
`DataSections.init()`, la base GLF par `IO_LoadFile`. Voir `PortReader`.

## Lancer

```
gradle -p rebirth extract                 # tout
gradle -p rebirth extract -Pwhat=textures # une catégorie
gradle -p rebirth extract -Pvblk=N        # luminosité baké des textures murales (0=plus clair)
```

## Structure de sortie (`rebirth/assets/`)

```
textures/
  walls/<NOM>.png     # 16 textures murales, nom d'origine (STONEWALL.png…)
  floors/<NOM>.png    # (à venir)
  objects/<NOM>.png   # sprites (à venir)
  sky/backdrop.png    # ciel (rawbackpacked)
palette.png           # palette de référence (256×1)
levels/<lettre>.json  # (à venir) géométrie secteurs + objets + portes
models/<NOM>/frame_###.obj  # (à venir) vectobj, un OBJ par frame (format universel)
glf.json              # (à venir) base GLF : défs objets/aliens, noms, anims
```

## Formats

- **Images** : palette-indexé → PNG RGB. Noms d'origine conservés (le mapping complet sera
  aussi dans `glf.json`). Textures murales décodées depuis le format shade-table + texels.
- **Niveaux** : JSON (zones floor/roof/upper/water, arêtes + join, points, murs+texIndex,
  portes/ascenseurs, instances d'objets).
- **Modèles (vectobj)** : OBJ, **un fichier par frame** (`frame_000.obj`, …) — format le plus
  simple et universel, lisible par n'importe quel outil 3D.

## État

- [x] Base GLF → JSON (`glf.json` : levels/walls/objects/vectorModels/spriteSheets/aliens/guns/bullets/sfx/files)
- [x] Textures murales → PNG (13 présentes, noms d'origine, dimensions réelles)
- [x] Ciel (backdrop) → PNG
- [x] Palette → PNG
- [x] Textures de sol → PNG (FLOORTILE = **16 tuiles** 64×64 = 4 banques × 4 entrelacées ;
      texel = pal[base + b*256 + k + (T*256+S)*4] ; + `floortile_atlas.png` 4×4)
- [x] Sprites objets/aliens → PNG avec transparence (`Sprites.java` : 14 feuilles, 269 frames ;
      colonnes/strips, 3 formats de pixel, tx=0 transparent, palette propre à la feuille)
- [x] Niveaux → JSON (`LevelExport.java` : 16 niveaux ; zones floor/roof/upper + brightness + arêtes/join,
      points, **murs+texIndex** (449 niv. A = réf jme3d), objets+noms, départs joueurs, exitZone)
- [x] vectobj (vectorModels) → OBJ + UV + MTL (`VectObjExport.java` : 22 modèles, un/frame ; vertices ÷128
      Y/Z inversés, faces = ptIdx des parts visibles/frame (numVerts=numLines+1), UV mappant l'atlas)
- [x] Atlas texture-maps → `textures/texturemaps_atlas.png` (256×512, 8 strips banque×slot)
- [x] Portes/ascenseurs dans les niveaux JSON (`doors`/`lifts` : course, vitesses, zone, murs contrôlés)

**EXTRACTION COMPLÈTE** — tous les assets d'origine sont en formats modernes. Prochaine étape : le moteur 3D.

**Note formats** : les `vectorModels`/`spriteSheets` du glf.json sont des LISTES DE RESSOURCES
(fichiers indexés 0..n) ; un objet/alien y référence son graphisme par INDEX (via l'anim), pas
par position dans la table des noms d'objets.
