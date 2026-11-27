package com.expapp.trigger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.PixelSemantic

private data class ConfirmStyle(
    val actionName: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val confirmBg: Color,
    val confirmContent: Color,
    val description: String
)

@Composable
fun ActionConfirmDialog(
    action: TriggerAction,
    deviceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalIsDarkTheme.current
    val style = when (action) {
        TriggerAction.POWER_ON -> ConfirmStyle(
            actionName = "Power On",
            icon = Icons.Default.PowerSettingsNew,
            iconBg = PixelSemantic.onlineContainer(dark),
            iconTint = PixelSemantic.onOnlineContainer(dark),
            confirmBg = PixelSemantic.onlineContainer(dark),
            confirmContent = PixelSemantic.onOnlineContainer(dark),
            description = "This will send a wake signal to boot the remote machine."
        )
        TriggerAction.REBOOT -> ConfirmStyle(
            actionName = "Reboot",
            icon = Icons.Default.RestartAlt,
            iconBg = PixelSemantic.warningContainer(dark),
            iconTint = PixelSemantic.onWarningContainer(dark),
            confirmBg = PixelSemantic.warningContainer(dark),
            confirmContent = PixelSemantic.onWarningContainer(dark),
            description = "This will restart the remote machine. Active sessions will be interrupted."
        )
        TriggerAction.POWER_OFF -> ConfirmStyle(
            actionName = "Power Off",
            icon = Icons.Default.PowerOff,
            iconBg = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
            confirmBg = MaterialTheme.colorScheme.error,
            confirmContent = MaterialTheme.colorScheme.onError,
            description = "This will shut down the remote machine. All running processes will stop."
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(style.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.iconTint,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Confirm ${style.actionName}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = style.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(100),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = style.confirmBg,
                            contentColor = style.confirmContent
                        )
                    ) {
                        Text(style.actionName)
                    }
                }
            }
        }
    }
}
