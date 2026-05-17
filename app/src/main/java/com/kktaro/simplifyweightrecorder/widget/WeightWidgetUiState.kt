package com.kktaro.simplifyweightrecorder.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kktaro.simplifyweightrecorder.R

sealed interface WeightWidgetUiState {
    data object Idle : WeightWidgetUiState
    data class Saving(val targetKg: Double) : WeightWidgetUiState
    data class Success(val savedKg: Double) : WeightWidgetUiState
    data class Error(val reason: WidgetErrorReason) : WeightWidgetUiState
}

internal fun WeightWidgetUiState.statusText(context: Context): String = when (this) {
    WeightWidgetUiState.Idle -> context.getString(R.string.widget_status_idle)
    is WeightWidgetUiState.Saving -> context.getString(R.string.widget_status_saving, targetKg)
    is WeightWidgetUiState.Success -> context.getString(R.string.widget_status_success, savedKg)
    is WeightWidgetUiState.Error -> reason.message(context)
}

private fun WidgetErrorReason.message(context: Context): String = when (this) {
    WidgetErrorReason.PermissionDenied -> context.getString(R.string.widget_error_permission_denied)
    WidgetErrorReason.HealthConnectUnavailable ->
        context.getString(R.string.widget_error_health_connect_unavailable)
    is WidgetErrorReason.Unknown -> context.getString(R.string.widget_error_unknown)
}

private const val STATUS_SAVING = "Saving"
private const val STATUS_SUCCESS = "Success"
private const val STATUS_ERROR = "Error"

private const val REASON_PERMISSION_DENIED = "PermissionDenied"
private const val REASON_HEALTH_CONNECT_UNAVAILABLE = "HealthConnectUnavailable"
private const val REASON_UNKNOWN = "Unknown"

private val STATUS_KEY = stringPreferencesKey("widget_status")
private val STATUS_KG_KEY = doublePreferencesKey("widget_status_kg")
private val STATUS_REASON_KEY = stringPreferencesKey("widget_status_reason")
private val STATUS_REASON_MESSAGE_KEY = stringPreferencesKey("widget_status_reason_message")

internal fun Preferences.toWidgetUiState(): WeightWidgetUiState = when (this[STATUS_KEY]) {
    STATUS_SAVING -> WeightWidgetUiState.Saving(this[STATUS_KG_KEY] ?: 0.0)
    STATUS_SUCCESS -> WeightWidgetUiState.Success(this[STATUS_KG_KEY] ?: 0.0)
    STATUS_ERROR -> WeightWidgetUiState.Error(decodeReason(this))
    else -> WeightWidgetUiState.Idle
}

private fun decodeReason(prefs: Preferences): WidgetErrorReason =
    when (prefs[STATUS_REASON_KEY]) {
        REASON_PERMISSION_DENIED -> WidgetErrorReason.PermissionDenied
        REASON_HEALTH_CONNECT_UNAVAILABLE -> WidgetErrorReason.HealthConnectUnavailable
        else -> WidgetErrorReason.Unknown(prefs[STATUS_REASON_MESSAGE_KEY])
    }

internal fun MutablePreferences.writeWidgetUiState(state: WeightWidgetUiState) {
    remove(STATUS_KEY)
    remove(STATUS_KG_KEY)
    remove(STATUS_REASON_KEY)
    remove(STATUS_REASON_MESSAGE_KEY)
    when (state) {
        WeightWidgetUiState.Idle -> Unit
        is WeightWidgetUiState.Saving -> {
            this[STATUS_KEY] = STATUS_SAVING
            this[STATUS_KG_KEY] = state.targetKg
        }
        is WeightWidgetUiState.Success -> {
            this[STATUS_KEY] = STATUS_SUCCESS
            this[STATUS_KG_KEY] = state.savedKg
        }
        is WeightWidgetUiState.Error -> {
            this[STATUS_KEY] = STATUS_ERROR
            this[STATUS_REASON_KEY] = state.reason.code
            if (state.reason is WidgetErrorReason.Unknown) {
                state.reason.message?.let { this[STATUS_REASON_MESSAGE_KEY] = it }
            }
        }
    }
}

private val WidgetErrorReason.code: String
    get() = when (this) {
        WidgetErrorReason.PermissionDenied -> REASON_PERMISSION_DENIED
        WidgetErrorReason.HealthConnectUnavailable -> REASON_HEALTH_CONNECT_UNAVAILABLE
        is WidgetErrorReason.Unknown -> REASON_UNKNOWN
    }
