package com.tamapoke.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.core.dex.DexTable

/** Mirrors the firmware's hardcoded STARTER_DEX = {1, 4, 7}: Bulbasaur, Charmander, Squirtle. */
private val STARTER_DEX = listOf(1, 4, 7)

@Composable
fun StarterPickerScreen(dex: DexTable, onChoose: (Int) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.orig_choose_starter), style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                STARTER_DEX.forEach { id ->
                    if (dex.contains(id)) {
                        val entry = dex[id]
                        Button(onClick = { onChoose(id) }) { Text(entry.name) }
                    }
                }
            }
        }
    }
}
