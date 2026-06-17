package ab3d2;

import static ab3d2.data.DrawData.draw_ScrollChars_vb;

/**
 * Traduction du scroller de messages horizontal de hires.s (NARRATOR/SENDMESSAGE),
 * fourni par l'utilisateur depuis les sources d'origine (absent du git porté, qui
 * utilise le système de lignes verticales message.c).
 *
 * Bandeau de 1 bitplane (SCROLLSCRN, 80 octets/ligne × 16 lignes, glyphes 8×8 via
 * scrollfont = draw_ScrollChars_vb). NARRATOR (1×/frame) gère le timer, pose 2
 * caractères/frame en phase active et avance SCROLLXPOS (+2, bouclage à 80).
 * SENDMESSAGE (ré)initialise pointeur/XPos/fin/timer à l'arrivée d'un message.
 * L'affichage (copperlist 1 plan en bas) est remplacé par ScreenC (chunky).
 */
public final class Narrator {

    // ---- données (hires.s:13923) ----
    public static final int SCROLLSCRN = Mem.alloc(20 * 16 * 4);   // ds.l 20*16 = 1280 o (80 o/ligne × 16)
    public static final int SCROLLTIMER = Mem.dcW(100);
    public static final int SCROLLXPOS = Mem.dcW(0);               // = draw_GameMessageXPos_w
    public static final int SCROLLPOINTER;                          // = draw_GameMessagePtr_l (lecture)
    public static final int ENDSCROLL;
    public static final int BLANKSCROLL;                            // 80 espaces
    public static final int scrollMsg;                             // scratch 160 o (messages dynamiques)

    static {
        BLANKSCROLL = Mem.dcbB(80, ' ');
        scrollMsg = Mem.dcbB(160, ' ');
        SCROLLPOINTER = Mem.dcL(BLANKSCROLL);
        ENDSCROLL = Mem.dcL(BLANKSCROLL + 80);
    }

    private Narrator() {
    }

    /**
     * SENDMESSAGE (hires.s:10859) — amorce l'affichage d'un message (d0 = pointeur sur
     * la chaîne de 160 octets). Pose pointeur de lecture, XPos=0, fin=+160, timer=40
     * (démarre directement en phase active). (File circulaire MESSPTR/recall omise.)
     */
    public static void SENDMESSAGE(int d0) {
        Mem.wl(SCROLLPOINTER, d0);                  // move.l d0,SCROLLPOINTER
        Mem.ww(SCROLLXPOS, 0);                       // move.w #0,SCROLLXPOS
        Mem.wl(ENDSCROLL, d0 + 160);                 // add.l #160,d0 ; move.l d0,ENDSCROLL
        Mem.ww(SCROLLTIMER, 40);                      // move.w #40,SCROLLTIMER
    }

    /** Pratique : copie une chaîne (len octets) dans scrollMsg (complétée d'espaces sur 160) puis SENDMESSAGE. */
    public static void sendText(int textPtr, int len) {
        for (int i = 0; i < 160; i++) {
            Mem.wb(scrollMsg + i, i < len ? Mem.ub(textPtr + i) : ' ');
        }
        SENDMESSAGE(scrollMsg);
    }

    /**
     * NARRATOR (hires.s:7149) — appelé 1×/frame. Timer : >=40 attente, 0..39 actif
     * (pose 2 caractères, avance XPos +2 bouclé à 80), <0 relance un cycle (timer=150).
     */
    public static void NARRATOR() {
        int d0 = (short) Mem.uw(SCROLLTIMER) - 1;    // move.w SCROLLTIMER,d0 ; subq #1,d0
        Mem.ww(SCROLLTIMER, d0);                      // move.w d0,SCROLLTIMER
        if (d0 >= 40) {                               // cmp.w #40,d0 ; bge .NOCHARYET
            return;
        }
        if (d0 < 0) {                                 // tst.w d0 ; bge .okcha ; sinon
            Mem.ww(SCROLLTIMER, 150);                 // move.w #150,SCROLLTIMER
            return;
        }
        // .okcha : phase active
        int a0 = SCROLLSCRN + (Mem.uw(SCROLLXPOS) & 0xFFFF); // SCROLLSCRN + SCROLLXPOS
        for (int d7 = 1; d7 >= 0; d7--) {             // moveq #1,d7 → 2 caractères
            int a1 = Mem.l(SCROLLPOINTER);
            int ch = Mem.ub(a1); a1 += 1;             // move.b (a1)+,d1
            if (a1 >= Mem.l(ENDSCROLL)) {             // cmp.l ENDSCROLL,d2 ; blt .notrestart
                a1 = BLANKSCROLL;                     // bascule sur les blancs
                Mem.wl(ENDSCROLL, BLANKSCROLL + 80);
            }
            Mem.wl(SCROLLPOINTER, a1);
            int g = draw_ScrollChars_vb + (ch << 3);  // glyphe : ASCII*8 (8 octets, 1/ligne)
            for (int row = 0; row < 8; row++) {       // pose les 8 lignes (pas vertical = 80)
                Mem.wb(a0 + 80 * row, Mem.ub(g + row));
            }
            a0 += 1;                                  // colonne suivante (+8 px)
        }
        int xp = (Mem.uw(SCROLLXPOS) + 2) & 0xFFFF;   // addq #2,SCROLLXPOS
        if (xp >= 80) {                               // cmp #80 ; blt .NOCHARYET ; sinon 0
            xp = 0;
        }
        Mem.ww(SCROLLXPOS, xp);
    }
}
