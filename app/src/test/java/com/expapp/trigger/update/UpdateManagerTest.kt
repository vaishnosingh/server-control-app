package com.expapp.trigger.update

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UpdateManagerTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun isNewerVersion_correctlyComparesVersions() {
        assertTrue(UpdateManager.isNewerVersion("1.1", "1.0"))
        assertTrue(UpdateManager.isNewerVersion("1.0.1", "1.0.0"))
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(UpdateManager.isNewerVersion("1.2.0", "1.1"))

        assertFalse(UpdateManager.isNewerVersion("1.0", "1.0"))
        assertFalse(UpdateManager.isNewerVersion("1.0.0", "1.0"))
        assertFalse(UpdateManager.isNewerVersion("1.0", "1.1"))
        assertFalse(UpdateManager.isNewerVersion("0.9", "1.0"))
    }
}
