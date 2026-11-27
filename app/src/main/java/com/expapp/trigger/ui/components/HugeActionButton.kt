package com.expapp.trigger.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expapp.trigger.ui.theme.BgCard
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary

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
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "button_scale"
    )

    val shape = RoundedCornerShape(16.dp)
    val surfaceColor = if (enabled) BgCard else BgSurface.copy(alpha = 0.65f)
    val tintAccent = if (enabled) borderColor else TextMuted.copy(alpha = 0.45f)
    val cardBorder = if (enabled) borderColor.copy(alpha = 0.28f) else BorderSubtle.copy(alpha = 0.5f)
    val leadTint = gradientColors.firstOrNull()?.copy(alpha = if (enabled) 0.35f else 0.08f) ?: Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (enabled) borderColor.copy(alpha = 0.08f) else leadTint,
                        surfaceColor
                    )
                )
            )
            .border(1.dp, cardBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = borderColor.copy(alpha = 0.35f)),
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 15.dp)
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
                // Sleek left indicator bar
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tintAccent)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Icon tile
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tintAccent.copy(alpha = if (enabled) 0.13f else 0.06f))
                        .border(
                            1.dp,
                            tintAccent.copy(alpha = if (enabled) 0.32f else 0.12f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = tintAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) TextPrimary else TextMuted.copy(alpha = 0.7f),
                        letterSpacing = 0.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtext,
                        fontSize = 12.sp,
                        color = if (enabled) TextSecondary else TextMuted.copy(alpha = 0.45f)
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = borderColor,
                    strokeWidth = 2.dp,
                    trackColor = borderColor.copy(alpha = 0.2f)
                )
            } else if (enabled) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = borderColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
