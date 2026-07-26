package com.tamapoke.app.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate as drawRotate
import androidx.compose.ui.unit.dp
import com.tamapoke.app.sprite.AnimatedSprite
import kotlinx.coroutines.delay

/** Fired once when a species change is detected between consecutive states (see MainViewModel). */
data class EvolutionEvent(val fromSpeciesId: Int, val toSpeciesId: Int, val shiny: Boolean)

const val EVOLUTION_ANIM_MS = 5200L // matches EVOLVE_ANIM_MS in pet.h

/**
 * Halo + rotating rays + flicker between the old and new form, echoing the
 * firmware's evolution animation (evolveT()/EVOLVE_ANIM_MS in pet.h /
 * TamaPoke.ino's evolution draw code) without trying to match its exact
 * pixel choreography.
 */
@Composable
fun EvolutionOverlay(event: EvolutionEvent, onDone: () -> Unit) {
    var showTo by remember(event) { mutableStateOf(false) }

    LaunchedEffect(event) {
        val flickerStart = System.currentTimeMillis()
        while (System.currentTimeMillis() - flickerStart < EVOLUTION_ANIM_MS - 800) {
            delay(160)
            showTo = !showTo
        }
        showTo = true
        delay(800)
        onDone()
    }

    val infinite = rememberInfiniteTransition(label = "evolution")
    val haloScale by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "haloScale",
    )
    val rayRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "rayRotation",
    )
    val sparkleAlpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "sparkleAlpha",
    )

    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(260.dp).rotate(rayRotation)) {
            repeat(8) { i ->
                drawRotate(i * 45f) {
                    drawLine(
                        color = Color(0x66FFEE99),
                        start = Offset(size.width / 2, size.height / 2),
                        end = Offset(size.width / 2, 0f),
                        strokeWidth = 6f,
                    )
                }
            }
        }
        Box(
            Modifier
                .size(170.dp)
                .scale(haloScale)
                .background(
                    Brush.radialGradient(listOf(Color(0x99FFF6C7), Color.Transparent)),
                    CircleShape,
                ),
        )
        AnimatedSprite(
            speciesId = if (showTo) event.toSpeciesId else event.fromSpeciesId,
            action = "idle",
            shiny = event.shiny,
            modifier = Modifier.size(128.dp),
        )
        listOf(
            Offset(-80f, -70f), Offset(90f, -50f), Offset(-60f, 80f), Offset(75f, 90f),
        ).forEach { off ->
            Text(
                "✨",
                modifier = Modifier.offset(x = off.x.dp, y = off.y.dp),
                color = Color.White.copy(alpha = sparkleAlpha),
            )
        }
    }
}
