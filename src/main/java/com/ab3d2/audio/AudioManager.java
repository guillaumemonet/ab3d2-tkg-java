package com.ab3d2.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.openal.ALC10.*;

/**
 * Gestionnaire audio OpenAL.
 * Stub pour l'instant — sera complété avec le chargement des modules Amiga (PT/MOD/SVVX).
 */
public class AudioManager {

    private static final Logger log = LoggerFactory.getLogger(AudioManager.class);

    private long device;
    private long context;
    private boolean initialized = false;

    public void init() {
        try {
            device = alcOpenDevice((java.nio.ByteBuffer) null);
            if (device == 0L) {
                log.warn("No OpenAL device found (audio disabled)");
                return;
            }

            ALCCapabilities alcCaps = ALC.createCapabilities(device);
            context = alcCreateContext(device, (int[]) null);
            alcMakeContextCurrent(context);
            AL.createCapabilities(alcCaps);

            initialized = true;
            log.info("OpenAL initialized");
        } catch (Exception e) {
            log.warn("OpenAL init failed (audio disabled): {}", e.getMessage());
        }
    }

    public void playMusic(String track) {
        if (!initialized) return;
        log.debug("Play music: {} (not implemented yet)", track);
    }

    public void stopMusic() {
        if (!initialized) return;
    }

    public void playSound(String sfx) {
        if (!initialized) return;
        log.debug("Play SFX: {} (not implemented yet)", sfx);
    }

    public void destroy() {
        if (!initialized) return;
        alcDestroyContext(context);
        alcCloseDevice(device);
    }
}
