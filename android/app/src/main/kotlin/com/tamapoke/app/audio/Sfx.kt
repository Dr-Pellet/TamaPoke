package com.tamapoke.app.audio

/** Mirrors audio.h's Sfx enum - order matches SFX[] in audio.cpp. */
enum class Sfx { TAP, EAT, PLAY, HEART, HATCH, EVOLVE, MEDAL, DENY, BYE, LEVEL }

/** One Game Boy-style square-wave note: frequency in Hz (0 = silence) and duration in ms. */
data class Note(val freqHz: Int, val ms: Int)

/** Exact note tables ported from audio.cpp's N_* arrays. */
object SfxTable {
    private val TAP = listOf(Note(880, 35))
    private val EAT = listOf(Note(660, 45), Note(0, 12), Note(660, 45))
    private val PLAY = listOf(Note(784, 45), Note(988, 60))
    private val HEART = listOf(Note(1047, 55), Note(1319, 90))
    private val HATCH = listOf(Note(523, 80), Note(659, 80), Note(784, 110), Note(1047, 170))
    private val EVOLVE = listOf(Note(523, 80), Note(659, 80), Note(784, 80), Note(1047, 90), Note(1319, 230))
    private val MEDAL = listOf(Note(784, 70), Note(0, 25), Note(784, 70), Note(0, 25), Note(1047, 200))
    private val DENY = listOf(Note(300, 110), Note(200, 170))
    private val BYE = listOf(Note(784, 150), Note(659, 150), Note(523, 280))
    private val LEVEL = listOf(Note(784, 70), Note(1047, 130))

    private val TABLE: Map<Sfx, List<Note>> = mapOf(
        Sfx.TAP to TAP, Sfx.EAT to EAT, Sfx.PLAY to PLAY, Sfx.HEART to HEART, Sfx.HATCH to HATCH,
        Sfx.EVOLVE to EVOLVE, Sfx.MEDAL to MEDAL, Sfx.DENY to DENY, Sfx.BYE to BYE, Sfx.LEVEL to LEVEL,
    )

    operator fun get(sfx: Sfx): List<Note> = TABLE.getValue(sfx)
}
