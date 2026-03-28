package com.ab3d2.core;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Gestion de la fenêtre GLFW et du contexte OpenGL.
 * Résolution cible : 320x200 (native AB3D2) upscalée.
 */
public class Window {

    private static final Logger log = LoggerFactory.getLogger(Window.class);

    // Résolution interne du jeu (Amiga OCS/AGA 320x200/256)
    public static final int GAME_WIDTH  = 320;
    public static final int GAME_HEIGHT = 200;

    // Résolution de la fenêtre (upscale x3 par défaut)
    private int windowWidth;
    private int windowHeight;
    private final String title;
    private long handle;
    private boolean vsync;
    private int scale;

    private InputHandler inputHandler;

    public Window(String title, int scale, boolean vsync) {
        this.title   = title;
        this.scale   = scale;
        this.vsync   = vsync;
        this.windowWidth  = GAME_WIDTH  * scale;
        this.windowHeight = GAME_HEIGHT * scale;
    }

    public void init() {
        // Init GLFW
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE,   GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // macOS

        handle = glfwCreateWindow(windowWidth, windowHeight, title, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Input handler
        inputHandler = new InputHandler(handle);

        // Centre la fenêtre
        try (MemoryStack stack = stackPush()) {
            IntBuffer pw = stack.mallocInt(1);
            IntBuffer ph = stack.mallocInt(1);
            glfwGetWindowSize(handle, pw, ph);

            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowPos(
                    handle,
                    (vidMode.width()  - pw.get(0)) / 2,
                    (vidMode.height() - ph.get(0)) / 2
                );
            }
        }

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(vsync ? 1 : 0);
        glfwShowWindow(handle);

        GL.createCapabilities();

        // Viewport initial
        glViewport(0, 0, windowWidth, windowHeight);

        // Resize callback
        glfwSetFramebufferSizeCallback(handle, (win, w, h) -> {
            this.windowWidth  = w;
            this.windowHeight = h;
            glViewport(0, 0, w, h);
            log.debug("Window resized: {}x{}", w, h);
        });

        log.info("Window created: {}x{} (scale {}x, vsync={})", windowWidth, windowHeight, scale, vsync);
        log.info("OpenGL: {} / {}", glGetString(GL_VERSION), glGetString(GL_RENDERER));
    }

    public void setTitle(String extra) {
        glfwSetWindowTitle(handle, title + (extra.isEmpty() ? "" : " — " + extra));
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public void destroy() {
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public long getHandle()      { return handle; }
    public int  getWidth()       { return windowWidth; }
    public int  getHeight()      { return windowHeight; }
    public int  getGameWidth()   { return GAME_WIDTH; }
    public int  getGameHeight()  { return GAME_HEIGHT; }
    public InputHandler getInput() { return inputHandler; }

    /**
     * Ratio pixel : on garde l'aspect 16:10 (320/200) en letterbox ou pillarbox.
     */
    public float[] getViewportRect() {
        float targetAspect = (float) GAME_WIDTH / GAME_HEIGHT;
        float winAspect    = (float) windowWidth / windowHeight;
        int vpW, vpH, vpX, vpY;

        if (winAspect > targetAspect) {
            vpH = windowHeight;
            vpW = (int)(windowHeight * targetAspect);
            vpX = (windowWidth - vpW) / 2;
            vpY = 0;
        } else {
            vpW = windowWidth;
            vpH = (int)(windowWidth / targetAspect);
            vpX = 0;
            vpY = (windowHeight - vpH) / 2;
        }
        return new float[]{ vpX, vpY, vpW, vpH };
    }
}
