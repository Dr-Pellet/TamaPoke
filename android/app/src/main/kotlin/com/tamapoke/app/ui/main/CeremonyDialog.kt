package com.tamapoke.app.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.Ceremony
import kotlinx.coroutines.delay

private const val CEREMONY_DISPLAY_MS = 4500L

/**
 * Full-screen animated send-off, replacing what was a plain AlertDialog:
 * floating particles matching the mood of each ending (warm hearts for
 * Farewell, falling rain for Runaway, drifting sparkles for Release) - the
 * firmware plays a comparable themed scene for CEREMONY_MS (pet.h) before
 * the next egg appears; here it auto-advances after a shorter pause (a
 * "Skip" button is also offered) rather than freezing the UI for 10s.
 */
@Composable
fun CeremonyDialog(state: PetState, dex: DexTable, onDismiss: () -> Unit) {
    if (state.ceremony == Ceremony.NONE) return
    val ceremony = state.ceremony
    val entry = if (state.speciesId in 1..dex.count) dex[state.speciesId] else null
    val name = state.nickname.ifBlank { entry?.name ?: "?" }

    LaunchedEffect(ceremony) {
        delay(CEREMONY_DISPLAY_MS)
        onDismiss()
    }

    val title = when (ceremony) {
        Ceremony.FAREWELL -> stringResource(R.string.orig_farewell)
        Ceremony.RUNAWAY -> stringResource(R.string.orig_runaway)
        Ceremony.RELEASE -> stringResource(R.string.orig_goodbye)
        Ceremony.NONE -> ""
    }
    val bgColor = when (ceremony) {
        Ceremony.FAREWELL -> Color(0xFF3A2E12)
        Ceremony.RUNAWAY -> Color(0xFF10151F)
        Ceremony.RELEASE -> Color(0xFF122A2A)
        Ceremony.NONE -> Color.Black
    }
    val particle = when (ceremony) {
        Ceremony.FAREWELL -> "💛"
        Ceremony.RUNAWAY -> "💧"
        Ceremony.RELEASE -> "🕊️"
        Ceremony.NONE -> ""
    }
    val fallsDown = ceremony == Ceremony.RUNAWAY

    Box(Modifier.fillMaxSize().background(bgColor)) {
        val infinite = rememberInfiniteTransition(label = "ceremony")
        repeat(7) { i ->
            val progress by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(1600 + i * 190, easing = LinearEasing),
                ),
                label = "particle$i",
            )
            val travel = 260f * progress
            val y = if (fallsDown) travel - 130f else 130f - travel
            val x = (i * 47 - 150).toFloat()
            Text(
                particle,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = x.dp, y = y.dp),
                color = Color.White.copy(alpha = 1f - progress),
            )
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                stringResource(R.string.orig_release_fmt, name),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.orig_yes), color = Color.White)
            }
        }
    }
}
