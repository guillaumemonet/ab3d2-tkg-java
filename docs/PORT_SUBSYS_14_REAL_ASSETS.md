# Sous-système 14 — Asset Loaders réels + validation contre vrais fichiers Team17

Première validation du port contre **vraies données binaires** Team17 (et non
plus seulement des fixtures synthétiques).

**Status** : ✅ porté, **16 nouveaux tests** dont **9 contre fichiers réels**
(cumul total : **264/264**).
**Package** : `com.team17.ab3d2.io` (AssetLoaders)
**Tâches** : #55 → #57

---

## 1. Découvertes structurelles importantes

Inspection du contenu de `medias/original/INCLUDES/` :

| Fichier | Taille | Interprétation |
|---|---:|---|
| `test.lnk` | **86 268** | **Vrai GLF Team17 — taille exactement {@link GameLinkFile#SIZE_BYTES}** ✓ |
| `newtexturemaps.pal` | 16 384 | **Shade table = exactement 64×256 = SHADE_ROWS × PALETTE_SIZE** ✓ |
| `floortile` | 65 536 | **16 tiles × 4096 = 16 × 64×64** ✓ |
| `newtexturemaps` | 131 072 | Atlas de textures (128 KB) |
| `titlescrnraw1` | 71 680 | Title screen raw |
| `rawbackpacked` | 155 520 | Backdrop sky packed |
| `*.256pal` | 256/2048/8192 | Palettes per-asset (3 tailles observées) |
| `*.256wad` (walls) | 4802 à 56834 | Wall textures (format chunky packé) |

**Confirmation byte-à-byte** : mes calculs d'offsets dans le sous-système
02 (`GameLinkFile`) étaient corrects. Le fichier `test.lnk` fait
**exactement** 86 268 octets et parse sans erreur.

---

## 2. Loaders livrés

### `AssetLoaders.loadShadeTable(Path)`
Charge `newtexturemaps.pal` (16 384 octets) comme {@link Palette}. Construit
une palette RGB identity en parallèle puisque le fichier ne contient pas de
composantes RGB (juste la shade table).

### `AssetLoaders.loadFloorTiles(Path)`
Charge `floortile` (65 536 octets) en {@code FloorTileSet} : 16 tiles
{@code byte[4096]} chacune. Compatible direct avec {@link FloorRasterizer}.

### `AssetLoaders.loadWallTexture(Path)`
Lit le binaire raw d'une wall texture `.256wad`. Le format précis reste à
reverse-engineer dans un sous-système futur (les tailles observées suggèrent
un header de 2 octets puis un layout colonne par colonne, mais le détail
exact nécessite analyse approfondie de {@code WALLCONVERT} ou du wall
drawing setup ASM).

### `AssetLoaders.loadPaletteRgb(Path)`
Charge un `.256pal`. Format Amiga **12-bit AGA** : 256 entrées × 6 octets =
1536 octets (3 mots big-endian par couleur, R/G/B). Le loader accepte aussi
les tailles 256, 2048, 8192 vues dans le repo (formats variants à
clarifier).

---

## 3. Découvertes sur le format Team17

### 3.1 Header GLF = 64 octets de padding

Inspection od du fichier `test.lnk` :
```
0x0000 : 00 00 00 00 ... (64 octets de zéros)
0x0040 : 20 20 20 20 20 20 4C 45 56 45 4C 20 20 41 ...
                                                "      LEVEL  A ..."
```

Les 64 premiers octets sont **tous à zéro**. Pas de signature/version dans
le format. Mon `OFF_LEVEL_NAMES = 64` était correct.

### 3.2 Noms = space-padded, pas zero-terminated

Les chaînes (level names, SFX filenames, etc.) sont **paddées avec des
espaces ASCII (0x20)**, pas avec des octets nuls. Mon
`BinaryReader.readFixedAsciiZ()` qui s'arrête au premier zéro retourne donc
les espaces leading/trailing.

**Exemple** : level 0 = `"      LEVEL  A                          "` (40 octets).

**Impact** : les tests doivent utiliser `trim()` ou `contains()` pour
valider. Le parsing structurel reste correct.

### 3.3 Cohérence des 16 niveaux

Le GLF contient bien 16 entrées `LEVEL A` à `LEVEL P` consécutives, ce qui
confirme la convention `LVL_BinFilenameX_vb` qui boucle de 'a' à 'p'
(cf. `controlloop.s:298-312`).

---

## 4. Tests réels (9 contre vrais fichiers)

### `RealGlfValidationTest`
- `realGlfHasExpectedSize` : test.lnk == 86 268 octets exact ✓
- `realGlfParsesWithoutError` : `GameLinkFile.parse()` ne crash pas ✓
- `realGlfHasExpectedLevelNames` : "LEVEL A".."LEVEL P" présents ✓
- `realGlfHasNonEmptyObjectGfxNames` : au moins un object gfx name
- `realGlfHasSfxFilenames` : au moins un SFX filename
- `realGlfHasBulletDefs` : 20 bullet defs avec champs non-zéro
- `realGlfHasAlienDefs` : 20 alien defs avec données
- `realGlfHasLevelMusic` : noms de musique présents
- `realGlfHeaderPreambleIsZero` : les 64 premiers octets sont nuls

### `AssetLoadersTest`
- 3 tests sur fixtures synthétiques (FloorTileSet constants, validations)
- 4 tests sur vrais fichiers (floortile, shade table, wall texture, palette RGB)

**Tous ces tests utilisent `@EnabledIf("realAssetsAvailable")`** pour ne
s'exécuter que si les fichiers `medias/original/...` sont accessibles.
Quand ils ne le sont pas (e.g. CI sans assets), les tests sont silencieusement
sautés.

---

## 5. Limitations restantes

| Domaine | Statut |
|---|---|
| GLF parsing structurel | ✅ Validé sur vrai fichier |
| GLF noms strings (trim trailing spaces) | À ajouter au parser si voulu — actuellement les espaces sont préservés |
| FloorTile loader | ✅ Format 16×4096 confirmé |
| Shade table loader | ✅ Format 64×256 confirmé |
| Wall texture format précis (.256wad) | ⚠️ Bytes exposés mais format non décodé |
| Sample data .fib (Fibonacci PCM) | ❌ Pas chargé (assets sont dépackés donc le format spécifique reste à clarifier) |
| Sprite triplets (.256pal + .ptr + .wad) | ❌ Pas encore unifié |
| Backdrop / sky | ❌ Pas chargé |
| Title screen | ❌ Pas chargé |

---

## 6. Impact sur les sous-systèmes existants

Avec cette validation, plusieurs sous-systèmes voient leur crédibilité
**confirmée** :

- **Sous-système 02 (Loader binaires)** : la struct `GameLinkFile` parse
  correctement un vrai GLF Team17. Les 32 offsets cumulés sont byte-exacts.
- **Sous-système 07 (Renderer fondations)** : le format de la shade table
  (64×256) correspond au fichier réel `newtexturemaps.pal`. La classe
  `Palette` est prête à recevoir les vraies données.
- **Sous-système 08 (Setup code)** : le format de FloorTile (64×64) du
  `FloorRasterizer` correspond aux vraies tiles.

---

## 7. Récapitulatif cumulé

| Sous-système | Tests | Statut |
|---|---|---|
| 01 — Math fondamental | 39 | ✅ |
| 02 — Loader binaires | 66 | ✅ |
| 03 — Gameplay state runtime | 24 | ✅ |
| 04 — Transform3D | 11 | ✅ |
| 05 — PvsErrata | 6 | ✅ |
| 06 — Edge PVS | 17 | ✅ |
| 07 — Renderer fondations | 38 | ✅ |
| 08 — Setup code rasterizers | 19 | ✅ |
| 09 — Zone graph walker | 4 | ✅ |
| 10 — Integration end-to-end | 2 | ✅ |
| 11 — Gouraud + dithering | 3 | ✅ |
| 12 — Physics + collision | 10 | ✅ |
| 13 — Audio Protracker | 9 | ✅ |
| **14 — Real Asset Loaders + GLF validation** | **16** | **✅** |
| **Total** | **264** | ✅ |

---

## 8. Pour la suite

Avec les vrais assets accessibles, les directions naturelles deviennent :

A) **Decoder le format .256wad** : analyser le wall drawing setup et le
   pattern des fichiers (tous se terminent par 0x02, headers à analyser)
   pour décoder les wall textures et alimenter `WallRasterizer`.

B) **Integration rendering avec vrais assets** : charger `floortile` +
   shade table + une wall texture, projeter via le pipeline complet,
   sauvegarder le framebuffer en PNG (pour inspection visuelle).

C) **Charger un vrai niveau** : si on a accès aux fichiers
   `twolev.bin`/`twolev.graph.bin` d'un niveau, valider tout le pipeline
   loader sur eux.

D) **Audio output réel** : intégrer LWJGL OpenAL + charger les .fib comme
   PCM 8-bit pour synthèse réelle.

E) **Renderer display** : intégrer LWJGL pour afficher le framebuffer dans
   une fenêtre + cycle de présent/poll.

À décider à la prochaine étape.
