package com.tamapoke.core

import com.tamapoke.core.enums.BattleRewardStat

data class BattleReward(val stat: BattleRewardStat = BattleRewardStat.NONE, val amount: Int = 0)
