package com.expapp.trigger.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expapp.trigger.MainActivity
import com.expapp.trigger.R

object NotificationHelper {
    const val CHANNEL_ID = "server_status_channel"
    const val CHANNEL_NAME = "Server Status Alerts"

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your servers go offline or come back online"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                enableLights(true)
                lightColor = Color.GREEN
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        isSuccess: Boolean = true,
        extraAction: String? = null
    ): Boolean {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (extraAction != null) {
                putExtra("action_trigger", extraAction)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val accentColor = if (isSuccess) 0xFF22C55E.toInt() else 0xFFFF4757.toInt()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun sendUpdateAvailableNotification(
        context: Context,
        newVersion: String,
        releaseTitle: String? = null
    ): Boolean {
        val title = if (!releaseTitle.isNullOrBlank() && releaseTitle != "v$newVersion") {
            "🚀 Update Available: $releaseTitle"
        } else {
            "🚀 Update Available: v$newVersion"
        }
        return sendNotification(
            context = context,
            id = 8888,
            title = title,
            message = "A new version (v$newVersion) of Device Trigger is ready to install. Tap to update!",
            isSuccess = true,
            extraAction = "open_update"
        )
    }

    fun sendTestNotification(context: Context): Boolean {
        return sendNotification(
            context = context,
            id = 9999,
            title = "🔔 Test Alert: Notifications Working",
            message = "Server status notifications are properly configured and working on this device!",
            isSuccess = true
        )
    }
}
