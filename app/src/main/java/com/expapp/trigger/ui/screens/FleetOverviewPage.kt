package com.expapp.trigger.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.repository.ThemeMode
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.ui.components.AccentColorDialog
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.MonoFontFamily
import com.expapp.trigger.ui.theme.PixelSemantic
import com.expapp.trigger.ui.theme.ServerAccents
import com.expapp.trigger.ui.theme.ServerMaterialTheme

@Composable
fun FleetOverviewPage(
    devices: List<DeviceConfig>,
    nodeStatuses: Map<String, NodeStatus>,
    checkingStatusMap: Map<String, Boolean>,
    isSyncing: Boolean,
    themeMode: ThemeMode,
    onOpenServer: (index: Int, openSysPulse: Boolean) -> Unit,
    onAssignAccentColor: (deviceId: String, hex: String) -> Unit,
    onSyncAllR2: () -> Unit,
    onRefreshAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsDarkTheme.current
    var colorPickerDevice by remember { mutableStateOf<DeviceConfig?>(null) }

    if (colorPickerDevice != null) {
        val target = colorPickerDevice!!
        AccentColorDialog(
            serverName = target.name,
            selectedHex = target.accentColorHex,
            onSelectHex = { hex -> onAssignAccentColor(target.id, hex) },
            onDismiss = { colorPickerDevice = null }
        )
    }

    val onlineCount = devices.count { nodeStatuses[it.id]?.isOnline == true }
    val offlineCount = devices.count { nodeStatuses[it.id]?.isOnline == false }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── PIXEL FLEET SUMMARY CARD ─────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Infrastructure Fleet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap any server to manage or customize its palette",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(onClick = onSyncAllR2) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = if (isSyncing) "Syncing" else "Sync R2",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        FilledTonalIconButton(onClick = onRefreshAll) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Ping All",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FleetStatPill(
                        label = "Total",
                        value = "${devices.size}",
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    FleetStatPill(
                        label = "Online",
                        value = "$onlineCount",
                        containerColor = PixelSemantic.onlineContainer(dark),
                        contentColor = PixelSemantic.onOnlineContainer(dark),
                        modifier = Modifier.weight(1f)
                    )
                    FleetStatPill(
                        label = "Offline",
                        value = "$offlineCount",
                        containerColor = if (offlineCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (offlineCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── PER-SERVER MATERIAL YOU SEEDED CARDS ─────────────────────────
        if (devices.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "No Servers Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap + Add Server below to configure from Cloudflare R2.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            devices.forEachIndexed { idx, device ->
                val status = nodeStatuses[device.id]
                val isChecking = checkingStatusMap[device.id] == true

                ServerMaterialTheme(seedHex = device.accentColorHex, themeMode = themeMode) {
                    FleetServerCard(
                        device = device,
                        status = status,
                        isChecking = isChecking,
                        onOpenControl = { onOpenServer(idx, false) },
                        onOpenSysPulse = { onOpenServer(idx, true) },
                        onPickAccentColor = { colorPickerDevice = device }
                    )
                }
            }
        }

        // Bottom space so ExtendedFloatingActionButton never overlaps the last card
        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
private fun FleetServerCard(
    device: DeviceConfig,
    status: NodeStatus?,
    isChecking: Boolean,
    onOpenControl: () -> Unit,
    onOpenSysPulse: () -> Unit,
    onPickAccentColor: () -> Unit
) {
    val dark = LocalIsDarkTheme.current
    val isOnline = status?.isOnline
    val accentSwatch = ServerAccents.parseColor(device.accentColorHex)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onOpenControl),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(device.node)
                            if (!status?.ip.isNullOrBlank()) {
                                append(" • ")
                                append(status!!.ip)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 1-Tap Seed Color Picker Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onPickAccentColor),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(accentSwatch)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme color",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Status & Uptime Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val chipBg = when {
                    isChecking -> MaterialTheme.colorScheme.surfaceContainerHigh
                    isOnline == true -> PixelSemantic.onlineContainer(dark)
                    else -> MaterialTheme.colorScheme.errorContainer
                }
                val chipFg = when {
                    isChecking -> MaterialTheme.colorScheme.onSurfaceVariant
                    isOnline == true -> PixelSemantic.onOnlineContainer(dark)
                    else -> MaterialTheme.colorScheme.onErrorContainer
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = chipBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = chipFg
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(chipFg)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isChecking) "Checking" else (status?.statusText?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Unknown"),
                            style = MaterialTheme.typography.labelMedium,
                            color = chipFg
                        )
                    }
                }

                val metaText = when {
                    isOnline == true && !status.uptime.isNullOrBlank() -> "Up ${status.uptime}"
                    isOnline == false && !status.lastActiveAgo.isNullOrBlank() -> "Seen ${status.lastActiveAgo}"
                    device.r2Url.isNotBlank() -> "R2 Synced"
                    else -> "Manual"
                }
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenControl,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Controls")
                }

                FilledTonalButton(
                    onClick = onOpenSysPulse,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SysPulse")
                }
            }
        }
    }
}

@Composable
private fun FleetStatPill(
    label: String,
    value: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
