package com.expapp.trigger.data.parser

import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class ParsedEndpoints(
    val powerOn: String,
    val powerOff: String,
    val reboot: String,
    val status: String = "",
    val syspulse: String = "",
    val syspulseSecret: String = "",
    val name: String = "",
    val node: String = "",
    val secretKey: String = "",
    val accentColor: String = ""
)

object R2ConfigParser {
    fun parse(jsonString: String): Result<ParsedEndpoints> {
        return runCatching {
            val jsonElement = JsonParser.parseString(jsonString)
            if (jsonElement.isJsonArray) {
                val arr = jsonElement.asJsonArray
                if (arr.size() == 0 || !arr[0].isJsonObject) {
                    throw IllegalArgumentException("Expected non-empty JSON object or array")
                }
                return@runCatching parseObject(arr[0].asJsonObject)
            }
            if (!jsonElement.isJsonObject) {
                throw IllegalArgumentException("Expected JSON object")
            }
            val root = jsonElement.asJsonObject
            if (root.has("servers") && root.get("servers").isJsonArray) {
                val arr = root.getAsJsonArray("servers")
                if (arr.size() > 0 && arr[0].isJsonObject) {
                    return@runCatching parseObject(arr[0].asJsonObject)
                }
            }
            parseObject(root)
        }
    }

    fun parseList(jsonString: String): Result<List<ParsedEndpoints>> {
        return runCatching {
            val jsonElement = JsonParser.parseString(jsonString)
            when {
                jsonElement.isJsonArray -> {
                    jsonElement.asJsonArray
                        .filter { it.isJsonObject }
                        .map { parseObject(it.asJsonObject) }
                        .ifEmpty { throw IllegalArgumentException("No valid server entries in JSON array") }
                }
                jsonElement.isJsonObject -> {
                    val root = jsonElement.asJsonObject
                    val arrElem = root.get("servers") ?: root.get("devices")
                    if (arrElem != null && arrElem.isJsonArray) {
                        arrElem.asJsonArray
                            .filter { it.isJsonObject }
                            .map { parseObject(it.asJsonObject) }
                            .ifEmpty { throw IllegalArgumentException("No valid server entries in JSON list") }
                    } else {
                        listOf(parseObject(root))
                    }
                }
                else -> throw IllegalArgumentException("Expected JSON object or array")
            }
        }
    }

    private fun parseObject(jsonObject: JsonObject): ParsedEndpoints {
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

        val name = normalizedMap["name"]
            ?: normalizedMap["devicename"]
            ?: normalizedMap["servername"]
            ?: normalizedMap["title"]
            ?: normalizedMap["label"]
            ?: ""

        val node = normalizedMap["node"]
            ?: normalizedMap["nodename"]
            ?: normalizedMap["host"]
            ?: normalizedMap["hostname"]
            ?: ""

        val secretKey = normalizedMap["secretkey"]
            ?: normalizedMap["secret"]
            ?: normalizedMap["apikey"]
            ?: ""

        val accentColor = normalizedMap["accentcolor"]
            ?: normalizedMap["accent"]
            ?: normalizedMap["color"]
            ?: normalizedMap["theme"]
            ?: ""

        return ParsedEndpoints(
            powerOn = powerOn,
            powerOff = powerOff,
            reboot = reboot,
            status = status,
            syspulse = syspulse,
            syspulseSecret = syspulseSecret,
            name = name,
            node = node,
            secretKey = secretKey,
            accentColor = accentColor
        )
    }
}
