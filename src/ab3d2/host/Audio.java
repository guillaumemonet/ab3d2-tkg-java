package ab3d2.host;

import ab3d2.Mem;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * Couche hôte : backend audio (OpenAL via LWJGL) qui « joue Paula ».
 *
 * Le jeu fait du mixage logiciel (Hires.newsampbitl) : il combine 8 voies en 4 buffers
 * de sortie écrits dans les registres canaux Paula (CustomChips.audLC/VOL/PER). Ici on
 * lit ces 4 buffers, on applique le volume canal Paula, on somme en stéréo
 * (gauche = ch0+ch3, droite = ch1+ch2 — affectation Paula) et on diffuse en streaming.
 *
 * Pilotage MONO-THREAD : JUSTSOUNDS (boucle de frame) appelle ensureStarted/needsBuffer/
 * queueFromPaula. Pas de thread audio → aucun problème de concurrence avec l'état jeu.
 * Si aucun périphérique audio (ex. CI headless), ensureStarted renvoie false → silencieux.
 */
public final class Audio {

    private static final int SAMPLES_PER_BUF = 200;   // newsampbitl produit 200 octets/canal
    private static final int NUM_BUFFERS = 8;         // pool de buffers OpenAL
    private static final int TARGET_QUEUED = 4;       // profondeur de file visée

    /** Désactive complètement l'audio (ex. moteur jME : OpenAL piloté ailleurs). */
    public static boolean disabled = false;

    private static boolean started;
    private static boolean running;                   // false si pas de périphérique
    private static long device;
    private static long context;
    private static int source;
    private static final int[] freePool = new int[NUM_BUFFERS];
    private static int freeCount;
    private static ShortBuffer pcm;                   // trame stéréo 16 bits réutilisable

    private Audio() {
    }

    /** Initialise OpenAL une fois. Renvoie true si l'audio est opérationnel. */
    public static boolean ensureStarted() {
        if (disabled) {
            return false;
        }
        if (started) {
            return running;
        }
        started = true;
        try {
            device = alcOpenDevice((java.nio.ByteBuffer) null);
            if (device == MemoryUtil.NULL) {
                return false;
            }
            ALCCapabilities caps = ALC.createCapabilities(device);
            context = alcCreateContext(device, (IntBuffer) null);
            if (context == MemoryUtil.NULL || !alcMakeContextCurrent(context)) {
                return false;
            }
            AL.createCapabilities(caps);
            source = alGenSources();
            for (int i = 0; i < NUM_BUFFERS; i++) {
                freePool[i] = alGenBuffers();
            }
            freeCount = NUM_BUFFERS;
            pcm = MemoryUtil.memAllocShort(SAMPLES_PER_BUF * 2);
            running = true;
        } catch (Throwable t) {
            System.err.println("[Audio] OpenAL indisponible (" + t + ") — son désactivé");
            running = false;
        }
        return running;
    }

    /** Recycle les buffers déjà joués ; vrai s'il faut produire une trame de plus. */
    public static boolean needsBuffer() {
        if (!running) {
            return false;
        }
        int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int b = alSourceUnqueueBuffers(source);
            if (freeCount < NUM_BUFFERS) {
                freePool[freeCount++] = b;
            }
        }
        int queued = alGetSourcei(source, AL_BUFFERS_QUEUED);
        return queued < TARGET_QUEUED && freeCount > 0;
    }

    // Émulation DMA Paula par canal : au démarrage DMA, Paula latche AUDxLC/LEN (segment joué
    // une fois) puis recharge le point de boucle. On reproduit ça : segLC/segLEN = segment
    // courant, segPos = position de lecture (octets) avançant au débit du canal (refPer/per).
    private static final int[] segLC = new int[4];
    private static final int[] segLEN = new int[4];
    private static final double[] segPos = new double[4];
    private static final boolean[] segActive = new boolean[4];

    /** Consomme les déclenchements DMA (un par note) : (re)latche le segment à jouer. */
    private static void consumeTriggers() {
        for (int ch = 0; ch < 4; ch++) {
            if (CustomChips.dmaTrig[ch]) {
                segLC[ch] = CustomChips.dmaLC[ch];
                segLEN[ch] = CustomChips.dmaLEN[ch];
                segPos[ch] = 0;
                segActive[ch] = true;
                CustomChips.dmaTrig[ch] = false;
            }
            if ((CustomChips.dmacon & (1 << ch)) == 0) segActive[ch] = false; // DMA du canal coupé
        }
    }

    /**
     * Lit les 4 canaux Paula (CustomChips), mixe en stéréo 16 bits et empile dans OpenAL.
     *
     * Les canaux ont des PÉRIODES INDÉPENDANTES : en jeu, ch0 = musique (période propre),
     * ch1-3 = SFX (période fixe, buffers de 200 octets rafraîchis par newsampbitl à chaque
     * appel). Le taux de sortie = celui des SFX (1:1 pour ch1-3) ; tout canal de période
     * différente (la musique) est ré-échantillonné via une phase persistante, en bouclant
     * sur sa longueur d'échantillon (AUDxLEN mots).
     */
    public static void queueFromPaula() {
        if (!running || freeCount == 0) {
            return;
        }
        int refPer = CustomChips.audPER[3] > 0 ? CustomChips.audPER[3]
                   : (CustomChips.audPER[1] > 0 ? CustomChips.audPER[1] : 443);

        consumeTriggers();
        pcm.clear();
        for (int i = 0; i < SAMPLES_PER_BUF; i++) {
            int l = smp(0, refPer) + smp(3, refPer);      // gauche = Paula ch0 + ch3
            int r = smp(1, refPer) + smp(2, refPer);      // droite = Paula ch1 + ch2
            pcm.put((short) clamp16(l << 6));
            pcm.put((short) clamp16(r << 6));
        }
        pcm.flip();

        int rate = 3546895 / refPer;                      // fréquence de sortie = horloge PAL / période SFX
        // GARDE-FOU : une période Paula transitoire minuscule (1-2) ferait exploser le rate
        // (jusqu'à ~3,5 MHz) → OpenAL alloue un buffer interne démesuré → STACK_BUFFER_OVERRUN natif
        // (crash 0xC0000409 observé après un temps de jeu). On borne à une plage audio saine.
        if (rate < 1000) rate = 1000;
        else if (rate > 48000) rate = 48000;

        int buf = freePool[--freeCount];
        alBufferData(buf, AL_FORMAT_STEREO16, pcm, rate);
        alSourceQueueBuffers(source, buf);

        if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) {
            alSourcePlay(source);
        }
        // Vidange défensive : ne jamais laisser une erreur OpenAL s'accumuler entre trames
        // (un état natif corrompu qui persiste finit en crash). alGetError() efface le drapeau.
        alGetError();
    }

    /**
     * Échantillon 8 bits signé du canal Paula `ch` pour la i-ème trame de sortie, mis à
     * l'échelle du volume (0..64). Période == refPer → lecture directe (1:1, SFX) ;
     * sinon ré-échantillonnage par phase (musique) bouclé sur AUDxLEN*2 octets.
     */
    private static int smp(int ch, int refPer) {
        int vol = CustomChips.audVOL[ch], per = CustomChips.audPER[ch];
        if (!segActive[ch] || vol == 0 || per <= 0 || segLC[ch] <= 0) {
            return 0;
        }
        int segBytes = segLEN[ch] * 2;
        int p = (int) segPos[ch];
        if (segBytes < 2 || p >= segBytes) {              // segment fini → recharge le point de boucle
            segLC[ch] = CustomChips.audLC[ch];
            segLEN[ch] = CustomChips.audLEN[ch];
            segBytes = segLEN[ch] * 2;
            segPos[ch] = 0;
            p = 0;
            if (segBytes < 2 || segLC[ch] <= 0) {         // nullsample (LEN≤1) → silence
                return 0;
            }
        }
        int s = (byte) Mem.ub(segLC[ch] + p);
        segPos[ch] += (double) refPer / per;              // avance au débit du canal (pitch)
        return s * vol / 64;
    }

    private static int clamp16(int v) {
        if (v > 32767) return 32767;
        if (v < -32768) return -32768;
        return v;
    }

    // ---- DIAG : capture WAV du mix (indépendante d'OpenAL, pour analyse headless) ----
    public static boolean dbgCapture = false;
    private static java.io.ByteArrayOutputStream capBuf;
    private static int capRate = 8006;

    /** Mixe une trame (comme queueFromPaula) mais l'accumule en PCM stéréo 16 bits LE. */
    public static void captureFrame() {
        if (capBuf == null) capBuf = new java.io.ByteArrayOutputStream();
        int refPer = CustomChips.audPER[3] > 0 ? CustomChips.audPER[3]
                   : (CustomChips.audPER[1] > 0 ? CustomChips.audPER[1] : 443);
        capRate = 3546895 / refPer;
        consumeTriggers();
        for (int i = 0; i < SAMPLES_PER_BUF; i++) {
            int l = clamp16((smp(0, refPer) + smp(3, refPer)) << 6);
            int r = clamp16((smp(1, refPer) + smp(2, refPer)) << 6);
            capBuf.write(l & 0xFF); capBuf.write((l >> 8) & 0xFF);
            capBuf.write(r & 0xFF); capBuf.write((r >> 8) & 0xFF);
        }
    }

    /** Écrit le PCM capturé en fichier WAV (stéréo 16 bits). */
    public static void dumpWav(String path) {
        if (capBuf == null) return;
        byte[] d = capBuf.toByteArray();
        try (java.io.OutputStream os = new java.io.FileOutputStream(path)) {
            int byteRate = capRate * 2 * 2, dataLen = d.length;
            java.io.ByteArrayOutputStream h = new java.io.ByteArrayOutputStream();
            writeStr(h, "RIFF"); writeLE32(h, 36 + dataLen); writeStr(h, "WAVE");
            writeStr(h, "fmt "); writeLE32(h, 16); writeLE16(h, 1); writeLE16(h, 2);
            writeLE32(h, capRate); writeLE32(h, byteRate); writeLE16(h, 4); writeLE16(h, 16);
            writeStr(h, "data"); writeLE32(h, dataLen);
            os.write(h.toByteArray()); os.write(d);
            System.out.println("[Audio] WAV écrit : " + path + " (" + dataLen + " octets PCM, " + capRate + " Hz, "
                + (dataLen / 4) + " trames)");
        } catch (Exception e) {
            System.out.println("[Audio] échec écriture WAV : " + e);
        }
    }

    private static void writeStr(java.io.OutputStream o, String s) throws java.io.IOException { for (char c : s.toCharArray()) o.write(c); }
    private static void writeLE16(java.io.OutputStream o, int v) throws java.io.IOException { o.write(v & 0xFF); o.write((v >> 8) & 0xFF); }
    private static void writeLE32(java.io.OutputStream o, int v) throws java.io.IOException { o.write(v & 0xFF); o.write((v >> 8) & 0xFF); o.write((v >> 16) & 0xFF); o.write((v >> 24) & 0xFF); }

    /** Arrêt (closeeverything). Idempotent. */
    public static void stop() {
        if (!running) {
            return;
        }
        try {
            alSourceStop(source);
            alDeleteSources(source);
            for (int i = 0; i < freeCount; i++) {
                alDeleteBuffers(freePool[i]);
            }
            alcMakeContextCurrent(MemoryUtil.NULL);
            if (context != MemoryUtil.NULL) {
                alcDestroyContext(context);
            }
            if (device != MemoryUtil.NULL) {
                alcCloseDevice(device);
            }
        } catch (Throwable t) {
            // ignore
        } finally {
            if (pcm != null) {
                MemoryUtil.memFree(pcm);
                pcm = null;
            }
            running = false;
            started = false;
        }
    }
}
