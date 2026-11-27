package com.expapp.trigger.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.FallbackConfig
import com.expapp.trigger.data.parser.ParsedEndpoints
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.UUID

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

private fun JsonObject.getStringSafe(key: String, default: String = ""): String {
    val elem = get(key) ?: return default
    if (elem.isJsonNull || !elem.isJsonPrimitive) return default
    return try { elem.asString ?: default } catch (_: Exception) { default }
}

class DeviceConfigDeserializer : JsonDeserializer<DeviceConfig> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): DeviceConfig {
        val obj = if (json.isJsonObject) json.asJsonObject else JsonObject()
        return DeviceConfig(
            id = obj.getStringSafe("id").ifBlank { UUID.randomUUID().toString() },
            name = obj.getStringSafe("name", "Server"),
            node = obj.getStringSafe("node", "debian"),
            secretKey = obj.getStringSafe("secretKey"),
            r2Url = obj.getStringSafe("r2Url"),
            powerOnUrl = obj.getStringSafe("powerOnUrl"),
            powerOffUrl = obj.getStringSafe("powerOffUrl"),
            rebootUrl = obj.getStringSafe("rebootUrl"),
            statusUrl = obj.getStringSafe("statusUrl"),
            syspulseUrl = obj.getStringSafe("syspulseUrl"),
            syspulseSecretKey = obj.getStringSafe("syspulseSecretKey"),
            accentColorHex = obj.getStringSafe("accentColorHex").ifBlank { FallbackConfig.DEFAULT_ACCENT_HEX }
        )
    }
}

class DeviceRepository(private val storage: KeyValueStorage) {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(DeviceConfig::class.java, DeviceConfigDeserializer())
        .create()
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
        val existingIdx = current.indexOfFirst { it.id == device.id }
        if (existingIdx != -1) {
            current[existingIdx] = device
        } else {
            current.add(device)
        }
        saveDevices(current)
    }

    fun deleteDevice(id: String) {
        val current = getDevices().toMutableList()
        current.removeAll { it.id == id }
        saveDevices(current)
    }

    fun updateDevice(device: DeviceConfig) {
        val current = getDevices().toMutableList()
        val existingIdx = current.indexOfFirst { it.id == device.id }
        if (existingIdx != -1) {
            current[existingIdx] = device
            saveDevices(current)
        } else {
            addDevice(device)
        }
    }

    fun applyParsedEndpoints(deviceId: String, ep: ParsedEndpoints): Boolean {
        return updateEndpoints(
            deviceId = deviceId,
            powerOnUrl = ep.powerOn,
            powerOffUrl = ep.powerOff,
            rebootUrl = ep.reboot,
            statusUrl = ep.status,
            syspulseUrl = ep.syspulse,
            syspulseSecretKey = ep.syspulseSecret,
            secretKey = ep.secretKey,
            name = ep.name,
            node = ep.node,
            accentColorHex = ep.accentColor
        )
    }

    fun updateDeviceAccent(deviceId: String, accentColorHex: String): Boolean {
        val current = getDevices().toMutableList()
        val index = current.indexOfFirst { it.id == deviceId }
        if (index != -1 && current[index].accentColorHex != accentColorHex) {
            current[index] = current[index].copy(accentColorHex = accentColorHex)
            saveDevices(current)
            return true
        }
        return false
    }

    fun updateEndpoints(
        deviceId: String,
        powerOnUrl: String,
        powerOffUrl: String,
        rebootUrl: String,
        statusUrl: String = "",
        syspulseUrl: String = "",
        syspulseSecretKey: String = "",
        secretKey: String = "",
        name: String = "",
        node: String = "",
        accentColorHex: String = ""
    ): Boolean {
        val current = getDevices().toMutableList()
        val index = current.indexOfFirst { it.id == deviceId }
        if (index != -1) {
            val old = current[index]
            val effectiveStatusUrl = if (statusUrl.isNotBlank()) statusUrl else old.statusUrl
            val effectiveSyspulseUrl = if (syspulseUrl.isNotBlank()) syspulseUrl else old.syspulseUrl
            val effectiveSyspulseKey = if (syspulseSecretKey.isNotBlank()) syspulseSecretKey else old.syspulseSecretKey
            val effectiveSecretKey = if (secretKey.isNotBlank()) secretKey else old.secretKey
            val effectiveName = if (name.isNotBlank()) name else old.name
            val effectiveNode = if (node.isNotBlank()) node else old.node
            val effectiveAccent = if (accentColorHex.isNotBlank()) accentColorHex else old.accentColorHex
            if (old.powerOnUrl != powerOnUrl || old.powerOffUrl != powerOffUrl ||
                old.rebootUrl != rebootUrl || old.statusUrl != effectiveStatusUrl ||
                old.syspulseUrl != effectiveSyspulseUrl || old.syspulseSecretKey != effectiveSyspulseKey ||
                old.secretKey != effectiveSecretKey || old.name != effectiveName ||
                old.node != effectiveNode || old.accentColorHex != effectiveAccent
            ) {
                current[index] = old.copy(
                    powerOnUrl = powerOnUrl,
                    powerOffUrl = powerOffUrl,
                    rebootUrl = rebootUrl,
                    statusUrl = effectiveStatusUrl,
                    syspulseUrl = effectiveSyspulseUrl,
                    syspulseSecretKey = effectiveSyspulseKey,
                    secretKey = effectiveSecretKey,
                    name = effectiveName,
                    node = effectiveNode,
                    accentColorHex = effectiveAccent
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
