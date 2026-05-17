package com.kktaro.simplifyweightrecorder.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.kktaro.simplifyweightrecorder.MainActivity
import com.kktaro.simplifyweightrecorder.R
import com.kktaro.simplifyweightrecorder.domain.model.WeightCandidate
import java.util.Locale

class WeightWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = widgetEntryPoint(context)
        val baseKg = ep.lastWeightRepository().getLastWeight()
        val candidates = ep.computeWeightCandidatesUseCase().invoke(baseKg)

        provideContent {
            GlanceTheme {
                WeightWidgetContent(
                    candidates = candidates,
                    uiState = currentState<Preferences>().toWidgetUiState()
                )
            }
        }
    }
}

@Composable
private fun WeightWidgetContent(
    candidates: List<WeightCandidate>,
    uiState: WeightWidgetUiState
) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp)
    ) {
        Text(
            text = context.getString(R.string.widget_title),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurface
            )
        )
        Text(
            text = uiState.statusText(context),
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
        if (uiState is WeightWidgetUiState.Error) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Button(
                text = context.getString(R.string.widget_error_open_app),
                onClick = { actionStartActivity<MainActivity>() },
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.primary,
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        CandidatesGrid(candidates)
    }
}

@Composable
private fun CandidatesGrid(candidates: List<WeightCandidate>) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        candidates.chunked(COLUMNS).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(modifier = GlanceModifier.height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                row.forEachIndexed { columnIndex, candidate ->
                    if (columnIndex > 0) Spacer(modifier = GlanceModifier.width(4.dp))
                    CandidateCell(
                        candidate = candidate,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
                if (row.size < COLUMNS) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun CandidateCell(candidate: WeightCandidate, modifier: GlanceModifier) {
    val baseline = candidate.isBaseline
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .cornerRadius(12.dp)
            .background(
                if (baseline) GlanceTheme.colors.primaryContainer
                else GlanceTheme.colors.surfaceVariant
            )
            .clickable(
                actionRunCallback<WeightSelectionActionCallback>(
                    actionParametersOf(WeightSelectionActionCallback.kgParam to candidate.kg)
                )
            )
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Text(
            text = String.format(Locale.US, "%.1f", candidate.kg),
            style = TextStyle(
                fontWeight = if (baseline) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp,
                color = if (baseline) GlanceTheme.colors.onPrimaryContainer
                else GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

private const val COLUMNS = 2

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 220)
@Composable
private fun WeightWidgetContentIdlePreview() {
    GlanceTheme {
        WeightWidgetContent(
            candidates = previewCandidates(),
            uiState = WeightWidgetUiState.Idle
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 260)
@Composable
private fun WeightWidgetContentErrorPreview() {
    GlanceTheme {
        WeightWidgetContent(
            candidates = previewCandidates(),
            uiState = WeightWidgetUiState.Error(WidgetErrorReason.PermissionDenied)
        )
    }
}

private fun previewCandidates(): List<WeightCandidate> = listOf(
    WeightCandidate(kg = 59.5, isBaseline = false),
    WeightCandidate(kg = 59.6, isBaseline = false),
    WeightCandidate(kg = 59.7, isBaseline = false),
    WeightCandidate(kg = 59.8, isBaseline = false),
    WeightCandidate(kg = 59.9, isBaseline = false),
    WeightCandidate(kg = 60.0, isBaseline = true),
    WeightCandidate(kg = 60.1, isBaseline = false),
    WeightCandidate(kg = 60.2, isBaseline = false),
    WeightCandidate(kg = 60.3, isBaseline = false),
    WeightCandidate(kg = 60.4, isBaseline = false),
    WeightCandidate(kg = 60.5, isBaseline = false)
)
