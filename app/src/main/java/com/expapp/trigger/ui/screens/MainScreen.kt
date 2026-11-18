package com.expapp.trigger.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.expapp.trigger.BuildConfig
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.ui.components.UpdateDialog
import com.expapp.trigger.ui.theme.BgBase
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.OnlineBg
import com.expapp.trigger.ui.theme.OnlineBorder
import com.expapp.trigger.ui.theme.OnlineText
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary
import com.expapp.trigger.update.DownloadState
import com.expapp.trigger.update.ReleaseInfo
import com.expapp.trigger.update.UpdateCheckResult
import com.expapp.trigger.update.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    val lifecycleOwner = LocalLifecycleOwner.current

    var devices by remember { mutableStateOf(repository.getDevices()) }
    val pagerState = rememberPagerState(pageCount = { devices.size + 1 })

    var isAppInForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            isAppInForeground = when (event) {
                Lifecycle.Event.ON_RESUME -> true
                else -> false
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val updateManager = remember { UpdateManager(context) }
    var availableUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    fun checkAppUpdate(showToast: Boolean = false) {
        isCheckingUpdate = true
        updateManager.checkForUpdate(BuildConfig.VERSION_NAME) { result ->
            isCheckingUpdate = false
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    availableUpdate = result.info
                    showUpdateDialog = true
                }
                is UpdateCheckResult.UpToDate -> {
                    if (showToast) {
                        Toast.makeText(context, "App is up to date (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (showToast) {
                        Toast.makeText(context, "Update check failed: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val loadingStates     = remember { mutableStateMapOf<String, Boolean>() }
    val statusStates      = remember { mutableStateMapOf<String, Pair<String, Boolean>>() }
    val nodeStatuses      = remember { mutableStateMapOf<String, NodeStatus>() }
    val checkingStatusMap = remember { mutableStateMapOf<String, Boolean>() }
    var isSyncing by remember { mutableStateOf(false) }

    fun refreshStatus(device: DeviceConfig, showIndicator: Boolean = false) {
        if (showIndicator || nodeStatuses[device.id] == null) {
            checkingStatusMap[device.id] = true
        }
        triggerClient.checkStatus(device) { status ->
            checkingStatusMap[device.id] = false
            nodeStatuses[device.id] = status
        }
    }

    fun syncEndpoints(showToast: Boolean = false) {
        val current = repository.getDevices()
        if (current.isEmpty()) {
            checkAppUpdate(showToast)
            return
        }
        isSyncing = true
        var updated = 0; var completed = 0
        val total = current.count { it.r2Url.isNotBlank() }
        if (total == 0) {
            isSyncing = false
            checkAppUpdate(showToast)
            return
        }
        current.filter { it.r2Url.isNotBlank() }.forEach { device ->
            triggerClient.fetchR2Config(device.r2Url) { result ->
                completed++
                result.onSuccess { ep ->
                    val changed = repository.updateEndpoints(device.id, ep.powerOn, ep.powerOff, ep.reboot, ep.status)
                    if (changed) { updated++; devices = repository.getDevices() }
                }
                if (completed == total) {
                    isSyncing = false
                    if (showToast) Toast.makeText(
                        context,
                        if (updated > 0) "Endpoints updated" else "Already up to date",
                        Toast.LENGTH_SHORT
                    ).show()
                    checkAppUpdate(showToast)
                }
            }
        }
    }

    // Initial R2 sync and update check on launch
    LaunchedEffect(Unit) {
        syncEndpoints(false)
        checkAppUpdate(false)
    }

    // Active 5-second polling — only while in foreground on a device page
    LaunchedEffect(pagerState.currentPage, isAppInForeground, devices.size) {
        if (!isAppInForeground) return@LaunchedEffect
        val page = pagerState.currentPage
        if (page >= devices.size) return@LaunchedEffect
        val dev = devices[page]
        while (isActive && isAppInForeground) {
            refreshStatus(dev, showIndicator = false)
            delay(5000L)
        }
    }

    fun executeAction(device: DeviceConfig, action: TriggerAction) {
        loadingStates[device.id] = true
        statusStates.remove(device.id)
        triggerClient.sendTrigger(device, action) { response ->
            loadingStates[device.id] = false
            val msg = if (response.isSuccess) {
                when (action) {
                    TriggerAction.POWER_ON  -> "Power on signal sent"
                    TriggerAction.POWER_OFF -> "Shutdown signal sent"
                    TriggerAction.REBOOT    -> "Restart signal sent"
                }
            } else {
                val raw = response.message.trim()
                val clean = if (raw.startsWith("<")) "Request failed (HTTP ${response.statusCode})" else raw.take(60)
                "Failed — $clean"
            }
            statusStates[device.id] = msg to response.isSuccess
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                delay(1500); refreshStatus(device, showIndicator = true)
                delay(4000); refreshStatus(device)
                delay(5000); refreshStatus(device)
            }
        }
    }

    // Spin animation for sync button
    val rotationAngle by animateFloatAsState(
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = if (isSyncing) tween(900, easing = FastOutSlowInEasing) else tween(0),
        label = "sync_rotate"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgBase,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = GreenVibrant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DEVICE TRIGGER",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        val onlineCount = devices.count { nodeStatuses[it.id]?.isOnline == true }
                        if (devices.isNotEmpty() && onlineCount > 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(OnlineBg)
                                    .border(1.dp, OnlineBorder, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "$onlineCount online",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnlineText,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (availableUpdate != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenVibrant.copy(alpha = 0.15f))
                                .border(1.dp, GreenVibrant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { showUpdateDialog = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SystemUpdate, null, tint = GreenVibrant, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "v${availableUpdate!!.versionName}",
                                color = GreenVibrant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(onClick = { syncEndpoints(true) }, enabled = !isSyncing) {
                        Icon(
                            Icons.Default.Refresh,
                            "Sync",
                            tint = if (isSyncing) TextMuted else GreenVibrant,
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                    IconButton(
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(devices.size) } }
                    ) {
                        Icon(Icons.Default.Add, "Add Device", tint = GreenVibrant)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BgSurface
                )
            )
        },
        bottomBar = {
            val total = devices.size + 1
            if (total > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(total) { i ->
                        val selected = pagerState.currentPage == i
                        val isAdd    = i == devices.size
                        val dotColor = when {
                            selected && !isAdd -> GreenVibrant
                            selected           -> TextSecondary
                            isAdd              -> TextMuted.copy(alpha = 0.4f)
                            else               -> TextMuted.copy(alpha = 0.3f)
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(if (selected) 22.dp else 6.dp)
                                .clip(if (selected) RoundedCornerShape(3.dp) else CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { page ->
            if (page < devices.size) {
                val dev = devices[page]
                DevicePage(
                    device = dev,
                    isLoading = loadingStates[dev.id] == true,
                    nodeStatus = nodeStatuses[dev.id],
                    isCheckingStatus = checkingStatusMap[dev.id] == true,
                    lastStatus = statusStates[dev.id]?.first,
                    lastSuccess = statusStates[dev.id]?.second,
                    onDismissStatus = { statusStates.remove(dev.id) },
                    onRefreshStatus = { refreshStatus(dev, showIndicator = true) },
                    onTrigger = { action -> executeAction(dev, action) },
                    onDelete = { d -> repository.deleteDevice(d.id); devices = repository.getDevices() },
                    onSyncConfig = {
                        triggerClient.fetchR2Config(dev.r2Url) { result ->
                            result.onSuccess { ep ->
                                val changed = repository.updateEndpoints(dev.id, ep.powerOn, ep.powerOff, ep.reboot, ep.status)
                                devices = repository.getDevices()
                                val msg = if (changed) "Endpoints updated from R2" else "Endpoints already up to date"
                                statusStates[dev.id] = msg to true
                                refreshStatus(dev, showIndicator = true)
                            }.onFailure { ex ->
                                statusStates[dev.id] = "Failed to fetch R2: ${ex.message}" to false
                            }
                        }
                    },
                    onUpdateDevice = { updated ->
                        repository.updateDevice(updated)
                        devices = repository.getDevices()
                        if (updated.r2Url.isNotBlank() && updated.r2Url != dev.r2Url) {
                            triggerClient.fetchR2Config(updated.r2Url) { result ->
                                result.onSuccess { ep ->
                                    repository.updateEndpoints(updated.id, ep.powerOn, ep.powerOff, ep.reboot, ep.status)
                                    devices = repository.getDevices()
                                    statusStates[updated.id] = "Device & endpoints updated" to true
                                    refreshStatus(updated, showIndicator = true)
                                }
                            }
                        } else {
                            statusStates[updated.id] = "Device updated" to true
                        }
                    }
                )
            } else {
                AddDevicePage(
                    triggerClient = triggerClient,
                    onDeviceAdded = { newDev ->
                        repository.addDevice(newDev)
                        devices = repository.getDevices()
                        refreshStatus(newDev, showIndicator = true)
                        coroutineScope.launch {
                            val idx = devices.indexOfFirst { it.id == newDev.id }
                            if (idx >= 0) pagerState.animateScrollToPage(idx)
                        }
                    }
                )
            }
        }
    }

    if (showUpdateDialog && availableUpdate != null) {
        UpdateDialog(
            releaseInfo = availableUpdate!!,
            currentVersion = BuildConfig.VERSION_NAME,
            downloadState = downloadState,
            onDownload = {
                downloadState = DownloadState.Downloading(0, 0, availableUpdate!!.sizeBytes)
                updateManager.downloadApk(availableUpdate!!.downloadUrl) { state ->
                    downloadState = state
                }
            },
            onInstall = { file ->
                updateManager.installApk(file)
            },
            onDismiss = {
                showUpdateDialog = false
            }
        )
    }
}
