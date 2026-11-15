package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.data.repository.WeightRepository
import com.kktaro.simplifyweightrecorder.domain.model.WeightReadError
import com.kktaro.simplifyweightrecorder.domain.model.WeightRecordEntry
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadWeightsUseCaseTest {

    @Test
    fun `passes start and end time to repository and returns its result`() = runTest {
        val expected = listOf(
            WeightRecordEntry(
                id = "id-1",
                weightKg = 60.0,
                recordedAt = Instant.ofEpochSecond(1_700_000_000),
                zoneOffset = ZoneOffset.ofHours(9)
            )
        )
        val recorded = mutableListOf<Pair<Instant, Instant>>()
        val fakeRepo = object : WeightRepository {
            override suspend fun saveWeight(
                weightKg: Double,
                time: Instant,
                offset: ZoneOffset
            ): Result<Unit> = Result.success(Unit)

            override suspend fun readWeights(
                startTime: Instant,
                endTime: Instant
            ): Result<List<WeightRecordEntry>> {
                recorded.add(startTime to endTime)
                return Result.success(expected)
            }
        }
        val useCase = ReadWeightsUseCase(fakeRepo)
        val start = Instant.ofEpochSecond(1_600_000_000)
        val end = Instant.ofEpochSecond(1_700_000_000)

        val result = useCase(start, end)

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
        assertEquals(listOf(start to end), recorded)
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val fakeRepo = object : WeightRepository {
            override suspend fun saveWeight(
                weightKg: Double,
                time: Instant,
                offset: ZoneOffset
            ): Result<Unit> = Result.success(Unit)

            override suspend fun readWeights(
                startTime: Instant,
                endTime: Instant
            ): Result<List<WeightRecordEntry>> =
                Result.failure(WeightReadError.PermissionDenied)
        }
        val useCase = ReadWeightsUseCase(fakeRepo)

        val result = useCase(Instant.EPOCH, Instant.ofEpochSecond(1))

        assertTrue(result.isFailure)
        assertEquals(WeightReadError.PermissionDenied, result.exceptionOrNull())
    }
}
