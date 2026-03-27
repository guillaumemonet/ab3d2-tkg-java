package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;
import com.ab3d2.core.*;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Menu principal AB3D2 — reproduction fidèle du menu Amiga.
 *
 * Pipeline de rendu (bas -> haut) : 1. Background scrollant (back2.raw, 2
 * bitplanes) 2. Effet feu (simulation blitter) 3. Frame credits 4. Texte menu
 * (font 16x16) 5. Curseur animé
 */
public class MainMenuScreen implements Screen {

    private static final Logger log = LoggerFactory.getLogger(MainMenuScreen.class);

    private static final int GW = 320;
    private static final int GH = 200;

    // Positions (reproduit l'ASM : xPos=4 words=8bytes, yCursor=70, spread=20)
    private static final int TEXT_X = 4 * 8;  // 32px
    private static final int ITEMS_Y = 70;
    private static final int SPREAD = 20;
    private static final int CURSOR_X = TEXT_X - 20;

    private MenuAssets assets;
    private ScrollingBackground background;
    private FireEffect fire;
    private Ab3dFont font;
    private MenuCursor cursor;

    private static final List<String> MAIN_ITEMS = List.of(
            "Play game",
            "Control options",
            "Game credits",
            "Load position",
            "Save position",
            "Quit"
    );

    private int selectedItem = 0;
    private float fadeAlpha = 0f;
    private float fadeTarget = 1f;
    private GameState pendingTransition = null;
    private double exitTimer = 0;

    @Override
    public void init(GameContext ctx) {
        log.info("MainMenuScreen init");

        assets = new MenuAssets();
        assets.load(ctx);

        background = new ScrollingBackground();
        background.init(readFile(ctx, "menu/back2.raw"), assets.getBackpal());

        fire = new FireEffect(assets.getBackpal(), assets.getFirepal());
        //fire.init();

        font = assets.getFontTexture() >= 0 ? new Ab3dFont(assets.getFontTexture()) : null;
        cursor = new MenuCursor();

        selectedItem = 0;
        fadeAlpha = 0f;
        fadeTarget = 1f;
        pendingTransition = null;

        log.info("Menu ready — font={}, bg={}, fire={}",
                font != null, background.getTexture() >= 0, fire.getTexture() >= 0);
    }

    @Override
    public GameState update(GameContext ctx, double dt) {
        // Fade
        if (Math.abs(fadeAlpha - fadeTarget) > 0.01f) {
            float dir = fadeTarget > fadeAlpha ? 1f : -1f;
            fadeAlpha = Math.clamp(fadeAlpha + dir * (float) (dt * 2.0), 0f, 1f);
        }

        // Transition après fade out
        if (pendingTransition != null && fadeAlpha <= 0.01f) {
            return pendingTransition;
        }

        if (pendingTransition != null) {
            return null;
        }

        background.update();
        fire.update();
        cursor.update(dt);

        InputHandler input = ctx.input();
        if (input.isKeyPressed(GLFW.GLFW_KEY_UP) || input.isKeyPressed(GLFW.GLFW_KEY_W)) {
            selectedItem = (selectedItem - 1 + MAIN_ITEMS.size()) % MAIN_ITEMS.size();
            cursor.reset();
        }
        if (input.isKeyPressed(GLFW.GLFW_KEY_DOWN) || input.isKeyPressed(GLFW.GLFW_KEY_S)) {
            selectedItem = (selectedItem + 1) % MAIN_ITEMS.size();
            cursor.reset();
        }
        if (input.isKeyPressed(GLFW.GLFW_KEY_ENTER) || input.isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
            activate(ctx, selectedItem);
        }
        if (input.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            activate(ctx, MAIN_ITEMS.size() - 1);
        }

        return null;
    }

    private void activate(GameContext ctx, int idx) {
        log.info("Menu: {}", MAIN_ITEMS.get(idx));
        switch (MAIN_ITEMS.get(idx)) {
            case "Play game" ->
                fadeAndGo(new GameState.LevelSelect());
            case "Quit" ->
                GLFW.glfwSetWindowShouldClose(ctx.window().getHandle(), true);
            default ->
                log.warn("'{}' not implemented yet", MAIN_ITEMS.get(idx));
        }
    }

    private void fadeAndGo(GameState next) {
        fadeTarget = 0f;
        pendingTransition = next;
    }

    @Override
    public void render(GameContext ctx, double alpha) {
        var r = ctx.renderer();
        r.beginFrame();

        // 1. Background scrollant
        background.render(r, GW, GH);

        // 2. Feu
        if (fire.getTexture() >= 0) {
            r.drawTexture(fire.getTexture(), 0, 0, GW, GH);
        }

        // 3. Credits frame (positionné en haut, ligne 32 dans l'original)
        if (assets.getCreditsTexture() >= 0) {
            //r.drawTexture(assets.getCreditsTexture(), 0, 0, GW, 192);
        }

        // 4. Texte
        if (font != null) {
            font.drawString(r, "Level A:", TEXT_X, 12);
            for (int i = 0; i < MAIN_ITEMS.size(); i++) {
                font.drawString(r, MAIN_ITEMS.get(i), TEXT_X, ITEMS_Y + i * SPREAD);
            }
        }

        // 5. Curseur
        if (font != null) {
            cursor.render(r, font, CURSOR_X, ITEMS_Y + selectedItem * SPREAD);
        }

        // 6. Fade overlay (noir)
        r.drawFadeOverlay(1f - fadeAlpha);

        float[] vp = ctx.window().getViewportRect();
        r.endFrame(ctx.window().getWidth(), ctx.window().getHeight(), vp);
    }

    @Override
    public void destroy(GameContext ctx) {
        if (background != null) {
            background.destroy();
        }
        if (fire != null) {
            fire.destroy();
        }
        log.info("MainMenuScreen destroyed");
    }

    private byte[] readFile(GameContext ctx, String path) {
        try {
            return java.nio.file.Files.readAllBytes(ctx.assets().getRoot().resolve(path));
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
