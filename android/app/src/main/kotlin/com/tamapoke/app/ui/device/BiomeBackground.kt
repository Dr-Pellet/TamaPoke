package com.tamapoke.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.time.LocalTime

/**
 * Sky/ground colors ported 1:1 from TamaPoke.ino's drawScene()/BIOME_SOIL -
 * same hex values, just a smooth gradient instead of the firmware's banded
 * fillRect loop (the device has no GPU, Android does). Biome ids match
 * dex.json's `biome` field: 0 meadow, 1 beach, 2 forest, 3 volcano,
 * 4 mountain, 5 snow.
 */
private val NIGHT_TOP = Color(0xFF0C1224)
private val NIGHT_BOTTOM = Color(0xFF1E2646)
private val DAWN_TOP = Color(0xFFD16A86)
private val DAWN_BOTTOM = Color(0xFFF3B87C)
private val DAY_TOP = Color(0xFF8FC8EA)
private val DAY_BOTTOM = Color(0xFFDCEEE6)
private val DUSK_TOP = Color(0xFFC75A4A)
private val DUSK_BOTTOM = Color(0xFFF0AE64)
private val NIGHT_SOIL_TINT = Color(0xFF161C30)

private val BIOME_SOIL = listOf(
    Color(0xFF7EC07F), // 0 meadow
    Color(0xFFDCCA94), // 1 beach
    Color(0xFF4F8A55), // 2 forest
    Color(0xFF8A5544), // 3 volcano
    Color(0xFFA8906A), // 4 mountain
    Color(0xFFE6EEF5), // 5 snow
)

private fun isNight(hour: Int) = hour < 6 || hour >= 21

private fun skyColors(hour: Int): Pair<Color, Color> = when {
    isNight(hour) -> NIGHT_TOP to NIGHT_BOTTOM
    hour < 8 -> DAWN_TOP to DAWN_BOTTOM
    hour < 18 -> DAY_TOP to DAY_BOTTOM
    else -> DUSK_TOP to DUSK_BOTTOM
}

private fun soilColor(biome: Int, hour: Int): Color {
    val base = BIOME_SOIL.getOrElse(biome) { BIOME_SOIL[0] }
    return if (isNight(hour)) lerp(base, NIGHT_SOIL_TINT, 9f / 16f) else base
}

@Composable
fun BiomeBackground(biome: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val hour = remember { LocalTime.now().hour }
    val (top, bottom) = skyColors(hour)
    val soil = soilColor(biome, hour)

    Box(modifier.background(Brush.verticalGradient(listOf(top, bottom)))) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.32f)
                .align(Alignment.BottomCenter)
                .background(soil),
        )
        content()
    }
}
