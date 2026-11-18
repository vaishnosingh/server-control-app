package com.expapp.trigger.network

enum class TriggerAction {
    POWER_ON,
    POWER_OFF,
    REBOOT
}

data class TriggerResponse(
    val isSuccess: Boolean,
    val statusCode: Int,
    val message: String
)

data class NodeStatus(
    val isOnline: Boolean,
    val statusText: String,
    val ip: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
