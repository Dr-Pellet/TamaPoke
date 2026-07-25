package com.tamapoke.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tamapoke.app.data.PetRepository
import com.tamapoke.app.worker.CatchUpWorker
import java.util.concurrent.TimeUnit

class TamaPokeApp : Application() {
    val repository: PetRepository by lazy { PetRepository.get(this) }

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicCatchUp()
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
