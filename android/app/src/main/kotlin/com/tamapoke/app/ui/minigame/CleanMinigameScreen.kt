package com.tamapoke.app.ui.minigame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val SESSION_MS = 15_000L
private const val SPAWN_COUNT = 6

private data class Spot(val id: Int, val x: Float, val y: Float, val cleaned: Boolean = false)

/** Tap dirt spots to clean them before time runs out. Ported concept from the expanded fork's Clean minigame. */
@Composable
fun CleanMinigameScreen(onFinish: (score: Int) -> Unit) {
    var spots by remember {
        mutableStateOf((0 until SPAWN_COUNT).map { Spot(it, Random.nextFloat() * 0.85f, Random.nextFloat() * 0.8f) })
    }
    var timeLeftMs by remember { mutableStateOf(SESSION_MS) }
    var finished by remember { mutableStateOf(false) }
    val score = spots.count { it.cleaned }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (timeLeftMs > 0 && spots.any { !it.cleaned }) {
            delay(100)
            timeLeftMs = (SESSION_MS - (System.currentTimeMillis() - start)).coerceAtLeast(0)
        }
        finished = true
    }
    LaunchedEffect(finished) { if (finished) onFinish(score) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.orig_score_fmt, score) + "   ${timeLeftMs / 1000}s",
            style = MaterialTheme.typography.titleMedium,
        )
        Box(Modifier.fillMaxSize().padding(top = 16.dp).fillMaxWidth().aspectRatio(1f)) {
            val arenaSize: Dp = 260.dp
            for (spot in spots) {
                if (spot.cleaned) continue
                Box(
                    Modifier
                        .offset(x = arenaSize * spot.x, y = arenaSize * spot.y)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6B4A2A))
                        .clickable { spots = spots.map { if (it.id == spot.id) it.copy(cleaned = true) else it } },
                )
            }
        }
    }
}
