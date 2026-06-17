package ab3d2.host;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Couche hôte : fenêtre + présentation via LWJGL 3 (GLFW + OpenGL).
 *
 * Le moteur rend dans un buffer CHUNKY 320x256 (1 octet d'index palette par
 * pixel) dans Mem.RAM (Vid_FastBufferPtr_l). present() reçoit l'image déjà
 * convertie en ARGB (index → palette, fait par ScreenC.Vid_Present) et l'envoie
 * à une texture OpenGL plein écran.
 *
 * OpenGL « legacy » (GL11) suffit ici : c'est juste un quad texturé filtré
 * NEAREST pour respecter l'esthétique pixel d'origine, mis à l'échelle.
 */
public final class Display {

    private final int srcWidth;
    private final int srcHeight;

    private long window;
    private int texture;
    private ByteBuffer pixels;  // octets R,G,B,A → uploadés en GL_RGBA/UNSIGNED_BYTE

    public Display(int srcWidth, int srcHeight) {
        this.srcWidth = srcWidth;
        this.srcHeight = srcHeight;
    }

    /** Ouvre la fenêtre (taille = source * scale) et initialise le contexte GL. */
    public void open(String title, int scale) {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW: échec de l'initialisation");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // Contexte de compatibilité (pipeline fixe GL11).
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);

        int winW = srcWidth * scale;
        int winH = srcHeight * scale;
        window = glfwCreateWindow(winW, winH, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("GLFW: échec de création de la fenêtre");
        }

        // Centrer la fenêtre.
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(window, (vidmode.width() - winW) / 2, (vidmode.height() - winH) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // vsync
        glfwShowWindow(window);

        Input.install(window);  // clavier GLFW → KeyMap_vb (key_interrupt hôte)

        GL.createCapabilities();

        // Texture cible.
        glEnable(GL_TEXTURE_2D);
        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        // Allocation initiale (contenu indéfini, rempli par present()).
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, srcWidth, srcHeight, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        pixels = memAlloc(srcWidth * srcHeight * 4);

        glClearColor(0f, 0f, 0f, 1f);
    }

    /**
     * Présente une image ARGB (srcWidth*srcHeight, row-major, 0xAARRGGBB).
     * Met à jour la texture et échange les tampons.
     */
    public void present(int[] argb) {
        pixels.clear();
        final int n = srcWidth * srcHeight;
        for (int i = 0; i < n; i++) {
            int c = argb[i];
            pixels.put((byte) (c >> 16)); // R
            pixels.put((byte) (c >> 8));  // G
            pixels.put((byte) c);         // B
            pixels.put((byte) 0xFF);      // A
        }
        pixels.flip();

        glBindTexture(GL_TEXTURE_2D, texture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, srcWidth, srcHeight,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        // Viewport = taille réelle du framebuffer (gère le hiDPI).
        int[] fbW = new int[1];
        int[] fbH = new int[1];
        glfwGetFramebufferSize(window, fbW, fbH);
        glViewport(0, 0, fbW[0], fbH[0]);

        glClear(GL_COLOR_BUFFER_BIT);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0, 1.0, 1.0, 0.0, -1.0, 1.0); // origine en haut-gauche
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glBindTexture(GL_TEXTURE_2D, texture);
        glBegin(GL_QUADS);
        glTexCoord2f(0f, 0f); glVertex2f(0f, 0f);
        glTexCoord2f(1f, 0f); glVertex2f(1f, 0f);
        glTexCoord2f(1f, 1f); glVertex2f(1f, 1f);
        glTexCoord2f(0f, 1f); glVertex2f(0f, 1f);
        glEnd();

        glfwSwapBuffers(window);
    }

    /** Pompe les événements fenêtre/entrée (callbacks GLFW). */
    public void pollEvents() {
        glfwPollEvents();
    }

    /** Vrai si l'utilisateur a demandé la fermeture (croix fenêtre). */
    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    public long handle() {
        return window;
    }

    public void close() {
        if (pixels != null) {
            memFree(pixels);
            pixels = null;
        }
        if (window != NULL) {
            glfwDestroyWindow(window);
            window = NULL;
        }
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
    }
}
