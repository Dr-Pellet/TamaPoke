package com.tamapoke.app.ui.minigame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val SESSION_MS = 8_000L

/** Tap-count challenge, matches Pet::trainStrength()'s "~4 hits = +1 STRENGTH" reward curve. */
@Composable
fun TrainingBagScreen(onFinish: (hits: Int) -> Unit) {
    var hits by remember { mutableIntStateOf(0) }
    var timeLeftMs by remember { mutableStateOf(SESSION_MS) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (timeLeftMs > 0) {
            delay(100)
            timeLeftMs = (SESSION_MS - (System.currentTimeMillis() - start)).coerceAtLeast(0)
        }
        finished = true
    }

    LaunchedEffect(finished) {
        if (finished) onFinish(hits)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$hits HITS   ${timeLeftMs / 1000}s", style = MaterialTheme.typography.titleMedium)
        Box(Modifier.fillMaxSize().padding(top = 24.dp)) {
            Button(
                onClick = { hits++ },
                modifier = Modifier.fillMaxSize().padding(24.dp).height(200.dp),
            ) {
                Text("HIT FAST!", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
