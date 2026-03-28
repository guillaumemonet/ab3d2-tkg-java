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
 * Architecture de rendu (fidèle à l'original) :
 *   1. Background scrollant (plans 0-1, back2.raw)
 *   2. FireEffect (plans 0-5 simulés, blend additif) :
 *      - bits 0-2 : points Lissajous (mnu_plot, plans 0-2)
 *      - bits 3-5 : feu + TEXTE (même plans 3-5, le texte brûle par le bas)
 *   3. Fade noir
 *
 * Le texte est rendu dans MenuRenderer (buffer bitplane), injecté dans FireEffect.
 * Les crédits (credits_only.raw) s'affichent UNIQUEMENT sur "GAME CREDITS"
 * (reproduit mnu_viewcredz : remplace le texte, retour sur touche).
 */
public class MainMenuScreen implements Screen {

    private static final Logger log = LoggerFactory.getLogger(MainMenuScreen.class);

    private static final int GW = 320;
    private static final int GH = 200;

    // ── Positions ASM ─────────────────────────────────────────────────────────
    // mnu_mainmenu: dc.w 6,12 (xPos bytes, yPos px), dc.w 4,70 (curX bytes, curY px), spread=20
    private static final int MAIN_X_BYTES = 6;   // xPos texte en bytes
    private static final int MAIN_Y       = 12;
    private static final int MAIN_CUR_X   = 4;   // curX en bytes
    private static final int MAIN_CUR_Y   = 70;
    private static final int MAIN_SPREAD  = 20;
    // mnu_quitmenu: dc.w 4,82, dc.w 4,120, spread=20
    private static final int QUIT_X_BYTES = 4;
    private static final int QUIT_Y       = 82;
    private static final int QUIT_CUR_X   = 4;
    private static final int QUIT_CUR_Y   = 120;
    private static final int QUIT_SPREAD  = 20;

    // ── Textes (depuis ASM, majuscules) ───────────────────────────────────────
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
    private MenuRenderer        menuRenderer; // texte -> buffer bitplane
    private MenuCursor          cursor;

    // ── État ──────────────────────────────────────────────────────────────────
    private enum Mode { MAIN, QUIT, CREDITS }
    private Mode mode         = Mode.MAIN;
    private int  selectedItem = 0;
    private Mode prevMode     = null; // pour détecter les changements
    private int  prevSelected = -1;

    private float     fadeAlpha         = 0f;
    private float     fadeTarget        = 1f;
    private GameState pendingTransition = null;

    @Override
    public void init(GameContext ctx) {
        log.info("MainMenuScreen init");

        assets = new MenuAssets();
        assets.load(ctx);

        background = new ScrollingBackground();
        background.init(readFile(ctx, "menu/back2.raw"), assets.getBackpal());

        // FireEffect reçoit la palette complète 256 couleurs
        fire = new FireEffect(assets.getMenuPalette());
        //fire.init();

        menuRenderer = new MenuRenderer(assets.getFontRaw());
        cursor = new MenuCursor();

        mode              = Mode.MAIN;
        selectedItem      = 0;
        prevMode          = null;
        prevSelected      = -1;
        fadeAlpha         = 0f;
        fadeTarget        = 1f;
        pendingTransition = null;

        // Rendu initial du texte
        rebuildTextLayer();

        log.info("Menu ready — bg={}, fire={}, font={}",
            background.getTexture(), fire.getTexture(), assets.getFontTexture());
    }

    @Override
    public GameState update(GameContext ctx, double dt) {
        if (fadeAlpha < fadeTarget) fadeAlpha = Math.min(1f, fadeAlpha + (float)(dt * 2.0));
        else if (fadeAlpha > fadeTarget) fadeAlpha = Math.max(0f, fadeAlpha - (float)(dt * 2.0));

        if (pendingTransition != null && fadeAlpha <= 0.01f) return pendingTransition;
        if (pendingTransition != null) return null;

        background.update();

        // Rebuild texte si mode ou sélection a changé
        if (mode != prevMode || selectedItem != prevSelected) {
            rebuildTextLayer();
            prevMode     = mode;
            prevSelected = selectedItem;
        }

        fire.update();
        cursor.update(dt);
        handleInput(ctx);
        return null;
    }

    /** Régénère le buffer texte dans le feu. */
    private void rebuildTextLayer() {
        menuRenderer.clear();

        if (mode == Mode.CREDITS) {
            // Crédits : pas de texte menu, on affichera la texture credits dans render()
        } else if (mode == Mode.MAIN) {
            // Header : 2 newlines depuis Y=12 -> Y=52
            menuRenderer.drawString("<LEVEL A:", MAIN_X_BYTES, MAIN_Y + 40);
            // Items à partir de yCursor=70
            for (int i = 0; i < MAIN_ITEMS.length; i++) {
                menuRenderer.drawString(MAIN_ITEMS[i], MAIN_X_BYTES, MAIN_CUR_Y + i * MAIN_SPREAD);
            }
            // Crédit port en bas (centré, demi-taille = on l'écrit quand même en taille normale
            // mais positionné sur la dernière ligne disponible)
            menuRenderer.drawString(PORT_CREDIT, 2, GH - 16);
        } else { // QUIT
            // Titre : 2 newlines depuis Y=82 -> Y=122
            menuRenderer.drawString("QUIT GAME", QUIT_X_BYTES, QUIT_Y + 40);
            for (int i = 0; i < QUIT_ITEMS.length; i++) {
                menuRenderer.drawString(QUIT_ITEMS[i], QUIT_X_BYTES, QUIT_CUR_Y + i * QUIT_SPREAD);
            }
        }

        // Curseur dans le textLayer (s'enflamme aussi)
        {
            int curX  = (mode == Mode.MAIN ? MAIN_CUR_X : QUIT_CUR_X);
            int curY  = (mode == Mode.MAIN ? MAIN_CUR_Y : QUIT_CUR_Y)
                      + selectedItem * (mode == Mode.MAIN ? MAIN_SPREAD : QUIT_SPREAD);
            int glyph = cursor.getCurrentGlyph();
            menuRenderer.drawString(String.valueOf((char)glyph), curX, curY);
        }
        fire.setTextLayer(menuRenderer.getTextLayer());
    }

    private void handleInput(GameContext ctx) {
        InputHandler in = ctx.input();

        if (mode == Mode.CREDITS) {
            if (anyKey(in)) { switchTo(Mode.MAIN, 2); }
            return;
        }

        String[] items = (mode == Mode.MAIN) ? MAIN_ITEMS : QUIT_ITEMS;
        if (in.isKeyPressed(GLFW.GLFW_KEY_UP)   || in.isKeyPressed(GLFW.GLFW_KEY_W))
            { selectedItem = (selectedItem - 1 + items.length) % items.length; cursor.reset(); }
        if (in.isKeyPressed(GLFW.GLFW_KEY_DOWN)  || in.isKeyPressed(GLFW.GLFW_KEY_S))
            { selectedItem = (selectedItem + 1) % items.length; cursor.reset(); }
        if (in.isKeyPressed(GLFW.GLFW_KEY_ENTER) || in.isKeyPressed(GLFW.GLFW_KEY_SPACE))
            activate(ctx);
        if (in.isKeyPressed(GLFW.GLFW_KEY_ESCAPE))
            { if (mode == Mode.QUIT) switchTo(Mode.MAIN, 0); else switchTo(Mode.QUIT, 0); }
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
    private void fadeAndGo(GameState next) { fadeTarget = 0f; pendingTransition = next; }

    @Override
    public void render(GameContext ctx, double alpha) {
        Renderer2D r = ctx.renderer();
        r.beginFrame();

        // 1. Background scrollant
        background.render(r, GW, GH);

        // 2. Crédits (UNIQUEMENT en mode CREDITS)
        if (mode == Mode.CREDITS && assets.getCreditsTexture() >= 0) {
            r.drawTexture(assets.getCreditsTexture(), 0, 32, GW, 192);
        }

        // 3. Feu + texte + points (blend ADDITIF)
        // Le texte est dans le buffer fire (bits 3-5), s'additionne sur le background
        if (fire.getTexture() >= 0) {
            r.drawTexture(fire.getTexture(), 0, 0, GW, GH);
        }

        // 4. Curseur (rendu dans le feu aussi via menuRenderer serait idéal,
        //    mais on l'overlay en direct pour garantir sa visibilité)
        if (mode != Mode.CREDITS) {
            renderCursorOverlay(r);
        }

        // 5. Fade
        r.drawFadeOverlay(1f - fadeAlpha);

        r.endFrame(ctx.window().getWidth(), ctx.window().getHeight(),
                   ctx.window().getViewportRect());
    }

    /**
     * Curseur glyphe rendu directement via la texture font (overlay au-dessus du feu).
     * Dans l'original le curseur est aussi dans les plans 3-5, donc dans le feu.
     * On le met en overlay pour qu'il reste toujours visible.
     */
    private void renderCursorOverlay(Renderer2D r) {
        if (assets.getFontTexture() < 0) return;

        // Glyphe curseur courant
        int c = cursor.getCurrentGlyph();
        int idx = c - 32;
        if (idx < 0 || idx >= MenuAssets.FONT_NUM_CHARS) return;

        int atlasX = (idx % MenuAssets.FONT_COLS) * MenuAssets.FONT_GLYPH_W;
        int atlasY = (idx / MenuAssets.FONT_COLS) * MenuAssets.FONT_GLYPH_H;
        float u0 = (float) atlasX / MenuAssets.FONT_W;
        float v0 = (float) atlasY / MenuAssets.FONT_H;
        float u1 = u0 + (float) MenuAssets.FONT_GLYPH_W / MenuAssets.FONT_W;
        float v1 = v0 + (float) MenuAssets.FONT_GLYPH_H / MenuAssets.FONT_H;

        float cx   = (mode == Mode.MAIN ? MAIN_CUR_X : QUIT_CUR_X) * 8f;
        float cy   = (mode == Mode.MAIN ? MAIN_CUR_Y : QUIT_CUR_Y)
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
        catch (java.io.IOException e) { return null; }
    }
}
