package com.expapp.trigger.network

import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.parser.ParsedEndpoints
import com.expapp.trigger.data.parser.R2ConfigParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class TriggerClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun buildRequest(device: DeviceConfig, action: TriggerAction): Request {
        return when (action) {
            TriggerAction.POWER_ON -> {
                Request.Builder()
                    .url(device.powerOnUrl)
                    .get()
                    .build()
            }
            TriggerAction.POWER_OFF -> {
                val jsonPayload = """{"node":"${device.node}"}"""
                val body = jsonPayload.toRequestBody(jsonMediaType)
                Request.Builder()
                    .url(device.powerOffUrl)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Secret-Key", device.secretKey)
                    .post(body)
                    .build()
            }
            TriggerAction.REBOOT -> {
                val jsonPayload = """{"node":"${device.node}"}"""
                val body = jsonPayload.toRequestBody(jsonMediaType)
                Request.Builder()
                    .url(device.rebootUrl)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Secret-Key", device.secretKey)
                    .post(body)
                    .build()
            }
        }
    }

    fun sendTrigger(
        device: DeviceConfig,
        action: TriggerAction,
        callback: (TriggerResponse) -> Unit
    ) {
        val request = try {
            buildRequest(device, action)
        } catch (e: Exception) {
            callback(
                TriggerResponse(
                    isSuccess = false,
                    statusCode = -1,
                    message = "Invalid URL or configuration: ${e.message}"
                )
            )
            return
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(
                    TriggerResponse(
                        isSuccess = false,
                        statusCode = -1,
                        message = e.localizedMessage ?: "Network connection failed"
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val code = it.code
                    val bodyText = it.body?.string().orEmpty()
                    callback(
                        TriggerResponse(
                            isSuccess = it.isSuccessful,
                            statusCode = code,
                            message = if (bodyText.isNotEmpty()) bodyText else "HTTP $code"
                        )
                    )
                }
            }
        })
    }

    fun fetchR2Config(
        url: String,
        callback: (Result<ParsedEndpoints>) -> Unit
    ) {
        val request = try {
            Request.Builder().url(url).get().build()
        } catch (e: Exception) {
            callback(Result.failure(e))
            return
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(Result.failure(IOException("Failed to fetch R2 JSON: HTTP ${it.code}")))
                        return
                    }
                    val jsonString = it.body?.string().orEmpty()
                    val parseResult = R2ConfigParser.parse(jsonString)
                    callback(parseResult)
                }
            }
        })
    }

    fun checkStatus(
        device: DeviceConfig,
        callback: (NodeStatus) -> Unit
    ) {
        val baseStatusUrl = if (device.statusUrl.isNotBlank()) {
            device.statusUrl
        } else {
            "https://cosmos.linuxguy.space/status?node=${device.node}"
        }

        val targetUrl = if (!baseStatusUrl.contains("node=") && device.node.isNotBlank()) {
            if (baseStatusUrl.contains("?")) {
                "$baseStatusUrl&node=${device.node}"
            } else {
                "$baseStatusUrl?node=${device.node}"
            }
        } else {
            baseStatusUrl
        }

        val request = try {
            Request.Builder().url(targetUrl).get().build()
        } catch (e: Exception) {
            callback(NodeStatus(isOnline = false, statusText = "INVALID URL"))
            return
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(NodeStatus(isOnline = false, statusText = "OFFLINE"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(NodeStatus(isOnline = false, statusText = "OFFLINE"))
                        return
                    }
                    val body = it.body?.string().orEmpty().trim()
                    if (body.startsWith("{")) {
                        try {
                            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
                            val online = when {
                                json.has("online") && json.get("online").isJsonPrimitive && json.get("online").asJsonPrimitive.isBoolean ->
                                    json.get("online").asBoolean
                                json.has("status") && json.get("status").isJsonPrimitive ->
                                    json.get("status").asString.equals("online", ignoreCase = true)
                                else -> false
                            }
                            val ip = if (json.has("ip") && json.get("ip").isJsonPrimitive) json.get("ip").asString else null
                            callback(NodeStatus(isOnline = online, statusText = if (online) "ONLINE" else "OFFLINE", ip = ip))
                        } catch (e: Exception) {
                            callback(NodeStatus(isOnline = false, statusText = "ERROR"))
                        }
                    } else {
                        val isOnline = body.equals("ONLINE", ignoreCase = true)
                        callback(NodeStatus(isOnline = isOnline, statusText = if (isOnline) "ONLINE" else "OFFLINE"))
                    }
                }
            }
        })
    }
}
