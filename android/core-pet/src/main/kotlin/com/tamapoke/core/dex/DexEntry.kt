package com.tamapoke.core.dex

import com.tamapoke.core.enums.BattleType
import com.tamapoke.core.enums.Rarity

/** One row of the Pokedex, mirroring dex.h's DexEntry / dex.json (generated from tools/dex_data.py). */
data class DexEntry(
    val id: Int,
    val slug: String,
    val name: String,
    val type: String,
    val accent: String,
    val evolvesTo: Int, // 0 = final form
    val evolveLevel: Int,
    val rarity: Rarity,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val baseSpe: Int,
    val biome: Int,
    /** Real Gen-1 battle typing (distinct from [type], which is just the UI accent/biome key). */
    val battleType1: BattleType? = null,
    val battleType2: BattleType? = null,
    /** Localized display names by language code (es/en/fr/de/it/pt); falls back to [name] if absent. */
    val localizedNames: Map<String, String> = emptyMap(),
) {
    fun displayName(lang: String): String = localizedNames[lang] ?: name
}
