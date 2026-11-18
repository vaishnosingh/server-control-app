package com.expapp.trigger.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.ui.theme.DarkBackground
import com.expapp.trigger.ui.theme.DarkSurface
import com.expapp.trigger.ui.theme.PowerOnGreen
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary
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
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = PowerOnGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Device Trigger",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
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
                            tint = PowerOnGreen
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
                                .width(if (isSelected) 22.dp else 8.dp)
                                .clip(if (isSelected) RoundedCornerShape(4.dp) else CircleShape)
                                .background(
                                    when {
                                        isSelected -> PowerOnGreen
                                        isAddPage -> TextSecondary.copy(alpha = 0.5f)
                                        else -> TextSecondary.copy(alpha = 0.3f)
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
                    lastStatus = statusInfo?.first,
                    lastSuccess = statusInfo?.second,
                    onDismissStatus = { statusStates.remove(currentDevice.id) },
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
