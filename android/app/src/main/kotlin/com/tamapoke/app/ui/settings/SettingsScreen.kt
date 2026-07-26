package com.tamapoke.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.app.audio.SoundMode
import com.tamapoke.app.ui.device.RoundDeviceFrame
import kotlinx.coroutines.launch

/** Codes match the firmware's 6 supported languages (i18n.cpp); English is the shared default. */
private val LANGUAGES = listOf(
    "en" to "English",
    "es" to "Espanol",
    "fr" to "Francais",
    "de" to "Deutsch",
    "it" to "Italiano",
    "pt" to "Portugues",
)

private val SOUND_MODES = listOf(
    SoundMode.OFF to R.string.sound_off,
    SoundMode.LOW to R.string.sound_low,
    SoundMode.MED to R.string.sound_med,
    SoundMode.FULL to R.string.sound_full,
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onExportSave: ((String) -> Unit) -> Unit,
    onImportSave: (String, () -> Unit) -> Unit,
) {
    val soundMode by viewModel.soundMode.collectAsState(initial = SoundMode.FULL)
    val language by viewModel.language.collectAsState(initial = "en")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        onExportSave { json ->
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.onSuccess {
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read file")
                onImportSave(json) {
                    Toast.makeText(context, "Loaded", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Import failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight) * 0.94f
        RoundDeviceFrame(Modifier.size(diameter)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF10131A))) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Column(Modifier.fillMaxSize().padding(28.dp)) {
                        Text(
                            stringResource(R.string.settings_sound),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Row {
                            SOUND_MODES.forEach { (mode, labelRes) ->
                                FilterChip(
                                    selected = soundMode == mode,
                                    onClick = { viewModel.setSoundMode(mode) },
                                    label = { Text(stringResource(labelRes)) },
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }

                        Text(
                            stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                        )
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

                        Text(
                            stringResource(R.string.settings_save_data),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                        )
                        Row {
                            Button(
                                onClick = { exportLauncher.launch("tamapoke_save.json") },
                                modifier = Modifier.padding(end = 8.dp),
                            ) { Text(stringResource(R.string.settings_export)) }
                            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                                Text(stringResource(R.string.settings_import))
                            }
                        }
                    }
                }
            }
        }
    }
}
