package ab3d2.data;

import ab3d2.Assets;
import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/data/tables_data.s
 */
public final class TablesData {

    public static final int MAX_ONE_OVER_N = 511;

    /** sine/cosine << 15, contains two full cycles (720 degrees) over 8192 entries */
    public static final int SinCosTable_vw;

    /** the size of one complete cycle - not the actual size of the table */
    public static final int SINE_SIZE = 4096;

    public static final int SINE_OFS = 0;
    public static final int COSINE_OFS = SINE_SIZE / 2;

    /** Modulus mask value when doing *address* based calculation, e.g. (a0,dN.w) */
    public static final int SINTAB_MASK_ADR = (SINE_SIZE * 2) - 2;

    /** Modulus mask value when doing *index* based calculation, e.g. (a0, dN.w*2) */
    public static final int SINTAB_MASK_IDX = (SINE_SIZE * 2) - 1;

    /** stores x/3 and x mod 3 for x=0...660 */
    public static final int DivThreeTable_vb;

    static {
        Mem.align(4);
        SinCosTable_vw = Assets.incbin("bigsine");

        // DivThreeTable_vb : REPT 220 { dc.b val,0 ; dc.b val,1 ; dc.b val,2 ; val++ }
        DivThreeTable_vb = Mem.allocTop();
        for (int val = 0; val < 220; val++) {
            Mem.dcB(val, 0);
            Mem.dcB(val, 1);
            Mem.dcB(val, 2);
        }
    }

    private TablesData() {
    }

    /** AMOD_A : and.w #SINTAB_MASK_ADR,dN — modulo d'angle (masque adresse). */
    public static int AMOD_A(int reg) {
        return ab3d2.M68k.setw(reg, reg & SINTAB_MASK_ADR);
    }

    /** AMOD_I : and.w #SINTAB_MASK_IDX,dN — modulo d'angle (masque index). */
    public static int AMOD_I(int reg) {
        return ab3d2.M68k.setw(reg, reg & SINTAB_MASK_IDX);
    }
}
