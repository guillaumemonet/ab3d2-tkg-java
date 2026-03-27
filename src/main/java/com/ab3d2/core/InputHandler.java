package com.ab3d2.core;


import java.util.BitSet;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Gestion input clavier + souris via GLFW callbacks. Edge-triggered
 * (pressed/released) + level-triggered (held).
 */
public class InputHandler {

    private static final int MAX_KEYS = GLFW_KEY_LAST + 1;
    private static final int MAX_BUTTONS = 8;

    private final BitSet keysHeld = new BitSet(MAX_KEYS);
    private final BitSet keysPressed = new BitSet(MAX_KEYS);
    private final BitSet keysReleased = new BitSet(MAX_KEYS);

    private final boolean[] mouseHeld = new boolean[MAX_BUTTONS];
    private final boolean[] mousePressed = new boolean[MAX_BUTTONS];
    private final boolean[] mouseReleased = new boolean[MAX_BUTTONS];

    private double mouseX, mouseY;
    private double mouseDX, mouseDY;
    private double scrollDY;

    public InputHandler(long window) {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key < 0 || key >= MAX_KEYS) {
                return;
            }
            switch (action) {
                case GLFW_PRESS -> {
                    keysHeld.set(key);
                    keysPressed.set(key);
                }
                case GLFW_RELEASE -> {
                    keysHeld.clear(key);
                    keysReleased.set(key);
                }
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button < 0 || button >= MAX_BUTTONS) {
                return;
            }
            switch (action) {
                case GLFW_PRESS -> {
                    mouseHeld[button] = true;
                    mousePressed[button] = true;
                }
                case GLFW_RELEASE -> {
                    mouseHeld[button] = false;
                    mouseReleased[button] = true;
                }
            }
        });

        double[] cx = new double[1], cy = new double[1];
        glfwGetCursorPos(window, cx, cy);
        mouseX = cx[0];
        mouseY = cy[0];

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            mouseDX += x - mouseX;
            mouseDY += y - mouseY;
            mouseX = x;
            mouseY = y;
        });

        glfwSetScrollCallback(window, (win, dx, dy) -> {
            scrollDY += dy;
        });
    }

    /**
     * À appeler en fin de frame pour reset les états edge-triggered.
     */
    public void endFrame() {
        keysPressed.clear();
        keysReleased.clear();
        for (int i = 0; i < MAX_BUTTONS; i++) {
            mousePressed[i] = false;
            mouseReleased[i] = false;
        }
        mouseDX = 0;
        mouseDY = 0;
        scrollDY = 0;
    }

    public boolean isKeyHeld(int key) {
        return keysHeld.get(key);
    }

    public boolean isKeyPressed(int key) {
        return keysPressed.get(key);
    }

    public boolean isKeyReleased(int key) {
        return keysReleased.get(key);
    }

    public boolean isMouseHeld(int btn) {
        return mouseHeld[btn];
    }

    public boolean isMousePressed(int btn) {
        return mousePressed[btn];
    }

    public boolean isMouseReleased(int btn) {
        return mouseReleased[btn];
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public double getMouseDX() {
        return mouseDX;
    }

    public double getMouseDY() {
        return mouseDY;
    }

    public double getScrollDY() {
        return scrollDY;
    }
}
