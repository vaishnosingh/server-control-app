package com.expapp.trigger.data.repository

import android.content.Context

class AppSettings(private val storage: KeyValueStorage) {

    constructor(context: Context) : this(SharedPrefsStorage(context, "app_settings_prefs"))

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_WIDGET_BIOMETRIC_ENABLED = "key_widget_biometric_enabled"
        private const val KEY_BG_MONITOR_ENABLED = "key_bg_monitor_enabled"
        private const val KEY_LAST_STATUS_PREFIX = "last_status_"
        private const val KEY_LAST_NOTIFIED_VERSION = "key_last_notified_version"
    }

    var lastNotifiedUpdateVersion: String?
        get() = storage.getString(KEY_LAST_NOTIFIED_VERSION, null)
        set(value) {
            if (value != null) storage.putString(KEY_LAST_NOTIFIED_VERSION, value)
            else storage.remove(KEY_LAST_NOTIFIED_VERSION)
        }

    var isBiometricEnabled: Boolean
        get() = storage.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = storage.putBoolean(KEY_BIOMETRIC_ENABLED, value)

    var isWidgetBiometricEnabled: Boolean
        get() = storage.getBoolean(KEY_WIDGET_BIOMETRIC_ENABLED, false)
        set(value) = storage.putBoolean(KEY_WIDGET_BIOMETRIC_ENABLED, value)

    var isBackgroundMonitoringEnabled: Boolean
        get() = storage.getBoolean(KEY_BG_MONITOR_ENABLED, true)
        set(value) = storage.putBoolean(KEY_BG_MONITOR_ENABLED, value)

    fun getLastStatus(deviceId: String): Boolean? {
        val key = "$KEY_LAST_STATUS_PREFIX$deviceId"
        return if (storage.contains(key)) storage.getBoolean(key, false) else null
    }

    fun setLastStatus(deviceId: String, isOnline: Boolean) {
        val key = "$KEY_LAST_STATUS_PREFIX$deviceId"
        storage.putBoolean(key, isOnline)
    }
}
