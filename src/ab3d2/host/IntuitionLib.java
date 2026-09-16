package ab3d2.host;

import ab3d2.Mem;

/**
 * Couche hôte : sous-ensemble d'intuition.library.
 */
public final class IntuitionLib {

    // intuition/intuition.i
    public static final int RECOVERY_ALERT = 0;
    public static final long DEADEND_ALERT = 0x80000000L;

    // intuition/screens.i — offsets dans struct Screen
    public static final int sc_ViewPort = 44;
    public static final int sc_RastPort = 84;

    private IntuitionLib() {
    }

    /**
     * DisplayAlert(d0=alertNumber, a0=string, d1=height).
     * Décode le format d'alerte Amiga : suites de { X (word), Y (byte),
     * caractères..., 0, octet continuation } et affiche sur stderr.
     */
    public static boolean DisplayAlert(int alertNumber, int string, int height) {
        int p = string;
        StringBuilder sb = new StringBuilder();
        while (true) {
            p += 2;          // X (word)
            p += 1;          // Y (byte)
            StringBuilder line = new StringBuilder();
            int c;
            while ((c = Mem.ub(p++)) != 0) {
                line.append((char) c);
            }
            sb.append(line).append(System.lineSeparator());
            int cont = Mem.ub(p++);
            if (cont == 0) {
                break;
            }
        }
        System.err.println("[ALERT " + Integer.toHexString(alertNumber) + "]");
        System.err.print(sb);
        return true;
    }
}
