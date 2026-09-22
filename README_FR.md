# Alien Breed 3D II : The Killing Grounds — Portage Java

![Alien Breed 3D II : The Killing Grounds](docs/img/the_killing_grounds_full_hd.jpg)

Portage **Java fidèle** du moteur d'*Alien Breed 3D II : The Killing Grounds* (Team17, Amiga,
1996), depuis l'assembleur 68k et le C d'origine. Réécriture **ligne à ligne**, sans émulateur,
exécutée en natif sur PC via LWJGL 3.

> Porté par **Guillaume Monet**, avec l'assistance de Claude.

*[English version](README.md)*

![Le moteur d'origine](docs/img/classic-niveau.png)

*Le niveau A, rendu par le moteur d'origine porté — 320×256, comme sur Amiga.*

---

## 1. Principe

Le moteur d'origine est écrit en assembleur 68000/68020/68060 et en C, et cible le matériel
Amiga (puces custom, blitter, copper, audio Paula, conversion chunky→planar « C2P »). Ce projet
le réécrit **ligne à ligne** en Java, sans approximation :

- **Mémoire plate big-endian** — un unique `byte[] Mem.RAM` (64 Mo) émule la RAM 68k. Tous les
  accès passent par des helpers typés (`Mem.b/w/l`, `Mem.ub/uw`, `Mem.wb/ww/wl`…) qui
  reproduisent l'ordre des octets et l'arithmétique du 68k. Registres `d0-d7`/`a0-a6` → `int`.
- **Helpers 68k** — `M68k` (`swap`, `muls/mulu`, `divs/divu`, `asrw`, extensions de signe…)
  reproduit les idiomes du CPU, y compris ses pièges : `divs.w` renvoie `(reste << 16) | quotient`,
  et oublier le reste a déjà coûté un bug d'IA entier.
- **Cible 68060 + RTG** — on ne porte que la branche du processeur le plus rapide et l'affichage
  *chunky*. Les variantes CPU/résolution et la **conversion C2P sont volontairement exclues** :
  le moteur rend directement dans un tampon chunky présenté par l'hôte.
- **Couche hôte LWJGL 3** (GLFW / OpenGL / OpenAL) — remplace les puces custom Amiga pour
  l'affichage, la boucle de jeu, l'entrée clavier/souris et l'audio.
- **Deux moteurs, une seule simulation** — voir §2.

### Fonctionnalités portées

- Rendu 3D temps réel des niveaux (murs texturés, sols/plafonds, éclairage, gouraud).
- Objets/sprites, aliens et leur IA, armes et tir, ramassages, portes/ascenseurs/interrupteurs.
- HUD texturé, messages en jeu, carte automatique.
- Audio : musique (ProTracker) + effets sonores (mixage logiciel façon Paula).
- **Menu complet** : écran de feu animé, navigation, sous-menus options/contrôles, save/load,
  sliders/cyclers, persistance des préférences.
- **Texte narratif d'intro** de chaque niveau (police proportionnelle, rendu fidèle).
- Transition de téléport + musique de fin de niveau.

### Limitations connues

- **Mode 2 joueurs** non disponible (le lien série Amiga n'est pas porté ; un mode TCP local est
  prévu — voir §8).
- Plateforme de développement : **Windows x64** (natives LWJGL `natives-windows`).

---

## 2. Le remake full 3D, ailleurs

Ce dépôt ne contient que le **portage fidèle** : la traduction ligne à ligne du moteur d'origine,
qui reste la seule version complète et exacte.

Le remake **full 3D** sur jMonkeyEngine est un projet à part entière :
**[ab3d2-tkg-rebirth](https://github.com/guillaumemonet/ab3d2-tkg-rebirth)**. Il n'est pas un portage de plus mais un autre moteur — il reconstruit
les niveaux en vraie 3D et rejoue la même logique de jeu. Il s'appuie sur ce dépôt de deux façons :

- comme **lecteur** autoritatif des formats d'origine, pour extraire les assets ;
- comme **oracle** : quand un comportement du remake est douteux, c'est contre ce portage qu'on
  tranche, en instrumentant les deux et en comparant les traces.

Ce dépôt-ci n'en dépend pas : il se compile et tourne seul.

## 3. Les données : les disquettes, et rien d'autre

**Aucun asset du jeu n'est versionné ici**, et le build redistribuable n'en embarque aucun non
plus. La seule source est le jeu de cinq images `.adf`, dans un dossier `adf/`.

Si elles manquent, le jeu **propose de les télécharger** depuis [Dream17](https://dream17.abime.net),
le site de préservation du catalogue Amiga de Team17 (archive ZIP d'environ 3,4 Mo). Rien n'est
téléchargé sans un oui explicite. Puis il monte les disquettes, dépacke ce qui doit l'être et
écrit un cache ; ensuite il lit ce cache.

```bash
gradle -p java fetchDisks    # récupérer les disquettes à la main
## 4. Prérequis

| Composant | Version |
| --- | --- |
| **JDK** | 21 (toolchain Gradle configurée sur Java 21) |
| **Gradle** | 9.x |
| **OS** | Windows x64 (natives LWJGL `natives-windows`) |
| **GPU** | OpenGL |
| **Données** | les cinq `.adf` du jeu, dans un dossier `adf/` |

Les dépendances LWJGL (GLFW, OpenGL, OpenAL) sont récupérées depuis Maven Central au premier build.

---

## 5. Structure du projet

```
java/                       ← racine du dépôt
├── README.md             version anglaise
├── README_FR.md          ce fichier
├── build.gradle
├── docs/                   architecture et notes de portage (PORT_SUBSYS_*, PVS.md)
├── resources/incbin/       les incbin liés au binaire d'origine (cf. §3)
├── run/                    généré à l'exécution (préférences, sauvegardes, journal)
└── src/ab3d2/
    ├── *.java              cœur du moteur (Hires, Controlloop, Plr*control, Objdraw…)
    ├── c/                  portage des fichiers C (ScreenC, DrawC, MenuC, GameC…)
    ├── modules/            sous-systèmes (Player, Res, FileIo, RawKeyMacros…)
    ├── data/               sections de données initialisées (tables, polices, menus)
    ├── bss/                sections BSS (buffers, KeyMap…)
    ├── menu/               moteur de menu (Menunb)
    ├── host/               couche LWJGL, lecture des disquettes, dépacking =SB=
    └── tools/              outillage (CheckLayout, AdfCheck, Depack, SkyDump)
```

---

## 6. Compilation & exécution

```bash
gradle -p java run             # le jeu
gradle -p java compileJava
```

### Build redistribuable (Windows)

Produit une **app-image portable** : un dossier autonome avec l'exécutable et un **JRE
embarqué**, et *sans aucune donnée du jeu*.

```bash
gradle -p java packageApp
```

Résultat : `java/build/jpackage/AlienBreed3D2-TKG/` — lancer `AlienBreed3D2-TKG.exe`. Le dossier
est déplaçable : les chemins sont résolus relativement à l'exécutable.

```
AlienBreed3D2-TKG/
├── AlienBreed3D2-TKG.exe
├── LISEZMOI.txt
├── adf/        les disquettes (vide au départ)
├── app/        les jars
├── run/        réglages et sauvegardes
└── runtime/    le JRE embarqué
```

Au **premier lancement**, le jeu constate que les données manquent et propose de télécharger les
disquettes ; il les dépacke ensuite tout seul dans `medias/`. Comme rien de Team17 n'est
redistribué, le paquet peut circuler tel quel.

### Diagnostic (gel / logs)

L'app-image est sans console : `Main` redirige `stdout`/`stderr` vers `<App>/run/ab3d2.log`. En
cas de gel, un *watchdog* écrit la pile de tous les threads après 5 s sans frame (chercher
`"main"`).

- `-Dab3d2.watchdogMs=N` — seuil (`0` désactive) ; `-Dab3d2.log=chemin` (`off` garde la console).
- `-Dab3d2.assets=…` force la racine des assets, `-Dab3d2.adf=…` le dossier des disquettes.

### Vérifier l'intégrité du portage

```bash
gradle -p java checkLayout    # disposition mémoire — doit afficher « TOUT OK »
gradle -p java adfCheck       # dépacking des disquettes — doit afficher PASS
```

---

## 7. Contrôles

Remappables dans le menu Options › Contrôles.

| Touche | Action |
| --- | --- |
| `W` / `S` | Avancer / Reculer |
| `←` / `→` | Tourner à gauche / droite |
| `A` / `D` | Pas de côté gauche / droite |
| `Ctrl` | Tirer |
| `F` | Actionner (portes, interrupteurs) |
| `Shift` gauche | Courir |
| `Alt` gauche | Forcer le pas de côté |
| `C` / `Espace` | S'accroupir / Sauter |
| `=` / `-` / `;` | Regarder haut / bas / recentrer |
| `L` | Regarder derrière |
| `\` , `1`…`9`,`0` | Arme suivante, sélection directe |
| `Échap` / `P` / `Tab` | Quitter le niveau / Pause / Carte |
| `F7` / `F10` | Limite de FPS / Plein écran |

> Le mapping clavier est **physique** (`W` de l'hôte → `RAWKEY_W`) ; les bindings de jeu sont
> appliqués sur ces rawkeys par le moteur, exactement comme sur Amiga.

---

## 8. Avancement

| Sous-système | Statut |
| --- | --- |
| Infra `Mem` / `M68k` / `Assets` | ✅ `gradle checkLayout` « TOUT OK » |
| Rendu : murs, sols/plafonds, gouraud, PVS | ✅ `gradle levelTest` |
| Objets, sprites, modèles vectoriels | ✅ |
| Aliens : IA, animations, ligne de vue, dégâts | ✅ `gradle alienTest` |
| Joueur : déplacement, collision, capacités | ✅ `gradle moveTest` |
| Armes, tir, projectiles, impacts | ✅ `gradle shotTest` |
| Portes, ascenseurs, interrupteurs | ✅ |
| HUD, messages, carte automatique | ✅ |
| Menu complet + préférences persistées | ✅ `gradle menuTest` |
| Textes d'intro et de fin | ✅ |
| Audio : ProTracker + effets façon Paula | ✅ |
| Build redistribuable (app-image jpackage) | ✅ `gradle packageApp` |
| Dépacking `=SB=` + lecture des disquettes | ✅ `gradle adfCheck` — 313/313 |
| Mode 2 joueurs (TCP local, remplace le lien série) | ⏳ |
| Chargement des sauvegardes par niveau (`DEFGAME`) | ⏳ |
| Portage Linux/macOS (natives LWJGL) | ⏳ |

Le remake full 3D a son propre suivi dans [ab3d2-tkg-rebirth](https://github.com/guillaumemonet/ab3d2-tkg-rebirth).

---

## 9. Notes d'architecture

- **`Mem`** : mémoire 68k émulée (tableau plat big-endian) + helpers d'initialisation des
  sections data/bss (`dcB/dcW/dcL/dcStr/incbin/alloc/align`).
- **Affichage** : le moteur rend dans `Vid_FastBufferPtr_l` (cible chunky). `ScreenC.Vid_Present`
  compose le HUD et présente l'image ; `host/Display` pousse le tampon ARGB via OpenGL.
- **Entrée** : `host/Input` traduit les évènements GLFW en rawkeys Amiga écrits dans `KeyMap_vb`,
  que lisent les routines de contrôle (`Plr*control` → `modules/Player`).
- **Audio** : mixage logiciel des canaux façon Paula, sortie OpenAL.
- **Fichiers** : `modules/FileIo` + `host/DosLib` réimplémentent les I/O AmigaDOS ; les écritures
  (préférences, sauvegardes) vont dans `run/`.

La documentation détaillée par sous-système est dans `docs/` (`ARCHITECTURE.md`,
`PORT_SUBSYS_01..20.md`, `PVS.md`).

### Méthode

Une règle a gouverné tout le projet : **porter l'ASM littéralement, jamais approximer**. Quand un
comportement diverge, on n'argumente pas — on **instrumente les deux moteurs et on compare les
traces**. Quelques exemples de ce que cette méthode a débusqué :

- Les aliens se regroupaient tous au même endroit : `divs.w` renvoie `(reste << 16) | quotient`
  et le reste était jeté, donc chaque monstre tirait « au hasard » le point de contrôle 0.
- Les monstres devenaient increvables : le jeu tient **deux** compteurs de dégâts distincts, le
  cumul permanent (`AI_Damaged_vw`) et un champ du workspace de ronde que `ai_ProwlFly` remet à
  zéro **à chaque frame**. Les confondre ne laissait mourir que ceux qui encaissaient quatre fois
  leurs points de vie en une seule frame.
- Les portes n'étaient pas des volumes : `DoorRoutine` écrit la position du battant dans le flat
  de plafond de sa zone, et ce dessous-là ne bougeait pas.
- Les escaliers superposés du niveau C manquaient : une zone porte **deux** flux de géométrie,
  l'extracteur n'en lisait qu'un.

---

## 10. Licence & crédits

*Alien Breed 3D II : The Killing Grounds* et ses données sont la propriété de **Team17**. Ce
projet est un portage **non commercial** à but d'étude et de préservation. Les assets du jeu ne
sont **pas** redistribués avec ce dépôt : il faut posséder le jeu et fournir ses disquettes.

Moteur d'origine : Team17 (Andy Clitheroe et al.). Sources ASM/C de référence :
[mheyer32/alienbreed3d2](https://github.com/mheyer32/alienbreed3d2). Portage Java :
**Guillaume Monet**.
