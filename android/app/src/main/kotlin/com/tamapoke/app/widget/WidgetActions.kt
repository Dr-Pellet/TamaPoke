package com.tamapoke.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.tamapoke.app.data.PetRepository

class FeedWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = PetRepository.get(context)
        repository.catchUp()
        repository.feed()
        PetWidget().update(context, glanceId)
    }
}

class PetWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = PetRepository.get(context)
        repository.catchUp()
        repository.caress()
        PetWidget().update(context, glanceId)
    }
}
