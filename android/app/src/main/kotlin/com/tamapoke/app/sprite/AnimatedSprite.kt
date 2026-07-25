package com.tamapoke.app.sprite

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay

/**
 * Renders one animation from a bundled PMD sprite sheet, cycling frames on
 * their own individual durations (matches the firmware's per-frame timing
 * from AnimData.xml, not a fixed frame rate). [fallbackAction] is tried if
 * [action] isn't bundled for this species (e.g. not every species has an
 * "Eat" sheet); returns false from the content lambda so callers can fall
 * back to a placeholder when nothing at all is available.
 */
@Composable
fun AnimatedSprite(
    speciesId: Int,
    action: String,
    modifier: Modifier = Modifier,
    fallbackAction: String = "idle",
    placeholder: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val frames = remember(speciesId, action) {
        SpriteLoader.load(context, speciesId, action) ?: SpriteLoader.load(context, speciesId, fallbackAction)
    }

    if (frames == null || frames.frameCount == 0) {
        placeholder()
        return
    }

    var frameIndex by remember(frames) { mutableIntStateOf(0) }
    LaunchedEffect(frames) {
        while (true) {
            delay(frames.frameDurationsMs[frameIndex].toLong())
            frameIndex = (frameIndex + 1) % frames.frameCount
        }
    }

    Canvas(modifier) {
        drawSpriteFrame(frames, frameIndex)
    }
}

private fun DrawScope.drawSpriteFrame(frames: SpriteFrameSet, frameIndex: Int) {
    drawImage(
        image = frames.bitmap,
        srcOffset = IntOffset(frameIndex * frames.frameWidth, frames.row * frames.frameHeight),
        srcSize = IntSize(frames.frameWidth, frames.frameHeight),
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        filterQuality = FilterQuality.None,
    )
}
