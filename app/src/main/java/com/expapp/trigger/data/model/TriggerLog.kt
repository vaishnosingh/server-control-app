package com.expapp.trigger.data.model

import com.expapp.trigger.network.TriggerAction
import java.util.UUID

data class TriggerLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String,
    val deviceName: String,
    val node: String,
    val action: TriggerAction,
    val isSuccess: Boolean,
    val statusCode: Int,
    val message: String,
    val source: String = "App" // "App", "Widget", "NFC"
)
