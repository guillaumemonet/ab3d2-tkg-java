package ab3d2.host;

import java.util.concurrent.locks.LockSupport;

/**
 * Couche hôte : la base de temps du balayage vertical, à 50 Hz (PAL).
 *
 * <p>Sur Amiga, {@code VBlankInterrupt} se déclenche <b>cinquante fois par seconde</b>, quelle
 * que soit la vitesse à laquelle le jeu dessine. C'est elle qui avance {@code Vid_VBLCount_l} et
 * décrémente {@code Anim_Timer_w}, donc qui donne leur cadence aux animations, à l'IA et aux
 * dégâts de sol. Le rendu, lui, tourne aussi vite qu'il peut et consomme le nombre de pas que
 * l'horloge a accumulés ({@code Anim_FramesToDraw_w}) : le jeu compense donc DÉJÀ sa vitesse
 * d'affichage.
 *
 * <p>Toute la question est de ne pas casser cette compensation. Déclencher l'interruption une
 * fois par image dessinée — ce que faisait le portage — la remplace par « la vitesse du jeu = la
 * fréquence de l'écran » : 20 % trop rapide sur un écran 60 Hz, 40 % trop lent à 30.
 *
 * <p>Cette classe rend donc le vrai nombre de tops écoulés, et sait attendre le suivant pour
 * {@code WaitTOF} — dont dépend le limiteur de F7.
 */
public final class Vbl {

    /** Fréquence du balayage PAL, celle du jeu d'origine. */
    public static final int HZ = 50;

    private static final long PERIOD_NS = 1_000_000_000L / HZ;

    /**
     * Au-delà, on ne rattrape plus : après une pause longue (fenêtre déplacée, chargement de
     * niveau, point d'arrêt), rattraper des centaines de tops ferait détaler le jeu.
     */
    private static final int MAX_CATCHUP = 4;

    /** Instant du prochain top, en nanosecondes de {@link System#nanoTime()}. */
    private static long next;

    private Vbl() {
    }

    /** Repart de maintenant : à appeler quand le jeu a été suspendu longtemps. */
    public static synchronized void reset() {
        next = System.nanoTime();
    }

    /** Nombre de tops écoulés depuis le dernier appel, rattrapage borné. */
    public static synchronized int due() {
        long now = System.nanoTime();
        if (next == 0) {
            next = now;
        }
        int n = 0;
        while (now >= next && n < MAX_CATCHUP) {
            next += PERIOD_NS;
            n++;
        }
        if (now >= next) {                  // trop de retard : on se recale sans rattraper
            next = now + PERIOD_NS;
        }
        return n;
    }

    /** Attend le prochain top (WaitTOF). */
    public static void waitNext() {
        long target;
        synchronized (Vbl.class) {
            long now = System.nanoTime();
            if (next == 0 || next <= now) {
                next = now + PERIOD_NS;
            }
            target = next;
            next += PERIOD_NS;
        }
        // Thread.sleep est trop grossier sous Windows pour une période de 20 ms : on dort le
        // gros du temps, puis on finit en attente active sur le dernier millième.
        long rough = target - System.nanoTime() - 1_500_000L;
        if (rough > 0) {
            LockSupport.parkNanos(rough);
        }
        while (System.nanoTime() < target) {
            Thread.onSpinWait();
        }
    }
}
