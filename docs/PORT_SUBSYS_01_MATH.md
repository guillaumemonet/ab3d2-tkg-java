# Sous-système 01 — Math fondamental

Premier sous-système porté vers Java. Tables précalculées et helpers d'arithmétique entière utilisés par **tout** le reste du moteur (transformation 3D, rasterisation, AI, physique).

**Status** : ✅ porté, 39 tests de parité passent.
**Package** : `com.team17.ab3d2.math`
**Tâches** : #11 → #16 (terminées).

---

## 1. Périmètre

Trois tables / classes :

| Classe Java | Origine ASM/C | Rôle |
|---|---|---|
| `SinCosTable` | `SinCosTable_vw` (binaire `bigsine`, déclaré `data/tables_data.s:9-10`), macros `sinw`/`cosw` (`c/math25d.h:43-49`) | Lookup trigonométrique Q15 |
| `ConstantTable` | `ConstantTable_vl` (déclaré `bss/tables_bss.s:26`), construction dans `hires.s:151-170` | Table de réciproques précalculée |
| `DivThreeTable` | `DivThreeTable_vb` (`data/tables_data.s:34-42`) | Division par 3 + reste précalculée |

Tout est statique, immutable, sans dépendance externe (JDK pur).

---

## 2. SinCosTable

### 2.1 Analyse ASM/C détaillée

#### Déclarations canoniques

**`data/tables_data.s:6-22`** :

```asm
MAX_ONE_OVER_N  EQU 511

; sine/cosine << 15, contains two full cycles (720 degrees) over 8192 entries
        DCLC SinCosTable_vw
                incbin  "bigsine"

; the size of one complete cycle - not the actual size of the table
SINE_SIZE       EQU 4096

SINE_OFS        EQU 0
COSINE_OFS      EQU (SINE_SIZE/2)             ; = 2048 (OFFSET EN OCTETS, pas en index)

; Modulus mask value when doing *address* based calculation, e.g. (a0, dN.w)
SINTAB_MASK_ADR EQU (SINE_SIZE*2)-2           ; = 0x1FFE

; Modulus mask value when doing *index* based calculation, e.g. (a0, dN.w*2)
SINTAB_MASK_IDX EQU (SINE_SIZE*2)-1           ; = 0x1FFF

; Angle modulus (address mask)
AMOD_A          MACRO
                and.w   #SINTAB_MASK_ADR,\1
                ENDM

; Angle modulus (index mask)
AMOD_I          MACRO
                and.w   #SINTAB_MASK_IDX,\1
                ENDM
```

**`c/math25d.h:39-49`** :

```c
#define SINTAB_SIZE 8192

extern WORD const SinCosTable_vw[SINTAB_SIZE];

static inline WORD sinw(WORD a) {
    return SinCosTable_vw[(a & (SINTAB_SIZE - 2)) >> 1];
}

static inline WORD cosw(WORD a) {
    return SinCosTable_vw[((a + SINTAB_SIZE / 4) & (SINTAB_SIZE - 2)) >> 1];
}
```

**`hires.s:3214-3224`** (site d'usage du début du rendu) :

```asm
; bigsine is 16kb = 8192 words for 4pi (720deg)
; --> 4096 words per 2pi
; --> 1024 words = 2048byte per 90deg

move.l #SinCosTable_vw, a0
move.w Vis_AngPos_w, d0
move.w (a0, d0.w), d6      ; sin
adda.w #COSINE_OFS, a0     ; a0 += 2048 bytes = 1024 words = 90°
move.w (a0, d0.w), d7      ; cos
move.w d6, Vis_SinVal_w
move.w d7, Vis_CosVal_w
```

#### Modèle mémoire

- **Taille** : 8192 mots (= 16 384 octets), `WORD` = signed 16-bit big-endian.
- **Échelle** : Q15 (sin = 1.0 → 32767). Cf. commentaire « sine/cosine << 15 ».
- **Couverture** : 4π radians (720° = deux cycles complets). Un cycle = 4096 mots.
- **Layout** : table[0..4095] = premier cycle 0→360°, table[4096..8191] = second cycle 360→720° (identique au premier).

**Raison de la double-période** : permettre à l'ASM d'appliquer un offset `+COSINE_OFS = +2048 bytes = +90°` sans risquer de débordement de table, sans remasquage. Si la table ne faisait qu'un cycle (4096 mots), l'addition `(a + 2048)` pour un angle proche de 360° dépasserait — la duplication absorbe ce cas.

#### Convention angulaire

- **8192 unités angulaires = 1 tour complet** (360°). Cf. directions documentées dans `docs/README.md` :

| Direction | Angle | sin | cos |
|---|---|---|---|
| Nord | 0 | 0 | 32767 |
| Est | 2048 | 32767 | 0 |
| Sud | 4096 | 0 | −32767 |
| Ouest | 6144 | −32767 | 0 |

- **Résolution** : 360° / 8192 ≈ 0.0439° par unité.
- **Incohérence à noter** : `docs/README.md` mentionne aussi « divides the full circle into 4096 », ce qui contredit la table des directions. La vérité du code est **8192 = un tour** ; le « 4096 » de la doc se réfère probablement au nombre d'entrées de table par cycle (`SINE_SIZE`).

#### Macros vs ASM : différence d'accès

| Aspect | Macros C `sinw`/`cosw` | ASM direct `(a0, d0.w)` |
|---|---|---|
| Masquage | À chaque appel : `(a & 0x1FFE) >> 1` | Présupposé pair, borné, déjà masqué via `AMOD_A` en amont |
| Index final | Toujours dans [0, 4095] | Peut atteindre [0, 8190 bytes] = [0, 4095 mots] pour sin ; [1024, 5119 mots] pour cos via `+COSINE_OFS` |
| Cycle lu | Toujours le premier (0..4095) | Sin → premier cycle, cos → premier ou second selon angle |

Les deux conventions produisent les **mêmes valeurs** grâce à la duplication. Le port Java suit la convention des macros C (masquage explicite à chaque appel) — plus défensive, JIT-friendly, pas de risque sur des entrées non bornées.

### 2.2 Pseudocode

```
classe statique SinCosTable :
    CONSTANTES :
        SIZE = 8192                    # mots dans la table
        SINE_SIZE = 4096               # entries par cycle 360°
        COSINE_OFS_BYTES = 2048
        COSINE_OFS_INDEX = 1024        # +90°
        ANGLE_UNITS_PER_TURN = 8192
        SINTAB_MASK_ADR = 0x1FFE       # mask pour byte-addressing
        SINTAB_MASK_IDX = 0x1FFF       # mask pour index-based access
        Q15_SCALE = 32767

    TABLE: short[SIZE]                 # signé 16-bit

    init() :                           # une seule fois au load de classe
        pour i in [0..SIZE-1] :
            angle_rad = 2π * i / SINE_SIZE
            TABLE[i] = round(sin(angle_rad) * Q15_SCALE)

    sinw(angle) :
        retourne TABLE[(angle & SINTAB_MASK_ADR) >> 1]

    cosw(angle) :
        retourne TABLE[((angle + 2048) & SINTAB_MASK_ADR) >> 1]
```

### 2.3 Mapping ASM ↔ Java

| ASM | Java | Équivalence |
|---|---|---|
| `WORD` (16-bit signed) | `short` (storage) / `int` (arithmétique) | Java `short` est 16-bit signé ; on stocke en `short[]`, on retourne `int` pour éviter sign-extension implicite |
| `incbin "bigsine"` | génération `Math.sin` à l'init + `installBigsine(byte[])` pour override binaire | Pas de binaire dans le repo ; la formule canonique est appliquée |
| `(a & 0x1FFE) >> 1` | `(a & 0x1FFE) >>> 1` | Shift logique (`>>>`) pour ne pas propager le signe d'un int négatif issu d'un wrap |
| `adda.w #COSINE_OFS, a0` | `(angle + 2048) & SINTAB_MASK_ADR` | Java fait l'addition puis masque ; sémantique identique |
| `(a0, d0.w)` byte-offset | `byByteOffset(offset)` | Helper pour les cas où l'aval calcule un offset byte (style ASM) |

### 2.4 Risques de divergence

| Risque | Sévérité | Mitigation |
|---|---|---|
| **Régénération `Math.sin` ≠ `bigsine` binaire** | Faible mais existante | `installBigsine(byte[])` permet de remplacer la table générée par le binaire d'origine. Validation byte-à-byte côté test. À activer si le fichier devient disponible. |
| Math.sin(π) ≠ 0 (limite double) | Aucune en pratique | `round(Math.sin(π) * 32767) = 0` donné `|Math.sin(π)| < 1.3e-16` |
| Wrap d'angle Java vs Amiga | Aucune | Java `int + int` overflow silent, masque `& 0x1FFE` absorbe le wrap. Identique à m68k. |
| Sign extension à la lecture | Possible si on lit `short` sans cast explicite | Java retourne automatiquement `int` sign-extended depuis `short` lors d'un access `short[]`. Cohérent avec `move.w` Motorola qui sign-extend si destination est plus large. |

### 2.5 Test de parité fournis

13 tests pour SinCosTable, tous passent :

1. `tableSizeIs8192Entries` — taille
2. `canonicalConstantsMatchAsmSource` — constantes (SINE_SIZE, COSINE_OFS, masks)
3. `northSinCos` / `eastSinCos` / `southSinCos` / `westSinCos` — 4 directions cardinales valeurs exactes
4. `tableContainsTwoFullCycles` — `TABLE[i] == TABLE[i+4096]` pour tout i
5. `cardinalIndicesInTable` — indices 0/1024/2048/3072/4096
6. `periodicityModuloOneTurn` — `sinw(a) == sinw(a+8192)`
7. `bit0IsMaskedOut` — `sinw(2k) == sinw(2k+1)`
8. `pythagoreanIdentityWithinOneLSB` — `sin² + cos² ≈ Q15²` à ±65535 près (bound théorique 2·Q15+1)
9. `sineIsOddFunctionForEvenAngles` — antisymétrie pour angles pairs (impairs cassent à cause du mask)
10. `sinwEqualsTableDirectAccess` — équivalence macro / accès direct
11. `coswEqualsSinwShiftedByQuarterTurn` — `cosw(a) == sinw(a+2048)`
12. `driftVsMathSinIsZero` — la régénération est exactement la formule
13. `installBigsineReplacesTable` / `installBigsineRejectsWrongSize` — mécanisme override binaire
14. `byByteOffsetMatchesIndexedAccess` — équivalence avec accès style ASM

---

## 3. ConstantTable

### 3.1 Analyse ASM détaillée

**`bss/tables_bss.s:26`** — déclaration :
```asm
ConstantTable_vl: ds.l 8192*2 ; 8192 pairs of long
```

**`hires.s:151-170`** — construction au boot :
```asm
; Setup constant table
move.l  #ConstantTable_vl, a0
moveq   #1, d0                  ; n = 1
move.w  #8191, d1               ; loop count (dbra → 8192 iterations)

.fill_const:
        move.l #16384*64, d2    ; d2 = 1 048 576 = 2^20
        divs.l d0, d2           ; d2 = 1 048 576 / n             "c#"
        move.l #64*64*65536, d3 ; d3 = 268 435 456 = 2^28
        divs.l d2, d3           ; d3 = 268 435 456 / c            "e#"
        move.l d3, (a0)+        ; stocke e en (n-1)*8 bytes
        asr.l  #1, d2           ; d2 = c / 2                      "c#/2.0"
        sub.l  #40*64, d2       ; d2 = c/2 − 2560                 "d#"
        muls.l d3, d2           ; d2 = d * e (32-bit signed, silent overflow)
        asr.l  #6, d2           ; d2 >>= 6
        move.l d2, (a0)+        ; stocke d en (n-1)*8 + 4 bytes
        addq   #1, d0           ; n++
        dbra   d1, .fill_const  ; loop n=1..8192
```

### 3.2 Mapping ASM ↔ Java

| Opération m68k | Sémantique | Équivalent Java |
|---|---|---|
| `divs.l Dx, Dy` | 32/32 → 32 signed, truncation vers zéro | `int / int` (JLS §15.17.2) |
| `muls.l Dx, Dy` | 32×32 → 32 signed, low bits, silent overflow | `int * int` |
| `asr.l #N, Dy` | Arithmetic shift right (sign propag.) | `int >> N` |
| `(a0)+` | Auto-increment | Counter `writePos++` |
| Boucle `dbra` | Décrémente puis branche tant que ≥ 0 | `for (n=1; n<=8192; n++)` |

### 3.3 Validation numérique

Valeurs calculées à la main (cf. `ConstantTableTest`) :

| n | c = 2²⁰/n | e = 2²⁸/c | d = ((c/2 − 2560)·e) >> 6 | Note |
|---|---|---|---|---|
| 1 | 1048576 | 256 | 2086912 | borne basse |
| 2 | 524288 | 512 | 2076672 | |
| 256 | 4096 | 65536 | −524288 | passage par zéro de (c/2 − 2560) |
| 1024 | 1024 | 262144 | −8388608 | |
| 8192 | 128 | 2097152 | **−14680064** | overflow 32-bit dans `(c/2−2560)·e` |

**Détail de l'overflow pour n=8192** :
- `c/2 − 2560 = −2496`
- `−2496 × 2097152` (réel) = `−5 234 491 392`
- Tronqué à 32-bit signé : `−5234491392 + 2³² = −939524096`
- `>> 6` = `−14680064` (exact, sans reste)

Java `int * int` produit exactement le même résultat (cf. JLS §15.17.1 : « binary numeric promotion → int, then truncated to 32 bits »). Vérifié par le test `valuesForN_8192`.

### 3.4 Mapping mémoire

L'ASM écrit en ordre auto-incrémenté ; en Java :

```
ConstantTable_vl[(n-1)*2]     = e(n)
ConstantTable_vl[(n-1)*2 + 1] = d(n)
```

**Pas de slot pour n=0** (la boucle ASM commence à n=1). L'accesseur `e(0)` ou `d(0)` lève `IndexOutOfBoundsException`.

### 3.5 Tests fournis (10)

- Taille (`tableSizeIs8192Pairs`, `rangeBoundariesMatchAsm`).
- Valeurs spécifiques (`valuesForN_1`, `valuesForN_2`, `valuesForN_256`, `valuesForN_1024`, `valuesForN_8192`).
- Cohérence avec une réimplémentation indépendante step-by-step (`allEntriesMatchAsmFaithfulReimplementation`).
- Hors-plage (`outOfRangeThrows`).
- Propriétés faibles (`eIsAlwaysPositiveAndNonZero`, `eFor_n1_isMinimumValue`).
- Layout (`layoutIsInterleavedPairs`, `rawLongAccess`).

---

## 4. DivThreeTable

### 4.1 Analyse ASM

**`data/tables_data.s:34-42`** :

```asm
; stores x/3 and x mod 3 for x=0...660
DivThreeTable_vb:
val   SET   0
        REPT  220
        dc.b  val, 0
        dc.b  val, 1
        dc.b  val, 2
val   SET   val + 1
        ENDR
```

Le commentaire dit « x=0...660 » mais la macro itère 220 fois en produisant 3 paires `(val, 0)`, `(val, 1)`, `(val, 2)` chacune — donc couvre x ∈ [0..659] (220 × 3 = 660 entrées). Le commentaire est légèrement inexact (off-by-one).

### 4.2 Layout

Pour x ∈ [0..659] :
- `TABLE[2*x + 0]` = `x / 3` (quotient)
- `TABLE[2*x + 1]` = `x % 3` (reste, valeurs ∈ {0, 1, 2})

Taille : `220 × 6 = 1320` octets.

**Subtilité signed/unsigned** : `val` atteint 219 dans la dernière itération. Casté en `byte` Java, 219 devient `(byte)0xDB = -37`. Les accesseurs publics font `& 0xFF` pour ré-élever en `int` non-signé.

### 4.3 Tests fournis (8)

- Taille (`sizeMatchesAsm`).
- Quotient/reste sur toute la plage [0..659] (`quotientMatchesIntegerDivisionByThree`, `remainderMatchesIntegerModuloByThree`).
- Layout exact des 6 premiers et 6 derniers octets (`firstSixBytesMatchExpectedLayout`, `lastSixBytesMatchExpectedLayout`).
- Hors-plage (`outOfRangeThrows`).
- Identité `x = 3q + r` (`quotientRemainderRelation`).
- Accès brut par byte-offset (`rawByteAccess`).

---

## 5. Hypothèses & inconnues restantes

1. **`bigsine` binaire d'origine** : non disponible dans le repo. La table est régénérée via `Math.sin`. Le risque de drift Q15 vs original est noté ; `installBigsine()` permet l'override si le fichier devient disponible.

2. **Usage exact de `ConstantTable`** : la signification sémantique de `e(n)` et `d(n)` (variables muettes dans l'ASM) sera élucidée lors du portage du rasterizer (sous-système rendu). Pour l'instant, on garantit la **valeur exacte**.

3. **`MAX_ONE_OVER_N = 511`** dans `tables_data.s:6` : référencé mais pas utilisé dans le code ASM visible. À explorer lors du portage du rendu sols.

4. **Convention angle 8192 vs 4096** dans `docs/README.md` : on retient 8192 (vérité du code).

---

## 6. Conformité aux contraintes du projet

| Règle | Statut |
|---|---|
| Pas de `float`/`double` en hot path | ✅ — seul `regenerateFromFormula()` utilise `Math.sin`/`Math.round` ; n'est appelé qu'à l'init |
| Pas de `Math.sin`/`cos` en runtime | ✅ — runtime utilise exclusivement `sinw`/`cosw` (lookup table) |
| Préservation exact des shifts | ✅ — `>>` arithmétique pour les ints signés, `>>>` logique uniquement après mask |
| Préservation des overflows | ✅ — vérifié explicitement pour n=8192 (overflow `muls.l`) |
| Pas d'ECS / framework moderne | ✅ — classes statiques pures |
| Tests de parité | ✅ — 39/39 |
| Déterminisme | ✅ — toutes les tables sont calculées une fois au class-load, immutables ensuite |

---

## 7. Prochain sous-système

Proposition : **Sous-système 02 — Loader binaires** : lecture des structures `PlrT`, `ObjT`, `ZoneT`, `EdgeT`, `GLFT`, headers `twolev.bin`/`graph.bin`. Permettra de valider que le layout mémoire des structures Java est strictement compatible avec les fichiers data Team17, et de poser les bases du chargement de niveau.

Alternative : **Sous-système 02 — Transformation 3D (`modules/transform.s`)** : peut être porté sans dépendre du loader, en alimentant des points fictifs. Permet de tester la projection + sin/cos en pipeline avant de toucher au format des assets.

À décider à la prochaine étape.
