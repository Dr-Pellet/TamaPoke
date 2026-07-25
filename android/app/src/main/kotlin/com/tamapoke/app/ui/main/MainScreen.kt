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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamapoke.core.PetEngine
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.PetMood

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val dex = viewModel.dex
    val current = state

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            current == null -> LoadingView()
            current.isEgg -> EggView(onTap = viewModel::eggTap)
            else -> PetView(current, dex, viewModel)
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("...")
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
        Text("🥚 Tap the egg", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun PetView(state: PetState, dex: DexTable, vm: MainViewModel) {
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
        Text("Lv.${state.level()} · ${moodLabel(mood)}", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(androidx.compose.ui.graphics.Color(0xFFEFEFEF), RoundedCornerShape(24.dp))
                .clickable(onClick = vm::caress),
            contentAlignment = Alignment.Center,
        ) {
            Text(entry.name.take(1), style = MaterialTheme.typography.displayLarge)
        }

        Spacer(Modifier.height(16.dp))
        StatBar("Food", state.fullness)
        StatBar("Joy", state.joy)
        StatBar("Energy", state.energy)
        StatBar("Hygiene", state.hygiene)

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::feed) { Text("Feed") }
            Button(onClick = vm::play) { Text("Play") }
            Button(onClick = vm::clean) { Text("Bath") }
            Button(onClick = vm::toggleLight) { Text(if (state.sleeping) "Wake" else "Sleep") }
        }

        if (PetEngine.canEvolveNow(state, dex)) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = vm::evolve) { Text("Evolve!") }
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: $value", style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
    }
}

private fun moodLabel(mood: PetMood): String = when (mood) {
    PetMood.HAPPY -> "Happy"
    PetMood.SAD -> "Sad"
    PetMood.EATING -> "Eating"
    PetMood.SLEEPING -> "Sleeping"
}
