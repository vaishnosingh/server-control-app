package com.expapp.trigger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    size: Int = 110
) {
    val safePct = if (percentage.isNaN() || percentage.isInfinite()) 0.0 else percentage
    val clampedPct = safePct.coerceIn(0.0, 100.0).toFloat()
    val animatedSweep by animateFloatAsState(
        targetValue = clampedPct * 2.6f, // 260 degrees arc
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "gauge_sweep"
    )

    val gaugeColor by animateColorAsState(
        targetValue = when {
            clampedPct >= 85f -> RedVibrant
            clampedPct >= 65f -> AmberVibrant
            else -> GreenVibrant
        },
        animationSpec = tween(500),
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
            Canvas(modifier = Modifier.size((size - 10).dp)) {
                val strokeW = 10.dp.toPx()
                // Background Track
                drawArc(
                    color = Color(0xFF26262B),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                // Foreground Progress Arc
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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = gaugeColor,
                    letterSpacing = 0.5.sp
                )
            }
        }
        if (sublabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sublabel,
                fontSize = 11.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CpuCard(
    cpu: CpuMetrics,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DeveloperBoard,
                    contentDescription = null,
                    tint = GreenVibrant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CPU LOAD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${cpu.cores} Cores",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgElevated)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircularMetricGauge(
                    percentage = cpu.pct,
                    label = "CPU",
                    sublabel = ""
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "LOAD AVERAGES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val loads = cpu.load.ifEmpty { listOf(0.0, 0.0, 0.0) }
                        LoadChip(label = "1m", value = loads.getOrElse(0) { 0.0 })
                        LoadChip(label = "5m", value = loads.getOrElse(1) { 0.0 })
                        LoadChip(label = "15m", value = loads.getOrElse(2) { 0.0 })
                    }
                }
            }
        }
    }
}

@Composable
fun LoadChip(label: String, value: Double) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BgElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 9.sp, color = TextMuted)
        Text(
            text = String.format(Locale.US, "%.2f", value),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RamCard(
    ram: RamMetrics,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = AmberVibrant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MEMORY (RAM)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = String.format(Locale.US, "%.1f%%", ram.pct),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ram.pct > 80) RedVibrant else AmberVibrant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val usedGb = ram.usedMb / 1024.0
            val totalGb = ram.totalMb / 1024.0
            val freeGb = (ram.totalMb - ram.usedMb).coerceAtLeast(0) / 1024.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Used: %.2f GB", usedGb),
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(Locale.US, "Total: %.2f GB", totalGb),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progress = (ram.pct / 100.0).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress > 0.85f) RedVibrant else AmberVibrant,
                trackColor = BgElevated
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = String.format(Locale.US, "Free Memory: %.2f GB", freeGb),
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun DisksCard(
    disks: List<DiskMetrics>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STORAGE & DISK MOUNTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${disks.size} Mounts",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgElevated.copy(alpha = 0.6f))
            .border(1.dp, BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = disk.mount,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "%.1f%%", disk.pct),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (disk.pct > 85) RedVibrant else Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val progress = (disk.pct / 100.0).coerceIn(0.0, 1.0).toFloat()
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (progress > 0.85f) RedVibrant else Color(0xFF38BDF8),
            trackColor = BgSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "Used: %.1f GB", disk.usedGb),
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "Total: %.1f GB", disk.totalGb),
                fontSize = 10.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ServiceCard(
    service: SysServiceInfo,
    onOperate: (SysServiceInfo, ServiceOperation) -> Unit,
    onSchedule: (SysServiceInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = service.active && service.state.equals("running", ignoreCase = true)
    val isExited = service.state.equals("exited", ignoreCase = true)

    val stateBadgeColor = when {
        isRunning -> GreenVibrant
        isExited -> AmberVibrant
        else -> RedVibrant
    }

    val stateBadgeBg = when {
        isRunning -> GreenVibrant.copy(alpha = 0.12f)
        isExited -> AmberVibrant.copy(alpha = 0.12f)
        else -> RedVibrant.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Name, PID & State Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = service.name.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        if (service.pid != null && service.pid > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PID ${service.pid}",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BgElevated)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = service.unit,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // State Pill Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(stateBadgeBg)
                        .border(1.dp, stateBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(stateBadgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = service.state.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateBadgeColor
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRunning) {
                    // Start Button
                    ServiceActionButton(
                        label = "START",
                        icon = Icons.Default.PlayArrow,
                        color = GreenVibrant,
                        onClick = { onOperate(service, ServiceOperation.START) }
                    )
                } else {
                    // Stop Button
                    ServiceActionButton(
                        label = "STOP",
                        icon = Icons.Default.Stop,
                        color = RedVibrant,
                        onClick = { onOperate(service, ServiceOperation.STOP) }
                    )
                }

                // Restart Button
                ServiceActionButton(
                    label = "RESTART",
                    icon = Icons.Default.RestartAlt,
                    color = AmberVibrant,
                    onClick = { onOperate(service, ServiceOperation.RESTART) }
                )

                // Schedule Timer Button
                ServiceActionButton(
                    label = "TIMER",
                    icon = Icons.Default.MoreTime,
                    color = Color(0xFF38BDF8),
                    onClick = { onSchedule(service) }
                )
            }
        }
    }
}

@Composable
fun ServiceActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
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
                    modifier = Modifier.size(20.dp)
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

@Composable
fun ScheduleServiceDialog(
    service: SysServiceInfo,
    onConfirm: (operation: ServiceOperation, delaySeconds: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOp by remember {
        mutableStateOf(if (service.active) ServiceOperation.STOP else ServiceOperation.START)
    }
    var selectedMinutes by remember { mutableStateOf(5L) }
    var customMinutesText by remember { mutableStateOf("") }

    val presetMinutes = listOf(1L, 5L, 15L, 30L, 60L)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Schedule ${service.name.uppercase()}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Automatically execute operation after a scheduled delay:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                // Operation Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ServiceOperation.START, ServiceOperation.STOP).forEach { op ->
                        val isSelected = selectedOp == op
                        val opColor = if (op == ServiceOperation.START) GreenVibrant else RedVibrant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) opColor.copy(alpha = 0.2f) else BgElevated)
                                .border(1.dp, if (isSelected) opColor else BorderSubtle, RoundedCornerShape(10.dp))
                                .clickable { selectedOp = op }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = op.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) opColor else TextSecondary
                            )
                        }
                    }
                }

                Text(
                    text = "SELECT DELAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.forEach { mins ->
                        val isSelected = selectedMinutes == mins && customMinutesText.isBlank()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else BgElevated)
                                .border(1.dp, if (isSelected) Color(0xFF38BDF8) else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedMinutes = mins
                                    customMinutesText = ""
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (mins >= 60) "${mins / 60}h" else "${mins}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF38BDF8) else TextSecondary
                            )
                        }
                    }
                }

                // Custom Minutes Input
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = {
                        customMinutesText = it.filter { char -> char.isDigit() }
                        it.toLongOrNull()?.let { m -> selectedMinutes = m }
                    },
                    label = { Text("Or custom minutes") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = BorderDefault,
                        cursorColor = Color(0xFF38BDF8),
                        focusedLabelColor = Color(0xFF38BDF8),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSeconds = selectedMinutes * 60
                    onConfirm(selectedOp, finalSeconds)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8).copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Schedule in ${selectedMinutes}m",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
