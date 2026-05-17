package com.kktaro.simplifyweightrecorder.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectAvailability
import kotlinx.coroutines.delay

private const val SUCCESS_DISPLAY_MILLIS = 3000L

class WeightSelectionActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val kg = parameters[kgParam] ?: return
        val ep = widgetEntryPoint(context)

        if (ep.checkHealthConnectAvailabilityUseCase().invoke() != HealthConnectAvailability.Installed) {
            updateState(
                context,
                glanceId,
                WeightWidgetUiState.Error(WidgetErrorReason.HealthConnectUnavailable)
            )
            return
        }

        updateState(context, glanceId, WeightWidgetUiState.Saving(kg))

        ep.saveWeightUseCase().invoke(kg).fold(
            onSuccess = {
                ep.lastWeightRepository().setLastWeight(kg)
                updateState(context, glanceId, WeightWidgetUiState.Success(kg))
                delay(SUCCESS_DISPLAY_MILLIS)
                updateState(context, glanceId, WeightWidgetUiState.Idle)
            },
            onFailure = { throwable ->
                updateState(
                    context,
                    glanceId,
                    WeightWidgetUiState.Error(throwable.toWidgetErrorReason())
                )
            }
        )
    }

    companion object {
        val kgParam = ActionParameters.Key<Double>("kg")
    }
}

private suspend fun updateState(
    context: Context,
    glanceId: GlanceId,
    state: WeightWidgetUiState
) {
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        prefs.toMutablePreferences().apply { writeWidgetUiState(state) }
    }
    WeightWidget().update(context, glanceId)
}
