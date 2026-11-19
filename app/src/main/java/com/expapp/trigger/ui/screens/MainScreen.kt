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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.expapp.trigger.data.model.TriggerLog
import com.expapp.trigger.data.repository.AppSettings
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.TriggerHistoryRepository
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.nfc.NfcHelper
import com.expapp.trigger.nfc.NfcTriggerPayload
import com.expapp.trigger.security.BiometricHelper
import com.expapp.trigger.ui.components.HistoryDialog
import com.expapp.trigger.ui.components.NfcWriteDialog
import com.expapp.trigger.ui.components.SettingsDialog
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
import com.expapp.trigger.widget.ServerWidgetProvider
import com.expapp.trigger.worker.ActionTriggerWorker
import com.expapp.trigger.worker.ServerMonitorWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
@Composable
fun MainScreen(
    repository: DeviceRepository,
    triggerClient: TriggerClient,
    appSettings: AppSettings,
    historyRepository: TriggerHistoryRepository,
    requestBiometricAuth: (title: String, subtitle: String, onResult: (Boolean) -> Unit) -> Unit = { _, _, cb -> cb(true) },
    isNotificationPermissionGranted: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    nfcWriteResult: Result<Unit>? = null,
    onStartNfcWriting: (NfcTriggerPayload) -> Unit = {},
    onStopNfcWriting: () -> Unit = {},
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
    var dismissedVersion by remember { mutableStateOf<String?>(null) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // Dialog & Feature states
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNfcWriteDialog by remember { mutableStateOf(false) }
    var historyLogs by remember { mutableStateOf(historyRepository.getLogs()) }
    var isBiometricEnabled by remember { mutableStateOf(appSettings.isBiometricEnabled) }
    var isBgMonitorEnabled by remember { mutableStateOf(appSettings.isBackgroundMonitoringEnabled) }

    val loadingStates     = remember { mutableStateMapOf<String, Boolean>() }
    val statusStates      = remember { mutableStateMapOf<String, Pair<String, Boolean>>() }
    val nodeStatuses      = remember { mutableStateMapOf<String, NodeStatus>() }
    val checkingStatusMap = remember { mutableStateMapOf<String, Boolean>() }
    var isSyncing by remember { mutableStateOf(false) }
    var isPullRefreshing by remember { mutableStateOf(false) }

    fun refreshStatus(device: DeviceConfig, showIndicator: Boolean = false) {
        if (showIndicator || nodeStatuses[device.id] == null) {
            checkingStatusMap[device.id] = true
        }
        triggerClient.checkStatus(device) { status ->
            checkingStatusMap[device.id] = false
            nodeStatuses[device.id] = status
            // Only update stored status when server is actually reached
            if (status.isReachable) {
                appSettings.setLastStatus(device.id, status.isOnline)
            }
        }
    }

    fun syncEndpoints(showToast: Boolean = false) {
        val current = repository.getDevices()
        if (current.isEmpty()) return
        isSyncing = true
        var updated = 0; var completed = 0
        val total = current.count { it.r2Url.isNotBlank() }
        if (total == 0) {
            isSyncing = false
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
                }
            }
        }
    }

    fun checkAppUpdate(isManual: Boolean = false) {
        isCheckingUpdate = true
        updateManager.checkForUpdate(BuildConfig.VERSION_NAME) { result ->
            isCheckingUpdate = false
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    availableUpdate = result.info
                    showUpdateDialog = true
                    val newVer = result.info.versionName
                    if (appSettings.lastNotifiedUpdateVersion != newVer) {
                        appSettings.lastNotifiedUpdateVersion = newVer
                        com.expapp.trigger.worker.NotificationHelper.sendUpdateAvailableNotification(context, newVer)
                    }
                    if (isManual) {
                        Toast.makeText(context, "New update available: v${result.info.versionName}!", Toast.LENGTH_SHORT).show()
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    availableUpdate = null
                    if (isManual) {
                        Toast.makeText(context, "Page refreshed • App is up to date (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (isManual) {
                        Toast.makeText(context, "Page refreshed • Update check failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Pull-to-refresh entire page handler
    fun refreshWholePage() {
        isPullRefreshing = true
        dismissedVersion = null
        coroutineScope.launch {
            // 1. Sync R2 endpoints
            syncEndpoints(showToast = false)

            // 2. Poll status for all configured devices
            devices.forEach { dev ->
                refreshStatus(dev, showIndicator = true)
            }

            // 3. Explicitly check for app updates with user feedback
            checkAppUpdate(isManual = true)

            // 4. Update home screen widgets
            ServerWidgetProvider.updateAllWidgets(context)

            delay(750)
            isPullRefreshing = false
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isPullRefreshing,
        onRefresh = { refreshWholePage() }
    )

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
        val actionTitle = when (action) {
            TriggerAction.POWER_ON -> "Power On"
            TriggerAction.REBOOT -> "Reboot"
            TriggerAction.POWER_OFF -> "Shutdown"
        }

        val performDispatch = {
            loadingStates[device.id] = true
            statusStates.remove(device.id)
            Toast.makeText(context, "Sending $actionTitle signal to ${device.name}...", Toast.LENGTH_SHORT).show()

            // Enqueue guaranteed background worker: executes HTTP request, sends notifications, updates widgets & monitors transition
            ActionTriggerWorker.enqueue(context, device.id, action, "App")

            // In foreground, refresh UI smoothly after short intervals if user stays in the app
            coroutineScope.launch {
                delay(2000)
                refreshStatus(device, showIndicator = true)
                historyLogs = historyRepository.getLogs()
                loadingStates[device.id] = false
                delay(3000)
                refreshStatus(device, showIndicator = false)
                historyLogs = historyRepository.getLogs()
                delay(4000)
                refreshStatus(device, showIndicator = false)
            }
        }

        // Biometric Security Gate
        if (appSettings.isBiometricEnabled && BiometricHelper.isBiometricAvailable(context)) {
            requestBiometricAuth(
                "Confirm $actionTitle",
                "Authenticate with fingerprint to trigger ${device.name}"
            ) { success ->
                if (success) {
                    performDispatch()
                } else {
                    Toast.makeText(context, "Action cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            performDispatch()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgBase,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = GreenVibrant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TRIGGER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        val onlineCount = devices.count { nodeStatuses[it.id]?.isOnline == true }
                        if (devices.isNotEmpty() && onlineCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(OnlineBg)
                                    .border(1.dp, OnlineBorder, CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "$onlineCount",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnlineText
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Update Button (only when update is ready)
                    if (availableUpdate != null) {
                        IconButton(
                            onClick = { showUpdateDialog = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = "Update available",
                                    tint = GreenVibrant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(GreenVibrant, CircleShape)
                                        .border(1.dp, BgSurface, CircleShape)
                                )
                            }
                        }
                    }

                    // Trigger History Button
                    IconButton(
                        onClick = {
                            historyLogs = historyRepository.getLogs()
                            showHistoryDialog = true
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Trigger History",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Add Device Button
                    IconButton(
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(devices.size) } },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            "Add Device",
                            tint = GreenVibrant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                                .padding(horizontal = 4.dp)
                                .size(if (selected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(i)
                                    }
                                }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Pull to refresh container enclosing HorizontalPager
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
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
                        onDelete = { d ->
                            repository.deleteDevice(d.id)
                            devices = repository.getDevices()
                        },
                        onSyncConfig = { syncEndpoints(true) },
                        onUpdateDevice = { updated ->
                            repository.updateDevice(updated)
                            devices = repository.getDevices()
                            refreshStatus(updated, showIndicator = true)
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

            PullRefreshIndicator(
                refreshing = isPullRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = BgElevated,
                contentColor = GreenVibrant
            )
        }
    }

    // Trigger History Dialog
    if (showHistoryDialog) {
        HistoryDialog(
            logs = historyLogs,
            onClearOlderThan = { millis ->
                historyRepository.clearOlderThan(millis)
                historyLogs = historyRepository.getLogs()
            },
            onClearAll = {
                historyRepository.clearAll()
                historyLogs = emptyList()
            },
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            isBiometricEnabled = isBiometricEnabled,
            onBiometricToggled = { enabled ->
                isBiometricEnabled = enabled
                appSettings.isBiometricEnabled = enabled
            },
            isBgMonitorEnabled = isBgMonitorEnabled,
            onBgMonitorToggled = { enabled ->
                isBgMonitorEnabled = enabled
                appSettings.isBackgroundMonitoringEnabled = enabled
                if (enabled) {
                    ServerMonitorWorker.schedule(context)
                } else {
                    ServerMonitorWorker.cancel(context)
                }
            },
            isBiometricAvailable = BiometricHelper.isBiometricAvailable(context),
            isNfcAvailable = NfcHelper.isNfcSupported(context),
            isNotificationPermissionGranted = isNotificationPermissionGranted,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onSendTestNotification = onSendTestNotification,
            onOpenNfcWriter = {
                showNfcWriteDialog = true
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // NFC Write Tag Dialog
    if (showNfcWriteDialog) {
        NfcWriteDialog(
            devices = devices,
            writeResult = nfcWriteResult,
            onStartWriting = { payload ->
                onStartNfcWriting(payload)
            },
            onStopWriting = {
                onStopNfcWriting()
            },
            onDismiss = {
                onStopNfcWriting()
                showNfcWriteDialog = false
            }
        )
    }

    // Update Dialog
    if (showUpdateDialog && availableUpdate != null) {
        UpdateDialog(
            releaseInfo = availableUpdate!!,
            currentVersion = BuildConfig.VERSION_NAME,
            downloadState = downloadState,
            onDownload = {
                val info = availableUpdate!!
                // If retrying from failure, re-check to get fresh release asset URLs
                if (downloadState is DownloadState.Failed) {
                    downloadState = DownloadState.Downloading(0, 0, info.sizeBytes)
                    updateManager.checkForUpdate(BuildConfig.VERSION_NAME) { checkResult ->
                        if (checkResult is UpdateCheckResult.UpdateAvailable) {
                            availableUpdate = checkResult.info
                            val fresh = checkResult.info
                            updateManager.downloadApk(fresh.downloadUrl, fresh.assetApiUrl, fresh.sizeBytes) { state ->
                                downloadState = state
                            }
                        } else {
                            updateManager.downloadApk(info.downloadUrl, info.assetApiUrl, info.sizeBytes) { state ->
                                downloadState = state
                            }
                        }
                    }
                } else {
                    downloadState = DownloadState.Downloading(0, 0, info.sizeBytes)
                    updateManager.downloadApk(info.downloadUrl, info.assetApiUrl, info.sizeBytes) { state ->
                        downloadState = state
                    }
                }
            },
            onInstall = { file ->
                showUpdateDialog = false
                updateManager.installApk(file)
            },
            onDismiss = {
                dismissedVersion = availableUpdate?.versionName
                showUpdateDialog = false
            }
        )
    }
}
