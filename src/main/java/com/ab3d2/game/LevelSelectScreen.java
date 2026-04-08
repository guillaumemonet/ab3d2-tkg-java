package com.ab3d2.game;

import com.ab3d2.LevelManager;
import com.ab3d2.core.*;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL33.*;

/**
 * Écran de sélection de niveau (A → P).
 */
public class LevelSelectScreen implements Screen {

    private static final Logger log = LoggerFactory.getLogger(LevelSelectScreen.class);

    private static final int GW = Window.GAME_WIDTH;
    private static final int GH = Window.GAME_HEIGHT;

    private static final String[] LEVEL_NAMES = {
        "LEVEL A", "LEVEL B", "LEVEL C", "LEVEL D",
        "LEVEL E", "LEVEL F", "LEVEL G", "LEVEL H",
        "LEVEL I", "LEVEL J", "LEVEL K", "LEVEL L",
        "LEVEL M", "LEVEL N", "LEVEL O", "LEVEL P"
    };
    private static final int NUM_LEVELS     = LEVEL_NAMES.length;
    private static final int ITEMS_PER_PAGE = 8;

    private int       selectedIndex = 0;
    private int       scrollOffset  = 0;
    private float     fadeAlpha     = 0f;
    private float     fadeTarget    = 1f;
    private GameState pending       = null;

    private int      screenTexId = -1;
    private int[]    screenBuf   = new int[GW * GH];

    private boolean[] levelAvailable = new boolean[NUM_LEVELS];

    @Override
    public void init(GameContext ctx) {
        log.info("LevelSelectScreen init");

        LevelManager mgr = new LevelManager(ctx.assets().getRoot());
        String[] available = mgr.listAvailableLevels();
        for (String lv : available) {
            if (lv.length() == 1) {
                int idx = lv.toUpperCase().charAt(0) - 'A';
                if (idx >= 0 && idx < NUM_LEVELS) levelAvailable[idx] = true;
            }
        }

        fadeAlpha  = 0f;
        fadeTarget = 1f;
        pending    = null;

        renderFrame();
        screenTexId = ctx.assets().createTextureFromARGB(screenBuf, GW, GH);
    }

    @Override
    public GameState update(GameContext ctx, double dt) {
        if (fadeAlpha < fadeTarget) fadeAlpha = Math.min(1f, fadeAlpha + (float)(dt * 3.0));
        else if (fadeAlpha > fadeTarget) fadeAlpha = Math.max(0f, fadeAlpha - (float)(dt * 3.0));

        if (pending != null && fadeAlpha <= 0.01f) return pending;
        if (pending != null) return null;

        InputHandler in = ctx.input();

        if (in.isKeyPressed(GLFW.GLFW_KEY_UP)   || in.isKeyPressed(GLFW.GLFW_KEY_W)
         || in.isKeyPressed(GLFW.GLFW_KEY_Z)) {
            selectedIndex = (selectedIndex - 1 + NUM_LEVELS) % NUM_LEVELS;
            clampScroll();
        }
        if (in.isKeyPressed(GLFW.GLFW_KEY_DOWN)  || in.isKeyPressed(GLFW.GLFW_KEY_S)) {
            selectedIndex = (selectedIndex + 1) % NUM_LEVELS;
            clampScroll();
        }
        if (in.isKeyPressed(GLFW.GLFW_KEY_ENTER) || in.isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
            fadeTarget = 0f;
            pending    = new GameState.InGame(selectedIndex);
        }
        if (in.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            fadeTarget = 0f;
            pending    = new GameState.MainMenu();
        }

        renderFrame();
        return null;
    }

    @Override
    public void render(GameContext ctx, double alpha) {
        uploadFrame();
        var r = ctx.renderer();
        r.beginFrame();
        if (screenTexId >= 0) r.drawTexture(screenTexId, 0, 0, GW, GH);
        r.drawFadeOverlay(1f - fadeAlpha);
        r.endFrame(ctx.window().getWidth(), ctx.window().getHeight(),
                   ctx.window().getViewportRect());
    }

    @Override
    public void destroy(GameContext ctx) {
        if (screenTexId >= 0) { glDeleteTextures(screenTexId); screenTexId = -1; }
    }

    private void renderFrame() {
        Arrays.fill(screenBuf, 0xFF080818);
        drawText("SELECT LEVEL", 84, 10, 0xFFFFFF00);
        drawSeparator(20, 0xFF444444);

        int y = 28;
        int end = Math.min(scrollOffset + ITEMS_PER_PAGE, NUM_LEVELS);
        for (int i = scrollOffset; i < end; i++) {
            boolean sel   = (i == selectedIndex);
            boolean avail = levelAvailable[i];
            int col = sel ? 0xFF00FF00 : (avail ? 0xFFCCCCCC : 0xFF555555);
            if (sel) { fillRect(0, y - 1, GW, 11, 0xFF111133); drawText(">", 26, y, 0xFF00FF00); }
            drawText(LEVEL_NAMES[i], 38, y, col);
            if (!avail) drawText("[N/A]", 130, y, 0xFF883333);
            y += 18;
        }

        drawSeparator(GH - 18, 0xFF444444);
        drawText("ENTER=PLAY    ESC=BACK", 28, GH - 14, 0xFF666666);

        if (NUM_LEVELS > ITEMS_PER_PAGE) {
            int totalH = ITEMS_PER_PAGE * 18;
            int barH   = Math.max(4, totalH * ITEMS_PER_PAGE / NUM_LEVELS);
            int maxScrl = NUM_LEVELS - ITEMS_PER_PAGE;
            int barY   = 28 + (totalH - barH) * scrollOffset / Math.max(1, maxScrl);
            fillRect(GW - 5, 28, 3, totalH, 0xFF222222);
            fillRect(GW - 5, barY, 3, barH,  0xFF555555);
        }
    }

    private void drawSeparator(int y, int col) {
        for (int x = 4; x < GW - 4; x++) setPixel(x, y, col);
    }

    private void uploadFrame() {
        if (screenTexId < 0) return;
        ByteBuffer buf = ByteBuffer.allocateDirect(GW * GH * 4);
        for (int px : screenBuf) {
            buf.put((byte)((px >> 16) & 0xFF));
            buf.put((byte)((px >>  8) & 0xFF));
            buf.put((byte)( px        & 0xFF));
            buf.put((byte)((px >> 24) & 0xFF));
        }
        buf.flip();
        glBindTexture(GL_TEXTURE_2D, screenTexId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, GW, GH, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void drawText(String text, int x, int y, int col) {
        int cx = x;
        for (char c : text.toUpperCase().toCharArray()) { drawChar5x7(c, cx, y, col); cx += 6; }
    }

    private void drawChar5x7(char c, int x, int y, int col) {
        long[] g = GLYPHS_5x7(c);
        if (g == null) return;
        for (int row = 0; row < 7; row++)
            for (int bit = 4; bit >= 0; bit--)
                if (((g[row] >> bit) & 1) != 0)
                    setPixel(x + (4 - bit), y + row, col);
    }

    private static long[] GLYPHS_5x7(char c) {
        return switch (c) {
            case ' ' -> new long[]{0,0,0,0,0,0,0};
            case '>' -> new long[]{0b10000,0b01000,0b00100,0b00010,0b00100,0b01000,0b10000};
            case '[' -> new long[]{0b01110,0b01000,0b01000,0b01000,0b01000,0b01000,0b01110};
            case ']' -> new long[]{0b01110,0b00010,0b00010,0b00010,0b00010,0b00010,0b01110};
            case '/' -> new long[]{0b00001,0b00010,0b00100,0b01000,0b10000,0b00000,0b00000};
            case 'A' -> new long[]{0b01110,0b10001,0b10001,0b11111,0b10001,0b10001,0b10001};
            case 'B' -> new long[]{0b11110,0b10001,0b10001,0b11110,0b10001,0b10001,0b11110};
            case 'C' -> new long[]{0b01111,0b10000,0b10000,0b10000,0b10000,0b10000,0b01111};
            case 'D' -> new long[]{0b11110,0b10001,0b10001,0b10001,0b10001,0b10001,0b11110};
            case 'E' -> new long[]{0b11111,0b10000,0b10000,0b11110,0b10000,0b10000,0b11111};
            case 'F' -> new long[]{0b11111,0b10000,0b10000,0b11110,0b10000,0b10000,0b10000};
            case 'G' -> new long[]{0b01111,0b10000,0b10000,0b10111,0b10001,0b10001,0b01111};
            case 'H' -> new long[]{0b10001,0b10001,0b10001,0b11111,0b10001,0b10001,0b10001};
            case 'I' -> new long[]{0b11111,0b00100,0b00100,0b00100,0b00100,0b00100,0b11111};
            case 'J' -> new long[]{0b11111,0b00001,0b00001,0b00001,0b00001,0b10001,0b01110};
            case 'K' -> new long[]{0b10001,0b10010,0b10100,0b11000,0b10100,0b10010,0b10001};
            case 'L' -> new long[]{0b10000,0b10000,0b10000,0b10000,0b10000,0b10000,0b11111};
            case 'M' -> new long[]{0b10001,0b11011,0b10101,0b10001,0b10001,0b10001,0b10001};
            case 'N' -> new long[]{0b10001,0b11001,0b10101,0b10011,0b10001,0b10001,0b10001};
            case 'O' -> new long[]{0b01110,0b10001,0b10001,0b10001,0b10001,0b10001,0b01110};
            case 'P' -> new long[]{0b11110,0b10001,0b10001,0b11110,0b10000,0b10000,0b10000};
            case 'Q' -> new long[]{0b01110,0b10001,0b10001,0b10001,0b10101,0b10010,0b01101};
            case 'R' -> new long[]{0b11110,0b10001,0b10001,0b11110,0b10100,0b10010,0b10001};
            case 'S' -> new long[]{0b01111,0b10000,0b10000,0b01110,0b00001,0b00001,0b11110};
            case 'T' -> new long[]{0b11111,0b00100,0b00100,0b00100,0b00100,0b00100,0b00100};
            case 'U' -> new long[]{0b10001,0b10001,0b10001,0b10001,0b10001,0b10001,0b01110};
            case 'V' -> new long[]{0b10001,0b10001,0b10001,0b10001,0b10001,0b01010,0b00100};
            case 'W' -> new long[]{0b10001,0b10001,0b10101,0b10101,0b10101,0b11011,0b10001};
            case 'X' -> new long[]{0b10001,0b01010,0b00100,0b00100,0b00100,0b01010,0b10001};
            case 'Y' -> new long[]{0b10001,0b01010,0b00100,0b00100,0b00100,0b00100,0b00100};
            case 'Z' -> new long[]{0b11111,0b00001,0b00010,0b00100,0b01000,0b10000,0b11111};
            case '0' -> new long[]{0b01110,0b10001,0b10011,0b10101,0b11001,0b10001,0b01110};
            case '1' -> new long[]{0b00100,0b01100,0b00100,0b00100,0b00100,0b00100,0b01110};
            case '2' -> new long[]{0b01110,0b10001,0b00001,0b00010,0b00100,0b01000,0b11111};
            case '3' -> new long[]{0b01110,0b10001,0b00001,0b00110,0b00001,0b10001,0b01110};
            case '4' -> new long[]{0b00010,0b00110,0b01010,0b10010,0b11111,0b00010,0b00010};
            case '5' -> new long[]{0b11111,0b10000,0b11110,0b00001,0b00001,0b10001,0b01110};
            case '6' -> new long[]{0b00110,0b01000,0b10000,0b11110,0b10001,0b10001,0b01110};
            case '7' -> new long[]{0b11111,0b00001,0b00010,0b00100,0b01000,0b01000,0b01000};
            case '8' -> new long[]{0b01110,0b10001,0b10001,0b01110,0b10001,0b10001,0b01110};
            case '9' -> new long[]{0b01110,0b10001,0b10001,0b01111,0b00001,0b00010,0b01100};
            case '-' -> new long[]{0,0,0,0b11111,0,0,0};
            case '=' -> new long[]{0,0b11111,0,0,0b11111,0,0};
            default  -> null;
        };
    }

    private void setPixel(int x, int y, int col) {
        if (x >= 0 && x < GW && y >= 0 && y < GH) screenBuf[y * GW + x] = col;
    }

    private void fillRect(int x, int y, int w, int h, int col) {
        for (int dy = 0; dy < h; dy++)
            for (int dx = 0; dx < w; dx++)
                setPixel(x + dx, y + dy, col);
    }

    private void clampScroll() {
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        if (selectedIndex >= scrollOffset + ITEMS_PER_PAGE)
            scrollOffset = selectedIndex - ITEMS_PER_PAGE + 1;
    }
}
