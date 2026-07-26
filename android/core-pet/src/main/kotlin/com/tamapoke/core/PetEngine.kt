package com.tamapoke.core

import com.tamapoke.core.battle.BattleStats
import com.tamapoke.core.dex.DexEntry
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.BattleRewardStat
import com.tamapoke.core.enums.Ceremony
import com.tamapoke.core.enums.DailyGoalType
import com.tamapoke.core.enums.Medal
import com.tamapoke.core.enums.PetEventType
import com.tamapoke.core.enums.PetMood
import com.tamapoke.core.enums.PetPersonality
import com.tamapoke.core.enums.Rarity
import kotlin.random.Random

data class TickOutcome(val state: PetState, val events: List<PetEvent> = emptyList())

/**
 * Faithful, pure-function port of the original firmware's pet.cpp. Every
 * function takes a [PetState] and returns a new one (plus [PetEvent]s where
 * the original fired an SFX/celebration) — nothing here touches a clock,
 * a database or Android APIs, which is what makes it unit-testable on the
 * plain JVM and safe to replay minute-by-minute for offline catch-up.
 */
object PetEngine {

    // ---- one real/game minute, matches Pet::tick() ----
    fun tickOnce(state: PetState, dex: DexTable, rng: Random = Random.Default): TickOutcome {
        if (state.ceremony != Ceremony.NONE) return TickOutcome(state)

        var s = state.copy(ageMinutes = state.ageMinutes + 1)
        val events = mutableListOf<PetEvent>()

        if (s.isEgg) {
            if (s.ageMinutes >= 3) {
                s = hatch(s, dex, rng)
                events += PetEvent.Hatched
            }
            return TickOutcome(s, events)
        }

        if (s.sleeping) {
            var energy = clamp100(s.energy + 6)
            var weight = s.weight
            if (weight > 0 && s.ageMinutes % 3 == 0L) weight--
            var fullness = s.fullness
            var joy = s.joy
            if (s.ageMinutes % 2 == 0L) {
                fullness = dropTo(fullness, 1, 30)
                joy = dropTo(joy, 1, 35)
            }
            var hygiene = s.hygiene
            if (s.ageMinutes % 3 == 0L) hygiene = dropTo(hygiene, 1, 45)
            s = s.copy(energy = energy, weight = weight, fullness = fullness, joy = joy, hygiene = hygiene)
            val (s2, medalEvents) = checkMedals(s, dex)
            return TickOutcome(s2, events + medalEvents)
        }

        if (s.ageMinutes % PetState.MINUTES_PER_LEVEL == 0L) events += PetEvent.LevelUp

        var fullness = clamp100(s.fullness - 2)
        var energy = clamp100(s.energy - 1)
        var poops = s.poops
        if (fullness > 40 && poops < 3 && rng.nextInt(100) < 15) poops++
        var hygiene = clamp100(s.hygiene - 1 - 4 * poops)
        if (s.weight > 50) energy = clamp100(energy - 1)
        var weight = s.weight
        if (weight > 0 && s.ageMinutes % 3 == 0L) weight--

        var goodTicks = s.goodTicks
        var trDef = s.trDef
        if (minOf(fullness, s.joy, energy, hygiene) >= 40) {
            goodTicks++
            if (goodTicks >= 720) {
                goodTicks = 0
                if (trDef < 100) trDef++
            }
        } else {
            goodTicks = 0
        }

        var dJoy = -1
        if (fullness < 30) dJoy -= 2
        if (hygiene < 30) dJoy -= 2
        val joy = clamp100(s.joy + dJoy)

        var mistakeCooldown = s.mistakeCooldown
        var careMistakes = s.careMistakes
        var bond = s.bond
        if (mistakeCooldown > 0) mistakeCooldown--
        val lowest = minOf(fullness, joy, energy, hygiene)
        if (lowest <= 10 && mistakeCooldown == 0) {
            careMistakes++
            mistakeCooldown = 30
            if (bond > 3) bond -= 3
        }

        s = s.copy(
            fullness = fullness, energy = energy, poops = poops, hygiene = hygiene, weight = weight,
            goodTicks = goodTicks, trDef = trDef, joy = joy, mistakeCooldown = mistakeCooldown,
            careMistakes = careMistakes, bond = bond,
        )

        val (s2, medalEvents) = checkMedals(s, dex)
        events += medalEvents

        val neglectTicks = if (s2.fullness == 0 && s2.joy == 0 && s2.energy == 0 && s2.hygiene == 0) {
            minOf(s2.neglectTicks + 1, PetState.RUNAWAY_TICKS)
        } else 0

        return TickOutcome(s2.copy(neglectTicks = neglectTicks), events)
    }

    /**
     * Offline/background catch-up, matches Pet::syncClock(): a gentler decay
     * (floors instead of hard zero, no care-mistakes/runaway/evolution while
     * away) replayed minute-by-minute, capped at two weeks — same behaviour
     * as the original RTC-driven catch-up.
     */
    fun advanceOffline(state: PetState, dex: DexTable, nowEpochSeconds: Long, rng: Random = Random.Default): PetState {
        if (nowEpochSeconds == 0L) return state
        val seen = state.lastSeenEpochSeconds
        var mins = if (seen != 0L && nowEpochSeconds > seen) (nowEpochSeconds - seen) / 60 else 0L
        if (mins < 2 || state.ceremony != Ceremony.NONE) {
            return state.copy(lastSeenEpochSeconds = nowEpochSeconds)
        }
        if (mins > PetState.OFFLINE_CATCHUP_CAP_MIN) mins = PetState.OFFLINE_CATCHUP_CAP_MIN

        var s = state
        for (i in 0 until mins) {
            s = s.copy(ageMinutes = s.ageMinutes + 1)
            if (s.isEgg) {
                if (s.ageMinutes >= 3) s = hatch(s, dex, rng)
                continue
            }
            if (s.sleeping) {
                var energy = clamp100(s.energy + 6)
                var fullness = s.fullness
                var joy = s.joy
                if (s.ageMinutes % 2 == 0L) {
                    fullness = dropTo(fullness, 1, 30)
                    joy = dropTo(joy, 1, 35)
                }
                var hygiene = s.hygiene
                if (s.ageMinutes % 3 == 0L) hygiene = dropTo(hygiene, 1, 45)
                s = s.copy(energy = energy, fullness = fullness, joy = joy, hygiene = hygiene)
                continue
            }
            s = s.copy(
                fullness = dropTo(s.fullness, 2, 15),
                energy = dropTo(s.energy, 1, 15),
                hygiene = dropTo(s.hygiene, 1, 15),
                joy = dropTo(s.joy, 1, 15),
            )
        }
        if (!s.isEgg && !s.sleeping) {
            val p = minOf(3, s.poops + (mins / 240).toInt())
            s = s.copy(poops = p)
        }
        return s.copy(lastSeenEpochSeconds = nowEpochSeconds)
    }

    /** Persists the clock without applying any progression (Pet::setClock). */
    fun setClock(state: PetState, nowEpochSeconds: Long): PetState =
        state.copy(lastSeenEpochSeconds = nowEpochSeconds)

    // ---- actions ----

    fun lovesBerry(state: PetState, color: Int): Boolean =
        !state.isEgg && (state.speciesId % 3) == color

    fun feed(state: PetState): PetState = feedBerry(state, 0)

    fun feedBerry(state: PetState, color: Int): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg || state.sleeping) return state
        var s = state
        s = if (lovesBerry(s, color)) {
            addBond(s.copy(fullness = clamp100(s.fullness + 35), joy = clamp100(s.joy + 10), berryKnown = true), 2)
        } else {
            s.copy(fullness = clamp100(s.fullness + 25))
        }
        return registerCare(s)
    }

    fun feedCandy(state: PetState): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg || state.sleeping) return state
        val s = state.copy(
            fullness = clamp100(state.fullness + 10),
            joy = clamp100(state.joy + 12),
            weight = clamp100(state.weight + 12),
        )
        return registerCare(s)
    }

    fun play(state: PetState): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg || state.sleeping) return state
        var s = state.copy(
            joy = clamp100(state.joy + 25),
            energy = clamp100(state.energy - 10),
            fullness = clamp100(state.fullness - 5),
        )
        s = addBond(s, 2)
        return registerCare(s)
    }

    /** Reward from the minigame (0..~20 score): trains SPEED. */
    fun playResult(state: PetState, score: Int): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state
        val trSpe = minOf(100, state.trSpe + score / 5)
        val joy = clamp100(state.joy + 5 + (if (score > 15) 30 else score * 2))
        val energy = dropTo(state.energy, 10 + score / 2, 5)
        val fullness = dropTo(state.fullness, 5, 5)
        val weight = maxOf(0, state.weight - score * 2)
        val gameHi = maxOf(state.gameHi, score)
        var s = state.copy(trSpe = trSpe, joy = joy, energy = energy, fullness = fullness, weight = weight, gameHi = gameHi)
        s = addBond(s, 2)
        return registerCare(s)
    }

    /** Training bag: returns (newState, strength gained this session). */
    fun trainStrength(state: PetState, hits: Int): Pair<PetState, Int> {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state to 0
        val gain = minOf(18, hits / 4)
        val trAtk = minOf(100, state.trAtk + gain)
        val energy = dropTo(state.energy, 12, 5)
        val fullness = dropTo(state.fullness, 5, 5)
        val weight = maxOf(0, state.weight - hits / 3)
        val joy = clamp100(state.joy + 6)
        val strHi = maxOf(state.strHi, hits)
        var s = state.copy(trAtk = trAtk, energy = energy, fullness = fullness, weight = weight, joy = joy, strHi = strHi)
        s = addBond(s, 2)
        s = registerCare(s)
        return s to gain
    }

    fun clean(state: PetState): PetState {
        if (state.ceremony != Ceremony.NONE) return state
        var s = state.copy(poops = 0, hygiene = 100)
        s = addBond(s, 1)
        return registerCare(s)
    }

    fun caress(state: PetState): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg || state.sleeping) return state
        var s = state.copy(joy = clamp100(state.joy + 5))
        s = addBond(s, 1)
        return registerCare(s)
    }

    fun rename(state: PetState, nickname: String): PetState = state.copy(nickname = nickname.take(11))

    fun toggleLight(state: PetState): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state
        return state.copy(sleeping = !state.sleeping)
    }

    fun eggTap(state: PetState, dex: DexTable, rng: Random = Random.Default): PetState {
        if (!state.isEgg) return state
        val taps = state.eggTaps + 1
        return if (taps >= 3) hatch(state.copy(eggTaps = taps), dex, rng) else state.copy(eggTaps = taps)
    }

    fun newEgg(state: PetState, dex: DexTable, rng: Random = Random.Default): PetState {
        val eggTarget = pickEggSpecies(state, dex, rng)
        val starterPick = state.registeredCount() == 0
        val shinyBase = maxOf(8, (if (state.lastEnd == Ceremony.FAREWELL) 24 else 48) - careBonus(state))
        val eggShiny = rng.nextInt(shinyBase) == 0
        return PetState(
            fullness = 80, joy = 80, energy = 80, hygiene = 100, poops = 0, weight = 0,
            speciesId = -1, prevSpeciesId = -1,
            ceremony = Ceremony.NONE, lastEnd = state.lastEnd,
            dexRegistered = state.dexRegistered, dexShinyRegistered = state.dexShinyRegistered,
            streak = state.streak, bestStreak = state.bestStreak, lastCareDayEpochDay = state.lastCareDayEpochDay,
            totalMedals = state.totalMedals, gameHi = state.gameHi, strHi = state.strHi,
            eggTarget = eggTarget, eggShiny = eggShiny, eggTaps = 0, starterPick = starterPick,
            ageMinutes = 0, careMistakes = 0, mistakeCooldown = 0, sleeping = false,
            lastSeenEpochSeconds = state.lastSeenEpochSeconds,
        )
    }

    fun canEvolveNow(state: PetState, dex: DexTable): Boolean {
        if (state.isEgg || state.sleeping || state.ceremony != Ceremony.NONE) return false
        val d = dex[state.speciesId]
        if (d.evolvesTo == 0) return false
        return state.level() >= d.evolveLevel + state.careMistakes && state.lowestStat() >= 40
    }

    fun evolve(state: PetState, dex: DexTable, rng: Random = Random.Default): PetState {
        if (!canEvolveNow(state, dex)) return state
        val d = dex[state.speciesId]
        var next = d.evolvesTo
        if (state.speciesId == dex.eeveeId) {
            val opts = dex.eeveeEvolutions.filter { !state.isRegistered(it) }
            next = if (opts.isNotEmpty()) opts[rng.nextInt(opts.size)] else dex.eeveeEvolutions[rng.nextInt(dex.eeveeEvolutions.size)]
        }
        var s = state.copy(prevSpeciesId = state.speciesId, speciesId = next)
        return registerSpecies(s, next, s.shiny)
    }

    fun canFarewellNow(state: PetState, dex: DexTable): Boolean =
        !state.isEgg && !state.sleeping && state.ceremony == Ceremony.NONE &&
            dex[state.speciesId].evolvesTo == 0 && state.ageMinutes >= PetState.FAREWELL_AGE_MIN

    fun canRunawayNow(state: PetState): Boolean =
        !state.isEgg && !state.sleeping && state.ceremony == Ceremony.NONE &&
            state.neglectTicks >= PetState.RUNAWAY_TICKS

    /** "Evolve?" button should show: eligible, and not already declined at this level. */
    fun wantEvolveButton(state: PetState, dex: DexTable): Boolean =
        canEvolveNow(state, dex) && state.level() > state.evoDeclinedLevel

    /** "Say goodbye?" button should show: eligible, and not declined within the last day. */
    fun wantFarewellButton(state: PetState, dex: DexTable): Boolean =
        canFarewellNow(state, dex) && state.ageMinutes >= state.farewellDeclinedAgeMinutes

    /** "Keep form": postpone the evolution offer until the next level-up. */
    fun declineEvolve(state: PetState): PetState = state.copy(evoDeclinedLevel = state.level())

    /** "Stay together": postpone the farewell offer by one in-game day. */
    fun declineFarewell(state: PetState): PetState = state.copy(farewellDeclinedAgeMinutes = state.ageMinutes + 1440)

    /** First game only: overrides the (already-rolled) hidden egg target with the player's chosen starter. */
    fun chooseStarter(state: PetState, starterDex: Int): PetState {
        if (!state.starterPick) return state
        return state.copy(eggTarget = starterDex, starterPick = false)
    }

    fun startFarewell(state: PetState): PetState {
        if (state.isEgg || state.ceremony != Ceremony.NONE) return state
        return state.copy(lastEnd = Ceremony.FAREWELL, ceremony = Ceremony.FAREWELL)
    }

    fun startRunaway(state: PetState): PetState {
        if (state.isEgg || state.ceremony != Ceremony.NONE) return state
        return state.copy(lastEnd = Ceremony.RUNAWAY, ceremony = Ceremony.RUNAWAY)
    }

    fun release(state: PetState): PetState {
        if (state.isEgg || state.ceremony != Ceremony.NONE) return state
        return state.copy(lastEnd = Ceremony.RELEASE, ceremony = Ceremony.RELEASE)
    }

    fun mood(state: PetState, eating: Boolean): PetMood = when {
        state.sleeping -> PetMood.SLEEPING
        eating -> PetMood.EATING
        state.lowestStat() < 25 -> PetMood.SAD
        else -> PetMood.HAPPY
    }

    fun atkStat(state: PetState, dex: DexTable): Int =
        if (state.isEgg) 0 else calcStat(dex[state.speciesId].baseAtk, state.geneAtk, state.level(), state.trAtk)

    fun defStat(state: PetState, dex: DexTable): Int =
        if (state.isEgg) 0 else calcStat(dex[state.speciesId].baseDef, state.geneDef, state.level(), state.trDef)

    fun speStat(state: PetState, dex: DexTable): Int =
        if (state.isEgg) 0 else calcStat(dex[state.speciesId].baseSpe, state.geneSpe, state.level(), state.trSpe)

    /** Ported from TamaPoke.ino's petBattleStats(): the pet's current fighting stats/typing. */
    fun petBattleStats(state: PetState, dex: DexTable): BattleStats {
        if (state.isEgg) return BattleStats(atk = 0, def = 0, spe = 0, level = state.level())
        val entry = dex[state.speciesId]
        return BattleStats(
            atk = atkStat(state, dex),
            def = defStat(state, dex),
            spe = speStat(state, dex),
            level = state.level(),
            type1 = entry.battleType1,
            type2 = entry.battleType2,
        )
    }

    fun eggRarity(state: PetState, dex: DexTable): Rarity =
        if (state.eggTarget in 1..dex.count) dex[state.eggTarget].rarity else Rarity.COMMON

    fun careBonus(state: PetState): Int = minOf(state.streak, 30) / 3 + state.bond / 25

    fun lineHasUnregistered(state: PetState, dex: DexTable, base: Int): Boolean {
        var cur = base
        var guard = 0
        while (cur in 1..dex.count && guard < 6) {
            guard++
            if (!state.isRegistered(cur)) return true
            if (cur == dex.eeveeId) return dex.eeveeEvolutions.any { !state.isRegistered(it) }
            cur = dex[cur].evolvesTo
        }
        return false
    }

    /** Rolls the hidden species for the next egg: rarity tier biased by streak/bond/last ending, then biased toward incomplete evolution lines. */
    fun pickEggSpecies(state: PetState, dex: DexTable, rng: Random = Random.Default): Int {
        if (state.registeredCount() == 0) {
            return dex.classicStarters[rng.nextInt(dex.classicStarters.size)]
        }

        var tier = Rarity.COMMON
        if (state.lastEnd != Ceremony.RUNAWAY) {
            val blessed = state.lastEnd == Ceremony.FAREWELL
            val rare = (if (blessed) 45 else 27) + careBonus(state)
            val leg = if (state.registeredCount() >= 25) (if (blessed) 10 else 3) + careBonus(state) / 3 else 0
            val r = rng.nextInt(100)
            tier = when {
                r < leg -> Rarity.LEGENDARY
                r < leg + rare -> Rarity.RARE
                else -> Rarity.COMMON
            }
        }

        for (pass in 0..1) {
            var t = tier.ordinal
            while (t >= Rarity.COMMON.ordinal) {
                val rarity = Rarity.entries[t]
                val candidates = dex.entriesOfRarity(rarity).filter { entry ->
                    pass != 0 || lineHasUnregistered(state, dex, entry.id)
                }
                if (candidates.isNotEmpty()) return candidates[rng.nextInt(candidates.size)].id
                t--
            }
        }
        return dex.classicStarters[rng.nextInt(dex.classicStarters.size)]
    }

    // ---- personality, daily goals, extra minigames, pet events, battle rewards ----
    // (ported from the ShadowEnemy expanded fork: github.com/ShadowEnemyx/TamaPoke, tamapoke-expanded-update)

    fun personality(state: PetState): PetPersonality {
        if (state.isEgg) return PetPersonality.BALANCED
        if (state.weight >= 72 || state.energy <= 20) return PetPersonality.LAZY
        if (state.battleWins >= 8 || state.bestBattleStreak >= 4) return PetPersonality.BRAVE
        if (state.catchHi >= 18 || state.memoHi >= 8 || state.gameHi >= 24 || state.trSpe >= 55) return PetPersonality.PLAYFUL
        if ((state.bond >= 45 && state.careMistakes <= 1) || (state.streak >= 5 && state.careMistakes == 0)) return PetPersonality.CALM
        return PetPersonality.BALANCED
    }

    fun dailyGoalTarget(goalType: DailyGoalType): Int = when (goalType) {
        DailyGoalType.CATCH -> 5
        DailyGoalType.MEMO -> 3
        else -> 1
    }

    fun dailyGoalComplete(state: PetState, index: Int): Boolean =
        index in 0..2 && (state.dailyGoalDone and (1 shl index)) != 0

    private val DAILY_GOAL_POOL = listOf(
        DailyGoalType.CARE, DailyGoalType.PLAY, DailyGoalType.CATCH, DailyGoalType.MEMO, DailyGoalType.BATTLE,
    )

    /** Rolls today's 3 daily goals if a new day has started (player-wide, like the streak). */
    fun ensureDailyGoals(state: PetState): PetState {
        if (state.isEgg || state.ceremony != Ceremony.NONE) return state
        val d = today(state.lastSeenEpochSeconds)
        if (d == 0L || d == state.dailyGoalDay) return state
        val seed = ((d + (if (state.speciesId > 0) state.speciesId else 0)) % 5).toInt()
        val types = (0 until 3).map { i -> DAILY_GOAL_POOL[(seed + i) % 5].ordinal }
        return state.copy(dailyGoalType = types, dailyGoalProgress = listOf(0, 0, 0), dailyGoalDone = 0, dailyGoalDay = d)
    }

    private fun applyDailyReward(state: PetState): PetState = addBond(state.copy(joy = clamp100(state.joy + 4)), 1)

    /** Advances progress on any daily-goal slot matching [goalType]; rewards once if a slot completes. */
    fun noteDailyGoal(state: PetState, goalType: DailyGoalType, amount: Int): PetState {
        if (state.isEgg || state.ceremony != Ceremony.NONE || amount <= 0) return state
        var s = ensureDailyGoals(state)
        if (s.dailyGoalDay == 0L) return s
        val progress = s.dailyGoalProgress.toMutableList()
        var done = s.dailyGoalDone
        for (i in 0 until 3) {
            if (s.dailyGoalType.getOrNull(i) != goalType.ordinal || dailyGoalComplete(s, i)) continue
            val target = dailyGoalTarget(goalType)
            progress[i] = (progress[i] + amount).coerceAtMost(target)
            if (progress[i] >= target) done = done or (1 shl i)
        }
        s = s.copy(dailyGoalProgress = progress, dailyGoalDone = done)
        if (done != state.dailyGoalDone) s = applyDailyReward(s)
        return s
    }

    /** Catch minigame: tap the target before it vanishes. Trains SPEED. */
    fun applyCatchResult(state: PetState, score: Int): Pair<PetState, Int> {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state to 0
        val gain = minOf(12, score / 3)
        val trSpe = minOf(100, state.trSpe + gain)
        val joy = clamp100(state.joy + 4 + (if (score > 12) 20 else score))
        val energy = dropTo(state.energy, 8 + score / 3, 5)
        val fullness = dropTo(state.fullness, 4, 5)
        val weight = maxOf(0, state.weight - score)
        val catchHi = maxOf(state.catchHi, score)
        var s = state.copy(trSpe = trSpe, joy = joy, energy = energy, fullness = fullness, weight = weight, catchHi = catchHi)
        s = addBond(s, 1)
        s = registerCare(s)
        s = noteDailyGoal(s, DailyGoalType.CATCH, score)
        return s to gain
    }

    /** Memo (Simon-says) minigame. Trains DEFENSE. */
    fun applyMemoResult(state: PetState, rounds: Int): Pair<PetState, Int> {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state to 0
        val gain = minOf(10, rounds / 2)
        val trDef = minOf(100, state.trDef + gain)
        val joy = clamp100(state.joy + 5 + (if (rounds > 8) 18 else rounds * 2))
        val energy = dropTo(state.energy, 6 + rounds / 2, 5)
        val fullness = dropTo(state.fullness, 3, 5)
        val memoHi = maxOf(state.memoHi, rounds)
        var s = state.copy(trDef = trDef, joy = joy, energy = energy, fullness = fullness, memoHi = memoHi)
        s = addBond(s, 2)
        s = registerCare(s)
        s = noteDailyGoal(s, DailyGoalType.MEMO, rounds)
        return s to gain
    }

    /** Clean minigame: an extra way to clear poops/raise hygiene beyond a plain bath. */
    fun applyCleanResult(state: PetState, score: Int): Pair<PetState, Int> {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state to 0
        val gain = minOf(18, score / 2)
        val hygiene = clamp100(state.hygiene + 20 + score * 3)
        val joy = clamp100(state.joy + 3 + (if (score > 10) 12 else score))
        val energy = dropTo(state.energy, 4 + score / 4, 8)
        val poops = if (state.poops > 0 && score >= 4) state.poops - 1 else state.poops
        val cleanHi = maxOf(state.cleanHi, score)
        var s = state.copy(hygiene = hygiene, joy = joy, energy = energy, poops = poops, cleanHi = cleanHi)
        s = addBond(s, 1)
        s = registerCare(s)
        s = noteDailyGoal(s, DailyGoalType.CARE, 1)
        return s to gain
    }

    /** Type-match minigame: teaches the same type logic used in battle. Trains STRENGTH. */
    fun applyTypeResult(state: PetState, score: Int): Pair<PetState, Int> {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state to 0
        val gain = minOf(10, score / 4)
        val trAtk = minOf(100, state.trAtk + gain)
        val joy = clamp100(state.joy + 4 + (if (score > 12) 18 else score))
        val energy = dropTo(state.energy, 5 + score / 3, 8)
        val fullness = dropTo(state.fullness, 2, 5)
        val typeHi = maxOf(state.typeHi, score)
        var s = state.copy(trAtk = trAtk, joy = joy, energy = energy, fullness = fullness, typeHi = typeHi)
        s = addBond(s, 1)
        s = registerCare(s)
        s = noteDailyGoal(s, DailyGoalType.PLAY, 1)
        return s to gain
    }

    /** A spontaneous pet event (favorite-berry find, affectionate nuzzle, or a lucky sparkle). */
    fun applyPetEvent(state: PetState, eventType: PetEventType): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state
        var s = when (eventType) {
            PetEventType.BERRY -> state.copy(fullness = clamp100(state.fullness + 10), joy = clamp100(state.joy + 4))
            PetEventType.HEART -> addBond(state.copy(joy = clamp100(state.joy + 6)), 1)
            PetEventType.SPARKLE -> {
                val joy = clamp100(state.joy + 5)
                if (state.energy <= state.hygiene) {
                    state.copy(joy = joy, energy = clamp100(state.energy + 3))
                } else {
                    state.copy(joy = joy, hygiene = clamp100(state.hygiene + 3))
                }
            }
        }
        s = registerCare(s)
        return noteDailyGoal(s, DailyGoalType.CARE, 1)
    }

    /**
     * A gentle pet (distinct from [caress]): rate-limited to once per 10 game-minutes,
     * personality-flavored joy/energy/bond gains. [eveningBonus] is caller-supplied (real
     * wall-clock time of day), since the engine has no clock of its own.
     */
    fun interactPet(state: PetState, eveningBonus: Boolean): Pair<PetState, PetInteractResult> {
        if (state.ceremony != Ceremony.NONE || state.isEgg || state.sleeping) return state to PetInteractResult()
        val nowMinute = if (state.ageMinutes > 0) state.ageMinutes else 1
        if (state.lastPetInteractMinute != 0L && nowMinute < state.lastPetInteractMinute + 10) {
            return state to PetInteractResult()
        }
        val p = personality(state)
        val joyGain = if (p == PetPersonality.PLAYFUL) 4 else 2
        var s = state.copy(lastPetInteractMinute = nowMinute, joy = clamp100(state.joy + joyGain))
        var energyGained = false
        if (p == PetPersonality.LAZY) {
            s = s.copy(energy = clamp100(s.energy + 2))
            energyGained = true
        }
        val bondEligible = eveningBonus || p == PetPersonality.CALM || (p == PetPersonality.BRAVE && state.battleWins > 0)
        var bondGained = false
        if (bondEligible) {
            val before = s.bond
            s = addBond(s, 1)
            bondGained = s.bond > before
        }
        s = registerCare(s)
        s = noteDailyGoal(s, DailyGoalType.CARE, 1)
        return s to PetInteractResult(joyGained = true, bondGained = bondGained, energyGained = energyGained)
    }

    fun nextDexGoal(state: PetState): Int {
        val goals = intArrayOf(10, 25, 50, 100, 151)
        val known = state.knownDexCount()
        return goals.firstOrNull { known < it } ?: 151
    }

    /** Registers a battle-caught wild Pokemon (separate from bred [PetState.dexRegistered]); may trigger dex-milestone rewards. */
    fun registerCaught(state: PetState, dexId: Int): PetState {
        if (dexId !in 1..151) return state
        val wasKnown = state.isRegistered(dexId) || state.isCaught(dexId)
        var s = state.copy(dexCaught = state.dexCaught + dexId)
        s = noteDailyGoal(s, DailyGoalType.CATCH, 1)
        if (!wasKnown) s = applyDexRewards(s)
        return s
    }

    /** Milestone rewards for 10/25/50/100/151 known species (bred + caught combined). */
    fun applyDexRewards(state: PetState): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state
        val goals = intArrayOf(10, 25, 50, 100, 151)
        val known = state.knownDexCount()
        var s = state
        for ((i, goal) in goals.withIndex()) {
            val bit = 1 shl i
            if (known < goal || (s.dexRewardMask and bit) != 0) continue
            s = s.copy(dexRewardMask = s.dexRewardMask or bit)
            s = when (goal) {
                10 -> s.copy(joy = clamp100(s.joy + 5))
                25 -> addBond(s, 2)
                50 -> when {
                    s.trAtk <= s.trDef && s.trAtk <= s.trSpe -> s.copy(trAtk = clamp100(s.trAtk + 1))
                    s.trDef <= s.trAtk && s.trDef <= s.trSpe -> s.copy(trDef = clamp100(s.trDef + 1))
                    else -> s.copy(trSpe = clamp100(s.trSpe + 1))
                }
                100 -> addBond(s, 4)
                else -> s
            }
        }
        return s
    }

    /** Wild-battle win: trains whichever stat the wild's dominant base-stat corresponds to. */
    fun applyBattleWin(state: PetState, wild: DexEntry, closeWin: Boolean): Pair<PetState, BattleReward> {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state to BattleReward()
        var amount = if (wild.rarity == Rarity.RARE) 2 else 1
        if (closeWin) amount++
        var s = state
        val stat: BattleRewardStat
        s = when {
            wild.baseAtk >= wild.baseDef && wild.baseAtk >= wild.baseSpe -> {
                stat = BattleRewardStat.DEF
                s.copy(trDef = clamp100(s.trDef + amount))
            }
            wild.baseDef >= wild.baseAtk && wild.baseDef >= wild.baseSpe -> {
                stat = BattleRewardStat.ATK
                s.copy(trAtk = clamp100(s.trAtk + amount))
            }
            else -> {
                stat = BattleRewardStat.SPE
                s.copy(trSpe = clamp100(s.trSpe + amount))
            }
        }
        s = s.copy(
            battleWins = s.battleWins + 1,
            battleStreak = s.battleStreak + 1,
            joy = clamp100(s.joy + 8 + (if (closeWin) 4 else 0)),
            energy = dropTo(s.energy, 8, 20),
            fullness = dropTo(s.fullness, 3, 10),
        )
        s = s.copy(bestBattleStreak = maxOf(s.bestBattleStreak, s.battleStreak))
        s = addBond(s, if (closeWin) 3 else 2)
        s = registerCare(s)
        s = noteDailyGoal(s, DailyGoalType.BATTLE, 1)
        return s to BattleReward(stat, amount)
    }

    fun applyBattleLoss(state: PetState): PetState {
        if (state.ceremony != Ceremony.NONE || state.isEgg) return state
        return state.copy(
            battleLosses = state.battleLosses + 1,
            battleStreak = 0,
            joy = dropTo(state.joy, 12, 20),
            energy = dropTo(state.energy, 18, 20),
            fullness = dropTo(state.fullness, 4, 10),
        )
    }

    // ---- internal helpers ----

    private fun hatch(state: PetState, dex: DexTable, rng: Random): PetState {
        val speciesId = state.eggTarget
        var s = state.copy(
            speciesId = speciesId,
            shiny = state.eggShiny,
            geneAtk = 90 + rng.nextInt(21),
            geneDef = 90 + rng.nextInt(21),
            geneSpe = 90 + rng.nextInt(21),
            trAtk = 0, trDef = 0, trSpe = 0,
            berryKnown = false,
            bond = 0, bondToday = 0,
            medals = emptySet(),
            nickname = "",
        )
        s = registerSpecies(s, speciesId, s.shiny)
        val (s2, _) = checkMedals(s, dex)
        return s2
    }

    private fun registerSpecies(state: PetState, dex: Int, shiny: Boolean): PetState {
        if (dex !in 1..151) return state
        val reg = state.dexRegistered + dex
        val shinyReg = if (shiny) state.dexShinyRegistered + dex else state.dexShinyRegistered
        return state.copy(dexRegistered = reg, dexShinyRegistered = shinyReg)
    }

    private fun checkMedals(state: PetState, dex: DexTable): Pair<PetState, List<PetEvent>> {
        if (state.isEgg) return state to emptyList()
        val before = state.medals
        val medals = before.toMutableSet()
        if (state.level() >= 10) medals += Medal.LV10
        if (state.level() >= 25) medals += Medal.LV25
        if (state.level() >= 50) medals += Medal.LV50
        if (state.berryKnown) medals += Medal.BERRY
        if (state.streak >= 7) medals += Medal.STREAK7
        if (state.bond >= 100) medals += Medal.BOND
        if (dex[state.speciesId].evolvesTo == 0) medals += Medal.FINAL
        if (state.weight == 0 && state.level() >= 5 && state.careMistakes == 0) medals += Medal.FIT
        val gained = medals - before
        val newState = state.copy(medals = medals, totalMedals = state.totalMedals + gained.size)
        return newState to if (gained.isNotEmpty()) listOf(PetEvent.MedalsGained(gained)) else emptyList()
    }

    private fun registerCare(state: PetState): PetState {
        if (state.isEgg || state.ceremony != Ceremony.NONE) return state
        val d = today(state.lastSeenEpochSeconds)
        if (d == 0L || d == state.lastCareDayEpochDay) return state
        var streak = state.streak
        var lastMilestone = state.lastMilestone
        if (state.lastCareDayEpochDay == 0L || d == state.lastCareDayEpochDay + 1) {
            streak++
        } else {
            streak = 1
            lastMilestone = 0
        }
        val bestStreak = maxOf(state.bestStreak, streak)
        val bond = clamp100(state.bond + 4)
        val ms = when {
            streak >= 100 -> 100
            streak >= 30 -> 30
            streak >= 7 -> 7
            streak >= 3 -> 3
            else -> 0
        }
        if (ms > lastMilestone) lastMilestone = ms
        return state.copy(
            streak = streak, lastCareDayEpochDay = d, bondToday = 0,
            bestStreak = bestStreak, bond = bond, lastMilestone = lastMilestone,
        )
    }

    private fun addBond(state: PetState, amt: Int): PetState {
        if (state.bondToday >= 8) return state
        return state.copy(bond = clamp100(state.bond + amt), bondToday = state.bondToday + amt)
    }

    private fun today(epochSeconds: Long): Long = if (epochSeconds != 0L) epochSeconds / 86400 else 0L

    private fun calcStat(base: Int, gene: Int, lvl: Int, tr: Int): Int = base * gene / 100 + lvl + tr

    private fun clamp100(v: Int): Int = v.coerceIn(0, 100)

    /** Mirrors dropTo(): decays toward a floor, never below it. */
    private fun dropTo(v: Int, d: Int, floor: Int): Int {
        if (v <= floor) return v
        return if (v - floor > d) v - d else floor
    }
}
