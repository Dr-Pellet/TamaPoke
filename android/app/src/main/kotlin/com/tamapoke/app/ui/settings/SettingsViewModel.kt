package com.tamapoke.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tamapoke.app.data.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val soundOn = repository.soundOn
    val language = repository.language

    fun setSoundOn(on: Boolean) = viewModelScope.launch { repository.setSoundOn(on) }
    fun setLanguage(code: String) = viewModelScope.launch { repository.setLanguage(code) }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
    }
}
