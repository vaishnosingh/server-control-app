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
import android.widget.RemoteViews
import android.widget.Toast
import com.expapp.trigger.MainActivity
import com.expapp.trigger.R
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.TriggerLog
import com.expapp.trigger.data.repository.AppSettings
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.SharedPrefsStorage
import com.expapp.trigger.data.repository.TriggerHistoryRepository
import com.expapp.trigger.network.NodeStatus
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.worker.ActionTriggerWorker
import java.util.concurrent.Executors

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

                // If device exists, fetch live status asynchronously
                if (device != null && device.statusUrl.isNotBlank()) {
                    executor.execute {
                        try {
                            val status = TriggerClient().checkStatusSync(device)
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
                val action = try { TriggerAction.valueOf(actionName) } catch (e: Exception) { return }

                val appSettings = AppSettings(context)
                if (appSettings.isBiometricEnabled) {
                    // Biometric is enabled: open MainActivity with Trigger intent
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        this.action = ACTION_TRIGGER
                        putExtra(EXTRA_DEVICE_ID, deviceId)
                        putExtra(EXTRA_ACTION, actionName)
                    }
                    context.startActivity(launchIntent)
                } else {
                    // Biometric disabled: execute via guaranteed background worker
                    Toast.makeText(context, "Sending ${action.name} signal...", Toast.LENGTH_SHORT).show()
                    ActionTriggerWorker.enqueue(context, deviceId, action, "Widget")
                }
            }

            ACTION_REFRESH -> {
                executor.execute {
                    val repository = DeviceRepository(SharedPrefsStorage(context))
                    val device = repository.getDevices().firstOrNull()
                    if (device != null) {
                        val status = TriggerClient().checkStatusSync(device)
                        mainHandler.post {
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val thisWidget = ComponentName(context, ServerWidgetProvider::class.java)
                            val ids = appWidgetManager.getAppWidgetIds(thisWidget)
                            for (id in ids) {
                                renderWidget(context, appWidgetManager, id, device, status, "Updated just now")
                            }
                            Toast.makeText(context, "Widget refreshed", Toast.LENGTH_SHORT).show()
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
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_gray)
                views.setTextViewText(R.id.widget_footer_text, "Open app to add server")

                val appIntent = Intent(context, MainActivity::class.java)
                val pendingApp = PendingIntent.getActivity(
                    context, 0, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                views.setOnClickPendingIntent(R.id.widget_header_clickable, pendingApp)
                views.setOnClickPendingIntent(R.id.widget_root, pendingApp)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            views.setTextViewText(R.id.widget_device_name, device.name)
            views.setTextViewText(R.id.widget_node_name, device.node)

            // Status display
            if (status == null) {
                views.setTextViewText(R.id.widget_status_text, "SYNCING")
                views.setTextColor(R.id.widget_status_text, 0xFF71717A.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_gray)
            } else if (status.isOnline) {
                views.setTextViewText(R.id.widget_status_text, "ONLINE")
                views.setTextColor(R.id.widget_status_text, 0xFF22C55E.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_green)
            } else {
                views.setTextViewText(R.id.widget_status_text, "OFFLINE")
                views.setTextColor(R.id.widget_status_text, 0xFFEF4444.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_dot_red)
            }

            views.setTextViewText(
                R.id.widget_footer_text,
                footerOverride ?: (if (status?.uptime != null) "Up: ${status.uptime}" else "Tap header to open • Tap button to trigger")
            )

            // Header click -> Open app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingOpenApp = PendingIntent.getActivity(
                context,
                appWidgetId * 10,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            views.setOnClickPendingIntent(R.id.widget_header_clickable, pendingOpenApp)

            // Refresh click
            val refreshIntent = Intent(context, ServerWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingRefresh = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, pendingRefresh)

            // Power On click
            views.setOnClickPendingIntent(
                R.id.widget_btn_power_on,
                createTriggerPendingIntent(context, appWidgetId * 10 + 2, device.id, TriggerAction.POWER_ON)
            )

            // Reboot click
            views.setOnClickPendingIntent(
                R.id.widget_btn_reboot,
                createTriggerPendingIntent(context, appWidgetId * 10 + 3, device.id, TriggerAction.REBOOT)
            )

            // Power Off click
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
}
