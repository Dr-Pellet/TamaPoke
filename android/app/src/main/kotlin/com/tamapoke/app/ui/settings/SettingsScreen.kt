package com.tamapoke.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Codes match the firmware's 6 supported languages (i18n.cpp); English is the shared default. */
private val LANGUAGES = listOf(
    "en" to "English",
    "es" to "Espanol",
    "fr" to "Francais",
    "de" to "Deutsch",
    "it" to "Italiano",
    "pt" to "Portugues",
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val soundOn by viewModel.soundOn.collectAsState(initial = true)
    val language by viewModel.language.collectAsState(initial = "en")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sound", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 12.dp))
            Switch(checked = soundOn, onCheckedChange = viewModel::setSoundOn)
        }

        Text("Language", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        Column {
            LANGUAGES.chunked(3).forEach { row ->
                Row {
                    row.forEach { (code, label) ->
                        FilterChip(
                            selected = language == code,
                            onClick = { viewModel.setLanguage(code) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
