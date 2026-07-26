package com.tamapoke.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tamapoke.app.R
import com.tamapoke.app.ui.main.CeremonyDialog
import com.tamapoke.app.ui.main.EvolutionOverlay
import com.tamapoke.app.ui.main.MainScreen
import com.tamapoke.app.ui.main.MainViewModel
import com.tamapoke.app.ui.main.StarterPickerScreen
import com.tamapoke.app.ui.minigame.MinigameScreen
import com.tamapoke.app.ui.minigame.TrainingBagScreen
import com.tamapoke.app.ui.pokedex.PokedexScreen
import com.tamapoke.app.ui.settings.SettingsScreen
import com.tamapoke.app.ui.settings.SettingsViewModel
import com.tamapoke.app.ui.stats.StatCardScreen
import com.tamapoke.core.enums.Ceremony

private enum class Overlay { NONE, MINIGAME, TRAINING }

@Composable
fun AppRoot(mainViewModel: MainViewModel, settingsViewModel: SettingsViewModel) {
    val state by mainViewModel.state.collectAsState()
    val current = state

    // First game only: starter picker takes over the whole screen, like the
    // firmware's "eleccion de inicial (prioridad total)".
    if (current != null && current.starterPick) {
        StarterPickerScreen(mainViewModel.dex, onChoose = mainViewModel::chooseStarter)
        return
    }

    var screen by remember { mutableStateOf(Screen.MAIN) }
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }

    val strengthGain by mainViewModel.lastStrengthGain.collectAsState()
    val strengthGainText = stringResource(R.string.orig_str_gain_fmt, strengthGain ?: 0)
    LaunchedEffect(strengthGain) {
        if (strengthGain != null) {
            snackbarHostState.showSnackbar(strengthGainText)
            mainViewModel.clearStrengthGainMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    Screen.entries.forEach { s ->
                        NavigationBarItem(
                            selected = screen == s,
                            onClick = { screen = s },
                            icon = { Text(s.emoji) },
                            label = { Text(s.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.MAIN -> MainScreen(mainViewModel, onPlayClick = { overlay = Overlay.MINIGAME })
                    Screen.POKEDEX -> PokedexScreen(current, mainViewModel.dex)
                    Screen.STATS -> StatCardScreen(
                        current,
                        mainViewModel.dex,
                        onRename = mainViewModel::rename,
                        onTrain = { overlay = Overlay.TRAINING },
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        settingsViewModel,
                        onExportSave = mainViewModel::exportSave,
                        onImportSave = mainViewModel::importSave,
                    )
                }
            }
        }

        if (overlay == Overlay.MINIGAME) {
            Surface(Modifier.fillMaxSize()) {
                MinigameScreen(onFinish = { score ->
                    mainViewModel.playResult(score)
                    overlay = Overlay.NONE
                })
            }
        }
        if (overlay == Overlay.TRAINING) {
            Surface(Modifier.fillMaxSize()) {
                TrainingBagScreen(onFinish = { hits ->
                    mainViewModel.trainStrength(hits)
                    overlay = Overlay.NONE
                })
            }
        }

        if (current != null && current.ceremony != Ceremony.NONE) {
            CeremonyDialog(current, mainViewModel.dex, onDismiss = mainViewModel::resolveCeremony)
        }

        val evolutionEvent by mainViewModel.evolutionEvent.collectAsState()
        evolutionEvent?.let { event ->
            EvolutionOverlay(event, onDone = mainViewModel::clearEvolutionEvent)
        }
    }
}
