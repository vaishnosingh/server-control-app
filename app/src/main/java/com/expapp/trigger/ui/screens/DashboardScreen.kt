package com.expapp.trigger.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.ScheduledTask
import com.expapp.trigger.data.model.ServiceOperation
import com.expapp.trigger.data.model.SysPulseStatus
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.SysPulseClient
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.components.ComputeOverviewGrid
import com.expapp.trigger.ui.components.DisksCard
import com.expapp.trigger.ui.components.ScheduledTaskCard
import com.expapp.trigger.ui.components.ServiceDetailView
import com.expapp.trigger.ui.components.ServiceListRow
import com.expapp.trigger.ui.theme.AmberVibrant
import com.expapp.trigger.ui.theme.BgCard
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.RedVibrant
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun DashboardScreen(
    device: DeviceConfig,
    nodeStatus: NodeStatus?,
    accentColor: Color = GreenVibrant,
    onBack: () -> Unit,
    onTriggerPowerAction: ((TriggerAction) -> Unit)? = null,
    onRequireBiometric: ((title: String, subtitle: String, onAuthorized: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sysPulseClient = remember { SysPulseClient() }

    val effectiveUrl = remember(device, nodeStatus) {
        sysPulseClient.resolveBaseUrl(device, nodeStatus)
    }
    val effectiveSecret = remember(device) {
        sysPulseClient.resolveSecretKey(device)
    }

    var pulseStatus by remember { mutableStateOf<SysPulseStatus?>(null) }
    var scheduledTasks by remember { mutableStateOf<List<ScheduledTask>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isPollingActive by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedServiceName by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, RUNNING, STOPPED, BOOT ON, BOOT OFF

    fun refreshData(showIndicator: Boolean = false) {
        if (effectiveUrl.isBlank()) {
            isLoading = false
            errorMessage = "No SysPulse API endpoint configured or detected for this server."
            return
        }
        if (showIndicator) isLoading = true

        sysPulseClient.fetchStatus(effectiveUrl, effectiveSecret) { result ->
            isLoading = false
            result.onSuccess {
                pulseStatus = it
                errorMessage = null
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Unable to connect to SysPulse at $effectiveUrl"
            }
        }

        sysPulseClient.fetchTasks(effectiveUrl, effectiveSecret) { result ->
            result.onSuccess { allTasks ->
                scheduledTasks = allTasks.filter {
                    it.status.equals("pending", ignoreCase = true) ||
                        it.status.equals("active", ignoreCase = true) ||
                        it.status.equals("scheduled", ignoreCase = true)
                }
            }
        }
    }

    // Active polling loop every 3 seconds while screen is displayed
    LaunchedEffect(effectiveUrl, effectiveSecret, isPollingActive) {
        if (effectiveUrl.isBlank()) return@LaunchedEffect
        while (isActive && isPollingActive) {
            refreshData(showIndicator = false)
            delay(3000L)
        }
    }

    fun handleServiceOperation(
        service: com.expapp.trigger.data.model.SysServiceInfo,
        op: ServiceOperation,
        delaySec: Long? = null
    ) {
        val executeCall = {
            val delayStr = if (delaySec != null && delaySec > 0) {
                if (delaySec < 60) " in ${delaySec}s" else " in ${delaySec / 60}m"
            } else ""
            Toast.makeText(context, "Executing ${op.displayName} on ${service.name}$delayStr...", Toast.LENGTH_SHORT).show()

            sysPulseClient.operateService(
                baseUrl = effectiveUrl,
                secretKey = effectiveSecret,
                serviceName = service.name,
                operation = op,
                delaySeconds = delaySec
            ) { result ->
                result.onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    refreshData(showIndicator = false)
                }.onFailure { err ->
                    Toast.makeText(context, err.localizedMessage ?: "Operation failed", Toast.LENGTH_LONG).show()
                }
            }
        }

        if (op == ServiceOperation.STOP && onRequireBiometric != null) {
            onRequireBiometric("Confirm Stop ${service.name.uppercase()}", "Authenticate to stop ${service.unit}") {
                executeCall()
            }
        } else {
            executeCall()
        }
    }

    val activeSelectedService = remember(selectedServiceName, pulseStatus) {
        val target = selectedServiceName
        if (target == null) null
        else pulseStatus?.services?.find {
            it.name.equals(target, ignoreCase = true) || it.unit.equals(target, ignoreCase = true)
        }
    }

    // Back gesture handling: from ServiceDetailView back to SysPulse, or SysPulse back to Power Control
    BackHandler(enabled = true) {
        if (selectedServiceName != null) {
            selectedServiceName = null
        } else {
            onBack()
        }
    }

    if (activeSelectedService != null) {
        ServiceDetailView(
            service = activeSelectedService,
            serverName = device.name,
            accentColor = accentColor,
            isLoading = isLoading,
            serviceTasks = scheduledTasks.filter {
                it.serviceName.equals(activeSelectedService.name, ignoreCase = true)
            },
            onBack = { selectedServiceName = null },
            onRefresh = { refreshData(showIndicator = true) },
            onOperate = { s, op, delaySec -> handleServiceOperation(s, op, delaySec) },
            onCancelTask = { t ->
                sysPulseClient.cancelTask(effectiveUrl, effectiveSecret, t.taskId) { res ->
                    res.onSuccess { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        refreshData(false)
                    }.onFailure { err ->
                        Toast.makeText(context, err.localizedMessage ?: "Cancel failed", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = modifier
        )
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSurface)
    ) {
        // ── TOP APP BAR ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "${device.name} • SYSPULSE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.6.sp
                    )
                }
                Text(
                    text = if (effectiveUrl.isNotBlank()) effectiveUrl else "SysPulse API Offline",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            // Live Polling Toggle Pill
            IconButton(
                onClick = { isPollingActive = !isPollingActive },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isPollingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPollingActive) "Pause Polling" else "Resume Polling",
                    tint = if (isPollingActive) accentColor else TextMuted,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Manual Refresh Button
            IconButton(
                onClick = { refreshData(showIndicator = true) },
                modifier = Modifier.size(34.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // ── DASHBOARD BODY ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── HOST UPTIME & POLLING STRIP ──────────────────────────────
            val status = pulseStatus
            if (status != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UPTIME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = status.uptime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val isPulseError = errorMessage != null
                    Text(
                        text = if (!isPollingActive) "PAUSED" else if (isPulseError) "RETRYING" else "LIVE 3s",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isPollingActive) AmberVibrant else if (isPulseError) RedVibrant else accentColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Offline / Unreachable State
            if (status == null && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = RedVibrant,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "SysPulse Daemon Unreachable",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = errorMessage ?: "The server may be offline or SysPulse is not running at $effectiveUrl.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    if (onTriggerPowerAction != null && nodeStatus?.isOnline != true) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onTriggerPowerAction(TriggerAction.POWER_ON) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenVibrant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wake Server Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── HARDWARE METRICS (2-COLUMN CPU/RAM + DISKS) ──────────────
            if (status != null) {
                ComputeOverviewGrid(cpu = status.cpu, ram = status.ram)

                if (status.disk.isNotEmpty()) {
                    DisksCard(disks = status.disk)
                }
            }

            // ── PENDING SCHEDULED TASKS ──────────────────────────────────
            if (scheduledTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AmberVibrant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PENDING TASKS (${scheduledTasks.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    scheduledTasks.forEach { task ->
                        ScheduledTaskCard(
                            task = task,
                            onCancel = { t ->
                                sysPulseClient.cancelTask(effectiveUrl, effectiveSecret, t.taskId) { res ->
                                    res.onSuccess { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        refreshData(false)
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.localizedMessage ?: "Cancel failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // ── CLEAN SERVICE DIRECTORY (NO CLUTTERED INLINE BUTTONS) ────
            if (status != null && status.services.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SERVICES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        val runningCount = status.services.count { it.active }
                        val bootCount = status.services.count { it.enabled }
                        Text(
                            text = "$runningCount/${status.services.size} active  ·  $bootCount boot",
                            fontSize = 11.sp,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search services or systemd unit...", fontSize = 12.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = BorderSubtle,
                            cursorColor = accentColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL", "RUNNING", "STOPPED", "BOOT ON", "BOOT OFF").forEach { mode ->
                            val isSelected = filterMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.16f) else BgElevated)
                                    .border(1.dp, if (isSelected) accentColor else BorderSubtle, RoundedCornerShape(9.dp))
                                    .clickable { filterMode = mode }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) accentColor else TextSecondary
                                )
                            }
                        }
                    }

                    val filteredServices = status.services.filter { s ->
                        val matchesSearch = searchQuery.isBlank() ||
                            s.name.contains(searchQuery, ignoreCase = true) ||
                            s.unit.contains(searchQuery, ignoreCase = true)
                        val matchesMode = when (filterMode) {
                            "RUNNING" -> s.active
                            "STOPPED" -> !s.active
                            "BOOT ON" -> s.enabled
                            "BOOT OFF" -> !s.enabled
                            else -> true
                        }
                        matchesSearch && matchesMode
                    }

                    if (filteredServices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No services matching current filter",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        // Single cohesive enterprise container with clean dividers & zero inline clutter
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BgCard)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            filteredServices.forEachIndexed { index, service ->
                                ServiceListRow(
                                    service = service,
                                    accentColor = accentColor,
                                    onOpenService = { s -> selectedServiceName = s.name }
                                )
                                if (index < filteredServices.lastIndex) {
                                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
