package ab3d2.c;

import ab3d2.Mem;
import ab3d2.modules.Sys;

import static ab3d2.bss.SystemBss.Sys_FrameTimeECV_q;
import static ab3d2.bss.LevelBss.Lvl_DataPtr_l;
import static ab3d2.bss.VidBss.Vid_FullScreen_b;
import static ab3d2.bss.VidBss.Vid_LetterBoxMarginHeight_w;
import static ab3d2.bss.VidBss.Vid_FastBufferPtr_l;
import static ab3d2.bss.VidBss.Vid_ScreenHeight;
import static ab3d2.data.VidData.SMALL_YPOS;
import static ab3d2.ControlloopData.Prefs_ShowMessages_b;

/**
 * Traduction littérale de ab3d2_source/c/message.c.
 *
 * Système de messages à l'écran : buffer circulaire de 8 lignes, défilement
 * temporisé (EClock), reflow proportionnel et rendu chunky (RTG/fullscreen) ou
 * planar (AGA). Le rendu de texte délègue à DrawC (draw.c §1, déjà traduit).
 *
 * msg_Buffer (struct module statique) est alloué en mémoire plate (84 octets =
 * 21 longs, aligné 4) ; layout ci-dessous. EClockVal = {ev_hi@0, ev_lo@4}.
 * Sys_CheckTimeGE/Sys_AddTime (inlines system.h) et Sys_EClockRate sont dans SystemC.
 * Draw_ClearRect reste un stub (graphics.library hôte) → Msg_RenderSmallScreenRTG
 * lèvera tant que draw.c §3 n'est pas fait ; le reste est fonctionnel.
 */
public final class Message {

    private static final int MSG_LINE_BUFFER_EXP = 3;
    private static final int MSG_LINE_BUFFER_SIZE = 1 << MSG_LINE_BUFFER_EXP; // 8
    private static final int MSG_LINE_BUFFER_MASK = MSG_LINE_BUFFER_SIZE - 1; // 7

    private static final int MSG_LENGTH_MASK = 0x3FFF;
    private static final int MSG_TAG_SHIFT = 14;

    // message.h
    private static final int MSG_SINGLE_LINE_LENGTH = 80;
    private static final int MSG_MAX_LENGTH = MSG_SINGLE_LINE_LENGTH * 2; // 160
    private static final int MSG_MAX_CUSTOM = 10;
    private static final int MSG_SCROLL_PERIOD_MS = 2000;
    private static final int MSG_DEDUPLICATION_PERIOD_MS = 2000;
    private static final int MSG_MAX_LINES_SMALL = 4;

    // screen.h / draw.h
    private static final int SCREEN_WIDTH = 320;
    private static final int SMALL_HEIGHT = 160;
    private static final int HUD_BORDER_WIDTH = 16;
    private static final int MAX_PROP_CHAR_WIDTH = 7;
    private static final int DRAW_MSG_CHAR_W = 8;
    private static final int DRAW_MSG_CHAR_H = 8;
    private static final int DRAW_TEXT_MARGIN = 4;
    private static final int DRAW_TEXT_Y_SPACING = 2;

    // -- layout de msg_Buffer (84 octets) --
    private static final int MB_nextTickECV = 0;            // EClockVal (8)
    private static final int MB_nextDuplicateTickECV = 8;   // EClockVal (8)
    private static final int MB_tickPeriod = 16;            // ULONG
    private static final int MB_deduplicationPeriod = 20;   // ULONG
    private static final int MB_lineTextPtrs = 24;          // const char*[8] (32)
    private static final int MB_lineLengths = 56;           // UWORD[8] (16)
    private static final int MB_lastMessagePtr = 72;        // const char*
    private static final int MB_lineNumber = 76;            // WORD
    private static final int MB_guaranteedFitFull = 78;     // UWORD
    private static final int MB_guaranteedFitSmall = 80;    // UWORD
    private static final int MB_redrawCount = 82;           // UBYTE
    private static final int MB_linesVis = 83;              // UBYTE
    private static final int MSG_BUFFER_SIZEOF = 84;        // 21 longs

    private static final int msg_Buffer = Mem.alloc(MSG_BUFFER_SIZEOF);

    /** Pens par tag : NARRATIVE, DEFAULT, OPTIONS, OTHER. */
    private static final int[] msg_TagPens = { 255, 254, 125, 252 };

    private Message() {
    }

    /** Msg_Enabled (message.h inline) — return Prefs_ShowMessages_b. */
    public static boolean Msg_Enabled() {
        return Mem.ub(ab3d2.ControlloopData.Prefs_ShowMessages_b) != 0;
    }

    /** Msg_SmallScreenNeedsRedraw — redrawCount>0 && linesVis. */
    public static boolean Msg_SmallScreenNeedsRedraw() {
        return Mem.ub(msg_Buffer + MB_redrawCount) > 0 && Mem.ub(msg_Buffer + MB_linesVis) != 0;
    }

    /** msg_NextLineNumber — numéro de ligne suivant (cyclique selon plein/petit écran). */
    private static int msg_NextLineNumber(int lineNumber) {
        if (Mem.ub(Vid_FullScreen_b) != 0) {                    // if (Vid_FullScreen_b)
            return (lineNumber + 1) & MSG_LINE_BUFFER_MASK;     //   return (lineNumber + 1) & MSG_LINE_BUFFER_MASK;
        }
        return (lineNumber < MSG_MAX_LINES_SMALL) ? lineNumber + 1 : 0; // (lineNumber < MSG_MAX_LINES_SMALL) ? lineNumber + 1 : 0
    }

    /** msg_PushLineRaw — pousse une ligne brute dans le buffer circulaire. */
    private static void msg_PushLineRaw(int textPtr, int lengthAndTag) {
        int lineNumber = msg_NextLineNumber(Mem.w(msg_Buffer + MB_lineNumber)); // WORD lineNumber = msg_NextLineNumber(msg_Buffer.lineNumber)
        Mem.wl(msg_Buffer + MB_lineTextPtrs + lineNumber * 4, textPtr);         // msg_Buffer.lineTextPtrs[lineNumber] = textPtr
        Mem.ww(msg_Buffer + MB_lineLengths + lineNumber * 2, lengthAndTag);     // msg_Buffer.lineLengths[lineNumber] = lengthAndTag
        Mem.ww(msg_Buffer + MB_lineNumber, lineNumber);                         // msg_Buffer.lineNumber = lineNumber
        if (textPtr != 0) {                                                     // if (textPtr)
            Mem.wb(msg_Buffer + MB_linesVis, 1);                                //   msg_Buffer.linesVis = 1
        }
    }

    /** Msg_Init — initialise le système de messages (début de niveau). */
    public static void Msg_Init() {
        Sys.Sys_MemFillLong(msg_Buffer, 0, MSG_BUFFER_SIZEOF / 4);              // Sys_MemFillLong(&msg_Buffer, 0, sizeof/sizeof(ULONG))

        Mem.ww(msg_Buffer + MB_lineNumber, MSG_LINE_BUFFER_SIZE - 1);           // lineNumber = MSG_LINE_BUFFER_SIZE - 1
        Mem.wb(msg_Buffer + MB_redrawCount, 1);                                 // redrawCount = 1

        Mem.ww(msg_Buffer + MB_guaranteedFitFull, (SCREEN_WIDTH / MAX_PROP_CHAR_WIDTH) - 2); // (SCREEN_WIDTH / MAX_PROP_CHAR_WIDTH) - 2
        Mem.ww(msg_Buffer + MB_guaranteedFitSmall, ((SCREEN_WIDTH - (HUD_BORDER_WIDTH * 2)) / MAX_PROP_CHAR_WIDTH) - 2);

        Mem.wl(msg_Buffer + MB_tickPeriod, (SystemC.Sys_EClockRate * MSG_SCROLL_PERIOD_MS) / 1000);          // tickPeriod
        Mem.wl(msg_Buffer + MB_deduplicationPeriod, (SystemC.Sys_EClockRate * MSG_DEDUPLICATION_PERIOD_MS) / 1000); // deduplicationPeriod

        int levelTextPtr = Mem.l(Lvl_DataPtr_l);                                // char* levelTextPtr = (char*)Lvl_DataPtr_l
        if (levelTextPtr != 0) {                                                // if (levelTextPtr)
            for (int i = 0; i < MSG_MAX_CUSTOM; ++i, levelTextPtr += MSG_MAX_LENGTH) {
                if (DrawC.Draw_IsPrintable(Mem.ub(levelTextPtr + MSG_SINGLE_LINE_LENGTH - 1))
                    && DrawC.Draw_IsPrintable(Mem.ub(levelTextPtr + MSG_SINGLE_LINE_LENGTH))) {
                    msg_NudgeString(levelTextPtr + MSG_SINGLE_LINE_LENGTH, MSG_SINGLE_LINE_LENGTH - 1);
                }
                msg_CompactString(levelTextPtr, MSG_MAX_LENGTH);
            }
        }
    }

    /** Msg_PushLine — pousse une ligne, en la segmentant si trop longue. */
    public static void Msg_PushLine(int textPtr, int lengthAndTag) {
        if (Mem.ub(Prefs_ShowMessages_b) == 0) {                                // if (!Prefs_ShowMessages_b) return;
            return;
        }

        int textLength = lengthAndTag & MSG_LENGTH_MASK;                        // UWORD textLength = lengthAndTag & MSG_LENGTH_MASK
        int maxFit = Mem.ub(Vid_FullScreen_b) != 0
            ? Mem.uw(msg_Buffer + MB_guaranteedFitFull)
            : Mem.uw(msg_Buffer + MB_guaranteedFitSmall);                       // maxFit = Vid_FullScreen_b ? ...Full : ...Small

        Mem.wb(msg_Buffer + MB_redrawCount, 1);                                 // redrawCount = 1

        if (textLength <= maxFit) {                                            // if (textLength <= maxFit)
            msg_PushLineRaw(textPtr, lengthAndTag);                            //   msg_PushLineRaw(textPtr, lengthAndTag)
        } else {
            int[] nextTextPtr = { textPtr };                                   // const char* nextTextPtr = textPtr
            int lines = MSG_MAX_LINES_SMALL;                                   // int lines = MSG_MAX_LINES_SMALL
            int textTag = lengthAndTag & ~MSG_LENGTH_MASK;                     // UWORD textTag = lengthAndTag & ~MSG_LENGTH_MASK
            maxFit = Mem.ub(Vid_FullScreen_b) != 0
                ? SCREEN_WIDTH - (2 * DRAW_MSG_CHAR_W)
                : SCREEN_WIDTH - (2 * (HUD_BORDER_WIDTH + DRAW_MSG_CHAR_W));    // maxFit = Vid_FullScreen_b ? ... : ...
            do {
                int fitLength = DrawC.Draw_CalcPropTextSplit(nextTextPtr, textLength, maxFit); // Draw_CalcPropTextSplit(&nextTextPtr, textLength, maxFit)
                msg_PushLineRaw(textPtr, fitLength | textTag);                 // msg_PushLineRaw(textPtr, fitLength|textTag)
                textPtr = nextTextPtr[0];                                      // textPtr = nextTextPtr
                textLength -= fitLength;                                       // textLength -= fitLength
            } while (nextTextPtr[0] != 0 && lines-- != 0);                     // while (nextTextPtr && lines--)
        }
        Mem.wl(msg_Buffer + MB_lastMessagePtr, 0);                             // msg_Buffer.lastMessagePtr = NULL
    }

    /** Msg_PushLineDedupLast — pousse si différent du dernier (ou délai écoulé). */
    public static void Msg_PushLineDedupLast(int textPtr, int lengthAndTag) {
        if (Mem.ub(Prefs_ShowMessages_b) != 0
            && (textPtr != Mem.l(msg_Buffer + MB_lastMessagePtr)
                || SystemC.Sys_CheckTimeGE(Sys_FrameTimeECV_q, msg_Buffer + MB_nextDuplicateTickECV))) {
            // nextDuplicateTickECV = Sys_FrameTimeECV_q[0]  (copie 8 octets)
            Mem.wl(msg_Buffer + MB_nextDuplicateTickECV, Mem.l(Sys_FrameTimeECV_q));
            Mem.wl(msg_Buffer + MB_nextDuplicateTickECV + 4, Mem.l(Sys_FrameTimeECV_q + 4));
            SystemC.Sys_AddTime(msg_Buffer + MB_nextDuplicateTickECV, Mem.l(msg_Buffer + MB_deduplicationPeriod)); // Sys_AddTime(&nextDuplicateTickECV, deduplicationPeriod)
            Msg_PushLine(textPtr, lengthAndTag);                               // Msg_PushLine(textPtr, lengthAndTag)
            Mem.wl(msg_Buffer + MB_lastMessagePtr, textPtr);                   // lastMessagePtr = textPtr
            Mem.wb(msg_Buffer + MB_redrawCount, 1);                            // redrawCount = 1
        }
    }

    /** Msg_PullLast — TODO (vide dans l'original). */
    public static void Msg_PullLast() {
    }

    /** Msg_Tick — fait défiler le buffer après chaque période. */
    public static void Msg_Tick() {
        if (SystemC.Sys_CheckTimeGE(Sys_FrameTimeECV_q, msg_Buffer + MB_nextTickECV)) { // if (Sys_CheckTimeGE(&Sys_FrameTimeECV_q[0], &nextTickECV))
            Mem.wl(msg_Buffer + MB_nextTickECV, Mem.l(Sys_FrameTimeECV_q));        // nextTickECV = Sys_FrameTimeECV_q[0]
            Mem.wl(msg_Buffer + MB_nextTickECV + 4, Mem.l(Sys_FrameTimeECV_q + 4));
            SystemC.Sys_AddTime(msg_Buffer + MB_nextTickECV, Mem.l(msg_Buffer + MB_tickPeriod)); // Sys_AddTime(&nextTickECV, tickPeriod)
            msg_PushLineRaw(0, 0);                                                 // msg_PushLineRaw(NULL, 0)
            Mem.wb(msg_Buffer + MB_redrawCount, 1);                               // redrawCount = 1
        }
    }

    /** Msg_RenderFullsccreenBuffer — rend le buffer dans le chunky fullscreen. */
    public static void Msg_RenderFullsccreenBuffer() {
        int lastLine = msg_NextLineNumber(Mem.w(msg_Buffer + MB_lineNumber));
        int nextLine = lastLine;
        int yPos = (Mem.w(Vid_LetterBoxMarginHeight_w) + DRAW_TEXT_MARGIN) & 0xFFFF; // UWORD yPos = Vid_LetterBoxMarginHeight_w + DRAW_TEXT_MARGIN
        do {
            if (Mem.l(msg_Buffer + MB_lineTextPtrs + nextLine * 4) != 0) {     // if (lineTextPtrs[nextLine])
                int len = Mem.uw(msg_Buffer + MB_lineLengths + nextLine * 2);
                DrawC.Draw_ChunkyTextProp(
                    Mem.l(Vid_FastBufferPtr_l),                                // Vid_FastBufferPtr_l
                    SCREEN_WIDTH,
                    len & MSG_LENGTH_MASK,
                    Mem.l(msg_Buffer + MB_lineTextPtrs + nextLine * 4),
                    DRAW_TEXT_MARGIN,
                    yPos,
                    msg_TagPens[len >> MSG_TAG_SHIFT]);
                yPos = (yPos + DRAW_MSG_CHAR_H + DRAW_TEXT_Y_SPACING) & 0xFFFF;
            }
            nextLine = msg_NextLineNumber(nextLine);
        } while (nextLine != lastLine);
    }

    /** Msg_RenderFullscreenRTG — rend dans le bitmap RTG fullscreen. */
    public static void Msg_RenderFullscreenRTG(int bmBaseAddr, int bmBytesPerRow) {
        int lastLine = msg_NextLineNumber(Mem.w(msg_Buffer + MB_lineNumber));
        int nextLine = lastLine;
        int yPos = (Mem.w(Vid_LetterBoxMarginHeight_w) + DRAW_TEXT_MARGIN) & 0xFFFF;
        do {
            if (Mem.l(msg_Buffer + MB_lineTextPtrs + nextLine * 4) != 0) {
                int len = Mem.uw(msg_Buffer + MB_lineLengths + nextLine * 2);
                DrawC.Draw_ChunkyTextProp(
                    bmBaseAddr,
                    bmBytesPerRow,
                    len & MSG_LENGTH_MASK,
                    Mem.l(msg_Buffer + MB_lineTextPtrs + nextLine * 4),
                    DRAW_TEXT_MARGIN,
                    yPos,
                    msg_TagPens[len >> MSG_TAG_SHIFT]);
                yPos = (yPos + DRAW_MSG_CHAR_H + DRAW_TEXT_Y_SPACING) & 0xFFFF;
            }
            nextLine = msg_NextLineNumber(nextLine);
        } while (nextLine != lastLine);
    }

    /** Msg_RenderSmallScreenRTG — rend hors zone 3D (RTG, écran réduit). */
    public static void Msg_RenderSmallScreenRTG(int bmBaseAddr, int bmBytesPerRow) {
        int lastLine = msg_NextLineNumber(Mem.w(msg_Buffer + MB_lineNumber));
        int nextLine = lastLine;
        int yPos = (SMALL_HEIGHT + Mem.w(SMALL_YPOS) + DRAW_TEXT_MARGIN) & 0xFFFF; // SMALL_HEIGHT + SMALL_YPOS + DRAW_TEXT_MARGIN

        Mem.wb(msg_Buffer + MB_linesVis, 0);                                   // msg_Buffer.linesVis = 0

        // Efface la zone de texte.
        DrawC.Draw_ClearRect(
            HUD_BORDER_WIDTH,
            yPos,
            SCREEN_WIDTH - HUD_BORDER_WIDTH - 1,
            Mem.w(Vid_ScreenHeight) - HUD_BORDER_WIDTH - 8);

        do {
            if (Mem.l(msg_Buffer + MB_lineTextPtrs + nextLine * 4) != 0) {
                int len = Mem.uw(msg_Buffer + MB_lineLengths + nextLine * 2);
                DrawC.Draw_ChunkyTextProp(
                    bmBaseAddr,
                    bmBytesPerRow,
                    len & MSG_LENGTH_MASK,
                    Mem.l(msg_Buffer + MB_lineTextPtrs + nextLine * 4),
                    DRAW_TEXT_MARGIN + HUD_BORDER_WIDTH,
                    yPos,
                    msg_TagPens[len >> MSG_TAG_SHIFT]);
                yPos = (yPos + DRAW_MSG_CHAR_H + DRAW_TEXT_Y_SPACING) & 0xFFFF;
                Mem.wb(msg_Buffer + MB_linesVis, (Mem.ub(msg_Buffer + MB_linesVis) + 1) & 0xFF); // ++linesVis
            }
            nextLine = msg_NextLineNumber(nextLine);
        } while (nextLine != lastLine);
        Mem.wb(msg_Buffer + MB_redrawCount, (Mem.ub(msg_Buffer + MB_redrawCount) - 1) & 0xFF); // --redrawCount
    }

    /** Msg_RenderSmallScreenPlanar — rend dans un bitplane (AGA). */
    public static void Msg_RenderSmallScreenPlanar(int plane) {
        int lastLine = msg_NextLineNumber(Mem.w(msg_Buffer + MB_lineNumber));
        int nextLine = lastLine;
        int yPos = 0;
        Mem.wb(msg_Buffer + MB_linesVis, 0);                                   // linesVis = 0

        Sys.Sys_MemFillLong(plane, 0, SCREEN_WIDTH * 4);                       // Sys_MemFillLong(plane, 0, SCREEN_WIDTH * 4)
        do {
            if (Mem.l(msg_Buffer + MB_lineTextPtrs + nextLine * 4) != 0) {
                int len = Mem.uw(msg_Buffer + MB_lineLengths + nextLine * 2);
                DrawC.Draw_PlanarTextProp(
                    plane,
                    len & MSG_LENGTH_MASK,
                    Mem.l(msg_Buffer + MB_lineTextPtrs + nextLine * 4),
                    DRAW_TEXT_MARGIN + HUD_BORDER_WIDTH,
                    yPos);
                yPos = (yPos + DRAW_MSG_CHAR_H + DRAW_TEXT_Y_SPACING) & 0xFFFF;
                Mem.wb(msg_Buffer + MB_linesVis, (Mem.ub(msg_Buffer + MB_linesVis) + 1) & 0xFF); // ++linesVis
            }
            nextLine = msg_NextLineNumber(nextLine);
        } while (nextLine != lastLine);
        Mem.wb(msg_Buffer + MB_redrawCount, (Mem.ub(msg_Buffer + MB_redrawCount) - 1) & 0xFF); // --redrawCount
    }

    /** msg_NudgeString — décale d'un caractère vers la droite, insère une espace en tête. */
    private static void msg_NudgeString(int bufferPtr, int bufferLen) {
        int toPtr = bufferPtr + bufferLen;                  // char* toPtr = bufferPtr + bufferLen
        int fromPtr = toPtr - 1;                            // char* fromPtr = toPtr - 1
        while (fromPtr > bufferPtr) {                        // while (fromPtr > bufferPtr)
            --toPtr;
            --fromPtr;
            Mem.wb(toPtr, Mem.ub(fromPtr));                 //   *--toPtr = *--fromPtr
        }
        Mem.wb(bufferPtr, ' ');                             // *bufferPtr = ' '
    }

    /** msg_CompactString — compacte les espaces multiples ; renvoie la nouvelle longueur. */
    private static int msg_CompactString(int bufferPtr, int bufferLen) {
        int readPtr = bufferPtr;                            // char* readPtr = bufferPtr
        int writePtr = bufferPtr;                           // char* writePtr = bufferPtr
        int lastPtr = bufferPtr + bufferLen;                // char* lastPtr = bufferPtr + bufferLen
        boolean skip = true;                                // BOOL skip = TRUE
        int n = bufferLen & 0xFFFF;
        while (n-- != 0) {                                  // while (bufferLen--)
            int charCode = Mem.ub(readPtr);                 // UBYTE charCode = *readPtr++
            readPtr++;
            if (DrawC.Draw_IsPrintable(charCode)) {         // if (Draw_IsPrintable(charCode))
                skip = false;                               //   skip = FALSE
            } else if (!skip) {                             // else if (!skip)
                skip = true;                                //   skip = TRUE
            } else {
                continue;                                   // else continue
            }
            Mem.wb(writePtr, charCode);                     // *writePtr++ = charCode
            writePtr++;
        }
        if (skip) {                                         // if (skip)
            --writePtr;                                     //   --writePtr
        }
        if (writePtr < lastPtr) {                           // if (writePtr < lastPtr)
            Mem.wb(writePtr, 0);                            //   *writePtr = 0
        }
        return (writePtr - bufferPtr) & 0xFFFF;             // return (UWORD)(writePtr - bufferPtr)
    }
}
