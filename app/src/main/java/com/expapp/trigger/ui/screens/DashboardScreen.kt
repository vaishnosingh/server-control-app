package com.expapp.trigger.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.ScheduledTask
import com.expapp.trigger.data.model.ServiceOperation
import com.expapp.trigger.data.model.SysPulseStatus
import com.expapp.trigger.data.model.SysServiceInfo
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.SysPulseClient
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.components.ComputeOverviewGrid
import com.expapp.trigger.ui.components.DisksCard
import com.expapp.trigger.ui.components.ScheduledTaskCard
import com.expapp.trigger.ui.components.ServiceDetailView
import com.expapp.trigger.ui.components.ServiceListRow
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.MonoFontFamily
import com.expapp.trigger.ui.theme.PixelSemantic
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun DashboardScreen(
    device: DeviceConfig,
    nodeStatus: NodeStatus?,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showBackButton: Boolean = true,
    onBack: () -> Unit = {},
    onTriggerPowerAction: ((TriggerAction) -> Unit)? = null,
    onRequireBiometric: ((title: String, subtitle: String, onAuthorized: () -> Unit) -> Unit)? = null,
    onServiceDetailVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dark = LocalIsDarkTheme.current
    val sysPulseClient = remember { SysPulseClient() }

    val effectiveUrl = remember(device, nodeStatus) {
        sysPulseClient.resolveBaseUrl(device, nodeStatus)
    }
    val effectiveSecret = remember(device) {
        sysPulseClient.resolveSecretKey(device)
    }

    var pulseStatus by remember(device.id) { mutableStateOf<SysPulseStatus?>(null) }
    var scheduledTasks by remember(device.id) { mutableStateOf<List<ScheduledTask>>(emptyList()) }
    var isLoading by remember(device.id) { mutableStateOf(true) }
    var isPollingActive by remember { mutableStateOf(true) }
    var errorMessage by remember(device.id) { mutableStateOf<String?>(null) }

    var selectedServiceName by remember(device.id) { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") }

    LaunchedEffect(selectedServiceName) {
        onServiceDetailVisibilityChanged(selectedServiceName != null)
    }

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

    LaunchedEffect(effectiveUrl, effectiveSecret, isPollingActive) {
        if (effectiveUrl.isBlank()) return@LaunchedEffect
        while (isActive && isPollingActive) {
            refreshData(showIndicator = false)
            delay(3000L)
        }
    }

    fun handleServiceOperation(
        service: SysServiceInfo,
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

    BackHandler(enabled = selectedServiceName != null || showBackButton) {
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
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (showBackButton) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${device.name} • SysPulse",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (effectiveUrl.isNotBlank()) effectiveUrl else "SysPulse offline",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                FilledTonalIconButton(onClick = { isPollingActive = !isPollingActive }) {
                    Icon(
                        imageVector = if (isPollingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPollingActive) "Pause Polling" else "Resume Polling",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { refreshData(showIndicator = true) }) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Host Uptime & Live Poll Strip
            val status = pulseStatus
            if (status != null) {
                val isPulseError = errorMessage != null
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(PixelSemantic.onlineAccent(dark))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Up ${status.uptime}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = effectiveUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    !isPollingActive -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    isPulseError -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isPollingActive = !isPollingActive }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPollingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = when {
                                            !isPollingActive -> MaterialTheme.colorScheme.onSurfaceVariant
                                            isPulseError -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (!isPollingActive) "Paused" else if (isPulseError) "Retrying" else "Live 3s",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when {
                                            !isPollingActive -> MaterialTheme.colorScheme.onSurfaceVariant
                                            isPulseError -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }
                            }

                            if (!showBackButton) {
                                IconButton(
                                    onClick = { refreshData(showIndicator = true) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Unreachable State
            if (status == null && !isLoading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "SysPulse Unreachable",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = errorMessage ?: "Server may be powered off or SysPulse is stopped at $effectiveUrl.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onTriggerPowerAction != null && nodeStatus?.isOnline != true) {
                            Button(
                                onClick = { onTriggerPowerAction(TriggerAction.POWER_ON) },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wake Server Now")
                            }
                        }
                    }
                }
            }

            // Hardware Compute & Storage Cards
            if (status != null) {
                ComputeOverviewGrid(cpu = status.cpu, ram = status.ram)

                if (status.disk.isNotEmpty()) {
                    DisksCard(disks = status.disk)
                }
            }

            // Pending Tasks
            if (scheduledTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Scheduled Tasks (${scheduledTasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
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

            // Services Directory (Android Settings-style Grouped List)
            if (status != null && status.services.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Services",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val runningCount = status.services.count { it.active }
                        val bootCount = status.services.count { it.enabled }
                        Text(
                            text = "$runningCount/${status.services.size} active • $bootCount boot",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Pill Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search services or systemd unit") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "RUNNING" to "Running",
                            "STOPPED" to "Stopped",
                            "BOOT ON" to "Boot On",
                            "BOOT OFF" to "Boot Off"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = filterMode == key,
                                onClick = { filterMode = key },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
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
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No services match the current filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column {
                                filteredServices.forEachIndexed { index, service ->
                                    ServiceListRow(
                                        service = service,
                                        onOpenService = { s -> selectedServiceName = s.name }
                                    )
                                    if (index < filteredServices.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
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
