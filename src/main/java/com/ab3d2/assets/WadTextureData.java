package com.ab3d2.assets;

/**
 * Données d'une texture de mur décodée depuis un fichier .256wad.
 *
 * <p>Les pixels sont stockés en ARGB 32 bits, indexés ligne par ligne
 * (y * width + x), prêts pour un upload OpenGL direct via
 * {@link AssetManager#createTextureFromARGB(int[], int, int)}.
 *
 * <p>L'éclairage (shade) est appliqué au rendu par multiplication flottante,
 * pas ici — les pixels contiennent toujours la couleur full-brightness.
 */
public record WadTextureData(
        /** Nom du fichier source, sans extension (ex. "stonewall"). */
        String  name,
        /** Largeur en pixels (puissance de 2, typiquement 32/64/128). */
        int     width,
        /** Hauteur en pixels (puissance de 2). */
        int     height,
        /**
         * Pixels ARGB 32 bits, taille = width * height.
         * Index : {@code y * width + x}.
         * Alpha = 0xFF pour tous les pixels (murs opaques).
         */
        int[]   pixels,
        /**
         * Table de shade complète : 32 lignes × 32 entrées = 1024 ints ARGB.
         * {@code shadeTable[row * 32 + texel5bit]} = couleur ARGB pour ce
         * texel à ce niveau de luminosité.
         * Row 0 = plus sombre, row 31 = full brightness.
         * Utile si le renderer veut faire le lookup sans multiplication float.
         */
        int[]   shadeTable
) {
    // Constantes partagées avec WallTextureExtractor
    public static final int SHADE_ROWS      = 32;
    public static final int ENTRIES_PER_ROW = 32;
    public static final int SHADE_TABLE_ENTRIES = SHADE_ROWS * ENTRIES_PER_ROW; // 1024

    /** Retourne la couleur ARGB pour un texel 5 bits à un niveau de luminosité. */
    public int shadeColor(int shadeRow, int texel5bit) {
        return shadeTable[(shadeRow & 31) * ENTRIES_PER_ROW + (texel5bit & 31)];
    }

    /** Masque de wrap horizontal : {@code u & widthMask}. */
    public int widthMask()  { return width  - 1; }
    /** Masque de wrap vertical : {@code v & heightMask}. */
    public int heightMask() { return height - 1; }
}
