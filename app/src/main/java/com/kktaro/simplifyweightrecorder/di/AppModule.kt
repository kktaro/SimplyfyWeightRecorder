package com.kktaro.simplifyweightrecorder.di

import com.kktaro.simplifyweightrecorder.data.preferences.DataStoreLastWeightRepository
import com.kktaro.simplifyweightrecorder.data.preferences.LastWeightRepository
import com.kktaro.simplifyweightrecorder.data.repository.HealthConnectWeightRepository
import com.kktaro.simplifyweightrecorder.data.repository.WeightRepository
import com.kktaro.simplifyweightrecorder.domain.time.ClockProvider
import com.kktaro.simplifyweightrecorder.domain.time.SystemClockProvider
import com.kktaro.simplifyweightrecorder.widget.WeightWidgetUpdater
import com.kktaro.simplifyweightrecorder.widget.WidgetUpdater
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

    @Binds
    @Singleton
    abstract fun bindLastWeightRepository(
        impl: DataStoreLastWeightRepository
    ): LastWeightRepository

    @Binds
    @Singleton
    abstract fun bindWidgetUpdater(impl: WeightWidgetUpdater): WidgetUpdater
}
