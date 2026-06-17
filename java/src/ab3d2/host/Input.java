package ab3d2.host;

import ab3d2.Mem;

import static ab3d2.bss.TablesBss.KeyMap_vb;
import static ab3d2.modules.RawKeyMacros.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Couche hôte : équivalent de hires.s::key_interrupt. Installe un callback clavier
 * GLFW qui traduit la touche physique en « rawkey » Amiga et écrit l'état dans
 * KeyMap_vb (0xFF = enfoncée, 0 = relâchée) — exactement ce que lisent les routines
 * de contrôle joueur (player.s via Plr1control/Plr2control, appelées par dosomething).
 *
 * Le mapping est PHYSIQUE (la touche W du clavier hôte → RAWKEY_W) ; les bindings de
 * jeu (forward_key=W, fire_key=CTRL, …) sont appliqués côté moteur sur ces rawkeys.
 *
 * SOURIS : le matériel Amiga expose JOY0DAT, un compteur quadrature 8 bits par axe
 * (octet bas = X, octet haut = Y) que Sys_ReadMouse diffère contre sa valeur précédente.
 * On émule ce compteur en y accumulant les déplacements relatifs du curseur GLFW
 * (mode curseur capturé = visée FPS), avec une sensibilité < 1 (souris modernes à haute
 * résolution ≈ bien plus de pas que les ~200/pouce d'une souris Amiga). Boutons gauche/
 * droit → CustomChips.mouseLeftPressed/RightPressed (tir / arme suivante).
 */
public final class Input {

    /** Sensibilité : facteur appliqué aux pixels GLFW avant accumulation dans le compteur Paula. */
    private static final double MOUSE_SENSITIVITY = 0.30;

    private static int mouseCounterX, mouseCounterY; // compteurs quadrature 8 bits (joy0dat)
    private static double accX, accY;                // accumulateurs sous-pixel
    private static double lastX, lastY;
    private static boolean haveLast;

    /** File de touches enfoncées (edge-trigger) consommée par key_readkey du menu. */
    private static final java.util.ArrayDeque<Integer> keyQueue = new java.util.ArrayDeque<>();
    /** Dernier rawkey enfoncé (mnu_getrawvalue). */
    public static volatile int lastpressed = 0;

    private Input() {
    }

    /** key_readkey : retire et renvoie le prochain rawkey enfoncé (0 si rien). */
    public static synchronized int keyReadKey() {
        return keyQueue.isEmpty() ? 0 : keyQueue.poll();
    }

    /** Vide la file de touches (Sys_ClearKeyboard côté menu). */
    public static synchronized void clearKeyQueue() {
        keyQueue.clear();
        lastpressed = 0;
    }

    /** Installe les callbacks clavier + souris sur la fenêtre GLFW. */
    public static void install(long window) {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            int raw = mapKey(key);
            if (raw < 0) {
                return;
            }
            if (action == GLFW_PRESS) {
                Mem.wb(KeyMap_vb + raw, 0xFF);
                synchronized (Input.class) {                  // file edge-trigger pour key_readkey (menu)
                    keyQueue.add(raw);
                    lastpressed = raw;
                }
            } else if (action == GLFW_RELEASE) {
                Mem.wb(KeyMap_vb + raw, 0);
            }
            // GLFW_REPEAT : ignoré (déjà 0xFF).
            // La touche « arme suivante » (défaut \) : plr_MouseControl écrase KeyMap[next_weapon]
            // depuis la droite souris chaque frame → la touche clavier seule serait sans effet.
            // On la route donc vers le même signal (mouseRightPressed) pour qu'elle marche aussi.
            if (raw == Mem.ub(ab3d2.ControlloopData.next_weapon_key) && action != GLFW_REPEAT) {
                CustomChips.mouseRightPressed = (action == GLFW_PRESS);
            }
        });

        // Souris capturée (visée FPS) + mouvement brut si supporté.
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (glfwRawMouseMotionSupported()) {
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }

        // Déplacement → accumule dans le compteur quadrature joy0dat.
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (!haveLast) {
                lastX = xpos; lastY = ypos; haveLast = true;
                return;
            }
            accX += (xpos - lastX) * MOUSE_SENSITIVITY;
            accY += (ypos - lastY) * MOUSE_SENSITIVITY;
            lastX = xpos; lastY = ypos;
            int idx = (int) accX; accX -= idx;
            int idy = (int) accY; accY -= idy;
            mouseCounterX = (mouseCounterX + idx) & 0xFF;
            mouseCounterY = (mouseCounterY + idy) & 0xFF;
            CustomChips.joy0dat = (mouseCounterY << 8) | mouseCounterX; // octet haut=Y, bas=X
        });

        // Boutons (actifs bas sur le matériel ; CustomChips gère l'inversion).
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            boolean pressed = action == GLFW_PRESS;
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                CustomChips.mouseLeftPressed = pressed;   // tir (fire_key)
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                CustomChips.mouseRightPressed = pressed;  // arme suivante (next_weapon_key)
            }
        });
    }

    /** Touche GLFW physique → rawkey Amiga (-1 = non mappée). */
    private static int mapKey(int k) {
        switch (k) {
            // Déplacement / actions (bindings par défaut)
            case GLFW_KEY_W: return RAWKEY_W;             // forward
            case GLFW_KEY_S: return RAWKEY_S;             // backward
            case GLFW_KEY_A: return RAWKEY_A;             // sidestep left
            case GLFW_KEY_D: return RAWKEY_D;             // sidestep right
            case GLFW_KEY_C: return RAWKEY_C;             // duck
            case GLFW_KEY_F: return RAWKEY_F;             // operate
            case GLFW_KEY_L: return RAWKEY_L;             // look behind (regarder derrière)
            case GLFW_KEY_LEFT:  return RAWKEY_LEFT;      // turn left
            case GLFW_KEY_RIGHT: return RAWKEY_RIGHT;     // turn right
            case GLFW_KEY_UP:    return RAWKEY_UP;
            case GLFW_KEY_DOWN:  return RAWKEY_DOWN;
            case GLFW_KEY_SPACE: return RAWKEY_SPACEBAR;  // jump / valider menu
            case GLFW_KEY_ENTER: return RAWKEY_ENTER;     // valider menu
            case GLFW_KEY_LEFT_CONTROL:
            case GLFW_KEY_RIGHT_CONTROL: return RAWKEY_CTRL; // fire
            case GLFW_KEY_LEFT_SHIFT:  return RAWKEY_LSHIFT;  // run
            case GLFW_KEY_RIGHT_SHIFT: return RAWKEY_RSHIFT;
            case GLFW_KEY_LEFT_ALT:  return RAWKEY_LALT;   // force sidestep
            case GLFW_KEY_RIGHT_ALT: return RAWKEY_RALT;
            case GLFW_KEY_EQUAL:     return RAWKEY_EQUAL;       // look up
            case GLFW_KEY_MINUS:     return RAWKEY_UNDERSCORE;  // look down
            case GLFW_KEY_SEMICOLON: return RAWKEY_SEMICOLON;   // centre view
            case GLFW_KEY_BACKSLASH: return RAWKEY_BSLASH;      // next weapon

            // Touches système / options
            case GLFW_KEY_ESCAPE: return RAWKEY_ESC;
            case GLFW_KEY_P:      return RAWKEY_P;          // pause
            case GLFW_KEY_TAB:    return RAWKEY_TAB;        // carte
            case GLFW_KEY_F3:     return RAWKEY_F3;
            case GLFW_KEY_F4:     return RAWKEY_F4;
            case GLFW_KEY_F5:     return RAWKEY_F5;
            case GLFW_KEY_F6:     return RAWKEY_F6;
            case GLFW_KEY_F7:     return RAWKEY_F7;          // limite FPS (cycle)
            case GLFW_KEY_F10:    return RAWKEY_F10;         // bascule plein écran / petit écran HUD

            // Rangée chiffres du haut (sélection directe d'arme : 1..9, 0)
            case GLFW_KEY_1: return RAWKEY_1;
            case GLFW_KEY_2: return RAWKEY_2;
            case GLFW_KEY_3: return RAWKEY_3;
            case GLFW_KEY_4: return RAWKEY_4;
            case GLFW_KEY_5: return RAWKEY_5;
            case GLFW_KEY_6: return RAWKEY_6;
            case GLFW_KEY_7: return RAWKEY_7;
            case GLFW_KEY_8: return RAWKEY_8;
            case GLFW_KEY_9: return RAWKEY_9;
            case GLFW_KEY_0: return RAWKEY_0;

            // Pavé numérique (défilement carte)
            case GLFW_KEY_KP_1: return RAWKEY_NUM_1;
            case GLFW_KEY_KP_2: return RAWKEY_NUM_2;
            case GLFW_KEY_KP_3: return RAWKEY_NUM_3;
            case GLFW_KEY_KP_4: return RAWKEY_NUM_4;
            case GLFW_KEY_KP_5: return RAWKEY_NUM_5;
            case GLFW_KEY_KP_6: return RAWKEY_NUM_6;
            case GLFW_KEY_KP_7: return RAWKEY_NUM_7;
            case GLFW_KEY_KP_8: return RAWKEY_NUM_8;
            case GLFW_KEY_KP_9: return RAWKEY_NUM_9;
            default: return -1;
        }
    }
}
