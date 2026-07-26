package com.tamapoke.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A round bezel echoing the device's actual shape: the Waveshare
 * ESP32-S3-Touch-AMOLED-1.75 has a circular 466x466 display, not a
 * rectangular one - this is the single biggest visual cue that's easy to
 * add without redoing every screen's layout from scratch.
 */
@Composable
fun RoundDeviceFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF15181F), CircleShape)
                .padding(10.dp)
                .clip(CircleShape),
            content = content,
        )
    }
}
