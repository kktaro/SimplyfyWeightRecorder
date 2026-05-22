package com.kktaro.simplifyweightrecorder.data.repository

import android.content.Context
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectClientProvider
import com.kktaro.simplifyweightrecorder.domain.model.WeightReadError
import com.kktaro.simplifyweightrecorder.domain.model.WeightRecordEntry
import com.kktaro.simplifyweightrecorder.domain.model.WeightSaveError
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

class HealthConnectWeightRepository @Inject constructor(
    private val provider: HealthConnectClientProvider,
    @ApplicationContext private val context: Context
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

    override suspend fun readWeights(
        startTime: Instant,
        endTime: Instant
    ): Result<List<WeightRecordEntry>> = runCatching {
        val client = provider.getClientOrNull()
            ?: throw WeightReadError.HealthConnectUnavailable
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
            dataOriginFilter = setOf(DataOrigin(context.packageName))
        )
        client.readRecords(request).records.map { record ->
            WeightRecordEntry(
                id = record.metadata.id,
                weightKg = record.weight.inKilograms,
                recordedAt = record.time,
                zoneOffset = record.zoneOffset
            )
        }
    }.recoverCatching { throwable ->
        throw when (throwable) {
            is WeightReadError -> throwable
            is SecurityException -> WeightReadError.PermissionDenied
            else -> WeightReadError.Unknown(throwable)
        }
    }
}
