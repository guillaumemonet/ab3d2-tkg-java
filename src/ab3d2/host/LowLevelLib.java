package ab3d2.host;

/**
 * Couche hôte : lowlevel.library d'AmigaOS (utilisée par cd32joy.s pour lire les
 * manettes CD32/joystick via ReadJoyPort).
 *
 * ReadJoyPort(port) renvoie l'état d'un port de jeu au format lowlevel :
 *   bits 31-28 = type (JP_TYPE_*), boutons en bits 17-23, directions en bits 0-3.
 * En l'absence de source d'entrée hôte branchée, renvoie 0 (= JP_TYPE_NOTAVAIL).
 * joyPortState[port] peut être renseigné par la couche d'entrée hôte.
 */
public final class LowLevelLib {

    /** Base de la librairie (0 = non ouverte). */
    public static int LowBase;

    /** État courant injecté par l'hôte pour chaque port (0 = aucune manette). */
    public static final int[] joyPortState = new int[4];

    private LowLevelLib() {
    }

    /** ReadJoyPort(d0=port 0..3) → état du port (format lowlevel.library). */
    public static int ReadJoyPort(int port) {
        return joyPortState[port & 3];
    }
}
