package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.domain.model.WeightInputResult
import javax.inject.Inject

class ValidateWeightInputUseCase @Inject constructor() {
    operator fun invoke(raw: String): WeightInputResult {
        if (raw.isEmpty()) return WeightInputResult.Empty
        if (!STRICT_FORMAT.matches(raw)) return WeightInputResult.NotANumber
        val parts = raw.split('.')
        if (parts.size == 2 && parts[1].length > 1) return WeightInputResult.TooManyDecimals
        val kg = raw.toDoubleOrNull() ?: return WeightInputResult.NotANumber
        if (kg <= 0.0 || kg > MAX_WEIGHT_KG) return WeightInputResult.OutOfRange
        return WeightInputResult.Valid(kg)
    }

    companion object {
        const val MAX_WEIGHT_KG = 500.0
        private val STRICT_FORMAT = Regex("^\\d+(\\.\\d+)?$")
    }
}
