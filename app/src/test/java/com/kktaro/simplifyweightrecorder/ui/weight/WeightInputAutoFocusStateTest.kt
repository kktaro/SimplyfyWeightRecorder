package com.kktaro.simplifyweightrecorder.ui.weight

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightInputAutoFocusStateTest {

    @Test
    fun `生成直後はフォーカス要求が立っている`() {
        assertTrue(WeightInputAutoFocusState().isFocusRequested)
    }

    @Test
    fun `フォーカス後は要求が消費される`() {
        val state = WeightInputAutoFocusState()
        state.onFocused()
        assertFalse(state.isFocusRequested)
    }

    @Test
    fun `フォアグラウンド復帰で再度フォーカス要求が立つ`() {
        val state = WeightInputAutoFocusState()
        state.onFocused()
        state.onStart()
        assertTrue(state.isFocusRequested)
    }

    @Test
    fun `権限フローからの復帰ではフォーカス要求が立たない`() {
        val state = WeightInputAutoFocusState()
        state.onFocused()
        state.onPermissionRequestLaunched()
        state.onStart()
        state.onPermissionResult()
        assertFalse(state.isFocusRequested)
    }

    @Test
    fun `権限フロー完了後のフォアグラウンド復帰では再びフォーカス要求が立つ`() {
        val state = WeightInputAutoFocusState()
        state.onFocused()
        state.onPermissionRequestLaunched()
        state.onPermissionResult()
        state.onStart()
        assertTrue(state.isFocusRequested)
    }
}
