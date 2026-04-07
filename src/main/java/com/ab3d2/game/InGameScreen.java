package com.ab3d2.game;

import com.ab3d2.core.*;
import com.ab3d2.core.level.*;
import com.ab3d2.core.math.Tables;
import com.ab3d2.render.Camera;
import com.ab3d2.render.WireRenderer3D;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL33.*;

/**
 * Ecran de jeu principal — renderer 3D filaire (etape 1).
 *
 * <h2>Phase actuelle</h2>
 * Renderer 3D filaire (WireRenderer3D) : murs colores par zone,
 * projection perspective, zone traversal.
 *
 * <h2>Controles</h2>
 * <pre>
 * W/Z     — avancer          S     — reculer
 * A/Q     — tourner gauche   D     — tourner droite
 * Shift   — sprint           Tab   — debug toggle
 * ESC     — retour menu      M     — basculer top-down/3D
 * </pre>
 */
public class InGameScreen implements Screen {

    private static final Logger log = LoggerFactory.getLogger(InGameScreen.class);

    // Ecran de jeu 3D (192x160) upscale vers 320x200
    private static final int GW3D = Camera.SCREEN_W;  // 192
    private static final int GH3D = Camera.SCREEN_H;  // 160
    private static final int GW   = Window.GAME_WIDTH;  // 320
    private static final int GH   = Window.GAME_HEIGHT; // 200

    private static final float MOVE_SPEED  = 64.0f;
    private static final float SPRINT_MULT = 3.0f;
    private static final int   TURN_SPEED  = 32;

    // ── Etat ─────────────────────────────────────────────────────────────────
    private final int   levelIndex;
    private LevelData   level;
    private PlayerState player;

    // ── Renderer ─────────────────────────────────────────────────────────────
    private WireRenderer3D renderer3D;
    private TopDownRenderer topDown;
    private boolean         showTopDown = false;   // M pour basculer

    // ── GPU ───────────────────────────────────────────────────────────────────
    private int tex3DId   = -1;
    private int texTopId  = -1;

    // ── Debug ─────────────────────────────────────────────────────────────────
    private boolean showDebug = false;

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

        // Positionner le joueur sur les coords de depart TLBT
        if (level != null) {
            ZoneData startZone = level.zone(level.plr1StartZoneId);
            float eyeH = (startZone != null)
                ? startZone.floorHeight() - Camera.PLR_EYE_ABOVE_FLOOR
                : -Camera.PLR_EYE_ABOVE_FLOOR;

            player = new PlayerState(level.plr1StartX, level.plr1StartZ, 0,
                                     (short) level.plr1StartZoneId);
            player.yOff = (int)(eyeH * 256);  // stocker eyeH en virgule fixe

            log.info("Joueur : ({},{}) zone={} eyeH={}", 
                level.plr1StartX, level.plr1StartZ, level.plr1StartZoneId, eyeH);
        } else {
            player = new PlayerState();
        }

        // Renderers
        renderer3D = new WireRenderer3D(GW3D, GH3D);
        topDown     = new TopDownRenderer(GW, GH);

        // Premiere frame
        renderFrame();
        tex3DId  = ctx.assets().createTextureFromARGB(
            upscale3D(renderer3D.getPixels()), GW, GH);
        topDown.update(level, player);
        texTopId = ctx.assets().createTextureFromARGB(topDown.getPixels(), GW, GH);
    }

    @Override
    public GameState update(GameContext ctx, double dt) {
        InputHandler in = ctx.input();

        if (in.isKeyPressed(GLFW.GLFW_KEY_ESCAPE))    return new GameState.MainMenu();
        if (in.isKeyPressed(GLFW.GLFW_KEY_TAB))       showDebug  = !showDebug;
        if (in.isKeyPressed(GLFW.GLFW_KEY_M))         showTopDown = !showTopDown;

        if (level == null) return null;

        // Vitesse de deplacement
        float speed = (float)(MOVE_SPEED * dt);
        if (in.isKeyHeld(GLFW.GLFW_KEY_LEFT_SHIFT) || in.isKeyHeld(GLFW.GLFW_KEY_RIGHT_SHIFT))
            speed *= SPRINT_MULT;

        // Sauvegarder position avant mouvement (pour zone traversal)
        int oldZone = player.currentZoneId;

        // Mouvement
        if (in.isKeyHeld(GLFW.GLFW_KEY_W) || in.isKeyHeld(GLFW.GLFW_KEY_Z))
            player.moveForward(speed);
        if (in.isKeyHeld(GLFW.GLFW_KEY_S))
            player.moveForward(-speed);
        if (in.isKeyHeld(GLFW.GLFW_KEY_Q))
            player.moveStrafe(-speed);
        if (in.isKeyHeld(GLFW.GLFW_KEY_E))
            player.moveStrafe(speed);

        // Rotation clavier
        if (in.isKeyHeld(GLFW.GLFW_KEY_A))
            player.rotate(-TURN_SPEED);
        if (in.isKeyHeld(GLFW.GLFW_KEY_D))
            player.rotate(TURN_SPEED);

        // Rotation souris
        double mdx = in.getMouseDX();
        if (Math.abs(mdx) > 0.5) player.rotate((int)(mdx * 2.0));

        // Zone traversal : mettre a jour la zone apres le mouvement
        int newZone = ZoneTraversal.updateZone(level,
            player.currentZoneId & 0xFFFF,
            player.worldX(), player.worldZ());
        if (newZone != (player.currentZoneId & 0xFFFF)) {
            log.debug("Zone traversal : {} -> {}", player.currentZoneId & 0xFFFF, newZone);
            player.currentZoneId = (short) newZone;

            // Mettre a jour la hauteur des yeux selon la nouvelle zone
            ZoneData newZoneData = level.zone(newZone);
            if (newZoneData != null) {
                float newEyeH = newZoneData.floorHeight() - Camera.PLR_EYE_ABOVE_FLOOR;
                player.yOff = (int)(newEyeH * 256);
            }
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

        // Calculer la hauteur des yeux courante
        float eyeH = (float) player.yOff / 256.0f;

        if (showTopDown) {
            topDown.update(level, player);
        } else {
            Camera cam = new Camera(
                player.worldX(), player.worldZ(),
                eyeH,
                player.angle);
            renderer3D.render(level, cam, player.currentZoneId & 0xFFFF);
        }
    }

    private void uploadAndDraw(GameContext ctx) {
        var r = ctx.renderer();
        r.beginFrame();

        if (showTopDown) {
            // Mettre a jour la texture top-down
            uploadPixels(texTopId, topDown.getPixels(), GW, GH);
            r.drawTexture(texTopId, 0, 0, GW, GH);
        } else {
            // Upscale 192x160 → 320x200 et upload
            int[] upscaled = upscale3D(renderer3D.getPixels());
            uploadPixels(tex3DId, upscaled, GW, GH);
            r.drawTexture(tex3DId, 0, 0, GW, GH);
        }

        r.endFrame(ctx.window().getWidth(), ctx.window().getHeight(),
                   ctx.window().getViewportRect());
    }

    // ── Upscale 192x160 → 320x200 ─────────────────────────────────────────────

    /**
     * Upscale bilineaire de 192x160 vers 320x200.
     * Rapport : X * 320/192 = X * 5/3, Y * 200/160 = Y * 5/4.
     */
    private int[] upscale3D(int[] src) {
        int[] dst = new int[GW * GH];
        for (int dy = 0; dy < GH; dy++) {
            // Source Y : dy * 160 / 200 = dy * 4/5
            int sy = Math.min(GH3D - 1, dy * GH3D / GH);
            for (int dx = 0; dx < GW; dx++) {
                // Source X : dx * 192 / 320 = dx * 3/5
                int sx = Math.min(GW3D - 1, dx * GW3D / GW);
                dst[dy * GW + dx] = src[sy * GW3D + sx];
            }
        }
        return dst;
    }

    /** Upload des pixels dans une texture OpenGL existante. */
    private void uploadPixels(int texId, int[] pixels, int w, int h) {
        if (texId < 0) return;
        glBindTexture(GL_TEXTURE_2D, texId);
        var buf = java.nio.ByteBuffer.allocateDirect(w * h * 4);
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
