package com.tamapoke.core

import com.tamapoke.core.enums.Ceremony
import com.tamapoke.core.enums.Medal

/** One-shot occurrences emitted by [PetEngine] calls, for the UI layer to react to (SFX, celebrations). */
sealed class PetEvent {
    data object LevelUp : PetEvent()
    data class MedalsGained(val medals: Set<Medal>) : PetEvent()
    data object Hatched : PetEvent()
    data object Evolved : PetEvent()
    data class CeremonyStarted(val kind: Ceremony) : PetEvent()
}
