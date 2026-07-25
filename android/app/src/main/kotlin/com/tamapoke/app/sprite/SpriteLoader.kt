package com.tamapoke.app.sprite

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject

/**
 * One PMD SpriteCollab animation, as packed by tools/pack_pmd_android.py:
 * the original multi-direction sprite sheet PNG (unmodified, no re-encoding)
 * plus the frame rect/timing metadata needed to crop it at render time.
 */
data class SpriteFrameSet(
    val bitmap: ImageBitmap,
    val frameWidth: Int,
    val frameHeight: Int,
    val row: Int,
    val frameDurationsMs: List<Int>,
) {
    val frameCount: Int get() = frameDurationsMs.size
}

/**
 * Loads sprites from assets/sprites/<speciesId>/<action>.{png,json} (normal)
 * or assets/sprites/<speciesId>/shiny/<action>.{png,json} (shiny). All 151
 * species are bundled for Idle/Walk-L/Walk-R/Sleep/Eat/Hurt/Attack/Pose,
 * both variants, where the source sheet has that animation at all - a null
 * result just means "fall back to a placeholder / the non-shiny variant",
 * not a bug (see android/README.md).
 */
object SpriteLoader {
    private val cache = mutableMapOf<String, SpriteFrameSet?>()

    fun load(context: Context, speciesId: Int, action: String, shiny: Boolean = false): SpriteFrameSet? {
        if (shiny) {
            loadInternal(context, "sprites/$speciesId/shiny/$action")?.let { return it }
        }
        return loadInternal(context, "sprites/$speciesId/$action")
    }

    private fun loadInternal(context: Context, basePath: String): SpriteFrameSet? {
        if (cache.containsKey(basePath)) return cache[basePath]
        val result = runCatching {
            val jsonText = context.assets.open("$basePath.json").bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)
            val bitmap = context.assets.open("$basePath.png").use {
                BitmapFactory.decodeStream(it)
            }.asImageBitmap()
            val durationsJson = json.getJSONArray("frameDurationsMs")
            val durations = (0 until durationsJson.length()).map { durationsJson.getInt(it) }
            SpriteFrameSet(
                bitmap = bitmap,
                frameWidth = json.getInt("frameWidth"),
                frameHeight = json.getInt("frameHeight"),
                row = json.getInt("row"),
                frameDurationsMs = durations,
            )
        }.getOrNull()
        cache[basePath] = result
        return result
    }
}
