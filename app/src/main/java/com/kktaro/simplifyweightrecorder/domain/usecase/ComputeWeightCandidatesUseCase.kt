package com.kktaro.simplifyweightrecorder.domain.usecase

import com.kktaro.simplifyweightrecorder.domain.model.WeightCandidate
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ComputeWeightCandidatesUseCase @Inject constructor() {

    operator fun invoke(baseKg: Double?): List<WeightCandidate> {
        val base = (baseKg ?: DEFAULT_BASELINE_KG).toScaledBigDecimal()
        val maxKg = ValidateWeightInputUseCase.MAX_WEIGHT_KG.toScaledBigDecimal()
        return (-OFFSET_STEPS..OFFSET_STEPS)
            .map { step -> base.add(STEP_KG.multiply(BigDecimal(step))) }
            .filter { it > BigDecimal.ZERO && it <= maxKg }
            .map { value ->
                WeightCandidate(
                    kg = value.toDouble(),
                    isBaseline = value.compareTo(base) == 0
                )
            }
    }

    private fun Double.toScaledBigDecimal(): BigDecimal =
        BigDecimal.valueOf(this).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)

    companion object {
        const val DEFAULT_BASELINE_KG = 60.0
        private const val OFFSET_STEPS = 5
        private const val DECIMAL_SCALE = 1
        private val STEP_KG = BigDecimal("0.1")
    }
}
