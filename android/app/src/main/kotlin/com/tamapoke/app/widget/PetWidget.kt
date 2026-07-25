package com.tamapoke.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.tamapoke.app.R
import com.tamapoke.app.data.PetRepository

/**
 * Home-screen widget: shows the pet's name/level and core stats, with Feed/Pet
 * quick actions that mutate the same [PetRepository] the app reads from.
 * RemoteViews-based surfaces can't run a frame-timer animation, so this shows
 * static text/mood rather than an animated sprite (see Phase 3 in the plan).
 */
class PetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = PetRepository.get(context)
        repository.catchUp()
        val state = repository.state.value

        provideContent {
            Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                if (state == null || state.isEgg) {
                    Text("🥚 " + context.getString(R.string.orig_egg_hdr))
                } else {
                    val entry = repository.dex[state.speciesId]
                    val name = state.nickname.ifBlank { entry.name }
                    Text("$name · Lv.${state.level()}")
                    Text("${context.getString(R.string.orig_bar_food)} ${state.fullness}  ${context.getString(R.string.orig_bar_joy)} ${state.joy}  ${context.getString(R.string.orig_bar_ene)} ${state.energy}")
                    Row {
                        Text(
                            context.getString(R.string.widget_feed),
                            modifier = GlanceModifier.clickable(
                                actionRunCallback<FeedWidgetAction>(),
                            ),
                        )
                        Text(
                            "  " + context.getString(R.string.widget_pet),
                            modifier = GlanceModifier.clickable(
                                actionRunCallback<PetWidgetAction>(),
                            ),
                        )
                    }
                }
            }
        }
    }
}
