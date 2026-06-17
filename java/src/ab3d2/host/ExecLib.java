package ab3d2.host;

import ab3d2.Mem;

/**
 * Couche hôte : sous-ensemble d'exec.library utilisé par le jeu.
 *
 * AllocVec/FreeVec : servis par l'allocateur bump de Mem (taille cachée en
 * en-tête comme l'original ; FreeVec ne récupère pas — 64 Mo suffisent, à
 * revisiter si besoin). MEMF_CLEAR est implicite (mémoire jamais réutilisée,
 * donc toujours vierge).
 *
 * RawDoFmt : émulation du formateur exec — IMPORTANT : par défaut les
 * arguments sont des WORDs ; le modificateur 'l' lit des LONGs. Le NUL final
 * est transmis au callback, comme sur l'Amiga.
 */
public final class ExecLib {

    // exec/memory.i
    public static final int MEMF_PUBLIC = 1;
    public static final int MEMF_CHIP = 2;
    public static final int MEMF_FAST = 4;
    public static final int MEMF_CLEAR = 0x10000;

    private ExecLib() {
    }

    /** AllocVec(d0=byteSize, d1=requirements) → adresse ou 0. */
    public static int AllocVec(int byteSize, int requirements) {
        if (byteSize <= 0) {
            return 0;
        }
        int base = Mem.allocAligned(byteSize + 8, 8);
        Mem.wl(base, byteSize + 8); // taille cachée, comme exec
        return base + 8;
    }

    /** FreeVec(a1=adresse). No-op (pas de récupération). */
    public static void FreeVec(int addr) {
    }

    /** AllocMem/FreeMem : mêmes services pour les appels directs. */
    public static int AllocMem(int byteSize, int requirements) {
        return AllocVec(byteSize, requirements);
    }

    public static void FreeMem(int addr, int byteSize) {
    }

    /** CopyMem(a0=source, a1=dest, d0=size) — copie octet par octet (sens croissant). */
    public static void CopyMem(int source, int dest, int size) {
        for (int i = 0; i < size; i++) {
            Mem.wb(dest + i, Mem.ub(source + i));
        }
    }

    /** Callback caractère de RawDoFmt (équivalent du PutChProc). */
    public interface PutCh {
        void put(int ch);
    }

    /**
     * RawDoFmt(a0=format (chaîne C dans Mem), a1=flux d'arguments (dans Mem),
     * sink=PutChProc). Supporte %%, %c, %d, %u, %x, %s avec modificateur 'l',
     * largeur, zéro-padding et justification '-' (sous-ensemble exec).
     */
    public static void RawDoFmt(int a0, int a1, PutCh sink) {
        while (true) {
            int c = Mem.ub(a0++);
            if (c != '%') {
                sink.put(c);
                if (c == 0) {
                    return; // le NUL terminal est transmis puis on s'arrête
                }
                continue;
            }
            // séquence % :
            boolean leftJustify = false;
            boolean zeroPad = false;
            int width = 0;
            int limit = -1;
            c = Mem.ub(a0++);
            if (c == '-') {
                leftJustify = true;
                c = Mem.ub(a0++);
            }
            if (c == '0') {
                zeroPad = true;
            }
            while (c >= '0' && c <= '9') {
                width = width * 10 + (c - '0');
                c = Mem.ub(a0++);
            }
            if (c == '.') {
                limit = 0;
                c = Mem.ub(a0++);
                while (c >= '0' && c <= '9') {
                    limit = limit * 10 + (c - '0');
                    c = Mem.ub(a0++);
                }
            }
            boolean isLong = false;
            if (c == 'l') {
                isLong = true;
                c = Mem.ub(a0++);
            }
            String out;
            switch (c) {
                case '%':
                    out = "%";
                    break;
                case 'c': {
                    int v = Mem.uw(a1); a1 += 2; // %c lit un word
                    out = String.valueOf((char) (v & 0xFF));
                    break;
                }
                case 'd': {
                    int v;
                    if (isLong) {
                        v = Mem.l(a1); a1 += 4;
                    } else {
                        v = Mem.w(a1); a1 += 2;
                    }
                    out = Integer.toString(v);
                    break;
                }
                case 'u': {
                    long v;
                    if (isLong) {
                        v = Mem.l(a1) & 0xFFFFFFFFL; a1 += 4;
                    } else {
                        v = Mem.uw(a1); a1 += 2;
                    }
                    out = Long.toString(v);
                    break;
                }
                case 'x': {
                    long v;
                    if (isLong) {
                        v = Mem.l(a1) & 0xFFFFFFFFL; a1 += 4;
                    } else {
                        v = Mem.uw(a1); a1 += 2;
                    }
                    out = Long.toHexString(v).toUpperCase();
                    break;
                }
                case 's': {
                    int p = Mem.l(a1); a1 += 4;
                    out = p == 0 ? "" : Mem.cstr(p);
                    if (limit >= 0 && out.length() > limit) {
                        out = out.substring(0, limit);
                    }
                    break;
                }
                case 'b': { // BSTR (BCPL) — longueur préfixée
                    int p = Mem.l(a1) << 2; a1 += 4;
                    int len = Mem.ub(p);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        sb.append((char) Mem.ub(p + 1 + i));
                    }
                    out = sb.toString();
                    break;
                }
                default:
                    out = "%" + (char) c; // séquence inconnue : recopiée
                    break;
            }
            // padding
            while (!leftJustify && out.length() < width) {
                out = (zeroPad ? "0" : " ") + out;
            }
            while (leftJustify && out.length() < width) {
                out = out + " ";
            }
            for (int i = 0; i < out.length(); i++) {
                sink.put(out.charAt(i) & 0xFF);
            }
        }
    }
}
