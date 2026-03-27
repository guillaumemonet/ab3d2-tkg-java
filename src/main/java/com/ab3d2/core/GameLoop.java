package com.ab3d2.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11.*;

/**
 * Boucle de jeu avec timestep fixe (fixed update) + interpolation du rendu.
 * Pattern classique : https://gafferongames.com/post/fix_your_timestep/
 */
public class GameLoop {

    private static final Logger log = LoggerFactory.getLogger(GameLoop.class);

    private static final double FIXED_DT = 1.0 / 60.0; // 60 Hz logique
    private static final double MAX_FRAME_TIME = 0.25;     // Evite la spirale de la mort

    private final Window window;
    private final GameContext ctx;
    private final StateManager stateManager;

    private long frameCount = 0;
    private double fps = 0;

    public GameLoop(Window window, GameContext ctx, StateManager stateManager) {
        this.window = window;
        this.ctx = ctx;
        this.stateManager = stateManager;
    }

    public void run(GameState initialState) {
        stateManager.transition(ctx, initialState);

        double currentTime = getTime();
        double accumulator = 0.0;

        double fpsTimer = 0;
        long fpsFrames = 0;

        while (!window.shouldClose()) {
            double newTime = getTime();
            double frameTime = Math.min(newTime - currentTime, MAX_FRAME_TIME);
            currentTime = newTime;

            accumulator += frameTime;

            // Poll events AVANT les updates
            window.pollEvents();

            // Fixed timestep updates
            while (accumulator >= FIXED_DT) {
                stateManager.update(ctx, FIXED_DT);
                accumulator -= FIXED_DT;
            }

            // Interpolation pour le rendu (alpha = position dans l'intervalle fixe actuel)
            double alpha = accumulator / FIXED_DT;

            // Rendu
            glClearColor(0f, 0f, 0f, 1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            stateManager.render(ctx, alpha);

            window.swapBuffers();

            // Reset états edge-triggered input
            ctx.input().endFrame();

            // FPS counter
            fpsFrames++;
            fpsTimer += frameTime;
            frameCount++;
            if (fpsTimer >= 1.0) {
                fps = fpsFrames / fpsTimer;
                window.setTitle(String.format("FPS: %.0f", fps));
                fpsFrames = 0;
                fpsTimer = 0;
            }
        }

        log.info("Game loop ended after {} frames", frameCount);
    }

    private double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    public double getFps() {
        return fps;
    }
}
