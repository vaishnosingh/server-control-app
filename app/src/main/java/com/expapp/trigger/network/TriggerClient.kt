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

    private val mainHandler = try {
        val looper = android.os.Looper.getMainLooper()
        if (looper != null) android.os.Handler(looper) else null
    } catch (e: Throwable) {
        null
    }

    private fun runOnMain(block: () -> Unit) {
        val handler = mainHandler
        if (handler != null) {
            handler.post(block)
        } else {
            block()
        }
    }

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
            runOnMain {
                callback(
                    TriggerResponse(
                        isSuccess = false,
                        statusCode = -1,
                        message = "Invalid URL or configuration: ${e.message}"
                    )
                )
            }
            return
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain {
                    callback(
                        TriggerResponse(
                            isSuccess = false,
                            statusCode = -1,
                            message = e.localizedMessage ?: "Network connection failed"
                        )
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val code = it.code
                    val bodyText = it.body?.string().orEmpty()
                    runOnMain {
                        callback(
                            TriggerResponse(
                                isSuccess = it.isSuccessful,
                                statusCode = code,
                                message = if (bodyText.isNotEmpty()) bodyText else "HTTP $code"
                            )
                        )
                    }
                }
            }
        })
    }

    fun sendTriggerSync(device: DeviceConfig, action: TriggerAction): TriggerResponse {
        return try {
            val request = buildRequest(device, action)
            val response = client.newCall(request).execute()
            response.use {
                val code = it.code
                val bodyText = it.body?.string().orEmpty()
                TriggerResponse(
                    isSuccess = it.isSuccessful,
                    statusCode = code,
                    message = if (bodyText.isNotEmpty()) bodyText else "HTTP $code"
                )
            }
        } catch (e: Exception) {
            TriggerResponse(
                isSuccess = false,
                statusCode = -1,
                message = e.localizedMessage ?: "Request failed"
            )
        }
    }

    fun fetchR2Config(
        url: String,
        callback: (Result<ParsedEndpoints>) -> Unit
    ) {
        val request = try {
            Request.Builder().url(url).get().build()
        } catch (e: Exception) {
            runOnMain { callback(Result.failure(e)) }
            return
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain { callback(Result.failure(e)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        runOnMain {
                            callback(Result.failure(IOException("Failed to fetch R2 JSON: HTTP ${it.code}")))
                        }
                        return
                    }
                    val jsonString = it.body?.string().orEmpty()
                    val parseResult = R2ConfigParser.parse(jsonString)
                    runOnMain { callback(parseResult) }
                }
            }
        })
    }

    fun checkStatus(
        device: DeviceConfig,
        callback: (NodeStatus) -> Unit
    ) {
        val baseStatusUrl = device.statusUrl.trim()
        if (baseStatusUrl.isBlank()) {
            runOnMain {
                callback(NodeStatus(isOnline = false, statusText = "NO STATUS URL", isReachable = false))
            }
            return
        }

        val request = try {
            Request.Builder().url(baseStatusUrl).get().build()
        } catch (e: Exception) {
            runOnMain {
                callback(NodeStatus(isOnline = false, statusText = "INVALID URL", isReachable = false))
            }
            return
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain {
                    callback(NodeStatus(isOnline = false, statusText = "OFFLINE", isReachable = false))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val nodeStatus = response.use {
                    val body = it.body?.string().orEmpty().trim()
                    parseNodeStatus(body, it.isSuccessful, device.node)
                }
                runOnMain { callback(nodeStatus) }
            }
        })
    }

    fun checkStatusSync(device: DeviceConfig): NodeStatus {
        val baseStatusUrl = device.statusUrl.trim()
        if (baseStatusUrl.isBlank()) {
            return NodeStatus(isOnline = false, statusText = "NO STATUS URL", isReachable = false)
        }

        return try {
            val request = Request.Builder().url(baseStatusUrl).get().build()
            val response = client.newCall(request).execute()
            response.use {
                val body = it.body?.string().orEmpty().trim()
                parseNodeStatus(body, it.isSuccessful, device.node)
            }
        } catch (e: Exception) {
            NodeStatus(isOnline = false, statusText = "OFFLINE", isReachable = false)
        }
    }

    private fun parseNodeStatus(body: String, isHttpSuccess: Boolean, targetNode: String = ""): NodeStatus {
        if (body.startsWith("{")) {
            return try {
                val rootJson = com.google.gson.JsonParser.parseString(body).asJsonObject

                // Support both single-node JSON ({"node":"debian", "online":true, ...})
                // and multi-node dashboard JSON ({"service":"Tailscale Sentinel", "nodes":{"debian":{...}, ...}})
                val json = if (rootJson.has("nodes") && rootJson.get("nodes").isJsonObject) {
                    val nodesObj = rootJson.getAsJsonObject("nodes")
                    var matchedObj: com.google.gson.JsonObject? = null
                    if (targetNode.isNotBlank()) {
                        for (key in nodesObj.keySet()) {
                            if (key.equals(targetNode, ignoreCase = true)) {
                                matchedObj = nodesObj.getAsJsonObject(key)
                                break
                            }
                        }
                    }
                    // If targetNode not matched or not provided, fallback to root or first entry
                    matchedObj ?: if (nodesObj.size() > 0) nodesObj.entrySet().first().value.asJsonObject else rootJson
                } else {
                    rootJson
                }

                val online = when {
                    json.has("online") && json.get("online").isJsonPrimitive && json.get("online").asJsonPrimitive.isBoolean ->
                        json.get("online").asBoolean
                    json.has("status") && json.get("status").isJsonPrimitive ->
                        json.get("status").asString.equals("online", ignoreCase = true)
                    else -> false
                }
                val ip = if (json.has("ip") && !json.get("ip").isJsonNull) json.get("ip").asString else null
                val uptime = when {
                    json.has("uptime") && !json.get("uptime").isJsonNull -> json.get("uptime").asString
                    json.has("on_since_duration") && !json.get("on_since_duration").isJsonNull -> json.get("on_since_duration").asString
                    else -> null
                }
                val onSinceIst = if (json.has("on_since_ist") && !json.get("on_since_ist").isJsonNull) {
                    json.get("on_since_ist").asString
                } else null
                val lastActiveAgo = if (json.has("last_active_ago") && !json.get("last_active_ago").isJsonNull) {
                    json.get("last_active_ago").asString
                } else null
                val lastActiveIst = if (json.has("last_active_ist") && !json.get("last_active_ist").isJsonNull) {
                    json.get("last_active_ist").asString
                } else null

                NodeStatus(
                    isOnline = online,
                    statusText = if (online) "ONLINE" else "OFFLINE",
                    ip = ip,
                    uptime = uptime,
                    onSinceIst = onSinceIst,
                    lastActiveAgo = lastActiveAgo,
                    lastActiveIst = lastActiveIst,
                    isReachable = true
                )
            } catch (e: Exception) {
                NodeStatus(isOnline = false, statusText = "ERROR", isReachable = true)
            }
        } else if (isHttpSuccess) {
            val isOnline = body.equals("ONLINE", ignoreCase = true)
            return NodeStatus(isOnline = isOnline, statusText = if (isOnline) "ONLINE" else "OFFLINE", isReachable = true)
        } else {
            return NodeStatus(isOnline = false, statusText = "OFFLINE", isReachable = false)
        }
    }
}
