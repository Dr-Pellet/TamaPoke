package com.tamapoke.app.ui.minigame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.core.battle.BattleEngine
import com.tamapoke.core.enums.BattleType
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val ROUNDS = 10
private const val ROUND_MS = 4000L

/**
 * Pick the type that's super-effective against the shown wild type. Ported
 * concept from the expanded fork's Type-match minigame; trains STRENGTH.
 */
@Composable
fun TypeMinigameScreen(onFinish: (score: Int) -> Unit) {
    val allTypes = remember { BattleType.entries.toList() }
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf(allTypes.random()) }
    var choices by remember { mutableStateOf(pickChoices(allTypes, target)) }
    var timeLeftMs by remember { mutableStateOf(ROUND_MS) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(round) {
        val start = System.currentTimeMillis()
        while (timeLeftMs > 0) {
            delay(100)
            timeLeftMs = (ROUND_MS - (System.currentTimeMillis() - start)).coerceAtLeast(0)
        }
        if (round < ROUNDS - 1) {
            round++
            target = allTypes.random()
            choices = pickChoices(allTypes, target)
            timeLeftMs = ROUND_MS
        } else {
            finished = true
        }
    }
    LaunchedEffect(finished) { if (finished) onFinish(score) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.orig_score_fmt, score) + "   ${round + 1}/$ROUNDS",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(target.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.padding(top = 16.dp)) {
            choices.forEach { choice ->
                Button(
                    onClick = {
                        if (BattleEngine.typeEffectPct(choice, target, null) > 100) score++
                        if (round < ROUNDS - 1) {
                            round++
                            target = allTypes.random()
                            choices = pickChoices(allTypes, target)
                            timeLeftMs = ROUND_MS
                        } else {
                            finished = true
                        }
                    },
                    modifier = Modifier.padding(end = 6.dp),
                ) { Text(choice.name) }
            }
        }
    }
}

private fun pickChoices(all: List<BattleType>, target: BattleType): List<BattleType> {
    val correct = all.filter { BattleEngine.typeEffectPct(it, target, null) > 100 }.randomOrNull() ?: all.random()
    val others = (all - correct).shuffled().take(2)
    return (listOf(correct) + others).shuffled()
}
