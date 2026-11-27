package com.expapp.trigger.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
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
import com.expapp.trigger.ui.theme.AmberVibrant
import com.expapp.trigger.ui.theme.BgCard
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderDefault
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.RedVibrant
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun CircularMetricGauge(
    percentage: Double,
    label: String,
    sublabel: String,
    modifier: Modifier = Modifier,
    size: Int = 88
) {
    val safePct = if (percentage.isNaN() || percentage.isInfinite()) 0.0 else percentage
    val clampedPct = safePct.coerceIn(0.0, 100.0).toFloat()
    val animatedSweep by animateFloatAsState(
        targetValue = clampedPct * 2.6f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "gauge_sweep"
    )

    val gaugeColor by animateColorAsState(
        targetValue = when {
            clampedPct >= 85f -> RedVibrant
            clampedPct >= 65f -> AmberVibrant
            else -> GreenVibrant
        },
        animationSpec = tween(450),
        label = "gauge_color"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size((size - 8).dp)) {
                val strokeW = 7.dp.toPx()
                drawArc(
                    color = Color(0xFF222B3C),
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
                    text = String.format(Locale.US, "%.1f%%", clampedPct),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor,
                    letterSpacing = 0.6.sp
                )
            }
        }
        if (sublabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sublabel,
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Balanced 2-column CPU & RAM executive overview strip for the SysPulse dashboard.
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // CPU Card
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(12.dp),
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
                        tint = GreenVibrant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "CPU",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }
                Text(
                    text = "${cpu.cores}C",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            CircularMetricGauge(
                percentage = cpu.pct,
                label = "LOAD",
                sublabel = String.format(
                    Locale.US,
                    "%.2f · %.2f · %.2f",
                    loads.getOrElse(0) { 0.0 },
                    loads.getOrElse(1) { 0.0 },
                    loads.getOrElse(2) { 0.0 }
                )
            )
        }

        // RAM Card
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(12.dp),
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
                        tint = AmberVibrant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "MEMORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.1fG free", freeGb),
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            CircularMetricGauge(
                percentage = safeRamPct,
                label = "RAM",
                sublabel = String.format(Locale.US, "%.1f / %.1f GB", usedGb, totalGb)
            )
        }
    }
}

@Composable
fun DisksCard(
    disks: List<DiskMetrics>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "STORAGE MOUNTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${disks.size} volumes",
                fontSize = 10.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        disks.forEach { disk ->
            DiskMountRow(disk = disk)
        }
    }
}

@Composable
fun DiskMountRow(disk: DiskMetrics) {
    val safePct = if (disk.pct.isNaN() || disk.pct.isInfinite()) 0.0 else disk.pct
    val progress = (safePct / 100.0).coerceIn(0.0, 1.0).toFloat()
    val barColor = if (progress > 0.85f) RedVibrant else Color(0xFF38BDF8)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = disk.mount,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "%.1f / %.1f GB  (%.0f%%)", disk.usedGb, disk.totalGb, safePct),
                fontSize = 11.sp,
                color = if (progress > 0.85f) RedVibrant else TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = BgElevated
        )
    }
}

/**
 * Clean, zero-clutter service row inside the SysPulse Services list.
 * Displays status telemetry & boot persistence only — tapping opens the dedicated ServiceDetailView.
 */
@Composable
fun ServiceListRow(
    service: SysServiceInfo,
    accentColor: Color,
    onOpenService: (SysServiceInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = service.active && service.state.equals("running", ignoreCase = true)
    val isExited = service.state.equals("exited", ignoreCase = true)
    val dotColor = when {
        isRunning -> GreenVibrant
        isExited -> AmberVibrant
        else -> RedVibrant
    }
    val bootColor = if (service.enabled) Color(0xFF38BDF8) else TextMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenService(service) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // State Dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Service Name & Systemd Unit
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = service.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (service.pid != null && service.pid > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${service.pid}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = service.unit,
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Compact Status & Boot Pills
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = service.state.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = dotColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (service.enabled) "BOOT: ON" else "BOOT: OFF",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = bootColor,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Manage ${service.name}",
            tint = accentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Full-screen dedicated Service Control Workspace rendered when tapping a service row.
 * Houses Runtime Lifecycle controls, systemd Enable/Disable on Boot, and Scheduled Actions.
 */
@Composable
fun ServiceDetailView(
    service: SysServiceInfo,
    serverName: String,
    accentColor: Color,
    isLoading: Boolean,
    serviceTasks: List<ScheduledTask>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOperate: (SysServiceInfo, ServiceOperation, Long?) -> Unit,
    onCancelTask: (ScheduledTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = service.active && service.state.equals("running", ignoreCase = true)
    val isExited = service.state.equals("exited", ignoreCase = true)
    val stateColor = when {
        isRunning -> GreenVibrant
        isExited -> AmberVibrant
        else -> RedVibrant
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
            .background(BgSurface)
    ) {
        // ── SERVICE TOP BAR ──────────────────────────────────────────────
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
                    contentDescription = "Back to Services",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name.uppercase(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "$serverName • ${service.unit}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRefresh,
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── SERVICE IDENTITY & TELEMETRY HERO CARD ───────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgCard)
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.14f))
                                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = service.unit,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // State badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(stateColor.copy(alpha = 0.14f))
                                .border(1.dp, stateColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(stateColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = service.state.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = stateColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Boot badge
                        val bootColor = if (service.enabled) Color(0xFF38BDF8) else AmberVibrant
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bootColor.copy(alpha = 0.12f))
                                .border(1.dp, bootColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (service.enabled) "ENABLED ON BOOT" else "DISABLED ON BOOT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = bootColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (service.pid != null && service.pid > 0) {
                            Text(
                                text = "PID ${service.pid}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BgElevated)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // ── SECTION 1: RUNTIME LIFECYCLE CONTROLS ────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RUNTIME LIFECYCLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServiceControlTile(
                        label = "Start",
                        icon = Icons.Default.PlayArrow,
                        color = GreenVibrant,
                        enabled = !isRunning,
                        onClick = { onOperate(service, ServiceOperation.START, null) },
                        modifier = Modifier.weight(1f)
                    )
                    ServiceControlTile(
                        label = "Stop",
                        icon = Icons.Default.Stop,
                        color = RedVibrant,
                        enabled = isRunning || service.active,
                        onClick = { onOperate(service, ServiceOperation.STOP, null) },
                        modifier = Modifier.weight(1f)
                    )
                    ServiceControlTile(
                        label = "Restart",
                        icon = Icons.Default.RestartAlt,
                        color = AmberVibrant,
                        enabled = true,
                        onClick = { onOperate(service, ServiceOperation.RESTART, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── SECTION 2: BOOT AUTO-START (SYSTEMD ENABLE / DISABLE) ────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "BOOT PERSISTENCE (SYSTEMD)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Start on Boot",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (service.enabled) {
                                    "Unit starts automatically on server startup"
                                } else {
                                    "Manual start only — disabled from boot sequence"
                                },
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = service.enabled,
                            onCheckedChange = { enable ->
                                onOperate(
                                    service,
                                    if (enable) ServiceOperation.ENABLE else ServiceOperation.DISABLE,
                                    null
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF38BDF8),
                                checkedTrackColor = Color(0xFF38BDF8).copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BgElevated
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOperate(service, ServiceOperation.ENABLE, null) },
                            enabled = !service.enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF38BDF8).copy(alpha = 0.16f),
                                disabledContainerColor = BgElevated
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (!service.enabled) Color(0xFF38BDF8) else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!service.enabled) Color(0xFF38BDF8) else TextMuted
                            )
                        }

                        Button(
                            onClick = { onOperate(service, ServiceOperation.DISABLE, null) },
                            enabled = service.enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedVibrant.copy(alpha = 0.15f),
                                disabledContainerColor = BgElevated
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (service.enabled) RedVibrant else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Disable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (service.enabled) RedVibrant else TextMuted
                            )
                        }
                    }
                }
            }

            // ── SECTION 3: PENDING SCHEDULED ACTIONS ─────────────────────
            if (serviceTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PENDING SCHEDULED ACTIONS (${serviceTasks.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberVibrant,
                        letterSpacing = 1.sp
                    )
                    serviceTasks.forEach { task ->
                        ScheduledTaskCard(task = task, onCancel = onCancelTask)
                    }
                }
            }

            // ── SECTION 4: SCHEDULE DELAYED ACTION ───────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SCHEDULE DELAYED EXECUTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            ServiceOperation.START,
                            ServiceOperation.STOP,
                            ServiceOperation.RESTART,
                            ServiceOperation.ENABLE,
                            ServiceOperation.DISABLE
                        ).forEach { op ->
                            val isSelected = selectedDelayOp == op
                            val opColor = when (op) {
                                ServiceOperation.START -> GreenVibrant
                                ServiceOperation.STOP, ServiceOperation.DISABLE -> RedVibrant
                                ServiceOperation.RESTART -> AmberVibrant
                                ServiceOperation.ENABLE -> Color(0xFF38BDF8)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) opColor.copy(alpha = 0.2f) else BgElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) opColor else BorderSubtle,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable { selectedDelayOp = op }
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = op.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) opColor else TextSecondary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetSeconds.forEach { (sec, label) ->
                            val isSelected = selectedDelaySeconds == sec && customMinutesText.isBlank()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else BgElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor else BorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedDelaySeconds = sec
                                        customMinutesText = ""
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) accentColor else TextSecondary
                                )
                            }
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
                        label = { Text("Or custom delay (minutes)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = BorderDefault,
                            cursorColor = accentColor,
                            focusedLabelColor = accentColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.MoreTime,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Schedule ${selectedDelayOp.displayName} in $delayDisplay",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
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
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) color.copy(alpha = 0.12f) else BgCard)
            .border(
                1.dp,
                if (enabled) color.copy(alpha = 0.38f) else BorderSubtle,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) color else TextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) color else TextMuted.copy(alpha = 0.55f)
        )
    }
}

@Composable
fun ScheduledTaskCard(
    task: ScheduledTask,
    onCancel: (ScheduledTask) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgElevated)
            .border(1.dp, AmberVibrant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(12.dp)
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
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AmberVibrant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Scheduled ${task.operation.uppercase()}: ${task.serviceName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fires in ${task.delaySeconds}s • ${task.executeAt}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val isCancellable = task.status.equals("pending", ignoreCase = true) ||
                task.status.equals("active", ignoreCase = true) ||
                task.status.equals("scheduled", ignoreCase = true)

            if (isCancellable) {
                IconButton(
                    onClick = { onCancel(task) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel Task",
                        tint = RedVibrant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
