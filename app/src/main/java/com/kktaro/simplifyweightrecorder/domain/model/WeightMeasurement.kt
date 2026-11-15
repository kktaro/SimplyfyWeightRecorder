package com.kktaro.simplifyweightrecorder.domain.model

import java.time.Instant

/** ある時点で計測された1件の体重記録。 */
data class WeightMeasurement(val time: Instant, val kg: Double)
