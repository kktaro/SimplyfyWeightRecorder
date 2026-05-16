package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.data.repository.WeightRepository
import com.kktaro.simplifyweightrecorder.domain.time.ClockProvider
import javax.inject.Inject

class SaveWeightUseCase @Inject constructor(
    private val repository: WeightRepository,
    private val clock: ClockProvider
) {
    suspend operator fun invoke(weightKg: Double): Result<Unit> {
        val now = clock.nowInTokyo()
        return repository.saveWeight(
            weightKg = weightKg,
            time = now.toInstant(),
            offset = now.offset
        )
    }
}
