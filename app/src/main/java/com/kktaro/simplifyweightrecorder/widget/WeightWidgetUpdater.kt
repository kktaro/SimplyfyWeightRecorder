package com.kktaro.simplifyweightrecorder.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetUpdater {
    override suspend fun updateAll() {
        WeightWidget().updateAll(context)
    }
}
