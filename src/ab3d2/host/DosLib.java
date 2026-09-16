package ab3d2.host;

import ab3d2.Assets;
import ab3d2.Mem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Couche hôte : sous-ensemble de dos.library.
 *
 * Résolution des chemins Amiga :
 *  - lectures : d'abord l'overlay inscriptible run/ (sauvegardes, prefs),
 *    sinon les assets originaux via Assets.resolve (medias/original) ;
 *  - écritures : toujours dans run/ (les originaux ne sont jamais modifiés) ;
 *  - "ram:xxx" → run/ram/xxx.
 *
 * Les handles sont des entiers positifs (0 = échec, comme dos).
 */
public final class DosLib {

    // libraries/dos.i
    public static final int MODE_OLDFILE = 1005;
    public static final int MODE_NEWFILE = 1006;
    public static final int MODE_READWRITE = 1004;

    public static final int OFFSET_BEGINNING = -1;
    public static final int OFFSET_CURRENT = 0;
    public static final int OFFSET_END = 1;

    /** Overlay inscriptible. */
    public static Path runDir = Path.of("run");

    private static final Map<Integer, RandomAccessFile> handles = new HashMap<>();
    private static int nextHandle = 1;

    private DosLib() {
    }

    private static Path writablePath(String amigaPath) {
        String clean = amigaPath.replace('\\', '/');
        int colon = clean.indexOf(':');
        if (colon >= 0) {
            String assign = clean.substring(0, colon).toLowerCase();
            String rest = clean.substring(colon + 1);
            clean = assign.equals("ram") ? "ram/" + rest : rest;
        }
        return runDir.resolve(clean);
    }

    /** Open(d1=nom (adresse Mem), d2=mode) → handle ou 0. */
    public static int Open(int nameAddr, int mode) {
        String name = Mem.cstr(nameAddr);
        try {
            Path overlay = writablePath(name);
            Path target;
            if (mode == MODE_NEWFILE) {
                Files.createDirectories(overlay.getParent());
                target = overlay;
            } else if (Files.isRegularFile(overlay)) {
                target = overlay; // version inscriptible déjà présente
            } else {
                Path original = Assets.resolve(name);
                if (mode == MODE_OLDFILE) {
                    if (original == null) {
                        return 0;
                    }
                    target = original;
                } else { // MODE_READWRITE
                    Files.createDirectories(overlay.getParent());
                    if (original != null) {
                        Files.copy(original, overlay); // copie sur écriture
                    }
                    target = overlay;
                }
            }
            String rafMode = (mode == MODE_OLDFILE && target != writablePath(name)) ? "r" : "rw";
            RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(target.toFile(), rafMode);
            } catch (IOException e) {
                raf = new RandomAccessFile(target.toFile(), "r"); // assets en lecture seule
            }
            if (mode == MODE_NEWFILE) {
                raf.setLength(0);
            }
            int h = nextHandle++;
            handles.put(h, raf);
            return h;
        } catch (IOException e) {
            return 0;
        }
    }

    /** Read(d1=handle, d2=buffer (adresse Mem), d3=longueur) → octets lus ou -1. */
    public static int Read(int handle, int buffer, int length) {
        RandomAccessFile raf = handles.get(handle);
        if (raf == null) {
            return -1;
        }
        try {
            byte[] tmp = new byte[length];
            int n = raf.read(tmp);
            if (n > 0) {
                System.arraycopy(tmp, 0, Mem.RAM, buffer, n);
            }
            return Math.max(n, 0);
        } catch (IOException e) {
            return -1;
        }
    }

    /** Write(d1=handle, d2=buffer (adresse Mem), d3=longueur) → octets écrits ou -1. */
    public static int Write(int handle, int buffer, int length) {
        RandomAccessFile raf = handles.get(handle);
        if (raf == null) {
            return -1;
        }
        try {
            raf.write(Mem.RAM, buffer, length);
            return length;
        } catch (IOException e) {
            return -1;
        }
    }

    /** Seek(d1=handle, d2=position, d3=mode) → ancienne position ou -1. */
    public static int Seek(int handle, int position, int mode) {
        RandomAccessFile raf = handles.get(handle);
        if (raf == null) {
            return -1;
        }
        try {
            int old = (int) raf.getFilePointer();
            switch (mode) {
                case OFFSET_BEGINNING -> raf.seek(position);
                case OFFSET_CURRENT -> raf.seek(old + position);
                case OFFSET_END -> raf.seek(raf.length() + position);
                default -> {
                    return -1;
                }
            }
            return old;
        } catch (IOException e) {
            return -1;
        }
    }

    /** dos/dosextens.i : offset de fib_Size dans struct FileInfoBlock. */
    public static final int fib_Size = 124;

    /** ExamineFH(d1=handle, d2=FileInfoBlock (adresse Mem)) → booléen dos. */
    public static int ExamineFH(int handle, int fib) {
        RandomAccessFile raf = handles.get(handle);
        if (raf == null) {
            return 0;
        }
        try {
            Mem.wl(fib + fib_Size, (int) raf.length());
            return -1; // DOSTRUE
        } catch (IOException e) {
            return 0;
        }
    }

    /** Close(d1=handle). */
    public static int Close(int handle) {
        RandomAccessFile raf = handles.remove(handle);
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException ignored) {
            }
        }
        return -1; // DOSTRUE
    }
}
