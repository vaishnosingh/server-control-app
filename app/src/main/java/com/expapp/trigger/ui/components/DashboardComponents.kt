package com.expapp.trigger.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expapp.trigger.data.model.CpuMetrics
import com.expapp.trigger.data.model.DiskMetrics
import com.expapp.trigger.data.model.RamMetrics
import com.expapp.trigger.data.model.ScheduledTask
import com.expapp.trigger.data.model.ServiceOperation
import com.expapp.trigger.data.model.SysServiceInfo
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.MonoFontFamily
import com.expapp.trigger.ui.theme.PixelSemantic
import java.util.Locale

@Composable
fun CircularMetricGauge(
    percentage: Double,
    label: String,
    sublabel: String,
    modifier: Modifier = Modifier,
    size: Int = 96
) {
    val dark = LocalIsDarkTheme.current
    val safePct = if (percentage.isNaN() || percentage.isInfinite()) 0.0 else percentage
    val clampedPct = safePct.coerceIn(0.0, 100.0).toFloat()
    val animatedSweep by animateFloatAsState(
        targetValue = clampedPct * 2.6f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "gauge_sweep"
    )

    val gaugeColor by animateColorAsState(
        targetValue = when {
            clampedPct >= 85f -> MaterialTheme.colorScheme.error
            clampedPct >= 65f -> PixelSemantic.warningAccent(dark)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(450),
        label = "gauge_color"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size((size - 8).dp)) {
                val strokeW = 9.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = gaugeColor,
                    startAngle = 140f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%.0f%%", clampedPct),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = gaugeColor
                )
            }
        }
        if (sublabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sublabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = MonoFontFamily,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Side-by-side Material You tonal Compute Overview grid (CPU & Memory).
 */
@Composable
fun ComputeOverviewGrid(
    cpu: CpuMetrics,
    ram: RamMetrics,
    modifier: Modifier = Modifier
) {
    val safeRamPct = if (ram.pct.isNaN() || ram.pct.isInfinite()) 0.0 else ram.pct
    val usedGb = ram.usedMb / 1024.0
    val totalGb = ram.totalMb / 1024.0
    val freeGb = (ram.totalMb - ram.usedMb).coerceAtLeast(0) / 1024.0
    val loads = cpu.load.ifEmpty { listOf(0.0, 0.0, 0.0) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DeveloperBoard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CPU",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${cpu.cores} cores",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                CircularMetricGauge(
                    percentage = cpu.pct,
                    label = String.format(Locale.US, "%.1f%%", cpu.pct.coerceIn(0.0, 100.0)),
                    sublabel = String.format(
                        Locale.US,
                        "%.2f · %.2f · %.2f",
                        loads.getOrElse(0) { 0.0 },
                        loads.getOrElse(1) { 0.0 },
                        loads.getOrElse(2) { 0.0 }
                    )
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Memory",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.1fG free", freeGb),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                CircularMetricGauge(
                    percentage = safeRamPct,
                    label = String.format(Locale.US, "%.1f%%", safeRamPct.coerceIn(0.0, 100.0)),
                    sublabel = String.format(Locale.US, "%.1f / %.1f GB", usedGb, totalGb)
                )
            }
        }
    }
}

@Composable
fun DisksCard(
    disks: List<DiskMetrics>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Storage Volumes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${disks.size} mounts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            disks.forEach { disk ->
                DiskMountRow(disk = disk)
            }
        }
    }
}

@Composable
fun DiskMountRow(disk: DiskMetrics) {
    val safePct = if (disk.pct.isNaN() || disk.pct.isInfinite()) 0.0 else disk.pct
    val progress = (safePct / 100.0).coerceIn(0.0, 1.0).toFloat()
    val barColor = if (progress > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = disk.mount,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = String.format(Locale.US, "%.1f / %.1f GB (%.0f%%)", disk.usedGb, disk.totalGb, safePct),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = MonoFontFamily,
                    fontFeatureSettings = "tnum"
                ),
                color = if (progress > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
    }
}

/**
 * Clean Pixel Settings-style grouped service row — zero inline button clutter.
 */
@Composable
fun ServiceListRow(
    service: SysServiceInfo,
    onOpenService: (SysServiceInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsDarkTheme.current
    val isRunning = service.active && service.state.equals("running", ignoreCase = true)
    val isExited = service.state.equals("exited", ignoreCase = true)

    val dotContainer = when {
        isRunning -> PixelSemantic.onlineContainer(dark)
        isExited -> PixelSemantic.warningContainer(dark)
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val dotTint = when {
        isRunning -> PixelSemantic.onlineAccent(dark)
        isExited -> PixelSemantic.warningAccent(dark)
        else -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenService(service) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(dotContainer),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotTint)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (service.pid != null && service.pid > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "PID ${service.pid}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = service.unit,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = service.state.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = dotTint
            )
            Text(
                text = if (service.enabled) "Boot On" else "Boot Off",
                style = MaterialTheme.typography.labelSmall,
                color = if (service.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Manage ${service.name}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Dedicated Material You Service Management screen when tapping any service row.
 */
@Composable
fun ServiceDetailView(
    service: SysServiceInfo,
    serverName: String,
    isLoading: Boolean,
    serviceTasks: List<ScheduledTask>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOperate: (SysServiceInfo, ServiceOperation, Long?) -> Unit,
    onCancelTask: (ScheduledTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsDarkTheme.current
    val isRunning = service.active && service.state.equals("running", ignoreCase = true)
    val isExited = service.state.equals("exited", ignoreCase = true)

    val stateContainer = when {
        isRunning -> PixelSemantic.onlineContainer(dark)
        isExited -> PixelSemantic.warningContainer(dark)
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val stateText = when {
        isRunning -> PixelSemantic.onOnlineContainer(dark)
        isExited -> PixelSemantic.onWarningContainer(dark)
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    var selectedDelayOp by remember(service.name) {
        mutableStateOf(if (service.active) ServiceOperation.STOP else ServiceOperation.START)
    }
    var selectedDelaySeconds by remember { mutableStateOf(60L) }
    var customMinutesText by remember { mutableStateOf("") }
    val presetSeconds = listOf(30L to "30s", 60L to "1m", 300L to "5m", 900L to "15m", 1800L to "30m", 3600L to "1h")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Pixel Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Services",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$serverName • ${service.unit}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRefresh) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Identity Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = service.unit,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = stateContainer
                        ) {
                            Text(
                                text = service.state.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = stateText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (service.enabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ) {
                            Text(
                                text = if (service.enabled) "Enabled on Boot" else "Disabled on Boot",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (service.enabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        if (service.pid != null && service.pid > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = "PID ${service.pid}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Runtime Lifecycle Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Runtime Controls",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ServiceControlTile(
                        label = "Start",
                        icon = Icons.Default.PlayArrow,
                        containerColor = PixelSemantic.onlineContainer(dark),
                        contentColor = PixelSemantic.onOnlineContainer(dark),
                        enabled = !isRunning,
                        onClick = { onOperate(service, ServiceOperation.START, null) },
                        modifier = Modifier.weight(1f)
                    )
                    ServiceControlTile(
                        label = "Stop",
                        icon = Icons.Default.Stop,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        enabled = isRunning || service.active,
                        onClick = { onOperate(service, ServiceOperation.STOP, null) },
                        modifier = Modifier.weight(1f)
                    )
                    ServiceControlTile(
                        label = "Restart",
                        icon = Icons.Default.RestartAlt,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        enabled = true,
                        onClick = { onOperate(service, ServiceOperation.RESTART, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Boot Persistence Section (Pixel Android Settings Switch Tile + Action Pills)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Startup & Boot (systemd)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start automatically on boot",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (service.enabled) {
                                        "Managed unit launches automatically at host boot"
                                    } else {
                                        "Manual start only — won't launch on host boot"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = service.enabled,
                                onCheckedChange = { enable ->
                                    onOperate(
                                        service,
                                        if (enable) ServiceOperation.ENABLE else ServiceOperation.DISABLE,
                                        null
                                    )
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (service.enabled) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onOperate(service, ServiceOperation.ENABLE, null) },
                                enabled = !service.enabled,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enable on Boot")
                            }

                            FilledTonalButton(
                                onClick = { onOperate(service, ServiceOperation.DISABLE, null) },
                                enabled = service.enabled,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Disable")
                            }
                        }
                    }
                }
            }

            // Pending Scheduled Actions
            if (serviceTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pending Scheduled Actions (${serviceTasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    serviceTasks.forEach { task ->
                        ScheduledTaskCard(task = task, onCancel = onCancelTask)
                    }
                }
            }

            // Schedule Delayed Execution Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Schedule Delayed Action",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ServiceOperation.START,
                                ServiceOperation.STOP,
                                ServiceOperation.RESTART,
                                ServiceOperation.ENABLE,
                                ServiceOperation.DISABLE
                            ).forEach { op ->
                                FilterChip(
                                    selected = selectedDelayOp == op,
                                    onClick = { selectedDelayOp = op },
                                    label = {
                                        Text(
                                            op.name.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetSeconds.forEach { (sec, label) ->
                                FilterChip(
                                    selected = selectedDelaySeconds == sec && customMinutesText.isBlank(),
                                    onClick = {
                                        selectedDelaySeconds = sec
                                        customMinutesText = ""
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = {
                                val clean = it.filter { ch -> ch.isDigit() }
                                customMinutesText = clean
                                clean.toLongOrNull()?.let { m ->
                                    if (m > 0) selectedDelaySeconds = m * 60L
                                }
                            },
                            label = { Text("Custom delay (minutes)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val delayDisplay = if (selectedDelaySeconds < 60) {
                            "${selectedDelaySeconds}s"
                        } else {
                            "${selectedDelaySeconds / 60}m"
                        }
                        Button(
                            onClick = {
                                if (selectedDelaySeconds > 0) {
                                    onOperate(service, selectedDelayOp, selectedDelaySeconds)
                                }
                            },
                            enabled = selectedDelaySeconds > 0,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.MoreTime, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Schedule ${selectedDelayOp.displayName} in $delayDisplay",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ServiceControlTile(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun ScheduledTaskCard(
    task: ScheduledTask,
    onCancel: (ScheduledTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PixelSemantic.warningContainer(dark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = PixelSemantic.onWarningContainer(dark),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${task.operation.replaceFirstChar { it.uppercase() }}: ${task.serviceName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = PixelSemantic.onWarningContainer(dark)
                    )
                    Text(
                        text = "Fires in ${task.delaySeconds}s • ${task.executeAt}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                        color = PixelSemantic.onWarningContainer(dark).copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val isCancellable = task.status.equals("pending", ignoreCase = true) ||
                task.status.equals("active", ignoreCase = true) ||
                task.status.equals("scheduled", ignoreCase = true)

            if (isCancellable) {
                IconButton(onClick = { onCancel(task) }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel Task",
                        tint = PixelSemantic.onWarningContainer(dark)
                    )
                }
            }
        }
    }
}
