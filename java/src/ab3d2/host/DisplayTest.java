package ab3d2.host;

import ab3d2.Mem;
import ab3d2.bss.Bss;
import ab3d2.c.ScreenC;
import ab3d2.data.DataSections;

import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;

/**
 * Harnais de validation de la chaîne d'affichage (Phase 1) — NON partie du jeu.
 *
 * Vérifie de bout en bout : la VRAIE palette du jeu (256pal, chargée dans
 * draw_Palette_vw par DataSections.init, passée par le pipeline
 * Vid_LoadMainPalette → LoadRGB32) + un buffer chunky 320x256 dans Mem.RAM →
 * ScreenC.Vid_Present → fenêtre LWJGL.
 *
 * Affiche un motif XOR (balaie les 256 index de palette) animé pour confirmer
 * que le rendu est « live » et que la palette s'applique. Fermer la fenêtre
 * (croix) pour quitter.
 */
public final class DisplayTest {

    private DisplayTest() {
    }

    public static void main(String[] args) {
        Bss.init();
        DataSections.init();
        // draw_Palette_vw contient déjà la vraie palette (incbin "256pal", 768 mots).

        // Buffer chunky 320x256 (1 octet/pixel) alloué dans Mem et publié dans
        // Vid_FastBufferPtr_l (comme le ferait Draw_Init).
        int fast = Mem.alloc(ScreenC.SCREEN_WIDTH * ScreenC.SCREEN_HEIGHT);
        Mem.wl(Vid_FastBufferPtr_l, fast);

        // 3) Ouverture de la fenêtre (construit la palette hôte depuis draw_Palette_vw).
        ScreenC.setHostScale(3);
        ScreenC.Vid_OpenMainScreen();

        // 4) Boucle de présentation : motif XOR animé.
        int frame = 0;
        while (!ScreenC.hostDisplay().shouldClose()) {
            for (int y = 0; y < ScreenC.SCREEN_HEIGHT; y++) {
                int row = fast + y * ScreenC.SCREEN_WIDTH;
                for (int x = 0; x < ScreenC.SCREEN_WIDTH; x++) {
                    Mem.wb(row + x, (x ^ y) + frame);
                }
            }
            ScreenC.Vid_Present(); // upload + swap + pollEvents (vsync)
            frame++;
        }

        ScreenC.Vid_CloseMainScreen();
    }
}
