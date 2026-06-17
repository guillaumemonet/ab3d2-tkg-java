package ab3d2.modules;

import ab3d2.ControlloopData;
import ab3d2.Mem;
import ab3d2.c.MenuC;
import ab3d2.host.DosLib;
import ab3d2.host.ExecLib;
import ab3d2.menu.Menunb;
import ab3d2.menu.MenunbData;

import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.bss.IoBss.IO_DOSFileHandle_l;
import static ab3d2.bss.IoBss.IO_MemType_l;
import static ab3d2.bss.IoBss.io_BlockLength_l;
import static ab3d2.bss.IoBss.io_BlockStart_l;
import static ab3d2.bss.IoBss.io_EndOfQueue_l;
import static ab3d2.bss.IoBss.io_FileInfoBlock_vb;
import static ab3d2.bss.SystemBss.Sys_Workspace_vl;

/**
 * Traduction littérale de ab3d2_source/modules/file_io.s
 *
 * "Definitions specific to the loading of data from disk.
 *  Mostly refactored from newloadfromdisk.s and wallchunk.s"
 *
 * TODO (original): It's possible that some resources are leaked if a fatal
 * error occurs during the loading process, but for now that seems better
 * than crashing.
 *
 * Convention de retour : les routines de chargement renvoient (d0=adresse,
 * d1=longueur) — encodés ici dans un long : (d0 << 32) | (d1 & 0xFFFFFFFF) ;
 * cf. addr(r) / len(r).
 *
 * unLHA : l'original incbin un blob de code machine ("decomp4.raw"). Les
 * assets de ce portage sont déjà dépackés (cf. mémoire projet) — le chemin
 * '=SB=' ne doit jamais se présenter ; unLHA lève donc une exception.
 */
public final class FileIo {

    public static final int IO_MAX_FILENAME_LEN = 79;

    public static final int MEMF_ANY = 0;

    // Données locales de io_LoadSample (CNOP 0,4)
    private static final int compressed_sample_position_l;
    private static final int compressed_sample_size_l;
    private static final int sample_position_l;
    private static final int sample_size_l;
    private static final int fibonnaci_lookup_vb;

    // Données locales de io_HandlePacked (CNOP 0,4)
    private static final int unpacked_start_l;
    private static final int unpacked_length_l;

    // section .bss : .unlha_temp_buffer_vl ds.l 4096 ("unLHA wants 16kb")
    private static final int unlha_temp_buffer_vl;

    static {
        Mem.align(4);
        compressed_sample_position_l = Mem.dcL(0);
        compressed_sample_size_l = Mem.dcL(0);
        sample_position_l = Mem.dcL(0);
        sample_size_l = Mem.dcL(0);
        fibonnaci_lookup_vb = Mem.dcB(-34, -21, -13, -8, -5, -3, -2, -1, 0, 1, 2, 3, 5, 8, 13, 21);

        Mem.align(4);
        unpacked_start_l = Mem.dcL(0);
        unpacked_length_l = Mem.dcL(0);

        Mem.align(4);
        unlha_temp_buffer_vl = Mem.alloc(4 * 4096);
    }

    private FileIo() {
    }

    /** Adresse (d0) d'un résultat de chargement. */
    public static int addr(long r) {
        return (int) (r >>> 32);
    }

    /** Longueur (d1) d'un résultat de chargement. */
    public static int len(long r) {
        return (int) r;
    }

    private static long result(int d0, int d1) {
        return ((long) d0 << 32) | (d1 & 0xFFFFFFFFL);
    }

    // *****************************************************************************
    // * IO Queue
    // *****************************************************************************

    /** IO_InitQueue */
    public static void IO_InitQueue() {
        Mem.wl(io_EndOfQueue_l, Sys_Workspace_vl);     // move.l #Sys_Workspace_vl,io_EndOfQueue_l
        // rts
    }

    /**
     * IO_QueueFile — a0 = pointer to filename, d0 = ptr to dest. of addr,
     * d1 = ptr to dest. of len., IO_MemType_l = type of memory.
     */
    public static void IO_QueueFile(int a0, int d0, int d1) {
        // SAVEREGS — locaux en Java
        int a1 = Mem.l(io_EndOfQueue_l);               // move.l io_EndOfQueue_l,a1
        Mem.wl(a1, d0); a1 += 4;                       // move.l d0,(a1)+
        Mem.wl(a1, d1); a1 += 4;                       // move.l d1,(a1)+
        Mem.wl(a1, Mem.l(IO_MemType_l)); a1 += 4;      // move.l IO_MemType_l,(a1)+
        int dc = IO_MAX_FILENAME_LEN;                  // move.w #IO_MAX_FILENAME_LEN,d0

        do { // .copy_name:
            Mem.wb(a1, Mem.ub(a0)); a0 += 1; a1 += 1;  // move.b (a0)+,(a1)+
            dc -= 1;                                   // dbra d0,.copy_name
        } while ((short) dc != -1);
        Mem.wl(io_EndOfQueue_l, Mem.l(io_EndOfQueue_l) + 100); // add.l #100,io_EndOfQueue_l
        // GETREGS ; rts
    }

    /** IO_FlushQueue */
    public static void IO_FlushQueue() {
        int d6 = io_FlushPass();                       // bsr io_FlushPass

        while (true) { // .retry:
            if (Mem.b(ControlloopData.LOADEXT) != 0) { // tst.b LOADEXT ; bne .loaded_all
                return;                                // .loaded_all: rts
            }
            if (d6 == 0) {                             // tst.b d6 ; beq .loaded_all
                return;
            }

            // Find first unloaded file and prompt for disk.
            int a2 = Sys_Workspace_vl;                 // move.l #Sys_Workspace_vl,a2

            while (Mem.l(a2) == 0) {                   // .find_loop: tst.l (a2) ; bne.s .found_unloaded
                a2 += 100;                             // add.l #100,a2
            }                                          // bra.s .find_loop

            // .found_unloaded: A2 points at an unloaded file thingy. Prompt for the disk.
            int a3 = MenunbData.mnu_diskline;          // move.l #mnu_diskline,a3
            Mem.wl(a3, 0x20202020); a3 += 4;           // move.l #$20202020,(a3)+
            Mem.wl(a3, 0x20202020); a3 += 4;           // move.l #$20202020,(a3)+
            Mem.wl(a3, 0x20202020); a3 += 4;           // move.l #$20202020,(a3)+
            Mem.wl(a3, 0x20202020); a3 += 4;           // move.l #$20202020,(a3)+
            Mem.wl(a3, 0x20202020); a3 += 4;           // move.l #$20202020,(a3)+

            a3 = MenunbData.mnu_diskline + 10;         // move.l #mnu_diskline+10,a3
            int d0 = -1;                               // moveq #-1,d0
            int a4 = a2;                               // move.l a2,a4
            a4 += 12;                                  // add.l #12,a4

            do { // .not_found_loop:
                d0 += 1;                               // addq #1,d0
            } while (Mem.ub(a4++) != ':');             // cmp.b #':',(a4)+ ; bne.s .not_found_loop

            int d1 = setw(0, d0);                      // move.w d0,d1
            d1 = setw(d1, ((short) d1) >> 1);          // asr.w #1,d1
            a3 -= (short) d1;                          // sub.w d1,a3
            a4 = a2;                                   // move.l a2,a4
            a4 += 12;                                  // add.l #12,a4

            do { // .volume_name_loop:
                Mem.wb(a3, Mem.ub(a4)); a4 += 1; a3 += 1; // move.b (a4)+,(a3)+
                d0 -= 1;                               // dbra d0,.volume_name_loop
            } while ((short) d0 != -1);

            // SAVEREGS
            MenuC.mnu_setscreen();                     // CALLC mnu_setscreen
            Menunb.mnu_domenu(Menunb.mnu_askfordisk);  // lea mnu_askfordisk,a0 ; jsr mnu_domenu
            MenuC.mnu_clearscreen(1);                  // moveq #1,d0 ; Fade out ; CALLC mnu_clearscreen
            // GETREGS

            if (Mem.b(ControlloopData.Game_ShouldQuit_b) != 0) { // tst.b Game_ShouldQuit_b ; beq .no_quit
                int a5 = a2 + 12;                      // lea 12(a2),a5
                io_LoadFailure(a5);                    // bra io_LoadFailure
                return; // (io_LoadFailure ne revient pas)
            }
            // .no_quit:
            d6 = io_FlushPass();                       // bsr io_FlushPass
            // bra .retry
        }
    }

    /** io_FlushPass — renvoie d6 (0xFF si au moins un fichier a échoué, 0 sinon). */
    private static int io_FlushPass() {
        int a2 = Sys_Workspace_vl;                     // move.l #Sys_Workspace_vl,a2
        int d7 = 0;                                    // moveq #0,d7 ; loaded a file
        int d6 = 0;                                    // moveq #0,d6 ; tried+failed

        while (true) { // .do_flush:
            int d0 = a2;                               // move.l a2,d0
            if (d0 >= Mem.l(io_EndOfQueue_l)) {        // cmp.l io_EndOfQueue_l,d0 ; bge.s .flushed
                return d6;                             // .flushed: rts
            }

            if (Mem.l(a2) != 0) {                      // tst.l (a2) ; beq.s .load_completed
                int a0 = a2 + 12;                      // lea 12(a2),a0 ; ptr to name
                Mem.wl(IO_MemType_l, Mem.l(a2 + 8));   // move.l 8(a2),IO_MemType_l
                d0 = io_TryToOpen(a0);                 // jsr io_TryToOpen

                if (d0 == 0) {                         // tst.l d0 ; beq.s .load_failed
                    // .load_failed:
                    d6 = setb(d6, 0xFF);               // st d6
                } else {
                    Mem.wl(IO_DOSFileHandle_l, d0);    // move.l d0,IO_DOSFileHandle_l
                    long r = io_LoadAndUnpackFile();   // jsr io_LoadAndUnpackFile
                    d0 = addr(r);
                    int d1 = len(r);

                    d7 = setb(d7, 0xFF);               // st d7
                    int a3 = Mem.l(a2);                // move.l (a2),a3
                    Mem.wl(a3, d0);                    // move.l d0,(a3)
                    int dl = Mem.l(a2 + 4);            // move.l 4(a2),d0
                    if (dl != 0) {                     // beq.s .nolenstore
                        Mem.wl(dl, d1);                // move.l d0,a3 ; move.l d1,(a3)
                    }
                    // .nolenstore:
                    Mem.wl(a2, 0);                     // move.l #0,(a2)
                }
            }
            // .load_completed:
            a2 += 100;                                 // add.l #100,a2
            // bra .do_flush
        }
    }

    /** io_TryToOpen — a0 = nom ; renvoie le handle dos (0 si échec). Préserve les registres. */
    private static int io_TryToOpen(int a0) {
        // movem.l d1-d7/a0-a6,-(a7)
        int d1 = a0;                                   // move.l a0,d1
        int d2 = DosLib.MODE_OLDFILE;                  // move.l #MODE_OLDFILE,d2
        int d0 = DosLib.Open(d1, d2);                  // CALLDOS Open
        // movem.l (a7)+,d1-d7/a0-a6 ; rts
        return d0;
    }

    // *****************************************************************************
    // * File Load
    // *****************************************************************************

    /**
     * io_LoadAndUnpackFile — load a file in and unpack it if necessary.
     * (IO_DOSFileHandle_l déjà positionné). Returns address (d0) and length (d1).
     */
    private static long io_LoadAndUnpackFile() {
        // SAVEREGS ; bra.s io_LoadCommon
        return io_LoadCommon(0);
    }

    /** IO_LoadFileOptional — load an optional file, i.e. one that might not exist. */
    public static long IO_LoadFileOptional(int a0) {
        // SAVEREGS
        int d1 = a0;                                   // move.l a0,d1
        int a5 = a0;                                   // move.l a0,a5 ; Save filename for error reporting
        int d2 = DosLib.MODE_OLDFILE;                  // move.l #MODE_OLDFILE,d2
        int d0 = DosLib.Open(d1, d2);                  // CALLDOS Open

        Mem.wl(IO_DOSFileHandle_l, d0);                // move.l d0,IO_DOSFileHandle_l
        if (d0 != 0) {                                 // bne.s io_LoadCommon
            return io_LoadCommon(a5);
        }
        // GETREGS
        return result(0, 0);                           // clr.l d0 ; clr.l d1 ; rts
    }

    /**
     * IO_LoadFile — load a file in and unpack it if necessary.
     * a0 = pointer to name. Returns address (d0) and length (d1).
     */
    public static long IO_LoadFile(int a0) {
        // SAVEREGS
        int d1 = a0;                                   // move.l a0,d1
        int a5 = a0;                                   // move.l a0,a5 ; Save filename for error reporting
        int d2 = DosLib.MODE_OLDFILE;                  // move.l #MODE_OLDFILE,d2
        int d0 = DosLib.Open(d1, d2);                  // CALLDOS Open

        Mem.wl(IO_DOSFileHandle_l, d0);                // move.l d0,IO_DOSFileHandle_l
        if (d0 == 0) {                                 // beq io_LoadFailure
            io_LoadFailure(a5);
        }
        return io_LoadCommon(a5);
    }

    /** io_LoadCommon (a5 = nom pour le rapport d'erreur, 0 si inconnu) */
    private static long io_LoadCommon(int a5name) {
        int a5 = io_FileInfoBlock_vb;                  // lea io_FileInfoBlock_vb,a5
        int d1 = Mem.l(IO_DOSFileHandle_l);            // move.l IO_DOSFileHandle_l,d1
        int d2 = a5;                                   // move.l a5,d2
        DosLib.ExamineFH(d1, d2);                      // CALLDOS ExamineFH

        int d0 = Mem.l(a5 + DosLib.fib_Size);          // move.l fib_Size(a5),d0
        Mem.wl(io_BlockLength_l, d0);                  // move.l d0,io_BlockLength_l
        d0 += 8;                                       // add.l #8,d0 ; over-allocate by 8 bytes
        d1 = Mem.l(IO_MemType_l);                      // move.l IO_MemType_l,d1
        d0 = Sys.Sys_AllocVec(d0, d1);                 // jsr Sys_AllocVec

        Mem.wl(io_BlockStart_l, d0);                   // move.l d0,io_BlockStart_l
        d1 = Mem.l(IO_DOSFileHandle_l);                // move.l IO_DOSFileHandle_l,d1
        d2 = d0;                                       // move.l d0,d2
        int d3 = Mem.l(io_BlockLength_l);              // move.l io_BlockLength_l,d3
        DosLib.Read(d1, d2, d3);                       // CALLDOS Read

        d1 = Mem.l(IO_DOSFileHandle_l);                // move.l IO_DOSFileHandle_l,d1
        DosLib.Close(d1);                              // CALLDOS Close

        int a0 = Mem.l(io_BlockStart_l);               // move.l io_BlockStart_l,a0
        Mem.wl(a0 + d3, 0);                            // clr.l (a0,d3.l) ; clear last 8 bytes
        Mem.wl(a0 + d3 + 4, 0);                        // clr.l 4(a0,d3.l)
        d0 = Mem.l(a0);                                // move.l (a0),d0
        if (d0 == 0x3D53423D) {                        // cmp.l #'=SB=',d0 ; beq io_HandlePacked
            return io_HandlePacked(a0);
        }

        d0 = Mem.l(io_BlockStart_l);                   // move.l io_BlockStart_l,d0
        d1 = Mem.l(io_BlockLength_l);                  // move.l io_BlockLength_l,d1
        a0 = d0;                                       // move.l d0,a0
        if (Mem.l(a0) == 0x43534658) {                 // cmp.l #'CSFX',(a0) ; beq io_LoadSample
            return io_LoadSample(d0, d1);
        }

        // Not a packed file so just return now.
        // GETREGS
        return result(Mem.l(io_BlockStart_l), Mem.l(io_BlockLength_l)); // rts
    }

    /** io_LoadFailure — a5 = filename. Ne revient pas (Sys_FatalError). */
    private static void io_LoadFailure(int a5) {
        // .errfmt: dc.b 'Error loading file:',10,'%s',0 — émis dans Mem une seule fois
        int fmt = errfmt();
        int args = Mem.alloc(4);                       // move.l a5,-(a7) ; move.l a7,a1
        Mem.wl(args, a5);
        // move.l #1,d0 ; Error code 1 (transmis à Sys_FatalError via d0, inutilisé par celui-ci)
        Sys.Sys_FatalError(fmt, args);                 // bra Sys_FatalError
    }

    private static int errfmtAddr;

    private static int errfmt() {
        if (errfmtAddr == 0) {
            errfmtAddr = Mem.dcStr("Error loading file:");
            Mem.dcB(10);
            Mem.dcStr("%s");
            Mem.dcB(0);
            Mem.align(2); // even
        }
        return errfmtAddr;
    }

    /**
     * io_LoadSample — décompression Fibonacci-delta des samples 'CSFX'
     * (d0 = adresse du bloc 'CSFX', d1 = taille du bloc).
     */
    private static long io_LoadSample(int d0, int d1) {
        d0 += 4;                                       // add.l #4,d0 ; Skip "CSFX"
        Mem.wl(compressed_sample_size_l, d1);          // move.l d1,.compressed_sample_size_l
        int a0 = d0;                                   // move.l d0,a0
        d0 = Mem.l(a0); a0 += 4;                       // move.l (a0)+,d0 ; file size
        Mem.wl(sample_size_l, d0);                     // move.l d0,.sample_size_l
        Mem.wl(compressed_sample_position_l, a0);      // move.l a0,.compressed_sample_position_l
        d1 = MEMF_ANY;                                 // move.l #MEMF_ANY,d1
        d0 = Sys.Sys_AllocVec(d0, d1);                 // jsr Sys_AllocVec
        Mem.wl(sample_position_l, d0);                 // move.l d0,.sample_position_l
        a0 = Mem.l(compressed_sample_position_l);      // move.l .compressed_sample_position_l,a0
        int a1 = d0;                                   // move.l d0,a1
        d0 = Mem.l(sample_size_l);                     // move.l .sample_size_l,d0
        d0 = setw(d0, d0 - 2);                         // sub.w #2,d0
        d1 = setb(d1, Mem.ub(a0)); a0 += 1;            // move.b (a0)+,d1 ; first byte (actual value)
        Mem.wb(a1, d1); a1 += 1;                       // move.b d1,(a1)+
        int a2 = fibonnaci_lookup_vb;                  // lea .fibonnaci_lookup_vb(pc),a2

        int d2, d3, d4;
        decompress:
        while (true) { // .decompress_loop:
            d2 = Mem.ub(a0); a0 += 1;                  // move.b (a0)+,d2
            d2 = setw(d2, d2 & 0x00FF);                // and.w #$00ff,d2
            d3 = setw(0, d2);                          // move.w d2,d3
            d2 = setw(d2, (d2 & 0xFFFF) >>> 4);        // lsr.w #4,d2
            d3 = setw(d3, d3 & 0x000F);                // and.w #$000f,d3
            d4 = Mem.b(a2 + (short) d2);               // move.b (a2,d2.w),d4 ; first fib value
            d1 = setb(d1, d1 + d4);                    // add.b d4,d1
            Mem.wb(a1, d1); a1 += 1;                   // move.b d1,(a1)+ ; store sample value
            d0 = setw(d0, d0 - 1);                     // dbra d0,.continue
            if ((short) d0 == -1) {
                break;                                 // bra.s .sample_finished
            }
            // .continue:
            d4 = Mem.b(a2 + (short) d3);               // move.b (a2,d3.w),d4 ; second fib value
            d1 = setb(d1, d1 + d4);                    // add.b d4,d1
            Mem.wb(a1, d1); a1 += 1;                   // move.b d1,(a1)+ ; store sample value
            d0 = setw(d0, d0 - 1);                     // dbra d0,.decompress_loop
            if ((short) d0 == -1) {
                break;
            }
        }

        // .sample_finished:
        a1 = Mem.l(compressed_sample_position_l);      // move.l .compressed_sample_position_l,a1
        a1 -= 8;                                       // sub.l #8,a1
        ExecLib.FreeVec(a1);                           // CALLEXEC FreeVec

        // Now check the sample and clip it if it ever gets too big
        a0 = Mem.l(sample_position_l);                 // move.l .sample_position_l,a0
        d0 = Mem.l(sample_size_l);                     // move.l .sample_size_l,d0
        d0 = setw(d0, d0 - 1);                         // sub.w #1,d0
        do { // .clip_loop:
            d1 = Mem.b(a0);                            // move.b (a0),d1
            if ((byte) d1 >= 64) {                     // cmp.b #64,d1 ; blt.s .not_too_big
                d1 = setb(d1, 63);                     // move.b #63,d1
            }
            // .not_too_big:
            if ((byte) d1 < -64) {                     // cmp.b #-64,d1 ; bge.s .not_too_small
                d1 = setb(d1, -64);                    // move.b #-64,d1
            }
            // .not_too_small:
            Mem.wb(a0, d1); a0 += 1;                   // move.b d1,(a0)+
            d0 = setw(d0, d0 - 1);                     // dbra d0,.clip_loop
        } while ((short) d0 != -1);

        // GETREGS
        return result(Mem.l(sample_position_l), Mem.l(sample_size_l)); // rts
    }

    /** io_HandlePacked — a0 = début du bloc '=SB=' (LHA packé). */
    private static long io_HandlePacked(int a0) {
        int d0 = Mem.l(a0 + 4);                        // move.l 4(a0),d0 ; length of unpacked file.
        Mem.wl(unpacked_length_l, d0);                 // move.l d0,.unpacked_length_l
        int d1 = Mem.l(IO_MemType_l);                  // move.l IO_MemType_l,d1
        d0 = Sys.Sys_AllocVec(d0, d1);                 // jsr Sys_AllocVec

        Mem.wl(unpacked_start_l, d0);                  // move.l d0,.unpacked_start_l
        d0 = Mem.l(io_BlockStart_l);                   // move.l io_BlockStart_l,d0
        d1 = 0;                                        // moveq #0,d1
        int ua0 = Mem.l(unpacked_start_l);             // move.l .unpacked_start_l,a0
        int ua1 = unlha_temp_buffer_vl;                // move.l #.unlha_temp_buffer_vl,a1
        int ua2 = 0;                                   // lea $0,a2
        unLHA(d0, d1, ua0, ua1, ua2);                  // jsr unLHA

        d1 = Mem.l(io_BlockStart_l);                   // move.l io_BlockStart_l,d1
        ExecLib.FreeVec(d1);                           // move.l d1,a1 ; CALLEXEC FreeVec

        d0 = Mem.l(unpacked_start_l);                  // move.l .unpacked_start_l,d0
        d1 = Mem.l(unpacked_length_l);                 // move.l .unpacked_length_l,d1
        ua0 = d0;                                      // move.l d0,a0
        if (Mem.l(ua0) == 0x43534658) {                // cmp.l #'CSFX',(a0) ; beq io_LoadSample
            return io_LoadSample(d0, d1);
        }

        // GETREGS
        return result(Mem.l(unpacked_start_l), Mem.l(unpacked_length_l)); // rts
    }

    /**
     * unLHA (_unLHA::) — l'original est du code machine binaire
     * (incbin "decomp4.raw", dépacker LHA). Les assets du portage sont
     * déjà dépackés : aucun fichier '=SB=' ne doit être rencontré.
     */
    public static void unLHA(int d0, int d1, int a0, int a1, int a2) {
        throw new UnsupportedOperationException(
                "unLHA (decomp4.raw) : fichier packé '=SB=' rencontré alors que les assets sont censés être dépackés");
    }
}
