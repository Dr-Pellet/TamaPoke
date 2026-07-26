package com.tamapoke.app.data

import com.tamapoke.core.PetState
import com.tamapoke.core.enums.Ceremony
import com.tamapoke.core.enums.Medal
import org.json.JSONArray
import org.json.JSONObject

/**
 * Full-fidelity JSON (de)serialization of [PetState] for export/import to a
 * user-chosen local file (Settings > Save data) - a save-file backup/
 * transfer feature the original firmware doesn't have (NVS only survives
 * on the device itself), but a natural fit for a phone.
 */
object SaveFileCodec {
    private const val FORMAT_VERSION = 1

    fun encode(state: PetState): String {
        val o = JSONObject()
        o.put("formatVersion", FORMAT_VERSION)
        o.put("fullness", state.fullness)
        o.put("joy", state.joy)
        o.put("energy", state.energy)
        o.put("hygiene", state.hygiene)
        o.put("poops", state.poops)
        o.put("weight", state.weight)
        o.put("geneAtk", state.geneAtk)
        o.put("geneDef", state.geneDef)
        o.put("geneSpe", state.geneSpe)
        o.put("trAtk", state.trAtk)
        o.put("trDef", state.trDef)
        o.put("trSpe", state.trSpe)
        o.put("berryKnown", state.berryKnown)
        o.put("shiny", state.shiny)
        o.put("ageMinutes", state.ageMinutes)
        o.put("speciesId", state.speciesId)
        o.put("prevSpeciesId", state.prevSpeciesId)
        o.put("careMistakes", state.careMistakes)
        o.put("sleeping", state.sleeping)
        o.put("lastSeenEpochSeconds", state.lastSeenEpochSeconds)
        o.put("ceremony", state.ceremony.name)
        o.put("lastEnd", state.lastEnd.name)
        o.put("dexRegistered", JSONArray(state.dexRegistered.toList()))
        o.put("dexShinyRegistered", JSONArray(state.dexShinyRegistered.toList()))
        o.put("streak", state.streak)
        o.put("bestStreak", state.bestStreak)
        o.put("lastCareDayEpochDay", state.lastCareDayEpochDay)
        o.put("bond", state.bond)
        o.put("nickname", state.nickname)
        o.put("medals", JSONArray(state.medals.map { it.name }))
        o.put("totalMedals", state.totalMedals)
        o.put("lastMilestone", state.lastMilestone)
        o.put("gameHi", state.gameHi)
        o.put("strHi", state.strHi)
        o.put("eggTarget", state.eggTarget)
        o.put("eggShiny", state.eggShiny)
        o.put("eggTaps", state.eggTaps)
        o.put("starterPick", state.starterPick)
        o.put("mistakeCooldown", state.mistakeCooldown)
        o.put("evoDeclinedLevel", state.evoDeclinedLevel)
        o.put("farewellDeclinedAgeMinutes", state.farewellDeclinedAgeMinutes)
        o.put("neglectTicks", state.neglectTicks)
        o.put("goodTicks", state.goodTicks)
        o.put("bondToday", state.bondToday)
        return o.toString(2)
    }

    fun decode(json: String): PetState {
        val o = JSONObject(json)
        fun intSet(key: String): Set<Int> {
            val arr = o.getJSONArray(key)
            return (0 until arr.length()).map { arr.getInt(it) }.toSet()
        }
        return PetState(
            fullness = o.getInt("fullness"),
            joy = o.getInt("joy"),
            energy = o.getInt("energy"),
            hygiene = o.getInt("hygiene"),
            poops = o.getInt("poops"),
            weight = o.getInt("weight"),
            geneAtk = o.getInt("geneAtk"),
            geneDef = o.getInt("geneDef"),
            geneSpe = o.getInt("geneSpe"),
            trAtk = o.getInt("trAtk"),
            trDef = o.getInt("trDef"),
            trSpe = o.getInt("trSpe"),
            berryKnown = o.getBoolean("berryKnown"),
            shiny = o.getBoolean("shiny"),
            ageMinutes = o.getLong("ageMinutes"),
            speciesId = o.getInt("speciesId"),
            prevSpeciesId = o.getInt("prevSpeciesId"),
            careMistakes = o.getInt("careMistakes"),
            sleeping = o.getBoolean("sleeping"),
            lastSeenEpochSeconds = o.getLong("lastSeenEpochSeconds"),
            ceremony = Ceremony.valueOf(o.getString("ceremony")),
            lastEnd = Ceremony.valueOf(o.getString("lastEnd")),
            dexRegistered = intSet("dexRegistered"),
            dexShinyRegistered = intSet("dexShinyRegistered"),
            streak = o.getInt("streak"),
            bestStreak = o.getInt("bestStreak"),
            lastCareDayEpochDay = o.getLong("lastCareDayEpochDay"),
            bond = o.getInt("bond"),
            nickname = o.getString("nickname"),
            medals = o.getJSONArray("medals").let { arr ->
                (0 until arr.length()).map { Medal.valueOf(arr.getString(it)) }.toSet()
            },
            totalMedals = o.getInt("totalMedals"),
            lastMilestone = o.getInt("lastMilestone"),
            gameHi = o.getInt("gameHi"),
            strHi = o.getInt("strHi"),
            eggTarget = o.getInt("eggTarget"),
            eggShiny = o.getBoolean("eggShiny"),
            eggTaps = o.getInt("eggTaps"),
            starterPick = o.getBoolean("starterPick"),
            mistakeCooldown = o.getInt("mistakeCooldown"),
            evoDeclinedLevel = o.getInt("evoDeclinedLevel"),
            farewellDeclinedAgeMinutes = o.getLong("farewellDeclinedAgeMinutes"),
            neglectTicks = o.getInt("neglectTicks"),
            goodTicks = o.getInt("goodTicks"),
            bondToday = o.getInt("bondToday"),
        )
    }
}
