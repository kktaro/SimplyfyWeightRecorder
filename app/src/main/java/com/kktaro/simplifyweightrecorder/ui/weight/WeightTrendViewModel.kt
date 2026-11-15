package com.kktaro.simplifyweightrecorder.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kktaro.simplifyweightrecorder.data.repository.WeightHistoryRepository
import com.kktaro.simplifyweightrecorder.domain.model.TrendPeriod
import com.kktaro.simplifyweightrecorder.domain.model.WeightReadError
import com.kktaro.simplifyweightrecorder.domain.usecase.GetWeightTrendUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    private val historyRepository: WeightHistoryRepository
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
            _permissionRequest.tryEmit(historyRepository.readPermissionsToRequest())
        }
    }

    fun onPeriodSelected(period: TrendPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period, content = TrendContent.Loading) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!historyRepository.hasReadPermission()) {
                _uiState.update { it.copy(content = TrendContent.PermissionRequired) }
                return@launch
            }
            val content = getTrend(_uiState.value.period).fold(
                onSuccess = { points ->
                    if (points.isEmpty()) TrendContent.Empty else TrendContent.Loaded(points)
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
