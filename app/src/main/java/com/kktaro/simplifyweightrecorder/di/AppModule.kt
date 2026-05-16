package com.kktaro.simplifyweightrecorder.di

import com.kktaro.simplifyweightrecorder.data.repository.HealthConnectWeightRepository
import com.kktaro.simplifyweightrecorder.data.repository.WeightRepository
import com.kktaro.simplifyweightrecorder.domain.time.ClockProvider
import com.kktaro.simplifyweightrecorder.domain.time.SystemClockProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindWeightRepository(impl: HealthConnectWeightRepository): WeightRepository

    @Binds
    @Singleton
    abstract fun bindClockProvider(impl: SystemClockProvider): ClockProvider
}
