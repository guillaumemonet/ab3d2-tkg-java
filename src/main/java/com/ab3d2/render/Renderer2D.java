package com.ab3d2.render;

import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Renderer 2D :
 *  - FBO interne 320x200 (pixel art natif)
 *  - Upscale nearest-neighbor vers la fenêtre (letterbox)
 *  - Sprite batch (positions en pixel coords 320x200)
 *  - Support blend normal + additif
 *  - Fade overlay via uniform alpha
 */
public class Renderer2D {

    private static final Logger log = LoggerFactory.getLogger(Renderer2D.class);

    // ── Constantes batch ──────────────────────────────────────────────────────
    private static final int MAX_QUADS       = 4096;
    private static final int FLOATS_PER_VERT = 4;   // x, y, u, v
    private static final int VERTS_PER_QUAD  = 6;   // 2 triangles

    // ── GL objects ────────────────────────────────────────────────────────────
    private int fbo, fboTexture, rbo;
    private int blitProgram, blitVao;
    private int spriteProgram, spriteVao, spriteVbo;
    private int uScreenLoc, uTexLoc, uAlphaLoc, uBlendModeLoc;

    // ── Batch ─────────────────────────────────────────────────────────────────
    private final float[] batchBuffer    = new float[MAX_QUADS * VERTS_PER_QUAD * FLOATS_PER_VERT];
    private int   batchCount             = 0;
    private int   currentTexture         = -1;
    private int   currentBlendMode       = 0; // 0=normal, 1=additif

    // ── Textures utilitaires ─────────────────────────────────────────────────
    private int fadeTexture = -1;   // 1x1 noir opaque

    private final int gameWidth, gameHeight;

    public Renderer2D(int gameWidth, int gameHeight) {
        this.gameWidth  = gameWidth;
        this.gameHeight = gameHeight;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Init
    // ═════════════════════════════════════════════════════════════════════════

    public void init() {
        createFBO();
        createBlitShader();
        createSpriteShader();
        createUtilityTextures();
        log.info("Renderer2D initialized ({}x{})", gameWidth, gameHeight);
    }

    private void createFBO() {
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        fboTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fboTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, gameWidth, gameHeight,
                     0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                               GL_TEXTURE_2D, fboTexture, 0);

        rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, gameWidth, gameHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT,
                                  GL_RENDERBUFFER, rbo);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("FBO incomplete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void createBlitShader() {
        // Triangle plein écran hardcodé dans le vertex shader
        String vert = """
            #version 330 core
            out vec2 vUV;
            void main() {
                const vec2 pos[3] = vec2[](vec2(-1,-1), vec2(3,-1), vec2(-1,3));
                const vec2 uvs[3] = vec2[](vec2(0,0),   vec2(2,0),  vec2(0,2));
                vUV         = uvs[gl_VertexID];
                gl_Position = vec4(pos[gl_VertexID], 0.0, 1.0);
            }
            """;
        String frag = """
            #version 330 core
            in  vec2 vUV;
            out vec4 fragColor;
            uniform sampler2D uScreen;
            void main() { fragColor = texture(uScreen, vUV); }
            """;
        blitProgram = compileProgram(vert, frag);
        blitVao     = glGenVertexArrays();
    }

    private void createSpriteShader() {
        // uBlendMode : 0=normal alpha, 1=additif, 2=fade (alpha uniforme)
        String vert = """
            #version 330 core
            layout(location=0) in vec2 aPos;
            layout(location=1) in vec2 aUV;
            out vec2 vUV;
            uniform vec2 uScreen;
            void main() {
                vec2 ndc = (aPos / uScreen) * 2.0 - 1.0;
                ndc.y    = -ndc.y;
                gl_Position = vec4(ndc, 0.0, 1.0);
                vUV = aUV;
            }
            """;
        String frag = """
            #version 330 core
            in  vec2 vUV;
            out vec4 fragColor;
            uniform sampler2D uTex;
            uniform float     uAlpha;
            uniform int       uBlendMode; // 0=normal, 1=additif, 2=fade
            void main() {
                vec4 c = texture(uTex, vUV);
                if (uBlendMode == 2) {
                    // fade overlay : couleur fixe noire * alpha
                    fragColor = vec4(0.0, 0.0, 0.0, uAlpha);
                } else {
                    if (c.a < 0.01) discard;
                    fragColor = vec4(c.rgb, c.a * uAlpha);
                }
            }
            """;
        spriteProgram = compileProgram(vert, frag);

        // Récupère les uniform locations une seule fois
        uScreenLoc    = glGetUniformLocation(spriteProgram, "uScreen");
        uTexLoc       = glGetUniformLocation(spriteProgram, "uTex");
        uAlphaLoc     = glGetUniformLocation(spriteProgram, "uAlpha");
        uBlendModeLoc = glGetUniformLocation(spriteProgram, "uBlendMode");

        spriteVao = glGenVertexArrays();
        glBindVertexArray(spriteVao);
        spriteVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, spriteVbo);
        glBufferData(GL_ARRAY_BUFFER, (long) batchBuffer.length * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void createUtilityTextures() {
        // Texture 1x1 blanche pour les overlays
        ByteBuffer white = ByteBuffer.allocateDirect(4);
        white.put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).put((byte)0xFF).flip();
        fadeTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fadeTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, white);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // API publique
    // ═════════════════════════════════════════════════════════════════════════

    /** Début de frame : bind le FBO, clear. */
    public void beginFrame() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, gameWidth, gameHeight);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        batchCount      = 0;
        currentTexture  = -1;
        currentBlendMode = 0;
    }

    /** Draw texture, blend normal. */
    public void drawTexture(int texId, float x, float y, float w, float h) {
        drawTexture(texId, x, y, w, h, 0f, 0f, 1f, 1f);
    }

    /** Draw texture avec UV custom, blend normal. */
    public void drawTexture(int texId, float x, float y, float w, float h,
                            float u0, float v0, float u1, float v1) {
        if (texId != currentTexture || currentBlendMode != 0) {
            flushBatch();
            currentTexture   = texId;
            currentBlendMode = 0;
        }
        if (batchCount >= MAX_QUADS) flushBatch();
        pushQuad(x, y, w, h, u0, v0, u1, v1);
    }

    /** Draw texture en blend additif (feu par-dessus le texte). */
    public void drawTextureAdditive(int texId, float x, float y, float w, float h) {
        if (texId != currentTexture || currentBlendMode != 1) {
            flushBatch();
            currentTexture   = texId;
            currentBlendMode = 1;
        }
        if (batchCount >= MAX_QUADS) flushBatch();
        pushQuad(x, y, w, h, 0f, 0f, 1f, 1f);
    }

    /**
     * Overlay fade noir.
     * @param darkness 0.0=transparent, 1.0=noir opaque
     */
    public void drawFadeOverlay(float darkness) {
        if (darkness <= 0.001f) return;
        flushBatch();
        // Mode 2 = fade dans le shader (ignore texture RGB, applique darkness comme alpha)
        glUseProgram(spriteProgram);
        glUniform2f(uScreenLoc, gameWidth, gameHeight);
        glUniform1i(uTexLoc, 0);
        glUniform1f(uAlphaLoc, darkness);
        glUniform1i(uBlendModeLoc, 2);

        // Un quad plein écran avec la texture blanche
        float[] quad = {
            0f,         0f,          0f, 0f,
            gameWidth,  gameHeight,  1f, 1f,
            gameWidth,  0f,          1f, 0f,
            0f,         0f,          0f, 0f,
            0f,         gameHeight,  0f, 1f,
            gameWidth,  gameHeight,  1f, 1f,
        };

        glBindTexture(GL_TEXTURE_2D, fadeTexture);
        glBindVertexArray(spriteVao);
        glBindBuffer(GL_ARRAY_BUFFER, spriteVbo);
        try (MemoryStack stack = stackPush()) {
            FloatBuffer buf = stack.mallocFloat(quad.length);
            buf.put(quad).flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        }
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glDisable(GL_BLEND);
        glBindVertexArray(0);
    }

    /**
     * Fin de frame : flush batch, blit FBO → back buffer.
     */
    public void endFrame(int winW, int winH, float[] viewport) {
        flushBatch();

        // Blit FBO → back buffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        glViewport((int)viewport[0], (int)viewport[1], (int)viewport[2], (int)viewport[3]);

        glUseProgram(blitProgram);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, fboTexture);
        glUniform1i(glGetUniformLocation(blitProgram, "uScreen"), 0);
        glBindVertexArray(blitVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Internals
    // ═════════════════════════════════════════════════════════════════════════

    private void pushQuad(float x, float y, float w, float h,
                          float u0, float v0, float u1, float v1) {
        int b = batchCount * VERTS_PER_QUAD * FLOATS_PER_VERT;
        // Triangle 1
        putV(b,      x,     y,     u0, v0);
        putV(b +  4, x + w, y + h, u1, v1);
        putV(b +  8, x + w, y,     u1, v0);
        // Triangle 2
        putV(b + 12, x,     y,     u0, v0);
        putV(b + 16, x,     y + h, u0, v1);
        putV(b + 20, x + w, y + h, u1, v1);
        batchCount++;
    }

    private void putV(int b, float x, float y, float u, float v) {
        batchBuffer[b]     = x;
        batchBuffer[b + 1] = y;
        batchBuffer[b + 2] = u;
        batchBuffer[b + 3] = v;
    }

    private void flushBatch() {
        if (batchCount == 0 || currentTexture < 0) {
            batchCount = 0;
            return;
        }

        glUseProgram(spriteProgram);
        glUniform2f(uScreenLoc, gameWidth, gameHeight);
        glUniform1i(uTexLoc, 0);
        glUniform1f(uAlphaLoc, 1.0f);
        glUniform1i(uBlendModeLoc, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, currentTexture);

        glBindVertexArray(spriteVao);
        glBindBuffer(GL_ARRAY_BUFFER, spriteVbo);

        int vertCount = batchCount * VERTS_PER_QUAD;
        try (MemoryStack stack = stackPush()) {
            FloatBuffer buf = stack.mallocFloat(vertCount * FLOATS_PER_VERT);
            buf.put(batchBuffer, 0, vertCount * FLOATS_PER_VERT).flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        }

        glEnable(GL_BLEND);
        if (currentBlendMode == 1) {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);          // additif
        } else {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // normal
        }
        glDrawArrays(GL_TRIANGLES, 0, vertCount);
        glDisable(GL_BLEND);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        batchCount   = 0;
        currentTexture = -1;
    }

    public void destroy() {
        glDeleteFramebuffers(fbo);
        glDeleteTextures(fboTexture);
        glDeleteRenderbuffers(rbo);
        glDeleteProgram(blitProgram);
        glDeleteProgram(spriteProgram);
        glDeleteVertexArrays(blitVao);
        glDeleteVertexArrays(spriteVao);
        glDeleteBuffers(spriteVbo);
        if (fadeTexture >= 0) glDeleteTextures(fadeTexture);
    }

    // ── Shader compilation ────────────────────────────────────────────────────

    private int compileProgram(String vertSrc, String fragSrc) {
        int v = compileShader(GL_VERTEX_SHADER,   vertSrc);
        int f = compileShader(GL_FRAGMENT_SHADER, fragSrc);
        int p = glCreateProgram();
        glAttachShader(p, v); glAttachShader(p, f);
        glLinkProgram(p);
        if (glGetProgrami(p, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Link error: " + glGetProgramInfoLog(p));
        glDeleteShader(v); glDeleteShader(f);
        return p;
    }

    private int compileShader(int type, String src) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader error: " + glGetShaderInfoLog(s));
        return s;
    }

    public int getGameWidth()  { return gameWidth; }
    public int getGameHeight() { return gameHeight; }
}
