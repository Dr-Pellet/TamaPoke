package com.tamapoke.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tamapoke.app.audio.SoundMode
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
        val SOUND_MODE = stringPreferencesKey("sound_mode")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val soundMode: Flow<SoundMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.SOUND_MODE]?.let { runCatching { SoundMode.valueOf(it) }.getOrNull() } ?: SoundMode.FULL
    }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }

    suspend fun setSoundMode(mode: SoundMode) {
        context.dataStore.edit { it[Keys.SOUND_MODE] = mode.name }
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
