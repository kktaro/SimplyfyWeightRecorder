package com.kktaro.simplifyweightrecorder.ui.weight

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kktaro.simplifyweightrecorder.R
import com.kktaro.simplifyweightrecorder.data.healthconnect.HealthConnectAvailability
import com.kktaro.simplifyweightrecorder.domain.model.WeightInputResult

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

@Composable
fun WeightScreen(
    modifier: Modifier = Modifier,
    viewModel: WeightViewModel = hiltViewModel(),
    trendViewModel: WeightTrendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trendUiState by trendViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val autoFocusState = rememberWeightInputAutoFocusState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        autoFocusState.onPermissionResult()
        val writePermission = HealthPermission.getWritePermission(WeightRecord::class)
        viewModel.onPermissionResult(grantedPermissions.contains(writePermission))
    }

    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        autoFocusState.onPermissionResult()
        trendViewModel.refresh()
    }

    LaunchedEffect(trendViewModel) {
        trendViewModel.permissionRequest.collect { permissions ->
            autoFocusState.onPermissionRequestLaunched()
            readPermissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.permissionRequest.collect {
            autoFocusState.onPermissionRequestLaunched()
            permissionLauncher.launch(
                setOf(HealthPermission.getWritePermission(WeightRecord::class))
            )
        }
    }

    val snackbarSuccess = stringResource(id = R.string.snackbar_save_success)
    val snackbarPermissionDenied = stringResource(id = R.string.snackbar_permission_denied)
    val snackbarUnavailable = stringResource(id = R.string.snackbar_health_connect_unavailable)
    val snackbarUnknownPrefix = stringResource(id = R.string.snackbar_unknown_error_prefix)

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { event ->
            val message = when (event) {
                SnackbarEvent.SaveSuccess -> snackbarSuccess
                SnackbarEvent.PermissionDenied -> snackbarPermissionDenied
                SnackbarEvent.HealthConnectUnavailable -> snackbarUnavailable
                is SnackbarEvent.UnknownError ->
                    event.message?.let { "$snackbarUnknownPrefix: $it" } ?: snackbarUnknownPrefix
            }
            if (event == SnackbarEvent.SaveSuccess) trendViewModel.refresh()
            snackbarHostState.showSnackbar(message)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        autoFocusState.onStart()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAvailability()
        trendViewModel.refresh()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        WeightRouter(
            uiState = uiState,
            shouldFocusInput = autoFocusState.isFocusRequested,
            onInputFocused = autoFocusState::onFocused,
            trendSection = {
                WeightTrendSection(
                    uiState = trendUiState,
                    onPeriodSelected = trendViewModel::onPeriodSelected,
                    onRequestPermission = trendViewModel::onRequestPermission,
                    onRetry = trendViewModel::retry
                )
            },
            onWeightChange = viewModel::onWeightChange,
            onSubmit = {
                focusManager.clearFocus()
                viewModel.onSubmit()
            },
            onOpenHealthConnect = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "market://details?id=$HEALTH_CONNECT_PACKAGE".toUri()
                    setPackage("com.android.vending")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
internal fun WeightRouter(
    uiState: WeightUiState,
    shouldFocusInput: Boolean,
    onInputFocused: () -> Unit,
    onWeightChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    modifier: Modifier = Modifier,
    trendSection: @Composable () -> Unit = {}
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (uiState) {
            WeightUiState.Initializing -> CircularProgressIndicator()
            is WeightUiState.Unavailable -> HealthConnectUnavailableContent(
                reason = uiState.reason,
                onOpenHealthConnect = onOpenHealthConnect
            )
            is WeightUiState.Ready -> InputAndTrendContent(trendSection) {
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                LaunchedEffect(shouldFocusInput) {
                    if (shouldFocusInput) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                        onInputFocused()
                    }
                }
                WeightInputContent(
                    weightInput = uiState.weightInput,
                    validation = uiState.validation,
                    isSubmitting = false,
                    onWeightChange = onWeightChange,
                    onSubmit = onSubmit,
                    focusRequester = focusRequester
                )
            }
            is WeightUiState.Submitting -> InputAndTrendContent(trendSection) {
                WeightInputContent(
                    weightInput = uiState.weightInput,
                    validation = WeightInputResult.Empty,
                    isSubmitting = true,
                    onWeightChange = {},
                    onSubmit = {}
                )
            }
        }
    }
}

@Composable
private fun InputAndTrendContent(
    trendSection: @Composable () -> Unit,
    inputContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        inputContent()
        trendSection()
    }
}

@Composable
private fun HealthConnectUnavailableContent(
    reason: HealthConnectAvailability,
    onOpenHealthConnect: () -> Unit
) {
    val messageRes = when (reason) {
        HealthConnectAvailability.NotInstalled -> R.string.health_connect_not_installed
        HealthConnectAvailability.UpdateRequired -> R.string.health_connect_update_required
        else -> R.string.health_connect_not_installed
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = messageRes),
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onOpenHealthConnect) {
            Text(stringResource(id = R.string.health_connect_open_store))
        }
    }
}
