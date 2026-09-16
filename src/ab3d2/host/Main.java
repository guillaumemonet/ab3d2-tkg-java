package ab3d2.host;

import ab3d2.Assets;
import ab3d2.bss.Bss;
import ab3d2.c.MainC;
import ab3d2.data.DataSections;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Point d'entrée du programme porté.
 *
 * Initialise la disposition mémoire (sections BSS/DATA, dans l'ordre fixé par
 * hires.s) puis appelle MainC.run, équivalent du main() C (c/main.c).
 *
 * Tant que la boucle de frame (Phase 2) n'est pas branchée, MainC.run lèvera
 * sur le stub hires.s::_startup ; utiliser ab3d2.host.DisplayTest pour valider
 * l'afficheur en attendant.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path base = appImageBaseDir();   // racine du build jpackage, ou null en dev
        resolveDataDirs(base);           // AVANT DataSections.init() (qui lit les assets incbin)
        setupLogging(base);              // build packagé : redirige stdout/err vers run/ab3d2.log
        installCrashHandler();
        startFreezeWatchdog();
        Bss.init();
        DataSections.init();
        int rval = MainC.run(args);
        System.exit(rval);
    }

    /**
     * Localise les dossiers de données ({@code medias/original}) et d'écriture ({@code run}).
     * Ordre de priorité :
     *   1. surcharges explicites {@code -Dab3d2.dataDir=...} / {@code -Dab3d2.runDir=...} ;
     *   2. build redistribuable jpackage : le code tourne depuis {@code <App>/app/*.jar} →
     *      base = {@code <App>}, donc {@code <App>/medias/original} et {@code <App>/run} ;
     *   3. sinon (dev/Gradle) : valeurs par défaut relatives au répertoire courant (inchangé).
     */
    private static void resolveDataDirs(Path base) {
        String dataOverride = System.getProperty("ab3d2.dataDir");
        if (dataOverride != null) {
            Assets.root = Path.of(dataOverride);
        } else if (base != null) {
            Assets.root = base.resolve("medias").resolve("original");
        }

        String runOverride = System.getProperty("ab3d2.runDir");
        if (runOverride != null) {
            DosLib.runDir = Path.of(runOverride);
        } else if (base != null) {
            DosLib.runDir = base.resolve("run");
        }
    }

    /** Racine d'un build jpackage app-image (le jar est dans {@code <App>/app/}), ou null en dev. */
    private static Path appImageBaseDir() {
        try {
            Path code = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path parent = code.getParent();
            if (Files.isRegularFile(code) && parent != null
                    && "app".equals(String.valueOf(parent.getFileName()))) {
                return parent.getParent();      // <App>/app/foo.jar → <App>
            }
        } catch (Exception ignore) {
            // code source indéterminable → dev/CWD
        }
        return null;
    }

    /**
     * Build packagé (sans console) : redirige stdout/stderr vers {@code <run>/ab3d2.log} pour
     * disposer de logs. Surcharge par {@code -Dab3d2.log=chemin} ({@code "off"} = désactive).
     * En dev (base null, pas de surcharge) : on garde la console.
     */
    private static void setupLogging(Path base) {
        String logProp = System.getProperty("ab3d2.log");
        if ("off".equalsIgnoreCase(logProp)) {
            return;
        }
        Path logPath;
        if (logProp != null) {
            logPath = Path.of(logProp);
        } else if (base != null) {
            logPath = DosLib.runDir.resolve("ab3d2.log");   // run/ résolu par resolveDataDirs
        } else {
            return;                                          // dev : console
        }
        try {
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }
            PrintStream ps = new PrintStream(new FileOutputStream(logPath.toFile(), true), true, "UTF-8");
            System.setOut(ps);
            System.setErr(ps);
            System.out.println("=== AlienBreed3D2-TKG — démarrage (" + new java.util.Date() + ") ===");
            System.out.println("data=" + Assets.root.toAbsolutePath() + "  run=" + DosLib.runDir.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[Main] impossible d'ouvrir le log " + logPath + " : " + e);
        }
    }

    /** Journalise toute exception/erreur non capturée (sinon invisible en build sans console). */
    private static void installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("=== CRASH thread '" + t.getName() + "' ===");
            e.printStackTrace(System.err);
        });
    }

    /**
     * Watchdog anti-freeze : surveille {@link Display#frameHeartbeat}. S'il cesse d'avancer
     * pendant {@code -Dab3d2.watchdogMs} ms (défaut 5000 ; 0 = désactivé), dump la pile de tous
     * les threads dans le log → on identifie la boucle ({@code while(true)}) où le moteur gèle.
     * Thread démon, ne bloque jamais l'arrêt.
     */
    private static void startFreezeWatchdog() {
        long stallMs = Long.getLong("ab3d2.watchdogMs", 5000L);
        if (stallMs <= 0) {
            return;
        }
        Thread wd = new Thread(() -> {
            long lastBeat = Display.frameHeartbeat;
            long lastChange = System.nanoTime();
            boolean dumped = false;
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    return;
                }
                long beat = Display.frameHeartbeat;
                if (beat != lastBeat) {                      // le moteur avance → RAS
                    lastBeat = beat;
                    lastChange = System.nanoTime();
                    dumped = false;
                    continue;
                }
                if (beat == 0) {                             // pas encore démarré (chargement)
                    lastChange = System.nanoTime();
                    continue;
                }
                long stalledMs = (System.nanoTime() - lastChange) / 1_000_000L;
                if (stalledMs >= stallMs && !dumped) {
                    dumped = true;                           // un seul dump par épisode de gel
                    dumpAllThreads(stalledMs);
                }
            }
        }, "ab3d2-watchdog");
        wd.setDaemon(true);
        wd.start();
    }

    /** Écrit la pile de tous les threads (le thread principal = la boucle gelée) dans le log. */
    private static void dumpAllThreads(long stalledMs) {
        System.err.println();
        System.err.println("############################################################");
        System.err.println("# FREEZE détecté : aucune frame depuis " + stalledMs + " ms");
        System.err.println("# (pile des threads — cherche 'main' pour la boucle gelée)");
        System.err.println("############################################################");
        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            Thread t = e.getKey();
            System.err.println("\n\"" + t.getName() + "\" état=" + t.getState());
            for (StackTraceElement f : e.getValue()) {
                System.err.println("\tat " + f);
            }
        }
        System.err.println("############################################################\n");
    }
}
