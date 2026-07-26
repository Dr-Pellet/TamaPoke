package com.tamapoke.app.ui.battle

import com.tamapoke.core.BattleReward
import com.tamapoke.core.battle.BattleRuntime
import com.tamapoke.core.battle.BattleTurnResult

/**
 * Transient (never persisted) UI state for one wild encounter - only the
 * final win/loss/catch outcome is written back to [PetState] via
 * [com.tamapoke.core.PetEngine]'s applyBattleWin/applyBattleLoss/registerCaught.
 */
data class BattleUiState(
    val wildDexId: Int,
    val runtime: BattleRuntime,
    val lastTurn: BattleTurnResult? = null,
    val ended: Boolean = false,
    val playerWon: Boolean = false,
    val reward: BattleReward? = null,
    val catchOffered: Boolean = false,
    val catchIsRespect: Boolean = false,
    val catchChancePct: Int = 0,
    val catchResult: Boolean? = null,
)
