package com.domain.app.ui.components.core.button

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

@Composable
fun SingleTapButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    alreadyDone: Boolean = false,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    doneColor: Color = Color(0xFF10B981),
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    Button(
        onClick = {
            if (!alreadyDone && enabled) {
                isPressed = true
                onClick()
            }
        },
        enabled = !alreadyDone && enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (alreadyDone) doneColor else primaryColor,
            disabledContainerColor = doneColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(scale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (alreadyDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (alreadyDone) "Done ✓" else text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
