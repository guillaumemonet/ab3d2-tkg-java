package ab3d2.modules.draw;

import ab3d2.Mem;

import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;

/**
 * Traduction littérale de ab3d2_source/modules/draw/draw_wall.s
 *
 * "68020+ generic inner wall rendering loop"
 *
 * Strip-drawers verticaux des murs, appelés par hireswall.s. Chaque pixel
 * alterne entre la palette claire (a2) et la palette sombre (a4) — les deux
 * boucles bright/dim se renvoient l'une à l'autre via dbra (dithering).
 * Trois formats de texel packés :
 *   PACK0 : texel = octet impair & 31 ; PACK1 : (mot >> 5) & 31 ;
 *   PACK2 : octet pair >> 2.
 *
 * Registres d'entrée (fournis par hireswall.s) :
 *   d0 = stride écran (word, ajouté à a3), d1 = scratch texel (les bits
 *   8-15 hérités de l'appelant participent à l'index (aX,d1.w*2) — préservés),
 *   d2 = pas Y entier (word, additionné via addx avec la retenue du pas
 *   fractionnaire), d3 = pas Y fractionnaire (long), d4 = position Y texture,
 *   d6 = nombre de pixels (dbra), d7 = masque hauteur texture,
 *   a2 = palette claire, a3 = écran, a4 = palette sombre, a5 = colonne texture.
 *
 * La retenue X de add.l d3,d4 est consommée par addx.w d2,d4 : reproduite
 * explicitement (somme non signée 32 bits).
 *
 * nostrip: rts — point d'entrée vide utilisé quand il n'y a rien à dessiner.
 */
public final class DrawWall {

    private DrawWall() {
    }

    /** nostrip: rts */
    public static void nostrip() {
    }

    /** drawwallPACK0 — premier pixel sur la palette claire (a2). */
    public static void drawwallPACK0(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                     int a2, int a3, int a4, int a5) {
        pack0(false, d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    /** drawwalldimPACK0 — premier pixel sur la palette sombre (a4). */
    public static void drawwalldimPACK0(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                        int a2, int a3, int a4, int a5) {
        pack0(true, d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    // drawwallPACK0 <-> drawwalldimPACK0 : même corps, palette alternée à chaque pixel.
    private static void pack0(boolean dim, int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                              int a2, int a3, int a4, int a5) {
        while (true) {
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4
            d1 = setb(d1, Mem.ub(a5 + ((short) d4) * 2 + 1)); // move.b 1(a5,d4.w*2),d1 ; fetch texel
            d1 = setb(d1, d1 & 31);                    // and.b #31,d1 ; pull out right part
            long sum = (d4 & 0xFFFFFFFFL) + (d3 & 0xFFFFFFFFL); // add.l d3,d4 ; add fractional part (X = retenue)
            boolean x = sum > 0xFFFFFFFFL;
            d4 = (int) sum;
            Mem.wb(a3, Mem.ub((dim ? a4 : a2) + ((short) d1) * 2)); // move.b (a4|a2,d1.w*2),(a3)
            a3 += (short) d0;                          // adda.w d0,a3 ; next line in screen
            d4 = setw(d4, d4 + (short) d2 + (x ? 1 : 0)); // addx.w d2,d4 ; texture Y + dy
            d6 = setw(d6, d6 - 1);                     // dbra d6,drawwall(dim)PACK0
            if ((short) d6 == -1) {
                return;                                // rts
            }
            dim = !dim;
        }
    }

    /** drawwallPACK1 — premier pixel sur la palette claire (a2). */
    public static void drawwallPACK1(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                     int a2, int a3, int a4, int a5) {
        pack1(false, d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    /** drawwalldimPACK1 — premier pixel sur la palette sombre (a4). */
    public static void drawwalldimPACK1(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                        int a2, int a3, int a4, int a5) {
        pack1(true, d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    private static void pack1(boolean dim, int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                              int a2, int a3, int a4, int a5) {
        while (true) {
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4
            d1 = setw(d1, Mem.uw(a5 + ((short) d4) * 2)); // move.w (a5,d4.w*2),d1
            d1 = setw(d1, (d1 & 0xFFFF) >>> 5);        // lsr.w #5,d1
            d1 = setw(d1, d1 & 31);                    // and.w #31,d1
            long sum = (d4 & 0xFFFFFFFFL) + (d3 & 0xFFFFFFFFL); // add.l d3,d4
            boolean x = sum > 0xFFFFFFFFL;
            d4 = (int) sum;
            Mem.wb(a3, Mem.ub((dim ? a4 : a2) + ((short) d1) * 2)); // move.b (a4|a2,d1.w*2),(a3)
            a3 += (short) d0;                          // adda.w d0,a3
            d4 = setw(d4, d4 + (short) d2 + (x ? 1 : 0)); // addx.w d2,d4
            d6 = setw(d6, d6 - 1);                     // dbra d6,drawwall(dim)PACK1
            if ((short) d6 == -1) {
                return;                                // rts
            }
            dim = !dim;
        }
    }

    /** drawwallPACK2 — premier pixel sur la palette claire (a2). */
    public static void drawwallPACK2(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                     int a2, int a3, int a4, int a5) {
        pack2(false, d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    /** drawwalldimPACK2 — premier pixel sur la palette sombre (a4). */
    public static void drawwalldimPACK2(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                        int a2, int a3, int a4, int a5) {
        pack2(true, d0, d1, d2, d3, d4, d6, d7, a2, a3, a4, a5);
    }

    private static void pack2(boolean dim, int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                              int a2, int a3, int a4, int a5) {
        while (true) {
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4
            d1 = setb(d1, Mem.ub(a5 + ((short) d4) * 2)); // move.b (a5,d4.w*2),d1
            d1 = setb(d1, (d1 & 0xFF) >>> 2);          // lsr.b #2,d1
            long sum = (d4 & 0xFFFFFFFFL) + (d3 & 0xFFFFFFFFL); // add.l d3,d4
            boolean x = sum > 0xFFFFFFFFL;
            d4 = (int) sum;
            Mem.wb(a3, Mem.ub((dim ? a4 : a2) + ((short) d1) * 2)); // move.b (a4|a2,d1.w*2),(a3)
            a3 += (short) d0;                          // adda.w d0,a3
            d4 = setw(d4, d4 + (short) d2 + (x ? 1 : 0)); // addx.w d2,d4
            d6 = setw(d6, d6 - 1);                     // dbra d6,drawwall(dim)PACK2
            if ((short) d6 == -1) {
                return;                                // rts
            }
            dim = !dim;
        }
    }
}
