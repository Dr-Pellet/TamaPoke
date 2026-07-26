package com.tamapoke.core.battle

import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.BattleType
import com.tamapoke.core.enums.BattleType.BUG
import com.tamapoke.core.enums.BattleType.DARK
import com.tamapoke.core.enums.BattleType.DRAGON
import com.tamapoke.core.enums.BattleType.ELECTRIC
import com.tamapoke.core.enums.BattleType.FAIRY
import com.tamapoke.core.enums.BattleType.FIGHTING
import com.tamapoke.core.enums.BattleType.FIRE
import com.tamapoke.core.enums.BattleType.FLYING
import com.tamapoke.core.enums.BattleType.GHOST
import com.tamapoke.core.enums.BattleType.GRASS
import com.tamapoke.core.enums.BattleType.GROUND
import com.tamapoke.core.enums.BattleType.ICE
import com.tamapoke.core.enums.BattleType.NORMAL
import com.tamapoke.core.enums.BattleType.POISON
import com.tamapoke.core.enums.BattleType.PSYCHIC
import com.tamapoke.core.enums.BattleType.ROCK
import com.tamapoke.core.enums.BattleType.STEEL
import com.tamapoke.core.enums.BattleType.WATER
import com.tamapoke.core.enums.Rarity
import kotlin.random.Random

/**
 * Turn-based wild-encounter battle system, ported 1:1 from the ShadowEnemy
 * expanded fork's battle.cpp: type effectiveness (18-type chart),
 * attack/quick-attack/heavy-attack/dodge/rest actions, a counter mechanic,
 * and level-scaled HP/damage. Pure functions + immutable [BattleRuntime],
 * same style as [com.tamapoke.core.PetEngine].
 */
object BattleEngine {
    private const val MAX_TURN_ROUNDS = 20

    /** -2/-1/0/+1 relation exactly matching battle.cpp's typeRelation() switch. */
    private fun typeRelation(attack: BattleType, defend: BattleType): Int = when (attack) {
        NORMAL -> when (defend) { ROCK, STEEL -> -1; GHOST -> -2; else -> 0 }
        FIRE -> when (defend) {
            BUG, STEEL, GRASS, ICE -> 1
            ROCK, FIRE, WATER, DRAGON -> -1
            else -> 0
        }
        WATER -> when (defend) {
            GROUND, ROCK, FIRE -> 1
            WATER, GRASS, DRAGON -> -1
            else -> 0
        }
        ELECTRIC -> when (defend) {
            FLYING, WATER -> 1
            GRASS, ELECTRIC, DRAGON -> -1
            GROUND -> -2
            else -> 0
        }
        GRASS -> when (defend) {
            GROUND, ROCK, WATER -> 1
            FLYING, POISON, BUG, STEEL, FIRE, GRASS, DRAGON -> -1
            else -> 0
        }
        ICE -> when (defend) {
            FLYING, GROUND, GRASS, DRAGON -> 1
            STEEL, FIRE, WATER, ICE -> -1
            else -> 0
        }
        FIGHTING -> when (defend) {
            NORMAL, ROCK, STEEL, ICE, DARK -> 1
            FLYING, POISON, BUG, PSYCHIC, FAIRY -> -1
            GHOST -> -2
            else -> 0
        }
        POISON -> when (defend) {
            GRASS, FAIRY -> 1
            POISON, GROUND, ROCK, GHOST -> -1
            STEEL -> -2
            else -> 0
        }
        GROUND -> when (defend) {
            POISON, ROCK, STEEL, FIRE, ELECTRIC -> 1
            BUG, GRASS -> -1
            FLYING -> -2
            else -> 0
        }
        FLYING -> when (defend) {
            FIGHTING, BUG, GRASS -> 1
            ROCK, STEEL, ELECTRIC -> -1
            else -> 0
        }
        PSYCHIC -> when (defend) {
            FIGHTING, POISON -> 1
            STEEL, PSYCHIC -> -1
            DARK -> -2
            else -> 0
        }
        BUG -> when (defend) {
            GRASS, PSYCHIC, DARK -> 1
            FIGHTING, FLYING, POISON, GHOST, STEEL, FIRE, FAIRY -> -1
            else -> 0
        }
        ROCK -> when (defend) {
            FLYING, BUG, FIRE, ICE -> 1
            FIGHTING, GROUND, STEEL -> -1
            else -> 0
        }
        GHOST -> when (defend) {
            GHOST, PSYCHIC -> 1
            DARK -> -1
            NORMAL -> -2
            else -> 0
        }
        DRAGON -> when (defend) {
            DRAGON -> 1
            STEEL -> -1
            FAIRY -> -2
            else -> 0
        }
        DARK -> when (defend) {
            GHOST, PSYCHIC -> 1
            FIGHTING, DARK, FAIRY -> -1
            else -> 0
        }
        STEEL -> when (defend) {
            ROCK, ICE, FAIRY -> 1
            STEEL, FIRE, WATER, ELECTRIC -> -1
            else -> 0
        }
        FAIRY -> when (defend) {
            FIGHTING, DRAGON, DARK -> 1
            POISON, STEEL, FIRE -> -1
            else -> 0
        }
    }

    /** Effectiveness percentage (100 = neutral), combining both defender types. */
    fun typeEffectPct(attack: BattleType?, defend1: BattleType?, defend2: BattleType?): Int {
        if (attack == null) return 100
        var pct = 100
        for (defend in listOf(defend1, defend2)) {
            if (defend == null) continue
            when (typeRelation(attack, defend)) {
                1 -> pct = pct * 120 / 100
                -1 -> pct = pct * 85 / 100
                -2 -> pct = pct * 70 / 100
            }
        }
        return pct.coerceIn(1, 255)
    }

    fun wildLevelFor(petLevel: Int, luckRoll: Int): Int {
        val base = if (petLevel > 0) petLevel else 1
        val delta = when {
            luckRoll < 55 -> (luckRoll % 3) - 1 // -1..+1, most common
            luckRoll < 85 -> -2 - (luckRoll % 3) // -2..-4, fair catch-up fights
            else -> 2 + (luckRoll % 2) // +2..+3, occasional danger
        }
        return (base + delta).coerceIn(1, 100)
    }

    fun pickWildSpecies(dex: DexTable, roll: Int): Int {
        val targetRarity = if (roll % 100 < 25) Rarity.RARE else Rarity.COMMON
        var pool = dex.entriesOfRarity(targetRarity)
        if (pool.isEmpty() && targetRarity == Rarity.RARE) pool = dex.entriesOfRarity(Rarity.COMMON)
        return if (pool.isNotEmpty()) pool[roll % pool.size].id else 1
    }

    fun wildBattleStats(dex: DexTable, dexId: Int, level: Int): BattleStats {
        val id = if (dexId in 1..dex.count) dexId else 1
        val entry = dex[id]
        val lvl = if (level > 0) level else 1
        return BattleStats(
            atk = entry.baseAtk + lvl,
            def = entry.baseDef + lvl,
            spe = entry.baseSpe + lvl,
            level = lvl,
            type1 = entry.battleType1,
            type2 = entry.battleType2,
        )
    }

    private fun turnHpFor(stats: BattleStats): Int {
        val level = if (stats.level > 0) stats.level else 1
        return (30 + level * 5 + stats.def).coerceAtMost(65535)
    }

    private fun damageFor(attacker: BattleStats, defender: BattleStats, luck: Int): Int {
        val level = if (attacker.level > 0) attacker.level else 1
        val pressure = attacker.atk * (10 + level / 4)
        val guard = defender.def + 35
        val base = 1 + pressure / guard
        val variation = 90 + (luck.coerceIn(0, 99) % 21) // 90..110%
        var damage = base * variation / 100
        if (damage <= 0) damage = 1
        val scaled = damage * typeEffectPct(attacker.type1, defender.type1, defender.type2) / 100
        return scaled.coerceIn(1, 65535)
    }

    private fun attackPowerPct(action: BattleAction): Int = when (action) {
        BattleAction.ATTACK_QUICK -> 85
        BattleAction.ATTACK_HEAVY -> 125
        else -> 100
    }

    private fun cappedTurnDamage(
        attacker: BattleStats,
        defender: BattleStats,
        defenderMaxHp: Int,
        luck: Int,
        counter: Boolean,
        powerPct: Int,
    ): Int {
        var damage = damageFor(attacker, defender, luck)
        damage = damage * powerPct / 100
        if (damage <= 0) damage = 1
        if (counter) damage = damage * 3 / 2
        val cap = (defenderMaxHp * 35 / 100).coerceAtLeast(1)
        return damage.coerceAtMost(cap)
    }

    private fun enemyDodgeChance(battle: BattleRuntime, action: BattleAction): Int {
        var chance = if (battle.enemy.spe > battle.player.spe) 18 else 10
        when (action) {
            BattleAction.ATTACK_QUICK -> chance -= 9
            BattleAction.ATTACK_HEAVY -> chance += 22
            else -> {}
        }
        return chance.coerceIn(0, 45)
    }

    private fun isAttackAction(action: BattleAction) =
        action == BattleAction.ATTACK || action == BattleAction.ATTACK_QUICK || action == BattleAction.ATTACK_HEAVY

    fun beginBattleRuntime(player: BattleStats, enemy: BattleStats): BattleRuntime {
        val playerMaxHp = turnHpFor(player)
        val enemyMaxHp = turnHpFor(enemy)
        return BattleRuntime(
            player = player, enemy = enemy,
            playerHp = playerMaxHp, enemyHp = enemyMaxHp,
            playerMaxHp = playerMaxHp, enemyMaxHp = enemyMaxHp,
        )
    }

    private fun finished(battle: BattleRuntime) =
        battle.playerHp <= 0 || battle.enemyHp <= 0 || battle.round >= MAX_TURN_ROUNDS

    private fun winner(battle: BattleRuntime): Boolean {
        if (battle.enemyHp <= 0) return true
        if (battle.playerHp <= 0) return false
        return battle.playerHp >= battle.enemyHp
    }

    /** One battle turn. Returns the updated runtime plus what happened, for the UI to narrate. */
    fun stepBattle(battle: BattleRuntime, action: BattleAction, luckRoll: Int): Pair<BattleRuntime, BattleTurnResult> {
        if (finished(battle)) {
            return battle to BattleTurnResult(battleEnded = true, playerWon = winner(battle))
        }

        val luck = luckRoll.coerceIn(0, 99)
        val playerTypePct = typeEffectPct(battle.player.type1, battle.enemy.type1, battle.enemy.type2)
        val enemyTypePct = typeEffectPct(battle.enemy.type1, battle.player.type1, battle.player.type2)

        if (action == BattleAction.REST && battle.restUsesLeft == 0) {
            return battle to BattleTurnResult(
                playerRested = true, restFailed = true,
                playerTypePct = playerTypePct, enemyTypePct = enemyTypePct,
            )
        }

        var b = battle.copy(round = battle.round + 1)
        var playerDamage = 0
        var playerHeal = 0
        var enemyDodged = false
        var counterUsed = false
        var restedNow = false

        when {
            action == BattleAction.REST -> {
                restedNow = true
                val heal = (b.playerMaxHp * 28 / 100).coerceAtLeast(6)
                val missing = b.playerMaxHp - b.playerHp
                playerHeal = heal.coerceAtMost(missing)
                b = b.copy(restUsesLeft = b.restUsesLeft - 1, playerHp = b.playerHp + playerHeal)
            }
            isAttackAction(action) -> {
                val dodgeChance = enemyDodgeChance(b, action)
                enemyDodged = ((luck + b.enemy.spe / 2) % 100) < dodgeChance
                if (!enemyDodged) {
                    counterUsed = b.counterReady
                    playerDamage = cappedTurnDamage(b.player, b.enemy, b.enemyMaxHp, luck, b.counterReady, attackPowerPct(action))
                    val newEnemyHp = (b.enemyHp - playerDamage).coerceAtLeast(0)
                    b = b.copy(
                        enemyHp = newEnemyHp,
                        counterReady = false,
                        playerDamageTotal = b.playerDamageTotal + (b.enemyHp - newEnemyHp),
                    )
                }
            }
        }

        var enemyDamage = 0
        var playerDodged = false
        var counterReadyNow = false
        var quickGuard = false
        var heavyRisk = false
        var playerGuarded = false

        val enemyActs = b.enemyHp > 0 && (!enemyDodged || action == BattleAction.ATTACK_HEAVY)
        if (enemyActs) {
            var enemyHit = cappedTurnDamage(b.enemy, b.player, b.playerMaxHp, 99 - luck, false, 100)
            when (action) {
                BattleAction.DODGE -> {
                    if (luck < 85) {
                        playerDodged = true
                        counterReadyNow = true
                        enemyHit = 0
                    } else {
                        enemyHit = if (enemyHit > 2) enemyHit / 3 else 1
                    }
                }
                BattleAction.ATTACK_QUICK -> {
                    quickGuard = true
                    enemyHit = (enemyHit * 85 / 100).coerceAtLeast(1)
                }
                BattleAction.ATTACK_HEAVY -> {
                    heavyRisk = true
                    enemyHit = (enemyHit * 120 / 100).coerceAtLeast(1)
                }
                BattleAction.REST -> {
                    playerGuarded = true
                    enemyHit = (enemyHit * 70 / 100).coerceAtLeast(1)
                }
                else -> {}
            }
            if (enemyHit > 0) {
                enemyDamage = enemyHit
                val newPlayerHp = (b.playerHp - enemyDamage).coerceAtLeast(0)
                b = b.copy(
                    playerHp = newPlayerHp,
                    enemyDamageTotal = b.enemyDamageTotal + (b.playerHp - newPlayerHp),
                )
            }
        }
        if (counterReadyNow) b = b.copy(counterReady = true)

        val ended = finished(b)
        val turn = BattleTurnResult(
            playerDamage = playerDamage,
            enemyDamage = enemyDamage,
            playerHeal = playerHeal,
            playerDodged = playerDodged,
            enemyDodged = enemyDodged,
            playerRested = restedNow,
            playerGuarded = playerGuarded,
            quickGuard = quickGuard,
            heavyRisk = heavyRisk,
            counterReady = counterReadyNow,
            counterUsed = counterUsed,
            restFailed = false,
            battleEnded = ended,
            playerWon = if (ended) winner(b) else false,
            playerTypePct = playerTypePct,
            enemyTypePct = enemyTypePct,
        )
        return b to turn
    }

    /**
     * Catch chance after a win, ported 1:1 from pet.cpp's catchChanceForWild():
     * legendaries can't be caught this way, rares are capped lower than commons,
     * bond helps a little, a closely-won fight helps a little more.
     */
    fun catchChance(rarity: Rarity, wildLevel: Int, petLevel: Int, closeWin: Boolean, bond: Int): Int {
        if (rarity == Rarity.LEGENDARY) return 0
        var chance = if (rarity == Rarity.RARE) 28 else 55
        val levelGap = wildLevel - (if (petLevel > 0) petLevel else 1)
        chance += if (levelGap > 0) -levelGap * 4 else (-levelGap) * 2
        if (closeWin) chance += 8
        chance += bond / 20
        if (rarity == Rarity.RARE && chance > 60) chance = 60
        return chance.coerceIn(10, 75)
    }

    /** Gentler "respect catch": only offered after a loss where the wild Pokemon ended below 30% HP. */
    fun respectCatchChance(rarity: Rarity, wildLevel: Int, petLevel: Int, bond: Int): Int {
        val normal = catchChance(rarity, wildLevel, petLevel, closeWin = true, bond = bond)
        if (normal == 0) return 0
        return (normal * 40 / 100).coerceIn(5, 25)
    }
}
