package com.expapp.trigger.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.expapp.trigger.data.model.DeviceConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

interface KeyValueStorage {
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String?)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
    fun putBoolean(key: String, value: Boolean) {}
    fun contains(key: String): Boolean = false
    fun remove(key: String) {}
}

class SharedPrefsStorage(
    context: Context,
    prefsName: String = "device_trigger_prefs"
) : KeyValueStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun getString(key: String, defaultValue: String?): String? =
        prefs.getString(key, defaultValue)

    override fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun contains(key: String): Boolean =
        prefs.contains(key)

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

class DeviceRepository(private val storage: KeyValueStorage) {
    private val gson = Gson()
    private val storageKey = "saved_devices_json"

    fun getDevices(): List<DeviceConfig> {
        val json = storage.getString(storageKey, null) ?: return emptyList()
        val type = object : TypeToken<List<DeviceConfig>>() {}.type
        return try {
            gson.fromJson<List<DeviceConfig>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addDevice(device: DeviceConfig) {
        val current = getDevices().toMutableList()
        current.removeAll { it.id == device.id }
        current.add(device)
        saveDevices(current)
    }

    fun deleteDevice(id: String) {
        val current = getDevices().toMutableList()
        current.removeAll { it.id == id }
        saveDevices(current)
    }

    fun updateDevice(device: DeviceConfig) {
        addDevice(device)
    }

    fun updateEndpoints(
        deviceId: String,
        powerOnUrl: String,
        powerOffUrl: String,
        rebootUrl: String,
        statusUrl: String = ""
    ): Boolean {
        val current = getDevices().toMutableList()
        val index = current.indexOfFirst { it.id == deviceId }
        if (index != -1) {
            val old = current[index]
            val effectiveStatusUrl = if (statusUrl.isNotBlank()) statusUrl else old.statusUrl
            if (old.powerOnUrl != powerOnUrl || old.powerOffUrl != powerOffUrl || old.rebootUrl != rebootUrl || old.statusUrl != effectiveStatusUrl) {
                current[index] = old.copy(
                    powerOnUrl = powerOnUrl,
                    powerOffUrl = powerOffUrl,
                    rebootUrl = rebootUrl,
                    statusUrl = effectiveStatusUrl
                )
                saveDevices(current)
                return true
            }
        }
        return false
    }

    private fun saveDevices(devices: List<DeviceConfig>) {
        val json = gson.toJson(devices)
        storage.putString(storageKey, json)
    }
}
