package com.ab3d2.game;

import com.ab3d2.assets.MenuAssets;
import com.ab3d2.core.GameContext;
import com.ab3d2.core.InputHandler;
import com.ab3d2.menu.Ab3dFont;
import com.ab3d2.render.Renderer2D;

public class GameTest {

    private Renderer2D renderer;
    private GameScreen gameScreen;

    private final InputAdapter inputAdapter = new InputAdapter();

    public void init(int gameW, int gameH) {
        renderer = new Renderer2D(gameW, gameH);
        renderer.init();

        // Font / assets (à adapter à ton système existant)
        MenuAssets assets = new MenuAssets();
        assets.load(null);
        Ab3dFont font = new Ab3dFont(assets.getFontTexture(), 16, 16);

        Level level = new Level("TEST LEVEL");
        Player player = new Player();
        Renderer3D renderer3D = new Renderer3D();
        HUDRenderer hud = new HUDRenderer(font, gameW, gameH);
        MessageLog log = new MessageLog(font, gameW, gameH);

        gameScreen = new GameScreen(level, player, renderer3D, hud, log);
        gameScreen.init();
    }

    public void update(double dt, InputHandler inputHandler) {
        inputAdapter.update(inputHandler);
        gameScreen.update(dt, inputAdapter);

    }

    public void render(int windowW, int windowH, float[] viewport) {
        renderer.beginFrame();
        gameScreen.render(renderer);
        renderer.endFrame(windowW, windowH, viewport);
    }

    public void destroy() {
        gameScreen.destroy();
        renderer.destroy();
    }
}
