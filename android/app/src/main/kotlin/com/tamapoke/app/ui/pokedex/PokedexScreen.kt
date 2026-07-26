package com.tamapoke.app.ui.pokedex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.app.sprite.SpriteLoader
import com.tamapoke.app.ui.device.RoundDeviceFrame
import com.tamapoke.core.PetState
import com.tamapoke.core.dex.DexTable

@Composable
fun PokedexScreen(state: PetState?, dex: DexTable) {
    val registeredCount = state?.registeredCount() ?: 0

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight) * 0.94f
        RoundDeviceFrame(Modifier.size(diameter)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF10131A)).padding(20.dp)) {
                Text(
                    stringResource(R.string.orig_pokedex_fmt, registeredCount),
                    modifier = Modifier.padding(top = 22.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 44.dp, start = 4.dp, end = 4.dp, bottom = 4.dp),
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
                                    if (registered) Color(android.graphics.Color.parseColor(entry.accent)) else Color(0xFF2A2E38),
                                    RoundedCornerShape(8.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (registered) {
                                StaticSpriteThumb(id, shiny = shiny, modifier = Modifier.size(36.dp))
                            }
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
    }
}

/** Single static idle frame (no animation timer) - cheap enough for a 151-cell grid. */
@Composable
private fun StaticSpriteThumb(speciesId: Int, shiny: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val frames = remember(speciesId, shiny) { SpriteLoader.load(context, speciesId, "idle", shiny) } ?: return
    Canvas(modifier) {
        drawImage(
            image = frames.bitmap,
            srcOffset = IntOffset(0, frames.row * frames.frameHeight),
            srcSize = IntSize(frames.frameWidth, frames.frameHeight),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.None,
        )
    }
}
