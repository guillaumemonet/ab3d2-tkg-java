package ab3d2.modules;

import ab3d2.HiresData;
import ab3d2.Mem;
import ab3d2.host.CustomChips;

import static ab3d2.M68k.divs;
import static ab3d2.M68k.mulu;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.M68k.swap;

/**
 * Traduction littérale de ab3d2_source/modules/music.s
 *
 * Replay ProTracker (mt_*) écrivant dans Paula via CustomChips :
 * a5 = base des registres du canal ($dff0a0/b0/c0/d0), a6 = structure de
 * voix (28 octets dans Mem) : +0 note(l), +2 cmd(w), +3 cmdlo(b),
 * +4 début sample(l), +8 longueur(w), +$a loopstart(l), +$e looplen(w),
 * +$10 période(w), +$12 volume(w), +$14 bit DMA(w), +$16 sens porta(b),
 * +$17 vitesse porta(b), +$18 période visée(w), +$1a cmd vibrato(b),
 * +$1b position vibrato(b).
 */
public final class Music {

    // ---- données du fichier (fin de music.s) ----
    public static final int mt_sin;
    public static final int mt_periods;

    public static final int reachedend;
    public static final int mt_speed;
    public static final int mt_songpos;
    private static final int _a0;
    public static final int mt_pattpos;
    public static final int mt_counter;

    public static final int mt_break;
    public static final int mt_dmacon;
    public static final int mt_samplestarts;
    public static final int mt_voice1;
    public static final int mt_voice2;
    public static final int mt_voice3;
    public static final int mt_voice4;

    public static final int mt_data;

    static {
        mt_sin = Mem.dcB(
                0x00, 0x18, 0x31, 0x4a, 0x61, 0x78, 0x8d, 0xa1, 0xb4, 0xc5, 0xd4, 0xe0, 0xeb, 0xf4, 0xfa, 0xfd,
                0xff, 0xfd, 0xfa, 0xf4, 0xeb, 0xe0, 0xd4, 0xc5, 0xb4, 0xa1, 0x8d, 0x78, 0x61, 0x4a, 0x31, 0x18);

        mt_periods = Mem.dcW(
                0x0358, 0x0328, 0x02fa, 0x02d0, 0x02a6, 0x0280, 0x025c, 0x023a, 0x021a, 0x01fc, 0x01e0,
                0x01c5, 0x01ac, 0x0194, 0x017d, 0x0168, 0x0153, 0x0140, 0x012e, 0x011d, 0x010d, 0x00fe,
                0x00f0, 0x00e2, 0x00d6, 0x00ca, 0x00be, 0x00b4, 0x00aa, 0x00a0, 0x0097, 0x008f, 0x0087,
                0x007f, 0x0078, 0x0071, 0x0000, 0x0000);

        reachedend = Mem.dcB(0);
        mt_speed = Mem.dcB(6);
        mt_songpos = Mem.dcB(0);
        _a0 = Mem.align(2);
        mt_pattpos = Mem.dcW(0);
        mt_counter = Mem.dcB(0);

        mt_break = Mem.dcB(0);
        mt_dmacon = Mem.dcW(0);
        mt_samplestarts = Mem.alloc(4 * 0x1f);   // ds.l $1f
        mt_voice1 = Mem.alloc(2 * 10);           // ds.w 10
        Mem.dcW(1);                              // dc.w 1 (bit DMA, offset $14)
        Mem.alloc(2 * 3);                        // ds.w 3
        mt_voice2 = Mem.alloc(2 * 10);
        Mem.dcW(2);
        Mem.alloc(2 * 3);
        mt_voice3 = Mem.alloc(2 * 10);
        Mem.dcW(4);
        Mem.alloc(2 * 3);
        mt_voice4 = Mem.alloc(2 * 10);
        Mem.dcW(8);
        Mem.alloc(2 * 3);

        mt_data = Mem.dcL(0);
    }

    private Music() {
    }

    /** mt_init */
    public static void mt_init() {
        int a0 = Mem.l(mt_data);                       // move.l mt_data,a0
        int a1 = a0;                                   // move.l a0,a1
        a1 += 0x3b8;                                   // add.l #$3b8,a1
        int d0 = 0x7f;                                 // moveq #$7f,d0
        int d1 = 0;                                    // moveq #0,d1
        int d2;

        mt_loop:
        while (true) { // mt_loop:
            d2 = d1;                                   // move.l d1,d2
            d0 = setw(d0, d0 - 1);                     // subq.w #1,d0
            while (true) { // mt_lop2:
                d1 = setb(d1, Mem.ub(a1)); a1 += 1;    // move.b (a1)+,d1
                if ((byte) d1 > (byte) d2) {           // cmp.b d2,d1 ; bgt.s mt_loop
                    continue mt_loop;
                }
                d0 = setw(d0, d0 - 1);                 // dbf d0,mt_lop2
                if ((short) d0 == -1) {
                    break mt_loop;
                }
            }
        }
        d2 = setb(d2, d2 + 1);                         // addq.b #1,d2

        a1 = mt_samplestarts;                          // lea mt_samplestarts(pc),a1
        d2 <<= 8;                                      // asl.l #8,d2
        d2 <<= 2;                                      // asl.l #2,d2
        d2 += 0x43c;                                   // add.l #$43c,d2
        d2 += a0;                                      // add.l a0,d2
        int a2 = d2;                                   // move.l d2,a2
        d0 = 0x1e;                                     // moveq #$1e,d0
        do { // mt_lop3:
            d1 = 0;                                    // moveq #0,d1
            d1 = setw(d1, Mem.uw(a0 + 42));            // move.w 42(a0),d1
            // Avoid writing past end of allocated buffer for empty (last) samples
            if ((short) d1 != 0) {                     // beq .empty
                Mem.wl(a2, 0);                         // clr.l (a2)
                Mem.wl(a1, a2); a1 += 4;               // move.l a2,(a1)+
            } else {
                // .empty:
                Mem.wl(a1, HiresData.nullsample); a1 += 4; // move.l #nullsample,(a1)+
            }
            // .next:
            d1 <<= 1;                                  // asl.l #1,d1
            a2 += d1;                                  // add.l d1,a2
            a0 += 0x1e;                                // add.l #$1e,a0
            d0 = setw(d0, d0 - 1);                     // dbf d0,mt_lop3
        } while ((short) d0 != -1);

        CustomChips.ciaOr(0x2);                        // or.b #$2,$bfe001
        Mem.wb(mt_speed, 0x6);                         // move.b #$6,mt_speed
        CustomChips.write16(0xdff0a8, 0);              // clr.w $dff0a8
        CustomChips.write16(0xdff0b8, 0);              // clr.w $dff0b8
        CustomChips.write16(0xdff0c8, 0);              // clr.w $dff0c8
        CustomChips.write16(0xdff0d8, 0);              // clr.w $dff0d8
        Mem.wb(mt_songpos, 0);                         // clr.b mt_songpos
        Mem.wb(mt_counter, 0);                         // clr.b mt_counter
        Mem.ww(mt_pattpos, 0);                         // clr.w mt_pattpos
        // rts
    }

    /** mt_end */
    public static void mt_end() {
        CustomChips.write16(0xdff0a8, 0);              // clr.w $dff0a8
        CustomChips.write16(0xdff0b8, 0);              // clr.w $dff0b8
        CustomChips.write16(0xdff0c8, 0);              // clr.w $dff0c8
        CustomChips.write16(0xdff0d8, 0);              // clr.w $dff0d8
        CustomChips.write16(0xdff096, 0xf);            // move.w #$f,$dff096
        // rts
    }

    /** mt_music — tick appelé à chaque frame (interruption). */
    public static void mt_music() {
        // movem.l d0-d4/a0-a3/a5-a6,-(a7) — locaux en Java
        int a0 = Mem.l(mt_data);                       // move.l mt_data,a0
        Mem.wb(mt_counter, Mem.ub(mt_counter) + 1);    // addq.b #$1,mt_counter
        int d0 = Mem.ub(mt_counter);                   // move.b mt_counter,d0
        if ((byte) d0 < (byte) Mem.b(mt_speed)) {      // cmp.b mt_speed,d0 ; blt.s mt_nonew
            // mt_nonew:
            int a6 = mt_voice1;                        // lea mt_voice1(pc),a6
            int a5 = 0xdff0a0;                         // lea $dff0a0,a5
            mt_checkcom(a5, a6);                       // bsr mt_checkcom
            if (Mem.b(HiresData.UseAllChannels) != 0) { // tst.b UseAllChannels ; beq mt_endr
                a6 = mt_voice2;                        // lea mt_voice2(pc),a6
                a5 = 0xdff0b0;                         // lea $dff0b0,a5
                mt_checkcom(a5, a6);                   // bsr mt_checkcom
                a6 = mt_voice3;                        // lea mt_voice3(pc),a6
                a5 = 0xdff0c0;                         // lea $dff0c0,a5
                mt_checkcom(a5, a6);                   // bsr mt_checkcom
                a6 = mt_voice4;                        // lea mt_voice4(pc),a6
                a5 = 0xdff0d0;                         // lea $dff0d0,a5
                mt_checkcom(a5, a6);                   // bsr mt_checkcom
            }
            mt_endr();                                 // bra mt_endr
            return;
        }
        Mem.wb(mt_counter, 0);                         // clr.b mt_counter
        mt_getnew();                                   // bra mt_getnew
    }

    /** mt_arpeggio (a5, a6) */
    private static void mt_arpeggio(int a5, int a6) {
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(mt_counter));             // move.b mt_counter,d0
        d0 = divs(d0, 3);                              // divs #$3,d0
        d0 = swap(d0);                                 // swap d0
        int d2;
        if ((short) d0 == 0) {                         // cmp.w #$0,d0 ; beq.s mt_arp2
            // mt_arp2:
            d2 = Mem.uw(a6 + 0x10);                    // move.w $10(a6),d2
            CustomChips.write16(a5 + 0x6, d2);         // mt_arp4: move.w d2,$6(a5)
            return;                                    // rts
        }
        if ((short) d0 == 2) {                         // cmp.w #$2,d0 ; beq.s mt_arp1
            // mt_arp1:
            d0 = 0;                                    // moveq #0,d0
            d0 = setb(d0, Mem.ub(a6 + 0x3));           // move.b $3(a6),d0
            d0 = setb(d0, d0 & 0xf);                   // and.b #$f,d0
        } else {
            d0 = 0;                                    // moveq #0,d0
            d0 = setb(d0, Mem.ub(a6 + 0x3));           // move.b $3(a6),d0
            d0 = setb(d0, (d0 & 0xFF) >>> 4);          // lsr.b #4,d0
        }
        // mt_arp3:
        d0 = setw(d0, d0 << 1);                        // asl.w #1,d0
        int d1 = 0;                                    // moveq #0,d1
        d1 = setw(d1, Mem.uw(a6 + 0x10));              // move.w $10(a6),d1
        int ta0 = mt_periods;                          // lea mt_periods(pc),a0
        int d7 = 0x24;                                 // moveq #$24,d7
        do { // mt_arploop:
            d2 = Mem.uw(ta0 + (short) d0);             // move.w (a0,d0.w),d2
            if ((short) d1 >= Mem.w(ta0)) {            // cmp.w (a0),d1 ; bge.s mt_arp4
                CustomChips.write16(a5 + 0x6, d2);     // mt_arp4: move.w d2,$6(a5)
                return;                                // rts
            }
            ta0 += 2;                                  // addq.l #2,a0
            d7 = setw(d7, d7 - 1);                     // dbf d7,mt_arploop
        } while ((short) d7 != -1);
        // rts
    }

    /** mt_getnew */
    private static void mt_getnew() {
        int a0 = Mem.l(mt_data);                       // move.l mt_data,a0
        int a3 = a0;                                   // move.l a0,a3
        int a2 = a0;                                   // move.l a0,a2
        a3 += 0xc;                                     // add.l #$c,a3
        a2 += 0x3b8;                                   // add.l #$3b8,a2
        a0 += 0x43c;                                   // add.l #$43c,a0

        int d0 = 0;                                    // moveq #0,d0
        int d1 = 0;                                    // move.l d0,d1
        d0 = setb(d0, Mem.ub(mt_songpos));             // move.b mt_songpos,d0
        d1 = setb(d1, Mem.ub(a2 + (short) d0));        // move.b (a2,d0.w),d1
        d1 <<= 8;                                      // asl.l #8,d1
        d1 <<= 2;                                      // asl.l #2,d1
        d1 += Mem.w(mt_pattpos);                       // add.w mt_pattpos,d1 (add.w : mot signé)
        Mem.ww(mt_dmacon, 0);                          // clr.w mt_dmacon

        int a5 = 0xdff0a0;                             // lea $dff0a0,a5
        int a6 = mt_voice1;                            // lea mt_voice1(pc),a6
        d1 = mt_playvoice(a0, a3, d1, a5, a6);         // bsr mt_playvoice
        if (Mem.b(HiresData.UseAllChannels) != 0) {    // tst.b UseAllChannels ; beq mt_setdma
            a5 = 0xdff0b0;                             // lea $dff0b0,a5
            a6 = mt_voice2;                            // lea mt_voice2(pc),a6
            d1 = mt_playvoice(a0, a3, d1, a5, a6);     // bsr mt_playvoice
            a5 = 0xdff0c0;                             // lea $dff0c0,a5
            a6 = mt_voice3;                            // lea mt_voice3(pc),a6
            d1 = mt_playvoice(a0, a3, d1, a5, a6);     // bsr mt_playvoice
            a5 = 0xdff0d0;                             // lea $dff0d0,a5
            a6 = mt_voice4;                            // lea mt_voice4(pc),a6
            d1 = mt_playvoice(a0, a3, d1, a5, a6);     // bsr mt_playvoice
        }
        mt_setdma();                                   // bra mt_setdma
    }

    /** mt_playvoice (a0 = patterns, a3 = infos samples, d1 = offset note, a5, a6) → d1 avancé */
    private static int mt_playvoice(int a0, int a3, int d1, int a5, int a6) {
        Mem.wl(a6, Mem.l(a0 + d1));                    // move.l (a0,d1.l),(a6)
        d1 += 4;                                       // addq.l #4,d1
        int d2 = 0;                                    // moveq #0,d2
        d2 = setb(d2, Mem.ub(a6 + 0x2));               // move.b $2(a6),d2
        d2 = setb(d2, d2 & 0xf0);                      // and.b #$f0,d2
        d2 = setb(d2, (d2 & 0xFF) >>> 4);              // lsr.b #4,d2
        int d0 = Mem.ub(a6);                           // move.b (a6),d0
        d0 = d0 & 0xf0;                                // and.b #$f0,d0
        d2 = setb(d2, d0 | d2);                        // or.b d0,d2
        if ((byte) d2 != 0) {                          // tst.b d2 ; beq.s mt_setregs
            int d3 = 0;                                // moveq #0,d3
            int a1 = mt_samplestarts;                  // lea mt_samplestarts(pc),a1
            int d4 = d2;                               // move.l d2,d4
            d2 -= 1;                                   // subq.l #$1,d2
            d2 <<= 2;                                  // asl.l #2,d2
            d4 = mulu(d4, 0x1e);                       // mulu #$1e,d4
            Mem.wl(a6 + 0x4, Mem.l(a1 + d2));          // move.l (a1,d2.l),$4(a6)
            Mem.ww(a6 + 0x8, Mem.uw(a3 + d4));         // move.w (a3,d4.l),$8(a6)
            Mem.ww(a6 + 0x12, Mem.uw(a3 + d4 + 0x2));  // move.w $2(a3,d4.l),$12(a6)
            d3 = setw(d3, Mem.uw(a3 + d4 + 0x4));      // move.w $4(a3,d4.l),d3
            if ((short) d3 != 0) {                     // tst.w d3 ; beq.s mt_noloop
                d2 = Mem.l(a6 + 0x4);                  // move.l $4(a6),d2
                d3 = setw(d3, d3 << 1);                // asl.w #1,d3
                d2 += d3;                              // add.l d3,d2
                Mem.wl(a6 + 0xa, d2);                  // move.l d2,$a(a6)
                d0 = Mem.uw(a3 + d4 + 0x4);            // move.w $4(a3,d4.l),d0
                d0 = setw(d0, d0 + Mem.uw(a3 + d4 + 0x6)); // add.w $6(a3,d4.l),d0
                Mem.ww(a6 + 8, d0);                    // move.w d0,8(a6)
                Mem.ww(a6 + 0xe, Mem.uw(a3 + d4 + 0x6)); // move.w $6(a3,d4.l),$e(a6)
                d0 = Mem.uw(a6 + 0x12);                // move.w $12(a6),d0
                CustomChips.write16(a5 + 0x8, d0);     // move.w d0,$8(a5)
                // bra.s mt_setregs
            } else {
                // mt_noloop:
                d2 = Mem.l(a6 + 0x4);                  // move.l $4(a6),d2
                d2 += d3;                              // add.l d3,d2
                Mem.wl(a6 + 0xa, d2);                  // move.l d2,$a(a6)
                Mem.ww(a6 + 0xe, Mem.uw(a3 + d4 + 0x6)); // move.w $6(a3,d4.l),$e(a6)
                d0 = Mem.uw(a6 + 0x12);                // move.w $12(a6),d0
                CustomChips.write16(a5 + 0x8, d0);     // move.w d0,$8(a5)
            }
        }
        // mt_setregs:
        d0 = Mem.uw(a6);                               // move.w (a6),d0
        d0 = d0 & 0xfff;                               // and.w #$fff,d0
        if (d0 != 0) {                                 // beq mt_checkcom2
            d0 = setb(d0, Mem.ub(a6 + 0x2));           // move.b $2(a6),d0
            d0 = setb(d0, d0 & 0xF);                   // and.b #$F,d0
            if ((byte) d0 == 0x3) {                    // cmp.b #$3,d0 ; bne.s mt_setperiod
                mt_setmyport(a6);                      // bsr mt_setmyport
                mt_checkcom2(a5, a6);                  // bra mt_checkcom2
                return d1;
            }
            // mt_setperiod:
            Mem.ww(a6 + 0x10, Mem.uw(a6));             // move.w (a6),$10(a6)
            Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) & 0xfff); // and.w #$fff,$10(a6)
            d0 = Mem.uw(a6 + 0x14);                    // move.w $14(a6),d0
            CustomChips.write16(0xdff096, d0);         // move.w d0,$dff096
            Mem.wb(a6 + 0x1b, 0);                      // clr.b $1b(a6)

            CustomChips.write32(a5, Mem.l(a6 + 0x4));  // move.l $4(a6),(a5)
            CustomChips.write16(a5 + 0x4, Mem.uw(a6 + 0x8)); // move.w $8(a6),$4(a5)
            d0 = Mem.uw(a6 + 0x10);                    // move.w $10(a6),d0
            d0 = d0 & 0xfff;                           // and.w #$fff,d0
            CustomChips.write16(a5 + 0x6, d0);         // move.w d0,$6(a5)
            d0 = Mem.uw(a6 + 0x14);                    // move.w $14(a6),d0
            Mem.ww(mt_dmacon, Mem.uw(mt_dmacon) | d0); // or.w d0,mt_dmacon
        }
        mt_checkcom2(a5, a6);                          // bra mt_checkcom2
        return d1;
    }

    /** mt_setdma */
    private static void mt_setdma() {
        int d0 = 250;                                  // move.w #250,d0
        do { // mt_wait:
            Mem.ww(HiresData.testchip, Mem.uw(HiresData.testchip) + 1); // add.w #1,testchip
            d0 = setw(d0, d0 - 1);                     // dbra d0,mt_wait
        } while ((short) d0 != -1);
        d0 = Mem.uw(mt_dmacon);                        // move.w mt_dmacon,d0
        d0 = setw(d0, d0 | 0x8000);                    // or.w #$8000,d0
        if (Mem.b(HiresData.UseAllChannels) == 0) {    // tst.b UseAllChannels ; bne.s .splib
            d0 = setw(d0, d0 & 0b1111111111110001);    // and.w #%1111111111110001,d0
        }
        // .splib:
        CustomChips.write16(0xdff096, d0);             // move.w d0,$dff096
        d0 = 250;                                      // move.w #250,d0
        do { // mt_wait2:
            Mem.ww(HiresData.testchip, Mem.uw(HiresData.testchip) + 1); // add.w #1,testchip
            d0 = setw(d0, d0 - 1);                     // dbra d0,mt_wait2
        } while ((short) d0 != -1);
        int a5 = 0xdff000;                             // lea $dff000,a5
        int a6;
        if (Mem.b(HiresData.UseAllChannels) != 0) {    // tst.b UseAllChannels ; beq.s noall
            a6 = mt_voice4;                            // lea mt_voice4(pc),a6
            CustomChips.write32(a5 + 0xd0, Mem.l(a6 + 0xa)); // move.l $a(a6),$d0(a5)
            CustomChips.write16(a5 + 0xd4, Mem.uw(a6 + 0xe)); // move.w $e(a6),$d4(a5)
            a6 = mt_voice3;                            // lea mt_voice3(pc),a6
            CustomChips.write32(a5 + 0xc0, Mem.l(a6 + 0xa)); // move.l $a(a6),$c0(a5)
            CustomChips.write16(a5 + 0xc4, Mem.uw(a6 + 0xe)); // move.w $e(a6),$c4(a5)
            a6 = mt_voice2;                            // lea mt_voice2(pc),a6
            CustomChips.write32(a5 + 0xb0, Mem.l(a6 + 0xa)); // move.l $a(a6),$b0(a5)
            CustomChips.write16(a5 + 0xb4, Mem.uw(a6 + 0xe)); // move.w $e(a6),$b4(a5)
        }
        // noall:
        a6 = mt_voice1;                                // lea mt_voice1(pc),a6
        CustomChips.write32(a5 + 0xa0, Mem.l(a6 + 0xa)); // move.l $a(a6),$a0(a5)
        CustomChips.write16(a5 + 0xa4, Mem.uw(a6 + 0xe)); // move.w $e(a6),$a4(a5)

        Mem.ww(mt_pattpos, Mem.uw(mt_pattpos) + 0x10); // add.w #$10,mt_pattpos
        if (Mem.uw(mt_pattpos) == 0x400) {             // cmp.w #$400,mt_pattpos ; bne.s mt_endr
            mt_nex();
            return;
        }
        mt_endr();
    }

    /** mt_nex */
    private static void mt_nex() {
        Mem.ww(mt_pattpos, 0);                         // clr.w mt_pattpos
        Mem.wb(mt_break, 0);                           // clr.b mt_break
        Mem.wb(mt_songpos, Mem.ub(mt_songpos) + 1);    // addq.b #1,mt_songpos
        Mem.wb(mt_songpos, Mem.ub(mt_songpos) & 0x7f); // and.b #$7f,mt_songpos
        // move.b mt_songpos,d1 (comparaison à la fin de chanson commentée dans l'original)
        mt_endr();
    }

    /** mt_endr */
    private static void mt_endr() {
        if (Mem.b(mt_break) != 0) {                    // tst.b mt_break ; bne.s mt_nex
            mt_nex();
            return;
        }
        // movem.l (a7)+,... ; rts
    }

    /** mt_setmyport (a6) */
    private static void mt_setmyport(int a6) {
        int d2 = Mem.uw(a6);                           // move.w (a6),d2
        d2 = d2 & 0xfff;                               // and.w #$fff,d2
        Mem.ww(a6 + 0x18, d2);                         // move.w d2,$18(a6)
        int d0 = Mem.uw(a6 + 0x10);                    // move.w $10(a6),d0
        Mem.wb(a6 + 0x16, 0);                          // clr.b $16(a6)
        if ((short) d2 == (short) d0) {                // cmp.w d0,d2 ; beq.s mt_clrport
            // mt_clrport:
            Mem.ww(a6 + 0x18, 0);                      // clr.w $18(a6)
            return;                                    // mt_rt: rts
        }
        if ((short) d2 >= (short) d0) {                // bge.s mt_rt
            return;
        }
        Mem.wb(a6 + 0x16, 0x1);                        // move.b #$1,$16(a6)
        // rts
    }

    /** mt_myport (a5, a6) */
    private static void mt_myport(int a5, int a6) {
        int d0 = Mem.ub(a6 + 0x3);                     // move.b $3(a6),d0
        if ((byte) d0 != 0) {                          // beq.s mt_myslide
            Mem.wb(a6 + 0x17, d0);                     // move.b d0,$17(a6)
            Mem.wb(a6 + 0x3, 0);                       // clr.b $3(a6)
        }
        // mt_myslide:
        if (Mem.w(a6 + 0x18) == 0) {                   // tst.w $18(a6) ; beq.s mt_rt
            return;
        }
        d0 = 0;                                        // moveq #0,d0
        d0 = setb(d0, Mem.ub(a6 + 0x17));              // move.b $17(a6),d0
        if (Mem.b(a6 + 0x16) != 0) {                   // tst.b $16(a6) ; bne.s mt_mysub
            // mt_mysub:
            Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) - d0); // sub.w d0,$10(a6)
            d0 = Mem.uw(a6 + 0x18);                    // move.w $18(a6),d0
            if ((short) d0 < Mem.w(a6 + 0x10)) {       // cmp.w $10(a6),d0 ; blt.s mt_myok
                CustomChips.write16(a5 + 0x6, Mem.uw(a6 + 0x10)); // mt_myok: move.w $10(a6),$6(a5)
                return;                                // rts
            }
            Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x18));      // move.w $18(a6),$10(a6)
            Mem.ww(a6 + 0x18, 0);                      // clr.w $18(a6)
            CustomChips.write16(a5 + 0x6, Mem.uw(a6 + 0x10)); // move.w $10(a6),$6(a5)
            return;                                    // rts
        }
        Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) + d0);     // add.w d0,$10(a6)
        d0 = Mem.uw(a6 + 0x18);                        // move.w $18(a6),d0
        if ((short) d0 <= Mem.w(a6 + 0x10)) {          // cmp.w $10(a6),d0 ; bgt.s mt_myok
            Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x18));      // move.w $18(a6),$10(a6)
            Mem.ww(a6 + 0x18, 0);                      // clr.w $18(a6)
        }
        // mt_myok:
        CustomChips.write16(a5 + 0x6, Mem.uw(a6 + 0x10)); // move.w $10(a6),$6(a5)
        // rts
    }

    /** mt_vib (a5, a6) */
    private static void mt_vib(int a5, int a6) {
        int d0 = Mem.ub(a6 + 0x3);                     // move.b $3(a6),d0
        if ((byte) d0 != 0) {                          // beq.s mt_vi
            Mem.wb(a6 + 0x1a, d0);                     // move.b d0,$1a(a6)
        }
        // mt_vi:
        d0 = Mem.ub(a6 + 0x1b);                        // move.b $1b(a6),d0
        int a4 = mt_sin;                               // lea mt_sin(pc),a4
        d0 = setw(d0, (d0 & 0xFFFF) >>> 2);            // lsr.w #$2,d0
        d0 = setw(d0, d0 & 0x1f);                      // and.w #$1f,d0
        int d2 = 0;                                    // moveq #0,d2
        d2 = setb(d2, Mem.ub(a4 + (short) d0));        // move.b (a4,d0.w),d2
        d0 = setb(d0, Mem.ub(a6 + 0x1a));              // move.b $1a(a6),d0
        d0 = setw(d0, d0 & 0xf);                       // and.w #$f,d0
        d2 = mulu(d2, d0);                             // mulu d0,d2
        d2 = setw(d2, (d2 & 0xFFFF) >>> 6);            // lsr.w #$6,d2
        d0 = setw(d0, Mem.uw(a6 + 0x10));              // move.w $10(a6),d0
        if (Mem.b(a6 + 0x1b) < 0) {                    // tst.b $1b(a6) ; bmi.s mt_vibmin
            // mt_vibmin:
            d0 = setw(d0, d0 - d2);                    // sub.w d2,d0
        } else {
            d0 = setw(d0, d0 + d2);                    // add.w d2,d0
        }
        // mt_vib2:
        CustomChips.write16(a5 + 0x6, d0);             // move.w d0,$6(a5)
        d0 = setb(d0, Mem.ub(a6 + 0x1a));              // move.b $1a(a6),d0
        d0 = setw(d0, (d0 & 0xFFFF) >>> 2);            // lsr.w #$2,d0
        d0 = setw(d0, d0 & 0x3c);                      // and.w #$3c,d0
        Mem.wb(a6 + 0x1b, Mem.ub(a6 + 0x1b) + d0);     // add.b d0,$1b(a6)
        // rts
    }

    /** mt_checkcom (a5, a6) */
    private static void mt_checkcom(int a5, int a6) {
        int d0 = Mem.uw(a6 + 0x2);                     // move.w $2(a6),d0
        d0 = d0 & 0xfff;                               // and.w #$fff,d0
        if (d0 == 0) {                                 // beq.s mt_nop
            // mt_nop:
            CustomChips.write16(a5 + 0x6, Mem.uw(a6 + 0x10)); // move.w $10(a6),$6(a5)
            return;                                    // rts
        }
        d0 = setb(d0, Mem.ub(a6 + 0x2));               // move.b $2(a6),d0
        d0 = setb(d0, d0 & 0xf);                       // and.b #$f,d0
        if ((byte) d0 == 0) {                          // tst.b d0 ; beq mt_arpeggio
            mt_arpeggio(a5, a6);
            return;
        }
        if ((byte) d0 == 0x1) {                        // cmp.b #$1,d0 ; beq.s mt_portup
            mt_portup(a5, a6);
            return;
        }
        if ((byte) d0 == 0x2) {                        // cmp.b #$2,d0 ; beq mt_portdown
            mt_portdown(a5, a6);
            return;
        }
        if ((byte) d0 == 0x3) {                        // cmp.b #$3,d0 ; beq mt_myport
            mt_myport(a5, a6);
            return;
        }
        if ((byte) d0 == 0x4) {                        // cmp.b #$4,d0 ; beq mt_vib
            mt_vib(a5, a6);
            return;
        }
        CustomChips.write16(a5 + 0x6, Mem.uw(a6 + 0x10)); // move.w $10(a6),$6(a5)
        if ((byte) d0 == 0xa) {                        // cmp.b #$a,d0 ; beq.s mt_volslide
            mt_volslide(a5, a6);
        }
        // rts
    }

    /** mt_volslide (a5, a6) */
    private static void mt_volslide(int a5, int a6) {
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a6 + 0x3));               // move.b $3(a6),d0
        d0 = setb(d0, (d0 & 0xFF) >>> 4);              // lsr.b #4,d0
        if ((byte) d0 == 0) {                          // tst.b d0 ; beq.s mt_voldown
            // mt_voldown:
            d0 = 0;                                    // moveq #0,d0
            d0 = setb(d0, Mem.ub(a6 + 0x3));           // move.b $3(a6),d0
            d0 = setb(d0, d0 & 0xf);                   // and.b #$f,d0
            Mem.ww(a6 + 0x12, Mem.uw(a6 + 0x12) - d0); // sub.w d0,$12(a6)
            if (Mem.w(a6 + 0x12) < 0) {                // bpl.s mt_vol3
                Mem.ww(a6 + 0x12, 0);                  // clr.w $12(a6)
            }
            // mt_vol3:
            CustomChips.write16(a5 + 0x8, Mem.uw(a6 + 0x12)); // move.w $12(a6),d0 ; move.w d0,$8(a5)
            return;                                    // rts
        }
        Mem.ww(a6 + 0x12, Mem.uw(a6 + 0x12) + d0);     // add.w d0,$12(a6)
        if (Mem.w(a6 + 0x12) >= 0x40) {                // cmp.w #$40,$12(a6) ; bmi.s mt_vol2
            Mem.ww(a6 + 0x12, 0x40);                   // move.w #$40,$12(a6)
        }
        // mt_vol2:
        CustomChips.write16(a5 + 0x8, Mem.uw(a6 + 0x12)); // move.w $12(a6),d0 ; move.w d0,$8(a5)
        // rts
    }

    /** mt_portup (a5, a6) */
    private static void mt_portup(int a5, int a6) {
        int d0 = 0;                                    // moveq #0,d0
        d0 = setb(d0, Mem.ub(a6 + 0x3));               // move.b $3(a6),d0
        Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) - d0);     // sub.w d0,$10(a6)
        d0 = setw(d0, Mem.uw(a6 + 0x10));              // move.w $10(a6),d0
        d0 = setw(d0, d0 & 0xfff);                     // and.w #$fff,d0
        if ((short) d0 < 0x71) {                       // cmp.w #$71,d0 ; bpl.s mt_por2
            Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) & 0xf000); // and.w #$f000,$10(a6)
            Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) | 0x71);   // or.w #$71,$10(a6)
        }
        // mt_por2:
        d0 = setw(d0, Mem.uw(a6 + 0x10));              // move.w $10(a6),d0
        d0 = setw(d0, d0 & 0xfff);                     // and.w #$fff,d0
        CustomChips.write16(a5 + 0x6, d0);             // move.w d0,$6(a5)
        // rts
    }

    /** mt_portdown (a5, a6) */
    private static void mt_portdown(int a5, int a6) {
        int d0 = 0;                                    // clr.w d0
        d0 = setb(d0, Mem.ub(a6 + 0x3));               // move.b $3(a6),d0
        Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) + d0);     // add.w d0,$10(a6)
        d0 = setw(d0, Mem.uw(a6 + 0x10));              // move.w $10(a6),d0
        d0 = setw(d0, d0 & 0xfff);                     // and.w #$fff,d0
        if ((short) d0 >= 0x358) {                     // cmp.w #$358,d0 ; bmi.s mt_por3
            Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) & 0xf000); // and.w #$f000,$10(a6)
            Mem.ww(a6 + 0x10, Mem.uw(a6 + 0x10) | 0x358);  // or.w #$358,$10(a6)
        }
        // mt_por3:
        d0 = setw(d0, Mem.uw(a6 + 0x10));              // move.w $10(a6),d0
        d0 = setw(d0, d0 & 0xfff);                     // and.w #$fff,d0
        CustomChips.write16(a5 + 0x6, d0);             // move.w d0,$6(a5)
        // rts
    }

    /** mt_checkcom2 (a5, a6) */
    private static void mt_checkcom2(int a5, int a6) {
        int d0 = Mem.ub(a6 + 0x2);                     // move.b $2(a6),d0
        d0 = d0 & 0xf;                                 // and.b #$f,d0
        if ((byte) d0 == 0xe) {                        // cmp.b #$e,d0 ; beq.s mt_setfilt
            // mt_setfilt:
            d0 = Mem.ub(a6 + 0x3);                     // move.b $3(a6),d0
            d0 = (d0 & 0x1) << 1;                      // and.b #$1,d0 ; asl.b #$1,d0
            CustomChips.ciaAnd(0xfd);                  // and.b #$fd,$bfe001
            CustomChips.ciaOr(d0);                     // or.b d0,$bfe001
            return;                                    // rts
        }
        if ((byte) d0 == 0xd) {                        // cmp.b #$d,d0 ; beq.s mt_pattbreak
            // mt_pattbreak:
            Mem.wb(mt_break, ~Mem.ub(mt_break));       // not.b mt_break
            return;                                    // rts
        }
        if ((byte) d0 == 0xb) {                        // cmp.b #$b,d0 ; beq.s mt_posjmp
            // mt_posjmp:
            Mem.wb(reachedend, 0xFF);                  // st reachedend
            d0 = Mem.ub(a6 + 0x3);                     // move.b $3(a6),d0
            d0 = setb(d0, d0 - 1);                     // subq.b #$1,d0
            Mem.wb(mt_songpos, d0);                    // move.b d0,mt_songpos
            Mem.wb(mt_break, ~Mem.ub(mt_break));       // not.b mt_break
            return;                                    // rts
        }
        if ((byte) d0 == 0xc) {                        // cmp.b #$c,d0 ; beq.s mt_setvol
            // mt_setvol:
            if ((byte) Mem.b(a6 + 0x3) > 0x40) {       // cmp.b #$40,$3(a6) ; ble.s mt_vol4
                Mem.wb(a6 + 0x3, 0x40);                // move.b #$40,$3(a6)
            }
            // mt_vol4:
            d0 = Mem.ub(a6 + 0x3);                     // move.b $3(a6),d0
            CustomChips.write16(a5 + 0x8, d0);         // move.w d0,$8(a5)
            return;                                    // rts
        }
        if ((byte) d0 == 0xf) {                        // cmp.b #$f,d0 ; beq.s mt_setspeed
            // mt_setspeed:
            if ((byte) Mem.b(a6 + 0x3) > 0x1f) {       // cmp.b #$1f,$3(a6) ; ble.s mt_sets
                Mem.wb(a6 + 0x3, 0x1f);                // move.b #$1f,$3(a6)
            }
            // mt_sets:
            d0 = Mem.ub(a6 + 0x3);                     // move.b $3(a6),d0
            if ((byte) d0 != 0) {                      // beq.s mt_rts2
                Mem.wb(mt_speed, d0);                  // move.b d0,mt_speed
                Mem.wb(mt_counter, 0);                 // clr.b mt_counter
            }
            // mt_rts2: rts
        }
        // rts
    }
}
