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
    fun parse_withSysPulseEndpoints_returnsSysPulseData() {
        val json = """
            {
                "poweron": "https://cosmos.linuxguy.space/poweron",
                "poweroff": "https://cosmos.linuxguy.space/poweroff",
                "reboot": "https://cosmos.linuxguy.space/reboot",
                "status": "https://cosmos.linuxguy.space/status",
                "syspulse": "http://100.85.63.44:8088",
                "syspulse_secret": "my_secret_key"
            }
        """.trimIndent()

        val result = R2ConfigParser.parse(json)
        assertTrue(result.isSuccess)
        val endpoints = result.getOrThrow()
        assertEquals("http://100.85.63.44:8088", endpoints.syspulse)
        assertEquals("my_secret_key", endpoints.syspulseSecret)
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

    @Test
    fun parse_withFullServerMetadataAndAccent_returnsMetadata() {
        val json = """
            {
                "name": "Production Node",
                "node": "prod-01",
                "secret": "ProdSecret",
                "accent_color": "#06B6D4",
                "poweron": "https://cosmos.linuxguy.space/poweron",
                "poweroff": "https://cosmos.linuxguy.space/poweroff",
                "reboot": "https://cosmos.linuxguy.space/reboot",
                "status": "https://cosmos.linuxguy.space/status",
                "syspulse": "https://pulse.linuxguy.space",
                "syspulse_secret": "Carbon@12"
            }
        """.trimIndent()

        val result = R2ConfigParser.parse(json)
        assertTrue(result.isSuccess)
        val ep = result.getOrThrow()
        assertEquals("Production Node", ep.name)
        assertEquals("prod-01", ep.node)
        assertEquals("ProdSecret", ep.secretKey)
        assertEquals("#06B6D4", ep.accentColor)
        assertEquals("https://pulse.linuxguy.space", ep.syspulse)
        assertEquals("Carbon@12", ep.syspulseSecret)
    }

    @Test
    fun parseList_withMultipleServers_returnsAllEntries() {
        val json = """
            {
                "servers": [
                    {
                        "name": "Alpha",
                        "accent_color": "#22D47E",
                        "poweron": "https://a.example/on",
                        "poweroff": "https://a.example/off",
                        "reboot": "https://a.example/reboot"
                    },
                    {
                        "name": "Beta",
                        "accent_color": "#3B82F6",
                        "poweron": "https://b.example/on",
                        "poweroff": "https://b.example/off",
                        "reboot": "https://b.example/reboot"
                    }
                ]
            }
        """.trimIndent()

        val result = R2ConfigParser.parseList(json)
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)
        assertEquals("Alpha", list[0].name)
        assertEquals("#3B82F6", list[1].accentColor)
    }
}
