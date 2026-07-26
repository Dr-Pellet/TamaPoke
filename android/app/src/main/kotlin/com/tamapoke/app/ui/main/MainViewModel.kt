package com.tamapoke.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tamapoke.app.audio.Sfx
import com.tamapoke.app.audio.SfxPlayer
import com.tamapoke.app.data.PetRepository
import com.tamapoke.app.ui.battle.BattleUiState
import com.tamapoke.core.PetEngine
import com.tamapoke.core.PetState
import com.tamapoke.core.battle.BattleAction
import com.tamapoke.core.battle.BattleEngine
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.Ceremony
import com.tamapoke.core.enums.Medal
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: PetRepository,
    private val sfx: SfxPlayer,
) : ViewModel() {
    val state = repository.state
    val dex: DexTable get() = repository.dex

    /** Last training-bag strength gain (Pet::trainStrength()'s "STR +%u" feedback); UI clears it after showing. */
    private val _lastStrengthGain = MutableStateFlow<Int?>(null)
    val lastStrengthGain: StateFlow<Int?> = _lastStrengthGain

    private val _evolutionEvent = MutableStateFlow<EvolutionEvent?>(null)
    val evolutionEvent: StateFlow<EvolutionEvent?> = _evolutionEvent

    fun clearEvolutionEvent() {
        _evolutionEvent.value = null
    }

    /** Bumped on every caress() - UI plays a heart-burst animation keyed on this value. */
    private val _heartBurstTrigger = MutableStateFlow(0)
    val heartBurstTrigger: StateFlow<Int> = _heartBurstTrigger

    /** Newly-earned medals (checkMedals()'s "MEDAL!" banner); UI clears it after showing. */
    private val _medalBanner = MutableStateFlow<Set<Medal>?>(null)
    val medalBanner: StateFlow<Set<Medal>?> = _medalBanner

    fun clearMedalBanner() {
        _medalBanner.value = null
    }

    /** Newly-reached streak milestone (3/7/30/100 days); UI clears it after showing. */
    private val _streakMilestone = MutableStateFlow<Int?>(null)
    val streakMilestone: StateFlow<Int?> = _streakMilestone

    fun clearStreakMilestone() {
        _streakMilestone.value = null
    }

    init {
        viewModelScope.launch { repository.catchUp() }
        // Detects transitions (level up, medal, hatch, evolve, ceremony start) purely from
        // consecutive states, so it fires whether they happened live or during offline catch-up -
        // same as the original, where checkMedals()/tick() play these SFX as a side effect of state change.
        viewModelScope.launch {
            state.filterNotNull()
                .runningFold(Pair<PetState?, PetState?>(null, null)) { acc, next -> acc.second to next }
                .collect { (prev, next) -> if (prev != null && next != null) playTransitionSfx(prev, next) }
        }
    }

    private fun playTransitionSfx(prev: PetState, next: PetState) {
        when {
            prev.ceremony == Ceremony.NONE && next.ceremony != Ceremony.NONE -> sfx.play(Sfx.BYE)
            prev.isEgg && !next.isEgg -> sfx.play(Sfx.HATCH)
            !prev.isEgg && !next.isEgg && prev.speciesId != next.speciesId -> {
                sfx.play(Sfx.EVOLVE)
                _evolutionEvent.value = EvolutionEvent(prev.speciesId, next.speciesId, next.shiny)
            }
            next.medals.size > prev.medals.size -> {
                sfx.play(Sfx.MEDAL)
                _medalBanner.value = next.medals - prev.medals
            }
            !next.isEgg && next.level() > prev.level() -> sfx.play(Sfx.LEVEL)
        }
        if (next.lastMilestone > prev.lastMilestone) {
            _streakMilestone.value = next.lastMilestone
        }
    }

    fun onResume() {
        viewModelScope.launch { repository.catchUp() }
    }

    /** Drives the visible per-minute decay while the app is in the foreground. */
    fun liveTick() {
        viewModelScope.launch { repository.tickOnce() }
    }

    fun feed() = viewModelScope.launch { repository.feed(); sfx.play(Sfx.EAT) }
    fun feedBerry(color: Int) = viewModelScope.launch { repository.feedBerry(color); sfx.play(Sfx.EAT) }
    fun feedCandy() = viewModelScope.launch { repository.feedCandy(); sfx.play(Sfx.EAT) }
    fun play() = viewModelScope.launch { repository.play() }
    fun playResult(score: Int) = viewModelScope.launch { repository.playResult(score); sfx.play(Sfx.PLAY) }
    fun trainStrength(hits: Int) = viewModelScope.launch {
        _lastStrengthGain.value = repository.trainStrength(hits)
        sfx.play(Sfx.PLAY)
    }

    fun clearStrengthGainMessage() {
        _lastStrengthGain.value = null
    }

    fun clean() = viewModelScope.launch { repository.clean() }
    fun caress() = viewModelScope.launch {
        repository.caress()
        sfx.play(Sfx.HEART)
        _heartBurstTrigger.value++
    }
    fun toggleLight() = viewModelScope.launch { repository.toggleLight(); sfx.play(Sfx.TAP) }
    fun eggTap() = viewModelScope.launch { repository.eggTap(); sfx.play(Sfx.TAP) }
    fun evolve() = viewModelScope.launch { repository.evolve() }
    fun startFarewell() = viewModelScope.launch { repository.startFarewell() }
    fun startRunaway() = viewModelScope.launch { repository.startRunaway() }
    fun release() = viewModelScope.launch { repository.release() }
    fun rename(nickname: String) = viewModelScope.launch { repository.rename(nickname) }
    fun chooseStarter(starterDex: Int) = viewModelScope.launch { repository.chooseStarter(starterDex) }
    fun declineEvolve() = viewModelScope.launch { repository.declineEvolve() }
    fun declineFarewell() = viewModelScope.launch { repository.declineFarewell() }
    fun resolveCeremony() = viewModelScope.launch { repository.resolveCeremony() }

    // ---- wild battles (ShadowEnemy expanded fork) ----

    private val _battle = MutableStateFlow<BattleUiState?>(null)
    val battle: StateFlow<BattleUiState?> = _battle

    /** Starts a wild encounter using the pet's current stats/level; no-op if the pet can't fight right now. */
    fun startWildBattle() {
        val current = state.value ?: return
        if (current.isEgg || current.ceremony != Ceremony.NONE || current.sleeping) return
        val dex = repository.dex
        val wildDexId = BattleEngine.pickWildSpecies(dex, Random.nextInt(100))
        val wildLevel = BattleEngine.wildLevelFor(current.level(), Random.nextInt(100))
        val wildStats = BattleEngine.wildBattleStats(dex, wildDexId, wildLevel)
        val playerStats = PetEngine.petBattleStats(current, dex)
        val runtime = BattleEngine.beginBattleRuntime(playerStats, wildStats)
        _battle.value = BattleUiState(wildDexId = wildDexId, runtime = runtime)
    }

    fun battleAct(action: BattleAction) {
        val current = _battle.value ?: return
        if (current.ended) return
        val (next, turn) = BattleEngine.stepBattle(current.runtime, action, Random.nextInt(100))
        _battle.value = current.copy(runtime = next, lastTurn = turn)
        sfx.play(if (turn.enemyDodged || turn.playerDodged) Sfx.TAP else Sfx.PLAY)
        if (turn.battleEnded) viewModelScope.launch { resolveBattleEnd() }
    }

    private suspend fun resolveBattleEnd() {
        val battleState = _battle.value ?: return
        val dex = repository.dex
        val wild = dex[battleState.wildDexId]
        val runtime = battleState.runtime
        val bond = state.value?.bond ?: 0
        if (battleState.runtime.enemyHp <= 0) {
            val closeWin = runtime.playerHp <= runtime.playerMaxHp * 3 / 10
            val reward = repository.applyBattleWin(wild, closeWin)
            sfx.play(Sfx.LEVEL)
            val chance = BattleEngine.catchChance(wild.rarity, runtime.enemy.level, runtime.player.level, closeWin, bond)
            _battle.value = battleState.copy(
                ended = true, playerWon = true, reward = reward,
                catchOffered = true, catchIsRespect = false, catchChancePct = chance,
            )
        } else {
            repository.applyBattleLoss()
            sfx.play(Sfx.DENY)
            val closeLoss = runtime.enemyHp <= runtime.enemyMaxHp * 3 / 10
            val chance = if (closeLoss) {
                BattleEngine.respectCatchChance(wild.rarity, runtime.enemy.level, runtime.player.level, bond)
            } else 0
            _battle.value = battleState.copy(
                ended = true, playerWon = false,
                catchOffered = closeLoss, catchIsRespect = true, catchChancePct = chance,
            )
        }
    }

    fun attemptCatch() = viewModelScope.launch {
        val battleState = _battle.value ?: return@launch
        if (!battleState.catchOffered || battleState.catchResult != null) return@launch
        val success = Random.nextInt(100) < battleState.catchChancePct
        if (success) {
            repository.registerCaught(battleState.wildDexId)
            sfx.play(Sfx.MEDAL)
        } else {
            sfx.play(Sfx.DENY)
        }
        _battle.value = battleState.copy(catchResult = success, catchOffered = false)
    }

    fun closeBattle() {
        _battle.value = null
    }

    // ---- extra minigames (ShadowEnemy expanded fork): Catch/Memo/Clean/Type ----

    fun catchMinigameResult(score: Int) = viewModelScope.launch { repository.applyCatchResult(score); sfx.play(Sfx.PLAY) }
    fun memoMinigameResult(rounds: Int) = viewModelScope.launch { repository.applyMemoResult(rounds); sfx.play(Sfx.PLAY) }
    fun cleanMinigameResult(score: Int) = viewModelScope.launch { repository.applyCleanResult(score); sfx.play(Sfx.PLAY) }
    fun typeMinigameResult(score: Int) = viewModelScope.launch { repository.applyTypeResult(score); sfx.play(Sfx.PLAY) }

    /** Exports the current save as JSON for the caller to write to a user-chosen file. */
    fun exportSave(onResult: (String) -> Unit) = viewModelScope.launch { onResult(repository.exportSave()) }

    /** Imports a save previously produced by [exportSave], overwriting the current one. */
    fun importSave(json: String, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.importSave(json)
        onDone()
    }

    class Factory(
        private val repository: PetRepository,
        private val sfx: SfxPlayer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository, sfx) as T
    }
}
