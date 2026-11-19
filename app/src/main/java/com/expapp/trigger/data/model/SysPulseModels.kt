package com.expapp.trigger.data.model

import com.google.gson.annotations.SerializedName

data class SysPulseStatus(
    val success: Boolean = true,
    val uptime: String = "",
    val cpu: CpuMetrics = CpuMetrics(),
    val ram: RamMetrics = RamMetrics(),
    val disk: List<DiskMetrics> = emptyList(),
    val services: List<SysServiceInfo> = emptyList()
)

data class CpuMetrics(
    val pct: Double = 0.0,
    val cores: Int = 1,
    val load: List<Double> = emptyList()
)

data class RamMetrics(
    @SerializedName("used_mb")
    val usedMb: Long = 0,
    @SerializedName("total_mb")
    val totalMb: Long = 0,
    val pct: Double = 0.0
)

data class DiskMetrics(
    val mount: String = "",
    @SerializedName("used_gb")
    val usedGb: Double = 0.0,
    @SerializedName("total_gb")
    val totalGb: Double = 0.0,
    val pct: Double = 0.0
)

data class SysServiceInfo(
    val name: String = "",
    val unit: String = "",
    val active: Boolean = false,
    val enabled: Boolean = false,
    val state: String = "stopped",
    val pid: Long? = null
)

data class ScheduledTasksResponse(
    val success: Boolean = true,
    val count: Int = 0,
    val tasks: List<ScheduledTask> = emptyList()
)

data class ScheduledTask(
    @SerializedName("task_id")
    val taskId: String = "",
    @SerializedName("service_name")
    val serviceName: String = "",
    val operation: String = "",
    @SerializedName("delay_seconds")
    val delaySeconds: Long = 0,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("execute_at")
    val executeAt: String = "",
    val status: String = "pending"
)

enum class ServiceOperation(val apiName: String, val displayName: String) {
    START("start", "Start"),
    STOP("stop", "Stop"),
    RESTART("restart", "Restart"),
    ENABLE("enable", "Enable on Boot"),
    DISABLE("disable", "Disable on Boot")
}
