package com.expapp.trigger.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.ui.components.AccentColorDialog
import com.expapp.trigger.ui.theme.BgCard
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.OfflineBg
import com.expapp.trigger.ui.theme.OfflineBorder
import com.expapp.trigger.ui.theme.OfflineText
import com.expapp.trigger.ui.theme.OnlineBg
import com.expapp.trigger.ui.theme.OnlineBorder
import com.expapp.trigger.ui.theme.OnlineText
import com.expapp.trigger.ui.theme.RedVibrant
import com.expapp.trigger.ui.theme.ServerAccents
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary

@Composable
fun FleetOverviewPage(
    devices: List<DeviceConfig>,
    nodeStatuses: Map<String, NodeStatus>,
    checkingStatusMap: Map<String, Boolean>,
    isSyncing: Boolean,
    onOpenServer: (index: Int, openDashboard: Boolean) -> Unit,
    onAssignAccentColor: (deviceId: String, hex: String) -> Unit,
    onSyncAllR2: () -> Unit,
    onRefreshAll: () -> Unit,
    onAddServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── FLEET SUMMARY HEADER ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(BgElevated, BgSurface)))
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SERVER FLEET",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap a server to manage or pick an accent color",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FleetTopActionChip(
                            label = if (isSyncing) "SYNCING" else "SYNC R2",
                            icon = Icons.Default.CloudSync,
                            color = Color(0xFF38BDF8),
                            onClick = onSyncAllR2
                        )
                        FleetTopActionChip(
                            label = "PING",
                            icon = Icons.Default.Refresh,
                            color = GreenVibrant,
                            onClick = onRefreshAll
                        )
                    }
                }

                // KPI Metrics Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FleetStatChip(
                        label = "TOTAL",
                        value = "${devices.size}",
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    FleetStatChip(
                        label = "ONLINE",
                        value = "$onlineCount",
                        color = GreenVibrant,
                        modifier = Modifier.weight(1f)
                    )
                    FleetStatChip(
                        label = "OFFLINE",
                        value = "$offlineCount",
                        color = if (offlineCount > 0) RedVibrant else TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── SERVER CARDS LIST ────────────────────────────────────────────
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Servers Configured",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add a server using its Cloudflare R2 JSON config URL.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            devices.forEachIndexed { idx, device ->
                val status = nodeStatuses[device.id]
                val isChecking = checkingStatusMap[device.id] == true
                val accent = ServerAccents.parseColor(device.accentColorHex)

                FleetServerCard(
                    device = device,
                    accentColor = accent,
                    status = status,
                    isChecking = isChecking,
                    onOpenControl = { onOpenServer(idx, false) },
                    onOpenSysPulse = { onOpenServer(idx, true) },
                    onPickAccentColor = { colorPickerDevice = device }
                )
            }
        }

        // ── ADD SERVER CTA CARD ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, GreenVibrant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable(onClick = onAddServerClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = GreenVibrant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add New Server via R2",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GreenVibrant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FleetServerCard(
    device: DeviceConfig,
    accentColor: Color,
    status: NodeStatus?,
    isChecking: Boolean,
    onOpenControl: () -> Unit,
    onOpenSysPulse: () -> Unit,
    onPickAccentColor: () -> Unit
) {
    val isOnline = status?.isOnline

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenControl)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Vivid top accent bar for instant server identification
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.35f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.14f))
                            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = device.node,
                                fontSize = 11.sp,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!status?.ip.isNullOrBlank()) {
                                Text(
                                    text = "• ${status?.ip}",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 1-Tap Accent Color Assignment Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgElevated)
                            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onPickAccentColor)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Assign accent color",
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Status & Uptime Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusBg = if (isChecking) BgElevated else if (isOnline == true) OnlineBg else OfflineBg
                    val statusBorder = if (isChecking) BorderSubtle else if (isOnline == true) OnlineBorder else OfflineBorder
                    val statusColor = if (isChecking) TextSecondary else if (isOnline == true) OnlineText else OfflineText

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .border(1.dp, statusBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(9.dp),
                                strokeWidth = 1.5.dp,
                                color = TextSecondary
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(statusColor, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isChecking) "CHECKING" else (status?.statusText ?: "UNKNOWN"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val metaText = when {
                        isOnline == true && !status.uptime.isNullOrBlank() -> "Up ${status.uptime}"
                        isOnline == false && !status.lastActiveAgo.isNullOrBlank() -> "Seen ${status.lastActiveAgo}"
                        device.r2Url.isNotBlank() -> "R2 Synced"
                        else -> "Manual"
                    }
                    Text(
                        text = metaText,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Action Bar: Power Page vs SysPulse Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.14f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenControl)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Power Control",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenSysPulse)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "SysPulse",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FleetTopActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun FleetStatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.8.sp
        )
    }
}
