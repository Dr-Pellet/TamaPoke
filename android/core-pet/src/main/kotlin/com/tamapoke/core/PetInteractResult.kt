package com.tamapoke.core

/** What interactPet() actually changed (ported from the expanded fork's PetInteractResult bitmask). */
data class PetInteractResult(
    val joyGained: Boolean = false,
    val bondGained: Boolean = false,
    val energyGained: Boolean = false,
) {
    val isEmpty: Boolean get() = !joyGained && !bondGained && !energyGained
}
