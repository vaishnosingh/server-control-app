package com.expapp.trigger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.expapp.trigger.data.model.TriggerLog
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.MonoFontFamily
import com.expapp.trigger.ui.theme.PixelSemantic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trigger History",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${logs.size} events recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (logs.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showClearMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear history",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showClearMenu,
                                onDismissRequest = { showClearMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear older than 24h") },
                                    onClick = {
                                        showClearMenu = false
                                        onClearOlderThan(24L * 60 * 60 * 1000)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear older than 7 days") },
                                    onClick = {
                                        showClearMenu = false
                                        onClearOlderThan(7L * 24 * 60 * 60 * 1000)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Clear all history",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showClearMenu = false
                                        onClearAll()
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filterAction == null && filterSuccessOnly == null,
                            onClick = {
                                filterAction = null
                                filterSuccessOnly = null
                            },
                            label = { Text("All (${logs.size})") },
                            shape = RoundedCornerShape(100)
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterAction == TriggerAction.POWER_ON,
                            onClick = {
                                filterAction = if (filterAction == TriggerAction.POWER_ON) null else TriggerAction.POWER_ON
                            },
                            label = { Text("Power On") },
                            shape = RoundedCornerShape(100)
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterAction == TriggerAction.REBOOT,
                            onClick = {
                                filterAction = if (filterAction == TriggerAction.REBOOT) null else TriggerAction.REBOOT
                            },
                            label = { Text("Reboot") },
                            shape = RoundedCornerShape(100)
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterAction == TriggerAction.POWER_OFF,
                            onClick = {
                                filterAction = if (filterAction == TriggerAction.POWER_OFF) null else TriggerAction.POWER_OFF
                            },
                            label = { Text("Shutdown") },
                            shape = RoundedCornerShape(100)
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterSuccessOnly == false,
                            onClick = {
                                filterSuccessOnly = if (filterSuccessOnly == false) null else false
                            },
                            label = { Text("Failed Only") },
                            shape = RoundedCornerShape(100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No trigger events recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (filterAction != null || filterSuccessOnly != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = {
                                        filterAction = null
                                        filterSuccessOnly = null
                                    }
                                ) {
                                    Text("Reset Filters")
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
    val dark = LocalIsDarkTheme.current
    val (actionName, actionContainer, actionOnContainer) = when (log.action) {
        TriggerAction.POWER_ON -> Triple(
            "POWER ON",
            PixelSemantic.onlineContainer(dark),
            PixelSemantic.onOnlineContainer(dark)
        )
        TriggerAction.REBOOT -> Triple(
            "REBOOT",
            PixelSemantic.warningContainer(dark),
            PixelSemantic.onWarningContainer(dark)
        )
        TriggerAction.POWER_OFF -> Triple(
            "SHUTDOWN",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    val formattedTime = remember(log.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = actionContainer
                ) {
                    Text(
                        text = actionName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = actionOnContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = log.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (log.isSuccess) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = PixelSemantic.onlineAccent(dark),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${log.statusCode}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PixelSemantic.onlineAccent(dark)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (log.statusCode > 0) "${log.statusCode}" else "ERR",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${log.deviceName} (${log.node})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (log.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                    color = if (log.isSuccess) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
