package com.tamapoke.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tamapoke.app.data.PetRepository
import com.tamapoke.core.dex.DexTable
import kotlinx.coroutines.launch

class MainViewModel(private val repository: PetRepository) : ViewModel() {
    val state = repository.state
    val dex: DexTable get() = repository.dex

    init {
        viewModelScope.launch { repository.catchUp() }
    }

    fun onResume() {
        viewModelScope.launch { repository.catchUp() }
    }

    /** Drives the visible per-minute decay while the app is in the foreground. */
    fun liveTick() {
        viewModelScope.launch { repository.tickOnce() }
    }

    fun feed() = viewModelScope.launch { repository.feed() }
    fun feedBerry(color: Int) = viewModelScope.launch { repository.feedBerry(color) }
    fun feedCandy() = viewModelScope.launch { repository.feedCandy() }
    fun play() = viewModelScope.launch { repository.play() }
    fun playResult(score: Int) = viewModelScope.launch { repository.playResult(score) }
    fun trainStrength(hits: Int) = viewModelScope.launch { repository.trainStrength(hits) }
    fun clean() = viewModelScope.launch { repository.clean() }
    fun caress() = viewModelScope.launch { repository.caress() }
    fun toggleLight() = viewModelScope.launch { repository.toggleLight() }
    fun eggTap() = viewModelScope.launch { repository.eggTap() }
    fun evolve() = viewModelScope.launch { repository.evolve() }
    fun startFarewell() = viewModelScope.launch { repository.startFarewell() }
    fun startRunaway() = viewModelScope.launch { repository.startRunaway() }
    fun release() = viewModelScope.launch { repository.release() }

    class Factory(private val repository: PetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository) as T
    }
}
