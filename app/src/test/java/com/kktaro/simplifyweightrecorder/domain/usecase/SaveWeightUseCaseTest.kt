package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.data.repository.WeightRepository
import com.kktaro.simplifyweightrecorder.domain.model.WeightRecordEntry
import com.kktaro.simplifyweightrecorder.domain.time.ClockProvider
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveWeightUseCaseTest {

    @Test
    fun `passes Asia Tokyo instant and offset to repository`() = runTest {
        val fixedTokyo = ZonedDateTime.of(
            2026, 5, 16, 12, 30, 0, 0,
            ZoneId.of("Asia/Tokyo")
        )
        val fakeClock = object : ClockProvider {
            override fun nowInTokyo(): ZonedDateTime = fixedTokyo
        }
        val recorded = mutableListOf<Triple<Double, Instant, ZoneOffset>>()
        val fakeRepo = object : WeightRepository {
            override suspend fun saveWeight(
                weightKg: Double,
                time: Instant,
                offset: ZoneOffset
            ): Result<Unit> {
                recorded.add(Triple(weightKg, time, offset))
                return Result.success(Unit)
            }

            override suspend fun readWeights(
                startTime: Instant,
                endTime: Instant
            ): Result<List<WeightRecordEntry>> = Result.success(emptyList())
        }
        val useCase = SaveWeightUseCase(fakeRepo, fakeClock)

        val result = useCase(65.4)

        assertTrue(result.isSuccess)
        assertEquals(1, recorded.size)
        val (kg, instant, offset) = recorded.single()
        assertEquals(65.4, kg, 0.0)
        assertEquals(fixedTokyo.toInstant(), instant)
        assertEquals(ZoneOffset.ofHours(9), offset)
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val fakeClock = object : ClockProvider {
            override fun nowInTokyo(): ZonedDateTime =
                ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        }
        val expected = RuntimeException("boom")
        val fakeRepo = object : WeightRepository {
            override suspend fun saveWeight(
                weightKg: Double,
                time: Instant,
                offset: ZoneOffset
            ): Result<Unit> = Result.failure(expected)

            override suspend fun readWeights(
                startTime: Instant,
                endTime: Instant
            ): Result<List<WeightRecordEntry>> = Result.success(emptyList())
        }
        val useCase = SaveWeightUseCase(fakeRepo, fakeClock)

        val result = useCase(70.0)

        assertTrue(result.isFailure)
        assertEquals(expected, result.exceptionOrNull())
    }
}
