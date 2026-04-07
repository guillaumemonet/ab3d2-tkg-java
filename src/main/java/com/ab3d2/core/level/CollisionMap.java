package com.ab3d2.core.level;

import java.nio.file.*;
import java.io.IOException;

/**
 * Grille de collision 100×100 du niveau.
 * Correspond à {@code twolev.map} — 1 byte par cellule, big-endian.
 *
 * Valeurs :
 *   0   = vide (pas de géométrie)
 *   1-N = identifiant de zone (secteur)
 *   255 = mur solide
 */
public class CollisionMap {

    public static final int GRID_SIZE  = 100;
    public static final int WALL_VALUE = 255;
    public static final int EMPTY      = 0;

    private final byte[] grid; // raw bytes [row*100 + col]
    private final int    width;
    private final int    height;

    public CollisionMap(byte[] raw) {
        this.grid   = raw;
        this.width  = GRID_SIZE;
        this.height = GRID_SIZE;
    }

    /** Valeur brute en (col, row). */
    public int get(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return WALL_VALUE;
        return grid[row * width + col] & 0xFF;
    }

    /** Vrai si la cellule est un mur solide. */
    public boolean isWall(int col, int row) {
        return get(col, row) == WALL_VALUE;
    }

    /** Vrai si la cellule est praticable (non-mur, non-vide). */
    public boolean isWalkable(int col, int row) {
        int v = get(col, row);
        return v > EMPTY && v < WALL_VALUE;
    }

    /** Identifiant de zone pour la cellule, ou -1 si mur/vide. */
    public int zoneIdAt(int col, int row) {
        int v = get(col, row);
        return (v > EMPTY && v < WALL_VALUE) ? v - 1 : -1; // 1-indexed → 0-indexed
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
    public byte[] getRaw() { return grid; }

    /** Charge depuis un fichier twolev.map. */
    public static CollisionMap load(Path path) throws IOException {
        byte[] raw = Files.readAllBytes(path);
        if (raw.length < GRID_SIZE * GRID_SIZE) {
            throw new IOException("twolev.map trop petit: " + raw.length + " bytes");
        }
        return new CollisionMap(raw);
    }
}
