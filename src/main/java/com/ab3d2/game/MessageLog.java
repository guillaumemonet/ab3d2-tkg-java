package com.ab3d2.game;

import com.ab3d2.menu.Ab3dFont;
import com.ab3d2.render.Renderer2D;

import java.util.ArrayDeque;
import java.util.Deque;

public class MessageLog {

    private final Ab3dFont font;
    private final int screenW;
    private final int screenH;

    private final Deque<String> messages = new ArrayDeque<>();
    private double timer = 0.0;
    private double messageDuration = 3.0; // secondes

    public MessageLog(Ab3dFont font, int screenW, int screenH) {
        this.font = font;
        this.screenW = screenW;
        this.screenH = screenH;
    }

    public void pushMessage(String msg) {
        messages.clear();
        messages.addLast(msg);
        timer = 0.0;
    }

    public void update(double dt) {
        if (messages.isEmpty()) return;
        timer += dt;
        if (timer > messageDuration) {
            messages.clear();
        }
    }

    public void render(Renderer2D r) {
        if (font == null || messages.isEmpty()) return;
        String msg = messages.peekFirst();
        float x = font.centerX(msg, screenW, 1f);
        float y = screenH - 16; // bande du bas
        font.drawString(r, msg, x, y);
    }
}
