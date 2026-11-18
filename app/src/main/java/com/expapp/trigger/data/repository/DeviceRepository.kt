package com.expapp.trigger.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.expapp.trigger.data.model.DeviceConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

interface KeyValueStorage {
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String?)
}

class SharedPrefsStorage(context: Context) : KeyValueStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("device_trigger_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String, defaultValue: String?): String? =
        prefs.getString(key, defaultValue)

    override fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
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

    private fun saveDevices(devices: List<DeviceConfig>) {
        val json = gson.toJson(devices)
        storage.putString(storageKey, json)
    }
}
