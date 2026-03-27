package com.ab3d2.render;

import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Renderer 2D minimaliste : - Framebuffer interne 320x200 (texture RGBA) -
 * Upscale via un fullscreen quad en nearest-neighbor (pixel art fidèle) -
 * Permet aussi de rendre des sprites/UI directement via batch de quads
 *
 * Workflow : beginFrame() → drawTexture()/drawRect() → endFrame() → blit()
 */
public class Renderer2D {

    private static final Logger log = LoggerFactory.getLogger(Renderer2D.class);

    // Framebuffer interne
    private int fbo;
    private int fboTexture;
    private int rbo; // depth/stencil (optionnel, utile pour l'éditeur)

    // Fullscreen blit shader
    private int blitProgram;
    private int blitVao;

    // Sprite batch shader
    private int spriteProgram;
    private int spriteVao;
    private int spriteVbo;

    private final int gameWidth;
    private final int gameHeight;

    // Batch sprites (max 4096 quads par frame)
    private static final int MAX_QUADS = 4096;
    private static final int FLOATS_PER_VERT = 4; // x, y, u, v
    private static final int VERTS_PER_QUAD = 6; // 2 triangles
    private final float[] batchBuffer = new float[MAX_QUADS * VERTS_PER_QUAD * FLOATS_PER_VERT];
    private int batchCount = 0;
    private int currentTexture = -1;

    public Renderer2D(int gameWidth, int gameHeight) {
        this.gameWidth = gameWidth;
        this.gameHeight = gameHeight;
    }

    public void init() {
        createFramebuffer();
        createBlitShader();
        createSpriteShader();
        log.info("Renderer2D initialized ({}x{})", gameWidth, gameHeight);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Framebuffer interne
    // ──────────────────────────────────────────────────────────────────────────
    private void createFramebuffer() {
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        fboTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fboTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, gameWidth, gameHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTexture, 0);

        rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, gameWidth, gameHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer incomplete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Shader : blit framebuffer → écran (fullscreen quad)
    // ──────────────────────────────────────────────────────────────────────────
    private void createBlitShader() {
        String vert = """
            #version 330 core
            out vec2 vUV;
            void main() {
                // Triangle couvrant tout l'écran, 3 vertices hardcodés
                vec2 positions[3] = vec2[](
                    vec2(-1.0, -1.0), vec2( 3.0, -1.0), vec2(-1.0,  3.0)
                );
                vec2 uvs[3] = vec2[](
                    vec2(0.0, 0.0), vec2(2.0, 0.0), vec2(0.0, 2.0)
                );
                vUV = uvs[gl_VertexID];
                gl_Position = vec4(positions[gl_VertexID], 0.0, 1.0);
            }
            """;

        String frag = """
            #version 330 core
            in  vec2 vUV;
            out vec4 fragColor;
            uniform sampler2D uScreen;
            void main() {
                fragColor = texture(uScreen, vUV);
            }
            """;

        blitProgram = compileProgram(vert, frag);

        // VAO vide — on n'a pas besoin de VBO (les positions sont hardcodées dans le shader)
        blitVao = glGenVertexArrays();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Shader : sprite batch
    // ──────────────────────────────────────────────────────────────────────────
    private void createSpriteShader() {
        String vert = """
            #version 330 core
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aUV;
            out vec2 vUV;
            uniform vec2 uScreen; // taille de l'écran en pixels
            void main() {
                // Pixel coords → NDC
                vec2 ndc = (aPos / uScreen) * 2.0 - 1.0;
                ndc.y = -ndc.y; // Y flip
                gl_Position = vec4(ndc, 0.0, 1.0);
                vUV = aUV;
            }
            """;

        String frag = """
            #version 330 core
            in  vec2 vUV;
            out vec4 fragColor;
            uniform sampler2D uTex;
            void main() {
                vec4 c = texture(uTex, vUV);
                if (c.a < 0.01) discard; // alpha test (couleur 0 = transparent)
                fragColor = c;
            }
            """;

        spriteProgram = compileProgram(vert, frag);

        spriteVao = glGenVertexArrays();
        glBindVertexArray(spriteVao);

        spriteVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, spriteVbo);
        glBufferData(GL_ARRAY_BUFFER, (long) batchBuffer.length * Float.BYTES, GL_DYNAMIC_DRAW);

        // aPos (xy) + aUV (uv) = 4 floats par vertex
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);

        glBindVertexArray(0);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API publique
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * Début de frame : on rend dans le FBO interne.
     */
    public void beginFrame() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, gameWidth, gameHeight);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        batchCount = 0;
        currentTexture = -1;
    }

    /**
     * Draw une texture (sprite, background...) en pixel coords 320x200.
     *
     * @param texId texture OpenGL
     * @param x, y position destination (pixels)
     * @param w, h taille destination
     * @param u0,v0,u1,v1 UV source (normalisés 0..1)
     */
    public void drawTexture(int texId, float x, float y, float w, float h,
            float u0, float v0, float u1, float v1) {
        if (texId != currentTexture) {
            flushBatch();
            currentTexture = texId;
        }
        if (batchCount >= MAX_QUADS) {
            flushBatch();
        }

        int base = batchCount * VERTS_PER_QUAD * FLOATS_PER_VERT;
        // Triangle 1 : TL, BR, TR
        putVertex(base, x, y, u0, v0);
        putVertex(base + 4, x + w, y + h, u1, v1);
        putVertex(base + 8, x + w, y, u1, v0);
        // Triangle 2 : TL, BL, BR
        putVertex(base + 12, x, y, u0, v0);
        putVertex(base + 16, x, y + h, u0, v1);
        putVertex(base + 20, x + w, y + h, u1, v1);

        batchCount++;
    }

    /**
     * Raccourci : texture plein quad (UV 0..1).
     */
    public void drawTexture(int texId, float x, float y, float w, float h) {
        drawTexture(texId, x, y, w, h, 0, 0, 1, 1);
    }

    /**
     * Overlay de fondu noir (fade in/out).
     *
     * @param darkness 0.0 = transparent, 1.0 = noir opaque
     */
    public void drawFadeOverlay(float darkness) {
        if (darkness <= 0.001f) {
            return;
        }
        // Crée une texture 1x1 noire si nécessaire
        if (fadeTexture < 0) {
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocateDirect(4);
            buf.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0xFF);
            buf.flip();
            fadeTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, fadeTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
        // Hack alpha : on re-render avec alpha modifié via un quad plein écran
        // Pour l'instant simple approximation : darkness * black
        // (un vrai tint uniform viendra avec le refactor shader)
        if (darkness >= 0.99f) {
            drawTexture(fadeTexture, 0, 0, gameWidth, gameHeight);
        }
        // TODO: partial alpha via uniform color dans le sprite shader
    }
    private int fadeTexture = -1;

    /**
     * Flush le batch en cours et blit le FBO sur l'écran.
     */
    public void endFrame(int windowWidth, int windowHeight, float[] viewport) {
        flushBatch();

        // Blit FBO → back buffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport((int) viewport[0], (int) viewport[1], (int) viewport[2], (int) viewport[3]);

        glUseProgram(blitProgram);
        glBindTexture(GL_TEXTURE_2D, fboTexture);
        glUniform1i(glGetUniformLocation(blitProgram, "uScreen"), 0);

        glBindVertexArray(blitVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
    }

    private void flushBatch() {
        if (batchCount == 0 || currentTexture < 0) {
            return;
        }

        glUseProgram(spriteProgram);
        glUniform2f(glGetUniformLocation(spriteProgram, "uScreen"), gameWidth, gameHeight);
        glUniform1i(glGetUniformLocation(spriteProgram, "uTex"), 0);

        glBindTexture(GL_TEXTURE_2D, currentTexture);

        glBindVertexArray(spriteVao);
        glBindBuffer(GL_ARRAY_BUFFER, spriteVbo);

        int vertCount = batchCount * VERTS_PER_QUAD;
        try (MemoryStack stack = stackPush()) {
            FloatBuffer buf = stack.mallocFloat(vertCount * FLOATS_PER_VERT);
            buf.put(batchBuffer, 0, vertCount * FLOATS_PER_VERT);
            buf.flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        }

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_TRIANGLES, 0, vertCount);
        glDisable(GL_BLEND);

        glBindVertexArray(0);
        batchCount = 0;
    }

    private void putVertex(int base, float x, float y, float u, float v) {
        batchBuffer[base] = x;
        batchBuffer[base + 1] = y;
        batchBuffer[base + 2] = u;
        batchBuffer[base + 3] = v;
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
        if (fadeTexture >= 0) {
            glDeleteTextures(fadeTexture);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utilitaires shader
    // ──────────────────────────────────────────────────────────────────────────
    private int compileProgram(String vertSrc, String fragSrc) {
        int vert = compileShader(GL_VERTEX_SHADER, vertSrc);
        int frag = compileShader(GL_FRAGMENT_SHADER, fragSrc);

        int prog = glCreateProgram();
        glAttachShader(prog, vert);
        glAttachShader(prog, frag);
        glLinkProgram(prog);

        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader link error: " + glGetProgramInfoLog(prog));
        }

        glDeleteShader(vert);
        glDeleteShader(frag);
        return prog;
    }

    private int compileShader(int type, String src) {
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public int getGameWidth() {
        return gameWidth;
    }

    public int getGameHeight() {
        return gameHeight;
    }
}
