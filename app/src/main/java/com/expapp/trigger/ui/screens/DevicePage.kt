package com.expapp.trigger.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.SysPulseStatus
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.SysPulseClient
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.components.AccentColorDialog
import com.expapp.trigger.ui.components.AccentColorPickerRow
import com.expapp.trigger.ui.components.ActionConfirmDialog
import com.expapp.trigger.ui.components.HugeActionButton
import com.expapp.trigger.ui.components.StatusBanner
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.MonoFontFamily
import com.expapp.trigger.ui.theme.PixelSemantic
import com.expapp.trigger.ui.theme.ServerAccents

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
    onOpenSysPulse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dark = LocalIsDarkTheme.current
    val serverAccent = ServerAccents.parseColor(device.accentColorHex)

    var pendingAction by remember { mutableStateOf<TriggerAction?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showAccentDialog) {
        AccentColorDialog(
            serverName = device.name,
            selectedHex = device.accentColorHex,
            onSelectHex = { hex ->
                onUpdateDevice(device.copy(accentColorHex = hex))
            },
            onDismiss = { showAccentDialog = false }
        )
    }

    if (showEditDialog) {
        var editName by remember(device.id) { mutableStateOf(device.name) }
        var editNode by remember(device.id) { mutableStateOf(device.node) }
        var editR2Url by remember(device.id) { mutableStateOf(device.r2Url) }
        var editSecretKey by remember(device.id) { mutableStateOf(device.secretKey) }
        var editSyspulseUrl by remember(device.id) { mutableStateOf(device.syspulseUrl) }
        var editSyspulseSecret by remember(device.id) { mutableStateOf(device.syspulseSecretKey) }
        var editAccentHex by remember(device.id) { mutableStateOf(device.accentColorHex) }
        var showKey by remember { mutableStateOf(false) }

        val editFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    "Edit Server",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Server Name") },
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNode,
                        onValueChange = { editNode = it },
                        label = { Text("Node Identifier") },
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editR2Url,
                        onValueChange = { editR2Url = it },
                        label = { Text("Cloudflare R2 JSON URL") },
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(16.dp),
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
                                    null
                                )
                            }
                        },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSyspulseUrl,
                        onValueChange = { editSyspulseUrl = it },
                        label = { Text("SysPulse URL (e.g. https://pulse.linuxguy.space)") },
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSyspulseSecret,
                        onValueChange = { editSyspulseSecret = it },
                        label = { Text("SysPulse Secret Key (optional)") },
                        singleLine = true,
                        colors = editFieldColors,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Page Accent Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AccentColorPickerRow(
                        selectedHex = editAccentHex,
                        onSelectHex = { editAccentHex = it }
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
                            secretKey = editSecretKey.trim(),
                            syspulseUrl = editSyspulseUrl.trim(),
                            syspulseSecretKey = editSyspulseSecret.trim(),
                            accentColorHex = editAccentHex
                        )
                        onUpdateDevice(updated)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    "Remove Server",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Remove \"${device.name}\" from your saved servers?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete(device) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isOnline = nodeStatus?.isOnline
    val scrollState = rememberScrollState()

    var glanceStatus by remember(device.id) { mutableStateOf<SysPulseStatus?>(null) }
    var glanceLoading by remember(device.id) { mutableStateOf(false) }

    LaunchedEffect(device.id, device.syspulseUrl, device.syspulseSecretKey, isOnline) {
        if (isOnline == true) {
            glanceLoading = true
            SysPulseClient.shared.fetchStatus(device, nodeStatus) { res ->
                if (res.isSuccess) {
                    glanceStatus = res.getOrNull()
                }
                glanceLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── PIXEL SERVER HERO CARD ───────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val metaLine = buildString {
                            append(device.node)
                            if (!nodeStatus?.ip.isNullOrBlank()) {
                                append(" • ")
                                append(nodeStatus!!.ip)
                            }
                        }
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 1-tap Theme Seed Color Swatch Button
                    IconButton(onClick = { showAccentDialog = true }) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(serverAccent)
                        )
                    }

                    // Overflow menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Server options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh Status") },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                onClick = {
                                    showMenu = false
                                    onRefreshStatus()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sync from R2") },
                                leadingIcon = { Icon(Icons.Default.CloudSync, null) },
                                onClick = {
                                    showMenu = false
                                    onSyncConfig()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Theme Color") },
                                leadingIcon = { Icon(Icons.Default.Palette, null, tint = serverAccent) },
                                onClick = {
                                    showMenu = false
                                    showAccentDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Server") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Server", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
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

                // Tonal Reachability Pill & Uptime Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedContent(
                        targetState = Triple(isOnline, isCheckingStatus, nodeStatus?.statusText),
                        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                        label = "status_badge"
                    ) { (online, checking, statusTxt) ->
                        val chipBg = when {
                            checking -> MaterialTheme.colorScheme.surfaceContainerHighest
                            online == true -> PixelSemantic.onlineContainer(dark)
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                        val chipFg = when {
                            checking -> MaterialTheme.colorScheme.onSurfaceVariant
                            online == true -> PixelSemantic.onOnlineContainer(dark)
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                        val dotTint = when {
                            checking -> MaterialTheme.colorScheme.onSurfaceVariant
                            online == true -> PixelSemantic.onlineAccent(dark)
                            else -> MaterialTheme.colorScheme.error
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = chipBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (checking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = chipFg
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (online == true) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .scale(pulseScale)
                                                    .clip(CircleShape)
                                                    .background(dotTint.copy(alpha = 0.3f))
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(dotTint)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = if (checking) "Checking" else (statusTxt?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Unknown"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = chipFg
                                )
                            }
                        }
                    }

                    val showUptime = nodeStatus != null && !isCheckingStatus &&
                        isOnline == true && !nodeStatus.uptime.isNullOrBlank()
                    val showOffline = nodeStatus != null && !isCheckingStatus &&
                        isOnline == false && !nodeStatus.lastActiveAgo.isNullOrBlank()

                    AnimatedVisibility(visible = showUptime || showOffline) {
                        val label = if (isOnline == true) {
                            "Up ${nodeStatus?.uptime}"
                        } else {
                            "Seen ${nodeStatus?.lastActiveAgo}"
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── SYSPULSE GLANCE SNAPSHOT CARD ────────────────────────────────
        if (isOnline == true) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenSysPulse),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Live Telemetry",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Open SysPulse",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (glanceLoading && glanceStatus == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Connecting to SysPulse...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (glanceStatus != null) {
                        val st = glanceStatus!!
                        val rootDisk = st.disk.find { it.mount == "/" }
                        val runningServices = st.services.count { it.active }
                        val totalServices = st.services.size

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniMetricChip(
                                label = "CPU",
                                value = "%.1f%%".format(st.cpu.pct),
                                modifier = Modifier.weight(1f)
                            )
                            MiniMetricChip(
                                label = "RAM",
                                value = "%.0f%%".format(st.ram.pct),
                                modifier = Modifier.weight(1f)
                            )
                            MiniMetricChip(
                                label = "DISK",
                                value = "%.0f%%".format(rootDisk?.pct ?: 0.0),
                                modifier = Modifier.weight(1f)
                            )
                            MiniMetricChip(
                                label = "SVCS",
                                value = "$runningServices/$totalServices",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = "Tap to inspect hardware metrics & manage services",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

        // ── MATERIAL YOU POWER ACTION TILES ──────────────────────────────
        val isPowerOnEnabled = isOnline != true && !isLoading
        val isRebootEnabled = isOnline != false && !isLoading
        val isPowerOffEnabled = isOnline != false && !isLoading

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Power Controls",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            HugeActionButton(
                text = "Power On",
                subtext = if (isOnline == true) "Server is already online" else "Send wake signal to boot host",
                icon = Icons.Default.PowerSettingsNew,
                isLoading = isLoading,
                enabled = isPowerOnEnabled,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                iconBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                onClick = { pendingAction = TriggerAction.POWER_ON }
            )
            HugeActionButton(
                text = "Reboot",
                subtext = if (isOnline == false) "Unavailable while offline" else "Gracefully restart the server",
                icon = Icons.Default.RestartAlt,
                isLoading = isLoading,
                enabled = isRebootEnabled,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                iconBackgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                onClick = { pendingAction = TriggerAction.REBOOT }
            )
            HugeActionButton(
                text = "Power Off",
                subtext = if (isOnline == false) "Unavailable while offline" else "Shut down the remote machine",
                icon = Icons.Default.PowerOff,
                isLoading = isLoading,
                enabled = isPowerOffEnabled,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                iconBackgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                onClick = { pendingAction = TriggerAction.POWER_OFF }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MiniMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
