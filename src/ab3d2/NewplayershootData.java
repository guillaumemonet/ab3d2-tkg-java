package ab3d2;

/**
 * Données embarquées de ab3d2_source/newplayershoot.s. Voir Newplayershoot.
 */
public final class NewplayershootData {

    // ---- newplayershoot.s:3-8 ----
    private static final int _a0 = Mem.align(4);
    public static final int targetydiff = Mem.dcL(0);
    public static final int targdist = Mem.dcW(0);
    public static final int tempangpos = Mem.dcW(0);
    public static final int MaxFrame = Mem.dcW(0);
    public static final int BULTYPE = Mem.dcW(0);      // accédé aussi en BULTYPE+1 (octet faible)
    public static final int AmmoInMyGun = Mem.dcW(0);

    // ---- newplayershoot.s:686-692 ----
    private static final int _a1 = Mem.align(4);
    public static final int tempyoff = Mem.dcL(0);
    public static final int BulletSpd = Mem.dcW(0);
    public static final int tempStoodInTop = Mem.dcW(0); // écrit en byte
    public static final int tempxdir = Mem.dcW(0);
    public static final int tempzdir = Mem.dcW(0);
    public static final int tempgun = Mem.dcW(0);        // écrit en byte
    public static final int tstfire = Mem.dcW(0);

    private NewplayershootData() {
    }
}
