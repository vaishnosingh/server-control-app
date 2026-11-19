package com.expapp.trigger.data.parser

import com.google.gson.JsonParser

data class ParsedEndpoints(
    val powerOn: String,
    val powerOff: String,
    val reboot: String,
    val status: String = "",
    val syspulse: String = "",
    val syspulseSecret: String = ""
)

object R2ConfigParser {
    fun parse(jsonString: String): Result<ParsedEndpoints> {
        return runCatching {
            val jsonElement = JsonParser.parseString(jsonString)
            if (!jsonElement.isJsonObject) {
                throw IllegalArgumentException("Expected JSON object")
            }
            val jsonObject = jsonElement.asJsonObject

            // Case-insensitive & variant normalization
            val normalizedMap = mutableMapOf<String, String>()
            for ((key, value) in jsonObject.entrySet()) {
                val cleanKey = key.lowercase().replace("_", "").replace("-", "")
                if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                    normalizedMap[cleanKey] = value.asString
                }
            }

            val powerOn = normalizedMap["poweron"]
                ?: normalizedMap["on"]
                ?: normalizedMap["start"]
                ?: throw IllegalArgumentException("Missing required endpoint key: 'poweron'")

            val powerOff = normalizedMap["poweroff"]
                ?: normalizedMap["off"]
                ?: normalizedMap["shutdown"]
                ?: throw IllegalArgumentException("Missing required endpoint key: 'poweroff'")

            val reboot = normalizedMap["reboot"]
                ?: normalizedMap["restart"]
                ?: normalizedMap["reset"]
                ?: throw IllegalArgumentException("Missing required endpoint key: 'reboot'")

            val status = normalizedMap["status"]
                ?: normalizedMap["ping"]
                ?: normalizedMap["health"]
                ?: ""

            val syspulse = normalizedMap["syspulse"]
                ?: normalizedMap["pulse"]
                ?: normalizedMap["metrics"]
                ?: normalizedMap["dashboard"]
                ?: normalizedMap["syspulseurl"]
                ?: ""

            val syspulseSecret = normalizedMap["syspulsesecret"]
                ?: normalizedMap["pulsesecret"]
                ?: normalizedMap["pulsekey"]
                ?: ""

            ParsedEndpoints(
                powerOn = powerOn,
                powerOff = powerOff,
                reboot = reboot,
                status = status,
                syspulse = syspulse,
                syspulseSecret = syspulseSecret
            )
        }
    }
}
