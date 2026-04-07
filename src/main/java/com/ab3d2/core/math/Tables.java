package com.ab3d2.core.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;

/**
 * Tables mathématiques d'AB3D2 — sinus/cosinus et constantes de projection.
 *
 * <h2>SinCosTable</h2>
 * Fichier source : {@code bigsine} (binaire big-endian, chargé via {@code incbin} dans tables_data.s).
 * <pre>
 * SinCosTable_vw[8192]  — 8192 valeurs WORD signées (16 bits, big-endian)
 *                         = 16384 bytes au total
 * Représente deux cycles complets (720°) de sinus.
 * Un quart de cycle = 2048 entrées = 90°.
 * </pre>
 *
 * <h2>Accesseurs</h2>
 * <pre>
 * sinw(angle) = SinCosTable[(angle & 8190) >> 1]
 * cosw(angle) = SinCosTable[((angle + 2048) & 8190) >> 1]
 * </pre>
 * Reproduit exactement les macros {@code sinw()}/{@code cosw()} de math25d.h.
 *
 * <h2>ConstantTable</h2>
 * Calculée au démarrage dans {@code _startup} d'hires.s.
 * 8192 paires de LONG (e, d) pour les calculs de projection des objets.
 * <pre>
 * Pour k = 1 … 8192 :
 *   c = (16384 * 64) / k
 *   e = (64 * 64 * 65536) / c
 *   d = e * (c/2 - 40*64) >> 6
 * Table[k-1] = { e, d }  → int[16384]
 * </pre>
 */
public class Tables {

    private static final Logger log = LoggerFactory.getLogger(Tables.class);

    // ── Constantes ────────────────────────────────────────────────────────────

    public static final int SINTAB_SIZE   = 8192;    // nombre d'entrées
    public static final int SINTAB_BYTES  = SINTAB_SIZE * 2; // 16384 bytes
    public static final int CONST_SIZE    = 8192;    // nombre de paires
    public static final int SINE_QUARTER  = SINTAB_SIZE / 4; // = 2048

    // ── Tables ───────────────────────────────────────────────────────────────

    private static short[] sinCosTable;
    private static int[]   constantTable; // [e0, d0, e1, d1, … ] = 16384 ints

    private static boolean initialized = false;

    // ── Initialisation ───────────────────────────────────────────────────────

    /**
     * Initialise les tables depuis un fichier {@code bigsine} sur le disque.
     *
     * @param bigsinePath chemin vers le fichier bigsine (16384 bytes big-endian)
     */
    public static void init(Path bigsinePath) throws IOException {
        log.info("Loading SinCosTable from {}", bigsinePath);
        byte[] data = Files.readAllBytes(bigsinePath);
        initFromBytes(data);
    }

    /**
     * Initialise depuis une ressource classpath (pour les tests et le runtime).
     * Le fichier doit être dans le classpath sous le nom {@code /bigsine}.
     */
    public static void initFromClasspath() throws IOException {
        try (InputStream is = Tables.class.getResourceAsStream("/bigsine")) {
            if (is == null)
                throw new IOException("Ressource /bigsine introuvable dans le classpath");
            byte[] data = is.readAllBytes();
            initFromBytes(data);
        }
    }

    /**
     * Initialise les tables depuis des bytes bruts big-endian.
     * Fallback : si les données sont trop courtes, génère une table synthétique.
     */
    public static void initFromBytes(byte[] data) {
        sinCosTable = new short[SINTAB_SIZE];

        if (data.length >= SINTAB_BYTES) {
            ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
            for (int i = 0; i < SINTAB_SIZE; i++) {
                sinCosTable[i] = buf.getShort();
            }
            log.info("SinCosTable chargée depuis bytes ({} bytes lus)", SINTAB_BYTES);
        } else {
            log.warn("bigsine trop petit ({} bytes), génération synthétique", data.length);
            buildSyntheticSinTable();
        }

        buildConstantTable();
        initialized = true;
        log.info("Tables initialisées (sin={} entrées, const={} paires)",
            SINTAB_SIZE, CONST_SIZE);
    }

    /**
     * Génère une table sinus synthétique par calcul Java.
     * Précision légèrement inférieure à la table originale mais fonctionnelle.
     */
    private static void buildSyntheticSinTable() {
        // La table couvre 2 cycles complets (0..4π) sur 8192 entrées
        // Valeurs entre -32767 et +32767 (WORD signé 16 bits, << 15)
        for (int i = 0; i < SINTAB_SIZE; i++) {
            double angle = 2.0 * Math.PI * 2.0 * i / SINTAB_SIZE;
            sinCosTable[i] = (short) Math.round(Math.sin(angle) * 32767.0);
        }
        log.debug("Table sinus synthétique générée ({} entrées)", SINTAB_SIZE);
    }

    /**
     * Calcule la ConstantTable exactement comme dans {@code _startup} (hires.s).
     * <pre>
     * for k = 1 to 8192:
     *   c = (16384*64) / k            ; c = cos_scale
     *   e = (64*64*65536) / c         ; e = ConstantTable[k-1][0]
     *   d = e * (c/2 - 40*64) >> 6   ; d = ConstantTable[k-1][1]
     * </pre>
     */
    private static void buildConstantTable() {
        constantTable = new int[CONST_SIZE * 2];
        for (int k = 1; k <= CONST_SIZE; k++) {
            long c = (16384L * 64L) / k;                // c = cos_scale
            long e = (64L * 64L * 65536L) / c;          // e
            long d = c / 2 - 40L * 64L;                 // d_before_mul
            d = (e * d) >> 6;                            // d_final
            constantTable[(k - 1) * 2]     = (int) e;
            constantTable[(k - 1) * 2 + 1] = (int) d;
        }
        log.debug("ConstantTable calculée ({} paires)", CONST_SIZE);
    }

    // ── Accesseurs sinus/cosinus ─────────────────────────────────────────────

    /**
     * Sinus d'un angle.
     * Reproduit exactement {@code sinw(angle)} de math25d.h.
     *
     * @param angle angle en unités AB3D2 (0..8191 = un cycle complet)
     * @return sinus ≈ sin(angle * 2π / 4096), signé 16 bits
     */
    public static short sinw(int angle) {
        checkInit();
        return sinCosTable[(angle & (SINTAB_SIZE - 2)) >> 1];
    }

    /**
     * Cosinus d'un angle.
     * Reproduit exactement {@code cosw(angle)} de math25d.h.
     *
     * @param angle angle en unités AB3D2
     * @return cosinus, signé 16 bits
     */
    public static short cosw(int angle) {
        checkInit();
        return sinCosTable[((angle + SINE_QUARTER) & (SINTAB_SIZE - 2)) >> 1];
    }

    /**
     * Sinus en float (0..1 range), pour les calculs Java modernes.
     */
    public static float sinf(int angle) {
        return sinw(angle) / 32767.0f;
    }

    /**
     * Cosinus en float.
     */
    public static float cosf(int angle) {
        return cosw(angle) / 32767.0f;
    }

    // ── Accesseurs ConstantTable ─────────────────────────────────────────────

    /**
     * Valeur e de la ConstantTable pour l'index k (1..8192).
     */
    public static int constE(int k) {
        checkInit();
        if (k < 1 || k > CONST_SIZE) return 0;
        return constantTable[(k - 1) * 2];
    }

    /**
     * Valeur d de la ConstantTable pour l'index k (1..8192).
     */
    public static int constD(int k) {
        checkInit();
        if (k < 1 || k > CONST_SIZE) return 0;
        return constantTable[(k - 1) * 2 + 1];
    }

    // ── Diagnostic ───────────────────────────────────────────────────────────

    public static boolean isInitialized() { return initialized; }

    public static short[] getSinCosTable()  { return sinCosTable; }
    public static int[]   getConstantTable(){ return constantTable; }

    /**
     * Dump de validation : compare sin(0°)=0, sin(90°)≈32767, sin(180°)=0, sin(270°)≈-32767.
     */
    public static String dumpValidation() {
        if (!initialized) return "[Tables non initialisées]";
        return String.format(
            "Validation SinCos :%n" +
            "  sin(  0°) = %6d  (attendu ≈     0)%n" +
            "  sin( 90°) = %6d  (attendu ≈ 32767)%n" +
            "  sin(180°) = %6d  (attendu ≈     0)%n" +
            "  sin(270°) = %6d  (attendu ≈-32767)%n" +
            "  cos(  0°) = %6d  (attendu ≈ 32767)%n" +
            "  cos( 90°) = %6d  (attendu ≈     0)%n",
            sinw(0), sinw(1024), sinw(2048), sinw(3072),
            cosw(0), cosw(1024));
    }

    private static void checkInit() {
        if (!initialized)
            throw new IllegalStateException(
                "Tables.init() ou Tables.initFromClasspath() doit être appelé d'abord.");
    }
}
