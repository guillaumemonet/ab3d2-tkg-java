package ab3d2.modules;

import ab3d2.Mem;
import ab3d2.data.VidData;

import static ab3d2.data.VidData.VID_CONTRAST_ADJ_DEF;
import static ab3d2.data.VidData.VID_CONTRAST_ADJ_MAX;
import static ab3d2.data.VidData.VID_CONTRAST_ADJ_MIN;
import static ab3d2.data.VidData.VID_CONTRAST_ADJ_STEP;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_1;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_2;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_3;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_4;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_5;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_6;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_7;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_8;
import static ab3d2.modules.RawKeyMacros.RAWKEY_NUM_9;

/**
 * Traduction littérale de ab3d2_source/modules/vid.s
 *
 * (C2P_Init est entièrement commenté dans l'original — les routines C2P sont
 * exclues du portage de toute façon.)
 */
public final class Vid {

    public static final int VID_BRIGHT_ADJ_STEP = 128;
    public static final int VID_BRIGHT_ADJ_MIN = -4096;
    public static final int VID_BRIGHT_ADJ_MAX = 5120;

    private Vid() {
    }

    /**
     * Vid_CheckSettingsAdjust — a5 contains keyboard state. No regs clobbered.
     * Pavé numérique : 1/2/3 = luminosité -, reset, + ; 4/5/6 = contraste ;
     * 7/8/9 = gamma.
     */
    public static void Vid_CheckSettingsAdjust(int a5) {
        Mem.wb(VidData.Vid_UpdatePalette_b, 0);        // clr.b Vid_UpdatePalette_b

        // Brightness offset (black point)
        // .dec_bright_offset:
        if (Mem.b(a5 + RAWKEY_NUM_1) != 0) {           // tst.b RAWKEY_NUM_1(a5) ; beq.s .res_bright_offset
            Mem.wb(a5 + RAWKEY_NUM_1, 0);              // clr.b RAWKEY_NUM_1(a5)
            if (Mem.w(VidData.Vid_BrightnessOffset_w) <= VID_BRIGHT_ADJ_MIN) { // cmpi.w ; ble .skip_update_palette
                return;
            }
            Mem.ww(VidData.Vid_BrightnessOffset_w,
                    Mem.w(VidData.Vid_BrightnessOffset_w) - VID_BRIGHT_ADJ_STEP); // sub.w #VID_BRIGHT_ADJ_STEP,...
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // bra .update_palette ; st Vid_UpdatePalette_b
            return;
        }

        // .res_bright_offset:
        if (Mem.b(a5 + RAWKEY_NUM_2) != 0) {           // tst.b RAWKEY_NUM_2(a5) ; beq.s .inc_bright_offset
            Mem.wb(a5 + RAWKEY_NUM_2, 0);              // clr.b RAWKEY_NUM_2(a5)
            Mem.ww(VidData.Vid_BrightnessOffset_w, 0); // clr.w Vid_BrightnessOffset_w
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // bra .update_palette
            return;
        }

        // .inc_bright_offset:
        if (Mem.b(a5 + RAWKEY_NUM_3) != 0) {           // tst.b RAWKEY_NUM_3(a5) ; beq.s .dec_contrast_adjust
            Mem.wb(a5 + RAWKEY_NUM_3, 0);              // clr.b RAWKEY_NUM_3(a5)
            if (Mem.w(VidData.Vid_BrightnessOffset_w) >= VID_BRIGHT_ADJ_MAX) { // cmpi.w ; bge .skip_update_palette
                return;
            }
            Mem.ww(VidData.Vid_BrightnessOffset_w,
                    Mem.w(VidData.Vid_BrightnessOffset_w) + VID_BRIGHT_ADJ_STEP); // add.w
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // bra .update_palette
            return;
        }

        // Contrast
        // .dec_contrast_adjust:
        if (Mem.b(a5 + RAWKEY_NUM_4) != 0) {           // tst.b RAWKEY_NUM_4(a5) ; beq .res_contrast_adjust
            Mem.wb(a5 + RAWKEY_NUM_4, 0);              // clr.b RAWKEY_NUM_4(a5)
            if (Mem.w(VidData.Vid_ContrastAdjust_w) <= VID_CONTRAST_ADJ_MIN) { // cmpi.w ; ble .skip_update_palette
                return;
            }
            Mem.ww(VidData.Vid_ContrastAdjust_w,
                    Mem.w(VidData.Vid_ContrastAdjust_w) - VID_CONTRAST_ADJ_STEP); // sub.w
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // bra .update_palette
            return;
        }

        // .res_contrast_adjust:
        if (Mem.b(a5 + RAWKEY_NUM_5) != 0) {           // tst.b RAWKEY_NUM_5(a5) ; beq.s .inc_contrast_adjust
            Mem.wb(a5 + RAWKEY_NUM_5, 0);              // clr.b RAWKEY_NUM_5(a5)
            Mem.ww(VidData.Vid_ContrastAdjust_w, VID_CONTRAST_ADJ_DEF); // move.w #VID_CONTRAST_ADJ_DEF,...
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // bra .update_palette
            return;
        }

        // .inc_contrast_adjust:
        if (Mem.b(a5 + RAWKEY_NUM_6) != 0) {           // tst.b RAWKEY_NUM_6(a5) ; beq.s .dec_gamma
            Mem.wb(a5 + RAWKEY_NUM_6, 0);              // clr.b RAWKEY_NUM_6(a5)
            if (Mem.w(VidData.Vid_ContrastAdjust_w) >= VID_CONTRAST_ADJ_MAX) { // cmpi.w ; bge .skip_update_palette
                return;
            }
            Mem.ww(VidData.Vid_ContrastAdjust_w,
                    Mem.w(VidData.Vid_ContrastAdjust_w) + VID_CONTRAST_ADJ_STEP); // add.w
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // bra .update_palette
            return;
        }

        // Gamma
        // .dec_gamma:
        if (Mem.b(a5 + RAWKEY_NUM_7) != 0) {           // tst.b RAWKEY_NUM_7(a5) ; beq.s .res_gamma
            Mem.wb(a5 + RAWKEY_NUM_7, 0);              // clr.b RAWKEY_NUM_7(a5)
            if (Mem.b(VidData.Vid_GammaLevel_b) == 0) { // tst.b Vid_GammaLevel_b ; beq .skip_update_palette
                return;
            }
            Mem.wb(VidData.Vid_GammaLevel_b, Mem.ub(VidData.Vid_GammaLevel_b) - 1); // sub.b #1,Vid_GammaLevel_b
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // bra.s .update_palette
            return;
        }

        // .res_gamma:
        if (Mem.b(a5 + RAWKEY_NUM_8) != 0) {           // tst.b RAWKEY_NUM_8(a5) ; beq.s .inc_gamma
            Mem.wb(a5 + RAWKEY_NUM_8, 0);              // clr.b RAWKEY_NUM_8(a5)
            Mem.wb(VidData.Vid_GammaLevel_b, 0);       // clr.b Vid_GammaLevel_b
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // bra.s .update_palette
            return;
        }

        // .inc_gamma:
        if (Mem.b(a5 + RAWKEY_NUM_9) != 0) {           // tst.b RAWKEY_NUM_9(a5) ; beq .skip_update_palette
            Mem.wb(a5 + RAWKEY_NUM_9, 0);              // clr.b RAWKEY_NUM_9(a5)
            if ((byte) Mem.b(VidData.Vid_GammaLevel_b) >= 8) { // cmpi.b #8,Vid_GammaLevel_b ; bge.s .skip
                return;
            }
            Mem.wb(VidData.Vid_GammaLevel_b, Mem.ub(VidData.Vid_GammaLevel_b) + 1); // add.b #1,Vid_GammaLevel_b
            // .update_palette:
            Mem.wb(VidData.Vid_UpdatePalette_b, 0xFF); // st Vid_UpdatePalette_b
        }
        // .skip_update_palette: rts
    }
}
