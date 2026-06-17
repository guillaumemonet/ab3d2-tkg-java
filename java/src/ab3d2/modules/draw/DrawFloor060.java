package ab3d2.modules.draw;

import ab3d2.HiresData;
import ab3d2.Mem;

import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;

/**
 * Traduction littérale de ab3d2_source/modules/draw/draw_floor_060.s
 *
 * "68060 optimised Gouraud Floor routine by @paraj / @saimo"
 *
 * Sélectionnée par IFD OPT060 dans hires.s (le port générique 68030+ utilise
 * DrawFloor ; cette variante est fournie pour exhaustivité — résultat pixel
 * identique, ordonnancement différent).
 * Mêmes registres d'entrée que DrawFloor.draw_GoraudFloor. Renvoie a3 avancé.
 */
public final class DrawFloor060 {

    private DrawFloor060() {
    }

    /** draw_GoraudFloor (variante 060) */
    public static int draw_GoraudFloor(int d1, int d2, int d5, int d7, int a0, int a1, int a3) {
        int d0 = 0;                                    // moveq #0,d0
        d0 = setw(d0, Mem.uw(HiresData.leftbright));   // move.w leftbright,d0
        int d4 = Mem.uw(HiresData.brightspd);          // move.w brightspd,d4
        int a2 = d2;                                   // move.l d2,a2

        d5 &= d1;                                      // and.l d1,d5
        int d6 = d5;                                   // move.l d5,d6
        d6 >>>= 8;                                     // lsr.l #8,d6
        d2 = d6;                                       // move.l d6,d2
        d6 >>>= 8;                                     // lsr.l #8,d6
        d6 = setb(d6, d2);                             // move.b d2,d6 ; d6 ready for first iteration

        int d3;
        do { // .loop:
            d3 = d0;                                   // move.l d0,d3 ; d3=00Cc
            d3 = setb(d3, Mem.ub(a0 + d6 * 4));        // move.b (a0,d6.l*4),d3 ; d3=00CT
            d0 = setw(d0, d0 + d4);                    // add.w d4,d0 ; c += dcdx
            d5 += a2;                                  // add.l a2,d5 ; uv += duvdx
            d5 &= d1;                                  // and.l d1,d5 ; uv &= uvmask
            d6 = d5;                                   // move.l d5,d6 ; d6=VvUu
            d6 >>>= 8;                                 // lsr.l #8,d6 ; d6=0VvU
            d2 = d6;                                   // move.l d6,d2 ; d2=0VvU
            d6 >>>= 8;                                 // lsr.l #8,d6 ; d6=00Vv
            d6 = setb(d6, d2);                         // move.b d2,d6 ; d6=00VU
            Mem.wb(a3, Mem.ub(a1 + d3)); a3 += 1;      // move.b (a1,d3.l),(a3)+
            d7 = setw(d7, d7 - 1);                     // subq.w #1,d7
        } while ((short) d7 != 0);                     // bne.b .loop
        return a3;                                     // rts
    }
}
