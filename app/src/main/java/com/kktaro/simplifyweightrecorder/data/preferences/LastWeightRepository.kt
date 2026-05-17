package com.kktaro.simplifyweightrecorder.data.preferences

interface LastWeightRepository {
    suspend fun getLastWeight(): Double?
    suspend fun setLastWeight(kg: Double)
}
