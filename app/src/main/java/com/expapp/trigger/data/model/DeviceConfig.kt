package com.expapp.trigger.data.model

import java.util.UUID

data class DeviceConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val node: String,
    val secretKey: String,
    val r2Url: String = "",
    val powerOnUrl: String,
    val powerOffUrl: String,
    val rebootUrl: String,
    val statusUrl: String = "",
    val syspulseUrl: String = "",
    val syspulseSecretKey: String = "",
    val accentColorHex: String = FallbackConfig.DEFAULT_ACCENT_HEX
)

/**
 * Hardcoded fallback defaults used ONLY when Cloudflare R2 endpoint discovery
 * has not yet succeeded or is temporarily unreachable.
 */
object FallbackConfig {
    const val DEFAULT_DEVICE_ID = "2dc98ae9-9f71-4f29-95a6-a59ac94d7bf6"
    const val DEFAULT_R2_URL = "https://bucket.linuxguy.space/home-server-app/endpoints.json"
    const val DEFAULT_NAME = "Server"
    const val DEFAULT_NODE = "debian"
    const val DEFAULT_SECRET_KEY = "Carbon@12"
    const val DEFAULT_POWER_ON_URL = "https://trigger.macrodroid.com/87f2f62c-09ee-43cb-8796-fd1a6465754b/wakeserver"
    const val DEFAULT_POWER_OFF_URL = "https://cosmos.linuxguy.space/poweroff?node=debian"
    const val DEFAULT_REBOOT_URL = "https://cosmos.linuxguy.space/reboot?node=debian"
    const val DEFAULT_STATUS_URL = "https://cosmos.linuxguy.space/status?node=debian"
    const val DEFAULT_SYSPULSE_URL = "https://pulse.linuxguy.space"
    const val DEFAULT_SYSPULSE_SECRET = "Carbon@12"
    const val DEFAULT_ACCENT_HEX = "#22D47E"

    // Legacy hardcoded defaults from earlier versions that should be migrated when R2 is available
    val LEGACY_SYSPULSE_URLS = setOf("http://100.85.63.44:8088")
    val LEGACY_SYSPULSE_SECRETS = setOf("default_pulse_secret_key_change_me")

    fun createDefaultDevice(): DeviceConfig = DeviceConfig(
        id = DEFAULT_DEVICE_ID,
        name = DEFAULT_NAME,
        node = DEFAULT_NODE,
        secretKey = DEFAULT_SECRET_KEY,
        r2Url = DEFAULT_R2_URL,
        powerOnUrl = DEFAULT_POWER_ON_URL,
        powerOffUrl = DEFAULT_POWER_OFF_URL,
        rebootUrl = DEFAULT_REBOOT_URL,
        statusUrl = DEFAULT_STATUS_URL,
        syspulseUrl = DEFAULT_SYSPULSE_URL,
        syspulseSecretKey = DEFAULT_SYSPULSE_SECRET,
        accentColorHex = DEFAULT_ACCENT_HEX
    )
}
