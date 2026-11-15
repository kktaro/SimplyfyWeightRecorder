package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.data.repository.WeightRepository
import com.kktaro.simplifyweightrecorder.domain.model.WeightRecordEntry
import java.time.Instant
import javax.inject.Inject

class ReadWeightsUseCase @Inject constructor(
    private val repository: WeightRepository
) {
    suspend operator fun invoke(
        startTime: Instant,
        endTime: Instant
    ): Result<List<WeightRecordEntry>> = repository.readWeights(startTime, endTime)
}
