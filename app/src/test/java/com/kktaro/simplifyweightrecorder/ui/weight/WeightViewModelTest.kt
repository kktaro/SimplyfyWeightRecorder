package com.kktaro.simplifyweightrecorder.ui.weight

import app.cash.turbine.test
import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectAvailability
import com.kktaro.simplifyweightrecorder.data.preferences.LastWeightRepository
import com.kktaro.simplifyweightrecorder.data.repository.WeightRepository
import com.kktaro.simplifyweightrecorder.domain.model.WeightInputResult
import com.kktaro.simplifyweightrecorder.domain.model.WeightSaveError
import com.kktaro.simplifyweightrecorder.domain.time.ClockProvider
import com.kktaro.simplifyweightrecorder.domain.usecase.CheckHealthConnectAvailabilityUseCase
import com.kktaro.simplifyweightrecorder.domain.usecase.SaveWeightUseCase
import com.kktaro.simplifyweightrecorder.domain.usecase.ValidateWeightInputUseCase
import com.kktaro.simplifyweightrecorder.widget.WidgetUpdater
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeightViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeClock(): ClockProvider = object : ClockProvider {
        override fun nowInTokyo(): ZonedDateTime =
            ZonedDateTime.of(2026, 5, 16, 12, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
    }

    @Test
    fun `init refreshes to Ready when Health Connect installed`() = runTest {
        val checkAvailability = mockk<CheckHealthConnectAvailabilityUseCase>()
        every { checkAvailability() } returns HealthConnectAvailability.Installed
        val viewModel = WeightViewModel(
            saveWeight = SaveWeightUseCase(NoopRepository(), fakeClock()),
            validateInput = ValidateWeightInputUseCase(),
            checkAvailability = checkAvailability,
            lastWeightRepository = mockk(relaxed = true),
            widgetUpdater = mockk(relaxed = true)
        )

        val state = viewModel.uiState.value
        assertTrue(state is WeightUiState.Ready)
        assertEquals("", (state as WeightUiState.Ready).weightInput)
        assertEquals(WeightInputResult.Empty, state.validation)
    }

    @Test
    fun `init goes to Unavailable when not installed`() = runTest {
        val checkAvailability = mockk<CheckHealthConnectAvailabilityUseCase>()
        every { checkAvailability() } returns HealthConnectAvailability.NotInstalled
        val viewModel = WeightViewModel(
            saveWeight = SaveWeightUseCase(NoopRepository(), fakeClock()),
            validateInput = ValidateWeightInputUseCase(),
            checkAvailability = checkAvailability,
            lastWeightRepository = mockk(relaxed = true),
            widgetUpdater = mockk(relaxed = true)
        )

        val state = viewModel.uiState.value
        assertTrue(state is WeightUiState.Unavailable)
        assertEquals(HealthConnectAvailability.NotInstalled, (state as WeightUiState.Unavailable).reason)
    }

    @Test
    fun `onWeightChange updates Ready state with validation`() = runTest {
        val viewModel = readyViewModel(repo = NoopRepository())

        viewModel.onWeightChange("65.4")

        val state = viewModel.uiState.value as WeightUiState.Ready
        assertEquals("65.4", state.weightInput)
        assertTrue(state.validation is WeightInputResult.Valid)
    }

    @Test
    fun `onWeightChange filters non numeric characters`() = runTest {
        val viewModel = readyViewModel(repo = NoopRepository())

        viewModel.onWeightChange("6a.5b.")

        val state = viewModel.uiState.value as WeightUiState.Ready
        assertEquals("6.5", state.weightInput)
    }

    @Test
    fun `onSubmit emits permission request only when validation is Valid`() = runTest {
        val viewModel = readyViewModel(repo = NoopRepository())

        viewModel.permissionRequest.test {
            viewModel.onWeightChange("abc")
            viewModel.onSubmit()
            expectNoEvents()

            viewModel.onWeightChange("65.4")
            viewModel.onSubmit()
            awaitItem()
        }
    }

    @Test
    fun `onPermissionResult granted true triggers save and resets to Ready Empty on success`() = runTest {
        val recorded = mutableListOf<Double>()
        val repo = object : WeightRepository {
            override suspend fun saveWeight(
                weightKg: Double,
                time: Instant,
                offset: ZoneOffset
            ): Result<Unit> {
                recorded.add(weightKg)
                return Result.success(Unit)
            }
        }
        val viewModel = readyViewModel(repo = repo)
        viewModel.onWeightChange("70")
        viewModel.snackbarEvents.test {
            viewModel.onPermissionResult(granted = true)
            assertEquals(SnackbarEvent.SaveSuccess, awaitItem())
        }
        assertEquals(listOf(70.0), recorded)
        val state = viewModel.uiState.value as WeightUiState.Ready
        assertEquals("", state.weightInput)
        assertEquals(WeightInputResult.Empty, state.validation)
    }

    @Test
    fun `onPermissionResult granted false emits PermissionDenied without saving`() = runTest {
        val repo = NoopRepository()
        val viewModel = readyViewModel(repo = repo)
        viewModel.onWeightChange("70")
        viewModel.snackbarEvents.test {
            viewModel.onPermissionResult(granted = false)
            assertEquals(SnackbarEvent.PermissionDenied, awaitItem())
        }
        assertEquals(0, repo.callCount)
        val state = viewModel.uiState.value as WeightUiState.Ready
        assertEquals("70", state.weightInput)
    }

    @Test
    fun `successful save persists last weight`() = runTest {
        val lastWeightRepository = mockk<LastWeightRepository>(relaxed = true)
        val viewModel = readyViewModel(
            repo = NoopRepository(),
            lastWeightRepository = lastWeightRepository
        )
        viewModel.onWeightChange("65.4")
        viewModel.snackbarEvents.test {
            viewModel.onPermissionResult(granted = true)
            assertEquals(SnackbarEvent.SaveSuccess, awaitItem())
        }
        coVerify { lastWeightRepository.setLastWeight(65.4) }
    }

    @Test
    fun `failed save does not persist last weight`() = runTest {
        val repo = object : WeightRepository {
            override suspend fun saveWeight(
                weightKg: Double,
                time: Instant,
                offset: ZoneOffset
            ): Result<Unit> = Result.failure(WeightSaveError.PermissionDenied)
        }
        val lastWeightRepository = mockk<LastWeightRepository>(relaxed = true)
        val viewModel = readyViewModel(repo = repo, lastWeightRepository = lastWeightRepository)
        viewModel.onWeightChange("70")
        viewModel.snackbarEvents.test {
            viewModel.onPermissionResult(granted = true)
            awaitItem()
        }
        coVerify(exactly = 0) { lastWeightRepository.setLastWeight(any()) }
    }

    @Test
    fun `successful save triggers widget update`() = runTest {
        val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
        val viewModel = readyViewModel(repo = NoopRepository(), widgetUpdater = widgetUpdater)
        viewModel.onWeightChange("65.4")
        viewModel.snackbarEvents.test {
            viewModel.onPermissionResult(granted = true)
            assertEquals(SnackbarEvent.SaveSuccess, awaitItem())
        }
        coVerify { widgetUpdater.updateAll() }
    }

    @Test
    fun `failed save does not trigger widget update`() = runTest {
        val repo = object : WeightRepository {
            override suspend fun saveWeight(
                weightKg: Double,
                time: Instant,
                offset: ZoneOffset
            ): Result<Unit> = Result.failure(WeightSaveError.PermissionDenied)
        }
        val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
        val viewModel = readyViewModel(repo = repo, widgetUpdater = widgetUpdater)
        viewModel.onWeightChange("70")
        viewModel.snackbarEvents.test {
            viewModel.onPermissionResult(granted = true)
            awaitItem()
        }
        coVerify(exactly = 0) { widgetUpdater.updateAll() }
    }

    @Test
    fun `save failure with PermissionDenied keeps input and emits PermissionDenied`() = runTest {
        val repo = object : WeightRepository {
            override suspend fun saveWeight(
                weightKg: Double,
                time: Instant,
                offset: ZoneOffset
            ): Result<Unit> = Result.failure(WeightSaveError.PermissionDenied)
        }
        val viewModel = readyViewModel(repo = repo)
        viewModel.onWeightChange("70")
        viewModel.snackbarEvents.test {
            viewModel.onPermissionResult(granted = true)
            assertEquals(SnackbarEvent.PermissionDenied, awaitItem())
        }
        val state = viewModel.uiState.value as WeightUiState.Ready
        assertEquals("70", state.weightInput)
        assertTrue(state.validation is WeightInputResult.Valid)
    }

    private fun readyViewModel(
        repo: WeightRepository,
        lastWeightRepository: LastWeightRepository = mockk(relaxed = true),
        widgetUpdater: WidgetUpdater = mockk(relaxed = true)
    ): WeightViewModel {
        val checkAvailability = mockk<CheckHealthConnectAvailabilityUseCase>()
        every { checkAvailability() } returns HealthConnectAvailability.Installed
        return WeightViewModel(
            saveWeight = SaveWeightUseCase(repo, fakeClock()),
            validateInput = ValidateWeightInputUseCase(),
            checkAvailability = checkAvailability,
            lastWeightRepository = lastWeightRepository,
            widgetUpdater = widgetUpdater
        )
    }

    private class NoopRepository : WeightRepository {
        var callCount: Int = 0
            private set

        override suspend fun saveWeight(
            weightKg: Double,
            time: Instant,
            offset: ZoneOffset
        ): Result<Unit> {
            callCount++
            return Result.success(Unit)
        }
    }
}
