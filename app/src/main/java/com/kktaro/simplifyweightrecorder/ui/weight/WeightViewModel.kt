package com.kktaro.simplifyweightrecorder.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectAvailability
import com.kktaro.simplifyweightrecorder.domain.model.WeightInputResult
import com.kktaro.simplifyweightrecorder.domain.model.WeightSaveError
import com.kktaro.simplifyweightrecorder.domain.usecase.CheckHealthConnectAvailabilityUseCase
import com.kktaro.simplifyweightrecorder.domain.usecase.SaveWeightUseCase
import com.kktaro.simplifyweightrecorder.domain.usecase.ValidateWeightInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val saveWeight: SaveWeightUseCase,
    private val validateInput: ValidateWeightInputUseCase,
    private val checkAvailability: CheckHealthConnectAvailabilityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeightUiState>(WeightUiState.Initializing)
    val uiState: StateFlow<WeightUiState> = _uiState.asStateFlow()

    init {
        refreshAvailability()
    }

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    private val _permissionRequest = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val permissionRequest: SharedFlow<Unit> = _permissionRequest.asSharedFlow()

    fun refreshAvailability() {
        val current = _uiState.value
        val isInputting = current is WeightUiState.Ready || current is WeightUiState.Submitting
        when (val availability = checkAvailability()) {
            HealthConnectAvailability.Installed -> {
                if (!isInputting) {
                    _uiState.value = WeightUiState.Ready(
                        weightInput = "",
                        validation = WeightInputResult.Empty
                    )
                }
            }
            HealthConnectAvailability.NotInstalled,
            HealthConnectAvailability.UpdateRequired -> {
                _uiState.value = WeightUiState.Unavailable(reason = availability)
            }
            HealthConnectAvailability.Unknown -> {
                _uiState.value = WeightUiState.Initializing
            }
        }
    }

    fun onWeightChange(raw: String) {
        val current = _uiState.value as? WeightUiState.Ready ?: return
        val filtered = filterInput(raw)
        _uiState.value = current.copy(
            weightInput = filtered,
            validation = validateInput(filtered)
        )
    }

    fun onSubmit() {
        val current = _uiState.value as? WeightUiState.Ready ?: return
        if (current.validation !is WeightInputResult.Valid) return
        _permissionRequest.tryEmit(Unit)
    }

    fun onPermissionResult(granted: Boolean) {
        val current = _uiState.value as? WeightUiState.Ready ?: return
        val valid = current.validation as? WeightInputResult.Valid ?: return
        if (!granted) {
            _snackbarEvents.tryEmit(SnackbarEvent.PermissionDenied)
            return
        }
        _uiState.value = WeightUiState.Submitting(weightInput = current.weightInput)
        viewModelScope.launch {
            val result = saveWeight(valid.kg)
            result.fold(
                onSuccess = {
                    _uiState.value = WeightUiState.Ready(
                        weightInput = "",
                        validation = WeightInputResult.Empty
                    )
                    _snackbarEvents.tryEmit(SnackbarEvent.SaveSuccess)
                },
                onFailure = { throwable ->
                    _uiState.value = WeightUiState.Ready(
                        weightInput = current.weightInput,
                        validation = current.validation
                    )
                    val event = when (throwable) {
                        is WeightSaveError.PermissionDenied -> SnackbarEvent.PermissionDenied
                        is WeightSaveError.HealthConnectUnavailable ->
                            SnackbarEvent.HealthConnectUnavailable
                        else -> SnackbarEvent.UnknownError(throwable.message)
                    }
                    _snackbarEvents.tryEmit(event)
                }
            )
        }
    }

    private fun filterInput(raw: String): String {
        val sb = StringBuilder()
        var seenDot = false
        for (c in raw) {
            when {
                c.isDigit() -> sb.append(c)
                c == '.' && !seenDot -> {
                    sb.append(c)
                    seenDot = true
                }
            }
        }
        return sb.toString()
    }

}
