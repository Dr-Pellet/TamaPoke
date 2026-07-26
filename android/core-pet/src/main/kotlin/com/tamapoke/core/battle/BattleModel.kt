package com.tamapoke.core.battle

import com.tamapoke.core.enums.BattleType

/** Ported 1:1 from the ShadowEnemy expanded fork's battle.h/battle.cpp. */
data class BattleStats(
    val atk: Int,
    val def: Int,
    val spe: Int,
    val level: Int,
    val type1: BattleType? = null,
    val type2: BattleType? = null,
)

enum class BattleAction { ATTACK, ATTACK_QUICK, ATTACK_HEAVY, DODGE, REST }

data class BattleRuntime(
    val player: BattleStats,
    val enemy: BattleStats,
    val playerHp: Int,
    val enemyHp: Int,
    val playerMaxHp: Int,
    val enemyMaxHp: Int,
    val round: Int = 0,
    val playerDamageTotal: Int = 0,
    val enemyDamageTotal: Int = 0,
    val restUsesLeft: Int = 2,
    val counterReady: Boolean = false,
)

data class BattleTurnResult(
    val playerDamage: Int = 0,
    val enemyDamage: Int = 0,
    val playerHeal: Int = 0,
    val playerDodged: Boolean = false,
    val enemyDodged: Boolean = false,
    val playerRested: Boolean = false,
    val playerGuarded: Boolean = false,
    val quickGuard: Boolean = false,
    val heavyRisk: Boolean = false,
    val counterReady: Boolean = false,
    val counterUsed: Boolean = false,
    val restFailed: Boolean = false,
    val battleEnded: Boolean = false,
    val playerWon: Boolean = false,
    val playerTypePct: Int = 100,
    val enemyTypePct: Int = 100,
)
