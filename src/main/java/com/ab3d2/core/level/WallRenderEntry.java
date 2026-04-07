package com.ab3d2.core.level;

/**
 * Donnees de rendu d'un mur ou element de zone, extraites depuis le
 * ZoneGraphAdds de twolev.graph.bin.
 *
 * <h2>Format binaire (30 bytes par entree, depuis hireswall.s Draw_Wall)</h2>
 * <pre>
 * +0   BYTE  type       0=mur, 1=sol, 2=plafond, 4=objet, >=128=fin
 * +1   BYTE  wall_id    id pour la minimap (high byte du type WORD)
 * +2   WORD  left_pt    index point gauche
 * +4   WORD  right_pt   index point droit
 * +6   BYTE  which_left   index table brightness gauche
 * +7   BYTE  which_right  index table brightness droit
 * +8   WORD  flags
 * +10  WORD  from_tile   decalage X texture (a diviser par 16)
 * +12  WORD  y_offset    decalage Y texture
 * +14  WORD  tex_index   index 0-15 dans Draw_WallTexturePtrs_vl
 * +16  BYTE  h_mask      masque hauteur texture (tex_height - 1)
 * +17  BYTE  h_shift     shift hauteur texture
 * +18  BYTE  w_mask      masque largeur texture (tex_width - 1)
 * +19  BYTE  which_pbr   brightness table selector
 * +20  LONG  top_wall    hauteur haut du mur (raw, avant soustraction PlayerY)
 * +24  LONG  bot_wall    hauteur bas du mur
 * +28  BYTE  bright_ofs  offset de luminosite
 * +29  BYTE  other_zone  zone de l'autre cote
 * </pre>
 */
public final class WallRenderEntry {

    public static final int BYTE_SIZE = 30;

    // Types
    public static final int TYPE_WALL      = 0;
    public static final int TYPE_FLOOR     = 1;
    public static final int TYPE_CEIL      = 2;
    public static final int TYPE_SET_CLIP  = 3;
    public static final int TYPE_OBJECT    = 4;
    public static final int TYPE_WATER     = 7;
    public static final int TYPE_BACKDROP  = 12;
    public static final int TYPE_END       = 0x80; // >= 128 = fin de liste

    // ── Champs ────────────────────────────────────────────────────────────────

    public final int type;        // 0=mur 1=sol 2=plafond >=128=fin
    public final int wallId;      // id minimap
    public final int leftPt;      // index point gauche
    public final int rightPt;     // index point droit
    public final int whichLeft;   // bright table left
    public final int whichRight;  // bright table right
    public final int flags;
    public final int fromTile;    // decalage X texture (raw/16 = vraie valeur)
    public final int yOffset;     // decalage Y texture
    public final int texIndex;    // 0-15 → fichier .256wad
    public final int hMask;       // tex_height - 1 (masque wrap vertical)
    public final int hShift;      // shift hauteur
    public final int wMask;       // tex_width - 1 (masque wrap horizontal)
    public final int whichPbr;
    public final int topWall;     // hauteur top (LONG signe, raw)
    public final int botWall;     // hauteur bas (LONG signe, raw)
    public final int brightOfs;   // offset luminosite
    public final int otherZone;   // zone adjacente

    // ── Constructeur ──────────────────────────────────────────────────────────

    public WallRenderEntry(int type, int wallId,
                           int leftPt, int rightPt,
                           int whichLeft, int whichRight,
                           int flags, int fromTile, int yOffset,
                           int texIndex,
                           int hMask, int hShift, int wMask, int whichPbr,
                           int topWall, int botWall,
                           int brightOfs, int otherZone) {
        this.type       = type;
        this.wallId     = wallId;
        this.leftPt     = leftPt;
        this.rightPt    = rightPt;
        this.whichLeft  = whichLeft;
        this.whichRight = whichRight;
        this.flags      = flags;
        this.fromTile   = fromTile;
        this.yOffset    = yOffset;
        this.texIndex   = texIndex;
        this.hMask      = hMask;
        this.hShift     = hShift;
        this.wMask      = wMask;
        this.whichPbr   = whichPbr;
        this.topWall    = topWall;
        this.botWall    = botWall;
        this.brightOfs  = brightOfs;
        this.otherZone  = otherZone;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public boolean isWall()     { return type == TYPE_WALL; }
    public boolean isFloor()    { return type == TYPE_FLOOR; }
    public boolean isCeil()     { return type == TYPE_CEIL; }
    public boolean isObject()   { return type == TYPE_OBJECT; }
    public boolean isEnd()      { return (type & 0xFF) >= TYPE_END; }

    /** Hauteur haute en height-units (>>8). */
    public int topHeightUnits()  { return topWall >> 8; }
    /** Hauteur basse en height-units. */
    public int botHeightUnits()  { return botWall >> 8; }

    /** Largeur texture en pixels (wMask + 1). */
    public int texWidth()   { return (wMask & 0xFF) + 1; }
    /** Hauteur texture en pixels (hMask + 1). */
    public int texHeight()  { return (hMask & 0xFF) + 1; }

    /** Decalage texture X reel (fromTile >> 4 dans l'assembleur original). */
    public int texOffsetX() { return fromTile & 0xFFFF; }
    /** Decalage texture Y. */
    public int texOffsetY() { return yOffset & 0xFFFF; }

    @Override
    public String toString() {
        if (isEnd()) return "WallRenderEntry[END]";
        String typeName = switch (type) {
            case TYPE_WALL     -> "WALL";
            case TYPE_FLOOR    -> "FLOOR";
            case TYPE_CEIL     -> "CEIL";
            case TYPE_OBJECT   -> "OBJ";
            case TYPE_WATER    -> "WATER";
            case TYPE_BACKDROP -> "SKY";
            default            -> "type=" + type;
        };
        if (!isWall()) return "WallRenderEntry[" + typeName + "]";
        return String.format(
            "WallRenderEntry[WALL pts=%d-%d tex=%d w=%d h=%d top=%d bot=%d bright=%d]",
            leftPt, rightPt, texIndex, texWidth(), texHeight(),
            topHeightUnits(), botHeightUnits(), brightOfs);
    }
}
