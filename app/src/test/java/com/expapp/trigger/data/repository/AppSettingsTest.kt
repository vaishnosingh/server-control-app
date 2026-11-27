package com.expapp.trigger.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSettingsTest {

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
    private lateinit var settings: AppSettings

    @Before
    fun setup() {
        storage = FakeKeyValueStorage()
        settings = AppSettings(storage)
    }

    @Test
    fun defaultValues_areTrue() {
        assertTrue(settings.isBiometricEnabled)
        assertTrue(settings.isBackgroundMonitoringEnabled)
        assertFalse(settings.isWidgetBiometricEnabled)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.isDynamicColorEnabled)
    }

    @Test
    fun themePreferences_persistCorrectly() {
        settings.themeMode = ThemeMode.LIGHT
        assertEquals(ThemeMode.LIGHT, settings.themeMode)

        settings.themeMode = ThemeMode.DARK
        assertEquals(ThemeMode.DARK, settings.themeMode)

        settings.isDynamicColorEnabled = false
        assertFalse(settings.isDynamicColorEnabled)
    }

    @Test
    fun toggles_updateCorrectly() {
        settings.isBiometricEnabled = false
        assertFalse(settings.isBiometricEnabled)

        settings.isBackgroundMonitoringEnabled = false
        assertFalse(settings.isBackgroundMonitoringEnabled)

        settings.isWidgetBiometricEnabled = true
        assertTrue(settings.isWidgetBiometricEnabled)
    }

    @Test
    fun deviceStatus_tracksTransitions() {
        assertNull(settings.getLastStatus("dev-1"))

        settings.setLastStatus("dev-1", true)
        assertEquals(true, settings.getLastStatus("dev-1"))

        settings.setLastStatus("dev-1", false)
        assertEquals(false, settings.getLastStatus("dev-1"))
    }
}
