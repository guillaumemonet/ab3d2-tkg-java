package ab3d2.data;

import ab3d2.Assets;

/**
 * Traduction littérale de ab3d2_source/titlecop.s.
 *
 * Uniquement la palette de l'écran-titre (incbin). Asset possiblement absent
 * (mode tolérant).
 */
public final class TitlecopData {

    /** TITLEPAL: incbin "includes/newtitlepal" */
    public static final int TITLEPAL = Assets.incbin("includes/newtitlepal");

    private TitlecopData() {
    }
}
