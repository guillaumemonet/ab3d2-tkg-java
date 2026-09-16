package ab3d2.host;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Barrière de frame entre le thread du portage (simulation) et le thread de rendu jME (vue 3D).
 *
 * Renderer-swap : le portage tourne en mode {@code Hires.headless} sur son propre thread et appelle
 * {@link #run()} une fois par frame (via {@code Hires.frameSync}). À cet appel, la simulation est
 * « garée » : l'état de {@code Mem.RAM} est cohérent (joueur déplacé, objets/IA à jour). jME lit alors
 * cet état puis relâche le portage pour la frame suivante.
 *
 * Protocole (ping-pong) :
 *   PORT  : calcule frame N → {@link #run()} = signale prêt + attend l'autorisation
 *   jME   : {@link #awaitFrame} (attend prêt) → lit l'état → rend → {@link #releaseAdvance} (frame N+1)
 *
 * Aucune dépendance jME ici : jME pilote via les méthodes publiques.
 */
public final class FrameBridge implements Runnable {

    private final Semaphore frameReady = new Semaphore(0); // le port a fini une frame
    private final Semaphore advance = new Semaphore(0);    // jME autorise la frame suivante
    private volatile boolean stopping = false;

    /** Appelé par le THREAD DU PORT, une fois par itération de game_main_loop (frameSync). */
    @Override
    public void run() {
        frameReady.release();                 // « frame calculée, état cohérent »
        if (stopping) {
            return;
        }
        try {
            advance.acquire();                // bloque jusqu'à ce que jME dise « continue »
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Appelé par le THREAD jME : attend que le port ait fini une frame (timeout en ms). */
    public boolean awaitFrame(long timeoutMs) {
        try {
            return frameReady.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Appelé par le THREAD jME : laisse le port calculer la frame suivante. */
    public void releaseAdvance() {
        advance.release();
    }

    /** Arrêt propre : débloque le port (qui verra Hires.headlessStop et sortira de la boucle). */
    public void stop() {
        stopping = true;
        advance.release();
    }
}
