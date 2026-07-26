package com.tamapoke.app.ui.minigame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import kotlinx.coroutines.delay
import kotlin.random.Random

private val COLORS = listOf(Color(0xFFE5382E), Color(0xFF4C98E5), Color(0xFFF2C744), Color(0xFF6EDC7A))
private const val FLASH_MS = 500L
private const val GAP_MS = 200L

/** Simon-says: repeat an ever-growing color sequence. Ported concept from the expanded fork's Memo minigame; trains DEFENSE. */
@Composable
fun MemoMinigameScreen(onFinish: (rounds: Int) -> Unit) {
    var sequence by remember { mutableStateOf(listOf(Random.nextInt(4))) }
    var playerIndex by remember { mutableIntStateOf(0) }
    var showingIndex by remember { mutableIntStateOf(-1) }
    var showingSequence by remember { mutableStateOf(true) }
    var rounds by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(sequence) {
        showingSequence = true
        playerIndex = 0
        for (i in sequence.indices) {
            showingIndex = i
            delay(FLASH_MS)
            showingIndex = -1
            delay(GAP_MS)
        }
        showingSequence = false
    }
    LaunchedEffect(finished) { if (finished) onFinish(rounds) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.orig_score_fmt, rounds),
            style = MaterialTheme.typography.titleMedium,
        )
        Column(Modifier.fillMaxSize().padding(top = 20.dp)) {
            for (row in 0..1) {
                Row(Modifier.fillMaxWidth().height(120.dp)) {
                    for (col in 0..1) {
                        val idx = row * 2 + col
                        val active = showingIndex == idx
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) COLORS[idx].copy(alpha = 1f) else COLORS[idx].copy(alpha = 0.45f))
                                .clickable(enabled = !showingSequence && !finished) {
                                    if (sequence[playerIndex] == idx) {
                                        if (playerIndex == sequence.lastIndex) {
                                            rounds = sequence.size
                                            sequence = sequence + Random.nextInt(4)
                                        } else {
                                            playerIndex++
                                        }
                                    } else {
                                        finished = true
                                    }
                                },
                        ) {}
                    }
                }
            }
        }
    }
}
