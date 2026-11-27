package com.expapp.trigger.network

import android.os.Handler
import android.os.Looper
import com.expapp.trigger.data.model.CpuMetrics
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.DiskMetrics
import com.expapp.trigger.data.model.RamMetrics
import com.expapp.trigger.data.model.ScheduledTask
import com.expapp.trigger.data.model.ScheduledTasksResponse
import com.expapp.trigger.data.model.ServiceOperation
import com.expapp.trigger.data.model.SysPulseStatus
import com.expapp.trigger.data.model.SysServiceInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

class SysPulseStatusDeserializer : JsonDeserializer<SysPulseStatus> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SysPulseStatus {
        if (!json.isJsonObject) return SysPulseStatus()
        val obj = json.asJsonObject
        val cpu = if (obj.has("cpu") && obj.get("cpu").isJsonObject) {
            context.deserialize<CpuMetrics>(obj.get("cpu"), CpuMetrics::class.java) ?: CpuMetrics()
        } else CpuMetrics()
        val ram = if (obj.has("ram") && obj.get("ram").isJsonObject) {
            context.deserialize<RamMetrics>(obj.get("ram"), RamMetrics::class.java) ?: RamMetrics()
        } else RamMetrics()
        val disk = if (obj.has("disk") && obj.get("disk").isJsonArray) {
            val listType = object : TypeToken<List<DiskMetrics>>() {}.type
            context.deserialize<List<DiskMetrics>>(obj.get("disk"), listType) ?: emptyList()
        } else emptyList()
        val services = if (obj.has("services") && obj.get("services").isJsonArray) {
            val listType = object : TypeToken<List<SysServiceInfo>>() {}.type
            context.deserialize<List<SysServiceInfo>>(obj.get("services"), listType) ?: emptyList()
        } else emptyList()
        val uptime = if (obj.has("uptime") && !obj.get("uptime").isJsonNull && obj.get("uptime").isJsonPrimitive) {
            try { obj.get("uptime").asString ?: "" } catch (_: Exception) { "" }
        } else ""
        val success = if (obj.has("success") && !obj.get("success").isJsonNull && obj.get("success").isJsonPrimitive) {
            try { obj.get("success").asBoolean } catch (_: Exception) { true }
        } else true

        return SysPulseStatus(
            success = success,
            uptime = uptime,
            cpu = cpu,
            ram = ram,
            disk = disk,
            services = services
        )
    }
}

class SysPulseClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(SysPulseStatus::class.java, SysPulseStatusDeserializer())
        .create()
) {
    companion object {
        const val DEFAULT_FALLBACK_SECRET = "default_pulse_secret_key_change_me"
        const val HEADER_SECRET_KEY = "X-Secret-Key"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val shared by lazy { SysPulseClient() }
    }

    private val mainHandler = try {
        val looper = Looper.getMainLooper()
        if (looper != null) Handler(looper) else null
    } catch (_: Throwable) {
        null
    }

    private fun runOnMain(block: () -> Unit) {
        mainHandler?.post(block) ?: block()
    }

    fun resolveBaseUrl(device: DeviceConfig, nodeStatus: NodeStatus? = null): String {
        if (device.syspulseUrl.isNotBlank()) {
            return device.syspulseUrl.trim()
                .removeSuffix("/")
                .removeSuffix("/api/status")
                .removeSuffix("/api")
                .removeSuffix("/")
        }
        val ip = nodeStatus?.ip?.trim()
        if (!ip.isNullOrBlank()) {
            return "http://$ip:8088"
        }
        return ""
    }

    fun resolveSecretKey(device: DeviceConfig): String {
        return when {
            device.syspulseSecretKey.isNotBlank() -> device.syspulseSecretKey.trim()
            device.secretKey.isNotBlank() -> device.secretKey.trim()
            else -> DEFAULT_FALLBACK_SECRET
        }
    }

    /**
     * Asynchronously fetches concise hardware metrics using device configuration and optional node status.
     */
    fun fetchStatus(
        device: DeviceConfig,
        nodeStatus: NodeStatus? = null,
        onResult: (Result<SysPulseStatus>) -> Unit
    ) {
        val baseUrl = resolveBaseUrl(device, nodeStatus)
        if (baseUrl.isBlank()) {
            runOnMain { onResult(Result.failure(IllegalStateException("No SysPulse URL available"))) }
            return
        }
        val secretKey = resolveSecretKey(device)
        fetchStatus(baseUrl, secretKey, onResult)
    }

    /**
     * Asynchronously fetches concise hardware metrics (CPU, RAM, Disk, Host Uptime) and systemd services.
     */
    fun fetchStatus(
        baseUrl: String,
        secretKey: String,
        onResult: (Result<SysPulseStatus>) -> Unit
    ) {
        val url = "${baseUrl.removeSuffix("/")}/api/status"
        val request = Request.Builder()
            .url(url)
            .addHeader(HEADER_SECRET_KEY, secretKey)
            .get()
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain { onResult(Result.failure(e)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (res.code == 401 && secretKey != DEFAULT_FALLBACK_SECRET) {
                        // Secret key failed; retry with default pulse secret key fallback
                        fetchStatus(baseUrl, DEFAULT_FALLBACK_SECRET, onResult)
                        return
                    }

                    if (!res.isSuccessful) {
                        runOnMain {
                            onResult(Result.failure(IOException("HTTP ${res.code}: ${res.message}")))
                        }
                        return
                    }

                    val body = res.body?.string().orEmpty()
                    try {
                        val status = gson.fromJson(body, SysPulseStatus::class.java)
                        runOnMain { onResult(Result.success(status)) }
                    } catch (e: Exception) {
                        runOnMain { onResult(Result.failure(e)) }
                    }
                }
            }
        })
    }

    /**
     * Executes an immediate or scheduled service operation (start, stop, restart, enable, disable).
     * Dedicated /api/service/enable and /api/service/disable endpoints are used for enable/disable
     * with automatic fallback to /api/service/operate if the backend returns 404.
     */
    fun operateService(
        baseUrl: String,
        secretKey: String,
        serviceName: String,
        operation: ServiceOperation,
        delaySeconds: Long? = null,
        onResult: (Result<String>) -> Unit
    ) {
        val cleanBase = baseUrl.removeSuffix("/")
        val useDedicatedEndpoint =
            operation == ServiceOperation.ENABLE || operation == ServiceOperation.DISABLE

        val endpointPath = when (operation) {
            ServiceOperation.ENABLE -> "/api/service/enable"
            ServiceOperation.DISABLE -> "/api/service/disable"
            else -> "/api/service/operate"
        }

        executeServiceCall(
            baseUrl = cleanBase,
            endpointPath = endpointPath,
            secretKey = secretKey,
            serviceName = serviceName,
            operation = operation,
            delaySeconds = delaySeconds,
            includeOperationField = !useDedicatedEndpoint,
            allowOperateFallback = useDedicatedEndpoint,
            onResult = onResult
        )
    }

    private fun executeServiceCall(
        baseUrl: String,
        endpointPath: String,
        secretKey: String,
        serviceName: String,
        operation: ServiceOperation,
        delaySeconds: Long?,
        includeOperationField: Boolean,
        allowOperateFallback: Boolean,
        onResult: (Result<String>) -> Unit
    ) {
        val url = "$baseUrl$endpointPath"
        val payload = JsonObject().apply {
            addProperty("servicename", serviceName)
            if (includeOperationField) {
                addProperty("operation", operation.apiName)
            }
            if (delaySeconds != null && delaySeconds > 0) {
                addProperty("delay_seconds", delaySeconds)
                when (operation) {
                    ServiceOperation.START -> addProperty("time_to_start_after", delaySeconds)
                    ServiceOperation.STOP -> addProperty("time_to_stop_after", delaySeconds)
                    ServiceOperation.ENABLE -> addProperty("time_to_enable_after", delaySeconds)
                    ServiceOperation.DISABLE -> addProperty("time_to_disable_after", delaySeconds)
                    ServiceOperation.RESTART -> Unit
                }
            }
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .addHeader(HEADER_SECRET_KEY, secretKey)
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain { onResult(Result.failure(e)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (res.code == 401 && secretKey != DEFAULT_FALLBACK_SECRET) {
                        executeServiceCall(
                            baseUrl = baseUrl,
                            endpointPath = endpointPath,
                            secretKey = DEFAULT_FALLBACK_SECRET,
                            serviceName = serviceName,
                            operation = operation,
                            delaySeconds = delaySeconds,
                            includeOperationField = includeOperationField,
                            allowOperateFallback = allowOperateFallback,
                            onResult = onResult
                        )
                        return
                    }

                    if (res.code == 404 && allowOperateFallback) {
                        executeServiceCall(
                            baseUrl = baseUrl,
                            endpointPath = "/api/service/operate",
                            secretKey = secretKey,
                            serviceName = serviceName,
                            operation = operation,
                            delaySeconds = delaySeconds,
                            includeOperationField = true,
                            allowOperateFallback = false,
                            onResult = onResult
                        )
                        return
                    }

                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        val errorMsg = try {
                            val json = JsonParser.parseString(body).asJsonObject
                            json.get("error")?.asString ?: "HTTP ${res.code}: ${res.message}"
                        } catch (_: Exception) {
                            "HTTP ${res.code}: ${res.message}"
                        }
                        runOnMain { onResult(Result.failure(IOException(errorMsg))) }
                        return
                    }

                    val message = try {
                        val json = JsonParser.parseString(body).asJsonObject
                        json.get("message")?.asString ?: "Operation '${operation.displayName}' sent successfully"
                    } catch (_: Exception) {
                        "Operation '${operation.displayName}' sent successfully"
                    }

                    runOnMain { onResult(Result.success(message)) }
                }
            }
        })
    }

    /**
     * Lists active and pending scheduled operations.
     */
    fun fetchTasks(
        baseUrl: String,
        secretKey: String,
        onResult: (Result<List<ScheduledTask>>) -> Unit
    ) {
        val url = "${baseUrl.removeSuffix("/")}/api/service/tasks"
        val request = Request.Builder()
            .url(url)
            .addHeader(HEADER_SECRET_KEY, secretKey)
            .get()
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain { onResult(Result.failure(e)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (res.code == 401 && secretKey != DEFAULT_FALLBACK_SECRET) {
                        fetchTasks(baseUrl, DEFAULT_FALLBACK_SECRET, onResult)
                        return
                    }

                    if (!res.isSuccessful) {
                        runOnMain { onResult(Result.failure(IOException("HTTP ${res.code}"))) }
                        return
                    }

                    val body = res.body?.string().orEmpty()
                    try {
                        val resp = gson.fromJson(body, ScheduledTasksResponse::class.java)
                        runOnMain { onResult(Result.success(resp.tasks)) }
                    } catch (e: Exception) {
                        runOnMain { onResult(Result.failure(e)) }
                    }
                }
            }
        })
    }

    /**
     * Cancels a pending scheduled service operation.
     */
    fun cancelTask(
        baseUrl: String,
        secretKey: String,
        taskId: String,
        onResult: (Result<String>) -> Unit
    ) {
        val url = "${baseUrl.removeSuffix("/")}/api/service/cancel-task"
        val payload = JsonObject().apply {
            addProperty("task_id", taskId)
        }
        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .addHeader(HEADER_SECRET_KEY, secretKey)
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain { onResult(Result.failure(e)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (res.code == 401 && secretKey != DEFAULT_FALLBACK_SECRET) {
                        cancelTask(baseUrl, DEFAULT_FALLBACK_SECRET, taskId, onResult)
                        return
                    }

                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        val errorMsg = try {
                            val json = JsonParser.parseString(body).asJsonObject
                            json.get("error")?.asString ?: "HTTP ${res.code}"
                        } catch (_: Exception) {
                            "HTTP ${res.code}"
                        }
                        runOnMain { onResult(Result.failure(IOException(errorMsg))) }
                        return
                    }

                    val message = try {
                        val json = JsonParser.parseString(body).asJsonObject
                        json.get("message")?.asString ?: "Task cancelled"
                    } catch (_: Exception) {
                        "Task cancelled"
                    }
                    runOnMain { onResult(Result.success(message)) }
                }
            }
        })
    }
}
