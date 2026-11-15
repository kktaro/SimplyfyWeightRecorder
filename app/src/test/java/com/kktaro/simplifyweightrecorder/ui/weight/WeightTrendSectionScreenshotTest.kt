package com.kktaro.simplifyweightrecorder.ui.weight

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.kktaro.simplifyweightrecorder.domain.model.DailyWeight
import com.kktaro.simplifyweightrecorder.domain.model.TrendPeriod
import java.time.LocalDate
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 体重推移グラフの見た目を固定する。差分が出たら `./gradlew recordRoborazziDebug` で撮り直し、
 * `app/src/test/screenshots/` の画像を目で確認してからコミットする。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WeightTrendSectionScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val lastDay = LocalDate.of(2026, 5, 31)

    private fun dailyWeights(days: Int) = (0 until days).map { i ->
        DailyWeight(lastDay.minusDays((days - 1 - i).toLong()), 62.0 + 0.6 * sin(i / 4.0) - i * 0.01)
    }

    private fun capture(name: String, state: WeightTrendUiState) {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    WeightTrendSection(
                        uiState = state,
                        onPeriodSelected = {},
                        onRequestPermission = {},
                        onRetry = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/$name.png",
            roborazziOptions = options
        )
    }

    @Test
    fun singleDay() = capture(
        "trend_single_day",
        WeightTrendUiState(TrendPeriod.WEEK, TrendContent.Loaded(dailyWeights(1)))
    )

    @Test
    fun week() = capture(
        "trend_week",
        WeightTrendUiState(TrendPeriod.WEEK, TrendContent.Loaded(dailyWeights(7)))
    )

    @Test
    fun month() = capture(
        "trend_month",
        WeightTrendUiState(TrendPeriod.MONTH, TrendContent.Loaded(dailyWeights(30)))
    )

    @Test
    fun year() = capture(
        "trend_year",
        WeightTrendUiState(TrendPeriod.YEAR, TrendContent.Loaded(dailyWeights(365)))
    )

    @Test
    fun permissionRequired() = capture(
        "trend_permission_required",
        WeightTrendUiState(TrendPeriod.MONTH, TrendContent.PermissionRequired)
    )

    @Test
    fun empty() = capture(
        "trend_empty",
        WeightTrendUiState(TrendPeriod.MONTH, TrendContent.Empty)
    )

    private companion object {
        // macOS で撮った画像を Linux の CI で比較するため、フォントのアンチエイリアス差は許容する
        val options = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
        )
    }
}
