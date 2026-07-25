package com.tamapoke.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.tamapoke.core.PetState
import com.tamapoke.core.enums.Ceremony
import com.tamapoke.core.enums.Medal

/** Single-row table: like the original device, this app raises one pet at a time. */
@Entity(tableName = "pet")
@TypeConverters(Converters::class)
data class PetEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val fullness: Int,
    val joy: Int,
    val energy: Int,
    val hygiene: Int,
    val poops: Int,
    val weight: Int,
    val geneAtk: Int,
    val geneDef: Int,
    val geneSpe: Int,
    val trAtk: Int,
    val trDef: Int,
    val trSpe: Int,
    val berryKnown: Boolean,
    val shiny: Boolean,
    val ageMinutes: Long,
    val speciesId: Int,
    val prevSpeciesId: Int,
    val careMistakes: Int,
    val sleeping: Boolean,
    val lastSeenEpochSeconds: Long,
    val ceremony: Ceremony,
    val lastEnd: Ceremony,
    val dexRegistered: Set<Int>,
    val dexShinyRegistered: Set<Int>,
    val streak: Int,
    val bestStreak: Int,
    val lastCareDayEpochDay: Long,
    val bond: Int,
    val nickname: String,
    val medals: Set<Medal>,
    val totalMedals: Int,
    val lastMilestone: Int,
    val gameHi: Int,
    val strHi: Int,
    val eggTarget: Int,
    val eggShiny: Boolean,
    val eggTaps: Int,
    val starterPick: Boolean,
    val mistakeCooldown: Int,
    val evoDeclinedLevel: Int,
    val farewellDeclinedAgeMinutes: Long,
    val neglectTicks: Int,
    val goodTicks: Int,
    val bondToday: Int,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

fun PetState.toEntity(): PetEntity = PetEntity(
    fullness = fullness, joy = joy, energy = energy, hygiene = hygiene, poops = poops, weight = weight,
    geneAtk = geneAtk, geneDef = geneDef, geneSpe = geneSpe, trAtk = trAtk, trDef = trDef, trSpe = trSpe,
    berryKnown = berryKnown, shiny = shiny, ageMinutes = ageMinutes, speciesId = speciesId,
    prevSpeciesId = prevSpeciesId, careMistakes = careMistakes, sleeping = sleeping,
    lastSeenEpochSeconds = lastSeenEpochSeconds, ceremony = ceremony, lastEnd = lastEnd,
    dexRegistered = dexRegistered, dexShinyRegistered = dexShinyRegistered, streak = streak,
    bestStreak = bestStreak, lastCareDayEpochDay = lastCareDayEpochDay, bond = bond, nickname = nickname,
    medals = medals, totalMedals = totalMedals, lastMilestone = lastMilestone, gameHi = gameHi, strHi = strHi,
    eggTarget = eggTarget, eggShiny = eggShiny, eggTaps = eggTaps, starterPick = starterPick,
    mistakeCooldown = mistakeCooldown, evoDeclinedLevel = evoDeclinedLevel,
    farewellDeclinedAgeMinutes = farewellDeclinedAgeMinutes, neglectTicks = neglectTicks,
    goodTicks = goodTicks, bondToday = bondToday,
)

fun PetEntity.toState(): PetState = PetState(
    fullness = fullness, joy = joy, energy = energy, hygiene = hygiene, poops = poops, weight = weight,
    geneAtk = geneAtk, geneDef = geneDef, geneSpe = geneSpe, trAtk = trAtk, trDef = trDef, trSpe = trSpe,
    berryKnown = berryKnown, shiny = shiny, ageMinutes = ageMinutes, speciesId = speciesId,
    prevSpeciesId = prevSpeciesId, careMistakes = careMistakes, sleeping = sleeping,
    lastSeenEpochSeconds = lastSeenEpochSeconds, ceremony = ceremony, lastEnd = lastEnd,
    dexRegistered = dexRegistered, dexShinyRegistered = dexShinyRegistered, streak = streak,
    bestStreak = bestStreak, lastCareDayEpochDay = lastCareDayEpochDay, bond = bond, nickname = nickname,
    medals = medals, totalMedals = totalMedals, lastMilestone = lastMilestone, gameHi = gameHi, strHi = strHi,
    eggTarget = eggTarget, eggShiny = eggShiny, eggTaps = eggTaps, starterPick = starterPick,
    mistakeCooldown = mistakeCooldown, evoDeclinedLevel = evoDeclinedLevel,
    farewellDeclinedAgeMinutes = farewellDeclinedAgeMinutes, neglectTicks = neglectTicks,
    goodTicks = goodTicks, bondToday = bondToday,
)
