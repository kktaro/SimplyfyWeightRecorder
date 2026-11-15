package com.kktaro.simplifyweightrecorder.data.repository

import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectClientProvider
import com.kktaro.simplifyweightrecorder.domain.model.WeightMeasurement
import com.kktaro.simplifyweightrecorder.domain.model.WeightReadError
import java.time.Instant
import javax.inject.Inject

class HealthConnectWeightHistoryRepository @Inject constructor(
    private val provider: HealthConnectClientProvider
) : WeightHistoryRepository {

    override suspend fun readWeights(
        start: Instant,
        end: Instant
    ): Result<List<WeightMeasurement>> = runCatching {
        val client = provider.getClientOrNull()
            ?: throw WeightReadError.HealthConnectUnavailable
        val measurements = mutableListOf<WeightMeasurement>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageToken = pageToken
                )
            )
            response.records.mapTo(measurements) {
                WeightMeasurement(time = it.time, kg = it.weight.inKilograms)
            }
            pageToken = response.pageToken
        } while (pageToken != null)
        measurements.toList()
    }.recoverCatching { throwable ->
        throw when (throwable) {
            is WeightReadError -> throwable
            is SecurityException -> WeightReadError.PermissionDenied
            else -> WeightReadError.Unknown(throwable)
        }
    }

    override suspend fun hasReadPermission(): Boolean {
        val client = provider.getClientOrNull() ?: return false
        return runCatching {
            READ_WEIGHT in client.permissionController.getGrantedPermissions()
        }.getOrDefault(false)
    }

    override suspend fun readPermissionsToRequest(): Set<String> {
        val client = provider.getClientOrNull() ?: return setOf(READ_WEIGHT)
        val historyAvailable = runCatching {
            client.features.getFeatureStatus(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY
            ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        }.getOrDefault(false)
        return if (historyAvailable) {
            setOf(READ_WEIGHT, HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
        } else {
            setOf(READ_WEIGHT)
        }
    }

    private companion object {
        val READ_WEIGHT = HealthPermission.getReadPermission(WeightRecord::class)
    }
}
