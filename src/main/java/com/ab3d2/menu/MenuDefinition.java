package com.ab3d2.menu;

import java.util.List;

/**
 * Structure de données des menus AB3D2.
 *
 * Reproduit exactement la structure ASM :
 *   dc.w  xPos, yPos       ; position texte (X en words=bytes, Y en lignes pixel)
 *   dc.l  textPtr          ; pointeur texte
 *   dc.w  xCursor, yCursor ; position curseur initial
 *   dc.w  spread           ; espacement entre items (pixels)
 *   dc.w  numItems         ; nombre d'items
 *   dc.l  type, ptr        ; * numItems
 *
 * Types d'items (reproduit les cas de mnu_domenu) :
 *   0 = do nothing
 *   1 = sous-menu (push + recurse)
 *   2 = exit (retour menu parent)
 *   3 = call function (BSR)
 *   4 = cycle value (left/right)
 *   5 = cycle level (left/right)
 *   6 = jump (JMP, modifie la stack)
 *   7 = changemenu (remplace menu courant)
 *   8 = raw key input
 *   9 = load position
 *  10 = save position
 */
public record MenuDefinition(
    int    xPos,       // X position texte en bytes (word)
    int    yPos,       // Y position texte en pixels
    String text,       // texte du menu (lignes séparées par \n)
    int    xCursor,    // X curseur initial
    int    yCursor,    // Y curseur initial
    int    spread,     // pixels entre items (20 dans l'original)
    List<MenuAction> actions
) {

    /**
     * Une action associée à un item du menu.
     *
     * @param type   type d'action (0..10)
     * @param target cible (sous-menu, callback, etc.)
     */
    public record MenuAction(int type, Object target) {
        // Types constants pour lisibilité
        public static final int DO_NOTHING   = 0;
        public static final int SUBMENU      = 1;
        public static final int EXIT         = 2;
        public static final int CALL         = 3;
        public static final int CYCLE        = 4;
        public static final int CYCLE_LEVEL  = 5;
        public static final int JUMP         = 6;
        public static final int CHANGE_MENU  = 7;
        public static final int RAW_KEY      = 8;
        public static final int LOAD_POS     = 9;
        public static final int SAVE_POS     = 10;

        public boolean isExit()    { return type == EXIT; }
        public boolean isSubmenu() { return type == SUBMENU; }
        public boolean isCall()    { return type == CALL; }
    }

    // ── Menus du jeu (traduits depuis l'ASM) ────────────────────────────────

    /**
     * Menu principal (mnu_mainmenu de l'ASM).
     *
     * ASM original :
     *   dc.w 6,12         ; X=6bytes, Y=12px
     *   dc.l mnu_maintext
     *   dc.w 4,70         ; cursor X=4, Y=70
     *   dc.w 20           ; spread
     *   dc.w 7            ; 7 items
     */
    public static MenuDefinition mainMenu() {
        return new MenuDefinition(
            6, 12,
            // mnu_maintext dans l'ASM (niveau + options)
            "\n\nLevel A:\n\nPlay game\nControl options\nGame credits\nLoad position\nSave position\nQuit",
            4, 70,
            20,
            List.of(
                new MenuAction(MenuAction.CYCLE_LEVEL,  null),    // cycle player type
                new MenuAction(MenuAction.CALL,         "playgame"),
                new MenuAction(MenuAction.SUBMENU,      "controls"),
                new MenuAction(MenuAction.CALL,         "credits"),
                new MenuAction(MenuAction.SUBMENU,      "load"),
                new MenuAction(MenuAction.SUBMENU,      "save"),
                new MenuAction(MenuAction.EXIT,         null)
            )
        );
    }

    /**
     * Menu quit (mnu_quitmenu).
     *
     * ASM original :
     *   dc.w 4,82
     *   dc.l mnu_quitmenutext
     *   dc.w 4,120
     *   dc.w 20
     *   dc.w 2
     *   dc.l 6,mnu_loop    ; jump back to loop
     *   dc.l 6,mnu_exit    ; jump to exit
     */
    public static MenuDefinition quitMenu() {
        return new MenuDefinition(
            4, 82,
            "Are you sure\nyou want to quit?\n\nNo\nYes",
            4, 120,
            20,
            List.of(
                new MenuAction(MenuAction.JUMP, "mainloop"),
                new MenuAction(MenuAction.JUMP, "exit")
            )
        );
    }

    /**
     * Nombre d'items navigables (lignes non-vides du texte correspondant aux actions).
     */
    public int itemCount() {
        return actions.size();
    }

    /**
     * Position Y du Nième item du curseur.
     */
    public int itemY(int index) {
        return yCursor + index * spread;
    }
}