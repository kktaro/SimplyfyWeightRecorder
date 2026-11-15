package com.kktaro.simplifyweightrecorder.ui.weight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * 体重入力欄へ自動でフォーカスを当てるかどうかを管理する。
 *
 * - 画面生成時（コールドスタート・再生成）とフォアグラウンド復帰時にフォーカス要求を立てる。
 * - Health Connect の権限フローから戻った際のフォアグラウンド復帰は対象外。
 * - 要求は入力欄が表示（Ready）された時点で消費される。そのため保存後の Submitting → Ready では再フォーカスしない。
 */
class WeightInputAutoFocusState(
    awaitingPermission: Boolean = false
) {
    var isFocusRequested by mutableStateOf(true)
        private set

    private var awaitingPermission = awaitingPermission

    fun onStart() {
        if (!awaitingPermission) {
            isFocusRequested = true
        }
    }

    fun onPermissionRequestLaunched() {
        awaitingPermission = true
    }

    fun onPermissionResult() {
        awaitingPermission = false
    }

    fun onFocused() {
        isFocusRequested = false
    }

    companion object {
        val Saver: Saver<WeightInputAutoFocusState, Boolean> = Saver(
            save = { it.awaitingPermission },
            restore = { WeightInputAutoFocusState(awaitingPermission = it) }
        )
    }
}

@Composable
fun rememberWeightInputAutoFocusState(): WeightInputAutoFocusState =
    rememberSaveable(saver = WeightInputAutoFocusState.Saver) { WeightInputAutoFocusState() }
