package com.ab3d2.menu;

import com.ab3d2.assets.MenuAssets;
import com.ab3d2.core.*;
import com.ab3d2.render.Renderer2D;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Menu principal AB3D2.
 *
 * Architecture de rendu :
 *   1. Background scrollant (back2.raw)
 *   2. FireEffect (blend normal, alpha par pixel, crop 200/256 lignes)
 *      → contient texte (plans 3-5) + feu (plans 0-2) + plots
 *      → le texte ici sert principalement de seed pour le feu
 *   3. Texte items — rendu DIRECT via font texture (mêmes UV que cursor overlay)
 *      → garantit les bonnes couleurs, indépendant du plan dans le feu
 *   4. Curseur overlay direct (font texture)
 *   5. Fade noir
 */
public class MainMenuScreen implements Screen {

    private static final Logger log = LoggerFactory.getLogger(MainMenuScreen.class);

    private static final int GW = 320;
    private static final int GH = 200;

    // Crop UV : buffer feu 320x256 → FBO 320x200 (crop, pas squish)
    private static final float FIRE_V1 = (float) GH / FireEffect.H;  // ≈ 0.781

    // ── Positions ASM ─────────────────────────────────────────────────────────
    private static final int MAIN_X_BYTES = 6;
    private static final int MAIN_Y       = 12;
    private static final int MAIN_CUR_X   = 4;
    private static final int MAIN_CUR_Y   = 70;
    private static final int MAIN_SPREAD  = 20;

    private static final int QUIT_X_BYTES = 4;
    private static final int QUIT_Y       = 82;
    private static final int QUIT_CUR_X   = 4;
    private static final int QUIT_CUR_Y   = 120;
    private static final int QUIT_SPREAD  = 20;

    // ── Textes ────────────────────────────────────────────────────────────────
    private static final String[] MAIN_ITEMS = {
        "PLAY GAME", "CONTROL OPTIONS", "GAME CREDITS",
        "LOAD POSITION", "SAVE POSITION", "QUIT"
    };
    private static final String[] QUIT_ITEMS = {
        "NO, I'M ADDICTED", "YES! LET ME OUT"
    };
    private static final String PORT_CREDIT = "PORTED BY GUILLAUME MONET";

    // ── Composants ────────────────────────────────────────────────────────────
    private MenuAssets          assets;
    private ScrollingBackground background;
    private FireEffect          fire;
    private MenuRenderer        menuRenderer;
    private MenuCursor          cursor;

    // ── État ──────────────────────────────────────────────────────────────────
    private enum Mode { MAIN, QUIT, CREDITS }
    private Mode mode         = Mode.MAIN;
    private int  selectedItem = 0;

    private Mode prevMode     = null;
    private int  prevSelected = -1;
    private int  prevGlyph    = -1;

    private float     fadeAlpha         = 0f;
    private float     fadeTarget        = 1f;
    private GameState pendingTransition = null;

    @Override
    public void init(GameContext ctx) {
        log.info("MainMenuScreen.init() start");

        assets = new MenuAssets();
        assets.load(ctx);

        background = new ScrollingBackground();
        background.init(readFile(ctx, "menu/back2.raw"), assets.getBackpal());

        fire = new FireEffect(assets.getMenuPalette());
        fire.init();  // NE PAS COMMENTER

        menuRenderer = new MenuRenderer(assets.getFontRaw());
        cursor       = new MenuCursor();

        mode              = Mode.MAIN;
        selectedItem      = 0;
        prevMode          = null;
        prevSelected      = -1;
        prevGlyph         = -1;
        fadeAlpha         = 0f;
        fadeTarget        = 1f;
        pendingTransition = null;

        rebuildTextLayer();

        log.info("MainMenuScreen.init() done — bg={} fire={} font={}",
            background.getTexture(), fire.getTexture(), assets.getFontTexture());
    }

    @Override
    public GameState update(GameContext ctx, double dt) {
        if (fadeAlpha < fadeTarget) fadeAlpha = Math.min(1f, fadeAlpha + (float)(dt * 2.0));
        else if (fadeAlpha > fadeTarget) fadeAlpha = Math.max(0f, fadeAlpha - (float)(dt * 2.0));

        if (pendingTransition != null && fadeAlpha <= 0.01f) return pendingTransition;
        if (pendingTransition != null) return null;

        background.update();
        cursor.update(dt);

        int curGlyph = cursor.getCurrentGlyph();
        if (mode != prevMode || selectedItem != prevSelected || curGlyph != prevGlyph) {
            rebuildTextLayer();
            prevMode     = mode;
            prevSelected = selectedItem;
            prevGlyph    = curGlyph;
        }

        fire.update();
        handleInput(ctx);
        return null;
    }

    /** Alimente le feu avec les positions du texte + curseur. */
    private void rebuildTextLayer() {
        menuRenderer.clear();

        if (mode == Mode.CREDITS) {
            // Pas de seed en mode crédits
        } else if (mode == Mode.MAIN) {
            menuRenderer.drawString("<LEVEL A:", MAIN_X_BYTES, MAIN_Y + 40);
            for (int i = 0; i < MAIN_ITEMS.length; i++) {
                menuRenderer.drawString(MAIN_ITEMS[i], MAIN_X_BYTES, MAIN_CUR_Y + i * MAIN_SPREAD);
            }
            menuRenderer.drawString(PORT_CREDIT, 2, GH - 16);
        } else {
            menuRenderer.drawString("QUIT GAME", QUIT_X_BYTES, QUIT_Y + 40);
            for (int i = 0; i < QUIT_ITEMS.length; i++) {
                menuRenderer.drawString(QUIT_ITEMS[i], QUIT_X_BYTES, QUIT_CUR_Y + i * QUIT_SPREAD);
            }
        }

        // Curseur dans les seeds du feu
        if (mode != Mode.CREDITS) {
            int curX = (mode == Mode.MAIN ? MAIN_CUR_X : QUIT_CUR_X);
            int curY = (mode == Mode.MAIN ? MAIN_CUR_Y : QUIT_CUR_Y)
                     + selectedItem * (mode == Mode.MAIN ? MAIN_SPREAD : QUIT_SPREAD);
            menuRenderer.drawString(String.valueOf((char) cursor.getCurrentGlyph()), curX, curY);
        }

        fire.setTextLayer(menuRenderer.getTextLayer());
    }

    private void handleInput(GameContext ctx) {
        InputHandler in = ctx.input();

        if (mode == Mode.CREDITS) {
            if (anyKey(in)) switchTo(Mode.MAIN, 2);
            return;
        }

        String[] items = (mode == Mode.MAIN) ? MAIN_ITEMS : QUIT_ITEMS;
        if (in.isKeyPressed(GLFW.GLFW_KEY_UP)   || in.isKeyPressed(GLFW.GLFW_KEY_W))
            { selectedItem = (selectedItem - 1 + items.length) % items.length; cursor.reset(); }
        if (in.isKeyPressed(GLFW.GLFW_KEY_DOWN)  || in.isKeyPressed(GLFW.GLFW_KEY_S))
            { selectedItem = (selectedItem + 1) % items.length; cursor.reset(); }
        if (in.isKeyPressed(GLFW.GLFW_KEY_ENTER) || in.isKeyPressed(GLFW.GLFW_KEY_SPACE))
            activate(ctx);
        if (in.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            if (mode == Mode.QUIT) switchTo(Mode.MAIN, 0);
            else switchTo(Mode.QUIT, 0);
        }
    }

    private boolean anyKey(InputHandler in) {
        return in.isKeyPressed(GLFW.GLFW_KEY_ENTER) || in.isKeyPressed(GLFW.GLFW_KEY_SPACE)
            || in.isKeyPressed(GLFW.GLFW_KEY_ESCAPE) || in.isKeyPressed(GLFW.GLFW_KEY_BACKSPACE);
    }

    private void activate(GameContext ctx) {
        if (mode == Mode.MAIN) {
            switch (MAIN_ITEMS[selectedItem]) {
                case "PLAY GAME"    -> fadeAndGo(new GameState.LevelSelect());
                case "GAME CREDITS" -> switchTo(Mode.CREDITS, 0);
                case "QUIT"         -> switchTo(Mode.QUIT, 0);
                default -> log.warn("'{}' not implemented yet", MAIN_ITEMS[selectedItem]);
            }
        } else {
            if (selectedItem == 0) switchTo(Mode.MAIN, 0);
            else GLFW.glfwSetWindowShouldClose(ctx.window().getHandle(), true);
        }
    }

    private void switchTo(Mode m, int sel) { mode = m; selectedItem = sel; cursor.reset(); }
    private void fadeAndGo(GameState next)  { fadeTarget = 0f; pendingTransition = next; }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    @Override
    public void render(GameContext ctx, double alpha) {
        Renderer2D r = ctx.renderer();
        r.beginFrame();

        // 1. Background scrollant
        background.render(r, GW, GH);

        // 2. Crédits (mode CREDITS uniquement)
        if (mode == Mode.CREDITS && assets.getCreditsTexture() >= 0) {
            r.drawTexture(assets.getCreditsTexture(), 0, 32, GW, 192);
        }

        // 3. Feu + texte-dans-feu + plots
        //    UV crop : v1 = 200/256 pour éviter le squish vertical
        if (fire.getTexture() >= 0) {
            r.drawTexture(fire.getTexture(), 0, 0, GW, GH,
                          0f, 0f, 1f, FIRE_V1);
        }

        // 4. Texte direct via font texture — mêmes couleurs que le curseur
        //    Rendu PAR-DESSUS le feu pour garantir lisibilité et couleur correcte.
        if (mode != Mode.CREDITS) {
            renderAllTextDirect(r);
        }

        // 5. Curseur overlay direct
        if (mode != Mode.CREDITS) {
            renderCursorOverlay(r);
        }

        // 6. Fade
        r.drawFadeOverlay(1f - fadeAlpha);

        r.endFrame(ctx.window().getWidth(), ctx.window().getHeight(),
                   ctx.window().getViewportRect());
    }

    // ── Rendu texte direct via font texture ───────────────────────────────────

    private void renderAllTextDirect(Renderer2D r) {
        if (assets.getFontTexture() < 0) return;

        if (mode == Mode.MAIN) {
            renderStringDirect(r, "<LEVEL A:", MAIN_X_BYTES * 8f, MAIN_Y + 40);
            for (int i = 0; i < MAIN_ITEMS.length; i++) {
                renderStringDirect(r, MAIN_ITEMS[i],
                    MAIN_X_BYTES * 8f, MAIN_CUR_Y + i * MAIN_SPREAD);
            }
            renderStringDirect(r, PORT_CREDIT, 2 * 8f, GH - 16);
        } else {
            renderStringDirect(r, "QUIT GAME",
                QUIT_X_BYTES * 8f, QUIT_Y + 40);
            for (int i = 0; i < QUIT_ITEMS.length; i++) {
                renderStringDirect(r, QUIT_ITEMS[i],
                    QUIT_X_BYTES * 8f, QUIT_CUR_Y + i * QUIT_SPREAD);
            }
        }
    }

    private void renderStringDirect(Renderer2D r, String text, float x, float y) {
        float curX = x;
        for (int i = 0; i < text.length(); i++) {
            int c   = text.charAt(i);
            int idx = c - MenuAssets.FONT_FIRST_CHAR;
            if (idx < 0 || idx >= MenuAssets.FONT_NUM_CHARS) {
                curX += MenuAssets.FONT_GLYPH_W;
                continue;
            }
            int atlasX = (idx % MenuAssets.FONT_COLS) * MenuAssets.FONT_GLYPH_W;
            int atlasY = (idx / MenuAssets.FONT_COLS) * MenuAssets.FONT_GLYPH_H;
            float u0 = (float) atlasX / MenuAssets.FONT_W;
            float v0 = (float) atlasY / MenuAssets.FONT_H;
            float u1 = u0 + (float) MenuAssets.FONT_GLYPH_W / MenuAssets.FONT_W;
            float v1 = v0 + (float) MenuAssets.FONT_GLYPH_H / MenuAssets.FONT_H;
            r.drawTexture(assets.getFontTexture(), curX, y,
                          MenuAssets.FONT_GLYPH_W, MenuAssets.FONT_GLYPH_H,
                          u0, v0, u1, v1);
            curX += MenuAssets.FONT_GLYPH_W;
        }
    }

    private void renderCursorOverlay(Renderer2D r) {
        if (assets.getFontTexture() < 0) return;
        int c   = cursor.getCurrentGlyph();
        int idx = c - MenuAssets.FONT_FIRST_CHAR;
        if (idx < 0 || idx >= MenuAssets.FONT_NUM_CHARS) return;
        int atlasX = (idx % MenuAssets.FONT_COLS) * MenuAssets.FONT_GLYPH_W;
        int atlasY = (idx / MenuAssets.FONT_COLS) * MenuAssets.FONT_GLYPH_H;
        float u0 = (float) atlasX / MenuAssets.FONT_W;
        float v0 = (float) atlasY / MenuAssets.FONT_H;
        float u1 = u0 + (float) MenuAssets.FONT_GLYPH_W / MenuAssets.FONT_W;
        float v1 = v0 + (float) MenuAssets.FONT_GLYPH_H / MenuAssets.FONT_H;
        float cx = (mode == Mode.MAIN ? MAIN_CUR_X : QUIT_CUR_X) * 8f;
        float cy = (mode == Mode.MAIN ? MAIN_CUR_Y : QUIT_CUR_Y)
                 + selectedItem * (mode == Mode.MAIN ? MAIN_SPREAD : QUIT_SPREAD);
        r.drawTexture(assets.getFontTexture(), cx, cy,
                      MenuAssets.FONT_GLYPH_W, MenuAssets.FONT_GLYPH_H, u0, v0, u1, v1);
    }

    @Override
    public void destroy(GameContext ctx) {
        if (background != null) background.destroy();
        if (fire       != null) fire.destroy();
        log.info("MainMenuScreen destroyed");
    }

    private byte[] readFile(GameContext ctx, String path) {
        try { return java.nio.file.Files.readAllBytes(ctx.assets().getRoot().resolve(path)); }
        catch (java.io.IOException e) { log.warn("Cannot read {}: {}", path, e.getMessage()); return null; }
    }
}
