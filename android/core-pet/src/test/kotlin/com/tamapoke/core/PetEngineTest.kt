package com.tamapoke.core

import com.tamapoke.core.enums.Ceremony
import com.tamapoke.core.enums.PetMood
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PetEngineTest {
    private val dex = TestFixtures.dex
    private val bulbasaur = PetState(speciesId = 1, ageMinutes = 100)

    @Test
    fun `awake tick decays fullness by 2 and energy by 1`() {
        val (s, _) = PetEngine.tickOnce(bulbasaur, dex, Random(1))
        assertEquals(bulbasaur.fullness - 2, s.fullness)
        assertEquals(bulbasaur.energy - 1, s.energy)
        assertEquals(bulbasaur.ageMinutes + 1, s.ageMinutes)
    }

    @Test
    fun `sleeping tick restores energy instead of draining it`() {
        val sleepy = bulbasaur.copy(sleeping = true, energy = 50)
        val (s, _) = PetEngine.tickOnce(sleepy, dex, Random(1))
        assertEquals(56, s.energy)
    }

    @Test
    fun `ceremony freezes ticking`() {
        val dying = bulbasaur.copy(ceremony = Ceremony.FAREWELL)
        val (s, _) = PetEngine.tickOnce(dying, dex, Random(1))
        assertEquals(dying, s)
    }

    @Test
    fun `egg hatches after 3 taps`() {
        var egg = PetState(speciesId = -1, eggTarget = 1)
        egg = PetEngine.eggTap(egg, dex, Random(1))
        assertTrue(egg.isEgg)
        egg = PetEngine.eggTap(egg, dex, Random(1))
        assertTrue(egg.isEgg)
        egg = PetEngine.eggTap(egg, dex, Random(1))
        assertFalse(egg.isEgg)
        assertEquals(1, egg.speciesId)
        assertTrue(egg.isRegistered(1))
    }

    @Test
    fun `egg hatches on its own after 3 game minutes if untouched`() {
        var egg = PetState(speciesId = -1, eggTarget = 1, ageMinutes = 1)
        repeat(2) { egg = PetEngine.tickOnce(egg, dex, Random(1)).state }
        assertFalse(egg.isEgg)
    }

    @Test
    fun `evolves only once level and all stats requirements are met`() {
        val notReady = bulbasaur.copy(ageMinutes = 0, fullness = 100, joy = 100, energy = 100, hygiene = 100)
        assertFalse(PetEngine.canEvolveNow(notReady, dex))

        val ready = bulbasaur.copy(ageMinutes = 16 * 60L, fullness = 100, joy = 100, energy = 100, hygiene = 100)
        assertTrue(PetEngine.canEvolveNow(ready, dex))
        val evolved = PetEngine.evolve(ready, dex, Random(1))
        assertEquals(2, evolved.speciesId)
        assertEquals(1, evolved.prevSpeciesId)
        assertTrue(evolved.isRegistered(2))
    }

    @Test
    fun `care mistake fires once when a stat bottoms out, with cooldown`() {
        var s = bulbasaur.copy(fullness = 12, joy = 50, energy = 50, hygiene = 50, mistakeCooldown = 0)
        s = PetEngine.tickOnce(s, dex, Random(2)).state
        assertEquals(1, s.careMistakes)
        // still within the 30-tick cooldown: no second mistake even though a stat is still low
        s = PetEngine.tickOnce(s, dex, Random(2)).state
        assertEquals(1, s.careMistakes)
    }

    @Test
    fun `offline catch-up decays gently down to a floor of 15 and caps at two weeks`() {
        val start = bulbasaur.copy(lastSeenEpochSeconds = 0L)
            .let { PetEngine.setClock(it, 1_000_000L) }
        val threeDaysLater = 1_000_000L + 3L * 24 * 3600
        val caught = PetEngine.advanceOffline(start, dex, threeDaysLater, Random(1))
        assertTrue(caught.fullness >= 15)
        assertEquals(threeDaysLater, caught.lastSeenEpochSeconds)
    }

    @Test
    fun `feeding a favorite berry grants a bigger bonus and reveals it`() {
        // speciesId 1 % 3 == 1, so color 1 is bulbasaur's favorite berry
        val fed = PetEngine.feedBerry(bulbasaur.copy(lastSeenEpochSeconds = 86400L), 1)
        assertTrue(fed.berryKnown)
        assertEquals(minOf(100, bulbasaur.fullness + 35), fed.fullness)
    }

    @Test
    fun `mood reflects sleeping, eating and low stats`() {
        assertEquals(PetMood.SLEEPING, PetEngine.mood(bulbasaur.copy(sleeping = true), eating = false))
        assertEquals(PetMood.EATING, PetEngine.mood(bulbasaur, eating = true))
        assertEquals(PetMood.SAD, PetEngine.mood(bulbasaur.copy(fullness = 10), eating = false))
        assertEquals(PetMood.HAPPY, PetEngine.mood(bulbasaur, eating = false))
    }

    @Test
    fun `eevee evolves into whichever branch is still unregistered`() {
        var s = PetState(speciesId = 133, ageMinutes = 30 * 60L, fullness = 100, joy = 100, energy = 100, hygiene = 100)
        s = s.copy(dexRegistered = setOf(133, 134, 135)) // only Flareon (136) missing
        val evolved = PetEngine.evolve(s, dex, Random(1))
        assertEquals(136, evolved.speciesId)
    }

    @Test
    fun `first egg always rolls a classic starter`() {
        val fresh = PetState()
        val target = PetEngine.pickEggSpecies(fresh, dex, Random(1))
        assertTrue(target in dex.classicStarters)
    }
}
