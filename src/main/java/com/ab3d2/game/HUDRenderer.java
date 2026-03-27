package com.ab3d2.game;

import com.ab3d2.assets.MenuAssets;
import com.ab3d2.menu.Ab3dFont;
import com.ab3d2.render.Renderer2D;

public class HUDRenderer {

    private final Ab3dFont font;
    private final int screenW;
    private final int screenH;

    public HUDRenderer(Ab3dFont font, int screenW, int screenH) {
        this.font = font;
        this.screenW = screenW;
        this.screenH = screenH;
    }

    public void renderHUD(Renderer2D r, Player player, Level level) {
        if (font == null) {
            return;
        }

        // Health
        String health = "HEALTH " + player.getHealth();
        float hx = 4;
        float hy = screenH - 32;
        font.drawString(r, health, hx, hy);

        // Ammo
        String ammo = "AMMO " + player.getAmmo();
        float ax = screenW - font.stringWidth(ammo, 1f) - 4;
        float ay = screenH - 32;
        font.drawString(r, ammo, ax, ay);

        // Level name (top center)
        String name = level.getName().toUpperCase();
        float nx = font.centerX(name, screenW, 1f);
        float ny = 4;
        font.drawString(r, name, nx, ny);
    }
}
