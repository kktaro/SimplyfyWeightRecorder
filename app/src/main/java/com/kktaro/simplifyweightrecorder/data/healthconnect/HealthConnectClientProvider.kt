package com.kktaro.simplifyweightrecorder.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectClientProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getStatus(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Installed
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectAvailability.NotInstalled
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.UpdateRequired
            else -> HealthConnectAvailability.Unknown
        }

    fun getClientOrNull(): HealthConnectClient? =
        if (getStatus() == HealthConnectAvailability.Installed) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
}
