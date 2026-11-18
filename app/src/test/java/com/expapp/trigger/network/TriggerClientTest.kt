package com.expapp.trigger.network

import com.expapp.trigger.data.model.DeviceConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TriggerClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: TriggerClient

    private val sampleDevice = DeviceConfig(
        id = "test-1",
        name = "My Debian",
        node = "debian",
        secretKey = "Carbon@12",
        r2Url = "http://localhost/config.json",
        powerOnUrl = "https://cosmos.linuxguy.space/poweron",
        powerOffUrl = "https://cosmos.linuxguy.space/poweroff",
        rebootUrl = "https://cosmos.linuxguy.space/reboot"
    )

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = TriggerClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun buildRequest_powerOff_hasCorrectMethodHeaderAndBody() {
        val request = client.buildRequest(sampleDevice, TriggerAction.POWER_OFF)

        assertEquals("POST", request.method)
        assertEquals("https://cosmos.linuxguy.space/poweroff", request.url.toString())
        assertEquals("Carbon@12", request.header("X-Secret-Key"))

        val buffer = Buffer()
        request.body?.writeTo(buffer)
        val bodyString = buffer.readUtf8()
        assertEquals("""{"node":"debian"}""", bodyString)
    }

    @Test
    fun buildRequest_reboot_usesRebootUrl() {
        val request = client.buildRequest(sampleDevice, TriggerAction.REBOOT)

        assertEquals("POST", request.method)
        assertEquals("https://cosmos.linuxguy.space/reboot", request.url.toString())
        assertEquals("Carbon@12", request.header("X-Secret-Key"))
    }

    @Test
    fun buildRequest_powerOn_usesGetMethodWithoutSecretKey() {
        val request = client.buildRequest(sampleDevice, TriggerAction.POWER_ON)

        assertEquals("GET", request.method)
        assertEquals("https://cosmos.linuxguy.space/poweron", request.url.toString())
        org.junit.Assert.assertNull(request.header("X-Secret-Key"))
        org.junit.Assert.assertNull(request.body)
    }

    @Test
    fun sendTrigger_withMockServer_executesAndReturnsResponse() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"shutting down"}""")
        )

        val localDevice = sampleDevice.copy(
            powerOffUrl = server.url("/poweroff").toString()
        )

        val latch = CountDownLatch(1)
        var responseResult: TriggerResponse? = null

        client.sendTrigger(localDevice, TriggerAction.POWER_OFF) { response ->
            responseResult = response
            latch.countDown()
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        val response = responseResult!!
        assertTrue(response.isSuccess)
        assertEquals(200, response.statusCode)
        assertTrue(response.message.contains("shutting down"))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("Carbon@12", recorded.getHeader("X-Secret-Key"))
        assertEquals("""{"node":"debian"}""", recorded.body.readUtf8())
    }

    @Test
    fun fetchR2Config_withMockServer_returnsParsedEndpoints() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "poweron": "https://cosmos.linuxguy.space/poweron",
                      "poweroff": "https://cosmos.linuxguy.space/poweroff",
                      "reboot": "https://cosmos.linuxguy.space/reboot"
                    }
                    """.trimIndent()
                )
        )

        val latch = CountDownLatch(1)
        var fetchResult: Result<*>? = null

        client.fetchR2Config(server.url("/config.json").toString()) { result ->
            fetchResult = result
            latch.countDown()
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        val result = fetchResult!!
        assertTrue(result.isSuccess)
    }

    @Test
    fun checkStatus_withJsonResponse_parsesOnlineAndIp() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"node":"debian","online":true,"status":"online","ip":"100.85.63.44"}""")
        )

        val localDevice = sampleDevice.copy(
            statusUrl = server.url("/status").toString()
        )

        val latch = CountDownLatch(1)
        var statusResult: NodeStatus? = null

        client.checkStatus(localDevice) { status ->
            statusResult = status
            latch.countDown()
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        val status = statusResult!!
        assertTrue(status.isOnline)
        assertEquals("ONLINE", status.statusText)
        assertEquals("100.85.63.44", status.ip)
    }

    @Test
    fun checkStatus_withPlainText_parsesOnline() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("ONLINE")
        )

        val localDevice = sampleDevice.copy(
            statusUrl = server.url("/ping").toString()
        )

        val latch = CountDownLatch(1)
        var statusResult: NodeStatus? = null

        client.checkStatus(localDevice) { status ->
            statusResult = status
            latch.countDown()
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        val status = statusResult!!
        assertTrue(status.isOnline)
        assertEquals("ONLINE", status.statusText)
    }
}
