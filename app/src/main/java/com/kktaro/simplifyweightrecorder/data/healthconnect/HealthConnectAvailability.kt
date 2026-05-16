package com.kktaro.simplifyweightrecorder.data.healthconnect

sealed class HealthConnectAvailability {
    data object Unknown : HealthConnectAvailability()
    data object Installed : HealthConnectAvailability()
    data object NotInstalled : HealthConnectAvailability()
    data object UpdateRequired : HealthConnectAvailability()
}
