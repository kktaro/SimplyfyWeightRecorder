package com.kktaro.simplifyweightrecorder.domain.model

sealed class WeightInputResult {
    data class Valid(val kg: Double) : WeightInputResult()
    data object Empty : WeightInputResult()
    data object NotANumber : WeightInputResult()
    data object OutOfRange : WeightInputResult()
    data object TooManyDecimals : WeightInputResult()
}
