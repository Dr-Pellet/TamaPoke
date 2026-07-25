package com.tamapoke.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tamapoke.app.audio.SfxPlayer
import com.tamapoke.app.data.PetRepository
import com.tamapoke.app.data.SettingsRepository
import com.tamapoke.app.worker.CatchUpWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TamaPokeApp : Application() {
    val repository: PetRepository by lazy { PetRepository.get(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository.get(this) }
    val sfxPlayer: SfxPlayer by lazy { SfxPlayer() }
    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicCatchUp()
        appScope.launch {
            settingsRepository.soundOn.collect { sfxPlayer.setEnabled(it) }
        }
        appScope.launch {
            settingsRepository.language.collect { code ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
            }
        }
    }

    private fun schedulePeriodicCatchUp() {
        // 15 minutes is WorkManager's floor for periodic work; the math itself is
        // elapsed-time based, so imprecise timing here only affects freshness, not correctness.
        val request = PeriodicWorkRequestBuilder<CatchUpWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CatchUpWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
