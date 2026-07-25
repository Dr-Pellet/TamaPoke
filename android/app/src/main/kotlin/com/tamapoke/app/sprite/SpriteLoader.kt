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
 * Loads sprites from assets/sprites/<speciesId>/<action>.{png,json}. Only a
 * starter-species subset is bundled so far (see android/README.md) - callers
 * should treat a null result as "fall back to a placeholder" rather than a bug.
 */
object SpriteLoader {
    private val cache = mutableMapOf<String, SpriteFrameSet?>()

    fun load(context: Context, speciesId: Int, action: String): SpriteFrameSet? {
        val key = "$speciesId/$action"
        if (cache.containsKey(key)) return cache[key]
        val result = runCatching {
            val jsonText = context.assets.open("sprites/$speciesId/$action.json").bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)
            val bitmap = context.assets.open("sprites/$speciesId/$action.png").use {
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
        cache[key] = result
        return result
    }
}
