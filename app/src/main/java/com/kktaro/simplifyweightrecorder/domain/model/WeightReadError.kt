package com.kktaro.simplifyweightrecorder.domain.model

sealed class WeightReadError : Throwable() {
    data object PermissionDenied : WeightReadError()
    data object HealthConnectUnavailable : WeightReadError()
    data class Unknown(override val cause: Throwable) : WeightReadError()
}
