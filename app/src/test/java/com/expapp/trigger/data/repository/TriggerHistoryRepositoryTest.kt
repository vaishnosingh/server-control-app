package com.expapp.trigger.data.repository

import com.expapp.trigger.data.model.TriggerLog
import com.expapp.trigger.network.TriggerAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class TriggerHistoryRepositoryTest {

    private class FakeKeyValueStorage : KeyValueStorage {
        private val data = mutableMapOf<String, String?>()
        private val boolData = mutableMapOf<String, Boolean>()

        override fun getString(key: String, defaultValue: String?): String? = data[key] ?: defaultValue
        override fun putString(key: String, value: String?) { data[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = boolData[key] ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { boolData[key] = value }
        override fun contains(key: String): Boolean = data.containsKey(key) || boolData.containsKey(key)
        override fun remove(key: String) {
            data.remove(key)
            boolData.remove(key)
        }
    }

    private lateinit var storage: FakeKeyValueStorage
    private lateinit var repository: TriggerHistoryRepository

    @Before
    fun setup() {
        storage = FakeKeyValueStorage()
        repository = TriggerHistoryRepository(storage)
    }

    @Test
    fun getLogs_whenEmpty_returnsEmptyList() {
        assertTrue(repository.getLogs().isEmpty())
    }

    @Test
    fun addLog_savesAndReturnsSortedByTimestampDescending() {
        val now = System.currentTimeMillis()
        val log1 = TriggerLog(
            timestamp = now - 5000,
            deviceId = "dev-1",
            deviceName = "Server",
            node = "debian",
            action = TriggerAction.POWER_ON,
            isSuccess = true,
            statusCode = 200,
            message = "Power on ok"
        )
        val log2 = TriggerLog(
            timestamp = now,
            deviceId = "dev-1",
            deviceName = "Server",
            node = "debian",
            action = TriggerAction.REBOOT,
            isSuccess = true,
            statusCode = 200,
            message = "Reboot ok"
        )

        repository.addLog(log1)
        repository.addLog(log2)

        val logs = repository.getLogs()
        assertEquals(2, logs.size)
        assertEquals(TriggerAction.REBOOT, logs[0].action)
        assertEquals(TriggerAction.POWER_ON, logs[1].action)
    }

    @Test
    fun clearOlderThan_removesExpiredLogsOnly() {
        val now = System.currentTimeMillis()
        val oldLog = TriggerLog(
            timestamp = now - TimeUnit.HOURS.toMillis(48), // 2 days ago
            deviceId = "dev-1",
            deviceName = "Server",
            node = "debian",
            action = TriggerAction.POWER_ON,
            isSuccess = true,
            statusCode = 200,
            message = "Old"
        )
        val freshLog = TriggerLog(
            timestamp = now - TimeUnit.HOURS.toMillis(2), // 2 hours ago
            deviceId = "dev-1",
            deviceName = "Server",
            node = "debian",
            action = TriggerAction.REBOOT,
            isSuccess = true,
            statusCode = 200,
            message = "Fresh"
        )

        repository.addLog(oldLog)
        repository.addLog(freshLog)
        assertEquals(2, repository.getLogs().size)

        // Clear older than 24 hours
        repository.clearOlderThan(TimeUnit.HOURS.toMillis(24))

        val remaining = repository.getLogs()
        assertEquals(1, remaining.size)
        assertEquals("Fresh", remaining[0].message)
    }

    @Test
    fun clearAll_clearsEverything() {
        val log = TriggerLog(
            deviceId = "dev-1",
            deviceName = "Server",
            node = "debian",
            action = TriggerAction.POWER_ON,
            isSuccess = true,
            statusCode = 200,
            message = "Test"
        )
        repository.addLog(log)
        assertEquals(1, repository.getLogs().size)

        repository.clearAll()
        assertTrue(repository.getLogs().isEmpty())
    }
}
