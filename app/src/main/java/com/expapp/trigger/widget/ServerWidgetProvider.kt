package com.expapp.trigger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews
import android.widget.Toast
import com.expapp.trigger.MainActivity
import com.expapp.trigger.R
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.repository.AppSettings
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.SharedPrefsStorage
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.worker.ActionTriggerWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Self-contained Home Screen Widget for server control and monitoring.
 * Executes server triggers and status refreshes completely in the background
 * without opening the main application.
 */
class ServerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TRIGGER = "com.expapp.trigger.widget.ACTION_TRIGGER"
        const val ACTION_REFRESH = "com.expapp.trigger.widget.ACTION_REFRESH"
        const val ACTION_CYCLE_DEVICE = "com.expapp.trigger.widget.ACTION_CYCLE_DEVICE"

        const val EXTRA_DEVICE_ID = "extra_device_id"
        const val EXTRA_ACTION = "extra_action"

        private val executor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ServerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, ServerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        fun updateStatus(context: Context, statusText: String, footerText: String? = null) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, ServerWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (id in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_server_control)
                        views.setTextViewText(R.id.widget_status_text, statusText)
                        if (statusText == "SENDING..." || statusText == "CHECKING...") {
                            views.setTextColor(R.id.widget_status_text, 0xFFF59E0B.toInt())
                            views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_gray)
                            views.setInt(R.id.widget_status_chip, "setBackgroundResource", R.drawable.widget_chip_checking)
                        }
                        if (footerText != null) {
                            views.setTextViewText(R.id.widget_footer_text, footerText)
                        }
                        appWidgetManager.partiallyUpdateAppWidget(id, views)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            val repository = DeviceRepository(SharedPrefsStorage(context))
            val devices = repository.getDevices()

            for (appWidgetId in appWidgetIds) {
                val device = devices.firstOrNull()
                renderWidget(context, appWidgetManager, appWidgetId, device, null)

                // If device exists, fetch live status asynchronously in background
                if (device != null && device.statusUrl.isNotBlank()) {
                    executor.execute {
                        try {
                            val status = TriggerClient().checkStatusSync(device)
                            val appSettings = AppSettings(context)
                            appSettings.setLastStatus(device.id, status.isOnline)
                            mainHandler.post {
                                try {
                                    renderWidget(context, appWidgetManager, appWidgetId, device, status)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TRIGGER -> {
                val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return
                val actionName = intent.getStringExtra(EXTRA_ACTION) ?: return
                val action = try { TriggerAction.valueOf(actionName) } catch (_: Exception) { return }

                val appSettings = AppSettings(context)
                if (appSettings.isWidgetBiometricEnabled && appSettings.isBiometricEnabled) {
                    // Biometric is explicitly required for widget: launch transparent prompt over home screen
                    val launchIntent = Intent(context, WidgetAuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        putExtra(EXTRA_DEVICE_ID, deviceId)
                        putExtra(EXTRA_ACTION, actionName)
                    }
                    context.startActivity(launchIntent)
                } else {
                    // Frictionless 1-tap trigger directly from home screen without opening the app
                    val repository = DeviceRepository(SharedPrefsStorage(context))
                    val device = repository.getDevices().find { it.id == deviceId }
                    val deviceName = device?.name ?: "Server"
                    val actionTitle = when (action) {
                        TriggerAction.POWER_ON  -> "Power On"
                        TriggerAction.REBOOT    -> "Reboot"
                        TriggerAction.POWER_OFF -> "Shutdown"
                    }

                    vibrate(context)
                    Toast.makeText(context, "⚡ Sending $actionTitle signal to $deviceName...", Toast.LENGTH_SHORT).show()
                    updateStatus(context, "SENDING...", "⚡ Dispatched $actionTitle • Verifying...")
                    ActionTriggerWorker.enqueue(context, deviceId, action, "Widget")
                }
            }

            ACTION_REFRESH -> {
                // Immediate visual feedback on home screen
                updateStatus(context, "CHECKING...", "🔄 Refreshing server status...")
                Toast.makeText(context, "Checking server status...", Toast.LENGTH_SHORT).show()
                vibrate(context)

                executor.execute {
                    val repository = DeviceRepository(SharedPrefsStorage(context))
                    val device = repository.getDevices().firstOrNull()
                    if (device != null) {
                        val status = TriggerClient().checkStatusSync(device)
                        val appSettings = AppSettings(context)
                        appSettings.setLastStatus(device.id, status.isOnline)

                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val footer = if (status.isOnline) {
                            if (!status.uptime.isNullOrBlank()) "🟢 Up: ${status.uptime} • Checked $timeStr"
                            else "🟢 Online • Checked $timeStr"
                        } else {
                            "🛑 Server is offline • Checked $timeStr"
                        }

                        mainHandler.post {
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val thisWidget = ComponentName(context, ServerWidgetProvider::class.java)
                            val ids = appWidgetManager.getAppWidgetIds(thisWidget)
                            for (id in ids) {
                                renderWidget(context, appWidgetManager, id, device, status, footer)
                            }
                            val toastMsg = if (status.isOnline) "🟢 ${device.name} is online" else "🛑 ${device.name} is offline"
                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        device: DeviceConfig?,
        status: NodeStatus?,
        footerOverride: String? = null
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_server_control)

            if (device == null) {
                views.setTextViewText(R.id.widget_device_name, "No Device")
                views.setTextViewText(R.id.widget_node_name, "Configure in app")
                views.setTextViewText(R.id.widget_status_text, "OFFLINE")
                views.setTextColor(R.id.widget_status_text, 0xFFEF4444.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_red)
                views.setInt(R.id.widget_status_chip, "setBackgroundResource", R.drawable.widget_chip_offline)
                views.setTextViewText(R.id.widget_footer_text, "Tap icon to open app & add server")

                // Only icon opens app when no device configured
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingApp = PendingIntent.getActivity(
                    context, 0, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                views.setOnClickPendingIntent(R.id.widget_app_icon_badge, pendingApp)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            views.setTextViewText(R.id.widget_device_name, device.name)
            views.setTextViewText(R.id.widget_node_name, "${device.node} • Server Control")

            // Status chip display
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            if (status == null) {
                views.setTextViewText(R.id.widget_status_text, "CHECKING")
                views.setTextColor(R.id.widget_status_text, 0xFF9CA3AF.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_gray)
                views.setInt(R.id.widget_status_chip, "setBackgroundResource", R.drawable.widget_chip_checking)
            } else if (status.isOnline) {
                views.setTextViewText(R.id.widget_status_text, "ONLINE")
                views.setTextColor(R.id.widget_status_text, 0xFF22C55E.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_green)
                views.setInt(R.id.widget_status_chip, "setBackgroundResource", R.drawable.widget_chip_online)
            } else {
                views.setTextViewText(R.id.widget_status_text, "OFFLINE")
                views.setTextColor(R.id.widget_status_text, 0xFFEF4444.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_red)
                views.setInt(R.id.widget_status_chip, "setBackgroundResource", R.drawable.widget_chip_offline)
            }

            // Contextual Footer Text
            val defaultFooter = when {
                status == null -> "Checking server status..."
                status.isOnline -> if (!status.uptime.isNullOrBlank()) "🟢 Up: ${status.uptime} • Checked $timeStr" else "🟢 Online • Checked $timeStr"
                else -> "🛑 Offline • Tap Refresh to check"
            }
            views.setTextViewText(R.id.widget_footer_text, footerOverride ?: defaultFooter)

            // Dedicated Refresh Broadcast Intent
            val refreshIntent = Intent(context, ServerWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingRefresh = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            // Refresh button & Info row triggers Refresh (NOT app launch)
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, pendingRefresh)
            views.setOnClickPendingIntent(R.id.widget_header_info, pendingRefresh)
            views.setOnClickPendingIntent(R.id.widget_footer_text, pendingRefresh)

            // Explicit App Icon Badge Intent (Only this small terminal icon opens MainActivity if desired)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingOpenApp = PendingIntent.getActivity(
                context,
                appWidgetId * 10,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            views.setOnClickPendingIntent(R.id.widget_app_icon_badge, pendingOpenApp)

            // Power On click (1-tap background action)
            views.setOnClickPendingIntent(
                R.id.widget_btn_power_on,
                createTriggerPendingIntent(context, appWidgetId * 10 + 2, device.id, TriggerAction.POWER_ON)
            )

            // Reboot click (1-tap background action)
            views.setOnClickPendingIntent(
                R.id.widget_btn_reboot,
                createTriggerPendingIntent(context, appWidgetId * 10 + 3, device.id, TriggerAction.REBOOT)
            )

            // Power Off click (1-tap background action)
            views.setOnClickPendingIntent(
                R.id.widget_btn_power_off,
                createTriggerPendingIntent(context, appWidgetId * 10 + 4, device.id, TriggerAction.POWER_OFF)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createTriggerPendingIntent(
        context: Context,
        requestCode: Int,
        deviceId: String,
        action: TriggerAction
    ): PendingIntent {
        val intent = Intent(context, ServerWidgetProvider::class.java).apply {
            this.action = ACTION_TRIGGER
            putExtra(EXTRA_DEVICE_ID, deviceId)
            putExtra(EXTRA_ACTION, action.name)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}
    }
}
