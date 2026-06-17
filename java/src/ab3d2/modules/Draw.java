package ab3d2.modules;

import ab3d2.ControlloopData;
import ab3d2.Hires;
import ab3d2.HiresData;
import ab3d2.HireswallData;
import ab3d2.Mem;

import static ab3d2.M68k.muls;
import static ab3d2.M68k.negw;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;

/**
 * Traduction littérale de ab3d2_source/modules/draw.s
 *
 * "Ad hoc drawing routines — Refactored from various places"
 */
public final class Draw {

    private static final int _a0 = Mem.align(4);

    // move me (commentaire d'origine)
    public static final int firstdigit_b = Mem.dcB(0);
    public static final int secdigit_b = Mem.dcB(0);
    public static final int thirddigit_b = Mem.dcB(0);
    public static final int gunny_b = Mem.dcB(0);

    private static final int _a1 = Mem.align(4);

    /**
     * TODO (original) - this should be definable by mod and/or user as they are
     * currently defined by the default palette
     */
    public static final int Draw_CrosshairPens_vb = Mem.dcB(
            0,    // off
            255,  // intense green
            254,  // mid green
            190,  // intense yellow
            25,   // bright grey
            250,  // intense red
            133,  // ice blue
            69);  // intense blue

    private Draw() {
    }

    /** Draw_Crosshair */
    public static void Draw_Crosshair() {
        // Get the pen
        int d0 = 0;                                    // clr.l d0
        d0 = setb(d0, Mem.ub(ControlloopData.Prefs_CrossHairColour_b)); // move.b Prefs_CrossHairColour_b,d0
        d0 = setb(d0, d0 & 7);                         // and.b #7,d0 ; paranoia
        int a1 = Draw_CrosshairPens_vb;                // move.l #Draw_CrosshairPens_vb,a1
        a1 += (short) d0;                              // add.w d0,a1
        if (Mem.b(a1) == 0) {                          // tst.b (a1) ; beq .done
            return;                                    // .done: rts
        }

        int a0 = Mem.l(Vid_FastBufferPtr_l);           // move.l Vid_FastBufferPtr_l,a0
        a0 += Mem.w(HiresData.Vid_CentreX_w);          // add.w Vid_CentreX_w,a0
        d0 = Mem.w(HireswallData.Vid_BottomY_w);       // move.w Vid_BottomY_w,d0
        d0 = muls(d0, Hires.SCREEN_WIDTH / 2);         // muls.w #SCREEN_WIDTH/2,d0
        // ***************************************************************
        // dirty hack for fullscreen to allow the crosshair to match 2/3 screen
        // position while looking for a more robust solution.
        // ***************************************************************
        if (Mem.b(Vid_FullScreen_b) != 0) {            // tst.b Vid_FullScreen_b ; beq.s .small
            int d1 = Mem.w(HireswallData.STOPOFFSET);  // move.w STOPOFFSET,d1
            if (d1 > 0) {                              // ble.s .above
                d1 = muls(d1, 256 / 9);                // muls.w #(256/9),d1
                d1 >>= 8;                              // asr.l #8,d1
                d1 = setb(d1, d1 & 0xFE);              // and.b #$fe,d1
                d1 = muls(d1, Hires.SCREEN_WIDTH);     // muls.w #SCREEN_WIDTH,d1
                d0 = setw(d0, d0 + d1);                // add.w d1,d0
                // bra.s .below
            } else {
                // .above:
                d1 = negw(d1);                         // neg.w d1
                d1 = muls(d1, 256 / 9);                // muls.w #(256/9),d1
                d1 >>= 8;                              // asr.l #8,d1
                d1 = setb(d1, d1 & 0xFE);              // and.b #$fe,d1
                d1 = muls(d1, Hires.SCREEN_WIDTH);     // muls.w #SCREEN_WIDTH,d1
                d0 = setw(d0, d0 - d1);                // sub.w d1,d0
            }
            // .below:
        }
        // .small:
        a0 += d0;                                      // add.l d0,a0

        d0 = setb(d0, Mem.ub(a1));                     // move.b (a1),d0

        // TODO (original) - Mod Properties should define a mechanism to allow
        //                   per gun crosshair designs

        Mem.wb(a0 - 4 * Hires.SCREEN_WIDTH - 4, d0);   // move.b d0,-4*SCREEN_WIDTH-4(a0) ; TL
        Mem.wb(a0 - 4 * Hires.SCREEN_WIDTH + 4, d0);   // move.b d0,-4*SCREEN_WIDTH+4(a0) ; TR
        Mem.wb(a0 - 2 * Hires.SCREEN_WIDTH - 2, d0);   // move.b d0,-2*SCREEN_WIDTH-2(a0) ; TL
        Mem.wb(a0 - 2 * Hires.SCREEN_WIDTH + 2, d0);   // move.b d0,-2*SCREEN_WIDTH+2(a0) ; TR

        Mem.wb(a0 + 2 * Hires.SCREEN_WIDTH - 2, d0);   // move.b d0,2*SCREEN_WIDTH-2(a0) ; BL
        Mem.wb(a0 + 2 * Hires.SCREEN_WIDTH + 2, d0);   // move.b d0,2*SCREEN_WIDTH+2(a0) ; BR
        Mem.wb(a0 + 4 * Hires.SCREEN_WIDTH - 4, d0);   // move.b d0,4*SCREEN_WIDTH-4(a0) ; BL
        Mem.wb(a0 + 4 * Hires.SCREEN_WIDTH + 4, d0);   // move.b d0,4*SCREEN_WIDTH+4(a0) ; BR
        // .done: rts
    }
}
