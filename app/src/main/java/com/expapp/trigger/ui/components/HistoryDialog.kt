package com.expapp.trigger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.expapp.trigger.data.model.TriggerLog
import com.expapp.trigger.network.TriggerAction
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HistoryDialog(
    logs: List<TriggerLog>,
    onClearOlderThan: (durationMillis: Long) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var filterAction by remember { mutableStateOf<TriggerAction?>(null) }
    var filterSuccessOnly by remember { mutableStateOf<Boolean?>(null) }
    var showClearMenu by remember { mutableStateOf(false) }

    val filteredLogs = logs.filter { log ->
        val actionMatches = filterAction == null || log.action == filterAction
        val successMatches = filterSuccessOnly == null || log.isSuccess == filterSuccessOnly
        actionMatches && successMatches
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = BgSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = GreenVibrant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TRIGGER HISTORY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )

                    // Clear options menu button
                    Box {
                        IconButton(
                            onClick = { showClearMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showClearMenu,
                            onDismissRequest = { showClearMenu = false },
                            modifier = Modifier.background(BgElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear older than 24 hours", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showClearMenu = false
                                    onClearOlderThan(TimeUnit.HOURS.toMillis(24))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear older than 7 days", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showClearMenu = false
                                    onClearOlderThan(TimeUnit.DAYS.toMillis(7))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear older than 30 days", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    showClearMenu = false
                                    onClearOlderThan(TimeUnit.DAYS.toMillis(30))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear all history", color = RedVibrant, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                onClick = {
                                    showClearMenu = false
                                    onClearAll()
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = filterAction == null && filterSuccessOnly == null,
                            onClick = {
                                filterAction = null
                                filterSuccessOnly = null
                            },
                            label = { Text("All (${logs.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenVibrant.copy(alpha = 0.2f),
                                selectedLabelColor = GreenVibrant,
                                containerColor = BgElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filterAction == null && filterSuccessOnly == null,
                                borderColor = BorderSubtle,
                                selectedBorderColor = GreenVibrant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterAction == TriggerAction.POWER_ON,
                            onClick = {
                                filterAction = if (filterAction == TriggerAction.POWER_ON) null else TriggerAction.POWER_ON
                            },
                            label = { Text("Power On", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenVibrant.copy(alpha = 0.2f),
                                selectedLabelColor = GreenVibrant,
                                containerColor = BgElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filterAction == TriggerAction.POWER_ON,
                                borderColor = BorderSubtle,
                                selectedBorderColor = GreenVibrant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterAction == TriggerAction.REBOOT,
                            onClick = {
                                filterAction = if (filterAction == TriggerAction.REBOOT) null else TriggerAction.REBOOT
                            },
                            label = { Text("Reboot", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberVibrant.copy(alpha = 0.2f),
                                selectedLabelColor = AmberVibrant,
                                containerColor = BgElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filterAction == TriggerAction.REBOOT,
                                borderColor = BorderSubtle,
                                selectedBorderColor = AmberVibrant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterAction == TriggerAction.POWER_OFF,
                            onClick = {
                                filterAction = if (filterAction == TriggerAction.POWER_OFF) null else TriggerAction.POWER_OFF
                            },
                            label = { Text("Shutdown", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedVibrant.copy(alpha = 0.2f),
                                selectedLabelColor = RedVibrant,
                                containerColor = BgElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filterAction == TriggerAction.POWER_OFF,
                                borderColor = BorderSubtle,
                                selectedBorderColor = RedVibrant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterSuccessOnly == false,
                            onClick = {
                                filterSuccessOnly = if (filterSuccessOnly == false) null else false
                            },
                            label = { Text("Failed Only", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedVibrant.copy(alpha = 0.2f),
                                selectedLabelColor = RedVibrant,
                                containerColor = BgElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filterSuccessOnly == false,
                                borderColor = BorderSubtle,
                                selectedBorderColor = RedVibrant.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Logs list or empty state
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "No trigger events recorded",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                            if (filterAction != null || filterSuccessOnly != null) {
                                TextButton(
                                    onClick = {
                                        filterAction = null
                                        filterSuccessOnly = null
                                    }
                                ) {
                                    Text("Reset Filters", color = GreenVibrant, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            HistoryItemCard(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(log: TriggerLog) {
    val actionColor = when (log.action) {
        TriggerAction.POWER_ON -> GreenVibrant
        TriggerAction.REBOOT -> AmberVibrant
        TriggerAction.POWER_OFF -> RedVibrant
    }

    val actionName = when (log.action) {
        TriggerAction.POWER_ON -> "POWER ON"
        TriggerAction.REBOOT -> "REBOOT"
        TriggerAction.POWER_OFF -> "SHUTDOWN"
    }

    val formattedTime = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    val relativeTime = remember(log.timestamp) {
        val diff = System.currentTimeMillis() - log.timestamp
        when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${diff / 60_000L}m ago"
            diff < 86400_000L -> "${diff / 3600_000L}h ago"
            else -> "${diff / 86400_000L}d ago"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(actionColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        actionName,
                        color = actionColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Source Pill (App / Widget / NFC)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        log.source,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Success/Failure Icon + Status Code
                if (log.isSuccess) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = GreenVibrant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${log.statusCode}",
                        color = GreenVibrant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Failed",
                        tint = RedVibrant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (log.statusCode > 0) "${log.statusCode}" else "ERR",
                        color = RedVibrant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Device Name + Node
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${log.deviceName} (${log.node})",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = relativeTime,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // Message / Details
            if (log.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    color = if (log.isSuccess) TextSecondary else RedVibrant.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
            }

            // Exact timestamp
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedTime,
                color = TextMuted.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}
