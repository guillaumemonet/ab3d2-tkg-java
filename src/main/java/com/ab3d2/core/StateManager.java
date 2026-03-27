package com.ab3d2.core;

import com.ab3d2.menu.MainMenuScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Gère le cycle de vie et les transitions entre les GameState/Screen.
 */
public class StateManager {

    private static final Logger log = LoggerFactory.getLogger(StateManager.class);

    private final Map<Class<? extends GameState>, Screen> screens = new HashMap<>();
    private Screen currentScreen;
    private GameState currentState;

    public StateManager() {
        // Enregistrement de tous les screens
        register(GameState.MainMenu.class, new MainMenuScreen());
        // register(GameState.LevelSelect.class, new LevelSelectScreen());
        // register(GameState.InGame.class,      new InGameScreen());
        // register(GameState.LevelEditor.class, new LevelEditorScreen());
        // register(GameState.Options.class,     new OptionsScreen());
    }

    private void register(Class<? extends GameState> stateClass, Screen screen) {
        screens.put(stateClass, screen);
    }

    public void transition(GameContext ctx, GameState newState) {
        log.info("State transition: {} → {}", currentState, newState);

        if (currentScreen != null) {
            currentScreen.destroy(ctx);
        }

        currentState = newState;
        currentScreen = screens.get(newState.getClass());

        if (currentScreen == null) {
            throw new IllegalStateException("No Screen registered for state: " + newState.getClass().getSimpleName());
        }

        currentScreen.init(ctx);
    }

    public void update(GameContext ctx, double deltaTime) {
        if (currentScreen == null) {
            return;
        }

        GameState next = currentScreen.update(ctx, deltaTime);
        if (next != null) {
            transition(ctx, next);
        }
    }

    public void render(GameContext ctx, double alpha) {
        if (currentScreen != null) {
            currentScreen.render(ctx, alpha);
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }
}
