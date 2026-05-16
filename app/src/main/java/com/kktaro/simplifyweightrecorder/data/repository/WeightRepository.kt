package com.kktaro.simplifyweightrecorder.data.repository

import java.time.Instant
import java.time.ZoneOffset

interface WeightRepository {
    suspend fun saveWeight(weightKg: Double, time: Instant, offset: ZoneOffset): Result<Unit>
}
