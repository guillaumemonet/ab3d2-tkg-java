package com.ab3d2.game;

import com.ab3d2.render.Renderer2D;

public class GameScreen {

    private final Level level;
    private final Player player;
    private final Renderer3D renderer3D;
    private final HUDRenderer hudRenderer;
    private final MessageLog messageLog;

    public GameScreen(Level level, Player player, Renderer3D renderer3D,
            HUDRenderer hudRenderer, MessageLog messageLog) {
        this.level = level;
        this.player = player;
        this.renderer3D = renderer3D;
        this.hudRenderer = hudRenderer;
        this.messageLog = messageLog;
    }

    public void init() {
        level.init();
        player.init(level);
        messageLog.pushMessage("WELCOME TO AB3D2 JAVA PORT");
    }

    public void update(double dt, InputAdapter input) {
        player.update(dt, input, level);
        level.update(dt);
        messageLog.update(dt);
    }

    public void render(Renderer2D r) {
        // 1. Rendu 3D (stub pour l’instant)
        renderer3D.render(level, player, r);

        // 2. HUD
        hudRenderer.renderHUD(r, player, level);

        // 3. Messages bas d’écran
        messageLog.render(r);
    }

    public void destroy() {
        level.destroy();
        renderer3D.destroy();
        // HUDRenderer et MessageLog n’ont pas forcément besoin de destroy
    }
}
