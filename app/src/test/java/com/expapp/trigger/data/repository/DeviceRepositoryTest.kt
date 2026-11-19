package com.expapp.trigger.data.repository

import com.expapp.trigger.data.model.DeviceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceRepositoryTest {

    private class FakeKeyValueStorage : KeyValueStorage {
        private val data = mutableMapOf<String, String?>()
        override fun getString(key: String, defaultValue: String?): String? = data[key] ?: defaultValue
        override fun putString(key: String, value: String?) {
            data[key] = value
        }
    }

    private lateinit var storage: FakeKeyValueStorage
    private lateinit var repository: DeviceRepository

    @Before
    fun setup() {
        storage = FakeKeyValueStorage()
        repository = DeviceRepository(storage)
    }

    @Test
    fun getDevices_whenEmpty_returnsEmptyList() {
        val devices = repository.getDevices()
        assertTrue(devices.isEmpty())
    }

    @Test
    fun addDevice_savesAndReturnsDevice() {
        val device = DeviceConfig(
            id = "dev-1",
            name = "Debian Server",
            node = "debian",
            secretKey = "Carbon@12",
            r2Url = "https://r2.example.com/config.json",
            powerOnUrl = "https://cosmos.linuxguy.space/poweron",
            powerOffUrl = "https://cosmos.linuxguy.space/poweroff",
            rebootUrl = "https://cosmos.linuxguy.space/reboot"
        )

        repository.addDevice(device)
        val devices = repository.getDevices()
        assertEquals(1, devices.size)
        assertEquals(device, devices[0])
    }

    @Test
    fun deleteDevice_removesDeviceById() {
        val d1 = DeviceConfig(id = "1", name = "D1", node = "n1", secretKey = "k1", r2Url = "", powerOnUrl = "", powerOffUrl = "", rebootUrl = "")
        val d2 = DeviceConfig(id = "2", name = "D2", node = "n2", secretKey = "k2", r2Url = "", powerOnUrl = "", powerOffUrl = "", rebootUrl = "")

        repository.addDevice(d1)
        repository.addDevice(d2)
        assertEquals(2, repository.getDevices().size)

        repository.deleteDevice("1")
        val remaining = repository.getDevices()
        assertEquals(1, remaining.size)
        assertEquals("2", remaining[0].id)
    }

    @Test
    fun updateEndpoints_whenEndpointsChange_updatesDeviceAndReturnsTrue() {
        val device = DeviceConfig(
            id = "dev-1",
            name = "Debian Server",
            node = "debian",
            secretKey = "Carbon@12",
            r2Url = "https://r2.example.com/config.json",
            powerOnUrl = "https://old.url/on",
            powerOffUrl = "https://old.url/off",
            rebootUrl = "https://old.url/reboot"
        )
        repository.addDevice(device)

        val updated = repository.updateEndpoints(
            deviceId = "dev-1",
            powerOnUrl = "https://new.url/on",
            powerOffUrl = "https://new.url/off",
            rebootUrl = "https://new.url/reboot",
            syspulseUrl = "http://100.85.63.44:8088",
            syspulseSecretKey = "secret123"
        )

        assertTrue(updated)
        val saved = repository.getDevices().first { it.id == "dev-1" }
        assertEquals("https://new.url/on", saved.powerOnUrl)
        assertEquals("https://new.url/off", saved.powerOffUrl)
        assertEquals("https://new.url/reboot", saved.rebootUrl)
        assertEquals("http://100.85.63.44:8088", saved.syspulseUrl)
        assertEquals("secret123", saved.syspulseSecretKey)
    }

    @Test
    fun updateEndpoints_whenEndpointsIdentical_returnsFalse() {
        val device = DeviceConfig(
            id = "dev-1",
            name = "Debian Server",
            node = "debian",
            secretKey = "Carbon@12",
            r2Url = "https://r2.example.com/config.json",
            powerOnUrl = "https://same.url/on",
            powerOffUrl = "https://same.url/off",
            rebootUrl = "https://same.url/reboot"
        )
        repository.addDevice(device)

        val updated = repository.updateEndpoints(
            deviceId = "dev-1",
            powerOnUrl = "https://same.url/on",
            powerOffUrl = "https://same.url/off",
            rebootUrl = "https://same.url/reboot"
        )

        org.junit.Assert.assertFalse(updated)
    }
}
