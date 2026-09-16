package ab3d2;

/**
 * Traduction de ab3d2_source/serial_nightmare.s — lien 2 joueurs (série Paula et
 * parallèle).
 *
 * L'I/O matérielle (registres Paula serdat/serdatr/intreq via a6=custom ; bit-bang
 * CIA $bfd000 ; port parallèle $bfe101/$bfe301) relève de la couche hôte et reste en
 * stubs documentés (à implémenter avec le host dans une prochaine session).
 *
 * La LOGIQUE DE PROTOCOLE est traduite fidèlement : SENDFIRST/RECFIRST échangent un
 * long word entrelacé octet par octet (envoi d'un octet de d0 ⇄ réception d'un octet
 * assemblé dans d2 par ror.l #8), construits sur SERSEND/SERREC. Conventions 68k :
 * ror.l #8 = Integer.rotateRight(x,8) ; lsr.w #8 = décalage du seul mot bas ;
 * swap = échange des deux demi-mots ; move.b = mise à jour de l'octet bas uniquement.
 */
public final class SerialNightmare {

    private SerialNightmare() {
    }

    // ------------------------------------------------------------------
    // Primitives série Paula — couche hôte (serdat/serdatr/intreq). Stubs.
    // ------------------------------------------------------------------

    /**
     * SERSEND : envoie l'octet bas de d1 sur le lien. Le transport série Paula (serdat + bit
     * stop) est remplacé par une socket TCP ({@link ab3d2.host.SerialLink}) — cf. plan 2 joueurs.
     */
    public static void SERSEND(int d1) {
        ab3d2.host.SerialLink.sendByte(d1);
    }

    /** SERREC : attend (bloquant) et renvoie un octet du pair (octet bas), via {@link ab3d2.host.SerialLink}. */
    public static int SERREC() {
        return ab3d2.host.SerialLink.recvByte();
    }

    // ------------------------------------------------------------------
    // Protocole d'échange long-word entrelacé (LOGIQUE, traduit).
    // ------------------------------------------------------------------

    /** SENDFIRST : échange un long word d0⇄d0, en envoyant d'abord. */
    public static int SENDFIRST(int d0) {
        int d1;
        int d2 = 0;

        d1 = d0 & 0xFF;                                     // move.b d0,d1
        SERSEND(d1);                                        // bsr SERSEND
        d1 = SERREC();                                      // bsr SERREC
        d2 = (d2 & ~0xFF) | (d1 & 0xFF);                    // move.b d1,d2
        d2 = Integer.rotateRight(d2, 8);                    // ror.l #8,d2

        d0 = (d0 & 0xFFFF0000) | ((d0 & 0xFFFF) >>> 8);     // lsr.w #8,d0
        d1 = (d1 & ~0xFF) | (d0 & 0xFF);                    // move.b d0,d1
        SERSEND(d1);
        d1 = SERREC();
        d2 = (d2 & ~0xFF) | (d1 & 0xFF);                    // move.b d1,d2
        d2 = Integer.rotateRight(d2, 8);                    // ror.l #8,d2

        d0 = (d0 >>> 16) | (d0 << 16);                      // swap d0
        d1 = (d1 & ~0xFF) | (d0 & 0xFF);                    // move.b d0,d1
        SERSEND(d1);
        d1 = SERREC();
        d2 = (d2 & ~0xFF) | (d1 & 0xFF);
        d2 = Integer.rotateRight(d2, 8);

        d0 = (d0 & 0xFFFF0000) | ((d0 & 0xFFFF) >>> 8);     // lsr.w #8,d0
        d1 = (d1 & ~0xFF) | (d0 & 0xFF);
        SERSEND(d1);
        d1 = SERREC();
        d2 = (d2 & ~0xFF) | (d1 & 0xFF);
        d2 = Integer.rotateRight(d2, 8);

        d0 = d2;                                            // move.l d2,d0
        return d0;
    }

    /** RECFIRST : échange un long word d0⇄d0, en recevant d'abord. */
    public static int RECFIRST(int d0) {
        int d1;
        int d2 = 0;

        d1 = SERREC();                                      // bsr SERREC
        d2 = (d2 & ~0xFF) | (d1 & 0xFF);                    // move.b d1,d2
        d1 = (d1 & ~0xFF) | (d0 & 0xFF);                    // move.b d0,d1
        SERSEND(d1);                                        // bsr SERSEND
        d2 = Integer.rotateRight(d2, 8);                    // ror.l #8,d2

        d1 = SERREC();
        d2 = (d2 & ~0xFF) | (d1 & 0xFF);                    // move.b d1,d2
        d0 = (d0 & 0xFFFF0000) | ((d0 & 0xFFFF) >>> 8);     // lsr.w #8,d0
        d1 = (d1 & ~0xFF) | (d0 & 0xFF);                    // move.b d0,d1
        SERSEND(d1);
        d2 = Integer.rotateRight(d2, 8);

        d1 = SERREC();
        d2 = (d2 & ~0xFF) | (d1 & 0xFF);
        d0 = (d0 >>> 16) | (d0 << 16);                      // swap d0
        d1 = (d1 & ~0xFF) | (d0 & 0xFF);
        SERSEND(d1);
        d2 = Integer.rotateRight(d2, 8);

        d1 = SERREC();
        d2 = (d2 & ~0xFF) | (d1 & 0xFF);
        d0 = (d0 & 0xFFFF0000) | ((d0 & 0xFFFF) >>> 8);     // lsr.w #8,d0
        d1 = (d1 & ~0xFF) | (d0 & 0xFF);
        SERSEND(d1);
        d2 = Integer.rotateRight(d2, 8);

        d0 = d2;                                            // move.l d2,d0
        return d0;
    }

    // ------------------------------------------------------------------
    // Transports alternatifs — bit-bang série CIA et port parallèle. Tout est
    // du timing matériel pur (macros PAUSE/WT, $bfd000/$bfe101/$bfe301) → host.
    // ------------------------------------------------------------------

    /** INITSEND/SENDLONG/SENDLAST : émission bit-bang via CIA $bfd000. Host. */
    public static void INITSEND() {
        throw new UnsupportedOperationException("serial_nightmare.s::INITSEND (bit-bang CIA hôte)");
    }

    public static void SENDLONG(int d0) {
        throw new UnsupportedOperationException("serial_nightmare.s::SENDLONG (bit-bang CIA hôte)");
    }

    public static void SENDLAST(int d0) {
        throw new UnsupportedOperationException("serial_nightmare.s::SENDLAST (bit-bang CIA hôte)");
    }

    /** INITREC/RECEIVE : réception bit-bang via CIA $bfd000. Host. */
    public static void INITREC() {
        throw new UnsupportedOperationException("serial_nightmare.s::INITREC (bit-bang CIA hôte)");
    }

    public static void RECEIVE() {
        throw new UnsupportedOperationException("serial_nightmare.s::RECEIVE (bit-bang CIA hôte)");
    }

    /** InitParSlave/Master + ParSendFirst/ParRecFirst : port parallèle $bfe101/$bfd000. Host. */
    public static void InitParSlave() {
        throw new UnsupportedOperationException("serial_nightmare.s::InitParSlave (port parallèle hôte)");
    }

    public static void InitParMaster() {
        throw new UnsupportedOperationException("serial_nightmare.s::InitParMaster (port parallèle hôte)");
    }

    public static int ParSendFirst(int d0) {
        throw new UnsupportedOperationException("serial_nightmare.s::ParSendFirst (port parallèle hôte)");
    }

    public static int ParRecFirst(int d0) {
        throw new UnsupportedOperationException("serial_nightmare.s::ParRecFirst (port parallèle hôte)");
    }
}
