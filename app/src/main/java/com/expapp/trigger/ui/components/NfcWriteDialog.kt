package com.expapp.trigger.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.nfc.NfcTriggerPayload
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.PixelSemantic

@Composable
fun NfcWriteDialog(
    devices: List<DeviceConfig>,
    writeResult: Result<Unit>?,
    onStartWriting: (NfcTriggerPayload) -> Unit,
    onStopWriting: () -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalIsDarkTheme.current
    var selectedDevice by remember { mutableStateOf(devices.firstOrNull()) }
    var selectedAction by remember { mutableStateOf(TriggerAction.POWER_ON) }
    var isWaitingForTag by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            onStopWriting()
        }
    }

    val pulseScale by rememberInfiniteTransition(label = "nfcPulse").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = {
            onStopWriting()
            onDismiss()
        }
    ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "NFC Tag Writer",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onStopWriting()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    writeResult != null -> {
                        if (writeResult.isSuccess) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = PixelSemantic.onlineAccent(dark),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "NFC Tag Programmed!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap this tag on your phone to trigger ${selectedAction.name} on ${selectedDevice?.name ?: "server"}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Write Failed",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = writeResult.exceptionOrNull()?.message ?: "Could not write to NFC tag.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                onStopWriting()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(100)
                        ) {
                            Text("Done")
                        }
                    }

                    isWaitingForTag -> {
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(50.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Hold Tag Near Phone",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ready to write ${selectedAction.name} for ${selectedDevice?.name ?: ""}.\nHold an NFC tag against the back of your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        TextButton(
                            onClick = {
                                isWaitingForTag = false
                                onStopWriting()
                            },
                            shape = RoundedCornerShape(100)
                        ) {
                            Text("Cancel")
                        }
                    }

                    else -> {
                        if (devices.isEmpty()) {
                            Text(
                                text = "Add a server first before programming an NFC tag.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Select Server",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                devices.forEach { dev ->
                                    val selected = selectedDevice?.id == dev.id
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedDevice = dev },
                                        label = { Text(dev.name) },
                                        shape = RoundedCornerShape(100),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Select Action",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TriggerAction.entries.forEach { action ->
                                    val selected = selectedAction == action
                                    val label = when (action) {
                                        TriggerAction.POWER_ON -> "Power On"
                                        TriggerAction.REBOOT -> "Reboot"
                                        TriggerAction.POWER_OFF -> "Shutdown"
                                    }
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedAction = action },
                                        modifier = Modifier.weight(1f),
                                        label = {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontWeight = FontWeight.SemiBold)
                                            }
                                        },
                                        shape = RoundedCornerShape(100)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val dev = selectedDevice
                                    if (dev != null) {
                                        isWaitingForTag = true
                                        onStartWriting(
                                            NfcTriggerPayload(
                                                nodeOrDeviceId = dev.node,
                                                action = selectedAction
                                            )
                                        )
                                    }
                                },
                                enabled = selectedDevice != null,
                                shape = RoundedCornerShape(100),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Write to Tag")
                            }
                        }
                    }
                }
            }
        }
    }
}
