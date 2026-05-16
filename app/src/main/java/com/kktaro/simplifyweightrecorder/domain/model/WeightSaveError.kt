package com.kktaro.simplifyweightrecorder.domain.model

sealed class WeightSaveError : Throwable() {
    data object PermissionDenied : WeightSaveError()
    data object HealthConnectUnavailable : WeightSaveError()
    data class Unknown(override val cause: Throwable) : WeightSaveError()
}
