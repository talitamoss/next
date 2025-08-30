package com.domain.app.ui.components.core.button

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * State of the action button
 */
sealed class ActionButtonState {
    object Idle : ActionButtonState()
    data class Active(val startTime: Long = System.currentTimeMillis()) : ActionButtonState()
    object Completed : ActionButtonState()
    data class Error(val message: String) : ActionButtonState()
}

/**
 * Configuration for the ActionButton
 */
data class ActionButtonConfig(
    // Visual configuration
    val size: Dp = 120.dp,
    val shape: ActionButtonShape = ActionButtonShape.CIRCLE,
    
    // Colors for different states
    val idleColor: Color = Color(0xFF4CAF50),  // Green
    val activeColor: Color = Color(0xFFFF5252),  // Red
    val completedColor: Color = Color(0xFF2196F3),  // Blue
    val errorColor: Color = Color(0xFFFFA726),  // Orange
    
    // Icons for different states
    val idleIcon: ImageVector = Icons.Default.PlayArrow,
    val activeIcon: ImageVector = Icons.Default.Stop,
    val completedIcon: ImageVector = Icons.Default.Check,
    val errorIcon: ImageVector = Icons.Default.Warning,
    
    // Text labels (optional)
    val idleText: String? = null,
    val activeText: String? = null,
    val completedText: String? = null,
    val errorText: String? = null,
    
    // Behavior
    val showPulseAnimation: Boolean = true,
    val showTimer: Boolean = false,
    val autoResetDelay: Long? = null,  // Auto reset to idle after completion (ms)
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false
)

/**
 * Button shape options
 */
enum class ActionButtonShape {
    CIRCLE,
    ROUNDED_SQUARE,
    SQUARE
}

/**
 * Main ActionButton composable
 * A reusable button for starting/stopping actions with visual feedback
 */
@Composable
fun ActionButton(
    state: ActionButtonState,
    config: ActionButtonConfig = ActionButtonConfig(),
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Animation values
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val borderAlpha by pulseAnimation.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border"
    )
    
    // State-based values with animation
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            is ActionButtonState.Idle -> config.idleColor
            is ActionButtonState.Active -> config.activeColor
            is ActionButtonState.Completed -> config.completedColor
            is ActionButtonState.Error -> config.errorColor
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val currentIcon = when (state) {
        is ActionButtonState.Idle -> config.idleIcon
        is ActionButtonState.Active -> config.activeIcon
        is ActionButtonState.Completed -> config.completedIcon
        is ActionButtonState.Error -> config.errorIcon
    }
    
    val currentText = when (state) {
        is ActionButtonState.Idle -> config.idleText
        is ActionButtonState.Active -> config.activeText
        is ActionButtonState.Completed -> config.completedText
        is ActionButtonState.Error -> config.errorText
    }
    
    // Timer for active state
    var elapsedTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(state) {
        when (state) {
            is ActionButtonState.Active -> {
                while (state is ActionButtonState.Active) {
                    delay(100)
                    elapsedTime = System.currentTimeMillis() - state.startTime
                }
            }
            is ActionButtonState.Completed -> {
                config.autoResetDelay?.let { delay ->
                    delay(delay)
                    // Trigger reset through onClick or external state management
                }
            }
            else -> {
                elapsedTime = 0
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(config.size)
            .scale(if (config.showPulseAnimation && state is ActionButtonState.Active) pulseScale else 1f),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring when active
        if (config.showPulseAnimation && state is ActionButtonState.Active) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(getShape(config.shape))
                    .border(
                        width = 3.dp,
                        color = backgroundColor.copy(alpha = borderAlpha),
                        shape = getShape(config.shape)
                    )
            )
        }
        
        // Main button
        Surface(
            modifier = Modifier
                .fillMaxSize(0.9f)  // Slightly smaller than container for pulse effect
                .clip(getShape(config.shape))
                .clickable(enabled = enabled) { onClick() },
            shape = getShape(config.shape),
            color = backgroundColor,
            shadowElevation = if (enabled) 6.dp else 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                Icon(
                    imageVector = currentIcon,
                    contentDescription = currentText,
                    tint = Color.White,
                    modifier = Modifier.size(config.size * 0.3f)
                )
                
                // Optional text
                currentText?.let { text ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = (config.size.value * 0.12f).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                
                // Timer display when active
                if (config.showTimer && state is ActionButtonState.Active) {
                    Text(
                        text = formatTime(elapsedTime),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = (config.size.value * 0.1f).sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Variant: Rectangular action button
 */
@Composable
fun RectangularActionButton(
    state: ActionButtonState,
    config: ActionButtonConfig = ActionButtonConfig(),
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = true
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            is ActionButtonState.Idle -> config.idleColor
            is ActionButtonState.Active -> config.activeColor
            is ActionButtonState.Completed -> config.completedColor
            is ActionButtonState.Error -> config.errorColor
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(
            if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (state) {
                    is ActionButtonState.Idle -> config.idleIcon
                    is ActionButtonState.Active -> config.activeIcon
                    is ActionButtonState.Completed -> config.completedIcon
                    is ActionButtonState.Error -> config.errorIcon
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            val text = when (state) {
                is ActionButtonState.Idle -> config.idleText
                is ActionButtonState.Active -> config.activeText
                is ActionButtonState.Completed -> config.completedText
                is ActionButtonState.Error -> config.errorText
            }
            
            text?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            // Show timer for active state
            if (config.showTimer && state is ActionButtonState.Active) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTime(System.currentTimeMillis() - state.startTime),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * Mini floating action button variant
 */
@Composable
fun MiniActionButton(
    state: ActionButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            is ActionButtonState.Idle -> MaterialTheme.colorScheme.primary
            is ActionButtonState.Active -> Color(0xFFFF5252)
            is ActionButtonState.Completed -> Color(0xFF4CAF50)
            is ActionButtonState.Error -> MaterialTheme.colorScheme.error
        },
        label = "color"
    )
    
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = backgroundColor,
        modifier = modifier
    ) {
        Icon(
            imageVector = when (state) {
                is ActionButtonState.Idle -> Icons.Default.Add
                is ActionButtonState.Active -> Icons.Default.Stop
                is ActionButtonState.Completed -> Icons.Default.Check
                is ActionButtonState.Error -> Icons.Default.Warning
            },
            contentDescription = null
        )
    }
}

// Helper functions

private fun getShape(shape: ActionButtonShape) = when (shape) {
    ActionButtonShape.CIRCLE -> CircleShape
    ActionButtonShape.ROUNDED_SQUARE -> androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ActionButtonShape.SQUARE -> androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        String.format("%d:%02d", minutes, remainingSeconds)
    } else {
        String.format("0:%02d", remainingSeconds)
    }
}

/**
 * Preview helpers for development
 */
@Composable
fun ActionButtonPreview() {
    var state by remember { mutableStateOf<ActionButtonState>(ActionButtonState.Idle) }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Circular button
        ActionButton(
            state = state,
            config = ActionButtonConfig(
                idleText = "START",
                activeText = "STOP",
                completedText = "DONE",
                showTimer = true
            ),
            onClick = {
                state = when (state) {
                    is ActionButtonState.Idle -> ActionButtonState.Active()
                    is ActionButtonState.Active -> ActionButtonState.Completed
                    is ActionButtonState.Completed -> ActionButtonState.Idle
                    is ActionButtonState.Error -> ActionButtonState.Idle
                }
            }
        )
        
        // Rectangular button
        RectangularActionButton(
            state = state,
            config = ActionButtonConfig(
                idleText = "Start Recording",
                activeText = "Stop Recording",
                completedText = "Recording Saved",
                showTimer = true
            ),
            onClick = {
                state = when (state) {
                    is ActionButtonState.Idle -> ActionButtonState.Active()
                    is ActionButtonState.Active -> ActionButtonState.Completed
                    is ActionButtonState.Completed -> ActionButtonState.Idle
                    is ActionButtonState.Error -> ActionButtonState.Idle
                }
            }
        )
        
        // Mini FAB
        MiniActionButton(
            state = state,
            onClick = {
                state = when (state) {
                    is ActionButtonState.Idle -> ActionButtonState.Active()
                    is ActionButtonState.Active -> ActionButtonState.Idle
                    else -> ActionButtonState.Idle
                }
            }
        )
    }
}
