package com.tamapoke.app.ui.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable

@Composable
fun PokedexScreen(state: PetState?, dex: DexTable) {
    val registeredCount = state?.registeredCount() ?: 0

    Box(Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            "POKEDEX $registeredCount/${dex.count}",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 40.dp, start = 4.dp, end = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items((1..dex.count).toList()) { id ->
                val entry = dex[id]
                val registered = state?.isRegistered(id) == true
                val shiny = state?.isShinyRegistered(id) == true
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .background(
                            if (registered) Color(android.graphics.Color.parseColor(entry.accent)) else Color(0xFFDDDDDD),
                            RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (registered) "#$id${if (shiny) " ✨" else ""}\n${entry.name}" else "#$id\n???",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (registered) Color.White else Color(0xFF888888),
                    )
                }
            }
        }
    }
}
