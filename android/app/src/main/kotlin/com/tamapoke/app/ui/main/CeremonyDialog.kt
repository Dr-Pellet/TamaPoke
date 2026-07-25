package com.tamapoke.app.ui.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tamapoke.app.R
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.Ceremony

/**
 * Shows while ceremony != NONE (time is frozen in the engine during this).
 * The pet's life cycle ends and a fresh egg appears once catch-up/tick
 * notices the ceremony has run its course (mirrors CEREMONY_MS on the device;
 * here it simply resolves the next time the repository is touched).
 */
@Composable
fun CeremonyDialog(state: PetState, dex: DexTable, onDismiss: () -> Unit) {
    if (state.ceremony == Ceremony.NONE) return
    val entry = if (state.speciesId in 1..dex.count) dex[state.speciesId] else null
    val name = state.nickname.ifBlank { entry?.name ?: "?" }
    val title = when (state.ceremony) {
        Ceremony.FAREWELL -> stringResource(R.string.orig_farewell)
        Ceremony.RUNAWAY -> stringResource(R.string.orig_runaway)
        Ceremony.RELEASE -> stringResource(R.string.orig_goodbye)
        Ceremony.NONE -> ""
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(stringResource(R.string.orig_release_fmt, name)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.orig_yes)) } },
    )
}
