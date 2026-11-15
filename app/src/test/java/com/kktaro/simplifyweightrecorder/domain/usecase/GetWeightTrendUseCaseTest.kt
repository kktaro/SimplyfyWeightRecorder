package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.data.repository.WeightHistoryRepository
import com.kktaro.simplifyweightrecorder.domain.model.DailyWeight
import com.kktaro.simplifyweightrecorder.domain.model.TrendPeriod
import com.kktaro.simplifyweightrecorder.domain.model.WeightMeasurement
import com.kktaro.simplifyweightrecorder.domain.model.WeightReadError
import com.kktaro.simplifyweightrecorder.domain.time.ClockProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWeightTrendUseCaseTest {

    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val now = ZonedDateTime.of(2026, 5, 16, 12, 0, 0, 0, tokyo)
    private val clock = object : ClockProvider {
        override fun nowInTokyo(): ZonedDateTime = now
    }

    private class FakeHistoryRepository(
        private val result: Result<List<WeightMeasurement>> = Result.success(emptyList())
    ) : WeightHistoryRepository {
        val requestedRanges = mutableListOf<Pair<Instant, Instant>>()

        override suspend fun readWeights(start: Instant, end: Instant): Result<List<WeightMeasurement>> {
            requestedRanges.add(start to end)
            return result
        }

        override suspend fun hasReadPermission(): Boolean = true

        override suspend fun readPermissionsToRequest(): Set<String> = emptySet()
    }

    private fun tokyoInstant(y: Int, m: Int, d: Int, h: Int, min: Int = 0): Instant =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, tokyo).toInstant()

    @Test
    fun `month period reads from start of the day one month ago plus one day until now`() = runTest {
        val repo = FakeHistoryRepository()

        GetWeightTrendUseCase(repo, clock)(TrendPeriod.MONTH)

        assertEquals(
            listOf(tokyoInstant(2026, 4, 17, 0) to now.toInstant()),
            repo.requestedRanges
        )
    }

    @Test
    fun `each period starts at the expected Tokyo day`() = runTest {
        val expectedStarts = mapOf(
            TrendPeriod.WEEK to tokyoInstant(2026, 5, 10, 0),
            TrendPeriod.THREE_MONTHS to tokyoInstant(2026, 2, 17, 0),
            TrendPeriod.YEAR to tokyoInstant(2025, 5, 17, 0)
        )
        expectedStarts.forEach { (period, expectedStart) ->
            val repo = FakeHistoryRepository()

            GetWeightTrendUseCase(repo, clock)(period)

            assertEquals(period.name, expectedStart, repo.requestedRanges.single().first)
        }
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val error = WeightReadError.PermissionDenied
        val repo = FakeHistoryRepository(Result.failure(error))

        val result = GetWeightTrendUseCase(repo, clock)(TrendPeriod.MONTH)

        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `uses the last measurement of each Tokyo day sorted by date`() = runTest {
        val repo = FakeHistoryRepository(
            Result.success(
                listOf(
                    // 5/15 23:30 JST (= 5/15 14:30 UTC) と 5/16 00:30 JST (= 5/15 15:30 UTC) は別の日
                    WeightMeasurement(tokyoInstant(2026, 5, 16, 0, 30), 61.0),
                    WeightMeasurement(tokyoInstant(2026, 5, 15, 23, 30), 62.5),
                    WeightMeasurement(tokyoInstant(2026, 5, 15, 7, 0), 63.0),
                    WeightMeasurement(tokyoInstant(2026, 5, 10, 8, 0), 64.0)
                )
            )
        )

        val result = GetWeightTrendUseCase(repo, clock)(TrendPeriod.MONTH)

        assertEquals(
            listOf(
                DailyWeight(LocalDate.of(2026, 5, 10), 64.0),
                DailyWeight(LocalDate.of(2026, 5, 15), 62.5),
                DailyWeight(LocalDate.of(2026, 5, 16), 61.0)
            ),
            result.getOrThrow()
        )
    }
}
