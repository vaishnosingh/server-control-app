package com.expapp.trigger.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
                description = "Alerts when your servers go offline, come online, or receive updates"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                enableLights(true)
                lightColor = Color.GREEN
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Renders a high-contrast 192x192 branded circular badge containing our monochrome
     * terminal emblem so both Android notification shades and smartwatch companion
     * bridges (e.g. Nothing CMF Watch Pro 2 / Wear OS) display a crisp app icon.
     */
    private fun createLargeBadgeBitmap(context: Context, accentColor: Int): Bitmap? {
        return try {
            val size = 192
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF0B0F19.toInt()
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }

            val rect = RectF(6f, 6f, size - 6f, size - 6f)
            canvas.drawRoundRect(rect, 48f, 48f, bgPaint)
            canvas.drawRoundRect(rect, 48f, 48f, borderPaint)

            val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_notification)?.mutate()
            if (iconDrawable != null) {
                iconDrawable.setTint(accentColor)
                val pad = 42
                iconDrawable.setBounds(pad, pad, size - pad, size - pad)
                iconDrawable.draw(canvas)
            }
            bitmap
        } catch (_: Throwable) {
            null
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

        val accentColor = if (isSuccess) 0xFF22D47E.toInt() else 0xFFFF4757.toInt()
        val largeIcon = createLargeBadgeBitmap(context, accentColor)
        val category = if (extraAction == "open_update") {
            NotificationCompat.CATEGORY_RECOMMENDATION
        } else {
            NotificationCompat.CATEGORY_STATUS
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(message)
            .setSubText(context.getString(R.string.app_name))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(category)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .extend(NotificationCompat.WearableExtender())

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        return try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
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
            "Update Available: $releaseTitle"
        } else {
            "Update Available: v$newVersion"
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
            title = "Notifications Configured",
            message = "Server alerts and smartwatch icon forwarding are working on this device!",
            isSuccess = true
        )
    }
}
