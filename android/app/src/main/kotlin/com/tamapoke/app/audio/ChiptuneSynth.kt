package com.tamapoke.app.audio

/**
 * Faithful port of audio.cpp's playTone(): a square wave with a linear
 * attack (first 64 samples) and release (last 96 samples) to avoid clicks,
 * same amplitude and sample rate as the firmware.
 */
object ChiptuneSynth {
    const val SAMPLE_RATE = 16_000
    private const val AMPLITUDE = 5000

    /** Renders one note to mono 16-bit PCM samples. */
    fun render(note: Note): ShortArray {
        val total = SAMPLE_RATE * note.ms / 1000
        val half = if (note.freqHz > 0) SAMPLE_RATE / (2 * note.freqHz) else 0
        val out = ShortArray(total)
        var phase = 0
        var high = true
        for (idx in 0 until total) {
            var s = 0
            if (note.freqHz > 0) {
                s = if (high) AMPLITUDE else -AMPLITUDE
                if (idx < 64) s = s * idx / 64
                else if (idx > total - 96) s = s * (total - idx) / 96
                if (++phase >= half) {
                    phase = 0
                    high = !high
                }
            }
            out[idx] = s.toShort()
        }
        return out
    }

    fun renderSequence(notes: List<Note>): ShortArray {
        val parts = notes.map { render(it) }
        val total = parts.sumOf { it.size }
        val out = ShortArray(total)
        var offset = 0
        for (part in parts) {
            part.copyInto(out, offset)
            offset += part.size
        }
        return out
    }
}
