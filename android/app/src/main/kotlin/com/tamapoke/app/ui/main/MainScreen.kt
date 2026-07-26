package com.tamapoke.app.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.app.sprite.AnimatedSprite
import com.tamapoke.app.ui.device.ArcButton
import com.tamapoke.app.ui.device.ArcButtonBar
import com.tamapoke.app.ui.device.BiomeBackground
import com.tamapoke.app.ui.device.RoundDeviceFrame
import com.tamapoke.core.PetEngine
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.PetMood
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RELEASE_HOLD_MS = 3000L

@Composable
fun MainScreen(viewModel: MainViewModel, onPlayClick: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val dex = viewModel.dex
    val current = state

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            current == null -> LoadingView()
            current.isEgg -> EggView(current, onTap = viewModel::eggTap)
            else -> PetView(current, dex, viewModel, onPlayClick)
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.loading))
    }
}

@Composable
private fun EggView(state: PetState, onTap: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🥚 " + stringResource(R.string.orig_egg_touch), style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Text(if (i < state.eggTaps) "●" else "○", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
private fun PetView(state: PetState, dex: DexTable, vm: MainViewModel, onPlayClick: () -> Unit) {
    val entry = dex[state.speciesId]
    val mood = PetEngine.mood(state, eating = false)

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (if (state.nickname.isNotBlank()) state.nickname else entry.name) + (if (state.shiny) " ✨" else ""),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            if (state.streak > 0) {
                Text(
                    "🔥${state.streak}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Text(
            stringResource(R.string.orig_lvl_fmt, state.level()) + " · " + moodLabel(mood),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        RoundDeviceFrame(Modifier.fillMaxWidth(0.86f)) {
            // Wanders left/right when awake, matching the original's idle animation cadence.
            var wanderAction by remember { mutableStateOf("idle") }
            LaunchedEffect(state.sleeping, mood) {
                if (state.sleeping || mood == PetMood.EATING) return@LaunchedEffect
                while (true) {
                    wanderAction = "idle"
                    delay(3500)
                    wanderAction = "walk_l"
                    delay(1400)
                    wanderAction = "idle"
                    delay(3000)
                    wanderAction = "walk_r"
                    delay(1400)
                }
            }
            val spriteAction = when {
                state.sleeping -> "sleep"
                mood == PetMood.EATING -> "eat"
                else -> wanderAction
            }

            var holdProgress by remember { mutableFloatStateOf(0f) }
            var holdJob by remember { mutableStateOf<Job?>(null) }
            val scope = rememberCoroutineScope()

            BiomeBackground(
                biome = entry.biome,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { vm.caress() },
                            onPress = {
                                holdProgress = 0f
                                holdJob = scope.launch {
                                    val start = System.currentTimeMillis()
                                    while (true) {
                                        val elapsed = System.currentTimeMillis() - start
                                        holdProgress = (elapsed / RELEASE_HOLD_MS.toFloat()).coerceIn(0f, 1f)
                                        if (elapsed >= RELEASE_HOLD_MS) {
                                            vm.release()
                                            break
                                        }
                                        delay(16)
                                    }
                                }
                                tryAwaitRelease()
                                holdJob?.cancel()
                                holdProgress = 0f
                            },
                        )
                    },
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(36.dp))
                    Box(
                        Modifier
                            .padding(horizontal = 40.dp)
                            .background(Color(0x991A1D24), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Column {
                            StatBar(stringResource(R.string.orig_bar_food), state.fullness)
                            StatBar(stringResource(R.string.orig_bar_joy), state.joy)
                            StatBar(stringResource(R.string.orig_bar_ene), state.energy)
                            StatBar(stringResource(R.string.orig_bar_hyg), state.hygiene)
                        }
                    }

                    if (state.poops > 0) {
                        Row(Modifier.padding(top = 6.dp)) {
                            repeat(state.poops) { Text("💩", modifier = Modifier.padding(horizontal = 2.dp)) }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedSprite(
                            speciesId = state.speciesId,
                            action = spriteAction,
                            modifier = Modifier.size(96.dp),
                            shiny = state.shiny,
                            placeholder = { Text(entry.name.take(1), style = MaterialTheme.typography.displayLarge) },
                        )
                        HeartBurst(vm)
                        if (holdProgress > 0f) {
                            CircularProgressIndicator(
                                progress = { holdProgress },
                                modifier = Modifier.size(112.dp),
                                color = Color(0xFFFF6E6E),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))

                    ArcButtonBar(
                        buttons = listOf(
                            ArcButton("🍎", stringResource(R.string.action_feed), vm::feed),
                            ArcButton("⚽", stringResource(R.string.action_play), onPlayClick),
                            ArcButton(
                                "🌙",
                                stringResource(if (state.sleeping) R.string.action_wake else R.string.action_sleep),
                                vm::toggleLight,
                            ),
                            ArcButton("🫧", stringResource(R.string.action_bath), vm::clean),
                        ),
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }

        MedalBanner(vm)
        StreakMilestoneBanner(vm)

        val displayName = state.nickname.ifBlank { entry.name }

        // Priority mirrors the original: evolve > runaway (sad, forced) > farewell (happy, chosen).
        when {
            PetEngine.wantEvolveButton(state, dex) -> {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::evolve) { Text(stringResource(R.string.orig_evo_tap)) }
                    Text(
                        stringResource(R.string.orig_evo_keep),
                        modifier = Modifier
                            .clickable(onClick = vm::declineEvolve)
                            .padding(top = 12.dp),
                    )
                }
            }
            PetEngine.canRunawayNow(state) -> {
                Spacer(Modifier.height(12.dp))
                Button(onClick = vm::startRunaway) { Text(stringResource(R.string.orig_runaway_btn, displayName)) }
            }
            PetEngine.wantFarewellButton(state, dex) -> {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.orig_farewell_btn, displayName))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::startFarewell) { Text(stringResource(R.string.orig_far_go)) }
                    Text(
                        stringResource(R.string.orig_far_stay),
                        modifier = Modifier
                            .clickable(onClick = vm::declineFarewell)
                            .padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

/** Brief heart pop, replayed each time [MainViewModel.heartBurstTrigger] is bumped (on every caress()). */
@Composable
private fun HeartBurst(vm: MainViewModel) {
    val trigger by vm.heartBurstTrigger.collectAsState()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        visible = true
        delay(700)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(300)),
    ) {
        Text("💗", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(bottom = 90.dp))
    }
}

@Composable
private fun MedalBanner(vm: MainViewModel) {
    val medals by vm.medalBanner.collectAsState()
    val current = medals ?: return
    LaunchedEffect(current) {
        delay(3000)
        vm.clearMedalBanner()
    }
    Box(
        Modifier
            .padding(top = 8.dp)
            .background(Color(0xFF2A2410), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text("🏅 " + stringResource(R.string.orig_medal_banner) + " " + current.joinToString(", ") { it.name })
    }
}

@Composable
private fun StreakMilestoneBanner(vm: MainViewModel) {
    val milestone by vm.streakMilestone.collectAsState()
    val days = milestone ?: return
    LaunchedEffect(days) {
        delay(3000)
        vm.clearStreakMilestone()
    }
    Box(
        Modifier
            .padding(top = 8.dp)
            .background(Color(0xFF2A1810), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text("🔥 " + stringResource(R.string.orig_streak_days_fmt, days))
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label: $value", style = MaterialTheme.typography.labelSmall, color = Color.White)
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
    }
}

@Composable
private fun moodLabel(mood: PetMood): String = when (mood) {
    PetMood.HAPPY -> stringResource(R.string.orig_happy)
    PetMood.SAD -> stringResource(R.string.orig_sad)
    PetMood.EATING -> stringResource(R.string.orig_eating)
    PetMood.SLEEPING -> stringResource(R.string.mood_sleeping) // no direct equivalent in the original's status strings
}
