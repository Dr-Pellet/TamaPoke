package com.tamapoke.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.app.ui.device.RoundDeviceFrame
import com.tamapoke.core.PetEngine
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.DailyGoalType
import com.tamapoke.core.enums.Medal
import com.tamapoke.core.enums.PetPersonality

private val TABS = listOf("Profile", "Battle", "Medals", "Progress", "Goals", "Games")

@Composable
fun StatCardScreen(
    state: PetState?,
    dex: DexTable,
    onRename: (String) -> Unit,
    onTrain: () -> Unit,
    onWildBattle: () -> Unit = {},
    onCatchGame: () -> Unit = {},
    onMemoGame: () -> Unit = {},
    onCleanGame: () -> Unit = {},
    onTypeGame: () -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight) * 0.94f
        RoundDeviceFrame(Modifier.size(diameter)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF10131A))) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Column(Modifier.fillMaxSize().padding(top = 30.dp)) {
                        TabRow(
                            selectedTabIndex = tab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ) {
                            TABS.forEachIndexed { i, label ->
                                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                            }
                        }
                        if (state == null || state.isEgg) {
                            Text(stringResource(R.string.no_pet_yet), Modifier.padding(16.dp))
                        } else {
                            when (tab) {
                                0 -> ProfileTab(state, dex, onRename)
                                1 -> BattleTab(state, dex, onTrain, onWildBattle)
                                2 -> MedalsTab(state)
                                3 -> ProgressTab(state, dex)
                                4 -> GoalsTab(state)
                                5 -> GamesTab(state, onCatchGame, onMemoGame, onCleanGame, onTypeGame)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(state: PetState, dex: DexTable, onRename: (String) -> Unit) {
    val entry = dex[state.speciesId]
    var name by remember(state.speciesId) { mutableStateOf(state.nickname) }
    val ageDays = state.ageMinutes / 1440

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("${entry.name}${if (state.shiny) " ✨ SHINY" else ""}   AGE ${ageDays}d", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(11) },
                label = { Text(stringResource(R.string.orig_name)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF888888),
                    cursorColor = Color.White,
                ),
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
private fun BattleTab(state: PetState, dex: DexTable, onTrain: () -> Unit, onWildBattle: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(stringResource(R.string.orig_battle), style = MaterialTheme.typography.titleMedium)
        Text("${stringResource(R.string.orig_stat_atk)} ${PetEngine.atkStat(state, dex)}")
        Text("${stringResource(R.string.orig_stat_def)} ${PetEngine.defStat(state, dex)}")
        Text("${stringResource(R.string.orig_stat_spe)} ${PetEngine.speStat(state, dex)}")
        Text("${stringResource(R.string.orig_stat_wgt)} ${state.weight}")
        Text(stringResource(R.string.orig_record_fmt, state.strHi), Modifier.padding(top = 12.dp))
        Button(onClick = onTrain, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.orig_train_str)) }
        Text(
            "${stringResource(R.string.personality_title)}: ${personalityLabel(PetEngine.personality(state))}",
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "${stringResource(R.string.orig_battle)}: ${state.battleWins}W / ${state.battleLosses}L (streak ${state.battleStreak}, best ${state.bestBattleStreak})",
        )
        Button(onClick = onWildBattle, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.battle_start)) }
    }
}

@Composable
private fun personalityLabel(p: PetPersonality): String = when (p) {
    PetPersonality.BALANCED -> stringResource(R.string.personality_balanced)
    PetPersonality.PLAYFUL -> stringResource(R.string.personality_playful)
    PetPersonality.BRAVE -> stringResource(R.string.personality_brave)
    PetPersonality.CALM -> stringResource(R.string.personality_calm)
    PetPersonality.LAZY -> stringResource(R.string.personality_lazy)
}

private val ALL_MEDALS = Medal.entries

@Composable
private fun MedalsTab(state: PetState) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (earned) "🏅" else "▫️", style = MaterialTheme.typography.headlineMedium)
                    Text(medal.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun GoalsTab(state: PetState) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.daily_goals_title), style = MaterialTheme.typography.titleMedium)
        for (i in 0 until 3) {
            val typeOrdinal = state.dailyGoalType.getOrNull(i) ?: continue
            val goalType = DailyGoalType.entries.getOrNull(typeOrdinal) ?: continue
            val progress = state.dailyGoalProgress.getOrNull(i) ?: 0
            val target = PetEngine.dailyGoalTarget(goalType)
            val done = PetEngine.dailyGoalComplete(state, i)
            Text(
                "${if (done) "✅" else "▫️"} ${dailyGoalLabel(goalType)}: " +
                    stringResource(R.string.daily_goal_progress_fmt, progress, target),
            )
        }
    }
}

@Composable
private fun dailyGoalLabel(type: DailyGoalType): String = when (type) {
    DailyGoalType.CARE -> stringResource(R.string.daily_goal_care)
    DailyGoalType.PLAY -> stringResource(R.string.daily_goal_play)
    DailyGoalType.BATTLE -> stringResource(R.string.daily_goal_battle)
    DailyGoalType.CATCH -> stringResource(R.string.daily_goal_catch)
    DailyGoalType.MEMO -> stringResource(R.string.daily_goal_memo)
}

@Composable
private fun GamesTab(
    state: PetState,
    onCatchGame: () -> Unit,
    onMemoGame: () -> Unit,
    onCleanGame: () -> Unit,
    onTypeGame: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.minigame_menu_title), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onCatchGame) { Text("${stringResource(R.string.minigame_catch_title)} (best ${state.catchHi})") }
        Button(onClick = onMemoGame) { Text("${stringResource(R.string.minigame_memo_title)} (best ${state.memoHi})") }
        Button(onClick = onCleanGame) { Text("${stringResource(R.string.minigame_clean_title)} (best ${state.cleanHi})") }
        Button(onClick = onTypeGame) { Text("${stringResource(R.string.minigame_type_title)} (best ${state.typeHi})") }
    }
}

@Composable
private fun ProgressTab(state: PetState, dex: DexTable) {
    val entry = dex[state.speciesId]
    val level = state.level()
    val minutesToNextLevel = PetState.MINUTES_PER_LEVEL - (state.ageMinutes % PetState.MINUTES_PER_LEVEL)

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
