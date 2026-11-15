package com.kktaro.simplifyweightrecorder.ui.weight

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kktaro.simplifyweightrecorder.domain.model.WeightInputResult
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeightRouterFocusTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ready = WeightUiState.Ready(weightInput = "", validation = WeightInputResult.Empty)

    @Test
    fun initializingから遷移したReadyで入力欄にフォーカスが当たる() {
        var uiState: WeightUiState by mutableStateOf(WeightUiState.Initializing)
        var shouldFocus by mutableStateOf(true)
        composeRule.setContent {
            WeightRouter(
                uiState = uiState,
                shouldFocusInput = shouldFocus,
                onInputFocused = { shouldFocus = false },
                onWeightChange = {},
                onSubmit = {},
                onOpenHealthConnect = {}
            )
        }

        composeRule.runOnIdle { uiState = ready }

        composeRule.onNodeWithTag(WEIGHT_INPUT_TEST_TAG).assertIsFocused()
        composeRule.runOnIdle { assertFalse(shouldFocus) }
    }

    @Test
    fun フォーカス要求がなければReadyでもフォーカスしない() {
        composeRule.setContent {
            WeightRouter(
                uiState = ready,
                shouldFocusInput = false,
                onInputFocused = {},
                onWeightChange = {},
                onSubmit = {},
                onOpenHealthConnect = {}
            )
        }

        composeRule.onNodeWithTag(WEIGHT_INPUT_TEST_TAG).assertIsNotFocused()
    }
}
