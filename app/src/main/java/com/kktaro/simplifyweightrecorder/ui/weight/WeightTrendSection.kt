package com.kktaro.simplifyweightrecorder.ui.weight

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kktaro.simplifyweightrecorder.R
import com.kktaro.simplifyweightrecorder.domain.model.DailyWeight
import com.kktaro.simplifyweightrecorder.domain.model.TrendPeriod
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

private val chartHeight = 240.dp
private const val MAX_DAYS_WITH_POINTS = 45

@Composable
fun WeightTrendSection(
    uiState: WeightTrendUiState,
    onPeriodSelected: (TrendPeriod) -> Unit,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.trend_title),
            style = MaterialTheme.typography.titleMedium
        )
        TrendPeriodSelector(selected = uiState.period, onSelected = onPeriodSelected)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.Center
        ) {
            when (val content = uiState.content) {
                TrendContent.Loading -> CircularProgressIndicator()
                TrendContent.PermissionRequired -> TrendMessage(
                    messageRes = R.string.trend_permission_required,
                    actionRes = R.string.trend_permission_button,
                    onAction = onRequestPermission,
                    filledAction = true
                )
                TrendContent.Empty -> TrendMessage(messageRes = R.string.trend_empty)
                TrendContent.Error -> TrendMessage(
                    messageRes = R.string.trend_error,
                    actionRes = R.string.trend_retry_button,
                    onAction = onRetry
                )
                is TrendContent.Loaded -> WeightTrendChart(
                    dailyWeights = content.dailyWeights,
                    period = uiState.period
                )
            }
        }
    }
}

@Composable
private fun TrendPeriodSelector(selected: TrendPeriod, onSelected: (TrendPeriod) -> Unit) {
    val periods = TrendPeriod.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        periods.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                label = { Text(stringResource(id = period.labelRes)) }
            )
        }
    }
}

@Composable
private fun TrendMessage(
    @StringRes messageRes: Int,
    @StringRes actionRes: Int? = null,
    onAction: () -> Unit = {},
    filledAction: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(id = messageRes), style = MaterialTheme.typography.bodyMedium)
        if (actionRes != null) {
            if (filledAction) {
                Button(onClick = onAction) { Text(stringResource(id = actionRes)) }
            } else {
                OutlinedButton(onClick = onAction) { Text(stringResource(id = actionRes)) }
            }
        }
    }
}

@Composable
private fun WeightTrendChart(dailyWeights: List<DailyWeight>, period: TrendPeriod) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(dailyWeights) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = dailyWeights.map { it.date.toEpochDay() },
                    y = dailyWeights.map { it.kg }
                )
            }
        }
    }
    val datePattern = stringResource(id = R.string.trend_date_format)
    val markerTemplate = stringResource(id = R.string.trend_marker_format)
    val axisDateFormatter = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern) }
    val dateFormatter = remember(axisDateFormatter) {
        CartesianValueFormatter { _, value, _ ->
            LocalDate.ofEpochDay(value.toLong()).format(axisDateFormatter)
        }
    }
    val markerFormatter = remember(axisDateFormatter, markerTemplate) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val target = targets.firstOrNull() ?: return@ValueFormatter ""
            val kg = (target as? LineCartesianLayerMarkerTarget)?.points?.firstOrNull()?.entry?.y
            val date = LocalDate.ofEpochDay(target.x.toLong()).format(axisDateFormatter)
            if (kg == null) date else markerTemplate.format(date, kg)
        }
    }
    val labelSpacingDays = period.axisLabelSpacingDays
    val itemPlacer = remember(labelSpacingDays) {
        HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacingDays })
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val pointComponent = rememberShapeComponent(fill = Fill(lineColor), shape = CircleShape)
    val pointProvider = remember(pointComponent) {
        LineCartesianLayer.PointProvider.single(
            LineCartesianLayer.Point(component = pointComponent, size = 6.dp)
        )
    }
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = remember(lineColor) {
                                LineCartesianLayer.LineFill.single(Fill(lineColor))
                            },
                            // 記録が1日分だけでも見えるよう点を描く。日数が多いと線が潰れるので省く
                            pointProvider = pointProvider.takeIf {
                                dailyWeights.size <= MAX_DAYS_WITH_POINTS
                            }
                        )
                    ),
                    rangeProvider = PaddedWeightRangeProvider
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = remember { CartesianValueFormatter.decimal(decimalCount = 1) }
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = dateFormatter,
                    itemPlacer = itemPlacer
                ),
                marker = rememberDefaultCartesianMarker(
                    label = rememberTextComponent(
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    ),
                    valueFormatter = markerFormatter,
                    guideline = rememberAxisGuidelineComponent()
                ),
                // 右端の日付ラベルが切れないよう余白を取る
                layerPadding = remember {
                    { CartesianLayerPadding(unscalableStart = 8.dp, unscalableEnd = 20.dp) }
                },
                getXStep = { _, _, _ -> 1.0 },
                markerController = CartesianMarkerController.rememberToggleOnTap()
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(
                zoomEnabled = false,
                initialZoom = Zoom.Content,
                minZoom = Zoom.Content,
                maxZoom = Zoom.Content
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        )
    }
}

/** 縦軸を表示期間内の最小値〜最大値に 0.5kg 単位の余白を足した範囲にする。 */
private object PaddedWeightRangeProvider : CartesianLayerRangeProvider {
    private const val PADDING_KG = 0.5

    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore) =
        floor((minY - PADDING_KG) * 2) / 2

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore) =
        ceil((maxY + PADDING_KG) * 2) / 2
}

@get:StringRes
private val TrendPeriod.labelRes: Int
    get() = when (this) {
        TrendPeriod.WEEK -> R.string.trend_period_week
        TrendPeriod.MONTH -> R.string.trend_period_month
        TrendPeriod.THREE_MONTHS -> R.string.trend_period_three_months
        TrendPeriod.YEAR -> R.string.trend_period_year
    }

private val TrendPeriod.axisLabelSpacingDays: Int
    get() = when (this) {
        TrendPeriod.WEEK -> 1
        TrendPeriod.MONTH -> 7
        TrendPeriod.THREE_MONTHS -> 14
        TrendPeriod.YEAR -> 60
    }
