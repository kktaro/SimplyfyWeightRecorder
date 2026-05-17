package com.kktaro.simplifyweightrecorder.widget

import android.content.Context
import com.kktaro.simplifyweightrecorder.data.preferences.LastWeightRepository
import com.kktaro.simplifyweightrecorder.domain.usecase.CheckHealthConnectAvailabilityUseCase
import com.kktaro.simplifyweightrecorder.domain.usecase.ComputeWeightCandidatesUseCase
import com.kktaro.simplifyweightrecorder.domain.usecase.SaveWeightUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeightWidgetEntryPoint {
    fun lastWeightRepository(): LastWeightRepository
    fun computeWeightCandidatesUseCase(): ComputeWeightCandidatesUseCase
    fun saveWeightUseCase(): SaveWeightUseCase
    fun checkHealthConnectAvailabilityUseCase(): CheckHealthConnectAvailabilityUseCase
}

internal fun widgetEntryPoint(context: Context): WeightWidgetEntryPoint =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        WeightWidgetEntryPoint::class.java
    )
