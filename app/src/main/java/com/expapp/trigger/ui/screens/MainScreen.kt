package com.expapp.trigger.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.ui.theme.DarkBackground
import com.expapp.trigger.ui.theme.DarkCardBorder
import com.expapp.trigger.ui.theme.DarkSurface
import com.expapp.trigger.ui.theme.DarkSurfaceVariant
import com.expapp.trigger.ui.theme.PowerOnBorder
import com.expapp.trigger.ui.theme.PowerOnGreen
import com.expapp.trigger.ui.theme.StatusOnlineBg
import com.expapp.trigger.ui.theme.StatusOnlineBorder
import com.expapp.trigger.ui.theme.StatusOnlineText
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: DeviceRepository,
    triggerClient: TriggerClient,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var devices by remember { mutableStateOf(repository.getDevices()) }
    val pagerState = rememberPagerState(pageCount = { devices.size + 1 })

    // Device ID -> In-Flight Loading State
    val loadingStates = remember { mutableStateMapOf<String, Boolean>() }
    // Device ID -> Pair(StatusMessage, IsSuccess)
    val statusStates = remember { mutableStateMapOf<String, Pair<String, Boolean>>() }
    // Device ID -> Live NodeStatus (online / offline / ip)
    val nodeStatuses = remember { mutableStateMapOf<String, NodeStatus>() }
    // Device ID -> Is Checking Status
    val checkingStatusMap = remember { mutableStateMapOf<String, Boolean>() }

    var isSyncing by remember { mutableStateOf(false) }

    fun refreshDeviceStatus(device: DeviceConfig) {
        checkingStatusMap[device.id] = true
        triggerClient.checkStatus(device) { status ->
            checkingStatusMap[device.id] = false
            nodeStatuses[device.id] = status
        }
    }

    fun syncEndpoints(showToast: Boolean = false) {
        val currentDevices = repository.getDevices()
        if (currentDevices.isEmpty()) return
        isSyncing = true
        var updatedCount = 0
        var completedCount = 0
        val totalToSync = currentDevices.count { it.r2Url.isNotBlank() }

        if (totalToSync == 0) {
            isSyncing = false
            // Still check ping/status for all devices
            currentDevices.forEach { refreshDeviceStatus(it) }
            return
        }

        currentDevices.forEach { device ->
            // Trigger status check simultaneously
            refreshDeviceStatus(device)

            if (device.r2Url.isNotBlank()) {
                triggerClient.fetchR2Config(device.r2Url) { result ->
                    completedCount++
                    result.onSuccess { endpoints ->
                        val changed = repository.updateEndpoints(
                            deviceId = device.id,
                            powerOnUrl = endpoints.powerOn,
                            powerOffUrl = endpoints.powerOff,
                            rebootUrl = endpoints.reboot,
                            statusUrl = endpoints.status
                        )
                        if (changed) {
                            updatedCount++
                            devices = repository.getDevices()
                        }
                    }

                    if (completedCount == totalToSync) {
                        isSyncing = false
                        if (showToast) {
                            if (updatedCount > 0) {
                                Toast.makeText(context, "Updated endpoints for $updatedCount device(s)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "All device endpoints are up to date", Toast.LENGTH_SHORT).show()
                            }
                        } else if (updatedCount > 0) {
                            Toast.makeText(context, "Synced: Updated endpoints from Cloudflare R2", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // Automatically check Cloudflare R2 and device status on every app launch
    LaunchedEffect(Unit) {
        syncEndpoints(showToast = false)
        devices.forEach { refreshDeviceStatus(it) }
    }

    // Auto-refresh status when swiping to a device
    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (page < devices.size) {
            refreshDeviceStatus(devices[page])
        }
    }

    fun executeAction(device: DeviceConfig, action: TriggerAction) {
        val actionLabel = when (action) {
            TriggerAction.POWER_ON -> "Power On"
            TriggerAction.POWER_OFF -> "Power Off"
            TriggerAction.REBOOT -> "Reboot"
        }

        loadingStates[device.id] = true
        statusStates.remove(device.id)

        triggerClient.sendTrigger(device, action) { response ->
            loadingStates[device.id] = false
            val statusText = if (response.isSuccess) {
                "$actionLabel sent: HTTP ${response.statusCode} - ${response.message.take(60)}"
            } else {
                "Failed: ${response.message.take(60)}"
            }
            statusStates[device.id] = Pair(statusText, response.isSuccess)

            Toast.makeText(
                context,
                if (response.isSuccess) "$actionLabel successful (${response.statusCode})" else "Failed: ${response.message}",
                Toast.LENGTH_SHORT
            ).show()

            // Re-check live status after action
            coroutineScope.launch {
                delay(2000)
                refreshDeviceStatus(device)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = PowerOnBorder,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DEVICE TRIGGER",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )

                        // Online counter badge if devices exist
                        val onlineCount = devices.count { nodeStatuses[it.id]?.isOnline == true }
                        if (devices.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (onlineCount > 0) StatusOnlineBg else DarkSurfaceVariant)
                                    .border(1.dp, if (onlineCount > 0) StatusOnlineBorder else DarkCardBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$onlineCount online",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (onlineCount > 0) StatusOnlineText else TextSecondary
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Manual sync check
                    IconButton(
                        onClick = { syncEndpoints(showToast = true) },
                        enabled = !isSyncing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Endpoints",
                            tint = if (isSyncing) TextSecondary.copy(alpha = 0.5f) else PowerOnBorder
                        )
                    }

                    // Quick jump to Add Device tab
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(devices.size)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Device",
                            tint = PowerOnBorder
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            // Horizontal Pager Dots Indicator
            val totalPages = devices.size + 1
            if (totalPages > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalPages) { pageIndex ->
                        val isSelected = pagerState.currentPage == pageIndex
                        val isAddPage = pageIndex == devices.size

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(if (isSelected) RoundedCornerShape(4.dp) else CircleShape)
                                .background(
                                    when {
                                        isSelected -> PowerOnBorder
                                        isAddPage -> TextSecondary.copy(alpha = 0.5f)
                                        else -> TextSecondary.copy(alpha = 0.25f)
                                    }
                                )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            if (page < devices.size) {
                val currentDevice = devices[page]
                val isLoading = loadingStates[currentDevice.id] == true
                val statusInfo = statusStates[currentDevice.id]

                DevicePage(
                    device = currentDevice,
                    isLoading = isLoading,
                    nodeStatus = nodeStatuses[currentDevice.id],
                    isCheckingStatus = checkingStatusMap[currentDevice.id] == true,
                    lastStatus = statusInfo?.first,
                    lastSuccess = statusInfo?.second,
                    onDismissStatus = { statusStates.remove(currentDevice.id) },
                    onRefreshStatus = { refreshDeviceStatus(currentDevice) },
                    onTrigger = { action -> executeAction(currentDevice, action) },
                    onDelete = { dev ->
                        repository.deleteDevice(dev.id)
                        devices = repository.getDevices()
                    }
                )
            } else {
                AddDevicePage(
                    triggerClient = triggerClient,
                    onDeviceAdded = { newDevice ->
                        repository.addDevice(newDevice)
                        devices = repository.getDevices()
                        refreshDeviceStatus(newDevice)
                        coroutineScope.launch {
                            val newPageIndex = devices.indexOfFirst { it.id == newDevice.id }
                            if (newPageIndex >= 0) {
                                pagerState.animateScrollToPage(newPageIndex)
                            }
                        }
                    }
                )
            }
        }
    }
}
