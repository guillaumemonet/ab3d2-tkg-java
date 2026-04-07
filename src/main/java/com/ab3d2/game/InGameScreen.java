package com.ab3d2.game;

import com.ab3d2.assets.WallTextureManager;
import com.ab3d2.core.*;
import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;
import com.ab3d2.render.Camera;
import com.ab3d2.render.TexturedRenderer3D;
import com.ab3d2.render.WireRenderer3D;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL33.*;

/**
 * Ecran de jeu principal — renderer 3D texturé.
 *
 * <h2>Controles</h2>
 * <pre>
 * W/Z     — avancer          S     — reculer
 * A/Q     — tourner gauche   D     — tourner droite
 * E       — strafe droite
 * Shift   — sprint
 * ESC     — retour menu
 * M/Tab   — basculer top-down/3D
 * F1      — basculer wireframe/texturé
 * </pre>
 */
public class InGameScreen implements Screen {

    private static final Logger log = LoggerFactory.getLogger(InGameScreen.class);

    private static final int GW3D = Camera.SCREEN_W;    // 192
    private static final int GH3D = Camera.SCREEN_H;    // 160
    private static final int GW   = Window.GAME_WIDTH;  // 320
    private static final int GH   = Window.GAME_HEIGHT; // 200

    private static final float MOVE_SPEED  = 64.0f;
    private static final float SPRINT_MULT = 3.0f;
    private static final int   TURN_SPEED  = 32;

    // ── Etat ─────────────────────────────────────────────────────────────────
    private final int           levelIndex;
    private LevelData           level;
    private WallRenderEntry[][] zoneEntries;
    private PlayerState         player;

    // ── Renderers ────────────────────────────────────────────────────────────
    private TexturedRenderer3D renderer3D;
    private WireRenderer3D     rendererWire;
    private TopDownRenderer    topDown;
    private boolean            showTopDown   = false;
    private boolean            showWireframe = false;  // F1 pour debug

    // ── GPU ───────────────────────────────────────────────────────────────────
    private int tex3DId  = -1;
    private int texTopId = -1;

    public InGameScreen(int levelIndex) { this.levelIndex = levelIndex; }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    @Override
    public void init(GameContext ctx) {
        log.info("InGameScreen init — level index {}", levelIndex);

        // Tables maths
        if (!Tables.isInitialized()) {
            try { Tables.initFromClasspath(); }
            catch (Exception e) {
                log.warn("bigsine absent, tables synthetiques");
                Tables.initFromBytes(new byte[0]);
            }
        }

        // Chargement du niveau
        String letter = String.valueOf((char)('A' + levelIndex));
        try {
            level = new com.ab3d2.LevelManager(ctx.assets().getRoot()).load(letter);
            log.info("Niveau {} : {}", letter, level);
        } catch (Exception e) {
            log.error("Impossible de charger le niveau {} : {}", letter, e.getMessage());
            level = null;
        }

        // ZoneGraphAdds (murs texturés)
        if (level != null) {
            try {
                Path graphPath = ctx.assets().getRoot()
                    .resolve("levels/LEVEL_" + letter + "/twolev.graph.bin");
                byte[] graphRaw = Files.readAllBytes(graphPath);
                ByteBuffer gBuf = ByteBuffer.wrap(graphRaw).order(ByteOrder.BIG_ENDIAN);
                gBuf.getInt(); gBuf.getInt(); gBuf.getInt();
                int zgaOffset = gBuf.getInt();
                zoneEntries = new ZoneGraphParser().parse(graphRaw, level.numZones(), zgaOffset);
                int totalWalls = 0;
                for (WallRenderEntry[] ze : zoneEntries)
                    for (WallRenderEntry e : ze) if (e.isWall()) totalWalls++;
                log.info("ZoneGraphAdds : {} murs", totalWalls);
            } catch (Exception e) {
                log.error("ZoneGraphParser erreur : {}", e.getMessage());
                zoneEntries = new WallRenderEntry[level.numZones()][];
                for (int i = 0; i < zoneEntries.length; i++)
                    zoneEntries[i] = new WallRenderEntry[0];
            }
        }

        // Positionner le joueur
        if (level != null) {
            ZoneData startZone = level.zone(level.plr1StartZoneId);
            float eyeH = (startZone != null)
                ? startZone.floorHeight() - Camera.PLR_EYE_ABOVE_FLOOR
                : -Camera.PLR_EYE_ABOVE_FLOOR;
            player = new PlayerState(level.plr1StartX, level.plr1StartZ, 0,
                                     (short) level.plr1StartZoneId);
            player.yOff = (int)(eyeH * 256);
            log.info("Joueur : ({},{}) zone={} eyeH={}",
                level.plr1StartX, level.plr1StartZ, level.plr1StartZoneId, eyeH);
        } else {
            player = new PlayerState();
        }

        // Textures murs (palette deja chargee depuis 256pal.bin par AssetManager)
        WallTextureManager texMgr = new WallTextureManager();
        try {
            Path wallsDir = ctx.assets().getRoot().resolve("walls");
            texMgr.loadAll(wallsDir, ctx.assets().getPalette());
            log.info(texMgr.dump());
        } catch (Exception e) {
            log.error("Erreur chargement textures murs : {}", e.getMessage());
        }

        // Renderers
        renderer3D   = new TexturedRenderer3D(GW3D, GH3D, texMgr);
        rendererWire = new WireRenderer3D(GW3D, GH3D);
        topDown      = new TopDownRenderer(GW, GH);

        // Premiere frame
        renderFrame();
        tex3DId  = ctx.assets().createTextureFromARGB(upscale3D(currentPixels()), GW, GH);
        topDown.update(level, player);
        texTopId = ctx.assets().createTextureFromARGB(topDown.getPixels(), GW, GH);
    }

    @Override
    public GameState update(GameContext ctx, double dt) {
        InputHandler in = ctx.input();

        if (in.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) return new GameState.MainMenu();
        if (in.isKeyPressed(GLFW.GLFW_KEY_TAB))    showTopDown   = !showTopDown;
        if (in.isKeyPressed(GLFW.GLFW_KEY_M))       showTopDown   = !showTopDown;
        if (in.isKeyPressed(GLFW.GLFW_KEY_F1))      showWireframe = !showWireframe;

        if (level == null) return null;

        float speed = (float)(MOVE_SPEED * dt);
        if (in.isKeyHeld(GLFW.GLFW_KEY_LEFT_SHIFT) || in.isKeyHeld(GLFW.GLFW_KEY_RIGHT_SHIFT))
            speed *= SPRINT_MULT;

        if (in.isKeyHeld(GLFW.GLFW_KEY_W) || in.isKeyHeld(GLFW.GLFW_KEY_Z))
            player.moveForward(speed);
        if (in.isKeyHeld(GLFW.GLFW_KEY_S))
            player.moveForward(-speed);
        if (in.isKeyHeld(GLFW.GLFW_KEY_Q))
            player.moveStrafe(-speed);
        if (in.isKeyHeld(GLFW.GLFW_KEY_E))
            player.moveStrafe(speed);

        if (in.isKeyHeld(GLFW.GLFW_KEY_A))
            player.rotate(-TURN_SPEED);
        if (in.isKeyHeld(GLFW.GLFW_KEY_D))
            player.rotate(TURN_SPEED);

        double mdx = in.getMouseDX();
        if (Math.abs(mdx) > 0.5) player.rotate((int)(mdx * 2.0));

        // Zone traversal
        int newZone = ZoneTraversal.updateZone(level,
            player.currentZoneId & 0xFFFF,
            player.worldX(), player.worldZ());
        if (newZone != (player.currentZoneId & 0xFFFF)) {
            log.debug("Zone : {} -> {}", player.currentZoneId & 0xFFFF, newZone);
            player.currentZoneId = (short) newZone;
            ZoneData zd = level.zone(newZone);
            if (zd != null)
                player.yOff = (int)((zd.floorHeight() - Camera.PLR_EYE_ABOVE_FLOOR) * 256);
        }

        return null;
    }

    @Override
    public void render(GameContext ctx, double alpha) {
        renderFrame();
        uploadAndDraw(ctx);
    }

    @Override
    public void destroy(GameContext ctx) {
        if (tex3DId  >= 0) { glDeleteTextures(tex3DId);  tex3DId  = -1; }
        if (texTopId >= 0) { glDeleteTextures(texTopId); texTopId = -1; }
        log.info("InGameScreen destroyed");
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    private void renderFrame() {
        if (level == null) return;
        float eyeH = (float) player.yOff / 256.0f;
        Camera cam = new Camera(player.worldX(), player.worldZ(), eyeH, player.angle);

        if (showTopDown) {
            topDown.update(level, player);
        } else if (showWireframe) {
            rendererWire.render(level, cam, player.currentZoneId & 0xFFFF);
        } else {
            renderer3D.render(level, zoneEntries, cam, player.currentZoneId & 0xFFFF);
        }
    }

    private int[] currentPixels() {
        if (showWireframe) return rendererWire.getPixels();
        return renderer3D.getPixels();
    }

    private void uploadAndDraw(GameContext ctx) {
        var r = ctx.renderer();
        r.beginFrame();
        if (showTopDown) {
            uploadPixels(texTopId, topDown.getPixels(), GW, GH);
            r.drawTexture(texTopId, 0, 0, GW, GH);
        } else {
            uploadPixels(tex3DId, upscale3D(currentPixels()), GW, GH);
            r.drawTexture(tex3DId, 0, 0, GW, GH);
        }
        r.endFrame(ctx.window().getWidth(), ctx.window().getHeight(),
                   ctx.window().getViewportRect());
    }

    // ── Upscale 192x160 → 320x200 ────────────────────────────────────────────

    private int[] upscale3D(int[] src) {
        int[] dst = new int[GW * GH];
        for (int dy = 0; dy < GH; dy++) {
            int sy = Math.min(GH3D - 1, dy * GH3D / GH);
            for (int dx = 0; dx < GW; dx++) {
                int sx = Math.min(GW3D - 1, dx * GW3D / GW);
                dst[dy * GW + dx] = src[sy * GW3D + sx];
            }
        }
        return dst;
    }

    private void uploadPixels(int texId, int[] pixels, int w, int h) {
        if (texId < 0) return;
        glBindTexture(GL_TEXTURE_2D, texId);
        var buf = ByteBuffer.allocateDirect(w * h * 4);
        for (int px : pixels) {
            buf.put((byte)((px >> 16) & 0xFF));
            buf.put((byte)((px >>  8) & 0xFF));
            buf.put((byte)( px        & 0xFF));
            buf.put((byte)((px >> 24) & 0xFF));
        }
        buf.flip();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
