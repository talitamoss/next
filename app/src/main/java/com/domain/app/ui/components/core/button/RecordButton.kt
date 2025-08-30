package com.domain.app.ui.components.core.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple circular record button that toggles between START and STOP
 * Following the pattern from existing button components in this package
 * 
 * @param isRecording Current recording state
 * @param onToggle Callback when button is tapped
 * @param modifier Optional modifier for the button
 * @param enabled Whether the button is clickable
 */
@Composable
fun RecordButton(
    isRecording: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Animate color changes - pattern from ActionButton.kt
    val backgroundColor by animateColorAsState(
        targetValue = if (isRecording) {
            Color(0xFFDC2626)  // Red when recording (matches ActionButton active color)
        } else {
            Color(0xFF10B981)  // Green when idle (matches SingleTapButton done color)
        },
        animationSpec = tween(300),  // Animation duration from ActionButton
        label = "backgroundColor"
    )
    
    // Simple circular button - size from ActionButton (140dp for large buttons)
    Box(
        modifier = modifier
            .size(140.dp)
            .clip(CircleShape)  // CircleShape already used in ActionButton
            .background(backgroundColor)
            .clickable(enabled = enabled) { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isRecording) "STOP" else "START",
            color = Color.White,
            fontSize = 24.sp,  // Large text for visibility
            fontWeight = FontWeight.Bold  // Pattern from SingleTapButton
        )
    }
}
