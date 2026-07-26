package com.tamapoke.core.enums

/**
 * The 18 standard type-chart types, ported from the ShadowEnemy expanded
 * fork's battle.cpp (typeRelation()). Distinct from [Rarity]'s biome/accent
 * "tipo" field on [com.tamapoke.core.dex.DexEntry] - this is purely for
 * battle type-effectiveness.
 */
enum class BattleType {
    NORMAL, FIRE, WATER, ELECTRIC, GRASS, ICE, FIGHTING, POISON, GROUND,
    FLYING, PSYCHIC, BUG, ROCK, GHOST, DRAGON, DARK, STEEL, FAIRY;

    companion object {
        fun fromSlug(slug: String?): BattleType? = when (slug) {
            "normal" -> NORMAL
            "fire" -> FIRE
            "water" -> WATER
            "electric" -> ELECTRIC
            "grass" -> GRASS
            "ice" -> ICE
            "fighting" -> FIGHTING
            "poison" -> POISON
            "ground" -> GROUND
            "flying" -> FLYING
            "psychic" -> PSYCHIC
            "bug" -> BUG
            "rock" -> ROCK
            "ghost" -> GHOST
            "dragon" -> DRAGON
            "dark" -> DARK
            "steel" -> STEEL
            "fairy" -> FAIRY
            else -> null
        }
    }
}
