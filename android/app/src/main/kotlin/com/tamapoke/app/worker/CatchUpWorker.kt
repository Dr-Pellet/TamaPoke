package com.tamapoke.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.tamapoke.app.data.PetRepository
import com.tamapoke.app.widget.PetWidget

/**
 * Background equivalent of the firmware's RTC-driven syncClock(): applies
 * elapsed-time decay while the app isn't running, then refreshes the widget.
 */
class CatchUpWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        PetRepository.get(applicationContext).catchUp()
        PetWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "pet-catch-up"
    }
}
