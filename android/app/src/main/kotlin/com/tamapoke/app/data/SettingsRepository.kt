package com.tamapoke.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Settings orthogonal to pet state (language, sound) - kept in a separate
 * DataStore from the Room-backed [PetRepository] rather than overloading one
 * store with two different concerns.
 */
class SettingsRepository private constructor(private val context: Context) {
    private object Keys {
        val SOUND_ON = booleanPreferencesKey("sound_on")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val soundOn: Flow<Boolean> = context.dataStore.data.map { it[Keys.SOUND_ON] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }

    suspend fun setSoundOn(on: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ON] = on }
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = code }
    }

    companion object {
        @Volatile private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository = instance ?: synchronized(this) {
            instance ?: SettingsRepository(context.applicationContext).also { instance = it }
        }
    }
}
