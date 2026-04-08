package com.ab3d2.core.level;

/**
 * Donnees de rendu d'un mur ou element de zone, extraites depuis le
 * ZoneGraphAdds de twolev.graph.bin.
 *
 * <h2>Format binaire (30 bytes par entree mur)</h2>
 * <pre>
 * +0   BYTE  wallId    (high byte du type WORD)
 * +1   BYTE  typeByte  (low byte : 0=mur, >=128=fin)
 * +2   WORD  left_pt
 * +4   WORD  right_pt
 * +6   BYTE  which_left
 * +7   BYTE  which_right
 * +8   WORD  flags
 * +10  WORD  from_tile
 * +12  WORD  y_offset
 * +14  WORD  tex_index
 * +16  BYTE  h_mask
 * +17  BYTE  h_shift
 * +18  BYTE  w_mask
 * +19  BYTE  which_pbr
 * +20  LONG  top_wall
 * +24  LONG  bot_wall
 * +28  BYTE  bright_ofs
 * +29  BYTE  other_zone
 * </pre>
 */
public final class WallRenderEntry {

    public static final int BYTE_SIZE = 30;

    // Types (low byte du type WORD dans le flux)
    public static final int TYPE_WALL      = 0;
    public static final int TYPE_FLOOR     = 1;
    public static final int TYPE_CEIL      = 2;
    public static final int TYPE_OBJECT    = 4;
    public static final int TYPE_WATER     = 7;
    public static final int TYPE_BACKDROP  = 12;
    public static final int TYPE_END       = 0x80;

    // Champs mur
    public final int type;
    public final int wallId;
    public final int leftPt;
    public final int rightPt;
    public final int whichLeft;
    public final int whichRight;
    public final int flags;
    public final int fromTile;
    public final int yOffset;
    public final int texIndex;
    public final int hMask;
    public final int hShift;
    public final int wMask;
    public final int whichPbr;
    public final int topWall;
    public final int botWall;
    public final int brightOfs;
    public final int otherZone;

    // Champ special pour enregistrements floor (type=1, extrait du flux zone graph)
    // -1 = ce n'est pas un floor record
    public final int floorWhichTile;

    // ── Constructeur mur normal ───────────────────────────────────────────────

    public WallRenderEntry(int type, int wallId,
                           int leftPt, int rightPt,
                           int whichLeft, int whichRight,
                           int flags, int fromTile, int yOffset,
                           int texIndex,
                           int hMask, int hShift, int wMask, int whichPbr,
                           int topWall, int botWall,
                           int brightOfs, int otherZone) {
        this.type            = type;
        this.wallId          = wallId;
        this.leftPt          = leftPt;
        this.rightPt         = rightPt;
        this.whichLeft       = whichLeft;
        this.whichRight      = whichRight;
        this.flags           = flags;
        this.fromTile        = fromTile;
        this.yOffset         = yOffset;
        this.texIndex        = texIndex;
        this.hMask           = hMask;
        this.hShift          = hShift;
        this.wMask           = wMask;
        this.whichPbr        = whichPbr;
        this.topWall         = topWall;
        this.botWall         = botWall;
        this.brightOfs       = brightOfs;
        this.otherZone       = otherZone;
        this.floorWhichTile  = -1;
    }

    /** Cree un enregistrement floor ou ceil special avec le whichtile extrait du flux. */
    public static WallRenderEntry makeFloor(int whichTile) {
        return new WallRenderEntry(TYPE_FLOOR, whichTile);
    }
    public static WallRenderEntry makeCeil(int whichTile) {
        return new WallRenderEntry(TYPE_CEIL, whichTile);
    }

    /** Constructeur prive pour makeFloor/makeCeil. */
    private WallRenderEntry(int typeFC, int whichTile) {
        this.type = typeFC; this.wallId = 0; this.leftPt = 0; this.rightPt = 0;
        this.whichLeft = 0; this.whichRight = 0; this.flags = 0;
        this.fromTile = 0; this.yOffset = 0; this.texIndex = 0;
        this.hMask = 0; this.hShift = 0; this.wMask = 0; this.whichPbr = 0;
        this.topWall = 0; this.botWall = 0; this.brightOfs = 0; this.otherZone = 0;
        this.floorWhichTile = whichTile;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public boolean isWall()        { return type == TYPE_WALL; }
    public boolean isFloorRecord() { return type == TYPE_FLOOR && floorWhichTile >= 0; }
    public boolean isCeilRecord()  { return type == TYPE_CEIL  && floorWhichTile >= 0; }
    public boolean isEnd()         { return (type & 0xFF) >= TYPE_END; }

    public int texWidth()   { return (wMask & 0xFF) + 1; }
    public int texHeight()  { return (hMask & 0xFF) + 1; }
    public int texOffsetX() { return fromTile & 0xFFFF; }
    public int texOffsetY() { return yOffset & 0xFFFF; }

    @Override
    public String toString() {
        if (isFloorRecord()) return String.format("WallRenderEntry[FLOOR whichTile=%d]", floorWhichTile);
        if (!isWall()) return "WallRenderEntry[type=" + type + "]";
        return String.format(
            "WallRenderEntry[WALL pts=%d-%d tex=%d top=%d bot=%d]",
            leftPt, rightPt, texIndex, topWall >> 8, botWall >> 8);
    }
}
