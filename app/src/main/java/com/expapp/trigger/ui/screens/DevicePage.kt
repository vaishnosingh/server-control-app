package com.expapp.trigger.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.components.ActionConfirmDialog
import com.expapp.trigger.ui.components.HugeActionButton
import com.expapp.trigger.ui.components.StatusBanner
import com.expapp.trigger.ui.theme.DarkCardBorder
import com.expapp.trigger.ui.theme.DarkSurface
import com.expapp.trigger.ui.theme.DarkSurfaceVariant
import com.expapp.trigger.ui.theme.PowerOffBorder
import com.expapp.trigger.ui.theme.PowerOffGradient
import com.expapp.trigger.ui.theme.PowerOffRed
import com.expapp.trigger.ui.theme.PowerOnBorder
import com.expapp.trigger.ui.theme.PowerOnGradient
import com.expapp.trigger.ui.theme.RebootBorder
import com.expapp.trigger.ui.theme.RebootGradient
import com.expapp.trigger.ui.theme.StatusOfflineBg
import com.expapp.trigger.ui.theme.StatusOfflineBorder
import com.expapp.trigger.ui.theme.StatusOfflineText
import com.expapp.trigger.ui.theme.StatusOnlineBg
import com.expapp.trigger.ui.theme.StatusOnlineBorder
import com.expapp.trigger.ui.theme.StatusOnlineText
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary

@Composable
fun DevicePage(
    device: DeviceConfig,
    isLoading: Boolean,
    nodeStatus: NodeStatus?,
    isCheckingStatus: Boolean,
    lastStatus: String?,
    lastSuccess: Boolean?,
    onDismissStatus: () -> Unit,
    onRefreshStatus: () -> Unit,
    onTrigger: (TriggerAction) -> Unit,
    onDelete: (DeviceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingAction by remember { mutableStateOf<TriggerAction?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (pendingAction != null) {
        ActionConfirmDialog(
            action = pendingAction!!,
            deviceName = device.name,
            onConfirm = {
                val actionToExecute = pendingAction!!
                pendingAction = null
                onTrigger(actionToExecute)
            },
            onDismiss = { pendingAction = null }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Device") },
            text = { Text("Are you sure you want to remove \"${device.name}\" from your trigger list?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(device)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PowerOffRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Professional Server Info Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
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
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = if (nodeStatus?.isOnline == true) StatusOnlineText else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = device.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "node: ${device.node}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete device",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Server Status Badge & IP Address
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live Status Pill
                        val isOnline = nodeStatus?.isOnline == true
                        val badgeBg = if (isCheckingStatus) DarkSurfaceVariant else if (isOnline) StatusOnlineBg else StatusOfflineBg
                        val badgeBorder = if (isCheckingStatus) DarkCardBorder else if (isOnline) StatusOnlineBorder else StatusOfflineBorder
                        val badgeText = if (isCheckingStatus) TextSecondary else if (isOnline) StatusOnlineText else StatusOfflineText

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBg)
                                .border(1.dp, badgeBorder, RoundedCornerShape(8.dp))
                                .clickable { onRefreshStatus() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCheckingStatus) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PINGING...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(badgeText)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = nodeStatus?.statusText ?: "OFFLINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeText,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // IP Address chip if returned
                        if (!nodeStatus?.ip.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = nodeStatus!!.ip!!,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Tap to re-check indicator
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRefreshStatus() }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Check Status",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "check",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Banner for Action Result
        StatusBanner(
            status = lastStatus,
            isSuccess = lastSuccess,
            onDismiss = onDismissStatus
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3 Massive Tactile Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            HugeActionButton(
                text = "POWER ON",
                subtext = "Wake / Start Remote Machine",
                gradientColors = PowerOnGradient,
                borderColor = PowerOnBorder,
                icon = Icons.Default.PowerSettingsNew,
                isLoading = isLoading && pendingAction == null,
                onClick = { onTrigger(TriggerAction.POWER_ON) }
            )

            HugeActionButton(
                text = "REBOOT",
                subtext = "Graceful Restart (Auth Required)",
                gradientColors = RebootGradient,
                borderColor = RebootBorder,
                icon = Icons.Default.RestartAlt,
                isLoading = isLoading && pendingAction == null,
                onClick = { pendingAction = TriggerAction.REBOOT }
            )

            HugeActionButton(
                text = "POWER OFF",
                subtext = "Remote Shutdown (Auth Required)",
                gradientColors = PowerOffGradient,
                borderColor = PowerOffBorder,
                icon = Icons.Default.PowerOff,
                isLoading = isLoading && pendingAction == null,
                onClick = { pendingAction = TriggerAction.POWER_OFF }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
