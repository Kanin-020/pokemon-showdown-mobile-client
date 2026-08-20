package com.pokemonshowdown.app;

import android.media.AudioManager;

/**
 * Manages audio mute/unmute for the STREAM_MUSIC stream.
 * Saves and restores volume level across mute cycles.
 */
public class AudioController {

    private final AudioManager audioManager;
    private int savedVolume = -1;

    public AudioController(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    /** Mutes audio, saving the current volume for later restore. */
    public void mute() {
        if (audioManager == null) return;
        if (savedVolume < 0) {
            savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
    }

    /** Restores previously saved volume, or sets to max if no save exists. */
    public void unmute() {
        if (audioManager == null) return;
        if (savedVolume >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0);
            savedVolume = -1;
        } else {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);
        }
    }

    /** Applies the given mute state. */
    public void applyState(boolean muted) {
        if (muted) mute(); else unmute();
    }
}
