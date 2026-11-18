package com.expapp.trigger

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.TriggerLog
import com.expapp.trigger.data.repository.AppSettings
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.SharedPrefsStorage
import com.expapp.trigger.data.repository.TriggerHistoryRepository
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.nfc.NfcHelper
import com.expapp.trigger.nfc.NfcTriggerPayload
import com.expapp.trigger.security.BiometricHelper
import com.expapp.trigger.ui.screens.MainScreen
import com.expapp.trigger.ui.theme.DarkBackground
import com.expapp.trigger.ui.theme.ExpAppTheme
import com.expapp.trigger.widget.ServerWidgetProvider
import com.expapp.trigger.worker.ServerMonitorWorker

class MainActivity : FragmentActivity() {

    private lateinit var repository: DeviceRepository
    private lateinit var triggerClient: TriggerClient
    private lateinit var appSettings: AppSettings
    private lateinit var historyRepository: TriggerHistoryRepository

    private var activeNfcWritePayload by mutableStateOf<NfcTriggerPayload?>(null)
    private var nfcWriteResult by mutableStateOf<Result<Unit>?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted && appSettings.isBackgroundMonitoringEnabled) {
                ServerMonitorWorker.schedule(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = DeviceRepository(SharedPrefsStorage(applicationContext))
        triggerClient = TriggerClient()
        appSettings = AppSettings(applicationContext)
        historyRepository = TriggerHistoryRepository(applicationContext)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Initialize background server monitoring
        if (appSettings.isBackgroundMonitoringEnabled) {
            ServerMonitorWorker.createNotificationChannel(this)
            ServerMonitorWorker.schedule(this)
        }

        // Handle initial launch intent (from NFC or Widget)
        handleIntent(intent)

        setContent {
            ExpAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainScreen(
                        repository = repository,
                        triggerClient = triggerClient,
                        appSettings = appSettings,
                        historyRepository = historyRepository,
                        requestBiometricAuth = { title, subtitle, onResult ->
                            BiometricHelper.authenticate(this, title, subtitle) { success, _ ->
                                onResult(success)
                            }
                        },
                        nfcWriteResult = nfcWriteResult,
                        onStartNfcWriting = { payload ->
                            nfcWriteResult = null
                            activeNfcWritePayload = payload
                        },
                        onStopNfcWriting = {
                            activeNfcWritePayload = null
                            nfcWriteResult = null
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (NfcHelper.isNfcSupported(this)) {
            NfcHelper.enableForegroundDispatch(this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (NfcHelper.isNfcSupported(this)) {
            NfcHelper.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // 1. NFC Tag write handling
        val writePayload = activeNfcWritePayload
        if (writePayload != null) {
            @Suppress("DEPRECATION")
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tag != null) {
                val message = NfcHelper.createNdefMessage(writePayload.nodeOrDeviceId, writePayload.action)
                val result = NfcHelper.writeTag(tag, message)
                nfcWriteResult = result
                vibrateFeedback(result.isSuccess)
                return
            }
        }

        // 2. NFC Tag trigger execution
        val nfcPayload = NfcHelper.parseTriggerPayload(intent)
        if (nfcPayload != null) {
            val devices = repository.getDevices()
            val device = devices.find { it.node.equals(nfcPayload.nodeOrDeviceId, ignoreCase = true) || it.id == nfcPayload.nodeOrDeviceId }
            if (device != null) {
                executeExternalTrigger(device, nfcPayload.action, "NFC")
            } else {
                Toast.makeText(this, "NFC Tag: Unknown server (${nfcPayload.nodeOrDeviceId})", Toast.LENGTH_LONG).show()
            }
            return
        }

        // 3. Widget biometric trigger launch
        if (intent.action == ServerWidgetProvider.ACTION_TRIGGER) {
            val deviceId = intent.getStringExtra(ServerWidgetProvider.EXTRA_DEVICE_ID)
            val actionName = intent.getStringExtra(ServerWidgetProvider.EXTRA_ACTION)
            if (deviceId != null && actionName != null) {
                val action = try { TriggerAction.valueOf(actionName) } catch (e: Exception) { null }
                val device = repository.getDevices().find { it.id == deviceId }
                if (device != null && action != null) {
                    executeExternalTrigger(device, action, "Widget")
                }
            }
        }
    }

    private fun executeExternalTrigger(device: DeviceConfig, action: TriggerAction, source: String) {
        val actionName = when (action) {
            TriggerAction.POWER_ON -> "Power On"
            TriggerAction.REBOOT -> "Reboot"
            TriggerAction.POWER_OFF -> "Shutdown"
        }

        val onAuthorized = {
            vibrateFeedback(true)
            Toast.makeText(this, "Sending $actionName signal to ${device.name}...", Toast.LENGTH_SHORT).show()
            triggerClient.sendTrigger(device, action) { response ->
                val msg = if (response.isSuccess) {
                    "$actionName signal sent successfully"
                } else {
                    "Failed: ${response.message.take(50)}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                historyRepository.addLog(
                    TriggerLog(
                        deviceId = device.id,
                        deviceName = device.name,
                        node = device.node,
                        action = action,
                        isSuccess = response.isSuccess,
                        statusCode = response.statusCode,
                        message = response.message,
                        source = source
                    )
                )
                ServerWidgetProvider.updateAllWidgets(this)
            }
        }

        if (appSettings.isBiometricEnabled && BiometricHelper.isBiometricAvailable(this)) {
            BiometricHelper.authenticate(
                this,
                title = "Confirm $actionName",
                subtitle = "Authenticate with fingerprint to trigger ${device.name}"
            ) { success, errorMsg ->
                if (success) {
                    onAuthorized()
                } else if (errorMsg != null) {
                    Toast.makeText(this, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            onAuthorized()
        }
    }

    private fun vibrateFeedback(success: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        if (success) 80 else 200,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(
                            if (success) 80 else 200,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(if (success) 80 else 200)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration is not supported
        }
    }
}
