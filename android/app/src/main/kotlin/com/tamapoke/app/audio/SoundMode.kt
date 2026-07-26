package com.tamapoke.app.audio

/** Replaces the original simple on/off toggle with the expanded fork's 4-step volume control. */
enum class SoundMode(val gain: Float) {
    OFF(0f),
    LOW(0.35f),
    MED(0.7f),
    FULL(1.0f),
}
