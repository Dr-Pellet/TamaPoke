package com.tamapoke.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.core.PetEngine
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.Medal

private val TABS = listOf("Profile", "Battle", "Medals", "Progress")

@Composable
fun StatCardScreen(
    state: PetState?,
    dex: DexTable,
    onRename: (String) -> Unit,
    onTrain: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            TABS.forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
            }
        }
        if (state == null || state.isEgg) {
            Text(stringResource(R.string.no_pet_yet), Modifier.padding(16.dp))
            return
        }
        when (tab) {
            0 -> ProfileTab(state, dex, onRename)
            1 -> BattleTab(state, dex, onTrain)
            2 -> MedalsTab(state)
            3 -> ProgressTab(state, dex)
        }
    }
}

@Composable
private fun ProfileTab(state: PetState, dex: DexTable, onRename: (String) -> Unit) {
    val entry = dex[state.speciesId]
    var name by remember(state.speciesId) { mutableStateOf(state.nickname) }
    val ageDays = state.ageMinutes / 1440

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("${entry.name}${if (state.shiny) " ✨ SHINY" else ""}   AGE ${ageDays}d", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(top = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(11) },
                label = { Text(stringResource(R.string.orig_name)) },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onRename(name) }, modifier = Modifier.padding(start = 8.dp)) { Text("Save") }
        }
        Text(stringResource(R.string.orig_streak_fmt, state.streak, state.bestStreak), Modifier.padding(top = 16.dp))
        Text("${stringResource(R.string.orig_vin)} ${state.bond}/100")
        Text(
            if (state.berryKnown) "🍓" else stringResource(R.string.orig_berry_unk),
        )
    }
}

@Composable
private fun BattleTab(state: PetState, dex: DexTable, onTrain: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.orig_battle), style = MaterialTheme.typography.titleMedium)
        Text("${stringResource(R.string.orig_stat_atk)} ${PetEngine.atkStat(state, dex)}")
        Text("${stringResource(R.string.orig_stat_def)} ${PetEngine.defStat(state, dex)}")
        Text("${stringResource(R.string.orig_stat_spe)} ${PetEngine.speStat(state, dex)}")
        Text("${stringResource(R.string.orig_stat_wgt)} ${state.weight}")
        Text(stringResource(R.string.orig_record_fmt, state.strHi), Modifier.padding(top = 12.dp))
        Button(onClick = onTrain, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.orig_train_str)) }
    }
}

private val ALL_MEDALS = Medal.entries

@Composable
private fun MedalsTab(state: PetState) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.orig_medals_fmt, state.medals.size, ALL_MEDALS.size),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(R.string.medals_total_fmt, state.totalMedals))
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.padding(top = 12.dp)) {
            items(ALL_MEDALS) { medal ->
                val earned = medal in state.medals
                Column(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Text(if (earned) "🏅" else "▫️", style = MaterialTheme.typography.headlineMedium)
                    Text(medal.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ProgressTab(state: PetState, dex: DexTable) {
    val entry = dex[state.speciesId]
    val level = state.level()
    val minutesToNextLevel = PetState.MINUTES_PER_LEVEL - (state.ageMinutes % PetState.MINUTES_PER_LEVEL)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.orig_progress), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.orig_lvl_fmt, level))
        Text(stringResource(R.string.orig_next_lvl_fmt, minutesToNextLevel, level + 1))
        if (entry.evolvesTo == 0) {
            Text(stringResource(R.string.orig_final_form))
        } else when {
            PetEngine.canEvolveNow(state, dex) -> Text(stringResource(R.string.orig_evo_ready))
            state.lowestStat() < 40 -> Text(stringResource(R.string.orig_evo_blocked))
            else -> {
                val evolveLevel = entry.evolveLevel + state.careMistakes
                Text(stringResource(R.string.orig_evo_in_fmt, (evolveLevel - level).coerceAtLeast(0)))
            }
        }
        Text(stringResource(R.string.orig_mistakes_fmt, state.careMistakes))
    }
}
