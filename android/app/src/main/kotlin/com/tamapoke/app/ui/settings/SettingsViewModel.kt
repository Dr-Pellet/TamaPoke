package com.tamapoke.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tamapoke.app.audio.SoundMode
import com.tamapoke.app.data.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val soundMode = repository.soundMode
    val language = repository.language

    fun setSoundMode(mode: SoundMode) = viewModelScope.launch { repository.setSoundMode(mode) }
    fun setLanguage(code: String) = viewModelScope.launch { repository.setLanguage(code) }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
    }
}
