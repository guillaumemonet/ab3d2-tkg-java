package ab3d2.tools;

import ab3d2.SerialNightmare;
import ab3d2.host.SerialLink;

import java.util.ArrayList;
import java.util.List;

/**
 * Test « client/serveur local » du lien 2 joueurs (TCP).
 *
 * Vérifie que le protocole d'origine (serial_nightmare.s : SENDFIRST côté master, RECFIRST
 * côté slave) échange correctement des longs word en LOCK-STEP par-dessus une socket TCP
 * ({@link SerialLink}), sur localhost — la brique « client/serveur en local ».
 *
 * <p>Un process = un endpoint (fidèle au global ASM). Sans argument, le process se comporte
 * en MASTER et FORKE un second JVM en SLAVE, puis les deux s'échangent une séquence de longs
 * et vérifient réciproquement les valeurs reçues.
 *
 * <pre>
 *   gradle -p java netTest          # lance master + slave (fork) sur localhost
 *   java ... SerialLinkTest slave 7989   # (interne : endpoint slave)
 * </pre>
 */
public final class SerialLinkTest {

    private static final int PORT = 7989;
    private static final int TIMEOUT_MS = 15_000;

    /** Valeurs testées (couvre 0, négatifs, bit de signe des octets, motifs). */
    private static final int[] MASTER_SEQ = buildSeq(0x11223344);
    private static final int[] SLAVE_SEQ  = buildSeq(0x55667788);

    private static int[] buildSeq(int base) {
        int[] fixed = {0x00000000, 0xFFFFFFFF, 0x80000000, 0x7FFFFFFF, 0x0000FF00, 0xDEADBEEF};
        int[] seq = new int[fixed.length + 8];
        System.arraycopy(fixed, 0, seq, 0, fixed.length);
        for (int i = 0; i < 8; i++) {
            seq[fixed.length + i] = base + i * 0x01010101;
        }
        return seq;
    }

    private SerialLinkTest() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && "slave".equals(args[0])) {
            int port = args.length >= 2 ? Integer.parseInt(args[1]) : PORT;
            System.exit(runSlave(port));
        } else {
            System.exit(runMasterAndForkSlave());
        }
    }

    /** MASTER : forke un slave, accepte, échange la séquence et vérifie. */
    private static int runMasterAndForkSlave() throws Exception {
        System.out.println("[netTest] MASTER : fork du slave puis écoute sur " + PORT + " …");
        Process slave = forkSlave(PORT);
        int masterRc;
        try {
            SerialLink.startMaster(PORT, TIMEOUT_MS);
            System.out.println("[netTest] MASTER : slave connecté (" + SerialLink.isConnected() + ")");
            masterRc = exchangeAndVerify("MASTER", MASTER_SEQ, SLAVE_SEQ, true);
        } finally {
            SerialLink.close();
        }
        boolean slaveOk = slave.waitFor() == 0;
        boolean ok = masterRc == 0 && slaveOk;
        System.out.println("[netTest] ===== " + (ok ? "PASS" : "FAIL")
                + " (master rc=" + masterRc + ", slave rc=" + slave.exitValue() + ") =====");
        return ok ? 0 : 1;
    }

    /** SLAVE : se connecte au master, échange la séquence et vérifie. */
    private static int runSlave(int port) {
        System.out.println("[netTest] SLAVE : connexion à localhost:" + port + " …");
        try {
            SerialLink.startSlave("localhost", port, TIMEOUT_MS);
            System.out.println("[netTest] SLAVE : connecté au master");
            return exchangeAndVerify("SLAVE", SLAVE_SEQ, MASTER_SEQ, false);
        } catch (Exception e) {
            System.out.println("[netTest] SLAVE : erreur " + e);
            return 1;
        } finally {
            SerialLink.close();
        }
    }

    /**
     * Échange la séquence en lock-step : master fait SENDFIRST, slave fait RECFIRST. Chaque
     * appel envoie {@code mine[i]} et renvoie ce que le pair a envoyé, qui doit valoir {@code peer[i]}.
     */
    private static int exchangeAndVerify(String role, int[] mine, int[] peer, boolean master) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < mine.length; i++) {
            int got = master ? SerialNightmare.SENDFIRST(mine[i]) : SerialNightmare.RECFIRST(mine[i]);
            if (got != peer[i]) {
                errors.add(String.format("  [%d] envoyé=0x%08X reçu=0x%08X attendu=0x%08X",
                        i, mine[i], got, peer[i]));
            }
        }
        if (errors.isEmpty()) {
            System.out.println("[netTest] " + role + " : " + mine.length + " longs échangés OK");
            return 0;
        }
        System.out.println("[netTest] " + role + " : " + errors.size() + " ERREUR(S) :");
        errors.forEach(System.out::println);
        return 1;
    }

    /** Lance un second JVM (même classpath) en mode slave. */
    private static Process forkSlave(int port) throws Exception {
        String javaBin = System.getProperty("java.home") + java.io.File.separator + "bin"
                + java.io.File.separator + "java";
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(SerialLinkTest.class.getName());
        cmd.add("slave");
        cmd.add(Integer.toString(port));
        return new ProcessBuilder(cmd).inheritIO().start();
    }
}
