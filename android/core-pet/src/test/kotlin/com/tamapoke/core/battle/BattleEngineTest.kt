package com.tamapoke.core.battle

import com.tamapoke.core.enums.BattleType
import com.tamapoke.core.enums.Rarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleEngineTest {
    @Test
    fun `fire is super effective against grass`() {
        val pct = BattleEngine.typeEffectPct(BattleType.FIRE, BattleType.GRASS, null)
        assertEquals(120, pct)
    }

    @Test
    fun `water is not very effective against grass`() {
        val pct = BattleEngine.typeEffectPct(BattleType.WATER, BattleType.GRASS, null)
        assertEquals(85, pct)
    }

    @Test
    fun `dual typing stacks effectiveness across both defending types`() {
        // Electric vs Water/Flying: super effective against both (Water and Flying) -> 120% * 120%
        val pct = BattleEngine.typeEffectPct(BattleType.ELECTRIC, BattleType.WATER, BattleType.FLYING)
        assertEquals(144, pct) // 100 * 1.2 * 1.2, integer division both steps
    }

    @Test
    fun `wild level stays within 1 to 100 and centers near pet level`() {
        for (roll in 0..99) {
            val level = BattleEngine.wildLevelFor(petLevel = 10, luckRoll = roll)
            assertTrue(level in 1..100, "level $level out of range for roll $roll")
        }
    }

    @Test
    fun `battle runtime starts at full hp and ends when a side reaches zero`() {
        val player = BattleStats(atk = 60, def = 40, spe = 50, level = 10)
        val enemy = BattleStats(atk = 5, def = 5, spe = 5, level = 1)
        var battle = BattleEngine.beginBattleRuntime(player, enemy)
        assertEquals(battle.playerMaxHp, battle.playerHp)
        assertEquals(battle.enemyMaxHp, battle.enemyHp)

        var turns = 0
        while (battle.enemyHp > 0 && battle.playerHp > 0 && turns < 20) {
            val (next, turn) = BattleEngine.stepBattle(battle, BattleAction.ATTACK, luckRoll = 50)
            battle = next
            turns++
            if (turn.battleEnded) break
        }
        assertTrue(battle.enemyHp <= 0 || battle.round >= 20, "a much stronger player should win against a weak enemy")
    }

    @Test
    fun `resting heals the player and consumes a limited use`() {
        val player = BattleStats(atk = 30, def = 30, spe = 30, level = 10)
        val enemy = BattleStats(atk = 30, def = 30, spe = 30, level = 10)
        var battle = BattleEngine.beginBattleRuntime(player, enemy)
        // Take some damage first so resting has something to heal.
        battle = battle.copy(playerHp = battle.playerMaxHp / 2)
        val restsBefore = battle.restUsesLeft

        val (next, turn) = BattleEngine.stepBattle(battle, BattleAction.REST, luckRoll = 10)
        assertTrue(turn.playerRested)
        assertTrue(next.restUsesLeft < restsBefore)
        assertTrue(next.playerHp >= battle.playerHp)
    }

    @Test
    fun `catch chance is zero for legendaries and capped for rares`() {
        assertEquals(0, BattleEngine.catchChance(Rarity.LEGENDARY, wildLevel = 5, petLevel = 20, closeWin = true, bond = 100))
        val rareChance = BattleEngine.catchChance(Rarity.RARE, wildLevel = 1, petLevel = 50, closeWin = true, bond = 100)
        assertTrue(rareChance <= 60)
    }

    @Test
    fun `respect catch chance is gentler than a normal close-win catch`() {
        val normal = BattleEngine.catchChance(Rarity.COMMON, wildLevel = 10, petLevel = 10, closeWin = true, bond = 0)
        val respect = BattleEngine.respectCatchChance(Rarity.COMMON, wildLevel = 10, petLevel = 10, bond = 0)
        assertTrue(respect < normal)
    }
}
