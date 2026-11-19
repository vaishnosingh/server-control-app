package com.expapp.trigger.network

import com.expapp.trigger.data.model.SysPulseStatus
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SysPulseClientTest {

    @Test
    fun testDeserializeSysPulseStatus_realPayload() {
        val json = """{"cpu":{"cores":4,"load":[0.17,1.25,0.88],"pct":5.01},"disk":[{"mount":"/","pct":31.22,"total_gb":115.83,"used_gb":36.16},{"mount":"/mnt/storage","pct":1.95,"total_gb":1862.11,"used_gb":36.27}],"ram":{"pct":40.78,"total_mb":7855,"used_mb":3203},"services":[{"name":"openclaw","unit":"openclaw-gateway.service","active":true,"enabled":true,"state":"running","pid":927},{"name":"forge","unit":"forge.service","active":true,"enabled":true,"state":"exited","pid":1751},{"name":"oort","unit":"oort.service","active":true,"enabled":true,"state":"running","pid":1752},{"name":"minio","unit":"minio.service","active":true,"enabled":true,"state":"running","pid":1711},{"name":"xrdp","unit":"xrdp.service","active":false,"enabled":false,"state":"stopped"},{"name":"xrdp sesman","unit":"xrdp-sesman.service","active":false,"enabled":false,"state":"stopped"},{"name":"docker","unit":"docker.service","active":true,"enabled":true,"state":"running","pid":1020},{"name":"postgres","unit":"postgresql@17-main.service","active":true,"enabled":true,"state":"running","pid":1883}],"success":true,"uptime":"16m 15s"}"""

        val gson = GsonBuilder()
            .registerTypeAdapter(SysPulseStatus::class.java, SysPulseStatusDeserializer())
            .create()

        val status = gson.fromJson(json, SysPulseStatus::class.java)
        assertNotNull(status)
        assertEquals("16m 15s", status.uptime)
        assertEquals(5.01, status.cpu.pct, 0.01)
        assertEquals(2, status.disk.size)
        assertEquals(8, status.services.size)
        val activeCount = status.services.count { it.active }
        assertEquals(6, activeCount)
    }

    @Test
    fun testDeserializeSysPulseStatus_emptyOrErrorPayload() {
        val errorJson = """{"error":"Unauthorized","success":false}"""

        val gson = GsonBuilder()
            .registerTypeAdapter(SysPulseStatus::class.java, SysPulseStatusDeserializer())
            .create()

        val status = gson.fromJson(errorJson, SysPulseStatus::class.java)
        assertNotNull(status)
        assertEquals("", status.uptime)
        assertEquals(0.0, status.cpu.pct, 0.01)
        assertTrue(status.disk.isEmpty())
        assertTrue(status.services.isEmpty())
    }
}
