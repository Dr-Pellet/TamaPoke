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
import androidx.compose.runtime.key
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
private const val TARGET_LIFETIME_MS = 1100L

/** Tap the target before it vanishes. Ported concept from the expanded fork's Catch minigame; trains SPEED. */
@Composable
fun CatchMinigameScreen(onFinish: (score: Int) -> Unit) {
    var score by remember { mutableIntStateOf(0) }
    var timeLeftMs by remember { mutableStateOf(SESSION_MS) }
    var targetX by remember { mutableStateOf(0.5f) }
    var targetY by remember { mutableStateOf(0.5f) }
    var targetKey by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (timeLeftMs > 0) {
            targetX = Random.nextFloat() * 0.8f + 0.05f
            targetY = Random.nextFloat() * 0.7f + 0.1f
            targetKey++
            delay(TARGET_LIFETIME_MS)
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
            key(targetKey) {
                Box(
                    Modifier
                        .offset(x = arenaSize * targetX, y = arenaSize * targetY)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5382E))
                        .clickable { score++ },
                )
            }
        }
    }
}
