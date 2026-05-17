package com.kktaro.simplifyweightrecorder.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeWeightCandidatesUseCaseTest {

    private val useCase = ComputeWeightCandidatesUseCase()

    @Test
    fun `returns 11 candidates centered on baseline when base is provided`() {
        val candidates = useCase(65.0)

        assertEquals(11, candidates.size)
        assertEquals(64.5, candidates.first().kg, 0.0001)
        assertEquals(65.5, candidates.last().kg, 0.0001)
        assertEquals(1, candidates.count { it.isBaseline })
        assertEquals(65.0, candidates.single { it.isBaseline }.kg, 0.0001)
    }

    @Test
    fun `uses default 60_0 when baseline is null`() {
        val candidates = useCase(null)

        assertEquals(11, candidates.size)
        assertEquals(60.0, candidates.single { it.isBaseline }.kg, 0.0001)
        assertEquals(59.5, candidates.first().kg, 0.0001)
        assertEquals(60.5, candidates.last().kg, 0.0001)
    }

    @Test
    fun `excludes non-positive values for small baselines`() {
        val candidates = useCase(0.3)

        assertTrue(candidates.all { it.kg > 0.0 })
        assertEquals(8, candidates.size)
        assertEquals(0.1, candidates.first().kg, 0.0001)
        assertEquals(0.8, candidates.last().kg, 0.0001)
    }

    @Test
    fun `excludes values above max for large baselines`() {
        val candidates = useCase(500.0)

        assertEquals(6, candidates.size)
        assertEquals(499.5, candidates.first().kg, 0.0001)
        assertEquals(500.0, candidates.last().kg, 0.0001)
        assertEquals(500.0, candidates.single { it.isBaseline }.kg, 0.0001)
    }

    @Test
    fun `rounds fractional baseline to nearest 0_1`() {
        val candidates = useCase(65.45)

        assertEquals(65.5, candidates.single { it.isBaseline }.kg, 0.0001)
    }

    @Test
    fun `produces values without floating point noise`() {
        val candidates = useCase(65.4)

        val expectedKgs = listOf(64.9, 65.0, 65.1, 65.2, 65.3, 65.4, 65.5, 65.6, 65.7, 65.8, 65.9)
        assertEquals(expectedKgs, candidates.map { it.kg })
    }
}
