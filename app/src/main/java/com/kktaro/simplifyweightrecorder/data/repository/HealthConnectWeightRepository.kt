package com.kktaro.simplifyweightrecorder.data.repository

import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectClientProvider
import com.kktaro.simplifyweightrecorder.domain.model.WeightSaveError
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

class HealthConnectWeightRepository @Inject constructor(
    private val provider: HealthConnectClientProvider
) : WeightRepository {
    override suspend fun saveWeight(
        weightKg: Double,
        time: Instant,
        offset: ZoneOffset
    ): Result<Unit> = runCatching {
        val client = provider.getClientOrNull()
            ?: throw WeightSaveError.HealthConnectUnavailable
        val record = WeightRecord(
            weight = Mass.kilograms(weightKg),
            time = time,
            zoneOffset = offset,
            metadata = Metadata.manualEntry()
        )
        client.insertRecords(listOf(record))
        Unit
    }.recoverCatching { throwable ->
        throw when (throwable) {
            is WeightSaveError -> throwable
            is SecurityException -> WeightSaveError.PermissionDenied
            else -> WeightSaveError.Unknown(throwable)
        }
    }
}
