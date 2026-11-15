package com.kktaro.simplifyweightrecorder.ui.weight

import com.kktaro.simplifyweightrecorder.data.repository.WeightMeasurementRepository
import com.kktaro.simplifyweightrecorder.domain.model.DailyWeight
import com.kktaro.simplifyweightrecorder.domain.model.TrendPeriod
import com.kktaro.simplifyweightrecorder.domain.model.WeightReadError
import com.kktaro.simplifyweightrecorder.domain.usecase.GetWeightTrendUseCase
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeightTrendViewModelTest {

    private val measurementRepository = mockk<WeightMeasurementRepository>()
    private val getTrend = mockk<GetWeightTrendUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = WeightTrendViewModel(getTrend, measurementRepository)

    @Test
    fun `refresh without read permission shows PermissionRequired`() = runTest {
        coEvery { measurementRepository.hasReadPermission() } returns false
        val vm = viewModel()

        vm.refresh()

        assertEquals(TrendContent.PermissionRequired, vm.uiState.value.content)
    }

    @Test
    fun `refresh with permission shows daily weights for the default month period`() = runTest {
        val points = listOf(
            DailyWeight(LocalDate.of(2026, 5, 15), 62.5),
            DailyWeight(LocalDate.of(2026, 5, 16), 62.1)
        )
        coEvery { measurementRepository.hasReadPermission() } returns true
        coEvery { getTrend(TrendPeriod.MONTH) } returns Result.success(points)
        val vm = viewModel()

        vm.refresh()

        assertEquals(
            WeightTrendUiState(TrendPeriod.MONTH, TrendContent.Loaded(points)),
            vm.uiState.value
        )
    }

    @Test
    fun `refresh with no measurements in the period shows Empty`() = runTest {
        coEvery { measurementRepository.hasReadPermission() } returns true
        coEvery { getTrend(TrendPeriod.MONTH) } returns Result.success(emptyList())
        val vm = viewModel()

        vm.refresh()

        assertEquals(TrendContent.Empty, vm.uiState.value.content)
    }

    @Test
    fun `refresh shows Error when reading fails`() = runTest {
        coEvery { measurementRepository.hasReadPermission() } returns true
        coEvery { getTrend(TrendPeriod.MONTH) } returns
            Result.failure(WeightReadError.Unknown(RuntimeException("boom")))
        val vm = viewModel()

        vm.refresh()

        assertEquals(TrendContent.Error, vm.uiState.value.content)
    }

    @Test
    fun `refresh shows PermissionRequired when permission is revoked while reading`() = runTest {
        coEvery { measurementRepository.hasReadPermission() } returns true
        coEvery { getTrend(TrendPeriod.MONTH) } returns Result.failure(WeightReadError.PermissionDenied)
        val vm = viewModel()

        vm.refresh()

        assertEquals(TrendContent.PermissionRequired, vm.uiState.value.content)
    }

    @Test
    fun `selecting a period reloads daily weights for that period`() = runTest {
        val weekPoints = listOf(DailyWeight(LocalDate.of(2026, 5, 16), 62.1))
        coEvery { measurementRepository.hasReadPermission() } returns true
        coEvery { getTrend(TrendPeriod.MONTH) } returns Result.success(emptyList())
        coEvery { getTrend(TrendPeriod.WEEK) } returns Result.success(weekPoints)
        val vm = viewModel()
        vm.refresh()

        vm.onPeriodSelected(TrendPeriod.WEEK)

        assertEquals(
            WeightTrendUiState(TrendPeriod.WEEK, TrendContent.Loaded(weekPoints)),
            vm.uiState.value
        )
    }

    @Test
    fun `requesting permission asks for the permissions the repository needs`() = runTest {
        val permissions = setOf("READ_WEIGHT", "READ_HEALTH_DATA_HISTORY")
        coEvery { measurementRepository.readPermissionsToRequest() } returns permissions
        val vm = viewModel()

        vm.permissionRequest.test {
            vm.onRequestPermission()

            assertEquals(permissions, awaitItem())
        }
    }

    @Test
    fun `a stale result from the previous period does not overwrite the selected period`() = runTest {
        val monthResult = CompletableDeferred<Result<List<DailyWeight>>>()
        val weekPoints = listOf(DailyWeight(LocalDate.of(2026, 5, 16), 62.1))
        coEvery { measurementRepository.hasReadPermission() } returns true
        coEvery { getTrend(TrendPeriod.MONTH) } coAnswers { monthResult.await() }
        coEvery { getTrend(TrendPeriod.WEEK) } returns Result.success(weekPoints)
        val vm = viewModel()
        vm.refresh()

        vm.onPeriodSelected(TrendPeriod.WEEK)
        monthResult.complete(
            Result.success(listOf(DailyWeight(LocalDate.of(2026, 4, 20), 64.0)))
        )

        assertEquals(
            WeightTrendUiState(TrendPeriod.WEEK, TrendContent.Loaded(weekPoints)),
            vm.uiState.value
        )
    }

    @Test
    fun `retry shows Loading while reloading after an error`() = runTest {
        val retryResult = CompletableDeferred<Result<List<DailyWeight>>>()
        coEvery { measurementRepository.hasReadPermission() } returns true
        coEvery { getTrend(TrendPeriod.MONTH) } returns
            Result.failure(WeightReadError.Unknown(RuntimeException("boom")))
        val vm = viewModel()
        vm.refresh()
        coEvery { getTrend(TrendPeriod.MONTH) } coAnswers { retryResult.await() }

        vm.retry()

        assertEquals(TrendContent.Loading, vm.uiState.value.content)
    }
}
