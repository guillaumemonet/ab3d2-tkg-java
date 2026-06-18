# Alien Breed 3D II : The Killing Grounds — Portage Java

Portage **Java fidèle** du moteur d'*Alien Breed 3D II : The Killing Grounds* (Team17, Amiga,
1996), depuis l'assembleur 68k et le C d'origine. Le jeu tourne en natif sur PC (Windows) via
LWJGL 3, **sans émulateur**.

> Porté par **Guillaume Monet**, avec l'assistance de Claude.

---

## 1. Présentation

Le moteur d'origine est écrit en assembleur 68000/68020/68060 et en C, et cible le matériel
Amiga (puces custom, blitter, copper, audio Paula, conversion chunky→planar « C2P »). Ce projet
le réécrit **ligne à ligne** en Java, en respectant la logique d'origine sans approximation :

- **Mémoire plate big-endian** — un unique tableau d'octets (`Mem.RAM`, 64 Mo) émule la RAM 68k.
  Tous les accès passent par des helpers typés (`Mem.b/w/l`, `Mem.ub/uw`, `Mem.wb/ww/wl`…) qui
  reproduisent l'ordre des octets et l'arithmétique du 68k.
- **Cible 68060 + RTG** — on ne porte que la branche du processeur le plus rapide et l'affichage
  *chunky* (RTG / carte graphique). Les variantes CPU/résolution et la conversion **C2P (planar)
  sont volontairement exclues** : le moteur rend directement dans un tampon chunky présenté par
  l'hôte.
- **Couche hôte** — l'afficheur, la boucle de jeu, l'entrée clavier/souris et l'audio sont
  réimplémentés au-dessus de **GLFW / OpenGL / OpenAL** (LWJGL 3), en lieu et place des puces
  custom Amiga.

### Fonctionnalités portées

- Rendu 3D temps réel des niveaux (murs texturés, sols/plafonds, éclairage, gouraud).
- Objets/sprites, aliens et leur IA, armes et tir, ramassages, portes/ascenseurs/interrupteurs.
- HUD texturé, messages en jeu, carte.
- Audio : musique (ProTracker) + effets sonores (mixage logiciel façon Paula).
- **Menu complet** : écran de feu animé, navigation, sous-menus options/contrôles, save/load,
  sliders/cyclers, persistance des préférences.
- **Texte narratif d'intro** de chaque niveau (police proportionnelle, rendu fidèle).
- Transition de téléport + musique en fin de niveau.
- **Sélection d'arme** : cycle (`\`) **et sélection directe par les touches `1`…`9`,`0`**.

### Limitations connues

- **Mode 2 joueurs** non disponible (le lien série Amiga d'origine n'est pas porté ; un mode
  réseau TCP local est prévu — voir *Feuille de route*).
- Plateforme de développement : **Windows x64** (natives LWJGL `natives-windows`). D'autres
  plateformes nécessitent d'ajouter le classifier de natives correspondant.

---

## 2. Prérequis

| Composant | Version |
| --- | --- |
| **JDK** | 21 (toolchain Gradle configurée sur Java 21) |
| **Gradle** | 9.x (ou utiliser le wrapper si présent) |
| **OS** | Windows x64 (natives LWJGL `natives-windows`) |
| **GPU** | OpenGL (pilote standard) |

Les dépendances LWJGL (GLFW, OpenGL, OpenAL) sont récupérées automatiquement depuis Maven Central
au premier build.

### Données du jeu

Le moteur lit les **assets dépackés** depuis `medias/original/` (résolution des assigns Amiga
`ab3:` / `-I media`). Ce dossier doit contenir les données du jeu (palette `256pal`, `levels/`,
`includes/`, etc.). Il est résolu **par rapport au répertoire racine du projet** (parent de
`java/`), c'est pourquoi toutes les tâches Gradle s'exécutent depuis cette racine.

---

## 3. Structure du projet

```
ab3d2-tkg-new/
├── README.md            ← ce fichier
├── medias/original/     ← assets dépackés du jeu (palette, niveaux, includes, samples…)
├── ab3d2_source/        ← sources ASM/C de référence (lecture seule)
├── docs/                ← documentation d'architecture et notes de portage (PORT_SUBSYS_*)
├── run/                 ← données générées à l'exécution (prefs.cfg, sauvegardes)
└── java/
    ├── build.gradle     ← build + tâches d'exécution/test
    └── src/ab3d2/
        ├── *.java        ← cœur du moteur (Hires, Controlloop, Plr*control, Objdraw…)
        ├── c/            ← portage des fichiers C (ScreenC, DrawC, MenuC, GameC…)
        ├── modules/      ← sous-systèmes (Player, Res, FileIo, RawKeyMacros…)
        ├── data/         ← sections de données initialisées (tables, polices, menus)
        ├── bss/          ← sections BSS (buffers, KeyMap…)
        ├── menu/         ← moteur de menu (Menunb)
        ├── host/         ← couche hôte LWJGL : Main, Display, Input, CustomChips + harnais de test
        └── tools/        ← outillage (CheckLayout : garde-fou de disposition mémoire)
```

---

## 4. Compilation & exécution

Toutes les commandes se lancent **depuis le dossier `java/`** (ou avec `-p java` depuis la racine).

### Lancer le jeu

```bash
cd java
gradle run
```

Le point d'entrée est `ab3d2.host.Main` (équivalent du `main.c` d'origine). Le jeu démarre sur le
menu : choisis un niveau en solo, le texte d'intro s'affiche, puis le niveau se charge.

### Compiler seulement

```bash
gradle -p java compileJava
```

### Build redistribuable (Windows)

Produit une **app-image portable** : un dossier autonome contenant l'exécutable, un **JRE
embarqué** (rien à installer côté utilisateur) et les assets du jeu.

```bash
gradle -p java packageApp
```

Résultat : `java/build/jpackage/AlienBreed3D2-TKG/` — lancer `AlienBreed3D2-TKG.exe`.

- Nécessite `jpackage` (inclus dans le JDK qui exécute Gradle).
- Le dossier est **déplaçable** : `Main` résout les assets (`medias/original`) et le dossier
  d'écriture (`run/`) **relativement à l'emplacement de l'exécutable**. On peut aussi forcer ces
  chemins avec `-Dab3d2.dataDir=...` / `-Dab3d2.runDir=...`.
- ⚠️ Les assets embarqués appartiennent à Team17 : ce paquet est réservé à un **usage personnel /
  possesseurs du jeu**, pas à une diffusion publique.
- Build **Windows x64** uniquement (natives LWJGL). Pour d'autres OS, ajouter le classifier de
  natives correspondant dans `build.gradle`.

### Diagnostic (gel / logs)

L'app-image est **sans console** : `Main` redirige donc `stdout`/`stderr` vers
`<App>/run/ab3d2.log`. En cas de **gel**, un *watchdog* surveille le rythme des frames et, si plus
aucune frame ne passe pendant 5 s, écrit dans le log la **pile de tous les threads** (cherche
`"main"` → c'est la boucle où le moteur est bloqué).

- `-Dab3d2.watchdogMs=N` : seuil de détection (ms ; `0` = désactive le watchdog).
- `-Dab3d2.log=chemin` : fichier de log (`off` = garder la console).
- Les `OutOfMemoryError` éventuels sont journalisés et déclenchent un *heap dump* (le build fixe
  `-Xms256m -Xmx1g`). Un **gel n'est pas un OOM** : un manque de mémoire produit une erreur tracée,
  pas un blocage silencieux.
- Pour un debug **en direct**, lancer depuis un terminal `gradle -p java run` (la console est
  conservée en mode dev).

### Vérifier l'intégrité du portage

`checkLayout` valide la disposition mémoire (offsets des structures) — garde-fou de
non-régression. Il **doit afficher « TOUT OK »**.

```bash
gradle -p java checkLayout
```

---

## 5. Contrôles

### Déplacement & combat (valeurs par défaut, remappables dans le menu Options › Contrôles)

| Touche | Action |
| --- | --- |
| `W` / `S` | Avancer / Reculer |
| `←` / `→` | Tourner à gauche / droite |
| `A` / `D` | Pas de côté gauche / droite |
| `Ctrl` | Tirer |
| `F` | Actionner (portes, interrupteurs) |
| `Shift` (gauche) | Courir |
| `Alt` (gauche) | Forcer le pas de côté (strafe) |
| `C` | S'accroupir |
| `Espace` | Sauter |
| `=` / `-` | Regarder en haut / en bas |
| `;` | Recentrer la vue |
| `L` | Regarder derrière |

### Armes

| Touche | Action |
| --- | --- |
| `\` | Arme suivante (cycle parmi les armes possédées) |
| `1` … `9`, `0` | **Sélection directe** d'une arme (si possédée) |

### Touches système

| Touche | Action |
| --- | --- |
| `Échap` | Quitter le niveau |
| `P` | Pause |
| `Tab` | Carte |
| `F7` | Cycle de la limite de FPS |
| `F10` | Bascule plein écran / petit écran (HUD) |

> Le mapping clavier est **physique** (la touche `W` de l'hôte → `RAWKEY_W`) ; les bindings de jeu
> sont ensuite appliqués sur ces rawkeys par le moteur, exactement comme sur Amiga.

---

## 6. Harnais de développement

Plusieurs tâches Gradle court-circuitent le menu pour tester des sous-systèmes isolément.

### Menu

```bash
# Menu interactif
gradle -p java menuTest

# Capture d'un écran de menu (N frames) → menu_screenshot.png
gradle -p java menuTest -PmenuFrames=120 -PmenuShow=main      # main|custom|controls|level|load|save|demo

# Capture du texte d'intro d'un niveau → intro_screenshot.png
gradle -p java menuTest -PintroText=0

# Test de persistance des préférences (sans fenêtre)
gradle -p java menuTest -PprefsTest=1
```

### Niveau (rendu direct)

```bash
# Charge un niveau et le rend : -PlvlArgs="niveau frames angle"
gradle -p java levelTest -PlvlArgs="0 300 0"
```

`levelTest` accepte de nombreux `-P` de diagnostic, par ex. : `-PshowMap`, `-Pfullbright`,
`-Pfov=N`, `-PfullScreen=1`, `-PtestWeapons=1`, `-PtestPickup=1`, `-PtestSfx=1`, `-PtestMsg=1`,
`-PtelefxFrame=N`, `-PwavOut=fichier.wav`, `-PdumpObjects=1`, `-PforceFrame=N`, `-PaltTex=1`…
(voir `java/build.gradle` pour la liste complète).

### Affichage

```bash
# Valide la chaîne d'affichage LWJGL : fenêtre + palette + buffer chunky de test
gradle -p java displayTest
```

---

## 7. Notes d'architecture

- **`Mem`** : mémoire 68k émulée (tableau plat big-endian) + helpers d'accès et d'initialisation
  des sections data/bss (`dcB/dcW/dcL/dcStr/incbin/alloc/align`).
- **`M68k` / `Macros`** : helpers reproduisant des opérations 68k (extensions de signe, etc.).
- **Affichage** : le moteur rend dans `Vid_FastBufferPtr_l` (cible chunky). `ScreenC.Vid_Present`
  compose le HUD et présente l'image ; `Vid_PresentMenu` gère le chemin planar→chunky du menu.
  L'hôte (`host/Display`) pousse le tampon ARGB via OpenGL.
- **Entrée** : `host/Input` traduit les évènements GLFW en rawkeys Amiga écrits dans `KeyMap_vb`,
  que lisent les routines de contrôle (`Plr*control` → `modules/Player`).
- **Audio** : mixage logiciel des canaux façon Paula, sortie via OpenAL.
- **Fichiers** : `modules/FileIo` + `DosLib` réimplémentent les I/O AmigaDOS ; les écritures
  (préférences, sauvegardes) vont dans `run/`.

La documentation détaillée du portage par sous-système se trouve dans `docs/` (`ARCHITECTURE.md`,
`PORT_SUBSYS_01..20.md`, `PVS.md`).

---

## 8. Feuille de route

- [ ] **Mode 2 joueurs** via socket TCP local/LAN (remplace le lien série Amiga ; protocole
  lock-step longword).
- [ ] Chargement des sauvegardes par niveau (`DEFGAME`).
- [ ] Crédits du jeu (`mnu_viewcredz`, désactivé dans l'original).
- [ ] Portage sur d'autres plateformes (ajout des natives LWJGL Linux/macOS).

---

## 9. Licence & crédits

*Alien Breed 3D II : The Killing Grounds* et ses données sont la propriété de **Team17**. Ce
projet est un portage **non commercial** à but d'étude/préservation. Les assets du jeu
(`medias/`) ne sont pas redistribués avec le code source et restent soumis à leurs droits
d'origine.

Moteur d'origine : Team17 (Andy Clitheroe et al.). Portage Java : **Guillaume Monet**.
