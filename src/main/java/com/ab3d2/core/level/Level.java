package com.ab3d2.core.level;

/**
 * Métadonnées haut-niveau d'un niveau (nom, index, configuration).
 * Distinct de {@link LevelData} qui contient la géométrie parsée.
 *
 * @see LevelData pour les données binaires complètes
 */
public class Level {

    private final String id;      // "A"..."P"
    private final int    index;   // 0...15
    private LevelData    data;    // null tant que non chargé

    public Level(String id, int index) {
        this.id    = id;
        this.index = index;
    }

    public String    getId()    { return id; }
    public int       getIndex() { return index; }
    public LevelData getData()  { return data; }

    public void setData(LevelData data) { this.data = data; }

    public boolean isLoaded() { return data != null; }

    @Override
    public String toString() {
        return "Level{id=" + id + ", loaded=" + isLoaded() + "}";
    }
}
