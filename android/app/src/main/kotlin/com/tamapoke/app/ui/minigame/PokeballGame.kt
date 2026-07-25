package com.tamapoke.app.ui.minigame

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Faithful port of the firmware's minigame physics (TamaPoke.ino's
 * startGame()/respawnBall()/gameTap()/stepGame(), lines ~944-1023): juggle a
 * Pokeball inside a circular arena by tapping it before it hits the floor,
 * 3 misses ends the round. Coordinates are in the firmware's original
 * 466x466 logical space (CX/CY=233, arena radius 205) - the UI scales this
 * to the actual canvas size.
 */
object PokeballGame {
    const val ARENA_SIZE = 466f
    const val CX = 233f
    const val CY = 233f
    const val RADIUS = 205f
    const val FLOOR_Y = 384f
    const val HEADER_Y = 72f
    const val HIT_RADIUS = 74f
    const val MAX_MISSES = 3

    data class Ball(val x: Float, val y: Float, val vx: Float, val vy: Float)

    data class GameState(
        val ball: Ball,
        val score: Int = 0,
        val misses: Int = 0,
        val petX: Float = 233f,
        val gameOver: Boolean = false,
    )

    fun newGame(rng: Random = Random.Default): GameState = GameState(ball = spawnBall(0, rng))

    private fun spawnBall(score: Int, rng: Random): Ball {
        val x = 150f + rng.nextInt(166)
        var speed = 1.6f + score * 0.05f
        if (speed > 4.0f) speed = 4.0f
        val vx = if (rng.nextBoolean()) speed else -speed
        return Ball(x = x, y = 96f, vx = vx, vy = 0f)
    }

    /** Registers a tap at (x, y) in logical arena coordinates; scores a hit if it lands on the ball. */
    fun tap(state: GameState, x: Float, y: Float): GameState {
        if (state.gameOver) return state
        val dx = state.ball.x - x
        val dy = state.ball.y - y
        if (dx * dx + dy * dy >= HIT_RADIUS * HIT_RADIUS) return state

        val newScore = state.score + 1
        val lift = 6.6f + if (newScore > 16) 3.5f else newScore * 0.22f
        var vx = state.ball.vx + dx * 0.12f
        vx = vx.coerceIn(-6.5f, 6.5f)
        return state.copy(score = newScore, ball = state.ball.copy(vy = -lift, vx = vx))
    }

    /** One physics tick (the firmware calls this every ~85ms while the minigame is open). */
    fun step(state: GameState, rng: Random = Random.Default): GameState {
        if (state.gameOver) return state

        var grav = 0.40f + state.score * 0.013f
        if (grav > 0.80f) grav = 0.80f
        var vy = state.ball.vy + grav
        var vx = state.ball.vx
        var x = state.ball.x + vx
        var y = state.ball.y + vy

        val dx = x - CX
        val dy = y - CY
        val d = sqrt(dx * dx + dy * dy)
        if (d > RADIUS) {
            val nx = dx / d
            val ny = dy / d
            val dot = vx * nx + vy * ny
            if (dot > 0) {
                vx = (vx - 2 * dot * nx) * 0.85f
                vy = (vy - 2 * dot * ny) * 0.85f
            }
            x = CX + nx * RADIUS
            y = CY + ny * RADIUS
        }

        var ball = Ball(x, y, vx, vy)
        var misses = state.misses
        var gameOver = false
        if (y > FLOOR_Y) {
            misses++
            if (misses >= MAX_MISSES) {
                gameOver = true
            } else {
                ball = spawnBall(state.score, rng)
            }
        }

        var chase = (ball.x - state.petX) * 0.12f
        chase = chase.coerceIn(-7f, 7f)

        return state.copy(ball = ball, misses = misses, petX = state.petX + chase, gameOver = gameOver)
    }
}
