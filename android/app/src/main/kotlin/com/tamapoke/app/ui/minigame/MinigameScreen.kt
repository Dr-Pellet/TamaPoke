package com.tamapoke.app.ui.minigame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import kotlinx.coroutines.delay

private const val STEP_MS = 85L // matches the firmware's render/step cadence while the minigame is open
private const val GAME_OVER_PAUSE_MS = 1500L

/**
 * The firmware's real minigame: juggle a Pokeball inside a circular arena
 * (gravity, wall bounce, tap-to-lift), 3 misses ends the round - ported
 * 1:1 from TamaPoke.ino's stepGame()/gameTap() via [PokeballGame]. Score
 * feeds PetEngine.playResult() exactly like pet.playResult(gameScore) did
 * on the device.
 */
@Composable
fun MinigameScreen(onFinish: (score: Int) -> Unit) {
    var state by remember { mutableStateOf(PokeballGame.newGame()) }
    var quit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (!state.gameOver && !quit) {
            delay(STEP_MS)
            state = PokeballGame.step(state)
        }
    }

    LaunchedEffect(state.gameOver) {
        if (state.gameOver) {
            delay(GAME_OVER_PAUSE_MS)
            onFinish(state.score)
        }
    }

    LaunchedEffect(quit) {
        if (quit) onFinish(state.score)
    }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.orig_score_fmt, state.score) + "   ${state.misses}/${PokeballGame.MAX_MISSES}",
            style = MaterialTheme.typography.titleMedium,
        )
        Box(Modifier.fillMaxSize().padding(top = 48.dp)) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val scale = size.width / PokeballGame.ARENA_SIZE
                            val lx = offset.x / scale
                            val ly = offset.y / scale
                            if (ly < PokeballGame.HEADER_Y) {
                                quit = true
                            } else {
                                state = PokeballGame.tap(state, lx, ly)
                            }
                        }
                    },
            ) {
                val scale = size.width / PokeballGame.ARENA_SIZE
                fun p(x: Float, y: Float) = Offset(x * scale, y * scale)

                drawCircle(
                    color = Color(0xFFE5E5E5),
                    radius = PokeballGame.RADIUS * scale,
                    center = p(PokeballGame.CX, PokeballGame.CY),
                    style = Stroke(width = 2f),
                )

                val ballRadius = 20f * scale
                val ballCenter = p(state.ball.x, state.ball.y)
                clipRect(
                    left = ballCenter.x - ballRadius, top = ballCenter.y - ballRadius,
                    right = ballCenter.x + ballRadius, bottom = ballCenter.y,
                ) {
                    drawCircle(color = Color(0xFFE5382E), radius = ballRadius, center = ballCenter)
                }
                clipRect(
                    left = ballCenter.x - ballRadius, top = ballCenter.y,
                    right = ballCenter.x + ballRadius, bottom = ballCenter.y + ballRadius,
                ) {
                    drawCircle(color = Color.White, radius = ballRadius, center = ballCenter)
                }
                drawCircle(color = Color.Black, radius = ballRadius, center = ballCenter, style = Stroke(width = 2f))
                drawLine(
                    color = Color.Black,
                    start = Offset(ballCenter.x - ballRadius, ballCenter.y),
                    end = Offset(ballCenter.x + ballRadius, ballCenter.y),
                    strokeWidth = 2f,
                )
                drawCircle(color = Color.Black, radius = ballRadius * 0.28f, center = ballCenter, style = Stroke(width = 2f))
                drawCircle(color = Color.White, radius = ballRadius * 0.16f, center = ballCenter)
            }
        }
    }
}
