package com.expapp.trigger.data.model

import java.util.UUID

data class DeviceConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val node: String,
    val secretKey: String,
    val r2Url: String,
    val powerOnUrl: String,
    val powerOffUrl: String,
    val rebootUrl: String
)
