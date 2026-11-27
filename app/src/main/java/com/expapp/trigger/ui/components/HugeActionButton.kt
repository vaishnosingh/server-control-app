package com.expapp.trigger.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Google Pixel Material You tonal action card for server power operations.
 */
@Composable
fun HugeActionButton(
    text: String,
    subtext: String,
    gradientColors: List<Color> = emptyList(),
    borderColor: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconBackgroundColor: Color = borderColor.copy(alpha = 0.18f),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "button_scale"
    )

    val shape = RoundedCornerShape(24.dp)
    val effectiveContainer = if (enabled) {
        containerColor
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val effectiveText = if (enabled) {
        contentColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
    }
    val effectiveSubtext = if (enabled) {
        contentColor.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    val effectiveIconBg = if (enabled) {
        iconBackgroundColor
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val effectiveIconTint = if (enabled) {
        contentColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = contentColor.copy(alpha = 0.2f)),
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        shape = shape,
        color = effectiveContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(effectiveIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = effectiveIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = effectiveText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = effectiveSubtext
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = contentColor,
                    strokeWidth = 2.5.dp,
                    trackColor = contentColor.copy(alpha = 0.2f)
                )
            } else if (enabled) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
