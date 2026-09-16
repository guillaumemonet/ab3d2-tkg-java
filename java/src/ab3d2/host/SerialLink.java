package ab3d2.host;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Couche hôte : lien 2 joueurs sur socket TCP (remplace le lien série/parallèle Amiga).
 *
 * Le protocole d'origine (serial_nightmare.s) échange les longs en LOCK-STEP octet par
 * octet ({@link ab3d2.SerialNightmare#SENDFIRST}/{@link ab3d2.SerialNightmare#RECFIRST}).
 * On garde CE protocole intact ; ici on ne remplace que le TRANSPORT matériel : SERSEND =
 * envoyer un octet, SERREC = recevoir un octet, sur une socket TCP fiable et ordonnée.
 *
 * <p>Rôles : <b>master = serveur</b> (écoute + accepte), <b>slave = client</b> (se connecte).
 * {@code TCP_NODELAY} activé (sinon Nagle bufferise le ping-pong → lag). Un octet est
 * {@code flush}é immédiatement pour que le pair débloque son SERREC — c'est un aller-retour
 * par octet, négligeable en localhost/LAN, injouable sur Internet (cf. plan 2 joueurs).
 *
 * <p>Singleton statique : un process = un lien (fidèle au global ASM). Pour tester deux
 * endpoints, lancer deux process (cf. {@code ab3d2.tools.SerialLinkTest}).
 */
public final class SerialLink {

    /** Port TCP par défaut du lien 2 joueurs. */
    public static final int DEFAULT_PORT = 7979;

    private static ServerSocket server;
    private static Socket socket;
    private static InputStream in;
    private static OutputStream out;

    private SerialLink() {
    }

    /** Erreur de lien (rupture de connexion, I/O) — désync fatale du lock-step. */
    public static final class LinkException extends RuntimeException {
        LinkException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /** true si le lien est établi et ouvert. */
    public static synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * MASTER : écoute sur {@code port} et bloque jusqu'à ce que le slave se connecte.
     * @param acceptTimeoutMs délai max d'attente d'un slave (0 = infini).
     */
    public static synchronized void startMaster(int port, int acceptTimeoutMs) throws IOException {
        close();
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(port));
        if (acceptTimeoutMs > 0) {
            server.setSoTimeout(acceptTimeoutMs);
        }
        socket = server.accept();
        setup();
    }

    /**
     * SLAVE : se connecte au master {@code host:port}. Réessaie tant que le master n'écoute
     * pas encore, jusqu'à {@code totalTimeoutMs}.
     */
    public static synchronized void startSlave(String host, int port, int totalTimeoutMs) throws IOException {
        close();
        long deadline = System.nanoTime() + totalTimeoutMs * 1_000_000L;
        IOException last = null;
        while (true) {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, port), 1000);
                socket = s;
                setup();
                return;
            } catch (IOException e) {
                last = e;
                if (totalTimeoutMs > 0 && System.nanoTime() >= deadline) {
                    throw new IOException("connexion au master " + host + ":" + port + " impossible", last);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("connexion interrompue", ie);
                }
            }
        }
    }

    private static void setup() throws IOException {
        socket.setTcpNoDelay(true);          // pas de Nagle : le ping-pong octet part tout de suite
        in = new BufferedInputStream(socket.getInputStream());
        out = socket.getOutputStream();
    }

    /** SERSEND : envoie un octet (octet bas de {@code b}) et le pousse immédiatement. */
    public static void sendByte(int b) {
        OutputStream o = out;
        if (o == null) {
            throw new LinkException("lien non établi (sendByte)", null);
        }
        try {
            o.write(b & 0xFF);
            o.flush();
        } catch (IOException e) {
            throw new LinkException("échec envoi", e);
        }
    }

    /** SERREC : attend (bloquant) et renvoie un octet du pair. */
    public static int recvByte() {
        InputStream i = in;
        if (i == null) {
            throw new LinkException("lien non établi (recvByte)", null);
        }
        try {
            int b = i.read();
            if (b < 0) {
                throw new EOFException("pair déconnecté");
            }
            return b & 0xFF;
        } catch (IOException e) {
            throw new LinkException("échec réception", e);
        }
    }

    /** Ferme le lien et libère la socket serveur. */
    public static synchronized void close() {
        for (AutoCloseable c : new AutoCloseable[]{in, out, socket, server}) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignore) {
                    // best effort
                }
            }
        }
        in = null;
        out = null;
        socket = null;
        server = null;
    }
}
