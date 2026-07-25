package com.tamapoke.app.ui.minigame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Simplified stand-in for the firmware's physics-based "dodge the obstacles"
 * minigame: tap the ball before it vanishes. Score maps 1:1 into
 * PetEngine.playResult(score) - same reward curve, different mechanic; a
 * faithful pixel-perfect port of the original's flappy-bird-style physics
 * is deferred (see project plan, Phase 3 notes).
 */
private const val SESSION_MS = 15_000L

@Composable
fun MinigameScreen(onFinish: (score: Int) -> Unit) {
    var score by remember { mutableIntStateOf(0) }
    var timeLeftMs by remember { mutableStateOf(SESSION_MS) }
    var targetVisible by remember { mutableStateOf(false) }
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var targetY by remember { mutableFloatStateOf(0.5f) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            timeLeftMs = (SESSION_MS - elapsed).coerceAtLeast(0)
            if (timeLeftMs <= 0) break

            targetX = 0.15f + Random.nextFloat() * 0.7f
            targetY = 0.15f + Random.nextFloat() * 0.6f
            targetVisible = true
            val visibleMs = (900L - score * 20).coerceAtLeast(350L)
            delay(visibleMs)
            targetVisible = false
            delay(150)
        }
        finished = true
    }

    LaunchedEffect(finished) {
        if (finished) onFinish(score)
    }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SCORE: $score   ${timeLeftMs / 1000}s", style = MaterialTheme.typography.titleMedium)
        if (targetVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .align(Alignment.TopStart)
                        .padding(
                            start = (targetX * 260).dp,
                            top = (targetY * 400).dp,
                        )
                        .background(Color(0xFFEA503A), CircleShape)
                        .clickable {
                            score++
                            targetVisible = false
                        },
                )
            }
        }
    }
}
