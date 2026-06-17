package ab3d2.host;

import ab3d2.Mem;

/**
 * Couche hôte : timer.device / ReadEClock d'AmigaOS.
 *
 * Sur Amiga, ReadEClock() lit l'horloge E (CIA), écrit la valeur 64 bits
 * {ev_hi, ev_lo} (deux ULONG big-endian) à l'adresse fournie et renvoie la
 * fréquence de l'horloge E (≈ 709379 Hz en PAL). On reproduit ce comportement
 * à partir de System.nanoTime() ramené à la cadence de l'horloge E.
 */
public final class SysTimer {

    /** Fréquence de l'horloge E (PAL : CLK/10 = 3546895/5 ≈ 709379 Hz). */
    public static final int ECLOCK_RATE = 709379;

    private SysTimer() {
    }

    /**
     * ReadEClock(dest) : écrit le compteur 64 bits courant (ev_hi:ev_lo,
     * big-endian) à l'adresse dest et renvoie la fréquence de l'horloge.
     */
    public static int readEClock(int dest) {
        long ticks = (System.nanoTime() / 1000L) * ECLOCK_RATE / 1_000_000L; // µs → ticks d'horloge E
        Mem.wl(dest, (int) (ticks >>> 32));   // ev_hi
        Mem.wl(dest + 4, (int) ticks);        // ev_lo
        return ECLOCK_RATE;
    }

    // struct timeval { ULONG tv_secs; ULONG tv_micro; } (8 octets)
    private static final int TV_SECS = 0;
    private static final int TV_MICRO = 4;

    /** GetSysTime(dest) : écrit l'heure système courante dans une timeval. */
    public static void GetSysTime(int dest) {
        long nanos = System.nanoTime();
        Mem.wl(dest + TV_SECS, (int) (nanos / 1_000_000_000L));         // tv_secs
        Mem.wl(dest + TV_MICRO, (int) ((nanos / 1000L) % 1_000_000L));  // tv_micro
    }

    /** SubTime(dest, src) : dest -= src (soustraction de timeval avec retenue). */
    public static void SubTime(int dest, int src) {
        long destMicro = Mem.l(dest + TV_MICRO) & 0xFFFFFFFFL;
        long srcMicro = Mem.l(src + TV_MICRO) & 0xFFFFFFFFL;
        long destSecs = Mem.l(dest + TV_SECS) & 0xFFFFFFFFL;
        long srcSecs = Mem.l(src + TV_SECS) & 0xFFFFFFFFL;
        if (destMicro < srcMicro) {
            destSecs -= 1;
            destMicro += 1_000_000L;
        }
        destMicro -= srcMicro;
        destSecs -= srcSecs;
        Mem.wl(dest + TV_SECS, (int) destSecs);
        Mem.wl(dest + TV_MICRO, (int) destMicro);
    }
}
