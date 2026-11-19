package com.expapp.trigger.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.components.ActionConfirmDialog
import com.expapp.trigger.ui.components.HugeActionButton
import com.expapp.trigger.ui.components.StatusBanner
import com.expapp.trigger.ui.theme.AmberVibrant
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderDefault
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.OfflineBg
import com.expapp.trigger.ui.theme.OfflineBorder
import com.expapp.trigger.ui.theme.OfflineText
import com.expapp.trigger.ui.theme.OnlineBg
import com.expapp.trigger.ui.theme.OnlineBorder
import com.expapp.trigger.ui.theme.OnlineText
import com.expapp.trigger.ui.theme.PowerOffBorder
import com.expapp.trigger.ui.theme.PowerOffGradient
import com.expapp.trigger.ui.theme.PowerOnBorder
import com.expapp.trigger.ui.theme.PowerOnGradient
import com.expapp.trigger.ui.theme.RebootBorder
import com.expapp.trigger.ui.theme.RebootGradient
import com.expapp.trigger.ui.theme.RedVibrant
import com.expapp.trigger.ui.theme.TextMuted
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
    onSyncConfig: () -> Unit = {},
    onUpdateDevice: (DeviceConfig) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingAction by remember { mutableStateOf<TriggerAction?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var editName by remember { mutableStateOf(device.name) }
        var editNode by remember { mutableStateOf(device.node) }
        var editR2Url by remember { mutableStateOf(device.r2Url) }
        var editSecretKey by remember { mutableStateOf(device.secretKey) }
        var showKey by remember { mutableStateOf(false) }

        val editFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenVibrant,
            unfocusedBorderColor = BorderDefault,
            focusedLabelColor = GreenVibrant,
            unfocusedLabelColor = TextSecondary,
            cursorColor = GreenVibrant,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = BgSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Edit Device", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Device Name") },
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNode,
                        onValueChange = { editNode = it },
                        label = { Text("Node Identifier") },
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editR2Url,
                        onValueChange = { editR2Url = it },
                        label = { Text("Cloudflare R2 JSON URL") },
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSecretKey,
                        onValueChange = { editSecretKey = it },
                        label = { Text("Secret Key") },
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditDialog = false
                        val updated = device.copy(
                            name = editName.trim().ifBlank { device.name },
                            node = editNode.trim().ifBlank { device.node },
                            r2Url = editR2Url.trim(),
                            secretKey = editSecretKey.trim()
                        )
                        onUpdateDevice(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenVibrant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = GreenVibrant, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (pendingAction != null) {
        ActionConfirmDialog(
            action = pendingAction!!,
            deviceName = device.name,
            onConfirm = {
                val a = pendingAction!!
                pendingAction = null
                onTrigger(a)
            },
            onDismiss = { pendingAction = null }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = BgSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Remove Device", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove \"${device.name}\" from your devices?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete(device) },
                    colors = ButtonDefaults.buttonColors(containerColor = RedVibrant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Remove", color = RedVibrant, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Spin animation for refresh icon
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spin"
    )

    // Pulse for online dot
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isOnline = nodeStatus?.isOnline
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── HEADER CARD ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(BgElevated, BgSurface)))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
        ) {
            // Top accent line that changes color with server state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                when (isOnline) {
                                    true  -> GreenVibrant.copy(alpha = 0.7f)
                                    false -> RedVibrant.copy(alpha = 0.5f)
                                    null  -> BorderDefault.copy(alpha = 0.4f)
                                },
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top row: Server Icon + Name / Node (flexible) + Header Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        when (isOnline) {
                                            true  -> GreenVibrant.copy(alpha = 0.18f)
                                            false -> RedVibrant.copy(alpha = 0.12f)
                                            null  -> BorderDefault.copy(alpha = 0.1f)
                                        },
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                when (isOnline) {
                                    true  -> GreenVibrant.copy(alpha = 0.35f)
                                    false -> RedVibrant.copy(alpha = 0.25f)
                                    null  -> BorderSubtle
                                },
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = when (isOnline) {
                                true  -> GreenVibrant
                                false -> RedVibrant.copy(alpha = 0.6f)
                                null  -> TextSecondary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = device.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = device.node,
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Compact, non-overlapping actions: Single Refresh + Overflow Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Refresh button (syncs status & R2 endpoints)
                        IconButton(
                            onClick = {
                                if (!isCheckingStatus) {
                                    onRefreshStatus()
                                    onSyncConfig()
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isCheckingStatus) GreenVibrant.copy(alpha = 0.15f)
                                    else BorderSubtle.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    if (isCheckingStatus) GreenVibrant.copy(alpha = 0.4f)
                                    else BorderSubtle,
                                    RoundedCornerShape(9.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh server",
                                tint = if (isCheckingStatus) GreenVibrant else TextSecondary,
                                modifier = Modifier
                                    .size(17.dp)
                                    .rotate(if (isCheckingStatus) spinAngle else 0f)
                            )
                        }

                        // More Options Overflow (Edit / Delete)
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(BorderSubtle.copy(alpha = 0.25f))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(9.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Device options",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier
                                    .background(BgElevated)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Device", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showEditDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove Device", color = RedVibrant) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = RedVibrant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── STATUS ROW ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedContent(
                            targetState = Triple(isOnline, isCheckingStatus, nodeStatus?.statusText),
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                            label = "status_badge"
                        ) { (online, checking, statusTxt) ->
                            val badgeBg     = if (checking) BgElevated else if (online == true) OnlineBg else OfflineBg
                            val badgeBorder = if (checking) BorderDefault else if (online == true) OnlineBorder else OfflineBorder
                            val dotColor    = if (online == true) GreenVibrant else RedVibrant
                            val labelColor  = if (checking) TextSecondary else if (online == true) OnlineText else OfflineText

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(badgeBg)
                                    .border(1.dp, badgeBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (checking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(9.dp),
                                        strokeWidth = 1.5.dp,
                                        color = TextSecondary,
                                        trackColor = Color.Transparent
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (online == true) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .scale(pulseScale)
                                                    .background(dotColor.copy(alpha = 0.25f), CircleShape)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(dotColor, CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = if (checking) "CHECKING" else (statusTxt ?: "UNKNOWN"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = labelColor,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        // IP chip
                        if (!nodeStatus?.ip.isNullOrBlank()) {
                            Text(
                                text = nodeStatus!!.ip!!,
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BgElevated)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // ── UPTIME / LAST SEEN BAR ─────────────────────────────────
                val showUptime = nodeStatus != null && !isCheckingStatus &&
                    isOnline == true && !nodeStatus.uptime.isNullOrBlank()
                val showOffline = nodeStatus != null && !isCheckingStatus &&
                    isOnline == false && !nodeStatus.lastActiveAgo.isNullOrBlank()

                AnimatedVisibility(visible = showUptime || showOffline) {
                    val barOnline  = isOnline == true
                    val barBg      = if (barOnline) OnlineBg.copy(alpha = 0.6f) else OfflineBg.copy(alpha = 0.6f)
                    val barBorder  = if (barOnline) OnlineBorder.copy(alpha = 0.5f) else OfflineBorder.copy(alpha = 0.5f)
                    val barColor   = if (barOnline) OnlineText else OfflineText
                    val barLabel   = if (barOnline) "Uptime  ${nodeStatus?.uptime}" else "Last seen  ${nodeStatus?.lastActiveAgo}"
                    val barDetail  = if (barOnline) nodeStatus?.onSinceIst else nodeStatus?.lastActiveIst

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(barBg)
                            .border(1.dp, barBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            tint = barColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = barLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = barColor
                        )
                        if (!barDetail.isNullOrBlank()) {
                            Text(
                                text = "  ·  $barDetail",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── ACTION RESULT BANNER ─────────────────────────────────────────
        StatusBanner(
            status = lastStatus,
            isSuccess = lastSuccess,
            onDismiss = onDismissStatus
        )

        // ── ACTION BUTTONS ───────────────────────────────────────────────
        val isPowerOnEnabled  = isOnline != true  && !isLoading
        val isRebootEnabled   = isOnline != false && !isLoading
        val isPowerOffEnabled = isOnline != false && !isLoading

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            HugeActionButton(
                text = "Power On",
                subtext = if (isOnline == true) "Already online — unavailable" else "Send wake signal to boot",
                gradientColors = PowerOnGradient,
                borderColor = PowerOnBorder,
                icon = Icons.Default.PowerSettingsNew,
                isLoading = isLoading,
                enabled = isPowerOnEnabled,
                onClick = { pendingAction = TriggerAction.POWER_ON }
            )
            HugeActionButton(
                text = "Reboot",
                subtext = if (isOnline == false) "Offline — unavailable" else "Gracefully restart server",
                gradientColors = RebootGradient,
                borderColor = RebootBorder,
                icon = Icons.Default.RestartAlt,
                isLoading = isLoading,
                enabled = isRebootEnabled,
                onClick = { pendingAction = TriggerAction.REBOOT }
            )
            HugeActionButton(
                text = "Power Off",
                subtext = if (isOnline == false) "Offline — unavailable" else "Shut down remote machine",
                gradientColors = PowerOffGradient,
                borderColor = PowerOffBorder,
                icon = Icons.Default.PowerOff,
                isLoading = isLoading,
                enabled = isPowerOffEnabled,
                onClick = { pendingAction = TriggerAction.POWER_OFF }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
