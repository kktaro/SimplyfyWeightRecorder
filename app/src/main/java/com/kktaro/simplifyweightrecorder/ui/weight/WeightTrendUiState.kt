package com.kktaro.simplifyweightrecorder.ui.weight

import com.kktaro.simplifyweightrecorder.domain.model.DailyWeight
import com.kktaro.simplifyweightrecorder.domain.model.TrendPeriod

data class WeightTrendUiState(
    val period: TrendPeriod = TrendPeriod.MONTH,
    val content: TrendContent = TrendContent.Loading
)

sealed interface TrendContent {
    data object Loading : TrendContent
    data object PermissionRequired : TrendContent
    data object Empty : TrendContent
    data class Loaded(val points: List<DailyWeight>) : TrendContent
    data object Error : TrendContent
}
