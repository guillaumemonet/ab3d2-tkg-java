package ab3d2.modules;

import ab3d2.Controlloop;
import ab3d2.Mem;
import ab3d2.host.ExecLib;
import ab3d2.host.FatalError;
import ab3d2.host.IntuitionLib;

import static ab3d2.M68k.setw;
import static ab3d2.bss.SystemBss.sys_ErrorBuffer_vb;
import static ab3d2.bss.SystemBss.sys_ErrorHeight_b;

/**
 * Traduction littérale de ab3d2_source/modules/system.s
 *
 * "system initialisation code — Refactored from dev_inst.s, hires.s etc."
 */
public final class Sys {

    public static final int SYS_ALERT_Y_SPACE = 12;

    // .errorfmt de Sys_AllocVec (chaîne embarquée dans le code)
    private static final int errorfmt;
    // zone de travail équivalente au movem.l d0-d1,-(sp) de Sys_AllocVec
    private static final int allocVecArgs = Mem.alloc(8);

    static {
        errorfmt = Mem.dcStr("Allocation failed. %ld bytes requested, flags=$%lx");
        Mem.dcB(0);
        Mem.align(2); // even
    }

    private Sys() {
    }

    /**
     * Sys_MemFillLong — dest ULONG a0, value ULONG d0, size WORD d1 (in longs).
     */
    public static void Sys_MemFillLong(int a0, int d0, int d1) {
        d1 = setw(d1, (d1 & 0xFFFF) >>> 2);            // lsr.w #2,d1 ; 4 longs per loop
        d1 = setw(d1, d1 - 1);                         // subq.w #1,d1

        do { // .fill_loop:
            Mem.wl(a0, d0); a0 += 4;                   // move.l d0,(a0)+
            Mem.wl(a0, d0); a0 += 4;                   // move.l d0,(a0)+
            Mem.wl(a0, d0); a0 += 4;                   // move.l d0,(a0)+
            Mem.wl(a0, d0); a0 += 4;                   // move.l d0,(a0)+
            d1 = setw(d1, d1 - 1);                     // dbra d1,.fill_loop
        } while ((short) d1 != -1);
        // rts
    }

    /**
     * Sys_CopyMemMove16 — copy using move16 (d0 = taille, a0 = src, a1 = dst).
     * "Don't call this if [...] less than 64 bytes or greater than 4MiB."
     */
    public static void Sys_CopyMemMove16(int d0, int a0, int a1) {
        // round the source. Is this actually needed?
        a0 = (a0 + 15) & 0xFFFFFFF0;                   // exg a0,d0 ; add.l #15,d0 ; and.l #$FFFFFFF0,d0 ; exg d0,a0
        // round the destination. Is this actually needed?
        a1 = (a1 + 15) & 0xFFFFFFF0;                   // exg a1,d0 ; ... ; exg d0,a1

        d0 >>>= 6;                                     // lsr.l #6,d0 ; 4 cache lines of 16 bytes per loop
        d0 -= 1;                                       // subq.l #1,d0

        do { // .copy_loop:
            Mem.copy(a0, a1, 16); a0 += 16; a1 += 16;  // move16 (a0)+,(a1)+
            Mem.copy(a0, a1, 16); a0 += 16; a1 += 16;  // move16 (a0)+,(a1)+
            Mem.copy(a0, a1, 16); a0 += 16; a1 += 16;  // move16 (a0)+,(a1)+
            Mem.copy(a0, a1, 16); a0 += 16; a1 += 16;  // move16 (a0)+,(a1)+
            d0 = setw(d0, d0 - 1);                     // dbra d0,.copy_loop ; assume have less than 4MB
        } while ((short) d0 != -1);
        // rts
    }

    // état du callback .putch (l'original garde le curseur dans a3)
    private static int putch_a3;

    /**
     * Sys_FatalError — prépare l'alerte puis abandonne (a0 = format,
     * a1 = arguments pour RawDoFmt).
     * "Prepare alert for later display, restore stack pointer and abort program."
     * L'équivalent du reset de pile + bra Game_Quit est l'exception FatalError
     * (cf. host.FatalError) — Game_Quit puis Sys_DisplayError sont exécutés par
     * le point de récupération.
     */
    public static void Sys_FatalError(int a0, int a1) {
        // Prepare error message, but don't display it
        // until system has been almost completely shut down.
        putch_a3 = sys_ErrorBuffer_vb;                 // lea sys_ErrorBuffer_vb,a3
        Mem.wb(sys_ErrorHeight_b, SYS_ALERT_Y_SPACE + 2); // move.b #SYS_ALERT_Y_SPACE+2,sys_ErrorHeight_b
        startline();                                   // bsr .startline
        ExecLib.RawDoFmt(a0, a1, Sys::putch);          // CALLEXEC RawDoFmt
        throw new FatalError();                        // move.l sys_RecoveryStack,a7 ; bra Game_Quit
    }

    /** .putch */
    private static void putch(int d0) {
        if ((byte) d0 == 0) {                          // tst.b d0 ; beq .end
            // .end:
            Mem.ww(putch_a3, 0);                       // clr.w (a3) ; NUL terminator and indicate last line
            return;                                    // rts
        }
        if ((byte) d0 == 10) {                         // cmp.b #10,d0 ; beq .nl
            // .nl:
            Mem.wb(putch_a3, 0); putch_a3 += 1;        // clr.b (a3)+ ; Terminate line
            Mem.wb(putch_a3, 1); putch_a3 += 1;        // move.b #1,(a3)+ ; Continue on next
            startline();                               // And start a new one
            return;
        }
        Mem.wb(putch_a3, d0); putch_a3 += 1;           // move.b d0,(a3)+
        // rts
    }

    /** .startline */
    private static void startline() {
        Mem.ww(putch_a3, 10); putch_a3 += 2;           // move.w #10,(a3)+ ; X
        Mem.wb(putch_a3, Mem.ub(sys_ErrorHeight_b)); putch_a3 += 1; // move.b sys_ErrorHeight_b,(a3)+
        Mem.wb(sys_ErrorHeight_b, Mem.ub(sys_ErrorHeight_b) + SYS_ALERT_Y_SPACE); // add.b #SYS_ALERT_Y_SPACE,...
        // rts
    }

    /** Sys_DisplayError — show alert (if any) prepared by Sys_FatalError. */
    public static void Sys_DisplayError() {
        int d1 = 0;                                    // moveq #0,d1
        d1 = (d1 & ~0xFF) | Mem.ub(sys_ErrorHeight_b); // move.b sys_ErrorHeight_b,d1
        if (d1 == 0) {                                 // beq .out
            return;                                    // .out: rts
        }
        // .has_error:
        int d0 = IntuitionLib.RECOVERY_ALERT;          // moveq #RECOVERY_ALERT,d0
        int a0 = sys_ErrorBuffer_vb;                   // lea sys_ErrorBuffer_vb,a0
        IntuitionLib.DisplayAlert(d0, a0, d1);         // CALLINT DisplayAlert
        // rts
    }

    /**
     * Sys_AllocVec — like exec/AllocVec, but calls Sys_FatalError on allocation
     * failure (d0 = taille, d1 = flags). Renvoie l'adresse allouée.
     */
    public static int Sys_AllocVec(int d0, int d1) {
        // movem.l d0-d1,-(sp) ; Save arguments
        Mem.wl(allocVecArgs, d0);
        Mem.wl(allocVecArgs + 4, d1);
        int r = ExecLib.AllocVec(d0, d1);              // CALLEXEC AllocVec
        if (r != 0) {                                  // tst.l d0 ; beq .fail
            return r;                                  // addq.l #8,sp ; rts
        }
        // .fail:
        Sys_FatalError(errorfmt, allocVecArgs);        // lea .errorfmt(pc),a0 ; move.l sp,a1 ; bra Sys_FatalError
        return 0; // inatteignable
    }

    /** Référence vers Game_Quit pour les chemins d'erreur (controlloop.s). */
    public static void gameQuitAfterFatal() {
        Controlloop.Game_Quit();
        Sys_DisplayError();
    }
}
