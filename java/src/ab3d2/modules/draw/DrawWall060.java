package ab3d2.modules.draw;

import ab3d2.Mem;

import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;

/**
 * Traduction littérale de ab3d2_source/modules/draw/draw_wall_060.s
 *
 * "68060 tuned inner wall rendering loop by @paraj"
 *
 * Sélectionnée par IFD OPT060 (le port générique utilise DrawWall). Même
 * sémantique pixel que DrawWall mais ordonnancement différent : le masque
 * and.w d7,d4 est appliqué APRÈS le pas (et une fois à l'entrée), le pas
 * fractionnaire/entier précède l'écriture. L'alternance claire/sombre
 * (a2/a4) est générée par la macro drawwallS060_loop (val ^= 1).
 *
 * PACK0 : d1 = 31 & octet impair ; PACK1 : (mot >> 5) & 31 ;
 * PACK2 : d1 = (0x7C & octet pair) >> 2 — ici le moveq écrase tout d1.
 */
public final class DrawWall060 {

    private DrawWall060() {
    }

    /** drawwallPACK0: drawwallS060 0 */
    public static void drawwallPACK0(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                     int a2, int a3, int a4, int a5) {
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4 ; make sure offset is masked
        boolean dim = false;                           // \2 = 0 → première itération sur a2
        while (true) { // .loop<val>:
            // grab packed texel (IFEQ \1-0)
            d1 = 0b00011111;                           // moveq #%00011111,d1
            d1 = setb(d1, d1 & Mem.ub(a5 + ((short) d4) * 2 + 1)); // and.b 1(a5,d4.w*2),d1

            // v step (fractional first, then integer part)
            long sum = (d4 & 0xFFFFFFFFL) + (d3 & 0xFFFFFFFFL); // add.l d3,d4
            boolean x = sum > 0xFFFFFFFFL;
            d4 = (int) sum;
            d4 = setw(d4, d4 + (short) d2 + (x ? 1 : 0)); // addx.w d2,d4
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4

            // store through palette lookup (note: alternates between a2 and a4)
            Mem.wb(a3, Mem.ub((dim ? a4 : a2) + ((short) d1) * 2)); // move.b (a2|a4,d1.w*2),(a3)

            a3 += (short) d0;                          // adda.w d0,a3 ; dest += width
            d6 = setw(d6, d6 - 1);                     // dbf d6,.loop<val^1>
            if ((short) d6 == -1) {
                return;                                // rts
            }
            dim = !dim;
        }
    }

    /** drawwallPACK1: drawwallS060 1 */
    public static void drawwallPACK1(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                     int a2, int a3, int a4, int a5) {
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        boolean dim = false;
        while (true) { // .loop<val>:
            // grab packed texel (IFEQ \1-1)
            d1 = setw(d1, Mem.uw(a5 + ((short) d4) * 2)); // move.w (a5,d4.w*2),d1
            d1 = setw(d1, (d1 & 0xFFFF) >>> 5);        // lsr.w #5,d1
            d1 = setw(d1, d1 & 0b00011111);            // and.w #%00011111,d1

            long sum = (d4 & 0xFFFFFFFFL) + (d3 & 0xFFFFFFFFL); // add.l d3,d4
            boolean x = sum > 0xFFFFFFFFL;
            d4 = (int) sum;
            d4 = setw(d4, d4 + (short) d2 + (x ? 1 : 0)); // addx.w d2,d4
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4

            Mem.wb(a3, Mem.ub((dim ? a4 : a2) + ((short) d1) * 2)); // move.b (a2|a4,d1.w*2),(a3)

            a3 += (short) d0;                          // adda.w d0,a3
            d6 = setw(d6, d6 - 1);                     // dbf d6,.loop<val^1>
            if ((short) d6 == -1) {
                return;                                // rts
            }
            dim = !dim;
        }
    }

    /** drawwallPACK2: drawwallS060 2 */
    public static void drawwallPACK2(int d0, int d1, int d2, int d3, int d4, int d6, int d7,
                                     int a2, int a3, int a4, int a5) {
        d4 = setw(d4, d4 & d7);                        // and.w d7,d4
        boolean dim = false;
        while (true) { // .loop<val>:
            // grab packed texel (IFEQ \1-2)
            d1 = 0b01111100;                           // moveq #%01111100,d1
            d1 = setb(d1, d1 & Mem.ub(a5 + ((short) d4) * 2)); // and.b (a5,d4.w*2),d1
            d1 = setb(d1, (d1 & 0xFF) >>> 2);          // lsr.b #2,d1

            long sum = (d4 & 0xFFFFFFFFL) + (d3 & 0xFFFFFFFFL); // add.l d3,d4
            boolean x = sum > 0xFFFFFFFFL;
            d4 = (int) sum;
            d4 = setw(d4, d4 + (short) d2 + (x ? 1 : 0)); // addx.w d2,d4
            d4 = setw(d4, d4 & d7);                    // and.w d7,d4

            Mem.wb(a3, Mem.ub((dim ? a4 : a2) + ((short) d1) * 2)); // move.b (a2|a4,d1.w*2),(a3)

            a3 += (short) d0;                          // adda.w d0,a3
            d6 = setw(d6, d6 - 1);                     // dbf d6,.loop<val^1>
            if ((short) d6 == -1) {
                return;                                // rts
            }
            dim = !dim;
        }
    }
}
