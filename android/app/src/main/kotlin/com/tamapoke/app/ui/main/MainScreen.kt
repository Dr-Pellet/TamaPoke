package com.tamapoke.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun MainScreen(viewModel: MainViewModel, onPlayClick: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val dex = viewModel.dex
    val current = state

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            current == null -> LoadingView()
            current.isEgg -> EggView(onTap = viewModel::eggTap)
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
private fun EggView(onTap: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text("🥚 " + stringResource(R.string.orig_egg_touch), style = MaterialTheme.typography.headlineSmall)
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
        Text(
            text = (if (state.nickname.isNotBlank()) state.nickname else entry.name) + (if (state.shiny) " ✨" else ""),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.orig_lvl_fmt, state.level()) + " · " + moodLabel(mood),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        RoundDeviceFrame(Modifier.fillMaxWidth(0.86f)) {
            val spriteAction = when {
                state.sleeping -> "sleep"
                mood == PetMood.EATING -> "eat"
                else -> "idle"
            }
            BiomeBackground(biome = entry.biome, modifier = Modifier.fillMaxSize().clickable(onClick = vm::caress)) {
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

                    Spacer(Modifier.weight(1f))
                    AnimatedSprite(
                        speciesId = state.speciesId,
                        action = spriteAction,
                        modifier = Modifier.size(96.dp),
                        shiny = state.shiny,
                        placeholder = { Text(entry.name.take(1), style = MaterialTheme.typography.displayLarge) },
                    )
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
