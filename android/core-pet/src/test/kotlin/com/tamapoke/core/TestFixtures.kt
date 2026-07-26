package com.tamapoke.core

import com.tamapoke.core.dex.DexEntry
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.BattleType
import com.tamapoke.core.enums.Rarity

/** Small hand-built dex used only by unit tests, so core-pet tests never depend on Android assets. */
object TestFixtures {
    // Bulbasaur -> Ivysaur -> Venusaur (real gen-1 numbers/stats/levels), plus Eevee -> {Vaporeon, Jolteon, Flareon}.
    val dex = DexTable(
        entries = listOf(
            DexEntry(
                1, "bulbasaur", "BULBASAUR", "planta", "#3C49", 2, 16, Rarity.COMMON, 45, 49, 49, 45, 2,
                battleType1 = BattleType.GRASS, battleType2 = BattleType.POISON,
            ),
            DexEntry(2, "ivysaur", "IVYSAUR", "planta", "#3C49", 3, 32, Rarity.EVO, 60, 62, 63, 60, 2),
            DexEntry(3, "venusaur", "VENUSAUR", "planta", "#3C49", 0, 0, Rarity.EVO, 80, 82, 83, 80, 2),
            DexEntry(
                4, "charmander", "CHARMANDER", "fuego", "#EA87", 5, 16, Rarity.COMMON, 39, 52, 43, 65, 3,
                battleType1 = BattleType.FIRE,
            ),
            // evolvesTo=134 is just a placeholder like the real dex.h: the actual branch
            // (134/135/136) is chosen at runtime by PetEngine.evolve()'s Eevee special-case.
            DexEntry(133, "eevee", "EEVEE", "normal", "#8C4D", 134, 30, Rarity.COMMON, 55, 55, 50, 55, 0),
            DexEntry(134, "vaporeon", "VAPOREON", "agua", "#4C98", 0, 0, Rarity.EVO, 130, 65, 60, 65, 1),
            DexEntry(135, "jolteon", "JOLTEON", "electrico", "#BCA1", 0, 0, Rarity.EVO, 65, 65, 60, 130, 0),
            DexEntry(136, "flareon", "FLAREON", "fuego", "#EA87", 0, 0, Rarity.EVO, 65, 130, 60, 65, 3),
            DexEntry(150, "mewtwo", "MEWTWO", "psiquico", "#D452", 0, 0, Rarity.LEGENDARY, 106, 110, 90, 130, 0),
        ),
        eeveeId = 133,
        eeveeEvolutions = listOf(134, 135, 136),
        classicStarters = listOf(1, 4),
    )
}
