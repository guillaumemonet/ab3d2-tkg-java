package com.ab3d2.core;

import com.ab3d2.game.InGameScreen;
import com.ab3d2.game.LevelSelectScreen;
import com.ab3d2.menu.MainMenuScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Gère le cycle de vie et les transitions entre les GameState/Screen.
 *
 * Les screens paramétriques (InGame, LevelEditor…) sont créés via des factories
 * pour pouvoir passer les arguments du GameState au Screen correspondant.
 */
public class StateManager {

    private static final Logger log = LoggerFactory.getLogger(StateManager.class);

    @FunctionalInterface
    interface ScreenFactory {
        Screen create(GameState state);
    }

    private final Map<Class<? extends GameState>, ScreenFactory> factories = new HashMap<>();
    private Screen    currentScreen;
    private GameState currentState;

    public StateManager() {
        // Screens simples (sans paramètre d'état utilisé)
        register(GameState.MainMenu.class,    state -> new MainMenuScreen());
        register(GameState.LevelSelect.class, state -> new LevelSelectScreen());

        // Screens paramétriques
        register(GameState.InGame.class, state -> {
            GameState.InGame s = (GameState.InGame) state;
            return new InGameScreen(s.levelIndex());
        });

        // À implémenter :
        // register(GameState.Options.class,  state -> new OptionsScreen());
        // register(GameState.Credits.class,  state -> new CreditsScreen());
        // register(GameState.LevelEditor.class, state -> {
        //     GameState.LevelEditor s = (GameState.LevelEditor) state;
        //     return new LevelEditorScreen(s.levelPath());
        // });
        // register(GameState.Loading.class, state -> {
        //     GameState.Loading s = (GameState.Loading) state;
        //     return new LoadingScreen(s.next(), s.message());
        // });
    }

    private void register(Class<? extends GameState> stateClass, ScreenFactory factory) {
        factories.put(stateClass, factory);
    }

    public void transition(GameContext ctx, GameState newState) {
        log.info("State transition: {} → {}", currentState, newState);

        if (currentScreen != null) {
            currentScreen.destroy(ctx);
            currentScreen = null;
        }

        currentState = newState;
        ScreenFactory factory = factories.get(newState.getClass());

        if (factory == null) {
            throw new IllegalStateException(
                "Aucun Screen enregistré pour : " + newState.getClass().getSimpleName()
                + " — ajouter dans StateManager.java");
        }

        currentScreen = factory.create(newState);
        currentScreen.init(ctx);
    }

    public void update(GameContext ctx, double deltaTime) {
        if (currentScreen == null) return;
        GameState next = currentScreen.update(ctx, deltaTime);
        if (next != null) transition(ctx, next);
    }

    public void render(GameContext ctx, double alpha) {
        if (currentScreen != null) currentScreen.render(ctx, alpha);
    }

    public GameState getCurrentState() { return currentState; }
}
