package com.ab3d2.core;

import com.ab3d2.assets.AssetManager;
import com.ab3d2.audio.AudioManager;
import com.ab3d2.render.Renderer2D;

/**
 * Contexte global du jeu — injecté dans chaque Screen.
 * Pattern "service locator" léger, évite le singleton global.
 */
public record GameContext(
    Window       window,
    AssetManager assets,
    AudioManager audio,
    Renderer2D   renderer
) {
    public InputHandler input() {
        return window.getInput();
    }
}
