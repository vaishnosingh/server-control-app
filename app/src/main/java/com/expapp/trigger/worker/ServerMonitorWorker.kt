package com.expapp.trigger.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expapp.trigger.data.repository.AppSettings
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.SharedPrefsStorage
import com.expapp.trigger.network.TriggerClient
import java.util.concurrent.TimeUnit

class ServerMonitorWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "server_status_periodic_monitor"
        const val ONE_TIME_WORK_NAME = "server_status_one_time_check"

        fun schedule(context: Context, repeatIntervalMinutes: Long = 15) {
            NotificationHelper.createNotificationChannel(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ServerMonitorWorker>(
                repeatIntervalMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun runOnce(context: Context) {
            NotificationHelper.createNotificationChannel(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<ServerMonitorWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
        }

        fun createNotificationChannel(context: Context) {
            NotificationHelper.createNotificationChannel(context)
        }
    }

    override suspend fun doWork(): Result {
        val appSettings = AppSettings(appContext)
        if (!appSettings.isBackgroundMonitoringEnabled) {
            return Result.success()
        }

        val repository = DeviceRepository(SharedPrefsStorage(appContext))
        val devices = repository.getDevices()
        if (devices.isEmpty()) {
            return Result.success()
        }

        val triggerClient = TriggerClient()

        for (device in devices) {
            if (device.statusUrl.isBlank()) continue

            val status = triggerClient.checkStatusSync(device)
            
            // If the endpoint could not be reached (e.g. phone has no internet / network failure),
            // do NOT treat this as the server going offline. Maintain the last known state.
            if (!status.isReachable) {
                continue
            }

            val previousStatus = appSettings.getLastStatus(device.id)

            // Save new status
            appSettings.setLastStatus(device.id, status.isOnline)

            // Determine if notification is needed only when server actually transitions
            if (previousStatus != null && previousStatus != status.isOnline) {
                NotificationHelper.sendNotification(
                    context = appContext,
                    id = device.id.hashCode(),
                    title = if (status.isOnline) "✅ Server Online" else "⚠️ Server Offline",
                    message = if (status.isOnline) {
                        "${device.name} is now online and reachable."
                    } else {
                        "${device.name} has powered off or is offline."
                    },
                    isSuccess = status.isOnline
                )
            }
        }

        // Check for app updates in the background synchronously and notify if a new update is found
        try {
            val updateManager = com.expapp.trigger.update.UpdateManager(appContext)
            val currentVersion = com.expapp.trigger.BuildConfig.VERSION_NAME
            val result = updateManager.checkForUpdateSync(currentVersion)
            if (result is com.expapp.trigger.update.UpdateCheckResult.UpdateAvailable) {
                val newVer = result.info.versionName
                val lastNotified = appSettings.lastNotifiedUpdateVersion
                if (lastNotified != newVer) {
                    appSettings.lastNotifiedUpdateVersion = newVer
                    NotificationHelper.sendUpdateAvailableNotification(appContext, newVer)
                }
            }
        } catch (_: Exception) {
            // Ignore background check failure
        }

        return Result.success()
    }
}
