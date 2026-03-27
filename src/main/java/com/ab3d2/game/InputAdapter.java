package com.ab3d2.game;

import com.ab3d2.core.InputHandler;

import static org.lwjgl.glfw.GLFW.*;

public class InputAdapter {

    public boolean forward;
    public boolean backward;
    public boolean left;
    public boolean right;
    public boolean fire;
    public boolean use;

    public float lookDX;
    public float lookDY;

    public void update(InputHandler input) {

        forward = input.isKeyHeld(GLFW_KEY_W);
        backward = input.isKeyHeld(GLFW_KEY_S);
        left = input.isKeyHeld(GLFW_KEY_A);
        right = input.isKeyHeld(GLFW_KEY_D);

        fire = input.isMouseHeld(GLFW_MOUSE_BUTTON_LEFT);
        use = input.isKeyPressed(GLFW_KEY_E);

        lookDX = (float) input.getMouseDX();
        lookDY = (float) input.getMouseDY();
    }
}
