package com.ab3d2.core;

/**
 * Interface de base pour tous les écrans (Menu, InGame, Editor...). Cycle de
 * vie : init() → update()/render() en boucle → destroy()
 */
public interface Screen {

    /**
     * Appelé une fois à l'entrée dans l'état.
     */
    void init(GameContext ctx);

    /**
     * Mise à jour logique, retourne le prochain état ou null pour rester.
     */
    GameState update(GameContext ctx, double deltaTime);

    /**
     * Rendu OpenGL.
     */
    void render(GameContext ctx, double alpha);

    /**
     * Appelé à la sortie de l'état. Libérer les ressources locales ici.
     */
    void destroy(GameContext ctx);
}
