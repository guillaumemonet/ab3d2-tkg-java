package com.ab3d2;

import com.ab3d2.assets.AssetManager;
import com.ab3d2.audio.AudioManager;
import com.ab3d2.core.*;
import com.ab3d2.render.Renderer2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Alien Breed 3D II — Port Java/LWJGL Point d'entrée principal.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("AB3D2 Java Port starting...");

        // Résolution du répertoire assets
        Path assetRoot = resolveAssetRoot(args);
        log.info("Assets root: {}", assetRoot.toAbsolutePath());

        // Création des composants
        Window window = new Window("Alien Breed 3D II - Java Port", 3, true);
        AssetManager assets = new AssetManager(assetRoot);
        AudioManager audio = new AudioManager();
        Renderer2D renderer = new Renderer2D(Window.GAME_WIDTH, Window.GAME_HEIGHT);

        try {
            // Init dans le bon ordre
            window.init();
            assets.init();
            audio.init();
            renderer.init();

            GameContext ctx = new GameContext(window, assets, audio, renderer);
            StateManager states = new StateManager();
            GameLoop loop = new GameLoop(window, ctx, states);

            // Démarrage sur le menu principal
            loop.run(new GameState.MainMenu());

        } catch (Exception e) {
            log.error("Fatal error", e);
            System.exit(1);
        } finally {
            // Cleanup dans l'ordre inverse
            renderer.destroy();
            audio.destroy();
            assets.destroy();
            window.destroy();
            log.info("AB3D2 shutdown complete.");
        }
    }

    private static Path resolveAssetRoot(String[] args) {
        // Argument CLI optionnel : --assets /path/to/ab3d2/data
        for (int i = 0; i < args.length - 1; i++) {
            if ("--assets".equals(args[i])) {
                return Paths.get(args[i + 1]);
            }
        }
        // Défaut : ./assets/ à côté du jar (ou dans le working dir en dev)
        return Paths.get("C:\\Users\\guill\\Documents\\NetBeansProjects\\ab3d2-tkg-java\\build\\resources\\main");
    }
}
