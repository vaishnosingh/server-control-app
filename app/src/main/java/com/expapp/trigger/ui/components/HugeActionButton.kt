package com.expapp.trigger.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HugeActionButton(
    text: String,
    subtext: String,
    gradientColors: List<Color>,
    borderColor: Color,
    icon: ImageVector,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "button_scale"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.25f,
        animationSpec = tween(300),
        label = "border_alpha"
    )

    val contentAlpha = if (enabled) 1f else 0.35f
    val shape = RoundedCornerShape(20.dp)

    // Pulsing glow when enabled
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val shadowColor = borderColor.copy(alpha = if (enabled && !isLoading) glowAlpha * 0.5f else 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (enabled) 16.dp else 0.dp,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    if (enabled) gradientColors
                    else gradientColors.map { it.copy(alpha = 0.2f) }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor.copy(alpha = borderAlpha * 0.8f),
                        borderColor.copy(alpha = borderAlpha * 0.2f),
                        borderColor.copy(alpha = borderAlpha * 0.5f),
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = borderColor.copy(alpha = 0.4f)),
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon circle with glow
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    borderColor.copy(alpha = if (enabled) 0.25f else 0.08f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            1.dp,
                            borderColor.copy(alpha = if (enabled) 0.4f else 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = borderColor.copy(alpha = contentAlpha),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column {
                    Text(
                        text = text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = contentAlpha),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = contentAlpha * 0.6f)
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = borderColor,
                    strokeWidth = 2.5.dp,
                    trackColor = borderColor.copy(alpha = 0.2f)
                )
            } else if (enabled) {
                // Chevron
                Text(
                    text = "›",
                    fontSize = 28.sp,
                    color = borderColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}
