package com.kktaro.simplifyweightrecorder.widget

import com.kktaro.simplifyweightrecorder.domain.model.WeightSaveError

sealed interface WidgetErrorReason {
    data object PermissionDenied : WidgetErrorReason
    data object HealthConnectUnavailable : WidgetErrorReason
    data class Unknown(val message: String?) : WidgetErrorReason
}

internal fun Throwable.toWidgetErrorReason(): WidgetErrorReason = when (this) {
    is WeightSaveError.PermissionDenied -> WidgetErrorReason.PermissionDenied
    is WeightSaveError.HealthConnectUnavailable -> WidgetErrorReason.HealthConnectUnavailable
    is WeightSaveError.Unknown -> WidgetErrorReason.Unknown(cause.message)
    else -> WidgetErrorReason.Unknown(message)
}
