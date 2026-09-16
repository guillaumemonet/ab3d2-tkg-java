package ab3d2.host;

import ab3d2.Mem;

/**
 * Couche hôte : sous-ensemble de graphics.library (texte du HUD développeur).
 * Le rendu réel sera branché avec la couche vidéo ; en attendant, l'état du
 * curseur et le dernier texte sont conservés pour inspection.
 */
public final class GraphicsLib {

    public static int penX;
    public static int penY;
    public static String lastText = "";

    private GraphicsLib() {
    }

    /** Move(rp, x, y) — positionne le curseur graphique. */
    public static void Move(int rastPort, int x, int y) {
        penX = x;
        penY = y;
    }

    /** Text(rp, addr, len) — dessine len caractères depuis Mem. */
    public static void Text(int rastPort, int addr, int len) {
        if (len <= 0) {
            lastText = "";
            return;
        }
        lastText = new String(Mem.RAM, addr, len, java.nio.charset.StandardCharsets.ISO_8859_1);
        // TODO host vidéo : dessiner lastText en (penX, penY) sur l'écran OS
    }
}
