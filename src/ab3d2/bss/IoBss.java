package ab3d2.bss;

import ab3d2.Mem;
import ab3d2.SystemInc;

/**
 * Traduction littérale de ab3d2_source/bss/io_bss.s
 */
public final class IoBss {

    private static final int _align0 = Mem.align(4);

    /** Memory class to use for next loaded entity */
    public static final int IO_MemType_l = Mem.alloc(4);

    /** dos.library file handle */
    public static final int IO_DOSFileHandle_l = Mem.alloc(4);

    // Private stuff
    public static final int io_EndOfQueue_l = Mem.alloc(4);

    // Array of object pointers
    // io_ObjectPointers_vl: ds.l 160 (commenté dans l'original)

    // block properties
    public static final int io_BlockLength_l = Mem.alloc(4);
    public static final int io_BlockName_l = Mem.alloc(4);
    public static final int io_BlockStart_l = Mem.alloc(4);

    /** Pointer to the file extension (i.e. the substring starting at .) */
    public static final int io_FileExtPointer_l = Mem.alloc(4);

    public static final int io_ObjectName_vb = Mem.alloc(160);
    public static final int io_Buffer_vb = Mem.alloc(80);     // todo - can these be merged ?

    /** File info block (_io_FileInfoBlock::) */
    public static final int io_FileInfoBlock_vb = Mem.alloc(SystemInc.fib_SIZEOF);

    private IoBss() {
    }
}
