package com.kktaro.simplifyweightrecorder.domain.model

import java.time.LocalDate

/** ある1日（Asia/Tokyo 基準）を代表する体重値。その日の最後の体重記録の値。 */
data class DailyWeight(val date: LocalDate, val kg: Double)
