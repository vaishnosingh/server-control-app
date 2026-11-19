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
import com.expapp.trigger.BuildConfig
import com.expapp.trigger.data.repository.AppSettings
import com.expapp.trigger.update.UpdateCheckResult
import com.expapp.trigger.update.UpdateManager
import java.util.concurrent.TimeUnit

/**
 * Dedicated background worker that checks for new app updates independently of server devices.
 * Ensures notifications are dispatched even if no servers are configured or if servers are offline.
 */
class AppUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "app_update_periodic_check"
        const val ONE_TIME_WORK_NAME = "app_update_one_time_check"

        fun schedule(context: Context, repeatIntervalMinutes: Long = 15) {
            NotificationHelper.createNotificationChannel(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(
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

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun runOnce(context: Context) {
            NotificationHelper.createNotificationChannel(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<AppUpdateWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val appSettings = AppSettings(appContext)

        try {
            val updateManager = UpdateManager(appContext)
            val currentVersion = BuildConfig.VERSION_NAME
            val result = updateManager.checkForUpdateSync(currentVersion)

            if (result is UpdateCheckResult.UpdateAvailable) {
                val newVer = result.info.versionName
                val lastNotified = appSettings.lastNotifiedUpdateVersion

                if (lastNotified != newVer) {
                    val notified = NotificationHelper.sendUpdateAvailableNotification(
                        context = appContext,
                        newVersion = newVer,
                        releaseTitle = result.info.title
                    )
                    if (notified) {
                        appSettings.lastNotifiedUpdateVersion = newVer
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore failure; will retry next interval
        }

        return Result.success()
    }
}
