package com.tamapoke.core

import com.tamapoke.core.enums.Ceremony
import com.tamapoke.core.enums.Medal

/**
 * Immutable snapshot of a pet's full save state. Mirrors the `Pet` class fields
 * in the original firmware's pet.h, minus purely-visual/UI timer fields
 * (eatUntil/heartUntil/evolveUntil/ceremonyUntil etc.), which belong to the
 * Compose UI layer, not the simulation.
 *
 * [PetEngine] never mutates a PetState in place — every action/tick returns a
 * new copy, so the whole engine is trivially unit-testable.
 */
data class PetState(
    // stats 0..100
    val fullness: Int = 80,
    val joy: Int = 80,
    val energy: Int = 80,
    val hygiene: Int = 100,
    val poops: Int = 0,
    val weight: Int = 0,

    // genes (90..110%) and training (0..100)
    val geneAtk: Int = 100,
    val geneDef: Int = 100,
    val geneSpe: Int = 100,
    val trAtk: Int = 0,
    val trDef: Int = 0,
    val trSpe: Int = 0,

    val berryKnown: Boolean = false,
    val shiny: Boolean = false,
    val ageMinutes: Long = 0,
    val speciesId: Int = -1, // -1 = egg
    val prevSpeciesId: Int = -1,
    val careMistakes: Int = 0,
    val sleeping: Boolean = false,
    val lastSeenEpochSeconds: Long = 0,
    val ceremony: Ceremony = Ceremony.NONE,
    val lastEnd: Ceremony = Ceremony.NONE,

    val dexRegistered: Set<Int> = emptySet(),
    val dexShinyRegistered: Set<Int> = emptySet(),

    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastCareDayEpochDay: Long = 0,
    val bond: Int = 0,
    val nickname: String = "",
    val medals: Set<Medal> = emptySet(),
    val totalMedals: Int = 0,
    val lastMilestone: Int = 0,
    val gameHi: Int = 0,
    val strHi: Int = 0,

    // egg / hatching bookkeeping
    val eggTarget: Int = 1,
    val eggShiny: Boolean = false,
    val eggTaps: Int = 0,
    val starterPick: Boolean = false,

    // internal cooldowns/counters, not user-facing
    val mistakeCooldown: Int = 0,
    val evoDeclinedLevel: Int = 0,
    val farewellDeclinedAgeMinutes: Long = 0,
    val neglectTicks: Int = 0,
    val goodTicks: Int = 0,
    val bondToday: Int = 0,
) {
    val isEgg: Boolean get() = speciesId < 0

    fun isRegistered(dex: Int): Boolean = dex in dexRegistered
    fun isShinyRegistered(dex: Int): Boolean = dex in dexShinyRegistered
    fun lowestStat(): Int = minOf(fullness, joy, energy, hygiene)
    fun level(): Int = (1 + ageMinutes / MINUTES_PER_LEVEL).toInt()
    fun registeredCount(): Int = dexRegistered.size

    companion object {
        const val MINUTES_PER_LEVEL = 60L
        const val FAREWELL_AGE_MIN = 3L * 24 * 60
        const val RUNAWAY_TICKS = 60
        const val OFFLINE_CATCHUP_CAP_MIN = 14L * 24 * 60
    }
}
