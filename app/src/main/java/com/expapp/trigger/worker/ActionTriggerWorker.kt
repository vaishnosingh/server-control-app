package com.expapp.trigger.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.TriggerLog
import com.expapp.trigger.data.repository.AppSettings
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.SharedPrefsStorage
import com.expapp.trigger.data.repository.TriggerHistoryRepository
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.widget.ServerWidgetProvider
import kotlinx.coroutines.delay

class ActionTriggerWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_ACTION = "action"
        const val KEY_SOURCE = "source"

        /**
         * Enqueues guaranteed background execution of a server power action.
         * Runs even if the user immediately closes the app, locks phone, or clears recents.
         */
        fun enqueue(
            context: Context,
            deviceId: String,
            action: TriggerAction,
            source: String = "App"
        ) {
            NotificationHelper.createNotificationChannel(context)

            val inputData = Data.Builder()
                .putString(KEY_DEVICE_ID, deviceId)
                .putString(KEY_ACTION, action.name)
                .putString(KEY_SOURCE, source)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ActionTriggerWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "action_trigger_${deviceId}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val deviceId = inputData.getString(KEY_DEVICE_ID) ?: return Result.failure()
        val actionName = inputData.getString(KEY_ACTION) ?: return Result.failure()
        val source = inputData.getString(KEY_SOURCE) ?: "App"

        val action = try {
            TriggerAction.valueOf(actionName)
        } catch (e: Exception) {
            return Result.failure()
        }

        val repository = DeviceRepository(SharedPrefsStorage(appContext))
        val device = repository.getDevices().find { it.id == deviceId } ?: return Result.failure()

        val actionTitle = when (action) {
            TriggerAction.POWER_ON  -> "Power On"
            TriggerAction.REBOOT    -> "Reboot"
            TriggerAction.POWER_OFF -> "Shutdown"
        }

        val triggerClient = TriggerClient()
        val historyRepo = TriggerHistoryRepository(appContext)
        val appSettings = AppSettings(appContext)

        // 1. Execute the HTTP trigger to the server
        val response = triggerClient.sendTriggerSync(device, action)

        // 2. Post notification immediately confirming the signal was sent (or failed)
        val signalMsg = if (response.isSuccess) {
            when (action) {
                TriggerAction.POWER_ON  -> "Power on signal sent successfully to ${device.name}"
                TriggerAction.POWER_OFF -> "Shutdown signal sent to ${device.name}"
                TriggerAction.REBOOT    -> "Reboot signal sent to ${device.name}"
            }
        } else {
            val raw = response.message.trim()
            val clean = if (raw.startsWith("<")) "HTTP ${response.statusCode}" else raw.take(60)
            "Failed to send $actionTitle: $clean"
        }

        // 3. Save to history log
        historyRepo.addLog(
            TriggerLog(
                deviceId = device.id,
                deviceName = device.name,
                node = device.node,
                action = action,
                isSuccess = response.isSuccess,
                statusCode = response.statusCode,
                message = signalMsg,
                source = source
            )
        )

        // 4. Update home screen widgets
        ServerWidgetProvider.updateAllWidgets(appContext)

        // 5. If trigger succeeded and device has a status endpoint, actively poll until state changes!
        if (response.isSuccess && device.statusUrl.isNotBlank()) {
            pollForTransition(device, action, triggerClient, appSettings)
        }

        return Result.success()
    }

    private suspend fun pollForTransition(
        device: DeviceConfig,
        action: TriggerAction,
        triggerClient: TriggerClient,
        appSettings: AppSettings
    ) {
        val maxAttempts = 30 // Up to 60 seconds (30 attempts * 2s) for instant notification
        var attempts = 0
        var consecutiveTargetMatches = 0

        // Brief delay before first check to allow server to receive trigger
        delay(1500L)

        while (attempts < maxAttempts) {
            attempts++
            val status = triggerClient.checkStatusSync(device)

            // If phone network is unavailable, do not misinterpret as server going offline
            if (!status.isReachable) {
                delay(2000L)
                continue
            }

            when (action) {
                TriggerAction.POWER_ON -> {
                    if (status.isOnline) {
                        consecutiveTargetMatches++
                        // Require 2 consecutive online confirmations (or 1 if status explicitly verified)
                        if (consecutiveTargetMatches >= 2 || status.statusText == "ONLINE") {
                            appSettings.setLastStatus(device.id, true)
                            NotificationHelper.sendNotification(
                                context = appContext,
                                id = device.id.hashCode(),
                                title = "✅ Server Online",
                                message = "${device.name} is now online and reachable.",
                                isSuccess = true
                            )
                            ServerWidgetProvider.updateAllWidgets(appContext)
                            return
                        }
                    } else {
                        consecutiveTargetMatches = 0
                    }
                }
                TriggerAction.POWER_OFF -> {
                    if (!status.isOnline) {
                        consecutiveTargetMatches++
                        // Require 2 consecutive offline confirmations to completely eliminate false positives
                        if (consecutiveTargetMatches >= 2) {
                            appSettings.setLastStatus(device.id, false)
                            NotificationHelper.sendNotification(
                                context = appContext,
                                id = device.id.hashCode(),
                                title = "⚠️ Server Offline",
                                message = "${device.name} has powered off or is offline.",
                                isSuccess = false
                            )
                            ServerWidgetProvider.updateAllWidgets(appContext)
                            return
                        }
                    } else {
                        consecutiveTargetMatches = 0
                    }
                }
                TriggerAction.REBOOT -> {
                    // No notification for reboot per user instructions
                    if (attempts >= 4 && status.isOnline) {
                        appSettings.setLastStatus(device.id, true)
                        ServerWidgetProvider.updateAllWidgets(appContext)
                        return
                    }
                }
            }

            delay(2000L)
        }
    }
}
