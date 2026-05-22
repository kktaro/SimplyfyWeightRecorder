package com.kktaro.simplifyweightrecorder.domain.model

import java.time.Instant
import java.time.ZoneOffset

data class WeightRecordEntry(
    val id: String,
    val weightKg: Double,
    val recordedAt: Instant,
    val zoneOffset: ZoneOffset?
)
