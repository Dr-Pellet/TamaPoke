package com.tamapoke.app.data.db

import androidx.room.TypeConverter
import com.tamapoke.core.enums.Ceremony
import com.tamapoke.core.enums.Medal

class Converters {
    @TypeConverter
    fun ceremonyToString(value: Ceremony): String = value.name

    @TypeConverter
    fun stringToCeremony(value: String): Ceremony = Ceremony.valueOf(value)

    @TypeConverter
    fun intSetToString(value: Set<Int>): String = value.joinToString(",")

    @TypeConverter
    fun stringToIntSet(value: String): Set<Int> =
        if (value.isEmpty()) emptySet() else value.split(",").map { it.toInt() }.toSet()

    @TypeConverter
    fun medalSetToString(value: Set<Medal>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun stringToMedalSet(value: String): Set<Medal> =
        if (value.isEmpty()) emptySet() else value.split(",").map { Medal.valueOf(it) }.toSet()
}
