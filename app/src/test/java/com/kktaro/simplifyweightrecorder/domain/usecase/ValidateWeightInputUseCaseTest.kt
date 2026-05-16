package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.domain.model.WeightInputResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateWeightInputUseCaseTest {

    private lateinit var useCase: ValidateWeightInputUseCase

    @Before
    fun setUp() {
        useCase = ValidateWeightInputUseCase()
    }

    @Test
    fun `empty string returns Empty`() {
        assertEquals(WeightInputResult.Empty, useCase(""))
    }

    @Test
    fun `non numeric returns NotANumber`() {
        assertEquals(WeightInputResult.NotANumber, useCase("abc"))
    }

    @Test
    fun `zero returns OutOfRange`() {
        assertEquals(WeightInputResult.OutOfRange, useCase("0"))
    }

    @Test
    fun `over 500 returns OutOfRange`() {
        assertEquals(WeightInputResult.OutOfRange, useCase("500.1"))
    }

    @Test
    fun `exactly 500 returns Valid`() {
        val result = useCase("500")
        assertTrue(result is WeightInputResult.Valid)
        assertEquals(500.0, (result as WeightInputResult.Valid).kg, 0.0)
    }

    @Test
    fun `two decimals returns TooManyDecimals`() {
        assertEquals(WeightInputResult.TooManyDecimals, useCase("65.45"))
    }

    @Test
    fun `one decimal returns Valid`() {
        val result = useCase("65.4")
        assertTrue(result is WeightInputResult.Valid)
        assertEquals(65.4, (result as WeightInputResult.Valid).kg, 0.0001)
    }

    @Test
    fun `integer value returns Valid`() {
        val result = useCase("70")
        assertTrue(result is WeightInputResult.Valid)
        assertEquals(70.0, (result as WeightInputResult.Valid).kg, 0.0)
    }

    @Test
    fun `negative value returns NotANumber due to leading sign rejection`() {
        // 正規表現で先頭 '-' が拒否されるためフォーマット不一致 → NotANumber
        assertEquals(WeightInputResult.NotANumber, useCase("-1"))
    }
}
