package com.expapp.trigger.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class R2ConfigParserTest {

    @Test
    fun parse_standardLowercaseKeys_returnsEndpoints() {
        val json = """
            {
                "poweron": "https://cosmos.linuxguy.space/poweron",
                "poweroff": "https://cosmos.linuxguy.space/poweroff",
                "reboot": "https://cosmos.linuxguy.space/reboot",
                "status": "https://cosmos.linuxguy.space/status"
            }
        """.trimIndent()

        val result = R2ConfigParser.parse(json)
        assertTrue(result.isSuccess)
        val endpoints = result.getOrThrow()
        assertEquals("https://cosmos.linuxguy.space/poweron", endpoints.powerOn)
        assertEquals("https://cosmos.linuxguy.space/poweroff", endpoints.powerOff)
        assertEquals("https://cosmos.linuxguy.space/reboot", endpoints.reboot)
        assertEquals("https://cosmos.linuxguy.space/status", endpoints.status)
    }

    @Test
    fun parse_camelCaseAndUnderscoreKeys_returnsEndpoints() {
        val json = """
            {
                "power_on": "https://cosmos.linuxguy.space/poweron",
                "powerOff": "https://cosmos.linuxguy.space/poweroff",
                "restart": "https://cosmos.linuxguy.space/reboot"
            }
        """.trimIndent()

        val result = R2ConfigParser.parse(json)
        assertTrue(result.isSuccess)
        val endpoints = result.getOrThrow()
        assertEquals("https://cosmos.linuxguy.space/poweron", endpoints.powerOn)
        assertEquals("https://cosmos.linuxguy.space/poweroff", endpoints.powerOff)
        assertEquals("https://cosmos.linuxguy.space/reboot", endpoints.reboot)
    }

    @Test
    fun parse_missingRequiredKey_returnsFailure() {
        val json = """
            {
                "poweron": "https://cosmos.linuxguy.space/poweron",
                "reboot": "https://cosmos.linuxguy.space/reboot"
            }
        """.trimIndent()

        val result = R2ConfigParser.parse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("poweroff", ignoreCase = true) == true)
    }

    @Test
    fun parse_invalidJson_returnsFailure() {
        val invalid = "{ invalid json"
        val result = R2ConfigParser.parse(invalid)
        assertTrue(result.isFailure)
    }
}
