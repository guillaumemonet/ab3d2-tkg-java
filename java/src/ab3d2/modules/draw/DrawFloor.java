package ab3d2.modules.draw;

import ab3d2.HiresData;
import ab3d2.Mem;

import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;

/**
 * Traduction littérale de ab3d2_source/modules/draw/draw_floor.s
 *
 * "Generic Gouraud Floor Routine"
 *
 * Boucle texel interne des sols/plafonds, appelée par hires.s::Draw_Flats
 * une fois par scanline. Registres d'entrée (fournis par Draw_Flats) :
 *   d1 = masque UV (long), d2 = incrément UV par pixel (long),
 *   d5 = UV courant (T dans le mot fort, S 8.8 dans le mot faible),
 *   d7 = nombre de pixels (word),
 *   a0 = texture (texel = a0[(T<<8|S>>8)*4]), a1 = table de shading
 *   (index = bright<<8 | texel), a3 = renderbuffer.
 * leftbright/brightspd (hires.s) : brightness gouraud 8.8 et son incrément.
 * Renvoie a3 avancé.
 */
public final class DrawFloor {

    private DrawFloor() {
    }

    /** draw_GoraudFloor */
    public static int draw_GoraudFloor(int d1, int d2, int d5, int d7, int a0, int a1, int a3) {
        int d0 = Mem.uw(HiresData.leftbright);         // move.w leftbright,d0
        int d4 = d1;                                   // move.l d1,d4
        d1 = setw(d1, Mem.uw(HiresData.brightspd));    // move.w brightspd,d1

        int d3 = setw(0, d7);                          // move.w d7,d3
        d7 = setw(d7, ((short) d7) >> 1);              // asr.w #1,d7
        int d6 = 0;
        if ((d3 & 1) != 0) {                           // btst #0,d3 ; beq.s .nosingle1
            d3 = setw(d3, d5);                         // move.w d5,d3 ; d3 = S
            d6 = d5;                                   // move.l d5,d6
            d3 = setw(d3, (d3 & 0xFFFF) >>> 8);        // lsr.w #8,d3
            d6 = swap(d6);                             // swap d6 ; d6 = T
            d6 = setb(d6, d3);                         // move.b d3,d6 ; T * 256 + S

            d3 = setw(d3, d0);                         // move.w d0,d3 ; line X

            d3 = setb(d3, Mem.ub(a0 + ((short) d6) * 4)); // move.b (a0,d6.w*4),d3 ; fetch floor texel; but why d6*4?

            d0 = setw(d0, d0 + d1);                    // add.w d1,d0
            d5 += d2;                                  // add.l d2,d5
            d5 &= d4;                                  // and.l d4,d5
            Mem.wb(a3, Mem.ub(a1 + (short) d3)); a3 += 1; // move.b (a1,d3.w),(a3)+ ; map through palette and write
        }

        // .nosingle1:
        d3 = setw(d3, d7);                             // move.w d7,d3
        d7 = setw(d7, ((short) d7) >> 1);              // asr.w #1,d7
        if ((d3 & 1) != 0) {                           // btst #0,d3 ; beq.s .nosingle2
            d3 = setw(d3, d5);                         // move.w d5,d3
            d6 = d5;                                   // move.l d5,d6
            d3 = setw(d3, (d3 & 0xFFFF) >>> 8);        // lsr.w #8,d3
            d6 = swap(d6);                             // swap d6
            d6 = setb(d6, d3);                         // move.b d3,d6
            d3 = setw(d3, d0);                         // move.w d0,d3
            d3 = setb(d3, Mem.ub(a0 + ((short) d6) * 4)); // move.b (a0,d6.w*4),d3
            d0 = setw(d0, d0 + d1);                    // add.w d1,d0
            d5 += d2;                                  // add.l d2,d5
            d5 &= d4;                                  // and.l d4,d5
            d6 = d5;                                   // move.l d5,d6
            d6 = swap(d6);                             // swap d6
            Mem.wb(a3, Mem.ub(a1 + (short) d3)); a3 += 1; // move.b (a1,d3.w),(a3)+
            d3 = setw(d3, d5);                         // move.w d5,d3
            d3 = setw(d3, (d3 & 0xFFFF) >>> 8);        // lsr.w #8,d3
            d6 = setb(d6, d3);                         // move.b d3,d6
            d3 = setw(d3, d0);                         // move.w d0,d3
            d3 = setb(d3, Mem.ub(a0 + ((short) d6) * 4)); // move.b (a0,d6.w*4),d3
            d0 = setw(d0, d0 + d1);                    // add.w d1,d0
            d5 += d2;                                  // add.l d2,d5
            d5 &= d4;                                  // and.l d4,d5
            Mem.wb(a3, Mem.ub(a1 + (short) d3)); a3 += 1; // move.b (a1,d3.w),(a3)+
        }

        // .nosingle2:
        d6 = d5;                                       // move.l d5,d6
        d6 = swap(d6);                                 // swap d6

        d7 = setw(d7, d7 - 1);                         // dbra d7,acrossscrngour
        while ((short) d7 != -1) {
            // acrossscrngour: (4 pixels par itération)
            d3 = setw(d3, d5);                         // move.w d5,d3
            d3 = setw(d3, (d3 & 0xFFFF) >>> 8);        // lsr.w #8,d3
            d6 = setb(d6, d3);                         // move.b d3,d6
            d3 = setw(d3, d0);                         // move.w d0,d3
            d3 = setb(d3, Mem.ub(a0 + ((short) d6) * 4)); // move.b (a0,d6.w*4),d3
            d0 = setw(d0, d0 + d1);                    // add.w d1,d0
            d5 += d2;                                  // add.l d2,d5
            d5 &= d4;                                  // and.l d4,d5
            d6 = d5;                                   // move.l d5,d6
            d6 = swap(d6);                             // swap d6
            Mem.wb(a3, Mem.ub(a1 + (short) d3)); a3 += 1; // move.b (a1,d3.w),(a3)+

            d3 = setw(d3, d5);                         // move.w d5,d3
            d3 = setw(d3, (d3 & 0xFFFF) >>> 8);        // lsr.w #8,d3
            d6 = setb(d6, d3);                         // move.b d3,d6
            d3 = setw(d3, d0);                         // move.w d0,d3
            d3 = setb(d3, Mem.ub(a0 + ((short) d6) * 4)); // move.b (a0,d6.w*4),d3
            d0 = setw(d0, d0 + d1);                    // add.w d1,d0
            d5 += d2;                                  // add.l d2,d5
            d5 &= d4;                                  // and.l d4,d5
            d6 = d5;                                   // move.l d5,d6
            d6 = swap(d6);                             // swap d6
            Mem.wb(a3, Mem.ub(a1 + (short) d3)); a3 += 1; // move.b (a1,d3.w),(a3)+

            d3 = setw(d3, d5);                         // move.w d5,d3
            d3 = setw(d3, (d3 & 0xFFFF) >>> 8);        // lsr.w #8,d3
            d6 = setb(d6, d3);                         // move.b d3,d6
            d3 = setw(d3, d0);                         // move.w d0,d3
            d3 = setb(d3, Mem.ub(a0 + ((short) d6) * 4)); // move.b (a0,d6.w*4),d3
            d0 = setw(d0, d0 + d1);                    // add.w d1,d0
            d5 += d2;                                  // add.l d2,d5
            d5 &= d4;                                  // and.l d4,d5
            d6 = d5;                                   // move.l d5,d6
            d6 = swap(d6);                             // swap d6
            Mem.wb(a3, Mem.ub(a1 + (short) d3)); a3 += 1; // move.b (a1,d3.w),(a3)+

            d3 = setw(d3, d5);                         // move.w d5,d3
            d3 = setw(d3, (d3 & 0xFFFF) >>> 8);        // lsr.w #8,d3
            d6 = setb(d6, d3);                         // move.b d3,d6
            d3 = setw(d3, d0);                         // move.w d0,d3
            d3 = setb(d3, Mem.ub(a0 + ((short) d6) * 4)); // move.b (a0,d6.w*4),d3
            d0 = setw(d0, d0 + d1);                    // add.w d1,d0
            d5 += d2;                                  // add.l d2,d5
            d5 &= d4;                                  // and.l d4,d5
            d6 = d5;                                   // move.l d5,d6
            d6 = swap(d6);                             // swap d6
            Mem.wb(a3, Mem.ub(a1 + (short) d3)); a3 += 1; // move.b (a1,d3.w),(a3)+

            d7 = setw(d7, d7 - 1);                     // dbra d7,acrossscrngour
        }
        return a3;                                     // rts
    }
}
