package com.kktaro.simplifyweightrecorder.data.repository

import com.kktaro.simplifyweightrecorder.domain.model.WeightRecordEntry
import java.time.Instant
import java.time.ZoneOffset

interface WeightRepository {
    suspend fun saveWeight(weightKg: Double, time: Instant, offset: ZoneOffset): Result<Unit>

    suspend fun readWeights(
        startTime: Instant,
        endTime: Instant
    ): Result<List<WeightRecordEntry>>
}
