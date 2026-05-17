package com.kktaro.simplifyweightrecorder.widget

import android.content.Context
import com.kktaro.simplifyweightrecorder.data.preferences.LastWeightRepository
import com.kktaro.simplifyweightrecorder.domain.usecase.ComputeWeightCandidatesUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeightWidgetEntryPoint {
    fun lastWeightRepository(): LastWeightRepository
    fun computeWeightCandidatesUseCase(): ComputeWeightCandidatesUseCase
}

internal fun widgetEntryPoint(context: Context): WeightWidgetEntryPoint =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        WeightWidgetEntryPoint::class.java
    )
