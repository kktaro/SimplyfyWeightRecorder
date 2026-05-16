package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectAvailability
import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectClientProvider
import javax.inject.Inject

class CheckHealthConnectAvailabilityUseCase @Inject constructor(
    private val provider: HealthConnectClientProvider
) {
    operator fun invoke(): HealthConnectAvailability = provider.getStatus()
}
