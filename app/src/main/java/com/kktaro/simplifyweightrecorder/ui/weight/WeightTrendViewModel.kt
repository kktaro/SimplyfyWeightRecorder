package com.kktaro.simplifyweightrecorder.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kktaro.simplifyweightrecorder.data.repository.WeightMeasurementRepository
import com.kktaro.simplifyweightrecorder.domain.model.TrendPeriod
import com.kktaro.simplifyweightrecorder.domain.model.WeightReadError
import com.kktaro.simplifyweightrecorder.domain.usecase.GetWeightTrendUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WeightTrendViewModel @Inject constructor(
    private val getTrend: GetWeightTrendUseCase,
    private val measurementRepository: WeightMeasurementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightTrendUiState())
    val uiState: StateFlow<WeightTrendUiState> = _uiState.asStateFlow()

    private val _permissionRequest = MutableSharedFlow<Set<String>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val permissionRequest: SharedFlow<Set<String>> = _permissionRequest.asSharedFlow()

    fun onRequestPermission() {
        viewModelScope.launch {
            _permissionRequest.tryEmit(measurementRepository.readPermissionsToRequest())
        }
    }

    fun onPeriodSelected(period: TrendPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period, content = TrendContent.Loading) }
        refresh()
    }

    private var loadJob: Job? = null

    fun retry() {
        _uiState.update { it.copy(content = TrendContent.Loading) }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!measurementRepository.hasReadPermission()) {
                _uiState.update { it.copy(content = TrendContent.PermissionRequired) }
                return@launch
            }
            val content = getTrend(_uiState.value.period).fold(
                onSuccess = { dailyWeights ->
                    if (dailyWeights.isEmpty()) TrendContent.Empty else TrendContent.Loaded(dailyWeights)
                },
                onFailure = { error ->
                    if (error is WeightReadError.PermissionDenied) {
                        TrendContent.PermissionRequired
                    } else {
                        TrendContent.Error
                    }
                }
            )
            _uiState.update { it.copy(content = content) }
        }
    }
}
