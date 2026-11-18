package com.expapp.trigger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.theme.AmberBorder
import com.expapp.trigger.ui.theme.AmberVibrant
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderDefault
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.RedBorder
import com.expapp.trigger.ui.theme.RedVibrant
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary

@Composable
fun ActionConfirmDialog(
    action: TriggerAction,
    deviceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (actionName, actionIcon, accentColor, bgGradient, description) = when (action) {
        TriggerAction.POWER_ON -> Quintuple(
            "Power On", Icons.Default.PowerSettingsNew, GreenVibrant,
            listOf(Color(0xFF071A0F), Color(0xFF0C1118)),
            "This will send a wake signal to boot the remote machine."
        )
        TriggerAction.POWER_OFF -> Quintuple(
            "Power Off", Icons.Default.PowerOff, RedVibrant,
            listOf(Color(0xFF1A0007), Color(0xFF0C1118)),
            "This will shut down the remote machine. All running processes will stop."
        )
        TriggerAction.REBOOT -> Quintuple(
            "Reboot", Icons.Default.RestartAlt, AmberVibrant,
            listOf(Color(0xFF191000), Color(0xFF0C1118)),
            "This will gracefully restart the remote machine."
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(bgGradient)
                )
                .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(accentColor.copy(alpha = 0.2f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionName,
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Confirm $actionName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = deviceName,
                    fontSize = 13.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgElevated)
                            .border(1.dp, BorderDefault, RoundedCornerShape(14.dp))
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.15f),
                            contentColor = accentColor
                        )
                    ) {
                        Text(
                            actionName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// Helper data class
private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component1() = a
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component2() = b
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component3() = c
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component4() = d
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component5() = e
