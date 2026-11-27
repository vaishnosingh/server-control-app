package com.expapp.trigger.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expapp.trigger.BuildConfig
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.TriggerLog
import com.expapp.trigger.data.repository.AppSettings
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.ThemeMode
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
import com.expapp.trigger.ui.theme.GlobalAppTheme
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.PixelSemantic
import com.expapp.trigger.ui.theme.ServerAccents
import com.expapp.trigger.ui.theme.ServerMaterialTheme
import com.expapp.trigger.update.DownloadState
import com.expapp.trigger.update.ReleaseInfo
import com.expapp.trigger.update.UpdateCheckResult
import com.expapp.trigger.update.UpdateManager
import com.expapp.trigger.widget.ServerWidgetProvider
import com.expapp.trigger.worker.AppUpdateWorker
import com.expapp.trigger.worker.ServerMonitorWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class MainTab(val label: String) {
    CONTROL("Control"),
    SYSPULSE("SysPulse"),
    FLEET("Fleet")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: DeviceRepository,
    triggerClient: TriggerClient,
    appSettings: AppSettings,
    historyRepository: TriggerHistoryRepository,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    isDynamicColorEnabled: Boolean = true,
    onDynamicColorToggled: (Boolean) -> Unit = {},
    requestBiometricAuth: (title: String, subtitle: String, onResult: (Boolean) -> Unit) -> Unit,
    isNotificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    nfcWriteResult: Result<Unit>?,
    onStartNfcWriting: (NfcTriggerPayload) -> Unit,
    onStopNfcWriting: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateManager = remember(context) { UpdateManager(context) }

    var devices by remember { mutableStateOf(repository.getDevices()) }
    var selectedServerId by remember { mutableStateOf(devices.firstOrNull()?.id) }
    var selectedTab by remember { mutableStateOf(MainTab.CONTROL) }
    var showAddDeviceScreen by remember { mutableStateOf(false) }
    var isServiceDetailOpen by remember { mutableStateOf(false) }

    var nodeStatuses by remember { mutableStateOf<Map<String, NodeStatus>>(emptyMap()) }
    var checkingStatusMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loadingDevicesMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var statusBannerMap by remember { mutableStateOf<Map<String, Pair<String, Boolean>>>(emptyMap()) }
    var isSyncing by remember { mutableStateOf(false) }

    var isBiometricEnabled by remember { mutableStateOf(appSettings.isBiometricEnabled) }
    var isWidgetBiometricEnabled by remember { mutableStateOf(appSettings.isWidgetBiometricEnabled) }
    var isBgMonitorEnabled by remember { mutableStateOf(appSettings.isBackgroundMonitoringEnabled) }
    val isBiometricAvailable = remember(context) { BiometricHelper.isBiometricAvailable(context) }
    val isNfcAvailable = remember(context) {
        NfcHelper.isNfcSupported(context) && NfcHelper.isNfcEnabled(context)
    }

    var availableUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNfcWriteDialog by remember { mutableStateOf(false) }
    var historyLogs by remember { mutableStateOf<List<TriggerLog>>(emptyList()) }

    // Keep selectedServerId aligned with existing devices
    LaunchedEffect(devices) {
        if (devices.isEmpty()) {
            selectedServerId = null
        } else if (devices.none { it.id == selectedServerId }) {
            selectedServerId = devices.first().id
        }
    }

    val selectedIndex = devices.indexOfFirst { it.id == selectedServerId }.let {
        if (it >= 0) it else 0
    }
    val selectedDevice = devices.getOrNull(selectedIndex)

    fun refreshDeviceStatus(device: DeviceConfig) {
        if (device.statusUrl.isBlank()) return
        checkingStatusMap = checkingStatusMap + (device.id to true)
        triggerClient.checkStatus(device) { status ->
            nodeStatuses = nodeStatuses + (device.id to status)
            checkingStatusMap = checkingStatusMap - device.id
            appSettings.setLastStatus(device.id, status.isOnline)
        }
    }

    fun refreshAllStatuses(targetDevices: List<DeviceConfig> = devices) {
        targetDevices.forEach { refreshDeviceStatus(it) }
    }

    fun syncDeviceR2(device: DeviceConfig, onDone: ((DeviceConfig) -> Unit)? = null) {
        if (device.r2Url.isBlank()) {
            onDone?.invoke(device)
            return
        }
        isSyncing = true
        triggerClient.fetchR2Config(device.r2Url) { result ->
            isSyncing = false
            result.onSuccess { endpoints ->
                val changed = repository.applyParsedEndpoints(device.id, endpoints)
                if (changed) {
                    devices = repository.getDevices()
                    ServerWidgetProvider.updateAllWidgets(context)
                }
                val updated = repository.getDevices().find { it.id == device.id } ?: device
                refreshDeviceStatus(updated)
                onDone?.invoke(updated)
            }.onFailure {
                onDone?.invoke(device)
            }
        }
    }

    fun syncAllR2(targetDevices: List<DeviceConfig> = devices) {
        val withR2 = targetDevices.filter { it.r2Url.isNotBlank() }
        if (withR2.isEmpty()) {
            refreshAllStatuses(targetDevices)
            return
        }
        isSyncing = true
        var remaining = withR2.size
        withR2.forEach { dev ->
            triggerClient.fetchR2Config(dev.r2Url) { result ->
                result.onSuccess { endpoints ->
                    if (repository.applyParsedEndpoints(dev.id, endpoints)) {
                        devices = repository.getDevices()
                        ServerWidgetProvider.updateAllWidgets(context)
                    }
                }
                remaining -= 1
                if (remaining <= 0) {
                    isSyncing = false
                    refreshAllStatuses(repository.getDevices())
                }
            }
        }
    }

    fun dispatchPowerAction(device: DeviceConfig, action: TriggerAction) {
        loadingDevicesMap = loadingDevicesMap + (device.id to true)
        triggerClient.sendTrigger(device, action) { response ->
            loadingDevicesMap = loadingDevicesMap - device.id
            statusBannerMap = statusBannerMap + (device.id to (response.message to response.isSuccess))
            historyRepository.addLog(
                TriggerLog(
                    deviceId = device.id,
                    deviceName = device.name,
                    node = device.node,
                    action = action,
                    isSuccess = response.isSuccess,
                    statusCode = response.statusCode,
                    message = response.message,
                    source = "App"
                )
            )
            ServerWidgetProvider.updateAllWidgets(context)
            coroutineScope.launch {
                delay(2000L)
                refreshDeviceStatus(device)
            }
        }
    }

    fun executePowerAction(device: DeviceConfig, action: TriggerAction) {
        val actionLabel = when (action) {
            TriggerAction.POWER_ON -> "Power On"
            TriggerAction.REBOOT -> "Reboot"
            TriggerAction.POWER_OFF -> "Power Off"
        }
        if (isBiometricEnabled && isBiometricAvailable) {
            requestBiometricAuth(
                "Confirm $actionLabel",
                "Verify identity to trigger $actionLabel on ${device.name}"
            ) { success ->
                if (success) {
                    dispatchPowerAction(device, action)
                } else {
                    statusBannerMap = statusBannerMap + (device.id to ("Authentication cancelled" to false))
                }
            }
        } else {
            dispatchPowerAction(device, action)
        }
    }

    // Initial update check + R2 config sync
    LaunchedEffect(Unit) {
        updateManager.checkForUpdate(BuildConfig.VERSION_NAME) { result ->
            if (result is UpdateCheckResult.UpdateAvailable) {
                availableUpdate = result.info
            }
        }
        syncAllR2(devices)
    }

    // Periodic status polling every 15 seconds while in foreground
    LaunchedEffect(devices.map { it.id to it.statusUrl }) {
        while (isActive) {
            refreshAllStatuses(devices)
            delay(15_000L)
        }
    }

    val useServerSeedTheme = !showAddDeviceScreen &&
        (selectedTab == MainTab.CONTROL || selectedTab == MainTab.SYSPULSE) &&
        selectedDevice != null

    val chromeContent: @Composable () -> Unit = {
        val dark = LocalIsDarkTheme.current
        val hideChrome = showAddDeviceScreen ||
            (selectedTab == MainTab.SYSPULSE && isServiceDetailOpen && selectedDevice != null)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!hideChrome) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (selectedTab == MainTab.FLEET) "Server Fleet" else "Device Trigger",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            if (availableUpdate != null) {
                                FilledTonalButton(
                                    onClick = { showUpdateDialog = true },
                                    shape = RoundedCornerShape(100),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = PixelSemantic.onlineContainer(dark),
                                        contentColor = PixelSemantic.onOnlineContainer(dark)
                                    ),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = "Update available",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "v${availableUpdate!!.versionName}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            IconButton(
                                onClick = {
                                    historyLogs = historyRepository.getLogs()
                                    showHistoryDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Trigger History",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (!hideChrome) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == MainTab.CONTROL,
                            onClick = {
                                isServiceDetailOpen = false
                                selectedTab = MainTab.CONTROL
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Control"
                                )
                            },
                            label = { Text("Control") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.SYSPULSE,
                            onClick = {
                                selectedTab = MainTab.SYSPULSE
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "SysPulse"
                                )
                            },
                            label = { Text("SysPulse") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.FLEET,
                            onClick = {
                                isServiceDetailOpen = false
                                selectedTab = MainTab.FLEET
                            },
                            icon = {
                                if (devices.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text("${devices.size}")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Dns,
                                            contentDescription = "Fleet"
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = "Fleet"
                                    )
                                }
                            },
                            label = { Text("Fleet") }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == MainTab.FLEET && !showAddDeviceScreen) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddDeviceScreen = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        },
                        text = { Text("Add Server") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    showAddDeviceScreen -> {
                        BackHandler { showAddDeviceScreen = false }
                        AddDevicePage(
                            triggerClient = triggerClient,
                            existingDeviceCount = devices.size,
                            onBack = { showAddDeviceScreen = false },
                            onDeviceAdded = { newDevice ->
                                repository.addDevice(newDevice)
                                devices = repository.getDevices()
                                selectedServerId = newDevice.id
                                showAddDeviceScreen = false
                                selectedTab = MainTab.CONTROL
                                ServerWidgetProvider.updateAllWidgets(context)
                                refreshDeviceStatus(newDevice)
                            }
                        )
                    }

                    selectedTab == MainTab.CONTROL -> {
                        if (devices.isEmpty()) {
                            EmptyServersView(
                                onAddServer = { showAddDeviceScreen = true }
                            )
                        } else {
                            val pagerState = rememberPagerState(
                                initialPage = selectedIndex.coerceIn(0, (devices.size - 1).coerceAtLeast(0)),
                                pageCount = { devices.size }
                            )

                            LaunchedEffect(selectedServerId, devices) {
                                val target = devices.indexOfFirst { it.id == selectedServerId }
                                if (target >= 0 && pagerState.currentPage != target) {
                                    pagerState.animateScrollToPage(target)
                                }
                            }

                            LaunchedEffect(pagerState.currentPage, devices) {
                                devices.getOrNull(pagerState.currentPage)?.let { active ->
                                    selectedServerId = active.id
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                if (devices.size > 1) {
                                    ServerSwitcherStrip(
                                        devices = devices,
                                        selectedServerId = selectedServerId,
                                        nodeStatuses = nodeStatuses,
                                        onSelectServer = { dev ->
                                            selectedServerId = dev.id
                                        }
                                    )
                                }

                                HorizontalPager(
                                    state = pagerState,
                                    key = { idx -> devices[idx].id },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) { page ->
                                    val pageDevice = devices[page]
                                    ServerMaterialTheme(
                                        seedHex = pageDevice.accentColorHex,
                                        themeMode = themeMode
                                    ) {
                                        val banner = statusBannerMap[pageDevice.id]
                                        DevicePage(
                                            device = pageDevice,
                                            isLoading = loadingDevicesMap[pageDevice.id] == true,
                                            nodeStatus = nodeStatuses[pageDevice.id],
                                            isCheckingStatus = checkingStatusMap[pageDevice.id] == true,
                                            lastStatus = banner?.first,
                                            lastSuccess = banner?.second,
                                            onDismissStatus = {
                                                statusBannerMap = statusBannerMap - pageDevice.id
                                            },
                                            onRefreshStatus = { refreshDeviceStatus(pageDevice) },
                                            onTrigger = { action -> executePowerAction(pageDevice, action) },
                                            onDelete = { target ->
                                                repository.deleteDevice(target.id)
                                                devices = repository.getDevices()
                                                ServerWidgetProvider.updateAllWidgets(context)
                                            },
                                            onSyncConfig = { syncDeviceR2(pageDevice) },
                                            onUpdateDevice = { updated ->
                                                repository.updateDevice(updated)
                                                devices = repository.getDevices()
                                                ServerWidgetProvider.updateAllWidgets(context)
                                                refreshDeviceStatus(updated)
                                            },
                                            onOpenSysPulse = {
                                                selectedServerId = pageDevice.id
                                                isServiceDetailOpen = false
                                                selectedTab = MainTab.SYSPULSE
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    selectedTab == MainTab.SYSPULSE -> {
                        if (devices.isEmpty() || selectedDevice == null) {
                            BackHandler { selectedTab = MainTab.CONTROL }
                            EmptyServersView(
                                onAddServer = { showAddDeviceScreen = true }
                            )
                        } else {
                            BackHandler(enabled = !isServiceDetailOpen) {
                                selectedTab = MainTab.CONTROL
                            }
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (!isServiceDetailOpen && devices.size > 1) {
                                    ServerSwitcherStrip(
                                        devices = devices,
                                        selectedServerId = selectedDevice.id,
                                        nodeStatuses = nodeStatuses,
                                        onSelectServer = { dev ->
                                            selectedServerId = dev.id
                                            isServiceDetailOpen = false
                                        }
                                    )
                                }

                                DashboardScreen(
                                    device = selectedDevice,
                                    nodeStatus = nodeStatuses[selectedDevice.id],
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    showBackButton = false,
                                    onBack = { selectedTab = MainTab.CONTROL },
                                    onTriggerPowerAction = { action ->
                                        executePowerAction(selectedDevice, action)
                                    },
                                    onRequireBiometric = { title, subtitle, onAuthorized ->
                                        if (isBiometricEnabled && isBiometricAvailable) {
                                            requestBiometricAuth(title, subtitle) { ok ->
                                                if (ok) onAuthorized()
                                            }
                                        } else {
                                            onAuthorized()
                                        }
                                    },
                                    onServiceDetailVisibilityChanged = { visible ->
                                        isServiceDetailOpen = visible
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            }
                        }
                    }

                    selectedTab == MainTab.FLEET -> {
                        BackHandler { selectedTab = MainTab.CONTROL }
                        FleetOverviewPage(
                            devices = devices,
                            nodeStatuses = nodeStatuses,
                            checkingStatusMap = checkingStatusMap,
                            isSyncing = isSyncing,
                            themeMode = themeMode,
                            onOpenServer = { index, openSysPulse ->
                                devices.getOrNull(index)?.let { target ->
                                    selectedServerId = target.id
                                    isServiceDetailOpen = false
                                    selectedTab = if (openSysPulse) MainTab.SYSPULSE else MainTab.CONTROL
                                }
                            },
                            onAssignAccentColor = { deviceId, hex ->
                                repository.updateDeviceAccent(deviceId, hex)
                                devices = repository.getDevices()
                            },
                            onSyncAllR2 = { syncAllR2(devices) },
                            onRefreshAll = { refreshAllStatuses(devices) }
                        )
                    }
                }
            }
        }
    }

    if (useServerSeedTheme && selectedDevice != null) {
        ServerMaterialTheme(
            seedHex = selectedDevice.accentColorHex,
            themeMode = themeMode
        ) {
            chromeContent()
        }
    } else {
        chromeContent()
    }

    // ── GLOBAL MODALS & DIALOGS (rendered in GlobalAppTheme) ────────────────
    if (showHistoryDialog || showSettingsDialog || showNfcWriteDialog || (showUpdateDialog && availableUpdate != null)) {
        GlobalAppTheme(
            themeMode = themeMode,
            dynamicColor = isDynamicColorEnabled
        ) {
            if (showHistoryDialog) {
                HistoryDialog(
                    logs = historyLogs,
                    onClearOlderThan = { durationMillis ->
                        historyRepository.clearOlderThan(durationMillis)
                        historyLogs = historyRepository.getLogs()
                    },
                    onClearAll = {
                        historyRepository.clearAll()
                        historyLogs = emptyList()
                    },
                    onDismiss = { showHistoryDialog = false }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    isBiometricEnabled = isBiometricEnabled,
                    onBiometricToggled = { enabled ->
                        appSettings.isBiometricEnabled = enabled
                        isBiometricEnabled = enabled
                    },
                    isWidgetBiometricEnabled = isWidgetBiometricEnabled,
                    onWidgetBiometricToggled = { enabled ->
                        appSettings.isWidgetBiometricEnabled = enabled
                        isWidgetBiometricEnabled = enabled
                    },
                    isBgMonitorEnabled = isBgMonitorEnabled,
                    onBgMonitorToggled = { enabled ->
                        appSettings.isBackgroundMonitoringEnabled = enabled
                        isBgMonitorEnabled = enabled
                        if (enabled) {
                            ServerMonitorWorker.schedule(context)
                            AppUpdateWorker.schedule(context)
                        } else {
                            ServerMonitorWorker.cancel(context)
                        }
                    },
                    isBiometricAvailable = isBiometricAvailable,
                    isNfcAvailable = isNfcAvailable,
                    isNotificationPermissionGranted = isNotificationPermissionGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onSendTestNotification = onSendTestNotification,
                    onOpenNfcWriter = { showNfcWriteDialog = true },
                    themeMode = themeMode,
                    onThemeModeChanged = onThemeModeChanged,
                    isDynamicColorEnabled = isDynamicColorEnabled,
                    onDynamicColorToggled = onDynamicColorToggled,
                    onDismiss = { showSettingsDialog = false }
                )
            }

            if (showNfcWriteDialog) {
                NfcWriteDialog(
                    devices = devices,
                    writeResult = nfcWriteResult,
                    onStartWriting = onStartNfcWriting,
                    onStopWriting = onStopNfcWriting,
                    onDismiss = {
                        onStopNfcWriting()
                        showNfcWriteDialog = false
                    }
                )
            }

            if (showUpdateDialog && availableUpdate != null) {
                val release = availableUpdate!!
                UpdateDialog(
                    releaseInfo = release,
                    currentVersion = BuildConfig.VERSION_NAME,
                    downloadState = downloadState,
                    onDownload = {
                        updateManager.downloadApk(
                            downloadUrl = release.downloadUrl,
                            assetApiUrl = release.assetApiUrl,
                            expectedSizeBytes = release.sizeBytes
                        ) { state ->
                            downloadState = state
                        }
                    },
                    onInstall = { apkFile ->
                        updateManager.installApk(apkFile)
                        showUpdateDialog = false
                    },
                    onDismiss = { showUpdateDialog = false }
                )
            }
        }
    }
}

@Composable
private fun ServerSwitcherStrip(
    devices: List<DeviceConfig>,
    selectedServerId: String?,
    nodeStatuses: Map<String, NodeStatus>,
    onSelectServer: (DeviceConfig) -> Unit
) {
    val dark = LocalIsDarkTheme.current
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices, key = { it.id }) { dev ->
            val selected = dev.id == selectedServerId
            val status = nodeStatuses[dev.id]
            val seedColor = ServerAccents.parseColor(dev.accentColorHex)
            val dotColor = when {
                status == null -> seedColor
                status.isOnline -> PixelSemantic.onlineAccent(dark)
                else -> MaterialTheme.colorScheme.error
            }

            FilterChip(
                selected = selected,
                onClick = { onSelectServer(dev) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                },
                label = {
                    Text(
                        text = dev.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(100),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun EmptyServersView(
    onAddServer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Servers Added",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connect a server via Cloudflare R2 JSON or manual endpoints to manage power controls and SysPulse telemetry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onAddServer,
                    shape = RoundedCornerShape(100)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Server")
                }
            }
        }
    }
}
