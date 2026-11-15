package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.data.repository.WeightMeasurementRepository
import com.kktaro.simplifyweightrecorder.domain.model.DailyWeight
import com.kktaro.simplifyweightrecorder.domain.model.TrendPeriod
import com.kktaro.simplifyweightrecorder.domain.time.ClockProvider
import javax.inject.Inject

class GetWeightTrendUseCase @Inject constructor(
    private val repository: WeightMeasurementRepository,
    private val clock: ClockProvider
) {
    suspend operator fun invoke(period: TrendPeriod): Result<List<DailyWeight>> {
        val now = clock.nowInTokyo()
        val start = period.startDate(now.toLocalDate()).atStartOfDay(now.zone).toInstant()
        return repository.readWeights(start, now.toInstant()).map { measurements ->
            measurements
                .groupBy { it.time.atZone(now.zone).toLocalDate() }
                .map { (date, sameDay) -> DailyWeight(date, sameDay.maxBy { it.time }.kg) }
                .sortedBy { it.date }
        }
    }
}
