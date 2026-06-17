package ab3d2;

import static ab3d2.bss.VidBss.mnu_palette;
import static ab3d2.bss.MenunbBss.mnu_morescreen;
import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.data.MenunbData.*;
import static ab3d2.ControlloopData.forward_key;
import static ab3d2.ControlloopData.backward_key;
import static ab3d2.ControlloopData.fire_key;
import static ab3d2.ControlloopData.Game_ShouldQuit_b;
import static ab3d2.modules.RawKeyMacros.RAWKEY_ESC;
import static ab3d2.modules.RawKeyMacros.RAWKEY_UP;
import static ab3d2.modules.RawKeyMacros.RAWKEY_DOWN;
import static ab3d2.modules.RawKeyMacros.RAWKEY_LEFT;
import static ab3d2.modules.RawKeyMacros.RAWKEY_RIGHT;
import static ab3d2.modules.RawKeyMacros.RAWKEY_ENTER;
import static ab3d2.modules.RawKeyMacros.RAWKEY_SPACEBAR;

/**
 * Traduction de ab3d2_source/menu/menunb.s (code).
 *
 * Moteur du menu : effet de feu (source aléatoire mnu_initrnd/getrnd), rendu de
 * texte/curseur via la police 16x16 planar (mnu_printxy), navigation clavier.
 * Le rendu matériel (blitter/affichage) est dans MenuC + ScreenC.Vid_PresentMenu ;
 * les attentes VBlank passent par MenuC.WaitTOF (une frame hôte). Le plot sinusoïdal
 * d'étincelles (mnu_plot) est différé (purement décoratif).
 */
public final class MenuNb {

    private static final int PS = 40 * 256;     // PLANESIZE

    private MenuNb() {
    }

    /** ror.w #1 : rotation droite de 1 du seul mot bas (haut préservé). */
    private static int rorw1(int v) {
        int lw = v & 0xFFFF;
        lw = ((lw >>> 1) | (lw << 15)) & 0xFFFF;
        return (v & ~0xFFFF) | lw;
    }

    /**
     * mnu_initrnd — construit une table de parité (256 octets, écrite à l'envers
     * dans le buffer palette comme scratch) puis remplit mnu_morescreen+6*40*256
     * (source aléatoire du feu) via un LFSR de graine 'TBL!'.
     */
    public static void mnu_initrnd() {
        int a1 = mnu_palette + 256;                         // lea mnu_palette+256,a1
        for (int d0 = 255; d0 >= 0; --d0) {                 // move.w #255,d0 ; .parityloop ... dbra d0
            int d1 = d0 & 0x1;                              // move.b d0,d1 ; and.w #1,d1
            int d2 = (d0 >> 2) & 0x1; d1 ^= d2;             // lsr.w #2,d2 ; and #1 ; eor
            d2 = (d0 >> 3) & 0x1; d1 ^= d2;                 // lsr.w #3 ...
            d2 = (d0 >> 5) & 0x1; d1 ^= d2;                 // lsr.w #5 ...
            a1--;
            Mem.wb(a1, d1);                                 // move.b d1,-(a1)
        }
        int a4 = a1;                                        // a4 = mnu_palette (table de parité)
        int d3 = 0x54424C21;                                // move.l #'TBL!',d3 (graine)
        int a0 = mnu_morescreen + 6 * 40 * 256;             // lea mnu_morescreen+6*40*256,a0
        for (int d0 = 40 * 256 + 8192 - 1; d0 >= 0; --d0) { // move.w #40*256+8192-1,d0 ; .loop ... dbra d0
            int d1 = 0;                                     // moveq.l #0,d1
            int d2 = d3;                                    // move.l d3,d2
            d1 = (d1 & ~0xFF) | (d2 & 0xFF);                // move.b d2,d1
            d2 = d2 & 0xFFFFFFFE;                           // and.b #$fe,d2
            d1 = (d1 & ~0xFF) | Mem.ub(a4 + (d1 & 0xFF));   // move.b (a4,d1.l),d1
            d2 = (d2 & ~0xFF) | ((d2 | d1) & 0xFF);         // or.b d1,d2
            d2 = rorw1(d2);                                 // ror.w #1,d2
            d2 = (d2 >>> 16) | (d2 << 16);                  // swap.w d2
            d1 = (d1 & ~0xFF) | (d2 & 0xFF);                // move.b d2,d1
            d2 = d2 & 0xFFFFFFFE;                           // and.b #$fe,d2
            d1 = (d1 & ~0xFF) | Mem.ub(a4 + (d1 & 0xFF));   // move.b (a4,d1.l),d1
            d2 = (d2 & ~0xFF) | ((d2 | d1) & 0xFF);         // or.b d1,d2
            d2 = rorw1(d2);                                 // ror.w #1,d2
            d3 = d2;                                        // move.l d2,d3
            d1 = (d1 & ~0xFFFF) | (d2 & 0xFFFF);            // move.w d2,d1
            d1 = (d1 & ~0xFFFF) | ((d1 & 0xFFFF) >>> 8);    // lsr.w #8,d1
            d2 = (d2 & ~0xFFFF) | ((d2 | d1) & 0xFFFF);     // or.w d1,d2
            d1 = d2;                                        // move.l d2,d1
            d1 = (d1 >>> 16) | (d1 << 16);                  // swap.w d1
            d2 = (d2 & ~0xFFFF) | ((d2 | d1) & 0xFFFF);     // or.w d1,d2
            Mem.wb(a0, d2 & 0xFF);                          // move.b d2,(a0)+
            a0++;
        }
    }

    /** getrnd — fait avancer mnu_rndptr dans la source aléatoire (mnu_rnd += 5). */
    public static void getrnd() {
        int d0 = Mem.uw(mnu_rnd) & 8190;                    // move.w mnu_rnd,d0 ; and.l #8190,d0
        d0 += mnu_morescreen + 6 * 40 * 256;                // add.l #mnu_morescreen+6*40*256,d0
        Mem.wl(mnu_rndptr, d0);                             // move.l d0,mnu_rndptr
        Mem.ww(mnu_rnd, Mem.uw(mnu_rnd) + 5);               // addq.w #5,mnu_rnd
    }

    /**
     * mnu_copycredz — copie l'image des crédits (3 plans 320x192) dans les plans
     * de police 3,4,5 du menu, à la ligne 32 (l'image « brûle » comme source du feu).
     */
    public static void mnu_copycredz() {
        int dst = mnu_morescreen + 3 * PS + 32 * 40;        // lea mnu_morescreen+3*40*256+32*40,a1
        int src = mnu_frame;                                // lea mnu_frame,a0
        for (int k = 0; k < 10 * 192; ++k) {                // move.w #10*192-1,d0 ... dbra
            Mem.wl(dst + k * 4,            Mem.l(src + k * 4));                 // plan 3
            Mem.wl(dst + k * 4 + PS,       Mem.l(src + k * 4 + 40 * 192));      // plan 4
            Mem.wl(dst + k * 4 + 2 * PS,   Mem.l(src + k * 4 + 2 * 40 * 192));  // plan 5
        }
    }

    /** mnu_cls — efface les 3 plans de police (3,4,5) du menu. */
    public static void mnu_cls() {
        ab3d2.modules.Sys.Sys_MemFillLong(mnu_morescreen + 3 * PS, 0, 3 * PS / 4);
    }

    /**
     * mnu_printxy — rend une chaîne (police mnu_font 16x16, 3 plans) dans les plans
     * 3,4,5 du menu à (d0=X octets, d1=Y pixels). Octet < 32 = saut de ligne (20 px).
     * Délai « machine à écrire » par caractère via mnu_printdelay (frames hôte).
     */
    public static void mnu_printxy(int a0, int d0, int d1) {
        int a3 = mnu_font;                                  // lea mnu_font,a3 (plan 0)
        int a4 = mnu_font + 176 * 40;                       // plan 1
        int a5 = mnu_font + 176 * 40 * 2;                   // plan 2
        final int d7 = 40, d6 = 20, d5 = 40 * 16;           // stride, glyphes/ligne, stride glyphe
        int base = (d1 & 0xFFFF) * d7 + (d0 & 0xFFFF) + mnu_morescreen + 40 * 256 * 3;
        int a1 = base, a2 = base;
        while (true) {
            int c = Mem.ub(a0); a0++;                        // move.b (a0)+,d2
            if (c == 0) break;                              // beq .exit
            Mem.wl(mnu_timer, Mem.l(mnu_printdelay));       // move.l mnu_printdelay,timer
            while (Mem.l(mnu_timer) > 0) {                  // .w8a : tst timer ; bne
                MenuC_WaitTOF();
            }
            int d2 = (c & 0xFF) - 32;                       // and.l #$ff,d2 ; sub.w #32,d2
            if (d2 < 0) {                                   // bge .ok ; sinon saut de ligne
                a1 = a2 + 20 * 40;                          // a1 = a2 + 20*40
                a2 = a1;
                continue;                                   // bra .loop
            }
            int row = d2 / d6;                              // divu d6,d2 → quotient = ligne
            int col = d2 % d6;                              // reste = colonne
            int off = row * d5 + col * 2;                   // d3 = ligne*640 + col*2
            int a6 = a1;
            for (int y = 0; y < 16; ++y) {                  // moveq #15,d2 ; .yloop
                Mem.ww(a6,            Mem.uw(a3 + off));     // (a3,d3) → (a6)
                Mem.ww(a6 + 40 * 256, Mem.uw(a4 + off));    // (a4,d3) → 40*256(a6)
                Mem.ww(a6 + 40 * 256 * 2, Mem.uw(a5 + off));// (a5,d3) → 40*256*2(a6)
                off += d7;                                  // add.l d7,d3
                a6 += d7;                                   // add.l d7,a6
            }
            a1 += 2;                                         // addq.l #2,a1
        }
    }

    /** mnu_animcursor — anime le caractère curseur (mnu_arrow) depuis mnu_frameptr (1 frame/2). */
    public static void mnu_animcursor() {
        if ((Mem.l(main_counter) & 1) == 0) {               // btst #0,main_counter+3 ; beq .skip
            return;
        }
        int ptr = Mem.l(mnu_frameptr);                      // move.l mnu_frameptr,a0
        Mem.wb(mnu_arrow, Mem.ub(ptr));                     // move.b (a0),mnu_arrow
        int next = Mem.ub(ptr + 1);                         // tst.b 1(a0)
        if (next == 0) {                                    // beq .skip
            return;
        }
        if (next <= 40) {                                   // cmp.b #40,1(a0) ; bhi .ok
            Mem.wl(mnu_frameptr, Mem.l(mnu_frameptr) - next); // sub.l d0,mnu_frameptr
        }
        Mem.wl(mnu_frameptr, Mem.l(mnu_frameptr) + 1);      // .ok : addq.l #1,mnu_frameptr
    }

    /** mnu_vblint — VBlank du menu : défilement + feu + animation curseur.
     *  NB : mnu_plot (étincelles) NON appelé — il sème des points de feu mobiles que mon
     *  émulation du feu ne fait pas décroître comme le blitter Amiga → accumulation jusqu'à
     *  remplir l'écran. Plusieurs tentatives (random dynamique, masque de densité) n'ont pas
     *  borné le feu sans le transformer en bruit. Le code mnu_plot reste dispo ; le réactiver
     *  nécessite de reproduire fidèlement le « refroidissement » naturel du blitter d'origine. */
    public static void mnu_vblint() {
        ab3d2.c.MenuC.mnu_movescreen();                     // CALLC mnu_movescreen
        ab3d2.c.MenuC.mnu_dofire();                         // CALLC mnu_dofire
        mnu_animcursor();                                   // bsr mnu_animcursor
    }

    /**
     * mnu_plot (menunb.s:837) — trace 50 étincelles sinusoïdales (Lissajous) dans les plans
     * de feu 0-2 (mnu_morescreen) : X = (sines[xsine0]+sines[ysine0])/16+160, Y = (sines[xsine1]
     * +sines[ysine1])/16+128 ; bit (7-X)&7 à l'offset Y*40+X/8. Les 4 oscillateurs avancent de
     * (sines[k]&3)+2 par point. Ces points lumineux sont ensuite emportés par le feu.
     */
    public static void mnu_plot() {
        int a0 = mnu_sines;                                 // table de base
        int a1 = mnu_xsine0;                                // oscillateurs (xsine0,xsine1,ysine0,ysine1)
        int a3 = mnu_sines;                                 // lecteur d'avance
        for (int pt = 49; pt >= 0; pt--) {                  // move.w #49,d7 ; plotlist ... dbra
            int d0 = Mem.uw(a1) & 1022;                     // (a1) = xsine0
            int d1 = (short) Mem.uw(a0 + d0);               // sines[xsine0]
            d0 = Mem.uw(a1 + 4) & 1022;                     // 4(a1) = ysine0
            d1 += (short) Mem.uw(a0 + d0);                  // + sines[ysine0]
            d1 = (d1 >> 4) + 160;                           // asr.w #4 ; add.w #160 → X
            d0 = Mem.uw(a1 + 2) & 1022;                     // 2(a1) = xsine1
            int d2 = (short) Mem.uw(a0 + d0);               // sines[xsine1]
            d0 = Mem.uw(a1 + 6) & 1022;                     // 6(a1) = ysine1
            d2 += (short) Mem.uw(a0 + d0);                  // + sines[ysine1]
            d2 = ((d2 >> 4) + 128) * 40;                    // asr.w #4 ; add.w #128 ; mulu #40 → Y*40
            int x = d1 & 0xFFFF;
            int off = d2 + (x >>> 3);                       // d2 + X/8
            int bit = (7 - x) & 7;                          // neg.w d0 ; addq #7 → bit (7-X)&7
            int a2 = mnu_morescreen + off;
            Mem.wb(a2, Mem.ub(a2) | (1 << bit));            // bset.b d0,(a2) — plan 0
            Mem.wb(a2 + 40 * 256, Mem.ub(a2 + 40 * 256) | (1 << bit));         // plan 1
            Mem.wb(a2 + 40 * 256 * 2, Mem.ub(a2 + 40 * 256 * 2) | (1 << bit)); // plan 2
            for (int r = 0; r < 4; r++) {                   // REPT 4 : avance les 4 oscillateurs
                int adv = (Mem.uw(a3) & 3) + 2; a3 += 2;    // (a3)+ & 3 ; + 2
                Mem.ww(a1, (Mem.uw(a1) + adv) & 0xFFFF); a1 += 2; // add.w d0,(a1)+
            }
        }
    }

    /** mnu_openmenu — ouvre/dessine un menu (a0 = ptr struct menu). */
    public static void mnu_openmenu(int a0) {
        int cl = (Mem.uw(mnu_currentlevel) + 65) & 0xFF;    // move.w mnu_currentlevel,d0 ; add #65 ; move.b
        Mem.wb(mnu_mainleveltext, cl);                      // → mnu_mainleveltext (legacy)
        Mem.wl(mnu_printdelay, 0);                          // move.l #0,mnu_printdelay
        mnu_cls();                                          // bsr mnu_cls
        Mem.wl(mnu_timer, 35);                              // move.l #35,timer
        while (Mem.l(mnu_timer) > 0) {                      // .w8a : attente
            MenuC_WaitTOF();
        }
        int x = Mem.uw(a0);                                 // move.w (a0),d0
        int y = Mem.uw(a0 + 2);                             // move.w 2(a0),d1
        int text = Mem.l(a0 + 4);                           // move.l 4(a0),a0
        mnu_printxy(text, x, y);                            // bsr mnu_printxy
        Mem.ww(mnu_curx, Mem.uw(a0 + 8));                   // move.w 8(a0),mnu_curx
        Mem.ww(mnu_cury, Mem.uw(a0 + 10));                  // move.w 10(a0),mnu_cury
        Mem.ww(mnu_spread, Mem.uw(a0 + 12));                // move.w 12(a0),mnu_spread
        Mem.ww(mnu_items, Mem.uw(a0 + 14));                 // move.w 14(a0),mnu_items
        int row = (Mem.uw(a0 + 14) * 3000) & 0xFFFF;        // mulu #3000,d0
        Mem.ww(mnu_row, row);                               // move.w d0,mnu_row
        Mem.ww(mnu_oldrow, row);                            // move.w d0,mnu_oldrow
        mnu_update(a0);                                     // bsr mnu_update
    }

    /** mnu_redraw — redessine le texte du menu courant. */
    public static void mnu_redraw(int a0) {
        int x = Mem.uw(a0);
        int y = Mem.uw(a0 + 2);
        int text = Mem.l(a0 + 4);
        mnu_printxy(text, x, y);
    }

    /**
     * mnu_update — parcourt les items du menu et dessine sliders (type 4) / cyclers
     * (type 5). Les types 8 (rawkey) / 9-10 (niveau) ne sont pas utilisés par les
     * menus portés (restent sans rendu). Les items type 0 ne dessinent rien.
     */
    public static void mnu_update(int a0) {
        int d7 = Mem.uw(a0 + 14) - 1;                       // items-1 (compteur)
        int d1 = Mem.uw(a0 + 10);                           // Y
        int d0 = Mem.uw(a0 + 8);                            // X
        int a1 = a0 + 16;
        for (; d7 >= 0; --d7) {                             // .itemloop ... dbra
            int type = Mem.l(a1); a1 += 4;                  // move.l (a1)+,d2
            int data = Mem.l(a1);                           // ptr slider/cycler (a1)
            if (type == 4) {                                // .doslider
                mnu_putslider(d0, d1, d7, data);            // (d7 = compteur — fidèle à l'ASM)
            } else if (type == 5) {                         // .docycler
                mnu_putcycler(d0, d1, data);
            }
            // .continue:
            d1 += Mem.uw(a0 + 12);                          // Y += spread
            a1 += 4;                                        // skip data ptr
        }
    }

    /**
     * mnu_putcycler (menunb.s) — dessine le texte de l'option courante d'un cycler.
     * Struct : 0=Xadd,2=Yadd,4=#items,6=ptr valeur,10=ptr textes[]. Enroule la valeur
     * mod #items et imprime le texte correspondant.
     */
    public static void mnu_putcycler(int d0, int d1, int a0) {
        d0 += Mem.w(a0);                                    // add.w (a0),d0
        d1 += Mem.w(a0 + 2);                                // add.w 2(a0),d1
        int a1 = Mem.l(a0 + 6);                             // ptr valeur
        int items = Mem.uw(a0 + 4);
        int d2 = items == 0 ? 0 : Integer.remainderUnsigned(Mem.uw(a1), items); // divu #items ; swap → reste
        Mem.ww(a1, d2);                                     // enroule la valeur
        int txt = Mem.l(a0 + 10 + d2 * 4);                  // 10(a0,d2*4) = texte
        mnu_printxy(txt, d0, d1);
    }

    /**
     * mnu_putslider (menunb.s) — dessine une barre de slider (parenthèses + blocs +
     * curseur) dans les plans police. Struct : 0=Xadd,2=Yadd,4=max,6=largeur(quartets),
     * 8=pas,10=ptr valeur. PORT FIDÈLE du code d'origine, qui utilise d7 (=compteur de
     * mnu_update) comme stride de ligne — INCORRECT mais aucun menu du jeu n'emploie de
     * slider (type 4), donc ce code n'est jamais exécuté dans le jeu réel. Conservé pour
     * la complétude du module.
     */
    public static void mnu_putslider(int d0, int d1, int d7, int a0) {
        d0 = (d0 + Mem.w(a0)) & 0xFFFF;                     // add.w (a0),d0
        d1 = (d1 + Mem.w(a0 + 2)) & 0xFFFF;                 // add.w 2(a0),d1
        slider_xpos = d0;                                   // move.w d0,.xpos
        mnu_printxy(mnu_leftslider, d0, d1);                // parenthèse gauche
        d0 = (d0 + 2) & 0xFFFF;                             // addq #2,d0
        int d2 = (Mem.uw(a0 + 6) >>> 4) & 0xFFFF;           // 6(a0) >> 4 = blocs pleins
        if (d2 != 0) {                                      // .skip si 0
            int d4 = d2;
            int a1 = mnu_sliderspace;
            for (int k = 0; k < d2; k++) { Mem.wb(a1, 59); a1++; } // remplit de 59
            Mem.wb(a1, 0);
            mnu_printxy(mnu_sliderspace, d0, d1);
            d0 = (d0 + d4 * 2) & 0xFFFF;                    // add.w d4,d0 ; add.w d4,d0
        }
        d2 = Mem.uw(a0 + 6) & 0xf;                          // 6(a0) & $f = bloc partiel
        if (d2 != 0) {                                      // .skip2 si 0
            int d5 = d2;
            int d3 = 0;
            for (int k = 0; k < d2; k++) {                  // .loop1 : masque de d2 bits hauts
                d3 = (((d3 & 0xFFFF) >>> 1) | (d3 << 15)) & 0xFFFF; // ror.w #1
                d3 |= 0x8000;
            }
            int a4 = ((d1 * (d7 & 0xFFFF)) + d0 + mnu_morescreen + 40 * 256 * 3); // mulu d7,d1 (fidèle)
            int d3full = (d3 << 16);                        // swap.w d3 ; clr.w d3
            int a5 = a4;
            int a3 = Mem.l(mnu_sliddat);
            for (int i = 0; i < 16; i++) {                  // .loop2 : AND
                Mem.wl(a4, Mem.l(a3) & d3full);
                Mem.wl(a4 + 40 * 256, Mem.l(a3 + 176 * 40) & d3full);
                Mem.wl(a4 + 40 * 256 * 2, Mem.l(a3 + 176 * 40 * 2) & d3full);
                a3 += 40; a4 += 40;
            }
            a4 = a5; a3 = Mem.l(mnu_sliddat);
            for (int i = 0; i < 16; i++) {                  // .loop3 : OR (sliddat+2 >> d5)
                Mem.wl(a4, Mem.l(a4) | (Mem.l(a3 + 2) >>> d5));
                Mem.wl(a4 + 40 * 256, Mem.l(a4 + 40 * 256) | (Mem.l(a3 + 2 + 176 * 40) >>> d5));
                Mem.wl(a4 + 40 * 256 * 2, Mem.l(a4 + 40 * 256 * 2) | (Mem.l(a3 + 2 + 176 * 40 * 2) >>> d5));
                a3 += 40; a4 += 40;
            }
        } else {                                            // .skip2
            mnu_printxy(mnu_rightslider, d0, d1);           // parenthèse droite
        }
        // .cont1 : le curseur
        int a1 = Mem.l(a0 + 10);                            // ptr valeur
        int v = Mem.w(a1);
        if (v < 0) v = 0;                                   // .ok1
        if (v > Mem.w(a0 + 4)) v = Mem.w(a0 + 4);           // .ok2
        Mem.ww(a1, v);
        int max = Mem.uw(a0 + 4);
        int kx = max == 0 ? 0 : (v * Mem.uw(a0 + 6)) / max; // mulu 6(a0) ; divu 4(a0)
        kx = (kx - Mem.uw(mnu_sliderwidth)) & 0xFFFF;
        kx = (kx + (slider_xpos << 3)) & 0xFFFF;            // + .xpos<<3
        int d2b = kx & 0xf;
        kx = ((kx & 0xFFFF) >>> 4) << 1;
        kx = (kx + 2) & 0xFFFF;
        int a4 = (d1 * (d7 & 0xFFFF)) + kx + mnu_morescreen + 40 * 256 * 3; // mulu d7,d1 (fidèle)
        int a3 = Mem.l(mnu_sliddat);
        for (int i = 0; i < 16; i++) {                      // .loop4 : OR (sliddat+6 >> d2b)
            Mem.wl(a4, Mem.l(a4) | (Mem.l(a3 + 6) >>> d2b));
            Mem.wl(a4 + 40 * 256, Mem.l(a4 + 40 * 256) | (Mem.l(a3 + 6 + 176 * 40) >>> d2b));
            Mem.wl(a4 + 40 * 256 * 2, Mem.l(a4 + 40 * 256 * 2) | (Mem.l(a3 + 6 + 176 * 40 * 2) >>> d2b));
            a3 += 40; a4 += 40;
        }
    }

    private static int slider_xpos;                         // .xpos de mnu_putslider

    /** mnu_docursor — dessine le caractère curseur (mnu_arrow) sur la ligne courante. */
    public static void mnu_docursor() {
        int row = Mem.uw(mnu_row);                          // move.w mnu_row,d1
        Mem.ww(mnu_oldrow, row);                            // move.w d1,mnu_oldrow
        int items = Mem.uw(mnu_items);
        int rem = items == 0 ? 0 : Integer.remainderUnsigned(row, items); // divu items ; swap → reste
        int yy = rem * Mem.uw(mnu_spread) + Mem.uw(mnu_cury); // mulu spread ; add cury
        int xx = Mem.uw(mnu_curx);                          // move.w mnu_curx,d0
        mnu_printxy(mnu_arrow, xx, yy);                     // lea mnu_arrow,a0 ; bsr mnu_printxy
    }

    /**
     * mnu_waitmenu — boucle d'attente d'une sélection. Gère le déplacement du curseur
     * (haut/bas) en boucle jusqu'à validation (Entrée/Espace/Feu) ; rend la main.
     * La valeur retournée est recalculée par game_CheckMenu (= mnu_row % items), donc
     * seul le fait de rendre la main au bon moment importe.
     */
    /** d1 renvoyé par mnu_waitmenu : 0 = validation/Esc, 41 = droite, 42 = gauche (sliders/cyclers). */
    public static int mnu_waitFlag;

    public static int mnu_waitmenu() {
        Mem.wl(mnu_printdelay, 0);                          // clr.l mnu_printdelay
        mnu_waitFlag = 0;
        while (true) {                                      // .loop
            int oldrow = Mem.uw(mnu_oldrow);
            if (oldrow != Mem.uw(mnu_row)) {                // cmp mnu_row,mnu_oldrow ; beq .skip
                int items = Mem.uw(mnu_items);
                int rem = items == 0 ? 0 : Integer.remainderUnsigned(oldrow, items);
                int yy = rem * Mem.uw(mnu_spread) + Mem.uw(mnu_cury);
                int xx = Mem.uw(mnu_curx);
                mnu_printxy(mnu_cleararrow, xx, yy);        // efface l'ancien curseur
            }
            // .w8key
            while (true) {
                if (Mem.b(Game_ShouldQuit_b) != 0) {        // tst Game_ShouldQuit_b ; bne .exit_game
                    Mem.wb(Game_ShouldQuit_b, 0xFF);
                    return 0;
                }
                mnu_docursor();                             // bsr mnu_docursor
                MenuC_WaitTOF();                            // CALLGRAF WaitTOF
                MenuC_WaitTOF();                            // CALLGRAF WaitTOF
                ab3d2.Cd32joy._ReadJoy1();                  // jsr _ReadJoy1

                int fk = Mem.ub(forward_key);
                int up = Mem.ub(KeyMap_vb + fk);
                Mem.wb(KeyMap_vb + fk, 0);
                if (up != 0) {                              // forward_key → .up
                    Mem.ww(mnu_row, (Mem.uw(mnu_row) - 1) & 0xFFFF);
                    break;
                }
                int bk = Mem.ub(backward_key);
                int dn = Mem.ub(KeyMap_vb + bk);
                Mem.wb(KeyMap_vb + bk, 0);
                if (dn != 0) {                              // backward_key → .down
                    Mem.ww(mnu_row, (Mem.uw(mnu_row) + 1) & 0xFFFF);
                    break;
                }
                int ck = Mem.ub(fire_key);
                int fr = Mem.ub(KeyMap_vb + ck);
                Mem.wb(KeyMap_vb + ck, 0);
                if (fr != 0) {                              // fire_key → validation
                    return selection();
                }

                int d0 = ab3d2.host.Input.keyReadKey();     // jsr key_readkey
                if (d0 == 0) {
                    continue;                               // .w8key
                }
                if (d0 == RAWKEY_ESC) {                     // Esc → .exit
                    return -1;
                }
                if (d0 == RAWKEY_DOWN) {
                    Mem.ww(mnu_row, (Mem.uw(mnu_row) + 1) & 0xFFFF);
                    break;
                }
                if (d0 == RAWKEY_UP) {
                    Mem.ww(mnu_row, (Mem.uw(mnu_row) - 1) & 0xFFFF);
                    break;
                }
                if (d0 == RAWKEY_ENTER || d0 == RAWKEY_SPACEBAR) { // validation
                    mnu_waitFlag = 0;
                    return selection();
                }
                if (d0 == RAWKEY_RIGHT) {                    // .sliderr : d1=41
                    mnu_waitFlag = 41;
                    return selection();
                }
                if (d0 == RAWKEY_LEFT) {                     // .sliderl : d1=42
                    mnu_waitFlag = 42;
                    return selection();
                }
                if (d0 == Hires.QUIT_KEY) {                 // QUIT_KEY → .exit_game
                    Mem.wb(Game_ShouldQuit_b, 0xFF);
                    return 0;
                }
                // touche inconnue : animation d'erreur, on relance .loop
                Mem.wl(mnu_frameptr, mnu_errcursanim);
                break;
            }
        }
    }

    /** .cpcont : d0 = mnu_row % mnu_items (sélection). */
    private static int selection() {
        int items = Mem.uw(mnu_items);
        return items == 0 ? 0 : Integer.remainderUnsigned(Mem.uw(mnu_row), items);
    }

    /** mnu_getrawvalue — lit une touche brute saisie (config contrôles ; non utilisé par le cœur). */
    public static int mnu_getrawvalue() {
        while (true) {
            mnu_docursor();
            MenuC_WaitTOF();
            int d0 = ab3d2.host.Input.lastpressed;
            if (d0 != 0) {
                ab3d2.host.Input.lastpressed = 0;
                Mem.ww(mnu_oldrow, 0xFFFF);
                return d0;
            }
        }
    }

    // -- alias court vers la primitive de frame hôte --
    private static void MenuC_WaitTOF() {
        ab3d2.c.MenuC.WaitTOF();
    }
}
