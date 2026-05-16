package com.kktaro.simplifyweightrecorder.ui.weight

import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectAvailability
import com.kktaro.simplifyweightrecorder.domain.model.WeightInputResult

sealed interface WeightUiState {
    data object Initializing : WeightUiState

    data class Unavailable(
        val reason: HealthConnectAvailability
    ) : WeightUiState

    data class Ready(
        val weightInput: String,
        val validation: WeightInputResult
    ) : WeightUiState {
        val isSubmitEnabled: Boolean get() = validation is WeightInputResult.Valid
    }

    data class Submitting(
        val weightInput: String
    ) : WeightUiState
}

sealed interface SnackbarEvent {
    data object SaveSuccess : SnackbarEvent
    data object PermissionDenied : SnackbarEvent
    data object HealthConnectUnavailable : SnackbarEvent
    data class UnknownError(val message: String?) : SnackbarEvent
}
