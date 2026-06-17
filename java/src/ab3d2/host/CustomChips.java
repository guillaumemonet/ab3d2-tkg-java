package ab3d2.host;

/**
 * Couche hôte : registres custom Amiga (Paula audio + DMACON) et CIA-A PRA,
 * tels qu'utilisés par le replay ProTracker (music.s) et l'audio du jeu.
 *
 * Les écritures 68k vers $dffxxx / $bfe001 sont décodées ici et stockées ;
 * un backend audio Java les consommera (AUDxLC/LEN pointent dans Mem.RAM,
 * samples 8 bits signés, période Paula → fréquence = 3546895/période PAL).
 */
public final class CustomChips {

    // État Paula par canal (0..3 = $dff0a0/b0/c0/d0)
    public static final int[] audLC = new int[4];   // AUDxLC  : adresse sample (dans Mem)
    public static final int[] audLEN = new int[4];  // AUDxLEN : longueur en mots
    public static final int[] audPER = new int[4];  // AUDxPER : période
    public static final int[] audVOL = new int[4];  // AUDxVOL : volume 0..64

    /** Bits DMA actifs (DMACON $dff096, sémantique set/clear par bit 15). */
    public static int dmacon;

    // Latch Paula : à l'activation DMA d'un canal, Paula latche AUDxLC/LEN (segment à jouer
    // une fois), puis recharge le point de boucle quand le segment est fini. On capture
    // ce latch pour que le backend audio joue le sample COMPLET au lieu du point de boucle.
    public static final int[] dmaLC = new int[4];     // LC latché au démarrage DMA
    public static final int[] dmaLEN = new int[4];    // LEN latché (mots)
    public static final boolean[] dmaTrig = new boolean[4]; // canal (re)déclenché ; consommé par Audio

    /** CIA-A PRA ($bfe001) — bit 1 = LED/filtre audio. */
    public static int ciaaPra;

    /**
     * JOY0DAT ($dff00a) — compteur souris/joystick port 0 (UWORD).
     * Octet haut = compteur vertical, octet bas = compteur horizontal ;
     * injecté par la couche d'entrée hôte. Lu par Sys_ReadMouse.
     */
    public static int joy0dat;

    /**
     * JOY1DAT ($dff00c) — compteur souris/joystick port 1 (UWORD).
     * Octet haut @$dff00c, octet bas @$dff00d. Injecté par la couche d'entrée hôte.
     * Lu par cd32joy.s::_ReadJoy*.Joystick.
     */
    public static int joy1dat;

    // État des boutons souris injecté par la couche d'entrée hôte (actifs bas
    // sur le matériel : bouton gauche = bit 6 de CIA-A PRA, bouton droit =
    // bit 10 de POTGOR, soit le bit 2 de l'octet à $dff016).
    public static boolean mouseLeftPressed;
    public static boolean mouseRightPressed;

    /** btst #2,$dff000+potinp — vrai si le bit est positionné (bouton droit RELÂCHÉ). */
    public static boolean potinpBit2() {
        return !mouseRightPressed;
    }

    /** btst #CIAB_GAMEPORT0(6),$bfe001 — vrai si le bit est positionné (bouton gauche RELÂCHÉ). */
    public static boolean ciaaPraBit6() {
        return !mouseLeftPressed;
    }

    /** Tir port 1 (CIAB_GAMEPORT1, bit 7 de CIA-A PRA, actif bas) injecté par l'hôte. */
    public static boolean fire1Pressed;

    /** btst #7,$bfe001 — vrai si le bit est positionné (tir port 1 RELÂCHÉ). */
    public static boolean ciaaPraBit7() {
        return !fire1Pressed;
    }

    private CustomChips() {
    }

    /** Écriture word vers l'espace custom ($dffxxx). */
    public static void write16(int addr, int value) {
        value &= 0xFFFF;
        if (addr == 0xdff096) { // DMACON
            if ((value & 0x8000) != 0) {
                int newBits = value & 0x7FFF;
                for (int ch = 0; ch < 4; ch++) {       // DMAF_AUDx = bit ch (0..3)
                    int bit = 1 << ch;
                    // Paula ne latche LC/LEN qu'à la TRANSITION OFF→ON du canal
                    // (réactiver un canal déjà actif ne relatche pas).
                    if ((newBits & bit) != 0 && (dmacon & bit) == 0) {
                        dmaLC[ch] = audLC[ch];
                        dmaLEN[ch] = audLEN[ch];
                        dmaTrig[ch] = true;
                    }
                }
                dmacon |= newBits;
            } else {
                dmacon &= ~value;
            }
            return;
        }
        int off = addr - 0xdff0a0;
        if (off >= 0 && off < 0x40) {
            int ch = off >> 4;
            switch (off & 0xF) {
                case 0x0 -> audLC[ch] = (audLC[ch] & 0xFFFF) | (value << 16);
                case 0x2 -> audLC[ch] = (audLC[ch] & 0xFFFF0000) | value;
                case 0x4 -> audLEN[ch] = value;
                case 0x6 -> audPER[ch] = value;
                case 0x8 -> audVOL[ch] = value;
                default -> { /* AUDxDAT et autres : ignorés */ }
            }
        }
        // autres registres custom : sans objet pour l'audio
    }

    /** Écriture long vers l'espace custom (ex. move.l ptr,AUDxLC). */
    public static void write32(int addr, int value) {
        write16(addr, value >>> 16);
        write16(addr + 2, value);
    }

    /** or.b #mask,$bfe001 */
    public static void ciaOr(int mask) {
        ciaaPra = (ciaaPra | mask) & 0xFF;
    }

    /** and.b #mask,$bfe001 */
    public static void ciaAnd(int mask) {
        ciaaPra = ciaaPra & mask & 0xFF;
    }

    /** bset.b #bit,$bfe001 */
    public static void ciaBset(int bit) {
        ciaaPra |= 1 << bit;
    }

    /** bchg.b #bit,$bfe001 */
    public static void ciaBchg(int bit) {
        ciaaPra ^= 1 << bit;
    }
}
