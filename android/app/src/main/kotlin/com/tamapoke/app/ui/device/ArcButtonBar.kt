package com.tamapoke.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ArcButton(val emoji: String, val contentDescription: String, val onClick: () -> Unit)

/**
 * Icon-only round buttons along the bottom of the circular screen, echoing
 * the firmware's bottom arc buttons (Feed/Play/Light/Bath). A literal
 * curved arc layout would need per-button trigonometry for little visual
 * gain at this size; a tight centered row reads the same at a glance.
 */
@Composable
fun ArcButtonBar(buttons: List<ArcButton>, modifier: Modifier = Modifier) {
    Row(modifier) {
        buttons.forEach { button ->
            Box(
                Modifier
                    .padding(6.dp)
                    .size(48.dp)
                    .background(Color(0xCC1A1D24), CircleShape)
                    .clickable(onClick = button.onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(button.emoji, fontSize = 22.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}
