package com.expapp.trigger.data.repository

import android.content.Context
import com.expapp.trigger.data.model.TriggerLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TriggerHistoryRepository(private val storage: KeyValueStorage) {

    constructor(context: Context) : this(SharedPrefsStorage(context, "trigger_history_prefs"))

    private val gson = Gson()
    private val storageKey = "history_logs_json"
    private val maxLogs = 500

    @Synchronized
    fun getLogs(): List<TriggerLog> {
        val json = storage.getString(storageKey, null) ?: return emptyList()
        val type = object : TypeToken<List<TriggerLog>>() {}.type
        return try {
            val list = gson.fromJson<List<TriggerLog>>(json, type) ?: emptyList()
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addLog(log: TriggerLog) {
        val current = getLogs().toMutableList()
        current.add(0, log)
        if (current.size > maxLogs) {
            current.subList(maxLogs, current.size).clear()
        }
        saveLogs(current)
    }

    @Synchronized
    fun clearOlderThan(durationMillis: Long) {
        val cutoff = System.currentTimeMillis() - durationMillis
        val filtered = getLogs().filter { it.timestamp >= cutoff }
        saveLogs(filtered)
    }

    @Synchronized
    fun clearAll() {
        storage.remove(storageKey)
    }

    private fun saveLogs(logs: List<TriggerLog>) {
        val json = gson.toJson(logs)
        storage.putString(storageKey, json)
    }
}
